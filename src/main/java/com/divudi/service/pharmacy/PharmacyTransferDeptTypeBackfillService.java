package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.util.CommonFunctions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * EJB service that backfills the missing departmentType on historical pharmacy
 * transfer bills (issue / receive / cancellations / returns).
 *
 * These bills were saved with departmentType = NULL during the capture
 * regression fixed under issue #22056 (and by legacy flows that never stamped
 * the field). Department-type-filtered reports drop NULL bills silently, so
 * the historical rows must be corrected (#22057, #22058, #22067).
 *
 * Resolution order per bill:
 *   1. ITEMS - the single department type shared unanimously by all non-null
 *      item types on the bill (mixed-type bills are never guessed)
 *   2. BACKWARD_REFERENCE_BILL - the referenced source bill's type
 *      (issue for a receive, request for an issue)
 *   3. BILLED_BILL - the original bill's type (for cancellation bills)
 *   4. unresolved - reported for manual review, never written
 */
@Stateless
public class PharmacyTransferDeptTypeBackfillService {

    @EJB
    private BillFacade billFacade;

    @EJB
    private BillItemFacade billItemFacade;

    private static final List<BillTypeAtomic> TRANSFER_TYPES = Arrays.asList(
            BillTypeAtomic.PHARMACY_ISSUE,
            BillTypeAtomic.PHARMACY_ISSUE_PRE,
            BillTypeAtomic.PHARMACY_RECEIVE,
            BillTypeAtomic.PHARMACY_RECEIVE_PRE,
            BillTypeAtomic.PHARMACY_ISSUE_CANCELLED,
            BillTypeAtomic.PHARMACY_RECEIVE_CANCELLED,
            BillTypeAtomic.PHARMACY_ISSUE_RETURN);

    /**
     * Finds transfer bills with departmentType IS NULL in the date range and
     * resolves each one's type, in dry-run or apply mode.
     *
     * @param departmentId optional bill.department filter (null = all departments)
     * @param fromDate     start date inclusive (normalised to start of day)
     * @param toDate       end date inclusive (normalised to end of day)
     * @param apply        false = return the resolution plan without writing
     * @param auditComment audit trail comment (required by the API layer)
     * @param approvedBy   approver name (required by the API layer)
     * @param apiUser      the API user performing the backfill
     * @return summary with per-source counts, the per-bill plan, unresolved
     *         bill IDs and any per-bill errors
     */
    public Map<String, Object> backfillTransferDepartmentTypes(
            Long departmentId,
            Date fromDate,
            Date toDate,
            boolean apply,
            String auditComment,
            String approvedBy,
            WebUser apiUser) {

        Date from = CommonFunctions.getStartOfDay(fromDate);
        Date to = CommonFunctions.getEndOfDay(toDate);

        String jpql = "SELECT b FROM Bill b"
                + " WHERE b.retired = false"
                + " AND b.billTypeAtomic IN :types"
                + " AND b.departmentType IS NULL"
                + " AND b.createdAt BETWEEN :from AND :to"
                + (departmentId != null ? " AND b.department.id = :deptId" : "")
                + " ORDER BY b.id";

        Map<String, Object> params = new HashMap<>();
        params.put("types", TRANSFER_TYPES);
        params.put("from", from);
        params.put("to", to);
        if (departmentId != null) {
            params.put("deptId", departmentId);
        }

        List<Bill> bills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

        int fromItems = 0;
        int fromBackwardReferenceBill = 0;
        int fromBilledBill = 0;
        int appliedCount = 0;
        List<Long> unresolvedBillIds = new ArrayList<>();
        List<Map<String, Object>> plan = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Bill bill : bills) {
            try {
                DepartmentType resolved = unanimousItemDepartmentType(bill);
                String source;
                if (resolved != null) {
                    source = "ITEMS";
                    fromItems++;
                } else if (bill.getBackwardReferenceBill() != null
                        && bill.getBackwardReferenceBill().getDepartmentType() != null) {
                    resolved = bill.getBackwardReferenceBill().getDepartmentType();
                    source = "BACKWARD_REFERENCE_BILL";
                    fromBackwardReferenceBill++;
                } else if (bill.getBilledBill() != null
                        && bill.getBilledBill().getDepartmentType() != null) {
                    resolved = bill.getBilledBill().getDepartmentType();
                    source = "BILLED_BILL";
                    fromBilledBill++;
                } else {
                    unresolvedBillIds.add(bill.getId());
                    continue;
                }

                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("billId", bill.getId());
                entry.put("deptId", bill.getDeptId());
                entry.put("billTypeAtomic", bill.getBillTypeAtomic() != null ? bill.getBillTypeAtomic().name() : null);
                entry.put("resolvedDepartmentType", resolved.name());
                entry.put("source", source);
                plan.add(entry);

                if (apply) {
                    bill.setDepartmentType(resolved);
                    appendAuditLog(bill, resolved, source, auditComment, approvedBy, apiUser);
                    billFacade.edit(bill);
                    appliedCount++;
                }
            } catch (Exception ex) {
                errors.add("Bill " + bill.getId() + ": " + ex.getMessage());
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("apply", apply);
        summary.put("totalCandidates", bills.size());
        summary.put("resolvedFromItems", fromItems);
        summary.put("resolvedFromBackwardReferenceBill", fromBackwardReferenceBill);
        summary.put("resolvedFromBilledBill", fromBilledBill);
        summary.put("unresolved", unresolvedBillIds.size());
        summary.put("appliedCount", appliedCount);
        summary.put("unresolvedBillIds", unresolvedBillIds);
        summary.put("plan", plan);
        summary.put("errors", errors);
        return summary;
    }

    /**
     * Returns the department type shared unanimously by every non-null item
     * type on the bill, or null when the items are mixed or untyped.
     */
    private DepartmentType unanimousItemDepartmentType(Bill bill) {
        Map<String, Object> params = new HashMap<>();
        params.put("bill", bill);
        List<BillItem> items = billItemFacade.findByJpql(
                "SELECT bi FROM BillItem bi WHERE bi.bill = :bill AND bi.retired = false", params);
        DepartmentType found = null;
        for (BillItem bi : items) {
            if (bi.getItem() == null || bi.getItem().getDepartmentType() == null) {
                continue;
            }
            if (found == null) {
                found = bi.getItem().getDepartmentType();
            } else if (!found.equals(bi.getItem().getDepartmentType())) {
                return null;
            }
        }
        return found;
    }

    private void appendAuditLog(Bill bill,
            DepartmentType resolved,
            String source,
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
        sb.append("[Transfer DepartmentType Backfill]")
                .append("\nTime: ").append(now)
                .append("\nPreviousValue: null")
                .append("\nNewValue: ").append(resolved.name())
                .append("\nSource: ").append(source)
                .append("\nCorrectedByApiUser: ").append(correctedBy)
                .append("\nApprovedBy: ").append(approvedBy)
                .append("\nAuditComment: ").append(auditComment);

        bill.setComments(sb.toString());
    }
}
