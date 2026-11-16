package checkout.store;

import checkout.model.LogEntry;
import checkout.model.Order;
import checkout.model.Product;

import java.util.List;
import java.util.Optional;

/** Lapisan penyimpanan: implementasi memori atau Supabase. */
public interface DataStore {
    String name();

    List<Product> products();

    Optional<Product> product(String code);

    void setStock(String code, int stock);

    void saveOrder(Order order);

    Optional<Order> order(String id);

    /** Pesanan terbaru lebih dulu. */
    List<Order> recentOrders(int limit);

    long points(String customer);

    void setPoints(String customer, long points);

    void appendLog(LogEntry entry);

    /** Log terbaru lebih dulu. */
    List<LogEntry> recentLogs(int limit);
}
