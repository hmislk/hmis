/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.CommonController;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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

//PharmacyDealerController
    //pharmacyDealerController
    @EJB
    CreditBean creditBean;
    @EJB
    CashTransactionBean cashTransactionBean;

    @Inject
    private BillController billController;
    @Inject
    private BillBeanController billBean;
    @Inject
    private PaymentSchemeController paymentSchemeController;

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
    //Atribtes
    private boolean printPreview;
    private SearchKeyword searchKeyword;
    private Bill current;
    private PaymentMethodData paymentMethodData;
    private BillItem currentBillItem;
    private Institution institution;
    //List
    private List<BillItem> billItems;
    private List<BillItem> selectedBillItems;
    //EJB
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
    //Inject
    @Inject
    private SessionController sessionController;
    @Inject
    CommonController commonController;
    private int tabIndex = 0;

    public String navigateToDealerPaymentIndex() {
        return "/dealerPayment/index?faces-redirect=true";
    }

    public String navigateToDealerDueSearch() {
        bills = new ArrayList<>();
        return "/dealerPayment/dealor_due?faces-redirect=true";
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

    public String navigateToGRNPaymentDoneSearch() {
        makeNull();
        return "/dealerPayment/search_dealor_payment?faces-redirect=true";
    }

    public String navigateToSupplierPaymentDoneSearch() {
        bills = null;
        return "/dealerPayment/search_dealor_payment?faces-redirect=true";
    }

    public String navigateToCreditDuesAndAccess() {
        return "/credit/index_pharmacy_due_access?faces-redirect=true";
    }

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
    
    public void fillAllCreditBillssettled(){
        bills = null;
        String jpql;
        Map params = new HashMap();
        List<BillType> billTypes = new ArrayList<>();
        billTypes.add(BillType.GrnPaymentPre);
        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.SUPPLIER_PAYMENT);

        jpql = "select b from Bill b "
                + " where b.retired=false "
                + " and (b.billType = :billTypes or b.billTypeAtomic = :btas)  "
                + " and b.createdAt between :fromDate and :toDate ";

        params.put("billTypes", BillType.GrnPaymentPre);
        params.put("toDate", getToDate());
        params.put("fromDate", getFromDate());
        params.put("btas", BillTypeAtomic.SUPPLIER_PAYMENT);

        bills = getBillFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
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

}
