package id.ac.ui.cs.advprog.backend;

import id.ac.ui.cs.advprog.backend.model.Item;
import id.ac.ui.cs.advprog.backend.repository.ItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(ItemRepository repository) {
        return args -> {
            repository.save(new Item(null, "Dummy 1", 15000000.0));
            repository.save(new Item(null, "Dummy 2", 500000.0));
        };
    }
}
