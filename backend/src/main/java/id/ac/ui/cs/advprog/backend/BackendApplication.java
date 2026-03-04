package id.ac.ui.cs.advprog.backend;

import id.ac.ui.cs.advprog.backend.auth.AuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/*
BackendApplication.java: Tanggung jawab: entrypoint Spring Boot + enable config AuthProperties.
Skema DB Auth (fondasi yang stabil)
Ada 2 tabel:
- app_users (user identity + role + status)
- app_sessions (token session: access + refresh + expiry + revocation)

Ini penting: token disimpan di DB (opaque token),
bukan JWT -- supaya logout/revoke bisa benar-benar mematikan token tanpa key sharing.
 */
@SpringBootApplication
@EnableConfigurationProperties(AuthProperties.class)
public class BackendApplication {

    public static void main(final String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}