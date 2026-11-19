package com.divudi.core.converter;

import com.divudi.core.data.Privileges;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Stores a {@link Privileges} value by its name, as {@code @Enumerated(EnumType.STRING)} did.
 * <p>
 * A privilege row whose name this build does not know (for example a row added by a newer
 * build, then the code was rolled back) is read as {@code null} with a WARNING, instead of
 * failing the whole query. Without this, one unknown row made the user's privilege list
 * load empty, so every menu was hidden.
 */
@Converter
public class PrivilegesConverter implements AttributeConverter<Privileges, String> {

    private static final Logger LOGGER = Logger.getLogger(PrivilegesConverter.class.getName());

    @Override
    public String convertToDatabaseColumn(Privileges attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Privileges convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return Privileges.valueOf(dbData.trim());
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Unknown privilege name in the database, ignored: {0}", dbData);
            return null;
        }
    }
}
