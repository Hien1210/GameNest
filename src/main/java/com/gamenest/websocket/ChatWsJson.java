package com.gamenest.websocket;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal, hand-rolled JSON codec scoped to this project's flat Chat
 * WebSocket protocol (task spec §12/§13). No JSON library exists anywhere
 * in this project's dependency tree (verified: not in pom.xml, not
 * transitively via Cloudinary/Mail/mssql-jdbc), and the protocol never
 * needs nested objects/arrays — every event is a flat object of strings and
 * integers — so pulling in Jackson/Gson would be unjustified weight for
 * three fields.
 * <p>
 * {@link #parseFlatObject} tokenizes properly (quote/escape aware) instead
 * of substring-searching for {@code "key"} in the raw text, so a message
 * CONTENT that happens to contain literal text like
 * {@code "conversationId":999} can never be misread as a real top-level
 * key. Malformed input never throws — every parse failure returns
 * {@code null}, so the caller can always fail safely into an ERROR event
 * instead of the connection dying.
 */
final class ChatWsJson {

    private ChatWsJson() {
    }

    /** JSON-escapes a string for embedding as a quoted value in a hand-built payload. */
    static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(value.length() + 8);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Parses one flat JSON object (string/number/null values only — the
     * only shape this protocol's incoming client events ever use) into a
     * key→text map; numbers are kept as their literal digit text. Returns
     * {@code null} on any malformed input.
     */
    static Map<String, String> parseFlatObject(String json) {
        if (json == null) {
            return null;
        }
        int i = skipWhitespace(json, 0);
        if (i >= json.length() || json.charAt(i) != '{') {
            return null;
        }
        i++;
        Map<String, String> result = new LinkedHashMap<>();
        i = skipWhitespace(json, i);
        if (i < json.length() && json.charAt(i) == '}') {
            return result;
        }
        while (true) {
            i = skipWhitespace(json, i);
            if (i >= json.length() || json.charAt(i) != '"') {
                return null;
            }
            int[] end = new int[1];
            String key = parseString(json, i, end);
            if (key == null) {
                return null;
            }
            i = skipWhitespace(json, end[0]);
            if (i >= json.length() || json.charAt(i) != ':') {
                return null;
            }
            i = skipWhitespace(json, i + 1);
            if (i >= json.length()) {
                return null;
            }
            char c = json.charAt(i);
            String value;
            if (c == '"') {
                value = parseString(json, i, end);
                if (value == null) {
                    return null;
                }
            } else if (c == '-' || Character.isDigit(c)) {
                value = parseNumber(json, i, end);
                if (value == null) {
                    return null;
                }
            } else if (json.startsWith("null", i)) {
                value = null;
                end[0] = i + 4;
            } else {
                return null;
            }
            result.put(key, value);
            i = skipWhitespace(json, end[0]);
            if (i >= json.length()) {
                return null;
            }
            if (json.charAt(i) == ',') {
                i++;
                continue;
            }
            if (json.charAt(i) == '}') {
                return result;
            }
            return null;
        }
    }

    private static int skipWhitespace(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }

    /** startIdx points at the opening quote. Writes the index just past the closing quote into endOut[0]. */
    private static String parseString(String s, int startIdx, int[] endOut) {
        StringBuilder sb = new StringBuilder();
        int i = startIdx + 1;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '"') {
                endOut[0] = i + 1;
                return sb.toString();
            }
            if (c == '\\') {
                if (i + 1 >= s.length()) {
                    return null;
                }
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"':
                        sb.append('"');
                        i += 2;
                        continue;
                    case '\\':
                        sb.append('\\');
                        i += 2;
                        continue;
                    case '/':
                        sb.append('/');
                        i += 2;
                        continue;
                    case 'n':
                        sb.append('\n');
                        i += 2;
                        continue;
                    case 'r':
                        sb.append('\r');
                        i += 2;
                        continue;
                    case 't':
                        sb.append('\t');
                        i += 2;
                        continue;
                    case 'b':
                        sb.append('\b');
                        i += 2;
                        continue;
                    case 'f':
                        sb.append('\f');
                        i += 2;
                        continue;
                    case 'u':
                        if (i + 5 >= s.length()) {
                            return null;
                        }
                        try {
                            int code = Integer.parseInt(s.substring(i + 2, i + 6), 16);
                            sb.append((char) code);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                        i += 6;
                        continue;
                    default:
                        return null;
                }
            }
            if (c < 0x20) {
                return null; // raw control character inside a string is not valid JSON
            }
            sb.append(c);
            i++;
        }
        return null; // unterminated string
    }

    private static String parseNumber(String s, int startIdx, int[] endOut) {
        int i = startIdx;
        if (i < s.length() && s.charAt(i) == '-') {
            i++;
        }
        int digitsStart = i;
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
        }
        if (i == digitsStart) {
            return null;
        }
        // This protocol only ever carries integer ids — reject decimals/exponents rather than mis-parse them.
        if (i < s.length() && (s.charAt(i) == '.' || s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            return null;
        }
        endOut[0] = i;
        return s.substring(startIdx, i);
    }
}
