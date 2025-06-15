/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.AmpFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.CategoryFacade;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.service.pharmacy.PharmacyCostingService;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import java.math.BigDecimal;
import java.util.Optional;
import org.primefaces.event.RowEditEvent;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class GrnCostingController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;
    private BilledBill bill;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private CategoryFacade categoryFacade;
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private AmpFacade ampFacade;

    @EJB
    @Deprecated
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    PharmacyCostingService pharmacyCostingService;
    @Inject
    private PharmacyCalculation pharmacyCalculation;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;


    public boolean isShowProfitInGrnBill() {
        return configOptionApplicationController.getBooleanValueByKey("Show Profit Percentage in GRN", true);
    }

    /**
     * Wrapper for PharmacyCostingService.calculateProfitMarginForPurchases to be used in JSF.
     */
    public double calcProfitMargin(BillItem bi) {
        return pharmacyCostingService.calculateProfitMarginForPurchases(bi);
    }
    /////////////////
    private Bill approveBill;
    private Bill grnBill;
    private Bill currentGrnBillPre;
    private boolean printPreview;
    //////////////
    private List<BillItem> billItems;
    private List<BillItem> selectedBillItems;
    private SearchKeyword searchKeyword;
    private double insTotal;
    private double difference;
    private Institution fromInstitution;
    private Institution referenceInstitution;
    private Date invoiceDate;
    private String invoiceNumber;
    BillItem currentExpense;
    List<BillItem> billExpenses;

    public double calDifference() {
        difference = Math.abs(insTotal) - Math.abs(getGrnBill().getNetTotal());
        return difference;
    }

    public String navigateToResiveCosting() {
        clear();
        createGrn();
        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setCreditDuration(getApproveBill().getCreditDuration());
        return "/pharmacy/pharmacy_grn_costing?faces-redirect=true";
    }

    public void clear() {
        billExpenses = null;
        grnBill = null;
        invoiceDate = null;
        invoiceNumber = null;
        printPreview = false;
        billItems = null;
        difference = 0;
        insTotal = 0;
    }


    public void removeItem(BillItem bi) {
        getBillItems().remove(bi.getSearialNo());
        calGrossTotal();
    }

    public List<BillItem> findAllBillItemsRefernceToOriginalItem(BillItem referenceBillItem) {
        List<BillItem> tmpBillItems = new ArrayList<>();
        for (BillItem i : getBillItems()) {
            if (i.getReferanceBillItem() == referenceBillItem) {
                tmpBillItems.add(i);
            }
        }
        return tmpBillItems;
    }

    public void duplicateItem(BillItem originalBillItemToDuplicate) {
        BillItem newBillItemCreatedByDuplication = new BillItem();
        double totalQuantityOfBillItemsRefernceToOriginalItem = 0.0;
        double totalFreeQuantityOfBillItemsRefernceToOriginalItem = 0.0;

        double remainFreeQty;
        double remainQty;
        if (originalBillItemToDuplicate != null) {
            PharmaceuticalBillItem newPharmaceuticalBillItemCreatedByDuplication = new PharmaceuticalBillItem();
            newPharmaceuticalBillItemCreatedByDuplication.copy(originalBillItemToDuplicate.getPharmaceuticalBillItem());
            newPharmaceuticalBillItemCreatedByDuplication.setBillItem(newBillItemCreatedByDuplication);
            newBillItemCreatedByDuplication.setItem(originalBillItemToDuplicate.getItem());
            newBillItemCreatedByDuplication.setReferanceBillItem(originalBillItemToDuplicate.getReferanceBillItem());
            newBillItemCreatedByDuplication.setPharmaceuticalBillItem(newPharmaceuticalBillItemCreatedByDuplication);

            List<BillItem> tmpBillItems = findAllBillItemsRefernceToOriginalItem(originalBillItemToDuplicate.getReferanceBillItem());

            for (BillItem bi : tmpBillItems) {
                totalQuantityOfBillItemsRefernceToOriginalItem += bi.getPharmaceuticalBillItem().getQtyInUnit();
                totalFreeQuantityOfBillItemsRefernceToOriginalItem += bi.getPharmaceuticalBillItem().getFreeQtyInUnit();
            }
            remainQty = originalBillItemToDuplicate.getPreviousRecieveQtyInUnit() - totalQuantityOfBillItemsRefernceToOriginalItem;
            remainFreeQty = originalBillItemToDuplicate.getPreviousRecieveFreeQtyInUnit() - totalFreeQuantityOfBillItemsRefernceToOriginalItem;

            newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setQty(remainQty);
            newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setQtyInUnit(remainQty);

            newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setFreeQty(remainFreeQty);
            newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setFreeQtyInUnit(remainFreeQty);

            newBillItemCreatedByDuplication.setTmpQty(remainQty);
            newBillItemCreatedByDuplication.setTmpFreeQty(remainFreeQty);

            newBillItemCreatedByDuplication.setPreviousRecieveQtyInUnit(originalBillItemToDuplicate.getPreviousRecieveQtyInUnit());
            newBillItemCreatedByDuplication.setPreviousRecieveFreeQtyInUnit(originalBillItemToDuplicate.getPreviousRecieveFreeQtyInUnit());
            getBillItems().add(newBillItemCreatedByDuplication);
        }
        calGrossTotal();
    }

    public void removeSelected() {
        //  //System.err.println("1");
        if (selectedBillItems == null) {
            //   //System.err.println("2");
            return;
        }

        //   //System.err.println("3");
        for (BillItem b : selectedBillItems) {
            //  //System.err.println("4");
            getBillItems().remove(b.getSearialNo());
            calGrossTotal();
        }

        selectedBillItems = null;
    }

    public void clearList() {
        //   pharmacyItems = null;
        //  billItems = null;
    }

    public void setBatch(BillItem pid) {
        if (pid.getPharmaceuticalBillItem().getDoe() == null) {
            return;
        }

        if (pid.getPharmaceuticalBillItem().getDoe() != null) {
            if (pid.getPharmaceuticalBillItem().getDoe().getTime() < Calendar.getInstance().getTimeInMillis()) {
                pid.getPharmaceuticalBillItem().setStringValue(null);
                return;
                //    return;
            }
        }

        if (pid.getPharmaceuticalBillItem().getStringValue().trim().isEmpty()) {
            Date date = pid.getPharmaceuticalBillItem().getDoe();
            DateFormat df = new SimpleDateFormat("ddMMyyyy");
            String reportDate = df.format(date);
// Print what date is today!
            //       //System.err.println("Report Date: " + reportDate);
            pid.getPharmaceuticalBillItem().setStringValue(reportDate);
        }
    }



    public void request() {
//        if (Math.abs(difference) > 1) {
//            JsfUtil.addErrorMessage("The invoice does not match..! Check again");
//            return;
//        }
        if (getCurrentGrnBillPre().getFromInstitution() == null) {
            getCurrentGrnBillPre().setFromInstitution(getFromInstitution());
        }
        if (getCurrentGrnBillPre().getReferenceInstitution() == null) {
            getCurrentGrnBillPre().setReferenceInstitution(getReferenceInstitution());
        }
        getCurrentGrnBillPre().setInvoiceDate(invoiceDate);
        getCurrentGrnBillPre().setInvoiceNumber(invoiceNumber);
        getCurrentGrnBillPre().setPaymentMethod(getApproveBill().getPaymentMethod());

        if (getCurrentGrnBillPre().getInvoiceDate() == null) {
            getCurrentGrnBillPre().setInvoiceDate(getApproveBill().getCreatedAt());
        }

        String msg = pharmacyCalculation.errorCheck(getCurrentGrnBillPre(), billItems);
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return;
        }

        saveGrnPreBill();
        if (!getCurrentGrnBillPre().getBillItems().isEmpty()) {
            getCurrentGrnBillPre().setBillItems(null);
        }

