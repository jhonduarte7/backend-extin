package com.extin.backend.mappers;

import com.extin.backend.models.dtos.CatalogItemDto;
import com.extin.backend.models.entities.CatalogItem;
import org.springframework.stereotype.Component;

/**
 * Mapper responsible for transforming between CatalogItem entities and CatalogItemDto records.
 */
@Component
public class CatalogItemMapper {

    /**
     * Converts a CatalogItem entity to CatalogItemDto record.
     *
     * @param item CatalogItem entity.
     * @return CatalogItemDto record or null if entity is null.
     */
    public CatalogItemDto toDto(CatalogItem item) {
        if (item == null) {
            return null;
        }

        return new CatalogItemDto(
                item.getId(),
                item.getSkuCode(),
                item.getName(),
                item.getType(),
                item.getBasePrice(),
                item.getCurrency(),
                item.getImageUrl(),
                item.getDescription(),
                item.getActive()
        );
    }

    /**
     * Converts a CatalogItemDto record to CatalogItem entity.
     *
     * @param dto CatalogItemDto record.
     * @return CatalogItem entity or null if dto is null.
     */
    public CatalogItem toEntity(CatalogItemDto dto) {
        if (dto == null) {
            return null;
        }

        CatalogItem item = new CatalogItem();
        item.setId(dto.id());
        item.setSkuCode(dto.skuCode());
        item.setName(dto.name());
        item.setType(dto.type());
        item.setBasePrice(dto.basePrice());
        if (dto.currency() != null) {
            item.setCurrency(dto.currency());
        }
        item.setImageUrl(dto.imageUrl());
        item.setDescription(dto.description());
        if (dto.active() != null) {
            item.setActive(dto.active());
        }
        return item;
    }
}
