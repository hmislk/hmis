/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.dataStructure.ChannelDoctor;
import com.divudi.ejb.ChannelBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
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
import java.util.Calendar;
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
public class ChannelReportController implements Serializable {

    private ServiceSession serviceSession;
    private double billedTotalFee;
    private double repayTotalFee;
    private double taxTotal;
    private double total;
    ///////
    private List<BillSession> billSessions;
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

    public void makeNull() {
        serviceSession = null;
        billSessions = null;
    }

    public List<BillSession> getBillSessionsNurse() {
        billSessions = new ArrayList<BillSession>();
        if (serviceSession != null) {
            String sql = "Select bs From BillSession bs where bs.retired=false and bs.bill.cancelled=false and bs.bill.refunded=false and bs.serviceSession.id=" + serviceSession.getId() + " and bs.sessionDate= :ssDate";
            HashMap hh = new HashMap();
            hh.put("ssDate", serviceSession.getSessionAt());
            billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);

        }

        return billSessions;
    }

    public List<ChannelDoctor> getTotalPatient() {

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
                        if (bs.getPresent() == false) {
                            cd.setAbsentCount(cd.getAbsentCount() + 1);
                        }
                    }
                }
            }

        }

        return channelDoctors;
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
                        if (bs.getPresent() == false) {
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
}
