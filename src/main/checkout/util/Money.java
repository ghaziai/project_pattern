package checkout.util;

/** Format rupiah, mis. 1234567 menjadi "Rp1.234.567". */
public final class Money {
    private Money() {
    }

    public static String format(long value) {
        String digits = Long.toString(Math.abs(value));
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = digits.length() - 1; i >= 0; i--) {
            sb.append(digits.charAt(i));
            if (++count % 3 == 0 && i > 0) {
                sb.append('.');
            }
        }
        return (value < 0 ? "-" : "") + "Rp" + sb.reverse();
    }
}
