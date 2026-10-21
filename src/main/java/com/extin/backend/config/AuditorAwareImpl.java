package com.extin.backend.config;

import com.extin.backend.models.entities.User;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Implementation of Spring Data JPA AuditorAware to resolve the currently authenticated
 * user's email/identifier for @CreatedBy and @LastModifiedBy auditing fields.
 * Returns "SYSTEM" if the operation is unauthenticated or executed during startup/seeding.
 */
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of("SYSTEM");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                return Optional.of(user.getEmail());
            }
            return Optional.of(user.getUsername());
        }

        if (principal instanceof UserDetails userDetails) {
            return Optional.of(userDetails.getUsername());
        }

        if (principal instanceof String str && !"anonymousUser".equalsIgnoreCase(str)) {
            return Optional.of(str);
        }

        String name = authentication.getName();
        if (name != null && !name.isBlank() && !"anonymousUser".equalsIgnoreCase(name)) {
            return Optional.of(name);
        }

        return Optional.of("SYSTEM");
    }
}
