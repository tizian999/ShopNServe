package shop.serve.ShopNServe.handler;

import org.springframework.stereotype.Service;
import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.model.MessageEventRequest;

import java.util.Map;

@Service
public class AuthorizationHandler implements CapabilityHandler {

    public AuthorizationHandler() {
    }

    @Override
    public Capability capability() {
        return Capability.Authorization;
    }

    @Override
    public BlackboardResponse handle(MessageEventRequest event) {

        return new BlackboardResponse(true, Map.of(
                "authorized", true
        ));
    }
}