package shop.serve.ShopNServe.service;

import org.springframework.stereotype.Service;
import shop.serve.ShopNServe.model.OrderEntity;
import shop.serve.ShopNServe.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void createOrder(String username, int totalCents, String itemsJson) {

        OrderEntity order = new OrderEntity();
        order.setUserName(username);
        order.setTotalCents(totalCents);
        order.setItemsJson(itemsJson);

        orderRepository.save(order);
    }
}