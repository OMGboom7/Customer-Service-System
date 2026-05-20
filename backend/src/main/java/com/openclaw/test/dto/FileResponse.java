package com.openclaw.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "文件操作响应")
public class FileResponse {
    @Schema(description = "Agent 回复内容")
    private String reply;

    @Schema(description = "文件下载 URL")
    private String downloadUrl;

    @Schema(description = "文件大小（字节）")
    private long fileSize;

    @Schema(description = "Token 使用情况")
    private String tokenUsage;

    public FileResponse() {}

    public FileResponse(String reply, String downloadUrl, long fileSize, String tokenUsage) {
        this.reply = reply;
        this.downloadUrl = downloadUrl;
        this.fileSize = fileSize;
        this.tokenUsage = tokenUsage;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(String tokenUsage) { this.tokenUsage = tokenUsage; }
}
