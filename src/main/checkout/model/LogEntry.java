package checkout.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record LogEntry(Instant time, String orderId, String event, String method, long amount, String detail) {

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("time", time.toString());
        m.put("orderId", orderId);
        m.put("event", event);
        m.put("method", method);
        m.put("amount", amount);
        m.put("detail", detail);
        return m;
    }

    public Map<String, Object> toRow() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("created_at", time.toString());
        m.put("order_id", orderId);
        m.put("event", event);
        m.put("method", method);
        m.put("amount", amount);
        m.put("detail", detail);
        return m;
    }

    public static LogEntry fromRow(Map<String, Object> r) {
        return new LogEntry(
                OffsetDateTime.parse(String.valueOf(r.get("created_at"))).toInstant(),
                str(r.get("order_id")), str(r.get("event")), str(r.get("method")),
                Order.num(r.get("amount")), str(r.get("detail")));
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
