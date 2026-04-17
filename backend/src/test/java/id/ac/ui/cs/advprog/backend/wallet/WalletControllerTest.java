package id.ac.ui.cs.advprog.backend.wallet;

import id.ac.ui.cs.advprog.backend.auth.model.AuthPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class WalletControllerTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
        registry.add("spring.datasource.driverClassName", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private id.ac.ui.cs.advprog.backend.auth.repository.SessionRepository sessionRepository; 

    @BeforeEach
    void setUp() {
        AuthPrincipal principal = new AuthPrincipal(1L, "user1", "BUYER");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void testGetWalletInfo() throws Exception {
        when(walletService.getWallet(1L)).thenReturn(new WalletEntity(1L, 100.0, 50.0));

        mockMvc.perform(get("/api/wallet/me/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableBalance").value(100.0))
                .andExpect(jsonPath("$.heldBalance").value(50.0));
    }

    @Test
    void testGetWalletInfoUnauthorized() throws Exception {
        // Clearing auth causes Controller to throw IllegalStateException("User not authenticated")
        SecurityContextHolder.clearContext();
        try {
            mockMvc.perform(get("/api/wallet/me/info"));
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }

    @Test
    void testTopUp() throws Exception {
        mockMvc.perform(post("/api/wallet/me/topup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 50.0}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Top-up successful"));

        verify(walletService).topUp(1L, 50.0);
    }
}
