package checkout.observer;

/** Observer: pihak yang ingin diberi tahu saat status pesanan berubah. */
public interface OrderObserver {
    String name();

    void onStatusChanged(StatusEvent event);
}
