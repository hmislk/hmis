/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.InstitutionBills;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.data.table.String1Value5;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.CreditBean;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.BillService;
import com.divudi.service.PaymentService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import java.text.DecimalFormat;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class SupplierPaymentController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    CreditBean creditBean;
    @EJB
    CashTransactionBean cashTransactionBean;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    BillService billService;
    @EJB
    PaymentService paymentService;
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private BillController billController;
    @Inject
    private BillBeanController billBean;
    @Inject
    private PaymentSchemeController paymentSchemeController;
    @Inject
    private SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private List<Bill> bills;
    private List<Bill> selectedBills;
    private List<InstitutionBills> items;
    private List<String1Value5> dealorCreditAge;
    private List<String1Value5> filteredList;
    private Double netTotal;
    private Double paidAmount;
    private Double refundAmount;
    private Double balance;
    private Date fromDate;
    private Date toDate;
    private Date chequeFromDate;
    private Date chequeToDate;
    private String chequeNo;
    private boolean printPreview;
    private SearchKeyword searchKeyword;
    private Bill current;
    private Bill currentCancellationBill;
    private List<Bill> currentReturnBills;
    private List<BillItem> currentPaymentBillItems;
    private List<Bill> currentPaymentRefundBills;
    private List<BillItem> currentSummeryBillItems;
    private Payment currentPayment;
    private double currentSummaryPurchaseTotalValue;
    private double currentSummaryPurchaseReturnTotalValue;
    private double currentSummaryPurchaseNetTotalValue;
    private double currentTotalPaymentSettledValue;
    private double currentTotalPaymentToSettleValue;

    private PaymentMethodData paymentMethodData;
    private BillItem currentBillItem;
    private Institution institution;
    private Bill bill;
    private String comment;
    private Institution toInstitution;
    private Institution bank;
    private List<BillItem> billItems;
    private List<BillItem> selectedBillItems;
    private int tabIndex = 0;
    private List<String> supplierPaymentStatusList;
    private String supplierPaymentStatus;
    boolean changed = false;
    private boolean acPayeeOnly;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Constructors">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Navigation Methods">
    public String navigateToDealerPaymentIndex() {
        return "/dealerPayment/index?faces-redirect=true";
    }

    public String navigateToDealerDueSearch() {
        bills = new ArrayList<>();
        netTotal = 0.0;
        return "/dealerPayment/dealor_due?faces-redirect=true";
    }

//    public String navigateToGenerateSupplierPayments() {
//        bills = new ArrayList<>();
//        netTotal = 0.0;
//        return "/dealerPayment/dealor_due?faces-redirect=true";
//    }
    public String navigateToGenerateSupplierPayments() {
        bills = new ArrayList<>();
        netTotal = 0.0;
        return "/dealerPayment/list_bills_to_generate_supplier_payments?faces-redirect=true";
    }

    public String navigateToGenerateSupplierReturnPayments() {
        bills = new ArrayList<>();
        netTotal = 0.0;
        return "/dealerPayment/list_grn_returns_to_generate_supplier_payments?faces-redirect=true";
    }

    public String navigateToListAllGrns() {
        bills = new ArrayList<>();
        netTotal = 0.0;
        return "/dealerPayment/list_all_grns?faces-redirect=true";
    }

    public String navigateToApproveSupplierPayments() {
        bills = new ArrayList<>();
        netTotal = 0.0;
        return "/dealerPayment/list_bills_to_approve_supplier_payments?faces-redirect=true";
    }

    public String navigateToViewSupplierPayments() {
        bills = new ArrayList<>();
        netTotal = 0.0;
        return "/dealerPayment/list_supplier_payments?faces-redirect=true";
    }

    public String navigateToSettleSupplierPayments() {
        bills = new ArrayList<>();
        netTotal = 0.0;
        return "/dealerPayment/list_bills_to_settle_supplier_payments?faces-redirect=true";
    }

    public String navigateToCompleteSupplierPayments() {
        bills = new ArrayList<>();
        netTotal = 0.0;
        return "/dealerPayment/list_bills_to_complete_supplier_payments?faces-redirect=true";
    }

    public String navigateToDealerDoneSearch() {
        bills = new ArrayList<>();
        netTotal = 0.0;
        return "/dealerPayment/settled_bills?faces-redirect=true";
    }

    public String navigateToDealerDuehalfPaymentsSearch() {
        bills = new ArrayList<>();
        netTotal = 0.0;
        printPreview = false;
        paymentMethodData = null;
        current = null;
        billItems = null;
        selectedBillItems = null;
        return "/dealerPayment/bill_dealor_all?faces-redirect=true";
    }

    public String navigateToDealerDueSearchPharmacy() {
        bills = new ArrayList<>();
        netTotal = 0.0;
        return "/dealerPayment/dealor_due_pharmacy?faces-redirect=true";
    }

    public String navigateToDealerDueByAge() {
        makeNull();
        return "/dealerPayment/dealor_due_age?faces-redirect=true";
    }

    public String navigateToByDealer() {
        makeNull();
        return "/dealerPayment/bill_dealor_all?faces-redirect=true";
    }

    public String navigateToByBill() {
        makeNull();
        return "/dealerPayment/bill_dealor?faces-redirect=true";
    }

    public String navigateToGRNPaymentApprove() {
        makeNull();
        return "/dealerPayment/search_dealor_payment_pre?faces-redirect=true";
    }

    public String navigateToGRNPaymentApprovePharmacy() {
        bills = new ArrayList<>();
        netTotal = 0.0;
        return "/dealerPayment/search_dealor_payment_pharmacy?faces-redirect=true";
    }

    public String navigateToGRNPaymentDoneSearch() {
        bills = new ArrayList<>();
        netTotal = 0.0;
        return "/dealerPayment/dealor_payment_done_search?faces-redirect=true";
    }

    public String navigateToPaymentDoneSearch() {
        bills = new ArrayList<>();
        netTotal = 0.0;
        return "/dealerPayment/dealor_payment_done?faces-redirect=true";
    }

    public String navigateToSupplierPaymentDoneSearch() {
        bills = null;
        return "/dealerPayment/search_dealor_payment?faces-redirect=true";
    }

    public String navigateToCreditDuesAndAccess() {
        return "/credit/index_pharmacy_due_access?faces-redirect=true";
    }

    public String naviateToSupplierBillSettleManagement() {
        bills = null;
        return "/dealerPayment/supplier_bill_settle_management?faces-redirect=true";
    }

    public String naviateToSupplierBillPaymentCompletionManagement() {
        bills = null;
        return "/dealerPayment/supplier_bill_payment_completion_management?faces-redirect=true";
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Functions">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Inner Classes">
    // </editor-fold>
    @Deprecated
    public void fillPharmacySupplierPayments() {
        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.SUPPLIER_PAYMENT);
        List<InstitutionType> institutionTypes = new ArrayList<>();
        institutionTypes.add(InstitutionType.Dealer);
        createGrnPaymentTable(institutionTypes, btas);
    }

    public void fillStoreSupplierPayments() {
        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.SUPPLIER_PAYMENT);
        List<InstitutionType> institutionTypes = new ArrayList<>();
        institutionTypes.add(InstitutionType.StoreDealor);
        createGrnPaymentTable(institutionTypes, btas);
    }

    public void fillSupplierPayments() {
        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.SUPPLIER_PAYMENT);
        createGrnPaymentTable(null, btas);
    }

    private void createGrnPaymentTable(List<InstitutionType> institutionTypes, List<BillTypeAtomic> billTypes) {
        bills = null;
        String jpql;
        Map params = new HashMap();

        jpql = "select b from Bill b "
                + " where b.billTypeAtomic in :billTypes "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false ";

        if (institutionTypes != null) {
            jpql += " and b.toInstitution.institutionType in :insTps ";
            params.put("insTps", institutionTypes);
        }

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().isEmpty()) {
            jpql += " and  ((b.patient.person.name) like :patientName )";
            params.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().isEmpty()) {
            jpql += " and  ((b.patient.person.phone) like :patientPhone )";
            params.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().isEmpty()) {
            jpql += " and  ((b.insId) like :billNo )";
            params.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().isEmpty()) {
            jpql += " and  ((b.netTotal) like :netTotal )";
            params.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().isEmpty()) {
            jpql += " and  ((b.total) like :total )";
            params.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        jpql += " order by b.createdAt desc  ";
//
        params.put("billTypes", billTypes);

        params.put("toDate", getToDate());
        params.put("fromDate", getFromDate());

        bills = getBillFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);

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

    @Deprecated //Create or Use Spefici prepare methods
    public void makeNull() {
        printPreview = false;
        current = null;
        currentBillItem = null;
        institution = null;
        paymentMethodData = null;
        selectedBillItems = null;
        billItems = null;
    }

    public void prepareForNewSupplierPayment() {
        printPreview = false;
        current = new BilledBill();
        current.setBillType(BillType.GrnPayment);
        current.setBillTypeAtomic(BillTypeAtomic.SUPPLIER_PAYMENT);
        currentBillItem = null;
        paymentMethodData = null;
        selectedBillItems = new ArrayList<>();
        billItems = new ArrayList<>();
    }

    public void prepareForNewSupplierPaymentGeneration() {
        printPreview = false;
        current = new PreBill();
        current.setBillType(BillType.GrnPaymentPreparation);
        current.setBillTypeAtomic(BillTypeAtomic.SUPPLIER_PAYMENT_PREPERATION);
        currentBillItem = null;
        paymentMethodData = null;
        selectedBillItems = new ArrayList<>();;
        billItems = new ArrayList<>();
    }

    private boolean errorCheckForAdding() {
        if (getCurrentBillItem().getReferenceBill().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("U cant add without credit company name");
            return true;
        }

        if (!isPaidAmountOk(getCurrentBillItem())) {
            JsfUtil.addSuccessMessage("U cant add more than ballance");
            return true;
        }

        for (BillItem b : getBillItems()) {
            if (b.getReferenceBill() != null && b.getReferenceBill().getFromInstitution() != null) {
                if (!Objects.equals(getCurrentBillItem().getReferenceBill().getFromInstitution().getId(), b.getReferenceBill().getFromInstitution().getId())) {
                    JsfUtil.addErrorMessage("U can add only one type Credit companies at Once");
                    return true;
                }
            }
        }

        return false;
    }

    private double getReferenceBallance(BillItem billItem) {
        double refBallance;
        double neTotal = Math.abs(billItem.getReferenceBill().getNetTotal());
        double returned = Math.abs(billItem.getReferenceBill().getTmpReturnTotal());
        List<BillTypeAtomic> bts = new ArrayList<>();
        bts.add(BillTypeAtomic.SUPPLIER_PAYMENT);
        bts.add(BillTypeAtomic.SUPPLIER_PAYMENT_CANCELLED);
        bts.add(BillTypeAtomic.SUPPLIER_PAYMENT_RETURNED);
        double paidAmt = Math.abs(getCreditBean().getPaidAmountByBillTypeAtomic(billItem.getReferenceBill(), bts));

        refBallance = neTotal - (paidAmt + returned);

        return refBallance;
    }

    public void selectListener() {

        double ballanceAmt = getReferenceBallance(getCurrentBillItem());

        if (ballanceAmt > 0.1) {
            getCurrentBillItem().setNetValue(ballanceAmt);
        }

    }

    public void fillNetValueForBillItems(BillItem fillinfItem) {
        if (fillinfItem == null) {
            return;
        }
        double ballanceAmt = getReferenceBallance(fillinfItem);
        if (ballanceAmt > 0.1) {
            fillinfItem.setNetValue(ballanceAmt);
        }
    }

    @Deprecated
    public void selectInstitutionListener() {
        Institution ins = institution;
        makeNull();

        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill, BillType.StoreGrnBill, BillType.StorePurchase};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        BillType[] billTypesArrayReturn = {BillType.PharmacyGrnReturn, BillType.PurchaseReturn, BillType.StoreGrnReturn, BillType.StorePurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);

        List<Bill> list = getBillController().getDealorBills(ins, billTypesListBilled);
        for (Bill b : list) {
            getCurrentBillItem().setReferenceBill(b);
            double returned = Math.abs(getCreditBean().getGrnReturnValue(getCurrentBillItem().getReferenceBill(), billTypesListReturn));
            getCurrentBillItem().getReferenceBill().setTmpReturnTotal(returned);
            fillNetValueForBillItems(currentBillItem);
//            addToBill();
        }
        calTotalBySelectedBillTems();
    }

    public void fillInstitutionBillsToSettle() {
        Institution ins = institution;
        makeNull();
        institution = ins;
        getCurrent().setToInstitution(institution);
        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill, BillType.StoreGrnBill, BillType.StorePurchase};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        BillType[] billTypesArrayReturn = {BillType.PharmacyGrnReturn, BillType.PurchaseReturn, BillType.StoreGrnReturn, BillType.StorePurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);

        billItems = new ArrayList<>();
        selectedBillItems = new ArrayList<>();

        List<Bill> list = getBillController().getDealorBills(ins, billTypesListBilled);
        int generatedSerialNumber = 0;
        for (Bill b : list) {
            BillItem availableBillItem = new BillItem();
            availableBillItem.setSearialNo(generatedSerialNumber++);
            availableBillItem.setReferenceBill(b);
            double returned = Math.abs(getCreditBean().getGrnReturnValue(getCurrentBillItem().getReferenceBill(), billTypesListReturn));
            availableBillItem.getReferenceBill().setTmpReturnTotal(returned);
            fillNetValueForBillItems(availableBillItem);
            billItems.add(availableBillItem);
        }
        calTotalBySelectedBillTems();
    }

    public void addToBill() {

        if (errorCheckForAdding()) {
            return;
        }

        getCurrent().setToInstitution(getCurrentBillItem().getReferenceBill().getFromInstitution());
        //     getCurrentBillItem().getBill().setNetTotal(getCurrentBillItem().getNetValue());
        //     getCurrentBillItem().getBill().setTotal(getCurrent().getNetTotal());

        if (getCurrentBillItem().getNetValue() != 0) {
            //  System.err.println("11 " + getCurrentBillItem().getReferenceBill().getDeptId());
            //   System.err.println("aa " + getCurrentBillItem().getNetValue());
            getCurrentBillItem().setSearialNo(getBillItems().size());
            getBillItems().add(getCurrentBillItem());
        }

        currentBillItem = null;
        calTotal();
    }

    public void changeNetValueListener(BillItem billItem) {
        if (!isPaidAmountOk(billItem)) {
            billItem.setNetValue(0);
        }
        calculateTotalBySelectedBillItems();
    }

    public void changeDiscountListenerForPaymentPreperation() {
        calculateTotalBySelectedBillItems();
    }

    public void calTotalBySelectedBillTems() {

        if (getCurrent() == null) {
            return;
        }

        if (selectedBillItems == null || selectedBillItems.isEmpty()) {
            getCurrent().setTotal(0);
            getCurrent().setNetTotal(0);
            return;
        }

        double calculatedTotal = 0.0;

        for (BillItem sbi : selectedBillItems) {
            if (sbi != null) {
                calculatedTotal += Math.abs(sbi.getNetValue()); // Ensure positive accumulation
            }
        }

        double discount = Math.abs(getCurrent().getDiscount()); // Ensure discount is positive
        double calculatedNetTotal = calculatedTotal - discount;

        getCurrent().setTotal(-calculatedTotal); // Keep total negative for cash out
        getCurrent().setNetTotal(-calculatedNetTotal); // Keep net total negative
    }

    public void calculateTotalByCurrentBillsBillItems() {
        if (current == null || current.getBillItems() == null) {
            return;
        }

        double methodTotal = 0.0;
        for (BillItem b : current.getBillItems()) {
            methodTotal += Math.abs(b.getNetValue()); // Ensure positive values before summing
        }

        double discount = Math.abs(current.getDiscount()); // Ensure discount is positive
        double methodNetTotal = methodTotal - discount;

        current.setTotal(-methodTotal); // Keep total negative
        current.setNetTotal(-methodNetTotal); // Keep net total negative
    }

    public void calculateTotalBySelectedBillItems() {
        if (current == null || selectedBillItems == null) {
            return;
        }

        double methodTotal = 0.0;
        for (BillItem b : selectedBillItems) {
            methodTotal += Math.abs(b.getNetValue()); // Ensure all amounts are positive before summing
        }

        double discount = Math.abs(current.getDiscount()); // Ensure discount is positive
        double methodNetTotal = methodTotal - discount;

        current.setTotal(-methodTotal);  // Keep the total negative
        current.setNetTotal(-methodNetTotal);  // Keep the net total negative
    }

    public void calTotal() {
        if (billItems == null || getCurrent() == null) {
            return;
        }

        double methodTotal = 0.0;
        for (BillItem b : billItems) {
            methodTotal += Math.abs(b.getNetValue()); // Ensure positive sum
        }

        double discount = Math.abs(getCurrent().getDiscount()); // Ensure discount is positive
        double methodNetTotal = methodTotal - discount;

        getCurrent().setTotal(-methodTotal);  // Keep total negative
        getCurrent().setNetTotal(-methodNetTotal);  // Keep net total negative
    }

    public void calTotal(Bill b) {
        if (b.getBillItems() == null || b == null) {
            return;
        }

        double methodTotal = 0.0;
        for (BillItem bi : b.getBillItems()) {
            methodTotal += Math.abs(bi.getNetValue()); // Ensure positive sum
        }

        double discount = Math.abs(b.getDiscount()); // Ensure discount is positive
        double methodNetTotal = methodTotal - discount;

        b.setTotal(-methodTotal);  // Keep total negative
        b.setNetTotal(-methodNetTotal);  // Keep net total negative
        changed = true;
    }

    public void calculateTotal(List<BillItem> billItemsWithReferanceToSettlingBills) {
        double n = 0.0;
        for (BillItem payingBillItem : billItemsWithReferanceToSettlingBills) {
            double biNetTotal = 0 - Math.abs(payingBillItem.getNetValue());
            payingBillItem.setNetValue(biNetTotal);
            payingBillItem.setGrossValue(biNetTotal);
            n += payingBillItem.getNetValue();
        }
        getCurrent().setTotal(n);
        getCurrent().setNetTotal(n);
    }

    public void updateReferanceBillBalances(List<BillItem> billItemsWithReferanceToSettlingBills) {
        double n = 0.0;
        for (BillItem payingBillItem : billItemsWithReferanceToSettlingBills) {
            double biNetTotal = 0 - Math.abs(payingBillItem.getNetValue());
            payingBillItem.setNetValue(biNetTotal);
            payingBillItem.setGrossValue(biNetTotal);
            n += payingBillItem.getNetValue();
            Bill originalBill = payingBillItem.getReferenceBill();
            double previouslyPaidAmount = Math.abs(originalBill.getPaidAmount());
            double payingThisTime = Math.abs(biNetTotal);
            double totalInitialToBePaid = Math.abs(originalBill.getNetTotal());
            double totalRefundsToOrigianl = Math.abs(originalBill.getRefundAmount());
            double originalBillBallance = (totalInitialToBePaid - (totalRefundsToOrigianl)) - (previouslyPaidAmount + payingThisTime);
            originalBill.setPaidAmount(previouslyPaidAmount + payingThisTime);
            originalBill.setBalance(originalBillBallance);
            billFacade.edit(originalBill);
        }
//        getCurrent().setTotal(n);
//        getCurrent().setNetTotal(n);
    }

    public void updateReferanceBillCompletionStatus(List<BillItem> billItemsWithReferanceToSettlingBills) {
        for (BillItem payingBillItem : billItemsWithReferanceToSettlingBills) {
            Bill originalBill = payingBillItem.getReferenceBill();
            if (originalBill == null) {
                continue;
            }
            originalBill.setPaymentGenerated(false);
            billFacade.edit(originalBill);
        }
    }

    public void updateReferanceBillAsPaymentApproved(List<BillItem> billItemsWithReferanceToSettlingBills) {
        for (BillItem payingBillItem : billItemsWithReferanceToSettlingBills) {
            Bill originalBill = payingBillItem.getReferenceBill();
            originalBill.setPaymentApproved(true);
            originalBill.setPaymentApprovedAt(new Date());
            originalBill.setPaymentApprovedBy(sessionController.getLoggedUser());
            billFacade.edit(originalBill);
        }
    }

    public void updateReferanceBillAsPaymentGenerated(List<BillItem> billItemsWithReferanceToSettlingBills) {
        for (BillItem payingBillItem : billItemsWithReferanceToSettlingBills) {
            Bill originalBill = payingBillItem.getReferenceBill();

            originalBill.setPaymentGenerated(true);
            originalBill.setPaymentGeneratedAt(new Date());
            originalBill.setPaymentGeneratedBy(sessionController.getLoggedUser());

            originalBill.setPaymentApproved(false);

            billFacade.edit(originalBill);
        }
    }

    public void calTotalWithResetingIndex() {
        int index = 0;
        double n = 0.0;
        for (BillItem b : billItems) {
            b.setSearialNo(index++);
            n += b.getNetValue();
        }
        getCurrent().setNetTotal(0 - n);
    }

    public void calTotalWithResetingIndexSelected() {
        int index = 0;
        double n = 0.0;
        for (BillItem b : selectedBillItems) {
            b.setSearialNo(index++);
            n += b.getNetValue();
        }
        getCurrent().setNetTotal(0 - n);
    }

    public void removeAll() {
        for (BillItem b : selectedBillItems) {
            remove(b);
        }

        //   calTotalWithResetingIndex();
        selectedBillItems = null;
    }

