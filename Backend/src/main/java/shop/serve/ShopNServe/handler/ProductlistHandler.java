package shop.serve.ShopNServe.handler;

import org.springframework.stereotype.Service;
import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.model.MessageEventRequest;
import shop.serve.ShopNServe.service.GraphService;
import shop.serve.ShopNServe.service.ProductCatalogService;

import java.util.Map;

@Service
public class ProductlistHandler implements CapabilityHandler {

    private final ProductCatalogService catalogService;
    private final GraphService graphService;

    public ProductlistHandler(ProductCatalogService catalogService, GraphService graphService) {
        this.catalogService = catalogService;
        this.graphService = graphService;
    }

    @Override
    public Capability capability() {
        return Capability.ProductList;
    }

    @Override
    public BlackboardResponse handle(MessageEventRequest event) {
        var products = catalogService.getProducts();

        graphService.storeProvides("OrderService", Capability.ProductList.name());
        graphService.storeCommunicatesWith(event.sender().component(), "OrderService");

        graphService.storeMessageEvent(
                event.sender().component(),
                "PROVIDES",
                Capability.ProductList.name(),
                "",
                "OrderService"
        );


        return new BlackboardResponse(true, Map.of("productList", products));
    }
}
