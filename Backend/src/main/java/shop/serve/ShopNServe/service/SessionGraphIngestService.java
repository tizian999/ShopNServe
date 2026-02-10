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
        } catch (Exception ignored) {}

        String now = Instant.now().toString();

        neo4j.query("""
            MERGE (s:Session {id:$sid})
            ON CREATE SET s.startedAt = datetime($now)

            // optional: für deine alten /traces Queries
            MERGE (t:Trace {id:$sid})
            ON CREATE SET t.startedAt = datetime($now)
            MERGE (s)-[:HAS_TRACE]->(t)

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

        neo4j.query("""
            MATCH (s:Session {id:$sid})-[:TRIGGERED_BY]->(ui:UIComponent {name:$ui})
            MERGE (b:BackendComponent {name:$backend})
            MERGE (c:Capability {name:$cap})

            CREATE (r:RequestedData {
              id:$rid,
              payload:$payload,
              requestedAt: datetime($now)
            })

            MERGE (ui)-[:REQUESTS]->(r)
            MERGE (r)-[:HANDLED_BY]->(b)
            MERGE (b)-[:TRIGGERS_EVENT]->(c)
        """).bindAll(Map.of(
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

        neo4j.query("""
            MATCH (c:Capability {name:$cap})

            CREATE (p:ProvidedData {
              id:$pid,
              payload:$payload,
              providedAt: datetime($now)
            })

            MERGE (c)-[:PROVIDES]->(p)
        """).bindAll(Map.of(
                "cap", capability.name(),
                "pid", provId,
                "payload", payload,
                "now", now
        )).run();

        return provId;
    }

    public void markRequestedCompleted(String requestedId) {
        neo4j.query("""
            MATCH (r:RequestedData {id:$id})
            SET r.completedAt = datetime()
        """).bindAll(Map.of("id", requestedId)).run();
    }

    public void markRequestedFailed(String requestedId, String error) {
        neo4j.query("""
            MATCH (r:RequestedData {id:$id})
            SET r.failedAt = datetime(),
                r.error = $err
        """).bindAll(Map.of(
                "id", requestedId,
                "err", error == null ? "" : error
        )).run();
    }

    public void markProvidedCompleted(String providedId) {
        neo4j.query("""
            MATCH (p:ProvidedData {id:$id})
            SET p.completedAt = datetime()
        """).bindAll(Map.of("id", providedId)).run();
    }

    public void markProvidedFailed(String providedId, String error) {
        neo4j.query("""
            MATCH (p:ProvidedData {id:$id})
            SET p.failedAt = datetime(),
                p.error = $err
        """).bindAll(Map.of(
                "id", providedId,
                "err", error == null ? "" : error
        )).run();
    }
}