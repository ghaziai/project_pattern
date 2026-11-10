package checkout.mediator;

import java.util.LinkedHashMap;
import java.util.Map;

public class CartComponent extends Component {
    private Map<String, Integer> items = new LinkedHashMap<>();

    public void setItems(Map<String, Integer> newItems) {
        this.items = new LinkedHashMap<>(newItems);
        mediator.notifyChange(this, "ITEMS_CHANGED");
    }

    public Map<String, Integer> items() {
        return items;
    }
}
