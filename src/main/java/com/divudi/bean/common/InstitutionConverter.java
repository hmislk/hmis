/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 */
package com.divudi.bean.common;

import com.divudi.core.entity.Institution;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * JSF converter for Institution entities, enabling safe use in select
 * components (p:selectOneMenu, p:selectOneListbox).
 *
 * @author Dr M H B Ariyaratne
 */
@FacesConverter(value = "institutionConverter")
public class InstitutionConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            Long id = Long.valueOf(value.trim());
            InstitutionController controller = (InstitutionController) context.getApplication()
                    .getELResolver().getValue(context.getELContext(), null, "institutionController");
            return controller.getEjbFacade().find(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Institution) {
            Institution institution = (Institution) value;
            return institution.getId() == null ? "" : institution.getId().toString();
        }
        return "";
    }
}
