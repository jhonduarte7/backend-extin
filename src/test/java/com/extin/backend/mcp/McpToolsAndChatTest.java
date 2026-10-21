package com.extin.backend.mcp;

import com.extin.backend.controllers.McpChatController;
import com.extin.backend.models.dtos.ChatRequestDto;
import com.extin.backend.models.dtos.ChatResponseDto;
import com.extin.backend.mcp.client.config.McpClientConfig;
import com.extin.backend.mcp.server.tools.CatalogTools;
import com.extin.backend.mcp.server.tools.CustomerTools;
import com.extin.backend.mcp.server.tools.DocumentTools;
import com.extin.backend.models.entities.Customer;
import com.extin.backend.models.entities.CustomerEquipment;
import com.extin.backend.repositories.CustomerEquipmentRepository;
import com.extin.backend.repositories.CustomerRepository;
import com.extin.backend.services.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit and component tests for MCP tools (CatalogTools, CustomerTools, DocumentTools),
 * ChatService conversation orchestration, and McpChatController.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class McpToolsAndChatTest {

    @Nested
    @DisplayName("CatalogTools Unit Tests")
    class CatalogToolsTests {

        @Mock
        private VectorStore vectorStore;

        private CatalogTools catalogTools;

        @BeforeEach
        void setUp() {
            catalogTools = new CatalogTools(vectorStore);
        }

        @Test
        @DisplayName("semanticProductSearch - Should return formatted items when matches found")
        void testSemanticProductSearchSuccess() {
            Document doc1 = Document.builder()
                    .text("Extintor PQS 10 Lbs - Polvo Químico Seco ABC")
                    .build();
            Document doc2 = Document.builder()
                    .text("Extintor CO2 5 Lbs - Dióxido de Carbono BC")
                    .build();

            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc1, doc2));

            String result = catalogTools.semanticProductSearch("extintor para oficina");

            assertThat(result).contains("Resultados del catálogo:");
            assertThat(result).contains("Extintor PQS 10 Lbs");
            assertThat(result).contains("Extintor CO2 5 Lbs");
            verify(vectorStore).similaritySearch(any(SearchRequest.class));
        }

        @Test
        @DisplayName("semanticProductSearch - Should handle no matches found")
        void testSemanticProductSearchNoMatches() {
            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

            String result = catalogTools.semanticProductSearch("bomba nuclear");

            assertThat(result).contains("No se encontraron productos o servicios en el catálogo");
        }

        @Test
        @DisplayName("semanticProductSearch - Should reject blank query")
        void testSemanticProductSearchBlankQuery() {
            String result = catalogTools.semanticProductSearch("   ");

            assertThat(result).contains("La consulta no puede estar vacía.");
        }
    }

    @Nested
    @DisplayName("CustomerTools Unit Tests")
    class CustomerToolsTests {

        @Mock
        private CustomerRepository customerRepository;

        @Mock
        private CustomerEquipmentRepository customerEquipmentRepository;

        private CustomerTools customerTools;

        @BeforeEach
        void setUp() {
            customerTools = new CustomerTools(customerRepository, customerEquipmentRepository);
        }

        @Test
        @DisplayName("getCustomerHistory - Should return customer data and equipment history")
        void testGetCustomerHistoryFound() {
            Customer customer = new Customer("J-12345678-9", "Inversiones El Sol C.A.", "Av. Libertador", "0212-5551234", "info@elsol.com");
            UUID customerId = UUID.randomUUID();
            customer.setId(customerId);

            CustomerEquipment equipment = new CustomerEquipment(
                    customer,
                    "Extintor PQS 10 Lbs",
                    LocalDate.now().minusMonths(6),
                    LocalDate.now().plusMonths(6)
            );
            equipment.setSerialNumber("SN-998877");
            equipment.setLocation("Almacén Principal");

            when(customerRepository.findByTaxId("J-12345678-9")).thenReturn(Optional.of(customer));
            when(customerEquipmentRepository.findByCustomerId(customerId)).thenReturn(List.of(equipment));

            String result = customerTools.getCustomerHistory("J-12345678-9");

            assertThat(result).contains("=== Datos del Cliente ===");
            assertThat(result).contains("J-12345678-9");
            assertThat(result).contains("Inversiones El Sol C.A.");
            assertThat(result).contains("info@elsol.com");
            assertThat(result).contains("Extintor PQS 10 Lbs");
            assertThat(result).contains("SN-998877");
            assertThat(result).contains("Almacén Principal");
        }

        @Test
        @DisplayName("getCustomerHistory - Should return not found when taxId does not exist")
        void testGetCustomerHistoryNotFound() {
            when(customerRepository.findByTaxId("J-00000000-0")).thenReturn(Optional.empty());

            String result = customerTools.getCustomerHistory("J-00000000-0");

            assertThat(result).contains("No se encontró ningún cliente registrado con el RIF/ID: J-00000000-0");
        }

        @Test
        @DisplayName("getCustomerHistory - Should reject blank taxId")
        void testGetCustomerHistoryBlank() {
            String result = customerTools.getCustomerHistory("  ");

            assertThat(result).contains("El RIF o identificación fiscal no puede estar vacío.");
        }
    }

    @Nested
    @DisplayName("DocumentTools Unit Tests")
    class DocumentToolsTests {

        @Mock
        private VectorStore vectorStore;

        private DocumentTools documentTools;

        @BeforeEach
        void setUp() {
            documentTools = new DocumentTools(vectorStore);
        }

        @Test
        @DisplayName("searchInManuals - Should return formatted chunks when matches found")
        void testSearchInManualsSuccess() {
            Document doc1 = Document.builder()
                    .text("Altura de montaje: 1.50 metros desde el piso acabado.")
                    .metadata("file_name", "manual-nfpa10.pdf")
                    .metadata("page_number", 5)
                    .build();

            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc1));

            String result = documentTools.searchInManuals("altura de montaje de extintores");

            assertThat(result).contains("=== Fragmentos Relevantes de Manuales y Normativas ===");
            assertThat(result).contains("manual-nfpa10.pdf");
            assertThat(result).contains("Pág: 5");
            assertThat(result).contains("Altura de montaje: 1.50 metros");
            verify(vectorStore).similaritySearch(any(SearchRequest.class));
        }

        @Test
        @DisplayName("searchInManuals - Should handle no matches found")
        void testSearchInManualsNoMatches() {
            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

            String result = documentTools.searchInManuals("normativa de submarinos");

            assertThat(result).contains("No se encontraron fragmentos relevantes");
        }

        @Test
        @DisplayName("searchInManuals - Should reject blank query")
        void testSearchInManualsBlankQuery() {
            String result = documentTools.searchInManuals("   ");

            assertThat(result).contains("La consulta sobre manuales o normativas no puede estar vacía.");
        }
    }

    @Nested
    @DisplayName("McpClientConfig Unit Tests")
    class McpClientConfigTests {

        @Test
        @DisplayName("chatMemory bean creation - Should instantiate in-memory chat memory")
        void testChatMemoryBeanCreation() {
            McpClientConfig config = new McpClientConfig();
            ChatMemory memory = config.chatMemory();

            assertThat(memory).isNotNull();
        }
    }

    @Nested
    @DisplayName("ChatService MCP Logic & System Prompt Tests")
    class ChatServiceMcpTests {

        @Mock
        private ChatClient chatClient;

        @Mock
        private ChatClient.ChatClientRequestSpec requestSpec;

        @Mock
        private ChatClient.CallResponseSpec callResponseSpec;

        @Mock
        private CustomerEquipmentRepository customerEquipmentRepository;

        @Mock
        private CustomerRepository customerRepository;

        private ChatService chatService;

        @BeforeEach
        void setUp() {
            chatService = new ChatService(chatClient, customerEquipmentRepository, customerRepository);
        }

        @Test
        @DisplayName("processChat - Should inject SYSTEM_PROMPT, advisors and return markdown response")
        void testProcessChatSuccess() {
            String expectedMarkdown = "### Recomendación Técnica\n- Extintor CO2 5 Lbs";

            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.system(eq(ChatService.SYSTEM_PROMPT))).thenReturn(requestSpec);
            when(requestSpec.user("¿Qué extintor sirve para servidores?")).thenReturn(requestSpec);
            when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
            when(requestSpec.call()).thenReturn(callResponseSpec);
            when(callResponseSpec.content()).thenReturn(expectedMarkdown);

            String reply = chatService.processChat("session-101", "¿Qué extintor sirve para servidores?");

            assertThat(reply).isEqualTo(expectedMarkdown);
            verify(requestSpec).system(ChatService.SYSTEM_PROMPT);
            verify(requestSpec).user("¿Qué extintor sirve para servidores?");
            verify(requestSpec).advisors(any(Consumer.class));
        }

        @Test
        @DisplayName("processChat - Should reject blank prompt")
        void testProcessChatBlankPrompt() {
            String reply = chatService.processChat("session-101", "   ");

            assertThat(reply).isEqualTo("Por favor ingresa una consulta válida.");
        }

        @Test
        @DisplayName("processChat - Should catch exceptions and provide friendly error markdown")
        void testProcessChatExceptionFallback() {
            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.system(anyString())).thenReturn(requestSpec);
            when(requestSpec.user(anyString())).thenReturn(requestSpec);
            when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
            when(requestSpec.call()).thenThrow(new RuntimeException("Connection refused to Ollama"));

            String reply = chatService.processChat("session-101", "Hola");

            assertThat(reply).contains("⚠️ **Error en el Asistente IA:**");
            assertThat(reply).contains("Connection refused to Ollama");
        }
    }

    @Nested
    @DisplayName("McpChatController Unit Tests")
    class McpChatControllerTests {

        @Mock
        private ChatService chatService;

        private McpChatController controller;

        @BeforeEach
        void setUp() {
            controller = new McpChatController(chatService);
        }

        @Test
        @DisplayName("POST /api/admin/chat/mcp - Should delegate to ChatService and return JSON response with chatId")
        void testChatMcpJsonResponse() {
            String expectedMarkdown = "### Recomendación Técnica\n- **Extintor PQS 10 Lbs**: Ideal para almacenes.";
            when(chatService.processChat(eq("custom-chat-id"), eq("¿Qué extintor me recomiendas?"))).thenReturn(expectedMarkdown);

            ChatRequestDto requestDto = new ChatRequestDto("¿Qué extintor me recomiendas?", "custom-chat-id");
            ResponseEntity<?> response = controller.chat(requestDto, null, false, "application/json");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isInstanceOf(ChatResponseDto.class);

            ChatResponseDto body = (ChatResponseDto) response.getBody();
            assertThat(body.reply()).isEqualTo(expectedMarkdown);
            assertThat(body.chatId()).isEqualTo("custom-chat-id");
            assertThat(body.toolsUsed()).contains("semanticProductSearch", "getCustomerHistory", "searchInManuals");
            verify(chatService).processChat(eq("custom-chat-id"), eq("¿Qué extintor me recomiendas?"));
        }

        @Test
        @DisplayName("POST /api/admin/chat/mcp - Should use header X-Chat-Id when provided")
        void testChatMcpHeaderChatId() {
            String expectedMarkdown = "Respuesta contextual.";
            when(chatService.processChat(eq("header-chat-id"), eq("Hola"))).thenReturn(expectedMarkdown);

            ChatRequestDto requestDto = new ChatRequestDto("Hola");
            ResponseEntity<?> response = controller.chat(requestDto, "header-chat-id", false, "application/json");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(chatService).processChat(eq("header-chat-id"), eq("Hola"));
        }

        @Test
        @DisplayName("POST /api/admin/chat/mcp - Should return raw Markdown when requested")
        void testChatMcpRawMarkdown() {
            String expectedMarkdown = "# Guía de Extintores\nRespuesta directa en Markdown.";
            when(chatService.processChat(eq("default-chat-session"), eq("Información"))).thenReturn(expectedMarkdown);

            ChatRequestDto requestDto = new ChatRequestDto("Información");
            ResponseEntity<?> response = controller.chat(requestDto, null, true, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(expectedMarkdown);
            assertThat(response.getHeaders().getContentType().toString()).contains("text/markdown");
            verify(chatService).processChat(eq("default-chat-session"), eq("Información"));
        }
    }
}
