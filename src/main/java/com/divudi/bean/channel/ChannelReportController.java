/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;

import com.divudi.bean.hr.StaffController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.MessageType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.channel.DateEnum;
import com.divudi.data.channel.PaymentEnum;
import com.divudi.data.dataStructure.BillsTotals;
import com.divudi.data.dataStructure.ChannelDoctor;
import com.divudi.data.dataStructure.WebUserBillsTotal;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.data.table.String1Value1;
import com.divudi.data.table.String1Value3;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.SmsManagerEjb;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Sms;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.SmsFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.facade.SessionInstanceFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

@Named
@SessionScoped
public class ChannelReportController implements Serializable {

    @Deprecated
    private ServiceSession serviceSession;
    private SessionInstance sessionInstance;
    private double billedTotalFee;
    private double repayTotalFee;
    private double taxTotal;
    private double total;
    ///////
    private List<BillSession> billSessions;
    private List<BillSession> billSessionsBilled;
    private List<BillSession> billSessionsReturn;
    private List<BillSession> billSessionsCancelled;
    List<BillSession> selectedBillSessions;
    List<ServiceSession> serviceSessions;
    List<ChannelReportColumnModel> channelReportColumnModels;
    double netTotal;
    double cancelTotal;
    double refundTotal;
    double totalBilled;
    double totalCancel;
    double totalRefund;
    double grantTotalBilled;
    double grantTotalCancel;
    double grantTotalRefund;
    double grantNetTotal;
    double doctorFeeTotal;
    double doctorFeeBilledTotal;
    double doctorFeeCancellededTotal;
    double doctorFeeRefundededTotal;
    double hospitalFeeBilledTotal;
    double hospitalFeeCancellededTotal;
    double hospitalFeeRefundededTotal;
    private int channelReportMenuIndex;
    List<String1Value3> valueList;
    ReportKeyWord reportKeyWord;
    Date fromDate;
    Date toDate;
    private Date newSessionDateTime;
    Institution institution;
    WebUser webUser;
    Staff staff;
    ChannelBillTotals billTotals;
    Department department;
    boolean paid = false;
    /////
    private List<ChannelDoctor> channelDoctors;
    List<AgentHistory> agentHistorys;
    List<AgentHistoryWithDate> agentHistoryWithDate;
    List<BookingCountSummryRow> bookingCountSummryRows;
    List<DoctorPaymentSummeryRow> doctorPaymentSummeryRows;
    List<Bill> channelBills;
    List<Bill> channelBillsCancelled;
    List<Bill> channelBillsRefunded;
    
    /////
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    AgentHistoryFacade agentHistoryFacade;
    ///////////
    @EJB
    private ChannelBean channelBean;
    @Inject
    SessionController sessionController;
    @Inject
    StaffController staffController;
    @Inject
    BookingController bookingController;

    @EJB
    DepartmentFacade departmentFacade;
    
    CommonFunctions commonFunctions;
    @EJB
    StaffFacade staffFacade;
    @EJB
    ServiceSessionFacade serviceSessionFacade;
    @EJB
    SmsManagerEjb smsManagerEjb;
    @EJB
    SessionInstanceFacade sessionInstanceFacade;
    
    private List<SessionInstance> sessioninstances;

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public ChannelBillTotals getBillTotals() {
        return billTotals;
    }

    public void setBillTotals(ChannelBillTotals billTotals) {
        this.billTotals = billTotals;
    }

    public double getGrantTotalBilled() {
        return grantTotalBilled;
    }

    public void setGrantTotalBilled(double grantTotalBilled) {
        this.grantTotalBilled = grantTotalBilled;
    }

    public double getGrantTotalCancel() {
        return grantTotalCancel;
    }

    public void setGrantTotalCancel(double grantTotalCancel) {
        this.grantTotalCancel = grantTotalCancel;
    }

    public double getGrantTotalRefund() {
        return grantTotalRefund;
    }

    public void setGrantTotalRefund(double grantTotalRefund) {
        this.grantTotalRefund = grantTotalRefund;
    }

    public double getGrantNetTotal() {
        return grantNetTotal;
    }

    public void setGrantNetTotal(double grantNetTotal) {
        this.grantNetTotal = grantNetTotal;
    }

    public double getDoctorFeeTotal() {
        return doctorFeeTotal;
    }

    public void setDoctorFeeTotal(double doctorFeeTotal) {
        this.doctorFeeTotal = doctorFeeTotal;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public List<DoctorPaymentSummeryRow> getDoctorPaymentSummeryRows() {
        return doctorPaymentSummeryRows;
    }

    public void setDoctorPaymentSummeryRows(List<DoctorPaymentSummeryRow> doctorPaymentSummeryRows) {
        this.doctorPaymentSummeryRows = doctorPaymentSummeryRows;
    }

    public double getDoctorFeeBilledTotal() {
        return doctorFeeBilledTotal;
    }

    public void setDoctorFeeBilledTotal(double doctorFeeBilledTotal) {
        this.doctorFeeBilledTotal = doctorFeeBilledTotal;
    }

    public double getDoctorFeeCancellededTotal() {
        return doctorFeeCancellededTotal;
    }

    public void setDoctorFeeCancellededTotal(double doctorFeeCancellededTotal) {
        this.doctorFeeCancellededTotal = doctorFeeCancellededTotal;
    }

    public double getDoctorFeeRefundededTotal() {
        return doctorFeeRefundededTotal;
    }

    public void setDoctorFeeRefundededTotal(double doctorFeeRefundededTotal) {
        this.doctorFeeRefundededTotal = doctorFeeRefundededTotal;
    }

    public double getHospitalFeeBilledTotal() {
        return hospitalFeeBilledTotal;
    }

    public void setHospitalFeeBilledTotal(double hospitalFeeBilledTotal) {
        this.hospitalFeeBilledTotal = hospitalFeeBilledTotal;
    }

    public double getHospitalFeeCancellededTotal() {
        return hospitalFeeCancellededTotal;
    }

    public void setHospitalFeeCancellededTotal(double hospitalFeeCancellededTotal) {
        this.hospitalFeeCancellededTotal = hospitalFeeCancellededTotal;
    }

    public double getHospitalFeeRefundededTotal() {
        return hospitalFeeRefundededTotal;
    }

    public void setHospitalFeeRefundededTotal(double hospitalFeeRefundededTotal) {
        this.hospitalFeeRefundededTotal = hospitalFeeRefundededTotal;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    ChannelReportColumnModelBundle rowBundle;
    List<ChannelReportColumnModel> rows;
    List<ChannelReportColumnModel> filteredRows;

    public ChannelReportColumnModelBundle getRowBundle() {
        return rowBundle;
    }

    public void setRowBundle(ChannelReportColumnModelBundle rowBundle) {
        this.rowBundle = rowBundle;
    }

    public List<ChannelReportColumnModel> getFilteredRows() {
        return filteredRows;
    }

    public void setFilteredRows(List<ChannelReportColumnModel> filteredRows) {
        this.filteredRows = filteredRows;
    }

    public List<ChannelReportColumnModel> getRows() {
        return rows;
    }

    public void setRows(List<ChannelReportColumnModel> rows) {
        this.rows = rows;
    }

    public List<BillSession> getSelectedBillSessions() {
        return selectedBillSessions;
    }

    public void setSelectedBillSessions(List<BillSession> selectedBillSessions) {
        this.selectedBillSessions = selectedBillSessions;
    }

    public List<ServiceSession> getServiceSessions() {
        return serviceSessions;
    }

    public void setServiceSessions(List<ServiceSession> serviceSessions) {
        this.serviceSessions = serviceSessions;
    }
    
    public void fillSessionsForChannelDoctorCard(){
        String sql;
        Map m = new HashMap();

        sql = " select s from SessionInstance s "
                + " where s.retired=false "
                + " and s.sessionDate between :fromDate and :toDate ";
        
        m.put("fromDate", fromDate);
        m.put("toDate", toDate);
        sessioninstances=sessionInstanceFacade.findByJpql(sql,m);
    }


    public void fillIncomeWithAgentBookings() {
        String j;
        Map m = new HashMap();

//        Bill b = new Bill();
//        b.setNetTotal(netTotal);
//        
        rows = new ArrayList<>();

        ChannelReportColumnModel br;
        ChannelReportColumnModel cr;
        ChannelReportColumnModel rr;
        ChannelReportColumnModel nr;

        j = "select sum(b.netTotal)"
                + " from Bill b "
                + " where b.retired=false "
                + " and b.createdAt between :fd and :td "
                + " and b.billType in :bts "
                + " and type(b) = :bt "
                + " and b.paymentMethod =:pm ";

        if (institution != null) {
            m.put("ins", institution);
            j += " and b.institution=:ins ";
        }

        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.ChannelAgent);
        bts.add(BillType.ChannelCash);
        bts.add(BillType.ChannelPaid);

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bts", bts);

        ////System.out.println("j = " + j);
        //Bookings
        br = new ChannelReportColumnModel();
        m.put("bt", BilledBill.class);
        br.setName("Bookings");

        m.put("pm", PaymentMethod.Cash);
        br.setCashTotal(billFacade.findDoubleByJpql(j, m, TemporalType.TIMESTAMP));

        m.put("pm", PaymentMethod.Card);
        br.setCardTotal(billFacade.findDoubleByJpql(j, m, TemporalType.TIMESTAMP));

        m.put("pm", PaymentMethod.Cheque);
        br.setChequeTotal(billFacade.findDoubleByJpql(j, m, TemporalType.TIMESTAMP));

        m.put("pm", PaymentMethod.Slip);
        br.setSlipTotal(billFacade.findDoubleByJpql(j, m, TemporalType.TIMESTAMP));

        br.setCollectionTotal(br.cashTotal + br.cardTotal + br.chequeTotal);

        m.put("pm", PaymentMethod.Agent);
        br.setAgentTotal(billFacade.findDoubleByJpql(j, m, TemporalType.TIMESTAMP));

        br.setValue(br.getAgentTotal() + br.getCollectionTotal());

        //Cancellations
        cr = new ChannelReportColumnModel();
        m.put("bt", CancelledBill.class);
        cr.setName("Cancellations");

        m.put("pm", PaymentMethod.Cash);
        cr.setCashTotal(billFacade.findDoubleByJpql(j, m, TemporalType.TIMESTAMP));

        m.put("pm", PaymentMethod.Card);
        cr.setCardTotal(billFacade.findDoubleByJpql(j, m, TemporalType.TIMESTAMP));

        m.put("pm", PaymentMethod.Cheque);
        cr.setChequeTotal(billFacade.findDoubleByJpql(j, m, TemporalType.TIMESTAMP));

        m.put("pm", PaymentMethod.Slip);
        cr.setSlipTotal(billFacade.findDoubleByJpql(j, m, TemporalType.TIMESTAMP));

        cr.setCollectionTotal(cr.cashTotal + cr.cardTotal + cr.chequeTotal);

        m.put("pm", PaymentMethod.Agent);
        cr.setAgentTotal(billFacade.findDoubleByJpql(j, m, TemporalType.TIMESTAMP));

        cr.setValue(cr.getAgentTotal() + cr.getCollectionTotal());

        //Refunds
        rr = new ChannelReportColumnModel();
        m.put("bt", RefundBill.class);
        rr.setName("Refunds");

        m.put("pm", PaymentMethod.Cash);
        rr.setCashTotal(billFacade.findDoubleByJpql(j, m, TemporalType.TIMESTAMP));

        m.put("pm", PaymentMethod.Card);
        rr.setCardTotal(billFacade.findDoubleByJpql(j, m, TemporalType.TIMESTAMP));

        m.put("pm", PaymentMethod.Cheque);
        rr.setChequeTotal(billFacade.findDoubleByJpql(j, m, TemporalType.TIMESTAMP));

        m.put("pm", PaymentMethod.Slip);
        rr.setSlipTotal(billFacade.findDoubleByJpql(j, m, TemporalType.TIMESTAMP));

        rr.setCollectionTotal(rr.cashTotal + rr.cardTotal + rr.chequeTotal);

        m.put("pm", PaymentMethod.Agent);
        rr.setAgentTotal(billFacade.findDoubleByJpql(j, m, TemporalType.TIMESTAMP));

        rr.setValue(rr.getAgentTotal() + rr.getCollectionTotal());

        nr = new ChannelReportColumnModel();
        nr.name = "Total";
        nr.cashTotal = br.cashTotal + rr.cashTotal + cr.cardTotal;
        nr.cardTotal = br.cardTotal + rr.cardTotal + cr.cardTotal;
        nr.chequeTotal = br.chequeTotal + rr.chequeTotal + cr.chequeTotal;
        nr.slipTotal = br.slipTotal + rr.slipTotal + cr.slipTotal;
        nr.collectionTotal = br.collectionTotal + rr.collectionTotal + cr.collectionTotal;
        nr.agentTotal = br.agentTotal + rr.agentTotal + cr.agentTotal;
        nr.value = br.value + rr.value + cr.value;

        rows.add(br);
        rows.add(cr);
        rows.add(rr);

        rowBundle = new ChannelReportColumnModelBundle();
        rowBundle.setBundle(nr);
        rowBundle.setRows(rows);

    }

    public List<BillSession> createBillSessionQuery(Bill bill, PaymentEnum paymentEnum, DateEnum dateEnum, ReportKeyWord reportKeyWord) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        HashMap hm = new HashMap();

        String sql = "SELECT b FROM BillSession b "
                + "  where type(b.bill)=:class "
                + " and b.bill.billType in :bt "
                + " and b.bill.retired=false ";

        if (reportKeyWord.getSpeciality() != null) {
            sql += " and b.staff.speciality=:sp";
            hm.put("sp", reportKeyWord.getSpeciality());
        }

        if (reportKeyWord.getStaff() != null) {
            sql += "and b.staff=:stf";
            hm.put("stf", reportKeyWord.getStaff());
        }

        if (reportKeyWord.getPatient() != null) {
            sql += "and b.bill.patient=:pt";
            hm.put("pt", reportKeyWord.getPatient());
        }

        if (reportKeyWord.getInstitution() != null) {
            sql += "and b.bill.fromInstitution=:ins";
            hm.put("ins", reportKeyWord.getInstitution());
        }

        if (reportKeyWord.getPaymentMethod() != null) {
            sql += "and b.bill.paymentMethod=:pm";
            hm.put("pm", reportKeyWord.getPaymentMethod());
        }

        if (reportKeyWord.getItem() != null) {
            sql += "and b.serviceSession=:sv";
            hm.put("sv", reportKeyWord.getItem());
        }

        switch (paymentEnum) {
            case Paid:
                sql += " and b.bill.paidAmount!=0";
                break;
            case NotPaid:
                sql += " and b.bill.paidAmount=0";
                break;
            case All:
                break;
        }

        switch (dateEnum) {
            case AppointmentDate:
                sql += " and b.bill.appointmentAt between :frm and  :to";
                break;
            case PaidDate:
                sql += " and b.bill.paidAt between :frm and  :to";
                break;
            case CreatedDate:
                sql += " and b.bill.createdAt between :frm and  :to";
                break;

        }
        hm.put("bt", bts);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("class", bill.getClass());

        return billSessionFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Object[]> createBillSessionQueryAgregation(Bill bill, PaymentEnum paymentEnum, DateEnum dateEnum, ReportKeyWord reportKeyWord) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        HashMap hm = new HashMap();

        String sql = "SELECT sum(b.billItem.qty),b.bill.billType "
                + " FROM BillSession b "
                + "  where type(b.bill)=:class "
                + " and b.bill.billType in :bt "
                + " and b.bill.retired=false ";

        if (reportKeyWord.getSpeciality() != null) {
            sql += " and b.staff.speciality=:sp ";
            hm.put("sp", reportKeyWord.getSpeciality());
        }

        if (reportKeyWord.getStaff() != null) {
            sql += " and b.staff=:stf ";
            hm.put("stf", reportKeyWord.getStaff());
        }

        if (reportKeyWord.getPatient() != null) {
            sql += " and b.bill.patient=:pt ";
            hm.put("pt", reportKeyWord.getPatient());
        }

        if (reportKeyWord.getInstitution() != null) {
            sql += " and b.bill.fromInstitution=:ins ";
            hm.put("ins", reportKeyWord.getInstitution());
        }

        if (reportKeyWord.getPaymentMethod() != null) {
            sql += " and b.bill.paymentMethod=:pm ";
            hm.put("pm", reportKeyWord.getPaymentMethod());
        }

        if (reportKeyWord.getItem() != null) {
            sql += " and b.serviceSession=:sv ";
            hm.put("sv", reportKeyWord.getItem());
        }

        switch (paymentEnum) {
            case Paid:
                sql += " and b.bill.paidAmount!=0";
                break;
            case NotPaid:
                sql += " and b.bill.paidAmount=0";
                break;
            case All:
                break;
        }

        switch (dateEnum) {
            case AppointmentDate:
                sql += " and b.bill.appointmentAt between :frm and  :to";
                break;
            case PaidDate:
                sql += " and b.bill.paidAt between :frm and  :to";
                break;
            case CreatedDate:
                sql += " and b.bill.createdAt between :frm and  :to";
                break;

        }

        sql += " group by b.bill.billType "
                + " order by b.bill.billType ";

        hm.put("bt", bts);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("class", bill.getClass());

        return billSessionFacade.findAggregates(sql, hm, TemporalType.TIMESTAMP);
    }

