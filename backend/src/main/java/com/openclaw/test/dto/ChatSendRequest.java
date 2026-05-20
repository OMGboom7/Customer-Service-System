package com.openclaw.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "聊天发送请求")
public class ChatSendRequest {
    @Schema(description = "用户消息", example = "你好", required = true)
    private String message;

    @Schema(description = "附件列表（可选）")
    private List<Map<String, String>> attachments;

    @Schema(description = "幂等键（可选），用于防止重复发送。相同 key 的请求在 5 分钟内只会执行一次")
    private String idempotencyKey;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<Map<String, String>> getAttachments() { return attachments; }
    public void setAttachments(List<Map<String, String>> attachments) { this.attachments = attachments; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
