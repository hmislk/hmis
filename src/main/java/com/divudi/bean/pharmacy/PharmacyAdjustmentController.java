/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.data.dto.StockDTO;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.BillService;
import com.divudi.service.pharmacy.PharmacyCostingService;
import com.divudi.service.pharmacy.StockSearchService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
    private SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
////////////////////////
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private StockFacade stockFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    private PharmacyCostingService pharmacyCostingService;
    @EJB
    private com.divudi.core.facade.StaffFacade staffFacade;
    @Inject
    private BillService billService;
    @Inject
    private StockSearchService stockSearchService;

/////////////////////////
//    Item selectedAlternative;
    private Bill deptAdjustmentPreBill;
    private Bill saleBill;
    Bill bill;
    BillItem billItem;
    BillItem removingBillItem;
    BillItem editingBillItem;

    Stock stock;
    StockDTO selectedStockDto;
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

    private int tabIndex = 0;

    private YearMonthDay yearMonthDay;

    List<BillItem> billItems;
    List<BillItem> expiryDateAdjustmentBillItems;
    List<Stock> stocks;
    List<Bill> bills;

    private Amp amp;
    private List<StockDTO> ampStock;
    private List<StockDTO> retailRateStockDtos;
    private List<StockDTO> costRateStockDtos;
    private StockDTO selectedRetailRateStockDto;
    private StockDTO selectedCostRateStockDto;

    private boolean printPreview;

    // Staff selection properties
    private com.divudi.core.entity.Staff selectedStaff;
    private List<Stock> staffStocks;

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public String navigateToViewReportOnItemviceAdjustments() {
        if (fromDepartment == null) {
            fromDepartment = sessionController.getDepartment();
        }
        return "/pharmacy/pharmacy_report_adjustment_bill_item?faces-redirect=true";
    }

    public void fillDepartmentAdjustmentByBillItem() {
        billItems = fetchBillItemsForAllAdjustmentTypes();
    }

    public void fillExpiryDateAdjustmentReport() {
        expiryDateAdjustmentBillItems = fetchExpiryDateAdjustmentBillItems();
    }

    public void fillAmpStocks() {
        ampStock = fetchAmpStocks(false);
    }

    public void fillAmpStocksWithPositiveQty() {
        ampStock = fetchAmpStocks(true);
    }

    public void fillRetailRateStockDtos() {
        if (amp == null) {
            JsfUtil.addErrorMessage("No Item Selected");
            retailRateStockDtos = new ArrayList<>();
            return;
        }
        retailRateStockDtos = stockSearchService.findRetailRateStockDtos(amp, sessionController.getLoggedUser().getDepartment());
    }

    public void fillCostRateStockDtos() {
        if (amp == null) {
            JsfUtil.addErrorMessage("No Item Selected");
            costRateStockDtos = new ArrayList<>();
            return;
        }
        costRateStockDtos = stockSearchService.findCostRateStockDtos(amp, sessionController.getLoggedUser().getDepartment());
        // Clear comment field when new item is selected
        comment = null;
    }

    private List<StockDTO> fetchAmpStocks(boolean onlyPositive) {
        if (amp == null) {
            JsfUtil.addErrorMessage("No Item Selected");
            return new ArrayList<>();
        }

        String sql = "SELECT new com.divudi.core.data.dto.StockDTO("
                + "s.id, "
                + "s.id, "
                + "s.itemBatch.id, "
                + "s.itemBatch.item.name, "
                + "s.itemBatch.item.code, "
                + "s.itemBatch.retailsaleRate, "
                + "s.stock, "
                + "s.itemBatch.dateOfExpire, "
                + "s.itemBatch.batchNo, "
                + "s.itemBatch.purcahseRate, "
                + "s.itemBatch.wholesaleRate, "
                + "s.itemBatch.costRate) "
                + "FROM Stock s "
                + "WHERE s.department = :d "
                + "AND s.itemBatch.item = :amp ";

        if (onlyPositive) {
            sql += "AND s.stock > 0 ";
        }

        sql += "ORDER BY s.stock DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("d", sessionController.getDepartment());
        params.put("amp", amp);

        return (List<StockDTO>) getStockFacade().findLightsByJpql(sql, params);
    }

    public List<BillItem> fetchBillItemsForAllAdjustmentTypes() {
        List<BillItem> billItems;
        List<BillType> adjustmentBillTypes = Arrays.asList(
                BillType.PharmacyAdjustment,
                BillType.PharmacyStockAdjustmentBill,
                BillType.PharmacyAdjustmentDepartmentStock,
                BillType.PharmacyAdjustmentDepartmentSingleStock,
                BillType.PharmacyAdjustmentStaffStock,
                BillType.PharmacyAdjustmentSaleRate,
                BillType.PharmacyAdjustmentWholeSaleRate,
                BillType.PharmacyAdjustmentPurchaseRate,
                BillType.PharmacyAdjustmentCostRate,
                BillType.PharmacyAdjustmentExpiryDate
        );

        Map m = new HashMap();
        String sql = "select bi from BillItem bi where "
                + " bi.bill.createdAt between :fd and :td "
                + " and bi.bill.billType in :billTypes ";

        if (fromDepartment != null) {
            sql += " and bi.bill.department=:fdept ";
            m.put("fdept", fromDepartment);
        }

        sql += " order by bi.id";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("billTypes", adjustmentBillTypes);

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        return billItems;
    }

    public List<BillItem> fetchBillItems(BillType bt) {
        List<BillItem> billItems;

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

    public List<BillItem> fetchExpiryDateAdjustmentBillItems() {
        List<BillItem> expiryAdjustmentItems;

        Map m = new HashMap();
        String sql;
        sql = "select bi from BillItem bi where "
                + " bi.bill.createdAt between :fd and :td "
                + " and bi.bill.billType=:bt "
                + " and bi.pharmaceuticalBillItem.beforeAdjustmentExpiry is not null "
                + " and bi.pharmaceuticalBillItem.afterAdjustmentExpiry is not null ";

        if (fromDepartment != null) {
            sql += " and bi.bill.department=:fdept ";
            m.put("fdept", fromDepartment);
        }

        sql += " order by bi.bill.createdAt desc, bi.id";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyAdjustmentExpiryDate);

        expiryAdjustmentItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        return expiryAdjustmentItems;
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
        retailRateStockDtos = new ArrayList<>();
        selectedRetailRateStockDto = null;
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

    public StockDTO getSelectedStockDto() {
        return selectedStockDto;
    }

    public void setSelectedStockDto(StockDTO selectedStockDto) {
        this.selectedStockDto = selectedStockDto;
        // When a DTO is selected, also load the corresponding Stock entity if needed
        if (selectedStockDto != null) {
            this.stock = getStockFacade().find(selectedStockDto.getId());
        }
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
                + "((i.itemBatch.item.code) like :n or "
                + "(i.staff.person.name) like :n or "
                + "(i.staff.code) like :n or "
                + "(i.itemBatch.item.name) like :n ) "
                + "order by i.itemBatch.item.name, i.itemBatch.dateOfExpire , i.stock desc";
        items = getStockFacade().findByJpql(sql, m, 20);

        return items;
    }

    public List<Stock> completeStaffStocksInStore(String qry) {
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
                + " and i.itemBatch.item."
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

    public List<com.divudi.core.entity.Staff> completeStaff(String qry) {
        List<com.divudi.core.entity.Staff> staffs;
        String sql;
        Map<String, Object> m = new HashMap<>();
        m.put("n", "%" + qry.toUpperCase() + "%");
        sql = "select s from Staff s where (s.person.name like :n or s.code like :n) "
                + "and s.retired = false order by s.person.name";
        staffs = staffFacade.findByJpql(sql, m, 20);
        return staffs;
    }

    public void listStaffStock() {
        if (selectedStaff == null) {
            JsfUtil.addErrorMessage("Please select a staff member first");
            staffStocks = null;
            return;
        }

        List<Stock> items;
        String sql;
        Map<String, Object> m = new HashMap<>();
        double d = 0.0;
        m.put("s", d);
        m.put("staff", selectedStaff);
        sql = "select i from Stock i where i.stock > :s and i.staff = :staff "
                + "order by i.itemBatch.item.name, i.itemBatch.dateOfExpire, i.stock desc";
        items = getStockFacade().findByJpql(sql, m);
        staffStocks = items;

        if (staffStocks == null || staffStocks.isEmpty()) {
            JsfUtil.addErrorMessage("No stock found for the selected staff member");
        } else {
            JsfUtil.addSuccessMessage(staffStocks.size() + " stock items found for " + selectedStaff.getPerson().getName());
        }
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

    public List<BillItem> getExpiryDateAdjustmentBillItems() {
        if (expiryDateAdjustmentBillItems == null) {
            expiryDateAdjustmentBillItems = new ArrayList<>();
        }
        return expiryDateAdjustmentBillItems;
    }

    public void setExpiryDateAdjustmentBillItems(List<BillItem> expiryDateAdjustmentBillItems) {
        this.expiryDateAdjustmentBillItems = expiryDateAdjustmentBillItems;
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
        String deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT);
        getDeptAdjustmentPreBill().setDeptId(deptId);
        getDeptAdjustmentPreBill().setInsId(deptId);
        getDeptAdjustmentPreBill().setBillType(BillType.PharmacyAdjustmentDepartmentStock);
        getDeptAdjustmentPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT);
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
        String deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_STAFF_STOCK_ADJUSTMENT);
        getDeptAdjustmentPreBill().setDeptId(deptId);
        getDeptAdjustmentPreBill().setInsId(deptId);
        getDeptAdjustmentPreBill().setBillType(BillType.PharmacyAdjustmentStaffStock);
        getDeptAdjustmentPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_STAFF_STOCK_ADJUSTMENT);
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
        String deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_PURCHASE_RATE_ADJUSTMENT);
        getDeptAdjustmentPreBill().setDeptId(deptId);
        getDeptAdjustmentPreBill().setInsId(deptId);
        getDeptAdjustmentPreBill().setBillType(BillType.PharmacyAdjustmentPurchaseRate);
        getDeptAdjustmentPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_PURCHASE_RATE_ADJUSTMENT);
        getDeptAdjustmentPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setToDepartment(null);
        getDeptAdjustmentPreBill().setToInstitution(null);
        getDeptAdjustmentPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setComments(comment);

        // Create BillFinanceDetails for the adjustment
        if (getDeptAdjustmentPreBill().getBillFinanceDetails() == null) {
            BillFinanceDetails bfd = new BillFinanceDetails(getDeptAdjustmentPreBill());
            getDeptAdjustmentPreBill().setBillFinanceDetails(bfd);
        }

        if (getDeptAdjustmentPreBill().getId() == null) {
            getBillFacade().create(getDeptAdjustmentPreBill());
        } else {
            getBillFacade().edit(getDeptAdjustmentPreBill());
        }
    }

    private void saveCostRateAdjustmentBill() {
        getDeptAdjustmentPreBill().setBillDate(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setBillTime(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreater(getSessionController().getLoggedUser());
        String deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_COST_RATE_ADJUSTMENT);
        getDeptAdjustmentPreBill().setDeptId(deptId);
        getDeptAdjustmentPreBill().setInsId(deptId);
        getDeptAdjustmentPreBill().setBillType(BillType.PharmacyAdjustmentCostRate);
        getDeptAdjustmentPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_COST_RATE_ADJUSTMENT);
        getDeptAdjustmentPreBill().setDepartment(sessionController.getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setInstitution(sessionController.getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setToDepartment(null);
        getDeptAdjustmentPreBill().setToInstitution(null);
        getDeptAdjustmentPreBill().setFromDepartment(sessionController.getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setFromInstitution(sessionController.getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setComments(comment);

        if (getDeptAdjustmentPreBill().getBillFinanceDetails() == null) {
            BillFinanceDetails bfd = new BillFinanceDetails(getDeptAdjustmentPreBill());
            getDeptAdjustmentPreBill().setBillFinanceDetails(bfd);
        }

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
        String deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT);
        getDeptAdjustmentPreBill().setDeptId(deptId);
        getDeptAdjustmentPreBill().setInsId(deptId);
        getDeptAdjustmentPreBill().setBillType(BillType.PharmacyAdjustmentSaleRate);
        getDeptAdjustmentPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT);
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
        String deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_WHOLESALE_RATE_ADJUSTMENT);
        getDeptAdjustmentPreBill().setDeptId(deptId);
        getDeptAdjustmentPreBill().setInsId(deptId);
        getDeptAdjustmentPreBill().setBillType(BillType.PharmacyAdjustmentWholeSaleRate);
        getDeptAdjustmentPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_WHOLESALE_RATE_ADJUSTMENT);
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
        String deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_STOCK_EXPIRY_DATE_AJUSTMENT);
        getDeptAdjustmentPreBill().setDeptId(deptId);
        getDeptAdjustmentPreBill().setInsId(deptId);
        getDeptAdjustmentPreBill().setBillType(BillType.PharmacyAdjustmentExpiryDate);
        getDeptAdjustmentPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_STOCK_EXPIRY_DATE_AJUSTMENT);

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

