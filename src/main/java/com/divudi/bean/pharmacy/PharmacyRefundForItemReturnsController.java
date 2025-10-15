/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.cashTransaction.FinancialTransactionController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ControllerWithMultiplePayments;
import com.divudi.bean.common.PatientDepositController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SearchController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.TokenController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BooleanMessage;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.data.TokenType;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.Token;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.TokenFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.PharmacyBean;
import com.divudi.service.BillService;
import com.divudi.service.DiscountSchemeValidationService;
import com.divudi.service.PaymentService;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PharmacyRefundForItemReturnsController implements Serializable, ControllerWithMultiplePayments {

    /**
     * Creates a new instance of PharmacySaleController
     */
    public PharmacyRefundForItemReturnsController() {
    }

    @Inject
    SessionController sessionController;
    @Inject
    SearchController searchController;
    @Inject
    PatientDepositController patientDepositController;
    @Inject
    TokenController tokenController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    FinancialTransactionController financialTransactionController;
    @Inject
    DrawerController drawerController;
////////////////////////
    @EJB
    DiscountSchemeValidationService discountSchemeValidationService;
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
    PaymentFacade paymentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PaymentService paymentService;
    @EJB
    BillService billService;
/////////////////////////
    Item selectedAlternative;

    private Bill refundBill;
    private Bill saleBill;
    private Bill itemReturnBill;
    Bill bill;
    BillItem billItem;
    BillItem removingBillItem;
    BillItem editingBillItem;
    Double qty;
    Stock stock;
    private Institution toInstitution;

    private Patient newPatient;
    private Patient searchedPatient;
    private YearMonthDay yearMonthDay;
    private String patientTabId = "tabNewPt";
    private String strTenderedValue = "";
    boolean printPreview = false;
    private final AtomicBoolean billSettlingStarted = new AtomicBoolean(false);
    /////////////////
    private List<Bill> bills;
    private SearchKeyword searchKeyword;
    private Date fromDate;
    private Date toDate;
    /////////////////
    List<Stock> replaceableStocks;
    List<BillItem> billItems;
    List<Item> itemsWithoutStocks;
    private List<Token> settledToken;
    private PaymentMethodData paymentMethodData;
    private String refundComment;
    double cashPaid;
    double netTotal;
    double balance;
    double total;
    Double editingQty;
    private Token token;

    public void fillAllItemReturnBills() {

        bills = null;
        String jpql;
        Map params = new HashMap();
        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY);
        jpql = "select b from Bill b "
                + " where b.billTypeAtomic in :btas "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            jpql += " and  ((b.patient.person.name) like :patientName )";
            params.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            jpql += " and  ((b.patient.person.phone) like :patientPhone )";
            params.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            jpql += " and  ((b.deptId) like :billNo )";
            params.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            jpql += " and  ((b.netTotal) = :netTotal )";
            params.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            jpql += " and  ((b.total) like :total )";
            params.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        jpql += " order by b.createdAt desc  ";
//
        params.put("toDate", getToDate());
        params.put("fromDate", getFromDate());
        params.put("ins", getSessionController().getInstitution());
        params.put("btas", btas);

        bills = getBillFacade().findByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP, 50);

    }

    public double calculatRemainForMultiplePaymentTotal() {

        total = getRefundBill().getNetTotal();
        return total - calculateMultiplePaymentMethodTotal();
    }

    public void recieveRemainAmountAutomatically() {
        double remainAmount = calculatRemainForMultiplePaymentTotal();
        if (getRefundBill().getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            int arrSize = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
            ComponentDetail pm = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);
            if (pm.getPaymentMethod() == PaymentMethod.Cash) {
                pm.getPaymentMethodData().getCash().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Card) {
                pm.getPaymentMethodData().getCreditCard().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Cheque) {
                pm.getPaymentMethodData().getCheque().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Slip) {
                pm.getPaymentMethodData().getSlip().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.ewallet) {
                pm.getPaymentMethodData().getEwallet().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                if (getRefundBill().getPatient() == null || getRefundBill().getPatient().getId() == null) {
                    pm.getPaymentMethodData().getPatient_deposit().setTotalValue(0.0);
                    return; // Patient not selected yet, ignore
                }
                // Initialize patient deposit data for UI component
                pm.getPaymentMethodData().getPatient_deposit().setPatient(getRefundBill().getPatient());
                PatientDeposit pd = patientDepositController.getDepositOfThePatient(getRefundBill().getPatient(), sessionController.getDepartment());
                if (pd != null && pd.getId() != null) {
                    pm.getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                    pm.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                    // Set total value to remain amount only if there's sufficient balance, otherwise set to available balance
                    double availableBalance = pd.getBalance();
                    if (availableBalance >= remainAmount) {
                        pm.getPaymentMethodData().getPatient_deposit().setTotalValue(remainAmount);
                    } else {
                        pm.getPaymentMethodData().getPatient_deposit().setTotalValue(availableBalance);
                    }
                } else {
                    pm.getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(false);
                    pm.getPaymentMethodData().getPatient_deposit().setTotalValue(0.0);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.Credit) {
                pm.getPaymentMethodData().getCredit().setTotalValue(remainAmount);
            }

        }
    }

    public void updateTotals() {
        calculateAllRates();
    }

    public void calculateAllRates() {
        for (BillItem tbi : getRefundBill().getBillItems()) {
            calculateRates(tbi);
        }
        calculateTotals();
    }

    public void calculateTotals() {
        getRefundBill().setTotal(0);
        double netTotal = 0.0, grossTotal = 0.0, discountTotal = 0.0;
        int index = 0;

        for (BillItem b : getRefundBill().getBillItems()) {
            if (!b.isRetired()) {
                b.setSearialNo(index++);
                netTotal += b.getNetValue();
                grossTotal += b.getGrossValue();
                discountTotal += b.getDiscount();
            }
        }

        getRefundBill().setNetTotal(netTotal);
        getRefundBill().setTotal(grossTotal);
        getRefundBill().setGrantTotal(grossTotal);
        getRefundBill().setDiscount(discountTotal);
        setNetTotal(getRefundBill().getNetTotal());
    }

    public void calculateRates(BillItem bi) {
        PharmaceuticalBillItem pharmBillItem = bi.getPharmaceuticalBillItem();
        if (pharmBillItem != null && pharmBillItem.getStock() != null) {
            ItemBatch itemBatch = pharmBillItem.getStock().getItemBatch();
            if (itemBatch != null) {
                bi.setRate(itemBatch.getRetailsaleRate());
            }
            bi.setDiscountRate(calculateBillItemDiscountRate(bi));
            bi.setNetRate(bi.getRate() - bi.getDiscountRate());

            bi.setGrossValue(bi.getRate() * bi.getQty());
            bi.setDiscount(bi.getDiscountRate() * bi.getQty());
            bi.setNetValue(bi.getGrossValue() - bi.getDiscount());

        }
    }

    @Inject
    private PriceMatrixController priceMatrixController;

    public double calculateBillItemDiscountRate(BillItem bi) {
        if (bi == null) {
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem() == null) {
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem().getStock() == null) {
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
            return 0.0;
        }
        bi.setItem(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getItem());
        double retailRate = bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate();
        double discountRate = 0;
        boolean discountAllowed = bi.getItem().isDiscountAllowed();
//        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());
        //MEMBERSHIPSCHEME DISCOUNT
//        if (membershipScheme != null && discountAllowed) {
//            PaymentMethod tpm = getPaymentMethod();
//            if (tpm == null) {
//                tpm = PaymentMethod.Cash;
//            }
//            PriceMatrix priceMatrix = getPriceMatrixController().getPharmacyMemberDisCount(tpm, membershipScheme, getSessionController().getDepartment(), bi.getItem().getCategory());
//            if (priceMatrix == null) {
//                return 0;
//            } else {
//                bi.setPriceMatrix(priceMatrix);
//                return (retailRate * priceMatrix.getDiscountPercent()) / 100;
//            }
//        }
//
        //PAYMENTSCHEME DISCOUNT

        if (getRefundBill().getPaymentScheme() != null && discountAllowed) {
            PriceMatrix priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(getRefundBill().getPaymentMethod(), getRefundBill().getPaymentScheme(), getSessionController().getDepartment(), bi.getItem());

            if (priceMatrix != null) {
                bi.setPriceMatrix(priceMatrix);
                discountRate = priceMatrix.getDiscountPercent();
            }

            double dr;
            dr = (retailRate * discountRate) / 100;
            return dr;

        }

        //PAYMENTMETHOD DISCOUNT
        if (getRefundBill().getPaymentMethod() != null && discountAllowed) {
            PriceMatrix priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(getRefundBill().getPaymentMethod(), getSessionController().getDepartment(), bi.getItem());

            if (priceMatrix != null) {
                bi.setPriceMatrix(priceMatrix);
                discountRate = priceMatrix.getDiscountPercent();
            }

            double dr;
            dr = (retailRate * discountRate) / 100;
            return dr;

        }

        //CREDIT COMPANY DISCOUNT
        if (getRefundBill().getPaymentMethod() == PaymentMethod.Credit && toInstitution != null) {
            discountRate = toInstitution.getPharmacyDiscount();

            double dr;
            dr = (retailRate * discountRate) / 100;
            return dr;
        }
        return 0;

    }

    private boolean alreadyPaid(Bill itemReturnBill) {
        String jpql = "Select b "
                + " from Bill b"
                + " where b.referenceBill=:bil"
                + " and b.retired=false "
                + " and b.billTypeAtomic=:bta "
                + " and b.cancelled=false ";
        HashMap hm = new HashMap();
        hm.put("bil", itemReturnBill);
        hm.put("bta", BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS);
        Bill b = getBillFacade().findFirstByJpql(jpql, hm);
        if (b == null) {
            return false;
        }
        return true;
    }

    public String toSettleReturn(Bill selecttedItemReturnBill) {
        System.out.println("toSettleReturn");
        System.out.println("args = " + selecttedItemReturnBill);

        if (selecttedItemReturnBill == null) {
            JsfUtil.addErrorMessage("No Bill. Programmatic Error. Inform system administrator.");
            return null;
        }
        if (selecttedItemReturnBill.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No Bill. Programmatic Error. Inform system administrator.");
            return null;
        }
        if (selecttedItemReturnBill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY) {
            JsfUtil.addErrorMessage("Wrong Bill Type. Programmatic Error. Inform system administrator.");
            return null;
        }
        if (alreadyPaid(selecttedItemReturnBill)) {
            JsfUtil.addErrorMessage("This bill is already paid");
            return null;
        }
        this.itemReturnBill = billService.reloadBill(selecttedItemReturnBill);

        if (this.itemReturnBill == null) {
            JsfUtil.addErrorMessage("No Bill. Programmatic Error. Inform system administrator.");
            return null;
        }

        Bill originalSaleBill = this.itemReturnBill.getReferenceBill();

        if (originalSaleBill == null) {
            JsfUtil.addErrorMessage("No Bill. Programmatic Error. Inform system administrator.");
            return null;
        }

        Bill originalSalePreBill = originalSaleBill.getReferenceBill();
        
        

        Bill reloadedReturnBill = this.itemReturnBill;

        prepareForNewRefundForPharmacyReturnItems();

        this.itemReturnBill = reloadedReturnBill;

        refundBill = new Bill();
        refundBill.copy(this.itemReturnBill);
        refundBill.copyValue(this.itemReturnBill);
        refundBill.setBillType(BillType.PharmacySale);
        refundBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS);
        refundBill.setReferenceBill(this.itemReturnBill);

        List<Payment> originalPayments = billService.fetchBillPayments(originalSaleBill);

        if (originalPayments == null || originalPayments.isEmpty()) {
            originalPayments = billService.fetchBillPayments(originalSalePreBill);
        }

        if (originalPayments == null) {
            JsfUtil.addErrorMessage("Payments for the Original Bill is null. Programmatic Error. Inform system administrator.");
            return null;
        }
        if (originalPayments.isEmpty()) {
            JsfUtil.addErrorMessage("No Payment for the Original Bill. Programmatic Error. Inform system administrator.");
            return null;
        }

        System.out.println("=== TOSETTLE RETURN: About to initialize refund payments ===");
        // Initialize payment method data based on original payments
        initializeRefundPaymentFromOriginalPayments(originalPayments);
        System.out.println("=== TOSETTLE RETURN: Completed initialization ===");
        return "/pharmacy/pharmacy_bill_return_pre_cash?faces-redirect=true";

    }

    public void prepareForNewRefundForPharmacyReturnItems() {
        printPreview = false;
        selectedAlternative = null;
        refundBill = null;
        saleBill = null;
        itemReturnBill = null;
        bill = null;
        billItem = null;
        removingBillItem = null;
        editingBillItem = null;
        qty = 0.0;
        stock = null;
        newPatient = null;
        searchedPatient = null;
        yearMonthDay = null;
        patientTabId = "tabNewPt";
        strTenderedValue = "";
        replaceableStocks = null;
        billItems = null;
        itemsWithoutStocks = null;
        paymentMethodData = null;
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        editingQty = null;

    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
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

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public Double getEditingQty() {
        return editingQty;
    }

    public void setEditingQty(Double editingQty) {
        this.editingQty = editingQty;
    }

    public void onTabChange(TabChangeEvent event) {
        setPatientTabId(event.getTab().getId());

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

    public List<Item> getItemsWithoutStocks() {
        return itemsWithoutStocks;
    }

    public void setItemsWithoutStocks(List<Item> itemsWithoutStocks) {
        this.itemsWithoutStocks = itemsWithoutStocks;
    }

    public BillItem getBillItem() {
        if (billItem == null) {
            billItem = new BillItem();
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem(); // TODO : Why ?
            pbi.setBillItem(billItem);
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

    @Inject
    private PaymentSchemeController paymentSchemeController;

    private boolean errorCheckForSaleBill() {

        if (getRefundBill().getPaymentMethod() == null) {
            return true;
        }

        if (getRefundBill().getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            if (getPaymentMethodData() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }

            double remain = Math.abs(calculatRemainForMultiplePaymentTotal());
            if (remain > 1.0) {
                JsfUtil.addErrorMessage("Mismatch in differences of multiple payment method total and bill total");
                return true;
            }
        }
        //pharmacyPreSettleController.cashPaid
        if (paymentService.checkPaymentMethodError(getRefundBill().getPaymentMethod(), getPaymentMethodData(), getRefundBill().getNetTotal(), cashPaid, getRefundBill().getPatient(), getRefundBill().getToStaff())) {
            return true;
        }
        if (getRefundBill().getPaymentMethod() == PaymentMethod.Cash && (getCashPaid() - getRefundBill().getNetTotal()) < 0.0) {
            JsfUtil.addErrorMessage("Please select tendered amount correctly");
            return true;
        }
        return false;
    }

    private boolean errorCheckForSaleBillAraedyAddToStock() {
        Bill b = getBillFacade().find(getRefundBill().getId());
        if (b.isCancelled()) {
            return true;
        }

        return false;
    }

    @Inject
    private BillBeanController billBean;

    private void saveSaleBill() {
        getSaleBill().copy(getRefundBill());
        getSaleBill().copyValue(getRefundBill());

        getSaleBill().setBillType(BillType.PharmacySale);
        getSaleBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER);

        getSaleBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getSaleBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getBillBean().setPaymentMethodData(getSaleBill(), getSaleBill().getPaymentMethod(), paymentMethodData);

        getSaleBill().setBillDate(new Date());
        getSaleBill().setBillTime(new Date());
        getSaleBill().setCreatedAt(Calendar.getInstance().getTime());
        getSaleBill().setCreater(getSessionController().getLoggedUser());

        getSaleBill().setReferenceBill(getRefundBill());

        getSaleBill().setInsId(getRefundBill().getInsId());
        getSaleBill().setDeptId(getRefundBill().getDeptId());

        updateBalanceInBill(refundBill, getSaleBill(), refundBill.getPaymentMethod(), paymentMethodData);

//        getSaleBill().setCashPaid(cashPaid);
//        getSaleBill().setBalance(cashPaid - getPreBill().getNetTotal());
        if (getSaleBill().getId() == null) {
            getBillFacade().create(getSaleBill());
        }

        updatePreBill();

    }

    public void updateBalanceInBill(Bill preBill, Bill saleBill, PaymentMethod salePaymentMethod, PaymentMethodData paymentMethodDataForSaleBill) {
        if (salePaymentMethod == PaymentMethod.Cash) {
            saleBill.setCashPaid(cashPaid);
            saleBill.setBalance(cashPaid - preBill.getNetTotal());
        } else if (salePaymentMethod == PaymentMethod.Card) {
            saleBill.setBalance(paymentMethodDataForSaleBill.getCreditCard().getTotalValue() - preBill.getNetTotal());
        } else if (salePaymentMethod == PaymentMethod.Cheque) {
            saleBill.setBalance(paymentMethodData.getCheque().getTotalValue() - preBill.getNetTotal());
        } else if (salePaymentMethod == PaymentMethod.MultiplePaymentMethods) {
            saleBill.setBalance(calculatRemainForMultiplePaymentTotal());
        } else if (salePaymentMethod == PaymentMethod.Slip) {
            saleBill.setBalance(paymentMethodData.getSlip().getTotalValue() - preBill.getNetTotal());
        } else if (salePaymentMethod == PaymentMethod.ewallet) {
            saleBill.setBalance(paymentMethodData.getEwallet().getTotalValue() - preBill.getNetTotal());
        } else {
            saleBill.setBalance(-preBill.getNetTotal());
        }
    }

    private void saveBill() {
        getRefundBill().setNetTotal(0 - Math.abs(getRefundBill().getNetTotal()));
        getRefundBill().setTotal(0 - Math.abs(getRefundBill().getTotal()));
        getRefundBill().setDiscount(0 - Math.abs(getRefundBill().getDiscount()));
        getRefundBill().setHospitalFee(0 - Math.abs(getRefundBill().getHospitalFee()));
        getRefundBill().setProfessionalFee(0 - Math.abs(getRefundBill().getProfessionalFee()));

        getRefundBill().setBillType(BillType.PharmacySale);
        getRefundBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS);
        getRefundBill().setReferenceBill(getItemReturnBill());
        getRefundBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getRefundBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getBillBean().setPaymentMethodData(getRefundBill(), getRefundBill().getPaymentMethod(), paymentMethodData);

        getRefundBill().setBillDate(new Date());
        getRefundBill().setBillTime(new Date());

        // Generate Department ID based on configured strategy
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Retail Sale Return Item Payments - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Retail Sale Return Item Payments - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Retail Sale Return Item Payments - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS);
        } else {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(
                    getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS);
        }

        // Generate Institution ID independently according to configuration
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Retail Sale Return Item Payments - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Retail Sale Return Item Payments - Prefix + Institution Code + Department Code + Year + Yearly Number", false)
                || configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Retail Sale Return Item Payments - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            insId = deptId;
        } else {
            insId = getBillNumberBean().departmentBillNumberGeneratorYearly(
                    getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS);
        }

        getRefundBill().setDeptId(deptId);
        getRefundBill().setInsId(insId);

        if (getRefundBill().getId() == null) {
            getBillFacade().create(getRefundBill());
            getRefundBill().setCreatedAt(Calendar.getInstance().getTime());
            getRefundBill().setCreater(getSessionController().getLoggedUser());
        } else {
            getBillFacade().edit(getRefundBill());
        }
    }

    private void updatePreBill() {
        getRefundBill().setReferenceBill(getSaleBill());
        getBillFacade().editAndCommit(getRefundBill());
    }

    /**
     * Utility method to check that the newly created sale bill will have the
     * same number of bill items as the original pre bill. This is used to
     * detect when a bill was edited concurrently in multiple browser windows.
     *
     * @return {@code true} if both bills contain the same number of items
     */
    protected boolean billItemCountMatches() {
        int originalCount = 0;
        Bill fetchedBill = billService.fetchBillById(getRefundBill().getId());
        billService.reloadBill(fetchedBill);
        if (fetchedBill != null && fetchedBill.getBillItems() != null) {
            originalCount = fetchedBill.getBillItems().size();
        }

        int currentCount = 0;
        if (getRefundBill() != null && getRefundBill().getBillItems() != null) {
            currentCount = getRefundBill().getBillItems().size();
        }

        return originalCount == currentCount;
    }

    private void saveSaleBillItems() {
        for (BillItem tbi : getRefundBill().getBillItems()) {
            BillItem newBil = new BillItem();
            newBil.copy(tbi);
            newBil.setBill(getSaleBill());
            newBil.setInwardChargeType(InwardChargeType.Medicine);
            //      newBil.setBill(getSaleBill());
            newBil.setCreatedAt(Calendar.getInstance().getTime());
            newBil.setCreater(getSessionController().getLoggedUser());

            if (newBil.getId() == null) {
                getBillItemFacade().create(newBil);
            }

            PharmaceuticalBillItem newPhar = new PharmaceuticalBillItem();
            newPhar.copy(tbi.getPharmaceuticalBillItem());
            newPhar.setBillItem(newBil);

            if (newPhar.getId() == null) {
                getPharmaceuticalBillItemFacade().create(newPhar);
            }

            newBil.setPharmaceuticalBillItem(newPhar);
            getBillItemFacade().edit(newBil);

            //   getPharmacyBean().deductFromStock(tbi.getItem(), tbi.getQty(), tbi.getBill().getDepartment());
            getSaleBill().getBillItems().add(newBil);
        }
        getBillFacade().editAndCommit(getSaleBill());

    }

    private void saveSaleBillItems(Payment p) {
        for (BillItem tbi : getRefundBill().getBillItems()) {
            BillItem newBil = new BillItem();
            newBil.copy(tbi);
            newBil.setBill(getSaleBill());
            newBil.setInwardChargeType(InwardChargeType.Medicine);
            //      newBil.setBill(getSaleBill());
            newBil.setCreatedAt(Calendar.getInstance().getTime());
            newBil.setCreater(getSessionController().getLoggedUser());

            if (newBil.getId() == null) {
                getBillItemFacade().create(newBil);
            }

            PharmaceuticalBillItem newPhar = new PharmaceuticalBillItem();
            newPhar.copy(tbi.getPharmaceuticalBillItem());
            newPhar.setBillItem(newBil);

            if (newPhar.getId() == null) {
                getPharmaceuticalBillItemFacade().create(newPhar);
            }

            newBil.setPharmaceuticalBillItem(newPhar);
            getBillItemFacade().edit(newBil);

            //   getPharmacyBean().deductFromStock(tbi.getItem(), tbi.getQty(), tbi.getBill().getDepartment());
            //create billFee
            saveBillFee(newBil, p);

            getSaleBill().getBillItems().add(newBil);
        }
        getBillFacade().editAndCommit(getSaleBill());

    }

    private void saveSaleReturnBillItems() {
        for (BillItem tbi : getRefundBill().getBillItems()) {

            BillItem sbi = new BillItem();

            sbi.copy(tbi);
            // sbi.invertAndAssignValuesFromOtherBill(tbi);

            sbi.setBill(getRefundBill());
            sbi.setReferanceBillItem(tbi);
            sbi.setCreatedAt(Calendar.getInstance().getTime());
            sbi.setCreater(getSessionController().getLoggedUser());

            if (sbi.getId() == null) {
                getBillItemFacade().create(sbi);
            }

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(tbi.getPharmaceuticalBillItem());

            ph.setBillItem(sbi);

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            //        getPharmacyBean().deductFromStock(tbi.getItem(), tbi.getQty(), tbi.getBill().getDepartment());
            getRefundBill().getBillItems().add(sbi);
        }
        getBillFacade().edit(getRefundBill());
    }

    public List<Payment> createPaymentsForBill(Bill b) {
        return createMultiplePayments(b, b.getPaymentMethod());
    }

    public List<Payment> createMultiplePayments(Bill bill, PaymentMethod pm) {
        List<Payment> ps = new ArrayList<>();
        if (pm == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment p = new Payment();
                p.setBill(bill);
                p.setInstitution(getSessionController().getInstitution());
                p.setDepartment(getSessionController().getDepartment());
                p.setCreatedAt(new Date());
                p.setCreater(getSessionController().getLoggedUser());
                p.setPaymentMethod(cd.getPaymentMethod());

                switch (cd.getPaymentMethod()) {
                    case Card:
                        p.setBank(cd.getPaymentMethodData().getCreditCard().getInstitution());
                        p.setCreditCardRefNo(cd.getPaymentMethodData().getCreditCard().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCreditCard().getTotalValue());
                        break;
                    case Cheque:
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        break;
                    case ewallet:
                        p.setPaidValue(cd.getPaymentMethodData().getEwallet().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getEwallet().getComment());
                        break;
                    case PatientDeposit:
                        p.setPaidValue(cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                        break;
                    case Agent:
                    case Credit:
                    case Slip:
                    case OnCall:
                    case OnlineSettlement:
                    case Staff:
                    case YouOweMe:
                    case MultiplePaymentMethods:
                }

                paymentFacade.create(p);
                ps.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(bill);
            p.setInstitution(getSessionController().getInstitution());
            p.setDepartment(getSessionController().getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            p.setPaymentMethod(pm);

            switch (pm) {
                case Card:
                    p.setBank(paymentMethodData.getCreditCard().getInstitution());
                    p.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
                    p.setPaidValue(paymentMethodData.getCreditCard().getTotalValue());
                    break;
                case Cheque:
                    p.setChequeDate(paymentMethodData.getCheque().getDate());
                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
                    p.setPaidValue(paymentMethodData.getCheque().getTotalValue());
                    break;
                case Cash:
                    p.setPaidValue(paymentMethodData.getCash().getTotalValue());
                    break;
                case ewallet:
                    p.setPaidValue(paymentMethodData.getEwallet().getTotalValue());
                    p.setComments(paymentMethodData.getEwallet().getComment());
                    break;
                case Credit:
                    p.setPolicyNo(paymentMethodData.getCredit().getReferralNo());
                    p.setReferenceNo(paymentMethodData.getCredit().getReferenceNo());
                    p.setCreditCompany(paymentMethodData.getCredit().getInstitution());
                    p.setPaidValue(paymentMethodData.getCredit().getTotalValue());
                    p.setComments(paymentMethodData.getCredit().getComment());
                    bill.setToInstitution(paymentMethodData.getCredit().getInstitution());
                    break;
                case PatientDeposit:
                    p.setPaidValue(paymentMethodData.getPatient_deposit().getTotalValue());
                    break;
                case Agent:
                case Slip:
                case OnCall:
                case OnlineSettlement:
                case Staff:
                case YouOweMe:
                case MultiplePaymentMethods:
            }

            p.setPaidValue(p.getBill().getNetTotal());
            billFacade.edit(bill);
            paymentFacade.create(p);

            ps.add(p);
        }
        return ps;
    }

    private void saveSaleReturnBillItems(List<Payment> refundPayments) {
        for (BillItem tbi : getRefundBill().getBillItems()) {

            BillItem sbi = new BillItem();

            sbi.copy(tbi);
            // sbi.invertAndAssignValuesFromOtherBill(tbi);

            sbi.setBill(getRefundBill());
            sbi.setReferanceBillItem(tbi);
            sbi.setCreatedAt(Calendar.getInstance().getTime());
            sbi.setCreater(getSessionController().getLoggedUser());

            if (sbi.getId() == null) {
                getBillItemFacade().create(sbi);
            }

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(tbi.getPharmaceuticalBillItem());

            ph.setBillItem(sbi);

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            //        getPharmacyBean().deductFromStock(tbi.getItem(), tbi.getQty(), tbi.getBill().getDepartment());
            getRefundBill().getBillItems().add(sbi);
        }
    }

    public boolean errorCheckOnPaymentMethod() {
        System.out.println("=== ERROR CHECK ON PAYMENT METHOD DEBUG ===");
        System.out.println("Payment Method: " + getRefundBill().getPaymentMethod());
        System.out.println("Calling PaymentSchemeController.checkPaymentMethodError");

        if (getPaymentSchemeController().checkPaymentMethodError(getRefundBill().getPaymentMethod(), getPaymentMethodData())) {
            System.out.println("PaymentSchemeController returned error - exiting early");
            System.out.println("=== END ERROR CHECK DEBUG ===");
            return true;
        }
        System.out.println("PaymentSchemeController passed - continuing to specific payment method checks");

        if (getRefundBill().getPaymentMethod() == PaymentMethod.PatientDeposit) {
            System.out.println("=== PRESETTLE PATIENT DEPOSIT CHECK ===");
            // Ensure patient deposit data is initialized
            if (getRefundBill().getPatient() == null || getRefundBill().getPatient().getId() == null) {
                JsfUtil.addErrorMessage("Please select a patient first");
                return true;
            }

            double creditLimitAbsolute = 0.0;
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(getRefundBill().getPatient(), sessionController.getDepartment());

            if (pd == null) {
                JsfUtil.addErrorMessage("No Patient Deposit");
                return true;
            }

            double runningBalance = pd.getBalance();
            double availableForPurchase = runningBalance + creditLimitAbsolute;

            System.out.println("=== PATIENT DEPOSIT VALIDATION DEBUG ===");
            System.out.println("Patient ID: " + getRefundBill().getPatient().getId());
            System.out.println("Department ID: " + sessionController.getDepartment().getId());
            System.out.println("Patient Deposit ID: " + (pd != null ? pd.getId() : "NULL"));
            System.out.println("Running Balance: " + runningBalance);
            System.out.println("Available for Purchase: " + availableForPurchase);
            System.out.println("Bill Net Total: " + getRefundBill().getNetTotal());
            System.out.println("Current Total Value: " + getPaymentMethodData().getPatient_deposit().getTotalValue());

            // Always initialize/refresh patient deposit UI data for validation
            getPaymentMethodData().getPatient_deposit().setPatient(getRefundBill().getPatient());
            getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
            getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);

            // For single payment method, the user should pay the full bill amount from patient deposit
            double requiredAmount = getRefundBill().getNetTotal();
            getPaymentMethodData().getPatient_deposit().setTotalValue(requiredAmount);

            System.out.println("Required Amount: " + requiredAmount);
            System.out.println("Validation: " + requiredAmount + " > " + availableForPurchase + " = " + (requiredAmount > availableForPurchase));
            System.out.println("=== END DEBUG ===");

            if (requiredAmount > availableForPurchase) {
                JsfUtil.addErrorMessage("No Sufficient Patient Deposit. Available: " + availableForPurchase + ", Required: " + requiredAmount);
                return true;
            }
        }

        if (getRefundBill().getPaymentMethod() == PaymentMethod.Staff) {
            if (getRefundBill().getToStaff() == null) {
                JsfUtil.addErrorMessage("Please select Staff Member.");
                return true;
            }

            if (getRefundBill().getToStaff().getCurrentCreditValue() + netTotal > getRefundBill().getToStaff().getCreditLimitQualified()) {
                JsfUtil.addErrorMessage("No enough Credit.");
                return true;
            }
        }

        if (getRefundBill().getPaymentMethod() == PaymentMethod.Staff_Welfare) {
            System.out.println("=== STAFF WELFARE VALIDATION DEBUG ===");
            System.out.println("PreBill ToStaff: " + (getRefundBill().getToStaff() != null ? getRefundBill().getToStaff().getPerson().getName() : "NULL"));
            System.out.println("PaymentMethodData: " + (getPaymentMethodData() != null ? "EXISTS" : "NULL"));

            // Enhanced synchronization logic
            if (getPaymentMethodData() != null && getPaymentMethodData().getStaffCredit() != null) {
                System.out.println("PaymentMethodData.StaffCredit exists");
                System.out.println("StaffCredit ToStaff: " + (paymentMethodData.getStaffCredit().getToStaff() != null ? paymentMethodData.getStaffCredit().getToStaff().getPerson().getName() : "NULL"));

                if (paymentMethodData.getStaffCredit().getToStaff() != null && getRefundBill().getToStaff() == null) {
                    getRefundBill().setToStaff(paymentMethodData.getStaffCredit().getToStaff());
                    System.out.println("Synchronized from paymentMethodData to preBill: " + getRefundBill().getToStaff().getPerson().getName());
                } else if (paymentMethodData.getStaffCredit().getToStaff() == null && getRefundBill().getToStaff() != null) {
                    paymentMethodData.getStaffCredit().setToStaff(getRefundBill().getToStaff());
                    System.out.println("Synchronized from preBill to paymentMethodData: " + paymentMethodData.getStaffCredit().getToStaff().getPerson().getName());
                } else {
                    System.out.println("No synchronization needed - both have same state");
                }
            } else {
                System.out.println("WARNING: PaymentMethodData or StaffCredit is NULL during validation");
            }

            // Check if staff is selected in either location
            boolean staffSelected = false;
            if (getRefundBill().getToStaff() != null) {
                staffSelected = true;
                System.out.println("Staff found in preBill.toStaff: " + getRefundBill().getToStaff().getPerson().getName());
            } else if (getPaymentMethodData() != null && getPaymentMethodData().getStaffCredit() != null && getPaymentMethodData().getStaffCredit().getToStaff() != null) {
                // Synchronize one more time
                getRefundBill().setToStaff(getPaymentMethodData().getStaffCredit().getToStaff());
                staffSelected = true;
                System.out.println("Staff found in paymentMethodData and synchronized: " + getRefundBill().getToStaff().getPerson().getName());
            } else {
                System.out.println("WARNING: No staff found in either location");
            }

            System.out.println("Staff selected: " + staffSelected);
            System.out.println("=== END STAFF WELFARE VALIDATION DEBUG ===");

            if (!staffSelected) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare.");
                return true;
            }
            if (Math.abs(getRefundBill().getToStaff().getAnnualWelfareUtilized()) + netTotal > getRefundBill().getToStaff().getAnnualWelfareQualified()) {
                JsfUtil.addErrorMessage("No enough credit.");
                return true;
            }

        }
        if (getRefundBill().getPaymentMethod() == PaymentMethod.Card) {
            if (getPaymentMethodData().getCreditCard().getNo() == null || getPaymentMethodData().getCreditCard().getNo().isEmpty()) {
                JsfUtil.addErrorMessage("Card last 4 digits are missing");
                return true;
            }
        }

        if (getRefundBill().getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            if (getPaymentMethodData() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }

            // Validate individual payment methods in multiple payments
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (cd.getPaymentMethod().equals(PaymentMethod.PatientDeposit)) {
                    double creditLimitAbsolute = 0.0;
                    PatientDeposit pd = patientDepositController.getDepositOfThePatient(getRefundBill().getPatient(), sessionController.getDepartment());

                    if (pd == null) {
                        JsfUtil.addErrorMessage("No Patient Deposit");
                        return true;
                    }

                    double runningBalance = pd.getBalance();
                    double availableForPurchase = runningBalance + creditLimitAbsolute;

                    if (cd.getPaymentMethodData().getPatient_deposit().getTotalValue() > availableForPurchase) {
                        JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                        return true;
                    }
                }
            }

            //double differenceOfBillTotalAndPaymentValue = refundBill.getNetTotal() - calculateMultiplePaymentMethodTotal();
            //differenceOfBillTotalAndPaymentValue = Math.abs(differenceOfBillTotalAndPaymentValue);
            if (checkAndUpdateBalance() < 0) {
                JsfUtil.addErrorMessage("Mismatch in differences of multiple payment method total and bill total");
                return true;
            }

        }
        return false;
    }

    //ToDo : have to duplicate methods in the pharmacy sale. Will implement service class and need to centralize them.
    public double calculateMultiplePaymentMethodTotal() {
        double multiplePaymentMethodTotalValue = 0;
        if (refundBill.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {

            for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
            }
        }
        return multiplePaymentMethodTotalValue;
    }

    public double checkAndUpdateBalance() {
        System.out.println("=== CHECKANDUPDATE DEBUG ===");
        System.out.println("Payment Method: " + (getRefundBill().getPaymentMethod() != null ? getRefundBill().getPaymentMethod() : "NULL"));

        // Ensure Patient reference is managed in current UnitOfWork to avoid EclipseLink-6004
        try {
            if (getRefundBill() != null && getRefundBill().getPatient() != null && getRefundBill().getPatient().getId() != null) {
                Patient managedPatient = patientFacade.find(getRefundBill().getPatient().getId());
                if (managedPatient != null) {
                    getRefundBill().setPatient(managedPatient);
                }
            }
        } catch (Exception e) {
            // Safe fallback: keep existing patient reference
        }

        // Initialize PaymentMethodData if needed
        if (getPaymentMethodData() == null) {
            setPaymentMethodData(new PaymentMethodData());
            System.out.println("PaymentMethodData was null, created new instance");
        }

        // Initialize StaffCredit for Staff_Welfare payment method
        if (getRefundBill().getPaymentMethod() == PaymentMethod.Staff_Welfare) {
            System.out.println("Staff_Welfare payment method in checkAndUpdateBalance");
            System.out.println("PreBill ToStaff: " + (getRefundBill().getToStaff() != null ? getRefundBill().getToStaff().getPerson().getName() : "NULL"));

            // This will auto-initialize staffCredit if null
            getPaymentMethodData().getStaffCredit();
            System.out.println("StaffCredit initialized/retrieved");

            // Synchronize staff selection if it exists in refundBill but not in paymentMethodData
            if (getRefundBill().getToStaff() != null && getPaymentMethodData().getStaffCredit().getToStaff() == null) {
                getPaymentMethodData().getStaffCredit().setToStaff(getRefundBill().getToStaff());
                System.out.println("Synchronized staff from preBill to paymentMethodData: " + getRefundBill().getToStaff().getPerson().getName());
            } else {
                System.out.println("No synchronization needed in checkAndUpdateBalance");
            }
            System.out.println("StaffCredit ToStaff: " + (getPaymentMethodData().getStaffCredit().getToStaff() != null ? getPaymentMethodData().getStaffCredit().getToStaff().getPerson().getName() : "NULL"));
        }

        // Initialize PatientDeposit for PatientDeposit payment method
        if (getRefundBill().getPaymentMethod() == PaymentMethod.PatientDeposit) {
            System.out.println("=== PATIENTDEPOSIT INIT DEBUG ===");
            System.out.println("PreBill Patient: " + (getRefundBill().getPatient() != null ? getRefundBill().getPatient().getId() : "NULL"));
            if (getRefundBill().getPatient() == null || getRefundBill().getPatient().getId() == null) {
                System.out.println("Patient not selected, returning balance");
                System.out.println("=== END PATIENTDEPOSIT INIT DEBUG ===");
                return balance; // Patient not selected yet, ignore
            }
            try {
                System.out.println("About to initialize patient deposit UI data");
                // Initialize patient deposit data for UI component
                getPaymentMethodData().getPatient_deposit().setPatient(getRefundBill().getPatient());
                System.out.println("Set patient in payment method data");

                // Always use a managed Patient when querying facades/controllers
                Patient managedPatient = getRefundBill().getPatient();
                PatientDeposit pd = patientDepositController.getDepositOfThePatient(managedPatient, sessionController.getDepartment());
                System.out.println("Retrieved patient deposit: " + (pd != null ? pd.getId() : "NULL"));

                if (pd != null && pd.getId() != null) {
                    System.out.println("Patient deposit exists, setting up UI data");
                    getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                    getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                    // Set total value to bill amount only if there's sufficient balance, otherwise set to available balance
                    double availableBalance = pd.getBalance();
                    System.out.println("Available balance: " + availableBalance + ", Bill total: " + getRefundBill().getNetTotal());
                    if (availableBalance >= getRefundBill().getNetTotal()) {
                        getPaymentMethodData().getPatient_deposit().setTotalValue(getRefundBill().getNetTotal());
                        System.out.println("Set total value to bill amount: " + getRefundBill().getNetTotal());
                    } else {
                        getPaymentMethodData().getPatient_deposit().setTotalValue(availableBalance);
                        System.out.println("Set total value to available balance: " + availableBalance);
                    }
                } else {
                    System.out.println("No patient deposit found, setting hasAnAccount to false");
                    getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(false);
                    getPaymentMethodData().getPatient_deposit().setTotalValue(0.0);
                }
                System.out.println("Patient deposit initialization completed successfully");
            } catch (Exception e) {
                System.out.println("Exception during patient deposit initialization: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("=== END PATIENTDEPOSIT INIT DEBUG ===");
        }

        System.out.println("=== END CHECKANDUPDATE DEBUG ===");

        if (getRefundBill().getPaymentMethod() != null) {
            switch (getRefundBill().getPaymentMethod()) {
                case Cash:
                    balance = getRefundBill().getNetTotal() - cashPaid;
                    break;
                case Card:
                    cashPaid = 0;
                    balance = getRefundBill().getNetTotal() - getPaymentMethodData().getCreditCard().getTotalValue();
                    break;
                case Cheque:
                    cashPaid = 0;
                    balance = getRefundBill().getNetTotal() - getPaymentMethodData().getCheque().getTotalValue();
                    break;
                case Slip:
                    cashPaid = 0;
                    balance = getRefundBill().getNetTotal() - getPaymentMethodData().getSlip().getTotalValue();
                    break;
                case ewallet:
                    cashPaid = 0;
                    balance = getRefundBill().getNetTotal() - getPaymentMethodData().getEwallet().getTotalValue();
                    break;
                case MultiplePaymentMethods:
                    cashPaid = 0;
                    balance = getRefundBill().getNetTotal() - calculateMultiplePaymentMethodTotal();
                    break;
                case Staff_Welfare:
                    cashPaid = 0;
                    balance = 0; // Staff welfare covers the full amount
                    break;
                case PatientDeposit:
                    cashPaid = 0;
                    balance = getRefundBill().getNetTotal() - getPaymentMethodData().getPatient_deposit().getTotalValue();
                    break;
            }
        }

        updateTotals();
        return balance;
    }

//    public String settlePaymentAndNavigateToPrint() {
//        Boolean pharmacyBillingAfterShiftStart = configOptionApplicationController.getBooleanValueByKey("Pharmacy billing can be done after shift start", false);
//
//        if (pharmacyBillingAfterShiftStart) {
//            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
//            if (financialTransactionController.getNonClosedShiftStartFundBill() == null) {
//                JsfUtil.addErrorMessage("Start Your Shift First !");
//                financialTransactionController.navigateToFinancialTransactionIndex();
//            }
//        }
//
//        editingQty = null;
//        if (getRefundBill().getBillType() == BillType.PharmacyPre
//                && getRefundBill().getBillClassType() != BillClassType.PreBill) {
//            JsfUtil.addErrorMessage("This Bill isn't Accept. Please Try Again.");
//            prepareForNewRefundForPharmacyReturnItems();
//            return null;
//        }
//        if (errorCheckForSaleBill()) {
//            return null;
//        }
//        if (!billItemCountMatches()) {
//            JsfUtil.addErrorMessage("Bill was opened in multiple windows. Please close all windows and start again");
//            return null;
//        }
//        if (errorCheckForSaleBillAraedyAddToStock()) {
//            JsfUtil.addErrorMessage("This Bill Can't Pay.Because this bill already added to stock in Pharmacy.");
//            return null;
//        }
//        if (!getRefundBill().getDepartment().equals(getSessionController().getLoggedUser().getDepartment())) {
//            JsfUtil.addErrorMessage("Can't settle bills of " + getRefundBill().getDepartment().getName());
//            return null;
//        }
//
//        if (errorCheckOnPaymentMethod()) {
//            return null;
//        }
//
//        if (getRefundBill().getPaymentMethod() == PaymentMethod.Cash) {
//            if (checkAndUpdateBalance() > 0) {
//                JsfUtil.addErrorMessage("Missmatch in bill total and paid total amounts.");
//                return null;
//            }
//        }
//
//        BooleanMessage discountSchemeValidation = discountSchemeValidationService.validateDiscountScheme(getRefundBill().getPaymentMethod(), getRefundBill().getPaymentScheme(), getPaymentMethodData());
//        if (!discountSchemeValidation.isFlag()) {
//            JsfUtil.addErrorMessage(discountSchemeValidation.getMessage());
//            return null;
//        }
//
//        Map<String, Object> params = new HashMap<>();
//        params.put("pre", getRefundBill().getId());
//        Bill existing = getBillFacade().findFirstByJpql("select b from BilledBill b where b.referenceBill.id=:pre", params, true);
//        if (existing != null) {
//            JsfUtil.addErrorMessage("Already Paid");
//            return null;
//        }
//
//        saveSaleBill();
////        saveSaleBillItems();
//
//        List<Payment> payments = createPaymentsForBill(getSaleBill());
//        drawerController.updateDrawerForIns(payments);
//        saveSaleBillItems();
//
//        getBillFacade().editAndCommit(getRefundBill());
//
//        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(getSaleBill(), getSessionController().getLoggedUser());
//        getSessionController().setLoggedUser(wb);
//        setBill(getBillFacade().find(getSaleBill().getId()));
//
//        paymentService.updateBalances(payments);
//
//        markComplete(getRefundBill());
//        printPreview = true;
//
//        return navigateToPrintPharmacyRetailBillSettlePrint();
//    }

    public String navigateToPrintPharmacyRetailBillSettlePrint() {
        return "/pharmacy/printing/settle_retail_sale_for_cashier?faces-redirect=true";
    }

//    @Deprecated // Please use settlePaymentAndNavigateToPrint
//    public void settleBillWithPay2() {
//
//        Boolean pharmacyBillingAfterShiftStart = configOptionApplicationController.getBooleanValueByKey("Pharmacy billing can be done after shift start", false);
//
//        if (pharmacyBillingAfterShiftStart) {
//            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
//            if (financialTransactionController.getNonClosedShiftStartFundBill() == null) {
//                JsfUtil.addErrorMessage("Start Your Shift First !");
//                financialTransactionController.navigateToFinancialTransactionIndex();
//            }
//        }
//
//        editingQty = null;
//        if (getRefundBill().getBillType() == BillType.PharmacyPre
//                && getRefundBill().getBillClassType() != BillClassType.PreBill) {
//            JsfUtil.addErrorMessage("This Bill isn't Accept. Please Try Again.");
//            prepareForNewRefundForPharmacyReturnItems();
//            return;
//        }
//        if (errorCheckForSaleBill()) {
//            return;
//        }
//        if (!billItemCountMatches()) {
//            JsfUtil.addErrorMessage("Bill was opened in multiple windows. Please close all windows and start again");
//            return;
//        }
//        if (errorCheckForSaleBillAraedyAddToStock()) {
//            JsfUtil.addErrorMessage("This Bill Can't Pay.Because this bill already added to stock in Pharmacy.");
//            return;
//        }
//        if (!getRefundBill().getDepartment().equals(getSessionController().getLoggedUser().getDepartment())) {
//            JsfUtil.addErrorMessage("Can't settle bills of " + getRefundBill().getDepartment().getName());
//            return;
//        }
//
//        if (errorCheckOnPaymentMethod()) {
//            return;
//        }
//
//        if (getRefundBill().getPaymentMethod() == PaymentMethod.Cash) {
//            if (checkAndUpdateBalance() > 0) {
//                JsfUtil.addErrorMessage("Missmatch in bill total and paid total amounts.");
//                return;
//            }
//        }
//
//        BooleanMessage discountSchemeValidation = discountSchemeValidationService.validateDiscountScheme(getRefundBill().getPaymentMethod(), getRefundBill().getPaymentScheme(), getPaymentMethodData());
//        if (!discountSchemeValidation.isFlag()) {
//            JsfUtil.addErrorMessage(discountSchemeValidation.getMessage());
//            return;
//        }
//
//        Map<String, Object> params = new HashMap<>();
//        params.put("pre", getRefundBill().getId());
//        Bill existing = getBillFacade().findFirstByJpql("select b from BilledBill b where b.referenceBill.id=:pre", params, true);
//        if (existing != null) {
//            JsfUtil.addErrorMessage("Already Paid");
//            return;
//        }
//
//        saveSaleBill();
////        saveSaleBillItems();
//
//        List<Payment> payments = createPaymentsForBill(getSaleBill());
//        drawerController.updateDrawerForIns(payments);
//        saveSaleBillItems();
//
//        getBillFacade().editAndCommit(getRefundBill());
//
//        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(getSaleBill(), getSessionController().getLoggedUser());
//        getSessionController().setLoggedUser(wb);
//        setBill(getBillFacade().find(getSaleBill().getId()));
//
//        paymentService.updateBalances(payments);
//
//        markComplete(getRefundBill());
//        printPreview = true;
//
//    }

    public Token findTokenFromBill(Bill bill) {
        return tokenController.findPharmacyTokenSaleForCashier(bill, TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER);
    }

    public String findTokenNumberFromBill(Bill bill) {
        Token currentToken = tokenController.findPharmacyTokenSaleForCashier(bill, TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER);
        if (currentToken == null) {
            return null;
        }
        return currentToken.getTokenNumber();
    }

    public void markInProgress(Bill bill) {
        Token t = findTokenFromBill(bill);
        if (t == null) {
            return;
        }
        t.setCalled(false);
        t.setCalledAt(null);
        t.setInProgress(true);
        t.setCompleted(false);
        tokenController.save(t);

    }

    public void markComplete(Bill bill) {
        Token t = findTokenFromBill(bill);
        if (t == null) {
            return;
        }
        t.setInProgress(false);
        t.setCompleted(true);
        t.setCompletedAt(new Date());
        tokenController.save(t);

    }

    public void paymentOngoingToken(Bill bill) {
        Token t = findTokenFromBill(bill);
        if (t == null) {
            return;
        }
        t.setCalled(true);
        t.setCalledAt(null);
        t.setInProgress(true);
        t.setCompleted(false);
        tokenController.save(t);
        tokenFacade.flush();

    }

    public void tokenDisplayToggle(Bill bill) {
        Token t = findTokenFromBill(bill);
        if (t == null) {
            return;
        }

        if (t.getDisplayToken() == null) {
            t.setDisplayToken(true);
        } else {
            t.setDisplayToken(!t.getDisplayToken());
        }
        tokenFacade.edit(t);

    }

    public void unmarkToken(Bill bill) {
        Token t = findTokenFromBill(bill);
        if (t == null) {
            return;
        }
        t.setCalled(false);
        t.setCalledAt(null);
        t.setInProgress(true);
        t.setCompleted(false);
        tokenController.save(t);
        tokenFacade.flush();

    }

    @EJB
    private TokenFacade tokenFacade;

    public void markToken(Bill bill) {
        Token t = findTokenFromBill(bill);
        if (t == null) {
            return;
        }
        t.setCalled(true);
        t.setCalledAt(new Date());
        t.setInProgress(false);
        t.setCompleted(false);
        tokenController.save(t);
        tokenFacade.flush();

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
    }

    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        setPaymentMethodData(p, pm);
        return p;
    }

    public void setPaymentMethodData(Payment p, PaymentMethod pm) {
        if (p == null) {
            return;
        }

        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);

        if (p.getBill() != null) {
            p.setPaidValue(p.getBill().getNetTotal());
        } else {
            p.setPaidValue(0.0);
        }

        if (p.getId() == null) {
            try {
                getPaymentFacade().create(p);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }

    }

    @EJB
    CashTransactionBean cashTransactionBean;

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void settleRefundForReturnItems() {
        editingQty = null;
        saveBill();

        List<Payment> refundPayments = paymentService.createPayment(getRefundBill(), getPaymentMethodData());
        // Negate payment values for refunds (money going out)
        for (Payment p : refundPayments) {
            p.setPaidValue(0 - Math.abs(p.getPaidValue()));
            paymentFacade.edit(p);
        }
        saveSaleReturnBillItems(refundPayments);

        getBillFacade().edit(getRefundBill());

        setBill(getBillFacade().find(getRefundBill().getId()));
        paymentService.updateBalances(refundPayments);
        clearBill();
        clearBillItem();
        printPreview = true;

    }

    private void clearBill() {
        refundBill = null;
        saleBill = null;
        newPatient = null;
        searchedPatient = null;
        billItems = null;
        patientTabId = "tabNewPt";
        refundComment = null;
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
    }

    private void clearBillItem() {
        billItem = null;
        removingBillItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        editingQty = null;
//        paymentMethod = PaymentMethod.Cash;
        paymentMethodData = null;
        setCashPaid(0.0);
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

    public Patient getNewPatient() {
        if (newPatient == null) {
            newPatient = new Patient();
            Person p = new Person();

            newPatient.setPerson(p);
        }
        return newPatient;
    }

    public void setNewPatient(Patient newPatient) {
        this.newPatient = newPatient;
    }

    public Patient getSearchedPatient() {
        return searchedPatient;
    }

    public void setSearchedPatient(Patient searchedPatient) {
        this.searchedPatient = searchedPatient;
    }

    public YearMonthDay getYearMonthDay() {
        if (yearMonthDay == null) {
            yearMonthDay = new YearMonthDay();
        }
        return yearMonthDay;
    }

    public void setYearMonthDay(YearMonthDay yearMonthDay) {
        this.yearMonthDay = yearMonthDay;
    }

    public String toSettle(Bill billToSettle) {
        Boolean pharmacyBillingAfterShiftStart = configOptionApplicationController.getBooleanValueByKey("Pharmacy billing can be done after shift start", false);
        if (pharmacyBillingAfterShiftStart) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() == null) {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        }

        if (billToSettle == null) {
            JsfUtil.addErrorMessage("Programming Error.");
            return null;
        }

        if (billToSettle.getId() == null) {
            JsfUtil.addErrorMessage("Programming Error.");
            return null;
        }

        billToSettle = billService.reloadBill(billToSettle.getId());

        if (billToSettle.getBillType() == BillType.PharmacyPre && billToSettle.getBillClassType() == BillClassType.PreBill) {
            String sql = "Select b from BilledBill b"
                    + " where b.referenceBill=:bil"
                    + " and b.retired=false "
                    + " and b.cancelled=false ";
            HashMap hm = new HashMap();
            hm.put("bil", billToSettle);
            Bill b = getBillFacade().findFirstByJpql(sql, hm);

            if (b != null) {
                JsfUtil.addErrorMessage("Allready Paid");
                return "";
            } else {
                setRefundBill(billToSettle);
                getRefundBill().setPaymentMethod(billToSettle.getPaymentMethod());
                getRefundBill().setPaymentScheme(billToSettle.getPaymentScheme());

                // Extract and assign staff for Staff_Welfare payment method
                System.out.println("=== TOSETTLE DEBUG ===");
                System.out.println("Payment Method: " + billToSettle.getPaymentMethod());
                System.out.println("Args ToStaff: " + (billToSettle.getToStaff() != null ? billToSettle.getToStaff().getPerson().getName() : "NULL"));

                if (billToSettle.getPaymentMethod() == PaymentMethod.Staff_Welfare) {
                    System.out.println("Staff_Welfare payment method detected");
                    if (billToSettle.getToStaff() != null) {
                        // Assign staff to refundBill
                        getRefundBill().setToStaff(billToSettle.getToStaff());
                        System.out.println("Assigned to preBill.toStaff: " + getRefundBill().getToStaff().getPerson().getName());

                        // Initialize PaymentMethodData and assign staff to payment method data
                        if (getPaymentMethodData() == null) {
                            setPaymentMethodData(new PaymentMethodData());
                            System.out.println("PaymentMethodData was null, created new instance");
                        }
                        getPaymentMethodData().getStaffCredit().setToStaff(billToSettle.getToStaff());
                        System.out.println("Assigned to paymentMethodData.staffCredit.toStaff: " + getPaymentMethodData().getStaffCredit().getToStaff().getPerson().getName());
                    } else {
                        System.out.println("WARNING: Staff_Welfare payment method but args.getToStaff() is NULL");
                    }
                } else {
                    System.out.println("Not Staff_Welfare payment method, skipping staff extraction");
                }
                System.out.println("=== END TOSETTLE DEBUG ===");

                billSettlingStarted.set(false);
//                paymentMethod = getPreBill().getPaymentMethod();
                return "/pharmacy/pharmacy_bill_pre_settle?faces-redirect=true";
            }
        } else {
            searchController.makeListNull();
            JsfUtil.addErrorMessage("Please Search Again and Accept Bill");
            return "";
        }
    }

    public Bill getRefundBill() {
        if (refundBill == null) {
            refundBill = new PreBill();
            refundBill.setBillType(BillType.PharmacyPre);
            refundBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS);
        }
        return refundBill;
    }

    public void setRefundBill(Bill refundBill) {
        prepareForNewRefundForPharmacyReturnItems();
        this.refundBill = refundBill;
        //System.err.println("Setting Bill " + refundBill);
        printPreview = false;

    }

    public Bill getSaleBill() {
        if (saleBill == null) {
            saleBill = new BilledBill();
        }
        return saleBill;
    }

    public void setSaleBill(Bill saleBill) {
        this.saleBill = saleBill;
    }

    public String getPatientTabId() {
        return patientTabId;
    }

    public void setPatientTabId(String patientTabId) {
        this.patientTabId = patientTabId;
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

    public String getStrTenderedValue() {
        return strTenderedValue;
    }

    public void setStrTenderedValue(String strTenderedValue) {
        this.strTenderedValue = strTenderedValue;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public double getCashPaid() {
        return cashPaid;
    }

    public void setCashPaid(double cashPaid) {
        balance = cashPaid - netTotal;
        this.cashPaid = cashPaid;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        //balance = cashPaid - netTotal;
        this.netTotal = netTotal;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {

        this.bill = bill;
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

    public String getRefundComment() {
        return refundComment;
    }

    public void setRefundComment(String refundComment) {
        this.refundComment = refundComment;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public List<Token> getSettledToken() {
        return settledToken;
    }

    public void setSettledToken(List<Token> settledToken) {
        this.settledToken = settledToken;
    }

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public boolean isBillSettlingStarted() {
        return billSettlingStarted.get();
    }

    public void setBillSettlingStarted(boolean billSettlingStarted) {
        this.billSettlingStarted.set(billSettlingStarted);
    }

    public void listenerForPaymentMethodChange() {
        if (getRefundBill().getPaymentMethod() == PaymentMethod.PatientDeposit) {
            getPaymentMethodData().getPatient_deposit().setPatient(getRefundBill().getPatient());
            getPaymentMethodData().getPatient_deposit().setTotalValue(getRefundBill().getNetTotal());
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(getRefundBill().getPatient(), sessionController.getDepartment());
            if (pd != null && pd.getId() != null) {
                getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
            }
        } else if (getRefundBill().getPaymentMethod() == PaymentMethod.Card) {
            getPaymentMethodData().getCreditCard().setTotalValue(getRefundBill().getNetTotal());
        } else if (getRefundBill().getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            getPaymentMethodData().getPatient_deposit().setPatient(getRefundBill().getPatient());
            getPaymentMethodData().getPatient_deposit().setTotalValue(calculatRemainForMultiplePaymentTotal());
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(getRefundBill().getPatient(), sessionController.getDepartment());

            if (pd != null && pd.getId() != null) {
                boolean hasPatientDeposit = false;
                for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    if (cd.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                        hasPatientDeposit = true;
                        cd.getPaymentMethodData().getPatient_deposit().setPatient(getRefundBill().getPatient());
                        cd.getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                        cd.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                    }
                }
            }
        }
    }

    public void calTotals() {
        System.out.println("=== CALTOTALS DEBUG ===");
        System.out.println("Payment Method: " + (getRefundBill().getPaymentMethod() != null ? getRefundBill().getPaymentMethod() : "NULL"));
        System.out.println("PreBill ToStaff: " + (getRefundBill().getToStaff() != null ? getRefundBill().getToStaff().getPerson().getName() : "NULL"));
        System.out.println("PaymentMethodData: " + (getPaymentMethodData() != null ? "EXISTS" : "NULL"));
        if (getPaymentMethodData() != null) {
            System.out.println("StaffCredit: " + (getPaymentMethodData().getStaffCredit() != null ? "EXISTS" : "NULL"));
            if (getPaymentMethodData().getStaffCredit() != null) {
                System.out.println("StaffCredit ToStaff: " + (getPaymentMethodData().getStaffCredit().getToStaff() != null ? getPaymentMethodData().getStaffCredit().getToStaff().getPerson().getName() : "NULL"));
            }
        }

        // Synchronize staff selection for Staff_Welfare payment method
        if (getRefundBill().getPaymentMethod() == PaymentMethod.Staff_Welfare) {
            System.out.println("Staff_Welfare payment method in calTotals");
            // Ensure PaymentMethodData is initialized
            if (getPaymentMethodData() != null && getPaymentMethodData().getStaffCredit() != null) {
                // Get the selected staff from the payment method data
                if (getPaymentMethodData().getStaffCredit().getToStaff() != null) {
                    // Synchronize to refundBill.toStaff for validation
                    getRefundBill().setToStaff(getPaymentMethodData().getStaffCredit().getToStaff());
                    System.out.println("Staff synchronized in calTotals: " + getRefundBill().getToStaff().getPerson().getName());
                } else {
                    System.out.println("WARNING: StaffCredit.toStaff is NULL in calTotals");
                }
            } else {
                System.out.println("WARNING: PaymentMethodData or StaffCredit is NULL in calTotals");
            }
        }

        // Handle patient deposit initialization
        listenerForPaymentMethodChange();
        System.out.println("=== END CALTOTALS DEBUG ===");

        calculateAllRates();
        checkAndUpdateBalance();
    }

    /**
     * Retrieves the original bill's payments for a refund bill. Used to display
     * original payment details and auto-populate refund payment methods.
     *
     * @return List of Payment objects from the original sale bill, or empty
     * list if not available
     */
    public List<Payment> getOriginalBillPayments() {
        if (getRefundBill() != null
                && getRefundBill().getBillClassType() == BillClassType.RefundBill
                && getRefundBill().getReferenceBill() != null) {
            Bill originalSaleBill = getRefundBill().getReferenceBill();
            if (originalSaleBill.getPayments() != null && !originalSaleBill.getPayments().isEmpty()) {
                return originalSaleBill.getPayments();
            }
        }
        return new ArrayList<>();
    }

    /**
     * Automatically initializes refund payment method data based on original
     * bill's payments. This ensures refunds use the same payment methods as the
     * original sale for proper audit trail.
     *
     * @param originalPayments List of payments from the original sale bill
     */
    private void initializeRefundPaymentFromOriginalPayments(List<Payment> originalPayments) {
        if (originalPayments == null || originalPayments.isEmpty()) {
            System.out.println("=== INIT REFUND PAYMENT DEBUG: No original payments ===");
            return;
        }

        System.out.println("=== INIT REFUND PAYMENT DEBUG ===");
        System.out.println("Original payments count: " + originalPayments.size());

        // If single payment method
        if (originalPayments.size() == 1) {
            Payment originalPayment = originalPayments.get(0);
            getRefundBill().setPaymentMethod(originalPayment.getPaymentMethod());

            System.out.println("Payment Method: " + originalPayment.getPaymentMethod());

            // Initialize paymentMethodData based on payment method
            switch (originalPayment.getPaymentMethod()) {
                case Cash:
                    getPaymentMethodData().getCash().setTotalValue(Math.abs(getRefundBill().getNetTotal()));
                    break;
                case Card:
                    getPaymentMethodData().getCreditCard().setInstitution(originalPayment.getBank());
                    getPaymentMethodData().getCreditCard().setNo(originalPayment.getCreditCardRefNo());
                    getPaymentMethodData().getCreditCard().setTotalValue(Math.abs(getRefundBill().getNetTotal()));
                    break;
                case Cheque:
                    getPaymentMethodData().getCheque().setDate(originalPayment.getChequeDate());
                    getPaymentMethodData().getCheque().setNo(originalPayment.getChequeRefNo());
                    getPaymentMethodData().getCheque().setTotalValue(Math.abs(getRefundBill().getNetTotal()));
                    break;
                case Slip:
                    getPaymentMethodData().getSlip().setInstitution(originalPayment.getBank());
                    getPaymentMethodData().getSlip().setDate(originalPayment.getChequeDate());
                    getPaymentMethodData().getSlip().setReferenceNo(originalPayment.getReferenceNo());
                    getPaymentMethodData().getSlip().setComment(originalPayment.getComments());
                    getPaymentMethodData().getSlip().setTotalValue(Math.abs(getRefundBill().getNetTotal()));
                    break;
                case ewallet:
                    getPaymentMethodData().getEwallet().setInstitution(originalPayment.getBank());
                    getPaymentMethodData().getEwallet().setReferenceNo(originalPayment.getReferenceNo());
                    getPaymentMethodData().getEwallet().setTotalValue(Math.abs(getRefundBill().getNetTotal()));
                    getPaymentMethodData().getEwallet().setComment(originalPayment.getComments());
                    break;
                case PatientDeposit:
                    getPaymentMethodData().getPatient_deposit().setTotalValue(Math.abs(getRefundBill().getNetTotal()));
                    getPaymentMethodData().getPatient_deposit().setPatient(getRefundBill().getPatient());
                    getPaymentMethodData().getPatient_deposit().setComment(originalPayment.getComments());
                    // Load and set the PatientDeposit object for displaying balance
                    if (getRefundBill().getPatient() != null) {
                        PatientDeposit pd = patientDepositController.getDepositOfThePatient(
                            getRefundBill().getPatient(),
                            sessionController.getDepartment()
                        );
                        if (pd != null && pd.getId() != null) {
                            getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                            getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                        }
                    }
                    break;
                case Credit:
                    System.out.println("Credit Company: " + (originalPayment.getCreditCompany() != null ? originalPayment.getCreditCompany().getName() : "NULL"));
                    System.out.println("Policy No: " + originalPayment.getPolicyNo());
                    System.out.println("Reference No: " + originalPayment.getReferenceNo());
                    System.out.println("Comments: " + originalPayment.getComments());

                    getPaymentMethodData().getCredit().setInstitution(originalPayment.getCreditCompany());
                    getPaymentMethodData().getCredit().setReferenceNo(originalPayment.getReferenceNo());
                    getPaymentMethodData().getCredit().setReferralNo(originalPayment.getPolicyNo());
                    getPaymentMethodData().getCredit().setTotalValue(Math.abs(getRefundBill().getNetTotal()));
                    getPaymentMethodData().getCredit().setComment(originalPayment.getComments());

                    System.out.println("After setting - Institution: " + (getPaymentMethodData().getCredit().getInstitution() != null ? getPaymentMethodData().getCredit().getInstitution().getName() : "NULL"));
                    System.out.println("After setting - Referral No: " + getPaymentMethodData().getCredit().getReferralNo());
                    System.out.println("After setting - Reference No: " + getPaymentMethodData().getCredit().getReferenceNo());
                    System.out.println("After setting - Comment: " + getPaymentMethodData().getCredit().getComment());
                    break;
                default:
                    // For other payment methods, just set the total value
                    break;
            }
        } // If multiple payment methods
        else {
            getRefundBill().setPaymentMethod(PaymentMethod.MultiplePaymentMethods);

            // Clear any existing multiple payment details
            getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().clear();

            for (Payment originalPayment : originalPayments) {
                ComponentDetail cd = new ComponentDetail();
                cd.setPaymentMethod(originalPayment.getPaymentMethod());

                // Set payment details based on method - use absolute value for UI display
                double refundAmount = Math.abs(originalPayment.getPaidValue());

                switch (originalPayment.getPaymentMethod()) {
                    case Cash:
                        cd.getPaymentMethodData().getCash().setTotalValue(refundAmount);
                        break;
                    case Card:
                        cd.getPaymentMethodData().getCreditCard().setInstitution(originalPayment.getBank());
                        cd.getPaymentMethodData().getCreditCard().setNo(originalPayment.getCreditCardRefNo());
                        cd.getPaymentMethodData().getCreditCard().setTotalValue(refundAmount);
                        break;
                    case Cheque:
                        cd.getPaymentMethodData().getCheque().setDate(originalPayment.getChequeDate());
                        cd.getPaymentMethodData().getCheque().setNo(originalPayment.getChequeRefNo());
                        cd.getPaymentMethodData().getCheque().setTotalValue(refundAmount);
                        break;
                    case Slip:
                        cd.getPaymentMethodData().getSlip().setInstitution(originalPayment.getBank());
                        cd.getPaymentMethodData().getSlip().setDate(originalPayment.getChequeDate());
                        cd.getPaymentMethodData().getSlip().setReferenceNo(originalPayment.getReferenceNo());
                        cd.getPaymentMethodData().getSlip().setComment(originalPayment.getComments());
                        cd.getPaymentMethodData().getSlip().setTotalValue(refundAmount);
                        break;
                    case ewallet:
                        cd.getPaymentMethodData().getEwallet().setInstitution(originalPayment.getBank());
                        cd.getPaymentMethodData().getEwallet().setReferenceNo(originalPayment.getReferenceNo());
                        cd.getPaymentMethodData().getEwallet().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getEwallet().setComment(originalPayment.getComments());
                        break;
                    case PatientDeposit:
                        cd.getPaymentMethodData().getPatient_deposit().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getPatient_deposit().setPatient(getRefundBill().getPatient());
                        cd.getPaymentMethodData().getPatient_deposit().setComment(originalPayment.getComments());
                        // Load and set the PatientDeposit object for displaying balance
                        if (getRefundBill().getPatient() != null) {
                            PatientDeposit pd = patientDepositController.getDepositOfThePatient(
                                getRefundBill().getPatient(),
                                sessionController.getDepartment()
                            );
                            if (pd != null && pd.getId() != null) {
                                cd.getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                                cd.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                            }
                        }
                        break;
                    case Credit:
                        cd.getPaymentMethodData().getCredit().setInstitution(originalPayment.getCreditCompany());
                        cd.getPaymentMethodData().getCredit().setReferenceNo(originalPayment.getReferenceNo());
                        cd.getPaymentMethodData().getCredit().setReferralNo(originalPayment.getPolicyNo());
                        cd.getPaymentMethodData().getCredit().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getCredit().setComment(originalPayment.getComments());
                        break;
                    default:
                        // For other payment methods
                        break;
                }

                getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().add(cd);
            }
        }
    }

    public Bill getItemReturnBill() {
        return itemReturnBill;
    }

    public void setItemReturnBill(Bill itemReturnBill) {
        this.itemReturnBill = itemReturnBill;
    }

}
