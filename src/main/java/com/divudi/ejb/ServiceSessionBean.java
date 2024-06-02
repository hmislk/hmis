/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.data.BillType;
import static com.divudi.data.SessionNumberType.ByCategory;
import static com.divudi.data.SessionNumberType.ByItem;
import static com.divudi.data.SessionNumberType.BySubCategory;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Item;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.facade.BillSessionFacade;
import com.divudi.java.CommonFunctions;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Singleton
public class ServiceSessionBean {

    @EJB
    BillSessionFacade billSessionFacade;
    BillSession billSession;

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public List<BillSession> getBillSessions(Item i, Date d) {
        //   //////// // System.out.println("getting bill sessions");
        if (i == null || i.getSessionNumberType() == null) {
            return null;
        }
        switch (i.getSessionNumberType()) {
            case ByCategory:
                //   //////// // System.out.println("by cat");
                if (i.getCategory().getParentCategory() == null) {
                    //   //////// // System.out.println("by cat 2");
                    billSessions = getBillSessionsByCat(i.getCategory(), d);
                    return billSessions;
                } else {
                    //   //////// // System.out.println("by cat 3");
                    billSessions = getBillSessionsByCat(i.getCategory().getParentCategory(), d);
                    return billSessions;
                }
            case BySubCategory:
                //   //////// // System.out.println("by sc");
                billSessions = getBillSessionsByCat(i.getCategory(), d);
                return billSessions;
            case ByItem:
                //   //////// // System.out.println("by items 3");
                billSessions = getBillSessionsByItem(i, d);
                return billSessions;
            default:
                return new ArrayList<>();

        }
    }

//    public int getBillSessionNo(Item i, Date d) {
//        switch (i.getSessionNumberType()) {
//            case ByCategory:
//                if (i.getCategory().getParentCategory() == null) {
//                    return getIdByCat(i.getCategory(), d);
//                } else {
//                    return getIdByCat(i.getCategory().getParentCategory(), d);
//                }
//            case BySubCategory:
//                return getIdByCat(i.getCategory(), d);
//            case ByItem:
//                return getIdByItem(i, d);
//            default:
//                return 0;
//        }
//    }
    public BillSession createBillSession(BillItem bi) {
        //   //////// // System.out.println("Going to saving bill item sessions");
        if (bi == null || bi.getItem() == null || bi.getItem().getSessionNumberType() == null) {
            //   //////// // System.out.println("Bil items sessions not save because of null values");
            return null;
        }
        Item i = bi.getItem();
        BillSession bs = new BillSession();
//        bs.setBill(bi.getBill());
        bs.setBillItem(bi);
        bs.setItem(i);
        bs.setCreatedAt(Calendar.getInstance().getTime());
//        bs.setCreater(bi.getCreater());
        Date sessDate;
        if (bi.getSessionDate() == null) {
            sessDate = CommonFunctions.removeTime(new Date());
            //sessDate = new Date();
        } else {
            sessDate = CommonFunctions.removeTime(bi.getSessionDate());
            //  sessDate = bi.getSessionDate();
        }
        bi.setSessionDate(sessDate);
        bs.setSessionDate(sessDate);
//        bs.setSessionDate(CommonFunctions.removeTime(bi.getSessionDate()));
        // ////////// // System.out.println("bill item session switch - pre");

        int count = getBillSessions(i, bi.getSessionDate()).size() + 1;
        bs.setSerialNo(count);
//        if (bs != null) {
//            getBillSessionFacade().create(bs);
//        }
        return bs;
    }

    List<BillSession> billSessions;
    Long countLong;

    public List<BillSession> getBillSessionsByCat(Category c, Date d) {
        if (c == null || c.getId() == null) {
            return null;
        }
        String s;
        s = "select b from BillSession b where b.category.id =:catId and b.sessionDate =:sd order by b.serialNo";
        Map m = new HashMap();
        m.put("catId", c.getId());
        m.put("sd", d);
        billSessions = getBillSessionFacade().findByJpql(s, m, TemporalType.DATE);
        return billSessions;
    }

