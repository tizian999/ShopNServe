package shop.serve.ShopNServe.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import shop.serve.ShopNServe.model.Capability;

public interface CapabilityRepository extends Neo4jRepository<Capability, Long> {
    Capability findByName(String name);
}
