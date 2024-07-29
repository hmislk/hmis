/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.hr.StaffController;
import com.divudi.data.ApplicationInstitution;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.DoctorDayChannelCount;
import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.PersonInstitutionType;
import com.divudi.data.ReportTemplateRow;
import com.divudi.data.ReportTemplateRowBundle;
import com.divudi.data.WeekdayDisplay;
import com.divudi.data.channel.DateEnum;
import com.divudi.data.channel.PaymentEnum;
import com.divudi.data.dataStructure.BillsTotals;
import com.divudi.data.dataStructure.ChannelDoctor;
import com.divudi.data.dataStructure.WebUserBillsTotal;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.data.table.String1Value1;
import com.divudi.data.table.String1Value3;
import com.divudi.ejb.ChannelBean;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.Area;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.channel.ArrivalRecord;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.ArrivalRecordFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.WebUserFacade;
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
import java.util.Objects;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.hl7.fhir.r5.model.Bundle;

@Named
@SessionScoped
public class ChannelReportTemplateController implements Serializable {

    private ServiceSession serviceSession;
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
    private List<AvalabelChannelDoctorRow> acdrs;// created 2016.8.10

    List<DoctorDayChannelCount> doctorDayChannelCounts;
    private List<WeekdayDisplay> weekdayDisplays;

    double netTotal;
    double netTotalDoc;
    double cancelTotal;
    double refundTotal;
    double totalBilled;
    double totalCancel;
    double totalRefundDoc;
    double totalBilledDoc;
    double totalCancelDoc;
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
    List<String1Value3> valueList;
    ReportKeyWord reportKeyWord;
    Date fromDate;
    Date toDate;
    Date date;
    Institution institution;
    private Category category;
    WebUser webUser;
    Staff staff;
    ChannelBillTotals billTotals;
    Department department;
    boolean paid = false;
    boolean summery = false;
    boolean sessoinDate = false;
    boolean withDates = false;
    boolean agncyOnCall = false;
    boolean agncy = false;
    boolean showPatient = false;
    PaymentMethod paymentMethod;
    ChannelTotal channelTotal;

    /////
    private List<ChannelDoctor> channelDoctors;
    List<AgentHistory> agentHistorys;
    List<AgentHistoryWithDate> agentHistoryWithDate;
    List<BookingCountSummryRow> bookingCountSummryRows;
    List<BookingCountSummryRow> bookingCountSummryRowsScan;
    List<DoctorPaymentSummeryRow> doctorPaymentSummeryRows;
    List<Bill> channelBills;
    List<Bill> channelBillsCancelled;
    List<Bill> channelBillsRefunded;
    List<ArrivalRecord> arrivalRecords;
    List<StaffBookingWithCount> staffBookingWithCounts;
    List<AreaWithCount> areaWithCount;
    List<StaffWithAreaRow> staffWithAreaRows;
    CommonFunctions commonFunctions;
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
    CommonController commonController;
    @Inject
    StaffController staffController;
    @Inject
    BookingController bookingController;
    @Inject
    BookingPastController bookingPastController;
    @Inject
    WebUserController webUserController;

    @EJB
    DepartmentFacade departmentFacade;

    @EJB
    StaffFacade staffFacade;
    @EJB
    ServiceSessionFacade serviceSessionFacade;
    @EJB
    ArrivalRecordFacade arrivalRecordFacade;

    public void clearAll() {
        billedBills = new ArrayList<>();
        cancelBills = new ArrayList<>();
        refundBills = new ArrayList<>();
        netTotal = 0.0;
        cancelTotal = 0.0;
        refundTotal = 0.0;
        totalBilled = 0.0;
        totalCancel = 0.0;
        totalRefund = 0.0;
        staff = null;
        sessoinDate = false;
        institution = null;
        date = null;
        bookingCountSummryRows = new ArrayList<>();
        bookingCountSummryRowsScan = new ArrayList<>();
    }

    public String navigateToChannlingSessionCount() {
        bundle = new ReportTemplateRowBundle();
        return "/channel/reports/daily_session_counts?faces-redirect=true;";
    }

    public String navigateToChannlingDoctorCount() {
        bundle = new ReportTemplateRowBundle();
        return "/channel/reports/daily_doctor_counts?faces-redirect=true;";
    }

    public String navigateToCategorySessionCount() {
        bundle = new ReportTemplateRowBundle();
        return "/channel/reports/category_session_counts?faces-redirect=true;";
    }

    //ToDo : Pubudu
    public String navigateToOnlineBookings() {
        bundle = new ReportTemplateRowBundle();
        return "/channel/reports/category_session_counts?faces-redirect=true;";
    }

    public void clearWithDefultValue() {
        getReportKeyWord().setFrom(45.0);
    }

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