//    public void deselectAllBillItems() {
//        if (selectedBillItems == null) {
//            selectedBillItems = new ArrayList<>();
//        } else {
//            selectedBillItems.clear();
//        }
//    }
//
//    public void selectAllBillItems() {
//        if (billItems == null || billItems.isEmpty()) {
//            selectedBillItems = new ArrayList<>();
//        } else {
//            selectedBillItems = new ArrayList<>(billItems);
//        }
//    }
    public void remove(BillItem billItem) {
        getBillItems().remove(billItem); // removes by object, not by index
        calTotalWithResetingIndex();
    }

    public void removeselected(BillItem billItem) {
        if (selectedBillItems != null) {
            selectedBillItems.remove(billItem); // removes by object, not by index
            calTotalWithResetingIndexSelected();
            calculateTotalBySelectedBillItems();
        }
    }

    public void removeFromCurrent(BillItem billItem) {
        getCurrent().getBillItems().remove(billItem); // removes by object, not by index
        updateReferanceBillAsNotPaymentGenerated(billItem);
        calTotal(current);
        billItem.setRetired(true);
    }

    public void removeWithoutIndex(BillItem billItem) {
        getBillItems().remove(billItem.getSearialNo());

    }

    public void updateReferanceBillAsNotPaymentGenerated(BillItem billItemsWithReferanceToSettlingBills) {
        Bill originalBill = billItemsWithReferanceToSettlingBills.getReferenceBill();

        originalBill.setPaymentGenerated(false);
        originalBill.setPaymentGeneratedAt(null);
        originalBill.setPaymentGeneratedBy(null);

        originalBill.setPaymentApproved(false);

        billFacade.edit(originalBill);
    }

    private boolean errorCheck() {
        if (getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return true;
        }

        if (getCurrent().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Select Cant settle without Dealor");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), getPaymentMethodData())) {
            return true;
        }

        return false;
    }

    private boolean errorCheckForAllSelectedItemsSettlingBill() {
        if (getSelectedBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return true;
        }

        if (getCurrent().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Select Cant settle without Dealor");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), getPaymentMethodData())) {
            return true;
        }

        return false;
    }

    private boolean errorCheckForSettlingApprovedPayments() {
        if (getCurrent() == null) {
            JsfUtil.addErrorMessage("Nothing to settle");
            return true;
        }
        if (getCurrent().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return true;
        }

        if (getCurrent().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Select Cant settle without Dealor");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), getPaymentMethodData())) {
            return true;
        }

        return false;
    }

    private boolean errorCheckForCompletingApprovedAndSettledPayment() {
        if (getCurrent() == null) {
            JsfUtil.addErrorMessage("Nothing to settle");
            return true;
        }
        if (getCurrent().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return true;
        }

        if (getCurrent().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Select Cant settle without Dealor");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }
        return false;
    }

    private boolean errorCheckForSettlingSelectedSupplierBills() {
        if (getSelectedBillItems() == null || getSelectedBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill is selected to pay");
            return true;
        }

        if (getCurrent().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Can not select without a Supplier");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), getPaymentMethodData())) {
            return true;
        }

        return false;
    }

    private boolean errorCheckForSettlingPaymentForApprovedPayment() {
        if (getCurrent() == null) {
            JsfUtil.addErrorMessage("No Current Bill");
            return true;
        }
        if (getCurrent().getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No Bill Type Atomic");
            return true;
        }
        if (getCurrent().getBillTypeAtomic() != BillTypeAtomic.SUPPLIER_PAYMENT) {
            JsfUtil.addErrorMessage("Wrong Bill Type Atomic");
            return true;
        }
        if (getCurrent().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Items");
            return true;
        }
        if (getCurrent().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Select Cant settle without Dealor");
            return true;
        }
        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }
        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), getPaymentMethodData())) {
            return true;
        }
        return false;
    }

    private boolean errorCheckForPaymentPreperationBill() {
        if (getSelectedBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return true;
        }

        if (getCurrent().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Select Cant settle without Dealor");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        return false;
    }

    private boolean errorCheckForApprovingSupplierPayment() {
        if (getCurrent() == null) {
            JsfUtil.addErrorMessage("Nothing to approve");
            return true;
        }

        if (getCurrent().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return true;
        }

        if (!getCurrent().isCompleted()) {
            JsfUtil.addErrorMessage("Need to check before Approving");
            return true;
        }

        if (getCurrent().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Select Cant settle without Dealor");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        return false;
    }

    private boolean errorCheckForCheckingSupplierPayment() {
        if (getCurrent() == null) {
            JsfUtil.addErrorMessage("Nothing to approve");
            return true;
        }

        if (getCurrent().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return true;
        }
        if (getCurrent().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Select Cant settle without Dealor");
            return true;
        }
        if (getCurrent().isCompleted()) {
            JsfUtil.addErrorMessage("Already Checked, Cano not mark as checked again.");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        return false;
    }

    private void saveBill(BillType billType) {
        getCurrent().setToInstitution(institution);
        getCurrent().setFromInstitution(getSessionController().getInstitution());
        getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), billType, BillClassType.BilledBill, BillNumberSuffix.CRDPAY));
        getCurrent().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), billType, BillClassType.BilledBill, BillNumberSuffix.CRDPAY));

        getCurrent().setBillType(billType);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.SUPPLIER_PAYMENT);

        getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrent().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());

        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

