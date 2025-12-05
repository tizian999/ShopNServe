package shop.serve.ShopNServe.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.List;

@Node("MicroClient")
public class MicroClient {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    // Provided + Required Capabilities
    private List<String> provides;
    private List<String> requires;

    public MicroClient() {}

    public MicroClient(String name) {
        this.name = name;
    }

    // Getter & Setter

    public Long getId() {
        return id;
    }

    public void setId(Long id) { this.id = id; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getProvides() {
        return provides;
    }

    public void setProvides(List<String> provides) {
        this.provides = provides;
    }

    public List<String> getRequires() {
        return requires;
    }

    public void setRequires(List<String> requires) {
        this.requires = requires;
    }
}
