/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.AuditEventApplicationController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillType;
import com.divudi.data.dataStructure.StockReportRecord;
import com.divudi.data.inward.SurgeryBillType;
import com.divudi.data.table.String1Value3;

import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.AuditEvent;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.entity.pharmacy.ItemBatch;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.StockFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.joda.time.Days;
import org.joda.time.LocalDate;

/**
 *
 * @author Buddhika
 */
@Named(value = "reportsTransfer")
@SessionScoped
public class ReportsTransfer implements Serializable {

    /**
     * Bean Variables
     */
    Department fromDepartment;
    Department toDepartment;
    Department department;
    Date fromDate;
    Date toDate;
    BillType[] billTypes;

    Institution institution;
    List<Stock> stocks;
    List<ItemCount> itemCounts;
    List<ItemCountWithOutMargin> itemCountWithOutMargins;
    double saleValue;
    double purchaseValue;
    double totalsValue;
    double discountsValue;
    double marginValue;
    double netTotalValues;
    double retailValue;
    Category category;

    List<BillItem> transferItems;
    List<Bill> transferBills;
    private List<ItemBHTIssueCountTrancerReciveCount> itemBHTIssueCountTrancerReciveCounts;

    List<StockReportRecord> movementRecords;
    List<StockReportRecord> movementRecordsQty;
    @Inject
    BillBeanController billBeanController;

    double stockPurchaseValue;
    double stockSaleValue;
    double valueOfQOH;
    double qoh;
    double totalIssueQty;
    double totalBHTIssueQty;
    double totalIssueValue;
    double totalBHTIssueValue;

    /**
     * EJBs
     */
    @EJB
    StockFacade stockFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFacade BillFacade;
    @EJB
    PharmacyBean pharmacyBean;
    @EJB
    ItemFacade itemFacade;
    
    
    CommonFunctions commonFunctions;

    ////////////
    @Inject
    CommonController commonController;
    
    @Inject
    AuditEventApplicationController auditEventApplicationController;
    @Inject
    private SessionController sessionController;

    /**
     * Methods
     */
    public void fillFastMoving() {
        Date startTime = new Date();

        fillMoving(true);
        fillMovingQty(true);

        
    }

    public void fillSlowMoving() {
        Date startTime = new Date();

        fillMoving(false);
        fillMovingQty(false);

        
    }

    public BillBeanController getBillBeanController() {
        return billBeanController;
    }

