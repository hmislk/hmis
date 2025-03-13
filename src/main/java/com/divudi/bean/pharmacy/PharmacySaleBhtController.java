/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.CommonFunctionsController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UserNotificationController;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.inward.InwardBeanController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.DepartmentType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.StockQty;
import com.divudi.data.Title;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.data.inward.SurgeryBillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.PreBill;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Ampp;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.entity.pharmacy.UserStock;
import com.divudi.entity.pharmacy.UserStockContainer;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.entity.pharmacy.Vmpp;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.StockFacade;
import com.divudi.facade.StockHistoryFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

@Named
@SessionScoped
public class PharmacySaleBhtController implements Serializable {

    /**
     * Creates a new instance of PharmacySaleController
     */
    public PharmacySaleBhtController() {
    }

    @Inject
    UserStockController userStockController;
    @Inject
    PaymentSchemeController PaymentSchemeController;

    @Inject
    SessionController sessionController;
    @Inject
    PharmacyCalculation pharmacyCalculation;
    @Inject
    UserNotificationController userNotificationController;
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

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
/////////////////////////
    Item selectedAlternative;
    private PreBill preBill;
    Bill printBill;
    Bill bill;
    private Bill bhtRequestBill;
    BillItem billItem;
    Stock replacableStock;
    Item selectedAvailableAmp;
    //BillItem removingBillItem;
    BillItem editingBillItem;
    Double qty;
    Stock stock;
    private Item item;
    private PatientEncounter patientEncounter;
    int activeIndex;
    boolean billPreview = false;
    Department department;
    String errorMessage = "";
    /////////////////
    List<Stock> replaceableStocks;
    //List<BillItem> billItems;
    List<Item> itemsWithoutStocks;
    List<BillItem> billItems;
    /////////////////////////
    private UserStockContainer userStockContainer;

    private Bill batchBill;
    @Inject
    private BillBeanController billBean;
    private Stock tmpStock;

    public void selectSurgeryBillListener() {
        patientEncounter = getBatchBill().getPatientEncounter();
    }

    public void settleSurgeryBhtIssue() {
        if (getBatchBill() == null) {
            return;
        }
        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("There are No Medicines/Devices to Bill!!!");
            return;
        }
        if (getBatchBill().getProcedure() == null) {
            return;
        }
        if (getBatchBill().getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage("Sorry Patient is Discharged!!!");
            return;
        }
        BillTypeAtomic bta = BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE;
        BillType bt = BillType.PharmacyBhtPre;
        settleBhtIssue(bt, bta, getBatchBill().getFromDepartment());
        getBillBean().saveEncounterComponents(getPrintBill(), getBatchBill(), getSessionController().getLoggedUser());
        getBillBean().updateBatchBill(getBatchBill());
    }

