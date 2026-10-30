package com.extin.backend.mcp.client.config;

import io.netty.channel.ChannelOption;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Infrastructure configuration for Spring AI MCP client components.
 * Located in com.extin.backend.mcp.client.config as per Clean Architecture / SDD guidelines.
 *
 * Responsibilities:
 * - Declares technical infrastructure beans such as ChatMemory.
 * - Configures HTTP client timeouts (connect: 120s, read: 180s) across RestClient and WebClient
 *   to ensure complex local inference and tool-calling loops with Ollama (Llama 3.1)
 *   do not terminate prematurely.
 * - Adheres to Separation of AI Concerns: System prompt and business orchestration
 *   reside exclusively in ChatService.
 */
@Configuration
public class McpClientConfig {

    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(120);
    public static final Duration READ_TIMEOUT = Duration.ofSeconds(180);

    /**
     * In-memory ChatMemory bean providing conversational history buffer.
     * Backed by InMemoryChatMemoryRepository with a rolling window of recent messages.
     *
     * @return ChatMemory bean instance.
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    /**
     * Customizes RestClient.Builder used by OllamaApi and other HTTP clients
     * to enforce extended connect and read timeouts (>= 120s).
     *
     * @return RestClientCustomizer bean.
     */
    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
            requestFactory.setReadTimeout(READ_TIMEOUT);
            restClientBuilder.requestFactory(requestFactory);
        };
    }

    /**
     * Customizes WebClient.Builder used for reactive and streaming operations with Ollama
     * to enforce extended connection and response timeouts.
     *
     * @return WebClientCustomizer bean.
     */
    @Bean
    public WebClientCustomizer webClientCustomizer() {
        return webClientBuilder -> {
            HttpClient httpClient = HttpClient.create()
                    .responseTimeout(READ_TIMEOUT)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis());
            webClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient));
        };
    }
}
