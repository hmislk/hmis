/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.lab.LabTestHistoryController;
import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.bean.lab.PatientReportController;
import com.divudi.bean.pharmacy.PharmacyPreSettleController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillSummery;
import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.EjbApplication;
import com.divudi.ejb.PharmacyBean;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillComponent;
import com.divudi.core.entity.BillEntry;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.AgentHistoryFacade;
import com.divudi.core.facade.BillComponentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.EmailFacade;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;

import com.divudi.core.data.OptionScope;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.facade.FeeFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.StaffFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.light.common.BillLight;
import com.divudi.service.BillService;
import com.divudi.service.PatientDepositService;
import com.divudi.service.PaymentService;
import com.divudi.service.ProfessionalPaymentService;
import com.divudi.service.StaffService;
import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.PrivilegeInfo;
import javax.annotation.PostConstruct;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import com.divudi.bean.pharmacy.SaleReturnController;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PatientSampleComponantFacade;
import java.util.HashMap;

import org.primefaces.model.LazyDataModel;

/**
 * @author Buddhika
 */
@Named
@SessionScoped
public class OpdBillCancellationController implements Serializable, ControllerWithMultiplePayments {

    /**
     * EJBs
     */
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillItemFacade billItemFacede;
    @EJB
    private BillComponentFacade billCommponentFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    PatientInvestigationFacade patientInvestigationFacade;

    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private EjbApplication ejbApplication;
    @EJB
    private AgentHistoryFacade agentHistoryFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    private StaffService staffBean;
    @EJB
    private WebUserFacade webUserFacade;
    @EJB
    CashTransactionBean cashTransactionBean;
    @EJB
    private EmailFacade emailFacade;
    @EJB
    FeeFacade feeFacade;
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    BillService billService;
    @EJB
    ProfessionalPaymentService professionalPaymentService;
    @EJB
    PaymentService paymentService;
    @EJB
    PatientDepositService patientDepositService;
    /**
     * Controllers
     */
    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private PharmacyPreSettleController pharmacyPreSettleController;
    @Inject
    private OpdPreSettleController opdPreSettleController;
    @Inject
    private PatientInvestigationController patientInvestigationController;
    @Inject
    private BillController billController;
    @Inject
    private BillBeanController billBean;
    @Inject
    private SecurityController securityController;
    @Inject
    NotificationController notificationController;
    @Inject
    ConfigOptionController configOptionController;
    @Inject
    private AuditEventApplicationController auditEventApplicationController;
    @Inject
    PageMetadataRegistry pageMetadataRegistry;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    DrawerController drawerController;

    @Inject
    LabTestHistoryController labTestHistoryController;
    /**
     * Class Variables
     */
    private Bill billForCancel;
    boolean showAllBills;
    private boolean printPreview = false;
    private double refundAmount;
    private Bill bill;
    private BillLight billLight;
    private Long selectedBillId;
    private Bill printingBill;
    private PaymentMethod paymentMethod;
    private List<PaymentMethod> paymentMethods;

    // Storage for original payment details loaded during navigation
    private List<ComponentDetail> originalPaymentDetails;
    private PaymentMethodData originalPaymentMethodData;
    @Temporal(TemporalType.TIME)
    private Date fromDate;
    @Temporal(TemporalType.TIME)
    private Date toDate;
    private String comment;
    private WebUser user;
    private BillType billType;
    private BillClassType billClassType;
    /// /////////////
    private List<Bill> billsToApproveCancellation;
    private List<Bill> billsApproving;
    private List<BillItem> refundingItems;
    private List<BillFee> refundingFees;
    private Bill refundingBill;
    private List<Bill> bills;
    private List<Bill> filteredBill;
    private List<BillEntry> billEntrys;
    private List<Payment> billPayments;
    private List<BillItem> billItems;
    private PaymentMethodData paymentMethodData;
    private List<BillComponent> billComponents;
    private List<BillFee> billFees;
    private List<BillItem> tempbillItems;
    private Institution creditCompany;
    private PatientInvestigation patientInvestigation;
    private Institution institution;
    private Department department;

    private Bill viewingBill;
    private List<Bill> viewingReferanceBills;
    private List<PharmaceuticalBillItem> viewingPharmaceuticalBillItems;
    private List<PatientInvestigation> viewingPatientInvestigations;
    private boolean duplicate;

    private int billItemSize;

    @PostConstruct
    public void init() {
        registerPageMetadata();
    }

    /**
     * Register page metadata for the admin configuration interface
     */
    private void registerPageMetadata() {
        if (pageMetadataRegistry == null) {
            return;
        }

        PageMetadata metadata = new PageMetadata();
        metadata.setPagePath("opd/bill_reprint");
        metadata.setPageName("OPD Bill Reprint");
        metadata.setDescription("Reprint OPD bills with various paper formats, cancel bills, and refund fees");
        metadata.setControllerClass("BillSearch");

        // Configuration Options - Bill Operations
        metadata.addConfigOption(new ConfigOptionInfo(
                "Refund Allow for OPD Bill",
                "Enables the 'To Refund Fees' button for OPD bill refunds",
                "Line 76: Refund Fees button visibility",
                OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
                "Return Allow for OPD Bill",
                "Enables the 'To Return Items' button for OPD bill item returns",
                "Line 85: Return Items button visibility",
                OptionScope.APPLICATION
        ));

        // Configuration Options - Bill Paper Formats
        metadata.addConfigOption(new ConfigOptionInfo(
                "OPD Sale Bill is FiveFiveCustom3",
                "Uses FiveFiveCustom3 format for OPD sale bill printing",
                "Line 204: Custom3 bill format rendering",
                OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
                "OPD Bill Paper Size is FiveFivePaper",
                "Uses 5x5 inch paper with headings for OPD bills",
                "Line 210: FiveFive paper format",
                OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
                "OPD Bill Paper Size is FiveFivePrintedPaper",
                "Uses 5x5 inch pre-printed paper without headings for OPD bills",
                "Line 216: FiveFive printed paper format",
                OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
                "OPD Bill Paper Size is PosPaper",
                "Uses POS (Point of Sale) paper format for OPD bills",
                "Lines 222, 251: POS paper format for bills and refunds",
                OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
                "OPD Bill Paper Size is FiveFivePaperCoustom1",
                "Uses 5x5 inch custom format 1 for OPD bills",
                "Lines 228, 259: Custom 1 format for bills and refunds",
                OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
                "OPD Bill Paper Size is FiveFiveCustom3",
                "Uses 5x5 inch custom format 3 for OPD bills",
                "Lines 234, 275: Custom 3 format for bills and refunds",
                OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
                "OPD Bill Paper Size is 5x8 inch Paper",
                "Uses 5x8 inch paper format for OPD bills",
                "Lines 240, 283: 5x8 paper format for bills and refunds",
                OptionScope.APPLICATION
        ));

        // Privileges
        metadata.addPrivilege(new PrivilegeInfo(
                "Admin",
                "Administrative access to page configuration",
                "Config button visibility"
        ));

        metadata.addPrivilege(new PrivilegeInfo(
                "OpdReprintOriginalBill",
                "Access to reprint original OPD bills",
                "Line 40: 'Print to Original Bill' button"
        ));

        metadata.addPrivilege(new PrivilegeInfo(
                "OpdIndividualCancel",
                "Ability to cancel individual OPD bills",
                "Line 57: 'To Cancel' button"
        ));

        metadata.addPrivilege(new PrivilegeInfo(
                "EditData",
                "Edit bill data including referring doctor information",
                "Line 122: Edit bill button and dialog"
        ));

        metadata.addPrivilege(new PrivilegeInfo(
                "ChangeProfessionalFee",
                "Change the staff member assigned to professional fees",
                "Line 449: Professional fee staff assignment dropdown"
        ));

        // Register the metadata
        pageMetadataRegistry.registerPage(metadata);
    }

