package com.openclaw.test.controller;

import com.openclaw.test.dto.ChatSendRequest;
import com.openclaw.test.entity.ChatMessage;
import com.openclaw.test.entity.User;
import com.openclaw.test.repository.ChatMessageRepository;
import com.openclaw.test.repository.UserRepository;
import com.openclaw.test.service.IdempotencyCache;
import com.openclaw.test.service.OpenClawService;
import com.openclaw.test.service.StreamManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "聊天历史", description = "聊天历史记录与 SSE 流接口")
public class ChatHistoryController {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryController.class);

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final OpenClawService openClawService;
    private final IdempotencyCache idempotencyCache;

    public ChatHistoryController(ChatMessageRepository chatMessageRepository,
                                  UserRepository userRepository,
                                  OpenClawService openClawService,
                                  IdempotencyCache idempotencyCache) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.openClawService = openClawService;
        this.idempotencyCache = idempotencyCache;
    }

    private Long getUserId(HttpServletRequest request) { return (Long) request.getAttribute("userId"); }
    private String getUsername(HttpServletRequest request) { return (String) request.getAttribute("username"); }

    @PostMapping("/send")
    @Operation(summary = "发送消息", description = "发送消息并获取完整回复")
    public ResponseEntity<?> sendMessage(@RequestBody ChatSendRequest request, HttpServletRequest servletRequest) {
        Long userId = getUserId(servletRequest);
        String username = getUsername(servletRequest);
        if (request.getMessage() == null || request.getMessage().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "消息不能为空"));
        try {
            var result = openClawService.chat(request.getMessage(), null, 8192);
            ChatMessage chatMessage = new ChatMessage(userId, username, request.getMessage(), result.content(), result.tokenUsage());
            chatMessage = chatMessageRepository.save(chatMessage);
            return ResponseEntity.ok(Map.of("id", chatMessage.getId(), "message", chatMessage.getMessage(),
                "reply", chatMessage.getReply(), "tokenUsage", chatMessage.getTokenUsage(), "createdAt", chatMessage.getCreatedAt().toString()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "发送消息失败: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    @Operation(summary = "获取历史记录", description = "获取当前用户的所有聊天历史")
    public ResponseEntity<?> getHistory(HttpServletRequest request) {
        Long userId = getUserId(request);
        List<ChatMessage> messages = chatMessageRepository.findByUserIdOrderByCreatedAtDesc(userId);
        var result = messages.stream().map(m -> Map.of("id", m.getId(), "message", m.getMessage(),
            "reply", m.getReply(), "tokenUsage", m.getTokenUsage(), "createdAt", m.getCreatedAt().toString())).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history/{id}")
    @Operation(summary = "获取历史详情", description = "根据 ID 获取单条历史记录详情")
    public ResponseEntity<?> getHistoryDetail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        var opt = chatMessageRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        ChatMessage msg = opt.get();
        if (!msg.getUserId().equals(userId)) return ResponseEntity.status(403).body(Map.of("error", "无权查看此记录"));
        return ResponseEntity.ok(Map.of("id", msg.getId(), "message", msg.getMessage(), "reply", msg.getReply(),
            "tokenUsage", msg.getTokenUsage(), "createdAt", msg.getCreatedAt().toString()));
    }

    @DeleteMapping("/history/{id}")
    @Operation(summary = "删除历史记录", description = "根据 ID 删除单条历史记录")
    public ResponseEntity<?> deleteHistory(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        var opt = chatMessageRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        ChatMessage msg = opt.get();
        if (!msg.getUserId().equals(userId)) return ResponseEntity.status(403).body(Map.of("error", "无权删除此记录"));
        chatMessageRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @GetMapping("/admin/all")
    @Operation(summary = "管理员查看全部记录", description = "管理员查看所有用户的聊天记录")
    public ResponseEntity<?> adminAll(HttpServletRequest request) {
        Long userId = getUserId(request);
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || !"admin".equals(userOpt.get().getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "仅管理员可查看"));
        List<ChatMessage> allMessages = chatMessageRepository.findAllByOrderByCreatedAtDesc();
        var result = allMessages.stream().map(m -> Map.of("id", m.getId(), "userId", m.getUserId(), "username", m.getUsername(),
            "message", m.getMessage(), "reply", m.getReply(), "tokenUsage", m.getTokenUsage(), "createdAt", m.getCreatedAt().toString())).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/stream")
    @Operation(summary = "SSE 流式对话", description = "通过 SSE 流式传输 OpenClaw 回复（支持幂等保护）")
    public SseEmitter streamMessage(@RequestBody ChatSendRequest request,
                                     HttpServletRequest servletRequest) {
        Long userId = getUserId(servletRequest);
        String username = getUsername(servletRequest);

        // ------ 幂等保护检查 ------
        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String cacheKey = "stream:" + idempotencyKey;
            Object cached = idempotencyCache.get(cacheKey);
            if (cached != null) {
                if (cached == IdempotencyCache.PROCESSING_MARKER) {
                    // 正在处理中，返回 409
                    log.warn("Idempotency conflict (processing): {}", cacheKey);
                    SseEmitter conflictEmitter = new SseEmitter(0L);
                    try {
                        conflictEmitter.send(SseEmitter.event().name("error").data("该请求正在处理中，请勿重复发送"));
                    } catch (Exception ex) { /* ignore */ }
                    conflictEmitter.complete();
                    return conflictEmitter;
                } else if (cached instanceof String cachedResult) {
                    // 已有缓存结果，直接返回
                    log.info("Idempotency cache hit: {}", cacheKey);
                    SseEmitter cachedEmitter = new SseEmitter();
                    try {
                        // 模拟流式输出：直接发送 token + done 事件
                        cachedEmitter.send(SseEmitter.event().name("token").data(cachedResult));
                        cachedEmitter.send(SseEmitter.event().name("done").data(cachedResult));
                        cachedEmitter.complete();
                    } catch (Exception ex) { /* ignore */ }
                    return cachedEmitter;
                }
            }
            // 标记为正在处理
            idempotencyCache.put(cacheKey, IdempotencyCache.PROCESSING_MARKER);
        }

        SseEmitter emitter = new SseEmitter(120000L);

        // Generate a sessionId for abort tracking
        String sessionId = UUID.randomUUID().toString();
        StreamManager.registerEmitter(sessionId, emitter);

        // Save context for async thread
        var context = org.springframework.security.core.context.SecurityContextHolder.getContext();

        String finalIdempotencyKey = idempotencyKey;
        String finalCacheKey = (idempotencyKey != null && !idempotencyKey.isBlank()) ? "stream:" + idempotencyKey : null;

        var future = java.util.concurrent.CompletableFuture.runAsync(() -> {
            org.springframework.security.core.context.SecurityContextHolder.setContext(context);
            try {
                // 收集完整内容用于缓存
                var contentHolder = new StringBuilder();
                openClawService.chatStream(request.getMessage(), null, emitter,
                    (fullContent, tokenUsage) -> {
                        try {
                            ChatMessage msg = new ChatMessage(userId, username,
                                request.getMessage(), fullContent, tokenUsage);
                            chatMessageRepository.save(msg);
                            contentHolder.append(fullContent);
                        } catch (Exception e) {
                            // Log but don't crash on save failure
                        }
                    }, sessionId);

                // 缓存完整结果
                if (finalCacheKey != null && contentHolder.length() > 0) {
                    idempotencyCache.put(finalCacheKey, contentHolder.toString());
                    log.info("Idempotency cache saved: {}", finalCacheKey);
                }
            } catch (Exception e) {
                try { emitter.send(SseEmitter.event().name("error").data(e.getMessage())); }
                catch (Exception ex) { /* ignore */ }
            } finally {
                StreamManager.unregister(sessionId);
            }
        });

        StreamManager.registerFuture(sessionId, future);
        return emitter;
    }

    @PostMapping("/stream/abort")
    @Operation(summary = "中止 SSE 流", description = "根据 sessionId 中止正在进行的 SSE 流")
    public ResponseEntity<?> abortStream(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId is required"));
        }
        boolean aborted = StreamManager.abort(sessionId);
        if (aborted) {
            return ResponseEntity.ok(Map.of("status", "aborted", "sessionId", sessionId));
        } else {
            return ResponseEntity.ok(Map.of("status", "not_found", "sessionId", sessionId));
        }
    }
}
