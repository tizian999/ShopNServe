package shop.serve.ShopNServe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "shop.serve.ShopNServe")
public class ShopNServeApplication {
	public static void main(String[] args) {
		SpringApplication.run(ShopNServeApplication.class, args);
	}
}

