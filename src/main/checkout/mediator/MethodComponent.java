package checkout.mediator;

public class MethodComponent extends Component {
    private String code = "";

    public void select(String newCode) {
        this.code = newCode == null ? "" : newCode;
        mediator.notifyChange(this, "METHOD_CHANGED");
    }

    public String code() {
        return code;
    }
}
