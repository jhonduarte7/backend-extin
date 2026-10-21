package com.extin.backend.config;

import com.extin.backend.models.entities.CatalogItem;
import com.extin.backend.models.enums.ItemType;
import com.extin.backend.repositories.CatalogItemRepository;
import com.extin.backend.services.VectorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying {@link DataInitializer} startup behavior and vectorization conditional execution.
 */
@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private CatalogItemRepository catalogItemRepository;

    @Mock
    private VectorizationService vectorizationService;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    @DisplayName("run - Skips initialization and vectorization when database already contains catalog items")
    void testRunSkipsWhenCatalogNotEmpty() {
        when(catalogItemRepository.count()).thenReturn(20L);

        dataInitializer.run();

        verify(catalogItemRepository).count();
        verifyNoInteractions(vectorizationService);
        verify(catalogItemRepository, never()).findBySkuCode(anyString());
        verify(catalogItemRepository, never()).save(any(CatalogItem.class));
    }

    @Test
    @DisplayName("run - Executes initialization and vectorization when database catalog is empty")
    void testRunExecutesWhenCatalogEmpty() {
        when(catalogItemRepository.count()).thenReturn(0L);
        when(catalogItemRepository.findBySkuCode(anyString())).thenAnswer(invocation -> {
            String sku = invocation.getArgument(0);
            return Optional.of(new CatalogItem(sku, "Item " + sku, ItemType.PRODUCT, BigDecimal.TEN, "USD", null));
        });

        dataInitializer.run();

        verify(catalogItemRepository).count();
        verify(vectorizationService, times(4)).saveVector(anyString(), anyMap());
    }
}
