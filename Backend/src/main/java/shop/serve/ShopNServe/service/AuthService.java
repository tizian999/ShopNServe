package shop.serve.ShopNServe.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final ConcurrentHashMap<String, String> users = new ConcurrentHashMap<>();
    private final JwtService jwtService;

    public AuthService(JwtService jwtService) {
        this.jwtService = jwtService;
        users.put("demo", "demo");
    }

    public AuthResult login(String username, String password) {
        String pw = users.get(username);
        if (pw == null || !pw.equals(password)) {
            return new AuthResult(false, null, null, "Invalid credentials");
        }
        String token = jwtService.generate(username);
        return new AuthResult(true, username, token, "ok");
    }

    public AuthResult register(String username, String password) {
        if (users.containsKey(username)) {
            return new AuthResult(false, null, null, "User already exists");
        }
        users.put(username, password);
        String token = jwtService.generate(username);
        return new AuthResult(true, username, token, "registered");
    }

    public boolean validate(String authHeaderOrToken) {
        return jwtService.validate(authHeaderOrToken);
    }

    public record AuthResult(boolean success, String username, String token, String message) {}
}