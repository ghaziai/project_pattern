package checkout.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record OrderLine(String code, String name, long price, int qty) {

    public long amount() {
        return price * qty;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("name", name);
        m.put("price", price);
        m.put("qty", qty);
        return m;
    }
}
