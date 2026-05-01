/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.ControllerWithPatient;
import com.divudi.service.DiscountSchemeValidationService;
import com.divudi.bean.common.PatientDepositController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BooleanMessage;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dto.BillItemData;
import com.divudi.core.data.dto.PrintBillData;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.clinical.Prescription;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
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
public class RetailSaleNativeSqlController implements Serializable, ControllerWithPatient {

    private static final Logger LOGGER = Logger.getLogger(RetailSaleNativeSqlController.class.getName());

    // ---- CDI ----
    @Inject
    private SessionController sessionController;
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

        if (configOptionApplicationController.getBooleanValueByKey(
                "Enable blacklist patient management in the system", false)
                && configOptionApplicationController.getBooleanValueByKey(
                        "Enable blacklist patient management for Pharmacy from the system", false)) {
            if (getPatient().isBlacklisted()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("This patient is blacklisted from the system. Can't Bill.");
                return null;
            }
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

        // Save or update the patient record
        savePatientIfNeeded();

        // Build bill header
        Bill bill = buildBillHeader();

        // Stamp dept/institution IDs on each item (needed by native service for StockHistory aggregates)
        long deptId = sessionController.getLoggedUser().getDepartment().getId();
        long instId = sessionController.getLoggedUser().getDepartment().getInstitution().getId();
        for (BillItemData bid : billItemDataList) {
            bid.setDepartmentId(deptId);
            bid.setInstitutionId(instId);
        }

        try {
            nativeSqlService.settle(bill, billItemDataList, paymentMethod, getPaymentMethodData(), paymentScheme);

            buildPrintBill(bill);
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

    private Bill buildBillHeader() {
        Bill b = preBill != null ? preBill : new Bill();

        b.setBillType(BillType.PharmacySale);
        b.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE);

        String billNo = generateBillNumber();
        b.setInsId(billNo);
        b.setDeptId(billNo);

        b.setDepartment(sessionController.getLoggedUser().getDepartment());
        b.setInstitution(sessionController.getLoggedUser().getDepartment().getInstitution());
        b.setPatient(patient);
        b.setFromDepartment(sessionController.getLoggedUser().getDepartment());
        b.setFromInstitution(sessionController.getLoggedUser().getDepartment().getInstitution());
        b.setBillDate(new Date());
        b.setBillTime(new Date());
        b.setCreatedAt(Calendar.getInstance().getTime());
        b.setCreater(sessionController.getLoggedUser());
        b.setComments(comment);
        b.setCashPaid(cashPaid);
        b.setPaymentMethod(paymentMethod);
        b.setPaymentScheme(paymentScheme);

        if (paymentMethod == PaymentMethod.Credit && getPaymentMethodData().getCredit().getInstitution() != null) {
            b.setToInstitution(getPaymentMethodData().getCredit().getInstitution());
            b.setCreditCompany(getPaymentMethodData().getCredit().getInstitution());
        }
        if ((paymentMethod == PaymentMethod.Staff || paymentMethod == PaymentMethod.Staff_Welfare)
                && toStaff != null) {
            b.setToStaff(toStaff);
        }

        if (getPreBill().getReferredBy() != null) {
            b.setReferredBy(getPreBill().getReferredBy());
        }

        double netTot = 0.0;
        for (BillItemData bid : billItemDataList) {
            netTot += Math.abs(bid.getNetValue());
        }
        b.setTotal(netTot);
        b.setNetTotal(netTot);
        b.setGrantTotal(netTot);
        b.setBalance(paymentMethod == PaymentMethod.Credit ? netTot : 0.0);
        b.setPaidAmount(paymentMethod == PaymentMethod.Credit ? 0.0 : netTot);

        return b;
    }

    private String generateBillNumber() {
        if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            return billNumberGenerator.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE);
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            return billNumberGenerator.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE);
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            return billNumberGenerator.institutionBillNumberGeneratorYearlyWithPrefixInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE);
        }
        return billNumberGenerator.departmentBillNumberGeneratorYearly(
                sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE);
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
        if (bill.getCreater() != null && bill.getCreater().getPerson() != null) {
            pbd.setCreatorName(bill.getCreater().getPerson().getName());
        }

        // Patient
        if (patient != null && patient.getPerson() != null) {
            pbd.setPatientName(patient.getPerson().getNameWithTitle());
            pbd.setPatientPhone(patient.getPerson().getPhone());
            pbd.setPatientPhn(patient.getPhn());
        }

        // Payment
        pbd.setPaymentMethodLabel(paymentMethod != null ? paymentMethod.getLabel() : "");
        if (paymentScheme != null) {
            pbd.setPaymentSchemePrintingName(
                    paymentScheme.getPrintingName() != null ? paymentScheme.getPrintingName() : paymentScheme.getName());
        }
        pbd.setComment(comment);

        // Targets for credit/staff/dept bills
        if (toStaff != null && toStaff.getPerson() != null) {
            pbd.setToStaffName(toStaff.getPerson().getNameWithTitle());
        }
        if (paymentMethod == com.divudi.core.data.PaymentMethod.Credit
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
        pbd.setCashPaid(cashPaid);
        pbd.setBalance(cashPaid - netTot);

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
        calTotal();
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
