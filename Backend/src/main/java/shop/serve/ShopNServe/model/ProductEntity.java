package shop.serve.ShopNServe.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_cents", nullable = false)
    private Integer priceCents;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer stock;

    public ProductEntity() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public Integer getPriceCents() { return priceCents; }
    public String getDescription() { return description; }
    public Integer getStock() { return stock; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPriceCents(Integer priceCents) { this.priceCents = priceCents; }
    public void setDescription(String description) { this.description = description; }
    public void setStock(Integer stock) { this.stock = stock; }
}