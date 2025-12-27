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
    public Map<String, Object> getGraph() {
        String q = """
                    MATCH (a)-[r]->(b)
                    WHERE exists(a.name) AND exists(b.name)
                      AND (a:UIComponent OR a:BackendComponent OR a:Capability OR a:MicroClient)
                      AND (b:UIComponent OR b:BackendComponent OR b:Capability OR b:MicroClient)
                    RETURN labels(a)[0] AS aLabel, a.name AS aName,
                           type(r) AS rel,
                           labels(b)[0] AS bLabel, b.name AS bName
                """;

        List<Map<String, Object>> edges = new ArrayList<>();
        Map<String, Map<String, Object>> nodes = new LinkedHashMap<>();

        neo4j.query(q).fetch().all().forEach(row -> {
            String aLabel = Objects.toString(row.get("aLabel"), "");
            String aName = Objects.toString(row.get("aName"), "");
            String bLabel = Objects.toString(row.get("bLabel"), "");
            String bName = Objects.toString(row.get("bName"), "");
            String rel = Objects.toString(row.get("rel"), "");

            if (aName.isBlank() || bName.isBlank() || aLabel.isBlank() || bLabel.isBlank() || rel.isBlank()) return;

            String aId = aLabel + ":" + aName;
            String bId = bLabel + ":" + bName;

            nodes.putIfAbsent(aId, Map.of("id", aId, "label", aName, "type", aLabel));
            nodes.putIfAbsent(bId, Map.of("id", bId, "label", bName, "type", bLabel));

            edges.add(Map.of("from", aId, "to", bId, "type", rel));
        });

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", new ArrayList<>(nodes.values()));
        result.put("edges", edges);
        return result;
    }
    public List<Map<String, Object>> getMessages(int limit) {
        String q = """
            MATCH (u:UIComponent)-[:SENDS]->(e:MessageEvent)
            OPTIONAL MATCH (e)-[:HANDLED_BY]->(b:BackendComponent)
            RETURN
              e.id AS id,
              u.name AS sender,
              b.name AS receiver,
              e.eventType AS eventType,
              e.capability AS capability,
              e.payload AS payload,
              toString(e.timestamp) AS timestamp
            ORDER BY e.timestamp DESC
            LIMIT $limit
        """;

        return new ArrayList<>(
                neo4j.query(q)
                        .bind(limit).to("limit")
                        .fetch()
                        .all()
        );
    }
}
