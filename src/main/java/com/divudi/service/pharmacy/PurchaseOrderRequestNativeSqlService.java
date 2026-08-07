package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BillType;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.PaymentMethod;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
}