//        getCurrent().setNetTotal(getCurrent().getNetTotal());
        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        } else {
            getBillFacade().edit(getCurrent());
        }

    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void fillUnsettledCreditPharmacyBills() {
        BillTypeAtomic[] billTypesArrayBilled = {
            BillTypeAtomic.PHARMACY_GRN,
            BillTypeAtomic.PHARMACY_GRN_CANCELLED,
            BillTypeAtomic.PHARMACY_DIRECT_PURCHASE,
            BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL,
            BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL};
        List<BillTypeAtomic> billTypesListBilled = Arrays.asList(billTypesArrayBilled);

        boolean needPaymentApproval = configOptionApplicationController.getBooleanValueByKey("Approval is necessary for Procument Payments", false);
        if (needPaymentApproval) {
            bills = billController.findUnpaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01, true);
        } else {
            bills = billController.findUnpaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01);
        }

        netTotal = 0.0;
        paidAmount = 0.0;
        refundAmount = 0.0;
        balance = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
            paidAmount += b.getPaidAmount();
            balance += b.getBalance();
            refundAmount += b.getRefundAmount();
        }
    }

    public void fillUnsettledCreditPharmacyReturnBills() {
        BillTypeAtomic[] billTypesArrayBilled = {
            BillTypeAtomic.PHARMACY_GRN_RETURN,
            BillTypeAtomic.PHARMACY_GRN_REFUND};
        List<BillTypeAtomic> billTypesListBilled = Arrays.asList(billTypesArrayBilled);

        boolean needPaymentApproval = configOptionApplicationController.getBooleanValueByKey("Approval is necessary for Procument Payments", false);
        if (needPaymentApproval) {
            bills = billController.findUnpaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01, true);
        } else {
            bills = billController.findUnpaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01);
        }

        netTotal = 0.0;
        paidAmount = 0.0;
        refundAmount = 0.0;
        balance = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
            paidAmount += b.getPaidAmount();
            balance += b.getBalance();
            refundAmount += b.getRefundAmount();
        }
    }

    public void fillSupplierPaymentsToApprove() {
        BillTypeAtomic[] billTypesArrayBilled = {BillTypeAtomic.SUPPLIER_PAYMENT_PREPERATION};
        List<BillTypeAtomic> billTypesListBilled = Arrays.asList(billTypesArrayBilled);

//        bills = billController.findUnpaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01, true);
        String jpql = "SELECT b FROM Bill b WHERE b.retired = :ret AND b.cancelled = :can "
                + "AND b.createdAt BETWEEN :frm AND :to";

        HashMap<String, Object> params = new HashMap<>();
        params.put("frm", fromDate);
        params.put("to", toDate);
        params.put("ret", false);
        params.put("can", false);

        jpql += " AND (b.billTypeAtomic IN :bts)";
        params.put("bts", billTypesListBilled);

        jpql += " AND (b.paymentApproved = false OR b.paymentApproved IS NULL) "
                + "AND (b.paymentGenerated = 0 OR b.paymentGenerated IS NULL)";
        // Logging

        bills = getBillFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);

        netTotal = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
        }
    }

    public void fillSupplierPaymentsDone() {
        BillTypeAtomic[] billTypesArrayBilled = {BillTypeAtomic.SUPPLIER_PAYMENT};
        List<BillTypeAtomic> billTypesListBilled = Arrays.asList(billTypesArrayBilled);

        bills = billController.findPaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01);

        String jpql = "SELECT b FROM Bill b WHERE b.retired = :ret AND b.cancelled = :can "
                + "AND b.createdAt BETWEEN :frm AND :to";

        HashMap<String, Object> params = new HashMap<>();
        params.put("frm", fromDate);
        params.put("to", toDate);
        params.put("ret", false);
        params.put("can", false);

        jpql += " AND (b.billTypeAtomic IN :bts)";
        params.put("bts", billTypesListBilled);
        // Logging

        bills = getBillFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);

        netTotal = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
        }
    }

    public void listSupplierPayments() {
        BillTypeAtomic[] billTypesArrayBilled = {BillTypeAtomic.SUPPLIER_PAYMENT};
        List<BillTypeAtomic> billTypesListBilled = Arrays.asList(billTypesArrayBilled);

        bills = billController.findUnpaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01, true);

        String jpql = "SELECT b FROM Bill b WHERE b.retired = :ret AND b.cancelled = :can "
                + "AND b.createdAt BETWEEN :frm AND :to";

        HashMap<String, Object> params = new HashMap<>();
        params.put("frm", fromDate);
        params.put("to", toDate);
        params.put("ret", false);
        params.put("can", false);

        jpql += " AND (b.billTypeAtomic IN :bts)";
        params.put("bts", billTypesListBilled);

        bills = getBillFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);

        netTotal = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
        }
    }

    public void fillSupplierPaymentsToSettle() {
        BillTypeAtomic[] billTypesArrayBilled = {BillTypeAtomic.SUPPLIER_PAYMENT_PREPERATION};
        List<BillTypeAtomic> billTypesListBilled = Arrays.asList(billTypesArrayBilled);

        bills = billController.findUnpaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01, true);

        String jpql = "SELECT b FROM Bill b WHERE b.retired = :ret AND b.cancelled = :can "
                + "AND (b.paymentGenerated = false OR b.paymentGenerated = 0) "
                + "AND b.createdAt BETWEEN :frm AND :to";

        HashMap<String, Object> params = new HashMap<>();
        params.put("frm", fromDate);
        params.put("to", toDate);
        params.put("ret", false);
        params.put("can", false);

        jpql += " AND (b.billTypeAtomic IN :bts)";
        params.put("bts", billTypesListBilled);

        jpql += " AND (b.paymentApproved = true OR b.paymentApproved = 1 ) ";
        // Logging

        bills = getBillFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);

        netTotal = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
        }
    }

    public void fillSettledCreditPharmacyBills() {
        BillTypeAtomic[] billTypesArrayBilled = {
            BillTypeAtomic.PHARMACY_GRN,
            BillTypeAtomic.PHARMACY_GRN_CANCELLED,
            BillTypeAtomic.PHARMACY_GRN_RETURN,
            BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL_CANCELLED,
            BillTypeAtomic.PHARMACY_DIRECT_PURCHASE,
            BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL,
            BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL};
        List<BillTypeAtomic> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        bills = billController.findPaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01);
        netTotal = 0.0;
        paidAmount = 0.0;
        refundAmount = 0.0;
        balance = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
            paidAmount += b.getPaidAmount();
            balance += b.getBalance();
            refundAmount += b.getRefundAmount();
        }
    }

    public void fillUnsettledCreditStoreBills() {
        BillTypeAtomic[] billTypesArrayBilled = {BillTypeAtomic.STORE_GRN,
            BillTypeAtomic.STORE_DIRECT_PURCHASE,
            BillTypeAtomic.STORE_GRN_CANCELLED,
            BillTypeAtomic.STORE_GRN_REFUND,
            BillTypeAtomic.STORE_GRN_RETURN};
        List<BillTypeAtomic> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        boolean needPaymentApproval = configOptionApplicationController.getBooleanValueByKey("Approval is necessary for Procument Payments", false);
        if (needPaymentApproval) {
            bills = billController.findUnpaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01, false);
        } else {
            bills = billController.findUnpaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01);
        }
        netTotal = 0.0;
        paidAmount = 0.0;
        refundAmount = 0.0;
        balance = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
            paidAmount += b.getPaidAmount();
            balance += b.getBalance();
            refundAmount += b.getRefundAmount();
        }
    }

    public void fillSettledCreditStoreBills() {
        BillTypeAtomic[] billTypesArrayBilled = {BillTypeAtomic.STORE_GRN, BillTypeAtomic.STORE_DIRECT_PURCHASE};
        List<BillTypeAtomic> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        bills = billController.findPaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01);
        netTotal = 0.0;
        paidAmount = 0.0;
        refundAmount = 0.0;
        balance = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
            paidAmount += b.getPaidAmount();
            balance += b.getBalance();
            refundAmount += b.getRefundAmount();
        }
    }

    public void fillUnsettledCreditBills() {
        BillTypeAtomic[] billTypesArrayBilled = {
            BillTypeAtomic.PHARMACY_GRN,
            BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL,
            BillTypeAtomic.PHARMACY_DIRECT_PURCHASE,
            BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL,
            BillTypeAtomic.PHARMACY_GRN_RETURN,
            BillTypeAtomic.PHARMACY_GRN_REFUND,
            BillTypeAtomic.STORE_GRN,
            BillTypeAtomic.STORE_DIRECT_PURCHASE,
            BillTypeAtomic.STORE_GRN_CANCELLED,
            BillTypeAtomic.STORE_GRN_REFUND,
            BillTypeAtomic.STORE_GRN_RETURN};
        List<BillTypeAtomic> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        boolean needPaymentApproval = configOptionApplicationController.getBooleanValueByKey("Approval is necessary for Procument Payments", false);
        if (needPaymentApproval) {
            bills = billController.findUnpaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01, false);
        } else {
            bills = billController.findUnpaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01);
        }
        netTotal = 0.0;
        paidAmount = 0.0;
        refundAmount = 0.0;
        balance = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
            paidAmount += b.getPaidAmount();
            balance += b.getBalance();
            refundAmount += b.getRefundAmount();
        }
    }

    public void fillSettledCreditBills() {
        BillTypeAtomic[] billTypesArrayBilled = {
            BillTypeAtomic.PHARMACY_GRN,
            BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL,
            BillTypeAtomic.PHARMACY_DIRECT_PURCHASE, 
            BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL, 
            BillTypeAtomic.STORE_GRN, 
            BillTypeAtomic.STORE_DIRECT_PURCHASE};
        List<BillTypeAtomic> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        bills = billController.findPaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01);
        netTotal = 0.0;
        paidAmount = 0.0;
        refundAmount = 0.0;
        balance = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
            paidAmount += b.getPaidAmount();
            balance += b.getBalance();
            refundAmount += b.getRefundAmount();
        }
    }

    public void fillAllCreditBills() {
        bills = null;
        String jpql;
        Map params = new HashMap();
        List<BillTypeAtomic> billTypesAtomics = new ArrayList<>();
        billTypesAtomics.add(BillTypeAtomic.PHARMACY_GRN);
        billTypesAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL);
        billTypesAtomics.add(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
        billTypesAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL);
        billTypesAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL);

        jpql = "select b from Bill b "
                + " where b.billTypeAtomic in :billTypes "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false ";

        params.put("billTypes", billTypesAtomics);
        params.put("toDate", getToDate());
        params.put("fromDate", getFromDate());

        bills = getBillFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public void fillAllCanceledReturnedSupplierPayments() {
        bills = null;
        netTotal = 0.0;
        String jpql;
        Map params = new HashMap();
        List<BillTypeAtomic> billTypesListBilled = new ArrayList<>();
        billTypesListBilled.add(BillTypeAtomic.SUPPLIER_PAYMENT_CANCELLED);
        billTypesListBilled.add(BillTypeAtomic.SUPPLIER_PAYMENT_RETURNED);

        jpql = "select b from Bill b "
                + " where b.retired=false "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.billType = :billTypes "
                + " and b.billTypeAtomic IN :bTA ";
        params.put("billTypes", BillType.GrnPayment);
        params.put("bTA", billTypesListBilled);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        bills = getBillFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);

        netTotal = 0.0;
        paidAmount = 0.0;
        refundAmount = 0.0;
        balance = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
            paidAmount += b.getPaidAmount();
            balance += b.getBalance();
            refundAmount += b.getRefundAmount();
        }
    }

    public void fillPharmacySupplierPayment() {
        List<InstitutionType> institutionTypes = new ArrayList<>();
        institutionTypes.add(InstitutionType.Dealer);
        fillAllCreditBillssettled(institutionTypes);
    }

    public void fillStoreSupplierPayment() {
        List<InstitutionType> institutionTypes = new ArrayList<>();
        institutionTypes.add(InstitutionType.StoreDealor);
        fillAllCreditBillssettled(institutionTypes);
    }

    public void fillAllSupplierPayment() {
        fillAllCreditBillssettled(null);
    }

    public void fillAllCreditBillssettled(List<InstitutionType> institutionTypes) {
        bills = null;
        netTotal = 0.0;

        // Ensure dates are not null
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("From Date and To Date Empty");
        }

        // Build JPQL query
        StringBuilder jpql = new StringBuilder("SELECT b FROM Bill b "
                + "WHERE b.retired = false "
                + "AND b.cancelled = false "
                + "AND b.refunded = false "
                + "AND b.createdAt BETWEEN :fromDate AND :toDate "
                + "AND (b.billType = :billType OR b.billTypeAtomic = :billTypeAtomic) ");

        // Initialize parameters map
        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        params.put("billType", BillType.GrnPaymentPre);
        params.put("billTypeAtomic", BillTypeAtomic.SUPPLIER_PAYMENT);

        if (institutionTypes != null) {
            jpql.append(" and b.toInstitution.institutionType in :insTps ");
            params.put("insTps", institutionTypes);
        }

        // Append optional filters if they are provided
        if (chequeFromDate != null && chequeToDate != null) {
            jpql.append("AND b.chequeDate BETWEEN :chequeFromDate AND :chequeToDate ");
            params.put("chequeFromDate", chequeFromDate);
            params.put("chequeToDate", chequeToDate);
        }

        if (chequeNo != null && !chequeNo.trim().isEmpty()) {
            jpql.append("AND b.chequeRefNo = :chequeRefNo ");
            params.put("chequeRefNo", chequeNo);
        }
        if (toInstitution != null) {
            jpql.append("AND b.toInstitution = :supplier ");
            params.put("supplier", toInstitution);
        }
        if (bank != null) {
            jpql.append("AND b.bank = :bank ");
            params.put("bank", bank);
        }

        jpql.append("AND b.billType <> :excludeBillType ");
        params.put("excludeBillType", BillType.GrnPayment);
        jpql.append("ORDER BY b.chequeDate");

        // Execute query
        bills = getBillFacade().findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        // Calculate net total
        Iterator<Bill> iterator = bills.iterator();
        while (iterator.hasNext()) {
            Bill b = iterator.next();
            netTotal += b.getNetTotal();
        }

    }

    @Deprecated
    public void fillAllCreditBillssettledByChequeDate() {
        bills = null;
        netTotal = 0.0;

        // Ensure dates are not null
        if (chequeFromDate == null || chequeToDate == null) {
            JsfUtil.addErrorMessage("Cheque From Date and Cheque To Date Empty");
        }

        // Build JPQL query
        StringBuilder jpql = new StringBuilder("SELECT b FROM Bill b "
                + "WHERE b.retired = false "
                + "AND b.cancelled = false "
                + "AND b.refunded = false "
                + "AND b.chequeDate BETWEEN :chequeFromDate AND :chequeToDate "
                + "AND (b.billType = :billType OR b.billTypeAtomic = :billTypeAtomic) ");

        // Initialize parameters map
        Map<String, Object> params = new HashMap<>();
        params.put("chequeFromDate", chequeFromDate);
        params.put("chequeToDate", chequeToDate);
        params.put("billType", BillType.GrnPaymentPre);
        params.put("billTypeAtomic", BillTypeAtomic.SUPPLIER_PAYMENT);

        if (chequeNo != null && !chequeNo.trim().isEmpty()) {
            jpql.append("AND b.chequeRefNo = :chequeRefNo ");
            params.put("chequeRefNo", chequeNo);
        }
        if (toInstitution != null) {
            jpql.append("AND b.toInstitution = :supplier ");
            params.put("supplier", toInstitution);
        }
        if (bank != null) {
            jpql.append("AND b.bank = :bank ");
            params.put("bank", bank);
        }

        jpql.append("AND b.billType <> :excludeBillType ");
        params.put("excludeBillType", BillType.GrnPayment);
        jpql.append("ORDER BY b.id");

        // Execute query
        bills = getBillFacade().findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        // Calculate net total
//        netTotal = bills.stream()
//                .mapToDouble(Bill::getNetTotal)
//                .sum();
        Iterator<Bill> iterator = bills.iterator();
        while (iterator.hasNext()) {
            Bill b = iterator.next();
            netTotal += b.getNetTotal();
        }

    }

    public void fillPharmacySupplierPaymentsByChequDate() {
        List<InstitutionType> institutionTypes = new ArrayList<>();
        institutionTypes.add(InstitutionType.Dealer);
        createSupplierPaymentTableByChequDate(institutionTypes);
    }

    public void fillStoreSupplierPaymentByChequeDate() {
        List<InstitutionType> institutionTypes = new ArrayList<>();
        institutionTypes.add(InstitutionType.StoreDealor);
        createSupplierPaymentTableByChequDate(institutionTypes);
    }

    public void fillAllSupplierPaymentsByChequDate() {
        createSupplierPaymentTableByChequDate(null);
    }

    public void createSupplierPaymentTableByChequDate(List<InstitutionType> institutionTypes) {
        bills = null;
        netTotal = 0.0;

        // Ensure dates are not null
        if (chequeFromDate == null || chequeToDate == null) {
            JsfUtil.addErrorMessage("Cheque From Date and Cheque To Date Empty");
        }

        // Build JPQL query
        StringBuilder jpql = new StringBuilder("SELECT b FROM Bill b "
                + "WHERE b.retired = false "
                + "AND b.cancelled = false "
                + "AND b.refunded = false "
                + "AND b.chequeDate BETWEEN :chequeFromDate AND :chequeToDate "
                + "AND (b.billType = :billType OR b.billTypeAtomic = :billTypeAtomic) ");

        Map<String, Object> params = new HashMap<>();
        params.put("chequeFromDate", chequeFromDate);
        params.put("chequeToDate", chequeToDate);
        params.put("billType", BillType.GrnPaymentPre);
        params.put("billTypeAtomic", BillTypeAtomic.SUPPLIER_PAYMENT);

        if (institutionTypes != null) {
            jpql.append(" and b.toInstitution.institutionType in :insTps ");
            params.put("insTps", institutionTypes);
        }

        if (supplierPaymentStatus != null && !supplierPaymentStatus.equals("Any")) {
            switch (supplierPaymentStatus) {
                case "Pending":
                    jpql.append(" AND b.referenceBill IS NULL ");
                    break;
                case "Approved":
                    jpql.append(" AND b.referenceBill.billType = :approvedBillType AND b.referenceBill.cancelled = false ");
                    params.put("approvedBillType", BillType.GrnPayment);
                    break;
                case "Canceled":
                    jpql.append(" AND b.referenceBill.cancelled = true ");
                    break;
            }
        }

        if (chequeNo != null && !chequeNo.trim().isEmpty()) {
            jpql.append("AND b.chequeRefNo = :chequeRefNo ");
            params.put("chequeRefNo", chequeNo);
        }
        if (toInstitution != null) {
            jpql.append("AND b.toInstitution = :supplier ");
            params.put("supplier", toInstitution);
        }
        if (bank != null) {
            jpql.append("AND b.bank = :bank ");
            params.put("bank", bank);
        }

        jpql.append("AND b.billType <> :excludeBillType ");
        params.put("excludeBillType", BillType.GrnPayment);
        jpql.append("ORDER BY b.chequeDate");

        bills = getBillFacade().findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        Iterator<Bill> iterator = bills.iterator();
        while (iterator.hasNext()) {
            Bill b = iterator.next();
            netTotal += b.getNetTotal();
        }
    }

    public void fillAllCancelledSupplierPaymentsByChequDate() {
        bills = null;
        netTotal = 0.0;

        // Ensure dates are not null
        if (chequeFromDate == null || chequeToDate == null) {
            JsfUtil.addErrorMessage("Cheque From Date and Cheque To Date Empty");
        }

        // Build JPQL query
        StringBuilder jpql = new StringBuilder("SELECT b FROM Bill b "
                + "WHERE b.retired = false "
                + "AND b.reactivated = true "
                + "AND b.chequeDate BETWEEN :chequeFromDate AND :chequeToDate "
                + "AND (b.billType = :billType OR b.billTypeAtomic = :billTypeAtomic) ");

        Map<String, Object> params = new HashMap<>();
        params.put("chequeFromDate", chequeFromDate);
        params.put("chequeToDate", chequeToDate);
        params.put("billType", BillType.GrnPaymentPre);
        params.put("billTypeAtomic", BillTypeAtomic.SUPPLIER_PAYMENT);

        if (supplierPaymentStatus != null && !supplierPaymentStatus.equals("Any")) {
            switch (supplierPaymentStatus) {
                case "Pending":
                    jpql.append(" AND b.referenceBill IS NULL ");
                    break;
                case "Approved":
                    jpql.append(" AND b.referenceBill.billType = :approvedBillType AND b.referenceBill.cancelled = false ");
                    params.put("approvedBillType", BillType.GrnPayment);
                    break;
                case "Canceled":
                    jpql.append(" AND b.referenceBill.cancelled = true ");
                    break;
            }
        }

        if (chequeNo != null && !chequeNo.trim().isEmpty()) {
            jpql.append("AND b.chequeRefNo = :chequeRefNo ");
            params.put("chequeRefNo", chequeNo);
        }
        if (toInstitution != null) {
            jpql.append("AND b.toInstitution = :supplier ");
            params.put("supplier", toInstitution);
        }
        if (bank != null) {
            jpql.append("AND b.bank = :bank ");
            params.put("bank", bank);
        }

        jpql.append("AND b.billType <> :excludeBillType ");
        params.put("excludeBillType", BillType.GrnPayment);
        jpql.append("ORDER BY b.chequeDate");

        bills = getBillFacade().findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        Iterator<Bill> iterator = bills.iterator();
        while (iterator.hasNext()) {
            Bill b = iterator.next();
            netTotal += b.getNetTotal();
        }
    }

    public void fillDealorPaymentDone() {
        bills = null;
        netTotal = 0.0;
        supplierPaymentStatus = "Any";
        String jpql;
        Map params = new HashMap();

        jpql = "select b from Bill b "
                + " where b.retired=false "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.billType = :billTypes "
                + " and b.billTypeAtomic = :bTA ";

        params.put("billTypes", BillType.GrnPayment);
        params.put("bTA", BillTypeAtomic.SUPPLIER_PAYMENT);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        bills = getBillFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);

        Iterator<Bill> iterator = bills.iterator();
        while (iterator.hasNext()) {
            Bill b = iterator.next();
            netTotal += b.getNetTotal();
        }
    }

    public void fillSupplierPayments(Boolean completed, Boolean paymentCompleted) {
        bills = null;
        netTotal = 0.0;
        StringBuilder jpql = new StringBuilder("select b from Bill b "
                + " where b.retired=:retired "
                + " and b.cancelled=:cancelled "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.billTypeAtomic in :btas ");

        Map<String, Object> params = new HashMap<>();
        List<BillTypeAtomic> btas = Arrays.asList(
                BillTypeAtomic.SUPPLIER_PAYMENT_PREPERATION);

        params.put("btas", btas);
        params.put("cancelled", false);
        params.put("retired", false);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        // Conditionally append paymentApproved if parameter is not null
        if (completed != null) {
            if (completed) {
                jpql.append(" and b.completed = :completed ");
                params.put("completed", true);
            } else {
                jpql.append(" and b.completed = :completed ");
                params.put("completed", false);
            }
        }

        // Conditionally append paymentCompleted if parameter is not null
        if (paymentCompleted != null) {
            if (paymentCompleted) {
                jpql.append(" and b.paymentCompleted = :completed ");
                params.put("completed", true);
            } else {
                jpql.append(" and b.paymentCompleted = :completed ");
                params.put("completed", false);
            }
        }

        bills = getBillFacade().findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        netTotal = bills.stream().mapToDouble(Bill::getNetTotal).sum();
    }

    public void fillSupplierPayments(Boolean completed, Boolean paymentApproved, Boolean paymentCompleted) {
        bills = null;
        netTotal = 0.0;
        StringBuilder jpql = new StringBuilder("select b from Bill b "
                + " where b.retired=:retired "
                + " and b.cancelled=:cancelled "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.billTypeAtomic in :btas ");

        Map<String, Object> params = new HashMap<>();
        List<BillTypeAtomic> btas = Arrays.asList(
                BillTypeAtomic.SUPPLIER_PAYMENT_PREPERATION);

        params.put("btas", btas);
        params.put("cancelled", false);
        params.put("retired", false);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        // Conditionally append paymentApproved if parameter is not null
        if (completed != null) {
            if (completed) {
                jpql.append(" and b.completed = :completed ");
                params.put("completed", true);
            } else {
                jpql.append(" and b.completed != :completed ");
                params.put("completed", true);
            }
        }

        // Conditionally append paymentCompleted if parameter is not null
        if (paymentCompleted != null) {
            if (paymentCompleted) {
                jpql.append(" and b.paymentCompleted = :paymentCompleted ");
                params.put("paymentCompleted", true);
            } else {
                jpql.append(" and b.paymentCompleted != :paymentCompleted ");
                params.put("paymentCompleted", true);
            }
        }

        // Conditionally append paymentCompleted if parameter is not null
        if (paymentApproved != null) {
            if (paymentApproved) {
                jpql.append(" and b.paymentApproved = :paymentApproved ");
                params.put("paymentApproved", true);
            } else {
                jpql.append(" and b.paymentApproved != :paymentApproved ");
                params.put("paymentApproved", true);
            }
        }

        bills = getBillFacade().findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        netTotal = bills.stream().mapToDouble(Bill::getNetTotal).sum();
    }

    public void fillApprovedSupplierPaymentsToSettle() {
        supplierPaymentStatus = "Pending Settling";
        fillSupplierPayments(false, null);
    }

    public void fillApprovedSupplierPaymentsSettled() {
        supplierPaymentStatus = "Settled";
        fillSupplierPayments(true, null);
    }

    public void fillApprovedSupplierPaymentsSettledOrPending() {
        supplierPaymentStatus = "All";
        fillSupplierPayments(null, null);
    }

    public void fillApprovedSupplierPaymentsToComplete() {
        supplierPaymentStatus = "Pending Completion";
        fillSupplierPayments(null, true, false);
    }

    public void fillSupplierPaymentsIgnoringApprovealAndCompletion() {
        supplierPaymentStatus = "All";
        fillSupplierPayments(null, null);
    }

    public String navigateToSettleApprovedPayment() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Please select a bill");
            return null;
        }

        if (bill.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("System Error. No bill type atomic");
            return null;
        }

        if (bill.getBillTypeAtomic() != BillTypeAtomic.SUPPLIER_PAYMENT_PREPERATION) {
            JsfUtil.addErrorMessage("System Error. Wrong bill type atomic");
            return null;
        }

        printPreview = false;

        billService.reloadBill(bill);

        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("Supplier Payment Approval Bill is Cancelled. Can NOT proceed");
            return null;
        }

        if (bill.isCompleted()) {
            JsfUtil.addErrorMessage("Supplier Payment Approval Bill is already settled. Can NOT proceed");
            return null;
        }

        if (bill.getBillItems() == null) {
            JsfUtil.addErrorMessage("System Error. Bill Items Null. Can NOT proceed");
            return null;
        }

        if (bill.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("System Error. No Bill Items. Can NOT proceed");
            return null;
        }

        current = new BilledBill();

        current.copy(bill);
        current.copyValue(bill);
        current.setReferenceBill(bill);

        current.setBillType(BillType.GrnPayment);
        current.setBillTypeAtomic(BillTypeAtomic.SUPPLIER_PAYMENT);

        current.setFromInstitution(bill.getFromInstitution());
        current.setFromDepartment(bill.getFromDepartment());
        current.setToInstitution(bill.getToInstitution());
        current.setReferenceBill(bill);
        current.setInstitution(sessionController.getInstitution());
        current.setDepartment(sessionController.getDepartment());
        current.setCreatedAt(new Date());
        current.setCreater(sessionController.getLoggedUser());

        paymentMethodData = null;
        selectedBillItems = new ArrayList<>();
        billItems = new ArrayList<>();

        for (BillItem originalBillItem : bill.getBillItems()) {
            BillItem newlyCreateBillItem = new BillItem();
            newlyCreateBillItem.copy(originalBillItem);
            newlyCreateBillItem.setReferanceBillItem(originalBillItem);
            newlyCreateBillItem.setBill(bill);
            getBillItems().add(currentBillItem);
        }

        return "/dealerPayment/approve_bill_for_payment?faces-redirect=true";
    }

    public String approvePayment() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return null;
        }
        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("Bill Cancelled. Can not approve payment");
            return null;
        }
        if (bill.isPaymentApproved()) {
            JsfUtil.addErrorMessage("Bill Payment is already Approved. Can not approve again");
            return null;
        }
        if (bill.getPaymentApprovalComments() == null || bill.getPaymentApprovalComments().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Noot a Comment. Can not approve payment");
            return null;
        }
        bill.setPaymentApproved(true);
        bill.setPaymentApprovedAt(new Date());
        bill.setPaymentApprovedBy(sessionController.getLoggedUser());
        billFacade.edit(bill);
        return naviateToSupplierBillSettleManagement();
    }

    public String navigateToCompletePayment() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Please select a bill");
            return null;
        }
        return "/dealerPayment/complete_bill_payment?faces-redirect=true";
    }

    public String completePayment() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return null;
        }
        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("Bill Cancelled. Cannot complete payment");
            return null;
        }
        if (!bill.isPaymentApproved()) {
            JsfUtil.addErrorMessage("Bill Payment is not Approved. Cannot complete payment");
            return null;
        }
        if (bill.isPaymentCompleted()) {
            JsfUtil.addErrorMessage("Bill Payment is already Completed. Cannot complete again");
            return null;
        }
        if (bill.getPaymentCompletionComments() == null || bill.getPaymentCompletionComments().trim().isEmpty()) {
            JsfUtil.addErrorMessage("No Comment. Cannot complete payment");
            return null;
        }
        bill.setPaymentCompleted(true);
        bill.setPaymentCompletedAt(new Date());
        bill.setPaymentCompletedBy(sessionController.getLoggedUser());
        billFacade.edit(bill);
        return navigateToCompletePayment();
    }

