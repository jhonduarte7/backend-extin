package com.extin.backend.security;

import com.extin.backend.models.entities.User;
import com.extin.backend.models.enums.Role;
import com.extin.backend.repositories.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying route-level authorization, role-based access control,
 * and 401/403 HTTP status code handling across public, user, and admin endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessControlTest {

    @MockitoBean
    private VectorStore vectorStore;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername("testadmin")) {
            userRepository.save(new User("testadmin", passwordEncoder.encode("adminpass"), Role.ADMIN));
        }
        if (!userRepository.existsByUsername("testuser")) {
            userRepository.save(new User("testuser", passwordEncoder.encode("userpass"), Role.USER));
        }

        User adminUser = userRepository.findByUsername("testadmin").orElseThrow();
        User standardUser = userRepository.findByUsername("testuser").orElseThrow();

        adminToken = jwtService.generateToken(adminUser);
        userToken = jwtService.generateToken(standardUser);
    }

    @Test
    @DisplayName("Public routes: /api/public/catalog accessible without token")
    void testPublicCatalogAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/public/catalog"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin routes: /api/admin/invoices returns 401 Unauthorized without jwt_token cookie")
    void testAdminAccessUnauthorizedWithoutCookie() throws Exception {
        mockMvc.perform(get("/api/admin/invoices"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Admin routes: /api/admin/invoices returns 403 Forbidden with USER role cookie")
    void testAdminAccessForbiddenWithUserRoleCookie() throws Exception {
        mockMvc.perform(get("/api/admin/invoices")
                        .cookie(new Cookie(JwtAuthenticationFilter.JWT_COOKIE_NAME, userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("Admin routes: /api/admin/invoices returns 200 OK with ADMIN role cookie")
    void testAdminAccessAllowedWithAdminRoleCookie() throws Exception {
        mockMvc.perform(get("/api/admin/invoices")
                        .cookie(new Cookie(JwtAuthenticationFilter.JWT_COOKIE_NAME, adminToken)))
                .andExpect(status().isOk());
    }
}
