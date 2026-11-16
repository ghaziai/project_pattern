package checkout.store;

import checkout.model.LogEntry;
import checkout.model.Order;
import checkout.model.Product;
import checkout.util.Json;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Penyimpanan Supabase melalui REST (PostgREST). Gunakan kunci service_role hanya di sisi server. */
public class SupabaseStore implements DataStore {
    private final String base;
    private final String key;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public SupabaseStore(String url, String key) {
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        this.base = u + "/rest/v1/";
        this.key = key.trim();
    }

    @Override
    public String name() {
        return "supabase";
    }

    /** Memeriksa koneksi dan mengisi data awal bila tabel masih kosong. */
    public void init() {
        List<Map<String, Object>> existing = select("products", "select=code&limit=1");
        if (existing.isEmpty()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Product p : SeedData.products()) {
                rows.add(p.toMap());
            }
            send("POST", "products", null, rows, "return=minimal");
        }
        List<Map<String, Object>> loyalty = select("loyalty",
                "select=customer&customer=eq." + enc(SeedData.CUSTOMER));
        if (loyalty.isEmpty()) {
            setPoints(SeedData.CUSTOMER, SeedData.INITIAL_POINTS);
        }
    }

    @Override
    public List<Product> products() {
        List<Product> out = new ArrayList<>();
        for (Map<String, Object> r : select("products", "select=*&order=code.asc")) {
            out.add(toProduct(r));
        }
        return out;
    }

    @Override
    public Optional<Product> product(String code) {
        List<Map<String, Object>> rows = select("products", "select=*&code=eq." + enc(code));
        return rows.isEmpty() ? Optional.empty() : Optional.of(toProduct(rows.get(0)));
    }

    @Override
    public void setStock(String code, int stock) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("stock", stock);
        send("PATCH", "products", "code=eq." + enc(code), body, "return=minimal");
    }

    @Override
    public void saveOrder(Order order) {
        send("POST", "orders", "on_conflict=id", order.toRow(),
                "resolution=merge-duplicates,return=minimal");
    }

    @Override
    public Optional<Order> order(String id) {
        List<Map<String, Object>> rows = select("orders", "select=*&id=eq." + enc(id));
        return rows.isEmpty() ? Optional.empty() : Optional.of(Order.fromRow(rows.get(0)));
    }

    @Override
    public List<Order> recentOrders(int limit) {
        List<Order> out = new ArrayList<>();
        for (Map<String, Object> r : select("orders", "select=*&order=created_at.desc&limit=" + limit)) {
            out.add(Order.fromRow(r));
        }
        return out;
    }

    @Override
    public long points(String customer) {
        List<Map<String, Object>> rows = select("loyalty", "select=points&customer=eq." + enc(customer));
        return rows.isEmpty() ? 0L : Order.num(rows.get(0).get("points"));
    }

    @Override
    public void setPoints(String customer, long points) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("customer", customer);
        row.put("points", points);
        send("POST", "loyalty", "on_conflict=customer", row,
                "resolution=merge-duplicates,return=minimal");
    }

    @Override
    public void appendLog(LogEntry entry) {
        send("POST", "transaction_log", null, entry.toRow(), "return=minimal");
    }

    @Override
    public List<LogEntry> recentLogs(int limit) {
        List<LogEntry> out = new ArrayList<>();
        for (Map<String, Object> r : select("transaction_log", "select=*&order=id.desc&limit=" + limit)) {
            out.add(LogEntry.fromRow(r));
        }
        return out;
    }

    private static Product toProduct(Map<String, Object> r) {
        return new Product(String.valueOf(r.get("code")), String.valueOf(r.get("name")),
                Order.num(r.get("price")), (int) Order.num(r.get("stock")));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> select(String table, String query) {
        HttpResponse<String> res = execute(request(table, query).GET().build());
        Object parsed = Json.parse(res.body());
        if (!(parsed instanceof List<?> list)) {
            throw new StoreException("Respons Supabase tidak berupa array (" + table + ")");
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : list) {
            out.add((Map<String, Object>) o);
        }
        return out;
    }

    private void send(String method, String table, String query, Object body, String prefer) {
        HttpRequest.Builder b = request(table, query)
                .header("Content-Type", "application/json")
                .header("Prefer", prefer)
                .method(method, HttpRequest.BodyPublishers.ofString(Json.write(body), StandardCharsets.UTF_8));
        execute(b.build());
    }

    private HttpRequest.Builder request(String table, String query) {
        String url = base + table + (query == null || query.isEmpty() ? "" : "?" + query);
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Accept", "application/json");
    }

    private HttpResponse<String> execute(HttpRequest req) {
        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                String body = res.body() == null ? "" : res.body();
                throw new StoreException("Supabase " + req.method() + " " + req.uri().getPath()
                        + " gagal (HTTP " + res.statusCode() + "): "
                        + (body.length() > 300 ? body.substring(0, 300) : body));
            }
            return res;
        } catch (IOException e) {
            throw new StoreException("Tidak dapat menghubungi Supabase: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StoreException("Permintaan ke Supabase terputus", e);
        }
    }
}