// Method to retrieve completed payments
    public void fillSupplierBillsCompletedForPayments() {
        supplierPaymentStatus = "Payment Completed";
        fillSupplierPayments(null, true);
    }

// Method to retrieve pending completion (approved but not completed)
    public void fillSupplierBillsPendingCompletionForPayments() {
        supplierPaymentStatus = "Pending Payment Completion";
        boolean approvalIsNeeded = configOptionApplicationController.getBooleanValueByKey("Approval is necessary for Procument Payments", false);
        if (approvalIsNeeded) {
            fillSupplierPayments(true, false);
        } else {
            fillSupplierPayments(null, false);
        }
    }

    public void fillDealorPaymentCanceled() {
        bills = null;
        netTotal = 0.0;
        supplierPaymentStatus = "Canceled";
        String jpql;
        Map params = new HashMap();

        jpql = "select b from Bill b "
                + " where b.retired=false "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.billType = :billTypes "
                + " and b.billTypeAtomic = :bTA ";
        params.put("billTypes", BillType.GrnPayment);
        params.put("bTA", BillTypeAtomic.SUPPLIER_PAYMENT_CANCELLED);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        bills = getBillFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
        Iterator<Bill> iterator = bills.iterator();
        while (iterator.hasNext()) {
            Bill b = iterator.next();
            netTotal += b.getNetTotal();
        }
    }

    @Deprecated
    public void fillPharmacyDue() {
        Date startTime = new Date();
        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        bills = billController.findUnpaidBillsOld(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01);

        BillType[] billTypesArrayReturn = {BillType.PharmacyGrnReturn, BillType.PurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);
        fillIDealorDue(billTypesListBilled, billTypesListReturn);

    }

    public void fillStoreDue() {

        BillType[] billTypesArrayBilled = {BillType.StoreGrnBill, BillType.StorePurchase};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        BillType[] billTypesArrayReturn = {BillType.StoreGrnReturn, BillType.StorePurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);
        fillIDealorDue(billTypesListBilled, billTypesListReturn);

    }

    public void fillPharmacyStoreDue() {

        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill, BillType.StoreGrnBill, BillType.StorePurchase};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        BillType[] billTypesArrayReturn = {BillType.PharmacyGrnReturn, BillType.PurchaseReturn, BillType.StoreGrnReturn, BillType.StorePurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);
        fillIDealorDue(billTypesListBilled, billTypesListReturn);

    }

    private void fillIDealorDue(List<BillType> billTypeBilled, List<BillType> billTypeReturned) {
        List<Institution> list = getCreditBean().getDealorFromBills(getFromDate(), getToDate(), billTypeBilled);

        list.addAll(getCreditBean().getDealorFromReturnBills(getFromDate(), getToDate(), billTypeReturned));

        Set<Institution> setIns = new HashSet<>(list);
        items = new ArrayList<>();
        for (Institution ins : setIns) {
            //     System.err.println("Ins " + ins.getName());
            InstitutionBills newIns = new InstitutionBills();
            newIns.setInstitution(ins);
            List<Bill> lst = getCreditBean().getBills(ins, getFromDate(), getToDate(), billTypeBilled);

            newIns.setBills(lst);

            for (Bill b : lst) {
                double rt = getCreditBean().getGrnReturnValue(b, billTypeReturned);
                b.setTmpReturnTotal(rt);

                double dbl = Math.abs(b.getNetTotal()) - (Math.abs(b.getTmpReturnTotal()) + Math.abs(b.getPaidAmount()));

                if (dbl > 0.1) {
                    b.setTransBoolean(true);
                    newIns.setReturned(newIns.getReturned() + b.getTmpReturnTotal());
                    newIns.setTotal(newIns.getTotal() + b.getNetTotal());
                    newIns.setPaidTotal(newIns.getPaidTotal() + b.getPaidAmount());

                }
            }

            double finalValue = (newIns.getPaidTotal() + newIns.getTotal() + newIns.getReturned());
            if (finalValue != 0 && finalValue < 0.1) {
                items.add(newIns);
            }
        }
    }

    public void settleBill() {
        if (errorCheck()) {
            return;
        }

        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        getCurrent().setTotal(getCurrent().getNetTotal());

        saveBill(BillType.GrnPaymentPre);
        saveBillItem();

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);

        if (getCurrent().getPaymentMethod() == PaymentMethod.Cheque) {

        }

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public String navigateToStartSupplierPayment(Bill originalBill) {
        if (originalBill == null) {
            JsfUtil.addErrorMessage("No Bill Is Selected");
            return null;
        }
        prepareForNewSupplierPayment();
        current.setFromInstitution(sessionController.getInstitution());
        current.setFromDepartment(sessionController.getDepartment());
        current.setToInstitution(originalBill.getFromInstitution());
        if (originalBill.getBillTypeAtomic() == BillTypeAtomic.SUPPLIER_PAYMENT_CANCELLED || originalBill.getBillTypeAtomic() == BillTypeAtomic.SUPPLIER_PAYMENT_RETURNED) {
            current.setReferenceBill(originalBill);
            originalBill.setReferenceBill(current);
            current.setReactivated(true);
        }
        currentBillItem = new BillItem();
        currentBillItem.setReferenceBill(originalBill);
        double settlingValue = Math.abs(originalBill.getNetTotal()) - (Math.abs(originalBill.getRefundAmount()) + Math.abs(originalBill.getPaidAmount()));
        currentBillItem.setNetValue(-settlingValue);
        currentBillItem.setGrossValue(-settlingValue);
        getSelectedBillItems().add(currentBillItem);
//        current.getBillItems().add(currentBillItem);
        calculateTotalBySelectedBillItems();
        return "/dealerPayment/settle_supplier_payment?faces-redirect=true";
    }

    public String navigateToViewSupplierPayment(Bill originalBill) {
        if (originalBill == null) {
            JsfUtil.addErrorMessage("No Bill Is Selected");
            return null;
        }
        current = billService.reloadBill(originalBill);
        return "/dealerPayment/view_supplier_payment?faces-redirect=true";
    }

    public String navigateToViewProcurementBill(Bill originalBill) {
        if (originalBill == null) {
            JsfUtil.addErrorMessage("No Bill Is Selected");
            return null;
        }
        current = billService.reloadBill(originalBill);
        if (current.isCancelled()) {
            currentCancellationBill = billService.reloadBill(current.getCancelledBill());
        } else {
            currentCancellationBill = null;
        }
        if (current.isRefunded()) {
            currentReturnBills = billService.fetchReturnBills(current);
            if (currentReturnBills == null || currentReturnBills.isEmpty()) {
            }
        } else {
            currentReturnBills = billService.fetchReturnBills(current);
            if (currentReturnBills != null && !currentReturnBills.isEmpty()) {
            }
        }
        currentSummeryBillItems = createSummeryBillItems(current, currentReturnBills);

        currentPaymentBillItems = billService.fetchPaymentBillItems(current);
        currentSummaryPurchaseTotalValue = current.getTotal();
        currentSummaryPurchaseReturnTotalValue = calculateTotalGrossTotalValue(currentReturnBills);
        currentSummaryPurchaseNetTotalValue = Math.abs(currentSummaryPurchaseTotalValue) - Math.abs(currentSummaryPurchaseReturnTotalValue);
        currentTotalPaymentSettledValue = calculateTotalValue(currentPaymentBillItems);

        currentTotalPaymentToSettleValue = Math.abs(currentSummaryPurchaseNetTotalValue) - Math.abs(currentTotalPaymentSettledValue);

        return "/dealerPayment/view_purchase_bill?faces-redirect=true";
    }

    private List<BillItem> createSummeryBillItems(Bill originalBill, List<Bill> returnBills) {
        List<BillItem> newlyCreatedSummeryBillItems = new ArrayList<>();
        if (originalBill == null || originalBill.getBillItems() == null) {
            return newlyCreatedSummeryBillItems;
        }

        // Map to store the total purchased quantities grouped by batch
        Map<ItemBatch, BillItem> batchMap = new HashMap<>();

        // Add original bill items to the map based on ItemBatch
        for (BillItem originalBillItem : originalBill.getBillItems()) {
            PharmaceuticalBillItem pbi = originalBillItem.getPharmaceuticalBillItem();
            if (pbi == null || pbi.getItemBatch() == null) {
                continue;
            }

            ItemBatch batch = pbi.getItemBatch();
            Item item = pbi.getItemBatch().getItem();

            BillItem summaryItem = batchMap.get(batch);
            if (summaryItem == null) {
                summaryItem = new BillItem();
                summaryItem.setItem(item);
                summaryItem.setPharmaceuticalBillItem(new PharmaceuticalBillItem());

                // Copy batch-specific details
                summaryItem.getPharmaceuticalBillItem().setItemBatch(batch);
                summaryItem.getPharmaceuticalBillItem().setPurchaseRate(pbi.getPurchaseRate());
                summaryItem.getPharmaceuticalBillItem().setRetailRate(pbi.getRetailRate());
                summaryItem.getPharmaceuticalBillItem().setStringValue(pbi.getStringValue()); // Batch details
                summaryItem.getPharmaceuticalBillItem().setDoe(pbi.getDoe());

                summaryItem.getPharmaceuticalBillItem().setQty(0.0);
                summaryItem.getPharmaceuticalBillItem().setFreeQty(0.0);

                batchMap.put(batch, summaryItem);
            }

            summaryItem.getPharmaceuticalBillItem().setQty(
                    summaryItem.getPharmaceuticalBillItem().getQty() + pbi.getQty()
            );
            summaryItem.getPharmaceuticalBillItem().setFreeQty(
                    summaryItem.getPharmaceuticalBillItem().getFreeQty() + pbi.getFreeQty()
            );
        }

        // Deduct return bill items from the map based on ItemBatch
        if (returnBills != null) {
            for (Bill returnBill : returnBills) {
                if (returnBill.getBillItems() == null) {
                    continue;
                }
                for (BillItem returningBillItem : returnBill.getBillItems()) {
                    PharmaceuticalBillItem retPbi = returningBillItem.getPharmaceuticalBillItem();
                    if (retPbi == null || retPbi.getItemBatch() == null) {
                        continue;
                    }

                    ItemBatch batch = retPbi.getItemBatch();
                    if (!batchMap.containsKey(batch)) {
                        continue;
                    }

                    BillItem summaryItem = batchMap.get(batch);
                    summaryItem.getPharmaceuticalBillItem().setQty(
                            Math.abs(summaryItem.getPharmaceuticalBillItem().getQty()) - Math.abs(retPbi.getQty())
                    );
                    summaryItem.getPharmaceuticalBillItem().setFreeQty(
                            Math.abs(summaryItem.getPharmaceuticalBillItem().getFreeQty()) - Math.abs(retPbi.getFreeQty())
                    );

                }
            }
        }

        // Add all processed batch-based items to the final list
        newlyCreatedSummeryBillItems.addAll(batchMap.values());
        return newlyCreatedSummeryBillItems;
    }

    private double calculateTotalValue(List<BillItem> billItems) {
        if (billItems == null || billItems.isEmpty()) {
            return 0.0;
        }

        double totalPurchaseValue = 0.0;
        for (BillItem billItem : billItems) {
            totalPurchaseValue += billItem.getNetValue();
        }
        return totalPurchaseValue;
    }

    private double calculateTotalNetTotalValue(List<Bill> inputBills) {
        if (inputBills == null || inputBills.isEmpty()) {
            return 0.0;
        }
        double totalPaidValue = 0.0;
        for (Bill inputBill : inputBills) {
            totalPaidValue += inputBill.getNetTotal();
        }
        return totalPaidValue;
    }

    private double calculateTotalGrossTotalValue(List<Bill> inputBills) {
        if (inputBills == null || inputBills.isEmpty()) {
            return 0.0;
        }
        double totalPaidValue = 0.0;
        for (Bill inputBill : inputBills) {
            totalPaidValue += inputBill.getTotal();
        }
        return totalPaidValue;
    }

    private void loadProcurementBillDetails() {
        if (current == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }
        if (current.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No Bill Type");
            return;
        }

        BillTypeAtomic[] billTypes = {
            BillTypeAtomic.PHARMACY_GRN,
            BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL,
            BillTypeAtomic.PHARMACY_DIRECT_PURCHASE,
            BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL,
            BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL,
            BillTypeAtomic.STORE_GRN,
            BillTypeAtomic.STORE_DIRECT_PURCHASE
        };

        if (!Arrays.asList(billTypes).contains(current.getBillTypeAtomic())) {
            JsfUtil.addErrorMessage("Wrong Bill Type");
            return;
        }
        current = billService.reloadBill(current);
        currentCancellationBill = current.getCancelledBill();
        currentReturnBills = billService.fetchReturnBills(current);

    }

    public String navigateToPrepareSupplierPayment(Bill originalBill) {
        if (originalBill == null) {
            JsfUtil.addErrorMessage("No Bill Is Selected");
            return null;
        }
        prepareForNewSupplierPaymentGeneration();
        current.setFromInstitution(sessionController.getInstitution());
        current.setFromDepartment(sessionController.getDepartment());
        current.setToInstitution(originalBill.getFromInstitution());
        if (originalBill.getBillTypeAtomic() == BillTypeAtomic.SUPPLIER_PAYMENT_CANCELLED || originalBill.getBillTypeAtomic() == BillTypeAtomic.SUPPLIER_PAYMENT_RETURNED) {
            current.setReferenceBill(originalBill);
            originalBill.setReferenceBill(current);
            current.setReactivated(true);
        }
        currentBillItem = new BillItem();
        currentBillItem.setSearialNo(1);
        currentBillItem.setReferenceBill(originalBill);
        double settlingValue = Math.abs(originalBill.getNetTotal()) - (Math.abs(originalBill.getRefundAmount()) + Math.abs(originalBill.getPaidAmount()));
        currentBillItem.setNetValue(-settlingValue);
        currentBillItem.setGrossValue(-settlingValue);
        getSelectedBillItems().add(currentBillItem);
        calculateTotalBySelectedBillItems();
        return "/dealerPayment/generate_supplier_payment?faces-redirect=true";
    }

    public String navigateToApproveSupplierPayment(Bill approvalBill) {
        makeNull();
        if (approvalBill == null) {
            JsfUtil.addErrorMessage("No Bill Is Selected");
            return null;
        }
        current = billService.reloadBill(approvalBill);
        return "/dealerPayment/approve_supplier_payment?faces-redirect=true";
    }

    public String navigateToCheckSupplierPayment(Bill approvalBill) {
        makeNull();
        if (approvalBill == null) {
            JsfUtil.addErrorMessage("No Bill Is Selected");
            return null;
        }
        current = billService.reloadBill(approvalBill);
        return "/dealerPayment/check_supplier_payment?faces-redirect=true";
    }

    public String navigateToSettleSupplierPayment(Bill approvalBill) {
        makeNull();
        if (approvalBill == null) {
            JsfUtil.addErrorMessage("No Bill Is Selected");
            return null;
        }
        current = billService.reloadBill(approvalBill);
        return "/dealerPayment/settle_approved_supplier_payment?faces-redirect=true";
    }

    public String navigateToCompleteSupplierPayment(Bill approvedAndSettledPaymentBill) {
        makeNull();
        if (approvedAndSettledPaymentBill == null) {
            JsfUtil.addErrorMessage("No Bill Is Selected");
            return null;
        }
        current = billService.reloadBill(approvedAndSettledPaymentBill);
        currentPayment = billService.fetchBillPayment(current.getReferenceBill());

        if (!current.isPaymentApproved()) {
            JsfUtil.addErrorMessage("Not Approved. Can not complete.");
            return null;
        }
        if (current.isPaymentCompleted()) {
            JsfUtil.addErrorMessage("Already Completed. Can not complete again.");
            return null;
        }

        return "/dealerPayment/complete_approved_and_settled_supplier_payment?faces-redirect=true";
    }

    public String findAndnavigateToViewSupplierPaymentVoucher(Bill b) {
        String jpql = "Select bi from BillItem bi where "
                + " bi.bill.billTypeAtomic =:bta "
                + " and bi.referenceBill =:bill "
                + " and bi.retired = false";
        HashMap hm = new HashMap();
        hm.put("bta", BillTypeAtomic.SUPPLIER_PAYMENT_PREPERATION);
        hm.put("bill", b);
        BillItem supBillItem = getBillItemFacade().findFirstByJpql(jpql, hm);
        Bill supplierPaymentBill = supBillItem.getBill();

        if (supplierPaymentBill == null) {
            JsfUtil.addErrorMessage("No Bill Is Selected");
            return null;
        }
        if (supplierPaymentBill.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No Bill Type");
            return null;
        }
        if (supplierPaymentBill.getBillTypeAtomic() == BillTypeAtomic.SUPPLIER_PAYMENT_PREPERATION) {
            if (supplierPaymentBill.getReferenceBill() != null) {
                current = supplierPaymentBill.getReferenceBill();
            } else if (supplierPaymentBill.getBackwardReferenceBill() != null) {
                current = supplierPaymentBill.getBackwardReferenceBill();
            } else {
                JsfUtil.addErrorMessage("Not a supplier bill");
                return null;
            }
        } else {
            current = supplierPaymentBill;
        }
        if (current.getBillTypeAtomic() != BillTypeAtomic.SUPPLIER_PAYMENT) {
            JsfUtil.addErrorMessage("Not a supplier bill");
            return null;
        }
        current = billService.reloadBill(supplierPaymentBill);
        if (!current.isPaymentApproved()) {
            JsfUtil.addErrorMessage("Not Approved. Can not complete.");
            return null;
        }

        return "/dealerPayment/view_supplier_payment_voucher?faces-redirect=true";
    }

    public String navigateToViewSupplierPaymentVoucher(Bill supplierPaymentBill) {
        if (supplierPaymentBill == null) {
            JsfUtil.addErrorMessage("No Bill Is Selected");
            return null;
        }
        if (supplierPaymentBill.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No Bill Type");
            return null;
        }
        if (supplierPaymentBill.getBillTypeAtomic() == BillTypeAtomic.SUPPLIER_PAYMENT_PREPERATION) {
            if (supplierPaymentBill.getReferenceBill() != null) {
                current = supplierPaymentBill.getReferenceBill();
            } else if (supplierPaymentBill.getBackwardReferenceBill() != null) {
                current = supplierPaymentBill.getBackwardReferenceBill();
            } else {
                JsfUtil.addErrorMessage("Not a supplier bill");
                return null;
            }
        } else {
            current = supplierPaymentBill;
        }
        if (current.getBillTypeAtomic() != BillTypeAtomic.SUPPLIER_PAYMENT) {
            JsfUtil.addErrorMessage("Not a supplier bill");
            return null;
        }
        current = billService.reloadBill(supplierPaymentBill);
        if (!current.isPaymentApproved()) {
            JsfUtil.addErrorMessage("Not Approved. Can not complete.");
            return null;
        }
        if (current.isPaymentCompleted()) {
            JsfUtil.addErrorMessage("Already Completed. Can not complete again.");
            return null;
        }

        return "/dealerPayment/view_supplier_payment_voucher?faces-redirect=true";
    }

    public String navigateToStartSupplierPaymentOfSelectedBills() {
        prepareForNewSupplierPayment();
        if (getSelectedBills().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill is selected to settle");
            return null;
        }
        current.setFromInstitution(sessionController.getInstitution());
        current.setFromDepartment(sessionController.getDepartment());
        Institution paymentSupplier = null;
        for (Bill b : getSelectedBills()) {
            if (paymentSupplier == null) {
                paymentSupplier = b.getFromInstitution();
            }
            if (!paymentSupplier.equals(b.getFromInstitution())) {
                JsfUtil.addErrorMessage("Bills from multiple suppliers are selected. Please check selection.");
                return null;
            }
        }
        for (Bill b : getSelectedBills()) {
            currentBillItem = new BillItem();
            currentBillItem.setReferenceBill(b);
            double settlingValue = Math.abs(b.getNetTotal()) - (Math.abs(b.getRefundAmount()) + Math.abs(b.getPaidAmount()));
            currentBillItem.setNetValue(-settlingValue);
            currentBillItem.setGrossValue(-settlingValue);
            getSelectedBillItems().add(currentBillItem);
        }
        current.setToInstitution(paymentSupplier);
        calTotalBySelectedBillTems();
        return "/dealerPayment/pay_supplier?faces-redirect=true";
    }

    public String navigateToPrepareSupplierPaymentForAllSelectedBills() {
        if (selectedBills == null) {
            JsfUtil.addErrorMessage("No Bills are Selected");
            return null;
        }
        if (selectedBills.isEmpty()) {
            JsfUtil.addErrorMessage("No Bills are Selected");
            return null;
        }
        Institution payingSupplier = null;
        for (Bill b : getSelectedBills()) {
            if (b.getFromInstitution() == null) {
                JsfUtil.addErrorMessage("One purchase or GRN bill does not have a Supplier. Can not proceed.");
                return null;
            }
            if (payingSupplier == null) {
                payingSupplier = b.getFromInstitution();
            } else {
                if (!payingSupplier.equals(b.getFromInstitution())) {
                    JsfUtil.addErrorMessage("Can not settle purchase or GRN bills from more than one supplier at once.");
                    return null;
                }
            }

        }
        prepareForNewSupplierPaymentGeneration();
        current.setFromInstitution(sessionController.getInstitution());
        current.setFromDepartment(sessionController.getDepartment());
        current.setToInstitution(payingSupplier);

        for (Bill billsPaymentsWillBeCreated : selectedBills) {
            currentBillItem = new BillItem();
            currentBillItem.setSearialNo(1);
            currentBillItem.setReferenceBill(billsPaymentsWillBeCreated);
            double settlingValue = Math.abs(billsPaymentsWillBeCreated.getNetTotal()) - (Math.abs(billsPaymentsWillBeCreated.getRefundAmount()) + Math.abs(billsPaymentsWillBeCreated.getPaidAmount()));
            currentBillItem.setNetValue(-settlingValue);
            currentBillItem.setGrossValue(-settlingValue);
            getSelectedBillItems().add(currentBillItem);
        }
        calculateTotalBySelectedBillItems();
        return "/dealerPayment/generate_supplier_payment?faces-redirect=true";
    }

    public void settleSupplierPaymentForApprovedPayment() {
        if (errorCheckForSettlingPaymentForApprovedPayment()) {
            return;
        }

        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.SUPPLIER_PAYMENT);

        getCurrent().setInsId(deptId);
        getCurrent().setDeptId(deptId);

        getCurrent().setBillType(BillType.GrnPaymentPre);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.SUPPLIER_PAYMENT);

        getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrent().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());

        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

