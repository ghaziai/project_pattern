package checkout.payment;

import checkout.util.Money;

/** Adapter: menyesuaikan {@link LegacyBankGateway} ke kontrak {@link Payment}. */
public class BankTransferAdapter implements Payment {
    private static final long FLAT_FEE = 2_500;
    private static final String MERCHANT_ACCOUNT = "1234567890";
    private final LegacyBankGateway legacy;

    public BankTransferAdapter() {
        this(new LegacyBankGateway());
    }

    public BankTransferAdapter(LegacyBankGateway legacy) {
        this.legacy = legacy;
    }

    @Override
    public String name() {
        return "Transfer Bank";
    }

    @Override
    public String feeDescription() {
        return "Rp2.500 tetap; batas " + Money.format((long) LegacyBankGateway.LIMIT);
    }

    @Override
    public long fee(long base) {
        return FLAT_FEE;
    }

    @Override
    public PaymentResult pay(long amount, String reference) {
        if (reference == null || reference.isBlank()) {
            return PaymentResult.failed("Referensi pesanan wajib diisi");
        }
        int code = legacy.kirimDana(MERCHANT_ACCOUNT, (double) amount);
        if (code != 0) {
            return PaymentResult.failed(legacy.pesanKesalahan(code));
        }
        String ref = legacy.getKodeTransaksi();
        return PaymentResult.ok(ref, name() + " berhasil (simulasi): " + Money.format(amount) + ", ref " + ref);
    }
}
