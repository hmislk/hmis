package com.divudi.service;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillFinanceDetailsFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BillItemFinanceDetailsFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class BillDataCorrectionService {

    @EJB
    private BillFacade billFacade;

    @EJB
    private BillItemFacade billItemFacade;

    @EJB
    private BillFinanceDetailsFacade billFinanceDetailsFacade;

    @EJB
    private BillFeeFacade billFeeFacade;

    @EJB
    private BillItemFinanceDetailsFacade billItemFinanceDetailsFacade;

    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;

    private static final Set<String> BILL_FIELDS = new HashSet<>(Arrays.asList("netTotal", "grossTotal", "comments"));
    private static final Set<String> BILL_ITEM_FIELDS = new HashSet<>(Arrays.asList("qty", "rate", "grossValue", "netValue", "discount"));
    private static final Set<String> BILL_FINANCE_FIELDS = new HashSet<>(Arrays.asList("totalRetailSaleValue", "totalCostValue", "totalPurchaseValue", "netTotal", "grossTotal"));
    private static final Set<String> BILL_FEE_FIELDS = new HashSet<>(Arrays.asList("feeValue", "grossValue"));
    private static final Set<String> BILL_ITEM_FINANCE_FIELDS = new HashSet<>(Arrays.asList("valueAtRetailRate", "valueAtCostRate", "costRate", "retailSaleRate"));
    private static final Set<String> PHARMACEUTICAL_BILL_ITEM_FIELDS = new HashSet<>(Arrays.asList("qty", "retailRate", "costRate", "retailValue", "costValue"));

    public Map<String, Object> correctData(String targetType,
            Long targetId,
            Map<String, Object> fields,
            String auditComment,
            String approvedBy,
            WebUser apiUser) {

        if (targetType == null || targetType.trim().isEmpty()) {
            throw new IllegalArgumentException("targetType is required");
        }
        if (targetId == null) {
            throw new IllegalArgumentException("targetId is required");
        }
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("fields are required");
        }

        String normalizedType = targetType.trim().toUpperCase();

        Map<String, Object> previousValues = new LinkedHashMap<>();
        Map<String, Object> newValues = new LinkedHashMap<>();
        Bill parentBill;

        switch (normalizedType) {
            case "BILL":
                parentBill = updateBill(targetId, fields, previousValues, newValues);
                break;
            case "BILL_ITEM":
                parentBill = updateBillItem(targetId, fields, previousValues, newValues);
                break;
            case "BILL_FINANCE_DETAILS":
                parentBill = updateBillFinanceDetails(targetId, fields, previousValues, newValues);
                break;
            case "BILL_FEES":
                parentBill = updateBillFee(targetId, fields, previousValues, newValues);
                break;
            case "BILL_ITEM_FINANCE_DETAILS":
                parentBill = updateBillItemFinanceDetails(targetId, fields, previousValues, newValues);
                break;
            case "PHARMACEUTICAL_BILL_ITEM":
                parentBill = updatePharmaceuticalBillItem(targetId, fields, previousValues, newValues);
                break;
            default:
                throw new IllegalArgumentException("Unsupported targetType: " + targetType);
        }

        if (parentBill == null) {
            throw new IllegalStateException("Unable to resolve parent bill for targetType " + targetType + " and targetId " + targetId);
        }

        appendAuditLog(parentBill, normalizedType, targetId, previousValues, newValues, auditComment, approvedBy, apiUser);
        billFacade.edit(parentBill);

        String correctedBy = apiUser != null ? apiUser.getName() : null;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("targetType", normalizedType);
        result.put("targetId", targetId);
        result.put("previousValues", previousValues);
        result.put("newValues", newValues);
        result.put("auditComment", auditComment);
        result.put("approvedBy", approvedBy);
        result.put("correctedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        result.put("correctedByApiUser", correctedBy);
        return result;
    }

    private Bill updateBill(Long id, Map<String, Object> fields, Map<String, Object> previousValues, Map<String, Object> newValues) {
        Bill entity = billFacade.find(id);
        if (entity == null) {
            throw new IllegalArgumentException("Bill not found for id " + id);
        }
        validateAllowedFields(fields, BILL_FIELDS, "BILL");

        if (fields.containsKey("netTotal")) {
            previousValues.put("netTotal", entity.getNetTotal());
            double value = toDouble(fields.get("netTotal"), "netTotal");
            entity.setNetTotal(value);
            newValues.put("netTotal", entity.getNetTotal());
        }
        if (fields.containsKey("grossTotal")) {
            previousValues.put("grossTotal", entity.getTotal());
            double value = toDouble(fields.get("grossTotal"), "grossTotal");
            entity.setTotal(value);
            newValues.put("grossTotal", entity.getTotal());
        }
        if (fields.containsKey("comments")) {
            previousValues.put("comments", entity.getComments());
            String value = toStringValue(fields.get("comments"));
            entity.setComments(value);
            newValues.put("comments", entity.getComments());
        }

        billFacade.edit(entity);
        return entity;
    }

    private Bill updateBillItem(Long id, Map<String, Object> fields, Map<String, Object> previousValues, Map<String, Object> newValues) {
        BillItem entity = billItemFacade.find(id);
        if (entity == null) {
            throw new IllegalArgumentException("BillItem not found for id " + id);
        }
        validateAllowedFields(fields, BILL_ITEM_FIELDS, "BILL_ITEM");

        if (fields.containsKey("qty")) {
            previousValues.put("qty", entity.getQty());
            double value = toDouble(fields.get("qty"), "qty");
            entity.setQty(value);
            newValues.put("qty", entity.getQty());
        }
        if (fields.containsKey("rate")) {
            previousValues.put("rate", entity.getRate());
            double value = toDouble(fields.get("rate"), "rate");
            entity.setRate(value);
            newValues.put("rate", entity.getRate());
        }
        if (fields.containsKey("grossValue")) {
            previousValues.put("grossValue", entity.getGrossValue());
            double value = toDouble(fields.get("grossValue"), "grossValue");
            entity.setGrossValue(value);
            newValues.put("grossValue", entity.getGrossValue());
        }
        if (fields.containsKey("netValue")) {
            previousValues.put("netValue", entity.getNetValue());
            double value = toDouble(fields.get("netValue"), "netValue");
            entity.setNetValue(value);
            newValues.put("netValue", entity.getNetValue());
        }
        if (fields.containsKey("discount")) {
            previousValues.put("discount", entity.getDiscount());
            double value = toDouble(fields.get("discount"), "discount");
            entity.setDiscount(value);
            newValues.put("discount", entity.getDiscount());
        }

        billItemFacade.edit(entity);
        return entity.getBill();
    }

    private Bill updateBillFinanceDetails(Long id, Map<String, Object> fields, Map<String, Object> previousValues, Map<String, Object> newValues) {
        BillFinanceDetails entity = billFinanceDetailsFacade.find(id);
        if (entity == null) {
            throw new IllegalArgumentException("BillFinanceDetails not found for id " + id);
        }
        validateAllowedFields(fields, BILL_FINANCE_FIELDS, "BILL_FINANCE_DETAILS");

        if (fields.containsKey("totalRetailSaleValue")) {
            previousValues.put("totalRetailSaleValue", entity.getTotalRetailSaleValue());
            BigDecimal value = toBigDecimal(fields.get("totalRetailSaleValue"), "totalRetailSaleValue");
            entity.setTotalRetailSaleValue(value);
            newValues.put("totalRetailSaleValue", entity.getTotalRetailSaleValue());
        }
        if (fields.containsKey("totalCostValue")) {
            previousValues.put("totalCostValue", entity.getTotalCostValue());
            BigDecimal value = toBigDecimal(fields.get("totalCostValue"), "totalCostValue");
            entity.setTotalCostValue(value);
            newValues.put("totalCostValue", entity.getTotalCostValue());
        }
        if (fields.containsKey("totalPurchaseValue")) {
            previousValues.put("totalPurchaseValue", entity.getTotalPurchaseValue());
            BigDecimal value = toBigDecimal(fields.get("totalPurchaseValue"), "totalPurchaseValue");
            entity.setTotalPurchaseValue(value);
            newValues.put("totalPurchaseValue", entity.getTotalPurchaseValue());
        }
        if (fields.containsKey("netTotal")) {
            previousValues.put("netTotal", entity.getNetTotal());
            BigDecimal value = toBigDecimal(fields.get("netTotal"), "netTotal");
            entity.setNetTotal(value);
            newValues.put("netTotal", entity.getNetTotal());
        }
        if (fields.containsKey("grossTotal")) {
            previousValues.put("grossTotal", entity.getGrossTotal());
            BigDecimal value = toBigDecimal(fields.get("grossTotal"), "grossTotal");
            entity.setGrossTotal(value);
            newValues.put("grossTotal", entity.getGrossTotal());
        }

        billFinanceDetailsFacade.edit(entity);
        return entity.getBill();
    }

    private Bill updateBillFee(Long id, Map<String, Object> fields, Map<String, Object> previousValues, Map<String, Object> newValues) {
        BillFee entity = billFeeFacade.find(id);
        if (entity == null) {
            throw new IllegalArgumentException("BillFee not found for id " + id);
        }
        validateAllowedFields(fields, BILL_FEE_FIELDS, "BILL_FEES");

        if (fields.containsKey("feeValue")) {
            previousValues.put("feeValue", entity.getFeeValue());
            double value = toDouble(fields.get("feeValue"), "feeValue");
            entity.setFeeValue(value);
            newValues.put("feeValue", entity.getFeeValue());
        }
        if (fields.containsKey("grossValue")) {
            previousValues.put("grossValue", entity.getFeeGrossValue());
            double value = toDouble(fields.get("grossValue"), "grossValue");
            entity.setFeeGrossValue(value);
            newValues.put("grossValue", entity.getFeeGrossValue());
        }
        billFeeFacade.edit(entity);
        if (entity.getBill() != null) {
            return entity.getBill();
        }
        if (entity.getBillItem() != null) {
            return entity.getBillItem().getBill();
        }
        return null;
    }

    private Bill updateBillItemFinanceDetails(Long id, Map<String, Object> fields, Map<String, Object> previousValues, Map<String, Object> newValues) {
        BillItemFinanceDetails entity = billItemFinanceDetailsFacade.find(id);
        if (entity == null) {
            throw new IllegalArgumentException("BillItemFinanceDetails not found for id " + id);
        }
        validateAllowedFields(fields, BILL_ITEM_FINANCE_FIELDS, "BILL_ITEM_FINANCE_DETAILS");

        if (fields.containsKey("valueAtRetailRate")) {
            previousValues.put("valueAtRetailRate", entity.getValueAtRetailRate());
            BigDecimal value = toBigDecimal(fields.get("valueAtRetailRate"), "valueAtRetailRate");
            entity.setValueAtRetailRate(value);
            newValues.put("valueAtRetailRate", entity.getValueAtRetailRate());
        }
        if (fields.containsKey("valueAtCostRate")) {
            previousValues.put("valueAtCostRate", entity.getValueAtCostRate());
            BigDecimal value = toBigDecimal(fields.get("valueAtCostRate"), "valueAtCostRate");
            entity.setValueAtCostRate(value);
            newValues.put("valueAtCostRate", entity.getValueAtCostRate());
        }
        if (fields.containsKey("costRate")) {
            previousValues.put("costRate", entity.getCostRate());
            BigDecimal value = toBigDecimal(fields.get("costRate"), "costRate");
            entity.setCostRate(value);
            newValues.put("costRate", entity.getCostRate());
        }
        if (fields.containsKey("retailSaleRate")) {
            previousValues.put("retailSaleRate", entity.getRetailSaleRate());
            BigDecimal value = toBigDecimal(fields.get("retailSaleRate"), "retailSaleRate");
            entity.setRetailSaleRate(value);
            newValues.put("retailSaleRate", entity.getRetailSaleRate());
        }

        billItemFinanceDetailsFacade.edit(entity);
        return entity.getBillItem() != null ? entity.getBillItem().getBill() : null;
    }

    private Bill updatePharmaceuticalBillItem(Long id, Map<String, Object> fields, Map<String, Object> previousValues, Map<String, Object> newValues) {
        PharmaceuticalBillItem entity = pharmaceuticalBillItemFacade.find(id);
        if (entity == null) {
            throw new IllegalArgumentException("PharmaceuticalBillItem not found for id " + id);
        }
        validateAllowedFields(fields, PHARMACEUTICAL_BILL_ITEM_FIELDS, "PHARMACEUTICAL_BILL_ITEM");

        if (fields.containsKey("qty")) {
            previousValues.put("qty", entity.getQty());
            double value = toDouble(fields.get("qty"), "qty");
            entity.setQty(value);
            newValues.put("qty", entity.getQty());
        }
        if (fields.containsKey("retailRate")) {
            previousValues.put("retailRate", entity.getRetailRate());
            double value = toDouble(fields.get("retailRate"), "retailRate");
            entity.setRetailRate(value);
            newValues.put("retailRate", entity.getRetailRate());
        }
        if (fields.containsKey("costRate")) {
            previousValues.put("costRate", entity.getCostRate());
            double value = toDouble(fields.get("costRate"), "costRate");
            entity.setCostRate(value);
            newValues.put("costRate", entity.getCostRate());
        }
        if (fields.containsKey("retailValue")) {
            previousValues.put("retailValue", entity.getRetailValue());
            double value = toDouble(fields.get("retailValue"), "retailValue");
            entity.setRetailValue(value);
            newValues.put("retailValue", entity.getRetailValue());
        }
        if (fields.containsKey("costValue")) {
            previousValues.put("costValue", entity.getCostValue());
            double value = toDouble(fields.get("costValue"), "costValue");
            entity.setCostValue(value);
            newValues.put("costValue", entity.getCostValue());
        }

        pharmaceuticalBillItemFacade.edit(entity);
        return entity.getBillItem() != null ? entity.getBillItem().getBill() : null;
    }

    private void validateAllowedFields(Map<String, Object> fields, Set<String> allowedFields, String targetType) {
        for (String key : fields.keySet()) {
            if (!allowedFields.contains(key)) {
                throw new IllegalArgumentException("Field '" + key + "' is not allowed for " + targetType);
            }
        }
    }

    private void appendAuditLog(Bill bill,
            String targetType,
            Long targetId,
            Map<String, Object> previousValues,
            Map<String, Object> newValues,
            String auditComment,
            String approvedBy,
            WebUser apiUser) {

        String existing = bill.getComments();
        String correctedBy = apiUser != null ? apiUser.getName() : "Unknown API User";
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        StringBuilder sb = new StringBuilder();
        if (existing != null && !existing.trim().isEmpty()) {
            sb.append(existing.trim()).append("\n\n");
        }
        sb.append("[Bill Data Correction]")
                .append("\nTime: ").append(now)
                .append("\nTargetType: ").append(targetType)
                .append("\nTargetId: ").append(targetId)
                .append("\nCorrectedByApiUser: ").append(correctedBy)
                .append("\nApprovedBy: ").append(approvedBy)
                .append("\nAuditComment: ").append(auditComment)
                .append("\nPreviousValues: ").append(previousValues)
                .append("\nNewValues: ").append(newValues);

        bill.setComments(sb.toString());
    }

    private BigDecimal toBigDecimal(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Field '" + fieldName + "' must be numeric");
        }
    }

    private double toDouble(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("Field '" + fieldName + "' can not be null");
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Field '" + fieldName + "' must be numeric");
        }
    }

    private String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
