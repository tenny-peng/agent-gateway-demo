package org.tenny.util;

/**
 * Pretty-print and truncate for logs (avoid huge lines / log cost).
 */
public final class TruncateUtil {

    public static final int DEFAULT_MAX_CHARS = 16_000;

    private TruncateUtil() {
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
