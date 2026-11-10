package checkout.facade;

import checkout.observer.AuditObserver;
import checkout.observer.NotificationObserver;
import checkout.observer.OrderStatusPublisher;
import checkout.observer.PointsObserver;
import checkout.observer.StockObserver;
import checkout.singleton.TransactionLog;
import checkout.store.DataStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import checkout.model.LogEntry;

/** Perakitan seluruh komponen aplikasi di sekitar satu {@link DataStore}. */
public class AppContext {
    public final DataStore store;
    public final OrderStatusPublisher publisher = new OrderStatusPublisher();
    public final NotificationObserver notifications = new NotificationObserver();
    public final CheckoutFacade facade;

    public AppContext(DataStore store) {
        this.store = store;
        TransactionLog log = TransactionLog.getInstance();
        log.reset();
        List<LogEntry> old = new ArrayList<>(store.recentLogs(200));
        Collections.reverse(old);
        log.preload(old);
        log.setSink(store::appendLog);

        publisher.subscribe(new StockObserver(store));
        publisher.subscribe(new PointsObserver(store));
        publisher.subscribe(notifications);
        publisher.subscribe(new AuditObserver());
        this.facade = new CheckoutFacade(store, publisher);
    }
}
