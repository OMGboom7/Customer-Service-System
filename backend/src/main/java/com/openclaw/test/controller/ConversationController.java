package com.openclaw.test.controller;

import com.openclaw.test.dto.ChatSendRequest;
import com.openclaw.test.entity.ChatMessage;
import com.openclaw.test.entity.Conversation;
import com.openclaw.test.repository.ChatMessageRepository;
import com.openclaw.test.repository.ConversationRepository;
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
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "对话管理", description = "对话 CRUD 与 SSE 流式接口")
public class ConversationController {

    private static final Logger log = LoggerFactory.getLogger(ConversationController.class);

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final OpenClawService openClawService;
    private final com.openclaw.test.config.OpenClawProperties gatewayProps;
    private final com.openclaw.test.config.NginxProperties nginxProps;
    private final IdempotencyCache idempotencyCache;

    // Track which conversations are currently streaming (concurrency protection)
    private static final ConcurrentHashMap<Long, Boolean> STREAMING_CONVERSATIONS = new ConcurrentHashMap<>();

    public ConversationController(ConversationRepository conversationRepository,
                                   ChatMessageRepository chatMessageRepository,
                                   OpenClawService openClawService,
                                   com.openclaw.test.config.OpenClawProperties gatewayProps,
                                   com.openclaw.test.config.NginxProperties nginxProps,
                                   IdempotencyCache idempotencyCache) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.openClawService = openClawService;
        this.gatewayProps = gatewayProps;
        this.nginxProps = nginxProps;
        this.idempotencyCache = idempotencyCache;
    }

    private Long getUserId(HttpServletRequest req) { return (Long) req.getAttribute("userId"); }

    @GetMapping
    @Operation(summary = "获取对话列表", description = "获取当前用户的所有对话")
    public ResponseEntity<?> list(HttpServletRequest req) {
        Long uid = getUserId(req);
        List<Conversation> list = conversationRepository.findByUserIdOrderByUpdatedAtDesc(uid);
        var result = list.stream().map(c -> Map.of(
            "id", c.getId(), "title", c.getTitle(),
            "createdAt", c.getCreatedAt().toString(), "updatedAt", c.getUpdatedAt().toString()
        )).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Operation(summary = "创建对话", description = "创建一个新对话")
    public ResponseEntity<?> create(@RequestBody Map<String, String> body, HttpServletRequest req) {
        Long uid = getUserId(req);
        String rawTitle = body.getOrDefault("title", "新对话");
        if (rawTitle.length() > 100) rawTitle = rawTitle.substring(0, 97) + "...";
        Conversation c = new Conversation(uid, rawTitle);
        c = conversationRepository.save(c);
        return ResponseEntity.ok(Map.of("id", c.getId(), "title", c.getTitle()));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "获取对话消息", description = "获取指定对话的所有消息")
    public ResponseEntity<?> messages(@PathVariable Long id, HttpServletRequest req) {
        Long uid = getUserId(req);
        var conv = conversationRepository.findById(id);
        if (conv.isEmpty() || !conv.get().getUserId().equals(uid))
            return ResponseEntity.status(403).body(Map.of("error", "无权访问"));
        List<ChatMessage> msgs = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(id);
        var result = msgs.stream().map(m -> {
            var map = new java.util.LinkedHashMap<String, Object>();
            map.put("id", m.getId());
            map.put("message", m.getMessage());
            map.put("reply", m.getReply());
            map.put("tokenUsage", m.getTokenUsage());
            map.put("createdAt", m.getCreatedAt().toString());
            if (m.getFileUrl() != null) map.put("fileUrl", m.getFileUrl());
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除对话", description = "删除指定对话及其所有消息")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest req) {
        Long uid = getUserId(req);
        var conv = conversationRepository.findById(id);
        if (conv.isEmpty() || !conv.get().getUserId().equals(uid))
            return ResponseEntity.status(403).body(Map.of("error", "无权删除"));
        // Delete all messages in this conversation
        List<ChatMessage> msgs = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(id);
        chatMessageRepository.deleteAll(msgs);
        conversationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    private String buildHistory(Long conversationId) {
        List<ChatMessage> history = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < history.size(); i++) {
            ChatMessage m = history.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"role\":\"user\",\"content\":\"")
              .append(escape(m.getMessage())).append("\"}");
            sb.append(",{\"role\":\"assistant\",\"content\":\"")
              .append(escape(m.getReply())).append("\"}");
        }
        sb.append("]");
        String result = sb.toString();
        return result.equals("[]") ? null : result;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "发送对话消息", description = "向指定对话发送消息并获取完整回复")
    public ResponseEntity<?> send(@PathVariable Long id, @RequestBody ChatSendRequest request,
                                   HttpServletRequest req) {
        Long uid = getUserId(req);
        var conv = conversationRepository.findById(id);
        if (conv.isEmpty() || !conv.get().getUserId().equals(uid))
            return ResponseEntity.status(403).body(Map.of("error", "无权访问"));
        if (request.getMessage() == null || request.getMessage().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "消息不能为空"));
        try {
            String history = buildHistory(id);
            var result = openClawService.chat(request.getMessage(), history, gatewayProps.getMaxTokens());
            ChatMessage msg = new ChatMessage(uid, conv.get().getTitle(),
                request.getMessage(), result.content(), result.tokenUsage(), id);
            msg = chatMessageRepository.save(msg);
            // Update conversation title from first message
            Conversation c = conv.get();
            if (c.getTitle().equals("新对话") || c.getTitle() == null) {
                c.setTitle(request.getMessage().length() > 30 ? request.getMessage().substring(0, 30) + "..." : request.getMessage());
                c.setUpdatedAt(java.time.LocalDateTime.now());
                conversationRepository.save(c);
            } else {
                c.setUpdatedAt(java.time.LocalDateTime.now());
                conversationRepository.save(c);
            }
            return ResponseEntity.ok(Map.of("id", msg.getId(), "message", msg.getMessage(),
                "reply", msg.getReply(), "tokenUsage", msg.getTokenUsage(), "createdAt", msg.getCreatedAt().toString()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "发送失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/stream")
    @Operation(summary = "SSE 流式对话", description = "通过 SSE 流式传输回复（支持中止与幂等保护）")
    public SseEmitter stream(@PathVariable Long id, @RequestBody ChatSendRequest request,
                              HttpServletRequest req) {
        Long uid = getUserId(req);

        // Concurrency protection: check if this conversation is already streaming
        Boolean alreadyStreaming = STREAMING_CONVERSATIONS.putIfAbsent(id, Boolean.TRUE);
        if (alreadyStreaming != null && alreadyStreaming) {
            log.warn("Conversation {} already streaming, rejecting new request", id);
            SseEmitter errorEmitter = new SseEmitter(0L);
            try {
                errorEmitter.send(SseEmitter.event().name("error").data("该对话正在生成回复中，请等待完成"));
            } catch (Exception ex) { /* ignore */ }
            errorEmitter.complete();
            return errorEmitter;
        }

        // ------ 幂等保护检查 ------
        String idempotencyKey = request.getIdempotencyKey();
        final String cacheKey;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            cacheKey = "conv:" + id + ":" + idempotencyKey;
            Object cached = idempotencyCache.get(cacheKey);
            if (cached != null) {
                if (cached == IdempotencyCache.PROCESSING_MARKER) {
                    // 正在处理中，返回 409
                    log.warn("Idempotency conflict (processing): {}", cacheKey);
                    STREAMING_CONVERSATIONS.remove(id);
                    SseEmitter conflictEmitter = new SseEmitter(0L);
                    try {
                        conflictEmitter.send(SseEmitter.event().name("error").data("该请求正在处理中，请勿重复发送"));
                    } catch (Exception ex) { /* ignore */ }
                    conflictEmitter.complete();
                    return conflictEmitter;
                } else if (cached instanceof String cachedResult) {
                    // 已有缓存结果，直接返回
                    log.info("Idempotency cache hit: {}", cacheKey);
                    STREAMING_CONVERSATIONS.remove(id);
                    SseEmitter cachedEmitter = new SseEmitter();
                    try {
                        cachedEmitter.send(SseEmitter.event().name("token").data(cachedResult));
                        cachedEmitter.send(SseEmitter.event().name("done").data(cachedResult));
                        cachedEmitter.complete();
                    } catch (Exception ex) { /* ignore */ }
                    return cachedEmitter;
                }
            }
            // 标记为正在处理
            idempotencyCache.put(cacheKey, IdempotencyCache.PROCESSING_MARKER);
        } else {
            cacheKey = null;
        }

        SseEmitter emitter = new SseEmitter(120000L);
        String streamKey = String.valueOf(id);
        StreamManager.registerEmitter(streamKey, emitter);

        var context = org.springframework.security.core.context.SecurityContextHolder.getContext();

        var future = java.util.concurrent.CompletableFuture.runAsync(() -> {
            org.springframework.security.core.context.SecurityContextHolder.setContext(context);
            try {
                // Save uploaded attachments to nginx dir
                var attachments = request.getAttachments();
                StringBuilder fileContext = new StringBuilder();
                if (attachments != null && !attachments.isEmpty()) {
                    String uploadDir = nginxProps.getOutputDir();
                    java.nio.file.Files.createDirectories(java.nio.file.Path.of(uploadDir));
                    for (int i = 0; i < attachments.size(); i++) {
                        var att = attachments.get(i);
                        String fileName = att.getOrDefault("fileName", "file_" + i);
                        String dataUrl = att.get("dataUrl");
                        if (dataUrl != null && dataUrl.startsWith("data:")) {
                            String base64Data = dataUrl.contains(",") ? dataUrl.substring(dataUrl.indexOf(',') + 1) : dataUrl;
                            try {
                                byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Data);
                                String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
                                String storedName = java.util.UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;
                                java.nio.file.Path targetPath = java.nio.file.Path.of(uploadDir, storedName);
                                java.nio.file.Files.write(targetPath, fileBytes);
                                String publicBase = nginxProps.getPublicBaseUrl();
                                if (publicBase == null || publicBase.isBlank()) publicBase = nginxProps.getBaseUrl();
                                String downloadUrl = nginxProps.getBaseUrl() + "/" + storedName;
                                String publicUrl = publicBase + "/" + storedName;
                                fileContext.append("\n附件").append(i+1).append(": ").append(fileName).append(" → ").append(publicUrl);
                            } catch (Exception ex) { /* ignore */ }
                        }
                    }
                }

                // Build prompt with file context
                String userMessage = request.getMessage();
                if (fileContext.length() > 0) {
                    userMessage = userMessage + "\n\n[已上传文件]" + fileContext.toString();
                }

                // Check if user wants to generate a file (not convert/format existing)
                String lowerMsg = request.getMessage().toLowerCase();
                boolean isConversion = lowerMsg.contains("转为") || lowerMsg.contains("转换") ||
                    lowerMsg.contains("改成") || lowerMsg.contains("转成") ||
                    lowerMsg.contains("convert") || lowerMsg.contains("transform") ||
                    lowerMsg.matches("^(转为|转成|改成|转换成|转化为).*");
                boolean shouldGenFile = !isConversion && (
                    lowerMsg.contains("报告") || lowerMsg.contains("文件") ||
                    lowerMsg.contains("生成") || lowerMsg.contains("创建") ||
                    lowerMsg.contains("导出") || lowerMsg.contains("保存") ||
                    lowerMsg.contains("report") || lowerMsg.contains("file") ||
                    lowerMsg.contains("generate") || lowerMsg.contains("save") ||
                    lowerMsg.contains("html") || lowerMsg.contains("csv") ||
                    lowerMsg.matches(".*\\.(html|htm|csv|json|xml|md|txt|pdf|docx?|xlsx?)\\s*$"));

                String finalDownloadUrl = null;

                if (shouldGenFile) {
                    // File generation flow (with conversation history)
                    String fileName = "chat_" + java.util.UUID.randomUUID().toString().substring(0, 8) + ".txt";
                    String publicBase = nginxProps.getPublicBaseUrl();
                    if (publicBase == null || publicBase.isBlank()) publicBase = nginxProps.getBaseUrl();
                    String genHistory = buildHistory(id);
                    var result = openClawService.generateFile(request.getMessage(), fileName, publicBase, genHistory);
                    finalDownloadUrl = result.downloadUrl();

                    String fullContent = result.content();
                    // Stream as tokens
                    int chunkSize = 50;
                    for (int i = 0; i < fullContent.length(); i += chunkSize) {
                        String chunk = fullContent.substring(i, Math.min(i + chunkSize, fullContent.length()));
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .name("token").data(chunk));
                        java.lang.Thread.sleep(10);
                    }
                    // done 事件包含下载链接
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("done").data(fullContent + "\n__FILE_DOWNLOAD__:" + finalDownloadUrl));
                    emitter.complete();

                    // Save to DB
                    ChatMessage msg = new ChatMessage(uid,
                        conversationRepository.findById(id).map(Conversation::getTitle).orElse("新对话"),
                        request.getMessage(), fullContent, result.tokenUsage(), id, finalDownloadUrl);
                    chatMessageRepository.save(msg);
                    conversationRepository.findById(id).ifPresent(c -> {
                        if (c.getTitle().equals("新对话") || c.getTitle() == null) {
                            String t = request.getMessage();
                            c.setTitle(t.length() > 30 ? t.substring(0, 30) + "..." : t);
                        }
                        c.setUpdatedAt(java.time.LocalDateTime.now());
                        conversationRepository.save(c);
                    });

                    // Cache for idempotency
                    if (cacheKey != null && fullContent != null) {
                        idempotencyCache.put(cacheKey, fullContent);
                        log.info("Idempotency cache saved: {}", cacheKey);
                    }
                } else {
                    // Normal chat stream
                    String history = buildHistory(id);
                    openClawService.chatStream(userMessage, history, emitter,
                        (fullContent, tokenUsage) -> {
                            try {
                                ChatMessage msg = new ChatMessage(uid,
                                    conversationRepository.findById(id).map(Conversation::getTitle).orElse("新对话"),
                                    request.getMessage(), fullContent, tokenUsage, id);
                                chatMessageRepository.save(msg);
                                conversationRepository.findById(id).ifPresent(c -> {
                                    if (c.getTitle().equals("新对话") || c.getTitle() == null) {
                                        String t = request.getMessage();
                                        c.setTitle(t.length() > 30 ? t.substring(0, 30) + "..." : t);
                                    }
                                    c.setUpdatedAt(java.time.LocalDateTime.now());
                                    conversationRepository.save(c);
                                });

                                // Cache for idempotency
                                if (cacheKey != null && fullContent != null) {
                                    idempotencyCache.put(cacheKey, fullContent);
                                    log.info("Idempotency cache saved: {}", cacheKey);
                                }
                            } catch (Exception e) { /* ignore save error */ }
                        }, streamKey);
                }
            } catch (Exception e) {
                log.error("Stream error for conversation {}: {}", id, e.getMessage());
                try { emitter.send(SseEmitter.event().name("error").data(e.getMessage())); }
                catch (Exception ex) { /* ignore */ }
            } finally {
                STREAMING_CONVERSATIONS.remove(id);
                StreamManager.unregister(streamKey);
            }
        });

        StreamManager.registerFuture(streamKey, future);
        return emitter;
    }

    @PostMapping("/{id}/stream/abort")
    @Operation(summary = "中止 SSE 流", description = "中止指定对话正在进行的 SSE 流")
    public ResponseEntity<?> abortStream(@PathVariable Long id) {
        String streamKey = String.valueOf(id);
        log.info("Abort request for conversation {}", id);
        boolean aborted = StreamManager.abort(streamKey);
        STREAMING_CONVERSATIONS.remove(id);
        if (aborted) {
            return ResponseEntity.ok(Map.of("status", "aborted", "conversationId", id));
        } else {
            return ResponseEntity.ok(Map.of("status", "not_found", "conversationId", id));
        }
    }

    // ========== Task A: Steer ==========

    @PostMapping("/{id}/steer")
    @Operation(summary = "转向注入", description = "断开旧流，用新消息开新流并返回新 SseEmitter")
    public SseEmitter steer(@PathVariable Long id, @RequestBody Map<String, String> body,
                             HttpServletRequest req) {
        Long uid = getUserId(req);
        var conv = conversationRepository.findById(id);
        if (conv.isEmpty() || !conv.get().getUserId().equals(uid)) {
            SseEmitter err = new SseEmitter(0L);
            try { err.send(SseEmitter.event().name("error").data("无权访问")); } catch (Exception ignored) {}
            err.complete();
            return err;
        }

        String newMessage = body.get("message");
        if (newMessage == null || newMessage.isBlank()) {
            SseEmitter err = new SseEmitter(0L);
            try { err.send(SseEmitter.event().name("error").data("消息不能为空")); } catch (Exception ignored) {}
            err.complete();
            return err;
        }

        String streamKey = String.valueOf(id);
        SseEmitter newEmitter = new SseEmitter(120000L);
        var context = org.springframework.security.core.context.SecurityContextHolder.getContext();

        var newFuture = java.util.concurrent.CompletableFuture.runAsync(() -> {
            org.springframework.security.core.context.SecurityContextHolder.setContext(context);
            try {
                String history = buildHistory(id);
                openClawService.chatStream(newMessage, history, newEmitter,
                    (fullContent, tokenUsage) -> {
                        try {
                            ChatMessage msg = new ChatMessage(uid,
                                conversationRepository.findById(id).map(Conversation::getTitle).orElse("新对话"),
                                newMessage, fullContent, tokenUsage, id);
                            chatMessageRepository.save(msg);
                            conversationRepository.findById(id).ifPresent(c -> {
                                if (c.getTitle().equals("新对话") || c.getTitle() == null) {
                                    String t = newMessage;
                                    c.setTitle(t.length() > 30 ? t.substring(0, 30) + "..." : t);
                                }
                                c.setUpdatedAt(java.time.LocalDateTime.now());
                                conversationRepository.save(c);
                            });
                        } catch (Exception e) { /* ignore save error */ }
                    }, streamKey);
            } catch (Exception e) {
                try { newEmitter.send(SseEmitter.event().name("error").data(e.getMessage())); }
                catch (Exception ex) { /* ignore */ }
            } finally {
                STREAMING_CONVERSATIONS.remove(id);
                StreamManager.unregister(streamKey);
            }
        });

        // 原子替换: 断开旧流 → 注册新流
        StreamManager.replace(streamKey, newEmitter, null, newFuture);
        STREAMING_CONVERSATIONS.put(id, Boolean.TRUE);
        log.info("Steer: new stream started for conversation {}", id);

        return newEmitter;
    }

    @GetMapping("/{id}/queue-status")
    @Operation(summary = "检查队列状态", description = "检查指定对话是否正在流式传输")
    public ResponseEntity<?> queueStatus(@PathVariable Long id, HttpServletRequest req) {
        Long uid = getUserId(req);
        var conv = conversationRepository.findById(id);
        if (conv.isEmpty() || !conv.get().getUserId().equals(uid))
            return ResponseEntity.status(403).body(Map.of("error", "无权访问"));
        boolean isStreaming = STREAMING_CONVERSATIONS.getOrDefault(id, false);
        return ResponseEntity.ok(Map.of("isStreaming", isStreaming));
    }

    // ========== Task B: Context Usage API ==========

    @GetMapping("/{id}/context-usage")
    @Operation(summary = "上下文用量", description = "估算该对话已使用的 token 数量 (粗略: message.length/2 + reply.length/2)")
    public ResponseEntity<?> contextUsage(@PathVariable Long id, HttpServletRequest req) {
        Long uid = getUserId(req);
        var conv = conversationRepository.findById(id);
        if (conv.isEmpty() || !conv.get().getUserId().equals(uid))
            return ResponseEntity.status(403).body(Map.of("error", "无权访问"));
        List<ChatMessage> msgs = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(id);
        int usedTokens = 0;
        for (ChatMessage m : msgs) {
            if (m.getMessage() != null) usedTokens += m.getMessage().length() / 2;
            if (m.getReply() != null) usedTokens += m.getReply().length() / 2;
        }
        int maxTokens = gatewayProps.getMaxTokens();
        return ResponseEntity.ok(Map.of("usedTokens", usedTokens, "maxTokens", maxTokens));
    }
}
