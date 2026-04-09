package org.tenny.util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Pretty-print and truncate for logs (avoid huge lines / log cost).
 */
public final class JsonLogging {

    public static final int DEFAULT_MAX_CHARS = 16_000;

    private JsonLogging() {
    }

    public static String prettyTruncated(ObjectMapper om, Object value) {
        if (value == null) {
            return "null";
        }
        try {
            String s = om.writerWithDefaultPrettyPrinter().writeValueAsString(value);
            return truncate(s, DEFAULT_MAX_CHARS);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    public static String truncate(String s, int max) {
        if (s == null) {
            return "null";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "\n... [truncated, total " + s.length() + " chars]";
    }
}
