package com.openclaw.test.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileParser {
    private static final Logger log = LoggerFactory.getLogger(FileParser.class);

    /**
     * Extract text from a Word (.docx) file bytes.
     * Returns null if parsing fails.
     */
    public static String extractDocxText(byte[] data) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(data));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText();
            return text == null ? "" : text.trim();
        } catch (Exception e) {
            log.warn("Failed to parse docx: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract text from an Excel (.xlsx) file bytes.
     * Returns null if parsing fails.
     */
    public static String extractXlsxText(byte[] data) {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                if (sheetName != null && !sheetName.isBlank()) {
                    sb.append("=== ").append(sheetName).append(" ===\n");
                }
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (Cell cell : row) {
                        cells.add(getCellValueAsString(cell));
                    }
                    sb.append(String.join("\t", cells)).append("\n");
                }
                sb.append("\n");
            }
            String text = sb.toString().trim();
            return text.isEmpty() ? null : text;
        } catch (Exception e) {
            log.warn("Failed to parse xlsx: {}", e.getMessage());
            return null;
        }
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception e2) {
                        yield cell.getCellFormula();
                    }
                }
            }
            case BLANK -> "";
            default -> "";
        };
    }

    /**
     * Extract text from a PDF file bytes.
     * Returns null if parsing fails.
     */
    public static String extractPdfText(byte[] data) {
        try (PDDocument doc = Loader.loadPDF(data)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            return text == null ? "" : text.trim();
        } catch (Exception e) {
            log.warn("Failed to parse pdf: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract text from a PowerPoint (.pptx) file bytes.
     * Returns null if parsing fails.
     */
    public static String extractPptxText(byte[] data) {
        try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(data))) {
            StringBuilder sb = new StringBuilder();
            int slideNum = 1;
            for (XSLFSlide slide : ppt.getSlides()) {
                sb.append("=== 幻灯片 ").append(slideNum++).append(" ===\n");
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        String text = ((XSLFTextShape) shape).getText();
                        if (text != null && !text.isBlank()) {
                            sb.append(text).append("\n");
                        }
                    }
                }
                sb.append("\n");
            }
            String text = sb.toString().trim();
            return text.isEmpty() ? null : text;
        } catch (Exception e) {
            log.warn("Failed to parse pptx: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract text from an old PowerPoint (.ppt) file bytes.
     * Returns null if parsing fails.
     */
    public static String extractPptText(byte[] data) {
        try (HSLFSlideShow ppt = new HSLFSlideShow(new ByteArrayInputStream(data))) {
            StringBuilder sb = new StringBuilder();
            int slideNum = 1;
            for (HSLFSlide slide : ppt.getSlides()) {
                sb.append("=== 幻灯片 ").append(slideNum++).append(" ===\n");
                for (List<HSLFTextParagraph> paras : slide.getTextParagraphs()) {
                    String text = HSLFTextParagraph.getRawText(paras);
                    if (text != null && !text.isBlank()) {
                        sb.append(text).append("\n");
                    }
                }
                sb.append("\n");
            }
            String text = sb.toString().trim();
            return text.isEmpty() ? null : text;
        } catch (Exception e) {
            log.warn("Failed to parse ppt: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract text from an old Word (.doc) file bytes.
     */
    public static String extractDocText(byte[] data) {
        try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(data));
             WordExtractor extractor = new WordExtractor(doc)) {
            String text = extractor.getText();
            return text == null ? "" : text.trim();
        } catch (Exception e) {
            log.warn("Failed to parse doc: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract text from an old Excel (.xls) file bytes.
     */
    public static String extractXlsText(byte[] data) {
        try (HSSFWorkbook workbook = new HSSFWorkbook(new ByteArrayInputStream(data))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                if (sheetName != null && !sheetName.isBlank()) {
                    sb.append("=== ").append(sheetName).append(" ===\n");
                }
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (Cell cell : row) {
                        cells.add(getCellValueAsString(cell));
                    }
                    sb.append(String.join("\t", cells)).append("\n");
                }
                sb.append("\n");
            }
            String text = sb.toString().trim();
            return text.isEmpty() ? null : text;
        } catch (Exception e) {
            log.warn("Failed to parse xls: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if a file extension is a supported Office/PDF format.
     */
    public static boolean isOfficeFile(String lowerName) {
        return lowerName.endsWith(".docx") || lowerName.endsWith(".doc") ||
               lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") ||
               lowerName.endsWith(".pdf") ||
               lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt");
    }
}