    public void fillIncomeWithAgentBookings() {
        Date startTime = new Date();

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

    private ReportTemplateRowBundle bundle;

    public void fillDailySessionCounts() {
        bundle = new ReportTemplateRowBundle();
        String j;
        Map m = new HashMap();
        rows = new ArrayList<>();

        j = "select new com.divudi.data.ReportTemplateRow(si) "
                + " from SessionInstance si "
                + " where si.retired=false "
                + " and si.sessionDate between :fd and :td ";

        if (institution != null) {
            m.put("ins", institution);
            j += " and si.institution=:ins ";
        }

        boolean test = false;
        if (test) {
            SessionInstance si = new SessionInstance();
            si.getSessionDate();
            si.getInstitution();
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) billFacade.findLightsByJpql(j, m, TemporalType.DATE);

        bundle.setReportTemplateRows(rs);
        bundle.setLong1(0l);
        bundle.setLong2(0l);
        bundle.setLong3(0l);
        bundle.setLong4(0l);
        bundle.setLong5(0l);
        bundle.setLong6(0l);

        long idCounter = 1;

        for (ReportTemplateRow row : rs) {
            if (row != null) {
                row.setId(idCounter++);
                SessionInstance sessionInstance = row.getSessionInstance();
                if (sessionInstance != null) {
                    bundle.setLong1(bundle.getLong1() + (sessionInstance.getBookedPatientCount() != null ? sessionInstance.getBookedPatientCount() : 0));
                    bundle.setLong2(bundle.getLong2() + (sessionInstance.getPaidPatientCount() != null ? sessionInstance.getPaidPatientCount() : 0));
                    bundle.setLong3(bundle.getLong3() + (sessionInstance.getCompletedPatientCount() != null ? sessionInstance.getCompletedPatientCount() : 0));
                    bundle.setLong4(bundle.getLong4() + (sessionInstance.getCancelPatientCount() != null ? sessionInstance.getCancelPatientCount() : 0));
                    bundle.setLong5(bundle.getLong5() + (sessionInstance.getRefundedPatientCount() != null ? sessionInstance.getRefundedPatientCount() : 0));
                    bundle.setLong6(bundle.getLong6() + (sessionInstance.getRemainingPatientCount() != null ? sessionInstance.getRemainingPatientCount() : 0));
                }
            }
        }

    }

    public void fillCategorySessionCounts() {
        bundle = new ReportTemplateRowBundle();
        String j;
        Map m = new HashMap();
        rows = new ArrayList<>();

        j = "select new com.divudi.data.ReportTemplateRow(pubudu) "
                + " from SessionInstance pubudu "
                + " where pubudu.retired=false "
                + " and pubudu.sessionDate between :fd and :td ";

        if (institution != null) {
            m.put("ins", institution);
            j += " and pubudu.institution=:ins ";
        }

        if (category != null) {
            m.put("cat", category);
            j += " and pubudu.originatingSession.category=:cat ";
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) billFacade.findLightsByJpql(j, m, TemporalType.DATE);

        bundle.setReportTemplateRows(rs);

        bundle.setLong1(0l);
        bundle.setLong2(0l);
        bundle.setLong3(0l);
        bundle.setLong4(0l);
        bundle.setLong5(0l);
        bundle.setLong6(0l);

        long idCounter = 1;

        for (ReportTemplateRow row : rs) {
            if (row != null) {
                row.setId(idCounter++);
                SessionInstance sessionInstance = row.getSessionInstance();
                if (sessionInstance != null) {
                    bundle.setLong1(bundle.getLong1() + (sessionInstance.getBookedPatientCount() != null ? sessionInstance.getBookedPatientCount() : 0));
                    bundle.setLong2(bundle.getLong2() + (sessionInstance.getPaidPatientCount() != null ? sessionInstance.getPaidPatientCount() : 0));
                    bundle.setLong3(bundle.getLong3() + (sessionInstance.getCompletedPatientCount() != null ? sessionInstance.getCompletedPatientCount() : 0));
                    bundle.setLong4(bundle.getLong4() + (sessionInstance.getCancelPatientCount() != null ? sessionInstance.getCancelPatientCount() : 0));
                    bundle.setLong5(bundle.getLong5() + (sessionInstance.getRefundedPatientCount() != null ? sessionInstance.getRefundedPatientCount() : 0));
                    bundle.setLong6(bundle.getLong6() + (sessionInstance.getRemainingPatientCount() != null ? sessionInstance.getRemainingPatientCount() : 0));
                }
            }
        }

    }

    //To Do - Pubudu
    public void fillOnlineBookings() {
        bundle = new ReportTemplateRowBundle();
        String j;
        Map m = new HashMap();
        rows = new ArrayList<>();
        //BillSession
        boolean test = false;
        if(test){
            BillSession bs = new BillSession();
            bs.getSessionInstance().getSessionDate();
            if(bs.getBill().getBillTypeAtomic()==BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT_ONLINE ){
                
            }
        }
      
        j = "select new com.divudi.data.ReportTemplateRow(pubudu) "
                + " from SessionInstance pubudu "
                + " where pubudu.retired=false "
                + " and pubudu.sessionDate between :fd and :td ";

        if (institution != null) {
            m.put("ins", institution);
            j += " and pubudu.institution=:ins ";
        }

        if (category != null) {
            m.put("cat", category);
            j += " and pubudu.originatingSession.category=:cat ";
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) billFacade.findLightsByJpql(j, m, TemporalType.DATE);

        bundle.setReportTemplateRows(rs);

        bundle.setLong1(0l);
        bundle.setLong2(0l);
        bundle.setLong3(0l);
        bundle.setLong4(0l);
        bundle.setLong5(0l);
        bundle.setLong6(0l);

        long idCounter = 1;

        for (ReportTemplateRow row : rs) {
            if (row != null) {
                row.setId(idCounter++);
                SessionInstance sessionInstance = row.getSessionInstance();
                if (sessionInstance != null) {
                    bundle.setLong1(bundle.getLong1() + (sessionInstance.getBookedPatientCount() != null ? sessionInstance.getBookedPatientCount() : 0));
                    bundle.setLong2(bundle.getLong2() + (sessionInstance.getPaidPatientCount() != null ? sessionInstance.getPaidPatientCount() : 0));
                    bundle.setLong3(bundle.getLong3() + (sessionInstance.getCompletedPatientCount() != null ? sessionInstance.getCompletedPatientCount() : 0));
                    bundle.setLong4(bundle.getLong4() + (sessionInstance.getCancelPatientCount() != null ? sessionInstance.getCancelPatientCount() : 0));
                    bundle.setLong5(bundle.getLong5() + (sessionInstance.getRefundedPatientCount() != null ? sessionInstance.getRefundedPatientCount() : 0));
                    bundle.setLong6(bundle.getLong6() + (sessionInstance.getRemainingPatientCount() != null ? sessionInstance.getRemainingPatientCount() : 0));
                }
            }
        }

    }


    public void fillDailyDoctorCounts() {
        bundle = new ReportTemplateRowBundle();
        String j;
        Map m = new HashMap();
        rows = new ArrayList<>();

        j = "select new com.divudi.data.ReportTemplateRow(si.originatingSession.staff, "
                + "sum(si.bookedPatientCount), "
                + "sum(si.paidPatientCount),"
                + "sum(si.completedPatientCount),"
                + "sum(si.cancelPatientCount),"
                + "sum(si.refundedPatientCount),"
                + "sum(si.remainingPatientCount)"
                + ") "
                + " from SessionInstance si "
                + " where si.retired=false "
                + " and si.sessionDate between :fd and :td ";

        if (institution != null) {
            m.put("ins", institution);
            j += " and si.institution=:ins ";
        }

        j += "group by si.originatingSession.staff ";
        j += "order by si.originatingSession.staff.person.name ";
        boolean test = false;
        if (test) {
            SessionInstance si = new SessionInstance();
            si.getSessionDate();
            si.getInstitution();
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) billFacade.findLightsByJpql(j, m, TemporalType.DATE);

        bundle.setReportTemplateRows(rs);
        bundle.setLong1(0l);
        bundle.setLong2(0l);
        bundle.setLong3(0l);
        bundle.setLong4(0l);
        bundle.setLong5(0l);
        bundle.setLong6(0l);

        long idCounter = 1;

        for (ReportTemplateRow row : rs) {
            if (row != null) {
                row.setId(idCounter++);
                SessionInstance sessionInstance = row.getSessionInstance();
                if (sessionInstance != null) {
                    bundle.setLong1(bundle.getLong1() + (row.getLong1() != null ? row.getLong1() : 0));
                    bundle.setLong2(bundle.getLong2() + (sessionInstance.getPaidPatientCount() != null ? sessionInstance.getPaidPatientCount() : 0));
                    bundle.setLong3(bundle.getLong3() + (sessionInstance.getCompletedPatientCount() != null ? sessionInstance.getCompletedPatientCount() : 0));
                    bundle.setLong4(bundle.getLong4() + (sessionInstance.getCancelPatientCount() != null ? sessionInstance.getCancelPatientCount() : 0));
                    bundle.setLong5(bundle.getLong5() + (sessionInstance.getRefundedPatientCount() != null ? sessionInstance.getRefundedPatientCount() : 0));
                    bundle.setLong6(bundle.getLong6() + (sessionInstance.getRemainingPatientCount() != null ? sessionInstance.getRemainingPatientCount() : 0));
                }
            }
        }

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
        Date startTime = new Date();

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
        Date startTime = new Date();

        billedBills = new ArrayList<>();
        cancelBills = new ArrayList<>();
        refundBills = new ArrayList<>();

        billedBills = channelListByBillClass(new BilledBill(), webUser, sessoinDate, institution, agncyOnCall);
        cancelBills = channelListByBillClass(new CancelledBill(), webUser, sessoinDate, institution, agncyOnCall);
        refundBills = channelListByBillClass(new RefundBill(), webUser, sessoinDate, institution, agncyOnCall);

        totalBilled = calTotal(billedBills);
        totalCancel = calTotal(cancelBills);
        totalRefund = calTotal(refundBills);
        totalBilledDoc = calTotalDoc(billedBills);
        totalCancelDoc = calTotalDoc(cancelBills);
        totalRefundDoc = calTotalDoc(refundBills);
        netTotal = totalBilled + totalCancel + totalRefund;
        netTotalDoc = totalBilledDoc + totalCancelDoc + totalRefundDoc;

    }

    public void channelBillClassListByPaymentMethord() {
        Date startTime = new Date();

        if (webUser == null && !webUserController.hasPrivilege("Developers")) {
            JsfUtil.addErrorMessage("Select User......");
            return;
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Select Payment Methord.....");
            return;
        }
        billedBills = new ArrayList<>();
        cancelBills = new ArrayList<>();
        refundBills = new ArrayList<>();
        BillType bt = null;
        switch (paymentMethod) {
            case Cash:
                bt = BillType.ChannelCash;
                break;
            case Cheque:
                bt = BillType.ChannelCash;
                break;
            case Slip:
                bt = BillType.ChannelCash;
                break;
            case Card:
                bt = BillType.ChannelCash;
                break;
            case Agent:
                bt = BillType.ChannelAgent;
                break;
            case OnCall:
                bt = BillType.ChannelPaid;
                break;
            case Staff:
                bt = BillType.ChannelPaid;
                break;
        }
        billedBills = fetchBills(new BilledBill(), bt, paymentMethod, webUser, fromDate, toDate);
        cancelBills = fetchBills(new CancelledBill(), bt, paymentMethod, webUser, fromDate, toDate);
        refundBills = fetchBills(new RefundBill(), bt, paymentMethod, webUser, fromDate, toDate);

        totalBilled = calTotal(billedBills);
        totalCancel = calTotal(cancelBills);
        totalRefund = calTotal(refundBills);
        totalBilledDoc = calTotalDoc(billedBills);
        totalCancelDoc = calTotalDoc(cancelBills);
        totalRefundDoc = calTotalDoc(refundBills);
        netTotal = totalBilled + totalCancel + totalRefund;
        netTotalDoc = totalBilledDoc + totalCancelDoc + totalRefundDoc;

    }

    public List<Bill> channelListByBillClass(Bill bill, WebUser webUser, boolean sd, Institution agent, boolean crditAgent) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        HashMap hm = new HashMap();

        String sql = " select b from Bill b "
                + " where b.retired=false "
                + " and type(b)=:class ";

        if (webUser != null) {
            sql += " and b.creater=:web ";
            hm.put("web", webUser);
        }
        if (sd) {
            sql += " and b.singleBillSession.sessionDate between :fd and :td ";
        } else {
            sql += " and b.createdAt between :fd and :td ";
        }
        if (crditAgent) {
            if (agent != null) {
                sql += " and b.creditCompany=:a ";
                hm.put("a", agent);
            } else {
                sql += " and b.creditCompany is not null ";
            }
            sql += " and b.billType=:bt ";
            hm.put("bt", BillType.ChannelOnCall);
        } else {
            sql += " and b.billType in :bts ";
            hm.put("bts", bts);
        }
        if (getReportKeyWord().getBillType() != null) {
            sql += " and b.singleBillSession.serviceSession.originatingSession.forBillType=:bt ";
            hm.put("bt", getReportKeyWord().getBillType());
        }
//        sql += " order by b.singleBillSession.sessionDate ";

        hm.put("class", bill.getClass());
        hm.put("fd", getFromDate());
        hm.put("td", getToDate());
        return billFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    private List<Bill> fetchBills(Bill billClass, BillType bt, PaymentMethod paymentMethod, WebUser wUser, Date fd, Date td) {
        String sql;
        Map temMap = new HashMap();

        sql = "SELECT b FROM Bill b WHERE b.retired=false "
                + " and b.billType=:btp "
                + " and type(b)=:bill "
                + " and b.institution=:ins ";

        if (billClass.getClass().equals(BilledBill.class)) {
            sql += " and b.singleBillSession.sessionDate between :fromDate and :toDate ";
        }
        if (billClass.getClass().equals(CancelledBill.class)) {
            sql += " and b.createdAt between :fromDate and :toDate ";
        }
        if (billClass.getClass().equals(RefundBill.class)) {
            sql += " and b.createdAt between :fromDate and :toDate ";
        }

        if (wUser != null) {
            sql += " and b.creater=:w ";
            temMap.put("w", wUser);
        }

        if (paymentMethod == PaymentMethod.OnCall || paymentMethod == PaymentMethod.Staff || paymentMethod == PaymentMethod.Agent) {
            if (billClass instanceof BilledBill) {
                if (paymentMethod == PaymentMethod.Agent) {
                    sql += " and b.paymentMethod=:pm ";
                    temMap.put("pm", paymentMethod);
                } else {
                    sql += " and b.referenceBill.paymentMethod=:pm ";
                    temMap.put("pm", paymentMethod);
                }
            } else if (paymentMethod == PaymentMethod.Agent) {
                sql += " and b.billedBill.paymentMethod=:pm ";
                temMap.put("pm", paymentMethod);
            } else {
                sql += " and b.billedBill.referenceBill.paymentMethod=:pm ";
                temMap.put("pm", paymentMethod);
            }

        } else {
            sql += " and b.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (getReportKeyWord().getBillType() != null) {
            sql += " and b.singleBillSession.serviceSession.originatingSession.forBillType=:bt ";
            temMap.put("bt", getReportKeyWord().getBillType());
        }

        temMap.put("fromDate", fd);
        temMap.put("toDate", td);
        temMap.put("btp", bt);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bill", billClass.getClass());

        sql += " order by b.insId ";
        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    List<Department> deps;
    List<DepartmentBill> depBills;

    double departmentBilledBillTotal;
    double departmentCanceledBillTotal;
    double departmentRefundBillTotal;

    public void createDepartmentBills() {
        Date startTime = new Date();

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

    public double calTotalDoc(List<Bill> bills) {

        double departmentTotal = 0.0;
        for (Bill bill : bills) {
            departmentTotal += bill.getStaffFee();
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

        getBilledBillList().setBills(createUserBills(new BilledBill(), getWebUser(), getDepartment()));
        getCanceledBillList().setBills(createUserBills(new CancelledBill(), getWebUser(), getDepartment()));
        getRefundBillList().setBills(createUserBills(new RefundBill(), getWebUser(), getDepartment()));

        getBilledBillList().setAgent(calTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Agent));
        getCanceledBillList().setAgent(calTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Agent));
        getRefundBillList().setAgent(calTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Agent));

        getBilledBillList().setCash(calTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Cash));
        getCanceledBillList().setCash(calTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Cash));
        getRefundBillList().setCash(calTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Cash));

        getBilledBillList().setCard(calTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Card));
        getCanceledBillList().setCard(calTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Card));
        getRefundBillList().setCard(calTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Card));

        getBilledBillList().setSlip(calTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Slip));
        getCanceledBillList().setSlip(calTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Slip));
        getRefundBillList().setSlip(calTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Slip));

        getBilledBillList().setCheque(calTotal(new BilledBill(), getWebUser(), getDepartment(), PaymentMethod.Cheque));
        getCanceledBillList().setCheque(calTotal(new CancelledBill(), getWebUser(), getDepartment(), PaymentMethod.Cheque));
        getRefundBillList().setCheque(calTotal(new RefundBill(), getWebUser(), getDepartment(), PaymentMethod.Cheque));

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

    public List<Bill> createUserBills(Bill billClass, WebUser webUser, Department department) {

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid};
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

    public double calTotal(Bill billClass, WebUser wUser, Department department, PaymentMethod paymentMethod) {

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid};
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
        Date startTime = new Date();

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
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid};
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
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.singleBillSession.sessionDate between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }
        } else {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }

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

    public double countBillByBillTypeAndFeeType(Bill bill, FeeType ft, BillType bt, boolean sessoinDate, boolean paid, boolean onlineAgent) {

        String sql;
        Map m = new HashMap();

        sql = " select count(distinct(bf.bill)) from BillFee  bf where "
                + " bf.bill.retired=false "
                + " and bf.bill.billType=:bt "
                + " and type(bf.bill)=:class "
                + " and bf.fee.feeType =:ft "
                + " and bf.feeValue>0 ";

        if (bt == BillType.ChannelAgent) {
            if (onlineAgent) {
                sql += " and bf.bill.creditCompany.id=20385287 ";
            } else {
                sql += " and bf.bill.creditCompany.id!=20385287 ";
            }
        }

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
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.singleBillSession.sessionDate between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }
        } else {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }

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
//        if (bill.getClass().equals(RefundBill.class)) {
//            sql += " and bf.bill.refunded=true";
//            System.err.println("Refund");
//        }

        if (fts == FeeType.OwnInstitution) {
            sql += " and bf.fee.name =:fn ";
            m.put("fn", "Hospital Fee");
        }

        if (paid) {
            sql += " and bf.bill.paidBill is not null "
                    + " and bf.bill.paidAmount!=0 ";
        }
        if (sessoinDate) {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.singleBillSession.sessionDate between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }
        } else {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }

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
                    d += (bf.getFeeValue() + bf.getFeeVat());
                }
            }
        }
        return d;
    }

    public double[] hospitalTotalBillByBillTypeAndFeeTypeWithdocfee(Bill bill, FeeType fts, BillType bt, boolean sessoinDate, boolean paid) {

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

        if (fts == FeeType.OwnInstitution) {
            sql += " and bf.fee.name =:fn ";
            m.put("fn", "Hospital Fee");
        }

        if (paid) {
            sql += " and bf.bill.paidBill is not null "
                    + " and bf.bill.paidAmount!=0 ";
        }
        if (sessoinDate) {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.singleBillSession.sessionDate between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }
        } else {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }

        }

        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("class", BilledBill.class);
        m.put("ft", fts);
        m.put("bt", bt);