//        Payment p = createPayment(getGrnBill(), getGrnBill().getPaymentMethod());
        for (BillItem i : getBillItems()) {
            if (i.getTmpQty() == 0.0 && i.getTmpFreeQty() == 0.0) {
                continue;
            }

            applyFinanceDetailsToPharmaceutical(i);

            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);

            i.setCreatedAt(new Date());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getCurrentGrnBillPre());
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            } else {
                getBillItemFacade().edit(i);
            }

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            } else {
                getPharmaceuticalBillItemFacade().edit(ph);
            }

            i.setPharmaceuticalBillItem(ph);
            getBillItemFacade().edit(i);

//                 updatePoItemQty(i);
//            System.err.println("1 " + i);
            ItemBatch itemBatch = getPharmacyCalculation().saveItemBatch(i);
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());

            double addingQty = i.getPharmaceuticalBillItem().getQtyInUnit() + i.getPharmaceuticalBillItem().getFreeQtyInUnit();

            i.getPharmaceuticalBillItem().setItemBatch(itemBatch);
//
//            Stock stock = getPharmacyBean().addToStock(
//                    i.getPharmaceuticalBillItem(),
//                    Math.abs(addingQty),
//                    getSessionController().getDepartment());
//
//            i.getPharmaceuticalBillItem().setStock(stock);
            getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            getPharmacyCalculation().editBillItem(i.getPharmaceuticalBillItem(), getSessionController().getLoggedUser());
//            saveBillFee(i, p);
            getCurrentGrnBillPre().getBillItems().add(i);
        }
        for (BillItem bi : getCurrentGrnBillPre().getBillItems()) {
        }

        calGrossTotal();

        pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getCurrentGrnBillPre());
//        updateBalanceForGrn(getCurrentGrnBillPre());

        getBillFacade().edit(getCurrentGrnBillPre());

        //  getPharmacyBillBean().editBill(, , getSessionController());
//        printPreview = true;
        JsfUtil.addSuccessMessage("Request Saved");

    }

    public void requestFinalize() {
        if (Math.abs(difference) > 1) {
            JsfUtil.addErrorMessage("The invoice does not match..! Check again");
            return;
        }
        if (getCurrentGrnBillPre().getFromInstitution() == null) {
            getCurrentGrnBillPre().setFromInstitution(getFromInstitution());
        }
        if (getCurrentGrnBillPre().getReferenceInstitution() == null) {
            getCurrentGrnBillPre().setReferenceInstitution(getReferenceInstitution());
        }
        getCurrentGrnBillPre().setInvoiceDate(invoiceDate);
        getCurrentGrnBillPre().setInvoiceNumber(invoiceNumber);
        getCurrentGrnBillPre().setPaymentMethod(getApproveBill().getPaymentMethod());

        String msg = pharmacyCalculation.errorCheck(getCurrentGrnBillPre(), billItems);
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return;
        }
        if (!getCurrentGrnBillPre().getBillItems().isEmpty()) {
            getCurrentGrnBillPre().setBillItems(null);
        }
        finalizeBill();
        if (!getCurrentGrnBillPre().getBillItems().isEmpty()) {
            getCurrentGrnBillPre().setBillItems(null);
        }

//        Payment p = createPayment(getGrnBill(), getGrnBill().getPaymentMethod());
        for (BillItem i : getBillItems()) {
            if (i.getTmpQty() == 0.0 && i.getTmpFreeQty() == 0.0) {
                continue;
            }

            applyFinanceDetailsToPharmaceutical(i);

            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);

            i.setCreatedAt(new Date());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getCurrentGrnBillPre());
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            } else {
                getBillItemFacade().edit(i);
            }

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            } else {
                getPharmaceuticalBillItemFacade().edit(ph);
            }

            i.setPharmaceuticalBillItem(ph);
            getBillItemFacade().edit(i);

//                 updatePoItemQty(i);
//            System.err.println("1 " + i);
            ItemBatch itemBatch = getPharmacyCalculation().saveItemBatch(i);
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());

            double addingQty = i.getPharmaceuticalBillItem().getQtyInUnit() + i.getPharmaceuticalBillItem().getFreeQtyInUnit();

            i.getPharmaceuticalBillItem().setItemBatch(itemBatch);
//
//            Stock stock = getPharmacyBean().addToStock(
//                    i.getPharmaceuticalBillItem(),
//                    Math.abs(addingQty),
//                    getSessionController().getDepartment());
//
//            i.getPharmaceuticalBillItem().setStock(stock);
            getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            getPharmacyCalculation().editBillItem(i.getPharmaceuticalBillItem(), getSessionController().getLoggedUser());
//            saveBillFee(i, p);
            getCurrentGrnBillPre().getBillItems().add(i);
        }
        getCurrentGrnBillPre().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));
        getCurrentGrnBillPre().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));

        getCurrentGrnBillPre().setToInstitution(getApproveBill().getFromInstitution());
        getCurrentGrnBillPre().setToDepartment(getApproveBill().getFromDepartment());

        getCurrentGrnBillPre().setInstitution(getSessionController().getInstitution());
        getCurrentGrnBillPre().setDepartment(getSessionController().getDepartment());

        getCurrentGrnBillPre().setCreater(getSessionController().getLoggedUser());
        getCurrentGrnBillPre().setCreatedAt(Calendar.getInstance().getTime());

        calGrossTotal();

//        pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getCurrentGrnBillPre());
//        updateBalanceForGrn(getCurrentGrnBillPre());
        getBillFacade().edit(getCurrentGrnBillPre());

        //  getPharmacyBillBean().editBill(, , getSessionController());
