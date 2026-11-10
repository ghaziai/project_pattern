package checkout.mediator;

import checkout.model.Product;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Komponen stok: membaca snapshot produk yang dimuat sekali per sesi. */
public class StockComponent extends Component {
    private final Map<String, Product> snapshot = new HashMap<>();

    public StockComponent(List<Product> products) {
        for (Product p : products) {
            snapshot.put(p.code(), p);
        }
    }

    public Product product(String code) {
        return snapshot.get(code);
    }

    /** Mengembalikan pesan kesalahan, atau null bila stok cukup. */
    public String check(Product p, int qty) {
        if (p.stock() < qty) {
            return "Stok " + p.name() + " tidak mencukupi (tersisa " + p.stock() + ", diminta " + qty + ")";
        }
        return null;
    }
}
