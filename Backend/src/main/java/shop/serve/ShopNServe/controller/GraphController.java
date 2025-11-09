package shop.serve.ShopNServe.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.data.neo4j.core.Neo4jClient;
import java.util.*;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final Neo4jClient client;

    public GraphController(Neo4jClient client) {
        this.client = client;
    }

    @GetMapping
    public Map<String, Object> getGraph() {
        String query = """
            MATCH (n)-[r]->(m)
            RETURN collect({id: elementId(n), label: labels(n)[0], name: n.name}) AS nodes,
                   collect({source: elementId(startNode(r)), target: elementId(endNode(r)), type: type(r)}) AS links
        """;

        return client.query(query)
                .fetch()
                .one()  // fetch() statt fetchAs() – kein Mapping nötig
                .orElse(Map.of("nodes", List.of(), "links", List.of()));
    }
}
