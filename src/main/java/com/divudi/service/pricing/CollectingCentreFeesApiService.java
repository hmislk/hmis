/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pricing;

import com.divudi.core.data.FeeType;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.dto.pricing.CollectingCentreFeeCreateRequestDTO;
import com.divudi.core.data.dto.pricing.CollectingCentreFeeDTO;
import com.divudi.core.data.dto.service.ItemFeeUpdateRequestDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.ItemFee;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.ItemFeeFacade;
import com.divudi.core.facade.SpecialityFacade;
import com.divudi.core.facade.StaffFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for Collecting Centre Fees API operations.
 * Provides business logic for listing, creating, updating, retiring,
 * and recalculating item fees associated with a specific collecting centre.
 *
 * Fees are identified by ItemFee.forInstitution pointing to an Institution
 * with institutionType = CollectingCentre, and ItemFee.forCategory = null.
 */
@Stateless
public class CollectingCentreFeesApiService implements Serializable {

    @EJB
    private ItemFeeFacade itemFeeFacade;

    @EJB
    private InstitutionFacade institutionFacade;

    @EJB
    private ItemFacade itemFacade;

    @EJB
    private DepartmentFacade departmentFacade;

    @EJB
    private SpecialityFacade specialityFacade;

    @EJB
    private StaffFacade staffFacade;

    // =========================================================================
    // List
    // =========================================================================

    /**
     * List all active (non-retired) fees for the given collecting centre.
     * Optionally filter by item name/code query.
     */
    public List<CollectingCentreFeeDTO> listFeesForCollectingCentre(
            Long institutionId, String query, int limit) throws Exception {

        Institution cc = loadAndValidateCollectingCentre(institutionId);

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("cc", cc);

        StringBuilder jpql = new StringBuilder(
                "SELECT f FROM ItemFee f "
                + "WHERE f.retired = :ret "
                + "AND f.forInstitution = :cc "
                + "AND f.forCategory IS NULL ");

        if (query != null && !query.trim().isEmpty()) {
            jpql.append("AND (UPPER(f.item.name) LIKE :q OR UPPER(f.item.code) LIKE :q) ");
            params.put("q", "%" + query.trim().toUpperCase() + "%");
        }

        jpql.append("ORDER BY f.item.name, f.name");

        List<ItemFee> fees = itemFeeFacade.findByJpql(jpql.toString(), params, limit);
        List<CollectingCentreFeeDTO> dtos = new ArrayList<>();
        for (ItemFee fee : fees) {
            dtos.add(buildDTO(fee));
        }
        return dtos;
    }

    // =========================================================================
    // Create
    // =========================================================================

    /**
     * Create a new collecting centre fee.
     * The collecting centre is set via forInstitution; forCategory is left null.
     */
    public CollectingCentreFeeDTO addFee(CollectingCentreFeeCreateRequestDTO request, WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid request is required: collectingCentreId, itemId, name, feeType, and fee are all required");
        }

        Institution cc = loadAndValidateCollectingCentre(request.getCollectingCentreId());

        Item item = itemFacade.find(request.getItemId());
        if (item == null) {
            throw new Exception("Item not found with ID: " + request.getItemId());
        }
        if (item.isRetired()) {
            throw new Exception("Item with ID " + request.getItemId() + " is retired");
        }

        FeeType feeType;
        try {
            feeType = FeeType.valueOf(request.getFeeType().trim());
        } catch (IllegalArgumentException e) {
            throw new Exception("Invalid feeType: " + request.getFeeType());
        }

        // Department is required for institution-linked fee types
        boolean requiresDepartment = feeType == FeeType.OtherInstitution
                || feeType == FeeType.OwnInstitution
                || feeType == FeeType.Referral;

        ItemFee itemFee = new ItemFee();
        itemFee.setName(request.getName().trim());
        itemFee.setFeeType(feeType);
        itemFee.setFee(request.getFee());
        itemFee.setFfee(request.getFfee() != null && request.getFfee() > 0
                ? request.getFfee() : request.getFee());
        itemFee.setDiscountAllowed(request.isDiscountAllowed());
        itemFee.setForInstitution(cc);
        itemFee.setItem(item);
        itemFee.setCreater(user);
        itemFee.setCreatedAt(Calendar.getInstance().getTime());

