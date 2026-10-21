package com.extin.backend.services;

import com.extin.backend.mcp.server.tools.CatalogTools;
import com.extin.backend.mcp.server.tools.CustomerTools;
import com.extin.backend.mcp.server.tools.DocumentTools;
import com.extin.backend.models.dtos.ChatRequestDto;
import com.extin.backend.models.dtos.ChatResponseDto;
import com.extin.backend.models.entities.CustomerEquipment;
import com.extin.backend.repositories.CustomerEquipmentRepository;
import com.extin.backend.repositories.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service orchestrating the MCP (Model Context Protocol) AI assistant queries and business logic.
 * Encapsulates the domain System Prompt, ChatMemory conversation state, and orchestrates
 * the ChatClient with backend domain tools.
 *
 * Conforms to docs/system-spec.md Section 7.4 (Separation of AI Concerns).
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /**
     * Domain System Prompt defining strict behavioral rules, fire safety guidelines,
     * tool usage rules, and mandatory Markdown formatting.
     */
    public static final String SYSTEM_PROMPT = """
            Eres un asistente experto en seguridad contra incendios, extintores y normativas NFPA/COVENIN de la empresa Extin.
            Tu objetivo es ayudar a los clientes y administradores con información técnica precisa sobre el catálogo de extintores y servicios, normativas de seguridad e historial de clientes.
            
            REGLAS ESTRICTAS DE COMPORTAMIENTO:
            1. NUNCA respondas preguntas fuera del dominio de seguridad industrial, extintores, prevención de incendios o la empresa Extin. Si te preguntan algo no relacionado (por ejemplo política, deportes, turismo, recetas), recházalo cordialmente indicando que tu ámbito exclusivo es la seguridad contra incendios y los servicios de Extin.
            2. Utiliza SIEMPRE las herramientas disponibles para buscar datos reales y comprobables antes de responder:
               - semanticProductSearch: para buscar productos, extintores, precios o servicios de recarga en el catálogo.
               - getCustomerHistory: para buscar información exacta, historial o datos transaccionales de un cliente usando su RIF/CNPJ.
               - searchInManuals: para buscar respuestas sobre normativas, manuales, reglas de instalación o seguridad en los documentos PDF.
            3. Responde SIEMPRE en formato Markdown limpio, estructurado y profesional (usa títulos, subtítulos, viñetas o tablas según convenga).
            4. Sé cordial, conciso y técnicamente riguroso en tus recomendaciones.
            """;

    private final ChatMemory chatMemory;
    private final ChatClient chatClient;
    private final CatalogTools catalogTools;
    private final CustomerTools customerTools;
    private final DocumentTools documentTools;
    private final CustomerEquipmentRepository customerEquipmentRepository;
    private final CustomerRepository customerRepository;

    /**
     * Primary constructor injecting ChatClient.Builder, tools, repositories, and ChatMemory.
     */
    @Autowired
    public ChatService(ChatClient.Builder chatClientBuilder,
                       CatalogTools catalogTools,
                       CustomerTools customerTools,
                       DocumentTools documentTools,
                       CustomerEquipmentRepository customerEquipmentRepository,
                       CustomerRepository customerRepository,
                       @Autowired(required = false) ChatMemory chatMemory) {
        this.catalogTools = catalogTools;
        this.customerTools = customerTools;
        this.documentTools = documentTools;
        this.customerEquipmentRepository = customerEquipmentRepository;
        this.customerRepository = customerRepository;

        // InMemoryChatMemory / MessageWindowChatMemory con repositorio en memoria
        this.chatMemory = (chatMemory != null)
                ? chatMemory
                : MessageWindowChatMemory.builder()
                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                        .maxMessages(20)
                        .build();

        // Configuración de ChatClient con SYSTEM_PROMPT, tools y MessageChatMemoryAdvisor
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(catalogTools, customerTools, documentTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(this.chatMemory).build())
                .build();
    }

    /**
     * Overloaded constructor for unit testing with a pre-mocked ChatClient.
     */
    public ChatService(ChatClient chatClient,
                       CustomerEquipmentRepository customerEquipmentRepository,
                       CustomerRepository customerRepository) {
        this.chatClient = chatClient;
        this.catalogTools = null;
        this.customerTools = null;
        this.documentTools = null;
        this.customerEquipmentRepository = customerEquipmentRepository;
        this.customerRepository = customerRepository;
        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    /**
     * Primary chat execution method isolating conversations by chatId, injecting
     * the SYSTEM_PROMPT by default, passing the user message, and enabling domain tools.
     *
     * @param chatId Unique identifier for the user session or conversation.
     * @param userMessage The natural language question or request.
     * @return Formatted Markdown response from the LLM.
     */
    public String processChat(String chatId, String userMessage) {
        String effectiveChatId = (chatId != null && !chatId.isBlank()) ? chatId : "default-chat-session";
        log.info("ChatService: Processing chat for chatId='{}', message='{}'", effectiveChatId, userMessage);

        if (userMessage == null || userMessage.isBlank()) {
            return "Por favor ingresa una consulta válida.";
        }

        try {
            var promptSpec = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, effectiveChatId));

            if (catalogTools != null && customerTools != null && documentTools != null) {
                promptSpec.tools(catalogTools, customerTools, documentTools);
            }

            String markdownReply = promptSpec.call().content();

            if (markdownReply == null || markdownReply.isBlank()) {
                return "No se obtuvo respuesta del modelo de lenguaje.";
            }

            return markdownReply;
        } catch (Exception e) {
            log.error("ChatService: Error executing ChatClient call for chatId='{}': {}", effectiveChatId, e.getMessage(), e);
            return "⚠️ **Error en el Asistente IA:** " + e.getMessage() +
                    "\n\n*Asegúrate de que Ollama esté ejecutándose con el modelo configurado (ej. llama3.1).*";
        }
    }

    /**
     * Executes natural language query using default session ID.
     */
    public String generateMcpResponse(String userPrompt) {
        return processChat("default-chat-session", userPrompt);
    }

    /**
     * Overloaded method with explicit chatId.
     */
    public String generateMcpResponse(String chatId, String userPrompt) {
        return processChat(chatId, userPrompt);
    }

    /**
     * Returns the ChatMemory instance used by the service.
     */
    public ChatMemory getChatMemory() {
        return this.chatMemory;
    }

    /**
     * Processes natural language queries via direct SQL rule-based tools (Legacy/Phase 1 support).
     */
    public ChatResponseDto processChat(ChatRequestDto requestDto) {
        String prompt = requestDto.prompt() != null ? requestDto.prompt().toLowerCase() : "";
        List<String> toolsUsed = new ArrayList<>();
        StringBuilder reply = new StringBuilder();

        if (prompt.contains("vencid") || prompt.contains("expirad") || prompt.contains("mantenimiento") || prompt.contains("extintor")) {
            toolsUsed.add("tool_sql_customer_history");
            LocalDate threshold = LocalDate.now();
            List<CustomerEquipment> expired = customerEquipmentRepository.findByExpirationDateBefore(threshold);

            if (expired.isEmpty()) {
                reply.append("Actualmente no se registran clientes con extintores o equipos vencidos.");
            } else {
                reply.append(String.format("Se encontraron %d equipo(s) con fecha de recarga vencida o por vencer:%n", expired.size()));
                for (int i = 0; i < Math.min(expired.size(), 10); i++) {
                    CustomerEquipment eq = expired.get(i);
                    reply.append(String.format("- %s (RIF/ID: %s): %s, venció el %s.%n",
                            eq.getCustomer().getBusinessName(),
                            eq.getCustomer().getTaxId(),
                            eq.getEquipmentType(),
                            eq.getExpirationDate()));
                }
                if (expired.size() > 10) {
                    reply.append(String.format("... y %d equipos adicionales.", expired.size() - 10));
                }
            }
        } else if (prompt.contains("cliente") || prompt.contains("total")) {
            toolsUsed.add("tool_sql_customer_history");
            long totalCustomers = customerRepository.count();
            reply.append(String.format("En el sistema se encuentran registrados %d clientes en total.", totalCustomers));
        } else {
            reply.append("Hola. Soy tu Asistente de Gestión Contra Incendios (MCP). Puedes consultarme sobre clientes, extintores vencidos, catálogo de productos y servicios o historial de cotizaciones.");
        }

        return new ChatResponseDto(
                reply.toString().trim(),
                toolsUsed,
                Collections.emptyList()
        );
    }
}
