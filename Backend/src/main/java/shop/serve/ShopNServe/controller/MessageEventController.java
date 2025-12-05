package shop.serve.ShopNServe.controller;

import org.springframework.web.bind.annotation.*;
import shop.serve.ShopNServe.model.MessageEvent;
import shop.serve.ShopNServe.model.MicroClient;
import shop.serve.ShopNServe.model.Capability;
import shop.serve.ShopNServe.repository.MessageEventRepository;
import shop.serve.ShopNServe.repository.MicroClientRepository;
import shop.serve.ShopNServe.repository.CapabilityRepository;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin("*")
public class MessageEventController {

    private final MessageEventRepository repo;
    private final MicroClientRepository microRepo;
    private final CapabilityRepository capabilityRepo;
    private final Neo4jClient client;

    public MessageEventController(
            MessageEventRepository repo,
            MicroClientRepository microRepo,
            CapabilityRepository capabilityRepo,
            Neo4jClient client
    ) {
        this.repo = repo;
        this.microRepo = microRepo;
        this.capabilityRepo = capabilityRepo;
        this.client = client;
    }

    @GetMapping
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> all() {
        // Query liefert pro Record ein Map mit Key "msg". Wir unwrapen auf reines Array von Message-Objekten.
        Collection<Map<String, Object>> raw = client.query("""
            MATCH (m:Message)
            OPTIONAL MATCH (m)-[:SENT_BY]->(c:MicroClient)
            RETURN {
              elementId: elementId(m),
              eventType: m.eventType,
              capability: m.capability,
              payload: m.payload,
              timestamp: m.timestamp,
              sender: c.name
            } AS msg
            ORDER BY msg.timestamp DESC
        """).fetch().all();
        return raw.stream()
                .map(rec -> {
                    Object inner = rec.get("msg");
                    return inner instanceof Map ? (Map<String,Object>) inner : null;
                })
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    @PostMapping
    public Map<String,Object> create(@RequestBody MessageEvent event) {
        // Fehlende Entities automatisch anlegen
        MicroClient sender = microRepo.findByName(event.getSender());
        if (sender == null && event.getSender() != null) {
            sender = new MicroClient(event.getSender());
            microRepo.save(sender);
        }
        Capability cap = capabilityRepo.findByName(event.getCapability());
        if (cap == null && event.getCapability() != null) {
            cap = new Capability(event.getCapability());
            capabilityRepo.save(cap);
        }

        MessageEvent saved = new MessageEvent();
        saved.setSender(event.getSender());
        saved.setEventType(event.getEventType());
        saved.setCapability(event.getCapability());
        saved.setPayload(event.getPayload());
        saved.setTimestamp(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        saved.setSentBy(sender);
        saved.setTargets(cap);
        repo.save(saved);

        // ElementId abfragen
        String elementId = client.query("""
            MATCH (m:Message) WHERE id(m) = $internalId RETURN elementId(m) AS eid
        """)
                .bind(saved.getId()).to("internalId")
                .fetch().one()
                .map(m -> (String) m.get("eid"))
                .orElse(null);

        return Map.of(
                "elementId", elementId,
                "eventType", saved.getEventType(),
                "capability", saved.getCapability(),
                "payload", saved.getPayload(),
                "timestamp", saved.getTimestamp(),
                "sender", saved.getSender()
        );
    }
}
