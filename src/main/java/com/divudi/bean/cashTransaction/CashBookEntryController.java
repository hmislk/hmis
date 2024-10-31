/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.CashBookEntryData;
import com.divudi.data.PaymentMethod;
import static com.divudi.data.PaymentMethod.Card;
import static com.divudi.data.PaymentMethod.Cash;
import static com.divudi.data.PaymentMethod.OnCall;
import static com.divudi.data.PaymentMethod.PatientDeposit;
import com.divudi.data.ReportTemplateRow;
import com.divudi.data.ReportTemplateRowBundle;
import com.divudi.data.SitesGroupedIntoInstitutions;
import com.divudi.entity.Bill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Payment;
import com.divudi.entity.cashTransaction.CashBook;
import com.divudi.entity.cashTransaction.CashBookEntry;
import com.divudi.facade.CashBookEntryFacade;
import com.divudi.facade.CashBookFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Lawan Chaamindu
 */
@Named
@SessionScoped
public class CashBookEntryController implements Serializable {

    @EJB
    private CashBookEntryFacade cashbookEntryFacade;
    @EJB
    private CashBookFacade cashbookFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    DepartmentFacade departmentFacade;

    private CashBook cashBook;
    @Inject
    private SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    private CashBookEntry current;
    private List<CashBookEntry> cashBookEntryList;

    boolean doNotWriteCashBookEntriesAtBillingForAnyPaymentMethod = true;
    private List<SitesGroupedIntoInstitutions> sitesGroupedIntoInstitutionses;
    private List<Date> dates;

    private Date fromDate;
    private Date toDate;

    public void generateDailyCashbookSummary() {
        sitesGroupedIntoInstitutionses = new ArrayList<>();

        Date fd = CommonFunctions.getStartOfDay(fromDate);
        Date td = CommonFunctions.getEndOfDay(toDate);
        
        List<Department> departmentsFromCashBookEntries = fetchToDepartmentsFromCashbookEntries(fd, td);

        Map<Institution, List<Institution>> institutionToSitesMap = new HashMap<>();

        for (Department d : departmentsFromCashBookEntries) {
            Institution institution = d.getInstitution();
            Institution site = d.getSite();

            institutionToSitesMap
                    .computeIfAbsent(institution, k -> new ArrayList<>())
                    .add(site);
        }

        // Transform the map into a list of SitesGroupedIntoInstitutions
        for (Map.Entry<Institution, List<Institution>> entry : institutionToSitesMap.entrySet()) {
            SitesGroupedIntoInstitutions grouped = new SitesGroupedIntoInstitutions();
            grouped.setInstitution(entry.getKey());
            // Use a Set to remove duplicate sites, then convert it back to a List
            List<Institution> uniqueSites = new ArrayList<>(new HashSet<>(entry.getValue()));
            grouped.setSites(uniqueSites);
            sitesGroupedIntoInstitutionses.add(grouped);
        }

        // Generate the list of dates between fromDate and toDate
        dates = getDatesInRange(fromDate, toDate);
    }

    private List<Date> getDatesInRange(Date fromDate, Date toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate and toDate cannot be null");
        }

        List<Date> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        // Normalize fromDate to start of the day
        calendar.setTime(fromDate);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar.getTime();

        // Normalize toDate to start of the day
        calendar.setTime(toDate);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date endDate = calendar.getTime();

        // Validate date range
        if (startDate.after(endDate)) {
            // Return an empty list if fromDate is after toDate
            return dates;
        }

        // Initialize calendar with startDate
        calendar.setTime(startDate);

        while (!calendar.getTime().after(endDate)) {
            // Add the current date to the list
            dates.add(calendar.getTime());
            // Move to the next day
            calendar.add(Calendar.DATE, 1);
        }

