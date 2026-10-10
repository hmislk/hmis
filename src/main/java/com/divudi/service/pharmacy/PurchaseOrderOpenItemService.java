/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.facade.BillItemFacade;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Finds open (not-yet-fully-received) purchase orders that already carry a
 * given item, so the Purchase Order Request pages can warn the user instead
 * of silently letting the same item be re-ordered. Related issue: #23811.
 *
 * "Open PO" is the union of:
 * a) Approved PO lines (PHARMACY_ORDER_APPROVAL) whose received quantity is
 *    still short of the ordered quantity. A part-received line stays open,
 *    because the outstanding balance is exactly what a repeat order would
 *    duplicate.
 * b) Finalized-but-not-yet-approved requests (PHARMACY_ORDER with a null
 *    referenceBill).
 *
 * Receipt is measured from the GRN lines that reference the order line
 * (BillItem.referanceBillItem), not from Bill.fullyIssued: the native GRN
 * path never maintains that flag, so it reads false on orders that were in
 * fact received long ago.
 */
@Stateless
public class PurchaseOrderOpenItemService {

    /** Cap so a heavily back-ordered item doesn't produce a wall of PO numbers. */
    private static final int MAX_RESULTS_PER_QUERY = 5;

    /** Cap on item names listed in the single summary warning for bulk adds. */
    private static final int MAX_ITEM_NAMES = 10;

    /** GRN bill types that count as goods received against an order line. */
    private static final List<BillTypeAtomic> GRN_BILL_TYPES = Arrays.asList(
            BillTypeAtomic.PHARMACY_GRN,
            BillTypeAtomic.PHARMACY_GRN_PRE);

    /**
     * Correlated subquery: total received (qty + free qty) on this order line is
     * still less than what was ordered. Cancelled and retired GRNs don't count,
     * so cancelling a GRN reopens the line.
     */
    private static final String NOT_FULLY_RECEIVED
            = " and (select coalesce(sum(abs(g.pharmaceuticalBillItem.qty) + abs(g.pharmaceuticalBillItem.freeQty)), 0) "
            + "        from BillItem g "
            + "        where g.referanceBillItem = bi "
            + "          and g.retired = false "
            + "          and g.bill.retired = false "
            + "          and g.bill.cancelled = false "
            + "          and g.bill.billTypeAtomic in :grnTypes) "
            + "      < (abs(bi.pharmaceuticalBillItem.qty) + abs(bi.pharmaceuticalBillItem.freeQty)) ";

    @EJB
    private BillItemFacade billItemFacade;

    /**
     * Purchase order numbers of open orders already carrying this item, for the
     * warning shown when a single item is added.
     *
     * @param excludeRequestBillId  the request being edited, so it never warns about itself
     * @param excludeApprovalBillId the approval raised from that request, for the same
     *                              reason: an order must not be reported as competing
     *                              with its own approved copy
     */
    public List<String> findOpenPurchaseOrderNumbersForItem(Item item, Institution institution,
            Long excludeRequestBillId, Long excludeApprovalBillId) {
        List<String> result = new ArrayList<>();
        if (item == null || institution == null) {
            return result;
        }
        addAll(result, findApprovedOpenPurchaseOrderNumbers(item, institution, excludeApprovalBillId));
        addAll(result, findFinalizedNotApprovedPurchaseOrderNumbers(item, institution, excludeRequestBillId));
        return result;
    }

    /**
     * Names of the items in the given list that sit on an open purchase order.
     * Used by the bulk "Add All" / "Add Items Below ROL" actions, which would
     * otherwise need one query per item and would produce one message per item.
     */
    public List<String> findItemNamesOnOpenPurchaseOrders(List<Item> items, Institution institution,
            Long excludeRequestBillId, Long excludeApprovalBillId) {
        List<String> result = new ArrayList<>();
        if (items == null || items.isEmpty() || institution == null) {
            return result;
        }
        addAll(result, findApprovedOpenItemNames(items, institution, excludeApprovalBillId));
        addAll(result, findFinalizedNotApprovedItemNames(items, institution, excludeRequestBillId));
        if (result.size() > MAX_ITEM_NAMES) {
            return new ArrayList<>(result.subList(0, MAX_ITEM_NAMES));
        }
        return result;
    }

    // -------------------------------------------------------------------
    // a) Approved PO lines not yet fully received via a GRN
    // -------------------------------------------------------------------
    private List<String> findApprovedOpenPurchaseOrderNumbers(Item item, Institution institution, Long excludeApprovalBillId) {
        StringBuilder jpql = new StringBuilder("select distinct coalesce(bi.bill.deptId, bi.bill.insId) "
                + " from BillItem bi "
                + " where bi.bill.billTypeAtomic = :approvalType "
                + approvedOpenConditions()
                + " and bi.item = :item "
                + NOT_FULLY_RECEIVED);
        Map<String, Object> params = approvedOpenParams(institution);
        params.put("item", item);
        appendExcludedApprovalBill(jpql, params, excludeApprovalBillId);
        return capped(billItemFacade.findStringListByJpql(jpql.toString(), params), MAX_RESULTS_PER_QUERY);
    }

