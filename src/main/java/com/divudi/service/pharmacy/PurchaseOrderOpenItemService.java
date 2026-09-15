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
 * a) Approved PO lines (PHARMACY_ORDER_APPROVAL) that have no non-retired GRN
 *    line referencing them yet.
 * b) Finalized-but-not-yet-approved requests (PHARMACY_ORDER with a null
 *    referenceBill).
 */
@Stateless
public class PurchaseOrderOpenItemService {

    /** Cap per query so a heavily back-ordered item doesn't produce a wall of PO numbers. */
    private static final int MAX_RESULTS_PER_QUERY = 5;

    @EJB
    private BillItemFacade billItemFacade;

    public List<String> findOpenPurchaseOrderNumbersForItem(Item item, Institution institution, Long excludeBillId) {
        List<String> result = new ArrayList<>();
        if (item == null || institution == null) {
            return result;
        }

        for (String no : findApprovedOpenPurchaseOrderNumbers(item, institution)) {
            if (no != null && !no.trim().isEmpty() && !result.contains(no)) {
                result.add(no);
            }
        }
        for (String no : findFinalizedNotApprovedPurchaseOrderNumbers(item, institution, excludeBillId)) {
            if (no != null && !no.trim().isEmpty() && !result.contains(no)) {
                result.add(no);
            }
        }
        return result;
    }

    // -------------------------------------------------------------------
    // a) Approved PO lines not yet fully received via a GRN
    // -------------------------------------------------------------------
    private List<String> findApprovedOpenPurchaseOrderNumbers(Item item, Institution institution) {
        String jpql = "select distinct coalesce(bi.bill.deptId, bi.bill.insId) "
                + " from BillItem bi "
                + " where bi.bill.billTypeAtomic = :approvalType "
                + " and bi.retired = false "
                + " and bi.bill.retired = false "
                + " and bi.bill.cancelled = false "
                + " and bi.bill.billClosed = false "
                + " and bi.bill.institution = :ins "
                + " and bi.item = :item "
                + " and not exists ("
                + "     select g from BillItem g "
                + "     where g.referanceBillItem = bi "
                + "     and g.retired = false "
                + "     and g.bill.retired = false "
                + "     and g.bill.cancelled = false "
                + " )";
        Map<String, Object> params = new HashMap<>();
        params.put("approvalType", BillTypeAtomic.PHARMACY_ORDER_APPROVAL);
        params.put("ins", institution);
        params.put("item", item);
        return capped(billItemFacade.findStringListByJpql(jpql, params));
    }

    // -------------------------------------------------------------------
    // b) Finalized requests not yet approved
    // -------------------------------------------------------------------
    private List<String> findFinalizedNotApprovedPurchaseOrderNumbers(Item item, Institution institution, Long excludeBillId) {
        StringBuilder jpql = new StringBuilder(
                "select distinct coalesce(bi.bill.deptId, bi.bill.insId) "
                + " from BillItem bi "
                + " where bi.bill.billTypeAtomic = :requestType "
                + " and bi.retired = false "
                + " and bi.bill.retired = false "
                + " and bi.bill.cancelled = false "
                + " and bi.bill.referenceBill is null "
                + " and bi.bill.institution = :ins "
                + " and bi.item = :item ");
        Map<String, Object> params = new HashMap<>();
        params.put("requestType", BillTypeAtomic.PHARMACY_ORDER);
        params.put("ins", institution);
        params.put("item", item);
        if (excludeBillId != null) {
            jpql.append(" and bi.bill.id <> :excludeBillId ");
            params.put("excludeBillId", excludeBillId);
        }
        return capped(billItemFacade.findStringListByJpql(jpql.toString(), params));
    }

    private List<String> capped(List<String> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        if (list.size() > MAX_RESULTS_PER_QUERY) {
            return new ArrayList<>(list.subList(0, MAX_RESULTS_PER_QUERY));
        }
        return list;
    }
}
