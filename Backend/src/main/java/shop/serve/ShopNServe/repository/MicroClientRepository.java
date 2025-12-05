package shop.serve.ShopNServe.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import shop.serve.ShopNServe.model.MicroClient;

public interface MicroClientRepository extends Neo4jRepository<MicroClient, Long> {
    MicroClient findByName(String name);
}
