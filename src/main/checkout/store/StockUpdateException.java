package checkout.store;

public class StockUpdateException extends RuntimeException {
    public enum Reason {
        INVALID_QUANTITY,
        PRODUCT_NOT_FOUND,
        CAPACITY_EXCEEDED
    }

    private final Reason reason;

    public StockUpdateException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
