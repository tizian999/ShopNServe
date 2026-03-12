package shop.serve.ShopNServe.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.MessageEventRequest;
import shop.serve.ShopNServe.service.BlackboardService;

import java.util.Map;

@RestController
@RequestMapping("/api/blackboard")
public class BlackboardController {

    private final BlackboardService blackboardService;

    public BlackboardController(BlackboardService blackboardService) {
        this.blackboardService = blackboardService;
    }

    @PostMapping("/messages")
    public ResponseEntity<BlackboardResponse> postMessage(
            @RequestBody MessageEventRequest event,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            BlackboardResponse resp = blackboardService.handle(event, authHeader);
            return resp.ok() ? ResponseEntity.ok(resp) : ResponseEntity.status(401).body(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new BlackboardResponse(false, Map.of(
                    "error", "Internal Server Error: " + e.getClass().getSimpleName(),
                    "message", String.valueOf(e.getMessage()),
                    "traceId", event != null ? event.traceIdOrNull() : null
            )));
        }
    }
}