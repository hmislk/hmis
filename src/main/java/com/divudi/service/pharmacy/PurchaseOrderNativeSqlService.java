/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.dto.pharmacy.PurchaseOrderItemPrintDto;
import com.divudi.core.data.dto.pharmacy.PurchaseOrderPrintDto;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads Purchase Order print data via native SQL.
 * No JPA entity graph inflation — all data fetched in two queries (header + items).
 * LEFT JOINs throughout so null FKs never drop the bill from the result set.
 * Related issue: #20923
 */
@Stateless
public class PurchaseOrderNativeSqlService {

    private static final Logger LOGGER = Logger.getLogger(PurchaseOrderNativeSqlService.class.getName());

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    /**
     * Loads the full print DTO for a PO approval bill.
     * Works for both PHARMACY_ORDER and PHARMACY_ORDER_APPROVAL atomics —
     * caller may pass either the approval bill ID or a request bill ID.
     * When a request bill ID is given (PHARMACY_ORDER), the query finds the
     * corresponding approval bill via referenceBill_ID.
     *
     * @param billId  ID of the approval bill (PHARMACY_ORDER_APPROVAL) or request
     *                bill (PHARMACY_ORDER). Must not be null.
     * @return populated DTO, or null if no bill found.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public PurchaseOrderPrintDto loadPrintDtoByBillId(long billId) {
        System.out.println(">>>PO_NATIVE_SERVICE: loadPrintDtoByBillId called, billId=" + billId);
        try {
            PurchaseOrderPrintDto result = doLoad(billId);
            System.out.println(">>>PO_NATIVE_SERVICE: result=" + (result == null ? "NULL" : "OK"));
            return result;
        } catch (Exception e) {
            System.out.println(">>>PO_NATIVE_SERVICE: EXCEPTION " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Failed to load PO print DTO for billId=" + billId, e);
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Private
    // -----------------------------------------------------------------------

    private PurchaseOrderPrintDto doLoad(long billId) {
        return loadHeader(billId);
    }


    @SuppressWarnings("unchecked")
    private PurchaseOrderPrintDto loadHeader(long billId) {
        // First resolve whether this is an approval bill or request bill,
        // and get the approval bill ID either way.
        String resolveAtomicSql =
            "SELECT billTypeAtomic, referenceBill_ID FROM bill WHERE ID = ?1 AND retired = 0 LIMIT 1";
        System.out.println(">>>PO_NATIVE_SERVICE: resolve SQL=" + resolveAtomicSql + " param=" + billId);
        List<Object[]> atomicRows;
        try {
            atomicRows = em.createNativeQuery(resolveAtomicSql)
                    .setParameter(1, billId)
                    .getResultList();
        } catch (Exception e) {
            System.out.println(">>>PO_NATIVE_SERVICE: resolve EXCEPTION " + e.getMessage());
            LOGGER.log(Level.WARNING, "Could not resolve bill atomic for billId=" + billId, e);
            return null;
        }
        System.out.println(">>>PO_NATIVE_SERVICE: atomicRows size=" + (atomicRows == null ? "null" : atomicRows.size()));
        if (atomicRows == null || atomicRows.isEmpty()) {
            return null;
        }
        Object[] atomicRow = atomicRows.get(0);
        String billTypeAtomic = atomicRow[0] != null ? atomicRow[0].toString() : "";
        System.out.println(">>>PO_NATIVE_SERVICE: billTypeAtomic=" + billTypeAtomic + " referenceBill_ID=" + atomicRow[1]);
        long approvalBillId;
        if ("PHARMACY_ORDER_APPROVAL".equals(billTypeAtomic)) {
            approvalBillId = billId;
        } else if ("PHARMACY_ORDER".equals(billTypeAtomic)) {
            if (atomicRow[1] == null) {
                LOGGER.log(Level.WARNING, "No approval bill linked for request billId=" + billId);
                System.out.println(">>>PO_NATIVE_SERVICE: no approval bill linked, returning null");
                return null;
            }
            approvalBillId = ((Number) atomicRow[1]).longValue();
        } else {
            LOGGER.log(Level.WARNING, "Unsupported billTypeAtomic for native PO print: " + billTypeAtomic + ", billId=" + billId);
            System.out.println(">>>PO_NATIVE_SERVICE: unsupported billTypeAtomic=" + billTypeAtomic);
            return null;
        }
        System.out.println(">>>PO_NATIVE_SERVICE: approvalBillId=" + approvalBillId);

        String sql =
            "SELECT " +
            "  ab.ID, ab.deptId, ab.paymentMethod, ab.creditDuration, " +
            "  ab.createdAt, ab.consignment, ab.comments, ab.approveAt, " +
            "  ab.netTotal, ab.cancelled, " +
            // approver
            "  approverPerson.name            AS approvedByName, " +
            // order dept
            "  orderDept.name                 AS orderDeptName, " +
            "  orderDept.address              AS orderDeptAddress, " +
            "  orderDept.telephone1           AS orderDeptTel1, " +
            "  orderDept.telephone2           AS orderDeptTel2, " +
            "  orderDept.fax                  AS orderDeptFax, " +
            // order institution
            "  orderInst.name                 AS orderInstName, " +
            "  orderInst.email                AS orderInstEmail, " +
            // order site
            "  orderSite.name                 AS orderSiteName, " +
            // supplier
            "  supplier.name                  AS supplierName, " +
            "  supplier.code                  AS supplierCode, " +
            // request pre-bill
            "  rb.createdAt                   AS poDate, " +
            "  rb.comments                    AS requestComments, " +
            "  rb.checkeAt                    AS preparedAt, " +
            "  preparerPerson.name            AS preparedByName, " +
            // institution from request creater
            "  reqCreaterInst.name            AS instName, " +
            "  reqCreaterInst.phone           AS instPhone, " +
            "  reqCreaterInst.fax             AS instFax, " +
            "  reqCreaterDept.telephone1      AS reqDeptTel1, " +
            "  reqCreaterDept.email           AS reqDeptEmail " +
            "FROM bill ab " +
            "LEFT JOIN bill rb                ON rb.ID = ab.referenceBill_ID " +
            "LEFT JOIN webuser approverWU      ON approverWU.ID = ab.creater_ID " +
            "LEFT JOIN person  approverPerson  ON approverPerson.ID = approverWU.webUserPerson_ID " +
            "LEFT JOIN department orderDept    ON orderDept.ID = ab.department_ID " +
            "LEFT JOIN institution orderInst   ON orderInst.ID = orderDept.institution_ID " +
            "LEFT JOIN institution orderSite    ON orderSite.ID = orderDept.site_ID " +
            "LEFT JOIN institution supplier    ON supplier.ID = ab.toInstitution_ID " +
            "LEFT JOIN webuser checkedByWU     ON checkedByWU.ID = rb.checkedBy_ID " +
            "LEFT JOIN person  preparerPerson  ON preparerPerson.ID = checkedByWU.webUserPerson_ID " +
            "LEFT JOIN webuser reqCreaterWU    ON reqCreaterWU.ID = rb.creater_ID " +
            "LEFT JOIN department reqCreaterDept ON reqCreaterDept.ID = reqCreaterWU.department_ID " +
            "LEFT JOIN institution reqCreaterInst ON reqCreaterInst.ID = reqCreaterDept.institution_ID " +
            "WHERE ab.ID = ?1 AND ab.retired = 0 " +
            "LIMIT 1";

        List<Object[]> rows;
        try {
            rows = em.createNativeQuery(sql)
                    .setParameter(1, approvalBillId)
                    .getResultList();
        } catch (Exception e) {
            System.out.println(">>>PO_NATIVE_SERVICE: header query EXCEPTION " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Header query failed for approvalBillId=" + approvalBillId, e);
            return null;
        }
        System.out.println(">>>PO_NATIVE_SERVICE: header rows size=" + (rows == null ? "null" : rows.size()));
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        Object[] r = rows.get(0);
        int col = 0;

        PurchaseOrderPrintDto dto = new PurchaseOrderPrintDto();
        col++; // ab.ID — skip, just used for routing
        dto.setPoNumber(str(r[col++]));
        dto.setPaymentMethod(str(r[col++]));
        dto.setCreditDuration(r[col] != null ? ((Number) r[col]).intValue() : 0); col++;
        dto.setPoTime(toDate(r[col++]));
        dto.setConsignment(r[col] != null && ((Number) r[col]).intValue() != 0); col++;
        dto.setApproveComments(str(r[col++]));
        dto.setApprovedAt(toDate(r[col++]));
        dto.setNetTotal(r[col] != null ? ((Number) r[col]).doubleValue() : 0.0); col++;
        dto.setCancelled(r[col] != null && ((Number) r[col]).intValue() != 0); col++;

        dto.setApprovedByName(str(r[col++]));

        dto.setOrderDepartmentName(str(r[col++]));
        dto.setOrderDepartmentAddress(str(r[col++]));
        dto.setOrderDepartmentTelephone1(str(r[col++]));
        dto.setOrderDepartmentTelephone2(str(r[col++]));
        dto.setOrderDepartmentFax(str(r[col++]));
        dto.setOrderInstitutionName(str(r[col++]));
        dto.setOrderInstitutionEmail(str(r[col++]));
        dto.setOrderSiteName(str(r[col++]));

        dto.setSupplierName(str(r[col++]));
        dto.setSupplierCode(str(r[col++]));

        dto.setPoDate(toDate(r[col++]));
        dto.setRequestComments(str(r[col++]));
        dto.setPreparedAt(toDate(r[col++]));
        dto.setPreparedByName(str(r[col++]));

        dto.setInstitutionName(str(r[col++]));
        dto.setInstitutionPhone(str(r[col++]));
        dto.setInstitutionFax(str(r[col++]));
        dto.setDepartmentTelephone1(str(r[col++]));
        dto.setDepartmentEmail(str(r[col]));

        // Store approval bill ID on DTO for item loading
        dto.getItems(); // ensure list initialised
        loadItems(dto, approvalBillId);
        return dto;
    }

    @SuppressWarnings("unchecked")
    private void loadItems(PurchaseOrderPrintDto dto, long approvalBillId) {
        String sql =
            "SELECT " +
            "  i.name               AS itemName, " +
            "  i.code               AS itemCode, " +
            "  iu.name              AS issueUnitName, " +
            "  COALESCE(bifd.lineGrossRate, 0)   AS purchaseRate, " +
            "  COALESCE(bifd.quantity, 0)         AS quantity, " +
            "  COALESCE(bifd.freeQuantity, 0)     AS freeQuantity, " +
            "  COALESCE(bifd.lineGrossTotal, 0)   AS lineTotal " +
            "FROM billitem bi " +
            "LEFT JOIN item i                    ON i.ID = bi.item_ID " +
            "LEFT JOIN category iu               ON iu.ID = i.issueUnit_ID " +
            "LEFT JOIN billitemfinancedetails bifd ON bifd.ID = bi.BILLITEMFINANCEDETAILS_ID " +
            "WHERE bi.bill_ID = ?1 " +
            "  AND bi.retired = 0 " +
            "ORDER BY bi.ID";

        List<Object[]> rows;
        try {
            rows = em.createNativeQuery(sql)
                    .setParameter(1, approvalBillId)
                    .getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Item query failed for approvalBillId=" + approvalBillId, e);
            return;
        }

        List<PurchaseOrderItemPrintDto> items = new ArrayList<>();
        if (rows != null) {
            for (Object[] r : rows) {
                PurchaseOrderItemPrintDto item = new PurchaseOrderItemPrintDto();
                item.setItemName(str(r[0]));
                item.setItemCode(str(r[1]));
                item.setIssueUnitName(str(r[2]));
                item.setPurchaseRate(r[3] != null ? ((Number) r[3]).doubleValue() : 0.0);
                item.setQuantity(r[4] != null ? ((Number) r[4]).doubleValue() : 0.0);
                item.setFreeQuantity(r[5] != null ? ((Number) r[5]).doubleValue() : 0.0);
                item.setLineTotal(r[6] != null ? ((Number) r[6]).doubleValue() : 0.0);
                items.add(item);
            }
        }
        dto.setItems(items);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String str(Object o) {
        return o != null ? o.toString().trim() : "";
    }

    private Date toDate(Object o) {
        if (o == null) return null;
        if (o instanceof Timestamp) return new java.util.Date(((Timestamp) o).getTime());
        if (o instanceof java.util.Date) return (java.util.Date) o;
        return null;
    }
}
