/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.membership.MembershipSchemeController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.ComponentDetail;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.StaffBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillFeePayment;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.Payment;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Person;
import com.divudi.entity.PreBill;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.Staff;
import com.divudi.entity.Token;
import com.divudi.entity.WebUser;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillFeePaymentFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.TokenFacade;

import java.io.Serializable;
import java.text.DecimalFormat;
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
public class OpdPreSettleController implements Serializable, ControllerWithMultiplePayments {

    /**
     * Creates a new instance of PharmacySaleController
     */
    public OpdPreSettleController() {
    }

    @Inject
    SessionController sessionController;
    @Inject
    OpdPreBillController opdPreBillController;
    @Inject
    BillController billController;
    @Inject
    PriceMatrixController priceMatrixController;
    @Inject
    MembershipSchemeController membershipSchemeController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    ItemFacade itemFacade;
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
    TokenFacade tokenFacade;
    @EJB
    StaffBean staffBean;
    @EJB
    private StaffFacade staffFacade;
    
/////////////////////////
    Item selectedAlternative;

    private Bill preBill;
    private Bill saleBill;
    private Bill billedBill;
    Bill bill;
    private List<Bill> billsOfBatchBillPre;
    private List<Bill> billsOfBatchBilledBill;
    BillItem billItem;
    BillItem removingBillItem;
    BillItem editingBillItem;
    Double qty;
    Stock stock;
    private Long billID;
    private Bill currentBill;

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
    /////////////////////////
    //   PaymentScheme paymentScheme;
    private PaymentMethodData paymentMethodData;
    private double total;
    PaymentMethod paymentMethod;
    double cashPaid;
    double reminingCashPaid;
    double netTotal;
    double balance;
    Double editingQty;
    private Token token;
    private Staff toStaff;
    private Institution creditCompany;
    private List<Payment> payments;
    private PaymentScheme paymentScheme;
    private boolean foreigner;

    public void makeNull() {
        selectedAlternative = null;
        preBill = null;
        saleBill = null;
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

    public void markAsForeigner() {
        setForeigner(true);
        calculateDiscount();
    }

    public void markAsLocal() {
        setForeigner(false);
        calculateDiscount();
    }

    public void calculateDiscount() {
        double billDiscount = 0.0;
        double billGross = 0.0;
        double billNet = 0.0;

        for (Bill b : billsOfBatchBillPre) {
            double entryGross = 0.0;
            double entryDis = 0.0;
            double entryNet = 0.0;

            List<BillFee> billFeesOfSingleBillOfBatchBillPre;

            if (b.getBillFees() == null || b.getBillFees().isEmpty()) {
                billFeesOfSingleBillOfBatchBillPre = billController.billFeesOfBill(b);
                b.setBillFees(billFeesOfSingleBillOfBatchBillPre);
            }

            for (BillFee bff : b.getBillFees()) {

                Department department = bff.getBillItem().getItem().getDepartment();
                Item item = bff.getBillItem().getItem();
                Category category = null;
                PriceMatrix priceMatrix;

                priceMatrix = priceMatrixController.getPaymentSchemeDiscount(getPreBill().getPaymentMethod(), paymentScheme, department, item);
                getBillBean().setBillFees(bff, isForeigner(), paymentMethod, paymentScheme, getCreditCompany(), priceMatrix);

                bff.setFeeVatPlusValue(bff.getFeeValue() + bff.getFeeVat());
                entryGross += bff.getFeeGrossValue();
                entryNet += bff.getFeeValue();
                entryDis += bff.getFeeDiscount();

//                BillItem bi = bff.getBillItem();
//                billGross += bi.getGrossValue();
//                System.out.println("bi.getGrossValue() = " + bi.getGrossValue());
//                billNet += bi.getNetValue();
//                billDiscount += bi.getDiscount();
//
//                bi.setDiscount(entryDis);
//                bi.setGrossValue(entryGross);
//                bi.setNetValue(entryNet);
            }
            billGross += entryGross;
            billNet += entryNet;
            billDiscount += entryDis;
        }

        getPreBill().setDiscount(billDiscount);
        getPreBill().setTotal(billGross);
        getPreBill().setNetTotal(billNet);

    }

    private double roundOff(double d) {
        DecimalFormat newFormat = new DecimalFormat("#.##");
        try {
            return Double.valueOf(newFormat.format(d));
        } catch (Exception e) {
            return 0;
        }
    }

    public String navigateToScanBills() {
        return "/cashier/scan_bill_by_barcode_scanner?faces-redirect=true";
    }

    public String navigateToSettleOpdPreBills() {
        return "/opd/opd_search_pre_bill?faces-redirect=true";
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
            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
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
        if (getPaymentSchemeController().checkPaymentMethodError(paymentMethod, getPaymentMethodData())) {
            return true;
        }
        if (paymentMethod == PaymentMethod.PatientDeposit) {
            if (!preBill.getPatient().getHasAnAccount()) {
                JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                return true;
            }
            double creditLimitAbsolute = Math.abs(preBill.getPatient().getCreditLimit());
            double runningBalance;
            if (preBill.getPatient().getRunningBalance() != null) {
                runningBalance = preBill.getPatient().getRunningBalance();
            } else {
                runningBalance = 0.0;
            }
            double availableForPurchase = runningBalance + creditLimitAbsolute;

            if (netTotal > availableForPurchase) {
                JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                return true;
            }

        }

        if (paymentMethod == PaymentMethod.Credit) {
            if (creditCompany == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare or credit company or Collecting centre.");
                return true;
            }
        }
        if (toStaff != null && getPaymentMethod() == PaymentMethod.Staff) {
            staffBean.updateStaffCredit(toStaff, netTotal);
            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
        }

        if (paymentMethod == PaymentMethod.Staff_Welfare) {
            if (toStaff == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare.");
                return true;
            }
            if (Math.abs(toStaff.getAnnualWelfareUtilized()) + netTotal > toStaff.getAnnualWelfareQualified()) {
                JsfUtil.addErrorMessage("No enough credit.");
                return true;
            }

        }

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
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
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (paymentSchemeController.checkPaymentMethodError(cd.getPaymentMethod(), cd.getPaymentMethodData())) {
                    return true;
                }
                if (cd.getPaymentMethod().equals(PaymentMethod.PatientDeposit)) {
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

                    if (cd.getPaymentMethodData().getPatient_deposit().getTotalValue() > availableForPurchase) {
                        JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                        return true;
                    }

                }
                if (cd.getPaymentMethod().equals(PaymentMethod.Staff)) {
                    if (cd.getPaymentMethodData().getStaffCredit().getTotalValue() == 0.0 || cd.getPaymentMethodData().getStaffCredit().getToStaff() == null) {
                        JsfUtil.addErrorMessage("Please fill the Paying Amount and Staff Name");
                        return true;
                    }
                    if (cd.getPaymentMethodData().getStaffCredit().getToStaff().getCurrentCreditValue() + cd.getPaymentMethodData().getStaffCredit().getTotalValue() > cd.getPaymentMethodData().getStaffCredit().getToStaff().getCreditLimitQualified()) {
                        JsfUtil.addErrorMessage("No enough Credit.");
                        return true;
                    }
                }
                //TODO - filter only relavant value
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();
            }
            double differenceOfBillTotalAndPaymentValue = netTotal - multiplePaymentMethodTotalValue;
            differenceOfBillTotalAndPaymentValue = Math.abs(differenceOfBillTotalAndPaymentValue);
            if (differenceOfBillTotalAndPaymentValue > 1.0) {
                JsfUtil.addErrorMessage("Mismatch in differences of multiple payment method total and bill total");
                return true;
            }
            if (cashPaid == 0.0) {
                setCashPaid(multiplePaymentMethodTotalValue);
            }

        }

