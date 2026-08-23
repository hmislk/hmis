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
import com.divudi.core.data.dto.timeditem.TimedItemFeeBulkRequestDTO;
import com.divudi.core.data.dto.timeditem.TimedItemFeeUpsertDTO;
import com.divudi.core.data.dto.timeditem.TimedItemResponseDTO;
import com.divudi.core.data.dto.timeditem.TimedItemSearchPageDTO;
import com.divudi.core.data.dto.timeditem.TimedItemSearchResultDTO;
import com.divudi.core.data.dto.timeditem.TimedItemUpdateRequestDTO;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.WebUser;
import com.divudi.core.data.inward.TimedItemDurationUnit;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @EJB
    private TimedItemFeeRules timedItemFeeRules;

    // =========================================================================
    // Search
    // =========================================================================

    /**
     * Search timed items, returning one page plus the total number of matches.
     *
     * <p>Every filter here is a field this same API sets on create, so a caller can find
     * a record again by whatever it used to configure it. {@code includeRetired} exists
     * because the retire/restore round trip is otherwise blind — a retired item is
     * invisible to every other read path (issue #23236 §2, §4).
     */
    public TimedItemSearchPageDTO searchTimedItems(String query, String departmentType,
            Boolean inactive, Long categoryId, String inwardChargeType, Long departmentId,
            Long institutionId, boolean includeRetired, int limit, int offset) throws Exception {

        Map<String, Object> params = new HashMap<>();

        StringBuilder where = new StringBuilder();
        where.append(" WHERE 1 = 1 ");

        if (!includeRetired) {
            where.append("AND i.retired = false ");
        }

        if (query != null && !query.trim().isEmpty()) {
            // Upper-cased on both sides so matching is defined here rather than by the
            // deployment's collation — the same thing listCategories and the UI
            // autocomplete already do.
            where.append("AND (upper(i.name) LIKE :query OR upper(i.code) LIKE :query) ");
            params.put("query", "%" + query.trim().toUpperCase() + "%");
        }

        if (departmentType != null && !departmentType.trim().isEmpty()) {
            where.append("AND i.departmentType = :departmentType ");
            try {
                params.put("departmentType", DepartmentType.valueOf(departmentType.trim()));
            } catch (IllegalArgumentException e) {
                throw new Exception("Invalid departmentType: " + departmentType);
            }
        }

        if (inwardChargeType != null && !inwardChargeType.trim().isEmpty()) {
            where.append("AND i.inwardChargeType = :inwardChargeType ");
            try {
                params.put("inwardChargeType", InwardChargeType.valueOf(inwardChargeType.trim()));
            } catch (IllegalArgumentException e) {
                throw new Exception("Invalid inwardChargeType: " + inwardChargeType);
            }
        }

        if (inactive != null) {
            where.append("AND i.inactive = :inactive ");
            params.put("inactive", inactive);
        }

        if (categoryId != null) {
            where.append("AND i.category.id = :categoryId ");
            params.put("categoryId", categoryId);
        }

        if (departmentId != null) {
            where.append("AND i.department.id = :departmentId ");
            params.put("departmentId", departmentId);
        }

        if (institutionId != null) {
            where.append("AND i.institution.id = :institutionId ");
            params.put("institutionId", institutionId);
        }

        // COUNT returns a Long — findLongByJpql, never findDoubleByJpql (which would
        // swallow the ClassCastException and report 0 every time).
        long total = timedItemFacade.findLongByJpql(
                "SELECT COUNT(i) FROM TimedItem i" + where, params);

        List<TimedItem> results = timedItemFacade.findByJpqlWithRange(
                "SELECT i FROM TimedItem i" + where + "ORDER BY i.name, i.id",
                params, offset, limit);

        List<TimedItemSearchResultDTO> dtos = new ArrayList<>();
        for (TimedItem item : results) {
            dtos.add(buildSearchResultDTO(item));
        }
        return new TimedItemSearchPageDTO(dtos, total, limit, offset);
    }

    // =========================================================================
    // Get by ID
    // =========================================================================

    public TimedItemResponseDTO findTimedItemById(Long id) throws Exception {
        return findTimedItemById(id, false);
    }

    /**
     * @param includeRetired read a retired item (and its retired fees) instead of
     *                       rejecting it, so an agent can review what it retired before
     *                       deciding whether to restore it
     */
    public TimedItemResponseDTO findTimedItemById(Long id, boolean includeRetired) throws Exception {
        TimedItem item = includeRetired ? loadAllowingRetired(id) : loadAndValidate(id);
        List<TimedItemFee> fees = fetchFees(item, includeRetired);
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

    /**
     * Undo a retire. Without this, an agent that retires the wrong service cannot put it
     * back through the API at all — recovery needs the UI or direct database access
     * (issue #23236 §2).
     */
    public TimedItemResponseDTO restoreTimedItem(Long id, WebUser user) throws Exception {
        TimedItem item = loadAllowingRetired(id);
        if (!item.isRetired()) {
            throw new Exception("TimedItem with ID " + id + " is not retired");
        }
        item.setRetired(false);
        item.setRetiredAt(null);
        item.setRetirer(null);
        item.setRetireComments(null);
        item.setEditer(user);
        item.setEditedAt(Calendar.getInstance().getTime());
        timedItemFacade.edit(item);

        // Fees retired alongside the item stay retired: restoring the service should not
        // silently resurrect slot configuration the user may have removed on purpose.
        List<TimedItemFee> fees = fetchFees(item);
        return buildResponseDTO(item, fees, "TimedItem restored successfully");
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
        return listFees(timedItemId, false);
    }

    public List<TimedItemFeeDTO> listFees(Long timedItemId, boolean includeRetired) throws Exception {
        TimedItem item = includeRetired ? loadAllowingRetired(timedItemId) : loadAndValidate(timedItemId);
        List<TimedItemFee> fees = fetchFees(item, includeRetired);
        List<TimedItemFeeDTO> dtos = new ArrayList<>();
        for (TimedItemFee fee : fees) {
            dtos.add(buildFeeDTO(fee));
        }
        return dtos;
    }

    public TimedItemResponseDTO addFee(Long timedItemId, TimedItemFeeCreateRequestDTO request, WebUser user) throws Exception {
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
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
        // Omitted by older clients. Store the explicit HOUR default rather than
        // leaving the column null — that is what the fee page writes for a
        // UI-created fee, and it keeps new rows unambiguous. Reads still default
        // a null unit to HOUR for rows created before duration units existed.
        fee.setDurationUnit(request.getDurationUnit() != null
                ? request.getDurationUnit() : TimedItemDurationUnit.HOUR);

        // Same rules the fee page applies, from the same place, so an identical payload
        // is accepted or rejected identically on both surfaces (issue #23236 §3).
        fee.setSortOrder(timedItemFeeRules.applyToFee(fee, fetchFees(item)));

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
        if (request.getDurationUnit() != null) {
            fee.setDurationUnit(request.getDurationUnit());
        }

        // Validate the fee as it will be stored, not just the fields that arrived: a
        // payload that only switches the unit can still leave a zero-length block, and a
        // payload that only moves the slot order can still collide with a sibling.
        timedItemFeeRules.validateDuration(fee.getDurationUnit(), fee.getDurationHours());
        timedItemFeeRules.validateSlotOrder(fee.getSortOrder(), fetchFees(item), fee.getId());

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

    /**
     * Undo a fee retire. The parent must be live — restore the item first — because a
     * live fee hanging off a retired service is a state neither surface can show.
     */
    public TimedItemResponseDTO restoreFee(Long timedItemId, Long feeId, WebUser user) throws Exception {
        TimedItem item = loadAndValidate(timedItemId);

        if (feeId == null) {
            throw new Exception("Fee ID is required");
        }
        TimedItemFee fee = timedItemFeeFacade.find(feeId);
        if (fee == null) {
            throw new Exception("TimedItemFee not found with ID: " + feeId);
        }
        if (fee.getItem() == null || !fee.getItem().getId().equals(item.getId())) {
            throw new Exception("Fee with ID " + feeId + " does not belong to TimedItem with ID " + item.getId());
        }
        if (!fee.isRetired()) {
            throw new Exception("TimedItemFee with ID " + feeId + " is not retired");
        }

        // The slot this fee used to occupy may have been taken while it was retired.
        timedItemFeeRules.validateSlotOrder(fee.getSortOrder(), fetchFees(item), fee.getId());

        fee.setRetired(false);
        fee.setRetiredAt(null);
        fee.setRetirer(null);
        fee.setRetireComments(null);
        fee.setEditer(user);
        fee.setEditedAt(Calendar.getInstance().getTime());
        timedItemFeeFacade.edit(fee);

        recalculateTotal(item);

        List<TimedItemFee> fees = fetchFees(item);
        return buildResponseDTO(item, fees, "Fee restored successfully");
    }

    /**
     * Replace a timed item's whole slot list in one call.
     *
     * <p>Validating the complete set together is the point: slot-order uniqueness can only
     * really be checked against every slot at once, and the parent's total is recalculated
     * once instead of once per round trip (issue #23236 §4).
     *
     * <p>Live fees whose id is absent from the payload are retired.
     */
    public TimedItemResponseDTO replaceFees(Long timedItemId, TimedItemFeeBulkRequestDTO request,
            WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new TimedItemFeeRuleException(
                    "A fees array is required; send [] to clear all slots");
        }

        TimedItem item = loadAndValidate(timedItemId);
        List<TimedItemFee> existing = fetchFees(item);

        Map<Long, TimedItemFee> existingById = new HashMap<>();
        for (TimedItemFee f : existing) {
            existingById.put(f.getId(), f);
        }

        // Retired fees are tracked only to tell "this id is not yours" apart from "this id
        // is yours but retired" — a bulk write operates on the live slot list, so naming a
        // retired fee is a mistake worth reporting accurately rather than as a wrong owner.
        Set<Long> retiredIds = new HashSet<>();
        for (TimedItemFee f : fetchFees(item, true)) {
            if (f.isRetired()) {
                retiredIds.add(f.getId());
            }
        }

        // Validate the entire submitted set before writing anything, so a bad slot in the
        // middle of the list does not leave the service half-reconfigured.
        Set<Integer> seenSlots = new HashSet<>();
        List<Integer> resolvedSlots = new ArrayList<>();
        int nextAutoSlot = 0;
        for (TimedItemFeeUpsertDTO row : request.getFees()) {
            if (row == null || row.getName() == null || row.getName().trim().isEmpty()) {
                throw new TimedItemFeeRuleException("Every fee in the list requires a name");
            }
            if (row.getFee() < 0) {
                throw new TimedItemFeeRuleException("Fee must be a non-negative value");
            }
            if (row.getId() != null && !existingById.containsKey(row.getId())) {
                if (retiredIds.contains(row.getId())) {
                    throw new TimedItemFeeRuleException("Fee with ID " + row.getId()
                            + " is retired. Restore it first, or omit the id to add a new slot.");
                }
                throw new TimedItemFeeRuleException("Fee with ID " + row.getId()
                        + " does not belong to TimedItem with ID " + item.getId());
            }
            timedItemFeeRules.validateDuration(row.getDurationUnit(), row.getDurationHours());

            int slot = row.getSortOrder();
            if (slot == 0) {
                // Auto-assign above every slot in the submitted set, not just the stored
                // ones — the set being written is what the uniqueness rule applies to.
                if (nextAutoSlot == 0) {
                    nextAutoSlot = highestSubmittedSlot(request.getFees());
                }
                slot = ++nextAutoSlot;
            }
            if (slot < 1) {
                throw new TimedItemFeeRuleException("Slot Order must be 1 or greater.");
            }
            if (!seenSlots.add(slot)) {
                throw new TimedItemFeeRuleException("Slot Order must be unique per service.");
            }
            resolvedSlots.add(slot);
        }

        Date now = Calendar.getInstance().getTime();
        Set<Long> submittedIds = new HashSet<>();

        for (int i = 0; i < request.getFees().size(); i++) {
            TimedItemFeeUpsertDTO row = request.getFees().get(i);
            boolean isNew = row.getId() == null;
            TimedItemFee fee = isNew ? new TimedItemFee() : existingById.get(row.getId());

            fee.setItem(item);
            fee.setName(row.getName().trim());
            fee.setFee(row.getFee());
            fee.setFfee(row.getFfee() > 0 ? row.getFfee() : row.getFee());
            fee.setDurationHours(row.getDurationHours());
            fee.setOverShootHours(row.getOverShootHours());
            fee.setDurationDaysForMoCharge(row.getDurationDaysForMoCharge());
            fee.setSortOrder(resolvedSlots.get(i));
            fee.setRepeating(row.isRepeating());
            fee.setDurationUnit(row.getDurationUnit() != null
                    ? row.getDurationUnit() : TimedItemDurationUnit.HOUR);

            if (isNew) {
                fee.setRetired(false);
                fee.setCreater(user);
                fee.setCreatedAt(now);
                timedItemFeeFacade.create(fee);
            } else {
                fee.setEditer(user);
                fee.setEditedAt(now);
                timedItemFeeFacade.edit(fee);
                submittedIds.add(fee.getId());
            }
        }

        for (TimedItemFee f : existing) {
            if (!submittedIds.contains(f.getId())) {
                f.setRetired(true);
                f.setRetirer(user);
                f.setRetiredAt(now);
                f.setRetireComments("Removed by bulk fee update");
                timedItemFeeFacade.edit(f);
            }
        }

        recalculateTotal(item);

        List<TimedItemFee> fees = fetchFees(item);
        return buildResponseDTO(item, fees, "Fees replaced successfully");
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private int highestSubmittedSlot(List<TimedItemFeeUpsertDTO> rows) {
        int max = 0;
        for (TimedItemFeeUpsertDTO r : rows) {
            if (r != null && r.getSortOrder() > max) {
                max = r.getSortOrder();
            }
        }
        return max;
    }

    private TimedItemCategory resolveCategory(Long categoryId) throws Exception {
        Category category = categoryFacade.find(categoryId);
        if (category == null || !(category instanceof TimedItemCategory) || category.isRetired()) {
            throw new Exception("TimedItemCategory not found with ID: " + categoryId);
        }
        return (TimedItemCategory) category;
    }

    private TimedItem loadAndValidate(Long id) throws Exception {
        TimedItem item = loadAllowingRetired(id);
        if (item.isRetired()) {
            throw new Exception("TimedItem with ID " + id + " is retired");
        }
        return item;
    }

    /** Load without rejecting a retired row — for reads that opted in, and for restore. */
    private TimedItem loadAllowingRetired(Long id) throws Exception {
        if (id == null) {
            throw new Exception("TimedItem ID is required");
        }
        TimedItem item = timedItemFacade.find(id);
        if (item == null) {
            throw new Exception("TimedItem not found with ID: " + id);
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
        return fetchFees(item, false);
    }

    private List<TimedItemFee> fetchFees(TimedItem item, boolean includeRetired) {
        String jpql = "SELECT f FROM TimedItemFee f WHERE f.item = :item "
                + (includeRetired ? "" : "AND f.retired = false ")
                + "ORDER BY f.sortOrder, f.id";
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
        dto.setRetired(item.isRetired());
        if (item.getDepartmentType() != null) {
            dto.setDepartmentType(item.getDepartmentType().name());
        }
        if (item.getInwardChargeType() != null) {
            dto.setInwardChargeType(item.getInwardChargeType().name());
        }
        if (item.getCategory() != null) {
            dto.setCategoryId(item.getCategory().getId());
            dto.setCategoryName(item.getCategory().getName());
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
        dto.setDurationUnit(fee.getDurationUnit());
        dto.setDurationDaysForMoCharge(fee.getDurationDaysForMoCharge());
        dto.setSortOrder(fee.getSortOrder());
        dto.setRepeating(fee.isRepeating());
        dto.setRetired(fee.isRetired());
        return dto;
    }
}
