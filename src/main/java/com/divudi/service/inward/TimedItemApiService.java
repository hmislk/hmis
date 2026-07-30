/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.inward;

import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.timeditem.TimedItemCreateRequestDTO;
import com.divudi.core.data.dto.timeditem.TimedItemFeeCreateRequestDTO;
import com.divudi.core.data.dto.timeditem.TimedItemFeeDTO;
import com.divudi.core.data.dto.timeditem.TimedItemFeeUpdateRequestDTO;
import com.divudi.core.data.dto.timeditem.TimedItemResponseDTO;
import com.divudi.core.data.dto.timeditem.TimedItemSearchResultDTO;
import com.divudi.core.data.dto.timeditem.TimedItemUpdateRequestDTO;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.TimedItem;
import com.divudi.core.entity.inward.TimedItemCategory;
import com.divudi.core.entity.inward.TimedItemFee;
import com.divudi.core.facade.CategoryFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.TimedItemFacade;
import com.divudi.core.facade.TimedItemFeeFacade;
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
 * Business logic for Timed Item API.
 * Manages TimedItem entities and their TimedItemFee entries.
 *
 * @author Buddhika
 */
@Stateless
public class TimedItemApiService implements Serializable {

    @EJB
    private TimedItemFacade timedItemFacade;

    @EJB
    private TimedItemFeeFacade timedItemFeeFacade;

    @EJB
    private DepartmentFacade departmentFacade;

    @EJB
    private InstitutionFacade institutionFacade;

    @EJB
    private CategoryFacade categoryFacade;

    // =========================================================================
    // Search
    // =========================================================================

    public List<TimedItemSearchResultDTO> searchTimedItems(String query, String departmentType,
            Boolean inactive, int limit) throws Exception {

        Map<String, Object> params = new HashMap<>();

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT i FROM TimedItem i WHERE i.retired = false ");

        if (query != null && !query.trim().isEmpty()) {
            jpql.append("AND (i.name LIKE :query OR i.code LIKE :query) ");
            params.put("query", "%" + query.trim() + "%");
        }

        if (departmentType != null && !departmentType.trim().isEmpty()) {
            jpql.append("AND i.departmentType = :departmentType ");
            try {
                params.put("departmentType", DepartmentType.valueOf(departmentType.trim()));
            } catch (IllegalArgumentException e) {
                throw new Exception("Invalid departmentType: " + departmentType);
            }
        }

        if (inactive != null) {
            jpql.append("AND i.inactive = :inactive ");
            params.put("inactive", inactive);
        }

        jpql.append("ORDER BY i.name");

        List<TimedItem> results = timedItemFacade.findByJpql(
                jpql.toString(), params, TemporalType.TIMESTAMP, limit);

        List<TimedItemSearchResultDTO> dtos = new ArrayList<>();
        for (TimedItem item : results) {
            dtos.add(buildSearchResultDTO(item));
        }
        return dtos;
    }

    // =========================================================================
    // Get by ID
    // =========================================================================

    public TimedItemResponseDTO findTimedItemById(Long id) throws Exception {
        TimedItem item = loadAndValidate(id);
        List<TimedItemFee> fees = fetchFees(item);
        return buildResponseDTO(item, fees, "TimedItem found successfully");
    }

    // =========================================================================
    // Create
    // =========================================================================

    public TimedItemResponseDTO createTimedItem(TimedItemCreateRequestDTO request, WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid create request required (name, departmentType, inwardChargeType are required)");
        }

        DepartmentType deptType;
        try {
            deptType = DepartmentType.valueOf(request.getDepartmentType().trim());
        } catch (IllegalArgumentException e) {
            throw new Exception("Invalid departmentType: " + request.getDepartmentType());
        }

        InwardChargeType chargeType;
        try {
            chargeType = InwardChargeType.valueOf(request.getInwardChargeType().trim());
        } catch (IllegalArgumentException e) {
            throw new Exception("Invalid inwardChargeType: " + request.getInwardChargeType());
        }

        TimedItem item = new TimedItem();
        item.setName(request.getName().trim());
        item.setCode(request.getCode() != null && !request.getCode().trim().isEmpty()
                ? request.getCode().trim()
                : CommonFunctions.nameToCode(request.getName()));

        if (request.getPrintName() != null) {
            item.setPrintName(request.getPrintName());
        }
        if (request.getFullName() != null) {
            item.setFullName(request.getFullName());
        }

        item.setDepartmentType(deptType);
        item.setInwardChargeType(chargeType);
        item.setInactive(request.isInactive());
        item.setRetired(false);

