package com.openclaw.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "聊天响应")
public class ChatResponse {
    @Schema(description = "Agent 回复内容")
    private String reply;

    @Schema(description = "Token 使用情况")
    private String tokenUsage;

    @Schema(description = "HTTP 状态码")
    private int status;

    public ChatResponse() {}

    public ChatResponse(String reply, String tokenUsage, int status) {
        this.reply = reply;
        this.tokenUsage = tokenUsage;
        this.status = status;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public String getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(String tokenUsage) { this.tokenUsage = tokenUsage; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
