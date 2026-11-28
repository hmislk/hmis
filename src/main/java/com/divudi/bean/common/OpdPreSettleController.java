/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.membership.MembershipSchemeController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.service.StaffService;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.Token;
import com.divudi.core.entity.WebUser;
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
import com.divudi.core.facade.StaffFacade;
import com.divudi.core.facade.TokenFacade;
import com.divudi.service.BillService;
import com.divudi.service.opd.OpdPreSettleException;
import com.divudi.service.opd.OpdPreSettlePaymentRows;
import com.divudi.service.opd.OpdPreSettleRequest;
import com.divudi.service.opd.OpdPreSettleWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
    @Inject
    DrawerController drawerController;

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
    StaffService staffBean;
    @EJB
    private StaffFacade staffFacade;
    @EJB
    BillService billService;
    @EJB
    OpdPreSettleWriter opdPreSettleWriter;
    @Inject
    PatientDepositController patientDepositController;

    private static final Logger LOGGER = Logger.getLogger(OpdPreSettleController.class.getName());

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
    private final AtomicBoolean billSettlingStarted = new AtomicBoolean(false);
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

    /**
     * What the patient can pay with Patient Deposit: the balance of the patient's
     * deposit record (the one the Patient Deposit receipt raises) plus the credit
     * limit, as the normal OPD bill checks it (#26260).
     */
    private double depositAvailableForPurchase(Patient patient) {
        PatientDeposit pd = patientDepositController.checkDepositOfThePatient(patient, getSessionController().getDepartment());
        double balance = (pd == null || pd.getBalance() == null) ? 0.0 : pd.getBalance();
        double creditLimitAbsolute = patient.getCreditLimit() == null ? 0.0 : Math.abs(patient.getCreditLimit());
        return balance + creditLimitAbsolute;
    }

    private boolean errorCheckForSaleBill() {
        if (getPaymentSchemeController().checkPaymentMethodError(paymentMethod, getPaymentMethodData())) {
            return true;
        }
        if (paymentMethod == PaymentMethod.PatientDeposit) {
            if (!preBill.getPatient().getHasAnAccount()) {
                JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                return true;
            }
            double availableForPurchase = depositAvailableForPurchase(preBill.getPatient());

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
            for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (paymentSchemeController.checkPaymentMethodError(cd.getPaymentMethod(), cd.getPaymentMethodData())) {
                    return true;
                }
                if (cd.getPaymentMethod().equals(PaymentMethod.PatientDeposit)) {
                    if (!getPreBill().getPatient().getHasAnAccount()) {
                        JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                        return true;
                    }
                    double availableForPurchase = depositAvailableForPurchase(getPreBill().getPatient());

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
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getOnlineSettlement().getTotalValue();
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
            PatientDeposit pd = patientDepositController.checkDepositOfThePatient(preBill.getPatient(), getSessionController().getDepartment());
            if (pd != null && pd.getId() != null) {
                getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
            }
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

    private boolean saveSettlingBatchBill() {
        getSaleBill().copy(getPreBill());
        getSaleBill().copyValue(getPreBill());
        getSaleBill().setBalance(getSaleBill().getNetTotal());
        getSaleBill().setCashPaid(cashPaid);
        getSaleBill().setBillClassType(BillClassType.BilledBill);
        getSaleBill().setBillType(BillType.OpdBill);
        getSaleBill().setBillTypeAtomic(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);

        getSaleBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getSaleBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getBillBean().setPaymentMethodData(getSaleBill(), getSaleBill().getPaymentMethod(), getPaymentMethodData());
        getBillBean().setBills(billsOfBatchBillPre);

        getSaleBill().setBillDate(new Date());
        getSaleBill().setBillTime(new Date());
        getSaleBill().setCreatedAt(Calendar.getInstance().getTime());
        getSaleBill().setCreater(getSessionController().getLoggedUser());

        getSaleBill().setReferenceBill(getPreBill());

        getSaleBill().setInsId(getPreBill().getInsId());
        getSaleBill().setDeptId(getPreBill().getDeptId());

        getSaleBill().setSessionId(billNumberBean.generateDailyBillNumberForOpd(getPreBill().getDepartment()));

        OpdPreSettleRequest request = new OpdPreSettleRequest();
        request.setSaleBill(getSaleBill());
        request.setPreBill(getPreBill());
        request.setPreIndividualBills(billController.billsOfBatchBill(getPreBill()));
        request.setLoggedUser(getSessionController().getLoggedUser());
        request.setPreBillPaymentMethod(getPreBill().getPaymentMethod());
        request.setPaymentMethodData(getPaymentMethodData());
        request.captureBillState();
        List<Payment> payments;

        // One transaction: the bill, its payments and the links commit together or not at all (#26246).
        try {
            payments = buildPayments(getSaleBill(), getSaleBill().getPaymentMethod(), request);
            request.setPayments(payments);
            if (toStaff != null && paymentMethod == PaymentMethod.Staff) {
                // Single Staff method: the credit is saved by the writer, in the same transaction.
                request.addStaffCredit(toStaff, netTotal);
            }
            if (paymentMethod == PaymentMethod.PatientDeposit) {
                // Single Patient Deposit method: lower the patient balance and the deposit record, as the normal OPD bill does (#26260).
                final Patient depositPatient = getSaleBill().getPatient();
                final Double balanceBefore = depositPatient.getRunningBalance();
                request.addUndo(() -> depositPatient.setRunningBalance(balanceBefore));
                depositPatient.setRunningBalance((balanceBefore == null ? 0.0 : balanceBefore) - getSaleBill().getNetTotal());
                request.getPatientsToSave().add(depositPatient);
                request.addDepositUse(depositPatient, getSaleBill().getNetTotal());
            }
            billsOfBatchBilledBill = opdPreSettleWriter.settle(request);
        } catch (RuntimeException e) {
            request.restore();
            LOGGER.log(Level.SEVERE, "OPD pre-bill settle failed; nothing was saved", e);
            if (e instanceof OpdPreSettleException) {
                JsfUtil.addErrorMessage(e.getMessage());
            } else {
                JsfUtil.addErrorMessage("The bill was not saved. Nothing was changed. Please try again.");
            }
            return false;
        }
        // The bill is committed; a busy drawer must not undo it, so the drawer is updated after.
        // The writer saved the pre-bills with edit(); drop them from the shared cache as editAndCommit did.
        getBillFacade().evictFromSharedCache(getPreBill().getId());
        for (Bill individualPreBill : request.getPreIndividualBills()) {
            getBillFacade().evictFromSharedCache(individualPreBill.getId());
        }
        drawerController.updateDrawerForIns(payments);
        for (int i = 0; i < request.getStaffCredits().size(); i++) {
            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
        }
        return true;
    }

    /** Builds the payments of the settle in memory. Nothing is saved here; the writer saves them in its transaction. */
    private List<Payment> buildPayments(Bill bill, PaymentMethod pm, OpdPreSettleRequest request) {
        List<Payment> ps = new ArrayList<>();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment p = new Payment();
                p.setBill(bill);
                p.setInstitution(getSessionController().getInstitution());
                p.setDepartment(getSessionController().getDepartment());
                p.setCreatedAt(new Date());
                p.setCreater(getSessionController().getLoggedUser());
                p.setPaymentMethod(cd.getPaymentMethod());
                if (cd.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                    // The deposit record is lowered by the amount of this row, in the writer's transaction (#26260).
                    request.addDepositUse(bill.getPatient(), cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                }

                OpdPreSettlePaymentRows.fillMultipleRow(p, cd, getSaleBill().getPatient(), request);

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
                    p.setBank(getPaymentMethodData().getCreditCard().getInstitution());
                    p.setCreditCardRefNo(getPaymentMethodData().getCreditCard().getNo());
                    p.setComments(getPaymentMethodData().getCreditCard().getComment());
                    break;
                case Cheque:
                    p.setChequeDate(getPaymentMethodData().getCheque().getDate());
                    p.setChequeRefNo(getPaymentMethodData().getCheque().getNo());
                    p.setComments(getPaymentMethodData().getCheque().getComment());
                    break;
                case ewallet:
                    p.setPolicyNo(getPaymentMethodData().getEwallet().getReferralNo());
                    p.setReferenceNo(getPaymentMethodData().getEwallet().getReferenceNo());
                    p.setCreditCompany(getPaymentMethodData().getEwallet().getInstitution());
                    p.setComments(getPaymentMethodData().getEwallet().getComment());
                    break;
                case Agent:
                case Credit:
                    p.setReferenceNo(getPaymentMethodData().getCredit().getReferralNo());
                    p.setComments(getPaymentMethodData().getCredit().getComment());
                    break;
                case Slip:
                    p.setBank(getPaymentMethodData().getSlip().getInstitution());
                    p.setRealizedAt(getPaymentMethodData().getSlip().getDate());
                    p.setComments(getPaymentMethodData().getSlip().getComment());
                    break;
                default:
                    break;
            }

            p.setPaidValue(p.getBill().getNetTotal());

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
            getBillFacade().editAndCommit(savingSettlingIndividualBill);
        }

        individualBill.setReferenceBill(savingSettlingIndividualBill);
        getBillFacade().editAndCommit(individualBill);

        return savingSettlingIndividualBill;
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
        getBillFacade().editAndCommit(individualPreBill);

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

    public void settleBillWithPay2() {
        if (!billSettlingStarted.compareAndSet(false, true)) {
            return;
        }
        try {
            editingQty = null;
            if (errorCheckForSaleBill()) {
                return;
            }

        Bill latestPreBill = getBillFacade().findWithoutCache(getPreBill().getId());
        Map<String, Object> params = new HashMap<>();
        params.put("pre", latestPreBill);
        Bill existing = getBillFacade().findFirstByJpql("select b from BilledBill b where b.referenceBill=:pre", params);
        if (existing != null) {
            JsfUtil.addErrorMessage("Already Paid");
            return;
        }

        if (getCashPaid() < getPreBill().getNetTotal()) {
            JsfUtil.addErrorMessage("Tendered Amount is lower than Total");
            return;
        }
        if (!saveSettlingBatchBill()) {
            return;
        }

//        getPreBill().getCashBillsPre().add(getSaleBill());
        getBillFacade().editAndCommit(getPreBill());

        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(getSaleBill(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        setBill(getBillFacade().find(getSaleBill().getId()));

//        makeNull();
        billPreview = true;
    } finally {
        billSettlingStarted.set(false);
    }
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
            if (getPaymentMethodData().getCredit().getInstitution() == null) {
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
            double availableForPurchase = depositAvailableForPurchase(preBill.getPatient());

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
            for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (paymentSchemeController.checkPaymentMethodError(cd.getPaymentMethod(), cd.getPaymentMethodData())) {
                    return true;
                }
                if (cd.getPaymentMethod().equals(PaymentMethod.PatientDeposit)) {
                    if (!getPreBill().getPatient().getHasAnAccount()) {
                        JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                        return true;
                    }
                    double availableForPurchase = depositAvailableForPurchase(getPreBill().getPatient());

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

        if (!saveSettlingBatchBill()) {
            return;
        }

        getBillFacade().editAndCommit(getPreBill());
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
            for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
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
            int arrSize = getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
            ComponentDetail pm = getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);
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
            } else if (pm.getPaymentMethod() == PaymentMethod.OnlineSettlement) {
                pm.getPaymentMethodData().getOnlineSettlement().setTotalValue(remainAmount);
            }

        }
    }

    @Override
    public boolean isLastPaymentEntry(ComponentDetail cd) {
        if (cd == null ||
            getPaymentMethodData() == null ||
            getPaymentMethodData().getPaymentMethodMultiple() == null ||
            getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null ||
            getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
            return false;
        }

        List<ComponentDetail> details = getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails();
        int lastIndex = details.size() - 1;
        int currentIndex = details.indexOf(cd);
        return currentIndex != -1 && currentIndex == lastIndex;
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
        // BillFeePayment is deprecated and no longer used
    }

//    public void setBillFeePaymentAndPayment(BillFee bf, Payment p) {
//        BillFeePayment bfp = new BillFeePayment();
//        bfp.setBillFee(bf);
//        bfp.setAmount(bf.getSettleValue());
//        if (bf.getBillItem() != null && bf.getBillItem().getItem() != null) {
//            bfp.setInstitution(bf.getBillItem().getItem().getInstitution());
//            bfp.setDepartment(bf.getBillItem().getItem().getDepartment());
//        } else {
//            bfp.setInstitution(getSessionController().getInstitution());
//            bfp.setDepartment(getSessionController().getDepartment());
//        }
//        bfp.setCreater(getSessionController().getLoggedUser());
//        bfp.setCreatedAt(new Date());
//        bfp.setPayment(p);
//        getBillFeePaymentFacade().create(bfp);
//    }
    public double calBillPaidValue(Bill b) {
        String jpql = "select sum(bfp.amount) "
                + " from BillFeePayment bfp "
                + " where bfp.retired = :ret "
                + " and bfp.billFee.bill.id = :bid";

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("bid", b.getId());

        double d = getBillFeePaymentFacade().findDoubleByJpql(jpql, params);
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

//    public void calculateBillfeePaymentsForCancelRefundBill(List<BillFee> billFees, List<Payment> ps) {
//        for (BillFee bf : billFees) {
//            setBillFeePaymentAndPayment(bf, ps);
//        }
//    }
//    public void createOpdCancelRefundBillFeePayment(Bill bill, List<BillFee> billFees, List<Payment> ps) {
//        calculateBillfeePaymentsForCancelRefundBill(billFees, ps);
//    }
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
            bill.getToStaff().setCurrentCreditValue(bill.getToStaff().getCurrentCreditValue() - Math.abs(bill.getNetTotal()));
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

    public List<Payment> createPaymentsForCancellationsforOPDBill(Bill bill, PaymentMethod pm) {
        System.out.println("createPaymentsForCancellationsforOPDBill");
        System.out.println("bill = " + bill);
        List<Payment> ps = new ArrayList<>();
        if (pm == null) {
            Bill originalIndividualBill = bill.getBilledBill();
            if (originalIndividualBill == null) {
                return ps;
            }
            Bill originalBathcBill = billService.fetchBatchBillOfIndividualBill(originalIndividualBill);
            if (originalBathcBill == null) {
                return ps;
            }
            List<Payment> originalBillPayments = billService.fetchBillPayments(originalBathcBill);
            if (originalBillPayments == null || originalBillPayments.isEmpty()) {
                return ps;
            }
            for (Payment originalBillPayment : originalBillPayments) {
                Payment p = originalBillPayment.clonePaymentForNewBill();
                p.invertValues();
                p.setReferancePayment(originalBillPayment);
                p.setBill(bill);
                p.setInstitution(getSessionController().getInstitution());
                p.setDepartment(getSessionController().getDepartment());
                p.setCreatedAt(new Date());
                p.setCreater(getSessionController().getLoggedUser());
                paymentFacade.create(p);
                ps.add(p);
            }
            bill.setPaymentMethod(PaymentMethod.MultiplePaymentMethods);
            billFacade.edit(bill);
        } else {
            Payment p = new Payment();
            p.setBill(bill);
            double valueToSet = 0 - Math.abs(bill.getNetTotal());
            p.setPaidValue(valueToSet);

            p.setInstitution(getSessionController().getInstitution());
            p.setDepartment(getSessionController().getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            p.setPaymentMethod(pm);

            if (pm == PaymentMethod.PatientDeposit) {
                if (bill.getPatient().getRunningBalance() == null) {
                    bill.getPatient().setRunningBalance(Math.abs(bill.getNetTotal()));
                } else {
                    bill.getPatient().setRunningBalance(bill.getPatient().getRunningBalance() + Math.abs(bill.getNetTotal()));
                }
                patientFacade.edit(bill.getPatient());
            }

            if (pm == PaymentMethod.Staff) {
                bill.getToStaff().setCurrentCreditValue(bill.getToStaff().getCurrentCreditValue() - Math.abs(bill.getNetTotal()));
                staffFacade.edit(bill.getToStaff());
            }
            if (p.getId() == null) {
                getPaymentFacade().create(p);
            }
            getPaymentFacade().edit(p);
            ps.add(p);
        }
        return ps;
    }

    public Payment createPaymentForCollectingCenterBill(Bill bill) {
        Payment p = new Payment();
        p.setBill(bill);
        double valueToSet = 0 - Math.abs(bill.getNetTotal());
        p.setPaidValue(valueToSet);

        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());

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

        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);
        if (p.getBill().getBillType() == BillType.PaymentBill) {
            p.setPaidValue(bill.getNetTotal());
        } else {
            double valueToSet = 0 - Math.abs(bill.getNetTotal());
            p.setPaidValue(valueToSet);
        }

        if (p.getId() == null) {
            getPaymentFacade().create(p);
        }
        getPaymentFacade().edit(p);
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

    public boolean isBillSettlingStarted() {
        return billSettlingStarted.get();
    }

    public void setBillSettlingStarted(boolean billSettlingStarted) {
        this.billSettlingStarted.set(billSettlingStarted);
    }

}