    public List<BillSession> getBillSessionsByItem(Item i, Date d) {
        if (i == null || i.getId() == null) {
            return null;
        }
        String s;
        s = "select b from BillSession b where b.item=:item"
                + " and b.sessionDate=:sd "
                + " order by b.serialNo";
        Map m = new HashMap();
        m.put("item", i);
        m.put("sd", d);
        billSessions = getBillSessionFacade().findByJpql(s, m, TemporalType.DATE);
        return billSessions;
    }

//    public int getIdByItem(Item i, Date d) {
//        if (i == null || i.getId() == null) {
//            return 0;
//        }
//        String s;
//        s = "select count(b.id) from BillSession b where b.item.id=:itemId and b.sessionDate=:sd";
//        Map m = new HashMap();
//        m.put("itemId", i.getId());
//        m.put("sd", d);
//        Double sn = getBillSessionFacade().findDoubleByJpql(s, m, TemporalType.DATE);
//        try {
//            return sn.intValue();
//        } catch (Exception e) {
//            ////////// // System.out.println("Error in converting double to int is" + e.getMessage());
//            return 0;
//        }
//    }
//
//    public int getIdByCat(Category c, Date d) {
//        if (c == null || c.getId() == null) {
//            return 0;
//        }
//
//        String s;
//        s = "select b from BillSession b where b.category.id =:catId and b.sessionDate =:sd order by b.serialNo";
////        s = "select count(b.id) from BillSession b where b.category.id=:catId and b.sessionDate=:sd";
//        Map m = new HashMap();
//        m.put("catId", c.getId());
//        m.put("sd", d);
//        Double sn = getBillSessionFacade().findDoubleByJpql(s, m, TemporalType.DATE);
//        ////////// // System.out.println("id by cat count is " + sn );
//        try {
//            ////////// // System.out.println("int val of ount is " + sn.intValue());
//            return sn.intValue();
//        } catch (Exception e) {
//            ////////// // System.out.println("Error in converting double to int is" + e.getMessage());
//            return 0;
//        }
//    }
    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public int getSessionNumber(ServiceSession serviceSession, Date sessionDate) {
        ////// // System.out.println("Service count " + serviceSession.getSessionNumberGenerator());

        BillType[] billTypes = {BillType.ChannelAgent,
            BillType.ChannelCash,
            BillType.ChannelOnCall,
            BillType.ChannelStaff,
            BillType.ClinicalOpdBooking,
            BillType.ChannelCredit
        };

        List<BillType> bts = Arrays.asList(billTypes);
        String sql = "Select count(bs) From BillSession bs where "
                + " bs.serviceSession.sessionNumberGenerator=:ss "
                + " and bs.bill.billType in :bt "
                + " and type(bs.bill)=:class"
                + " and bs.sessionDate= :ssDate";
        HashMap hh = new HashMap();

        hh.put(
                "ssDate", sessionDate);
        hh.put(
                "bt", bts);
        hh.put(
                "class", BilledBill.class
        );
        hh.put(
                "ss", serviceSession.getSessionNumberGenerator());
        Long lgValue = getBillSessionFacade().findAggregateLong(sql, hh, TemporalType.DATE);
        ////// // System.out.println("serviceSession = " + serviceSession);
        ////// // System.out.println("serviceSession.getSessionNumberGenerator() = " + serviceSession.getSessionNumberGenerator());
        ////// // System.out.println("sql = " + sql);
        ////// // System.out.println("hh = " + hh);
        ////// // System.out.println("lgValue= " + lgValue);
        if (lgValue
                == null) {
            return 1;
        }

        return lgValue.intValue()
                + 1;
    }

