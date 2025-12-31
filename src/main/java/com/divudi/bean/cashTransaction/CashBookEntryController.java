/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.CashBookEntryData;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.ReportTemplateRowBundle;
import com.divudi.core.data.SitesGroupedIntoInstitutions;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.cashTransaction.CashBook;
import com.divudi.core.entity.cashTransaction.CashBookEntry;
import com.divudi.core.facade.CashBookEntryFacade;
import com.divudi.core.facade.CashBookFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.util.CommonFunctions;
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
        return result;
    }

    public Double fetchStartingBalanceForFromSite(Date date, Institution site) {
        if (site == null) {
            return null;
        }
        String jpql = "select cbe "
                + " from CashBookEntry cbe "
                + " where cbe.retired = :ret "
                + " and cbe.fromSite.id = :siteId"
                + " and cbe.createdAt < :st"
                + " order by cbe.id desc";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("siteId", site.getId());
        params.put("st", CommonFunctions.getStartOfDay(date));
        System.out.println("params = " + params);
        CashBookEntry cbe = cashbookEntryFacade.findFirstByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (cbe != null) {
            Double result = cbe.getFromSiteBalanceAfter();
            return result;
        }
        return null;
    }

    public Double fetchStartingBalanceForFromSite(Date date, Institution site, String paymentMethodStr) {
        if (site == null) {
            return null;
        }
        String jpql = "select cbe "
                + " from CashBookEntry cbe "
                + " where cbe.retired = :ret "
                + " and cbe.fromSite.id = :siteId"
                + " and cbe.createdAt < :st"
                + " order by cbe.id desc";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("siteId", site.getId());
        params.put("st", CommonFunctions.getStartOfDay(date));
        System.out.println("params = " + params);
        CashBookEntry cbe = cashbookEntryFacade.findFirstByJpql(jpql, params, TemporalType.TIMESTAMP);
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(paymentMethodStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (cbe != null) {
            Double result;
            switch (paymentMethod) {
                case Agent:
                    result = cbe.getFromSiteAgentBalanceAfter();
                    break;
                case Card:
                    result = cbe.getFromSiteCardBalanceAfter();
                    break;
                case Cheque:
                    result = cbe.getFromSiteChequeBalanceAfter();
                    break;
                case Slip:
                    result = cbe.getFromSiteSlipBalanceAfter();
                    break;
                case ewallet:
                    result = cbe.getFromSiteEwalletBalanceAfter();
                    break;
                case PatientDeposit:
                    result = cbe.getFromSitePatientDepositBalanceAfter();
                    break;
                case PatientPoints:
                    result = cbe.getFromSitePatientPointsBalanceAfter();
                    break;
                case OnlineSettlement:
                    result = cbe.getFromSiteOnlineSettlementBalanceAfter();
                    break;
                case Cash:
                    result = cbe.getFromSiteCashBalanceAfter();
                    break;
                case Credit:
                    result = cbe.getFromSiteCreditBalanceAfter();
                    break;
                case IOU:
                    result = cbe.getFromSiteIouBalanceAfter();
                    break;
                case OnCall:
                    result = cbe.getFromSiteOnCallBalanceAfter();
                    break;
                case Staff:
                    result = cbe.getFromSiteStaffBalanceAfter();
                    break;
                case Staff_Welfare:
                    result = cbe.getFromSiteStaffWelfareBalanceAfter();
                    break;
                case Voucher:
                    result = cbe.getFromSiteVoucherBalanceAfter();
                    break;
                case MultiplePaymentMethods:
                    result = cbe.getFromSiteMultiplePaymentMethodsBalanceAfter();
                    break;
                default:
                    result = cbe.getFromSiteBalanceAfter();
            }
            return result;
        }
        return null;
    }

    public Double fetchEndingBalanceForFromSite(Date date, Institution site, String paymentMethodStr) {
        if (site == null) {
            return null;
        }
        String jpql = "select cbe "
                + " from CashBookEntry cbe "
                + " where cbe.retired = :ret "
                + " and cbe.fromSite.id = :siteId"
                + " and cbe.createdAt < :et"
                + " order by cbe.id desc";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("siteId", site.getId());
        params.put("et", CommonFunctions.getEndOfDay(date));
        System.out.println("params = " + params);
        CashBookEntry cbe = cashbookEntryFacade.findFirstByJpql(jpql, params, TemporalType.TIMESTAMP);
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(paymentMethodStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (cbe != null) {
            Double result;
            switch (paymentMethod) {
                case Agent:
                    result = cbe.getFromSiteAgentBalanceAfter();
                    break;
                case Card:
                    result = cbe.getFromSiteCardBalanceAfter();
                    break;
                case Cheque:
                    result = cbe.getFromSiteChequeBalanceAfter();
                    break;
                case Slip:
                    result = cbe.getFromSiteSlipBalanceAfter();
                    break;
                case ewallet:
                    result = cbe.getFromSiteEwalletBalanceAfter();
                    break;
                case PatientDeposit:
                    result = cbe.getFromSitePatientDepositBalanceAfter();
                    break;
                case PatientPoints:
                    result = cbe.getFromSitePatientPointsBalanceAfter();
                    break;
                case OnlineSettlement:
                    result = cbe.getFromSiteOnlineSettlementBalanceAfter();
                    break;
                case Cash:
                    result = cbe.getFromSiteCashBalanceAfter();
                    break;
                case Credit:
                    result = cbe.getFromSiteCreditBalanceAfter();
                    break;
                case IOU:
                    result = cbe.getFromSiteIouBalanceAfter();
                    break;
                case OnCall:
                    result = cbe.getFromSiteOnCallBalanceAfter();
                    break;
                case Staff:
                    result = cbe.getFromSiteStaffBalanceAfter();
                    break;
                case Staff_Welfare:
                    result = cbe.getFromSiteStaffWelfareBalanceAfter();
                    break;
                case Voucher:
                    result = cbe.getFromSiteVoucherBalanceAfter();
                    break;
                case MultiplePaymentMethods:
                    result = cbe.getFromSiteMultiplePaymentMethodsBalanceAfter();
                    break;
                default:
                    result = cbe.getFromSiteBalanceAfter();
            }
            return result;
        }
        return null;
    }

    public Double fetchEndingBalanceForFromSite(Date date, Institution site) {
        if (site == null) {
            return null;
        }
        String jpql = "select cbe "
                + " from CashBookEntry cbe "
                + " where cbe.retired = :ret "
                + " and cbe.fromSite.id = :siteId"
                + " and cbe.createdAt < :et"
                + " order by cbe.id desc";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("siteId", site.getId());
        params.put("et", CommonFunctions.getEndOfDay(date));
        System.out.println("params = " + params);
        CashBookEntry cbe = cashbookEntryFacade.findFirstByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (cbe != null) {
            Double result = cbe.getFromSiteBalanceAfter();
            return result;
        }
        return null;
    }

    public Double fetchSumOfEntryValuesForFromSite(Date date, Institution site) {
        if (site == null) {
            return null;
        }
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
        return result;
    }

    public Double fetchSumOfEntryValuesForFromSite(Date date, Institution site, String paymentMethodStr) {
        if (site == null) {
            return null;
        }
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(paymentMethodStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
        String jpqlField = "";
        switch (paymentMethod) {
            case Agent:
                jpqlField = "agentValue";
                break;
            case Card:
                jpqlField = "cardValue";
                break;
            case Cheque:
                jpqlField = "chequeValue";
                break;
            case Slip:
                jpqlField = "slipValue";
                break;
            case ewallet:
                jpqlField = "ewalletValue";
                break;
            case PatientDeposit:
                jpqlField = "patientDepositValue";
                break;
            case PatientPoints:
                jpqlField = "patientPointsValue";
                break;
            case OnlineSettlement:
                jpqlField = "onlineSettlementValue";
                break;
            case Cash:
                jpqlField = "cashValue";
                break;
            case Credit:
                jpqlField = "creditValue";
                break;
            case IOU:
                jpqlField = "iouValue";
                break;
            case OnCall:
                jpqlField = "onCallValue";
                break;
            case Staff:
                jpqlField = "staffValue";
                break;
            case Staff_Welfare:
                jpqlField = "staffWelfareValue";
                break;
            case Voucher:
                jpqlField = "voucherValue";
                break;
            case MultiplePaymentMethods:
                jpqlField = "multiplePaymentMethodsValue";
                break;

            default:
                jpqlField = "cashValue";
        }
        String jpql = "select sum(cbe." + jpqlField + ") "
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
        CashBookEntry lastToSiteEntry = fetchLastCashbookEntryForToSite(entryData.getToDepartment().getSite());

        //ALL
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

        //CASH
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
        cbe.setToSiteCashBalanceAfter(cbe.getToSiteCashBalanceBefore() + entryData.getCashValue());

        //CARD
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

        //AGENT
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

        //CHEQUE
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
        cbe.setFromDepartmentStaffBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentStaffBalanceAfter() : 0.0);
        cbe.setToDepartmentStaffBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentStaffBalanceAfter() : 0.0);
        cbe.setFromInstitutionStaffBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionStaffBalanceAfter() : 0.0);
        cbe.setToInstitutionStaffBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionStaffBalanceAfter() : 0.0);
        cbe.setFromSiteStaffBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteStaffBalanceAfter() : 0.0);
        cbe.setToSiteStaffBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteStaffBalanceAfter() : 0.0);

        cbe.setFromDepartmentStaffBalanceAfter(cbe.getFromDepartmentStaffBalanceBefore() + entryData.getStaffValue());
        cbe.setToDepartmentStaffBalanceAfter(cbe.getToDepartmentStaffBalanceBefore() + entryData.getStaffValue());
        cbe.setFromInstitutionStaffBalanceAfter(cbe.getFromInstitutionStaffBalanceBefore() + entryData.getStaffValue());
        cbe.setToInstitutionStaffBalanceAfter(cbe.getToInstitutionStaffBalanceBefore() + entryData.getStaffValue());
        cbe.setFromSiteStaffBalanceAfter(cbe.getFromSiteStaffBalanceBefore() + entryData.getStaffValue());
        cbe.setToSiteStaffBalanceAfter(cbe.getToSiteStaffBalanceBefore() + entryData.getStaffValue());

