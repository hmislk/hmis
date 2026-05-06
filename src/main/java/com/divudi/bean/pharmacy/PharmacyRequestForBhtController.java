/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.NotificationController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.ConfigOptionApplicationController;

import com.divudi.core.util.JsfUtil;
import com.divudi.bean.inward.InwardBeanController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Sex;
import com.divudi.core.data.StockQty;
import com.divudi.core.data.Title;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.data.inward.SurgeryBillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.ejb.PharmacyService;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.UserStockContainer;
import com.divudi.core.entity.clinical.ClinicalFindingValue;
import com.divudi.core.entity.clinical.Prescription;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.PrescriptionFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.StockHistoryFacade;
import java.io.Serializable;
import java.util.*;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;

import com.divudi.core.util.CommonFunctions;
import com.divudi.service.BillService;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

@Named
@SessionScoped
public class PharmacyRequestForBhtController implements Serializable {

    /**
     * Creates a new instance of PharmacySaleController
     */
    public PharmacyRequestForBhtController() {
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
    ConfigOptionApplicationController configOptionApplicationController;

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
    private PrescriptionFacade prescriptionFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    com.divudi.ejb.PrescriptionService prescriptionService;
    @EJB
    com.divudi.ejb.PrescriptionToItemService prescriptionToItemService;
    @EJB
    private PharmacyService pharmacyService;
/////////////////////////
    Item selectedAlternative;
    private PreBill preBill;
    Bill printBill;
    Bill bill;
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

    private List<ClinicalFindingValue> allergyListOfPatient;

    private Bill batchBill;
    @Inject
    private BillBeanController billBean;
    @Inject
    NotificationController notificationController;
    @Inject
    MeasurementUnitController measurementUnitController;
    @EJB
    BillService billService;
    private String comment;

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

        settleBhtIssue(BillType.PharmacyBhtPre, getBatchBill().getFromDepartment(), BillNumberSuffix.PHISSUE);

        getBillBean().saveEncounterComponents(getPrintBill(), getBatchBill(), getSessionController().getLoggedUser());
        getBillBean().updateBatchBill(getBatchBill());

    }

    public void settleSurgeryBhtIssueStore() {
        if (getBatchBill() == null) {
            return;
        }

        if (getBatchBill().getProcedure() == null) {
            return;
        }

        settleBhtIssue(BillType.StoreBhtPre, getBatchBill().getFromDepartment(), BillNumberSuffix.PHISSUE);

        getBillBean().saveEncounterComponents(getPrintBill(), getBatchBill(), getSessionController().getLoggedUser());
        getBillBean().updateBatchBill(getBatchBill());

    }

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

    public void onEdit(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
        onEdit(tmp);
    }

    private void setZeroToQty(BillItem tmp) {
        tmp.setQty(0.0);
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0.0f);

