package shop.serve.ShopNServe.repository;

import shop.serve.ShopNServe.model.Product;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ProductRepository extends Neo4jRepository<Product, Long> {
}
