package com.divudi.core.converter;

import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.facade.RoomCategoryFacade;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Named;

@Named
@ApplicationScoped
public class RoomCategoryConverter implements Converter<RoomCategory> {

    @EJB
    private RoomCategoryFacade roomCategoryFacade;

    @Override
    public String getAsString(FacesContext context, UIComponent component, RoomCategory cat) {
        if (cat == null || cat.getId() == null) {
            return "";
        }
        return String.valueOf(cat.getId());
    }

    @Override
    public RoomCategory getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            Long id = Long.parseLong(value.trim());
            return roomCategoryFacade.find(id);
        } catch (Exception e) {
            return null;
        }
    }
}