package com.divudi.ejb;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.AppointmentType;
import com.divudi.core.entity.BillNumber;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.AppointmentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillNumberFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientFacade;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.persistence.TemporalType;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */

@Singleton
public class NumberGenerator {

    @EJB
    private AppointmentFacade appointmentFacade;
    @EJB
    BillNumberFacade billNumberFacade;

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    
    private String getLockKey(Institution institution, AppointmentType appointmentType) {
        String institutionId = institution != null ? institution.getId().toString() : "null";
        String appType = appointmentType != null ? appointmentType.getDisplayName() : "null";
        return institutionId + "-" + appType;
    }
    
    private BillNumber fetchLastAppointmentNumberSynchronized(Institution institution, AppointmentType appointmentType) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.institution=:ins "
                + " AND b.appointmentType =:aType "
                + " AND b.billYear=:yr";

        HashMap<String, Object> hm = new HashMap<>();
        hm.put("ins", institution);
        hm.put("aType", appointmentType);
        hm.put("yr", currentYear);
        
        BillNumber billNumber = billNumberFacade.findFreshByJpql(sql, hm);

        if (billNumber == null) {
            billNumber = new BillNumber();
            
            billNumber.setInstitution(institution);
            billNumber.setAppointmentType(appointmentType);
            billNumber.setBillYear(currentYear);  // Set the current year

            sql = "SELECT count(a) FROM Appointment a "
                    + " where a.retired=false "
                    + " and a.institution=:ins "
                    + " and a.appointmentType=:aType "
                    + " AND a.createdAt BETWEEN :startOfYear AND :endOfYear";

            Calendar startOfYear = Calendar.getInstance();
            startOfYear.set(Calendar.DAY_OF_YEAR, 1);
            Calendar endOfYear = Calendar.getInstance();
            endOfYear.set(Calendar.MONTH, 11);  // December
            endOfYear.set(Calendar.DAY_OF_MONTH, 31);

            hm = new HashMap<>();
            hm.put("ins", institution);
            hm.put("aType", appointmentType);
            hm.put("startOfYear", startOfYear.getTime());
            hm.put("endOfYear", endOfYear.getTime());

            Long dd = appointmentFacade.findAggregateLong(sql, hm, TemporalType.DATE);
            if (dd == null) {
                dd = 0L;
            }
            billNumber.setLastBillNumber(dd);
            billNumberFacade.createAndFlush(billNumber);
        } else {
            Long newBillNumberLong = billNumber.getLastBillNumber();
            if (newBillNumberLong == null) {
                newBillNumberLong = 0L;
            }
            billNumber.setLastBillNumber(newBillNumberLong);
            billNumberFacade.editAndFlush(billNumber);
        }

        return billNumber;
    }
    
    public BillNumber fetchLastAppointmentNumberForYear(Institution institution, AppointmentType appointmentType) {
        String lockKey = getLockKey(institution, appointmentType);
        
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());
        lock.lock();
        
        try {
            return fetchLastAppointmentNumberSynchronized(institution, appointmentType);
        } finally {
            lock.unlock();
            // Optionally keep the lock in the map or use an appropriate strategy to remove it if necessary
        }
    }
    
    private String getNumberDelimiter() {
        String delimiter = configOptionApplicationController.getShortTextValueByKey("Number Delimiter", "/");
        if (delimiter == null) {
            return "/";
        }
        delimiter = delimiter.trim();
        if (delimiter.isEmpty()) {
            return "/";
        }
        return delimiter;
    }

    public String inwardAppointmentNumberGeneratorYearly(Institution institution, AppointmentType appointmentType ) {
        if (institution == null) {
            return "";
        }
        
        if (appointmentType == null) {
            return "";
        }
        
        BillNumber billNumber;
        
        billNumber = fetchLastAppointmentNumberForYear(institution, appointmentType);
        
        System.out.println("billNumber = " + billNumber);
        
        // Get the last bill number
        Long dd = billNumber.getLastBillNumber();
        // Increment the bill number
        dd = dd + 1;

        // Set the updated bill number in the BillNumber entity
        billNumber.setLastBillNumber(dd);

        // Update the BillNumber entity in the database
        billNumberFacade.editAndFlush(billNumber);

        // Generate the Request number string
        StringBuilder result = new StringBuilder();
        
        String appointmentTypeCode = configOptionApplicationController.getShortTextValueByKey(appointmentType.getDisplayName() + " + Number Generation Suffix", appointmentType.getCode());
        
        // Append AppointmentType code
        result.append(appointmentTypeCode);
        result.append(getNumberDelimiter());

        // Append institution code
        result.append(institution.getInstitutionCode());
        result.append(getNumberDelimiter());

        
        // Append current year (last two digits)
        int year = Calendar.getInstance().get(Calendar.YEAR) % 100; // Get last two digits of year
        result.append(String.format("%02d", year)); // Ensure year is always two digits
        result.append(getNumberDelimiter());
        
        // Append formatted 6-digit bill number
        result.append(String.format("%06d", dd)); // Ensure bill number is always six digits

        // Return the formatted bill number
        return result.toString();
    }

}
