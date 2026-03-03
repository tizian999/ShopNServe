package shop.serve.ShopNServe.handler;

import org.springframework.stereotype.Component;
import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.model.MessageEventRequest;
import shop.serve.ShopNServe.model.ProductEntity;
import shop.serve.ShopNServe.service.ProductService;

import java.util.List;
import java.util.Map;

@Component
public class ProductListHandler implements CapabilityHandler {

    private final ProductService productService;

    public ProductListHandler(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public Capability capability() {
        return Capability.ProductList;
    }

    @Override
    public BlackboardResponse handle(MessageEventRequest event) {
        // optional: check action
        String action = null;
        try {
            Object payload = event != null ? event.payload() : null;
            if (payload instanceof Map<?, ?> m) {
                Object a = m.get("action");
                action = a != null ? String.valueOf(a) : null;
            }
        } catch (Exception ignored) {}

        if (action != null && !action.equalsIgnoreCase("listProducts")) {
            return new BlackboardResponse(false, Map.of(
                    "error", "Unknown action for ProductList: " + action
            ));
        }

        List<ProductEntity> products = productService.listAll();

        // Frontend erwartet: json.data.productList
        return new BlackboardResponse(true, Map.of(
                "productList", products,
                "backend", "ProductService"
        ));
    }
}