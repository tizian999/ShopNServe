package shop.serve.ShopNServe.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import shop.serve.ShopNServe.model.Order;

public interface OrderRepository extends Neo4jRepository<Order, Long> {
}

