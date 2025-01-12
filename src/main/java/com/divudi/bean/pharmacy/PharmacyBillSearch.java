/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.bean.inward.InwardBeanController;
import com.divudi.bean.store.StoreBillSearch;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.ejb.EjbApplication;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.service.StaffService;
import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillEntry;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillFeePayment;
import com.divudi.entity.BillItem;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Payment;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillFeePaymentFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemBatchFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
import com.divudi.entity.PreBill;
import com.divudi.entity.StockBill;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.joda.time.LocalDate;
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.LazyDataModel;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PharmacyBillSearch implements Serializable {

    /**
     * EJBs
     */
    /**
     * Controllers
     */
    @Inject
    PharmacyCalculation pharmacyCalculation;

    /**
     * Properties
     */
    private boolean printPreview = false;
    private double refundAmount;
    String txtSearch;
    private String comment;
    Bill bill;
    PaymentMethod paymentMethod;
    PaymentScheme paymentScheme;
    private RefundBill billForRefund;
    @Temporal(TemporalType.TIME)
    private Date fromDate;
    @Temporal(TemporalType.TIME)
    private Date toDate;
    //  private String comment;
    WebUser user;
    StoreBillSearch storeBillSearch;
    ////////////////
    List<BillItem> refundingItems;
    List<Bill> bills;
    private List<Bill> filteredBill;
    private List<Bill> selectedBills;
    List<BillEntry> billEntrys;
    List<BillItem> billItems;
    List<BillComponent> billComponents;
    List<BillFee> billFees;
    private List<BillItem> tempbillItems;
    List<Bill> searchRetaiBills;
    //////////////////
    @EJB
    StaffService staffBean;

    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    private BillItemFacade billItemFacede;
    @EJB
    private BillComponentFacade billCommponentFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    ////////////////////

    private CommonFunctions commonFunctions;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    EjbApplication ejbApplication;
    @EJB
    BillFeePaymentFacade billFeePaymentFacade;
    ///////////////////
    @Inject
    SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    InwardBeanController inwardBean;
    @Inject
    PharmacySaleController pharmacySaleController;
    @Inject
    DrawerController drawerController;
    @Inject
    PharmacyRequestForBhtController pharmacyRequestForBhtController;

    public String navigateToCancelPharmacyDirectIssueToInpatients() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        return "/inward/pharmacy_cancel_bill_retail_bht?faces-redirect=true";
    }

    public String navigatePharmacyReprintPo() {
        return "pharmacy_reprint_po?faces-redirect=true";
    }

    public String editInwardPharmacyRequestBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Not Bill Found !");
            return "";
        }
        pharmacyRequestForBhtController.setBillPreview(false);
        pharmacyRequestForBhtController.setPatientEncounter(bill.getPatientEncounter());
        pharmacyRequestForBhtController.setDepartment(bill.getFromDepartment());
        pharmacyRequestForBhtController.setPreBill((PreBill) bill);
        System.out.println("setPatientEncounter" + bill.getPatientEncounter());
        System.out.println("setDepartment" + bill.getFromDepartment());
        billFacade.edit(bill);
        return "/ward/ward_pharmacy_bht_issue_request_edit?faces-redirect=true";
    }

    public String cancelInwardPharmacyRequestBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Not Bill Found !");
            return "";
        }
        if (comment == null) {
            JsfUtil.addErrorMessage("Provide Comment To Cancel !");
            return "";
        }
        CancelledBill cb = pharmacyCreateCancelBill();
        cb.setBillItems(getBill().getBillItems());
        bill.setCancelled(true);
        bill.setCancelledBill(cb);
        billFacade.edit(bill);
        return "/ward/ward_pharmacy_bht_issue_request_list_for_issue?faces-redirect=true";
    }

    public String cancelInwardPharmacyRequestBillFromInward() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Not Bill Found !");
            return "";
        }
        if (comment == null) {
            JsfUtil.addErrorMessage("Provide Comment To Cancel !");
            return "";
        }
        CancelledBill cb = pharmacyCreateCancelBill();
        cb.setBillItems(getBill().getBillItems());
        bill.setCancelled(true);
        bill.setCancelledBill(cb);
        billFacade.edit(bill);
        return "/ward/ward_pharmacy_bht_issue_request_bill_search?faces-redirect=true";
    }

    public String cancelPharmacyTransferRequestBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Not Bill Found !");
            return "";
        }
        CancelledBill cb = pharmacyCreateCancelBill();
        cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_TRANSFER_REQUEST_CANCELLED);
        cb.setBillItems(getBill().getBillItems());
        bill.setCancelled(true);
        bill.setCancelledBill(cb);
        billFacade.edit(bill);
        return "/pharmacy/pharmacy_transfer_request_list?faces-redirect=true";
    }

    public void markAsChecked() {
        if (bill == null) {
            return;
        }

        if (bill.getPatientEncounter() == null) {
            return;
        }

        if (bill.getPatientEncounter().isPaymentFinalized()) {
            return;
        }

        bill.setCheckeAt(new Date());
        bill.setCheckedBy(getSessionController().getLoggedUser());

        getBillFacade().edit(bill);

        JsfUtil.addSuccessMessage("Mark as Checked");

    }

    public void markAsUnChecked() {
        if (bill == null) {
            return;
        }

        if (bill.getPatientEncounter() == null) {
            return;
        }

        if (bill.getPatientEncounter().isPaymentFinalized()) {
            return;
        }

        bill.setCheckeAt(null);
        bill.setCheckedBy(null);

        getBillFacade().edit(bill);

        JsfUtil.addSuccessMessage("Mark As Un Check");

    }

    public void unitCancell() {

        if (bill == null) {
            JsfUtil.addErrorMessage("Please Select a Bill");
            return;
        }

        if (checkIssueReturn(getBill())) {
            JsfUtil.addErrorMessage("Issue Bill had been Returned You can't cancell bill ");
            return;
        }

        if (getBill().getComments() == null || getBill().getComments().trim().equals("")) {
            JsfUtil.addErrorMessage("Please Enter Comments ");
            return;
        }

        if (checkDepartment(getBill())) {
            return;
        }

        Bill prebill = getPharmacyBean().reAddToStock(getBill(), getSessionController().getLoggedUser(),
                getSessionController().getDepartment(), BillNumberSuffix.ISSCAN);

        if (prebill != null) {
            getBill().setCancelled(true);
            getBill().setCancelledBill(prebill);
            getBill().setReferenceBill(prebill);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Canceled");
            printPreview = true;
        }
    }

    public void cancelBill() {
        if (getBill() == null) {
            JsfUtil.addErrorMessage("Please Select a Bill");
            return;
        }

        if (checkIssueReturn(getBill())) {
            JsfUtil.addErrorMessage("Issue Bill had been Returned. You can't cancel the bill.");
            return;
        }

        if (getBill().getComments() == null || getBill().getComments().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please Enter Comments");
            return;
        }

        if (checkDepartment(getBill())) {
            return;
        }

//        Bill prebill = getPharmacyBean().reAddToStock(getBill(), getSessionController().getLoggedUser(),
//                getSessionController().getDepartment(), BillNumberSuffix.ISSCAN);
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Canceled");
            return;
        }

        try {
            createCancellationBill();
            JsfUtil.addSuccessMessage("Canceled");
            printPreview = true;
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Failed to cancel the bill: " + e.getMessage());
            // Log the exception
        }
    }

    private void createCancellationBill() {
        CancelledBill c = createCancelledBillInstance(getBill());

        List<BillItem> billItems = savePreBillItems(c, sessionController.getLoggedUser(),
                sessionController.getLoggedUser().getDepartment());
        c.setBillItems(billItems);

        if (c.getStockBill() != null) {
            c.getStockBill().invertStockBillValues(getBill());
        } else {
            // Ensure getBill().getStockBill() is not null
            StockBill stockBill = getBill().getStockBill();
            if (stockBill != null) {
                c.setStockBill(stockBill);
                c.getStockBill().invertStockBillValues(getBill());
            } else {
                // Handle the case where there is no StockBill (either log or throw an exception)
                System.out.println("No StockBill available in getBill()");
            }
        }

        getBillFacade().edit(c);

        bill.setForwardReferenceBill(c);
        bill.setCancelled(true);
        bill.setCancelledBill(c);
        bill.setReferenceBill(c);
        getBillFacade().edit(bill);
    }

    private CancelledBill createCancelledBillInstance(Bill b) {
        CancelledBill c = new CancelledBill();
        c.copy(b);
        c.copyValue(b);
        c.invertQty();
        c.setBilledBill(getBill());
        c.setDeptId(getBillNumberBean().institutionBillNumberGenerator(
                sessionController.getLoggedUser().getDepartment(), getBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.ISSCAN));
        c.setInsId(getBillNumberBean().institutionBillNumberGenerator(
                sessionController.getLoggedUser().getInstitution(), getBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.ISSCAN));
        c.setInstitution(sessionController.getLoggedUser().getInstitution());
        c.setDepartment(sessionController.getLoggedUser().getDepartment());
        c.setCreatedAt(new Date());
        c.setCreater(sessionController.getLoggedUser());
        c.setComments("Re Add To Stock");
        c.setBackwardReferenceBill(b);
        c.setReferenceBill(b);
        c.setBillClassType(BillClassType.CancelledBill);
        c.setBillType(BillType.PharmacyIssue);
        c.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ISSUE_CANCELLED);

        if (c.getId() == null) {
            getBillFacade().create(c);
        }
        return c;
    }

    private List<BillItem> savePreBillItems(CancelledBill cancelBill, WebUser user, Department department) {
        List<BillItem> billItems = new ArrayList<>();

        if (getBill() == null) {
            return billItems;
        }

        if (getBill().getBillItems() == null) {
            return billItems;
        }

        for (BillItem bItem : getBill().getBillItems()) {
            try {
                if (bItem == null) {
                    System.err.println("BillItem is null, skipping.");
                    continue;
                }

                BillItem newBillItem = new BillItem();
                newBillItem.copy(bItem);
                newBillItem.invertValue(bItem);
                newBillItem.setBill(cancelBill);
                newBillItem.setReferanceBillItem(bItem);
                newBillItem.setCreatedAt(new Date());
                newBillItem.setCreater(user);

                if (newBillItem.getId() == null) {
                    System.out.println("Creating BillItem: " + newBillItem);
                    getBillItemFacade().create(newBillItem);
                }

                PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
                if (bItem.getPharmaceuticalBillItem() != null) {
                    ph.copy(bItem.getPharmaceuticalBillItem());
                    ph.invertValue(bItem.getPharmaceuticalBillItem());
                    ph.setBillItem(newBillItem);

                    if (ph.getId() == null) {
                        System.out.println("Creating PharmaceuticalBillItem: " + ph);
                        getPharmaceuticalBillItemFacade().create(ph);
                    }

                    newBillItem.setPharmaceuticalBillItem(ph);
                    getBillItemFacade().edit(newBillItem);

                    double qty = (bItem.getQty() != null) ? Math.abs(bItem.getQty()) : 0.0;
                    if (ph.getStock() != null) {
                        getPharmacyBean().addToStock(ph.getStock(), qty, ph, department);
                    } else {
                        System.err.println("Stock is null for PharmaceuticalBillItem: " + ph);
                    }
                } else {
                    System.err.println("PharmaceuticalBillItem is null for BillItem: " + bItem);
                }

                billItems.add(newBillItem);
            } catch (Exception e) {
                System.err.println("Error processing BillItem: " + bItem);
                e.printStackTrace();
                throw e; // Re-throw to trigger transaction rollback
            }
        }

        return billItems;
    }

    public void unitIssueCancel() {

        if (bill == null) {
            JsfUtil.addErrorMessage("Please Select a Bill");
            return;
        }
        CancelledBill c = new CancelledBill();

        c.setBillDate(new Date());
        c.setBillTime(new Date());

        getBillFacade().create(c);
        c.setBillType(BillType.PharmacyIssue);
        c.setBilledBill(bill);
        c.setReferenceBill(bill);
        c.setCreater(getSessionController().getLoggedUser());
        c.setCreatedAt(new Date());

        c.setTotal(0.0 - bill.getTotal());
        c.setCashPaid(0.0 - bill.getCashPaid());
        c.setClaimableTotal(0.0 - bill.getClaimableTotal());
        c.setDiscount(0.0 - bill.getDiscount());
        c.setGrantTotal(0.0 - bill.getGrantTotal());
        c.setPaidAmount(0.0 - bill.getPaidAmount());
        c.setNetTotal(0.0 - bill.getNetTotal());
        c.setCancelled(true);

        List<BillItem> cancelBillItems = new ArrayList<>();
        List<PharmaceuticalBillItem> cancelPharmaciuticalBillItems = new ArrayList<>();

        for (BillItem bi : bill.getBillItems()) {
            BillItem canbi = new BillItem();

            canbi.setItem(bi.getItem());
            canbi.setItemId(bi.getItemId());

            canbi.setDiscount(0.0 - bi.getDiscount());
            canbi.setDiscountRate(0.0 - bi.getDiscountRate());
            canbi.setGrossValue(0.0 - bi.getGrossValue());
            canbi.setNetRate(0.0 - bi.getNetRate());
            canbi.setNetValue(0.0 - bi.getNetValue());
            canbi.setQty(0.0 - bi.getQty());
            canbi.setRate(0.0 - bi.getRate());
            canbi.setRemainingQty(0.0 - bi.getRemainingQty());
            canbi.setTmpQty(0.0 - bi.getTmpQty());
            canbi.setTotalGrnQty(0.0 - bi.getTotalGrnQty());
            canbi.setReferanceBillItem(bi);
            canbi.setBill(c);

            PharmaceuticalBillItem canphbi = new PharmaceuticalBillItem();
            canphbi.copy(bi.getPharmaceuticalBillItem());
            canphbi.invertValue(bi.getPharmaceuticalBillItem());
            canphbi.setBillItem(bi);
            if (canphbi.getId() == null) {
                getPharmaceuticalBillItemFacade().create(canphbi);
            }

            bi.setPharmaceuticalBillItem(canphbi);

            //System.err.println("QTY " + bi.getQty());
            double qty = 0;
            if (bi.getQty() != null) {
                qty = Math.abs(bi.getQty());
            }
            //Add to Stock
            getPharmacyBean().addToStock(bi.getPharmaceuticalBillItem(), qty, bill.getDepartment());
            getPharmacyBean().addToStock(canphbi, canphbi.getQty(), bill.getDepartment());

            getBillItemFacede().create(canbi);
            getPharmaceuticalBillItemFacade().edit(canphbi);

            cancelBillItems.add(canbi);
            cancelPharmaciuticalBillItems.add(canphbi);

        }

        c.setBillItems(cancelBillItems);
        getBillFacade().edit(bill);

        JsfUtil.addSuccessMessage("Canceled");
        printPreview = true;

    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    @EJB
    BillItemFacade billItemFacade;
    @Inject
    PriceMatrixController priceMatrixController;

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    public void updateMargin(List<BillItem> billItems, Bill bill, Department matrixDepartment, PaymentMethod paymentMethod) {
        double total = 0;
        double netTotal = 0;
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
        }

        bill.setTotal(total);
        bill.setNetTotal(netTotal);
        getBillFacade().edit(bill);

    }

    public void updateFeeMargin() {

        updateMargin(getBill().getBillItems(), getBill(), getBill().getFromDepartment(), getBill().getPatientEncounter().getPaymentMethod());

    }

    public void editBill() {
        if (errorCheckForEdit()) {
            return;
        }
        getBillFacade().edit(getBill());
    }

    public void editBill(Bill bill) {

        getBillFacade().edit(bill);
    }

    private boolean errorCheckForEdit() {
        ////System.out.println("error = " + getBill());

        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        if (getBill().getPaidAmount() != 0.0) {
            JsfUtil.addErrorMessage("Already Credit Company Paid For This Bill. Can not cancel.");
            return true;
        }

        return false;
    }

    private boolean errorCheckForEdit(Bill bill) {
        ////System.out.println("error = " + bill);

        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (bill.isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        if (bill.getPaidAmount() != 0.0) {
            JsfUtil.addErrorMessage("Already Credit Company Paid For This Bill. Can not cancel.");
            return true;
        }

        return false;
    }

    public void editBillItem(BillItem billItem) {
        ////System.out.println("billItem = " + billItem);

        if (errorCheckForEdit(billItem.getBill())) {
            return;
        }

        getBillItemFacede().edit(billItem);
        getPharmaceuticalBillItemFacade().edit(billItem.getPharmaceuticalBillItem());

        calTotalAndUpdate(billItem.getBill());
    }

    public void editBillItem2(BillItem billItem) {
        getBillItemFacede().edit(billItem);
        billItem.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - billItem.getQty()));
        getPharmaceuticalBillItemFacade().edit(billItem.getPharmaceuticalBillItem());

        calTotalAndUpdate2(billItem.getBill());
    }

    private void calTotalAndUpdate2(Bill bill) {
        double tmp = 0;
        for (BillItem b : bill.getBillItems()) {
            double tmp2 = (b.getPharmaceuticalBillItem().getQtyInUnit() * b.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate());
            tmp += tmp2;
        }

        bill.setTotal(tmp);
        bill.setNetTotal(tmp);
        getBillFacade().edit(bill);
    }

    private void calTotalAndUpdate(Bill bill) {
        double tmp = 0;
        for (BillItem b : bill.getBillItems()) {
            if (b.getPharmaceuticalBillItem() == null) {
                continue;
            }
            double tmp2 = (b.getPharmaceuticalBillItem().getQtyInUnit() * b.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            tmp += tmp2;
        }

        bill.setTotal(0 - tmp);
        bill.setNetTotal(0 - tmp);
        getBillFacade().edit(bill);
    }

    public void calTotalSaleRate(Bill bill) {
        double tmp = 0;
        for (BillItem b : bill.getBillItems()) {
            if (b.getPharmaceuticalBillItem() == null) {
                continue;
            }
            double tmp2 = (b.getPharmaceuticalBillItem().getQty() * b.getPharmaceuticalBillItem().getRetailRate());
            tmp += tmp2;
        }

        bill.setTransTotalSaleValue(tmp);

    }

    private boolean errorsPresentOnPoBillCancellation() {
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        return false;
    }

    public String cancelPoBill() {
        if (getSelectedBills() == null || getSelectedBills().isEmpty()) {
            JsfUtil.addErrorMessage("Please select Bills");
            return "";
        }
        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return "";
        }
        for (Bill selected : selectedBills) {
            setBill(selected);
            if (getBill() == null) {
                JsfUtil.addErrorMessage("No bill");
                continue;
            }
            if (getBill().getId() == null) {
                JsfUtil.addErrorMessage("No Saved bill");
                continue;
            }

            if (errorsPresentOnPoBillCancellation()) {
                continue;
            }

            CancelledBill cancellationBill = pharmacyCreateCancelBill();
            cancellationBill.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyPurchaseBill, BillClassType.CancelledBill, BillNumberSuffix.GRNCAN));
            cancellationBill.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyPurchaseBill, BillClassType.CancelledBill, BillNumberSuffix.GRNCAN));
            cancellationBill.setBillTypeAtomic(BillTypeAtomic.MULTIPLE_PHARMACY_ORDER_CANCELLED_BILL);

            if (cancellationBill.getId() == null) {
                getBillFacade().create(cancellationBill);
            }
            getBill().setCancelled(true);
            getBill().setCancelledBill(cancellationBill);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

        }
        selectedBills = null;
        return "";
    }

    public String navigateToViewPharmacyGrn() {
        return "/pharmacy/pharmacy_reprint_grn?faces-redirect=true;";
    }

    public String navigateToViewPurchaseOrder() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return "";
        }
        double tmp = 0;
        for (BillItem b : bill.getBillItems()) {
            if (b.getPharmaceuticalBillItem() == null) {
                continue;
            }
            double tmp2 = (b.getPharmaceuticalBillItem().getQty() * b.getPharmaceuticalBillItem().getRetailRate());
            tmp += tmp2;
        }
        bill.setTransTotalSaleValue(tmp);
        return "/pharmacy/pharmacy_reprint_grn?faces-redirect=true";
    }

    public String navigateToViewPharmacyBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return "";
        }
        double tmp = 0;
        for (BillItem b : bill.getBillItems()) {
            if (b.getPharmaceuticalBillItem() == null) {
                continue;
            }
            double tmp2 = (b.getPharmaceuticalBillItem().getQty() * b.getPharmaceuticalBillItem().getRetailRate());
            tmp += tmp2;
        }
        bill.setTransTotalSaleValue(tmp);
        return "/pharmacy/pharmacy_reprint_bill?faces-redirect=true";
    }

    public WebUser getUser() {
        return user;
    }

    public void onEdit(RowEditEvent event) {

        BillFee tmp = (BillFee) event.getObject();

        if (tmp.getPaidValue() != 0.0) {
            JsfUtil.addErrorMessage("Already Staff FeePaid");
            return;
        }

        getBillFeeFacade().edit(tmp);

    }

    public void setUser(WebUser user) {
        // recreateModel();
        this.user = user;
        recreateModel();
    }

    private LazyDataModel<Bill> lazyBills;

    public LazyDataModel<Bill> getSearchSaleBills() {
        return lazyBills;
    }

    public EjbApplication getEjbApplication() {
        return ejbApplication;
    }

    public void setEjbApplication(EjbApplication ejbApplication) {
        this.ejbApplication = ejbApplication;
    }

    public boolean calculateRefundTotal() {
        Double d = 0.0;
        //billItems=null;
        tempbillItems = null;
        for (BillItem i : getRefundingItems()) {
            if (checkPaidIndividual(i)) {
                JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Refund Bill");
                return false;
            }

            if (!i.isRefunded()) {
                d = d + i.getNetValue();
                getTempbillItems().add(i);
            }

        }
        refundAmount = d;
        return true;
    }

    public List<Bill> getUserBillsOwn() {
        List<Bill> userBills;
        if (getUser() == null) {
            userBills = new ArrayList<>();
            //////System.out.println("user is null");
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), getSessionController().getInstitution(), BillType.OpdBill);
            //////System.out.println("user ok");
        }
        if (userBills == null) {
            userBills = new ArrayList<>();
        }
        return userBills;
    }

    public List<Bill> getBillsOwn() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), getSessionController().getInstitution(), BillType.OpdBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), getSessionController().getInstitution(), BillType.OpdBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public StoreBillSearch getStoreBillSearch() {
        return storeBillSearch;
    }

    public void setStoreBillSearch(StoreBillSearch storeBillSearch) {
        this.storeBillSearch = storeBillSearch;
    }

    public List<BillItem> getRefundingItems() {
        return refundingItems;
    }

    public void setRefundingItems(List<BillItem> refundingItems) {
        this.refundingItems = refundingItems;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public PaymentMethod getPaymentMethod() {
        if (paymentMethod == null) {
            paymentMethod = getBill().getPaymentMethod();
        }
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void recreateModel() {
        billForRefund = null;
        refundAmount = 0.0;
        billFees = null;
//        billFees
        lazyBills = null;
        billComponents = null;
        billForRefund = null;
        filteredBill = null;
        billItems = null;
        bills = null;
        printPreview = false;
        tempbillItems = null;
        //  comment = null;
    }

    private boolean checkPaid() {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.bill.id=" + getBill().getId();
        List<BillFee> tempFe = getBillFeeFacade().findByJpql(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    private boolean checkPaidIndividual(BillItem bi) {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
        List<BillFee> tempFe = getBillFeeFacade().findByJpql(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    private boolean errorCheck() {
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        if (getBill().getPaidAmount() != 0.0) {
            JsfUtil.addErrorMessage("Already Credit Company Paid For This Bill. Can not cancel.");
            return true;
        }

        if (checkPaid()) {
            JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
            return true;
        }

        if (checkCollectedReported()) {
            JsfUtil.addErrorMessage("Sample Already collected can't cancel or report already issued");
            return true;
        }

        if (getBill().getBillType() != BillType.LabBill && getBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment scheme.");
            return true;
        }
        if (getBill().getComments() == null || getBill().getComments().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }

        return false;
    }

    private boolean pharmacyErrorCheck() {
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().getBillType() == BillType.PharmacyOrderApprove) {
            if (checkGrn()) {
                JsfUtil.addErrorMessage("Grn already head been Come u can't bill ");
                return true;
            }
        }

        if (getBill().getBillType() == BillType.PharmacyGrnBill) {
            if (checkGrnReturn()) {
                JsfUtil.addErrorMessage("Grn had been Returned u can't cancell bill ");
                return true;
            }
        }
        if (getBill().getBillType() == BillType.PharmacyPre) {
            if (checkSaleReturn(getBill())) {
                JsfUtil.addErrorMessage("Sale had been Returned u can't cancell bill ");
                return true;
            }
        }

        if (getBill().getBillType() == BillType.PharmacySale) {
            if (checkSaleReturn(getBill().getReferenceBill())) {
                JsfUtil.addErrorMessage("Sale had been Returned u can't cancell bill ");
                return true;
            }
        }

        if (getBill().getBillType() == BillType.PharmacyTransferIssue) {
            if (getBill().checkActiveForwardReference()) {
                JsfUtil.addErrorMessage("Item for this bill already recieve");
                return true;
            }
            if (!getBill().getDepartment().equals(getSessionController().getLoggedUser().getDepartment())) {
                ////System.out.println("getBill().getDepartment()"+getBill().getDepartment());
                ////System.out.println("getSessionController().getLoggedUser().getDepartment() = " + getSessionController().getLoggedUser().getDepartment());
                JsfUtil.addErrorMessage("You Can't Cancel This Transfer Using " + getSessionController().getLoggedUser().getDepartment().getName()
                        + " Department. Please Log " + getBill().getDepartment().getName() + " Deaprtment.");
                return true;
            }
        }

        if (getBill().getComments() == null || getBill().getComments().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }
        return false;
    }

    private boolean checkGrn() {
        String sql = "Select b From BilledBill b where b.retired=false and b.creater is not null"
                + " and b.cancelled=false and b.billType=:btp and "
                + " b.referenceBill=:ref and b.referenceBill.cancelled=false ";
        HashMap hm = new HashMap();
        hm.put("ref", getBill());
        hm.put("btp", BillType.PharmacyOrder);
        List<Bill> tmp = getBillFacade().findByJpql(sql, hm);

        if (!tmp.isEmpty()) {
            return false;
        }

        return true;
    }

    private boolean checkGrnReturn() {
        String sql = "Select b From BilledBill b where b.retired=false and b.creater is not null"
                + " and b.cancelled=false and b.billType=:btp and "
                + " b.referenceBill=:ref and b.referenceBill.cancelled=false";
        HashMap hm = new HashMap();
        hm.put("ref", getBill());
        hm.put("btp", BillType.PharmacyGrnReturn);
        List<Bill> tmp = getBillFacade().findByJpql(sql, hm);

        if (!tmp.isEmpty()) {
            return true;
        }

        return false;
    }

    private boolean checkSaleReturn(Bill b) {
        String sql = "Select b From RefundBill b where b.retired=false "
                + " and b.creater is not null"
                + " and b.cancelled=false "
                + " and b.billType=:btp "
                + " and b.billedBill=:ref "
                + " and b.referenceBill.cancelled=false";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyPre);
        List<Bill> tmp = getBillFacade().findByJpql(sql, hm);

        if (!tmp.isEmpty()) {
            return true;
        }

        return false;
    }

    private boolean checkIssueReturn(Bill b) {
        String sql = "Select b From RefundBill b where b.retired=false "
                + " and b.creater is not null"
                + " and b.cancelled=false "
                + " and b.billType=:btp "
                + " and b.billedBill=:ref ";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyIssue);
        List<Bill> tmp = getBillFacade().findByJpql(sql, hm);

        if (!tmp.isEmpty()) {
            return true;
        }

        return false;
    }

    private CancelledBill pharmacyCreateCancelBill() {
        CancelledBill cb = new CancelledBill();

        cb.setBilledBill(getBill());
        cb.copy(getBill());
        cb.setReferenceBill(getBill().getReferenceBill());
        cb.invertAndAssignValuesFromOtherBill(getBill());

        cb.setPaymentScheme(getBill().getPaymentScheme());
        cb.setPaymentMethod(getBill().getPaymentMethod());
        cb.setBalance(0.0);
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());

        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

        cb.setComments(getComment());
        cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED);

        return cb;
    }

    private RefundBill pharmacyCreateRefundCancelBill() {
        RefundBill cb = new RefundBill();
        cb.invertQty();
        cb.copy(getBill());
        cb.invertAndAssignValuesFromOtherBill(getBill());
        cb.setRefundedBill(getBill());
        cb.setReferenceBill(getBill().getReferenceBill());
        cb.setForwardReferenceBill(getBill().getForwardReferenceBill());

        cb.setPaymentMethod(paymentMethod);
        cb.setPaymentScheme(getBill().getPaymentScheme());
        cb.setBalance(0.0);
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());

        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

        cb.setComments(getBill().getComments());
        cb.setPaymentMethod(getPaymentMethod());
        cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION);

        return cb;
    }