        if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
            if (cashPaid == 0.0) {
                JsfUtil.addErrorMessage("Please enter the paid amount");
                return true;
            }

        }

        return false;
    }

    @Inject
    private BillBeanController billBean;

    public void listnerForPaymentMethodChange() {
        paymentMethod = preBill.getPaymentMethod();
        if (paymentMethod == PaymentMethod.PatientDeposit) {
            if (preBill.getPatient() != null) {
                getPaymentMethodData().getPatient_deposit().setPatient(preBill.getPatient());
            }

            getPaymentMethodData().getPatient_deposit().setTotalValue(netTotal);
        }
        if (paymentMethod == PaymentMethod.Card) {
            getPaymentMethodData().getCreditCard().setTotalValue(netTotal);
        }
        if (!sessionController.getDepartmentPreference().isPartialPaymentOfOpdBillsAllowed()) {
            if (paymentMethod != PaymentMethod.Cash) {
                setCashPaid(preBill.getNetTotal());
            } else {
                setCashPaid(0.00);
            }
        }
    }

    private void saveSettlingBatchBill() {
        getSaleBill().copy(getPreBill());
        getSaleBill().copyValue(getPreBill());
        getSaleBill().setBalance(getSaleBill().getNetTotal());
        getSaleBill().setCashPaid(cashPaid);
        getSaleBill().setBillClassType(BillClassType.BilledBill);
        getSaleBill().setBillType(BillType.OpdBill);
        getSaleBill().setBillTypeAtomic(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);

        getSaleBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getSaleBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getBillBean().setPaymentMethodData(getSaleBill(), getSaleBill().getPaymentMethod(), paymentMethodData);
        getBillBean().setBills(billsOfBatchBillPre);

        getSaleBill().setBillDate(new Date());
        getSaleBill().setBillTime(new Date());
        getSaleBill().setCreatedAt(Calendar.getInstance().getTime());
        getSaleBill().setCreater(getSessionController().getLoggedUser());

        getSaleBill().setReferenceBill(getPreBill());

        getSaleBill().setInsId(getPreBill().getInsId());
        getSaleBill().setDeptId(getPreBill().getDeptId());

        getSaleBill().setSessionId(billNumberBean.generateDailyBillNumberForOpd(getPreBill().getDepartment()));

        if (getSaleBill().getId() == null) {
            getBillFacade().create(getSaleBill());
        }
        getSaleBill().setPayments(createPayment(getSaleBill(), getSaleBill().getPaymentMethod()));
        if (getSaleBill().getId() == null) {
            getBillFacade().create(getSaleBill());
        } else {
            getBillFacade().edit(getSaleBill());
        }

        updateSettledBatchBill();
    }

    public Bill createNewBilledBillfromPreBill(Bill inputPreBill) {
        Bill outputBilledBill = new BilledBill();
        outputBilledBill.copy(inputPreBill);
        outputBilledBill.copyValue(inputPreBill);
        outputBilledBill.setBalance(0.0);
        outputBilledBill.setBillClassType(BillClassType.BilledBill);
        outputBilledBill.setBillType(BillType.OpdBill);
        outputBilledBill.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);

        outputBilledBill.setDepartment(getSessionController().getLoggedUser().getDepartment());
        outputBilledBill.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getBillBean().setBills(billsOfBatchBillPre);

        outputBilledBill.setBillDate(new Date());
        outputBilledBill.setBillTime(new Date());
        outputBilledBill.setCreatedAt(Calendar.getInstance().getTime());
        outputBilledBill.setCreater(getSessionController().getLoggedUser());

        outputBilledBill.setReferenceBill(inputPreBill);

        outputBilledBill.setBackwardReferenceBill(getSaleBill());

        outputBilledBill.setInsId(inputPreBill.getInsId());
        outputBilledBill.setDeptId(inputPreBill.getDeptId());

        if (outputBilledBill.getId() == null) {
            getBillFacade().create(outputBilledBill);
        }

        if (outputBilledBill.getId() == null) {
            getBillFacade().create(outputBilledBill);
        } else {
            getBillFacade().edit(outputBilledBill);
        }
        inputPreBill.setReferenceBill(outputBilledBill);
        getBillFacade().edit(inputPreBill);
        return outputBilledBill;
    }

    public List<Payment> createPayment(Bill bill, PaymentMethod pm) {
        List<Payment> ps = new ArrayList<>();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
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
                        p.setComments(cd.getPaymentMethodData().getCreditCard().getComment());
                        break;
                    case Cheque:
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCheque().getComment());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        break;
                    case ewallet:

                    case Agent:
                    case Credit:
                        p.setReferenceNo(cd.getPaymentMethodData().getCredit().getReferralNo());
                        p.setComments(cd.getPaymentMethodData().getCredit().getComment());
                    case PatientDeposit:
                        if (getSaleBill().getPatient().getRunningBalance() != null) {
                            getSaleBill().getPatient().setRunningBalance(getSaleBill().getPatient().getRunningBalance() - cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                        } else {
                            getSaleBill().getPatient().setRunningBalance(0.0 - cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                        }
                        getPatientFacade().edit(getSaleBill().getPatient());
                    case Slip:
                        p.setPaidValue(cd.getPaymentMethodData().getSlip().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
                        p.setComments(cd.getPaymentMethodData().getSlip().getComment());
                    case OnCall:
                    case OnlineSettlement:
                    case Staff:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffCredit().getToStaff() != null) {
                            staffBean.updateStaffCredit(cd.getPaymentMethodData().getStaffCredit().getToStaff(), cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
                        }
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
                    p.setComments(paymentMethodData.getCreditCard().getComment());
                    break;
                case Cheque:
                    p.setChequeDate(paymentMethodData.getCheque().getDate());
                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
                    p.setPaidValue(paymentMethodData.getCheque().getTotalValue());
                    p.setComments(paymentMethodData.getCheque().getComment());
                    break;
                case Cash:
                    p.setPaidValue(paymentMethodData.getCash().getTotalValue());
                    break;
                case ewallet:

                case Agent:
                case Credit:
                    p.setReferenceNo(paymentMethodData.getCredit().getReferralNo());
                    p.setComments(paymentMethodData.getCredit().getComment());
                case PatientDeposit:
                case Slip:
                    p.setBank(paymentMethodData.getSlip().getInstitution());
                    p.setPaidValue(paymentMethodData.getSlip().getTotalValue());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
                    p.setComments(paymentMethodData.getSlip().getComment());
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

    private Bill saveSettlingIndividualBill(Bill individualBill, Bill settlingBatchBill) {
        Bill savingSettlingIndividualBill = new Bill();
        savingSettlingIndividualBill.copy(individualBill);
        savingSettlingIndividualBill.copyValue(individualBill);
        savingSettlingIndividualBill.setBalance(individualBill.getNetTotal());
        savingSettlingIndividualBill.setCashPaid(cashPaid);
        savingSettlingIndividualBill.setBillClassType(BillClassType.BilledBill);
        savingSettlingIndividualBill.setBillType(BillType.OpdBill);
        savingSettlingIndividualBill.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        savingSettlingIndividualBill.setDepartment(getSessionController().getLoggedUser().getDepartment());
        savingSettlingIndividualBill.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        savingSettlingIndividualBill.setBillDate(new Date());
        savingSettlingIndividualBill.setBillTime(new Date());
        savingSettlingIndividualBill.setCreatedAt(Calendar.getInstance().getTime());
        savingSettlingIndividualBill.setCreater(getSessionController().getLoggedUser());
        savingSettlingIndividualBill.setReferenceBill(individualBill);
        savingSettlingIndividualBill.setInsId(individualBill.getInsId());
        savingSettlingIndividualBill.setDeptId(individualBill.getDeptId());

        if (savingSettlingIndividualBill.getId() == null) {
            getBillFacade().create(savingSettlingIndividualBill);
        } else {
            getBillFacade().edit(savingSettlingIndividualBill);
        }

        individualBill.setReferenceBill(savingSettlingIndividualBill);
        getBillFacade().edit(individualBill);

        return savingSettlingIndividualBill;
    }

    private void updateSettledBatchBill() {
        getPreBill().setReferenceBill(getSaleBill());
        getBillFacade().edit(getPreBill());

    }

    private void saveIndividualBilledBillsOfPreBatchBill() {
        billsOfBatchBilledBill = new ArrayList<>();
        for (Bill individualBillsOfPreBatchBill : billController.billsOfBatchBill(preBill)) {

            Bill newlyCreatedIndividualBilledBillOfBilledBatchBill = createNewBilledBillfromPreBill(individualBillsOfPreBatchBill);

            for (BillItem tbi : individualBillsOfPreBatchBill.getBillItems()) {
                BillItem newBillItem = new BillItem();
                newBillItem.copy(tbi);
                newBillItem.setBill(newlyCreatedIndividualBilledBillOfBilledBatchBill);
                newBillItem.setCreatedAt(Calendar.getInstance().getTime());
                newBillItem.setCreater(getSessionController().getLoggedUser());
                if (newBillItem.getId() == null) {
                    getBillItemFacade().create(newBillItem);
                } else {
                    getBillItemFacade().edit(newBillItem);
                }
                String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + tbi.getId();
                List<BillFee> preBillItemFees = getBillFeeFacade().findByJpql(sql);
                List<BillFee> newBillBillItemFees = createNewBillItemFeesForBilledBillFromPreBillItem(preBillItemFees, newBillItem);
                newBillItem.setBillFees(newBillBillItemFees);
                newlyCreatedIndividualBilledBillOfBilledBatchBill.getBillItems().add(newBillItem);
                newlyCreatedIndividualBilledBillOfBilledBatchBill.getBillFees().addAll(newBillBillItemFees);

            }

            newlyCreatedIndividualBilledBillOfBilledBatchBill.setPaymentMethod(getSaleBill().getPaymentMethod());

            getBillBean().setPaymentMethodData(newlyCreatedIndividualBilledBillOfBilledBatchBill, getPreBill().getPaymentMethod(), paymentMethodData);

            getBillFacade().edit(newlyCreatedIndividualBilledBillOfBilledBatchBill);
            getBillFacade().edit(preBill);
            billsOfBatchBilledBill.add(newlyCreatedIndividualBilledBillOfBilledBatchBill);
        }
    }

    private void saveOpdIndividualBillItems(Bill individualPreBill, Bill newlyCreatedSettlingIndividualBill) {
        for (BillItem tbi : individualPreBill.getBillItems()) {
            BillItem newSettlingBillItem = new BillItem();
            newSettlingBillItem.copy(tbi);
            newSettlingBillItem.setBill(newlyCreatedSettlingIndividualBill);
            newSettlingBillItem.setCreatedAt(Calendar.getInstance().getTime());
            newSettlingBillItem.setCreater(getSessionController().getLoggedUser());
            if (newSettlingBillItem.getId() == null) {
                getBillItemFacade().create(newSettlingBillItem);
            } else {
                getBillItemFacade().edit(newSettlingBillItem);
            }
            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + tbi.getId();
            List<BillFee> originalBillFees = getBillFeeFacade().findByJpql(sql);
            saveBillFee(originalBillFees, newSettlingBillItem);
            newlyCreatedSettlingIndividualBill.getBillItems().add(newSettlingBillItem);
        }
        getBillFacade().edit(individualPreBill);

    }

    public void saveBillFee(List<BillFee> bfs, BillItem bi) {
        for (BillFee bfo : bfs) {
            BillFee bf = new BillFee();
            bf.copy(bfo);
            bf.setCreatedAt(Calendar.getInstance().getTime());
            bf.setCreater(sessionController.getLoggedUser());
            bf.setBillItem(bi);
            bf.setBill(bi.getBill());

            if (bf.getId() == null) {
                getBillFeeFacade().create(bf);
            }
        }

    }

    public List<BillFee> createNewBillItemFeesForBilledBillFromPreBillItem(List<BillFee> preBillFees, BillItem newBilledBillItem) {
        List<BillFee> nbfs = new ArrayList<>();
        for (BillFee bfo : preBillFees) {
            BillFee nbf = new BillFee();
            nbf.copy(bfo);
            nbf.setCreatedAt(Calendar.getInstance().getTime());
            nbf.setCreater(sessionController.getLoggedUser());
            nbf.setBillItem(newBilledBillItem);
            nbf.setBill(newBilledBillItem.getBill());

            if (nbf.getId() == null) {
                getBillFeeFacade().create(nbf);
            }
            nbfs.add(nbf);
        }
        return nbfs;
    }

    public void settleBillWithPay2() {
        editingQty = null;
        if (errorCheckForSaleBill()) {
            return;
        }

        if (getCashPaid() < getPreBill().getNetTotal()) {
            JsfUtil.addErrorMessage("Tendered Amount is lower than Total");
            return;
        }
        saveSettlingBatchBill();
        saveIndividualBilledBillsOfPreBatchBill();

//        getPreBill().getCashBillsPre().add(getSaleBill());
        getBillFacade().edit(getPreBill());

        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(getSaleBill(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        setBill(getBillFacade().find(getSaleBill().getId()));

//        makeNull();
        billPreview = true;

    }

    private boolean paymentMethodDataErrorCheck() {
        if (getPaymentMethod() == PaymentMethod.Card) {
            if (getPaymentMethodData().getCreditCard().getNo().trim().equalsIgnoreCase("")) {
                JsfUtil.addErrorMessage("Add CreditCard No");
                return true;
            } else if (getPaymentMethodData().getCreditCard().getInstitution() == null) {
                JsfUtil.addErrorMessage("Select Card Bank");
                return true;
            } else if (getPaymentMethodData().getCreditCard().getComment().trim().equalsIgnoreCase("") && configOptionApplicationController.getBooleanValueByKey("Billing for Cashier - CreditCard Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Add Comment");
                return true;
            }
            return false;
        } else if (getPaymentMethod() == PaymentMethod.Cheque) {
            if (getPaymentMethodData().getCheque().getNo().trim().equalsIgnoreCase("")) {
                JsfUtil.addErrorMessage("Add Cheque No");
                return true;
            } else if (getPaymentMethodData().getCheque().getInstitution() == null) {
                JsfUtil.addErrorMessage("Select Cheque Bank");
                return true;
            } else if (getPaymentMethodData().getCheque().getDate() == null) {
                JsfUtil.addErrorMessage("Add Cheque Date");
                return true;
            } else if (getPaymentMethodData().getCheque().getComment().trim().equalsIgnoreCase("") && configOptionApplicationController.getBooleanValueByKey("Billing for Cashier - Cheque Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Add Comment");
                return true;
            }
            return false;
        } else if (getPaymentMethod() == PaymentMethod.Slip) {
            if (getPaymentMethodData().getSlip().getTotalValue() <= 0.0) {
                JsfUtil.addErrorMessage("Add Slip Amount");
                return true;
            } else if (getPaymentMethodData().getSlip().getInstitution() == null) {
                JsfUtil.addErrorMessage("Select Slip Bank");
                return true;
            } else if (getPaymentMethodData().getSlip().getDate() == null) {
                JsfUtil.addErrorMessage("Add Slip Date");
                return true;
            } else if (getPaymentMethodData().getSlip().getComment().trim().equalsIgnoreCase("") && configOptionApplicationController.getBooleanValueByKey("Billing for Cashier - Slip Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Add Comment");
                return true;
            }
            return false;
        } else if (getPaymentMethod() == PaymentMethod.ewallet) {
            if (getPaymentMethodData().getEwallet().getInstitution() == null) {
                JsfUtil.addErrorMessage("Select Ewallet Institution");
                return true;
            } else if (getPaymentMethodData().getEwallet().getNo() == null) {
                JsfUtil.addErrorMessage("Add Ewallet No");
                return true;
            } else if (getPaymentMethodData().getEwallet().getComment().trim().equalsIgnoreCase("") && configOptionApplicationController.getBooleanValueByKey("Billing for Cashier - Ewallet Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Add Comment");
                return true;
            }
            return false;
        } else if (getPaymentMethod() == PaymentMethod.Credit) {
            if (getCreditCompany() == null) {
                JsfUtil.addErrorMessage("Select Credit Company");
                return true;
            } else if (getPaymentMethodData().getCredit().getReferralNo().equalsIgnoreCase("") && configOptionApplicationController.getBooleanValueByKey("Billing for Cashier - Credit Policy No is Mandatory", false)) {
                JsfUtil.addErrorMessage("Add Policy No");
                return true;
            }
            return false;
        } else if (getPaymentMethod() == PaymentMethod.PatientDeposit) {
            if (!preBill.getPatient().getHasAnAccount()) {
                JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                return true;
            }
            double creditLimitAbsolute = Math.abs(preBill.getPatient().getCreditLimit());
            double runningBalance;
            if (preBill.getPatient().getRunningBalance() != null) {
                runningBalance = preBill.getPatient().getRunningBalance();
            } else {
                runningBalance = 0.0;
            }
            double availableForPurchase = runningBalance + creditLimitAbsolute;

            if (netTotal > availableForPurchase) {
                JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                return true;
            }
            return false;
        } else if (toStaff != null && getPaymentMethod() == PaymentMethod.Staff) {
            if (toStaff == null) {
                JsfUtil.addSuccessMessage("Select the Staff");
                return true;
            }
            staffBean.updateStaffCredit(toStaff, netTotal);
            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
            return false;
        } else if (paymentMethod == PaymentMethod.Staff_Welfare) {
            if (toStaff == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare.");
                return true;
            }
            if (Math.abs(toStaff.getAnnualWelfareUtilized()) + netTotal > toStaff.getAnnualWelfareQualified()) {
                JsfUtil.addErrorMessage("No enough credit.");
                return true;
            }
            return false;
        } else if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
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
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (paymentSchemeController.checkPaymentMethodError(cd.getPaymentMethod(), cd.getPaymentMethodData())) {
                    return true;
                }
                if (cd.getPaymentMethod().equals(PaymentMethod.PatientDeposit)) {
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

                    if (cd.getPaymentMethodData().getPatient_deposit().getTotalValue() > availableForPurchase) {
                        JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                        return true;
                    }

                }
                if (cd.getPaymentMethod().equals(PaymentMethod.Staff)) {
                    if (cd.getPaymentMethodData().getStaffCredit().getTotalValue() == 0.0 || cd.getPaymentMethodData().getStaffCredit().getToStaff() == null) {
                        JsfUtil.addErrorMessage("Please fill the Paying Amount and Staff Name");
                        return true;
                    }
                    if (cd.getPaymentMethodData().getStaffCredit().getToStaff().getCurrentCreditValue() + cd.getPaymentMethodData().getStaffCredit().getTotalValue() > cd.getPaymentMethodData().getStaffCredit().getToStaff().getCreditLimitQualified()) {
                        JsfUtil.addErrorMessage("No enough Credit.");
                        return true;
                    }
                }
                //TODO - filter only relavant value
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();
            }
            double differenceOfBillTotalAndPaymentValue = netTotal - multiplePaymentMethodTotalValue;
            differenceOfBillTotalAndPaymentValue = Math.abs(differenceOfBillTotalAndPaymentValue);
            if (differenceOfBillTotalAndPaymentValue > 1.0) {
                JsfUtil.addErrorMessage("Mismatch in differences of multiple payment method total and bill total");
                return true;
            }
            if (cashPaid == 0.0) {
                setCashPaid(multiplePaymentMethodTotalValue);
            }

        }
        return false;
    }

    public void settleOpdBatchBill() {
        editingQty = null;
        if (paymentMethodDataErrorCheck()) {
            return;
        }

        if (getPaymentMethod() == paymentMethod.Cash) {
            if (getCashPaid() < getPreBill().getNetTotal()) {
                JsfUtil.addErrorMessage("Tendered Amount is lower than Total");
                return;
            }
        }

        saveSettlingBatchBill();
        saveIndividualBilledBillsOfPreBatchBill();

        getBillFacade().edit(getPreBill());
        setBill(getBillFacade().find(getSaleBill().getId()));

        billPreview = true;
        completeTokenAfterAcceptPayment();
    }

    public void completeTokenAfterAcceptPayment() {
        if (token == null) {
            return;
        }
        token.setCompleted(true);
        token.setCompletedAt(new Date());
        tokenFacade.edit(token);

    }

    public void createPaymentsForCashierAcceptpayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);
        p.setPaidValue(p.getBill().getNetTotal());
        if (p.getId() == null) {
            getPaymentFacade().create(p);
        } else {
            getPaymentFacade().edit(p);
        }

    }

    public double calculatRemainForMultiplePaymentTotal() {
        total = getPreBill().getNetTotal();
        if (getPreBill().getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();
            }
            return total - multiplePaymentMethodTotalValue;
        }
        return total;
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
                if (preBill.getPatient() != null) {
                    pm.getPaymentMethodData().getPatient_deposit().setPatient(preBill.getPatient());
                }
                pm.getPaymentMethodData().getPatient_deposit().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Credit) {
                pm.getPaymentMethodData().getCredit().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Staff) {
                pm.getPaymentMethodData().getStaffCredit().setTotalValue(remainAmount);
            }

        }
    }

    public BilledBill createIndividualBilledBillFormIndividualPreBill(Bill preBill) {
        setPreBill(preBill);
//        if (errorCheckForSaleBill()) {
////            return;
//        }

        // saveSettlingBatchBill();
        // saveOpdBillsOfBatchBill();
        return (BilledBill) getSaleBill();
    }

    public BilledBill createBilledBillForPreBillOld(Bill preBill) {
        setPreBill(preBill);
//        if (errorCheckForSaleBill()) {
////            return;
//        }

        // saveSettlingBatchBill();
        // saveOpdBillsOfBatchBill();
        return (BilledBill) getSaleBill();
    }

    public BilledBill createIndividualBilledBillForIndividualPreBill(Bill preBill) {
        setPreBill(preBill);
        saveIndividualBilledBillsOfPreBatchBill();
        return (BilledBill) getSaleBill();
    }

    @EJB
    CashTransactionBean cashTransactionBean;

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
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

    public String toSettle(Bill preBatchBill) {
        String sql = "Select b from BilledBill b"
                + " where b.referenceBill=:bil"
                + " and b.retired=false "
                + " and b.cancelled=false ";
        HashMap hm = new HashMap();
        hm.put("bil", preBatchBill);
        Bill b = getBillFacade().findFirstByJpql(sql, hm);
        if (b != null) {
            JsfUtil.addErrorMessage("Already Paid");
            return "";
        }
        setPreBill(preBatchBill);
        billsOfBatchBillPre = billController.billsOfBatchBill(preBatchBill);
        for (Bill billOfBatchBillPre : billsOfBatchBillPre) {
            if (billOfBatchBillPre.getBillItems() == null) {
                billOfBatchBillPre.setBillItems(billController.billItemsOfBill(billOfBatchBillPre));
                for (BillItem billItemOfPreBill : billOfBatchBillPre.getBillItems()) {
                    if (billItemOfPreBill.getBillFees() == null) {
                        billItemOfPreBill.setBillFees(billController.billFeesOfBillItem(billItemOfPreBill));
                    }
                }
            }
            if (billOfBatchBillPre.getBillFees() == null) {
                billOfBatchBillPre.setBillFees(billController.billFeesOfBill(billOfBatchBillPre));
            }
        }
        getPreBill().setPaymentMethod(null);
        paymentScheme = new PaymentScheme();
        creditCompany = null;
        toStaff = null;
        //fillPaymentMethodDetails();
        netTotal = getPreBill().getNetTotal();
        //calculateDiscount();
        return "/opd/opd_bill_pre_settle?faces-redirect=true";

    }

    public void fillPaymentMethodDetails() {
        if (getPreBill().getPaymentMethod() == PaymentMethod.PatientDeposit) {
            if (preBill.getPatient() != null) {
                getPaymentMethodData().getPatient_deposit().setPatient(preBill.getPatient());
            }

            getPaymentMethodData().getPatient_deposit().setTotalValue(preBill.getNetTotal());
        }
        if (getPreBill().getPaymentMethod() == PaymentMethod.Card) {
            getPaymentMethodData().getCreditCard().setTotalValue(preBill.getNetTotal());
        }
        if (!sessionController.getDepartmentPreference().isPartialPaymentOfOpdBillsAllowed()) {
            if (getPreBill().getPaymentMethod() != PaymentMethod.Cash) {
                setCashPaid(preBill.getNetTotal());
            }
        }
        paymentMethod = getPreBill().getPaymentMethod();
    }

    public String toSettleBatch(Bill preBatchBill) {
        setPreBill(preBatchBill);
//        if (getPreBill().getReferenceBill() == null) {
//            //billed bill create for pre bills
//            BilledBill tmp = createBatchBilledBill(getPreBill());
//
//            for (Bill pb : getPreBill().getForwardReferenceBills()) {
//                //create BilledBills For PreBills
//                BilledBill bb = createIndividualBilledBillFormIndividualPreBill(pb);
//                bb.setBackwardReferenceBill(tmp);
//                //// // System.out.println("bb.getCashPaid = " + bb.getCashPaid());
//                getBillFacade().edit(bb);
//                tmp.getForwardReferenceBills().add(bb);
//            }
//            //// // System.out.println("tmp.getCashPaid = " + tmp.getCashPaid());
//            tmp.setBalance(tmp.getNetTotal());
//            getBillFacade().edit(tmp);
//            //set batch billed bill
//            setBilledBill(tmp);
//        } else {
//            //billed bills alredy saved
//            setBilledBill(getPreBill().getReferenceBill());
//        }

        return "/opd_bill_batch_pre_settle?faces-redirect=true";
    }

    @Deprecated
    public String toSettleBatchOldMetgod(Bill preBatchBill) {
        setPreBill(preBatchBill);
        if (getPreBill().getReferenceBill() == null) {
            //billed bill create for pre bills
            BilledBill tmp = createBatchBilledBill(getPreBill());

            for (Bill pb : getPreBill().getForwardReferenceBills()) {
                //create BilledBills For PreBills
                BilledBill bb = createIndividualBilledBillFormIndividualPreBill(pb);
                bb.setBackwardReferenceBill(tmp);
                //// // System.out.println("bb.getCashPaid = " + bb.getCashPaid());
                getBillFacade().edit(bb);
                tmp.getForwardReferenceBills().add(bb);
            }
            //// // System.out.println("tmp.getCashPaid = " + tmp.getCashPaid());
            tmp.setBalance(tmp.getNetTotal());
            getBillFacade().edit(tmp);
            //set batch billed bill
            setBilledBill(tmp);
        } else {
            //billed bills alredy saved
            setBilledBill(getPreBill().getReferenceBill());
        }

        return "/opd_bill_batch_pre_settle?faces-redirect=true";
    }

    public String settle() {

        if (cashPaid < 1) {
            JsfUtil.addErrorMessage("Please enter a valid amount");
            return "";
        }

        if (errorCheck()) {
            return "";
        }

        String str = settleBatchBillAfterFiistTime();
        cashPaid = 0.0;

        return str;
    }

    public boolean errorCheck() {
        if (cashPaid == 0.0 && getSessionController().getApplicationPreference().isPartialPaymentOfOpdPreBillsAllowed()) {
            JsfUtil.addErrorMessage("Please Enter Correct Amount");
            return true;
        }
        if (getBilledBill() == null) {
            JsfUtil.addErrorMessage("Nothing To Pay");
            return true;
        }
        return false;
    }

