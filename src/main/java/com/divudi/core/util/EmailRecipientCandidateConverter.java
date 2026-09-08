package com.divudi.core.util;

import com.divudi.core.data.EmailRecipientCandidate;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * Round-trips an EmailRecipientCandidate through a p:autoComplete chip's
 * string form. The email address alone is the string identity - label and
 * sourceType are cosmetic and only matter while the candidate is still an
 * in-memory search result, not after a postback.
 *
 * Usage in XHTML:
 * <p:autoComplete ... converter="emailRecipientCandidateConverter" />
 */
@FacesConverter("emailRecipientCandidateConverter")
public class EmailRecipientCandidateConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new EmailRecipientCandidate(value, value, null);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof EmailRecipientCandidate) {
            return ((EmailRecipientCandidate) value).getEmail();
        }
        return value.toString();
    }
}