        if (request.getDepartmentId() != null) {
            Department dept = departmentFacade.find(request.getDepartmentId());
            if (dept == null) {
                throw new Exception("Department not found with ID: " + request.getDepartmentId());
            }
            item.setDepartment(dept);
        }

        if (request.getInstitutionId() != null) {
            Institution inst = institutionFacade.find(request.getInstitutionId());
            if (inst == null) {
                throw new Exception("Institution not found with ID: " + request.getInstitutionId());
            }
            item.setInstitution(inst);
        }

        if (request.getCategoryId() != null) {
            item.setCategory(resolveCategory(request.getCategoryId()));
        }

        item.setCreater(user);
        item.setCreatedAt(Calendar.getInstance().getTime());

        timedItemFacade.create(item);

        // Self-references set after initial persist (ID now available)
        item.setBilledAs(item);
        item.setReportedAs(item);
        timedItemFacade.edit(item);

        return buildResponseDTO(item, new ArrayList<>(), "TimedItem created successfully");
    }

    // =========================================================================
    // Update
    // =========================================================================

    public TimedItemResponseDTO updateTimedItem(Long id, TimedItemUpdateRequestDTO request, WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid update request required (at least one field must be provided)");
        }

        TimedItem item = loadAndValidate(id);

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            item.setName(request.getName().trim());
        }
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            item.setCode(request.getCode().trim());
        }
        if (request.getPrintName() != null) {
            item.setPrintName(request.getPrintName());
        }
        if (request.getFullName() != null) {
            item.setFullName(request.getFullName());
        }
        if (request.getDepartmentType() != null && !request.getDepartmentType().trim().isEmpty()) {
            try {
                item.setDepartmentType(DepartmentType.valueOf(request.getDepartmentType().trim()));
            } catch (IllegalArgumentException e) {
                throw new Exception("Invalid departmentType: " + request.getDepartmentType());
            }
        }
        if (request.getInwardChargeType() != null && !request.getInwardChargeType().trim().isEmpty()) {
            try {
                item.setInwardChargeType(InwardChargeType.valueOf(request.getInwardChargeType().trim()));
            } catch (IllegalArgumentException e) {
                throw new Exception("Invalid inwardChargeType: " + request.getInwardChargeType());
            }
        }
        if (request.getInactive() != null) {
            item.setInactive(request.getInactive());
        }
        if (request.getDepartmentId() != null) {
            Department dept = departmentFacade.find(request.getDepartmentId());
            if (dept == null) {
                throw new Exception("Department not found with ID: " + request.getDepartmentId());
            }
            item.setDepartment(dept);
        }
        if (request.getInstitutionId() != null) {
            Institution inst = institutionFacade.find(request.getInstitutionId());
            if (inst == null) {
                throw new Exception("Institution not found with ID: " + request.getInstitutionId());
            }
            item.setInstitution(inst);
        }
        if (request.getCategoryId() != null) {
            item.setCategory(resolveCategory(request.getCategoryId()));
        }

        item.setEditer(user);
        item.setEditedAt(Calendar.getInstance().getTime());
        timedItemFacade.edit(item);

        List<TimedItemFee> fees = fetchFees(item);
        return buildResponseDTO(item, fees, "TimedItem updated successfully");
    }

    // =========================================================================
    // Retire / Activate / Deactivate
    // =========================================================================

    public TimedItemResponseDTO retireTimedItem(Long id, String retireComments, WebUser user) throws Exception {
        TimedItem item = loadAndValidate(id);
        item.setRetired(true);
        item.setRetirer(user);
        item.setRetiredAt(Calendar.getInstance().getTime());
        item.setRetireComments(retireComments);
        timedItemFacade.edit(item);
        return buildResponseDTO(item, new ArrayList<>(), "TimedItem retired successfully");
    }

    public TimedItemResponseDTO activateTimedItem(Long id, WebUser user) throws Exception {
        TimedItem item = loadAndValidate(id);
        item.setInactive(false);
        item.setEditer(user);
        item.setEditedAt(Calendar.getInstance().getTime());
        timedItemFacade.edit(item);
        return buildResponseDTO(item, new ArrayList<>(), "TimedItem activated successfully");
    }

    public TimedItemResponseDTO deactivateTimedItem(Long id, WebUser user) throws Exception {
        TimedItem item = loadAndValidate(id);
        item.setInactive(true);
        item.setEditer(user);
        item.setEditedAt(Calendar.getInstance().getTime());
        timedItemFacade.edit(item);
        return buildResponseDTO(item, new ArrayList<>(), "TimedItem deactivated successfully");
    }

    // =========================================================================
    // Fee Management
    // =========================================================================

    public List<TimedItemFeeDTO> listFees(Long timedItemId) throws Exception {
        TimedItem item = loadAndValidate(timedItemId);
        List<TimedItemFee> fees = fetchFees(item);
        List<TimedItemFeeDTO> dtos = new ArrayList<>();
        for (TimedItemFee fee : fees) {
            dtos.add(buildFeeDTO(fee));
        }
        return dtos;
    }

    public TimedItemResponseDTO addFee(Long timedItemId, TimedItemFeeCreateRequestDTO request, WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid fee request required (name is required)");
        }
        if (request.getFee() < 0) {
            throw new Exception("Fee must be a non-negative value");
        }

        TimedItem item = loadAndValidate(timedItemId);

        TimedItemFee fee = new TimedItemFee();
        fee.setItem(item);
        fee.setName(request.getName().trim());
        fee.setFee(request.getFee());
        fee.setFfee(request.getFfee() > 0 ? request.getFfee() : request.getFee());
        fee.setDurationHours(request.getDurationHours());
        fee.setOverShootHours(request.getOverShootHours());
        fee.setDurationDaysForMoCharge(request.getDurationDaysForMoCharge());
        fee.setSortOrder(request.getSortOrder());
        fee.setRepeating(request.isRepeating());
        fee.setCreater(user);
        fee.setCreatedAt(Calendar.getInstance().getTime());
        fee.setRetired(false);

        timedItemFeeFacade.create(fee);

        recalculateTotal(item);

        List<TimedItemFee> fees = fetchFees(item);
        return buildResponseDTO(item, fees, "Fee added successfully");
    }

    public TimedItemResponseDTO updateFee(Long timedItemId, Long feeId,
            TimedItemFeeUpdateRequestDTO request, WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid update request required (at least one field must be provided)");
        }

        TimedItem item = loadAndValidate(timedItemId);
        TimedItemFee fee = loadAndValidateFee(feeId, item);

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            fee.setName(request.getName().trim());
        }
        if (request.getFee() != null) {
            if (request.getFee() < 0) {
                throw new Exception("Fee must be a non-negative value");
            }
            fee.setFee(request.getFee());
        }
        if (request.getFfee() != null) {
            if (request.getFfee() < 0) {
                throw new Exception("Foreigner fee must be a non-negative value");
            }
            fee.setFfee(request.getFfee());
        }
        if (request.getDurationHours() != null) {
            fee.setDurationHours(request.getDurationHours());
        }
        if (request.getOverShootHours() != null) {
            fee.setOverShootHours(request.getOverShootHours());
        }
        if (request.getDurationDaysForMoCharge() != null) {
            fee.setDurationDaysForMoCharge(request.getDurationDaysForMoCharge());
        }
        if (request.getSortOrder() != null) {
            fee.setSortOrder(request.getSortOrder());
        }
        if (request.getRepeating() != null) {
            fee.setRepeating(request.getRepeating());
        }

        fee.setEditer(user);
        fee.setEditedAt(Calendar.getInstance().getTime());
        timedItemFeeFacade.edit(fee);

        recalculateTotal(item);

        List<TimedItemFee> fees = fetchFees(item);
        return buildResponseDTO(item, fees, "Fee updated successfully");
    }

    public TimedItemResponseDTO removeFee(Long timedItemId, Long feeId, WebUser user) throws Exception {
        TimedItem item = loadAndValidate(timedItemId);
        TimedItemFee fee = loadAndValidateFee(feeId, item);

        fee.setRetired(true);
        fee.setRetirer(user);
        fee.setRetiredAt(Calendar.getInstance().getTime());
        timedItemFeeFacade.edit(fee);

        recalculateTotal(item);

        List<TimedItemFee> fees = fetchFees(item);
        return buildResponseDTO(item, fees, "Fee removed successfully");
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private TimedItemCategory resolveCategory(Long categoryId) throws Exception {
        Category category = categoryFacade.find(categoryId);
        if (category == null || !(category instanceof TimedItemCategory) || category.isRetired()) {
            throw new Exception("TimedItemCategory not found with ID: " + categoryId);
        }
        return (TimedItemCategory) category;
    }

    private TimedItem loadAndValidate(Long id) throws Exception {
        if (id == null) {
            throw new Exception("TimedItem ID is required");
        }
        TimedItem item = timedItemFacade.find(id);
        if (item == null) {
            throw new Exception("TimedItem not found with ID: " + id);
        }
        if (item.isRetired()) {
            throw new Exception("TimedItem with ID " + id + " is retired");
        }
        return item;
    }

    private TimedItemFee loadAndValidateFee(Long feeId, TimedItem item) throws Exception {
        if (feeId == null) {
            throw new Exception("Fee ID is required");
        }
        TimedItemFee fee = timedItemFeeFacade.find(feeId);
        if (fee == null) {
            throw new Exception("TimedItemFee not found with ID: " + feeId);
        }
        if (fee.isRetired()) {
            throw new Exception("TimedItemFee with ID " + feeId + " is already retired");
        }
        if (fee.getItem() == null || !fee.getItem().getId().equals(item.getId())) {
            throw new Exception("Fee with ID " + feeId + " does not belong to TimedItem with ID " + item.getId());
        }
        return fee;
    }

    private List<TimedItemFee> fetchFees(TimedItem item) {
        String jpql = "SELECT f FROM TimedItemFee f WHERE f.item = :item AND f.retired = false ORDER BY f.sortOrder, f.id";
        Map<String, Object> params = new HashMap<>();
        params.put("item", item);
        return timedItemFeeFacade.findByJpql(jpql, params);
    }

    private void recalculateTotal(TimedItem item) {
        // TimedItem total reflects the first (base) non-retired fee for display purposes.
        // Actual billing uses calTotalTimedChargeForItem() which evaluates all fee tiers.
        List<TimedItemFee> fees = fetchFees(item);
        double total = 0.0;
        double totalForForeigner = 0.0;
        if (!fees.isEmpty()) {
            TimedItemFee first = fees.get(0);
            total = first.getFee();
            totalForForeigner = first.getFfee() > 0 ? first.getFfee() : first.getFee();
        }
        item.setTotal(total);
        item.setTotalForForeigner(totalForForeigner);
        timedItemFacade.edit(item);
    }

    private TimedItemSearchResultDTO buildSearchResultDTO(Item item) {
        TimedItemSearchResultDTO dto = new TimedItemSearchResultDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setCode(item.getCode());
        dto.setPrintName(item.getPrintName());
        dto.setTotal(item.getTotal());
        dto.setTotalForForeigner(item.getTotalForForeigner());
        dto.setInactive(item.isInactive());
        if (item.getDepartmentType() != null) {
            dto.setDepartmentType(item.getDepartmentType().name());
        }
        if (item.getInwardChargeType() != null) {
            dto.setInwardChargeType(item.getInwardChargeType().name());
        }
        return dto;
    }

    private TimedItemResponseDTO buildResponseDTO(TimedItem item, List<TimedItemFee> fees, String message) {
        TimedItemResponseDTO dto = new TimedItemResponseDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setCode(item.getCode());
        dto.setPrintName(item.getPrintName());
        dto.setFullName(item.getFullName());
        dto.setTotal(item.getTotal());
        dto.setTotalForForeigner(item.getTotalForForeigner());
        dto.setInactive(item.isInactive());
        dto.setRetired(item.isRetired());
        if (item.getDepartmentType() != null) {
            dto.setDepartmentType(item.getDepartmentType().name());
        }
        if (item.getInwardChargeType() != null) {
            dto.setInwardChargeType(item.getInwardChargeType().name());
        }
        if (item.getDepartment() != null) {
            dto.setDepartmentId(item.getDepartment().getId());
            dto.setDepartmentName(item.getDepartment().getName());
        }
        if (item.getInstitution() != null) {
            dto.setInstitutionId(item.getInstitution().getId());
            dto.setInstitutionName(item.getInstitution().getName());
        }
        if (item.getCategory() != null) {
            dto.setCategoryId(item.getCategory().getId());
            dto.setCategoryName(item.getCategory().getName());
        }
        dto.setCreatedAt(item.getCreatedAt());

        List<TimedItemFeeDTO> feeDtos = new ArrayList<>();
        for (TimedItemFee fee : fees) {
            feeDtos.add(buildFeeDTO(fee));
        }
        dto.setFees(feeDtos);
        dto.setMessage(message);
        return dto;
    }

    private TimedItemFeeDTO buildFeeDTO(TimedItemFee fee) {
        TimedItemFeeDTO dto = new TimedItemFeeDTO();
        dto.setId(fee.getId());
        dto.setName(fee.getName());
        dto.setFee(fee.getFee());
        dto.setFfee(fee.getFfee());
        dto.setDurationHours(fee.getDurationHours());
        dto.setOverShootHours(fee.getOverShootHours());
        dto.setDurationDaysForMoCharge(fee.getDurationDaysForMoCharge());
        dto.setSortOrder(fee.getSortOrder());
        dto.setRepeating(fee.isRepeating());
        dto.setRetired(fee.isRetired());
        return dto;
    }
}
