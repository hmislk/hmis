package com.divudi.core.converter;

import com.divudi.core.data.dto.search.ItemDTO;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Named;

/**
 * CDI converter for ItemDTO used in PrimeFaces autocomplete components.
 * Encodes the DTO as "id:typeName:dblValue:rateItemId" — no DB access needed.
 */
@Named
@ApplicationScoped
public class ItemDtoConverter implements Converter<ItemDTO> {

    @Override
    public String getAsString(FacesContext context, UIComponent component, ItemDTO dto) {
        if (dto == null || dto.getId() == null) {
            return "";
        }
        return dto.getId()
                + ":" + (dto.getItemTypeName() != null ? dto.getItemTypeName() : "")
                + ":" + (dto.getDblValue() != null ? dto.getDblValue() : 0.0)
                + ":" + (dto.getRateItemId() != null ? dto.getRateItemId() : dto.getId());
    }

    @Override
    public ItemDTO getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            String[] parts = value.split(":");
            if (parts.length < 4) {
                return null;
            }
            Long id = Long.parseLong(parts[0]);
            String typeName = parts[1];
            Double dblValue = Double.parseDouble(parts[2]);
            Long rateItemId = Long.parseLong(parts[3]);
            return new ItemDTO(id, null, null, dblValue, typeName, rateItemId);
        } catch (Exception e) {
            return null;
        }
    }
}