//    public void settleSurgeryBhtIssueStore() {
//        if (getBatchBill() == null) {
//            return;
//        }
//
//        if (getBatchBill().getProcedure() == null) {
//            return;
//        }
//
//        settleBhtIssue(BillType.StoreBhtPre, getBatchBill().getFromDepartment(), BillNumberSuffix.PHISSUE);
//
//        getBillBean().saveEncounterComponents(getPrintBill(), getBatchBill(), getSessionController().getLoggedUser());
//        getBillBean().updateBatchBill(getBatchBill());
//
//    }
    public void makeNull() {
        selectedAlternative = null;
        preBill = null;
        printBill = null;
        bill = null;
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        activeIndex = 0;
        billPreview = false;
        replaceableStocks = null;
        itemsWithoutStocks = null;
        patientEncounter = null;
        batchBill = null;
    }

    public void makeNullWithFill() {
        makeNull();
//        searchController.createInwardBHTForIssueTable();
    }

    public double getOldQty(BillItem bItem) {
        String sql = "Select b.qty From BillItem b"
                + " where b.retired=false and b.bill=:b and b=:itm";
        HashMap hm = new HashMap();
        hm.put("b", getPreBill());
        hm.put("itm", bItem);
        return getBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public String navigateToCancelBhtRequest() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        return "/inward/bht_bill_cancel?faces-redirect=true;";
    }

    public void onEdit(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
        onEdit(tmp);
    }

    public void onEditing(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();

        tmp.setQty(tmp.getPharmaceuticalBillItem().getQtyInUnit());
        if (tmp.getPharmaceuticalBillItem().getQtyInUnit() <= 0) {
            setZeroToQty(tmp);
            JsfUtil.addErrorMessage("Can not enter a minus value");
            return;
        }
        Stock fetchedStock = tmp.getPharmaceuticalBillItem().getStock();
        if (tmp.getPharmaceuticalBillItem().getQtyInUnit() > fetchedStock.getStock()) {
            setZeroToQty(tmp);
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
        }

    }

    private void setZeroToQty(BillItem tmp) {
        tmp.setQty(0.0);
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0.0f);
        userStockController.updateUserStock(tmp.getTransUserStock(), 0);
    }

    private void setQtyToMatchAvailability(BillItem tmp, Double availableQty) {
        tmp.setQty(availableQty);
        tmp.getPharmaceuticalBillItem().setQtyInUnit(availableQty);
        tmp.getPharmaceuticalBillItem().setQty(availableQty);
        userStockController.updateUserStock(tmp.getTransUserStock(), availableQty);
    }

    //Check when edititng Qty
    //
    public boolean onEdit(BillItem tmp) {
        //Cheking Minus Value && Null
        if (tmp.getQty() <= 0 || tmp.getQty() == null) {
            setZeroToQty(tmp);
            onEditCalculation(tmp);

            JsfUtil.addErrorMessage("Can not enter a minus value");
            return true;
        }

        Stock fetchedStock = getStockFacade().find(tmp.getPharmaceuticalBillItem().getStock().getId());

        if (tmp.getQty() > fetchedStock.getStock()) {
            setQtyToMatchAvailability(tmp, fetchedStock.getStock());
            onEditCalculation(tmp);
            JsfUtil.addErrorMessage("There are no sufficient stocks. Please adjust quantity");
            return true;
        }

        //Check Is There Any Other User using same Stock
        if (!userStockController.isStockAvailable(tmp.getPharmaceuticalBillItem().getStock(), tmp.getQty(), getSessionController().getLoggedUser())) {

            setZeroToQty(tmp);
            onEditCalculation(tmp);

            JsfUtil.addErrorMessage("Another User On Change Bill Item "
                    + " Qty value is resetted");
            return true;
        }

        userStockController.updateUserStock(tmp.getTransUserStock(), tmp.getQty());

        onEditCalculation(tmp);

        return false;
    }

    private void onEditCalculation(BillItem tmp) {
        if (tmp == null) {
            return;
        }
        if (tmp.getPharmaceuticalBillItem() == null) {
            return;
        }
        if (tmp.getPharmaceuticalBillItem().getStock() == null) {
            return;
        }
        if (tmp.getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
            return;
        }
        if (tmp.getPharmaceuticalBillItem().getStock().getItemBatch().getItem() == null) {
            return;
        }
        calculateRates(tmp);
        tmp.setGrossValue(tmp.getQty() * tmp.getRate());
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0 - tmp.getQty());
//        calculateBillItemForEditing(tmp);
        calTotal();
    }

    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public List<Stock> getReplaceableStocks() {
        return replaceableStocks;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        if (qty != null && qty <= 0) {
            JsfUtil.addErrorMessage("Can not enter a minus value");
            return;
        }
        this.qty = qty;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public void setReplaceableStocks(List<Stock> replaceableStocks) {
        this.replaceableStocks = replaceableStocks;
    }

    public Item getSelectedAlternative() {
        return selectedAlternative;
    }

    public void setSelectedAlternative(Item selectedAlternative) {
        this.selectedAlternative = selectedAlternative;
    }

    public void selectReplaceableStocks() {
        if (selectedAlternative == null || !(selectedAlternative instanceof Amp)) {
            replaceableStocks = new ArrayList<>();
            return;
        }
        String sql;
        Map m = new HashMap();
        double d = 0.0;
        Amp amp = (Amp) selectedAlternative;
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("s", d);
        m.put("vmp", amp.getVmp());
        sql = "select i from Stock i join treat(i.itemBatch.item as Amp) amp where i.stock >:s and i.department=:d and amp.vmp=:vmp order by i.itemBatch.item.name";
        replaceableStocks = getStockFacade().findByJpql(sql, m);
    }

    public List<Item> getItemsWithoutStocks() {
        return itemsWithoutStocks;
    }

    public void setItemsWithoutStocks(List<Item> itemsWithoutStocks) {
        this.itemsWithoutStocks = itemsWithoutStocks;
    }

    public void resetAll() {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        clearBill();
        clearBillItem();
        billPreview = false;
        makeNull();
        department = null;
        replaceableStocks = new ArrayList<>();
        itemsWithoutStocks = new ArrayList<>();
        errorMessage = "";
    }

    public void selectReplaceableStocksNew() {
        if (selectedAvailableAmp == null || !(selectedAvailableAmp instanceof Amp)) {
            replaceableStocks = new ArrayList<>();
            return;
        }
        fillReplaceableStocksForAmp((Amp) selectedAvailableAmp);
    }

    public void makeStockAsBillItemStock() {
        ////// // System.out.println("replacableStock = " + replacableStock);
        setStock(replacableStock);
        getBillItem().getPharmaceuticalBillItem().setStock(getStock());
        calculateRates(billItem);
        ////// // System.out.println("getStock() = " + getStock());
    }

    public void fillReplaceableStocksForAmp(Amp ampIn) {
        String sql;
        Map m = new HashMap();
        double d = 0.0;
        Amp amp = ampIn;
        m.put("d", getDepartment());
        m.put("s", d);
        m.put("vmp", amp.getVmp());
        m.put("a", amp);
        sql = "select i from Stock i join treat(i.itemBatch.item as Amp) amp "
                + "where i.stock >:s and "
                + "i.department=:d and "
                + "amp.vmp=:vmp "
                + "and amp<>:a "
                + "order by i.itemBatch.item.name";
        replaceableStocks = getStockFacade().findByJpql(sql, m);
    }

    public List<Item> completeRetailSaleItems(String qry) {
        Map m = new HashMap<>();
        List<Item> items;
        String sql;
        sql = "select i from Item i"
                + " where i.retired=false "
                + " and (i.name) like :n "
                + " and type(i)=:t and i.id not "
                + " in(select ibs.id from Stock ibs "
                + " where ibs.stock >:s "
                + " and ibs.department=:d "
                + " and (ibs.itemBatch.item.name) like :n )"
                + " order by i.name ";
        m.put("t", Amp.class);
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("n", "%" + qry + "%");
        double s = 0.0;
        m.put("s", s);
        items = getItemFacade().findByJpql(sql, m, 10);
        return items;
    }

    public List<Item> completeRetailSaleItems(String qry, Department d) {
        Map m = new HashMap<>();
        List<Item> items;
        String sql;
        sql = "select i from Item i"
                + " where i.retired=false "
                + " and (i.name) like :n "
                + " and i.vmp is not null "
                + " and type(i)=:t and i.id not "
                + " in(select ibs.id from Stock ibs "
                + " where ibs.stock >:s "
                + " and ibs.department=:d "
                + " and ibs.itemBatch.item.vmp is not null "
                + " and (ibs.itemBatch.item.name) like :n )"
                + " order by i.name ";
        m.put("t", Amp.class);
        m.put("d", d);
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
        m.put("depTp", DepartmentType.Store);
        m.put("n", "%" + qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i"
                    + " where i.stock >:s"
                    + " and i.department=:d "
                    + " and i.itemBatch.item.departmentType is null "
                    + " or i.itemBatch.item.departmentType!=:depTp "
                    + " and ((i.itemBatch.item.name) like :n "
                    + " or (i.itemBatch.item.code) like :n "
                    + " or (i.itemBatch.item.barcode) like :n )  "
                    + " order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i "
                    + " where i.stock >:s "
                    + " and i.department=:d"
                    + " and i.itemBatch.item.departmentType is null "
                    + " or i.itemBatch.item.departmentType!=:depTp "
                    + "  and ((i.itemBatch.item.name) like :n "
                    + " or (i.itemBatch.item.code) like :n)  "
                    + " order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }
        items = getStockFacade().findByJpql(sql, m, 20);
        itemsWithoutStocks = completeRetailSaleItems(qry);
        //////// // System.out.println("selectedSaleitems = " + itemsWithoutStocks);
        return items;
    }

    public List<Stock> completeAvailableStocksStore(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("s", d);
        m.put("depTp", DepartmentType.Store);
        m.put("n", "%" + qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i"
                    + " where i.stock >:s"
                    + " and i.department=:d "
                    + " and i.itemBatch.item.departmentType=:depTp "
                    + " and ((i.itemBatch.item.name) like :n "
                    + " or (i.itemBatch.item.code) like :n "
                    + " or (i.itemBatch.item.barcode) like :n )  "
                    + " order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i "
                    + " where i.stock >:s "
                    + " and i.department=:d"
                    + " and i.itemBatch.item.departmentType=:depTp "
                    + "  and ((i.itemBatch.item.name) like :n "
                    + " or (i.itemBatch.item.code) like :n)  "
                    + " order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }
        items = getStockFacade().findByJpql(sql, m, 20);
        //  itemsWithoutStocks = completeRetailSaleItems(qry);
        //////// // System.out.println("selectedSaleitems = " + itemsWithoutStocks);
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

    private void savePreBillFinally(Patient pt, Department matrixDepartment, BillType billType, BillTypeAtomic billTypeAtomic) {
        getPreBill().setBillType(billType);
        getPreBill().setBillTypeAtomic(billTypeAtomic);
        String deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getLoggedUser().getDepartment(), billTypeAtomic);
        getPreBill().setInsId(deptId);
        getPreBill().setDeptId(deptId);

        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getPreBill().setCreater(getSessionController().getLoggedUser());

        getPreBill().setPatient(pt);
        getPreBill().setPatientEncounter(getPatientEncounter());

        getPreBill().setToDepartment(null);
        getPreBill().setToInstitution(null);
        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());

        getPreBill().setFromDepartment(matrixDepartment);
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        //TODO: What is this doing here. Need to investigate
        getBillBean().setSurgeryData(getPreBill(), getBatchBill(), SurgeryBillType.PharmacyItem);

        if (getPreBill().getId() == null) {
            getBillFacade().create(getPreBill());
        }

    }

    private void savePreBillFinallyRequest(Patient pt, Department matrixDepartment, BillType billType, BillNumberSuffix billNumberSuffix) {
        getPreBill().setBillType(billType);
        getPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), billType, BillClassType.PreBill, billNumberSuffix));
        getPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), billType, BillClassType.PreBill, billNumberSuffix));

        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getPreBill().setCreater(getSessionController().getLoggedUser());

        getPreBill().setPatient(pt);
        getPreBill().setPatientEncounter(getPatientEncounter());

        getPreBill().setToDepartment(getDepartment());
        getPreBill().setToInstitution(getDepartment().getInstitution());
        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());

        getPreBill().setFromDepartment(matrixDepartment);
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getBillBean().setSurgeryData(getPreBill(), getBatchBill(), SurgeryBillType.PharmacyItem);

        if (getPreBill().getId() == null) {
            getBillFacade().create(getPreBill());
        }

    }

    private void savePreBillItemsFinally(List<BillItem> list) {
        for (BillItem tbi : list) {
            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());
            tbi.setCreatedAt(Calendar.getInstance().getTime());
            tbi.setCreater(getSessionController().getLoggedUser());
            if (tbi.getId() == null) {
                getBillItemFacade().create(tbi);
            } else {
                getBillItemFacade().edit(tbi);
            }

            double qtyL = tbi.getPharmaceuticalBillItem().getQty() + tbi.getPharmaceuticalBillItem().getFreeQty();

            //Deduct Stock
            boolean returnFlag = getPharmacyBean().deductFromStock(tbi.getPharmaceuticalBillItem().getStock(),
                    Math.abs(qtyL), tbi.getPharmaceuticalBillItem(), getPreBill().getDepartment());
            if (!returnFlag) {
                tbi.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(tbi.getPharmaceuticalBillItem());
                getBillItemFacade().edit(tbi);
            }

            getPreBill().getBillItems().add(tbi);
        }

        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());