        return dates;
    }

    public List<Department> fetchToDepartmentsFromCashbookEntries(Date fd, Date td) {
        String jpql = "select d "
                + " from CashBookEntry cbe, "
                + " cbe.toDepartment d "
                + " where cbe.retired=:ret "
                + " and cbe.createdAt between :fd and :td"
                + " group by d";
        Map params = new HashMap();
        params.put("ret", false);
        params.put("fd", fd);
        params.put("td", td);
        return departmentFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public Double fetchStartingBalanceForToSite(Date date, Institution site) {
        String jpql = "select cbe.toSiteBalanceAfter "
                + " from CashBookEntry cbe, "
                + " cbe.toDepartment d "
                + " where cbe.retired=:ret "
                + " and cbe.toSite=:site"
                + " and cbe.createdAt>:ed"
                + " order by cbe.id";
        Map params = new HashMap();
        params.put("ret", false);
        params.put("site", site);
        params.put("ed", CommonFunctions.getStartOfDay(date));
        System.out.println("params = " + params);
        System.out.println("jpql = " + jpql);
        Double result = departmentFacade.findDoubleByJpql(jpql, params, TemporalType.TIMESTAMP);
        System.out.println("result = " + result);
        return result;
    }

    public Double fetchEndingBalanceForToSite(Date date, Institution site) {
        String jpql = "select cbe.toSiteBalanceAfter "
                + " from CashBookEntry cbe, "
                + " cbe.toDepartment d "
                + " where cbe.retired=:ret "
                + " and cbe.toSite=:site"
                + " and cbe.createdAt<:ed"
                + " order by cbe.id desc";
        Map params = new HashMap();
        params.put("ret", false);
        params.put("site", site);
        params.put("ed", CommonFunctions.getEndOfDay(date));
        System.out.println("params = " + params);
        System.out.println("jpql = " + jpql);
        Double result = departmentFacade.findDoubleByJpql(jpql, params, TemporalType.TIMESTAMP);
        System.out.println("result = " + result);
        return result;
    }

    public Double fetchSumOfEntryValuesForToSite(Date date, Institution site) {
        String jpql = "select sum(cbe.entryValue) "
                + " from CashBookEntry cbe, "
                + " cbe.toDepartment d "
                + " where cbe.retired=:ret "
                + " and cbe.toSite=:site"
                + " and cbe.createdAt>:eds"
                + " and cbe.createdAt<:ede";
        Map params = new HashMap();
        params.put("ret", false);
        params.put("site", site);
        params.put("eds", CommonFunctions.getStartOfDay(date));
        params.put("ede", CommonFunctions.getEndOfDay(date));
        System.out.println("params = " + params);
        System.out.println("jpql = " + jpql);
        Double result = departmentFacade.findDoubleByJpql(jpql, params, TemporalType.TIMESTAMP);
        System.out.println("result = " + result);
        return result;
    }

    public Double fetchStartingBalanceForFromSite(Date date, Institution site) {
        String jpql = "select cbe "
                + " from CashBookEntry cbe "
                + " where cbe.retired = :ret "
                + " and cbe.fromSite.id = :siteId"
                + " and cbe.createdAt > :ed"
                + " order by cbe.id";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("siteId", site.getId());
        params.put("ed", CommonFunctions.getStartOfDay(date));
        System.out.println("params = " + params);
        System.out.println("jpql = " + jpql);
        CashBookEntry cbe = cashbookEntryFacade.findFirstByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (cbe != null) {
            Double result = cbe.getFromSiteBalanceAfter();
            System.out.println("result = " + result);
            return result;
        }
        return null;
    }

    public Double fetchEndingBalanceForFromSite(Date date, Institution site) {
        String jpql = "select cbe "
                + " from CashBookEntry cbe "
                + " where cbe.retired = :ret "
                + " and cbe.fromSite.id = :siteId"
                + " and cbe.createdAt > :sd"
                + " and cbe.createdAt < :ed"
                + " order by cbe.id desc";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("siteId", site.getId());
        params.put("ed", CommonFunctions.getEndOfDay(date));
        params.put("sd", CommonFunctions.getStartOfDay(date));
        System.out.println("params = " + params);
        System.out.println("jpql = " + jpql);
        CashBookEntry cbe = cashbookEntryFacade.findFirstByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (cbe != null) {
            Double result = cbe.getFromSiteBalanceAfter();
            System.out.println("result = " + result);
            return result;
        }
        return null;
    }

    public Double fetchSumOfEntryValuesForFromSite(Date date, Institution site) {
        String jpql = "select sum(cbe.entryValue) "
                + " from CashBookEntry cbe "
                + " where cbe.retired=:ret "
                + " and cbe.fromSite=:site"
                + " and cbe.createdAt>:eds"
                + " and cbe.createdAt<:ede";
        Map params = new HashMap();
        params.put("ret", false);
        params.put("site", site);
        params.put("eds", CommonFunctions.getStartOfDay(date));
        params.put("ede", CommonFunctions.getEndOfDay(date));
        System.out.println("params = " + params);
        System.out.println("jpql = " + jpql);
        Double result = departmentFacade.findDoubleByJpql(jpql, params, TemporalType.TIMESTAMP);
        System.out.println("result = " + result);
        return result;
    }

    public void writeCashBookEntryAtPaymentCreation(Payment p) {
        if (p == null) {
            JsfUtil.addErrorMessage("Cashbook Entry Error !");
            return;
        }
        if (doNotWriteCashBookEntriesAtBillingForAnyPaymentMethod) {
            return;
        }
        if (!chackPaymentMethodForCashBookEntryAtPaymentMethodCreation(p.getPaymentMethod())) {
            return;
        }

        current = new CashBookEntry();
        current.setInstitution(p.getInstitution());
        current.setDepartment(p.getDepartment());
        current.setCreater(sessionController.getLoggedUser());
        current.setCreatedAt(new Date());
        current.setPaymentMethod(p.getPaymentMethod());
        current.setEntryValue(p.getPaidValue());
        current.setPayment(p);
        current.setCashBook(sessionController.getLoggedCashbook());
        current.setSite(sessionController.getDepartment().getSite());
        current.setBill(p.getBill());
        updateBalances(p.getPaymentMethod(), p.getPaidValue(), current);
        cashbookEntryFacade.create(current);

    }

    private CashBookEntry fetchLastCashbookEntry(Department dept) {
        String jpql = "select e "
                + " from CashBookEntry e "
                + " where e.retired=:ret "
                + " and e.department=:dep "
                + " order by e.id desc";
        Map params = new HashMap();
        params.put("ret", false);
        params.put("dept", dept);
        return cashbookEntryFacade.findFirstByJpql(jpql, params, true);
    }

    private CashBookEntry fetchLastCashbookEntry(Institution ins) {
        String jpql = "select e "
                + " from CashBookEntry e "
                + " where e.retired=:ret "
                + " and e.institution=:ins "
                + " order by e.id desc";
        Map params = new HashMap();
        params.put("ret", false);
        params.put("ins", ins);
        return cashbookEntryFacade.findFirstByJpql(jpql, params, true);
    }

    private CashBookEntry fetchLastCashbookEntryForSite(Institution site) {
        String jpql = "select e "
                + " from CashBookEntry e "
                + " where e.retired=:ret "
                + " and e.site=:site "
                + " order by e.id desc";
        Map params = new HashMap();
        params.put("ret", false);
        params.put("site", site);
        return cashbookEntryFacade.findFirstByJpql(jpql, params, true);
    }

    public List<CashBookEntry> writeCashBookEntryAtHandover(ReportTemplateRowBundle bundle, Bill bill, CashBook bundleCb) {
        System.out.println("writeCashBookEntryAtHandover by Bundle");

        if (bundle == null) {
            JsfUtil.addErrorMessage("Cashbook Entry Error - No bundle !");
            return null;
        }
        if (bundleCb == null) {
            JsfUtil.addErrorMessage("Cashbook Entry Error - No cashbook!");
            return null;
        }
        if (bundle.getUser() == null) {
            JsfUtil.addErrorMessage("Cashbook Entry Error  - No user !");
            return null;
        }

        Map<String, CashBookEntryData> cashbookEntryDataMap = new HashMap<>();

        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            if (row.getPayment() == null || row.getPayment().getPaymentMethod() == null || row.getPayment().getBill() == null || row.getPayment().getBill().getBillTypeAtomic() == null) {
                continue;
            }
            if (row.getPayment().getCashbookEntryCompleted()) {
                continue;
            }

            Department fromDepartment = row.getPayment().getBill().getFromDepartment() != null ? row.getPayment().getBill().getFromDepartment() : sessionController.getDepartment();
            Department toDepartment = row.getPayment().getBill().getToDepartment() != null ? row.getPayment().getBill().getToDepartment() : sessionController.getDepartment();

            String departmentKey = fromDepartment.getId() + "-" + toDepartment.getId();

            CashBookEntryData entryData = cashbookEntryDataMap.getOrDefault(departmentKey, new CashBookEntryData(fromDepartment, toDepartment));

            PaymentMethod pm = row.getPayment().getPaymentMethod();
            double paidValue = row.getPayment().getPaidValue();

            switch (pm) {
                case Cash:
                    entryData.setCashValue(entryData.getCashValue() + paidValue);
                    break;
                case Card:
                    entryData.setCardValue(entryData.getCardValue() + paidValue);
                    break;
                case Agent:
                    entryData.setAgentValue(entryData.getAgentValue() + paidValue);
                    break;
                case Cheque:
                    entryData.setChequeValue(entryData.getChequeValue() + paidValue);
                    break;
                case Credit:
                    entryData.setCreditValue(entryData.getCreditValue() + paidValue);
                    break;
                case IOU:
                    entryData.setIouValue(entryData.getIouValue() + paidValue);
                    break;
                case MultiplePaymentMethods:
                    entryData.setMultiplePaymentMethodsValue(entryData.getMultiplePaymentMethodsValue() + paidValue);
                    break;
                case OnCall:
                    entryData.setOnCallValue(entryData.getOnCallValue() + paidValue);
                    break;
                case OnlineSettlement:
                    entryData.setOnlineSettlementValue(entryData.getOnlineSettlementValue() + paidValue);
                    break;
                case PatientDeposit:
                    entryData.setPatientDepositValue(entryData.getPatientDepositValue() + paidValue);
                    break;
                case PatientPoints:
                    entryData.setPatientPointsValue(entryData.getPatientPointsValue() + paidValue);
                    break;
                case Slip:
                    entryData.setSlipValue(entryData.getSlipValue() + paidValue);
                    break;
                case Staff:
                    entryData.setStaffValue(entryData.getStaffValue() + paidValue);
                    break;
                case Staff_Welfare:
                    entryData.setStaffWelfareValue(entryData.getStaffWelfareValue() + paidValue);
                    break;
                case Voucher:
                    entryData.setVoucherValue(entryData.getVoucherValue() + paidValue);
                    break;
                case ewallet:
                    entryData.setEwalletValue(entryData.getEwalletValue() + paidValue);
                    break;
            }

            cashbookEntryDataMap.put(departmentKey, entryData);
        }

        List<CashBookEntry> cashbookEntries = new ArrayList<>();

        for (CashBookEntryData entryData : cashbookEntryDataMap.values()) {
            bundleCb = cashbookFacade.findWithLock(bundleCb.getId());
            // Create a new CashBookEntry for each department combination
            CashBookEntry newCbEntry = new CashBookEntry();

            newCbEntry.setCardValue(entryData.getCardValue());
            newCbEntry.setAgentValue(entryData.getAgentValue());
            newCbEntry.setChequeValue(entryData.getChequeValue());
            newCbEntry.setCreditValue(entryData.getCreditValue());
            newCbEntry.setIouValue(entryData.getIouValue());
            newCbEntry.setMultiplePaymentMethodsValue(entryData.getMultiplePaymentMethodsValue());
            newCbEntry.setOnCallValue(entryData.getOnCallValue());
            newCbEntry.setOnlineSettlementValue(entryData.getOnlineSettlementValue());
            newCbEntry.setPatientDepositValue(entryData.getPatientDepositValue());
            newCbEntry.setPatientPointsValue(entryData.getPatientPointsValue());
            newCbEntry.setSlipValue(entryData.getSlipValue());
            newCbEntry.setStaffValue(entryData.getStaffValue());
            newCbEntry.setStaffWelfareValue(entryData.getStaffWelfareValue());
            newCbEntry.setVoucherValue(entryData.getVoucherValue());
            newCbEntry.setEwalletValue(entryData.getEwalletValue());

            newCbEntry.setInstitution(bundle.getDepartment().getInstitution());
            newCbEntry.setDepartment(bundle.getDepartment());
            newCbEntry.setSite(bundle.getDepartment().getSite());
            newCbEntry.setWebUser(bundle.getUser());

            newCbEntry.setFromDepartment(entryData.getFromDepartment());
            newCbEntry.setToDepartment(entryData.getToDepartment());

            newCbEntry.setFromInstitution(entryData.getFromDepartment().getInstitution());
            newCbEntry.setToInstitution(entryData.getToDepartment().getInstitution());

            newCbEntry.setFromSite(entryData.getFromDepartment().getSite());
            newCbEntry.setToSite(entryData.getFromDepartment().getSite());

            newCbEntry.setCashbookDate(bundle.getDate());
            newCbEntry.setCreater(sessionController.getLoggedUser());
            newCbEntry.setCreatedAt(new Date());
            newCbEntry.setBill(bill);

            newCbEntry.setCashValue(entryData.getCashValue());
            newCbEntry.setCardValue(entryData.getCardValue());
            newCbEntry.setAgentValue(entryData.getAgentValue());
            newCbEntry.setChequeValue(entryData.getChequeValue());
            newCbEntry.setCreditValue(entryData.getCreditValue());
            newCbEntry.setIouValue(entryData.getIouValue());
            newCbEntry.setMultiplePaymentMethodsValue(entryData.getMultiplePaymentMethodsValue());
            newCbEntry.setOnCallValue(entryData.getOnCallValue());
            newCbEntry.setOnlineSettlementValue(entryData.getOnlineSettlementValue());
            newCbEntry.setPatientDepositValue(entryData.getPatientDepositValue());
            newCbEntry.setPatientPointsValue(entryData.getPatientPointsValue());
            newCbEntry.setSlipValue(entryData.getSlipValue());
            newCbEntry.setStaffValue(entryData.getStaffValue());
            newCbEntry.setStaffWelfareValue(entryData.getStaffWelfareValue());
            newCbEntry.setVoucherValue(entryData.getVoucherValue());
            newCbEntry.setEwalletValue(entryData.getEwalletValue());

            double totalEntryValue
                    = entryData.getCashValue()
                    + entryData.getCardValue()
                    + entryData.getAgentValue()
                    + entryData.getChequeValue()
                    + entryData.getCreditValue()
                    + entryData.getIouValue()
                    + entryData.getMultiplePaymentMethodsValue()
                    + entryData.getOnCallValue()
                    + entryData.getOnlineSettlementValue()
                    + entryData.getPatientDepositValue()
                    + entryData.getPatientPointsValue()
                    + entryData.getSlipValue()
                    + entryData.getStaffValue()
                    + entryData.getStaffWelfareValue()
                    + entryData.getVoucherValue()
                    + entryData.getEwalletValue();
            entryData.setTotal(totalEntryValue);

            newCbEntry.setEntryValue(totalEntryValue);
            newCbEntry.setCashBook(bundleCb);
            updateCashbookEntryBalances(entryData, newCbEntry);

            cashbookEntryFacade.create(newCbEntry);
            cashbookEntries.add(newCbEntry);

            cashbookEntryFacade.edit(newCbEntry);
            cashbookFacade.editAndCommit(bundleCb); // Update CashBook after balance change
        }

        return cashbookEntries;
    }

    private void updateCashbookEntryBalances(CashBookEntryData entryData, CashBookEntry cbe) {
        CashBookEntry lastFromDepartmentEntry = fetchLastCashbookEntryForFromDepartment(entryData.getFromDepartment());
        CashBookEntry lastToDepartmentEntry = fetchLastCashbookEntryForToDepartment(entryData.getToDepartment());
        CashBookEntry lastFromInstitutionEntry = fetchLastCashbookEntryForFromInstitution(entryData.getFromDepartment().getInstitution());
        CashBookEntry lastToInstitutionEntry = fetchLastCashbookEntryForToInstitution(entryData.getToDepartment().getInstitution());
        CashBookEntry lastFromSiteEntry = fetchLastCashbookEntryForFromSite(entryData.getFromDepartment().getSite());
        System.out.println("lastFromSiteEntry = " + lastFromSiteEntry);
        CashBookEntry lastToSiteEntry = fetchLastCashbookEntryForToSite(entryData.getToDepartment().getSite());

        cbe.setFromDepartmentBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentBalanceAfter() : 0.0);
        cbe.setToDepartmentBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getFromDepartmentBalanceAfter() : 0.0);
        cbe.setFromInstitutionBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionBalanceAfter() : 0.0);
        cbe.setToInstitutionBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionBalanceAfter() : 0.0);
        cbe.setFromSiteBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteBalanceAfter() : 0.0);
        cbe.setToSiteBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteBalanceAfter() : 0.0);

        cbe.setFromDepartmentBalanceAfter(cbe.getFromDepartmentBalanceBefore() + entryData.getTotal());
        cbe.setToDepartmentBalanceAfter(cbe.getToDepartmentBalanceBefore() + entryData.getTotal());
        cbe.setFromInstitutionBalanceAfter(cbe.getFromInstitutionBalanceBefore() + entryData.getTotal());
        cbe.setToInstitutionBalanceAfter(cbe.getToInstitutionBalanceBefore() + entryData.getTotal());
        cbe.setFromSiteBalanceAfter(cbe.getFromSiteBalanceBefore() + entryData.getTotal());
        cbe.setToSiteBalanceAfter(cbe.getToSiteBalanceBefore() + entryData.getTotal());

        cbe.setFromDepartmentCashBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentCashBalanceAfter() : 0.0);
        cbe.setToDepartmentCashBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentCashBalanceAfter() : 0.0);
        cbe.setFromInstitutionCashBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionCashBalanceAfter() : 0.0);
        cbe.setToInstitutionCashBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionCashBalanceAfter() : 0.0);
        cbe.setFromSiteCashBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteCashBalanceAfter() : 0.0);
        cbe.setToSiteCashBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteCashBalanceAfter() : 0.0);

        cbe.setFromDepartmentCashBalanceAfter(cbe.getFromDepartmentCashBalanceBefore() + entryData.getCashValue());
        cbe.setToDepartmentCashBalanceAfter(cbe.getToDepartmentCashBalanceBefore() + entryData.getCashValue());
        cbe.setFromInstitutionCashBalanceAfter(cbe.getFromInstitutionCashBalanceBefore() + entryData.getCashValue());
        cbe.setToInstitutionCashBalanceAfter(cbe.getToInstitutionCashBalanceBefore() + entryData.getCashValue());
        cbe.setFromSiteCashBalanceAfter(cbe.getFromSiteCashBalanceBefore() + entryData.getCashValue());

        System.out.println("cbe.getFromSiteCashBalanceBefore() = " + cbe.getFromSiteCashBalanceBefore());
        System.out.println("entryData.getCashValue() = " + entryData.getCashValue());
        System.out.println("cbe.getFromSiteCashBalanceAfter() = " + cbe.getFromSiteCashBalanceAfter());

        cbe.setToSiteCashBalanceAfter(cbe.getToSiteCashBalanceBefore() + entryData.getCashValue());

        cbe.setFromDepartmentCardBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentCardBalanceAfter() : 0.0);
        cbe.setToDepartmentCardBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentCardBalanceAfter() : 0.0);
        cbe.setFromInstitutionCardBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionCardBalanceAfter() : 0.0);
        cbe.setToInstitutionCardBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionCardBalanceAfter() : 0.0);
        cbe.setFromSiteCardBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteCardBalanceAfter() : 0.0);
        cbe.setToSiteCardBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteCardBalanceAfter() : 0.0);

        cbe.setFromDepartmentCardBalanceAfter(cbe.getFromDepartmentCardBalanceBefore() + entryData.getCardValue());
        cbe.setToDepartmentCardBalanceAfter(cbe.getToDepartmentCardBalanceBefore() + entryData.getCardValue());
        cbe.setFromInstitutionCardBalanceAfter(cbe.getFromInstitutionCardBalanceBefore() + entryData.getCardValue());
        cbe.setToInstitutionCardBalanceAfter(cbe.getToInstitutionCardBalanceBefore() + entryData.getCardValue());
        cbe.setFromSiteCardBalanceAfter(cbe.getFromSiteCardBalanceBefore() + entryData.getCardValue());
        cbe.setToSiteCardBalanceAfter(cbe.getToSiteCardBalanceBefore() + entryData.getCardValue());

        cbe.setFromDepartmentAgentBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentAgentBalanceAfter() : 0.0);
        cbe.setToDepartmentAgentBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentAgentBalanceAfter() : 0.0);
        cbe.setFromInstitutionAgentBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionAgentBalanceAfter() : 0.0);
        cbe.setToInstitutionAgentBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionAgentBalanceAfter() : 0.0);
        cbe.setFromSiteAgentBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteAgentBalanceAfter() : 0.0);
        cbe.setToSiteAgentBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteAgentBalanceAfter() : 0.0);

        cbe.setFromDepartmentAgentBalanceAfter(cbe.getFromDepartmentAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setToDepartmentAgentBalanceAfter(cbe.getToDepartmentAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setFromInstitutionAgentBalanceAfter(cbe.getFromInstitutionAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setToInstitutionAgentBalanceAfter(cbe.getToInstitutionAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setFromSiteAgentBalanceAfter(cbe.getFromSiteAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setToSiteAgentBalanceAfter(cbe.getToSiteAgentBalanceBefore() + entryData.getAgentValue());

        cbe.setFromDepartmentChequeBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentChequeBalanceAfter() : 0.0);
        cbe.setToDepartmentChequeBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentChequeBalanceAfter() : 0.0);
        cbe.setFromInstitutionChequeBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionChequeBalanceAfter() : 0.0);
        cbe.setToInstitutionChequeBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionChequeBalanceAfter() : 0.0);
        cbe.setFromSiteChequeBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteChequeBalanceAfter() : 0.0);
        cbe.setToSiteChequeBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteChequeBalanceAfter() : 0.0);

        cbe.setFromDepartmentChequeBalanceAfter(cbe.getFromDepartmentChequeBalanceBefore() + entryData.getChequeValue());
        cbe.setToDepartmentChequeBalanceAfter(cbe.getToDepartmentChequeBalanceBefore() + entryData.getChequeValue());
        cbe.setFromInstitutionChequeBalanceAfter(cbe.getFromInstitutionChequeBalanceBefore() + entryData.getChequeValue());
        cbe.setToInstitutionChequeBalanceAfter(cbe.getToInstitutionChequeBalanceBefore() + entryData.getChequeValue());
        cbe.setFromSiteChequeBalanceAfter(cbe.getFromSiteChequeBalanceBefore() + entryData.getChequeValue());
        cbe.setToSiteChequeBalanceAfter(cbe.getToSiteChequeBalanceBefore() + entryData.getChequeValue());

        cbe.setFromDepartmentChequeBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentChequeBalanceAfter() : 0.0);
        cbe.setToDepartmentChequeBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentChequeBalanceAfter() : 0.0);
        cbe.setFromInstitutionChequeBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionChequeBalanceAfter() : 0.0);
        cbe.setToInstitutionChequeBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionChequeBalanceAfter() : 0.0);
        cbe.setFromSiteChequeBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteChequeBalanceAfter() : 0.0);
        cbe.setToSiteChequeBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteChequeBalanceAfter() : 0.0);

        cbe.setFromDepartmentChequeBalanceAfter(cbe.getFromDepartmentChequeBalanceBefore() + entryData.getChequeValue());
        cbe.setToDepartmentChequeBalanceAfter(cbe.getToDepartmentChequeBalanceBefore() + entryData.getChequeValue());
        cbe.setFromInstitutionChequeBalanceAfter(cbe.getFromInstitutionChequeBalanceBefore() + entryData.getChequeValue());
        cbe.setToInstitutionChequeBalanceAfter(cbe.getToInstitutionChequeBalanceBefore() + entryData.getChequeValue());
        cbe.setFromSiteChequeBalanceAfter(cbe.getFromSiteChequeBalanceBefore() + entryData.getChequeValue());
        cbe.setToSiteChequeBalanceAfter(cbe.getToSiteChequeBalanceBefore() + entryData.getChequeValue());

