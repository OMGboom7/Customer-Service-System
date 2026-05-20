package com.openclaw.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "聊天请求")
public class ChatRequest {
    @Schema(description = "用户消息", example = "你好，请介绍一下自己", required = true)
    private String message;

    @Schema(description = "对话历史（可选），格式为 [{\"role\":\"user/assistant\",\"content\":\"...\"}]")
    private String history;

    @Schema(description = "最大输出 Token 数", example = "500")
    private int maxTokens = 8192;

    @Schema(description = "幂等键（可选），用于防止重复发送。相同 key 的请求在 5 分钟内只会执行一次")
    private String idempotencyKey;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getHistory() { return history; }
    public void setHistory(String history) { this.history = history; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
