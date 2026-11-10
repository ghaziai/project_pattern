package checkout.payment;

public class BankTransferGateway extends PaymentGateway {
    @Override
    protected Payment createPayment() {
        return new BankTransferAdapter();
    }

    @Override
    public String code() {
        return "BANK";
    }
}