// Staff
        cbe.setFromDepartmentStaffBalanceAfter(cbe.getFromDepartmentStaffBalanceBefore() + entryData.getStaffValue());
        cbe.setToDepartmentStaffBalanceAfter(cbe.getToDepartmentStaffBalanceBefore() + entryData.getStaffValue());
        cbe.setFromInstitutionStaffBalanceAfter(cbe.getFromInstitutionStaffBalanceBefore() + entryData.getStaffValue());
        cbe.setToInstitutionStaffBalanceAfter(cbe.getToInstitutionStaffBalanceBefore() + entryData.getStaffValue());
        cbe.setFromSiteStaffBalanceAfter(cbe.getFromSiteStaffBalanceBefore() + entryData.getStaffValue());
        cbe.setToSiteStaffBalanceAfter(cbe.getToSiteStaffBalanceBefore() + entryData.getStaffValue());

// Credit
        cbe.setFromDepartmentCreditBalanceAfter(cbe.getFromDepartmentCreditBalanceBefore() + entryData.getCreditValue());
        cbe.setToDepartmentCreditBalanceAfter(cbe.getToDepartmentCreditBalanceBefore() + entryData.getCreditValue());
        cbe.setFromInstitutionCreditBalanceAfter(cbe.getFromInstitutionCreditBalanceBefore() + entryData.getCreditValue());
        cbe.setToInstitutionCreditBalanceAfter(cbe.getToInstitutionCreditBalanceBefore() + entryData.getCreditValue());
        cbe.setFromSiteCreditBalanceAfter(cbe.getFromSiteCreditBalanceBefore() + entryData.getCreditValue());
        cbe.setToSiteCreditBalanceAfter(cbe.getToSiteCreditBalanceBefore() + entryData.getCreditValue());

