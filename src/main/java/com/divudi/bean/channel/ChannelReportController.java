/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.channel.DateEnum;
import com.divudi.data.channel.PaymentEnum;
import com.divudi.data.dataStructure.ChannelDoctor;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.data.table.String1Value1;
import com.divudi.data.table.String1Value3;
import com.divudi.ejb.ChannelBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Staff;
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
    List<String1Value3> valueList;
    ReportKeyWord reportKeyWord;
    Date fromDate;
    Date toDate;
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
                + " and b.bill.retired=false";

        if (reportKeyWord.getSpeciality() != null) {
            sql += "and b.staff.speciality=:sp";
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

        sql += " group by b.bill.billType "
                + " order by b.bill.billType ";

        hm.put("bt", bts);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("class", bill.getClass());

        return billSessionFacade.findAggregates(sql, hm, TemporalType.TIMESTAMP);
    }

    public void createBillSession_report_1() {
        billSessionsBilled = createBillSessionQuery(new BilledBill(), PaymentEnum.Paid, DateEnum.AppointmentDate, getReportKeyWord());
        billSessionsCancelled = createBillSessionQuery(new CancelledBill(), PaymentEnum.Paid, DateEnum.CreatedDate, getReportKeyWord());
        billSessionsReturn = createBillSessionQuery(new RefundBill(), PaymentEnum.Paid, DateEnum.CreatedDate, getReportKeyWord());

        valueList=new ArrayList<>();
        
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
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
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

    public List<BillSession> getBillSessionsNurse() {
        billSessions = new ArrayList<>();
        if (serviceSession != null) {
            String sql = "Select bs From BillSession bs where bs.retired=false and bs.bill.cancelled=false and bs.bill.refunded=false and bs.serviceSession.id=" + serviceSession.getId() + " and bs.sessionDate= :ssDate";
            HashMap hh = new HashMap();
            hh.put("ssDate", serviceSession.getSessionAt());
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

}
