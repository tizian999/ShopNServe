package shop.serve.ShopNServe.handler;

import org.springframework.stereotype.Service;
import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.model.MessageEventRequest;
import shop.serve.ShopNServe.service.AuthService;
import shop.serve.ShopNServe.service.GraphService;

import java.util.Map;

@Service
public class AuthorizationHandler implements CapabilityHandler {

    private final AuthService authService;
    private final GraphService graphService;

    public AuthorizationHandler(AuthService authService, GraphService graphService) {
        this.authService = authService;
        this.graphService = graphService;
    }

    @Override
    public Capability capability() {
        return Capability.Authorization;
    }

    @Override
    public BlackboardResponse handle(MessageEventRequest event) {
        String traceId = event.traceIdOrNull();
        graphService.storeProvides("AuthService", Capability.Authorization.name());
        graphService.storeCommunicatesWith(event.sender().component(), "AuthService");

        String usedTraceId = graphService.storeMessageEvent(
                event.sender().component(),
                "AuthService",
                "PROVIDES",
                Capability.Authorization.name(),
                Map.of("action", "check"),
                traceId
        );

        return new BlackboardResponse(true, Map.of(
                "authorized", true,
                "traceId", usedTraceId
        ));
    }
}