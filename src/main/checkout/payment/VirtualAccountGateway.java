package checkout.payment;

public class VirtualAccountGateway extends PaymentGateway {
    @Override
    protected Payment createPayment() {
        return new VirtualAccountPayment();
    }

    @Override
    public String code() {
        return "VA";
    }
}
