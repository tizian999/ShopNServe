package shop.serve.ShopNServe.service;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class GraphService {

    private final Neo4jClient neo4j;

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

        String id = UUID.randomUUID().toString();
        String payloadStr = payload == null ? null : String.valueOf(payload);

        neo4j.query("""
            MERGE (u:UIComponent {name: $sender})
            CREATE (e:MessageEvent {
              id: $id,
              eventType: $eventType,
              capability: $capability,
              payload: $payload,
              timestamp: datetime($ts)
            })
            MERGE (u)-[:SENDS]->(e)
        """).bindAll(Map.of(
                "sender", senderUiComponent,
                "id", id,
                "eventType", eventType,
                "capability", capability,
                "payload", payloadStr,
                "ts", Instant.now().toString()
        )).run();

        return id;
    }
    public String storeMessageEvent(String senderUiComponent,
                                    String receiverBackendComponent,
                                    String eventType,
                                    String capability,
                                    Object payload) {

        String id = storeMessageEvent(senderUiComponent, eventType, capability, payload);

        if (receiverBackendComponent != null && !receiverBackendComponent.isBlank()) {
            linkHandledBy(id, receiverBackendComponent);
            storeCommunicatesWith(senderUiComponent, receiverBackendComponent);
        }
        return id;
    }

    public void linkHandledBy(String eventId, String backendComponent) {
        neo4j.query("""
            MATCH (e:MessageEvent {id: $id})
            MERGE (b:BackendComponent {name: $backend})
            MERGE (e)-[:HANDLED_BY]->(b)
        """).bindAll(Map.of("id", eventId, "backend", backendComponent)).run();
    }
}