    private List<String> findApprovedOpenItemNames(List<Item> items, Institution institution, Long excludeApprovalBillId) {
        StringBuilder jpql = new StringBuilder("select distinct bi.item.name "
                + " from BillItem bi "
                + " where bi.bill.billTypeAtomic = :approvalType "
                + approvedOpenConditions()
                + " and bi.item in :items "
                + NOT_FULLY_RECEIVED);
        Map<String, Object> params = approvedOpenParams(institution);
        params.put("items", items);
        appendExcludedApprovalBill(jpql, params, excludeApprovalBillId);
        return capped(billItemFacade.findStringListByJpql(jpql.toString(), params), MAX_ITEM_NAMES);
    }

    private void appendExcludedApprovalBill(StringBuilder jpql, Map<String, Object> params, Long excludeApprovalBillId) {
        if (excludeApprovalBillId != null) {
            jpql.append(" and bi.bill.id <> :excludeApprovalBillId ");
            params.put("excludeApprovalBillId", excludeApprovalBillId);
        }
    }

    private String approvedOpenConditions() {
        return " and bi.retired = false "
                + " and bi.bill.retired = false "
                + " and bi.bill.cancelled = false "
                + " and bi.bill.billClosed = false "
                + " and bi.bill.institution = :ins ";
    }

    private Map<String, Object> approvedOpenParams(Institution institution) {
        Map<String, Object> params = new HashMap<>();
        params.put("approvalType", BillTypeAtomic.PHARMACY_ORDER_APPROVAL);
        params.put("ins", institution);
        params.put("grnTypes", GRN_BILL_TYPES);
        return params;
    }

    // -------------------------------------------------------------------
    // b) Finalized requests not yet approved
    // -------------------------------------------------------------------
    private List<String> findFinalizedNotApprovedPurchaseOrderNumbers(Item item, Institution institution, Long excludeBillId) {
        StringBuilder jpql = new StringBuilder("select distinct coalesce(bi.bill.deptId, bi.bill.insId) "
                + " from BillItem bi "
                + finalizedNotApprovedConditions()
                + " and bi.item = :item ");
        Map<String, Object> params = finalizedNotApprovedParams(institution);
        params.put("item", item);
        appendExcludedBill(jpql, params, excludeBillId);
        return capped(billItemFacade.findStringListByJpql(jpql.toString(), params), MAX_RESULTS_PER_QUERY);
    }

    private List<String> findFinalizedNotApprovedItemNames(List<Item> items, Institution institution, Long excludeBillId) {
        StringBuilder jpql = new StringBuilder("select distinct bi.item.name "
                + " from BillItem bi "
                + finalizedNotApprovedConditions()
                + " and bi.item in :items ");
        Map<String, Object> params = finalizedNotApprovedParams(institution);
        params.put("items", items);
        appendExcludedBill(jpql, params, excludeBillId);
        return capped(billItemFacade.findStringListByJpql(jpql.toString(), params), MAX_ITEM_NAMES);
    }

    private String finalizedNotApprovedConditions() {
        return " where bi.bill.billTypeAtomic = :requestType "
                + " and bi.retired = false "
                + " and bi.bill.retired = false "
                + " and bi.bill.cancelled = false "
                + " and bi.bill.referenceBill is null "
                + " and bi.bill.institution = :ins ";
    }

    private Map<String, Object> finalizedNotApprovedParams(Institution institution) {
        Map<String, Object> params = new HashMap<>();
        params.put("requestType", BillTypeAtomic.PHARMACY_ORDER);
        params.put("ins", institution);
        return params;
    }

    private void appendExcludedBill(StringBuilder jpql, Map<String, Object> params, Long excludeBillId) {
        if (excludeBillId != null) {
            jpql.append(" and bi.bill.id <> :excludeBillId ");
            params.put("excludeBillId", excludeBillId);
        }
    }

    // -------------------------------------------------------------------
    private void addAll(List<String> target, List<String> found) {
        if (found == null) {
            return;
        }
        for (String value : found) {
            if (value != null && !value.trim().isEmpty() && !target.contains(value)) {
                target.add(value);
            }
        }
    }

    private List<String> capped(List<String> list, int max) {
        if (list == null) {
            return new ArrayList<>();
        }
        if (list.size() > max) {
            return new ArrayList<>(list.subList(0, max));
        }
        return list;
    }
}