//    private void updateRemainingQty(PharmaceuticalBillItem nB) {
//        String sql = "Select p from PharmaceuticalBillItem p where p.billItem.id=" + nB.getBillItem().getReferanceBillItem().getId();
//        PharmaceuticalBillItem po = getPharmaceuticalBillItemFacade().findFirstByJpql(sql);
//        po.setRemainingQty(po.getRemainingQty() + nB.getQty());
//
//        //System.err.println("Added Remaini Qty " + nB.getQty());
//        //System.err.println("Final Remaini Qty " + po.getRemainingQty());
//        getPharmaceuticalBillItemFacade().edit(po);
//
//    }
    private void pharmacyCancelBillItemsAddStock(CancelledBill can) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            if (can.getBillType() == BillType.PharmacyGrnBill || can.getBillType() == BillType.PharmacyGrnReturn) {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            } else {
                b.setReferanceBillItem(nB);
            }

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().create(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            getPharmacyBean().addToStock(ph.getStock(),
                    Math.abs(qty),
                    ph, getSessionController().getDepartment());

            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelBillItemsAddStock(CancelledBill can, Payment p) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            if (can.getBillType() == BillType.PharmacyGrnBill || can.getBillType() == BillType.PharmacyGrnReturn) {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            } else {
                b.setReferanceBillItem(nB);
            }

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().create(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            getPharmacyBean().addToStock(ph.getStock(),
                    Math.abs(qty),
                    ph, getSessionController().getDepartment());

            getBillItemFacede().edit(b);
            //get billfees from using cancel billItem
            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
            cancelBillFee(can, b, tmp);

            //create BillFeePayments For cancel
            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + b.getId();
            List<BillFee> tmpC = getBillFeeFacade().findByJpql(sql);
            calculateBillfeePaymentsForCancelRefundBill(tmpC, p);
            //

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelBillItemsReduceStock(CancelledBill can) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            if (can.getBillType() == BillType.PharmacyGrnBill || can.getBillType() == BillType.PharmacyGrnReturn) {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            } else {
                b.setReferanceBillItem(nB);
            }

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            getPharmaceuticalBillItemFacade().create(ph);

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().create(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            boolean returnFlag = getPharmacyBean().deductFromStock(ph.getStock(), Math.abs(qty), ph, getSessionController().getDepartment());

            if (!returnFlag) {
                b.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(b.getPharmaceuticalBillItem());
            }

            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelBillItemsReduceStock(CancelledBill cancellationBill, Payment p) {
        for (BillItem originalBillItem : getBill().getBillItems()) {
            BillItem newlyCreatedBillItemForCancelBill = new BillItem();
            newlyCreatedBillItemForCancelBill.setBill(cancellationBill);
            newlyCreatedBillItemForCancelBill.copy(originalBillItem);
            newlyCreatedBillItemForCancelBill.invertValue(originalBillItem);

            if (cancellationBill.getBillType() == BillType.PharmacyGrnBill || cancellationBill.getBillType() == BillType.PharmacyGrnReturn) {
                newlyCreatedBillItemForCancelBill.setReferanceBillItem(originalBillItem.getReferanceBillItem());
            } else {
                newlyCreatedBillItemForCancelBill.setReferanceBillItem(originalBillItem);
            }

            newlyCreatedBillItemForCancelBill.setCreatedAt(new Date());
            newlyCreatedBillItemForCancelBill.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = newlyCreatedBillItemForCancelBill.getPharmaceuticalBillItem();
            ph.copy(originalBillItem.getPharmaceuticalBillItem());
            ph.invertValue(originalBillItem.getPharmaceuticalBillItem());

//            getPharmaceuticalBillItemFacade().create(ph);
            newlyCreatedBillItemForCancelBill.setPharmaceuticalBillItem(ph);

            ph.setBillItem(newlyCreatedBillItemForCancelBill);
//            getPharmaceuticalBillItemFacade().edit(ph);

            if (newlyCreatedBillItemForCancelBill.getId() == null) {
                getBillItemFacade().create(newlyCreatedBillItemForCancelBill);
            } else {
                getBillItemFacade().edit(newlyCreatedBillItemForCancelBill);
            }

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            boolean returnFlag = getPharmacyBean().deductFromStock(ph.getStock(), Math.abs(qty), ph, getSessionController().getDepartment());

            if (!returnFlag) {
                newlyCreatedBillItemForCancelBill.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(newlyCreatedBillItemForCancelBill.getPharmaceuticalBillItem());
            }
            //get billfees from using cancel billItem
            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + originalBillItem.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
            cancelBillFee(cancellationBill, newlyCreatedBillItemForCancelBill, tmp);

            //create BillFeePayments For cancel
            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + newlyCreatedBillItemForCancelBill.getId();
            List<BillFee> tmpC = getBillFeeFacade().findByJpql(sql);
            calculateBillfeePaymentsForCancelRefundBill(tmpC, p);
            //
            getBillItemFacede().edit(newlyCreatedBillItemForCancelBill);

            cancellationBill.getBillItems().add(newlyCreatedBillItemForCancelBill);
        }

        getBillFacade().edit(cancellationBill);
    }

    private void pharmacyCancelIssuedItems(CancelledBill can) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            b.setReferanceBillItem(nB);

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().edit(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            boolean returnFlag = getPharmacyBean().deductFromStockWithoutHistory(ph.getStaffStock(), Math.abs(qty), ph, getSessionController().getDepartment());

            if (returnFlag) {
                getPharmacyBean().addToStock(ph.getStock(), Math.abs(qty), ph, getSessionController().getDepartment());
            } else {
                b.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(b.getPharmaceuticalBillItem());
            }

            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelReceivedItems(CancelledBill can) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            b.setReferanceBillItem(nB);

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().edit(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            getPharmacyBean().addToStockWithoutHistory(ph.getStaffStock(), Math.abs(qty), ph, getSessionController().getDepartment());

            boolean returnFlag = getPharmacyBean().deductFromStock(ph.getStock(), Math.abs(qty), ph, getSessionController().getDepartment());

            if (!returnFlag) {
                b.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(b.getPharmaceuticalBillItem());
            }

            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelBillItems(CancelledBill can) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            if (can.getBillType() == BillType.PharmacyGrnBill || can.getBillType() == BillType.PharmacyGrnReturn) {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            } else {
                b.setReferanceBillItem(nB);
            }

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().edit(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelBillItems(CancelledBill can, Payment p) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = nB;
            b.setBill(can);
//            b.copy(nB);
            b.invertValue(nB);

            if (can.getBillType() == BillType.PharmacyGrnBill || can.getBillType() == BillType.PharmacyGrnReturn) {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            } else {
                b.setReferanceBillItem(nB);
            }

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().create(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            getBillItemFacede().edit(b);

            //get billfees from using cancel billItem
            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
            cancelBillFee(can, b, tmp);

            //create BillFeePayments For cancel
            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + b.getId();
            List<BillFee> tmpC = getBillFeeFacade().findByJpql(sql);
            calculateBillfeePaymentsForCancelRefundBill(tmpC, p);
            //

            can.getBillItems().add(b);

        }

        getBillFacade().edit(can);
    }

    private void cancelBillFee(Bill can, BillItem bt, List<BillFee> tmp) {
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.setFee(nB.getFee());
            bf.setPatienEncounter(nB.getPatienEncounter());
            bf.setPatient(nB.getPatient());
            bf.setDepartment(nB.getDepartment());
            bf.setInstitution(nB.getInstitution());
            bf.setSpeciality(nB.getSpeciality());
            bf.setStaff(nB.getStaff());

            bf.setBill(can);
            bf.setBillItem(bt);
            bf.setFeeValue(0 - nB.getFeeValue());
            bf.setSettleValue(0 - nB.getSettleValue());

            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());

            getBillFeeFacade().edit(bf);
        }
    }

    public void calculateBillfeePaymentsForCancelRefundBill(List<BillFee> billFees, Payment p) {
        for (BillFee bf : billFees) {
            setBillFeePaymentAndPayment(bf, p);
        }
    }

    public void setBillFeePaymentAndPayment(BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(bf.getSettleValue());
        bfp.setInstitution(p.getBill().getFromInstitution());
        bfp.setDepartment(p.getBill().getFromDepartment());
        if (bfp.getDepartment() == null) {
            bfp.setDepartment(p.getDepartment());
        }
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        getBillFeePaymentFacade().create(bfp);
    }

    private void pharmacyCancelReturnBillItems(Bill can) {
        for (PharmaceuticalBillItem nB : getPharmacyBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB.getBillItem());
            b.invertValue(nB.getBillItem());

            b.setReferanceBillItem(nB.getBillItem().getReferanceBillItem());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB);
            ph.invertValue(nB);

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().create(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

//            //System.err.println("Updating QTY " + ph.getQtyInUnit());
//            getPharmacyBean().deductFromStock(ph.getStock(), Math.abs(ph.getQtyInUnit()), ph, getSessionController().getDepartment());
//
//            //    updateRemainingQty(nB);
            // if (b.getReferanceBillItem() != null) {
            //    b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            //  }
            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelReturnBillItems(Bill can, Payment p) {
        for (PharmaceuticalBillItem nB : getPharmacyBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB.getBillItem());
            b.invertValue(nB.getBillItem());

            b.setReferanceBillItem(nB.getBillItem().getReferanceBillItem());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB);
            ph.invertValue(nB);

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().edit(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //get billfees from using cancel billItem
            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getBillItem().getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
            cancelBillFee(can, b, tmp);

            //create BillFeePayments For cancel
            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + b.getId();
            List<BillFee> tmpC = getBillFeeFacade().findByJpql(sql);
            calculateBillfeePaymentsForCancelRefundBill(tmpC, p);
            //

//            //System.err.println("Updating QTY " + ph.getQtyInUnit());
//            getPharmacyBean().deductFromStock(ph.getStock(), Math.abs(ph.getQtyInUnit()), ph, getSessionController().getDepartment());
//
//            //    updateRemainingQty(nB);
            // if (b.getReferanceBillItem() != null) {
            //    b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            //  }
            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelReturnBillItemsWithReducingStock(Bill can) {
        for (PharmaceuticalBillItem nB : getPharmacyBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB.getBillItem());
            b.invertValue(nB.getBillItem());
            b.setTransBillItem(nB.getBillItem());
            b.setReferanceBillItem(nB.getBillItem().getReferanceBillItem());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB);
            ph.invertValue(nB);

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().edit(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //System.err.println("Updating QTY " + ph.getQtyInUnit());
            boolean returnFlag = getPharmacyBean().deductFromStock(ph.getStock(), Math.abs(ph.getQtyInUnit()), ph, getSessionController().getDepartment());

            if (!returnFlag) {
                b.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(b.getPharmaceuticalBillItem());
            }

//
//            //    updateRemainingQty(nB);
            // if (b.getReferanceBillItem() != null) {
            //    b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            //  }
            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

//    private void pharmacyCancelReturnBillItems(Bill can) {
//        for (PharmaceuticalBillItem nB : getPharmacyBillItems()) {
//            BillItem b = new BillItem();
//            b.setBill(can);
//            b.copy(nB.getBillItem());
//            b.invertAndAssignValuesFromOtherBill(nB.getBillItem());
//
//            b.setReferanceBillItem(nB.getBillItem().getReferanceBillItem());
//            b.setCreatedAt(new Date());
//            b.setCreater(getSessionController().getLoggedUser());
//
//            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
//            ph.copy(nB);
//            ph.invertAndAssignValuesFromOtherBill(nB);
//
//            getPharmaceuticalBillItemFacade().create(ph);
//
//            b.setPharmaceuticalBillItem(ph);
//            getBillItemFacede().create(b);
//
//            ph.setBillItem(b);
//            getPharmaceuticalBillItemFacade().edit(ph);
//   
//            getBillItemFacede().edit(b);
//
//            can.getBillItems().add(b);
//        }
//
//        getBillFacade().edit(can);
//    }
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    CashTransactionBean cashTransactionBean;

    public LazyDataModel<Bill> getLazyBills() {
        return lazyBills;
    }

    public void setLazyBills(LazyDataModel<Bill> lazyBills) {
        this.lazyBills = lazyBills;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void pharmacyRetailCancelBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();

            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.SALCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.SALCAN));

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }

            //for Payment,billFee and BillFeepayment
            Payment p = pharmacySaleController.createPayment(cb, paymentMethod);
            drawerController.updateDrawerForOuts(p);
            pharmacyCancelBillItems(cb, p);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

            if (getBill().getReferenceBill() != null) {
                getBill().getReferenceBill().setReferenceBill(null);
                getBillFacade().edit(getBill().getReferenceBill());
            }

            WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    private boolean checkDepartment(Bill bill) {
        if (bill == null) {
            JsfUtil.addErrorMessage("Bill Null");
            return true;

        }

        if (bill.getDepartment() == null) {
            JsfUtil.addErrorMessage("Bill Department Null");
            return true;
        }

        if (!Objects.equals(bill.getDepartment(), getSessionController().getDepartment())) {
            JsfUtil.addErrorMessage("Billed Department Is Defferent than Logged Department");
            return true;
        }

        return false;
    }

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    public void pharmacyRetailCancelBillWithStock() throws ParseException {

        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (getBill().getReferenceBill() == null) {
                return;
            }
            if (!webUserController.hasPrivilege("Admin")) {
                if (configOptionApplicationController.getBooleanValueByKey("Settled pharmacy bills can be cancelled only withing settled day.", false)) {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
                    Date createdDate = formatter.parse(formatter.format(getBill().getCreatedAt()));
                    Date today = formatter.parse(formatter.format(new Date()));
                    if (!createdDate.equals(today)) {
                        JsfUtil.addErrorMessage("Settled bills cancelled can be done only within settled day.");
                        return;
                    }

                }
            }

            if (getBill().getReferenceBill().getBillType() != BillType.PharmacyPre && getBill().getReferenceBill().getBillType() != BillType.PharmacyWholesalePre) {
                return;
            }

//            if (calculateNumberOfBillsPerOrder(getBill().getReferenceBill())) {
//                return;
//            } before
            ////System.out.println("getBill().getReferenceBill().getDepartment() = " + getBill().getReferenceBill().getDepartment().getName());
            ////System.out.println("bill.getDepartment() = " + getBill().getDepartment().getName());
            ////System.out.println("getSessionController().getDepartment() = " + getSessionController().getDepartment().getName());
            if (checkDepartment(getBill())) {
                return;
            }

            getPharmacyBean().reAddToStock(getBill().getReferenceBill(), getSessionController().getLoggedUser(), getSessionController().getDepartment(), BillNumberSuffix.PRECAN);

            CancelledBill cb = pharmacyCreateCancelBill();

            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.SALCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.SALCAN));

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }

            //for Payment,billFee and BillFeepayment
            Payment p = pharmacySaleController.createPayment(cb, paymentMethod);
            drawerController.updateDrawerForOuts(p);
            pharmacyCancelBillItems(cb, p);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

//            if (getBill().getReferenceBill() != null) {
//                getBill().getReferenceBill().setReferenceBill(null);
//                getBillFacade().edit(getBill().getReferenceBill());
//            }
            getCashTransactionBean().saveBillCashOutTransaction(cb, getSessionController().getLoggedUser());

            JsfUtil.addSuccessMessage("Cancelled");
            //   ////System.out.println("going to cancel staff payments");
            if (getBill().getPaymentMethod() == PaymentMethod.Credit) {
                //   ////System.out.println("getBill().getPaymentMethod() = " + getBill().getPaymentMethod());
                //   ////System.out.println("getBill().getToStaff() = " + getBill().getToStaff());
                if (getBill().getToStaff() != null) {
                    //   ////System.out.println("getBill().getNetTotal() = " + getBill().getNetTotal());
                    getStaffBean().updateStaffCredit(getBill().getToStaff(), 0 - getBill().getNetTotal());
                    JsfUtil.addSuccessMessage("Staff Credit Updated");
                    cb.setFromStaff(getBill().getToStaff());
                    getBillFacade().edit(cb);
                }
            }

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyRetailCancelBillWithStock(Bill cbill) {
        setBill(cbill);
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (getBill().getBillType() != BillType.PharmacyPre && getBill().getBillType() != BillType.PharmacyWholesalePre) {
                return;
            }

//            if (calculateNumberOfBillsPerOrder(getBill().getReferenceBill())) {
//                return;
//            } before
//            System.out.println("getBill().getReferenceBill().getDepartment() = " + getBill().getReferenceBill().getDepartment().getName());
//            System.out.println("bill.getDepartment() = " + getBill().getDepartment().getName());
//            System.out.println("getSessionController().getDepartment() = " + getSessionController().getDepartment().getName());
            if (checkDepartment(getBill())) {
                return;
            }
//            System.out.println("reAddToStock = " );
            getPharmacyBean().reAddToStock(getBill(), getSessionController().getLoggedUser(), getSessionController().getDepartment(), BillNumberSuffix.PRECAN);
//            System.out.println("After reAddToStock = " );
            getBill().setCancelled(true);
            getBill().setCancelledBill(null);
            getBillFacade().edit(getBill());

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void savePreComponent(Bill cbill) {
        setBill(cbill);
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (getBill().getBillType() != BillType.PharmacyPre && getBill().getBillType() != BillType.PharmacyWholesalePre) {
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }
            for (BillItem i : getBill().getBillItems()) {
                i.getPharmaceuticalBillItem().setQty((double) (double) i.getQty());
                if (i.getPharmaceuticalBillItem().getQty() == 0.0) {
                    continue;
                }

                i.setCreatedAt(Calendar.getInstance().getTime());
                i.setCreater(getSessionController().getLoggedUser());
                //   i.getBillItem().setQty(i.getPharmaceuticalBillItem().getQty());
                double value = i.getNetRate() * i.getQty();
                i.setGrossValue(0 - value);
                i.setNetValue(0 - value);

                PharmaceuticalBillItem tmpPh = i.getPharmaceuticalBillItem();
                i.setPharmaceuticalBillItem(null);
                getBillItemFacade().create(i);

                tmpPh.setBillItem(i);
                getPharmaceuticalBillItemFacade().create(tmpPh);

                i.setPharmaceuticalBillItem(tmpPh);
                getBillItemFacade().edit(i);
                //   getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
                //   getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
                //   getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
                getPharmacyBean().addToStock(tmpPh.getStock(), Math.abs(tmpPh.getQtyInUnit()), tmpPh, getSessionController().getDepartment());

                //   i.getBillItem().getTmpReferenceBillItem().getPharmaceuticalBillItem().setRemainingQty(i.getRemainingQty() - i.getQty());
                //   getPharmaceuticalBillItemFacade().edit(i.getBillItem().getTmpReferenceBillItem().getPharmaceuticalBillItem());
                //      updateRemainingQty(i);
            }

        }

    }

    public void cancelPharmacyDirectIssueToBht() {
        if (getBill().getBillType() != BillType.PharmacyBhtPre) {
            return;
        }
        if (getBill().getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("This Bill Already Discharged");
            return;
        }
        if (getBill().getCheckedBy() != null) {
            JsfUtil.addErrorMessage("Checked Bill. Can not cancel");
            return;
        }
        if (getBill().checkActiveReturnBhtIssueBills()) {
            JsfUtil.addErrorMessage("There some return Bill for this please cancel that bills first");
            return;
        }
        if (checkDepartment(getBill())) {
            return;
        }
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION);
        Bill newlyCreatedCancellationBill = getPharmacyBean().reAddToStock(getBill(), getSessionController().getLoggedUser(), getSessionController().getDepartment(), BillNumberSuffix.PHISSCAN);
        newlyCreatedCancellationBill.setForwardReferenceBill(getBill().getForwardReferenceBill());
        newlyCreatedCancellationBill.setBillTypeAtomic(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION);
        newlyCreatedCancellationBill.setDeptId(deptId);
        
        getBillFacade().edit(newlyCreatedCancellationBill);

        getBill().setCancelled(true);
        getBill().setCancelledBill(newlyCreatedCancellationBill);
        getBillFacade().edit(getBill());
        JsfUtil.addSuccessMessage("Cancelled");

        printPreview = true;

    }

    public void storeRetailCancelBillWithStockBht() {
        if (getBill().getBillType() != BillType.StoreBhtPre) {
            return;
        }

        cancelDirectIssueToBht(BillNumberSuffix.STTISSUECAN);
    }

    public void cancelPreBillFees(List<BillItem> list) {
        for (BillItem b : list) {
            List<BillFee> bfs = getBillFees(b.getTransBillItem());
            for (BillFee bf : bfs) {
                BillFee nBillFee = new BillFee();
                nBillFee.copy(bf);
                nBillFee.invertValue(bf);
                nBillFee.setBill(b.getBill());
                nBillFee.setBillItem(b);
                nBillFee.setCreatedAt(new Date());
                nBillFee.setCreater(getSessionController().getLoggedUser());

                if (nBillFee.getId() == null) {
                    getBillFeeFacade().create(nBillFee);
                }
            }
        }
    }

    private void cancelDirectIssueToBht(BillNumberSuffix billNumberSuffix) {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                return;
            }

            if (getBill().checkActiveReturnBhtIssueBills()) {
                JsfUtil.addErrorMessage("There some return Bill for this please cancel that bills first");
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }

            Bill cb = getPharmacyBean().reAddToStock(getBill(), getSessionController().getLoggedUser(), getSessionController().getDepartment(), billNumberSuffix);
            cb.setForwardReferenceBill(getBill().getForwardReferenceBill());
            cb.setBillTypeAtomic(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION);
            getBillFacade().edit(cb);

            //   cancelPreBillFees(cb.getBillItems());
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

//            if (getBill().getReferenceBill() != null) {
//                getBill().getReferenceBill().setReferenceBill(null);
//                getBillFacade().edit(getBill().getReferenceBill());
//            }
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyReturnPreCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (getBill().checkActiveReturnCashBill()) {
                JsfUtil.addErrorMessage("Payment for this bill Already Paid");
                return;
            }

            RefundBill cb = pharmacyCreateRefundCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RETCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RETCAN));

            if (cb.getId() == null) {
                getBillFacade().edit(cb);
            }

            pharmacyCancelReturnBillItemsWithReducingStock(cb);

