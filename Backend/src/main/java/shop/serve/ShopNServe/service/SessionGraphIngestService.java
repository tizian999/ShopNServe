package shop.serve.ShopNServe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.model.MessageEventRequest;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class SessionGraphIngestService {

    private final Neo4jClient neo4j;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public SessionGraphIngestService(Neo4jClient neo4j) {
        this.neo4j = neo4j;
    }

    private boolean swapRequestedAndProvided(Capability capability) {
        return capability == Capability.Authentication
                || capability == Capability.OrderPlaced;
    }

    private String requestNodeLabel(Capability capability) {
        return swapRequestedAndProvided(capability) ? "ProvidedData" : "RequestedData";
    }

    private String responseNodeLabel(Capability capability) {
        return swapRequestedAndProvided(capability) ? "RequestedData" : "ProvidedData";
    }

    public String ensureSession(String traceIdOrNull) {
        String sid = (traceIdOrNull == null || traceIdOrNull.isBlank())
                ? UUID.randomUUID().toString()
                : traceIdOrNull.trim();

        String now = Instant.now().toString();
        neo4j.query("""
            MERGE (s:Session {id:$sid})
            ON CREATE SET s.startedAt = datetime($now)
        """).bindAll(Map.of("sid", sid, "now", now)).run();

        return sid;
    }

    public String ensureSessionAndUi(MessageEventRequest event) {
        String sessionId = event.traceIdOrNull();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        } else {
            sessionId = sessionId.trim();
        }

        String ui = "unknown";
        try {
            if (event.sender() != null && event.sender().component() != null) {
                ui = event.sender().component().replace(".vue", "");
                if (ui.isBlank()) ui = "unknown";
            }
        } catch (Exception ignored) {
        }

        String now = Instant.now().toString();

        neo4j.query("""
            MERGE (s:Session {id:$sid})
            ON CREATE SET s.startedAt = datetime($now)

            MERGE (ui:UIComponent {name:$ui})
            MERGE (s)-[:TRIGGERED_BY]->(ui)
        """).bindAll(Map.of(
                "sid", sessionId,
                "ui", ui,
                "now", now
        )).run();

        return sessionId;
    }

    public String createRequestedData(
            String sessionId,
            String uiComponent,
            String backendComponent,
            Capability capability,
            Object requestPayload
    ) {
        String reqId = UUID.randomUUID().toString();
        String now = Instant.now().toString();

        String payload;
        try {
            payload = requestPayload == null ? null : MAPPER.writeValueAsString(requestPayload);
        } catch (Exception e) {
            payload = String.valueOf(requestPayload);
        }

        String requestLabel = requestNodeLabel(capability);

        neo4j.query("""
            MATCH (s:Session {id:$sid})-[:TRIGGERED_BY]->(ui:UIComponent {name:$ui})
            MERGE (b:BackendComponent {name:$backend})
            MERGE (c:Capability {name:$cap})

            CREATE (r:%s {
              id:$rid,
              payload:$payload,
              requestedAt: datetime($now)
            })

            MERGE (ui)-[:REQUESTS]->(r)
            MERGE (r)-[:HANDLED_BY]->(b)
            MERGE (b)-[:TRIGGERS_EVENT]->(c)
        """.formatted(requestLabel)).bindAll(Map.of(
                "sid", sessionId,
                "ui", uiComponent,
                "backend", backendComponent,
                "cap", capability.name(),
                "rid", reqId,
                "payload", payload,
                "now", now
        )).run();

        return reqId;
    }

    public String createProvidedData(
            Capability capability,
            Object responsePayload
    ) {
        String provId = UUID.randomUUID().toString();
        String now = Instant.now().toString();

        String payload;
        try {
            payload = responsePayload == null ? null : MAPPER.writeValueAsString(responsePayload);
        } catch (Exception e) {
            payload = String.valueOf(responsePayload);
        }

        String responseLabel = responseNodeLabel(capability);

        neo4j.query("""
            MATCH (c:Capability {name:$cap})

            CREATE (p:%s {
              id:$pid,
              payload:$payload,
              providedAt: datetime($now)
            })

            MERGE (c)-[:PROVIDES]->(p)
        """.formatted(responseLabel)).bindAll(Map.of(
                "cap", capability.name(),
                "pid", provId,
                "payload", payload,
                "now", now
        )).run();

        return provId;
    }

    public void markRequestedCompleted(String requestedId) {
        neo4j.query("""
            MATCH (n {id:$id})
            WHERE n:RequestedData OR n:ProvidedData
            SET n.completedAt = datetime()
        """).bindAll(Map.of("id", requestedId)).run();
    }

    public void markRequestedFailed(String requestedId, String error) {
        neo4j.query("""
            MATCH (n {id:$id})
            WHERE n:RequestedData OR n:ProvidedData
            SET n.failedAt = datetime(),
                n.error = $err
        """).bindAll(Map.of(
                "id", requestedId,
                "err", error == null ? "" : error
        )).run();
    }

    public void markProvidedCompleted(String providedId) {
        neo4j.query("""
            MATCH (n {id:$id})
            WHERE n:RequestedData OR n:ProvidedData
            SET n.completedAt = datetime()
        """).bindAll(Map.of("id", providedId)).run();
    }

    public void markProvidedFailed(String providedId, String error) {
        neo4j.query("""
            MATCH (n {id:$id})
            WHERE n:RequestedData OR n:ProvidedData
            SET n.failedAt = datetime(),
                n.error = $err
        """).bindAll(Map.of(
                "id", providedId,
                "err", error == null ? "" : error
        )).run();
    }
}