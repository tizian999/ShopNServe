package shop.serve.ShopNServe.controller;

import org.springframework.web.bind.annotation.*;
import shop.serve.ShopNServe.model.MessageEvent;
import shop.serve.ShopNServe.repository.MessageEventRepository;
import shop.serve.ShopNServe.repository.MicroClientRepository;
import shop.serve.ShopNServe.repository.DataNodeRepository;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/blackboard")
@CrossOrigin("*")
public class BlackboardController {

    private final MessageEventRepository messageRepo;
    private final MicroClientRepository clientRepo;
    private final DataNodeRepository dataRepo;

    public BlackboardController(
            MessageEventRepository messageRepo,
            MicroClientRepository clientRepo,
            DataNodeRepository dataRepo
    ) {
        this.messageRepo = messageRepo;
        this.clientRepo = clientRepo;
        this.dataRepo = dataRepo;
    }

    @PostMapping("/event")
    public MessageEvent addEvent(@RequestBody MessageEvent event) {
        return messageRepo.save(event);
    }

    @GetMapping("/events")
    public List<MessageEvent> getEvents() {
        return messageRepo.findAll();
    }
}