//            List<PharmaceuticalBillItem> tmp = getPharmaceuticalBillItemFacade().findByJpql("Select p from PharmaceuticalBillItem p where p.billItem.bill.id=" + getBill().getId());
//
//            for (PharmaceuticalBillItem ph : tmp) {
//                getPharmacyBean().deductFromStock(ph.getItemBatch(), ph.getQtyInUnit() + ph.getFreeQtyInUnit(), getBill().getDepartment());
//
//                getPharmacyBean().reSetPurchaseRate(ph.getItemBatch(), getBill().getDepartment());
//                getPharmacyBean().reSetRetailRate(ph.getItemBatch(), getSessionController().getDepartment());
//            }
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyReturnBhtCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            if (checkDepartment(getBill())) {
                return;
            }

            if (getBill().getCheckedBy() != null) {
                JsfUtil.addErrorMessage("Checked Bill. Can not cancel");
                return;
            }

            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                JsfUtil.addErrorMessage("This BHT Already Discharge..");
                return;
            }

            RefundBill cb = pharmacyCreateRefundCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RETCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RETCAN));

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }

            pharmacyCancelReturnBillItemsWithReducingStock(cb);

            // cancelPreBillFees(cb.getBillItems());
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyReturnCashCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            RefundBill cb = pharmacyCreateRefundCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RETCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RETCAN));

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }
            //for Payment,billFee and BillFeepayment
            Payment p = pharmacySaleController.createPayment(cb, paymentMethod);
            pharmacyCancelReturnBillItems(cb, p);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

            getCashTransactionBean().saveBillCashInTransaction(cb, getSessionController().getLoggedUser());

            JsfUtil.addSuccessMessage("Cancelled");
            if (getBill().getPaymentMethod() == PaymentMethod.Credit) {
                //   ////System.out.println("getBill().getPaymentMethod() = " + getBill().getPaymentMethod());
                //   ////System.out.println("getBill().getToStaff() = " + getBill().getToStaff());
                if (getBill().getToStaff() != null) {
                    //   ////System.out.println("getBill().getNetTotal() = " + getBill().getNetTotal());
                    getStaffBean().updateStaffCredit(getBill().getToStaff(), 0 - getBill().getNetTotal());
                    JsfUtil.addSuccessMessage("Staff Credit Updated");
                    cb.setFromStaff(getBill().getToStaff());
                    getBillFacade().edit(cb);
                }
            }

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyPoCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.POCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.POCAN));

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }
            pharmacyCancelBillItems(cb);

            getBill().getReferenceBill().setReferenceBill(null);
            getBillFacade().edit(getBill().getReferenceBill());

            getBill().setReferenceBill(null);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            //       //System.err.println("Bill : "+getBill().getBillType());
