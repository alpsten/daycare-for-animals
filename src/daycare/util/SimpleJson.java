package daycare.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SimpleJson {
    private SimpleJson() {
    }

    public static Map<String, Object> parseObject(String json) {
        Parser parser = new Parser(json);
        Object value = parser.parseValue();
        parser.ensureFullyConsumed();
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("JSON-roten måste vara ett objekt.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> object = (Map<String, Object>) value;
        return object;
    }

    public static List<Object> parseArray(String json) {
        Parser parser = new Parser(json);
        Object value = parser.parseValue();
        parser.ensureFullyConsumed();
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("JSON-roten måste vara en array.");
        }

        @SuppressWarnings("unchecked")
        List<Object> array = (List<Object>) value;
        return array;
    }

    public static String stringify(Object value) {
        StringBuilder json = new StringBuilder();
        writeValue(json, value);
        return json.toString();
    }

    private static void writeValue(StringBuilder json, Object value) {
        if (value == null) {
            json.append("null");
            return;
        }
        if (value instanceof String) {
            json.append("\"").append(escape((String) value)).append("\"");
            return;
        }
        if (value instanceof Boolean || value instanceof Number) {
            json.append(value);
            return;
        }
        if (value instanceof Map) {
            writeObject(json, (Map<?, ?>) value);
            return;
        }
        if (value instanceof Iterable) {
            writeArray(json, (Iterable<?>) value);
            return;
        }

        throw new IllegalArgumentException("Kan inte serialisera typ till JSON: " + value.getClass().getSimpleName());
    }

    private static void writeObject(StringBuilder json, Map<?, ?> object) {
        json.append("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : object.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            json.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":");
            writeValue(json, entry.getValue());
        }
        json.append("}");
    }

    private static void writeArray(StringBuilder json, Iterable<?> array) {
        json.append("[");
        boolean first = true;
        for (Object item : array) {
            if (!first) {
                json.append(",");
            }
            first = false;
            writeValue(json, item);
        }
        json.append("]");
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static final class Parser {
        private final String input;
        private int index = 0;

        private Parser(String input) {
            this.input = input;
        }

        private void ensureFullyConsumed() {
            skipWhitespace();
            if (index != input.length()) {
                throw new IllegalArgumentException("Oväntat innehåll efter JSON-värdet vid position " + index);
            }
        }

        private Object parseValue() {
            skipWhitespace();
            if (peek('{')) {
                return parseObject();
            }
            if (peek('[')) {
                return parseArray();
            }
            if (peek('"')) {
                return parseString();
            }
            if (startsWith("true")) {
                index += 4;
                return true;
            }
            if (startsWith("false")) {
                index += 5;
                return false;
            }
            if (startsWith("null")) {
                index += 4;
                return null;
            }
            throw new IllegalArgumentException("Ogiltigt JSON-värde vid position " + index);
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return object;
            }

            while (true) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                object.put(key, value);
                skipWhitespace();
                if (peek(',')) {
                    expect(',');
                    continue;
                }
                expect('}');
                return object;
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> array = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return array;
            }

            while (true) {
                array.add(parseValue());
                skipWhitespace();
                if (peek(',')) {
                    expect(',');
                    continue;
                }
                expect(']');
                return array;
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();

            while (index < input.length()) {
                char current = input.charAt(index++);
                if (current == '"') {
                    return value.toString();
                }
                if (current == '\\') {
                    value.append(parseEscapedCharacter());
                } else {
                    value.append(current);
                }
            }

            throw new IllegalArgumentException("Oavslutad JSON-sträng.");
        }

        private char parseEscapedCharacter() {
            if (index >= input.length()) {
                throw new IllegalArgumentException("Ogiltig escape-sekvens i JSON.");
            }

            char escaped = input.charAt(index++);
            switch (escaped) {
                case '"':
                    return '"';
                case '\\':
                    return '\\';
                case '/':
                    return '/';
                case 'b':
                    return '\b';
                case 'f':
                    return '\f';
                case 'n':
                    return '\n';
                case 'r':
                    return '\r';
                case 't':
                    return '\t';
                default:
                    throw new IllegalArgumentException("Ogiltig escape-sekvens i JSON: \\" + escaped);
            }
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private boolean peek(char expected) {
            skipWhitespace();
            return index < input.length() && input.charAt(index) == expected;
        }

        private boolean startsWith(String value) {
            skipWhitespace();
            return input.startsWith(value, index);
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= input.length() || input.charAt(index) != expected) {
                throw new IllegalArgumentException("Förväntade '" + expected + "' vid position " + index);
            }
            index++;
        }
    }
}