// Staff_Welfare
        cbe.setFromDepartmentStaffWelfareBalanceAfter(cbe.getFromDepartmentStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());
        cbe.setToDepartmentStaffWelfareBalanceAfter(cbe.getToDepartmentStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());
        cbe.setFromInstitutionStaffWelfareBalanceAfter(cbe.getFromInstitutionStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());
        cbe.setToInstitutionStaffWelfareBalanceAfter(cbe.getToInstitutionStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());
        cbe.setFromSiteStaffWelfareBalanceAfter(cbe.getFromSiteStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());
        cbe.setToSiteStaffWelfareBalanceAfter(cbe.getToSiteStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());

// Voucher
        cbe.setFromDepartmentVoucherBalanceAfter(cbe.getFromDepartmentVoucherBalanceBefore() + entryData.getVoucherValue());
        cbe.setToDepartmentVoucherBalanceAfter(cbe.getToDepartmentVoucherBalanceBefore() + entryData.getVoucherValue());
        cbe.setFromInstitutionVoucherBalanceAfter(cbe.getFromInstitutionVoucherBalanceBefore() + entryData.getVoucherValue());
        cbe.setToInstitutionVoucherBalanceAfter(cbe.getToInstitutionVoucherBalanceBefore() + entryData.getVoucherValue());
        cbe.setFromSiteVoucherBalanceAfter(cbe.getFromSiteVoucherBalanceBefore() + entryData.getVoucherValue());
        cbe.setToSiteVoucherBalanceAfter(cbe.getToSiteVoucherBalanceBefore() + entryData.getVoucherValue());

