package shop.serve.ShopNServe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.serve.ShopNServe.model.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {}