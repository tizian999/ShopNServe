package shop.serve.ShopNServe.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JwtService {

    public String generate(String username) {
        // Demo-Token (später echte JWT Lib)
        return "demo-" + username + "-" + UUID.randomUUID();
    }

    public boolean validate(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) return false;
        if (!authHeader.startsWith("Bearer ")) return false;
        String token = authHeader.substring("Bearer ".length());
        return token.startsWith("demo-");
    }
}
