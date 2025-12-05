package shop.serve.ShopNServe.controller;

import org.springframework.web.bind.annotation.*;
import shop.serve.ShopNServe.model.Order;
import shop.serve.ShopNServe.model.UserAccount;
import shop.serve.ShopNServe.repository.OrderRepository;
import shop.serve.ShopNServe.repository.UserAccountRepository;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin("*")
public class OrderController {

    private final OrderRepository orderRepo;
    private final UserAccountRepository userRepo;

    public OrderController(OrderRepository orderRepo, UserAccountRepository userRepo) {
        this.orderRepo = orderRepo;
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<Map<String,Object>> all() {
        return orderRepo.findAll().stream().map(o -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("productName", o.getProductName());
            m.put("quantity", o.getQuantity());
            m.put("status", o.getStatus());
            m.put("username", o.getUser() != null ? o.getUser().getUsername() : null);
            return m;
        }).collect(Collectors.toList());
    }

    @PostMapping
    public Map<String,Object> create(@RequestBody Map<String,Object> body) {
        String username = (String) body.get("username");
        String productName = (String) body.get("productName");
        Object qtyObj = body.getOrDefault("quantity", 1);
        int quantity = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 1;
        UserAccount user = userRepo.findByUsername(username);
        if (user == null) {
            Map<String,Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("message", "User not found");
            return resp;
        }
        Order order = new Order(productName, quantity, "CREATED", user);
        orderRepo.save(order);
        Map<String,Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("id", order.getId());
        resp.put("status", order.getStatus());
        return resp;
    }

    @PostMapping("/{id}/confirm")
    public Map<String,Object> confirm(@PathVariable Long id) {
        Order order = orderRepo.findById(id).orElse(null);
        if (order == null) {
            Map<String,Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("message", "Order not found");
            return resp;
        }
        order.setStatus("CONFIRMED");
        orderRepo.save(order);
        Map<String,Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("id", order.getId());
        resp.put("status", order.getStatus());
        return resp;
    }
}
