package shop.serve.ShopNServe.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import shop.serve.ShopNServe.model.MessageEvent;

public interface MessageEventRepository extends Neo4jRepository<MessageEvent, Long> {
}
