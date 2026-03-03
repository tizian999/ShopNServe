package shop.serve.ShopNServe.handler;

import org.springframework.stereotype.Component;
import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.model.MessageEventRequest;
import shop.serve.ShopNServe.repository.OrderRepository;

import java.util.Map;

@Component
public class OrderHistoryHandler implements CapabilityHandler {

    private final OrderRepository orderRepository;

    public OrderHistoryHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Capability capability() {
        return Capability.OrderHistory;
    }

    @Override
    public BlackboardResponse handle(MessageEventRequest event) {

        var orders = orderRepository.findAll();

        var mapped = orders.stream().map(o -> Map.of(
                "id", o.getId(),
                "total_cents", o.getTotalCents(),
                "items", o.getItemsJson(),
                "created_at", o.getCreatedAt()
        )).toList();

        return new BlackboardResponse(true, Map.of(
                "action", "ListOrderHistory",
                "orders", mapped
        ));
    }
}