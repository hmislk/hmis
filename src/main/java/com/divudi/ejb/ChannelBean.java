/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.bean.channel.BookingController;
import com.divudi.bean.channel.ChannelBillController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.ApplicationInstitution;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.dataStructure.ChannelFee;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.ServiceSessionLeave;
import com.divudi.entity.SessionNumberGenerator;
import com.divudi.entity.Staff;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.ServiceSessionLeaveFacade;
import com.divudi.facade.SessionNumberGeneratorFacade;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
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
    @EJB
    private ServiceSessionFacade serviceSessionFacade;
    @EJB
    SessionNumberGeneratorFacade sessionNumberGeneratorFacade;
    @Inject
    SessionController sessionController;
    @Inject
    BookingController bookingController;
    @Inject
    ChannelBillController channelBillController;

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

    public int getBillSessionsCount(long ss, Date date) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        String sql = "Select count(bs) From BillSession bs "
                + " where bs.retired=false"
                + " and bs.serviceSession.id =:ser "
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
                + " and bs.bill.paidAmount=:pa "
                + " and bs.bill.cancelled=false "
                + " and bs.bill.refunded=false ";
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
                        ////////// // System.out.println("Specific Count : " + sessionDayCount);
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
                        // //////// // System.out.println("Count : " + sessionDayCount);

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
                if (getSessionController().getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna) {
                    createDocLeaveSession(createdSessions, nowDate, rowIndex);
                    rowIndex++;
                }
//                System.err.println("INSIDE");
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
                        newSs.setDeactivated(false);
                        newSs.setStaff(ss.getStaff());
//                        //// // System.out.println("getBillSessionsCountWithOutCancelRefund(ss, nowDate) = " + getBillSessionsCountWithOutCancelRefund(ss, nowDate));
//                        //// // System.out.println("getBillSessionsCountCrditBill(ss, nowDate) = " + getBillSessionsCountCrditBill(ss, nowDate));
//                        //// // System.out.println("newSs.getDepartment() = " + newSs.getDepartment());
//                        //// // System.out.println("newSs.getInstitution() = " + newSs.getInstitution());
                        //Temprory
                        newSs.setRoomNo(rowIndex++);
                        ////////// // System.out.println("Specific Count : " + sessionDayCount);
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
//                        //// // System.out.println("newSs.getDepartment() 2= " + newSs.getDepartment());
//                        //// // System.out.println("newSs.getInstitution() 2= " + newSs.getInstitution());
                        newSs.setSessionAt(nowDate);
                        newSs.setDisplayCount(getBillSessionsCount(ss, nowDate));
                        newSs.setTransDisplayCountWithoutCancelRefund(getBillSessionsCountWithOutCancelRefund(ss, nowDate));
                        newSs.setTransCreditBillCount(getBillSessionsCountCrditBill(ss, nowDate));
