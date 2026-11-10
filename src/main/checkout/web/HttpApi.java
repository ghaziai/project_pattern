package checkout.web;

import checkout.facade.ApiException;
import checkout.facade.AppContext;
import checkout.model.LogEntry;
import checkout.model.Order;
import checkout.model.OrderRequest;
import checkout.model.Product;
import checkout.observer.NotificationObserver;
import checkout.payment.PaymentGateways;
import checkout.singleton.TransactionLog;
import checkout.store.StoreException;
import checkout.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** REST API + berkas UI menggunakan HttpServer bawaan JDK. */
public class HttpApi {
    private static final int MAX_BODY = 64 * 1024;
    private final AppContext ctx;
    private HttpServer server;
    private ExecutorService executor;

    public HttpApi(AppContext ctx) {
        this.ctx = ctx;
    }

    public void start(String host, int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/api/", this::handleApi);
        server.createContext("/", this::handleStatic);
        executor = Executors.newFixedThreadPool(8);
        server.setExecutor(executor);
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    // ---------------------------------------------------------------- API

    private record Reply(int status, Object body) {
    }

    private void handleApi(HttpExchange ex) throws IOException {
        Reply reply;
        try {
            reply = route(ex);
        } catch (ApiException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            err.put("details", e.details());
            reply = new Reply(e.status(), err);
        } catch (IllegalArgumentException e) {
            reply = new Reply(400, error(e.getMessage()));
        } catch (StoreException e) {
            System.err.println("[API] Kesalahan penyimpanan: " + e.getMessage());
            reply = new Reply(502, error("Penyimpanan data bermasalah: " + e.getMessage()));
        } catch (RuntimeException e) {
            e.printStackTrace();
            reply = new Reply(500, error("Terjadi kesalahan internal"));
        }
        sendJson(ex, reply.status(), reply.body());
    }

    private Reply route(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod().toUpperCase();
        String path = ex.getRequestURI().getPath();
        String rest = path.length() > 5 ? path.substring(5) : "";
        while (rest.endsWith("/")) {
            rest = rest.substring(0, rest.length() - 1);
        }
        String[] seg = rest.isEmpty() ? new String[0] : rest.split("/");
        if (seg.length == 0) {
            throw new ApiException(404, "Endpoint tidak ditemukan");
        }
        switch (seg[0]) {
            case "health" -> {
                requireMethod(ex, method, "GET", seg.length == 1);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("status", "ok");
                m.put("store", ctx.store.name());
                return new Reply(200, m);
            }
            case "products" -> {
                requireMethod(ex, method, "GET", seg.length == 1);
                List<Map<String, Object>> out = new ArrayList<>();
                for (Product p : ctx.facade.products()) {
                    out.add(p.toMap());
                }
                return new Reply(200, out);
            }
            case "methods" -> {
                requireMethod(ex, method, "GET", seg.length == 1);
                return new Reply(200, PaymentGateways.available());
            }
            case "me" -> {
                requireMethod(ex, method, "GET", seg.length == 1);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("customer", OrderRequest.DEFAULT_CUSTOMER);
                m.put("points", ctx.facade.points());
                return new Reply(200, m);
            }
            case "quote" -> {
                requireMethod(ex, method, "POST", seg.length == 1);
                OrderRequest req = OrderRequest.fromJson(Json.parse(readBody(ex)));
                return new Reply(200, ctx.facade.quote(req).toMap());
            }
            case "orders" -> {
                return routeOrders(ex, method, seg);
            }
            case "notifications" -> {
                requireMethod(ex, method, "GET", seg.length == 1);
                List<Map<String, Object>> out = new ArrayList<>();
                for (NotificationObserver.Notification n : ctx.notifications.recent(50)) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("time", n.time().toString());
                    m.put("orderId", n.orderId());
                    m.put("message", n.message());
                    out.add(m);
                }
                return new Reply(200, out);
            }
            case "log" -> {
                requireMethod(ex, method, "GET", seg.length == 1);
                List<Map<String, Object>> out = new ArrayList<>();
                for (LogEntry e : TransactionLog.getInstance().recent(50)) {
                    out.add(e.toMap());
                }
                return new Reply(200, out);
            }
            default -> throw new ApiException(404, "Endpoint tidak ditemukan");
        }
    }

    private Reply routeOrders(HttpExchange ex, String method, String[] seg) throws IOException {
        if (seg.length == 1) {
            if (method.equals("GET")) {
                List<Map<String, Object>> out = new ArrayList<>();
                for (Order o : ctx.facade.recent(50)) {
                    out.add(o.toMap());
                }
                return new Reply(200, out);
            }
            if (method.equals("POST")) {
                OrderRequest req = OrderRequest.fromJson(Json.parse(readBody(ex)));
                return new Reply(201, ctx.facade.checkout(req).toMap());
            }
            ex.getResponseHeaders().set("Allow", "GET, POST");
            throw new ApiException(405, "Metode " + method + " tidak diizinkan");
        }
        String id = seg[1];
        if (seg.length == 2) {
            requireMethod(ex, method, "GET", true);
            return new Reply(200, ctx.facade.get(id).toMap());
        }
        if (seg.length == 3 && (seg[2].equals("pay") || seg[2].equals("cancel"))) {
            requireMethod(ex, method, "POST", true);
            readBody(ex);
            Order o = seg[2].equals("pay") ? ctx.facade.pay(id) : ctx.facade.cancel(id);
            return new Reply(200, o.toMap());
        }
        throw new ApiException(404, "Endpoint tidak ditemukan");
    }

    private static void requireMethod(HttpExchange ex, String actual, String expected, boolean pathOk) {
        if (!pathOk) {
            throw new ApiException(404, "Endpoint tidak ditemukan");
        }
        if (!actual.equals(expected)) {
            ex.getResponseHeaders().set("Allow", expected);
            throw new ApiException(405, "Metode " + actual + " tidak diizinkan (gunakan " + expected + ")");
        }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            byte[] data = in.readNBytes(MAX_BODY + 1);
            if (data.length > MAX_BODY) {
                throw new ApiException(413, "Body terlalu besar (maks. " + MAX_BODY / 1024 + " KB)");
            }
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private static Map<String, Object> error(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", message);
        m.put("details", List.of());
        return m;
    }

    private static void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = Json.write(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ------------------------------------------------------------- static

    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        byte[] bytes;
        int status = 200;
        String type = "text/html; charset=utf-8";
        if (!method.equals("GET")) {
            ex.getResponseHeaders().set("Allow", "GET");
            status = 405;
            bytes = "Metode tidak diizinkan".getBytes(StandardCharsets.UTF_8);
            type = "text/plain; charset=utf-8";
        } else if (path.equals("/") || path.equals("/index.html")) {
            try (InputStream in = HttpApi.class.getResourceAsStream("/index.html")) {
                if (in == null) {
                    status = 404;
                    type = "text/plain; charset=utf-8";
                    bytes = "UI tidak ditemukan: pastikan folder 'web' ada pada classpath.".getBytes(StandardCharsets.UTF_8);
                } else {
                    bytes = in.readAllBytes();
                }
            }
        } else {
            status = 404;
            type = "text/plain; charset=utf-8";
            bytes = "Tidak ditemukan".getBytes(StandardCharsets.UTF_8);
        }
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
