package shop.serve.ShopNServe.model;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Order")
public class Order {
    @Id
    @GeneratedValue
    private Long id;
    private String productName;
    private int quantity;
    private String status; // CREATED, CONFIRMED

    @Relationship(type = "PLACED_BY")
    private UserAccount user;

    public Order() {}
    public Order(String productName, int quantity, String status, UserAccount user) {
        this.productName = productName;
        this.quantity = quantity;
        this.status = status;
        this.user = user;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
}

