package com.divudi.service;

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
    private String getLockKey(Department department) {
        return department.getId().toString();
    }

    // Category
    private String getLockKey(Department department, Category categorye) {
        return department.getId() + "-" + categorye.getId();
    }

    // Staff
    private String getLockKey(Department department, Staff staff) {
        return department.getId() + "-" + staff.getId();
    }

    //Item
    private String getLockKey(Department department, Item item) {
        return department.getId() + "-" + item.getId();
    }

    //ItemDeDepartment
    private String getLockKey(Department department, Department itemDeDepartment) {
        return department.getId() + "-" + itemDeDepartment.getId();
    }
    // </editor-fold>

    //ByBill
    public String fetchLastSerialNumberForDayUsingBill(Department department) {
        if (department == null) {
            return "";
        }

        String lockKey = getLockKey(department);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateDailyBillItemSerialNumberByBill(department);
        } finally {
            lock.unlock();
        }
    }

    //ByItem
    public String fetchLastSerialNumberForDayUsingItem(Department department, Item item) {
        if (department == null || item == null) {
            return "";
        }

        String lockKey = getLockKey(department, item);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateDailyBillItemSerialNumberByItem(department, item);
        } finally {
            lock.unlock();
        }
    }

    //ByItemDepartment
    public String fetchLastSerialNumberForDayUsingItemDeDepartment(Department department, Department itemDepartment) {
        if (department == null || itemDepartment == null) {
            return "";
        }

        String lockKey = getLockKey(department, itemDepartment);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateDailyBillItemSerialNumberByItemDepartment(department, itemDepartment);
        } finally {
            lock.unlock();
        }
    }

    //ByCategory or BySubCategory
    public String fetchLastSerialNumberForDayUsingCategory(Department department, Category category) {
        if (department == null || category == null) {
            return "";
        }

        String lockKey = getLockKey(department, category);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateDailyBillItemSerialNumberByCategory(department, category);
        } finally {
            lock.unlock();
        }

    }

    //ByDoctor or //ByDoctorSession
    public String fetchLastSerialNumberForDayUsingDoctor(Department department, Staff staff) {
        if (department == null || staff == null) {
            return "";
        }

        String lockKey = getLockKey(department, staff);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateDailyBillItemSerialNumberByDoctor(department, staff);
        } finally {
            lock.unlock();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Find Session Number">
    private String generateDailyBillItemSerialNumberByBill(Department department) {
        if (department == null) {
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

        String jpql = "SELECT count(b) FROM Bill b "
                + " where b.createdAt between :fd and :td "
                + " and b.department=:dep ";
        HashMap hm = new HashMap();

        hm.put("dep", department);
        hm.put("fd", startOfDay);
        hm.put("td", endOfDay);

        Long dd = billItemFacade.findAggregateLong(jpql, hm, TemporalType.TIMESTAMP);

        return (dd != null) ? String.valueOf(dd) : "0";
    }

    private String generateDailyBillItemSerialNumberByCategory(Department department, Category category) {
        if (department == null) {
            return "";
        }
        if (category == null) {
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
                + " and bi.item.category=:cat ";
        HashMap hm = new HashMap();

        hm.put("dep", department);
        hm.put("cat", category);
        hm.put("fd", startOfDay);
        hm.put("td", endOfDay);

        Long dd = billItemFacade.findAggregateLong(jpql, hm, TemporalType.TIMESTAMP);

        return (dd != null) ? String.valueOf(dd) : "0";
    }

    private String generateDailyBillItemSerialNumberByItem(Department department, Item item) {
        if (department == null) {
            return "";
        }
        if (item == null) {
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
                + " and bi.item=:item ";
        HashMap hm = new HashMap();

        hm.put("dep", department);
        hm.put("item", item);
        hm.put("fd", startOfDay);
        hm.put("td", endOfDay);

        Long dd = billItemFacade.findAggregateLong(jpql, hm, TemporalType.TIMESTAMP);

        return (dd != null) ? String.valueOf(dd) : "0";
    }

    private String generateDailyBillItemSerialNumberByItemDepartment(Department department, Department itemDepartment) {
        if (department == null) {
            return "";
        }
        if (itemDepartment == null) {
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
                + " and bi.item.department=:itDep ";
        HashMap hm = new HashMap();

        hm.put("dep", department);
        hm.put("itDep", itemDepartment);
        hm.put("fd", startOfDay);
        hm.put("td", endOfDay);

        Long dd = billItemFacade.findAggregateLong(jpql, hm, TemporalType.TIMESTAMP);

        return (dd != null) ? String.valueOf(dd) : "0";
    }
    
    private String generateDailyBillItemSerialNumberByDoctor(Department department, Staff staff) {
        if (department == null) {
            return "";
        }
        if (staff == null) {
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
                + " and bi.primaryStaff=:sff ";
        HashMap hm = new HashMap();

        hm.put("dep", department);
        hm.put("sff", staff);
        hm.put("fd", startOfDay);
        hm.put("td", endOfDay);

        Long dd = billItemFacade.findAggregateLong(jpql, hm, TemporalType.TIMESTAMP);

        return (dd != null) ? String.valueOf(dd) : "0";
    }
    // </editor-fold>

}
