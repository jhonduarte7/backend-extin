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
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final List<ToolCallback> uniqueToolCallbacks;

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

        // Limpia y deduplica la lista de callbacks para que cada herramienta aparezca estrictamente una sola vez
        this.uniqueToolCallbacks = resolveUniqueToolCallbacks(catalogTools, customerTools, documentTools);

        // Configuración de ChatClient con SYSTEM_PROMPT y MessageChatMemoryAdvisor.
        // NOTA: No se inyectan defaultTools aquí para evitar duplicidad con ToolCallingChatOptions
        // en la invocación por prompt.
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
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
        this.uniqueToolCallbacks = Collections.emptyList();
    }

    /**
     * Resolves ToolCallbacks from provided tool beans and deduplicates them strictly by tool name,
     * ensuring that each tool (semanticProductSearch, getCustomerHistory, searchInManuals)
     * appears strictly once in ToolCallingChatOptions before invoking the AI model.
     *
     * @param toolBeans Array of Spring components containing @Tool methods.
     * @return Immutable list of unique ToolCallback instances verified with validateToolCallbacks.
     */
    public static List<ToolCallback> resolveUniqueToolCallbacks(Object... toolBeans) {
        if (toolBeans == null || toolBeans.length == 0) {
            return Collections.emptyList();
        }

        List<Object> nonNullTools = Arrays.stream(toolBeans)
                .filter(Objects::nonNull)
                .toList();

        if (nonNullTools.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, ToolCallback> uniqueMap = new LinkedHashMap<>();
        for (Object tool : nonNullTools) {
            ToolCallback[] callbacks = ToolCallbacks.from(tool);
            for (ToolCallback callback : callbacks) {
                if (callback != null && callback.getToolDefinition() != null) {
                    String toolName = callback.getToolDefinition().name();
                    if (uniqueMap.putIfAbsent(toolName, callback) != null) {
                        log.warn("ChatService: Duplicate tool '{}' detected from source '{}' and omitted from ToolCallingChatOptions.",
                                toolName, tool.getClass().getSimpleName());
                    } else {
                        log.debug("ChatService: Registered unique tool callback: '{}'", toolName);
                    }
                }
            }
        }

        List<ToolCallback> cleanedList = List.copyOf(uniqueMap.values());
        // Validación estricta con Spring AI para asegurar que no existan nombres duplicados
        ToolCallingChatOptions.validateToolCallbacks(cleanedList);
        return cleanedList;
    }

    /**
     * Returns the deduplicated list of ToolCallback instances configured for the assistant.
     */
    public List<ToolCallback> getUniqueToolCallbacks() {
        return this.uniqueToolCallbacks;
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

            // Inyecta la lista limpia y desduplicada de callbacks estrictamente una vez
            if (uniqueToolCallbacks != null && !uniqueToolCallbacks.isEmpty()) {
                promptSpec.toolCallbacks(uniqueToolCallbacks);
            }

            String markdownReply = promptSpec.call().content();

            if (markdownReply == null || markdownReply.isBlank()) {
                return "No se obtuvo respuesta del modelo de lenguaje.";
            }

            return markdownReply;
        } catch (Exception e) {
            log.error("ChatService: Error executing ChatClient call for chatId='{}': {}", effectiveChatId, e.getMessage(), e);
            return buildContingencyResponse(e);
        }
    }

    /**
     * Executes reactive streaming chat isolating conversations by chatId, injecting
     * the SYSTEM_PROMPT by default, passing the user message, and enabling domain tools.
     * Streams tokens in real time via Flux to prevent client timeouts on local LLM inference.
     *
     * @param chatId Unique identifier for the user session or conversation.
     * @param userMessage The natural language question or request.
     * @return Flux of streaming tokens from the LLM.
     */
    public Flux<String> streamChat(String chatId, String userMessage) {
        String effectiveChatId = (chatId != null && !chatId.isBlank()) ? chatId : "default-chat-session";
        log.info("ChatService: Streaming chat for chatId='{}', message='{}'", effectiveChatId, userMessage);

        if (userMessage == null || userMessage.isBlank()) {
            return Flux.just("Por favor ingresa una consulta válida.");
        }

        try {
            var promptSpec = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, effectiveChatId));

            // Inyecta la lista limpia y desduplicada de callbacks estrictamente una vez
            if (uniqueToolCallbacks != null && !uniqueToolCallbacks.isEmpty()) {
                promptSpec.toolCallbacks(uniqueToolCallbacks);
            }

            return promptSpec.stream().content()
                    .filter(Objects::nonNull)
                    .onErrorResume(e -> {
                        log.error("ChatService: Error during chat stream for chatId='{}': {}", effectiveChatId, e.getMessage(), e);
                        return Flux.just(buildContingencyResponse(e));
                    });
        } catch (Exception e) {
            log.error("ChatService: Error initializing chat stream for chatId='{}': {}", effectiveChatId, e.getMessage(), e);
            return Flux.just(buildContingencyResponse(e));
        }
    }

    /**
     * Constructs a friendly, structured Markdown contingency message when local AI inference
     * encounters a timeout, network failure, or service unavailability, preventing HTTP 500 errors.
     *
     * @param e Caught exception or throwable from ChatClient execution.
     * @return User-friendly contingency Markdown message.
     */
    public String buildContingencyResponse(Throwable e) {
        String msg = (e.getMessage() != null && !e.getMessage().isBlank()) ? e.getMessage() : e.getClass().getSimpleName();
        String lowerMsg = msg.toLowerCase();

        boolean isTimeout = e instanceof java.net.SocketTimeoutException
                || e instanceof java.net.http.HttpTimeoutException
                || lowerMsg.contains("timeout")
                || lowerMsg.contains("timed out")
                || (e.getCause() != null && e.getCause().toString().toLowerCase().contains("timeout"));

        boolean isConnectionError = e instanceof java.net.ConnectException
                || lowerMsg.contains("connection refused")
                || lowerMsg.contains("connect")
                || (e.getCause() != null && e.getCause().toString().toLowerCase().contains("connect"));

        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ **Error en el Asistente IA:** Modo Contingencia — ");

        if (isTimeout) {
            sb.append("La inferencia con el modelo local de IA excedió el tiempo de espera configurado (120s).\n\n");
            sb.append("### Motivo:\n");
            sb.append("- La consulta es compleja o requirió múltiples herramientas consecutivas.\n");
            sb.append("- El modelo local (`llama3.1`) está operando bajo alta carga de inferencia en CPU/GPU.\n\n");
            sb.append("### Sugerencias:\n");
            sb.append("1. Formula una pregunta más concreta (ej. consultar por un extintor específico o el RIF exacto del cliente).\n");
            sb.append("2. Reintenta la consulta en unos momentos.\n\n");
            sb.append("*Detalle técnico:* `").append(msg).append("`");
        } else if (isConnectionError) {
            sb.append("No fue posible establecer conexión con el motor local de IA (Ollama).\n\n");
            sb.append("### Sugerencias de Verificación:\n");
            sb.append("1. Asegúrate de que Ollama esté ejecutándose en `http://localhost:11434` (`ollama serve`).\n");
            sb.append("2. Comprueba que el modelo `llama3.1` esté disponible localmente (`ollama list`).\n\n");
            sb.append("*Detalle técnico:* `").append(msg).append("`");
        } else {
            sb.append(msg).append("\n\n");
            sb.append("*Asegúrate de que Ollama esté ejecutándose con el modelo configurado (ej. llama3.1).*");
        }

        return sb.toString();
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

        try {
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
        } catch (Exception e) {
            log.error("ChatService: Error processing rule-based chat prompt: {}", e.getMessage(), e);
            reply = new StringBuilder("⚠️ **Modo Contingencia:** Ocurrió un error al procesar los datos locales: ").append(e.getMessage());
        }

        return new ChatResponseDto(
                reply.toString().trim(),
                toolsUsed,
                Collections.emptyList()
        );
    }
}
