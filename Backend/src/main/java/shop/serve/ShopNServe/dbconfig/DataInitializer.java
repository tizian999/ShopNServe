package shop.serve.ShopNServe.dbconfig;

import org.springframework.data.neo4j.core.Neo4jClient;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

@Component
@Profile("docker")
public class DataInitializer {

    private final Neo4jClient client;

    public DataInitializer(Neo4jClient client) {
        this.client = client;
    }

    @PostConstruct
    public void init() {
        client.query("""
            MERGE (c1:MicroClient {name:'VueSelector'})
            MERGE (cap1:Capability {name:'ProductList', type:'REQUIRED'})
            MERGE (cap2:Capability {name:'ProductSelection', type:'PROVIDED'})
            MERGE (c1)-[:REQUIRES]->(cap1)
            MERGE (c1)-[:PROVIDES]->(cap2)
            MERGE (msg1:Message {eventType:'PROVIDES', capability:'ProductSelection', payload:'{"sample":"payload"}', timestamp: toString(datetime())})
            MERGE (msg1)-[:SENT_BY]->(c1)
            MERGE (msg1)-[:TARGETS]->(cap1)
            MERGE (:UserAccount {username:'demo', password:'demo'})
        """).run();
    }
}