//        printPreview = true;
        JsfUtil.addSuccessMessage("Request Finalized");

    }

    public void settle() {
        if (!validateInputs()) {
            return;
        }

        saveGrnBill();
        distributeValuesToItems();
        processBillItems();
        finalizeSettle();
        createAndPersistPayment();
    }

    private boolean validateInputs() {
        if (Math.abs(difference) > 1) {
            JsfUtil.addErrorMessage("The invoice does not match..! Check again");
            return false;
        }
        if (getGrnBill().getFromInstitution() == null) {
            getGrnBill().setFromInstitution(getFromInstitution());
        }
        if (getGrnBill().getReferenceInstitution() == null) {
            getGrnBill().setReferenceInstitution(getReferenceInstitution());
        }
        if (getGrnBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return false;
        }

        getGrnBill().setInvoiceDate(invoiceDate);
        getGrnBill().setInvoiceNumber(invoiceNumber);
        String msg = pharmacyCalculation.errorCheck(getGrnBill(), billItems);
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return false;
        }
        pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getGrnBill());
        if (getGrnBill().getInvoiceDate() == null) {
            getGrnBill().setInvoiceDate(getApproveBill().getCreatedAt());
        }
        return true;
    }

    private void saveGrnBill() {
        saveBill();
    }

    private void distributeValuesToItems() {
        pharmacyCostingService.distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
    }

    private void processBillItems() {
        for (BillItem i : getBillItems()) {
            if ((i.getTmpQty() == 0.0 && i.getTmpFreeQty() == 0.0)
                    || (i.getTmpQty() < 0.0 && i.getTmpFreeQty() < 0.0)) {
                continue;
            }
            applyFinanceDetailsToPharmaceutical(i);
            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);
            i.setCreatedAt(new Date());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getGrnBill());
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }
            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }
            i.setPharmaceuticalBillItem(ph);
            getBillItemFacade().edit(i);
            updateStockAndBatches(i);
            getPharmacyCalculation().editBillItem(i.getPharmaceuticalBillItem(), getSessionController().getLoggedUser());
            saveBillFee(i);
            getGrnBill().getBillItems().add(i);
        }
    }

    private void updateStockAndBatches(BillItem i) {
        ItemBatch itemBatch;
        if (configOptionApplicationController.getBooleanValueByKey("Manage Costing", true)) {
            itemBatch = getPharmacyCalculation().saveItemBatchWithCosting(i);
        } else {
            itemBatch = getPharmacyCalculation().saveItemBatch(i);
        }
        double addingQty = i.getPharmaceuticalBillItem().getQtyInUnit()
                + i.getPharmaceuticalBillItem().getFreeQtyInUnit();
        i.getPharmaceuticalBillItem().setItemBatch(itemBatch);
        Stock stock = getPharmacyBean().addToStock(
                i.getPharmaceuticalBillItem(),
                Math.abs(addingQty),
                getSessionController().getDepartment());
        i.getPharmaceuticalBillItem().setStock(stock);
        getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
    }

    private void createAndPersistPayment() {
        createPayment(getGrnBill(), getGrnBill().getPaymentMethod());
    }

    private void finalizeSettle() {
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(
                getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_GRN);
        getGrnBill().setDeptId(deptId);
        getGrnBill().setInsId(deptId);
        getGrnBill().setToInstitution(getApproveBill().getFromInstitution());
        getGrnBill().setToDepartment(getApproveBill().getFromDepartment());
        getGrnBill().setInstitution(getSessionController().getInstitution());
        getGrnBill().setDepartment(getSessionController().getDepartment());
        getGrnBill().setCreater(getSessionController().getLoggedUser());
        getGrnBill().setCreatedAt(Calendar.getInstance().getTime());
        getGrnBill().setBillExpenses(billExpenses);
        getGrnBill().setExpenseTotal(calExpenses());
        calGrossTotal();
        getGrnBill().setNetTotal(getGrnBill().getNetTotal() - calExpenses());
        pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getGrnBill());
        updateBalanceForGrn(getGrnBill());
        getBillFacade().edit(getGrnBill());
        printPreview = true;
    }

    public void settleWholesale() {
        if (insTotal == 0 && difference != 0) {
            JsfUtil.addErrorMessage("Fill the invoice Total");
            return;
        }
        if (difference != 0) {
            JsfUtil.addErrorMessage("The invoice does not match..! Check again");
            return;
        }

        String msg = pharmacyCalculation.errorCheck(getGrnBill(), billItems);
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return;
        }
        pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getGrnBill());
        if (getGrnBill().getInvoiceDate() == null) {
            getGrnBill().setInvoiceDate(getApproveBill().getCreatedAt());
        }

        saveWholesaleBill();
        pharmacyCostingService.distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());

        Payment p = createPayment(getGrnBill(), getGrnBill().getPaymentMethod());

        for (BillItem i : getBillItems()) {
            if (i.getTmpQty() == 0.0 && i.getTmpFreeQty() == 0.0) {
                continue;
            }

            applyFinanceDetailsToPharmaceutical(i);

            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);

            i.setCreatedAt(new Date());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getGrnBill());
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            i.setPharmaceuticalBillItem(ph);
            getBillItemFacade().edit(i);

            //     updatePoItemQty(i);
            //System.err.println("1 " + i);
            ItemBatch itemBatch;
            if (configOptionApplicationController.getBooleanValueByKey("Manage Costing", true)) {
                itemBatch = getPharmacyCalculation().saveItemBatchWithCosting(i);
            } else {
                itemBatch = getPharmacyCalculation().saveItemBatch(i);
            }
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());

            double addingQty = i.getPharmaceuticalBillItem().getQtyInUnit() + i.getPharmaceuticalBillItem().getFreeQtyInUnit();

            i.getPharmaceuticalBillItem().setItemBatch(itemBatch);

            Stock stock = getPharmacyBean().addToStock(
                    i.getPharmaceuticalBillItem(),
                    Math.abs(addingQty),
                    getSessionController().getDepartment());

            i.getPharmaceuticalBillItem().setStock(stock);

            getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            getPharmacyCalculation().editBillItem(i.getPharmaceuticalBillItem(), getSessionController().getLoggedUser());
            saveBillFee(i, p);
            getGrnBill().getBillItems().add(i);
        }

        getGrnBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));
        getGrnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));

        getGrnBill().setToInstitution(getApproveBill().getFromInstitution());
        getGrnBill().setToDepartment(getApproveBill().getFromDepartment());

        getGrnBill().setInstitution(getSessionController().getInstitution());
        getGrnBill().setDepartment(getSessionController().getDepartment());

        getGrnBill().setCreater(getSessionController().getLoggedUser());
        getGrnBill().setCreatedAt(Calendar.getInstance().getTime());

        calGrossTotal();

        pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getGrnBill());
        updateBalanceForGrn(getGrnBill());

        getBillFacade().edit(getGrnBill());
        //  getPharmacyBillBean().editBill(, , getSessionController());
        printPreview = true;

    }

    public double calFreeQuantityPurchaseValue(Bill b) {
        double freeTotal = 0.0;
        for (BillItem bi : b.getBillItems()) {
            freeTotal = freeTotal + (bi.getPharmaceuticalBillItem().getFreeQty() * bi.getPharmaceuticalBillItem().getPurchaseRate());
        }
        return freeTotal;
    }

    public double calFreeQuantitySaleValue(Bill b) {
        double freeTotal = 0.0;
        for (BillItem bi : b.getBillItems()) {
            freeTotal = freeTotal + (bi.getPharmaceuticalBillItem().getFreeQty() * bi.getPharmaceuticalBillItem().getRetailRate());
        }
        return freeTotal;
    }

    private void updateBalanceForGrn(Bill grn) {
        if (grn == null) {
            return;
        }
        switch (grn.getPaymentMethod()) {
            case Agent:
            case Card:
            case Cash:
            case Cheque:
            case MultiplePaymentMethods:
            case OnCall:
            case OnlineSettlement:
            case PatientDeposit:
            case Slip:
            case Staff:
            case YouOweMe:
            case ewallet:
                grn.setBalance(0.0);
                break;
            case Credit:
                grn.setBalance(Math.abs(grn.getNetTotal()));
            default:
        }
    }

    public void viewPoList() {
        clearList();

    }

    public GrnCostingController() {
    }

    private String txtSearch;

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Bill getApproveBill() {
        if (approveBill == null) {
            approveBill = new BilledBill();
        }
        return approveBill;
    }

    public void finalizeBill() {
        if (currentGrnBillPre == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }
        if (currentGrnBillPre.getId() == null) {
            request();
        }
        getCurrentGrnBillPre().setEditedAt(new Date());
        getCurrentGrnBillPre().setEditor(sessionController.getLoggedUser());
        getCurrentGrnBillPre().setCheckeAt(new Date());
        getCurrentGrnBillPre().setCheckedBy(sessionController.getLoggedUser());
        getCurrentGrnBillPre().setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER);
        getBillFacade().edit(getCurrentGrnBillPre());

    }

    public void saveGrnPreBill() {
        getCurrentGrnBillPre().setBillDate(new Date());
        getCurrentGrnBillPre().setBillTime(new Date());
        getCurrentGrnBillPre().setPaymentMethod(getApproveBill().getPaymentMethod());
        getCurrentGrnBillPre().setReferenceBill(getApproveBill());
        if (getCurrentGrnBillPre().getFromInstitution() == null) {
            getCurrentGrnBillPre().setFromInstitution(getFromInstitution());
        }
        getCurrentGrnBillPre().setReferenceInstitution(getReferenceInstitution());
        getCurrentGrnBillPre().setDepartment(getSessionController().getDepartment());
        getCurrentGrnBillPre().setInstitution(getSessionController().getInstitution());

        getCurrentGrnBillPre().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));
        getCurrentGrnBillPre().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));

        getCurrentGrnBillPre().setToInstitution(getApproveBill().getFromInstitution());
        getCurrentGrnBillPre().setToDepartment(getApproveBill().getFromDepartment());

        getCurrentGrnBillPre().setInstitution(getSessionController().getInstitution());
        getCurrentGrnBillPre().setDepartment(getSessionController().getDepartment());

        getCurrentGrnBillPre().setCreater(getSessionController().getLoggedUser());
        getCurrentGrnBillPre().setCreatedAt(Calendar.getInstance().getTime());
        //   getGrnBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnBill, BillNumberSuffix.GRN));
        //   getGrnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getGrnBill(), BillType.PharmacyGrnBill, BillNumberSuffix.GRN));
