/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.institution;

import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.department.DepartmentCreateRequestDTO;
import com.divudi.core.data.dto.department.DepartmentRelationshipUpdateDTO;
import com.divudi.core.data.dto.department.DepartmentResponseDTO;
import com.divudi.core.data.dto.department.DepartmentUpdateRequestDTO;
import com.divudi.core.data.dto.search.DepartmentDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.DepartmentFacade;
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
 * Service for Department API operations
 * Provides business logic for department management including create, update, retire, and search
 * Follows patterns from PharmacySearchApiService and PharmacyAdjustmentApiService
 *
 * @author Buddhika
 */
@Stateless
public class DepartmentApiService implements Serializable {

    @EJB
    private DepartmentFacade departmentFacade;

    @EJB
    private InstitutionFacade institutionFacade;

    /**
     * Search departments by name or code with optional type and institution filtering
     * Uses DTO constructor queries for optimal performance
     */
    public List<DepartmentDTO> searchDepartments(String query, DepartmentType type, Long institutionId, Integer limit) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            throw new Exception("Department search query is required");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toUpperCase() + "%");

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT new com.divudi.core.data.dto.search.DepartmentDTO(")
            .append("d.id, d.name, d.code) ")
            .append("FROM Department d ")
            .append("WHERE d.retired = false ")
            .append("AND (upper(d.name) LIKE :query OR upper(d.code) LIKE :query) ");

        // Add type filter if provided
        if (type != null) {
            jpql.append("AND d.departmentType = :type ");
            params.put("type", type);
        }

        // Add institution filter if provided
        if (institutionId != null) {
            jpql.append("AND d.institution.id = :institutionId ");
            params.put("institutionId", institutionId);
        }

        jpql.append("ORDER BY d.name");

        int resultLimit = (limit != null && limit > 0 && limit <= 100) ? limit : 20;

        @SuppressWarnings("unchecked")
        List<DepartmentDTO> results = (List<DepartmentDTO>) departmentFacade.findLightsByJpql(
            jpql.toString(), params, TemporalType.TIMESTAMP, resultLimit);

        return results;
    }

    /**
     * Find department by ID and return detailed response
     */
    public DepartmentResponseDTO findDepartmentById(Long id) throws Exception {
        if (id == null) {
            throw new Exception("Department ID is required");
        }

        Department department = departmentFacade.find(id);
        if (department == null) {
            throw new Exception("Department not found with ID: " + id);
        }

        return buildDepartmentResponseDTO(department, "Department found successfully");
    }

    /**
     * Create new department
     * Sets creater and createdAt fields
     */
    public DepartmentResponseDTO createDepartment(DepartmentCreateRequestDTO request, WebUser user) throws Exception {
        validateCreateRequest(request);

        if (user == null) {
            throw new Exception("User is required for creating department");
        }

        // Load and validate institution
        Institution institution = loadAndValidateInstitution(request.getInstitutionId());

        // Create new department entity
        Department department = new Department();
        department.setName(request.getName());

        // Generate code if not provided
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            department.setCode(request.getCode());
        } else {
            department.setCode(CommonFunctions.nameToCode(request.getName()));
        }

        department.setDepartmentType(request.getDepartmentType());
        department.setInstitution(institution);
        department.setDescription(request.getDescription());
        department.setPrintingName(request.getPrintingName());
        department.setAddress(request.getAddress());
        department.setTelephone1(request.getTelephone1());
        department.setTelephone2(request.getTelephone2());
        department.setFax(request.getFax());
        department.setEmail(request.getEmail());

        // Set site if provided
        if (request.getSiteId() != null) {
            Institution site = loadAndValidateSite(request.getSiteId());
            department.setSite(site);
        }

        // Set super department if provided
        if (request.getSuperDepartmentId() != null) {
            Department superDepartment = loadAndValidateDepartment(request.getSuperDepartmentId());
            department.setSuperDepartment(superDepartment);
        }

        // Set margins if provided
        if (request.getMargin() != null) {
            department.setMargin(request.getMargin());
        }

        if (request.getPharmacyMarginFromPurchaseRate() != null) {
            department.setPharmacyMarginFromPurchaseRate(request.getPharmacyMarginFromPurchaseRate());
        }

        // Set audit fields
        department.setCreater(user);
        department.setCreatedAt(Calendar.getInstance().getTime());
        department.setRetired(false);

        // Save department
        departmentFacade.create(department);

        return buildDepartmentResponseDTO(department, "Department created successfully");
    }

    /**
     * Update existing department
     * Only updates fields that are provided (not null)
     */
    public DepartmentResponseDTO updateDepartment(Long id, DepartmentUpdateRequestDTO request, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Department ID is required");
        }

        if (request == null || !request.isValid()) {
            throw new Exception("Valid update request is required");
        }

        if (user == null) {
            throw new Exception("User is required for updating department");
        }

        Department department = loadAndValidateDepartment(id);

        // Update fields only if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            department.setName(request.getName());
        }

        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            department.setCode(request.getCode());
        }

        if (request.getDepartmentType() != null) {
            department.setDepartmentType(request.getDepartmentType());
        }

        if (request.getDescription() != null) {
            department.setDescription(request.getDescription());
        }

        if (request.getPrintingName() != null) {
            department.setPrintingName(request.getPrintingName());
        }

        if (request.getAddress() != null) {
            department.setAddress(request.getAddress());
        }

        if (request.getTelephone1() != null) {
            department.setTelephone1(request.getTelephone1());
        }

        if (request.getTelephone2() != null) {
            department.setTelephone2(request.getTelephone2());
        }

        if (request.getFax() != null) {
            department.setFax(request.getFax());
        }

        if (request.getEmail() != null) {
            department.setEmail(request.getEmail());
        }

        if (request.getMargin() != null) {
            department.setMargin(request.getMargin());
        }

        if (request.getPharmacyMarginFromPurchaseRate() != null) {
            department.setPharmacyMarginFromPurchaseRate(request.getPharmacyMarginFromPurchaseRate());
        }

        // Save updated department
        departmentFacade.edit(department);

        return buildDepartmentResponseDTO(department, "Department updated successfully");
    }

    /**
     * Retire department
     * Sets retired flag and retirer/retiredAt fields
     */
    public DepartmentResponseDTO retireDepartment(Long id, String retireComments, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Department ID is required");
        }

        if (user == null) {
            throw new Exception("User is required for retiring department");
        }

        Department department = loadAndValidateDepartment(id);

        if (department.isRetired()) {
            throw new Exception("Department is already retired");
        }

        // Set retire fields
        department.setRetired(true);
        department.setRetirer(user);
        department.setRetiredAt(Calendar.getInstance().getTime());
        department.setRetireComments(retireComments);

        // Save retired department
        departmentFacade.edit(department);

        return buildDepartmentResponseDTO(department, "Department retired successfully");
    }

    /**
     * Change department relationships (institution, site, super department)
     */
    public DepartmentResponseDTO changeDepartmentRelationships(DepartmentRelationshipUpdateDTO request, WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid relationship update request is required");
        }

        if (user == null) {
            throw new Exception("User is required for updating department relationships");
        }

        Department department = loadAndValidateDepartment(request.getDepartmentId());

        // Update institution if provided
        if (request.getInstitutionId() != null) {
            Institution institution = loadAndValidateInstitution(request.getInstitutionId());
            department.setInstitution(institution);
        }

        // Update site if provided
        if (request.getSiteId() != null) {
            Institution site = loadAndValidateSite(request.getSiteId());
            department.setSite(site);
        }

        // Update super department if provided
        if (request.getSuperDepartmentId() != null) {
            Department superDepartment = loadAndValidateDepartment(request.getSuperDepartmentId());

            // Prevent circular reference (direct and transitive)
            if (superDepartment.getId().equals(department.getId())) {
                throw new Exception("Department cannot be its own super department");
            }

            // Walk the ancestor chain to detect circular hierarchy
            Department ancestor = superDepartment.getSuperDepartment();
            while (ancestor != null) {
                if (ancestor.getId().equals(department.getId())) {
                    throw new Exception("Circular department hierarchy detected");
                }
                ancestor = ancestor.getSuperDepartment();
            }

            department.setSuperDepartment(superDepartment);
        }

        // Save updated department
        departmentFacade.edit(department);

        return buildDepartmentResponseDTO(department, "Department relationships updated successfully");
    }

    // Private helper methods

    /**
     * Validate create request
     */
    private void validateCreateRequest(DepartmentCreateRequestDTO request) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid create request is required");
        }

        // Check for duplicate name within same institution
        if (isDuplicateName(request.getName(), request.getInstitutionId(), null)) {
            throw new Exception("Department with name '" + request.getName() + "' already exists in this institution");
        }

        // Check for duplicate code if provided
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            if (isDuplicateCode(request.getCode(), request.getInstitutionId(), null)) {
                throw new Exception("Department with code '" + request.getCode() + "' already exists in this institution");
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
        if (site.isRetired()) {
            throw new Exception("Site is retired");
        }
        return site;
    }

    /**
     * Load and validate department
     */
    private Department loadAndValidateDepartment(Long departmentId) throws Exception {
        Department department = departmentFacade.find(departmentId);
        if (department == null) {
            throw new Exception("Department not found with ID: " + departmentId);
        }
        if (department.isRetired()) {
            throw new Exception("Department is retired");
        }
        return department;
    }

    /**
     * Check for duplicate department name within institution
     */
    private boolean isDuplicateName(String name, Long institutionId, Long excludeId) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name.trim());
        params.put("institutionId", institutionId);

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT COUNT(d) FROM Department d ")
            .append("WHERE d.retired = false ")
            .append("AND d.name = :name ")
            .append("AND d.institution.id = :institutionId ");

        if (excludeId != null) {
            jpql.append("AND d.id != :excludeId ");
            params.put("excludeId", excludeId);
        }

        Long count = departmentFacade.countByJpql(jpql.toString(), params);
        return count != null && count > 0;
    }

    /**
     * Check for duplicate department code within institution
     */
    private boolean isDuplicateCode(String code, Long institutionId, Long excludeId) {
        Map<String, Object> params = new HashMap<>();
        params.put("code", code.trim());
        params.put("institutionId", institutionId);

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT COUNT(d) FROM Department d ")
            .append("WHERE d.retired = false ")
            .append("AND d.code = :code ")
            .append("AND d.institution.id = :institutionId ");

        if (excludeId != null) {
            jpql.append("AND d.id != :excludeId ");
            params.put("excludeId", excludeId);
        }

        Long count = departmentFacade.countByJpql(jpql.toString(), params);
        return count != null && count > 0;
    }

    /**
     * Build department response DTO from entity
     */
    private DepartmentResponseDTO buildDepartmentResponseDTO(Department department, String message) {
        DepartmentResponseDTO response = new DepartmentResponseDTO();
        response.setId(department.getId());
        response.setName(department.getName());
        response.setCode(department.getCode());
        response.setDepartmentType(department.getDepartmentType());
        response.setDescription(department.getDescription());
        response.setPrintingName(department.getPrintingName());
        response.setAddress(department.getAddress());
        response.setTelephone1(department.getTelephone1());
        response.setTelephone2(department.getTelephone2());
        response.setFax(department.getFax());
        response.setEmail(department.getEmail());
        response.setMargin(department.getMargin());
        response.setPharmacyMarginFromPurchaseRate(department.getPharmacyMarginFromPurchaseRate());
        response.setActive(!department.isRetired());
        response.setCreatedAt(department.getCreatedAt());

        // Set institution details
        if (department.getInstitution() != null) {
            response.setInstitutionId(department.getInstitution().getId());
            response.setInstitutionName(department.getInstitution().getName());
        }

        // Set site details if exists
        if (department.getSite() != null) {
            response.setSiteId(department.getSite().getId());
            response.setSiteName(department.getSite().getName());
        }

        // Set super department details if exists
        if (department.getSuperDepartment() != null) {
            response.setSuperDepartmentId(department.getSuperDepartment().getId());
            response.setSuperDepartmentName(department.getSuperDepartment().getName());
        }

        response.setMessage(message);
        return response;
    }
}