//                        //// // System.out.println("getBillSessionsCountWithOutCancelRefund(ss, nowDate) = " + getBillSessionsCountWithOutCancelRefund(ss, nowDate));
//                        //// // System.out.println("getBillSessionsCountCrditBill(ss, nowDate) = " + getBillSessionsCountCrditBill(ss, nowDate));
                        newSs.setStaff(ss.getStaff());
                        newSs.setSessionDate(nowDate);
                        //Temprory
                        newSs.setRoomNo(rowIndex++);
                        // //////// // System.out.println("Count : " + sessionDayCount);
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

        for (ServiceSession cs : createdSessions) {
        }

        return createdSessions;
    }

    public List<ServiceSession> generateDailyServiceSessionsFromWeekdaySessionsNew(List<ServiceSession> inputSessions, Date d) {
        int sessionDayCount = 0;
        List<ServiceSession> createdSessions = new ArrayList<>();

        if (inputSessions == null || inputSessions.isEmpty()) {
            return createdSessions;
        }
        Date nowDate;
        if (d == null) {
            nowDate = Calendar.getInstance().getTime();
        } else {
            nowDate = d;
        }

        Calendar c = Calendar.getInstance();
        c.setTime(nowDate);
        c.add(Calendar.MONTH, 1);
        Date toDate = c.getTime();
        Integer tmp = 0;
        int rowIndex = 0;
        while (toDate.after(nowDate) && sessionDayCount < getFinalVariables().getSessionSessionDayCounterLargest(inputSessions)) {
            boolean hasSpecificDateSession = false;

            for (ServiceSession ss : inputSessions) {
                if (ss.getSessionDate() != null) {
                    Calendar sessionDate = Calendar.getInstance();
                    sessionDate.setTime(ss.getSessionDate());
                    Calendar nDate = Calendar.getInstance();
                    nDate.setTime(nowDate);
                    if (sessionDate.get(Calendar.DATE) == nDate.get(Calendar.DATE)) {
                        hasSpecificDateSession = true;
                        ServiceSession newSs = new ServiceSession();
                        newSs = fetchCreatedServiceSession(ss.getStaff(), nowDate, ss);
                        if (newSs == null) {
                            newSs = createServiceSessionForChannelShedule(ss, nowDate);
                        }
                        //Temprory
                        newSs.setDisplayCount(getBillSessionsCount(ss, nowDate));
                        newSs.setTransDisplayCountWithoutCancelRefund(getBillSessionsCountWithOutCancelRefund(ss, nowDate));
                        newSs.setTransCreditBillCount(getBillSessionsCountCrditBill(ss, nowDate));
                        newSs.setStaff(ss.getStaff());
                        newSs.setTransRowNumber(rowIndex++);
                        //add to list
                        createdSessions.add(newSs);

                        if (!Objects.equals(tmp, ss.getSessionWeekday())) {
                            sessionDayCount++;
                        }
                    }
                }
            }

            if (hasSpecificDateSession == false) {
                for (ServiceSession ss : inputSessions) {
                    Calendar wdc = Calendar.getInstance();
                    wdc.setTime(nowDate);

                    if (ss.getSessionWeekday() != null && (ss.getSessionWeekday() == wdc.get(Calendar.DAY_OF_WEEK))) {
                        ServiceSession newSs = new ServiceSession();
                        newSs = fetchCreatedServiceSession(ss.getStaff(), nowDate, ss);
                        if (newSs == null) {
                            newSs = new ServiceSession();
                            newSs = createServiceSessionForChannelShedule(ss, nowDate);
                        }
                        //Temprory
                        newSs.setDisplayCount(getBillSessionsCount(newSs, nowDate));
                        newSs.setTransDisplayCountWithoutCancelRefund(getBillSessionsCountWithOutCancelRefund(newSs, nowDate));
                        newSs.setTransCreditBillCount(getBillSessionsCountCrditBill(newSs, nowDate));
                        newSs.setTransRowNumber(rowIndex++);
                        //add to list
                        createdSessions.add(newSs);
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
        getBookingController().calculateFeeBooking(createdSessions, channelBillController.getPaymentMethod());

        return createdSessions;
    }

    public List<ServiceSession> generateDailyServiceSessionsFromWeekdaySessionsNewByServiceSessionIdNew(Staff s, Date d) {
        List<ServiceSession> createdSessions = new ArrayList<>();
        Date start = new Date();
        Date nowDate;
        if (d == null) {
            nowDate = Calendar.getInstance().getTime();
        } else {
            nowDate = d;
        }

        Calendar c = Calendar.getInstance();
        c.setTime(nowDate);
        c.add(Calendar.MONTH, 2);
        c.set(Calendar.HOUR, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        Date toDate = c.getTime();
        Integer tmp = 0;
        int rowIndex = 0;
        System.err.println("Time 1 = " + new Date());

        createdSessions = fetchCreatedServiceSessions(s, new Date(), toDate);

        System.err.println("Time 2 = " + new Date());
        getBookingController().calculateFeeBookingNew(createdSessions, channelBillController.getPaymentMethod());
        System.err.println("Time 3 = " + new Date());

        Date end = new Date();
        double time = (start.getTime() - end.getTime()) / 1000;

        return createdSessions;
    }

    public List<ServiceSession> generateDailyServiceSessionsFromWeekdaySessionsNewByServiceSessionId(List<Long> inputSessions, Date d) {
        int sessionDayCount = 0;
        List<ServiceSession> createdSessions = new ArrayList<>();

        if (inputSessions == null || inputSessions.isEmpty()) {
            return createdSessions;
        }
        Date nowDate;
        if (d == null) {
            nowDate = Calendar.getInstance().getTime();
        } else {
            nowDate = d;
        }

        Calendar c = Calendar.getInstance();
        c.setTime(nowDate);
        c.add(Calendar.MONTH, 2);
        Date toDate = c.getTime();
        Integer tmp = 0;
        int rowIndex = 0;
        List<ServiceSession> sessions = new ArrayList<>();
        int finalSessionDayCount = getFinalVariables().getSessionSessionDayCounterLargestById(inputSessions);
        while (toDate.after(nowDate) && sessionDayCount < finalSessionDayCount) {
            if (sessions.isEmpty()) {
                for (Long s : inputSessions) {
                    ServiceSession ss = getServiceSessionFacade().find(s);
                    sessions.add(ss);
                    if (ss.getSessionDate() != null) {
                        Calendar sessionDate = Calendar.getInstance();
                        sessionDate.setTime(ss.getSessionDate());
                        Calendar nDate = Calendar.getInstance();
                        nDate.setTime(nowDate);
                        if (sessionDate.get(Calendar.DATE) == nDate.get(Calendar.DATE)) {
                            ServiceSession newSs = new ServiceSession();
                            newSs = fetchCreatedServiceSession(ss.getStaff(), nowDate, ss);
                            if (newSs == null) {
                                newSs = createServiceSessionForChannelShedule(ss, nowDate);
                            }
                            //Temprory
                            newSs.setDisplayCount(getBillSessionsCount(ss, nowDate));
                            newSs.setTransDisplayCountWithoutCancelRefund(getBillSessionsCountWithOutCancelRefund(ss, nowDate));
                            newSs.setTransCreditBillCount(getBillSessionsCountCrditBill(ss, nowDate));
                            newSs.setStaff(ss.getStaff());
                            newSs.setTransRowNumber(rowIndex++);
                            //add to list

                            createdSessions.add(newSs);
                            bookingController.checkDoctorArival(newSs);
                            ss.setServiceSessionCreateForOriginatingSession(true);
                            if (Objects.equals(tmp, ss.getSessionWeekday())) {
                                sessionDayCount++;
                            }
                        }
                    } else {
                        Calendar wdc = Calendar.getInstance();
                        wdc.setTime(nowDate);
//                        System.err.println("Normal Date");
                        if (ss.getSessionWeekday() != null && (ss.getSessionWeekday() == wdc.get(Calendar.DAY_OF_WEEK))) {
                            ServiceSession newSs = new ServiceSession();
                            newSs = fetchCreatedServiceSession(ss.getStaff(), nowDate, ss);
                            if (newSs == null) {
                                newSs = new ServiceSession();
//                            System.err.println("Cretate New");
                                newSs = createServiceSessionForChannelShedule(ss, nowDate);
                            }
//                        //// // System.out.println("newSs = " + newSs);
                            //Temprory
                            newSs.setDisplayCount(getBillSessionsCount(newSs, nowDate));
                            newSs.setTransDisplayCountWithoutCancelRefund(getBillSessionsCountWithOutCancelRefund(newSs, nowDate));
                            newSs.setTransCreditBillCount(getBillSessionsCountCrditBill(newSs, nowDate));
                            newSs.setTransRowNumber(rowIndex++);
                            //add to list
                            createdSessions.add(newSs);
                            bookingController.checkDoctorArival(newSs);
                            if (!Objects.equals(tmp, ss.getSessionWeekday())) {
                                sessionDayCount++;
                            }
                        }
                    }
                }
            } else {
//                System.err.println("sessions.size() = " + sessions.size());
                for (ServiceSession ss : sessions) {
//                    if (ss.isServiceSessionCreateForOriginatingSession()) {
//                        System.err.println("******");
//                        continue;
//                    }
                    if (ss.getSessionDate() != null) {
                        if (ss.isServiceSessionCreateForOriginatingSession()) {
//                            System.err.println("continue");
                            continue;
                        }
                        Calendar sessionDate = Calendar.getInstance();
                        sessionDate.setTime(ss.getSessionDate());
                        Calendar nDate = Calendar.getInstance();
                        nDate.setTime(nowDate);
                        if (sessionDate.get(Calendar.DATE) == nDate.get(Calendar.DATE)) {
                            ServiceSession newSs = new ServiceSession();
                            newSs = fetchCreatedServiceSession(ss.getStaff(), nowDate, ss);
                            if (newSs == null) {
                                newSs = createServiceSessionForChannelShedule(ss, nowDate);
                            }
                            //Temprory
                            newSs.setDisplayCount(getBillSessionsCount(ss, nowDate));
                            newSs.setTransDisplayCountWithoutCancelRefund(getBillSessionsCountWithOutCancelRefund(ss, nowDate));
                            newSs.setTransCreditBillCount(getBillSessionsCountCrditBill(ss, nowDate));
                            newSs.setStaff(ss.getStaff());
                            newSs.setTransRowNumber(rowIndex++);
                            //add to list
                            createdSessions.add(newSs);
                            bookingController.checkDoctorArival(newSs);
                            ss.setServiceSessionCreateForOriginatingSession(true);
                            if (Objects.equals(tmp, ss.getSessionWeekday())) {
                                sessionDayCount++;
                            }
                        }
                    } else {
                        Calendar wdc = Calendar.getInstance();
                        wdc.setTime(nowDate);
//                        System.err.println("Normal Date");
                        if (ss.getSessionWeekday() != null && (ss.getSessionWeekday() == wdc.get(Calendar.DAY_OF_WEEK))) {
                            ServiceSession newSs = new ServiceSession();
                            newSs = fetchCreatedServiceSession(ss.getStaff(), nowDate, ss);
                            if (newSs == null) {
                                newSs = new ServiceSession();
//                            System.err.println("Cretate New");
                                newSs = createServiceSessionForChannelShedule(ss, nowDate);
                            }
//                        //// // System.out.println("newSs = " + newSs);
                            //Temprory
                            newSs.setDisplayCount(getBillSessionsCount(newSs, nowDate));
                            newSs.setTransDisplayCountWithoutCancelRefund(getBillSessionsCountWithOutCancelRefund(newSs, nowDate));
                            newSs.setTransCreditBillCount(getBillSessionsCountCrditBill(newSs, nowDate));
                            newSs.setTransRowNumber(rowIndex++);
                            //add to list
                            createdSessions.add(newSs);
                            bookingController.checkDoctorArival(newSs);
                            if (!Objects.equals(tmp, ss.getSessionWeekday())) {
                                sessionDayCount++;
                            }
                        }
                    }
                }
            }

            Calendar nc = Calendar.getInstance();
            nc.setTime(nowDate);
            nc.add(Calendar.DATE, 1);
            nowDate = nc.getTime();

        }

        getBookingController().calculateFeeBooking(createdSessions, channelBillController.getPaymentMethod());

        return createdSessions;
    }

    public void createDocLeaveSession(List<ServiceSession> createdSessions, Date nDate, int rIndex) {
        ServiceSession newSs = new ServiceSession();
        newSs.setId(1l);
        newSs.setName("Leave");
        newSs.setSessionAt(nDate);
        newSs.setMaxNo(0);
        newSs.setTransDisplayCountWithoutCancelRefund(0);
        newSs.setTransCreditBillCount(0);
        newSs.setDeactivated(true);
        Calendar e = Calendar.getInstance();
        e.setTime(new Date());
        e.set(Calendar.HOUR, 0);
        e.set(Calendar.MINUTE, 0);
        e.set(Calendar.SECOND, 00);
        newSs.setStartingTime(e.getTime());
        e.add(Calendar.HOUR, 2);
        newSs.setEndingTime(e.getTime());
        newSs.setRoomNo(rIndex);
        createdSessions.add(newSs);

    }

    public ServiceSession createServiceSessionForChannelShedule(ServiceSession ss, Date d) {
        ServiceSession newSs = new ServiceSession();
        newSs.setOriginatingSession(ss);
        newSs.setName(ss.getName());
        newSs.setStartingNo(ss.getStartingNo());
        newSs.setMaxNo(ss.getMaxNo());
        newSs.setStartingTime(ss.getStartingTime());
        newSs.setSessionWeekday(ss.getSessionWeekday());
        newSs.setHospitalFee(ss.getHospitalFee());
        newSs.setProfessionalFee(ss.getProfessionalFee());
        newSs.setDepartment(ss.getDepartment());
        newSs.setInstitution(ss.getInstitution());
        newSs.setSessionAt(d);//what is this feild
        newSs.setSessionDate(d);
        newSs.setSessionTime(ss.getStartingTime());
        newSs.setStartingTime(ss.getStartingTime());
        newSs.setEndingTime(ss.getEndingTime());
        newSs.setCreatedAt(new Date());
//        newSs.setCreater(getSessionController().getLoggedUser());
        newSs.setStaff(ss.getStaff());
        newSs.setRoomNo(ss.getRoomNo());
        newSs.setSessionNumberGenerator(saveSessionNumber(ss));
        getServiceSessionFacade().create(newSs);
        return newSs;
    }

    public ServiceSession fetchCreatedServiceSession(Staff s, Date d, ServiceSession ss) {
        String sql;
        Map m = new HashMap();
        ServiceSession tmp = new ServiceSession();
        sql = "Select s From ServiceSession s where s.retired=false "
                + " and s.staff=:staff "
                + " and s.originatingSession=:os "
                + " and s.sessionDate=:d "
                + " and type(s)=:class "
                + " order by s.sessionWeekday,s.startingTime ";
        m.put("d", d);
        m.put("staff", s);
        m.put("os", ss);
        m.put("class", ServiceSession.class);
        try {
            tmp = getServiceSessionFacade().findFirstBySQL(sql, m, TemporalType.TIMESTAMP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tmp;
    }

    public List<ServiceSession> fetchCreatedServiceSessions(Staff s, Date fd, Date td) {
        //// // System.out.println("fetchCreatedServiceSessions");
        String sql;
        Map m = new HashMap();
        List<ServiceSession> tmp = new ArrayList<>();
        sql = "Select s From ServiceSession s where s.retired=false "
                + " and s.staff=:staff "
                + " and s.originatingSession is not null "
                + " and s.sessionDate between :fd and :td "
                + " and type(s)=:class "
                + " order by s.sessionDate,s.startingTime ";
        m.put("fd", fd);
        m.put("td", td);
        m.put("staff", s);
        m.put("class", ServiceSession.class);
        try {
            tmp = getServiceSessionFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 14);
            //// // System.out.println("m = " + m);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tmp;
    }

    public SessionNumberGenerator saveSessionNumber(ServiceSession ss) {
        SessionNumberGenerator sessionNumberGenerator;
        sessionNumberGenerator = ss.getSessionNumberGenerator();
        if (sessionNumberGenerator == null) {
            sessionNumberGenerator = new SessionNumberGenerator();
            sessionNumberGenerator.setSpeciality(ss.getStaff().getSpeciality());
            sessionNumberGenerator.setStaff(ss.getStaff());
            sessionNumberGenerator.setName(ss.getStaff().getPerson().getName() + " " + ss.getName());
            sessionNumberGeneratorFacade.create(sessionNumberGenerator);
        }
        return sessionNumberGenerator;
    }

    public List<ServiceSession> generateServiceSessionsForSelectedDate(List<ServiceSession> inputSessions, Date date) {
        int sessionDayCount = 0;
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
            ////////// // System.out.println("Specific Count : " + sessionDayCount);
            createdSessions.add(newSs);

        }
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

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public BookingController getBookingController() {
        return bookingController;
    }

    public void setBookingController(BookingController bookingController) {
        this.bookingController = bookingController;
    }
}
