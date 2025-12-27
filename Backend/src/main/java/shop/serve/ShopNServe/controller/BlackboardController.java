package shop.serve.ShopNServe.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shop.serve.ShopNServe.model.BlackboardResponse;
import shop.serve.ShopNServe.model.MessageEventRequest;
import shop.serve.ShopNServe.service.BlackboardQueryService;
import shop.serve.ShopNServe.service.BlackboardService;

import java.util.Map;

@RestController
@RequestMapping("/api/blackboard")
public class BlackboardController {

    private final BlackboardService blackboardService;
    private final BlackboardQueryService queryService;

    public BlackboardController(BlackboardService blackboardService, BlackboardQueryService queryService) {
        this.blackboardService = blackboardService;
        this.queryService = queryService;
    }

    @PostMapping("/messages")
    public ResponseEntity<BlackboardResponse> postMessage(
            @RequestBody MessageEventRequest event,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        BlackboardResponse resp = blackboardService.handle(event, authHeader);
        return resp.ok() ? ResponseEntity.ok(resp) : ResponseEntity.status(401).body(resp);
    }

    @GetMapping("/graph")
    public ResponseEntity<Map<String, Object>> getGraph() {
        return ResponseEntity.ok(queryService.getGraph());
    }

    @GetMapping("/messages")
    public ResponseEntity<?> getMessages(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(queryService.getMessages(limit));
    }
}
