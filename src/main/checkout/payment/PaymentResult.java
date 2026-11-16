package checkout.payment;

public record PaymentResult(boolean success, String reference, String message) {
    public static PaymentResult ok(String reference, String message) {
        return new PaymentResult(true, reference, message);
    }

    public static PaymentResult failed(String message) {
        return new PaymentResult(false, "", message);
    }
}
