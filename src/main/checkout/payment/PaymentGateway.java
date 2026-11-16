package checkout.payment;

/** Factory Method: Creator yang menentukan {@link Payment} konkret lewat {@link #createPayment()}. */
public abstract class PaymentGateway {

    /** Factory method. */
    protected abstract Payment createPayment();

    public abstract String code();

    public final String displayName() {
        return createPayment().name();
    }

    public final String feeDescription() {
        return createPayment().feeDescription();
    }

    public final long estimateFee(long base) {
        return createPayment().fee(base);
    }

    /** Operasi umum yang memakai produk factory method; kegagalan dikembalikan sebagai hasil, bukan exception. */
    public final PaymentResult charge(long amount, String reference) {
        try {
            return createPayment().pay(amount, reference);
        } catch (RuntimeException e) {
            return PaymentResult.failed(e.getMessage() == null ? "Pembayaran gagal" : e.getMessage());
        }
    }
}
