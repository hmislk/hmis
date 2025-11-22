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
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.ControllerWithMultiplePayments;
import com.divudi.bean.common.ControllerWithPatient;
import com.divudi.bean.common.PatientDepositController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SearchController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.TokenController;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.membership.MembershipSchemeController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BooleanMessage;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.data.TokenType;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.AuditEvent;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyService;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.StaffService;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.Token;
import com.divudi.core.entity.clinical.ClinicalFindingValue;
import com.divudi.core.entity.clinical.Prescription;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.UserStock;
import com.divudi.core.entity.pharmacy.UserStockContainer;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ConfigOptionFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.PrescriptionFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.StockHistoryFacade;
import com.divudi.core.facade.TokenFacade;
import com.divudi.core.facade.UserStockContainerFacade;
import com.divudi.core.facade.UserStockFacade;
import com.divudi.service.AuditService;
import com.divudi.service.BillService;
import com.divudi.service.DiscountSchemeValidationService;
import com.divudi.service.PatientDepositService;
import com.divudi.service.PaymentService;
import com.divudi.service.pharmacy.PharmacyCostingService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;

/**
 * @author Buddhika
 */
/**
 *
 * @author Buddhika
 */
@Named("pharmacySaleController1")
@SessionScoped
public class PharmacySaleController1 implements Serializable, ControllerWithPatient, ControllerWithMultiplePayments {

    /**
     * Creates a new instance of PharmacySaleController1
     */
    public PharmacySaleController1() {
    }

    @Inject
    PaymentSchemeController PaymentSchemeController;
    @Inject
    StockController stockController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    ConfigOptionController configOptionController;

    @Inject
    FinancialTransactionController financialTransactionController;
    @Inject
    SessionController sessionController;
    @Inject
    SearchController searchController;
    @Inject
    PatientDepositController patientDepositController;

    @Inject
    TokenController tokenController;
    @Inject
    DrawerController drawerController;
    @EJB
    PrescriptionFacade prescriptionFacade;
    @EJB
    private ConfigOptionFacade configOptionFacade;

    ////////////////////////
    @EJB
    private BillFacade billFacade;
    @EJB
    DiscountSchemeValidationService discountSchemeValidationService;
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
    StaffService staffBean;
    @EJB
    PaymentService paymentService;
    @EJB
    private UserStockContainerFacade userStockContainerFacade;
    @EJB
    private UserStockFacade userStockFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    TokenFacade tokenFacade;
    @EJB
    private PharmacyService pharmacyService;
    @EJB
    BillService billService;
    @EJB
    private PharmacyCostingService pharmacyCostingService;
    @EJB
    private AuditService auditService;
    @EJB
    private PatientDepositService patientDepositService;
    /////////////////////////
    private PreBill preBill;
    private Bill saleBill;
    Bill printBill;
    Bill bill;
    BillItem billItem;
    private List<BillItem> selectedBillItems;
    //BillItem removingBillItem;
    BillItem editingBillItem;
    Double qty;
    Integer intQty;
    Stock stock;
    StockDTO stockDto;
    private List<ClinicalFindingValue> allergyListOfPatient;
    private boolean billSettlingStarted;

    private PaymentScheme paymentScheme;

    int activeIndex;

    private Token token;
    private Patient patient;
    private YearMonthDay yearMonthDay;
    private String patientTabId = "tabPt";
    private String strTenderedValue = "";
    boolean billPreview = false;
    boolean fromOpdEncounter = false;
    String opdEncounterComments = "";
    int patientSearchTab = 0;

    Staff toStaff;
    Institution toInstitution;
    String errorMessage = "";

    double cashPaid;
    double netTotal;
    double balance;
    Double editingQty;
    String cashPaidStr;
    String comment;
    ///////////////////
    private UserStockContainer userStockContainer;
    PaymentMethodData paymentMethodData;
    private boolean patientDetailsEditable;
    private Department counter;
    Token currentToken;

    PaymentMethod paymentMethod;

    public Token getCurrentToken() {
        return currentToken;
    }

    public void setCurrentToken(Token currentToken) {
        this.currentToken = currentToken;
    }