//        PharmaceuticalBillItem ph = getBillItem().getPharmaceuticalBillItem();
        tbi.setPharmaceuticalBillItem(null);
        getBillItem().getPharmaceuticalBillItem().setStock(stock);

        tbi.setItem(getStock().getItemBatch().getItem());
        tbi.setQty(qty);

        //pharmaceutical Bill Item
        getBillItem().getPharmaceuticalBillItem().setDoe(getStock().getItemBatch().getDateOfExpire());
        getBillItem().getPharmaceuticalBillItem().setFreeQty(0.0f);
        getBillItem().getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());

        Stock fetchedStock = getStockFacade().find(stock.getId());
        double stockQty = fetchedStock.getStock();
        double changingQty;

        changingQty = qty - stockQty;

        //set before, after values
        getBillItem().getPharmaceuticalBillItem().setBeforeAdjustmentValue(stockQty);
        getBillItem().getPharmaceuticalBillItem().setQty(changingQty);
        getBillItem().getPharmaceuticalBillItem().setAfterAdjustmentValue(stockQty + changingQty);

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
        if (tbi.getId() == null) {
            getBillItemFacade().create(tbi);
        } else {
            getBillItemFacade().edit(tbi);
        }
        getDeptAdjustmentPreBill().getBillItems().add(tbi);
        getBillFacade().edit(getDeptAdjustmentPreBill());
        return getBillItem().getPharmaceuticalBillItem();
    }

    private PharmaceuticalBillItem saveDeptAdjustmentBillItems(Stock s) {
        billItem = null;
        BillItem tbi = getBillItem();

        PharmaceuticalBillItem ph = getBillItem().getPharmaceuticalBillItem();

//        tbi.setPharmaceuticalBillItem(null);
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

    private void savePrAdjustmentBillItems(StockDTO dto, double oldPurchaseRate, double newPurchaseRate,
            double purchaseRateChange, double changeValue) {
        BillItem tbi = new BillItem();
        tbi.setBill(getDeptAdjustmentPreBill());
        PharmaceuticalBillItem ph = tbi.getPharmaceuticalBillItem();

        // Validate DTO and retrieve entities using IDs
        if (dto.getStockId() == null || dto.getItemBatchId() == null) {
            throw new RuntimeException("StockDTO stockId or itemBatchId is null");
        }

        Stock stockEntity = stockFacade.find(dto.getStockId());
        if (stockEntity == null) {
            throw new RuntimeException("Stock not found with ID: " + dto.getStockId());
        }

        ItemBatch ib = itemBatchFacade.find(dto.getItemBatchId());
        if (ib == null) {
            throw new RuntimeException("ItemBatch not found with ID: " + dto.getItemBatchId());
        }

        // Record the adjustment values in PharmaceuticalBillItem
        ph.setPurchaseRate(oldPurchaseRate); // Store old purchase rate for record
        ph.setRetailRate(dto.getRetailRate()); // Keep retail rate unchanged as per requirement
        ph.setStock(stockEntity);

        // Store batch details in PharmaceuticalBillItem
        ph.setItemBatch(ib);
        ph.setQtyInUnit(dto.getStockQty());
        ph.setQty(dto.getStockQty());

        // Set adjustment-specific data - store new purchase rate in lastPurchaseRate field
        ph.setLastPurchaseRate(newPurchaseRate); // New purchase rate stored for reference
        // REPURPOSED FIELD: freeQty is temporarily used to store the purchase rate change amount
        // This allows tracking of the adjustment value for audit and display purposes
        ph.setFreeQty(purchaseRateChange); // Store rate change in freeQty field (repurposed)

        // Set before and after adjustment values for proper tracking in reports
        ph.setBeforeAdjustmentValue(oldPurchaseRate);
        ph.setAfterAdjustmentValue(newPurchaseRate);

        // Validate getDeptAdjustmentPreBill
        if (getDeptAdjustmentPreBill() == null) {
            throw new RuntimeException("DeptAdjustmentPreBill is null");
        }

        // Configure BillItem
        tbi.setItem(ib.getItem());
        tbi.setRate(newPurchaseRate); // New purchase rate
        tbi.setQty(dto.getStockQty()); // Stock quantity

        // Store the change value as the bill item value
        tbi.setGrossValue(Math.abs(changeValue)); // Absolute change value
        tbi.setNetValue(changeValue); // Signed change value (+ or -)
        tbi.setDiscount(0.0); // No discount for adjustments

        // Create and populate BillItemFinanceDetails
        BillItemFinanceDetails bifd = new BillItemFinanceDetails();
        bifd.setBillItem(tbi);
        bifd.setQuantity(java.math.BigDecimal.valueOf(dto.getStockQty()));
        bifd.setFreeQuantity(java.math.BigDecimal.ZERO);
        bifd.setLineGrossRate(java.math.BigDecimal.valueOf(newPurchaseRate));
        bifd.setLineNetRate(java.math.BigDecimal.valueOf(newPurchaseRate));
        bifd.setLineGrossTotal(java.math.BigDecimal.valueOf(Math.abs(changeValue)));
        bifd.setLineNetTotal(java.math.BigDecimal.valueOf(changeValue));
        bifd.setLineDiscount(java.math.BigDecimal.ZERO);
        bifd.setLineCost(java.math.BigDecimal.valueOf(Math.abs(changeValue)));
        bifd.setRetailSaleRate(java.math.BigDecimal.valueOf(dto.getRetailRate()));
        bifd.setCreatedAt(Calendar.getInstance().getTime());
        bifd.setCreatedBy(getSessionController().getLoggedUser());

        tbi.setBillItemFinanceDetails(bifd);

        tbi.setInwardChargeType(InwardChargeType.Medicine);
        tbi.setItem(ib.getItem());
        tbi.setBill(getDeptAdjustmentPreBill());
        tbi.setSearialNo(getDeptAdjustmentPreBill().getBillItems().size() + 1);
        tbi.setCreatedAt(Calendar.getInstance().getTime());
        tbi.setCreater(getSessionController().getLoggedUser());

        try {
            if (tbi.getId() == null) {
                getBillItemFacade().create(tbi);
            } else {
                getBillItemFacade().edit(tbi);
            }
        } catch (javax.persistence.PersistenceException e) {
            Logger.getLogger(PharmacyAdjustmentController.class.getName()).log(Level.SEVERE, "Failed to save purchase rate adjustment bill items", e);
            throw new RuntimeException("Failed to save purchase rate adjustment bill items", e);
        }

//        getDeptAdjustmentPreBill().getBillItems().add(tbi);
        // Update bill totals with the change value
        Double currentTotalObj = getDeptAdjustmentPreBill().getTotal();
        Double currentNetTotalObj = getDeptAdjustmentPreBill().getNetTotal();
        double currentTotal = currentTotalObj != null ? currentTotalObj : 0.0;
        double currentNetTotal = currentNetTotalObj != null ? currentNetTotalObj : 0.0;

        getDeptAdjustmentPreBill().setTotal(currentTotal + Math.abs(changeValue));
        getDeptAdjustmentPreBill().setNetTotal(currentNetTotal + changeValue);

        // Update BillFinanceDetails using PharmacyCostingService for proper financial tracking
        BillFinanceDetails bfd = getDeptAdjustmentPreBill().getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails(getDeptAdjustmentPreBill());
            getDeptAdjustmentPreBill().setBillFinanceDetails(bfd);
        }

        // Set the purchase value change (only this should be recorded as per requirement)
        java.math.BigDecimal changeVal = java.math.BigDecimal.valueOf(changeValue);
        java.math.BigDecimal beforeVal = java.math.BigDecimal.valueOf(oldPurchaseRate * getStock().getStock());
        java.math.BigDecimal afterVal = java.math.BigDecimal.valueOf(newPurchaseRate * getStock().getStock());

        bfd.setTotalPurchaseValue(changeVal);
        bfd.setNetTotal(changeVal);
        bfd.setGrossTotal(java.math.BigDecimal.valueOf(Math.abs(changeValue)));
        bfd.setTotalQuantity(java.math.BigDecimal.valueOf(getStock().getStock()));

        // Aggregate before/after totals
        java.math.BigDecimal prevBefore = bfd.getTotalBeforeAdjustmentValue() == null ? java.math.BigDecimal.ZERO : bfd.getTotalBeforeAdjustmentValue();
        java.math.BigDecimal prevAfter = bfd.getTotalAfterAdjustmentValue() == null ? java.math.BigDecimal.ZERO : bfd.getTotalAfterAdjustmentValue();
        bfd.setTotalBeforeAdjustmentValue(prevBefore.add(beforeVal));
        bfd.setTotalAfterAdjustmentValue(prevAfter.add(afterVal));

        // Ensure cost rate, retail rate, wholesale rate values are NOT recorded as per requirement
        bfd.setTotalCostValue(java.math.BigDecimal.ZERO);
        bfd.setTotalRetailSaleValue(java.math.BigDecimal.ZERO);
        bfd.setTotalWholesaleValue(java.math.BigDecimal.ZERO);

        if (tbi.getId() == null) {
            getBillItemFacade().create(tbi);
        } else {
            getBillItemFacade().edit(tbi);
        }
    }

    private double calculateCostRateChange(StockDTO dto) {
        if (dto.getNewCostRate() == null || dto.getPurchaseRate() == null) {
            return 0.0;
        }
        return dto.getNewCostRate() - dto.getPurchaseRate();
    }

    private boolean validateCostRateAdjustment(StockDTO dto) {
        if (dto.getNewCostRate() == null) {
            return false;
        }
        if (dto.getNewCostRate() < 0) {
            JsfUtil.addErrorMessage("Cost rate must be positive");
            return false;
        }
        return true;
    }

    private void saveCrAdjustmentBillItems(StockDTO dto, double oldCostRate, double newCostRate,
            double costRateChange, double changeValue) {
        BillItem tbi = new BillItem();
        tbi.setBill(getDeptAdjustmentPreBill());
        PharmaceuticalBillItem ph = tbi.getPharmaceuticalBillItem();

        if (dto.getStockId() == null || dto.getItemBatchId() == null) {
            throw new RuntimeException("StockDTO stockId or itemBatchId is null");
        }

        Stock stockEntity = stockFacade.find(dto.getStockId());
        if (stockEntity == null) {
            throw new RuntimeException("Stock not found with ID: " + dto.getStockId());
        }

        ItemBatch ib = itemBatchFacade.find(dto.getItemBatchId());
        if (ib == null) {
            throw new RuntimeException("ItemBatch not found with ID: " + dto.getItemBatchId());
        }

        ph.setPurchaseRate(ib.getCostRate() != null ? ib.getCostRate() : 0.0);
        ph.setBeforeAdjustmentValue(oldCostRate);
        ph.setAfterAdjustmentValue(newCostRate);
        ph.setStock(stockEntity);
        ph.setItemBatch(ib);
        ph.setQtyInUnit(dto.getStockQty());
        ph.setQty(dto.getStockQty());
        // REPURPOSED FIELD: freeQty is temporarily used to store the cost rate change amount
        // This field is repurposed for cost rate adjustments to track the difference for audit and display
        ph.setFreeQty(costRateChange);

        if (getDeptAdjustmentPreBill() == null) {
            throw new RuntimeException("DeptAdjustmentPreBill is null");
        }

        tbi.setItem(ib.getItem());
        tbi.setRate(newCostRate);
        tbi.setQty(dto.getStockQty());
        tbi.setGrossValue(Math.abs(changeValue));
        tbi.setNetValue(changeValue);
        tbi.setDiscount(0.0);

        BillItemFinanceDetails bifd = new BillItemFinanceDetails();
        bifd.setBillItem(tbi);
        bifd.setQuantity(java.math.BigDecimal.valueOf(dto.getStockQty()));
        bifd.setFreeQuantity(java.math.BigDecimal.ZERO);
        bifd.setLineGrossRate(java.math.BigDecimal.valueOf(newCostRate));
        bifd.setLineNetRate(java.math.BigDecimal.valueOf(newCostRate));
        bifd.setLineGrossTotal(java.math.BigDecimal.valueOf(Math.abs(changeValue)));
        bifd.setLineNetTotal(java.math.BigDecimal.valueOf(changeValue));
        bifd.setLineDiscount(java.math.BigDecimal.ZERO);
        bifd.setLineCost(java.math.BigDecimal.valueOf(Math.abs(changeValue)));
        bifd.setRetailSaleRate(java.math.BigDecimal.valueOf(dto.getRetailRate()));
        bifd.setCreatedAt(Calendar.getInstance().getTime());
        bifd.setCreatedBy(getSessionController().getLoggedUser());

        tbi.setBillItemFinanceDetails(bifd);

        tbi.setInwardChargeType(InwardChargeType.Medicine);
        tbi.setBill(getDeptAdjustmentPreBill());
        tbi.setSearialNo(getDeptAdjustmentPreBill().getBillItems().size() + 1);
        tbi.setCreatedAt(Calendar.getInstance().getTime());
        tbi.setCreater(getSessionController().getLoggedUser());

        try {
            if (tbi.getId() == null) {
                getBillItemFacade().create(tbi);
            } else {
                getBillItemFacade().edit(tbi);
            }
        } catch (javax.persistence.PersistenceException e) {
            Logger.getLogger(PharmacyAdjustmentController.class.getName()).log(Level.SEVERE, "Failed to save cost rate adjustment bill items", e);
            throw new RuntimeException("Failed to save cost rate adjustment bill items", e);
        }

        Double currentTotalObj = getDeptAdjustmentPreBill().getTotal();
        Double currentNetTotalObj = getDeptAdjustmentPreBill().getNetTotal();
        double currentTotal = currentTotalObj != null ? currentTotalObj : 0.0;
        double currentNetTotal = currentNetTotalObj != null ? currentNetTotalObj : 0.0;

        getDeptAdjustmentPreBill().setTotal(currentTotal + Math.abs(changeValue));
        getDeptAdjustmentPreBill().setNetTotal(currentNetTotal + changeValue);

        BillFinanceDetails bfd = getDeptAdjustmentPreBill().getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails(getDeptAdjustmentPreBill());
            getDeptAdjustmentPreBill().setBillFinanceDetails(bfd);
        }

        java.math.BigDecimal changeVal = java.math.BigDecimal.valueOf(changeValue);

        java.math.BigDecimal beforeVal = java.math.BigDecimal.valueOf(oldCostRate * dto.getStockQty());
        java.math.BigDecimal afterVal = java.math.BigDecimal.valueOf(newCostRate * dto.getStockQty());
        bfd.setTotalCostValue(changeVal);
        bfd.setNetTotal(changeVal);
        bfd.setGrossTotal(java.math.BigDecimal.valueOf(Math.abs(changeValue)));
        bfd.setTotalQuantity(java.math.BigDecimal.valueOf(dto.getStockQty()));
        java.math.BigDecimal prevBefore = bfd.getTotalBeforeAdjustmentValue() == null ? java.math.BigDecimal.ZERO : bfd.getTotalBeforeAdjustmentValue();
        java.math.BigDecimal prevAfter = bfd.getTotalAfterAdjustmentValue() == null ? java.math.BigDecimal.ZERO : bfd.getTotalAfterAdjustmentValue();
        bfd.setTotalBeforeAdjustmentValue(prevBefore.add(beforeVal));
        bfd.setTotalAfterAdjustmentValue(prevAfter.add(afterVal));

        bfd.setTotalPurchaseValue(java.math.BigDecimal.ZERO);
        bfd.setTotalRetailSaleValue(java.math.BigDecimal.ZERO);
        bfd.setTotalWholesaleValue(java.math.BigDecimal.ZERO);

        if (tbi.getId() == null) {
            getBillItemFacade().create(tbi);
        } else {
            getBillItemFacade().edit(tbi);
        }
    }

    private void deductBeforeAdjustmentItemFromStock() {
        if (stock == null) {
            JsfUtil.addErrorMessage("Please select a stock");
            return;
        }

        BillItem tbi = new BillItem();
        PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
        ItemBatch itemBatch = itemBatchFacade.find(getStock().getItemBatch().getId());

        tbi.setItem(getStock().getItemBatch().getItem());
        tbi.setGrossValue(getStock().getItemBatch().getRetailsaleRate() * getStock().getStock());
        tbi.setNetValue(getStock().getStock() * tbi.getNetRate());
        tbi.setDiscount(tbi.getGrossValue() - tbi.getNetValue());
        tbi.setInwardChargeType(InwardChargeType.Medicine);
        tbi.setBill(getDeptAdjustmentPreBill());
        tbi.setSearialNo(getDeptAdjustmentPreBill().getBillItems().size() + 1);
        tbi.setCreatedAt(Calendar.getInstance().getTime());
        tbi.setCreater(getSessionController().getLoggedUser());

        ph.setPurchaseRate(itemBatch.getPurcahseRate());
        ph.setRetailRate(itemBatch.getRetailsaleRate());
        ph.setStock(stock);
        ph.setQty(0 - stock.getStock());

        ph.setBillItem(tbi);
        tbi.setPharmaceuticalBillItem(ph);

        getBillItemFacade().create(tbi);

        boolean returnFlag = getPharmacyBean().deductFromStock(
                getStock(), Math.abs(getStock().getStock()), ph, getDeptAdjustmentPreBill().getDepartment());
        if (!returnFlag) {
            JsfUtil.addErrorMessage("Stock deduction failed – transaction rolled back?");
        }

    }

    private void saveRsrAdjustmentBillItems(StockDTO dto, double oldRetailRate, double newRetailRate,
            double retailRateChange, double changeValue) {
        BillItem tbi = new BillItem();
        tbi.setBill(getDeptAdjustmentPreBill());
        PharmaceuticalBillItem ph = tbi.getPharmaceuticalBillItem();

        // Validate DTO and retrieve entities using IDs
        if (dto.getStockId() == null || dto.getItemBatchId() == null) {
            throw new RuntimeException("StockDTO stockId or itemBatchId is null");
        }

        Stock stockEntity = stockFacade.find(dto.getStockId());
        if (stockEntity == null) {
            throw new RuntimeException("Stock not found with ID: " + dto.getStockId());
        }

        ItemBatch ib = itemBatchFacade.find(dto.getItemBatchId());
        if (ib == null) {
            throw new RuntimeException("ItemBatch not found with ID: " + dto.getItemBatchId());
        }

        // Record the adjustment values in PharmaceuticalBillItem
        ph.setPurchaseRate(dto.getPurchaseRate()); // Keep purchase rate unchanged
        ph.setRetailRate(oldRetailRate); // Store old retail rate for record
        ph.setStock(stockEntity);

        // Store batch details in PharmaceuticalBillItem
        ph.setItemBatch(ib);
        ph.setQtyInUnit(dto.getStockQty());
        ph.setQty(dto.getStockQty());

        // Set adjustment-specific data - store new retail rate in lastPurchaseRate field
        ph.setLastPurchaseRate(newRetailRate); // New retail rate stored for reference
        // REPURPOSED FIELD: freeQty is temporarily used to store the retail rate change amount
        // This allows tracking of the adjustment value for audit and display purposes
        ph.setFreeQty(retailRateChange); // Store rate change in freeQty field (repurposed)

        // Set before and after adjustment values for proper tracking in reports
        ph.setBeforeAdjustmentValue(oldRetailRate);
        ph.setAfterAdjustmentValue(newRetailRate);

        // Validate getDeptAdjustmentPreBill
        if (getDeptAdjustmentPreBill() == null) {
            throw new RuntimeException("DeptAdjustmentPreBill is null");
        }

        // Configure BillItem
        tbi.setItem(ib.getItem());
        tbi.setRate(newRetailRate); // New retail rate
        tbi.setQty(dto.getStockQty()); // Stock quantity

        // Store the change value as the bill item value
        tbi.setGrossValue(Math.abs(changeValue)); // Absolute change value
        tbi.setNetValue(changeValue); // Signed change value (+ or -)
        tbi.setDiscount(0.0); // No discount for adjustments

        // Create and populate BillItemFinanceDetails
        BillItemFinanceDetails bifd = new BillItemFinanceDetails();
        bifd.setBillItem(tbi);
        bifd.setQuantity(java.math.BigDecimal.valueOf(dto.getStockQty()));
        bifd.setFreeQuantity(java.math.BigDecimal.ZERO);
        bifd.setLineGrossRate(java.math.BigDecimal.valueOf(newRetailRate));
        bifd.setLineNetRate(java.math.BigDecimal.valueOf(newRetailRate));
        bifd.setLineGrossTotal(java.math.BigDecimal.valueOf(Math.abs(changeValue)));
        bifd.setLineNetTotal(java.math.BigDecimal.valueOf(changeValue));
        bifd.setLineDiscount(java.math.BigDecimal.ZERO);
        bifd.setLineCost(java.math.BigDecimal.valueOf(Math.abs(changeValue)));
        bifd.setRetailSaleRate(java.math.BigDecimal.valueOf(newRetailRate));
        bifd.setCreatedAt(Calendar.getInstance().getTime());
        bifd.setCreatedBy(getSessionController().getLoggedUser());

        tbi.setBillItemFinanceDetails(bifd);

        tbi.setInwardChargeType(InwardChargeType.Medicine);
        tbi.setItem(ib.getItem());
        tbi.setBill(getDeptAdjustmentPreBill());
        tbi.setSearialNo(getDeptAdjustmentPreBill().getBillItems().size() + 1);
        tbi.setCreatedAt(Calendar.getInstance().getTime());
        tbi.setCreater(getSessionController().getLoggedUser());

        try {
            // Save entities
            getBillItemFacade().create(tbi);
            getDeptAdjustmentPreBill().getBillItems().add(tbi);

        } catch (Exception e) {
            Logger.getLogger(PharmacyAdjustmentController.class.getName()).log(Level.SEVERE, "Failed to create retail rate adjustment bill item", e);
            throw new RuntimeException("Failed to create retail rate adjustment bill item: " + e.getMessage(), e);
        }
    }

    private void saveRsrAdjustmentBillItems() {
        if (stock == null) {
            JsfUtil.addErrorMessage("Please select a stock");
            return;
        }

        BillItem tbi = new BillItem();
        PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
        ItemBatch itemBatch = itemBatchFacade.find(getStock().getItemBatch().getId());

        tbi.setItem(getStock().getItemBatch().getItem());
        tbi.setRate(rsr);
        tbi.setGrossValue(getStock().getItemBatch().getRetailsaleRate() * getStock().getStock());
        tbi.setNetValue(getStock().getStock() * tbi.getNetRate());
        tbi.setDiscount(tbi.getGrossValue() - tbi.getNetValue());
        tbi.setInwardChargeType(InwardChargeType.Medicine);
        tbi.setBill(getDeptAdjustmentPreBill());
        tbi.setSearialNo(getDeptAdjustmentPreBill().getBillItems().size() + 1);
        tbi.setCreatedAt(Calendar.getInstance().getTime());
        tbi.setCreater(getSessionController().getLoggedUser());

        ph.setPurchaseRate(itemBatch.getPurcahseRate());
        ph.setBeforeAdjustmentValue(itemBatch.getRetailsaleRate());
        ph.setAfterAdjustmentValue(rsr);
        ph.setRetailRate(rsr);
        ph.setStock(stock);
        ph.setQty(stock.getStock());

        ph.setBillItem(tbi);
        tbi.setPharmaceuticalBillItem(ph);

        getBillItemFacade().create(tbi);

        boolean addFlag = getPharmacyBean().addToStock(
                getStock(), Math.abs(getStock().getStock()), ph, getDeptAdjustmentPreBill().getDepartment());

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
        // Set before and after adjustment values for proper tracking in reports
        ph.setBeforeAdjustmentValue(itemBatch.getWholesaleRate());
        ph.setAfterAdjustmentValue(wsr);
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
        ItemBatch itemBatch = itemBatchFacade.find(getStock().getItemBatch().getId());
        tbi.getPharmaceuticalBillItem().setDoe(itemBatch.getDateOfExpire());
        // Set before and after adjustment expiry dates for the bill item
        tbi.getPharmaceuticalBillItem().setBeforeAdjustmentExpiry(itemBatch.getDateOfExpire());
        tbi.getPharmaceuticalBillItem().setAfterAdjustmentExpiry(exDate);
        // For expiry date adjustments, set value fields to 0 to avoid confusion in reports
        tbi.getPharmaceuticalBillItem().setBeforeAdjustmentValue(0.0);
        tbi.getPharmaceuticalBillItem().setAfterAdjustmentValue(0.0);
        //tbi.setItem(getStock().getItemBatch().getItem());
        //itemBatch.setDateOfExpire(exDate);
        //tbi.setRate(rsr);
        //pharmaceutical Bill Item
        tbi.getPharmaceuticalBillItem().setStock(stock);
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
        if (tbi.getId() == null) {
            getBillItemFacade().create(tbi);
        } else {
            getBillItemFacade().edit(tbi);
        }
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
            fromBi.setQty(0 - s.getStock());
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
            toBi.setQty(0 - s.getStock());
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

    }

    public void adjustDepartmentStock() {
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

    }

    public void adjustStockForDepartment() {
        if (errorCheck()) {
            return;
        }

        if (qty == null) {
            JsfUtil.addErrorMessage("Add Quantity..");
            return;
        }

        if ((comment == null) || (comment.trim().isEmpty())) {
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

    }

    public void adjustStaffStock() {
        if (errorCheck()) {
            return;
        }
        if (qty == null) {
            JsfUtil.addErrorMessage("Add Quantity..");
            return;
        }
        if ((comment == null) || (comment.trim().isEmpty())) {
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

        JsfUtil.addSuccessMessage("Staff Stock Adjustment Successfully..");

    }

    public void adjustDepartmentStockAll() {
        if (errorCheckAll()) {
            return;
        }
        deptAdjustmentPreBill = new PreBill();
        for (Stock s : stocks) {
            if (s.getStock() != s.getCalculated()) {
                saveDeptSingleStockAdjustmentBill();
                PharmaceuticalBillItem ph = saveDeptAdjustmentBillItems(s);
                getPharmacyBean().resetStock(ph, s, s.getCalculated(), getSessionController().getDepartment());

            }

        }
//        for (BillItem bi : getDeptAdjustmentPreBill().getBillItems()){
//            System.out.println("bi = " + bi.getPharmaceuticalBillItem().getDoe());
//        }

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

        if ((comment == null) || (comment.trim().isEmpty())) {
            JsfUtil.addErrorMessage("Add the Comment..");
            return;
        }

        try {
            // This method should work with selectedStockDto - if not available, this indicates
            // the UI needs to be updated to work with DTOs instead of Stock entities
            if (selectedStockDto == null) {
                throw new RuntimeException("No stock DTO selected. This method should work with StockDTO.");
            }

            // Calculate purchase rate changes using DTO data
            double oldPurchaseRate = selectedStockDto.getPurchaseRate() != null ? selectedStockDto.getPurchaseRate() : 0.0;
            double newPurchaseRate = pr;
            double purchaseRateChange = newPurchaseRate - oldPurchaseRate;
            double stockQuantity = selectedStockDto.getStockQty() != null ? selectedStockDto.getStockQty() : 0.0;
            double changeValue = stockQuantity * purchaseRateChange;

            savePurchaseRateAdjustmentBill();
            savePrAdjustmentBillItems(selectedStockDto, oldPurchaseRate, newPurchaseRate, purchaseRateChange, changeValue);

            // Update the item batch with new purchase rate - only retrieve entity when needed for business operation
            ItemBatch itemBatch = itemBatchFacade.find(selectedStockDto.getItemBatchId());
            if (itemBatch != null) {
                itemBatch.setPurcahseRate(pr);
                getItemBatchFacade().edit(itemBatch);
            }

            deptAdjustmentPreBill = billFacade.find(getDeptAdjustmentPreBill().getId());

            printPreview = true;

            JsfUtil.addSuccessMessage("Purchase Rate Adjustment Successfully. Change: "
                    + (purchaseRateChange >= 0 ? "+" : "") + purchaseRateChange
                    + ", Change Value: " + changeValue);
        } catch (Exception e) {
            Logger.getLogger(PharmacyAdjustmentController.class.getName()).log(Level.SEVERE, "Failed to adjust purchase rate", e);
            JsfUtil.addErrorMessage("Failed to adjust purchase rate: " + e.getMessage());
            return;
        }

    }

    public void adjustPurchaseRates() {
        if (ampStock == null || ampStock.isEmpty()) {
            JsfUtil.addErrorMessage("No Stocks");
            return;
        }

        if ((comment == null) || (comment.trim().isEmpty())) {
            JsfUtil.addErrorMessage("Add the Comment..");
            return;
        }

        savePurchaseRateAdjustmentBill();

        boolean any = false;
        for (StockDTO dto : ampStock) {
            if (dto.getNewPurchaseRate() == null) {
                continue;
            }
            any = true;
            Stock s = stockFacade.find(dto.getStockId());
            if (s == null) {
                continue;
            }
            stock = s;

            double oldPurchaseRate = dto.getPurchaseRate();
            double newPurchaseRate = dto.getNewPurchaseRate();
            double purchaseRateChange = newPurchaseRate - oldPurchaseRate;
            double changeValue = dto.getStockQty() * purchaseRateChange;

            savePrAdjustmentBillItems(dto, oldPurchaseRate, newPurchaseRate, purchaseRateChange, changeValue);

            s.getItemBatch().setPurcahseRate(newPurchaseRate);
            itemBatchFacade.edit(s.getItemBatch());
        }

        if (!any) {
            JsfUtil.addErrorMessage("Enter at least one new purchase rate");
            return;
        }

        if (getDeptAdjustmentPreBill().getId() == null) {
            getBillFacade().create(getDeptAdjustmentPreBill());
        } else {
            getBillFacade().edit(getDeptAdjustmentPreBill());
        }

        deptAdjustmentPreBill = billService.reloadBill(getDeptAdjustmentPreBill());
        printPreview = true;
        JsfUtil.addSuccessMessage("Purchase Rate Adjustment Successfully");
    }

    public void adjustCostRates() {
        if (costRateStockDtos == null || costRateStockDtos.isEmpty()) {
            JsfUtil.addErrorMessage("No Stocks");
            return;
        }

        if ((comment == null) || (comment.trim().isEmpty())) {
            JsfUtil.addErrorMessage("Add the Comment..");
            return;
        }

        saveCostRateAdjustmentBill();

        boolean any = false;
        for (StockDTO dto : costRateStockDtos) {
            if (dto.getNewCostRate() == null) {
                continue;
            }
            any = true;
            Stock s = stockFacade.find(dto.getStockId());
            if (s == null) {
                continue;
            }
            stock = s;

            double oldCostRate = s.getItemBatch().getCostRate() != null ? s.getItemBatch().getCostRate() : 0.0;
            double newCostRate = dto.getNewCostRate();
            double costRateChange = newCostRate - oldCostRate;
            double changeValue = dto.getStockQty() * costRateChange;

            saveCrAdjustmentBillItems(dto, oldCostRate, newCostRate, costRateChange, changeValue);

            s.getItemBatch().setCostRate(newCostRate);
            itemBatchFacade.edit(s.getItemBatch());
        }

        if (!any) {
            JsfUtil.addErrorMessage("Enter at least one new cost rate");
            return;
        }

        if (getDeptAdjustmentPreBill().getId() == null) {
            getBillFacade().create(getDeptAdjustmentPreBill());
        } else {
            getBillFacade().edit(getDeptAdjustmentPreBill());
        }

        deptAdjustmentPreBill = billService.reloadBill(getDeptAdjustmentPreBill());
        printPreview = true;
        JsfUtil.addSuccessMessage("Cost Rate Adjustment Successfully");
    }

    public void adjustRetailRates() {
        if (retailRateStockDtos == null || retailRateStockDtos.isEmpty()) {
            JsfUtil.addErrorMessage("No Stocks");
            return;
        }

        if ((comment == null) || (comment.trim().isEmpty())) {
            JsfUtil.addErrorMessage("Add the Comment..");
            return;
        }

        saveSaleRateAdjustmentBill();

        boolean any = false;
        for (StockDTO dto : retailRateStockDtos) {
            if (dto.getNewRetailRate() == null) {
                continue;
            }
            any = true;
            Stock s = stockFacade.find(dto.getStockId());
            if (s == null) {
                continue;
            }
            stock = s;

            double oldRetailRate = dto.getRetailRate();
            double newRetailRate = dto.getNewRetailRate();
            double retailRateChange = newRetailRate - oldRetailRate;
            double changeValue = dto.getStockQty() * retailRateChange;

            saveRsrAdjustmentBillItems(dto, oldRetailRate, newRetailRate, retailRateChange, changeValue);

            s.getItemBatch().setRetailsaleRate(newRetailRate);
            itemBatchFacade.edit(s.getItemBatch());
        }

        if (!any) {
            JsfUtil.addErrorMessage("Enter at least one new retail rate");
            return;
        }

        if (getDeptAdjustmentPreBill().getId() == null) {
            getBillFacade().create(getDeptAdjustmentPreBill());
        } else {
            getBillFacade().edit(getDeptAdjustmentPreBill());
        }

        deptAdjustmentPreBill = billService.reloadBill(getDeptAdjustmentPreBill());
        printPreview = true;
        JsfUtil.addSuccessMessage("Retail Sale Rate Adjustment Successfully");
    }

    public void adjustExDate() {
        if (errorCheck()) {
            return;
        }

        if (exDate == null) {
            JsfUtil.addErrorMessage("Add Expiry Date..");
            return;
        }

        // Normalize selected expiry to end-of-month when month-year pattern is used
        boolean expiryIsAlwaysMonthEnd = configOptionApplicationController.getBooleanValueByKey("Always Set Expiry Date to Month End", false);
        if (expiryIsAlwaysMonthEnd) {
            exDate = normalizeToEndOfMonth(exDate);
        }
        if ((comment == null) || (comment.trim().isEmpty())) {
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

    }

    /**
     * Ensure the expiry date reflects the last instant of the selected month.
     * Useful when UI captures only month-year (e.g., pattern "MMM yyyy").
     */
    private Date normalizeToEndOfMonth(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    public void adjustRetailRate() {
        if (errorCheck()) {
            return;
        }

        if (rsr == null) {
            JsfUtil.addErrorMessage("Add Retail Sale Rate..");
            return;
        }

        if ((comment == null) || (comment.trim().isEmpty())) {
            JsfUtil.addErrorMessage("Add the Comment..");
            return;
        }

        saveSaleRateAdjustmentBill();
        deductBeforeAdjustmentItemFromStock();
        saveRsrAdjustmentBillItems();
        getStock().getItemBatch().setRetailsaleRate(rsr);
        getItemBatchFacade().edit(getStock().getItemBatch());
        bill = billFacade.find(getDeptAdjustmentPreBill().getId());

        JsfUtil.addSuccessMessage("Retail Sale Rate Adjustment Successfully..");

        printPreview = true;

    }

    public void adjustWholesaleRate() {
        if (errorCheck()) {
            return;
        }

        if (wsr == null) {
            JsfUtil.addErrorMessage("Invalied Wholesale Rate..");
            return;
        }

        if ((comment == null) || (comment.trim().isEmpty())) {
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
        expiryDateAdjustmentBillItems = null;
        stocks = new ArrayList<>();
        bills = new ArrayList<>();
        item = new Item();
        qty = null;
        printPreview = false;
    }

    public void clearBill() {
        deptAdjustmentPreBill = null;
        billItems = null;
        expiryDateAdjustmentBillItems = null;
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

    public List<StockDTO> getAmpStock() {
        return ampStock;
    }

    public void setAmpStock(List<StockDTO> ampStock) {
        this.ampStock = ampStock;
    }

    public List<StockDTO> getRetailRateStockDtos() {
        return retailRateStockDtos;
    }

    public void setRetailRateStockDtos(List<StockDTO> retailRateStockDtos) {
        this.retailRateStockDtos = retailRateStockDtos;
    }

    public StockDTO getSelectedRetailRateStockDto() {
        return selectedRetailRateStockDto;
    }

    public void setSelectedRetailRateStockDto(StockDTO selectedRetailRateStockDto) {
        this.selectedRetailRateStockDto = selectedRetailRateStockDto;
    }

    public List<StockDTO> getCostRateStockDtos() {
        return costRateStockDtos;
    }

    public void setCostRateStockDtos(List<StockDTO> costRateStockDtos) {
        this.costRateStockDtos = costRateStockDtos;
    }

    public StockDTO getSelectedCostRateStockDto() {
        return selectedCostRateStockDto;
    }

    public void setSelectedCostRateStockDto(StockDTO selectedCostRateStockDto) {
        this.selectedCostRateStockDto = selectedCostRateStockDto;
    }

    public double calculateTotalBefore(Bill bill) {
        double total = 0.0;
        if (bill != null && bill.getBillItems() != null) {
            for (BillItem bi : bill.getBillItems()) {
                if (bi.getPharmaceuticalBillItem() != null) {
                    double qty = bi.getQty() != null ? bi.getQty() : 0.0;
                    total += bi.getPharmaceuticalBillItem().getPurchaseRate() * qty;
                }
            }
        }
        return total;
    }

    public double calculateTotalAfter(Bill bill) {
        double total = 0.0;
        if (bill != null && bill.getBillItems() != null) {
            for (BillItem bi : bill.getBillItems()) {
                if (bi.getPharmaceuticalBillItem() != null) {
                    double qty = bi.getQty() != null ? bi.getQty() : 0.0;
                    total += bi.getPharmaceuticalBillItem().getLastPurchaseRate() * qty;
                }
            }
        }
        return total;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
    }

    public com.divudi.core.entity.Staff getSelectedStaff() {
        return selectedStaff;
    }

    public void setSelectedStaff(com.divudi.core.entity.Staff selectedStaff) {
        this.selectedStaff = selectedStaff;
        // Clear staff stocks when staff changes
        staffStocks = null;
    }

    public List<Stock> getStaffStocks() {
        return staffStocks;
    }

    public void setStaffStocks(List<Stock> staffStocks) {
        this.staffStocks = staffStocks;
    }

}