    public int getSessionNumber(ServiceSession serviceSession, Date sessionDate, BillSession billSession) {
        //////// // System.out.println("Service count " + serviceSession.getSessionNumberGenerator());

        BillType[] billTypes = {BillType.ChannelAgent,
            BillType.ChannelCash,
            BillType.ChannelOnCall,
            BillType.ChannelStaff,
            BillType.ChannelCredit};

        List<BillType> bts = Arrays.asList(billTypes);
        String sql = "Select bs From BillSession bs where "
                + " bs.serviceSession.sessionNumberGenerator=:ss "
                + " and bs.bill.billType in :bt "
                + " and type(bs.bill)=:class"
                + " and bs.sessionDate= :ssDate"
                + " and bs.retired=false ";
        HashMap hh = new HashMap();
        hh.put("ssDate", sessionDate);
        hh.put("bt", bts);
        hh.put("class", BilledBill.class
        );
        hh.put("ss", serviceSession.getSessionNumberGenerator());

        List<BillSession> lgValue = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);
        ////// // System.out.println("sql = " + sql);
        ////// // System.out.println("hh = " + hh);
        ////// // System.out.println("lgValue.size() = " + lgValue.size());

        List<Integer> availabeNumbers;
        String temStr = "";
        if (billSession.getBill().getBillType() == BillType.ChannelAgent) {
            temStr = serviceSession.getAgencyNumbers();
        } else if (billSession.getBill().getBillType() == BillType.ChannelCash) {
            temStr = serviceSession.getCashNumbers();
        } else if (billSession.getBill().getBillType() == BillType.ChannelOnCall) {
            temStr = serviceSession.getCreditNumbers();
        }

//        //// // System.out.println("temStr = " + temStr);
        availabeNumbers = stringNumbersToInts(temStr, serviceSession);
//        //// // System.out.println("availableNumbers = " + availabeNumbers.toString());

        boolean numberGiven;

