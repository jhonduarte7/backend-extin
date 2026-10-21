package com.extin.backend.controllers;

import com.extin.backend.models.dtos.LoginRequestDto;
import com.extin.backend.models.dtos.LoginResponseDto;
import com.extin.backend.security.JwtAuthenticationFilter;
import com.extin.backend.security.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller handling user authentication, login and logout.
 * Secures JWT issuance inside HttpOnly cookies to protect against XSS token theft.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${security.jwt.cookie.secure:false}")
    private boolean secureCookie;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Authenticates user credentials and attaches an HttpOnly, Path=/, Secure (in prod) cookie
     * containing the generated JWT token. The token is never returned in the JSON response body.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        log.info("Attempting authentication for user '{}'", request.username());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        boolean isSecure = secureCookie || httpRequest.isSecure();

        Cookie jwtCookie = new Cookie(JwtAuthenticationFilter.JWT_COOKIE_NAME, token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(isSecure);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge((int) (jwtService.getExpirationMs() / 1000));

        httpResponse.addCookie(jwtCookie);

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("USER");

        log.info("User '{}' authenticated successfully with role '{}'", userDetails.getUsername(), role);

        return ResponseEntity.ok(new LoginResponseDto("Login successful", userDetails.getUsername(), role));
    }

    /**
     * Clears the authentication HttpOnly cookie to invalidate the client session.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        boolean isSecure = secureCookie || httpRequest.isSecure();

        Cookie jwtCookie = new Cookie(JwtAuthenticationFilter.JWT_COOKIE_NAME, "");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(isSecure);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);

        httpResponse.addCookie(jwtCookie);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /**
     * Returns current authenticated user information from the SecurityContext.
     */
    @GetMapping("/me")
    public ResponseEntity<LoginResponseDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("USER");

        return ResponseEntity.ok(new LoginResponseDto("Authenticated", userDetails.getUsername(), role));
    }
}
