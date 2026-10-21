package com.extin.backend.controllers;

import com.extin.backend.exceptions.GlobalExceptionHandler;
import com.extin.backend.models.dtos.LoginRequestDto;
import com.extin.backend.models.entities.User;
import com.extin.backend.models.enums.Role;
import com.extin.backend.security.JwtAuthenticationFilter;
import com.extin.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests verifying {@link AuthController} login, logout, and HttpOnly cookie generation.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/auth/login - Success: Returns 200, sets HttpOnly cookie, does NOT return token in body")
    void testLoginSuccess() throws Exception {
        User user = new User("admin", "hashedpassword", Role.ADMIN);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(user)).thenReturn("mocked.jwt.token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        LoginRequestDto request = new LoginRequestDto("admin", "adminpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(JwtAuthenticationFilter.JWT_COOKIE_NAME))
                .andExpect(cookie().value(JwtAuthenticationFilter.JWT_COOKIE_NAME, "mocked.jwt.token"))
                .andExpect(cookie().httpOnly(JwtAuthenticationFilter.JWT_COOKIE_NAME, true))
                .andExpect(cookie().path(JwtAuthenticationFilter.JWT_COOKIE_NAME, "/"))
                .andExpect(jsonPath("$.message", is("Login successful")))
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.role", is("ADMIN")))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.jwt").doesNotExist())
                .andExpect(jsonPath("$.jwt_token").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/login - Failure: Bad credentials returns 401 Unauthorized")
    void testLoginFailure() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequestDto request = new LoginRequestDto("admin", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist(JwtAuthenticationFilter.JWT_COOKIE_NAME))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("POST /api/auth/logout - Clears HttpOnly cookie")
    void testLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(JwtAuthenticationFilter.JWT_COOKIE_NAME))
                .andExpect(cookie().maxAge(JwtAuthenticationFilter.JWT_COOKIE_NAME, 0))
                .andExpect(jsonPath("$.message", is("Logged out successfully")));
    }
}
