/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Native SQL replacements for PriceMatrixController lookups used in the
 * inpatient direct-issue add-item path.
 *
 * Avoids loading the Item entity (3 EAGER @OneToMany collections) and
 * InwardPriceAdjustment / InwardDiscountMatrix entities on every addBillItem()
 * call.  Each method issues 1–5 scalar native SQL queries instead.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class PriceMatrixNativeSqlService {

    private static final Logger LOGGER = Logger.getLogger(PriceMatrixNativeSqlService.class.getName());

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    private volatile String tItem = null;
    private volatile String tCategory = null;
    private volatile String tInwardPriceAdj = null;
    private volatile String tInwardDiscountMatrix = null;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Returns true if discount is allowed for the given item ID.
     * Replaces: itemFacade.find(itemId).isDiscountAllowed()
     */
    public boolean isDiscountAllowed(long itemId) {
        try {
            Object result = em.createNativeQuery(
                    "SELECT discountAllowed FROM " + itemTable() + " WHERE ID=?")
                    .setParameter(1, itemId)
                    .getSingleResult();
            return result != null && ((Number) result).intValue() != 0;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[PriceMatrixNative] isDiscountAllowed failed for itemId={0}: {1}",
                    new Object[]{itemId, e.getMessage()});
            return false;
        }
    }

    /**
     * Returns the inward margin percentage for the given item / department / grossValue.
     * Replaces: priceMatrixController.fetchInwardMargin(item, grossValue, dept).getMargin()
     *
     * Returns 0.0 when no matrix row is found.
     */
    public double getInwardMarginPct(long itemId, long deptId, double grossValue) {
        try {
            Long catId = getItemCategoryId(itemId);
            if (catId == null) return 0.0;

            Double margin = queryMargin(catId, deptId, grossValue);
            if (margin != null) return margin;

            Long parentCatId = getParentCategoryId(catId);
            if (parentCatId != null) {
                margin = queryMargin(parentCatId, deptId, grossValue);
                if (margin != null) return margin;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[PriceMatrixNative] getInwardMarginPct failed for itemId={0}: {1}",
                    new Object[]{itemId, e.getMessage()});
        }
        return 0.0;
    }

    /**
     * Returns the inward discount percentage for the given parameters.
     * Replaces: priceMatrixController.getInwardDiscountPercent(paymentMethod, scheme, admissionType, dept, item)
     *
     * Mirrors the fallback chain: item → category → parent category → department → global wildcard.
     * Returns 0.0 when no row is found or when paymentMethodName / admissionTypeId is null.
     */
    public double getInwardDiscountPct(long itemId, String paymentMethodName,
            Long schemeId, Long admissionTypeId, Long deptId) {
        if (paymentMethodName == null || admissionTypeId == null) {
            return 0.0;
        }
        try {
            Long catId = getItemCategoryId(itemId);

            // 1. By item
            Double pct = fetchDiscountPct(paymentMethodName, schemeId, admissionTypeId, null, null, itemId, null);
            if (pct == null && schemeId != null) {
                pct = fetchDiscountPct(paymentMethodName, null, admissionTypeId, null, null, itemId, null);
            }
            if (pct != null) return pct;

            if (catId != null) {
                // 2. By category
                pct = fetchDiscountPct(paymentMethodName, schemeId, admissionTypeId, deptId, catId, null, null);
                if (pct == null && schemeId != null) {
                    pct = fetchDiscountPct(paymentMethodName, null, admissionTypeId, deptId, catId, null, null);
                }
                if (pct != null) return pct;

                // 3. By parent category
                Long parentCatId = getParentCategoryId(catId);
                if (parentCatId != null) {
                    pct = fetchDiscountPct(paymentMethodName, schemeId, admissionTypeId, deptId, parentCatId, null, null);
                    if (pct == null && schemeId != null) {
                        pct = fetchDiscountPct(paymentMethodName, null, admissionTypeId, deptId, parentCatId, null, null);
                    }
                    if (pct != null) return pct;
                }
            }

            // 4. By department
            if (deptId != null) {
                pct = fetchDiscountPct(paymentMethodName, schemeId, admissionTypeId, deptId, null, null, null);
                if (pct == null && schemeId != null) {
                    pct = fetchDiscountPct(paymentMethodName, null, admissionTypeId, deptId, null, null, null);
                }
                if (pct != null) return pct;
            }

            // 5. Global wildcard
            pct = fetchDiscountPct(paymentMethodName, schemeId, admissionTypeId, null, null, null, null);
            if (pct == null && schemeId != null) {
                pct = fetchDiscountPct(paymentMethodName, null, admissionTypeId, null, null, null, null);
            }
            if (pct != null) return pct;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[PriceMatrixNative] getInwardDiscountPct failed for itemId={0}: {1}",
                    new Object[]{itemId, e.getMessage()});
        }
        return 0.0;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private Long getItemCategoryId(long itemId) {
        Object result = em.createNativeQuery(
                "SELECT category_ID FROM " + itemTable() + " WHERE ID=?")
                .setParameter(1, itemId)
                .getSingleResult();
        return result == null ? null : ((Number) result).longValue();
    }

    private Long getParentCategoryId(long catId) {
        try {
            Object result = em.createNativeQuery(
                    "SELECT parentCategory_ID FROM " + categoryTable() + " WHERE ID=?")
                    .setParameter(1, catId)
                    .getSingleResult();
            return result == null ? null : ((Number) result).longValue();
        } catch (Exception e) {
            return null;
        }
    }

    private Double queryMargin(long catId, long deptId, double grossValue) {
        @SuppressWarnings("unchecked")
        List<Object> rs = em.createNativeQuery(
                "SELECT margin FROM " + inwardPriceAdjTable()
                + " WHERE retired=0"
                + " AND category_ID=? AND department_ID=?"
                + " AND fromPrice < ? AND toPrice > ?"
                + " AND creditCompany_ID IS NULL"
                + " ORDER BY CASE WHEN admissionType_ID IS NULL THEN 1 ELSE 0 END ASC"
                + " LIMIT 1")
                .setParameter(1, catId)
                .setParameter(2, deptId)
                .setParameter(3, grossValue)
                .setParameter(4, grossValue)
                .getResultList();
        if (rs == null || rs.isEmpty() || rs.get(0) == null) return null;
        return ((Number) rs.get(0)).doubleValue();
    }

    /**
     * Core single-row fetch for InwardDiscountMatrix.
     * Mirrors fetchInwardDiscountMatrixPercentCore() in PriceMatrixController.
     * All nullable args become IS NULL filters; non-null args become equality filters.
     * paymentMethod and admissionType also match wildcard (NULL) rows in the matrix.
     */
    @SuppressWarnings("unchecked")
    private Double fetchDiscountPct(String paymentMethodName, Long schemeId, Long admissionTypeId,
            Long deptId, Long catId, Long itemId, Long creditCompanyId) {

        StringBuilder sql = new StringBuilder(
                "SELECT discountPercent FROM " + inwardDiscountMatrixTable()
                + " WHERE retired=0 AND inwardChargeType IS NULL");

        // NULL paymentMethod in matrix = all BHT types
        sql.append(" AND (paymentMethod=? OR paymentMethod IS NULL)");

        // NULL admissionType in matrix = all admission types
        sql.append(" AND (admissionType_ID=? OR admissionType_ID IS NULL)");

        if (schemeId != null) {
            sql.append(" AND paymentScheme_ID=?");
        } else {
            sql.append(" AND paymentScheme_ID IS NULL");
        }
        if (deptId != null) {
            sql.append(" AND department_ID=?");
        } else {
            sql.append(" AND department_ID IS NULL");
        }
        if (catId != null) {
            sql.append(" AND category_ID=?");
        } else {
            sql.append(" AND category_ID IS NULL");
        }
        if (itemId != null) {
            sql.append(" AND item_ID=?");
        } else {
            sql.append(" AND item_ID IS NULL");
        }
        if (creditCompanyId != null) {
            sql.append(" AND creditCompany_ID=?");
        } else {
            sql.append(" AND creditCompany_ID IS NULL");
        }

        // Prefer specific rows over wildcards (mirrors JPQL ORDER BY in PriceMatrixController)
        sql.append(" ORDER BY"
                + " CASE WHEN paymentMethod IS NULL THEN 1 ELSE 0 END ASC,"
                + " CASE WHEN admissionType_ID IS NULL THEN 1 ELSE 0 END ASC,"
                + " CASE WHEN department_ID IS NULL THEN 1 ELSE 0 END ASC,"
                + " CASE WHEN category_ID IS NULL THEN 1 ELSE 0 END ASC,"
                + " CASE WHEN item_ID IS NULL THEN 1 ELSE 0 END ASC"
                + " LIMIT 1");

        var query = em.createNativeQuery(sql.toString());
        int p = 1;
        query.setParameter(p++, paymentMethodName);
        query.setParameter(p++, admissionTypeId);
        if (schemeId != null)      query.setParameter(p++, schemeId);
        if (deptId != null)        query.setParameter(p++, deptId);
        if (catId != null)         query.setParameter(p++, catId);
        if (itemId != null)        query.setParameter(p++, itemId);
        if (creditCompanyId != null) query.setParameter(p++, creditCompanyId);

        try {
            List<Object> rs = query.getResultList();
            if (rs == null || rs.isEmpty() || rs.get(0) == null) return null;
            return ((Number) rs.get(0)).doubleValue();
        } catch (Exception e) {
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Table name resolution (INFORMATION_SCHEMA, cached after first call)
    // -----------------------------------------------------------------------

    private String resolveTable(String upperName) {
        Object name = em.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA=DATABASE() AND UPPER(TABLE_NAME)=? LIMIT 1")
                .setParameter(1, upperName)
                .getSingleResult();
        return name.toString();
    }

    private String itemTable() {
        if (tItem == null) tItem = resolveTable("ITEM");
        return tItem;
    }

    private String categoryTable() {
        if (tCategory == null) tCategory = resolveTable("CATEGORY");
        return tCategory;
    }

    private String inwardPriceAdjTable() {
        if (tInwardPriceAdj == null) tInwardPriceAdj = resolveTable("INWARDPRICEADJUSTMENT");
        return tInwardPriceAdj;
    }

    private String inwardDiscountMatrixTable() {
        if (tInwardDiscountMatrix == null) tInwardDiscountMatrix = resolveTable("INWARDDISCOUNTMATRIX");
        return tInwardDiscountMatrix;
    }
}
