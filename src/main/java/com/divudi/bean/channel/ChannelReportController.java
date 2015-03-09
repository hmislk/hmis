/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.channel.DateEnum;
import com.divudi.data.channel.PaymentEnum;
import com.divudi.data.dataStructure.ChannelDoctor;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.data.table.String1Value3;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.CommonFunctions;
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
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillSessionFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
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
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class ChannelReportController implements Serializable {

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
    List<String1Value3> valueList;
    ReportKeyWord reportKeyWord;
    Date fromDate;
    Date toDate;
    Institution institution;
    WebUser webUser;
    ChannelBillTotals billTotals;
    Department department;
    private List<ChannelDoctor> channelDoctors;
    /////
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillFacade billFacade;
    ///////////
    @EJB
    private ChannelBean channelBean;
    @Inject
    SessionController sessionController;

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

        System.out.println("j = " + j);

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

        return billSessionFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);
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

        if (webUser != null) {
            doctorFeeTotal = calDoctorFeeNetTotal(pms, btys, FeeType.Staff, webUser);
        }
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
        if (webUser != null) {
            totalBilled = calCashierNetTotal(new BilledBill(), pay, bty, webUser);
            totalCancel = calCashierNetTotal(new CancelledBill(), pay, bty, webUser);
            totalRefund = calCashierNetTotal(new RefundBill(), pay, bty, webUser);
        }

        totalBilled = calCashierNetTotal(new BilledBill(), pay, bty);
        totalCancel = calCashierNetTotal(new CancelledBill(), pay, bty);
        totalRefund = calCashierNetTotal(new RefundBill(), pay, bty);

        System.out.println("Billed,Cancell,Refund" + totalBilled + "," + totalCancel + "," + totalRefund);
        if (pay == PaymentMethod.Cash) {
            System.out.println("payment method=" + pay);
            System.out.println("Billed,Cancell,Refund" + totalBilled + "," + totalCancel + "," + totalRefund);
            totalBilled += calCashierNetTotal(new BilledBill(), pay, BillType.ChannelPaid);
            totalCancel += calCashierNetTotal(new CancelledBill(), pay, BillType.ChannelPaid);
            totalRefund += calCashierNetTotal(new RefundBill(), pay, BillType.ChannelPaid);
            System.out.println("netTotal" + netTotal);
        }
        netTotal = totalBilled - (totalCancel + totalRefund);
        System.out.println("netTotal = " + netTotal);

        chm.setPaymentMethod(pay);
        chm.setBilledTotal(totalBilled);
        chm.setCancellTotal(totalCancel);
        chm.setRefundTotal(totalRefund);

        chm.setTotal(netTotal);

        System.out.println("chmlst = " + chmlst);
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

        hm.put("class", bill.getClass());
        hm.put("bt", billType);
        hm.put("pm", paymentMethod);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());

        return billFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double calCashierNetTotal(Bill bill, PaymentMethod paymentMethod, BillType billType, WebUser webUser) {
        HashMap hm = new HashMap();

        String sql = " SELECT sum(b.netTotal) from Bill b "
                + " where type(b)=:class "
                + " and b.retired=false "
                + " and b.billType =:bt "
                + " and b.paymentMethod=:pm "
                + " and b.creater=:wu "
                + " and b.createdAt between :frm and :to ";

        hm.put("class", bill.getClass());
        hm.put("bt", billType);
        hm.put("pm", paymentMethod);
        hm.put("wu", webUser);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());

        return billFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double calDoctorFeeNetTotal(List<PaymentMethod> paymentMethod, List<BillType> billType, FeeType ftp) {
        HashMap hm = new HashMap();

        String sql = " SELECT sum(b.feeValue) from BillFee b "
                //+ " where type(b)=:class "
                + " where b.bill.retired=false "
                + " and b.bill.billType in :bt "
                + " and b.bill.paymentMethod in :pm "
                + " and b.fee.feeType=:ft"
                + " and b.bill.createdAt between :frm and :to ";

        //hm.put("class", bill.getClass());
        hm.put("bt", billType);
        hm.put("pm", paymentMethod);
        hm.put("ft", ftp);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());

        return billFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double calDoctorFeeNetTotal(List<PaymentMethod> paymentMethod, List<BillType> billType, FeeType ftp, WebUser webUser) {
        HashMap hm = new HashMap();

        String sql = " SELECT sum(b.feeValue) from BillFee b "
                //+ " where type(b)=:class "
                + " where b.bill.retired=false "
                + " and b.bill.billType in :bt "
                + " and b.bill.paymentMethod in :pm "
                + " and b.fee.feeType=:ft "
                + " and b.bill.creater=:wu "
                + " and b.bill.createdAt between :frm and :to ";

        //hm.put("class", bill.getClass());
        hm.put("bt", billType);
        hm.put("pm", paymentMethod);
        hm.put("ft", ftp);
        hm.put("wu", webUser);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());

        return billFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

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
        System.out.println("getBillSessionFacade().findBySQL(sql, m) = " + getBillSessionFacade().findBySQL(sql, m));
        List<BillSession> billSessions = getBillSessionFacade().findBySQL(sql, m);
        System.out.println("billSessions = " + billSessions.size());
        for (BillSession bs : billSessions) {
            System.out.println("In");
            bs.getBill().setSingleBillSession(bs);
            System.out.println("bs.getSingleBillSession() = " + bs.getBill().getSingleBillSession());
            getBillFacade().edit(bs.getBill());
            System.out.println("Out");
        }
    }

    public void createChannelFees() {
        valueList = new ArrayList<>();
        FeeType[] fts = {FeeType.Staff, FeeType.OwnInstitution, FeeType.OtherInstitution, FeeType.Service};

        for (FeeType ft : fts) {
            setFeeTotals(valueList, ft);
        }
        calTotals(valueList);

        System.out.println("***Done***");
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

        System.out.println("*************");
        System.out.println("Fee - " + s1v3.getString());
        System.out.println("Bill - " + s1v3.getValue1());
        System.out.println("Can - " + s1v3.getValue2());
        System.out.println("Ref - " + s1v3.getValue3());

        s1v3s.add(s1v3);
        System.out.println("Add");
        System.out.println("*************");
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

    public void fillNurseView() {
        nurseViewSessions = new ArrayList<>();
        if (serviceSession != null) {
            String sql = "Select bs From BillSession bs "
                    + " where bs.retired=false and "
                    + " bs.bill.cancelled=false and "
                    + " bs.bill.refunded=false and "
                    + " bs.bill.billType in :tbs and "
                    + " bs.serviceSession.id=" + serviceSession.getId() + " and bs.sessionDate= :ssDate"
                    + " order by bs.serialNo";
            HashMap hh = new HashMap();
            hh.put("ssDate", serviceSession.getSessionAt());
            List<BillType> bts = new ArrayList<>();
            bts.add(BillType.ChannelAgent);
            bts.add(BillType.ChannelCash);
            bts.add(BillType.ChannelOnCall);
            hh.put("tbs", bts);
            nurseViewSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);
        }
    }

    public void fillDoctorView() {
        doctorViewSessions = new ArrayList<>();
        if (serviceSession != null) {
            String sql = "Select bs From BillSession bs "
                    + " where bs.retired=false and "
                    + " bs.bill.cancelled=false and "
                    + " bs.bill.refunded=false and "
                    + " bs.bill.billType in :tbs and "
                    + " bs.serviceSession.id=" + serviceSession.getId() + " and bs.sessionDate= :ssDate"
                    + " order by bs.serialNo";
            HashMap hh = new HashMap();
            hh.put("ssDate", serviceSession.getSessionAt());
            List<BillType> bts = new ArrayList<>();
            bts.add(BillType.ChannelAgent);
            bts.add(BillType.ChannelCash);
            bts.add(BillType.ChannelOnCall);
            hh.put("tbs", bts);
            doctorViewSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);
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
            hh.put("ssDate", serviceSession.getSessionAt());
            List<BillType> bts = new ArrayList<>();
            bts.add(BillType.ChannelAgent);
            bts.add(BillType.ChannelCash);
            bts.add(BillType.ChannelOnCall);
            hh.put("tbs", bts);
            billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);
        }
        return billSessions;
    }

    public List<ChannelDoctor> getTotalPatient() {

        channelDoctors = new ArrayList<>();

        String sql = "Select bs.bill From BillSession bs where bs.bill.staff is not null and bs.retired=false and bs.sessionDate= :ssDate";
        HashMap hh = new HashMap();
        hh.put("ssDate", Calendar.getInstance().getTime());
        List<Bill> bills = getBillFacade().findBySQL(sql, hh, TemporalType.DATE);

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
                        BillSession bs = getBillSessionFacade().findFirstBySQL("select b from BillSession b where b.retired=false and b.bill.id=" + b.getId());
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
        List<Bill> bills = getBillFacade().findBySQL(sql, hh, TemporalType.DATE);

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
                        BillSession bs = getBillSessionFacade().findFirstBySQL("select b from BillSession b where b.retired=false and b.bill.id=" + b.getId());
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
            billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);

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
            billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);

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
        billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);

        for (BillSession bs : billSessions) {
            bs.setDoctorFee(getChannelBean().getChannelFee(bs, FeeType.Staff));
            bs.setTax(getChannelBean().getChannelFee(bs, FeeType.Tax));

            setBilledTotalFee(getBilledTotalFee() + bs.getDoctorFee().getBilledFee().getFeeValue());
            setRepayTotalFee(getRepayTotalFee() + bs.getDoctorFee().getPrevFee().getFeeValue());
            setTaxTotal(getTaxTotal() + bs.getTax().getBilledFee().getFeeValue());
        }

        return billSessions;
    }

    /**
     * Creates a new instance of ChannelReportController
     */
    public ChannelReportController() {
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

    List<ChannelReportColumnModel> columns;

    public List<ChannelReportColumnModel> getColumns() {
        return columns;
    }

    public void setColumns(List<ChannelReportColumnModel> columns) {
        this.columns = columns;
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

}
