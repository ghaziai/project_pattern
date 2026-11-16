package checkout.payment;

import checkout.util.Money;

/**
 * Template Method: kerangka proses pembayaran dikunci pada {@link #pay}
 * (validasi, otorisasi, struk); langkah otorisasi ditentukan subclass.
 */
public abstract class AbstractPayment implements Payment {

    @Override
    public final PaymentResult pay(long amount, String reference) {
        validate(amount, reference);
        PaymentResult result = authorize(amount, reference);
        if (!result.success()) {
            return result;
        }
        return PaymentResult.ok(result.reference(), receipt(amount, result.reference()));
    }

    /** Langkah konkret bersama; subclass boleh menambah aturan lewat override + super. */
    protected void validate(long amount, String reference) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Nominal pembayaran harus lebih dari nol");
        }
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Referensi pesanan wajib diisi");
        }
    }

    /** Langkah abstrak: simulasi persetujuan oleh penyedia pembayaran. */
    protected abstract PaymentResult authorize(long amount, String reference);

    /** Hook: teks struk. */
    protected String receipt(long amount, String paymentRef) {
        return name() + " berhasil (simulasi): " + Money.format(amount) + ", ref " + paymentRef;
    }

    /** Pembulatan persen ke rupiah terdekat tanpa bilangan pecahan. */
    protected static long percent(long base, long permille) {
        return (base * permille + 500) / 1000;
    }

    /** Referensi deterministik singkat dari kode pesanan. */
    protected static String digits(String reference, int length) {
        long h = 7;
        for (int i = 0; i < reference.length(); i++) {
            h = h * 31 + reference.charAt(i);
        }
        String s = Long.toString(Math.abs(h % 1_000_000_000_000L));
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < length) {
            sb.insert(0, '0');
        }
        return sb.substring(sb.length() - length);
    }
}
