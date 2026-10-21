package com.extin.backend.config;

import com.extin.backend.models.entities.CatalogItem;
import com.extin.backend.models.enums.ItemType;
import com.extin.backend.repositories.CatalogItemRepository;
import com.extin.backend.services.VectorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DataInitializer performs initial AI vectorization seeding for baseline catalog items.
 * Conforms to Phase 3 requirements in docs/system-spec.md Section 7.2.
 */
@Component
@Order(2)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CatalogItemRepository catalogItemRepository;
    private final VectorizationService vectorizationService;

    public DataInitializer(CatalogItemRepository catalogItemRepository,
                           VectorizationService vectorizationService) {
        this.catalogItemRepository = catalogItemRepository;
        this.vectorizationService = vectorizationService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (catalogItemRepository.count() == 0) {
            log.info("Starting DataInitializer: Checking and vectorizing baseline catalog items into pgvector...");

            // Define 4 representative baseline items (PQS, CO2, Solkaflam, Service)
            List<CatalogItem> seedItems = List.of(
                    createOrGet("EXT-PQS-10", "Extintor Polvo Químico Seco (PQS) 10 Lbs ABC", ItemType.PRODUCT,
                            new BigDecimal("38.50"), "Extintor portátil multipropósito para fuegos clase A, B y C con manómetro y manguera."),
                    createOrGet("EXT-CO2-5", "Extintor Dióxido de Carbono (CO2) 5 Lbs Clase BC", ItemType.PRODUCT,
                            new BigDecimal("65.00"), "Extintor de CO2 para salas eléctricas, servidores y laboratorios. No deja residuos."),
                    createOrGet("EXT-SOLK-10", "Extintor Agente Limpio Solkaflam 10 Lbs Clase ABC", ItemType.PRODUCT,
                            new BigDecimal("145.00"), "Extintor ecológico con agente limpio Solkaflam, ideal para centros de cómputo y equipos delicados."),
                    createOrGet("REC-PQS-10", "Servicio de Recarga y Mantenimiento PQS 10 Lbs", ItemType.SERVICE,
                            new BigDecimal("12.00"), "Inspección técnica, presurización con nitrógeno, cambio de sellos y certificación anual.")
            );

            int count = 0;
            for (CatalogItem item : seedItems) {
                String text = String.format("Product/Service: %s. SKU: %s. Type: %s. Price: %s %s. Description: %s",
                        item.getName(), item.getSkuCode(), item.getType(), item.getBasePrice(), item.getCurrency(),
                        item.getDescription() != null ? item.getDescription() : "");

                Map<String, Object> metadata = Map.of(
                        "sku", item.getSkuCode(),
                        "name", item.getName(),
                        "type", item.getType().name(),
                        "price", item.getBasePrice().doubleValue(),
                        "currency", item.getCurrency()
                );

                vectorizationService.saveVector(text, metadata);
                count++;
            }

            log.info("DataInitializer completed: Successfully vectorized {} baseline catalog items into pgvector VectorStore.", count);
        } else {
            log.info("Datos iniciales ya existen. Saltando inicialización.");
            System.out.println("Datos iniciales ya existen. Saltando inicialización.");
        }
    }

    private CatalogItem createOrGet(String sku, String name, ItemType type, BigDecimal price, String description) {
        return catalogItemRepository.findBySkuCode(sku).orElseGet(() -> {
            CatalogItem item = new CatalogItem(sku, name, type, price, "USD", "https://assets.extin.com/catalog/" + sku.toLowerCase() + ".png");
            item.setDescription(description);
            return catalogItemRepository.save(item);
        });
    }
}
