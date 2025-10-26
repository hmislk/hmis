package com.divudi.data;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.descriptors.DescriptorEventAdapter;
import org.eclipse.persistence.sessions.Session;

/**
 * EclipseLink SessionCustomizer that prevents all DELETE operations globally.
 *
 * This customizer registers a preDelete event listener on all entities to block
 * any delete operations including:
 * - Direct EntityManager.remove() calls
 * - Cascade deletes
 * - Orphan removals
 *
 * All attempted delete operations are logged with comprehensive audit information
 * including entity details, ID, and stack trace for troubleshooting.
 *
 * HMIS uses a soft-delete pattern: entities should be marked as retired=true
 * rather than permanently deleted from the database.
 *
 * To enable/disable, add/remove the following property in persistence.xml:
 * <property name="eclipselink.session.customizer"
 *           value="com.divudi.data.PreventDeleteSessionCustomizer"/>
 *
 * @author HMIS Development Team
 */
public class PreventDeleteSessionCustomizer implements SessionCustomizer {

    private static final Logger LOGGER = Logger.getLogger(PreventDeleteSessionCustomizer.class.getName());

    @Override
    public void customize(Session session) throws Exception {
        LOGGER.info("Initializing PreventDeleteSessionCustomizer - all delete operations will be blocked");

        // Apply delete prevention to all entity descriptors
        for (ClassDescriptor descriptor : session.getDescriptors().values()) {
            descriptor.getEventManager().addListener(new DescriptorEventAdapter() {
                @Override
                public void preDelete(DescriptorEvent event) {
                    Object entity = event.getObject();
                    String entityName = entity.getClass().getSimpleName();
                    String entityFullName = entity.getClass().getName();
                    Object entityId = extractEntityId(entity);

                    // Log comprehensive audit information
                    String logMessage = String.format(
                        "BLOCKED DELETE ATTEMPT - Entity: %s, ID: %s, Full Class: %s",
                        entityName,
                        entityId != null ? entityId.toString() : "unknown",
                        entityFullName
                    );

                    // Log at WARNING level with stack trace for troubleshooting
                    LOGGER.log(Level.WARNING, logMessage, new Exception("Stack trace for delete attempt"));

                    // Construct developer-friendly error message
                    String errorMessage = String.format(
                        "Delete operation not allowed for entity '%s' (ID: %s). " +
                        "HMIS uses a soft-delete pattern - please set the 'retired' property to true instead of deleting. " +
                        "Example: entity.setRetired(true); facade.edit(entity);",
                        entityName,
                        entityId != null ? entityId : "unknown"
                    );

                    throw new EntityDeleteNotAllowedException(entityName, entityId, errorMessage);
                }
            });
        }

        LOGGER.info("PreventDeleteSessionCustomizer initialized successfully for " +
                    session.getDescriptors().size() + " entity descriptors");
    }

    /**
     * Attempts to extract the entity ID using reflection.
     * Tries common ID getter methods: getId(), id property.
     *
     * @param entity the entity object
     * @return the entity ID, or null if not found
     */
    private Object extractEntityId(Object entity) {
        if (entity == null) {
            return null;
        }

        try {
            // Try getId() method
            Method getIdMethod = entity.getClass().getMethod("getId");
            return getIdMethod.invoke(entity);
        } catch (Exception e) {
            // getId() not available or failed, return null
            return null;
        }
    }
}
