package checkout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Konfigurasi dari variabel lingkungan; berkas .env (opsional) hanya sebagai nilai bawaan. */
public final class Config {
    private final Map<String, String> values = new HashMap<>();

    public Config(Path envFile) {
        if (envFile != null && Files.isRegularFile(envFile)) {
            try {
                for (String line : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                    String t = line.trim();
                    if (t.isEmpty() || t.startsWith("#") || !t.contains("=")) {
                        continue;
                    }
                    int i = t.indexOf('=');
                    String k = t.substring(0, i).trim();
                    String v = t.substring(i + 1).trim();
                    if (v.length() >= 2 && ((v.startsWith("\"") && v.endsWith("\""))
                            || (v.startsWith("'") && v.endsWith("'")))) {
                        v = v.substring(1, v.length() - 1);
                    }
                    values.put(k, v);
                }
            } catch (IOException e) {
                System.err.println("Peringatan: gagal membaca " + envFile + ": " + e.getMessage());
            }
        }
    }

    public String get(String key, String fallback) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String v = values.get(key);
        return v == null || v.isBlank() ? fallback : v;
    }
}