        if (request.getDepartmentId() != null) {
            Department dept = departmentFacade.find(request.getDepartmentId());
            if (dept == null) {
                throw new Exception("Department not found with ID: " + request.getDepartmentId());
            }
            itemFee.setDepartment(dept);
        } else if (requiresDepartment) {
            throw new Exception("departmentId is required for feeType: " + feeType.name());
        }

        if (request.getInstitutionId() != null) {
            Institution inst = institutionFacade.find(request.getInstitutionId());
            if (inst == null) {
                throw new Exception("Institution not found with ID: " + request.getInstitutionId());
            }
            itemFee.setInstitution(inst);
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
        recalculateItemTotal(item);

        return buildDTO(itemFee);
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Update an existing collecting centre fee.
     * Only non-null fields in the request are applied.
     */
    public CollectingCentreFeeDTO updateFee(Long feeId, ItemFeeUpdateRequestDTO request, WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid update request is required (at least one field must be provided)");
        }

        ItemFee itemFee = loadAndValidateFee(feeId);

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
            itemFee.setFee(request.getFee());
        }
        if (request.getFfee() != null) {
            itemFee.setFfee(request.getFfee());
        }
        if (request.getDiscountAllowed() != null) {
            itemFee.setDiscountAllowed(request.getDiscountAllowed());
        }
        if (request.getDepartmentId() != null) {
            Department dept = departmentFacade.find(request.getDepartmentId());
            if (dept == null) {
                throw new Exception("Department not found with ID: " + request.getDepartmentId());
            }
            itemFee.setDepartment(dept);
        }
        if (request.getInstitutionId() != null) {
            Institution inst = institutionFacade.find(request.getInstitutionId());
            if (inst == null) {
                throw new Exception("Institution not found with ID: " + request.getInstitutionId());
            }
            itemFee.setInstitution(inst);
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

        if (itemFee.getItem() != null) {
            recalculateItemTotal(itemFee.getItem());
        }

        return buildDTO(itemFee);
    }

    // =========================================================================
    // Retire single fee
    // =========================================================================

    /**
     * Soft-retire a single collecting centre fee by ID.
     */
    public CollectingCentreFeeDTO retireFee(Long feeId, String retireComments, WebUser user) throws Exception {
        ItemFee itemFee = loadAndValidateFee(feeId);

        itemFee.setRetired(true);
        itemFee.setRetirer(user);
        itemFee.setRetiredAt(Calendar.getInstance().getTime());
        itemFee.setRetireComments(retireComments != null ? retireComments : "");
        itemFeeFacade.edit(itemFee);

        if (itemFee.getItem() != null) {
            recalculateItemTotal(itemFee.getItem());
        }

        return buildDTO(itemFee);
    }

    // =========================================================================
    // Retire all fees for a collecting centre
    // =========================================================================

    /**
     * Soft-retire ALL active fees for the given collecting centre.
     * Returns the count of fees retired.
     */
    public int retireAllFeesForCollectingCentre(Long institutionId, String retireComments, WebUser user) throws Exception {
        Institution cc = loadAndValidateCollectingCentre(institutionId);

        String jpql = "SELECT f FROM ItemFee f "
                + "WHERE f.retired = false "
                + "AND f.forInstitution = :cc "
                + "AND f.forCategory IS NULL";
        Map<String, Object> params = new HashMap<>();
        params.put("cc", cc);

        List<ItemFee> fees = itemFeeFacade.findByJpql(jpql, params);
        if (fees == null || fees.isEmpty()) {
            return 0;
        }

        Set<Item> affectedItems = new HashSet<>();
        String comment = retireComments != null ? retireComments : "";

        for (ItemFee fee : fees) {
            fee.setRetired(true);
            fee.setRetirer(user);
            fee.setRetiredAt(Calendar.getInstance().getTime());
            fee.setRetireComments(comment);
            itemFeeFacade.edit(fee);
            if (fee.getItem() != null) {
                affectedItems.add(fee.getItem());
            }
        }

        for (Item item : affectedItems) {
            recalculateItemTotal(item);
        }

        return fees.size();
    }

