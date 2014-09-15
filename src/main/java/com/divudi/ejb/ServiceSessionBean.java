/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import com.divudi.facade.BillSessionFacade;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
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
        //   System.out.println("getting bill sessions");
        if (i == null || i.getSessionNumberType() == null) {
            return null;
        }
        switch (i.getSessionNumberType()) {
            case ByCategory:
                //   System.out.println("by cat");
                if (i.getCategory().getParentCategory() == null) {
                    //   System.out.println("by cat 2");
                    billSessions = getBillSessionsByCat(i.getCategory(), d);
                    return billSessions;
                } else {
                    //   System.out.println("by cat 3");
                    billSessions = getBillSessionsByCat(i.getCategory().getParentCategory(), d);
                    return billSessions;
                }
            case BySubCategory:
                //   System.out.println("by sc");
                billSessions = getBillSessionsByCat(i.getCategory(), d);
                return billSessions;
            case ByItem:
                //   System.out.println("by items 3");
                billSessions = getBillSessionsByItem(i, d);
                return billSessions;
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
        //   System.out.println("Going to saving bill item sessions");
        if (bi == null || bi.getItem() == null || bi.getItem().getSessionNumberType() == null) {
            //   System.out.println("Bil items sessions not save because of null values");
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
        System.err.println("Date " + sessDate);
        bi.setSessionDate(sessDate);
        bs.setSessionDate(sessDate);
//        bs.setSessionDate(CommonFunctions.removeTime(bi.getSessionDate()));
        // //System.out.println("bill item session switch - pre");
        int count = getBillSessions(i, bi.getSessionDate()).size() + 1;
        System.err.println("COUNT " + count);
        bs.setSerialNo(count);
        switch (i.getSessionNumberType()) {
            case ByCategory:
                //   System.out.println("by cat");
                if (i.getCategory().getParentCategory() == null) {
                    //   System.out.println("by cat only ");
                    bs.setCategory(i.getCategory());
//                    bs.setSerialNo(getIdByCat(i.getCategory(), bi.getSessionDate()) + 1);
                } else {
                    //   System.out.println("by parent cat");
                    bs.setCategory(i.getCategory().getParentCategory());
//                    bs.setSerialNo(getIdByCat(i.getCategory().getParentCategory(), bi.getSessionDate()) + 1);
                }
                break;
            case BySubCategory:
                System.err.println("By Sub Category");
                bs.setCategory(i.getCategory());
//                bs.setSerialNo(getIdByCat(i.getCategory(), bi.getSessionDate()) + 1);
                break;
            case ByItem:
                System.err.println("By Item");
//                bs.setSerialNo(getIdByItem(i, bi.getSessionDate()) + 1);
                break;
            default:
                bs = null;
        }
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
        billSessions = getBillSessionFacade().findBySQL(s, m, TemporalType.DATE);
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
        billSessions = getBillSessionFacade().findBySQL(s, m, TemporalType.DATE);
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
//            //System.out.println("Error in converting double to int is" + e.getMessage());
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
//        //System.out.println("id by cat count is " + sn );
//        try {
//            //System.out.println("int val of ount is " + sn.intValue());
//            return sn.intValue();
//        } catch (Exception e) {
//            //System.out.println("Error in converting double to int is" + e.getMessage());
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
        System.out.println("Service count " + serviceSession.getSessionNumberGenerator());
        
        BillType[] billTypes = {BillType.ChannelAgent,
            BillType.ChannelCash,
            BillType.ChannelOnCall,
            BillType.ChannelStaff};

        List<BillType> bts = Arrays.asList(billTypes);
        String sql = "Select count(bs) From BillSession bs where "
                + " bs.serviceSession.sessionNumberGenerator=:ss "
                + " and bs.bill.billType in :bt "
                + " and type(bs.bill)=:class"
                + " and bs.sessionDate= :ssDate";
        HashMap hh = new HashMap();
        hh.put("ssDate", sessionDate);
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        hh.put("ss", serviceSession.getSessionNumberGenerator());
        Long lgValue = getBillSessionFacade().findAggregateLong(sql, hh, TemporalType.DATE);
        if (lgValue == null) {
            return 1;
        }

        return lgValue.intValue() + 1;
    }

}
