package checkout.store;

import checkout.model.LogEntry;
import checkout.model.Order;
import checkout.model.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Penyimpanan di memori (data hilang saat aplikasi berhenti). */
public class InMemoryStore implements DataStore {
    private final Map<String, Product> products = new LinkedHashMap<>();
    private final Map<String, Order> orders = new LinkedHashMap<>();
    private final Map<String, Long> points = new LinkedHashMap<>();
    private final List<LogEntry> logs = new ArrayList<>();

    public InMemoryStore() {
        for (Product p : SeedData.products()) {
            products.put(p.code(), p);
        }
        points.put(SeedData.CUSTOMER, SeedData.INITIAL_POINTS);
    }

    @Override
    public String name() {
        return "memory";
    }

    @Override
    public synchronized List<Product> products() {
        return new ArrayList<>(products.values());
    }

    @Override
    public synchronized Optional<Product> product(String code) {
        return Optional.ofNullable(products.get(code));
    }

    @Override
    public synchronized void setStock(String code, int stock) {
        Product p = products.get(code);
        if (p == null) {
            throw new StoreException("Produk tidak ditemukan: " + code);
        }
        products.put(code, p.withStock(stock));
    }

    @Override
    public synchronized void saveOrder(Order order) {
        orders.put(order.id, copy(order));
    }

    @Override
    public synchronized Optional<Order> order(String id) {
        Order o = orders.get(id);
        return o == null ? Optional.empty() : Optional.of(copy(o));
    }

    @Override
    public synchronized List<Order> recentOrders(int limit) {
        List<Order> all = new ArrayList<>();
        for (Order o : orders.values()) {
            all.add(copy(o));
        }
        all.sort((a, b) -> b.createdAt.compareTo(a.createdAt));
        return all.size() > limit ? new ArrayList<>(all.subList(0, limit)) : all;
    }

    @Override
    public synchronized long points(String customer) {
        return points.getOrDefault(customer, 0L);
    }

    @Override
    public synchronized void setPoints(String customer, long value) {
        points.put(customer, value);
    }

    @Override
    public synchronized void appendLog(LogEntry entry) {
        logs.add(entry);
    }

    @Override
    public synchronized List<LogEntry> recentLogs(int limit) {
        List<LogEntry> copy = new ArrayList<>(logs);
        Collections.reverse(copy);
        return copy.size() > limit ? new ArrayList<>(copy.subList(0, limit)) : copy;
    }

    /** Salinan agar objek di penyimpanan tidak ikut berubah saat objek luar dimodifikasi. */
    private static Order copy(Order o) {
        Order c = Order.fromRow(o.toRow());
        return c;
    }
}
