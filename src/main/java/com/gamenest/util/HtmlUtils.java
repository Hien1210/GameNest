package com.gamenest.util;

/**
 * Minimal HTML-escaping for rendering user-generated content (Question /
 * Answer title, content, username) safely in JSP scriptlets. Every UGC
 * field must be passed through this before being written into HTML.
 */
public final class HtmlUtils {

    private HtmlUtils() {
    }

    public static String escape(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
