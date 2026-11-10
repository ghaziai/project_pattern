package checkout.observer;

import checkout.model.Order;
import checkout.model.OrderStatus;

public record StatusEvent(Order order, OrderStatus previous, OrderStatus current) {
}
