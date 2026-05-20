package com.openclaw.test.controller;

import com.openclaw.test.config.NginxProperties;
import com.openclaw.test.dto.*;
import com.openclaw.test.service.OpenClawService;
import com.openclaw.test.util.EncodingUtil;
import com.openclaw.test.util.FileParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "OpenClaw 集成接口", description = "与 OpenClaw Agent 交互的 REST API")
public class OpenClawController {

    private final OpenClawService service;
    private final NginxProperties nginxProps;

    public OpenClawController(OpenClawService service, NginxProperties nginxProps) {
        this.service = service;
        this.nginxProps = nginxProps;
    }

    @PostMapping("/chat")
    @Operation(summary = "发送对话", description = "向 OpenClaw Agent 发送消息并获取回复")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            var result = service.chat(request.getMessage(), request.getHistory(), request.getMaxTokens());
            return ResponseEntity.ok(new ChatResponse(result.content(), result.tokenUsage(), 200));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ChatResponse("Error: " + e.getMessage(), "", 500));
        }
    }

    @PostMapping("/file/analyze")
    @Operation(summary = "分析文件", description = "让 Agent 读取并分析服务器上的文件（CSV、TXT 等）")
    public ResponseEntity<ChatResponse> analyzeFile(@RequestBody FileAnalyzeRequest request) {
        try {
            var result = service.analyzeFile(request.getFilePath(), request.getInstruction());
            return ResponseEntity.ok(new ChatResponse(result.content(), result.tokenUsage(), 200));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ChatResponse("Error: " + e.getMessage(), "", 500));
        }
    }

    @PostMapping("/file/generate")
    @Operation(summary = "生成文件", description = "让 Agent 生成文件并保存到 nginx 目录，返回下载链接")
    public ResponseEntity<FileResponse> generateFile(@RequestBody FileGenerateRequest request) {
        try {
            var result = service.generateFile(request.getInstruction(), request.getFileName(), nginxProps.getPublicBaseUrl(), null);
            return ResponseEntity.ok(new FileResponse(
                result.content(), result.downloadUrl(), result.fileSize(), result.tokenUsage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new FileResponse("Error: " + e.getMessage(), null, 0, ""));
        }
    }

    @PostMapping("/file/upload-base64")
    @Operation(summary = "上传文件(base64)", description = "通过base64编码上传文件，绕过nginx multipart限制")
    public ResponseEntity<?> uploadFileBase64(@RequestBody Map<String, String> body) {
        try {
            String uploadDir = nginxProps.getOutputDir();
            Files.createDirectories(Path.of(uploadDir));

            String originalName = body.getOrDefault("fileName", "file");
            String base64Data = body.get("fileData");
            if (base64Data == null || base64Data.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "fileData is required"));
            }

            byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Data);
            String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (safeName.isBlank() || safeName.equals(".")) {
                safeName = "file";
            }
            String storedName = UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;
            Path targetPath = Path.of(uploadDir, storedName);
            Files.write(targetPath, fileBytes);

            long fileSize = Files.size(targetPath);
            String downloadUrl = nginxProps.getBaseUrl() + "/" + storedName;

            return ResponseEntity.ok(Map.of(
                "fileName", storedName,
                "originalName", originalName,
                "fileSize", fileSize,
                "downloadUrl", downloadUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "上传失败: " + e.getMessage()));
        }
    }

    @PostMapping("/file/upload")
    @Operation(summary = "上传文件", description = "上传文件到服务器并返回下载链接")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String uploadDir = nginxProps.getOutputDir();
            Files.createDirectories(Path.of(uploadDir));
            
            // Preserve original filename with UUID to avoid conflicts
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                originalName = "file";
            }
            // Sanitize: keep only ASCII alphanumeric, dot, dash, underscore
            String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (safeName.isBlank() || safeName.equals(".")) {
                safeName = "file" + (originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "");
                safeName = safeName.replaceAll("[^a-zA-Z0-9._-]", "_");
                if (safeName.isBlank()) safeName = "file";
            }
            String storedName = UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;
            Path targetPath = Path.of(uploadDir, storedName);
            
            // Auto-detect encoding and convert text files to UTF-8
            String lowerName = originalName.toLowerCase();
            boolean isTextFile = lowerName.endsWith(".txt") || lowerName.endsWith(".csv") ||
                lowerName.endsWith(".json") || lowerName.endsWith(".xml") ||
                lowerName.endsWith(".md") || lowerName.endsWith(".log") ||
                lowerName.endsWith(".yaml") || lowerName.endsWith(".yml") ||
                lowerName.endsWith(".ini") || lowerName.endsWith(".cfg") ||
                lowerName.endsWith(".html") || lowerName.endsWith(".htm") ||
                lowerName.endsWith(".js") || lowerName.endsWith(".ts") ||
                lowerName.endsWith(".css") || lowerName.endsWith(".sql") ||
                lowerName.endsWith(".sh") || lowerName.endsWith(".bat") ||
                lowerName.endsWith(".properties") || lowerName.endsWith(".env");
            
            boolean isOffice = com.openclaw.test.util.FileParser.isOfficeFile(lowerName);
            String textDownloadUrl = null;
            long fileSize = 0;
            
            if (isTextFile) {
                byte[] originalBytes = file.getBytes();
                byte[] utf8Bytes = EncodingUtil.toUtf8Bytes(originalBytes);
                Files.write(targetPath, utf8Bytes);
                fileSize = Files.size(targetPath);
            } else if (isOffice) {
                // Save original file
                byte[] originalBytes = file.getBytes();
                Files.write(targetPath, originalBytes);
                fileSize = Files.size(targetPath);
                
                // Extract text and save as .txt
                String extractedText;
                if (lowerName.endsWith(".pdf")) {
                    extractedText = FileParser.extractPdfText(originalBytes);
                } else if (lowerName.endsWith(".docx")) {
                    extractedText = FileParser.extractDocxText(originalBytes);
                } else if (lowerName.endsWith(".doc")) {
                    extractedText = FileParser.extractDocText(originalBytes);
                } else if (lowerName.endsWith(".pptx")) {
                    extractedText = FileParser.extractPptxText(originalBytes);
                } else if (lowerName.endsWith(".ppt")) {
                    extractedText = FileParser.extractPptText(originalBytes);
                } else if (lowerName.endsWith(".xlsx")) {
                    extractedText = FileParser.extractXlsxText(originalBytes);
                } else {
                    extractedText = FileParser.extractXlsText(originalBytes);
                }
                
                if (extractedText != null && !extractedText.isBlank()) {
                    String textStoredName = storedName + ".txt";
                    Path textPath = Path.of(uploadDir, textStoredName);
                    Files.writeString(textPath, extractedText, java.nio.charset.StandardCharsets.UTF_8);
                    textDownloadUrl = nginxProps.getBaseUrl() + "/" + textStoredName;
                }
            } else {
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                fileSize = Files.size(targetPath);
            }
            
            String downloadUrl = nginxProps.getBaseUrl() + "/" + storedName;
            
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("fileName", storedName);
            result.put("originalName", originalName);
            result.put("fileSize", fileSize);
            result.put("downloadUrl", downloadUrl);
            if (textDownloadUrl != null) {
                result.put("textDownloadUrl", textDownloadUrl);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "上传失败: " + e.getMessage()));
        }
    }

    @GetMapping("/file/download/{fileName}")
    @Operation(summary = "下载文件", description = "从 nginx 下载 Agent 生成的文件")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "文件名", example = "report.html")
            @PathVariable String fileName) {
        try {
            Path filePath = Path.of("/home/node/.openclaw/nginx/html", fileName);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + fileName + "\"")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