//        getCurrent().setNetTotal(getCurrent().getNetTotal());
        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        } else {
            getBillFacade().edit(getCurrent());
        }

        Payment p = createPayment(getCurrent(), getCurrent().getPaymentMethod());

        updateReferanceBillBalances(getCurrent().getBillItems());

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public void settleSupplierPayment() {
        if (errorCheckForAllSelectedItemsSettlingBill()) {
            return;
        }
        calculateTotalBySelectedBillItems();
        updateReferanceBillBalances(selectedBillItems);

        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.SUPPLIER_PAYMENT);

        getCurrent().setInsId(deptId);
        getCurrent().setDeptId(deptId);

        getCurrent().setBillType(BillType.GrnPaymentPre);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.SUPPLIER_PAYMENT);

        getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrent().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());

        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

//        getCurrent().setNetTotal(getCurrent().getNetTotal());
        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        } else {
            getBillFacade().edit(getCurrent());
        }

        for (BillItem savingBillItem : selectedBillItems) {
            savingBillItem.setBill(current);
            if (savingBillItem.getId() == null) {
                savingBillItem.setCreatedAt(new Date());
                savingBillItem.setCreater(sessionController.getLoggedUser());
                billItemFacade.create(savingBillItem);
            } else {
                billItemFacade.edit(savingBillItem);
            }
        }

        List<Payment> ps = paymentService.createPayment(current, paymentMethodData);
