package shop.serve.ShopNServe.service;

import org.springframework.stereotype.Service;
import shop.serve.ShopNServe.handler.CapabilityHandler;
import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.model.MessageEventRequest;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class BlackboardService {

    private final Map<Capability, CapabilityHandler> handlers = new EnumMap<>(Capability.class);
    private final AuthService authService;

    public BlackboardService(List<CapabilityHandler> handlerList, AuthService authService) {
        this.authService = authService;
        for (CapabilityHandler h : handlerList) {
            handlers.put(h.capability(), h);
        }
    }

    public BlackboardResponse handle(MessageEventRequest event, String authHeader) {
        List<Capability> caps = event.capabilities();
        boolean isAuthentication = caps.contains(Capability.Authentication);

        // Zentraler Auth-Guard: alles außer Authentication braucht JWT
        if (!isAuthentication) {
            if (!authService.validate(authHeader)) {
                return unauthorized(event.traceIdOrNull());
            }
        }

        BlackboardResponse last = null;

        for (Capability c : caps) {
            CapabilityHandler h = handlers.get(c);
            if (h == null) {
                return new BlackboardResponse(false, Map.of(
                        "error", "No handler for capability: " + c.name(),
                        "traceId", event.traceIdOrNull()
                ));
            }

            last = h.handle(event);

            if (last == null) {
                return new BlackboardResponse(false, Map.of(
                        "error", "Handler returned null for: " + c.name(),
                        "traceId", event.traceIdOrNull()
                ));
            }

            if (!last.ok()) return last;
        }

        return last != null
                ? last
                : new BlackboardResponse(true, Map.of("traceId", event.traceIdOrNull()));
    }

    private BlackboardResponse unauthorized(String traceId) {
        return new BlackboardResponse(false, Map.of(
                "error", "Unauthorized",
                "traceId", traceId
        ));
    }
}