// Credit
        cbe.setFromDepartmentCreditBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentCreditBalanceAfter() : 0.0);
        cbe.setToDepartmentCreditBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentCreditBalanceAfter() : 0.0);
        cbe.setFromInstitutionCreditBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionCreditBalanceAfter() : 0.0);
        cbe.setToInstitutionCreditBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionCreditBalanceAfter() : 0.0);
        cbe.setFromSiteCreditBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteCreditBalanceAfter() : 0.0);
        cbe.setToSiteCreditBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteCreditBalanceAfter() : 0.0);

        cbe.setFromDepartmentCreditBalanceAfter(cbe.getFromDepartmentCreditBalanceBefore() + entryData.getCreditValue());
        cbe.setToDepartmentCreditBalanceAfter(cbe.getToDepartmentCreditBalanceBefore() + entryData.getCreditValue());
        cbe.setFromInstitutionCreditBalanceAfter(cbe.getFromInstitutionCreditBalanceBefore() + entryData.getCreditValue());
        cbe.setToInstitutionCreditBalanceAfter(cbe.getToInstitutionCreditBalanceBefore() + entryData.getCreditValue());
        cbe.setFromSiteCreditBalanceAfter(cbe.getFromSiteCreditBalanceBefore() + entryData.getCreditValue());
        cbe.setToSiteCreditBalanceAfter(cbe.getToSiteCreditBalanceBefore() + entryData.getCreditValue());

