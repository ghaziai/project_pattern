package checkout.observer;

import checkout.model.Order;
import checkout.store.DataStore;

/** Memotong poin saat pesanan dibuat, mengembalikannya bila gagal/batal, dan memberi poin saat lunas. */
public class PointsObserver implements OrderObserver {
    /** 1 poin untuk setiap Rp10.000 pembayaran. */
    public static final long RUPIAH_PER_POINT = 10_000;
    private final DataStore store;

    public PointsObserver(DataStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "PointsObserver";
    }

    @Override
    public void onStatusChanged(StatusEvent e) {
        Order o = e.order();
        switch (e.current()) {
            case PENDING_PAYMENT -> {
                if (o.pointsUsed > 0 && !o.pointsReserved) {
                    long balance = store.points(o.customer);
                    if (balance < o.pointsUsed) {
                        throw new IllegalStateException("Saldo poin tidak mencukupi");
                    }
                    store.setPoints(o.customer, balance - o.pointsUsed);
                    o.pointsReserved = true;
                }
            }
            case FAILED, CANCELLED -> {
                if (o.pointsReserved) {
                    store.setPoints(o.customer, store.points(o.customer) + o.pointsUsed);
                    o.pointsReserved = false;
                }
            }
            case PAID -> {
                long earned = o.total / RUPIAH_PER_POINT;
                if (earned > 0 && o.pointsEarned == 0) {
                    store.setPoints(o.customer, store.points(o.customer) + earned);
                    o.pointsEarned = earned;
                }
            }
        }
    }
}
