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
import com.divudi.core.data.StockLight;
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
import com.divudi.service.BillService;
import com.divudi.service.DiscountSchemeValidationService;
import com.divudi.service.PaymentService;
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
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author Dr M H Buddhika Ariyaratne buddhika.ari@gmail.com
 *
 */
@Named
@SessionScoped
@Deprecated // Simple Sale feature removed. Use PharmacyRetailSaleController for retail sales.
public class PharmacySimpleRetailSaleController implements Serializable, ControllerWithPatient, ControllerWithMultiplePayments {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
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
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private StaffService staffBean;
    @EJB
    PaymentService paymentService;
    @EJB
    private UserStockContainerFacade userStockContainerFacade;
    @EJB
    private UserStockFacade userStockFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    private TokenFacade tokenFacade;
    @EJB
    private PharmacyService pharmacyService;
    @EJB
    private BillService billService;

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private PaymentSchemeController PaymentSchemeController;
    @Inject
    private StockController stockController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private FinancialTransactionController financialTransactionController;
    @Inject
    private SessionController sessionController;
    @Inject
    private SearchController searchController;
    @Inject
    private PatientDepositController patientDepositController;
    @Inject
    private TokenController tokenController;
    @Inject
    private DrawerController drawerController;

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private PreBill preBill;
    private Bill saleBill;
    private Bill printBill;
    private Bill bill;
    private BillItem billItem;
    private List<BillItem> selectedBillItems;
    private BillItem editingBillItem;
    private Double qty;
    private Stock stock;
    private StockLight stockLight;
    private boolean billSettlingStarted;
    private PaymentScheme paymentScheme;

    private int activeIndex;

    private Token token;
    private Patient patient;
    private YearMonthDay yearMonthDay;
    private String patientTabId = "tabPt";
    private String strTenderedValue = "";
    private boolean billPreview = false;
    private boolean fromOpdEncounter = false;
    private String opdEncounterComments = "";
    private int patientSearchTab = 0;

    private Staff toStaff;
    private Institution toInstitution;
    private String errorMessage = "";

    /////////////////////////
    private double cashPaid;
    private double netTotal;
    private double balance;
    private Double editingQty;
    private String cashPaidStr;
    private String comment;
    ///////////////////
    private UserStockContainer userStockContainer;
    private PaymentMethodData paymentMethodData;
    private boolean patientDetailsEditable;
    private Department counter;
    private Token currentToken;