// Staff_Welfare
        cbe.setFromDepartmentStaffWelfareBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentStaffWelfareBalanceAfter() : 0.0);
        cbe.setToDepartmentStaffWelfareBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentStaffWelfareBalanceAfter() : 0.0);
        cbe.setFromInstitutionStaffWelfareBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionStaffWelfareBalanceAfter() : 0.0);
        cbe.setToInstitutionStaffWelfareBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionStaffWelfareBalanceAfter() : 0.0);
        cbe.setFromSiteStaffWelfareBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteStaffWelfareBalanceAfter() : 0.0);
        cbe.setToSiteStaffWelfareBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteStaffWelfareBalanceAfter() : 0.0);

        cbe.setFromDepartmentStaffWelfareBalanceAfter(cbe.getFromDepartmentStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());
        cbe.setToDepartmentStaffWelfareBalanceAfter(cbe.getToDepartmentStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());
        cbe.setFromInstitutionStaffWelfareBalanceAfter(cbe.getFromInstitutionStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());
        cbe.setToInstitutionStaffWelfareBalanceAfter(cbe.getToInstitutionStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());
        cbe.setFromSiteStaffWelfareBalanceAfter(cbe.getFromSiteStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());
        cbe.setToSiteStaffWelfareBalanceAfter(cbe.getToSiteStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());

