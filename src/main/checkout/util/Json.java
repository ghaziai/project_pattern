package checkout.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parser dan penulis JSON minimal (tanpa dependensi eksternal). */
public final class Json {
    private static final int MAX_DEPTH = 64;

    private Json() {
    }

    public static Object parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("JSON kosong");
        }
        Parser p = new Parser(text);
        p.skipWs();
        Object v = p.value(0);
        p.skipWs();
        if (!p.end()) {
            throw p.error("Karakter tambahan setelah JSON");
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object o) {
        if (o instanceof Map) {
            return (Map<String, Object>) o;
        }
        throw new IllegalArgumentException("Body harus berupa objek JSON");
    }

    public static String write(Object o) {
        StringBuilder sb = new StringBuilder();
        write(sb, o);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object o) {
        if (o == null) {
            sb.append("null");
        } else if (o instanceof String s) {
            quote(sb, s);
        } else if (o instanceof Boolean b) {
            sb.append(b.booleanValue());
        } else if (o instanceof Double || o instanceof Float) {
            double d = ((Number) o).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                sb.append("null");
            } else {
                sb.append(d);
            }
        } else if (o instanceof Number n) {
            sb.append(n.toString());
        } else if (o instanceof Map<?, ?> m) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                quote(sb, String.valueOf(e.getKey()));
                sb.append(':');
                write(sb, e.getValue());
            }
            sb.append('}');
        } else if (o instanceof Iterable<?> it) {
            sb.append('[');
            boolean first = true;
            for (Object x : it) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(sb, x);
            }
            sb.append(']');
        } else {
            quote(sb, String.valueOf(o));
        }
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    private static final class Parser {
        private final String s;
        private int i = 0;

        Parser(String s) {
            this.s = s;
        }

        boolean end() {
            return i >= s.length();
        }

        IllegalArgumentException error(String msg) {
            return new IllegalArgumentException("JSON tidak valid: " + msg + " (posisi " + i + ")");
        }

        void skipWs() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    i++;
                } else {
                    break;
                }
            }
        }

        Object value(int depth) {
            if (depth > MAX_DEPTH) {
                throw error("terlalu dalam");
            }
            if (end()) {
                throw error("data berakhir tiba-tiba");
            }
            char c = s.charAt(i);
            if (c == '{') {
                return object(depth);
            }
            if (c == '[') {
                return array(depth);
            }
            if (c == '"') {
                return string();
            }
            if (s.startsWith("true", i)) {
                i += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", i)) {
                i += 5;
                return Boolean.FALSE;
            }
            if (s.startsWith("null", i)) {
                i += 4;
                return null;
            }
            if (c == '-' || (c >= '0' && c <= '9')) {
                return number();
            }
            throw error("karakter tak terduga '" + c + "'");
        }

        Map<String, Object> object(int depth) {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            skipWs();
            if (!end() && s.charAt(i) == '}') {
                i++;
                return m;
            }
            while (true) {
                skipWs();
                if (end() || s.charAt(i) != '"') {
                    throw error("kunci objek harus berupa string");
                }
                String key = string();
                skipWs();
                if (end() || s.charAt(i) != ':') {
                    throw error("':' diharapkan");
                }
                i++;
                skipWs();
                m.put(key, value(depth + 1));
                skipWs();
                if (end()) {
                    throw error("objek tidak ditutup");
                }
                char c = s.charAt(i++);
                if (c == '}') {
                    return m;
                }
                if (c != ',') {
                    throw error("',' atau '}' diharapkan");
                }
            }
        }

        List<Object> array(int depth) {
            List<Object> l = new ArrayList<>();
            i++;
            skipWs();
            if (!end() && s.charAt(i) == ']') {
                i++;
                return l;
            }
            while (true) {
                skipWs();
                l.add(value(depth + 1));
                skipWs();
                if (end()) {
                    throw error("array tidak ditutup");
                }
                char c = s.charAt(i++);
                if (c == ']') {
                    return l;
                }
                if (c != ',') {
                    throw error("',' atau ']' diharapkan");
                }
            }
        }

        String string() {
            StringBuilder sb = new StringBuilder();
            i++;
            while (true) {
                if (end()) {
                    throw error("string tidak ditutup");
                }
                char c = s.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c < 0x20) {
                    throw error("karakter kontrol dalam string");
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (end()) {
                    throw error("escape tidak lengkap");
                }
                char e = s.charAt(i++);
                switch (e) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (i + 4 > s.length()) {
                            throw error("escape unicode tidak lengkap");
                        }
                        try {
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw error("escape unicode tidak valid");
                        }
                        i += 4;
                    }
                    default -> throw error("escape tidak dikenal");
                }
            }
        }

        Object number() {
            int start = i;
            if (s.charAt(i) == '-') {
                i++;
            }
            int digitsStart = i;
            while (!end() && Character.isDigit(s.charAt(i))) {
                i++;
            }
            if (i == digitsStart) {
                throw error("angka tidak valid");
            }
            if (s.charAt(digitsStart) == '0' && i - digitsStart > 1) {
                throw error("angka tidak boleh diawali nol");
            }
            boolean fractional = false;
            if (!end() && s.charAt(i) == '.') {
                fractional = true;
                i++;
                int f = i;
                while (!end() && Character.isDigit(s.charAt(i))) {
                    i++;
                }
                if (i == f) {
                    throw error("pecahan tidak valid");
                }
            }
            if (!end() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                fractional = true;
                i++;
                if (!end() && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                    i++;
                }
                int ex = i;
                while (!end() && Character.isDigit(s.charAt(i))) {
                    i++;
                }
                if (i == ex) {
                    throw error("eksponen tidak valid");
                }
            }
            String num = s.substring(start, i);
            if (!fractional) {
                try {
                    return Long.parseLong(num);
                } catch (NumberFormatException ex) {
                    return Double.parseDouble(num);
                }
            }
            return Double.parseDouble(num);
        }
    }
}
