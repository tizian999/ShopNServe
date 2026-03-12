package shop.serve.ShopNServe.service;

import org.springframework.stereotype.Service;
import shop.serve.ShopNServe.repository.OrderRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderListService {

    private final OrderRepository orderRepository;

    public OrderListService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Map<String, Object>> listOrders() {

        return orderRepository.findAll().stream().map(order -> {

            Map<String, Object> o = new HashMap<>();

            o.put("id", order.getId());
            o.put("user_name", order.getUserName());
            o.put("total_cents", order.getTotalCents());
            o.put("items", order.getItemsJson());
            o.put("created_at", order.getCreatedAt());

            return o;

        }).toList();
    }
}