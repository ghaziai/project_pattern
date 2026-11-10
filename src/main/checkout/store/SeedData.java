package checkout.store;

import checkout.model.Product;

import java.util.List;

/** Data awal untuk demo. */
public final class SeedData {
    public static final String CUSTOMER = "demo";
    public static final long INITIAL_POINTS = 20_000;

    private SeedData() {
    }

    public static List<Product> products() {
        return List.of(
                new Product("SKU-001", "Kopi Arabika 250 g", 75_000, 20),
                new Product("SKU-002", "Teko Keramik", 120_000, 5),
                new Product("SKU-003", "Mesin Giling Kopi", 500_000, 3),
                new Product("SKU-004", "Filter Kertas (isi 100)", 25_000, 50),
                new Product("SKU-005", "Timbangan Digital", 150_000, 8));
    }
}
