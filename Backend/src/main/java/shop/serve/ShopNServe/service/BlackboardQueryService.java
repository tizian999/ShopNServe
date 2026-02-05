package shop.serve.ShopNServe.service;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BlackboardQueryService {

    private final Neo4jClient neo4j;

    public BlackboardQueryService(Neo4jClient neo4j) {
        this.neo4j = neo4j;
    }

    public List<Map<String, Object>> getTraces(int limit) {
        return new ArrayList<>(neo4j.query("""
        MATCH (t:Trace)
        WITH t
        ORDER BY t.startedAt DESC
        RETURN
          t.id AS id,
          toString(t.startedAt) AS startedAt
        LIMIT $limit
    """)
                .bind(limit).to("limit")
                .fetch()
                .all());
    }

    public Map<String, Object> getGraph(String traceId) {
        String q = """
            OPTIONAL MATCH (tLatest:Trace)
            WITH tLatest
            ORDER BY tLatest.startedAt DESC
            LIMIT 1
            WITH coalesce($traceId, tLatest.id) AS tid
            MATCH (t:Trace {id: tid})
            OPTIONAL MATCH (t)-[:HAS_EVENT]->(e:MessageEvent)
            OPTIONAL MATCH (u:UIComponent)-[:SENDS]->(e)
            OPTIONAL MATCH (e)-[:HANDLED_BY]->(b:BackendComponent)
            OPTIONAL MATCH (e)-[:ABOUT]->(c:Capability)
            RETURN
              t.id AS traceId,
              e.id AS eventId,
              e.eventType AS eventType,
              toString(e.timestamp) AS ts,
              u.name AS uiName,
              b.name AS backendName,
              c.name AS capName
        """;

        List<Map<String, Object>> edges = new ArrayList<>();
        Map<String, Map<String, Object>> nodes = new LinkedHashMap<>();
        Set<String> edgeDedup = new HashSet<>();

        neo4j.query(q)
                .bind(traceId).to("traceId")
                .fetch()
                .all()
                .forEach(row -> {
                    String tid = Objects.toString(row.get("traceId"), "");
                    if (tid.isBlank()) return;

                    String tNodeId = "Trace:" + tid;
                    nodes.putIfAbsent(tNodeId, Map.of("id", tNodeId, "label", tid, "type", "Trace"));

                    String eventId   = Objects.toString(row.get("eventId"), "");
                    String eventType = Objects.toString(row.get("eventType"), "");
                    String ts        = Objects.toString(row.get("ts"), "");

                    String uiName     = Objects.toString(row.get("uiName"), "");
                    String backendName = Objects.toString(row.get("backendName"), "");
                    String capName    = Objects.toString(row.get("capName"), "");

                    String eNodeId = null;
                    if (!eventId.isBlank()) {
                        eNodeId = "MessageEvent:" + eventId;
                        String eLabel = (eventType == null || eventType.isBlank() ? "EVENT" : eventType)
                                + (ts == null || ts.isBlank() ? "" : (" @ " + ts));
                        nodes.putIfAbsent(eNodeId, Map.of("id", eNodeId, "label", eLabel, "type", "MessageEvent"));
                        addEdge(edges, edgeDedup, tNodeId, eNodeId, "HAS_EVENT");
                    }

                    String uNodeId = null;
                    if (!uiName.isBlank()) {
                        uNodeId = "UIComponent:" + uiName;
                        nodes.putIfAbsent(uNodeId, Map.of("id", uNodeId, "label", uiName, "type", "UIComponent"));
                        if (eNodeId != null) addEdge(edges, edgeDedup, uNodeId, eNodeId, "SENDS");
                    }

                    String bNodeId = null;
                    if (!backendName.isBlank()) {
                        bNodeId = "BackendComponent:" + backendName;
                        nodes.putIfAbsent(bNodeId, Map.of("id", bNodeId, "label", backendName, "type", "BackendComponent"));
                        if (eNodeId != null) addEdge(edges, edgeDedup, eNodeId, bNodeId, "HANDLED_BY");
                    }

                    String cNodeId = null;
                    if (!capName.isBlank()) {
                        cNodeId = "Capability:" + capName;
                        nodes.putIfAbsent(cNodeId, Map.of("id", cNodeId, "label", capName, "type", "Capability"));
                        if (eNodeId != null) addEdge(edges, edgeDedup, eNodeId, cNodeId, "ABOUT");
                    }

                    if (uNodeId != null && cNodeId != null && "REQUIRES".equalsIgnoreCase(eventType)) {
                        addEdge(edges, edgeDedup, uNodeId, cNodeId, "REQUIRES");
                    }
                    if (bNodeId != null && cNodeId != null && "PROVIDES".equalsIgnoreCase(eventType)) {
                        addEdge(edges, edgeDedup, bNodeId, cNodeId, "PROVIDES");
                    }
                    if (uNodeId != null && bNodeId != null) {
                        addEdge(edges, edgeDedup, uNodeId, bNodeId, "COMMUNICATES_WITH");
                    }
                });

        return Map.of(
                "nodes", new ArrayList<>(nodes.values()),
                "edges", edges
        );
    }

    private static void addEdge(List<Map<String, Object>> edges, Set<String> dedup, String from, String to, String type) {
        String k = from + "->" + to + "#" + type;
        if (dedup.add(k)) {
            edges.add(Map.of("from", from, "to", to, "type", type));
        }
    }

    public List<Map<String, Object>> getMessages(int limit, String traceId) {
        String q = """
            OPTIONAL MATCH (tLatest:Trace)
            WITH tLatest
            ORDER BY tLatest.startedAt DESC
            LIMIT 1
            WITH coalesce($traceId, tLatest.id) AS tid
            MATCH (t:Trace {id: tid})-[:HAS_EVENT]->(e:MessageEvent)
            OPTIONAL MATCH (u:UIComponent)-[:SENDS]->(e)
            OPTIONAL MATCH (e)-[:HANDLED_BY]->(b:BackendComponent)
            RETURN
              e.id AS id,
              coalesce(u.name, 'unknown') AS sender,
              b.name AS receiver,
              e.eventType AS eventType,
              e.capability AS capability,
              e.payload AS payload,
              toString(e.timestamp) AS timestamp,
              e.traceId AS traceId
            ORDER BY e.timestamp DESC
            LIMIT $limit
        """;

        return new ArrayList<>(
                neo4j.query(q)
                        .bind(limit).to("limit")
                        .bind(traceId).to("traceId")
                        .fetch()
                        .all()
        );
    }
}