//        saveBillItemBySelectedItems(p);

        current = billService.reloadBill(current);

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public void settleApprovedSupplierPayment() {
        if (errorCheckForSettlingApprovedPayments()) {
            return;
        }
        current = billService.reloadBill(current);
        Bill newlyCreatedSupplierPaymentBill = new Bill();
        newlyCreatedSupplierPaymentBill.copy(current);
        newlyCreatedSupplierPaymentBill.copyValue(current);
        newlyCreatedSupplierPaymentBill.setReferenceBill(current);

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.SUPPLIER_PAYMENT);

        newlyCreatedSupplierPaymentBill.setInsId(deptId);
        newlyCreatedSupplierPaymentBill.setDeptId(deptId);

        newlyCreatedSupplierPaymentBill.setBillType(BillType.GrnPaymentPre);
        newlyCreatedSupplierPaymentBill.setBillTypeAtomic(BillTypeAtomic.SUPPLIER_PAYMENT);

        newlyCreatedSupplierPaymentBill.setDepartment(getSessionController().getLoggedUser().getDepartment());
        newlyCreatedSupplierPaymentBill.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        newlyCreatedSupplierPaymentBill.setBillDate(new Date());
        newlyCreatedSupplierPaymentBill.setBillTime(new Date());

        newlyCreatedSupplierPaymentBill.setCreatedAt(new Date());
        newlyCreatedSupplierPaymentBill.setCreater(getSessionController().getLoggedUser());

