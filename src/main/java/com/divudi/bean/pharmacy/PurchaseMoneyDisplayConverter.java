package com.divudi.bean.pharmacy;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.util.Locale;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

/**
 * Shows a money field with two decimals while the value behind it keeps its full
 * precision, and reads what the user types back as an exact BigDecimal (issue #22864).
 * Used on the purchase rate and purchase value inputs only when the option
 * "Allow Entering Total Purchase Value to Calculate the Purchase Rate in Direct Purchase and GRN Costing"
 * is on.
 */
public class PurchaseMoneyDisplayConverter implements Converter<BigDecimal>, Serializable {

    private static final long serialVersionUID = 1L;
    private static final String PATTERN = "#,##0.00";

    private static DecimalFormat newFormat() {
        DecimalFormat df = new DecimalFormat(PATTERN, DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        df.setParseBigDecimal(true);
        return df;
    }

    @Override
    public BigDecimal getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        Object current = component instanceof ValueHolder ? ((ValueHolder) component).getValue() : null;
        return parse(value.trim(), current);
    }

    /**
     * Leaving a box untouched sends back its 2-decimal text. If that is what the current value
     * shows, keep the current value so extra decimals (e.g. 12.3456) are not rounded away.
     */
    static BigDecimal parse(String text, Object current) {
        if (current instanceof BigDecimal && text.equals(format((BigDecimal) current))) {
            return (BigDecimal) current;
        }
        ParsePosition position = new ParsePosition(0);
        Number parsed = newFormat().parse(text, position);
        if (parsed == null || position.getIndex() != text.length()) {
            throw new ConverterException(new javax.faces.application.FacesMessage(
                    javax.faces.application.FacesMessage.SEVERITY_ERROR, "Please enter a valid number", null));
        }
        return (BigDecimal) parsed;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, BigDecimal value) {
        if (value == null) {
            return "";
        }
        return format(value);
    }

    private static String format(BigDecimal value) {
        return newFormat().format(value);
    }
}