    public void setBillBeanController(BillBeanController billBeanController) {
        this.billBeanController = billBeanController;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void fillMovingWithStock() {
        String sql;
        Map m = new HashMap();
        m.put("i", institution);
        m.put("t1", BillType.PharmacyTransferIssue);
        m.put("t2", BillType.PharmacyPre);
        m.put("fd", fromDate);
        m.put("td", toDate);
        BillItem bi = new BillItem();

        sql = "select bi.item, abs(SUM(bi.pharmaceuticalBillItem.qty)), "
                + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty)), "
                + "SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.qty)) "
                + "FROM BillItem bi where bi.retired=false and  bi.bill.department.institution=:i and "
                + "(bi.bill.billType=:t1 or bi.bill.billType=:t2) and "
                + "bi.bill.billDate between :fd and :td group by bi.item "
                + "order by  SUM(bi.pharmaceuticalBillItem.qty) desc";
        List<Object[]> objs = getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
        movementRecordsQty = new ArrayList<>();
        for (Object[] obj : objs) {
            StockReportRecord r = new StockReportRecord();
            r.setItem((Item) obj[0]);
            r.setQty((Double) obj[1]);
            Days daysBetween = Days.daysBetween(LocalDate.fromDateFields(fromDate), LocalDate.fromDateFields(toDate));
            int ds = daysBetween.getDays();
            r.setPurchaseValue((Double) (r.getQty() / ds));
//            r.setRetailsaleValue((Double) obj[2]);
            r.setStockQty(getPharmacyBean().getStockQty(r.getItem(), institution));
            movementRecordsQty.add(r);
        }
    }

    public void fillMoving(boolean fast) {
        String sql;
        Map m = new HashMap();
//        m.put("r", StockReportRecord.class);
        m.put("d", department);
//        m.put("t1", BillType.PharmacyTransferIssue);
//        m.put("t2", BillType.PharmacyPre);
        m.put("fd", fromDate);
        m.put("td", toDate);
        List<BillType> bts = Arrays.asList(billTypes);
        Class[] btsa = {PreBill.class, CancelledBill.class, RefundBill.class, BilledBill.class};
        List<Class> bcts = Arrays.asList(btsa);
        m.put("bt", bts);
        m.put("bct", bcts);
        BillItem bi = new BillItem();

        sql = "select bi.item, abs(SUM(bi.pharmaceuticalBillItem.qty)), "
                + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty)), "
                + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.qty))  "
                + "FROM BillItem bi where "
                + " type(bi.bill) in :bct "
                + " and bi.retired=false "
                + " and bi.bill.department=:d "
                + " and bi.bill.createdAt between :fd and :td "
                + " and bi.bill.billType in :bt "
                + " group by bi.item ";

        if (!fast) {
            sql += "order by  SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.pharmaceuticalBillItem.qty) desc";
        } else {
            sql += "order by  SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.pharmaceuticalBillItem.qty) asc";
        }
        ////System.out.println("sql = " + sql);
        ////System.out.println("m = " + m);
        List<Object[]> objs = getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
        movementRecords = new ArrayList<>();
        if (objs == null) {
            ////System.out.println("objs = " + objs);
            return;
        }
        for (Object[] obj : objs) {
            StockReportRecord r = new StockReportRecord();
            r.setItem((Item) obj[0]);
            r.setQty((Double) obj[1]);
            r.setPurchaseValue((Double) obj[2]);
            r.setRetailsaleValue((Double) obj[3]);
            r.setStockQty(getPharmacyBean().getStockByPurchaseValue(r.getItem(), department));
            r.setStockOnHand(getPharmacyBean().getStockWithoutPurchaseValue(r.getItem(), department));
            movementRecords.add(r);
        }

        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        valueOfQOH = 0.0;
        qoh = 0.0;

        for (StockReportRecord strr : movementRecords) {
            stockPurchaseValue = stockPurchaseValue + (strr.getPurchaseValue());
            stockSaleValue = stockSaleValue + (strr.getRetailsaleValue());
            valueOfQOH = valueOfQOH + (strr.getStockQty());
            qoh = qoh + (strr.getStockOnHand());
        }
    }

    public void fillMovingQty(boolean fast) {
        String sql;
        Map m = new HashMap();

        List<BillType> bts = Arrays.asList(billTypes);

        m.put("d", department);
//        m.put("t1", BillType.PharmacyTransferIssue);
//        m.put("t2", BillType.PharmacyPre);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", bts);

        Class[] btsa = {PreBill.class, CancelledBill.class, RefundBill.class, BilledBill.class};
        List<Class> bcts = Arrays.asList(btsa);
        m.put("bct", bcts);
        BillItem bi = new BillItem();

        sql = "select bi.item, "
                + " abs(SUM(bi.pharmaceuticalBillItem.qty)), "
                + " abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty)), "
                + " SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.qty)"
                + " FROM BillItem bi "
                + " where bi.retired=false "
                + " and  bi.bill.department=:d "
                + " and bi.bill.billType in :bt "
                + " and type(bi.bill) in :bct "
                + " and bi.bill.createdAt between :fd and :td "
                + " group by bi.item ";

        if (!fast) {
            sql += "order by  SUM(bi.pharmaceuticalBillItem.qty) desc";
        } else {
            sql += "order by  SUM(bi.pharmaceuticalBillItem.qty) asc";
        }
        List<Object[]> objs = getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
        movementRecordsQty = new ArrayList<>();
        if (objs == null) {
            return;
        }
        for (Object[] obj : objs) {
            StockReportRecord r = new StockReportRecord();
            r.setItem((Item) obj[0]);
            r.setQty((Double) obj[1]);
            r.setPurchaseValue((Double) obj[2]);
            r.setRetailsaleValue((Double) obj[3]);
            r.setStockQty(getPharmacyBean().getStockByPurchaseValue(r.getItem(), department));
            r.setStockOnHand(getPharmacyBean().getStockWithoutPurchaseValue(r.getItem(), department));
            movementRecordsQty.add(r);
        }

        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        valueOfQOH = 0.0;
        qoh = 0.0;

        for (StockReportRecord strr : movementRecords) {
            stockPurchaseValue = stockPurchaseValue + (strr.getPurchaseValue());
            stockSaleValue = stockSaleValue + (strr.getRetailsaleValue());
            valueOfQOH = valueOfQOH + (strr.getStockQty());
            qoh = qoh + (strr.getStockOnHand());
        }
    }

    public void fillDepartmentTransfersReceive() {
        Date startTime = new Date();

        transferItems = fetchBillItems(BillType.PharmacyTransferReceive);
        purchaseValue = 0.0;
        saleValue = 0.0;
        for (BillItem ts : transferItems) {
            purchaseValue += (ts.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate() * ts.getPharmaceuticalBillItem().getQtyInUnit());
            saleValue += (ts.getPharmaceuticalBillItem().getItemBatch().getRetailsaleRate() * ts.getPharmaceuticalBillItem().getQtyInUnit());
        }

        
    }

    public void fillDepartmentTransfersIssueByBillItem() {
        Date startTime = new Date();

        transferItems = fetchBillItems(BillType.PharmacyTransferIssue);
        purchaseValue = 0.0;
        saleValue = 0.0;
        for (BillItem ts : transferItems) {
            purchaseValue += (ts.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate() * ts.getPharmaceuticalBillItem().getQtyInUnit());
            saleValue += (ts.getPharmaceuticalBillItem().getItemBatch().getRetailsaleRate() * ts.getPharmaceuticalBillItem().getQtyInUnit());
        }

        
    }
    
    public void fillDepartmentAdjustmentByBillItem() {
        Date startTime = new Date();
        transferItems = fetchBillItems(BillType.PharmacyAdjustment);
        
    }

    public List<BillItem> fetchBillItems(BillType bt) {
        List<BillItem> billItems = new ArrayList<>();

        Map m = new HashMap();
        String sql;
        sql = "select bi from BillItem bi where "
                + " bi.bill.createdAt between :fd and :td "
                + " and bi.bill.billType=:bt ";

        if (bt == BillType.PharmacyTransferIssue) {
            if (fromDepartment != null) {
                sql += " and bi.bill.department=:fdept ";
                m.put("fdept", fromDepartment);
            }

            if (toDepartment != null) {
                sql += " and bi.bill.toDepartment=:tdept ";
                m.put("tdept", toDepartment);
            }
        }
        
        if (bt == BillType.PharmacyTransferReceive) {
            if (fromDepartment != null) {
                sql += " and bi.bill.fromDepartment=:fdept ";
                m.put("fdept", fromDepartment);
            }

            if (toDepartment != null) {
                sql += " and bi.bill.department=:tdept ";
                m.put("tdept", toDepartment);
            }
        }

        sql += " order by bi.id";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", bt);

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);


        return billItems;
    }

    public void fillDepartmentTransfersIssueByBill() {
        Date startTime = new Date();

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyTransferIssue);
        if (fromDepartment != null && toDepartment != null) {
            m.put("fdept", fromDepartment);
            m.put("tdept", toDepartment);
            sql = "select b from Bill b where b.department=:fdept"
                    + " and b.toDepartment=:tdept "
                    + " and b.createdAt between :fd "
                    + " and :td and b.billType=:bt "
                    + " and b.retired=false "
                    + " order by b.id";
        } else if (fromDepartment == null && toDepartment != null) {
            m.put("tdept", toDepartment);
            sql = "select b from Bill b where"
                    + " b.toDepartment=:tdept and b.createdAt "
                    + " between :fd and :td "
                    + " and b.retired=false "
                    + " b.billType=:bt order by b.id";
        } else if (fromDepartment != null && toDepartment == null) {
            m.put("fdept", fromDepartment);
            sql = "select b from Bill b where "
                    + " b.department=:fdept and b.createdAt "
                    + " between :fd and :td "
                    + " and b.retired=false "
                    + "  b.billType=:bt order by b.id";
        } else {
            sql = "select b from Bill b where b.createdAt "
                    + " between :fd and :td and b.billType=:bt "
                    + " and b.retired=false "
                    + " order by b.id";
        }
        transferBills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        for (Bill b : transferBills) {
            totalsValue = totalsValue + (b.getTotal());
            discountsValue = discountsValue + b.getDiscount();
            netTotalValues = netTotalValues + b.getNetTotal();
        }

        
    }

    public void fillDepartmentBHTIssueByBill() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillDepartmentBHTIssueByBill()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyBhtPre);
        m.put("fdept", fromDepartment);
        sql = "select b from Bill b "
                + " where b.department=:fdept "
                + " and b.createdAt  between :fd and :td "
                + " and b.billType=:bt order by b.id";
        transferBills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        marginValue = 0;
        for (Bill b : transferBills) {
            totalsValue = totalsValue + (b.getTotal());
            discountsValue = discountsValue + b.getDiscount();
            marginValue += b.getMargin();
            netTotalValues = netTotalValues + b.getNetTotal();
        }
        
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
       

        
    }

    Item item;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void fillDepartmentBHTIssueByBillItems() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillDepartmentBHTIssueByBillItems()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyBhtPre);
        m.put("fdept", fromDepartment);
        sql = "select b from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.createdAt  between :fd and :td ";

        if (item != null) {
            sql += " and b.item=:itm ";
            m.put("itm", item);
        }

        sql += " and b.bill.billType=:bt "
                + " order by b.item.name";
        transferItems = billItemFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        marginValue = 0;
        for (BillItem b : transferItems) {
            totalsValue = totalsValue + (b.getGrossValue());
            discountsValue = discountsValue + b.getDiscount();
            marginValue += b.getMarginValue();
            netTotalValues = netTotalValues + b.getNetValue();
        }
        
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        
    }

    List<String1Value3> listz;

    public List<String1Value3> getListz() {

        return listz;
    }

    public void setListz(List<String1Value3> listz) {
        this.listz = listz;
    }

    public void createDepartmentIssue() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("createDepartmentIssue()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        listz = new ArrayList<>();

        List<Object[]> list = getBillBeanController().fetchBilledDepartmentItem(getFromDate(), getToDate(), getFromDepartment());
        if (list == null) {
            return;
        }

        netTotalValues = 0;
        for (Object[] obj : list) {
            Department item = (Department) obj[0];
            Double dbl = (Double) obj[1];
            //double count = 0;

            String1Value3 newD = new String1Value3();
            newD.setString(item.getName());
            newD.setValue1(dbl);
            newD.setSummery(false);
            listz.add(newD);

            netTotalValues += dbl;

        }
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
      
        

    }

    public void createTransferIssueBillSummery() {
        Date startTime = new Date();

        fetchBillTotalByToDepartment(fromDate, toDate, fromDepartment, BillType.PharmacyTransferIssue);

        
    }

    public void createTransferReciveBillSummery() {
        fetchBillTotalByFromDepartment(fromDate, toDate, fromDepartment, BillType.PharmacyTransferReceive);
    }

    public void fetchBillTotalByToDepartment(Date fd, Date td, Department dep, BillType bt) {
        listz = new ArrayList<>();
        netTotalValues = 0.0;

        List<Object[]> objects = getBillBeanController().fetchBilledDepartmentItem(fd, td, dep, bt,true);

        for (Object[] ob : objects) {
            Department d = (Department) ob[0];
            double dbl = (double) ob[1];

            String1Value3 sv = new String1Value3();
            sv.setString(d.getName());
            sv.setValue1(dbl);
            listz.add(sv);

            netTotalValues += dbl;

        }

    }
    
    public void fetchBillTotalByFromDepartment(Date fd, Date td, Department dep, BillType bt) {
        listz = new ArrayList<>();
        netTotalValues = 0.0;

        List<Object[]> objects = getBillBeanController().fetchBilledDepartmentItem(fd, td, dep, bt,false);

        for (Object[] ob : objects) {
            Department d = (Department) ob[0];
            double dbl = (double) ob[1];

            String1Value3 sv = new String1Value3();
            sv.setString(d.getName());
            sv.setValue1(dbl);
            listz.add(sv);

            netTotalValues += dbl;

        }

    }

    public void createDepartmentIssueStore() {
        listz = new ArrayList<>();

        List<Object[]> list = getBillBeanController().fetchBilledDepartmentItemStore(getFromDate(), getToDate(), getFromDepartment());
        if (list == null) {
            return;
        }

        for (Object[] obj : list) {
            Department item = (Department) obj[0];
            Double dbl = (Double) obj[1];
            //double count = 0;

            String1Value3 newD = new String1Value3();
            newD.setString(item.getName());
            newD.setValue1(dbl);
            newD.setSummery(false);
            listz.add(newD);

        }

        netTotalValues = getBillBeanController().calNetTotalBilledDepartmentItemStore(fromDate, toDate, department);

    }

    public void fillDepartmentUnitIssueByBill() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillDepartmentUnitIssueByBill()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        
       
        Map m = new HashMap();
        String sql;

        sql = "select b from Bill b where "
                + " b.createdAt "
                + " between :fd and :td  "
                + " and b.billType=:bt ";
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyIssue);

        if (fromDepartment != null) {
            sql += " and b.fromDepartment=:fdept ";
            m.put("fdept", fromDepartment);
        }

        if (toDepartment != null) {
            sql += " and b.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        sql += " order by b.id";

        transferBills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        for (Bill b : transferBills) {
            totalsValue = totalsValue + (b.getTotal());
            discountsValue = discountsValue + b.getDiscount();
            netTotalValues = netTotalValues + b.getNetTotal();
        }
         Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
    
    
        
    }

    public void fillDepartmentUnitIssueByBillStore() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.StoreIssue);
        m.put("fdept", fromDepartment);
        m.put("tdept", toDepartment);
        sql = "select b from Bill b where "
                + " b.fromDepartment=:fdept and "
                + " b.toDepartment=:tdept and "
                + " b.createdAt "
                + " between :fd and :td and "
                + " b.billType=:bt order by b.id";
        transferBills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        for (Bill b : transferBills) {
            totalsValue = totalsValue + (b.getTotal());
            discountsValue = discountsValue + b.getDiscount();
            netTotalValues = netTotalValues + b.getNetTotal();
        }
    }

    private List<Object[]> fetchBillItem(BillType billType) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select b.pharmaceuticalBillItem.itemBatch,"
                + " sum(b.grossValue),"
                + " sum(b.marginValue),"
                + " sum(b.discount),"
                + " sum(b.netValue)"
                + " from BillItem b "
                + " where b.bill.department=:fdept ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt"
                + " group by b.pharmaceuticalBillItem.itemBatch "
                + " order by b.item.name";

        return getBillFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
    }

    private List<Object[]> fetchBillItemWithOutMargin(BillType billType) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select DISTINCT(b.pharmaceuticalBillItem.itemBatch.item),"
                + " sum(b.netValue)"
                + " from BillItem b "
                + " where b.bill.department=:fdept ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt"
                + " group by b.pharmaceuticalBillItem.itemBatch.item "
                + " order by b.item.name";

        return getBillFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
    }

    private List<Object[]> fetchBillItem(BillType billType, SurgeryBillType surgeryBillType) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select b.pharmaceuticalBillItem.itemBatch,"
                + " sum(b.grossValue),"
                + " sum(b.marginValue),"
                + " sum(b.discount),"
                + " sum(b.netValue),"
                + " sum(b.pharmaceuticalBillItem.qty)"
                + " from BillItem b "
                + " where b.bill.department=:fdept ";

        if (surgeryBillType != null) {
            sql += " and b.bill.surgeryBillType=:surg";
            m.put("surg", surgeryBillType);
        } else {
//            sql += " and b.bill.surgeryBillType is null ";
        }

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        sql += " and b.bill.createdAt between :fd and :td "
                + " and b.bill.billType=:bt "
                + " group by b.pharmaceuticalBillItem.itemBatch "
                + " order by b.item.name ";

        return getBillFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
    }

    private Double fetchBillTotal(BillType billType) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select sum(b.netValue)"
                + " from BillItem b "
                + " where b.bill.department=:fdept ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    private Double fetchBillMargin(BillType billType) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select sum(b.netValue)"
                + " from BillItem b "
                + " where b.bill.department=:fdept ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    private Double fetchBillDiscount(BillType billType) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select sum(b.netValue)"
                + " from BillItem b "
                + " where b.bill.department=:fdept ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    private Double fetchBillNetTotal(BillType billType) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select sum(b.netValue)"
                + " from BillItem b "
                + " where b.bill.department=:fdept ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void fillItemCounts() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillItemCounts()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        
        
        List<Object[]> list = fetchBillItem(BillType.PharmacyIssue);

        if (list == null) {
            return;
        }

        itemCounts = new ArrayList<>();
        totalsValue = 0;
        marginValue = 0;
        netTotalValues = 0;
        purchaseValue = 0;
        retailValue = 0;
        for (Object[] obj : list) {
            ItemCount row = new ItemCount();
            row.setItemBatch((ItemBatch) obj[0]);
            row.setGross((Double) obj[1]);
            row.setMargin((Double) obj[2]);
            row.setNet((Double) obj[4]);

            Double pre = calCount(row.getItemBatch(), BillType.PharmacyIssue, new PreBill());
            Double preCancel = calCountCan(row.getItemBatch(), BillType.PharmacyIssue, new PreBill());
            Double returned = calCountReturn(row.getItemBatch(), BillType.PharmacyIssue, new RefundBill());

            row.setCount(pre - (preCancel + returned));

            totalsValue += row.getGross();
            marginValue += row.getMargin();
            netTotalValues += row.getNet();

            itemCounts.add(row);
        }

        billTotal = fetchBillTotal(BillType.PharmacyIssue);
        billMargin = fetchBillMargin(BillType.PharmacyIssue);
        billDiscount = fetchBillDiscount(BillType.PharmacyIssue);
        billNetTotal = fetchBillNetTotal(BillType.PharmacyIssue);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        
        
    }

    public void fillItemCountsWithOutMarginPharmacy() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillItemCountsWithOutMarginPharmacy()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        
        fillItemCountsWithOutMargin(BillType.PharmacyIssue);
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

        
    }

    public void fillItemCountsWithOutMarginStore() {
        Date startTime = new Date();
        fillItemCountsWithOutMargin(BillType.StoreIssue);

        
    }

    public void fillItemCountsWithOutMargin(BillType bt) {

        List<Object[]> list = fetchBillItemWithOutMargin(bt);
        ////System.out.println("list = " + list);
        if (list == null) {
            return;
        }

        itemCountWithOutMargins = new ArrayList<>();
        netTotalValues = 0;

        for (Object[] obj : list) {
            ItemCountWithOutMargin row = new ItemCountWithOutMargin();
            row.setItem((Item) obj[0]);
            row.setNet((Double) obj[1]);

            Double pre = calCountItem(row.getItem(), bt, new PreBill());
            Double preCancel = calCountCanItem(row.getItem(), bt, new PreBill());
            Double returned = calCountReturnItem(row.getItem(), bt, new RefundBill());

            row.setCount(pre - (preCancel + returned));

            netTotalValues += row.getNet();

            itemCountWithOutMargins.add(row);
        }

        billNetTotal = fetchBillNetTotal(bt);

    }

    public void fillItemCountsStore() {
        Date startTime = new Date();

        List<Object[]> list = fetchBillItem(BillType.StoreIssue);

        if (list == null) {
            return;
        }

        itemCounts = new ArrayList<>();
        totalsValue = 0;
        marginValue = 0;
        netTotalValues = 0;
        purchaseValue = 0;
        retailValue = 0;
        for (Object[] obj : list) {
            ItemCount row = new ItemCount();
            row.setItemBatch((ItemBatch) obj[0]);
            row.setGross((Double) obj[1]);
            row.setMargin((Double) obj[2]);
            row.setNet((Double) obj[4]);

            Double pre = calCount(row.getItemBatch(), BillType.StoreIssue, new PreBill());
            Double preCancel = calCountCan(row.getItemBatch(), BillType.StoreIssue, new PreBill());
            Double returned = calCountReturn(row.getItemBatch(), BillType.StoreIssue, new RefundBill());

            row.setCount(pre - (preCancel + returned));

            totalsValue += row.getGross();
            marginValue += row.getMargin();
            netTotalValues += row.getNet();

            itemCounts.add(row);
        }

        billTotal = fetchBillTotal(BillType.StoreIssue);
        billMargin = fetchBillMargin(BillType.StoreIssue);
        billDiscount = fetchBillDiscount(BillType.StoreIssue);
        billNetTotal = fetchBillNetTotal(BillType.StoreIssue);

        
    }

    public void fillItemCountsBht() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillItemCountsBht()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        List<Object[]> list = fetchBillItem(BillType.PharmacyBhtPre, null);

        if (list == null) {
            return;
        }

        itemCounts = new ArrayList<>();
        totalsValue = 0;
        marginValue = 0;
        discountsValue = 0;
        netTotalValues = 0;
        purchaseValue = 0;
        retailValue = 0;
        for (Object[] obj : list) {
            ItemCount row = new ItemCount();
            row.setItemBatch((ItemBatch) obj[0]);
            row.setGross((Double) obj[1]);
            row.setMargin((Double) obj[2]);
            row.setDiscount((Double) obj[3]);
            row.setNet((Double) obj[4]);
            row.setCount((Double) obj[5]);

            totalsValue += row.getGross();
            marginValue += row.getMargin();
            discountsValue += row.getDiscount();
            netTotalValues += row.getNet();

            itemCounts.add(row);
        }

        billTotal = fetchBillTotal(BillType.PharmacyBhtPre);
        billMargin = fetchBillMargin(BillType.PharmacyBhtPre);
        billDiscount = fetchBillDiscount(BillType.PharmacyBhtPre);
        billNetTotal = fetchBillNetTotal(BillType.PharmacyBhtPre);
        
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        

        

    }

    public void fillItemCountsBhtSurgery() {
        Date startTime = new Date();

        List<Object[]> list = fetchBillItem(BillType.PharmacyBhtPre, SurgeryBillType.PharmacyItem);

        if (list == null) {
            return;
        }

        itemCounts = new ArrayList<>();
        totalsValue = 0;
        marginValue = 0;
        discountsValue = 0;
        netTotalValues = 0;
        purchaseValue = 0;
        retailValue = 0;
        for (Object[] obj : list) {
            ItemCount row = new ItemCount();
            row.setItemBatch((ItemBatch) obj[0]);
            row.setGross((Double) obj[1]);
            row.setMargin((Double) obj[2]);
            row.setDiscount((Double) obj[3]);
            row.setNet((Double) obj[4]);

            Double pre = calCount(row.getItemBatch(), BillType.PharmacyBhtPre, new PreBill());
            Double preCancel = calCountCan(row.getItemBatch(), BillType.PharmacyBhtPre, new PreBill());
            Double returned = calCountReturn(row.getItemBatch(), BillType.PharmacyBhtPre, new RefundBill());

            row.setCount(pre - (preCancel + returned));

            totalsValue += row.getGross();
            marginValue += row.getMargin();
            discountsValue += row.getDiscount();
            netTotalValues += row.getNet();

            itemCounts.add(row);
        }

        billTotal = fetchBillTotal(BillType.PharmacyBhtPre);
        billMargin = fetchBillMargin(BillType.PharmacyBhtPre);
        billDiscount = fetchBillDiscount(BillType.PharmacyBhtPre);
        billNetTotal = fetchBillNetTotal(BillType.PharmacyBhtPre);

        

    }

    public void fillItemCountsBhtStore() {

        List<Object[]> list = fetchBillItem(BillType.StoreBhtPre);

        if (list == null) {
            return;
        }

        itemCounts = new ArrayList<>();
        totalsValue = 0;
        marginValue = 0;
        discountsValue = 0;
        netTotalValues = 0;
        purchaseValue = 0;
        retailValue = 0;
        for (Object[] obj : list) {
            ItemCount row = new ItemCount();
            row.setItemBatch((ItemBatch) obj[0]);
            row.setGross((Double) obj[1]);
            row.setMargin((Double) obj[2]);
            row.setDiscount((Double) obj[3]);
            row.setNet((Double) obj[4]);

            Double pre = calCount(row.getItemBatch(), BillType.StoreBhtPre, new PreBill());
            Double preCancel = calCountCan(row.getItemBatch(), BillType.StoreBhtPre, new PreBill());
            Double returned = calCountReturn(row.getItemBatch(), BillType.StoreBhtPre, new RefundBill());

            row.setCount(pre - (preCancel + returned));

            totalsValue += row.getGross();
            marginValue += row.getMargin();
            discountsValue += row.getDiscount();
            netTotalValues += row.getNet();

            itemCounts.add(row);
        }

        billTotal = fetchBillTotal(BillType.StoreBhtPre);
        billMargin = fetchBillMargin(BillType.StoreBhtPre);
        billDiscount = fetchBillDiscount(BillType.StoreBhtPre);
        billNetTotal = fetchBillNetTotal(BillType.StoreBhtPre);

    }

    double billTotal;
    double billMargin;
    double billDiscount;
    double billNetTotal;

    private Double calCount(ItemBatch item, BillType billType, Bill bill) {

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("itm", item);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select abs(sum(b.pharmaceuticalBillItem.qty))"
                + " from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.bill.billedBill is null "
                + " and type(b.bill)=:class "
                + " and b.pharmaceuticalBillItem.itemBatch=:itm ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private Double calCountReturn(ItemBatch item, BillType billType, Bill bill) {

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("itm", item);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select abs(sum(b.pharmaceuticalBillItem.qty))"
                + " from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.bill.billedBill is not null "
                + " and type(b.bill)=:class "
                + " and b.pharmaceuticalBillItem.itemBatch=:itm ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private Double calCountCan(ItemBatch item, BillType billType, Bill bill) {

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("itm", item);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select abs(sum(b.pharmaceuticalBillItem.qty))"
                + " from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.bill.billedBill is not null "
                + " and type(b.bill)=:class "
                + " and b.pharmaceuticalBillItem.itemBatch=:itm ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private Double calCountItem(Item item, BillType billType, Bill bill) {

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("itm", item);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select abs(sum(b.pharmaceuticalBillItem.qty))"
                + " from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.bill.billedBill is null "
                + " and type(b.bill)=:class "
                + " and b.pharmaceuticalBillItem.itemBatch.item=:itm ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private Double calCountReturnItem(Item item, BillType billType, Bill bill) {

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("itm", item);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select abs(sum(b.pharmaceuticalBillItem.qty))"
                + " from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.bill.billedBill is not null "
                + " and type(b.bill)=:class "
                + " and b.pharmaceuticalBillItem.itemBatch.item=:itm ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private Double calCountCanItem(Item item, BillType billType, Bill bill) {

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("itm", item);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select abs(sum(b.pharmaceuticalBillItem.qty))"
                + " from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.bill.billedBill is not null "
                + " and type(b.bill)=:class "
                + " and b.pharmaceuticalBillItem.itemBatch.item=:itm ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void fillDepartmentTransfersRecieveByBill() {
        Date startTime = new Date();

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyTransferReceive);
        if (fromDepartment != null && toDepartment != null) {
            m.put("fdept", fromDepartment);
            m.put("tdept", toDepartment);
            sql = "select b from Bill b where b.fromDepartment=:fdept"
                    + " and b.department=:tdept and b.createdAt between :fd "
                    + "and :td and b.billType=:bt order by b.id";
        } else if (fromDepartment == null && toDepartment != null) {
            m.put("tdept", toDepartment);
            sql = "select b from Bill b where b.department=:tdept and b.createdAt "
                    + " between :fd and :td and b.billType=:bt order by b.id";
        } else if (fromDepartment != null && toDepartment == null) {
            m.put("fdept", fromDepartment);
            sql = "select b from Bill b where b.fromDepartment=:fdept and b.createdAt "
                    + " between :fd and :td and b.billType=:bt order by b.id";
        } else {
            sql = "select b from Bill b where b.createdAt "
                    + " between :fd and :td and b.billType=:bt order by b.id";
        }
        transferBills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        for (Bill b : transferBills) {
            totalsValue = totalsValue + (b.getTotal());
            discountsValue = discountsValue + b.getDiscount();
            netTotalValues = netTotalValues + b.getNetTotal();
        }

        
    }

    public void fillTheaterTransfersReceiveWithBHTIssue() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillTheaterTransfersReceiveWithBHTIssue()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        
        if (fromDepartment == null || toDepartment == null) {
            JsfUtil.addErrorMessage("Please Check From To Departments");
            return;
        }
        itemBHTIssueCountTrancerReciveCounts = new ArrayList<>();
        totalIssueQty = 0.0;
        totalBHTIssueQty = 0.0;
        totalIssueValue = 0.0;
        totalBHTIssueValue = 0.0;
        for (Item i : fetchStockItems()) {
            ItemBHTIssueCountTrancerReciveCount count = new ItemBHTIssueCountTrancerReciveCount();
            count.setI(i);
            //System.out.println("i.getName() = " + i.getName());
            List<Object[]> object = fetchItemDetails(i);
            double qty;
            try {
                qty = (double) object.get(0)[0];
                totalIssueQty += qty;
            } catch (Exception e) {
                qty = 0.0;
            }
            double totalValue;
            try {
                totalValue = (double) object.get(0)[1];
                totalIssueValue += totalValue;
            } catch (Exception e) {
                totalValue = 0.0;
            }
            count.setCountIssue(qty);
            count.setTotalIssue(totalValue);
            List<Object[]> objectBHT = fetchBHTIsssue(BillType.PharmacyBhtPre, i);
            double qtyBHT;
            try {
                qtyBHT = (double) objectBHT.get(0)[0];
                totalBHTIssueQty += qtyBHT;
            } catch (Exception e) {
                qtyBHT = 0.0;
            }
            double totalBHTValue;
            try {
                totalBHTValue = (double) objectBHT.get(0)[1];
                totalBHTIssueValue += totalBHTValue;
            } catch (Exception e) {
                totalBHTValue = 0.0;
            }
            count.setCountBht(qtyBHT);
            count.setTotalBht(totalBHTValue);
            itemBHTIssueCountTrancerReciveCounts.add(count);
        }

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        

    }

    public List<Item> fetchStockItems() {

        Map m = new HashMap();
        String sql;
        sql = "select distinct(s.itemBatch.item) from Stock s "
                + " where s.department=:d "
                + " order by s.itemBatch.item.name";
        m.put("d", toDepartment);
        List<Item> items = getItemFacade().findByJpql(sql, m);
        return items;
    }

    public List<Object[]> fetchItemDetails(Item i) {
        Map m = new HashMap();
        String sql;

        sql = "select sum(bi.pharmaceuticalBillItem.qty),sum(bi.pharmaceuticalBillItem.itemBatch.purcahseRate*bi.pharmaceuticalBillItem.qty) "
                + " from BillItem bi "
                + " where bi.bill.fromDepartment=:fdept "
                + " and bi.bill.department=:tdept "
                + " and bi.bill.createdAt between :fd and :td "
                + " and bi.bill.billType=:bt "
                + " and bi.item=:i ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyTransferReceive);
        m.put("fdept", fromDepartment);
        m.put("tdept", toDepartment);
        m.put("i", i);

        return getItemFacade().findObjectsArrayByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public List<Object[]> fetchBHTIsssue(BillType billType, Item i) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", toDepartment);
        m.put("i", i);

        sql = "select sum(b.pharmaceuticalBillItem.qty),sum(b.pharmaceuticalBillItem.qty*b.pharmaceuticalBillItem.itemBatch.purcahseRate) "
                + " from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.bill.createdAt between :fd and :td "
                + " and b.bill.billType=:bt "
                + " and b.item=:i ";

        return getBillFacade().findObjectsArrayByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public List<ItemBHTIssueCountTrancerReciveCount> getItemBHTIssueCountTrancerReciveCounts() {
        return itemBHTIssueCountTrancerReciveCounts;
    }

    public void setItemBHTIssueCountTrancerReciveCounts(List<ItemBHTIssueCountTrancerReciveCount> itemBHTIssueCountTrancerReciveCounts) {
        this.itemBHTIssueCountTrancerReciveCounts = itemBHTIssueCountTrancerReciveCounts;
    }

    public class ItemBHTIssueCountTrancerReciveCount {

        private Item i;
        private double countIssue;
        private double countBht;
        private double totalIssue;
        private double totalBht;

        public Item getI() {
            return i;
        }

        public void setI(Item i) {
            this.i = i;
        }

        public double getCountIssue() {
            return countIssue;
        }

        public void setCountIssue(double countIssue) {
            this.countIssue = countIssue;
        }

        public double getCountBht() {
            return countBht;
        }

        public void setCountBht(double countBht) {
            this.countBht = countBht;
        }

        public double getTotalIssue() {
            return totalIssue;
        }

        public void setTotalIssue(double totalIssue) {
            this.totalIssue = totalIssue;
        }

        public double getTotalBht() {
            return totalBht;
        }

        public void setTotalBht(double totalBht) {
            this.totalBht = totalBht;
        }

    }

    /**
     * Getters & Setters
     *
     * @return
     */
    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(List<Stock> stocks) {
        this.stocks = stocks;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    /**
     * Constructor
     */
    public ReportsTransfer() {
    }

    public double getSaleValue() {
        return saleValue;
    }

    public void setSaleValue(double saleValue) {
        this.saleValue = saleValue;
    }

    public double getStockPurchaseValue() {
        return stockPurchaseValue;
    }

    public void setStockPurchaseValue(double stockPurchaseValue) {
        this.stockPurchaseValue = stockPurchaseValue;
    }

    public double getStockSaleValue() {
        return stockSaleValue;
    }

    public void setStockSaleValue(double stockSaleValue) {
        this.stockSaleValue = stockSaleValue;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public List<BillItem> getTransferItems() {
        return transferItems;
    }

    public void setTransferItems(List<BillItem> transferItems) {
        this.transferItems = transferItems;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public List<StockReportRecord> getMovementRecords() {
        return movementRecords;
    }

    public void setMovementRecords(List<StockReportRecord> movementRecords) {
        this.movementRecords = movementRecords;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public List<StockReportRecord> getMovementRecordsQty() {
        return movementRecordsQty;
    }

    public void setMovementRecordsQty(List<StockReportRecord> movementRecordsQty) {
        this.movementRecordsQty = movementRecordsQty;
    }

    public List<Bill> getTransferBills() {
        return transferBills;
    }

    public void setTransferBills(List<Bill> transferBills) {
        this.transferBills = transferBills;
    }

    public BillFacade getBillFacade() {
        return BillFacade;
    }

    public void setBillFacade(BillFacade BillFacade) {
        this.BillFacade = BillFacade;
    }

    public double getTotalsValue() {
        return totalsValue;
    }

    public void setTotalsValue(double totalsValue) {
        this.totalsValue = totalsValue;
    }

    public double getDiscountsValue() {
        return discountsValue;
    }

    public void setDiscountsValue(double discountsValue) {
        this.discountsValue = discountsValue;
    }

    public double getNetTotalValues() {
        return netTotalValues;
    }

    public void setNetTotalValues(double netTotalValues) {
        this.netTotalValues = netTotalValues;
    }

    public BillType[] getBillTypes() {
        if (billTypes == null) {
            billTypes = new BillType[]{BillType.PharmacySale, BillType.PharmacyIssue, BillType.PharmacyPre, BillType.PharmacyWholesalePre};
        }
        return billTypes;
    }

    public void setBillTypes(BillType[] billTypes) {
        this.billTypes = billTypes;
    }

    public class ItemCount {

        ItemBatch itemBatch;
        double count;
        double gross;
        double margin;
        double discount;
        double net;

        public ItemBatch getItemBatch() {
            return itemBatch;
        }

        public void setItemBatch(ItemBatch itemBatch) {
            this.itemBatch = itemBatch;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

        public double getCount() {
            return count;
        }

        public void setCount(double count) {
            this.count = count;
        }

        public double getGross() {
            return gross;
        }

        public void setGross(double gross) {
            this.gross = gross;
        }

        public double getMargin() {
            return margin;
        }

        public void setMargin(double margin) {
            this.margin = margin;
        }

        public double getNet() {
            return net;
        }

        public void setNet(double net) {
            this.net = net;
        }

    }

    public class ItemCountWithOutMargin {

        Item item;
        double count;
        double net;

        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }

        public double getCount() {
            return count;
        }

        public void setCount(double count) {
            this.count = count;
        }

        public double getNet() {
            return net;
        }

        public void setNet(double net) {
            this.net = net;
        }

    }

    public List<ItemCountWithOutMargin> getItemCountWithOutMargins() {
        return itemCountWithOutMargins;
    }

    public void setItemCountWithOutMargins(List<ItemCountWithOutMargin> itemCountWithOutMargins) {
        this.itemCountWithOutMargins = itemCountWithOutMargins;
    }

    public List<ItemCount> getItemCounts() {
        return itemCounts;
    }

    public void setItemCounts(List<ItemCount> itemCounts) {
        this.itemCounts = itemCounts;
    }

    public double getMarginValue() {
        return marginValue;
    }

    public void setMarginValue(double marginValue) {
        this.marginValue = marginValue;
    }

    public double getBillTotal() {
        return billTotal;
    }

    public void setBillTotal(double billTotal) {
        this.billTotal = billTotal;
    }

    public double getBillMargin() {
        return billMargin;
    }

    public void setBillMargin(double billMargin) {
        this.billMargin = billMargin;
    }

    public double getBillDiscount() {
        return billDiscount;
    }

    public void setBillDiscount(double billDiscount) {
        this.billDiscount = billDiscount;
    }

    public double getBillNetTotal() {
        return billNetTotal;
    }

    public void setBillNetTotal(double billNetTotal) {
        this.billNetTotal = billNetTotal;
    }

    public double getRetailValue() {
        return retailValue;
    }

    public void setRetailValue(double retailValue) {
        this.retailValue = retailValue;
    }

    public double getValueOfQOH() {
        return valueOfQOH;
    }

    public void setValueOfQOH(double valueOfQOH) {
        this.valueOfQOH = valueOfQOH;
    }

    public double getQoh() {
        return qoh;
    }

    public void setQoh(double qoh) {
        this.qoh = qoh;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public double getTotalIssueQty() {
        return totalIssueQty;
    }

    public void setTotalIssueQty(double totalIssueQty) {
        this.totalIssueQty = totalIssueQty;
    }

    public double getTotalBHTIssueQty() {
        return totalBHTIssueQty;
    }

    public void setTotalBHTIssueQty(double totalBHTIssueQty) {
        this.totalBHTIssueQty = totalBHTIssueQty;
    }

    public double getTotalIssueValue() {
        return totalIssueValue;
    }

    public void setTotalIssueValue(double totalIssueValue) {
        this.totalIssueValue = totalIssueValue;
    }

    public double getTotalBHTIssueValue() {
        return totalBHTIssueValue;
    }

    public void setTotalBHTIssueValue(double totalBHTIssueValue) {
        this.totalBHTIssueValue = totalBHTIssueValue;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

}
