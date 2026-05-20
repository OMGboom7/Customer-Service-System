package com.openclaw.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "文件分析请求")
public class FileAnalyzeRequest {
    @Schema(description = "文件路径（服务器上的绝对路径）", example = "/home/node/.openclaw/nginx/html/orders.csv")
    private String filePath;

    @Schema(description = "分析要求", example = "统计订单总数、总金额、各状态数量")
    private String instruction;

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
}
