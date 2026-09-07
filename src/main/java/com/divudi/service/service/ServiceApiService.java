/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.service;

import com.divudi.core.data.FeeType;
import com.divudi.core.data.dto.service.ItemFeeCreateRequestDTO;
import com.divudi.core.data.dto.service.ItemFeeDTO;
import com.divudi.core.data.dto.service.ItemFeeUpdateRequestDTO;
import com.divudi.core.data.dto.service.ServiceCategoryDTO;
import com.divudi.core.data.dto.service.ServiceCreateRequestDTO;
import com.divudi.core.data.dto.service.ServiceResponseDTO;
import com.divudi.core.data.dto.service.ServiceSearchResultDTO;
import com.divudi.core.data.dto.service.ServiceUpdateRequestDTO;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.ItemFee;
import com.divudi.core.entity.Service;
import com.divudi.core.entity.ServiceCategory;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.InwardService;
import com.divudi.core.facade.CategoryFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.InwardServiceFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.ItemFeeFacade;
import com.divudi.core.facade.ServiceCategoryFacade;
import com.divudi.core.facade.ServiceFacade;
import com.divudi.core.facade.SpecialityFacade;
import com.divudi.core.facade.StaffFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.AuditService;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Service API operations.
 * Provides business logic for managing OPD Services (Service DTYPE),
 * Inward Services (InwardService DTYPE), their fees (ItemFee), and
 * their categories (ServiceCategory).
 *
 * @author Buddhika
 */
@Stateless
public class ServiceApiService implements Serializable {

    @EJB
    private ServiceFacade serviceFacade;

    @EJB
    private InwardServiceFacade inwardServiceFacade;

    @EJB
    private ServiceCategoryFacade serviceCategoryFacade;

    @EJB
    private CategoryFacade categoryFacade;

    @EJB
    private ItemFacade itemFacade;

    @EJB
    private ItemFeeFacade itemFeeFacade;

    @EJB
    private InstitutionFacade institutionFacade;

    @EJB
    private DepartmentFacade departmentFacade;

    @EJB
    private SpecialityFacade specialityFacade;

    @EJB
    private StaffFacade staffFacade;

    @EJB
    private AuditService auditService;

    // =========================================================================
    // Service Search
    // =========================================================================

    /**
     * Search services by name with optional type and category filters.
     * Queries Item table using DTYPE column for type discrimination.
     * Always excludes retired items.
     */
    public List<ServiceSearchResultDTO> searchServices(String query, String serviceType,
            Long categoryId, Boolean inactive, int limit) throws Exception {

        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + (query != null ? query : "") + "%");

        StringBuilder jpql = new StringBuilder();
        // Query FROM Service covers both Service (OPD) and InwardService (Inward)
        // since InwardService extends Service. Type filter narrows if needed.
        jpql.append("SELECT i FROM Service i ")
            .append("WHERE i.retired = false ");

        // Type filter using DTYPE discriminator
        if ("OPD".equalsIgnoreCase(serviceType)) {
            jpql.append("AND type(i) = Service ");
        } else if ("Inward".equalsIgnoreCase(serviceType)) {
            jpql.append("AND type(i) = InwardService ");
        } else {
            // Default: restrict to OPD and Inward only, excluding other subtypes (e.g. TheatreService)
            jpql.append("AND (type(i) = Service OR type(i) = InwardService) ");
        }

        if (query != null && !query.trim().isEmpty()) {
            jpql.append("AND i.name LIKE :query ");
        } else {
            params.remove("query");
        }

        if (categoryId != null) {
            jpql.append("AND i.category.id = :categoryId ");
            params.put("categoryId", categoryId);
        }

        if (inactive != null) {
            jpql.append("AND i.inactive = :inactive ");
            params.put("inactive", inactive);
        }

        jpql.append("ORDER BY i.name");

        @SuppressWarnings("unchecked")
        List<Service> results = serviceFacade.findByJpql(
                jpql.toString(), params, TemporalType.TIMESTAMP, limit);