//        calculateAllRates();
        getBillFacade().edit(getPreBill());
    }

    private void savePreBillItemsFinallyRequest(List<BillItem> list) {
        for (BillItem tbi : list) {
            if (onEdit(tbi)) {//If any issue in Stock Bill Item will not save & not include for total
                continue;
            }

            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());

            tbi.setCreatedAt(Calendar.getInstance().getTime());
            tbi.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem tmpPh = tbi.getPharmaceuticalBillItem();
            tbi.setPharmaceuticalBillItem(null);

            if (tbi.getId() == null) {
                getBillItemFacade().create(tbi);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            tbi.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(tbi);
            tbi.getPharmaceuticalBillItem().setBillItem(tbi);
            getPharmaceuticalBillItemFacade().edit(tbi.getPharmaceuticalBillItem());

//            double qtyL = tbi.getPharmaceuticalBillItem().getQtyInUnit() + tbi.getPharmaceuticalBillItem().getFreeQtyInUnit();
//
//            //Deduct Stock
//            boolean returnFlag = getPharmacyBean().deductFromStock(tbi.getPharmaceuticalBillItem().getStock(),
//                    Math.abs(qtyL), tbi.getPharmaceuticalBillItem(), getPreBill().getDepartment());
//
//            if (!returnFlag) {
//                tbi.setTmpQty(0);
//                getPharmaceuticalBillItemFacade().edit(tbi.getPharmaceuticalBillItem());
//                getBillItemFacade().edit(tbi);
//            }
            getPreBill().getBillItems().add(tbi);
        }

        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());

        calculateAllRates();

        getBillFacade().edit(getPreBill());
    }

    private boolean checkAllBillItem() {
        if (getPreBill().getBillItems() == null) {
            return true;
        }
        if (getPreBill().getBillItems().isEmpty()) {
            return true;
        }
        for (BillItem b : getPreBill().getBillItems()) {

            if (onEdit(b)) {
                return true;
            }
        }

        return false;

    }

    public void settlePharmacyBhtIssue() {
        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please add items to the bill.");
            return;
        }
        if (errorCheck()) {
            return;
        }
        BillTypeAtomic bta = BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE;
        BillType bt = BillType.PharmacyBhtPre;
        Department matrixDept = null;
        boolean matrixByAdmissionDepartment;
        boolean matrixByIssuingDepartment;
        matrixByAdmissionDepartment = configOptionApplicationController.getBooleanValueByKey("Price Matrix is calculated from Inpatient Department for " + sessionController.getDepartment().getName(), true);
        matrixByIssuingDepartment = configOptionApplicationController.getBooleanValueByKey("Price Matrix is calculated from Issuing Department for " + sessionController.getDepartment().getName(), true);

        if (matrixByAdmissionDepartment) {
            if (getPatientEncounter() == null) {
                matrixDept = getSessionController().getDepartment();
            }
            if (getPatientEncounter().getCurrentPatientRoom() == null) {
                matrixDept = getSessionController().getDepartment();
            }
            if (getPatientEncounter().getCurrentPatientRoom() != null) {
                if (getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge() != null) {
                    matrixDept = getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment();
                }
            }

        } else if (matrixByIssuingDepartment) {
            matrixDept = getSessionController().getDepartment();
        } else {
            matrixDept = getSessionController().getDepartment();
        }

        settleBhtIssue(bt, bta, matrixDept);

    }

    public void settlePharmacyBhtIssueAccept() {
        if (errorCheck()) {
            return;
        }
        if (getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Nothing To Settle.");
            return;
        }
        BillTypeAtomic bta = BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE;
        BillType bt = BillType.PharmacyBhtPre;
        settleBhtIssueRequestAccept(bt, bta, getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment(), BillNumberSuffix.PHISSUE);
        userNotificationController.userNotificationRequestComplete();

    }

    public void settlePharmacyBhtIssueRequest() {
        if (errorCheck()) {
            return;
        }
        settleBhtIssueRequest(BillType.InwardPharmacyRequest, getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment(), BillNumberSuffix.PHISSUEREQ);

    }

    public void settleStoreBhtIssue() {
        if (errorCheck()) {
            return;
        }
        BillTypeAtomic bta = BillTypeAtomic.DIRECT_ISSUE_STORE_INWARD;
        BillType bt = BillType.StoreBhtPre;
        settleBhtIssue(bt, bta, getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment());
    }

    private boolean errorCheck() {
        if (getPatientEncounter() == null || getPatientEncounter().getPatient() == null) {
            JsfUtil.addErrorMessage("Please Select a BHT");
            return true;
        }

        if (getPatientEncounter().getAdmissionType().isRoomChargesAllowed() || getPatientEncounter().getCurrentPatientRoom() != null) {

            if (getPatientEncounter().getCurrentPatientRoom() == null) {
                JsfUtil.addErrorMessage("Please Select Patient Room");
                return true;
            }

            if (getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge() == null) {
                JsfUtil.addErrorMessage("Please Set Room");
                return true;
            }

        }

        if (getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage("Sorry Patient is Discharged!!!");
            return true;
        }

        if (getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Sorry this BHT was Settled !!!");
            return true;
        }
        for (BillItem bi : getPreBill().getBillItems()) {
            if (bi.getItem() == null) {
                JsfUtil.addErrorMessage("Requested item could not empty" + bi.getItem().getName());
                return true;
            }
            if (bi.getPharmaceuticalBillItem() == null) {
                JsfUtil.addErrorMessage("Requested item not found" + bi.getItem().getName());
                return true;
            }
            if (bi.getPharmaceuticalBillItem().getStock() == null) {
                JsfUtil.addErrorMessage("Requested item not found" + bi.getItem().getName());
                return true;
            }
            if (bi.getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
                JsfUtil.addErrorMessage("Please edit the item quantity to save" + bi.getItem().getName());
                return true;
            }

        }

//        if (checkAllBillItem()) {
//            //  UtilityController.addErrorMessage("Please Set Room 33");
//            return true;
//        }
        return false;
    }

    private void settleBhtIssue(BillType btp, BillTypeAtomic bta, Department matrixDepartment) {
        if (matrixDepartment == null) {
            JsfUtil.addErrorMessage("This Bht can't issue as this Surgery Has No Department");
            return;
        }

        Patient pt = getPatientEncounter().getPatient();
        getPreBill().setPaidAmount(0);

        List<BillItem> tmpBillItems = getPreBill().getBillItems();
        getPreBill().setBillItems(null);

        savePreBillFinally(pt, matrixDepartment, btp, bta);
        savePreBillItemsFinally(tmpBillItems);

//        updateMargin(getPreBill().getBillItems(), getPreBill(), getPreBill().getFromDepartment(), getPatientEncounter().getPaymentMethod());
        setPrintBill(getBillFacade().find(getPreBill().getId()));

        clearBill();
        clearBillItem();
        billPreview = true;

    }

    private void settleBhtIssueRequest(BillType btp, Department matrixDepartment, BillNumberSuffix billNumberSuffix) {

        if (matrixDepartment == null) {
            JsfUtil.addErrorMessage("This Bht can't issue as this Surgery Has No Department");
            return;
        }

        Patient pt = getPatientEncounter().getPatient();
        getPreBill().setPaidAmount(0);

        List<BillItem> tmpBillItems = getPreBill().getBillItems();
        getPreBill().setBillItems(null);

        savePreBillFinallyRequest(pt, matrixDepartment, btp, billNumberSuffix);
        savePreBillItemsFinallyRequest(tmpBillItems);

        // Calculation Margin
        updateMargin(getPreBill().getBillItems(), getPreBill(), getPreBill().getFromDepartment(), getPatientEncounter().getPaymentMethod());

        setPrintBill(getBillFacade().find(getPreBill().getId()));

        clearBill();
        clearBillItem();
        billPreview = true;

    }

    public void removeBillItem(BillItem b) {
        userStockController.removeUserStock(b.getTransUserStock(), getSessionController().getLoggedUser());
        getPreBill().getBillItems().remove(b.getSearialNo());

        calTotal();
    }

    private void settleBhtIssueRequestAccept(BillType btp, BillTypeAtomic bta, Department matrixDepartment, BillNumberSuffix billNumberSuffix) {

        if (matrixDepartment == null) {
            JsfUtil.addErrorMessage("This Bht can't issue as this Surgery Has No Department");
            return;
        }
        List<BillItem> tmpBillItems = getBillItems();

        if (!getBillItems().isEmpty()) {
            getPreBill().setReferenceBill(getBillItems().get(0).getReferanceBillItem().getBill());
        }

        for (BillItem tbi : tmpBillItems) {
            if (tbi.getPharmaceuticalBillItem().getQty() == 0.0) {
                JsfUtil.addErrorMessage("Item Qty is Zero " + tbi.getItem().getName());
                return;
            }
            if (tbi.getPharmaceuticalBillItem().getQty() > tbi.getPharmaceuticalBillItem().getStock().getStock()) {
                JsfUtil.addErrorMessage("Not Enough Stock " + tbi.getItem().getName());
                return;
            }
        }

        Patient pt = getPatientEncounter().getPatient();
        getPreBill().setPaidAmount(0);

        getPreBill().setBillItems(null);

        savePreBillFinally(pt, matrixDepartment, btp, bta);
        savePreBillItemsFinally(tmpBillItems);

        // Calculation Margin
        updateMargin(getPreBill().getBillItems(), getPreBill(), getPreBill().getFromDepartment(), getPatientEncounter().getPaymentMethod());

        setPrintBill(getBillFacade().find(getPreBill().getId()));

        clearBill();
        clearBillItem();
        billPreview = true;

    }

    public void updateMargin(BillItem bi, Department matrixDepartment, PaymentMethod paymentMethod) {
        double rate = Math.abs(bi.getRate());
        double margin;
        PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(bi, rate, matrixDepartment, paymentMethod);
        if (priceMatrix != null) {
            margin = ((bi.getGrossValue() * priceMatrix.getMargin()) / 100);
            bi.setMarginRate((bi.getRate() * (priceMatrix.getMargin() + 100)) / 100);
        } else {
            margin = 0.0;
            bi.setMarginRate(0.0);
        }

        bi.setMarginValue(margin);
        bi.setNetValue(bi.getGrossValue() + bi.getMarginValue());
        bi.setAdjustedValue(bi.getNetValue());
    }

    @Deprecated
    public void updateMargin(List<BillItem> billItems, Bill bill, Department matrixDepartment, PaymentMethod paymentMethod) {
        double total = 0;
        double netTotal = 0;
        double marginTotal = 0;
        for (BillItem bi : billItems) {

            double rate = Math.abs(bi.getRate());
            double margin;

            PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(bi, rate, matrixDepartment, paymentMethod);

            if (priceMatrix != null) {
                margin = ((bi.getGrossValue() * priceMatrix.getMargin()) / 100);
                bi.setMarginRate((bi.getRate() * (priceMatrix.getMargin() + 100)) / 100);
            } else {
                margin = 0.0;
                bi.setMarginRate(0.0);
            }

            bi.setMarginValue(margin);

            bi.setNetValue(bi.getGrossValue() + bi.getMarginValue());
            bi.setAdjustedValue(bi.getNetValue());
            getBillItemFacade().edit(bi);

            total += bi.getGrossValue();
            netTotal += bi.getNetValue();
            marginTotal += bi.getMarginValue();
        }

        bill.setTotal(total);
        bill.setNetTotal(netTotal);
        bill.setMargin(marginTotal);
        getBillFacade().edit(bill);

    }

    @EJB
    private BillFeeFacade billFeeFacade;
    @Inject
    private InwardBeanController inwardBean;

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    @Inject
    PriceMatrixController priceMatrixController;

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    private boolean checkItemBatch() {
        for (BillItem bItem : getPreBill().getBillItems()) {
            if (Objects.equals(bItem.getPharmaceuticalBillItem().getStock().getId(), getBillItem().getPharmaceuticalBillItem().getStock().getId())) {
                return true;
            }
        }

        return false;
    }

    public void addBillItem() {
        if (getStock() == null) {
            JsfUtil.addErrorMessage("No Stock");
            return;
        }
        if (getQty() == null) {
            errorMessage = "Quantity?";
            JsfUtil.addErrorMessage("Please enter a Quantity?");
            return;
        }
        if (getQty() <= 0.0) {
            errorMessage = "Quantity?";
            JsfUtil.addErrorMessage("Please enter a Quantity?");
            return;
        }
        if (getStock().getItemBatch().getDateOfExpire().before(CommonController.getCurrentDateTime())) {
            JsfUtil.addErrorMessage("You are NOT allowed to select Expired Items");
            return;
        }
        if (getPreBill() == null) {
            JsfUtil.addErrorMessage("No Prebill");
            return;
        }
        if (getBillItem() == null) {
            JsfUtil.addErrorMessage("No Bill Item");
            return;
        }
        if (getBillItem().getPharmaceuticalBillItem() == null) {
            JsfUtil.addErrorMessage("No Pharmaceutical Bill Item");
            return;
        }

        Stock fetchStock = getStockFacade().find(getStock().getId());

        if (getQty() > fetchStock.getStock()) {
            errorMessage = "No Sufficient Stocks?";
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return;
        }

        if (checkItemBatch()) {
            errorMessage = "Already added this item batch";
            JsfUtil.addErrorMessage("Already added this item batch");
            return;
        }
        //Checking User Stock Entity
        if (!userStockController.isStockAvailable(getStock(), getQty(), getSessionController().getLoggedUser())) {
            errorMessage = "Sorry Already Other User Try to Billing This Stock You Cant Add";
            JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
            return;
        }

//        if (CheckDateAfterOneMonthCurrentDateTime(getStock().getItemBatch().getDateOfExpire())) {
//            errorMessage = "This batch is Expire With in 31 Days.";
//            JsfUtil.addErrorMessage("This batch is Expire With in 31 Days.");
//            return;
//        }
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - Math.abs(qty));
        billItem.getPharmaceuticalBillItem().setQty(0 - Math.abs(qty));
        billItem.getPharmaceuticalBillItem().setStock(stock);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());

        //Bill Item
        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setQty(qty);

        //pharmaceutical Bill Item
        billItem.getPharmaceuticalBillItem().setDoe(getStock().getItemBatch().getDateOfExpire());
        billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);
        billItem.getPharmaceuticalBillItem().setQty(0 - Math.abs(qty));

        calculateRates(billItem);

        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setBill(getPreBill());
        billItem.setSearialNo(getPreBill().getBillItems().size() + 1);

        getPreBill().getBillItems().add(billItem);

        //User Stock Container Save if New Bill
        UserStockContainer usc = userStockController.saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());
        UserStock us = userStockController.saveUserStock(billItem, getSessionController().getLoggedUser(), usc);
        billItem.setTransUserStock(us);

