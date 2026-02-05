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
        String id = (traceIdOrNull == null || traceIdOrNull.isBlank())
                ? UUID.randomUUID().toString()
                : traceIdOrNull.trim();

        String now = Instant.now().toString();

        neo4j.query("""
            MERGE (s:Session {id: $id})
            ON CREATE SET s.startedAt = datetime($now)
        """).bindAll(Map.of("id", id, "now", now)).run();

        neo4j.query("""
            MERGE (t:Trace {id: $id})
            ON CREATE SET t.startedAt = datetime($now)
        """).bindAll(Map.of("id", id, "now", now)).run();

        neo4j.query("""
            MATCH (s:Session {id:$id})
            MATCH (t:Trace {id:$id})
            MERGE (s)-[:HAS_TRACE]->(t)
        """).bindAll(Map.of("id", id)).run();

        return id;
    }
    public String ingestInvocation(
            String sessionId,
            MessageEventRequest event,
            Capability cap,
            Map<String, Object> handlerData
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = ensureSession(event != null ? event.traceIdOrNull() : null);
        }

        String now = Instant.now().toString();
        String stepId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();

        String uiName = safeUiName(event);
        String backendName = inferBackendName(cap, handlerData);
        String capName = cap != null ? cap.name() : "Unknown";

        String payloadJson;
        try {
            payloadJson = event != null && event.payload() != null
                    ? MAPPER.writeValueAsString(event.payload())
                    : null;
        } catch (Exception e) {
            payloadJson = String.valueOf(event.payload());
        }
        neo4j.query("""
            MATCH (s:Session {id:$sid})
            CREATE (st:Step {
              id: $stepId,
              sessionId: $sid,
              capability: $cap,
              status: "working",
              createdAt: datetime($now)
            })
            MERGE (s)-[:HAS_STEP]->(st)
        """).bindAll(Map.of(
                "sid", sessionId,
                "stepId", stepId,
                "cap", capName,
                "now", now
        )).run();
        neo4j.query("""
            MATCH (s:Session {id:$sid})
            MATCH (st:Step {id:$stepId})
            OPTIONAL MATCH (s)-[:FIRST_STEP]->(first:Step)
            OPTIONAL MATCH (s)-[:HAS_STEP]->(prev:Step)
            WITH s, st, first, prev
            ORDER BY prev.createdAt DESC
            WITH s, st, first, collect(prev)[0] AS last
            FOREACH (_ IN CASE WHEN first IS NULL THEN [1] ELSE [] END |
                MERGE (s)-[:FIRST_STEP]->(st)
            )
            FOREACH (_ IN CASE WHEN first IS NOT NULL AND last IS NOT NULL AND last.id <> st.id THEN [1] ELSE [] END |
                MERGE (last)-[:NEXT]->(st)
            )
        """).bindAll(Map.of(
                "sid", sessionId,
                "stepId", stepId
        )).run();
        neo4j.query("""
            MATCH (st:Step {id:$stepId})
            CREATE (e:MessageEvent {
              id: $eid,
              sessionId: $sid,
              capability: $cap,
              payload: $payload,
              createdAt: datetime($now)
            })
            MERGE (st)-[:HAS_EVENT]->(e)
        """).bindAll(Map.of(
                "stepId", stepId,
                "eid", eventId,
                "sid", sessionId,
                "cap", capName,
                "payload", payloadJson,
                "now", now
        )).run();
        if (!"unknown".equalsIgnoreCase(uiName)) {
            neo4j.query("""
                MATCH (e:MessageEvent {id:$eid})
                MERGE (u:UIComponent {sessionId:$sid, name:$ui})
                MERGE (u)-[:REQUESTED]->(e)
            """).bindAll(Map.of(
                    "eid", eventId,
                    "sid", sessionId,
                    "ui", uiName
            )).run();
        }
        if (!"unknown".equalsIgnoreCase(backendName)) {
            neo4j.query("""
                MATCH (e:MessageEvent {id:$eid})
                MERGE (b:BackendComponent {sessionId:$sid, name:$backend})
                MERGE (e)-[:HANDLED_BY]->(b)
            """).bindAll(Map.of(
                    "eid", eventId,
                    "sid", sessionId,
                    "backend", backendName
            )).run();
        }
        neo4j.query("""
            MATCH (e:MessageEvent {id:$eid})
            MERGE (c:Capability {sessionId:$sid, name:$cap})
            MERGE (e)-[:ABOUT]->(c)
        """).bindAll(Map.of(
                "eid", eventId,
                "sid", sessionId,
                "cap", capName
        )).run();

        return stepId;
    }

    public void setStepStatus(String stepId, String status, String errorMsgOrNull) {
        String now = Instant.now().toString();

        neo4j.query("""
            MATCH (st:Step {id:$id})-[:HAS_EVENT]->(e:MessageEvent)
            SET e.status = $status
            SET e.updatedAt = datetime($now)
            FOREACH (_ IN CASE WHEN $status = "complete" THEN [1] ELSE [] END |
                SET e.completeAt = datetime($now)
            )
            FOREACH (_ IN CASE WHEN $status = "failed" THEN [1] ELSE [] END |
                SET e.failedAt = datetime($now),
                    e.error = $err
            )
        """).bindAll(Map.of(
                "id", stepId,
                "status", status,
                "err", errorMsgOrNull == null ? "" : errorMsgOrNull,
                "now", now
        )).run();
    }

    private String safeUiName(MessageEventRequest event) {
        try {
            if (event == null || event.sender() == null) return "unknown";
            String c = event.sender().component();
            return (c == null || c.isBlank()) ? "unknown" : c.replace(".vue", "");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String inferBackendName(Capability cap, Map<String, Object> handlerData) {
        if (handlerData != null) {
            Object b = handlerData.get("backend");
            if (b == null) b = handlerData.get("service");
            if (b != null && !String.valueOf(b).isBlank()) {
                return String.valueOf(b);
            }
        }

        if (cap == null) return "unknown";

        return switch (cap) {
            case Authentication, Authorization -> "AuthService";
            case ProductList, OrderPlaced -> "ProductListService";
            default -> "unknown";
        };
    }
}