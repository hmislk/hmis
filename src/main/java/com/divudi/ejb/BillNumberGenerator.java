package com.divudi.ejb;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.TokenType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillNumber;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.Staff;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillNumberFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientFacade;
import java.text.DecimalFormat;
import java.time.Month;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.persistence.TemporalType;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import java.util.regex.Pattern;

/**
 *
 * @author Dr. M H B Ariyaratne <buddhika.ari at gmail.com>
 *
 */
@Singleton
public class BillNumberGenerator {

    @EJB
    private DepartmentFacade depFacade;
    @EJB
    private InstitutionFacade insFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    PatientFacade patientFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    BillNumberFacade billNumberFacade;

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    private String getLockKey(Institution institution, Department toDepartment, BillType billType) {
        return institution.getId() + "-" + toDepartment.getId() + "-" + billType.name();
    }

    private String getLockKey(Institution institution, Department toDepartment, BillTypeAtomic billType, PaymentMethod paymentMethod) {
        return institution.getId() + "-" + toDepartment.getId() + "-" + billType.name() + "-" + paymentMethod.getLabel();
    }

    private String getLockKey(Institution institution, Department toDepartment, BillTypeAtomic billType) {
        String institutionId = institution != null ? institution.getId().toString() : "null";
        String departmentId = toDepartment != null ? toDepartment.getId().toString() : "null";
        String billTypeLabel = billType != null ? billType.getLabel() : "null";
        return institutionId + "-" + departmentId + "-" + billTypeLabel;
    }

    private String getLockKey(Institution institution, Department toDepartment, List<BillTypeAtomic> billTypes) {
        String institutionId = institution != null ? institution.getId().toString() : "null";
        String departmentId = toDepartment != null ? toDepartment.getId().toString() : "null";
        String billTypeKey = "null";
        if (billTypes != null && !billTypes.isEmpty()) {
            // Sort to ensure consistent ordering
            billTypeKey = billTypes.stream()
                    .sorted()
                    .map(bt -> bt != null ? bt.name() : "null")
                    .collect(Collectors.joining(","));
        }
        return institutionId + "-" + departmentId + "-" + billTypeKey;
    }

    private String getLockKey(Department toDepartment, List<BillTypeAtomic> billTypes) {
        String departmentId = toDepartment != null ? toDepartment.getId().toString() : "null";
        String billTypeKey = "null";
        if (billTypes != null && !billTypes.isEmpty()) {
            // Sort to ensure consistent ordering
            billTypeKey = billTypes.stream()
                    .sorted()
                    .map(bt -> bt != null ? bt.name() : "null")
                    .collect(Collectors.joining(","));
        }
        return departmentId + "-" + billTypeKey;
    }
    private String getLockKey(Institution institution, Department fromDepartment, Department toDepartment, BillTypeAtomic billType) {
        String institutionId = institution != null ? institution.getId().toString() : "null";
        String fromDepartmentId = fromDepartment != null ? fromDepartment.getId().toString() : "null";
        String departmentId = toDepartment != null ? toDepartment.getId().toString() : "null";
        String billTypeLabel = billType != null ? billType.getLabel() : "null";
        return institutionId + "-" + fromDepartmentId + "-" + departmentId + "-" + billTypeLabel;
    }

    private String getLockKey(Institution institution) {
        return institution.getId() + "-" + "null" + "-" + "null";
    }

    private String getBillNumberDelimiter() {
        String delimiter = configOptionApplicationController.getShortTextValueByKey("Bill Number Delimiter", "/");
        if (delimiter == null) {
            return "/";
        }
        delimiter = delimiter.trim();
        if (delimiter.isEmpty()) {
            return "/";
        }
        return delimiter;
    }

    private String[] splitByBillNumberDelimiter(String value) {
        if (value == null) {
            return new String[0];
        }
        String delimiter = getBillNumberDelimiter();
        String[] parts = value.split(Pattern.quote(delimiter));
        if (parts.length <= 1 && !"/".equals(delimiter)) {
            parts = value.split("/");
        }
        return parts;
    }

