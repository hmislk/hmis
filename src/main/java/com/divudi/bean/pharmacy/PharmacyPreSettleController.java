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
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
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
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.TokenFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.PharmacyBean;
import com.divudi.service.DiscountSchemeValidationService;
import com.divudi.service.PaymentService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PharmacyPreSettleController implements Serializable, ControllerWithMultiplePayments {

    /**
     * Creates a new instance of PharmacySaleController
     */
    public PharmacyPreSettleController() {
    }

    @Inject
    SessionController sessionController;
    @Inject
    SearchController searchController;
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
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PaymentService paymentService;
/////////////////////////
    Item selectedAlternative;
    Bill saleReturnBill;

    private Bill preBill;
    private Bill saleBill;
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
    boolean billPreview = false;
    /////////////////
    List<Stock> replaceableStocks;
    List<BillItem> billItems;
    List<Item> itemsWithoutStocks;
    private List<Token> settledToken;
    /////////////////////////
//    private PaymentScheme paymentScheme;
    private PaymentMethodData paymentMethodData;
    double cashPaid;
    double netTotal;
    double balance;
    double total;
    Double editingQty;
    private Token token;
//    private PaymentMethod paymentMethod;

    public double calculatRemainForMultiplePaymentTotal() {

        total = getPreBill().getNetTotal();
        return total - calculateMultiplePaymentMethodTotal();
    }

    public void recieveRemainAmountAutomatically() {
        double remainAmount = calculatRemainForMultiplePaymentTotal();
        if (getPreBill().getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
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
                pm.getPaymentMethodData().getPatient_deposit().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Credit) {
                pm.getPaymentMethodData().getCredit().setTotalValue(remainAmount);
            }

        }
    }

    public void updateTotals() {
        calculateAllRates();
    }

    public void calculateAllRates() {
        for (BillItem tbi : getPreBill().getBillItems()) {
            calculateRates(tbi);
//            calculateBillItemForEditing(tbi);
        }
        calculateTotals();
    }

    public void calculateTotals() {
        getPreBill().setTotal(0);
        double netTotal = 0.0, grossTotal = 0.0, discountTotal = 0.0;
        int index = 0;

        for (BillItem b : getPreBill().getBillItems()) {
            if (!b.isRetired()) {
                b.setSearialNo(index++);
                netTotal += b.getNetValue();
                grossTotal += b.getGrossValue();
                discountTotal += b.getDiscount();
//                getPreBill().setTotal(getPreBill().getTotal() + b.getNetValue());
            }
        }

        getPreBill().setNetTotal(netTotal);
        getPreBill().setTotal(grossTotal);
        getPreBill().setGrantTotal(grossTotal);
        getPreBill().setDiscount(discountTotal);
        setNetTotal(getPreBill().getNetTotal());
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

        //System.out.println("getPaymentScheme() = " + getPaymentScheme());
        if (getPreBill().getPaymentScheme() != null && discountAllowed) {
//            System.out.println("getPaymentMethod() = " + getPaymentMethod());
//            System.out.println("getPaymentScheme() = " + getPaymentScheme());
//            System.out.println("getSessionController().getDepartment() = " + getSessionController().getDepartment());
//            System.out.println("bi.getItem() = " + bi.getItem());
            PriceMatrix priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(getPreBill().getPaymentMethod(), getPreBill().getPaymentScheme(), getSessionController().getDepartment(), bi.getItem());

            System.err.println("priceMatrix = " + priceMatrix);
            if (priceMatrix != null) {
                bi.setPriceMatrix(priceMatrix);
                discountRate = priceMatrix.getDiscountPercent();
                System.out.println("discountRate = " + discountRate);
            }

            double dr;
            dr = (retailRate * discountRate) / 100;
            System.out.println("1 dr = " + dr);
            return dr;

        }

        //PAYMENTMETHOD DISCOUNT
        if (getPreBill().getPaymentMethod() != null && discountAllowed) {
            PriceMatrix priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(getPreBill().getPaymentMethod(), getSessionController().getDepartment(), bi.getItem());

            if (priceMatrix != null) {
                bi.setPriceMatrix(priceMatrix);
                discountRate = priceMatrix.getDiscountPercent();
            }

            double dr;
            dr = (retailRate * discountRate) / 100;
            System.out.println("2 dr = " + dr);
            return dr;

        }

        //CREDIT COMPANY DISCOUNT
        if (getPreBill().getPaymentMethod() == PaymentMethod.Credit && toInstitution != null) {
            discountRate = toInstitution.getPharmacyDiscount();

            double dr;
            dr = (retailRate * discountRate) / 100;
            System.out.println("3 dr = " + dr);
            return dr;
        }
        System.out.println("no dr");
        return 0;

    }

    public String toSettleReturn(Bill args) {
        if (args.getBillType() == BillType.PharmacyPre && args.getBillClassType() == BillClassType.RefundBill) {
            String sql = "Select b from RefundBill b"
                    + " where b.referenceBill=:bil"
                    + " and b.retired=false "
                    + " and b.refundedBill is null "
                    + " and b.cancelled=false ";
            HashMap hm = new HashMap();
            hm.put("bil", args);
            Bill b = getBillFacade().findFirstByJpql(sql, hm);

            if (b != null) {
                JsfUtil.addErrorMessage("Allready Paid");
                return "";
            } else {
                setPreBill(args);
                return "/pharmacy/pharmacy_bill_return_pre_cash";
            }
        } else {
            searchController.makeListNull();
            JsfUtil.addErrorMessage("Please Search Again and Refund Bill");
            return "";
        }
    }

    public void makeNull() {
        selectedAlternative = null;
        preBill = null;
        saleBill = null;
        saleReturnBill = null;
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
        billPreview = false;
        replaceableStocks = null;
        billItems = null;
        itemsWithoutStocks = null;
        paymentMethodData = null;
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        editingQty = null;

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

    @SuppressWarnings("empty-statement")
    private boolean errorCheckForSaleBill() {

        if (getPreBill().getPaymentMethod() == null) {
            return true;
        }

        if (getPreBill().getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
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

            if (calculatRemainForMultiplePaymentTotal() != 0) {
                JsfUtil.addErrorMessage("MutiplePayment total is not equal to bill Total. Check the Balance");
                return true;
            }

//            double multiplePaymentMethodTotalValue = 0.0;
//            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
//                //TODO - filter only relavant value
//                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
//                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
//                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
//                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
//                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
//                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
//            }
//            double differenceOfBillTotalAndPaymentValue = netTotal - multiplePaymentMethodTotalValue;
//            differenceOfBillTotalAndPaymentValue = Math.abs(differenceOfBillTotalAndPaymentValue);
//            if (differenceOfBillTotalAndPaymentValue > 1.0) {
//                JsfUtil.addErrorMessage("Mismatch in differences of multiple payment method total and bill total");
//                return true;
//            }
//            if (cashPaid == 0.0) {
//                setCashPaid(multiplePaymentMethodTotalValue);
//            }
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getPreBill().getPaymentMethod(), paymentMethodData));

        if (getPreBill().getPaymentMethod() == PaymentMethod.Cash && (getCashPaid() - getPreBill().getNetTotal()) < 0.0) {
            JsfUtil.addErrorMessage("Please select tendered amount correctly");
            return true;
        }

//        if (getPreBill().getPaymentScheme().getPaymentMethod() == PaymentMethod.Cash) {
//            if (cashPaid == 0.0) {
//                JsfUtil.addErrorMessage("Please select tendered amount correctly");
//                return true;
//            }
//            if (cashPaid < getNetTotal()) {
//                JsfUtil.addErrorMessage("Please select tendered amount correctly");
//                return true;
//            }
//        }
        return false;
    }

    private boolean errorCheckForSaleBillAraedyAddToStock() {
        Bill b = getBillFacade().find(getPreBill().getId());
        if (b.isCancelled()) {
            return true;
        }

        return false;
    }

    @Inject
    private BillBeanController billBean;

    private void saveSaleBill() {
        getSaleBill().copy(getPreBill());
        getSaleBill().copyValue(getPreBill());

        getSaleBill().setBillType(BillType.PharmacySale);
        getSaleBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER);

        getSaleBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getSaleBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getBillBean().setPaymentMethodData(getSaleBill(), getSaleBill().getPaymentMethod(), paymentMethodData);

        getSaleBill().setBillDate(new Date());
        getSaleBill().setBillTime(new Date());
        getSaleBill().setCreatedAt(Calendar.getInstance().getTime());
        getSaleBill().setCreater(getSessionController().getLoggedUser());

        getSaleBill().setReferenceBill(getPreBill());

        getSaleBill().setInsId(getPreBill().getInsId());
        getSaleBill().setDeptId(getPreBill().getDeptId());

        updateBalanceInBill(preBill, getSaleBill(), preBill.getPaymentMethod(), paymentMethodData);

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

    private void saveSaleReturnBill() {
        getSaleReturnBill().copy(getPreBill());
        getSaleReturnBill().copyValue(getPreBill());

        getSaleReturnBill().setBillType(BillType.PharmacySale);
        getSaleReturnBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS);
        getSaleReturnBill().setReferenceBill(getPreBill());
        getSaleReturnBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getSaleReturnBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getBillBean().setPaymentMethodData(getSaleReturnBill(), getSaleReturnBill().getPaymentMethod(), paymentMethodData);

        getSaleReturnBill().setBillDate(new Date());
        getSaleReturnBill().setBillTime(new Date());
        getSaleReturnBill().setCreatedAt(Calendar.getInstance().getTime());
        getSaleReturnBill().setCreater(getSessionController().getLoggedUser());

        getSaleReturnBill().setInsId(getPreBill().getInsId());
        getSaleReturnBill().setDeptId(getPreBill().getDeptId());

        if (getSaleReturnBill().getId() == null) {
            getBillFacade().create(getSaleReturnBill());
        }

        updateSaleReturnPreBill();
    }

    private void updateSaleReturnPreBill() {
        getPreBill().setReferenceBill(getSaleReturnBill());
        getBillFacade().edit(getPreBill());
    }

    private void updatePreBill() {
        getPreBill().setReferenceBill(getSaleBill());
        getBillFacade().edit(getPreBill());
    }

    private void saveSaleBillItems() {
        for (BillItem tbi : getPreBill().getBillItems()) {
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
        getBillFacade().edit(getSaleBill());

    }

    private void saveSaleBillItems(Payment p) {
        for (BillItem tbi : getPreBill().getBillItems()) {
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
        getBillFacade().edit(getSaleBill());

    }

    private void saveSaleReturnBillItems() {
        for (BillItem tbi : getPreBill().getBillItems()) {

            BillItem sbi = new BillItem();

            sbi.copy(tbi);
            // sbi.invertAndAssignValuesFromOtherBill(tbi);

            sbi.setBill(getSaleReturnBill());
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
            getSaleReturnBill().getBillItems().add(sbi);
        }
        getBillFacade().edit(getSaleReturnBill());
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

                    case Agent:
                    case Credit:
                    case PatientDeposit:
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

                case Agent:
                case Credit:
                case PatientDeposit:
                case Slip:
                case OnCall:
                case OnlineSettlement:
                case Staff:
                case YouOweMe:
                case MultiplePaymentMethods:
            }

            p.setPaidValue(p.getBill().getNetTotal());
            paymentFacade.create(p);

            ps.add(p);
        }
        return ps;
    }

    private void saveSaleReturnBillItems(Payment p) {
        for (BillItem tbi : getPreBill().getBillItems()) {

            BillItem sbi = new BillItem();

            sbi.copy(tbi);
            // sbi.invertAndAssignValuesFromOtherBill(tbi);

            sbi.setBill(getSaleReturnBill());
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

            saveBillFee(sbi, p);

            //        getPharmacyBean().deductFromStock(tbi.getItem(), tbi.getQty(), tbi.getBill().getDepartment());
            getSaleReturnBill().getBillItems().add(sbi);
        }
        getBillFacade().edit(getSaleReturnBill());
    }

    public boolean errorCheckOnPaymentMethod() {
        if (getPaymentSchemeController().checkPaymentMethodError(getPreBill().getPaymentMethod(), getPaymentMethodData())) {
            return true;
        }

        if (getPreBill().getPaymentMethod() == PaymentMethod.PatientDeposit) {
            if (!getPreBill().getPatient().getHasAnAccount()) {
                JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                return true;
            }
            double creditLimitAbsolute = Math.abs(getPreBill().getPatient().getCreditLimit());
            double runningBalance;
            if (getPreBill().getPatient().getRunningBalance() != null) {
                runningBalance = getPreBill().getPatient().getRunningBalance();
            } else {
                runningBalance = 0.0;
            }
            double availableForPurchase = runningBalance + creditLimitAbsolute;

            if (getPreBill().getNetTotal() > availableForPurchase) {
                JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                return true;
            }

        }

//        if (paymentMethod == PaymentMethod.Credit) {
//            if (creditCompany == null && collectingCentre == null) {
//                JsfUtil.addErrorMessage("Please select Staff Member under welfare or credit company or Collecting centre.");
//                return true;
//            }
//        }
        if (getPreBill().getPaymentMethod() == PaymentMethod.Staff) {
            if (getPreBill().getToStaff() == null) {
                JsfUtil.addErrorMessage("Please select Staff Member.");
                return true;
            }

            if (getPreBill().getToStaff().getCurrentCreditValue() + netTotal > getPreBill().getToStaff().getCreditLimitQualified()) {
                JsfUtil.addErrorMessage("No enough Credit.");
                return true;
            }
        }

        if (getPreBill().getPaymentMethod() == PaymentMethod.Staff_Welfare) {
            
            if(paymentMethodData.getStaffCredit().getToStaff()!=null && getPreBill().getToStaff()==null){
                getPreBill().setToStaff(paymentMethodData.getStaffCredit().getToStaff());
            }else if(paymentMethodData.getStaffCredit().getToStaff()==null && getPreBill().getToStaff()!=null){
                paymentMethodData.getStaffCredit().setToStaff(getPreBill().getToStaff());
            }
            
            if (getPreBill().getToStaff() == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare.");
                return true;
            }
            if (Math.abs(getPreBill().getToStaff().getAnnualWelfareUtilized()) + netTotal > getPreBill().getToStaff().getAnnualWelfareQualified()) {
                JsfUtil.addErrorMessage("No enough credit.");
                return true;
            }

        }

        if (getPreBill().getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
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

            //double differenceOfBillTotalAndPaymentValue = preBill.getNetTotal() - calculateMultiplePaymentMethodTotal();
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
        if (preBill.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {

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
        if (getPreBill().getPaymentMethod() != null) {
            switch (getPreBill().getPaymentMethod()) {
                case Cash:                   
                    balance = getPreBill().getNetTotal() - cashPaid;
                    break;
                case Card:
                    cashPaid = 0;
                    balance = getPreBill().getNetTotal() - getPaymentMethodData().getCreditCard().getTotalValue();
                    break;
                case Cheque:
                    cashPaid = 0;
                    balance = getPreBill().getNetTotal() - getPaymentMethodData().getCheque().getTotalValue();
                    break;
                case Slip:
                    cashPaid = 0;
                    balance = getPreBill().getNetTotal() - getPaymentMethodData().getSlip().getTotalValue();
                    break;
                case ewallet:
                    cashPaid = 0;
                    balance = getPreBill().getNetTotal() - getPaymentMethodData().getEwallet().getTotalValue();
                    break;
                case MultiplePaymentMethods:
                    cashPaid = 0;
                    balance = getPreBill().getNetTotal() - calculateMultiplePaymentMethodTotal();
                    break;
            }
        }
       
        updateTotals();
        return balance;
    }

    public void settleBillWithPay2() {
        Boolean pharmacyBillingAfterShiftStart = configOptionApplicationController.getBooleanValueByKey("Pharmacy billing can be done after shift start", false);

        if (pharmacyBillingAfterShiftStart) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() == null) {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                financialTransactionController.navigateToFinancialTransactionIndex();
            }
        }

        editingQty = null;
        if (getPreBill().getBillType() == BillType.PharmacyPre
                && getPreBill().getBillClassType() != BillClassType.PreBill) {
            JsfUtil.addErrorMessage("This Bill isn't Accept. Please Try Again.");
            makeNull();
            return;
        }
        if (errorCheckForSaleBill()) {
            return;
        }
        if (errorCheckForSaleBillAraedyAddToStock()) {
            JsfUtil.addErrorMessage("This Bill Can't Pay.Because this bill already added to stock in Pharmacy.");
            return;
        }
        if (!getPreBill().getDepartment().equals(getSessionController().getLoggedUser().getDepartment())) {
            JsfUtil.addErrorMessage("Can't settle bills of " + getPreBill().getDepartment().getName());
            return;
        }

        if (errorCheckOnPaymentMethod()) {
            return;
        }

        if (getPreBill().getPaymentMethod() == PaymentMethod.Cash) {
            if (checkAndUpdateBalance() > 0) {
                JsfUtil.addErrorMessage("Missmatch in bill total and paid total amounts.");
                return;
            }
        }

         BooleanMessage discountSchemeValidation = discountSchemeValidationService.validateDiscountScheme(getPreBill().getPaymentMethod(), getPreBill().getPaymentScheme(), getPaymentMethodData());
        if (!discountSchemeValidation.isFlag()) {
//            billSettlingStarted = false;
            JsfUtil.addErrorMessage(discountSchemeValidation.getMessage());
            return;
        }
        
        saveSaleBill();
//        saveSaleBillItems();

        //create Billfees,payments,billfeepayments
        List<Payment> payments = createPaymentsForBill(getSaleBill());
        drawerController.updateDrawerForIns(payments);
        saveSaleBillItems();

//        getPreBill().getCashBillsPre().add(getSaleBill());
        getBillFacade().edit(getPreBill());

        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(getSaleBill(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        setBill(getBillFacade().find(getSaleBill().getId()));

        paymentService.updateBalances(payments);

        markComplete(getPreBill());
//        markToken();
//        makeNull();
        //    removeSettledToken();
        billPreview = true;
    }
    
    public Token findTokenFromBill(Bill bill){
        return  tokenController.findPharmacyTokens(bill);     
    }
    
     public void markInProgress(Bill bill){
        System.out.println("start unmark");
        Token t = findTokenFromBill(bill);
        if(t == null){
            return;
        }
        t.setCalled(false);
        t.setCalledAt(null);
        t.setInProgress(true);
        t.setCompleted(false);
        tokenController.save(t);
        System.out.println("end unmark");
        
    }
     
     public void markComplete(Bill bill){
        Token t = findTokenFromBill(bill);
        if(t == null){
            return;
        }
        t.setInProgress(false);
        t.setCompleted(true);
        t.setCompletedAt(new Date());
        tokenController.save(t);
        
    }
     
      public void paymentOngoingToken(Bill bill){
        Token t = findTokenFromBill(bill);
        if(t == null){
            return;
        }
        t.setCalled(true);
        t.setCalledAt(null);
        t.setInProgress(true);
        t.setCompleted(false);
        tokenController.save(t);
        tokenFacade.flush();
        
    }
    
      public void tokenDisplayToggle(Bill bill){
          Token t = findTokenFromBill(bill);
          if(t == null){
              return;
          }
          
          if(t.getDisplayToken() == null){
              t.setDisplayToken(true);
          }else{
              t.setDisplayToken(!t.getDisplayToken());
          }
          tokenFacade.edit(t);
       
      }
    
    public void unmarkToken(Bill bill){
        Token t = findTokenFromBill(bill);
        if(t == null){
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

//    public void removeSettledToken() {
//        Token t = tokenController.findPharmacyTokens(getPreBill());
//        System.out.println("t = " + t.getTokenNumber());
//        if (t == null) {
//            return;
//        }
//        settledToken.add(t);
//        if (settledToken.size() > 3) {
//            saveSettledToken(settledToken.get(0));
//            settledToken.remove(0);
//        }
//    }
//    public void saveSettledToken(Token t) {
//        if (t == null) {
//            return;
//        }
//        t.setInProgress(false);
//        t.setCompletedAt(new Date());
//        t.setCompleted(true);
//        tokenController.save(t);
//        tokenController.fillPharmacyTokens();
//    }
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

        if (p.getId() == null) {
            getPaymentFacade().create(p);
        }

    }

    public void createBillFeePaymentAndPayment(BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(bf.getSettleValue());
        bfp.setInstitution(p.getBill().getFromInstitution());
        bfp.setDepartment(p.getBill().getFromDepartment());
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        getBillFeePaymentFacade().create(bfp);
    }

    @EJB
    CashTransactionBean cashTransactionBean;

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void settleReturnBillWithPay() {
        editingQty = null;
        if (getPreBill().getBillType() == BillType.PharmacyPre
                && getPreBill().getBillClassType() != BillClassType.RefundBill) {
            JsfUtil.addErrorMessage("This Bill isn't Return. Please Try Again.");
            clearBill();
            clearBillItem();
            return;
        }

        saveSaleReturnBill();
//        saveSaleReturnBillItems();

        Payment p = createPayment(getSaleReturnBill(), getSaleReturnBill().getPaymentMethod());
        saveSaleReturnBillItems(p);

//        getPreBill().getReturnCashBills().add(getSaleReturnBill());
        getBillFacade().edit(getPreBill());

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getSaleReturnBill(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        setBill(getBillFacade().find(getSaleReturnBill().getId()));

        clearBill();
        clearBillItem();
        billPreview = true;

    }

    private void clearBill() {
        preBill = null;
        saleBill = null;
        newPatient = null;
        searchedPatient = null;
        billItems = null;
        patientTabId = "tabNewPt";
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

    public String toSettle(Bill args) {
        Boolean pharmacyBillingAfterShiftStart = configOptionApplicationController.getBooleanValueByKey("Pharmacy billing can be done after shift start", false);

        if (pharmacyBillingAfterShiftStart) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() == null) {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        }

        if (args.getBillType() == BillType.PharmacyPre && args.getBillClassType() == BillClassType.PreBill) {
            String sql = "Select b from BilledBill b"
                    + " where b.referenceBill=:bil"
                    + " and b.retired=false "
                    + " and b.cancelled=false ";
            HashMap hm = new HashMap();
            hm.put("bil", args);
            Bill b = getBillFacade().findFirstByJpql(sql, hm);

            if (b != null) {
                JsfUtil.addErrorMessage("Allready Paid");
                return "";
            } else {
                setPreBill(args);
                getPreBill().setPaymentMethod(args.getPaymentMethod());
                getPreBill().setPaymentScheme(args.getPaymentScheme());
//                paymentMethod = getPreBill().getPaymentMethod();
                return "/pharmacy/pharmacy_bill_pre_settle?faces-redirect=true";
            }
        } else {
            searchController.makeListNull();
            JsfUtil.addErrorMessage("Please Search Again and Accept Bill");
            return "";
        }
    }

    public Bill getPreBill() {
        if (preBill == null) {
            preBill = new PreBill();
            preBill.setBillType(BillType.PharmacyPre);
            preBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        }
        return preBill;
    }

    public void setPreBill(Bill preBill) {
        makeNull();
        this.preBill = preBill;
        //System.err.println("Setting Bill " + preBill);
        billPreview = false;

    }

    public Bill getSaleBill() {
        if (saleBill == null) {
            saleBill = new BilledBill();
        }
        return saleBill;
    }

    public Bill getSaleReturnBill() {
        if (saleReturnBill == null) {
            saleReturnBill = new RefundBill();
            saleReturnBill.setBillType(BillType.PharmacySale);
            saleReturnBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND);
        }
        return saleReturnBill;
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

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
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

//    public PaymentMethod getPaymentMethod() {
//        return paymentMethod;
//    }
//
//    public void setPaymentMethod(PaymentMethod paymentMethod) {
//        this.paymentMethod = paymentMethod;
//    }
//    public PaymentScheme getPaymentScheme() {
//        return paymentScheme;
//    }
//
//    public void setPaymentScheme(PaymentScheme paymentScheme) {
//        this.paymentScheme = paymentScheme;
//    }
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

}
