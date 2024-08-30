package com.divudi.bean.common;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.inject.Inject;

@FacesConverter("longDateConverter")
public class LongDateConverter implements Converter {

    @Inject 
    SessionController sessionController;
    
    // Moved DATE_FORMAT to be dynamically loaded
    private String dateFormat;
    
    // Load date format from SessionController
    private void loadLongDateFormat() {
        if (sessionController != null && sessionController.getApplicationPreference() != null) {
            dateFormat = sessionController.getApplicationPreference().getLongDateFormat();
        } else {
            // Fallback to default if sessionController or preferences are null
            dateFormat = "dd/MM/yyyy";  // Default format
        }
    }
    
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        loadLongDateFormat();  // Load the format before parsing
        
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            return sdf.parse(value);
        } catch (Exception e) {
            throw new RuntimeException("Date parsing error", e);
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        loadLongDateFormat();  // Load the format before formatting
        
        if (value == null) {
            return "";
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            return sdf.format((Date) value);
        } catch (Exception e) {
            throw new RuntimeException("Date formatting error", e);
        }
    }
}
