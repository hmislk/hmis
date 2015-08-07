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
import com.divudi.entity.BilledBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.ServiceSessionLeave;
import com.divudi.entity.Staff;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.ServiceSessionLeaveFacade;
import java.util.ArrayList;
import java.util.Arrays;
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

    public int getBillSessionsCount(ServiceSession ss, Date date) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        String sql = "Select count(bs) From BillSession bs "
                + " where bs.retired=false"
                + " and bs.serviceSession =:ser "
                + " and bs.bill.billType in :bt"
                + " and type(bs.bill)=:class "
                + " and bs.sessionDate= :ssDate";
        HashMap hh = new HashMap();
        hh.put("ssDate", date);
        hh.put("ser", ss);
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        Long lg = getBillSessionFacade().findAggregateLong(sql, hh, TemporalType.DATE);

        return lg.intValue();
    }
    
    public int getBillSessionsCountWithOutCancelRefund(ServiceSession ss, Date date) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        String sql = "Select count(bs) From BillSession bs "
                + " where bs.retired=false"
                + " and bs.serviceSession =:ser "
                + " and bs.bill.billType in :bt"
                + " and type(bs.bill)=:class "
                + " and bs.sessionDate= :ssDate "
                + " and bs.bill.cancelled=false "
                + " and bs.bill.refunded=false ";
        HashMap hh = new HashMap();
        hh.put("ssDate", date);
        hh.put("ser", ss);
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        Long lg = getBillSessionFacade().findAggregateLong(sql, hh, TemporalType.DATE);

        return lg.intValue();
    }
    
    public int getBillSessionsCountCrditBill(ServiceSession ss, Date date) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        String sql = "Select count(bs) From BillSession bs "
                + " where bs.retired=false"
                + " and bs.serviceSession =:ser "
                + " and bs.bill.billType in :bt"
                + " and type(bs.bill)=:class "
                + " and bs.sessionDate= :ssDate "
                + " and bs.bill.paidAmount=:pa ";
        HashMap hh = new HashMap();
        hh.put("ssDate", date);
        hh.put("ser", ss);
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        hh.put("pa", 0.0);
        Long lg = getBillSessionFacade().findAggregateLong(sql, hh, TemporalType.DATE);

        return lg.intValue();
    }

    public List<ServiceSession> setSessionAt(List<ServiceSession> sessions) {
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
                        newSs.setStaff(ss.getStaff());
                        //Temprory
                        newSs.setRoomNo(rowIndex++);
                        //////System.out.println("Specific Count : " + sessionDayCount);
                        serviceSessions.add(newSs);

                        if (tmp != ss.getSessionWeekday()) {
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
                        newSs.setStaff(ss.getStaff());
                        //Temprory
                        newSs.setRoomNo(rowIndex++);
                        // ////System.out.println("Count : " + sessionDayCount);

                        serviceSessions.add(newSs);

                        if (tmp != ss.getSessionWeekday()) {
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

    private boolean checkLeaveDate(Date date, Staff staff) {
        System.err.println("Leave Staff " + staff);
        System.err.println("Date " + date);
        String slq = "Select s From ServiceSessionLeave s"
                + "  Where s.sessionDate=:dt"
                + "  and s.staff=:st"
                + " and s.retired=false ";
        HashMap hm = new HashMap();
        hm.put("dt", date);
        hm.put("st", staff);
        ServiceSessionLeave tmp = getServiceSessionLeaveFacade().findFirstBySQL(slq, hm, TemporalType.DATE);

        if (tmp != null) {
            return true;
        } else {
            return false;
        }
    }

    public List<ServiceSession> generateDailyServiceSessionsFromWeekdaySessions(List<ServiceSession> inputSessions) {
        int sessionDayCount = 0;
        System.err.println("Passing Sessions " + inputSessions);
        List<ServiceSession> createdSessions = new ArrayList<>();

        if (inputSessions == null || inputSessions.isEmpty()) {
            return createdSessions;
        }

        Date nowDate = Calendar.getInstance().getTime();

        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 1);
        Date toDate = c.getTime();
        Integer tmp = 0;
        int rowIndex = 0;

        while (toDate.after(nowDate) && sessionDayCount < getFinalVariables().getSessionSessionDayCounter()) {
            boolean hasSpecificDateSession = false;
//            System.err.println("SESSSION");
            if (checkLeaveDate(nowDate, inputSessions.get(0).getStaff())) {
                System.err.println("INSIDE");
                Calendar nc = Calendar.getInstance();
                nc.setTime(nowDate);
                nc.add(Calendar.DATE, 1);
                nowDate = nc.getTime();
                continue;
            }
//            System.err.println("After Check Leave");

            for (ServiceSession ss : inputSessions) {
//                System.err.println("@@@1");
                if (ss.getSessionDate() != null) {
                    Calendar sessionDate = Calendar.getInstance();
                    sessionDate.setTime(ss.getSessionDate());
                    Calendar nDate = Calendar.getInstance();
                    nDate.setTime(nowDate);

                    if (sessionDate.get(Calendar.DATE) == nDate.get(Calendar.DATE)) {
                        hasSpecificDateSession = true;
                        ServiceSession newSs = new ServiceSession();
                        newSs.setOriginatingSession(ss);
                        newSs.setName(ss.getName());
                        newSs.setMaxNo(ss.getMaxNo());
                        newSs.setStartingTime(ss.getStartingTime());
                        newSs.setSessionWeekday(ss.getSessionWeekday());
                        newSs.setHospitalFee(ss.getHospitalFee());
                        newSs.setProfessionalFee(ss.getProfessionalFee());
                        newSs.setId(ss.getId());
                        newSs.setDepartment(ss.getDepartment());
                        newSs.setInstitution(ss.getInstitution());
                        newSs.setSessionAt(nowDate);
                        newSs.setSessionDate(nowDate);
                        newSs.setDisplayCount(getBillSessionsCount(ss, nowDate));
                        newSs.setTransDisplayCountWithoutCancelRefund(getBillSessionsCountWithOutCancelRefund(ss, nowDate));
                        newSs.setTransCreditBillCount(getBillSessionsCountCrditBill(ss, nowDate));
                        newSs.setStaff(ss.getStaff());
                        System.out.println("getBillSessionsCountWithOutCancelRefund(ss, nowDate) = " + getBillSessionsCountWithOutCancelRefund(ss, nowDate));
                        System.out.println("getBillSessionsCountCrditBill(ss, nowDate) = " + getBillSessionsCountCrditBill(ss, nowDate));
                        System.out.println("newSs.getDepartment() = " + newSs.getDepartment());
                        System.out.println("newSs.getInstitution() = " + newSs.getInstitution());
                        //Temprory
                        newSs.setRoomNo(rowIndex++);
                        //////System.out.println("Specific Count : " + sessionDayCount);
                        createdSessions.add(newSs);

                        if (!Objects.equals(tmp, ss.getSessionWeekday())) {
                            sessionDayCount++;
                        }
                    }
                }
            }

//            System.err.println("@@@4");
            if (hasSpecificDateSession == false) {
//                System.err.println("@@@41");
                for (ServiceSession ss : inputSessions) {
//                    System.err.println("@@@42");
                    Calendar wdc = Calendar.getInstance();
                    wdc.setTime(nowDate);
//                    System.err.println("@@@421");
//                    System.err.println("Week " + ss.getSessionWeekday());
//                    System.err.println("WW " + wdc);
//                    System.err.println("WDC " + wdc.get(Calendar.DAY_OF_WEEK));

                    if (ss.getSessionWeekday() != null && (ss.getSessionWeekday() == wdc.get(Calendar.DAY_OF_WEEK))) {
//                        System.err.println("@@@43");
                        ServiceSession newSs = new ServiceSession();
                        newSs.setName(ss.getName());
                        newSs.setOriginatingSession(ss);
                        newSs.setMaxNo(ss.getMaxNo());
                        newSs.setStartingTime(ss.getStartingTime());
                        newSs.setSessionWeekday(ss.getSessionWeekday());
                        newSs.setHospitalFee(ss.getHospitalFee());
                        newSs.setProfessionalFee(ss.getProfessionalFee());
                        newSs.setId(ss.getId());
                        newSs.setDepartment(ss.getDepartment());
                        newSs.setInstitution(ss.getInstitution());
                        System.out.println("newSs.getDepartment() 2= " + newSs.getDepartment());
                        System.out.println("newSs.getInstitution() 2= " + newSs.getInstitution());
                        newSs.setSessionAt(nowDate);
                        newSs.setDisplayCount(getBillSessionsCount(ss, nowDate));
                        newSs.setTransDisplayCountWithoutCancelRefund(getBillSessionsCountWithOutCancelRefund(ss, nowDate));
                        newSs.setTransCreditBillCount(getBillSessionsCountCrditBill(ss, nowDate));
                        System.out.println("getBillSessionsCountWithOutCancelRefund(ss, nowDate) = " + getBillSessionsCountWithOutCancelRefund(ss, nowDate));
                        System.out.println("getBillSessionsCountCrditBill(ss, nowDate) = " + getBillSessionsCountCrditBill(ss, nowDate));
                        newSs.setStaff(ss.getStaff());
                        newSs.setSessionDate(nowDate);
                        //Temprory
                        newSs.setRoomNo(rowIndex++);
                        // ////System.out.println("Count : " + sessionDayCount);
//                        System.err.println("@@@45");
                        createdSessions.add(newSs);
//                        System.err.println("@@@46");
                        if (!Objects.equals(tmp, ss.getSessionWeekday())) {
                            sessionDayCount++;
                        }
//                        System.err.println("@@@47");
                    }
//                    System.err.println("@@@471");

                }
//                System.err.println("@@@48");
            }

//            System.err.println("@@@5");
            Calendar nc = Calendar.getInstance();
            nc.setTime(nowDate);
            nc.add(Calendar.DATE, 1);
            nowDate = nc.getTime();

        }

        System.err.println("Created Sessions  " + createdSessions);
        return createdSessions;
    }

    public List<ServiceSession> generateServiceSessionsForSelectedDate(List<ServiceSession> inputSessions,Date date) {
        int sessionDayCount = 0;
        System.err.println("Passing Sessions " + inputSessions);
        List<ServiceSession> createdSessions = new ArrayList<>();

        if (inputSessions == null || inputSessions.isEmpty()) {
            return createdSessions;
        }

        Integer tmp = 0;
        int rowIndex = 0;

        for (ServiceSession ss : inputSessions) {
            ServiceSession newSs = new ServiceSession();
            newSs.setOriginatingSession(ss);
            newSs.setName(ss.getName());
            newSs.setMaxNo(ss.getMaxNo());
            newSs.setStartingTime(ss.getStartingTime());
            newSs.setSessionWeekday(ss.getSessionWeekday());
            newSs.setHospitalFee(ss.getHospitalFee());
            newSs.setProfessionalFee(ss.getProfessionalFee());
            newSs.setId(ss.getId());
            newSs.setSessionAt(date);
            newSs.setSessionDate(date);
            newSs.setDisplayCount(getBillSessionsCount(ss, date));
            newSs.setStaff(ss.getStaff());
            //Temprory
            newSs.setRoomNo(rowIndex++);
            //////System.out.println("Specific Count : " + sessionDayCount);
            createdSessions.add(newSs);

        }
        System.err.println("Created Sessions  " + createdSessions);
        return createdSessions;
    }

    public Date calSessionTime(ServiceSession serviceSession) {
        Calendar starting = Calendar.getInstance();
        starting.setTime(serviceSession.getStartingTime());
        int count = getBillSessionsCount(serviceSession, serviceSession.getSessionAt());

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