    public BillNumber fetchLastBillNumberForYear(Institution institution, Department toDepartment, BillType billType, BillClassType billClassType) {
        String lockKey = getLockKey(institution, toDepartment, billType);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return fetchLastBillNumberSynchronized(institution, toDepartment, billType, billClassType);
        } finally {
            lock.unlock();
            // Optionally keep the lock in the map or use an appropriate strategy to remove it if necessary
        }
    }

    public BillNumber fetchLastBillNumberForYear(Institution institution, Department fromDepartment, Department toDepartment, BillTypeAtomic billType) {
        String lockKey = getLockKey(institution, fromDepartment, toDepartment, billType);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return fetchLastBillNumberSynchronized(institution, fromDepartment, toDepartment, billType);
        } finally {
            lock.unlock();
            // Optionally keep the lock in the map or use an appropriate strategy to remove it if necessary
        }
    }

    public BillNumber fetchLastBillNumberForYear(Institution institution, Department toDepartment, BillType billType, BillClassType billClassType, PaymentMethod paymentMethod) {
        String lockKey = getLockKey(institution, toDepartment, billType);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {

//            BillNumber fetchLastBillNumberSynchronized(Institution institution, Department toDepartment, BillTypeAtomic billType, PaymentMethod paymentMethod) {
            return fetchLastBillNumberSynchronized(institution, toDepartment, billType, billClassType);
        } finally {
            lock.unlock();
            // Optionally keep the lock in the map or use an appropriate strategy to remove it if necessary
        }
    }

    public BillNumber fetchLastBillNumberForYear(Institution institution, Department toDepartment, BillTypeAtomic billType) {
        String lockKey = getLockKey(institution, toDepartment, billType);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return fetchLastBillNumberSynchronized(institution, toDepartment, billType);
        } finally {
            lock.unlock();
            // Optionally keep the lock in the map or use an appropriate strategy to remove it if necessary
        }
    }

    public BillNumber fetchLastBillNumberForYear(Institution institution, Department toDepartment, List<BillTypeAtomic> billTypes) {
        String lockKey = getLockKey(institution, toDepartment, billTypes);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return fetchLastBillNumberSynchronized(institution, toDepartment, billTypes);
        } finally {
            lock.unlock();
        }
    }

    public BillNumber fetchLastBillNumberForYear(Institution institution, Department toDepartment, BillTypeAtomic billType, PaymentMethod paymentMethod) {
        String lockKey = getLockKey(institution, toDepartment, billType, paymentMethod);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return fetchLastBillNumberSynchronized(institution, toDepartment, billType, paymentMethod);
        } finally {
            lock.unlock();
            // Optionally keep the lock in the map or use an appropriate strategy to remove it if necessary
        }
    }

    public BillNumber fetchLastBillNumberForYear(Institution institution) {
        String lockKey = getLockKey(institution);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());
        lock.lock();
        try {
            return fetchLastBillNumberSynchronized(institution);
        } finally {
            lock.unlock();
            // Optionally keep the lock in the map or use an appropriate strategy to remove it if necessary
        }
    }

    private BillNumber fetchLastBillNumberSynchronized(Institution institution) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billType is null  "
                + " and b.billClassType is null "
                + " and b.institution=:ins "
                + " AND b.toDepartment is null "
                + " AND b.billYear=:yr";

        HashMap<String, Object> hm = new HashMap<>();
        hm.put("ins", institution);
        hm.put("yr", currentYear);
        BillNumber billNumber = billNumberFacade.findFreshByJpql(sql, hm);

        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillType(null);
            billNumber.setBillClassType(null);
            billNumber.setInstitution(institution);
            billNumber.setToDepartment(null);
            billNumber.setBillYear(currentYear);  // Set the current year

            sql = "SELECT count(b) FROM Bill b "
                    + " where b.retired=false "
                    + " and b.institution=:ins "
                    + " AND b.billDate BETWEEN :startOfYear AND :endOfYear";

            Calendar startOfYear = Calendar.getInstance();
            startOfYear.set(Calendar.DAY_OF_YEAR, 1);
            Calendar endOfYear = Calendar.getInstance();
            endOfYear.set(Calendar.MONTH, 11);  // December
            endOfYear.set(Calendar.DAY_OF_MONTH, 31);

            hm = new HashMap<>();
            hm.put("ins", institution);
            hm.put("startOfYear", startOfYear.getTime());
            hm.put("endOfYear", endOfYear.getTime());

            Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
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

    private BillNumber fetchLastBillNumberSynchronized(Institution institution, Department toDepartment, BillType billType, BillClassType billClassType) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billType=:bTp "
                + " and b.billClassType=:bcl"
                + " and b.institution=:ins "
                + " AND b.toDepartment=:tDep"
                + " AND b.billYear=:yr";

        HashMap<String, Object> hm = new HashMap<>();
        hm.put("bTp", billType);
        hm.put("bcl", billClassType);
        hm.put("ins", institution);
        hm.put("tDep", toDepartment);
        hm.put("yr", currentYear);

        BillNumber billNumber = billNumberFacade.findFreshByJpql(sql, hm);

        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillType(billType);
            billNumber.setBillClassType(billClassType);
            billNumber.setInstitution(institution);
            billNumber.setToDepartment(toDepartment);
            billNumber.setBillYear(currentYear);  // Set the current year

            sql = "SELECT count(b) FROM Bill b "
                    + " where b.billType=:bTp "
                    + " and b.retired=false"
                    + " and type(b)=:class"
                    + " and b.institution=:ins "
                    + " and b.toDepartment=:tDep"
                    + " AND b.billDate BETWEEN :startOfYear AND :endOfYear";

            Calendar startOfYear = Calendar.getInstance();
            startOfYear.set(Calendar.DAY_OF_YEAR, 1);
            Calendar endOfYear = Calendar.getInstance();
            endOfYear.set(Calendar.MONTH, 11);  // December
            endOfYear.set(Calendar.DAY_OF_MONTH, 31);

            hm = new HashMap<>();
            hm.put("bTp", billType);
            hm.put("ins", institution);
            hm.put("tDep", toDepartment);
            hm.put("startOfYear", startOfYear.getTime());
            hm.put("endOfYear", endOfYear.getTime());

            switch (billClassType) {
                case BilledBill:
                    hm.put("class", BilledBill.class);
                    break;
                case CancelledBill:
                    hm.put("class", CancelledBill.class);
                    break;
                case RefundBill:
                    hm.put("class", RefundBill.class);
                    break;
                case PreBill:
                    hm.put("class", PreBill.class);
                    break;
            }

            Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
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

    private BillNumber fetchLastBillNumberSynchronized(Institution institution, Department toDepartment, BillTypeAtomic billTypeAtomic, PaymentMethod paymentMethod) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billTypeAtomic=:bTp "
                + " and b.institution=:ins "
                + " and b.paymentMethod=:paymentMethod "
                + " AND b.toDepartment=:tDep"
                + " AND b.billYear=:yr";

        HashMap<String, Object> hm = new HashMap<>();
        hm.put("bTp", billTypeAtomic);
        hm.put("ins", institution);
        hm.put("tDep", toDepartment);
        hm.put("yr", currentYear);
        hm.put("paymentMethod", paymentMethod);

        BillNumber billNumber = billNumberFacade.findFreshByJpql(sql, hm);

        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillTypeAtomic(billTypeAtomic);
            billNumber.setInstitution(institution);
            billNumber.setToDepartment(toDepartment);
            billNumber.setPaymentMethod(paymentMethod);
            billNumber.setBillYear(currentYear);  // Set the current year

            sql = "SELECT count(b) FROM Bill b "
                    + " where b.billType=:bTp "
                    + " and b.retired=false"
                    + " and b.institution=:ins "
                    + " and b.paymentMethod=:paymentMethod "
                    + " and b.toDepartment=:tDep"
                    + " AND b.billDate BETWEEN :startOfYear AND :endOfYear";

            Calendar startOfYear = Calendar.getInstance();
            startOfYear.set(Calendar.DAY_OF_YEAR, 1);
            Calendar endOfYear = Calendar.getInstance();
            endOfYear.set(Calendar.MONTH, 11);  // December
            endOfYear.set(Calendar.DAY_OF_MONTH, 31);

            hm = new HashMap<>();
            hm.put("bTp", billTypeAtomic);
            hm.put("ins", institution);
            hm.put("tDep", toDepartment);
            hm.put("startOfYear", startOfYear.getTime());
            hm.put("endOfYear", endOfYear.getTime());
            hm.put("paymentMethod", paymentMethod);

            Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
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

    private BillNumber fetchLastBillNumberSynchronized(Institution institution, Department toDepartment, BillTypeAtomic billType) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billTypeAtomic=:bTp "
                + " and b.institution=:ins "
                + " AND b.toDepartment=:tDep"
                + " AND b.billYear=:yr";

        HashMap<String, Object> hm = new HashMap<>();
        hm.put("bTp", billType);
        hm.put("ins", institution);
        hm.put("tDep", toDepartment);
        hm.put("yr", currentYear);

        BillNumber billNumber = billNumberFacade.findFreshByJpql(sql, hm);

        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillTypeAtomic(billType);
            billNumber.setInstitution(institution);
            billNumber.setToDepartment(toDepartment);
            billNumber.setBillYear(currentYear);  // Set the current year

            sql = "SELECT count(b) FROM Bill b "
                    + " where b.billTypeAtomic=:bTp "
                    + " and b.retired=false"
                    + " and b.institution=:ins "
                    + " and b.toDepartment=:tDep"
                    + " AND b.billDate BETWEEN :startOfYear AND :endOfYear";

            Calendar startOfYear = Calendar.getInstance();
            startOfYear.set(Calendar.DAY_OF_YEAR, 1);
            Calendar endOfYear = Calendar.getInstance();
            endOfYear.set(Calendar.MONTH, 11);  // December
            endOfYear.set(Calendar.DAY_OF_MONTH, 31);

            hm = new HashMap<>();
            hm.put("bTp", billType);
            hm.put("ins", institution);
            hm.put("tDep", toDepartment);
            hm.put("startOfYear", startOfYear.getTime());
            hm.put("endOfYear", endOfYear.getTime());

            Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
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

    private BillNumber fetchLastBillNumberSynchronized(Institution institution, Department toDepartment, List<BillTypeAtomic> billTypes) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billTypeAtomic is null "
                + " and b.institution=:ins "
                + " AND b.toDepartment=:tDep"
                + " AND b.billYear=:yr";

        HashMap<String, Object> hm = new HashMap<>();
        hm.put("ins", institution);
        hm.put("tDep", toDepartment);
        hm.put("yr", currentYear);

        BillNumber billNumber = billNumberFacade.findFirstByJpql(sql, hm);

        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillTypeAtomic(null);
            billNumber.setInstitution(institution);
            billNumber.setToDepartment(toDepartment);
            billNumber.setBillYear(currentYear);

            sql = "SELECT b FROM Bill b "
                    + " where b.billTypeAtomic in :bTp "
                    + " and b.retired=false"
                    + " and b.institution=:ins "
                    + " and b.toDepartment=:tDep"
                    + " AND b.billDate BETWEEN :startOfYear AND :endOfYear"
                    + " ORDER BY b.id DESC";

            Calendar startOfYear = Calendar.getInstance();
            startOfYear.set(Calendar.DAY_OF_YEAR, 1);
            startOfYear.set(Calendar.HOUR_OF_DAY, 0);
            startOfYear.set(Calendar.MINUTE, 0);
            startOfYear.set(Calendar.SECOND, 0);
            startOfYear.set(Calendar.MILLISECOND, 0);

            Calendar endOfYear = Calendar.getInstance();
            endOfYear.set(Calendar.MONTH, 11);
            endOfYear.set(Calendar.DAY_OF_MONTH, 31);
            endOfYear.set(Calendar.HOUR_OF_DAY, 23);
            endOfYear.set(Calendar.MINUTE, 59);
            endOfYear.set(Calendar.SECOND, 59);
            endOfYear.set(Calendar.MILLISECOND, 999);

            hm = new HashMap<>();
            hm.put("bTp", billTypes);
            hm.put("ins", institution);
            hm.put("tDep", toDepartment);
            hm.put("startOfYear", startOfYear.getTime());
            hm.put("endOfYear", endOfYear.getTime());

            List<Bill> lastBills = getBillFacade().findByJpql(sql, hm, 1);

            Long dd = 0L;
            if (lastBills != null && !lastBills.isEmpty()) {
                Bill lastBill = lastBills.get(0);
                String lastDeptId = lastBill.getDeptId();
                if (lastDeptId != null && !lastDeptId.trim().isEmpty()) {
                    try {
                        String[] parts = splitByBillNumberDelimiter(lastDeptId);
                        if (parts.length >= 4) {
                            dd = Long.parseLong(parts[parts.length - 1]);
                        } else {
                        }
                    } catch (NumberFormatException e) {
                        dd = 0L;
                    }
                } else {
                }
            } else {
            }
            billNumber.setLastBillNumber(dd);
            billNumberFacade.createAndFlush(billNumber);
        } else {
            Long newBillNumberLong = billNumber.getLastBillNumber();
            if (newBillNumberLong == null) {
                newBillNumberLong = 0L;
            } else {
            }
            billNumber.setLastBillNumber(newBillNumberLong);
            billNumberFacade.editAndFlush(billNumber);
        }

        return billNumber;
    }

    // Special method for Single Number strategy only
    public BillNumber fetchLastBillNumberForYearSingleNumber(Institution institution, Department toDepartment, List<BillTypeAtomic> billTypes) {
        String lockKey = getLockKey(institution, toDepartment, billTypes);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return fetchLastBillNumberSynchronizedSingleNumber(toDepartment, billTypes);
        } finally {
            lock.unlock();
        }
    }

   
    public BillNumber fetchLastBillNumberForYearOpdAndInpatientServiceBatchBills(Department department, List<BillTypeAtomic> billTypes) {
        String lockKey = getLockKey(department, billTypes);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());
        lock.lock();
        try {
            return fetchLastBillNumberSynchronizedForOpdAndInpatientBatchBills(department, billTypes);
        } finally {
            lock.unlock();
        }
    }

    // Special synchronized method for Single Number strategy only
    private BillNumber fetchLastBillNumberSynchronizedSingleNumber(Department department, List<BillTypeAtomic> billTypes) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        HashMap<String, Object> hm = new HashMap<>();
        String jpql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.opdAndInpatientServiceBills=true "
                + " AND b.billYear=:yr";
        if (department != null) {
            jpql += " AND b.department=:tDep";
            hm.put("tDep", department);
        }
        jpql += " order by b.id desc";
        hm.put("yr", currentYear);
        BillNumber billNumber = billNumberFacade.findFirstByJpql(jpql, hm);

        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillTypeAtomic(null);
            billNumber.setDepartment(department);
            // METHOD EXISTS: setOpdAndInpatientServiceBills(boolean) defined in BillNumber.java:215
            billNumber.setOpdAndInpatientServiceBills(true);
            billNumber.setBillYear(currentYear);
            jpql = "SELECT count(b) FROM Bill b "
                    + " where b.billTypeAtomic in :bTp "
                    + " AND b.billDate > :startOfYear ";
            if (department != null) {
                jpql += " AND b.department=:tDep";
                hm.put("tDep", department);
            }
            jpql += " ORDER BY b.id DESC";
            Calendar startOfYear = Calendar.getInstance();
            startOfYear.set(Calendar.DAY_OF_YEAR, 1);
            startOfYear.set(Calendar.HOUR_OF_DAY, 0);
            startOfYear.set(Calendar.MINUTE, 0);
            startOfYear.set(Calendar.SECOND, 0);
            startOfYear.set(Calendar.MILLISECOND, 0);

            hm = new HashMap<>();
            hm.put("bTp", billTypes);
            hm.put("tDep", department);
            hm.put("startOfYear", startOfYear.getTime());

            Long countOfBills = billFacade.findLongByJpql(jpql, hm);

            billNumber.setLastBillNumber(countOfBills + 1);

            billNumberFacade.createAndFlush(billNumber);
        }
        return billNumber;
    }

    private BillNumber fetchLastBillNumberSynchronizedForOpdAndInpatientBatchBills(Department department, List<BillTypeAtomic> billTypes) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        HashMap<String, Object> hm = new HashMap<>();
        String jpql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.opdAndInpatientServiceBatchBills=true "
                + " AND b.billYear=:yr";
        if (department != null) {
            jpql += " AND b.department=:tDep";
            hm.put("tDep", department);
        }
        jpql += " order by b.id desc";
        hm.put("yr", currentYear);
        BillNumber billNumber = billNumberFacade.findFirstByJpql(jpql, hm);
        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillTypeAtomic(null);
            billNumber.setDepartment(department);
            // METHOD EXISTS: setOpdAndInpatientServiceBatchBills(boolean) defined in BillNumber.java:231
            billNumber.setOpdAndInpatientServiceBatchBills(true);
            billNumber.setBillYear(currentYear);
            jpql = "SELECT count(b) FROM Bill b "
                    + " where b.billTypeAtomic in :bTp "
                    + " AND b.billDate > :startOfYear ";
            if (department != null) {
                jpql += " AND b.department=:tDep";
                hm.put("tDep", department);
            }
            jpql += " ORDER BY b.id DESC";
            Calendar startOfYear = Calendar.getInstance();
            startOfYear.set(Calendar.DAY_OF_YEAR, 1);
            startOfYear.set(Calendar.HOUR_OF_DAY, 0);
            startOfYear.set(Calendar.MINUTE, 0);
            startOfYear.set(Calendar.SECOND, 0);
            startOfYear.set(Calendar.MILLISECOND, 0);
            hm = new HashMap<>();
            hm.put("bTp", billTypes);
            hm.put("tDep", department);
            hm.put("startOfYear", startOfYear.getTime());
            Long countOfBills = billFacade.findLongByJpql(jpql, hm);
            billNumber.setLastBillNumber(countOfBills + 1);
            billNumberFacade.createAndFlush(billNumber);
        }
        return billNumber;
    }
    
    private BillNumber fetchLastBillNumberSynchronizedForOpdAndInpatientIndividualBills(Department department, List<BillTypeAtomic> billTypes) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        HashMap<String, Object> hm = new HashMap<>();
        String jpql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.opdAndInpatientServiceBills=true "
                + " AND b.billYear=:yr";
        if (department != null) {
            jpql += " AND b.department=:tDep";
            hm.put("tDep", department);
        }
        jpql += " order by b.id desc";
        hm.put("yr", currentYear);
        BillNumber billNumber = billNumberFacade.findFirstByJpql(jpql, hm);
        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillTypeAtomic(null);
            billNumber.setDepartment(department);
            // METHOD EXISTS: setOpdAndInpatientServiceBills(boolean) defined in BillNumber.java:215
            billNumber.setOpdAndInpatientServiceBills(true);
            billNumber.setBillYear(currentYear);
            jpql = "SELECT count(b) FROM Bill b "
                    + " where b.billTypeAtomic in :bTp "
                    + " AND b.billDate > :startOfYear ";
            if (department != null) {
                jpql += " AND b.department=:tDep";
                hm.put("tDep", department);
            }
            jpql += " ORDER BY b.id DESC";
            Calendar startOfYear = Calendar.getInstance();
            startOfYear.set(Calendar.DAY_OF_YEAR, 1);
            startOfYear.set(Calendar.HOUR_OF_DAY, 0);
            startOfYear.set(Calendar.MINUTE, 0);
            startOfYear.set(Calendar.SECOND, 0);
            startOfYear.set(Calendar.MILLISECOND, 0);
            hm = new HashMap<>();
            hm.put("bTp", billTypes);
            hm.put("tDep", department);
            hm.put("startOfYear", startOfYear.getTime());
            Long countOfBills = billFacade.findLongByJpql(jpql, hm);
            billNumber.setLastBillNumber(countOfBills + 1);
            billNumberFacade.createAndFlush(billNumber);
        }
        return billNumber;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public synchronized String institutionBillNumberGenerator(Institution ins, BillType billType, BillClassType billClassType, BillNumberSuffix billNumberSuffix) {
        BillNumber billNumber = fetchLastBillNumber(ins, billType, billClassType);
        StringBuilder result = new StringBuilder();
        Long b = billNumber.getLastBillNumber();
        result.append(ins.getInstitutionCode());
        result.append(billNumberSuffix.getSuffix());
        result.append(getBillNumberDelimiter());
        result.append(++b);
        billNumber.setLastBillNumber(b);
        billNumberFacade.edit(billNumber);
        return result.toString();
    }

    public synchronized String fetchPaymentSchemeCount(PaymentScheme paymentScheme, BillType billType, Institution institution) {
        if (paymentScheme == null) {
            return "";
        }
        String sql = "SELECT count(b) FROM PreBill b "
                + "  where  b.retired=false "
                + " and b.institution=:ins "
                + " and b.billType = :bt ";
        HashMap hm = new HashMap();
        hm.put("ins", institution);
        hm.put("bt", billType);
//        hm.put("f", CommonFunctions.getFirstDayOfYear(new Date()));
//        hm.put("t", CommonFunctions.getLastDayOfYear(new Date()));
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        return (i + 1) + "";
    }

    //institution is not use to take Bill Count to privent errors in Overloding
    private BillNumber fetchLastBillNumberSynchronized(Institution institution, Department fromDepartment, Department toDepartment, BillTypeAtomic billType) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billTypeAtomic=:bTp "
                + " and b.department=:fDept "
                + " AND b.toDepartment=:tDep"
                + " AND b.billYear=:yr";

        HashMap<String, Object> hm = new HashMap<>();
        hm.put("bTp", billType);
        hm.put("fDept", fromDepartment);
        hm.put("tDep", toDepartment);
        hm.put("yr", currentYear);

        BillNumber billNumber = billNumberFacade.findFreshByJpql(sql, hm);

        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillTypeAtomic(billType);
            billNumber.setDepartment(fromDepartment);
            billNumber.setToDepartment(toDepartment);
            billNumber.setBillYear(currentYear);  // Set the current year

            sql = "SELECT count(b) FROM Bill b "
                    + " where b.billTypeAtomic=:bTp "
                    + " and b.retired=false"
                    + " and b.department=:fDept "
                    + " and b.toDepartment=:tDep"
                    + " AND b.billDate BETWEEN :startOfYear AND :endOfYear";

            Calendar startOfYear = Calendar.getInstance();
            startOfYear.set(Calendar.DAY_OF_YEAR, 1);
            Calendar endOfYear = Calendar.getInstance();
            endOfYear.set(Calendar.MONTH, 11);  // December
            endOfYear.set(Calendar.DAY_OF_MONTH, 31);

            hm = new HashMap<>();
            hm.put("bTp", billType);
            hm.put("fDept", fromDepartment);
            hm.put("tDep", toDepartment);
            hm.put("startOfYear", startOfYear.getTime());
            hm.put("endOfYear", endOfYear.getTime());

            Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
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

    public synchronized String institutionChannelBillNumberGenerator(Institution ins, Bill bill) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};

        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "SELECT count(b) FROM Bill b"
                + "  where type(b)=:type "
                + " and b.retired=false"
                + " AND b.institution=:ins "
                + " and b.billType in :bt "
                + " and b.createdAt is not null";
        String result = "";
        HashMap hm = new HashMap();
        hm.put("ins", ins);
        hm.put("bt", bts);
        hm.put("type", bill.getClass());
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        String suffix = "";

        if (bill instanceof BilledBill) {
            suffix = "CHANN";
        }

        if (bill instanceof CancelledBill) {
            suffix = "CHANNCAN";
        }

        if (bill instanceof RefundBill) {
            suffix = "CHANNREF";
        }

        String delimiter = getBillNumberDelimiter();
        if (i != null) {
            result = ins.getInstitutionCode() + suffix + delimiter + (i + 1);
        } else {
            result = ins.getInstitutionCode() + suffix + delimiter + 1;
        }

        return result;
    }

    public synchronized String institutionBillNumberGeneratorWithReference(Institution ins, Bill bill, BillType billType, BillNumberSuffix billNumberSuffix) {

        String sql = "SELECT count(b) "
                + " FROM Bill b"
                + " where type(b)=:type "
                + "and b.retired=false"
                + " AND  b.institution=:ins"
                + " AND b.billType=:btp"
                + " and b.createdAt is not null"
                + " and b.referenceBill is not null";
        StringBuilder result = new StringBuilder();
        HashMap hm = new HashMap();
        hm.put("ins", ins);
        hm.put("btp", billType);
        hm.put("type", bill.getClass());
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        result.append(ins.getInstitutionCode());
        result.append(billNumberSuffix.getSuffix());
        result.append(getBillNumberDelimiter());
        if (i != null) {
            result.append(i + 1);
        } else {
            result.append(1);
        }

        return result.toString();
    }

    public synchronized String institutionBillNumberGeneratorByPayment(Institution ins, Bill bill, BillType billType, BillNumberSuffix billNumberSuffix) {

        String sql = "SELECT count(b) FROM Bill b "
                + " where type(b)=:type "
                + " and b.retired=false "
                + " AND b.institution=:ins"
                + " AND b.billType=:btp "
                + " and b.createdAt is not null";
//                + " and (b.netTotal >0 or b.total >0)  ";
        StringBuilder result = new StringBuilder();
        HashMap hm = new HashMap();
        hm.put("ins", ins);
        hm.put("btp", billType);
        hm.put("type", bill.getClass());
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        result.append(ins.getInstitutionCode());
        result.append(billNumberSuffix.getSuffix());
        result.append(getBillNumberDelimiter());

        if (i != null) {
            result.append(i + 1);
        } else {
            result.append(1);
        }
        return result.toString();
    }

    public synchronized String institutionBillNumberGenerator(Department dep, BillType billType, BillClassType billClassType, BillNumberSuffix billNumberSuffix) {

        BillNumber billNumber = fetchLastBillNumber(dep, billType, billClassType);
        StringBuilder result = new StringBuilder();
        Long b = billNumber.getLastBillNumber();
        result.append(dep.getDepartmentCode());

        if (billNumberSuffix != BillNumberSuffix.NONE) {
            result.append(billNumberSuffix);
        }

        result.append(getBillNumberDelimiter());
        result.append(++b);

        billNumber.setLastBillNumber(b);
        billNumberFacade.edit(billNumber);

        return result.toString();
    }

    public boolean checkBillNumberDeptId(Department dep, Bill bill, BillType billType, String number) {

        String sql = "SELECT b "
                + " FROM Bill b "
                + " where type(b)=:type "
                + " AND b.retired=false "
                + " AND b.department=:dep"
                + " and (b.deptId)=:str "
                + " AND b.billType=:btp ";

        HashMap hm = new HashMap();
        hm.put("dep", dep);
        hm.put("btp", billType);
        hm.put("type", bill.getClass());
        hm.put("str", number.toUpperCase());
        Bill result = getBillFacade().findFirstByJpql(sql, hm, TemporalType.DATE);

        return result != null;
    }

    public String institutionBillNumberGeneratorWithReference(Department dep, Bill bill, BillType billType, BillNumberSuffix billNumberSuffix) {

        String sql = "SELECT count(b) "
                + " FROM Bill b "
                + " where type(b)=:type "
                + " AND b.retired=false "
                + " AND b.department=:dep "
                + " AND b.createdAt is not null "
                + " AND b.billType=:btp "
                + " AND b.referenceBill is not null ";
        String result = "";
        HashMap hm = new HashMap();
        hm.put("dep", dep);
        hm.put("btp", billType);
        hm.put("type", bill.getClass());
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        result = createNumber(i, billNumberSuffix, dep);

        if (checkBillNumberDeptId(dep, bill, billType, result)) {
            result = createNumber(i + 1, billNumberSuffix, dep);
        }

        return result;
    }

    private String createNumber(Long i, BillNumberSuffix billNumberSuffix, Department dep) {
        String result = "";
        String delimiter = getBillNumberDelimiter();
        if (i != null) {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = dep.getDepartmentCode() + billNumberSuffix + delimiter + (i + 1);
            } else {
                result = dep.getDepartmentCode() + delimiter + (i + 1);
            }

        } else {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = dep.getDepartmentCode() + billNumberSuffix + delimiter + 1;
            } else {
                result = dep.getDepartmentCode() + delimiter + 1;
            }

        }

        return result;

    }

    public synchronized String institutionBillNumberGeneratorByPayment(Department dep, Bill bill, BillType billType, BillNumberSuffix billNumberSuffix) {

        String sql = "SELECT count(b) FROM Bill b "
                + " where type(b)=:type"
                + " and b.retired=false "
                + " AND b.department=:dep "
                + " and b.createdAt is not null "
                + " AND b.billType=:btp "
                + " and b.billDate is not null";
//                + " and (b.netTotal >0 or b.total >0) ";
        String result = "";
        HashMap hm = new HashMap();
        hm.put("dep", dep);
        hm.put("btp", billType);
        hm.put("type", bill.getClass());
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
        String delimiter = getBillNumberDelimiter();

        if (i != null) {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = dep.getDepartmentCode() + billNumberSuffix + delimiter + (i + 1);
            } else {
                result = dep.getDepartmentCode() + delimiter + (i + 1);
            }

        } else {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = dep.getDepartmentCode() + billNumberSuffix + delimiter + 1;
            } else {
                result = dep.getDepartmentCode() + delimiter + 1;
            }

        }

        return result;
    }

    static String intToString(int num, int digits) {
        assert digits > 0 : "Invalid number of digits";
        // create variable length array of zeros
        char[] zeros = new char[digits];
        Arrays.fill(zeros, '0');
        // format number as String
        DecimalFormat df = new DecimalFormat(String.valueOf(zeros));
        return df.format(num);
    }

    public String patientCodeGenerator() {
        long pts = getPatientFacade().count();
        return intToString((int) pts, 6);
    }

    public String bookingIdGenerator(Institution institution, Bill bill) {
        BillType[] billTypes = BillType.ChannelCashFlow.allChildren();
        List<BillType> bts = Arrays.asList(billTypes);
        String sql = "SELECT count(b) FROM BillSession b "
                + " where b.bill.retired=false "
                + "  AND type(b.bill)=:class "
                + "  AND b.bill.billType in :bt ";
        String result;
        HashMap h = new HashMap();
        h.put("bt", bts);
        h.put("class", bill.getClass());

        String suff = "";

        if (bill instanceof CancelledBill) {
            suff = "CAN";
        }

        if (bill instanceof RefundBill) {
            suff = "REF";
        }

        Long b = getBillFacade().findAggregateLong(sql, h, TemporalType.DATE);
        String delimiter = getBillNumberDelimiter();

        if (b != 0) {
            result = institution.getInstitutionCode() + "CHANN" + suff + delimiter + (b + 1) + "";
            return result;
        } else {
            result = institution.getInstitutionCode() + "CHANN" + suff + delimiter + 1 + "";
            return result;
        }

    }

    public synchronized String institutionBillNumberGenerator(Institution institution, Department toDepartment, BillType billType, BillClassType billClassType, BillNumberSuffix billNumberSuffix) {
        BillNumber billNumber = fetchLastBillNumber(institution, toDepartment, billType, billClassType);
        StringBuilder result = new StringBuilder();
        Long b = billNumber.getLastBillNumber();
        if (institution == null) {
            return "";
        }
        if (toDepartment == null) {
            return "";
        }
        if (toDepartment.getInstitution() == null) {
            return "";
        }
        String insCode = "";
        if (toDepartment.getInstitution().equals(institution)) {
            if (institution.getInstitutionCode() == null) {
                insCode = institution.getInstitutionCode();
            } else if (institution.getCode() != null) {
                insCode = institution.getCode();
            }
        }
        result.append(insCode);
        String deptCode = "";
        if (toDepartment.getDepartmentCode() != null) {
            deptCode = toDepartment.getDepartmentCode();
        } else if (toDepartment.getCode() != null) {
            deptCode = toDepartment.getCode();
        }
        result.append(deptCode);

        if (BillNumberSuffix.NONE != billNumberSuffix) {
            result.append(billNumberSuffix);
        }
        result.append(getBillNumberDelimiter());
        b++;
        result.append(b);
        billNumber.setLastBillNumber(b);
        billNumberFacade.editAndFlush(billNumber);
        return result.toString();
    }

    public synchronized String institutionBillNumberGenerator(Institution institution, List<BillType> billTypes, BillClassType billClassType, String suffix) {
        BillNumber billNumber = fetchLastBillNumber(institution, billTypes, billClassType);
        StringBuilder result = new StringBuilder();
        Long b = billNumber.getLastBillNumber();

        result.append(suffix);

        result.append(getBillNumberDelimiter());

        result.append(++b);

        billNumber.setLastBillNumber(b);

        billNumberFacade.edit(billNumber);

        return result.toString();

    }

    public synchronized String institutionBillNumberGenerator(Institution institution, BillType billType, BillClassType billClassType, String suffix) {
        BillNumber billNumber = fetchLastBillNumber(institution, billType, billClassType);
        StringBuilder result = new StringBuilder();
        Long b = billNumber.getLastBillNumber();

        result.append(suffix);

        result.append(getBillNumberDelimiter());

        result.append(++b);

        billNumber.setLastBillNumber(b);

        billNumberFacade.edit(billNumber);

        return result.toString();

    }

    public synchronized String departmentBillNumberGenerator(Institution institution, Department department, List<BillType> billTypes, BillClassType billClassType, String suffix) {
        BillNumber billNumber = fetchLastBillNumber(institution, department, billTypes, billClassType);
        StringBuilder result = new StringBuilder();
        Long b = billNumber.getLastBillNumber();

        result.append(suffix);

        result.append(getBillNumberDelimiter());

        result.append(++b);

        billNumber.setLastBillNumber(b);

        billNumberFacade.edit(billNumber);

        return result.toString();

    }

    public synchronized String departmentBillNumberGenerator(Institution institution, Department department, BillType billType, BillClassType billClassType, String suffix) {
        BillNumber billNumber = fetchLastBillNumber(institution, billType, billClassType, department);
        StringBuilder result = new StringBuilder();
        Long b = billNumber.getLastBillNumber();

        result.append(suffix);

        result.append(getBillNumberDelimiter());

        result.append(++b);

        billNumber.setLastBillNumber(b);

        billNumberFacade.edit(billNumber);

        return result.toString();

    }

    private synchronized BillNumber fetchLastBillNumber(Department department, Department toDepartment, BillType billType, BillClassType billClassType) {
        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billType=:bTp "
                + " and b.billClassType=:bcl"
                + " and b.department=:dep "
                + " and b.toDepartment=:tDep";
        HashMap hm = new HashMap();
        hm.put("bTp", billType);
        hm.put("bcl", billClassType);
        hm.put("dep", department);
        hm.put("tDep", toDepartment);
        BillNumber billNumber = billNumberFacade.findFreshByJpql(sql, hm);

        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillType(billType);
            billNumber.setBillClassType(billClassType);
            billNumber.setDepartment(department);
            billNumber.setToDepartment(toDepartment);

            sql = "SELECT count(b) FROM Bill b "
                    + " where b.billType=:bTp "
                    + " and b.retired=false"
                    + " and type(b)=:class"
                    + " and b.department=:dep "
                    + " and b.toDepartment=:tDep";
            hm = new HashMap();
            hm.put("bTp", billType);
            hm.put("dep", department);
            hm.put("tDep", toDepartment);

            switch (billClassType) {
                case BilledBill:
                    hm.put("class", BilledBill.class);
                    break;
                case CancelledBill:
                    hm.put("class", CancelledBill.class);
                    break;
                case RefundBill:
                    hm.put("class", RefundBill.class);
                    break;
                case PreBill:
                    hm.put("class", PreBill.class);
                    break;
            }
            Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
            if (dd == null) {
                dd = 0L;
            }
            billNumber.setLastBillNumber(dd);
            billNumberFacade.createAndFlush(billNumber);
        }
        if (billNumber.getLastBillNumber() == null) {
            billNumber.setLastBillNumber(0L);
        }
        billNumber.setLastBillNumber(billNumber.getLastBillNumber() + 1);
        billNumberFacade.editAndFlush(billNumber);
        return billNumber;

    }

    public String generateDailyBillNumberForOpdBatchBillPre(Department department) {
        return generateDailyBillNumberForOpdBatchBillPre(department, null, null);
    }

    public String generateDailyBillNumberForOpdBatchBillPre(Department department, Category cat, Staff fromStaff) {
        String sql = "SELECT count(b) FROM Bill b "
                + " where b.billType=:bTp1 "
                + " and b.billDate=:bd "
                + " and (type(b)=:class1 or type(b)=:class2) ";
        HashMap hm = new HashMap();

        if (department != null) {
            sql += " and b.department=:dep ";
            hm.put("dep", department);
        }

        if (cat != null) {
            sql += " and b.category=:cat ";
            hm.put("cat", cat);
        }

        if (fromStaff != null) {
            sql += " and b.fromStaff=:staff ";
            hm.put("staff", fromStaff);
        }

        hm.put("bTp1", BillType.OpdBathcBillPre);
        hm.put("bd", new Date());
        hm.put("class1", BilledBill.class);
        hm.put("class2", PreBill.class);

        Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
        return (dd != null) ? String.valueOf(dd) : "0";
    }

    public String generateDailyBillNumberForOpd(Department department, Category cat, Staff fromStaff) {
        String sql = "SELECT count(b) FROM Bill b "
                + " where (b.billType=:bTp1 or b.billType=:bTp2) "
                + " and b.billDate=:bd "
                + " and (type(b)=:class1 or type(b)=:class2) ";
        HashMap hm = new HashMap();

        if (department != null) {
            sql += " and b.department=:dep ";
            hm.put("dep", department);
        }

        if (cat != null) {
            sql += " and b.category=:cat ";
            hm.put("cat", cat);
        }

        if (fromStaff != null) {
            sql += " and b.fromStaff=:staff ";
            hm.put("staff", fromStaff);
        }

        hm.put("bTp1", BillType.OpdBill);
        hm.put("bTp2", BillType.OpdPreBill);
        hm.put("bd", new Date());
        hm.put("class1", BilledBill.class);
        hm.put("class2", PreBill.class);

        Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
        return (dd != null) ? String.valueOf(dd) : "0";
    }

    public String generateDailyTokenNumberCounterWise(Department department, Department counter, Category cat, Staff staff, TokenType tokenType) {
        String sql = "SELECT count(b) "
                + " FROM Token b "
                + " where b.tokenType=:tt "
                + " and b.tokenDate=:bd ";
        HashMap hm = new HashMap();

        if (department != null) {
            sql += " and b.department=:dep ";
            hm.put("dep", department);
        }

        if (counter != null) {
            sql += " and b.counter=:cun ";
            hm.put("cun", counter);
        }

        if (cat != null) {
            sql += " and b.category=:cat ";
            hm.put("cat", cat);
        }

        if (staff != null) {
            sql += " and b.staff=:staff ";
            hm.put("staff", staff);
        }

        hm.put("tt", tokenType);
        hm.put("bd", new Date());
        Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
        if (dd == null) {
            dd = 0L;
        } else {
            dd++;
        }
        return String.valueOf(dd);
    }

    public String generateDailyTokenNumber(Department department, Category cat, Staff staff, TokenType tokenType) {
        String sql = "SELECT count(b) "
                + " FROM Token b "
                + " where b.tokenType=:tt "
                + " and b.tokenDate=:bd "
                + " and b.retired = false";
        HashMap hm = new HashMap();

        if (department != null) {
            sql += " and b.department=:dep ";
            hm.put("dep", department);
        }

        if (cat != null) {
            sql += " and b.category=:cat ";
            hm.put("cat", cat);
        }

        if (staff != null) {
            sql += " and b.staff=:staff ";
            hm.put("staff", staff);
        }

        hm.put("tt", tokenType);
        hm.put("bd", new Date());
        Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
        if (dd == null) {
            dd = 0L;
        } else {
            dd++;
        }
        return String.valueOf(dd);
    }

