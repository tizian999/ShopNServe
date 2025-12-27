package shop.serve.ShopNServe.service;

import org.springframework.stereotype.Service;
import shop.serve.ShopNServe.handler.CapabilityHandler;
import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.model.MessageEventRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BlackboardService {

    private final AuthService authService;
    private final GraphService graphService;
    private final Map<Capability, CapabilityHandler> handlers;

    public BlackboardService(AuthService authService, GraphService graphService, List<CapabilityHandler> handlerList) {
        this.authService = authService;
        this.graphService = graphService;
        this.handlers = handlerList.stream().collect(Collectors.toMap(CapabilityHandler::capability, h -> h));
    }

    public BlackboardResponse handle(MessageEventRequest event, String authHeader) {
        var caps = event.capabilities() == null ? List.<Capability>of() : event.capabilities();

        if (event.sender() == null || event.sender().component() == null || event.sender().component().isBlank()) {
            return new BlackboardResponse(false, Map.of("error", "Missing sender.component"));
        }
        if (event.sender().application() != null && !event.sender().application().isBlank()) {
            graphService.storeUiBelongsToApp(event.sender().component(), event.sender().application());
        }
        if (caps.contains(Capability.Authentication)) {
            graphService.storeMessageEvent(
                    event.sender().component(),
                    "REQUIRES",
                    Capability.Authentication.name(),
                    event.payload()
            );

            var h = handlers.get(Capability.Authentication);
            if (h == null) return new BlackboardResponse(false, Map.of("error", "No AuthenticationHandler registered"));
            return h.handle(event);
        }
        graphService.storeRequires(event.sender().component(), Capability.Authorization.name());
        graphService.storeMessageEvent(
                event.sender().component(),
                "REQUIRES",
                Capability.Authorization.name(),
                null
        );

        boolean ok = authService.validateJwt(authHeader);
        if (!ok) return new BlackboardResponse(false, Map.of("error", "Unauthorized"));
        for (Capability cap : caps) {
            if (cap == Capability.Authorization || cap == Capability.Authentication) continue;

            graphService.storeRequires(event.sender().component(), cap.name());
            graphService.storeMessageEvent(
                    event.sender().component(),
                    "REQUIRES",
                    cap.name(),
                    event.payload()
            );
        }
        Capability primary = caps.stream()
                .filter(c -> c != Capability.Authorization && c != Capability.Authentication)
                .findFirst()
                .orElse(null);

        if (primary == null) return new BlackboardResponse(true, Map.of("info", "No business capability provided"));

        var handler = handlers.get(primary);
        if (handler == null) return new BlackboardResponse(true, Map.of("info", "No handler for capability: " + primary));
        return handler.handle(event);
    }
}
