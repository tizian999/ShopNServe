package shop.serve.ShopNServe.handler;

import org.springframework.stereotype.Service;
import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.model.MessageEventRequest;
import shop.serve.ShopNServe.service.AuthService;
import shop.serve.ShopNServe.service.GraphService;

import java.util.Map;

@Service
public class AuthenticationHandler implements CapabilityHandler {

    private final AuthService authService;
    private final GraphService graphService;

    public AuthenticationHandler(AuthService authService, GraphService graphService) {
        this.authService = authService;
        this.graphService = graphService;
    }

    @Override
    public Capability capability() {
        return Capability.Authentication;
    }

    @Override
    public BlackboardResponse handle(MessageEventRequest event) {
        Map<String, Object> payload = event.payloadAsMap();
        String traceId = event.traceIdOrNull();

        String action = String.valueOf(payload.getOrDefault("action", "login"));
        String username = String.valueOf(payload.getOrDefault("username", ""));
        String password = String.valueOf(payload.getOrDefault("password", ""));

        if (username.isBlank() || password.isBlank()) {
            return new BlackboardResponse(false, Map.of("error", "username/password required"));
        }

        AuthService.AuthResult result =
                "register".equalsIgnoreCase(action)
                        ? authService.register(username, password)
                        : authService.login(username, password);

        graphService.storeProvides("AuthService", Capability.Authentication.name());
        graphService.storeCommunicatesWith(event.sender().component(), "AuthService");

        String usedTraceId = graphService.storeMessageEvent(
                event.sender().component(),
                "AuthService",
                "PROVIDES",
                Capability.Authentication.name(),
                Map.of("username", username, "action", action),
                traceId
        );

        if (!result.success()) {
            return new BlackboardResponse(false, Map.of(
                    "error", result.message(),
                    "traceId", usedTraceId
            ));
        }

        return new BlackboardResponse(true, Map.of(
                "username", result.username(),
                "token", result.token(),
                "traceId", usedTraceId
        ));
    }
}