//            //System.err.println("Reference Bill : "+getBill().getReferenceBill().getBillType());
            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyPoRequestCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (getBill().getReferenceBill() != null && !getBill().getReferenceBill().isCancelled()) {
                JsfUtil.addErrorMessage("Sorry You cant Cancell Approved Bill");
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PORCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PORCAN));

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }
            pharmacyCancelBillItems(cb);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            //       //System.err.println("Bill : "+getBill().getBillType());
//            //System.err.println("Reference Bill : "+getBill().getReferenceBill().getBillType());
            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    private boolean checkStock(PharmaceuticalBillItem pharmaceuticalBillItem) {
        //System.err.println("Batch " + pharmaceuticalBillItem.getItemBatch());
        double stockQty = getPharmacyBean().getStockQty(pharmaceuticalBillItem.getItemBatch(), getBill().getDepartment());
        if (Math.abs(pharmaceuticalBillItem.getQtyInUnit() + pharmaceuticalBillItem.getFreeQtyInUnit()) > stockQty) {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkBillItemStock() {
        //System.err.println("Checking Item Stock");
        for (BillItem bi : getBill().getBillItems()) {
            if (checkStock(bi.getPharmaceuticalBillItem())) {
                return true;
            }
        }

        return false;
    }

    public void pharmacyGrnCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }

            if (checkBillItemStock()) {
                JsfUtil.addErrorMessage("Items for this GRN Already issued so you can't cancel ");
                return;
            }

            if (getBill().getPaidAmount() != 0) {
                JsfUtil.addErrorMessage("Payments for this GRN Already Given ");
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.GRNCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.GRNCAN));
            cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_CANCELLED);

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            } else {
                getBillFacade().edit(cb);
            }

            //to create payments for cancel bill
            Payment p = pharmacySaleController.createPayment(cb, paymentMethod);

