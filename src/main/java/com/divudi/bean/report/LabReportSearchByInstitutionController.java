/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.report;

import com.divudi.bean.common.AuditEventApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.table.String1Value1;

import com.divudi.entity.AuditEvent;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.RefundBill;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.facade.BillFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class LabReportSearchByInstitutionController implements Serializable {

    @Inject
    private SessionController sessionController;
    @Inject
            private AuditEventApplicationController auditEventApplicationController;
    String txtSearch;
    Date fromDate;
    Date toDate;

    CommonFunctions commonFunctions;
    List<Bill> labBills;
    //   Department department;
    private Institution institution;
    @EJB
    BillFacade billFacade;
    double hosTot;
    double disTot;
    double profTot;
    double netTot;
    double labHandover;
    double hosTotB;
    double disTotB;
    double profTotB;
    double netTotB;
    double labHandoverB;
    double hosTotC;
    double disTotC;
    double profTotC;
    double netTotC;
    double labHandoverC;
    double hosTotR;
    double disTotR;
    double profTotR;
    double netTotR;
    double labHandoverR;
    private List<String1Value1> string1Value1s;

    List<PatientInvestigation> searchedPatientInvestigations;

    public List<PatientInvestigation> getSearchedPatientInvestigations() {
        return searchedPatientInvestigations;
    }

    public void setSearchedPatientInvestigations(List<PatientInvestigation> searchedPatientInvestigations) {
        this.searchedPatientInvestigations = searchedPatientInvestigations;
    }

    public List<Bill> getLabBillsB() {
        return labBillsB;
    }

    public void setLabBillsB(List<Bill> labBillsB) {
        this.labBillsB = labBillsB;
    }

    public List<Bill> getLabBillsC() {
        return labBillsC;
    }

    public void setLabBillsC(List<Bill> labBillsC) {
        this.labBillsC = labBillsC;
    }

    public List<Bill> getLabBillsR() {
        return labBillsR;
    }

    public void setLabBillsR(List<Bill> labBillsR) {
        this.labBillsR = labBillsR;
    }

    public void searchAll() {
        String sql;
        if (txtSearch != null) {
            sql = "select pi from PatientInvestigation pi join pi.investigation i join pi.billItem.bill b join b.patient.person p where ((p.name) like '%" + txtSearch.toUpperCase() + "%' or (b.insId) like '%" + txtSearch.toUpperCase() + "%' or p.phone like '%" + txtSearch + "%' or (i.name) like '%" + txtSearch.toUpperCase() + "%' ) order by pi.id desc";
            searchedPatientInvestigations = getPiFacade().findByJpql(sql, 50);
        } else {
            searchedPatientInvestigations = null;
        }
    }

    public double getHosTot() {
        return hosTot;
    }

    public void setHosTot(double hosTot) {
        this.hosTot = hosTot;
    }

    public double getDisTot() {
        return disTot;
    }

    public void setDisTot(double disTot) {
        this.disTot = disTot;
    }

    public double getProfTot() {
        return profTot;
    }

    public void setProfTot(double profTot) {
        this.profTot = profTot;
    }

    public double getNetTot() {
        return netTot;
    }

    public void setNetTot(double netTot) {
        this.netTot = netTot;
    }

    public double getLabHandover() {
        return labHandover;
    }

    public void setLabHandover(double labHandover) {
        this.labHandover = labHandover;
    }

    public void clearTotals() {
        hosTot = 0;
        disTot = 0;
        profTot = 0;
        netTot = 0;
        labHandover = 0;
        hosTotC = 0;
        disTotC = 0;
        profTotC = 0;
        netTotC = 0;
        labHandoverC = 0;
        hosTotB = 0;
        disTotB = 0;
        profTotB = 0;
        netTotB = 0;
        labHandoverB = 0;
        hosTotR = 0;
        disTotR = 0;
        profTotR = 0;
        netTotR = 0;
        labHandoverR = 0;
    }

    private double calHospitalFee(Bill bill, Institution ins) {
        String sql;
        Map tm;

        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and "
                + " type(f) = :billClass and f.billType = :billType and "
                + " f.createdAt between :fromDate and :toDate and f.toInstitution=:ins ";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", bill.getClass());
        tm.put("ins", ins);
        return getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
    }

    private double calProfessionalFee(Bill bill, Institution ins) {
        String sql;
        Map tm;
        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass "
                + " and f.billType = :billType and f.createdAt between :fromDate and :toDate and f.toInstitution=:ins ";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", bill.getClass());
        tm.put("ins", ins);

        return getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
    }

    private double calDiscountFee(Bill bill, Institution ins) {
        String sql;
        Map tm;
        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass "
                + " and f.billType = :billType and f.createdAt between :fromDate and :toDate and f.toInstitution=:ins";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", bill.getClass());
        tm.put("ins", ins);

        return getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);

    }

    private double calHospitalFeeWithout(Bill bill, Institution ins) {
        String sql;
        Map tm;

        sql = "select sum(f.total - f.staffFee) "
                + " from Bill f "
                + " where f.retired=false "
                + " and type(f) = :billClass "
                + " and f.billType = :billType and "
                + " (f.paymentMethod = :pm1 "
                + " or f.paymentMethod = :pm2 "
                + " or f.paymentMethod = :pm3 "
                + " or f.paymentMethod = :pm4) and "
                + " f.createdAt between :fromDate and :toDate "
                + " and f.toInstitution=:ins ";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", bill.getClass());
        tm.put("ins", ins);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        tm.put("pm4", PaymentMethod.Slip);
        return getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
    }

    private double calProfessionalFeeWithout(Bill bill, Institution ins) {
        String sql;
        Map tm;
        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass "
                + " and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 "
                + " or f.paymentMethod = :pm3  or f.paymentMethod = :pm4) and"
                + " f.billType = :billType and f.createdAt between :fromDate and :toDate and f.toInstitution=:ins ";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", bill.getClass());
        tm.put("ins", ins);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        tm.put("pm4", PaymentMethod.Slip);

        return getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
    }

    private double calDiscountFeeWithout(Bill bill, Institution ins) {
        String sql;
        Map tm;
        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass "
                + " and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 "
                + " or f.paymentMethod = :pm3  or f.paymentMethod = :pm4) and "
                + " f.billType = :billType and f.createdAt between :fromDate and :toDate and f.toInstitution=:ins";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", bill.getClass());
        tm.put("ins", ins);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        tm.put("pm4", PaymentMethod.Slip);

        return getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);

    }

    public void calTotals() {
        clearTotals();
        hosTotB = calHospitalFee(new BilledBill(), getInstitution());
        hosTotC = calHospitalFee(new CancelledBill(), getInstitution());
        hosTotR = calHospitalFee(new RefundBill(), getInstitution());

        hosTot = hosTotB + hosTotC + hosTotR;

        profTotB = calProfessionalFee(new BilledBill(), getInstitution());
        profTotC = calProfessionalFee(new CancelledBill(), getInstitution());
        profTotR = calProfessionalFee(new RefundBill(), getInstitution());
        profTot = profTotB + profTotC + profTotR;

        disTotB = calDiscountFee(new BilledBill(), getInstitution());
        disTotC = calDiscountFee(new CancelledBill(), getInstitution());
        disTotR = calDiscountFee(new RefundBill(), getInstitution());
        disTot = disTotB + disTotC + disTotR;

//        hosTot = 0;
//        disTot = 0;
//        profTot = 0;
        netTot = hosTot + profTot - disTot;
        netTotC = hosTotC + profTotC - disTotC;
        netTotB = hosTotB + profTotB - disTotB;
        netTotR = hosTotR + profTotR - disTotR;
        labHandover = netTot - profTot;

    }

    public void calTotalsWithout() {
        clearTotals();
        hosTotB = calHospitalFeeWithout(new BilledBill(), getInstitution());
        hosTotC = calHospitalFeeWithout(new CancelledBill(), getInstitution());
        hosTotR = calHospitalFeeWithout(new RefundBill(), getInstitution());

        hosTot = hosTotB + hosTotC + hosTotR;

        profTotB = calProfessionalFeeWithout(new BilledBill(), getInstitution());
        profTotC = calProfessionalFeeWithout(new CancelledBill(), getInstitution());
        profTotR = calProfessionalFeeWithout(new RefundBill(), getInstitution());
        profTot = profTotB + profTotC + profTotR;

        disTotB = calDiscountFeeWithout(new BilledBill(), getInstitution());
        disTotC = calDiscountFeeWithout(new CancelledBill(), getInstitution());
        disTotR = calDiscountFeeWithout(new RefundBill(), getInstitution());
        disTot = disTotB + disTotC + disTotR;

//        hosTot = 0;
//        disTot = 0;
//        profTot = 0;
        netTot = hosTot + profTot - disTot;
        netTotC = hosTotC + profTotC - disTotC;
        netTotB = hosTotB + profTotB - disTotB;
        netTotR = hosTotR + profTotR - disTotR;
        labHandover = netTot - profTot;

    }

