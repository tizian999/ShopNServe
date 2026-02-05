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
    private final SessionGraphIngestService sessionGraph;

    public BlackboardService(
            List<CapabilityHandler> handlerList,
            AuthService authService,
            SessionGraphIngestService sessionGraph
    ) {
        this.authService = authService;
        this.sessionGraph = sessionGraph;

        for (CapabilityHandler h : handlerList) {
            handlers.put(h.capability(), h);
        }
    }

    public BlackboardResponse handle(MessageEventRequest event, String authHeader) {
        if (event == null) return error("Missing body", null);

        if (event.capabilities() == null || event.capabilities().isEmpty()) {
            return error("capabilities required", event.traceIdOrNull());
        }

        if (event.sender() == null || event.sender().component() == null || event.sender().component().isBlank()) {
            return error("sender.component required", event.traceIdOrNull());
        }

        boolean isAuthRequest = event.capabilities().contains(Capability.Authentication);
        if (!isAuthRequest && !authService.validate(authHeader)) {
            return unauthorized(event.traceIdOrNull());
        }
        String sessionId = sessionGraph.ensureSession(event.traceIdOrNull());

        BlackboardResponse last = null;
        for (Capability cap : event.capabilities()) {

            CapabilityHandler h = handlers.get(cap);
            if (h == null) {
                String stepId = sessionGraph.ingestInvocation(sessionId, event, cap, Map.of(
                        "error", "No handler for capability: " + cap.name()
                ));
                sessionGraph.setStepStatus(stepId, "failed", "No handler for capability: " + cap.name());
                return error("No handler for capability: " + cap.name(), sessionId);
            }

            BlackboardResponse handlerResp;
            try {
                handlerResp = h.handle(event);
            } catch (Exception ex) {
                String stepId = sessionGraph.ingestInvocation(sessionId, event, cap, Map.of(
                        "error", ex.getClass().getSimpleName(),
                        "message", String.valueOf(ex.getMessage())
                ));
                sessionGraph.setStepStatus(stepId, "failed", ex.getClass().getSimpleName() + ": " + ex.getMessage());

                return new BlackboardResponse(false, Map.of(
                        "error", "Handler exception for: " + cap.name(),
                        "message", String.valueOf(ex.getMessage()),
                        "traceId", sessionId
                ));
            }

            if (handlerResp == null) {
                String stepId = sessionGraph.ingestInvocation(sessionId, event, cap, Map.of(
                        "error", "Handler returned null for: " + cap.name()
                ));
                sessionGraph.setStepStatus(stepId, "failed", "Handler returned null for: " + cap.name());
                return error("Handler returned null for: " + cap.name(), sessionId);
            }

            last = handlerResp;
            String stepId = sessionGraph.ingestInvocation(sessionId, event, cap, handlerResp.data());

            if (handlerResp.ok()) {
                sessionGraph.setStepStatus(stepId, "complete", null);
            } else {
                String err = null;
                try {
                    Object o = handlerResp.data() != null ? handlerResp.data().get("error") : null;
                    err = o != null ? String.valueOf(o) : "Unknown error";
                } catch (Exception ignored) {
                    err = "Unknown error";
                }
                sessionGraph.setStepStatus(stepId, "failed", err);
                return handlerResp;
            }
        }

        return last != null ? last : new BlackboardResponse(true, Map.of("traceId", sessionId));
    }

    private BlackboardResponse error(String msg, String traceId) {
        return new BlackboardResponse(false, Map.of("error", msg, "traceId", traceId));
    }

    private BlackboardResponse unauthorized(String traceId) {
        return new BlackboardResponse(false, Map.of("error", "Unauthorized", "traceId", traceId));
    }
}