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
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public GraphService(Neo4jClient neo4j) {
        this.neo4j = neo4j;
    }

    public void storeRequires(String uiComponent, String capability) {
        neo4j.query("""
            MERGE (u:UIComponent {name: $ui})
            MERGE (c:Capability {name: $cap})
            MERGE (u)-[:REQUIRES]->(c)
        """).bindAll(Map.of("ui", uiComponent, "cap", capability)).run();
    }

    public void storeProvides(String backendComponent, String capability) {
        neo4j.query("""
            MERGE (b:BackendComponent {name: $backend})
            MERGE (c:Capability {name: $cap})
            MERGE (b)-[:PROVIDES]->(c)
        """).bindAll(Map.of("backend", backendComponent, "cap", capability)).run();
    }

    public void storeCommunicatesWith(String uiComponent, String backendComponent) {
        neo4j.query("""
            MERGE (u:UIComponent {name: $ui})
            MERGE (b:BackendComponent {name: $backend})
            MERGE (u)-[:COMMUNICATES_WITH]->(b)
        """).bindAll(Map.of("ui", uiComponent, "backend", backendComponent)).run();
    }

    public void storeUiBelongsToApp(String uiComponent, String appName) {
        neo4j.query("""
            MERGE (u:UIComponent {name: $ui})
            MERGE (m:MicroClient {name: $app})
            MERGE (u)-[:PART_OF]->(m)
        """).bindAll(Map.of("ui", uiComponent, "app", appName)).run();
    }

    public String storeMessageEvent(String senderUiComponent,
                                    String eventType,
                                    String capability,
                                    Object payload) {
        return storeMessageEvent(senderUiComponent, eventType, capability, payload, null);
    }

    public String storeMessageEvent(String senderUiComponent,
                                    String receiverBackendComponent,
                                    String eventType,
                                    String capability,
                                    Object payload) {
        return storeMessageEvent(senderUiComponent, receiverBackendComponent, eventType, capability, payload, null);
    }

    public String storeMessageEvent(String senderUiComponent,
                                    String eventType,
                                    String capability,
                                    Object payload,
                                    String traceIdOrNull) {

        String id = UUID.randomUUID().toString();
        String usedTraceId = (traceIdOrNull == null || traceIdOrNull.isBlank())
                ? UUID.randomUUID().toString()
                : traceIdOrNull;

        String payloadStr = null;
        try {
            if (payload != null) payloadStr = MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            payloadStr = String.valueOf(payload);
        }

        String now = Instant.now().toString();

        neo4j.query("""
            MERGE (t:Trace {id: $traceId})
              ON CREATE SET t.startedAt = datetime($now)
            MERGE (u:UIComponent {name: $sender})
            MERGE (c:Capability {name: $capability})
            CREATE (e:MessageEvent {
              id: $id,
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
                "sender", senderUiComponent,
                "id", id,
                "traceId", usedTraceId,
                "eventType", eventType,
                "capability", capability,
                "payload", payloadStr,
                "now", now
        )).run();

        return usedTraceId;
    }

    public String storeMessageEvent(String senderUiComponent,
                                    String receiverBackendComponent,
                                    String eventType,
                                    String capability,
                                    Object payload,
                                    String traceIdOrNull) {

        String usedTraceId = storeMessageEvent(senderUiComponent, eventType, capability, payload, traceIdOrNull);

        if (receiverBackendComponent != null && !receiverBackendComponent.isBlank()) {
            linkHandledByLatest(senderUiComponent, usedTraceId, receiverBackendComponent, capability);
            storeCommunicatesWith(senderUiComponent, receiverBackendComponent);
        }
        return usedTraceId;
    }

    private void linkHandledByLatest(String sender, String traceId, String backend, String capability) {
        neo4j.query("""
            MATCH (t:Trace {id: $traceId})-[:HAS_EVENT]->(e:MessageEvent {capability: $capability})
            MATCH (u:UIComponent {name: $sender})-[:SENDS]->(e)
            WITH e ORDER BY e.timestamp DESC LIMIT 1
            MERGE (b:BackendComponent {name: $backend})
            MERGE (e)-[:HANDLED_BY]->(b)
        """).bindAll(Map.of(
                "traceId", traceId,
                "sender", sender,
                "backend", backend,
                "capability", capability
        )).run();
    }
}