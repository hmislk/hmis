/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.clinical;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for validation results returned to AI agents
 * Contains validation status and suggestions for entity operations
 *
 * @author Buddhika
 */
public class ValidationResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<EntityValidationResultDTO> results;
    private int totalValidated;
    private int totalValid;
    private int totalInvalid;

    /**
     * Default constructor
     */
    public ValidationResultDTO() {
    }

    // Getters and setters
    public List<EntityValidationResultDTO> getResults() {
        return results;
    }

    public void setResults(List<EntityValidationResultDTO> results) {
        this.results = results;
    }

    public int getTotalValidated() {
        return totalValidated;
    }

    public void setTotalValidated(int totalValidated) {
        this.totalValidated = totalValidated;
    }

    public int getTotalValid() {
        return totalValid;
    }

    public void setTotalValid(int totalValid) {
        this.totalValid = totalValid;
    }

    public int getTotalInvalid() {
        return totalInvalid;
    }

    public void setTotalInvalid(int totalInvalid) {
        this.totalInvalid = totalInvalid;
    }

    /**
     * Inner class for individual entity validation results
     */
    public static class EntityValidationResultDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private String type;
        private boolean exists;
        private Long entityId;
        private String message;
        private List<String> suggestions;   // Alternative names/suggestions
        private boolean canCreate;          // Whether the entity can be auto-created

        public EntityValidationResultDTO() {
        }

        public EntityValidationResultDTO(String name, String type, boolean exists) {
            this.name = name;
            this.type = type;
            this.exists = exists;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isExists() {
            return exists;
        }

        public void setExists(boolean exists) {
            this.exists = exists;
        }

        public Long getEntityId() {
            return entityId;
        }

        public void setEntityId(Long entityId) {
            this.entityId = entityId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
        }

        public boolean isCanCreate() {
            return canCreate;
        }

        public void setCanCreate(boolean canCreate) {
            this.canCreate = canCreate;
        }

        @Override
        public String toString() {
            return "EntityValidationResultDTO{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", exists=" + exists +
                    ", entityId=" + entityId +
                    ", message='" + message + '\'' +
                    ", canCreate=" + canCreate +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ValidationResultDTO{" +
                "totalValidated=" + totalValidated +
                ", totalValid=" + totalValid +
                ", totalInvalid=" + totalInvalid +
                ", results=" + results +
                '}';
    }
}