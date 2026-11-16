package checkout.model;

import checkout.util.Json;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Permintaan quote/checkout yang sudah divalidasi bentuknya. */
public record OrderRequest(Map<String, Integer> items, String method, String voucher, long points, String customer) {
    public static final String DEFAULT_CUSTOMER = "demo";
    public static final int MAX_QTY = 99;
    public static final int MAX_LINES = 50;

    public static OrderRequest fromJson(Object json) {
        Map<String, Object> m = Json.asObject(json);
        Object rawItems = m.get("items");
        if (!(rawItems instanceof List<?> list)) {
            throw new IllegalArgumentException("Field 'items' harus berupa array");
        }
        if (list.size() > MAX_LINES) {
            throw new IllegalArgumentException("Jumlah baris maksimal " + MAX_LINES);
        }
        Map<String, Integer> items = new LinkedHashMap<>();
        for (Object x : list) {
            Map<String, Object> im = Json.asObject(x);
            Object code = im.get("code");
            if (!(code instanceof String c) || c.isBlank() || c.length() > 32) {
                throw new IllegalArgumentException("Setiap item wajib memiliki 'code' berupa teks (maks. 32 karakter)");
            }
            long qty = wholeNumber(im.get("qty"), "qty");
            if (qty < 1 || qty > MAX_QTY) {
                throw new IllegalArgumentException("Jumlah ('qty') harus 1 sampai " + MAX_QTY);
            }
            int total = items.merge(c.trim(), (int) qty, Integer::sum);
            if (total > MAX_QTY) {
                throw new IllegalArgumentException("Jumlah produk " + c + " melebihi " + MAX_QTY);
            }
        }
        Object method = m.get("method");
        if (!(method instanceof String ms) || ms.isBlank()) {
            throw new IllegalArgumentException("Metode pembayaran wajib diisi");
        }
        Object v = m.get("voucher");
        String voucher = "";
        if (v != null) {
            if (!(v instanceof String vs) || vs.length() > 32) {
                throw new IllegalArgumentException("Voucher harus berupa teks (maks. 32 karakter)");
            }
            voucher = vs.trim();
        }
        long points = m.get("points") == null ? 0 : wholeNumber(m.get("points"), "points");
        if (points < 0 || points > 100_000_000L) {
            throw new IllegalArgumentException("Poin harus berupa bilangan 0 sampai 100.000.000");
        }
        return new OrderRequest(items, ms.trim(), voucher, points, DEFAULT_CUSTOMER);
    }

    private static long wholeNumber(Object o, String field) {
        if (o instanceof Long l) {
            return l;
        }
        if (o instanceof Double d && d == Math.rint(d) && Math.abs(d) < 1e15) {
            return d.longValue();
        }
        throw new IllegalArgumentException("Field '" + field + "' harus berupa bilangan bulat");
    }
}