// Voucher
        cbe.setFromDepartmentVoucherBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentVoucherBalanceAfter() : 0.0);
        cbe.setToDepartmentVoucherBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentVoucherBalanceAfter() : 0.0);
        cbe.setFromInstitutionVoucherBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionVoucherBalanceAfter() : 0.0);
        cbe.setToInstitutionVoucherBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionVoucherBalanceAfter() : 0.0);
        cbe.setFromSiteVoucherBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteVoucherBalanceAfter() : 0.0);
        cbe.setToSiteVoucherBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteVoucherBalanceAfter() : 0.0);

        cbe.setFromDepartmentVoucherBalanceAfter(cbe.getFromDepartmentVoucherBalanceBefore() + entryData.getVoucherValue());
        cbe.setToDepartmentVoucherBalanceAfter(cbe.getToDepartmentVoucherBalanceBefore() + entryData.getVoucherValue());
        cbe.setFromInstitutionVoucherBalanceAfter(cbe.getFromInstitutionVoucherBalanceBefore() + entryData.getVoucherValue());
        cbe.setToInstitutionVoucherBalanceAfter(cbe.getToInstitutionVoucherBalanceBefore() + entryData.getVoucherValue());
        cbe.setFromSiteVoucherBalanceAfter(cbe.getFromSiteVoucherBalanceBefore() + entryData.getVoucherValue());
        cbe.setToSiteVoucherBalanceAfter(cbe.getToSiteVoucherBalanceBefore() + entryData.getVoucherValue());

// IOU
        cbe.setFromDepartmentIouBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentIouBalanceAfter() : 0.0);
        cbe.setToDepartmentIouBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentIouBalanceAfter() : 0.0);
        cbe.setFromInstitutionIouBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionIouBalanceAfter() : 0.0);
        cbe.setToInstitutionIouBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionIouBalanceAfter() : 0.0);
        cbe.setFromSiteIouBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteIouBalanceAfter() : 0.0);
        cbe.setToSiteIouBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteIouBalanceAfter() : 0.0);

        cbe.setFromDepartmentIouBalanceAfter(cbe.getFromDepartmentIouBalanceBefore() + entryData.getIouValue());
        cbe.setToDepartmentIouBalanceAfter(cbe.getToDepartmentIouBalanceBefore() + entryData.getIouValue());
        cbe.setFromInstitutionIouBalanceAfter(cbe.getFromInstitutionIouBalanceBefore() + entryData.getIouValue());
        cbe.setToInstitutionIouBalanceAfter(cbe.getToInstitutionIouBalanceBefore() + entryData.getIouValue());
        cbe.setFromSiteIouBalanceAfter(cbe.getFromSiteIouBalanceBefore() + entryData.getIouValue());
        cbe.setToSiteIouBalanceAfter(cbe.getToSiteIouBalanceBefore() + entryData.getIouValue());

