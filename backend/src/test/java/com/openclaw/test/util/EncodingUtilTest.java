package com.openclaw.test.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

class EncodingUtilTest {

    @Test
    void testDetectUtf8() {
        byte[] data = "Hello 世界".getBytes(StandardCharsets.UTF_8);
        String encoding = EncodingUtil.detectEncoding(data);
        assertEquals("UTF-8", encoding);
    }

    @Test
    void testDetectGbk() {
        byte[] gbkBytes = new byte[]{
            (byte)0xc4, (byte)0xe3, (byte)0xba, (byte)0xc3
        };
        String encoding = EncodingUtil.detectEncoding(gbkBytes);
        assertEquals("GBK", encoding);
    }

    @Test
    void testToUtf8Bytes() {
        byte[] gbkBytes = new byte[]{
            (byte)0xc4, (byte)0xe3, (byte)0xba, (byte)0xc3,
            0x48, 0x65, 0x6c, 0x6c, 0x6f
        };
        byte[] utf8 = EncodingUtil.toUtf8Bytes(gbkBytes);
        String result = new String(utf8, StandardCharsets.UTF_8);
        assertTrue(result.startsWith("你好"));
        assertTrue(result.contains("Hello"));
    }

    @Test
    void testEmptyData() {
        assertEquals("UTF-8", EncodingUtil.detectEncoding(new byte[0]));
        assertArrayEquals(new byte[0], EncodingUtil.toUtf8Bytes(new byte[0]));
    }

    @Test
    void testNullData() {
        assertEquals("UTF-8", EncodingUtil.detectEncoding(null));
    }
}
