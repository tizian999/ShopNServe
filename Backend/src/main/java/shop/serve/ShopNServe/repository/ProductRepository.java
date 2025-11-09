package shop.serve.ShopNServe.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;
import shop.serve.ShopNServe.model.Product;

@Repository
public interface ProductRepository extends Neo4jRepository<Product, String> {
}
