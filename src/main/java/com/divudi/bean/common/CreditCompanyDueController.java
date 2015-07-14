/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.DealerDueDetailRow;
import com.divudi.data.dataStructure.InstitutionBills;
import com.divudi.data.dataStructure.InstitutionEncounters;
import com.divudi.data.table.String1Value5;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.CreditBean;
import com.divudi.entity.Bill;
import com.divudi.entity.Institution;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.inward.Admission;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.facade.AdmissionFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.PatientEncounterFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ejb.EJB;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class CreditCompanyDueController implements Serializable {

    private Date fromDate;
    private Date toDate;
    Admission patientEncounter;

    ////////////
    private List<InstitutionBills> items;
    private List<InstitutionEncounters> institutionEncounters;
    List<PatientEncounter> patientEncounters;
    private List<String1Value5> creditCompanyAge;
    private List<String1Value5> filteredList;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private CommonFunctions commonFunctions;
    @EJB
    AdmissionFacade admissionFacade;

    public List<PatientEncounter> getPatientEncounters() {
        return patientEncounters;
    }

    public void setPatientEncounters(List<PatientEncounter> patientEncounters) {
        this.patientEncounters = patientEncounters;
    }

    public void makeNull() {
        fromDate = null;
        toDate = null;
        items = null;
        institutionEncounters = null;
        creditCompanyAge = null;
        filteredList = null;
    }

    public void createAgeTable() {
        makeNull();
        System.err.println("Fill Items");
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBills(true);

        setIns.addAll(list);
        System.err.println("size " + setIns.size());

        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setString(ins.getName());
            setValues(ins, newRow);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }

    }

    public void createAgeAccessTable() {
        makeNull();
        System.err.println("Fill Items");
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBills(false);

        setIns.addAll(list);
        System.err.println("size " + setIns.size());

        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setString(ins.getName());
            setValuesAccess(ins, newRow);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }

    }

    public void createInwardAgeTable() {
        makeNull();
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBht(true, PaymentMethod.Credit);
        System.err.println("Ins Size " + list.size());
        setIns.addAll(list);

        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setInstitution(ins);
            setInwardValues(ins, newRow, PaymentMethod.Credit);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }

    }

    List<DealerDueDetailRow> dealerDueDetailRows;

    public List<DealerDueDetailRow> getDealerDueDetailRows() {
        return dealerDueDetailRows;
    }

    public void setDealerDueDetailRows(List<DealerDueDetailRow> dealerDueDetailRows) {
        this.dealerDueDetailRows = dealerDueDetailRows;
    }

    
    
    public void createInwardAgeDetailAnalysis() {
        dealerDueDetailRows = new ArrayList<>();
        createInwardAgeTable();
        Institution dealer = null;
        for (String1Value5 s : creditCompanyAge) {
            DealerDueDetailRow row = new DealerDueDetailRow();
            if (dealer == null || dealer != s.getInstitution()) {
                dealer = s.getInstitution();
                row.setDealer(dealer);
                row.setZeroToThirty(s.getValue1());
                row.setThirtyToSixty(s.getValue2());
                row.setSixtyToNinty(s.getValue3());
                row.setMoreThanNinty(s.getValue4());
                dealerDueDetailRows.add(row);
            }

            int rowsForDealer = 0;
            if (rowsForDealer < s.getValue1PatientEncounters().size()) {
                rowsForDealer = s.getValue1PatientEncounters().size();
            }
            if (rowsForDealer < s.getValue2PatientEncounters().size()) {
                rowsForDealer = s.getValue2PatientEncounters().size();
            }
            if (rowsForDealer < s.getValue3PatientEncounters().size()) {
                rowsForDealer = s.getValue3PatientEncounters().size();
            }
            if (rowsForDealer < s.getValue4PatientEncounters().size()) {
                rowsForDealer = s.getValue4PatientEncounters().size();
            }

            for (int i = 0; i < rowsForDealer; i++) {
                DealerDueDetailRow rowi = new DealerDueDetailRow();
                if (s.getValue1PatientEncounters().size() > i) {
                    rowi.setZeroToThirtyEncounter(s.getValue1PatientEncounters().get(i));
                }
                if (s.getValue2PatientEncounters().size() > i) {
                    rowi.setThirtyToSixtyEncounter(s.getValue2PatientEncounters().get(i));
                }
                if (s.getValue3PatientEncounters().size() > i) {
                    rowi.setSixtyToNintyEncounter(s.getValue3PatientEncounters().get(i));
                }
                if (s.getValue4PatientEncounters().size() > i) {
                    rowi.setMoreThanNintyEncounter(s.getValue4PatientEncounters().get(i));
                }
                dealerDueDetailRows.add(rowi);
            }

        }
        
        creditCompanyAge=new ArrayList<>();

    }

    public void createInwardCashAgeTable() {
        makeNull();
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBht(true, PaymentMethod.Cash);
        System.err.println("Ins Size " + list.size());
        setIns.addAll(list);

        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setString(ins.getName());
            setInwardValues(ins, newRow, PaymentMethod.Cash);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }

    }

    public void createInwardAgeTableAccess() {
        makeNull();
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBht(false, PaymentMethod.Credit);

        setIns.addAll(list);

        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setString(ins.getName());
            setInwardValuesAccess(ins, newRow, PaymentMethod.Credit);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }

    }

    public void createInwardCashAgeTableAccess() {
        makeNull();
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBht(false, PaymentMethod.Cash);

        setIns.addAll(list);

        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setString(ins.getName());
            setInwardValuesAccess(ins, newRow, PaymentMethod.Cash);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }

    }

    private void setValues(Institution inst, String1Value5 dataTable5Value) {

        List<Bill> lst = getCreditBean().getCreditBills(inst, true);
        System.err.println("Institution Ins " + inst.getName());
        for (Bill b : lst) {

            Long dayCount = getCommonFunctions().getDayCountTillNow(b.getCreatedAt());

            double finalValue = (b.getNetTotal() + b.getPaidAmount());

            System.err.println("DayCount " + dayCount);
            System.err.println("NetTotal " + b.getNetTotal());
            System.err.println("Paid " + b.getPaidAmount());
            System.err.println("Final " + finalValue);

            if (dayCount < 30) {
                dataTable5Value.setValue1(dataTable5Value.getValue1() + finalValue);
            } else if (dayCount < 60) {
                dataTable5Value.setValue2(dataTable5Value.getValue2() + finalValue);
            } else if (dayCount < 90) {
                dataTable5Value.setValue3(dataTable5Value.getValue3() + finalValue);
            } else {
                dataTable5Value.setValue4(dataTable5Value.getValue4() + finalValue);
            }

        }

    }

    private void setValuesAccess(Institution inst, String1Value5 dataTable5Value) {

        List<Bill> lst = getCreditBean().getCreditBills(inst, false);
        System.err.println("Institution Ins " + inst.getName());
        for (Bill b : lst) {

            Long dayCount = getCommonFunctions().getDayCountTillNow(b.getCreatedAt());

            double finalValue = (b.getNetTotal() + b.getPaidAmount());

            System.err.println("DayCount " + dayCount);
            System.err.println("NetTotal " + b.getNetTotal());
            System.err.println("Paid " + b.getPaidAmount());
            System.err.println("Final " + finalValue);

            if (dayCount < 30) {
                dataTable5Value.setValue1(dataTable5Value.getValue1() + finalValue);
            } else if (dayCount < 60) {
                dataTable5Value.setValue2(dataTable5Value.getValue2() + finalValue);
            } else if (dayCount < 90) {
                dataTable5Value.setValue3(dataTable5Value.getValue3() + finalValue);
            } else {
                dataTable5Value.setValue4(dataTable5Value.getValue4() + finalValue);
            }

        }

    }

    private void setInwardValues(Institution inst, String1Value5 dataTable5Value, PaymentMethod paymentMethod) {

        List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounters(inst, true, paymentMethod);
        System.err.println("Institution Ins " + inst.getName());
        for (PatientEncounter b : lst) {

            Long dayCount = getCommonFunctions().getDayCountTillNow(b.getCreatedAt());
            b.setTransDayCount(dayCount);
            double finalValue = b.getFinalBill().getNetTotal() - (b.getFinalBill().getPaidAmount() + b.getCreditPaidAmount());

            if (dayCount < 30) {
                dataTable5Value.setValue1(dataTable5Value.getValue1() + finalValue);
                dataTable5Value.getValue1PatientEncounters().add(b);
            } else if (dayCount < 60) {
                dataTable5Value.setValue2(dataTable5Value.getValue2() + finalValue);
                dataTable5Value.getValue2PatientEncounters().add(b);
            } else if (dayCount < 90) {
                dataTable5Value.setValue3(dataTable5Value.getValue3() + finalValue);
                dataTable5Value.getValue3PatientEncounters().add(b);
            } else {
                dataTable5Value.setValue4(dataTable5Value.getValue4() + finalValue);
                dataTable5Value.getValue4PatientEncounters().add(b);
            }

        }

    }

    private void setInwardValuesAccess(Institution inst, String1Value5 dataTable5Value, PaymentMethod paymentMethod) {

        List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounters(inst, false, paymentMethod);
        System.err.println("Institution Ins " + inst.getName());
        for (PatientEncounter b : lst) {

            Long dayCount = getCommonFunctions().getDayCountTillNow(b.getCreatedAt());

            double finalValue = (Math.abs(b.getCreditUsedAmount()) - Math.abs(b.getCreditPaidAmount()));

            if (dayCount < 30) {
                dataTable5Value.setValue1(dataTable5Value.getValue1() + finalValue);
            } else if (dayCount < 60) {
                dataTable5Value.setValue2(dataTable5Value.getValue2() + finalValue);
            } else if (dayCount < 90) {
                dataTable5Value.setValue3(dataTable5Value.getValue3() + finalValue);
            } else {
                dataTable5Value.setValue4(dataTable5Value.getValue4() + finalValue);
            }

        }

    }

    public CreditCompanyDueController() {
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = com.divudi.java.CommonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    @EJB
    private CreditBean creditBean;

    public void createOpdCreditDue() {
        List<Institution> setIns = getCreditBean().getCreditInstitution(BillType.OpdBill, getFromDate(), getToDate(), true);
        items = new ArrayList<>();
        for (Institution ins : setIns) {
            List<Bill> bills = getCreditBean().getCreditBills(ins, BillType.OpdBill, getFromDate(), getToDate(), true);
            InstitutionBills newIns = new InstitutionBills();
            newIns.setInstitution(ins);
            newIns.setBills(bills);

            for (Bill b : bills) {
                newIns.setTotal(newIns.getTotal() + b.getNetTotal());
                newIns.setPaidTotal(newIns.getPaidTotal() + b.getPaidAmount());
            }

            items.add(newIns);
        }

    }

    public void createOpdCreditAccess() {
        List<Institution> setIns = getCreditBean().getCreditInstitution(BillType.OpdBill, getFromDate(), getToDate(), false);
        items = new ArrayList<>();
        for (Institution ins : setIns) {
            List<Bill> bills = getCreditBean().getCreditBills(ins, BillType.OpdBill, getFromDate(), getToDate(), false);
            InstitutionBills newIns = new InstitutionBills();
            newIns.setInstitution(ins);
            newIns.setBills(bills);

            for (Bill b : bills) {
                newIns.setTotal(newIns.getTotal() + b.getNetTotal());
                newIns.setPaidTotal(newIns.getPaidTotal() + b.getPaidAmount());
            }

            items.add(newIns);
        }

    }

    public void createInwardCreditDue() {
        List<Institution> setIns = getCreditBean().getCreditInstitutionByPatientEncounter(getFromDate(), getToDate(), PaymentMethod.Credit, true);
        System.err.println("Size  Ins " + setIns.size());
        institutionEncounters = new ArrayList<>();
        for (Institution ins : setIns) {
            List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounter(ins, getFromDate(), getToDate(), PaymentMethod.Credit, true);

            System.err.println("Size  Pe " + lst.size());
            InstitutionEncounters newIns = new InstitutionEncounters();
            newIns.setInstitution(ins);
            newIns.setPatientEncounters(lst);

            for (PatientEncounter b : lst) {
                newIns.setTotal(newIns.getTotal() + b.getFinalBill().getNetTotal());
                newIns.setPaidTotalPatient(newIns.getPaidTotalPatient() + b.getFinalBill().getPaidAmount());
                newIns.setPaidTotal(newIns.getPaidTotal() + b.getPaidByCreditCompany());
            }

            institutionEncounters.add(newIns);
        }

    }

    AdmissionType admissionType;
    PaymentMethod paymentMethod;
    @EJB
    PatientEncounterFacade patientEncounterFacade;

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public void createInwardCashDue() {
        HashMap m = new HashMap();
        String sql = " Select b from PatientEncounter b"
                + " where b.retired=false "
                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td "
                + " and (abs(b.finalBill.netTotal)-(abs(b.finalBill.paidAmount)+abs(b.creditPaidAmount))) > 1 ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }
        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = patientEncounterFacade.findBySQL(sql, m, TemporalType.TIMESTAMP);

        if (patientEncounters == null) {
            return;
        }
        billed = 0;
        paidByPatient = 0;
        paidByCompany = 0;
        for (PatientEncounter p : patientEncounters) {
            billed += p.getFinalBill().getNetTotal();
            paidByPatient += p.getFinalBill().getPaidAmount();
            paidByCompany += p.getPaidByCreditCompany();

        }

    }

    double billed;
    double paidByPatient;
    double paidByCompany;

    public double getBilled() {
        return billed;
    }

    public void setBilled(double billed) {
        this.billed = billed;
    }

    public double getPaidByPatient() {
        return paidByPatient;
    }

    public void setPaidByPatient(double paidByPatient) {
        this.paidByPatient = paidByPatient;
    }

    public double getPaidByCompany() {
        return paidByCompany;
    }

    public void setPaidByCompany(double paidByCompany) {
        this.paidByCompany = paidByCompany;
    }

    public void createInwardCreditAccess() {
        List<Institution> setIns = getCreditBean().getCreditInstitutionByPatientEncounter(getFromDate(), getToDate(), PaymentMethod.Credit, false);

        institutionEncounters = new ArrayList<>();
        for (Institution ins : setIns) {
            List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounter(ins, getFromDate(), getToDate(), PaymentMethod.Credit, false);
            InstitutionEncounters newIns = new InstitutionEncounters();
            newIns.setInstitution(ins);
            newIns.setPatientEncounters(lst);

            for (PatientEncounter b : lst) {
                newIns.setTotal(newIns.getTotal() + b.getCreditUsedAmount());
                newIns.setPaidTotal(newIns.getPaidTotal() + b.getCreditPaidAmount());
            }

            institutionEncounters.add(newIns);
        }

    }

    public void createInwardCashAccess() {
        List<Institution> setIns = getCreditBean().getCreditInstitutionByPatientEncounter(getFromDate(), getToDate(), PaymentMethod.Cash, false);

        institutionEncounters = new ArrayList<>();
        for (Institution ins : setIns) {
            List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounter(ins, getFromDate(), getToDate(), PaymentMethod.Cash, false);
            InstitutionEncounters newIns = new InstitutionEncounters();
            newIns.setInstitution(ins);
            newIns.setPatientEncounters(lst);

            for (PatientEncounter b : lst) {
                newIns.setTotal(newIns.getTotal() + b.getCreditUsedAmount());
                newIns.setPaidTotal(newIns.getPaidTotal() + b.getCreditPaidAmount());
            }

            institutionEncounters.add(newIns);
        }

    }

    public List<InstitutionBills> getItems() {
        return items;
    }

    private Institution institution;

    public List<Bill> getItems2() {
        String sql;
        HashMap hm;

        sql = "Select b From Bill b where b.retired=false and b.createdAt "
                + "  between :frm and :to and b.creditCompany=:cc "
                + " and b.paymentMethod= :pm and b.billType=:tp";
        hm = new HashMap();
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("cc", getInstitution());
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tp", BillType.OpdBill);
        return getBillFacade().findBySQL(sql, hm, TemporalType.TIMESTAMP);

    }

    public double getCreditTotal() {
        String sql;
        HashMap hm;

        sql = "Select sum(b.netTotal) From Bill b where b.retired=false and b.createdAt "
                + "  between :frm and :to and b.creditCompany=:cc "
                + " and b.paymentMethod= :pm and b.billType=:tp";
        hm = new HashMap();
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("cc", getInstitution());
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tp", BillType.OpdBill);
        return getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

//    public List<Admission> completePatientDishcargedNotFinalized(String query) {
//        List<Admission> suggestions;
//        String sql;
//        HashMap h = new HashMap();
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//            sql = "select c from Admission c where c.retired=false and "
//                    + " ( c.paymentFinalized is null or c.paymentFinalized=false )"
//                    + " and ( (upper(c.bhtNo) like :q )or (upper(c.patient.person.name)"
//                    + " like :q) ) order by c.bhtNo";
//            ////System.out.println(sql);
//            //      h.put("btp", BillType.InwardPaymentBill);
//            h.put("q", "%" + query.toUpperCase() + "%");
//            //suggestions = admissionFacade().findBySQL(sql, h);
//        }
//        //return suggestions;
//    }
    public void setItems(List<InstitutionBills> items) {
        this.items = items;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public CreditBean getCreditBean() {
        return creditBean;
    }

    public void setCreditBean(CreditBean creditBean) {
        this.creditBean = creditBean;
    }

    public List<String1Value5> getCreditCompanyAge() {
        return creditCompanyAge;
    }

    public void setCreditCompanyAge(List<String1Value5> creditCompanyAge) {
        this.creditCompanyAge = creditCompanyAge;
    }

    public List<String1Value5> getFilteredList() {
        return filteredList;
    }

    public void setFilteredList(List<String1Value5> filteredList) {
        this.filteredList = filteredList;
    }

    public List<InstitutionEncounters> getInstitutionEncounters() {
        return institutionEncounters;
    }

    public void setInstitutionEncounters(List<InstitutionEncounters> institutionEncounters) {
        this.institutionEncounters = institutionEncounters;
    }

    public Admission getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(Admission patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public AdmissionFacade getAdmissionFacade() {
        return admissionFacade;
    }

    public void setAdmissionFacade(AdmissionFacade admissionFacade) {
        this.admissionFacade = admissionFacade;
    }

}
