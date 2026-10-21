package com.extin.backend.exceptions;

import com.extin.backend.models.dtos.ErrorResponseDto;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying standard error response mapping in {@link GlobalExceptionHandler}.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test/resource");
    }

    @Test
    @DisplayName("Should handle EntityNotFoundException and return 404 with ErrorResponseDto")
    void testHandleEntityNotFound() {
        EntityNotFoundException ex = new EntityNotFoundException("Customer with id 123 not found");

        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("Customer with id 123 not found");
        assertThat(response.getBody().path()).isEqualTo("/api/test/resource");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException and return 404 with ErrorResponseDto")
    void testHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Invoice not found");

        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("Invoice not found");
        assertThat(response.getBody().path()).isEqualTo("/api/test/resource");
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException and return 400 with ErrorResponseDto")
    void testHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid currency code");

        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleIllegalArgument(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("Invalid currency code");
        assertThat(response.getBody().path()).isEqualTo("/api/test/resource");
    }

    @Test
    @DisplayName("Should handle DataIntegrityViolationException and return 400 without leaking DB internals")
    void testHandleDataIntegrityViolation() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint 'customers_tax_id_key'"
        );

        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message())
                .doesNotContain("customers_tax_id_key")
                .contains("Data integrity violation");
        assertThat(response.getBody().path()).isEqualTo("/api/test/resource");
    }

    @Test
    @DisplayName("Should handle AuthenticationException and return 401 with ErrorResponseDto")
    void testHandleAuthenticationException() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleAuthenticationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().error()).isEqualTo("Unauthorized");
        assertThat(response.getBody().path()).isEqualTo("/api/test/resource");
    }

    @Test
    @DisplayName("Should handle AccessDeniedException and return 403 with ErrorResponseDto")
    void testHandleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");

        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleAccessDeniedException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().error()).isEqualTo("Forbidden");
        assertThat(response.getBody().path()).isEqualTo("/api/test/resource");
    }

    @Test
    @DisplayName("Should handle generic Exception and return 500 without leaking database details")
    void testHandleGenericExceptionWithDbLeakProtection() {
        SQLException sqlException = new SQLException("SELECT * FROM sensitive_table WHERE secret = 1; Table 'sensitive_table' locked");

        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleGenericException(sqlException, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message())
                .doesNotContain("sensitive_table")
                .contains("An unexpected internal server error occurred");
        assertThat(response.getBody().path()).isEqualTo("/api/test/resource");
    }

    @Test
    @DisplayName("Should handle generic Exception and return safe error message for standard failure")
    void testHandleGenericExceptionStandard() {
        RuntimeException ex = new RuntimeException("Service temporarily unreachable");

        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message()).isEqualTo("Service temporarily unreachable");
        assertThat(response.getBody().path()).isEqualTo("/api/test/resource");
    }
}