    //    public String navigateToPharmacySaleWithoutStocks() {
//        prepareForPharmacySaleWithoutStock();
//        return "/pharmacy/pharmacy_sale_without_stock?faces-redirect=true";
//    }
    public String navigateToPharmacyBillForCashier() {
        if (sessionController.getPharmacyBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                resetAll();
                setBillSettlingStarted(false);
                return "/pharmacy/pharmacy_bill_retail_sale_for_cashier_1?faces-redirect=true";
            } else {
                setBillSettlingStarted(false);
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/pharmacy/pharmacy_bill_retail_sale_for_cashier_1?faces-redirect=true";
            }
        } else {
            resetAll();
            setBillSettlingStarted(false);
            return "/pharmacy/pharmacy_bill_retail_sale_for_cashier_1?faces-redirect=true";
        }
    }

    public String navigateToPharmacyBillForCashierWholeSale() {
        setBillSettlingStarted(false);
        return "/pharmacy_wholesale/pharmacy_bill_retail_sale_for_cashier_1?faces-redirect=true";
    }

    public String navigateToBillCancellationView() {
        return "pharmacy_cancel_bill_retail?faces-redirect=true";
    }

    private void prepareForPharmacySaleWithoutStock() {
        clearBill();
        clearBillItem();
        searchController.createPreBillsNotPaid();
        billPreview = false;
    }

    public void searchPatientListener() {
        //  createPaymentSchemeItems();
        calculateAllRates();
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

    public void prepareNewPharmacyBillForMembers() {
        clearNewBillForMembers();
    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
    }

    public void clearForNewBill() {
        preBill = null;
        saleBill = null;
        printBill = null;
        bill = null;
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        paymentScheme = null;
        paymentMethod = null;
        activeIndex = 0;
        patient = null;
        yearMonthDay = null;
        patientTabId = "tabPt";
        strTenderedValue = "";
        billPreview = false;
        paymentMethodData = null;
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        editingQty = null;
        cashPaidStr = null;
    }

    public void clearNewBillForMembers() {
        preBill = null;
        saleBill = null;
        printBill = null;
        bill = null;
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        paymentScheme = null;
        paymentMethod = null;
        activeIndex = 0;
        patient = null;
        yearMonthDay = null;
        patientTabId = "tabSearchPt";
        patientSearchTab = 1;
        strTenderedValue = "";
        billPreview = false;
        paymentMethodData = null;
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        editingQty = null;
        cashPaidStr = null;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCashPaidStr() {
        if (cashPaid == 0.0) {
            cashPaidStr = "";
        } else {
            cashPaidStr = String.format("%1$,.2f", cashPaid);
        }
        return cashPaidStr;
    }

    public void setCashPaidStr(String cashPaidStr) {
        try {
            setCashPaid(Double.valueOf(cashPaidStr));
        } catch (Exception e) {
            setCashPaid(0);
        }
        this.cashPaidStr = cashPaidStr;
    }

    public Double getEditingQty() {
        return editingQty;
    }

    public void setEditingQty(Double editingQty) {
        this.editingQty = editingQty;
    }

    public void onTabChange(TabChangeEvent event) {
        if (event != null && event.getTab() != null) {
            setPatientTabId(event.getTab().getId());
        }

        if (!getPatientTabId().equals("tabSearchPt")) {
            if (fromOpdEncounter == false) {
                setPatient(null);
            }
        }

//        createPaymentSchemeItems();
        calculateAllRates();

    }

    @Override
    public double calculatRemainForMultiplePaymentTotal() {

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffWelfare().getTotalValue();

            }
            return getPreBill().getNetTotal() - multiplePaymentMethodTotalValue;
        }
        return getPreBill().getTotal();
    }

    @Override
    public void recieveRemainAmountAutomatically() {
        double remainAmount = calculatRemainForMultiplePaymentTotal();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            int arrSize = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
            ComponentDetail pm = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);
            if (pm.getPaymentMethod() == PaymentMethod.Cash) {
                // Only set value automatically if not already set by user
                if (pm.getPaymentMethodData().getCash().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getCash().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.Card) {
                if (pm.getPaymentMethodData().getCreditCard().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getCreditCard().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.Cheque) {
                if (pm.getPaymentMethodData().getCheque().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getCheque().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.Slip) {
                if (pm.getPaymentMethodData().getSlip().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getSlip().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.ewallet) {
                if (pm.getPaymentMethodData().getEwallet().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getEwallet().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                if (patient == null || patient.getId() == null) {
                    pm.getPaymentMethodData().getPatient_deposit().setTotalValue(0.0);
                    return; // Patient not selected yet, ignore
                }
                // Initialize patient deposit data for UI component
                pm.getPaymentMethodData().getPatient_deposit().setPatient(patient);
                PatientDeposit pd = patientDepositController.getDepositOfThePatient(patient, sessionController.getDepartment());
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
                if (pm.getPaymentMethodData().getCredit().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getCredit().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.Staff) {
                if (pm.getPaymentMethodData().getStaffCredit().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getStaffCredit().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.Staff_Welfare) {
                if (pm.getPaymentMethodData().getStaffWelfare().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getStaffWelfare().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.OnlineSettlement) {
                if (pm.getPaymentMethodData().getOnlineSettlement().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getOnlineSettlement().setTotalValue(remainAmount);
                }
            }

        }
    }

    @Override
    public boolean isLastPaymentEntry(ComponentDetail cd) {
        if (cd == null ||
            paymentMethodData == null ||
            paymentMethodData.getPaymentMethodMultiple() == null ||
            paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null ||
            paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
            return false;
        }

        List<ComponentDetail> details = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails();
        int lastIndex = details.size() - 1;
        int currentIndex = details.indexOf(cd);
        return currentIndex != -1 && currentIndex == lastIndex;
    }

    public double getOldQty(BillItem bItem) {
        String sql = "Select b.qty From BillItem b where b.retired=false and b.bill=:b and b=:itm";
        HashMap hm = new HashMap();
        hm.put("b", getPreBill());
        hm.put("itm", bItem);
        return getBillItemFacade().findDoubleByJpql(sql, hm);
    }

    @Inject
    UserStockController userStockController;

    public void onEdit(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
        if (tmp == null) {
            return;
        }
        onEdit(tmp);
    }

    private void setZeroToQty(BillItem tmp) {
        tmp.setQty(0.0);
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0.0f);

        userStockController.updateUserStock(tmp.getTransUserStock(), 0);
    }

    //Check when edititng Qty
    //
    public boolean onEdit(BillItem tmp) {
        //Cheking Minus Value && Null
        if (tmp.getQty() <= 0 || tmp.getQty() == null) {
            setZeroToQty(tmp);
            onEditCalculation(tmp);
            JsfUtil.addErrorMessage("Can not enter a minus value");
            return true;
        }

        if (tmp.getQty() > tmp.getPharmaceuticalBillItem().getStock().getStock()) {
            setZeroToQty(tmp);
            onEditCalculation(tmp);
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return true;
        }

        //Check Is There Any Other User using same Stock
        if (!userStockController.isStockAvailable(tmp.getPharmaceuticalBillItem().getStock(), tmp.getQty(), getSessionController().getLoggedUser())) {

            setZeroToQty(tmp);
            onEditCalculation(tmp);
            JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
            return true;
        }

        userStockController.updateUserStock(tmp.getTransUserStock(), tmp.getQty());

        onEditCalculation(tmp);

        return false;
    }

    private Prescription prescription;
    private boolean enableLabelPrintFromSaleView = false;

    public void enableLabelPrint(Prescription p) {
        enableLabelPrintFromSaleView = true;
        this.prescription = p;
    }

    public void addPrescriptionToBillitem(BillItem billItem) {
        if (prescription == null) {
            prescription = new Prescription();
        }

        if (billItem.getInstructions() != null && !billItem.getInstructions().isEmpty()) {
            if (billItem.getPrescription().getComment() == null || billItem.getPrescription().getComment().isEmpty()) {
                billItem.getPrescription().setComment(billItem.getInstructions());
            } else if (billItem.getInstructions().equalsIgnoreCase(billItem.getPrescription().getComment())) {
                billItem.getPrescription().setComment(billItem.getInstructions());
            }
        }

    }

    private void onEditCalculation(BillItem tmp) {
        if (tmp == null) {
            return;
        }

        tmp.setGrossValue(tmp.getQty() * tmp.getRate());
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0 - tmp.getQty());

        calculateBillItemForEditing(tmp);

        calTotal();

    }

    public void quantityInTableChangeEvent(BillItem tmp) {
        if (tmp == null) {
            return;
        }

        tmp.setGrossValue(tmp.getQty() * tmp.getRate());
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0 - tmp.getQty());

        calculateBillItemForEditing(tmp);

        calTotal();

    }

    public void editQty(BillItem bi) {
        if (bi == null) {
            //////System.out.println("No Bill Item to Edit Qty");
            return;
        }
        if (editingQty == null) {
            //////System.out.println("Editing qty is null");
            return;
        }

        bi.setQty(editingQty);
        bi.getPharmaceuticalBillItem().setQtyInUnit(0 - editingQty);
        calculateBillItemForEditing(bi);

        calTotal();
        editingQty = null;
    }

    private Patient savePatient() {
        Patient pat = getPatient();
        // Check for null references and empty name
        if (pat == null
                || pat.getPerson() == null
                || pat.getPerson().getName() == null
                || pat.getPerson().getName().trim().isEmpty()) {
            return null;
        }

        pat.setCreater(getSessionController().getLoggedUser());
        pat.setCreatedAt(new Date());
        pat.getPerson().setCreater(getSessionController().getLoggedUser());
        pat.getPerson().setCreatedAt(new Date());

        if (pat.getPerson().getId() == null) {
            getPersonFacade().create(pat.getPerson());
        }
        if (pat.getId() == null) {
            getPatientFacade().create(pat);
        }
        return pat;
    }

    //    private Patient savePatient() {
//        switch (getPatientTabId()) {
//            case "tabPt":
//                if (!getSearchedPatient().getPerson().getName().trim().equals("")) {
//                    getSearchedPatient().setCreater(getSessionController().getLoggedUser());
//                    getSearchedPatient().setCreatedAt(new Date());
//                    getSearchedPatient().getPerson().setCreater(getSessionController().getLoggedUser());
//                    getSearchedPatient().getPerson().setCreatedAt(new Date());
//                    if (getSearchedPatient().getPerson().getId() == null) {
//                        getPersonFacade().create(getSearchedPatient().getPerson());
//                    }
//                    if (getSearchedPatient().getId() == null) {
//                        getPatientFacade().create(getSearchedPatient());
//                    }
//                    return getSearchedPatient();
//                } else {
//                    return null;
//                }
//            case "tabSearchPt":
//                return getSearchedPatient();
//        }
//        return null;
//    }
    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public Integer getIntQty() {
        if (qty == null) {
            return null;
        }
        return qty.intValue();
    }

    public void setIntQty(Integer intQty) {
        this.intQty = intQty;
        if (intQty == null) {
            setQty(null);
        } else {
            setQty(intQty.doubleValue());
        }
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

    public Stock convertStockDtoToEntity(StockDTO stockDto) {
        if (stockDto == null || stockDto.getId() == null) {
            return null;
        }
        return stockFacade.find(stockDto.getId());
    }

    public StockDTO getStockDto() {
        return stockDto;
    }

    public void setStockDto(StockDTO stockDto) {
        this.stockDto = stockDto;
        // Automatically convert DTO to entity
        if (stockDto != null) {
            this.stock = convertStockDtoToEntity(stockDto);
        } else {
            this.stock = null;
        }
    }

    public String newSaleBillWithoutReduceStock() {
        clearBill();
        clearBillItem();
        billPreview = false;
        setBillSettlingStarted(false);
        return "pharmacy_bill_retail_sale_1";
    }

    public String newSaleBillWithoutReduceStockForCashier() {
        clearBill();
        clearBillItem();
        billPreview = false;
        setBillSettlingStarted(false);
        return "pharmacy_bill_retail_sale_for_cashier_1";
    }

    public String navigateToPharmacyRetailSale() {
        if (sessionController.getPharmacyBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                resetAll();
                setBillSettlingStarted(false);
                return "/pharmacy/pharmacy_bill_retail_sale_1?faces-redirect=true";
            } else {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        } else {
            resetAll();
            setBillSettlingStarted(false);
            setBillSettlingStarted(false);
            return "/pharmacy/pharmacy_bill_retail_sale_1?faces-redirect=true";
        }
    }

    private String navigateToPharmacyRetailSaleAfterCashierCheck(Patient pt, PaymentScheme ps) {
        if (pt == null) {
            JsfUtil.addErrorMessage("No Patient Selected");
            return "";
        }
        if (ps == null) {
            JsfUtil.addErrorMessage("No Membership");
            return "";
        }
        if (patient == null) {
            JsfUtil.addErrorMessage("No patient selected");
            patient = new Patient();
            patientDetailsEditable = true;
        }
        resetAll();
        patient = pt;
        paymentScheme = ps;
        setPatient(getPatient());
        setBillSettlingStarted(false);
        return "/pharmacy/pharmacy_bill_retail_sale_1?faces-redirect=true";
    }

    public String navigateToPharmacyRetailSale(Patient pt, PaymentScheme ps) {
        if (sessionController.getPharmacyBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                return navigateToPharmacyRetailSaleAfterCashierCheck(pt, ps);
            } else {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        } else {
            return navigateToPharmacyRetailSaleAfterCashierCheck(pt, ps);
        }
    }

    private String navigateToPharmacyRetailSaleAfterCashierCheckForCashier(Patient pt, PaymentScheme ps) {
        if (pt == null) {
            JsfUtil.addErrorMessage("No Patient Selected");
            return "";
        }
        if (ps == null) {
            JsfUtil.addErrorMessage("No Membership");
            return "";
        }
        if (patient == null) {
            JsfUtil.addErrorMessage("No patient selected");
            patient = new Patient();
            patientDetailsEditable = true;
        }
        resetAll();
        patient = pt;
        paymentScheme = ps;
        setPatient(getPatient());
        setBillSettlingStarted(false);
        return "/pharmacy/pharmacy_bill_retail_sale_for_cashier_1?faces-redirect=true";
    }

    public String navigateToPharmacyRetailSaleForCashier(Patient pt, PaymentScheme ps) {
        if (sessionController.getPharmacyBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                return navigateToPharmacyRetailSaleAfterCashierCheckForCashier(pt, ps);
            } else {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        } else {
            return navigateToPharmacyRetailSaleAfterCashierCheckForCashier(pt, ps);
        }
    }

    public void resetAll() {
        setBillSettlingStarted(false);
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        clearBill();
        clearBillItem();
//        searchController.createPreBillsNotPaid();
        billPreview = false;
    }

    public void prepareForNewPharmacyRetailBill() {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        clearBill();
        clearBillItem();
//        searchController.createPreBillsNotPaid();
        billPreview = false;
    }

    public String pharmacyRetailSale() {
        setBillSettlingStarted(false);
        return "/pharmacy_wholesale/pharmacy_bill_retail_sale_1?faces-redirect=true";
    }

    public String toPharmacyRetailSale() {
        setBillSettlingStarted(false);
        return "/pharmacy/pharmacy_bill_retail_sale_1?faces-redirect=true";
    }

    public List<Item> completeRetailSaleItems(String qry) {
        Map m = new HashMap<>();
        List<Item> items;
        String sql;
        sql = "select i from Item i where i.retired=false "
                + " and (i.name) like :n and type(i)=:t "
                + " and i.id not in(select ibs.id from Stock ibs where ibs.stock >:s and ibs.department=:d and (ibs.itemBatch.item.name) like :n ) order by i.name ";
        m.put("t", Amp.class);
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("n", "%" + qry + "%");
        double s = 0.0;
        m.put("s", s);
        items = getItemFacade().findByJpql(sql, m, 10);
        return items;
    }

    @Deprecated
    public List<Stock> completeAvailableStocks(String qry) {
        Set<Stock> stockSet = new LinkedHashSet<>(); // Preserve insertion order
        List<Stock> initialStocks = completeAvailableStocksStartsWith(qry);
        if (initialStocks != null) {
            stockSet.addAll(initialStocks);
        }

        // No need to check if initialStocks is empty or null anymore, Set takes care of duplicates
        if (stockSet.size() <= 10) {
            List<Stock> additionalStocks = completeAvailableStocksContains(qry);
            if (additionalStocks != null) {
                stockSet.addAll(additionalStocks);
            }
        }

        return new ArrayList<>(stockSet);
    }

    @Deprecated
    public List<Stock> completeAvailableStocksStartsWith(String qry) {
        List<Stock> stockList;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("s", d);
        m.put("n", qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n )  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }
        stockList = getStockFacade().findByJpql(sql, m, 20);
        return stockList;
    }

    @Deprecated
    public List<Stock> completeAvailableStocksContains(String qry) {
        List<Stock> stockList;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("s", d);
        m.put("n", "%" + qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n )  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }
        stockList = getStockFacade().findByJpql(sql, m, 20);
        return stockList;
    }

    //matara pharmacy auto complete
    @Deprecated
    public List<Stock> completeAvailableStocksFromNameOrGenericOld(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("s", d);
        qry = qry.replaceAll("\n", "");
        qry = qry.replaceAll("\r", "");
        m.put("n", "%" + qry.toUpperCase().trim() + "%");

        //////System.out.println("qry = " + qry);
        if (qry.length() > 4) {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n or (i.itemBatch.item.vmp.name) like :n) order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.vmp.name) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }

        items = getStockFacade().findByJpql(sql, m, 20);

        if (qry.length() > 5 && items.size() == 1) {
            stock = items.get(0);
            handleSelectAction();
        }
        return items;
    }

    @Deprecated
    public List<Stock> completeAvailableStocksFromNameOrGeneric(String qry) {
        long startTime = System.nanoTime();
        if (qry == null || qry.trim().isEmpty()) {
            return Collections.emptyList();
        }

        qry = qry.replaceAll("[\\n\\r]", "").trim();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("department", getSessionController().getLoggedUser().getDepartment());
        parameters.put("stockMin", 0.0);
        parameters.put("query", "%" + qry + "%");

        String sql;
        if (qry.length() > 6) {
            sql = "SELECT i FROM Stock i "
                    + "WHERE i.stock > :stockMin "
                    + "AND i.department = :department "
                    + "AND (i.itemBatch.item.name LIKE :query ";

            if (configOptionApplicationController.getBooleanValueByKey("Enable search medicines by item code", true)) {
                sql += " OR i.itemBatch.item.code LIKE :query ";
            }

            if (configOptionApplicationController.getBooleanValueByKey("Enable search medicines by barcode", true)) {
                sql += "OR i.itemBatch.item.barcode = :query ";
            }
        } else {
            sql = "SELECT i FROM Stock i "
                    + "WHERE i.stock > :stockMin "
                    + "AND i.department = :department "
                    + "AND( i.itemBatch.item.name LIKE :query ";

            if (configOptionApplicationController.getBooleanValueByKey("Enable search medicines by item code", true)) {
                sql += " OR i.itemBatch.item.code LIKE :query ";
            }

            if (configOptionApplicationController.getBooleanValueByKey("Enable search medicines by barcode", false)) {
                sql += "OR i.itemBatch.item.barcode = :query ";
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Enable search medicines by generic name(VMP)", false)) {
            sql += "OR i.itemBatch.item.vmp.vtm.name LIKE :query ";
        }

        sql += ") ORDER BY i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        long endTime = System.nanoTime();
        long executionTime = endTime - startTime;
        return getStockFacade().findByJpql(sql, parameters, 20);
    }

    public List<Stock> completeAvailableStockOptimized(String qry) {
        if (qry == null || qry.trim().isEmpty()) {
            return Collections.emptyList();
        }

        qry = qry.replaceAll("[\\n\\r]", "").trim();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("department", getSessionController().getLoggedUser().getDepartment());
        parameters.put("stockMin", 0.0);
        parameters.put("query", "%" + qry + "%");

        boolean searchByItemCode = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by item code", true);
        boolean searchByBarcode = qry.length() > 6
                ? configOptionApplicationController.getBooleanValueByKey(
                        "Enable search medicines by barcode", true)
                : configOptionApplicationController.getBooleanValueByKey(
                        "Enable search medicines by barcode", false);
        boolean searchByGeneric = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by generic name(VMP)", false);

        StringBuilder sql = new StringBuilder("SELECT i FROM Stock i ")
                .append("WHERE i.stock > :stockMin ")
                .append("AND i.department = :department ")
                .append("AND (");

        sql.append("i.itemBatch.item.name LIKE :query ");

        if (searchByItemCode) {
            sql.append("OR i.itemBatch.item.code LIKE :query ");
        }

        if (searchByBarcode) {
            sql.append("OR i.itemBatch.item.barcode = :query ");
        }

        if (searchByGeneric) {
            sql.append("OR i.itemBatch.item.vmp.vtm.name LIKE :query ");
        }

        sql.append(") ORDER BY i.itemBatch.item.name, i.itemBatch.dateOfExpire");

        return getStockFacade().findByJpql(sql.toString(), parameters, 20);
    }

    public List<StockDTO> completeAvailableStockOptimizedDto(String qry) {
        if (qry == null || qry.trim().isEmpty()) {
            return Collections.emptyList();
        }

        qry = qry.replaceAll("[\\n\\r]", "").trim();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("department", getSessionController().getLoggedUser().getDepartment());
        parameters.put("stockMin", 0.0);
        parameters.put("query", "%" + qry + "%");

        boolean searchByItemCode = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by item code", true);
        boolean searchByBarcode = qry.length() > 6
                ? configOptionApplicationController.getBooleanValueByKey(
                        "Enable search medicines by barcode", true)
                : configOptionApplicationController.getBooleanValueByKey(
                        "Enable search medicines by barcode", false);
        boolean searchByGeneric = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by generic name(VMP)", false);

        StringBuilder sql = new StringBuilder("SELECT NEW com.divudi.core.data.dto.StockDTO(")
                .append("i.id, i.itemBatch.item.name, i.itemBatch.item.code, i.itemBatch.item.vmp.name, ")
                .append("i.itemBatch.retailsaleRate, i.stock, i.itemBatch.dateOfExpire) ")
                .append("FROM Stock i ")
                .append("WHERE i.stock > :stockMin ")
                .append("AND i.department = :department ")
                .append("AND (");

        sql.append("i.itemBatch.item.name LIKE :query ");

        if (searchByItemCode) {
            sql.append("OR i.itemBatch.item.code LIKE :query ");
        }

        if (searchByBarcode) {
            sql.append("OR i.itemBatch.item.barcode = :query ");
        }

        if (searchByGeneric) {
            sql.append("OR i.itemBatch.item.vmp.vtm.name LIKE :query ");
        }

        sql.append(") ORDER BY i.itemBatch.item.name, i.itemBatch.dateOfExpire");

        return (List<StockDTO>) getStockFacade().findLightsByJpql(sql.toString(), parameters, TemporalType.TIMESTAMP, 20);
    }

    public void handleSelectAction() {
        if (stock == null) {
            //////System.out.println("Stock NOT selected.");
        }
        if (getBillItem() == null || getBillItem().getPharmaceuticalBillItem() == null) {
            //////System.out.println("Internal Error at PharmacySaleController.java > handleSelectAction");
        }

        getBillItem().getPharmaceuticalBillItem().setStock(stock);
        calculateRates(billItem);
        pharmacyService.addBillItemInstructions(billItem);

    }

    public void handleSelect(SelectEvent event) {
        handleSelectAction();
    }

    //    public void calculateRatesOfSelectedBillItemBeforeAddingToTheList(BillItem bi) {
//        ////////System.out.println("calculating rates");
//        if (bi.getPharmaceuticalBillItem().getStock() == null) {
//            ////////System.out.println("stock is null");
//            return;
//        }
//        getBillItem();
//        PharmaceuticalBillItem pharmBillItem = bi.getPharmaceuticalBillItem();
//        if (pharmBillItem != null) {
//            Stock stock = pharmBillItem.getStock();
//            if (stock != null) {
//                ItemBatch itemBatch = stock.getItemBatch();
//                if (itemBatch != null) {
//                    // Ensure that each step in the chain is not null before accessing further.
//                    bi.setRate(itemBatch.getRetailsaleRate());
//                } else {
//                }
//            } else {
//            }
//        } else {
//        }
//
////        bi.setRate(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
//        bi.setDiscount(calculateBillItemDiscountRate(bi));
//        //  ////System.err.println("Discount "+bi.getDiscount());
//        bi.setNetRate(bi.getRate() - bi.getDiscount());
//        //  ////System.err.println("Net "+bi.getNetRate());
//    }
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
        if (getQty() > getStock().getStock()) {
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return;
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

    public void addBillItem() {
        if (configOptionApplicationController.getBooleanValueByKey("Add quantity from multiple batches in pharmacy retail billing")) {
            addBillItemMultipleBatches();
        } else {
            addBillItemSingleItem();
        }
        processBillItems();
        setActiveIndex(1);
    }

    public void processBillItems() {
        calculateAllRates();
        calculateTotals();
    }

    public void calculateAllRates() {
        for (BillItem tbi : getPreBill().getBillItems()) {
            calculateRates(tbi);
//            calculateBillItemForEditing(tbi);
        }
        calculateTotals();
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

    public double addBillItemSingleItem() {
        editingQty = null;
        errorMessage = null;
        double addedQty = 0.0;
        if (billItem == null) {
            return addedQty;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            return addedQty;
        }
        if (getStock() == null) {
            errorMessage = "Item ??";
            JsfUtil.addErrorMessage("Please select an Item Batch to Dispense ??");
            return addedQty;
        }
        if (getStock().getItemBatch().getDateOfExpire().before(CommonFunctions.getCurrentDateTime())) {
            JsfUtil.addErrorMessage("Please not select Expired Items");
            return addedQty;
        }
        if (getQty() == null) {
            errorMessage = "Quantity?";
            JsfUtil.addErrorMessage("Quantity?");
            return addedQty;
        }
        if (getQty() == 0.0) {
            errorMessage = "Quantity Zero?";
            JsfUtil.addErrorMessage("Quentity Zero?");
            return addedQty;
        }
        if (getQty() > getStock().getStock()) {
            errorMessage = "No sufficient stocks.";
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return addedQty;
        }

        if (checkItemBatch()) {
            errorMessage = "This batch is already there in the bill.";
            JsfUtil.addErrorMessage("Already added this item batch");
            return addedQty;
        }
//        if (CheckDateAfterOneMonthCurrentDateTime(getStock().getItemBatch().getDateOfExpire())) {
//            errorMessage = "This batch is Expire With in 31 Days.";
//            JsfUtil.addErrorMessage("This batch is Expire With in 31 Days.");
//            return;
//        }
        //Checking User Stock Entity
        if (!userStockController.isStockAvailable(getStock(), getQty(), getSessionController().getLoggedUser())) {
            JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
            return addedQty;
        }
        if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
            if (patient != null && getBillItem() != null) {

                if (allergyListOfPatient == null) {
                    allergyListOfPatient = pharmacyService.getAllergyListForPatient(patient);
                }
                String allergyMsg = pharmacyService.getAllergyMessageForPatient(patient, billItem, allergyListOfPatient);

                if (!allergyMsg.isEmpty()) {
                    JsfUtil.addErrorMessage(allergyMsg);
                    return addedQty;
                }
            }
        }

        addedQty = qty;
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);
        billItem.getPharmaceuticalBillItem().setStock(stock);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        calculateBillItem();
        ////System.out.println("Rate*****" + billItem.getRate());
        billItem.setInwardChargeType(InwardChargeType.Medicine);

        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setBill(getPreBill());

        billItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(billItem);

        if (getUserStockContainer().getId() == null) {
            saveUserStockContainer();
        }

        UserStock us = saveUserStock(billItem);
        billItem.setTransUserStock(us);

        pharmacyService.addBillItemInstructions(billItem);

        clearBillItem();
        getBillItem();
        return addedQty;
    }

    public void addBillItemMultipleBatches() {
        editingQty = null;
        errorMessage = null;

        if (billItem == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        if (getStock() == null) {
            errorMessage = "Please select an Item Batch to Dispense?";
            JsfUtil.addErrorMessage("Please select an Item Batch to Dispense?");
            return;
        }
        Stock userSelectedStock = stock;
//        if (getStock().getItemBatch().getDateOfExpire().before(commonController.getCurrentDateTime())) {
//            JsfUtil.addErrorMessage("You are NOT allowed to select Expired Items");
//            return;
//        }
//        if (getStock().getItemBatch().getDateOfExpire().before(commonController.getCurrentDateTime())) {
//            JsfUtil.addErrorMessage("You are NOT allowed to select Expired Items");
//            return;
//        }
//        if (getStock().getItemBatch().getDateOfExpire().before(commonController.getCurrentDateTime())) {
//            JsfUtil.addErrorMessage("You are NOT allowed to select Expired Items");
//            return;
//        }
//        if (getStock().getItemBatch().getDateOfExpire().before(commonController.getCurrentDateTime())) {
//            JsfUtil.addErrorMessage("You are NOT allowed to select Expired Items");
//            return;
//        }
        if (getQty() == null) {
            errorMessage = "Please enter a Quantity";
            JsfUtil.addErrorMessage("Quantity?");
            return;
        }
        if (getQty() == 0.0) {
            errorMessage = "Please enter a Quantity";
            JsfUtil.addErrorMessage("Quentity Zero?");
            return;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
            if (patient != null && getBillItem() != null) {
                if (allergyListOfPatient == null) {
                    allergyListOfPatient = pharmacyService.getAllergyListForPatient(patient);
                }
                String allergyMsg = pharmacyService.getAllergyMessageForPatient(patient, billItem, allergyListOfPatient);

                if (!allergyMsg.isEmpty()) {
                    JsfUtil.addErrorMessage(allergyMsg);
                    return;
                }
            }
        }

        double requestedQty = getQty();
        double addedQty = 0.0;
        double remainingQty = getQty();

        if (getQty() <= getStock().getStock()) {
            double thisTimeAddingQty = addBillItemSingleItem();
            if (thisTimeAddingQty >= requestedQty) {
                return;
            } else {
                addedQty += thisTimeAddingQty;
                remainingQty = remainingQty - thisTimeAddingQty;
            }
        } else {
            qty = getStock().getStock();
            double thisTimeAddingQty = addBillItemSingleItem();
            addedQty += thisTimeAddingQty;
            remainingQty = remainingQty - thisTimeAddingQty;
        }

//        addedQty = addBillItemSingleItem();
//        System.out.println("stock = " + userSelectedStock);
//        System.out.println("stock item batch = " + userSelectedStock.getItemBatch());
//        System.out.println("stock item batch item= " + userSelectedStock.getItemBatch().getItem());
        List<Stock> availableStocks = stockController.findNextAvailableStocks(userSelectedStock);
        for (Stock s : availableStocks) {
            stock = s;
            if (remainingQty < s.getStock()) {
                qty = remainingQty;
            } else {
                qty = s.getStock();
            }
            double thisTimeAddingQty = addBillItemSingleItem();
            addedQty += thisTimeAddingQty;
            remainingQty = remainingQty - thisTimeAddingQty;

            if (remainingQty <= 0) {
                return;
            }
        }
        if (addedQty < requestedQty) {
            errorMessage = "Quantity is not Enough...!";
            JsfUtil.addErrorMessage("Only " + String.format("%.0f", addedQty) + " is Available form the Requested Quantity");
        }

    }

    private void addSingleStock() {
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);
        billItem.getPharmaceuticalBillItem().setStock(stock);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        calculateBillItem();
        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setBill(getPreBill());
        billItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(billItem);
        if (getUserStockContainer().getId() == null) {
            saveUserStockContainer();
        }
        UserStock us = saveUserStock(billItem);
        billItem.setTransUserStock(us);
    }

    private void addMultipleStock() {
        Double remainingQty = Math.abs(qty) - Math.abs(getStock().getStock());
        addSingleStock();
        List<Stock> availableStocks = stockController.findNextAvailableStocks(getStock());

    }

    private void saveUserStockContainer() {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());

        getUserStockContainer().setCreater(getSessionController().getLoggedUser());
        getUserStockContainer().setCreatedAt(new Date());

        getUserStockContainerFacade().create(getUserStockContainer());

    }

    private UserStock saveUserStock(BillItem tbi) {
        UserStock us = new UserStock();
        us.setStock(tbi.getPharmaceuticalBillItem().getStock());
        us.setUpdationQty(tbi.getQty());
        us.setCreater(getSessionController().getLoggedUser());
        us.setCreatedAt(new Date());
        us.setUserStockContainer(getUserStockContainer());
        getUserStockFacade().create(us);

        getUserStockContainer().getUserStocks().add(us);

        return us;
    }

    //    public void calculateAllRatesNew() {
