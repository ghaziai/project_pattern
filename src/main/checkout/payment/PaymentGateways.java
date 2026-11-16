package checkout.payment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Daftar Creator yang tersedia. */
public final class PaymentGateways {
    private static final Map<String, PaymentGateway> REGISTRY = new LinkedHashMap<>();

    static {
        for (PaymentGateway g : List.of(new QrisGateway(), new VirtualAccountGateway(),
                new EWalletGateway(), new BankTransferGateway())) {
            REGISTRY.put(g.code(), g);
        }
    }

    private PaymentGateways() {
    }

    public static PaymentGateway forMethod(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Metode pembayaran wajib diisi");
        }
        PaymentGateway g = REGISTRY.get(code.trim().toUpperCase(Locale.ROOT));
        if (g == null) {
            throw new IllegalArgumentException("Metode pembayaran '" + code + "' tidak dikenal");
        }
        return g;
    }

    public static List<Map<String, Object>> available() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (PaymentGateway g : REGISTRY.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", g.code());
            m.put("name", g.displayName());
            m.put("fee", g.feeDescription());
            out.add(m);
        }
        return out;
    }
}
