package shop.serve.ShopNServe.handler;

import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.model.MessageEventRequest;

public interface CapabilityHandler {
    Capability capability();
    BlackboardResponse handle(MessageEventRequest event);
}