//    public void settleBatchBillFiistTime(Bill bill) {
//        //create Batch BilledBill
//        BilledBill tmp = createBatchBilledBill(bill);
//
//        double dbl = 0;
//        double pid = 0;
//        reminingCashPaid = cashPaid;
//        Payment p = new Payment();
//        p.setBill(tmp);
//        setPaymentMethodData(p, paymentMethod, paymentMethodData);
//        for (Bill b : bill.getForwardReferenceBills()) {
//            //create BilledBills For PreBills
//            BilledBill bb = createIndividualBilledBillFormIndividualPreBill(b);
//            bb.setBackwardReferenceBill(tmp);
//
//            //// // System.out.println("dbl = " + dbl);
//            //// // System.out.println("reminingCashPaid = " + reminingCashPaid);
//            //// // System.out.println("cashPaid = " + cashPaid);
//
//            for (BillItem bi : bb.getBillItems()) {
//
//                //// // System.out.println("bi = " + bi);
//                String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
//
//                List<BillFee> billFees = getBillFeeFacade().findByJpql(sql);
//                //// // System.out.println("billFees = " + billFees.size());
//                //for payments for billfees
//
//                calculateBillfeePayments(billFees, p);
//            }
//
//            bb.setCashPaid(calBillPaidValue(bb));
//            bb.setBalance(bb.getNetTotal() - bb.getCashPaid());
//            getBillFacade().edit(bb);
//            //for update Batch Bill
//            dbl += bb.getNetTotal();
//            pid += bb.getCashPaid();
//
//            tmp.getForwardReferenceBills().add(bb);
//        }
//
//        tmp.setCashPaid(pid);
//        tmp.setBalance(dbl - pid);
//        tmp.setNetTotal(dbl);
//        getBillFacade().edit(tmp);
//        JsfUtil.addSuccessMessage("Sucessfully Paid");
//    }
    public String settleBatchBillAfterFiistTime() {

        double dbl = 0;
        double pid = 0;
        reminingCashPaid = cashPaid;
        Payment p = new Payment();
        p.setBill(getBilledBill());
        setPaymentMethodData(p, paymentMethod, paymentMethodData);

        for (Bill b : getBilledBill().getForwardReferenceBills()) {
            //// // System.out.println("dbl = " + dbl);
            if (b.isCancelled()) {
                if (getBilledBill().getForwardReferenceBills().size() == 1) {

                    JsfUtil.addErrorMessage("Can't Pay,This Bill Cancelled");

                } else {
                    JsfUtil.addErrorMessage("Some Bill cancelled This Batch Bill");
                }
                continue;
            }

            if ((reminingCashPaid != 0.0) || !getSessionController().getApplicationPreference().isPartialPaymentOfOpdPreBillsAllowed()) {
                for (BillItem bi : b.getBillItems()) {

                    String sql = "SELECT bi FROM BillItem bi where bi.retired=false and bi.referanceBillItem.id=" + bi.getId();
                    BillItem rbi = getBillItemFacade().findFirstByJpql(sql);

                    if (rbi != null) {
                        JsfUtil.addErrorMessage("Some Bill Item Already Refunded");
                        continue;
                    }

                    sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();

                    List<BillFee> billFees = getBillFeeFacade().findByJpql(sql);

                    calculateBillfeePayments(billFees, p);
                }
            }

            b.setCashPaid(calBillPaidValue(b));
            b.setBalance(b.getNetTotal() - b.getCashPaid());
            getBillFacade().edit(b);
            dbl += b.getNetTotal();
            pid += b.getCashPaid();
        }

        getBilledBill().setCashPaid(pid);
        getBilledBill().setBalance(dbl - pid);
        getBilledBill().setNetTotal(dbl);
        getBillFacade().edit(getBilledBill());
        if (getBilledBill().getCashPaid() >= getBilledBill().getNetTotal()) {
            getOpdPreBillController().setBills(getBilledBill().getForwardReferenceBills());
            JsfUtil.addSuccessMessage("Sucessfully Fully Paid");
            return "/bill_print?faces-redirect=true";
        } else {
            JsfUtil.addSuccessMessage("Sucessfully Paid");
            getOpdPreBillController().setBills(getBilledBill().getForwardReferenceBills());
            return "/bill_print_advance?faces-redirect=true";
        }
    }

    public BilledBill createBatchBilledBill(Bill b) {
        BilledBill tmp = new BilledBill();
        tmp.copy(b);
        tmp.copyValue(b);
        tmp.setReferenceBill(b);
        tmp.setBillType(BillType.OpdBathcBill);
        tmp.setBillTypeAtomic(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        tmp.setBillClassType(BillClassType.BilledBill);
        tmp.setInstitution(getSessionController().getInstitution());
        tmp.setDepartment(getSessionController().getDepartment());
        tmp.setPaymentScheme(b.getPaymentScheme());
        tmp.setPaymentMethod(b.getPaymentMethod());
        tmp.setCreatedAt(new Date());
        tmp.setCreater(getSessionController().getLoggedUser());
        //Institution ID (INS ID)
        String insId = getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), tmp.getBillType(), tmp.getBillClassType(), BillNumberSuffix.NONE);
        tmp.setInsId(insId);

        //Department ID (DEPT ID)
        String deptId = getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getSessionController().getDepartment(), tmp.getBillType(), tmp.getBillClassType());
        tmp.setDeptId(deptId);

        getBillFacade().create(tmp);
        b.setReferenceBill(tmp);
        getBillFacade().edit(b);

        return tmp;
    }

    public void setPaymentMethodData(Payment p, PaymentMethod paymentMethod, PaymentMethodData paymentMethodData) {

        if (paymentMethod.equals(PaymentMethod.Cheque)) {
            p.setBank(paymentMethodData.getCheque().getInstitution());
            p.setChequeRefNo(paymentMethodData.getCheque().getNo());
            p.setChequeDate(paymentMethodData.getCheque().getDate());

        }
        if (paymentMethod.equals(PaymentMethod.Slip)) {
            p.setBank(paymentMethodData.getSlip().getInstitution());
            p.setChequeDate(paymentMethodData.getSlip().getDate());
            p.setComments(paymentMethodData.getSlip().getComment());
        }

        if (paymentMethod.equals(PaymentMethod.Card)) {
            p.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
            p.setBank(paymentMethodData.getCreditCard().getInstitution());
        }

        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(paymentMethod);

        if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdPreBillsAllowed()) {
            if (cashPaid < getBilledBill().getBalance()) {
                p.setPaidValue(cashPaid);
            } else {
                p.setPaidValue(getBilledBill().getBalance());
            }

        } else {
            p.setPaidValue(getBilledBill().getNetTotal());
        }

        if (p.getId() == null) {
            getPaymentFacade().create(p);
        }

    }

    public void setPaymentMethodData(Payment p, PaymentMethod pm) {
        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);
        if (p.getBill().getBillType() == BillType.PaymentBill) {
            p.setPaidValue(p.getBill().getNetTotal());
        } else {
            p.setPaidValue(p.getBill().getCashPaid());
        }

        if (p.getId() == null) {
            getPaymentFacade().create(p);
        }
        getPaymentFacade().edit(p);
    }

    public void setBillFeePaymentAndPayment(double amount, BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(amount);
        bfp.setInstitution(bf.getBillItem().getItem().getInstitution());
        bfp.setDepartment(bf.getBillItem().getItem().getDepartment());
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        getBillFeePaymentFacade().create(bfp);
    }

    public void setBillFeePaymentAndPayment(BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(bf.getSettleValue());
        if (bf.getBillItem() != null && bf.getBillItem().getItem() != null) {
            bfp.setInstitution(bf.getBillItem().getItem().getInstitution());
            bfp.setDepartment(bf.getBillItem().getItem().getDepartment());
        } else {
            bfp.setInstitution(getSessionController().getInstitution());
            bfp.setDepartment(getSessionController().getDepartment());
        }
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        getBillFeePaymentFacade().create(bfp);
    }

    public double calBillPaidValue(Bill b) {
        String sql;

        sql = "select sum(bfp.amount) from BillFeePayment bfp where "
                + " bfp.retired=false "
                + " and bfp.billFee.bill.id=" + b.getId();

        double d = getBillFeePaymentFacade().findDoubleByJpql(sql);

        return d;
    }

    public void calculateBillfeePayments(List<BillFee> billFees, Payment p) {
        for (BillFee bf : billFees) {

            if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdPreBillsAllowed()) {
                if (Math.abs((bf.getFeeValue() - bf.getSettleValue())) > 0.1) {
                    if (reminingCashPaid >= (bf.getFeeValue() - bf.getSettleValue())) {
                        //// // System.out.println("In If reminingCashPaid = " + reminingCashPaid);
                        //// // System.out.println("bf.getPaidValue() = " + bf.getSettleValue());
                        double d = (bf.getFeeValue() - bf.getSettleValue());
                        bf.setSettleValue(bf.getFeeValue());
                        setBillFeePaymentAndPayment(d, bf, p);
                        getBillFeeFacade().edit(bf);
                        reminingCashPaid -= d;
                    } else {
                        bf.setSettleValue(bf.getSettleValue() + reminingCashPaid);
                        setBillFeePaymentAndPayment(reminingCashPaid, bf, p);
                        getBillFeeFacade().edit(bf);
                        reminingCashPaid = 0.0;
                    }
                }
            } else {
                bf.setSettleValue(bf.getFeeValue());
                setBillFeePaymentAndPayment(bf.getFeeValue(), bf, p);
                getBillFeeFacade().edit(bf);
            }
        }
    }

    public void calculateBillfeePaymentsForCancelRefundBill(List<BillFee> billFees, Payment p) {
        for (BillFee bf : billFees) {
            setBillFeePaymentAndPayment(bf, p);
        }
    }

    public void createOpdCancelRefundBillFeePayment(Bill bill, List<BillFee> billFees, Payment p) {
        calculateBillfeePaymentsForCancelRefundBill(billFees, p);

        //JsfUtil.addSuccessMessage("Sucessfully Paid");
    }

    public Payment createPaymentForCancellationsforOPDBill(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        double valueToSet = 0 - Math.abs(bill.getNetTotal());
        p.setPaidValue(valueToSet);

        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);
        
        //System.out.println("pm = " + pm);

        if (pm == PaymentMethod.PatientDeposit) {
            //System.out.println("Before Balance = " + bill.getPatient().getRunningBalance());
            if (bill.getPatient().getRunningBalance() == null) {
                //System.out.println("Null");
                bill.getPatient().setRunningBalance(Math.abs(bill.getNetTotal()));
            } else {
                //System.out.println("Not Null - Add BillValue");
                bill.getPatient().setRunningBalance(bill.getPatient().getRunningBalance() + Math.abs(bill.getNetTotal()));
            }
            patientFacade.edit(bill.getPatient());
            //System.out.println("After Balance = " + bill.getPatient().getRunningBalance());
        }
        
        if (pm == PaymentMethod.Staff) {
            //System.out.println("PaymentMethod - Staff");
            //System.out.println("Before Balance = " + bill.getToStaff().getCurrentCreditValue());
            bill.getToStaff().setCurrentCreditValue( bill.getToStaff().getCurrentCreditValue() - Math.abs(bill.getNetTotal()));
            //System.out.println("After Balance = " + bill.getToStaff().getCurrentCreditValue());
            staffFacade.edit(bill.getToStaff());
            //System.out.println("Staff Credit Updated");
        }
        
        //System.out.println("001");  
        if (p.getId() == null) { 
            getPaymentFacade().create(p);
        }
        getPaymentFacade().edit(p);
        //System.out.println("End Payment");  
        return p;
    }

    public Payment createPaymentForCancellationsAndRefunds(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        double valueToSet = 0 - Math.abs(bill.getNetTotal());
        p.setPaidValue(valueToSet);
        setPaymentMethodData(p, pm);
        return p;
    }

    public Bill getPreBill() {
        if (preBill == null) {
            preBill = new PreBill();
        }
        return preBill;
    }

    public void setPreBill(Bill preBill) {
        makeNull();
        this.preBill = preBill;
        billPreview = false;
    }

    public Bill getSaleBill() {
        if (saleBill == null) {
            saleBill = new BilledBill();
            //  saleBill.setBillType(BillType.PharmacySale);
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
        balance = cashPaid - netTotal;
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

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
    }

    public Bill getBilledBill() {
        if (billedBill == null) {
            billedBill = new BilledBill();
        }
        return billedBill;
    }

    public void setBilledBill(Bill billedBill) {
        this.billedBill = billedBill;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public OpdPreBillController getOpdPreBillController() {
        return opdPreBillController;
    }

    public void setOpdPreBillController(OpdPreBillController opdPreBillController) {
        this.opdPreBillController = opdPreBillController;
    }

    public Bill getCurrentBill() {
        return currentBill;
    }

    public void setCurrentBill(Bill currentBill) {
        this.currentBill = currentBill;
    }

    public Long getBillID() {
        return billID;
    }

    public void setBillID(Long billID) {
        this.billID = billID;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public List<Bill> getBillsOfBatchBillPre() {
        return billsOfBatchBillPre;
    }

    public void setBillsOfBatchBillPre(List<Bill> billsOfBatchBillPre) {
        this.billsOfBatchBillPre = billsOfBatchBillPre;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public List<Bill> getBillsOfBatchBilledBill() {
        return billsOfBatchBilledBill;
    }

    public void setBillsOfBatchBilledBill(List<Bill> billsOfBatchBilledBill) {
        this.billsOfBatchBilledBill = billsOfBatchBilledBill;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public boolean isForeigner() {
        return foreigner;
    }

    public void setForeigner(boolean foreigner) {
        this.foreigner = foreigner;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

}