        for (Integer i : availabeNumbers) {
            numberGiven = false;
            for (BillSession bs : lgValue) {
                if (i == bs.getSerialNo()) {
//                    //// // System.out.println("i = " + i);
//                    //// // System.out.println("bs.getSerialNo() = " + bs.getSerialNo());
                    ////// // System.out.println("break");
                    numberGiven = true;
                    break;
                }
            }
//            //// // System.out.println("i = " + i);
//            //// // System.out.println("numberGiven = " + numberGiven);
            if (numberGiven == false) {
                return i;
            }
        }
        return getSessionNumber(serviceSession, sessionDate);
    }

    @Lock(LockType.WRITE)
    public Integer getNextNonReservedSerialNumber(SessionInstance si, List<Integer> reservedNumbers) {
        BillType[] billTypes = {
            BillType.ChannelAgent,
            BillType.ChannelCash,
            BillType.ChannelOnCall,
            BillType.ChannelStaff,
            BillType.ChannelCredit
        };

        List<BillType> bts = Arrays.asList(billTypes);
        String jpql = "SELECT bs.serialNo "
                + "FROM BillSession bs "
                + "WHERE bs.sessionInstance = :si "
                + "AND bs.bill.billType IN :bt "
                + "AND TYPE(bs.bill) = :class";

        HashMap<String, Object> params = new HashMap<>();
        params.put("si", si);
        params.put("bt", bts);
        params.put("class", BilledBill.class);

        // Fetch the booked (already assigned) numbers
        List<Integer> bookedNumbers = (List<Integer>) getBillSessionFacade().findLightsByJpql(jpql, params);

        // Combine reserved and booked numbers into a single set to ensure uniqueness
        Set<Integer> allUnavailableNumbers = new HashSet<>(bookedNumbers);
        allUnavailableNumbers.addAll(reservedNumbers);

        // Find the next available number
        // Assuming serial numbers start from 1 and increment by 1
        int nextAvailableNumber = 1;
        while (allUnavailableNumbers.contains(nextAvailableNumber)) {
            nextAvailableNumber++;
        }

        return nextAvailableNumber;
    }

    @Lock(LockType.WRITE)
    public Integer getNextAvailableReservedNumber(SessionInstance si, List<Integer> reservedNumbers, Integer selectedReservedBookingNumber) {
        BillType[] billTypes = {
            BillType.ChannelAgent,
            BillType.ChannelCash,
            BillType.ChannelOnCall,
            BillType.ChannelStaff,
            BillType.ChannelCredit
        };

        List<BillType> bts = Arrays.asList(billTypes);
        String jpql = "SELECT bs.serialNo "
                + "FROM BillSession bs "
                + "WHERE bs.sessionInstance = :si "
                + "AND bs.bill.billType IN :bt "
                + "AND TYPE(bs.bill) = :class";

        HashMap<String, Object> params = new HashMap<>();
        params.put("si", si);
        params.put("bt", bts);
        params.put("class", BilledBill.class);

        // Fetch the booked (already assigned) numbers
        List<Integer> bookedNumbers = (List<Integer>) getBillSessionFacade().findLightsByJpql(jpql, params);

        // Convert reservedNumbers to a Set for efficient search
        Set<Integer> reservedNumbersSet = new HashSet<>(reservedNumbers);
        // Remove all booked numbers from the reservedNumbers set
        reservedNumbersSet.removeAll(bookedNumbers);

        // First, check if selectedReservedBookingNumber is available and not booked
        if (selectedReservedBookingNumber != null && reservedNumbersSet.contains(selectedReservedBookingNumber)) {
            return selectedReservedBookingNumber;
        }

        if (reservedNumbersSet.isEmpty()) {
            // If no reserved numbers are available (not already booked), return null
            return null;
        } else {
            // Return the smallest number in the remaining reservedNumbers set
            return Collections.min(reservedNumbersSet);
        }
    }

    private void addToIntList(Integer fromInt, Integer toInt, List<Integer> lst) {
        for (int i = fromInt; i <= toInt; i++) {
            lst.add(i);
        }
    }

    public List<Integer> stringNumbersToInts(String str, ServiceSession ss) {
        int maxNo = 100;
        List<Integer> nits = new ArrayList();
        if (str == null || str.trim().equals("")) {
            if (ss.getStartingNo() > 0) {
                addToIntList(ss.getStartingNo(), maxNo, nits);
            } else {
                addToIntList(1, maxNo, nits);
            }

            return nits;
        }
        if (str.contains(">")) {
            ////// // System.out.println("contains > ");
            str = str.replace(">", "");
            String strs[] = str.split(" ");
            for (String s : strs) {
                ////// // System.out.println("s = " + s);
                if (s.trim().equals("")) {
                    continue;
                }
                if (isNumeric(s)) {
                    ////// // System.out.println("is numeric");
                    Integer i;
                    try {
                        i = Integer.valueOf(s);
                        ////// // System.out.println("i = " + i);
                    } catch (Exception e) {
                        i = 1;
                        ////// // System.out.println("e = " + e);
                    }
                    addToIntList(i + 1, maxNo, nits);
                    return nits;
                }
            }
        }
        if (str.contains("-")) {
            ////// // System.out.println("contains - ");
            str = str.replace("-", " ");
            String strs[] = str.split(" ");
            Integer fromNo = null;
            Integer toNo = null;
            for (String s : strs) {
                ////// // System.out.println("s = " + s);
                if (s.trim().equals("")) {
                    continue;
                }
                if (isNumeric(s)) {
                    ////// // System.out.println("is numeric");
                    Integer i;

                    try {
                        i = Integer.valueOf(s);
                        ////// // System.out.println("i = " + i);
                    } catch (Exception e) {
                        ////// // System.out.println("e = " + e);
                        i = 1;
                    }

                    if (fromNo == null) {
                        fromNo = i;
                    } else {
                        toNo = i;
                    }

                }
            }
            ////// // System.out.println("fromNo = " + fromNo);
            ////// // System.out.println("toNo = " + toNo);
            addToIntList(fromNo, toNo, nits);
            return nits;
        }
        return nits;
    }

    public static boolean isNumeric(String str) {
        NumberFormat formatter = NumberFormat.getInstance();
        ParsePosition pos = new ParsePosition(0);
        formatter.parse(str, pos);
        return str.length() == pos.getIndex();
    }

}
