package checkout.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record Product(String code, String name, long price, int stock) {

    public Product withStock(int newStock) {
        return new Product(code, name, price, newStock);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("name", name);
        m.put("price", price);
        m.put("stock", stock);
        return m;
    }
}
