/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ItemController;
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
import com.divudi.core.facade.DepartmentFacade;
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
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Vmp;
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
import org.primefaces.PrimeFaces;
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
    @Inject
    ItemController itemController;

////////////////////////
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    private DepartmentFacade departmentFacade;
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
    @EJB
    private com.divudi.core.facade.InpatientPackageItemFacade inpatientPackageItemFacade;
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
    // Prescription medicine-type toggle filters (VTM/ATM/VMP/AMP). Default all on = list all pharmaceutical items.
    private boolean includeVtm = true;
    private boolean includeAtm = true;
    private boolean includeVmp = true;
    private boolean includeAmp = true;
    // Edit-a-bill-item modal state: the row being edited and the same-generic substitute options
    private BillItem billItemForEdit;
    private List<Item> substituteAmps;
    private Item selectedSubstituteAmp;
    // Detached edit-model quantity. The dialog binds to this, not to the live
    // billItemForEdit.qty, so a failed validation never leaves a bad quantity on
    // the row. The value is committed to the row only when saveEditedBillItem()
    // fully passes.
    private Double editQty;
    // True only when the last saveEditedBillItem() call passed all validation and
    // committed. The dialog reads this in oncomplete to decide whether to close,
    // so a failed edit keeps the dialog open with the bad value visible.
    private boolean editSavedSuccessfully;
    // Re-entrancy guard for settleBhtRequest() so a rapid double-submit cannot
    // create duplicate bills / notifications.
    private boolean settlingBhtRequest;
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

        if (getBatchBill().getPatientEncounter().isNursingDischarged()) {
            JsfUtil.addErrorMessage("Cannot issue medicines: nursing discharge has already been confirmed for this patient.");
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
        bhtIssueRequestPrintDtoBillId = null;
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
        // Default the "Prescribed From" date to today whenever it is missing. This
        // covers the start of every new cycle (a fresh prescription after
        // clearBillItem()) as well as recovery from a postback that submitted the
        // calendar empty and nulled the field. Once a duration is entered,
        // calculateToDateFromDuration() derives "Prescribed To" from this
        // from-date + duration.
        if (billItem.getPrescription().getPrescribedFrom() == null) {
            billItem.getPrescription().setPrescribedFrom(new Date());
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
        // Server-side re-entry guard: a rapid double-submit (double-click, duplicate
        // AJAX) must not create a second request bill / notification. The first call
        // sets the flag; any overlapping call is rejected until this one finishes.
        if (settlingBhtRequest) {
            JsfUtil.addErrorMessage("This request is already being settled. Please wait.");
            return;
        }
        settlingBhtRequest = true;
        try {
            settleBhtRequestInternal();
        } finally {
            settlingBhtRequest = false;
        }
    }

    private void settleBhtRequestInternal() {
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

        if (getPatientEncounter().isNursingDischarged()) {
            JsfUtil.addErrorMessage("Cannot issue medicines: nursing discharge has already been confirmed for this patient.");
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
        rememberRequestedPharmacyForWard(fromDept, department);
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

        if (getPatientEncounter().isNursingDischarged()) {
            JsfUtil.addErrorMessage("Cannot issue medicines: nursing discharge has already been confirmed for this patient.");
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
        if (getPreBill() != null && getPreBill().getId() != null && hasNonCancelledIssuingAgainstRequest(getPreBill())) {
            JsfUtil.addErrorMessage("This request has already been issued (partially or fully) from pharmacy and can no longer be edited.");
            return;
        }
        settleEditedBhtIssueRequest(BillType.InwardPharmacyRequest, getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment(), BillNumberSuffix.PHISSUEREQ);
    }

    /**
     * XHTML-facing check so the Settle button can be disabled up front
     * (server-side re-check on submit still applies via
     * settleEditedPharmacyBhtIssueRequest()).
     */
    public boolean isPreBillAlreadyIssuedAgainstRequest() {
        return preBill != null && preBill.getId() != null && hasNonCancelledIssuingAgainstRequest(preBill);
    }

    /**
     * Mirrors PharmacyBillSearch.hasNonCancelledIssuingAgainstRequest - checks
     * whether any non-cancelled, non-refunded PharmacyBhtPre bill still
     * references this request, i.e. pharmacy has already issued (partially or
     * fully) against it and it should no longer be editable/settleable.
     */
    private boolean hasNonCancelledIssuingAgainstRequest(Bill requestBill) {
        String jpql = "SELECT COUNT(b) FROM Bill b WHERE b.retired = false "
                + "AND b.billType = :btp "
                + "AND b.referenceBill = :ref "
                + "AND b.cancelled = false "
                + "AND b.refunded = false";
        Map<String, Object> params = new HashMap<>();
        params.put("btp", BillType.PharmacyBhtPre);
        params.put("ref", requestBill);
        Long count = billFacade.findLongByJpql(jpql, params);
        return count != null && count > 0;
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
        // This is a re-settle of an existing request (same bill id) with edited
        // items - invalidate the cached print DTO so the preview re-reads the
        // updated item list instead of the pre-edit snapshot.
        bhtIssueRequestPrintDtoBillId = null;
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

    /**
     * The room category of the patient's current room, or null when the patient
     * is not in a room (or the room has no facility charge / category). Drives the
     * room-category dimension of the inward pharmacy-margin matrix (issue #21981);
     * null means "wildcard row only", preserving legacy behaviour.
     */
    private RoomCategory resolveCurrentRoomCategory(PatientEncounter encounter) {
        if (encounter == null
                || encounter.getCurrentPatientRoom() == null
                || encounter.getCurrentPatientRoom().getRoomFacilityCharge() == null) {
            return null;
        }
        return encounter.getCurrentPatientRoom().getRoomFacilityCharge().getRoomCategory();
    }

    public void updateMargin(List<BillItem> billItems, Bill bill, Department matrixDepartment, PaymentMethod paymentMethod) {
        double total = 0;
        double netTotal = 0;
        double marginTotal = 0;
        PatientEncounter encounter = bill != null ? bill.getPatientEncounter() : null;
        for (BillItem bi : billItems) {

            double rate = Math.abs(bi.getRate());
            double margin = 0;

            PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(bi, rate, matrixDepartment, paymentMethod, null,
                    encounter != null ? encounter.getAdmissionType() : null, resolveCurrentRoomCategory(encounter));

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

    private Double resolvePackageOverrideRate(com.divudi.core.entity.Item item, double requestedQty) {
        if (patientEncounter == null || patientEncounter.getInpatientPackage() == null || item == null) {
            return null;
        }
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("pkg", patientEncounter.getInpatientPackage());
        m.put("item", item);
        m.put("type", com.divudi.core.data.inward.InpatientPackageComponentType.PHARMACY_ITEM);
        java.util.List<com.divudi.core.entity.inward.InpatientPackageItem> matches = inpatientPackageItemFacade.findByJpql(
                "SELECT i FROM InpatientPackageItem i"
                        + " WHERE i.retired = false"
                        + " AND i.inpatientPackage = :pkg"
                        + " AND i.item = :item"
                        + " AND i.componentType = :type",
                m);
        if (matches.isEmpty()) {
            return null;
        }
        com.divudi.core.entity.inward.InpatientPackageItem packageItem = matches.get(0);

        java.util.Map<String, Object> qm = new java.util.HashMap<>();
        qm.put("pe", patientEncounter);
        qm.put("item", item);
        Double alreadyIssued = getBillItemFacade().findDoubleByJpql(
                "SELECT SUM(bi.qty) FROM BillItem bi"
                        + " WHERE bi.retired = false"
                        + " AND bi.fromPackage = true"
                        + " AND bi.patientEncounter = :pe"
                        + " AND bi.item = :item",
                qm);
        double consumed = alreadyIssued != null ? alreadyIssued : 0.0;

        if (getPreBill() != null && getPreBill().getBillItems() != null) {
            for (BillItem existing : getPreBill().getBillItems()) {
                if (existing.getId() == null && existing.isFromPackage() && item.equals(existing.getItem())) {
                    consumed += existing.getQty() != null ? existing.getQty() : 0.0;
                }
            }
        }

        if (consumed + requestedQty > packageItem.getQty()) {
            return null; // Beyond allocation — bill remaining/extra qty at live rate.
        }

        return packageItem.getFixedPrice() / packageItem.getQty();
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

        String reorderMsg = pharmacyService.getReorderWarningMessage(patientEncounter, billItem.getItem());
        if (!reorderMsg.isEmpty()) {
            JsfUtil.addWarningMessage(reorderMsg);
        }

        // Create a new billItem for the collection to avoid entity state issues
        BillItem newBillItem = new BillItem();
        newBillItem.setItem(billItem.getItem());
        newBillItem.setQty(getQty());
        newBillItem.setInwardChargeType(InwardChargeType.Medicine);
        newBillItem.setBill(getPreBill());
        newBillItem.setInstructions(billItem.getInstructions());
        // Required so resolvePackageOverrideRate()'s cumulative-quantity JPQL (bi.patientEncounter = :pe)
        // can find this row on subsequent dispenses of the same package-listed item.
        newBillItem.setPatientEncounter(patientEncounter);

        // Handle prescription only if prescription data is available
        boolean hasPrescriptionData = hasMeaningfulPrescriptionData(billItem.getPrescription(), billItem.getItem());

        if (hasPrescriptionData) {
            // Create a detached prescription instance for in-memory use only
            // This will be persisted later during settle operations
            Prescription inMemoryPrescription = new Prescription();
            // Carry the PRESCRIBED medicine (selected in the Prescription panel's
            // acMedicine) onto the prescription, NOT the resolved dispense item.
            // The Directions text is built from the prescription's own item, so it
            // must reflect what was prescribed (e.g. the VTM/ATM/VMP the doctor
            // ordered), not the concrete AMP/VMP chosen for dispensing. Fall back
            // to the dispense item only when no prescription medicine was picked.
            Item prescribedItem = billItem.getPrescription().getItem() != null
                    ? billItem.getPrescription().getItem()
                    : billItem.getItem();
            inMemoryPrescription.setItem(prescribedItem);
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

        Double packageRate = resolvePackageOverrideRate(newBillItem.getItem(), getQty());
        if (packageRate != null) {
            newBillItem.setOverriddenRate(packageRate);
            newBillItem.setRate(packageRate);
            newBillItem.setFromPackage(true);
        }

        newBillItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(newBillItem);

        clearBillItem();
        setActiveIndex(1);
        errorMessage = "";
        replaceableStocks = new ArrayList<>();
        itemsWithoutStocks = new ArrayList<>();
    }

    /**
     * Adds a single request bill item to the in-memory pre-bill from an
     * existing (ward) prescription. Resolves the dispensable item and quantity
     * via {@link PrescriptionToItemService} and carries the prescription
     * details (dose, frequency, duration, comment) onto a detached in-memory
     * prescription, mirroring {@link #addBillItem()}. Used when pre-filling the
     * BHT request from selected active ward medications.
     *
     * @param sourcePrescription the ward medicine prescription to request
     * @return true if an item was added, false otherwise
     */
    public boolean addBillItemFromPrescription(Prescription sourcePrescription) {
        if (sourcePrescription == null || sourcePrescription.getItem() == null) {
            return false;
        }
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No patient Selected.");
            return false;
        }

        Item dispensableItem = sourcePrescription.getItem();
        Double calculatedQty = null;
        try {
            com.divudi.ejb.PrescriptionToItemService.PrescriptionToItemResult result
                    = prescriptionToItemService.calculateItemAndQuantity(sourcePrescription);
            if (result == null) {
                JsfUtil.addErrorMessage("Could not resolve a dispensable item for "
                        + dispensableItem.getName() + ". Skipped.");
                return false;
            }
            if (result.isSuccess()) {
                if (result.getItem() != null) {
                    dispensableItem = result.getItem();
                }
                if (result.getQuantity() != null) {
                    calculatedQty = result.getQuantity();
                }
            } else {
                // The conversion did not succeed. When it failed only because the
                // prescription is incomplete (no dose/frequency/duration) we still
                // let the user request the prescribed item and edit the quantity on
                // the request page. Any other failure (e.g. no suitable AMP for a
                // VTM/ATM) is surfaced and the item is skipped rather than guessing.
                if (!prescriptionToItemService.isCalculationPossible(sourcePrescription)) {
                    JsfUtil.addWarningMessage(dispensableItem.getName()
                            + ": quantity could not be calculated (incomplete prescription). Please set the quantity on the request.");
                } else {
                    JsfUtil.addErrorMessage(dispensableItem.getName() + ": "
                            + (result.getErrorMessage() != null ? result.getErrorMessage()
                            : "could not be converted to a request item") + ". Skipped.");
                    return false;
                }
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error preparing request for " + dispensableItem.getName()
                    + ": " + e.getMessage() + ". Skipped.");
            return false;
        }
        if (calculatedQty == null || calculatedQty <= 0) {
            // Incomplete-prescription path only — the user must review/adjust on
            // the request page before settling.
            calculatedQty = 1.0;
        }

        BillItem newBillItem = new BillItem();
        newBillItem.setItem(dispensableItem);
        newBillItem.setQty(calculatedQty);
        newBillItem.setInwardChargeType(InwardChargeType.Medicine);
        newBillItem.setBill(getPreBill());

        Prescription inMemoryPrescription = new Prescription();
        // Carry the PRESCRIBED medicine onto the prescription so the Directions
        // text reflects what was ordered, not the resolved dispensable AMP/VMP.
        Item prescribedItem = sourcePrescription.getItem() != null
                ? sourcePrescription.getItem()
                : dispensableItem;
        inMemoryPrescription.setItem(prescribedItem);
        inMemoryPrescription.setDose(sourcePrescription.getDose());
        inMemoryPrescription.setDoseUnit(sourcePrescription.getDoseUnit());
        inMemoryPrescription.setFrequencyUnit(sourcePrescription.getFrequencyUnit());
        inMemoryPrescription.setDuration(sourcePrescription.getDuration());
        inMemoryPrescription.setDurationUnit(sourcePrescription.getDurationUnit());
        inMemoryPrescription.setPrescribedFrom(sourcePrescription.getPrescribedFrom());
        inMemoryPrescription.setPrescribedTo(sourcePrescription.getPrescribedTo());
        inMemoryPrescription.setComment(sourcePrescription.getComment());
        inMemoryPrescription.setPatient(getPatientEncounter().getPatient());
        inMemoryPrescription.setEncounter(getPatientEncounter());
        inMemoryPrescription.setIndoor(true);
        newBillItem.setPrescription(inMemoryPrescription);

        String prescriptionText = inMemoryPrescription.getFormattedPrescriptionWithoutIndoorOutdoor();
        if (inMemoryPrescription.getComment() != null && !inMemoryPrescription.getComment().trim().isEmpty()) {
            prescriptionText += " - " + inMemoryPrescription.getComment();
        }
        newBillItem.setDescreption(prescriptionText);

        PharmaceuticalBillItem pharmaceuticalBillItem = new PharmaceuticalBillItem();
        pharmaceuticalBillItem.setQty(-calculatedQty); // Negative quantity for requests
        pharmaceuticalBillItem.setBillItem(newBillItem);
        newBillItem.setPharmaceuticalBillItem(pharmaceuticalBillItem);

        newBillItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(newBillItem);
        return true;
    }

    // ===================================================================
    // Default / recent requested-pharmacy memory (scoped per ward dept)
    // ===================================================================
    private static final int MAX_RECENT_PHARMACIES = 5;

    private String lastPharmacyKey(Department wardDept) {
        Long id = wardDept != null ? wardDept.getId() : null;
        return "Last Requested Pharmacy For Ward " + id;
    }

    private String recentPharmaciesKey(Department wardDept) {
        Long id = wardDept != null ? wardDept.getId() : null;
        return "Recent Requested Pharmacies For Ward " + id;
    }

    /**
     * Resolve the ward department for the current patient encounter (the
     * patient's current room department), falling back to the logged-in
     * department.
     */
    public Department resolveWardDepartment() {
        if (patientEncounter != null
                && patientEncounter.getCurrentPatientRoom() != null
                && patientEncounter.getCurrentPatientRoom().getRoomFacilityCharge() != null
                && patientEncounter.getCurrentPatientRoom().getRoomFacilityCharge().getDepartment() != null) {
            return patientEncounter.getCurrentPatientRoom().getRoomFacilityCharge().getDepartment();
        }
        return sessionController.getDepartment();
    }

    /**
     * Records the pharmacy a ward last requested from, and maintains a deduped,
     * most-recent-first list (max {@value #MAX_RECENT_PHARMACIES}). Scoped per
     * ward department via the config key suffix.
     */
    public void rememberRequestedPharmacyForWard(Department wardDept, Department pharmacy) {
        if (wardDept == null || wardDept.getId() == null || pharmacy == null || pharmacy.getId() == null) {
            return;
        }
        String pharmacyId = String.valueOf(pharmacy.getId());
        configOptionApplicationController.saveShortTextOption(lastPharmacyKey(wardDept), pharmacyId);

        List<String> ids = new ArrayList<>();
        ids.add(pharmacyId);
        String existing = configOptionApplicationController.getLongTextValueByKey(recentPharmaciesKey(wardDept), "");
        if (existing != null && !existing.trim().isEmpty()) {
            for (String token : existing.split(",")) {
                String t = token.trim();
                if (!t.isEmpty() && !ids.contains(t)) {
                    ids.add(t);
                }
                if (ids.size() >= MAX_RECENT_PHARMACIES) {
                    break;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(ids.get(i));
        }
        configOptionApplicationController.setLongTextValueByKey(recentPharmaciesKey(wardDept), sb.toString());
    }

    /**
     * The default pharmacy to request from for the current ward, or null.
     */
    public Department getDefaultRequestedPharmacy() {
        Department wardDept = resolveWardDepartment();
        if (wardDept == null || wardDept.getId() == null) {
            return null;
        }
        String id = configOptionApplicationController.getShortTextValueByKey(lastPharmacyKey(wardDept), "");
        return findDepartmentById(id);
    }

    /**
     * Up to {@value #MAX_RECENT_PHARMACIES} recently-requested pharmacies for
     * the current ward, most-recent-first, for the quick-pick chips. The ward's
     * default (last-requested) pharmacy is guaranteed to appear first so the
     * user can apply it in one click.
     */
    public List<Department> getRecentRequestedPharmacies() {
        List<Department> result = new ArrayList<>();
        Department wardDept = resolveWardDepartment();
        if (wardDept == null || wardDept.getId() == null) {
            return result;
        }

        // Default first, if any.
        Department defaultPharmacy = getDefaultRequestedPharmacy();
        if (defaultPharmacy != null) {
            result.add(defaultPharmacy);
        }

        String csv = configOptionApplicationController.getLongTextValueByKey(recentPharmaciesKey(wardDept), "");
        if (csv != null && !csv.trim().isEmpty()) {
            for (String token : csv.split(",")) {
                Department d = findDepartmentById(token.trim());
                if (d != null && !result.contains(d)) {
                    result.add(d);
                }
                if (result.size() >= MAX_RECENT_PHARMACIES) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * True if the given pharmacy is the ward's default (last-requested) one;
     * used to visually mark the default chip.
     */
    public boolean isDefaultRequestedPharmacy(Department pharmacy) {
        if (pharmacy == null) {
            return false;
        }
        Department defaultPharmacy = getDefaultRequestedPharmacy();
        return defaultPharmacy != null && defaultPharmacy.equals(pharmacy);
    }

    private Department findDepartmentById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        try {
            return departmentFacade.find(Long.valueOf(id.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Quick-pick handler: sets the requesting pharmacy from a recent chip.
     */
    public void selectRequestedPharmacy(Department pharmacy) {
        this.department = pharmacy;
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

    /**
     * Opens the edit modal for a bill item already in the request. Loads the
     * same-generic substitute options (other AMPs sharing the resolved item's VMP)
     * so the user can swap the algorithm-picked item for an equivalent one.
     *
     * @param bi the bill item row to edit
     */
    public void prepareEditBillItem(BillItem bi) {
        billItemForEdit = bi;
        selectedSubstituteAmp = null;
        substituteAmps = new ArrayList<>();
        editQty = null;
        if (bi == null || bi.getItem() == null) {
            return;
        }
        selectedSubstituteAmp = bi.getItem();
        editQty = bi.getQty();
        fillSubstituteAmpsFor(bi.getItem());
    }

    /**
     * Populates {@link #substituteAmps} with AMPs that share the same VMP (generic)
     * as the given item, i.e. true therapeutic substitutes. The current item is
     * included so the dropdown shows the present selection.
     */
    private void fillSubstituteAmpsFor(Item currentItem) {
        substituteAmps = new ArrayList<>();
        if (!(currentItem instanceof Amp)) {
            // Only AMPs have a VMP-based generic equivalence to substitute within.
            if (currentItem != null) {
                substituteAmps.add(currentItem);
            }
            return;
        }
        Vmp vmp = ((Amp) currentItem).getVmp();
        if (vmp == null) {
            substituteAmps.add(currentItem);
            return;
        }
        String jpql = "select amp from Amp amp "
                + "where amp.retired = false "
                + "and amp.vmp = :vmp "
                + "order by amp.name";
        Map<String, Object> m = new HashMap<>();
        m.put("vmp", vmp);
        List<Item> found = itemFacade.findByJpql(jpql, m);
        if (found != null && !found.isEmpty()) {
            substituteAmps.addAll(found);
        } else {
            substituteAmps.add(currentItem);
        }
    }

    /**
     * Applies the substitute item and quantity chosen in the edit modal to the
     * bill item, and regenerates the directions text so it reflects the new item.
     */
    public void saveEditedBillItem() {
        editSavedSuccessfully = false;
        try {
            if (billItemForEdit == null) {
                JsfUtil.addErrorMessage("No item selected to edit.");
                return;
            }
            if (selectedSubstituteAmp == null) {
                JsfUtil.addErrorMessage("Please select an item.");
                return;
            }
            // Validate the detached edit-model quantity; the live row is untouched
            // until every check below passes, so a failed edit never corrupts the row.
            if (editQty == null || editQty <= 0) {
                JsfUtil.addErrorMessage("Please enter a valid quantity.");
                return;
            }
            // All checks passed — commit the edit to the live row.
            billItemForEdit.setItem(selectedSubstituteAmp);
            billItemForEdit.setQty(editQty);
            // The prescription keeps the originally prescribed medicine; only the
            // dispensed bill-item changes when a therapeutic substitute is chosen.
            rebuildBillItemDescription(billItemForEdit);
            editSavedSuccessfully = true;
            JsfUtil.addSuccessMessage("Item updated.");
        } finally {
            // Tell the client whether the save passed so the dialog closes only on
            // success (see btnSaveEditBillItem oncomplete).
            if (PrimeFaces.current().isAjaxRequest()) {
                PrimeFaces.current().ajax().addCallbackParam("editSaved", editSavedSuccessfully);
            }
        }
    }

    /**
     * Rebuilds a bill item's directions text from its prescription (or a simple
     * item + qty fallback), mirroring how {@link #addBillItem()} builds it.
     */
    private void rebuildBillItemDescription(BillItem bi) {
        Prescription rx = bi.hasPrescription() ? bi.getPrescription() : null;
        if (rx != null && hasMeaningfulPrescriptionData(rx, bi.getItem())) {
            String prescriptionText = rx.getFormattedPrescriptionWithoutIndoorOutdoor();
            if (rx.getComment() != null && !rx.getComment().trim().isEmpty()) {
                prescriptionText += " - " + rx.getComment();
            }
            bi.setDescreption(prescriptionText);
        } else if (bi.getItem() != null) {
            bi.setDescreption(bi.getItem().getName() + " - Qty: " + bi.getQty());
        }
    }

    public BillItem getBillItemForEdit() {
        return billItemForEdit;
    }

    public void setBillItemForEdit(BillItem billItemForEdit) {
        this.billItemForEdit = billItemForEdit;
    }

    public List<Item> getSubstituteAmps() {
        return substituteAmps;
    }

    public void setSubstituteAmps(List<Item> substituteAmps) {
        this.substituteAmps = substituteAmps;
    }

    public Item getSelectedSubstituteAmp() {
        return selectedSubstituteAmp;
    }

    public void setSelectedSubstituteAmp(Item selectedSubstituteAmp) {
        this.selectedSubstituteAmp = selectedSubstituteAmp;
    }

    public Double getEditQty() {
        return editQty;
    }

    public void setEditQty(Double editQty) {
        this.editQty = editQty;
    }

    public boolean isEditSavedSuccessfully() {
        return editSavedSuccessfully;
    }

    public void setEditSavedSuccessfully(boolean editSavedSuccessfully) {
        this.editSavedSuccessfully = editSavedSuccessfully;
    }

    /**
     * Autocomplete for the Prescription item field, filtered by the VTM/ATM/VMP/AMP
     * toggle buttons. Delegates to the shared ItemController query so the filtering
     * logic stays in one place. When no type is selected, returns an empty list and
     * warns the user.
     */
    public List<Item> completePrescriptionMedicineWithTypeFilter(String query) {
        if (!includeVtm && !includeAtm && !includeVmp && !includeAmp) {
            JsfUtil.addErrorMessage("Please select at least one medicine type to search");
            return new ArrayList<>();
        }
        return itemController.completeMedicineByTypeWithFilter(query, includeVtm, includeAtm, includeVmp, includeAmp);
    }

    public boolean isIncludeVtm() {
        return includeVtm;
    }

    public void setIncludeVtm(boolean includeVtm) {
        this.includeVtm = includeVtm;
    }

    public boolean isIncludeAtm() {
        return includeAtm;
    }

    public void setIncludeAtm(boolean includeAtm) {
        this.includeAtm = includeAtm;
    }

    public boolean isIncludeVmp() {
        return includeVmp;
    }

    public void setIncludeVmp(boolean includeVmp) {
        this.includeVmp = includeVmp;
    }

    public boolean isIncludeAmp() {
        return includeAmp;
    }

    public void setIncludeAmp(boolean includeAmp) {
        this.includeAmp = includeAmp;
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

    @EJB
    private com.divudi.service.pharmacy.BhtIssueRequestNativeSqlService bhtIssueRequestNativeSqlService;

    private com.divudi.core.data.dto.pharmacy.BhtIssueRequestPrintDto bhtIssueRequestPrintDto;
    private Long bhtIssueRequestPrintDtoBillId;

    public com.divudi.core.data.dto.pharmacy.BhtIssueRequestPrintDto getBhtIssueRequestPrintDto() {
        if (printBill == null || printBill.getId() == null) {
            return null;
        }
        if (!printBill.getId().equals(bhtIssueRequestPrintDtoBillId)) {
            bhtIssueRequestPrintDto = bhtIssueRequestNativeSqlService.loadPrintDtoByBillId(printBill.getId());
            bhtIssueRequestPrintDtoBillId = printBill.getId();
            if (bhtIssueRequestPrintDto == null) {
                JsfUtil.addErrorMessage("BHT Issue Request not found");
            }
        }
        return bhtIssueRequestPrintDto;
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
     * Case 1 — Generate but do NOT add.
     *
     * Recomputes the dispense item and quantity from the current prescription
     * details, then leaves the resolved values in the Dispense Request panel so
     * the user can review and adjust them before adding. Focus is moved to the
     * Dispense item field on the page (see the button's update/focus wiring).
     * This intentionally does not touch the bill-items table.
     */
    public void generateDispenseFromPrescription() {
        calculateItemAndQuantityFromPrescription();
        if (errorMessage != null && !errorMessage.isEmpty()) {
            JsfUtil.addErrorMessage(errorMessage);
        }
    }

    /**
     * Case 2 — Calculate and Add in one step.
     *
     * Recomputes the dispense item and quantity from the prescription and, only
     * if the calculation succeeds, adds the resolved line to the dispense
     * request. If the calculation cannot produce an item and quantity (e.g. a
     * missing dose or frequency), the error is shown and nothing is added, so a
     * blank or stale line can never be appended.
     */
    public void calculateAndAddBillItem() {
        if (billItem == null || billItem.getPrescription() == null) {
            JsfUtil.addErrorMessage("No prescription available for calculation");
            return;
        }

        try {
            com.divudi.ejb.PrescriptionToItemService.PrescriptionToItemResult result
                    = prescriptionToItemService.calculateItemAndQuantity(billItem.getPrescription());

            // Require a positive quantity too: setQty() silently drops values <= 0,
            // so a non-positive result would otherwise leave the prior quantity in
            // place and add a stale line.
            if (!result.isSuccess() || result.getItem() == null
                    || result.getQuantity() == null || result.getQuantity() <= 0) {
                String msg = result.getErrorMessage() != null && !result.getErrorMessage().isEmpty()
                        ? result.getErrorMessage()
                        : "Could not calculate the item and quantity from the prescription";
                JsfUtil.addErrorMessage("Calculation Error: " + msg);
                return;
            }

            setItem(result.getItem());
            billItem.setItem(result.getItem());
            setQty(result.getQuantity());
            setErrorMessage("");
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error calculating item and quantity: " + e.getMessage());
            return;
        }

        addBillItem();
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
    private boolean hasMeaningfulPrescriptionData(Prescription prescription, Item dispenseItem) {
        if (prescription == null) {
            return false;
        }

        // Check if any of the key prescription fields have meaningful values.
        // Note: prescribedFrom is intentionally excluded here because it is now
        // auto-defaulted to today for every new cycle (see getBillItem()), so on
        // its own it does not indicate the user entered prescription details.
        // The item check compares the prescribed medicine against the passed-in
        // dispensed item (a therapeutic substitute) rather than the controller-level
        // billItem, which may already have been cleared after a row was added.
        return prescription.getDose() != null
                || prescription.getDoseUnit() != null
                || prescription.getFrequencyUnit() != null
                || prescription.getDuration() != null
                || prescription.getDurationUnit() != null
                || prescription.getPrescribedTo() != null
                || (prescription.getComment() != null && !prescription.getComment().trim().isEmpty())
                || (prescription.getItem() != null && !Objects.equals(prescription.getItem(), dispenseItem));
    }

    public List<ClinicalFindingValue> getAllergyListOfPatient() {
        return allergyListOfPatient;
    }

    public void setAllergyListOfPatient(List<ClinicalFindingValue> allergyListOfPatient) {
        this.allergyListOfPatient = allergyListOfPatient;
    }

}
