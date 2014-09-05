/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ejb;

import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.dataStructure.ChannelFee;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillSession;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.ServiceSessionLeave;
import com.divudi.entity.Staff;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.ServiceSessionLeaveFacade;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Stateless
public class ChannelBean {

    @EJB
    private FinalVariables finalVariables;
    ////////
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private ServiceSessionLeaveFacade serviceSessionLeaveFacade;

    public ChannelFee getChannelFee(BillSession bs, FeeType feeType) {
        ChannelFee doctorFee = new ChannelFee();

        for (BillFee bf : getBillFee(bs)) {
            if (bf.getFee().getFeeType() == feeType) {
                if (bf.getBill().getBillType() == BillType.ChannelPaid) {
                    doctorFee.setBilledFee(bf);
                } else {
                    doctorFee.setBilledFee(new BillFee());
                }
            }
        }

        for (BillFee bf : getRefundBillFee(bs)) {
            if (bf.getFee().getFeeType() == feeType) {
                if (bf.getBill().getBillType() == BillType.ChannelPaid) {
                    doctorFee.setPrevFee(bf);
                } else {
                    doctorFee.setPrevFee(new BillFee());
                }
            }
        }

        doctorFee.setRepayment(new BillFee());

        return doctorFee;
    }

    public List<BillFee> getRefundBillFee(BillSession bs) {
        List<BillFee> refundBillFee = new ArrayList<>();
        if (bs != null) {
            //String sql = "Select s From BillFee s where s.retired=false and s.bill.id=" + billSession.getBill().getId();
            String sql = "Select s From BillFee s where s.retired=false and s.bill.billedBill.id=" + bs.getBill().getId();
            refundBillFee = getBillFeeFacade().findBySQL(sql);
        }

        return refundBillFee;
    }

    public List<BillFee> getBillFee(BillSession bs) {
        List<BillFee> billFee = new ArrayList<>();
        if (bs != null) {
            String sql = "Select s From BillFee s where s.retired=false and s.bill.id=" + bs.getBill().getId();
            billFee = getBillFeeFacade().findBySQL(sql);
        }

        return billFee;
    }

    public int getBillSessionsCount(ServiceSession ss) {

        String sql = "Select bs From BillSession bs where bs.retired=false and bs.serviceSession.id=" + ss.getId() + " and bs.sessionDate= :ssDate";
        HashMap hh = new HashMap();
        hh.put("ssDate", ss.getSessionAt());
        List<BillSession> billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);

        return billSessions.size();
    }

    private boolean checkLeaveDate(Date date, Staff staff) {
        String slq = "Select s From ServiceSessionLeave s Where s.sessionDate=:dt and s.staff=:st";
        HashMap hm = new HashMap();
        hm.put("dt", date);
        hm.put("st", staff);
        List<ServiceSessionLeave> tmp = getServiceSessionLeaveFacade().findBySQL(slq, hm, TemporalType.DATE);
        return !tmp.isEmpty();
    }

    public List<ServiceSession> generateDailyServiceSessionsFromWeekdaySessions(List<ServiceSession> sessions) {
        int sessionDayCount = 0;
        List<ServiceSession> serviceSessions = new ArrayList<>();
        Date nowDate = Calendar.getInstance().getTime();

        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 1);
        Date toDate = c.getTime();
        Integer tmp = 0;
        int rowIndex = 0;

        while (toDate.after(nowDate) && sessionDayCount < getFinalVariables().getSessionSessionDayCounter()) {
            boolean hasSpecificDateSession = false;

            if (checkLeaveDate(nowDate, sessions.get(0).getStaff())) {
                continue;
            }

            for (ServiceSession ss : sessions) {
                if (ss.getSessionDate() != null) {
                    Calendar sessionDate = Calendar.getInstance();
                    sessionDate.setTime(ss.getSessionDate());
                    Calendar nDate = Calendar.getInstance();
                    nDate.setTime(nowDate);

                    if (sessionDate.get(Calendar.DATE) == nDate.get(Calendar.DATE)) {
                        hasSpecificDateSession = true;
                        ServiceSession newSs = new ServiceSession();
                        newSs.setName(ss.getName());
                        newSs.setMaxNo(ss.getMaxNo());
                        newSs.setStartingTime(ss.getStartingTime());
                        newSs.setSessionWeekday(ss.getSessionWeekday());
                        newSs.setHospitalFee(ss.getHospitalFee());
                        newSs.setProfessionalFee(ss.getProfessionalFee());
                        newSs.setId(ss.getId());
                        newSs.setSessionAt(nowDate);
                        newSs.setDisplayCount(getBillSessionsCount(newSs));
                        newSs.setStaff(ss.getStaff());
                        //Temprory
                        newSs.setRoomNo(rowIndex++);
                        ////System.out.println("Specific Count : " + sessionDayCount);
                        serviceSessions.add(newSs);

                        if (!Objects.equals(tmp, ss.getSessionWeekday())) {
                            sessionDayCount++;
                        }
                    }
                }
            }

            if (hasSpecificDateSession == false) {
                for (ServiceSession ss : sessions) {
                    Calendar wdc = Calendar.getInstance();
                    wdc.setTime(nowDate);
                    if (ss.getSessionWeekday() == wdc.get(Calendar.DAY_OF_WEEK)) {
                        ServiceSession newSs = new ServiceSession();
                        newSs.setName(ss.getName());
                        newSs.setMaxNo(ss.getMaxNo());
                        newSs.setStartingTime(ss.getStartingTime());
                        newSs.setSessionWeekday(ss.getSessionWeekday());
                        newSs.setHospitalFee(ss.getHospitalFee());
                        newSs.setProfessionalFee(ss.getProfessionalFee());
                        newSs.setId(ss.getId());
                        newSs.setSessionAt(nowDate);
                        newSs.setDisplayCount(getBillSessionsCount(newSs));
                        newSs.setStaff(ss.getStaff());
                        //Temprory
                        newSs.setRoomNo(rowIndex++);
                        // //System.out.println("Count : " + sessionDayCount);

                        serviceSessions.add(newSs);

                        if (!Objects.equals(tmp, ss.getSessionWeekday())) {
                            sessionDayCount++;
                        }
                    }

                }
            }

            Calendar nc = Calendar.getInstance();
            nc.setTime(nowDate);
            nc.add(Calendar.DATE, 1);
            nowDate = nc.getTime();

        }
        return serviceSessions;
    }

    public Date calSessionTime(ServiceSession serviceSession) {
        Calendar starting = Calendar.getInstance();
        starting.setTime(serviceSession.getStartingTime());
        int count = getBillSessionsCount(serviceSession);

        if (count == 0) {
            return starting.getTime();
        }

        if (serviceSession.getDuration() != 0.0) {
            starting.add(Calendar.MINUTE, (int) (count * serviceSession.getDuration()));
        } else {
            starting.add(Calendar.MINUTE, (int) (count * getFinalVariables().getCahnnelingDurationMinute()));
        }

        return starting.getTime();

    }

    public FinalVariables getFinalVariables() {
        return finalVariables;
    }

    public void setFinalVariables(FinalVariables finalVariables) {
        this.finalVariables = finalVariables;
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

    public ServiceSessionLeaveFacade getServiceSessionLeaveFacade() {
        return serviceSessionLeaveFacade;
    }

    public void setServiceSessionLeaveFacade(ServiceSessionLeaveFacade serviceSessionLeaveFacade) {
        this.serviceSessionLeaveFacade = serviceSessionLeaveFacade;
    }
}