//            pharmacyCancelBillItemsReduceStock(cb); //for create billfees ,billfee payments
            pharmacyCancelBillItemsReduceStock(cb, p);
//
//            List<PharmaceuticalBillItem> tmp = getPharmaceuticalBillItemFacade().findByJpql("Select p from PharmaceuticalBillItem p where p.billItem.bill.id=" + getBill().getId());
//
//            for (PharmaceuticalBillItem ph : tmp) {
//                double qty = ph.getQtyInUnit() + ph.getFreeQtyInUnit();
//                getPharmacyBean().deductFromStock(ph.getStock(), qty);
//
//                getPharmacyBean().reSetPurchaseRate(ph.getItemBatch(), getBill().getDepartment());
//                getPharmacyBean().reSetRetailRate(ph.getItemBatch(), getSessionController().getDepartment());
//            }

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getBill());
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyTransferIssueCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PHTICAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PHTICAN));

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }

            pharmacyCancelIssuedItems(cb);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

//            getBill().getBackwardReferenceBill().setForwardReferenceBill(null);
//            getBillFacade().edit(getBill().getBackwardReferenceBill());
//
//            getBill().setBackwardReferenceBill(null);
//            getBill().setForwardReferenceBill(null);
//            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyTransferReceiveCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }

            if (checkBillItemStock()) {
                JsfUtil.addErrorMessage("Items for this Note Already issued so you can't cancel ");
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PHTRCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PHTRCAN));

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }

            pharmacyCancelReceivedItems(cb);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyPurchaseCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }

            if (checkBillItemStock()) {
                JsfUtil.addErrorMessage("ITems for this GRN Already issued so you can't cancel ");
                return;
            }

            if (getBill().getPaidAmount() != 0) {
                JsfUtil.addErrorMessage("Payments for this GRN Already Given ");
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PURCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PURCAN));
            cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED);

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }

            Payment p = pharmacySaleController.createPayment(cb, getBill().getPaymentMethod());

            pharmacyCancelBillItemsReduceStock(cb, p);

