package com.openclaw.test.util;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public class EncodingUtil {

    private static final Charset[] COMMON_CHINESE = {
        StandardCharsets.UTF_8,
        Charset.forName("GBK"),
        Charset.forName("GB2312"),
        Charset.forName("ISO-8859-1"),
    };

    /**
     * Detect encoding of byte array by trying common charsets.
     * Returns UTF-8 as fallback.
     */
    public static String detectEncoding(byte[] data) {
        if (data == null || data.length == 0) return "UTF-8";

        // Check BOM first
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            return "UTF-8";
        }
        if (data.length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xFE) {
            return "UTF-16LE";
        }
        if (data.length >= 2 && (data[0] & 0xFF) == 0xFE && (data[1] & 0xFF) == 0xFF) {
            return "UTF-16BE";
        }

        for (Charset charset : COMMON_CHINESE) {
            if (isValidEncoding(data, charset)) {
                return charset.name();
            }
        }
        return "UTF-8";
    }

    /**
     * Convert bytes to UTF-8 string, auto-detecting source encoding.
     */
    public static String toUtf8String(byte[] data) {
        String encoding = detectEncoding(data);
        try {
            return new String(data, encoding);
        } catch (Exception e) {
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    /**
     * Convert bytes to UTF-8 bytes, auto-detecting source encoding.
     */
    public static byte[] toUtf8Bytes(byte[] data) {
        return toUtf8String(data).getBytes(StandardCharsets.UTF_8);
    }

    private static boolean isValidEncoding(byte[] data, Charset charset) {
        try {
            CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
            decoder.decode(ByteBuffer.wrap(data));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }
}
