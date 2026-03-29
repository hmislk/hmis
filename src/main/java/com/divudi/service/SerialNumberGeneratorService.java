package com.divudi.service;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.SessionNumberType;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Staff;
import com.divudi.core.facade.BillItemFacade;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Stateless
public class SerialNumberGeneratorService {

    @EJB
    BillItemFacade billItemFacade;

    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    // <editor-fold defaultstate="collapsed" desc="Lock Keys">
    // Bill
    private String getLockKey(Department department, SessionNumberType type) {
        return department.getId() + type.toString();
    }

    // Category
    private String getLockKey(Department department, Category categorye, SessionNumberType type) {
        return department.getId() + "-" + categorye.getId() + "-" + type.toString();
    }

    // Staff
    private String getLockKey(Department department, Staff staff, SessionNumberType type) {
        return department.getId() + "-" + staff.getId() + "-" + type.toString();
    }

    //Item
    private String getLockKey(Department department, Item item) {
        return department.getId() + "-" + item.getId() + "-" + item.getSessionNumberType().toString();
    }

    //ItemDeDepartment
    private String getLockKey(Department department, Department itemDeDepartment, SessionNumberType type) {
        return department.getId() + "-" + itemDeDepartment.getId() + "-" + type.toString();
    }
    // </editor-fold>

