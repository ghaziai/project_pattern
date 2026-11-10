package checkout.singleton;

import checkout.model.LogEntry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/** Singleton: satu pencatat transaksi untuk seluruh aplikasi (aman untuk banyak thread). */
public final class TransactionLog {
    private static final int CAPACITY = 200;

    private static final class Holder {
        private static final TransactionLog INSTANCE = new TransactionLog();
    }

    private final Deque<LogEntry> entries = new ArrayDeque<>();
    private volatile Consumer<LogEntry> sink;

    private TransactionLog() {
    }

    public static TransactionLog getInstance() {
        return Holder.INSTANCE;
    }

    /** Penyimpanan permanen opsional; kegagalannya tidak boleh mengganggu transaksi. */
    public void setSink(Consumer<LogEntry> sink) {
        this.sink = sink;
    }

    /** Memuat riwayat lama (terlama lebih dulu) tanpa memanggil sink. */
    public synchronized void preload(List<LogEntry> oldestFirst) {
        for (LogEntry e : oldestFirst) {
            add(e);
        }
    }

    public void record(LogEntry entry) {
        synchronized (this) {
            add(entry);
        }
        Consumer<LogEntry> s = sink;
        if (s != null) {
            try {
                s.accept(entry);
            } catch (RuntimeException e) {
                System.err.println("[LOG] Gagal menyimpan log permanen: " + e.getMessage());
            }
        }
    }

    public synchronized List<LogEntry> recent(int limit) {
        List<LogEntry> out = new ArrayList<>();
        var it = entries.descendingIterator();
        while (it.hasNext() && out.size() < limit) {
            out.add(it.next());
        }
        return out;
    }

    /** Mengosongkan log (dipakai pengujian). */
    public synchronized void reset() {
        entries.clear();
        sink = null;
    }

    private void add(LogEntry e) {
        entries.addLast(e);
        while (entries.size() > CAPACITY) {
            entries.removeFirst();
        }
    }
}