    public String navigateToCancelBillView() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Bill is Missing..");
            return "";
        }

        // Load and store original payment details for cancellation
        loadOriginalPaymentDetails();

        printPreview = true;
        duplicate = true;
        return "/opd/bill_cancel?faces-redirect=true";
    }

    public List<Payment> fetchBillPayments(Bill bill) {
        return billService.fetchBillPayments(bill);
    }

    public OpdBillCancellationController() {
    }

    private List<BillItem> billItemList;

    public void makeNull() {
        printPreview = false;
        refundAmount = 0;
        bill = null;
        paymentMethod = null;
        comment = "";
        user = null;
        refundingItems = null;
        bills = null;
        filteredBill = null;
        billEntrys = null;
        billItems = null;
        billComponents = null;
        billFees = null;
        tempbillItems = null;
    }

    public PatientInvestigation getPatientInvestigation() {
        return patientInvestigation;
    }

    public EjbApplication getEjbApplication() {
        return ejbApplication;
    }

    public Bill getPrintingBill() {
        return printingBill;
    }

    public void setPrintingBill(Bill printingBill) {
        this.printingBill = printingBill;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    /**
     * Called when user changes payment method in individual bill cancellation
     * form. If user selects the original payment method, restores original
     * payment details. Otherwise, creates new payment data for the selected
     * method.
     */
    public void onPaymentMethodChange() {
        try {
            System.out.println("onPaymentMethodChange: Method started");
            System.out.println("onPaymentMethodChange: Payment method changed to " + this.paymentMethod);

            // Check if user selected the original payment method - if so, restore original details
            if (billPayments != null && !billPayments.isEmpty()) {
                // Check if selected payment method exists in any of the original payments
                for (Payment originalPayment : billPayments) {
                    // Use determineActualPaymentMethod to handle MultiplePaymentMethods correctly
                    PaymentMethod actualPaymentMethod = determineActualPaymentMethod(originalPayment);
                    if (paymentMethod == actualPaymentMethod) {
                        // User switched to a payment method that was used in original bill
                        paymentMethodData = new PaymentMethodData();
                        initializePaymentDataFromOriginalPayments(billPayments);
                        System.out.println("onPaymentMethodChange: Used billPayments for original payment method: " + paymentMethod
                                + " (detected from: " + originalPayment.getPaymentMethod() + ")");
                        return;
                    }
                }
            } else if (originalPaymentDetails != null && !originalPaymentDetails.isEmpty()) {
                // Check stored original payment details when billPayments is not available
                for (ComponentDetail originalDetail : originalPaymentDetails) {
                    if (paymentMethod == originalDetail.getPaymentMethod()) {
                        // User selected a payment method that was used in original bill
                        paymentMethodData = new PaymentMethodData();
                        initializePaymentMethodData();
                        System.out.println("onPaymentMethodChange: Used stored payment details for original payment method");
                        return;
                    }
                }
            }

            // User selected a different payment method - create new payment data
            paymentMethodData = new PaymentMethodData();

            // Initialize basic payment data based on newly selected payment method
            if (paymentMethod != null && getBill() != null) {
                double netTotal = Math.abs(getBill().getNetTotal());

                switch (paymentMethod) {
                    case Cash:
                        paymentMethodData.getCash().setTotalValue(netTotal);
                        break;
                    case Card:
                        paymentMethodData.getCreditCard().setTotalValue(netTotal);
                        break;
                    case Cheque:
                        paymentMethodData.getCheque().setTotalValue(netTotal);
                        break;
                    case Slip:
                        paymentMethodData.getSlip().setTotalValue(netTotal);
                        break;
                    case ewallet:
                        paymentMethodData.getEwallet().setTotalValue(netTotal);
                        break;
                    case Staff_Welfare:
                        paymentMethodData.getStaffWelfare().setTotalValue(netTotal);
                        // Note: toStaff property may need to be set separately in UI
                        break;
                    case Staff:
                    case OnCall:
                        paymentMethodData.getStaffCredit().setTotalValue(netTotal);
                        // Note: toStaff property may need to be set separately in UI
                        break;
                    case Credit:
                        paymentMethodData.getCredit().setTotalValue(netTotal);
                        // Note: creditCompany property may need to be set separately in UI
                        break;
                    case PatientDeposit:
                        paymentMethodData.getPatient_deposit().setTotalValue(netTotal);
                        if (getBill().getPatient() != null) {
                            paymentMethodData.getPatient_deposit().setPatient(getBill().getPatient());
                        }
                        break;
                    case MultiplePaymentMethods:
                        // For multiple payments, clear the component details
                        paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().clear();
                        break;
                    default:
                        // For other payment methods, just initialize with net total
                        break;
                }
                System.out.println("onPaymentMethodChange: Created new payment data with net total for " + paymentMethod);
            }
            System.out.println("onPaymentMethodChange: Method completed successfully - payment method is now: " + this.paymentMethod);
        } catch (Exception e) {
            System.err.println("onPaymentMethodChange: ERROR - " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Temporary test method to verify AJAX is working
    public void testAjaxMethod() {
        System.out.println("TEST: AJAX method called successfully! Payment method is: " + this.paymentMethod);
    }

    public void recreateModel() {
        refundAmount = 0.0;
        billFees = null;
        refundingItems = null;
        billComponents = null;
        billItems = null;
        bills = null;
        printPreview = false;
        tempbillItems = null;
        comment = null;
    }

    private CancelledBill createOpdCancelBill(Bill originalBill) {
        CancelledBill cb = new CancelledBill();
        if (originalBill == null) {
            return null;
        }

        cb.copy(originalBill);
        cb.copyValue(originalBill);
        cb.invertAndAssignValuesFromOtherBill(originalBill);
        cb.setBillType(BillType.OpdBill);
        cb.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_CANCELLATION);

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.OPD_BILL_CANCELLATION);

        cb.setDeptId(deptId);
        cb.setInsId(deptId);

        cb.setBalance(0.0);
        cb.setPaymentMethod(paymentMethod);
        cb.setBilledBill(originalBill);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

        return cb;
    }

    private boolean errorsPresentOnOpdBillCancellation() {
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        if (getBill().getBillType() == BillType.LabBill) {
            if (patientInvestigation.getCollected()) {
                JsfUtil.addErrorMessage("You can't cancell this bill. Sample is already taken");
                return true;
            }
            if (patientInvestigation.getPrinted()) {
                JsfUtil.addErrorMessage("You can't cancell this bill. Report is already printed");
                return true;
            }

        }
        // Now if the payment method is NULL, payments are created as in the original bill
//        if (getPaymentMethod() == null) {
//            JsfUtil.addErrorMessage("Please select a payment scheme for Cancellation.");
//            return true;
//        }

        if (getPaymentMethod() == null) {
            boolean hasMoreThanoneIndividualBillsForTHisIndividualBill = billService.hasMultipleIndividualBillsForBatchBillOfThisIndividualBill(getBill());
            if (hasMoreThanoneIndividualBillsForTHisIndividualBill) {
                JsfUtil.addErrorMessage("You can't use Same as Billed when there are multiple bills for a Batch bill. Please select a Payment Method");
                return true;
            }
        }

        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }

        return false;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    @Inject
    EnumController enumController;

    public boolean checkCancelBill(Bill originalBill) {
        List<PatientInvestigationStatus> availableStatus = enumController.getAvailableStatusforCancel();
        boolean canCancelBill = false;
        if (availableStatus.contains(originalBill.getStatus())) {
            canCancelBill = true;
        }
        return canCancelBill;
    }

    /**
     * Validates payment method data for bill cancellation operations. Checks if
     * mandatory fields are provided based on configuration settings.
     *
     * @return true if validation errors exist, false if validation passes
     */
    public boolean validatePaymentMethodData() {
        boolean error = false;

        if (getPaymentMethod() == PaymentMethod.Card) {
            if (getPaymentMethodData() != null && getPaymentMethodData().getCreditCard() != null) {
                String comment = getPaymentMethodData().getCreditCard().getComment();
                if ((comment == null || comment.trim().isEmpty())
                        && configOptionApplicationController.getBooleanValueByKey("OPD Billing - CreditCard Comment is Mandatory", false)) {
                    JsfUtil.addErrorMessage("Please Enter a Credit Card Comment..");
                    error = true;
                }
            }
        } else if (getPaymentMethod() == PaymentMethod.Cheque) {
            if (getPaymentMethodData() != null && getPaymentMethodData().getCheque() != null) {
                String comment = getPaymentMethodData().getCheque().getComment();
                if ((comment == null || comment.trim().isEmpty())
                        && configOptionApplicationController.getBooleanValueByKey("OPD Billing - Cheque Comment is Mandatory", false)) {
                    JsfUtil.addErrorMessage("Please Enter a Cheque Comment..");
                    error = true;
                }
            }
        } else if (getPaymentMethod() == PaymentMethod.ewallet) {
            if (getPaymentMethodData() != null && getPaymentMethodData().getEwallet() != null) {
                String comment = getPaymentMethodData().getEwallet().getComment();
                if ((comment == null || comment.trim().isEmpty())
                        && configOptionApplicationController.getBooleanValueByKey("OPD Billing - E-Wallet Comment is Mandatory", false)) {
                    JsfUtil.addErrorMessage("Please Enter a E-Wallet Comment..");
                    error = true;
                }
            }
        } else if (getPaymentMethod() == PaymentMethod.Slip) {
            if (getPaymentMethodData() != null && getPaymentMethodData().getSlip() != null) {
                String comment = getPaymentMethodData().getSlip().getComment();
                if ((comment == null || comment.trim().isEmpty())
                        && configOptionApplicationController.getBooleanValueByKey("OPD Billing - Slip Comment is Mandatory", false)) {
                    JsfUtil.addErrorMessage("Please Enter a Slip Comment..");
                    error = true;
                }
            }
        } else if (getPaymentMethod() == PaymentMethod.Credit) {
            if (getPaymentMethodData() != null && getPaymentMethodData().getCredit() != null) {
                String comment = getPaymentMethodData().getCredit().getComment();
                if ((comment == null || comment.trim().isEmpty())
                        && configOptionApplicationController.getBooleanValueByKey("OPD Billing - Credit Comment is Mandatory", false)) {
                    JsfUtil.addErrorMessage("Please Enter a Credit Comment..");
                    error = true;
                }
            }
        }
        return error;
    }

    public void cancelOpdBill() {
        if (getBill() == null) {
            JsfUtil.addErrorMessage("No Original Bill Selected To Cancel");
            return;
        }
        if (getBill().getId() == null) {
            JsfUtil.addErrorMessage("No Saved Original Bill");
            return;
        }
        if (getBill().getBackwardReferenceBill() == null) {
            JsfUtil.addErrorMessage("No Batch Bill found for the Individual Bill which is selected to Cancel");
            return;
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Select a Payment Method");
            return;
        }

        // Validate payment method data
        if (validatePaymentMethodData()) {
            return;
        }

        if (getBill().getBackwardReferenceBill().getPaymentMethod() == PaymentMethod.Credit) {
            List<BillItem> items = billService.checkCreditBillPaymentReciveFromCreditCompany(getBill().getBackwardReferenceBill());
            if (items != null && !items.isEmpty()) {
                JsfUtil.addErrorMessage("This bill has been paid for by the credit company. Therefore, it cannot be canceled.");
                return;
            }
        }

        List<PatientInvestigation> investigations = billService.fetchPatientInvestigations(getBill(), PatientInvestigationStatus.SAMPLE_SENT_TO_OUTLAB);

        if (investigations != null && !investigations.isEmpty()) {
            JsfUtil.addErrorMessage("Some Investigations's Samples Send to Out Lab.");
            return;
        }

        if (errorsPresentOnOpdBillCancellation()) {
            return;
        }

        if (professionalPaymentService.isProfessionalFeePaid(bill)) {
            JsfUtil.addErrorMessage("Payments are already made to Staff or Outside Institute. Please cancel them first before cancelling the bill.");
            return;
        }

        if (paymentMethod == PaymentMethod.Staff) {
            if (getBill().getToStaff() == null) {
                JsfUtil.addErrorMessage("Can't Select Staff Method");
                return;
            }
        }

        if (paymentMethod == PaymentMethod.Staff_Welfare) {
            if (getBill().getToStaff() == null) {
                JsfUtil.addErrorMessage("Can't Select Staff Welfare Method");
                return;
            }
        }

        if (!configOptionApplicationController.getBooleanValueByKey("Enable the Special Privilege of Canceling OPD Bills", false)) {
            if (!checkCancelBill(getBill())) {
                JsfUtil.addErrorMessage("This bill is processed in the laboratory.");
                if (getWebUserController().hasPrivilege("BillCancel")) {
                    JsfUtil.addErrorMessage("You have Special privilege to cancel This Bill");
                } else {
                    JsfUtil.addErrorMessage("You have no Privilege to Cancel OPD Bills. Please Contact System Administrator.");
                    return;
                }
            } else {
                if (!getWebUserController().hasPrivilege("OpdIndividualCancel")) {
                    JsfUtil.addErrorMessage("You have no Privilege to Cancel OPD Bills. Please Contact System Administrator.");
                    return;
                }
            }
        } else {
            if (!getWebUserController().hasPrivilege("OpdIndividualCancel")) {
                JsfUtil.addErrorMessage("You have no Privilege to Cancel OPD Bills. Please Contact System Administrator.");
                return;
            }
        }

        // CRITICAL: Check if batch bill has been settled with credit company
        Bill batchBill = bill.getBackwardReferenceBill();
        if (batchBill != null && batchBill.getPaymentMethod() == PaymentMethod.Credit) {
            List<BillItem> settledItems = billService.checkCreditBillPaymentReciveFromCreditCompany(batchBill);

            if (settledItems != null && !settledItems.isEmpty()) {
                JsfUtil.addErrorMessage("Cannot cancel: Batch bill has been settled by credit company. Please process manual adjustment.");
                return;
            }
        }

        CancelledBill cancellationBill = createOpdCancelBill(bill);
        billController.save(cancellationBill);

        // Apply refund sign to payment data
        applyRefundSignToPaymentData();

        // Create payments using PaymentService
        List<Payment> ps = paymentService.createPayment(cancellationBill, paymentMethodData);
        List<BillItem> list = cancelBillItems(getBill(), cancellationBill, ps);

        try {
            if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
                for (PatientInvestigation pi : patientInvestigationController.getPatientInvestigationsFromBill(getBill())) {
                    labTestHistoryController.addCancelHistory(pi, sessionController.getDepartment(), comment);
                }
            }
        } catch (Exception e) {
        }

        cancellationBill.setBillItems(list);
        billFacade.edit(cancellationBill);

        getBill().setCancelled(true);
        getBill().setCancelledBill(cancellationBill);

        billController.save(getBill());

        // Update batch bill balance for credit payment method
        updateBatchBillFinancialFieldsForIndividualCancellation(bill, cancellationBill);

        drawerController.updateDrawerForOuts(ps);
        JsfUtil.addSuccessMessage("Cancelled");

        if (cancellationBill.getPaymentMethod() == PaymentMethod.Credit) {
            //TODO: Manage Credit Balances for Company, Staff
            if (cancellationBill.getToStaff() != null) {
                staffBean.updateStaffCredit(cancellationBill.getToStaff(), 0 - (getBill().getNetTotal() + getBill().getVat()));
                JsfUtil.addSuccessMessage("Staff Credit Updated");
                cancellationBill.setFromStaff(cancellationBill.getToStaff());
                getBillFacade().edit(cancellationBill);
            }
        }

        if (cancellationBill.getPaymentMethod() == PaymentMethod.Staff_Welfare) {
            if (cancellationBill.getToStaff() != null) {
                staffBean.updateStaffWelfare(cancellationBill.getToStaff(), 0 - (getBill().getNetTotal() + getBill().getVat()));
                JsfUtil.addSuccessMessage("Staff Welfare Updated");
                cancellationBill.setFromStaff(cancellationBill.getToStaff());
                getBillFacade().edit(cancellationBill);
            }
        }

        if (cancellationBill.getPaymentMethod() == PaymentMethod.PatientDeposit) {
            PatientDeposit pd = patientDepositService.getDepositOfThePatient(cancellationBill.getPatient(), sessionController.getDepartment());
            patientDepositService.updateBalance(cancellationBill, pd);
        }

        notificationController.createNotification(cancellationBill);
        bill = billFacade.find(cancellationBill.getId());
        printPreview = true;
        duplicate = false;
        comment = null;

//            getEjbApplication().getBillsToCancel().add(cb);
//            JsfUtil.addSuccessMessage("Awaiting Cancellation");
    }

    @EJB
    PatientSampleComponantFacade patientSampleComponantFacade;
    @Inject
    PatientReportController patientReportController;

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void approveCancellation() {

        if (billsApproving == null) {
            JsfUtil.addErrorMessage("Select Bill to Approve Cancell");
            return;
        }
        for (Bill b : billsApproving) {

            b.setApproveUser(getSessionController().getCurrent());
            b.setApproveAt(Calendar.getInstance().getTime());
            getBillFacade().create(b);

            cancelBillItems(b);
            b.getBilledBill().setCancelled(true);
            b.getBilledBill().setCancelledBill(b);

            getBillFacade().edit(b);

            ejbApplication.getBillsToCancel().remove(b);

            JsfUtil.addSuccessMessage("Cancelled");

        }

        billForCancel = null;
    }

    public List<Bill> getOpdBillsToApproveCancellation() {
        billsToApproveCancellation = ejbApplication.getOpdBillsToCancel();
        return billsToApproveCancellation;
    }

    public List<Bill> getBillsToApproveCancellation() {
        billsToApproveCancellation = ejbApplication.getBillsToCancel();
        return billsToApproveCancellation;
    }

    public void setBillsToApproveCancellation(List<Bill> billsToApproveCancellation) {
        this.billsToApproveCancellation = billsToApproveCancellation;
    }

    public List<Bill> getBillsApproving() {
        return billsApproving;
    }

    public void setBillsApproving(List<Bill> billsApproving) {
        this.billsApproving = billsApproving;
    }

    private List<BillItem> cancelBillItems(Bill can) {
        List<BillItem> list = new ArrayList<>();
        for (BillItem nB : getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);

            if (can.getBillType() != BillType.PaymentBill) {
                b.setItem(nB.getItem());
            } else {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            }

            b.setNetValue(0 - nB.getNetValue());
            b.setGrossValue(0 - nB.getGrossValue());
            b.setRate(0 - nB.getRate());

            b.setCatId(nB.getCatId());
            b.setDeptId(nB.getDeptId());
            b.setInsId(nB.getInsId());
            b.setDiscount(nB.getDiscount());
            b.setQty(1.0);
            b.setRate(nB.getRate());

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            b.setPaidForBillFee(nB.getPaidForBillFee());

            billItemFacede.create(b);

//            cancelBillComponents(can, b);
            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();

            List<BillFee> originalBillFeesOfBillItem = getBillFeeFacade().findByJpql(sql);

            cancelBillFee(can, b, originalBillFeesOfBillItem);

            list.add(b);

        }

        return list;
    }

    private List<BillItem> cancelBillItems(Bill originalBill, Bill cancellationBill, List<Payment> ps) {
        List<BillItem> list = new ArrayList<>();
        for (BillItem nB : originalBill.getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(cancellationBill);

            if (cancellationBill.getBillType() != BillType.PaymentBill) {
                b.setItem(nB.getItem());
            } else {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            }

            b.setHospitalFee(0 - nB.getHospitalFee());
            b.setCollectingCentreFee(0 - nB.getCollectingCentreFee());
            b.setStaffFee(0 - nB.getStaffFee());
            b.setReagentFee(0 - nB.getReagentFee());
            b.setOtherFee(0 - nB.getOtherFee());

            b.setNetValue(0 - nB.getNetValue());
            b.setGrossValue(0 - nB.getGrossValue());
            b.setRate(0 - nB.getRate());
            b.setVat(0 - nB.getVat());
            b.setVatPlusNetValue(0 - nB.getVatPlusNetValue());

            b.setCatId(nB.getCatId());
            b.setDeptId(nB.getDeptId());
            b.setInsId(nB.getInsId());
            b.setDiscount(0 - nB.getDiscount());
            b.setQty(0 - nB.getQty());
            b.setRate(nB.getRate());

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            b.setPaidForBillFee(nB.getPaidForBillFee());

            billItemFacede.create(b);

            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
            cancelBillFee(cancellationBill, b, tmp);
            list.add(b);

        }

        return list;
    }

    private void cancelBillFee(Bill cancellationProfessionalPaymentBill,
            BillItem cancellationProfessionalBillItem,
            List<BillFee> originalProfessionalPaymentFeesForBillItem) {
        for (BillFee originalProfessionalPaymentFeeForBillItem : originalProfessionalPaymentFeesForBillItem) {
            BillFee newCancellingBillFee = new BillFee();
            newCancellingBillFee.setFee(originalProfessionalPaymentFeeForBillItem.getFee());
            newCancellingBillFee.setPatienEncounter(originalProfessionalPaymentFeeForBillItem.getPatienEncounter());
            newCancellingBillFee.setPatient(originalProfessionalPaymentFeeForBillItem.getPatient());
            if (originalProfessionalPaymentFeeForBillItem.getReferenceBillFee() != null && newCancellingBillFee.getPatient() == null) {
                newCancellingBillFee.setPatient(originalProfessionalPaymentFeeForBillItem.getReferenceBillFee().getBill().getPatient());
            }
            newCancellingBillFee.setDepartment(originalProfessionalPaymentFeeForBillItem.getDepartment());
            newCancellingBillFee.setInstitution(originalProfessionalPaymentFeeForBillItem.getInstitution());
            newCancellingBillFee.setSpeciality(originalProfessionalPaymentFeeForBillItem.getSpeciality());
            newCancellingBillFee.setStaff(originalProfessionalPaymentFeeForBillItem.getStaff());

            newCancellingBillFee.setBill(cancellationProfessionalPaymentBill);
            newCancellingBillFee.setBillItem(cancellationProfessionalBillItem);
            newCancellingBillFee.setFeeValue(0 - originalProfessionalPaymentFeeForBillItem.getFeeValue());
            newCancellingBillFee.setFeeGrossValue(0 - originalProfessionalPaymentFeeForBillItem.getFeeGrossValue());
            newCancellingBillFee.setFeeDiscount(0 - originalProfessionalPaymentFeeForBillItem.getFeeDiscount());
            newCancellingBillFee.setSettleValue(0 - originalProfessionalPaymentFeeForBillItem.getSettleValue());
            newCancellingBillFee.setFeeVat(0 - originalProfessionalPaymentFeeForBillItem.getFeeVat());
            newCancellingBillFee.setFeeVatPlusValue(0 - originalProfessionalPaymentFeeForBillItem.getFeeVatPlusValue());

            newCancellingBillFee.setCreatedAt(new Date());
            newCancellingBillFee.setCreater(getSessionController().getLoggedUser());

            getBillFeeFacade().create(newCancellingBillFee);
        }
    }

    public boolean isShowAllBills() {
        return showAllBills;
    }

    public void setShowAllBills(boolean showAllBills) {
        this.showAllBills = showAllBills;
    }

    public void allBills() {
        showAllBills = true;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Long getSelectedBillId() {
        return selectedBillId;
    }

    public void setSelectedBillId(Long selectedBillId) {
        this.selectedBillId = selectedBillId;
    }

    public String navigateToCancelOpdBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }

        // Initialize payment method data
        paymentMethodData = new PaymentMethodData();

        // Load original bill payments from batch bill and set default payment method
        if (bill.getBackwardReferenceBill() != null) {
            Bill batchBill = bill.getBackwardReferenceBill();
            billPayments = billBean.fetchBillPayments(batchBill);

            // Default payment method to batch bill's payment method
            if (batchBill.getPaymentMethod() != null) {
                paymentMethod = batchBill.getPaymentMethod();
            } else if (configOptionApplicationController.getBooleanValueByKey("Set the Original Bill PaymentMethod to Cancelation Bill")) {
                boolean moreThanOneIndividualBillsForTheBatchBillOfThisIndividualBill = billService.hasMultipleIndividualBillsForBatchBillOfThisIndividualBill(bill);
                if (moreThanOneIndividualBillsForTheBatchBillOfThisIndividualBill) {
                    paymentMethod = bill.getPaymentMethod();
                } else {
                    paymentMethod = null;
                }
            } else {
                paymentMethod = PaymentMethod.Cash;
            }

            // Initialize payment method data from original payments
            if (billPayments != null && !billPayments.isEmpty()) {
                initializePaymentDataFromOriginalPayments(billPayments);
            }

            // Load and store original payment details for UI compatibility (card picker, etc.)
            loadOriginalPaymentDetails();
        } else {
            // Fallback if no batch bill
            paymentMethod = PaymentMethod.Cash;
        }

        createBillItemsAndBillFees();

        boolean flag = billController.checkBillValues(bill);
        bill.setTransError(flag);
        printPreview = false;
        return "/opd/bill_cancel?faces-redirect=true";
    }
    
    private void createBillItemsAndBillFees() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "SELECT b FROM BillItem b"
                + "  WHERE b.retired=false "
                + " and b.bill=:b";
        hm.put("b", getBillSearch());
        billItems = billItemFacede.findByJpql(sql, hm);

        for (BillItem bi : billItems) {
            sql = "SELECT bi FROM BillItem bi where bi.retired=false and bi.referanceBillItem.id=" + bi.getId();
            BillItem rbi = getBillItemFacade().findFirstByJpql(sql);

            if (rbi != null) {
                bi.setTransRefund(true);
            } else {
                bi.setTransRefund(false);
            }
        }

    }

    /**
     * Initializes payment method data from the original bill's payment records.
     * This method populates payment method details (card numbers, reference
     * numbers, etc.) based on how the original bill was paid.
     *
     * @param originalPayments List of Payment objects from the original bill
     */
    private void initializePaymentDataFromOriginalPayments(List<Payment> originalPayments) {
        System.out.println("=== initializePaymentDataFromOriginalPayments() CALLED ===");
        System.out.println("User selected paymentMethod: " + this.paymentMethod);
        System.out.println("Original payments count: " + (originalPayments != null ? originalPayments.size() : 0));

        if (originalPayments == null || originalPayments.isEmpty()) {
            System.out.println("initializePaymentDataFromOriginalPayments: No original payments to process");
            return;
        }

        // For single payment method
        if (originalPayments.size() == 1) {
            Payment originalPayment = originalPayments.get(0);
            paymentMethod = originalPayment.getPaymentMethod();

            // Initialize paymentMethodData based on payment method (using absolute values for UI display)
            switch (originalPayment.getPaymentMethod()) {
                case Cash:
                    getPaymentMethodData().getCash().setTotalValue(Math.abs(bill.getNetTotal()));
                    break;
                case Card:
                    getPaymentMethodData().getCreditCard().setInstitution(originalPayment.getBank());
                    getPaymentMethodData().getCreditCard().setNo(originalPayment.getCreditCardRefNo());
                    getPaymentMethodData().getCreditCard().setComment(originalPayment.getComments());
                    getPaymentMethodData().getCreditCard().setTotalValue(Math.abs(bill.getNetTotal()));
                    break;
                case Cheque:
                    getPaymentMethodData().getCheque().setInstitution(originalPayment.getBank());
                    getPaymentMethodData().getCheque().setDate(originalPayment.getChequeDate());
                    getPaymentMethodData().getCheque().setNo(originalPayment.getChequeRefNo());
                    getPaymentMethodData().getCheque().setComment(originalPayment.getComments());
                    getPaymentMethodData().getCheque().setTotalValue(Math.abs(bill.getNetTotal()));
                    break;
                case Slip:
                    getPaymentMethodData().getSlip().setInstitution(originalPayment.getBank());
                    getPaymentMethodData().getSlip().setDate(originalPayment.getPaymentDate());
                    getPaymentMethodData().getSlip().setReferenceNo(originalPayment.getReferenceNo());
                    getPaymentMethodData().getSlip().setComment(originalPayment.getComments());
                    getPaymentMethodData().getSlip().setTotalValue(Math.abs(bill.getNetTotal()));
                    break;
                case ewallet:
                    getPaymentMethodData().getEwallet().setInstitution(originalPayment.getBank() != null ? originalPayment.getBank() : originalPayment.getInstitution());
                    getPaymentMethodData().getEwallet().setReferenceNo(originalPayment.getReferenceNo());
                    getPaymentMethodData().getEwallet().setNo(originalPayment.getReferenceNo());
                    getPaymentMethodData().getEwallet().setReferralNo(originalPayment.getPolicyNo());
                    getPaymentMethodData().getEwallet().setTotalValue(Math.abs(bill.getNetTotal()));
                    getPaymentMethodData().getEwallet().setComment(originalPayment.getComments());
                    break;
                case PatientDeposit:
                    getPaymentMethodData().getPatient_deposit().setTotalValue(Math.abs(bill.getNetTotal()));
                    getPaymentMethodData().getPatient_deposit().setPatient(bill.getPatient());
                    getPaymentMethodData().getPatient_deposit().setComment(originalPayment.getComments());
                    break;
                case Credit:
                    getPaymentMethodData().getCredit().setInstitution(originalPayment.getCreditCompany());
                    getPaymentMethodData().getCredit().setReferenceNo(originalPayment.getReferenceNo());
                    getPaymentMethodData().getCredit().setReferralNo(originalPayment.getPolicyNo());
                    getPaymentMethodData().getCredit().setComment(originalPayment.getComments());
                    getPaymentMethodData().getCredit().setTotalValue(Math.abs(bill.getNetTotal()));
                    break;
                case Staff:
                    com.divudi.core.entity.Staff staffForCredit = originalPayment.getToStaff();
                    if (staffForCredit == null && bill != null) {
                        staffForCredit = bill.getToStaff();
                    }
                    getPaymentMethodData().getStaffCredit().setToStaff(staffForCredit);
                    getPaymentMethodData().getStaffCredit().setComment(originalPayment.getComments());
                    getPaymentMethodData().getStaffCredit().setTotalValue(Math.abs(bill.getNetTotal()));
                    break;
                case Staff_Welfare:
                    com.divudi.core.entity.Staff staffForWelfare = originalPayment.getToStaff();
                    if (staffForWelfare == null && bill != null) {
                        staffForWelfare = bill.getToStaff();
                    }
                    getPaymentMethodData().getStaffWelfare().setToStaff(staffForWelfare);
                    getPaymentMethodData().getStaffWelfare().setComment(originalPayment.getComments());
                    getPaymentMethodData().getStaffWelfare().setTotalValue(Math.abs(bill.getNetTotal()));
                    break;
                case OnlineSettlement:
                    getPaymentMethodData().getOnlineSettlement().setInstitution(originalPayment.getBank() != null ? originalPayment.getBank() : originalPayment.getInstitution());
                    getPaymentMethodData().getOnlineSettlement().setReferenceNo(originalPayment.getReferenceNo());
                    getPaymentMethodData().getOnlineSettlement().setDate(originalPayment.getPaymentDate());
                    getPaymentMethodData().getOnlineSettlement().setComment(originalPayment.getComments());
                    getPaymentMethodData().getOnlineSettlement().setTotalValue(Math.abs(bill.getNetTotal()));
                    break;
                default:
                    // For any other payment method, just set the total value
                    break;
            }
        } else {
            // Multiple payments - check if user selected a specific payment method that exists
            System.out.println("initializePaymentDataFromOriginalPayments: Multiple payments detected");

            // Check if user selected a specific payment method (not MultiplePaymentMethods) that exists in original payments
            if (this.paymentMethod != null && this.paymentMethod != PaymentMethod.MultiplePaymentMethods) {
                Payment matchingPayment = null;

                // Find the payment that matches user's selected method
                for (Payment payment : originalPayments) {
                    PaymentMethod actualPaymentMethod = determineActualPaymentMethod(payment);
                    if (this.paymentMethod == actualPaymentMethod) {
                        matchingPayment = payment;
                        System.out.println("initializePaymentDataFromOriginalPayments: Found matching payment for " + this.paymentMethod);
                        break;
                    }
                }

                if (matchingPayment != null) {
                    // User selected a specific payment method that exists - initialize only that payment method
                    System.out.println("initializePaymentDataFromOriginalPayments: Initializing " + this.paymentMethod + " payment data");
                    initializeSinglePaymentMethodData(matchingPayment);
                    return; // Don't override to MultiplePaymentMethods
                } else {
                    System.out.println("initializePaymentDataFromOriginalPayments: User's selected payment method " + this.paymentMethod + " not found in original payments");
                }
            }

            // User either selected MultiplePaymentMethods explicitly OR selected a method not in original payments
            // Fall back to MultiplePaymentMethods behavior
            System.out.println("initializePaymentDataFromOriginalPayments: Using MultiplePaymentMethods");
            paymentMethod = PaymentMethod.MultiplePaymentMethods;

            // Initialize multiple payment method structure with original payment details
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
                        cd.getPaymentMethodData().getCreditCard().setComment(originalPayment.getComments());
                        cd.getPaymentMethodData().getCreditCard().setTotalValue(refundAmount);
                        break;
                    case Cheque:
                        cd.getPaymentMethodData().getCheque().setInstitution(originalPayment.getBank());
                        cd.getPaymentMethodData().getCheque().setDate(originalPayment.getChequeDate());
                        cd.getPaymentMethodData().getCheque().setNo(originalPayment.getChequeRefNo());
                        cd.getPaymentMethodData().getCheque().setComment(originalPayment.getComments());
                        cd.getPaymentMethodData().getCheque().setTotalValue(refundAmount);
                        break;
                    case Slip:
                        cd.getPaymentMethodData().getSlip().setInstitution(originalPayment.getBank());
                        cd.getPaymentMethodData().getSlip().setDate(originalPayment.getPaymentDate() != null ? originalPayment.getPaymentDate() : originalPayment.getRealizedAt());
                        cd.getPaymentMethodData().getSlip().setReferenceNo(originalPayment.getReferenceNo());
                        cd.getPaymentMethodData().getSlip().setComment(originalPayment.getComments());
                        cd.getPaymentMethodData().getSlip().setTotalValue(refundAmount);
                        break;
                    case ewallet:
                        cd.getPaymentMethodData().getEwallet().setInstitution(originalPayment.getBank() != null ? originalPayment.getBank() : originalPayment.getInstitution());
                        cd.getPaymentMethodData().getEwallet().setReferenceNo(originalPayment.getReferenceNo());
                        cd.getPaymentMethodData().getEwallet().setNo(originalPayment.getReferenceNo());
                        cd.getPaymentMethodData().getEwallet().setReferralNo(originalPayment.getPolicyNo());
                        cd.getPaymentMethodData().getEwallet().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getEwallet().setComment(originalPayment.getComments());
                        break;
                    case PatientDeposit:
                        cd.getPaymentMethodData().getPatient_deposit().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getPatient_deposit().setPatient(bill.getPatient());
                        cd.getPaymentMethodData().getPatient_deposit().setComment(originalPayment.getComments());
                        break;
                    case Credit:
                        cd.getPaymentMethodData().getCredit().setInstitution(originalPayment.getCreditCompany());
                        cd.getPaymentMethodData().getCredit().setReferenceNo(originalPayment.getReferenceNo());
                        cd.getPaymentMethodData().getCredit().setReferralNo(originalPayment.getPolicyNo());
                        cd.getPaymentMethodData().getCredit().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getCredit().setComment(originalPayment.getComments());
                        break;
                    case Staff:
                        com.divudi.core.entity.Staff staffForCredit = originalPayment.getToStaff();
                        if (staffForCredit == null && bill != null) {
                            staffForCredit = bill.getToStaff();
                        }
                        cd.getPaymentMethodData().getStaffCredit().setToStaff(staffForCredit);
                        cd.getPaymentMethodData().getStaffCredit().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getStaffCredit().setComment(originalPayment.getComments());
                        break;
                    case Staff_Welfare:
                        com.divudi.core.entity.Staff staffForWelfare = originalPayment.getToStaff();
                        if (staffForWelfare == null && bill != null) {
                            staffForWelfare = bill.getToStaff();
                        }
                        cd.getPaymentMethodData().getStaffWelfare().setToStaff(staffForWelfare);
                        cd.getPaymentMethodData().getStaffWelfare().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getStaffWelfare().setComment(originalPayment.getComments());
                        break;
                    case OnlineSettlement:
                        cd.getPaymentMethodData().getOnlineSettlement().setInstitution(originalPayment.getBank() != null ? originalPayment.getBank() : originalPayment.getInstitution());
                        cd.getPaymentMethodData().getOnlineSettlement().setReferenceNo(originalPayment.getReferenceNo());
                        cd.getPaymentMethodData().getOnlineSettlement().setDate(originalPayment.getPaymentDate());
                        cd.getPaymentMethodData().getOnlineSettlement().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getOnlineSettlement().setComment(originalPayment.getComments());
                        break;
                    default:
                        // For any other payment method, just set the total value
                        break;
                }

                // Add this component detail to the multiple payment method structure
                getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().add(cd);
            }
        }
    }

    /**
     * Initialize payment data for a specific payment method from a single Payment object
     * This is used when there are multiple original payments but user selected a specific payment method
     *
     * @param payment The Payment object containing the original payment data
     */
    private void initializeSinglePaymentMethodData(Payment payment) {
        if (payment == null) {
            System.out.println("initializeSinglePaymentMethodData: Payment is null");
            return;
        }

        PaymentMethod actualPaymentMethod = determineActualPaymentMethod(payment);
        System.out.println("initializeSinglePaymentMethodData: Initializing " + actualPaymentMethod + " with amount " + payment.getPaidValue());

        // Initialize paymentMethodData based on payment method (using absolute values for UI display)
        switch (actualPaymentMethod) {
            case Cash:
                getPaymentMethodData().getCash().setTotalValue(Math.abs(bill.getNetTotal()));
                break;
            case Card:
                getPaymentMethodData().getCreditCard().setInstitution(payment.getBank());
                getPaymentMethodData().getCreditCard().setNo(payment.getCreditCardRefNo());
                getPaymentMethodData().getCreditCard().setReferenceNo(payment.getReferenceNo());
                getPaymentMethodData().getCreditCard().setComment(payment.getComments());
                getPaymentMethodData().getCreditCard().setTotalValue(Math.abs(bill.getNetTotal()));
                System.out.println("initializeSinglePaymentMethodData: Card data - Bank: " + (payment.getBank() != null ? payment.getBank().getName() : "null")
                        + ", Ref: " + payment.getReferenceNo() + ", Amount: " + Math.abs(bill.getNetTotal()));
                break;
            case Cheque:
                getPaymentMethodData().getCheque().setInstitution(payment.getBank());
                getPaymentMethodData().getCheque().setDate(payment.getChequeDate());
                getPaymentMethodData().getCheque().setNo(payment.getChequeRefNo());
                getPaymentMethodData().getCheque().setComment(payment.getComments());
                getPaymentMethodData().getCheque().setTotalValue(Math.abs(bill.getNetTotal()));
                break;
            case Slip:
                getPaymentMethodData().getSlip().setInstitution(payment.getBank());
                getPaymentMethodData().getSlip().setDate(payment.getPaymentDate());
                getPaymentMethodData().getSlip().setReferenceNo(payment.getReferenceNo());
                getPaymentMethodData().getSlip().setComment(payment.getComments());
                getPaymentMethodData().getSlip().setTotalValue(Math.abs(bill.getNetTotal()));
                break;
            case ewallet:
                getPaymentMethodData().getEwallet().setInstitution(payment.getBank() != null ? payment.getBank() : payment.getInstitution());
                getPaymentMethodData().getEwallet().setReferenceNo(payment.getReferenceNo());
                getPaymentMethodData().getEwallet().setNo(payment.getReferenceNo());
                getPaymentMethodData().getEwallet().setReferralNo(payment.getPolicyNo());
                getPaymentMethodData().getEwallet().setTotalValue(Math.abs(bill.getNetTotal()));
                getPaymentMethodData().getEwallet().setComment(payment.getComments());
                break;
            case PatientDeposit:
                getPaymentMethodData().getPatient_deposit().setTotalValue(Math.abs(bill.getNetTotal()));
                getPaymentMethodData().getPatient_deposit().setPatient(bill.getPatient());
                getPaymentMethodData().getPatient_deposit().setComment(payment.getComments());
                break;
            case Credit:
                getPaymentMethodData().getCredit().setInstitution(payment.getCreditCompany());
                getPaymentMethodData().getCredit().setReferenceNo(payment.getReferenceNo());
                getPaymentMethodData().getCredit().setReferralNo(payment.getPolicyNo());
                getPaymentMethodData().getCredit().setComment(payment.getComments());
                getPaymentMethodData().getCredit().setTotalValue(Math.abs(bill.getNetTotal()));
                break;
            case Staff:
                com.divudi.core.entity.Staff staffForCredit = payment.getToStaff();
                if (staffForCredit == null && bill != null) {
                    staffForCredit = bill.getToStaff();
                }
                getPaymentMethodData().getStaffCredit().setToStaff(staffForCredit);
                getPaymentMethodData().getStaffCredit().setComment(payment.getComments());
                getPaymentMethodData().getStaffCredit().setTotalValue(Math.abs(bill.getNetTotal()));
                break;
            case Staff_Welfare:
                com.divudi.core.entity.Staff staffForWelfare = payment.getToStaff();
                if (staffForWelfare == null && bill != null) {
                    staffForWelfare = bill.getToStaff();
                }
                getPaymentMethodData().getStaffWelfare().setToStaff(staffForWelfare);
                getPaymentMethodData().getStaffWelfare().setComment(payment.getComments());
                getPaymentMethodData().getStaffWelfare().setTotalValue(Math.abs(bill.getNetTotal()));
                break;
            case OnlineSettlement:
                getPaymentMethodData().getOnlineSettlement().setInstitution(payment.getBank() != null ? payment.getBank() : payment.getInstitution());
                getPaymentMethodData().getOnlineSettlement().setReferenceNo(payment.getReferenceNo());
                getPaymentMethodData().getOnlineSettlement().setDate(payment.getPaymentDate());
                getPaymentMethodData().getOnlineSettlement().setComment(payment.getComments());
                getPaymentMethodData().getOnlineSettlement().setTotalValue(Math.abs(bill.getNetTotal()));
                break;
            default:
                // For any other payment method, just set the total value
                System.out.println("initializeSinglePaymentMethodData: Unhandled payment method: " + actualPaymentMethod);
                break;
        }

        System.out.println("initializeSinglePaymentMethodData: Successfully initialized " + actualPaymentMethod + " payment data");
    }

    /**
     * Applies refund sign (negative values) to all payment method data. This
     * ensures that payment records for refunds/cancellations are stored with
     * negative amounts.
     */
    private void applyRefundSignToPaymentData() {
        if (paymentMethodData == null) {
            return;
        }

        // Handle multiple payment methods
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            if (paymentMethodData.getPaymentMethodMultiple() != null
                    && paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() != null) {
                for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    if (cd.getPaymentMethodData() != null) {
                        applyRefundSignToSinglePaymentMethodData(cd.getPaymentMethodData());
                    }
                }
            }
        } else {
            // Handle single payment method
            applyRefundSignToSinglePaymentMethodData(paymentMethodData);
        }
    }

    /**
     * Helper method to apply refund sign to a single PaymentMethodData object.
     * This ensures all payment amounts are negative (cash going out).
     */
    private void applyRefundSignToSinglePaymentMethodData(PaymentMethodData pmd) {
        if (pmd == null) {
            return;
        }

        // Apply negative sign to each payment method's total value
        if (pmd.getCash() != null && pmd.getCash().getTotalValue() != 0) {
            pmd.getCash().setTotalValue(-Math.abs(pmd.getCash().getTotalValue()));
        }
        if (pmd.getCreditCard() != null && pmd.getCreditCard().getTotalValue() != 0) {
            pmd.getCreditCard().setTotalValue(-Math.abs(pmd.getCreditCard().getTotalValue()));
        }
        if (pmd.getCheque() != null && pmd.getCheque().getTotalValue() != 0) {
            pmd.getCheque().setTotalValue(-Math.abs(pmd.getCheque().getTotalValue()));
        }
        if (pmd.getSlip() != null && pmd.getSlip().getTotalValue() != 0) {
            pmd.getSlip().setTotalValue(-Math.abs(pmd.getSlip().getTotalValue()));
        }
        if (pmd.getEwallet() != null && pmd.getEwallet().getTotalValue() != 0) {
            pmd.getEwallet().setTotalValue(-Math.abs(pmd.getEwallet().getTotalValue()));
        }
        if (pmd.getPatient_deposit() != null && pmd.getPatient_deposit().getTotalValue() != 0) {
            pmd.getPatient_deposit().setTotalValue(-Math.abs(pmd.getPatient_deposit().getTotalValue()));
        }
        if (pmd.getCredit() != null && pmd.getCredit().getTotalValue() != 0) {
            pmd.getCredit().setTotalValue(-Math.abs(pmd.getCredit().getTotalValue()));
        }
        if (pmd.getStaffCredit() != null && pmd.getStaffCredit().getTotalValue() != 0) {
            pmd.getStaffCredit().setTotalValue(-Math.abs(pmd.getStaffCredit().getTotalValue()));
        }
        if (pmd.getStaffWelfare() != null && pmd.getStaffWelfare().getTotalValue() != 0) {
            pmd.getStaffWelfare().setTotalValue(-Math.abs(pmd.getStaffWelfare().getTotalValue()));
        }
        if (pmd.getOnlineSettlement() != null && pmd.getOnlineSettlement().getTotalValue() != 0) {
            pmd.getOnlineSettlement().setTotalValue(-Math.abs(pmd.getOnlineSettlement().getTotalValue()));
        }
    }

    public boolean chackRefundORCancelBill(Bill bill) {
        return CommonFunctions.dateAfter24Hours(bill.getCreatedAt()).after(new Date());
    }

    public String navigateToViewCancallationOpdBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        return "/opd/view/cancelled_opd_bill?faces-redirect=true";
    }

    public List<BillEntry> getBillEntrys() {
        return billEntrys;
    }

    public void setBillEntrys(List<BillEntry> billEntrys) {
        this.billEntrys = billEntrys;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

   

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    public Bill getBillForCancel() {
        return billForCancel;
    }

    public void setBillForCancel(Bill billForCancel) {
        this.billForCancel = billForCancel;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public List<BillItem> getTempbillItems() {
        if (tempbillItems == null) {
            tempbillItems = new ArrayList<>();
        }
        return tempbillItems;
    }

    public void setTempbillItems(List<BillItem> tempbillItems) {
        this.tempbillItems = tempbillItems;
    }

    public void resetLists() {
        recreateModel();
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        //  resetLists();
        this.toDate = toDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        //resetLists();
        this.fromDate = fromDate;

    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public ItemBatchFacade getItemBatchFacade() {
        return itemBatchFacade;
    }

    public void setItemBatchFacade(ItemBatchFacade itemBatchFacade) {
        this.itemBatchFacade = itemBatchFacade;
    }

    public List<Bill> getFilteredBill() {
        return filteredBill;
    }

    public void setFilteredBill(List<Bill> filteredBill) {
        this.filteredBill = filteredBill;
    }

    public PharmacyPreSettleController getPharmacyPreSettleController() {
        return pharmacyPreSettleController;
    }

    public void setPharmacyPreSettleController(PharmacyPreSettleController pharmacyPreSettleController) {
        this.pharmacyPreSettleController = pharmacyPreSettleController;
    }


    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }


    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

   

    public Bill getBillSearch() {
        return bill;
    }

    public Institution getCreditCompany() {
        if (getBillSearch().getPatientEncounter() == null) {
            creditCompany = getBillSearch().getCreditCompany();
        } else {
            creditCompany = getBillSearch().getPatientEncounter().getCreditCompany();
        }

        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

   

    public StaffService getStaffBean() {
        return staffBean;
    }

    public void setStaffBean(StaffService staffBean) {
        this.staffBean = staffBean;
    }

    public BillController getBillController() {
        return billController;
    }

    public void setBillController(BillController billController) {
        this.billController = billController;
    }

    public PatientInvestigationController getPatientInvestigationController() {
        return patientInvestigationController;
    }

    public void setPatientInvestigationController(PatientInvestigationController patientInvestigationController) {
        this.patientInvestigationController = patientInvestigationController;
    }

    public List<BillItem> getBillItemList() {
        return billItemList;
    }

    public void setBillItemList(List<BillItem> billItemList) {
        this.billItemList = billItemList;
    }

    public OpdPreSettleController getOpdPreSettleController() {
        return opdPreSettleController;
    }

    public void setOpdPreSettleController(OpdPreSettleController opdPreSettleController) {
        this.opdPreSettleController = opdPreSettleController;
    }

   
    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        if (department == null) {
            sessionController.getLoggedUser().getDepartment();
        }
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public BillClassType getBillClassType() {
        return billClassType;
    }

    public void setBillClassType(BillClassType billClassType) {
        this.billClassType = billClassType;
    }

    public EmailFacade getEmailFacade() {
        return emailFacade;
    }

   

    public SecurityController getSecurityController() {
        return securityController;
    }

    public BillLight getBillLight() {
        return billLight;
    }

    public void setBillLight(BillLight billLight) {
        bill = billFacade.find(billLight.getId());
        this.billLight = billLight;
    }

    @Override
    public PaymentMethodData getPaymentMethodData() {
        // If we're viewing a bill that was paid with multiple payment methods,
        // and we have stored original payment data, use that for the "Original Payment Details" section
        if (this.bill != null
                && this.bill.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods
                && originalPaymentMethodData != null
                && originalPaymentMethodData.getPaymentMethodMultiple() != null
                && !originalPaymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
            return originalPaymentMethodData;
        }

        // Otherwise return the normal payment method data for editing
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    @Override
    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    /**
     * Initialize payment method data for newly selected payment method Uses
     * stored original payment details if available
     */
    private void initializePaymentMethodData() {
        try {
            System.out.println("initializePaymentMethodData: Initializing for " + this.paymentMethod);

            // Convert stored ComponentDetail objects back to Payment entities for compatibility
            if (originalPaymentDetails != null && !originalPaymentDetails.isEmpty()) {
                List<Payment> paymentEntities = new ArrayList<>();

                for (ComponentDetail cd : originalPaymentDetails) {
                    Payment payment = new Payment();
                    payment.setPaymentMethod(cd.getPaymentMethod());
                    payment.setPaidValue(cd.getTotalValue());
                    payment.setComments(cd.getComment());
                    payment.setReferenceNo(cd.getReferenceNo());
                    payment.setPaymentDate(cd.getDate());
                    payment.setBank(cd.getInstitution());

                    paymentEntities.add(payment);
                }

                // Use existing method with converted payment entities
                initializePaymentDataFromOriginalPayments(paymentEntities);

                System.out.println("initializePaymentMethodData: Used stored payment details");
            } else {
                System.out.println("initializePaymentMethodData: No stored payment details available");
            }
        } catch (Exception e) {
            System.out.println("Error initializing payment method data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public double calculatRemainForMultiplePaymentTotal() {
        if (bill == null) {
            return 0.0;
        }

        double total = Math.abs(bill.getNetTotal());
        double multiplePaymentMethodTotalValue = 0;

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods && paymentMethodData != null) {
            if (paymentMethodData.getPaymentMethodMultiple() != null
                    && paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() != null) {
                for (com.divudi.core.data.dataStructure.ComponentDetail cd
                        : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    multiplePaymentMethodTotalValue += Math.abs(cd.getTotalValue());
                }
            }
        }

        return total - multiplePaymentMethodTotalValue;
    }

    @Override
    public void recieveRemainAmountAutomatically() {
        double remainAmount = calculatRemainForMultiplePaymentTotal();

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods && paymentMethodData != null) {
            if (paymentMethodData.getPaymentMethodMultiple() != null
                    && paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() != null) {
                int arrSize = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
                if (arrSize == 0) {
                    return;
                }

                com.divudi.core.data.dataStructure.ComponentDetail pm
                        = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);

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
                } else if (pm.getPaymentMethod() == PaymentMethod.Credit) {
                    pm.getPaymentMethodData().getCredit().setTotalValue(remainAmount);
                } else if (pm.getPaymentMethod() == PaymentMethod.Staff) {
                    pm.getPaymentMethodData().getStaffCredit().setTotalValue(remainAmount);
                } else if (pm.getPaymentMethod() == PaymentMethod.Staff_Welfare) {
                    pm.getPaymentMethodData().getStaffWelfare().setTotalValue(remainAmount);
                }
            }
        }
    }

    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public List<Bill> getViewingReferanceBills() {
        return viewingReferanceBills;
    }

    public void setViewingReferanceBills(List<Bill> viewingReferanceBills) {
        this.viewingReferanceBills = viewingReferanceBills;
    }

    public List<PharmaceuticalBillItem> getViewingPharmaceuticalBillItems() {
        return viewingPharmaceuticalBillItems;
    }

    public void setViewingPharmaceuticalBillItems(List<PharmaceuticalBillItem> viewingPharmaceuticalBillItems) {
        this.viewingPharmaceuticalBillItems = viewingPharmaceuticalBillItems;
    }

    public List<PatientInvestigation> getViewingPatientInvestigations() {
        return viewingPatientInvestigations;
    }

    public void setViewingPatientInvestigations(List<PatientInvestigation> viewingPatientInvestigations) {
        this.viewingPatientInvestigations = viewingPatientInvestigations;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public int getBillItemSize() {
        return billItemSize;
    }

    public void setBillItemSize(int billItemSize) {
        this.billItemSize = billItemSize;
    }

    /**
     * Updates the batch bill's financial tracking fields when an individual OPD
     * bill is cancelled within a credit payment batch.
     *
     * <p>
     * This method ensures accurate credit balance tracking for partial batch
     * bill cancellations by reducing the batch bill's balance, paidAmount, and
     * increasing refundAmount proportionally to the cancelled individual bill's
     * net total.</p>
     *
     * <p>
     * <b>Healthcare Domain Context:</b> When OPD bills are paid using Credit
     * payment method, the net total becomes the "due amount" (stored in balance
     * field). Credit companies settle these dues periodically. Accurate balance
     * tracking is critical for credit company settlement reports and financial
     * reconciliation.</p>
     *
     * <p>
     * <b>Pattern:</b> Mirrors pharmacy implementation in
     * {@link SaleReturnController#updateBillFinancialFields(Bill, double)}</p>
     *
     * @param individualBill The individual OPD bill being cancelled
     * @param cancellationBill The cancellation bill created for the individual
     * bill
     * @throws IllegalArgumentException if bills are null
     * @throws IllegalStateException if financial data is invalid
     * @see <a href="https://github.com/hmislk/hmis/issues/17138">GitHub Issue
     * #17138</a>
     */
    private void updateBatchBillFinancialFieldsForIndividualCancellation(Bill individualBill, Bill cancellationBill) {
        // Validate inputs
        if (individualBill == null) {
            throw new IllegalArgumentException("Individual bill cannot be null");
        }

        if (cancellationBill == null) {
            throw new IllegalArgumentException("Cancellation bill cannot be null");
        }

        Bill batchBill = individualBill.getBackwardReferenceBill();

        // Not all individual bills have batch bills (e.g., direct OPD bills)
        if (batchBill == null) {
            return;
        }

        // Only update balance for Credit payment method bills
        if (batchBill.getPaymentMethod() != PaymentMethod.Credit) {
            return;
        }

        // Validate numeric fields
        if (cancellationBill.getNetTotal() == 0.0) {
            throw new IllegalStateException("Cancellation bill net total is invalid");
        }

        // Refresh batch bill from database to ensure latest data and trigger optimistic locking
        batchBill = billFacade.find(batchBill.getId());

        if (batchBill == null) {
            throw new IllegalStateException("Batch bill not found in database");
        }

        // Calculate refund amount (always positive)
        double refundAmount = Math.abs(cancellationBill.getNetTotal());

        // Validate refund amount doesn't exceed original batch bill total
        if (refundAmount > batchBill.getNetTotal()) {
            throw new IllegalStateException(
                    String.format("CRITICAL: Refund amount (%.2f) exceeds batch bill total (%.2f). "
                            + "Batch Bill: %s, Individual Bill: %s",
                            refundAmount, batchBill.getNetTotal(),
                            batchBill.getInsId(), individualBill.getInsId())
            );
        }

        // Store old values for audit trail
        double oldBalance = batchBill.getBalance();
        double oldPaidAmount = batchBill.getPaidAmount();
        double oldRefundAmount = batchBill.getRefundAmount();

        // Update refundAmount - add the return amount
        batchBill.setRefundAmount(batchBill.getRefundAmount() + refundAmount);

        // Update paidAmount - deduct the return amount (only if payment exists)
        if (batchBill.getPaidAmount() > 0) {
            batchBill.setPaidAmount(Math.max(0d, batchBill.getPaidAmount() - refundAmount));
        }

        // Update balance (due amount) - deduct the return amount (only if balance > 0)
        if (batchBill.getBalance() > 0) {
            batchBill.setBalance(Math.max(0d, batchBill.getBalance() - refundAmount));
        }

        try {
            // Save the updated bill
            billFacade.edit(batchBill);
            // Create audit log entry after successful update (commented out - needs auditEventApplicationController)
            // TODO: Add audit trail logging when auditEventApplicationController is available
            /*
            auditEventApplicationController.logAuditEvent(
            "Individual Bill Cancellation - Batch Bill Balance Adjustment",
            String.format(
            "Batch Bill: %s | Individual Bill: %s | Cancellation Bill: %s | " +
            "Old Balance: %.2f → New Balance: %.2f | Refund: +%.2f | " +
            "Old Paid: %.2f → New Paid: %.2f | Old Refund: %.2f → New Refund: %.2f | " +
            "Adjusted By: %s",
            batchBill.getInsId(),
            individualBill.getInsId(),
            cancellationBill.getInsId(),
            oldBalance, batchBill.getBalance(),
            refundAmount,
            oldPaidAmount, batchBill.getPaidAmount(),
            oldRefundAmount, batchBill.getRefundAmount(),
            sessionController.getLoggedUser().getName()
            ),
            batchBill
            );
             */
            // Create audit log entry after successful update (commented out - needs auditEventApplicationController)
            // TODO: Add audit trail logging when auditEventApplicationController is available
            /*
            auditEventApplicationController.logAuditEvent(
                "Individual Bill Cancellation - Batch Bill Balance Adjustment",
                String.format(
                    "Batch Bill: %s | Individual Bill: %s | Cancellation Bill: %s | " +
                    "Old Balance: %.2f → New Balance: %.2f | Refund: +%.2f | " +
                    "Old Paid: %.2f → New Paid: %.2f | Old Refund: %.2f → New Refund: %.2f | " +
                    "Adjusted By: %s",
                    batchBill.getInsId(),
                    individualBill.getInsId(),
                    cancellationBill.getInsId(),
                    oldBalance, batchBill.getBalance(),
                    refundAmount,
                    oldPaidAmount, batchBill.getPaidAmount(),
                    oldRefundAmount, batchBill.getRefundAmount(),
                    sessionController.getLoggedUser().getName()
                ),
                batchBill
            );
             */

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error updating batch bill balance: " + e.getMessage());
            e.printStackTrace();
            // Don't re-throw to prevent cancellation from failing completely
            // The individual bill cancellation should still succeed
        }
    }

    // ======== Payment Method Data Picker Methods for Bill Cancellation ========
    /**
     * Copies payment method data from original bill's multiple payment method
     * component to the current bill's payment method data for cancellation
     * purposes.
     *
     * @param originalPm The original payment method component detail to copy
     * from
     */
    public void copyPaymentMethodData(ComponentDetail originalPm) {
        System.out.println("=== copyPaymentMethodData() CALLED ===");
        System.out.println("originalPm: " + (originalPm != null ? originalPm.getPaymentMethod() + " - " + originalPm.getTotalValue() : "null"));

        if (originalPm == null) {
            System.out.println("ERROR: originalPm is null!");
            JsfUtil.addErrorMessage("No payment method data to copy");
            return;
        }

        try {
            // Initialize paymentMethodData if null
            if (this.paymentMethodData == null) {
                this.paymentMethodData = new PaymentMethodData();
            }

            // Copy the payment method data from original component based on payment method type
            PaymentMethod pmType = originalPm.getPaymentMethod();

            switch (pmType) {
                case Card:
                    copyCardPaymentData(originalPm);
                    break;
                case ewallet:
                    copyEwalletPaymentData(originalPm);
                    break;
                case Cheque:
                    copyChequePaymentData(originalPm);
                    break;
                case Slip:
                    copySlipPaymentData(originalPm);
                    break;
                default:
                    // For other payment methods, copy basic data
                    copyBasicPaymentData(originalPm);
                    break;
            }

            System.out.println("copyPaymentMethodData: Successfully copied " + pmType + " payment data");
            JsfUtil.addSuccessMessage("Payment details copied from original bill");

        } catch (Exception e) {
            System.out.println("copyPaymentMethodData: ERROR - " + e.getMessage());
            JsfUtil.addErrorMessage("Error copying payment method data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Copy card payment data
     */
    private void copyCardPaymentData(ComponentDetail originalPm) {
        ComponentDetail creditCardDetail = this.paymentMethodData.getCreditCard();
        creditCardDetail.setTotalValue(originalPm.getTotalValue());
        creditCardDetail.setComment(originalPm.getComment());
        creditCardDetail.setNo(originalPm.getNo());
        creditCardDetail.setReferenceNo(originalPm.getReferenceNo());
        creditCardDetail.setDate(originalPm.getDate());
        creditCardDetail.setInstitution(originalPm.getInstitution());
    }

    /**
     * Copy ewallet payment data
     */
    private void copyEwalletPaymentData(ComponentDetail originalPm) {
        ComponentDetail ewalletDetail = this.paymentMethodData.getEwallet();
        ewalletDetail.setTotalValue(originalPm.getTotalValue());
        ewalletDetail.setComment(originalPm.getComment());
        ewalletDetail.setNo(originalPm.getNo());
        ewalletDetail.setReferenceNo(originalPm.getReferenceNo());
        ewalletDetail.setDate(originalPm.getDate());
        ewalletDetail.setInstitution(originalPm.getInstitution());
    }

    /**
     * Copy cheque payment data
     */
    private void copyChequePaymentData(ComponentDetail originalPm) {
        ComponentDetail chequeDetail = this.paymentMethodData.getCheque();
        chequeDetail.setTotalValue(originalPm.getTotalValue());
        chequeDetail.setComment(originalPm.getComment());
        chequeDetail.setNo(originalPm.getNo());
        chequeDetail.setReferenceNo(originalPm.getReferenceNo());
        chequeDetail.setDate(originalPm.getDate());
        chequeDetail.setInstitution(originalPm.getInstitution());
    }

    /**
     * Copy slip payment data
     */
    private void copySlipPaymentData(ComponentDetail originalPm) {
        ComponentDetail slipDetail = this.paymentMethodData.getSlip();
        slipDetail.setTotalValue(originalPm.getTotalValue());
        slipDetail.setComment(originalPm.getComment());
        slipDetail.setNo(originalPm.getNo());
        slipDetail.setReferenceNo(originalPm.getReferenceNo());
        slipDetail.setDate(originalPm.getDate());
        slipDetail.setInstitution(originalPm.getInstitution());
    }

    /**
     * Copy basic payment data for other payment methods
     */
    private void copyBasicPaymentData(ComponentDetail originalPm) {
        // This can be extended for other payment methods as needed
        // For now, just handle the basic fields
    }

    /**
     * Get list of Card payment methods from original bill payments
     *
     * @return
     */
    public List<ComponentDetail> getOriginalCardPayments() {
        List<ComponentDetail> cardPayments = getOriginalPaymentsByMethod(PaymentMethod.Card);
        System.out.println("=== getOriginalCardPayments DEBUG ===");
        System.out.println("Individual bill.paymentMethod: " + (bill != null ? bill.getPaymentMethod() : "bill is null"));
        System.out.println("Batch bill.paymentMethod: " + (bill != null && bill.getBackwardReferenceBill() != null ? bill.getBackwardReferenceBill().getPaymentMethod() : "no batch bill"));
        System.out.println("originalPaymentDetails size: " + (originalPaymentDetails != null ? originalPaymentDetails.size() : "null"));
        System.out.println("Card payments found: " + cardPayments.size());
        for (int i = 0; i < cardPayments.size(); i++) {
            ComponentDetail cd = cardPayments.get(i);
            System.out.println("  Card[" + i + "] Method: " + cd.getPaymentMethod() + ", Amount: " + cd.getTotalValue() + ", Ref: " + cd.getReferenceNo());
        }
        System.out.println("=== END getOriginalCardPayments DEBUG ===");
        return cardPayments;
    }

    /**
     * Get list of eWallet payment methods from original bill payments
     *
     * @return
     */
    public List<ComponentDetail> getOriginalEWalletPayments() {
        return getOriginalPaymentsByMethod(PaymentMethod.ewallet);
    }

    /**
     * Get list of Cheque payment methods from original bill payments
     *
     * @return
     */
    public List<ComponentDetail> getOriginalChequePayments() {
        return getOriginalPaymentsByMethod(PaymentMethod.Cheque);
    }

    /**
     * Get list of Slip payment methods from original bill payments
     *
     * @return
     */
    public List<ComponentDetail> getOriginalSlipPayments() {
        return getOriginalPaymentsByMethod(PaymentMethod.Slip);
    }

    /**
     * Get the original payment component details from stored payment details
     * This provides the same data as the "Original Payment Details" section
     *
     * @return
     */
    public List<ComponentDetail> getOriginalPaymentComponentDetails() {
        if (originalPaymentDetails != null) {
            return new ArrayList<>(originalPaymentDetails);
        }
        return new ArrayList<>();
    }

    /**
     * Helper method to get original payment methods by type from stored payment
     * details
     */
    private List<ComponentDetail> getOriginalPaymentsByMethod(PaymentMethod paymentMethod) {
        List<ComponentDetail> filteredPayments = new ArrayList<>();

        try {
            // Use stored original payment details loaded during navigation
            if (originalPaymentDetails != null) {
                for (ComponentDetail pm : originalPaymentDetails) {
                    if (pm != null && pm.getPaymentMethod() == paymentMethod) {
                        filteredPayments.add(pm);
                    }
                }
            }

            System.out.println("getOriginalPaymentsByMethod(" + paymentMethod + "): Found " + filteredPayments.size() + " payments");
        } catch (Exception e) {
            System.out.println("Error getting original payments by method: " + e.getMessage());
            e.printStackTrace();
        }

        return filteredPayments;
    }

    /**
     * Determine the actual payment method from Payment entity fields
     * For multiple payment bills, all Payment records have paymentMethod = MultiplePaymentMethods
     * The actual type is determined by which fields are populated
     */
    private PaymentMethod determineActualPaymentMethod(Payment payment) {
        if (payment.getPaymentMethod() != PaymentMethod.MultiplePaymentMethods) {
            // Single payment method bills - return the actual payment method
            return payment.getPaymentMethod();
        }

        // For multiple payment method bills, determine by populated fields

        // Cheque: has cheque-specific fields
        if (payment.getChequeDate() != null ||
            (payment.getChequeRefNo() != null && !payment.getChequeRefNo().trim().isEmpty())) {
            return PaymentMethod.Cheque;
        }

        // Card: has referenceNo and bank
        if ((payment.getReferenceNo() != null && !payment.getReferenceNo().trim().isEmpty()) &&
            payment.getBank() != null) {
            return PaymentMethod.Card;
        }

        // eWallet or Slip: has referenceNo and institution (but not bank)
        if ((payment.getReferenceNo() != null && !payment.getReferenceNo().trim().isEmpty()) &&
            payment.getInstitution() != null && payment.getBank() == null) {
            // Could be eWallet or Slip - for now default to eWallet
            // TODO: Add more specific logic to differentiate eWallet vs Slip if needed
            return PaymentMethod.ewallet;
        }

        // Staff or StaffWelfare: check for toStaff field
        if (payment.getToStaff() != null) {
            // TODO: Add logic to differentiate Staff vs Staff_Welfare if needed
            return PaymentMethod.Staff;
        }

        // Credit: check for credit company or policy number
        if (payment.getCreditCompany() != null ||
            (payment.getPolicyNo() != null && !payment.getPolicyNo().trim().isEmpty())) {
            return PaymentMethod.Credit;
        }

        // Default to Cash if no specific fields are populated
        return PaymentMethod.Cash;
    }

    /**
     * Load and store original payment details during navigation from
     * bill_reprint.xhtml This ensures payment details are available throughout
     * the cancellation process
     */
    public void loadOriginalPaymentDetails() {
        System.out.println("=== loadOriginalPaymentDetails() CALLED ===");
        originalPaymentDetails = new ArrayList<>();

        try {
            if (this.bill != null) {
                List<Payment> payments;

                // Load payments from the batch bill (like navigateToCancelOpdBill does)
                Bill batchBill = this.bill.getBackwardReferenceBill();
                if (batchBill != null) {
                    payments = fetchBillPayments(batchBill);
                    System.out.println("loadOriginalPaymentDetails: Using batch bill ID " + batchBill.getId());
                    System.out.println("loadOriginalPaymentDetails: Found " + payments.size() + " payments");
                } else {
                    // Fallback to individual bill if no batch bill
                    payments = fetchBillPayments(this.bill);
                    System.out.println("loadOriginalPaymentDetails: Using individual bill ID " + this.bill.getId() + " (no batch bill)");
                    System.out.println("loadOriginalPaymentDetails: Found " + payments.size() + " payments");
                }

                // Convert Payment entities to ComponentDetail objects for UI compatibility
                for (Payment payment : payments) {
                    if (payment.getPaidValue() > 0) {
                        ComponentDetail cd = new ComponentDetail();

                        // Determine the actual payment method for multiple payment bills
                        PaymentMethod actualPaymentMethod = determineActualPaymentMethod(payment);
                        cd.setPaymentMethod(actualPaymentMethod);

                        cd.setTotalValue(payment.getPaidValue());
                        cd.setComment(payment.getComments());
                        cd.setNo(payment.getReferenceNo());
                        cd.setReferenceNo(payment.getReferenceNo());
                        cd.setDate(payment.getPaymentDate());
                        cd.setInstitution(payment.getBank());

                        originalPaymentDetails.add(cd);
                        System.out.println("  Stored: " + actualPaymentMethod
                                + " (was: " + payment.getPaymentMethod() + ")"
                                + ", Amount: " + payment.getPaidValue()
                                + ", Ref: " + payment.getReferenceNo());
                    }
                }

                // Create PaymentMethodData structure for compatibility with existing components
                createOriginalPaymentMethodData();

                System.out.println("loadOriginalPaymentDetails: Total stored payment details: " + originalPaymentDetails.size());
            }
        } catch (Exception e) {
            System.out.println("Error loading original payment details: " + e.getMessage());
            e.printStackTrace();
            JsfUtil.addErrorMessage("Error loading original payment details: " + e.getMessage());
        }
    }

    /**
     * Create PaymentMethodData structure from stored payment details
     */
    private void createOriginalPaymentMethodData() {
        if (originalPaymentDetails != null && !originalPaymentDetails.isEmpty()) {
            originalPaymentMethodData = new PaymentMethodData();
            ComponentDetail multipleComponent = originalPaymentMethodData.getPaymentMethodMultiple();
            multipleComponent.setMultiplePaymentMethodComponentDetails(new ArrayList<>(originalPaymentDetails));
        }
    }

    /**
     * Check if original bill has any payment methods of the specified type
     *
     * @param paymentMethod
     * @return
     */
    public boolean hasOriginalPaymentMethod(PaymentMethod paymentMethod) {
        return !getOriginalPaymentsByMethod(paymentMethod).isEmpty();
    }

    // ======== Getter and Setter Methods for Original Payment Details ========
    /**
     * Get stored original payment details loaded during navigation
     *
     * @return
     */
    public List<ComponentDetail> getOriginalPaymentDetails() {
        return originalPaymentDetails;
    }

    /**
     * Set original payment details (used for testing or manual setting)
     *
     * @param originalPaymentDetails
     */
    public void setOriginalPaymentDetails(List<ComponentDetail> originalPaymentDetails) {
        this.originalPaymentDetails = originalPaymentDetails;
        // Recreate PaymentMethodData structure when details are set
        createOriginalPaymentMethodData();
    }

    /**
     * Get original payment method data structure
     *
     * @return
     */
    public PaymentMethodData getOriginalPaymentMethodData() {
        return originalPaymentMethodData;
    }

    /**
     * Set original payment method data structure
     *
     * @param originalPaymentMethodData
     */
    public void setOriginalPaymentMethodData(PaymentMethodData originalPaymentMethodData) {
        this.originalPaymentMethodData = originalPaymentMethodData;
    }

    public List<Payment> getBillPayments() {
        return billPayments;
    }

    public void setBillPayments(List<Payment> billPayments) {
        this.billPayments = billPayments;
    }

    
    
}