//        ////////System.out.println("calculating all rates");
//        for (BillItem tbi : getPreBill().getBillItems()) {
//            calculateRatesOfSelectedBillItemBeforeAddingToTheList(tbi);
//            calculateBillItemForEditing(tbi);
//        }
//        calTotal();
//    }
    public void calTotalNew() {
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
        ////System.out.println("2.discount = " + discount);
        //   netTot = netTot + getPreBill().getServiceCharge();
        getPreBill().setNetTotal(netTot);
        getPreBill().setTotal(grossTot);
        getPreBill().setGrantTotal(grossTot);
        getPreBill().setDiscount(discount);
        setNetTotal(getPreBill().getNetTotal());

    }

    //    Checked
    public BillItem getBillItem() {
        if (billItem == null) {
            billItem = new BillItem();
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.setBillItem(billItem);
            billItem.setPharmaceuticalBillItem(pbi);
        }
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    //    private boolean errorCheckForPreBill() {
//        if (getPreBill().getBillItems().isEmpty()) {
//            JsfUtil.addErrorMessage("No Items added to bill to sale");
//            return true;
//        }
//        return false;
//    }
//    private boolean checkPaymentScheme(PaymentScheme paymentScheme) {
//        if (paymentScheme != null && paymentScheme.getPaymentMethod() != null && paymentScheme.getPaymentMethod() == PaymentMethod.Cheque) {
//            if (getSaleBill().getBank() == null || getSaleBill().getChequeRefNo() == null || getSaleBill().getChequeDate() == null) {
//                JsfUtil.addErrorMessage("Please select Cheque Number,Bank and Cheque Date");
//                return true;
//            }
//
//        }
//
//        if (paymentScheme != null && paymentScheme.getPaymentMethod() != null && paymentScheme.getPaymentMethod() == PaymentMethod.Slip) {
//            if (getSaleBill().getBank() == null || getSaleBill().getComments() == null || getSaleBill().getChequeDate() == null) {
//                JsfUtil.addErrorMessage("Please Fill Memo,Bank and Slip Date ");
//                return true;
//            }
//
//        }
//
//        if (paymentScheme != null && paymentScheme.getPaymentMethod() != null && paymentScheme.getPaymentMethod() == PaymentMethod.Card) {
//            if (getSaleBill().getBank() == null || getSaleBill().getCreditCardRefNo() == null) {
//                JsfUtil.addErrorMessage("Please Fill Credit Card Number and Bank");
//                return true;
//            }
//
////            if (getCreditCardRefNo().trim().length() < 16) {
////                JsfUtil.addErrorMessage("Enter 16 Digit");
////                return true;
////            }
//        }
//
//        if (paymentScheme != null && paymentScheme.getPaymentMethod() != null && paymentScheme.getPaymentMethod() == PaymentMethod.Credit) {
//            if (getSaleBill().getCreditCompany() == null) {
//                JsfUtil.addErrorMessage("Please Select Credit Company");
//                return true;
//            }
//
//        }
//
//        if (paymentScheme != null && paymentScheme.getPaymentMethod() != null && paymentScheme.getPaymentMethod() == PaymentMethod.Cash) {
//            if (getPreBill().getCashPaid() == 0.0) {
//                JsfUtil.addErrorMessage("Please select tendered amount correctly");
//                return true;
//            }
//            if (getPreBill().getCashPaid() < getPreBill().getNetTotal()) {
//                JsfUtil.addErrorMessage("Please select tendered amount correctly");
//                return true;
//            }
//        }
//
//        return false;
//
//    }
    @Override
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    @Override
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    private boolean errorCheckForSaleBill() {

        if (getPaymentSchemeController().checkPaymentMethodError(getPaymentMethod(), paymentMethodData)) {
            return true;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need to Enter the Cash Tendered Amount to Settle Pharmacy Retail Bill", true)) {
            if (paymentMethod == PaymentMethod.Cash) {
                if (cashPaid == 0.0) {
                    JsfUtil.addErrorMessage("Please enter the paid amount");
                    return true;
                }
                if (cashPaid < getPreBill().getNetTotal()) {
                    JsfUtil.addErrorMessage("Please select tendered amount correctly");
                    return true;
                }
            }
        }

        return false;
    }

    private void savePreBillFinallyForRetailSale(Patient pt) {
        if (getPreBill().getId() == null) {
            getBillFacade().create(getPreBill());
        }
        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getPreBill().setCreater(getSessionController().getLoggedUser());

        getPreBill().setPatient(pt);
//        getPreBill().setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(pt, getSessionController().getApplicationPreference().isMembershipExpires()));
        getPreBill().setToStaff(toStaff);
        getPreBill().setToInstitution(toInstitution);

        getPreBill().setComments(comment);

        getPreBill().setCashPaid(cashPaid);
        getPreBill().setBalance(balance);

        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());
        getPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getPreBill().setPaymentMethod(getPaymentMethod());
        getPreBill().setPaymentScheme(getPaymentScheme());

        getBillBean().setPaymentMethodData(getPreBill(), getPaymentMethod(), getPaymentMethodData());

        // Handle Department ID generation
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        } else {
            // Use existing method for backward compatibility
            deptId = getBillNumberBean().departmentBillNumberGenerator(getPreBill().getDepartment(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        }

        // Handle Institution ID generation (completely separate)
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        } else {
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
                insId = deptId; // Use same number as department to avoid consuming counter twice
            } else {
                // Use existing method for backward compatibility
                insId = getBillNumberBean().institutionBillNumberGenerator(getPreBill().getInstitution(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
            }
        }
        getPreBill().setInsId(insId);
        getPreBill().setDeptId(deptId);
        getPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        getPreBill().setInvoiceNumber(billNumberBean.fetchPaymentSchemeCount(getPreBill().getPaymentScheme(), getPreBill().getBillType(), getPreBill().getInstitution()));

    }

    private void savePreBillFinallyForRetailSaleForCashier(Patient pt) {
        if (getPreBill().getId() == null) {
            getBillFacade().create(getPreBill());
        }
        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getPreBill().setCreater(getSessionController().getLoggedUser());

        getPreBill().setPatient(pt);
//        getPreBill().setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(pt, getSessionController().getApplicationPreference().isMembershipExpires()));
        getPreBill().setToStaff(toStaff);
        getPreBill().setToInstitution(toInstitution);

        getPreBill().setComments(comment);

        getPreBill().setCashPaid(cashPaid);
        getPreBill().setBalance(balance);

        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());
        getPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getPreBill().setPaymentMethod(getPaymentMethod());
        getPreBill().setPaymentScheme(getPaymentScheme());

        getBillBean().setPaymentMethodData(getPreBill(), getPaymentMethod(), getPaymentMethodData());

        // Handle Department ID generation
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else {
            // Use existing method for backward compatibility
            deptId = getBillNumberBean().departmentBillNumberGenerator(getPreBill().getDepartment(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        }

        // Handle Institution ID generation
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else {
            // Check if department strategy is enabled
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
                insId = deptId; // Use same number as department to avoid consuming counter twice
            } else {
                // Use existing method for backward compatibility
                insId = getBillNumberBean().institutionBillNumberGenerator(getPreBill().getInstitution(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
            }
        }
        getPreBill().setInsId(insId);
        getPreBill().setDeptId(deptId);
        getPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        getPreBill().setInvoiceNumber(billNumberBean.fetchPaymentSchemeCount(getPreBill().getPaymentScheme(), getPreBill().getBillType(), getPreBill().getInstitution()));

    }

    @Inject
    private BillBeanController billBean;

    private void saveSaleBill() {
        //  calculateRatesForAllBillItemsInPreBill();

        getSaleBill().copy(getPreBill());
        getSaleBill().copyValue(getPreBill());

        getSaleBill().setBillType(BillType.PharmacySale);
        getSaleBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE);

        getSaleBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getSaleBill().setInstitution(getSessionController().getLoggedUser().getInstitution());
        getSaleBill().setBillDate(new Date());
        getSaleBill().setBillTime(new Date());
        getSaleBill().setReferenceBill(getPreBill());

        getSaleBill().setInsId(getPreBill().getInsId());
        getSaleBill().setDeptId(getPreBill().getDeptId());
        getSaleBill().setInvoiceNumber(getPreBill().getInvoiceNumber());
        getSaleBill().setComments(comment);

        getSaleBill().setCashPaid(cashPaid);
        getSaleBill().setBalance(balance);

        getBillBean().setPaymentMethodData(getSaleBill(), getSaleBill().getPaymentMethod(), getPaymentMethodData());
        if (getSaleBill().getId() == null) {
            getSaleBill().setCreatedAt(Calendar.getInstance().getTime());
            getSaleBill().setCreater(getSessionController().getLoggedUser());
            getBillFacade().create(getSaleBill());
        } else {
            getBillFacade().edit(getSaleBill());
        }

        updatePreBill();
    }
//

    private void updatePreBill() {
        getPreBill().setReferenceBill(getSaleBill());
        getBillFacade().edit(getPreBill());
    }

    private void savePreBillItemsFinally(List<BillItem> list) {
        for (BillItem tbi : list) {
            if (onEdit(tbi)) {
//If any issue in Stock Bill Item will not save & not include for total
//                continue;
            }

            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());
            if (tbi.getId() == null) {
                tbi.setCreatedAt(new Date());
                tbi.setCreater(sessionController.getLoggedUser());
                getBillItemFacade().create(tbi);
            } else {
                getBillItemFacade().edit(tbi);
            }
            double qtyL = tbi.getPharmaceuticalBillItem().getQty() + tbi.getPharmaceuticalBillItem().getFreeQty();
            //Deduct Stock
            boolean returnFlag = getPharmacyBean().deductFromStock(tbi.getPharmaceuticalBillItem().getStock(),
                    Math.abs(qtyL), tbi.getPharmaceuticalBillItem(), getPreBill().getDepartment());
            if (!returnFlag) {
                // Store original item data before modification for audit
                BillItem originalBillItem = new BillItem();
                originalBillItem.setQty(tbi.getQty());
                originalBillItem.setItem(tbi.getItem());
                originalBillItem.setRate(tbi.getRate());
                originalBillItem.setNetValue(tbi.getNetValue());

                tbi.setQty(0.0);
                // Keep PharmaceuticalBillItem quantities in sync for consistency
                if (tbi.getPharmaceuticalBillItem() != null) {
                    tbi.getPharmaceuticalBillItem().setQty(0.0);
                    tbi.getPharmaceuticalBillItem().setFreeQty(0.0f);
                }
                JsfUtil.addErrorMessage("Error. Please check the bill again manually. When stock was recording, there was no sufficient stocks for the item : " + tbi.getItem().getName());

                // Record audit event for insufficient stock
                auditService.logAudit(
                        originalBillItem,
                        tbi,
                        sessionController.getLoggedUser(),
                        "BillItem",
                        "INSUFFICIENT_STOCK_QUANTITY_RESET"
                );

                getBillItemFacade().edit(tbi);
            }
            getPreBill().getBillItems().add(tbi);
        }

        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());

        calculateAllRates();

        getBillFacade().edit(getPreBill());
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
        if (paymentMethodData != null || toStaff != null) {
            getPaymentMethodData().getStaffCredit().setToStaff(toStaff);
            getPaymentMethodData().getStaffWelfare().setToStaff(toStaff);
        }
    }

    private void syncStaffSelectionFromPaymentDetails(PaymentMethod method) {
        if (method != PaymentMethod.Staff && method != PaymentMethod.Staff_Welfare) {
            return;
        }
        if (paymentMethodData == null) {
            return;
        }
        if (toStaff != null) {
            return;
        }
        ComponentDetail staffComponent = method == PaymentMethod.Staff
                ? paymentMethodData.getStaffCredit()
                : paymentMethodData.getStaffWelfare();
        if (staffComponent != null && staffComponent.getToStaff() != null) {
            setToStaff(staffComponent.getToStaff());
        }
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    private void saveSaleBillItems(List<BillItem> list) {
        for (BillItem tbi : list) {

            BillItem newBil = new BillItem();

            newBil.copy(tbi);
            newBil.setReferanceBillItem(tbi);
            newBil.setBill(getSaleBill());
            newBil.setInwardChargeType(InwardChargeType.Medicine);
            //      newBil.setBill(getSaleBill());
            newBil.setCreatedAt(Calendar.getInstance().getTime());
            newBil.setCreater(getSessionController().getLoggedUser());

            if (newBil.getId() == null) {
                getBillItemFacade().create(newBil);
            }

            if (tbi.getPrescription() != null) {
                if (tbi.getPrescription().getId() == null) {
                    prescriptionFacade.create(tbi.getPrescription());
                } else {
                    prescriptionFacade.edit(tbi.getPrescription());
                }

                newBil.setPrescription(tbi.getPrescription());
                tbi.getPrescription().setPatient(patient);
                tbi.getPrescription().setCreatedAt(new Date());
                tbi.getPrescription().setCreater(sessionController.getWebUser());
                tbi.getPrescription().setInstitution(sessionController.getInstitution());
                tbi.getPrescription().setDepartment(sessionController.getDepartment());
                prescriptionFacade.edit(tbi.getPrescription());
            }

            PharmaceuticalBillItem newPhar = new PharmaceuticalBillItem();
            newPhar.copy(tbi.getPharmaceuticalBillItem());
            newPhar.setBillItem(newBil);

            if (newPhar.getId() == null) {
                getPharmaceuticalBillItemFacade().create(newPhar);
            }

            newBil.setPharmaceuticalBillItem(newPhar);
            getBillItemFacade().edit(newBil);

            getSaleBill().getBillItems().add(newBil);

            tbi.setReferanceBillItem(newBil);
            getBillItemFacade().edit(tbi);
        }

        getBillFacade().edit(getSaleBill());
    }

    private void saveSaleBillItems(List<BillItem> list, Payment p) {
        for (BillItem tbi : list) {

            BillItem newBil = new BillItem();

            newBil.copy(tbi);
            newBil.setReferanceBillItem(tbi);
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

            getSaleBill().getBillItems().add(newBil);

            tbi.setReferanceBillItem(newBil);
            getBillItemFacade().edit(tbi);
            saveBillFee(newBil, p);
        }

        getBillFacade().edit(getSaleBill());
    }

    private boolean checkAllBillItem() {
        for (BillItem b : getPreBill().getBillItems()) {

            if (onEdit(b)) {
                return true;
            }
        }

        return false;

    }

    public void settlePharmacyToken(TokenType tokenType) {
        currentToken = new Token();
        currentToken.setTokenType(tokenType);
        currentToken.setDepartment(sessionController.getDepartment());
        currentToken.setFromDepartment(sessionController.getDepartment());
        currentToken.setPatient(getPatient());
        currentToken.setInstitution(sessionController.getInstitution());
        currentToken.setFromInstitution(sessionController.getInstitution());
        if (getCounter() == null) {
            if (sessionController.getLoggableSubDepartments() != null
                    && !sessionController.getLoggableSubDepartments().isEmpty()) {
                counter = sessionController.getLoggableSubDepartments().get(0);
            }
        }
        currentToken.setCounter(getCounter());
        if (counter != null) {
            currentToken.setToDepartment(counter.getSuperDepartment());
            if (counter.getSuperDepartment() != null) {
                currentToken.setToInstitution(counter.getSuperDepartment().getInstitution());
            }
        }
        if (getPatient().getId() == null) {
            JsfUtil.addErrorMessage("Please select a patient");
            return;
        } else if (getPatient().getPerson().getName() == null) {
            JsfUtil.addErrorMessage("Please select a patient");
            return;
        } else if (getPatient().getPerson().getName().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a patient");
            return;
        } else {
            Patient pt = savePatient();
            currentToken.setPatient(pt);
        }
        if (currentToken.getToDepartment() == null) {
            currentToken.setToDepartment(sessionController.getDepartment());
        }
        if (currentToken.getToInstitution() == null) {
            currentToken.setToInstitution(sessionController.getInstitution());
        }
        tokenFacade.create(currentToken);
        currentToken.setTokenNumber(billNumberBean.generateDailyTokenNumber(currentToken.getFromDepartment(), null, null, tokenType));
        currentToken.setCounter(counter);
        currentToken.setTokenDate(new Date());
        currentToken.setTokenAt(new Date());
        currentToken.setBill(getPreBill());
        tokenFacade.edit(currentToken);
        setToken(currentToken);
    }

    public String settlePreBillAndNavigateToPrint() {
        configOptionFacade.flush();
        editingQty = null;

        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Items added to bill to sale");
            return null;
        }

        if (!getPreBill().getBillItems().isEmpty()) {
            for (BillItem bi : getPreBill().getBillItems()) {
                if (!userStockController.isStockAvailable(bi.getPharmaceuticalBillItem().getStock(), bi.getQty(), getSessionController().getLoggedUser())) {
                    setZeroToQty(bi);
                    JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
                    return null;
                }
                if (bi.getQty() <= 0.0) {
                    JsfUtil.addErrorMessage("Some BillItem Quntity is Zero or less than Zero");
                    return null;
                }
            }
        }
        if (getPreBill().isCancelled() == true) {
            getPreBill().setCancelled(false);
        }

        if (getPaymentMethod() == null) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("Please select Payment Method");
            return null;
        }

        BooleanMessage discountSchemeValidation = discountSchemeValidationService.validateDiscountScheme(paymentMethod, paymentScheme);
        if (!discountSchemeValidation.isFlag()) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage(discountSchemeValidation.getMessage());
            return null;
        }

        // Pharmacy Sale Validation - Patient and Patient Details
        boolean patientRequired = configOptionApplicationController.getBooleanValueByKey("Patient is required in Pharmacy Retail Sale", false);
        boolean patientRequiredForPharmacySale = patientRequired; // Keep for backward compatibility with code below

        if (patientRequired) {
            if (getPatient() == null || getPatient().getPerson() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Patient is required.");
                return null;
            }
        }

        // Only validate patient details if patient is required OR if patient exists
        boolean hasPatient = getPatient() != null && getPatient().getPerson() != null;

        if (hasPatient || patientRequired) {
            // Patient Name validation
            if (configOptionApplicationController.getBooleanValueByKey("Patient Name is required in Pharmacy Retail Sale", false)) {
                if (getPatient() == null || getPatient().getPerson() == null
                        || getPatient().getPerson().getName() == null
                        || getPatient().getPerson().getName().trim().isEmpty()) {
                    billSettlingStarted = false;
                    JsfUtil.addErrorMessage("Patient name is required.");
                    return null;
                }
            }

            // Patient Phone validation
            if (configOptionApplicationController.getBooleanValueByKey("Patient Phone is required in Pharmacy Retail Sale", false)) {
                if (getPatient() == null || getPatient().getPerson() == null) {
                    billSettlingStarted = false;
                    JsfUtil.addErrorMessage("Patient is required.");
                    return null;
                }
                // Check both phone and mobile - at least one should be present
                boolean hasPhone = getPatient().getPerson().getPhone() != null
                        && !getPatient().getPerson().getPhone().trim().isEmpty();
                boolean hasMobile = getPatient().getPerson().getMobile() != null
                        && !getPatient().getPerson().getMobile().trim().isEmpty();

                if (!hasPhone && !hasMobile) {
                    billSettlingStarted = false;
                    JsfUtil.addErrorMessage("Patient phone number is required.");
                    return null;
                }
            }

            // Patient Gender validation
            if (configOptionApplicationController.getBooleanValueByKey("Patient Gender is required in Pharmacy Retail Sale", false)) {
                if (getPatient() == null || getPatient().getPerson() == null
                        || getPatient().getPerson().getSex() == null) {
                    billSettlingStarted = false;
                    JsfUtil.addErrorMessage("Patient gender is required.");
                    return null;
                }
            }

            // Patient Address validation
            if (configOptionApplicationController.getBooleanValueByKey("Patient Address is required in Pharmacy Retail Sale", false)) {
                if (getPatient() == null || getPatient().getPerson() == null
                        || getPatient().getPerson().getAddress() == null
                        || getPatient().getPerson().getAddress().trim().isEmpty()) {
                    billSettlingStarted = false;
                    JsfUtil.addErrorMessage("Patient address is required.");
                    return null;
                }
            }

            // Patient Area validation
            if (configOptionApplicationController.getBooleanValueByKey("Patient Area is required in Pharmacy Retail Sale", false)) {
                if (getPatient() == null || getPatient().getPerson() == null
                        || getPatient().getPerson().getArea() == null) {
                    billSettlingStarted = false;
                    JsfUtil.addErrorMessage("Patient area is required.");
                    return null;
                }
            }
        }

        // Referring Doctor validation
        if (configOptionApplicationController.getBooleanValueByKey("Referring Doctor is required in Pharmacy Retail Sale", false)) {
            if (getPreBill() == null || getPreBill().getReferredBy() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Referring doctor is required.");
                return null;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Patient Phone number is mandotary in sale for cashier", true)) {
            if (getPatient().getPatientPhoneNumber() == null && getPatient().getPatientMobileNumber() == null) {
                JsfUtil.addErrorMessage("Please enter phone number of the patient");
                return null;
            } else if (getPatient().getId() == null) {
                if (getPatient().getPatientPhoneNumber() != null && !(String.valueOf(getPatient().getPatientPhoneNumber()).length() >= 9)) {
                    JsfUtil.addErrorMessage("Please enter valid phone number with more than or equal 10 digits of the patient");
                    return null;
                } else if (getPatient().getPatientMobileNumber() != null && !(String.valueOf(getPatient().getPatientMobileNumber()).length() >= 9)) {
                    JsfUtil.addErrorMessage("Please enter valid mobile number with more than or equal 10 digits of the patient");
                    return null;
                }
            }
        }

        Patient pt = null;
        if (getPatient() != null && getPatient().getPerson() != null) {
            String name = getPatient().getPerson().getName();
            boolean hasValidName = name != null && !name.trim().isEmpty();

            if (patientRequiredForPharmacySale) {
                if (!hasValidName) {
                    JsfUtil.addErrorMessage("Please Select a Patient");
                    billSettlingStarted = false;
                    return null;
                } else {
                    pt = savePatient();
                }
            } else {
                if (hasValidName) {
                    pt = savePatient();
                }
            }
        } else if (patientRequiredForPharmacySale) {
            JsfUtil.addErrorMessage("Please Select a Patient");
            billSettlingStarted = false;
            return null;
        }

        if (billPreview) {

        }

        calculateAllRates();

        List<BillItem> tmpBillItems = new ArrayList<>(getPreBill().getBillItems());
        getPreBill().setBillItems(null);
        getPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);

        savePreBillFinallyForRetailSaleForCashier(pt);
        savePreBillItemsFinally(tmpBillItems);
        setPrintBill(getBillFacade().find(getPreBill().getId()));
        if (configOptionController.getBooleanValueByKey("Enable token system in sale for cashier", false)) {
            if (getPatient() != null) {
                Token t = tokenController.findPharmacyTokens(getPreBill());
                if (t == null) {
                    Token saleForCashierToken = tokenController.findPharmacyTokenSaleForCashier(getPreBill(), TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER);
                    if (saleForCashierToken == null) {
                        settlePharmacyToken(TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER);
                    }
                    markInprogress();

                } else if (t != null) {
                    markToken();
                }
            }

        }

        if (getCurrentToken() != null) {
            getCurrentToken().setBill(getPreBill());
            tokenFacade.edit(getCurrentToken());
        }

        resetAll();
        billPreview = true;
        return navigateToSaleBillForCashierPrint();
    }

    public String navigateToSaleBillForCashierPrint() {
        return "/pharmacy/printing/retail_sale_for_cashier?faces-redirect=true";
    }

    @Deprecated // Plse use settlePreBillAndNavigateToPrint
    public void settlePreBill() {
        configOptionFacade.flush();
        editingQty = null;

        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Items added to bill to sale");
            return;
        }

        if (!getPreBill().getBillItems().isEmpty()) {
            for (BillItem bi : getPreBill().getBillItems()) {
                if (!userStockController.isStockAvailable(bi.getPharmaceuticalBillItem().getStock(), bi.getQty(), getSessionController().getLoggedUser())) {
                    setZeroToQty(bi);
                    JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
                    return;
                }
                if (bi.getQty() <= 0.0) {
                    JsfUtil.addErrorMessage("Some BillItem Quntity is Zero or less than Zero");
                    return;
                }
            }
        }
        if (getPreBill().isCancelled() == true) {
            getPreBill().setCancelled(false);
        }

        if (getPaymentMethod() == null) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("Please select Payment Method");
            return;
        }

        BooleanMessage discountSchemeValidation = discountSchemeValidationService.validateDiscountScheme(paymentMethod, paymentScheme);
        if (!discountSchemeValidation.isFlag()) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage(discountSchemeValidation.getMessage());
            return;
        }

        boolean patientRequiredForPharmacySale = configOptionApplicationController.getBooleanValueByKey(
                "Patient is required in Pharmacy Retail Sale Bill for " + sessionController.getDepartment().getName(),
                false
        );

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Title And Gender To Save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getTitle() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select title.");
                return;
            }
            if (getPatient().getPerson().getSex() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select gender.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Name to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getName() == null || getPatient().getPerson().getName().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter name.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Age to Save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getDob() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient date of birth.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Phone Number to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getPhone() == null || getPatient().getPerson().getPhone().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter phone number.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Address to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getAddress() == null || getPatient().getPerson().getAddress().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient address.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Mail to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getEmail() == null || getPatient().getPerson().getEmail().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient email.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient NIC to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getNic() == null || getPatient().getPerson().getNic().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient NIC.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Area to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getArea() == null || getPatient().getPerson().getArea().getName().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select patient area.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Referring Doctor to settlle bill in Pharmacy Sale", false)) {
            if (getPreBill().getReferredBy() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select referring doctor.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Patient Phone number is mandotary in sale for cashier", true)) {
            if (getPatient().getPatientPhoneNumber() == null && getPatient().getPatientMobileNumber() == null) {
                JsfUtil.addErrorMessage("Please enter phone number of the patient");
                return;
            } else if (getPatient().getId() == null) {
                if (getPatient().getPatientPhoneNumber() != null && !(String.valueOf(getPatient().getPatientPhoneNumber()).length() >= 9)) {
                    JsfUtil.addErrorMessage("Please enter valid phone number with more than or equal 10 digits of the patient");
                    return;
                } else if (getPatient().getPatientMobileNumber() != null && !(String.valueOf(getPatient().getPatientMobileNumber()).length() >= 9)) {
                    JsfUtil.addErrorMessage("Please enter valid mobile number with more than or equal 10 digits of the patient");
                    return;
                }
            }
        }

        Patient pt = null;
        if (getPatient() != null && getPatient().getPerson() != null) {
            String name = getPatient().getPerson().getName();
            boolean hasValidName = name != null && !name.trim().isEmpty();

            if (patientRequiredForPharmacySale) {
                if (!hasValidName) {
                    JsfUtil.addErrorMessage("Please Select a Patient");
                    billSettlingStarted = false;
                    return;
                } else {
                    pt = savePatient();
                }
            } else {
                if (hasValidName) {
                    pt = savePatient();
                }
            }
        } else if (patientRequiredForPharmacySale) {
            JsfUtil.addErrorMessage("Please Select a Patient");
            billSettlingStarted = false;
            return;
        }

        if (billPreview) {

        }

        calculateAllRates();

        List<BillItem> tmpBillItems = new ArrayList<>(getPreBill().getBillItems());
        getPreBill().setBillItems(null);
        getPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);

        savePreBillFinallyForRetailSaleForCashier(pt);
        savePreBillItemsFinally(tmpBillItems);
        Long id = getPreBill().getId();
        if (id == null) {
            JsfUtil.addErrorMessage("Pre-bill is not persisted; cannot load for printing");
            return;
        }
        setPrintBill(getBillFacade().find(id));
        if (configOptionController.getBooleanValueByKey("Enable token system in sale for cashier", false)) {

            if (getPatient() != null) {
                Token t = tokenController.findPharmacyTokens(getPreBill());
                if (t == null) {
                    Token saleForCashierToken = tokenController.findPharmacyTokenSaleForCashier(getPreBill(), TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER);
                    if (saleForCashierToken == null) {
                        settlePharmacyToken(TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER);
                    }
                    markInprogress();

                } else if (t != null) {
                    markToken();
                }
            }

        }

        if (getCurrentToken() != null) {
            getCurrentToken().setBill(getPreBill());
            tokenFacade.edit(getCurrentToken());
        }

        resetAll();
        billPreview = true;
    }

    public void markInprogress() {
        Token t = getToken();
        if (t == null) {
            return;
        }
        t.setBill(getPreBill());
        t.setCalled(false);
        t.setCalledAt(null);
        t.setInProgress(true);
        t.setCompleted(false);
        tokenController.save(t);
    }

    public void markToken() {
        Token t = getToken();
        if (t == null) {
            return;
        }
        t.setBill(getPreBill());
        t.setCalled(true);
        t.setCalledAt(new Date());
        t.setInProgress(false);
        t.setCompleted(false);
        tokenController.save(t);
    }

    @EJB
    private CashTransactionBean cashTransactionBean;

    public boolean errorCheckOnPaymentMethod() {
        if (getPaymentSchemeController().checkPaymentMethodError(paymentMethod, getPaymentMethodData())) {
            return true;
        }

        syncStaffSelectionFromPaymentDetails(paymentMethod);

        if (paymentMethod == PaymentMethod.PatientDeposit) {
//            if (!getPatient().getHasAnAccount()) {
//                JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
//                return true;
//            }
            double creditLimitAbsolute = 0.0;
//            if (getPatient().getCreditLimit() == null) {
//                creditLimitAbsolute = 0.0;
//            } else {
//                creditLimitAbsolute = Math.abs(getPatient().getCreditLimit());
//            }
//
            double runningBalance;
//            if (getPatient().getRunningBalance() != null) {
//                runningBalance = getPatient().getRunningBalance();
//            } else {
//                runningBalance = 0.0;
//            }
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(getPatient(), sessionController.getDepartment());
            runningBalance = pd.getBalance();
            double availableForPurchase = runningBalance + creditLimitAbsolute;

            if (netTotal > availableForPurchase) {
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
        if (paymentMethod == PaymentMethod.Staff) {
            if (toStaff == null) {
                JsfUtil.addErrorMessage("Please select Staff Member.");
                return true;
            }

            if (toStaff.getCurrentCreditValue() + netTotal > toStaff.getCreditLimitQualified()) {
                JsfUtil.addErrorMessage("No enough Credit.");
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
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (cd.getPaymentMethod() != null) {
                    if (cd.getPaymentMethod().equals(PaymentMethod.PatientDeposit)) {
                        double creditLimitAbsolute = 0.0;
                        PatientDeposit pd = patientDepositController.getDepositOfThePatient(getPatient(), sessionController.getDepartment());

                        if (pd == null) {
                            JsfUtil.addErrorMessage("No Patient Deposit.");
                            return true;
                        }

                        double runningBalance = pd.getBalance();
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
                        Staff selectedStaff = cd.getPaymentMethodData().getStaffCredit().getToStaff();
                        if (selectedStaff.getCurrentCreditValue() + cd.getPaymentMethodData().getStaffCredit().getTotalValue() > selectedStaff.getCreditLimitQualified()) {
                            JsfUtil.addErrorMessage("No enough Credit.");
                            return true;
                        }
                    } else if (cd.getPaymentMethod().equals(PaymentMethod.Staff_Welfare)) {
                        if (cd.getPaymentMethodData().getStaffWelfare().getTotalValue() == 0.0 || cd.getPaymentMethodData().getStaffWelfare().getToStaff() == null) {
                            JsfUtil.addErrorMessage("Please fill the Paying Amount and Staff Name");
                            return true;
                        }
                        Staff welfareStaff = cd.getPaymentMethodData().getStaffWelfare().getToStaff();
                        double utilized = Math.abs(welfareStaff.getAnnualWelfareUtilized());
                        if (utilized + cd.getPaymentMethodData().getStaffWelfare().getTotalValue() > welfareStaff.getAnnualWelfareQualified()) {
                            JsfUtil.addErrorMessage("No enough credit.");
                            return true;
                        }
                    }
                    // Aggregate only the relevant payment method total
                    switch (cd.getPaymentMethod()) {
                        case Cash:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                            break;
                        case Card:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                            break;
                        case Cheque:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                            break;
                        case ewallet:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                            break;
                        case PatientDeposit:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                            break;
                        case Slip:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                            break;
                        case Staff:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();
                            break;
                        case Staff_Welfare:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffWelfare().getTotalValue();
                            break;
                        default:
                            break;
                    }
                }
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

    public List<Payment> createPaymentsForBill(Bill b) {
        return createMultiplePayments(b, b.getPaymentMethod());
    }

    public List<Payment> createMultiplePayments(Bill bill, PaymentMethod pm) {
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
                        p.setBank(cd.getPaymentMethodData().getCheque().getInstitution());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCheque().getComment());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        break;
                    case ewallet:
                        p.setPaidValue(cd.getPaymentMethodData().getEwallet().getTotalValue());
                        p.setPolicyNo(cd.getPaymentMethodData().getEwallet().getReferralNo());
                        p.setComments(cd.getPaymentMethodData().getEwallet().getComment());
                        p.setReferenceNo(cd.getPaymentMethodData().getEwallet().getReferenceNo());
                        p.setBank(cd.getPaymentMethodData().getEwallet().getInstitution());
                        break;
                    case Agent:
                    case PatientDeposit:
                        p.setPaidValue(cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                        break;
                    case Credit:
                        p.setPolicyNo(cd.getPaymentMethodData().getCredit().getReferralNo());
                        p.setReferenceNo(cd.getPaymentMethodData().getCredit().getReferenceNo());
                        p.setCreditCompany(cd.getPaymentMethodData().getCredit().getInstitution());
                        p.setPaidValue(cd.getPaymentMethodData().getCredit().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCredit().getComment());
                        break;
                    case Slip:
                        p.setPaidValue(cd.getPaymentMethodData().getSlip().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
                        p.setPaymentDate(cd.getPaymentMethodData().getSlip().getDate());
                        p.setComments(cd.getPaymentMethodData().getSlip().getComment());
                        p.setReferenceNo(cd.getPaymentMethodData().getSlip().getReferenceNo());
                        break;
                    case OnCall:
                        break;
                    case OnlineSettlement:
                        p.setPaidValue(cd.getPaymentMethodData().getOnlineSettlement().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getOnlineSettlement().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getOnlineSettlement().getDate());
                        p.setPaymentDate(cd.getPaymentMethodData().getOnlineSettlement().getDate());
                        p.setReferenceNo(cd.getPaymentMethodData().getOnlineSettlement().getReferenceNo());
                        p.setComments(cd.getPaymentMethodData().getOnlineSettlement().getComment());
                        break;
                    case Staff:
                        p.setToStaff(cd.getPaymentMethodData().getStaffCredit().getToStaff());
                        p.setPaidValue(cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffCredit().getToStaff() != null) {
                            staffBean.updateStaffCredit(cd.getPaymentMethodData().getStaffCredit().getToStaff(), cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                            JsfUtil.addSuccessMessage("Staff Credit Updated");
                        }
                        break;
                    case Staff_Welfare:
                        p.setToStaff(cd.getPaymentMethodData().getStaffWelfare().getToStaff());
                        p.setPaidValue(cd.getPaymentMethodData().getStaffWelfare().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffWelfare().getToStaff() != null) {
                            staffBean.updateStaffWelfare(cd.getPaymentMethodData().getStaffWelfare().getToStaff(), cd.getPaymentMethodData().getStaffWelfare().getTotalValue());
                            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
                        }
                        break;
                    case YouOweMe:
                    case MultiplePaymentMethods:
                        break;
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
                    p.setBank(paymentMethodData.getCheque().getInstitution());
                    p.setPaidValue(paymentMethodData.getCheque().getTotalValue());
                    p.setComments(paymentMethodData.getCheque().getComment());
                    break;
                case Cash:
                    p.setPaidValue(paymentMethodData.getCash().getTotalValue());
                    break;
                case ewallet:
                    p.setPaidValue(paymentMethodData.getEwallet().getTotalValue());
                    p.setPolicyNo(paymentMethodData.getEwallet().getReferralNo());
                    p.setComments(paymentMethodData.getEwallet().getComment());
                    p.setReferenceNo(paymentMethodData.getEwallet().getReferenceNo());
                    p.setBank(paymentMethodData.getEwallet().getInstitution());
                    break;
                case Credit:
                    p.setPolicyNo(paymentMethodData.getCredit().getReferralNo());
                    p.setReferenceNo(paymentMethodData.getCredit().getReferenceNo());
                    p.setCreditCompany(paymentMethodData.getCredit().getInstitution());
                    p.setPaidValue(paymentMethodData.getCredit().getTotalValue());
                    p.setComments(paymentMethodData.getCredit().getComment());
                    getSaleBill().setToInstitution(paymentMethodData.getCredit().getInstitution());
                    getSaleBill().setCreditCompany(paymentMethodData.getCredit().getInstitution());
                    break;
                case Agent:
                case PatientDeposit:
                    p.setPaidValue(paymentMethodData.getPatient_deposit().getTotalValue());
                    break;
                case Slip:
                    p.setPaidValue(paymentMethodData.getSlip().getTotalValue());
                    p.setBank(paymentMethodData.getSlip().getInstitution());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
                    p.setPaymentDate(paymentMethodData.getSlip().getDate());
                    p.setComments(paymentMethodData.getSlip().getComment());
                    p.setReferenceNo(paymentMethodData.getSlip().getReferenceNo());
                    break;
                case OnCall:
                    break;
                case OnlineSettlement:
                    p.setPaidValue(paymentMethodData.getOnlineSettlement().getTotalValue());
                    p.setBank(paymentMethodData.getOnlineSettlement().getInstitution());
                    p.setRealizedAt(paymentMethodData.getOnlineSettlement().getDate());
                    p.setPaymentDate(paymentMethodData.getOnlineSettlement().getDate());
                    p.setReferenceNo(paymentMethodData.getOnlineSettlement().getReferenceNo());
                    p.setComments(paymentMethodData.getOnlineSettlement().getComment());
                    break;
                case Staff:
                    p.setToStaff(paymentMethodData.getStaffCredit().getToStaff());
                    p.setPaidValue(paymentMethodData.getStaffCredit().getTotalValue());
                    if (paymentMethodData.getStaffCredit().getToStaff() != null) {
                        staffBean.updateStaffCredit(paymentMethodData.getStaffCredit().getToStaff(), paymentMethodData.getStaffCredit().getTotalValue());
                        JsfUtil.addSuccessMessage("Staff Credit Updated");
                    }
                    break;
                case Staff_Welfare:
                    p.setToStaff(paymentMethodData.getStaffWelfare().getToStaff());
                    p.setPaidValue(paymentMethodData.getStaffWelfare().getTotalValue());
                    if (paymentMethodData.getStaffWelfare().getToStaff() != null) {
                        staffBean.updateStaffWelfare(paymentMethodData.getStaffWelfare().getToStaff(), paymentMethodData.getStaffWelfare().getTotalValue());
                        JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
                    }
                    break;
                case YouOweMe:
                case MultiplePaymentMethods:
            }

            p.setPaidValue(p.getBill().getNetTotal());
            paymentFacade.create(p);

            ps.add(p);
        }
        return ps;
    }

    public void settleBillWithPay() {
        editingQty = null;

        if (billSettlingStarted) {
            return;
        }

        billSettlingStarted = true;
        if (sessionController.getApplicationPreference().isCheckPaymentSchemeValidation()) {
            if (getPaymentScheme() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select Payment Scheme");
                return;
            }
        }

        if (getPaymentMethod() == null) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("Please select Payment Method");
            return;
        }

        if (getPreBill().getBillItems().isEmpty()) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("Please add items to the bill.");
            return;
        }
        if ((getPatient().getMobileNumberStringTransient() == null
                || getPatient().getMobileNumberStringTransient().trim().isEmpty() || getPatient().getPerson().getName().trim().isEmpty())
                && configOptionApplicationController.getBooleanValueByKey("Patient details are required for retail sale")) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("Please enter patient name and mobile number.");
            return;
        }
        if (getPaymentMethod() == PaymentMethod.Card) {
            String cardNumber = getPaymentMethodData().getCreditCard().getNo();
            if ((cardNumber == null || cardNumber.trim().isEmpty()
                    || cardNumber.trim().length() != 4)
                    && configOptionApplicationController.getBooleanValueByKey("Pharmacy retail sale CreditCard last digits is Mandatory")) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter a Credit Card last 4 digits");
                return;
            }
        }

        if (getPaymentMethod() == PaymentMethod.Staff_Welfare && configOptionApplicationController.getBooleanValueByKey("Pharmacy discount should be staff when select Staff_welfare as payment method", false)) {
            if (paymentScheme == null || !paymentScheme.getName().equalsIgnoreCase("staff")) {
                JsfUtil.addErrorMessage("Saff Welfare need to set staff discount scheme.");
                billSettlingStarted = false;
                return;
            }
        }

        BooleanMessage discountSchemeValidation = discountSchemeValidationService.validateDiscountScheme(paymentMethod, paymentScheme, getPaymentMethodData());
        if (!discountSchemeValidation.isFlag()) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage(discountSchemeValidation.getMessage());
            return;
        }

        boolean patientRequiredForPharmacySale = configOptionApplicationController.getBooleanValueByKey(
                "Patient is required in Pharmacy Retail Sale Bill for " + sessionController.getDepartment().getName(),
                false
        );

        if (patientRequiredForPharmacySale) {
            if (getPatient() == null
                    || getPatient().getPerson() == null
                    || getPatient().getPerson().getName() == null
                    || getPatient().getPerson().getName().trim().isEmpty()) {
                JsfUtil.addErrorMessage("Please Select a Patient");
                billSettlingStarted = false;
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Title And Gender To Save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getTitle() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select title.");
                return;
            }
            if (getPatient().getPerson().getSex() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select gender.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Name to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getName() == null || getPatient().getPerson().getName().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter name.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Age to Save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getDob() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient date of birth.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Phone Number to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getPhone() == null || getPatient().getPerson().getPhone().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter phone number.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Address to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getAddress() == null || getPatient().getPerson().getAddress().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient address.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Mail to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getEmail() == null || getPatient().getPerson().getEmail().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient email.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient NIC to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getNic() == null || getPatient().getPerson().getNic().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient NIC.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Area to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getArea() == null || getPatient().getPerson().getArea().getName().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select patient area.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Referring Doctor to settlle bill in Pharmacy Sale", false)) {
            if (getPreBill().getReferredBy() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select referring doctor.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
            if (allergyListOfPatient == null) {
                allergyListOfPatient = pharmacyService.getAllergyListForPatient(patient);
            }
            String allergyMsg = pharmacyService.isAllergyForPatient(patient, getPreBill().getBillItems(), allergyListOfPatient);
            if (!allergyMsg.isEmpty()) {
                JsfUtil.addErrorMessage(allergyMsg);
                billSettlingStarted = false;
                return;
            }
        }

        if (!getPreBill().getBillItems().isEmpty()) {
            for (BillItem bi : getPreBill().getBillItems()) {
                if (!userStockController.isStockAvailable(bi.getPharmaceuticalBillItem().getStock(), bi.getQty(), getSessionController().getLoggedUser())) {
                    setZeroToQty(bi);
//                    onEditCalculation(bi);
                    JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
                    billSettlingStarted = false;
                    return;
                }
                ////System.out.println("bi.getItem().getName() = " + bi.getItem().getName());
                ////System.out.println("bi.getQty() = " + bi.getQty());
                if (bi.getQty() <= 0.0) {
                    ////System.out.println("bi.getQty() = " + bi.getQty());
                    JsfUtil.addErrorMessage("Some BillItem Quntity is Zero or less than Zero");
                    billSettlingStarted = false;
                    return;
                }
            }
        }

        Patient pt = savePatient();

        if (errorCheckForSaleBill()) {
            billSettlingStarted = false;
            return;
        }
        if (errorCheckOnPaymentMethod()) {
            billSettlingStarted = false;
            return;
        }

        calculateAllRates();

        getPreBill().setPaidAmount(getPreBill().getTotal());

        List<BillItem> tmpBillItems = getPreBill().getBillItems();
        getPreBill().setBillItems(null);

        savePreBillFinallyForRetailSale(pt);
        savePreBillItemsFinally(tmpBillItems);

        saveSaleBill();
        List<Payment> payments = createPaymentsForBill(getSaleBill());
        drawerController.updateDrawerForIns(payments);
        saveSaleBillItems(tmpBillItems);

        getBillFacade().edit(getPreBill());

        setPrintBill(getBillFacade().find(getSaleBill().getId()));

        // Ensure BillFinanceDetails are populated for accurate reporting
        if (getSaleBill() != null) {
            pharmacyCostingService.updateBillFinanceDetailsForRetailSale(getSaleBill());
            getBillFacade().edit(getSaleBill());
        }

        if (toStaff != null && getPaymentMethod() == PaymentMethod.Credit) {
            getStaffBean().updateStaffCredit(toStaff, netTotal);
            JsfUtil.addSuccessMessage("User Credit Updated");
        }

        paymentService.updateBalances(payments);

        resetAll();
        billSettlingStarted = false;
        billPreview = true;

    }

    public String newPharmacyRetailSale() {
        clearBill();
        clearBillItem();
        billPreview = false;
        setBillSettlingStarted(false);
        return "pharmacy_bill_retail_sale_1";
    }

    //    checked
    private boolean checkItemBatch() {
        for (BillItem bItem : getPreBill().getBillItems()) {
            if (bItem.getPharmaceuticalBillItem().getStock().equals(getBillItem().getPharmaceuticalBillItem().getStock())) {
                return true;
            }
        }
        return false;
    }

    public void addBillItemOld() {
        editingQty = null;

        if (billItem == null) {
            errorMessage = "No Bill Item";
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            errorMessage = "No Pharmaceutical Bill Item";
            return;
        }
        if (getStock() == null) {
            errorMessage = "Please select item";
            return;
        }

        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            errorMessage = "Pls Select Stock";
            return;
        }

        if (getQty() == null) {
            errorMessage = "Quentity?";
            return;
        }

        if (getQty() > getStock().getStock()) {
            errorMessage = "No Sufficient Stocks";
            return;
        }

        if (checkItemBatch()) {
            errorMessage = "Already added this item batch";
            return;
        }

        //Checking User Stock Entity
        if (!userStockController.isStockAvailable(getStock(), getQty(), getSessionController().getLoggedUser())) {
            errorMessage = "Sorry Already Other User Try to Billing This Stock You Cant Add";
            return;
        }

        billItem.setBill(getPreBill());
        billItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(billItem);

        //User Stock Container Save if New Bill
        userStockController.saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());
        UserStock us = userStockController.saveUserStock(billItem, getSessionController().getLoggedUser(), getUserStockContainer());
        billItem.setTransUserStock(us);

        calculateAllRates();

        calTotal();

        clearBillItem();
        setActiveIndex(1);
    }

    public void calTotal() {
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
            getPreBill().setTotal(getPreBill().getTotal() + b.getGrossValue());
        }
        ////System.out.println("1.discount = " + discount);
        netTot = netTot + getPreBill().getServiceCharge();

        getPreBill().setNetTotal(netTot);
        getPreBill().setTotal(grossTot);
        getPreBill().setGrantTotal(grossTot);
        getPreBill().setDiscount(discount);
        setNetTotal(getPreBill().getNetTotal());

    }

    @EJB
    private StockHistoryFacade stockHistoryFacade;

    public void removeBillItem(BillItem b) {
        userStockController.removeUserStock(b.getTransUserStock(), getSessionController().getLoggedUser());
        getPreBill().getBillItems().remove(b.getSearialNo());

        calTotal();
    }

    public void removeSelectedBillItems() {
        if (selectedBillItems == null || selectedBillItems.isEmpty()) {
            JsfUtil.addErrorMessage("Please select items to delete");
            return;
        }

        Iterator<BillItem> iterator = selectedBillItems.iterator();
        while (iterator.hasNext()) {
            BillItem billItem = iterator.next();
            userStockController.removeUserStock(billItem.getTransUserStock(), getSessionController().getLoggedUser());
            getPreBill().getBillItems().remove(billItem);
            iterator.remove();
        }

        calTotal();
    }

    //    Checked
    public void handleQuentityEntryListner(AjaxBehaviorEvent event) {
        if (stock == null) {
            errorMessage = "No stock";
            return;
        }
        if (getPreBill() == null) {
            errorMessage = "No Pre Bill";
            return;
        }
        if (billItem == null) {
            errorMessage = "No Bill Item";
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            errorMessage = "No Pharmaceutical Bill Item";
            return;
        }
        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            errorMessage = "No Stock. Stock Assigned.";
            getBillItem().getPharmaceuticalBillItem().setStock(stock);
        }
        if (getQty() == null) {
            qty = 0.0;
        }
        billItem.setQty(qty);
        //pharmaceutical Bill Item
        billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);
        //Values
        billItem.setGrossValue(getStock().getItemBatch().getRetailsaleRate() * qty);
    }

    public void calculateBillItemForEditing(BillItem bi) {
        //////System.out.println("calculateBillItemForEditing");
        //////System.out.println("bi = " + bi);
        if (getPreBill() == null || bi == null || bi.getPharmaceuticalBillItem() == null || bi.getPharmaceuticalBillItem().getStock() == null) {
            //////System.out.println("calculateItemForEditingFailedBecause of null");
            return;
        }
        //////System.out.println("bi.getQty() = " + bi.getQty());
        //////System.out.println("bi.getRate() = " + bi.getRate());
        bi.setGrossValue(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate() * bi.getQty());
        bi.setNetValue(bi.getQty() * bi.getNetRate());
        bi.setDiscount(bi.getGrossValue() - bi.getNetValue());
        //////System.out.println("bi.getNetValue() = " + bi.getNetValue());

    }

    //    Checked
    public void handleStockSelect(SelectEvent event) {
        if (stock == null) {
            JsfUtil.addErrorMessage("Empty Stock");
            return;
        }
        getBillItem();
        billItem.getPharmaceuticalBillItem().setStock(stock);

        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            JsfUtil.addErrorMessage("Null Stock");
            return;
        }
        if (billItem.getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
            JsfUtil.addErrorMessage("Null Batch Stock");
            return;
        }
