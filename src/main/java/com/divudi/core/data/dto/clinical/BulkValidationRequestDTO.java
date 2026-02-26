/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.clinical;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for bulk validation requests from AI agents
 * Used to validate multiple entities in a single API call
 *
 * @author Buddhika
 */
public class BulkValidationRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<EntityValidationDTO> entities;

    /**
     * Default constructor
     */
    public BulkValidationRequestDTO() {
    }

    // Getters and setters
    public List<EntityValidationDTO> getEntities() {
        return entities;
    }

    public void setEntities(List<EntityValidationDTO> entities) {
        this.entities = entities;
    }

    /**
     * Inner class for entity validation requests
     */
    public static class EntityValidationDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private String type;    // "VTM", "ATM", "VMP", "AMP", "UNIT", "CATEGORY"
        private String code;    // Optional entity code

        public EntityValidationDTO() {
        }

        public EntityValidationDTO(String name, String type) {
            this.name = name;
            this.type = type;
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

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return "EntityValidationDTO{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", code='" + code + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "BulkValidationRequestDTO{" +
                "entities=" + entities +
                '}';
    }
}