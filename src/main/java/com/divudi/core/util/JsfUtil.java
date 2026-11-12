package com.divudi.core.util;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsfUtil {

    public static SelectItem[] getSelectItems(List<?> entities, boolean selectOne) {
        if (entities == null || entities.isEmpty()) {
            if (selectOne) {
                return new SelectItem[]{new SelectItem("", "---")};
            } else {
                return new SelectItem[0];
            }
        }

        int size = selectOne ? entities.size() + 1 : entities.size();
        SelectItem[] items = new SelectItem[size];
        int i = 0;
        if (selectOne) {
            items[0] = new SelectItem("", "---");
            i++;
        }
        for (Object x : entities) {
            items[i++] = new SelectItem(x, x.toString());
        }
        return items;
    }

    public static void addErrorMessages(List<String> messages) {
        for (String message : messages) {
            addErrorMessage(message);
        }
    }

    public static void addErrorMessage(String msg) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
        FacesContext.getCurrentInstance().addMessage(null, facesMsg);
    }

    /**
     * Message shown when a user without a started shift is redirected to the
     * cashier index page.
     */
    public static final String START_SHIFT_FIRST_MESSAGE
            = "Please start your shift before making payments or billing. Use Start Shift on this page.";

    /**
     * Adds an error message that survives a faces-redirect. Without
     * {@code Flash.setKeepMessages(true)} the message is dropped when the
     * browser follows the redirect.
     */
    public static void addErrorMessageForRedirect(String msg) {
        FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
        addErrorMessage(msg);
    }

    /**
     * Adds the "start your shift" message so that it is shown on the cashier
     * index page after the redirect.
     */
    public static void addStartShiftFirstMessageForRedirect() {
        addErrorMessageForRedirect(START_SHIFT_FIRST_MESSAGE);
    }

    public static void addSuccessMessage(String msg) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null);
        FacesContext.getCurrentInstance().addMessage(null, facesMsg);
    }

    public static void addInfoMessage(String string) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, string, null);
        FacesContext.getCurrentInstance().addMessage(null, facesMsg);
    }

    public static void addWarningMessage(String msg) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_WARN, msg, null);
        FacesContext.getCurrentInstance().addMessage(null, facesMsg);
    }

    public static String getRequestParameter(String key) {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(key);
    }

    public static Object getObjectFromRequestParameter(String requestParameterName, Converter converter, UIComponent component) {
        String theId = JsfUtil.getRequestParameter(requestParameterName);
        return converter.getAsObject(FacesContext.getCurrentInstance(), component, theId);
    }

    public static <T> List<T> setToList(Set<T> set) {
        return new ArrayList<T>(set);
    }

    public static String getAsConvertedString(Object object, Converter converter) {
        return converter.getAsString(FacesContext.getCurrentInstance(), null, object);
    }

    public static void addErrorMessage(Exception ex, String defaultMsg) {
        String msg = ex.getLocalizedMessage();
        if (msg != null && !msg.isEmpty()) {
            addErrorMessage(msg);
        } else {
            addErrorMessage(defaultMsg);
        }
    }

    public static enum PersistAction {
        CREATE,
        DELETE,
        UPDATE
    }

    /**
     * Parses what a user typed into an amount search filter. Commas are accepted
     * only as thousands separators, so "2,500.50" is valid and "12,5" is not.
     *
     * @return the number, or null when the text is not a plain finite amount
     */
    public static Double parseAmountFilter(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String text = rawValue.trim();
        if (!text.matches("-?(\\d{1,3}(,\\d{3})+|\\d+)(\\.\\d+)?")) {
            return null;
        }
        double value = Double.parseDouble(text.replace(",", ""));
        return Double.isInfinite(value) ? null : value;
    }

    /**
     * Builds the JPQL condition for a Net Total style search filter on a Double
     * column, and binds its parameter as a number. Binding "%value%" to an
     * equality on a Double column makes EclipseLink throw a ConversionException.
     * <p>
     * Matches within half a cent because stored totals carry float noise. An amount
     * typed without a sign matches either sign, because return and cancellation
     * bills store negative totals while users type the positive amount. An amount
     * typed with a minus matches only that negative amount.
     * <p>
     * When the text is not a number this shows an error and returns a condition
     * matching nothing, so the search never lists bills that ignore the filter.
     *
     * @param fieldPath JPQL path of the column, e.g. "b.netTotal"
     * @param paramName name of the JPQL parameter to bind
     * @param rawValue what the user typed
     * @param label name of the filter, used in the error message
     * @param params query parameters, receives the numeric parameter
     * @return the JPQL fragment to append to the query
     */
    public static String exactAmountFilter(String fieldPath, String paramName, String rawValue, String label, Map params) {
        Double value = parseAmountFilter(rawValue);
        if (value == null) {
            if (FacesContext.getCurrentInstance() != null) {
                addErrorMessage("Invalid " + label + " value. Please enter a valid number.");
            }
            return " and 1=0 ";
        }
        params.put(paramName, value);
        if (value < 0) {
            return " and abs(" + fieldPath + " - :" + paramName + ") < 0.005 ";
        }
        return " and abs(abs(" + fieldPath + ") - :" + paramName + ") < 0.005 ";
    }

    public static boolean isValidationPassed() {
        return !FacesContext.getCurrentInstance().isValidationFailed();
    }
}
