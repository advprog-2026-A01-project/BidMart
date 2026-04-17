package id.ac.ui.cs.advprog.backend.wallet;

import id.ac.ui.cs.advprog.backend.auth.model.AuthPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

    @MockBean
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
        // Clearing auth triggers the 403 Forbidden via Spring Security
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/wallet/me/info"))
                .andExpect(status().isForbidden());
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