// IOU
        cbe.setFromDepartmentIouBalanceAfter(cbe.getFromDepartmentIouBalanceBefore() + entryData.getIouValue());
        cbe.setToDepartmentIouBalanceAfter(cbe.getFromDepartmentIouBalanceBefore() + entryData.getIouValue());
        cbe.setFromInstitutionIouBalanceAfter(cbe.getFromInstitutionIouBalanceBefore() + entryData.getIouValue());
        cbe.setToInstitutionIouBalanceAfter(cbe.getToInstitutionIouBalanceBefore() + entryData.getIouValue());
        cbe.setFromSiteIouBalanceAfter(cbe.getFromSiteIouBalanceBefore() + entryData.getIouValue());
        cbe.setToSiteIouBalanceAfter(cbe.getToSiteIouBalanceBefore() + entryData.getIouValue());

// Agent
        cbe.setFromDepartmentAgentBalanceAfter(cbe.getFromDepartmentAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setToDepartmentAgentBalanceAfter(cbe.getToDepartmentAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setFromInstitutionAgentBalanceAfter(cbe.getFromInstitutionAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setToInstitutionAgentBalanceAfter(cbe.getToInstitutionAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setFromSiteAgentBalanceAfter(cbe.getFromSiteAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setToSiteAgentBalanceAfter(cbe.getToSiteAgentBalanceBefore() + entryData.getAgentValue());

// Slip
        cbe.setFromDepartmentSlipBalanceAfter(cbe.getFromDepartmentSlipBalanceBefore() + entryData.getSlipValue());
        cbe.setToDepartmentSlipBalanceAfter(cbe.getToDepartmentSlipBalanceBefore() + entryData.getSlipValue());
        cbe.setFromInstitutionSlipBalanceAfter(cbe.getFromInstitutionSlipBalanceBefore() + entryData.getSlipValue());
        cbe.setToInstitutionSlipBalanceAfter(cbe.getToInstitutionSlipBalanceBefore() + entryData.getSlipValue());
        cbe.setFromSiteSlipBalanceAfter(cbe.getFromSiteSlipBalanceBefore() + entryData.getSlipValue());
        cbe.setToSiteSlipBalanceAfter(cbe.getToSiteSlipBalanceBefore() + entryData.getSlipValue());

// eWallet
        cbe.setFromDepartmentEwalletBalanceAfter(cbe.getFromDepartmentEwalletBalanceBefore() + entryData.getEwalletValue());
        cbe.setToDepartmentEwalletBalanceAfter(cbe.getToDepartmentEwalletBalanceBefore() + entryData.getEwalletValue());
        cbe.setFromInstitutionEwalletBalanceAfter(cbe.getFromInstitutionEwalletBalanceBefore() + entryData.getEwalletValue());
        cbe.setToInstitutionEwalletBalanceAfter(cbe.getToInstitutionEwalletBalanceBefore() + entryData.getEwalletValue());
        cbe.setFromSiteEwalletBalanceAfter(cbe.getFromSiteEwalletBalanceBefore() + entryData.getEwalletValue());
        cbe.setToSiteEwalletBalanceAfter(cbe.getToSiteEwalletBalanceBefore() + entryData.getEwalletValue());

// Patient Deposit
        cbe.setFromDepartmentPatientDepositBalanceAfter(cbe.getFromDepartmentPatientDepositBalanceBefore() + entryData.getPatientDepositValue());
        cbe.setToDepartmentPatientDepositBalanceAfter(cbe.getToDepartmentPatientDepositBalanceBefore() + entryData.getPatientDepositValue());
        cbe.setFromInstitutionPatientDepositBalanceAfter(cbe.getFromInstitutionPatientDepositBalanceBefore() + entryData.getPatientDepositValue());
        cbe.setToInstitutionPatientDepositBalanceAfter(cbe.getToInstitutionPatientDepositBalanceBefore() + entryData.getPatientDepositValue());
        cbe.setFromSitePatientDepositBalanceAfter(cbe.getFromSitePatientDepositBalanceBefore() + entryData.getPatientDepositValue());
        cbe.setToSitePatientDepositBalanceAfter(cbe.getToSitePatientDepositBalanceBefore() + entryData.getPatientDepositValue());

// Patient Points
        cbe.setFromDepartmentPatientPointsBalanceAfter(cbe.getFromDepartmentPatientPointsBalanceBefore() + entryData.getPatientPointsValue());
        cbe.setToDepartmentPatientPointsBalanceAfter(cbe.getToDepartmentPatientPointsBalanceBefore() + entryData.getPatientPointsValue());
        cbe.setFromInstitutionPatientPointsBalanceAfter(cbe.getFromInstitutionPatientPointsBalanceBefore() + entryData.getPatientPointsValue());
        cbe.setToInstitutionPatientPointsBalanceAfter(cbe.getToInstitutionPatientPointsBalanceBefore() + entryData.getPatientPointsValue());
        cbe.setFromSitePatientPointsBalanceAfter(cbe.getFromSitePatientPointsBalanceBefore() + entryData.getPatientPointsValue());
        cbe.setToSitePatientPointsBalanceAfter(cbe.getToSitePatientPointsBalanceBefore() + entryData.getPatientPointsValue());

// Online Settlement
        cbe.setFromDepartmentOnlineSettlementBalanceAfter(cbe.getFromDepartmentOnlineSettlementBalanceBefore() + entryData.getOnlineSettlementValue());
        cbe.setToDepartmentOnlineSettlementBalanceAfter(cbe.getToDepartmentOnlineSettlementBalanceBefore() + entryData.getOnlineSettlementValue());
        cbe.setFromInstitutionOnlineSettlementBalanceAfter(cbe.getFromInstitutionOnlineSettlementBalanceBefore() + entryData.getOnlineSettlementValue());
        cbe.setToInstitutionOnlineSettlementBalanceAfter(cbe.getToInstitutionOnlineSettlementBalanceBefore() + entryData.getOnlineSettlementValue());
        cbe.setFromSiteOnlineSettlementBalanceAfter(cbe.getFromSiteOnlineSettlementBalanceBefore() + entryData.getOnlineSettlementValue());
        cbe.setToSiteOnlineSettlementBalanceAfter(cbe.getToSiteOnlineSettlementBalanceBefore() + entryData.getOnlineSettlementValue());

    }

    private Double getBalanceByPaymentMethod(CashBook cashBook, PaymentMethod pm) {
        switch (pm) {
            case Cash:
                return cashBook.getCashBalance();
            case Card:
                return cashBook.getCardBalance();
            case Agent:
                return cashBook.getAgentBalance();
            case Cheque:
                return cashBook.getChequeBalance();
            case Credit:
                return cashBook.getCreditBalance();
            case IOU:
                return cashBook.getIouBalance();
            case MultiplePaymentMethods:
                return cashBook.getMultiplePaymentMethodsBalance();
            case OnCall:
                return cashBook.getOnCallBalance();
            case OnlineSettlement:
                return cashBook.getOnlineSettlementBalance();
            case PatientDeposit:
                return cashBook.getPatientDepositBalance();
            case PatientPoints:
                return cashBook.getPatientPointsBalance();
            case Slip:
                return cashBook.getSlipBalance();
            case Staff:
                return cashBook.getStaffBalance();
            case Staff_Welfare:
                return cashBook.getStaffWelfareBalance();
            case Voucher:
                return cashBook.getVoucherBalance();
            case ewallet:
                return cashBook.getEwalletBalance();
            default:
                return 0.0;
        }
    }

    private void updateCashBookBalance(CashBook cashBook, PaymentMethod pm, Double newBalance) {
        switch (pm) {
            case Cash:
                cashBook.setCashBalance(newBalance);
                break;
            case Card:
                cashBook.setCardBalance(newBalance);
                break;
            case Agent:
                cashBook.setAgentBalance(newBalance);
                break;
            case Cheque:
                cashBook.setChequeBalance(newBalance);
                break;
            case Credit:
                cashBook.setCreditBalance(newBalance);
                break;
            case IOU:
                cashBook.setIouBalance(newBalance);
                break;
            case MultiplePaymentMethods:
                cashBook.setMultiplePaymentMethodsBalance(newBalance);
                break;
            case OnCall:
                cashBook.setOnCallBalance(newBalance);
                break;
            case OnlineSettlement:
                cashBook.setOnlineSettlementBalance(newBalance);
                break;
            case PatientDeposit:
                cashBook.setPatientDepositBalance(newBalance);
                break;
            case PatientPoints:
                cashBook.setPatientPointsBalance(newBalance);
                break;
            case Slip:
                cashBook.setSlipBalance(newBalance);
                break;
            case Staff:
                cashBook.setStaffBalance(newBalance);
                break;
            case Staff_Welfare:
                cashBook.setStaffWelfareBalance(newBalance);
                break;
            case Voucher:
                cashBook.setVoucherBalance(newBalance);
                break;
            case ewallet:
                cashBook.setEwalletBalance(newBalance);
                break;
            default:
                break;
        }
    }

    public CashBookEntry fetchLastCashbookEntryForFromDepartment(Department dept) {
        String jpql = "select e "
                + " from CashBookEntry e "
                + " where e.retired=:ret "
                + " and e.fromDepartment=:dep"
                + " order by e.id desc";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("dep", dept);
        return cashbookEntryFacade.findFirstByJpql(jpql, m);
    }

    public CashBookEntry fetchLastCashbookEntryForToDepartment(Department dept) {
        String jpql = "select e "
                + " from CashBookEntry e "
                + " where e.retired=:ret "
                + " and e.toDepartment=:dep"
                + " order by e.id desc";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("dep", dept);
        return cashbookEntryFacade.findFirstByJpql(jpql, m);
    }

    public CashBookEntry fetchLastCashbookEntryForFromInstitution(Institution institution) {
        String jpql = "select e "
                + " from CashBookEntry e "
                + " where e.retired = :ret "
                + " and e.fromInstitution = :inst "
                + " order by e.id desc";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("inst", institution);
        return cashbookEntryFacade.findFirstByJpql(jpql, m);
    }

    public CashBookEntry fetchLastCashbookEntryForToInstitution(Institution institution) {
        String jpql = "select e "
                + " from CashBookEntry e "
                + " where e.retired = :ret "
                + " and e.toInstitution = :inst "
                + " order by e.id desc";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("inst", institution);
        return cashbookEntryFacade.findFirstByJpql(jpql, m);
    }

    public CashBookEntry fetchLastCashbookEntryForFromSite(Institution site) {
        String jpql = "select e "
                + " from CashBookEntry e "
                + " where e.retired = :ret "
                + " and e.fromSite = :site "
                + " order by e.id desc";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("site", site);
        return cashbookEntryFacade.findFirstByJpql(jpql, m);
    }

    public CashBookEntry fetchLastCashbookEntryForToSite(Institution site) {
        String jpql = "select e "
                + " from CashBookEntry e "
                + " where e.retired = :ret "
                + " and e.toSite = :site "
                + " order by e.id desc";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("site", site);
        return cashbookEntryFacade.findFirstByJpql(jpql, m);
    }

    private void setBalanceBefore(CashBookEntry cashBookEntry, PaymentMethod pm, Double balanceBefore) {
//        switch (pm) {
//            case Cash:
//                cashBookEntry.setCashBalanceBefore(balanceBefore);
//                break;
//            case Card:
//                cashBookEntry.setCardBalanceBefore(balanceBefore);
//                break;
//            case Agent:
//                cashBookEntry.setAgentBalanceBefore(balanceBefore);
//                break;
//            case Cheque:
//                cashBookEntry.setChequeBalanceBefore(balanceBefore);
//                break;
//            case Credit:
//                cashBookEntry.setCreditBalanceBefore(balanceBefore);
//                break;
//            case IOU:
//                cashBookEntry.setIouBalanceBefore(balanceBefore);
//                break;
//            case MultiplePaymentMethods:
//                cashBookEntry.setMultiplePaymentMethodsBalanceBefore(balanceBefore);
//                break;
//            case OnCall:
//                cashBookEntry.setOnCallBalanceBefore(balanceBefore);
//                break;
//            case OnlineSettlement:
//                cashBookEntry.setOnlineSettlementBalanceBefore(balanceBefore);
//                break;
//            case PatientDeposit:
//                cashBookEntry.setPatientDepositBalanceBefore(balanceBefore);
//                break;
//            case PatientPoints:
//                cashBookEntry.setPatientPointsBalanceBefore(balanceBefore);
//                break;
//            case Slip:
//                cashBookEntry.setSlipBalanceBefore(balanceBefore);
//                break;
//            case Staff:
//                cashBookEntry.setStaffBalanceBefore(balanceBefore);
//                break;
//            case Staff_Welfare:
//                cashBookEntry.setStaffWelfareBalanceBefore(balanceBefore);
//                break;
//            case Voucher:
//                cashBookEntry.setVoucherBalanceBefore(balanceBefore);
//                break;
//            case ewallet:
//                cashBookEntry.setEwalletBalanceBefore(balanceBefore);
//                break;
//            default:
//                break;
//        }
    }

    private void setBalanceAfter(CashBookEntry cashBookEntry, PaymentMethod pm, Double balanceAfter) {
//        switch (pm) {
//            case Cash:
//                cashBookEntry.setCashBalanceAfter(balanceAfter);
//                break;
//            case Card:
//                cashBookEntry.setCardBalanceAfter(balanceAfter);
//                break;
//            case Agent:
//                cashBookEntry.setAgentBalanceAfter(balanceAfter);
//                break;
//            case Cheque:
//                cashBookEntry.setChequeBalanceAfter(balanceAfter);
//                break;
//            case Credit:
//                cashBookEntry.setCreditBalanceAfter(balanceAfter);
//                break;
//            case IOU:
//                cashBookEntry.setIouBalanceAfter(balanceAfter);
//                break;
//            case MultiplePaymentMethods:
//                cashBookEntry.setMultiplePaymentMethodsBalanceAfter(balanceAfter);
//                break;
//            case OnCall:
//                cashBookEntry.setOnCallBalanceAfter(balanceAfter);
//                break;
//            case OnlineSettlement:
//                cashBookEntry.setOnlineSettlementBalanceAfter(balanceAfter);
//                break;
//            case PatientDeposit:
//                cashBookEntry.setPatientDepositBalanceAfter(balanceAfter);
//                break;
//            case PatientPoints:
//                cashBookEntry.setPatientPointsBalanceAfter(balanceAfter);
//                break;
//            case Slip:
//                cashBookEntry.setSlipBalanceAfter(balanceAfter);
//                break;
//            case Staff:
//                cashBookEntry.setStaffBalanceAfter(balanceAfter);
//                break;
//            case Staff_Welfare:
//                cashBookEntry.setStaffWelfareBalanceAfter(balanceAfter);
//                break;
//            case Voucher:
//                cashBookEntry.setVoucherBalanceAfter(balanceAfter);
//                break;
//            case ewallet:
//                cashBookEntry.setEwalletBalanceAfter(balanceAfter);
//                break;
//            default:
//                break;
//        }
    }

    public void writeCashBookEntryAtHandover(Payment p, CashBook cb) {
        System.out.println("writeCashBookEntryAtHandover");
        System.out.println("p = " + p);
        System.out.println("cb = " + cb);
        if (p == null) {
            JsfUtil.addErrorMessage("Cashbook Entry Error !");
            return;
        }

        if (!chackPaymentMethodForCashBookEntryAtHandover(p.getPaymentMethod())) {
            return;
        }
        CashBookEntry newCbEntry = new CashBookEntry();
        newCbEntry.setInstitution(p.getInstitution());
        newCbEntry.setDepartment(p.getDepartment());
        newCbEntry.setSite(p.getDepartment().getSite());
        newCbEntry.setCreater(sessionController.getLoggedUser());
        newCbEntry.setCreatedAt(new Date());
        newCbEntry.setPaymentMethod(p.getPaymentMethod());
        newCbEntry.setEntryValue(p.getPaidValue());
        newCbEntry.setPayment(p);
        newCbEntry.setBill(p.getHandoverAcceptBill());
        newCbEntry.setCashBook(cb);
        updateBalances(p.getPaymentMethod(), p.getPaidValue(), newCbEntry);

        p.setCashbook(cb);
        p.setCashbookEntry(newCbEntry);
        paymentFacade.edit(p);
        cashbookEntryFacade.create(newCbEntry);

    }

    public void updateBalances(PaymentMethod pm, Double Value, CashBookEntry cbe) {
        Map m = new HashMap<>();
        String jpql = "Select cbe from CashBookEntry cbe where "
                + " cbe.paymentMethod=:pm";

        m.put("pm", pm);

        if (cbe.getDepartment() != null) {
            jpql += " and cbe.department=:dep ";
            m.put("dep", cbe.getDepartment());
        }
        if (cbe.getInstitution() != null) {
            jpql += " and cbe.institution=:ins ";
            m.put("ins", cbe.getInstitution());
        }
        if (cbe.getDepartment() != null) {
            jpql += " and cbe.site=:si ";
            m.put("si", cbe.getSite());
        }

        jpql += "order by cbe.id desc";

        CashBookEntry lastCashBookEntry = cashbookEntryFacade.findFirstByJpql(jpql, m);

        Double lastDepartmentBalance;
        Double lastInstitutionBalance;
        Double lastSiteBalance;

        if (lastCashBookEntry == null) {
            lastDepartmentBalance = 0.0;
            lastInstitutionBalance = 0.0;
            lastSiteBalance = 0.0;
        } else {

        }
    }

    public boolean chackPaymentMethodForCashBookEntryAtPaymentMethodCreation(PaymentMethod pm) {
        boolean check = false;
        if (pm == null) {
            JsfUtil.addErrorMessage("Payment method is not found !");
            return false;
        }
        switch (pm) {
            case Card:
                check = true;
                break;

            case Cash:
                check = false;
                break;

            case Cheque:
                check = false;
                break;

            case Agent:
                check = false;
                break;

            case Credit:
                check = false;
                break;

            case OnCall:
                check = false;
                break;

            case PatientDeposit:
                check = true;
                break;

            case Slip:
                check = true;
                break;

            case Staff:
                check = false;
                break;

            case Staff_Welfare:
                check = false;
                break;

            case ewallet:
                check = true;
                break;

            case OnlineSettlement:
                check = true;
                break;

            default:

        }

        return check;

    }

    public boolean chackPaymentMethodForCashBookEntryAtHandover(PaymentMethod pm) {
        boolean check = false;
        if (pm == null) {
            JsfUtil.addErrorMessage("Payment method is not found !");
            return false;
        }
        switch (pm) {
            case Card:
                check = false;
                break;

            case Cash:
                check = true;
                break;

            case Cheque:
                check = false;
                break;

            case Agent:
                check = true;
                break;

            case Credit:
                check = false;
                break;

            case OnCall:
                check = true;
                break;

            case PatientDeposit:
                check = true;
                break;

            case Slip:
                check = false;
                break;

            case Staff:
                check = false;
                break;

            case Staff_Welfare:
                check = false;
                break;

            case ewallet:
                check = false;
                break;

            case OnlineSettlement:
                check = false;
                break;

            default:

        }

        return check;

    }

    public List<CashBookEntry> genarateCashBookEntries(Date fromDate, Date toDate, Institution site, Institution ins, Department dept) {
        String jpql;
        Map m = new HashMap<>();
        jpql = "select b from CashBookEntry b where b.retired=:ret and b.createdAt between :fromDate and :toDate ";
        if (site != null) {
            jpql += "and b.site=:site ";
            m.put("site", site);
        }
        if (ins != null) {
            jpql += "and b.institution=:ins ";
            m.put("ins", ins);
        }
        if (dept != null) {
            jpql += "and b.department=:dept ";
            m.put("dept", dept);
        }
        m.put("fromDate", fromDate);
        m.put("toDate", toDate);
        m.put("ret", false);
        cashBookEntryList = cashbookEntryFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        return cashBookEntryList;
    }

    public CashBookEntryController() {
    }

    public CashBookEntryFacade getCashbookEntryFacade() {
        return cashbookEntryFacade;
    }

    public void setCashbookEntryFacade(CashBookEntryFacade CashbookEntryFacade) {
        this.cashbookEntryFacade = CashbookEntryFacade;
    }

    public CashBook getCashBook() {
        return cashBook;
    }

    public void setCashBook(CashBook cashBook) {
        this.cashBook = cashBook;
    }

    public List<CashBookEntry> getCashBookEntryList() {
        return cashBookEntryList;
    }

    public void setCashBookEntryList(List<CashBookEntry> cashBookEntryList) {
        this.cashBookEntryList = cashBookEntryList;

    }

    public CashBookEntry getCurrent() {
        return current;
    }

    public void setCurrent(CashBookEntry current) {
        this.current = current;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<SitesGroupedIntoInstitutions> getSitesGroupedIntoInstitutionses() {
        return sitesGroupedIntoInstitutionses;
    }

    public void setSitesGroupedIntoInstitutionses(List<SitesGroupedIntoInstitutions> sitesGroupedIntoInstitutionses) {
        this.sitesGroupedIntoInstitutionses = sitesGroupedIntoInstitutionses;
    }

    public List<Date> getDates() {
        return dates;
    }

    public void setDates(List<Date> dates) {
        this.dates = dates;
    }

    /**
     *
     */
    @FacesConverter(forClass = CashBookEntry.class)
    public static class CashBookEntryConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            CashBookEntryController controller = (CashBookEntryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "CashBookEntryController");
            return controller.getCashbookEntryFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof CashBookEntry) {
                CashBookEntry o = (CashBookEntry) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + CashBookEntry.class.getName());
            }
        }
    }

}
