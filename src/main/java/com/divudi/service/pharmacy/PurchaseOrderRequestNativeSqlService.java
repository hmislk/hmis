package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BillType;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.pharmacy.PurchaseOrderRequestLineData;
import com.divudi.core.entity.BillItemFinanceDetails;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Native SQL write path for the Purchase Order Request draft bill.
 * Only bill / billitem / pharmaceuticalbillitem are touched here —
 * BillItemFinanceDetails stays JPA (IDENTITY PK, calculation-heavy),
 * matching RetailSaleNativeSqlService's split.
 * Related issue: #22727
 */
@Stateless
public class PurchaseOrderRequestNativeSqlService {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    private String tBill;
    private String tBillItem;
    private String tPharmBillItem;

    public long createDraftBill(long departmentId, long institutionId, long createrId, String deptId, String insId) {
        Date now = new Date();
        em.createNativeQuery(
            "INSERT INTO " + billTable()
            + " (BILLTYPEATOMIC, billType, department_ID, institution_ID, fromDepartment_ID, fromInstitution_ID,"
            + " creater_ID, createdAt, checked, retired, cancelled, deptId, insId, netTotal, total)"
            + " VALUES (?,?,?,?,?,?,?,?,0,0,0,?,?,0,0)")
            .setParameter(1, BillTypeAtomic.PHARMACY_ORDER_PRE.toString())
            .setParameter(2, BillType.PharmacyOrder.toString())
            .setParameter(3, departmentId)
            .setParameter(4, institutionId)
            .setParameter(5, departmentId)
            .setParameter(6, institutionId)
            .setParameter(7, createrId)
            .setParameter(8, new Timestamp(now.getTime()))
            .setParameter(9, deptId)
            .setParameter(10, insId)
            .executeUpdate();
        long billId = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
        evictCache();
        return billId;
    }

    public void updateDraftBillHeader(long billId, Long toInstitutionId, PaymentMethod paymentMethod,
                                       int creditDuration, boolean consignment, DepartmentType departmentType,
                                       Long editorId) {
        em.createNativeQuery(
            "UPDATE " + billTable()
            + " SET toInstitution_ID=?, paymentMethod=?, creditDuration=?, consignment=?,"
            + " departmentType=?, editor_ID=?, editedAt=?"
            + " WHERE ID=?")
            .setParameter(1, toInstitutionId)
            .setParameter(2, paymentMethod != null ? paymentMethod.toString() : null)
            .setParameter(3, creditDuration)
            .setParameter(4, consignment ? 1 : 0)
            .setParameter(5, departmentType != null ? departmentType.toString() : null)
            .setParameter(6, editorId)
            .setParameter(7, new Timestamp(new Date().getTime()))
            .setParameter(8, billId)
            .executeUpdate();
        evictCache();
    }

    public boolean isBillChecked(long billId) {
        Object result = em.createNativeQuery(
            "SELECT checked FROM " + billTable() + " WHERE ID=?")
            .setParameter(1, billId)
            .getSingleResult();
        if (result instanceof Boolean) return (Boolean) result;
        if (result instanceof Number) return ((Number) result).intValue() != 0;
        return false;
    }

    private void evictCache() {
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(com.divudi.core.entity.Bill.class);
    }

    private String resolveTable(String upperName) {
        Object name = em.createNativeQuery(
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
            + "WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = ? LIMIT 1")
            .setParameter(1, upperName)
            .getSingleResult();
        return name.toString();
    }

    private String billTable() {
        if (tBill == null) tBill = resolveTable("BILL");
        return tBill;
    }

    // Forward-declared table helpers; used by line-item and finalization methods added in Task 3/4
    private String billItemTable() {
        if (tBillItem == null) tBillItem = resolveTable("BILLITEM");
        return tBillItem;
    }

    // Forward-declared table helpers; used by line-item and finalization methods added in Task 3/4
    private String pharmBillItemTable() {
        if (tPharmBillItem == null) tPharmBillItem = resolveTable("PHARMACEUTICALBILLITEM");
        return tPharmBillItem;
    }

    /**
     * Pure calculation extracted from legacy calculateLineValues() — no em access,
     * unit tested directly. AMPP items store rates per-pack on BillItemFinanceDetails
     * but per-unit on PharmaceuticalBillItem; non-AMPP items use the same rate for both.
     */
    static final class LineValues {
        final BigDecimal qty, freeQty, purchaseRate, retailRate, unitsPerPack;
        final BigDecimal grossValue, netValue, purchaseValue, retailValue, netRate;
        final double pbiQty, pbiFreeQty, pbiPurchaseRate, pbiRetailRate;

