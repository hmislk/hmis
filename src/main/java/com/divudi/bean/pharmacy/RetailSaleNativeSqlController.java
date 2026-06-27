/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.ControllerWithMultiplePayments;
import com.divudi.bean.common.ControllerWithPatient;
import com.divudi.service.DiscountSchemeValidationService;
import com.divudi.bean.common.PatientDepositController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BooleanMessage;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dto.BillItemData;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.data.dto.PrintBillData;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.clinical.ClinicalFindingValue;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.clinical.Prescription;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyService;
import com.divudi.service.pharmacy.RetailSaleNativeSqlService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

/**
 * Controller for the native-SQL pharmacy retail sale page.
 *
 * The settle path uses RetailSaleNativeSqlService (native SQL inserts), avoiding
 * the EAGER cascade load (Stock → ItemBatch → Item) that is the dominant cold-start
 * cost in the original PharmacySaleController settle path.
 *
 * Patterned on InpatientDirectIssueNativeSqlController (issue #20214).
 * Issue: #20260
 */
@Named
@SessionScoped
public class RetailSaleNativeSqlController implements Serializable, ControllerWithPatient, ControllerWithMultiplePayments {

    private static final Logger LOGGER = Logger.getLogger(RetailSaleNativeSqlController.class.getName());

    // ---- CDI ----
    @Inject
    private SessionController sessionController;
    @Inject
    private DrawerController drawerController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private ConfigOptionController configOptionController;
    @Inject
    private PriceMatrixController priceMatrixController;
    @Inject
    private PatientDepositController patientDepositController;

    // ---- EJB ----
    @EJB
    private StockFacade stockFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    private BillNumberGenerator billNumberGenerator;
    @EJB
    private PharmacyService pharmacyService;
    @EJB
    private RetailSaleNativeSqlService nativeSqlService;
    @EJB
    private com.divudi.service.PaymentService paymentService;
    @EJB
    private com.divudi.service.pharmacy.PharmacySubstituteService pharmacySubstituteService;
    @EJB
    private DiscountSchemeValidationService discountSchemeValidationService;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PersonFacade personFacade;

    // ---- Working state ----
    private Patient patient;
    private Bill preBill;
    private PrintBillData printBill;
    private List<BillItemData> printBillItems;
    private BillItem billItem;
    private Integer intQty;
    private StockDTO stockDto;
    private Long selectedStockId;
    private List<StockDTO> lastAutocompleteResults;
    private List<BillItemData> billItemDataList;
    private boolean billPreview = false;
    private boolean billSettlingStarted = false;
    private boolean patientDetailsEditable = false;
    private String comment = "";
    private double cashPaid;
    private double balance;
    private PaymentMethod paymentMethod;
    private PaymentScheme paymentScheme;
    private PaymentMethodData paymentMethodData;
    private Staff toStaff;
    private Institution toInstitution;
    private List<ClinicalFindingValue> allergyListOfPatient;

    @PostConstruct
    public void init() {
        resetAll();
    }

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    public String pharmacyRetailSaleNative() {
        resetAll();
        billSettlingStarted = false;
        return "/pharmacy/pharmacy_bill_retail_sale_native?faces-redirect=true";
    }

    @SuppressWarnings("unchecked")
    public String viewByBillId(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        resetAll();
        Object[] result = nativeSqlService.loadViewDataByBillId(billId);
        if (result == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        printBill = (PrintBillData) result[0];
        printBillItems = (List<BillItemData>) result[1];
        billPreview = true;
        return "/pharmacy/pharmacy_bill_retail_sale_native?faces-redirect=true";
    }

    // -----------------------------------------------------------------------
    // Settle
    // -----------------------------------------------------------------------

    public String settleBillWithPay() {
        if (billSettlingStarted) {
            return null;
        }
        billSettlingStarted = true;

        if (sessionController.getApplicationPreference().isCheckPaymentSchemeValidation()) {
            if (paymentScheme == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select Payment Scheme");
                return null;
            }
        }

        if (paymentMethod == null) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("Please select Payment Method");
            return null;
        }

        if (billItemDataList == null || billItemDataList.isEmpty()) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("Please add items to the bill.");
            return null;
        }

