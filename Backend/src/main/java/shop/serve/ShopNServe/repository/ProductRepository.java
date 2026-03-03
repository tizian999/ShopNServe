package shop.serve.ShopNServe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.serve.ShopNServe.model.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
}