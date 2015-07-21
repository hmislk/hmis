/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
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
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.facade.util.JsfUtil;
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

    @EJB
    DepartmentFacade departmentFacade;

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

        //System.out.println("j = " + j);

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

        //System.out.println("Billed,Cancell,Refund" + totalBilled + "," + totalCancel + "," + totalRefund);
        if (pay == PaymentMethod.Cash) {
            //System.out.println("payment method=" + pay);
            //System.out.println("Billed,Cancell,Refund" + totalBilled + "," + totalCancel + "," + totalRefund);
            totalBilled += calCashierNetTotal(new BilledBill(), pay, BillType.ChannelPaid);
            totalCancel += calCashierNetTotal(new CancelledBill(), pay, BillType.ChannelPaid);
            totalRefund += calCashierNetTotal(new RefundBill(), pay, BillType.ChannelPaid);
            //System.out.println("netTotal" + netTotal);
        }
        netTotal = totalBilled + totalCancel + totalRefund;
        //System.out.println("netTotal = " + netTotal);

        chm.setPaymentMethod(pay);
        chm.setBilledTotal(totalBilled);
        chm.setCancellTotal(totalCancel);
        chm.setRefundTotal(totalRefund);

        chm.setTotal(netTotal);

        //System.out.println("chmlst = " + chmlst);
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
        //System.out.println("getBillSessionFacade().findBySQL(sql, m) = " + getBillSessionFacade().findBySQL(sql, m));
        List<BillSession> billSessions = getBillSessionFacade().findBySQL(sql, m);
        //System.out.println("billSessions = " + billSessions.size());
        for (BillSession bs : billSessions) {
            //System.out.println("In");
            bs.getBill().setSingleBillSession(bs);
            //System.out.println("bs.getSingleBillSession() = " + bs.getBill().getSingleBillSession());
            getBillFacade().edit(bs.getBill());
            //System.out.println("Out");
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

        return billFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);

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
    
    public double calTotal(List<Bill> bills){
        
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

    public List<Department> getDepartments() {
        String sql;
        HashMap hm = new HashMap();
        sql = " select d from Department d "
                + " where d.retired=false "
                + " order by d.name";
        return getDepartmentFacade().findBySQL(sql, hm);
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
        return billFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);
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

        return getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

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
        cashiers = getWebUserFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
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

        billFeeList = getBillFeeFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

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
    
    public double calFeeTypeTotal(List<BillFee> billFees){
        
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

        billFeeList = getBillFeeFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

        return billFeeList;
    }
    
    FeeType feeType; 
    List<BillFee> listBilledBillFees;
    List<BillFee> listCanceledBillFees;
    List<BillFee> listRefundBillFees;
   
    public void getUsersWithFeeType(){
        if(getFeeType() == null){
            JsfUtil.addErrorMessage("Please Insert Fee Type");
        }else{
            listBilledBillFees = getBillFeeWithFeeTypes(new BilledBill(), getFeeType());
            listCanceledBillFees = getBillFeeWithFeeTypes(new CancelledBill(), getFeeType());
            listRefundBillFees = getBillFeeWithFeeTypes(new RefundBill(), getFeeType());
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

    List<Bill> channelBills;

    public void channelBillList() {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        HashMap hm = new HashMap();

        String sql = " select b from Bill b "
                + " where b.billType in :bt "
                + " and b.retired=false "
                + " and b.createdAt between :fDate and :tDate "
                + " order by b.singleBillSession.sessionDate ";

        hm.put("bt", bts);
        hm.put("fDate", getFromDate());
        hm.put("tDate", getToDate());

        channelBills = billFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);

    }

    public void createChannelFees() {
        valueList = new ArrayList<>();
        FeeType[] fts = {FeeType.Staff, FeeType.OwnInstitution, FeeType.OtherInstitution, FeeType.Service};

        for (FeeType ft : fts) {
            setFeeTotals(valueList, ft);
        }
        calTotals(valueList);

        //System.out.println("***Done***");
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

        //System.out.println("*************");
        //System.out.println("Fee - " + s1v3.getString());
        //System.out.println("Bill - " + s1v3.getValue1());
        //System.out.println("Can - " + s1v3.getValue2());
        //System.out.println("Ref - " + s1v3.getValue3());

        s1v3s.add(s1v3);
        //System.out.println("Add");
        //System.out.println("*************");
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

}