    public void createChannelCashierSummeryTable() {
        channelReportColumnModels = new ArrayList<>();

        FeeType ft[] = {FeeType.OtherInstitution, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> fts = Arrays.asList(ft);
        BillType bty[] = {BillType.ChannelCash, BillType.ChannelStaff};
        List<BillType> btys = Arrays.asList(bty);
        PaymentMethod pm[] = {PaymentMethod.Cash, PaymentMethod.Staff};
        List<PaymentMethod> pms = Arrays.asList(pm);

//        if (webUser != null) {
//            doctorFeeTotal = calDoctorFeeNetTotal(pms, btys, FeeType.Staff, webUser);
//        }
        doctorFeeTotal = calDoctorFeeNetTotal(pms, btys, FeeType.Staff);

        for (PaymentMethod p : pm) {
            ChannelReportColumnModel cm = new ChannelReportColumnModel();
            switch (p) {
                case Cash:
                    getChannelCashierSumTotals(PaymentMethod.Cash, BillType.ChannelCash, cm, channelReportColumnModels);
                    break;
                case Staff:
                    getChannelCashierSumTotals(PaymentMethod.Staff, BillType.ChannelStaff, cm, channelReportColumnModels);
                    break;
            }
        }

        grantTotalBilled = 0;
        grantTotalCancel = 0;
        grantTotalRefund = 0;
        grantNetTotal = 0;

        for (ChannelReportColumnModel chm : channelReportColumnModels) {
            grantTotalBilled += chm.getBilledTotal();
            grantTotalCancel += chm.getCancellTotal();
            grantTotalRefund += chm.getRefundTotal();
            grantNetTotal += chm.getTotal();
        }

    }

    public void getChannelCashierSumTotals(PaymentMethod pay, BillType bty, ChannelReportColumnModel chm, List<ChannelReportColumnModel> chmlst) {
//        if (webUser != null) {
//            totalBilled = calCashierNetTotal(new BilledBill(), pay, bty, webUser);
//            totalCancel = calCashierNetTotal(new CancelledBill(), pay, bty, webUser);
//            totalRefund = calCashierNetTotal(new RefundBill(), pay, bty, webUser);
//        }

        totalBilled = calCashierNetTotal(new BilledBill(), pay, bty);
        totalCancel = calCashierNetTotal(new CancelledBill(), pay, bty);
        totalRefund = calCashierNetTotal(new RefundBill(), pay, bty);

        ////System.out.println("Billed,Cancell,Refund" + totalBilled + "," + totalCancel + "," + totalRefund);
        if (pay == PaymentMethod.Cash) {
            ////System.out.println("payment method=" + pay);
            ////System.out.println("Billed,Cancell,Refund" + totalBilled + "," + totalCancel + "," + totalRefund);
            totalBilled += calCashierNetTotal(new BilledBill(), pay, BillType.ChannelPaid);
            totalCancel += calCashierNetTotal(new CancelledBill(), pay, BillType.ChannelPaid);
            totalRefund += calCashierNetTotal(new RefundBill(), pay, BillType.ChannelPaid);
            ////System.out.println("netTotal" + netTotal);
        }
        netTotal = totalBilled + totalCancel + totalRefund;
        ////System.out.println("netTotal = " + netTotal);

        chm.setPaymentMethod(pay);
        chm.setBilledTotal(totalBilled);
        chm.setCancellTotal(totalCancel);
        chm.setRefundTotal(totalRefund);

        chm.setTotal(netTotal);

        ////System.out.println("chmlst = " + chmlst);
        chmlst.add(chm);
    }

    public double calCashierNetTotal(Bill bill, PaymentMethod paymentMethod, BillType billType) {
        HashMap hm = new HashMap();

        String sql = " SELECT sum(b.netTotal) from Bill b "
                + " where type(b)=:class "
                + " and b.retired=false "
                + " and b.billType =:bt "
                + " and b.paymentMethod=:pm "
                + " and b.createdAt between :frm and :to ";

        if (webUser != null) {
            sql += " and b.creater=:wb ";
            hm.put("wb", webUser);
        }
        hm.put("class", bill.getClass());
        hm.put("bt", billType);
        hm.put("pm", paymentMethod);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());

        return billFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

//    public double calCashierNetTotal(Bill bill, PaymentMethod paymentMethod, BillType billType, WebUser webUser) {
//        HashMap hm = new HashMap();
//
//        String sql = " SELECT sum(b.netTotal) from Bill b "
//                + " where type(b)=:class "
//                + " and b.retired=false "
//                + " and b.billType =:bt "
//                + " and b.paymentMethod=:pm "
//                + " and b.creater=:wu "
//                + " and b.createdAt between :frm and :to ";
//
//        hm.put("class", bill.getClass());
//        hm.put("bt", billType);
//        hm.put("pm", paymentMethod);
//        hm.put("wu", webUser);
//        hm.put("frm", getFromDate());
//        hm.put("to", getToDate());
//
//        return billFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);
//
//    }
    public double calDoctorFeeNetTotal(List<PaymentMethod> paymentMethod, List<BillType> billType, FeeType ftp) {
        HashMap hm = new HashMap();

        String sql = " SELECT sum(b.feeValue) from BillFee b "
                //+ " where type(b)=:class "
                + " where b.bill.retired=false "
                + " and b.bill.cancelled=false "
                + " and b.bill.refunded=false "
                + " and b.bill.billType in :bt "
                + " and b.bill.paymentMethod in :pm "
                + " and b.fee.feeType=:ft"
                + " and b.bill.createdAt between :frm and :to ";

        //hm.put("class", bill.getClass());
        if (webUser != null) {
            sql += " and b.bill.creater=:wb";
            hm.put("wb", webUser);
        }

        hm.put("bt", billType);
        hm.put("pm", paymentMethod);
        hm.put("ft", ftp);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());

        return billFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

//    public double calDoctorFeeNetTotal(List<PaymentMethod> paymentMethod, List<BillType> billType, FeeType ftp, WebUser webUser) {
//        HashMap hm = new HashMap();
//
//        String sql = " SELECT sum(b.feeValue) from BillFee b "
//                //+ " where type(b)=:class "
//                + " where b.bill.retired=false "
//                + " and b.bill.billType in :bt "
//                + " and b.bill.paymentMethod in :pm "
//                + " and b.fee.feeType=:ft "
//                + " and b.bill.creater=:wu "
//                + " and b.bill.createdAt between :frm and :to ";
//
//        //hm.put("class", bill.getClass());
//        hm.put("bt", billType);
//        hm.put("pm", paymentMethod);
//        hm.put("ft", ftp);
//        hm.put("wu", webUser);
//        hm.put("frm", getFromDate());
//        hm.put("to", getToDate());
//
//        return billFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);
//
//    }
    public double createBillSessionQueryTotal(Bill bill, PaymentEnum paymentEnum, DateEnum dateEnum, ReportKeyWord reportKeyWord) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        HashMap hm = new HashMap();

        String sql = "SELECT sum(b.bill.netTotal) FROM BillSession b "
                + "  where type(b.bill)=:class "
                + " and b.bill.billType in :bt "
                + " and b.bill.retired=false ";

        if (reportKeyWord.getSpeciality() != null) {
            sql += " and b.staff.speciality=:sp";
            hm.put("sp", reportKeyWord.getSpeciality());
        }

        if (reportKeyWord.getStaff() != null) {
            sql += "and b.staff=:stf";
            hm.put("stf", reportKeyWord.getStaff());
        }

        if (reportKeyWord.getPatient() != null) {
            sql += "and b.bill.patient=:pt";
            hm.put("pt", reportKeyWord.getPatient());
        }

        if (reportKeyWord.getInstitution() != null) {
            sql += "and b.bill.fromInstitution=:ins";
            hm.put("ins", reportKeyWord.getInstitution());
        }

        if (reportKeyWord.getPaymentMethod() != null) {
            sql += "and b.bill.paymentMethod=:pm";
            hm.put("pm", reportKeyWord.getPaymentMethod());
        }

        if (reportKeyWord.getItem() != null) {
            sql += "and b.serviceSession=:sv";
            hm.put("sv", reportKeyWord.getItem());
        }

        switch (paymentEnum) {
            case Paid:
                sql += " and b.bill.paidAmount!=0";
                break;
            case NotPaid:
                sql += " and b.bill.paidAmount=0";
                break;
            case All:
                break;
        }

        switch (dateEnum) {
            case AppointmentDate:
                sql += " and b.bill.appointmentAt between :frm and  :to";
                break;
            case PaidDate:
                sql += " and b.bill.paidAt between :frm and  :to";
                break;
            case CreatedDate:
                sql += " and b.bill.createdAt between :frm and  :to";
                break;

        }
        hm.put("bt", bts);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("class", bill.getClass());

        return billSessionFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public void createBillSession_report_1() {
        billSessionsBilled = createBillSessionQuery(new BilledBill(), PaymentEnum.Paid, DateEnum.AppointmentDate, getReportKeyWord());
        billSessionsCancelled = createBillSessionQuery(new CancelledBill(), PaymentEnum.Paid, DateEnum.CreatedDate, getReportKeyWord());
        billSessionsReturn = createBillSessionQuery(new RefundBill(), PaymentEnum.Paid, DateEnum.CreatedDate, getReportKeyWord());

        netTotal = createBillSessionQueryTotal(new BilledBill(), PaymentEnum.Paid, DateEnum.AppointmentDate, getReportKeyWord());
        cancelTotal = createBillSessionQueryTotal(new CancelledBill(), PaymentEnum.Paid, DateEnum.CreatedDate, getReportKeyWord());
        refundTotal = createBillSessionQueryTotal(new RefundBill(), PaymentEnum.Paid, DateEnum.CreatedDate, getReportKeyWord());

        valueList = new ArrayList<>();

        List<Object[]> billedAgg = createBillSessionQueryAgregation(new BilledBill(), PaymentEnum.Paid, DateEnum.AppointmentDate, getReportKeyWord());
        List<Object[]> cancelledAgg = createBillSessionQueryAgregation(new CancelledBill(), PaymentEnum.Paid, DateEnum.CreatedDate, getReportKeyWord());
        List<Object[]> returnedAgg = createBillSessionQueryAgregation(new RefundBill(), PaymentEnum.Paid, DateEnum.CreatedDate, getReportKeyWord());

        String1Value3 channelAgent = new String1Value3();
        String1Value3 channelCall = new String1Value3();
        String1Value3 channelCash = new String1Value3();
        String1Value3 channelStaff = new String1Value3();

        //SETTING BILLED COUNT
        for (Object[] obj : billedAgg) {
            Double dbl = (Double) obj[0];
            BillType btp = (BillType) obj[1];

            switch (btp) {
                case ChannelAgent:
                    channelAgent.setString(btp.getLabel());
                    channelAgent.setValue1(dbl);
                    break;
                case ChannelCash:
                    channelCash.setString(btp.getLabel());
                    channelCash.setValue1(dbl);
                    break;
                case ChannelOnCall:
                    channelCall.setString(btp.getLabel());
                    channelCall.setValue1(dbl);
                    break;
                case ChannelStaff:
                    channelStaff.setString(btp.getLabel());
                    channelStaff.setValue1(dbl);
                    break;
            }
        }

        //SETTING CANCELLED COUNT
        for (Object[] obj : cancelledAgg) {
            Double dbl = (Double) obj[0];
            BillType btp = (BillType) obj[1];

            switch (btp) {
                case ChannelAgent:
                    channelAgent.setString(btp.getLabel());
                    channelAgent.setValue2(dbl);
                    break;
                case ChannelCash:
                    channelCash.setString(btp.getLabel());
                    channelCash.setValue2(dbl);
                    break;
                case ChannelOnCall:
                    channelCall.setString(btp.getLabel());
                    channelCall.setValue2(dbl);
                    break;
                case ChannelStaff:
                    channelStaff.setString(btp.getLabel());
                    channelStaff.setValue2(dbl);
                    break;
            }
        }

        //SETTING REFUND COUNT
        for (Object[] obj : returnedAgg) {
            Double dbl = (Double) obj[0];
            BillType btp = (BillType) obj[1];

            switch (btp) {
                case ChannelAgent:
                    channelAgent.setString(btp.getLabel());
                    channelAgent.setValue3(dbl);
                    break;
                case ChannelCash:
                    channelCash.setString(btp.getLabel());
                    channelCash.setValue3(dbl);
                    break;
                case ChannelOnCall:
                    channelCall.setString(btp.getLabel());
                    channelCall.setValue3(dbl);
                    break;
                case ChannelStaff:
                    channelStaff.setString(btp.getLabel());
                    channelStaff.setValue3(dbl);
                    break;

            }
        }

        getValueList().add(channelAgent);
        getValueList().add(channelCall);
        getValueList().add(channelCash);
        getValueList().add(channelStaff);

        totalBilled = 0.0;
        totalCancel = 0.0;
        totalRefund = 0.0;

        for (String1Value3 ls : getValueList()) {
            totalBilled += ls.getValue1();
            totalCancel += ls.getValue2();
            totalRefund += ls.getValue3();
        }

    }

    public void updateChannel() {
        updateChannelCancelBillFee(new CancelledBill());
        updateChannelCancelBillFee(new RefundBill());
    }

    public void updateChannelCancelBillFee(Bill b) {
        String sql;
        Map m = new HashMap();
        BillType[] bts = {BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelStaff,};
        List<BillType> bt = Arrays.asList(bts);
        sql = " select bs from BillSession bs where "
                + " bs.bill.billType in :bt "
                + " and type(bs.bill)=:cla "
                + " and bs.bill.singleBillSession is null ";

        m.put("cla", b.getClass());
        m.put("bt", bt);
        ////System.out.println("getBillSessionFacade().findByJpql(sql, m) = " + getBillSessionFacade().findByJpql(sql, m));
        List<BillSession> billSessions = getBillSessionFacade().findByJpql(sql, m);
        ////System.out.println("billSessions = " + billSessions.size());
        for (BillSession bs : billSessions) {
            ////System.out.println("In");
            bs.getBill().setSingleBillSession(bs);
            ////System.out.println("bs.getSingleBillSession() = " + bs.getBill().getSingleBillSession());
            getBillFacade().edit(bs.getBill());
            ////System.out.println("Out");
        }
    }

    List<Bill> billedBills;
    List<Bill> cancelBills;
    List<Bill> refundBills;

    public void channelBillClassList() {

        billedBills = new ArrayList<>();
        cancelBills = new ArrayList<>();
        refundBills = new ArrayList<>();

        billedBills = channelListByBillClass(new BilledBill());
        cancelBills = channelListByBillClass(new CancelledBill());
        refundBills = channelListByBillClass(new RefundBill());
    }

    public List<Bill> channelListByBillClass(Bill bill) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        HashMap hm = new HashMap();

        String sql = " select b from Bill b "
                + " where b.billType in :bt "
                + " and b.retired=false "
                + " and type(b)=:class "
                + " and b.createdAt between :fDate and :tDate "
                + " order by b.singleBillSession.sessionDate ";

        hm.put("bt", bts);
        hm.put("class", bill.getClass());
        hm.put("fDate", getFromDate());
        hm.put("tDate", getToDate());

        return billFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    List<Department> deps;
    List<DepartmentBill> depBills;

    double departmentBilledBillTotal;
    double departmentCanceledBillTotal;
    double departmentRefundBillTotal;

    public void createDepartmentBills() {
        deps = getDepartments();
        depBills = new ArrayList<>();
        for (Department d : deps) {

            DepartmentBill db = new DepartmentBill();
            db.setBillDepartment(d);
            db.setBills(getDepartmentBills(d));
            if (db.getBills() != null && !db.getBills().isEmpty()) {
                db.setDepartmentBillTotal(calTotal(db.getBills()));
                depBills.add(db);

            }
        }

    }

    public double calTotal(List<Bill> bills) {

        double departmentTotal = 0.0;
        for (Bill bill : bills) {
            departmentTotal += bill.getNetTotal();
        }
        return departmentTotal;
    }

//     public void createDepartmentBills() {
//        deps = getDepartments();
//        depBills = new ArrayList<>();
//        for (Department d : deps) {
//
//            List<Object[]> depList = getDepartmentBills(d);
//            if (depList == null) {
//                continue;
//            }
//
//            DepartmentBill db = new DepartmentBill();
//            db.setBillDepartment(d);
//            for (Object[] obj : depList) {
//                List<Bill> bills = new ArrayList<>();
//                if (obj[0] != null) {
//                    bills = (List<Bill>) obj[0];
//                    db.setBills(bills);
//                }
//                if (obj[1] != null) {
//                    db.setDepartmentBillTotal((double) obj[1]);
//                }
//
//            }
//            if (db.getBills() != null && !db.getBills().isEmpty()) {
//                depBills.add(db);
//
//            }
//        }
//
//    }
    public double getDepartmentBilledBillTotal() {
        return departmentBilledBillTotal;
    }

    public void setDepartmentBilledBillTotal(double departmentBilledBillTotal) {
        this.departmentBilledBillTotal = departmentBilledBillTotal;
    }

    public double getDepartmentCanceledBillTotal() {
        return departmentCanceledBillTotal;
    }

    public void setDepartmentCanceledBillTotal(double departmentCanceledBillTotal) {
        this.departmentCanceledBillTotal = departmentCanceledBillTotal;
    }

    public double getDepartmentRefundBillTotal() {
        return departmentRefundBillTotal;
    }

    public void setDepartmentRefundBillTotal(double departmentRefundBillTotal) {
        this.departmentRefundBillTotal = departmentRefundBillTotal;
    }

    public List<Bill> getChannelBillsCancelled() {
        return channelBillsCancelled;
    }

    public void setChannelBillsCancelled(List<Bill> channelBillsCancelled) {
        this.channelBillsCancelled = channelBillsCancelled;
    }

    public List<Bill> getChannelBillsRefunded() {
        return channelBillsRefunded;
    }

    public void setChannelBillsRefunded(List<Bill> channelBillsRefunded) {
        this.channelBillsRefunded = channelBillsRefunded;
    }

    public List<Department> getDepartments() {
        String sql;
        HashMap hm = new HashMap();
        sql = " select d from Department d "
                + " where d.retired=false "
                + " order by d.name";
        return getDepartmentFacade().findByJpql(sql, hm);
    }

    public List<Bill> getDepartmentBills(Department dep) {

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        HashMap hm = new HashMap();
        String sql = " select b from Bill b "
                + " where b.billType in :bt "
                + " and b.retired=false "
                + " and b.department=:dep "
                + " and b.createdAt between :fDate and :tDate "
                + " order by b.singleBillSession.sessionDate ";

        hm.put("bt", bts);
        hm.put("dep", dep);
        hm.put("fDate", getFromDate());
        hm.put("tDate", getToDate());
        return billFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    BillsTotals billedBillList;
    BillsTotals canceledBillList;
    BillsTotals refundBillList;

    public void createChannelCashierBillList() {

        getBilledBillList().setBills(createUserChannelBills(new BilledBill(), getWebUser(), getDepartment()));
        getCanceledBillList().setBills(createUserChannelBills(new CancelledBill(), getWebUser(), getDepartment()));
        getRefundBillList().setBills(createUserChannelBills(new RefundBill(), getWebUser(), getDepartment()));

        getBilledBillList().setAgent(calChannelTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Agent));
        getCanceledBillList().setAgent(calChannelTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Agent));
        getRefundBillList().setAgent(calChannelTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Agent));

