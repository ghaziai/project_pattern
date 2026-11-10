package checkout.payment;

public class QrisGateway extends PaymentGateway {
    @Override
    protected Payment createPayment() {
        return new QrisPayment();
    }

    @Override
    public String code() {
        return "QRIS";
    }
}