// Agent
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

// Slip
        cbe.setFromDepartmentSlipBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentSlipBalanceAfter() : 0.0);
        cbe.setToDepartmentSlipBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentSlipBalanceAfter() : 0.0);
        cbe.setFromInstitutionSlipBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionSlipBalanceAfter() : 0.0);
        cbe.setToInstitutionSlipBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionSlipBalanceAfter() : 0.0);
        cbe.setFromSiteSlipBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteSlipBalanceAfter() : 0.0);
        cbe.setToSiteSlipBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteSlipBalanceAfter() : 0.0);

        cbe.setFromDepartmentSlipBalanceAfter(cbe.getFromDepartmentSlipBalanceBefore() + entryData.getSlipValue());
        cbe.setToDepartmentSlipBalanceAfter(cbe.getToDepartmentSlipBalanceBefore() + entryData.getSlipValue());
        cbe.setFromInstitutionSlipBalanceAfter(cbe.getFromInstitutionSlipBalanceBefore() + entryData.getSlipValue());
        cbe.setToInstitutionSlipBalanceAfter(cbe.getToInstitutionSlipBalanceBefore() + entryData.getSlipValue());
        cbe.setFromSiteSlipBalanceAfter(cbe.getFromSiteSlipBalanceBefore() + entryData.getSlipValue());
        cbe.setToSiteSlipBalanceAfter(cbe.getToSiteSlipBalanceBefore() + entryData.getSlipValue());

// eWallet
        cbe.setFromDepartmentEwalletBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentEwalletBalanceAfter() : 0.0);
        cbe.setToDepartmentEwalletBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentEwalletBalanceAfter() : 0.0);
        cbe.setFromInstitutionEwalletBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionEwalletBalanceAfter() : 0.0);
        cbe.setToInstitutionEwalletBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionEwalletBalanceAfter() : 0.0);
        cbe.setFromSiteEwalletBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteEwalletBalanceAfter() : 0.0);
        cbe.setToSiteEwalletBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteEwalletBalanceAfter() : 0.0);

        cbe.setFromDepartmentEwalletBalanceAfter(cbe.getFromDepartmentEwalletBalanceBefore() + entryData.getEwalletValue());
        cbe.setToDepartmentEwalletBalanceAfter(cbe.getToDepartmentEwalletBalanceBefore() + entryData.getEwalletValue());
        cbe.setFromInstitutionEwalletBalanceAfter(cbe.getFromInstitutionEwalletBalanceBefore() + entryData.getEwalletValue());
        cbe.setToInstitutionEwalletBalanceAfter(cbe.getToInstitutionEwalletBalanceBefore() + entryData.getEwalletValue());
        cbe.setFromSiteEwalletBalanceAfter(cbe.getFromSiteEwalletBalanceBefore() + entryData.getEwalletValue());
        cbe.setToSiteEwalletBalanceAfter(cbe.getToSiteEwalletBalanceBefore() + entryData.getEwalletValue());

