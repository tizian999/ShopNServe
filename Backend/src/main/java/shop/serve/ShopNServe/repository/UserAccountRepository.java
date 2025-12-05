package shop.serve.ShopNServe.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import shop.serve.ShopNServe.model.UserAccount;

public interface UserAccountRepository extends Neo4jRepository<UserAccount, Long> {
    UserAccount findByUsername(String username);
}

