package checkout.mediator;

/** Mediator: satu-satunya jalur komunikasi antarkomponen penghitungan keranjang. */
public interface Mediator {
    void notifyChange(Component sender, String event);
}