//        m.put("fn", "Scan Fee");

        List<Bill> b = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        double[] d = new double[4];
        d[0] = 0.0;
        d[1] = 0.0;
        d[2] = 0.0;
        d[3] = 0.0;
        for (Bill b1 : b) {
            for (BillFee bf : b1.getBillFees()) {
                if (bf.getFee().getFeeType() != FeeType.Staff) {
                    if (!bill.getClass().equals(RefundBill.class)) {
                        d[0] += bf.getFeeValue();
                        d[1] += bf.getFeeVat();
                    }
                } else {
                    d[2] += bf.getFeeValue();
                    d[3] += bf.getFeeVat();
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

        sql = " select count(distinct(b)) from Bill  b  "
                + " where b.retired=false ";

        if (sessoinDate) {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and b.singleBillSession.sessionDate between :fd and :td "
                        + " and b.paidBill is not null "
                        + " and b.paidAmount!=0 ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and b.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and b.createdAt between :fd and :td ";
            }
        } else {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and b.createdAt between :fd and :td "
                        + " and b.paidBill is not null "
                        + " and b.paidAmount!=0 ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and b.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and b.createdAt between :fd and :td ";
            }

        }

        if (bt != null) {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and b.billType=:bt ";
                m.put("bt", bt);
            }
            if (bill.getClass().equals(CancelledBill.class) || bill.getClass().equals(RefundBill.class)) {
                if (bt == BillType.ChannelOnCall || bt == BillType.ChannelStaff) {
                    sql += " and b.billedBill.referenceBill.billType=:bt ";
                    m.put("bt", bt);
                } else {
                    sql += " and b.billType=:bt ";
                    m.put("bt", bt);
                }

            }
        }

        if (bill != null) {
            sql += " and type(b)=:class ";
            m.put("class", bill.getClass());
        }

        if (st != null) {
            sql += " and b.singleBillSession.staff =:stf ";
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
        Date startTime = new Date();

        if (getFeeType() == null) {
            JsfUtil.addErrorMessage("Please Insert Fee Type");
        } else {
            listBilledBillFees = getBillFeeWithFeeTypes(new BilledBill(), getFeeType());
            listCanceledBillFees = getBillFeeWithFeeTypes(new CancelledBill(), getFeeType());
            listRefundBillFees = getBillFeeWithFeeTypes(new RefundBill(), getFeeType());
        }

    }

    public void createChannelDoctorPaymentTable() {
        Date startTime = new Date();
        createChannelDoctorPayment(false);

    }

    public void createChannelDoctorPaymentTableBySession() {
        Date startTime = new Date();
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
        Date startTime = new Date();

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

                //System.out.println("cashCount = " + cashCount);
                dpsrs.setCashCount(cashCount);
                dpsrs.setAgentCount(agentCount);
                dpsrs.setOnCallCount(onCallCount);
                dpsrs.setStaffCount(staffCount);
                //ptCount+=(cashCount + agentCount + onCallCount + staffCount);

            }
            //System.out.println("dpsrs.getCashCount() = " + dpsrs.getCashCount());

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
            //System.out.println("doctorPaymentSummeryRowSub.getDate() = " + doctorPaymentSummeryRowSub.getDate());

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
        Date startTime = new Date();

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
        //System.out.println("Inside getChannelUnPaidBillListbyClassTypes");
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
                //System.out.println("doctorPaymentSummeryRowSub.getDate() = " + doctorPaymentSummeryRowSub.getDate());
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
        //System.out.println("Inside getStaffbyClassType");
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
        Date startTime = new Date();
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
        //System.out.println("Inside getStaffbyClassType");
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
        Date startTime = new Date();
        createAllChannelBillReport(true);

    }

    public void createAllChannelBillReportBySessionDate() {
        Date startTime = new Date();
        createAllChannelBillReport(false);

    }

    public void createAllChannelBillReportForVat() {
        Date startTime = new Date();
        channelBills = new ArrayList<>();
        institution = null;

        List<BillType> bts;
        if (agncyOnCall) {
            bts = Arrays.asList(new BillType[]{BillType.ChannelCash, BillType.ChannelPaid});
        } else {
            bts = Arrays.asList(new BillType[]{BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid});
        }
        if (sessoinDate) {
            channelBills.addAll(channelBillListByBillType(false, new BilledBill(), bts, fromDate, toDate));
            channelBills.addAll(channelBillListByBillType(false, new CancelledBill(), bts, fromDate, toDate));
            channelBills.addAll(channelBillListByBillType(false, new RefundBill(), bts, fromDate, toDate));
            channelTotal = new ChannelTotal();
            channelTotal.setStaffFee(channelBillTotalByBillType(false, new BilledBill(), bts, fromDate, toDate, true, false, false, false)
                    + channelBillTotalByBillType(false, new CancelledBill(), bts, fromDate, toDate, true, false, false, false)
                    + channelBillTotalByBillType(false, new RefundBill(), bts, fromDate, toDate, true, false, false, false));
            channelTotal.setHosFee(channelBillTotalByBillType(false, new BilledBill(), bts, fromDate, toDate, false, true, false, false)
                    + channelBillTotalByBillType(false, new CancelledBill(), bts, fromDate, toDate, false, true, false, false)
                    + channelBillTotalByBillType(false, new RefundBill(), bts, fromDate, toDate, false, true, false, false));
            channelTotal.setNetTotal(channelBillTotalByBillType(false, new BilledBill(), bts, fromDate, toDate, false, false, true, false)
                    + channelBillTotalByBillType(false, new CancelledBill(), bts, fromDate, toDate, false, false, true, false)
                    + channelBillTotalByBillType(false, new RefundBill(), bts, fromDate, toDate, false, false, true, false));
            channelTotal.setVat(channelBillTotalByBillType(false, new BilledBill(), bts, fromDate, toDate, false, false, false, true)
                    + channelBillTotalByBillType(false, new CancelledBill(), bts, fromDate, toDate, false, false, false, true)
                    + channelBillTotalByBillType(false, new RefundBill(), bts, fromDate, toDate, false, false, false, true));
        } else {
            channelBills.addAll(channelBillListByBillType(true, new BilledBill(), bts, fromDate, toDate));
            channelBills.addAll(channelBillListByBillType(true, new CancelledBill(), bts, fromDate, toDate));
            channelBills.addAll(channelBillListByBillType(true, new RefundBill(), bts, fromDate, toDate));
            channelTotal = new ChannelTotal();
            channelTotal.setStaffFee(channelBillTotalByBillType(true, new BilledBill(), bts, fromDate, toDate, true, false, false, false)
                    + channelBillTotalByBillType(true, new CancelledBill(), bts, fromDate, toDate, true, false, false, false)
                    + channelBillTotalByBillType(true, new RefundBill(), bts, fromDate, toDate, true, false, false, false));
            channelTotal.setHosFee(channelBillTotalByBillType(true, new BilledBill(), bts, fromDate, toDate, false, true, false, false)
                    + channelBillTotalByBillType(true, new CancelledBill(), bts, fromDate, toDate, false, true, false, false)
                    + channelBillTotalByBillType(true, new RefundBill(), bts, fromDate, toDate, false, true, false, false));
            channelTotal.setNetTotal(channelBillTotalByBillType(true, new BilledBill(), bts, fromDate, toDate, false, false, true, false)
                    + channelBillTotalByBillType(true, new CancelledBill(), bts, fromDate, toDate, false, false, true, false)
                    + channelBillTotalByBillType(true, new RefundBill(), bts, fromDate, toDate, false, false, true, false));
            channelTotal.setVat(channelBillTotalByBillType(true, new BilledBill(), bts, fromDate, toDate, false, false, false, true)
                    + channelBillTotalByBillType(true, new CancelledBill(), bts, fromDate, toDate, false, false, false, true)
                    + channelBillTotalByBillType(true, new RefundBill(), bts, fromDate, toDate, false, false, false, true));
        }

    }

    public void createAllOPDBillReportForVat() {
        Date startTime = new Date();
        channelBills = new ArrayList<>();

        BillType[] billTypes = {BillType.OpdBill};
        List<BillType> bts = Arrays.asList(billTypes);

        channelBills.addAll(channelBillListByBillType2(true, null, bts, fromDate, toDate));
        channelTotal = new ChannelTotal();
        channelTotal.setStaffFee(channelBillTotalByBillType(true, null, bts, fromDate, toDate, true, false, false, false));
        channelTotal.setHosFee(channelBillTotalByBillType(true, null, bts, fromDate, toDate, false, true, false, false));
        channelTotal.setNetTotal(channelBillTotalByBillType(true, null, bts, fromDate, toDate, false, false, true, false));
        channelTotal.setVat(channelBillTotalByBillType(true, null, bts, fromDate, toDate, false, false, false, true));

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
            tot += (b.getNetTotal() - b.getStaffFee());
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
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and b.singleBillSession.sessionDate between :fDate and :tDate ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and b.createdAt between :fDate and :tDate ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and b.createdAt between :fDate and :tDate ";
            }
        }

        if (!billTypes.isEmpty()) {
            sql += " and b.billType in :bt ";
            hm.put("bt", billTypes);
        }

        if (bill != null) {
            sql += " and type(b)=:class";
            hm.put("class", bill.getClass());
        }

        if (reportKeyWord.getWebUser() != null) {
            sql += " and b.creater=:user";
            hm.put("user", reportKeyWord.getWebUser());
        }

        sql += " order by b.createdAt ";

        hm.put("fDate", fd);
        hm.put("tDate", td);

        return billFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Bill> channelBillListByBillType2(boolean createdDate, Bill bill, List<BillType> billTypes, Date fd, Date td) {

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

        if (institution != null) {
            sql += " and  b.institution=:ins ";
            hm.put("ins", institution);
        }

        sql += " order by b.toDepartment.name ,b.createdAt ";

        hm.put("fDate", fd);
        hm.put("tDate", td);

        return billFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double channelBillTotalByBillType(boolean createdDate, Bill bill, List<BillType> billTypes, Date fd, Date td, boolean staff, boolean hos, boolean net, boolean vat) {

        HashMap hm = new HashMap();
        String sql = "";

        if (vat) {
            sql = " select sum(b.vat) from Bill b "
                    + " where b.retired=false ";
        }
        if (staff) {
            sql = " select sum(b.staffFee) from Bill b "
                    + " where b.retired=false ";
        }
        if (net) {
            sql = " select sum(b.netTotal) from Bill b "
                    + " where b.retired=false ";
        }
        if (hos) {
            sql = " select sum(b.netTotal-b.staffFee) from Bill b "
                    + " where b.retired=false ";
        }

        if (createdDate) {
            sql += " and b.createdAt between :fDate and :tDate ";
        } else {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and b.singleBillSession.sessionDate between :fDate and :tDate ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and b.createdAt between :fDate and :tDate ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and b.createdAt between :fDate and :tDate ";
            }
        }

        if (!billTypes.isEmpty()) {
            sql += " and b.billType in :bt ";
            hm.put("bt", billTypes);
        }

        if (bill != null) {
            sql += " and type(b)=:class";
            hm.put("class", bill.getClass());
        }

        if (institution != null) {
            sql += " and b.institution=:ins ";
            hm.put("ins", institution);
        }

//        if (reportKeyWord.getWebUser() != null) {
//            sql += " and b.creater=:user";
//            hm.put("user", reportKeyWord.getWebUser());
//        }
        hm.put("fDate", fd);
        hm.put("tDate", td);

        return billFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public void createConsultantCountTableByCreatedDate() {
        Date startTime = new Date();
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
        channelTotal = new ChannelTotal();
        BillType[] billTypes = {BillType.ChannelCash, BillType.ChannelAgent, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

//        //System.out.println("getStaffbyClassType(bts) = " + getStaffbyClassType(bts, fromDate, toDate));
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
                //System.out.println("i" + i);
            }
            row.setCashCount(arr[0]);
            row.setAgentCount(arr[1]);
            row.setOncallCount(arr[2]);
            row.setStaffCount(arr[3]);
            channelTotal.setVat(channelTotal.getVat() + row.getCashCount());
            channelTotal.setDiscount(channelTotal.getDiscount() + row.getAgentCount());
            channelTotal.setHosFee(channelTotal.getHosFee() + row.getOncallCount());
            channelTotal.setNetTotal(channelTotal.getNetTotal() + row.getStaffCount());

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

    public void createStaffBookingTable() {
        channelBills = new ArrayList<>();
        channelBills = channelBillListByBillType(sessoinDate, paid, Arrays.asList(new BillType[]{BillType.ChannelStaff}), fromDate, toDate, staff);
    }

    public void createStaffBookingSummeryTable() {
        staffBookingWithCounts = new ArrayList<>();
        List<Object[]> objects = channelStaffCountList(sessoinDate, paid, Arrays.asList(new BillType[]{BillType.ChannelStaff}), fromDate, toDate);
        for (Object[] ob : objects) {
            StaffBookingWithCount sbwc = new StaffBookingWithCount();
            sbwc.setStaff((Staff) ob[0]);
            sbwc.setCount((long) ob[1]);
            staffBookingWithCounts.add(sbwc);
        }
    }

    public List<Bill> channelBillListByBillType(boolean createdDate, boolean paid, List<BillType> billTypes, Date fd, Date td, Staff s) {

        HashMap hm = new HashMap();

        String sql = " select b from Bill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.refunded=false ";

        if (paid) {
            sql += " and b.paidBill is not null "
                    + " and b.paidAmount!=0 ";
        }

        if (s != null) {
            sql += " and b.toStaff-:s ";
            hm.put("s", s);
        }

        if (createdDate) {
            sql += " and b.createdAt between :fDate and :tDate ";
        } else {
            sql += " and b.singleBillSession.sessionDate between :fDate and :tDate ";
        }

        if (!billTypes.isEmpty()) {
            sql += " and b.billType in :bt ";
            hm.put("bt", billTypes);
        }

        sql += " order by b.singleBillSession.sessionDate ";

        hm.put("fDate", fd);
        hm.put("tDate", td);

        return billFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Object[]> channelStaffCountList(boolean createdDate, boolean paid, List<BillType> billTypes, Date fd, Date td) {

        HashMap hm = new HashMap();

        String sql = " select b.toStaff,count(b) from Bill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.refunded=false "
                + " and b.toStaff is not null ";

        if (paid) {
            sql += " and b.paidBill is not null "
                    + " and b.paidAmount!=0 ";
        }

        if (createdDate) {
            sql += " and b.createdAt between :fDate and :tDate ";
        } else {
            sql += " and b.singleBillSession.sessionDate between :fDate and :tDate ";
        }

        if (!billTypes.isEmpty()) {
            sql += " and b.billType in :bt ";
            hm.put("bt", billTypes);
        }

        sql += " group by b.toStaff ";

        hm.put("fDate", fd);
        hm.put("tDate", td);

        return billFacade.findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    public void createChannelPatientCountByCreatedDate() {
        Date startTime = new Date();

        createChannelPatientCount(false);

    }

    public void createChannelPatientCountBySessionDate() {
        Date startTime = new Date();
        createChannelPatientCount(true);
    }

    public void createChannelHospitalIncomeByCreatedDate() {
        Date startTime = new Date();
        createChannelHospitalIncome(false);

    }

    public void createChannelHospitalIncomeBySessionDate() {
        Date startTime = new Date();
        createChannelHospitalIncome(true);

    }

    public void createChannelHospitalIncomeByCreatedDateWithDoc() {
        Date startTime = new Date();
        createChannelHospitalIncomeWithDoc(false);
    }

    public void createChannelHospitalIncomeBySessionDateWithDoc() {
        Date startTime = new Date();
        createChannelHospitalIncomeWithDoc(true);
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
        BookingCountSummryRow row = new BookingCountSummryRow();
        row.setBookingType("Total");
        for (BookingCountSummryRow bc : bookingCountSummryRows) {
            row.setBilledCount(row.getBilledCount() + bc.getBilledCount());
            row.setCancelledCount(row.getCancelledCount() + bc.getCancelledCount());
            row.setRefundCount(row.getRefundCount() + bc.getRefundCount());
        }
        bookingCountSummryRows.add(row);
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
        BookingCountSummryRow row = new BookingCountSummryRow();
        row.setBookingType("Total");
        for (BookingCountSummryRow bc : bookingCountSummryRows) {
            row.setBilledCount(row.getBilledCount() + bc.getBilledCount());
            row.setCancelledCount(row.getCancelledCount() + bc.getCancelledCount());
            row.setRefundCount(row.getRefundCount() + bc.getRefundCount());
        }
        bookingCountSummryRows.add(row);
    }

    public void createChannelHospitalIncomeWithDoc(boolean sessionDate) {
        bookingCountSummryRows = new ArrayList<>();
        bookingCountSummryRowsScan = new ArrayList<>();
        BillType[] billTypes = {BillType.ChannelCash,
            BillType.ChannelOnCall,
            BillType.ChannelStaff,
            BillType.ChannelAgent,};
        List<BillType> bts = Arrays.asList(billTypes);
        bookingCountSummryRows = createSmmeryRowsHospitalIncomeWithDoc(bts, sessionDate, FeeType.OwnInstitution);
        BookingCountSummryRow row = new BookingCountSummryRow();
        row.setBookingType("Total");
        for (BookingCountSummryRow bc : bookingCountSummryRows) {
            row.setBillHos(row.getBillHos() + bc.getBillHos());
            row.setBillHosVat(row.getBillHosVat() + bc.getBillHosVat());
            row.setbDoc(row.getbDoc() + bc.getbDoc());
            row.setbDocVat(row.getbDocVat() + bc.getbDocVat());
            row.setCanHos(row.getCanHos() + bc.getCanHos());
            row.setCanHosVat(row.getCanHosVat() + bc.getCanHosVat());
            row.setCanDoc(row.getCanDoc() + bc.getCanDoc());
            row.setCanDocVat(row.getCanDocVat() + bc.getCanDocVat());
            row.setRefHos(row.getRefHos() + bc.getRefHos());
            row.setRefHosVat(row.getRefHosVat() + bc.getRefHosVat());
            row.setRefDoc(row.getRefDoc() + bc.getRefDoc());
            row.setRefDocVat(row.getRefDocVat() + bc.getRefDocVat());
        }
        bookingCountSummryRows.add(row);

        bookingCountSummryRowsScan = createSmmeryRowsHospitalIncomeWithDoc(bts, sessionDate, FeeType.Service);

        row = new BookingCountSummryRow();
        row.setBookingType("Total");
        for (BookingCountSummryRow bc : bookingCountSummryRowsScan) {
            row.setBillHos(row.getBillHos() + bc.getBillHos());
            row.setBillHosVat(row.getBillHosVat() + bc.getBillHosVat());
            row.setbDoc(row.getbDoc() + bc.getbDoc());
            row.setbDocVat(row.getbDocVat() + bc.getbDocVat());
            row.setCanHos(row.getCanHos() + bc.getCanHos());
            row.setCanHosVat(row.getCanHosVat() + bc.getCanHosVat());
            row.setCanDoc(row.getCanDoc() + bc.getCanDoc());
            row.setCanDocVat(row.getCanDocVat() + bc.getCanDocVat());
            row.setRefHos(row.getRefHos() + bc.getRefHos());
            row.setRefHosVat(row.getRefHosVat() + bc.getRefHosVat());
            row.setRefDoc(row.getRefDoc() + bc.getRefDoc());
            row.setRefDocVat(row.getRefDocVat() + bc.getRefDocVat());
        }
        bookingCountSummryRowsScan.add(row);
    }

    public void createSmmeryRows(List<BillType> bts, boolean sessionDate, FeeType ft) {
        for (BillType bt : bts) {
            BookingCountSummryRow row = new BookingCountSummryRow();
            if (ft == FeeType.Service) {
                row.setBookingType("Scan " + bt.getLabel());
                row.setBilledCount(countBillByBillTypeAndFeeType(new BilledBill(), ft, bt, sessionDate, paid, false));
                row.setCancelledCount(countBillByBillTypeAndFeeType(new CancelledBill(), ft, bt, sessionDate, paid, false));
                row.setRefundCount(countBillByBillTypeAndFeeType(new RefundBill(), ft, bt, sessionDate, paid, false));
                bookingCountSummryRows.add(row);
                if (bt == BillType.ChannelAgent) {
                    row = new BookingCountSummryRow();
                    row.setBookingType("Scan Online " + bt.getLabel());
                    row.setBilledCount(countBillByBillTypeAndFeeType(new BilledBill(), ft, bt, sessionDate, paid, true));
                    row.setCancelledCount(countBillByBillTypeAndFeeType(new CancelledBill(), ft, bt, sessionDate, paid, true));
                    row.setRefundCount(countBillByBillTypeAndFeeType(new RefundBill(), ft, bt, sessionDate, paid, true));
                    bookingCountSummryRows.add(row);
                }
            } else {
                row.setBookingType(bt.getLabel());
                row.setBilledCount(countBillByBillTypeAndFeeType(new BilledBill(), ft, bt, sessionDate, paid, false));
                row.setCancelledCount(countBillByBillTypeAndFeeType(new CancelledBill(), ft, bt, sessionDate, paid, false));
                row.setRefundCount(countBillByBillTypeAndFeeType(new RefundBill(), ft, bt, sessionDate, paid, false));
                bookingCountSummryRows.add(row);
                if (bt == BillType.ChannelAgent) {
                    row = new BookingCountSummryRow();
                    row.setBookingType("Online " + bt.getLabel());
                    row.setBilledCount(countBillByBillTypeAndFeeType(new BilledBill(), ft, bt, sessionDate, paid, true));
                    row.setCancelledCount(countBillByBillTypeAndFeeType(new CancelledBill(), ft, bt, sessionDate, paid, true));
                    row.setRefundCount(countBillByBillTypeAndFeeType(new RefundBill(), ft, bt, sessionDate, paid, true));
                    bookingCountSummryRows.add(row);
                }
            }

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
//                row.setRefundCount(hospitalTotalBillByBillTypeAndFeeType(new RefundBill(), FeeType.Service, bt, sessionDate, paid));
                row.setRefundCount(0);
            } else {
                row.setBookingType(bt.getLabel());
                row.setBilledCount(hospitalTotalBillByBillTypeAndFeeType(new BilledBill(), FeeType.OwnInstitution, bt, sessionDate, paid));
                row.setCancelledCount(hospitalTotalBillByBillTypeAndFeeType(new CancelledBill(), FeeType.OwnInstitution, bt, sessionDate, paid));
//                row.setRefundCount(hospitalTotalBillByBillTypeAndFeeType(new RefundBill(), FeeType.OwnInstitution, bt, sessionDate, paid));
                row.setRefundCount(0);
            }

            bookingCountSummryRows.add(row);

        }
    }

    public List<BookingCountSummryRow> createSmmeryRowsHospitalIncomeWithDoc(List<BillType> bts, boolean sessionDate, FeeType ft) {

        List<BookingCountSummryRow> bcsrs = new ArrayList<>();

        for (BillType bt : bts) {
            BookingCountSummryRow row = new BookingCountSummryRow();
            if (ft == FeeType.Service) {
                row.setBookingType("Scan " + bt.getLabel());
                double d[] = new double[4];
                d = hospitalTotalBillByBillTypeAndFeeTypeWithdocfee(new BilledBill(), FeeType.Service, bt, sessionDate, paid);
                row.setBillHos(d[0]);
                row.setBillHosVat(d[1]);
                row.setbDoc(d[2]);
                row.setbDocVat(d[3]);
                d = hospitalTotalBillByBillTypeAndFeeTypeWithdocfee(new CancelledBill(), FeeType.Service, bt, sessionDate, paid);
                row.setCanHos(d[0]);
                row.setCanHosVat(d[1]);
                row.setCanDoc(d[2]);
                row.setCanDocVat(d[3]);
                d = hospitalTotalBillByBillTypeAndFeeTypeWithdocfee(new RefundBill(), FeeType.Service, bt, sessionDate, paid);
                row.setRefHos(d[0]);
                row.setRefHosVat(d[1]);
                row.setRefDoc(d[2]);
                row.setRefDocVat(d[3]);
            } else {
                row.setBookingType(bt.getLabel());
                double d[] = new double[4];
                d = hospitalTotalBillByBillTypeAndFeeTypeWithdocfee(new BilledBill(), FeeType.OwnInstitution, bt, sessionDate, paid);
                row.setBillHos(d[0]);
                row.setBillHosVat(d[1]);
                row.setbDoc(d[2]);
                row.setbDocVat(d[3]);
                d = hospitalTotalBillByBillTypeAndFeeTypeWithdocfee(new CancelledBill(), FeeType.OwnInstitution, bt, sessionDate, paid);
                row.setCanHos(d[0]);
                row.setCanHosVat(d[1]);
                row.setCanDoc(d[2]);
                row.setCanDocVat(d[3]);
                d = hospitalTotalBillByBillTypeAndFeeTypeWithdocfee(new RefundBill(), FeeType.OwnInstitution, bt, sessionDate, paid);
                row.setRefHos(d[0]);
                row.setRefHosVat(d[1]);
                row.setRefDoc(d[2]);
                row.setRefDocVat(d[3]);
            }

            bcsrs.add(row);

        }
        return bcsrs;
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
        Date startTime = new Date();

        channelBillList(true);
    }

    public void channelBillListSessionDate() {
        Date startTime = new Date();

        channelBillList(false);

    }

    public void channelBillList() {
        Date startTime = new Date();

        channelBills = new ArrayList<>();
        channelBillsCancelled = new ArrayList<>();
        channelBillsRefunded = new ArrayList<>();

        if (summery) {
            channelBills = channelBillList(sessoinDate, new BilledBill(), paid, getReportKeyWord().getStaff(), getReportKeyWord().getSpeciality(), getReportKeyWord().getArea());
        } else {
            channelBills = channelBillList(sessoinDate, new BilledBill(), paid, getReportKeyWord().getStaff(), getReportKeyWord().getSpeciality(), getReportKeyWord().getArea());
            channelBillsCancelled = channelBillList(sessoinDate, new CancelledBill(), paid, getReportKeyWord().getStaff(), getReportKeyWord().getSpeciality(), getReportKeyWord().getArea());
            channelBillsRefunded = channelBillList(sessoinDate, new RefundBill(), paid, getReportKeyWord().getStaff(), getReportKeyWord().getSpeciality(), getReportKeyWord().getArea());
        }

    }

    public void createAreaWithCountTable() {
        channelAreaWithCount(sessoinDate);
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

    public List<Bill> channelBillList(boolean createdDate, Bill bill, boolean paid, Staff s, Speciality sp, Area area) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        HashMap hm = new HashMap();

        String sql = " select b from Bill b "
                + " where b.billType in :bt "
                + " and b.retired=false "
                + " and type(b)=:class ";

        if (paid) {
            sql += " and b.paidBill is not null "
                    + " and b.paidAmount!=0 ";
        }

        if (!createdDate) {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and b.singleBillSession.sessionDate between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and b.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and b.refundedBill.createdAt between :fd and :td ";
            }
        } else {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and b.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and b.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and b.refundedBill.createdAt between :fd and :td ";
            }

        }

        if (s != null) {
            sql += " and b.staff=:stf ";
            hm.put("stf", s);
        }

        if (sp != null) {
            sql += " and b.staff.speciality=:sp ";
            hm.put("sp", sp);
        }

        if (area != null) {
            sql += " and b.patient.person.area=:area ";
            hm.put("area", area);
        }

        sql += " order by b.singleBillSession.sessionDate,b.singleBillSession.serviceSession.startingTime ";

        hm.put("class", bill.getClass());
        hm.put("bt", bts);
        hm.put("fd", getFromDate());
        hm.put("td", getToDate());

        if (!createdDate) {
            List<Bill> bills = billFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
            List<Bill> rangeBills = new ArrayList<>();
            for (Bill b : bills) {
//                //System.out.println("b.getSingleBillSession().getSessionDate() = " + b.getSingleBillSession().getSessionDate());
//                //System.out.println("b.getSingleBillSession().getSessionTime() = " + b.getSingleBillSession().getSessionTime());
                Calendar d = Calendar.getInstance();
                d.setTime(b.getSingleBillSession().getSessionDate());
                Calendar t = Calendar.getInstance();
                t.setTime(b.getSingleBillSession().getSessionTime());
//                //System.out.println("t.get(Calendar.HOUR) = " + t.get(Calendar.HOUR));
//                //System.out.println("t.get(Calendar.HOUR_OF_DAY) = " + t.get(Calendar.HOUR_OF_DAY));
//                //System.out.println("t.get(Calendar.MINUTE) = " + t.get(Calendar.MINUTE));
//                //System.out.println("t.get(Calendar.SECOND) = " + t.get(Calendar.SECOND));
//                Calendar cal = Calendar.getInstance();

                t.set(Calendar.YEAR, d.get(Calendar.YEAR));
                t.set(Calendar.MONTH, d.get(Calendar.MONTH));
                t.set(Calendar.DATE, d.get(Calendar.DATE));
//                cal.set(Calendar.HOUR, 00);
//                cal.add(Calendar.HOUR, t.get(Calendar.HOUR));
//                cal.set(Calendar.MINUTE, t.get(Calendar.MINUTE));
//                cal.set(Calendar.SECOND, t.get(Calendar.SECOND));
//                //System.out.println("cal.getTime() = " + cal.getTime());
                if (getFromDate().getTime() <= t.getTime().getTime()
                        && t.getTime().getTime() <= getToDate().getTime()) {
                    rangeBills.add(b);
                }
            }
            return rangeBills;
        } else {
            return billFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
        }

    }

    public void channelAreaWithCount(boolean createdDate) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid};
        List<BillType> bts = Arrays.asList(billTypes);
        HashMap hm = new HashMap();
        areaWithCount = new ArrayList<>();
        staffWithAreaRows = new ArrayList<>();
        total = 0.0;
        String sql;
        if (summery) {
            sql = " select b.patient.person.area,b.billClassType,count(b) ";
        } else {
            sql = " select b.staff,b.patient.person.area,b.billClassType,count(b) ";
        }

        sql += " from Bill b "
                + " where b.billType in :bt "
                + " and b.retired=false "
                + " and b.billClassType!=:cla "
                + " and b.patient.person.area is not null ";
        if (createdDate) {
            sql += " and b.createdAt between :fDate and :tDate ";
        } else {
            sql += " and b.singleBillSession.sessionDate between :fDate and :tDate ";
        }

        if (summery) {
            sql += " group by b.patient.person.area,b.billClassType "
                    + " order by b.patient.person.area.name ";
        } else {
            sql += " group by b.staff,b.patient.person.area,b.billClassType "
                    + " order by b.staff.person.name,b.patient.person.area.name ";
        }

        hm.put("bt", bts);
        hm.put("cla", BillClassType.RefundBill);
        hm.put("fDate", getFromDate());
        hm.put("tDate", getToDate());

        List<Object[]> objects = getBillFacade().findAggregates(sql, hm, TemporalType.TIMESTAMP);

        if (summery) {
            AreaWithCount row = null;
            Area beforeArea = null;
            for (Object[] ob : objects) {
                Area a = (Area) ob[0];
                BillClassType classType = (BillClassType) ob[1];
                long count = (long) ob[2];
                System.err.println("****************");
                //System.out.println("a.getName() = " + a.getName());
                if (classType == BillClassType.BilledBill) {
                    total += count;
                } else {
                    total -= count;
                }
                if (row == null) {
                    row = new AreaWithCount();
                    row.setArea(a);
                    if (classType == BillClassType.BilledBill) {
                        row.setCount(count);
                    } else {
                        row.setCount(0 - count);
                    }
                    beforeArea = a;
                    continue;
                }

                if (a.equals(beforeArea)) {
                    if (classType == BillClassType.BilledBill) {
                        row.setCount(row.getCount() + count);
                    } else {
                        row.setCount(row.getCount() - count);
                    }
                } else {
                    areaWithCount.add(row);
                    row = new AreaWithCount();
                    row.setArea(a);
                    if (classType == BillClassType.BilledBill) {
                        row.setCount(count);
                    } else {
                        row.setCount(0 - count);
                    }
                    beforeArea = a;
                }
            }
            areaWithCount.add(row);
        } else {
            StaffWithAreaRow row = null;
            AreaWithCount awc = null;
            Staff beforeStaff = null;
            Area beforeArea = null;
            for (Object[] ob : objects) {
                Staff s = (Staff) ob[0];
                Area a = (Area) ob[1];
                BillClassType classType = (BillClassType) ob[2];
                long count = (long) ob[3];
                System.err.println("****************");
                //System.out.println("s.getPerson().getName() = " + s.getPerson().getName());
                if (classType == BillClassType.BilledBill) {
                    total += count;
                } else {
                    total -= count;
                }
                if (row == null) {
                    row = new StaffWithAreaRow();
                    awc = new AreaWithCount();
                    row.setStaf(s);
                    awc.setArea(a);
                    if (classType == BillClassType.BilledBill) {
                        awc.setCount(count);
                    } else {
                        awc.setCount(0 - count);
                    }
                    beforeStaff = s;
                    beforeArea = a;
                    continue;
                }
                if (s.equals(beforeStaff)) {
                    if (a.equals(beforeArea)) {
                        if (classType == BillClassType.BilledBill) {
                            awc.setCount(awc.getCount() + count);
                        } else {
                            awc.setCount(awc.getCount() - count);
                        }
                    } else {
                        row.getAreaWithCounts().add(awc);
                        row.setSubTotal(row.getSubTotal() + awc.getCount());
                        awc = new AreaWithCount();
                        awc.setArea(a);
                        if (classType == BillClassType.BilledBill) {
                            awc.setCount(count);
                        } else {
                            awc.setCount(0 - count);
                        }
                        beforeArea = a;
                    }
                } else {
                    row.getAreaWithCounts().add(awc);
                    row.setSubTotal(row.getSubTotal() + awc.getCount());
                    staffWithAreaRows.add(row);
                    row = new StaffWithAreaRow();
                    awc = new AreaWithCount();
                    row.setStaf(s);
                    awc.setArea(a);
                    if (classType == BillClassType.BilledBill) {
                        awc.setCount(count);
                    } else {
                        awc.setCount(0 - count);
                    }
                    beforeStaff = s;
                    beforeArea = a;

                }

            }
            row.setSubTotal(row.getSubTotal() + awc.getCount());
            row.getAreaWithCounts().add(awc);
            staffWithAreaRows.add(row);
        }
    }

    public void createChannelFees() {
        Date startTime = new Date();

        valueList = new ArrayList<>();
        FeeType[] fts = {FeeType.Staff, FeeType.OwnInstitution, FeeType.OtherInstitution, FeeType.Service};

        for (FeeType ft : fts) {
            setFeeTotals(valueList, ft);
        }
        calTotals(valueList);

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
        if (bookingController.getSelectedServiceSession() == null) {
            JsfUtil.addErrorMessage("Please Select Session");
            return;
        }

        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false and "
                + " type(bs.bill)=:class and "
                //+ " bs.bill.refunded=false and "
                + " bs.bill.billType in :tbs and "
                + " bs.serviceSession.id=" + bookingController.getSelectedServiceSession().getId() + " order by bs.serialNo";
        HashMap hh = new HashMap();
        hh.put("class", BilledBill.class);
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.ChannelAgent);
        bts.add(BillType.ChannelCash);
        bts.add(BillType.ChannelOnCall);
        bts.add(BillType.ChannelStaff);
        hh.put("tbs", bts);
        nurseViewSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.TIMESTAMP);

    }

    public void fillNurseViewPb() {
        nurseViewSessions = new ArrayList<>();
        if (bookingPastController.getSelectedServiceSession() == null) {
            JsfUtil.addErrorMessage("Please Select Session");
            return;
        }

        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false and "
                + " type(bs.bill)=:class and "
                //+ " bs.bill.refunded=false and "
                + " bs.bill.billType in :tbs and "
                + " bs.serviceSession.id=" + bookingPastController.getSelectedServiceSession().getId() + " order by bs.serialNo";
        HashMap hh = new HashMap();
        hh.put("class", BilledBill.class);
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
        if (serviceSession != null) {
            String sql = "Select bs From BillSession bs "
                    + " where bs.retired=false and "
                    + " type(bs.bill)=:class and "
                    //                    + " bs.bill.cancelled=false and "
                    //                    + " bs.bill.refunded=false and "
                    + " bs.bill.billType in :tbs and "
                    + " bs.serviceSession.id=" + serviceSession.getId() + " and bs.sessionDate= :ssDate"
                    + " order by bs.serialNo";
            HashMap hh = new HashMap();
            hh.put("ssDate", serviceSession.getSessionDate());
            List<BillType> bts = new ArrayList<>();
            bts.add(BillType.ChannelAgent);
            bts.add(BillType.ChannelCash);
            bts.add(BillType.ChannelOnCall);
            bts.add(BillType.ChannelStaff);
            hh.put("tbs", bts);
            hh.put("class", BilledBill.class);
            doctorViewSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);
            netTotal = 0.0;
            grantNetTotal = 0.0;
            //Totals
            sql = "Select bs From BillSession bs "
                    + " where bs.retired=false and "
                    + " bs.bill.billType in :tbs and "
                    + " bs.serviceSession.id=" + serviceSession.getId() + " and bs.sessionDate= :ssDate"
                    + " order by bs.serialNo";
            HashMap h = new HashMap();
            h.put("ssDate", serviceSession.getSessionDate());
            List<BillType> bts2 = new ArrayList<>();
            bts2.add(BillType.ChannelAgent);
            bts2.add(BillType.ChannelCash);
            bts2.add(BillType.ChannelOnCall);
            bts2.add(BillType.ChannelStaff);
            h.put("tbs", bts2);
            List<BillSession> list = getBillSessionFacade().findByJpql(sql, h, TemporalType.DATE);

            for (BillSession bs : list) {
                if (bs.getBill().getBalance() == 0.0) {
                    if (!bs.getServiceSession().getOriginatingSession().isRefundable()) {
                        netTotal += bs.getBill().getStaffFee();
                        grantNetTotal += bs.getBill().getNetTotal();
                    } else if (bs.isAbsent() && !bs.getBill().isCancelled() && !bs.getBill().isRefunded()) {
//                        netTotal += bs.getBill().getStaffFee();
//                        grantNetTotal += bs.getBill().getNetTotal();
                    } else {
                        netTotal += bs.getBill().getStaffFee();
                        grantNetTotal += bs.getBill().getNetTotal();
                    }

                }
            }
        }
    }

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

    public void createDoctorArrival() {

        String sql;
        Map m = new HashMap();
        arrivalRecords = new ArrayList<>();

        sql = " select ar from ArrivalRecord ar where "
                + " ar.sessionDate between :fd and :td "
                + " and ar.retired=false "
                + " order by ar.serviceSession.startingTime ";

        m.put("fd", commonFunctions.getStartOfDay());
        m.put("td", commonFunctions.getEndOfDay());

        arrivalRecords = arrivalRecordFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void createTotalDoctor() {

        channelDoctors = new ArrayList<ChannelDoctor>();
        HashMap m = new HashMap();
        String sql;
        sql = "Select bs.bill From BillSession bs "
                + " where bs.bill.staff is not null "
                + " and bs.retired=false "
                + " and bs.sessionDate= :ssDate ";

        if (sessionController.getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative) {
            sql += " and bs.bill.singleBillSession.serviceSession.originatingSession.forBillType=:bt ";
            m.put("bt", BillType.Channel);
        }

        sql += " order by bs.bill.staff.person.name ";

        m.put("ssDate", Calendar.getInstance().getTime());
        List<Bill> bills = getBillFacade().findByJpql(sql, m, TemporalType.DATE);
        if (sessionController.getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna) {
//            //System.out.println("getReportKeyWord().getString() = " + getReportKeyWord().getString());
            if (getReportKeyWord().getString().equals("0")) {

            }
            if (getReportKeyWord().getString().equals("1")) {
                List<Bill> reBills = new ArrayList<>();
                for (Bill b : bills) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(b.getSingleBillSession().getServiceSession().getStartingTime());
//                    //System.out.println("cal.get(Calendar.HOUR) = " + cal.get(Calendar.HOUR));
//                    //System.out.println("cal.get(Calendar.MINUTE) = " + cal.get(Calendar.MINUTE));
//                    //System.out.println("cal.get(Calendar.AM_PM) = " + cal.get(Calendar.AM_PM));
//                    //System.out.println("cal.get(Calendar.HOUR_OF_DAY) = " + cal.get(Calendar.HOUR_OF_DAY));
                    if (cal.get(Calendar.HOUR_OF_DAY) >= 12) {
                        reBills.add(b);
//                        System.err.println("add 1");
                    }
                }
//                //System.out.println("bills.size() = " + bills.size());
//                //System.out.println("reBills.size() = " + reBills.size());
                bills.removeAll(reBills);
//                //System.out.println("bills.size() = " + bills.size());
            }
            if (getReportKeyWord().getString().equals("2")) {
                List<Bill> reBills = new ArrayList<>();
                for (Bill b : bills) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(b.getSingleBillSession().getServiceSession().getStartingTime());
//                    //System.out.println("cal.get(Calendar.HOUR) = " + cal.get(Calendar.HOUR));
//                    //System.out.println("cal.get(Calendar.MINUTE) = " + cal.get(Calendar.MINUTE));
//                    //System.out.println("cal.get(Calendar.AM_PM) = " + cal.get(Calendar.AM_PM));
//                    //System.out.println("cal.get(Calendar.HOUR_OF_DAY) = " + cal.get(Calendar.HOUR_OF_DAY));
                    if (cal.get(Calendar.HOUR_OF_DAY) < 12) {
                        reBills.add(b);
//                        System.err.println("add 2");
                    }
                }
//                //System.out.println("bills.size() = " + bills.size());
//                //System.out.println("reBills.size() = " + reBills.size());
                bills.removeAll(reBills);
//                //System.out.println("bills.size() = " + bills.size());
            }
        }
