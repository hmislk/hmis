/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.inward;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.admissioncharge.AdmissionChargeItemCreateRequestDTO;
import com.divudi.core.data.dto.admissioncharge.AdmissionChargeItemDTO;
import com.divudi.core.data.dto.admissioncharge.AdmissionChargeItemPageDTO;
import com.divudi.core.data.dto.admissioncharge.AdmissionChargeItemUpdateRequestDTO;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionChargeItem;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.AdmissionChargeItemFacade;
import com.divudi.core.facade.AdmissionTypeFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.ItemFeeFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Business logic for the Admission Charge Item API (issue #23594).
 *
 * <p>Validation here must match {@code AdmissionChargeApplicationBean}'s
 * runtime resolution exactly - a row this service accepts must be a row the
 * charging pipeline can actually bill:</p>
 * <ul>
 *   <li>the item must exist and carry a department, an institution and an
 *       inward charge type ({@code AdmissionChargeApplicationBean.buildEntry}
 *       dereferences all three unguarded)</li>
 *   <li>the item must have at least one live {@link com.divudi.core.entity.ItemFee}
 *       - an item with none produces a BillItem whose charge cancels and
 *       refunds as zero</li>
 *   <li>{@code paymentMethod}, when present, must be exactly {@code Cash} or
 *       {@code Credit} - the only two values an admission can hold (see
 *       {@code EnumController.getPaymentMethodForAdmission()})</li>
 *   <li>only one live row may exist per {@code (item, admissionType,
 *       paymentMethod)} triple</li>
 * </ul>
 *
 * <p>JPQL only, per project policy. {@code COUNT(...)} queries use
 * {@code findLongByJpql} - never {@code findDoubleByJpql}, which silently
 * {@code ClassCastException}s on the {@code Long} result and returns
 * {@code 0.0}, making every uniqueness/fee check pass by accident.</p>
 *
 * @author Buddhika
 */
@Stateless
public class AdmissionChargeApiService implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private AdmissionChargeItemFacade admissionChargeItemFacade;

    @EJB
    private ItemFacade itemFacade;

    @EJB
    private ItemFeeFacade itemFeeFacade;

    @EJB
    private AdmissionTypeFacade admissionTypeFacade;

    // =========================================================================
    // Search
    // =========================================================================

    /**
     * Search admission charge items, returning one page plus the total number
     * of matches.
     */
    public AdmissionChargeItemPageDTO search(Long itemId, Long admissionTypeId, String paymentMethod,
            boolean includeRetired, int limit, int offset) throws Exception {

        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");

        if (!includeRetired) {
            where.append("AND a.retired = false ");
        }
        if (itemId != null) {
            where.append("AND a.item.id = :itemId ");
            params.put("itemId", itemId);
        }
        if (admissionTypeId != null) {
            where.append("AND a.admissionType.id = :admissionTypeId ");
            params.put("admissionTypeId", admissionTypeId);
        }
        PaymentMethod pm = parsePaymentMethod(paymentMethod);
        if (pm != null) {
            where.append("AND a.paymentMethod = :paymentMethod ");
            params.put("paymentMethod", pm);
        }

        // COUNT returns a Long - findLongByJpql, never findDoubleByJpql (which would
        // swallow the ClassCastException and silently report 0 every time).
        long total = admissionChargeItemFacade.findLongByJpql(
                "SELECT COUNT(a) FROM AdmissionChargeItem a" + where, params);

        List<AdmissionChargeItem> rows = admissionChargeItemFacade.findByJpqlWithRange(
                "SELECT a FROM AdmissionChargeItem a" + where + "ORDER BY a.orderNo, a.id",
                params, offset, limit);

        List<AdmissionChargeItemDTO> dtos = new ArrayList<>();
        for (AdmissionChargeItem row : rows) {
            dtos.add(buildDTO(row));
        }
        return new AdmissionChargeItemPageDTO(dtos, total, limit, offset);
    }

    // =========================================================================
    // Get by ID
    // =========================================================================

    public AdmissionChargeItemDTO findById(Long id, boolean includeRetired) throws Exception {
        AdmissionChargeItem entity = includeRetired ? loadAllowingRetired(id) : loadAndValidate(id);
        return buildDTO(entity);
    }

    // =========================================================================
    // Create
    // =========================================================================

    public AdmissionChargeItemDTO create(AdmissionChargeItemCreateRequestDTO request, WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new AdmissionChargeValidationException("Valid create request required (itemId is required)");
        }
        if (request.getPrice() < 0) {
            throw new AdmissionChargeValidationException("Price must not be negative");
        }

        Item item = validateItemForCharging(request.getItemId());
        AdmissionType admissionType = resolveAdmissionType(request.getAdmissionTypeId());
        PaymentMethod paymentMethod = parsePaymentMethod(request.getPaymentMethod());

        rejectIfConflicting(item, admissionType, paymentMethod, null);

        AdmissionChargeItem entity = new AdmissionChargeItem();
        entity.setItem(item);
        entity.setAdmissionType(admissionType);
        entity.setPaymentMethod(paymentMethod);
        entity.setPrice(request.getPrice());
        entity.setQty(request.getQty() != null && request.getQty() > 0 ? request.getQty() : 1.0);
        entity.setOrderNo(request.getOrderNo());
        entity.setCreater(user);
        entity.setCreatedAt(Calendar.getInstance().getTime());
        entity.setRetired(false);

        admissionChargeItemFacade.create(entity);

        return buildDTO(entity);
    }

    // =========================================================================
    // Update
    // =========================================================================

    public AdmissionChargeItemDTO update(Long id, AdmissionChargeItemUpdateRequestDTO request, WebUser user) throws Exception {
        if (request == null || !request.isValid()) {
            throw new AdmissionChargeValidationException(
                    "Valid update request required (at least one field must be provided)");
        }

        AdmissionChargeItem entity = loadAndValidate(id);

        Item item = entity.getItem();
        if (request.getItemId() != null) {
            item = validateItemForCharging(request.getItemId());
        }

        AdmissionType admissionType = entity.getAdmissionType();
        if (request.getAdmissionTypeId() != null) {
            admissionType = resolveAdmissionType(request.getAdmissionTypeId());
        } else if (Boolean.TRUE.equals(request.getClearAdmissionType())) {
            admissionType = null;
        }

        PaymentMethod paymentMethod = entity.getPaymentMethod();
        if (request.getPaymentMethod() != null) {
            paymentMethod = parsePaymentMethod(request.getPaymentMethod());
        } else if (Boolean.TRUE.equals(request.getClearPaymentMethod())) {
            paymentMethod = null;
        }

        if (request.getPrice() != null && request.getPrice() < 0) {
            throw new AdmissionChargeValidationException("Price must not be negative");
        }

        // Re-checked whenever any identity dimension changes; a no-op update excludes
        // itself and always passes.
        rejectIfConflicting(item, admissionType, paymentMethod, entity.getId());

        entity.setItem(item);
        entity.setAdmissionType(admissionType);
        entity.setPaymentMethod(paymentMethod);
        if (request.getPrice() != null) {
            entity.setPrice(request.getPrice());
        }
        if (request.getQty() != null && request.getQty() > 0) {
            entity.setQty(request.getQty());
        }
        if (request.getOrderNo() != null) {
            entity.setOrderNo(request.getOrderNo());
        }

        admissionChargeItemFacade.edit(entity);

        return buildDTO(entity);
    }

    // =========================================================================
    // Retire / Restore
    // =========================================================================

    public AdmissionChargeItemDTO retire(Long id, String retireComments, WebUser user) throws Exception {
        AdmissionChargeItem entity = loadAndValidate(id);
        entity.setRetired(true);
        entity.setRetirer(user);
        entity.setRetiredAt(Calendar.getInstance().getTime());
        entity.setRetireComments(retireComments);
        admissionChargeItemFacade.edit(entity);
        return buildDTO(entity);
    }

    /**
     * Undo a retire. Rejected if a live row now occupies the same
     * {@code (item, admissionType, paymentMethod)} triple - restoring must not
     * silently create a second live row for the same combination.
     */
    public AdmissionChargeItemDTO restore(Long id, WebUser user) throws Exception {
        AdmissionChargeItem entity = loadAllowingRetired(id);
        if (!entity.isRetired()) {
            throw new AdmissionChargeValidationException("AdmissionChargeItem with ID " + id + " is not retired");
        }
        rejectIfConflicting(entity.getItem(), entity.getAdmissionType(), entity.getPaymentMethod(), entity.getId());

        entity.setRetired(false);
        entity.setRetiredAt(null);
        entity.setRetirer(null);
        entity.setRetireComments(null);
        admissionChargeItemFacade.edit(entity);
        return buildDTO(entity);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Validates the item against the same requirements
     * {@code AdmissionChargeApplicationBean.buildEntry} relies on at charge
     * time.
     */
    private Item validateItemForCharging(Long itemId) throws Exception {
        if (itemId == null) {
            throw new AdmissionChargeValidationException("Item is required");
        }
        Item item = itemFacade.find(itemId);
        if (item == null) {
            throw new AdmissionChargeValidationException("Item not found with ID: " + itemId);
        }
        if (item.getDepartment() == null) {
            throw new AdmissionChargeValidationException(
                    "Item \"" + item.getName() + "\" has no department configured. "
                    + "An admission charge item's Item must have a department.");
        }
        if (item.getInstitution() == null) {
            throw new AdmissionChargeValidationException(
                    "Item \"" + item.getName() + "\" has no institution configured. "
                    + "An admission charge item's Item must have an institution.");
        }
        if (item.getInwardChargeType() == null) {
            throw new AdmissionChargeValidationException(
                    "Item \"" + item.getName() + "\" has no inward charge type configured. "
                    + "An admission charge item's Item must have one.");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("item", item);
        long liveFees = itemFeeFacade.findLongByJpql(
                "SELECT COUNT(f) FROM ItemFee f WHERE f.item = :item AND f.retired = false", params);
        if (liveFees <= 0) {
            throw new AdmissionChargeValidationException(
                    "Item \"" + item.getName() + "\" has no active fee configured. "
                    + "A charge with no fee would cancel and refund as zero.");
        }

        return item;
    }

    private AdmissionType resolveAdmissionType(Long admissionTypeId) throws Exception {
        if (admissionTypeId == null) {
            return null;
        }
        AdmissionType admissionType = admissionTypeFacade.find(admissionTypeId);
        if (admissionType == null) {
            throw new AdmissionChargeValidationException("AdmissionType not found with ID: " + admissionTypeId);
        }
        return admissionType;
    }

    /**
     * @param raw "Cash", "Credit", or null/blank for "both". Anything else -
     *            including other {@link PaymentMethod} values - is rejected: an
     *            admission is only ever Cash or Credit
     *            ({@code EnumController.getPaymentMethodForAdmission()}).
     */
    private PaymentMethod parsePaymentMethod(String raw) throws Exception {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        PaymentMethod pm;
        try {
            pm = PaymentMethod.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new AdmissionChargeValidationException(
                    "Invalid paymentMethod: " + raw + ". Must be Cash or Credit, or omitted for both.");
        }
        if (pm != PaymentMethod.Cash && pm != PaymentMethod.Credit) {
            throw new AdmissionChargeValidationException(
                    "Invalid paymentMethod: " + raw + ". An admission charge item may only be "
                    + "Cash or Credit (or omitted for both) - an admission is never any other payment method.");
        }
        return pm;
    }

    /**
     * Enforces "only one live row per (item, admissionType, paymentMethod)
     * triple". Built as a conditional JPQL string rather than binding null
     * parameters: {@code a.admissionType = :admissionType} never matches a null
     * column, so the null case must use a literal {@code IS NULL} clause.
     */
    private void rejectIfConflicting(Item item, AdmissionType admissionType, PaymentMethod paymentMethod,
            Long excludeId) throws Exception {
        StringBuilder jpql = new StringBuilder(
                "SELECT COUNT(a) FROM AdmissionChargeItem a WHERE a.item = :item AND a.retired = false ");
        Map<String, Object> params = new HashMap<>();
        params.put("item", item);

        if (admissionType != null) {
            jpql.append("AND a.admissionType = :admissionType ");
            params.put("admissionType", admissionType);
        } else {
            jpql.append("AND a.admissionType IS NULL ");
        }

        if (paymentMethod != null) {
            jpql.append("AND a.paymentMethod = :paymentMethod ");
            params.put("paymentMethod", paymentMethod);
        } else {
            jpql.append("AND a.paymentMethod IS NULL ");
        }

        if (excludeId != null) {
            jpql.append("AND a.id <> :excludeId ");
            params.put("excludeId", excludeId);
        }

        long conflicts = admissionChargeItemFacade.findLongByJpql(jpql.toString(), params);
        if (conflicts > 0) {
            throw new AdmissionChargeValidationException(
                    "An admission charge item already exists for item \"" + item.getName() + "\" with admissionType="
                    + (admissionType != null ? admissionType.getName() : "any") + " and paymentMethod="
                    + (paymentMethod != null ? paymentMethod.name() : "both")
                    + ". Retire the existing row first, or update it instead of creating a duplicate.");
        }
    }

    private AdmissionChargeItem loadAndValidate(Long id) throws Exception {
        AdmissionChargeItem entity = loadAllowingRetired(id);
        if (entity.isRetired()) {
            throw new Exception("AdmissionChargeItem with ID " + id + " is retired");
        }
        return entity;
    }

    /** Load without rejecting a retired row - for reads that opted in, and for restore. */
    private AdmissionChargeItem loadAllowingRetired(Long id) throws Exception {
        if (id == null) {
            throw new AdmissionChargeValidationException("AdmissionChargeItem ID is required");
        }
        AdmissionChargeItem entity = admissionChargeItemFacade.find(id);
        if (entity == null) {
            throw new Exception("AdmissionChargeItem not found with ID: " + id);
        }
        return entity;
    }

    private AdmissionChargeItemDTO buildDTO(AdmissionChargeItem entity) {
        AdmissionChargeItemDTO dto = new AdmissionChargeItemDTO();
        dto.setId(entity.getId());

        Item item = entity.getItem();
        if (item != null) {
            dto.setItemId(item.getId());
            dto.setItemName(item.getName());
            if (item.getDepartment() != null) {
                dto.setItemDepartmentId(item.getDepartment().getId());
                dto.setItemDepartmentName(item.getDepartment().getName());
            }
            if (item.getInwardChargeType() != null) {
                dto.setInwardChargeType(item.getInwardChargeType().name());
            }
        }

        AdmissionType admissionType = entity.getAdmissionType();
        if (admissionType != null) {
            dto.setAdmissionTypeId(admissionType.getId());
            dto.setAdmissionTypeName(admissionType.getName());
        }

        if (entity.getPaymentMethod() != null) {
            dto.setPaymentMethod(entity.getPaymentMethod().name());
        }

        dto.setPrice(entity.getPrice());
        dto.setQty(entity.getQty());
        dto.setOrderNo(entity.getOrderNo());
        dto.setRetired(entity.isRetired());
        return dto;
    }
}
