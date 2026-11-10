package checkout.mediator;

import checkout.util.Money;

import java.util.Locale;

public class VoucherComponent extends Component {
    public record Result(String code, long discount, String error) {
    }

    private String code = "";

    public void apply(String newCode) {
        this.code = newCode == null ? "" : newCode.trim().toUpperCase(Locale.ROOT);
        mediator.notifyChange(this, "VOUCHER_CHANGED");
    }

    public String code() {
        return code;
    }

    public Result evaluate(long subtotal) {
        switch (code) {
            case "HEMAT10":
                if (subtotal < 50_000) {
                    return new Result(code, 0, "Voucher HEMAT10 membutuhkan belanja minimal " + Money.format(50_000));
                }
                return new Result(code, Math.min(subtotal / 10, 25_000), null);
            case "POTONG5K":
                if (subtotal < 30_000) {
                    return new Result(code, 0, "Voucher POTONG5K membutuhkan belanja minimal " + Money.format(30_000));
                }
                return new Result(code, 5_000, null);
            default:
                return new Result(code, 0, "Voucher '" + code + "' tidak dikenal");
        }
    }
}
