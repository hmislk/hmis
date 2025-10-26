package com.divudi.data;

/**
 * Exception thrown when an attempt is made to delete an entity in violation
 * of the global delete prevention policy.
 *
 * This exception indicates a business rule enforcement rather than a programming error.
 * The HMIS system uses a soft-delete pattern where entities should be marked as retired
 * rather than permanently deleted from the database.
 *
 * @author HMIS Development Team
 */
public class EntityDeleteNotAllowedException extends RuntimeException {

    private final String entityName;
    private final Object entityId;

    /**
     * Constructs a new exception with entity details.
     *
     * @param entityName the simple name of the entity class
     * @param entityId the ID of the entity being deleted (may be null)
     * @param message the detail message
     */
    public EntityDeleteNotAllowedException(String entityName, Object entityId, String message) {
        super(message);
        this.entityName = entityName;
        this.entityId = entityId;
    }

    /**
     * Constructs a new exception with entity details and a cause.
     *
     * @param entityName the simple name of the entity class
     * @param entityId the ID of the entity being deleted (may be null)
     * @param message the detail message
     * @param cause the underlying cause
     */
    public EntityDeleteNotAllowedException(String entityName, Object entityId, String message, Throwable cause) {
        super(message, cause);
        this.entityName = entityName;
        this.entityId = entityId;
    }

    /**
     * Gets the name of the entity that deletion was attempted on.
     *
     * @return the entity class simple name
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * Gets the ID of the entity that deletion was attempted on.
     *
     * @return the entity ID, or null if not available
     */
    public Object getEntityId() {
        return entityId;
    }
}
