package checkout.facade;

import checkout.mediator.CheckoutSession;
import checkout.model.Order;
import checkout.model.OrderRequest;
import checkout.model.OrderStatus;
import checkout.model.Product;
import checkout.model.Quote;
import checkout.observer.OrderStatusPublisher;
import checkout.observer.StatusEvent;
import checkout.payment.PaymentGateway;
import checkout.payment.PaymentGateways;
import checkout.payment.PaymentResult;
import checkout.store.DataStore;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Facade: satu pintu untuk seluruh alur checkout. Pemanggil tidak perlu tahu tentang
 * Mediator (penghitungan), Factory Method (metode pembayaran), maupun Observer (efek samping).
 */
public class CheckoutFacade {
    private final DataStore store;
    private final OrderStatusPublisher publisher;
    private final Object lock = new Object();
    private long lastIdMillis = 0;

    public CheckoutFacade(DataStore store, OrderStatusPublisher publisher) {
        this.store = store;
        this.publisher = publisher;
    }

    public List<Product> products() {
        return store.products();
    }

    public long points() {
        return store.points(OrderRequest.DEFAULT_CUSTOMER);
    }

    /** Menghitung harga tanpa membuat pesanan. */
    public Quote quote(OrderRequest request) {
        return CheckoutSession.quote(store, request);
    }

    /** Membuat pesanan berstatus PENDING_PAYMENT; stok dan poin langsung direservasi oleh pengamat. */
    public Order checkout(OrderRequest request) {
        synchronized (lock) {
            Quote q = CheckoutSession.quote(store, request);
            if (!q.valid()) {
                throw new ApiException(422, "Pesanan tidak valid", q.errors());
            }
            Order o = new Order();
            o.id = newId();
            o.customer = request.customer();
            o.method = q.method();
            o.lines = q.lines();
            o.subtotal = q.subtotal();
            o.discount = q.discount();
            o.voucher = q.voucher();
            o.pointsUsed = q.pointsUsed();
            o.fee = q.fee();
            o.total = q.total();
            o.status = OrderStatus.PENDING_PAYMENT;
            o.createdAt = Instant.now();
            o.updatedAt = o.createdAt;

            store.saveOrder(o);
            List<String> failures = publisher.publish(new StatusEvent(o, null, OrderStatus.PENDING_PAYMENT));
            if (!failures.isEmpty()) {
                o.message = "Gagal menyiapkan pesanan: " + String.join("; ", failures);
                transition(o, OrderStatus.FAILED, true);
                throw new ApiException(500, "Pesanan gagal diproses dan dibatalkan", failures);
            }
            try {
                saveWithRetry(o);
            } catch (RuntimeException e) {
                o.message = "Gagal menyimpan pesanan";
                transition(o, OrderStatus.FAILED, true);
                throw e;
            }
            return o;
        }
    }

    /** Membayar pesanan (SIMULASI, tidak ada transaksi nyata). */
    public Order pay(String orderId) {
        synchronized (lock) {
            Order o = find(orderId);
            requirePending(o, "dibayar");
            PaymentGateway gateway = PaymentGateways.forMethod(o.method);
            PaymentResult result = gateway.charge(o.total, o.id);
            if (result.success()) {
                o.paymentRef = result.reference();
                o.message = result.message();
                transition(o, OrderStatus.PAID, false);
            } else {
                o.message = result.message();
                transition(o, OrderStatus.FAILED, false);
            }
            return o;
        }
    }

    public Order cancel(String orderId) {
        synchronized (lock) {
            Order o = find(orderId);
            requirePending(o, "dibatalkan");
            o.message = "Dibatalkan oleh pelanggan";
            transition(o, OrderStatus.CANCELLED, false);
            return o;
        }
    }

    public Order get(String orderId) {
        return find(orderId);
    }

    public List<Order> recent(int limit) {
        return store.recentOrders(limit);
    }

    private Order find(String id) {
        return store.order(id).orElseThrow(() -> new ApiException(404, "Pesanan '" + id + "' tidak ditemukan"));
    }

    private static void requirePending(Order o, String action) {
        if (o.status != OrderStatus.PENDING_PAYMENT) {
            throw new ApiException(409, "Pesanan " + o.id + " berstatus " + o.status + " dan tidak dapat " + action);
        }
    }

    private void transition(Order o, OrderStatus next, boolean bestEffortSave) {
        OrderStatus previous = o.status;
        o.status = next;
        o.updatedAt = Instant.now();
        o.warnings.addAll(publisher.publish(new StatusEvent(o, previous, next)));
        if (bestEffortSave) {
            try {
                saveWithRetry(o);
            } catch (RuntimeException e) {
                System.err.println("[FACADE] Gagal menyimpan status " + next + " untuk " + o.id + ": " + e.getMessage());
            }
        } else {
            saveWithRetry(o);
        }
    }

    private void saveWithRetry(Order o) {
        try {
            store.saveOrder(o);
        } catch (RuntimeException first) {
            store.saveOrder(o);
        }
    }

    private String newId() {
        long now = System.currentTimeMillis();
        if (now <= lastIdMillis) {
            now = lastIdMillis + 1;
        }
        lastIdMillis = now;
        return "ORD-" + Long.toString(now, 36).toUpperCase(Locale.ROOT);
    }
}