        List<ServiceSearchResultDTO> dtos = new ArrayList<>();
        for (Service item : results) {
            dtos.add(buildSearchResultDTO(item));
        }
        return dtos;
    }

    // =========================================================================
    // Service Get by ID
    // =========================================================================

    /**
     * Find a service by ID and return full details including fees.
     */
    public ServiceResponseDTO findServiceById(Long id) throws Exception {
        Service item = loadAndValidateService(id);
        List<ItemFee> fees = fetchFeesForItem(item);
        return buildServiceResponseDTO(item, fees, "Service found successfully");
    }

    // =========================================================================
    // Service Create
    // =========================================================================

    /**
     * Create a new Service (OPD) or InwardService.
     * Mirrors ServiceController.saveSelected() / InwardServiceController.saveSelected() logic.
     */
    public ServiceResponseDTO createService(ServiceCreateRequestDTO request, WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid create request is required (serviceType and name are required)");
        }
        if (user == null) {
            throw new Exception("User is required for creating service");
        }

        String svcType = request.getServiceType().trim();

        // Validate inwardChargeType for Inward services
        InwardChargeType inwardChargeType = null;
        if ("Inward".equalsIgnoreCase(svcType)) {
            if (request.getInwardChargeType() == null || request.getInwardChargeType().trim().isEmpty()) {
                throw new Exception("inwardChargeType is required when serviceType is Inward");
            }
            try {
                inwardChargeType = InwardChargeType.valueOf(request.getInwardChargeType().trim());
            } catch (IllegalArgumentException e) {
                throw new Exception("Invalid inwardChargeType: " + request.getInwardChargeType());
            }
        }

        // Create entity
        Service service;
        if ("Inward".equalsIgnoreCase(svcType)) {
            service = new InwardService();
        } else {
            service = new Service();
        }

        service.setName(request.getName().trim());

        // Auto-generate code if not provided
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            service.setCode(request.getCode().trim());
        } else {
            service.setCode(CommonFunctions.nameToCode(request.getName()));
        }

        if (request.getPrintName() != null) {
            service.setPrintName(request.getPrintName());
        }
        if (request.getFullName() != null) {
            service.setFullName(request.getFullName());
        }

        if (inwardChargeType != null) {
            service.setInwardChargeType(inwardChargeType);
        }

        service.setInactive(request.isInactive());
        service.setDiscountAllowed(request.isDiscountAllowed());
        service.setUserChangable(request.isUserChangable());
        service.setChargesVisibleForInward(request.isChargesVisibleForInward());
        service.setMarginNotAllowed(request.isMarginNotAllowed());
        service.setRequestForQuentity(request.isRequestForQuentity());
        service.setPatientNotRequired(request.isPatientNotRequired());
        service.setVatable(request.isVatable());
        validateVatPercentage(request.getVatPercentage());
        service.setVatPercentage(request.getVatPercentage());

        // Resolve optional associations
        if (request.getCategoryId() != null) {
            // Category (not ServiceCategoryFacade) is used here since a service's category
            // can be any Category subtype already in use in the system (Category,
            // ServiceCategory, ...), not only rows created specifically as ServiceCategory.
            Category category = categoryFacade.find(request.getCategoryId());
            if (category == null || category.isRetired()) {
                throw new Exception("Category not found with ID: " + request.getCategoryId());
            }
            service.setCategory(category);
        }

        if (request.getInstitutionId() != null) {
            Institution institution = institutionFacade.find(request.getInstitutionId());
            if (institution == null) {
                throw new Exception("Institution not found with ID: " + request.getInstitutionId());
            }
            service.setInstitution(institution);
        }

        if (request.getDepartmentId() != null) {
            Department department = departmentFacade.find(request.getDepartmentId());
            if (department == null) {
                throw new Exception("Department not found with ID: " + request.getDepartmentId());
            }
            service.setDepartment(department);
        }

        // Set audit fields
        service.setCreater(user);
        service.setCreatedAt(Calendar.getInstance().getTime());
        service.setRetired(false);

        // Persist
        if ("Inward".equalsIgnoreCase(svcType)) {
            inwardServiceFacade.create((InwardService) service);
            // Set self-references after persist
            service.setBilledAs(service);
            service.setReportedAs(service);
            inwardServiceFacade.edit((InwardService) service);
        } else {
            serviceFacade.create(service);
            // Set self-references after persist
            service.setBilledAs(service);
            service.setReportedAs(service);
            serviceFacade.edit(service);
        }

        return buildServiceResponseDTO(service, new ArrayList<>(), "Service created successfully");
    }

    // =========================================================================
    // Service Update
    // =========================================================================

    /**
     * Update an existing service. Only non-null fields are applied.
     */
    public ServiceResponseDTO updateService(Long id, ServiceUpdateRequestDTO request, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Service ID is required");
        }
        if (request == null || !request.isValid()) {
            throw new Exception("Valid update request is required (at least one field must be provided)");
        }
        if (user == null) {
            throw new Exception("User is required for updating service");
        }

        Service service = loadAndValidateService(id);

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            service.setName(request.getName().trim());
        }
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            service.setCode(request.getCode().trim());
        }
        if (request.getPrintName() != null) {
            service.setPrintName(request.getPrintName());
        }
        if (request.getFullName() != null) {
            service.setFullName(request.getFullName());
        }
        if (request.getInwardChargeType() != null && !request.getInwardChargeType().trim().isEmpty()) {
            try {
                service.setInwardChargeType(InwardChargeType.valueOf(request.getInwardChargeType().trim()));
            } catch (IllegalArgumentException e) {
                throw new Exception("Invalid inwardChargeType: " + request.getInwardChargeType());
            }
        }
        if (request.getInactive() != null) {
            service.setInactive(request.getInactive());
        }
        if (request.getDiscountAllowed() != null) {
            service.setDiscountAllowed(request.getDiscountAllowed());
        }
        if (request.getUserChangable() != null) {
            service.setUserChangable(request.getUserChangable());
        }
        if (request.getChargesVisibleForInward() != null) {
            service.setChargesVisibleForInward(request.getChargesVisibleForInward());
        }
        if (request.getMarginNotAllowed() != null) {
            service.setMarginNotAllowed(request.getMarginNotAllowed());
        }
        if (request.getRequestForQuentity() != null) {
            service.setRequestForQuentity(request.getRequestForQuentity());
        }
        if (request.getPatientNotRequired() != null) {
            service.setPatientNotRequired(request.getPatientNotRequired());
        }
        if (request.getVatable() != null) {
            service.setVatable(request.getVatable());
        }
        if (request.getVatPercentage() != null) {
            validateVatPercentage(request.getVatPercentage());
            service.setVatPercentage(request.getVatPercentage());
        }

        if (request.getCategoryId() != null) {
            // Category (not ServiceCategoryFacade) is used here since a service's category
            // can be any Category subtype already in use in the system (Category,
            // ServiceCategory, ...), not only rows created specifically as ServiceCategory.
            Category category = categoryFacade.find(request.getCategoryId());
            if (category == null || category.isRetired()) {
                throw new Exception("Category not found with ID: " + request.getCategoryId());
            }
            service.setCategory(category);
        }
        if (request.getInstitutionId() != null) {
            Institution institution = institutionFacade.find(request.getInstitutionId());
            if (institution == null) {
                throw new Exception("Institution not found with ID: " + request.getInstitutionId());
            }
            service.setInstitution(institution);
        }
        if (request.getDepartmentId() != null) {
            Department department = departmentFacade.find(request.getDepartmentId());
            if (department == null) {
                throw new Exception("Department not found with ID: " + request.getDepartmentId());
            }
            service.setDepartment(department);
        }

        service.setEditer(user);
        service.setEditedAt(Calendar.getInstance().getTime());

        saveService(service);

        List<ItemFee> fees = fetchFeesForItem(service);
        return buildServiceResponseDTO(service, fees, "Service updated successfully");
    }

    // =========================================================================
    // Service Retire
    // =========================================================================

    /**
     * Retire a service (permanent soft-delete, sets retired=true).
     */
    public ServiceResponseDTO retireService(Long id, String retireComments, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Service ID is required");
        }
        if (user == null) {
            throw new Exception("User is required for retiring service");
        }

        Service service = loadAndValidateService(id);

        service.setRetired(true);
        service.setRetirer(user);
        service.setRetiredAt(Calendar.getInstance().getTime());
        service.setRetireComments(retireComments);

        saveService(service);

        return buildServiceResponseDTO(service, new ArrayList<>(), "Service retired successfully");
    }

    // =========================================================================
    // Service Activate / Deactivate
    // =========================================================================

    /**
     * Activate a service (set inactive=false).
     */
    public ServiceResponseDTO activateService(Long id, WebUser user) throws Exception {
        if (user == null) {
            throw new Exception("User is required for activating service");
        }
        Service service = loadAndValidateService(id);
        service.setInactive(false);
        service.setEditer(user);
        service.setEditedAt(Calendar.getInstance().getTime());
        saveService(service);
        return buildServiceResponseDTO(service, new ArrayList<>(), "Service activated successfully");
    }

    /**
     * Deactivate a service (set inactive=true).
     */
    public ServiceResponseDTO deactivateService(Long id, WebUser user) throws Exception {
        if (user == null) {
            throw new Exception("User is required for deactivating service");
        }
        Service service = loadAndValidateService(id);
        service.setInactive(true);
        service.setEditer(user);
        service.setEditedAt(Calendar.getInstance().getTime());
        saveService(service);
        return buildServiceResponseDTO(service, new ArrayList<>(), "Service deactivated successfully");
    }

    // =========================================================================
    // Fee Management
    // =========================================================================

    /**
     * List all non-retired fees for any Item (Service, InwardService, Investigation, etc.).
     */
    public List<ItemFeeDTO> listFees(Long serviceId) throws Exception {
        Item item = loadAndValidateItem(serviceId);
        List<ItemFee> fees = fetchFeesForItem(item);
        List<ItemFeeDTO> dtos = new ArrayList<>();
        for (ItemFee fee : fees) {
            dtos.add(buildItemFeeDTO(fee));
        }
        return dtos;
    }

    /**
     * Add a new fee to any Item (Service, InwardService, Investigation, etc.) and recalculate its total.
     */
    public ServiceResponseDTO addFee(Long serviceId, ItemFeeCreateRequestDTO request, WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid fee request is required (name and feeType are required)");
        }
        if (user == null) {
            throw new Exception("User is required for adding fee");
        }

        Item item = loadAndValidateItem(serviceId);

        FeeType feeType;
        try {
            feeType = FeeType.valueOf(request.getFeeType().trim());
        } catch (IllegalArgumentException e) {
            throw new Exception("Invalid feeType: " + request.getFeeType());
        }

        ItemFee itemFee = new ItemFee();
        itemFee.setItem(item);
        itemFee.setName(request.getName().trim());
        itemFee.setFeeType(feeType);
        if (request.getFee() == null || request.getFee() < 0) {
            throw new Exception("Fee amount must be a non-negative value");
        }
        itemFee.setFee(request.getFee());
        if (request.getFfee() != null) {
            if (request.getFfee() < 0) {
                throw new Exception("Foreigner fee amount must be a non-negative value");
            }
            itemFee.setFfee(request.getFfee());
        } else {
            itemFee.setFfee(request.getFee());
        }
        itemFee.setDiscountAllowed(request.isDiscountAllowed());
        itemFee.setCreater(user);
        itemFee.setCreatedAt(Calendar.getInstance().getTime());
        itemFee.setRetired(false);

        if (request.getInstitutionId() != null) {
            Institution institution = institutionFacade.find(request.getInstitutionId());
            if (institution == null) {
                throw new Exception("Institution not found with ID: " + request.getInstitutionId());
            }
            itemFee.setInstitution(institution);
        }
        if (request.getDepartmentId() != null) {
            Department department = departmentFacade.find(request.getDepartmentId());
            if (department == null) {
                throw new Exception("Department not found with ID: " + request.getDepartmentId());
            }
            itemFee.setDepartment(department);
        }
        if (request.getSpecialityId() != null) {
            Speciality speciality = specialityFacade.find(request.getSpecialityId());
            if (speciality == null) {
                throw new Exception("Speciality not found with ID: " + request.getSpecialityId());
            }
            itemFee.setSpeciality(speciality);
        }
        if (request.getStaffId() != null) {
            Staff staff = staffFacade.find(request.getStaffId());
            if (staff == null) {
                throw new Exception("Staff not found with ID: " + request.getStaffId());
            }
            itemFee.setStaff(staff);
        }

        itemFeeFacade.create(itemFee);

        // Recalculate item totals
        recalculateItemTotal(item);

        List<ItemFee> fees = fetchFeesForItem(item);
        return buildServiceResponseDTO(item, fees, "Fee added successfully");
    }

    /**
     * Update an existing fee and recalculate the parent item's total.
     * Works for any Item subtype (Service, InwardService, Investigation, etc.).
     */
    public ServiceResponseDTO updateFee(Long serviceId, Long feeId, ItemFeeUpdateRequestDTO request, WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid update request is required (at least one field must be provided)");
        }
        if (user == null) {
            throw new Exception("User is required for updating fee");
        }

        Item item = loadAndValidateItem(serviceId);
        ItemFee itemFee = loadAndValidateFee(feeId, item);

        // Snapshot before-state for audit
        Map<String, Object> beforeFlags = new HashMap<>();
        beforeFlags.put("marginAllowed", itemFee.getMarginAllowed());
        beforeFlags.put("discountAllowed", itemFee.isDiscountAllowed());

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            itemFee.setName(request.getName().trim());
        }
        if (request.getFeeType() != null && !request.getFeeType().trim().isEmpty()) {
            try {
                itemFee.setFeeType(FeeType.valueOf(request.getFeeType().trim()));
            } catch (IllegalArgumentException e) {
                throw new Exception("Invalid feeType: " + request.getFeeType());
            }
        }
        if (request.getFee() != null) {
            if (request.getFee() < 0) {
                throw new Exception("Fee amount must be a non-negative value");
            }
            itemFee.setFee(request.getFee());
        }
        if (request.getFfee() != null) {
            if (request.getFfee() < 0) {
                throw new Exception("Foreigner fee amount must be a non-negative value");
            }
            itemFee.setFfee(request.getFfee());
        }
        if (request.getDiscountAllowed() != null) {
            itemFee.setDiscountAllowed(request.getDiscountAllowed());
        }
        if (request.getMarginAllowed() != null) {
            itemFee.setMarginAllowed(request.getMarginAllowed());
        }
        if (request.getInstitutionId() != null) {
            Institution institution = institutionFacade.find(request.getInstitutionId());
            if (institution == null) {
                throw new Exception("Institution not found with ID: " + request.getInstitutionId());
            }
            itemFee.setInstitution(institution);
        }
        if (request.getDepartmentId() != null) {
            Department department = departmentFacade.find(request.getDepartmentId());
            if (department == null) {
                throw new Exception("Department not found with ID: " + request.getDepartmentId());
            }
            itemFee.setDepartment(department);
        }
        if (request.getSpecialityId() != null) {
            Speciality speciality = specialityFacade.find(request.getSpecialityId());
            if (speciality == null) {
                throw new Exception("Speciality not found with ID: " + request.getSpecialityId());
            }
            itemFee.setSpeciality(speciality);
        }
        if (request.getStaffId() != null) {
            Staff staff = staffFacade.find(request.getStaffId());
            if (staff == null) {
                throw new Exception("Staff not found with ID: " + request.getStaffId());
            }
            itemFee.setStaff(staff);
        }

        itemFee.setEditer(user);
        itemFee.setEditedAt(Calendar.getInstance().getTime());
        itemFeeFacade.edit(itemFee);

        // Audit flag changes
        Map<String, Object> afterFlags = new HashMap<>();
        afterFlags.put("marginAllowed", itemFee.getMarginAllowed());
        afterFlags.put("discountAllowed", itemFee.isDiscountAllowed());
        if (!java.util.Objects.equals(beforeFlags.get("marginAllowed"), afterFlags.get("marginAllowed"))
                || !java.util.Objects.equals(beforeFlags.get("discountAllowed"), afterFlags.get("discountAllowed"))) {
            auditService.logAudit(beforeFlags, afterFlags, user, "ItemFee", "FEE_FLAG_UPDATED", feeId);
        }

        // Recalculate item totals
        recalculateItemTotal(item);

        List<ItemFee> fees = fetchFeesForItem(item);
        return buildServiceResponseDTO(item, fees, "Fee updated successfully");
    }

    /**
     * Remove a fee (soft-delete) and recalculate the parent item's total.
     * Works for any Item subtype (Service, InwardService, Investigation, etc.).
     */
    public ServiceResponseDTO removeFee(Long serviceId, Long feeId, WebUser user) throws Exception {
        if (user == null) {
            throw new Exception("User is required for removing fee");
        }

        Item item = loadAndValidateItem(serviceId);
        ItemFee itemFee = loadAndValidateFee(feeId, item);

        itemFee.setRetired(true);
        itemFee.setRetirer(user);
        itemFee.setRetiredAt(Calendar.getInstance().getTime());
        itemFeeFacade.edit(itemFee);

        // Recalculate item totals
        recalculateItemTotal(item);

        List<ItemFee> fees = fetchFeesForItem(item);
        return buildServiceResponseDTO(item, fees, "Fee removed successfully");
    }

    // =========================================================================
    // Fee Flag Bulk Operations
    // =========================================================================

    /**
     * Item subtypes selectable by the bulk-flag endpoints below via itemType.
     * There is no API to enumerate every Category id in the system (e.g. every
     * InvestigationCategory), so scoping a bulk update to "every item of this
     * subtype" is the only reliable way to cover something like "all
     * investigations" without looping over guessed/incomplete category lists.
     */
    private Class<? extends Item> resolveItemType(String itemTypeStr) throws Exception {
        if (itemTypeStr == null || itemTypeStr.trim().isEmpty()) {
            return null;
        }
        switch (itemTypeStr.trim()) {
            case "Investigation":
                return com.divudi.core.entity.lab.Investigation.class;
            case "Service":
                return Service.class;
            case "InwardService":
                return InwardService.class;
            default:
                throw new Exception("Invalid itemType: " + itemTypeStr
                        + ". Use one of: Investigation, Service, InwardService");
        }
    }

    /**
     * Bulk-update marginAllowed and/or discountAllowed on all non-retired fees
     * for items in a given category and/or item subtype, with a given feeType.
     * At least one of categoryId/itemType is required as a safety guard against
     * an unscoped update of every fee in the system.
     */
    public Map<String, Object> bulkUpdateMargin(Long categoryId, String feeTypeStr,
            Boolean marginAllowed, Boolean discountAllowed, WebUser user) throws Exception {
        return bulkUpdateMargin(categoryId, null, feeTypeStr, marginAllowed, discountAllowed, user);
    }

    public Map<String, Object> bulkUpdateMargin(Long categoryId, String itemTypeStr, String feeTypeStr,
            Boolean marginAllowed, Boolean discountAllowed, WebUser user) throws Exception {
        Class<? extends Item> itemType = resolveItemType(itemTypeStr);
        if (categoryId == null && itemType == null) {
            throw new Exception("At least one of categoryId or itemType is required");
        }
        if (user == null) {
            throw new Exception("User is required for bulk update");
        }
        if (marginAllowed == null && discountAllowed == null) {
            throw new Exception("At least one of marginAllowed or discountAllowed must be provided");
        }

        FeeType feeType = null;
        if (feeTypeStr != null && !feeTypeStr.trim().isEmpty()) {
            try {
                feeType = FeeType.valueOf(feeTypeStr.trim());
            } catch (IllegalArgumentException e) {
                throw new Exception("Invalid feeType: " + feeTypeStr);
            }
        }

        StringBuilder jpqlBuilder = new StringBuilder("SELECT f FROM ItemFee f "
                + "WHERE f.retired = false");
        Map<String, Object> params = new HashMap<>();
        if (categoryId != null) {
            jpqlBuilder.append(" AND f.item.category.id = :catId");
            params.put("catId", categoryId);
        }
        if (itemType != null) {
            jpqlBuilder.append(" AND TYPE(f.item) = :itype");
            params.put("itype", itemType);
        }
        if (feeType != null) {
            jpqlBuilder.append(" AND f.feeType = :ft");
            params.put("ft", feeType);
        }

        List<ItemFee> fees = itemFeeFacade.findByJpql(jpqlBuilder.toString(), params);
        int count = 0;
        Map<String, Object> changes = new HashMap<>();
        changes.put("categoryId", categoryId);
        changes.put("itemType", itemType != null ? itemType.getSimpleName() : "ALL_TYPES");
        changes.put("feeType", feeType != null ? feeType.name() : "ALL_TYPES");
        if (marginAllowed != null) {
            changes.put("marginAllowed", marginAllowed);
        }
        if (discountAllowed != null) {
            changes.put("discountAllowed", discountAllowed);
        }

        for (ItemFee fee : fees) {
            if (marginAllowed != null) {
                fee.setMarginAllowed(marginAllowed);
            }
            if (discountAllowed != null) {
                fee.setDiscountAllowed(discountAllowed);
            }
            itemFeeFacade.edit(fee);
            count++;
        }

        changes.put("count", count);
        auditService.logAudit(null, changes, user, "ItemFee",
                "FEE_FLAGS_BULK_UPDATED", null);

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return result;
    }

    /**
     * Bulk-update discountAllowed (item-level, not fee-level) on all non-retired
     * items in a given category and/or item subtype. Distinct from
     * {@link #bulkUpdateMargin} above, which only touches ItemFee.discountAllowed
     * — the inward discount calculation
     * (InwardBeanController.applyInwardDiscountToBillFee) requires BOTH
     * Item.discountAllowed and ItemFee.discountAllowed to be true, so both bulk
     * operations are typically needed together. At least one of categoryId/
     * itemType is required as a safety guard against an unscoped update of
     * every item in the system; itemType lets a caller target e.g. "every
     * Investigation" directly, since there is no API to enumerate every
     * InvestigationCategory id to loop over instead.
     */
    public Map<String, Object> bulkUpdateItemDiscountAllowed(Long categoryId, Boolean discountAllowed, WebUser user) throws Exception {
        return bulkUpdateItemDiscountAllowed(categoryId, null, discountAllowed, user);
    }

    public Map<String, Object> bulkUpdateItemDiscountAllowed(Long categoryId, String itemTypeStr,
            Boolean discountAllowed, WebUser user) throws Exception {
        Class<? extends Item> itemType = resolveItemType(itemTypeStr);
        if (categoryId == null && itemType == null) {
            throw new Exception("At least one of categoryId or itemType is required");
        }
        if (user == null) {
            throw new Exception("User is required for bulk update");
        }
        if (discountAllowed == null) {
            throw new Exception("discountAllowed is required");
        }

        StringBuilder jpqlBuilder = new StringBuilder("SELECT i FROM Item i WHERE i.retired = false");
        Map<String, Object> params = new HashMap<>();
        if (categoryId != null) {
            jpqlBuilder.append(" AND i.category.id = :catId");
            params.put("catId", categoryId);
        }
        if (itemType != null) {
            jpqlBuilder.append(" AND TYPE(i) = :itype");
            params.put("itype", itemType);
        }
        String jpql = jpqlBuilder.toString();

        List<Item> items = itemFacade.findByJpql(jpql, params);
        int count = 0;
        for (Item item : items) {
            item.setDiscountAllowed(discountAllowed);
            itemFacade.edit(item);
            count++;
        }

        Map<String, Object> changes = new HashMap<>();
        changes.put("categoryId", categoryId);
        changes.put("itemType", itemType != null ? itemType.getSimpleName() : "ALL_TYPES");
        changes.put("discountAllowed", discountAllowed);
        changes.put("count", count);
        auditService.logAudit(null, changes, user, "Item", "ITEM_DISCOUNT_ALLOWED_BULK_UPDATED", null);

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return result;
    }

    /**
     * Find fees with marginAllowed disabled (false or null) for items in a category.
     */
    public List<ItemFeeDTO> findFeesWithMarginDisabled(Long categoryId) throws Exception {
        if (categoryId == null) {
            throw new Exception("categoryId is required");
        }

        String jpql = "SELECT f FROM ItemFee f "
                + "WHERE f.item.category.id = :catId "
                + "AND f.retired = false "
                + "AND (f.marginAllowed = false OR f.marginAllowed IS NULL)";
        Map<String, Object> params = new HashMap<>();
        params.put("catId", categoryId);

        List<ItemFee> fees = itemFeeFacade.findByJpql(jpql, params);
        List<ItemFeeDTO> dtos = new ArrayList<>();
        for (ItemFee fee : fees) {
            dtos.add(buildItemFeeDTO(fee));
        }
        return dtos;
    }

    // =========================================================================
    // Service Category CRUD
    // =========================================================================

    /**
     * Search service categories by name.
     */
    public List<ServiceCategoryDTO> searchServiceCategories(String query, int limit) throws Exception {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT c FROM ServiceCategory c ")
            .append("WHERE c.retired = false ");

        if (query != null && !query.trim().isEmpty()) {
            jpql.append("AND c.name LIKE :query ");
            params.put("query", "%" + query + "%");
        }

        jpql.append("ORDER BY c.name");

        @SuppressWarnings("unchecked")
        List<ServiceCategory> results = (List<ServiceCategory>) serviceCategoryFacade.findByJpql(
                jpql.toString(), params, TemporalType.TIMESTAMP, limit);

        List<ServiceCategoryDTO> dtos = new ArrayList<>();
        for (ServiceCategory cat : results) {
            dtos.add(buildServiceCategoryDTO(cat, null));
        }
        return dtos;
    }

    /**
     * Find service category by ID.
     */
    public ServiceCategoryDTO findServiceCategoryById(Long id) throws Exception {
        if (id == null) {
            throw new Exception("Category ID is required");
        }
        ServiceCategory category = serviceCategoryFacade.find(id);
        if (category == null) {
            throw new Exception("ServiceCategory not found with ID: " + id);
        }
        return buildServiceCategoryDTO(category, "Category found successfully");
    }

    /**
     * Create a new ServiceCategory.
     */
    public ServiceCategoryDTO createServiceCategory(String name, String code, String description, WebUser user) throws Exception {
        if (name == null || name.trim().isEmpty()) {
            throw new Exception("Category name is required");
        }
        if (user == null) {
            throw new Exception("User is required for creating category");
        }

        ServiceCategory category = new ServiceCategory();
        category.setName(name.trim());
        category.setCode(code != null && !code.trim().isEmpty() ? code.trim() : CommonFunctions.nameToCode(name));
        category.setDescription(description);
        category.setCreater(user);
        category.setCreatedAt(Calendar.getInstance().getTime());
        category.setRetired(false);

        serviceCategoryFacade.create(category);

        return buildServiceCategoryDTO(category, "Category created successfully");
    }

    /**
     * Update an existing ServiceCategory.
     */
    public ServiceCategoryDTO updateServiceCategory(Long id, String name, String code, String description, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Category ID is required");
        }
        if (user == null) {
            throw new Exception("User is required for updating category");
        }

        ServiceCategory category = serviceCategoryFacade.find(id);
        if (category == null) {
            throw new Exception("ServiceCategory not found with ID: " + id);
        }
        if (category.isRetired()) {
            throw new Exception("ServiceCategory is retired");
        }

        if (name != null && !name.trim().isEmpty()) {
            category.setName(name.trim());
        }
        if (code != null && !code.trim().isEmpty()) {
            category.setCode(code.trim());
        }
        if (description != null) {
            category.setDescription(description);
        }

        serviceCategoryFacade.edit(category);

        return buildServiceCategoryDTO(category, "Category updated successfully");
    }

    /**
     * Retire a ServiceCategory.
     */
    public ServiceCategoryDTO retireServiceCategory(Long id, String retireComments, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Category ID is required");
        }
        if (user == null) {
            throw new Exception("User is required for retiring category");
        }

        ServiceCategory category = serviceCategoryFacade.find(id);
        if (category == null) {
            throw new Exception("ServiceCategory not found with ID: " + id);
        }
        if (category.isRetired()) {
            throw new Exception("ServiceCategory is already retired");
        }

        category.setRetired(true);
        category.setRetirer(user);
        category.setRetiredAt(Calendar.getInstance().getTime());
        category.setRetireComments(retireComments);
        serviceCategoryFacade.edit(category);

        return buildServiceCategoryDTO(category, "Category retired successfully");
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Load a service by ID, ensuring it is not retired and is a Service subtype.
     */
    private Service loadAndValidateService(Long id) throws Exception {
        if (id == null) {
            throw new Exception("Service ID is required");
        }
        // ServiceFacade manages Service.class; since InwardService extends Service,
        // em.find(Service.class, id) correctly returns InwardService instances too.
        Service service = serviceFacade.find(id);
        if (service == null) {
            throw new Exception("Service not found with ID: " + id);
        }
        if (service.isRetired()) {
            throw new Exception("Service with ID " + id + " is retired");
        }
        // Restrict mutations to OPD (Service) and Inward (InwardService) only.
        // Other subtypes such as TheatreService must not be mutated via this API.
        boolean isAllowedType = service.getClass() == Service.class
                || service instanceof InwardService;
        if (!isAllowedType) {
            throw new Exception("Service with ID " + id + " is not an OPD or Inward service");
        }
        return service;
    }

    /**
     * Load any Item by ID (Service, InwardService, Investigation, etc.), ensuring
     * it is not retired. Used by fee-management methods, which operate on ItemFee
     * generically since ItemFee.item is typed as Item, not Service.
     */
    private Item loadAndValidateItem(Long id) throws Exception {
        if (id == null) {
            throw new Exception("Item ID is required");
        }
        Item item = itemFacade.find(id);
        if (item == null) {
            throw new Exception("Item not found with ID: " + id);
        }
        if (item.isRetired()) {
            throw new Exception("Item with ID " + id + " is retired");
        }
        return item;
    }

    private void validateVatPercentage(Double vatPercentage) throws Exception {
        if (vatPercentage != null && (vatPercentage < 0 || vatPercentage > 100)) {
            throw new Exception("vatPercentage must be between 0 and 100");
        }
    }

    /**
     * Load an ItemFee by ID, ensuring it belongs to the given item and is not retired.
     */
    private ItemFee loadAndValidateFee(Long feeId, Item item) throws Exception {
        if (feeId == null) {
            throw new Exception("Fee ID is required");
        }
        ItemFee fee = itemFeeFacade.find(feeId);
        if (fee == null) {
            throw new Exception("Fee not found with ID: " + feeId);
        }
        if (fee.isRetired()) {
            throw new Exception("Fee with ID " + feeId + " is already retired");
        }
        if (fee.getItem() == null || !fee.getItem().getId().equals(item.getId())) {
            throw new Exception("Fee with ID " + feeId + " does not belong to item with ID " + item.getId());
        }
        return fee;
    }

    /**
     * Save a service using the appropriate facade based on type.
     */
    private void saveService(Service service) {
        if (service instanceof InwardService) {
            inwardServiceFacade.edit((InwardService) service);
        } else {
            serviceFacade.edit(service);
        }
    }

    /**
     * Fetch all non-retired ItemFees for an item.
     */
    private List<ItemFee> fetchFeesForItem(Item item) {
        String jpql = "SELECT f FROM ItemFee f WHERE f.item = :item AND f.retired = false ORDER BY f.id";
        Map<String, Object> params = new HashMap<>();
        params.put("item", item);
        return itemFeeFacade.findByJpql(jpql, params);
    }

    /**
     * Recalculate an item's total and totalForForeigner by summing non-retired fees.
     * Mirrors ItemFeeManager.updateFee() logic. Works for any Item subtype
     * (Service, InwardService, Investigation, etc.) via the generic ItemFacade.
     */
    private void recalculateItemTotal(Item item) {
        List<ItemFee> fees = fetchFeesForItem(item);
        double total = 0.0;
        double totalForForeigner = 0.0;
        for (ItemFee fee : fees) {
            total += fee.getFee();
            totalForForeigner += fee.getFfee();
        }
        item.setTotal(total);
        item.setTotalForForeigner(totalForForeigner);
        itemFacade.edit(item);
    }

    /**
     * Build a ServiceSearchResultDTO from an Item entity.
     */
    private ServiceSearchResultDTO buildSearchResultDTO(Item item) {
        ServiceSearchResultDTO dto = new ServiceSearchResultDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setCode(item.getCode());
        dto.setPrintName(item.getPrintName());
        dto.setFullName(item.getFullName());
        dto.setServiceType(item instanceof InwardService ? "Inward" : "OPD");
        dto.setTotal(item.getTotal());
        dto.setTotalForForeigner(item.getTotalForForeigner());
        dto.setInactive(item.isInactive());
        if (item.getCategory() != null) {
            dto.setCategoryId(item.getCategory().getId());
            dto.setCategoryName(item.getCategory().getName());
        }
        if (item.getInwardChargeType() != null) {
            dto.setInwardChargeType(item.getInwardChargeType().name());
        }
        return dto;
    }

    /**
     * Build a ServiceResponseDTO from any Item entity and its fees.
     * Used both by Service/InwardService endpoints and by the generalized fee-management
     * endpoints, which accept any Item subtype (e.g. Investigation).
     */
    private ServiceResponseDTO buildServiceResponseDTO(Item item, List<ItemFee> fees, String message) {
        ServiceResponseDTO dto = new ServiceResponseDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setCode(item.getCode());
        dto.setPrintName(item.getPrintName());
        dto.setFullName(item.getFullName());
        dto.setServiceType(deriveServiceType(item));
        dto.setTotal(item.getTotal());
        dto.setTotalForForeigner(item.getTotalForForeigner());
        dto.setInactive(item.isInactive());
        dto.setRetired(item.isRetired());
        dto.setDiscountAllowed(item.isDiscountAllowed());
        dto.setUserChangable(item.isUserChangable());
        dto.setChargesVisibleForInward(item.isChargesVisibleForInward());
        dto.setMarginNotAllowed(item.isMarginNotAllowed());
        dto.setRequestForQuentity(item.isRequestForQuentity());
        dto.setPatientNotRequired(item.isPatientNotRequired());
        dto.setVatable(item.isVatable());
        dto.setVatPercentage(item.getVatPercentage());
        if (item.getInwardChargeType() != null) {
            dto.setInwardChargeType(item.getInwardChargeType().name());
        }
        if (item.getCategory() != null) {
            dto.setCategoryId(item.getCategory().getId());
            dto.setCategoryName(item.getCategory().getName());
        }
        if (item.getInstitution() != null) {
            dto.setInstitutionId(item.getInstitution().getId());
            dto.setInstitutionName(item.getInstitution().getName());
        }
        if (item.getDepartment() != null) {
            dto.setDepartmentId(item.getDepartment().getId());
            dto.setDepartmentName(item.getDepartment().getName());
        }
        dto.setCreatedAt(item.getCreatedAt());

        List<ItemFeeDTO> feeDtos = new ArrayList<>();
        for (ItemFee fee : fees) {
            feeDtos.add(buildItemFeeDTO(fee));
        }
        dto.setFees(feeDtos);
        dto.setMessage(message);
        return dto;
    }

    /**
     * Derive a human-readable type label for an Item: "Inward" for InwardService,
     * "OPD" for plain Service, otherwise the entity's simple class name (e.g. "Investigation").
     */
    private String deriveServiceType(Item item) {
        if (item instanceof InwardService) {
            return "Inward";
        }
        if (item.getClass() == Service.class) {
            return "OPD";
        }
        return item.getClass().getSimpleName();
    }

    /**
     * Build an ItemFeeDTO from an ItemFee entity.
     */
    private ItemFeeDTO buildItemFeeDTO(ItemFee fee) {
        ItemFeeDTO dto = new ItemFeeDTO();
        dto.setId(fee.getId());
        dto.setName(fee.getName());
        dto.setFeeType(fee.getFeeType() != null ? fee.getFeeType().name() : null);
        dto.setFee(fee.getFee());
        dto.setFfee(fee.getFfee());
        dto.setDiscountAllowed(fee.isDiscountAllowed());
        dto.setMarginAllowed(fee.getMarginAllowed());
        dto.setRetired(fee.isRetired());
        if (fee.getInstitution() != null) {
            dto.setInstitutionId(fee.getInstitution().getId());
            dto.setInstitutionName(fee.getInstitution().getName());
        }
        if (fee.getDepartment() != null) {
            dto.setDepartmentId(fee.getDepartment().getId());
            dto.setDepartmentName(fee.getDepartment().getName());
        }
        if (fee.getSpeciality() != null) {
            dto.setSpecialityId(fee.getSpeciality().getId());
            dto.setSpecialityName(fee.getSpeciality().getName());
        }
        if (fee.getStaff() != null) {
            dto.setStaffId(fee.getStaff().getId());
            dto.setStaffName(fee.getStaff().getName());
        }
        return dto;
    }

    /**
     * Build a ServiceCategoryDTO from a ServiceCategory entity.
     */
    private ServiceCategoryDTO buildServiceCategoryDTO(ServiceCategory category, String message) {
        ServiceCategoryDTO dto = new ServiceCategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setCode(category.getCode());
        dto.setDescription(category.getDescription());
        dto.setRetired(category.isRetired());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setMessage(message);
        return dto;
    }
}
