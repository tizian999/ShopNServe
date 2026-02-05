package shop.serve.ShopNServe.handler;

import org.springframework.stereotype.Service;
import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.model.MessageEventRequest;
import shop.serve.ShopNServe.service.GraphService;

import java.util.Map;
import java.util.UUID;

@Service
public class AuthorizationHandler implements CapabilityHandler {

    private final GraphService graphService;

    public AuthorizationHandler(GraphService graphService) {
        this.graphService = graphService;
    }

    @Override
    public Capability capability() {
        return Capability.Authorization;
    }

    @Override
    public BlackboardResponse handle(MessageEventRequest event) {

        String traceId = event.traceIdOrNull();
        String usedTraceId = (traceId == null || traceId.isBlank())
                ? UUID.randomUUID().toString()
                : traceId;

        graphService.storeProvides(usedTraceId, "AuthService", Capability.Authorization.name());
        graphService.storeCommunicatesWith(usedTraceId, event.sender().component(), "AuthService");

        graphService.storeMessageEvent(
                event.sender().component(),
                "AuthService",
                "PROVIDES",
                Capability.Authorization.name(),
                Map.of("action", "check"),
                usedTraceId
        );

        return new BlackboardResponse(true, Map.of(
                "authorized", true,
                "traceId", usedTraceId
        ));
    }
}