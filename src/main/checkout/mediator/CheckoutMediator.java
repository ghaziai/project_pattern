package checkout.mediator;

import checkout.model.OrderLine;
import checkout.model.Product;
import checkout.model.Quote;
import checkout.payment.PaymentGateway;
import checkout.payment.PaymentGateways;
import checkout.util.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** ConcreteMediator: seluruh aturan penghitungan antar komponen keranjang berada di sini. */
public class CheckoutMediator implements Mediator {
    /** Poin hanya boleh menutup sebagian tagihan agar total pembayaran selalu lebih dari nol. */
    private static final int MAX_POINTS_PERCENT = 50;

    private final CartComponent cart;
    private final VoucherComponent voucher;
    private final PointsComponent points;
    private final MethodComponent method;
    private final StockComponent stock;
    private final SummaryComponent summary;

    public CheckoutMediator(CartComponent cart, VoucherComponent voucher, PointsComponent points,
            MethodComponent method, StockComponent stock, SummaryComponent summary) {
        this.cart = cart;
        this.voucher = voucher;
        this.points = points;
        this.method = method;
        this.stock = stock;
        this.summary = summary;
        for (Component c : new Component[] {cart, voucher, points, method, stock, summary}) {
            c.setMediator(this);
        }
    }

    @Override
    public void notifyChange(Component sender, String event) {
        switch (event) {
            case "ITEMS_CHANGED", "VOUCHER_CHANGED", "POINTS_CHANGED", "METHOD_CHANGED" -> recalculate();
            default -> throw new IllegalArgumentException("Peristiwa tidak dikenal: " + event);
        }
    }

    private void recalculate() {
        List<String> errors = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        List<OrderLine> lines = new ArrayList<>();
        long subtotal = 0;

        if (cart.items().isEmpty()) {
            errors.add("Keranjang kosong");
        }
        for (Map.Entry<String, Integer> e : cart.items().entrySet()) {
            Product p = stock.product(e.getKey());
            if (p == null) {
                errors.add("Produk '" + e.getKey() + "' tidak ditemukan");
                continue;
            }
            String stockError = stock.check(p, e.getValue());
            if (stockError != null) {
                errors.add(stockError);
            }
            OrderLine line = new OrderLine(p.code(), p.name(), p.price(), e.getValue());
            lines.add(line);
            subtotal += line.amount();
        }

        long discount = 0;
        String appliedVoucher = "";
        if (!voucher.code().isEmpty()) {
            VoucherComponent.Result r = voucher.evaluate(subtotal);
            if (r.error() != null) {
                errors.add(r.error());
            } else {
                discount = r.discount();
                appliedVoucher = r.code();
            }
        }

        long afterVoucher = subtotal - discount;
        long cap = afterVoucher * MAX_POINTS_PERCENT / 100;
        long used = Math.min(Math.min(points.requested(), points.available()), cap);
        if (points.requested() > used) {
            notes.add("Poin yang dipakai disesuaikan menjadi " + used + " (tersedia " + points.available()
                    + ", maksimal " + MAX_POINTS_PERCENT + "% tagihan)");
        }
        long base = afterVoucher - used;

        long fee = 0;
        String methodCode = method.code();
        try {
            PaymentGateway gateway = PaymentGateways.forMethod(methodCode);
            methodCode = gateway.code();
            fee = gateway.estimateFee(base);
        } catch (IllegalArgumentException ex) {
            errors.add(ex.getMessage());
        }
        long total = base + fee;
        if (errors.isEmpty() && total > 0) {
            notes.add("Total yang dibayar: " + Money.format(total));
        }
        summary.show(new Quote(lines, subtotal, discount, appliedVoucher, used, points.available(),
                methodCode, fee, total, errors, notes));
    }
}
