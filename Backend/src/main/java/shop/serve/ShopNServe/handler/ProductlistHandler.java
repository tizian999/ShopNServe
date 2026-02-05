package shop.serve.ShopNServe.handler;

import org.springframework.stereotype.Service;
import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.model.MessageEventRequest;
import shop.serve.ShopNServe.service.GraphService;
import shop.serve.ShopNServe.service.ProductCatalogService;

import java.util.Map;
import java.util.UUID;

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
        String traceId = event.traceIdOrNull();
        String usedTraceId = (traceId == null || traceId.isBlank())
                ? UUID.randomUUID().toString()
                : traceId;

        var products = catalogService.getProducts();

        graphService.storeProvides(usedTraceId, "ProductService", Capability.ProductList.name());
        graphService.storeCommunicatesWith(usedTraceId, event.sender().component(), "ProductService");

        String storedTraceId = graphService.storeMessageEvent(
                event.sender().component(),
                "ProductService",
                "PROVIDES",
                Capability.ProductList.name(),
                Map.of("action", "listProducts"),
                usedTraceId
        );

        return new BlackboardResponse(true, Map.of(
                "productList", products,
                "traceId", storedTraceId
        ));
    }
}