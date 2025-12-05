package shop.serve.ShopNServe.controller;

import org.springframework.web.bind.annotation.*;
import shop.serve.ShopNServe.model.UserAccount;
import shop.serve.ShopNServe.repository.UserAccountRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {

    private final UserAccountRepository userRepo;

    public AuthController(UserAccountRepository userRepo) {
        this.userRepo = userRepo;
    }

    @PostMapping("/login")
    public Map<String,Object> login(@RequestBody Map<String,String> body) {
        String username = body.get("username");
        String password = body.get("password");
        UserAccount user = userRepo.findByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            return Map.of("success", false, "message", "Invalid credentials");
        }
        return Map.of("success", true, "username", user.getUsername());
    }

    @PostMapping("/register")
    public Map<String,Object> register(@RequestBody Map<String,String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (userRepo.findByUsername(username) != null) {
            return Map.of("success", false, "message", "Username already exists");
        }
        UserAccount u = new UserAccount(username, password);
        userRepo.save(u);
        return Map.of("success", true, "username", u.getUsername());
    }
}

