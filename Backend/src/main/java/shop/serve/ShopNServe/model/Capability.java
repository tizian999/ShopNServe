package shop.serve.ShopNServe.model;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node("Capability")
public class Capability {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @Relationship(type = "REQUIRES")
    private List<Capability> requires;

    @Relationship(type = "PROVIDES")
    private List<Capability> provides;

    public Capability() {
    }

    public Capability(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Capability> getRequires() {
        return requires;
    }

    public void setRequires(List<Capability> requires) {
        this.requires = requires;
    }

    public List<Capability> getProvides() {
        return provides;
    }

    public void setProvides(List<Capability> provides) {
        this.provides = provides;
    }
}
