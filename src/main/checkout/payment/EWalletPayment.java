package checkout.payment;

import checkout.util.Money;

public class EWalletPayment extends AbstractPayment {
    /** Simulasi batas saldo e-wallet; di atas ini pembayaran ditolak (untuk mencoba status GAGAL). */
    public static final long SIMULATED_BALANCE = 1_000_000;

    @Override
    public String name() {
        return "E-Wallet";
    }

    @Override
    public String feeDescription() {
        return "1,5% dari nominal; simulasi saldo maks. " + Money.format(SIMULATED_BALANCE);
    }

    @Override
    public long fee(long base) {
        return percent(base, 15);
    }

    @Override
    protected PaymentResult authorize(long amount, String reference) {
        if (amount > SIMULATED_BALANCE) {
            return PaymentResult.failed("Saldo e-wallet (simulasi) tidak mencukupi untuk "
                    + Money.format(amount));
        }
        return PaymentResult.ok("EW-" + digits(reference, 10), "");
    }
}
