package com.openclaw.test.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class FileParserTest {

    @Test
    void testIsOfficeFile() {
        assertTrue(FileParser.isOfficeFile("report.docx"));
        assertTrue(FileParser.isOfficeFile("data.xlsx"));
        assertTrue(FileParser.isOfficeFile("slides.pptx"));
        assertTrue(FileParser.isOfficeFile("doc.pdf"));
        assertTrue(FileParser.isOfficeFile("old.doc"));
        assertTrue(FileParser.isOfficeFile("old.xls"));
        assertTrue(FileParser.isOfficeFile("old.ppt"));
        assertFalse(FileParser.isOfficeFile("readme.txt"));
        assertFalse(FileParser.isOfficeFile("image.png"));
    }

    @Test
    void testExtractDocxReturnsNullOnInvalidData() {
        String result = FileParser.extractDocxText(new byte[0]);
        assertTrue(result == null || result.isEmpty());
    }

    @Test
    void testExtractPdfReturnsNullOnInvalidData() {
        String result = FileParser.extractPdfText(new byte[0]);
        assertTrue(result == null || result.isEmpty());
    }

    @Test
    void testExtractPptxReturnsNullOnInvalidData() {
        String result = FileParser.extractPptxText(new byte[0]);
        assertTrue(result == null || result.isEmpty());
    }
}
