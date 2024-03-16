/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;

import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.PreBill;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.ItemBatch;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.AdjustmentBillItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemBatchFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.StockFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PharmacyAdjustmentController implements Serializable {

    /**
     * Creates a new instance of PharmacySaleController
     */
    public PharmacyAdjustmentController() {
    }

    @Inject
    SessionController sessionController;
    @Inject
    CommonController commonController;
////////////////////////
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    StockFacade stockFacade;
    @EJB
    PharmacyBean pharmacyBean;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    ItemBatchFacade itemBatchFacade;
    

/////////////////////////
//    Item selectedAlternative;
    private Bill deptAdjustmentPreBill;
    private Bill saleBill;
    Bill bill;
    BillItem billItem;
    BillItem removingBillItem;
    BillItem editingBillItem;

    Stock stock;
    Item item;
    double total;
    boolean manualAdjust;

    String comment;

    Department fromDepartment;
    Department toDepartment;

    private Double qty;
    private Double pr;
    private Double rsr;
    private Double wsr;
    Date exDate;

    private Date fromDate;
    private Date toDate;

    private YearMonthDay yearMonthDay;

    List<BillItem> billItems;
    List<Stock> stocks;
    List<Bill> bills;

    private Amp amp;
    private List<Stock> ampStock;

    private boolean printPreview;

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void fillDepartmentAdjustmentByBillItem() {
        Date startTime = new Date();
        billItems = fetchBillItems(BillType.PharmacyAdjustment);
    }

    public void fillAmpStocks() {
        List<Stock> items = new ArrayList<>();
        if (amp == null) {
            ampStock = items;
            return;
        }
        String sql;
        Map m = new HashMap();
        sql = "select i "
                + " from Stock i "
                + " where i.department=:d "
                + " and i.itemBatch.item=:amp "
                + " order by i.stock desc";
        m.put("d", sessionController.getDepartment());
        m.put("amp", amp);

        items = getStockFacade().findByJpql(sql, m);

        if (items != null) {
            ampStock = items;
        }
    }

    public List<BillItem> fetchBillItems(BillType bt) {
        List<BillItem> billItems = new ArrayList<>();

        Map m = new HashMap();
        String sql;
        sql = "select bi from BillItem bi where "
                + " bi.bill.createdAt between :fd and :td "
                + " and bi.bill.billType=:bt ";

        if (bt == BillType.PharmacyAdjustment) {
            if (fromDepartment != null) {
                sql += " and bi.bill.department=:fdept ";
                m.put("fdept", fromDepartment);
            }
        }

        sql += " order by bi.id";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", bt);

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        return billItems;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public void makeNull() {
        printPreview = false;
        ampStock = new ArrayList<>();
        amp = null;
        clearBill();
        clearBillItem();
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public String newSaleBill() {
        clearBill();
        clearBillItem();
        return "";
    }

    public List<Item> completeRetailSaleItems(String qry) {
        Map m = new HashMap<>();
        List<Item> items;
        String sql;
        sql = "select i from Item i where i.retired=false and (i.name) like :n and type(i)=:t and i.id not in(select ibs.id from Stock ibs where ibs.stock >:s and ibs.department=:d and (ibs.itemBatch.item.name) like :n ) order by i.name ";
        m.put("t", Amp.class);
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("n", "%" + qry + "%");
        double s = 0.0;
        m.put("s", s);
        items = getItemFacade().findByJpql(sql, m, 10);
        return items;
    }

    public List<Stock> completeAvailableStocks(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("s", d);
        m.put("n", "%" + qry.toUpperCase() + "%");
        sql = "select i from Stock i where i.stock >=:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n ) order by i.itemBatch.item.name, i.itemBatch.dateOfExpire , i.stock desc";
        items = getStockFacade().findByJpql(sql, m, 20);
        return items;
    }

    public List<Stock> completeAllStocks(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("n", "%" + qry.toUpperCase() + "%");
        sql = "select i from Stock i where i.department=:d and "
                + " ((i.itemBatch.item.name) like :n  or "
                + " (i.itemBatch.item.code) like :n  or  "
                + " (i.itemBatch.item.barcode) like :n ) "
                + " order by i.stock desc";
        items = getStockFacade().findByJpql(sql, m, 20);

        return items;
    }

    public List<Stock> completeStaffStocks(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        double d = 0.0;
        m.put("s", d);
        m.put("n", "%" + qry.toUpperCase() + "%");
        sql = "select i from Stock i where i.stock !=:s and "
                + "((i.staff.code) like :n or "
                + "(i.staff.person.name) like :n or "
                + "(i.itemBatch.item.name) like :n ) "
                + "order by i.itemBatch.item.name, i.itemBatch.dateOfExpire , i.stock desc";
        items = getStockFacade().findByJpql(sql, m, 20);

        return items;
    }

    public List<Stock> completeStaffZeroStocks(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        double d = 0.0;
        m.put("s", d);
        m.put("n", "%" + qry.toUpperCase() + "%");
        sql = "select i from Stock i where i.stock =:s and "
                + "((i.staff.code) like :n or "
                + "(i.staff.person.name) like :n or "
                + "(i.itemBatch.item.name) like :n ) "
                + "order by i.itemBatch.item.name, i.itemBatch.dateOfExpire , i.stock desc";
        items = getStockFacade().findByJpql(sql, m, 20);

        return items;
    }

    public BillItem getBillItem() {
        if (billItem == null) {
            billItem = new BillItem();
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            
            pbi.setBillItem(billItem);
            billItem.setPharmaceuticalBillItem(pbi);
        }

        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public List<BillItem> getBillItems() {
        if (billItems == null) {
            billItems = new ArrayList<>();
        }
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    private void saveDeptAdjustmentBill() {
        getDeptAdjustmentPreBill().setBillDate(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setBillTime(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreater(getSessionController().getLoggedUser());
        getDeptAdjustmentPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setBillType(BillType.PharmacyAdjustment);
        getDeptAdjustmentPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setToDepartment(null);
        getDeptAdjustmentPreBill().setToInstitution(null);
        getDeptAdjustmentPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setComments(comment);
        if (getDeptAdjustmentPreBill().getId() == null) {
            getBillFacade().create(getDeptAdjustmentPreBill());
        } else {
            getBillFacade().edit(getDeptAdjustmentPreBill());
        }
    }

    private void saveDeptStockAdjustmentBill() {
        getDeptAdjustmentPreBill().setBillDate(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setBillTime(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreater(getSessionController().getLoggedUser());
        getDeptAdjustmentPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setBillType(BillType.PharmacyAdjustmentDepartmentStock);
        getDeptAdjustmentPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setToDepartment(null);
        getDeptAdjustmentPreBill().setToInstitution(null);
        getDeptAdjustmentPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setComments(comment);
        if (getDeptAdjustmentPreBill().getId() == null) {
            getBillFacade().create(getDeptAdjustmentPreBill());
        } else {
            getBillFacade().edit(getDeptAdjustmentPreBill());
        }
    }

    private void saveDeptSingleStockAdjustmentBill() {
        getDeptAdjustmentPreBill().setBillDate(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setBillTime(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreater(getSessionController().getLoggedUser());
        getDeptAdjustmentPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setBillType(BillType.PharmacyAdjustmentDepartmentSingleStock);
        getDeptAdjustmentPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setToDepartment(null);
        getDeptAdjustmentPreBill().setToInstitution(null);
        getDeptAdjustmentPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setComments(comment);
        if (getDeptAdjustmentPreBill().getId() == null) {
            getBillFacade().create(getDeptAdjustmentPreBill());
        } else {
            getBillFacade().edit(getDeptAdjustmentPreBill());
        }
    }

    private void saveStaffStockAdjustmentBill() {
        getDeptAdjustmentPreBill().setBillDate(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setBillTime(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreater(getSessionController().getLoggedUser());
        getDeptAdjustmentPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setBillType(BillType.PharmacyAdjustmentStaffStock);
        getDeptAdjustmentPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setToDepartment(null);
        getDeptAdjustmentPreBill().setToInstitution(null);
        getDeptAdjustmentPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setComments(comment);
        if (getDeptAdjustmentPreBill().getId() == null) {
            getBillFacade().create(getDeptAdjustmentPreBill());
        } else {
            getBillFacade().edit(getDeptAdjustmentPreBill());
        }
    }

    private void savePurchaseRateAdjustmentBill() {
        getDeptAdjustmentPreBill().setBillDate(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setBillTime(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreater(getSessionController().getLoggedUser());
        getDeptAdjustmentPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setBillType(BillType.PharmacyAdjustmentPurchaseRate);
        getDeptAdjustmentPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setToDepartment(null);
        getDeptAdjustmentPreBill().setToInstitution(null);
        getDeptAdjustmentPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setComments(comment);
        if (getDeptAdjustmentPreBill().getId() == null) {
            getBillFacade().create(getDeptAdjustmentPreBill());
        } else {
            getBillFacade().edit(getDeptAdjustmentPreBill());
        }
    }

    private void saveSaleRateAdjustmentBill() {
        getDeptAdjustmentPreBill().setBillDate(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setBillTime(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreater(getSessionController().getLoggedUser());
        getDeptAdjustmentPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setBillType(BillType.PharmacyAdjustmentSaleRate);
        getDeptAdjustmentPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setToDepartment(null);
        getDeptAdjustmentPreBill().setToInstitution(null);
        getDeptAdjustmentPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setComments(comment);
        if (getDeptAdjustmentPreBill().getId() == null) {
            //System.out.println("savesakeAjes null = " + getDeptAdjustmentPreBill().getId());
            getBillFacade().create(getDeptAdjustmentPreBill());
        } else {
            //System.out.println("savesakeAjes getId() = " + getDeptAdjustmentPreBill().getId());
            getBillFacade().edit(getDeptAdjustmentPreBill());
        }
    }

    private void saveWholeSaleRateAdjustmentBill() {
        getDeptAdjustmentPreBill().setBillDate(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setBillTime(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreater(getSessionController().getLoggedUser());
        getDeptAdjustmentPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setBillType(BillType.PharmacyAdjustmentWholeSaleRate);
        getDeptAdjustmentPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setToDepartment(null);
        getDeptAdjustmentPreBill().setToInstitution(null);
        getDeptAdjustmentPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setComments(comment);
        if (getDeptAdjustmentPreBill().getId() == null) {
            getBillFacade().create(getDeptAdjustmentPreBill());
        } else {
            getBillFacade().edit(getDeptAdjustmentPreBill());
        }
    }

    private void saveExpiryDateAdjustmentBill() {
        getDeptAdjustmentPreBill().setBillDate(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setBillTime(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreater(getSessionController().getLoggedUser());
        getDeptAdjustmentPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setBillType(BillType.PharmacyAdjustmentExpiryDate);
        getDeptAdjustmentPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setToDepartment(null);
        getDeptAdjustmentPreBill().setToInstitution(null);
        getDeptAdjustmentPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setComments(comment);
        if (getDeptAdjustmentPreBill().getId() == null) {
            getBillFacade().create(getDeptAdjustmentPreBill());
        } else {
            getBillFacade().edit(getDeptAdjustmentPreBill());
        }
    }

    private PharmaceuticalBillItem saveDeptAdjustmentBillItems() {
        billItem = null;
        BillItem tbi = getBillItem();

        PharmaceuticalBillItem ph = getBillItem().getPharmaceuticalBillItem();

        tbi.setPharmaceuticalBillItem(null);
        ph.setStock(stock);

        tbi.setItem(getStock().getItemBatch().getItem());
        tbi.setQty((double) qty);

        //pharmaceutical Bill Item
        ph.setDoe(getStock().getItemBatch().getDateOfExpire());
        ph.setFreeQty(0.0f);
        ph.setItemBatch(getStock().getItemBatch());

        Stock fetchedStock = getStockFacade().find(stock.getId());
        double stockQty = fetchedStock.getStock();
        double changingQty;

        changingQty = qty - stockQty;

        ph.setQty(changingQty);

        //Rates
        //Values
        tbi.setGrossValue(getStock().getItemBatch().getRetailsaleRate() * qty);
        tbi.setNetValue(qty * tbi.getNetRate());
        tbi.setDiscount(tbi.getGrossValue() - tbi.getNetValue());
        tbi.setInwardChargeType(InwardChargeType.Medicine);
        tbi.setItem(getStock().getItemBatch().getItem());
        tbi.setBill(getDeptAdjustmentPreBill());
        tbi.setSearialNo(getDeptAdjustmentPreBill().getBillItems().size() + 1);
        tbi.setCreatedAt(Calendar.getInstance().getTime());
        tbi.setCreater(getSessionController().getLoggedUser());

        ph.setBillItem(null);

        if (ph.getId() == null) {
            getPharmaceuticalBillItemFacade().create(ph);
        }

        tbi.setPharmaceuticalBillItem(ph);

        if (tbi.getId() == null) {
            getBillItemFacade().edit(tbi);
        }

        ph.setBillItem(tbi);
        getPharmaceuticalBillItemFacade().edit(ph);

        getDeptAdjustmentPreBill().getBillItems().add(tbi);

        getBillFacade().edit(getDeptAdjustmentPreBill());

        return ph;

    }

    private PharmaceuticalBillItem saveDeptAdjustmentBillItems(Stock s) {
        billItem = null;
        BillItem tbi = getBillItem();

        PharmaceuticalBillItem ph = getBillItem().getPharmaceuticalBillItem();

        tbi.setPharmaceuticalBillItem(null);
        ph.setStock(s);

        tbi.setItem(s.getItemBatch().getItem());
        tbi.setQty(s.getCalculated());

        //pharmaceutical Bill Item
        ph.setDoe(s.getItemBatch().getDateOfExpire());
        ph.setFreeQty(0.0f);
        ph.setItemBatch(s.getItemBatch());

        Stock fetchedStock = getStockFacade().find(s.getId());
        double stockQty = fetchedStock.getStock();
        double changingQty;

        changingQty = s.getCalculated() - stockQty;

        ph.setQty(changingQty);

        //Rates
        //Values
        tbi.setGrossValue(s.getItemBatch().getRetailsaleRate() * s.getCalculated());
        tbi.setNetValue(s.getCalculated() * tbi.getNetRate());
        tbi.setDiscount(tbi.getGrossValue() - tbi.getNetValue());
        tbi.setInwardChargeType(InwardChargeType.Medicine);
        tbi.setItem(s.getItemBatch().getItem());
        tbi.setBill(getDeptAdjustmentPreBill());
        tbi.setSearialNo(getDeptAdjustmentPreBill().getBillItems().size() + 1);
        tbi.setCreatedAt(Calendar.getInstance().getTime());
        tbi.setCreater(getSessionController().getLoggedUser());

        ph.setBillItem(null);

        if (ph.getId() == null) {
            getPharmaceuticalBillItemFacade().create(ph);
        }

        tbi.setPharmaceuticalBillItem(ph);

        if (tbi.getId() == null) {
            getBillItemFacade().edit(tbi);
        }

        ph.setBillItem(tbi);
        getPharmaceuticalBillItemFacade().edit(ph);

        getDeptAdjustmentPreBill().getBillItems().add(tbi);

        getBillFacade().edit(getDeptAdjustmentPreBill());

        return ph;

    }

    private void savePrAdjustmentBillItems() {
        billItem = null;
        BillItem tbi = getBillItem();
        PharmaceuticalBillItem ph = getBillItem().getPharmaceuticalBillItem();

        ph.setBillItem(null);
        ItemBatch ib = itemBatchFacade.find(getStock().getItemBatch().getId());
        ph.setPurchaseRate(ib.getPurcahseRate());
        ph.setRetailRate(ib.getRetailsaleRate());
        tbi.setItem(getStock().getItemBatch().getItem());
        tbi.setRate(pr);
        //pharmaceutical Bill Item
        ph.setStock(stock);
        //Rates
        //Values
        tbi.setGrossValue(getStock().getItemBatch().getRetailsaleRate() * getStock().getStock());
        tbi.setNetValue(getStock().getStock() * tbi.getNetRate());
        tbi.setDiscount(tbi.getGrossValue() - tbi.getNetValue());
        tbi.setInwardChargeType(InwardChargeType.Medicine);
        tbi.setItem(getStock().getItemBatch().getItem());
        tbi.setBill(getDeptAdjustmentPreBill());
        tbi.setSearialNo(getDeptAdjustmentPreBill().getBillItems().size() + 1);
        tbi.setCreatedAt(Calendar.getInstance().getTime());
        tbi.setCreater(getSessionController().getLoggedUser());
        if (ph.getId() == null) {
            getPharmaceuticalBillItemFacade().create(ph);
        }
        tbi.setPharmaceuticalBillItem(ph);

        if (tbi.getId() == null) {
            getBillItemFacade().edit(tbi);
        }

        ph.setBillItem(tbi);
        getPharmaceuticalBillItemFacade().edit(ph);
        getPharmaceuticalBillItemFacade().edit(tbi.getPharmaceuticalBillItem());
        getDeptAdjustmentPreBill().getBillItems().add(tbi);
        getBillFacade().edit(getDeptAdjustmentPreBill());
    }

    private void saveRsrAdjustmentBillItems() {
        if (stock == null) {
            JsfUtil.addErrorMessage("Please select a stock");
            return;
        }
        billItem = null;
        BillItem tbi = getBillItem();
        PharmaceuticalBillItem ph = getBillItem().getPharmaceuticalBillItem();
        ItemBatch itemBatch = itemBatchFacade.find(getStock().getItemBatch().getId());
        ph.setBillItem(null);
        ph.setPurchaseRate(itemBatch.getPurcahseRate());
        ph.setRetailRate(itemBatch.getRetailsaleRate());
        tbi.setItem(getStock().getItemBatch().getItem());
        tbi.setRate(rsr);
        //pharmaceutical Bill Item
        ph.setStock(stock);
        //Rates
        //Values
        tbi.setGrossValue(getStock().getItemBatch().getRetailsaleRate() * getStock().getStock());
        tbi.setNetValue(getStock().getStock() * tbi.getNetRate());
        tbi.setDiscount(tbi.getGrossValue() - tbi.getNetValue());
        tbi.setInwardChargeType(InwardChargeType.Medicine);
        tbi.setItem(getStock().getItemBatch().getItem());
        tbi.setBill(getDeptAdjustmentPreBill());
        tbi.setSearialNo(getDeptAdjustmentPreBill().getBillItems().size() + 1);
        tbi.setCreatedAt(Calendar.getInstance().getTime());
        tbi.setCreater(getSessionController().getLoggedUser());

        if (ph.getId() == null) {
            getPharmaceuticalBillItemFacade().create(ph);
        }
        tbi.setPharmaceuticalBillItem(ph);

        if (tbi.getId() == null) {
            getBillItemFacade().edit(tbi);
        }

        ph.setBillItem(tbi);
        getPharmaceuticalBillItemFacade().edit(ph);
//        getPharmaceuticalBillItemFacade().edit(tbi.getPharmaceuticalBillItem());
        getDeptAdjustmentPreBill().getBillItems().add(tbi);
        getBillFacade().edit(getDeptAdjustmentPreBill());
    }

    private void saveWsrAdjustmentBillItems() {
        if (stock == null) {
            JsfUtil.addErrorMessage("Please select a stock");
            return;
        }
        billItem = null;
        BillItem tbi = getBillItem();
        PharmaceuticalBillItem ph = getBillItem().getPharmaceuticalBillItem();
        ItemBatch itemBatch = itemBatchFacade.find(getStock().getItemBatch().getId());
        ph.setBillItem(null);
        ph.setPurchaseRate(itemBatch.getPurcahseRate());
        ph.setRetailRate(itemBatch.getRetailsaleRate());
        ph.setWholesaleRate(itemBatch.getWholesaleRate());
        tbi.setItem(getStock().getItemBatch().getItem());
        tbi.setRate(wsr);
        //pharmaceutical Bill Item
        ph.setStock(stock);
        //Rates
        //Values
        tbi.setGrossValue(getStock().getItemBatch().getWholesaleRate() * getStock().getStock());
        tbi.setNetValue(getStock().getStock() * tbi.getNetRate());
        tbi.setDiscount(tbi.getGrossValue() - tbi.getNetValue());
        tbi.setInwardChargeType(InwardChargeType.Medicine);
        tbi.setItem(getStock().getItemBatch().getItem());
        tbi.setBill(getDeptAdjustmentPreBill());
        tbi.setSearialNo(getDeptAdjustmentPreBill().getBillItems().size() + 1);
        tbi.setCreatedAt(Calendar.getInstance().getTime());
        tbi.setCreater(getSessionController().getLoggedUser());

        if (ph.getId() == null) {
            getPharmaceuticalBillItemFacade().create(ph);
        }
        tbi.setPharmaceuticalBillItem(ph);

        if (tbi.getId() == null) {
            getBillItemFacade().edit(tbi);
        }

        ph.setBillItem(tbi);
        getPharmaceuticalBillItemFacade().edit(ph);
//        getPharmaceuticalBillItemFacade().edit(tbi.getPharmaceuticalBillItem());
        getDeptAdjustmentPreBill().getBillItems().add(tbi);
        getBillFacade().edit(getDeptAdjustmentPreBill());
    }

    private void saveExDateAdjustmentBillItems() {
        billItem = null;
        BillItem tbi = getBillItem();
        PharmaceuticalBillItem ph = getBillItem().getPharmaceuticalBillItem();
        ItemBatch itemBatch = itemBatchFacade.find(getStock().getItemBatch().getId());
        ph.setBillItem(null);
//        ph.setPurchaseRate(itemBatch.getPurcahseRate());
//        ph.setRetailRate(itemBatch.getRetailsaleRate());
        ph.setDoe(itemBatch.getDateOfExpire());
        //tbi.setItem(getStock().getItemBatch().getItem());
        //itemBatch.setDateOfExpire(exDate);
        //tbi.setRate(rsr);
        //pharmaceutical Bill Item
        ph.setStock(stock);
        //Rates
        //Values
        tbi.setGrossValue(getStock().getItemBatch().getRetailsaleRate() * getStock().getStock());
        tbi.setNetValue(getStock().getStock() * tbi.getNetRate());
        tbi.setDiscount(tbi.getGrossValue() - tbi.getNetValue());
        tbi.setInwardChargeType(InwardChargeType.Medicine);
        tbi.setItem(getStock().getItemBatch().getItem());
        tbi.setBill(getDeptAdjustmentPreBill());
        tbi.setSearialNo(getDeptAdjustmentPreBill().getBillItems().size() + 1);
        tbi.setCreatedAt(Calendar.getInstance().getTime());
        tbi.setCreater(getSessionController().getLoggedUser());

        if (ph.getId() == null) {
            getPharmaceuticalBillItemFacade().create(ph);
        }
        tbi.setPharmaceuticalBillItem(ph);

        if (tbi.getId() == null) {
            getBillItemFacade().edit(tbi);
        }

        ph.setBillItem(tbi);
        getPharmaceuticalBillItemFacade().edit(ph);
        getItemBatchFacade().edit(itemBatch);
//        getPharmaceuticalBillItemFacade().edit(tbi.getPharmaceuticalBillItem());
        getDeptAdjustmentPreBill().getBillItems().add(tbi);
        getBillFacade().edit(getDeptAdjustmentPreBill());
    }

    private boolean errorCheck() {
        if (getStock() == null) {
            JsfUtil.addErrorMessage("Please Select Stocke");
            return true;
        }

        if (getStock().getItemBatch() == null) {
            JsfUtil.addErrorMessage("Select Item Batch");
            return true;
        }
        return false;
    }

    private boolean errorCheckAll() {

        if (getItem() == null) {
            JsfUtil.addErrorMessage("Select Item");
            return true;
        }
        if (getStocks().isEmpty()) {
            JsfUtil.addErrorMessage("No Stocks");
            return true;
        }
        if (qty == null || qty == 0.0) {
            JsfUtil.addErrorMessage("Please Select Correct Stock");
            return true;
        }

        return false;
    }

    private boolean errorCheckAllZero() {

        if (getItem() == null) {
            JsfUtil.addErrorMessage("Select Item");
            return true;
        }
        if (getStocks().isEmpty()) {
            JsfUtil.addErrorMessage("No Stocks");
            return true;
        }
        if (qty == null) {
            JsfUtil.addErrorMessage("Please Select Correct Stock");
            return true;
        }
        if (qty != 0.0) {
            JsfUtil.addErrorMessage("Please Select Correct Stock Qty");
            return true;
        }

        return false;
    }

    public void transferAllDepartmentStockAsAdjustment() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (fromDepartment == null) {
            JsfUtil.addErrorMessage("From ?");
            return;
        }
        if (toDepartment == null) {
            JsfUtil.addErrorMessage("To ?");
            return;
        }
        if (fromDepartment.equals(toDepartment)) {
            JsfUtil.addErrorMessage("From and To are same");
            return;
        }

        Bill fromBill = new PreBill();
        fromBill.setBillType(BillType.PharmacyAdjustment);
        fromBill.setBillDate(Calendar.getInstance().getTime());
        fromBill.setBillTime(Calendar.getInstance().getTime());
        fromBill.setCreatedAt(Calendar.getInstance().getTime());
        fromBill.setCreater(getSessionController().getLoggedUser());
        fromBill.setDeptId(getBillNumberBean().institutionBillNumberGenerator(fromDepartment, BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        fromBill.setInsId(getBillNumberBean().institutionBillNumberGenerator(fromDepartment.getInstitution(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        fromBill.setBillType(BillType.PharmacyAdjustment);
        fromBill.setDepartment(fromDepartment);
        fromBill.setInstitution(fromDepartment.getInstitution());
        fromBill.setToDepartment(toDepartment);
        fromBill.setToInstitution(toDepartment.getInstitution());
        fromBill.setFromDepartment(fromDepartment);
        fromBill.setFromInstitution(fromDepartment.getInstitution());
        fromBill.setComments(comment);
        getBillFacade().create(fromBill);

        Bill toBill = new PreBill();
        toBill.setBillType(BillType.PharmacyAdjustment);
        toBill.setBillDate(Calendar.getInstance().getTime());
        toBill.setBillTime(Calendar.getInstance().getTime());
        toBill.setCreatedAt(Calendar.getInstance().getTime());
        toBill.setCreater(getSessionController().getLoggedUser());
        toBill.setDeptId(getBillNumberBean().institutionBillNumberGenerator(toDepartment, BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        toBill.setInsId(getBillNumberBean().institutionBillNumberGenerator(toDepartment.getInstitution(), BillType.PharmacyAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        toBill.setBillType(BillType.PharmacyAdjustment);
        toBill.setDepartment(toDepartment);
        toBill.setInstitution(toDepartment.getInstitution());
        toBill.setToDepartment(toDepartment);
        toBill.setToInstitution(toDepartment.getInstitution());
        toBill.setFromDepartment(fromDepartment);
        toBill.setFromInstitution(fromDepartment.getInstitution());
        toBill.setComments(comment);
        getBillFacade().create(toBill);

        String sql;
        Map m = new HashMap();
        m.put("dept", fromDepartment);
        sql = "select s from Stock s where s.department=:dept";
        List<Stock> stocks = getStockFacade().findByJpql(sql, m);
        int i = 0;
        for (Stock s : stocks) {
            BillItem fromBi = new BillItem();
            PharmaceuticalBillItem fromPbi = new PharmaceuticalBillItem();
            fromBi.setPharmaceuticalBillItem(null);
            fromPbi.setStock(s);
            fromBi.setItem(s.getItemBatch().getItem());
            fromBi.setQty(0 - ((double) s.getStock()));
            //pharmaceutical Bill Item
            fromPbi.setDoe(s.getItemBatch().getDateOfExpire());
            fromPbi.setFreeQty(0.0);
            fromPbi.setItemBatch(s.getItemBatch());
            fromPbi.setQty(fromBi.getQty());
            //Rates
            fromBi.setNetRate(s.getItemBatch().getPurcahseRate());
            fromBi.setRate(s.getItemBatch().getRetailsaleRate());
            //Values
            fromBi.setGrossValue(s.getItemBatch().getRetailsaleRate() * s.getStock());
            fromBi.setNetValue(s.getStock() * fromBi.getNetRate());
            fromBi.setDiscount(0.0);
            fromBi.setInwardChargeType(InwardChargeType.Medicine);
            fromBi.setItem(s.getItemBatch().getItem());
            fromBi.setBill(fromBill);
            fromBi.setSearialNo(i + 1);
            fromBi.setCreatedAt(Calendar.getInstance().getTime());
            fromBi.setCreater(getSessionController().getLoggedUser());

            fromPbi.setBillItem(null);

            if (fromPbi.getId() == null) {
                getPharmaceuticalBillItemFacade().create(fromPbi);
            }
            fromBi.setPharmaceuticalBillItem(fromPbi);
            if (fromBi.getId() == null) {
                getBillItemFacade().create(fromBi);
            }
            fromPbi.setBillItem(fromBi);
            getPharmaceuticalBillItemFacade().edit(fromPbi);
            fromBill.getBillItems().add(fromBi);
            getBillFacade().edit(fromBill);

            BillItem toBi = new BillItem();
            PharmaceuticalBillItem toPbi = new PharmaceuticalBillItem();
            toBi.setPharmaceuticalBillItem(null);
            toPbi.setStock(s);
            toBi.setItem(s.getItemBatch().getItem());
            toBi.setQty(0 - ((double) s.getStock()));
            //pharmaceutical Bill Item
            toPbi.setDoe(s.getItemBatch().getDateOfExpire());
            toPbi.setFreeQty(0.0);
            toPbi.setItemBatch(s.getItemBatch());
            toPbi.setQty(toBi.getQty());
            //Rates
            toBi.setNetRate(s.getItemBatch().getPurcahseRate());
            toBi.setRate(s.getItemBatch().getRetailsaleRate());
            //Values
            toBi.setGrossValue(s.getItemBatch().getRetailsaleRate() * s.getStock());
            toBi.setNetValue(s.getStock() * toBi.getNetRate());
            toBi.setDiscount(0.0);
            toBi.setInwardChargeType(InwardChargeType.Medicine);
            toBi.setItem(s.getItemBatch().getItem());
            toBi.setBill(toBill);
            toBi.setSearialNo(i + 1);
            toBi.setCreatedAt(Calendar.getInstance().getTime());
            toBi.setCreater(getSessionController().getLoggedUser());

            toPbi.setBillItem(null);

            if (toPbi.getId() == null) {
                getPharmaceuticalBillItemFacade().create(toPbi);
            }
            toBi.setPharmaceuticalBillItem(toPbi);
            if (toBi.getId() == null) {
                getBillItemFacade().create(toBi);
            }
            toPbi.setBillItem(toBi);
            getPharmaceuticalBillItemFacade().edit(toPbi);
            toBill.getBillItems().add(toBi);
            getBillFacade().edit(toBill);

            getPharmacyBean().resetStock(fromPbi, s, 0.0, fromDepartment);
            getPharmacyBean().addToStock(toPbi, s.getStock(), toDepartment);

            i++;
        }
        printPreview = true;

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Adjustments/Tranfer all stock(/faces/pharmacy/pharmacy_adjustment_department_all.xhtml)");
    }

    public void tem() {
        Stock s = new Stock();
        Bill toBill = new PreBill();
        int i = 0;

        BillItem toBi = new BillItem();
        PharmaceuticalBillItem toPbi = new PharmaceuticalBillItem();
        toBi.setPharmaceuticalBillItem(null);
        toPbi.setStock(s);
        toBi.setItem(s.getItemBatch().getItem());
        toBi.setQty(0 - ((double) s.getStock()));
        //pharmaceutical Bill Item
        toPbi.setDoe(s.getItemBatch().getDateOfExpire());
        toPbi.setFreeQty(0.0);
        toPbi.setItemBatch(s.getItemBatch());
        toPbi.setQty(toBi.getQty());
        //Rates
        toBi.setNetRate(s.getItemBatch().getPurcahseRate());
        toBi.setRate(s.getItemBatch().getRetailsaleRate());
        //Values
        toBi.setGrossValue(s.getItemBatch().getRetailsaleRate() * s.getStock());
        toBi.setNetValue(s.getStock() * toBi.getNetRate());
        toBi.setDiscount(0.0);
        toBi.setInwardChargeType(InwardChargeType.Medicine);
        toBi.setItem(s.getItemBatch().getItem());
        toBi.setBill(toBill);
        toBi.setSearialNo(i + 1);
        toBi.setCreatedAt(Calendar.getInstance().getTime());
        toBi.setCreater(getSessionController().getLoggedUser());

        toPbi.setBillItem(null);

        if (toPbi.getId() == null) {
            getPharmaceuticalBillItemFacade().create(toPbi);
        }
        toBi.setPharmaceuticalBillItem(toPbi);
        if (toBi.getId() == null) {
            getBillItemFacade().create(toBi);
        }
        toPbi.setBillItem(toBi);
        getPharmaceuticalBillItemFacade().edit(toPbi);
        toBill.getBillItems().add(toBi);
        getBillFacade().edit(toBill);

    }

    public void adjustDepartmentStock() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (errorCheck()) {
            return;
        }

        saveDeptAdjustmentBill();
        PharmaceuticalBillItem ph = saveDeptAdjustmentBillItems();
//        getDeptAdjustmentPreBill().getBillItems().add(getBillItem());
//        getBillFacade().edit(getDeptAdjustmentPreBill());
        setBill(getBillFacade().find(getDeptAdjustmentPreBill().getId()));
        getPharmacyBean().resetStock(ph, stock, qty, getSessionController().getDepartment());

        printPreview = true;

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Adjustments/Department stock(qty)or (Staff stock adjustments)(/faces/pharmacy/pharmacy_adjustment_department.xhtml)");
    }

    public void adjustStockForDepartment() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (errorCheck()) {
            return;
        }

        if (qty == null) {
            JsfUtil.addErrorMessage("Add Quantity..");
            return;
        }

        if ((comment == null) || (comment.trim().equals(""))) {
            JsfUtil.addErrorMessage("Add the Comment..");
            return;
        }

        saveDeptStockAdjustmentBill();
        PharmaceuticalBillItem ph = saveDeptAdjustmentBillItems();

//        getDeptAdjustmentPreBill().getBillItems().add(getBillItem());
//        getBillFacade().edit(getDeptAdjustmentPreBill());
        setBill(getBillFacade().find(getDeptAdjustmentPreBill().getId()));
        getPharmacyBean().resetStock(ph, stock, qty, getSessionController().getDepartment());

        printPreview = true;

        JsfUtil.addSuccessMessage("Stock Adjustment Successfully..");

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Adjustments/Department stock(qty)or (Staff stock adjustments)(/faces/pharmacy/pharmacy_adjustment_department.xhtml)");
    }

    public void adjustStaffStock() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (errorCheck()) {
            return;
        }

        if (qty == null) {
            JsfUtil.addErrorMessage("Add Quantity..");
            return;
        }

        if ((comment == null) || (comment.trim().equals(""))) {
            JsfUtil.addErrorMessage("Add the Comment..");
            return;
        }

        saveStaffStockAdjustmentBill();
        PharmaceuticalBillItem ph = saveDeptAdjustmentBillItems();
//        getDeptAdjustmentPreBill().getBillItems().add(getBillItem());
//        getBillFacade().edit(getDeptAdjustmentPreBill());
        setBill(getBillFacade().find(getDeptAdjustmentPreBill().getId()));
        getPharmacyBean().resetStock(ph, stock, qty, getSessionController().getDepartment());

        printPreview = true;

        makeNull();

        JsfUtil.addSuccessMessage("Staff Stock Adjustment Successfully..");

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Adjustments/Department stock(qty)or (Staff stock adjustments)(/faces/pharmacy/pharmacy_adjustment_department.xhtml)");
    }

    public void adjustDepartmentStockAll() {
        if (errorCheckAll()) {
            return;
        }
        bills = new ArrayList<>();
        for (Stock s : stocks) {
            if (s.getStock() != s.getCalculated()) {
                deptAdjustmentPreBill = null;
                saveDeptSingleStockAdjustmentBill();
                PharmaceuticalBillItem ph = saveDeptAdjustmentBillItems(s);
                bills.add(getBillFacade().find(getDeptAdjustmentPreBill().getId()));
                getPharmacyBean().resetStock(ph, s, s.getCalculated(), getSessionController().getDepartment());

            }

        }

//        getDeptAdjustmentPreBill().getBillItems().add(getBillItem());
//        getBillFacade().edit(getDeptAdjustmentPreBill());
        printPreview = true;

    }

    public void adjustDepartmentStockAllZero() {
        if (errorCheckAllZero()) {
            return;
        }
        bills = new ArrayList<>();
        for (Stock s : stocks) {
            if (s.getStock() != s.getCalculated()) {
                deptAdjustmentPreBill = null;
                saveDeptAdjustmentBill();
                PharmaceuticalBillItem ph = saveDeptAdjustmentBillItems(s);
                bills.add(getBillFacade().find(getDeptAdjustmentPreBill().getId()));
                getPharmacyBean().resetStock(ph, s, s.getCalculated(), getSessionController().getDepartment());

            }

        }

//        getDeptAdjustmentPreBill().getBillItems().add(getBillItem());
//        getBillFacade().edit(getDeptAdjustmentPreBill());
        printPreview = true;

    }

    public void adjustPurchaseRate() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (errorCheck()) {
            return;
        }

        if (String.valueOf(pr) == null) {
            JsfUtil.addErrorMessage("Add Purchase Rate..");
            return;
        }

        if (pr < 0) {
            JsfUtil.addErrorMessage("Invalied Purchase Rate..");
            return;
        }

        if ((comment == null) || (comment.trim().equals(""))) {
            JsfUtil.addErrorMessage("Add the Comment..");
            return;
        }

        savePurchaseRateAdjustmentBill();
        savePrAdjustmentBillItems();
        getStock().getItemBatch().setPurcahseRate(pr);
        getItemBatchFacade().edit(getStock().getItemBatch());
        deptAdjustmentPreBill = billFacade.find(getDeptAdjustmentPreBill().getId());

        printPreview = true;

        JsfUtil.addSuccessMessage("Purchase Rate Adjustment Successfully..");

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Adjustments/Purchase rate(/faces/pharmacy/pharmacy_adjustment_purchase_rate.xhtml)");

    }

    public void adjustExDate() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (errorCheck()) {
            return;
        }

        if (exDate == null) {
            JsfUtil.addErrorMessage("Add Expiry Date..");
            return;
        }
        
        if ((comment == null) || (comment.trim().equals(""))) {
            JsfUtil.addErrorMessage("Add the Comment..");
            return;
        }

        saveExpiryDateAdjustmentBill();
        saveExDateAdjustmentBillItems();
        getStock().getItemBatch().setDateOfExpire(exDate);
        getItemBatchFacade().edit(getStock().getItemBatch());
        bill = billFacade.find(getDeptAdjustmentPreBill().getId());
//        clearBill();
//        clearBillItem();
        printPreview = true;

        JsfUtil.addSuccessMessage("Expiry Date Adjustment Successfully..");

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Adjustments/Expiry Rate(/faces/pharmacy/pharmacy_adjustment_expiry_date.xhtml)");
    }

    public void adjustRetailRate() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (errorCheck()) {
            return;
        }

        if (rsr == null) {
            JsfUtil.addErrorMessage("Add Retail Sale Rate..");
            return;
        }

        if ((comment == null) || (comment.trim().equals(""))) {
            JsfUtil.addErrorMessage("Add the Comment..");
            return;
        }

        saveSaleRateAdjustmentBill();
        saveRsrAdjustmentBillItems();
        getStock().getItemBatch().setRetailsaleRate(rsr);
        getItemBatchFacade().edit(getStock().getItemBatch());
        bill = billFacade.find(getDeptAdjustmentPreBill().getId());

        JsfUtil.addSuccessMessage("Retail Sale Rate Adjustment Successfully..");

        printPreview = true;

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Adjustments/Sale rate(/faces/pharmacy/pharmacy_adjustment_retail_sale_rate.xhtml)");
    }

    public void adjustWholesaleRate() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (errorCheck()) {
            return;
        }

        if (wsr == null) {
            JsfUtil.addErrorMessage("Invalied Wholesale Rate..");
            return;
        }

        if ((comment == null) || (comment.trim().equals(""))) {
            JsfUtil.addErrorMessage("Add the Comment..");
            return;
        }

        saveWholeSaleRateAdjustmentBill();
        saveWsrAdjustmentBillItems();
        getStock().getItemBatch().setWholesaleRate(wsr);
        getItemBatchFacade().edit(getStock().getItemBatch());
        bill = billFacade.find(getDeptAdjustmentPreBill().getId());
        printPreview = true;

        JsfUtil.addSuccessMessage("Wholesale Rate Adjustment Successfully..");

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Adjustments/Wholesale rate(/faces/pharmacy/pharmacy_adjustment_whole_sale_rate.xhtml)");
    }

    public void listnerItemSelect() {
        stocks = new ArrayList<>();
        if (getItem() == null) {
            return;
        }
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("i", getItem());
        sql = "select i from Stock i where i.department=:d "
                + " and i.itemBatch.item=:i "
                + " and i.stock>0 "
                + " order by i.stock desc ";
        stocks = getStockFacade().findByJpql(sql, m);
        total = 0.0;
        for (Stock s : stocks) {
            s.setCalculated(s.getStock());
            total += s.getStock();
        }
    }

    public void listnerChangeAdjustedStock() {
        if (qty == null) {
            for (Stock s : stocks) {
                s.setCalculated(s.getStock());
            }
            return;
        }
        if (total == qty) {
            JsfUtil.addErrorMessage("New Stock Equal To old Stock.");
        }
        double addQty = 0.0;

        if (total < qty) {
            for (Stock s : stocks) {
                s.setCalculated(s.getStock());
            }
            stocks.get(stocks.size() - 1).setCalculated(stocks.get(stocks.size() - 1).getStock() + (qty - total));
        } else {
            boolean flag = false;
            for (Stock s : stocks) {
                addQty += s.getStock();
                if (flag) {
                    s.setCalculated(0.0);
                } else {
                    if (addQty >= qty) {
                        flag = true;
                        s.setCalculated(s.getStock() - (addQty - qty));
                    } else {
                        s.setCalculated(s.getStock());
                    }
                }
            }
        }

    }

    public void onEdit() {
        qty = 0.0;
        for (Stock s : stocks) {
            qty += s.getCalculated();
        }
    }

    public void newBill() {
        deptAdjustmentPreBill = null;
        billItems = null;
        stocks = new ArrayList<>();
        bills = new ArrayList<>();
        item = new Item();
        qty = null;
        printPreview = false;
    }

    public void clearBill() {
        deptAdjustmentPreBill = null;
        billItems = null;
        comment = "";
        stocks = new ArrayList<>();
        stock = null;
    }

    private void clearBillItem() {
        billItem = null;
        removingBillItem = null;
        editingBillItem = null;
        qty = null;
        pr = null;
        rsr = null;
        wsr = null;
        stock = null;
        exDate = null;

    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillItem getRemovingBillItem() {
        return removingBillItem;
    }

    public void setRemovingBillItem(BillItem removingBillItem) {
        this.removingBillItem = removingBillItem;
    }

    public BillItem getEditingBillItem() {
        return editingBillItem;
    }

    public void setEditingBillItem(BillItem editingBillItem) {
        this.editingBillItem = editingBillItem;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public Bill getDeptAdjustmentPreBill() {
        if (deptAdjustmentPreBill == null) {
            deptAdjustmentPreBill = new PreBill();
            deptAdjustmentPreBill.setBillType(BillType.PharmacyAdjustment);
        }
        return deptAdjustmentPreBill;
    }

    public void setDeptAdjustmentPreBill(Bill deptAdjustmentPreBill) {
        this.deptAdjustmentPreBill = deptAdjustmentPreBill;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getExDate() {
        return exDate;
    }

    public void setExDate(Date exDate) {
        this.exDate = exDate;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public ItemBatchFacade getItemBatchFacade() {
        return itemBatchFacade;
    }

    public void setItemBatchFacade(ItemBatchFacade itemBatchFacade) {
        this.itemBatchFacade = itemBatchFacade;
    }

    public Double getPr() {
        return pr;
    }

    public Double getRsr() {
        return rsr;
    }

    public Double getWsr() {
        return wsr;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public void setPr(Double pr) {
        this.pr = pr;
    }

    public void setRsr(Double rsr) {
        this.rsr = rsr;
    }

    public void setWsr(Double wsr) {
        this.wsr = wsr;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public Bill getSaleBill() {
        return saleBill;
    }

    public void setSaleBill(Bill saleBill) {
        this.saleBill = saleBill;
    }

    public YearMonthDay getYearMonthDay() {
        return yearMonthDay;
    }

    public void setYearMonthDay(YearMonthDay yearMonthDay) {
        this.yearMonthDay = yearMonthDay;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public List<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(List<Stock> stocks) {
        this.stocks = stocks;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public boolean isManualAdjust() {
        return manualAdjust;
    }

    public void setManualAdjust(boolean manualAdjust) {
        this.manualAdjust = manualAdjust;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Amp getAmp() {
        return amp;
    }

    public void setAmp(Amp amp) {
        this.amp = amp;
    }

    public List<Stock> getAmpStock() {
        return ampStock;
    }

    public void setAmpStock(List<Stock> ampStock) {
        this.ampStock = ampStock;
    }

}
