package shop.serve.ShopNServe.model;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node
public class Product {
    @Id @GeneratedValue private Long id;
    private String name;
    private int priceCents;

    public Product() {}
    public Product(String name, int priceCents) {
        this.name = name;
        this.priceCents = priceCents;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getPriceCents() { return priceCents; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPriceCents(int priceCents) { this.priceCents = priceCents; }
}
