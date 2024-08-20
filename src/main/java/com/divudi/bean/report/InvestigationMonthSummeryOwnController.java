/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.report;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.InvestigationSummeryData;
import com.divudi.data.dataStructure.ItemInstitutionCollectingCentreCountRow;

import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.RefundBill;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.Machine;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.MachineFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Named
@RequestScoped
public class InvestigationMonthSummeryOwnController implements Serializable {

    @Inject
    private SessionController sessionController;
    @Inject
    CommonReport commonReport;
    @Inject
    CommonController commonController;
    @EJB
    private CommonFunctions commonFunctions;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    private InvestigationFacade investigationFacade;
    @EJB
    private BillItemFacade billItemFacade;
    private Date fromDate;
    private Date toDate;
    private Institution creditCompany;
    Institution institution;
    Institution collectingCentre;
    Item item;
    private List<InvestigationSummeryData> items;
    List<InvestigationSummeryData> investigationSummeryDatas;
    private List<InvestigationSummeryData> itemDetails;
    private List<Item> investigations;
    List<InvestigationSummeryData> itemsLab;
    List<Bill> bills;

    /**
     * Creates a new instance of CashierReportController
     */
    public InvestigationMonthSummeryOwnController() {
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public String ixCountByInstitutionAndCollectingCentre() {
        return "/reportLab/ix_count_by_institution_and_collecting_centre";
    }

    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public List<InvestigationSummeryData> getItemsLab() {
        return itemsLab;
    }

    public void setItemsLab(List<InvestigationSummeryData> itemsLab) {
        this.itemsLab = itemsLab;
    }

    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(Calendar.getInstance().getTime());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    private Institution collectingIns;

    public List<InvestigationSummeryData> getItems() {
        if (items == null) {
            createInvestigationMonthEndSummeryCounts();
        }
        return items;
    }

    public void createInvestigationMonthEndSummeryCounts() {
        items = new ArrayList<>();
        for (Item w : getInvestigations()) {
            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            setCountTotal(temp, w);
            if (temp.getCount() != 0) {
                items.add(temp);
            }
        }
    }

    public List<InvestigationSummeryData> getItems2() {
        items = new ArrayList<>();

        for (Item w : getInvestigations2()) {
            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            setCountTotal2(temp, w);
            if (temp.getCount() != 0) {
                items.add(temp);
            }
        }

        return items;
    }

    private long countTotal;
    double itemValue;

    public double getItemValue() {
        return itemValue;
    }

    public void setItemValue(double itemValue) {
        this.itemValue = itemValue;
    }

    List<ItemInstitutionCollectingCentreCountRow> insInvestigationCountRows;

    public void createIxCountByInstitutionAndCollectingCentre() {
        String jpql;
        Map m;
        m = new HashMap();

        jpql = "Select new com.divudi.data.dataStructure.ItemInstitutionCollectingCentreCountRow(bi.item, count(bi), bi.bill.institution, bi.bill.collectingCentre) "
                + " from BillItem bi "
                + " join bi.bill b "
                + " join b.institution ins "
                + " join b.collectingCentre cs "
                + " join bi.item item "
                + " where b.createdAt between :fd and :td "
                + " and type(item) =:ixbt "
                + " and bi.retired=false "
                + " and b.retired=false "
                + " and b.cancelled=false ";

        if (institution != null) {
            jpql = jpql + " and ins=:ins ";
            m.put("ins", institution);
        }

        if (collectingCentre != null) {
            jpql = jpql + " and cs=:cs ";
            m.put("cs", collectingCentre);
        }

        if (item != null) {
            jpql = jpql + " and item=:item ";
            m.put("item", item);
        }

        jpql = jpql + " group by item, ins, cs ";
        jpql = jpql + " order by ins.name, cs.name, item.name ";
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("ixbt", Investigation.class);
        insInvestigationCountRows = (List<ItemInstitutionCollectingCentreCountRow>) (Object) billFacade.findAggregates(jpql, m, TemporalType.DATE);

        if (collectingCentre != null) {
            return;
        }

        m = new HashMap();
        jpql = "Select new com.divudi.data.dataStructure.ItemInstitutionCollectingCentreCountRow(bi.item, count(bi), bi.bill.institution) "
                + " from BillItem bi "
                + " join bi.bill b "
                + " join b.institution ins "
                + " join bi.item item "
                + " where b.createdAt between :fd and :td "
                + " and b.collectingCentre is null "
                + " and type(item) =:ixbt "
                + " and bi.retired=false "
                + " and b.retired=false "
                + " and b.cancelled=false ";
        if (institution != null) {
            jpql = jpql + " and ins=:ins ";
            m.put("ins", institution);
        }

        if (item != null) {
            jpql = jpql + " and item=:item ";
            m.put("item", item);
        }

        jpql = jpql + " group by item, ins ";
        jpql = jpql + " order by ins.name, item.name ";
        m.put("fd", fromDate);
        m.put("td", toDate);

        m.put("ixbt", Investigation.class);

        insInvestigationCountRows.addAll((List<ItemInstitutionCollectingCentreCountRow>) (Object) billFacade.findAggregates(jpql, m, TemporalType.DATE));

        int c = 1;
        for (ItemInstitutionCollectingCentreCountRow r : insInvestigationCountRows) {
            r.setId(c);
            c++;
        }

    }

//    public void createIxCountByInstitutionAndCollectingCentreIndividual() {
//        String jpql;
//        Map m = new HashMap();
//        jpql = "Select item, count(bi), ins, cs "
//                + " from BillItem bi "
//                + " join bi.bill b "
//                + " join b.institution ins "
//                + " join b.collectingCentre cs "
//                + " join bi.item item "
//                + " where b.createdAt between :fd and :td "
//                + " and type(item) =:ixbt "
//                + " and bi.retired=false "
//                + " and b.retired=false "
//                + " and b.cancelled=false ";
//
//        if (institution != null) {
//            jpql = jpql + " and ins=:ins ";
//            m.put("ins", institution);
//        }
//        jpql = jpql + " group by item, ins, cs ";
//
//        jpql = jpql + " order by ins.name, cs.name, item.name ";
//
////        New Way
//        jpql = "Select item, count(bi), ins"
//                + " from BillItem bi "
//                + " join bi.bill b "
//                + " join b.institution ins "
//                + " join bi.item item "
//                + " where b.createdAt between :fd and :td "
//                + " and type(item) =:ixbt "
//                + " and bi.retired=false "
//                + " and b.retired=false "
//                + " and b.cancelled=false ";
//        if (institution != null) {
//            jpql = jpql + " and ins=:ins ";
//            m.put("ins", institution);
//        }
//        jpql = jpql + " group by item, ins ";
//        jpql = jpql + " order by ins.name, item.name ";
//
//        m.put("fd", fromDate);
//        m.put("td", toDate);
//        m.put("ixbt", Investigation.class);
//
//        List<Object[]> bojsl = billFacade.findAggregates(jpql, m, TemporalType.DATE);
//        ////// // System.out.println("bojsl = " + bojsl);
//        insInvestigationCountRows = new ArrayList<>();
//
//        Map<Institution, ItemInstitutionCollectingCentreCountRow> map = new HashMap<>();
//
//        for (Object[] bobj : bojsl) {
//            if (bobj.length < 3) {
//                continue;
//            }
//            
//            ItemInstitutionCollectingCentreCountRow r = new ItemInstitutionCollectingCentreCountRow();
//            r.setItem((Item) bobj[0]);
//            r.setCount((Long) bobj[1]);
//            r.setInstitution((Institution) bobj[2]);
////            if(bobj[3]!=null){
////                r.setCollectingCentre((Institution) bobj[3]);
////            }
//            insInvestigationCountRows.add(r);
//        }
//        ////// // System.out.println("sql = " + jpql);
//        ////// // System.out.println("m = " + m);
//        ////// // System.out.println("insInvestigationCountRows.size() = " + insInvestigationCountRows.size());
//    }
    public List<ItemInstitutionCollectingCentreCountRow> getInsInvestigationCountRows() {
        return insInvestigationCountRows;
    }

    public void createItemList3() {
        itemsLab = new ArrayList<>();
        countTotal = 0;
        itemValue = 0;
        for (Item w : getInvestigations3()) {
            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            long temCoint = calculateInvestigationBilledCount(w);
            temp.setCount(temCoint);
            countTotal += temCoint;
            double tempTotal = calculateInvestigationBilledValue(w);
            temp.setTotal(tempTotal);
            itemValue += tempTotal;
            if (temp.getCount() != 0 || temp.getTotal() != 0) {
                itemsLab.add(temp);
            }

        }
//        countTotal = 0;
//
//        long billed = getCount2(new BilledBill());
//        ////// // System.out.println("billed = " + billed);
//        long cancelled = getCount2(new CancelledBill());
//        ////// // System.out.println("cancelled = " + cancelled);
//        long refunded = getCount2(new RefundBill());
//        ////// // System.out.println("refunded = " + refunded);
//
//        countTotal = billed - (refunded + cancelled);
    }

    public void createItemNewChanges() {
        Date startTime = new Date();

        itemsLab = new ArrayList<>();
        countTotal = 0;
        itemValue = 0;
        BillType[] bts = {BillType.LabBill, BillType.CollectingCentreBill};
        for (Item w : getInvestigations3(bts)) {
            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            long temCoint = calculateInvestigationBilledCount(w, bts);
            temp.setCount(temCoint);
            countTotal += temCoint;
            double tempTotal = calculateInvestigationBilledValue(w, bts);
            temp.setTotal(tempTotal);
            itemValue += tempTotal;
            if (temp.getCount() != 0 || temp.getTotal() != 0) {
                itemsLab.add(temp);
            }

        }
//        countTotal = 0;
//
//        long billed = getCount2(new BilledBill());
//        ////// // System.out.println("billed = " + billed);
//        long cancelled = getCount2(new CancelledBill());
//        ////// // System.out.println("cancelled = " + cancelled);
//        long refunded = getCount2(new RefundBill());
//        ////// // System.out.println("refunded = " + refunded);
//
//        countTotal = billed - (refunded + cancelled);

        
    }

    public List<InvestigationSummeryData> getItems3() {

        return items;
    }

    public List<InvestigationSummeryData> getItemsWithoutC() {
        items = new ArrayList<>();

        for (Item w : getInvestigationsWithoutC()) {
            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            setCountTotalWithoutC(temp, w);
            items.add(temp);
        }

        return items;
    }

    private void setCountTotalWithoutC(InvestigationSummeryData is, Item w) {

        String sql;
        Map temMap = new HashMap();
        sql = "select bi FROM BillItem bi where  bi.bill.institution=:ins and type(bi.item) =:ixtype "
                + " and (bi.bill.paymentMethod = :pm1 or bi.bill.paymentMethod = :pm2 or"
                + " bi.bill.paymentMethod = :pm3 )    and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("ins", getSessionController().getInstitution());

        temMap.put("ixtype", com.divudi.entity.lab.Investigation.class);
        List<BillItem> temps = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        double tot = 0.0;
        int c = 0;

        for (BillItem b : temps) {
            if (b.getBill() != null && b.getBill().isCancelled() == false) {
                if (b.isRefunded() == null || b.isRefunded() == false) {
                    if (b.getItem().getId() == w.getId()) {
                        tot += b.getNetValue();
                        c++;
                    }
                }
            }
        }

        is.setCount(c);
        is.setTotal(tot);
    }

    private long getCount(Bill bill, Item item) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi where (bi.bill.billType=:bType1 or bi.bill.billType=:bType2) and bi.item =:itm"
                + " and type(bi.bill)=:billClass and (bi.bill.toInstitution=:ins or bi.item.department.institution=:ins ) "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("billClass", bill.getClass());
        temMap.put("bType1", BillType.OpdBill);
        temMap.put("bType2", BillType.InwardBill);
        temMap.put("ins", getSessionController().getInstitution());
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private long getCount(Bill bill, Machine m, List<BillType> bts) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi where bi.bill.billType in :bts and bi.item.machine =:mac"
                + " and type(bi.bill)=:billClass and (bi.bill.toInstitution=:ins or bi.item.department.institution=:ins ) "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.machine.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("mac", m);
        temMap.put("billClass", bill.getClass());
        temMap.put("bts", bts);
        temMap.put("ins", institution);
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<Object[]> getCount(List<BillType> bts) {
        String sql;
        Map m = new HashMap();
        sql = "select bi.item.machine, bi.bill.billType, bi.bill.billClassType, count(bi), sum(bi.netValue+bi.vat) "
                + " FROM BillItem bi where"
                + " bi.bill.billType in :bts "
                //                + " and bi.item.machine =:mac"
                + " and (bi.bill.toInstitution=:ins or bi.item.department.institution=:ins ) "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " group by bi.item.machine, bi.bill.billType, bi.bill.billClassType "
                + " order by bi.item.machine.name";
        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
//        m.put("mac", ma);
        m.put("bts", bts);
        m.put("ins", institution);
        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    private List<Object[]> getCountWithInvestigation(List<BillType> bts) {
        String sql;
        Map m = new HashMap();
        sql = "select bi.item.machine, bi.item, bi.bill.billType, bi.bill.billClassType, count(bi), sum(bi.netValue+bi.vat) "
                + " FROM BillItem bi where"
                + " bi.bill.billType in :bts "
                //                + " and bi.item.machine =:mac"
                + " and (bi.bill.toInstitution=:ins or bi.item.department.institution=:ins ) "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " group by bi.item.machine, bi.item, bi.bill.billType, bi.bill.billClassType "
                + " order by bi.item.machine.name, bi.item.name";
        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
//        m.put("mac", ma);
        m.put("bts", bts);
        m.put("ins", institution);
        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    private long getCount(Bill bill, Machine m, BillType bt) {
        List<BillType> bts = new ArrayList<>();
        bts.add(bt);
        return getCount(bill, m, bts);
    }

    private long getCount(Bill bill, Machine m) {
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.OpdBill);
        bts.add(BillType.LabBill);
        bts.add(BillType.InwardBill);
        return getCount(bill, m, bts);
    }

    private long getItemCount(Bill bill, Item i, List<BillType> bts) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi where bi.bill.billType in :bts and bi.item =:item"
                + " and type(bi.bill)=:billClass and (bi.bill.toInstitution=:ins or bi.item.department.institution=:ins ) "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.machine.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("item", i);
        temMap.put("billClass", bill.getClass());
        temMap.put("bts", bts);
        temMap.put("ins", institution);
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private long getItemCount(Bill bill, Item i, BillType bt) {
        List<BillType> bts = new ArrayList<>();
        bts.add(bt);
        return getItemCount(bill, i, bts);
    }

    private long getItemCount(Bill bill, Item i) {
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.OpdBill);
        bts.add(BillType.LabBill);
        bts.add(BillType.InwardBill);
        bts.add(BillType.CollectingCentreBill);
        return getItemCount(bill, i, bts);
    }

    private long getCount2(Bill bill, Item item) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi where bi.bill.billType=:bType and bi.item =:itm"
                + " and type(bi.bill)=:billClass and bi.bill.collectingCentre=:col "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("billClass", bill.getClass());
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCollectingIns());
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private long getCount3(Bill bill, Item item) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi where bi.bill.billType=:bType and bi.item =:itm"
                + " and type(bi.bill)=:billClass and bi.bill.collectingCentre=:col "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("billClass", bill.getClass());
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCreditCompany());
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private long getCount3(Bill bill, Item item, BillType[] bts) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi where bi.bill.billType in :bTypes and bi.item =:itm"
                + " and type(bi.bill)=:billClass "
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        if (getCreditCompany() != null) {
            sql += " and (bi.bill.collectingCentre=:col or bi.bill.fromInstitution=:col) ";
            temMap.put("col", getCreditCompany());
        } else {
            sql += " and (bi.bill.collectingCentre is not null or bi.bill.fromInstitution is not null) ";
        }
        sql += " order by bi.item.name";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("billClass", bill.getClass());
        temMap.put("bTypes", Arrays.asList(bts));
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getTotalValue(Bill bill, Item item) {
        String sql;
        Map temMap = new HashMap();
        sql = "select sum(bi.netValue) FROM BillItem bi where bi.bill.billType=:bType and bi.item =:itm"
                + " and type(bi.bill)=:billClass and bi.bill.collectingCentre=:col "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("billClass", bill.getClass());
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCreditCompany());
        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getTotalValue(Bill bill, Item item, BillType[] bts) {
        String sql;
        Map temMap = new HashMap();
        sql = "select sum(bi.netValue) FROM BillItem bi where bi.bill.billType in :bTypes and bi.item =:itm"
                + " and type(bi.bill)=:billClass "
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        if (getCreditCompany() != null) {
            sql += " and (bi.bill.collectingCentre=:col or bi.bill.fromInstitution=:col) ";
            temMap.put("col", getCreditCompany());
        } else {
            sql += " and (bi.bill.collectingCentre is not null or bi.bill.fromInstitution is not null) ";
        }
        sql += " order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("billClass", bill.getClass());
        temMap.put("bTypes", Arrays.asList(bts));
        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private long getCount(Bill bill) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi where bi.bill.billType=:bType and type(bi.item)=:ixtype "
                + " and type(bi.bill)=:billClass and bi.bill.toInstitution=:ins "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("billClass", bill.getClass());
        temMap.put("bType", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private long getCount2(Bill bill) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi where bi.bill.billType=:bType and type(bi.item)=:ixtype "
                + " and type(bi.bill)=:billClass and bi.bill.collectingCentre=:col "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("billClass", bill.getClass());
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCollectingIns());
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getTotal(Item item) {
        String sql;
        Map temMap = new HashMap();
        sql = "select sum(bi.netValue) FROM BillItem bi where bi.bill.billType=:bType and bi.item =:itm"
                + " and bi.bill.toInstitution=:ins "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("bType", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());
        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getTotal(Machine m) {
        String sql;
        Map temMap = new HashMap();
        sql = "select sum(bi.netValue) FROM BillItem bi where bi.bill.billType=:bType and bi.item.machine =:mac"
                + " and bi.bill.toInstitution=:ins "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.machine.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("mac", m);
        temMap.put("bType", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());
        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getTotal2(Item item) {
        String sql;
        Map temMap = new HashMap();
        sql = "select sum(bi.netValue) FROM BillItem bi where bi.bill.billType=:bType and bi.item =:itm"
                + " and bi.bill.collectingCentre=:col "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCollectingIns());
        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getTotal3(Item item) {
        String sql;
        Map temMap = new HashMap();
        sql = "select sum(bi.netValue) FROM BillItem bi where bi.bill.billType=:bType and bi.item =:itm"
                + " and bi.bill.creditCompany=:col "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCreditCompany());
        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private void setCountTotal(InvestigationSummeryData is, Item w) {

        long billed = getCount(new BilledBill(), w);
        long cancelled = getCount(new CancelledBill(), w);
        long refunded = getCount(new RefundBill(), w);

        long net = billed - (cancelled + refunded);
        is.setCount(net);

        is.setTotal(getTotal(w));
    }

    private void setCountTotal2(InvestigationSummeryData is, Item w) {

        long billed = getCount2(new BilledBill(), w);
        long cancelled = getCount2(new CancelledBill(), w);
        long refunded = getCount2(new RefundBill(), w);

        long net = billed - (cancelled + refunded);
        is.setCount(net);

        is.setTotal(getTotal2(w));
    }

    private void setCountTotal3(InvestigationSummeryData is, Item w) {

        long billed = getCount3(new BilledBill(), w);
        long cancelled = getCount3(new CancelledBill(), w);
        long refunded = getCount3(new RefundBill(), w);

        long net = billed - (cancelled + refunded);
        is.setCount(net);

        is.setTotal(getTotal3(w));
    }

    private long calculateInvestigationBilledCount(Item w) {
        long billed = getCount3(new BilledBill(), w);
        long cancelled = getCount3(new CancelledBill(), w);
        long refunded = getCount3(new RefundBill(), w);
        return billed - (cancelled + refunded);
    }

    private long calculateInvestigationBilledCount(Item w, BillType[] bts) {
        long billed = getCount3(new BilledBill(), w, bts);
        long cancelled = getCount3(new CancelledBill(), w, bts);
        long refunded = getCount3(new RefundBill(), w, bts);
        return billed - (cancelled + refunded);
    }

    private double calculateInvestigationBilledValue(Item w) {
        double billed = getTotalValue(new BilledBill(), w);
        double cancelled = getTotalValue(new CancelledBill(), w);
        double refunded = getTotalValue(new RefundBill(), w);
        return billed - (cancelled + refunded);
    }

    private double calculateInvestigationBilledValue(Item w, BillType[] bts) {
        double billed = getTotalValue(new BilledBill(), w, bts);
        double cancelled = getTotalValue(new CancelledBill(), w, bts);
        double refunded = getTotalValue(new RefundBill(), w, bts);
        return billed + (cancelled + refunded);
    }

    public void setItems(List<InvestigationSummeryData> items) {
        this.items = items;
    }
    @EJB
    private ItemFacade itemFacade;

    public List<Item> getInvestigationsWithoutC() {
        String sql;
        Map temMap = new HashMap();
        sql = "select distinct ix from BillItem bi join bi.item ix where type(ix) =:ixtype  "
                + "and (bi.bill.paymentMethod = :pm1 or bi.bill.paymentMethod = :pm2 or bi.bill.paymentMethod = :pm3 ) "
                + "and bi.bill.billType=:bTp and bi.bill.institution=:ins and bi.bill.createdAt between :fromDate and :toDate order by ix.name";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("ixtype", com.divudi.entity.lab.Investigation.class);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());
        investigations = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return investigations;
    }

    public List<Item> getInvestigations() {
        String sql;
        Map temMap = new HashMap();
        sql = "select distinct ix from BillItem bi join bi.item ix where type(ix) =:ixtype  "
                + " and bi.bill.toInstitution=:ins "
                + " and bi.bill.createdAt between :fromDate and :toDate order by ix.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("ins", getSessionController().getInstitution());
        investigations = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        return investigations;
    }

    public List<Item> getInvestigations2() {
        Map temMap = new HashMap();
        String sql = "select distinct ix from BillItem bi join bi.item ix where type(ix) =:ixtype  "
                + "and bi.bill.billType=:bType and bi.bill.collectingCentre=:col "
                + "and bi.bill.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCollectingIns());
        investigations = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return investigations;
    }

    public List<Item> getInvestigations3() {
        Map temMap = new HashMap();
        String sql = "select distinct ix from BillItem bi join bi.item ix "
                + " where type(ix) =:ixtype  "
                + "and bi.bill.billType=:bType and bi.bill.collectingCentre=:col "
                + "and bi.bill.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCreditCompany());
        investigations = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return investigations;
    }

    public List<Item> getInvestigations3(BillType[] bts) {
        Map temMap = new HashMap();
        String sql = "select distinct ix from BillItem bi join bi.item ix "
                + " where type(ix) =:ixtype  "
                + " and bi.bill.billType in :bTypes "
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        if (getCreditCompany() != null) {
            sql += " and (bi.bill.collectingCentre=:col or bi.bill.fromInstitution=:col) ";
            temMap.put("col", getCreditCompany());
        } else {
            sql += " and (bi.bill.collectingCentre is not null or bi.bill.fromInstitution is not null) ";
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("bTypes", Arrays.asList(bts));

        investigations = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return investigations;
    }

    public void setInvestigations(List<Item> investigations) {
        this.investigations = investigations;
    }

    public InvestigationFacade getInvestigationFacade() {
        return investigationFacade;
    }

    public void setInvestigationFacade(InvestigationFacade investigationFacade) {
        this.investigationFacade = investigationFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public List<InvestigationSummeryData> getItemDetailsWithoutCredit() {

        itemDetails = new ArrayList<InvestigationSummeryData>();

        for (Item w : getInvestigationsWithoutC()) {

            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            setBillItemsWithoutC(temp, w);
            itemDetails.add(temp);
        }

        return itemDetails;
    }

    public List<InvestigationSummeryData> getItemDetails() {

        itemDetails = new ArrayList<>();

        for (Item w : getInvestigations()) {

            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            setBillItems(temp, w);
            itemDetails.add(temp);
        }

        return itemDetails;
    }
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;

    private void setBillItems(InvestigationSummeryData t, Item w) {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BillItem b where b.bill.toInstitution=:ins"
                + " and b.item=:ii and  b.createdAt between :fromDate and :toDate";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("ii", w);

        List<BillItem> temps = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        t.setBillItems(temps);

    }

    private void setBillItemsWithoutC(InvestigationSummeryData t, Item w) {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BillItem b where b.bill.billType=:bTp and b.bill.institution=:ins "
                + " and b.item=:itm and (b.bill.paymentMethod = :pm1 or b.bill.paymentMethod = :pm2 or b.bill.paymentMethod = :pm3 ) and  b.createdAt between :fromDate and :toDate";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("itm", w);
        List<BillItem> temps = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        t.setBillItems(temps);

    }

    List<InvestigationCountWithMachine> investigationCountWithMachines;

    public void createLabServiceWithCount() {

        investigationCountWithMachines = new ArrayList<>();

        for (Item w : getInvestigationItems()) {
            InvestigationCountWithMachine temp = new InvestigationCountWithMachine();
            temp.setInvestigation((Investigation) w);

            long billed = getCount(new BilledBill(), w);
            long cancelled = getCount(new CancelledBill(), w);
            long refunded = getCount(new RefundBill(), w);

            long net = billed - (cancelled + refunded);

            temp.setCount(net);
            temp.setTotal(getTotal(w));

            if (temp.getCount() != 0) {
                ////// // System.out.println(investigationCountWithMachines.size() + " " + temp.getInvestigation().getName());
                investigationCountWithMachines.add(temp);
            }
        }

    }

    double totalCount;
    double total;

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getTotalOpd() {
        return totalOpd;
    }

    public void setTotalOpd(double totalOpd) {
        this.totalOpd = totalOpd;
    }

    public double getTotalcc() {
        return totalcc;
    }

    public void setTotalcc(double totalcc) {
        this.totalcc = totalcc;
    }

    public double getTotalInward() {
        return totalInward;
    }

    public void setTotalInward(double totalInward) {
        this.totalInward = totalInward;
    }

    double totalOpdCount;
    double totalOpd;
    double totalccCount;
    double totalcc;
    double totalInwardCount;
    double totalInward;

    public double getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(double totalCount) {
        this.totalCount = totalCount;
    }

    public double getTotalOpdCount() {
        return totalOpdCount;
    }

    public void setTotalOpdCount(double totalOpdCount) {
        this.totalOpdCount = totalOpdCount;
    }

    public double getTotalccCount() {
        return totalccCount;
    }

    public void setTotalccCount(double totalccCount) {
        this.totalccCount = totalccCount;
    }

    public double getTotalInwardCount() {
        return totalInwardCount;
    }

    public void setTotalInwardCount(double totalInwardCount) {
        this.totalInwardCount = totalInwardCount;
    }

    public void createLabServiceWithCountByMachineAndItem() {
        investigationCountWithMachines = new ArrayList<>();
        totalCount = 0;
        for (Machine w : getInvestigationMachines()) {
            InvestigationCountWithMachine temp = new InvestigationCountWithMachine();
            temp.setMachine(w);

            long billed = getCount(new BilledBill(), w);
            long cancelled = getCount(new CancelledBill(), w);
            long refunded = getCount(new RefundBill(), w);

            long net = billed - (cancelled + refunded);

            temp.setCount(net);
            temp.setTotal(getTotal(w));

            totalCount += net;

            if (temp.getCount() != 0) {
                ////// // System.out.println(investigationCountWithMachines.size() + " " + temp.getMachine().getName());
                investigationCountWithMachines.add(temp);
            }

        }

    }

    public void createLabServiceWithCountByMachine() {

        Date startTime = new Date();
        ////// // System.out.println("createLabServiceWithCountByMachine");
        investigationCountWithMachines = new ArrayList<>();
        totalCount = 0;
        for (Machine w : getInvestigationMachines()) {
            InvestigationCountWithMachine temp = new InvestigationCountWithMachine();
            temp.setMachine(w);

            long billed = getCount(new BilledBill(), w);
            long cancelled = getCount(new CancelledBill(), w);
            long refunded = getCount(new RefundBill(), w);

            long net = billed - (cancelled + refunded);

            temp.setCount(net);
            temp.setTotal(getTotal(w));

            totalCount += net;

            if (temp.getCount() != 0) {
                ////// // System.out.println(investigationCountWithMachines.size() + " " + temp.getMachine().getName());
                investigationCountWithMachines.add(temp);
            }

        }

        

    }

    public void createLabServiceWithCountByMachineAndBillType() {

        Date startTime = new Date();
        ////// // System.out.println("createLabServiceWithCountByMachine");
        investigationCountWithMachines = new ArrayList<>();
        totalCount = 0;
        totalOpdCount = 0;
        totalccCount = 0;
        totalInwardCount = 0;
        BillType[] bts = {BillType.OpdBill, BillType.LabBill, BillType.InwardBill, BillType.CollectingCentreBill};
        for (Machine w : getInvestigationMachines(bts)) {

            if (w == null) {
                continue;
            }

            InvestigationCountWithMachine tempMac = new InvestigationCountWithMachine();
            List<InvestigationCountWithMachine> lst = new ArrayList<>();

            tempMac.setMachine(w);

            long grandCount = 0;
            long opdCount = 0;
            long ccCount = 0;
            long inwardCount = 0;

            for (Item ix : getBilledMachineItems(w, bts)) {
                InvestigationCountWithMachine temp = new InvestigationCountWithMachine();
                temp.setMachine(w);
                temp.setInvestigation((Investigation) ix);
                long billed = getItemCount(new BilledBill(), ix);
                long cancelled = getItemCount(new CancelledBill(), ix);
                long refunded = getItemCount(new RefundBill(), ix);
                long net = billed - (cancelled + refunded);
                temp.setCount(net);
                temp.setTotal(getTotal(w));
                totalCount += net;
                grandCount += net;

                billed = getItemCount(new BilledBill(), ix, BillType.OpdBill);
                cancelled = getItemCount(new CancelledBill(), ix, BillType.OpdBill);
                refunded = getItemCount(new RefundBill(), ix, BillType.OpdBill);
                net = billed - (cancelled + refunded);
                opdCount += net;
                totalOpdCount += net;
                temp.setOpdCount(net);
                ////// // System.out.println("temp.getInvestigation().getName() = " + temp.getInvestigation().getName());
                ////// // System.out.println("billed = " + billed);
                ////// // System.out.println("cancelled = " + cancelled);
                ////// // System.out.println("refunded = " + refunded);
                ////// // System.out.println("temp.getOpdCount() = " + temp.getOpdCount());

                billed = getItemCount(new BilledBill(), ix, BillType.LabBill);
                billed += getItemCount(new BilledBill(), ix, BillType.CollectingCentreBill);
                cancelled = getItemCount(new CancelledBill(), ix, BillType.LabBill);
                cancelled += getItemCount(new CancelledBill(), ix, BillType.CollectingCentreBill);
                refunded = getItemCount(new RefundBill(), ix, BillType.LabBill);
                refunded += getItemCount(new RefundBill(), ix, BillType.CollectingCentreBill);
                net = billed - (cancelled + refunded);
                ccCount += net;
                totalccCount += net;
                temp.setCcCount(net);

                billed = getItemCount(new BilledBill(), ix, BillType.InwardBill);
                cancelled = getItemCount(new CancelledBill(), ix, BillType.InwardBill);
                refunded = getItemCount(new RefundBill(), ix, BillType.InwardBill);
                net = billed - (cancelled + refunded);
                inwardCount += net;
                totalInwardCount += net;
                temp.setInwardCount(net);

                if (temp.getCount() != 0) {
                    ////// // System.out.println(investigationCountWithMachines.size() + " " + temp.getMachine().getName());
                    lst.add(temp);
                }

            }
            tempMac.setCount(grandCount);
            tempMac.setOpdCount(opdCount);
            tempMac.setCcCount(ccCount);
            tempMac.setInwardCount(inwardCount);
            tempMac.setListOfInvestigationCounts(lst);
            investigationCountWithMachines.add(tempMac);
        }

        
    }

    public void createLabServiceWithCountAndValueByMachineAndBillType() {

        Date startTime = new Date();
        investigationCountWithMachines = new ArrayList<>();
        totalCount = 0;
        totalOpdCount = 0;
        totalccCount = 0;
        totalInwardCount = 0;
        total = 0;
        totalOpd = 0;
        totalcc = 0;
        totalInward = 0;

        BillType[] bts = {BillType.OpdBill, BillType.LabBill, BillType.InwardBill, BillType.CollectingCentreBill};
        List<Object[]> objects = getCount(Arrays.asList(bts));
//        List<Object[]> objects = getCount(Arrays.asList(new BillType[]{BillType.OpdBill}));
        Machine lastMachine = null;
        InvestigationCountWithMachine row = new InvestigationCountWithMachine();
        for (Object[] ob : objects) {
            Machine m = (Machine) ob[0];
            BillType bt = (BillType) ob[1];
            BillClassType classType = (BillClassType) ob[2];
            long count = (long) ob[3];
            double tot = (double) ob[4];
            if (m == null) {
                continue;
            }
            if (lastMachine == null) {
                lastMachine = m;
                row.setMachine(m);
                row.setCountAndTotal(count, bt, classType, tot);
                //// // System.out.println("m.getName() = " + m.getName());
                //// // System.out.println("bt = " + bt);
                continue;
            }
            //// // System.out.println("m.getName() = " + m.getName());
            //// // System.out.println("bt = " + bt);
            //// // System.out.println("lastMachine.getName() = " + lastMachine.getName());
            if (lastMachine == m) {
                row.setCountAndTotal(count, bt, classType, tot);
            } else {
                totalOpdCount += row.getOpdCount();
                totalccCount += row.getCcCount();
                totalInwardCount += row.getInwardCount();
//                totalCount += (totalOpdCount + totalccCount + totalInwardCount);
                totalOpd += row.getOpdTotal();
                totalcc += row.getCcTotal();
                totalInward += row.getInwardTotal();
//                total += (totalOpd + totalcc + totalInward);
                investigationCountWithMachines.add(row);
                row = new InvestigationCountWithMachine();
                row.setMachine(m);
                row.setCountAndTotal(count, bt, classType, tot);
                lastMachine = m;
            }

        }

        totalOpdCount += row.getOpdCount();
        totalccCount += row.getCcCount();
        totalInwardCount += row.getInwardCount();
        totalCount += (totalOpdCount + totalccCount + totalInwardCount);
        totalOpd += row.getOpdTotal();
        totalcc += row.getCcTotal();
        totalInward += row.getInwardTotal();
        total += (totalOpd + totalcc + totalInward);
        investigationCountWithMachines.add(row);

        
    }

    public void createLabServiceWithCountAndValueByMachineInvestigationAndBillType() {

        Date startTime = new Date();
        investigationCountWithMachines = new ArrayList<>();
        totalCount = 0;
        totalOpdCount = 0;
        totalccCount = 0;
        totalInwardCount = 0;
        total = 0;
        totalOpd = 0;
        totalcc = 0;
        totalInward = 0;

        BillType[] bts = {BillType.OpdBill, BillType.LabBill, BillType.InwardBill, BillType.CollectingCentreBill};
        List<Object[]> objects = getCountWithInvestigation(Arrays.asList(bts));
//        List<Object[]> objects = getCount(Arrays.asList(new BillType[]{BillType.OpdBill}));
//        List<Object[]> objects = getCount(Arrays.asList(new BillType[]{BillType.OpdBill}));
        InvestigationCountWithMachine row = new InvestigationCountWithMachine();
        InvestigationCountWithMachine rowInv = new InvestigationCountWithMachine();
        List<InvestigationCountWithMachine> listRowInv = new ArrayList<>();
        int j = 1;
        Machine lastMachine = null;
        Investigation lastInvestigation = null;
        for (Object[] ob : objects) {
            Machine m = (Machine) ob[0];
            Investigation i = (Investigation) ob[1];
            BillType bt = (BillType) ob[2];
            BillClassType classType = (BillClassType) ob[3];
            long count = (long) ob[4];
            double tot = (double) ob[5];
            if (m == null) {
                continue;
            }
            if (lastMachine == null) {
                row.setMachine(m);
                lastMachine = m;
                if (lastInvestigation == null) {
                    rowInv.setInvestigation(i);
                    rowInv.setCountAndTotal(count, bt, classType, tot);
                    lastInvestigation = i;
                } else {
                    if (lastInvestigation == i) {
                        rowInv.setCountAndTotal(count, bt, classType, tot);
                    } else {
                        listRowInv.add(rowInv);
                        rowInv = new InvestigationCountWithMachine();
                        rowInv.setInvestigation(i);
                        rowInv.setCountAndTotal(count, bt, classType, tot);
                        lastInvestigation = i;
                    }
                }
            } else {
                if (lastMachine == m) {
                    if (lastInvestigation == null) {
                        rowInv.setInvestigation(i);
                        rowInv.setCountAndTotal(count, bt, classType, tot);
                        lastInvestigation = i;
                    } else {
                        if (lastInvestigation == i) {
                            rowInv.setCountAndTotal(count, bt, classType, tot);
                        } else {
                            rowInv.setCount(rowInv.getOpdCount() + rowInv.getCcCount() + rowInv.getInwardCount());
                            rowInv.setTotal(rowInv.getOpdTotal() + rowInv.getCcTotal() + rowInv.getInwardTotal());

                            row.setOpdCount(row.getOpdCount() + rowInv.getOpdCount());
                            row.setCcCount(row.getCcCount() + rowInv.getCcCount());
                            row.setInwardCount(row.getInwardCount() + rowInv.getInwardCount());
                            row.setCount(row.getOpdCount() + row.getCcCount() + row.getInwardCount());

                            row.setOpdTotal(row.getOpdTotal() + rowInv.getOpdTotal());
                            row.setCcTotal(row.getCcTotal() + rowInv.getCcTotal());
                            row.setInwardTotal(row.getInwardTotal() + rowInv.getInwardTotal());
                            row.setTotal(row.getOpdTotal() + row.getCcTotal() + row.getInwardTotal());

                            listRowInv.add(rowInv);
                            rowInv = new InvestigationCountWithMachine();
                            rowInv.setInvestigation(i);
                            rowInv.setCountAndTotal(count, bt, classType, tot);
                            lastInvestigation = i;
                        }
                    }
                } else {
                    rowInv.setCount(rowInv.getOpdCount() + rowInv.getCcCount() + rowInv.getInwardCount());
                    rowInv.setTotal(rowInv.getOpdTotal() + rowInv.getCcTotal() + rowInv.getInwardTotal());

                    row.setOpdCount(row.getOpdCount() + rowInv.getOpdCount());
                    row.setCcCount(row.getCcCount() + rowInv.getCcCount());
                    row.setInwardCount(row.getInwardCount() + rowInv.getInwardCount());
                    row.setCount(row.getOpdCount() + row.getCcCount() + row.getInwardCount());

                    row.setOpdTotal(row.getOpdTotal() + rowInv.getOpdTotal());
                    row.setCcTotal(row.getCcTotal() + rowInv.getCcTotal());
                    row.setInwardTotal(row.getInwardTotal() + rowInv.getInwardTotal());
                    row.setTotal(row.getOpdTotal() + row.getCcTotal() + row.getInwardTotal());

                    listRowInv.add(rowInv);
                    rowInv = new InvestigationCountWithMachine();
                    row.setListOfInvestigationCounts(listRowInv);
                    listRowInv = new ArrayList<>();
//                    System.err.println("***********");
//                    //// // System.out.println("totalCount = " + totalCount);
//                    //// // System.out.println("total = " + total);
//                    System.err.println("***********");
                    totalOpdCount += row.getOpdCount();
                    totalccCount += row.getCcCount();
                    totalInwardCount += row.getInwardCount();
//                    totalCount += (totalOpdCount + totalccCount + totalInwardCount);

                    totalOpd += row.getOpdTotal();
                    totalcc += row.getCcTotal();
                    totalInward += row.getInwardTotal();
//                    total += (totalOpd + totalcc + totalInward);
//                    total += (totalOpd + totalcc + totalInward);

                    investigationCountWithMachines.add(row);
//                    //// // System.out.println("row.getMachine().getName() = " + row.getMachine().getName());
//                    //// // System.out.println("totalCount = " + totalCount);
//                    //// // System.out.println("total = " + total);
                    row = new InvestigationCountWithMachine();
                    row.setMachine(m);
                    lastMachine = m;
                    if (lastInvestigation == null) {
                        rowInv.setInvestigation(i);
                        rowInv.setCountAndTotal(count, bt, classType, tot);
                        lastInvestigation = i;
                    } else {
                        if (lastInvestigation == i) {
                            rowInv.setCountAndTotal(count, bt, classType, tot);
                        } else {
                            rowInv = new InvestigationCountWithMachine();
                            rowInv.setInvestigation(i);
                            rowInv.setCountAndTotal(count, bt, classType, tot);
                            lastInvestigation = i;
                        }
                    }
                }
            }

            j++;
        }
        listRowInv.add(rowInv);
        row.setListOfInvestigationCounts(listRowInv);

        rowInv.setCount(rowInv.getOpdCount() + rowInv.getCcCount() + rowInv.getInwardCount());
        rowInv.setTotal(rowInv.getOpdTotal() + rowInv.getCcTotal() + rowInv.getInwardTotal());

        row.setOpdCount(row.getOpdCount() + rowInv.getOpdCount());
        row.setCcCount(row.getCcCount() + rowInv.getCcCount());
        row.setInwardCount(row.getInwardCount() + rowInv.getInwardCount());
        row.setCount(row.getOpdCount() + row.getCcCount() + row.getInwardCount());

        row.setOpdTotal(row.getOpdTotal() + rowInv.getOpdTotal());
        row.setCcTotal(row.getCcTotal() + rowInv.getCcTotal());
        row.setInwardTotal(row.getInwardTotal() + rowInv.getInwardTotal());
        row.setTotal(row.getOpdTotal() + row.getCcTotal() + row.getInwardTotal());

        totalOpdCount += row.getOpdCount();
        totalccCount += row.getCcCount();
        totalInwardCount += row.getInwardCount();
        totalCount += (totalOpdCount + totalccCount + totalInwardCount);

        totalOpd += row.getOpdTotal();
        totalcc += row.getCcTotal();
        totalInward += row.getInwardTotal();
        total += (totalOpd + totalcc + totalInward);

        investigationCountWithMachines.add(row);

        
    }

    public List<Item> getInvestigationItems() {
        String sql;
        Map temMap = new HashMap();
        sql = "select DISTINCT(bi.item) from BillItem bi where "
                + " type(bi.item) =:ixtype  "
                + " and bi.bill.billType=:bType "
                + " and bi.bill.toInstitution=:ins "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " order by bi.item.name";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("bType", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());
        investigations = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        ////// // System.out.println("investigations = " + investigations);

        return investigations;
    }

    public List<Machine> getInvestigationMachines() {
        String sql;
        Map temMap = new HashMap();
        sql = "select DISTINCT(bi.item.machine) from BillItem bi where "
                //                + " type(bi.item) =:ixtype  "
                + " bi.bill.billType=:bType "
                + " and bi.bill.toInstitution=:ins "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " order by bi.item.name";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
//        temMap.put("ixtype", Investigation.class);
        temMap.put("bType", BillType.OpdBill);
        temMap.put("ins", institution);
        machines = machineFacade.findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        ////// // System.out.println("investigations = " + machines);

        return machines;
    }

    public List<Machine> getInvestigationMachines(BillType[] bts) {
        String sql;
        Map temMap = new HashMap();
        sql = "select DISTINCT(bi.item.machine) from BillItem bi where "
                //                + " type(bi.item) =:ixtype  "
                + " bi.bill.billType in :bts "
                + " and bi.bill.toInstitution=:ins "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " order by bi.item.name";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
//        temMap.put("ixtype", Investigation.class);
        temMap.put("bts", Arrays.asList(bts));
        temMap.put("ins", institution);
        machines = machineFacade.findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        ////// // System.out.println("investigations = " + machines);

        return machines;
    }

    public List<Item> getBilledMachineItems(Machine ma) {
        String sql;
        List<Item> t;
        Map temMap = new HashMap();
        sql = "select DISTINCT(bi.item) from BillItem bi where "
                + " bi.bill.billType=:bType "
                + " and bi.bill.toInstitution=:ins "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " and bi.item.machine=:ma "
                + " order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.OpdBill);
        temMap.put("ins", institution);
        temMap.put("ma", ma);
        t = itemFacade.findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        return t;
    }

    public List<Item> getBilledMachineItems(Machine ma, BillType[] bts) {
        String sql;
        List<Item> t;
        Map temMap = new HashMap();
        sql = "select DISTINCT(bi.item) from BillItem bi where "
                + " bi.bill.billType in :bts "
                + " and bi.bill.toInstitution=:ins "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " and bi.item.machine=:ma "
                + " order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bts", Arrays.asList(bts));
        temMap.put("ins", institution);
        temMap.put("ma", ma);
        t = itemFacade.findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        return t;
    }

    List<Machine> machines;
    @EJB
    MachineFacade machineFacade;

    public class InvestigationCountWithMachine {

        private Investigation investigation;
        private Machine machine;
        private long count;
        private long opdCount;
        private double opdTotal;
        private long inwardCount;
        private double inwardTotal;
        private long ccCount;
        private double ccTotal;
        private double total;
        List<InvestigationCountWithMachine> listOfInvestigationCounts;

        public List<InvestigationCountWithMachine> getListOfInvestigationCounts() {
            return listOfInvestigationCounts;
        }

        public void setListOfInvestigationCounts(List<InvestigationCountWithMachine> listOfInvestigationCounts) {
            this.listOfInvestigationCounts = listOfInvestigationCounts;
        }

        public long getOpdCount() {
            return opdCount;
        }

        public void setOpdCount(long opdCount) {
            this.opdCount = opdCount;
        }

        public long getInwardCount() {
            return inwardCount;
        }

        public void setInwardCount(long inwardCount) {
            this.inwardCount = inwardCount;
        }

        public long getCcCount() {
            return ccCount;
        }

        public void setCcCount(long ccCount) {
            this.ccCount = ccCount;
        }

        public Investigation getInvestigation() {
            return investigation;
        }

        public void setInvestigation(Investigation investigation) {
            this.investigation = investigation;
        }

        public long getCount() {
            return count;
        }

        public void setCountAndTotal(long count, BillType bt, BillClassType bct, double tot) {
            switch (bt) {
                case OpdBill:
                    this.opdTotal += tot;
                    switch (bct) {
                        case BilledBill:
                            this.opdCount += count;
                            break;
                        case CancelledBill:
                            this.opdCount -= count;
                            break;
                        case RefundBill:
                            this.opdCount -= count;
                            break;
                        default:
                            throw new AssertionError();
                    }
                    break;
                case LabBill:
                    this.ccTotal += tot;
                    switch (bct) {
                        case BilledBill:
                            this.ccCount += count;
                            break;
                        case CancelledBill:
                            this.ccCount -= count;
                            break;
                        case RefundBill:
                            this.ccCount -= count;
                            break;
                        default:
                            throw new AssertionError();
                    }
                    break;
                case InwardBill:
                    this.inwardTotal += tot;
                    switch (bct) {
                        case BilledBill:
                            this.inwardCount += count;
                            break;
                        case CancelledBill:
                            this.inwardCount -= count;
                            break;
                        case RefundBill:
                            this.inwardCount -= count;
                            break;
                        default:
                            throw new AssertionError();
                    }
                    break;
                case CollectingCentreBill:
                    this.ccTotal += tot;
                    switch (bct) {
                        case BilledBill:
                            this.ccCount += count;
                            break;
                        case CancelledBill:
                            this.ccCount -= count;
                            break;
                        case RefundBill:
                            this.ccCount -= count;
                            break;
                        default:
                            throw new AssertionError();
                    }
                    break;
                default:
                    throw new AssertionError();
            }
        }

        public void setCount(long count) {
            this.count = count;
        }

        public double getTotal() {
            return total;
        }

        public void setTotal(double total) {
            this.total = total;
        }

        public Machine getMachine() {
            return machine;
        }

        public void setMachine(Machine machine) {
            this.machine = machine;
        }

        public double getOpdTotal() {
            return opdTotal;
        }

        public void setOpdTotal(double opdTotal) {
            this.opdTotal = opdTotal;
        }

        public double getInwardTotal() {
            return inwardTotal;
        }

        public void setInwardTotal(double inwardTotal) {
            this.inwardTotal = inwardTotal;
        }

        public double getCcTotal() {
            return ccTotal;
        }

        public void setCcTotal(double ccTotal) {
            this.ccTotal = ccTotal;
        }

    }

    public List<InvestigationCountWithMachine> getInvestigationCountWithMachines() {
        return investigationCountWithMachines;
    }

    public void setInvestigationCountWithMachines(List<InvestigationCountWithMachine> investigationCountWithMachines) {
        this.investigationCountWithMachines = investigationCountWithMachines;
    }

    public void setItemDetails(List<InvestigationSummeryData> itemDetails) {
        this.itemDetails = itemDetails;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public long getCountTotal() {
        countTotal = 0;

        long billed = getCount(new BilledBill());
        long cancelled = getCount(new CancelledBill());
        long refunded = getCount(new RefundBill());

        countTotal = billed - (refunded + cancelled);

        //  //System.err.println("Billed : " + billed);
        //   //System.err.println("Cancelled : " + cancelled);
        //   //System.err.println("Refunded : " + refunded);
        //   //System.err.println("Gross Tot : " + countTotal);
        return countTotal;
    }

    public long getCountTotal2() {

        //  //System.err.println("Billed : " + billed);
        //  //System.err.println("Cancelled : " + cancelled);
        //  //System.err.println("Refunded : " + refunded);
        //  //System.err.println("Gross Tot : " + countTotal);
        return countTotal;
    }

    public void setCountTotal(long countTotal) {
        this.countTotal = countTotal;
    }

    public Institution getCollectingIns() {
        return collectingIns;
    }

    public void setCollectingIns(Institution collectingIns) {
        this.collectingIns = collectingIns;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;

    }

    public List<InvestigationSummeryData> getInvestigationSummeryDatas() {
        return investigationSummeryDatas;
    }

    public void setInvestigationSummeryDatas(List<InvestigationSummeryData> investigationSummeryDatas) {
        this.investigationSummeryDatas = investigationSummeryDatas;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public class institutionInvestigationCountRow {

        Institution institution;
        Institution collectingCentre;
        Item item;
        Investigation investigation;
        Double count;

        public institutionInvestigationCountRow(Institution institution, Institution collectingCentre, Item item, Double count) {
            this.institution = institution;
            this.collectingCentre = collectingCentre;
            this.item = item;
            this.count = count;
        }

        public Institution getInstitution() {
            return institution;
        }

        public void setInstitution(Institution institution) {
            this.institution = institution;
        }

        public Institution getCollectingCentre() {
            return collectingCentre;
        }

        public void setCollectingCentre(Institution collectingCentre) {
            this.collectingCentre = collectingCentre;
        }

        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }

        public Investigation getInvestigation() {
            return investigation;
        }

        public void setInvestigation(Investigation investigation) {
            this.investigation = investigation;
        }

        public Double getCount() {
            return count;
        }

        public void setCount(Double count) {
            this.count = count;
        }

    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

}
