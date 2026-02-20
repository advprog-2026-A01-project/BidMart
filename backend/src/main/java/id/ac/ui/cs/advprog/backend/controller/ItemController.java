package id.ac.ui.cs.advprog.backend.controller;

import id.ac.ui.cs.advprog.backend.model.Item;
import id.ac.ui.cs.advprog.backend.repository.ItemRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = "http://localhost:5173")
public class ItemController {

    private final ItemRepository repository;

    public ItemController(ItemRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Item> getAllItems() {
        return repository.findAll();
    }
}