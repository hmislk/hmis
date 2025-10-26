package com.divudi.data;

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
 * To enable/disable, add/remove the following property in persistence.xml:
 * <property name="eclipselink.session.customizer"
 *           value="com.divudi.data.PreventDeleteSessionCustomizer"/>
 *
 * @author HMIS Development Team
 */
public class PreventDeleteSessionCustomizer implements SessionCustomizer {

    @Override
    public void customize(Session session) throws Exception {
        // Apply delete prevention to all entity descriptors
        for (ClassDescriptor descriptor : session.getDescriptors().values()) {
            descriptor.getEventManager().addListener(new DescriptorEventAdapter() {
                @Override
                public void preDelete(DescriptorEvent event) {
                    String entityName = event.getObject().getClass().getSimpleName();
                    throw new IllegalStateException(
                        "Delete operation not allowed for entity: " + entityName +
                        ". All delete operations are globally disabled."
                    );
                }
            });
        }
    }
}