//        getCurrent().setNetTotal(getCurrent().getNetTotal());
        if (newlyCreatedSupplierPaymentBill.getId() == null) {
            getBillFacade().create(newlyCreatedSupplierPaymentBill);
        } else {
            getBillFacade().edit(newlyCreatedSupplierPaymentBill);
        }

        getCurrent().setBackwardReferenceBill(newlyCreatedSupplierPaymentBill);
        getCurrent().setPaymentGenerated(true);
        getCurrent().setPaymentGeneratedAt(new Date());
        getCurrent().setPaymentGeneratedBy(sessionController.getLoggedUser());
        getBillFacade().edit(getCurrent());

        for (BillItem originalBillItem : current.getBillItems()) {
            BillItem newlyCreateBillItem = new BillItem();
            newlyCreateBillItem.copy(originalBillItem);
            newlyCreateBillItem.setBill(newlyCreatedSupplierPaymentBill);
            newlyCreateBillItem.setReferanceBillItem(originalBillItem);
            if (newlyCreateBillItem.getId() == null) {
                newlyCreateBillItem.setCreatedAt(new Date());
                newlyCreateBillItem.setCreater(sessionController.getLoggedUser());
                billItemFacade.create(newlyCreateBillItem);
            } else {
                billItemFacade.edit(newlyCreateBillItem);
            }
        }

        current.setReferenceBill(newlyCreatedSupplierPaymentBill);
        current.setBackwardReferenceBill(newlyCreatedSupplierPaymentBill);
        billFacade.edit(current);

        List<Payment> ps = paymentService.createPayment(newlyCreatedSupplierPaymentBill, paymentMethodData);

//        saveBillItemBySelectedItems(p);
        newlyCreatedSupplierPaymentBill = billService.reloadBill(newlyCreatedSupplierPaymentBill);
        updateReferanceBillBalances(newlyCreatedSupplierPaymentBill.getBillItems());

        current = newlyCreatedSupplierPaymentBill;

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public void completeApprovedAndSettledSupplierPayment() {
        if (errorCheckForCompletingApprovedAndSettledPayment()) {
            return;
        }
        current = billService.reloadBill(current);

        getCurrent().setPaymentCompleted(true);
        getCurrent().setPaymentCompletedAt(new Date());
        getCurrent().setPaymentCompletedBy(sessionController.getLoggedUser());
        getBillFacade().edit(getCurrent());

        updateReferanceBillCompletionStatus(current.getBillItems());

        JsfUtil.addSuccessMessage("Bill Payment Completed");
        printPreview = true;

    }

    public void saveSupplierPayment(Bill b) {
        if (b == null) {
            return;
        }
        getBillFacade().edit(b);

        for (BillItem bi : b.getBillItems()) {
            billItemFacade.edit(bi);
        }
        changed = false;
        JsfUtil.addSuccessMessage("Bill Saved Successfully");
    }

    public void settleCheckingSupplierPayment() {
        if (errorCheckForCheckingSupplierPayment()) {
            return;
        }
        getCurrent().setCompleted(true);
        getCurrent().setCompletedAt(new Date());
        getCurrent().setCompletedBy(sessionController.getLoggedUser());

        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        } else {
            getBillFacade().edit(getCurrent());
        }
//        updateReferanceBillAsPaymentApproved(getCurrent().getBillItems());

        for (BillItem bi : getCurrent().getBillItems()) {
            bi.setBill(current);
            if (bi.getId() == null) {
                bi.setCreatedAt(new Date());
                bi.setCreater(sessionController.getLoggedUser());
                billItemFacade.create(bi);
            } else {
                billItemFacade.edit(bi);
            }
        }

        JsfUtil.addSuccessMessage("Payment Checking Done");
        printPreview = true;

    }

    public void settleApproveSupplierPayment() {
        if (errorCheckForApprovingSupplierPayment()) {
            return;
        }
        getCurrent().setPaymentApproved(true);
        getCurrent().setPaymentApprovedAt(new Date());
        getCurrent().setPaymentApprovedBy(sessionController.getLoggedUser());

        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        } else {
            getBillFacade().edit(getCurrent());
        }
        updateReferanceBillAsPaymentApproved(getCurrent().getBillItems());

        for (BillItem bi : getCurrent().getBillItems()) {
            bi.setBill(current);
            if (bi.getId() == null) {
                bi.setCreatedAt(new Date());
                bi.setCreater(sessionController.getLoggedUser());
                billItemFacade.create(bi);
            } else {
                billItemFacade.edit(bi);
            }
        }

        JsfUtil.addSuccessMessage("Payment Approved");
        printPreview = true;

    }

    public void settleGenerateSupplierPayment() {
        if (errorCheckForPaymentPreperationBill()) {
            return;
        }
        calculateTotalBySelectedBillItems();

        boolean supplierPaymentBillNumbersShouldIncludeThePaymentMethod = configOptionApplicationController.getBooleanValueByKey("Supplier Payment Bill Numbers Should Include The Payment Method", true);

        String deptId;
        if (supplierPaymentBillNumbersShouldIncludeThePaymentMethod) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.SUPPLIER_PAYMENT_PREPERATION, getCurrent().getPaymentMethod());
        } else {
            deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.SUPPLIER_PAYMENT_PREPERATION);
        }
        getCurrent().setInsId(deptId);
        getCurrent().setDeptId(deptId);

        getCurrent().setBillType(BillType.GrnPaymentPreparation);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.SUPPLIER_PAYMENT_PREPERATION);

        getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrent().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());

        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

//        getCurrent().setPaymentGenerated(true);
//        getCurrent().setPaymentGeneratedAt(new Date());
//        getCurrent().setPaymentGeneratedBy(sessionController.getLoggedUser());
        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        } else {
            getBillFacade().edit(getCurrent());
        }
        for (BillItem bi : selectedBillItems) {
            bi.setBill(getCurrent());
            if (bi.getId() == null) {
                bi.setCreatedAt(new Date());
                bi.setCreater(sessionController.getLoggedUser());
                billItemFacade.create(bi);
            } else {
                billItemFacade.edit(bi);
            }
        }

        updateReferanceBillAsPaymentGenerated(selectedBillItems);
        current = billService.reloadBill(current);
        JsfUtil.addSuccessMessage("Payment Generated");
        printPreview = true;

    }

    public void settlePaymentsOfSelectedSupplierBills() {
        if (errorCheckForSettlingSelectedSupplierBills()) {
            return;
        }
        calTotalBySelectedBillTems();
        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());
        saveBill(BillType.GrnPayment);
//        Payment p = createPayment(getCurrent(), getCurrent().getPaymentMethod());
        List<Payment> payments = paymentService.createPayment(getCurrent(), paymentMethodData);
        updateReferanceBillBalances(selectedBillItems);
        saveBillItemBySelectedItems();
        current = billService.reloadBill(current);
        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    private void saveBillItem() {
        for (BillItem tmp : getBillItems()) {
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setBill(getCurrent());
            tmp.setNetValue(0 - tmp.getNetValue());
            if (tmp.getId() == null) {
                getBillItemFacade().create(tmp);
            }
            updateReferenceBill(tmp);
        }
    }

    private void saveBillItemBySelectedItems() {
        for (BillItem tmp : getSelectedBillItems()) {
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setBill(getCurrent());
            tmp.setNetValue(0 - Math.abs(tmp.getNetValue()));
            if (tmp.getId() == null) {
                getBillItemFacade().create(tmp);
            } else {
                getBillItemFacade().edit(tmp);
            }
        }
    }

