package com.smartstock.util;

import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

public final class TextEncodingUtils {

    private static final char REPLACEMENT_CHAR = '\uFFFD';

    private TextEncodingUtils() {
    }

    public static String normalizeDisplayText(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.trim();
        if (!looksLikeMojibake(trimmed)) {
            return trimmed;
        }
        String repaired = new String(trimmed.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        if (countCjk(repaired) > countCjk(trimmed)) {
            return repaired;
        }
        return trimmed;
    }

    public static boolean hasCorruptedDisplayText(String value) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.indexOf(REPLACEMENT_CHAR) >= 0) {
            return true;
        }
        if (looksLikeMojibake(trimmed)) {
            return true;
        }
        int questionMarks = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            if (trimmed.charAt(i) == '?') {
                questionMarks++;
            }
        }
        return questionMarks >= 2;
    }

    private static boolean looksLikeMojibake(String value) {
        if (countCjk(value) > 0) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= 0x00C0 && ch <= 0x00FF) {
                return true;
            }
        }
        return false;
    }

    private static int countCjk(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(value.charAt(i));
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                count++;
            }
        }
        return count;
    }
}
