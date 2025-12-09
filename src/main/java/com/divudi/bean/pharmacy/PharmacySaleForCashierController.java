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
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyService;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.StaffService;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
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
//Removed for performance optimization
//import com.divudi.core.entity.pharmacy.UserStock;
//import com.divudi.core.entity.pharmacy.UserStockContainer;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ConfigOptionFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.StockHistoryFacade;
import com.divudi.core.facade.TokenFacade;
//Removed for performance optimization
//import com.divudi.core.facade.UserStockContainerFacade;
//import com.divudi.core.facade.UserStockFacade;
import com.divudi.service.AuditService;
import com.divudi.service.BillService;
import com.divudi.service.DiscountSchemeValidationService;
import com.divudi.service.PaymentService;

import java.io.Serializable;
import java.math.BigDecimal;
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
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
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
import org.primefaces.PrimeFaces;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PharmacySaleForCashierController implements Serializable, ControllerWithPatient, ControllerWithMultiplePayments {

    //Removed for performance optimization
//    @Inject
//    private UserStockController userStockController;
    @Inject
    private PriceMatrixController priceMatrixController;
    @Inject
    private PaymentSchemeController PaymentSchemeController;
    @Inject
    private StockController stockController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private ConfigOptionController configOptionController;
    @Inject
    private FinancialTransactionController financialTransactionController;
    @Inject
    private SessionController sessionController;
    @Inject
    private SearchController searchController;
    @Inject
    private PatientDepositController patientDepositController;
    @Inject
    private BillBeanController billBean;
    @Inject
    private TokenController tokenController;
    @Inject
    private DrawerController drawerController;
    @EJB
    private ConfigOptionFacade configOptionFacade;
    @EJB
    private CashTransactionBean cashTransactionBean;
    @EJB
    private StockHistoryFacade stockHistoryFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private DiscountSchemeValidationService discountSchemeValidationService;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private StockFacade stockFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private StaffService staffBean;
    @EJB
    private PaymentService paymentService;
    //Removed for performance optimization
//    @EJB
//    private UserStockContainerFacade userStockContainerFacade;
//    @EJB
//    private UserStockFacade userStockFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private TokenFacade tokenFacade;
    @EJB
    private PharmacyService pharmacyService;
    @EJB
    private BillService billService;
    @EJB
    private AuditService auditService;
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
    //Removed for performance optimization
    //private UserStockContainer userStockContainer;
    PaymentMethodData paymentMethodData;
    private boolean patientDetailsEditable;
    private Department counter;
    Token currentToken;

    PaymentMethod paymentMethod;

    /**
     * Creates a new instance of PharmacySaleController
     */
    public PharmacySaleForCashierController() {
    }

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
                return "/pharmacy/pharmacy_bill_retail_sale_for_cashier?faces-redirect=true";
            } else {
                setBillSettlingStarted(false);
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/pharmacy/pharmacy_bill_retail_sale_for_cashier?faces-redirect=true";
            }
        } else {
            resetAll();
            setBillSettlingStarted(false);
            return "/pharmacy/pharmacy_bill_retail_sale_for_cashier?faces-redirect=true";
        }
    }

    public String navigateToPharmacyBillForCashierWholeSale() {
        setBillSettlingStarted(false);
        return "/pharmacy_wholesale/pharmacy_bill_retail_sale_for_cashier?faces-redirect=true";
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
        calculateRatesForAllBillItemsInPreBill();
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
        calculateRatesForAllBillItemsInPreBill();

    }

    @Override
    public double calculatRemainForMultiplePaymentTotal() {

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (cd == null) {
                    continue;
                }
                if (cd.getPaymentMethodData() != null && cd.getPaymentMethod() != null) {
                    // Only add the value from the selected payment method for this ComponentDetail
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
                        case Credit:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCredit().getTotalValue();
                            break;
                        case OnlineSettlement:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getOnlineSettlement().getTotalValue();
                            break;
                        default:
                            break;
                    }
                }
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
            if (arrSize == 0) {
                return; // No payment methods added yet
            }
            ComponentDetail pm = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);
            if (pm.getPaymentMethodData() == null) {
                return; // Payment method data not initialized
            }
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

    private void cleanupInvalidPaymentDetails() {
        if (paymentMethodData == null
                || paymentMethodData.getPaymentMethodMultiple() == null
                || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
            return;
        }

        // Remove ComponentDetails with null paymentMethodData or null paymentMethod
        paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()
                .removeIf(cd -> cd.getPaymentMethodData() == null || cd.getPaymentMethod() == null);
    }

    public boolean isLastPaymentEntry(ComponentDetail cd) {
        if (cd == null
                || paymentMethodData == null
                || paymentMethodData.getPaymentMethodMultiple() == null
                || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null
                || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
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

    public void onEdit(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
        if (tmp == null) {
            return;
        }
        execureOnEditActions(tmp);
    }

    private void setZeroToQty(BillItem tmp) {
        tmp.setQty(0.0);
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0.0f);

        //UserStock update removed for performance optimization
    }

    //Check when edititng Qty
    //
    public boolean execureOnEditActions(BillItem tmp) {
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

        //Check Is There Any Other User using same Stock - REMOVED for performance optimization

        //UserStock update removed for performance optimization

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
        tmp.getPharmaceuticalBillItem().setQty(0 - tmp.getQty());

        calculateBillItemForEditing(tmp);

        calculateBillItemsAndBillTotalsOfPreBill();

    }

    public void quantityInTableChangeEvent(BillItem tmp) {
        if (tmp == null) {
            return;
        }

        tmp.setGrossValue(tmp.getQty() * tmp.getRate());
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0 - tmp.getQty());

        calculateBillItemForEditing(tmp);

        calculateBillItemsAndBillTotalsOfPreBill();

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

        calculateBillItemsAndBillTotalsOfPreBill();
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

    /**
     * Converts a StockDTO to a minimal Stock entity without database queries.
     * Creates detached entities populated only with data from the DTO.
     * This avoids JPA proxy issues and LazyInitializationException.
     *
     * @param stockDto The DTO containing all necessary data
     * @return A minimal Stock entity with ItemBatch and Item populated from DTO
     */
    public Stock convertStockDtoToEntity(StockDTO stockDto) {
        long conversionStart = System.currentTimeMillis();
        System.out.println("  >> convertStockDtoToEntity: Start");

        if (stockDto == null || stockDto.getId() == null) {
            System.out.println("  >> convertStockDtoToEntity: stockDto is null or has no ID - returning null");
            return null;
        }

        // Debug: Print DTO data
        System.out.println("    >>> DTO Details - StockId: " + stockDto.getId() +
                          ", ItemBatchId: " + stockDto.getItemBatchId() +
                          ", ItemId: " + stockDto.getItemId() +
                          ", CostRate: " + stockDto.getCostRate());

        // Check if we have the necessary data for lightweight creation
        if (stockDto.getItemBatchId() != null && stockDto.getItemId() != null) {
            long entityCreationStart = System.currentTimeMillis();
            System.out.println("    >>> Using lightweight DTO conversion (NO database query)");

            // Create minimal Stock entity with data from DTO
            Stock stock = new Stock();
            stock.setId(stockDto.getId());
            stock.setStock(stockDto.getStockQty());

            // Create minimal ItemBatch with data from DTO
            ItemBatch itemBatch = new ItemBatch();
            itemBatch.setId(stockDto.getItemBatchId());
            itemBatch.setRetailsaleRate(stockDto.getRetailRate());
            itemBatch.setDateOfExpire(stockDto.getDateOfExpire());
            itemBatch.setBatchNo(stockDto.getBatchNo());
            itemBatch.setCostRate(stockDto.getCostRate());

            // Create minimal Item (we use Amp as a concrete implementation)
            // Note: This is a detached entity used only for reference
            // The actual fetch happens when saving the bill if needed
            Item item = createMinimalItemFromDto(stockDto);

            itemBatch.setItem(item);
            stock.setItemBatch(itemBatch);

            System.out.println("    >>> Entity creation time: " + (System.currentTimeMillis() - entityCreationStart) + "ms");
            System.out.println("  >> convertStockDtoToEntity: Total time = " + (System.currentTimeMillis() - conversionStart) + "ms");
            return stock;
        }

        // Fallback to database fetch if DTO doesn't have all required data
        // This ensures backward compatibility with existing code
        System.out.println("    >>> WARNING: Falling back to database fetch (missing DTO data)");
        long dbFetchStart = System.currentTimeMillis();
        Stock result = stockFacade.find(stockDto.getId());
        System.out.println("    >>> Database fetch time: " + (System.currentTimeMillis() - dbFetchStart) + "ms");
        System.out.println("  >> convertStockDtoToEntity: Total time = " + (System.currentTimeMillis() - conversionStart) + "ms");
        return result;
    }

    /**
     * Creates a minimal Item entity from StockDTO data.
     * Since Item is abstract, we create an Amp instance.
     * This is safe because we only use properties set from DTO data.
     *
     * @param stockDto The DTO containing item data
     * @return A minimal Amp instance with properties from DTO
     */
    private Item createMinimalItemFromDto(StockDTO stockDto) {
        // Create a minimal Amp instance (Item is abstract)
        Amp item = new Amp();
        item.setId(stockDto.getItemId());
        item.setName(stockDto.getItemName());
        item.setCode(stockDto.getCode());
        // Set discountAllowed from DTO, defaulting to true for safety
        item.setDiscountAllowed(stockDto.getDiscountAllowed() != null ? stockDto.getDiscountAllowed() : true);
        return item;
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
        return "pharmacy_bill_retail_sale";
    }

    public String newSaleBillWithoutReduceStockForCashier() {
        clearBill();
        clearBillItem();
        billPreview = false;
        setBillSettlingStarted(false);
        return "pharmacy_bill_retail_sale_for_cashier";
    }

    public String navigateToPharmacyRetailSale() {
        if (sessionController.getPharmacyBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                resetAll();
                setBillSettlingStarted(false);
                return "/pharmacy/pharmacy_bill_retail_sale?faces-redirect=true";
            } else {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        } else {
            resetAll();
            setBillSettlingStarted(false);
            setBillSettlingStarted(false);
            return "/pharmacy/pharmacy_bill_retail_sale?faces-redirect=true";
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
        return "/pharmacy/pharmacy_bill_retail_sale?faces-redirect=true";
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
        return "/pharmacy/pharmacy_bill_retail_sale_for_cashier?faces-redirect=true";
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
        //UserStock cleanup removed for performance optimization
        clearBill();
        clearBillItem();
//        searchController.createPreBillsNotPaid();
        billPreview = false;
    }

    public void prepareForNewPharmacyRetailBill() {
        //UserStock cleanup removed for performance optimization
        clearBill();
        clearBillItem();
//        searchController.createPreBillsNotPaid();
        billPreview = false;
    }

    public String pharmacyRetailSale() {
        setBillSettlingStarted(false);
        return "/pharmacy_wholesale/pharmacy_bill_retail_sale?faces-redirect=true";
    }

    public String toPharmacyRetailSale() {
        setBillSettlingStarted(false);
        return "/pharmacy/pharmacy_bill_retail_sale?faces-redirect=true";
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

    public List<StockDTO> completeAvailableStockOptimized(String qry) {
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
                .append("i.id, i.itemBatch.id, i.itemBatch.item.id, i.itemBatch.item.name, i.itemBatch.item.code, i.itemBatch.item.vmp.name, ")
                .append("i.itemBatch.batchNo, i.itemBatch.retailsaleRate, i.stock, i.itemBatch.dateOfExpire, i.itemBatch.item.discountAllowed, i.itemBatch.costRate) ")
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

        System.out.println(">>> Autocomplete SQL: " + sql.toString());
        List<StockDTO> results = (List<StockDTO>) getStockFacade().findLightsByJpql(sql.toString(), parameters, TemporalType.TIMESTAMP, 20);
        System.out.println(">>> Autocomplete returned " + (results != null ? results.size() : 0) + " results");
        if (results != null && !results.isEmpty()) {
            StockDTO firstResult = results.get(0);
            System.out.println(">>> First result - StockId: " + firstResult.getId() +
                             ", ItemBatchId: " + firstResult.getItemBatchId() +
                             ", ItemId: " + firstResult.getItemId() +
                             ", CostRate: " + firstResult.getCostRate());
        }

        return results;
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
                .append("i.id, i.itemBatch.id, i.itemBatch.item.id, i.itemBatch.item.name, i.itemBatch.item.code, i.itemBatch.item.vmp.name, ")
                .append("i.itemBatch.batchNo, i.itemBatch.retailsaleRate, i.stock, i.itemBatch.dateOfExpire, i.itemBatch.item.discountAllowed, i.itemBatch.costRate) ")
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

        System.out.println(">>> AutocompleteDto SQL: " + sql.toString());
        List<StockDTO> results = (List<StockDTO>) getStockFacade().findLightsByJpql(sql.toString(), parameters, TemporalType.TIMESTAMP, 20);
        System.out.println(">>> AutocompleteDto returned " + (results != null ? results.size() : 0) + " results");
        if (results != null && !results.isEmpty()) {
            StockDTO firstResult = results.get(0);
            System.out.println(">>> First result - StockId: " + firstResult.getId() +
                             ", ItemBatchId: " + firstResult.getItemBatchId() +
                             ", ItemId: " + firstResult.getItemId() +
                             ", CostRate: " + firstResult.getCostRate());
        }

        return results;
    }

    /**
     * Handles item selection from autocomplete.
     * Converts DTO to minimal entity (no database query) and calculates rates.
     */
    public void handleSelectAction() {
        long startTime = System.currentTimeMillis();
        System.out.println("=== HANDLE SELECT ACTION START ===");

        if (stockDto == null) {
            System.out.println("handleSelectAction: stockDto is null - exiting");
            return;
        }

        // Convert DTO to minimal entity (no database query)
        long step1Start = System.currentTimeMillis();
        this.stock = convertStockDtoToEntity(stockDto);
        System.out.println("Step 1 - Convert DTO to entity: " + (System.currentTimeMillis() - step1Start) + "ms");

        if (stock == null) {
            System.out.println("handleSelectAction: stock is null after conversion - exiting");
            return;
        }

        long step2Start = System.currentTimeMillis();
        getBillItem().getPharmaceuticalBillItem().setStock(stock);
        System.out.println("Step 2 - Get BillItem and set stock: " + (System.currentTimeMillis() - step2Start) + "ms");

        long step3Start = System.currentTimeMillis();
        calculateRatesOfSelectedBillItemBeforeAddingToTheList(billItem);
        System.out.println("Step 3 - Calculate rates: " + (System.currentTimeMillis() - step3Start) + "ms");

        // Load item instructions only if configured (performance optimization)
        if (configOptionApplicationController.getBooleanValueByKey("Load Item Instructions in Pharmacy Retail Sale", false)) {
            long step4Start = System.currentTimeMillis();
            pharmacyService.addBillItemInstructions(billItem);
            System.out.println("Step 4 - Add bill item instructions: " + (System.currentTimeMillis() - step4Start) + "ms");
        } else {
            System.out.println("Step 4 - Add bill item instructions: SKIPPED (not enabled)");
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("=== HANDLE SELECT ACTION TOTAL TIME: " + totalTime + "ms ===");
    }

    public void handleSelect(SelectEvent event) {
        System.out.println(">>> handleSelect: Event received");

        // Get the complete DTO directly from the selection event
        // This bypasses the converter and preserves all DTO fields
        StockDTO selectedDto = (StockDTO) event.getObject();

        if (selectedDto != null) {
            System.out.println(">>> handleSelect: Got DTO from event - StockId: " + selectedDto.getId() +
                             ", ItemBatchId: " + selectedDto.getItemBatchId() +
                             ", ItemId: " + selectedDto.getItemId());

            // Store the complete DTO directly
            this.stockDto = selectedDto;

            // Now call handleSelectAction which will use the complete DTO
            handleSelectAction();
        } else {
            System.out.println(">>> handleSelect: DTO from event is NULL");
        }
    }

    public void showItemDetailsForSelectedStock() {
        try {
            if (stockDto == null) {
                JsfUtil.addErrorMessage("Please select a stock first");
                return;
            }

            Stock selectedStock = convertStockDtoToEntity(stockDto);
            if (selectedStock == null || selectedStock.getItemBatch() == null || selectedStock.getItemBatch().getItem() == null) {
                JsfUtil.addErrorMessage("Selected stock does not have valid item information");
                return;
            }

            Item selectedItem = selectedStock.getItemBatch().getItem();
            Long itemId = selectedItem.getId();

            // Construct the URL with the item ID parameter
            String contextPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
            String popupUrl = contextPath + "/faces/pharmacy/pharmacy_item_transactions_popup.xhtml?itemId=" + itemId;

            // Execute JavaScript to open the popup
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Opening item details for: " + selectedItem.getName(), null));

            PrimeFaces.current().executeScript("window.open('" + popupUrl + "', '_blank');");

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error opening item details: " + e.getMessage());
        }
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
        calculateBillItemsAndBillTotalsOfPreBill();
        setActiveIndex(1);
    }

    public void calculateBillItemsAndBillTotalsOfPreBill() {
        calculateRatesForAllBillItemsInPreBill();
        calculatePreBillTotals();
    }

    public void calculateRatesForAllBillItemsInPreBill() {
        for (BillItem tbi : getPreBill().getBillItems()) {
            calculateRatesOfSelectedBillItemBeforeAddingToTheList(tbi);
//            calculateBillItemForEditing(tbi);
        }
        calculatePreBillTotals();
    }

    public void calculateRatesOfSelectedBillItemBeforeAddingToTheList(BillItem bi) {
        long calcRatesStart = System.currentTimeMillis();
        System.out.println("  >> calculateRatesOfSelectedBillItemBeforeAddingToTheList: Start");

        PharmaceuticalBillItem pharmBillItem = bi.getPharmaceuticalBillItem();
        if (pharmBillItem != null && pharmBillItem.getStock() != null) {
            long step1Start = System.currentTimeMillis();
            ItemBatch itemBatch = pharmBillItem.getStock().getItemBatch();
            if (itemBatch != null) {
                bi.setRate(itemBatch.getRetailsaleRate());
            }
            System.out.println("    >>> Set rate from item batch: " + (System.currentTimeMillis() - step1Start) + "ms");

            // Performance optimization: Skip discount calculation if no discount scheme is active
            long step2Start = System.currentTimeMillis();
            boolean hasDiscountScheme = getPaymentScheme() != null ||
                                       (getPaymentMethod() == PaymentMethod.Credit && toInstitution != null);

            if (hasDiscountScheme) {
                System.out.println("    >>> Discount scheme detected, calculating discount...");
                long discountStart = System.currentTimeMillis();
                bi.setDiscountRate(calculateBillItemDiscountRate(bi));
                System.out.println("    >>> Calculate discount rate: " + (System.currentTimeMillis() - discountStart) + "ms");
            } else {
                System.out.println("    >>> No discount scheme, setting discount to 0");
                bi.setDiscountRate(0.0);
            }
            System.out.println("    >>> Check discount scheme and calculate: " + (System.currentTimeMillis() - step2Start) + "ms");

            long step3Start = System.currentTimeMillis();
            bi.setNetRate(bi.getRate() - bi.getDiscountRate());

            bi.setGrossValue(bi.getRate() * bi.getQty());
            bi.setDiscount(bi.getDiscountRate() * bi.getQty());
            bi.setNetValue(bi.getGrossValue() - bi.getDiscount());
            System.out.println("    >>> Calculate final values: " + (System.currentTimeMillis() - step3Start) + "ms");

        } else {
            System.out.println("  >> calculateRatesOfSelectedBillItemBeforeAddingToTheList: PharmBillItem or Stock is null - skipping");
        }

        System.out.println("  >> calculateRatesOfSelectedBillItemBeforeAddingToTheList: Total time = " + (System.currentTimeMillis() - calcRatesStart) + "ms");
    }

    public void calculatePreBillTotals() {
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
        //Checking User Stock Entity - REMOVED for performance optimization
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

        //UserStock creation removed for performance optimization

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
        //UserStock creation removed for performance optimization
    }

    private void addMultipleStock() {
        Double remainingQty = Math.abs(qty) - Math.abs(getStock().getStock());
        addSingleStock();
        List<Stock> availableStocks = stockController.findNextAvailableStocks(getStock());

    }

    //Helper methods removed for performance optimization
//    private void saveUserStockContainer() {
//        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
//
//        getUserStockContainer().setCreater(getSessionController().getLoggedUser());
//        getUserStockContainer().setCreatedAt(new Date());
//
//        getUserStockContainerFacade().create(getUserStockContainer());
//
//    }
//
//    private UserStock saveUserStock(BillItem tbi) {
//        UserStock us = new UserStock();
//        us.setStock(tbi.getPharmaceuticalBillItem().getStock());
//        us.setUpdationQty(tbi.getQty());
//        us.setCreater(getSessionController().getLoggedUser());
//        us.setCreatedAt(new Date());
//        us.setUserStockContainer(getUserStockContainer());
//        getUserStockFacade().create(us);
//
//        getUserStockContainer().getUserStocks().add(us);
//
//        return us;
//    }

    //    public void calculateAllRatesNew() {
//        ////////System.out.println("calculating all rates");
//        for (BillItem tbi : getPreBill().getBillItems()) {
//            calculateRatesOfSelectedBillItemBeforeAddingToTheList(tbi);
//            calculateBillItemForEditing(tbi);
//        }
//        calculateBillItemsAndBillTotalsOfPreBill();
//    }
    //    Checked
    public BillItem getBillItem() {
        if (billItem == null) {
            billItem = new BillItem();
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

    private void savePreBill(Patient pt) {
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

        // Set balance and paidAmount based on payment method
        // For Credit and Staff credit payment methods, balance is netTotal and paidAmount is zero
        // For all other payment methods including MultiplePaymentMethods, balance is zero and paidAmount is netTotal
        if (getPaymentMethod() == PaymentMethod.Credit || getPaymentMethod() == PaymentMethod.Staff) {
            getPreBill().setBalance(getPreBill().getNetTotal());
            getPreBill().setPaidAmount(0.0);
        } else {
            getPreBill().setBalance(0.0);
            getPreBill().setPaidAmount(getPreBill().getNetTotal());
        }

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

        // Set balance and paidAmount based on payment method
        // For Credit and Staff credit payment methods, balance is netTotal and paidAmount is zero
        // For all other payment methods including MultiplePaymentMethods, balance is zero and paidAmount is netTotal
        if (getPaymentMethod() == PaymentMethod.Credit || getPaymentMethod() == PaymentMethod.Staff) {
            getPreBill().setBalance(getPreBill().getNetTotal());
            getPreBill().setPaidAmount(0.0);
        } else {
            getPreBill().setBalance(0.0);
            getPreBill().setPaidAmount(getPreBill().getNetTotal());
        }

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

        getBillFacade().edit(getPreBill());
    }

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

        // Set balance and paidAmount based on payment method
        // For Credit and Staff credit payment methods, balance is netTotal and paidAmount is zero
        // For all other payment methods including MultiplePaymentMethods, balance is zero and paidAmount is netTotal
        if (getPaymentMethod() == PaymentMethod.Credit || getPaymentMethod() == PaymentMethod.Staff) {
            getSaleBill().setBalance(getSaleBill().getNetTotal());
            getSaleBill().setPaidAmount(0.0);
        } else {
            getSaleBill().setBalance(0.0);
            getSaleBill().setPaidAmount(getSaleBill().getNetTotal());
        }

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

    private void savePreBillItems() {
        for (BillItem tbi : getPreBill().getBillItems()) {
//            if (execureOnEditActions(tbi)) {
////If any issue in Stock Bill Item will not save & not include for total
////                continue;
//            }

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

//                getBillItemFacade().edit(tbi);
            }
//            getPreBill().getBillItems().add(tbi);
        }

        //UserStock cleanup removed for performance optimization

        calculateRatesForAllBillItemsInPreBill();

    }

    private void savePreBillItemsFinally(List<BillItem> list) {
        // Initialize the billItems collection if it was set to null
        if (getPreBill().getBillItems() == null) {
            getPreBill().setBillItems(new ArrayList<>());
            System.out.println("Initialized PreBill billItems collection");
        }
        System.out.println("savePreBillItemsFinally: Processing " + list.size() + " items");

        for (BillItem tbi : list) {
            if (execureOnEditActions(tbi)) {
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
            System.out.println("Added BillItem ID: " + tbi.getId() + " back to PreBill");
        }

        System.out.println("savePreBillItemsFinally: Final PreBill items count: " + getPreBill().getBillItems().size());
        //UserStock cleanup removed for performance optimization

        calculateRatesForAllBillItemsInPreBill();

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

    private void saveSaleBillItems() {
        for (BillItem preBillItem : getPreBill().getBillItems()) {

            BillItem newlyCreatedBillItemForSaleBill = new BillItem();

            newlyCreatedBillItemForSaleBill.copy(preBillItem);
            newlyCreatedBillItemForSaleBill.setReferanceBillItem(preBillItem);
            newlyCreatedBillItemForSaleBill.setBill(getSaleBill());
            newlyCreatedBillItemForSaleBill.setInwardChargeType(InwardChargeType.Medicine);
            //      newBil.setBill(getSaleBill());
            newlyCreatedBillItemForSaleBill.setCreatedAt(Calendar.getInstance().getTime());
            newlyCreatedBillItemForSaleBill.setCreater(getSessionController().getLoggedUser());

            if (preBillItem.getPrescription() != null) {
                Prescription newlyCreatedPrescreptionForSaleBillItem = preBillItem.getPrescription().cloneForNewEntity();
                newlyCreatedBillItemForSaleBill.setPrescription(newlyCreatedPrescreptionForSaleBillItem);
                newlyCreatedPrescreptionForSaleBillItem.setPatient(patient);
                newlyCreatedPrescreptionForSaleBillItem.setCreatedAt(new Date());
                newlyCreatedPrescreptionForSaleBillItem.setCreater(sessionController.getWebUser());
                newlyCreatedPrescreptionForSaleBillItem.setInstitution(sessionController.getInstitution());
                newlyCreatedPrescreptionForSaleBillItem.setDepartment(sessionController.getDepartment());
            }

            PharmaceuticalBillItem newPhar = new PharmaceuticalBillItem();
            newPhar.copy(preBillItem.getPharmaceuticalBillItem());
            newPhar.setBillItem(newlyCreatedBillItemForSaleBill);
            newlyCreatedBillItemForSaleBill.setPharmaceuticalBillItem(newPhar);

            getBillItemFacade().create(newlyCreatedBillItemForSaleBill);
            getSaleBill().getBillItems().add(newlyCreatedBillItemForSaleBill);

            preBillItem.setReferanceBillItem(newlyCreatedBillItemForSaleBill);
        }
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
                newBil.setPrescription(tbi.getPrescription());
                tbi.getPrescription().setPatient(patient);
                tbi.getPrescription().setCreatedAt(new Date());
                tbi.getPrescription().setCreater(sessionController.getWebUser());
                tbi.getPrescription().setInstitution(sessionController.getInstitution());
                tbi.getPrescription().setDepartment(sessionController.getDepartment());
            }

            PharmaceuticalBillItem newPhar = new PharmaceuticalBillItem();
            newPhar.copy(tbi.getPharmaceuticalBillItem());
            newPhar.setBillItem(newBil);

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

            if (execureOnEditActions(b)) {
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
        editingQty = null;
        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Items added to bill to sale");
            return null;
        }
        if (!getPreBill().getBillItems().isEmpty()) {
            for (BillItem bi : getPreBill().getBillItems()) {
                //UserStock availability check removed for performance optimization
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

        calculateRatesForAllBillItemsInPreBill();

        List<BillItem> tmpBillItems = new ArrayList<>(getPreBill().getBillItems());
        getPreBill().setBillItems(null);
        getPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);

        savePreBillFinallyForRetailSaleForCashier(pt);
        savePreBillItemsFinally(tmpBillItems);
        setPrintBill(getBillFacade().find(getPreBill().getId()));

        // Calculate and record costing values for stock valuation after persistence
        // Using current bill directly instead of reloading to avoid transaction timing issues
        System.out.println("=== Using current PreBill directly ===");
        System.out.println("PreBill ID: " + getPreBill().getId());
        System.out.println("PreBill items count: " + (getPreBill().getBillItems() != null ? getPreBill().getBillItems().size() : "null"));

        if (getPreBill().getBillItems() != null && !getPreBill().getBillItems().isEmpty()) {
            calculateAndRecordCostingValues(getPreBill());
        } else {
            System.out.println("WARNING: PreBill has no items, trying to reload...");
            Bill managedBill = loadBillWithPharmaceuticalItems(getPreBill().getId());
            if (managedBill != null) {
                calculateAndRecordCostingValues(managedBill);
                setPreBill((PreBill) managedBill);
            } else {
                // CRITICAL: Log when bill reload fails - possible concurrent delete or data corruption
                System.out.println("CRITICAL WARNING: Failed to reload Bill with ID: " + getPreBill().getId());
                System.out.println("Skipping costing calculations - bill may have been deleted or data corrupted");
                System.out.println("PreBill settlement will complete but without financial details");
            }
        }

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
        editingQty = null;

        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Items added to bill to sale");
            return;
        }

        if (!getPreBill().getBillItems().isEmpty()) {
            for (BillItem bi : getPreBill().getBillItems()) {
                //UserStock availability check removed for performance optimization
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

        calculateRatesForAllBillItemsInPreBill();

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
//        setPrintBill(getBillFacade().find(id));

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
        setPrintBill(billService.reloadBill(id));
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

            // Clean up invalid ComponentDetails before validation
            cleanupInvalidPaymentDetails();

            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (cd.getPaymentMethod() != null && cd.getPaymentMethodData() != null) {
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
                if (cd.getPaymentMethodData() == null || cd.getPaymentMethod() == null) {
                    continue;
                }
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
                        p.setPaidValue(cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffCredit().getToStaff() != null) {
                            p.setToStaff(cd.getPaymentMethodData().getStaffCredit().getToStaff());
                            // Set bill.toStaff from the first Staff payment component
                            if (bill.getToStaff() == null) {
                                bill.setToStaff(cd.getPaymentMethodData().getStaffCredit().getToStaff());
                            }
                        }
                        break;
                    case Staff_Welfare:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffWelfare().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffWelfare().getToStaff() != null) {
                            p.setToStaff(cd.getPaymentMethodData().getStaffWelfare().getToStaff());
                            // Set bill.toStaff from the first Staff_Welfare payment component
                            if (bill.getToStaff() == null) {
                                bill.setToStaff(cd.getPaymentMethodData().getStaffWelfare().getToStaff());
                            }
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
                    break;
                case Staff_Welfare:
                    p.setToStaff(paymentMethodData.getStaffWelfare().getToStaff());
                    p.setPaidValue(paymentMethodData.getStaffWelfare().getTotalValue());
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
                //UserStock availability check removed for performance optimization
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

        if (errorCheckForSaleBill()) {
            billSettlingStarted = false;
            return;
        }
        if (errorCheckOnPaymentMethod()) {
            billSettlingStarted = false;
            return;
        }

        Patient pt = savePatient();

        calculateRatesForAllBillItemsInPreBill();

        savePreBill(pt);
        savePreBillItems(); // Stocks are Updated here

        saveSaleBill();
        saveSaleBillItems();

        List<Payment> payments = createPaymentsForBill(getSaleBill());
        drawerController.updateDrawerForIns(payments);

        updateRetailSaleFinanceDetails(getSaleBill());

        updateAll(); // REQUIRED - finance detail entities may be detached

        setPrintBill(getBillFacade().find(getSaleBill().getId()));

        if (toStaff != null && getPaymentMethod() == PaymentMethod.Credit) {
            getStaffBean().updateStaffCredit(toStaff, netTotal);
            JsfUtil.addSuccessMessage("User Credit Updated");
        }

        paymentService.updateBalances(payments);

        resetAll();
        billSettlingStarted = false;
        billPreview = true;

    }

    /**
     * Updates BillFinanceDetails for retail sales by calculating from existing
     * bill items. This method does not alter existing calculation logic but
     * creates new specific calculation for retail sales.
     *
     * @param bill The retail sale bill to update
     */
    /**
     * Comprehensive method to update retail sale finance details for bill and
     * bill items. Combines functionality from
     * updateBillFinanceDetailsForRetailSale and calculateAndRecordCostingValues
     * with simplified retail-only calculations and proper cost rate handling.
     */
    public void updateRetailSaleFinanceDetails(Bill bill) {
        System.out.println("=== Starting updateRetailSaleFinanceDetails ===");

        if (bill == null || bill.getBillItems() == null || bill.getBillItems().isEmpty()) {
            System.out.println("Early return - no bill or bill items");
            return;
        }

        // Initialize bill-level totals
        BigDecimal totalRetailSaleValue = BigDecimal.ZERO;
        BigDecimal totalPurchaseValue = BigDecimal.ZERO;
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalFreeQuantity = BigDecimal.ZERO;

        // Process each bill item
        int itemIndex = 0;
        for (BillItem billItem : bill.getBillItems()) {
            itemIndex++;
            System.out.println("--- Processing Bill Item " + itemIndex + " ---");
            System.out.println("BillItem ID: " + (billItem != null ? billItem.getId() : "null"));
            System.out.println("BillItem retired: " + (billItem != null ? billItem.isRetired() : "null"));
            System.out.println("BillItem qty: " + (billItem != null ? billItem.getQty() : "null"));

            if (billItem == null || billItem.isRetired()) {
                System.out.println("Skipping retired or null bill item");
                continue;
            }

            // Get pharmaceutical bill item for rate information
            PharmaceuticalBillItem pharmaItem = billItem.getPharmaceuticalBillItem();
            System.out.println("PharmaceuticalBillItem: " + (pharmaItem != null ? "exists" : "null"));
            if (pharmaItem == null) {
                System.out.println("Skipping - no pharmaceutical bill item");
                continue;
            }

            // Get quantities
            BigDecimal qty = BigDecimal.valueOf(billItem.getQty());
            BigDecimal freeQty = BigDecimal.valueOf(pharmaItem.getFreeQty());
            BigDecimal totalQty = qty.add(freeQty);
            System.out.println("Quantities - qty: " + qty + ", freeQty: " + freeQty + ", totalQty: " + totalQty);

            // Get rates from pharmaceutical bill item
            BigDecimal retailRate = BigDecimal.valueOf(pharmaItem.getRetailRate());
            BigDecimal purchaseRate = BigDecimal.valueOf(pharmaItem.getPurchaseRate());
            BigDecimal wholesaleRate = BigDecimal.valueOf(pharmaItem.getWholesaleRate());

            System.out.println("Pharma rates - retail: " + retailRate + ", purchase: " + purchaseRate + ", wholesale: " + wholesaleRate);

            // Get cost rate from item batch (correct approach) with fallback to purchase rate
            BigDecimal costRate = purchaseRate; // default fallback
            if (pharmaItem.getItemBatch() != null) {
                Double batchCostRate = pharmaItem.getItemBatch().getCostRate();
                if (batchCostRate != null && batchCostRate > 0) {
                    costRate = BigDecimal.valueOf(batchCostRate);
                    System.out.println("Got costRate from itemBatch.getCostRate(): " + costRate);
                } else {
                    System.out.println("ItemBatch costRate is null or negative, using pharma purchaseRate: " + costRate);
                }
            } else {
                System.out.println("No itemBatch found, using pharma purchaseRate: " + costRate);
            }

            // Get BillItemFinanceDetails (note: getBillItemFinanceDetails() auto-creates if null)
            BillItemFinanceDetails bifd = billItem.getBillItemFinanceDetails();
            System.out.println("BillItemFinanceDetails for item - ID: " + bifd.getId() + " (will be null if newly created)");

            // Calculate absolute quantity for calculations
            BigDecimal absQty = qty.abs();

            // UPDATE RATE FIELDS in BillItemFinanceDetails
            bifd.setLineNetRate(BigDecimal.valueOf(billItem.getNetRate()));
            bifd.setGrossRate(BigDecimal.valueOf(billItem.getRate()));
            bifd.setLineGrossRate(BigDecimal.valueOf(billItem.getRate()));
            bifd.setBillCostRate(BigDecimal.ZERO); // Set to 0 as per user requirement
            bifd.setTotalCostRate(costRate);
            bifd.setLineCostRate(costRate);
            bifd.setCostRate(costRate);
            bifd.setPurchaseRate(purchaseRate);
            bifd.setRetailSaleRate(retailRate);

            System.out.println("Set rates - costRate: " + costRate.doubleValue() + ", purchaseRate: " + purchaseRate.doubleValue() + ", retailSaleRate: " + retailRate.doubleValue());

            // UPDATE TOTAL FIELDS in BillItemFinanceDetails
            bifd.setLineGrossTotal(BigDecimal.valueOf(billItem.getGrossValue()));
            bifd.setGrossTotal(BigDecimal.valueOf(billItem.getGrossValue()));

            // SIMPLIFIED RETAIL SALE CALCULATIONS (positive values for retail sales)
            BigDecimal itemCostValue = costRate.multiply(absQty);
            BigDecimal itemRetailValue = retailRate.multiply(totalQty); // Include free quantity
            BigDecimal itemPurchaseValue = purchaseRate.multiply(totalQty); // Include free quantity

            // UPDATE COST VALUES in BillItemFinanceDetails
            bifd.setLineCost(itemCostValue);
            bifd.setBillCost(BigDecimal.ZERO); // Set to 0 as per user requirement
            bifd.setTotalCost(itemCostValue);

            System.out.println("Cost values: lineCost: " + bifd.getLineCost() + ", totalCost: " + bifd.getTotalCost());

            // UPDATE VALUE FIELDS in BillItemFinanceDetails (for retail sales, use totalQty including free)
            bifd.setValueAtCostRate(costRate.multiply(totalQty));
            bifd.setValueAtPurchaseRate(purchaseRate.multiply(totalQty));
            bifd.setValueAtRetailRate(retailRate.multiply(totalQty));

            System.out.println("BIFD values: valueAtCostRate: " + bifd.getValueAtCostRate()
                    + ", valueAtPurchaseRate: " + bifd.getValueAtPurchaseRate()
                    + ", valueAtRetailRate: " + bifd.getValueAtRetailRate());

            // UPDATE QUANTITIES in BillItemFinanceDetails
            bifd.setQuantity(qty);
            bifd.setQuantityByUnits(qty);

            // UPDATE PHARMACEUTICAL BILL ITEM VALUES
            pharmaItem.setCostRate(costRate.doubleValue());
            pharmaItem.setCostValue(itemCostValue.doubleValue());
            pharmaItem.setRetailValue(itemRetailValue.doubleValue());
            pharmaItem.setPurchaseValue(itemPurchaseValue.doubleValue());

            System.out.println("PBI values: costValue: " + pharmaItem.getCostValue()
                    + ", retailValue: " + pharmaItem.getRetailValue()
                    + ", purchaseValue: " + pharmaItem.getPurchaseValue());

            // Accumulate bill-level totals
            totalCostValue = totalCostValue.add(itemCostValue);
            totalPurchaseValue = totalPurchaseValue.add(itemPurchaseValue);
            totalRetailSaleValue = totalRetailSaleValue.add(itemRetailValue);
            totalQuantity = totalQuantity.add(qty);
            totalFreeQuantity = totalFreeQuantity.add(freeQty);

            System.out.println("Item " + itemIndex + " processing complete");
        }

        // UPDATE BILL-LEVEL FINANCE DETAILS (check if auto-creation happens here too)
        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        if (bfd == null) {
            System.out.println("WARNING: BillFinanceDetails missing for bill ID " + bill.getId() + " - creating new one");
            bfd = new BillFinanceDetails();
            bfd.setBill(bill);
            bill.setBillFinanceDetails(bfd);
            System.out.println("Created new BillFinanceDetails for bill");
        } else {
            System.out.println("BillFinanceDetails for bill - ID: " + bfd.getId());
        }

        // Set basic totals from bill
        bfd.setNetTotal(BigDecimal.valueOf(bill.getNetTotal()));
        bfd.setGrossTotal(BigDecimal.valueOf(bill.getTotal()));

        // Set calculated totals
        bfd.setTotalCostValue(totalCostValue);
        bfd.setTotalPurchaseValue(totalPurchaseValue);
        bfd.setTotalRetailSaleValue(totalRetailSaleValue);
        bfd.setTotalQuantity(totalQuantity);
        bfd.setTotalFreeQuantity(totalFreeQuantity);

        System.out.println("=== PRECISION DEBUG ===");
        System.out.println("Before saving - totalCostValue BigDecimal: " + totalCostValue);
        System.out.println("Before saving - totalCostValue scale: " + totalCostValue.scale());
        System.out.println("Before saving - totalCostValue precision: " + totalCostValue.precision());
        System.out.println("Before saving - totalCostValue toString: " + totalCostValue.toString());
        System.out.println("Before saving - totalCostValue doubleValue: " + totalCostValue.doubleValue());

        System.out.println("Bill totals - netTotal: " + bfd.getNetTotal()
                + ", grossTotal: " + bfd.getGrossTotal()
                + ", totalCostValue: " + bfd.getTotalCostValue()
                + ", totalPurchaseValue: " + bfd.getTotalPurchaseValue()
                + ", totalRetailSaleValue: " + bfd.getTotalRetailSaleValue()
                + ", totalQuantity: " + bfd.getTotalQuantity()
                + ", totalFreeQuantity: " + bfd.getTotalFreeQuantity());

        System.out.println("After setting - BFD.totalCostValue BigDecimal: " + bfd.getTotalCostValue());
        System.out.println("After setting - BFD.totalCostValue scale: " + (bfd.getTotalCostValue() != null ? bfd.getTotalCostValue().scale() : "null"));
        System.out.println("After setting - BFD.totalCostValue toString: " + (bfd.getTotalCostValue() != null ? bfd.getTotalCostValue().toString() : "null"));
        System.out.println("=== END PRECISION DEBUG ===");

        System.out.println("=== Completed updateRetailSaleFinanceDetails ===");
    }

    private void updateAll() {
        System.out.println("=== updateAll() - Before saving to database ===");
        if (saleBill.getBillFinanceDetails() != null) {
            System.out.println("SaleBill BFD totalCostValue before DB save: " + saleBill.getBillFinanceDetails().getTotalCostValue());
        }

        for (BillItem pbi : preBill.getBillItems()) {
            billItemFacade.edit(pbi);
        }
        billFacade.edit(preBill);
        for (BillItem sbi : saleBill.getBillItems()) {
            billItemFacade.edit(sbi);
        }
        billFacade.edit(saleBill);

        System.out.println("=== updateAll() - After saving to database ===");
        if (saleBill.getBillFinanceDetails() != null) {
            System.out.println("SaleBill BFD totalCostValue after DB save: " + saleBill.getBillFinanceDetails().getTotalCostValue());
            System.out.println("*** DATABASE SCHEMA ISSUE CONFIRMED ***");
            System.out.println("Expected: DECIMAL(18,4) but database has DECIMAL(38,0)");
            System.out.println("This causes BigDecimal precision loss during JPA save!");
        }
    }

    public String newPharmacyRetailSale() {
        clearBill();
        clearBillItem();
        billPreview = false;
        setBillSettlingStarted(false);
        return "pharmacy_bill_retail_sale";
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

        //Checking User Stock Entity - REMOVED for performance optimization

        billItem.setBill(getPreBill());
        billItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(billItem);

        //User Stock Container Save if New Bill - REMOVED for performance optimization

        calculateRatesForAllBillItemsInPreBill();

        calculateBillItemsAndBillTotalsOfPreBill();

        clearBillItem();
        setActiveIndex(1);
    }

    public void removeBillItem(BillItem b) {
        //UserStock removal removed for performance optimization
        getPreBill().getBillItems().remove(b.getSearialNo());

        calculateBillItemsAndBillTotalsOfPreBill();
    }

    public void removeSelectedBillItems() {
        if (selectedBillItems == null || selectedBillItems.isEmpty()) {
            JsfUtil.addErrorMessage("Please select items to delete");
            return;
        }

        Iterator<BillItem> iterator = selectedBillItems.iterator();
        while (iterator.hasNext()) {
            BillItem billItem = iterator.next();
            //UserStock removal removed for performance optimization
            getPreBill().getBillItems().remove(billItem);
            iterator.remove();
        }

        calculateBillItemsAndBillTotalsOfPreBill();
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
        calculateRatesForAllBillItemsInPreBill();
    }

    @Deprecated // Use listnerForPaymentMethodChange
    public void changeListener() {
        if (paymentMethod == PaymentMethod.PatientDeposit) {
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
        }
        calculateRatesForAllBillItemsInPreBill();
    }

    //    public void calculateRatesForAllBillItemsInPreBill() {
//        //////System.out.println("calculating all rates");
//        for (BillItem tbi : getPreBill().getBillItems()) {
//            calculateDiscountRates(tbi);
//            calculateBillItemForEditing(tbi);
//        }
//        calculateBillItemsAndBillTotalsOfPreBill();
//    }
    public void calculateRateListner(AjaxBehaviorEvent event) {

    }

    //    Checked
    public void calculateDiscountRates(BillItem bi) {
        bi.setDiscountRate(calculateBillItemDiscountRate(bi));
        bi.setDiscount(bi.getDiscountRate() * bi.getQty());
        bi.setNetRate(bi.getRate() - bi.getDiscountRate());
    }

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    //    TO check the functionality
    public double calculateBillItemDiscountRate(BillItem bi) {
        long discountCalcStart = System.currentTimeMillis();
        System.out.println("      >>>> calculateBillItemDiscountRate: Start");

        if (bi == null) {
            System.out.println("      >>>> calculateBillItemDiscountRate: BillItem is null - returning 0");
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem() == null) {
            System.out.println("      >>>> calculateBillItemDiscountRate: PharmaceuticalBillItem is null - returning 0");
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem().getStock() == null) {
            System.out.println("      >>>> calculateBillItemDiscountRate: Stock is null - returning 0");
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
            System.out.println("      >>>> calculateBillItemDiscountRate: ItemBatch is null - returning 0");
            return 0.0;
        }

        long setupStart = System.currentTimeMillis();
        bi.setItem(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getItem());
        double retailRate = bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate();
        double discountRate = 0;
        boolean discountAllowed = bi.getItem().isDiscountAllowed();
        System.out.println("      >>>> Setup item and retail rate: " + (System.currentTimeMillis() - setupStart) + "ms");
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
            long priceMatrixStart = System.currentTimeMillis();
            System.out.println("      >>>> Checking PaymentScheme discount...");
            PriceMatrix priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(getPaymentMethod(), getPaymentScheme(), getSessionController().getDepartment(), bi.getItem());
            System.out.println("      >>>> PriceMatrix lookup (PaymentScheme): " + (System.currentTimeMillis() - priceMatrixStart) + "ms");

            if (priceMatrix != null) {
                bi.setPriceMatrix(priceMatrix);
                discountRate = priceMatrix.getDiscountPercent();
            }

            double dr;
            dr = (retailRate * discountRate) / 100;
            System.out.println("      >>>> calculateBillItemDiscountRate: Total time = " + (System.currentTimeMillis() - discountCalcStart) + "ms, returning: " + dr);
            return dr;

        }

        //PAYMENTMETHOD DISCOUNT
        if (getPaymentMethod() != null && discountAllowed) {
            long priceMatrixStart = System.currentTimeMillis();
            System.out.println("      >>>> Checking PaymentMethod discount...");
            PriceMatrix priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(getPaymentMethod(), getSessionController().getDepartment(), bi.getItem());
            System.out.println("      >>>> PriceMatrix lookup (PaymentMethod): " + (System.currentTimeMillis() - priceMatrixStart) + "ms");

            if (priceMatrix != null) {
                bi.setPriceMatrix(priceMatrix);
                discountRate = priceMatrix.getDiscountPercent();
            }

            double dr;
            dr = (retailRate * discountRate) / 100;
            System.out.println("      >>>> calculateBillItemDiscountRate: Total time = " + (System.currentTimeMillis() - discountCalcStart) + "ms, returning: " + dr);
            return dr;

        }

        //CREDIT COMPANY DISCOUNT
        if (getPaymentMethod() == PaymentMethod.Credit && toInstitution != null) {
            System.out.println("      >>>> Checking Credit company discount...");
            discountRate = toInstitution.getPharmacyDiscount();

            double dr;
            dr = (retailRate * discountRate) / 100;
            System.out.println("      >>>> calculateBillItemDiscountRate: Total time = " + (System.currentTimeMillis() - discountCalcStart) + "ms, returning: " + dr);
            return dr;
        }

        System.out.println("      >>>> calculateBillItemDiscountRate: No discount applicable, Total time = " + (System.currentTimeMillis() - discountCalcStart) + "ms, returning 0");
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
//        createBillFeePaymentAndPayment(bf, p);
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
                if (cd.getPaymentMethodData() == null || cd.getPaymentMethod() == null) {
                    continue;
                }
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

//    public void createBillFeePaymentAndPayment(BillFee bf, Payment p) {
//        BillFeePayment bfp = new BillFeePayment();
//        bfp.setBillFee(bf);
//        bfp.setAmount(bf.getSettleValue());
//        bfp.setInstitution(getSessionController().getInstitution());
//        bfp.setDepartment(getSessionController().getDepartment());
//        bfp.setCreater(getSessionController().getLoggedUser());
//        bfp.setCreatedAt(new Date());
//        bfp.setPayment(p);
//        getBillFeePaymentFacade().create(bfp);
//    }

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
        //userStockContainer = null; //Removed for performance optimization
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

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
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

    //Removed for performance optimization
//    public UserStockContainer getUserStockContainer() {
//        if (userStockContainer == null) {
//            userStockContainer = new UserStockContainer();
//        }
//        return userStockContainer;
//    }
//
//    public void setUserStockContainer(UserStockContainer userStockContainer) {
//        this.userStockContainer = userStockContainer;
//    }

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

    //Removed for performance optimization
//    public UserStockContainerFacade getUserStockContainerFacade() {
//        return userStockContainerFacade;
//    }
//
//    public void setUserStockContainerFacade(UserStockContainerFacade userStockContainerFacade) {
//        this.userStockContainerFacade = userStockContainerFacade;
//    }
//
//    public UserStockFacade getUserStockFacade() {
//        return userStockFacade;
//    }
//
//    public void setUserStockFacade(UserStockFacade userStockFacade) {
//        this.userStockFacade = userStockFacade;
//    }

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
                    if (cd.getPaymentMethod() == PaymentMethod.PatientDeposit && cd.getPaymentMethodData() != null) {
                        hasPatientDeposit = true;
                        cd.getPaymentMethodData().getPatient_deposit().setPatient(patient);
                        cd.getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                        cd.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);

                    }
                }
            }

        }
        calculateBillItemsAndBillTotalsOfPreBill();
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

    /**
     * Calculates and records comprehensive financial details for retail sale
     * bill items. Populates BillItemFinanceDetails (BIFD) and
     * BillFinanceDetails (BFD) with complete cost, purchase, retail, and
     * wholesale rate information required for pharmacy income reports.
     *
     * @param bill The retail sale bill to calculate financial details for
     */
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
                System.out.println(">>> CONVERTER: Converting ID: " + id);
                PharmacySaleForCashierController controller = (PharmacySaleForCashierController) facesContext.getApplication().getELResolver()
                        .getValue(facesContext.getELContext(), null, "pharmacySaleForCashierController");

                // Check if it's the currently selected stockDto (optimization for immediate reuse)
                if (controller != null && controller.getStockDto() != null && id.equals(controller.getStockDto().getId())) {
                    System.out.println(">>> CONVERTER: Found in current stockDto");
                    return controller.getStockDto();
                }

                // Create a minimal DTO with just the ID
                // This will trigger the database fetch in convertStockDtoToEntity to get fresh data
                System.out.println(">>> CONVERTER: Creating minimal DTO (will fetch fresh data from DB)");
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

    /**
     * Load bill with all pharmaceutical item associations to avoid lazy loading issues.
     * This is essential for financial calculations that require pharmaceutical item data.
     *
     * @param billId The ID of the bill to load
     * @return Bill with all pharmaceutical associations loaded
     */
    private Bill loadBillWithPharmaceuticalItems(Long billId) {
        System.out.println("=== loadBillWithPharmaceuticalItems START ===");
        System.out.println("Loading bill ID: " + billId);

        // First try to get the basic bill
        Bill bill = getBillFacade().find(billId);
        System.out.println("Basic bill found: " + (bill != null ? bill.getId() : "null"));

        if (bill != null) {
            System.out.println("Bill items before JPQL: " + (bill.getBillItems() != null ? bill.getBillItems().size() : "null"));
        }

        // Now try with JPQL to fetch associations
        String jpql = "SELECT b FROM Bill b "
                + "JOIN FETCH b.billItems bi "
                + "LEFT JOIN FETCH bi.pharmaceuticalBillItem pbi "
                + "LEFT JOIN FETCH pbi.itemBatch "
                + "WHERE b.id = :billId";

        System.out.println("JPQL query: " + jpql);
        Map<String, Object> params = new HashMap<>();
        params.put("billId", billId);

        try {
            Bill loadedBill = getBillFacade().findFirstByJpql(jpql, params);
            System.out.println("JPQL result: " + (loadedBill != null ? "Found bill ID " + loadedBill.getId() : "null"));

            if (loadedBill != null && loadedBill.getBillItems() != null) {
                System.out.println("JPQL loaded bill items count: " + loadedBill.getBillItems().size());
                for (BillItem bi : loadedBill.getBillItems()) {
                    System.out.println("  - BillItem ID: " + bi.getId() + ", PharmaItem: " + (bi.getPharmaceuticalBillItem() != null ? "EXISTS" : "NULL"));
                }
            } else {
                System.out.println("ERROR: JPQL returned bill with no items!");
            }

            return loadedBill;
        } catch (Exception e) {
            System.out.println("JPQL ERROR: " + e.getMessage());
            e.printStackTrace();

            // Check if original bill exists before attempting fallback
            if (bill == null) {
                System.out.println("CRITICAL: Original bill is null, cannot use fallback loading");
                System.out.println("Bill ID " + billId + " may have been deleted or does not exist");
                return null;
            }

            // Fallback: Force load collections manually
            System.out.println("Using fallback manual loading...");
            if (bill.getBillItems() != null) {
                System.out.println("Forcing bill items load - count: " + bill.getBillItems().size());
                for (BillItem bi : bill.getBillItems()) {
                    // Force lazy loading
                    if (bi.getPharmaceuticalBillItem() != null) {
                        System.out.println("Loaded PharmaItem for BillItem ID: " + bi.getId());
                        // Force load item batch if needed
                        if (bi.getPharmaceuticalBillItem().getItemBatch() != null) {
                            System.out.println("Loaded ItemBatch for BillItem ID: " + bi.getId());
                        }
                    }
                }
            } else {
                System.out.println("WARNING: Fallback bill has no items to load");
            }
            return bill;
        }
    }

    /**
     * Calculate and record costing values for stock valuation.
     * Creates BillFinanceDetails and BillItemFinanceDetails with proper financial calculations.
     *
     * @param bill The bill for which to calculate costing values
     */
        private void calculateAndRecordCostingValues(Bill bill) {
        System.out.println("=== CALCULATE AND RECORD COSTING VALUES START ===");
        System.out.println("Bill ID: " + (bill != null ? bill.getId() : "null"));
        System.out.println("Method call stack trace (first 3 levels):");
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 1; i <= Math.min(4, stack.length - 1); i++) {
            System.out.println("  " + i + ": " + stack[i].getClassName() + "." + stack[i].getMethodName() + ":" + stack[i].getLineNumber());
        }

        if (bill == null || bill.getBillItems() == null || bill.getBillItems().isEmpty()) {
            System.out.println("ERROR: Bill is null or has no items");
            System.out.println("Bill: " + bill);
            System.out.println("BillItems: " + (bill != null ? bill.getBillItems() : "bill is null"));
            return;
        }

        System.out.println("Bill Items Count: " + bill.getBillItems().size());

        // Initialize bill finance details if not present
        if (bill.getBillFinanceDetails() == null) {
            System.out.println("Creating new BillFinanceDetails for bill ID: " + bill.getId());
            BillFinanceDetails billFinanceDetails = new BillFinanceDetails();
            billFinanceDetails.setBill(bill);
            bill.setBillFinanceDetails(billFinanceDetails);
        } else {
            System.out.println("BillFinanceDetails already exists for bill ID: " + bill.getId());
            BillFinanceDetails existingBfd = bill.getBillFinanceDetails();

            // Check if calculations are already done
            if (existingBfd.getTotalCostValue() != null ||
                existingBfd.getTotalPurchaseValue() != null ||
                existingBfd.getTotalRetailSaleValue() != null) {
                System.out.println("WARNING: Bill finance details already calculated. Existing values:");
                System.out.println("  TotalCostValue: " + existingBfd.getTotalCostValue());
                System.out.println("  TotalPurchaseValue: " + existingBfd.getTotalPurchaseValue());
                System.out.println("  TotalRetailSaleValue: " + existingBfd.getTotalRetailSaleValue());
                System.out.println("SKIPPING to prevent duplicate calculations");
                return;
            }
        }

        // Initialize aggregated values
        java.math.BigDecimal totalCostValue = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalPurchaseValue = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalRetailSaleValue = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalWholesaleValue = java.math.BigDecimal.ZERO;

        // Iterate through bill items and calculate stock valuations
        int itemIndex = 0;
        for (BillItem billItem : bill.getBillItems()) {
            itemIndex++;
            System.out.println("\n--- Processing Bill Item " + itemIndex + " ---");
            System.out.println("BillItem ID: " + (billItem != null ? billItem.getId() : "null"));

            if (billItem == null) {
                System.out.println("SKIP: BillItem is null");
                continue;
            }

            System.out.println("BillItem considered for costing: " + billItem.isConsideredForCosting());
            if (!billItem.isConsideredForCosting()) {
                System.out.println("SKIP: BillItem not considered for costing");
                continue;
            }

            // Check if BillItem already has calculated finance details
            BillItemFinanceDetails existingDetails = billItem.getBillItemFinanceDetails();
            if (existingDetails != null) {
                System.out.println("BillItem ID: " + billItem.getId() + " already has BIFD ID: " + existingDetails.getId());
                System.out.println("Current BIFD values: costRate=" + existingDetails.getValueAtCostRate()
                                 + ", purchaseRate=" + existingDetails.getValueAtPurchaseRate()
                                 + ", retailRate=" + existingDetails.getValueAtRetailRate());

                // Check if values are already set - if so, skip calculation to prevent duplicates
                if (existingDetails.getValueAtCostRate() != null ||
                    existingDetails.getValueAtPurchaseRate() != null ||
                    existingDetails.getValueAtRetailRate() != null) {
                    System.out.println("SKIP: BillItem ID: " + billItem.getId() + " already has calculated finance details");
                    continue;
                }
            }

            // Initialize BillItemFinanceDetails explicitly (prevents auto-creation duplicates)
            // NOTE: This is the PREFERRED approach - gradually migrate all callers to this pattern
            billItem.initializeBillItemFinanceDetails();
            BillItemFinanceDetails itemFinanceDetails = billItem.getBillItemFinanceDetails();

            // Get quantity - default to 0 if null
            // NOTE: Consistent with updateRetailSaleFinanceDetails - only billItem.getQty() is used for valuations
            // Free quantities are tracked separately but NOT included in stock valuation calculations
            // This is correct behavior - free quantities don't affect cost/purchase/retail valuations
            java.math.BigDecimal quantity = billItem.getQty() != null
                    ? java.math.BigDecimal.valueOf(billItem.getQty()) : java.math.BigDecimal.ZERO;
            System.out.println("BillItem Quantity (excluding free qty - consistent with retail): " + quantity);

            // Calculate stock valuations for this item based on pharmaceutical bill item rates
            PharmaceuticalBillItem pharmaItem = billItem.getPharmaceuticalBillItem();
            System.out.println("PharmaceuticalBillItem: " + (pharmaItem != null ? "EXISTS" : "NULL"));

            if (pharmaItem != null) {
                System.out.println("PharmaItem ID: " + pharmaItem.getId());
                System.out.println("PharmaItem Purchase Rate: " + pharmaItem.getPurchaseRate());
                System.out.println("PharmaItem Retail Rate: " + pharmaItem.getRetailRate());
                System.out.println("PharmaItem Wholesale Rate: " + pharmaItem.getWholesaleRate());
                System.out.println("PharmaItem ItemBatch: " + (pharmaItem.getItemBatch() != null ? "EXISTS" : "NULL"));
                if (pharmaItem.getItemBatch() != null) {
                    System.out.println("ItemBatch Cost Rate: " + pharmaItem.getItemBatch().getCostRate());
                }
                // Calculate value at cost rate - use actual cost rate from ItemBatch
                Double costRateValue = null;
                if (pharmaItem.getItemBatch() != null) {
                    costRateValue = pharmaItem.getItemBatch().getCostRate();
                }
                System.out.println("Initial Cost Rate Value: " + costRateValue);

                if (costRateValue == null || costRateValue <= 0) {
                    costRateValue = pharmaItem.getPurchaseRate(); // fallback
                    System.out.println("Using Purchase Rate as fallback: " + costRateValue);
                }

                if (costRateValue > 0) {
                    java.math.BigDecimal costRate = java.math.BigDecimal.valueOf(costRateValue);
                    java.math.BigDecimal valueAtCostRate = quantity.multiply(costRate).negate();
                    System.out.println("Calculated valueAtCostRate: " + valueAtCostRate);
                    itemFinanceDetails.setValueAtCostRate(valueAtCostRate);
                    totalCostValue = totalCostValue.add(valueAtCostRate);
                    System.out.println("Set valueAtCostRate on itemFinanceDetails");
                } else {
                    System.out.println("SKIP: Cost rate is 0 or negative");
                }

                // Calculate value at purchase rate (same as cost rate for now)
                if (pharmaItem.getPurchaseRate() > 0) {
                    java.math.BigDecimal purchaseRate = java.math.BigDecimal.valueOf(pharmaItem.getPurchaseRate());
                    java.math.BigDecimal valueAtPurchaseRate = quantity.multiply(purchaseRate).negate();
                    System.out.println("Calculated valueAtPurchaseRate: " + valueAtPurchaseRate);
                    itemFinanceDetails.setValueAtPurchaseRate(valueAtPurchaseRate);
                    totalPurchaseValue = totalPurchaseValue.add(valueAtPurchaseRate);
                    System.out.println("Set valueAtPurchaseRate on itemFinanceDetails");
                } else {
                    System.out.println("SKIP: Purchase rate is 0 or negative");
                }

                // Calculate value at retail rate (based on retail rate)
                if (pharmaItem.getRetailRate() > 0) {
                    java.math.BigDecimal retailRate = java.math.BigDecimal.valueOf(pharmaItem.getRetailRate());
                    java.math.BigDecimal valueAtRetailRate = quantity.multiply(retailRate).negate();
                    System.out.println("Calculated valueAtRetailRate: " + valueAtRetailRate);
                    itemFinanceDetails.setValueAtRetailRate(valueAtRetailRate);
                    totalRetailSaleValue = totalRetailSaleValue.add(valueAtRetailRate);
                    System.out.println("Set valueAtRetailRate on itemFinanceDetails");
                } else {
                    System.out.println("SKIP: Retail rate is 0 or negative");
                }

                // Calculate value at wholesale rate (use retail rate if wholesale rate not available)
                double wholesaleRate = pharmaItem.getWholesaleRate() > 0
                        ? pharmaItem.getWholesaleRate()
                        : (pharmaItem.getRetailRate() > 0 ? pharmaItem.getRetailRate() : 0.0);

                if (wholesaleRate > 0) {
                    java.math.BigDecimal wholsaleRateBd = java.math.BigDecimal.valueOf(wholesaleRate);
                    java.math.BigDecimal valueAtWholesaleRate = quantity.multiply(wholsaleRateBd).negate();
                    itemFinanceDetails.setValueAtWholesaleRate(valueAtWholesaleRate);
                    totalWholesaleValue = totalWholesaleValue.add(valueAtWholesaleRate);
                }

                // Set rates in the finance details - use the SAME costRateValue used for calculations
                if (costRateValue != null && costRateValue > 0) {
                    // CRITICAL FIX: Store the actual cost rate used in calculations (not always purchaseRate)
                    itemFinanceDetails.setCostRate(java.math.BigDecimal.valueOf(costRateValue));
                    itemFinanceDetails.setTotalCostRate(java.math.BigDecimal.valueOf(costRateValue));
                    itemFinanceDetails.setLineCostRate(java.math.BigDecimal.valueOf(costRateValue));
                }
                if (pharmaItem.getPurchaseRate() > 0) {
                    itemFinanceDetails.setPurchaseRate(java.math.BigDecimal.valueOf(pharmaItem.getPurchaseRate()));
                }
                if (pharmaItem.getRetailRate() > 0) {
                    itemFinanceDetails.setRetailSaleRate(java.math.BigDecimal.valueOf(pharmaItem.getRetailRate()));
                }
                if (wholesaleRate > 0) {
                    itemFinanceDetails.setWholesaleRate(java.math.BigDecimal.valueOf(wholesaleRate));
                }
            }

            // Set quantity in finance details
            itemFinanceDetails.setQuantity(quantity);
            itemFinanceDetails.setTotalQuantity(quantity);

        }

        // Update bill level aggregated values
        BillFinanceDetails billFinanceDetails = bill.getBillFinanceDetails();
        billFinanceDetails.setTotalCostValue(totalCostValue);
        billFinanceDetails.setTotalPurchaseValue(totalPurchaseValue);
        billFinanceDetails.setTotalRetailSaleValue(totalRetailSaleValue);
        billFinanceDetails.setTotalWholesaleValue(totalWholesaleValue);

        // NOTE: Removed redundant billItemFacade.edit() calls to prevent duplicate cascades
        // The bill.save() below will cascade to all billItems and their financeDetails automatically

        // Save the bill with its finance details
        billFacade.edit(bill);
        System.out.println("=== CALCULATE AND RECORD COSTING VALUES COMPLETED ===");
    }

}
