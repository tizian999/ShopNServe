package shop.serve.ShopNServe.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import shop.serve.ShopNServe.model.Product;
import shop.serve.ShopNServe.repository.ProductRepository;

import java.util.List;

@Component
public class DataInitializer {

    private final ProductRepository repo;

    public DataInitializer(ProductRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void init() {
        if (repo.count() == 0) {


            Product cola = new Product("Coca Cola", 250);
            Product fanta = new Product("Fanta", 230);
            Product water = new Product("Mineralwasser", 180);
            Product coffee = new Product("Iced Coffee", 320);


            Product chips = new Product("Paprika Chips", 280);
            Product peanuts = new Product("Salted Peanuts", 220);
            Product choco = new Product("Chocolate Bar", 200);
            Product cookie = new Product("Oatmeal Cookie", 190);

            cola.addRelatedProduct(fanta);
            fanta.addRelatedProduct(water);
            coffee.addRelatedProduct(choco);
            peanuts.addRelatedProduct(chips);
            cookie.addRelatedProduct(choco);
            chips.addRelatedProduct(cola);

            repo.saveAll(List.of(
                    cola, fanta, water, coffee, chips, peanuts, choco, cookie
            ));

            System.out.println("Sample products + relationships inserted into Neo4j!");
        } else {
            System.out.println("Products already exist, skipping initialization.");
        }
    }
}
