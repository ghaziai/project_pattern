package checkout.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pesanan. Field bersifat mutable karena status dan penanda reservasi diubah oleh pengamat. */
public class Order {
    public String id;
    public String customer;
    public OrderStatus status;
    public String method;
    public List<OrderLine> lines = new ArrayList<>();
    public long subtotal;
    public long discount;
    public String voucher = "";
    public long pointsUsed;
    public long fee;
    public long total;
    public boolean stockReserved;
    public boolean pointsReserved;
    public long pointsEarned;
    public String paymentRef = "";
    public String message = "";
    public Instant createdAt = Instant.now();
    public Instant updatedAt = createdAt;
    /** Peringatan sementara (tidak disimpan) dari pengamat yang gagal. */
    public final List<String> warnings = new ArrayList<>();

    /** Representasi untuk API. */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("customer", customer);
        m.put("status", status.name());
        m.put("method", method);
        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderLine l : lines) {
            items.add(l.toMap());
        }
        m.put("items", items);
        m.put("subtotal", subtotal);
        m.put("discount", discount);
        m.put("voucher", voucher);
        m.put("pointsUsed", pointsUsed);
        m.put("fee", fee);
        m.put("total", total);
        m.put("pointsEarned", pointsEarned);
        m.put("paymentRef", paymentRef);
        m.put("message", message);
        m.put("createdAt", createdAt.toString());
        m.put("updatedAt", updatedAt.toString());
        m.put("warnings", warnings);
        return m;
    }

    /** Representasi baris tabel Supabase (snake_case). */
    public Map<String, Object> toRow() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("customer", customer);
        m.put("status", status.name());
        m.put("method", method);
        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderLine l : lines) {
            items.add(l.toMap());
        }
        m.put("items", items);
        m.put("subtotal", subtotal);
        m.put("discount", discount);
        m.put("voucher", voucher);
        m.put("points_used", pointsUsed);
        m.put("fee", fee);
        m.put("total", total);
        m.put("stock_reserved", stockReserved);
        m.put("points_reserved", pointsReserved);
        m.put("points_earned", pointsEarned);
        m.put("payment_ref", paymentRef);
        m.put("message", message);
        m.put("created_at", createdAt.toString());
        m.put("updated_at", updatedAt.toString());
        return m;
    }

    @SuppressWarnings("unchecked")
    public static Order fromRow(Map<String, Object> r) {
        Order o = new Order();
        o.id = str(r.get("id"));
        o.customer = str(r.get("customer"));
        o.status = OrderStatus.valueOf(str(r.get("status")));
        o.method = str(r.get("method"));
        Object items = r.get("items");
        if (items instanceof List<?> list) {
            for (Object x : list) {
                Map<String, Object> im = (Map<String, Object>) x;
                o.lines.add(new OrderLine(str(im.get("code")), str(im.get("name")),
                        num(im.get("price")), (int) num(im.get("qty"))));
            }
        }
        o.subtotal = num(r.get("subtotal"));
        o.discount = num(r.get("discount"));
        o.voucher = str(r.get("voucher"));
        o.pointsUsed = num(r.get("points_used"));
        o.fee = num(r.get("fee"));
        o.total = num(r.get("total"));
        o.stockReserved = Boolean.TRUE.equals(r.get("stock_reserved"));
        o.pointsReserved = Boolean.TRUE.equals(r.get("points_reserved"));
        o.pointsEarned = num(r.get("points_earned"));
        o.paymentRef = str(r.get("payment_ref"));
        o.message = str(r.get("message"));
        o.createdAt = OffsetDateTime.parse(str(r.get("created_at"))).toInstant();
        o.updatedAt = r.get("updated_at") == null ? o.createdAt
                : OffsetDateTime.parse(str(r.get("updated_at"))).toInstant();
        return o;
    }

    static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    public static long num(Object o) {
        if (o instanceof Number n) {
            return (n instanceof Double || n instanceof Float) ? Math.round(n.doubleValue()) : n.longValue();
        }
        return o == null ? 0L : Long.parseLong(o.toString());
    }
}