    //ByBill
    public String fetchLastSerialNumberForDayUsingBill(Department department, BillItem billItem) {
        if (department == null || billItem == null || billItem.getItem().getSessionNumberType() == null) {
            return "";
        }

        String lockKey = getLockKey(department, billItem.getItem().getSessionNumberType());
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateDailyBillItemSerialNumberByBill(department, billItem.getItem().getSessionNumberType());
        } finally {
            lock.unlock();
        }
    }

    //ByItem
    public String fetchLastSerialNumberForDayUsingItem(Department department, BillItem billItem) {
        if (department == null || billItem == null || billItem.getItem() == null) {
            return "";
        }

        String lockKey = getLockKey(department, billItem.getItem());
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateDailyBillItemSerialNumberByItem(department, billItem.getItem());
        } finally {
            lock.unlock();
        }
    }

    //ByItemDepartment
    public String fetchLastSerialNumberForDayUsingItemDeDepartment(Department department, BillItem billItem) {
        if (department == null || billItem == null || billItem.getItem() == null || billItem.getItem().getDepartment() == null) {
            return "";
        }
        if (billItem.getItem().getSessionNumberType() == null) {
            return "";
        }

        Department itemDepartment = billItem.getItem().getDepartment();
        SessionNumberType type = billItem.getItem().getSessionNumberType();

        String lockKey = getLockKey(department, itemDepartment, type);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateDailyBillItemSerialNumberByItemDepartment(department, itemDepartment, type);
        } finally {
            lock.unlock();
        }
    }

    //ByCategory or BySubCategory
    public String fetchLastSerialNumberForDayUsingCategory(Department department, BillItem billItem) {
        if (department == null || billItem == null || billItem.getItem() == null || billItem.getItem().getCategory() == null) {
            return "";
        }
        
        if(billItem.getItem().getSessionNumberType() == null){
            return "";
        }

        SessionNumberType type = billItem.getItem().getSessionNumberType();
        Category category = billItem.getItem().getCategory();

        String lockKey = getLockKey(department, category, type);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateDailyBillItemSerialNumberByCategory(department, category, type);
        } finally {
            lock.unlock();
        }

    }

    //ByDoctor or //ByDoctorSession
    public String fetchLastSerialNumberForDayUsingDoctor(Department department, BillItem billItem) {
        if (department == null || billItem == null || billItem.getPrimaryStaff() == null) {
            return "";
        }
        if (billItem.getItem() ==null || billItem.getItem().getSessionNumberType() == null) {
            return "";
        }

        SessionNumberType type = billItem.getItem().getSessionNumberType();

        String lockKey = getLockKey(department, billItem.getPrimaryStaff(), type);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateDailyBillItemSerialNumberByDoctor(department, billItem.getPrimaryStaff(), type);
        } finally {
            lock.unlock();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Find Session Number">
    private String generateDailyBillItemSerialNumberByBill(Department department, SessionNumberType type) {
        if (department == null || type == null) {
            return "";
        }

        Calendar calStart = Calendar.getInstance();
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);
        calStart.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calStart.getTime();

        Calendar calEnd = Calendar.getInstance();
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);
        calEnd.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calEnd.getTime();

        String jpql = "SELECT count(bi) FROM BillItem bi "
                + " where bi.bill.createdAt between :fd and :td "
                + " and bi.bill.billTypeAtomic =:type"
                + " and bi.bill.department=:dep "
                + " and bi.retired =:ret"
                + " and bi.item.sessionNumberType =:sType";
        HashMap hm = new HashMap();
        hm.put("type", BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        hm.put("dep", department);
        hm.put("fd", startOfDay);
        hm.put("td", endOfDay);
        hm.put("sType", type);
        hm.put("ret", false);

        Long dd = billItemFacade.findAggregateLong(jpql, hm, TemporalType.TIMESTAMP);

        return (dd != null) ? String.valueOf(dd) : "0";
    }

    private String generateDailyBillItemSerialNumberByCategory(Department department, Category category, SessionNumberType type) {
        if (department == null || category == null || type == null) {
            return "";
        }

        Calendar calStart = Calendar.getInstance();
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);
        calStart.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calStart.getTime();

        Calendar calEnd = Calendar.getInstance();
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);
        calEnd.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calEnd.getTime();

        String jpql = "SELECT count(bi) FROM BillItem bi "
                + " where bi.bill.billDate between :fd and :td "
                + " and bi.bill.department=:dep "
                + " and bi.bill.billTypeAtomic =:type"
                + " and bi.item.category=:cat "
                + " and bi.retired =:ret"
                + " and bi.item.sessionNumberType =:sType";
        HashMap hm = new HashMap();
        hm.put("sType", type);
        hm.put("type", BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        hm.put("dep", department);
        hm.put("cat", category);
        hm.put("fd", startOfDay);
        hm.put("td", endOfDay);
        hm.put("ret", false);

        Long dd = billItemFacade.findAggregateLong(jpql, hm, TemporalType.TIMESTAMP);

        return (dd != null) ? String.valueOf(dd) : "0";
    }

    private String generateDailyBillItemSerialNumberByItem(Department department, Item item) {
        if (department == null || item == null || item.getSessionNumberType() == null) {
            return "";
        }

        Calendar calStart = Calendar.getInstance();
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);
        calStart.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calStart.getTime();

        Calendar calEnd = Calendar.getInstance();
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);
        calEnd.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calEnd.getTime();

        String jpql = "SELECT count(bi) FROM BillItem bi "
                + " where bi.bill.billDate between :fd and :td "
                + " and bi.bill.department=:dep "
                + " and bi.bill.billTypeAtomic =:type"
                + " and bi.item=:item "
                + " and bi.retired =:ret"
                + " and bi.item.sessionNumberType =:sType";
        HashMap hm = new HashMap();
        hm.put("sType", item.getSessionNumberType());
        hm.put("dep", department);
        hm.put("type", BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        hm.put("item", item);
        hm.put("fd", startOfDay);
        hm.put("td", endOfDay);
        hm.put("ret", false);

        Long dd = billItemFacade.findAggregateLong(jpql, hm, TemporalType.TIMESTAMP);

        return (dd != null) ? String.valueOf(dd) : "0";
    }

    private String generateDailyBillItemSerialNumberByItemDepartment(Department department, Department itemDepartment, SessionNumberType type) {
        if (department == null || itemDepartment == null || type == null) {
            return "";
        }

        Calendar calStart = Calendar.getInstance();
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);
        calStart.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calStart.getTime();

        Calendar calEnd = Calendar.getInstance();
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);
        calEnd.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calEnd.getTime();

        String jpql = "SELECT count(bi) FROM BillItem bi "
                + " where bi.bill.billDate between :fd and :td "
                + " and bi.bill.department=:dep "
                + " and bi.bill.billTypeAtomic =:type"
                + " and bi.item.department=:itDep "
                + " and bi.retired =:ret"
                + " and bi.item.sessionNumberType =:sType";
        HashMap hm = new HashMap();
        hm.put("sType", type);
        hm.put("dep", department);
        hm.put("type", BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        hm.put("itDep", itemDepartment);
        hm.put("fd", startOfDay);
        hm.put("td", endOfDay);
        hm.put("ret", false);

        Long dd = billItemFacade.findAggregateLong(jpql, hm, TemporalType.TIMESTAMP);

        return (dd != null) ? String.valueOf(dd) : "0";
    }

    private String generateDailyBillItemSerialNumberByDoctor(Department department, Staff staff, SessionNumberType type) {
        if (department == null || staff == null || type == null) {
            return "";
        }

        Calendar calStart = Calendar.getInstance();
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);
        calStart.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calStart.getTime();

        Calendar calEnd = Calendar.getInstance();
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);
        calEnd.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calEnd.getTime();

        String jpql = "SELECT count(bi) FROM BillItem bi "
                + " where bi.bill.billDate between :fd and :td "
                + " and bi.bill.department=:dep "
                + " and bi.bill.billTypeAtomic =:type"
                + " and bi.primaryStaff=:sff "
                + " and bi.retired =:ret"
                + " and bi.item.sessionNumberType =:sType";
        HashMap hm = new HashMap();
        hm.put("sType", type);
        hm.put("dep", department);
        hm.put("sff", staff);
        hm.put("type", BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        hm.put("fd", startOfDay);
        hm.put("td", endOfDay);
        hm.put("ret", false);

        Long dd = billItemFacade.findAggregateLong(jpql, hm, TemporalType.TIMESTAMP);

        return (dd != null) ? String.valueOf(dd) : "0";
    }
    // </editor-fold>

}
