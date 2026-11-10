package checkout.mediator;

import checkout.model.OrderRequest;
import checkout.model.Quote;
import checkout.store.DataStore;

/** Merangkai komponen + mediator untuk satu permintaan, lalu menghasilkan {@link Quote}. */
public final class CheckoutSession {
    private CheckoutSession() {
    }

    public static Quote quote(DataStore store, OrderRequest req) {
        CartComponent cart = new CartComponent();
        VoucherComponent voucher = new VoucherComponent();
        PointsComponent points = new PointsComponent(store.points(req.customer()));
        MethodComponent method = new MethodComponent();
        StockComponent stock = new StockComponent(store.products());
        SummaryComponent summary = new SummaryComponent();
        new CheckoutMediator(cart, voucher, points, method, stock, summary);

        method.select(req.method());
        voucher.apply(req.voucher());
        points.request(req.points());
        cart.setItems(req.items());
        return summary.quote();
    }
}
