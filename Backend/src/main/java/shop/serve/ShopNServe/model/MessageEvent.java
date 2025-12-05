package shop.serve.ShopNServe.model;

import org.springframework.data.neo4j.core.schema.*;

@Node("Message")
public class MessageEvent {

    @Id
    @GeneratedValue
    private Long id;

    private String sender;
    private String eventType;
    private String capability;
    private String payload;
    private String timestamp;

    @Relationship(type = "SENT_BY", direction = Relationship.Direction.OUTGOING)
    private MicroClient sentBy;

    @Relationship(type = "TARGETS", direction = Relationship.Direction.OUTGOING)
    private Capability targets;

    public MessageEvent() {}

    // -------- GETTER --------
    public Long getId() { return id; }
    public String getSender() { return sender; }
    public String getEventType() { return eventType; }
    public String getCapability() { return capability; }
    public String getPayload() { return payload; }
    public String getTimestamp() { return timestamp; }
    public MicroClient getSentBy() { return sentBy; }
    public Capability getTargets() { return targets; }

    // -------- SETTER --------
    public void setSender(String sender) { this.sender = sender; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setCapability(String capability) { this.capability = capability; }
    public void setPayload(String payload) { this.payload = payload; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public void setSentBy(MicroClient sentBy) { this.sentBy = sentBy; }
    public void setTargets(Capability targets) { this.targets = targets; }
}
