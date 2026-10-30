package com.extin.backend.mcp.server.tools;

import com.extin.backend.models.entities.Customer;
import com.extin.backend.models.entities.CustomerEquipment;
import com.extin.backend.repositories.CustomerEquipmentRepository;
import com.extin.backend.repositories.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Domain-specific tool for retrieving customer history and fiscal data.
 * Conforms to docs/system-spec.md Section 4 (Tool 2: tool_customer_history).
 *
 * Optimizations:
 * - Bounds equipment list to a maximum of 8 items to protect LLM context window.
 * - Appends summary count for additional equipment.
 */
@Component
public class CustomerTools {

    private static final Logger log = LoggerFactory.getLogger(CustomerTools.class);
    public static final int MAX_EQUIPMENT_DISPLAY = 8;

    private final CustomerRepository customerRepository;
    private final CustomerEquipmentRepository customerEquipmentRepository;

    public CustomerTools(CustomerRepository customerRepository,
                         CustomerEquipmentRepository customerEquipmentRepository) {
        this.customerRepository = customerRepository;
        this.customerEquipmentRepository = customerEquipmentRepository;
    }

    /**
     * Retrieves customer information, contact details and registered equipment history.
     *
     * @param taxId Fiscal identifier / RIF of the customer.
     * @return Summary string of customer details or not found message.
     */
    @Tool(name = "getCustomerHistory",
          description = "Usa esta herramienta SIEMPRE para buscar información exacta, historial o datos transaccionales de un cliente usando su ID o RIF")
    public String getCustomerHistory(
            @ToolParam(description = "RIF o identificación fiscal del cliente (ej. J-12345678-0)")
            String taxId) {
        log.info("Executing CustomerTools.getCustomerHistory with taxId='{}'", taxId);

        if (taxId == null || taxId.isBlank()) {
            return "El RIF o identificación fiscal no puede estar vacío.";
        }

        String normalizedTaxId = taxId.trim();
        Optional<Customer> customerOpt = customerRepository.findByTaxId(normalizedTaxId);

        if (customerOpt.isEmpty()) {
            return "No se encontró ningún cliente registrado con el RIF/ID: " + normalizedTaxId;
        }

        Customer customer = customerOpt.get();
        StringBuilder sb = new StringBuilder();
        sb.append("=== Datos del Cliente ===\n");
        sb.append("- RIF/ID: ").append(customer.getTaxId()).append("\n");
        sb.append("- Razón Social: ").append(customer.getBusinessName()).append("\n");
        sb.append("- Dirección: ").append(customer.getAddress() != null ? customer.getAddress() : "No registrada").append("\n");
        sb.append("- Teléfono: ").append(customer.getPhone() != null ? customer.getPhone() : "No registrado").append("\n");
        sb.append("- Email: ").append(customer.getEmail() != null ? customer.getEmail() : "No registrado").append("\n");
        sb.append("- Fecha de Registro: ").append(customer.getCreatedAt() != null ? customer.getCreatedAt().toString() : "N/A").append("\n");

        try {
            List<CustomerEquipment> equipmentList = customerEquipmentRepository.findByCustomerId(customer.getId());
            if (equipmentList != null && !equipmentList.isEmpty()) {
                int displayLimit = Math.min(equipmentList.size(), MAX_EQUIPMENT_DISPLAY);
                sb.append("\n=== Equipos y Extintores Registrados (").append(equipmentList.size()).append(") ===\n");
                for (int i = 0; i < displayLimit; i++) {
                    CustomerEquipment eq = equipmentList.get(i);
                    sb.append("• ").append(eq.getEquipmentType())
                      .append(" | Serial: ").append(eq.getSerialNumber() != null ? eq.getSerialNumber() : "S/N")
                      .append(" | Ubicación: ").append(eq.getLocation() != null ? eq.getLocation() : "Principal")
                      .append(" | Vencimiento: ").append(eq.getExpirationDate() != null ? eq.getExpirationDate().toString() : "No registrada")
                      .append("\n");
                }
                if (equipmentList.size() > displayLimit) {
                    sb.append("... y ").append(equipmentList.size() - displayLimit)
                      .append(" equipo(s) adicional(es) registrado(s) para este cliente.\n");
                }
            } else {
                sb.append("\nNo tiene equipos registrados en el sistema actualmente.\n");
            }
        } catch (Exception e) {
            log.warn("Could not retrieve customer equipment for customerId={}: {}", customer.getId(), e.getMessage());
        }

        return sb.toString();
    }
}
