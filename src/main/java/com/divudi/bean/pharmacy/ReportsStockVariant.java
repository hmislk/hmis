/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.PreBill;
import com.divudi.entity.pharmacy.StockVarientBillItem;
import com.divudi.facade.BillFacade;
import com.divudi.facade.StockFacade;
import com.divudi.facade.StockVarientBillItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class ReportsStockVariant implements Serializable {

    /**
     * Bean Variables
     */
    Department department;
    private Category category;
    double systemStockValue;
    private double systemStockValueAfter;
    List<StockVarientBillItem> records;
    private List<StockVarientBillItem> recordsAfter;
    private Bill recordedBill;
    private Bill recordedBillAfter;

    /**
     * Managed Beans
     */
    @Inject
    DealerController dealerController;
    @Inject
    CommonController commonController;

    /**
     * EJBs
     */
    @EJB
    StockFacade stockFacade;

    /**
     * Methods
     */
    /**
     * Methods
     *
     * @return
     */
    public List<Object[]> calDepartmentStock() {

        String sql;
        Map m = new HashMap();
        m.put("dep", department);
        m.put("cat", category);
        sql = "select i.itemBatch.item,sum(i.stock),avg(i.itemBatch.purcahseRate) "
                + " from Stock i where "
                + " i.department=:dep and i.itemBatch.item.category=:cat group by i.itemBatch.item order by i.itemBatch.item.name";

        return getStockFacade().findAggregates(sql, m);

    }

    public double calStockByItem(Item item) {

        String sql;
        Map m = new HashMap();
        m.put("dep", department);
        m.put("itm", item);
        sql = "select sum(i.stock) "
                + " from Stock i where "
                + " i.department=:dep and i.itemBatch.item=:itm ";

        return getStockFacade().findDoubleByJpql(sql, m);

    }

    public double calPurchaseByItem(Item item) {

        String sql;
        Map m = new HashMap();
        m.put("dep", department);
        m.put("itm", item);
        sql = "select avg(i.itemBatch.purcahseRate) "
                + " from Stock i where "
                + " i.department=:dep and i.itemBatch.item=:itm ";

        return getStockFacade().findDoubleByJpql(sql, m);

    }

    @Inject
    private PharmacyErrorChecking pharmacyErrorChecking;

    public void fillCategoryStocks() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (department == null || category == null) {
            JsfUtil.addErrorMessage("Please select a department && Category");
            return;
        }

        records = new ArrayList<>();
        systemStockValue = 0.0;

        for (Object[] obj : calDepartmentStock()) {
            StockVarientBillItem r = new StockVarientBillItem();
            r.setItem((Item) obj[0]);
            r.setSystemStock((Double) obj[1]);
            r.setPurchaseRate((Double) obj[2]);
//            /////////
//            getPharmacyErrorChecking().setItem(r.getItem());
//            getPharmacyErrorChecking().setDepartment(department);
//            getPharmacyErrorChecking().calculateTotals3();
            //  r.setCalCulatedStock(getPharmacyErrorChecking().getCalculatedStock());
            //////////////
            records.add(r);

            systemStockValue += (r.getSystemStock() * r.getPurchaseRate());
        }
        
        

    }

    public void fillCategoryStocksAfter() {
        if (department == null || category == null) {
            return;
        }

        recordsAfter = new ArrayList<>();
        systemStockValue = 0.0;
        systemStockValueAfter = 0;

        for (StockVarientBillItem stockVarientBillItem : records) {
            StockVarientBillItem r = new StockVarientBillItem();
            r.setReferenceStockVariantBillItem(stockVarientBillItem);
            r.setItem(stockVarientBillItem.getItem());
            r.setSystemStock(calStockByItem(r.getItem()));
            r.setPurchaseRate(calPurchaseByItem(r.getItem()));

            recordsAfter.add(r);

            systemStockValue += (r.getReferenceStockVariantBillItem().getSystemStock() * r.getReferenceStockVariantBillItem().getPurchaseRate());
            systemStockValueAfter += (r.getSystemStock() * r.getPurchaseRate());
        }

    }

    @Inject
    private SessionController sessionController;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private StockVarientBillItemFacade stockVarientBillItemFacade;

    public String saveRecord() {

        getRecordedBill().setCreatedAt(new Date());
        getRecordedBill().setDepartment(department);
        getRecordedBill().setTotal(systemStockValue);
        getRecordedBill().setCategory(getCategory());
        getRecordedBill().setCreater(getSessionController().getLoggedUser());
        getRecordedBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(department, BillType.PharmacyMajorAdjustment, BillClassType.BilledBill, BillNumberSuffix.MJADJ));
        getRecordedBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(department.getInstitution(), BillType.PharmacyMajorAdjustment, BillClassType.BilledBill, BillNumberSuffix.MJADJ));

        if (getRecordedBill().getId() == null) {
            getBillFacade().create(getRecordedBill());
        }

        for (StockVarientBillItem i : records) {
            i.setBill(getRecordedBill());

            if (i.getId() == null) {
                getStockVarientBillItemFacade().create(i);
            }
            getRecordedBill().getStockVarientBillItems().add(i);
        }

        getBillFacade().edit(getRecordedBill());

        JsfUtil.addSuccessMessage("Succesfully Saved");

        return "";
    }

    public String saveRecordAfter() {

        getRecordedBillAfter().setCreatedAt(new Date());
        getRecordedBillAfter().setDepartment(department);
        getRecordedBillAfter().setCategory(getCategory());
        getRecordedBillAfter().setTotal(systemStockValue);
        getRecordedBillAfter().setNetTotal(systemStockValueAfter);
        getRecordedBillAfter().setCreater(getSessionController().getLoggedUser());
        getRecordedBillAfter().setDeptId(getBillNumberBean().institutionBillNumberGenerator(department, BillType.PharmacyMajorAdjustment, BillClassType.BilledBill, BillNumberSuffix.MJADJ));
        getRecordedBillAfter().setInsId(getBillNumberBean().institutionBillNumberGenerator(department.getInstitution(), BillType.PharmacyMajorAdjustment, BillClassType.BilledBill, BillNumberSuffix.MJADJ));
        getRecordedBillAfter().setBackwardReferenceBill(getRecordedBill());

        if (getRecordedBillAfter().getId() == null) {
            getBillFacade().create(getRecordedBillAfter());
        }

        getRecordedBill().getForwardReferenceBills().add(getRecordedBillAfter());
        getBillFacade().edit(getRecordedBill());

        for (StockVarientBillItem i : recordsAfter) {
            i.setBill(getRecordedBillAfter());
            if (i.getId() == null) {
                getStockVarientBillItemFacade().create(i);
            }
            getRecordedBillAfter().getStockVarientBillItems().add(i);

        }

        getBillFacade().edit(getRecordedBillAfter());

        JsfUtil.addSuccessMessage("Succesfully Saved");

        return "";
    }

    public void recreateModel() {
        department = null;
        category = null;
        systemStockValue = 0;
        records = null;
        recordedBill = null;
    }

    /**
     * Getters & Setters
     *
     * @return
     */
    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
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
    public ReportsStockVariant() {
    }

    public double getSystemStockValue() {
        return systemStockValue;
    }

    public void setSystemStockValue(double systemStockValue) {
        this.systemStockValue = systemStockValue;
    }

    public List<StockVarientBillItem> getRecords() {
        return records;
    }

    public void setRecords(List<StockVarientBillItem> records) {
        this.records = records;
    }

    public DealerController getDealerController() {
        return dealerController;
    }

    public void setDealerController(DealerController dealerController) {
        this.dealerController = dealerController;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public PharmacyErrorChecking getPharmacyErrorChecking() {
        return pharmacyErrorChecking;
    }

    public void setPharmacyErrorChecking(PharmacyErrorChecking pharmacyErrorChecking) {
        this.pharmacyErrorChecking = pharmacyErrorChecking;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public StockVarientBillItemFacade getStockVarientBillItemFacade() {
        return stockVarientBillItemFacade;
    }

    public void setStockVarientBillItemFacade(StockVarientBillItemFacade stockVarientBillItemFacade) {
        this.stockVarientBillItemFacade = stockVarientBillItemFacade;
    }

    public Bill getRecordedBill() {
        if (recordedBill == null) {
            recordedBill = new PreBill();
            recordedBill.setBillType(BillType.PharmacyMajorAdjustment);
        }
        return recordedBill;
    }

    public void setRecordedBill(Bill recordedBill) {
        this.recordedBill = recordedBill;
    }

    public List<StockVarientBillItem> getRecordsAfter() {
        return recordsAfter;
    }

    public void setRecordsAfter(List<StockVarientBillItem> recordsAfter) {
        this.recordsAfter = recordsAfter;
    }

    public double getSystemStockValueAfter() {
        return systemStockValueAfter;
    }

    public void setSystemStockValueAfter(double systemStockValueAfter) {
        this.systemStockValueAfter = systemStockValueAfter;
    }

    public Bill getRecordedBillAfter() {
        if (recordedBillAfter == null) {
            recordedBillAfter = new BilledBill();
            recordedBillAfter.setBillType(BillType.PharmacyMajorAdjustment);
        }
        return recordedBillAfter;
    }

    public void setRecordedBillAfter(Bill recordedBillAfter) {
        this.recordedBillAfter = recordedBillAfter;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }
    
    

}
