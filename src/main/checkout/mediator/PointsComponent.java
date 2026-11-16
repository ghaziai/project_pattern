package checkout.mediator;

public class PointsComponent extends Component {
    private final long available;
    private long requested;

    public PointsComponent(long available) {
        this.available = available;
    }

    public void request(long points) {
        this.requested = points;
        mediator.notifyChange(this, "POINTS_CHANGED");
    }

    public long requested() {
        return requested;
    }

    public long available() {
        return available;
    }
}