//    private void saveBillItemBySelectedItems(Payment p) {
//        System.out.println("saveBillItemBySelectedItems");
//        for (BillItem tmp : getSelectedBillItems()) {
//            System.out.println("tmp = " + tmp);
//            tmp.setCreatedAt(new Date());
//            tmp.setCreater(getSessionController().getLoggedUser());
//            tmp.setBill(getCurrent());
//            tmp.setNetValue(0 - tmp.getNetValue());
//            if (tmp.getId() == membershipnull) {
//                getBillItemFacade().create(tmp);
//            }
//            saveBillFee(tmp, p);
//            updateReferenceBill(tmp);
//        }
//    }
    private boolean isPaidAmountOk(BillItem tmp) {

        double refBallance = getReferenceBallance(tmp);
        double netValue = Math.abs(tmp.getNetValue());

        //   ballance=refBallance-tmp.getNetValue();
        if (refBallance >= netValue) {
            return true;
        }

        if (netValue - refBallance < 0.1) {
            return true;
        }

        return false;
    }

    private void updateReferenceBill(BillItem tmp) {
        double dbl = getCreditBean().getPaidAmount(tmp.getReferenceBill(), BillType.GrnPaymentPre);
        dbl = Math.abs(dbl);
        tmp.getReferenceBill().setPaidAmount(0 - dbl);
        getBillFacade().edit(tmp.getReferenceBill());
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
        createBillFeePaymentAndPayment(bf, p);
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
        p.setFromInstitution(p.getBill().getInstitution());
        p.setToInstitution(p.getBill().getToInstitution());
        if (p.getId() == null) {
            getPaymentFacade().create(p);
        }
    }

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

    private void setValues(Institution inst, String1Value5 dataTable5Value, List<BillType> billTypesBilled, List<BillType> billTypesReturned) {

        List<Bill> lst = getCreditBean().getBills(inst, billTypesBilled);
        for (Bill b : lst) {
            double rt = getCreditBean().getGrnReturnValue(b, billTypesReturned);

            //   double dbl = Math.abs(b.getNetTotal()) - (Math.abs(b.getTmpReturnTotal()) + Math.abs(b.getPaidAmount()));
            b.setTmpReturnTotal(rt);

            Long dayCount = CommonFunctions.getDayCountTillNow(b.getInvoiceDate());

            double finalValue = (b.getNetTotal() + b.getPaidAmount() + b.getTmpReturnTotal());

            if (dayCount < 30) {
                dataTable5Value.setValue1(dataTable5Value.getValue1() + finalValue);
            } else if (dayCount < 60) {
                dataTable5Value.setValue2(dataTable5Value.getValue2() + finalValue);
            } else if (dayCount < 90) {
                dataTable5Value.setValue3(dataTable5Value.getValue3() + finalValue);
            } else {
                dataTable5Value.setValue4(dataTable5Value.getValue4() + finalValue);
            }

        }

    }

    public void fillPharmacyDue1() {
        Date startTime = new Date();
        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        bills = billController.findUnpaidBillsOld(fromDate, toDate, billTypesListBilled, null, null);
    }

    public void fillPharmacyDue2() {
        Date startTime = new Date();
        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        bills = billController.findUnpaidBillsOld(fromDate, toDate, null, PaymentMethod.Credit, null);
    }

    public void fillPharmacyDue3() {
        Date startTime = new Date();
        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        bills = billController.findUnpaidBillsOld(fromDate, toDate, null, null, null);
    }

    public void fillStoreDueAge() {
        Date startTime = new Date();

        BillType[] billTypesArrayBilled = {BillType.StoreGrnBill, BillType.StorePurchase};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        BillType[] billTypesArrayReturn = {BillType.StoreGrnReturn, BillType.StorePurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);

        createAgeTable(billTypesListBilled, billTypesListReturn);

    }

    public void fillPharmacyDueAge() {
        Date startTime = new Date();

        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        BillType[] billTypesArrayReturn = {BillType.PharmacyGrnReturn, BillType.PurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);

        createAgeTable(billTypesListBilled, billTypesListReturn);

    }

    public void fillPharmacyStoreDueAge() {
        Date startTime = new Date();

        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill, BillType.StoreGrnBill, BillType.StorePurchase};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        BillType[] billTypesArrayReturn = {BillType.PharmacyGrnReturn, BillType.PurchaseReturn, BillType.StoreGrnReturn, BillType.StorePurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);
        createAgeTable(billTypesListBilled, billTypesListReturn);

    }

    private void createAgeTable(List<BillType> billTypesBilled, List<BillType> billTypesReturned) {
        makeNull();
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getDealorFromBills(billTypesBilled);
        list.addAll(getCreditBean().getDealorFromReturnBills(billTypesReturned));

        setIns.addAll(list);

        dealorCreditAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setString(ins.getName());
            setValues(ins, newRow, billTypesBilled, billTypesReturned);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                dealorCreditAge.add(newRow);
            }
        }

    }

    public List<Bill> getItems2() {
        String sql;
        HashMap hm;

        sql = "Select b From Bill b where b.retired=false and b.createdAt "
                + "  between :frm and :to and b.creditCompany=:cc "
                + " and b.paymentMethod= :pm and b.billType=:tp";
        hm = new HashMap();
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("cc", getInstitution());
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tp", BillType.OpdBill);
        return getBillFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double getCreditTotal() {
        String sql;
        HashMap hm;

        sql = "Select sum(b.netTotal) From Bill b where b.retired=false and b.createdAt "
                + "  between :frm and :to and b.creditCompany=:cc "
                + " and b.paymentMethod= :pm and b.billType=:tp";
        hm = new HashMap();
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("cc", getInstitution());
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tp", BillType.OpdBill);
        return getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    /**
     * Creates a new instance of pharmacyDealorBill
     */
    public SupplierPaymentController() {
        this.supplierPaymentStatusList = Arrays.asList("Pending", "Approved", "Canceled", "Any");
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public Bill getCurrent() {
        if (current == null) {
            current = new BilledBill();
        }
        return current;
    }

    public void setCurrent(Bill current) {
        this.current = current;
    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
        }
        return currentBillItem;
    }

    public String fillFooterDataOfPaymentVoucher(String s, Bill b) {
        if (configOptionApplicationController.getBooleanValueByKey("Supplier Payment - Fill Footer Data", false)) {
            Payment p;

            if (b.getBillTypeAtomic() == BillTypeAtomic.SUPPLIER_PAYMENT) {
                p = findPaymentFromBill(b);
            } else {
                p = findPaymentFromBill(b.getReferenceBill());
            }

            if (p.getPaymentMethod() != PaymentMethod.Cheque) {
                s = "";
                return s;
            }

            String filledFooter;

            String bankName = (p != null ? p.getBank().getName() : "");
            String chequeDate = (p != null ? CommonFunctions.getDateFormat(p.getChequeDate(), sessionController.getApplicationPreference().getLongDateFormat()) : "");
            String chequeNo = (p != null ? p.getChequeRefNo() : "");
            Double amount = (p != null ? Math.abs(p.getPaidValue()) : 0.0);

            DecimalFormat formatter = new DecimalFormat("#,###,##0.00");
            String formattedAmount = formatter.format(amount);

            filledFooter = s.replace("{{bank_name}}", bankName)
                    .replace("{{cheque_date}}", chequeDate)
                    .replace("{{cheque_no}}", chequeNo)
                    .replace("{{amount}}", formattedAmount);

            return filledFooter;
        } else {
            return s;
        }
    }

    public String convertToWord(Double d) {
        return d == null ? "" : CommonFunctions.convertToWord(d);
    }

    public Payment findPaymentFromBill(Bill b) {
        String jpql = "Select p From Payment p Where "
                + "p.bill = :bill and "
                + "p.retired = false";
        HashMap hm = new HashMap();
        hm.put("bill", b);

        return getPaymentFacade().findFirstByJpql(jpql, hm);
    }

    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
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

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
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

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public BillController getBillController() {
        return billController;
    }

    public void setBillController(BillController billController) {
        this.billController = billController;
    }

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public List<BillItem> getSelectedBillItems() {
        if (selectedBillItems == null) {
            selectedBillItems = new ArrayList<>();
        }
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }

    public CreditBean getCreditBean() {
        return creditBean;
    }

    public void setCreditBean(CreditBean creditBean) {
        this.creditBean = creditBean;
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

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth(new Date());
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

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(Double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public List<InstitutionBills> getItems() {
        return items;
    }

    public void setItems(List<InstitutionBills> items) {
        this.items = items;
    }

    public List<String1Value5> getDealorCreditAge() {
        return dealorCreditAge;
    }

    public void setDealorCreditAge(List<String1Value5> dealorCreditAge) {
        this.dealorCreditAge = dealorCreditAge;
    }

    public List<String1Value5> getFilteredList() {
        return filteredList;
    }

    public void setFilteredList(List<String1Value5> filteredList) {
        this.filteredList = filteredList;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
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

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getChequeFromDate() {
        if (chequeFromDate == null) {
            chequeFromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return chequeFromDate;
    }

    public void setChequeFromDate(Date chequeFromDate) {
        this.chequeFromDate = chequeFromDate;
    }

    public Date getChequeToDate() {
        if (chequeToDate == null) {
            chequeToDate = CommonFunctions.getEndOfDay(new Date());
        }
        return chequeToDate;
    }

    public void setChequeToDate(Date chequeToDate) {
        this.chequeToDate = chequeToDate;
    }

    public String getChequeNo() {
        return chequeNo;
    }

    public void setChequeNo(String chequeNo) {
        this.chequeNo = chequeNo;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Institution getBank() {
        return bank;
    }

    public void setBank(Institution bank) {
        this.bank = bank;
    }

    public List<String> getSupplierPaymentStatusList() {
        return supplierPaymentStatusList;
    }

    public void setSupplierPaymentStatusList(List<String> supplierPaymentStatusList) {
        this.supplierPaymentStatusList = supplierPaymentStatusList;
    }

    public String getSupplierPaymentStatus() {
        return supplierPaymentStatus;
    }

    public void setSupplierPaymentStatus(String supplierPaymentStatus) {
        this.supplierPaymentStatus = supplierPaymentStatus;
    }

    public Bill getCurrentCancellationBill() {
        return currentCancellationBill;
    }

    public void setCurrentCancellationBill(Bill currentCancellationBill) {
        this.currentCancellationBill = currentCancellationBill;
    }

    public List<Bill> getCurrentReturnBills() {
        return currentReturnBills;
    }

    public void setCurrentReturnBills(List<Bill> currentReturnBills) {
        this.currentReturnBills = currentReturnBills;
    }

    public List<BillItem> getCurrentPaymentBillItems() {
        return currentPaymentBillItems;
    }

    public void setCurrentPaymentBillItems(List<BillItem> currentPaymentBillItems) {
        this.currentPaymentBillItems = currentPaymentBillItems;
    }

    public List<Bill> getCurrentPaymentRefundBills() {
        return currentPaymentRefundBills;
    }

    public void setCurrentPaymentRefundBills(List<Bill> currentPaymentRefundBills) {
        this.currentPaymentRefundBills = currentPaymentRefundBills;
    }

    public List<BillItem> getCurrentSummeryBillItems() {
        return currentSummeryBillItems;
    }

    public void setCurrentSummeryBillItems(List<BillItem> currentSummeryBillItems) {
        this.currentSummeryBillItems = currentSummeryBillItems;
    }

    public double getCurrentSummaryPurchaseTotalValue() {
        return currentSummaryPurchaseTotalValue;
    }

    public void setCurrentSummaryPurchaseTotalValue(double currentSummaryPurchaseTotalValue) {
        this.currentSummaryPurchaseTotalValue = currentSummaryPurchaseTotalValue;
    }

    public double getCurrentTotalPaymentSettledValue() {
        return currentTotalPaymentSettledValue;
    }

    public void setCurrentTotalPaymentSettledValue(double currentTotalPaymentSettledValue) {
        this.currentTotalPaymentSettledValue = currentTotalPaymentSettledValue;
    }

    public double getCurrentTotalPaymentToSettleValue() {
        return currentTotalPaymentToSettleValue;
    }

    public void setCurrentTotalPaymentToSettleValue(double currentTotalPaymentToSettleValue) {
        this.currentTotalPaymentToSettleValue = currentTotalPaymentToSettleValue;
    }

    public double getCurrentSummaryPurchaseReturnTotalValue() {
        return currentSummaryPurchaseReturnTotalValue;
    }

    public void setCurrentSummaryPurchaseReturnTotalValue(double currentSummaryPurchaseReturnTotalValue) {
        this.currentSummaryPurchaseReturnTotalValue = currentSummaryPurchaseReturnTotalValue;
    }

    public double getCurrentSummaryPurchaseNetTotalValue() {
        return currentSummaryPurchaseNetTotalValue;
    }

    public void setCurrentSummaryPurchaseNetTotalValue(double currentSummaryPurchaseNetTotalValue) {
        this.currentSummaryPurchaseNetTotalValue = currentSummaryPurchaseNetTotalValue;
    }

    public Payment getCurrentPayment() {
        return currentPayment;
    }

    public void setCurrentPayment(Payment currentPayment) {
        this.currentPayment = currentPayment;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public boolean isAcPayeeOnly() {
        return acPayeeOnly;
    }

    public void setAcPayeeOnly(boolean acPayeeOnly) {
        this.acPayeeOnly = acPayeeOnly;
    }
}