// Patient Deposit
        cbe.setFromDepartmentPatientDepositBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentPatientDepositBalanceAfter() : 0.0);
        cbe.setToDepartmentPatientDepositBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentPatientDepositBalanceAfter() : 0.0);
        cbe.setFromInstitutionPatientDepositBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionPatientDepositBalanceAfter() : 0.0);
        cbe.setToInstitutionPatientDepositBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionPatientDepositBalanceAfter() : 0.0);
        cbe.setFromSitePatientDepositBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSitePatientDepositBalanceAfter() : 0.0);
        cbe.setToSitePatientDepositBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSitePatientDepositBalanceAfter() : 0.0);

        cbe.setFromDepartmentPatientDepositBalanceAfter(cbe.getFromDepartmentPatientDepositBalanceBefore() + entryData.getPatientDepositValue());
        cbe.setToDepartmentPatientDepositBalanceAfter(cbe.getToDepartmentPatientDepositBalanceBefore() + entryData.getPatientDepositValue());
        cbe.setFromInstitutionPatientDepositBalanceAfter(cbe.getFromInstitutionPatientDepositBalanceBefore() + entryData.getPatientDepositValue());
        cbe.setToInstitutionPatientDepositBalanceAfter(cbe.getToInstitutionPatientDepositBalanceBefore() + entryData.getPatientDepositValue());
        cbe.setFromSitePatientDepositBalanceAfter(cbe.getFromSitePatientDepositBalanceBefore() + entryData.getPatientDepositValue());
        cbe.setToSitePatientDepositBalanceAfter(cbe.getToSitePatientDepositBalanceBefore() + entryData.getPatientDepositValue());

// Patient Points
        cbe.setFromDepartmentPatientPointsBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentPatientPointsBalanceAfter() : 0.0);
        cbe.setToDepartmentPatientPointsBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentPatientPointsBalanceAfter() : 0.0);
        cbe.setFromInstitutionPatientPointsBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionPatientPointsBalanceAfter() : 0.0);
        cbe.setToInstitutionPatientPointsBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionPatientPointsBalanceAfter() : 0.0);
        cbe.setFromSitePatientPointsBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSitePatientPointsBalanceAfter() : 0.0);
        cbe.setToSitePatientPointsBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSitePatientPointsBalanceAfter() : 0.0);

        cbe.setFromDepartmentPatientPointsBalanceAfter(cbe.getFromDepartmentPatientPointsBalanceBefore() + entryData.getPatientPointsValue());
        cbe.setToDepartmentPatientPointsBalanceAfter(cbe.getToDepartmentPatientPointsBalanceBefore() + entryData.getPatientPointsValue());
        cbe.setFromInstitutionPatientPointsBalanceAfter(cbe.getFromInstitutionPatientPointsBalanceBefore() + entryData.getPatientPointsValue());
        cbe.setToInstitutionPatientPointsBalanceAfter(cbe.getToInstitutionPatientPointsBalanceBefore() + entryData.getPatientPointsValue());
        cbe.setFromSitePatientPointsBalanceAfter(cbe.getFromSitePatientPointsBalanceBefore() + entryData.getPatientPointsValue());
        cbe.setToSitePatientPointsBalanceAfter(cbe.getToSitePatientPointsBalanceBefore() + entryData.getPatientPointsValue());

// Online Settlement
        cbe.setFromDepartmentOnlineSettlementBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentOnlineSettlementBalanceAfter() : 0.0);
        cbe.setToDepartmentOnlineSettlementBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentOnlineSettlementBalanceAfter() : 0.0);
        cbe.setFromInstitutionOnlineSettlementBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionOnlineSettlementBalanceAfter() : 0.0);
        cbe.setToInstitutionOnlineSettlementBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionOnlineSettlementBalanceAfter() : 0.0);
        cbe.setFromSiteOnlineSettlementBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteOnlineSettlementBalanceAfter() : 0.0);
        cbe.setToSiteOnlineSettlementBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteOnlineSettlementBalanceAfter() : 0.0);

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

    public void writeCashBookEntryAtHandover(Payment p, CashBook cb, Bill handoverAcceptBill) {
        System.out.println("writeCashBookEntryAtHandover");
        System.out.println("p = " + p);
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
        newCbEntry.setBill(handoverAcceptBill);
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
