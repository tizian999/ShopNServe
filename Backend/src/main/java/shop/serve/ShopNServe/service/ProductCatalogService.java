package shop.serve.ShopNServe.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProductCatalogService {
    public List<Map<String, Object>> getProducts() {
        return List.of(
                Map.of("id", 1, "name", "Cola", "price", 0.99),
                Map.of("id", 2, "name", "Apfel", "price", 0.50),
                Map.of("id", 3, "name", "Käse", "price", 2.90)
        );
    }
}
