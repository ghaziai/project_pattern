package checkout.observer;

import checkout.model.Order;
import checkout.model.OrderLine;
import checkout.model.Product;
import checkout.store.DataStore;

import java.util.ArrayList;
import java.util.List;

/** Mereservasi stok saat pesanan dibuat dan mengembalikannya bila gagal atau dibatalkan. */
public class StockObserver implements OrderObserver {
    private final DataStore store;

    public StockObserver(DataStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "StockObserver";
    }

    @Override
    public void onStatusChanged(StatusEvent e) {
        switch (e.current()) {
            case PENDING_PAYMENT -> reserve(e.order());
            case FAILED, CANCELLED -> release(e.order());
            default -> {
            }
        }
    }

    private void reserve(Order o) {
        if (o.stockReserved) {
            return;
        }
        List<Product> done = new ArrayList<>();
        List<Integer> doneQty = new ArrayList<>();
        try {
            for (OrderLine l : o.lines) {
                Product p = store.product(l.code())
                        .orElseThrow(() -> new IllegalStateException("Produk " + l.code() + " tidak ditemukan"));
                if (p.stock() < l.qty()) {
                    throw new IllegalStateException("Stok " + p.name() + " tidak mencukupi");
                }
                store.setStock(p.code(), p.stock() - l.qty());
                done.add(p);
                doneQty.add(l.qty());
            }
            o.stockReserved = true;
        } catch (RuntimeException ex) {
            for (int i = 0; i < done.size(); i++) {
                try {
                    store.setStock(done.get(i).code(), done.get(i).stock());
                } catch (RuntimeException ignored) {
                    // upaya pemulihan terbaik; kegagalan asli tetap dilaporkan
                }
            }
            throw ex;
        }
    }

    private void release(Order o) {
        if (!o.stockReserved) {
            return;
        }
        for (OrderLine l : o.lines) {
            Product p = store.product(l.code()).orElse(null);
            if (p != null) {
                store.setStock(p.code(), p.stock() + l.qty());
            }
        }
        o.stockReserved = false;
    }
}