//        getGrnBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN);
        if (getCurrentGrnBillPre().getId() == null) {
            getBillFacade().create(getCurrentGrnBillPre());
        } else {
            getBillFacade().edit(getCurrentGrnBillPre());
        }

        getApproveBill().setReferenceBill(getCurrentGrnBillPre());
        getBillFacade().edit(getApproveBill());
    }

    public void saveBill() {
        getGrnBill().setBillDate(new Date());
        getGrnBill().setBillTime(new Date());
//        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setReferenceBill(getApproveBill());
        getGrnBill().setReferenceInstitution(getReferenceInstitution());
        getGrnBill().setDepartment(getSessionController().getDepartment());
        getGrnBill().setInstitution(getSessionController().getInstitution());
        getGrnBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN);

        if (getGrnBill().getId() == null) {
            getBillFacade().create(getGrnBill());
        } else {
            getBillFacade().edit(getGrnBill());
        }

        if (getCurrentGrnBillPre() != null) {

            if (getCurrentGrnBillPre().getId() == null) {
                billFacade.create(getCurrentGrnBillPre());
            } else {
                billFacade.edit(getCurrentGrnBillPre());
            }
            getCurrentGrnBillPre().setForwardReferenceBill(getGrnBill());
            getBillFacade().edit(getCurrentGrnBillPre());
        }

    }

    public void saveWholesaleBill() {
        getGrnBill().setBillDate(new Date());
        getGrnBill().setBillTime(new Date());
        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setReferenceBill(getApproveBill());
        if (getGrnBill().getFromInstitution() == null) {
            getGrnBill().setFromInstitution(getApproveBill().getToInstitution());
        }
        getGrnBill().setDepartment(getSessionController().getDepartment());
        getGrnBill().setInstitution(getSessionController().getInstitution());
        //   getGrnBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnBill, BillNumberSuffix.GRN));
        //   getGrnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getGrnBill(), BillType.PharmacyGrnBill, BillNumberSuffix.GRN));
        if (getGrnBill().getId() == null) {
            getBillFacade().create(getGrnBill());
        }
    }