// Overloaded methods
    public String generateDailyBillNumberForOpd(Department department) {
        return generateDailyBillNumberForOpd(department, null, null);
    }

    public String generateDailyBillNumberForOpd(Department department, Category cat) {
        return generateDailyBillNumberForOpd(department, cat, null);
    }

    public String generateDailyBillNumberForOpd(Department department, Staff fromStaff) {
        return generateDailyBillNumberForOpd(department, null, fromStaff);
    }

    public String generateDailyBillNumberForOpd(Category cat, Staff fromStaff) {
        return generateDailyBillNumberForOpd(null, cat, fromStaff);
    }

    public String generateDailyBillNumberForOpd(Category cat) {
        return generateDailyBillNumberForOpd(null, cat, null);
    }

    public String generateDailyBillNumberForOpd(Staff fromStaff) {
        return generateDailyBillNumberForOpd(null, null, fromStaff);
    }

    private BillNumber fetchLastBillNumber(Department department, BillType billType, BillClassType billClassType) {
        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and  b.billType=:bTp "
                + " and b.billClassType=:bcl "
                + " and b.department=:dep ";
        HashMap hm = new HashMap();
        hm.put("bTp", billType);
        hm.put("bcl", billClassType);
        hm.put("dep", department);
        BillNumber billNumber = billNumberFacade.findFirstByJpql(sql, hm);
        if (billNumber == null) {
            sql = "SELECT b FROM "
                    + " Bill b "
                    + " where b.retired=false "
                    + " and  b.billType=:bTp "
                    + " and b.billClassType=:bcl "
                    + " and b.department=:dep "
                    + " order by b.id desc ";
            hm = new HashMap();
            hm.put("bTp", BillType.StoreOrderApprove);
            hm.put("bcl", billClassType);
            hm.put("dep", department);
            Bill bill = billFacade.findFirstByJpql(sql, hm);
            if (bill != null) {
                String[] parts = splitByBillNumberDelimiter(bill.getDeptId());
                if (parts.length > 1) {
                    billNumber = new BillNumber();
                    billNumber.setBillType(billType);
                    billNumber.setBillClassType(billClassType);
                    billNumber.setDepartment(department);
                    billNumber.setLastBillNumber(Long.valueOf(parts[1]));
                    billNumberFacade.create(billNumber);
                    return billNumber;
                }
            }
        }
        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillType(billType);
            billNumber.setBillClassType(billClassType);
            billNumber.setDepartment(department);

            sql = "SELECT count(b) FROM Bill b "
                    + " where b.billType=:bTp "
                    + " and b.retired=false"
                    + " and b.deptId is not null "
                    + " and type(b)=:class"
                    + " and b.department=:dep ";
            hm = new HashMap();
            hm.put("bTp", billType);
            hm.put("dep", department);

            switch (billClassType) {
                case BilledBill:
                    hm.put("class", BilledBill.class);
                    break;
                case CancelledBill:
                    hm.put("class", CancelledBill.class);
                    break;
                case RefundBill:
                    hm.put("class", RefundBill.class);
                    break;
                case PreBill:
                    hm.put("class", PreBill.class);
                    break;
            }

            Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
            if (dd == null) {
                dd = 0L;
            }

            billNumber.setLastBillNumber(dd);

            billNumberFacade.create(billNumber);
        }

        return billNumber;

    }

    private synchronized BillNumber fetchLastBillNumber(Institution institution, Department toDepartment, BillType billType, BillClassType billClassType) {
        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billType=:bTp "
                + " and b.billClassType=:bcl"
                + " and b.institution=:ins "
                + " AND b.toDepartment=:tDep";
        HashMap hm = new HashMap();
        hm.put("bTp", billType);
        hm.put("bcl", billClassType);
        hm.put("ins", institution);
        hm.put("tDep", toDepartment);
        BillNumber billNumber = billNumberFacade.findFreshByJpql(sql, hm);
        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillType(billType);
            billNumber.setBillClassType(billClassType);
            billNumber.setInstitution(institution);
            billNumber.setToDepartment(toDepartment);
            sql = "SELECT count(b) FROM Bill b "
                    + " where b.billType=:bTp "
                    + " and b.retired=false"
                    + " and type(b)=:class"
                    + " and b.institution=:ins "
                    + " and b.toDepartment=:tDep";
            hm = new HashMap();
            hm.put("bTp", billType);
            hm.put("ins", institution);
            hm.put("tDep", toDepartment);

            switch (billClassType) {
                case BilledBill:
                    hm.put("class", BilledBill.class);
                    break;
                case CancelledBill:
                    hm.put("class", CancelledBill.class);
                    break;
                case RefundBill:
                    hm.put("class", RefundBill.class);
                    break;
                case PreBill:
                    hm.put("class", PreBill.class);
                    break;
            }
            Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
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

    private BillNumber fetchLastBillNumber(Institution institution, BillType billType, BillClassType billClassType) {
        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billType=:bTp "
                + " and b.billClassType=:bcl"
                + " and b.institution=:ins "
                + " and b.toDepartment is null ";
        HashMap hm = new HashMap();
        hm.put("bTp", billType);
        hm.put("bcl", billClassType);
        hm.put("ins", institution);
        BillNumber billNumber = billNumberFacade.findFirstByJpql(sql, hm);

        if (billNumber == null && billType == BillType.StoreOrderApprove) {
            sql = "SELECT b FROM "
                    + " Bill b "
                    + " where b.retired=false "
                    + " and  b.billType=:bTp "
                    + " and b.billClassType=:bcl "
                    + " and b.institution=:ins "
                    + " order by b.id desc ";
            hm = new HashMap();
            hm.put("bTp", BillType.StoreOrderApprove);
            hm.put("bcl", billClassType);
            hm.put("ins", institution);
            Bill bill = billFacade.findFirstByJpql(sql, hm);

            if (bill != null) {
                String[] parts = splitByBillNumberDelimiter(bill.getInsId());
                if (parts.length > 1) {
                    billNumber = new BillNumber();
                    billNumber.setBillType(billType);
                    billNumber.setBillClassType(billClassType);
                    billNumber.setInstitution(institution);
                    billNumber.setLastBillNumber(Long.valueOf(parts[1]));
                    billNumberFacade.create(billNumber);
                    return billNumber;
                }
            }
        }
        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillType(billType);
            billNumber.setBillClassType(billClassType);
            billNumber.setInstitution(institution);

            sql = "SELECT count(b) FROM Bill b "
                    + " where b.billType=:bTp "
                    + " and b.retired=false"
                    + " and b.deptId is not null "
                    + " and type(b)=:class"
                    + " and b.institution=:ins "
                    + " and b.toDepartment is null  ";
            hm = new HashMap();
            hm.put("bTp", billType);
            hm.put("ins", institution);

            switch (billClassType) {
                case BilledBill:
                    hm.put("class", BilledBill.class);
                    break;
                case CancelledBill:
                    hm.put("class", CancelledBill.class);
                    break;
                case RefundBill:
                    hm.put("class", RefundBill.class);
                    break;
                case PreBill:
                    hm.put("class", PreBill.class);
                    break;
            }

            Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

            if (dd == null) {
                dd = 0L;
            }

            billNumber.setLastBillNumber(dd);

            billNumberFacade.create(billNumber);
        }

        return billNumber;

    }

    private BillNumber fetchLastBillNumber(Institution institution, BillType billType, BillClassType billClassType, Department department) {
        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billType=:bTp "
                + " and b.billClassType=:bcl"
                + " and b.institution=:ins "
                + " and b.department=:dep ";
        HashMap hm = new HashMap();
        hm.put("bTp", billType);
        hm.put("bcl", billClassType);
        hm.put("ins", institution);
        hm.put("dep", department);
        BillNumber billNumber = billNumberFacade.findFirstByJpql(sql, hm);

        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillType(billType);
            billNumber.setBillClassType(billClassType);
            billNumber.setInstitution(institution);
            billNumber.setDepartment(department);

            sql = "SELECT count(b) FROM Bill b "
                    + " where b.billType=:bTp "
                    + " and b.retired=false "
                    + " and type(b)=:class "
                    + " and b.institution=:ins "
                    + " and b.department=:dep  ";
            hm = new HashMap();
            hm.put("bTp", billType);
            hm.put("ins", institution);
            hm.put("dep", department);

            switch (billClassType) {
                case BilledBill:
                    hm.put("class", BilledBill.class);
                    break;
                case CancelledBill:
                    hm.put("class", CancelledBill.class);
                    break;
                case RefundBill:
                    hm.put("class", RefundBill.class);
                    break;
                case PreBill:
                    hm.put("class", PreBill.class);
                    break;
            }

            Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

            if (dd == null) {
                dd = 0L;
            }

            billNumber.setLastBillNumber(dd);

            billNumberFacade.create(billNumber);
        }

        return billNumber;

    }

    private BillNumber fetchLastBillNumber(Institution institution, List<BillType> billTypes, BillClassType billClassType) {
        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billClassType=:class "
                + " and b.institution=:ins "
                + " and b.billType is null ";
        HashMap hm = new HashMap();
        hm.put("class", billClassType);
        hm.put("ins", institution);
        BillNumber billNumber = billNumberFacade.findFirstByJpql(sql, hm);

        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillClassType(billClassType);
            billNumber.setInstitution(institution);
            HashMap m = new HashMap();

            sql = "SELECT count(b) FROM Bill b"
                    + "  where type(b)=:class "
                    + " and b.retired=false"
                    + " and b.institution=:ins "
                    + " and b.billType in :bt "
                    + " and b.createdAt is not null";

            hm.put("ins", institution);
            hm.put("bt", billTypes);

            switch (billClassType) {
                case BilledBill:
                    hm.put("class", BilledBill.class);
                    break;
                case CancelledBill:
                    hm.put("class", CancelledBill.class);
                    break;
                case RefundBill:
                    hm.put("class", RefundBill.class);
                    break;
                case PreBill:
                    hm.put("class", PreBill.class);
                    break;
            }

            Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

            if (dd == null) {
                dd = 0L;
            }

            billNumber.setLastBillNumber(dd);

            billNumberFacade.create(billNumber);
        }

        return billNumber;

    }

    private BillNumber fetchLastBillNumber(Institution institution, Department department, List<BillType> billTypes, BillClassType billClassType) {
        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billClassType=:class "
                + " and b.institution=:ins "
                + " and b.department=:dep "
                + " and b.billType is null ";
        HashMap hm = new HashMap();
        hm.put("class", billClassType);
        hm.put("ins", institution);
        hm.put("dep", department);
        BillNumber billNumber = billNumberFacade.findFirstByJpql(sql, hm);

        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillClassType(billClassType);
            billNumber.setInstitution(institution);
            billNumber.setDepartment(department);
            HashMap m = new HashMap();

            sql = "SELECT count(b) FROM Bill b"
                    + "  where type(b)=:class "
                    + " and b.retired=false"
                    + " and b.institution=:ins "
                    + " and b.department=:dep "
                    + " and b.billType in :bt "
                    + " and b.createdAt is not null";

            hm.put("ins", institution);
            hm.put("bt", billTypes);
            hm.put("dep", department);

            switch (billClassType) {
                case BilledBill:
                    hm.put("class", BilledBill.class);
                    break;
                case CancelledBill:
                    hm.put("class", CancelledBill.class);
                    break;
                case RefundBill:
                    hm.put("class", RefundBill.class);
                    break;
                case PreBill:
                    hm.put("class", PreBill.class);
                    break;
            }

            Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

            if (dd == null) {
                dd = 0L;
            }

            billNumber.setLastBillNumber(dd);

            billNumberFacade.create(billNumber);
        }

        return billNumber;

    }

    public String departmentBillNumberGenerator(Department dep, Department toDept, BillType billType, BillClassType billClassType) {
        if (dep == null) {
            return "";
        }
        if (toDept == null) {
            return "";
        }

        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Sufix for " + billType, billType.getCode());
        String delimiter = getBillNumberDelimiter();

        boolean customFullBillNumberSuffix = configOptionApplicationController.getBooleanValueByKey("Enable Custom Full Bill Number Sufix");
 
        if (customFullBillNumberSuffix) {
            String template = "{{department_code}}" + delimiter + "{{to_department_code}}" + delimiter + billSuffix;
            String billSuffixTemplate = configOptionApplicationController.getLongTextValueByKey("Bill Number Sufix Template for " + billType, template);
            
            BillNumber billNumber = fetchLastBillNumber(dep, toDept, billType, billClassType);
            
            Long dd = billNumber.getLastBillNumber();

            String detpCode = dep.getDepartmentCode();
            String toDeptCode = toDept.getDepartmentCode();
            String insCode = dep.getInstitution().getInstitutionCode();

            String s = billSuffixTemplate.replace("{{department_code}}", detpCode)
                    .replace("{{to_department_code}}", toDeptCode)
                    .replace("{{ins_code}}", insCode);

            //        dd++;
            //
//        billNumber.setLastBillNumber(dd);
//        billNumberFacade.editAndFlush(billNumber);
            String result = s + dd;
            return result;
        } else {
            BillNumber billNumber = fetchLastBillNumber(dep, toDept, billType, billClassType);
            Long dd = billNumber.getLastBillNumber();
            String result = dep.getDepartmentCode() + toDept.getDepartmentCode() + delimiter + billSuffix + delimiter + dd;
            return result;
        }
    }

    public synchronized String generateBillNumber(Department fromDept, Department toDept, BillType billType, BillClassType billClassType) {
        String fromDeptCode = (fromDept == null) ? "" : fromDept.getCode();
        String toDeptCode = (toDept == null) ? "" : toDept.getCode();

        BillNumber billNumber = fetchLastBillNumber(fromDept, toDept, billType, billClassType);
        Long lastBillNumber = billNumber.getLastBillNumber() + 1;
        String delimiter = getBillNumberDelimiter();

        String result = fromDeptCode + toDeptCode + delimiter + lastBillNumber;

        billNumber.setLastBillNumber(lastBillNumber);
        billNumberFacade.edit(billNumber);

        return result;
    }

    public synchronized String generateBillNumber(Institution institution, BillType billType, BillClassType billClassType) {
        String insCode = (institution == null) ? "" : institution.getCode();
        BillNumber billNumber = fetchLastBillNumber(institution, billType, billClassType);
        Long lastBillNumber = billNumber.getLastBillNumber() + 1;
        String delimiter = getBillNumberDelimiter();
        String result = insCode + delimiter + lastBillNumber;

        billNumber.setLastBillNumber(lastBillNumber);
        billNumberFacade.edit(billNumber);
        return result;
    }

    public String departmentBillNumberGenerator(Institution ins, Department dep, BillType billType, BillClassType billClassType) {
        BillNumber billNumber = fetchLastBillNumber(ins, dep, billType, billClassType);
        Long dd = billNumber.getLastBillNumber();
        String delimiter = getBillNumberDelimiter();
        String result = dep.getDepartmentCode() + delimiter + ++dd;

        billNumber.setLastBillNumber(dd);
        billNumberFacade.edit(billNumber);
        return result;
    }

    public String departmentBillNumberGeneratorYearly(Department dep, BillTypeAtomic billType) {
        if (dep == null) {
            return "";
        }
        if (dep.getInstitution() == null) {
            return "";
        }
        BillNumber billNumber;
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + billType, "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            billSuffix = "";
        }
        boolean commonBillNumberForAllDepartmentsInstitutionsBillTypeAtomic
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Common Bill Number for All Departments, Institutions and Bill Types", false);
        boolean separateBillNumberForAllDepartmentsInstitutionsBillTypeAtomic
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Separate Bill Number for All Departments, Institutions and Bill Types", false);
        boolean separateBillNumberForInstitutionsOnly
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Separate Bill Number for Institutions Only", false);
        boolean separateBillNumberForDepartmentsOnly
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Separate Bill Number for Departments Only", false);
        boolean separateBillNumberForBillTypesOnly
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Separate Bill Number for Bill Types Only", false);

        if (commonBillNumberForAllDepartmentsInstitutionsBillTypeAtomic) {
            billNumber = fetchLastBillNumberForYear(dep.getInstitution(), null, (BillTypeAtomic) null);
        } else if (separateBillNumberForAllDepartmentsInstitutionsBillTypeAtomic) {
            billNumber = fetchLastBillNumberForYear(dep.getInstitution(), dep, billType);
        } else if (separateBillNumberForInstitutionsOnly) {
            billNumber = fetchLastBillNumberForYear(dep.getInstitution(), null, billType);
        } else if (separateBillNumberForDepartmentsOnly) {
            billNumber = fetchLastBillNumberForYear(null, dep, billType);
        } else if (separateBillNumberForBillTypesOnly) {
            billNumber = fetchLastBillNumberForYear(null, null, billType);
        } else {
            billNumber = fetchLastBillNumberForYear(dep.getInstitution());
        }

        // Get the last bill number
        Long dd = billNumber.getLastBillNumber();
        // Increment the bill number
        dd = dd + 1;

        // Set the updated bill number in the BillNumber entity
        billNumber.setLastBillNumber(dd);

        // Update the BillNumber entity in the database
        billNumberFacade.edit(billNumber);

        // Generate the bill number string
        StringBuilder result = new StringBuilder();

        // Append institution code
        if (configOptionApplicationController.getBooleanValueByKey("Add the Institution Code to the Bill Number Generator", true)) {
            result.append(dep.getInstitution().getInstitutionCode());
        }

        // Append department code
        result.append(dep.getDepartmentCode());
        result.append(getBillNumberDelimiter());
        result.append(billSuffix);

        // Append current year (last two digits)
        int year = Calendar.getInstance().get(Calendar.YEAR) % 100; // Get last two digits of year
        result.append(getBillNumberDelimiter());
        result.append(String.format("%02d", year)); // Ensure year is always two digits

        // Append formatted 6-digit bill number
        result.append(getBillNumberDelimiter());
        result.append(String.format("%06d", dd)); // Ensure bill number is always six digits

        // Return the formatted bill number
        return result.toString();
    }

    public String departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(Department dep, BillTypeAtomic billType) {
        if (dep == null) {
            return "";
        }
        if (dep.getInstitution() == null) {
            return "";
        }
        BillNumber billNumber;
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + billType, "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            billSuffix = "";
        }
        billNumber = fetchLastBillNumberForYear(dep.getInstitution(), dep, billType);
        // Get the last bill number
        Long dd = billNumber.getLastBillNumber();
        // Increment the bill number
        dd = dd + 1;
        // Set the updated bill number in the BillNumber entity
        billNumber.setLastBillNumber(dd);
        // Update the BillNumber entity in the database
        billNumberFacade.edit(billNumber);
        // Generate the bill number string - Format: PREFIX{delimiter}INSTITUTION_CODE{delimiter}DEPARTMENT_CODE{delimiter}YEAR{delimiter}YEARLY_NUMBER
        StringBuilder result = new StringBuilder();
        // Append bill suffix as prefix
        result.append(billSuffix);
        result.append(getBillNumberDelimiter());
        // Append institution code
        result.append(dep.getInstitution().getInstitutionCode());
        result.append(getBillNumberDelimiter());
        // Append department code
        result.append(dep.getDepartmentCode());
        result.append(getBillNumberDelimiter());
        // Append current year (last two digits)
        int year = Calendar.getInstance().get(Calendar.YEAR) % 100; // Get last two digits of year
        result.append(String.format("%02d", year)); // Ensure year is always two digits
        result.append(getBillNumberDelimiter());
        // Append formatted 6-digit bill number
        result.append(String.format("%06d", dd)); // Ensure bill number is always six digits
        // Return the formatted bill number
        return result.toString();
    }

    @Deprecated // This method uses existing logic. Use departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide for true institution-wide counting
    public String departmentBillNumberGeneratorYearlyWithPrefixInsYearCount(Department dep, BillTypeAtomic billType) {
        if (dep == null) {
            return "";
        }
        if (dep.getInstitution() == null) {
            return "";
        }
        BillNumber billNumber;
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + billType, "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            billSuffix = "";
        }
        billNumber = fetchLastBillNumberForYear(dep.getInstitution(), null, billType);
        // Get the last bill number
        Long dd = billNumber.getLastBillNumber();
        // Increment the bill number
        dd = dd + 1;
        // Set the updated bill number in the BillNumber entity
        billNumber.setLastBillNumber(dd);
        // Update the BillNumber entity in the database
        billNumberFacade.edit(billNumber);
        // Generate the bill number string - Format: PREFIX{delimiter}INSTITUTION_CODE{delimiter}YEAR{delimiter}YEARLY_NUMBER
        StringBuilder result = new StringBuilder();
        // Append bill suffix as prefix
        result.append(billSuffix);
        result.append(getBillNumberDelimiter());
        // Append institution code
        result.append(dep.getInstitution().getInstitutionCode());
        result.append(getBillNumberDelimiter());
        // Append current year (last two digits)
        int year = Calendar.getInstance().get(Calendar.YEAR) % 100; // Get last two digits of year
        result.append(String.format("%02d", year)); // Ensure year is always two digits
        result.append(getBillNumberDelimiter());
        // Append formatted 6-digit bill number
        result.append(String.format("%06d", dd)); // Ensure bill number is always six digits
        // Return the formatted bill number
        return result.toString();
    }

    @Deprecated // This method uses existing logic. Use institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide for true institution-wide counting
    public String institutionBillNumberGeneratorYearlyWithPrefixInsYearCount(Department dep, BillTypeAtomic billType) {
        if (dep == null) {
            return "";
        }
        if (dep.getInstitution() == null) {
            return "";
        }
        BillNumber billNumber;
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + billType, "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            billSuffix = "";
        }
        billNumber = fetchLastBillNumberForYear(dep.getInstitution(), null, billType);
        // Get the last bill number
        Long dd = billNumber.getLastBillNumber();
        // Increment the bill number
        dd = dd + 1;
        // Set the updated bill number in the BillNumber entity
        billNumber.setLastBillNumber(dd);
        // Update the BillNumber entity in the database
        billNumberFacade.edit(billNumber);
        // Generate the bill number string - Format: PREFIX{delimiter}INSTITUTION_CODE{delimiter}YEAR{delimiter}YEARLY_NUMBER
        StringBuilder result = new StringBuilder();
        // Append bill suffix as prefix
        result.append(billSuffix);
        // Append institution code
        result.append(dep.getInstitution().getInstitutionCode());
        // Append current year (last two digits)
        int year = Calendar.getInstance().get(Calendar.YEAR) % 100; // Get last two digits of year
        result.append(String.format("%02d", year)); // Ensure year is always two digits
        // Append formatted 6-digit bill number
        result.append(String.format("%06d", dd)); // Ensure bill number is always six digits
        // Return the formatted bill number
        return result.toString();
    }

    public BillNumber fetchLastBillNumberForYearInstitutionOnly(Institution institution, BillTypeAtomic billType) {
        String lockKey = getLockKey(institution, null, billType);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());
        lock.lock();
        try {
            return fetchLastBillNumberSynchronizedInstitutionOnly(institution, billType);
        } finally {
            lock.unlock();
        }
    }

    private BillNumber fetchLastBillNumberSynchronizedInstitutionOnly(Institution institution, BillTypeAtomic billType) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        // Find existing BillNumber for institution only (ignoring department)
        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billTypeAtomic=:bTp "
                + " and b.institution=:ins "
                + " and b.toDepartment is null "
                + " AND b.billYear=:yr";
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("bTp", billType);
        hm.put("ins", institution);
        hm.put("yr", currentYear);

        BillNumber billNumber = billNumberFacade.findFreshByJpql(sql, hm);

        if (billNumber == null) {
            // Create new BillNumber for institution-only counting
            billNumber = new BillNumber();
            billNumber.setBillTypeAtomic(billType);
            billNumber.setInstitution(institution);
            billNumber.setToDepartment(null); // This is institution-only counting
            billNumber.setBillYear(currentYear);

            // Count ALL bills for this institution and bill type, regardless of department
            sql = "SELECT count(b) FROM Bill b "
                    + " where b.billTypeAtomic=:bTp "
                    + " and b.retired=false"
                    + " and b.institution=:ins "
                    + " AND b.billDate BETWEEN :startOfYear AND :endOfYear";

            Calendar startOfYear = Calendar.getInstance();
            startOfYear.set(Calendar.DAY_OF_YEAR, 1);
            Calendar endOfYear = Calendar.getInstance();
            endOfYear.set(Calendar.MONTH, 11);  // December
            endOfYear.set(Calendar.DAY_OF_MONTH, 31);

            hm = new HashMap<>();
            hm.put("bTp", billType);
            hm.put("ins", institution);
            hm.put("startOfYear", startOfYear.getTime());
            hm.put("endOfYear", endOfYear.getTime());

            Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
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

    public String departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(Department dep, BillTypeAtomic billType) {
        if (dep == null) {
            return "";
        }
        if (dep.getInstitution() == null) {
            return "";
        }
        BillNumber billNumber;
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + billType, "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            billSuffix = "";
        }
        // Use true institution-wide counting - count all bills for this institution regardless of department
        billNumber = fetchLastBillNumberForYearInstitutionOnly(dep.getInstitution(), billType);
        // Get the last bill number
        Long dd = billNumber.getLastBillNumber();
        // Increment the bill number
        dd = dd + 1;
        // Set the updated bill number in the BillNumber entity
        billNumber.setLastBillNumber(dd);
        // Update the BillNumber entity in the database
        billNumberFacade.edit(billNumber);
        // Generate the bill number string - Format: PREFIX{delimiter}INSTITUTION_CODE{delimiter}YEAR{delimiter}YEARLY_NUMBER
        StringBuilder result = new StringBuilder();
        // Append bill suffix as prefix
        result.append(billSuffix);
        result.append(getBillNumberDelimiter());
        // Append institution code
        result.append(dep.getInstitution().getInstitutionCode());
        result.append(getBillNumberDelimiter());
        // Append current year (last two digits)
        int year = Calendar.getInstance().get(Calendar.YEAR) % 100; // Get last two digits of year
        result.append(String.format("%02d", year)); // Ensure year is always two digits
        result.append(getBillNumberDelimiter());
        // Append formatted 6-digit bill number
        result.append(String.format("%06d", dd)); // Ensure bill number is always six digits
        // Return the formatted bill number
        return result.toString();
    }

    public String institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(Department dep, BillTypeAtomic billType) {
        if (dep == null) {
            return "";
        }
        if (dep.getInstitution() == null) {
            return "";
        }
        BillNumber billNumber;
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + billType, "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            billSuffix = "";
        }
        // Use true institution-wide counting - count all bills for this institution regardless of department
        billNumber = fetchLastBillNumberForYearInstitutionOnly(dep.getInstitution(), billType);
        // Get the last bill number
        Long dd = billNumber.getLastBillNumber();
        // Increment the bill number
        dd = dd + 1;
        // Set the updated bill number in the BillNumber entity
        billNumber.setLastBillNumber(dd);
        // Update the BillNumber entity in the database
        billNumberFacade.edit(billNumber);
        // Generate the bill number string - Format: PREFIX{delimiter}INSTITUTION_CODE{delimiter}YEAR{delimiter}YEARLY_NUMBER
        StringBuilder result = new StringBuilder();
        // Append bill suffix as prefix
        result.append(billSuffix);
        result.append(getBillNumberDelimiter());
        // Append institution code
        result.append(dep.getInstitution().getInstitutionCode());
        result.append(getBillNumberDelimiter());
        // Append current year (last two digits)
        int year = Calendar.getInstance().get(Calendar.YEAR) % 100; // Get last two digits of year
        result.append(String.format("%02d", year)); // Ensure year is always two digits
        result.append(getBillNumberDelimiter());
        // Append formatted 6-digit bill number
        result.append(String.format("%06d", dd)); // Ensure bill number is always six digits
        // Return the formatted bill number
        return result.toString();
    }

    public String departmentBillNumberGeneratorYearly(Department dep, List<BillTypeAtomic> billTypes) {
        if (dep == null) {
            return "";
        }
        if (dep.getInstitution() == null) {
            return "";
        }
        if (billTypes == null || billTypes.isEmpty()) {
            return departmentBillNumberGeneratorYearly(dep, (BillTypeAtomic) null);
        }

        BillTypeAtomic sample = billTypes.get(0);
        BillNumber billNumber;
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + sample, "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            billSuffix = "";
        }
        
        boolean commonBillNumberForAllDepartmentsInstitutionsBillTypeAtomic
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Common Bill Number for All Departments, Institutions and Bill Types", false);
        
        boolean separateBillNumberForAllDepartmentsInstitutionsBillTypeAtomic
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Separate Bill Number for All Departments, Institutions and Bill Types", false);
        
        boolean separateBillNumberForInstitutionsOnly
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Separate Bill Number for Institutions Only", false);
        
        boolean separateBillNumberForDepartmentsOnly
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Separate Bill Number for Departments Only", false);
        
        boolean separateBillNumberForBillTypesOnly
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Separate Bill Number for Bill Types Only", false);
        
        boolean opdBillNumberGenerateStrategyForFromDepartmentAndToDepartmentCombination
                = configOptionApplicationController.getBooleanValueByKey("OPD Bill Number Generation Strategy - Separate Bill Number for fromDepartment, toDepartment and BillTypes", false);
        
        boolean opdBillNumberGenerateStrategySingleNumberForOpdAndInpatientInvestigationsAndServices
                = configOptionApplicationController.getBooleanValueByKey("OPD Bill Number Generation Strategy - Single Number for OPD and Inpatient Investigations and Services", false);
        
        if (commonBillNumberForAllDepartmentsInstitutionsBillTypeAtomic) {
            billNumber = fetchLastBillNumberForYear(null, null, billTypes);
        } else if (separateBillNumberForAllDepartmentsInstitutionsBillTypeAtomic) {
            billNumber = fetchLastBillNumberForYear(dep.getInstitution(), dep, billTypes);
        } else if (separateBillNumberForInstitutionsOnly) {
            billNumber = fetchLastBillNumberForYear(dep.getInstitution(), null, billTypes);
        } else if (separateBillNumberForDepartmentsOnly) {
            billNumber = fetchLastBillNumberForYear(null, dep, billTypes);
        } else if (separateBillNumberForBillTypesOnly) {
            billNumber = fetchLastBillNumberForYear(null, null, billTypes);
        } else if (opdBillNumberGenerateStrategyForFromDepartmentAndToDepartmentCombination) {
            billNumber = fetchLastBillNumberForYear(null, dep, billTypes); //TODO: Correct this in a seperate issue
        } else if (opdBillNumberGenerateStrategySingleNumberForOpdAndInpatientInvestigationsAndServices) {
            billNumber = fetchLastBillNumberForYearSingleNumber(null, dep, billTypes);
        } else {
            billNumber = fetchLastBillNumberForYear(dep.getInstitution());
        }

        Long dd = billNumber.getLastBillNumber();
        dd = dd + 1;

        billNumber.setLastBillNumber(dd);

        billNumberFacade.edit(billNumber);

        StringBuilder result = new StringBuilder();

        boolean addInstitutionCode = configOptionApplicationController.getBooleanValueByKey(
                "Add the Institution Code to the Bill Number Generator", true);

        if (addInstitutionCode) {
            String institutionCode = dep.getInstitution().getInstitutionCode();
            result.append(institutionCode);
        }

        String departmentCode = dep.getDepartmentCode();
        result.append(departmentCode);
        result.append(getBillNumberDelimiter());

        result.append(billSuffix);

        int year = Calendar.getInstance().get(Calendar.YEAR) % 100;

        result.append(getBillNumberDelimiter());
        result.append(String.format("%02d", year));
        result.append(getBillNumberDelimiter());
        result.append(String.format("%06d", dd));

        String finalResult = result.toString();
        return finalResult;
    }

    public BillNumber fetchLastBillNumberSynchronizedForOpdAndInpatientBatchBillForYear( Department department, List<BillTypeAtomic> billTypes) {
        String lockKey = getLockKey(department, billTypes);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return fetchLastBillNumberSynchronizedForOpdAndInpatientBatchBills(department, billTypes);
        } finally {
            lock.unlock();
        }
    }
    

    public String departmentBatchBillNumberGeneratorYearlyForInpatientAndOpdServices(Department dep, List<BillTypeAtomic> billTypes) {
        BillNumber billNumber = fetchLastBillNumberSynchronizedForOpdAndInpatientBatchBillForYear(dep, billTypes);

        Long dd = billNumber.getLastBillNumber();
        dd = dd + 1;
        billNumber.setLastBillNumber(dd);

        billNumberFacade.edit(billNumber);

        StringBuilder result = new StringBuilder();

        boolean addInstitutionCode = configOptionApplicationController.getBooleanValueByKey(
                "Add the Institution Code to the Bill Number Generator", true);

        if (addInstitutionCode) {
            String institutionCode = dep.getInstitution().getInstitutionCode();
            result.append(institutionCode);
        }

        String departmentCode = dep.getDepartmentCode();
        result.append(departmentCode);
        result.append(getBillNumberDelimiter());
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for OPD and Inpatient Service Batch Bills", "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            billSuffix = "";
        }
        result.append(billSuffix);

        int year = Calendar.getInstance().get(Calendar.YEAR) % 100;

        result.append(getBillNumberDelimiter());
        result.append(String.format("%02d", year));
        result.append(getBillNumberDelimiter());
        result.append(String.format("%06d", dd));

        String finalResult = result.toString();
        return finalResult;
    }

    
    public BillNumber fetchLastdepartmentIndividualBillNumberGeneratorYearlyForInpatientAndOpdServices( Department department, List<BillTypeAtomic> billTypes) {
        String lockKey = getLockKey(department, billTypes);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return fetchLastBillNumberSynchronizedForOpdAndInpatientIndividualBills(department, billTypes);
        } finally {
            lock.unlock();
        }
    }

    public String departmentIndividualBillNumberGeneratorYearlyForInpatientAndOpdServices(Department dep, List<BillTypeAtomic> billTypes) {
        BillNumber billNumber = fetchLastdepartmentIndividualBillNumberGeneratorYearlyForInpatientAndOpdServices(dep, billTypes);

        Long dd = billNumber.getLastBillNumber();
        dd = dd + 1;
        billNumber.setLastBillNumber(dd);

        billNumberFacade.edit(billNumber);

        StringBuilder result = new StringBuilder();

        boolean addInstitutionCode = configOptionApplicationController.getBooleanValueByKey(
                "Add the Institution Code to the Bill Number Generator", true);

        if (addInstitutionCode) {
            String institutionCode = dep.getInstitution().getInstitutionCode();
            result.append(institutionCode);
        }

        String departmentCode = dep.getDepartmentCode();
        result.append(departmentCode);
        result.append(getBillNumberDelimiter());
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for OPD and Inpatient Service Batch Bills", "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            billSuffix = "";
        }
        result.append(billSuffix);

        int year = Calendar.getInstance().get(Calendar.YEAR) % 100;

        result.append(getBillNumberDelimiter());
        result.append(String.format("%02d", year));
        result.append(getBillNumberDelimiter());
        result.append(String.format("%06d", dd));

        String finalResult = result.toString();
        return finalResult;
    }

    public String departmentBillNumberGeneratorYearly(Department dep, Department billingDepartment, BillTypeAtomic billType) {
        if (dep == null) {
            return "";
        }
        if (dep.getInstitution() == null) {
            return "";
        }
        BillNumber billNumber;
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + billType, "");

        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            billSuffix = "";
        }
        boolean commonBillNumberForAllDepartmentsInstitutionsBillTypeAtomic
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Common Bill Number for All Departments, Institutions and Bill Types", false);
        boolean separateBillNumberForAllDepartmentsInstitutionsBillTypeAtomic
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Separate Bill Number for All Departments, Institutions and Bill Types", false);
        boolean separateBillNumberForInstitutionsOnly
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Separate Bill Number for Institutions Only", false);
        boolean separateBillNumberForDepartmentsOnly
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Separate Bill Number for Departments Only", false);
        boolean separateBillNumberForBillTypesOnly
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Separate Bill Number for Bill Types Only", false);
        boolean separateBillNumberForFromDepartmentToDepaertmentAndBillTypeAtomic
                = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy - Separate Bill Number for fromDepartment, toDepartment and BillTypes", false);

        if (commonBillNumberForAllDepartmentsInstitutionsBillTypeAtomic) {
            billNumber = fetchLastBillNumberForYear(null, null, (BillTypeAtomic) null);
        } else if (separateBillNumberForAllDepartmentsInstitutionsBillTypeAtomic) {
            billNumber = fetchLastBillNumberForYear(dep.getInstitution(), dep, billType);
        } else if (separateBillNumberForInstitutionsOnly) {
            billNumber = fetchLastBillNumberForYear(dep.getInstitution(), null, billType);
        } else if (separateBillNumberForDepartmentsOnly) {
            billNumber = fetchLastBillNumberForYear(null, dep, billType);
        } else if (separateBillNumberForBillTypesOnly) {
            billNumber = fetchLastBillNumberForYear(null, null, billType);
        } else if (separateBillNumberForFromDepartmentToDepaertmentAndBillTypeAtomic) {
            billNumber = fetchLastBillNumberForYear(null, billingDepartment, dep, billType);
        } else {
            billNumber = fetchLastBillNumberForYear(dep.getInstitution());
        }

        // Get the last bill number
        Long dd = billNumber.getLastBillNumber();
        // Increment the bill number
        dd = dd + 1;

        // Set the updated bill number in the BillNumber entity
        billNumber.setLastBillNumber(dd);

        // Update the BillNumber entity in the database
        billNumberFacade.edit(billNumber);

        // Generate the bill number string
        StringBuilder result = new StringBuilder();

        // Append institution code
        if (configOptionApplicationController.getBooleanValueByKey("Add the Institution Code to the Bill Number Generator", true)) {
            result.append(dep.getInstitution().getInstitutionCode());
        }

        if (billingDepartment.getDepartmentCode() != null) {
            result.append(billingDepartment.getDepartmentCode());
            result.append(getBillNumberDelimiter());
        }

        // Append department code
        result.append(dep.getDepartmentCode());
        result.append(getBillNumberDelimiter());

        if (!billSuffix.isEmpty()) {
            result.append(getBillNumberDelimiter());
            result.append(billSuffix);
        }

        // Append current year (last two digits)
        int year = Calendar.getInstance().get(Calendar.YEAR) % 100; // Get last two digits of year

        result.append(String.format("%02d", year)); // Ensure year is always two digits

        // Append formatted 6-digit bill number
        result.append(getBillNumberDelimiter());
        result.append(String.format("%06d", dd)); // Ensure bill number is always six digits

        // Return the formatted bill number
        return result.toString();
    }

    public String departmentBillNumberGeneratorYearlyByFromDepartmentAndToDepartment(Department dep, Department billingDepartment, BillTypeAtomic billType) {
        if (dep == null) {
            return "";
        }
        if (dep.getInstitution() == null) {
            return "";
        }
        BillNumber billNumber;
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + billType, "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            billSuffix = "";
        }

        billNumber = fetchLastBillNumberForYear(null, billingDepartment, dep, billType);

        // Get the last bill number
        Long dd = billNumber.getLastBillNumber();
        // Increment the bill number
        dd = dd + 1;

        // Set the updated bill number in the BillNumber entity
        billNumber.setLastBillNumber(dd);

        // Update the BillNumber entity in the database
        billNumberFacade.edit(billNumber);

        // Generate the bill number string
        StringBuilder result = new StringBuilder();

        // Append institution code
        if (configOptionApplicationController.getBooleanValueByKey("Add the Institution Code to the Bill Number Generator", true)) {
            result.append(dep.getInstitution().getInstitutionCode());
        }

        if (billingDepartment.getDepartmentCode() != null) {
            result.append(billingDepartment.getDepartmentCode());
            result.append(getBillNumberDelimiter());
        }

        // Append department code
        result.append(dep.getDepartmentCode());
        result.append(getBillNumberDelimiter());

        if (!billSuffix.isEmpty()) {
            result.append(billSuffix);
            result.append(getBillNumberDelimiter());
        }

        // Append current year (last two digits)
        int year = Calendar.getInstance().get(Calendar.YEAR) % 100; // Get last two digits of year

        result.append(String.format("%02d", year)); // Ensure year is always two digits

        // Append formatted 6-digit bill number
        result.append(getBillNumberDelimiter());
        result.append(String.format("%06d", dd)); // Ensure bill number is always six digits

        // Return the formatted bill number
        return result.toString();
    }

    public String departmentBillNumberGeneratorYearly(Department dep, BillTypeAtomic billType, PaymentMethod paymentMethod) {
        if (dep == null) {
            return "";
        }
        if (dep.getInstitution() == null) {
            return "";
        }
        if (paymentMethod == null) {
            return "";
        }
        BillNumber billNumber;
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + billType, "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            billSuffix = "";
        }

        billNumber = fetchLastBillNumberForYear(dep.getInstitution(), dep, billType);

        // Get the last bill number
        Long dd = billNumber.getLastBillNumber();
        // Increment the bill number
        dd = dd + 1;

        // Set the updated bill number in the BillNumber entity
        billNumber.setLastBillNumber(dd);

        // Update the BillNumber entity in the database
        billNumberFacade.edit(billNumber);

        // Generate the bill number string
        StringBuilder result = new StringBuilder();

        // Append institution code
        if (configOptionApplicationController.getBooleanValueByKey("Add the Institution Code to the Bill Number Generator", true)) {
            result.append(dep.getInstitution().getInstitutionCode());
        }

        // Append department code
        result.append(dep.getDepartmentCode());
        result.append(getBillNumberDelimiter());
        result.append(billSuffix);

        // AppendPaymentMethod
        result.append(paymentMethod);
        result.append(getBillNumberDelimiter());
        result.append(billSuffix);
        // Append current year (last two digits)
        int year = Calendar.getInstance().get(Calendar.YEAR) % 100; // Get last two digits of year
        result.append(getBillNumberDelimiter());
        result.append(String.format("%02d", year)); // Ensure year is always two digits

        // Append formatted 6-digit bill number
        result.append(getBillNumberDelimiter());
        result.append(String.format("%06d", dd)); // Ensure bill number is always six digits

        // Return the formatted bill number
        return result.toString();
    }

    @Deprecated //Use public String departmentBillNumberGeneratorYearly(Department dep, BillTypeAtomic billType)
    public String departmentBillNumberGeneratorYearly(Institution ins, Department dep, BillType billType, BillClassType billClassType) {
        BillNumber billNumber = fetchLastBillNumberForYear(ins);
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + billType + billClassType, billType.getCode());
        String delimiter = getBillNumberDelimiter();

        // Get the last bill number
        Long dd = billNumber.getLastBillNumber();

        // Increment the bill number
        dd++;

        // Set the updated bill number in the BillNumber entity
        billNumber.setLastBillNumber(dd);

        // Update the BillNumber entity in the database
        billNumberFacade.edit(billNumber);

        if (configOptionApplicationController.getBooleanValueByKey("Enable Custom Full Bill Number Sufix")) {
            String template = "{{department_code}}" + delimiter + "{{to_department_code}}" + delimiter + billSuffix;
            String billSuffixTemplate = configOptionApplicationController.getLongTextValueByKey("Bill Number Sufix Template for " + billType, template);
            String detpCode = dep.getDepartmentCode();
            String insCode = dep.getInstitution().getInstitutionCode();
            String s = billSuffixTemplate.replace("{{department_code}}", detpCode)
                    .replace("{{ins_code}}", insCode);

            StringBuilder result = new StringBuilder();
            result.append(s);
//        dd++;
            // Append current year (last two digits)
            int year = Calendar.getInstance().get(Calendar.YEAR) % 100; // Get last two digits of year
            result.append(getBillNumberDelimiter());
            result.append(String.format("%02d", year)); // Ensure year is always two digits

            // Append formatted 6-digit bill number
            result.append(getBillNumberDelimiter());
            result.append(String.format("%06d", dd));
//        billNumber.setLastBillNumber(dd);
//        billNumberFacade.editAndFlush(billNumber);
            // Ensure bill number is always six digits

            return result.toString();

        } else {

            // Generate the bill number string
            StringBuilder result = new StringBuilder();

            // Append institution code
            result.append(ins.getInstitutionCode());

            // Append department code
            result.append(dep.getDepartmentCode());
            result.append(getBillNumberDelimiter());
            result.append(billSuffix);

            // Append current year (last two digits)
            int year = Calendar.getInstance().get(Calendar.YEAR) % 100; // Get last two digits of year
            result.append(getBillNumberDelimiter());
            result.append(String.format("%02d", year)); // Ensure year is always two digits

            // Append formatted 6-digit bill number
            result.append(getBillNumberDelimiter());
            result.append(String.format("%06d", dd)); // Ensure bill number is always six digits

            // Return the formatted bill number
            return result.toString();
        }
    }

    @Deprecated //Use public String departmentBillNumberGeneratorYearly(Department dep, BillTypeAtomic billType)
    public String departmentBillNumberGenerator(Department dep, Department toDept, BillType billType, BillClassType billClassType, BillNumberSuffix billNumberSuffix) {
        BillNumber billNumber = fetchLastBillNumber(dep, toDept, billType, billClassType);
        Long dd = billNumber.getLastBillNumber();
        StringBuilder result = new StringBuilder();

        result.append(dep.getDepartmentCode());

        if (toDept != null) {
            result.append(toDept.getDepartmentCode());
        }

        if (billNumberSuffix != BillNumberSuffix.NONE) {
            result.append(billNumberSuffix);
        }

        result.append(getBillNumberDelimiter());
        result.append(++dd);

        billNumber.setLastBillNumber(dd);
        billNumberFacade.edit(billNumber);

        return result.toString();
    }

    public String departmentBillNumberGenerator(Department dep, BillType billType, BillClassType billClassType, BillNumberSuffix billNumberSuffix) {
        BillNumber billNumber = fetchLastBillNumber(dep, billType, billClassType);
        Long dd = billNumber.getLastBillNumber();
        StringBuilder result = new StringBuilder();

        result.append(dep.getDepartmentCode());

        result.append(getBillNumberDelimiter());

        if (billNumberSuffix != BillNumberSuffix.NONE) {
            result.append(billNumberSuffix);
        }

        result.append(getBillNumberDelimiter());
        result.append(++dd);

        billNumber.setLastBillNumber(dd);
        billNumberFacade.edit(billNumber);

        return result.toString();
    }

