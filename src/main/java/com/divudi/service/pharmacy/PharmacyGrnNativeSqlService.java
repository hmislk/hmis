/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.dto.pharmacy.GrnPrintHeaderDto;
import com.divudi.core.data.dto.pharmacy.GrnPrintItemDto;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Native SQL service for GRN bill print/view data.
 * Two queries (header + items); no JPA entity graph loading.
 * Follows the pattern established by PurchaseOrderNativeSqlService.
 *
 * Handles BillTypeAtomic: PHARMACY_GRN, PHARMACY_GRN_PRE,
 * PHARMACY_GRN_CANCELLED, PHARMACY_GRN_REFUND,
 * PHARMACY_WHOLESALE_GRN_BILL, PHARMACY_WHOLESALE_GRN_BILL_CANCELLED,
 * PHARMACY_WHOLESALE_GRN_BILL_REFUND.
 */
@Stateless
public class PharmacyGrnNativeSqlService {

    private static final Logger LOGGER =
            Logger.getLogger(PharmacyGrnNativeSqlService.class.getName());

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    // Table-name cache: resolved once per EJB instance to handle case-sensitive
    // MySQL servers where table names may be UPPER or lower case.
    private volatile Map<String, String> tableCache = null;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Loads the GRN header DTO for the given bill ID.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public GrnPrintHeaderDto loadHeaderByBillId(long billId) {
        try {
            return doLoadHeader(billId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load GRN header DTO for billId=" + billId, e);
            return null;
        }
    }

    /**
     * Loads the GRN item DTOs for the given bill ID.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<GrnPrintItemDto> loadItemsByBillId(long billId) {
        try {
            return doLoadItems(billId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load GRN item DTOs for billId=" + billId, e);
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Private — header
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private GrnPrintHeaderDto doLoadHeader(long billId) {
        String sql =
            "SELECT " +
            "  b.ID, " +
            "  b.deptId, " +
            "  b.invoiceNumber, " +
            "  b.createdAt, " +
            "  b.paymentMethod, " +
            "  b.creditDuration, " +
            "  b.consignment, " +
            "  b.total, " +
            "  b.expenseTotal, " +
            "  b.expensesTotalConsideredForCosting, " +
            "  b.expensesTotalNotConsideredForCosting, " +
            "  b.tax, " +
            "  b.discount, " +
            "  b.netTotal, " +
            "  b.cancelled, " +
            "  dept.printingName        AS deptPrintingName, " +
            "  dept.name                AS deptName, " +
            "  dept.address             AS deptAddress, " +
            "  dept.telephone1          AS deptTel1, " +
            "  dept.telephone2          AS deptTel2, " +
            "  dept.fax                 AS deptFax, " +
            "  dept.email               AS deptEmail, " +
            "  rb.deptId                AS poNumber, " +
            "  rb.createdAt             AS poCreatedAt, " +
            "  poApproverPerson.name    AS poApproverName, " +
            "  supp.name                AS supplierName, " +
            "  grnCreaterPerson.name    AS createdByName, " +
            "  COALESCE(bfd.totalBillValue, 0)        AS totalBillValue, " +
            "  COALESCE(bfd.totalCostValue, 0)        AS totalCostValue, " +
            "  COALESCE(bfd.totalRetailSaleValue, 0)  AS totalRetailSaleValue " +
            "FROM " + t("BILL") + " b " +
            "LEFT JOIN " + t("DEPARTMENT") + " dept              ON dept.ID = b.department_ID " +
            "LEFT JOIN " + t("BILL") + " rb                      ON rb.ID = b.referenceBill_ID " +
            "LEFT JOIN " + t("WEBUSER") + " poApproverWU         ON poApproverWU.ID = rb.creater_ID " +
            "LEFT JOIN " + t("PERSON") + "  poApproverPerson     ON poApproverPerson.ID = poApproverWU.webUserPerson_ID " +
            "LEFT JOIN " + t("INSTITUTION") + " supp             ON supp.ID = b.fromInstitution_ID " +
            "LEFT JOIN " + t("WEBUSER") + " grnCreaterWU         ON grnCreaterWU.ID = b.creater_ID " +
            "LEFT JOIN " + t("PERSON") + "  grnCreaterPerson     ON grnCreaterPerson.ID = grnCreaterWU.webUserPerson_ID " +
            "LEFT JOIN " + t("BILLFINANCEDETAILS") + " bfd       ON bfd.ID = b.BILLFINANCEDETAILS_ID " +
            "WHERE b.ID = ?1 " +
            "  AND b.retired = 0 " +
            "LIMIT 1";

        List<Object[]> rows;
        try {
            rows = em.createNativeQuery(sql)
                    .setParameter(1, billId)
                    .getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Header query failed for billId=" + billId, e);
            return null;
        }

        if (rows == null || rows.isEmpty()) {
            return null;
        }

        Object[] r = rows.get(0);
        int col = 0;

        GrnPrintHeaderDto dto = new GrnPrintHeaderDto();
        dto.setBillId(r[col] != null ? ((Number) r[col]).longValue() : 0L); col++;
        dto.setGrnNumber(str(r[col++]));
        dto.setInvoiceNumber(str(r[col++]));
        dto.setCreatedAt(toDate(r[col++]));
        dto.setPaymentMethod(str(r[col++]));
        dto.setCreditDuration(r[col] != null ? ((Number) r[col]).intValue() : 0); col++;
        dto.setConsignment(toBool(r[col++]));
        dto.setTotal(toDouble(r[col++]));
        dto.setExpenseTotal(toDouble(r[col++]));
        dto.setExpensesConsideredForCosting(toDouble(r[col++]));
        dto.setExpensesNotConsideredForCosting(toDouble(r[col++]));
        dto.setTax(toDouble(r[col++]));
        dto.setDiscount(toDouble(r[col++]));
        dto.setNetTotal(toDouble(r[col++]));
        dto.setCancelled(toBool(r[col++]));
        dto.setDepartmentPrintingName(str(r[col++]));
        dto.setDepartmentName(str(r[col++]));
        dto.setDepartmentAddress(str(r[col++]));
        dto.setDepartmentTelephone1(str(r[col++]));
        dto.setDepartmentTelephone2(str(r[col++]));
        dto.setDepartmentFax(str(r[col++]));
        dto.setDepartmentEmail(str(r[col++]));
        dto.setPoNumber(str(r[col++]));
        dto.setPoCreatedAt(toDate(r[col++]));
        dto.setPoApproverName(str(r[col++]));
        dto.setSupplierName(str(r[col++]));
        dto.setCreatedByName(str(r[col++]));
        dto.setTotalBillValue(toDouble(r[col++]));
        dto.setTotalCostValue(toDouble(r[col++]));
        dto.setTotalRetailSaleValue(toDouble(r[col]));

        return dto;
    }

    // -----------------------------------------------------------------------
    // Private — items
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<GrnPrintItemDto> doLoadItems(long billId) {
        String sql =
            "SELECT " +
            "  i.name                                   AS itemName, " +
            "  i.code                                   AS itemCode, " +
            "  COALESCE(iu.name, '')                    AS issueUnitName, " +
            "  COALESCE(bifd.quantity, 0)               AS quantity, " +
            "  COALESCE(bifd.freeQuantity, 0)           AS freeQuantity, " +
            "  COALESCE(bifd.lineGrossRate, 0)          AS lineGrossRate, " +
            "  COALESCE(bifd.lineDiscountRate, 0)       AS lineDiscountRate, " +
            "  COALESCE(bifd.lineNetTotal, 0)           AS lineNetTotal, " +
            "  COALESCE(bifd.retailSaleRate, 0)         AS retailSaleRate, " +
            "  COALESCE(bifd.retailSaleRatePerUnit, 0)  AS retailSaleRatePerUnit, " +
            "  COALESCE(bifd.totalCost, 0)              AS totalCost, " +
            "  COALESCE(bifd.totalQuantityByUnits, 0)   AS totalQuantityByUnits, " +
            "  pbi.doe                                   AS doe, " +
            "  pbi.stringValue                           AS batchNumber " +
            "FROM " + t("BILLITEM") + " bi " +
            "LEFT JOIN " + t("ITEM") + " i                             ON i.ID = bi.item_ID " +
            "LEFT JOIN " + t("CATEGORY") + " iu                        ON iu.ID = i.issueUnit_ID " +
            "LEFT JOIN " + t("BILLITEMFINANCEDETAILS") + " bifd        ON bifd.ID = bi.BILLITEMFINANCEDETAILS_ID " +
            "LEFT JOIN " + t("PHARMACEUTICALBILLITEM") + " pbi         ON pbi.billItem_ID = bi.ID " +
            "WHERE bi.bill_ID = ?1 " +
            "  AND bi.retired = 0 " +
            "ORDER BY bi.ID";

        List<Object[]> rows;
        try {
            rows = em.createNativeQuery(sql)
                    .setParameter(1, billId)
                    .getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Item query failed for billId=" + billId, e);
            return new ArrayList<>();
        }

        List<GrnPrintItemDto> items = new ArrayList<>();
        if (rows != null) {
            for (Object[] r : rows) {
                GrnPrintItemDto item = new GrnPrintItemDto();
                item.setItemName(str(r[0]));
                item.setItemCode(str(r[1]));
                item.setIssueUnitName(str(r[2]));
                item.setQuantity(toDouble(r[3]));
                item.setFreeQuantity(toDouble(r[4]));
                item.setLineGrossRate(toDouble(r[5]));
                item.setLineDiscountRate(toDouble(r[6]));
                item.setLineNetTotal(toDouble(r[7]));
                item.setRetailSaleRate(toDouble(r[8]));
                item.setRetailSaleRatePerUnit(toDouble(r[9]));
                item.setTotalCost(toDouble(r[10]));
                item.setTotalQuantityByUnits(toDouble(r[11]));
                item.setDoe(toDate(r[12]));
                item.setBatchNumber(str(r[13]));
                items.add(item);
            }
        }
        return items;
    }

    // -----------------------------------------------------------------------
    // Table name resolution — lazy, per-instance cache
    // -----------------------------------------------------------------------

    private String t(String upperName) {
        if (tableCache == null) {
            tableCache = buildTableCache();
        }
        return tableCache.getOrDefault(upperName, upperName.toLowerCase());
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> buildTableCache() {
        String[] names = {
            "BILL", "DEPARTMENT", "WEBUSER", "PERSON", "INSTITUTION",
            "BILLFINANCEDETAILS", "BILLITEM", "ITEM", "CATEGORY",
            "BILLITEMFINANCEDETAILS", "PHARMACEUTICALBILLITEM"
        };
        Map<String, String> map = new HashMap<>();
        for (String n : names) {
            try {
                List<Object> rows = em.createNativeQuery(
                        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = ? LIMIT 1")
                        .setParameter(1, n)
                        .getResultList();
                map.put(n, rows.isEmpty() ? n.toLowerCase() : rows.get(0).toString());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not resolve table name for " + n + "; defaulting to lowercase", e);
                map.put(n, n.toLowerCase());
            }
        }
        return map;
    }

    // -----------------------------------------------------------------------
    // Type-safe helpers
    // -----------------------------------------------------------------------

    private String str(Object o) {
        return o != null ? o.toString().trim() : "";
    }

    private double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        return 0.0;
    }

    private boolean toBool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof Number) return ((Number) o).intValue() != 0;
        return false;
    }

    private Date toDate(Object o) {
        if (o == null) return null;
        if (o instanceof Timestamp) return new Date(((Timestamp) o).getTime());
        if (o instanceof java.sql.Date) return new Date(((java.sql.Date) o).getTime());
        if (o instanceof Date) return (Date) o;
        return null;
    }
}
