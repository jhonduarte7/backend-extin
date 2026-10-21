package com.extin.backend.config;

import com.extin.backend.models.entities.CatalogItem;
import com.extin.backend.models.entities.Customer;
import com.extin.backend.models.entities.User;
import com.extin.backend.models.enums.ItemType;
import com.extin.backend.models.enums.Role;
import com.extin.backend.repositories.CatalogItemRepository;
import com.extin.backend.repositories.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying JPA Auditing fields (@CreatedBy, @LastModifiedBy, @CreatedDate, @LastModifiedDate)
 * and {@link AuditorAwareImpl} behavior under authenticated and unauthenticated contexts.
 */
@SpringBootTest
class JpaAuditingTest {

    @MockitoBean
    private VectorStore vectorStore;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CatalogItemRepository catalogItemRepository;

    @Autowired
    private AuditorAwareImpl auditorAware;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("AuditorAware should return SYSTEM when no security context exists")
    void testAuditorAwareNoAuthentication() {
        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertThat(auditor).contains("SYSTEM");
    }

    @Test
    @DisplayName("AuditorAware should return SYSTEM for AnonymousAuthenticationToken")
    void testAuditorAwareAnonymous() {
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertThat(auditor).contains("SYSTEM");
    }

    @Test
    @DisplayName("AuditorAware should return user email when authenticated User has email")
    void testAuditorAwareWithUserEmail() {
        User user = new User("admin_test", "pwd", "admin.audit@extin.com", Role.ADMIN);
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertThat(auditor).contains("admin.audit@extin.com");
    }

    @Test
    @DisplayName("AuditorAware should return username when authenticated User has null or blank email")
    void testAuditorAwareWithUsernameFallback() {
        User user = new User("operator_test", "pwd", null, Role.USER);
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertThat(auditor).contains("operator_test");
    }

    @Test
    @DisplayName("Entity persisting without authentication should set createdBy and lastModifiedBy to SYSTEM")
    void testEntityAuditWithoutAuthentication() {
        Customer customer = new Customer(
                "J-77665544-3",
                "Audited Company C.A.",
                "Zona Industrial 1",
                "+58 212 9998877",
                "contacto@audit.com"
        );

        Customer saved = customerRepository.save(customer);

        assertThat(saved.getCreatedBy()).isEqualTo("SYSTEM");
        assertThat(saved.getLastModifiedBy()).isEqualTo("SYSTEM");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Entity persisting with authenticated user should set createdBy and lastModifiedBy to user email")
    void testEntityAuditWithAuthenticatedUser() {
        User user = new User("supervisor", "secret", "supervisor@extin.com", Role.ADMIN);
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        CatalogItem item = new CatalogItem(
                "AUDIT-SKU-001",
                "Extintor Prueba Auditoria 5L",
                ItemType.PRODUCT,
                new BigDecimal("49.99")
        );

        CatalogItem saved = catalogItemRepository.save(item);

        assertThat(saved.getCreatedBy()).isEqualTo("supervisor@extin.com");
        assertThat(saved.getLastModifiedBy()).isEqualTo("supervisor@extin.com");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
