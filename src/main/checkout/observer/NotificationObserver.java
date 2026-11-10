package checkout.observer;

import checkout.model.Order;
import checkout.util.Money;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Menyimpan notifikasi untuk pelanggan (disimulasikan sebagai daftar di memori). */
public class NotificationObserver implements OrderObserver {
    public record Notification(Instant time, String orderId, String message) {
    }

    private static final int CAPACITY = 100;
    private final Deque<Notification> items = new ArrayDeque<>();

    @Override
    public String name() {
        return "NotificationObserver";
    }

    @Override
    public void onStatusChanged(StatusEvent e) {
        Order o = e.order();
        String text = switch (e.current()) {
            case PENDING_PAYMENT -> "Pesanan " + o.id + " dibuat. Selesaikan pembayaran " + Money.format(o.total)
                    + " via " + o.method + ".";
            case PAID -> "Pembayaran pesanan " + o.id + " berhasil. Terima kasih!";
            case FAILED -> "Pembayaran pesanan " + o.id + " gagal: " + o.message;
            case CANCELLED -> "Pesanan " + o.id + " dibatalkan.";
        };
        synchronized (this) {
            items.addLast(new Notification(Instant.now(), o.id, text));
            while (items.size() > CAPACITY) {
                items.removeFirst();
            }
        }
    }

    public synchronized List<Notification> recent(int limit) {
        List<Notification> out = new ArrayList<>();
        var it = items.descendingIterator();
        while (it.hasNext() && out.size() < limit) {
            out.add(it.next());
        }
        return out;
    }
}
