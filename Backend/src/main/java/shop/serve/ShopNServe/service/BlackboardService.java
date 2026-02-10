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

        if (event.capabilities() == null || event.capabilities().isEmpty())
            return error("capabilities required", event.traceIdOrNull());

        if (event.sender() == null || event.sender().component() == null || event.sender().component().isBlank())
            return error("sender.component required", event.traceIdOrNull());

        boolean isAuthRequest = event.capabilities().contains(Capability.Authentication);
        if (!isAuthRequest && !authService.validate(authHeader)) {
            return unauthorized(event.traceIdOrNull());
        }
        String sessionId = sessionGraph.ensureSessionAndUi(event);

        String uiName = event.sender().component().replace(".vue", "");
        if (uiName.isBlank()) uiName = "unknown";

        BlackboardResponse last = null;
        for (Capability cap : event.capabilities()) {

            CapabilityHandler h = handlers.get(cap);
            if (h == null) {
                String reqId = sessionGraph.createRequestedData(
                        sessionId,
                        uiName,
                        backendFor(cap),
                        cap,
                        event.payload()
                );
                sessionGraph.markRequestedFailed(reqId, "No handler for capability: " + cap.name());
                return error("No handler for capability: " + cap.name(), sessionId);
            }
            String requestedId = sessionGraph.createRequestedData(
                    sessionId,
                    uiName,
                    backendFor(cap),
                    cap,
                    event.payload()
            );

            BlackboardResponse handlerResp;
            try {
                handlerResp = h.handle(event);
            } catch (Exception ex) {
                sessionGraph.markRequestedFailed(
                        requestedId,
                        ex.getClass().getSimpleName() + ": " + ex.getMessage()
                );
                return new BlackboardResponse(false, Map.of(
                        "error", "Handler exception for: " + cap.name(),
                        "message", String.valueOf(ex.getMessage()),
                        "traceId", sessionId
                ));
            }

            if (handlerResp == null) {
                sessionGraph.markRequestedFailed(requestedId, "Handler returned null for: " + cap.name());
                return error("Handler returned null for: " + cap.name(), sessionId);
            }

            last = handlerResp;

            if (handlerResp.ok()) {
                sessionGraph.markRequestedCompleted(requestedId);
            } else {
                sessionGraph.markRequestedFailed(requestedId, extractError(handlerResp));
                return handlerResp;
            }

            String providedId = sessionGraph.createProvidedData(cap, handlerResp.data());
            sessionGraph.markProvidedCompleted(providedId);
        }

        return last != null ? last : new BlackboardResponse(true, Map.of("traceId", sessionId));
    }

    private String backendFor(Capability cap) {
        if (cap == null) return "UnknownService";
        return switch (cap) {
            case Authentication, Authorization -> "AuthService";
            case ProductList, OrderPlaced -> "ProductListService";
            default -> cap.name() + "Service";
        };
    }

    private String extractError(BlackboardResponse resp) {
        try {
            Object o = resp.data() != null ? resp.data().get("error") : null;
            return o != null ? String.valueOf(o) : "Unknown error";
        } catch (Exception e) {
            return "Unknown error";
        }
    }

    private BlackboardResponse error(String msg, String traceId) {
        return new BlackboardResponse(false, Map.of("error", msg, "traceId", traceId));
    }

    private BlackboardResponse unauthorized(String traceId) {
        return new BlackboardResponse(false, Map.of("error", "Unauthorized", "traceId", traceId));
    }
}