        userStockController.updateUserStock(tmp.getTransUserStock(), 0);
    }

    //Check when edititng Qty
    //
    @Deprecated
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
            setZeroToQty(tmp);
            onEditCalculation(tmp);

            JsfUtil.addErrorMessage("No Sufficient Stocks?");
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
        tmp.setGrossValue(tmp.getQty() * tmp.getRate());
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0 - tmp.getQty());

        calculateBillItemForEditing(tmp);

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
        setStock(replacableStock);
        getBillItem().getPharmaceuticalBillItem().setStock(getStock());
        calculateRates(billItem);
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
        if (billItem.getPrescription() == null) {
            billItem.setPrescription(new Prescription());
        }
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    private void savePreBillFinally(Patient pt, Department matrixDepartment, BillType billType, BillNumberSuffix billNumberSuffix) {
        getPreBill().setBillType(billType);
        getPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), billType, BillClassType.PreBill, billNumberSuffix));
        getPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), billType, BillClassType.PreBill, billNumberSuffix));

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

    private void savePreBillFinallyPreservingToDepartment(Patient pt, Department matrixDepartment, BillType billType, BillNumberSuffix billNumberSuffix) {
        // Store the current toDepartment and toInstitution before updating
        Department currentToDepartment = getPreBill().getToDepartment();
        Institution currentToInstitution = getPreBill().getToInstitution();

        getPreBill().setBillType(billType);
        getPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), billType, BillClassType.PreBill, billNumberSuffix));
        getPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), billType, BillClassType.PreBill, billNumberSuffix));

        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getPreBill().setCreater(getSessionController().getLoggedUser());

        getPreBill().setPatient(pt);
        getPreBill().setPatientEncounter(getPatientEncounter());

        // Preserve the existing toDepartment and toInstitution instead of nullifying them
        getPreBill().setToDepartment(currentToDepartment);
        getPreBill().setToInstitution(currentToInstitution);
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
            if (onEdit(tbi)) {//If any issue in Stock Bill Item will not save & not include for total
                continue;
            }

            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());

            tbi.setCreatedAt(Calendar.getInstance().getTime());
            tbi.setCreater(getSessionController().getLoggedUser());

            // Set prescription metadata if prescription exists
            if (tbi.getPrescription() != null && tbi.getPrescription().getId() == null) {
                tbi.getPrescription().setCreatedAt(Calendar.getInstance().getTime());
                tbi.getPrescription().setCreater(getSessionController().getLoggedUser());
            }

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

            double qtyL = tbi.getPharmaceuticalBillItem().getQtyInUnit() + tbi.getPharmaceuticalBillItem().getFreeQtyInUnit();

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

        calculateAllRates();

        getBillFacade().edit(getPreBill());
    }

    @Deprecated
    private void savePreBillItemsFinallyRequestOld(List<BillItem> list) {
        List<BillItem> itemsToAdd = new ArrayList<>();
        for (BillItem tbi : list) {
//            if (onEdit(tbi)) {//If any issue in Stock Bill Item will not save & not include for total
//                continue;
//            }

            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());
            tbi.setCreatedAt(Calendar.getInstance().getTime());
            tbi.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem tmpPh = tbi.getPharmaceuticalBillItem();
            tbi.setPharmaceuticalBillItem(null);

            if (tbi.getId() == null) {
                getBillItemFacade().create(tbi);
            } else {
                getBillItemFacade().edit(tbi);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            } else {
                getPharmaceuticalBillItemFacade().edit(tmpPh);
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
            itemsToAdd.add(tbi);
        }
        getPreBill().getBillItems().addAll(itemsToAdd);

        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());

        calculateAllRates();

        getBillFacade().edit(getPreBill());
    }

    private void savePreBillItemsFinallyRequest(List<BillItem> list) {
        List<BillItem> itemsToAdd = new ArrayList<>();
        List<BillItem> existingItems = getPreBill().getBillItems(); // Existing items in the bill

        for (BillItem tbi : list) {
//        if (onEdit(tbi)) { // If any issue in Stock Bill Item, it will not save & not include for total
//            continue;
//        }

            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());
            tbi.setCreatedAt(Calendar.getInstance().getTime());
            tbi.setCreater(getSessionController().getLoggedUser());

            // Set prescription metadata if prescription exists
            if (tbi.getPrescription() != null) {
                if (tbi.getPrescription().getId() == null) {
                    tbi.getPrescription().setCreatedAt(Calendar.getInstance().getTime());
                    tbi.getPrescription().setCreater(getSessionController().getLoggedUser());
                } else {
                    tbi.getPrescription().setEditedAt(Calendar.getInstance().getTime());
                    tbi.getPrescription().setEditer(getSessionController().getLoggedUser());
                }
            }

            PharmaceuticalBillItem tmpPh = tbi.getPharmaceuticalBillItem();
            tbi.setPharmaceuticalBillItem(null);

            if (tbi.getId() == null) {
                getBillItemFacade().create(tbi);
            } else {
                getBillItemFacade().edit(tbi);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            } else {
                getPharmaceuticalBillItemFacade().edit(tmpPh);
            }

            tbi.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(tbi);
            tbi.getPharmaceuticalBillItem().setBillItem(tbi);
            getPharmaceuticalBillItemFacade().edit(tbi.getPharmaceuticalBillItem());

            // Add the item to itemsToAdd only if it is not already in the existing items
            if (!existingItems.contains(tbi)) {
                itemsToAdd.add(tbi);
            }
        }

        // Ensure only new items are added to the bill items
        getPreBill().getBillItems().addAll(itemsToAdd);

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

        return false;

    }

    public void settlePharmacyBhtIssue() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please add items to the bill.");
            return;
        }

        if (errorCheck()) {
            return;
        }
        if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
            Patient p = getPatientEncounter().getPatient();
            if (allergyListOfPatient == null) {
                allergyListOfPatient = pharmacyService.getAllergyListForPatient(p);
            }
            String allergyMsg = pharmacyService.isAllergyForPatient(p, getPreBill().getBillItems(), allergyListOfPatient);
            if (!allergyMsg.isEmpty()) {
                JsfUtil.addErrorMessage(allergyMsg);
                return;
            }
        }
        settleBhtIssue(BillType.PharmacyBhtPre, getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment(), BillNumberSuffix.PHISSUE);

    }

    public void settlePharmacyBhtIssueAccept() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;
        if (errorCheck()) {
            return;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
            Patient p = getPatientEncounter().getPatient();
            if (allergyListOfPatient == null) {
                allergyListOfPatient = pharmacyService.getAllergyListForPatient(p);
            }
            String allergyMsg = pharmacyService.isAllergyForPatient(p, getPreBill().getBillItems(), allergyListOfPatient);
            if (!allergyMsg.isEmpty()) {
                JsfUtil.addErrorMessage(allergyMsg);
                return;
            }
        }

        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Nothing To Settle.");
            return;
        }

        settleBhtIssueRequestAccept(BillType.PharmacyBhtPre, getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment(), BillNumberSuffix.PHISSUE);

    }

    @Deprecated // Use settleBhtRequest
    public void settlePharmacyBhtIssueRequest() {
        if (errorCheck()) {
            return;
        }
        if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
            Patient p = getPatientEncounter().getPatient();
            if (allergyListOfPatient == null) {
                allergyListOfPatient = pharmacyService.getAllergyListForPatient(p);
            }
            String allergyMsg = pharmacyService.isAllergyForPatient(p, getPreBill().getBillItems(), allergyListOfPatient);
            if (!allergyMsg.isEmpty()) {
                JsfUtil.addErrorMessage(allergyMsg);
                return;
            }
        }
        BillTypeAtomic bta = BillTypeAtomic.REQUEST_MEDICINE_INWARD;
        BillType bt = BillType.InwardPharmacyRequest;
        settleBhtIssueRequest(bt, bta, getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment(), BillNumberSuffix.PHISSUEREQ);
    }

    public void settleBhtRequest() {
        if (getPatientEncounter() == null || getPatientEncounter().getPatient() == null) {
            JsfUtil.addErrorMessage("Please Select a BHT");
            return;
        }

        if (getPatientEncounter().getCurrentPatientRoom() == null) {
            JsfUtil.addErrorMessage("Please Select Patient Room");
            return;
        }

        if (getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge() == null) {
            JsfUtil.addErrorMessage("Please Set Room");
            return;
        }

        if (getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage("Sorry Patient is Discharged!!!");
            return;
        }

        if (getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Sorry this BHT was Settled !!!");
            return;
        }

        if (getPreBill().getBillItems() == null) {
            return;
        }
        if (getPreBill().getBillItems().isEmpty()) {
            return;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
            Patient p = getPatientEncounter().getPatient();
            if (allergyListOfPatient == null) {
                allergyListOfPatient = pharmacyService.getAllergyListForPatient(p);
            }
            String allergyMsg = pharmacyService.isAllergyForPatient(p, getPreBill().getBillItems(), allergyListOfPatient);
            if (!allergyMsg.isEmpty()) {
                JsfUtil.addErrorMessage(allergyMsg);
                return;
            }
        }

        BillTypeAtomic bta = BillTypeAtomic.REQUEST_MEDICINE_INWARD;
        BillType bt = BillType.InwardPharmacyRequest;
        Patient pt = getPatientEncounter().getPatient();
        getPreBill().setPaidAmount(0);
        // From: ward (patient's current room department)
        Department fromDept = getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment();

        getPreBill().setDepartment(sessionController.getDepartment());
        getPreBill().setInstitution(sessionController.getInstitution());

        getPreBill().setFromDepartment(fromDept);
        getPreBill().setFromInstitution(fromDept.getInstitution());
        // To: selected department (Pharmacy)
        getPreBill().setToDepartment(department);
        getPreBill().setToInstitution(department.getInstitution());
        getPreBill().setPatientEncounter(patientEncounter);
        getPreBill().setBillTypeAtomic(bta);
        getPreBill().setBillType(bt);
        getPreBill().setComments(comment);
        String deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), bta);
        getPreBill().setDeptId(deptId);
        getPreBill().setInsId(deptId);
        if (getPreBill().getId() == null) {
            getPreBill().setCreatedAt(new Date());
            getPreBill().setCreater(sessionController.getLoggedUser());
//            getPreBill().setCompleted(true);
//            getPreBill().setCompletedAt(new Date());
//            getPreBill().setCompletedBy(sessionController.getLoggedUser());
            billFacade.create(getPreBill());
        } else {
//            getPreBill().setCompleted(true);
//            getPreBill().setCompletedAt(new Date());
//            getPreBill().setCompletedBy(sessionController.getLoggedUser());
            billFacade.edit(getPreBill());
        }
        for (BillItem savingBillItem : getPreBill().getBillItems()) {
            savingBillItem.setBill(getPreBill());
            if (savingBillItem.getId() == null) {
                savingBillItem.setCreatedAt(new Date());
                savingBillItem.setCreater(sessionController.getLoggedUser());
                billItemFacade.create(savingBillItem);
            } else {
                billItemFacade.edit(savingBillItem);
            }
        }
        setPrintBill(billService.reloadBill(getPreBill()));
        notificationController.createNotification(getPrintBill());
        clearBill();
        clearBillItem();
        comment = "";
        billPreview = true;
    }

    public void settleStoreBhtIssue() {
        if (errorCheck()) {
            return;
        }
        settleBhtIssue(BillType.StoreBhtPre, getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment(), BillNumberSuffix.PHISSUE);
    }

    private boolean errorCheck() {
        if (getPatientEncounter() == null || getPatientEncounter().getPatient() == null) {
            JsfUtil.addErrorMessage("Please Select a BHT");
            return true;
        }

        if (getPatientEncounter().getCurrentPatientRoom() == null) {
            JsfUtil.addErrorMessage("Please Select Patient Room");
            return true;
        }

        if (getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge() == null) {
            JsfUtil.addErrorMessage("Please Set Room");
            return true;
        }

        if (getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage("Sorry Patient is Discharged!!!");
            return true;
        }

        if (getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Sorry this BHT was Settled !!!");
            return true;
        }

        if (checkAllBillItem()) {
            //  JsfUtil.addErrorMessage("Please Set Room 33");
            return true;
        }

        return false;
    }

    private void settleBhtIssue(BillType btp, Department matrixDepartment, BillNumberSuffix billNumberSuffix) {

        if (matrixDepartment == null) {
            JsfUtil.addErrorMessage("This Bht can't issue as this Surgery Has No Department");
            return;
        }

        Patient pt = getPatientEncounter().getPatient();
        getPreBill().setPaidAmount(0);

        List<BillItem> tmpBillItems = getPreBill().getBillItems();
        getPreBill().getBillItems().clear();

        savePreBillFinally(pt, matrixDepartment, btp, billNumberSuffix);
        savePreBillItemsFinally(tmpBillItems);

        // Calculation Margin
        updateMargin(getPreBill().getBillItems(), getPreBill(), getPreBill().getFromDepartment(), getPatientEncounter().getPaymentMethod());

        setPrintBill(getBillFacade().find(getPreBill().getId()));

        clearBill();
        clearBillItem();
        billPreview = true;

    }

    public void settleEditedPharmacyBhtIssueRequest() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;
        if (errorCheck()) {
            return;
        }
        settleEditedBhtIssueRequest(BillType.InwardPharmacyRequest, getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment(), BillNumberSuffix.PHISSUEREQ);
    }

    private void settleEditedBhtIssueRequest(BillType btp, Department matrixDepartment, BillNumberSuffix billNumberSuffix) {

        if (matrixDepartment == null) {
            JsfUtil.addErrorMessage("This Bht can't issue as this Surgery Has No Department");
            return;
        }

        Patient pt = getPatientEncounter().getPatient();
        getPreBill().setPaidAmount(0);

        List<BillItem> tmpBillItems = new ArrayList<>(getPreBill().getBillItems());

        savePreBillItemsFinallyRequest(tmpBillItems);

        // Calculation Margin
        updateMargin(getPreBill().getBillItems(), getPreBill(), getPreBill().getFromDepartment(), getPatientEncounter().getPaymentMethod());
        setPrintBill(getBillFacade().find(getPreBill().getId()));
        Bill bill = getBillFacade().find(getPreBill().getId());
        bill.setBillTypeAtomic(BillTypeAtomic.REQUEST_MEDICINE_INWARD);
        bill.setEditedAt(new Date());
        bill.setEditor(getSessionController().getLoggedUser());
        billFacade.edit(bill);
        notificationController.createNotification(bill);
        clearBill();
        clearBillItem();
        billPreview = true;

    }

    @Deprecated
    private void settleBhtIssueRequest(BillType bt, BillTypeAtomic bta, Department matrixDepartment, BillNumberSuffix billNumberSuffix) {
        if (matrixDepartment == null) {
            JsfUtil.addErrorMessage("This Bht can't issue as this Surgery Has No Department");
            return;
        }

        Patient pt = getPatientEncounter().getPatient();
        getPreBill().setPaidAmount(0);

        List<BillItem> tmpBillItems = getPreBill().getBillItems();
        getPreBill().getBillItems().clear();

        savePreBillFinallyRequest(pt, matrixDepartment, bt, billNumberSuffix);
        savePreBillItemsFinallyRequest(tmpBillItems);

        // Calculation Margin
        updateMargin(getPreBill().getBillItems(), getPreBill(), getPreBill().getFromDepartment(), getPatientEncounter().getPaymentMethod());
        setPrintBill(getBillFacade().find(getPreBill().getId()));
        Bill bill = getBillFacade().find(getPreBill().getId());
        bill.setBillTypeAtomic(bta);
        bill.setBillType(bt);
        bill.setComments(comment);

        billFacade.edit(bill);
        notificationController.createNotification(bill);
        clearBill();
        clearBillItem();
        comment = "";
        billPreview = true;

    }

    private void settleBhtIssueRequestAccept(BillType btp, Department matrixDepartment, BillNumberSuffix billNumberSuffix) {

        if (matrixDepartment == null) {
            JsfUtil.addErrorMessage("This Bht can't issue as this Surgery Has No Department");
            return;
        }

        Patient pt = getPatientEncounter().getPatient();
        getPreBill().setPaidAmount(0);

        List<BillItem> tmpBillItems = new ArrayList<>(getPreBill().getBillItems());
        getPreBill().getBillItems().clear();

        if (!tmpBillItems.isEmpty()) {
            getPreBill().setReferenceBill(tmpBillItems.get(0).getReferanceBillItem().getBill());
        }

        savePreBillFinallyPreservingToDepartment(pt, matrixDepartment, btp, billNumberSuffix);
        savePreBillItemsFinally(tmpBillItems);

        // Calculation Margin
        updateMargin(getPreBill().getBillItems(), getPreBill(), getPreBill().getFromDepartment(), getPatientEncounter().getPaymentMethod());

        setPrintBill(getBillFacade().find(getPreBill().getId()));

        clearBill();
        clearBillItem();
        billPreview = true;

    }

    public void updateMargin(List<BillItem> billItems, Bill bill, Department matrixDepartment, PaymentMethod paymentMethod) {
        double total = 0;
        double netTotal = 0;
        double marginTotal = 0;
        for (BillItem bi : billItems) {

            double rate = Math.abs(bi.getRate());
            double margin = 0;

            PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(bi, rate, matrixDepartment, paymentMethod);

            if (priceMatrix != null) {
                margin = ((bi.getGrossValue() * priceMatrix.getMargin()) / 100);
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

        if (billItem == null) {
            return;
        }

        if (billItem.getItem() == null) {
            JsfUtil.addErrorMessage("Item?");
            return;
        }

        if (getQty() == null) {
            errorMessage = "Quantity?";
            JsfUtil.addErrorMessage("Quantity?");
            return;
        }

        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No patient Select.");
            return;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {

            List<ClinicalFindingValue> allergyListOfPatient = pharmacyService.getAllergyListForPatient(patientEncounter.getPatient());
            List<BillItem> billItems = new ArrayList<>();
            billItems.add(billItem);

            if (allergyListOfPatient != null && !allergyListOfPatient.isEmpty()) {
                String allergyMsg = pharmacyService.isAllergyForPatient(patientEncounter.getPatient(), billItems, allergyListOfPatient);

                if (!allergyMsg.isEmpty()) {
                    JsfUtil.addErrorMessage(allergyMsg);
                    return;
                }
            }

        }

        // Create a new billItem for the collection to avoid entity state issues
        BillItem newBillItem = new BillItem();
        newBillItem.setItem(billItem.getItem());
        newBillItem.setQty(getQty());
        newBillItem.setInwardChargeType(InwardChargeType.Medicine);
        newBillItem.setBill(getPreBill());

        // Handle prescription only if prescription data is available
        boolean hasPrescriptionData = hasMeaningfulPrescriptionData(billItem.getPrescription());

        if (hasPrescriptionData) {
            // Create a detached prescription instance for in-memory use only
            // This will be persisted later during settle operations
            Prescription inMemoryPrescription = new Prescription();
            inMemoryPrescription.setItem(billItem.getItem());
            inMemoryPrescription.setDose(billItem.getPrescription().getDose());
            inMemoryPrescription.setDoseUnit(billItem.getPrescription().getDoseUnit());
            inMemoryPrescription.setFrequencyUnit(billItem.getPrescription().getFrequencyUnit());
            inMemoryPrescription.setDuration(billItem.getPrescription().getDuration());
            inMemoryPrescription.setDurationUnit(billItem.getPrescription().getDurationUnit());
            inMemoryPrescription.setPrescribedFrom(billItem.getPrescription().getPrescribedFrom());
            inMemoryPrescription.setPrescribedTo(billItem.getPrescription().getPrescribedTo());
            inMemoryPrescription.setComment(billItem.getPrescription().getComment());
            inMemoryPrescription.setPatient(getPatientEncounter().getPatient());
            inMemoryPrescription.setEncounter(getPatientEncounter());
            inMemoryPrescription.setIndoor(true);

            // Attach prescription to bill item for later persistence (but don't persist now)
            newBillItem.setPrescription(inMemoryPrescription);

            // Compute description from in-memory prescription object
            String prescriptionText = inMemoryPrescription.getFormattedPrescriptionWithoutIndoorOutdoor();
            if (inMemoryPrescription.getComment() != null && !inMemoryPrescription.getComment().trim().isEmpty()) {
                prescriptionText += " - " + inMemoryPrescription.getComment();
            }
            newBillItem.setDescreption(prescriptionText);
        } else {
            // No meaningful prescription data, use simple description
            newBillItem.setDescreption(billItem.getItem().getName() + " - Qty: " + getQty());
        }

        // Create pharmaceutical bill item with quantity
        PharmaceuticalBillItem pharmaceuticalBillItem = new PharmaceuticalBillItem();
        pharmaceuticalBillItem.setQty(-getQty()); // Negative quantity for requests
        pharmaceuticalBillItem.setBillItem(newBillItem);
        newBillItem.setPharmaceuticalBillItem(pharmaceuticalBillItem);

        if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
            Patient p = getPatientEncounter() != null ? getPatientEncounter().getPatient() : null;
            if (p != null) {
                if (allergyListOfPatient == null) {
                    allergyListOfPatient = pharmacyService.getAllergyListForPatient(p);
                }
                String allergyMsg = pharmacyService.getAllergyMessageForPatient(p, newBillItem, allergyListOfPatient);
                if (!allergyMsg.isEmpty()) {
                    JsfUtil.addErrorMessage(allergyMsg);
                    return;
                }
            }
        }

        newBillItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(newBillItem);

        clearBillItem();
        setActiveIndex(1);
        errorMessage = "";
        replaceableStocks = new ArrayList<>();
        itemsWithoutStocks = new ArrayList<>();
    }

    private void calTotal() {
        getPreBill().setTotal(0);
        double netTot = 0.0;
        double discount = 0.0;
        double grossTot = 0.0;
        int index = 0;
        for (BillItem b : getPreBill().getBillItems()) {
            if (b.isRetired()) {
                continue;
            }
            b.setSearialNo(index++);

            netTot = netTot + b.getNetValue();
            grossTot = grossTot + b.getGrossValue();
            discount = discount + b.getDiscount();
            getPreBill().setTotal(getPreBill().getTotal() + b.getNetValue());
        }
        getPreBill().setNetTotal(netTot);
        getPreBill().setTotal(grossTot);
        getPreBill().setGrantTotal(grossTot);
        getPreBill().setDiscount(discount);

    }

    @EJB
    private StockHistoryFacade stockHistoryFacade;

    public void removeBillItem(BillItem b) {
        getPreBill().getBillItems().remove(b);
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
        if (billItem == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            getBillItem().getPharmaceuticalBillItem().setStock(stock);
        }
        if (getQty() == null) {
            qty = 0.0;
        }

        //Bill Item
//        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setQty(qty);

        //pharmaceutical Bill Item
        billItem.getPharmaceuticalBillItem().setDoe(getStock().getItemBatch().getDateOfExpire());
        billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);

        //Rates
        //Values
        billItem.setGrossValue(getStock().getItemBatch().getRetailsaleRate() * qty);
        billItem.setNetValue(qty * billItem.getNetRate());
        billItem.setDiscount(billItem.getGrossValue() - billItem.getNetValue());

    }

    public void calculateBillItemForEditing(BillItem bi) {
        if (getPreBill() == null || bi == null || bi.getPharmaceuticalBillItem() == null || bi.getPharmaceuticalBillItem().getStock() == null) {
            return;
        }
        bi.setGrossValue(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate() * bi.getQty());
        bi.setNetValue(bi.getQty() * bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
        bi.setDiscount(bi.getGrossValue() - bi.getNetValue());

    }

    public void handleSelect(SelectEvent event) {
        getBillItem().getPharmaceuticalBillItem().setStock(stock);
        calculateRates(billItem);
    }

    public void handleMedicineSelect(SelectEvent event) {
        if (billItem != null && billItem.getPrescription() != null && billItem.getPrescription().getItem() != null) {
            autoSetDoseUnitForMedicine(billItem.getPrescription().getItem());
        }
    }

    /**
     * Automatically set dose unit for AMP or VMP based on their properties
     */
    private void autoSetDoseUnitForMedicine(Item selectedItem) {
        com.divudi.core.entity.pharmacy.MeasurementUnit preferredDoseUnit = null;

        if (selectedItem instanceof com.divudi.core.entity.pharmacy.Amp) {
            preferredDoseUnit = getPreferredDoseUnitForAmp((com.divudi.core.entity.pharmacy.Amp) selectedItem);
        } else if (selectedItem instanceof com.divudi.core.entity.pharmacy.Vmp) {
            preferredDoseUnit = getPreferredDoseUnitForVmp((com.divudi.core.entity.pharmacy.Vmp) selectedItem);
        }

        // Set the dose unit if found
        if (preferredDoseUnit != null && billItem.getPrescription() != null) {
            billItem.getPrescription().setDoseUnit(preferredDoseUnit);
        }
    }

    /**
     * Get preferred dose unit for AMP Priority order: issueUnit > strengthUnit
     * > VMP issueUnit > VMP strengthUnit
     */
    private com.divudi.core.entity.pharmacy.MeasurementUnit getPreferredDoseUnitForAmp(com.divudi.core.entity.pharmacy.Amp amp) {
        if (amp.getIssueUnit() != null) {
            return amp.getIssueUnit();
        } else if (amp.getStrengthUnit() != null) {
            return amp.getStrengthUnit();
        } else if (amp.getVmp() != null) {
            if (amp.getVmp().getIssueUnit() != null) {
                return amp.getVmp().getIssueUnit();
            } else if (amp.getVmp().getStrengthUnit() != null) {
                return amp.getVmp().getStrengthUnit();
            }
        }
        return null;
    }

    /**
     * Get preferred dose unit for VMP Priority order: issueUnit > strengthUnit
     * > dosage form default
     */
    private com.divudi.core.entity.pharmacy.MeasurementUnit getPreferredDoseUnitForVmp(com.divudi.core.entity.pharmacy.Vmp vmp) {
        if (vmp.getIssueUnit() != null) {
            return vmp.getIssueUnit();
        } else if (vmp.getStrengthUnit() != null) {
            return vmp.getStrengthUnit();
        } else {
            // Try to get default dose unit based on dosage form
            if (vmp.getDosageForm() != null) {
                com.divudi.core.entity.pharmacy.MeasurementUnit defaultUnit = getDefaultDoseUnitForDosageForm(vmp.getDosageForm());
                if (defaultUnit != null) {
                    return defaultUnit;
                }
            }
        }
        return null;
    }

    /**
     * Get default dose unit based on dosage form
     */
    private com.divudi.core.entity.pharmacy.MeasurementUnit getDefaultDoseUnitForDosageForm(com.divudi.core.entity.Category dosageForm) {
        if (dosageForm == null || dosageForm.getName() == null) {
            return null;
        }

        String formName = dosageForm.getName().toLowerCase();

        // Try to get a suitable dose unit from measurement unit controller
        if (measurementUnitController != null) {
            java.util.List<com.divudi.core.entity.pharmacy.MeasurementUnit> doseUnits = measurementUnitController.getDoseUnits();

            // Map common dosage forms to appropriate dose units
            if (formName.contains("tablet") || formName.contains("capsule") || formName.contains("pill")) {
                // For solid forms, look for "tablet", "capsule", or count-based units
                for (com.divudi.core.entity.pharmacy.MeasurementUnit unit : doseUnits) {
                    String unitName = unit.getName().toLowerCase();
                    if (unitName.contains("tablet") || unitName.contains("capsule") || unitName.equals("nos") || unitName.equals("each")) {
                        return unit;
                    }
                }
            } else if (formName.contains("syrup") || formName.contains("liquid") || formName.contains("suspension") || formName.contains("solution")) {
                // For liquid forms, look for "ml", "mL", or volume-based units
                for (com.divudi.core.entity.pharmacy.MeasurementUnit unit : doseUnits) {
                    String unitName = unit.getName().toLowerCase();
                    if (unitName.equals("ml") || unitName.equals("ml") || unitName.contains("milliliter")) {
                        return unit;
                    }
                }
            } else if (formName.contains("cream") || formName.contains("ointment") || formName.contains("gel")) {
                // For topical forms, look for "g", "gram", or weight-based units
                for (com.divudi.core.entity.pharmacy.MeasurementUnit unit : doseUnits) {
                    String unitName = unit.getName().toLowerCase();
                    if (unitName.equals("g") || unitName.equals("gm") || unitName.contains("gram")) {
                        return unit;
                    }
                }
            } else if (formName.contains("injection") || formName.contains("ampoule") || formName.contains("vial")) {
                // For injections, look for "ml", "ampoule", or "vial"
                for (com.divudi.core.entity.pharmacy.MeasurementUnit unit : doseUnits) {
                    String unitName = unit.getName().toLowerCase();
                    if (unitName.equals("ml") || unitName.equals("ml") || unitName.contains("ampoule") || unitName.contains("vial")) {
                        return unit;
                    }
                }
            }
        }

        return null;
    }

    public void paymentSchemeChanged(AjaxBehaviorEvent ajaxBehavior) {
        calculateAllRates();
    }

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
        if (bi.getPharmaceuticalBillItem().getStock() == null) {
            return;
        }
        getBillItem();
        bi.setRate(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
//        bi.setDiscount(calculateBillItemDiscountRate(bi));
        //  //System.err.println("Discount "+bi.getDiscount());
        bi.setNetRate(bi.getRate() - bi.getDiscount());
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

    public void generateBillComponent(Bill b) {

        //User Stock Container Save if New Bill
        UserStockContainer usc = userStockController.saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());
        setPatientEncounter(b.getPatientEncounter());
        billItems = new ArrayList<>();
        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(b)) {
            double billedIssue = getPharmacyCalculation().getBilledInwardPharmacyRequest(i.getBillItem(), BillType.PharmacyBhtPre);
            double cancelledIssue = getPharmacyCalculation().getCancelledInwardPharmacyRequest(i.getBillItem(), BillType.PharmacyBhtPre);
            double refundedIssue = getPharmacyCalculation().getRefundedInwardPharmacyRequest(i.getBillItem(), BillType.PharmacyBhtPre);

            double issuableQty = Math.abs(i.getQtyInUnit()) - (Math.abs(billedIssue) - (Math.abs(cancelledIssue) + Math.abs(refundedIssue)));

            List<StockQty> stockQtys = pharmacyBean.getStockByQty(i.getBillItem().getItem(), issuableQty, getSessionController().getDepartment());

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
                billItem.setDescreption(i.getBillItem().getDescreption());

                billItem.getPharmaceuticalBillItem().setDoe(sq.getStock().getItemBatch().getDateOfExpire());
                billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
                billItem.getPharmaceuticalBillItem().setItemBatch(sq.getStock().getItemBatch());
                billItem.getPharmaceuticalBillItem().setQtyInUnit(sq.getQty());
//                billItem.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - sq.getQty()));

                billItem.setGrossValue(sq.getStock().getItemBatch().getRetailsaleRate() * sq.getQty());
                billItem.setNetValue(sq.getQty() * sq.getStock().getItemBatch().getRetailsaleRate());

                billItem.setInwardChargeType(InwardChargeType.Medicine);

                billItem.setItem(sq.getStock().getItemBatch().getItem());
                billItem.setReferanceBillItem(i.getBillItem());

                billItem.setSearialNo(getBillItems().size() + 1);
                getBillItems().add(billItem);

            }

        }

        getPreBill().setBillItems(getBillItems());

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
        }
        if (getBillItem() == null || getBillItem().getPharmaceuticalBillItem() == null) {
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
//        removingBillItem = null;
        editingBillItem = null;
        qty = null;
        item = null;
    }

    public boolean CheckDateAfterOneMonthCurrentDateTime(Date date) {
        Calendar calDateOfExpiry = Calendar.getInstance();
        calDateOfExpiry.setTime(CommonFunctions.getEndOfDay(date));
        Calendar cal = Calendar.getInstance();
        cal.setTime(CommonFunctions.getEndOfDay(new Date()));
        cal.add(Calendar.DATE, 31);
        if (cal.getTimeInMillis() <= calDateOfExpiry.getTimeInMillis()) {
            return false;
        } else {
            return true;
        }
    }

    public void saveBhtIssueRequestFrompharmacy() {
//        Date startTime = new Date();
//        Date fromDate = null;
//        Date toDate = null;
//        if (errorCheck()) {
//            return;
//        }
        if (getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment() == null) {
            JsfUtil.addErrorMessage("No Request Department");
        }
        Patient pt = getPatientEncounter().getPatient();
        getPreBill().setPaidAmount(0);

        List<BillItem> tmpBillItems = getPreBill().getBillItems();
        getPreBill().getBillItems().clear();
        getPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyBhtPre, BillClassType.PreBill, BillNumberSuffix.POR));
        getPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyBhtPre, BillClassType.PreBill, BillNumberSuffix.POR));

        getPreBill().setCreater(getSessionController().getLoggedUser());
        getPreBill().setCreatedAt(Calendar.getInstance().getTime());

        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setEditedAt(null);
        getPreBill().setEditor(null);

        getPreBill().setBillTypeAtomic(BillTypeAtomic.REQUEST_MEDICINE_INWARD);
        getPreBill().setBillClassType(BillClassType.PreBill);
        getPreBill().setBillType(BillType.PharmacyBhtPre);

        if (getPreBill().getId() == null) {
            getBillFacade().create(getPreBill());
        } else {
            getBillFacade().edit(getPreBill());
        }

        Department matrixDepartment = getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment();
        BillNumberSuffix billNumberSuffix = BillNumberSuffix.PHISSUEREQ;
        BillType btp = BillType.PharmacyBhtPre;

        savePreBillFinallyRequest(pt, matrixDepartment, btp, billNumberSuffix);
        savePreBillItemsFinallyRequest(tmpBillItems);
        JsfUtil.addSuccessMessage("Request Saved");
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

    public PrescriptionFacade getPrescriptionFacade() {
        return prescriptionFacade;
    }

    public void setPrescriptionFacade(PrescriptionFacade prescriptionFacade) {
        this.prescriptionFacade = prescriptionFacade;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    // Prescription Date and Duration Calculation Methods
    public void calculateDurationFromDates() {
        if (billItem != null && billItem.getPrescription() != null) {
            prescriptionService.autoCalculatePrescriptionDates(billItem.getPrescription());
        }
    }

    public void calculateToDateFromDuration() {
        if (billItem != null && billItem.getPrescription() != null) {
            prescriptionService.autoCalculatePrescriptionDates(billItem.getPrescription());
        }
    }

    public void calculateFromDateFromDuration() {
        if (billItem != null && billItem.getPrescription() != null) {
            prescriptionService.autoCalculatePrescriptionDates(billItem.getPrescription());
        }
    }

    public void validatePrescriptionDates() {
        if (billItem != null && billItem.getPrescription() != null) {
            String validationMessage = prescriptionService.validatePrescriptionDates(billItem.getPrescription());
            if (validationMessage != null && !validationMessage.isEmpty()) {
                setErrorMessage(validationMessage);
            } else {
                setErrorMessage(""); // Clear error message if valid
            }
        }
    }

    /**
     * Auto-calculate item and quantity from prescription details
     */
    public void calculateItemAndQuantityFromPrescription() {
        if (billItem == null || billItem.getPrescription() == null) {
            setErrorMessage("No prescription available for calculation");
            return;
        }

        try {
            com.divudi.ejb.PrescriptionToItemService.PrescriptionToItemResult result
                    = prescriptionToItemService.calculateItemAndQuantity(billItem.getPrescription());

            if (result.isSuccess()) {
                // Set the calculated item and quantity
                if (result.getItem() != null) {
                    setItem(result.getItem());
                    billItem.setItem(result.getItem());
                }

                if (result.getQuantity() != null) {
                    setQty(result.getQuantity());
                }

                // Clear any previous error messages
                setErrorMessage("");

                // Show calculation note if available
                if (result.getCalculationNote() != null && !result.getCalculationNote().isEmpty()) {
                    // You could store this in a separate field or display it in UI
                    // For now, we'll use it internally
                }

            } else {
                // Show error message
                setErrorMessage("Calculation Error: " + result.getErrorMessage());
            }

        } catch (Exception e) {
            setErrorMessage("Error calculating item and quantity: " + e.getMessage());
        }
    }

    /**
     * Check if prescription has enough information for item/quantity
     * calculation
     */
    public boolean isCalculationPossible() {
        if (billItem == null || billItem.getPrescription() == null) {
            return false;
        }
        return prescriptionToItemService.isCalculationPossible(billItem.getPrescription());
    }

    /**
     * Get calculation explanation for display
     */
    public String getCalculationExplanation() {
        if (billItem == null || billItem.getPrescription() == null) {
            return "No prescription available";
        }
        return prescriptionToItemService.getCalculationExplanation(billItem.getPrescription());
    }

    /**
     * Check if the prescription has meaningful data that warrants persistence
     *
     * @param prescription The prescription to check
     * @return true if prescription has meaningful data, false otherwise
     */
    private boolean hasMeaningfulPrescriptionData(Prescription prescription) {
        if (prescription == null) {
            return false;
        }

        // Check if any of the key prescription fields have meaningful values
        return prescription.getDose() != null
                || prescription.getDoseUnit() != null
                || prescription.getFrequencyUnit() != null
                || prescription.getDuration() != null
                || prescription.getDurationUnit() != null
                || prescription.getPrescribedFrom() != null
                || prescription.getPrescribedTo() != null
                || (prescription.getComment() != null && !prescription.getComment().trim().isEmpty())
                || (prescription.getItem() != null && billItem != null && !prescription.getItem().equals(billItem.getItem()));
    }

    public List<ClinicalFindingValue> getAllergyListOfPatient() {
        return allergyListOfPatient;
    }

    public void setAllergyListOfPatient(List<ClinicalFindingValue> allergyListOfPatient) {
        this.allergyListOfPatient = allergyListOfPatient;
    }

}
