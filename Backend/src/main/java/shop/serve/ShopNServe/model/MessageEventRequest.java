package shop.serve.ShopNServe.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public record MessageEventRequest(
        String traceId,
        Sender sender,
        List<Capability> capabilities,
        Object payload
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String traceIdOrNull() {
        return (traceId == null || traceId.isBlank()) ? null : traceId;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> payloadAsMap() {
        if (payload == null) return Map.of();
        if (payload instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return MAPPER.convertValue(payload, Map.class);
    }

    public record Sender(String component, String application) {}
}