//    public String departmentBillNumberGenerator(Bill bill, Department toDepartment, BillClassType billClassType) {
//        BillNumber billNumber = fetchLastBillNumber(bill.getDepartment(), toDepartment, bill.getBillType(), billClassType);
//        Long dd = billNumber.getLastBillNumber();
//        String result = "";
//
//        result += bill.getDepartment().getDepartmentCode();
//
//        if (toDepartment != null) {
//            result += bill.getToDepartment().getDepartmentCode();
//        }
//
//        result += "/";
//
//        dd++;
//
//        result += dd;
//
//        billNumber.setLastBillNumber(dd);
//        billNumberFacade.edit(billNumber);
//
//        return result;
//    }
//    public String departmentBillNumberGenerator(Department department, BillType billType, BillClassType billClassType, BillNumberSuffix billNumberSuffix) {
//        BillNumber billNumber = fetchLastBillNumber(department, billType, billClassType);
//        Long dd = billNumber.getLastBillNumber();
//        String result = "";
//
//        result += bill.getDepartment().getDepartmentCode();
//
//        if (billNumberSuffix != BillNumberSuffix.NONE) {
//            result += billNumberSuffix;
//        }
//
//        result += "/";
//
//        dd++;
//
//        result += dd;
//
//        billNumber.setLastBillNumber(dd);
//        billNumberFacade.edit(billNumber);
//
//        return result;
//    }
//    public String departmentCancelledBill(Department dep, BillType type, BillNumberSuffix billNumberSuffix) {
//        if (dep == null || dep.getId() == null) {
//            return "";
//        }
//        String sql = "SELECT count(b) FROM CancelledBill b where "
//                + " b.retired=false AND b.department=:dp AND b.billType= :btp";
//        String result;
//        HashMap h = new HashMap();
//        h.put("btp", type);
//        h.put("dp", dep);
//        Long b = getBillFacade().findAggregateLong(sql, h, TemporalType.TIMESTAMP);
//
//        if (b != 0) {
//            result = dep.getDepartmentCode() + billNumberSuffix + (b + 1);
//            return result;
//        } else {
//            result = dep.getDepartmentCode() + billNumberSuffix + 1;
//            return result;
//        }
//
//    }
//    public String departmentCancelledBill(Department dep, Department toDept, BillNumberSuffix billNumberSuffix) {
//        if (dep == null || dep.getId() == null) {
//            return "";
//        }
//        String sql = "SELECT count(b) FROM CancelledBill b where b.retired=false "
//                + " AND b.department=:dep AND b.toDepartment=:tDep";
//        String result;
//        HashMap hm = new HashMap();
//        hm.put("dep", dep);
//        hm.put("tDep", toDept);
//        Long b = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
//
//        if (b != 0) {
//            result = dep.getDepartmentCode() + toDept.getDepartmentCode() + billNumberSuffix + (b + 1);
//            return result;
//        } else {
//            result = dep.getDepartmentCode() + toDept.getDepartmentCode() + billNumberSuffix + 1;
//            return result;
//        }
//
//    }
//    public String departmentReturnBill(Department dep, BillType billType, BillNumberSuffix billNumberSuffix) {
//
//        String sql = "SELECT count(b) FROM RefundBill b where b.retired=false "
//                + " AND b.department=:dep and b.billType = :bTp ";
//        String result;
//        HashMap hash = new HashMap();
//        hash.put("bTp", billType);
//        hash.put("dep", dep);
//        Long b = getBillFacade().findAggregateLong(sql, hash, TemporalType.TIMESTAMP);
//
//        if (b != 0) {
//            result = dep.getDepartmentCode() + billNumberSuffix + (b + 1);
//            return result;
//        } else {
//            result = dep.getDepartmentCode() + billNumberSuffix + 1;
//            return result;
//        }
//    }
//    public String departmentRefundBill(Department dep, Department toDept, BillNumberSuffix billNumberSuffix) {
//
//        String sql = "SELECT count(b) FROM RefundBill b where b.retired=false "
//                + " AND b.department=:dep AND b.toDepartment=:tDep";
//        String result;
//        HashMap hm = new HashMap();
//        hm.put("dep", dep);
//        hm.put("tDep", toDept);
//        Long b = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
//
//        if (b != 0) {
//            result = dep.getDepartmentCode() + toDept.getDepartmentCode() + billNumberSuffix + (b + 1);
//            return result;
//        } else {
//            result = dep.getDepartmentCode() + toDept.getDepartmentCode() + billNumberSuffix + 1;
//            return result;
//        }
//
//    }
    public boolean itemCodeExistsInPharmacyOrStoreItems(String code) {
        HashMap hm = new HashMap();
        String sql = "SELECT count(b) FROM Amp b where b.code!=:code ";
        hm.put("code", code);
        long l = getItemFacade().findLongByJpql(sql, hm);
        return l != 0;
    }

    public String pharmacyItemNumberGenerator() {
        for (int i = 1; i < 100000; i++) {
            if (!itemCodeExistsInPharmacyOrStoreItems(i + "")) {
                return i + "";
            }
        }
        return "";
    }

    public String storeItemNumberGenerator() {
        HashMap hm = new HashMap();
        String sql = "SELECT count(b) FROM Amp b where b.retired=false"
                + " and b.departmentType=:dep ";
        hm.put("dep", DepartmentType.Store);
        String result;
        Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.TIMESTAMP);
        dd = dd + 1;
        result = "MS" + dd.toString();
        return result;

    }

    public String storeInventryItemNumberGenerator() {
        HashMap hm = new HashMap();
        String sql = "SELECT count(b) FROM Amp b where b.retired=false and b.departmentType=:dep ";
        hm.put("dep", DepartmentType.Inventry);
        String result;
        Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.TIMESTAMP);
        dd = dd + 1;
        result = "ASS" + dd.toString();
        return result;

    }

