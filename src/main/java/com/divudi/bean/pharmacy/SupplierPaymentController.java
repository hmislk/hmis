/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.InstitutionType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.InstitutionBills;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.data.table.String1Value5;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.CreditBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillFeePayment;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.Payment;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillFeePaymentFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.java.CommonFunctions;
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
import javax.enterprise.context.Dependent;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

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
    CommonController commonController;
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
        bills = null;
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
        bills = null;
        bills = new ArrayList<>();
        netTotal = 0.0;
        return "/dealerPayment/search_dealor_payment_pharmacy?faces-redirect=true";
    }

    public String navigateToGRNPaymentDoneSearch() {
        bills = null;
        bills = new ArrayList<>();
        netTotal = 0.0;
        return "/dealerPayment/dealor_payment_done_search?faces-redirect=true";
    }

    public String navigateToPaymentDoneSearch() {
        bills = null;
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

    public String naviateToSupplierBillApprovalManagement() {
        bills = null;
        return "/dealerPayment/supplier_bill_approval_management?faces-redirect=true";
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

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            jpql += " and  ((b.patient.person.name) like :patientName )";
            params.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            jpql += " and  ((b.patient.person.phone) like :patientPhone )";
            params.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            jpql += " and  ((b.insId) like :billNo )";
            params.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            jpql += " and  ((b.netTotal) like :netTotal )";
            params.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            jpql += " and  ((b.total) like :total )";
            params.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        jpql += " order by b.createdAt desc  ";
//    
        params.put("billTypes", billTypes);

        params.put("toDate", getToDate());
        params.put("fromDate", getFromDate());

        System.err.println("Sql " + jpql);
        System.out.println("temMap = " + params);
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
        double refBallance = 0;
        double neTotal = Math.abs(billItem.getReferenceBill().getNetTotal());
        double returned = Math.abs(billItem.getReferenceBill().getTmpReturnTotal());
        double paidAmt = Math.abs(getCreditBean().getPaidAmount(billItem.getReferenceBill(), BillType.GrnPaymentPre));

        refBallance = neTotal - (paidAmt + returned);

        return refBallance;
    }

    public void selectListener() {

        double ballanceAmt = getReferenceBallance(getCurrentBillItem());

        if (ballanceAmt > 0.1) {
            getCurrentBillItem().setNetValue(ballanceAmt);
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

            selectListener();
            addToBill();
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
        calTotal();
    }

    public void calTotalBySelectedBillTems() {
        if (selectedBillItems == null) {
            getCurrent().setNetTotal(0);
            return;
        }

        double n = 0.0;
        for (BillItem b : selectedBillItems) {
            n += Math.abs(b.getNetValue());
        }
        getCurrent().setTotal(-n);
        getCurrent().setNetTotal(-n);
    }

    public void calTotal() {
        double n = 0.0;
        for (BillItem b : billItems) {
            n += b.getNetValue();
        }
        getCurrent().setTotal(-n);
        getCurrent().setNetTotal(0 - n);
    }

    public void calTotalAtSupplierPaymentBillSettling() {
        double n = 0.0;
        for (BillItem payingBillItem : billItems) {
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
        getCurrent().setTotal(n);
        getCurrent().setNetTotal(n);
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

    public void removeAll() {
        for (BillItem b : selectedBillItems) {

            remove(b);
        }

        //   calTotalWithResetingIndex();
        selectedBillItems = null;
    }

    public void remove(BillItem billItem) {
        getBillItems().remove(billItem.getSearialNo());
        calTotalWithResetingIndex();
    }

    public void removeWithoutIndex(BillItem billItem) {
        getBillItems().remove(billItem.getSearialNo());

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

    private void saveBill(BillType billType) {

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

        getCurrent().setNetTotal(getCurrent().getNetTotal());

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
        BillTypeAtomic[] billTypesArrayBilled = {BillTypeAtomic.PHARMACY_GRN, BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL, BillTypeAtomic.PHARMACY_DIRECT_PURCHASE, BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL, BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL};
        List<BillTypeAtomic> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        bills = billController.findUnpaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01);
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
        BillTypeAtomic[] billTypesArrayBilled = {BillTypeAtomic.STORE_GRN, BillTypeAtomic.STORE_DIRECT_PURCHASE};
        List<BillTypeAtomic> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        bills = billController.findUnpaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01);
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
        BillTypeAtomic[] billTypesArrayBilled = {BillTypeAtomic.PHARMACY_GRN, BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL, BillTypeAtomic.PHARMACY_DIRECT_PURCHASE, BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL, BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL, BillTypeAtomic.STORE_GRN, BillTypeAtomic.STORE_DIRECT_PURCHASE};
        List<BillTypeAtomic> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        bills = billController.findUnpaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01);
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
            if (supplierPaymentStatus.equals("Pending")) {
                jpql.append(" AND b.referenceBill IS NULL ");
            } else if (supplierPaymentStatus.equals("Approved")) {
                jpql.append(" AND b.referenceBill.billType = :approvedBillType AND b.referenceBill.cancelled = false ");
                params.put("approvedBillType", BillType.GrnPayment);
            } else if (supplierPaymentStatus.equals("Canceled")) {
                jpql.append(" AND b.referenceBill.cancelled = true ");
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
            if (supplierPaymentStatus.equals("Pending")) {
                jpql.append(" AND b.referenceBill IS NULL ");
            } else if (supplierPaymentStatus.equals("Approved")) {
                jpql.append(" AND b.referenceBill.billType = :approvedBillType AND b.referenceBill.cancelled = false ");
                params.put("approvedBillType", BillType.GrnPayment);
            } else if (supplierPaymentStatus.equals("Canceled")) {
                jpql.append(" AND b.referenceBill.cancelled = true ");
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

    public void fillSupplierBillsForPayments(Boolean approved, Boolean completed) {
        bills = null;
        netTotal = 0.0;
        StringBuilder jpql = new StringBuilder("select b from Bill b "
                + " where b.retired=:retired "
                + " and b.cancelled=:cancelled "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.billTypeAtomic in :btas ");

        Map<String, Object> params = new HashMap<>();
        List<BillTypeAtomic> btas = Arrays.asList(
                BillTypeAtomic.PHARMACY_GRN,
                BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);

        params.put("btas", btas);
        params.put("cancelled", false);
        params.put("retired", false);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        // Conditionally append paymentApproved if parameter is not null
        if (approved != null) {
            if (approved) {
                jpql.append(" and b.paymentApproved = :approved ");
                params.put("approved", true);
            } else {
                jpql.append(" and b.paymentApproved = :approved ");
                params.put("approved", false);
            }
        }

        // Conditionally append paymentCompleted if parameter is not null
        if (completed != null) {
            if (completed) {
                jpql.append(" and b.paymentCompleted = :completed ");
                params.put("completed", true);
            } else {
                jpql.append(" and b.paymentCompleted = :completed ");
                params.put("completed", false);
            }
        }

        System.out.println("jpql = " + jpql);
        System.out.println("params = " + params);
        bills = getBillFacade().findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        System.out.println("bills = " + bills);
        netTotal = bills.stream().mapToDouble(Bill::getNetTotal).sum();
    }

    public void fillSupplierBillsToApproveForPayments() {
        supplierPaymentStatus = "Pending Payment Approval";
        fillSupplierBillsForPayments(false, null);
    }

    public void fillSupplierBillsApprovedForPayments() {
        supplierPaymentStatus = "Payment Approved";
        fillSupplierBillsForPayments(true, null);
    }

    public void fillSupplierBills() {
        supplierPaymentStatus = "All";
        fillSupplierBillsForPayments(null, null);
    }

    public String navigateToApprovePayment() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Please select a bill");
            return null;
        }
        return "/dealerPayment/approve_bill_for_payment?faces-redirect=true;";
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
        if (bill.getPaymentApprovalComments() == null || bill.getPaymentApprovalComments().trim().equals("")) {
            JsfUtil.addErrorMessage("Noot a Comment. Can not approve payment");
            return null;
        }
        bill.setPaymentApproved(true);
        bill.setPaymentApprovedAt(new Date());
        bill.setPaymentApprovedBy(sessionController.getLoggedUser());
        billFacade.edit(bill);
        return naviateToSupplierBillApprovalManagement();
    }

    public String navigateToCompletePayment() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Please select a bill");
            return null;
        }
        return "/dealerPayment/complete_bill_payment?faces-redirect=true;";
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
        if (bill.getComments() == null || bill.getComments().trim().equals("")) {
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
        fillSupplierBillsForPayments(null, true);
    }

// Method to retrieve pending completion (approved but not completed)
    public void fillSupplierBillsPendingCompletionForPayments() {
        supplierPaymentStatus = "Pending Payment Completion";
        boolean approvalIsNeeded = configOptionApplicationController.getBooleanValueByKey("Approval is necessary for Procument Payments", false);
        if (approvalIsNeeded) {
            fillSupplierBillsForPayments(true, false);
        } else {
            fillSupplierBillsForPayments(null, false);
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
        Set<Institution> setIns = new HashSet<>();
        List<Institution> list = getCreditBean().getDealorFromBills(getFromDate(), getToDate(), billTypeBilled);

        list.addAll(getCreditBean().getDealorFromReturnBills(getFromDate(), getToDate(), billTypeReturned));

        setIns.addAll(list);
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
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

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
        getBillItems().add(currentBillItem);
        calTotalBySelectedBillTems();
        calTotal();
        return "/dealerPayment/pay_supplier?faces-redirect=true";
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
            getBillItems().add(currentBillItem);
        }
        current.setToInstitution(paymentSupplier);
        calTotalBySelectedBillTems();
        calTotal();
        return "/dealerPayment/pay_supplier?faces-redirect=true";
    }

    public void settleSupplierPayment() {
        if (errorCheck()) {
            return;
        }
        calTotalAtSupplierPaymentBillSettling();
        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());
        getCurrent().setTotal(getCurrent().getNetTotal());

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

        getCurrent().setNetTotal(getCurrent().getNetTotal());

        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        } else {
            getBillFacade().edit(getCurrent());
        }

        Payment p = createPayment(getCurrent(), getCurrent().getPaymentMethod());
        saveBillItemBySelectedItems(p);

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public void settleBillAll() {
        if (errorCheck()) {
            return;
        }
        if (getSelectedBillItems() == null || getSelectedBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("There is No Bills seected to settle");
            return;
        }

        calTotalBySelectedBillTems();
        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        getCurrent().setTotal(getCurrent().getNetTotal());

        saveBill(BillType.GrnPaymentPre);
        Payment p = createPayment(getCurrent(), getCurrent().getPaymentMethod());
        saveBillItemBySelectedItems(p);

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);

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
            tmp.setNetValue(0 - tmp.getNetValue());

            if (tmp.getId() == null) {
                getBillItemFacade().create(tmp);
            }

            updateReferenceBill(tmp);

        }

    }

    private void saveBillItemBySelectedItems(Payment p) {
        for (BillItem tmp : getSelectedBillItems()) {
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setBill(getCurrent());
            tmp.setNetValue(0 - tmp.getNetValue());

            if (tmp.getId() == null) {
                getBillItemFacade().create(tmp);
            }
            saveBillFee(tmp, p);
            updateReferenceBill(tmp);

        }

    }

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

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = com.divudi.java.CommonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = new Date();
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

}
