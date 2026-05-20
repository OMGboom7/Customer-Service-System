package com.openclaw.test.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.test.config.NginxProperties;
import com.openclaw.test.config.OpenClawProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class OpenClawService {
    private static final Logger log = LoggerFactory.getLogger(OpenClawService.class);

    private final OpenClawProperties gatewayProps;
    private final NginxProperties nginxProps;
    private final ObjectMapper mapper;

    public OpenClawService(OpenClawProperties gatewayProps, NginxProperties nginxProps) {
        this.gatewayProps = gatewayProps;
        this.nginxProps = nginxProps;
        this.mapper = new ObjectMapper();
    }

    public ChatResult chat(String message, String historyJson, int maxTokens) throws Exception {
        String messagesJson = buildMessages(message, historyJson);
        String body = """
            {"model":"%s","messages":%s,"max_tokens":%d}
            """.formatted(gatewayProps.getModel(), messagesJson, maxTokens);
        String response = post(body);
        return parseChatResponse(response);
    }

    public ChatResult analyzeFile(String filePath, String instruction) throws Exception {
        String prompt = "Read and analyze the file at %s.\nInstruction: %s\nPlease provide a detailed analysis."
            .formatted(filePath, instruction);
        return chat(prompt, null, gatewayProps.getMaxTokens());
    }

    public FileGenResult generateFile(String instruction, String fileName, String publicBaseUrl, String historyJson) throws Exception {
        String outputPath = nginxProps.getOutputDir() + "/" + fileName;
        String prompt = "%s\nGenerate the content only, without any extra explanation or markdown formatting.".formatted(instruction);
        ChatResult result = chat(prompt, historyJson, gatewayProps.getMaxTokens());
        
        // Save the AI's response content to the file
        Path filePath = Path.of(outputPath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, result.content(), java.nio.charset.StandardCharsets.UTF_8);
        
        long fileSize = Files.size(filePath);
        String downloadUrl = (publicBaseUrl != null && !publicBaseUrl.isBlank() ? publicBaseUrl : nginxProps.getBaseUrl()) + "/" + fileName;
        return new FileGenResult(result.content(), result.tokenUsage(), downloadUrl, fileSize);
    }

    public void chatStream(String message, String historyJson,
                           org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter,
                           java.util.function.BiConsumer<String, String> onComplete)
            throws Exception {
        chatStream(message, historyJson, emitter, onComplete, null);
    }

    public void chatStream(String message, String historyJson,
                           org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter,
                           java.util.function.BiConsumer<String, String> onComplete,
                           String streamKey)
            throws Exception {
        String messagesJson = buildMessages(message, historyJson);
        String body = """
            {"model":"%s","messages":%s,"max_tokens":%d,"stream":true}
            """.formatted(gatewayProps.getModel(), messagesJson, gatewayProps.getMaxTokens());

        var url = new URI(gatewayProps.getBaseUrl() + "/v1/chat/completions").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + gatewayProps.getToken());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(120000);
        conn.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        if (streamKey != null && !streamKey.isBlank()) {
            StreamManager.registerConnection(streamKey, conn);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            String errorBody = new String(conn.getErrorStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                .name("error").data("OpenClaw 返回错误 (" + responseCode + "): " + errorBody));
            emitter.complete();
            if (streamKey != null && !streamKey.isBlank()) {
                StreamManager.unregister(streamKey);
            }
            return;
        }
        
        java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));

        StringBuilder fullContent = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;
                    try {
                        JsonNode chunk = mapper.readTree(data);
                        JsonNode delta = chunk.path("choices").get(0).path("delta");
                        if (delta != null && delta.has("content")) {
                            String token = delta.get("content").asText();
                            fullContent.append(token);
                            emitter.send(
                                org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                    .name("token").data(token));
                        }
                    } catch (Exception e) {
                        // skip malformed chunk
                    }
                }
            }
            // Save to database BEFORE sending done event (so frontend can reload)
            if (onComplete != null) {
                onComplete.accept(fullContent.toString(), "");
            }
            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                .name("done").data(fullContent.toString()));
            emitter.complete();
        } catch (Exception e) {
            try {
                emitter.completeWithError(e);
            } catch (Exception ex) {
                // ignore
            }
        } finally {
            if (streamKey != null && !streamKey.isBlank()) {
                StreamManager.unregister(streamKey);
            }
        }
    }

    // --- Private helpers ---

    private String buildMessages(String message, String historyJson) {
        if (historyJson == null || historyJson.isBlank()) {
            return "[{\"role\":\"user\",\"content\":\"" + escape(message) + "\"}]";
        }
        String history = historyJson.trim();
        if (history.startsWith("[") && history.endsWith("]")) {
            history = history.substring(1, history.length() - 1);
        }
        return "[" + history + ",{\"role\":\"user\",\"content\":\"" + escape(message) + "\"}]";
    }

    private String post(String jsonBody) throws Exception {
        var url = new URI(gatewayProps.getBaseUrl() + "/v1/chat/completions").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + gatewayProps.getToken());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(120000);
        conn.getOutputStream().write(jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        int status = conn.getResponseCode();
        java.io.InputStream is = status >= 200 && status < 300 ?
            conn.getInputStream() : conn.getErrorStream();
        return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private ChatResult parseChatResponse(String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        String content = "";
        JsonNode choices = root.get("choices");
        if (choices != null && choices.size() > 0) {
            JsonNode msg = choices.get(0).get("message");
            if (msg != null && msg.get("content") != null) {
                content = msg.get("content").asText();
            }
        }
        String tokenUsage = "";
        JsonNode usage = root.get("usage");
        if (usage != null) {
            int prompt = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : 0;
            int completion = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : 0;
            tokenUsage = prompt + " in / " + completion + " out";
        }
        return new ChatResult(content, tokenUsage);
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public record ChatResult(String content, String tokenUsage) {}
    public record FileGenResult(String content, String tokenUsage, String downloadUrl, long fileSize) {}
}