//    public void calTotalsWithout() {
//        clearTotals();
//        String sql;
//        Map tm;
//
//        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 or f.paymentMethod = :pm4) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
//        tm = new HashMap();
//        tm.put("fromDate", fromDate);
//        tm.put("toDate", toDate);
//        tm.put("billType", BillType.OpdBill);
//        tm.put("billClass", BilledBill.class);
//        tm.put("pm1", PaymentMethod.Cash);
//        tm.put("pm2", PaymentMethod.Card);
//        tm.put("pm3", PaymentMethod.Cheque);
//        tm.put("pm4", PaymentMethod.Slip);
//        hosTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
//        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 or f.paymentMethod = :pm4) and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
//        tm = new HashMap();
//        tm.put("fromDate", fromDate);
//        tm.put("toDate", toDate);
//        tm.put("billType", BillType.OpdBill);
//        tm.put("billClass", CancelledBill.class);
//        tm.put("pm1", PaymentMethod.Cash);
//        tm.put("pm2", PaymentMethod.Card);
//        tm.put("pm3", PaymentMethod.Cheque);
//        tm.put("pm4", PaymentMethod.Slip);
//        hosTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
//        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 or f.paymentMethod = :pm4 ) and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
//        tm = new HashMap();
//        tm.put("fromDate", fromDate);
//        tm.put("toDate", toDate);
//        tm.put("billType", BillType.OpdBill);
//        tm.put("billClass", RefundBill.class);
//        tm.put("pm1", PaymentMethod.Cash);
//        tm.put("pm2", PaymentMethod.Card);
//        tm.put("pm3", PaymentMethod.Cheque);
//        tm.put("pm4", PaymentMethod.Slip);
//        hosTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
//        hosTot = hosTotB + hosTotC + hosTotR;
//
//        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 or f.paymentMethod = :pm4 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
//        tm = new HashMap();
//        tm.put("fromDate", fromDate);
//        tm.put("toDate", toDate);
//        tm.put("billType", BillType.OpdBill);
//        tm.put("billClass", BilledBill.class);
//        tm.put("pm1", PaymentMethod.Cash);
//        tm.put("pm2", PaymentMethod.Card);
//        tm.put("pm3", PaymentMethod.Cheque);
//        tm.put("pm4", PaymentMethod.Slip);
//        profTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
//        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 or f.paymentMethod = :pm4 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
//        tm = new HashMap();
//        tm.put("fromDate", fromDate);
//        tm.put("toDate", toDate);
//        tm.put("billType", BillType.OpdBill);
//        tm.put("billClass", CancelledBill.class);
//        tm.put("pm1", PaymentMethod.Cash);
//        tm.put("pm2", PaymentMethod.Card);
//        tm.put("pm3", PaymentMethod.Cheque);
//        tm.put("pm4", PaymentMethod.Slip);
//        profTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
//        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 or f.paymentMethod = :pm4 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
//        tm = new HashMap();
//        tm.put("fromDate", fromDate);
//        tm.put("toDate", toDate);
//        tm.put("billType", BillType.OpdBill);
//        tm.put("billClass", RefundBill.class);
//        tm.put("pm1", PaymentMethod.Cash);
//        tm.put("pm2", PaymentMethod.Card);
//        tm.put("pm3", PaymentMethod.Cheque);
//        tm.put("pm4", PaymentMethod.Slip);
//        profTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
//        profTot = profTotB + profTotC + profTotR;
//
//        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 or f.paymentMethod = :pm4 ) and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
//        tm = new HashMap();
//        tm.put("fromDate", fromDate);
//        tm.put("toDate", toDate);
//        tm.put("billType", BillType.OpdBill);
//        tm.put("billClass", BilledBill.class);
//        tm.put("pm1", PaymentMethod.Cash);
//        tm.put("pm2", PaymentMethod.Card);
//        tm.put("pm3", PaymentMethod.Cheque);
//        tm.put("pm4", PaymentMethod.Slip);
//        disTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
//        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 or f.paymentMethod = :pm4) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
//        tm = new HashMap();
//        tm.put("fromDate", fromDate);
//        tm.put("toDate", toDate);
//        tm.put("billType", BillType.OpdBill);
//        tm.put("billClass", CancelledBill.class);
//        tm.put("pm1", PaymentMethod.Cash);
//        tm.put("pm2", PaymentMethod.Card);
//        tm.put("pm3", PaymentMethod.Cheque);
//        tm.put("pm4", PaymentMethod.Slip);
//        disTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
//        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 or f.paymentMethod = :pm4 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
//        tm = new HashMap();
//        tm.put("fromDate", fromDate);
//        tm.put("toDate", toDate);
//        tm.put("billType", BillType.OpdBill);
//        tm.put("billClass", RefundBill.class);
//        tm.put("pm1", PaymentMethod.Cash);
//        tm.put("pm2", PaymentMethod.Card);
//        tm.put("pm3", PaymentMethod.Cheque);
//        tm.put("pm4", PaymentMethod.Slip);
//        disTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
//        disTot = disTotB + disTotC + disTotR;
//
////        hosTot = 0;
////        disTot = 0;
////        profTot = 0;
//        netTot = hosTot + profTot - disTot;
//        netTotC = hosTotC + profTotC - disTotC;
//        netTotB = hosTotB + profTotB - disTotB;
//        netTotR = hosTotR + profTotR - disTotR;
//        labHandover = netTot - profTot;
//
//    }
    List<Bill> labBillsB;
    List<Bill> labBillsC;
    List<Bill> labBillsR;

    public List<Bill> getLabBillsWithoutB() {
        if (labBillsB == null) {
            String sql;
            sql = "select f from Bill f where f.retired=false and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and f.institution.id=" + getSessionController().getLoggedUser().getInstitution().getId() + " and type(f) = :bt and f.createdAt between :fromDate and :toDate order by  f.insId";
            Map tm = new HashMap();

            tm.put("fromDate", fromDate);
            tm.put("toDate", toDate);
            tm.put("billType", BillType.OpdBill);
            tm.put("pm1", PaymentMethod.Cash);
            tm.put("pm2", PaymentMethod.Card);
            tm.put("pm3", PaymentMethod.Cheque);
            tm.put("bt", BilledBill.class);
            labBillsB = getBillFacade().findByJpql(sql, tm, TemporalType.TIMESTAMP);
            calTotalsWithout();
        }
        return labBillsB;
    }
    
    public String navigateToReportIncomeWithoutCreditByInstitution(){
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();
        
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToReportIncomeWithoutCreditByInstitution()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/report_income_without_credit_by_institution.xhtml?faces-redirect=true";
        
    }

    public String navigatToReportLabHandOverByDateSummery(){
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();
        
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigatToReportLabHandOverByDateSummery()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportLab/report_lab_hand_over_by_date_summery.xhtml?faces-redirect=true";
        
    }
    
    
    public String navigatToReportCashierDetailedUserByBillType(){
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();
        
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigatToReportCashierDetailedUserByBillType()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportLab/report_cashier_detailed_user_by_billType.xhtml?faces-redirect=true";
        
    }
    public List<Bill> getLabBillsWithoutC() {
        if (labBillsC == null) {
            String sql;
            sql = "select f from Bill f where f.retired=false and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and f.institution.id=" + getSessionController().getLoggedUser().getInstitution().getId() + " and type(f) = :bt and f.createdAt between :fromDate and :toDate order by  f.insId";
            Map tm = new HashMap();

            tm.put("fromDate", fromDate);
            tm.put("toDate", toDate);
            tm.put("billType", BillType.OpdBill);
            tm.put("pm1", PaymentMethod.Cash);
            tm.put("pm2", PaymentMethod.Card);
            tm.put("pm3", PaymentMethod.Cheque);
            tm.put("bt", CancelledBill.class);
            labBillsC = getBillFacade().findByJpql(sql, tm, TemporalType.TIMESTAMP);
            calTotalsWithout();
        }
        return labBillsC;
    }

    public List<Bill> getLabBillsWithoutR() {
        if (labBillsR == null) {
            String sql;
            sql = "select f from Bill f where f.retired=false and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and f.institution.id=" + getSessionController().getLoggedUser().getInstitution().getId() + " and type(f) = :bt and f.createdAt between :fromDate and :toDate order by  f.insId";
            Map tm = new HashMap();

            tm.put("fromDate", fromDate);
            tm.put("toDate", toDate);
            tm.put("billType", BillType.OpdBill);
            tm.put("pm1", PaymentMethod.Cash);
            tm.put("pm2", PaymentMethod.Card);
            tm.put("pm3", PaymentMethod.Cheque);
            tm.put("bt", RefundBill.class);
            labBillsR = getBillFacade().findByJpql(sql, tm, TemporalType.TIMESTAMP);
            calTotalsWithout();
        }
        return labBillsR;
    }

    public double getHosTotB() {
        return hosTotB;
    }

    public void setHosTotB(double hosTotB) {
        this.hosTotB = hosTotB;
    }

    public double getDisTotB() {
        return disTotB;
    }

    public void setDisTotB(double disTotB) {
        this.disTotB = disTotB;
    }

    public double getProfTotB() {
        return profTotB;
    }

    public void setProfTotB(double profTotB) {
        this.profTotB = profTotB;
    }

    public double getNetTotB() {
        return netTotB;
    }

    public void setNetTotB(double netTotB) {
        this.netTotB = netTotB;
    }

    public double getLabHandoverB() {
        return labHandoverB;
    }

    public void setLabHandoverB(double labHandoverB) {
        this.labHandoverB = labHandoverB;
    }

    public double getHosTotC() {
        return hosTotC;
    }

    public void setHosTotC(double hosTotC) {
        this.hosTotC = hosTotC;
    }

    public double getDisTotC() {
        return disTotC;
    }

    public void setDisTotC(double disTotC) {
        this.disTotC = disTotC;
    }

    public double getProfTotC() {
        return profTotC;
    }

    public void setProfTotC(double profTotC) {
        this.profTotC = profTotC;
    }

    public double getNetTotC() {
        return netTotC;
    }

    public void setNetTotC(double netTotC) {
        this.netTotC = netTotC;
    }

    public double getLabHandoverC() {
        return labHandoverC;
    }

    public void setLabHandoverC(double labHandoverC) {
        this.labHandoverC = labHandoverC;
    }

    public double getHosTotR() {
        return hosTotR;
    }

    public void setHosTotR(double hosTotR) {
        this.hosTotR = hosTotR;
    }

    public double getDisTotR() {
        return disTotR;
    }

    public void setDisTotR(double disTotR) {
        this.disTotR = disTotR;
    }

    public double getProfTotR() {
        return profTotR;
    }

    public void setProfTotR(double profTotR) {
        this.profTotR = profTotR;
    }

    public double getNetTotR() {
        return netTotR;
    }

    public void setNetTotR(double netTotR) {
        this.netTotR = netTotR;
    }

    public double getLabHandoverR() {
        return labHandoverR;
    }

    public void setLabHandoverR(double labHandoverR) {
        this.labHandoverR = labHandoverR;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public List<Bill> getLabBillsOwn() {
        if (labBills == null) {
            if (institution == null) {
                return new ArrayList<>();
            }
            String sql = "select f from Bill f "
                    + "where f.retired=false "
                    + "and f.billType = :billType "
                    + "and f.createdAt between :fromDate and :toDate "
                    + " and f.toInstitution=:toIns "
                    + "order by type(f), f.insId";
            Map tm = new HashMap();
            tm.put("fromDate", fromDate);
            tm.put("toDate", toDate);
            tm.put("billType", BillType.OpdBill);
            //  tm.put("ins", getSessionController().getInstitution());
            tm.put("toIns", getInstitution());
            labBills = getBillFacade().findByJpql(sql, tm, TemporalType.TIMESTAMP);
            calTotals();
        }
        return labBills;
    }
    
    public List<Bill> getBills(){
        return bills;
    }

    public List<Bill> getLabBills() {
        if (labBills == null) {
            String sql;
            sql = "select f from Bill f where f.retired=false and f.billType = :billType and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
            Map tm = new HashMap();
            tm.put("fromDate", fromDate);
            tm.put("toDate", toDate);
            tm.put("billType", BillType.OpdBill);
            labBills = getBillFacade().findByJpql(sql, tm, TemporalType.TIMESTAMP);
            calTotals();
        }
        return labBills;
    }

    public void createLabBillsWithoutOwn() {
        String sql;
        Map tm;

        sql = "select f from Bill f where "
                + " f.retired=false "
                + " and f.billType = :billType"
                + " and (f.paymentMethod = :pm1 "
                + " or f.paymentMethod = :pm2 "
                + " or f.paymentMethod = :pm3 "
                + " or f.paymentMethod = :pm4 ) "
                + " and f.institution=:ins "
                + " and f.toInstitution=:toIns "
                + " and f.createdAt  between :fromDate and :toDate"
                + " order by type(f), f.insId";

        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        tm.put("pm4", PaymentMethod.Slip);
        tm.put("ins", getSessionController().getInstitution());
        tm.put("toIns", getInstitution());
        bills = getBillFacade().findByJpql(sql, tm, TemporalType.TIMESTAMP);
        if (bills != null) {
            calTotalsWithout();
        } else {
            clearTotals();
        }

        setString1Value1Table();

    }

    private void setString1Value1Table() {
        string1Value1s = new ArrayList<>();
        String1Value1 row;

        row = new String1Value1();
        row.setString("Gross Fee Total");
        row.setValue(hosTot + profTot);
        string1Value1s.add(row);

        row = new String1Value1();
        row.setString("Discount Total");
        row.setValue(disTot);
        string1Value1s.add(row);

        row = new String1Value1();
        row.setString("Net Fee Total");
        row.setValue(netTot);
        string1Value1s.add(row);

        row = new String1Value1();
        row.setString("Professional Fee Total");
        row.setValue(profTot);
        string1Value1s.add(row);

        row = new String1Value1();
        row.setString("Net Department Income");
        row.setValue(labHandover);
        string1Value1s.add(row);

    }

    public List<Bill> getLabBillsWithoutOwn() {
        if (labBills == null && getInstitution() != null) {
            String sql;
            Map tm;

            sql = "select f from Bill f where f.retired=false and f.billType = :billType and "
                    + "(f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 or f.paymentMethod = :pm4 ) "
                    + "and f.institution=:ins and f.toInstitution=:toIns and f.createdAt between :fromDate and :toDate order by type(f), f.insId";

            tm = new HashMap();
            tm.put("fromDate", fromDate);
            tm.put("toDate", toDate);
            tm.put("billType", BillType.OpdBill);
            tm.put("pm1", PaymentMethod.Cash);
            tm.put("pm2", PaymentMethod.Card);
            tm.put("pm3", PaymentMethod.Cheque);
            tm.put("pm4", PaymentMethod.Slip);
            tm.put("ins", getSessionController().getInstitution());
            tm.put("toIns", getInstitution());
            labBills = getBillFacade().findByJpql(sql, tm, TemporalType.TIMESTAMP);
            if (labBills != null) {
                calTotalsWithout();
            } else {
                clearTotals();
            }
        }
        return labBills;
    }

//    public List<Bill> getLabBillsWithout() {
//        if (labBills == null) {
//            String sql;
//            Map tm;
//            if (getInstitution() == null) {
//                sql = "select f from Bill f where f.retired=false and f.billType = :billType and "
//                        + "(f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 or f.paymentMethod = :pm4) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
//                tm = new HashMap();
//                tm.put("fromDate", fromDate);
//                tm.put("toDate", toDate);
//                tm.put("billType", BillType.OpdBill);
//                tm.put("pm1", PaymentMethod.Cash);
//                tm.put("pm2", PaymentMethod.Card);
//                tm.put("pm3", PaymentMethod.Cheque);
//                tm.put("pm4", PaymentMethod.Slip);
//                labBills = getBillFacade().findByJpql(sql, tm, TemporalType.TIMESTAMP);
//                calTotalsWithout();
//            } else {
//                sql = "select f from Bill f where f.retired=false and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 or f.paymentMethod = :pm4 ) and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
//                tm = new HashMap();
//                tm.put("fromDate", fromDate);
//                tm.put("toDate", toDate);
//                tm.put("billType", BillType.OpdBill);
//                tm.put("pm1", PaymentMethod.Cash);
//                tm.put("pm2", PaymentMethod.Card);
//                tm.put("pm3", PaymentMethod.Cheque);
//                tm.put("pm4", PaymentMethod.Slip);
//                labBills = getBillFacade().findByJpql(sql, tm, TemporalType.TIMESTAMP);
//                calTotalsWithout(getInstitution(), null);
//            }
//        }
//        return labBills;
//    }
    public List<Bill> getLabBillsIns() {
        if (labBills == null) {
            String sql;
            if (getInstitution() == null) {
                sql = "select f from Bill f where f.retired=false and f.billType = :billType and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
                Map tm = new HashMap();
                tm.put("fromDate", fromDate);
                tm.put("toDate", toDate);
                tm.put("billType", BillType.OpdBill);
                labBills = getBillFacade().findByJpql(sql, tm, TemporalType.TIMESTAMP);
                calTotals();
            } else {
                sql = "select f from Bill f where f.retired=false and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
                Map tm = new HashMap();
                tm.put("fromDate", fromDate);
                tm.put("toDate", toDate);
                tm.put("billType", BillType.OpdBill);
                labBills = getBillFacade().findByJpql(sql, tm, TemporalType.TIMESTAMP);
                calTotalsIns();
            }
        }
        return labBills;
    }

    private void calTotalsIns() {
        clearTotals();
        String sql;
        Map tm;
        if (getInstitution() == null) {
            return;
        }

        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm
                .put("billClass", BilledBill.class);
        hosTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();

        tm.put(
                "fromDate", fromDate);
        tm.put(
                "toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", CancelledBill.class);
        hosTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();

        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", RefundBill.class);
        hosTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        hosTot = hosTotB + hosTotC + hosTotR;
        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();

        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", BilledBill.class);
        profTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();

        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", CancelledBill.class);
        profTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();

        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", RefundBill.class);
        profTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        profTot = profTotB + profTotC + profTotR;
        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();

        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", BilledBill.class);
        disTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();

        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", CancelledBill.class);
        disTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();

        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", RefundBill.class);
        disTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        disTot = disTotB + disTotC + disTotR;
//        hosTot = 0;
//        disTot = 0;
//        profTot = 0;
        netTot = hosTot + profTot - disTot;
        netTotC = hosTotC + profTotC - disTotC;
        netTotB = hosTotB + profTotB - disTotB;
        netTotR = hosTotR + profTotR - disTotR;
        labHandover = netTot - profTot;
    }

    public void setLabBills(List<Bill> labBills) {
        this.labBills = labBills;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        recreteModal();
        this.fromDate = fromDate;

    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        recreteModal();
        this.toDate = toDate;

    }

    public String getTxtSearch() {
        return txtSearch;
    }

    public void setTxtSearch(String txtSearch) {
        recreteModal();
        this.txtSearch = txtSearch;
    }
    
    private List<Bill> bills;

    private void recreteModal() {
        patientInvestigations = null;

        hosTot = 0.0;
        hosTotB = 0.0;
        hosTotC = 0.0;
        hosTotR = 0.0;

        disTot = 0.0;
        disTotB = 0.0;
        disTotC = 0.0;
        disTotR = 0.0;

        labBills = null;
        labBillsB = null;
        labBillsC = null;
        labBillsR = null;

        labHandover = 0.0;
        labHandoverB = 0.0;
        labHandoverC = 0.0;
        labHandoverR = 0.0;
    }
    List<PatientInvestigation> patientInvestigations;
    @EJB
    PatientInvestigationFacade piFacade;

    public PatientInvestigationFacade getPiFacade() {
        return piFacade;
    }

    public void setPiFacade(PatientInvestigationFacade piFacade) {
        this.piFacade = piFacade;
    }

    public void createPatientInvestigaationList() {

        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        if (txtSearch == null || txtSearch.trim().equals("")) {
//                sql = "select pi from PatientInvestigation pi join pi.investigation i join pi.billItem.bill b join b.patient.person p where b.createdAt between :fromDate and :toDate order by pi.id desc";
            //               patientInvestigations = getPiFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 100);
            patientInvestigations = new ArrayList<>();
        } else {
            String sql = "select pi from PatientInvestigation pi join pi.investigation i "
                    + " join pi.billItem.bill b join b.patient.person p where ((p.name) "
                    + " like '%" + txtSearch.toUpperCase() + "%' or (b.insId) like '%"
                    + txtSearch.toUpperCase() + "%' or p.phone like '%" + txtSearch + "%' "
                    + " or (i.name) like '%" + txtSearch.toUpperCase() + "%' )  "
                    + " and b.createdAt between :fromDate and :toDate order by pi.id desc";
            patientInvestigations = getPiFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        }
    }

    public List<PatientInvestigation> getPatientInvestigations() {
        return patientInvestigations;
    }

    public void listAllPatientInvestigations() {
        String sql;

        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        sql = "select pi from PatientInvestigation pi join pi.investigation i "
                + "join pi.billItem.bill b join b.patient.person p where b.createdAt"
                + " between :fromDate and :toDate order by pi.id desc";
        ////// // System.out.println("m = " + m);
        ////// // System.out.println("sql = " + sql);
        patientInvestigations = getPiFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void setPatientInvestigations(List<PatientInvestigation> patientInvestigations) {
        this.patientInvestigations = patientInvestigations;
    }

    /**
     * Creates a new instance of LabReportSearchController
     */
    public LabReportSearchByInstitutionController() {
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public List<String1Value1> getString1Value1s() {
        return string1Value1s;
    }

    public void setString1Value1s(List<String1Value1> string1Value1s) {
        this.string1Value1s = string1Value1s;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }
}
