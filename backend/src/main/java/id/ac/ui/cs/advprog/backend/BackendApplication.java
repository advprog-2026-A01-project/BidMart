package id.ac.ui.cs.advprog.backend;

import id.ac.ui.cs.advprog.backend.auth.AuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AuthProperties.class)
public class BackendApplication {

    public static void main(final String[] args) {

        SpringApplication.run(BackendApplication.class, args);

    }

}


