package checkout.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Hasil perhitungan harga untuk keranjang (tanpa membuat pesanan). */
public record Quote(List<OrderLine> lines, long subtotal, long discount, String voucher,
        long pointsUsed, long pointsAvailable, String method, long fee, long total,
        List<String> errors, List<String> notes) {

    public boolean valid() {
        return errors.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderLine l : lines) {
            items.add(l.toMap());
        }
        m.put("valid", valid());
        m.put("items", items);
        m.put("subtotal", subtotal);
        m.put("discount", discount);
        m.put("voucher", voucher);
        m.put("pointsUsed", pointsUsed);
        m.put("pointsAvailable", pointsAvailable);
        m.put("method", method);
        m.put("fee", fee);
        m.put("total", total);
        m.put("errors", errors);
        m.put("notes", notes);
        return m;
    }
}