//            //   List<PharmaceuticalBillItem> tmp = getPharmaceuticalBillItemFacade().findByJpql("Select p from PharmaceuticalBillItem p where p.billItem.bill.id=" + getBill().getId());
//            for (BillItem bi : getBill().getBillItems()) {
//                double qty = bi.getPharmaceuticalBillItem().getQtyInUnit() + bi.getPharmaceuticalBillItem().getFreeQtyInUnit();
//                getPharmacyBean().deductFromStock(bi.getPharmaceuticalBillItem().getStock(), qty);
//
//                getPharmacyBean().reSetPurchaseRate(bi.getPharmaceuticalBillItem().getItemBatch(), getBill().getDepartment());
//                getPharmacyBean().reSetRetailRate(bi.getPharmaceuticalBillItem().getItemBatch(), getSessionController().getDepartment());
//            }
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);

            pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getBill());
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            WebUser wb = getCashTransactionBean().saveBillCashInTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyGrnReturnCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.GRNRETCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.GRNRETCAN));

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }
            //create Billfee Payments
            Payment p = pharmacySaleController.createPayment(cb, getBill().getPaymentMethod());
            pharmacyCancelBillItemsAddStock(cb, p);

            //        List<PharmaceuticalBillItem> tmp = getPharmaceuticalBillItemFacade().findByJpql("Select p from PharmaceuticalBillItem p where p.billItem.bill.id=" + getBill().getId());
