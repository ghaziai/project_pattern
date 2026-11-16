package checkout.payment;

public class QrisPayment extends AbstractPayment {
    @Override
    public String name() {
        return "QRIS";
    }

    @Override
    public String feeDescription() {
        return "0,7% dari nominal";
    }

    @Override
    public long fee(long base) {
        return percent(base, 7);
    }

    @Override
    protected PaymentResult authorize(long amount, String reference) {
        return PaymentResult.ok("QR-" + digits(reference, 10), "");
    }
}
