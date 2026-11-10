package checkout;

import checkout.facade.ApiException;
import checkout.facade.AppContext;
import checkout.facade.CheckoutFacade;
import checkout.model.Order;
import checkout.model.OrderRequest;
import checkout.model.OrderStatus;
import checkout.model.Quote;
import checkout.payment.PaymentGateways;
import checkout.singleton.TransactionLog;
import checkout.store.DataStore;
import checkout.store.InMemoryStore;
import checkout.store.StoreException;
import checkout.store.SupabaseStore;
import checkout.util.Json;
import checkout.web.HttpApi;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Pengujian mandiri tanpa framework: java -cp out:out-test:web checkout.SelfTest */
public class SelfTest {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("  [OK]   " + name); }
        else { failed++; System.out.println("  [GAGAL] " + name); }
    }

    static OrderRequest req(String json) {
        return OrderRequest.fromJson(Json.parse(json));
    }

    static String body(String method, String voucher, long points, String... items) {
        StringBuilder sb = new StringBuilder("{\"items\":[");
        for (int i = 0; i < items.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append("{\"code\":\"").append(items[i]).append("\",\"qty\":").append(items[i + 1]).append('}');
        }
        sb.append("],\"method\":\"").append(method).append("\",\"voucher\":\"").append(voucher)
          .append("\",\"points\":").append(points).append('}');
        return sb.toString();
    }

    static AppContext fresh() {
        return new AppContext(new InMemoryStore());
    }

    public static void main(String[] args) throws Exception {
        testJson();
        testPayments();
        testMediator();
        testFacadeFlows();
        testConcurrency();
        testObserverIsolation();
        testSupabaseMock();
        testHttp();
        System.out.println("\nHasil: " + passed + " lulus, " + failed + " gagal");
        System.exit(failed == 0 ? 0 : 1);
    }

    static void testJson() {
        System.out.println("# JSON");
        Object o = Json.parse("{\"a\":[1,2.5,\"x\\n\\u00e9\",true,null],\"b\":{\"c\":-3}}");
        check("parse-write bolak-balik", Json.write(o).equals(Json.write(Json.parse(Json.write(o)))));
        boolean bad = false;
        try { Json.parse("{\"a\":"); } catch (RuntimeException e) { bad = true; }
        check("JSON rusak ditolak", bad);
        bad = false;
        try { Json.parse("[".repeat(500)); } catch (RuntimeException e) { bad = true; }
        check("kedalaman berlebih ditolak", bad);
    }

    static void testPayments() {
        System.out.println("# Factory Method / Template Method / Adapter");
        check("fee QRIS 0,7% dari 75.000 = 525", PaymentGateways.forMethod("QRIS").estimateFee(75_000) == 525);
        check("fee VA flat 4.000", PaymentGateways.forMethod("VA").estimateFee(75_000) == 4_000);
        check("fee e-wallet 1,5% dari 100.000 = 1.500", PaymentGateways.forMethod("EWALLET").estimateFee(100_000) == 1_500);
        check("fee bank flat 2.500", PaymentGateways.forMethod("BANK").estimateFee(1_000) == 2_500);
        check("QRIS sukses", PaymentGateways.forMethod("QRIS").charge(50_000, "T1").success());
        check("e-wallet gagal > 1 juta", !PaymentGateways.forMethod("EWALLET").charge(1_000_001, "T2").success());
        check("bank (adapter) sukses", PaymentGateways.forMethod("BANK").charge(500_000, "T3").success());
        check("bank melewati limit gagal", !PaymentGateways.forMethod("BANK").charge(10_000_001, "T4").success());
        check("nominal nol ditolak template", !PaymentGateways.forMethod("QRIS").charge(0, "T5").success());
        boolean unknown = false;
        try { PaymentGateways.forMethod("XYZ"); } catch (RuntimeException e) { unknown = true; }
        check("metode tidak dikenal ditolak", unknown);
        check("4 metode tersedia", PaymentGateways.available().size() == 4);
    }

    static void testMediator() {
        System.out.println("# Mediator (quote)");
        AppContext c = fresh();
        Quote q = c.facade.quote(req(body("QRIS", "", 0, "SKU-001", "2")));
        check("subtotal 150.000 + fee 1.050 = 151.050", q.valid() && q.subtotal() == 150_000 && q.fee() == 1_050 && q.total() == 151_050);
        q = c.facade.quote(req(body("QRIS", "HEMAT10", 0, "SKU-001", "2")));
        check("HEMAT10 10% = 15.000", q.discount() == 15_000);
        q = c.facade.quote(req(body("QRIS", "HEMAT10", 0, "SKU-003", "1")));
        check("HEMAT10 dibatasi maks 25.000", q.discount() == 25_000);
        q = c.facade.quote(req(body("QRIS", "HEMAT10", 0, "SKU-004", "1")));
        check("HEMAT10 di bawah min 50.000 -> error", !q.valid());
        q = c.facade.quote(req(body("QRIS", "POTONG5K", 0, "SKU-001", "1")));
        check("POTONG5K = 5.000", q.discount() == 5_000);
        q = c.facade.quote(req(body("QRIS", "NGAWUR", 0, "SKU-001", "1")));
        check("voucher tidak dikenal -> error", !q.valid());
        q = c.facade.quote(req(body("QRIS", "", 20_000, "SKU-004", "1")));
        check("poin dibatasi 50% (12.500)", q.valid() && q.pointsUsed() == 12_500);
        q = c.facade.quote(req(body("QRIS", "", 0, "SKU-003", "4")));
        check("melebihi stok -> error", !q.valid());
        q = c.facade.quote(req(body("QRIS", "", 0, "SKU-999", "1")));
        check("produk tidak ada -> error", !q.valid());
        for (String bad : new String[]{"{}", "{\"items\":[],\"method\":\"QRIS\"}",
                "{\"items\":[{\"code\":\"SKU-001\",\"qty\":0}],\"method\":\"QRIS\"}",
                "{\"items\":[{\"code\":\"SKU-001\",\"qty\":1.5}],\"method\":\"QRIS\"}",
                "{\"items\":[{\"code\":\"SKU-001\",\"qty\":1}]}",
                "{\"items\":[{\"code\":\"SKU-001\",\"qty\":1}],\"method\":\"QRIS\",\"points\":-5}"}) {
            boolean rejected = false;
            try {
                Quote z = c.facade.quote(req(bad));
                rejected = !z.valid();
            } catch (RuntimeException e) { rejected = true; }
            check("input buruk ditolak: " + bad.substring(0, Math.min(40, bad.length())), rejected);
        }
    }

    static void testFacadeFlows() {
        System.out.println("# Facade + Observer");
        AppContext c = fresh();
        CheckoutFacade f = c.facade;
        Order o = f.checkout(req(body("QRIS", "", 5_000, "SKU-001", "2")));
        check("status awal PENDING_PAYMENT", o.status == OrderStatus.PENDING_PAYMENT);
        check("stok direservasi (20 -> 18)", c.store.product("SKU-001").get().stock() == 18);
        check("poin dipotong (20.000 -> 15.000)", f.points() == 15_000);
        o = f.pay(o.id);
        final String paidId = o.id;
        check("bayar -> PAID", o.status == OrderStatus.PAID);
        long expectEarn = o.total / 10_000;
        check("poin bertambah " + expectEarn, f.points() == 15_000 + expectEarn);
        ApiException e = expectApi(() -> f.pay(paidId));
        check("bayar dua kali -> 409", e != null && e.status() == 409);
        e = expectApi(() -> f.cancel(paidId));
        check("batal pesanan PAID -> 409", e != null && e.status() == 409);
        e = expectApi(() -> f.pay("ORD-TIDAKADA"));
        check("pesanan tak ada -> 404", e != null && e.status() == 404);

        Order c1 = f.checkout(req(body("QRIS", "", 3_000, "SKU-002", "1")));
        long ptsMid = f.points();
        c1 = f.cancel(c1.id);
        check("batal -> CANCELLED", c1.status == OrderStatus.CANCELLED);
        check("stok SKU-002 kembali 5", c.store.product("SKU-002").get().stock() == 5);
        check("poin dikembalikan", f.points() == ptsMid + 3_000);
        e = expectApi(() -> f.pay(c.facade.get(c.facade.recent(10).get(0).id).id));
        check("bayar pesanan CANCELLED -> 409", e != null && e.status() == 409);

        Order w = f.checkout(req(body("EWALLET", "", 0, "SKU-003", "3")));
        w = f.pay(w.id);
        check("e-wallet > 1 juta -> FAILED", w.status == OrderStatus.FAILED);
        check("stok dilepas setelah gagal (3)", c.store.product("SKU-003").get().stock() == 3);
        check("tidak ada poin didapat saat gagal", w.pointsEarned == 0);
        e = expectApi(() -> f.checkout(req(body("QRIS", "", 0, "SKU-003", "4"))));
        check("checkout melebihi stok -> 422", e != null && e.status() == 422);
        check("notifikasi tercatat", !c.notifications.recent(50).isEmpty());
        check("log singleton tercatat", !TransactionLog.getInstance().recent(50).isEmpty());
        check("singleton instance sama", TransactionLog.getInstance() == TransactionLog.getInstance());
    }

    static ApiException expectApi(Runnable r) {
        try { r.run(); } catch (ApiException e) { return e; }
        return null;
    }

    static void testConcurrency() throws Exception {
        System.out.println("# Konkurensi");
        AppContext c = fresh();
        ExecutorService ex = Executors.newFixedThreadPool(10);
        AtomicInteger ok = new AtomicInteger(), rejected = new AtomicInteger();
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            fs.add(ex.submit(() -> {
                try {
                    go.await();
                    c.facade.checkout(req(body("QRIS", "", 0, "SKU-003", "1")));
                    ok.incrementAndGet();
                } catch (ApiException e) { rejected.incrementAndGet(); }
                catch (InterruptedException ignored) { }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : fs) f.get(20, TimeUnit.SECONDS);
        ex.shutdown();
        check("tepat 3 dari 10 berhasil (stok 3)", ok.get() == 3 && rejected.get() == 7);
        check("stok akhir 0, tidak negatif", c.store.product("SKU-003").get().stock() == 0);
        Set<String> ids = new HashSet<>();
        for (Order o : c.facade.recent(50)) ids.add(o.id);
        check("ID pesanan unik", ids.size() == 3);
        Thread[] ts = new Thread[8];
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < ts.length; i++) { ts[i] = new Thread(() -> seen.add(TransactionLog.getInstance())); ts[i].start(); }
        for (Thread t : ts) t.join();
        check("singleton aman antar-thread", seen.size() == 1);
    }

    static void testObserverIsolation() {
        System.out.println("# Observer: isolasi kegagalan");
        AppContext c = fresh();
        c.publisher.subscribe(new checkout.observer.OrderObserver() {
            public String name() { return "Rusak"; }
            public void onStatusChanged(checkout.observer.StatusEvent e) {
                if (e.current() == OrderStatus.PAID) throw new IllegalStateException("sengaja");
            }
        });
        Order o = c.facade.checkout(req(body("QRIS", "", 0, "SKU-004", "1")));
        o = c.facade.pay(o.id);
        check("observer rusak tidak menggagalkan PAID", o.status == OrderStatus.PAID);
        check("peringatan observer dicatat", !o.warnings.isEmpty());

        AppContext c2 = fresh();
        c2.publisher.subscribe(new checkout.observer.OrderObserver() {
            public String name() { return "GagalReservasi"; }
            public void onStatusChanged(checkout.observer.StatusEvent e) {
                if (e.current() == OrderStatus.PENDING_PAYMENT) throw new IllegalStateException("gagal");
            }
        });
        ApiException ae = expectApi(() -> c2.facade.checkout(req(body("QRIS", "", 2_000, "SKU-004", "2"))));
        check("kegagalan saat PENDING -> 500", ae != null && ae.status() == 500);
        check("kompensasi: stok kembali 50", c2.store.product("SKU-004").get().stock() == 50);
        check("kompensasi: poin kembali 20.000", c2.facade.points() == 20_000);
        int n = c.publisher.count();
        checkout.observer.OrderObserver tmp = new checkout.observer.NotificationObserver();
        c.publisher.subscribe(tmp);
        c.publisher.unsubscribe(tmp);
        check("subscribe/unsubscribe", c.publisher.count() == n);
    }

    // ---------------------------------------------------------- mock PostgREST
    static class MockPostgrest {
        final Map<String, List<Map<String, Object>>> tables = new HashMap<>();
        final Map<String, String> pk = Map.of("products", "code", "orders", "id", "loyalty", "customer");
        long serial = 0;
        HttpServer server;
        volatile boolean fail = false;
        volatile String lastKey = "";

        MockPostgrest() throws Exception {
            for (String t : List.of("products", "orders", "loyalty", "transaction_log")) tables.put(t, new ArrayList<>());
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/rest/v1/", ex -> {
                byte[] out; int status = 200;
                synchronized (this) {
                    try {
                        lastKey = Objects.toString(ex.getRequestHeaders().getFirst("apikey"), "");
                        String auth = Objects.toString(ex.getRequestHeaders().getFirst("Authorization"), "");
                        if (fail) { status = 500; out = "{\"message\":\"boom\"}".getBytes(); }
                        else if (!auth.equals("Bearer " + lastKey)) { status = 401; out = "{}".getBytes(); }
                        else {
                            String table = ex.getRequestURI().getPath().substring("/rest/v1/".length());
                            Map<String, String> q = query(ex.getRequestURI());
                            List<Map<String, Object>> rows = tables.get(table);
                            String m = ex.getRequestMethod();
                            String reqBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                            if (rows == null) { status = 404; out = "{}".getBytes(); }
                            else if (m.equals("GET")) {
                                List<Map<String, Object>> res = filter(rows, q);
                                String ord = q.get("order");
                                if (ord != null) {
                                    String[] p = ord.split("\\.");
                                    Comparator<Map<String, Object>> cmp = (a, b) -> cmpv(a.get(p[0]), b.get(p[0]));
                                    res.sort(p[1].equals("desc") ? cmp.reversed() : cmp);
                                }
                                if (q.containsKey("limit")) res = new ArrayList<>(res.subList(0, Math.min(res.size(), Integer.parseInt(q.get("limit")))));
                                out = Json.write(res).getBytes(StandardCharsets.UTF_8);
                            } else if (m.equals("POST")) {
                                Object parsed = Json.parse(reqBody);
                                List<?> list = parsed instanceof List<?> l ? l : List.of(parsed);
                                for (Object o : list) {
                                    Map<String, Object> row = new LinkedHashMap<>(Json.asObject(o));
                                    String key = pk.get(table);
                                    if (table.equals("transaction_log")) { row.put("id", ++serial); row.put("created_at", row.getOrDefault("created_at", OffsetDateTime.now(ZoneOffset.UTC).toString().replace("Z", "+00:00"))); rows.add(row); continue; }
                                    boolean replaced = false;
                                    for (Map<String, Object> ex2 : rows) if (Objects.equals(ex2.get(key), row.get(key))) {
                                        if (q.containsKey("on_conflict")) { ex2.putAll(row); replaced = true; break; }
                                        status = 409;
                                    }
                                    if (status == 409) break;
                                    if (!replaced) rows.add(row);
                                }
                                out = new byte[0]; if (status == 200) status = 201; else out = "{\"message\":\"dup\"}".getBytes();
                            } else if (m.equals("PATCH")) {
                                Map<String, Object> patch = Json.asObject(Json.parse(reqBody));
                                for (Map<String, Object> r : filter(rows, q)) r.putAll(patch);
                                out = new byte[0]; status = 204;
                            } else { status = 405; out = "{}".getBytes(); }
                        }
                    } catch (RuntimeException e) { status = 400; out = ("{\"message\":\"" + e + "\"}").getBytes(); }
                }
                ex.sendResponseHeaders(status, status == 204 || out.length == 0 ? -1 : out.length);
                if (status != 204 && out.length > 0) { ex.getResponseBody().write(out); }
                ex.close();
            });
            server.start();
        }

        static int cmpv(Object a, Object b) {
            if (a instanceof Number x && b instanceof Number y) return Double.compare(x.doubleValue(), y.doubleValue());
            return String.valueOf(a).compareTo(String.valueOf(b));
        }

        static Map<String, String> query(URI u) {
            Map<String, String> m = new HashMap<>();
            String raw = u.getRawQuery();
            if (raw == null) return m;
            for (String kv : raw.split("&")) {
                int i = kv.indexOf('=');
                m.put(URLDecoder.decode(kv.substring(0, i), StandardCharsets.UTF_8), URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8));
            }
            return m;
        }

        static List<Map<String, Object>> filter(List<Map<String, Object>> rows, Map<String, String> q) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                boolean ok = true;
                for (Map.Entry<String, String> e : q.entrySet()) {
                    if (e.getValue().startsWith("eq.") && !String.valueOf(r.get(e.getKey())).equals(e.getValue().substring(3))) ok = false;
                }
                if (ok) out.add(r);
            }
            return out;
        }
    }

    static void testSupabaseMock() throws Exception {
        System.out.println("# SupabaseStore (mock PostgREST)");
        MockPostgrest mock = new MockPostgrest();
        String url = "http://127.0.0.1:" + mock.server.getAddress().getPort() + "/";
        SupabaseStore s = new SupabaseStore(url, "kunci-uji");
        s.init();
        s.init();
        check("seed 5 produk (idempoten)", s.products().size() == 5 && mock.tables.get("products").size() == 5);
        check("poin awal 20.000", s.points("demo") == 20_000);
        check("header apikey/Authorization terkirim", mock.lastKey.equals("kunci-uji"));
        AppContext c = new AppContext(s);
        Order o = c.facade.checkout(req(body("VA", "POTONG5K", 1_000, "SKU-002", "2")));
        check("stok tersimpan di Supabase (5->3)", s.product("SKU-002").get().stock() == 3);
        o = c.facade.pay(o.id);
        Order back = s.order(o.id).orElseThrow();
        check("pesanan PAID tersimpan & terbaca", back.status == OrderStatus.PAID && back.total == o.total && back.lines.size() == 1);
        check("upsert tidak menggandakan baris", mock.tables.get("orders").size() == 1);
        check("log tersimpan di transaction_log", mock.tables.get("transaction_log").size() >= 2 && !s.recentLogs(10).isEmpty());
        AppContext c2 = new AppContext(s);
        check("riwayat log dimuat ulang saat start", !TransactionLog.getInstance().recent(10).isEmpty() && c2 != null);
        check("recentOrders memuat pesanan", s.recentOrders(10).size() == 1);
        mock.fail = true;
        boolean threw = false;
        try { s.products(); } catch (StoreException e) { threw = true; }
        check("HTTP 500 -> StoreException", threw);
        mock.fail = false;
        mock.server.stop(0);
        threw = false;
        try { s.products(); } catch (StoreException e) { threw = true; }
        check("server mati -> StoreException", threw);
    }

    // -------------------------------------------------------------------- HTTP
    static void testHttp() throws Exception {
        System.out.println("# REST API");
        HttpApi api = new HttpApi(fresh());
        api.start("127.0.0.1", 0);
        String base = "http://127.0.0.1:" + api.port();
        HttpClient http = HttpClient.newHttpClient();
        try {
            check("GET /api/health", call(http, "GET", base + "/api/health", null).status == 200);
            R r = call(http, "GET", base + "/api/products", null);
            check("GET /api/products 5 item", r.status == 200 && ((List<?>) Json.parse(r.body)).size() == 5);
            check("GET / menyajikan UI", call(http, "GET", base + "/", null).body.contains("SIMULASI"));
            check("GET /api/me", call(http, "GET", base + "/api/me", null).body.contains("20000"));
            r = call(http, "POST", base + "/api/quote", body("QRIS", "", 0, "SKU-001", "1"));
            check("POST /api/quote 200", r.status == 200 && r.body.contains("\"valid\":true"));
            r = call(http, "POST", base + "/api/orders", body("QRIS", "", 0, "SKU-001", "1"));
            check("POST /api/orders 201", r.status == 201);
            String id = (String) Json.asObject(Json.parse(r.body)).get("id");
            r = call(http, "POST", base + "/api/orders/" + id + "/pay", "");
            check("pay -> PAID", r.status == 200 && r.body.contains("\"PAID\""));
            check("pay ulang -> 409", call(http, "POST", base + "/api/orders/" + id + "/pay", "").status == 409);
            check("GET order 200", call(http, "GET", base + "/api/orders/" + id, null).status == 200);
            check("order tak ada -> 404", call(http, "GET", base + "/api/orders/NOPE", null).status == 404);
            check("endpoint tak ada -> 404", call(http, "GET", base + "/api/xyz", null).status == 404);
            check("metode salah -> 405", call(http, "DELETE", base + "/api/orders", null).status == 405);
            check("JSON rusak -> 400", call(http, "POST", base + "/api/quote", "{bad").status == 400);
            check("stok kurang -> 422", call(http, "POST", base + "/api/orders", body("QRIS", "", 0, "SKU-003", "9")).status == 422);
            check("body > 64KB -> 413", call(http, "POST", base + "/api/quote", "x".repeat(70_000)).status == 413);
            check("GET notifications/log 200",
                    call(http, "GET", base + "/api/notifications", null).status == 200 && call(http, "GET", base + "/api/log", null).status == 200);
            check("path traversal statis -> 404", call(http, "GET", base + "/../etc/passwd", null).status == 404);
        } finally {
            api.stop();
        }
    }

    record R(int status, String body) { }

    static R call(HttpClient http, String method, String url, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json");
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> res = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new R(res.statusCode(), res.body());
    }
}