//            for (PharmaceuticalBillItem ph : tmp) {
//                getPharmacyBean().addToStock(ph.getStock(), ph.getQtyInUnit());
//
//                getPharmacyBean().reSetPurchaseRate(ph.getItemBatch(), getBill().getDepartment());
//                getPharmacyBean().reSetRetailRate(ph.getItemBatch(), getSessionController().getDepartment());
//            }
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    private void returnBillFee(Bill b, BillItem bt, List<BillFee> tmp) {
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.setFee(nB.getFee());
            bf.setPatienEncounter(nB.getPatienEncounter());
            bf.setPatient(nB.getPatient());
            bf.setDepartment(nB.getDepartment());
            bf.setInstitution(nB.getInstitution());
            bf.setSpeciality(nB.getSpeciality());
            bf.setStaff(nB.getStaff());

            bf.setBill(b);
            bf.setBillItem(bt);
            bf.setFeeValue(0 - nB.getFeeValue());
            bf.setFeeGrossValue(0 - nB.getFeeGrossValue());
            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());

            if (bf.getId() == null) {
                getBillFeeFacade().create(bf);
            }
        }
    }

    @Inject
    private BillBeanController billBean;

    boolean showAllBills;

    public boolean isShowAllBills() {
        return showAllBills;
    }

    public void setShowAllBills(boolean showAllBills) {
        this.showAllBills = showAllBills;
    }

    public void allBills() {
        showAllBills = true;
    }

    public List<Bill> getBills() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.OpdBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.OpdBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        showAllBills = false;
        return bills;
    }

    public List<Bill> getColBills() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.LabBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.LabBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        showAllBills = false;
        return bills;
    }

    public List<Bill> getPos() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyOrder);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyOrder);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getSales() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacySale);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacySale);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getPreBills() {
        if (bills == null) {
//            if (txtSearch == null || txtSearch.trim().equals("")) {
            bills = getBillBean().billsForTheDay2(getFromDate(), getToDate(), BillType.PharmacyPre);
//            } else {
//                bills = getBillBean().billsFromSearch2(txtSearch, getFromDate(), getToDate(), BillType.PharmacySale);
//            }
//            if (bills == null) {
//                bills = new ArrayList<>();
//            }
        }
        return bills;
    }

    public void makeNull() {
        refundAmount = 0;
        txtSearch = null;
        bill = null;
        paymentMethod = null;
        paymentScheme = null;
        billForRefund = null;
        fromDate = null;
        toDate = null;
        user = null;
        ////////////////
        refundingItems = null;
        bills = null;
        filteredBill = null;
        selectedBills = null;
        billEntrys = null;
        billItems = null;
        billComponents = null;
        billFees = null;
        tempbillItems = null;
        searchRetaiBills = null;
        lazyBills = null;

    }

    public List<Bill> getPreRefundBills() {
        if (bills == null) {
            //   List<Bill> lstBills;
            String sql;
            Map temMap = new HashMap();
            sql = "select b from RefundBill b where b.billType = :billType"
                    + " and b.createdAt between :fromDate and :toDate and b.retired=false order by b.id desc ";

            temMap.put("billType", BillType.PharmacyPre);
            // temMap.put("refBillType", BillType.PharmacySale);
            temMap.put("toDate", toDate);
            temMap.put("fromDate", fromDate);

            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
            //   bills = getBillBean().billsRefundForTheDay(getFromDate(), getToDate(), BillType.PharmacyPre);

        }
        return bills;
    }

    public List<Bill> getRequests() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyTransferRequest);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyTransferRequest);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getGrns() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyGrnBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyGrnBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getInstitutionPaymentBills() {
        if (bills == null) {
            String sql;
            Map temMap = new HashMap();
            if (bills == null) {
                if (txtSearch == null || txtSearch.trim().equals("")) {
                    sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.id in(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType!=:btp and bt.referenceBill.billType!=:btp2) and b.billType=:type and b.createdAt between :fromDate and :toDate order by b.id";

                } else {
                    sql = "select b from BilledBill b where b.retired=false and"
                            + " b.id in(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType!=:btp and bt.referenceBill.billType!=:btp2) "
                            + "and b.billType=:type and b.createdAt between :fromDate and :toDate and ((b.staff.person.name) like '%" + txtSearch.toUpperCase() + "%'  or (b.staff.person.phone) like '%" + txtSearch.toUpperCase() + "%'  or (b.insId) like '%" + txtSearch.toUpperCase() + "%') order by b.id desc  ";
                }

                temMap.put("toDate", getToDate());
                temMap.put("fromDate", getFromDate());
                temMap.put("type", BillType.PaymentBill);
                temMap.put("btp", BillType.ChannelPaid);
                temMap.put("btp2", BillType.ChannelCredit);
                bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);

                if (bills == null) {
                    bills = new ArrayList<>();
                }
            }
        }
        return bills;

    }

    public List<Bill> getChannelPaymentBills() {
        if (bills == null) {
            String sql;
            Map temMap = new HashMap();
            if (bills == null) {
                sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.id in"
                        + "(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType=:btp"
                        + " or bt.referenceBill.billType=:btp2) and b.billType=:type and b.createdAt "
                        + "between :fromDate and :toDate order by b.id";

                temMap.put("toDate", getToDate());
                temMap.put("fromDate", getFromDate());
                temMap.put("type", BillType.PaymentBill);
                temMap.put("btp", BillType.ChannelPaid);
                temMap.put("btp2", BillType.ChannelCredit);
                bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);

                if (bills == null) {
                    bills = new ArrayList<>();
                }
            }
        }
        return bills;

    }

    public List<Bill> getUserBills() {
        List<Bill> userBills;
        //////System.out.println("getting user bills");
        if (getUser() == null) {
            userBills = new ArrayList<>();
            //////System.out.println("user is null");
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), BillType.OpdBill);
            //////System.out.println("user ok");
        }
        if (userBills == null) {
            userBills = new ArrayList<>();
        }
        return userBills;
    }

    public List<Bill> getOpdBills() {
        if (txtSearch == null || txtSearch.trim().equals("")) {
            bills = getBillBean().billsForTheDay(fromDate, toDate, BillType.OpdBill);
        } else {
            bills = getBillBean().billsFromSearch(txtSearch, fromDate, toDate, BillType.OpdBill);
        }

        if (bills == null) {
            bills = new ArrayList<>();
        }
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public String getTxtSearch() {
        return txtSearch;
    }

    public void setTxtSearch(String txtSearch) {
        this.txtSearch = txtSearch;
        recreateModel();
    }

    public Bill getBill() {
        //recreateModel();
        return bill;
    }

    public void setBill(Bill bb) {
        recreateModel();
        if (bb == null) {
            bb = this.bill;
        }
        this.bill = bb;
//        if (bb.getPaymentMethod() != null) {
//            paymentMethod = bb.getPaymentMethod();
//        }

    }

    public List<BillEntry> getBillEntrys() {
        return billEntrys;
    }

    public void setBillEntrys(List<BillEntry> billEntrys) {
        this.billEntrys = billEntrys;
    }

    public List<BillItem> getBillItems() {
        String sql = "";
        if (getBill() != null) {
            if (getBill().getRefundedBill() == null) {
                sql = "SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            } else {
                sql = "SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + getBill().getRefundedBill().getId();
            }
            billItems = getBillItemFacede().findByJpql(sql);
            // //////System.out.println("sql for bill item search is " + sql);
            // //////System.out.println("results for bill item search is " + billItems);
            if (billItems == null) {
                billItems = new ArrayList<>();
            }
        }

        return billItems;
    }

    public List<PharmaceuticalBillItem> getPharmacyBillItems() {
        List<PharmaceuticalBillItem> tmp = new ArrayList<>();
        if (getBill() != null) {
            String sql = "SELECT b FROM PharmaceuticalBillItem b WHERE b.billItem.retired=false and b.billItem.bill.id=" + getBill().getId();
            tmp = getPharmaceuticalBillItemFacade().findByJpql(sql);
        }

        return tmp;
    }

    public List<BillComponent> getBillComponents() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillComponent b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billComponents = getBillCommponentFacade().findByJpql(sql);
            if (billComponents == null) {
                billComponents = new ArrayList<>();
            }
        }
        return billComponents;
    }

    public List<BillFee> getBillFees() {
        if (getBill() != null) {
            if (billFees == null || billForRefund == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
                billFees = getBillFeeFacade().findByJpql(sql);
            }
        }

        if (billFees == null) {
            billFees = new ArrayList<>();
        }

        return billFees;
    }

    public List<BillFee> getBillFees(BillItem bi) {

        String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.billItem=:b ";
        HashMap hm = new HashMap();
        hm.put("b", bi);
        List<BillFee> ls = getBillFeeFacade().findByJpql(sql, hm);

        if (ls == null) {
            ls = new ArrayList<>();
        }

        return ls;
    }

    public List<BillFee> getBillFees2() {
        if (getBill() != null) {
            if (billFees == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
                billFees = getBillFeeFacade().findByJpql(sql);
            }
        }

        if (billFees == null) {
            billFees = new ArrayList<>();
        }

        return billFees;
    }

    public List<BillFee> getPayingBillFees() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billFees = getBillFeeFacade().findByJpql(sql);
            if (billFees == null) {
                billFees = new ArrayList<>();
            }

        }

        return billFees;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public void setBillComponents(List<BillComponent> billComponents) {
        this.billComponents = billComponents;
    }

    /**
     * Creates a new instance of BillSearch
     */
    public PharmacyBillSearch() {
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillItemFacade getBillItemFacede() {
        return billItemFacede;
    }

    public void setBillItemFacede(BillItemFacade billItemFacede) {
        this.billItemFacede = billItemFacede;
    }

    public BillComponentFacade getBillCommponentFacade() {
        return billCommponentFacade;
    }

    public void setBillCommponentFacade(BillComponentFacade billCommponentFacade) {
        this.billCommponentFacade = billCommponentFacade;
    }

    private void setRefundAttribute() {
        billForRefund.setBalance(getBill().getBalance());

        billForRefund.setBillDate(Calendar.getInstance().getTime());
        billForRefund.setBillTime(Calendar.getInstance().getTime());
        billForRefund.setCreater(getSessionController().getLoggedUser());
        billForRefund.setCreatedAt(Calendar.getInstance().getTime());

        billForRefund.setBillType(getBill().getBillType());
        billForRefund.setBilledBill(getBill());

        billForRefund.setCatId(getBill().getCatId());
        billForRefund.setCollectingCentre(getBill().getCollectingCentre());
        billForRefund.setCreditCardRefNo(getBill().getCreditCardRefNo());
        billForRefund.setCreditCompany(getBill().getCreditCompany());

        billForRefund.setDepartment(getBill().getDepartment());
        billForRefund.setDeptId(getBill().getDeptId());
        billForRefund.setDiscount(getBill().getDiscount());

        billForRefund.setDiscountPercent(getBill().getDiscountPercent());
        billForRefund.setFromDepartment(getBill().getFromDepartment());
        billForRefund.setFromInstitution(getBill().getFromInstitution());
        billForRefund.setFromStaff(getBill().getFromStaff());

        billForRefund.setInsId(getBill().getInsId());
        billForRefund.setInstitution(getBill().getInstitution());

        billForRefund.setPatient(getBill().getPatient());
        billForRefund.setPatientEncounter(getBill().getPatientEncounter());
        billForRefund.setPaymentMethod(paymentMethod);
        billForRefund.setPaymentScheme(getBill().getPaymentScheme());
        billForRefund.setPaymentSchemeInstitution(getBill().getPaymentSchemeInstitution());

        billForRefund.setReferredBy(getBill().getReferredBy());
        billForRefund.setReferringDepartment(getBill().getReferringDepartment());

        billForRefund.setStaff(getBill().getStaff());

        billForRefund.setToDepartment(getBill().getToDepartment());
        billForRefund.setToInstitution(getBill().getToInstitution());
        billForRefund.setToStaff(getBill().getToStaff());
        billForRefund.setTotal(calTot());
        //Need To Add Net Total Logic
        billForRefund.setNetTotal(billForRefund.getTotal());
    }

    public double calTot() {
        if (getBillFees() == null) {
            return 0.0f;
        }
        double tot = 0.0f;
        for (BillFee f : getBillFees()) {
            //////System.out.println("Tot" + f.getFeeValue());
            tot += f.getFeeValue();
        }
        getBillForRefund().setTotal(tot);
        return tot;
    }

    public RefundBill getBillForRefund() {

        if (billForRefund == null) {
            billForRefund = new RefundBill();
            setRefundAttribute();
        }

        return billForRefund;
    }

    public String viewBill() {

        if (bill != null) {
            switch (bill.getBillType()) {
                case PharmacyPre:
                    return "pharmacy_reprint_bill_sale";
                case PharmacyBhtPre:
                    return "pharmacy_reprint_bill_sale";
                case PharmacyIssue:
                    return "pharmacy_reprint_bill_unit_issue";
                case PharmacyTransferIssue:
                    return "pharmacy_reprint_transfer_isssue";
                case PharmacyTransferReceive:
                    return "pharmacy_reprint_transfer_receive";
                case PharmacyPurchaseBill:
                    return "pharmacy_reprint_purchase";
                case PharmacyGrnBill:
                    return "pharmacy_reprint_grn";
                case PharmacyGrnReturn:
                    return "pharmacy_reprint_grn_return";
                case PurchaseReturn:
                    return "pharmacy_reprint_purchase_return";
                case PharmacyAdjustment:
                    return "pharmacy_reprint_adjustment";
                case PharmacyWholesalePre:
                    return "pharmacy_reprint_bill_sale";
                default:
                    return "pharmacy_reprint_bill_sale";
            }
        } else {

            return "";
        }

    }

    public void setBillForRefund(RefundBill billForRefund) {
        this.billForRefund = billForRefund;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public List<BillItem> getTempbillItems() {
        if (tempbillItems == null) {
            tempbillItems = new ArrayList<>();
        }
        return tempbillItems;
    }

    public void setTempbillItems(List<BillItem> tempbillItems) {
        this.tempbillItems = tempbillItems;
    }

    public void resetLists() {
        recreateModel();
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        resetLists();
        this.toDate = toDate;

    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        resetLists();
        this.fromDate = fromDate;

    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public ItemBatchFacade getItemBatchFacade() {
        return itemBatchFacade;
    }

    public void setItemBatchFacade(ItemBatchFacade itemBatchFacade) {
        this.itemBatchFacade = itemBatchFacade;
    }

    public List<Bill> getFilteredBill() {
        return filteredBill;
    }

    public void setFilteredBill(List<Bill> filteredBill) {
        this.filteredBill = filteredBill;
    }

    private boolean checkCollectedReported() {
        return false;
    }

    public List<Bill> getSearchRetaiBills() {
        return searchRetaiBills;
    }

    public void setSearchRetaiBills(List<Bill> searchRetaiBills) {
        this.searchRetaiBills = searchRetaiBills;
    }

    public List<Bill> getSelectedBills() {
        if (selectedBills == null) {
            selectedBills = new ArrayList<>();
        }
        return selectedBills;
    }

    public void setSelectedBills(List<Bill> selectedBills) {
        this.selectedBills = selectedBills;
    }

    public StaffService getStaffBean() {
        return staffBean;
    }

    public void setStaffBean(StaffService staffBean) {
        this.staffBean = staffBean;
    }

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