//        getBillItem();
        billItem.setRate(billItem.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setItem(getStock().getItemBatch().getItem());
        //pharmaceutical Bill Item
        billItem.getPharmaceuticalBillItem().setDoe(getStock().getItemBatch().getDateOfExpire());
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
    }

    public void paymentSchemeChanged(AjaxBehaviorEvent ajaxBehavior) {
        calculateAllRates();
    }

    @Deprecated // Use listnerForPaymentMethodChange
    public void changeListener() {
        if (paymentMethod == PaymentMethod.PatientDeposit) {
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
        }
        calculateAllRates();
    }

    //    public void calculateRatesForAllBillItemsInPreBill() {
//        //////System.out.println("calculating all rates");
//        for (BillItem tbi : getPreBill().getBillItems()) {
//            calculateDiscountRates(tbi);
//            calculateBillItemForEditing(tbi);
//        }
//        calTotal();
//    }
    public void calculateRateListner(AjaxBehaviorEvent event) {

    }

    //    Checked
    public void calculateDiscountRates(BillItem bi) {
        bi.setDiscountRate(calculateBillItemDiscountRate(bi));
        bi.setDiscount(bi.getDiscountRate() * bi.getQty());
        bi.setNetRate(bi.getRate() - bi.getDiscountRate());
    }

    @Inject
    PriceMatrixController priceMatrixController;
    @Inject
    MembershipSchemeController membershipSchemeController;

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    //    TO check the functionality
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

        if (getPaymentScheme() != null && discountAllowed) {
            PriceMatrix priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(getPaymentMethod(), getPaymentScheme(), getSessionController().getDepartment(), bi.getItem());

            if (priceMatrix != null) {
                bi.setPriceMatrix(priceMatrix);
                discountRate = priceMatrix.getDiscountPercent();
            }

            double dr;
            dr = (retailRate * discountRate) / 100;
            return dr;

        }

        //PAYMENTMETHOD DISCOUNT
        if (getPaymentMethod() != null && discountAllowed) {
            PriceMatrix priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(getPaymentMethod(), getSessionController().getDepartment(), bi.getItem());

            if (priceMatrix != null) {
                bi.setPriceMatrix(priceMatrix);
                discountRate = priceMatrix.getDiscountPercent();
            }

            double dr;
            dr = (retailRate * discountRate) / 100;
            return dr;

        }

        //CREDIT COMPANY DISCOUNT
        if (getPaymentMethod() == PaymentMethod.Credit && toInstitution != null) {
            discountRate = toInstitution.getPharmacyDiscount();

            double dr;
            dr = (retailRate * discountRate) / 100;
            return dr;
        }
        return 0;

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

    public List<Payment> createPaymentForRetailSaleCancellation(Bill cancellationBill, PaymentMethod inputPaymentMethod) {
        List<Payment> ps = new ArrayList<>();
        if (inputPaymentMethod == null) {
            List<Payment> originalBillPayments = billService.fetchBillPayments(cancellationBill.getBilledBill());
            if (originalBillPayments != null) {
                for (Payment originalBillPayment : originalBillPayments) {
                    Payment p = originalBillPayment.clonePaymentForNewBill();
                    p.invertValues();
                    p.setReferancePayment(originalBillPayment);
                    p.setBill(cancellationBill);
                    p.setInstitution(getSessionController().getInstitution());
                    p.setDepartment(getSessionController().getDepartment());
                    p.setCreatedAt(new Date());
                    p.setCreater(getSessionController().getLoggedUser());
                    paymentFacade.create(p);
                    ps.add(p);
                }
            }
        } else if (inputPaymentMethod == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment p = new Payment();
                p.setBill(cancellationBill);
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
                        p.setComments(cd.getPaymentMethodData().getCash().getComment());
                        break;
                    case ewallet:
                        p.setPaidValue(cd.getPaymentMethodData().getEwallet().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getEwallet().getComment());
                        break;
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
                p.setPaidValue(0 - Math.abs(p.getPaidValue()));
                paymentFacade.create(p);
                ps.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(cancellationBill);
            p.setInstitution(getSessionController().getInstitution());
            p.setDepartment(getSessionController().getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            p.setPaymentMethod(inputPaymentMethod);
            p.setPaidValue(cancellationBill.getNetTotal());

            switch (inputPaymentMethod) {
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
                case Cash:
                    p.setComments(getPaymentMethodData().getCash().getComment());
                    break;
                case ewallet:
                    p.setComments(getPaymentMethodData().getEwallet().getComment());
                    break;

                case Agent:
                case Credit:
                case PatientDeposit:
                case Slip:
                case OnCall:
                case OnlineSettlement:
                case Staff:
                case YouOweMe:
            }

            p.setPaidValue(0 - Math.abs(p.getBill().getNetTotal()));
            paymentFacade.create(p);
            ps.add(p);
        }
        return ps;
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
        bfp.setInstitution(getSessionController().getInstitution());
        bfp.setDepartment(getSessionController().getDepartment());
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        getBillFeePaymentFacade().create(bfp);
    }

    private void clearBill() {
        preBill = null;
        saleBill = null;
        patient = null;
        toInstitution = null;
        toStaff = null;
//        billItems = null;
        patientTabId = "tabPt";
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        paymentScheme = null;
        paymentMethod = PaymentMethod.Cash;
        userStockContainer = null;
        fromOpdEncounter = false;
        opdEncounterComments = null;
        patientSearchTab = 0;
        errorMessage = "";
        comment = null;
        token = null;
        currentToken = null;

    }

    private void clearBillItem() {
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        stockDto = null;
        editingQty = null;
        errorMessage = "";
        // paymentMethod = PaymentMethod.Cash; // Never do this. It shold be done in clear bill item
        paymentMethodData = null;
        setCashPaid(0.0);
        allergyListOfPatient = null;
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

    @Override
    public Patient getPatient() {
        if (patient == null) {
            patient = new Patient();
            patientDetailsEditable = true;
        }
        return patient;
    }

    @Override
    public void setPatient(Patient patient) {
        this.patient = patient;
        selectPaymentSchemeAsPerPatientMembership();
    }

    private void selectPaymentSchemeAsPerPatientMembership() {
        if (patient == null) {
            return;
        }
        if (patient.getPerson().getMembershipScheme() == null) {
            paymentScheme = null;
        } else {
            setPaymentScheme(patient.getPerson().getMembershipScheme().getPaymentScheme());
        }
        listnerForPaymentMethodChange();
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

    public PreBill getPreBill() {
        if (preBill == null) {
            preBill = new PreBill();
            preBill.setBillType(BillType.PharmacyPre);
            preBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
            //   preBill.setPaymentScheme(getPaymentSchemeController().getItems().get(0));
        }
        return preBill;
    }

    public void setPreBill(PreBill preBill) {
        this.preBill = preBill;
    }

    public Bill getSaleBill() {
        if (saleBill == null) {
            saleBill = new BilledBill();
            //   saleBill.setBillType(BillType.PharmacySale);
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

    public PaymentSchemeController getPaymentSchemeController() {
        return PaymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController PaymentSchemeController) {
        this.PaymentSchemeController = PaymentSchemeController;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
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

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public StaffService getStaffBean() {
        return staffBean;
    }

    public void setStaffBean(StaffService staffBean) {
        this.staffBean = staffBean;
    }

    public boolean isFromOpdEncounter() {
        return fromOpdEncounter;
    }

    public void setFromOpdEncounter(boolean fromOpdEncounter) {
        this.fromOpdEncounter = fromOpdEncounter;
    }

    public String getOpdEncounterComments() {
        return opdEncounterComments;
    }

    public void setOpdEncounterComments(String opdEncounterComments) {
        this.opdEncounterComments = opdEncounterComments;
    }

    public int getPatientSearchTab() {
        return patientSearchTab;
    }

    public void setPatientSearchTab(int patientSearchTab) {
        this.patientSearchTab = patientSearchTab;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public UserStockContainerFacade getUserStockContainerFacade() {
        return userStockContainerFacade;
    }

    public void setUserStockContainerFacade(UserStockContainerFacade userStockContainerFacade) {
        this.userStockContainerFacade = userStockContainerFacade;
    }

    public UserStockFacade getUserStockFacade() {
        return userStockFacade;
    }

    public void setUserStockFacade(UserStockFacade userStockFacade) {
        this.userStockFacade = userStockFacade;
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

    @Override
    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public List<BillItem> getSelectedBillItems() {
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }

    public Department getCounter() {
        return counter;
    }

    public void setCounter(Department counter) {
        this.counter = counter;
    }

    @Override
    public void listnerForPaymentMethodChange() {
        if (paymentMethod == PaymentMethod.PatientDeposit) {
            if (patient == null || patient.getId() == null) {
                return; // Patient not selected yet, ignore
            }
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(patient, sessionController.getDepartment());
            if (pd != null && pd.getId() != null) {
                getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                // Set total value to net total only if there's sufficient balance, otherwise set to available balance
                double availableBalance = pd.getBalance();
                if (availableBalance >= netTotal) {
                    getPaymentMethodData().getPatient_deposit().setTotalValue(netTotal);
                } else {
                    getPaymentMethodData().getPatient_deposit().setTotalValue(availableBalance);
                }
            } else {
                getPaymentMethodData().getPatient_deposit().setTotalValue(0.0);
            }
        } else if (paymentMethod == PaymentMethod.Card) {
            getPaymentMethodData().getCreditCard().setTotalValue(netTotal);
        } else if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            if (patient == null || patient.getId() == null) {
                return; // Patient not selected yet, ignore
            }
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
            getPaymentMethodData().getPatient_deposit().setTotalValue(calculatRemainForMultiplePaymentTotal());
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(patient, sessionController.getDepartment());

            if (pd != null && pd.getId() != null) {
                boolean hasPatientDeposit = false;
                for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    if (cd.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                        hasPatientDeposit = true;
                        cd.getPaymentMethodData().getPatient_deposit().setPatient(patient);
                        cd.getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                        cd.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);

                    }
                }
            }

        }
        processBillItems();
    }

    public Prescription getPrescription() {
        if (prescription == null) {
            prescription = new Prescription();
        }
        return prescription;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }

    public boolean isEnableLabelPrintFromSaleView() {
        return enableLabelPrintFromSaleView;
    }

    public void setEnableLabelPrintFromSaleView(boolean enableLabelPrintFromSaleView) {
        this.enableLabelPrintFromSaleView = enableLabelPrintFromSaleView;
    }

    public boolean isBillSettlingStarted() {
        return billSettlingStarted;
    }

    public void setBillSettlingStarted(boolean billSettlingStarted) {
        this.billSettlingStarted = billSettlingStarted;
    }

    public void calculateDobFromAge() {
        if (patient == null) {
            return;
        }
        if (patient.getPerson() == null) {
            return;
        }
        patient.getPerson().calDobFromAge();
    }

    // Getter method for JSF to access the converter
    public StockDtoConverter getStockDtoConverter() {
        return new StockDtoConverter();
    }

    // StockDTO Converter for JSF
    public static class StockDtoConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            try {
                Long id = Long.valueOf(value);
                PharmacySaleController controller = (PharmacySaleController) facesContext.getApplication().getELResolver()
                        .getValue(facesContext.getELContext(), null, "pharmacySaleController1");
                if (controller != null && controller.getStockDto() != null && id.equals(controller.getStockDto().getId())) {
                    return controller.getStockDto();
                }
                // Create a minimal DTO with just the ID for form submission
                StockDTO dto = new StockDTO();
                dto.setId(id);
                return dto;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
            if (value == null) {
                return "";
            }
            if (value instanceof StockDTO) {
                StockDTO stockDto = (StockDTO) value;
                return stockDto.getId() != null ? stockDto.getId().toString() : "";
            }
            return "";
        }
    }

}