    private PaymentMethod paymentMethod;

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Constructors">
    public PharmacySimpleRetailSaleController() {
    }

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Navigation Methods">
    public String navigateToPharmacySimpleRetailSale() {
        if (sessionController.getPharmacyBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                resetAll();
                setBillSettlingStarted(false);
                return "/pharmacy/pharmacy_simple_retail_sale?faces-redirect=true";
            } else {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        } else {
            resetAll();
            setBillSettlingStarted(false);
            return "/pharmacy/pharmacy_simple_retail_sale?faces-redirect=true";
        }
    }

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Functions">
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
            if ((getPatient().getMobileNumberStringTransient() == null
                    || getPatient().getMobileNumberStringTransient().trim().isEmpty() || getPatient().getPerson().getName().trim().isEmpty())
                    && configOptionApplicationController.getBooleanValueByKey("Patient details are required for retail sale", false)) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient name and mobile number.");
                return;
            }

        }

        if (!getPreBill().getBillItems().isEmpty()) {
            for (BillItem bi : getPreBill().getBillItems()) {
                if (!userStockController.isStockAvailable(bi.getPharmaceuticalBillItem().getStock(), bi.getQty(), getSessionController().getLoggedUser())) {
                    setZeroToQty(bi);
                    JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
                    billSettlingStarted = false;
                    return;
                }
                if (bi.getQty() <= 0.0) {
                    ////System.out.println("bi.getQty() = " + bi.getQty());
                    JsfUtil.addErrorMessage("Some BillItem Quntity is Zero or less than Zero");
                    billSettlingStarted = false;
                    return;
                }
            }
        }

        Patient pt = savePatient();

        boolean hadPaymentRelatedError = paymentService.checkPaymentMethodError(paymentMethod, paymentMethodData, getPreBill().getNetTotal(), cashPaid, getPatient(), toStaff);

        if (hadPaymentRelatedError) {
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

        if (toStaff != null && getPaymentMethod() == PaymentMethod.Credit) {
            getStaffBean().updateStaffCredit(toStaff, netTotal);
            JsfUtil.addSuccessMessage("User Credit Updated");
        } else if (getPaymentMethod() == PaymentMethod.PatientDeposit) {
            double runningBalance;
            if (pt != null) {
                if (pt.getRunningBalance() != null) {
                    runningBalance = pt.getRunningBalance();
                } else {
                    runningBalance = 0.0;
                }
                runningBalance += netTotal;
                pt.setRunningBalance(runningBalance);
            }

        }
        paymentService.updateBalances(payments);
        resetAll();
        billSettlingStarted = false;
        billPreview = true;
    }

    // ChatGPT contributed - 2025-05
    public List<Stock> completeAvailableStocks(String qry) {
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

        StringBuilder jpql = new StringBuilder("SELECT i FROM Stock i ")
                .append("WHERE i.stock > :stockMin ")
                .append("AND i.department = :department AND (");

        jpql.append("i.itemName LIKE :query ");

        if (searchByItemCode) {
            jpql.append("OR i.code LIKE :query ");
        }

        if (searchByBarcode) {
            jpql.append("OR i.barcode LIKE :query ");
        }

        Long longCode = CommonFunctions.stringToLong(qry);
        if (longCode != null) {
            jpql.append("OR i.longCode = :longCode ");
            parameters.put("longCode", longCode);
        }

        jpql.append(") ORDER BY i.itemName, i.dateOfExpire");

        System.out.println("sql.toString() = " + jpql.toString());
        System.out.println("parameters = " + parameters);
        List<Stock> ss = getStockFacade().findByJpql(jpql.toString(), parameters, 20);
        System.out.println("ss = " + ss);
        return ss;
    }

    public List<StockLight> completeStockLights(String qry) {
        long startTime = System.currentTimeMillis();
        System.out.println("INFO:   completeStockLights started");

        if (qry == null || qry.trim().isEmpty()) {
            System.out.println("INFO:   Validation check took: " + (System.currentTimeMillis() - startTime) + " ms");
            return Collections.emptyList();
        }

        long paramStartTime = System.currentTimeMillis();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("department", getSessionController().getLoggedUser().getDepartment());
        parameters.put("stockMin", 0.0);
        parameters.put("query", qry.trim() + "%");  // changed from %qry% to qry%
        parameters.put("today", new Date());
        System.out.println("INFO:   Building parameters took: " + (System.currentTimeMillis() - paramStartTime) + " ms");

        String jpql = "SELECT new com.divudi.core.data.StockLight("
                + "s.id, s.itemName, s.code, s.barcode, s.dateOfExpire, s.retailsaleRate, s.stock) "
                + "FROM Stock s "
                + "WHERE s.stock > :stockMin "
                + "AND s.department = :department "
                + "AND s.dateOfExpire >= :today "
                + // filter for non-expired
                "AND (s.itemName LIKE :query OR s.code LIKE :query OR s.barcode LIKE :query) "
                + "ORDER BY s.itemName";
        System.out.println("INFO:   JPQL built: " + jpql);
        System.out.println("INFO:   Parameters: " + parameters);

        long queryStartTime = System.currentTimeMillis();
        List<StockLight> sls = (List<StockLight>) stockFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP, 20);
        long queryEndTime = System.currentTimeMillis();
        System.out.println("INFO:   Query execution took: " + (queryEndTime - queryStartTime) + " ms");
        System.out.println("INFO:   Result size: " + sls.size());

        System.out.println("INFO:   completeStockLights finished. Total time: " + (System.currentTimeMillis() - startTime) + " ms");

        return sls;
    }

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Inner Classes Static Converter">
    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Inner Classes">
    // </editor-fold> 
