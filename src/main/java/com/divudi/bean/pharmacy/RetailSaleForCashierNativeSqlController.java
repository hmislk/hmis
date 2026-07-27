/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.ControllerWithMultiplePayments;
import com.divudi.bean.common.ControllerWithPatient;
import com.divudi.bean.common.TokenController;
import com.divudi.service.DiscountSchemeValidationService;
import com.divudi.bean.common.PatientDepositController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BooleanMessage;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.TokenType;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dto.BillItemData;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.data.dto.PrintBillData;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.Token;
import com.divudi.core.entity.clinical.ClinicalFindingValue;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.TokenFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyService;
import com.divudi.service.pharmacy.RetailSaleForCashierNativeSqlService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * Controller for the native-SQL pharmacy "Sale for Cashier" page.
 *
 * Settles through RetailSaleForCashierNativeSqlService, writing a SINGLE PreBill of
 * BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER with no Payment rows.
 * Stock is deducted here; the cashier takes payment later via PharmacyPreSettleController.
 *
 * Derived from RetailSaleNativeSqlController (#20260) with the cashier deltas re-applied:
 * token system, qty helpers, departmentType-filtered autocomplete, cashier bill numbering.
 *
 * Issue: #20261
 */
@Named
@SessionScoped
public class RetailSaleForCashierNativeSqlController implements Serializable, ControllerWithPatient, ControllerWithMultiplePayments {

    private static final Logger LOGGER = Logger.getLogger(RetailSaleForCashierNativeSqlController.class.getName());

    // ---- CDI ----
    @Inject
    private SessionController sessionController;
    @Inject
    private BillBeanController billBean;
    @Inject
    private TokenController tokenController;
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
    private RetailSaleForCashierNativeSqlService nativeSqlService;
    @EJB
    private TokenFacade tokenFacade;
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
    private List<BillItemData> selectedBillItemDataList;

    // ---- Token system (cashier page only) ----
    private Token currentToken;
    private Token token;
    private Department counter;

    @PostConstruct
    public void init() {
        resetAll();
    }

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    public String navigateToPharmacyBillForCashierNativeFromMenu() {
        resetAll();
        billSettlingStarted = false;
        return "/pharmacy/pharmacy_bill_retail_sale_for_cashier_native?faces-redirect=true";
    }

    /**
     * Navigation target of the 4-window switch buttons (Sale 1..4).
     *
     * Unlike {@link #navigateToPharmacyBillForCashierNativeFromMenu()} this deliberately
     * does <b>not</b> reset the window: a cart parked in another window must survive a
     * switch. It only clears the settle-in-progress latch so a window abandoned mid-settle
     * is usable again. Mirrors PharmacySaleController.pharmacyRetailSale(). Issue #22443.
     */
    public String switchToThisSaleWindow() {
        billSettlingStarted = false;
        return "/pharmacy/pharmacy_bill_retail_sale_for_cashier_native?faces-redirect=true";
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
        return "/pharmacy/pharmacy_bill_retail_sale_for_cashier_native?faces-redirect=true";
    }

    // -----------------------------------------------------------------------
    // Settle
    // -----------------------------------------------------------------------

    public String settleBillWithPay() {
        if (billSettlingStarted) {
            return null;
        }
        billSettlingStarted = true;
        try {

            // Session guard, ported from PharmacySaleForCashierController :3582-3586 (also :2958-2962).
            // Without it buildPreBill() would dereference getLoggedUser().getDepartment() and NPE to a
            // JSF error page with the latch left true, permanently disabling settle for the session.
            if (sessionController.getLoggedUser() == null) {
                JsfUtil.addErrorMessage("Session expired. Please log in again.");
                return null;
            }

            if (paymentMethod == null) {
                JsfUtil.addErrorMessage("Please select Payment Method");
                return null;
            }

            if (billItemDataList == null || billItemDataList.isEmpty()) {
                JsfUtil.addErrorMessage("Please add items to the bill.");
                return null;
            }

            // Zero-quantity gate, ported from PharmacySaleForCashierController :3591-3604 (message
            // spelling is legacy's). recalculateRow() can leave a row at qty 0 while the money fields
            // still hold the previous quantity's values, so without this gate buildPreBill() would
            // charge for a line the service dispenses no stock for.
            for (BillItemData bid : billItemDataList) {
                if (bid.getQty() <= 0.0) {
                    JsfUtil.addErrorMessage("Some BillItem Quntity is Zero or less than Zero");
                    return null;
                }
            }

            BooleanMessage discountValidation = discountSchemeValidationService.validateDiscountScheme(
                    paymentMethod, paymentScheme, getPaymentMethodData());
            if (!discountValidation.isFlag()) {
                JsfUtil.addErrorMessage(discountValidation.getMessage());
                return null;
            }

            // Validate department type consistency before settlement.
            // Mirrors PharmacySaleForCashierController.settlePreBillAndNavigateToPrint() :3622-3632;
            // the entity getter Item.getDepartmentType() defaults a null column to Pharmacy, so the
            // item is resolved through itemFacade rather than a JPQL projection to keep that default.
            if (getPreBill().getDepartmentType() != null) {
                for (BillItemData bid : billItemDataList) {
                    if (bid.getItemId() == null) {
                        continue;
                    }
                    Item lineItem = itemFacade.find(bid.getItemId());
                    if (lineItem != null && lineItem.getDepartmentType() != null
                            && !lineItem.getDepartmentType().equals(getPreBill().getDepartmentType())) {
                        JsfUtil.addErrorMessage("Inconsistent department types detected. All items must belong to the same department type.");
                        return null;
                    }
                }
            }

            // Pharmacy Sale Validation - Patient and Patient Details.
            // Keys and messages are those of the legacy cashier path
            // (PharmacySaleForCashierController :3635-3746), not the pay-now Sale page.
            boolean patientRequired = configOptionApplicationController.getBooleanValueByKey(
                    "Patient is required in Pharmacy Retail Sale", false);

            if (patientRequired) {
                if (getPatient() == null || getPatient().getPerson() == null) {
                    JsfUtil.addErrorMessage("Patient is required.");
                    return null;
                }
            }

            // Only validate patient details if patient is required OR if patient exists
            boolean hasPatient = getPatient() != null && getPatient().getPerson() != null;

            if (hasPatient || patientRequired) {
                // Patient Name validation
                if (configOptionApplicationController.getBooleanValueByKey(
                        "Patient Name is required in Pharmacy Retail Sale", false)) {
                    if (getPatient() == null || getPatient().getPerson() == null
                            || getPatient().getPerson().getName() == null
                            || getPatient().getPerson().getName().trim().isEmpty()) {
                        JsfUtil.addErrorMessage("Patient name is required.");
                        return null;
                    }
                }

                // Patient Phone validation
                if (configOptionApplicationController.getBooleanValueByKey(
                        "Patient Phone is required in Pharmacy Retail Sale", false)) {
                    if (getPatient() == null || getPatient().getPerson() == null) {
                        JsfUtil.addErrorMessage("Patient is required.");
                        return null;
                    }
                    // Check both phone and mobile - at least one should be present
                    boolean hasPhone = getPatient().getPerson().getPhone() != null
                            && !getPatient().getPerson().getPhone().trim().isEmpty();
                    boolean hasMobile = getPatient().getPerson().getMobile() != null
                            && !getPatient().getPerson().getMobile().trim().isEmpty();

                    if (!hasPhone && !hasMobile) {
                        JsfUtil.addErrorMessage("Patient phone number is required.");
                        return null;
                    }
                }

                // Patient Gender validation
                if (configOptionApplicationController.getBooleanValueByKey(
                        "Patient Gender is required in Pharmacy Retail Sale", false)) {
                    if (getPatient() == null || getPatient().getPerson() == null
                            || getPatient().getPerson().getSex() == null) {
                        JsfUtil.addErrorMessage("Patient gender is required.");
                        return null;
                    }
                }

                // Patient Address validation
                if (configOptionApplicationController.getBooleanValueByKey(
                        "Patient Address is required in Pharmacy Retail Sale", false)) {
                    if (getPatient() == null || getPatient().getPerson() == null
                            || getPatient().getPerson().getAddress() == null
                            || getPatient().getPerson().getAddress().trim().isEmpty()) {
                        JsfUtil.addErrorMessage("Patient address is required.");
                        return null;
                    }
                }

                // Patient Area validation
                if (configOptionApplicationController.getBooleanValueByKey(
                        "Patient Area is required in Pharmacy Retail Sale", false)) {
                    if (getPatient() == null || getPatient().getPerson() == null
                            || getPatient().getPerson().getArea() == null) {
                        JsfUtil.addErrorMessage("Patient area is required.");
                        return null;
                    }
                }
            }

            if (getPatient().isBlacklisted()) {
                JsfUtil.addErrorMessage("This patient is blacklisted from the system. Can't Bill.");
                return null;
            }

            // Referring Doctor validation
            if (configOptionApplicationController.getBooleanValueByKey(
                    "Referring Doctor is required in Pharmacy Retail Sale", false)) {
                if (getPreBill() == null || getPreBill().getReferredBy() == null) {
                    JsfUtil.addErrorMessage("Referring doctor is required.");
                    return null;
                }
            }

            // Defaults to TRUE - every deployment enforces this unless it is explicitly turned off.
            if (configOptionApplicationController.getBooleanValueByKey(
                    "Patient Phone number is mandotary in sale for cashier", true)) {
                // NOTE: legacy leaves the latch untouched on these three branches, but legacy's
                // settlePreBillAndNavigateToPrint() never sets billSettlingStarted at all (only its
                // separate settleBillWithPay() at :4505 does), so there it is harmless. Here the
                // enclosing try/finally always clears the latch on the way out, so returning from
                // any of these branches cannot leave settling disabled for the rest of the session.
                if (getPatient() != null && getPatient().getPerson() != null) {
                    if (getPatient().getPatientPhoneNumber() == null && getPatient().getPatientMobileNumber() == null) {
                        JsfUtil.addErrorMessage("Please enter phone number of the patient");
                        return null;
                    } else if (getPatient().getId() == null) {
                        if (getPatient().getPatientPhoneNumber() != null
                                && !(String.valueOf(getPatient().getPatientPhoneNumber()).length() >= 9)) {
                            JsfUtil.addErrorMessage("Please enter valid phone number with more than or equal 10 digits of the patient");
                            return null;
                        } else if (getPatient().getPatientMobileNumber() != null
                                && !(String.valueOf(getPatient().getPatientMobileNumber()).length() >= 9)) {
                            JsfUtil.addErrorMessage("Please enter valid mobile number with more than or equal 10 digits of the patient");
                            return null;
                        }
                    }
                } else if (patientRequired) {
                    JsfUtil.addErrorMessage("Patient is required.");
                    return null;
                }
            }

            // Duplicate bill item detection - prevent double stock deduction (Closes #18874).
            // Ported from PharmacySaleForCashierController :3749-3772; the native cart holds
            // BillItemData rows instead of BillItem entities, so the stock id is read straight
            // off the row rather than through PharmaceuticalBillItem.
            // Check 1: same row object appearing twice (rapid Add button click)
            Set<Integer> seenIdentities = new HashSet<>();
            for (BillItemData bid : billItemDataList) {
                if (!seenIdentities.add(System.identityHashCode(bid))) {
                    JsfUtil.addErrorMessage("Duplicate item detected. Please remove duplicate items and try again.");
                    return null;
                }
            }
            // Check 2: different rows pointing at the same Stock batch
            Set<Long> seenStockIds = new HashSet<>();
            for (BillItemData bid : billItemDataList) {
                if (bid.getStockId() != null && !seenStockIds.add(bid.getStockId())) {
                    JsfUtil.addErrorMessage("Duplicate item batch detected: "
                            + bid.getItemName() + ". Please remove duplicate items and try again.");
                    return null;
                }
            }

            // Ensure discounts reflect current payment scheme before building the bill
            recalculateDiscountsForAll();
            calTotal();

            // Back-fill the credit counterparties from the entered payment components before the
            // bill is built, so a Credit / Staff bill is not persisted without one.
            syncStaffSelectionFromPaymentDetails(paymentMethod);
            syncCreditInstitutionFromPaymentDetails(paymentMethod);

            // Patient-required gate + conditional patient save, ported from
            // PharmacySaleForCashierController :3790-3815. The earlier
            // "Patient is required in Pharmacy Retail Sale" null test can never fire because
            // getPatient() and Patient.getPerson() both self-instantiate (Patient.java:441-444);
            // legacy's real enforcement is this name-emptiness test. Equally important, an
            // anonymous sale must NOT persist an empty Person + Patient row - legacy attaches
            // null to the bill in that case.
            Patient pt = null;
            if (getPatient() != null && getPatient().getPerson() != null) {
                String name = getPatient().getPerson().getName();
                boolean hasValidName = name != null && !name.trim().isEmpty();
                if (patientRequired) {
                    if (!hasValidName) {
                        JsfUtil.addErrorMessage("Please Select a Patient");
                        return null;
                    } else {
                        pt = savePatient();
                        // savePatient() logs and returns null if the write fails. Settling
                        // anyway would produce a patient-less bill on a department that
                        // requires one, so abort instead.
                        if (pt == null) {
                            JsfUtil.addErrorMessage("Could not save the patient. Please try again.");
                            return null;
                        }
                    }
                } else {
                    if (hasValidName) {
                        pt = savePatient();
                    }
                }
            } else if (patientRequired) {
                JsfUtil.addErrorMessage("Please Select a Patient");
                return null;
            }

            PreBill preBillEntity = buildPreBill(pt);

            // Stamps the single-method payment reference fields on the bill itself: cheque /
            // slip / card / online-settlement / ewallet numbers, dates and banks, plus the
            // creditBill flag. BillBeanController.setPaymentMethodData (:2916-2951) has no
            // MultiplePaymentMethods branch, so a multiple-payment breakdown is deliberately
            // NOT written here - the cashier collects and records the money later. Mirrors
            // legacy savePreBillFinallyForRetailSaleForCashier (:2997).
            preBillEntity.setCashPaid(cashPaid);
            billBean.setPaymentMethodData(preBillEntity, paymentMethod, getPaymentMethodData());

            // Stamp dept/institution IDs on each item (needed by the native service for
            // StockHistory aggregates).
            long deptId = sessionController.getLoggedUser().getDepartment().getId();
            long instId = sessionController.getLoggedUser().getDepartment().getInstitution().getId();
            for (BillItemData bid : billItemDataList) {
                bid.setDepartmentId(deptId);
                bid.setInstitutionId(instId);
            }

            // Only the settle() call is allowed to report "failed". Nothing has been
            // committed if it throws, so the cart is left intact and a retry is safe.
            try {
                nativeSqlService.settle(preBillEntity, billItemDataList);
            } catch (RuntimeException e) {
                LOGGER.log(Level.SEVERE, "Native sale-for-cashier settle failed", e);
                JsfUtil.addErrorMessage("Failed to settle bill: " + e.getMessage());
                return null;
            }

            // Past this point settle() has committed: the bill exists and the stock is
            // already deducted. Anything failing from here must NOT be reported as a failed
            // settle — the operator would press Settle again and deduct the stock twice.
            try {
                // settle() is a @Stateless REQUIRED boundary, so the bill and the stock
                // deduction are already committed here. A token failure must not make a
                // completed sale report as failed and skip the printout.
                try {
                    settleTokenIfEnabled(preBillEntity);
                    // Legacy runs this OUTSIDE the "Enable token system in sale for cashier"
                    // check (:3869-3872), so a token created by any other route is still
                    // attached to the settled bill.
                    if (getCurrentToken() != null) {
                        getCurrentToken().setBill(preBillEntity);
                        tokenFacade.edit(getCurrentToken());
                    }
                } catch (RuntimeException tokenEx) {
                    LOGGER.log(Level.WARNING, "Token handling failed after cashier settle", tokenEx);
                }

                buildPrintBill(preBillEntity);
                // Legacy calls resetAll() here (:3874) so nothing from the settled bill leaks
                // into the next one. resetAll() also clears the just-built printout, which the
                // legacy controller keeps in a separate printBill field, so it is restored.
                PrintBillData settledPrintBill = printBill;
                List<BillItemData> settledPrintBillItems = printBillItems;
                resetAll();
                printBill = settledPrintBill;
                printBillItems = settledPrintBillItems;
                billPreview = true;
                JsfUtil.addSuccessMessage("Bill settled successfully.");
            } catch (RuntimeException e) {
                LOGGER.log(Level.SEVERE,
                        "Cashier bill was settled but post-settle processing failed", e);
                // Clear the cart so the bill cannot be settled a second time, and say
                // plainly that the sale itself went through.
                resetAll();
                billPreview = false;
                JsfUtil.addErrorMessage("Bill " + preBillEntity.getDeptId()
                        + " WAS settled and stock was deducted, but the printout could not be"
                        + " prepared. Do not settle again — reprint it from Search Sale for"
                        + " Cashier Bills.");
            }
            return null;
        } finally {
            billSettlingStarted = false;
        }
    }

    /**
     * Persists the entered patient, mirroring PharmacySaleForCashierController.savePatient()
     * (:1440-1462): a patient with no name is never written and null is returned, so an
     * anonymous cashier sale leaves no empty Person/Patient row behind and the bill carries
     * no patient at all.
     *
     * @return the persisted patient, or null when there was nothing worth saving.
     */
    private Patient savePatient() {
        try {
            if (patient == null || patient.getPerson() == null
                    || patient.getPerson().getName() == null
                    || patient.getPerson().getName().trim().isEmpty()) {
                return null;
            }
            // Round-trips the transient phone/mobile strings so the Long patientPhoneNumber /
            // patientMobileNumber columns are kept in step with Person.phone / Person.mobile.
            patient.setMobileNumberStringTransient(patient.getMobileNumberStringTransient());
            patient.setPhoneNumberStringTransient(patient.getPhoneNumberStringTransient());
            if (patient.getId() == null) {
                patient.setCreater(sessionController.getLoggedUser());
                patient.setCreatedAt(new Date());
                patient.getPerson().setCreater(sessionController.getLoggedUser());
                patient.getPerson().setCreatedAt(new Date());
                if (patient.getPerson().getId() == null) {
                    personFacade.create(patient.getPerson());
                }
                patientFacade.create(patient);
            } else {
                // Deliberate divergence from legacy savePatient() (:1440-1462), which only ever
                // creates. This page's "Patient Phone number is mandotary in sale for cashier"
                // gate defaults to on, so operators are expected to enter or correct an existing
                // patient's phone number at the counter, and that correction must persist.
                personFacade.edit(patient.getPerson());
                patientFacade.edit(patient);
            }
            return patient;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not save patient", e);
            return null;
        }
    }

    /**
     * Ported verbatim from PharmacySaleForCashierController :3377-3393. Back-fills the bill's
     * {@code toStaff} from the staff picked inside the Staff / Staff Welfare payment component
     * when the page has not set it directly, so a staff-credit bill always names its debtor.
     */
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

    /**
     * Credit counterpart of {@link #syncStaffSelectionFromPaymentDetails(PaymentMethod)}. The
     * credit company is only ever entered inside the Credit payment component; without this the
     * bill would persist with {@code creditBill = true} but no {@code creditCompany} /
     * {@code toInstitution}, and so would never surface in credit-company debtor reporting.
     * Mirrors how legacy stamps the same pair on its settled bill
     * (PharmacySaleForCashierController :4453-4454).
     */
    private void syncCreditInstitutionFromPaymentDetails(PaymentMethod method) {
        if (method != PaymentMethod.Credit) {
            return;
        }
        if (paymentMethodData == null || toInstitution != null) {
            return;
        }
        if (paymentMethodData.getCredit() != null && paymentMethodData.getCredit().getInstitution() != null) {
            setToInstitution(paymentMethodData.getCredit().getInstitution());
        }
    }

    private PreBill buildPreBill(Patient billPatient) {
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
        pb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        pb.setDepartment(sessionController.getLoggedUser().getDepartment());
        pb.setInstitution(sessionController.getLoggedUser().getDepartment().getInstitution());
        pb.setPatient(billPatient);
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

        // Legacy stamps both counterparties unconditionally
        // (PharmacySaleForCashierController :2972-2973). creditCompany is stamped alongside
        // toInstitution so a Credit bill is reportable as a debtor: BillBeanController
        // .setPaymentMethodData only flips creditBill = true, it never records who owes.
        pb.setToStaff(toStaff);
        pb.setToInstitution(toInstitution);
        if (paymentMethod == PaymentMethod.Credit && toInstitution != null) {
            pb.setCreditCompany(toInstitution);
        }

        if (getPreBill().getReferredBy() != null) {
            pb.setReferredBy(getPreBill().getReferredBy());
        }
        // Carried over from the cart so the cashier-side settle page can keep filtering
        // by department type (see completeAvailableStockOptimizedDtoFilteredByDepartmentType).
        pb.setDepartmentType(getPreBill().getDepartmentType());

        // Bill numbering. Legacy runs a SEPARATE institution generator for insId
        // (:3016-3029); the two numbers only collapse under the Dept+Ins strategy.
        String deptId = generateDepartmentBillNumber();
        String insId = generateInstitutionBillNumber(pb, deptId);
        pb.setInsId(insId);
        pb.setDeptId(deptId);
        pb.setInvoiceNumber(billNumberGenerator.fetchPaymentSchemeCount(
                pb.getPaymentScheme(), pb.getBillType(), pb.getInstitution()));
        return pb;
    }

    /** Ported verbatim from PharmacySaleForCashierController :3000-3013. */
    private String generateDepartmentBillNumber() {
        if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            return billNumberGenerator.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            return billNumberGenerator.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            return billNumberGenerator.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        }
        // Use existing method for backward compatibility
        return billNumberGenerator.departmentBillNumberGenerator(
                sessionController.getDepartment(), BillType.PharmacyPre,
                BillClassType.PreBill, BillNumberSuffix.SALE);
    }

    /** Ported verbatim from PharmacySaleForCashierController :3015-3028. */
    private String generateInstitutionBillNumber(PreBill pb, String deptId) {
        if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            return billNumberGenerator.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        }
        if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            return deptId; // Use same number as department to avoid consuming counter twice
        }
        // Use existing method for backward compatibility
        return billNumberGenerator.institutionBillNumberGenerator(
                pb.getInstitution(), pb.getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
    }

    private void buildPrintBill(Bill bill) {
        PrintBillData pbd = new PrintBillData();

        // Department / institution header
        Department dept = sessionController.getLoggedUser().getDepartment();
        pbd.setDepartmentName(dept.getName());
        pbd.setDepartmentPrintingName(dept.getPrintingName() != null ? dept.getPrintingName() : dept.getName());
        pbd.setDepartmentTelephone1(dept.getTelephone1());
        pbd.setDepartmentTelephone2(dept.getTelephone2());
        pbd.setDepartmentFax(dept.getFax());
        pbd.setDepartmentAddress(dept.getAddress());
        if (dept.getSite() != null) {
            pbd.setDepartmentSiteName(dept.getSite().getName());
        }
        if (dept.getInstitution() != null) {
            pbd.setInstitutionName(dept.getInstitution().getName());
            pbd.setInstitutionAddress(dept.getInstitution().getAddress());
            pbd.setInstitutionEmail(dept.getInstitution().getEmail());
            pbd.setInstitutionWeb(dept.getInstitution().getWeb());
        }

        // Bill identity
        pbd.setBillNo(bill.getDeptId());
        pbd.setBillIdStr(bill.getIdStr());
        pbd.setCancelled(bill.isCancelled());
        pbd.setInvoiceNumber(bill.getInvoiceNumber());
        pbd.setCreatedAt(bill.getCreatedAt());
        if (bill.getCreater() != null) {
            pbd.setCreatorName(bill.getCreater().getName());
            pbd.setCreatorCode(bill.getCreater().getCode());
        }
        // Token number for the token printouts. settleBillWithPay runs settle() ->
        // settleTokenIfEnabled() -> buildPrintBill(), so the token (if the token system is
        // enabled for this department) is already created and held in memory here - no Bill
        // reload needed. Stays null when the token system is off, and the composites then
        // print nothing where the token circle would be.
        Token printToken = getToken() != null ? getToken() : getCurrentToken();
        if (printToken != null) {
            pbd.setTokenNumber(printToken.getTokenNumber());
        }

        // Patient. Read off the settled bill rather than the working field, so an anonymous
        // sale (no Patient persisted, bill.patient == null) prints no patient block instead of
        // an empty one.
        Patient billPatient = bill.getPatient();
        if (billPatient != null && billPatient.getPerson() != null) {
            pbd.setPatientName(billPatient.getPerson().getNameWithTitle());
            pbd.setPatientPhone(billPatient.getPerson().getPhone());
            pbd.setPatientPhn(billPatient.getPhn());
            // Rendered as "age / sex" on the cashier bill formats, matching the legacy
            // composites' "#{...ageAsShortString} / #{...sex.label}" pair.
            String age = billPatient.getPerson().getAgeAsShortString();
            String sex = billPatient.getPerson().getSex() != null
                    ? billPatient.getPerson().getSex().getLabel() : null;
            boolean hasAge = age != null && !age.trim().isEmpty();
            boolean hasSex = sex != null && !sex.trim().isEmpty();
            if (hasAge && hasSex) {
                pbd.setPatientAgeSex(age + " / " + sex);
            } else if (hasAge) {
                pbd.setPatientAgeSex(age);
            } else if (hasSex) {
                pbd.setPatientAgeSex(sex);
            }
            // Left null when neither is known, so the print shows an empty Age/Sex cell
            // rather than a bare " / ".
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
        // Read the same field the bill was stamped from, so the printout can never name a
        // different credit company than the one persisted on the bill.
        if (paymentMethod == PaymentMethod.Credit && bill.getToInstitution() != null) {
            pbd.setToInstitutionName(bill.getToInstitution().getName());
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
        double printedBalance = tendered - netTot;
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            tendered = 0.0;
            for (PrintBillData.PaymentLine line : pbd.getPayments()) {
                tendered += line.getValue();
            }
            // No payment is actually taken on this cashier page, so the entered
            // component split is not validated against the net total at settle
            // time (unlike the pay-now retail sale page). Clamp a near-zero
            // printed balance to zero so an already-settled bill doesn't show a
            // stray residual from rounding.
            printedBalance = tendered - netTot;
            if (Math.abs(printedBalance) <= 1.0) {
                printedBalance = 0.0;
            }
        }
        pbd.setCashPaid(tendered);
        pbd.setBalance(printedBalance);

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
    // Token system (cashier page)
    // -----------------------------------------------------------------------

    /**
     * Cashier-page token integration, gated on config
     * "Enable token system in sale for cashier". Ported from
     * PharmacySaleForCashierController.settlePreBillAndNavigateToPrint().
     */
    private void settleTokenIfEnabled(Bill settledBill) {
        if (!configOptionController.getBooleanValueByKey("Enable token system in sale for cashier", false)) {
            return;
        }
        if (patient == null) {
            return;
        }
        Token existing = tokenController.findPharmacyTokens(settledBill);
        if (existing == null) {
            Token saleForCashierToken = tokenController.findPharmacyTokenSaleForCashier(
                    settledBill, TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER);
            if (saleForCashierToken == null) {
                settlePharmacyToken(TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER, settledBill);
            } else {
                // Adopt the token that already exists for this bill, otherwise token and
                // currentToken stay null and markInprogress() below is a no-op — the
                // existing token would never be attached to the settled bill.
                setToken(saleForCashierToken);
                setCurrentToken(saleForCashierToken);
            }
            markInprogress(settledBill);
        } else {
            markToken(settledBill);
        }
    }

    /**
     * Ported from PharmacySaleForCashierController.settlePharmacyToken(TokenType), with
     * the settled bill passed in instead of read from a getPreBill() field this controller
     * uses differently (here getPreBill() is the in-progress cart header, not the saved
     * bill). The patient is already persisted by savePatient() at this point, so
     * the legacy savePatient() call is not repeated.
     */
    public void settlePharmacyToken(TokenType tokenType, Bill settledBill) {
        if (patient == null || patient.getId() == null
                || patient.getPerson() == null
                || patient.getPerson().getName() == null
                || patient.getPerson().getName().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a patient");
            return;
        }
        currentToken = new Token();
        currentToken.setTokenType(tokenType);
        currentToken.setDepartment(sessionController.getDepartment());
        currentToken.setFromDepartment(sessionController.getDepartment());
        currentToken.setPatient(patient);
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
        if (currentToken.getToDepartment() == null) {
            currentToken.setToDepartment(sessionController.getDepartment());
        }
        if (currentToken.getToInstitution() == null) {
            currentToken.setToInstitution(sessionController.getInstitution());
        }
        tokenFacade.create(currentToken);
        currentToken.setTokenNumber(billNumberGenerator.generateDailyTokenNumber(
                currentToken.getFromDepartment(), null, null, tokenType));
        currentToken.setCounter(counter);
        currentToken.setTokenDate(new Date());
        currentToken.setTokenAt(new Date());
        currentToken.setBill(settledBill);
        tokenFacade.edit(currentToken);
        setToken(currentToken);
    }

    /** Ported from PharmacySaleForCashierController.markInprogress(). */
    public void markInprogress(Bill settledBill) {
        Token t = getToken();
        if (t == null) {
            return;
        }
        t.setBill(settledBill);
        t.setCalled(false);
        t.setCalledAt(null);
        t.setInProgress(true);
        t.setCompleted(false);
        tokenController.save(t);
    }

    /** Ported from PharmacySaleForCashierController.markToken(). */
    public void markToken(Bill settledBill) {
        Token t = getToken();
        if (t == null) {
            return;
        }
        t.setBill(settledBill);
        t.setCalled(true);
        t.setCalledAt(new Date());
        t.setInProgress(false);
        t.setCompleted(false);
        tokenController.save(t);
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

        // Issue #13260: when "Add quantity from multiple batches in pharmacy retail billing" is on,
        // a requested quantity larger than the selected batch's stock is filled from the next
        // available batches of the same item (FEFO — earliest expiry first), creating one bill line
        // per batch consumed. When off, the requested qty must fit within the selected batch.
        boolean multipleBatches = configOptionApplicationController.getBooleanValueByKey(
                "Add quantity from multiple batches in pharmacy retail billing", false);

        if (!multipleBatches) {
            if (stockDto.getStockQty() != null && intQty > stockDto.getStockQty()) {
                JsfUtil.addErrorMessage("No sufficient stock available.");
                return;
            }
        }

        // Allergy check is per item, not per batch — do it once up front.
        boolean shouldCheckAllergies = configOptionApplicationController.getBooleanValueByKey(
                "Check for Allergies during Dispensing", false)
                && patient != null && patient.getId() != null;
        try {
            if (shouldCheckAllergies) {
                Item itemRef = itemFacade.find(stockDto.getItemId());
                if (allergyListOfPatient == null) {
                    allergyListOfPatient = pharmacyService.getAllergyListForPatient(patient);
                }
                String allergyMsg = pharmacyService.getAllergyMessageForItem(patient, itemRef, allergyListOfPatient);
                if (allergyMsg != null && !allergyMsg.isEmpty()) {
                    JsfUtil.addErrorMessage(allergyMsg);
                    return;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Allergy check failed for item {0}: {1}",
                    new Object[]{stockDto.getItemId(), e.getMessage()});
            if (shouldCheckAllergies) {
                JsfUtil.addErrorMessage("Could not complete allergy check. Please try again.");
                return;
            }
        }

        // Department type validation, ported from PharmacySaleForCashierController :2384-2425.
        // Runs on EVERY add, including the first item of an empty cart - at that point the
        // autocomplete is still unfiltered because the bill's department type is not yet fixed.
        Item selectedItem = itemFacade.find(stockDto.getItemId());

        // Null-check for selected item (prevents NPE if item not found in database)
        if (selectedItem == null) {
            JsfUtil.addErrorMessage("Selected item not found. Please try again.");
            return;
        }

        // Note: Item.getDepartmentType() defaults a null column to DepartmentType.Pharmacy only
        // when the instance is a PharmaceuticalItem; for any other Item subtype a null column
        // is returned as null (see Item.java getDepartmentType()).
        DepartmentType itemDepartmentType = selectedItem.getDepartmentType();
        if (itemDepartmentType == null) {
            itemDepartmentType = DepartmentType.Pharmacy; // Defensive fallback (should never execute)
        }

        List<DepartmentType> allowedTypes = sessionController.getAvailableDepartmentTypesForPharmacyTransactions();
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            JsfUtil.addErrorMessage("No department types are configured for pharmacy transactions.");
            return;
        }

        if (!allowedTypes.contains(itemDepartmentType)) {
            JsfUtil.addErrorMessage("Items of type " + itemDepartmentType.getLabel()
                    + " are not allowed for pharmacy transactions.");
            return;
        }

        // If the bill already has a department type set, validate the item matches it
        if (getPreBill().getDepartmentType() != null
                && !itemDepartmentType.equals(getPreBill().getDepartmentType())) {
            JsfUtil.addErrorMessage("Cannot add items from different department types. "
                    + "Bill is set for " + getPreBill().getDepartmentType().getLabel()
                    + " items, but you are trying to add a " + itemDepartmentType.getLabel() + " item.");
            return;
        }

        double requestedQty = intQty.doubleValue();
        double remainingQty = requestedQty;

        if (!multipleBatches) {
            // Single-batch behaviour (unchanged): block if already added, then add the full qty.
            if (isStockAlreadyOnBill(selectedStockId)) {
                JsfUtil.addErrorMessage("This batch is already added to the bill. Edit the quantity instead.");
                return;
            }
            if (!addBillItemLineForStock(stockDto, requestedQty)) {
                return;
            }
            calTotal();
            clearBillItem();
            return;
        }

        // Multi-batch FEFO fill: merge the user-selected batch with additional batches and
        // sort all candidates by expiry before allocating, so earlier-expiring stock is always
        // dispensed first regardless of which batch the user picked.
        double addedQty = 0.0;
        boolean lineCreationFailed = false;

        List<StockDTO> candidates = new ArrayList<>();
        candidates.add(stockDto);
        candidates.addAll(findNextAvailableStockDtos(stockDto.getItemId(), selectedStockId));
        candidates.sort(Comparator.comparing(
                StockDTO::getDateOfExpire,
                Comparator.nullsLast(Date::compareTo)));

        for (StockDTO next : candidates) {
            if (remainingQty <= 0) {
                break;
            }
            if (isStockAlreadyOnBill(next.getId())) {
                continue;
            }
            double available = next.getStockQty() != null ? next.getStockQty() : 0.0;
            double take = Math.min(remainingQty, available);
            if (take <= 0) {
                continue;
            }
            if (!addBillItemLineForStock(next, take)) {
                // Every candidate batch belongs to the same item, so a failure to resolve it
                // would repeat for each remaining batch. addBillItemLineForStock has already
                // shown "Selected item not found"; stop so it is shown exactly once.
                lineCreationFailed = true;
                break;
            }
            addedQty += take;
            remainingQty -= take;
        }

        if (lineCreationFailed) {
            calTotal();
            clearBillItem();
            return;
        }

        if (addedQty <= 0) {
            JsfUtil.addErrorMessage("No sufficient stock available.");
            return;
        }
        if (remainingQty > 0) {
            JsfUtil.addErrorMessage("Only " + String.format("%.0f", addedQty)
                    + " of the requested " + String.format("%.0f", requestedQty)
                    + " is available across all batches.");
        }

        calTotal();
        clearBillItem();
    }

    /** True if a bill line already exists for the given stock id. */
    private boolean isStockAlreadyOnBill(Long stockId) {
        if (billItemDataList == null || stockId == null) {
            return false;
        }
        for (BillItemData existing : billItemDataList) {
            if (stockId.equals(existing.getStockId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Build a single bill line for the given stock/batch at the given quantity and append it to
     * {@link #billItemDataList}. Shared by the single-batch and multi-batch (#13260) paths so the
     * rate, discount and value calculations stay identical regardless of how the batch was chosen.
     *
     * @return false when the item could not be resolved and no line was added.
     */
    private boolean addBillItemLineForStock(StockDTO stk, double qty) {
        // Resolve the item up front and abort on null, matching legacy
        // PharmacySaleForCashierController :2389-2392. Doing it inside the discount try/catch
        // below would swallow the NPE and silently skip the department-type lock.
        Item itemRef = itemFacade.find(stk.getItemId());
        if (itemRef == null) {
            JsfUtil.addErrorMessage("Selected item not found. Please try again.");
            return false;
        }

        double[] batchRates = fetchBatchRates(stk.getItemBatchId());
        double batchRetailRate    = batchRates[0];
        double batchPurchaseRate  = batchRates[1];
        double batchWholesaleRate = batchRates[2];
        Double batchCostRate      = batchRates[3] > 0 ? batchRates[3] : null;

        long ampItemId = resolveAmpItemId(stk.getItemId());

        BillItemData bid = new BillItemData();
        bid.setItemId(stk.getItemId());
        bid.setItemName(stk.getItemName());
        bid.setItemCode(stk.getCode());
        bid.setAmpItemId(ampItemId);
        bid.setStockId(stk.getId());
        bid.setItemBatchId(stk.getItemBatchId());
        bid.setQty(qty);
        bid.setPbiQty(-Math.abs(qty));
        bid.setFreeQty(0.0);
        bid.setRetailRate(stk.getRetailRate() != null ? stk.getRetailRate() : 0.0);
        bid.setPurchaseRate(batchPurchaseRate);
        bid.setWholesaleRate(batchWholesaleRate);
        bid.setCostRate(batchCostRate != null ? batchCostRate : batchPurchaseRate);
        bid.setBatchRetailRate(batchRetailRate);
        bid.setBatchPurchaseRate(batchPurchaseRate);
        bid.setBatchWholesaleRate(batchWholesaleRate);
        bid.setBatchCostRate(batchCostRate);
        bid.setDoe(stk.getDateOfExpire());
        bid.setDescription(stk.getItemName());
        bid.setCreatedAt(new Date());
        bid.setCreaterId(sessionController.getLoggedUser().getId());

        double lineRetailRate = stk.getRetailRate() != null ? stk.getRetailRate() : 0.0;
        double grossValue = lineRetailRate * qty;
        double discountPct = 0.0;
        double discountValue = 0.0;
        try {
            if (Boolean.TRUE.equals(itemRef.isDiscountAllowed())) {
                Double pct = priceMatrixController.getPaymentSchemeDiscountPercent(
                        paymentMethod, paymentScheme, sessionController.getDepartment(), itemRef);
                discountPct = pct != null ? pct : 0.0;
                discountValue = (discountPct / 100.0) * grossValue;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Discount lookup failed for item {0}: {1}",
                    new Object[]{stk.getItemId(), e.getMessage()});
        }
        // The first item added fixes the department type of the whole bill; the autocomplete
        // then only offers matching items. Kept OUTSIDE the discount try/catch so a discount
        // lookup failure can never leave the cart unlocked. Mirrors
        // PharmacySaleForCashierController.addBillItemSingleItem().
        if (getPreBill().getDepartmentType() == null) {
            getPreBill().setDepartmentType(itemRef.getDepartmentType());
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
        return true;
    }

    /**
     * FEFO lookup (#13260): non-expired, in-stock batches of the same item in this department,
     * excluding the already-picked stock, ordered by earliest expiry first. JPQL DTO projection —
     * consistent with the rest of this controller (no JPA entity graph / cascade).
     */
    private List<StockDTO> findNextAvailableStockDtos(Long itemId, Long excludeStockId) {
        if (itemId == null) {
            return new ArrayList<>();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("itemId", itemId);
        params.put("department", sessionController.getDepartment());
        params.put("stockMin", 0.0);
        params.put("excludeStockId", excludeStockId != null ? excludeStockId : -1L);
        params.put("now", CommonFunctions.getCurrentDateTime());

        String jpql = "SELECT NEW com.divudi.core.data.dto.StockDTO("
                + "s.id, s.itemBatch.id, s.itemBatch.item.id, s.itemBatch.item.name, s.itemBatch.item.code, "
                + "s.itemBatch.item.name, s.itemBatch.retailsaleRate, s.stock, s.itemBatch.dateOfExpire) "
                + "FROM Stock s "
                + "WHERE s.itemBatch.item.id = :itemId "
                + "AND s.department = :department "
                + "AND s.stock > :stockMin "
                + "AND s.id <> :excludeStockId "
                + "AND s.itemBatch.dateOfExpire > :now "
                + "ORDER BY s.itemBatch.dateOfExpire";

        List<StockDTO> result = (List<StockDTO>) stockFacade.findLightsByJpql(
                jpql, params, TemporalType.TIMESTAMP);
        return result != null ? result : new ArrayList<>();
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
        recalculateRow(event.getObject());
        calTotal();
    }

    /**
     * Single home of the per-row quantity maths and the qty guards (qty &gt; 0 and
     * qty &lt;= available stock). Called by the row editor and by the four qty helpers,
     * so doubling a quantity can never exceed available stock either.
     */
    private void recalculateRow(BillItemData bid) {
        if (bid == null) {
            return;
        }
        if (bid.getQty() <= 0) {
            // Zero the money fields too. Returning early with the previous quantity's
            // grossValue/netValue/pbiQty still on the row would let calTotal()/buildPreBill()
            // charge for a line that dispenses no stock.
            bid.setQty(0);
            bid.setPbiQty(0);
            bid.setGrossValue(0);
            bid.setDiscountValue(0);
            bid.setNetValue(0);
            bid.setNetRate(bid.getRate());
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
    }

    public void onEditCancel(RowEditEvent<BillItemData> event) {
        calTotal();
    }

    public void removeBillItem(BillItemData bid) {
        if (billItemDataList != null) {
            billItemDataList.remove(bid);
        }
        if (selectedBillItemDataList != null) {
            selectedBillItemDataList.remove(bid);
        }
        clearDepartmentTypeIfCartEmpty();
        calTotal();
    }

    public void removeSelectedBillItems() {
        if (selectedBillItemDataList == null || selectedBillItemDataList.isEmpty()) {
            JsfUtil.addErrorMessage("Please select items to delete");
            return;
        }
        if (billItemDataList != null) {
            billItemDataList.removeAll(selectedBillItemDataList);
        }
        selectedBillItemDataList = new ArrayList<>();
        clearDepartmentTypeIfCartEmpty();
        calTotal();
    }

    /** An empty cart releases the department-type lock so the next item can set it afresh. */
    private void clearDepartmentTypeIfCartEmpty() {
        if (billItemDataList == null || billItemDataList.isEmpty()) {
            getPreBill().setDepartmentType(null);
        }
    }

    // -----------------------------------------------------------------------
    // Quantity helpers (cashier page)
    // -----------------------------------------------------------------------

    public void multiplyQuantityByTwo(BillItemData bid) {
        if (bid == null) {
            return;
        }
        bid.setQty(bid.getQty() * 2);
        recalculateRow(bid);
        calTotal();
    }

    public void divideQuantityByHalf(BillItemData bid) {
        if (bid == null) {
            return;
        }
        bid.setQty(halveQty(bid.getQty()));
        recalculateRow(bid);
        calTotal();
    }

    /**
     * Halves a dispense quantity the way legacy
     * PharmacySaleForCashierController.divideQuantityByHalf() does (:1234):
     * {@code Math.max(1.0, qty / 2.0)}. A plain division would leave 0.5 on an odd
     * quantity — a fractional dispense that the qty column renders with
     * {@code integerOnly="true"}, so the operator would never see it.
     */
    private double halveQty(double qty) {
        return Math.max(1.0, Math.floor(qty / 2.0));
    }

    public void multiplyAllQuantitiesByTwo() {
        if (billItemDataList == null) {
            return;
        }
        for (BillItemData bid : billItemDataList) {
            bid.setQty(bid.getQty() * 2);
            recalculateRow(bid);
        }
        calTotal();
    }

    public void divideAllQuantitiesByHalf() {
        if (billItemDataList == null) {
            return;
        }
        for (BillItemData bid : billItemDataList) {
            bid.setQty(halveQty(bid.getQty()));
            recalculateRow(bid);
        }
        calTotal();
    }

    // -----------------------------------------------------------------------
    // Autocomplete
    // -----------------------------------------------------------------------

    public List<StockDTO> completeAvailableStockOptimizedDto(String qry) {
        return searchAvailableStock(qry, null);
    }

    /**
     * Same autocomplete, restricted to the department type already fixed by the first
     * item on the cart ({@code getPreBill().getDepartmentType()}), so a single cashier
     * bill cannot mix pharmacy and non-pharmacy items. Ported from
     * PharmacySaleForCashierController; only the departmentType predicate is taken —
     * the StockDTO projection stays this controller's.
     */
    public List<StockDTO> completeAvailableStockOptimizedDtoFilteredByDepartmentType(String qry) {
        return searchAvailableStock(qry, getPreBill().getDepartmentType());
    }

    /**
     * @param filterType when non-null, only stock of items with this department type is
     * returned. {@code Item.getDepartmentType()} only defaults a null column to Pharmacy when
     * the instance is a {@code PharmaceuticalItem}; for any other Item subtype with a null
     * column it returns null, which will not match a non-null {@code filterType} here.
     */
    private List<StockDTO> searchAvailableStock(String qry, DepartmentType filterType) {
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
                + "AND i.department = :department ");

        if (filterType != null) {
            if (filterType == DepartmentType.Pharmacy) {
                sql.append("AND (i.itemBatch.item.departmentType = :departmentType "
                        + "OR i.itemBatch.item.departmentType IS NULL) ");
            } else {
                sql.append("AND i.itemBatch.item.departmentType = :departmentType ");
            }
            parameters.put("departmentType", filterType);
        }

        sql.append("AND (i.itemBatch.item.name LIKE :query ");

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
        // Legacy clearBill() :5421-5432 leaves the payment method at Cash and the comment null,
        // so the operator does not have to re-pick a payment method after every settle.
        comment = null;
        cashPaid = 0.0;
        balance = 0.0;
        paymentMethod = PaymentMethod.Cash;
        paymentScheme = null;
        paymentMethodData = null;
        toStaff = null;
        toInstitution = null;
        allergyListOfPatient = null;
        selectedBillItemDataList = null;
        currentToken = null;
        token = null;
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

    public List<BillItemData> getSelectedBillItemDataList() {
        if (selectedBillItemDataList == null) {
            selectedBillItemDataList = new ArrayList<>();
        }
        return selectedBillItemDataList;
    }

    public void setSelectedBillItemDataList(List<BillItemData> selectedBillItemDataList) {
        this.selectedBillItemDataList = selectedBillItemDataList;
    }

    public Token getCurrentToken() {
        return currentToken;
    }

    public void setCurrentToken(Token currentToken) {
        this.currentToken = currentToken;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public Department getCounter() {
        return counter;
    }

    public void setCounter(Department counter) {
        this.counter = counter;
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

    /**
     * Credit counterparty of the bill. buildPreBill() stamps it on the PreBill
     * unconditionally, matching PharmacySaleForCashierController :2973, so the page needs
     * to be able to set it (the legacy controller exposes the same pair at :3395-3401).
     */
    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
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
