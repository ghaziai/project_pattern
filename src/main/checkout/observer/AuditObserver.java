package checkout.observer;

import checkout.model.LogEntry;
import checkout.model.Order;
import checkout.singleton.TransactionLog;

import java.time.Instant;

/** Mencatat setiap perubahan status ke {@link TransactionLog} (Singleton). */
public class AuditObserver implements OrderObserver {
    @Override
    public String name() {
        return "AuditObserver";
    }

    @Override
    public void onStatusChanged(StatusEvent e) {
        Order o = e.order();
        String detail = e.previous() == null ? "baru" : e.previous().name() + " -> " + e.current().name();
        if (o.message != null && !o.message.isEmpty()) {
            detail += " | " + o.message;
        }
        TransactionLog.getInstance().record(
                new LogEntry(Instant.now(), o.id, e.current().name(), o.method, o.total, detail));
    }
}
