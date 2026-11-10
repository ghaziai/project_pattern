package checkout.payment;

/**
 * Pustaka "pihak ketiga" bergaya lama dengan antarmuka yang tidak cocok dengan {@link Payment}.
 * Sengaja dibiarkan apa adanya; dijembatani oleh {@link BankTransferAdapter}. Seluruhnya SIMULASI.
 */
public class LegacyBankGateway {
    public static final double LIMIT = 10_000_000;
    private String lastCode = "";

    /** Mengembalikan 0 bila sukses; kode lain menandakan kesalahan. */
    public int kirimDana(String noRekening, double nominal) {
        if (noRekening == null || noRekening.isEmpty()) {
            return 11;
        }
        if (nominal <= 0) {
            return 12;
        }
        if (nominal > LIMIT) {
            return 13;
        }
        lastCode = "TRF" + Math.abs(noRekening.hashCode() % 1_000_000) + (long) nominal;
        return 0;
    }

    public String getKodeTransaksi() {
        return lastCode;
    }

    public String pesanKesalahan(int kode) {
        return switch (kode) {
            case 11 -> "Nomor rekening kosong";
            case 12 -> "Nominal tidak valid";
            case 13 -> "Nominal melebihi batas transfer";
            default -> "Kesalahan tidak diketahui (" + kode + ")";
        };
    }
}
