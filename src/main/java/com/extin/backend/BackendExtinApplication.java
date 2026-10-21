package com.extin.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the EXTIN Fire Safety Management backend service.
 * <p>
 * This application provides comprehensive RESTful APIs, multi-currency quoting,
 * fiscal invoicing, document RAG (Retrieval-Augmented Generation), and an AI Assistant
 * backed by Spring AI and the Model Context Protocol (MCP).
 * </p>
 *
 * Annotations:
 * <ul>
 *   <li>{@link SpringBootApplication} enables auto-configuration, component scanning, and property support.</li>
 *   <li>{@link EnableAsync} enables background asynchronous execution (e.g., event-driven auto-vectorization).</li>
 * </ul>
 */
@SpringBootApplication
@EnableAsync
public class BackendExtinApplication {

	/**
	 * Application bootstrap method.
	 *
	 * @param args command-line arguments passed during startup.
	 */
	public static void main(String[] args) {
		SpringApplication.run(BackendExtinApplication.class, args);
	}

}
