package checkout.observer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Subject: menyiarkan perubahan status; kegagalan satu pengamat tidak menghentikan yang lain. */
public class OrderStatusPublisher {
    private final List<OrderObserver> observers = new CopyOnWriteArrayList<>();

    public void subscribe(OrderObserver observer) {
        observers.add(observer);
    }

    public void unsubscribe(OrderObserver observer) {
        observers.remove(observer);
    }

    public int count() {
        return observers.size();
    }

    /** Mengembalikan daftar pesan kegagalan pengamat (kosong bila semua berhasil). */
    public List<String> publish(StatusEvent event) {
        List<String> failures = new ArrayList<>();
        for (OrderObserver o : observers) {
            try {
                o.onStatusChanged(event);
            } catch (RuntimeException e) {
                failures.add(o.name() + ": " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            }
        }
        return failures;
    }
}