////////////////////////
/////////////////////////
    public Token getCurrentToken() {
        return currentToken;
    }

    public void setCurrentToken(Token currentToken) {
        this.currentToken = currentToken;
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
        searchController.createPreBillsNotPaid();
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

    public void handleSelectAction() {
        if (stock == null) {
        }
        if (getBillItem() == null || getBillItem().getPharmaceuticalBillItem() == null) {
        }
        getBillItem().getPharmaceuticalBillItem().setStock(stock);
        calculateRates(billItem);
    }

    public void handleSelect(SelectEvent event) {
        if (stockLight == null) {
            return;
        }
        if (stockLight.getId() == null) {
            return;
        }
        stock = stockFacade.find(stockLight.getId());
        if (stock == null) {
            return;
        }
        handleSelectAction();
    }

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
        System.out.println("processBillItems");
        calculateAllRates();
        calculateTotals();
    }

    public void calculateAllRates() {
        for (BillItem tbi : getPreBill().getBillItems()) {
            calculateRates(tbi);
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
//            calculateRates(tbi);
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

        String insId = getBillNumberBean().institutionBillNumberGenerator(getPreBill().getInstitution(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        getPreBill().setInsId(insId);
        String deptId = getBillNumberBean().departmentBillNumberGenerator(getPreBill().getDepartment(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
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

        String insId = getBillNumberBean().institutionBillNumberGenerator(getPreBill().getInstitution(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        getPreBill().setInsId(insId);
        String deptId = getBillNumberBean().departmentBillNumberGenerator(getPreBill().getDepartment(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        getPreBill().setDeptId(deptId);
        getPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        getPreBill().setInvoiceNumber(billNumberBean.fetchPaymentSchemeCount(getPreBill().getPaymentScheme(), getPreBill().getBillType(), getPreBill().getInstitution()));

    }

    @Inject
    private BillBeanController billBean;

    private void saveSaleBill() {
        //  calculateAllRates();

        getSaleBill().copy(getPreBill());
        getSaleBill().copyValue(getPreBill());

        getSaleBill().setBillType(BillType.PharmacySale);
        getSaleBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE);

        getSaleBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getSaleBill().setInstitution(getSessionController().getLoggedUser().getInstitution());
        getSaleBill().setBillDate(new Date());
        getSaleBill().setBillTime(new Date());
        getSaleBill().setCreatedAt(Calendar.getInstance().getTime());
        getSaleBill().setCreater(getSessionController().getLoggedUser());
        getSaleBill().setReferenceBill(getPreBill());

        getSaleBill().setInsId(getPreBill().getInsId());
        getSaleBill().setDeptId(getPreBill().getDeptId());
        getSaleBill().setInvoiceNumber(getPreBill().getInvoiceNumber());
        getSaleBill().setComments(comment);

        getSaleBill().setCashPaid(cashPaid);
        getSaleBill().setBalance(balance);

        getBillBean().setPaymentMethodData(getSaleBill(), getSaleBill().getPaymentMethod(), getPaymentMethodData());

        if (getSaleBill().getId() == null) {
            getBillFacade().create(getSaleBill());
        }

        updatePreBill();
    }
//

    private void updatePreBill() {
        getPreBill().setReferenceBill(getSaleBill());
        getBillFacade().edit(getPreBill());
    }

    @EJB
    PrescriptionFacade prescriptionFacade;

    private void savePreBillItemsFinally(List<BillItem> list) {
        for (BillItem tbi : list) {
            if (onEdit(tbi)) {
//If any issue in Stock Bill Item will not save & not include for total
//                continue;
            }

            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());

            tbi.setCreatedAt(Calendar.getInstance().getTime());
            tbi.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem tmpPh = tbi.getPharmaceuticalBillItem();
            tbi.setPharmaceuticalBillItem(null);

            if (tbi.getPrescription() != null) {
                if (tbi.getPrescription().getId() == null) {
                    prescriptionFacade.create(tbi.getPrescription());
                } else {
                    prescriptionFacade.edit(tbi.getPrescription());
                }
            }

            if (tbi.getId() == null) {
                getBillItemFacade().create(tbi);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            tbi.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(tbi);

            double qtyL = tbi.getPharmaceuticalBillItem().getQtyInUnit() + tbi.getPharmaceuticalBillItem().getFreeQtyInUnit();

            //Deduct Stock
            boolean returnFlag = getPharmacyBean().deductFromStock(tbi.getPharmaceuticalBillItem().getStock(),
                    Math.abs(qtyL), tbi.getPharmaceuticalBillItem(), getPreBill().getDepartment());

            if (!returnFlag) {
                tbi.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(tbi.getPharmaceuticalBillItem());
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
                System.out.println(patient);
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

    @EJB
    private ConfigOptionFacade configOptionFacade;

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
        if (patientRequiredForPharmacySale) {
            if (getPatient() == null
                    || getPatient().getPerson() == null
                    || getPatient().getPerson().getName() == null
                    || getPatient().getPerson().getName().trim().isEmpty()) {
                JsfUtil.addErrorMessage("Please Select a Patient");
                billSettlingStarted = false;
                return;
            } else {
                pt = savePatient();
            }
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

//    public boolean errorCheckOnPaymentMethod() {
//        if (getPaymentSchemeController().checkPaymentMethodError(paymentMethod, getPaymentMethodData())) {
//            return true;
//        }
//
//        if (paymentMethod == PaymentMethod.PatientDeposit) {
//            if (!getPatient().getHasAnAccount()) {
//                JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
//                return true;
//            }
//            double creditLimitAbsolute = Math.abs(getPatient().getCreditLimit());
//            double runningBalance;
//            if (getPatient().getRunningBalance() != null) {
//                runningBalance = getPatient().getRunningBalance();
//            } else {
//                runningBalance = 0.0;
//            }
//            double availableForPurchase = runningBalance + creditLimitAbsolute;
//
//            if (netTotal > availableForPurchase) {
//                JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
//                return true;
//            }
//
//        }
//
//        if (paymentMethod == PaymentMethod.Staff) {
//            if (toStaff == null) {
//                JsfUtil.addErrorMessage("Please select Staff Member.");
//                return true;
//            }
//
//            if (toStaff.getCurrentCreditValue() + netTotal > toStaff.getCreditLimitQualified()) {
//                JsfUtil.addErrorMessage("No enough Credit.");
//                return true;
//            }
//        }
//
//        if (paymentMethod == PaymentMethod.Staff_Welfare) {
//            if (toStaff == null) {
//                JsfUtil.addErrorMessage("Please select Staff Member under welfare.");
//                return true;
//            }
//            if (Math.abs(toStaff.getAnnualWelfareUtilized()) + netTotal > toStaff.getAnnualWelfareQualified()) {
//                JsfUtil.addErrorMessage("No enough credit.");
//                return true;
//            }
//
//        }
//
//        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
//            if (getPaymentMethodData() == null) {
//                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
//                return true;
//            }
//            if (getPaymentMethodData().getPaymentMethodMultiple() == null) {
//                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
//                return true;
//            }
//            if (getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
//                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
//                return true;
//            }
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
//
//        }
//        return false;
//    }
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

//    public void calculateAllRates() {
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
        System.out.println("calculateBillItemDiscountRate");
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
        System.out.println("discountAllowed = " + discountAllowed);
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

        System.out.println("getPaymentScheme() = " + getPaymentScheme());
        if (getPaymentScheme() != null && discountAllowed) {
            System.out.println("getPaymentMethod() = " + getPaymentMethod());
            System.out.println("getPaymentScheme() = " + getPaymentScheme());
            System.out.println("getSessionController().getDepartment() = " + getSessionController().getDepartment());
            System.out.println("bi.getItem() = " + bi.getItem());
            PriceMatrix priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(getPaymentMethod(), getPaymentScheme(), getSessionController().getDepartment(), bi.getItem());

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
        if (getPaymentMethod() != null && discountAllowed) {
            PriceMatrix priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(getPaymentMethod(), getSessionController().getDepartment(), bi.getItem());

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
        if (getPaymentMethod() == PaymentMethod.Credit && toInstitution != null) {
            discountRate = toInstitution.getPharmacyDiscount();

            double dr;
            dr = (retailRate * discountRate) / 100;
            System.out.println("3 dr = " + dr);
            return dr;
        }
        System.out.println("no dr");
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
        editingQty = null;
        errorMessage = "";
        // paymentMethod = PaymentMethod.Cash; // Never do this. It shold be done in clear bill item
        paymentMethodData = null;
        setCashPaid(0.0);
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
        System.out.println("setPatient in PharmacySaleController");
        this.patient = patient;
        selectPaymentSchemeAsPerPatientMembership();
    }

    private void selectPaymentSchemeAsPerPatientMembership() {
        System.out.println("selectPaymentSchemeAsPerPatientMembership");
        System.out.println("patient = " + patient);
        if (patient == null) {
            return;
        }
        System.out.println("patient.getPerson().getMembershipScheme() = " + patient.getPerson().getMembershipScheme());
        if (patient.getPerson().getMembershipScheme() == null) {
            paymentScheme = null;
        } else {
            System.out.println("patient.getPerson().getMembershipScheme().getPaymentScheme() = " + patient.getPerson().getMembershipScheme().getPaymentScheme());
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
        System.out.println("listnerForPaymentMethodChange");
        if (paymentMethod == PaymentMethod.PatientDeposit) {
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
            getPaymentMethodData().getPatient_deposit().setTotalValue(netTotal);
            PatientDeposit pd = patientDepositController.checkDepositOfThePatient(patient, sessionController.getDepartment());
            if (pd != null && pd.getId() != null) {
                getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
            }
        } else if (paymentMethod == PaymentMethod.Card) {
            getPaymentMethodData().getCreditCard().setTotalValue(netTotal);
            System.out.println("this = " + this);
        } else if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
            getPaymentMethodData().getPatient_deposit().setTotalValue(calculatRemainForMultiplePaymentTotal());
            PatientDeposit pd = patientDepositController.checkDepositOfThePatient(patient, sessionController.getDepartment());

            if (pd != null && pd.getId() != null) {
                System.out.println("pd = " + pd);
                boolean hasPatientDeposit = false;
                for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    System.out.println("cd = " + cd);
                    if (cd.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                        System.out.println("cd = " + cd);
                        hasPatientDeposit = true;
                        cd.getPaymentMethodData().getPatient_deposit().setPatient(patient);
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

    public StockLight getStockLight() {
        return stockLight;
    }

    public void setStockLight(StockLight stockLight) {
        this.stockLight = stockLight;
    }

}
