/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.report;

import com.divudi.bean.common.SessionController;
import com.divudi.data.BillType;
import com.divudi.data.dataStructure.InvestigationSummeryData;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.RefundBill;
import com.divudi.entity.lab.Investigation;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientInvestigationFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
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
    private List<InvestigationSummeryData> items;
    private List<InvestigationSummeryData> itemDetails;
    private List<Item> investigations;
    List<InvestigationSummeryData> itemsLab;

    /**
     * Creates a new instance of CashierReportController
     */
    public InvestigationMonthSummeryOwnController() {
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
        items = new ArrayList<>();

        for (Item w : getInvestigations()) {
            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            setCountTotal(temp, w);
            if (temp.getCount() != 0) {
                items.add(temp);
            }
        }

        return items;
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

    public void createItemList3() {
        itemsLab = new ArrayList<>();
        countTotal = 0;
        for (Item w : getInvestigations3()) {
            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            long temCoint = calculateInvestigationBilledCount(w);
            temp.setCount(temCoint);
            countTotal += temCoint;
            if (temp.getCount() != 0) {
                itemsLab.add(temp);
            }
        }
//        countTotal = 0;
//
//        long billed = getCount2(new BilledBill());
//        System.out.println("billed = " + billed);
//        long cancelled = getCount2(new CancelledBill());
//        System.out.println("cancelled = " + cancelled);
//        long refunded = getCount2(new RefundBill());
//        System.out.println("refunded = " + refunded);
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
        List<BillItem> temps = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

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
        return getBillItemFacade().countBySql(sql, temMap, TemporalType.TIMESTAMP);

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
        return getBillItemFacade().countBySql(sql, temMap, TemporalType.TIMESTAMP);

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
        return getBillItemFacade().countBySql(sql, temMap, TemporalType.TIMESTAMP);

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
        return getBillItemFacade().countBySql(sql, temMap, TemporalType.TIMESTAMP);

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
        return getBillItemFacade().countBySql(sql, temMap, TemporalType.TIMESTAMP);

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
        investigations = getItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        return investigations;
    }

    public List<Item> getInvestigations() {
        String sql;
        Map temMap = new HashMap();
        sql = "select distinct ix from BillItem bi join bi.item ix where type(ix) =:ixtype  "
                + "and bi.bill.billType=:bType and bi.bill.toInstitution=:ins "
                + "and bi.bill.createdAt between :fromDate and :toDate order by ix.name";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("bType", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());
        investigations = getItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

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
        investigations = getItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

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
        investigations = getItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

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

        List<BillItem> temps = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
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
        List<BillItem> temps = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        t.setBillItems(temps);

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
}