//    public String storeInventryItemNumberGenerator() {
//        HashMap hm = new HashMap();
//        String sql = "SELECT count(b) FROM Amp b where b.retired=false and b.departmentType=:dep ";
//        hm.put("dep", DepartmentType.Inventry);
//        String result;
//        Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.TIMESTAMP);
//
//        result = dd.toString();
//
//        return result;
//
//    }
    public String serialNumberGenerater(Institution ins, Department toDept, Item item) {
        if (ins == null) {
            return "";
        }

        String sql = "SELECT count(b) FROM BillItem b where "
                + " b.bill.institution=:ins "
                + " and b.bill.department=:tDep "
                + " and b.item=:item "
                + " and (b.bill.billType=:btp1 or b.bill.billType=:btp2)";

        HashMap hm = new HashMap();
        hm.put("ins", ins);
        hm.put("tDep", toDept);
        hm.put("item", item);
        hm.put("btp1", BillType.PharmacyPurchaseBill);
        hm.put("btp2", BillType.PharmacyGrnBill);
        Long b = getItemFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        String result;
        String delimiter = getBillNumberDelimiter();
        if (b != null && b != 0) {
            b = b + 1;
            if (toDept != null) {
                result = ins.getInstitutionCode() + toDept.getDepartmentCode() + delimiter + b;
            } else {
                result = ins.getInstitutionCode() + delimiter + b;
            }
        } else {
            if (toDept != null) {
                result = ins.getInstitutionCode() + toDept.getDepartmentCode() + delimiter + 1;
            } else {
                result = ins.getInstitutionCode() + delimiter + 1;
            }
        }

        return result;
    }

    public DepartmentFacade getDepFacade() {
        return depFacade;
    }

    public void setDepFacade(DepartmentFacade depFacade) {
        this.depFacade = depFacade;
    }

    public InstitutionFacade getInsFacade() {
        return insFacade;
    }

    public void setInsFacade(InstitutionFacade insFacade) {
        this.insFacade = insFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public Long inventoryItemSerialNumberGenerater(Institution ins, Item item) {
        if (ins == null) {
            return 0L;
        }
        String sql = "SELECT count(b) FROM BillItem b where "
                + " b.bill.institution=:ins "
                + " and b.item=:item "
                + " and (b.bill.billType=:btp1 or b.bill.billType=:btp2)";

        HashMap hm = new HashMap();
        hm.put("ins", ins);
        hm.put("item", item);
        hm.put("btp1", BillType.StoreGrnBill);
        hm.put("btp2", BillType.StorePurchase);
        Long b = getItemFacade().findAggregateLong(sql, hm, TemporalType.DATE);
        return b;
    }

    public Long inventoryItemSerialNumberGeneraterForYear(Institution ins, Item item) {
        if (ins == null) {
            return 0L;
        }
        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        c.set(y, 3, 1, 0, 0, 0);
        Date fd = c.getTime();
        c.set(y + 1, 2, 31, 23, 59, 59);
        Date td = c.getTime();

        String sql = "SELECT count(b) FROM BillItem b where "
                + " b.bill.institution=:ins "
                + " and b.item=:item "
                + " and (b.bill.billType=:btp1 or b.bill.billType=:btp2) "
                + " and b.bill.createdAt between :fd and :td ";

        HashMap hm = new HashMap();
        hm.put("ins", ins);
        hm.put("item", item);
        hm.put("btp1", BillType.StoreGrnBill);
        hm.put("btp2", BillType.StorePurchase);
        hm.put("fd", fd);
        hm.put("td", td);

        return getItemFacade().findAggregateLong(sql, hm, TemporalType.DATE);
    }

}