//        calculateAllRates();
        calTotal();

        clearBillItem();
        setActiveIndex(1);
        errorMessage = "";
        replaceableStocks = new ArrayList<>();
        itemsWithoutStocks = new ArrayList<>();
    }

    private void calTotal() {

// Start debugging
        // Start debugging
        // Start debugging
        // Start debugging
        // Reset total
        getPreBill().setTotal(0);

        // Local counters
        double netTot = 0.0;
        double discount = 0.0;
        double grossTot = 0.0;
        double marginTotal = 0.0;
        int index = 0;

        // Optional: see how many BillItems we're about to process
        if (getPreBill().getBillItems() != null) {
        } else {
            return; // Possibly return here if no items
        }

        for (BillItem b : getPreBill().getBillItems()) {

            // Check if retired
            if (b.isRetired()) {
                continue;
            }

            // Set serial number
            b.setSearialNo(index++);

            // Accumulate totals
            netTot += b.getNetValue();
            grossTot += b.getGrossValue();
            discount += b.getDiscount();
            marginTotal += b.getMarginValue();

            // Add the netValue to the Bill's total
            getPreBill().setTotal(getPreBill().getTotal() + b.getNetValue());
        }
        // Show intermediate totals
        // Show intermediate totals

        // Now set values back on the Bill
        getPreBill().setNetTotal(netTot);
        getPreBill().setTotal(grossTot);
        getPreBill().setGrantTotal(grossTot);
        getPreBill().setDiscount(discount);
        getPreBill().setMargin(marginTotal);
        // Final debug
        // Final debug

    }

    @EJB
    private StockHistoryFacade stockHistoryFacade;

    public void addBillItemNew() {
        errorMessage = null;

        billItem = new BillItem();
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        if (getTmpStock() == null) {
            errorMessage = "Item?";
            JsfUtil.addErrorMessage("Item?");
            return;
        }
        if (getTmpStock().getItemBatch().getDateOfExpire().before(CommonController.getCurrentDateTime())) {
            JsfUtil.addErrorMessage("Please not select Expired Items");
            return;
        }
        if (getQty() == null) {
            errorMessage = "Quantity?";
            JsfUtil.addErrorMessage("Quantity?");
            return;
        }
        if (getQty() == 0.0) {
            errorMessage = "Quantity Zero?";
            JsfUtil.addErrorMessage("Quentity Zero?");
            return;
        }
        if (getQty() > getTmpStock().getStock()) {
            errorMessage = "No sufficient stocks.";
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return;
        }

//        if (checkItemBatch()) {
//            errorMessage = "This batch is already there in the bill.";
//            UtilityController.addErrorMessage("Already added this item batch");
//            return;
//        }
//        if (CheckDateAfterOneMonthCurrentDateTime(getStock().getItemBatch().getDateOfExpire())) {
//            errorMessage = "This batch is Expire With in 31 Days.";
//            UtilityController.addErrorMessage("This batch is Expire With in 31 Days.");
//            return;
//        }
        //Checking User Stock Entity
        if (!userStockController.isStockAvailable(getTmpStock(), getQty(), getSessionController().getLoggedUser())) {
            JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
            return;
        }

        billItem.getPharmaceuticalBillItem().setQtyInUnit(qty);
        billItem.getPharmaceuticalBillItem().setStock(getTmpStock());
        billItem.getPharmaceuticalBillItem().setItemBatch(getTmpStock().getItemBatch());

//        calculateBillItem();
        ////System.out.println("Rate*****" + billItem.getRate());
        billItem.setInwardChargeType(InwardChargeType.Medicine);

        billItem.setItem(getTmpStock().getItemBatch().getItem());
        billItem.setQty(qty);
//        billItem.setBill(getPreBill());
        billItem.setSearialNo(getBillItems().size() + 1);
        getBillItems().add(billItem);

        qty = null;
        tmpStock = null;
        setActiveIndex(1);
    }

    public void removeBillItemFromBhtRequest(BillItem b) {
        if (b == null) {
            JsfUtil.addErrorMessage("Please selct item");
            return;
        }
        if (getBillItems() == null) {
            JsfUtil.addErrorMessage("No items in the bill");
            return;
        }
        getBillItems().remove(b);

        calTotal();
    }

    public void calculateBillItemListner(AjaxBehaviorEvent event) {
        calculateBillItem();
    }

    public void calculateBillItem() {
        if (stock == null) {
            return;
        }
        if (getPreBill() == null) {
            return;
        }
        if (getBillItem() == null) {
            return;
        }
        if (getBillItem().getPharmaceuticalBillItem() == null) {
            return;
        }
        if (getBillItem().getPharmaceuticalBillItem().getStock() == null) {
            getBillItem().getPharmaceuticalBillItem().setStock(stock);
        }
        if (getQty() == null) {
            qty = 0.0;
        }

        //Bill Item
        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setQty(qty);

        //pharmaceutical Bill Item
        billItem.getPharmaceuticalBillItem().setDoe(getStock().getItemBatch().getDateOfExpire());
        billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);
        billItem.getPharmaceuticalBillItem().setQty(0 - Math.abs(qty));

        calculateRates(billItem);

    }

    public void calculateBillItemForEditing(BillItem bi) {
        if (getPreBill() == null || bi == null || bi.getPharmaceuticalBillItem() == null || bi.getPharmaceuticalBillItem().getStock() == null) {
            return;
        }
        calculateRates(bi);
    }

    public void handleSelect(AjaxBehaviorEvent event) {
        handleSelect();
    }

    public void handleSelect(SelectEvent event) {
        handleSelect();
    }

    public void handleSelect() {
        if (getBillItem() == null) {
            return;
        }
        if (getBillItem().getPharmaceuticalBillItem() == null) {
            return;
        }
        if (stock == null) {
            return;
        }
        getBillItem().getPharmaceuticalBillItem().setStock(stock);
        if (getBillItem().getPharmaceuticalBillItem().getStock() == null) {
            return;
        }
        if (getBillItem().getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
            return;
        }
        if (getBillItem().getPharmaceuticalBillItem().getStock().getItemBatch().getItem() == null) {
            return;
        }
        getBillItem().setItem(getBillItem().getPharmaceuticalBillItem().getStock().getItemBatch().getItem());
        calculateRates(getBillItem());
    }

    public void paymentSchemeChanged(AjaxBehaviorEvent ajaxBehavior) {
        calculateAllRates();
    }

    @Deprecated
    public void calculateAllRates() {
        for (BillItem tbi : getPreBill().getBillItems()) {
            calculateRates(tbi);
            calculateBillItemForEditing(tbi);
        }
        calTotal();
    }

    public void calculateRateListner(AjaxBehaviorEvent event) {

    }

    public void calculateRates(BillItem bi) {
        if (bi == null) {
            return;
        }
        if (bi.getPharmaceuticalBillItem() == null) {
            return;
        }
        if (bi.getPharmaceuticalBillItem().getStock() == null) {
            return;
        }

        double originalRate;
        double estimatedValueBeforeAddingMarginToCalculateMatrix;
        double marginPercentage;

        double marginRate;
        double marginValue;
        double quantity;
        double grossValue;
        double netValue;

        Department matrixDept = null;
        boolean matrixByAdmissionDepartment;
        boolean matrixByIssuingDepartment;
        matrixByAdmissionDepartment = configOptionApplicationController.getBooleanValueByKey("Price Matrix is calculated from Inpatient Department for " + sessionController.getDepartment().getName(), true);
        matrixByIssuingDepartment = configOptionApplicationController.getBooleanValueByKey("Price Matrix is calculated from Issuing Department for " + sessionController.getDepartment().getName(), true);
        if (matrixByAdmissionDepartment) {
            if (getPatientEncounter() == null) {
                matrixDept = getSessionController().getDepartment();
            }
            if (getPatientEncounter().getCurrentPatientRoom() == null) {
                matrixDept = getPatientEncounter().getDepartment();
            }
            if (getPatientEncounter().getCurrentPatientRoom() != null) {
                if (getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge() != null) {
                    matrixDept = getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment();
                }
            }

        } else if (matrixByIssuingDepartment) {
            matrixDept = getSessionController().getDepartment();
        } else {
            matrixDept = getSessionController().getDepartment();
        }

        quantity = bi.getQty();
        originalRate = bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate();
        estimatedValueBeforeAddingMarginToCalculateMatrix = originalRate * quantity;

        PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(
                bi,
                estimatedValueBeforeAddingMarginToCalculateMatrix,
                matrixDept,
                getPatientEncounter().getPaymentMethod()
        );
        if (priceMatrix != null) {
            marginPercentage = priceMatrix.getMargin() / 100; // Normalize margin rate
        } else {
            marginPercentage = 0.0;
        }

        marginRate = marginPercentage * originalRate;
        marginValue = marginRate * quantity;
        grossValue = originalRate * quantity;
        netValue = grossValue + marginValue;

        // Update BillItem
        bi.setRate(originalRate);
        bi.setGrossValue(grossValue);
        bi.setMarginValue(marginValue);
        bi.setNetValue(netValue);
        bi.setMarginRate(marginRate);
        bi.setNetRate(originalRate + marginRate);
        bi.setAdjustedValue(netValue); // Assuming AdjustedValue is the same as NetValue here
        bi.setDiscount(0); // Explicitly set to 0 for clarity

    }

    public List<Stock> completeAvailableStocksSelectedPharmacy(String qry) {
        if (department == null) {
            JsfUtil.addErrorMessage("Please Select Depatment");
            return new ArrayList<>();
        }

        String sql;
        Map m = new HashMap();
        m.put("d", department);
        double d = 0.0;
        m.put("s", d);
        m.put("n", "%" + qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n or (i.itemBatch.item.vmp.name) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.vmp.name) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }

        List<Stock> items = getStockFacade().findByJpql(sql, m, 20);

        if (qry.length() > 5 && items.size() == 1) {
            stock = items.get(0);
            replaceableStocks = new ArrayList<>();
            itemsWithoutStocks = new ArrayList<>();
            handleSelectAction();
        } else if (!qry.trim().isEmpty() && qry.length() > 4) {
            itemsWithoutStocks = completeRetailSaleItems(qry, department);
            if (itemsWithoutStocks != null && !itemsWithoutStocks.isEmpty()) {
                fillReplaceableStocksForAmp((Amp) itemsWithoutStocks.get(0));
            }
        }

        return items;
    }

    public String navigateToIssueMedicinesDirectlyForBhtRequest() {
        if (bhtRequestBill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return "";
        }
        generateIssueBillComponentsForBhtRequest(bhtRequestBill);
        return "/ward/ward_pharmacy_bht_issue";
    }

    public void generateIssueBillComponentsForBhtRequest(Bill b) {
        if (b == null) {
            JsfUtil.addErrorMessage("No bill");
            return;
        }
        if (b.getBillItems() == null) {
            JsfUtil.addErrorMessage("Bill Items Null");
            return;
        }
        if (b.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Items");
            return;
        }

        UserStockContainer usc = userStockController.saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());

        setPatientEncounter(b.getPatientEncounter());
        billItems = new ArrayList<>();

        for (BillItem i : b.getBillItems()) {
            if (i.getItem() == null) {
                continue;
            }
            if (i.getQty() == null) {
                continue;
            }
            Item item = i.getItem();
            Double requestingQty = i.getQty();

            List<Stock> usedStocks = new ArrayList<>();

            if (item instanceof Amp) {

            } else if (item instanceof Vmp) {

            } else if (item instanceof Ampp) {
                JsfUtil.addErrorMessage("No Supported Yet");
                return;
            } else if (item instanceof Vmpp) {
                JsfUtil.addErrorMessage("No Supported Yet");
                return;
            }

            double billedIssue = getPharmacyCalculation().getBilledInwardPharmacyRequest(i, BillType.PharmacyBhtPre);
            double cancelledIssue = getPharmacyCalculation().getCancelledInwardPharmacyRequest(i, BillType.PharmacyBhtPre);
            double refundedIssue = getPharmacyCalculation().getRefundedInwardPharmacyRequest(i, BillType.PharmacyBhtPre);

            double issuableQty = Math.abs(i.getQty()) - (Math.abs(billedIssue) - (Math.abs(cancelledIssue) + Math.abs(refundedIssue)));

            List<StockQty> stockQtys = pharmacyBean.getStockByQty(i.getItem(), issuableQty, getSessionController().getDepartment());

            if (stockQtys != null && !stockQtys.isEmpty()) {

                for (StockQty sq : stockQtys) {
                    if (sq.getQty() == 0) {
                        continue;
                    }

                    //Checking User Stock Entity
                    if (!userStockController.isStockAvailable(sq.getStock(), sq.getQty(), getSessionController().getLoggedUser())) {
                        JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
                        continue;
                    }
                    billItem = new BillItem();
                    billItem.setPharmaceuticalBillItem(new PharmaceuticalBillItem());
                    billItem.getPharmaceuticalBillItem().setQtyInUnit(sq.getQty());
//                billItem.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - sq.getQty()));
                    billItem.getPharmaceuticalBillItem().setStock(sq.getStock());
                    billItem.getPharmaceuticalBillItem().setItemBatch(sq.getStock().getItemBatch());

                    billItem.setItem(sq.getStock().getItemBatch().getItem());
                    billItem.setQty(sq.getQty());
                    billItem.setDescreption(i.getDescreption());

                    billItem.getPharmaceuticalBillItem().setDoe(sq.getStock().getItemBatch().getDateOfExpire());
                    billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
                    billItem.getPharmaceuticalBillItem().setItemBatch(sq.getStock().getItemBatch());
                    billItem.getPharmaceuticalBillItem().setQtyInUnit(sq.getQty());
//                billItem.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - sq.getQty()));
//                billItem.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - sq.getQty()));
//                billItem.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - sq.getQty()));
//                billItem.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - sq.getQty()));
                    billItem.setGrossValue(sq.getStock().getItemBatch().getRetailsaleRate() * sq.getQty());
                    billItem.setNetValue(sq.getQty() * sq.getStock().getItemBatch().getRetailsaleRate());

                    billItem.setInwardChargeType(InwardChargeType.Medicine);
                    billItem.getPharmaceuticalBillItem().setBillItem(billItem);
                    billItem.setItem(sq.getStock().getItemBatch().getItem());
                    billItem.setReferanceBillItem(i);
                    billItem.setSearialNo(getBillItems().size() + 1);
                    billItems.add(billItem);

                }
            } else {
                billItem = new BillItem();
                billItem.setPharmaceuticalBillItem(new PharmaceuticalBillItem());
                billItem.getPharmaceuticalBillItem().setQtyInUnit(issuableQty);
                billItem.getPharmaceuticalBillItem().setStock(null);
                billItem.getPharmaceuticalBillItem().setItemBatch(null);
                billItem.setItem(i.getItem());
                billItem.setQty(issuableQty);
                billItem.setDescreption(i.getDescreption());
                billItem.setInwardChargeType(InwardChargeType.Medicine);
                billItem.setReferanceBillItem(i);
                billItem.setSearialNo(getBillItems().size() + 1);
                billItem.getPharmaceuticalBillItem().setBillItem(billItem);
                billItems.add(billItem);
            }

        }

        getPreBill().setBillItems(billItems);

