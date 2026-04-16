/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.institution;

import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.dto.search.SiteDTO;
import com.divudi.core.data.dto.site.SiteCreateRequestDTO;
import com.divudi.core.data.dto.site.SiteResponseDTO;
import com.divudi.core.data.dto.site.SiteUpdateRequestDTO;
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
 * Service for Site API operations
 * Site is an Institution with institutionType = InstitutionType.Site
 * Provides business logic for site management including create, update, retire, and search
 * Follows patterns from PharmacySearchApiService and PharmacyAdjustmentApiService
 *
 * @author Buddhika
 */
@Stateless
public class SiteApiService implements Serializable {

    @EJB
    private InstitutionFacade institutionFacade;

    /**
     * Search sites by name or code
     * Uses DTO constructor queries for optimal performance
     * Only returns institutions with type Site
     */
    public List<SiteDTO> searchSites(String query, Integer limit) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            throw new Exception("Site search query is required");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toUpperCase() + "%");
        params.put("type", InstitutionType.Site);

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT new com.divudi.core.data.dto.search.SiteDTO(")
            .append("i.id, i.name, i.code, i.address) ")
            .append("FROM Institution i ")
            .append("WHERE i.retired = false ")
            .append("AND i.institutionType = :type ")
            .append("AND (upper(i.name) LIKE :query OR upper(i.code) LIKE :query) ")
            .append("ORDER BY i.name");

        int resultLimit = (limit != null && limit > 0 && limit <= 100) ? limit : 20;

        @SuppressWarnings("unchecked")
        List<SiteDTO> results = (List<SiteDTO>) institutionFacade.findLightsByJpql(
            jpql.toString(), params, TemporalType.TIMESTAMP, resultLimit);

        return results;
    }

    /**
     * Find site by ID and return detailed response
     */
    public SiteResponseDTO findSiteById(Long id) throws Exception {
        if (id == null) {
            throw new Exception("Site ID is required");
        }

        Institution site = institutionFacade.find(id);
        if (site == null) {
            throw new Exception("Site not found with ID: " + id);
        }

        if (site.getInstitutionType() != InstitutionType.Site) {
            throw new Exception("Institution is not a site");
        }

        return buildSiteResponseDTO(site, "Site found successfully");
    }

    /**
     * Create new site
     * Site is an Institution with institutionType = InstitutionType.Site
     * Sets creater and createdAt fields
     */
    public SiteResponseDTO createSite(SiteCreateRequestDTO request, WebUser user) throws Exception {
        validateCreateRequest(request);

        if (user == null) {
            throw new Exception("User is required for creating site");
        }

        // Create new institution entity with type Site
        Institution site = new Institution();
        site.setName(request.getName());

        // Generate code if not provided
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            site.setCode(request.getCode());
        } else {
            site.setCode(CommonFunctions.nameToCode(request.getName()));
        }

        // Set institution type to Site
        site.setInstitutionType(InstitutionType.Site);

        site.setAddress(request.getAddress());
        site.setPhone(request.getPhone());
        site.setMobile(request.getMobile());
        site.setEmail(request.getEmail());
        site.setFax(request.getFax());

        // Set parent institution if provided
        if (request.getParentInstitutionId() != null) {
            Institution parentInstitution = loadAndValidateInstitution(request.getParentInstitutionId());
            site.setInstitution(parentInstitution);
        }

        // Set audit fields
        site.setCreater(user);
        site.setCreatedAt(Calendar.getInstance().getTime());
        site.setRetired(false);

        // Save site
        institutionFacade.create(site);

        return buildSiteResponseDTO(site, "Site created successfully");
    }

    /**
     * Update existing site
     * Only updates fields that are provided (not null)
     */
    public SiteResponseDTO updateSite(Long id, SiteUpdateRequestDTO request, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Site ID is required");
        }

        if (request == null || !request.isValid()) {
            throw new Exception("Valid update request is required");
        }

        if (user == null) {
            throw new Exception("User is required for updating site");
        }

        Institution site = loadAndValidateSite(id);

        // Update fields only if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            site.setName(request.getName());
        }

        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            site.setCode(request.getCode());
        }

        if (request.getAddress() != null) {
            site.setAddress(request.getAddress());
        }

        if (request.getPhone() != null) {
            site.setPhone(request.getPhone());
        }

        if (request.getMobile() != null) {
            site.setMobile(request.getMobile());
        }

        if (request.getEmail() != null) {
            site.setEmail(request.getEmail());
        }

        if (request.getFax() != null) {
            site.setFax(request.getFax());
        }

        // Save updated site
        institutionFacade.edit(site);

        return buildSiteResponseDTO(site, "Site updated successfully");
    }

    /**
     * Retire site
     * Sets retired flag and retirer/retiredAt fields
     */
    public SiteResponseDTO retireSite(Long id, String retireComments, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Site ID is required");
        }

        if (user == null) {
            throw new Exception("User is required for retiring site");
        }

        Institution site = loadAndValidateSite(id);

        if (site.isRetired()) {
            throw new Exception("Site is already retired");
        }

        // Set retire fields
        site.setRetired(true);
        site.setRetirer(user);
        site.setRetiredAt(Calendar.getInstance().getTime());
        site.setRetireComments(retireComments);

        // Save retired site
        institutionFacade.edit(site);

        return buildSiteResponseDTO(site, "Site retired successfully");
    }

    // Private helper methods

    /**
     * Validate create request
     */
    private void validateCreateRequest(SiteCreateRequestDTO request) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid create request is required");
        }

        // Check for duplicate name among sites
        if (isDuplicateName(request.getName(), null)) {
            throw new Exception("Site with name '" + request.getName() + "' already exists");
        }

        // Check for duplicate code if provided
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            if (isDuplicateCode(request.getCode(), null)) {
                throw new Exception("Site with code '" + request.getCode() + "' already exists");
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
     * Load and validate site (must be Institution with type Site)
     */
    private Institution loadAndValidateSite(Long siteId) throws Exception {
        Institution site = institutionFacade.find(siteId);
        if (site == null) {
            throw new Exception("Site not found with ID: " + siteId);
        }
        if (site.getInstitutionType() != InstitutionType.Site) {
            throw new Exception("Institution is not a site");
        }
        if (site.isRetired()) {
            throw new Exception("Site is retired");
        }
        return site;
    }

    /**
     * Check for duplicate site name
     */
    private boolean isDuplicateName(String name, Long excludeId) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name.trim());
        params.put("type", InstitutionType.Site);

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT COUNT(i) FROM Institution i ")
            .append("WHERE i.retired = false ")
            .append("AND i.institutionType = :type ")
            .append("AND i.name = :name ");

        if (excludeId != null) {
            jpql.append("AND i.id != :excludeId ");
            params.put("excludeId", excludeId);
        }

        Long count = institutionFacade.countByJpql(jpql.toString(), params);
        return count != null && count > 0;
    }

    /**
     * Check for duplicate site code
     */
    private boolean isDuplicateCode(String code, Long excludeId) {
        Map<String, Object> params = new HashMap<>();
        params.put("code", code.trim());
        params.put("type", InstitutionType.Site);

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT COUNT(i) FROM Institution i ")
            .append("WHERE i.retired = false ")
            .append("AND i.institutionType = :type ")
            .append("AND i.code = :code ");

        if (excludeId != null) {
            jpql.append("AND i.id != :excludeId ");
            params.put("excludeId", excludeId);
        }

        Long count = institutionFacade.countByJpql(jpql.toString(), params);
        return count != null && count > 0;
    }

    /**
     * Build site response DTO from entity
     */
    private SiteResponseDTO buildSiteResponseDTO(Institution site, String message) {
        SiteResponseDTO response = new SiteResponseDTO();
        response.setId(site.getId());
        response.setName(site.getName());
        response.setCode(site.getCode());
        response.setAddress(site.getAddress());
        response.setPhone(site.getPhone());
        response.setMobile(site.getMobile());
        response.setEmail(site.getEmail());
        response.setFax(site.getFax());
        response.setActive(!site.isRetired());
        response.setCreatedAt(site.getCreatedAt());

        // Set parent institution details if exists
        if (site.getInstitution() != null) {
            response.setParentInstitutionId(site.getInstitution().getId());
            response.setParentInstitutionName(site.getInstitution().getName());
        }

        response.setMessage(message);
        return response;
    }
}
