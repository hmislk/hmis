/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.data.SessionNumberType;

import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.Category;
import com.divudi.entity.Item;
import com.divudi.entity.ServiceSession;
import com.divudi.facade.BillSessionFacade;
import com.divudi.java.CommonFunctions;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author pdhs
 */
@Named(value = "serviceSessionFunctions")
@ApplicationScoped
public class ServiceSessionFunctions {

    /**
     * Creates a new instance of ServiceSessionFunctions
     */
    public ServiceSessionFunctions() {
    }

    @EJB
    BillSessionFacade billSessionFacade;

    List<BillSession> billSessions;
    Long countLong;

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public List<BillSession> getBillSessions(Item i, Date d) {
        //   ////// // System.out.println("getting bill sessions");
        if (i == null || i.getSessionNumberType() == null) {
            return null;
        }
        switch (i.getSessionNumberType()) {
            case ByCategory:
                //   ////// // System.out.println("by cat");
                if (i.getCategory().getParentCategory() == null) {
                    //   ////// // System.out.println("by cat 2");
                    billSessions = getBillSessionsByCat(i.getCategory(), d);
                    return billSessions;
                } else {
                    //   ////// // System.out.println("by cat 3");
                    billSessions = getBillSessionsByCat(i.getCategory().getParentCategory(), d);
                    return billSessions;
                }
            case BySubCategory:
                //   ////// // System.out.println("by sc");
                billSessions = getBillSessionsByCat(i.getCategory(), d);
                return billSessions;
            case ByItem:
                //   ////// // System.out.println("by items 3");
                billSessions = getBillSessionsByItem(i, d);
                return billSessions;
            case ByBill:
                billSessions = getBillSessionsByItem(i, d);
                return billSessions;
            default:
                return null;

        }
    }

    public Long calBillSessions(Item i, Date d) {
        System.out.println("calBillSessions");
        System.out.println("d = " + d);
        System.out.println("i = " + i);
        //   ////// // System.out.println("getting bill sessions");
        if (i == null || i.getSessionNumberType() == null) {
            return null;
        }
        switch (i.getSessionNumberType()) {
            case ByCategory:
                //   ////// // System.out.println("by cat");
                if (i.getCategory().getParentCategory() == null) {
                    //   ////// // System.out.println("by cat 2");
                    countLong = calBillSessionsByCat(i.getCategory(), d);
                    return countLong;
                } else {
                    //   ////// // System.out.println("by cat 3");
                    countLong = calBillSessionsByCat(i.getCategory().getParentCategory(), d);
                    return countLong;
                }
            case BySubCategory:
                //   ////// // System.out.println("by sc");
                countLong = calBillSessionsByCat(i.getCategory(), d);
                return countLong;
            case ByItem:
                //   ////// // System.out.println("by items 3");
                countLong = calBillSessionsByItem(i, d);
                return countLong;
            case ByBill:
                countLong = calBillSessionsByBill(i, d);
                return countLong;
            default:
                return null;

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
        System.out.println("Going to saving bill item sessions");
        if (bi == null || bi.getItem() == null || bi.getItem().getSessionNumberType() == null) {
               System.out.println("Bil items sessions not save because of null values");
            return null;
        }
        Item i = bi.getItem();
        BillSession bs = new BillSession();
//        bs.setBill(bi.getBill());
        bs.setBillItem(bi);
        bs.setBill(bi.getBill());
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
        // //////// // System.out.println("bill item session switch - pre");
        Long count = calBillSessions(i, bi.getSessionDate());
        if (count != null) {
            bs.setSerialNo(count.intValue() + 1);
        } else {
            bs.setSerialNo(1);
        }
//        if (bs != null) {
//            getBillSessionFacade().create(bs);
//        }
        return bs;
    }

    public List<BillSession> getBillSessionsByCat(Category c, Date d) {
        if (c == null || c.getId() == null) {
            return null;
        }
        String s;
        s = "select b from BillSession b "
                + " where b.category.id =:catId "
                + "and b.sessionDate =:sd "
                + " order by b.serialNo";
        Map m = new HashMap();
        m.put("catId", c.getId());
        m.put("sd", d);
        billSessions = getBillSessionFacade().findByJpql(s, m, TemporalType.DATE);
        return billSessions;
    }

    public Long calBillSessionsByCat(Category c, Date d) {
        if (c == null || c.getId() == null) {
            return null;
        }
        String s;
        s = "select count(b) from BillSession b "
                + " where b.category.id =:catId "
                + " and b.sessionDate =:sd ";
        Map m = new HashMap();
        m.put("catId", c.getId());
        m.put("sd", d);
        countLong = getBillSessionFacade().findLongByJpql(s, m, TemporalType.DATE);
        return countLong;
    }

    public List<BillSession> getBillSessionsByItem(Item i, Date d) {
        if (i == null || i.getId() == null) {
            return null;
        }
        String s;
        s = "select b from BillSession b "
                + " where b.item=:item"
                + " and b.sessionDate=:sd "
                + " order by b.serialNo";
        Map m = new HashMap();
        m.put("item", i);
        m.put("sd", d);
        billSessions = getBillSessionFacade().findByJpql(s, m, TemporalType.DATE);
        return billSessions;
    }

    public Long calBillSessionsByItem(Item i, Date d) {
        if (i == null || i.getId() == null) {
            return null;
        }
        String s;
        s = "select count(b) from BillSession b "
                + " where b.item=:item"
                + " and b.sessionDate=:sd "
                + " order by b.serialNo";
        Map m = new HashMap();
        m.put("item", i);
        m.put("sd", d);
        countLong = getBillSessionFacade().findLongByJpql(s, m, TemporalType.DATE);
        return countLong;
    }

    public Long calBillSessionsByBill(Item i, Date d) {
        System.out.println("calBillSessionsByBill" );
        System.out.println("d = " + d);
        if (i == null || i.getId() == null) {
            return null;
        }
        String s;
        s = "select count(b.bill) "
                + " from BillSession b "
                + " where b.item.sessionNumberType=:stp"
                + " and b.sessionDate=:sd ";
        Map m = new HashMap();
        m.put("stp", SessionNumberType.ByBill);
        m.put("sd", d);
        System.out.println("m = " + m);
        System.out.println("s = " + s);
        countLong = getBillSessionFacade().findLongByJpql(s, m, TemporalType.DATE);
        System.out.println("countLong = " + countLong);
        return countLong;
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
//            //////// // System.out.println("Error in converting double to int is" + e.getMessage());
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
//        //////// // System.out.println("id by cat count is " + sn );
//        try {
//            //////// // System.out.println("int val of ount is " + sn.intValue());
//            return sn.intValue();
//        } catch (Exception e) {
//            //////// // System.out.println("Error in converting double to int is" + e.getMessage());
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
        List<BillSession> tmp;
        String sql = "Select bs From BillSession bs where bs.serviceSession=:ss and bs.sessionDate= :ssDate";
        HashMap hh = new HashMap();
        hh.put("ssDate", sessionDate);
        hh.put("ss", serviceSession);
        tmp = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);
        return tmp.size() + 1;
    }

    public List<BillSession> getBillSessions() {
        return billSessions;
    }

    public void setBillSessions(List<BillSession> billSessions) {
        this.billSessions = billSessions;
    }

}