//    public void generateBillComponent() {
//
//        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getApproveBill())) {
//            double remains = getPharmacyCalculation().calQtyInTwoSql(i);
//
//            if (i.getQtyInUnit() >= remains && (i.getQtyInUnit() - remains) != 0) {
//                BillItem bi = new BillItem();
//                bi.setSearialNo(getBillItems().size());
//                bi.setItem(i.getBillItem().getItem());
//                bi.setReferanceBillItem(i.getBillItem());
//                bi.setQty(i.getQtyInUnit() - remains);
//                bi.setTmpQty(i.getQtyInUnit() - remains);
//                //Set Suggession
////                bi.setTmpSuggession(getPharmacyCalculation().getSuggessionOnly(bi.getItem()));
//
//                PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
//                ph.setBillItem(bi);
//                double tmpQty = bi.getQty();
//                ph.setQtyInUnit((double) tmpQty);
//                ph.setPurchaseRate(i.getPurchaseRate());
//                ph.setRetailRate(i.getRetailRate());
//                ph.setLastPurchaseRate(getPharmacyBean().getLastPurchaseRate(bi.getItem(), getSessionController().getDepartment()));
//
//                bi.setPharmaceuticalBillItem(ph);
//
//                getBillItems().add(bi);
//                //  getBillItems().r
//            }
//
//        }
//    }
    public void generateBillComponent() {

        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getApproveBill())) {
            double calculatedReturns = getPharmacyCalculation().calculateRemainigQtyFromOrder(i);
            double remains = Math.abs(i.getQtyInUnit()) - Math.abs(calculatedReturns);
            double remainFreeQty = i.getFreeQty() - getPharmacyCalculation().calculateRemainingFreeQtyFromOrder(i);

            if (remains > 0 || remainFreeQty > 0) {
                BillItem bi = new BillItem();
                bi.setSearialNo(getBillItems().size());
                bi.setItem(i.getBillItem().getItem());
                bi.setReferanceBillItem(i.getBillItem());
                bi.setQty(remains);
                bi.setTmpQty(remains);
                bi.setTmpFreeQty(remainFreeQty);
                //Set Suggession
//                bi.setTmpSuggession(getPharmacyCalculation().getSuggessionOnly(bi.getItem()));

                PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
                ph.setBillItem(bi);
                double tmpQty = bi.getQty();
                double tmpFreeQty = remainFreeQty;

                bi.setPreviousRecieveQtyInUnit(tmpQty);
                bi.setPreviousRecieveFreeQtyInUnit(tmpFreeQty);

                ph.setQty(tmpQty);
                ph.setQtyInUnit(tmpQty);

                ph.setFreeQtyInUnit(tmpFreeQty);
                ph.setFreeQty(tmpFreeQty);

                double pr = 0.0;
                double rr = 0.0;
                BigDecimal packRate = BigDecimal.ZERO;

                BillItem lastPurchasedBillItem = getPharmacyBean().getLastPurchaseItem(bi.getItem(), sessionController.getDepartment());
                if (lastPurchasedBillItem != null) {
                    BillItemFinanceDetails lastDetails = lastPurchasedBillItem.getBillItemFinanceDetails();
                    if (lastDetails != null) {
                        BigDecimal lineGrossRate = lastDetails.getLineGrossRate();
                        BigDecimal lastRetailRate = lastDetails.getRetailSaleRate();

                        pr = (lineGrossRate != null) ? lineGrossRate.doubleValue() : 0.0;
                        rr = (lastRetailRate != null) ? lastRetailRate.doubleValue() : 0.0;
                        packRate = lastRetailRate != null ? lastRetailRate : BigDecimal.ZERO;

                    }
                }

                // Fallback logic
                if (pr == 0.0 || rr == 0.0) {
                    double fallbackPr = getPharmacyBean().getLastPurchaseRate(bi.getItem(), sessionController.getDepartment());
                    double fallbackRr = getPharmacyBean().getLastRetailRateByBillItemFinanceDetails(bi.getItem(), sessionController.getDepartment());
                    pr = fallbackPr > 0.0 ? fallbackPr : pr;
                    rr = fallbackRr > 0.0 ? fallbackRr : rr;
                    packRate = BigDecimal.valueOf(rr);
                }

                ph.setPurchaseRate(pr);
                ph.setRetailRate(rr);
                //TODO: Maange Wholesalerate as a seperate issue

                bi.setPharmaceuticalBillItem(ph);

                BillItemFinanceDetails fd = new BillItemFinanceDetails(bi);
                fd.setQuantity(java.math.BigDecimal.valueOf(remains));
                fd.setFreeQuantity(java.math.BigDecimal.valueOf(remainFreeQty));
                fd.setLineGrossRate(java.math.BigDecimal.valueOf(pr));
                fd.setLineDiscountRate(java.math.BigDecimal.ZERO);
                fd.setRetailSaleRate(java.math.BigDecimal.valueOf(rr));

                bi.setBillItemFinanceDetails(fd);
                pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(fd);

                getBillItems().add(bi);
                //  getBillItems().r
            }

        }

    }

    public void generateBillComponent(Bill importGrnBill) {

        for (BillItem importBi : importGrnBill.getBillItems()) {
            PharmaceuticalBillItem i = importBi.getPharmaceuticalBillItem();
            double remains = i.getQty() - getPharmacyCalculation().calculateRemainigQtyFromOrder(i);
            double remainFreeQty = i.getFreeQty() - getPharmacyCalculation().calculateRemainingFreeQtyFromOrder(i);

            if (remains > 0 || remainFreeQty > 0) {
                BillItem bi = new BillItem();
                bi.setSearialNo(getBillItems().size());
                bi.setItem(i.getBillItem().getItem());
                bi.setReferanceBillItem(i.getBillItem());
                bi.setQty(remains);
                bi.setTmpQty(remains);
                bi.setTmpFreeQty(remainFreeQty);
                //Set Suggession
//                bi.setTmpSuggession(getPharmacyCalculation().getSuggessionOnly(bi.getItem()));

                PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
                ph.setBillItem(bi);
                double tmpQty = bi.getQty();
                double tmpFreeQty = remainFreeQty;

                bi.setPreviousRecieveQtyInUnit(tmpQty);
                bi.setPreviousRecieveFreeQtyInUnit(tmpFreeQty);

                ph.setQty(tmpQty);
                ph.setQtyInUnit(tmpQty);

                ph.setFreeQtyInUnit(tmpFreeQty);
                ph.setFreeQty(tmpFreeQty);

                ph.setPurchaseRate(i.getPurchaseRate());
                ph.setRetailRate(i.getRetailRate());

                ph.setDoe(i.getDoe());
                ph.setStringValue(i.getStringValue());

                ph.setWholesaleRate(getWholesaleRate(ph.getPurchaseRate(), ph.getQtyInUnit(), ph.getFreeQtyInUnit()));

                ph.setLastPurchaseRate(getPharmacyBean().getLastPurchaseRate(bi.getItem(), getSessionController().getDepartment()));

                bi.setPharmaceuticalBillItem(ph);

                BillItemFinanceDetails fd = new BillItemFinanceDetails(bi);
                fd.setQuantity(java.math.BigDecimal.valueOf(remains));
                fd.setFreeQuantity(java.math.BigDecimal.valueOf(remainFreeQty));
                fd.setLineGrossRate(java.math.BigDecimal.valueOf(ph.getPurchaseRate()));
                double unitsPerPack = 1.0;
                if (bi.getItem() instanceof com.divudi.core.entity.pharmacy.Ampp) {
                    unitsPerPack = bi.getItem().getDblValue();
                }
                fd.setRetailSaleRatePerUnit(java.math.BigDecimal.valueOf(ph.getRetailRate() / unitsPerPack));
                bi.setBillItemFinanceDetails(fd);
                pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(fd);

                getBillItems().add(bi);
                //  getBillItems().r
            }

        }
    }

    public void generateBillComponentAll() {

        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getApproveBill())) {
            double remains = i.getQtyInUnit() - getPharmacyCalculation().calculateRemainigQtyFromOrder(i);

            BillItem bi = new BillItem();
            bi.setSearialNo(getBillItems().size());
            bi.setItem(i.getBillItem().getItem());
            bi.setReferanceBillItem(i.getBillItem());
            bi.setQty(remains);
            bi.setTmpQty(remains);
            //Set Suggession
//                bi.setTmpSuggession(getPharmacyCalculation().getSuggessionOnly(bi.getItem()));

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.setBillItem(bi);
            double tmpQty = bi.getQty();

            ph.setQty(tmpQty);
            ph.setQtyInUnit(tmpQty);

            ph.setFreeQty(i.getFreeQty());
            ph.setFreeQtyInUnit(i.getFreeQty());

            ph.setPurchaseRate(i.getPurchaseRate());
            ph.setRetailRate(i.getRetailRate());

            ph.setWholesaleRate(getWholesaleRate(ph.getPurchaseRate(), ph.getQtyInUnit(), ph.getFreeQtyInUnit()));

            ph.setLastPurchaseRate(getPharmacyBean().getLastPurchaseRate(bi.getItem(), getSessionController().getDepartment()));
            ph.setFreeQty(i.getFreeQty());

            bi.setPharmaceuticalBillItem(ph);

            BillItemFinanceDetails fd = new BillItemFinanceDetails(bi);
            fd.setQuantity(java.math.BigDecimal.valueOf(remains));
            fd.setFreeQuantity(java.math.BigDecimal.valueOf(i.getFreeQty()));
            fd.setLineGrossRate(java.math.BigDecimal.valueOf(ph.getPurchaseRate()));
            double unitsPerPack = 1.0;
            if (bi.getItem() instanceof com.divudi.core.entity.pharmacy.Ampp) {
                unitsPerPack = bi.getItem().getDblValue();
            }
            fd.setRetailSaleRatePerUnit(java.math.BigDecimal.valueOf(ph.getRetailRate() / unitsPerPack));
            bi.setBillItemFinanceDetails(fd);
            pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(fd);

            getBillItems().add(bi);
            //  getBillItems().r

        }
    }

    public void createGrn() {
        setFromInstitution(getApproveBill().getToInstitution());
        setReferenceInstitution(getSessionController().getLoggedUser().getInstitution());
        generateBillComponent();
        pharmacyCostingService.distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        calculateBillTotalsFromItems();
//        calGrossTotal();
    }

    public void calculateBillTotalsFromItems() {
        int serialNo = 0;

        
        // Bill-level inputs: do not calculate here
        BigDecimal billDiscount = BigDecimal.valueOf(getGrnBill().getDiscount());
        BigDecimal billExpense = BigDecimal.ZERO;
        BigDecimal billTax = BigDecimal.ZERO;
        BigDecimal billCost = BigDecimal.ZERO;

        // Totals from bill items
        BigDecimal totalLineDiscounts = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalLineExpenses = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalCostLines = BigDecimal.ZERO;

        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalTaxLines = BigDecimal.ZERO;

        BigDecimal totalFreeItemValue = BigDecimal.ZERO;
        BigDecimal totalPurchase = BigDecimal.ZERO;
        BigDecimal totalRetail = BigDecimal.ZERO;
        BigDecimal totalWholesale = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalFreeQty = BigDecimal.ZERO;
        BigDecimal totalQtyAtomic = BigDecimal.ZERO;
        BigDecimal totalFreeQtyAtomic = BigDecimal.ZERO;

        BigDecimal grossTotal = BigDecimal.ZERO;
        BigDecimal lineGrossTotal = BigDecimal.ZERO;
        BigDecimal netTotal = BigDecimal.ZERO;
        BigDecimal lineNetTotal = BigDecimal.ZERO;

        for (BillItem bi : getBillItems()) {
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();

            if (bi.getItem() instanceof Ampp) {
                bi.setQty(pbi.getQtyPacks());
                bi.setRate(pbi.getPurchaseRatePack());
            } else if (bi.getItem() instanceof Amp) {
                bi.setQty(pbi.getQty());
                bi.setRate(pbi.getPurchaseRate());
            }

            bi.setSearialNo(serialNo++);
            double netValue = bi.getQty() * bi.getRate();
            bi.setNetValue(0 - netValue);


            if (f != null) {
                BigDecimal qty = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);
                BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);
                BigDecimal costRate = Optional.ofNullable(f.getLineCostRate()).orElse(BigDecimal.ZERO);
                BigDecimal retailRate = Optional.ofNullable(f.getRetailSaleRate()).orElse(BigDecimal.ZERO);
                BigDecimal wholesaleRate = Optional.ofNullable(f.getWholesaleRate()).orElse(BigDecimal.ZERO);

                BigDecimal qtyTotal = qty.add(freeQty);
                BigDecimal retailValue = retailRate.multiply(qtyTotal);
                BigDecimal wholesaleValue = wholesaleRate.multiply(qtyTotal);
                BigDecimal freeItemValue = costRate.multiply(freeQty);

                totalLineDiscounts = totalLineDiscounts.add(Optional.ofNullable(f.getLineDiscount()).orElse(BigDecimal.ZERO));
                totalLineExpenses = totalLineExpenses.add(Optional.ofNullable(f.getLineExpense()).orElse(BigDecimal.ZERO));
                totalTaxLines = totalTaxLines.add(Optional.ofNullable(f.getLineTax()).orElse(BigDecimal.ZERO));
                totalCostLines = totalCostLines.add(Optional.ofNullable(f.getLineCost()).orElse(BigDecimal.ZERO));

                totalDiscount = totalDiscount.add(Optional.ofNullable(f.getTotalDiscount()).orElse(BigDecimal.ZERO));
                totalExpense = totalExpense.add(Optional.ofNullable(f.getTotalExpense()).orElse(BigDecimal.ZERO));
                totalCost = totalCost.add(Optional.ofNullable(f.getTotalCost()).orElse(BigDecimal.ZERO));
                totalTax = totalTax.add(Optional.ofNullable(f.getTotalTax()).orElse(BigDecimal.ZERO));

                totalFreeItemValue = totalFreeItemValue.add(freeItemValue);
                totalPurchase = totalPurchase.add(Optional.ofNullable(f.getGrossTotal()).orElse(BigDecimal.ZERO));
                totalRetail = totalRetail.add(retailValue);
                totalWholesale = totalWholesale.add(wholesaleValue);

                totalQty = totalQty.add(qty);
                totalFreeQty = totalFreeQty.add(freeQty);
                totalQtyAtomic = totalQtyAtomic.add(Optional.ofNullable(f.getQuantityByUnits()).orElse(BigDecimal.ZERO));
                totalFreeQtyAtomic = totalFreeQtyAtomic.add(Optional.ofNullable(f.getFreeQuantityByUnits()).orElse(BigDecimal.ZERO));

                grossTotal = grossTotal.add(Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO));
                lineGrossTotal = lineGrossTotal.add(Optional.ofNullable(f.getLineGrossTotal()).orElse(BigDecimal.ZERO));

                netTotal = netTotal.add(Optional.ofNullable(f.getNetTotal()).orElse(BigDecimal.ZERO));
                lineNetTotal = lineNetTotal.add(Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO));
            }

        }

        // Assign legacy totals
        getGrnBill().setTotal(grossTotal.doubleValue());
        getGrnBill().setNetTotal(netTotal.doubleValue());
        getGrnBill().setSaleValue(totalRetail.doubleValue());

        // Assign to BillFinanceDetails
        BillFinanceDetails bfd = getGrnBill().getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails(getGrnBill());
            getGrnBill().setBillFinanceDetails(bfd);
        }

        // Inputs from user or UI – left unchanged if already set
        bfd.setBillDiscount(bfd.getBillDiscount() != null ? bfd.getBillDiscount() : billDiscount);
        bfd.setBillExpense(bfd.getBillExpense() != null ? bfd.getBillExpense() : billExpense);
        bfd.setBillTaxValue(bfd.getBillTaxValue() != null ? bfd.getBillTaxValue() : billTax);
        bfd.setBillCostValue(bfd.getBillCostValue() != null ? bfd.getBillCostValue() : billCost);

        // Assign calculated from items
        bfd.setLineDiscount(totalLineDiscounts);
        bfd.setLineExpense(totalLineExpenses);
        bfd.setItemTaxValue(totalTaxLines);
        bfd.setLineCostValue(totalCostLines);

        bfd.setTotalDiscount(totalDiscount);
        bfd.setTotalExpense(totalExpense);
        bfd.setTotalCostValue(totalCost);
        bfd.setTotalTaxValue(totalTax);

        bfd.setTotalOfFreeItemValues(totalFreeItemValue);
        bfd.setTotalPurchaseValue(totalPurchase);
        bfd.setTotalRetailSaleValue(totalRetail);
        bfd.setTotalWholesaleValue(totalWholesale);

        bfd.setTotalQuantity(totalQty);
        bfd.setTotalFreeQuantity(totalFreeQty);
        bfd.setTotalQuantityInAtomicUnitOfMeasurement(totalQtyAtomic);
        bfd.setTotalFreeQuantityInAtomicUnitOfMeasurement(totalFreeQtyAtomic);

        bfd.setGrossTotal(grossTotal);
        bfd.setLineGrossTotal(lineGrossTotal);
        bfd.setNetTotal(netTotal);
        bfd.setLineNetTotal(lineNetTotal);
    }

    public void createGrn(Bill importGrn) {
        setFromInstitution(importGrn.getToInstitution());
        setReferenceInstitution(importGrn.getDepartment().getInstitution());
        generateBillComponent(importGrn);
        calGrossTotal();
    }

    public void createGrnAll() {
        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setFromInstitution(getApproveBill().getToInstitution());
        getGrnBill().setReferenceInstitution(getSessionController().getLoggedUser().getInstitution());
        generateBillComponentAll();
        calGrossTotal();
    }

    public void createGrnWholesale() {
        getGrnBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_WHOLESALE);
        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setFromInstitution(getApproveBill().getToInstitution());
        getGrnBill().setReferenceInstitution(getSessionController().getLoggedUser().getInstitution());
        generateBillComponent();
        calGrossTotal();
    }

    private double getRetailPrice(BillItem billItem) {
        String sql = "select (p.retailRate) from PharmaceuticalBillItem p where p.billItem=:b";
        HashMap hm = new HashMap();
        hm.put("b", billItem.getReferanceBillItem());
        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    private double getWholesaleRate(double purchaseRate, double qtyInUnit, double freeQtyInUnit) {
        double wholesaleFactor =
            configOptionApplicationController.getDoubleValueByKey("Wholesale Rate Factor", 1.08);
        return (purchaseRate * wholesaleFactor) * qtyInUnit / (freeQtyInUnit + qtyInUnit);
    }

    private void applyFinanceDetailsToPharmaceutical(BillItem bi) {
        if (bi == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();

        if (f == null || pbi == null) {
            return;
        }

        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(f);

        if (bi.getItem() instanceof com.divudi.core.entity.pharmacy.Ampp) {
            BigDecimal unitsPerPack = Optional.ofNullable(f.getUnitsPerPack())
                    .orElse(BigDecimal.ONE);
            BigDecimal qtyUnits = Optional.ofNullable(f.getQuantity())
                    .orElse(BigDecimal.ZERO)
                    .multiply(unitsPerPack);
            BigDecimal freeQtyUnits = Optional.ofNullable(f.getFreeQuantity())
                    .orElse(BigDecimal.ZERO)
                    .multiply(unitsPerPack);

            pbi.setQty(qtyUnits.doubleValue());
            pbi.setQtyInUnit(pbi.getQty());
            pbi.setQtyPacks(Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO).doubleValue());

            pbi.setFreeQty(freeQtyUnits.doubleValue());
            pbi.setFreeQtyInUnit(pbi.getFreeQty());
            pbi.setFreeQtyPacks(Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO).doubleValue());

            pbi.setPurchaseRate(java.util.Optional.ofNullable(f.getNetRate()).orElse(java.math.BigDecimal.ZERO).doubleValue());
            pbi.setPurchaseRatePack(pbi.getPurchaseRate());

            pbi.setRetailRate(java.util.Optional.ofNullable(f.getRetailSaleRate()).orElse(java.math.BigDecimal.ZERO).doubleValue());
            pbi.setRetailRatePack(pbi.getRetailRate());
            pbi.setRetailRateInUnit(java.util.Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(java.math.BigDecimal.ZERO).doubleValue());
        } else {
            pbi.setQty(java.util.Optional.ofNullable(f.getQuantityByUnits()).orElse(java.math.BigDecimal.ZERO).doubleValue());
            pbi.setQtyInUnit(pbi.getQty());
            pbi.setQtyPacks(pbi.getQty());

            pbi.setFreeQty(java.util.Optional.ofNullable(f.getFreeQuantityByUnits()).orElse(java.math.BigDecimal.ZERO).doubleValue());
            pbi.setFreeQtyInUnit(pbi.getFreeQty());
            pbi.setFreeQtyPacks(pbi.getFreeQty());

            pbi.setPurchaseRate(java.util.Optional.ofNullable(f.getNetRate()).orElse(java.math.BigDecimal.ZERO).doubleValue());
            pbi.setPurchaseRatePack(pbi.getPurchaseRate());

            double r = java.util.Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(java.math.BigDecimal.ZERO).doubleValue();
            pbi.setRetailRate(r);
            pbi.setRetailRatePack(r);
            pbi.setRetailRateInUnit(r);
        }
    }

    public void onEdit(RowEditEvent event) {
        BillItem editingBillItem = (BillItem) event.getObject();
        setBatch(editingBillItem);
        onEdit(editingBillItem);
    }

    public void checkQty(BillItem bi) {

        if (bi.getTmpQty() < 0.0) {
            bi.setTmpQty(0.0);
        }

        if (bi.getTmpFreeQty() < 0.0) {
            bi.setTmpFreeQty(0.0);
        }

        onEdit(bi);
    }

    public void onEdit(BillItem tmp) {
        setBatch(tmp);
        BillItemFinanceDetails f = tmp.getBillItemFinanceDetails();
        if (f == null) {
            return;
        }
        double remains = getPharmacyCalculation().getRemainingQty(tmp.getPharmaceuticalBillItem());
        if (remains < f.getQuantity().doubleValue()) {
            f.setQuantity(java.math.BigDecimal.valueOf(remains));
            tmp.setTmpQty(remains);
            JsfUtil.addErrorMessage("You cant Change Qty than Remaining qty");
        }

        if (f.getLineGrossRate().compareTo(f.getRetailSaleRatePerUnit()) > 0) {
            f.setRetailSaleRatePerUnit(f.getLineGrossRate());
            JsfUtil.addErrorMessage("You cant set retail price below purchase rate");
        }
        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(f);
        calculateBillTotalsFromItems();
        calDifference();
    }

    public void onEditPurchaseRate(BillItem tmp) {
        double retail = tmp.getPharmaceuticalBillItem().getPurchaseRate() + (tmp.getPharmaceuticalBillItem().getPurchaseRate() * (getPharmacyBean().getMaximumRetailPriceChange() / 100));
        tmp.getPharmaceuticalBillItem().setRetailRate(retail);
    }

    @Deprecated
    public void calGrossTotal() {
        double tmp = 0.0;
        int serialNo = 0;
        for (BillItem p : getBillItems()) {
            BillItemFinanceDetails f = p.getBillItemFinanceDetails();
            if (f != null) {
                java.math.BigDecimal rate = java.util.Optional.ofNullable(f.getLineGrossRate()).orElse(java.math.BigDecimal.ZERO);
                java.math.BigDecimal qty = java.util.Optional.ofNullable(f.getQuantity()).orElse(java.math.BigDecimal.ZERO);
                tmp += rate.multiply(qty).doubleValue();
            }
            p.setSearialNo(serialNo++);
        }
        discountChangedLitener();
    }

    public void calGrossTotalForGrnPreBill() {
        double tmp = 0.0;
        int serialNo = 0;
        for (BillItem p : getBillItems()) {
            BillItemFinanceDetails f = p.getBillItemFinanceDetails();
            if (f != null) {
                java.math.BigDecimal rate = java.util.Optional.ofNullable(f.getLineGrossRate()).orElse(java.math.BigDecimal.ZERO);
                java.math.BigDecimal qty = java.util.Optional.ofNullable(f.getQuantity()).orElse(java.math.BigDecimal.ZERO);
                tmp += rate.multiply(qty).doubleValue();
            }
            p.setSearialNo(serialNo++);
        }

        getGrnBill().setTotal(0 - tmp);
        discountChangedLitener();
    }

    public void discountChangedLitener() {
        pharmacyCostingService.distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        calculateBillTotalsFromItems();
        calDifference();
    }


    public void saveBillFee(BillItem bi) {
        saveBillFee(bi, null);
    }

    public void saveBillFee(BillItem bi, Payment p) {
        BillFee bf = new BillFee();
        bf.setCreatedAt(Calendar.getInstance().getTime());
        bf.setCreater(getSessionController().getLoggedUser());
        bf.setBillItem(bi);
        bf.setPatienEncounter(bi.getBill().getPatientEncounter());
        bf.setPatient(bi.getBill().getPatient());
        bf.setFeeValue(bi.getNetValue());
        bf.setFeeGrossValue(bi.getGrossValue());
        bf.setSettleValue(bi.getNetValue());
        bf.setCreatedAt(new Date());
        bf.setDepartment(getSessionController().getDepartment());
        bf.setInstitution(getSessionController().getInstitution());
        bf.setBill(bi.getBill());

        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }
        // Bill Fee Payment Concept is no loger used. 
        // createBillFeePaymentAndPayment(bf, p);
    }

    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        setPaymentMethodData(p, pm);
        return p;
    }

    public void setPaymentMethodData(Payment p, PaymentMethod pm) {

        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);

        p.setPaidValue(p.getBill().getNetTotal());

        if (p.getId() == null) {
            getPaymentFacade().create(p);
        }

    }

    @Deprecated // THis is NO longer Needed
    public void createBillFeePaymentAndPayment(BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(bf.getSettleValue());
        bfp.setInstitution(getSessionController().getInstitution());
        bfp.setDepartment(getSessionController().getDepartment());
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        getBillFeePaymentFacade().create(bfp);
    }

    public BillItem getCurrentExpense() {
        if (currentExpense == null) {
            currentExpense = new BillItem();
            currentExpense.setQty(1.0);
        }
        return currentExpense;
    }

    public BilledBill getBill() {
        if (bill == null) {
            bill = new BilledBill();
            bill.setBillType(BillType.StorePurchase);
            bill.setReferenceInstitution(getSessionController().getInstitution());
        }
        return bill;
    }

    public void addExpense() {
        if (getBill().getId() == null) {
            getBillFacade().create(getBill());
        }
        if (getCurrentExpense().getItem() == null) {
            JsfUtil.addErrorMessage("Expense ?");
            return;
        }
        if (currentExpense.getQty() == null || currentExpense.getQty().equals(0.0)) {
            currentExpense.setQty(1.0);
        }
        if (currentExpense.getNetRate() == 0.0) {
            currentExpense.setNetRate(currentExpense.getRate());
        }

        currentExpense.setNetValue(currentExpense.getNetRate() * currentExpense.getQty());
        currentExpense.setGrossValue(currentExpense.getRate() * currentExpense.getQty());

        getCurrentExpense().setSearialNo(getBillExpenses().size());
        getBillExpenses().add(currentExpense);
        currentExpense = null;

    }

    public double calExpenses() {
        double tot = 0.0;
        for (BillItem be : billExpenses) {
            tot = tot + be.getNetValue();
        }
        return tot;
    }

    public void calTotal() {
        double tot = 0.0;
        double exp = 0.0;
        int serialNo = 0;
        for (BillItem p : getBillItems()) {
            p.setQty(p.getPharmaceuticalBillItem().getQtyInUnit());
            p.setRate(p.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            serialNo++;
            p.setSearialNo(serialNo);
            if (p.getParentBillItem() == null) {
                double netValue = p.getQty() * p.getRate();
                p.setNetValue(0 - netValue);
                tot += p.getNetValue();
            }
        }

        for (BillItem e : getBillExpenses()) {
            double nv = e.getNetRate() * e.getQty();
            e.setNetValue(0 - nv);
            exp += e.getNetValue();
        }

        getBill().setExpenseTotal(exp);
        getBill().setTotal(tot);
        getBill().setNetTotal(tot + exp);

    }

//    public double getNetTotal() {
//
//        double tmp = getGrnBill().getTotal() + getGrnBill().getTax() - getGrnBill().getDiscount();
//        getGrnBill().setNetTotal(tmp);
//
//        return tmp;
//    }
    public void setApproveBill(Bill approveBill) {
        this.approveBill = approveBill;
//        grnBill = null;
//        dealor = null;
//        pos = null;
//        printPreview = false;
//        billItems = null;
//        createGrn();
    }

    public Bill getGrnBill() {
        if (grnBill == null) {
            grnBill = new BilledBill();
            grnBill.setBillType(BillType.PharmacyGrnBill);
            grnBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN);
            grnBill.setBillFinanceDetails(new BillFinanceDetails(grnBill));
        }
        return grnBill;
    }

    public void setGrnBill(Bill grnBill) {
        this.grnBill = grnBill;
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

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
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

    public CategoryFacade getCategoryFacade() {
        return categoryFacade;
    }

    public void setCategoryFacade(CategoryFacade categoryFacade) {
        this.categoryFacade = categoryFacade;
    }

    public ItemBatchFacade getItemBatchFacade() {
        return itemBatchFacade;
    }

    public void setItemBatchFacade(ItemBatchFacade itemBatchFacade) {
        this.itemBatchFacade = itemBatchFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public AmpFacade getAmpFacade() {
        return ampFacade;
    }

    public void setAmpFacade(AmpFacade ampFacade) {
        this.ampFacade = ampFacade;
    }


    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

//    public List<BillItem> getBillItems() {
//        if (billItems == null) {
//            billItems = new ArrayList<>();
//        }
//        return billItems;
//    }
//
//    public void setBillItems(List<BillItem> billItems) {
//        this.billItems = billItems;
//    }
    public String getTxtSearch() {
        return txtSearch;
    }

    public void setTxtSearch(String txtSearch) {
        this.txtSearch = txtSearch;
    }

    public List<BillItem> getBillItems() {
        if (billItems == null) {
            billItems = new ArrayList<>();
            // serialNo = 0;
        }
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public List<BillItem> getSelectedBillItems() {
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public PharmacyCalculation getPharmacyCalculation() {
        return pharmacyCalculation;
    }

    public void setPharmacyCalculation(PharmacyCalculation pharmacyCalculation) {
        this.pharmacyCalculation = pharmacyCalculation;
    }

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public double getInsTotal() {
        return insTotal;
    }

    public void setInsTotal(double insTotal) {
        this.insTotal = insTotal;
    }

    public double getDifference() {
        return difference;
    }

    public void setDifference(double difference) {
        this.difference = difference;
    }

    public Bill getCurrentGrnBillPre() {
        if (currentGrnBillPre == null) {
            currentGrnBillPre = new BilledBill();
            currentGrnBillPre.setBillType(BillType.PharmacyGrnBill);
            currentGrnBillPre.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_PRE);
        }
        return currentGrnBillPre;
    }

    public void setCurrentGrnBillPre(Bill currentGrnBillPre) {
        this.currentGrnBillPre = currentGrnBillPre;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getReferenceInstitution() {
        return referenceInstitution;
    }

    public void setReferenceInstitution(Institution referenceInstitution) {
        this.referenceInstitution = referenceInstitution;
    }

    public Date getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(Date invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }


    public List<BillItem> getBillExpenses() {
        if (billExpenses == null) {
            billExpenses = new ArrayList<>();
        }
        return billExpenses;
    }

    public void setBillExpenses(List<BillItem> billExpenses) {
        this.billExpenses = billExpenses;
    }

    private void saveImportBill(Bill importGrn) {
        importGrn.setBillType(BillType.PharmacyGrnBillImport);
        importGrn.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_PRE);
        if (importGrn.getId() == null) {
            billFacade.create(importGrn);
        } else {
            billFacade.edit(importGrn);
        }
        for (BillItem bi : importGrn.getBillItems()) {
            if (bi.getId() == null) {
                billItemFacade.create(bi);
            } else {
                billItemFacade.edit(bi);
            }
        }
    }

}
