package checkout.payment;

public class VirtualAccountPayment extends AbstractPayment {
    private static final long FLAT_FEE = 4_000;

    @Override
    public String name() {
        return "Virtual Account";
    }

    @Override
    public String feeDescription() {
        return "Rp4.000 tetap";
    }

    @Override
    public long fee(long base) {
        return FLAT_FEE;
    }

    @Override
    protected PaymentResult authorize(long amount, String reference) {
        return PaymentResult.ok("VA-8808" + digits(reference, 9), "");
    }
}
