package shop.serve.ShopNServe.handler;

import org.springframework.stereotype.Service;
import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.model.MessageEventRequest;
import shop.serve.ShopNServe.service.AuthService;

import java.util.Map;

@Service
public class AuthenticationHandler implements CapabilityHandler {

    private final AuthService authService;

    public AuthenticationHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Capability capability() {
        return Capability.Authentication;
    }

    @Override
    public BlackboardResponse handle(MessageEventRequest event) {
        Map<String, Object> payload = event.payloadAsMap();

        String action = String.valueOf(payload.getOrDefault("action", "login"));
        String username = String.valueOf(payload.getOrDefault("username", ""));
        String password = String.valueOf(payload.getOrDefault("password", ""));

        if (username.isBlank() || password.isBlank()) {
            return new BlackboardResponse(false, Map.of(
                    "error", "username/password required"
            ));
        }

        AuthService.AuthResult result =
                "register".equalsIgnoreCase(action)
                        ? authService.register(username, password)
                        : authService.login(username, password);

        if (!result.success()) {
            return new BlackboardResponse(false, Map.of(
                    "error", result.message()
            ));
        }

        return new BlackboardResponse(true, Map.of(
                "username", result.username(),
                "token", result.token()
        ));
    }
}