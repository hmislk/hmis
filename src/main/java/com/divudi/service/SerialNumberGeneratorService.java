package com.divudi.service;

import com.divudi.core.entity.Category;
import com.divudi.core.entity.Institution;
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
    
    private String getLockKey(Institution institution, Category categorye) {
        return institution.getId() + "-" + categorye.getId();
    }
    
    public String fetchLastSerialNumberForDay(Institution institution, Category category){
        if (institution == null || category == null) {
            return "";
        }
        
        String lockKey = getLockKey(institution, category);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateDailyBillItemSerialNumber(institution, category);
        } finally {
            lock.unlock();
        }
        
    }
    
    public String generateDailyBillItemSerialNumber(Institution institution, Category category) {
        if(institution == null){
            return "";
        }
        if(category == null){
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
                + " and bi.bill.institution=:ins "
                + " and bi.item.category=:cat ";
        HashMap hm = new HashMap();

        hm.put("ins", institution);
        hm.put("cat", category);
        hm.put("fd", startOfDay);
        hm.put("td", endOfDay);

        Long dd = billItemFacade.findAggregateLong(jpql, hm, TemporalType.TIMESTAMP);

        return (dd != null) ? String.valueOf(dd) : "0";
    }
    

}