        if (paymentMethod == PaymentMethod.Cash
                && configOptionApplicationController.getBooleanValueByKey(
                        "Need to Enter the Cash Tendered Amount to Settle Pharmacy Retail Bill", true)) {
            if (cashPaid == 0.0) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter the paid amount.");
                return null;
            }
            if (cashPaid < getPreBill().getNetTotal()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Tendered amount is less than net total.");
                return null;
            }
        }

        if ((getPatient().getMobileNumberStringTransient() == null
                || getPatient().getMobileNumberStringTransient().trim().isEmpty()
                || getPatient().getPerson().getName().trim().isEmpty())
                && configOptionApplicationController.getBooleanValueByKey("Patient details are required for retail sale")) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("Please enter patient name and mobile number.");
            return null;
        }

        if (paymentMethod == PaymentMethod.Card) {
            String cardNumber = getPaymentMethodData().getCreditCard().getNo();
            if ((cardNumber == null || cardNumber.trim().isEmpty() || cardNumber.trim().length() != 4)
                    && configOptionApplicationController.getBooleanValueByKey("Pharmacy retail sale CreditCard last digits is Mandatory")) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter a Credit Card last 4 digits");
                return null;
            }
        }

        if (paymentMethod == PaymentMethod.Staff_Welfare
                && configOptionApplicationController.getBooleanValueByKey(
                        "Pharmacy discount should be staff when select Staff_welfare as payment method", false)) {
            if (paymentScheme == null || !paymentScheme.getName().equalsIgnoreCase("staff")) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Staff Welfare needs to set staff discount scheme.");
                return null;
            }
        }

        BooleanMessage discountValidation = discountSchemeValidationService.validateDiscountScheme(
                paymentMethod, paymentScheme, getPaymentMethodData());
        if (!discountValidation.isFlag()) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage(discountValidation.getMessage());
            return null;
        }

        boolean patientRequired = configOptionApplicationController.getBooleanValueByKey(
                "Patient is required in Pharmacy Retail Sale Bill for "
                + sessionController.getDepartment().getName(), false);
        if (patientRequired) {
            if (getPatient() == null || getPatient().getPerson() == null
                    || getPatient().getPerson().getName() == null
                    || getPatient().getPerson().getName().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please Select a Patient");
                return null;
            }
        }

        if (getPatient().isBlacklisted()) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("This patient is blacklisted from the system. Can't Bill.");
            return null;
        }

        if (configOptionApplicationController.getBooleanValueByKey(
                "Need Patient Title And Gender To Save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getTitle() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select title.");
                return null;
            }
            if (getPatient().getPerson().getSex() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select gender.");
                return null;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey(
                "Need Patient Name to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getName() == null
                    || getPatient().getPerson().getName().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter name.");
                return null;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey(
                "Need Patient Age to Save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getDob() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient date of birth.");
                return null;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey(
                "Need Patient Phone Number to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getPhone() == null
                    || getPatient().getPerson().getPhone().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter phone number.");
                return null;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey(
                "Need Patient Address to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getAddress() == null
                    || getPatient().getPerson().getAddress().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient address.");
                return null;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey(
                "Need Patient Mail to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getEmail() == null
                    || getPatient().getPerson().getEmail().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient email.");
                return null;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey(
                "Need Patient NIC to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getNic() == null
                    || getPatient().getPerson().getNic().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient NIC.");
                return null;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey(
                "Need Patient Area to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getArea() == null
                    || getPatient().getPerson().getArea().getName() == null
                    || getPatient().getPerson().getArea().getName().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select patient area.");
                return null;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey(
                "Need Referring Doctor to settlle bill in Pharmacy Sale", false)) {
            if (getPreBill().getReferredBy() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select referring doctor.");
                return null;
            }
        }

        // Ensure discounts reflect current payment scheme before building the bill
        recalculateDiscountsForAll();
        calTotal();

        // For Multiple Payment Methods, the entered components must cover the net
        // total (and balance-backed components must have sufficient balance)
        // before the bill is settled as paid. Mirrors the legacy retail sale
        // page validation (PharmacySaleController.errorCheck).
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods
                && validateMultiplePaymentsFailed()) {
            billSettlingStarted = false;
            return null;
        }

        // Save or update the patient record
        savePatientIfNeeded();

        // Build pre-bill and sale bill (replicates the two-bill structure of the old flow)
        PreBill preBillEntity = buildPreBill();
        BilledBill saleBillEntity = buildSaleBill(preBillEntity);

        // Stamp dept/institution IDs on each item (needed by native service for StockHistory aggregates)
        long deptId = sessionController.getLoggedUser().getDepartment().getId();
        long instId = sessionController.getLoggedUser().getDepartment().getInstitution().getId();
        for (BillItemData bid : billItemDataList) {
            bid.setDepartmentId(deptId);
            bid.setInstitutionId(instId);
        }

        try {
            List<Payment> payments = nativeSqlService.settle(preBillEntity, saleBillEntity, billItemDataList, paymentMethod, getPaymentMethodData(), paymentScheme);
            // Debit patient deposits and update staff/credit-company balances for
            // balance-backed components (no-op for cash/card/etc.).
            paymentService.updateBalances(payments);
            drawerController.updateDrawerForIns(payments);

            buildPrintBill(saleBillEntity);
            clearBill();
            clearBillItem();
            billPreview = true;
            billSettlingStarted = false;
            JsfUtil.addSuccessMessage("Bill settled successfully.");
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Native retail sale settle failed", e);
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("Failed to settle bill: " + e.getMessage());
        }
        return null;
    }

    private void savePatientIfNeeded() {
        try {
            if (patient == null) {
                return;
            }
            if (patient.getPerson() != null) {
                patient.setMobileNumberStringTransient(patient.getMobileNumberStringTransient());
                patient.setPhoneNumberStringTransient(patient.getPhoneNumberStringTransient());
            }
            if (patient.getId() == null) {
                if (patient.getPerson() != null) {
                    personFacade.create(patient.getPerson());
                }
                patientFacade.create(patient);
            } else {
                if (patient.getPerson() != null) {
                    personFacade.edit(patient.getPerson());
                }
                patientFacade.edit(patient);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not save patient", e);
        }
    }

    private PreBill buildPreBill() {
        String billNo = generateBillNumber();

        double netTot = 0.0;
        double grossTot = 0.0;
        double discountTot = 0.0;
        for (BillItemData bid : billItemDataList) {
            netTot += Math.abs(bid.getNetValue());
            grossTot += Math.abs(bid.getGrossValue());
            discountTot += bid.getDiscountValue();
        }

        PreBill pb = new PreBill();
        pb.setBillType(BillType.PharmacyPre);
        pb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        pb.setInsId(billNo);
        pb.setDeptId(billNo);
        pb.setDepartment(sessionController.getLoggedUser().getDepartment());
        pb.setInstitution(sessionController.getLoggedUser().getDepartment().getInstitution());
        pb.setPatient(patient);
        pb.setFromDepartment(sessionController.getLoggedUser().getDepartment());
        pb.setFromInstitution(sessionController.getLoggedUser().getDepartment().getInstitution());
        pb.setBillDate(new Date());
        pb.setBillTime(new Date());
        pb.setCreatedAt(Calendar.getInstance().getTime());
        pb.setCreater(sessionController.getLoggedUser());
        pb.setComments(comment);
        pb.setPaymentMethod(paymentMethod);
        pb.setPaymentScheme(paymentScheme);
        pb.setTotal(grossTot);
        pb.setNetTotal(netTot);
        pb.setGrantTotal(grossTot);
        pb.setDiscount(discountTot);
        if (paymentMethod == PaymentMethod.Credit || paymentMethod == PaymentMethod.Staff) {
            pb.setBalance(netTot);
            pb.setPaidAmount(0.0);
        } else {
            pb.setBalance(0.0);
            pb.setPaidAmount(netTot);
        }

        if (getPreBill().getReferredBy() != null) {
            pb.setReferredBy(getPreBill().getReferredBy());
        }
        return pb;
    }

    private BilledBill buildSaleBill(PreBill pb) {
        BilledBill sb = new BilledBill();
        sb.setBillType(BillType.PharmacySale);
        sb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE);
        sb.setInsId(pb.getInsId());
        sb.setDeptId(pb.getDeptId());
        sb.setDepartment(pb.getDepartment());
        sb.setInstitution(pb.getInstitution());
        sb.setPatient(pb.getPatient());
        sb.setFromDepartment(pb.getFromDepartment());
        sb.setFromInstitution(pb.getFromInstitution());
        sb.setBillDate(pb.getBillDate());
        sb.setBillTime(pb.getBillTime());
        sb.setCreatedAt(pb.getCreatedAt());
        sb.setCreater(pb.getCreater());
        sb.setComments(pb.getComments());
        sb.setCashPaid(cashPaid);
        sb.setPaymentMethod(paymentMethod);
        sb.setPaymentScheme(paymentScheme);
        sb.setTotal(pb.getTotal());
        sb.setNetTotal(pb.getNetTotal());
        sb.setGrantTotal(pb.getGrantTotal());
        sb.setBalance(paymentMethod == PaymentMethod.Credit ? pb.getNetTotal() : 0.0);
        sb.setPaidAmount(paymentMethod == PaymentMethod.Credit ? 0.0 : pb.getNetTotal());
        sb.setReferredBy(pb.getReferredBy());

        if (paymentMethod == PaymentMethod.Credit && getPaymentMethodData().getCredit().getInstitution() != null) {
            sb.setToInstitution(getPaymentMethodData().getCredit().getInstitution());
            sb.setCreditCompany(getPaymentMethodData().getCredit().getInstitution());
        }
        if ((paymentMethod == PaymentMethod.Staff || paymentMethod == PaymentMethod.Staff_Welfare)
                && toStaff != null) {
            sb.setToStaff(toStaff);
        }
        return sb;
    }

    private String generateBillNumber() {
        if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            return billNumberGenerator.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            return billNumberGenerator.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            return billNumberGenerator.institutionBillNumberGeneratorYearlyWithPrefixInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        }
        return billNumberGenerator.departmentBillNumberGeneratorYearly(
                sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
    }

    private void buildPrintBill(Bill bill) {
        PrintBillData pbd = new PrintBillData();

        // Department / institution header
        Department dept = sessionController.getLoggedUser().getDepartment();
        pbd.setDepartmentName(dept.getName());
        pbd.setDepartmentPrintingName(dept.getPrintingName() != null ? dept.getPrintingName() : dept.getName());
        pbd.setDepartmentTelephone1(dept.getTelephone1());
        pbd.setDepartmentAddress(dept.getAddress());
        if (dept.getInstitution() != null) {
            pbd.setInstitutionName(dept.getInstitution().getName());
            pbd.setInstitutionAddress(dept.getInstitution().getAddress());
            pbd.setInstitutionEmail(dept.getInstitution().getEmail());
            pbd.setInstitutionWeb(dept.getInstitution().getWeb());
        }

        // Bill identity
        pbd.setBillNo(bill.getDeptId());
        pbd.setCreatedAt(bill.getCreatedAt());
        if (bill.getCreater() != null) {
            pbd.setCreatorName(bill.getCreater().getName());
        }

        // Patient
        if (patient != null && patient.getPerson() != null) {
            pbd.setPatientName(patient.getPerson().getNameWithTitle());
            pbd.setPatientPhone(patient.getPerson().getPhone());
            pbd.setPatientPhn(patient.getPhn());
        }

        // Payment
        pbd.setPaymentMethodLabel(paymentMethod != null ? paymentMethod.getLabel() : "");
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            pbd.setPayments(buildPrintPaymentLines());
        }
        if (paymentScheme != null) {
            pbd.setPaymentSchemePrintingName(
                    paymentScheme.getPrintingName() != null ? paymentScheme.getPrintingName() : paymentScheme.getName());
        }
        pbd.setComment(comment);

        // Targets for credit/staff/dept bills
        if (toStaff != null && toStaff.getPerson() != null) {
            pbd.setToStaffName(toStaff.getPerson().getNameWithTitle());
        }
        if (paymentMethod == PaymentMethod.Credit
                && getPaymentMethodData().getCredit().getInstitution() != null) {
            pbd.setToInstitutionName(getPaymentMethodData().getCredit().getInstitution().getName());
        }

        // Totals
        double grossTot = 0.0;
        double discTot  = 0.0;
        double netTot   = 0.0;
        for (BillItemData bid : billItemDataList) {
            grossTot += Math.abs(bid.getGrossValue());
            discTot  += bid.getDiscountValue();
            netTot   += Math.abs(bid.getNetValue());
        }
        pbd.setTotal(grossTot);
        pbd.setDiscount(discTot);
        pbd.setNetTotal(netTot);
        pbd.setDiscountPercentPharmacy(grossTot > 0 ? (discTot / grossTot) * 100.0 : 0.0);
        // For Multiple Payment Methods the Tendered field is not used; the amount
        // paid is the sum of the entered components. Show that (and a zero
        // balance) instead of the unused single cashPaid (which is 0.0).
        double tendered = cashPaid;
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            tendered = 0.0;
            for (PrintBillData.PaymentLine line : pbd.getPayments()) {
                tendered += line.getValue();
            }
        }
        pbd.setCashPaid(tendered);
        pbd.setBalance(tendered - netTot);

        printBill = pbd;

        List<BillItemData> printCopy = new ArrayList<>();
        for (BillItemData src : billItemDataList) {
            BillItemData p = new BillItemData();
            p.setItemId(src.getItemId());
            p.setItemName(src.getItemName());
            p.setQty(Math.abs(src.getQty()));
            p.setRate(src.getRate());
            p.setNetRate(src.getNetRate());
            p.setNetValue(Math.abs(src.getNetValue()));
            p.setGrossValue(Math.abs(src.getGrossValue()));
            p.setDoe(src.getDoe());
            printCopy.add(p);
        }
        printBillItems = printCopy;
    }

    /**
     * Builds one {@link PrintBillData.PaymentLine} per entered component of a
     * Multiple Payment Methods bill, so the printout itemises each payment
     * (e.g. "Cash 50.00", "Credit Card 145.90") instead of showing the bundled
     * "Multiple Payment Methods" label with a single value. Components with no
     * value are skipped (matching the settle path).
     */
    private List<PrintBillData.PaymentLine> buildPrintPaymentLines() {
        List<PrintBillData.PaymentLine> lines = new ArrayList<>();
        if (getPaymentMethodData().getPaymentMethodMultiple() == null
                || getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
            return lines;
        }
        for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
            if (cd == null || cd.getPaymentMethod() == null || cd.getPaymentMethodData() == null) {
                continue;
            }
            PaymentMethodData cpmd = cd.getPaymentMethodData();
            double value = 0.0;
            String reference = "";
            switch (cd.getPaymentMethod()) {
                case Cash:
                    value = cpmd.getCash().getTotalValue();
                    break;
                case Card:
                    value = cpmd.getCreditCard().getTotalValue();
                    reference = cpmd.getCreditCard().getNo();
                    break;
                case Cheque:
                    value = cpmd.getCheque().getTotalValue();
                    reference = cpmd.getCheque().getNo();
                    break;
                case ewallet:
                    value = cpmd.getEwallet().getTotalValue();
                    reference = cpmd.getEwallet().getReferenceNo();
                    break;
                case Slip:
                    value = cpmd.getSlip().getTotalValue();
                    reference = cpmd.getSlip().getReferenceNo();
                    break;
                case OnlineSettlement:
                    value = cpmd.getOnlineSettlement().getTotalValue();
                    reference = cpmd.getOnlineSettlement().getReferenceNo();
                    break;
                case IOU:
                    value = cpmd.getIou().getTotalValue();
                    reference = cpmd.getIou().getReferenceNo();
                    break;
                case Credit:
                    value = cpmd.getCredit().getTotalValue();
                    reference = cpmd.getCredit().getReferenceNo();
                    break;
                case PatientDeposit:
                    value = cpmd.getPatient_deposit().getTotalValue();
                    break;
                case Staff:
                    value = cpmd.getStaffCredit().getTotalValue();
                    break;
                case Staff_Welfare:
                    value = cpmd.getStaffWelfare().getTotalValue();
                    break;
                default:
                    break;
            }
            if (value <= 0.0) {
                continue;
            }
            lines.add(new PrintBillData.PaymentLine(
                    cd.getPaymentMethod().getLabel(), value, reference == null ? "" : reference));
        }
        return lines;
    }

    // -----------------------------------------------------------------------
    // Add item
    // -----------------------------------------------------------------------

    public void addBillItem() {
        if (stockDto == null || selectedStockId == null || stockDto.getItemId() == null) {
            JsfUtil.addErrorMessage("No stock selected.");
            return;
        }
        if (intQty == null || intQty <= 0) {
            JsfUtil.addErrorMessage("Please enter a quantity.");
            return;
        }
        if (stockDto.getDateOfExpire() != null
                && stockDto.getDateOfExpire().before(CommonFunctions.getCurrentDateTime())) {
            JsfUtil.addErrorMessage("You are not allowed to select expired items.");
            return;
        }
        if (stockDto.getStockQty() != null && intQty > stockDto.getStockQty()) {
            JsfUtil.addErrorMessage("No sufficient stock available.");
            return;
        }
        if (billItemDataList != null) {
            for (BillItemData existing : billItemDataList) {
                if (selectedStockId.equals(existing.getStockId())) {
                    JsfUtil.addErrorMessage("This batch is already added to the bill. Edit the quantity instead.");
                    return;
                }
            }
        }

        double qty = intQty.doubleValue();

        double[] batchRates = fetchBatchRates(stockDto.getItemBatchId());
        double batchRetailRate    = batchRates[0];
        double batchPurchaseRate  = batchRates[1];
        double batchWholesaleRate = batchRates[2];
        Double batchCostRate      = batchRates[3] > 0 ? batchRates[3] : null;

        long ampItemId = resolveAmpItemId(stockDto.getItemId());

        BillItemData bid = new BillItemData();
        bid.setItemId(stockDto.getItemId());
        bid.setItemName(stockDto.getItemName());
        bid.setAmpItemId(ampItemId);
        bid.setStockId(selectedStockId);
        bid.setItemBatchId(stockDto.getItemBatchId());
        bid.setQty(qty);
        bid.setPbiQty(-Math.abs(qty));
        bid.setFreeQty(0.0);
        bid.setRetailRate(stockDto.getRetailRate() != null ? stockDto.getRetailRate() : 0.0);
        bid.setPurchaseRate(batchPurchaseRate);
        bid.setWholesaleRate(batchWholesaleRate);
        bid.setCostRate(batchCostRate != null ? batchCostRate : batchPurchaseRate);
        bid.setBatchRetailRate(batchRetailRate);
        bid.setBatchPurchaseRate(batchPurchaseRate);
        bid.setBatchWholesaleRate(batchWholesaleRate);
        bid.setBatchCostRate(batchCostRate);
        bid.setDoe(stockDto.getDateOfExpire());
        bid.setDescription(stockDto.getItemName());
        bid.setCreatedAt(new Date());
        bid.setCreaterId(sessionController.getLoggedUser().getId());

        double lineRetailRate = stockDto.getRetailRate() != null ? stockDto.getRetailRate() : 0.0;
        double grossValue = lineRetailRate * qty;
        double discountPct = 0.0;
        double discountValue = 0.0;
        try {
            Item itemRef = itemFacade.find(stockDto.getItemId());
            if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing", false)
                    && patient != null && patient.getId() != null) {
                if (allergyListOfPatient == null) {
                    allergyListOfPatient = pharmacyService.getAllergyListForPatient(patient);
                }
                String allergyMsg = pharmacyService.getAllergyMessageForItem(patient, itemRef, allergyListOfPatient);
                if (!allergyMsg.isEmpty()) {
                    JsfUtil.addErrorMessage(allergyMsg);
                    return;
                }
            }
            if (Boolean.TRUE.equals(itemRef.isDiscountAllowed())) {
                Double pct = priceMatrixController.getPaymentSchemeDiscountPercent(
                        paymentMethod, paymentScheme, sessionController.getDepartment(), itemRef);
                discountPct = pct != null ? pct : 0.0;
                discountValue = (discountPct / 100.0) * grossValue;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Discount lookup failed for item {0}: {1}",
                    new Object[]{stockDto.getItemId(), e.getMessage()});
        }
        double netValue = grossValue - discountValue;
        double netRate = qty > 0 ? netValue / qty : lineRetailRate;

        bid.setRate(lineRetailRate);
        bid.setNetRate(netRate);
        bid.setDiscountPercent(discountPct);
        bid.setDiscountValue(discountValue);
        bid.setMarginValue(0.0);
        bid.setNetValue(-netValue);
        bid.setGrossValue(-grossValue);

        if (billItemDataList == null) {
            billItemDataList = new ArrayList<>();
        }
        billItemDataList.add(bid);

        calTotal();
        clearBillItem();
    }

    private double[] fetchBatchRates(Long itemBatchId) {
        if (itemBatchId == null) {
            return new double[]{0, 0, 0, 0};
        }
        Map<String, Object> params = new HashMap<>();
        params.put("id", itemBatchId);
        String jpql = "SELECT ib.retailsaleRate, ib.purcahseRate, ib.wholesaleRate, COALESCE(ib.costRate, 0) "
                + "FROM ItemBatch ib WHERE ib.id = :id";
        try {
            Object[] row = (Object[]) itemBatchFacade.findLightsByJpql(jpql, params, TemporalType.DATE, 1)
                    .stream().findFirst().orElse(null);
            if (row == null) return new double[]{0, 0, 0, 0};
            return new double[]{toDouble(row[0]), toDouble(row[1]), toDouble(row[2]), toDouble(row[3])};
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not fetch batch rates for itemBatchId={0}", itemBatchId);
            return new double[]{0, 0, 0, 0};
        }
    }

    private Double fetchCurrentStockQty(Long stockId) {
        if (stockId == null) {
            return null;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("id", stockId);
        List<?> result = stockFacade.findLightsByJpql(
                "SELECT s.stock FROM Stock s WHERE s.id = :id",
                params, TemporalType.DATE, 1);
        if (result == null || result.isEmpty() || result.get(0) == null) {
            return null;
        }
        return ((Number) result.get(0)).doubleValue();
    }

    private long resolveAmpItemId(Long itemId) {
        if (itemId == null) return 0L;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("id", itemId);
            List<?> result = itemFacade.findLightsByJpql(
                    "SELECT i.amp.id FROM Item i WHERE i.id = :id AND TYPE(i) = Ampp",
                    params, TemporalType.DATE, 1);
            if (result != null && !result.isEmpty() && result.get(0) != null) {
                return ((Number) result.get(0)).longValue();
            }
            return itemId;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not resolve AMP for itemId={0}", itemId);
            return itemId;
        }
    }

    private static double toDouble(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    // -----------------------------------------------------------------------
    // Edit row
    // -----------------------------------------------------------------------

    public void onEdit(RowEditEvent<BillItemData> event) {
        BillItemData bid = event.getObject();
        if (bid.getQty() <= 0) {
            bid.setQty(0);
            JsfUtil.addErrorMessage("Quantity must be greater than zero.");
            return;
        }
        if (bid.getStockId() != null) {
            try {
                Double availableQty = fetchCurrentStockQty(bid.getStockId());
                if (availableQty != null && bid.getQty() > availableQty) {
                    bid.setQty(availableQty);
                    JsfUtil.addErrorMessage("Quantity cannot exceed available stock ("
                            + availableQty.intValue() + "). Quantity has been set to the maximum available.");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not verify stock qty for stockId={0}", bid.getStockId());
            }
        }
        double absQty = Math.abs(bid.getQty());
        double gross = absQty * bid.getRate();
        double discountVal = (bid.getDiscountPercent() / 100.0) * gross;
        double netVal = gross - discountVal;
        bid.setGrossValue(-gross);
        bid.setDiscountValue(discountVal);
        bid.setNetValue(-netVal);
        bid.setNetRate(absQty > 0 ? netVal / absQty : bid.getRate());
        bid.setPbiQty(-absQty);
        calTotal();
    }

    public void onEditCancel(RowEditEvent<BillItemData> event) {
        calTotal();
    }

    public void removeBillItem(BillItemData bid) {
        if (billItemDataList != null) {
            billItemDataList.remove(bid);
        }
        calTotal();
    }

    // -----------------------------------------------------------------------
    // Autocomplete
    // -----------------------------------------------------------------------

    public List<StockDTO> completeAvailableStockOptimizedDto(String qry) {
        if (qry == null || qry.trim().isEmpty()) {
            lastAutocompleteResults = new ArrayList<>();
            return lastAutocompleteResults;
        }
        qry = qry.replaceAll("[\\n\\r]", "").trim();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("department", sessionController.getLoggedUser().getDepartment());
        parameters.put("stockMin", 0.0);
        parameters.put("query", "%" + qry + "%");

        boolean searchByItemCode = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by item code", true);
        boolean searchByBarcode = qry.length() > 6
                ? configOptionApplicationController.getBooleanValueByKey("Enable search medicines by barcode", true)
                : configOptionApplicationController.getBooleanValueByKey("Enable search medicines by barcode", false);
        boolean searchByGeneric = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by generic name(VMP)", false);

        StringBuilder sql = new StringBuilder(
                "SELECT NEW com.divudi.core.data.dto.StockDTO("
                + "i.id, i.itemBatch.id, i.itemBatch.item.id, i.itemBatch.item.name, i.itemBatch.item.code, "
                + "i.itemBatch.item.name, i.itemBatch.retailsaleRate, i.stock, i.itemBatch.dateOfExpire) "
                + "FROM Stock i "
                + "WHERE i.stock > :stockMin "
                + "AND i.department = :department "
                + "AND (i.itemBatch.item.name LIKE :query ");

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

        lastAutocompleteResults = (List<StockDTO>) stockFacade.findLightsByJpql(
                sql.toString(), parameters, TemporalType.TIMESTAMP, 20);
        return lastAutocompleteResults != null ? lastAutocompleteResults : new ArrayList<>();
    }

    public void handleSelect(SelectEvent<StockDTO> event) {
        StockDTO selected = event.getObject();
        this.stockDto = selected;
        this.selectedStockId = selected != null ? selected.getId() : null;
        if (billItem == null) getBillItem();
        billItem.setNetRate(selected != null && selected.getRetailRate() != null ? selected.getRetailRate() : 0.0);
    }

    // -----------------------------------------------------------------------
    // Totals
    // -----------------------------------------------------------------------

    public void calTotal() {
        double netTot = 0.0;
        double grossTot = 0.0;
        double discountTot = 0.0;
        if (billItemDataList != null) {
            for (BillItemData bid : billItemDataList) {
                netTot += Math.abs(bid.getNetValue());
                grossTot += Math.abs(bid.getGrossValue());
                discountTot += bid.getDiscountValue();
            }
        }
        getPreBill().setNetTotal(netTot);
        getPreBill().setTotal(grossTot);
        getPreBill().setGrantTotal(grossTot);
        getPreBill().setDiscount(discountTot);
        balance = cashPaid - netTot;
    }

    public void listnerForPaymentMethodChange() {
        recalculateDiscountsForAll();
        calTotal();
    }

    // ===================================================================
    // On-demand substitute (alternative) medicines (issue #21697)
    // ===================================================================
    private BillItemData itemDataForSubstitution;
    private com.divudi.core.data.dto.StockDTO selectedSubstituteStock;
    private List<com.divudi.core.data.dto.StockDTO> substituteStocks;

    public void prepareSubstitute(BillItemData bid) {
        itemDataForSubstitution = bid;
        selectedSubstituteStock = null;
        substituteStocks = new ArrayList<>();
        if (bid == null || bid.getItemId() == null) {
            return;
        }
        Item item = itemFacade.find(bid.getItemId());
        if (item == null) {
            return;
        }
        double requiredQty = Math.abs(bid.getQty());
        substituteStocks = pharmacySubstituteService.findSubstituteStocks(item, sessionController.getDepartment(), requiredQty);
    }

    public void replaceSelectedSubstitute() {
        if (itemDataForSubstitution == null || selectedSubstituteStock == null
                || selectedSubstituteStock.getStockId() == null) {
            JsfUtil.addErrorMessage("Please select a substitute stock.");
            return;
        }

        com.divudi.core.data.dto.StockDTO sub = selectedSubstituteStock;
        BillItemData bid = itemDataForSubstitution;

        // Same quantity as the line being substituted. All rate/batch fields come
        // from the single native lookup already on the DTO - no extra queries.
        double qty = Math.abs(bid.getQty());

        double batchRetailRate = sub.getRetailRate() != null ? sub.getRetailRate() : 0.0;
        double batchPurchaseRate = sub.getPurchaseRate() != null ? sub.getPurchaseRate() : 0.0;
        double batchWholesaleRate = sub.getWholesaleRate() != null ? sub.getWholesaleRate() : 0.0;
        Double batchCostRate = (sub.getCostRate() != null && sub.getCostRate() > 0) ? sub.getCostRate() : null;

        long ampItemId = resolveAmpItemId(sub.getItemId());

        bid.setItemId(sub.getItemId());
        bid.setItemName(sub.getItemName());
        bid.setAmpItemId(ampItemId);
        bid.setStockId(sub.getStockId());
        bid.setItemBatchId(sub.getItemBatchId());
        bid.setPbiQty(-Math.abs(qty));
        bid.setRetailRate(batchRetailRate);
        bid.setPurchaseRate(batchPurchaseRate);
        bid.setWholesaleRate(batchWholesaleRate);
        bid.setCostRate(batchCostRate != null ? batchCostRate : batchPurchaseRate);
        bid.setBatchRetailRate(batchRetailRate);
        bid.setBatchPurchaseRate(batchPurchaseRate);
        bid.setBatchWholesaleRate(batchWholesaleRate);
        bid.setBatchCostRate(batchCostRate);
        bid.setDoe(sub.getDateOfExpire());
        bid.setDescription(sub.getItemName());

        double lineRetailRate = batchRetailRate;
        double grossValue = lineRetailRate * qty;
        double discountPct = 0.0;
        double discountValue = 0.0;
        try {
            Item substituteItem = itemFacade.find(sub.getItemId());
            if (substituteItem != null && Boolean.TRUE.equals(substituteItem.isDiscountAllowed())) {
                Double pct = priceMatrixController.getPaymentSchemeDiscountPercent(
                        paymentMethod, paymentScheme, sessionController.getDepartment(), substituteItem);
                discountPct = pct != null ? pct : 0.0;
                discountValue = (discountPct / 100.0) * grossValue;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Discount lookup failed for substitute item {0}: {1}",
                    new Object[]{sub.getItemId(), e.getMessage()});
        }
        double netValue = grossValue - discountValue;
        double netRate = qty > 0 ? netValue / qty : lineRetailRate;

        bid.setRate(lineRetailRate);
        bid.setNetRate(netRate);
        bid.setDiscountPercent(discountPct);
        bid.setDiscountValue(discountValue);
        bid.setMarginValue(0.0);
        bid.setNetValue(-netValue);
        bid.setGrossValue(-grossValue);

        calTotal();
        JsfUtil.addSuccessMessage("Stock replaced successfully.");
    }

    public BillItemData getItemDataForSubstitution() {
        return itemDataForSubstitution;
    }

    public void setItemDataForSubstitution(BillItemData itemDataForSubstitution) {
        this.itemDataForSubstitution = itemDataForSubstitution;
    }

    public com.divudi.core.data.dto.StockDTO getSelectedSubstituteStock() {
        return selectedSubstituteStock;
    }

    public void setSelectedSubstituteStock(com.divudi.core.data.dto.StockDTO selectedSubstituteStock) {
        this.selectedSubstituteStock = selectedSubstituteStock;
    }

    public List<com.divudi.core.data.dto.StockDTO> getSubstituteStocks() {
        return substituteStocks;
    }

    public void setSubstituteStocks(List<com.divudi.core.data.dto.StockDTO> substituteStocks) {
        this.substituteStocks = substituteStocks;
    }

    public void recalculateDiscountsForAll() {
        if (billItemDataList == null || billItemDataList.isEmpty()) {
            return;
        }
        for (BillItemData bid : billItemDataList) {
            if (bid.getItemId() == null) continue;
            try {
                Item itemRef = itemFacade.find(bid.getItemId());
                if (itemRef == null) continue;
                double grossValue = Math.abs(bid.getGrossValue());
                double discountPct = 0.0;
                double discountValue = 0.0;
                if (Boolean.TRUE.equals(itemRef.isDiscountAllowed())) {
                    Double pct = priceMatrixController.getPaymentSchemeDiscountPercent(
                            paymentMethod, paymentScheme, sessionController.getDepartment(), itemRef);
                    discountPct = pct != null ? pct : 0.0;
                    discountValue = (discountPct / 100.0) * grossValue;
                }
                double netValue = grossValue - discountValue;
                double qty = Math.abs(bid.getQty());
                double netRate = qty > 0 ? netValue / qty : bid.getRate();
                bid.setDiscountPercent(discountPct);
                bid.setDiscountValue(discountValue);
                bid.setNetValue(-netValue);
                bid.setNetRate(netRate);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Discount recalc failed for itemId={0}: {1}",
                        new Object[]{bid.getItemId(), e.getMessage()});
            }
        }
    }

    public void calculateDobFromAge() {
        // Called from age year/month/day keyup events to recalculate DOB
        if (patient != null && patient.getPerson() != null) {
            patient.getPerson().calDobFromAge();
        }
    }

    public void calculateBillItemListner() {
        if (stockDto != null && intQty != null) {
            double rate = stockDto.getRetailRate() != null ? stockDto.getRetailRate() : 0.0;
            getBillItem().setRate(rate);
            getBillItem().setNetRate(rate);
            getBillItem().setNetValue(rate * intQty);
        }
    }

    // -----------------------------------------------------------------------
    // Reset / clear
    // -----------------------------------------------------------------------

    public void resetAll() {
        patient = null;
        preBill = null;
        printBill = null;
        printBillItems = null;
        billItem = null;
        intQty = null;
        stockDto = null;
        selectedStockId = null;
        lastAutocompleteResults = null;
        billItemDataList = null;
        billPreview = false;
        billSettlingStarted = false;
        patientDetailsEditable = false;
        comment = "";
        cashPaid = 0.0;
        balance = 0.0;
        paymentMethod = null;
        paymentScheme = null;
        paymentMethodData = null;
        toStaff = null;
        toInstitution = null;
        allergyListOfPatient = null;
    }

    private void clearBill() {
        preBill = null;
        billItemDataList = null;
    }

    private void clearBillItem() {
        billItem = null;
        intQty = null;
        stockDto = null;
        selectedStockId = null;
        lastAutocompleteResults = null;
    }

    // -----------------------------------------------------------------------
    // Getters / Setters
    // -----------------------------------------------------------------------

    public Patient getPatient() {
        if (patient == null) {
            patient = new Patient();
        }
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
        allergyListOfPatient = null;
    }

    public Bill getPreBill() {
        if (preBill == null) {
            preBill = new Bill();
        }
        return preBill;
    }

    public void setPreBill(Bill preBill) {
        this.preBill = preBill;
    }

    public PrintBillData getPrintBill() {
        return printBill;
    }

    public List<BillItemData> getPrintBillItems() {
        return printBillItems;
    }

    public BillItem getBillItem() {
        if (billItem == null) {
            billItem = new BillItem();
            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.setBillItem(billItem);
            billItem.setPharmaceuticalBillItem(pbi);
        }
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public Integer getIntQty() {
        return intQty;
    }

    public void setIntQty(Integer intQty) {
        this.intQty = intQty;
    }

    public StockDTO getStockDto() {
        return stockDto;
    }

    public void setStockDto(StockDTO stockDto) {
        this.stockDto = stockDto;
        this.selectedStockId = stockDto != null ? stockDto.getId() : null;
    }

    public List<BillItemData> getBillItemDataList() {
        if (billItemDataList == null) {
            billItemDataList = new ArrayList<>();
        }
        return billItemDataList;
    }

    public void setBillItemDataList(List<BillItemData> billItemDataList) {
        this.billItemDataList = billItemDataList;
    }

    public boolean isBillPreview() {
        return billPreview;
    }

    public void setBillPreview(boolean billPreview) {
        this.billPreview = billPreview;
    }

    public boolean isBillSettlingStarted() {
        return billSettlingStarted;
    }

    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public double getCashPaid() {
        return cashPaid;
    }

    public void setCashPaid(double cashPaid) {
        this.cashPaid = cashPaid;
        calTotal();
    }

    public double getBalance() {
        return balance;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
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
                        case IOU:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getIou().getTotalValue();
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
        // Already fully covered (or over-paid): do not auto-fill a negative
        // remaining amount into the last component, which would later persist a
        // negative Payment.paidValue and distort drawer/balance updates.
        if (remainAmount <= 0.0) {
            return;
        }
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            int arrSize = getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
            if (arrSize == 0) {
                return; // No payment methods added yet
            }
            ComponentDetail pm = getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);
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
            } else if (pm.getPaymentMethod() == PaymentMethod.IOU) {
                if (pm.getPaymentMethodData().getIou().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getIou().setTotalValue(remainAmount);
                }
            }
        }
    }

    /**
     * Validates a Multiple Payment Methods bill before settling: there must be
     * at least one component, balance-backed components (Patient Deposit, Staff,
     * Staff Welfare) must have sufficient balance, and the sum of the entered
     * component values must match the bill net total (within a 1.0 tolerance).
     * Adds a user-facing error message and returns {@code true} when settling
     * must be aborted. Mirrors {@code PharmacySaleController.errorCheck}.
     */
    private boolean validateMultiplePaymentsFailed() {
        if (getPaymentMethodData().getPaymentMethodMultiple() == null
                || getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null
                || getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
            JsfUtil.addErrorMessage("No Details on multiple payment methods given");
            return true;
        }

        double netTotal = Math.abs(getPreBill().getNetTotal());
        double multiplePaymentMethodTotalValue = 0.0;
        for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
            if (cd.getPaymentMethod() == null || cd.getPaymentMethodData() == null) {
                continue;
            }
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
                case Slip:
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                    break;
                case OnlineSettlement:
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getOnlineSettlement().getTotalValue();
                    break;
                case IOU:
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getIou().getTotalValue();
                    break;
                case Credit:
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCredit().getTotalValue();
                    break;
                case PatientDeposit: {
                    double value = cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                    PatientDeposit pd = patientDepositController.getDepositOfThePatient(getPatient(), sessionController.getDepartment());
                    if (pd == null) {
                        JsfUtil.addErrorMessage("No Patient Deposit.");
                        return true;
                    }
                    if (value > pd.getBalance()) {
                        JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                        return true;
                    }
                    multiplePaymentMethodTotalValue += value;
                    break;
                }
                case Staff: {
                    double value = cd.getPaymentMethodData().getStaffCredit().getTotalValue();
                    Staff selectedStaff = cd.getPaymentMethodData().getStaffCredit().getToStaff();
                    if (value == 0.0 || selectedStaff == null) {
                        JsfUtil.addErrorMessage("Please fill the Paying Amount and Staff Name");
                        return true;
                    }
                    if (selectedStaff.getCurrentCreditValue() + value > selectedStaff.getCreditLimitQualified()) {
                        JsfUtil.addErrorMessage("No enough Credit.");
                        return true;
                    }
                    multiplePaymentMethodTotalValue += value;
                    break;
                }
                case Staff_Welfare: {
                    double value = cd.getPaymentMethodData().getStaffWelfare().getTotalValue();
                    Staff welfareStaff = cd.getPaymentMethodData().getStaffWelfare().getToStaff();
                    if (value == 0.0 || welfareStaff == null) {
                        JsfUtil.addErrorMessage("Please fill the Paying Amount and Staff Name");
                        return true;
                    }
                    if (Math.abs(welfareStaff.getAnnualWelfareUtilized()) + value > welfareStaff.getAnnualWelfareQualified()) {
                        JsfUtil.addErrorMessage("No enough credit.");
                        return true;
                    }
                    multiplePaymentMethodTotalValue += value;
                    break;
                }
                default:
                    break;
            }
        }

        if (Math.abs(netTotal - multiplePaymentMethodTotalValue) > 1.0) {
            JsfUtil.addErrorMessage("Mismatch in differences of multiple payment method total and bill total");
            return true;
        }
        return false;
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

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
        if (toStaff != null) {
            getPaymentMethodData().getStaffCredit().setToStaff(toStaff);
            getPaymentMethodData().getStaffWelfare().setToStaff(toStaff);
        }
    }

    public Double getPreviewRate() {
        if (stockDto == null) return null;
        return stockDto.getRetailRate();
    }

    public Double getPreviewNetValue() {
        if (stockDto == null || intQty == null) return null;
        double rate = stockDto.getRetailRate() != null ? stockDto.getRetailRate() : 0.0;
        return rate * intQty;
    }

    public StockDtoConverter getStockDtoConverter() {
        return new StockDtoConverter();
    }

    public class StockDtoConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext context, UIComponent component, String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            try {
                Long id = Long.valueOf(value);
                if (stockDto != null && id.equals(stockDto.getId())) {
                    return stockDto;
                }
                if (lastAutocompleteResults != null) {
                    for (StockDTO dto : lastAutocompleteResults) {
                        if (dto != null && id.equals(dto.getId())) {
                            return dto;
                        }
                    }
                }
                StockDTO dto = new StockDTO();
                dto.setId(id);
                return dto;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String getAsString(FacesContext context, UIComponent component, Object value) {
            if (value == null) return "";
            if (value instanceof StockDTO) {
                StockDTO dto = (StockDTO) value;
                return dto.getId() != null ? dto.getId().toString() : "";
            }
            return value.toString();
        }
    }
}