        getBilledBillList().setCash(calChannelTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Cash));
        getCanceledBillList().setCash(calChannelTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Cash));
        getRefundBillList().setCash(calChannelTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Cash));

        getBilledBillList().setCard(calChannelTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Card));
        getCanceledBillList().setCard(calChannelTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Card));
        getRefundBillList().setCard(calChannelTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Card));

        getBilledBillList().setSlip(calChannelTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Slip));
        getCanceledBillList().setSlip(calChannelTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Slip));
        getRefundBillList().setSlip(calChannelTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Slip));

        getBilledBillList().setCheque(calChannelTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Cheque));
        getCanceledBillList().setCheque(calChannelTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Cheque));
        getRefundBillList().setCheque(calChannelTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Cheque));

        createSummary();
    }
    
    public void createCashierBillList() {

        getBilledBillList().setBills(createUserCashierBills(new BilledBill(), getWebUser(), getDepartment()));
        getCanceledBillList().setBills(createUserCashierBills(new CancelledBill(), getWebUser(), getDepartment()));
        getRefundBillList().setBills(createUserCashierBills(new RefundBill(), getWebUser(), getDepartment()));

        getBilledBillList().setAgent(calCashierTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Agent));
        getCanceledBillList().setAgent(calCashierTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Agent));
        getRefundBillList().setAgent(calCashierTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Agent));

        getBilledBillList().setCash(calCashierTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Cash));
        getCanceledBillList().setCash(calCashierTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Cash));
        getRefundBillList().setCash(calCashierTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Cash));

        getBilledBillList().setCard(calCashierTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Card));
        getCanceledBillList().setCard(calCashierTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Card));
        getRefundBillList().setCard(calCashierTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Card));

        getBilledBillList().setSlip(calCashierTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Slip));
        getCanceledBillList().setSlip(calCashierTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Slip));
        getRefundBillList().setSlip(calCashierTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Slip));

        getBilledBillList().setCheque(calCashierTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Cheque));
        getCanceledBillList().setCheque(calCashierTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Cheque));
        getRefundBillList().setCheque(calCashierTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Cheque));

        createSummary();
    }
    
    public void createAllBillList() {

        getBilledBillList().setBills(createUserAllBills(new BilledBill(), getWebUser(), getDepartment()));
        getCanceledBillList().setBills(createUserAllBills(new CancelledBill(), getWebUser(), getDepartment()));
        getRefundBillList().setBills(createUserAllBills(new RefundBill(), getWebUser(), getDepartment()));

        getBilledBillList().setAgent(calAllTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Agent));
        getCanceledBillList().setAgent(calAllTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Agent));
        getRefundBillList().setAgent(calAllTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Agent));

        getBilledBillList().setCash(calAllTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Cash));
        getCanceledBillList().setCash(calAllTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Cash));
        getRefundBillList().setCash(calAllTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Cash));

        getBilledBillList().setCard(calAllTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Card));
        getCanceledBillList().setCard(calAllTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Card));
        getRefundBillList().setCard(calAllTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Card));

        getBilledBillList().setSlip(calAllTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Slip));
        getCanceledBillList().setSlip(calAllTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Slip));
        getRefundBillList().setSlip(calAllTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Slip));

        getBilledBillList().setCheque(calAllTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Cheque));
        getCanceledBillList().setCheque(calAllTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Cheque));
        getRefundBillList().setCheque(calAllTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Cheque));

        createSummary();
    }

    private List<String1Value1> channelSummary;

    public void createSummary() {
        List<BillsTotals> list2 = new ArrayList<>();
        list2.add(billedBillList);
        list2.add(canceledBillList);
        list2.add(refundBillList);

        double agent = 0.0;
        double slip = 0;
        double creditCard = 0.0;
        double cheque = 0.0;
        double cash = 0.0;
        for (BillsTotals bt : list2) {
            if (bt != null) {
                agent += bt.getAgent();
                slip += bt.getSlip();
                creditCard += bt.getCard();
                cheque += bt.getCheque();
                cash += bt.getCash();
            }
        }

        channelSummary = new ArrayList<>();
        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Agent Total");
        tmp1.setValue(agent);
        channelSummary.add(tmp1);

        String1Value1 tmp2 = new String1Value1();
        tmp2.setString("Final Slip Total");
        tmp2.setValue(slip);
        channelSummary.add(tmp2);

        String1Value1 tmp3 = new String1Value1();
        tmp3.setString("Final Cash Total");
        tmp3.setValue(cash);
        channelSummary.add(tmp3);

        String1Value1 tmp4 = new String1Value1();
        tmp4.setString("Final Card Total");
        tmp4.setValue(creditCard);
        channelSummary.add(tmp4);

        String1Value1 tmp5 = new String1Value1();
        tmp5.setString("Final Cheque Total");
        tmp5.setValue(cheque);
        channelSummary.add(tmp5);

        String1Value1 tmp6 = new String1Value1();
        tmp6.setString("Final Summary Total");
        tmp6.setValue(slip + cheque + agent + cash + creditCard);
        channelSummary.add(tmp6);

    }

    public List<String1Value1> getChannelSummary() {
        return channelSummary;
    }

    public void setChannelSummary(List<String1Value1> channelSummary) {
        this.channelSummary = channelSummary;
    }

    public List<Bill> createUserChannelBills(Bill billClass, WebUser webUser, Department department) {

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelProPayment, BillType.ChannelAgencyCommission, BillType.ChannelIncomeBill, BillType.ChannelExpenesBill, BillType.AgentCreditNoteBill, BillType.AgentDebitNoteBill};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "SELECT b FROM Bill b WHERE type(b)=:bill "
                + " and b.retired=false "
                + " and b.billType in :bt "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();

        if (department != null) {
            sql += " and b.department=:dep ";
            temMap.put("dep", department);
        }

        if (webUser != null) {
            sql += " and b.creater=:web ";
            temMap.put("web", webUser);
        }

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("bt", bts);
        temMap.put("ins", getSessionController().getInstitution());

        sql += " order by b.insId ";

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }
    
    public List<Bill> createUserCashierBills(Bill billClass, WebUser webUser, Department department) {

        BillType[] billTypes = {BillType.OpdBill, BillType.PharmacySale, BillType.PharmacyWholeSale, BillType.GrnPaymentPre, BillType.PaymentBill, BillType.PettyCash, BillType.CashRecieveBill, BillType.AgentPaymentReceiveBill, BillType.CollectingCentrePaymentReceiveBill, BillType.InwardPaymentBill, BillType.CashIn, BillType.CashOut, BillType.DrawerAdjustment};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "SELECT b FROM Bill b WHERE type(b)=:bill "
                + " and b.retired=false "
                + " and b.billType in :bt "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();

        if (department != null) {
            sql += " and b.department=:dep ";
            temMap.put("dep", department);
        }

        if (webUser != null) {
            sql += " and b.creater=:web ";
            temMap.put("web", webUser);
        }

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("bt", bts);
        temMap.put("ins", getSessionController().getInstitution());

        sql += " order by b.insId ";

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }
    
    public List<Bill> createUserAllBills(Bill billClass, WebUser webUser, Department department) {

        BillType[] billTypes = {BillType.OpdBill, BillType.PharmacySale, BillType.PharmacyWholeSale, BillType.GrnPaymentPre, BillType.PaymentBill, BillType.PettyCash, BillType.CashRecieveBill, BillType.AgentPaymentReceiveBill, BillType.CollectingCentrePaymentReceiveBill, BillType.InwardPaymentBill, BillType.CashIn, BillType.CashOut, BillType.DrawerAdjustment, BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelProPayment, BillType.ChannelAgencyCommission, BillType.ChannelIncomeBill, BillType.ChannelExpenesBill, BillType.AgentCreditNoteBill, BillType.AgentDebitNoteBill};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "SELECT b FROM Bill b WHERE type(b)=:bill "
                + " and b.retired=false "
                + " and b.billType in :bt "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();

        if (department != null) {
            sql += " and b.department=:dep ";
            temMap.put("dep", department);
        }

        if (webUser != null) {
            sql += " and b.creater=:web ";
            temMap.put("web", webUser);
        }

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("bt", bts);
        temMap.put("ins", getSessionController().getInstitution());

        sql += " order by b.insId ";

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calChannelTotal(Bill billClass, WebUser wUser, Department department, PaymentMethod paymentMethod) {

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelProPayment, BillType.ChannelAgencyCommission, BillType.ChannelIncomeBill, BillType.ChannelExpenesBill, BillType.AgentCreditNoteBill, BillType.AgentDebitNoteBill};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "SELECT sum(b.netTotal) FROM Bill b WHERE"
                + " type(b)=:bill "
                + " and b.retired=false  "
                + " and b.billType in :bt "
                + " and b.paymentMethod=:pm"
                + " and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();

        if (department != null) {
            sql += " and b.department=:dep ";
            temMap.put("dep", department);
        }

        if (webUser != null) {
            sql += " and b.creater=:w";
            temMap.put("w", wUser);
        }

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bt", bts);
        temMap.put("pm", paymentMethod);

        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bill", billClass.getClass());

        sql += " order by b.insId ";

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

     public double calCashierTotal(Bill billClass, WebUser wUser, Department department, PaymentMethod paymentMethod) {

        BillType[] billTypes = {BillType.OpdBill, BillType.PharmacySale, BillType.PharmacyWholeSale, BillType.GrnPaymentPre, BillType.PaymentBill, BillType.PettyCash, BillType.CashRecieveBill, BillType.AgentPaymentReceiveBill, BillType.CollectingCentrePaymentReceiveBill, BillType.InwardPaymentBill, BillType.CashIn, BillType.CashOut, BillType.DrawerAdjustment};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "SELECT sum(b.netTotal) FROM Bill b WHERE"
                + " type(b)=:bill "
                + " and b.retired=false  "
                + " and b.billType in :bt "
                + " and b.paymentMethod=:pm"
                + " and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();

        if (department != null) {
            sql += " and b.department=:dep ";
            temMap.put("dep", department);
        }

        if (webUser != null) {
            sql += " and b.creater=:w";
            temMap.put("w", wUser);
        }

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bt", bts);
        temMap.put("pm", paymentMethod);

        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bill", billClass.getClass());

        sql += " order by b.insId ";

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calAllTotal(Bill billClass, WebUser wUser, Department department, PaymentMethod paymentMethod) {

        BillType[] billTypes = {BillType.OpdBill, BillType.PharmacySale, BillType.PharmacyWholeSale, BillType.GrnPaymentPre, BillType.PaymentBill, BillType.PettyCash, BillType.CashRecieveBill, BillType.AgentPaymentReceiveBill, BillType.CollectingCentrePaymentReceiveBill, BillType.InwardPaymentBill, BillType.CashIn, BillType.CashOut, BillType.DrawerAdjustment, BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelProPayment, BillType.ChannelAgencyCommission, BillType.ChannelIncomeBill, BillType.ChannelExpenesBill, BillType.AgentCreditNoteBill, BillType.AgentDebitNoteBill};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "SELECT sum(b.netTotal) FROM Bill b WHERE"
                + " type(b)=:bill "
                + " and b.retired=false  "
                + " and b.billType in :bt "
                + " and b.paymentMethod=:pm"
                + " and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();

        if (department != null) {
            sql += " and b.department=:dep ";
            temMap.put("dep", department);
        }

        if (webUser != null) {
            sql += " and b.creater=:w";
            temMap.put("w", wUser);
        }

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bt", bts);
        temMap.put("pm", paymentMethod);

        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bill", billClass.getClass());

        sql += " order by b.insId ";

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }
    double finalCashTot;
    double finalAgentTot;
    double finalCardTot;
    double finalChequeTot;
    double finalSlipTot;
    List<WebUserBillsTotal> webUserBillsTotals;

    public void calCashierData() {

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        finalCashTot = finalChequeTot = finalCardTot = finalAgentTot = finalSlipTot = 0;
        webUserBillsTotals = new ArrayList<>();
        for (WebUser webUser : getCashiers()) {
            WebUserBillsTotal tmp = new WebUserBillsTotal();
            tmp.setWebUser(webUser);
            List<BillsTotals> billls = new ArrayList<>();

            double uCard = 0;
            double uCash = 0;
            double uCheque = 0;
            double uAgent = 0;
            double uSlip = 0;
            for (BillType btp : bts) {
                BillsTotals newB = createRow(btp, "Billed", new BilledBill(), webUser);

                if (newB.getCard() != 0 || newB.getCash() != 0 || newB.getCheque() != 0 || newB.getAgent() != 0 || newB.getSlip() != 0) {
                    billls.add(newB);
                }

                BillsTotals newC = createRow(btp, "Cancelled", new CancelledBill(), webUser);

                if (newC.getCard() != 0 || newC.getCash() != 0 || newC.getCheque() != 0 || newC.getAgent() != 0 || newC.getSlip() != 0) {
                    billls.add(newC);
                }

                BillsTotals newR = createRow(btp, "Refunded", new RefundBill(), webUser);

                if (newR.getCard() != 0 || newR.getCash() != 0 || newR.getCheque() != 0 || newR.getAgent() != 0 || newR.getSlip() != 0) {
                    billls.add(newR);
                }

                uCard += (newB.getCard() + newC.getCard() + newR.getCard());
                uCash += (newB.getCash() + newC.getCash() + newR.getCash());
                uCheque += (newB.getCheque() + newC.getCheque() + newR.getCheque());
                uAgent += (newB.getAgent() + newC.getAgent() + newR.getAgent());
                uSlip += (newB.getSlip() + newC.getSlip() + newR.getSlip());

            }

            BillsTotals newSum = new BillsTotals();
            newSum.setName("Total ");
            newSum.setBold(true);
            newSum.setCard(uCard);
            newSum.setCash(uCash);
            newSum.setCheque(uCheque);
            newSum.setAgent(uAgent);
            newSum.setSlip(uSlip);

            if (newSum.getCard() != 0 || newSum.getCash() != 0 || newSum.getCheque() != 0 || newSum.getAgent() != 0 || newSum.getSlip() != 0) {
                billls.add(newSum);
            }

            tmp.setBillsTotals(billls);
            webUserBillsTotals.add(tmp);

        }

    }

    private BillsTotals createRow(BillType billType, String suffix, Bill bill, WebUser webUser) {
        BillsTotals newB = new BillsTotals();
        newB.setName(billType.getLabel() + " " + suffix);
        newB.setCard(calTotalValueOwn(webUser, bill, PaymentMethod.Card, billType));
        finalCardTot += newB.getCard();
        newB.setCash(calTotalValueOwn(webUser, bill, PaymentMethod.Cash, billType));
        finalCashTot += newB.getCash();
        newB.setCheque(calTotalValueOwn(webUser, bill, PaymentMethod.Cheque, billType));
        finalChequeTot += newB.getCheque();
        newB.setAgent(calTotalValueOwn(webUser, bill, PaymentMethod.Agent, billType));
        finalAgentTot += newB.getAgent();
        newB.setSlip(calTotalValueOwn(webUser, bill, PaymentMethod.Slip, billType));
        finalSlipTot += newB.getSlip();

        return newB;

    }

    List<WebUser> cashiers;
    @EJB
    WebUserFacade webUserFacade;

    public List<WebUser> getCashiers() {

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql;
        Map temMap = new HashMap();
        sql = "select us from "
                + " Bill b "
                + " join b.creater us "
                + " where b.retired=false "
                + " and b.institution=:ins "
                + " and b.billType in :btp "
                + " and b.createdAt between :fromDate and :toDate "
                + " group by us "
                + " having sum(b.netTotal)!=0 ";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", bts);
        temMap.put("ins", sessionController.getInstitution());
        cashiers = getWebUserFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        if (cashiers == null) {
            cashiers = new ArrayList<>();
        }

        return cashiers;
    }

    private double calTotalValueOwn(WebUser w, Bill billClass, PaymentMethod pM, BillType billType) {

        String sql;
        Map temMap = new HashMap();

        sql = "select sum(b.netTotal) from Bill b where type(b)=:bill and b.creater=:cret and "
                + " b.paymentMethod= :payMethod  and b.institution=:ins"
                + " and b.billType= :billTp and b.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billTp", billType);
        temMap.put("payMethod", pM);
        temMap.put("bill", billClass.getClass());
        temMap.put("cret", w);
        temMap.put("ins", getSessionController().getInstitution());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void calCashierDataTotalOnly() {

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        finalCashTot = finalChequeTot = finalCardTot = finalAgentTot = finalSlipTot = 0;
        webUserBillsTotals = new ArrayList<>();
        for (WebUser webUser : getCashiers()) {
            WebUserBillsTotal tmp = new WebUserBillsTotal();
            tmp.setWebUser(webUser);
            List<BillsTotals> billls = new ArrayList<>();

            double uCard = 0;
            double uCash = 0;
            double uCheque = 0;
            double uAgent = 0;
            double uSlip = 0;
            for (BillType btp : bts) {
                BillsTotals newB = createRow(btp, "Billed", new BilledBill(), webUser);
                BillsTotals newC = createRow(btp, "Cancelled", new CancelledBill(), webUser);
                BillsTotals newR = createRow(btp, "Refunded", new RefundBill(), webUser);

                uCard += (newB.getCard() + newC.getCard() + newR.getCard());
                uCash += (newB.getCash() + newC.getCash() + newR.getCash());
                uCheque += (newB.getCheque() + newC.getCheque() + newR.getCheque());
                uAgent += (newB.getAgent() + newC.getAgent() + newR.getAgent());
                uSlip += (newB.getSlip() + newC.getSlip() + newR.getSlip());

            }

            BillsTotals newSum = new BillsTotals();
            newSum.setName("Total ");
            newSum.setBold(true);
            newSum.setCard(uCard);
            newSum.setCash(uCash);
            newSum.setCheque(uCheque);
            newSum.setAgent(uAgent);
            newSum.setSlip(uSlip);

            if (newSum.getCard() != 0 || newSum.getCash() != 0 || newSum.getCheque() != 0 || newSum.getAgent() != 0 || newSum.getSlip() != 0) {
                billls.add(newSum);
            }

            tmp.setBillsTotals(billls);
            webUserBillsTotals.add(tmp);

        }

    }

    List<BillFee> billedBillFeeList;
    List<BillFee> cancelledBillFeeList;
    List<BillFee> refundBillFeeList;
    double billedBillTotal;
    double canceledBillTotl;
    double refundBillTotal;

    public void createFeeTable() {

        billedBillFeeList = new ArrayList<>();
        cancelledBillFeeList = new ArrayList<>();
        refundBillFeeList = new ArrayList<>();
        billedBillTotal = 0.0;
        canceledBillTotl = 0.0;
        refundBillTotal = 0.0;

        billedBillFeeList = getBillFee(new BilledBill());
        cancelledBillFeeList = getBillFee(new CancelledBill());
        refundBillFeeList = getBillFee(new RefundBill());

        billedBillTotal = calFeeTotal(new BilledBill());
        canceledBillTotl = calFeeTotal(new CancelledBill());
        refundBillTotal = calFeeTotal(new RefundBill());

    }

    List<BillFee> billFeeList;

    public List<BillFee> getBillFee(Bill bill) {

        String sql;
        Map m = new HashMap();
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        FeeType[] feeTypes = {FeeType.Staff, FeeType.OwnInstitution, FeeType.OtherInstitution, FeeType.Service};
        List<FeeType> fts = Arrays.asList(feeTypes);

        sql = " select bf from BillFee  bf where "
                + " bf.bill.retired=false "
                + " and bf.bill.singleBillSession.sessionDate between :fd and :td "
                + " and bf.bill.billType in :bt "
                + " and type(bf.bill)=:class "
                + " and bf.fee.feeType in :ft ";

        if (getWebUser() != null) {
            sql += " and bf.bill.creater=:wu ";
            m.put("wu", getWebUser());
        }

        sql += " order by bf.bill.insId ";

        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("class", bill.getClass());
        m.put("ft", fts);
        m.put("bt", bts);

        billFeeList = getBillFeeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        return billFeeList;
    }

    List<FeetypeFee> feetypeFees;

    public void createFeeTypeBillFeeList() {

        feetypeFees = new ArrayList<>();

        FeeType[] feeTypes = {FeeType.Staff, FeeType.OwnInstitution, FeeType.OtherInstitution, FeeType.Service};
        List<FeeType> fts = Arrays.asList(feeTypes);
        for (FeeType feeType : fts) {
            FeetypeFee ftf = new FeetypeFee();
            ftf.setFeeType(feeType);
            ftf.setBilledBillFees(getBillFeeWithFeeTypes(new BilledBill(), feeType));
            ftf.setCanceledBillFees(getBillFeeWithFeeTypes(new CancelledBill(), feeType));
            ftf.setRefunBillFees(getBillFeeWithFeeTypes(new RefundBill(), feeType));

            ftf.setBilledBillFeeTypeTotal(calFeeTypeTotal(ftf.getBilledBillFees()));
            ftf.setCanceledBillFeeTypeTotal(calFeeTypeTotal(ftf.getCanceledBillFees()));
            ftf.setRefundBillFeeTypeTotal(calFeeTypeTotal(ftf.getRefunBillFees()));

            if (ftf.getBilledBillFees() != null && !ftf.getBilledBillFees().isEmpty() || ftf.getCanceledBillFees() != null && !ftf.getCanceledBillFees().isEmpty() || ftf.getRefunBillFees() != null && !ftf.getRefunBillFees().isEmpty()) {
                feetypeFees.add(ftf);
            }
        }

        billedBillTotal = 0.0;
        canceledBillTotl = 0.0;
        refundBillTotal = 0.0;

        billedBillTotal = calFeeTotal(new BilledBill());
        canceledBillTotl = calFeeTotal(new CancelledBill());
        refundBillTotal = calFeeTotal(new RefundBill());
    }

    public double calFeeTypeTotal(List<BillFee> billFees) {

        double feeTypeTotal = 0.0;
        for (BillFee bf : billFees) {
            feeTypeTotal += bf.getFee().getFee();
        }
        return feeTypeTotal;
    }

    public List<BillFee> getBillFeeWithFeeTypes(Bill bill, FeeType feeType) {

        String sql;
        Map m = new HashMap();
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        sql = " select bf from BillFee  bf where "
                + " bf.bill.retired=false "
                + " and bf.bill.singleBillSession.sessionDate between :fd and :td "
                + " and bf.bill.billType in :bt "
                + " and type(bf.bill)=:class "
                + " and bf.fee.feeType =:ft ";

        if (getWebUser() != null) {
            sql += " and bf.bill.creater=:wu ";
            m.put("wu", getWebUser());
        }

        sql += " order by bf.bill.insId ";

        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("class", bill.getClass());
        m.put("ft", feeType);
        m.put("bt", bts);

        billFeeList = getBillFeeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        return billFeeList;
    }

    //get scan count and other channel count seperatly
    public double countBillByBillTypeAndFeeType(Bill bill, FeeType ft, BillType bt, boolean sessoinDate, boolean paid) {

        String sql;
        Map m = new HashMap();

        sql = " select count(distinct(bf.bill)) from BillFee  bf where "
                + " bf.bill.retired=false "
                + " and bf.bill.billType=:bt "
                + " and type(bf.bill)=:class "
                + " and bf.fee.feeType =:ft "
                + " and bf.feeValue>0 ";

        if (bill.getClass().equals(CancelledBill.class)) {
            sql += " and bf.bill.cancelled=true";
        }
        if (bill.getClass().equals(RefundBill.class)) {
            sql += " and bf.bill.refunded=true";
        }

        if (ft == FeeType.OwnInstitution) {
            sql += " and bf.fee.name =:fn ";
            m.put("fn", "Hospital Fee");
        }

        if (paid) {
            sql += " and bf.bill.paidBill is not null "
                    + " and bf.bill.paidAmount!=0 ";
        }
        if (sessoinDate) {
            sql += " and bf.bill.singleBillSession.sessionDate between :fd and :td ";
        } else {
            sql += " and bf.bill.createdAt between :fd and :td ";
        }

        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("class", BilledBill.class);
        m.put("ft", ft);
        m.put("bt", bt);
//        m.put("fn", "Scan Fee");

        double d = getBillFeeFacade().findAggregateLong(sql, m, TemporalType.TIMESTAMP);

        return d;
    }

    public double hospitalTotalBillByBillTypeAndFeeType(Bill bill, FeeType fts, BillType bt, boolean sessoinDate, boolean paid) {

        String sql;
        Map m = new HashMap();

        sql = " select distinct(bf.bill) from BillFee  bf where "
                + " bf.bill.retired=false "
                + " and bf.bill.billType=:bt "
                + " and type(bf.bill)=:class "
                + " and bf.fee.feeType =:ft "
                + " and bf.feeValue>0 ";

        if (bill.getClass().equals(CancelledBill.class)) {
            sql += " and bf.bill.cancelled=true";
        }
        if (bill.getClass().equals(RefundBill.class)) {
            sql += " and bf.bill.refunded=true";
        }

        if (paid) {
            sql += " and bf.bill.paidBill is not null "
                    + " and bf.bill.paidAmount!=0 ";
        }
        if (sessoinDate) {
            sql += " and bf.bill.singleBillSession.sessionDate between :fd and :td ";
        } else {
            sql += " and bf.bill.createdAt between :fd and :td ";
        }

        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("class", BilledBill.class);
        m.put("ft", fts);
        m.put("bt", bt);
//        m.put("fn", "Scan Fee");

        List<Bill> b = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        double d = 0;

        for (Bill b1 : b) {
            for (BillFee bf : b1.getBillFees()) {
                if (bf.getFee().getFeeType() != FeeType.Staff) {
                    d += bf.getFeeValue();
                }
            }
        }

        return d;
    }

//    public double hospitalTotalBillByBillTypeAndFeeType(Bill bill, List<String> ftps, BillType bt, boolean sessoinDate, boolean paid) {
//
//        String sql;
//        Map m = new HashMap();
//
//        sql = " select sum(bf.bill.hospitalFee) from BillFee  bf "
//                + "where bf.bill.retired=false "
//                + " and bf.bill.billType=:bt "
//                + " and type(bf.bill)=:class "
//                + " and bf.fee.name not in :ftp "
//                + " and bf.feeValue>0 ";
//
//        if (bill.getClass().equals(CancelledBill.class)) {
//            sql += " and bf.bill.cancelled=true";
//            System.err.println("cancel");
//        }
//        if (bill.getClass().equals(RefundBill.class)) {
//            sql += " and bf.bill.refunded=true";
//            System.err.println("Refund");
//        }
//
//        if (paid) {
//            sql += " and bf.bill.paidBill is not null "
//                    + " and bf.bill.paidAmount!=0 ";
//        }
//        if (sessoinDate) {
//            sql += " and bf.bill.singleBillSession.sessionDate between :fd and :td ";
//        } else {
//            sql += " and bf.bill.createdAt between :fd and :td ";
//        }
//
//        m.put("fd", getFromDate());
//        m.put("td", getToDate());
//        m.put("class", BilledBill.class);
//        m.put("ftp", ftps);
//        m.put("bt", bt);
////        m.put("fn", "Scan Fee");
//
//        double d = getBillFeeFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
//
//        //System.out.println("sql = " + sql);
//        //System.out.println("m = " + m);
//        //System.out.println("getBillFeeFacade().findAggregateLong(sql, m, TemporalType.TIMESTAMP) = " + d);
//        return d;
//    }
    public double countBillByBillType(Bill bill, BillType bt, boolean sessoinDate, Staff st) {

        String sql;
        Map m = new HashMap();

        sql = " select count(distinct(bi.bill)) from BillItem  bi  "
                + " where bi.retired=false ";

        if (sessoinDate) {
            sql += " and bi.bill.singleBillSession.sessionDate between :fd and :td ";
        } else {
            sql += " and bi.bill.createdAt between :fd and :td ";
        }

        if (bt != null) {
            sql += " and bi.bill.billType=:bt ";
            m.put("bt", bt);
        }

        if (bill != null) {
            sql += " and type(bi.bill)=:class ";
            m.put("class", bill.getClass());
        }

        if (st != null) {
            sql += " and bi.billSession.staff =:stf ";
            m.put("stf", st);
        }

        m.put("fd", getFromDate());
        m.put("td", getToDate());

        double d = getBillFeeFacade().findAggregateLong(sql, m, TemporalType.TIMESTAMP);

        return d;
    }

//    public double countScan(BillType bt, Bill bill, boolean sessoinDate) {
//        Map m = new HashMap();
//        String sql = " select count(bf) from BillFee  bf where "
//                + " bf.bill.retired=false "
//                + " and bf.bill.billType=:bt "
//                + " and bf.feeValue>0 "
//                + " and type(bf.bill)=:class "
//                + " and bf.fee.feeType =:ft"
//                + " and bf.bill.createdAt between :fd and :td ";
//        
//        m.put("fd", getFromDate());
//        m.put("td", getToDate());
//        m.put("ft", FeeType.Service);
//        m.put("class", bill.getClass());
//        m.put("bt", bt);
//        //System.out.println("getBillFeeFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP) = " + getBillFeeFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP));
//        return getBillFeeFacade().findAggregateLong(sql, m, TemporalType.TIMESTAMP);
//    }
    FeeType feeType;
    List<BillFee> listBilledBillFees;
    List<BillFee> listCanceledBillFees;
    List<BillFee> listRefundBillFees;

    public void getUsersWithFeeType() {
        if (getFeeType() == null) {
            JsfUtil.addErrorMessage("Please Insert Fee Type");
        } else {
            listBilledBillFees = getBillFeeWithFeeTypes(new BilledBill(), getFeeType());
            listCanceledBillFees = getBillFeeWithFeeTypes(new CancelledBill(), getFeeType());
            listRefundBillFees = getBillFeeWithFeeTypes(new RefundBill(), getFeeType());
        }

    }

    public void createChannelDoctorPaymentTable() {
        createChannelDoctorPayment(false);
    }

    public void createChannelDoctorPaymentTableBySession() {
        createChannelDoctorPayment(true);
    }

    public void createChannelDoctorPayment(boolean bySession) {
        doctorPaymentSummeryRows = new ArrayList<>();

        BillType[] billTypes = {BillType.ChannelCash, BillType.ChannelAgent, BillType.ChannelPaid};
        List<BillType> bts = Arrays.asList(billTypes);

        List<Staff> staffs = new ArrayList<>();

        if (staff != null) {
            staffs.add(staff);
        } else {
            staffs.addAll(getChannelPaymentStaffbyClassType(bts, BillType.PaymentBill, fromDate, toDate));
        }

        for (Staff stf : staffs) {
            DoctorPaymentSummeryRow doctorPaymentSummeryRow = new DoctorPaymentSummeryRow();

            doctorPaymentSummeryRow.setConsultant(stf);

            if (bySession) {
                doctorPaymentSummeryRow.setDoctorPaymentSummeryRowSubs(getDoctorPaymentSummeryRowSubsBySession(bts, BillType.PaymentBill, stf, fromDate, toDate));
            } else {
                doctorPaymentSummeryRow.setDoctorPaymentSummeryRowSubs(getDoctorPaymentSummeryRowSubs(bts, BillType.PaymentBill, stf, fromDate, toDate));
            }
            if (!doctorPaymentSummeryRow.getDoctorPaymentSummeryRowSubs().isEmpty()) {
                doctorPaymentSummeryRows.add(doctorPaymentSummeryRow);
            }

        }

    }

    public void createDoctorPaymentBySession() {
        doctorPaymentSummeryRows = new ArrayList<>();

        BillType[] billTypes = {BillType.ChannelCash, BillType.ChannelAgent, BillType.ChannelPaid};
        List<BillType> bts = Arrays.asList(billTypes);

        List<Staff> staffs = new ArrayList<>();

        if (staff != null) {
            staffs.add(staff);
        } else {
            staffs.addAll(getChannelPaymentStaffbyClassType(bts, BillType.PaymentBill, fromDate, toDate));
        }

        for (Staff stf : staffs) {
            DoctorPaymentSummeryRow doctorPaymentSummeryRow = new DoctorPaymentSummeryRow();

            doctorPaymentSummeryRow.setConsultant(stf);

            doctorPaymentSummeryRow.setDoctorPaymentSummeryRowSubs(getSessionTotal(bts, BillType.PaymentBill, stf));

            if (!doctorPaymentSummeryRow.getDoctorPaymentSummeryRowSubs().isEmpty()) {
                doctorPaymentSummeryRows.add(doctorPaymentSummeryRow);
            }

        }
    }

    public List<DoctorPaymentSummeryRowSub> getSessionTotal(List<BillType> bts, BillType bt, Staff staff) {
        List<DoctorPaymentSummeryRowSub> doctorPaymentSummeryRowSubs;
        doctorPaymentSummeryRowSubs = new ArrayList<>();

        List<ServiceSession> sessions = new ArrayList<>();
        double ptCount = 0;

        for (ServiceSession ss : getServiceSessions(fromDate, toDate, staff)) {
            DoctorPaymentSummeryRowSub dpsrs = new DoctorPaymentSummeryRowSub();
            dpsrs.setServiceSession(ss);
            dpsrs.setBills(getChannelPaymentBillListbyClassTypes(bts, bt, null, fromDate, toDate, staff, ss));

            dpsrs.setStaffFeeTotal(getStaffFeeTotal(dpsrs.getBills()));

            double cashCount = 0;
            double onCallCount = 0;
            double agentCount = 0;
            double staffCount = 0;

            for (Bill b : dpsrs.getBills()) {
                if (b.getReferenceBill() == null) {
                    if (b.getPaymentMethod() == PaymentMethod.Cash) {
                        cashCount++;
                        ptCount++;
                    }

                    if (b.getPaymentMethod() == PaymentMethod.Agent) {
                        agentCount++;
                        ptCount++;
                    }
                }

                if (b.getReferenceBill() != null) {
                    if (b.getReferenceBill().getPaymentMethod() == PaymentMethod.OnCall) {
                        onCallCount++;
                        ptCount++;
                    }

                    if (b.getReferenceBill().getPaymentMethod() == PaymentMethod.Staff) {
                        staffCount++;
                        ptCount++;
                    }
                }

                dpsrs.setCashCount(cashCount);
                dpsrs.setAgentCount(agentCount);
                dpsrs.setOnCallCount(onCallCount);
                dpsrs.setStaffCount(staffCount);
                //ptCount+=(cashCount + agentCount + onCallCount + staffCount);

            }

            dpsrs.setTotalCount(ptCount);

            if (!dpsrs.getBills().isEmpty()) {
                doctorPaymentSummeryRowSubs.add(dpsrs);
            }

        }

        return doctorPaymentSummeryRowSubs;
    }

    public List<DoctorPaymentSummeryRowSub> getDoctorPaymentSummeryRowSubs(List<BillType> bts, BillType bt, Staff staff, Date fd, Date td) {
        List<DoctorPaymentSummeryRowSub> doctorPaymentSummeryRowSubs;
        doctorPaymentSummeryRowSubs = new ArrayList<>();

        Date nowDate = fd;
        Calendar cal = Calendar.getInstance();
        cal.setTime(nowDate);

        while (nowDate.before(td)) {
            DateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
            String formattedDate = df.format(nowDate);
            //System.out.println("formattedDate = " + formattedDate);
            //System.out.println("nowDate = " + nowDate);

            DoctorPaymentSummeryRowSub doctorPaymentSummeryRowSub = new DoctorPaymentSummeryRowSub();

            doctorPaymentSummeryRowSub.setDate(nowDate);

            doctorPaymentSummeryRowSub.setBills(getChannelPaymentBillListbyClassTypes(bts, bt, nowDate, null, null, staff, null));

            doctorPaymentSummeryRowSub.setHospitalFeeTotal(getHospitalFeeTotal(doctorPaymentSummeryRowSub.getBills()));
            doctorPaymentSummeryRowSub.setStaffFeeTotal(getStaffFeeTotal(doctorPaymentSummeryRowSub.getBills()));

            double cashCount = 0;
            double onCallCount = 0;
            double agentCount = 0;
            double staffCount = 0;

            for (Bill b : doctorPaymentSummeryRowSub.getBills()) {
                if (b.getReferenceBill() == null) {
                    if (b.getPaymentMethod() == PaymentMethod.Cash) {
                        cashCount++;
                    }

                    if (b.getPaymentMethod() == PaymentMethod.Agent) {
                        agentCount++;
                    }
                }

                if (b.getReferenceBill() != null) {
                    if (b.getReferenceBill().getPaymentMethod() == PaymentMethod.OnCall) {
                        onCallCount++;
                    }

                    if (b.getReferenceBill().getPaymentMethod() == PaymentMethod.Staff) {
                        staffCount++;
                    }
                }

                doctorPaymentSummeryRowSub.setCashCount(cashCount);
                doctorPaymentSummeryRowSub.setAgentCount(agentCount);
                doctorPaymentSummeryRowSub.setOnCallCount(onCallCount);
                doctorPaymentSummeryRowSub.setStaffCount(staffCount);

            }

            Calendar nc = Calendar.getInstance();
            nc.setTime(nowDate);
            nc.add(Calendar.DATE, 1);
            nowDate = nc.getTime();

            if (!doctorPaymentSummeryRowSub.getBills().isEmpty()) {
                doctorPaymentSummeryRowSubs.add(doctorPaymentSummeryRowSub);
            }

        }

        return doctorPaymentSummeryRowSubs;
    }

    public void createUnpaidDoctorVoucher() {
        doctorPaymentSummeryRows = new ArrayList<>();

        BillType[] billTypes = {BillType.ChannelCash, BillType.ChannelAgent, BillType.ChannelPaid};
        List<BillType> bts = Arrays.asList(billTypes);

        Date nowDate = fromDate;
        Calendar cal = Calendar.getInstance();
        cal.setTime(nowDate);

        while (nowDate.before(toDate)) {
            DateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
            String formattedDate = df.format(nowDate);

            DoctorPaymentSummeryRow doctorPaymentSummeryRow = new DoctorPaymentSummeryRow();

            doctorPaymentSummeryRow.setDate(nowDate);

            doctorPaymentSummeryRow.setDoctorPaymentSummeryRowSubs(getDoctorUnPaidSummeryRowSubs(doctorPaymentSummeryRow.getDate(), bts));

            Calendar nc = Calendar.getInstance();
            nc.setTime(nowDate);
            nc.add(Calendar.DATE, 1);
            nowDate = nc.getTime();

            if (!doctorPaymentSummeryRow.getDoctorPaymentSummeryRowSubs().isEmpty()) {
                doctorPaymentSummeryRows.add(doctorPaymentSummeryRow);
            }
        }
    }

    public List<DoctorPaymentSummeryRowSub> getDoctorUnPaidSummeryRowSubs(Date d, List<BillType> bts) {
        List<DoctorPaymentSummeryRowSub> doctorPaymentSummeryRowSubs;
        doctorPaymentSummeryRowSubs = new ArrayList<>();

        List<Staff> staffs = new ArrayList<>();

        if (staff != null) {
            staffs.add(staff);
        } else {
            staffs.addAll(getChannelUnPaidStaffbyClassType(bts, fromDate, toDate));
        }

        for (Staff staff : staffs) {
            DoctorPaymentSummeryRowSub doctorPaymentSummeryRowSub = new DoctorPaymentSummeryRowSub();
            doctorPaymentSummeryRowSub.setConsultant(staff);
            doctorPaymentSummeryRowSub.setBills(getChannelUnPaidBillListbyClassTypes(bts, d, staff));
            doctorPaymentSummeryRowSub.setHospitalFeeTotal(getHospitalFeeTotal(doctorPaymentSummeryRowSub.getBills()));
            doctorPaymentSummeryRowSub.setStaffFeeTotal(getStaffFeeTotal(doctorPaymentSummeryRowSub.getBills()));

            if (!doctorPaymentSummeryRowSub.getBills().isEmpty()) {
                doctorPaymentSummeryRowSubs.add(doctorPaymentSummeryRowSub);
            }

        }

        return doctorPaymentSummeryRowSubs;
    }

    public List<Bill> getChannelUnPaidBillListbyClassTypes(List<BillType> bts, Date d, Staff stf) {
        HashMap hm = new HashMap();

        Date fd = commonFunctions.getStartOfDay(d);
        Date td = commonFunctions.getEndOfDay(d);

        String sql = "SELECT distinct(bf.bill) FROM BillFee bf "
                + " WHERE bf.retired = false "
                + " and bf.bill.cancelled=false "
                + " and bf.bill.refunded=false "
                + " and bf.staff=:st "
                + " and bf.bill.billType in :bts "
                + " and bf.paidValue=0 "
                + " and bf.createdAt between :fd and :td ";

        hm.put("fd", fd);
        hm.put("td", td);
        hm.put("bts", bts);
        hm.put("st", stf);

        return billFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<DoctorPaymentSummeryRowSub> getDoctorPaymentSummeryRowSubsBySession(List<BillType> bts, BillType bt, Staff staff, Date fd, Date td) {
        List<DoctorPaymentSummeryRowSub> doctorPaymentSummeryRowSubs;
        doctorPaymentSummeryRowSubs = new ArrayList<>();

        List<ServiceSession> sessions = new ArrayList<>();

        Date nowDate = fd;
        Calendar cal = Calendar.getInstance();
        cal.setTime(nowDate);

        while (nowDate.before(td)) {
            DateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
            String formattedDate = df.format(nowDate);

            if (serviceSession != null) {
                sessions.add(serviceSession);
            } else {
                sessions.addAll(getServiceSessions(nowDate, null, staff));
            }

            for (ServiceSession ss : sessions) {

                DoctorPaymentSummeryRowSub doctorPaymentSummeryRowSub = new DoctorPaymentSummeryRowSub();
                doctorPaymentSummeryRowSub.setServiceSession(ss);
                doctorPaymentSummeryRowSub.setDate(nowDate);
                //System.out.println("doctorPaymentSummeryRowSub.getServiceSession() = " + doctorPaymentSummeryRowSub.getServiceSession());
                doctorPaymentSummeryRowSub.setBills(getChannelPaymentBillListbyClassTypes(bts, bt, nowDate, null, null, staff, ss));

                doctorPaymentSummeryRowSub.setHospitalFeeTotal(getHospitalFeeTotal(doctorPaymentSummeryRowSub.getBills()));
                doctorPaymentSummeryRowSub.setStaffFeeTotal(getStaffFeeTotal(doctorPaymentSummeryRowSub.getBills()));

                double cashCount = 0;
                double onCallCount = 0;
                double agentCount = 0;
                double staffCount = 0;

                for (Bill b : doctorPaymentSummeryRowSub.getBills()) {
                    if (b.getReferenceBill() == null) {
                        if (b.getPaymentMethod() == PaymentMethod.Cash) {
                            cashCount++;
                        }

                        if (b.getPaymentMethod() == PaymentMethod.Agent) {
                            agentCount++;
                        }
                    }

                    if (b.getReferenceBill() != null) {
                        if (b.getReferenceBill().getPaymentMethod() == PaymentMethod.OnCall) {
                            onCallCount++;
                        }

                        if (b.getReferenceBill().getPaymentMethod() == PaymentMethod.Staff) {
                            agentCount++;
                        }
                    }

                    doctorPaymentSummeryRowSub.setCashCount(cashCount);
                    doctorPaymentSummeryRowSub.setAgentCount(agentCount);
                    doctorPaymentSummeryRowSub.setOnCallCount(onCallCount);
                    doctorPaymentSummeryRowSub.setStaffCount(staffCount);

                }

                Calendar nc = Calendar.getInstance();
                nc.setTime(nowDate);
                nc.add(Calendar.DATE, 1);
                nowDate = nc.getTime();

                if (!doctorPaymentSummeryRowSub.getBills().isEmpty()) {
                    doctorPaymentSummeryRowSubs.add(doctorPaymentSummeryRowSub);
                }

            }

        }

        return doctorPaymentSummeryRowSubs;
    }

    public List<ServiceSession> getServiceSessions(Date fd, Date td, Staff s) {
        HashMap hm = new HashMap();
        String sql = "";

        Date frd = new Date();
        Date tod = new Date();

        if (fd != null && td == null) {
            frd = commonFunctions.getStartOfDay(fd);
            tod = commonFunctions.getEndOfDay(fd);
        }

        sql = "Select distinct(s) From ServiceSession s "
                + " where s.retired=false "
                + " and s.staff=:stf "
                + " and s.sessionDate between :fd and :td ";

        hm.put("stf", s);

        if (fd != null && td == null) {
            hm.put("fd", frd);
            hm.put("td", tod);
        } else {
            hm.put("fd", fd);
            hm.put("td", td);
        }

        return serviceSessionFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public void fillSessions() {
        String sql;
        Map m = new HashMap();
        sql = "Select s From ServiceSession s "
                + " where s.retired=false "
                + " and type(s)=:class "
                + " and s.staff=:doc "
                + " and s.originatingSession is null "
                + " order by s.sessionWeekday,s.startingTime";
        m.put("doc", staff);
        m.put("class", ServiceSession.class);
        serviceSessions = serviceSessionFacade.findByJpql(sql, m);
    }

    public List<Bill> getChannelPaymentBillListbyClassTypes(List<BillType> bts, BillType bt, Date d, Date sessionFDate, Date sessionTDate, Staff stf, ServiceSession ss) {
        HashMap hm = new HashMap();

        Date fd = new Date();
        Date td = new Date();

//        String sql = "select bf from BillFee bf "
//                + " where bf.bill.retired=false "
//                + " and bf.bill.billType=:bt "
//                + " and bf.staff =:st "
//                + " and bf.billItem.paidForBillFee.bill.billType in :bts"
//                + " and bf.createdAt between :fd and :td ";
        String sql = "SELECT bi.paidForBillFee.bill FROM BillItem bi "
                + " WHERE bi.retired = false "
                + " and bi.bill.cancelled=false "
                + " and bi.bill.refunded=false "
                + " and bi.bill.billType=:bt "
                + " and bi.paidForBillFee.staff=:st "
                + " and bi.paidForBillFee.bill.billType in :bts ";

        if (ss != null) {
            sql += " and bi.paidForBillFee.bill.singleBillSession.serviceSession=:itm ";
            hm.put("itm", ss);
        }

        if (sessionFDate != null && sessionTDate != null) {
            sql += " and bi.paidForBillFee.bill.singleBillSession.serviceSession.sessionDate between :fd and :td ";
            hm.put("fd", sessionFDate);
            hm.put("td", sessionTDate);
        }

        if (d != null) {
            fd = commonFunctions.getStartOfDay(d);
            td = commonFunctions.getEndOfDay(d);
            sql += " and bi.createdAt between :fd and :td ";
            hm.put("fd", fd);
            hm.put("td", td);
        }

        hm.put("bt", bt);
        hm.put("bts", bts);
        hm.put("st", stf);

        return billFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public void createAbsentPatientTable() {
        channelBills = new ArrayList<>();
        channelBills.addAll(getChannelBillsAbsentPatient(staff));
    }

    public List<Bill> getChannelBillsAbsentPatient(Staff stf) {
        HashMap hm = new HashMap();
        String sql = " select b from Bill b "
                + " where b.retired=false "
                + " and b.singleBillSession.absent=true "
                + " and b.createdAt between :fd and :td";

        if (stf != null) {
            sql += " and b.staff=:st";
            hm.put("st", stf);
        }

        sql += " order by b.insId ";

        hm.put("fd", fromDate);
        hm.put("td", toDate);

        List<Bill> b = getBillFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

        doctorFeeTotal = getStaffFeeTotal(b);

        return b;
    }

    public double getChannelPaymentBillCountbyClassTypes(Bill b, List<BillType> bts, BillType bt, Date d, Staff stf, PaymentMethod pm) {
        HashMap hm = new HashMap();

        Date fd = commonFunctions.getStartOfDay(d);
        Date td = commonFunctions.getEndOfDay(d);

        String sql = "SELECT count(bi.paidForBillFee.bill) FROM BillItem bi "
                + " WHERE bi.retired = false "
                + " and bi.bill.cancelled=false "
                + " and bi.bill.refunded=false "
                + " and bi.bill.billType=:bt "
                + " and bi.paidForBillFee.staff=:st "
                + " and bi.paidForBillFee.bill.billType in :bts "
                + " and bi.createdAt between :fd and :td ";

        if (b.getReferenceBill() == null) {
            sql += " and bi.paidForBillFee.bill.paymentMethod=:pay";
        }

        if (b.getReferenceBill() != null) {
            sql += " and bi.paidForBillFee.bill.referenceBill.paymentMethod=:pay";
        }

        hm.put("bt", bt);
        hm.put("fd", fd);
        hm.put("td", td);
        hm.put("bts", bts);
        hm.put("pay", pm);
        hm.put("st", stf);

        return billFacade.findAggregateLong(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Staff> getChannelPaymentStaffbyClassType(List<BillType> bts, BillType bt, Date fd, Date td) {
        HashMap hm = new HashMap();

//        String sql = "select distinct(bf.staff) from BillFee bf "
//                + " where bf.bill.retired=false "
//                + " and bf.bill.billType=:bt "
//                + " and bf.staff is not null "
//                + " and bf.billItem.paidForBillFee.bill.billType in :bts"
//                + " and bf.createdAt between :fd and :td ";
        String sql = "SELECT distinct(bi.paidForBillFee.staff) FROM BillItem bi "
                + " WHERE bi.retired = false "
                + " and bi.bill.billType=:bt "
                + " and bi.bill.cancelled=false "
                + " and bi.bill.refunded=false "
                + " and bi.paidForBillFee.staff is not null "
                //+ " and type(bi.bill)=:class "
                + " and bi.paidForBillFee.bill.billType in :bts "
                + " and bi.createdAt between :fromDate and :toDate ";

        hm.put("bt", bt);
        hm.put("fromDate", fd);
        hm.put("toDate", td);
        hm.put("bts", bts);

        return staffFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Staff> getChannelUnPaidStaffbyClassType(List<BillType> bts, Date fd, Date td) {
        HashMap hm = new HashMap();

        String sql = "SELECT distinct(bf.staff) FROM BillFee bf "
                + " WHERE bf.retired = false "
                + " and bf.bill.cancelled=false "
                + " and bf.bill.refunded=false "
                + " and bf.staff is not null "
                //+ " and type(bi.bill)=:class "
                + " and bf.bill.billType in :bts "
                + " and bf.paidValue=0 "
                + " and bf.createdAt between :fromDate and :toDate ";

        //hm.put("bt", bt);
        hm.put("fromDate", fd);
        hm.put("toDate", td);
        hm.put("bts", bts);

        return staffFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public void createAllChannelBillReportByCreatedDate() {
        createAllChannelBillReport(true);
    }

    public void createAllChannelBillReportBySessionDate() {
        createAllChannelBillReport(false);
    }

    public void createAllChannelBillReport(boolean createdDate) {
        channelBills = new ArrayList<>();
        channelBillsCancelled = new ArrayList<>();
        channelBillsRefunded = new ArrayList<>();

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid};
        List<BillType> bts = Arrays.asList(billTypes);

        channelBills.addAll(channelBillListByBillType(createdDate, new BilledBill(), bts, fromDate, toDate));
        channelBillsCancelled.addAll(channelBillListByBillType(createdDate, new CancelledBill(), bts, fromDate, toDate));
        channelBillsRefunded.addAll(channelBillListByBillType(createdDate, new RefundBill(), bts, fromDate, toDate));

        doctorFeeBilledTotal = getStaffFeeTotal(channelBills);
        doctorFeeCancellededTotal = getStaffFeeTotal(channelBillsCancelled);
        doctorFeeRefundededTotal = getStaffFeeTotal(channelBillsRefunded);

        hospitalFeeBilledTotal = getHospitalFeeTotal(channelBills);
        hospitalFeeCancellededTotal = getHospitalFeeTotal(channelBillsCancelled);
        hospitalFeeRefundededTotal = getHospitalFeeTotal(channelBillsRefunded);
    }

    public double getHospitalFeeTotal(List<Bill> bills) {
        double tot = 0;

        for (Bill b : bills) {
            tot += b.getHospitalFee();
        }
        return tot;
    }

    public double getStaffFeeTotal(List<Bill> bills) {
        double tot = 0;

        for (Bill b : bills) {
            tot += b.getStaffFee();
        }
        return tot;
    }

    public List<Bill> channelBillListByBillType(boolean createdDate, Bill bill, List<BillType> billTypes, Date fd, Date td) {

        HashMap hm = new HashMap();

        String sql = " select b from Bill b "
                + " where b.retired=false ";
        if (createdDate) {
            sql += " and b.createdAt between :fDate and :tDate ";
        } else {
            sql += " and b.singleBillSession.sessionDate between :fDate and :tDate ";
        }

        if (!billTypes.isEmpty()) {
            sql += " and b.billType in :bt ";
            hm.put("bt", billTypes);
        }

        if (bill != null) {
            sql += " and type(b)=:class";
            hm.put("class", bill.getClass());
        }

        sql += " order by b.singleBillSession.sessionDate,b.singleBillSession.serviceSession.startingTime ";

        hm.put("fDate", fd);
        hm.put("tDate", td);

        return billFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public void createConsultantCountTableByCreatedDate() {
        createConsultantCountTable(false);
    }

    public void createConsultantCountTableBySessionDate() {
        createConsultantCountTable(true);
    }

    public void createConsultantCountTable(boolean sessionDate) {
        bookingCountSummryRows = new ArrayList<>();
        double billedCount = 0;
        double canceledCount = 0;
        double refundCount = 0;

        BillType[] billTypes = {BillType.ChannelCash, BillType.ChannelAgent, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        for (Staff s : getStaffbyClassType(bts, fromDate, toDate)) {

            BookingCountSummryRow row = new BookingCountSummryRow();
            double[] arr = new double[4];
            int i = 0;

            row.setConsultant(s);

            for (BillType bt : bts) {
                billedCount = countBillByBillType(new BilledBill(), bt, sessionDate, s);
                canceledCount = countBillByBillType(new CancelledBill(), bt, sessionDate, s);
                refundCount = countBillByBillType(new RefundBill(), bt, sessionDate, s);
                arr[i] = billedCount - (canceledCount + refundCount);
                i++;
            }
            row.setCashCount(arr[0]);
            row.setAgentCount(arr[1]);
            row.setOncallCount(arr[2]);
            row.setStaffCount(arr[3]);

            bookingCountSummryRows.add(row);

        }
    }

    public List<Staff> getStaffbyClassType(List<BillType> bts, Date fd, Date td) {
        HashMap hm = new HashMap();
//        String sql = "select p from Staff p where p.retired=false ";
//        
//        if(st!=null){
//            //System.out.println("1");
//            sql+=" and type(p)=:class ";
//            hm.put("class", st.getClass());
//        }

        String sql = "select distinct(bf.staff) from BillFee bf "
                + " where bf.bill.retired=false "
                + " and bf.bill.billType in :bts "
                + " and bf.staff is not null "
                + " and bf.createdAt between :fd and :td ";

        hm.put("bts", bts);
        hm.put("fd", fd);
        hm.put("td", td);

        return staffFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public void createChannelPatientCountByCreatedDate() {
        createChannelPatientCount(false);
    }

    public void createChannelPatientCountBySessionDate() {
        createChannelPatientCount(true);
    }

    public void createChannelHospitalIncomeByCreatedDate() {
        createChannelHospitalIncome(false);
    }

    public void createChannelHospitalIncomeBySessionDate() {
        createChannelHospitalIncome(true);
    }

    public void createChannelPatientCount(boolean sessionDate) {
        bookingCountSummryRows = new ArrayList<>();
        BillType[] billTypes = {BillType.ChannelCash,
            BillType.ChannelOnCall,
            BillType.ChannelStaff,
            BillType.ChannelAgent,};
        List<BillType> bts = Arrays.asList(billTypes);
        createSmmeryRows(bts, sessionDate, FeeType.OwnInstitution);
        createSmmeryRows(bts, sessionDate, FeeType.Service);
    }

    public void createChannelHospitalIncome(boolean sessionDate) {
        bookingCountSummryRows = new ArrayList<>();
        BillType[] billTypes = {BillType.ChannelCash,
            BillType.ChannelOnCall,
            BillType.ChannelStaff,
            BillType.ChannelAgent,};
        List<BillType> bts = Arrays.asList(billTypes);
        createSmmeryRowsHospitalIncome(bts, sessionDate, FeeType.OwnInstitution);
        createSmmeryRowsHospitalIncome(bts, sessionDate, FeeType.Service);
    }

    public void createSmmeryRows(List<BillType> bts, boolean sessionDate, FeeType ft) {
        for (BillType bt : bts) {
            BookingCountSummryRow row = new BookingCountSummryRow();
            if (ft == FeeType.Service) {
                row.setBookingType("Scan " + bt.getLabel());
                row.setBilledCount(countBillByBillTypeAndFeeType(new BilledBill(), ft, bt, sessionDate, paid));
                row.setCancelledCount(countBillByBillTypeAndFeeType(new CancelledBill(), ft, bt, sessionDate, paid));
                row.setRefundCount(countBillByBillTypeAndFeeType(new RefundBill(), ft, bt, sessionDate, paid));
            } else {
                row.setBookingType(bt.getLabel());
                row.setBilledCount(countBillByBillTypeAndFeeType(new BilledBill(), ft, bt, sessionDate, paid));
                row.setCancelledCount(countBillByBillTypeAndFeeType(new CancelledBill(), ft, bt, sessionDate, paid));
                row.setRefundCount(countBillByBillTypeAndFeeType(new RefundBill(), ft, bt, sessionDate, paid));
            }

            bookingCountSummryRows.add(row);

        }
    }

    public void createSmmeryRowsHospitalIncome(List<BillType> bts, boolean sessionDate, FeeType ft) {
//        String[] ftpForScan = {"Doctor Fee", "Hospital Fee"};
//        List<String> ftpsForScan = Arrays.asList(ftpForScan);
//
//        String[] ftpForOnlyHos = {"Doctor Fee", "Scan Fee"};
//        List<String> ftpsForForOnlyHos = Arrays.asList(ftpForOnlyHos);

        for (BillType bt : bts) {
            BookingCountSummryRow row = new BookingCountSummryRow();
            if (ft == FeeType.Service) {
                row.setBookingType("Scan " + bt.getLabel());
                row.setBilledCount(hospitalTotalBillByBillTypeAndFeeType(new BilledBill(), FeeType.Service, bt, sessionDate, paid));
                row.setCancelledCount(hospitalTotalBillByBillTypeAndFeeType(new CancelledBill(), FeeType.Service, bt, sessionDate, paid));
                row.setRefundCount(hospitalTotalBillByBillTypeAndFeeType(new RefundBill(), FeeType.Service, bt, sessionDate, paid));
            } else {
                row.setBookingType(bt.getLabel());
                row.setBilledCount(hospitalTotalBillByBillTypeAndFeeType(new BilledBill(), FeeType.OwnInstitution, bt, sessionDate, paid));
                row.setCancelledCount(hospitalTotalBillByBillTypeAndFeeType(new CancelledBill(), FeeType.OwnInstitution, bt, sessionDate, paid));
                row.setRefundCount(hospitalTotalBillByBillTypeAndFeeType(new RefundBill(), FeeType.OwnInstitution, bt, sessionDate, paid));
            }

            bookingCountSummryRows.add(row);

        }
    }

    public List<BillFee> getListBilledBillFees() {
        return listBilledBillFees;
    }

    public void setListBilledBillFees(List<BillFee> listBilledBillFees) {
        this.listBilledBillFees = listBilledBillFees;
    }

    public List<BillFee> getListCanceledBillFees() {
        return listCanceledBillFees;
    }

    public void setListCanceledBillFees(List<BillFee> listCanceledBillFees) {
        this.listCanceledBillFees = listCanceledBillFees;
    }

    public List<BillFee> getListRefundBillFees() {
        return listRefundBillFees;
    }

    public void setListRefundBillFees(List<BillFee> listRefundBillFees) {
        this.listRefundBillFees = listRefundBillFees;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public void setFeeType(FeeType feeType) {
        this.feeType = feeType;
    }

    public List<FeetypeFee> getFeetypeFees() {
        return feetypeFees;
    }

    public void setFeetypeFees(List<FeetypeFee> feetypeFees) {
        this.feetypeFees = feetypeFees;
    }

    public List<BillFee> getBillFeeList() {
        if (billFeeList == null) {
            billFeeList = new ArrayList<>();
        }
        return billFeeList;
    }

    public void setBillFeeList(List<BillFee> billFeeList) {
        this.billFeeList = billFeeList;
    }

    public double calFeeTotal(Bill bill) {

        String sql;
        Map m = new HashMap();
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        FeeType[] feeTypes = {FeeType.Staff, FeeType.OwnInstitution, FeeType.OtherInstitution, FeeType.Service};
        List<FeeType> fts = Arrays.asList(feeTypes);

        sql = " select sum(bf.feeValue) from BillFee  bf where "
                + " bf.bill.retired=false "
                + " and bf.bill.singleBillSession.sessionDate between :fd and :td "
                + " and bf.bill.billType in :bt "
                + " and type(bf.bill)=:class "
                + " and bf.fee.feeType in :ft ";

        if (getWebUser() != null) {
            sql += " and bf.bill.creater=:wu ";
            m.put("wu", getWebUser());
        }

        sql += " order by bf.bill.insId ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("ft", fts);
        m.put("bt", bts);

        return getBillFeeFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public double getBilledBillTotal() {
        return billedBillTotal;
    }

    public void setBilledBillTotal(double billedBillTotal) {
        this.billedBillTotal = billedBillTotal;
    }

    public double getCanceledBillTotl() {
        return canceledBillTotl;
    }

    public void setCanceledBillTotl(double canceledBillTotl) {
        this.canceledBillTotl = canceledBillTotl;
    }

    public double getRefundBillTotal() {
        return refundBillTotal;
    }

    public void setRefundBillTotal(double refundBillTotal) {
        this.refundBillTotal = refundBillTotal;
    }

    public List<BillFee> getBilledBillFeeList() {
        if (billedBillFeeList == null) {
            billedBillFeeList = new ArrayList<>();
        }
        return billedBillFeeList;
    }

    public void setBilledBillFeeList(List<BillFee> billedBillFeeList) {
        this.billedBillFeeList = billedBillFeeList;
    }

    public List<BillFee> getCancelledBillFeeList() {
        return cancelledBillFeeList;
    }

    public void setCancelledBillFeeList(List<BillFee> cancelledBillFeeList) {
        this.cancelledBillFeeList = cancelledBillFeeList;
    }

    public List<BillFee> getRefundBillFeeList() {
        return refundBillFeeList;
    }

    public void setRefundBillFeeList(List<BillFee> refundBillFeeList) {
        this.refundBillFeeList = refundBillFeeList;
    }

    public double getFinalCashTot() {
        return finalCashTot;
    }

    public void setFinalCashTot(double finalCashTot) {
        this.finalCashTot = finalCashTot;
    }

    public double getFinalAgentTot() {
        return finalAgentTot;
    }

    public void setFinalAgentTot(double finalAgentTot) {
        this.finalAgentTot = finalAgentTot;
    }

    public double getFinalCardTot() {
        return finalCardTot;
    }

    public void setFinalCardTot(double finalCardTot) {
        this.finalCardTot = finalCardTot;
    }

    public double getFinalChequeTot() {
        return finalChequeTot;
    }

    public void setFinalChequeTot(double finalChequeTot) {
        this.finalChequeTot = finalChequeTot;
    }

    public double getFinalSlipTot() {
        return finalSlipTot;
    }

    public void setFinalSlipTot(double finalSlipTot) {
        this.finalSlipTot = finalSlipTot;
    }

    public List<WebUserBillsTotal> getWebUserBillsTotals() {
        return webUserBillsTotals;
    }

    public void setWebUserBillsTotals(List<WebUserBillsTotal> webUserBillsTotals) {
        this.webUserBillsTotals = webUserBillsTotals;
    }

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }

    public void setCashiers(List<WebUser> cashiers) {
        this.cashiers = cashiers;
    }

    public List<Department> getDeps() {
        return deps;
    }

    public void setDeps(List<Department> deps) {
        this.deps = deps;
    }

    public List<DepartmentBill> getDepBills() {
        return depBills;
    }

    public void setDepBills(List<DepartmentBill> depBills) {
        this.depBills = depBills;
    }

    public void channelBillListCreatedDate() {
        channelBillList(true);
    }

    public void channelBillListSessionDate() {
        channelBillList(false);
    }

    public void channelBillList(boolean createdDate) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        HashMap hm = new HashMap();

        String sql = " select b from Bill b "
                + " where b.billType in :bt "
                + " and b.retired=false ";
        if (createdDate) {
            sql += " and b.createdAt between :fDate and :tDate ";
        } else {
            sql += " and b.singleBillSession.sessionDate between :fDate and :tDate ";
        }
        sql += " order by b.singleBillSession.sessionDate,b.singleBillSession.serviceSession.startingTime ";

        hm.put("bt", bts);
        hm.put("fDate", getFromDate());
        hm.put("tDate", getToDate());

        channelBills = billFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public void createChannelFees() {
        valueList = new ArrayList<>();
        FeeType[] fts = {FeeType.Staff, FeeType.OwnInstitution, FeeType.OtherInstitution, FeeType.Service};

        for (FeeType ft : fts) {
            setFeeTotals(valueList, ft);
        }
        calTotals(valueList);

        ////System.out.println("***Done***");
    }

    public void setFeeTotals(List<String1Value3> s1v3s, FeeType feeType) {
        double totBill = 0.0;
        double totCan = 0.0;
        double totRef = 0.0;
        String1Value3 s1v3 = new String1Value3();
        totBill = getFeeTotal(new BilledBill(), feeType);
        totCan = getFeeTotal(new CancelledBill(), feeType);
        totRef = getFeeTotal(new RefundBill(), feeType);

        switch (feeType) {
            case Staff:
                s1v3.setString("Staff Fee");
                break;
            case OwnInstitution:
                s1v3.setString("Hospital Fee ");
                break;

            case OtherInstitution:
                s1v3.setString("Agency Fee");
                break;

            case Service:
                s1v3.setString("Scan Fee ");
                break;

        }
        s1v3.setValue1(totBill);
        s1v3.setValue2(totCan);
        s1v3.setValue3(totRef);

        ////System.out.println("*************");
        ////System.out.println("Fee - " + s1v3.getString());
        ////System.out.println("Bill - " + s1v3.getValue1());
        ////System.out.println("Can - " + s1v3.getValue2());
        ////System.out.println("Ref - " + s1v3.getValue3());
        s1v3s.add(s1v3);
        ////System.out.println("Add");
        ////System.out.println("*************");
    }

    public double getFeeTotal(Bill bill, FeeType feeType) {

        String sql;
        Map m = new HashMap();
        BillType[] bts = {BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelStaff,};
        List<BillType> bt = Arrays.asList(bts);
        sql = " select sum(bf.feeValue) from BillFee  bf where "
                + " bf.bill.retired=false "
                + " and bf.bill.singleBillSession.sessionDate between :fd and :td "
                + " and bf.bill.billType in :bt "
                + " and type(bf.bill)=:class "
                + " and bf.fee.feeType=:ft ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("ft", feeType);
        m.put("bt", bt);

        return getBillFeeFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void calTotals(List<String1Value3> string1Value3s) {
        totalBilled = 0.0;
        totalCancel = 0.0;
        totalRefund = 0.0;
        for (String1Value3 s1v3 : string1Value3s) {
            totalBilled += s1v3.getValue1();
            totalCancel += s1v3.getValue2();
            totalRefund += s1v3.getValue3();
        }

    }

    public List<ChannelReportColumnModel> getChannelReportColumnModels() {
        return channelReportColumnModels;
    }

    public void setChannelReportColumnModels(List<ChannelReportColumnModel> channelReportColumnModels) {
        this.channelReportColumnModels = channelReportColumnModels;
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
            toDate = CommonFunctions.getEndOfDay(toDate);
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public void makeNull() {
        reportKeyWord = null;
        serviceSession = null;
        billSessions = null;
        valueList = null;
    }

    List<BillSession> nurseViewSessions;
    List<BillSession> doctorViewSessions;

    public List<BillSession> getNurseViewSessions() {
        return nurseViewSessions;
    }

    public void setNurseViewSessions(List<BillSession> nurseViewSessions) {
        this.nurseViewSessions = nurseViewSessions;
    }

    public List<BillSession> getDoctorViewSessions() {
        return doctorViewSessions;
    }

    public void setDoctorViewSessions(List<BillSession> doctorViewSessions) {
        this.doctorViewSessions = doctorViewSessions;
    }

    public void markAsAbsent() {
        if (selectedBillSessions == null) {
            JsfUtil.addSuccessMessage("Please Select Sessions");
            return;
        }
        for (BillSession bs : selectedBillSessions) {
            bs.setAbsent(true);
            billSessionFacade.edit(bs);
            JsfUtil.addSuccessMessage("Marked Succesful");
        }
    }

    public void fillNurseView() {
        nurseViewSessions = new ArrayList<>();
        if (sessionInstance == null) {
            JsfUtil.addErrorMessage("Please Select Session");
            return;
        }

        String sql = "Select bs "
                + " From BillSession bs "
                + " where bs.retired=false "
                + " and type(bs.bill)=:class "
                + " and bs.bill.billType in :tbs "
                + " and bs.sessionInstance=:si"
                + " order by bs.serialNo";
        HashMap hh = new HashMap();
        hh.put("class", BilledBill.class);
        hh.put("si", sessionInstance);
        
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.ChannelAgent);
        bts.add(BillType.ChannelCash);
        bts.add(BillType.ChannelOnCall);
        bts.add(BillType.ChannelStaff);
        hh.put("tbs", bts);
        nurseViewSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.TIMESTAMP);

    }

    public void fillDoctorView() {
        doctorViewSessions = new ArrayList<>();
        if (sessionInstance != null) {
            String sql = "Select bs "
                    + " From BillSession bs "
                    + " where bs.retired=false "
                    + " and bs.bill.billType in :tbs "
                    + " and bs.sessionInstance=:si"
                    + " and bs.sessionDate= :ssDate"
                    + " order by bs.serialNo";
            HashMap hh = new HashMap();
            hh.put("ssDate", sessionInstance.getSessionDate());
            hh.put("si", sessionInstance);
            List<BillType> bts = new ArrayList<>();
            bts.add(BillType.ChannelAgent);
            bts.add(BillType.ChannelCash);
            bts.add(BillType.ChannelOnCall);
            bts.add(BillType.ChannelStaff);
            hh.put("tbs", bts);
            doctorViewSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);
            netTotal = 0.0;
            grantNetTotal = 0.0;
            for (BillSession bs : doctorViewSessions) {
                if (Math.abs(bs.getBill().getBalance()) < 1) {
                    netTotal += bs.getBill().getStaffFee();
                    grantNetTotal += bs.getBill().getNetTotal();
                }
            }
        }
    }

    private String sendingSmsText;

    public void sendCustomSms() {
        if (sendingSmsText == null) {
            JsfUtil.addErrorMessage("No SMS Text");
            return;
        }
        int count = 0;
        for (BillSession bs : getBillSessionsNurse()) {
            Sms e = new Sms();
            e.setCreatedAt(new Date());
            e.setBill(bs.getBill());
            e.setCreater(sessionController.getLoggedUser());
            e.setCreatedAt(new Date());
            e.setCreater(sessionController.getLoggedUser());
            e.setReceipientNumber(bs.getBill().getPatient().getPerson().getPhone());
            e.setSendingMessage((sendingSmsText));
            e.setDepartment(getSessionController().getLoggedUser().getDepartment());
            e.setInstitution(getSessionController().getLoggedUser().getInstitution());
            e.setSmsType(MessageType.ChannelCustom);
            smsFacade.create(e);
            count++;
            smsManagerEjb.sendSms(e);
        }
        sendingSmsText = null;
    }

    public void sendReminderSms() {

        if (serviceSession == null) {
            JsfUtil.addErrorMessage("No Service Session");
            return;
        }
        int count = 0;
        for (BillSession bs : getBillSessionsNurse()) {
            sendingSmsText = createReminderSmsBody(bs);
            if (bs == null) {
                continue;
            }
            Sms e = new Sms();
            e.setCreatedAt(new Date());
            e.setBill(bs.getBill());
            e.setCreater(sessionController.getLoggedUser());
            e.setCreatedAt(new Date());
            e.setCreater(sessionController.getLoggedUser());
            e.setReceipientNumber(bs.getBill().getPatient().getPerson().getPhone());
            e.setSendingMessage((sendingSmsText));
            e.setDepartment(getSessionController().getLoggedUser().getDepartment());
            e.setInstitution(getSessionController().getLoggedUser().getInstitution());
            e.setSmsType(MessageType.ChannelReminder);
            smsFacade.create(e);
            smsManagerEjb.sendSms(e);
            count++;
        }
        sendingSmsText = null;
    }

    public String createArrivalSmsBody(ServiceSession bs) {
        if (bs == null) {
            return null;
        }
        if (bs.getStaff() == null) {
            return null;
        }

        if (bs.getStaff().getPerson() == null) {
            return null;
        }
        if (bs.getStaff().getPerson().getNameWithTitle() == null) {
            return null;
        }
        String m = bs.getStaff().getPerson().getNameWithTitle();
        m += " for " + bs.getName();
        m += " arrived at Baddegama Medical Services";
        m += " on " + CommonFunctions.dateToString(new Date(), "dd/MM/yyyy");
        m += " at " + CommonFunctions.dateToString(new Date(), "hh:mm a");
        m += ".";
        m += " For details 0912293700.";
        return m;
    }

    public String createReminderSmsBody(BillSession bs) {
        if (bs == null) {
            return null;
        }
        ServiceSession ss = bs.getServiceSession();
        if (ss == null) {
            return null;
        }
        if (ss.getStaff() == null) {
            return null;
        }

        if (ss.getStaff().getPerson() == null) {
            return null;
        }
        if (ss.getStaff().getPerson().getNameWithTitle() == null) {
            return null;
        }

        String m = "REMINDER. APPOINTMENT FOR ";
        m += ss.getStaff().getPerson().getNameWithTitle();
        m += " for " + ss.getName();
        m += " @ Baddegama Medical Services ";
        m += " on " + CommonFunctions.dateToString(new Date(), "dd/MM/yyyy");
        m += " at " + CommonFunctions.dateToString(new Date(), "hh:mm a");
        m += ". YOUR N0 IS ";
        m += bs.getSerialNo();
        m += ". For Details 0912293700.";
        return m;
    }

    public String createCancellationSmsBody(ServiceSession bs) {
        if (bs == null) {
            return null;
        }
        if (bs.getStaff() == null) {
            return null;
        }

        if (bs.getStaff().getPerson() == null) {
            return null;
        }
        if (bs.getStaff().getPerson().getNameWithTitle() == null) {
            return null;
        }
        String m = bs.getStaff().getPerson().getNameWithTitle();
        m += " for " + bs.getName();
        m += " IS CANCELLED.";
        if (newSessionDateTime == null) {
            m += " Next date will be informed";
        } else {
            m += " New Session";
            m += " on " + CommonFunctions.dateToString(newSessionDateTime, "dd/MM/yyyy");
            m += " at " + CommonFunctions.dateToString(newSessionDateTime, "hh:mm a");
        }
        m += ". Sorry for the inconvenience";
        return m;
    }

   

  

    public void sendCancellationSms() {

        if (serviceSession == null) {
            JsfUtil.addErrorMessage("No Service Session");
            return;
        }
        sendingSmsText = createCancellationSmsBody(serviceSession);
        for (BillSession bs : getBillSessionsNurse()) {
            Sms e = new Sms();
            e.setCreatedAt(new Date());
            e.setBill(bs.getBill());
            e.setCreater(sessionController.getLoggedUser());
            e.setCreatedAt(new Date());
            e.setCreater(sessionController.getLoggedUser());
            e.setReceipientNumber(bs.getBill().getPatient().getPerson().getPhone());
            e.setSendingMessage((sendingSmsText));
            e.setDepartment(getSessionController().getLoggedUser().getDepartment());
            e.setInstitution(getSessionController().getLoggedUser().getInstitution());
            e.setSmsType(MessageType.ChannelCancellation);
            smsFacade.create(e);
            smsManagerEjb.sendSms(e);
        }
        sendingSmsText = null;
    }

    @EJB
    SmsFacade smsFacade;

    public List<BillSession> getBillSessionsNurse() {
        billSessions = new ArrayList<>();
        if (serviceSession != null) {
            String sql = "Select bs From BillSession bs "
                    + " where bs.retired=false and "
                    + " bs.bill.cancelled=false and "
                    + " bs.bill.refunded=false and "
                    + " bs.bill.billType in :tbs and "
                    + " bs.serviceSession.id=" + serviceSession.getId() + " and bs.sessionDate= :ssDate"
                    + " order by bs.serialNo";
            HashMap hh = new HashMap();
            hh.put("ssDate", serviceSession.getSessionDate());
            List<BillType> bts = new ArrayList<>();
            bts.add(BillType.ChannelAgent);
            bts.add(BillType.ChannelCash);
            bts.add(BillType.ChannelOnCall);
            hh.put("tbs", bts);
            billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);
        }
        return billSessions;
    }

    public List<ChannelDoctor> getTotalPatient() {

        channelDoctors = new ArrayList<>();

        String sql = "Select bs.bill From BillSession bs where bs.bill.staff is not null and bs.retired=false and bs.sessionDate= :ssDate";
        HashMap hh = new HashMap();
        hh.put("ssDate", Calendar.getInstance().getTime());
        List<Bill> bills = getBillFacade().findByJpql(sql, hh, TemporalType.DATE);

        Set<Staff> consultant = new HashSet();
        for (Bill b : bills) {
            consultant.add(b.getStaff());
        }

        for (Staff c : consultant) {
            ChannelDoctor cd = new ChannelDoctor();
            cd.setConsultant(c);
            channelDoctors.add(cd);
        }

        for (ChannelDoctor cd : channelDoctors) {

            for (Bill b : bills) {

                if (b.getStaff().getId() == cd.getConsultant().getId()) {

                    if (b.getBillType() == BillType.ChannelPaid && b.getReferenceBill() != null && b instanceof BilledBill) {
                        cd.setBillCount_c(cd.getBillCount_c() + 1);
                    } else if (b.getBillType() == BillType.ChannelPaid && b.getReferenceBill() != null && b instanceof CancelledBill) {
                        cd.setBillCanncelCount_c(cd.getBillCanncelCount_c() + 1);
                    } else if (b.getBillType() == BillType.ChannelPaid && b.getReferenceBill() != null && b instanceof RefundBill) {
                        cd.setRefundedCount_c(cd.getRefundedCount_c() + 1);
                    } else if (b.getBillType() == BillType.ChannelPaid && b instanceof BilledBill) {
                        cd.setBillCount(cd.getBillCount() + 1);
                    } else if (b.getBillType() == BillType.ChannelPaid && b instanceof CancelledBill) {
                        cd.setBillCanncelCount(cd.getBillCanncelCount() + 1);
                    } else if (b.getBillType() == BillType.ChannelPaid && b instanceof RefundBill) {
                        cd.setRefundedCount(cd.getRefundedCount() + 1);
                    } else if (b.getBillType() == BillType.ChannelCredit && b instanceof BilledBill) {
                        cd.setCreditCount(cd.getCreditCount() + 1);
                    } else if (b.getBillType() == BillType.ChannelCredit && b instanceof CancelledBill) {
                        cd.setCreditCancelledCount(cd.getCreditCancelledCount() + 1);
                    }

                    if ((b.getBillType() == BillType.ChannelPaid && b.getReferenceBill() == null && b instanceof BilledBill)
                            || (b.getBillType() == BillType.ChannelCredit && b instanceof BilledBill)) {
                        BillSession bs = getBillSessionFacade().findFirstByJpql("select b from BillSession b where b.retired=false and b.bill.id=" + b.getId());
                        if (bs.isAbsent()) {
                            cd.setAbsentCount(cd.getAbsentCount() + 1);
                        }
                    }
                }
            }

        }

        return channelDoctors;
    }

    public List<BillSession> getBillSessions() {
        return billSessions;
    }

    public List<ChannelDoctor> getTotalDoctor() {

        channelDoctors = new ArrayList<ChannelDoctor>();

        String sql = "Select bs.bill From BillSession bs where bs.bill.staff is not null and bs.retired=false and bs.sessionDate= :ssDate";
        HashMap hh = new HashMap();
        hh.put("ssDate", Calendar.getInstance().getTime());
        List<Bill> bills = getBillFacade().findByJpql(sql, hh, TemporalType.DATE);

        Set<Staff> consultant = new HashSet();
        for (Bill b : bills) {
            consultant.add(b.getStaff());
        }

        for (Staff c : consultant) {
            ChannelDoctor cd = new ChannelDoctor();
            cd.setConsultant(c);
            channelDoctors.add(cd);
        }

        for (ChannelDoctor cd : channelDoctors) {

            for (Bill b : bills) {
                if (b.getStaff().getId() == cd.getConsultant().getId()) {

                    if (b.getBillType() == BillType.ChannelPaid && b.getReferenceBill() != null && b instanceof BilledBill) {

                        cd.setBillCount_c(cd.getBillCount_c() + 1);
                        cd.setBillFee_c(cd.getBillFee_c() + getBillFees(b));

                    } else if (b.getBillType() == BillType.ChannelPaid && b.getReferenceBill() != null && b instanceof CancelledBill) {

                        cd.setBillCanncelCount_c(cd.getBillCanncelCount_c() + 1);
                        cd.setBillCanncelFee_c(cd.getBillCanncelFee_c() + getBillFees(b));

                    } else if (b.getBillType() == BillType.ChannelPaid && b.getReferenceBill() != null && b instanceof RefundBill) {

                        cd.setRefundedCount_c(cd.getRefundedCount_c() + 1);
                        cd.setRefundedFee_c(cd.getRefundedFee_c() + getBillFees(b));

                    } else if (b.getBillType() == BillType.ChannelPaid && b instanceof BilledBill) {

                        cd.setBillCount(cd.getBillCount() + 1);
                        cd.setBillFee(cd.getBillFee() + getBillFees(b));

                    } else if (b.getBillType() == BillType.ChannelPaid && b instanceof CancelledBill) {

                        cd.setBillCanncelCount(cd.getBillCanncelCount() + 1);
                        cd.setBillCanncelFee(cd.getBillCanncelFee() + getBillFees(b));

                    } else if (b.getBillType() == BillType.ChannelPaid && b instanceof RefundBill) {

                        cd.setRefundedCount(cd.getRefundedCount() + 1);
                        cd.setRefundFee(cd.getRefundFee() + getBillFees(b));

                    } else if (b.getBillType() == BillType.ChannelCredit && b instanceof BilledBill) {

                        cd.setCreditCount(cd.getCreditCount() + 1);
                        cd.setCreditFee(cd.getCreditFee() + getBillFees(b));

                    } else if (b.getBillType() == BillType.ChannelCredit && b instanceof CancelledBill) {

                        cd.setCreditCancelledCount(cd.getCreditCancelledCount() + 1);
                        cd.setCreditCancelFee(cd.getCreditCancelFee() + getBillFees(b));

                    }

                    if ((b.getBillType() == BillType.ChannelPaid && b.getReferenceBill() == null && b instanceof BilledBill)
                            || (b.getBillType() == BillType.ChannelCredit && b instanceof BilledBill)) {
                        BillSession bs = getBillSessionFacade().findFirstByJpql("select b from BillSession b where b.retired=false and b.bill.id=" + b.getId());
                        if (bs.isAbsent()) {
                            cd.setAbsentCount(cd.getAbsentCount() + 1);
                        }
                    }
                }
            }

        }

        calTotal();

        return channelDoctors;
    }

    private void calTotal() {
        total = 0.0;
        for (ChannelDoctor cd : channelDoctors) {
            total += cd.getBillFee() + cd.getBillFee_c();

        }

    }

    private double getBillFees(Bill b) {
        String sql = "Select sum(b.feeValue) From BillFee b where b.retired=false and b.bill.id=" + b.getId();

        return getBillFeeFacade().findAggregateDbl(sql);
    }

    public List<BillSession> getBillSessionsUser() {
        billSessions = new ArrayList<BillSession>();
        if (serviceSession != null) {
            String sql = "Select bs From BillSession bs where bs.retired=false and bs.serviceSession.id=" + serviceSession.getId() + " and bs.sessionDate= :ssDate";
            HashMap hh = new HashMap();
            hh.put("ssDate", serviceSession.getSessionAt());
            billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);

            for (BillSession bs : billSessions) {
                bs.setDoctorFee(getChannelBean().getChannelFee(bs, FeeType.Staff));
                bs.setTax(getChannelBean().getChannelFee(bs, FeeType.Tax));

                setBilledTotalFee(getBilledTotalFee() + bs.getDoctorFee().getBilledFee().getFeeValue());
                setRepayTotalFee(getRepayTotalFee() + bs.getDoctorFee().getPrevFee().getFeeValue());
                setTaxTotal(getTaxTotal() + bs.getTax().getBilledFee().getFeeValue());
            }
        }

        return billSessions;
    }

    public List<BillSession> getBillSessionsDoctor() {
        billedTotalFee = 0.0;
        repayTotalFee = 0.0;
        taxTotal = 0.0;
        billSessions = new ArrayList<BillSession>();
        if (serviceSession != null) {
            String sql = "Select bs From BillSession bs where bs.retired=false and bs.serviceSession.id=" + serviceSession.getId() + " and bs.sessionDate= :ssDate";
            HashMap hh = new HashMap();
            hh.put("ssDate", serviceSession.getSessionAt());
            billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);

            for (BillSession bs : billSessions) {
                bs.setDoctorFee(getChannelBean().getChannelFee(bs, FeeType.Staff));
                bs.setTax(getChannelBean().getChannelFee(bs, FeeType.Tax));

                setBilledTotalFee(getBilledTotalFee() + bs.getDoctorFee().getBilledFee().getFeeValue());
                setRepayTotalFee(getRepayTotalFee() + bs.getDoctorFee().getPrevFee().getFeeValue());
                setTaxTotal(getTaxTotal() + bs.getTax().getBilledFee().getFeeValue());
            }
        }

        return billSessions;
    }

    public List<BillSession> getBillSessionsDoctorToday() {
        billedTotalFee = 0.0;
        repayTotalFee = 0.0;
        taxTotal = 0.0;
        billSessions = new ArrayList<BillSession>();

        String sql = "Select bs From BillSession bs where bs.retired=false and bs.sessionDate= :ssDate";
        HashMap hh = new HashMap();
        hh.put("ssDate", Calendar.getInstance().getTime());
        billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);

        for (BillSession bs : billSessions) {
            bs.setDoctorFee(getChannelBean().getChannelFee(bs, FeeType.Staff));
            bs.setTax(getChannelBean().getChannelFee(bs, FeeType.Tax));

            setBilledTotalFee(getBilledTotalFee() + bs.getDoctorFee().getBilledFee().getFeeValue());
            setRepayTotalFee(getRepayTotalFee() + bs.getDoctorFee().getPrevFee().getFeeValue());
            setTaxTotal(getTaxTotal() + bs.getTax().getBilledFee().getFeeValue());
        }

        return billSessions;
    }

    public void createAgentHistoryTable() {
        agentHistorys = new ArrayList<>();

        agentHistorys = createAgentHistory(fromDate, toDate, institution, null);

    }

    public void createCollectingCentreHistoryTable() {
        agentHistorys = new ArrayList<>();
        HistoryType[] hts = {HistoryType.CollectingCentreBalanceUpdateBill, HistoryType.CollectingCentreDeposit, HistoryType.CollectingCentreDepositCancel, HistoryType.CollectingCentreBilling};
        List<HistoryType> historyTypes = Arrays.asList(hts);

        agentHistorys = createAgentHistory(fromDate, toDate, institution, historyTypes);

    }

    public void createAgentHistorySubTable() {
        if (institution == null) {
            JsfUtil.addErrorMessage("Please Select Agency.");
            return;
        }
        HistoryType[] ht = {HistoryType.ChannelBooking, HistoryType.ChannelDeposit, HistoryType.ChannelDepositCancel};
        List<HistoryType> historyTypes = Arrays.asList(ht);

        agentHistoryWithDate = new ArrayList<>();
        Date nowDate = getFromDate();

        while (nowDate.before(getToDate())) {
            Date fd = commonFunctions.getStartOfDay(nowDate);
            Date td = commonFunctions.getEndOfDay(nowDate);
            AgentHistoryWithDate ahwd = new AgentHistoryWithDate();
            if (createAgentHistory(fd, td, institution, historyTypes).size() > 0) {
                ahwd.setDate(nowDate);
                ahwd.setAhs(createAgentHistory(fd, td, institution, historyTypes));
                agentHistoryWithDate.add(ahwd);
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(nowDate);
            cal.add(Calendar.DATE, 1);
            nowDate = cal.getTime();
        }

    }

    public List<AgentHistory> createAgentHistory(Date fd, Date td, Institution i, List<HistoryType> hts) {
        String sql;
        Map m = new HashMap();

        sql = " select ah from AgentHistory ah where ah.retired=false "
                + " and ah.bill.retired=false "
                + " and ah.createdAt between :fd and :td ";

        if (i != null) {
            sql += " and (ah.bill.fromInstitution=:ins"
                    + " or ah.bill.creditCompany=:ins) ";

            m.put("ins", i);
        }

        if (hts != null) {
            sql += " and ah.historyType in :hts ";

            m.put("hts", hts);
        }

        m.put("fd", fd);
        m.put("td", td);

        sql += " order by ah.createdAt ";

        return getAgentHistoryFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    /**
     * Creates a new instance of ChannelReportController
     */
    public List<Bill> getBilledBills() {
        return billedBills;
    }

    public void setBilledBills(List<Bill> billedBills) {
        this.billedBills = billedBills;
    }

    public List<Bill> getCancelBills() {
        return cancelBills;
    }

    public void setCancelBills(List<Bill> cancelBills) {
        this.cancelBills = cancelBills;
    }

    public List<Bill> getRefundBills() {
        return refundBills;
    }

    public void setRefundBills(List<Bill> refundBills) {
        this.refundBills = refundBills;
    }

    public ChannelReportController() {
    }

    @Deprecated
    public ServiceSession getServiceSession() {
        return serviceSession;
    }

    @Deprecated
    public void setServiceSession(ServiceSession serviceSession) {
        this.serviceSession = serviceSession;
    }
    
    

    public void setBillSessions(List<BillSession> billSessions) {
        this.billSessions = billSessions;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public double getBilledTotalFee() {
        return billedTotalFee;
    }

    public void setBilledTotalFee(double billedTotalFee) {
        this.billedTotalFee = billedTotalFee;
    }

    public double getRepayTotalFee() {
        return repayTotalFee;
    }

    public void setRepayTotalFee(double repayTotalFee) {
        this.repayTotalFee = repayTotalFee;
    }

    public double getTaxTotal() {
        return taxTotal;
    }

    public void setTaxTotal(double taxTotal) {
        this.taxTotal = taxTotal;
    }

    public BillsTotals getBilledBillList() {
        if (billedBillList == null) {
            billedBillList = new BillsTotals();
        }
        return billedBillList;
    }

    public void setBilledBillList(BillsTotals billedBillList) {
        this.billedBillList = billedBillList;
    }

    public BillsTotals getCanceledBillList() {
        if (canceledBillList == null) {
            canceledBillList = new BillsTotals();
        }
        return canceledBillList;
    }

    public void setCanceledBillList(BillsTotals canceledBillList) {
        this.canceledBillList = canceledBillList;
    }

    public BillsTotals getRefundBillList() {
        if (refundBillList == null) {
            refundBillList = new BillsTotals();
        }
        return refundBillList;
    }

    public void setRefundBillList(BillsTotals refundBillList) {
        this.refundBillList = refundBillList;
    }

    public List<ChannelDoctor> getChannelDoctors() {
        return channelDoctors;
    }

    public void setChannelDoctors(List<ChannelDoctor> channelDoctors) {
        this.channelDoctors = channelDoctors;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public ChannelBean getChannelBean() {
        return channelBean;
    }

    public void setChannelBean(ChannelBean channelBean) {
        this.channelBean = channelBean;
    }

    public List<BillSession> getBillSessionsBilled() {
        return billSessionsBilled;
    }

    public void setBillSessionsBilled(List<BillSession> billSessionsBilled) {
        this.billSessionsBilled = billSessionsBilled;
    }

    public List<BillSession> getBillSessionsReturn() {
        return billSessionsReturn;
    }

    public void setBillSessionsReturn(List<BillSession> billSessionsReturn) {
        this.billSessionsReturn = billSessionsReturn;
    }

    public List<BillSession> getBillSessionsCancelled() {
        return billSessionsCancelled;
    }

    public void setBillSessionsCancelled(List<BillSession> billSessionsCancelled) {
        this.billSessionsCancelled = billSessionsCancelled;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public List<String1Value3> getValueList() {
        if (valueList == null) {
            valueList = new ArrayList<>();
        }
        return valueList;
    }

    public void setValueList(List<String1Value3> valueList) {
        this.valueList = valueList;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public double getCancelTotal() {
        return cancelTotal;
    }

    public List<Bill> getChannelBills() {
        return channelBills;
    }

    public void setChannelBills(List<Bill> channelBills) {
        this.channelBills = channelBills;
    }

    public void setCancelTotal(double cancelTotal) {
        this.cancelTotal = cancelTotal;
    }

    public double getRefundTotal() {
        return refundTotal;
    }

    public void setRefundTotal(double refundTotal) {
        this.refundTotal = refundTotal;
    }

    public double getTotalBilled() {
        return totalBilled;
    }

    public void setTotalBilled(double totalBilled) {
        this.totalBilled = totalBilled;
    }

    public double getTotalCancel() {
        return totalCancel;
    }

    public void setTotalCancel(double totalCancel) {
        this.totalCancel = totalCancel;
    }

    public double getTotalRefund() {
        return totalRefund;
    }

    public void setTotalRefund(double totalRefund) {
        this.totalRefund = totalRefund;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    List<ChannelReportColumnModel> columns;

    public List<ChannelReportColumnModel> getColumns() {
        return columns;
    }

    public void setColumns(List<ChannelReportColumnModel> columns) {
        this.columns = columns;
    }

    public List<AgentHistory> getAgentHistorys() {
        return agentHistorys;
    }

    public void setAgentHistorys(List<AgentHistory> agentHistorys) {
        this.agentHistorys = agentHistorys;
    }

    public AgentHistoryFacade getAgentHistoryFacade() {
        return agentHistoryFacade;
    }

    public void setAgentHistoryFacade(AgentHistoryFacade agentHistoryFacade) {
        this.agentHistoryFacade = agentHistoryFacade;
    }

    public List<AgentHistoryWithDate> getAgentHistoryWithDate() {
        return agentHistoryWithDate;
    }

    public void setAgentHistoryWithDate(List<AgentHistoryWithDate> agentHistoryWithDate) {
        this.agentHistoryWithDate = agentHistoryWithDate;
    }

    public List<BookingCountSummryRow> getBookingCountSummryRows() {
        return bookingCountSummryRows;
    }

    public void setBookingCountSummryRows(List<BookingCountSummryRow> bookingCountSummryRows) {
        this.bookingCountSummryRows = bookingCountSummryRows;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public String getSendingSmsText() {
        return sendingSmsText;
    }

    public void setSendingSmsText(String sendingSmsText) {
        this.sendingSmsText = sendingSmsText;
    }

    public Date getNewSessionDateTime() {
        return newSessionDateTime;
    }

    public void setNewSessionDateTime(Date newSessionDateTime) {
        this.newSessionDateTime = newSessionDateTime;
    }

    public SessionInstance getSessionInstance() {
        return sessionInstance;
    }

    public void setSessionInstance(SessionInstance sessionInstance) {
        this.sessionInstance = sessionInstance;
    }
  
    public int getChannelReportMenuIndex() {
        return channelReportMenuIndex;
    }

    public void setChannelReportMenuIndex(int channelReportMenuIndex) {
        this.channelReportMenuIndex = channelReportMenuIndex;
    }

    public List<SessionInstance> getSessioninstances() {
        return sessioninstances;
    }

    public void setSessioninstances(List<SessionInstance> sessioninstances) {
        this.sessioninstances = sessioninstances;
    }

    public class ChannelReportColumnModelBundle implements Serializable {

        List<ChannelReportColumnModel> rows;
        ChannelReportColumnModel bundle;

        public ChannelReportColumnModelBundle() {
        }

        public List<ChannelReportColumnModel> getRows() {
            return rows;
        }

        public void setRows(List<ChannelReportColumnModel> rows) {
            this.rows = rows;
        }

        public ChannelReportColumnModel getBundle() {
            return bundle;
        }

        public void setBundle(ChannelReportColumnModel bundle) {
            this.bundle = bundle;
        }

    }

    public class ChannelReportColumnModel implements Serializable {

        Bill bill;
        List<Bill> bills;
        BillItem billItem;
        PaymentMethod paymentMethod;
        BillType billType;
        double value;
        double doctorFee;
        double hospitalFee;
        double scanFee;
        double tax;
        double agencyFee;
        String name;
        String comments;
        int intNo;
        double cashTotal;
        double agentTotal;
        double chequeTotal;
        double slipTotal;
        double creditTotal;
        double cardTotal;
        double staffTotal;
        double onCallTotal;
        double collectionTotal;
        //for channell cashier report
        double billedTotal;
        double refundTotal;
        double cancellTotal;
        double total;

        public ChannelReportColumnModel() {
        }

        public List<Bill> getBills() {
            return bills;
        }

        public void setBills(List<Bill> bills) {
            this.bills = bills;
        }

        public double getCollectionTotal() {
            return collectionTotal;
        }

        public void setCollectionTotal(double collectionTotal) {
            this.collectionTotal = collectionTotal;
        }

        public double getCashTotal() {
            return cashTotal;
        }

        public void setCashTotal(double cashTotal) {
            this.cashTotal = cashTotal;
        }

        public double getAgentTotal() {
            return agentTotal;
        }

        public void setAgentTotal(double agentTotal) {
            this.agentTotal = agentTotal;
        }

        public double getChequeTotal() {
            return chequeTotal;
        }

        public void setChequeTotal(double chequeTotal) {
            this.chequeTotal = chequeTotal;
        }

        public double getSlipTotal() {
            return slipTotal;
        }

        public void setSlipTotal(double slipTotal) {
            this.slipTotal = slipTotal;
        }

        public double getCreditTotal() {
            return creditTotal;
        }

        public void setCreditTotal(double creditTotal) {
            this.creditTotal = creditTotal;
        }

        public double getCardTotal() {
            return cardTotal;
        }

        public void setCardTotal(double cardTotal) {
            this.cardTotal = cardTotal;
        }

        public double getStaffTotal() {
            return staffTotal;
        }

        public void setStaffTotal(double staffTotal) {
            this.staffTotal = staffTotal;
        }

        public double getOnCallTotal() {
            return onCallTotal;
        }

        public void setOnCallTotal(double onCallTotal) {
            this.onCallTotal = onCallTotal;
        }

        public double getBilledTotal() {
            return billedTotal;
        }

        public void setBilledTotal(double billedTotal) {
            this.billedTotal = billedTotal;
        }

        public double getRefundTotal() {
            return refundTotal;
        }

        public void setRefundTotal(double refundTotal) {
            this.refundTotal = refundTotal;
        }

        public double getCancellTotal() {
            return cancellTotal;
        }

        public void setCancellTotal(double cancellTotal) {
            this.cancellTotal = cancellTotal;
        }

        public double getTotal() {
            return total;
        }

        public void setTotal(double total) {
            this.total = total;
        }

        public int getIntNo() {
            return intNo;
        }

        public void setIntNo(int intNo) {
            this.intNo = intNo;
        }

        public Bill getBill() {
            return bill;
        }

        public void setBill(Bill bill) {
            this.bill = bill;
        }

        public BillItem getBillItem() {
            return billItem;
        }

        public void setBillItem(BillItem billItem) {
            this.billItem = billItem;
        }

        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public BillType getBillType() {
            return billType;
        }

        public void setBillType(BillType billType) {
            this.billType = billType;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public double getDoctorFee() {
            return doctorFee;
        }

        public void setDoctorFee(double doctorFee) {
            this.doctorFee = doctorFee;
        }

        public double getHospitalFee() {
            return hospitalFee;
        }

        public void setHospitalFee(double hospitalFee) {
            this.hospitalFee = hospitalFee;
        }

        public double getScanFee() {
            return scanFee;
        }

        public void setScanFee(double scanFee) {
            this.scanFee = scanFee;
        }
        
        

        public double getTax() {
            return tax;
        }

        public void setTax(double tax) {
            this.tax = tax;
        }

        public double getAgencyFee() {
            return agencyFee;
        }

        public void setAgencyFee(double agencyFee) {
            this.agencyFee = agencyFee;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getComments() {
            return comments;
        }

        public void setComments(String comments) {
            this.comments = comments;
        }

//        private void calTot() {
//            cashTotal = staffTotal = onCallTotal = agentTotal = 0.0;
//            for (Bill b : bills) {
//                if (b.getPaymentMethod() == PaymentMethod.Cash) {
//                    setCashTotal(getCashTotal() + b.getNetTotal());
//                } else if (b.getPaymentMethod() == PaymentMethod.OnCall) {
//                    setOnCallTotal(getOnCallTotal() + b.getNetTotal());
//                } else if (b.getPaymentMethod() == PaymentMethod.Agent) {
//                    setAgentTotal(getAgentTotal() + b.getNetTotal());
//                } else if (b.getPaymentMethod() == PaymentMethod.Staff) {
//                    setStaffTotal(getStaffTotal() + b.getNetTotal());
//                }
//
//            }
//
//        }
    }

    public class ChannelBillTotals {

        String name;
        List<Bill> bills;
        double cash;
        double agent;
        double staff;
        double onCall;
        double total;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Bill> getBills() {
            return bills;
        }

        public void setBills(List<Bill> bills) {
            this.bills = bills;
        }

        public double getCash() {
            return cash;
        }

        public void setCash(double cash) {
            this.cash = cash;
        }

        public double getAgent() {
            return agent;
        }

        public void setAgent(double agent) {
            this.agent = agent;
        }

        public double getStaff() {
            return staff;
        }

        public void setStaff(double staff) {
            this.staff = staff;
        }

        public double getOnCall() {
            return onCall;
        }

        public void setOnCall(double onCall) {
            this.onCall = onCall;
        }

    }

    public class DepartmentBill {

        Department billDepartment;
        List<Bill> bills;
        double departmentBillTotal;

        public Department getBillDepartment() {
            return billDepartment;
        }

        public void setBillDepartment(Department billDepartment) {
            this.billDepartment = billDepartment;
        }

        public List<Bill> getBills() {
            return bills;
        }

        public void setBills(List<Bill> bills) {
            this.bills = bills;
        }

        public double getDepartmentBillTotal() {
            return departmentBillTotal;
        }

        public void setDepartmentBillTotal(double departmentBillTotal) {
            this.departmentBillTotal = departmentBillTotal;
        }

    }

    public class FeetypeFee {

        List<BillFee> billedBillFees;
        List<BillFee> canceledBillFees;
        List<BillFee> refunBillFees;
        FeeType feeType;
        double billedBillFeeTypeTotal;
        double canceledBillFeeTypeTotal;
        double refundBillFeeTypeTotal;

        public List<BillFee> getBilledBillFees() {
            return billedBillFees;
        }

        public void setBilledBillFees(List<BillFee> billedBillFees) {
            this.billedBillFees = billedBillFees;
        }

        public List<BillFee> getCanceledBillFees() {
            return canceledBillFees;
        }

        public void setCanceledBillFees(List<BillFee> canceledBillFees) {
            this.canceledBillFees = canceledBillFees;
        }

        public List<BillFee> getRefunBillFees() {
            return refunBillFees;
        }

        public void setRefunBillFees(List<BillFee> refunBillFees) {
            this.refunBillFees = refunBillFees;
        }

        public FeeType getFeeType() {
            return feeType;
        }

        public void setFeeType(FeeType feeType) {
            this.feeType = feeType;
        }

        public double getBilledBillFeeTypeTotal() {
            return billedBillFeeTypeTotal;
        }

        public void setBilledBillFeeTypeTotal(double billedBillFeeTypeTotal) {
            this.billedBillFeeTypeTotal = billedBillFeeTypeTotal;
        }

        public double getCanceledBillFeeTypeTotal() {
            return canceledBillFeeTypeTotal;
        }

        public void setCanceledBillFeeTypeTotal(double canceledBillFeeTypeTotal) {
            this.canceledBillFeeTypeTotal = canceledBillFeeTypeTotal;
        }

        public double getRefundBillFeeTypeTotal() {
            return refundBillFeeTypeTotal;
        }

        public void setRefundBillFeeTypeTotal(double refundBillFeeTypeTotal) {
            this.refundBillFeeTypeTotal = refundBillFeeTypeTotal;
        }

    }

    public class AgentHistoryWithDate {

        Date date;
        List<AgentHistory> ahs;

        public AgentHistoryWithDate() {
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public List<AgentHistory> getAhs() {
            return ahs;
        }

        public void setAhs(List<AgentHistory> ahs) {
            this.ahs = ahs;
        }
    }

    public class BookingCountSummryRow {

        String bookingType;
        Staff consultant;
        double billedCount;
        double cancelledCount;
        double refundCount;
        double cashCount;
        double agentCount;
        double oncallCount;
        double staffCount;
        double bookingCount;

        public String getBookingType() {
            return bookingType;
        }

        public void setBookingType(String bookingType) {
            this.bookingType = bookingType;
        }

        public double getBilledCount() {
            return billedCount;
        }

        public void setBilledCount(double billedCount) {
            this.billedCount = billedCount;
        }

        public double getCancelledCount() {
            return cancelledCount;
        }

        public void setCancelledCount(double cancelledCount) {
            this.cancelledCount = cancelledCount;
        }

        public double getRefundCount() {
            return refundCount;
        }

        public void setRefundCount(double refundCount) {
            this.refundCount = refundCount;
        }

        public double getCashCount() {
            return cashCount;
        }

        public void setCashCount(double cashCount) {
            this.cashCount = cashCount;
        }

        public double getAgentCount() {
            return agentCount;
        }

        public void setAgentCount(double agentCount) {
            this.agentCount = agentCount;
        }

        public double getOncallCount() {
            return oncallCount;
        }

        public void setOncallCount(double oncallCount) {
            this.oncallCount = oncallCount;
        }

        public double getStaffCount() {
            return staffCount;
        }

        public void setStaffCount(double staffCount) {
            this.staffCount = staffCount;
        }

        public double getBookingCount() {
            return bookingCount;
        }

        public void setBookingCount(double bookingCount) {
            this.bookingCount = bookingCount;
        }

        public Staff getConsultant() {
            return consultant;
        }

        public void setConsultant(Staff consultant) {
            this.consultant = consultant;
        }

    }

    public class DoctorPaymentSummeryRowSub {

        Date date;
        List<Bill> bills;
        ServiceSession serviceSession;
        List<ServiceSession> serviceSessions;
        Staff consultant;
        double hospitalFeeTotal;
        double staffFeeTotal;

        double cashCount;
        double onCallCount;
        double staffCount;
        double agentCount;
        double totalCount;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public List<Bill> getBills() {
            return bills;
        }

        public void setBills(List<Bill> bills) {
            this.bills = bills;
        }

        public ServiceSession getServiceSession() {
            return serviceSession;
        }

        public void setServiceSession(ServiceSession serviceSession) {
            this.serviceSession = serviceSession;
        }

        public List<ServiceSession> getServiceSessions() {
            return serviceSessions;
        }

        public void setServiceSessions(List<ServiceSession> serviceSessions) {
            this.serviceSessions = serviceSessions;
        }

        public Staff getConsultant() {
            return consultant;
        }

        public void setConsultant(Staff consultant) {
            this.consultant = consultant;
        }

        public double getHospitalFeeTotal() {
            return hospitalFeeTotal;
        }

        public void setHospitalFeeTotal(double hospitalFeeTotal) {
            this.hospitalFeeTotal = hospitalFeeTotal;
        }

        public double getStaffFeeTotal() {
            return staffFeeTotal;
        }

        public void setStaffFeeTotal(double staffFeeTotal) {
            this.staffFeeTotal = staffFeeTotal;
        }

        public double getCashCount() {
            return cashCount;
        }

        public void setCashCount(double cashCount) {
            this.cashCount = cashCount;
        }

        public double getOnCallCount() {
            return onCallCount;
        }

        public void setOnCallCount(double onCallCount) {
            this.onCallCount = onCallCount;
        }

        public double getStaffCount() {
            return staffCount;
        }

        public void setStaffCount(double staffCount) {
            this.staffCount = staffCount;
        }

        public double getAgentCount() {
            return agentCount;
        }

        public void setAgentCount(double agentCount) {
            this.agentCount = agentCount;
        }

        public double getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(double totalCount) {
            this.totalCount = totalCount;
        }

    }

    public class DoctorPaymentSummeryRow {

        Staff consultant;
        List<DoctorPaymentSummeryRowSub> doctorPaymentSummeryRowSubs;
        Date date;

        public Staff getConsultant() {
            return consultant;
        }

        public void setConsultant(Staff consultant) {
            this.consultant = consultant;
        }

        public List<DoctorPaymentSummeryRowSub> getDoctorPaymentSummeryRowSubs() {
            return doctorPaymentSummeryRowSubs;
        }

        public void setDoctorPaymentSummeryRowSubs(List<DoctorPaymentSummeryRowSub> doctorPaymentSummeryRowSubs) {
            this.doctorPaymentSummeryRowSubs = doctorPaymentSummeryRowSubs;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
        
        

    }

}
