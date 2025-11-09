package shop.serve.ShopNServe.model;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.annotation.Version;
import java.util.*;

@Node
public class Product {

    @Id
    private String id = UUID.randomUUID().toString();
    private String name;
    private int priceCents;

    @Version
    private Long version;

    @Relationship(type = "RELATED_TO")
    private List<Product> relatedProducts = new ArrayList<>();

    public Product() {}

    public Product(String name, int priceCents) {
        this.name = name;
        this.priceCents = priceCents;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getPriceCents() { return priceCents; }
    public Long getVersion() { return version; }
    public List<Product> getRelatedProducts() { return relatedProducts; }

    public void addRelatedProduct(Product p) {
        this.relatedProducts.add(p);
    }
}
