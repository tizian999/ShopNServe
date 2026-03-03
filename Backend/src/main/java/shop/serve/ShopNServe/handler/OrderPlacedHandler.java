package shop.serve.ShopNServe.handler;

import org.springframework.stereotype.Component;
import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.model.MessageEventRequest;
import shop.serve.ShopNServe.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.util.List;
import java.util.Map;

@Component
public class OrderPlacedHandler implements CapabilityHandler {

    private final OrderService orderService;
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderPlacedHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public Capability capability() {
        return Capability.OrderPlaced;
    }

    @Override
    public BlackboardResponse handle(MessageEventRequest event) {

        Map<String,Object> payload =
                (Map<String,Object>) event.payload();

        List<Map<String,Object>> items =
                (List<Map<String,Object>>) payload.get("items");

        String username = event.sender().component();

        int totalCents = 0;

        for (Map<String,Object> i : items) {

            Map<String,Object> product =
                    (Map<String,Object>) i.get("product");

            int price = Integer.parseInt(product.get("price_cents").toString());
            int qty = Integer.parseInt(i.get("quantity").toString());

            totalCents += price * qty;
        }

        String itemsJson;

        try {
            itemsJson = mapper.writeValueAsString(items);
        } catch (Exception e) {
            return new BlackboardResponse(false, Map.of("error", "JSON conversion failed"));
        }

        orderService.createOrder(username, totalCents, itemsJson);

        return new BlackboardResponse(true, Map.of(
                "message", "Order stored successfully"
        ));
    }
}