        LineValues(BigDecimal qty, BigDecimal freeQty, BigDecimal purchaseRate, BigDecimal retailRate,
                   BigDecimal unitsPerPack, BigDecimal grossValue, BigDecimal netValue,
                   BigDecimal purchaseValue, BigDecimal retailValue, BigDecimal netRate,
                   double pbiQty, double pbiFreeQty, double pbiPurchaseRate, double pbiRetailRate) {
            this.qty = qty; this.freeQty = freeQty; this.purchaseRate = purchaseRate; this.retailRate = retailRate;
            this.unitsPerPack = unitsPerPack; this.grossValue = grossValue; this.netValue = netValue;
            this.purchaseValue = purchaseValue; this.retailValue = retailValue; this.netRate = netRate;
            this.pbiQty = pbiQty; this.pbiFreeQty = pbiFreeQty;
            this.pbiPurchaseRate = pbiPurchaseRate; this.pbiRetailRate = pbiRetailRate;
        }
    }

    LineValues computeLineValues(PurchaseOrderRequestLineData line) {
        BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
        BigDecimal freeQty = line.getFreeQuantity() != null ? line.getFreeQuantity() : BigDecimal.ZERO;
        BigDecimal purchaseRate = line.getPurchaseRate() != null ? line.getPurchaseRate() : BigDecimal.ZERO;
        BigDecimal retailRate = line.getRetailRate() != null ? line.getRetailRate() : BigDecimal.ZERO;
        BigDecimal unitsPerPack = (line.getUnitsPerPack() != null && line.getUnitsPerPack().doubleValue() > 0)
                ? line.getUnitsPerPack() : BigDecimal.ONE;

        BigDecimal grossValue = purchaseRate.multiply(qty);
        BigDecimal netValue = grossValue;
        BigDecimal purchaseValue = purchaseRate.multiply(qty.add(freeQty));
        BigDecimal retailValue = retailRate.multiply(qty.add(freeQty));
        BigDecimal netRate = qty.doubleValue() > 0
                ? netValue.divide(qty, 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

        double pbiQty = line.isAmpp() ? qty.doubleValue() * unitsPerPack.doubleValue() : qty.doubleValue();
        double pbiFreeQty = line.isAmpp() ? freeQty.doubleValue() * unitsPerPack.doubleValue() : freeQty.doubleValue();
        double pbiPurchaseRate = line.isAmpp() ? purchaseRate.doubleValue() / unitsPerPack.doubleValue() : purchaseRate.doubleValue();
        double pbiRetailRate = line.isAmpp() ? retailRate.doubleValue() / unitsPerPack.doubleValue() : retailRate.doubleValue();

        return new LineValues(qty, freeQty, purchaseRate, retailRate, unitsPerPack, grossValue, netValue,
                purchaseValue, retailValue, netRate, pbiQty, pbiFreeQty, pbiPurchaseRate, pbiRetailRate);
    }

    public long saveLine(long billId, PurchaseOrderRequestLineData line) {
        LineValues v = computeLineValues(line);
        BigDecimal qty = v.qty, freeQty = v.freeQty, purchaseRate = v.purchaseRate, retailRate = v.retailRate;
        BigDecimal grossValue = v.grossValue, netValue = v.netValue, netRate = v.netRate;
        BigDecimal purchaseValue = v.purchaseValue, retailValue = v.retailValue, unitsPerPack = v.unitsPerPack;

        long billItemId;
        if (line.getBillItemId() == null) {
            em.createNativeQuery(
                "INSERT INTO " + billItemTable()
                + " (bill_ID, item_ID, qty, netValue, grossValue, Rate, netRate,"
                + " createdAt, creater_ID, retired, refunded, billItemRefunded,"
                + " consideredForCosting, inwardChargeType, searialNo)"
                + " VALUES (?,?,?,?,?,?,?,?,?,0,0,0,1,'Medicine',?)")
                .setParameter(1, billId)
                .setParameter(2, line.getItemId())
                .setParameter(3, qty.doubleValue())
                .setParameter(4, netValue.doubleValue())
                .setParameter(5, grossValue.doubleValue())
                .setParameter(6, purchaseRate.doubleValue())
                .setParameter(7, netRate.doubleValue())
                .setParameter(8, new Timestamp(new Date().getTime()))
                .setParameter(9, line.getCreaterId())
                .setParameter(10, line.getSerialNo())
                .executeUpdate();
            billItemId = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
        } else {
            billItemId = line.getBillItemId();
            em.createNativeQuery(
                "UPDATE " + billItemTable()
                + " SET qty=?, netValue=?, grossValue=?, Rate=?, netRate=? WHERE ID=?")
                .setParameter(1, qty.doubleValue())
                .setParameter(2, netValue.doubleValue())
                .setParameter(3, grossValue.doubleValue())
                .setParameter(4, purchaseRate.doubleValue())
                .setParameter(5, netRate.doubleValue())
                .setParameter(6, billItemId)
                .executeUpdate();
        }

        double pbiQty = v.pbiQty, pbiFreeQty = v.pbiFreeQty, pbiPurchaseRate = v.pbiPurchaseRate, pbiRetailRate = v.pbiRetailRate;

        if (line.getPharmaceuticalBillItemId() == null) {
            em.createNativeQuery(
                "INSERT INTO " + pharmBillItemTable()
                + " (billItem_ID, qty, freeQty, purchaseRate, purchaseRatePack, purchaseValue,"
                + " retailRate, retailRatePack, retailRateInUnit, retailValue, costRate, costRatePack, costValue,"
                + " createdAt, creater_ID)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,0,0,0,?,?)")
                .setParameter(1, billItemId)
                .setParameter(2, pbiQty)
                .setParameter(3, pbiFreeQty)
                .setParameter(4, pbiPurchaseRate)
                .setParameter(5, purchaseRate.doubleValue())
                .setParameter(6, purchaseValue.doubleValue())
                .setParameter(7, pbiRetailRate)
                .setParameter(8, retailRate.doubleValue())
                .setParameter(9, pbiRetailRate)
                .setParameter(10, retailValue.doubleValue())
                .setParameter(11, new Timestamp(new Date().getTime()))
                .setParameter(12, line.getCreaterId())
                .executeUpdate();
        } else {
            em.createNativeQuery(
                "UPDATE " + pharmBillItemTable()
                + " SET qty=?, freeQty=?, purchaseRate=?, purchaseRatePack=?, purchaseValue=?,"
                + " retailRate=?, retailRatePack=?, retailRateInUnit=?, retailValue=? WHERE billItem_ID=?")
                .setParameter(1, pbiQty)
                .setParameter(2, pbiFreeQty)
                .setParameter(3, pbiPurchaseRate)
                .setParameter(4, purchaseRate.doubleValue())
                .setParameter(5, purchaseValue.doubleValue())
                .setParameter(6, pbiRetailRate)
                .setParameter(7, retailRate.doubleValue())
                .setParameter(8, pbiRetailRate)
                .setParameter(9, retailValue.doubleValue())
                .setParameter(10, billItemId)
                .executeUpdate();
        }

        saveBillItemFinanceDetails(billItemId, line, qty, freeQty, purchaseRate, retailRate, netValue, grossValue,
                netRate, unitsPerPack, pbiQty, pbiFreeQty);

        evictLineCache();
        return billItemId;
    }

    private void saveBillItemFinanceDetails(long billItemId, PurchaseOrderRequestLineData line,
            BigDecimal qty, BigDecimal freeQty, BigDecimal purchaseRate, BigDecimal retailRate,
            BigDecimal netValue, BigDecimal grossValue, BigDecimal netRate, BigDecimal unitsPerPack,
            double quantityByUnits, double freeQuantityByUnits) {
        BillItemFinanceDetails bifd;
        if (line.getBillItemId() != null) {
            String jpql = "SELECT bi.billItemFinanceDetails FROM BillItem bi WHERE bi.id = :id";
            java.util.List<BillItemFinanceDetails> existing = em.createQuery(jpql, BillItemFinanceDetails.class)
                    .setParameter("id", billItemId)
                    .getResultList();
            bifd = (existing != null && !existing.isEmpty() && existing.get(0) != null) ? existing.get(0) : new BillItemFinanceDetails();
        } else {
            bifd = new BillItemFinanceDetails();
        }

        bifd.setLineNetRate(purchaseRate);
        bifd.setLineGrossRate(purchaseRate);
        bifd.setGrossRate(purchaseRate);
        bifd.setLineNetTotal(netValue);
        bifd.setLineGrossTotal(grossValue);
        bifd.setGrossTotal(grossValue);
        bifd.setQuantity(qty);
        bifd.setFreeQuantity(freeQty);
        bifd.setQuantityByUnits(BigDecimal.valueOf(quantityByUnits));
        bifd.setFreeQuantityByUnits(BigDecimal.valueOf(freeQuantityByUnits));
        bifd.setRetailSaleRate(retailRate);
        bifd.setUnitsPerPack(unitsPerPack);
        bifd.setValueAtPurchaseRate(purchaseRate.multiply(qty.add(freeQty)));
        bifd.setValueAtRetailRate(retailRate.multiply(qty.add(freeQty)));
        bifd.setLineCost(BigDecimal.ZERO);
        bifd.setLineCostRate(BigDecimal.ZERO);
        bifd.setValueAtCostRate(BigDecimal.ZERO);

        if (bifd.getId() == null) {
            bifd.setCreatedAt(new Date());
            em.persist(bifd);
            em.flush();
            em.createNativeQuery("UPDATE " + billItemTable() + " SET BILLITEMFINANCEDETAILS_ID=? WHERE ID=?")
                    .setParameter(1, bifd.getId())
                    .setParameter(2, billItemId)
                    .executeUpdate();
        } else {
            em.merge(bifd);
        }
    }

    private void evictLineCache() {
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(com.divudi.core.entity.BillItem.class);
        cache.evict(com.divudi.core.entity.pharmacy.PharmaceuticalBillItem.class);
        cache.evict(BillItemFinanceDetails.class);
    }
}