//        boolean flag = false;
//        for (BillItem bi : getBillItems()) {
//            if (Objects.equals(bi.getPharmaceuticalBillItem().getStock().getId(), stock.getId())) {
//                flag = true;
//                break;
//            }
//            stock = bi.getPharmaceuticalBillItem().getStock();
//        }
//
//        if (flag) {
//            billItems = null;
//            JsfUtil.addErrorMessage("There is Some Item in request that are added Multiple Time in Transfer request!!! please check request you can't issue errornus transfer request");
//        }
    }

    public boolean checkBillComponent(Bill b) {

        boolean flag = false;
        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(b)) {
//            //// // System.out.println("i.getQtyInUnit() = " + i.getQtyInUnit());
            double billedIssue = getPharmacyCalculation().getBilledInwardPharmacyRequest(i.getBillItem(), BillType.PharmacyBhtPre);
//            //// // System.out.println("billedIssue = " + billedIssue);
            double cancelledIssue = getPharmacyCalculation().getCancelledInwardPharmacyRequest(i.getBillItem(), BillType.PharmacyBhtPre);
//            //// // System.out.println("cancelledIssue = " + cancelledIssue);
            double refundedIssue = getPharmacyCalculation().getRefundedInwardPharmacyRequest(i.getBillItem(), BillType.PharmacyBhtPre);
//            //// // System.out.println("refundedIssue = " + refundedIssue);

            double issuableQty = Math.abs(i.getQtyInUnit()) - (Math.abs(billedIssue) - (Math.abs(cancelledIssue) + Math.abs(refundedIssue)));
            if (issuableQty > 0) {
                flag = true;
            }

        }

        return flag;

    }

    public void handleSelectAction() {
        if (stock == null) {
            //////// // System.out.println("Stock NOT selected.");
        }
        if (getBillItem() == null || getBillItem().getPharmaceuticalBillItem() == null) {
            //////// // System.out.println("Internal Error at PharmacySaleController.java > handleSelectAction");
        }

        getBillItem().getPharmaceuticalBillItem().setStock(stock);
        calculateRates(billItem);
        if (stock != null && stock.getItemBatch() != null) {
            fillReplaceableStocksForAmp((Amp) stock.getItemBatch().getItem());
        }
    }

    private void clearBill() {
        preBill = null;
        userStockContainer = null;
    }

    private void clearBillItem() {
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
    }

    public boolean CheckDateAfterOneMonthCurrentDateTime(Date date) {
        Calendar calDateOfExpiry = Calendar.getInstance();
        calDateOfExpiry.setTime(CommonFunctionsController.getEndOfDay(date));
        Calendar cal = Calendar.getInstance();
        cal.setTime(CommonFunctionsController.getEndOfDay(new Date()));
        cal.add(Calendar.DATE, 31);
        if (cal.getTimeInMillis() <= calDateOfExpiry.getTimeInMillis()) {
            return false;
        } else {
            return true;
        }
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

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
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

    public PreBill getPreBill() {
        if (preBill == null) {
            preBill = new PreBill();
            // preBill.setPaymentScheme(getPaymentSchemeController().getItems().get(0));
        }
        return preBill;
    }

    public void setPreBill(PreBill preBill) {
        this.preBill = preBill;
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

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public boolean isBillPreview() {
        return billPreview;
    }

    public void setBillPreview(boolean billPreview) {
        this.billPreview = billPreview;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }

    public Bill getPrintBill() {
        return printBill;
    }

    public void setPrintBill(Bill printBill) {
        this.printBill = printBill;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return PaymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController PaymentSchemeController) {
        this.PaymentSchemeController = PaymentSchemeController;
    }

    public StockHistoryFacade getStockHistoryFacade() {
        return stockHistoryFacade;
    }

    public void setStockHistoryFacade(StockHistoryFacade stockHistoryFacade) {
        this.stockHistoryFacade = stockHistoryFacade;
    }

    public UserStockContainer getUserStockContainer() {
        if (userStockContainer == null) {
            userStockContainer = new UserStockContainer();
        }
        return userStockContainer;
    }

    public void setUserStockContainer(UserStockContainer userStockContainer) {
        this.userStockContainer = userStockContainer;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public Bill getBatchBill() {
        return batchBill;
    }

    public void setBatchBill(Bill batchBill) {
        this.batchBill = batchBill;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public PharmacyCalculation getPharmacyCalculation() {
        return pharmacyCalculation;
    }

    public void setPharmacyCalculation(PharmacyCalculation pharmacyCalculation) {
        this.pharmacyCalculation = pharmacyCalculation;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public Stock getReplacableStock() {
        return replacableStock;
    }

    public void setReplacableStock(Stock replacableStock) {
        this.replacableStock = replacableStock;
    }

    public Item getSelectedAvailableAmp() {
        return selectedAvailableAmp;
    }

    public void setSelectedAvailableAmp(Item selectedAvailableAmp) {
        this.selectedAvailableAmp = selectedAvailableAmp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Bill getBhtRequestBill() {
        return bhtRequestBill;
    }

    public void setBhtRequestBill(Bill bhtRequestBill) {
        this.bhtRequestBill = bhtRequestBill;
    }

    public Stock getTmpStock() {
        return tmpStock;
    }

    public void setTmpStock(Stock tmpStock) {
        this.tmpStock = tmpStock;
    }

}
