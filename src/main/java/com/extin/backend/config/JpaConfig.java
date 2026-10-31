package com.extin.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JPA and Database configuration for the backend.
 * Enables repository scanning, JPA Auditing with AuditorAwareImpl, and pgvector initialization.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.extin.backend.repositories")
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class JpaConfig {

    private static final Logger log = LoggerFactory.getLogger(JpaConfig.class);

    @Bean
    public AuditorAware<String> auditorAwareImpl() {
        return new AuditorAwareImpl();
    }

    /**
     * Ensures PostgreSQL pgvector extension is installed on application startup.
     * Safely catches non-PostgreSQL dialect exceptions (e.g., in-memory H2 tests).
     */
    @Bean
    @Order(0)
    public ApplicationRunner pgVectorExtensionInitializer(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                log.info("Ensuring PostgreSQL pgvector extension is installed...");
                jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector;");
                log.info("PostgreSQL pgvector extension verified successfully.");
            } catch (Exception e) {
                log.warn("Notice: 'CREATE EXTENSION IF NOT EXISTS vector' skipped or not applicable (e.g., H2 test database). Details: {}", e.getMessage());
            }
        };
    }
}