//        //System.out.println("bills = " + bills.size());
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
//            System.err.println("cd = " + cd.getConsultant().getPerson().getName());
            for (Bill b : bills) {
//                //System.out.println("b = " + b.getStaff().getPerson().getName());
//                //System.out.println("b = " + b.getBillClass());
                if (Objects.equals(b.getStaff().getId(), cd.getConsultant().getId())) {
                    if (b.getBillType() == BillType.ChannelCash
                            || b.getBillType() == BillType.ChannelPaid
                            || (b.getBillType() == BillType.ChannelAgent
                            && sessionController.getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna)
                            || (b.getBillType() == BillType.ChannelAgent
                            && agncy)) {
                        if (b instanceof BilledBill) {
                            cd.setBillCount(cd.getBillCount() + 1);
                            cd.setBillFee(cd.getBillFee() + getBillFees(b, FeeType.Staff));
                        } else if (b instanceof CancelledBill) {
                            cd.setBillCanncelCount(cd.getBillCanncelCount() + 1);
                            cd.setBillCanncelFee(cd.getBillCanncelFee() + getBillFees(b, FeeType.Staff));
                        } else if (b instanceof RefundBill) {
                            cd.setRefundedCount(cd.getRefundedCount() + 1);
                            cd.setRefundFee(cd.getRefundFee() + getBillFees(b, FeeType.Staff));
                        }
                    } else {
                        if ((b.getBillType() == BillType.ChannelStaff || b.getBillType() == BillType.ChannelOnCall)
                                && b.getPaidBill() == null && !b.isCancelled() && !b.isRefunded()) {
                            cd.setNotPaidBillCount(cd.getNotPaidBillCount() + 1);
                        }
                    }
                }
            }

        }

        calTotal();

    }

    public void createDailyDoctorAnalysis() {
        HashMap m = new HashMap();
        String sql;
        sql = "Select new com.divudi.data.DoctorDayChannelCount(bs.bill.staff, bs.sessionDate , count(bs) )"
                + " From BillSession bs "
                + " where bs.bill.staff is not null "
                + " and bs.retired=false "
                + " and bs.sessionDate between :ssFromDate and :ssToDate "
                + " group by bs.bill.staff, bs.sessionDate";
        m.put("ssFromDate", fromDate);
        m.put("ssToDate", toDate);
        doctorDayChannelCounts = getBillFacade().findByJpql(sql, m, TemporalType.DATE);
    }

    public void createDailyDoctorAnalysisDisplay() {
        HashMap m = new HashMap();
        String sql;
        sql = "Select new com.divudi.data.DoctorDayChannelCount(bs.bill.staff, bs.sessionDate , count(bs) )"
                + " From BillSession bs "
                + " where bs.bill.staff is not null "
                + " and bs.retired=false "
                + " and bs.sessionDate between :ssFromDate and :ssToDate "
                + " group by bs.sessionDate, bs.bill.staff";
        m.put("ssFromDate", fromDate);
        m.put("ssToDate", toDate);
        List<DoctorDayChannelCount> t = getBillFacade().findByJpql(sql, m, TemporalType.DATE);
        weekdayDisplays = new ArrayList<>();
        for (DoctorDayChannelCount c : t) {
            boolean found = false;

            WeekdayDisplay selectedDisplay = null;
            for (WeekdayDisplay d : weekdayDisplays) {
                if (d.getStaff().equals(c.getStaff())) {
                    Calendar tcal = Calendar.getInstance();
                    tcal.setTime(c.getAppointmentDate());
                    Integer ty = tcal.get(Calendar.YEAR);
                    Integer tm = tcal.get(Calendar.MONTH);
                    if (ty.equals(d.getWeekdayYear()) && tm.equals(d.getWeekdayMonth())) {
                        selectedDisplay = d;
                    }
                }
            }
            if (selectedDisplay == null) {
                selectedDisplay = new WeekdayDisplay();
                selectedDisplay.setDate(c.getAppointmentDate());
                selectedDisplay.setStaff(c.getStaff());
                weekdayDisplays.add(selectedDisplay);
            }
            for (int i = 0; i < 37; i++) {
                Date[] testDates = selectedDisplay.getDates();
                Date testDate = testDates[i];
                if (testDate != null && c.getAppointmentDate() != null) {
                    if (testDate.equals(c.getAppointmentDate())) {
                        (selectedDisplay.getCounts())[i] = c.getBooked();
                    }
                }
            }

        }
    }

    public void createTotalDoctorScan() {

        channelDoctors = new ArrayList<ChannelDoctor>();
        HashMap m = new HashMap();
        String sql;
        sql = "Select bs.bill From BillSession bs "
                + " where bs.bill.staff is not null "
                + " and bs.retired=false "
                + " and bs.sessionDate= :ssDate ";

        if (sessionController.getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative) {
            sql += " and bs.bill.singleBillSession.serviceSession.originatingSession.forBillType=:bt ";
            m.put("bt", BillType.XrayScan);
        }

        sql += " order by bs.bill.staff.person.name ";

        m.put("ssDate", Calendar.getInstance().getTime());
        List<Bill> bills = getBillFacade().findByJpql(sql, m, TemporalType.DATE);
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
                if (Objects.equals(b.getStaff().getId(), cd.getConsultant().getId())) {
                    if (b.getBillType() == BillType.ChannelCash
                            || b.getBillType() == BillType.ChannelPaid
                            || (b.getBillType() == BillType.ChannelAgent
                            && sessionController.getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna)
                            || (b.getBillType() == BillType.ChannelAgent
                            && agncy)) {
                        if (b instanceof BilledBill) {
                            cd.setBillCount(cd.getBillCount() + 1);
                            cd.setBillFee(cd.getBillFee() + getBillFees(b, FeeType.Staff));
                        } else if (b instanceof CancelledBill) {
                            cd.setBillCanncelCount(cd.getBillCanncelCount() + 1);
                            cd.setBillCanncelFee(cd.getBillCanncelFee() + getBillFees(b, FeeType.Staff));
                        } else if (b instanceof RefundBill) {
                            cd.setRefundedCount(cd.getRefundedCount() + 1);
                            cd.setRefundFee(cd.getRefundFee() + getBillFees(b, FeeType.Staff));
                        }
                    }
                }
            }

        }

        calTotal();

    }

    public void createTotalDoctor(Date date) {

        channelDoctors = new ArrayList<ChannelDoctor>();
        HashMap m = new HashMap();
        String sql;
        sql = "Select bs.bill From BillSession bs "
                + " where bs.bill.staff is not null "
                + " and bs.retired=false "
                + " and bs.sessionDate= :ssDate "
                + " order by bs.bill.staff.person.name ";

        m.put("ssDate", date);
        List<Bill> bills = getBillFacade().findByJpql(sql, m, TemporalType.DATE);
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
                if (Objects.equals(b.getStaff().getId(), cd.getConsultant().getId())) {
                    if (b.getBillType() == BillType.ChannelCash
                            || b.getBillType() == BillType.ChannelPaid
                            || (b.getBillType() == BillType.ChannelAgent
                            && sessionController.getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna)
                            || (b.getBillType() == BillType.ChannelAgent
                            && agncy)) {
                        if (b instanceof BilledBill) {
                            cd.setBillCount(cd.getBillCount() + 1);
                            cd.setBillFee(cd.getBillFee() + getBillFees(b, FeeType.Staff));
                        } else if (b instanceof CancelledBill) {
                            cd.setBillCanncelCount(cd.getBillCanncelCount() + 1);
                            cd.setBillCanncelFee(cd.getBillCanncelFee() + getBillFees(b, FeeType.Staff));
                        } else if (b instanceof RefundBill) {
                            cd.setRefundedCount(cd.getRefundedCount() + 1);
                            cd.setRefundFee(cd.getRefundFee() + getBillFees(b, FeeType.Staff));
                        }
                    }
                }
            }

        }

        calTotal();

    }

    public void createTotalDoctorAll() {
        Date startTime = new Date();

        channelDoctors = new ArrayList<ChannelDoctor>();
        HashMap m = new HashMap();
        String sql;
        sql = "Select distinct(bs.bill.staff) From BillSession bs "
                + " where bs.bill.staff is not null "
                + " and bs.retired=false "
                + " and bs.sessionDate between :fd and :td "
                + " order by bs.bill.staff.person.name ";

        m.put("fd", fromDate);
        m.put("td", toDate);

        List<Staff> staffs = staffFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

        BillType[] types = {BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent};

        total = 0.0;

        for (Staff s : staffs) {
            ChannelDoctor cd = new ChannelDoctor();
            cd.setConsultant(s);
            cd.setBillCount(fetchBillCount(s, fromDate, toDate, new BilledBill(), Arrays.asList(types)));
            cd.setBillCanncelCount(fetchBillCount(s, fromDate, toDate, new CancelledBill(), Arrays.asList(types)));
            cd.setRefundedCount(fetchBillCount(s, fromDate, toDate, new RefundBill(), Arrays.asList(types)));

            cd.setBillFee(fetchBillFees(new BilledBill(), FeeType.Staff, s, fromDate, toDate, Arrays.asList(types)));
            cd.setBillCanncelFee(fetchBillFees(new CancelledBill(), FeeType.Staff, s, fromDate, toDate, Arrays.asList(types)));
            cd.setRefundFee(fetchBillFees(new RefundBill(), FeeType.Staff, s, fromDate, toDate, Arrays.asList(types)));

            channelDoctors.add(cd);

            total += cd.getBillFee() + cd.getBillCanncelFee() + cd.getRefundFee();

        }

//    
    }

    public void createTotalDoctorAllBTT() {
        Date startTime = new Date();

        channelDoctors = new ArrayList<ChannelDoctor>();
        HashMap m = new HashMap();
        String sql;
        sql = "Select distinct(bs.bill.staff) From BillSession bs "
                + " where bs.bill.staff is not null "
                + " and bs.retired=false "
                + " and bs.sessionDate between :fd and :td "
                + " order by bs.bill.staff.person.name ";

        m.put("fd", fromDate);
        m.put("td", toDate);

        List<Staff> staffs = staffFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

        BillType[] types = {BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent};

        total = 0.0;

        for (Staff s : staffs) {
            //System.out.println("s.getPerson().getName() = " + s.getPerson().getName());
            ChannelDoctor cd = new ChannelDoctor();
            cd.setConsultant(s);
            double cb = fetchBillCount(s, fromDate, toDate, new BilledBill(), Arrays.asList(types));
            //System.out.println("cb = " + cb);
            double cc = fetchBillCount(s, fromDate, toDate, new CancelledBill(), Arrays.asList(types));
            //System.out.println("cc = " + cc);
            double cr = fetchBillCount(s, fromDate, toDate, new RefundBill(), Arrays.asList(types));
            int icb = (int) ((cb * reportKeyWord.getFrom()) / 100);
            int icc = (int) ((cc * reportKeyWord.getFrom()) / 100);
            int icr = (int) ((cr * reportKeyWord.getFrom()) / 100);
            cd.setBillCount(icb);
            cd.setBillCanncelCount(icc);
            cd.setRefundedCount(icr);

            cd.setBillFee(fetchBillFeesBTT(new BilledBill(), FeeType.Staff, s, fromDate, toDate, Arrays.asList(types), icb));
            cd.setBillCanncelFee(fetchBillFeesBTT(new CancelledBill(), FeeType.Staff, s, fromDate, toDate, Arrays.asList(types), icc));
            cd.setRefundFee(fetchBillFeesBTT(new RefundBill(), FeeType.Staff, s, fromDate, toDate, Arrays.asList(types), icr));

            channelDoctors.add(cd);

            total += cd.getBillFee() + cd.getBillCanncelFee() + cd.getRefundFee();

        }

//     
    }

    public void createTodayAbsentList() {

        createTodayList(true, false);

    }

    public void createTodayCancelList() {

        createTodayList(false, true);

    }

    public void createTodayList(boolean absent, boolean cancel) {

        billSessions = new ArrayList<>();
        HashMap m = new HashMap();
        String sql;
        sql = "Select bs From BillSession bs "
                + " where bs.bill.staff is not null "
                + " and bs.retired=false "
                + " and type(bs.bill)=:class "
                + " and bs.bill.billType in :bts "
                + " and bs.sessionDate=:ssDate ";
        if (absent) {
            sql += " and bs.absent=true ";
        }
        if (cancel) {
            sql += " and bs.bill.cancelled=true ";
        }
        m.put("bts", Arrays.asList(new BillType[]{BillType.ChannelCash, BillType.ChannelPaid}));
        m.put("ssDate", getDate());
        m.put("class", BilledBill.class);
        billSessions = getBillSessionFacade().findByJpql(sql, m, TemporalType.DATE);
        calTotalBS(billSessions);

    }

    private void calTotal() {
        total = 0.0;
        for (ChannelDoctor cd : channelDoctors) {
            total += cd.getBillFee() + cd.getBillCanncelFee() + cd.getRefundFee();

        }

    }

    private void calTotalBS(List<BillSession> billSessions) {
        netTotal = 0.0;
        netTotalDoc = 0.0;
        for (BillSession bs : billSessions) {
            netTotal += bs.getBill().getNetTotal();
            netTotalDoc += bs.getBill().getStaffFee();
        }

    }

    private double getBillFees(Bill b) {
        String sql = "Select sum(b.feeValue) From BillFee b where b.retired=false and b.bill.id=" + b.getId();

        return getBillFeeFacade().findAggregateDbl(sql);
    }

    private double getBillFees(Bill b, FeeType ft) {
        Map m = new HashMap();
        String sql;

        sql = "Select sum(b.feeValue) From BillFee b "
                + " where b.retired=false "
                + " and b.fee.feeType=:ft "
                + " and b.bill.id=" + b.getId();

        m.put("ft", ft);

        return getBillFeeFacade().findDoubleByJpql(sql, m);
    }

    double fetchBillCount(Staff s, Date fd, Date td, Bill b, List<BillType> billTypes) {
        HashMap m = new HashMap();
        String sql;
        sql = "Select count(bs.bill) From BillSession bs "
                + " where bs.bill.staff=:s "
                + " and type(bs.bill)=:class "
                + " and bs.retired=false "
                + " and bs.sessionDate between :fd and :td "
                + " and bs.bill.billType in :bts ";

        m.put("fd", fd);
        m.put("td", td);
        m.put("s", s);
        m.put("class", b.getClass());
        m.put("bts", billTypes);
        double d = getBillFacade().findAggregateLong(sql, m, TemporalType.TIMESTAMP);

        return d;

    }

    double fetchBillFees(Bill b, FeeType ft, Staff s, Date fd, Date td, List<BillType> billTypes) {
        Map m = new HashMap();
        String sql;

        sql = "Select sum(bf.feeValue) From BillFee bf "
                + " where bf.retired=false "
                + " and bf.fee.feeType=:ft "
                + " and type(bf.bill)=:class "
                + " and bf.bill.staff=:s "
                + " and bf.bill.singleBillSession.sessionDate between :fd and :td "
                + " and bf.bill.billType in :bts ";

        m.put("ft", ft);
        m.put("class", b.getClass());
        m.put("fd", fd);
        m.put("td", td);
        m.put("s", s);
        m.put("bts", billTypes);
        double d = getBillFeeFacade().findDoubleByJpql(sql, m);

        return d;
    }

    double fetchBillFeesBTT(Bill b, FeeType ft, Staff s, Date fd, Date td, List<BillType> billTypes, int max) {
        Map m = new HashMap();
        String sql;
        List<BillFee> bfs = new ArrayList<>();

        sql = "Select bf From BillFee bf "
                + " where bf.retired=false "
                + " and bf.fee.feeType=:ft "
                + " and type(bf.bill)=:class "
                + " and bf.bill.staff=:s "
                + " and bf.bill.singleBillSession.sessionDate between :fd and :td "
                + " and bf.bill.billType in :bts";

        m.put("ft", ft);
        m.put("class", b.getClass());
        m.put("fd", fd);
        m.put("td", td);
        m.put("s", s);
        m.put("bts", billTypes);
        bfs = getBillFeeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, max);
        double d = 0.0;
        for (BillFee bf : bfs) {
            d += bf.getFeeValue();
        }

        return d;
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
        Date startTime = new Date();
        agentHistorys = new ArrayList<>();

        agentHistorys = createAgentHistory(fromDate, toDate, institution, null);

    }

    public void createAgentCreditLimitUpdateDetail() {
        agentHistorys = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        sql = " select ah from AgentHistory ah where ah.retired=false "
                + " and ah.createdAt between :fd and :td"
                + " and ah.historyType=:ht ";

        if (institution != null) {
            sql += " and ah.institution=:ins ";
            m.put("ins", institution);
        }

        m.put("ht", HistoryType.AgentBalanceUpdateBill);
        m.put("fd", fromDate);
        m.put("td", toDate);

        sql += " order by ah.createdAt ";

        agentHistorys = getAgentHistoryFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void createAgentBookings() {
        Date startTime = new Date();

        HashMap m = new HashMap();

        String sql = " select b from Bill b "
                + " where b.retired=false ";
//                + " and type(b)=:class ";

        if (webUser != null) {
            sql += " and b.creater=:web ";
            m.put("web", webUser);
        }
        if (sessoinDate) {
            sql += " and b.singleBillSession.sessionDate between :fd and :td ";
        } else {
            sql += " and b.createdAt between :fd and :td ";
        }

        if (institution != null) {
            sql += " and b.creditCompany=:a ";
            m.put("a", institution);
        } else {
            sql += " and b.creditCompany is not null ";
        }

        if (getReportKeyWord().getBillType() != null) {
            sql += " and b.singleBillSession.serviceSession.originatingSession.forBillType=:bts ";
            m.put("bts", getReportKeyWord().getBillType());
        }

        sql += " and b.billType=:bt ";
        m.put("bt", BillType.ChannelAgent);

        sql += " order by b.creditCompany.name ";
//        m.put("class", BilledBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());

        channelBills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        for (Bill b : channelBills) {
            if (b.getPaymentMethod() == PaymentMethod.Cash && (b instanceof CancelledBill || b instanceof RefundBill)) {
                b.setStaffFee(0);
                b.setNetTotal(0);
            }
        }
        netTotal = calTotal(channelBills);
        netTotalDoc = calTotalDoc(channelBills);

    }

    public void createCollectingCentreHistoryTable() {
        Date startTime = new Date();
        agentHistorys = new ArrayList<>();
        HistoryType[] hts = {HistoryType.CollectingCentreBalanceUpdateBill, HistoryType.CollectingCentreDeposit, HistoryType.CollectingCentreDepositCancel, HistoryType.CollectingCentreBilling};
        List<HistoryType> historyTypes = Arrays.asList(hts);

        agentHistorys = createAgentHistoryErrorCheck(fromDate, toDate, institution, historyTypes);
        checkCumilativeTotal(agentHistorys);

    }

    public void createAgentHistorySubTable() {
        Date startTime = new Date();
        if (institution == null) {
            JsfUtil.addErrorMessage("Please Select Agency.");
            return;
        }
        HistoryType[] ht = {HistoryType.ChannelBooking, HistoryType.ChannelDeposit, HistoryType.ChannelDepositCancel, HistoryType.ChannelDebitNote,
            HistoryType.ChannelDebitNoteCancel, HistoryType.ChannelCreditNote, HistoryType.ChannelCreditNoteCancel};
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

    public void createCollectingCenterHistorySubTable() {
        Date startTime = new Date();

        if (institution == null) {
            JsfUtil.addErrorMessage("Please Select Agency.");
            return;
        }
        HistoryType[] ht = {HistoryType.CollectingCentreBalanceUpdateBill,
            HistoryType.CollectingCentreDeposit,
            HistoryType.CollectingCentreDepositCancel,
            HistoryType.CollectingCentreBilling,
            HistoryType.CollectingCentreCreditNote,
            HistoryType.CollectingCentreCreditNoteCancel,
            HistoryType.CollectingCentreDebitNote,
            HistoryType.CollectingCentreDebitNoteCancel};
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

    public List<AgentHistory> createAgentHistoryErrorCheck(Date fd, Date td, Institution i, List<HistoryType> hts) {
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

        sql += " order by ah.bill.fromInstitution.name ,ah.createdAt ";

        return getAgentHistoryFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public List<AgentHistory> createAgentHistoryByBook(Date fd, Date td, Institution i, List<HistoryType> hts, int sn, int en) {
        String sql;
        Map m = new HashMap();
        List<AgentHistory> ahs;

        sql = " select ah from AgentHistory ah where ah.retired=false "
                + " and ah.bill.retired=false "
                + " and ah.createdAt between :fd and :td "
                + " and ah.referenceNo >=" + sn
                + " and ah.referenceNo <=" + en;

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
//        m.put("sn", sn);
//        m.put("en", en);

        sql += " order by ah.bill.billClassType, ah.createdAt ";

        ahs = getAgentHistoryFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        return ahs;

    }

    public double createAgentHistoryByBookTotal(Date fd, Date td, Institution i, List<HistoryType> hts, int sn, int en) {
        String sql;
        Map m = new HashMap();
        double d = 0.0;

        sql = " select sum(ah.bill.netTotal+ah.bill.vat) from AgentHistory ah where ah.retired=false "
                + " and ah.bill.retired=false "
                + " and ah.createdAt between :fd and :td "
                + " and ah.referenceNo >=" + sn
                + " and ah.referenceNo <=" + en;

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

        sql += " order by ah.bill.billClassType, ah.createdAt ";
        d = getAgentHistoryFacade().findDoubleByJpql(sql, m);

        return d;

    }
    // new method  it is used to fetch list of all channeld doctors on 2016/08/10

    public List<Staff> fetchAllChannelDoctors() {

        String sql;
        Map m = new HashMap();

        sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                + " and pi.staff.retired=false "
                + " and pi.type=:typ ";

        m.put("typ", PersonInstitutionType.Channelling);

        return staffFacade.findByJpql(sql, m);
    }

    public List<ServiceSession> fetchCreatedServiceSessions(Staff s) {
        String sql;
        Map m = new HashMap();
        List<ServiceSession> tmp = new ArrayList<>();
        sql = "Select s From ServiceSession s where s.retired=false "
                + " and s.staff=:staff "
                + " and s.originatingSession is not null "
                + " and s.sessionDate=:today "
                + " and type(s)=:class "
                + " order by s.sessionDate,s.startingTime ";
        m.put("today", new Date());
        m.put("staff", s);
        m.put("class", ServiceSession.class);

        return serviceSessionFacade.findByJpql(sql, m, TemporalType.DATE);
    }

    //created a meathod which use above methods
    public void createDoctorsWithSessions() {
        List<Staff> staffs = fetchAllChannelDoctors();
        acdrs = new ArrayList<>();
        for (Staff s : staffs) {

            List<ServiceSession> serviceSessions = fetchCreatedServiceSessions(s);
            if (!serviceSessions.isEmpty()) {
//                ServiceSession temp = new ServiceSession();
                ServiceSession last = null;
                AvalabelChannelDoctorRow row = new AvalabelChannelDoctorRow();
                for (ServiceSession ss : serviceSessions) {
                    if (ss.isDeactivated()) {
                        if (last != null) {
                            if (last.isDeactivated()) {
                                continue;
                            }
                        }
                        row = new AvalabelChannelDoctorRow();
                        row.setS(s);
                        DateFormat d = new SimpleDateFormat("hh:mm a");
                        String time = d.format(ss.getStartingTime());
                        row.setTime(time);
                        time = d.format(ss.getEndingTime());
                        row.setTimeEnd(time);
                        row.setLeave(ss.isDeactivated());
                        acdrs.add(row);
                        last = ss;
                        row = null;
                        continue;
                    } else {
                        if (last != null) {
                            if (last.isDeactivated()) {
                                last = null;
                            }
                        }
                    }
                    if (last != null) {
                        if (ss.getStartingTime().getTime() - last.getStartingTime().getTime() < 3600000) {
//                            row.setS(s);
                            if (row == null) {
                                row = new AvalabelChannelDoctorRow();
                            }
                            DateFormat d = new SimpleDateFormat("hh:mm a");
                            String time = d.format(ss.getEndingTime());
                            row.setTimeEnd(time);
                            last = ss;
//                            row.setLeave(ss.isDeactivated());
//                            acdrs.add(row);
                        } else {
                            acdrs.add(row);
                            row = new AvalabelChannelDoctorRow();
                            row.setS(s);
                            DateFormat d = new SimpleDateFormat("hh:mm a");
                            String time = d.format(ss.getStartingTime());
                            row.setTime(time);
                            time = d.format(ss.getEndingTime());
                            row.setTimeEnd(time);
                            row.setLeave(ss.isDeactivated());
                            last = ss;
                        }
                    } else {
                        row = new AvalabelChannelDoctorRow();
                        row.setS(s);
                        DateFormat d = new SimpleDateFormat("hh:mm a");
                        String time = d.format(ss.getStartingTime());
                        row.setTime(time);
                        time = d.format(ss.getEndingTime());
                        row.setTimeEnd(time);
                        row.setLeave(ss.isDeactivated());
                        last = ss;
                    }

                }
                if (row != null) {
                    acdrs.add(row);
                    row = null;
                }
            } else {
                AvalabelChannelDoctorRow row = new AvalabelChannelDoctorRow();
                row.setS(s);
//                        DateFormat d =new SimpleDateFormat("HH:mm:ss");
//                        String time=d.format(ss.getStartingTime());
                row.setTime(null);
                row.setTimeEnd(null);
                row.setLeave(false);
                acdrs.add(row);
            }
        }
        //limit the rows in the page
        //created instance using listOfList
        listOfList = new ArrayList<>();
        DocPage dp = new DocPage();
        List<AvalabelChannelDoctorRow> list = new ArrayList<>();
        int i = 1;
        for (AvalabelChannelDoctorRow a : acdrs) {
            if (list.size() == 8) {
                if (dp.getTable1() == null) {
                    dp.setTable1(list);
                    list = new ArrayList<>();
                    list.add(a);
                } else {
                    dp.setTable2(list);
                    list = new ArrayList<>();
                    list.add(a);
                    listOfList.add(dp);
                    dp = new DocPage();
                }
            } else {
                list.add(a);
            }
            i++;
        }
        if (list.size() > 0) {
            if (dp.getTable1() == null) {
                dp.setTable1(list);
                list = new ArrayList<>();
            } else {
                dp.setTable2(list);
                list = new ArrayList<>();
            }
            listOfList.add(dp);
            dp = new DocPage();
        }
    }

    public void createCardSummery() {
        String sql = "";
        Map m = new HashMap();
        grantNetTotal = 0.0;

        sql += "select b from Bill b where b.retired=false "
                + " and b.paymentMethod=:pm ";
        m.put("pm", paymentMethod.Card);

        if (department != null) {
            sql += " and b.department=:d ";
            m.put("d", department);
        }
        if (reportKeyWord.getBank() != null) {
            sql += " and b.bank=:b ";
            m.put("b", reportKeyWord.getBank());
        }

        if (webUser != null) {
            sql += " and b.creater=:u ";
            m.put("u", webUser);
        }

        sql += " and (b.billType=:cc or b.billType=:cp) ";
        m.put("cc", BillType.ChannelCash);
        m.put("cp", BillType.ChannelPaid);

        sql += " and b.createdAt between :fd and :td ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        channelBills = billFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

        for (Bill b : channelBills) {
            grantNetTotal += b.getNetTotal() + b.getVat();
        }

    }

    public void createCardSummeryBankVise() {
        depBills = new ArrayList<>();
        String sql = "";
        Map m = new HashMap();
        sql = "select b.bank,sum(b.netTotal+b.vat) from Bill b where "
                + " b.retired=false "
                + " and b.paymentMethod=:pm"
                + " and (b.billType=:cc or b.billType=:cp) "
                + " and b.createdAt between :fd and :td "
                + " group by b.bank "
                + " order by b.bank.name ";
        m.put("cc", BillType.ChannelCash);
        m.put("cp", BillType.ChannelPaid);
        m.put("pm", paymentMethod.Card);
        m.put("fd", fromDate);
        m.put("td", toDate);
        List<Object[]> objects = getBillFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
        grantNetTotal = 0.0;
        for (Object[] o : objects) {
            DepartmentBill db = new DepartmentBill();
            Institution i = (Institution) o[0];
            db.setIns(i);
            if (getReportKeyWord().getString().equals("1")) {
                List<Bill> bills = fetchCardTransactionBills(i);
                db.setBills(bills);
            }
            double d = (double) o[1];
            grantNetTotal += d;
            db.setDepartmentBillTotal(d);
            depBills.add(db);
        }

//        objects=getBillFacade().findObjectsArrayBySQL(sql, m, TemporalType.TIMESTAMP);
//        //System.out.println("objects.size() = " + objects.size());
    }

    public List<Bill> fetchCardTransactionBills(Institution i) {
        String sql = "";
        Map m = new HashMap();

        sql += "select b from Bill b where "
                + " b.retired=false "
                + " and b.paymentMethod=:pm "
                + " and b.bank=:bank "
                + " and (b.billType=:cc or b.billType=:cp) "
                + " and b.createdAt between :fd and :td "
                + " group by b.bank "
                + " order by b.bank.name ";

        m.put("cc", BillType.ChannelCash);
        m.put("cp", BillType.ChannelPaid);
        m.put("pm", paymentMethod.Card);
        m.put("bank", i);
        m.put("fd", fromDate);
        m.put("td", toDate);
        List<Bill> bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        return bills;
    }

    public void checkCumilativeTotal(List<AgentHistory> agentHistorys) {
        boolean start = true;
        double d = 0.0;
        Institution i = null;
        for (AgentHistory a : agentHistorys) {
            if (start) {
                a.setTransCumilativeTotal(a.getBeforeBallance() + a.getTransactionValue());
                start = false;
                d = a.getBeforeBallance() + a.getTransactionValue();
                i = a.getBill().getFromInstitution();
                continue;
            }
            if (i.equals(a.getBill().getFromInstitution())) {
                a.setTransCumilativeTotal(d + a.getTransactionValue());
                d = a.getBeforeBallance() + a.getTransactionValue();
            } else {
                a.setTransCumilativeTotal(a.getBeforeBallance() + a.getTransactionValue());
                d = a.getBeforeBallance() + a.getTransactionValue();
                i = a.getBill().getFromInstitution();
            }

        }
    }

    List<DocPage> listOfList = new ArrayList<>();

    public List<WeekdayDisplay> getWeekdayDisplays() {
        return weekdayDisplays;
    }

    public void setWeekdayDisplays(List<WeekdayDisplay> weekdayDisplays) {
        this.weekdayDisplays = weekdayDisplays;
    }

    public ReportTemplateRowBundle getBundle() {
        return bundle;
    }

    public void setBundle(ReportTemplateRowBundle bundle) {
        this.bundle = bundle;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public class DocPage {

        List<AvalabelChannelDoctorRow> table1;
        List<AvalabelChannelDoctorRow> table2;

        public List<AvalabelChannelDoctorRow> getTable1() {
            return table1;
        }

        public void setTable1(List<AvalabelChannelDoctorRow> table1) {
            this.table1 = table1;
        }

        public List<AvalabelChannelDoctorRow> getTable2() {
            return table2;
        }

        public void setTable2(List<AvalabelChannelDoctorRow> table2) {
            this.table2 = table2;
        }

    }

    public List<DocPage> getListOfList() {
        createDoctorsWithSessions();
        return listOfList;
    }

    public void setListOfList(List<DocPage> listOfList) {
        this.listOfList = listOfList;
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

    public ChannelReportTemplateController() {
    }

    public ServiceSession getServiceSession() {
        return serviceSession;
    }

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

    public boolean isSessoinDate() {
        return sessoinDate;
    }

    public void setSessoinDate(boolean sessoinDate) {
        this.sessoinDate = sessoinDate;
    }

    public boolean isWithDates() {
        return withDates;
    }

    public void setWithDates(boolean withDates) {
        this.withDates = withDates;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean isAgncyOnCall() {
        return agncyOnCall;
    }

    public void setAgncyOnCall(boolean agncyOnCall) {
        this.agncyOnCall = agncyOnCall;
    }

    public double getTotalRefundDoc() {
        return totalRefundDoc;
    }

    public void setTotalRefundDoc(double totalRefundDoc) {
        this.totalRefundDoc = totalRefundDoc;
    }

    public double getTotalBilledDoc() {
        return totalBilledDoc;
    }

    public void setTotalBilledDoc(double totalBilledDoc) {
        this.totalBilledDoc = totalBilledDoc;
    }

    public double getTotalCancelDoc() {
        return totalCancelDoc;
    }

    public void setTotalCancelDoc(double totalCancelDoc) {
        this.totalCancelDoc = totalCancelDoc;
    }

    public double getNetTotalDoc() {
        return netTotalDoc;
    }

    public void setNetTotalDoc(double netTotalDoc) {
        this.netTotalDoc = netTotalDoc;
    }

    public Date getDate() {
        if (date == null) {
            date = new Date();
        }
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<AvalabelChannelDoctorRow> getAcdrs() {
        return acdrs;
    }

    public void setAcdrs(List<AvalabelChannelDoctorRow> acdrs) {
        this.acdrs = acdrs;
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
        Institution ins;
        List<Bill> bills;
        double departmentBillTotal;

        public Institution getIns() {
            return ins;
        }

        public void setIns(Institution ins) {
            this.ins = ins;
        }

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

        double billHos;
        double billHosVat;
        double billDoc;
        double billDocVat;
        double canHos;
        double canHosVat;
        double canDoc;
        double canDocVat;
        double refHos;
        double refHosVat;
        double refDoc;
        double refDocVat;

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

        public double getBillHos() {
            return billHos;
        }

        public void setBillHos(double billHos) {
            this.billHos = billHos;
        }

        public double getBillHosVat() {
            return billHosVat;
        }

        public void setBillHosVat(double billHosVat) {
            this.billHosVat = billHosVat;
        }

        public double getbDoc() {
            return billDoc;
        }

        public void setbDoc(double bDoc) {
            this.billDoc = bDoc;
        }

        public double getbDocVat() {
            return billDocVat;
        }

        public void setbDocVat(double bDocVat) {
            this.billDocVat = bDocVat;
        }

        public double getCanHos() {
            return canHos;
        }

        public void setCanHos(double canHos) {
            this.canHos = canHos;
        }

        public double getCanHosVat() {
            return canHosVat;
        }

        public void setCanHosVat(double canHosVat) {
            this.canHosVat = canHosVat;
        }

        public double getCanDoc() {
            return canDoc;
        }

        public void setCanDoc(double canDoc) {
            this.canDoc = canDoc;
        }

        public double getCanDocVat() {
            return canDocVat;
        }

        public void setCanDocVat(double canDocVat) {
            this.canDocVat = canDocVat;
        }

        public double getRefHos() {
            return refHos;
        }

        public void setRefHos(double refHos) {
            this.refHos = refHos;
        }

        public double getRefHosVat() {
            return refHosVat;
        }

        public void setRefHosVat(double refHosVat) {
            this.refHosVat = refHosVat;
        }

        public double getRefDoc() {
            return refDoc;
        }

        public void setRefDoc(double refDoc) {
            this.refDoc = refDoc;
        }

        public double getRefDocVat() {
            return refDocVat;
        }

        public void setRefDocVat(double refDocVat) {
            this.refDocVat = refDocVat;
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

    public class ChannelTotal {

        double hosFee;
        double staffFee;
        double Vat;
        double discount;
        double netTotal;

        public double getNetTotal() {
            return netTotal;
        }

        public void setNetTotal(double netTotal) {
            this.netTotal = netTotal;
        }

        public double getHosFee() {
            return hosFee;
        }

        public void setHosFee(double hosFee) {
            this.hosFee = hosFee;
        }

        public double getStaffFee() {
            return staffFee;
        }

        public void setStaffFee(double staffFee) {
            this.staffFee = staffFee;
        }

        public double getVat() {
            return Vat;
        }

        public void setVat(double Vat) {
            this.Vat = Vat;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

    }
//created inner class

    public class AvalabelChannelDoctorRow {

        private Staff s;
        private String time;
        private String timeEnd;
        private boolean leave;

        public Staff getS() {
            return s;
        }

        public void setS(Staff s) {
            this.s = s;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public boolean isLeave() {
            return leave;
        }

        public void setLeave(boolean leave) {
            this.leave = leave;
        }

        public String getTimeEnd() {
            return timeEnd;
        }

        public void setTimeEnd(String timeEnd) {
            this.timeEnd = timeEnd;
        }

    }

    public class StaffBookingWithCount {

        Staff staff;
        long count;

        public Staff getStaff() {
            return staff;
        }

        public void setStaff(Staff staff) {
            this.staff = staff;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public class AreaWithCount {

        Area area;
        long count;

        public Area getArea() {
            return area;
        }

        public void setArea(Area area) {
            this.area = area;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

    }

    public class StaffWithAreaRow {

        Staff staf;
        List<AreaWithCount> areaWithCounts;
        double subTotal;

        public Staff getStaf() {
            return staf;
        }

        public void setStaf(Staff staf) {
            this.staf = staf;
        }

        public List<AreaWithCount> getAreaWithCounts() {
            if (areaWithCounts == null) {
                areaWithCounts = new ArrayList<>();
            }
            return areaWithCounts;
        }

        public void setAreaWithCounts(List<AreaWithCount> areaWithCounts) {
            this.areaWithCounts = areaWithCounts;
        }

        public double getSubTotal() {
            return subTotal;
        }

        public void setSubTotal(double subTotal) {
            this.subTotal = subTotal;
        }
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public ChannelTotal getChannelTotal() {
        return channelTotal;
    }

    public void setChannelTotal(ChannelTotal channelTotal) {
        this.channelTotal = channelTotal;
    }

    public List<BookingCountSummryRow> getBookingCountSummryRowsScan() {
        return bookingCountSummryRowsScan;
    }

    public void setBookingCountSummryRowsScan(List<BookingCountSummryRow> bookingCountSummryRowsScan) {
        this.bookingCountSummryRowsScan = bookingCountSummryRowsScan;
    }

    public boolean isSummery() {
        return summery;
    }

    public void setSummery(boolean summery) {
        this.summery = summery;
    }

    public boolean isShowPatient() {
        return showPatient;
    }

    public void setShowPatient(boolean showPatient) {
        this.showPatient = showPatient;
    }

    public boolean isAgncy() {
        return agncy;
    }

    public void setAgncy(boolean agncy) {
        this.agncy = agncy;
    }

    public List<ArrivalRecord> getArrivalRecords() {
        return arrivalRecords;
    }

    public void setArrivalRecords(List<ArrivalRecord> arrivalRecords) {
        this.arrivalRecords = arrivalRecords;
    }

    public List<StaffBookingWithCount> getStaffBookingWithCounts() {
        return staffBookingWithCounts;
    }

    public void setStaffBookingWithCounts(List<StaffBookingWithCount> staffBookingWithCounts) {
        this.staffBookingWithCounts = staffBookingWithCounts;
    }

    public List<AreaWithCount> getAreaWithCount() {
        return areaWithCount;
    }

    public void setAreaWithCount(List<AreaWithCount> areaWithCount) {
        this.areaWithCount = areaWithCount;
    }

    public List<StaffWithAreaRow> getStaffWithAreaRows() {
        return staffWithAreaRows;
    }

    public void setStaffWithAreaRows(List<StaffWithAreaRow> staffWithAreaRows) {
        this.staffWithAreaRows = staffWithAreaRows;
    }

    public List<DoctorDayChannelCount> getDoctorDayChannelCounts() {
        return doctorDayChannelCounts;
    }

    public void setDoctorDayChannelCounts(List<DoctorDayChannelCount> doctorDayChannelCounts) {
        this.doctorDayChannelCounts = doctorDayChannelCounts;
    }

}
