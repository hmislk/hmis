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
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.InwardServiceFacade;
import com.divudi.core.facade.ItemFeeFacade;
import com.divudi.core.facade.ServiceCategoryFacade;
import com.divudi.core.facade.ServiceFacade;
import com.divudi.core.facade.SpecialityFacade;
import com.divudi.core.facade.StaffFacade;
import com.divudi.core.util.CommonFunctions;

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
    private ItemFeeFacade itemFeeFacade;

    @EJB
    private InstitutionFacade institutionFacade;

    @EJB
    private DepartmentFacade departmentFacade;

    @EJB
    private SpecialityFacade specialityFacade;

    @EJB
    private StaffFacade staffFacade;

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
            jpql.append("AND type(i) = com.divudi.core.entity.Service ");
        } else if ("Inward".equalsIgnoreCase(serviceType)) {
            jpql.append("AND type(i) = com.divudi.core.entity.inward.InwardService ");
        } else {
            // Default: restrict to OPD and Inward only, excluding other subtypes (e.g. TheatreService)
            jpql.append("AND (type(i) = com.divudi.core.entity.Service OR type(i) = com.divudi.core.entity.inward.InwardService) ");
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
        if (id == null) {
            throw new Exception("Service ID is required");
        }

        Service item = serviceFacade.find(id);
        if (item == null) {
            throw new Exception("Service not found with ID: " + id);
        }

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

        // Resolve optional associations
        if (request.getCategoryId() != null) {
            ServiceCategory category = serviceCategoryFacade.find(request.getCategoryId());
            if (category == null) {
                throw new Exception("ServiceCategory not found with ID: " + request.getCategoryId());
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

        if (request.getCategoryId() != null) {
            ServiceCategory category = serviceCategoryFacade.find(request.getCategoryId());
            if (category == null) {
                throw new Exception("ServiceCategory not found with ID: " + request.getCategoryId());
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
     * List all non-retired fees for a service.
     */
    public List<ItemFeeDTO> listFees(Long serviceId) throws Exception {
        Service service = loadAndValidateService(serviceId);
        List<ItemFee> fees = fetchFeesForItem(service);
        List<ItemFeeDTO> dtos = new ArrayList<>();
        for (ItemFee fee : fees) {
            dtos.add(buildItemFeeDTO(fee));
        }
        return dtos;
    }

    /**
     * Add a new fee to a service and recalculate the service total.
     */
    public ServiceResponseDTO addFee(Long serviceId, ItemFeeCreateRequestDTO request, WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid fee request is required (name and feeType are required)");
        }
        if (user == null) {
            throw new Exception("User is required for adding fee");
        }

        Service service = loadAndValidateService(serviceId);

        FeeType feeType;
        try {
            feeType = FeeType.valueOf(request.getFeeType().trim());
        } catch (IllegalArgumentException e) {
            throw new Exception("Invalid feeType: " + request.getFeeType());
        }

        ItemFee itemFee = new ItemFee();
        itemFee.setItem(service);
        itemFee.setName(request.getName().trim());
        itemFee.setFeeType(feeType);
        if (request.getFee() == null || request.getFee() < 0) {
            throw new Exception("Fee amount must be a non-negative value");
        }
        itemFee.setFee(request.getFee());
        itemFee.setFfee(request.getFfee() != null && request.getFfee() > 0 ? request.getFfee() : request.getFee());
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

        // Recalculate service totals
        recalculateServiceTotal(service);

        List<ItemFee> fees = fetchFeesForItem(service);
        return buildServiceResponseDTO(service, fees, "Fee added successfully");
    }

    /**
     * Update an existing fee and recalculate the service total.
     */
    public ServiceResponseDTO updateFee(Long serviceId, Long feeId, ItemFeeUpdateRequestDTO request, WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid update request is required (at least one field must be provided)");
        }
        if (user == null) {
            throw new Exception("User is required for updating fee");
        }

        Service service = loadAndValidateService(serviceId);
        ItemFee itemFee = loadAndValidateFee(feeId, service);

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

        // Recalculate service totals
        recalculateServiceTotal(service);

        List<ItemFee> fees = fetchFeesForItem(service);
        return buildServiceResponseDTO(service, fees, "Fee updated successfully");
    }

    /**
     * Remove a fee (soft-delete) and recalculate the service total.
     */
    public ServiceResponseDTO removeFee(Long serviceId, Long feeId, WebUser user) throws Exception {
        if (user == null) {
            throw new Exception("User is required for removing fee");
        }

        Service service = loadAndValidateService(serviceId);
        ItemFee itemFee = loadAndValidateFee(feeId, service);

        itemFee.setRetired(true);
        itemFee.setRetirer(user);
        itemFee.setRetiredAt(Calendar.getInstance().getTime());
        itemFeeFacade.edit(itemFee);

        // Recalculate service totals
        recalculateServiceTotal(service);

        List<ItemFee> fees = fetchFeesForItem(service);
        return buildServiceResponseDTO(service, fees, "Fee removed successfully");
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
     * Load an ItemFee by ID, ensuring it belongs to the given service and is not retired.
     */
    private ItemFee loadAndValidateFee(Long feeId, Service service) throws Exception {
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
        if (fee.getItem() == null || !fee.getItem().getId().equals(service.getId())) {
            throw new Exception("Fee with ID " + feeId + " does not belong to service with ID " + service.getId());
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
     * Recalculate service total and totalForForeigner by summing non-retired fees.
     * Mirrors ItemFeeManager.updateFee() logic.
     */
    private void recalculateServiceTotal(Service service) {
        List<ItemFee> fees = fetchFeesForItem(service);
        double total = 0.0;
        double totalForForeigner = 0.0;
        for (ItemFee fee : fees) {
            total += fee.getFee();
            totalForForeigner += fee.getFfee();
        }
        service.setTotal(total);
        service.setTotalForForeigner(totalForForeigner);
        saveService(service);
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
     * Build a ServiceResponseDTO from a Service entity and its fees.
     */
    private ServiceResponseDTO buildServiceResponseDTO(Service service, List<ItemFee> fees, String message) {
        ServiceResponseDTO dto = new ServiceResponseDTO();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setCode(service.getCode());
        dto.setPrintName(service.getPrintName());
        dto.setFullName(service.getFullName());
        dto.setServiceType(service instanceof InwardService ? "Inward" : "OPD");
        dto.setTotal(service.getTotal());
        dto.setTotalForForeigner(service.getTotalForForeigner());
        dto.setInactive(service.isInactive());
        dto.setRetired(service.isRetired());
        dto.setDiscountAllowed(service.isDiscountAllowed());
        dto.setUserChangable(service.isUserChangable());
        dto.setChargesVisibleForInward(service.isChargesVisibleForInward());
        dto.setMarginNotAllowed(service.isMarginNotAllowed());
        dto.setRequestForQuentity(service.isRequestForQuentity());
        dto.setPatientNotRequired(service.isPatientNotRequired());
        if (service.getInwardChargeType() != null) {
            dto.setInwardChargeType(service.getInwardChargeType().name());
        }
        if (service.getCategory() != null) {
            dto.setCategoryId(service.getCategory().getId());
            dto.setCategoryName(service.getCategory().getName());
        }
        if (service.getInstitution() != null) {
            dto.setInstitutionId(service.getInstitution().getId());
            dto.setInstitutionName(service.getInstitution().getName());
        }
        if (service.getDepartment() != null) {
            dto.setDepartmentId(service.getDepartment().getId());
            dto.setDepartmentName(service.getDepartment().getName());
        }
        dto.setCreatedAt(service.getCreatedAt());

        List<ItemFeeDTO> feeDtos = new ArrayList<>();
        for (ItemFee fee : fees) {
            feeDtos.add(buildItemFeeDTO(fee));
        }
        dto.setFees(feeDtos);
        dto.setMessage(message);
        return dto;
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
