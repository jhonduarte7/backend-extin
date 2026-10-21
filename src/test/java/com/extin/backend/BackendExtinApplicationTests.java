package com.extin.backend;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Integration test verifying that the Spring Boot ApplicationContext loads successfully.
 */
@SpringBootTest
class BackendExtinApplicationTests {

	@MockitoBean
	private VectorStore vectorStore;

	/**
	 * Verifies that all Spring beans, configurations, and repository definitions wire correctly.
	 */
	@Test
	void contextLoads() {
	}

}
