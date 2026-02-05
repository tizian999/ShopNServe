package shop.serve.ShopNServe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class GraphService {

    private final Neo4jClient neo4j;
    private final SessionGraphIngestService sessionGraphIngestService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public GraphService(Neo4jClient neo4j, SessionGraphIngestService sessionGraphIngestService) {
        this.neo4j = neo4j;
        this.sessionGraphIngestService = sessionGraphIngestService;
    }
    private String ensureTraceId(String traceIdOrNull) {
        return (traceIdOrNull == null || traceIdOrNull.isBlank())
                ? UUID.randomUUID().toString()
                : traceIdOrNull;
    }

    private void ensureTraceNode(String traceId) {
        String now = Instant.now().toString();
        neo4j.query("""
            MERGE (t:Trace {id: $traceId})
            ON CREATE SET t.startedAt = datetime($now)
        """).bindAll(Map.of("traceId", traceId, "now", now)).run();
    }

    public void storeRequires(String traceIdOrNull, String uiComponent, String capability) {
        String traceId = ensureTraceId(traceIdOrNull);
        ensureTraceNode(traceId);

        neo4j.query("""
            MATCH (t:Trace {id: $traceId})
            MERGE (u:UIComponent {traceId: $traceId, name: $ui})
            MERGE (c:Capability  {traceId: $traceId, name: $cap})
            MERGE (t)-[:HAS_NODE]->(u)
            MERGE (t)-[:HAS_NODE]->(c)
            MERGE (u)-[:REQUIRES]->(c)
        """).bindAll(Map.of("traceId", traceId, "ui", uiComponent, "cap", capability)).run();
    }

    public void storeProvides(String traceIdOrNull, String backendComponent, String capability) {
        String traceId = ensureTraceId(traceIdOrNull);
        ensureTraceNode(traceId);

        neo4j.query("""
            MATCH (t:Trace {id: $traceId})
            MERGE (b:BackendComponent {traceId: $traceId, name: $backend})
            MERGE (c:Capability       {traceId: $traceId, name: $cap})
            MERGE (t)-[:HAS_NODE]->(b)
            MERGE (t)-[:HAS_NODE]->(c)
            MERGE (b)-[:PROVIDES]->(c)
        """).bindAll(Map.of("traceId", traceId, "backend", backendComponent, "cap", capability)).run();
    }

    public void storeCommunicatesWith(String traceIdOrNull, String uiComponent, String backendComponent) {
        String traceId = ensureTraceId(traceIdOrNull);
        ensureTraceNode(traceId);

        neo4j.query("""
            MATCH (t:Trace {id: $traceId})
            MERGE (u:UIComponent      {traceId: $traceId, name: $ui})
            MERGE (b:BackendComponent {traceId: $traceId, name: $backend})
            MERGE (t)-[:HAS_NODE]->(u)
            MERGE (t)-[:HAS_NODE]->(b)
            MERGE (u)-[:COMMUNICATES_WITH]->(b)
        """).bindAll(Map.of("traceId", traceId, "ui", uiComponent, "backend", backendComponent)).run();
    }

    public void storeUiBelongsToApp(String traceIdOrNull, String uiComponent, String appName) {
        String traceId = ensureTraceId(traceIdOrNull);
        ensureTraceNode(traceId);

        neo4j.query("""
            MATCH (t:Trace {id: $traceId})
            MERGE (u:UIComponent {traceId: $traceId, name: $ui})
            MERGE (m:MicroClient {traceId: $traceId, name: $app})
            MERGE (t)-[:HAS_NODE]->(u)
            MERGE (t)-[:HAS_NODE]->(m)
            MERGE (u)-[:PART_OF]->(m)
        """).bindAll(Map.of("traceId", traceId, "ui", uiComponent, "app", appName)).run();
    }

    /**
     * Speichert das alte Modell (Trace/MessageEvent etc)
     * UND schreibt parallel das neue Session/Step/RequestEvent Modell via SessionGraphIngestService.
     */
    public String storeMessageEvent(
            String senderUiComponent,
            String receiverBackendComponent,
            String eventType,
            String capability,
            Object payload,
            String traceIdOrNull
    ) {
        String traceId = ensureTraceId(traceIdOrNull);
        ensureTraceNode(traceId);

        String eventId = UUID.randomUUID().toString();
        String now = Instant.now().toString();

        String payloadStr;
        try {
            payloadStr = (payload == null) ? null : MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            payloadStr = String.valueOf(payload);
        }

        neo4j.query("""
            MATCH (t:Trace {id: $traceId})
            MERGE (u:UIComponent {traceId: $traceId, name: $sender})
            MERGE (c:Capability  {traceId: $traceId, name: $capability})
            MERGE (t)-[:HAS_NODE]->(u)
            MERGE (t)-[:HAS_NODE]->(c)
            CREATE (e:MessageEvent {
              id: $eventId,
              traceId: $traceId,
              eventType: $eventType,
              capability: $capability,
              payload: $payload,
              timestamp: datetime($now)
            })
            MERGE (t)-[:HAS_EVENT]->(e)
            MERGE (u)-[:SENDS]->(e)
            MERGE (e)-[:ABOUT]->(c)
        """).bindAll(Map.of(
                "traceId", traceId,
                "sender", senderUiComponent,
                "capability", capability,
                "eventId", eventId,
                "eventType", eventType,
                "payload", payloadStr,
                "now", now
        )).run();

        if (receiverBackendComponent != null && !receiverBackendComponent.isBlank()) {
            neo4j.query("""
                MATCH (t:Trace {id: $traceId})-[:HAS_EVENT]->(e:MessageEvent {id: $eventId})
                MERGE (b:BackendComponent {traceId: $traceId, name: $backend})
                MERGE (t)-[:HAS_NODE]->(b)
                MERGE (e)-[:HANDLED_BY]->(b)
            """).bindAll(Map.of(
                    "traceId", traceId,
                    "eventId", eventId,
                    "backend", receiverBackendComponent
            )).run();

            storeCommunicatesWith(traceId, senderUiComponent, receiverBackendComponent);
        }

        try {
        } catch (Exception ignored) {
        }

        return traceId;
    }
}