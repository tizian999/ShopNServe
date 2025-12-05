package shop.serve.ShopNServe.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.*;

@RestController
@RequestMapping("/api/graph")
@CrossOrigin("*")
public class GraphController {

    private final Neo4jClient client;

    public GraphController(Neo4jClient client) {
        this.client = client;
    }

    @GetMapping
    public Map<String, Object> graph() {
        return client.query("""
            MATCH (n)
            OPTIONAL MATCH (n)-[r]->(m)
            WITH collect(distinct {
                id: elementId(n),
                labels: labels(n),
                props: properties(n),
                type: head(labels(n))
            }) AS nodes,
            collect(distinct {
                from: elementId(n),
                to: elementId(m),
                source: elementId(n),
                target: elementId(m),
                type: type(r)
            }) AS rawLinks
            WITH nodes, [link IN rawLinks WHERE link.target IS NOT NULL] AS links
            RETURN nodes, links
        """).fetch().one().orElseThrow();
    }

    @GetMapping("/blackboard")
    public Map<String, Object> blackboard() {
        return client.query("""
            MATCH (client:MicroClient)
            OPTIONAL MATCH (client)-[req:REQUIRES]->(capReq:Capability)
            OPTIONAL MATCH (client)-[prov:PROVIDES]->(capProv:Capability)
            OPTIONAL MATCH (msg:Message)-[:SENT_BY]->(client)
            OPTIONAL MATCH (msg)-[:TARGETS]->(capTarget:Capability)
            WITH
              collect(distinct {
                id: elementId(client), name: client.name, type: 'MicroClient'
              }) AS microClients,
              collect(distinct {
                id: elementId(capReq), name: capReq.name, capabilityType: capReq.type, type: 'Capability'
              }) AS reqCaps,
              collect(distinct {
                id: elementId(capProv), name: capProv.name, capabilityType: capProv.type, type: 'Capability'
              }) AS provCaps,
              collect(distinct {
                id: elementId(msg), name: msg.eventType, payload: msg.payload, capability: msg.capability, type: 'Message'
              }) AS messages,
              collect(distinct {
                from: elementId(client), to: elementId(capReq), source: elementId(client), target: elementId(capReq), type: 'REQUIRES'
              }) AS reqLinks,
              collect(distinct {
                from: elementId(client), to: elementId(capProv), source: elementId(client), target: elementId(capProv), type: 'PROVIDES'
              }) AS provLinks,
              collect(distinct {
                // Richtung korrigiert: Beziehung ist (msg)-[:SENT_BY]->(client)
                from: elementId(msg), to: elementId(client), source: elementId(msg), target: elementId(client), type: 'SENT_BY'
              }) AS sentLinks,
              collect(distinct {
                from: elementId(msg), to: elementId(capTarget), source: elementId(msg), target: elementId(capTarget), type: 'TARGETS'
              }) AS targetLinks
            WITH microClients + reqCaps + provCaps + messages AS nodes,
                 [l IN reqLinks    WHERE l.target IS NOT NULL] +
                 [l IN provLinks   WHERE l.target IS NOT NULL] +
                 [l IN sentLinks   WHERE l.target IS NOT NULL] +
                 [l IN targetLinks WHERE l.target IS NOT NULL] AS links
            RETURN nodes, links
        """).fetch().one().orElseThrow();
    }
}
