package shop.serve.ShopNServe.handler;

import org.springframework.stereotype.Component;
import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.model.MessageEventRequest;
import shop.serve.ShopNServe.service.OrderListService;

import java.util.Map;

@Component
public class OrderHistoryHandler implements CapabilityHandler {

    private final OrderListService orderListService;

    public OrderHistoryHandler(OrderListService orderListService) {
        this.orderListService = orderListService;
    }

    @Override
    public Capability capability() {
        return Capability.OrderHistory;
    }

    @Override
    public BlackboardResponse handle(MessageEventRequest event) {

        return new BlackboardResponse(true, Map.of(
                "action", "OrderHistory",
                "orders", orderListService.listOrders()
        ));
    }
}