    // =========================================================================
    // Recalculate item totals
    // =========================================================================

    /**
     * Recalculate the total and totalForForeigner for all items that have
     * at least one fee (active or retired) linked to the given collecting centre.
     * This is useful after a bulk retirement to refresh item totals.
     * Returns the number of items recalculated.
     */
    public int recalculateItemTotalsForCollectingCentre(Long institutionId) throws Exception {
        Institution cc = loadAndValidateCollectingCentre(institutionId);

        // Fetch distinct items that ever had a fee for this CC (including retired fees)
        String jpql = "SELECT DISTINCT f.item FROM ItemFee f "
                + "WHERE f.forInstitution = :cc "
                + "AND f.forCategory IS NULL "
                + "AND f.item IS NOT NULL";
        Map<String, Object> params = new HashMap<>();
        params.put("cc", cc);

        List<Item> items = itemFacade.findByJpql(jpql, params);
        if (items == null || items.isEmpty()) {
            return 0;
        }

        for (Item item : items) {
            recalculateItemTotal(item);
        }

        return items.size();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Institution loadAndValidateCollectingCentre(Long institutionId) throws Exception {
        if (institutionId == null) {
            throw new Exception("institutionId is required");
        }
        Institution cc = institutionFacade.find(institutionId);
        if (cc == null) {
            throw new Exception("Institution not found with ID: " + institutionId);
        }
        if (cc.getInstitutionType() != InstitutionType.CollectingCentre) {
            throw new Exception("Institution with ID " + institutionId + " is not a collecting centre");
        }
        return cc;
    }

    private ItemFee loadAndValidateFee(Long feeId) throws Exception {
        if (feeId == null) {
            throw new Exception("feeId is required");
        }
        ItemFee fee = itemFeeFacade.find(feeId);
        if (fee == null) {
            throw new Exception("Fee not found with ID: " + feeId);
        }
        if (fee.isRetired()) {
            throw new Exception("Fee with ID " + feeId + " is already retired");
        }
        return fee;
    }

    /**
     * Recalculate an item's total and totalForForeigner by summing its active fees.
     * Mirrors ItemFeeManager.updateFee() logic.
     */
    private void recalculateItemTotal(Item item) {
        String jpql = "SELECT f FROM ItemFee f WHERE f.item = :item AND f.retired = false";
        Map<String, Object> params = new HashMap<>();
        params.put("item", item);

        List<ItemFee> activeFees = itemFeeFacade.findByJpql(jpql, params);
        double total = 0.0;
        double totalForForeigner = 0.0;
        for (ItemFee fee : activeFees) {
            total += fee.getFee();
            totalForForeigner += fee.getFfee();
        }
        item.setTotal(total);
        item.setTotalForForeigner(totalForForeigner);
        itemFacade.edit(item);
    }

    private CollectingCentreFeeDTO buildDTO(ItemFee fee) {
        CollectingCentreFeeDTO dto = new CollectingCentreFeeDTO();
        dto.setId(fee.getId());
        dto.setName(fee.getName());
        dto.setFeeType(fee.getFeeType() != null ? fee.getFeeType().name() : null);
        dto.setFee(fee.getFee());
        dto.setFfee(fee.getFfee());
        dto.setDiscountAllowed(fee.isDiscountAllowed());
        dto.setRetired(fee.isRetired());

        if (fee.getForInstitution() != null) {
            dto.setCollectingCentreId(fee.getForInstitution().getId());
            dto.setCollectingCentreName(fee.getForInstitution().getName());
            dto.setCollectingCentreCode(fee.getForInstitution().getCode());
        }
        if (fee.getItem() != null) {
            dto.setItemId(fee.getItem().getId());
            dto.setItemName(fee.getItem().getName());
            dto.setItemCode(fee.getItem().getCode());
        }
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
}
