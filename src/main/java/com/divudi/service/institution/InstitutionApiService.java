/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.institution;

import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.dto.institution.InstitutionCreateRequestDTO;
import com.divudi.core.data.dto.institution.InstitutionRelationshipUpdateDTO;
import com.divudi.core.data.dto.institution.InstitutionResponseDTO;
import com.divudi.core.data.dto.institution.InstitutionUpdateRequestDTO;
import com.divudi.core.data.dto.search.InstitutionDTO;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.util.CommonFunctions;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Institution API operations
 * Provides business logic for institution management including create, update, retire, and search
 * Follows patterns from PharmacySearchApiService and PharmacyAdjustmentApiService
 *
 * @author Buddhika
 */
@Stateless
public class InstitutionApiService implements Serializable {

    @EJB
    private InstitutionFacade institutionFacade;

    /**
     * Search institutions by name or code with optional type filtering
     * Uses DTO constructor queries for optimal performance
     */
    public List<InstitutionDTO> searchInstitutions(String query, InstitutionType type, Integer limit) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            throw new Exception("Institution search query is required");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toUpperCase() + "%");

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT new com.divudi.core.data.dto.search.InstitutionDTO(")
            .append("i.id, i.name, i.code, i.institutionType, i.address, i.phone, i.email) ")
            .append("FROM Institution i ")
            .append("WHERE i.retired = false ")
            .append("AND (upper(i.name) LIKE :query OR upper(i.code) LIKE :query) ");

        // Add type filter if provided
        if (type != null) {
            jpql.append("AND i.institutionType = :type ");
            params.put("type", type);
        }

        jpql.append("ORDER BY i.name");

        int resultLimit = (limit != null && limit > 0 && limit <= 100) ? limit : 20;

        @SuppressWarnings("unchecked")
        List<InstitutionDTO> results = (List<InstitutionDTO>) institutionFacade.findLightsByJpql(
            jpql.toString(), params, TemporalType.TIMESTAMP, resultLimit);

        return results;
    }

    /**
     * Find institution by ID and return detailed response
     */
    public InstitutionResponseDTO findInstitutionById(Long id) throws Exception {
        if (id == null) {
            throw new Exception("Institution ID is required");
        }

        Institution institution = institutionFacade.find(id);
        if (institution == null) {
            throw new Exception("Institution not found with ID: " + id);
        }

        return buildInstitutionResponseDTO(institution, "Institution found successfully");
    }

    /**
     * Create new institution
     * Sets creater and createdAt fields
     */
    public InstitutionResponseDTO createInstitution(InstitutionCreateRequestDTO request, WebUser user) throws Exception {
        validateCreateRequest(request);

        if (user == null) {
            throw new Exception("User is required for creating institution");
        }

        // Create new institution entity
        Institution institution = new Institution();
        institution.setName(request.getName());

        // Generate code if not provided
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            institution.setCode(request.getCode());
        } else {
            institution.setCode(CommonFunctions.nameToCode(request.getName()));
        }

        institution.setInstitutionType(request.getInstitutionType());
        institution.setAddress(request.getAddress());
        institution.setPhone(request.getPhone());
        institution.setMobile(request.getMobile());
        institution.setEmail(request.getEmail());
        institution.setFax(request.getFax());
        institution.setWeb(request.getWeb());
        institution.setContactPersonName(request.getContactPersonName());
        institution.setOwnerName(request.getOwnerName());
        institution.setOwnerEmail(request.getOwnerEmail());

        // Set parent institution if provided
        if (request.getParentInstitutionId() != null) {
            Institution parentInstitution = loadAndValidateInstitution(request.getParentInstitutionId());
            institution.setInstitution(parentInstitution);
        }

        // Set audit fields
        institution.setCreater(user);
        institution.setCreatedAt(Calendar.getInstance().getTime());
        institution.setRetired(false);

        // Save institution
        institutionFacade.create(institution);

        return buildInstitutionResponseDTO(institution, "Institution created successfully");
    }

    /**
     * Update existing institution
     * Only updates fields that are provided (not null)
     */
    public InstitutionResponseDTO updateInstitution(Long id, InstitutionUpdateRequestDTO request, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Institution ID is required");
        }

        if (request == null || !request.isValid()) {
            throw new Exception("Valid update request is required");
        }

        if (user == null) {
            throw new Exception("User is required for updating institution");
        }

        Institution institution = loadAndValidateInstitution(id);

        // Update fields only if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            institution.setName(request.getName());
        }

        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            institution.setCode(request.getCode());
        }

        if (request.getInstitutionType() != null) {
            institution.setInstitutionType(request.getInstitutionType());
        }

        if (request.getAddress() != null) {
            institution.setAddress(request.getAddress());
        }

        if (request.getPhone() != null) {
            institution.setPhone(request.getPhone());
        }

        if (request.getMobile() != null) {
            institution.setMobile(request.getMobile());
        }

        if (request.getEmail() != null) {
            institution.setEmail(request.getEmail());
        }

        if (request.getFax() != null) {
            institution.setFax(request.getFax());
        }

        if (request.getWeb() != null) {
            institution.setWeb(request.getWeb());
        }

        if (request.getContactPersonName() != null) {
            institution.setContactPersonName(request.getContactPersonName());
        }

        if (request.getOwnerName() != null) {
            institution.setOwnerName(request.getOwnerName());
        }

        if (request.getOwnerEmail() != null) {
            institution.setOwnerEmail(request.getOwnerEmail());
        }

        // Save updated institution
        institutionFacade.edit(institution);

        return buildInstitutionResponseDTO(institution, "Institution updated successfully");
    }

    /**
     * Retire institution
     * Sets retired flag and retirer/retiredAt fields
     */
    public InstitutionResponseDTO retireInstitution(Long id, String retireComments, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Institution ID is required");
        }

        if (user == null) {
            throw new Exception("User is required for retiring institution");
        }

        Institution institution = loadAndValidateInstitution(id);

        if (institution.isRetired()) {
            throw new Exception("Institution is already retired");
        }

        // Set retire fields
        institution.setRetired(true);
        institution.setRetirer(user);
        institution.setRetiredAt(Calendar.getInstance().getTime());
        institution.setRetireComments(retireComments);

        // Save retired institution
        institutionFacade.edit(institution);

        return buildInstitutionResponseDTO(institution, "Institution retired successfully");
    }

    /**
     * Change parent institution relationship
     * Can set new parent or remove parent relationship (set to null)
     */
    public InstitutionResponseDTO changeParentInstitution(InstitutionRelationshipUpdateDTO request, WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid relationship update request is required");
        }

        if (user == null) {
            throw new Exception("User is required for updating institution relationship");
        }

        Institution institution = loadAndValidateInstitution(request.getInstitutionId());

        // Set new parent or remove parent relationship
        if (request.getParentInstitutionId() != null) {
            Institution parentInstitution = loadAndValidateInstitution(request.getParentInstitutionId());

            // Prevent circular reference
            if (parentInstitution.getId().equals(institution.getId())) {
                throw new Exception("Institution cannot be its own parent");
            }

            institution.setInstitution(parentInstitution);
        } else {
            institution.setInstitution(null);
        }

        // Save updated institution
        institutionFacade.edit(institution);

        return buildInstitutionResponseDTO(institution, "Institution parent relationship updated successfully");
    }

    // Private helper methods

    /**
     * Validate create request
     */
    private void validateCreateRequest(InstitutionCreateRequestDTO request) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid create request is required");
        }

        // Check for duplicate name
        if (isDuplicateName(request.getName(), null)) {
            throw new Exception("Institution with name '" + request.getName() + "' already exists");
        }

        // Check for duplicate code if provided
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            if (isDuplicateCode(request.getCode(), null)) {
                throw new Exception("Institution with code '" + request.getCode() + "' already exists");
            }
        }
    }

    /**
     * Load and validate institution
     */
    private Institution loadAndValidateInstitution(Long institutionId) throws Exception {
        Institution institution = institutionFacade.find(institutionId);
        if (institution == null) {
            throw new Exception("Institution not found with ID: " + institutionId);
        }
        if (institution.isRetired()) {
            throw new Exception("Institution is retired");
        }
        return institution;
    }

    /**
     * Check for duplicate institution name
     */
    private boolean isDuplicateName(String name, Long excludeId) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name.trim());

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT COUNT(i) FROM Institution i ")
            .append("WHERE i.retired = false ")
            .append("AND i.name = :name ");

        if (excludeId != null) {
            jpql.append("AND i.id != :excludeId ");
            params.put("excludeId", excludeId);
        }

        Long count = institutionFacade.countByJpql(jpql.toString(), params);
        return count != null && count > 0;
    }

    /**
     * Check for duplicate institution code
     */
    private boolean isDuplicateCode(String code, Long excludeId) {
        Map<String, Object> params = new HashMap<>();
        params.put("code", code.trim());

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT COUNT(i) FROM Institution i ")
            .append("WHERE i.retired = false ")
            .append("AND i.code = :code ");

        if (excludeId != null) {
            jpql.append("AND i.id != :excludeId ");
            params.put("excludeId", excludeId);
        }

        Long count = institutionFacade.countByJpql(jpql.toString(), params);
        return count != null && count > 0;
    }

    /**
     * Build institution response DTO from entity
     */
    private InstitutionResponseDTO buildInstitutionResponseDTO(Institution institution, String message) {
        InstitutionResponseDTO response = new InstitutionResponseDTO();
        response.setId(institution.getId());
        response.setName(institution.getName());
        response.setCode(institution.getCode());
        response.setInstitutionType(institution.getInstitutionType());
        response.setAddress(institution.getAddress());
        response.setPhone(institution.getPhone());
        response.setMobile(institution.getMobile());
        response.setEmail(institution.getEmail());
        response.setFax(institution.getFax());
        response.setWeb(institution.getWeb());
        response.setContactPersonName(institution.getContactPersonName());
        response.setOwnerName(institution.getOwnerName());
        response.setOwnerEmail(institution.getOwnerEmail());
        response.setActive(!institution.isRetired());
        response.setCreatedAt(institution.getCreatedAt());

        // Set parent institution details if exists
        if (institution.getInstitution() != null) {
            response.setParentInstitutionId(institution.getInstitution().getId());
            response.setParentInstitutionName(institution.getInstitution().getName());
        }

        response.setMessage(message);
        return response;
    }
}
