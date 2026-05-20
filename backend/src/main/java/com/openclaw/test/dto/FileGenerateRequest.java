package com.openclaw.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "文件生成请求")
public class FileGenerateRequest {
    @Schema(description = "生成要求", example = "根据 orders.csv 生成一份 HTML 报告")
    private String instruction;

    @Schema(description = "文件名", example = "report.html")
    private String fileName;

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
