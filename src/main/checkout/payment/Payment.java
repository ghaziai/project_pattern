package checkout.payment;

/** Kontrak seluruh metode pembayaran (hasil Factory Method). Seluruhnya SIMULASI. */
public interface Payment {
    String name();

    /** Deskripsi biaya untuk ditampilkan di UI. */
    String feeDescription();

    /** Biaya admin untuk nominal dasar tertentu. */
    long fee(long base);

    PaymentResult pay(long amount, String reference);
}
