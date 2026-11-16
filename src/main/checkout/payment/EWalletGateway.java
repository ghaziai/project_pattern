package checkout.payment;

public class EWalletGateway extends PaymentGateway {
    @Override
    protected Payment createPayment() {
        return new EWalletPayment();
    }

    @Override
    public String code() {
        return "EWALLET";
    }
}
