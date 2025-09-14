/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.cashTransaction.FinancialTransactionController;
import com.divudi.bean.cashTransaction.PaymentController;
import com.divudi.bean.channel.ChannelSearchController;
import com.divudi.bean.collectingCentre.CollectingCentreBillController;
import com.divudi.bean.lab.LabTestHistoryController;
import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.bean.pharmacy.PharmacyPreSettleController;
import com.divudi.bean.pharmacy.GrnReturnApprovalController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillSummery;
import com.divudi.core.data.BillType;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.HistoryType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.data.reports.PharmacyReports;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.EjbApplication;
import com.divudi.ejb.PharmacyBean;

import com.divudi.core.entity.AgentHistory;
import com.divudi.core.entity.AuditEvent;
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
import com.divudi.core.entity.cashTransaction.CashTransaction;
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
import com.divudi.bean.opd.OpdBillController;
import com.divudi.bean.pharmacy.BhtIssueReturnController;
import com.divudi.bean.pharmacy.GrnReturnWithCostingController;
import com.divudi.bean.pharmacy.GoodsReturnController;
import com.divudi.bean.pharmacy.IssueReturnController;
import com.divudi.bean.pharmacy.PharmacyBillSearch;
import com.divudi.bean.pharmacy.PharmacyIssueController;
import com.divudi.bean.pharmacy.PharmacyPurchaseController;
import com.divudi.bean.pharmacy.PharmacyReturnwithouttresing;
import com.divudi.bean.pharmacy.PharmacySaleBhtController;
import com.divudi.bean.pharmacy.PurchaseReturnController;
import com.divudi.bean.pharmacy.TransferIssueController;
import com.divudi.bean.pharmacy.TransferReceiveController;
import com.divudi.bean.pharmacy.TransferRequestController;
import com.divudi.core.data.BillTypeAtomic;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.divudi.core.data.BillTypeAtomic.PHARMACY_ISSUE_CANCELLED;

import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.facade.FeeFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.StaffFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.light.common.BillLight;
import com.divudi.service.BillService;
import com.divudi.service.PaymentService;
import com.divudi.service.ProfessionalPaymentService;
import com.divudi.service.StaffService;

import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import com.divudi.bean.pharmacy.DirectPurchaseReturnController;
import com.divudi.bean.pharmacy.PharmacyRequestForBhtController;
import com.divudi.bean.pharmacy.PharmacySaleController;
import com.divudi.bean.pharmacy.SaleReturnController;
import static com.divudi.core.data.BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER;
import com.divudi.core.facade.PatientInvestigationFacade;

import org.primefaces.event.RowEditEvent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import java.nio.charset.StandardCharsets;

/**
 * @author Buddhika
 */
@Named
@SessionScoped
public class BillSearch implements Serializable {

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
    /**
     * Controllers
     */
    @Inject
    BillPackageController billPackageController;
    @Inject
    BillItemController billItemController;
    @Inject
    BillFeeController billFeeController;
    @Inject
    PaymentController paymentController;
    @Inject
    private CollectingCentreBillController collectingCentreBillController;
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
    PharmacyBillSearch pharmacyBillSearch;
    @Inject
    PatientDepositController patientDepositController;
    @Inject
    OpdBillController opdBillController;
    @Inject
    SearchController searchController;
    @Inject
    CashRecieveBillController cashRecieveBillController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    ChannelSearchController channelSearchController;
    @Inject
    AgentAndCcApplicationController collectingCentreApplicationController;
    @Inject
    DrawerController drawerController;
    @Inject
    FinancialTransactionController financialTransactionController;
    @Inject
    IssueReturnController issueReturnController;
    @Inject
    PharmacyIssueController pharmacyIssueController;
    @Inject
    GrnReturnApprovalController grnReturnApprovalController;
    @Inject
    TransferIssueController transferIssueController;
    @Inject
    TransferReceiveController transferReceiveController;
    @Inject
    PharmacySaleBhtController pharmacySaleBhtController;
    @Inject
    BhtIssueReturnController bhtIssueReturnController;
    @Inject
    PharmacyPurchaseController pharmacyPurchaseController;
    @Inject
    PurchaseReturnController purchaseReturnController;
    @Inject
    DirectPurchaseReturnController directPurchaseReturnController;
    @Inject
    PharmacySaleController pharmacySaleController;

    @Inject
    GrnReturnWithCostingController grnReturnWithCostingController;
    @Inject
    PharmacyReturnwithouttresing pharmacyReturnwithouttresing;
    @Inject
    GoodsReturnController goodsReturnController;
    @Inject
    private ReportTimerController reportTimerController;
    @Inject
    TransferRequestController transferRequestController;
    @Inject
    SaleReturnController saleReturnController;
    @Inject
    PharmacyRequestForBhtController pharmacyRequestForBhtController;
    @Inject
    LabTestHistoryController labTestHistoryController;
    /**
     * Class Variables
     */
    private Bill billForCancel;
    boolean showAllBills;
    private boolean printPreview = false;
    private double refundAmount;
    private String txtSearch;
    private Bill bill;
    private BillLight billLight;
    private Bill printingBill;
    private PaymentMethod paymentMethod;
    private List<PaymentMethod> paymentMethods;
    private RefundBill billForRefund;
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
    private List<BillItem> billItems;
    private List<Payment> billPayments;
    private List<BillComponent> billComponents;
    private List<BillFee> billFees;
    private List<BillItem> tempbillItems;
    private LazyDataModel<BillItem> searchBillItems;
    private LazyDataModel<Bill> lazyBills;
    List<BillSummery> billSummeries;
    private BillSummery billSummery;
    /// /////////////////
    /// ////////////////
    private SearchKeyword searchKeyword;
    private Institution creditCompany;
    private PatientInvestigation patientInvestigation;
    private Institution institution;
    private Institution collectingCenter;
    private Department department;
    private double refundTotal = 0;
    private double refundDiscount = 0;
    private double refundMargin = 0;
    private double refundVat = 0;
    private double refundVatPlusTotal = 0;
    String encryptedPatientReportId;
    String encryptedExpiary;

    private double cashTotal;
    private double slipTotal;
    private double creditTotal;
    private double creditCardTotal;
    private double multiplePaymentsTotal;
    private double patientDepositsTotal;
    private OverallSummary overallSummary;

    private Bill currentRefundBill;

    private boolean opdBillCancellationSameDay = false;
    private boolean opdBillRefundAllowedSameDay = false;
    private final AtomicBoolean ccBillCancellingStarted = new AtomicBoolean(false);

    //Edit Bill details
    private Doctor referredBy;

    private Bill viewingBill;
    private List<Bill> viewingIndividualBillsOfBatchBill;
    private List<Bill> viewingRefundBills;
    private List<Bill> viewingReferanceBills;
    private List<BillItem> viewingBillItems;
    private List<PharmaceuticalBillItem> viewingPharmaceuticalBillItems;
    private List<PatientInvestigation> viewingPatientInvestigations;
    private List<BillFee> viewingBillFees;
    private List<BillComponent> viewingBillComponents;
    private List<Payment> viewingBillPayments;
    private boolean duplicate;

    private Payment payment;
    private int billItemSize;

    public String navigateToBillPaymentOpdBill() {
        return "bill_payment_opd?faces-redirect=true";
    }

    public String navigateToCancelBillView() {
        if (bill != null) {
            JsfUtil.addErrorMessage("Bill is Missing..");
            return "";
        }
        printPreview = true;
        duplicate = true;
        return "/opd/bill_cancel?faces-redirect=true";
    }

    public String navigateToInwardSearchService() {
        return "/inward/inward_reprint_bill_service?faces-redirect=true";
    }

    public List<Payment> fetchBillPayments(Bill bill) {
        return billService.fetchBillPayments(bill);
    }

    public List<BillItem> groupBillItemByNameFromBill(Bill b) {
        return groupBillItemByName(billService.fetchBillItems(b));
    }

    public List<BillItem> groupBillItemByName(List<BillItem> billItems) {
        Map<String, BillItem> groupedMap = new HashMap<>();

        for (BillItem item : billItems) {
            if (item.getItem() == null || item.getItem().getName() == null) {
                continue;
            }

            String name = item.getItem().getName();

            if (!groupedMap.containsKey(name)) {

                BillItem grouped = new BillItem();
                grouped.setItem(item.getItem());
                grouped.setQty(item.getQty());
                grouped.setGrossValue(item.getGrossValue());
                grouped.setNetValue(item.getNetValue());
                groupedMap.put(name, grouped);
            } else {

                BillItem grouped = groupedMap.get(name);
                grouped.setQty(grouped.getQty() + item.getQty());
                grouped.setGrossValue(grouped.getGrossValue() + item.getGrossValue());
                grouped.setNetValue(grouped.getNetValue() + item.getNetValue());
            }
        }

        billItemSize = new ArrayList<>(groupedMap.values()).size();

        return new ArrayList<>(groupedMap.values());
    }

    public void fillBillFees() {
        for (BillItem bi : getRefundingBill().getBillItems()) {
            for (BillFee bfee : bi.getBillFees()) {
                bfee.setFeeValue(bfee.getReferenceBillFee().getFeeValue());
            }
        }
    }

    public void editBillDetails() {
        Bill editedBill = bill;
        if (bill == null) {
            JsfUtil.addErrorMessage("Bill Error !");
            return;
        }
        if (referredBy == null) {
            JsfUtil.addErrorMessage("Pleace Select Reffering Doctor !");
            return;
        }
        editedBill.setReferredBy(referredBy);
        if (bill.getId() == null) {
            billFacade.create(editedBill);
        }
        billFacade.edit(editedBill);
        JsfUtil.addSuccessMessage("Saved");
        referredBy = null;
    }

    public void preparePatientReportByIdForRequests() {
        bill = null;
        if (encryptedPatientReportId == null) {
            return;
        }
        if (encryptedExpiary != null) {
            Date expiaryDate;
            try {
                String ed = encryptedExpiary;
                ed = securityController.decrypt(ed);
                if (ed == null) {
                    return;
                }
                expiaryDate = new SimpleDateFormat("ddMMMMyyyyhhmmss").parse(ed);
            } catch (ParseException ex) {
                return;
            }
            if (expiaryDate.before(new Date())) {
                return;
            }
        }
        String idStr = getSecurityController().decrypt(encryptedPatientReportId);
        Long id = 0l;
        try {
            id = Long.parseLong(idStr);
        } catch (Exception e) {
            return;
        }
        Bill pr = getBillFacade().find(id);
        if (pr == null) {
            return;
        }
        bill = pr;
    }

    public void fillBillTypeSummery() {
        Map m = new HashMap();
        String j;
        if (billClassType == null) {
            j = "select new com.divudi.core.data.BillSummery(b.paymentMethod, sum(b.total), sum(b.discount), sum(b.netTotal), sum(b.vat), count(b), b.billType) "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        } else {
            j = "select new com.divudi.core.data.BillSummery(b.paymentMethod, b.billClassType, sum(b.total), sum(b.discount), sum(b.netTotal), sum(b.vat), count(b), b.billType) "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        }

        if (department == null) {
            j += " and b.institution=:ins ";
            m.put("ins", sessionController.getLoggedUser().getInstitution());
        } else {
            j += " and b.department=:dep ";
            m.put("dep", department);
        }
        if (user != null) {
            j += " and b.creater=:wu ";
            m.put("wu", user);
        }
        if (billType != null) {
            j += " and b.billType=:bt ";
            m.put("bt", billType);
        }
        if (billClassType != null) {
            j += " and b.billClassType=:bct ";
            m.put("bct", billClassType);
        }

        if (billClassType == null) {
            j += " group by b.paymentMethod,  b.billType";
        } else {
            j += " group by b.paymentMethod, b.billClassType, b.billType";
        }
        Boolean bf = false;
        if (bf) {
            Bill b = new Bill();
            b.getPaymentMethod();
            b.getTotal();
            b.getDiscount();
            b.getNetTotal();
            b.getVat();
            b.getBillType();
            b.getBillTime();
            b.getInstitution();
            b.getCreater();
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        List<Object> objs = billFacade.findObjectByJpql(j, m, TemporalType.TIMESTAMP);
        billSummeries = new ArrayList<>();
        Long i = 1l;
        for (Object o : objs) {
            BillSummery tbs = (BillSummery) o;
            tbs.setKey(i);
            billSummeries.add(tbs);
            i++;
        }
    }

    public void processCashierBillSummery() {
        //For Auditing Purposes
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();
        String url = request.getRequestURL().toString();
        String ipAddress = request.getRemoteAddr();
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }
        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillTransactionTypeSummery()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Map m = new HashMap();
        String j;
        if (billClassType == null) {
            j = "select new com.divudi.core.data.BillSummery(b.paymentMethod, sum(b.total), sum(b.discount), sum(b.netTotal), sum(b.vat), count(b), b.billType) "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        } else {
            j = "select new com.divudi.core.data.BillSummery(b.paymentMethod, b.billClassType, sum(b.total), sum(b.discount), sum(b.netTotal), sum(b.vat), count(b), b.billType) "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        }

        if (institution != null) {
            j += " and b.institution=:ins ";
            m.put("ins", institution);
        }

        if (department != null) {
            j += " and b.department=:dep ";
            m.put("dep", department);
        }
        if (user != null) {
            j += " and b.creater=:wu ";
            m.put("wu", user);
        }
        if (billType != null) {
            j += " and b.billType=:bt ";
            m.put("bt", billType);
        }
        if (billClassType != null) {
            j += " and b.billClassType=:bct ";
            m.put("bct", billClassType);
        }

        if (billClassType == null) {
            j += " group by b.paymentMethod,  b.billType";
        } else {
            j += " group by b.paymentMethod, b.billClassType, b.billType";
        }
        Boolean bf = false;
        if (bf) {
            Bill b = new Bill();
            b.getPaymentMethod();
            b.getTotal();
            b.getDiscount();
            b.getNetTotal();
            b.getVat();
            b.getBillType();
            b.getBillTime();
            b.getInstitution();
            b.getCreater();
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        List<Object> objs = billFacade.findObjectByJpql(j, m, TemporalType.TIMESTAMP);
        billSummeries = new ArrayList<>();
        Long i = 1l;
        for (Object o : objs) {
            BillSummery tbs = (BillSummery) o;
            tbs.setKey(i);
            billSummeries.add(tbs);
            i++;
        }

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public void processCashierPharmacySaleBillSummery() {
        reportTimerController.trackReportExecution(() -> {
            //For Auditing Purposes
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
            ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();
            String url = request.getRequestURL().toString();
            String ipAddress = request.getRemoteAddr();
            AuditEvent auditEvent = new AuditEvent();
            auditEvent.setEventStatus("Started");
            long duration;
            Date startTime = new Date();
            auditEvent.setEventDataTime(startTime);
            if (sessionController != null && sessionController.getDepartment() != null) {
                auditEvent.setDepartmentId(sessionController.getDepartment().getId());
            }
            if (sessionController != null && sessionController.getInstitution() != null) {
                auditEvent.setInstitutionId(sessionController.getInstitution().getId());
            }
            if (sessionController != null && sessionController.getLoggedUser() != null) {
                auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
            }
            auditEvent.setUrl(url);
            auditEvent.setIpAddress(ipAddress);
            auditEvent.setEventTrigger("fillTransactionTypeSummery()");
            auditEventApplicationController.logAuditEvent(auditEvent);

            Map m = new HashMap();
            String j;
            j = "select new com.divudi.core.data.BillSummery(b.paymentMethod, b.billClassType, sum(b.total), sum(b.discount), sum(b.netTotal), sum(b.vat), count(b), b.billTypeAtomic, b.billType, b.creater) "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";

            if (institution != null) {
                j += " and b.institution=:ins ";
                m.put("ins", institution);
            }

            if (department != null) {
                j += " and b.department=:dep ";
                m.put("dep", department);
            }
            if (user != null) {
                j += " and b.creater=:wu ";
                m.put("wu", user);
            }
            List<BillType> billTypes = new ArrayList<>();
            billTypes.add(BillType.PharmacySale);
            billTypes.add(BillType.PharmacySaleWithoutStock);
            billTypes.add(BillType.PharmacyWholeSale);
            j += " and b.billType in :bts ";
            m.put("bts", billTypes);

            j += " group by b.paymentMethod, b.billClassType, b.billType, b.creater, b.billTypeAtomic";

            Boolean bf = false;
            if (bf) {
                Bill b = new Bill();
                b.getPaymentMethod();
                b.getTotal();
                b.getDiscount();
                b.getNetTotal();
                b.getVat();
                b.getBillType();
                b.getBillTime();
                b.getInstitution();
                b.getCreater();
            }

            m.put("fd", fromDate);
            m.put("td", toDate);

            List<Object> objs = billFacade.findObjectByJpql(j, m, TemporalType.TIMESTAMP);
            billSummeries = new ArrayList<>();
            Long i = 1l;
            for (Object o : objs) {
                BillSummery tbs = (BillSummery) o;
                tbs.setKey(i);
                billSummeries.add(tbs);
                i++;
            }
            calculateTotalForBillSummaries();

            Date endTime = new Date();
            duration = endTime.getTime() - startTime.getTime();
            auditEvent.setEventDuration(duration);
            auditEvent.setEventStatus("Completed");
            auditEventApplicationController.logAuditEvent(auditEvent);
        }, PharmacyReports.PHARMACY_SALE_SUMMARY, sessionController.getLoggedUser());
    }

    public void processCashierPharmacySaleBillTotalDateWise() {
        reportTimerController.trackReportExecution(() -> {
            //For Auditing Purposes

            Map m = new HashMap();
            String j;
            j = "select new com.divudi.core.data.BillSummery(Function('DATE',(b.createdAt)),sum(b.netTotal), count(b)) "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";

            if (institution != null) {
                j += " and b.institution=:ins ";
                m.put("ins", institution);
            }

            if (department != null) {
                j += " and b.department=:dep ";
                m.put("dep", department);
            }

            j += " group by Function('DATE',(b.createdAt))";

            m.put("fd", fromDate);
            m.put("td", toDate);

            List<Object> objs = billFacade.findObjectByJpql(j, m, TemporalType.TIMESTAMP);
            billSummeries = new ArrayList<>();
            Long i = 1l;
            for (Object o : objs) {
                BillSummery tbs = (BillSummery) o;
                tbs.setKey(i);
                billSummeries.add(tbs);
                i++;
            }
        }, PharmacyReports.PHARMACY_SALE_SUMMARY_DATE, sessionController.getLoggedUser());
    }

    public void fillCashierDetails() {

        //For Auditing Purposes
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();
        String url = request.getRequestURL().toString();
        String ipAddress = request.getRemoteAddr();
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }
        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillTransactionTypeSummery()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.OpdBill);
        bts.add(BillType.PharmacySale);
        bts.add(BillType.PharmacyWholeSale);
        bts.add(BillType.InwardPaymentBill);
        bts.add(BillType.CollectingCentrePaymentReceiveBill);
        bts.add(BillType.PaymentBill);
        bts.add(BillType.PatientPaymentReceiveBill);
        bts.add(BillType.CollectingCentreBill);
        bts.add(BillType.PaymentBill);
        bts.add(BillType.ChannelCash);
        bts.add(BillType.ChannelPaid);
        bts.add(BillType.ChannelAgent);
        bts.add(BillType.ChannelProPayment);
        bts.add(BillType.ChannelAgencyCommission);
        bts.add(BillType.PettyCash);

        billSummeries = generateBillSummaries(institution, department, user, bts, billClassType, fromDate, toDate);

        overallSummary = aggregateBillSummaries(billSummeries);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public void fillCashierSummery() {
        //For Auditing Purposes
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();
        String url = request.getRequestURL().toString();
        String ipAddress = request.getRemoteAddr();
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }
        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillTransactionTypeSummery()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.OpdBill);
        bts.add(BillType.PharmacySale);
        bts.add(BillType.PharmacyWholeSale);
        bts.add(BillType.InwardPaymentBill);
        bts.add(BillType.CollectingCentrePaymentReceiveBill);
        bts.add(BillType.PaymentBill);
        bts.add(BillType.PatientPaymentReceiveBill);
        bts.add(BillType.CollectingCentreBill);
        bts.add(BillType.PaymentBill);
        bts.add(BillType.ChannelCash);
        bts.add(BillType.ChannelPaid);
        bts.add(BillType.ChannelAgent);
        bts.add(BillType.ChannelProPayment);
        bts.add(BillType.ChannelAgencyCommission);
        bts.add(BillType.PettyCash);

        billSummeries = generateBillSummaries(institution, department, user, bts, billClassType, fromDate, toDate);

        overallSummary = aggregateBillSummaries(billSummeries);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public List<BillSummery> generateBillSummaries(Institution ins, Department dep, WebUser u, List<BillType> bts, BillClassType bct, Date fd, Date td) {
        Map<String, Object> parameters = new HashMap<>();
        StringBuilder queryString = new StringBuilder("select new com.divudi.core.data.BillSummery(b.paymentMethod, ");
//        if (bct != null) {
//            queryString.append("b.billClassType, ");
//        }
        queryString.append("b.billClassType, ");
        queryString.append("sum(b.total), sum(b.discount), sum(b.netTotal), sum(b.vat), count(b), b.billType) from Bill b where b.retired=false and b.billTime between :fd and :td");
        parameters.put("fd", fd);
        parameters.put("td", td);
        if (ins != null) {
            queryString.append(" and b.institution=:ins");
            parameters.put("ins", ins);
        }
        if (dep != null) {
            queryString.append(" and b.department=:dep");
            parameters.put("dep", dep);
        }
        if (u != null) {
            queryString.append(" and b.creater=:wu");
            parameters.put("wu", u);
        }
        if (bts != null) {
            queryString.append(" and b.billType in :bts");
            parameters.put("bts", bts);
        }
        if (bct != null) {
            queryString.append(" and b.billClassType=:bct");
            parameters.put("bct", bct);
        }

        queryString.append(" group by b.paymentMethod, b.billClassType, b.billType");

        // queryString.append(bct == null ? " group by b.paymentMethod, b.billType" : " group by b.paymentMethod, b.billClassType, b.billType");
        List<BillSummery> bss = (List<BillSummery>) billFacade.findLightsByJpql(queryString.toString(), parameters, TemporalType.TIMESTAMP);
        if (bss == null || bss.isEmpty()) {
            return new ArrayList<>();
        }
        long key = 1;
        for (BillSummery result : bss) {

            result.setKey(key++);
        }
        return bss;
    }

    public String listBillsFromBillTypeSummery() {
        if (billSummery == null) {
            JsfUtil.addErrorMessage("No Summary Selected");
            return "";
        }
        String directTo;
        Map m = new HashMap();
        String j;

        BillClassType filteredBillClassType = billSummery.getBillClassType();
        BillType filteredBillType = billSummery.getBillType();
        PaymentMethod filteredPaymentMethod = billSummery.getPaymentMethod();

        if (filteredBillClassType == null) {
            j = "select b "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        } else {
            j = "select b "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        }

        if (department != null) {
            j += " and b.department=:dep ";
            m.put("dep", department);
        }
        if (institution != null) {
            j += " and b.institution=:ins ";
            m.put("ins", institution);
        }
        if (user != null) {
            j += " and b.creater=:wu ";
            m.put("wu", user);
        }

        if (filteredPaymentMethod != null) {
            j += " and b.paymentMethod=:pm ";
            m.put("pm", filteredPaymentMethod);
        }

        if (filteredBillType != null) {
            j += " and b.billType=:bt ";
            m.put("bt", filteredBillType);
        }
        if (filteredBillClassType != null) {
            j += " and b.billClassType=:bct ";
            m.put("bct", filteredBillClassType);
        }
        m.put("fd", fromDate);
        m.put("td", toDate);

        bills = billFacade.findByJpql(j, m, TemporalType.TIMESTAMP);

        if (bills != null) {
        } else {
        }

        if (filteredBillClassType == BillClassType.CancelledBill || filteredBillClassType == BillClassType.RefundBill) {
            directTo = "/reportIncome/bill_list_cancelled";
        } else {
            directTo = "/reportIncome/bill_list";
        }

        return directTo;
    }

    public String listBillsFromBillTransactionTypeSummery() {
        if (billSummery == null) {
            JsfUtil.addErrorMessage("No Summary Selected");
            return "";
        }
        String directTo;
        Map m = new HashMap();
        String j;

        BillClassType tmpBillClassType = billSummery.getBillClassType();
        BillType tmpBllType = billSummery.getBillType();

        if (tmpBillClassType == null) {
            j = "select b "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        } else {
            j = "select b "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        }

        if (institution != null) {
            j += " and b.institution=:ins ";
            m.put("ins", institution);
        }

        if (department != null) {
            j += " and b.department=:dep ";
            m.put("dep", department);
        }

        if (user != null) {
            j += " and b.creater=:wu ";
            m.put("wu", user);
        }
        if (tmpBllType != null) {
            j += " and b.billType=:bt ";
            m.put("bt", tmpBllType);
        }
        if (tmpBillClassType != null) {
            j += " and b.billClassType=:bct ";
            m.put("bct", tmpBillClassType);
        }
        m.put("fd", fromDate);
        m.put("td", toDate);

        bills = billFacade.findByJpql(j, m, TemporalType.TIMESTAMP);

        if (tmpBillClassType == BillClassType.CancelledBill || tmpBillClassType == BillClassType.RefundBill) {
            directTo = "/reportInstitution/bill_list_cancelled";
        } else {
            directTo = "/reportInstitution/bill_list";
        }
        return directTo;
    }

    public void fillBillFeeTypeSummery() {
        Map m = new HashMap();
        String j;
        if (billClassType == null) {
            j = "select new com.divudi.core.data.BillSummery(b.paymentMethod, sum(bf.feeGrossValue), sum(bf.feeDiscount), sum(bf.feeValue), sum(bf.feeVat), count(b), b.billType) "
                    + " from BillFee bf inner join bf.bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        } else {
            j = "select new com.divudi.core.data.BillSummery(b.paymentMethod, b.billClassType, sum(bf.feeGrossValue), sum(bf.feeDiscount), sum(bf.feeValue), sum(bf.feeVat), count(b), b.billType) "
                    + " from BillFee bf inner join bf.bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        }

        if (department == null) {
            j += " and b.institution=:ins ";
            m.put("ins", sessionController.getLoggedUser().getInstitution());
        } else {
            j += " and b.toDepartment=:dep ";
            m.put("dep", department);
        }
        if (user != null) {
            j += " and b.creater=:wu ";
            m.put("wu", user);
        }
        if (billType != null) {
            j += " and b.billType=:bt ";
            m.put("bt", billType);
        }
        if (billClassType != null) {
            j += " and b.billClassType=:bct ";
            m.put("bct", billClassType);
        }

        if (billClassType == null) {
            j += " group by b.paymentMethod,  b.billType";
        } else {
            j += " group by b.paymentMethod, b.billClassType, b.billType";
        }
        Boolean bf = false;
        if (bf) {
            Bill b = new Bill();
            b.getPaymentMethod();
            b.getTotal();
            b.getDiscount();
            b.getNetTotal();
            b.getVat();
            b.getBillType();
            b.getBillTime();
            b.getInstitution();
            b.getCreater();
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        List<Object> objs = billFacade.findObjectByJpql(j, m, TemporalType.TIMESTAMP);
        billSummeries = new ArrayList<>();
        Long i = 1l;
        for (Object o : objs) {
            BillSummery tbs = (BillSummery) o;
            tbs.setKey(i);
            billSummeries.add(tbs);
            i++;
        }
    }

    public void clearSearchFIelds() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("clearSearchFIelds()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        auditEventApplicationController.logAuditEvent(auditEvent);
        department = null;
        fromDate = null;
        toDate = null;
        institution = null;
        user = null;
        billType = null;
        billClassType = null;

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
    }

    public BillSearch() {
    }

    public void updateBill() {

        if (bill.getPatientEncounter() == null) {
            bill.setCreditCompany(creditCompany);
        } else {
            bill.getPatientEncounter().setCreditCompany(creditCompany);
        }
        bill.setEditedAt(new Date());
        bill.setEditor(sessionController.getLoggedUser());
        billFacade.edit(bill);
        JsfUtil.addSuccessMessage("Bill Upadted");

    }

    private double roundOff(double d, int position) {
        DecimalFormat newFormat = new DecimalFormat("#.##");
        return Double.valueOf(newFormat.format(d));
    }

    public void updateValue() {

        // Round off and persist updates to BillFee objects
        for (BillFee bf : billFeesList) {
            bf.setFeeGrossValue(roundOff(bf.getFeeGrossValue(), 2));
            bf.setFeeDiscount(roundOff(bf.getFeeDiscount(), 2));
            bf.setFeeValue(roundOff(bf.getFeeValue(), 2));
            billFeeFacade.edit(bf);
        }

        // Update BillItem calculations
        for (BillItem bt : billItemList) {

            // feeGrossValue
            String jpql = "select sum(b.feeGrossValue) "
                    + " from BillFee b "
                    + " where b.retired = :ret "
                    + " and b.billItem.id = :bid";
            Map<String, Object> params = new HashMap<>();
            params.put("ret", false);
            params.put("bid", bt.getId());
            bt.setGrossValue(billItemFacade.findDoubleByJpql(jpql, params));

            // feeDiscount
            jpql = "select sum(b.feeDiscount) "
                    + " from BillFee b "
                    + " where b.retired = :ret "
                    + " and b.billItem.id = :bid";
            params = new HashMap<>();
            params.put("ret", false);
            params.put("bid", bt.getId());
            bt.setDiscount(billItemFacade.findDoubleByJpql(jpql, params));

            // feeValue
            jpql = "select sum(b.feeValue) "
                    + " from BillFee b "
                    + " where b.retired = :ret "
                    + " and b.billItem.id = :bid";
            params = new HashMap<>();
            params.put("ret", false);
            params.put("bid", bt.getId());
            bt.setNetValue(billItemFacade.findDoubleByJpql(jpql, params));
        }

        // Summaries for Bill object
        String jpql = "select sum(b.grossValue) "
                + " from BillItem b "
                + " where b.retired = :ret "
                + " and b.bill.id = :bid";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("bid", bill.getId());
        bill.setTotal(billItemFacade.findDoubleByJpql(jpql, params));

        jpql = "select sum(b.discount) "
                + " from BillItem b "
                + " where b.retired = :ret "
                + " and b.bill.id = :bid";
        params = new HashMap<>();
        params.put("ret", false);
        params.put("bid", bill.getId());
        bill.setDiscount(billItemFacade.findDoubleByJpql(jpql, params));

        jpql = "select sum(b.netValue) "
                + " from BillItem b "
                + " where b.retired = :ret "
                + " and b.bill.id = :bid";
        params = new HashMap<>();
        params.put("ret", false);
        params.put("bid", bill.getId());
        bill.setNetTotal(billItemFacade.findDoubleByJpql(jpql, params));

        // Persist final updates to Bill
        billFacade.edit(bill);

        JsfUtil.addSuccessMessage("Bill Updated");
    }

    public void updateBillFeeRetierd(BillFee bf) {
        if (bf.isRetired()) {
            bf.setRetiredAt(new Date());
            bf.setRetirer(sessionController.getLoggedUser());
            getBillFeeFacade().edit(bf);
            JsfUtil.addSuccessMessage("Bill Fee Retired");
        }
    }

    public void updateBillItemRetierd(BillItem bi) {

        if (bi.isRetired()) {
            bi.setRetiredAt(new Date());
            bi.setRetirer(sessionController.getLoggedUser());
            getBillItemFacade().edit(bi);
            JsfUtil.addSuccessMessage("Bill Item Retired");
        }
    }

    public void updateBillfee(BillFee bf) {

        getBillFeeFacade().edit(bf);
        JsfUtil.addSuccessMessage("Bill Item Retired");
    }

    private void createBillFees() {
        String sql = "SELECT b FROM BillFee b WHERE b.bill.id=" + getBillSearch().getId();
        billFeesList = getBillFeeFacade().findByJpql(sql);
    }

    private List<BillItem> billItemList;

    private void createBillItemsAll() {
        String sql = "SELECT b FROM BillItem b WHERE b.bill.id=" + getBillSearch().getId();
        billItemList = billItemFacade.findByJpql(sql);
    }

    public void createCashReturnBills() {
        bills = null;
        Map m = new HashMap();
        m.put("bt", BillType.PharmacySale);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from RefundBill b where  b.retired=false and b.institution=:ins and"
                + " b.createdAt between :fd and :td and b.billType=:bt ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.id desc  ";

        bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void makeNull() {
        printPreview = false;
        refundAmount = 0;
        txtSearch = "";
        bill = null;
        paymentMethod = null;
        billForRefund = null;
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
        lazyBills = null;
        searchKeyword = null;
    }

    public void update() {
        getBillFacade().edit(getBill());
    }

    public WebUser getUser() {
        return user;
    }

    public PatientInvestigation getPatientInvestigation() {
        return patientInvestigation;
    }

    public void setPatientInvestigation(PatientInvestigation patientInvestigation) {
        this.patientInvestigation = patientInvestigation;
    }

    public void onEdit(RowEditEvent event) {

        BillFee tmp = (BillFee) event.getObject();

        tmp.setEditedAt(new Date());
        tmp.setEditor(sessionController.getLoggedUser());

//        if (tmp.getPaidValue() != 0.0) {
//            JsfUtil.addErrorMessage("Already Staff FeePaid");
//            return;
//        }
        getBillFeeFacade().edit(tmp);

    }

    public void onEditItem(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
        tmp.setEditedAt(new Date());
        tmp.setEditor(sessionController.getLoggedUser());
        getBillItemFacade().edit(tmp);
        if (tmp.getPaidForBillFee() != null) {
            getBillFeeFacade().edit(tmp.getPaidForBillFee());
        }
    }

    public void setUser(WebUser user) {
        // recreateModel();
        this.user = user;
        recreateModel();
    }

    public EjbApplication getEjbApplication() {
        return ejbApplication;
    }

    public void setEjbApplication(EjbApplication ejbApplication) {
        this.ejbApplication = ejbApplication;
    }

    public Bill getPrintingBill() {
        return printingBill;
    }

    public void setPrintingBill(Bill printingBill) {
        this.printingBill = printingBill;
    }

    public boolean calculateRefundTotal() {
        refundAmount = 0;
        refundDiscount = 0;
        refundTotal = 0;
        refundMargin = 0;
        refundVat = 0;
        refundVatPlusTotal = 0;
        //billItems=null;
        tempbillItems = null;
        double totalHospitalRefund = 0.0;
        double totalCcRefund = 0.0;
        double totalStaffRefund = 0.0;

        for (BillItem i : getRefundingItems()) {
            if (checkPaidIndividual(i)) {
                JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Refund Bill");
                return false;
            }
            //Add for check refund is already done
            String sql = "SELECT bi FROM BillItem bi where bi.retired=false and bi.referanceBillItem.id=" + i.getId();
            BillItem rbi = getBillItemFacade().findFirstByJpql(sql);

            if (rbi != null) {
                JsfUtil.addErrorMessage("This Bill Item Already Refunded");
                return false;
            }
            //

//            if (!i.isRefunded()) {
            refundTotal += i.getGrossValue();
            refundAmount += i.getNetValue();
            refundMargin += i.getMarginValue();
            refundDiscount += i.getDiscount();
            refundVat += i.getVat();
            refundVatPlusTotal += i.getVatPlusNetValue();

            totalCcRefund += i.getHospitalFee();
            totalHospitalRefund += i.getHospitalFee();
            totalStaffRefund += i.getStaffFee();

            getTempbillItems().add(i);
//            }

        }

        return true;
    }

    public void calculateRefundTotalForOpdBillForAjex() {
        refundAmount = 0;
        refundDiscount = 0;
        refundTotal = 0;
        refundMargin = 0;
        refundVat = 0;
        refundVatPlusTotal = 0;

        if (getRefundingBill() == null) {
            return;
        }
        if (getRefundingBill().getBillItems() == null) {
            return;
        }
        for (BillItem refundingBillItem : getRefundingBill().getBillItems()) {

            if (refundingBillItem.getBillFees() == null) {
                return;
            }

            double refundingValue = 0;
            for (BillFee refundingBillFees : refundingBillItem.getBillFees()) {
                refundingValue += refundingBillFees.getFeeValue();
            }
            refundingBillItem.setNetValue(refundingValue);
            refundingBillItem.setGrossValue(refundingValue);
            refundingBillItem.setDiscount(0.0);
            refundTotal += refundingBillItem.getGrossValue();
            refundAmount += refundingBillItem.getNetValue();
            refundMargin += refundingBillItem.getMarginValue();
            refundDiscount += refundingBillItem.getDiscount();
            refundVat += refundingBillItem.getVat();
            refundVatPlusTotal += refundingBillItem.getVatPlusNetValue();
            getTempbillItems().add(refundingBillItem);

        }
        getRefundingBill().setNetTotal(refundAmount);
        getRefundingBill().setNetTotal(refundTotal);
        getRefundingBill().setMargin(refundMargin);
        getRefundingBill().setDiscount(refundDiscount);
        getRefundingBill().setVat(refundVat);

    }

    public boolean calculateRefundTotalForOpdBill() {
        if (refundingBill == null || refundingBill.getBillItems() == null) {
            return false;
        }

        double billStaffTotal = 0.0;
        double billCcTotal = 0.0;
        double billHospitalTotal = 0.0;
        double billNetTotal = 0.0;

        // We will collect items to remove after processing.
        List<BillItem> itemsToRemove = new ArrayList<>();

        for (BillItem bi : refundingBill.getBillItems()) {
            double billItemStaffTotal = 0.0;
            double billItemCcTotal = 0.0;
            double billItemHospitalTotal = 0.0;
            double billItemNetTotal = 0.0;

            if (bi.getBillFees() == null) {
                itemsToRemove.add(bi);
                continue;
            }

            for (BillFee bf : bi.getBillFees()) {
                // Convert all fee values to negative
                double feeValue = bf.getFeeValue();
                feeValue = -Math.abs(feeValue);

                if (bf.getInstitution() != null && bf.getInstitution().getInstitutionType() == InstitutionType.CollectingCentre) {
                    billItemCcTotal += feeValue;
                } else if (bf.getStaff() != null || bf.getSpeciality() != null) {
                    billItemStaffTotal += feeValue;
                } else {
                    billItemHospitalTotal += feeValue;
                }
            }

            billItemNetTotal = billItemCcTotal + billItemHospitalTotal + billItemStaffTotal;

            // If net total is zero, mark for removal
            if (billItemNetTotal == 0) {
                itemsToRemove.add(bi);
                continue;
            }

            bi.setCollectingCentreFee(billItemCcTotal);
            bi.setStaffFee(billItemStaffTotal);
            bi.setHospitalFee(billItemHospitalTotal);
            bi.setNetRate(billItemNetTotal);
            bi.setQty(-1.0);
            bi.setNetValue(billItemNetTotal);
            bi.setGrossValue(billItemNetTotal);

            // Accumulate totals at bill level
            billStaffTotal += billItemStaffTotal;
            billCcTotal += billItemCcTotal;
            billHospitalTotal += billItemHospitalTotal;
            billNetTotal += billItemNetTotal;
        }

        // Remove items with zero value from the list
        refundingBill.getBillItems().removeAll(itemsToRemove);

        // Set bill totals
        refundingBill.setHospitalFee(billHospitalTotal);
        refundingBill.setCollctingCentreFee(billCcTotal);
        refundingBill.setStaffFee(billStaffTotal);
        refundingBill.setNetTotal(billNetTotal);
        refundingBill.setTotal(billNetTotal);
        refundingBill.setDiscount(0);

        return true;
    }

    public List<Bill> getUserBillsOwn() {
        Date startTime = new Date();
        List<Bill> userBills;
        if (getUser() == null) {
            userBills = new ArrayList<>();
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), getSessionController().getInstitution(), BillType.OpdBill);
        }
        if (userBills == null) {
            userBills = new ArrayList<>();

        }

        return userBills;

    }

    public List<Bill> getBillsOwn() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), getSessionController().getInstitution(), BillType.OpdBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), getSessionController().getInstitution(), BillType.OpdBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public void recreateModel2() {
        billForRefund = null;
        refundAmount = 0.0;
        billFees = null;
//        billFees
        fromDate = null;
        toDate = null;
        billComponents = null;
        billForRefund = null;
        billItems = null;
        bills = null;
        printPreview = false;
        tempbillItems = null;
        comment = null;
        lazyBills = null;
    }

    public LazyDataModel<Bill> getSearchBills() {
        return lazyBills;
    }

    public void createDealorPaymentTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  ((b.toInstitution.name) like :ins )";
            temMap.put("ins", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBank() != null && !getSearchKeyword().getBank().trim().equals("")) {
            sql += " and  ((b.bank.name) like :bnk )";
            temMap.put("bnk", "%" + getSearchKeyword().getBank().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  ((b.chequeRefNo) like :chck )";
            temMap.put("chck", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.id desc  ";

        temMap.put("billType", BillType.GrnPayment);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("dept", getSessionController().getInstitution());
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);
    }

    public void makeKeywodNull() {
        searchKeyword = null;
    }

    public List<BillItem> getRefundingItems() {
        return refundingItems;
    }

    public void setRefundingItems(List<BillItem> refundingItems) {
        this.refundingItems = refundingItems;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String refundCollectingCenterBill() {
        if (refundingItems.isEmpty()) {
            JsfUtil.addErrorMessage("There is no item to Refund");
            return "";
        }
        if (comment == null || comment.trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return "";
        }
        if (getBill() == null && getBill().getId() == null && getBill().getId() == 0) {
            JsfUtil.addErrorMessage("No Bill to refund");
            return "";
        }
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not Refund again");
            return "";
        }
        if (!calculateRefundTotal()) {
            return "";
        }

        if (!getWebUserController().hasPrivilege("LabBillRefundSpecial")) {
            for (BillItem trbi : refundingItems) {
                if (patientInvestigationController.sampledForBillItem(trbi)) {
                    JsfUtil.addErrorMessage("One or more bill Item has been already undersone process at the Lab. Can not return.");
                    return "";
                }
            }
        }

        RefundBill rb = (RefundBill) createRefundBill();
        refundBillItems(rb);

        calculateRefundBillFees(rb);

        getBill().setRefunded(true);
        getBill().setRefundedBill(rb);
        getBillFacade().editAndCommit(getBill());
        double feeTotalExceptCcfs = 0.0;
//        Payment p = collectingCentreBillController.createPaymentForRefunds(rb, paymentMethod);
//        drawerController.updateDrawerForOuts(p);

//            for (BillItem bi : refundingItems) {
//                String sql = "select c from BillFee c where c.billItem.id = " + bi.getId();
//                List<BillFee> rbf = getBillFeeFacade().findByJpql(sql);
//                for (BillFee bf : rbf) {
//                    if (bf.getFee().getFeeType() != FeeType.CollectingCentre) {
//                        feeTotalExceptCcfs += (bf.getFeeValue() + bf.getFeeVat());
//                    }
//                }
//            }
//        Institution collectingCentre,
//            double hospitalFee,
//            double collectingCentreFee,
//            double staffFee,
//            double transactionValue,
//            HistoryType historyType,
//            Bill bill
//
        collectingCentreApplicationController.updateCcBalance(
                getBill().getInstitution(),
                rb.getTotalHospitalFee(),
                rb.getTotalCenterFee(),
                rb.getStaffFee(),
                rb.getNetTotal(),
                HistoryType.CollectingCentreBillingRefund,
                rb);

        bill = billFacade.find(rb.getId());
        printPreview = true;
        return "";
    }

    public String refundOpdBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("There is no Bill to Refund");
            return "";
        }
        if (getRefundingBill() == null) {
            JsfUtil.addErrorMessage("There is no refund bill");
            return "";
        }
        if (refundingBill.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("There are no items to Refund");
            return "";
        }

        if (comment == null || comment.trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return "";
        } else {
            getRefundingBill().setComments(comment);
        }

        bill = billService.reloadBill(bill);
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not Refund again");
            return "";
        }
        if (configOptionApplicationController.getBooleanValueByKey("Enable Drawer Manegment", true)) {
            if (refundingBill.getPaymentMethod() != null && refundingBill.getPaymentMethod() == PaymentMethod.Cash) {
                if (financialTransactionController.getLoggedUserDrawer().getCashInHandValue() < refundingBill.getNetTotal()) {
                    JsfUtil.addErrorMessage("Not enough cash in the Drawer");
                    return "";
                }
            }
        }
        if (!getWebUserController().hasPrivilege("LabBillRefundSpecial")) {
            if (configOptionApplicationController.getBooleanValueByKey("Immediate Refund Request for OPO Bills of Any Status", true)) {
                if (sampleHasBeenCollected(refundingBill)) {
                    JsfUtil.addErrorMessage("One or more bill Item you are refunding has been already undersone process at the Lab. Can not return.");
                    return "";
                }
            }

        }

        if (billFeeIsAlreadyRefunded(refundingBill)) {
            JsfUtil.addErrorMessage("One or more bill Item you are refunding has been already refunded. Can not refund again.");
            return "";
        }

        if (billFeeIsAlreadyPaidToStaff(refundingBill)) {
            JsfUtil.addErrorMessage("One or more bill Item you are refunding has been already paid to Service Provider. Can not refund again.");
            return "";
        }

        if (paymentMethod == PaymentMethod.Staff) {
            if (getBill().getToStaff() == null) {
                JsfUtil.addErrorMessage("Can't Select Staff Method");
                return "";
            }
        }

        if (paymentMethod == PaymentMethod.Staff_Welfare) {
            if (getBill().getToStaff() == null) {
                JsfUtil.addErrorMessage("Can't Select Staff Welfare Method");
                return "";
            }
        }

        if (refundingBill.getBillItems() != null) {
            for (BillItem refundingBillItemTmp : refundingBill.getBillItems()) {
                for (BillFee refundingBillFeeTmp : refundingBillItemTmp.getBillFees()) {
                    if (refundingBillFeeTmp.getReferenceBillFee().getFeeValue() < refundingBillFeeTmp.getFeeValue()) {
                        JsfUtil.addErrorMessage("Pleace Enter Correct Value");
                        return "";
                    }
                }
            }
        }

        boolean reundTotalCalculationsSuccess = calculateRefundTotalForOpdBill();
        if (!reundTotalCalculationsSuccess) {
            JsfUtil.addErrorMessage("Error in calculations. Please Check Bill");
            return "";
        }

        if (refundingBill.getNetTotal() == 0.0) {
            JsfUtil.addErrorMessage("There is no item to Refund");
            return "";
        }
//        boolean billValueInversionIsSuccess = invertBillValuesAndCalculate(refundingBill);
//        if (!billValueInversionIsSuccess) {
//            JsfUtil.addErrorMessage("Error in Bill Value Inversion");
//            return "";
//        }

        saveRefundBill(refundingBill);

        Payment p = getOpdPreSettleController().createPaymentForCancellationsAndRefunds(refundingBill, paymentMethod);

        //TODO: Create Payments for Bill Items
        if (getBill().getPaymentMethod() == PaymentMethod.Staff) {
            getBill().getToStaff().setCurrentCreditValue(getBill().getToStaff().getCurrentCreditValue() - Math.abs(getRefundingBill().getNetTotal()));
            staffFacade.edit(getBill().getToStaff());

        } else if (paymentMethod == PaymentMethod.PatientDeposit) {
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(getRefundingBill().getPatient(), sessionController.getDepartment());
            patientDepositController.updateBalance(getRefundingBill(), pd);
        } else if (paymentMethod == PaymentMethod.Staff_Welfare) {
            staffBean.updateStaffWelfare(getBill().getToStaff(), -Math.abs(getRefundingBill().getNetTotal()));
        }
        drawerController.updateDrawerForOuts(p);
        printPreview = true;
        return "";
    }

    public void saveViewingBillData() {
        billController.save(viewingBill);
        billItemController.save(viewingBillItems);
        billItemController.savePharmaceuticalItems(viewingPharmaceuticalBillItems);
        billItemController.savePatientInvestigations(viewingPatientInvestigations);
        billFeeController.save(viewingBillFees);
        paymentController.save(viewingBillPayments);
    }

    public boolean sampleHasBeenCollected(Bill rf) {
        boolean oneItemIsSampled = false;
        for (BillItem bi : rf.getBillItems()) {
            double refVal = bi.getNetValue();
            refVal = Math.abs(refVal);
            if (refVal > 0.0) {
                boolean sampled = patientInvestigationController.sampledForBillItem(bi.getReferanceBillItem());
                if (sampled) {
                    oneItemIsSampled = true;
                }
            }
        }
        return oneItemIsSampled;
    }

    public boolean billFeeIsAlreadyRefunded(Bill rf) {
        boolean oneItemIsAlreadyRefunded = false;
        for (BillItem bi : rf.getBillItems()) {
            for (BillFee bf : bi.getBillFees()) {
                double refVal = bi.getNetValue();
                refVal = Math.abs(refVal);
                if (refVal > 0.0) {
                    boolean refunded = billController.hasRefunded(bf.getReferenceBillFee());
                    if (refunded) {
                        oneItemIsAlreadyRefunded = true;
                    }
                }
            }
        }
        return oneItemIsAlreadyRefunded;
    }

    public boolean billFeeIsAlreadyPaidToStaff(Bill rf) {
        boolean oneItemIsAlreadyPaid = false;
        for (BillItem bi : rf.getBillItems()) {
            for (BillFee bf : bi.getBillFees()) {
                double refVal = bi.getNetValue();
                refVal = Math.abs(refVal);
                if (refVal > 0.0) {
                    boolean refunded = billController.hasPaidToStaff(bf.getReferenceBillFee());
                    if (refunded) {
                        oneItemIsAlreadyPaid = true;
                    }
                }
            }
        }
        return oneItemIsAlreadyPaid;
    }

    private boolean saveRefundBill(Bill rb) {
        if (rb == null) {
            JsfUtil.addErrorMessage("No bill");
            return false;
        }
        if (rb.getBilledBill() == null) {
            rb.setBilledBill(getBill());
        }
        Date bd = Calendar.getInstance().getTime();
        rb.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_REFUND);
        rb.setBillType(BillType.OpdBill);
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setDepartment(getSessionController().getDepartment());
        rb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        rb.setComments(comment);
        rb.setPaymentMethod(paymentMethod);
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.OPD_BILL_REFUND);
        rb.setInsId(deptId);
        rb.setDeptId(deptId);
        rb.setReferenceBill(bill);
        rb.setBilledBill(bill);

        // Step 3: Persist the Bill last
        billController.save(rb);
        currentRefundBill = rb;

        // Update the original bill
        List<Bill> refundBills = new ArrayList<>(bill.getRefundBills());
        refundBills.add(rb);
        bill.getForwardReferenceBills().add(rb);
        bill.setRefunded(true);
        bill.setRefundBills(refundBills);
        bill.setRefundedBill(rb);
        billController.save(bill);

        return true;
    }

    private Bill createRefundBill() {
        RefundBill rb = new RefundBill();
        rb.copy(getBill());
        rb.invertAndAssignValuesFromOtherBill(getBill());
        rb.setBillTypeAtomic(BillTypeAtomic.CC_BILL_REFUND);
        rb.setBilledBill(getBill());
        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setDepartment(getSessionController().getDepartment());
        rb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        rb.setDiscount(0.00);
        rb.setDiscountPercent(0.0);
        rb.setComments(comment);
        rb.setPaymentMethod(paymentMethod);

        rb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill().getToDepartment(), BillType.OpdBill, BillClassType.RefundBill, BillNumberSuffix.RF));
        rb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getBill().getToDepartment(), BillType.OpdBill, BillClassType.RefundBill, BillNumberSuffix.RF));

        rb.setTotal(0 - refundTotal);
        rb.setDiscount(0 - refundDiscount);
        rb.setNetTotal(0 - refundAmount);
        rb.setVat(0 - refundVat);
        rb.setVatPlusNetTotal(0 - refundVatPlusTotal);

        getBillFacade().createAndFlush(rb);

        return rb;

    }

    //    public String returnBill() {
//        if (refundingItems.isEmpty()) {
//            JsfUtil.addErrorMessage("There is no item to Refund");
//            return "";
//
//        }
//        if (refundAmount == 0.0) {
//            JsfUtil.addErrorMessage("There is no item to Refund");
//            return "";
//        }
//        if (comment == null || comment.trim().equals("")) {
//            JsfUtil.addErrorMessage("Please enter a comment");
//            return "";
//        }
//
//        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
//            if (getBill().isCancelled()) {
//                JsfUtil.addErrorMessage("Already Cancelled. Can not Refund again");
//                return "";
//            }
//            if (!calculateRefundTotal()) {
//                return "";
//            }
//
//            RefundBill rb = (RefundBill) createReturnBill();
//
//            refundBillItems(rb);
//
//            getBill().setRefunded(true);
//            getBill().setRefundedBill(rb);
//            getBillFacade().edit(getBill());
//
//            printPreview = true;
//            //JsfUtil.addSuccessMessage("Refunded");
//
//        } else {
//            JsfUtil.addErrorMessage("No Bill to refund");
//            return "";
//        }
//        //  recreateModel();
//        return "";
//    }
    private Bill createReturnBill() {
        RefundBill rb = new RefundBill();
        rb.setBilledBill(getBill());
        Date bd = Calendar.getInstance().getTime();
        rb.setBillType(getBill().getBillType());
        rb.setBilledBill(getBill());
        rb.setCreatedAt(bd);
        rb.setComments(comment);
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setCreditCompany(getBill().getCreditCompany());
        rb.setDepartment(getSessionController().getLoggedUser().getDepartment());

        rb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill().getBillType(), BillClassType.RefundBill, BillNumberSuffix.GRNRET));
        rb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getBill().getBillType(), BillClassType.RefundBill, BillNumberSuffix.GRNRET));

        rb.setToDepartment(getBill().getToDepartment());
        rb.setToInstitution(getBill().getToInstitution());

        rb.setFromDepartment(getBill().getFromDepartment());
        rb.setFromInstitution(getBill().getFromInstitution());

        rb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        rb.setDepartment(getSessionController().getDepartment());

        rb.setNetTotal(refundAmount);
        rb.setPatient(getBill().getPatient());
        rb.setPatientEncounter(getBill().getPatientEncounter());
        rb.setPaymentMethod(paymentMethod);
        rb.setReferredBy(getBill().getReferredBy());
        rb.setTotal(0 - refundAmount);
        rb.setNetTotal(0 - refundAmount);

        getBillFacade().create(rb);

        return rb;

    }

    public void calculateRefundBillFees(RefundBill rb) {
        double totalStaffFee = 0.0;
        double totalCollectingCentreFee = 0.0;
        double totalHospitalFee = 0.0;
        for (BillItem bi : refundingItems) {
            String sql = "select c from BillFee c where c.billItem.id = " + bi.getId();
            List<BillFee> rbf = getBillFeeFacade().findByJpql(sql);
            for (BillFee bf : rbf) {
                if (bf.getFee().getStaff() == null || bf.getFee().getSpeciality() == null) {
                    if (bf.getInstitution() != null && bf.getInstitution().getInstitutionType() == InstitutionType.CollectingCentre) {
                        totalCollectingCentreFee += bf.getFeeValue();
                    } else {
                        totalHospitalFee = totalHospitalFee + bf.getFeeValue();
                    }
                } else {
                    totalStaffFee = totalStaffFee + bf.getFeeValue();
                }
            }

        }
        rb.setStaffFee(0 - totalStaffFee);
        rb.setPerformInstitutionFee(0 - totalHospitalFee);
        rb.setTotalCenterFee(totalCollectingCentreFee);
        rb.setTotalHospitalFee(totalHospitalFee);
        rb.setTotalStaffFee(totalStaffFee);
        getBillFacade().edit(rb);
    }

    public void refundBillItems(RefundBill refundingBill) {
        for (BillItem bi : refundingItems) {
            //set Bill Item as Refunded
            BillItem originalBillItem = billItemFacade.find(bi.getId());
            BillItem rbi = new BillItem();
            rbi.copy(originalBillItem);
            rbi.invertValue(originalBillItem);
            rbi.setBill(refundingBill);
            rbi.setCreatedAt(Calendar.getInstance().getTime());
            rbi.setCreater(getSessionController().getLoggedUser());
            rbi.setReferanceBillItem(originalBillItem);
            getBillItemFacede().createAndFlush(rbi);
            originalBillItem.setRefunded(true);
            originalBillItem.setBillItemRefunded(true);
            getBillItemFacede().editAndFlush(originalBillItem);
            originalBillItem = billItemFacade.find(bi.getId());
            String sql = "Select bf From BillFee bf where "
                    + " bf.retired=false and bf.billItem.id=" + originalBillItem.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
            returnBillFee(refundingBill, rbi, tmp);
            //create BillFeePayments For Refund
            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + rbi.getId();
            List<BillFee> tmpC = getBillFeeFacade().findByJpql(sql);
//            getOpdPreSettleController().createOpdCancelRefundBillFeePayment(refundingBill, tmpC, p);
            refundingBill.getBillItems().add(rbi);

        }
    }

    //    public void refundBillItems(RefundBill rb) {
//        for (BillItem bi : refundingItems) {
//            //set Bill Item as Refunded
//
//            BillItem rbi = new BillItem();
//            rbi.copy(bi);
//            rbi.invertAndAssignValuesFromOtherBill(bi);
//            rbi.setBill(rb);
//            rbi.setCreatedAt(Calendar.getInstance().getTime());
//            rbi.setCreater(getSessionController().getLoggedUser());
//            rbi.setReferanceBillItem(bi);
//            getBillItemFacede().create(rbi);
//
//            bi.setRefunded(Boolean.TRUE);
//            getBillItemFacede().edit(bi);
//            BillItem bbb = getBillItemFacade().find(bi.getId());
//
//            String sql = "Select bf From BillFee bf where "
//                    + " bf.retired=false and bf.billItem.id=" + bi.getId();
//            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
//
//            returnBillFee(rb, rbi, tmp);
//
//        }
//    }
    public void recreateModel() {
        billForRefund = null;
        refundAmount = 0.0;
        billFees = null;
        refundingItems = null;
//        billFees
        billComponents = null;
        billForRefund = null;
        billItems = null;
        bills = null;
        printPreview = false;
        tempbillItems = null;
        comment = null;
        lazyBills = null;
        searchKeyword = null;
    }

    @Deprecated // NOT a Useful Method
    private void cancelBillComponents(Bill cancellationProfessionalPaymentBill, BillItem cancellationProfessionalBillItem) {
        for (BillComponent nB : getBillComponents()) {
            BillComponent bC = new BillComponent();
            bC.setCatId(nB.getCatId());
            bC.setDeptId(nB.getDeptId());
            bC.setInsId(nB.getInsId());
            bC.setDepartment(nB.getDepartment());
            bC.setDeptId(nB.getDeptId());
            bC.setInstitution(nB.getInstitution());
            bC.setItem(nB.getItem());
            bC.setName(nB.getName());
            bC.setPackege(nB.getPackege());
            bC.setSpeciality(nB.getSpeciality());
            bC.setStaff(nB.getStaff());

            bC.setBill(cancellationProfessionalPaymentBill);
            bC.setBillItem(cancellationProfessionalBillItem);
            bC.setCreatedAt(new Date());
            bC.setCreater(getSessionController().getLoggedUser());
            getBillCommponentFacade().create(bC);
        }

    }

    private boolean checkPaidIndividual(BillItem bi) {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
        List<BillFee> tempFe = getBillFeeFacade().findByJpql(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
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

    private CancelledBill createCollectingCenterCancelBill(Bill originalBill) {
        CancelledBill cb = new CancelledBill();
        if (originalBill == null) {
            return null;
        }

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getInstitution(), sessionController.getDepartment(), BillType.CollectingCentreBill, BillClassType.CancelledBill);

        cb.copy(originalBill);
        cb.copyValue(originalBill);
        cb.invertAndAssignValuesFromOtherBill(originalBill);
        cb.setBillTypeAtomic(BillTypeAtomic.CC_BILL_CANCELLATION);
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
        cb.setInsId(deptId);
        cb.setDeptId(deptId);

        return cb;
    }

    private CancelledBill createProfessionalPaymentCancelBill(Bill originalBill) {
        CancelledBill cb = new CancelledBill();
        if (originalBill != null) {
            cb.copy(originalBill);
            cb.copyValue(originalBill);
            cb.invertAndAssignValuesFromOtherBill(originalBill);
            cb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), originalBill.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PROCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), originalBill.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PROCAN));
        }
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES_RETURN);
        cb.setDeptId(deptId);
        cb.setInsId(deptId);
        cb.setBillTypeAtomic(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES_RETURN);
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

    private CancelledBill createCahsInOutCancelBill(Bill originalBill, BillNumberSuffix billNumberSuffix) {
        CancelledBill cb = new CancelledBill();
        if (originalBill != null) {
            cb.copy(originalBill);
            cb.invertAndAssignValuesFromOtherBill(originalBill);

            cb.setBilledBill(originalBill);

            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, billNumberSuffix));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, billNumberSuffix));

        }

        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

        cb.setPaymentMethod(paymentMethod);
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

    private boolean errorsPresentOnProfessionalPaymentBillCancellation(Bill originalBill) {
        originalBill = billFacade.findWithoutCache(originalBill.getId());
        if (originalBill == null) {
            JsfUtil.addErrorMessage("No Original Bill");
            return true;
        }
        if (originalBill.isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (originalBill.isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
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

        if (getBill().getBackwardReferenceBill().getPaymentMethod() == PaymentMethod.Credit) {
            List<BillItem> items = billService.checkCreditBillPaymentReciveFromCreditCompany(getBill().getBackwardReferenceBill());
            if (items != null && !items.isEmpty()) {
                JsfUtil.addErrorMessage("This bill has been paid for by the credit company. Therefore, it cannot be canceled.");
                return;
            }
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

        CancelledBill cancellationBill = createOpdCancelBill(bill);
        billController.save(cancellationBill);
        List<Payment> ps = getOpdPreSettleController().createPaymentsForCancellationsforOPDBill(cancellationBill, paymentMethod);
        List<BillItem> list = cancelBillItems(getBill(), cancellationBill, ps);

        try {
            if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
                for (PatientInvestigation pi : patientInvestigationController.getPatientInvestigationsFromBill(getBill())) {
                    labTestHistoryController.addCancelHistory(pi, sessionController.getDepartment(), comment);
                }
            }
        } catch (Exception e) {
            System.out.println("Error = " + e);
        }

        cancellationBill.setBillItems(list);
        billFacade.edit(cancellationBill);

        getBill().setCancelled(true);
        getBill().setCancelledBill(cancellationBill);

        billController.save(getBill());
        drawerController.updateDrawerForOuts(ps);
        JsfUtil.addSuccessMessage("Cancelled");

        if (getBill().getPaymentMethod() == PaymentMethod.Credit) {
            //TODO: Manage Credit Balances for Company, Staff
            if (getBill().getToStaff() != null) {
                staffBean.updateStaffCredit(getBill().getToStaff(), 0 - (getBill().getNetTotal() + getBill().getVat()));
                JsfUtil.addSuccessMessage("Staff Credit Updated");
                cancellationBill.setFromStaff(getBill().getToStaff());
                getBillFacade().edit(cancellationBill);
            }
        }

        if (getBill().getPaymentMethod() == PaymentMethod.Staff_Welfare) {
            if (getBill().getToStaff() != null) {
                staffBean.updateStaffWelfare(getBill().getToStaff(), 0 - (getBill().getNetTotal() + getBill().getVat()));
                JsfUtil.addSuccessMessage("Staff Welfare Updated");
                cancellationBill.setFromStaff(getBill().getToStaff());
                getBillFacade().edit(cancellationBill);
            }
        }

        if (cancellationBill.getPaymentMethod() == PaymentMethod.PatientDeposit) {
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(cancellationBill.getPatient(), sessionController.getDepartment());
            patientDepositController.updateBalance(cancellationBill, pd);
        }

        notificationController.createNotification(cancellationBill);
        bill = billFacade.find(cancellationBill.getId());
        printPreview = true;
        duplicate = false;
        comment = null;

//            getEjbApplication().getBillsToCancel().add(cb);
//            JsfUtil.addSuccessMessage("Awaiting Cancellation");
    }

    public void cancelCollectingCentreBill() {
        if (!ccBillCancellingStarted.compareAndSet(false, true)) {
            JsfUtil.addErrorMessage("Cancellation already Started");
            return;
        }
        if (getBill() == null) {
            JsfUtil.addErrorMessage("No bill");
            ccBillCancellingStarted.set(false);
            return;
        }
        if (getBill().getId() == null) {
            JsfUtil.addErrorMessage("No Saved bill");
            ccBillCancellingStarted.set(false);
            return;
        }

        if (!configOptionApplicationController.getBooleanValueByKey("Enable the Special Privilege of Canceling CC Bills", false)) {
            if (!checkCancelBill(getBill())) {
                JsfUtil.addErrorMessage("This bill is processed in the laboratory.");
                if (getWebUserController().hasPrivilege("BillCancel")) {
                    JsfUtil.addErrorMessage("You have Special privilege to cancel This Bill");
                } else {
                    JsfUtil.addErrorMessage("You have no Privilege to Cancel OPD Bills. Please Contact System Administrator.");
                    ccBillCancellingStarted.set(false);
                    return;
                }
            } else {
                if (!getWebUserController().hasPrivilege("OpdCancel")) {
                    JsfUtil.addErrorMessage("You have no Privilege to Cancel OPD Bills. Please Contact System Administrator.");
                    ccBillCancellingStarted.set(false);
                    return;
                }
            }
        } else {
            if (!getWebUserController().hasPrivilege("OpdCancel")) {
                JsfUtil.addErrorMessage("You have no Privilege to Cancel OPD Bills. Please Contact System Administrator.");
                ccBillCancellingStarted.set(false);
                return;
            }
        }

        CancelledBill cancellationBill = createCollectingCenterCancelBill(bill);
        billController.save(cancellationBill);
//        Payment p = getOpdPreSettleController().createPaymentForCancellationsforOPDBill(cancellationBill, paymentMethod);
        List<BillItem> list = cancelCcBillItems(getBill(), cancellationBill);

        try {
            if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
                for (PatientInvestigation pi : patientInvestigationController.getPatientInvestigationsFromBill(getBill())) {
                    labTestHistoryController.addCancelHistory(pi, sessionController.getDepartment(), comment);
                }
            }
        } catch (Exception e) {
            System.out.println("Error = " + e);
        }

        cancellationBill.setBillItems(list);
        billFacade.edit(cancellationBill);

        getBill().setCancelled(true);
        getBill().setCancelledBill(cancellationBill);

        billController.save(getBill());
        JsfUtil.addSuccessMessage("Cancelled");

//        Institution collectingCentre,
//            double hospitalFee,
//            double collectingCentreFee,
//            double staffFee,
//            double transactionValue,
//            HistoryType historyType,
//            Bill bill
        collectingCentreApplicationController.updateCcBalance(
                getBill().getCollectingCentre(),
                bill.getTotalHospitalFee(),
                bill.getTotalCenterFee(),
                bill.getTotalStaffFee(),
                bill.getNetTotal(),
                HistoryType.CollectingCentreBillingCancel,
                cancellationBill);

//        drawerController.updateDrawerForOuts(p);
        bill = billFacade.find(bill.getId());
        printPreview = true;
        comment = null;
        ccBillCancellingStarted.set(false);
    }

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }

    public void cancelCashInBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            CancelledBill cb = createCahsInOutCancelBill(bill, BillNumberSuffix.CSINCAN);
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());
            Calendar created = Calendar.getInstance();
            created.setTime(cb.getBilledBill().getCreatedAt());

            //Copy & paste
            if ((now.get(Calendar.DATE) == created.get(Calendar.DATE))
                    || (getBill().getBillType() == BillType.LabBill && getWebUserController().hasPrivilege("LabBillCancelling"))
                    || (getBill().getBillType() == BillType.OpdBill && getWebUserController().hasPrivilege("OpdCancel"))) {

                getBillFacade().create(cb);
                cancelBillItems(cb);
                getBill().setCancelled(true);
                getBill().setCancelledBill(cb);
                getBillFacade().edit(getBill());
                JsfUtil.addSuccessMessage("Cancelled");

                CashTransaction newCt = new CashTransaction();
                newCt.invertQty(getBill().getCashTransaction());

                CashTransaction tmp = getCashTransactionBean().saveCashOutTransaction(newCt, cb, getSessionController().getLoggedUser());
                cb.setCashTransaction(tmp);
                getBillFacade().edit(cb);

//                getCashTransactionBean().deductFromBallance(getSessionController().getLoggedUser().getDrawer(), tmp);
                WebUser wb = getWebUserFacade().find(getSessionController().getLoggedUser().getId());
                getSessionController().setLoggedUser(wb);
                printPreview = true;
            } else {
                getEjbApplication().getBillsToCancel().add(cb);
                JsfUtil.addSuccessMessage("Awaiting Cancellation");
            }
        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void cancelCashOutBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            CancelledBill cb = createCahsInOutCancelBill(bill, BillNumberSuffix.CSOUTCAN);
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());
            Calendar created = Calendar.getInstance();
            created.setTime(cb.getBilledBill().getCreatedAt());

            //Copy & paste
            if ((now.get(Calendar.DATE) == created.get(Calendar.DATE))
                    || (getBill().getBillType() == BillType.LabBill && getWebUserController().hasPrivilege("LabBillCancelling"))
                    || (getBill().getBillType() == BillType.OpdBill && getWebUserController().hasPrivilege("OpdCancel"))) {

                getBillFacade().create(cb);
                cancelBillItems(cb);
                getBill().setCancelled(true);
                getBill().setCancelledBill(cb);
                getBillFacade().edit(getBill());
                JsfUtil.addSuccessMessage("Cancelled");

                CashTransaction newCt = new CashTransaction();
                newCt.invertQty(getBill().getCashTransaction());

                CashTransaction tmp = getCashTransactionBean().saveCashInTransaction(newCt, cb, getSessionController().getLoggedUser());
                cb.setCashTransaction(tmp);
                getBillFacade().edit(cb);

//                getCashTransactionBean().addToBallance(getSessionController().getLoggedUser().getDrawer(), tmp);
                WebUser wb = getWebUserFacade().find(getSessionController().getLoggedUser().getId());
                getSessionController().setLoggedUser(wb);

                printPreview = true;
            } else {
                getEjbApplication().getBillsToCancel().add(cb);
                JsfUtil.addSuccessMessage("Awaiting Cancellation");
            }

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }

    }

    private void returnBillFee(Bill rb, BillItem bt, List<BillFee> tmp) {
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.copy(nB);
            bf.invertValue(nB);
            bf.setBill(rb);
            bf.setBillItem(bt);
            bf.setSettleValue(0 - nB.getSettleValue());
            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());
            getBillFeeFacade().create(bf);
        }
    }

    private void returnBillFeeForOpd(List<BillFee> tmp) {
        for (BillFee rbf : tmp) {
            rbf.invertValue(rbf);
            rbf.setSettleValue(0 - rbf.getSettleValue());
            rbf.setCreatedAt(new Date());
            rbf.setCreater(getSessionController().getLoggedUser());
            if (rbf.getId() == null) {
                getBillFeeFacade().create(rbf);
            } else {
                getBillFeeFacade().edit(rbf);
            }
        }
    }

    public void cancelProfessionalPaymentBill() {
        if (bill == null || bill.getId() == null && bill.getId() == 0) {
            JsfUtil.addErrorMessage("No Professional Payment Bill to cancel");
            return;
        }
        if (errorsPresentOnProfessionalPaymentBillCancellation(bill)) {
            return;
        }

        CancelledBill cancellationProfessionalPaymentBill = createProfessionalPaymentCancelBill(bill);
        getBillFacade().create(cancellationProfessionalPaymentBill);
        Payment cancellationBillPayment = getOpdPreSettleController().createPaymentForCancellationsAndRefunds(cancellationProfessionalPaymentBill, paymentMethod);
//            cancelBillItems(cb);
        drawerController.updateDrawerForIns(cancellationBillPayment);
        cancelProfessionalPaymentBillItems(cancellationProfessionalPaymentBill, bill, cancellationBillPayment);
        cancelPaymentItems(bill);
        getBill().setCancelled(true);
        getBill().setCancelledBill(cancellationProfessionalPaymentBill);
        getBillFacade().edit(getBill());
        JsfUtil.addSuccessMessage("Cancelled");

        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(cancellationProfessionalPaymentBill, getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        bill = cancellationProfessionalPaymentBill;
        printPreview = true;

    }

    private void cancelPaymentItems(Bill originalBill) {
        List<BillItem> originalBillItems;
        originalBillItems = getBillItemFacede().findByJpql("SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + originalBill.getId());
        for (BillItem originalBillItem : originalBillItems) {
            if (originalBillItem.getPaidForBillFee() != null) {
                originalBillItem.getPaidForBillFee().setPaidValue(0.0);
                getBillFeeFacade().edit(originalBillItem.getPaidForBillFee());
            }
        }
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

            getBillItemFacede().create(b);

//            cancelBillComponents(can, b);
            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();

            List<BillFee> originalBillFeesOfBillItem = getBillFeeFacade().findByJpql(sql);

            cancelBillFee(can, b, originalBillFeesOfBillItem);

            list.add(b);

        }

        return list;
    }

    private List<BillItem> cancelProfessionalPaymentBillItems(Bill cancellationProfessionalPaymentBill, Bill originalPaymentBill, Payment cancellationBillPayments) {
        List<BillItem> originalProfessionalPaymentBillItems = billBean.fetchBillItems(originalPaymentBill);
        List<BillItem> newlyCreatedCancellationBillItems = new ArrayList<>();
        if (originalProfessionalPaymentBillItems == null) {
            return newlyCreatedCancellationBillItems;
        }
        for (BillItem originalProfessionalPaymentBillItem : originalProfessionalPaymentBillItems) {
            BillItem cancellationProfessionalBillItem = new BillItem();
            cancellationProfessionalBillItem.setBill(cancellationProfessionalPaymentBill);

            cancellationProfessionalBillItem.setReferanceBillItem(originalProfessionalPaymentBillItem.getReferanceBillItem());

            cancellationProfessionalBillItem.setNetValue(0 - originalProfessionalPaymentBillItem.getNetValue());
            cancellationProfessionalBillItem.setGrossValue(0 - originalProfessionalPaymentBillItem.getGrossValue());
            cancellationProfessionalBillItem.setRate(0 - originalProfessionalPaymentBillItem.getRate());
            cancellationProfessionalBillItem.setVat(0 - originalProfessionalPaymentBillItem.getVat());
            cancellationProfessionalBillItem.setVatPlusNetValue(0 - originalProfessionalPaymentBillItem.getVatPlusNetValue());

            cancellationProfessionalBillItem.setCatId(originalProfessionalPaymentBillItem.getCatId());
            cancellationProfessionalBillItem.setDeptId(originalProfessionalPaymentBillItem.getDeptId());
            cancellationProfessionalBillItem.setInsId(originalProfessionalPaymentBillItem.getInsId());
            cancellationProfessionalBillItem.setDiscount(0 - originalProfessionalPaymentBillItem.getDiscount());
            cancellationProfessionalBillItem.setQty(1.0);
            cancellationProfessionalBillItem.setRate(originalProfessionalPaymentBillItem.getRate());

            cancellationProfessionalBillItem.setCreatedAt(new Date());
            cancellationProfessionalBillItem.setCreater(getSessionController().getLoggedUser());

            cancellationProfessionalBillItem.setPaidForBillFee(originalProfessionalPaymentBillItem.getPaidForBillFee());

            getBillItemFacede().create(cancellationProfessionalBillItem);

//            cancelBillComponents(cancellationProfessionalPaymentBill, cancellationProfessionalBillItem);
//            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + originalProfessionalPaymentBillItem.getId();
            List<BillFee> originalProfessionalPaymentFeesForBillItem = billBean.fetchBillFees(originalProfessionalPaymentBillItem);

            cancelBillFee(cancellationProfessionalPaymentBill, cancellationProfessionalBillItem, originalProfessionalPaymentFeesForBillItem);

//            //create BillFeePayments For cancel
//            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + cancellationProfessionalBillItem.getId();
//            List<BillFee> tmpC = getBillFeeFacade().findByJpql(sql);
//            getOpdPreSettleController().createOpdCancelRefundBillFeePayment(cancellationProfessionalPaymentBill, tmpC, cancellationBillPayments);
//            //
            newlyCreatedCancellationBillItems.add(cancellationProfessionalBillItem);

        }

        return newlyCreatedCancellationBillItems;
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

            getBillItemFacede().create(b);

            cancelBillComponents(cancellationBill, b);

            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
            cancelBillFee(cancellationBill, b, tmp);

//            //create BillFeePayments For cancel
//            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + b.getId();
//            List<BillFee> tmpC = getBillFeeFacade().findByJpql(sql);
//            getOpdPreSettleController().createOpdCancelRefundBillFeePayment(cancellationBill, tmpC, p);
//            //
            list.add(b);

        }

        return list;
    }

    private List<BillItem> cancelCcBillItems(Bill originalBill, Bill cancellationBill) {
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

            b.setNetValue(0 - nB.getNetValue());
            b.setGrossValue(0 - nB.getGrossValue());
            b.setRate(0 - nB.getRate());
            b.setVat(0 - nB.getVat());
            b.setVatPlusNetValue(0 - nB.getVatPlusNetValue());

            b.setCatId(nB.getCatId());
            b.setDeptId(nB.getDeptId());
            b.setInsId(nB.getInsId());
            b.setDiscount(0 - nB.getDiscount());
            b.setQty(1.0);
            b.setRate(nB.getRate());

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            b.setPaidForBillFee(nB.getPaidForBillFee());

            getBillItemFacede().create(b);

            cancelBillComponents(cancellationBill, b);

            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
            cancelBillFee(cancellationBill, b, tmp);

            list.add(b);

        }

        return list;
    }

    public void cancelBillFee(Bill cancellationProfessionalPaymentBill,
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

    public List<Bill> getBills() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.OpdBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.OpdBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        showAllBills = false;
        return bills;
    }

    public List<Bill> getPos() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyOrder);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyOrder);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getRequests() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyTransferRequest);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyTransferRequest);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getGrns() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyGrnBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyGrnBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getGrnReturns() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyGrnReturn);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyGrnReturn);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getInstitutionPaymentBills() {
        if (bills == null) {
            String sql;
            Map temMap = new HashMap();
            if (bills == null) {
                if (txtSearch == null || txtSearch.trim().equals("")) {
                    sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.id in"
                            + "(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType!=:btp and bt.referenceBill.billType!=:btp2) and b.billType=:type and b.createdAt between :fromDate and :toDate order by b.id";

                } else {
                    sql = "select b from BilledBill b where b.retired=false and"
                            + " b.id in(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType!=:btp and bt.referenceBill.billType!=:btp2) "
                            + "and b.billType=:type and b.createdAt between :fromDate and :toDate and ((b.staff.person.name) like '%" + txtSearch.toUpperCase() + "%'  or (b.staff.person.phone) like '%" + txtSearch.toUpperCase() + "%'  or (b.insId) like '%" + txtSearch.toUpperCase() + "%') order by b.id desc  ";
                }

                temMap.put("toDate", getToDate());
                temMap.put("fromDate", getFromDate());
                temMap.put("type", BillType.PaymentBill);
                temMap.put("btp", BillType.ChannelPaid);
                temMap.put("btp2", BillType.ChannelCredit);
                bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);

                if (bills == null) {
                    bills = new ArrayList<>();
                }
            }
        }
        return bills;

    }

    public String addPaymentToViewingBillForAdminToCorrectErrors() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No viewing Bill");
            return null;
        }
        List<Payment> newPayments = billService.createPayment(bill, paymentMethod, null);
        if (newPayments == null) {
            JsfUtil.addErrorMessage("Error");
            return null;
        }
        bill = viewingBill;
        return navigateToAdminBillByAtomicBillType();
    }

    public List<Bill> getUserBills() {
        List<Bill> userBills;
        if (getUser() == null) {
            userBills = new ArrayList<>();
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), BillType.OpdBill);
        }
        if (userBills == null) {
            userBills = new ArrayList<>();
        }
        return userBills;
    }

    public List<Bill> getOpdBills() {
        if (txtSearch == null || txtSearch.trim().equals("")) {
            bills = getBillBean().billsForTheDay(fromDate, toDate, BillType.OpdBill);
        } else {
            bills = getBillBean().billsFromSearch(txtSearch, fromDate, toDate, BillType.OpdBill);
        }

        if (bills == null) {
            bills = new ArrayList<>();
        }
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public String getTxtSearch() {
        return txtSearch;
    }

    public void setTxtSearch(String txtSearch) {
        this.txtSearch = txtSearch;
        //    recreateModel();
    }

    public Bill getBill() {
//        if (bill == null) {
//            bill = new Bill();
//        }
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public String navigateToCancelOpdBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }

        if (configOptionApplicationController.getBooleanValueByKey("Set the Original Bill PaymentMethod to Cancelation Bill")) {
            boolean moreThanOneIndividualBillsForTheBatchBillOfThisIndividualBill = billService.hasMultipleIndividualBillsForBatchBillOfThisIndividualBill(bill);
            if (moreThanOneIndividualBillsForTheBatchBillOfThisIndividualBill) {
                paymentMethod = bill.getPaymentMethod();
            } else {
                paymentMethod = null;
            }
        } else {
            paymentMethod = PaymentMethod.Cash;
        }
        createBillItemsAndBillFees();
        boolean flag = billController.checkBillValues(bill);
        bill.setTransError(flag);
        printPreview = false;
        return "/opd/bill_cancel?faces-redirect=true";
    }

    public boolean chackRefundORCancelBill(Bill bill) {
        return CommonFunctions.dateAfter24Hours(bill.getCreatedAt()).after(new Date());
    }

    public String navigateToViewSingleOpdBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        loadBillItemsAndBillFees(bill);
        if (configOptionController.getBooleanValueByKey("OPD Bill Cancelation is Limited to the Last 24 hours", OptionScope.APPLICATION, null, null, null)) {
            opdBillCancellationSameDay = chackRefundORCancelBill(bill);
        } else {
            opdBillCancellationSameDay = true;
        }

        if (configOptionController.getBooleanValueByKey("OPD Bill Refund Allowed to the Last 24 hours", OptionScope.APPLICATION, null, null, null)) {
            opdBillRefundAllowedSameDay = chackRefundORCancelBill(bill);
        } else {
            opdBillRefundAllowedSameDay = true;
        }
        findRefuendedBills(bill);
        paymentMethod = bill.getPaymentMethod();
        printPreview = false;
        return "/opd/bill_reprint?faces-redirect=true";
    }

    public String navigateToViewChannelBillSession() {
        return "/channel/manage_booking_by_date?faces-redirect=true";
    }

    private List<Bill> refuendedBills = new ArrayList();

    public void findRefuendedBills(Bill bill) {
        refuendedBills = new ArrayList<>();
        Map p = new HashMap();
        String jpql = "SELECT b"
                + " FROM Bill b"
                + " WHERE b.retired =:ret"
                + " and b.referenceBill =:refBill";

        p.put("ret", false);
        p.put("refBill", bill);

        refuendedBills = billFacade.findByJpql(jpql, p);
    }

    public List<Bill> getRefuendedBills() {
        return refuendedBills;
    }

    public void setRefuendedBills(List<Bill> refuendedBills) {
        this.refuendedBills = refuendedBills;
    }

    public String navigateToViewOpdBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill to Dsiplay");
            return "";
        }
        findRefuendedBills(viewingBill);
        return "/opd/view/opd_bill?faces-redirect=true";
    }

    public String navigateToViewIncomeBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill to Dsiplay");
            return "";
        }
        financialTransactionController.setCurrentBill(viewingBill);
        financialTransactionController.setCurrentBillPayments(viewingBillPayments);
        return "/cashier/income_bill_print?faces-redirect=true";
    }

    public String navigateToManageIncomeBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill to Dsiplay");
            return "";
        }
        financialTransactionController.setCurrentBill(viewingBill);
        financialTransactionController.setCurrentBillPayments(viewingBillPayments);
        return "/cashier/income_bill_reprint?faces-redirect=true";
    }

    public String navigateToCancelIncomeBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        billItems = billBean.fetchBillItems(bill);
        billFees = billBean.fetchBillFees(bill);
        billPayments = billBean.fetchBillPayments(bill);
        printPreview = false;
        return "/cashier/income_bill_cancel?faces-redirect=true";
    }

    public String navigateToViewCancelIncomeBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill to Dsiplay");
            return "";
        }
        financialTransactionController.setCurrentBill(viewingBill);
        financialTransactionController.setCurrentBillPayments(viewingBillPayments);
        return "/cashier/income_bill_cancellation_print?faces-redirect=true";
    }

    public String navigateToManageCancelIncomeBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill to Dsiplay");
            return "";
        }
        financialTransactionController.setCurrentBill(viewingBill);
        financialTransactionController.setCurrentBillPayments(viewingBillPayments);
        return "/cashier/income_bill_cancelled_reprint?faces-redirect=true";
    }

    public void cancelIncomeBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected to Canel");
            return;
        }
        if (bill.getBillTypeAtomic() != BillTypeAtomic.SUPPLEMENTARY_INCOME) {
            JsfUtil.addErrorMessage("Wrong Bill Type.");
            return;
        }
        if (billPayments == null) {
            JsfUtil.addErrorMessage("Payments Null.");
            return;
        }
        if (billPayments.isEmpty()) {
            JsfUtil.addErrorMessage("No Payments.");
            return;
        }
        bill = billFacade.findWithoutCache(bill.getId());
        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("Bill Is already cancelled.");
            return;
        }
        if (bill.isRefunded()) {
            JsfUtil.addErrorMessage("Bill Is refunded at least once.");
            return;
        }

        //TODO: Check Drawer Balance for each payment
        CancelledBill cancellationBill = new CancelledBill();
        cancellationBill.copy(bill);
        cancellationBill.copyValue(bill);
        cancellationBill.invertValueOfThisBill();

        cancellationBill.setCreatedAt(new Date());
        cancellationBill.setCreater(sessionController.getLoggedUser());
        cancellationBill.setComments(comment);
        cancellationBill.setBillType(BillType.SUPPLEMENTARY_INCOME_CANCELLED);
        cancellationBill.setBillTypeAtomic(BillTypeAtomic.SUPPLEMENTARY_INCOME_CANCELLED);

        getBill().setCancelled(true);
        getBill().setCancelledBill(cancellationBill);

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.SUPPLEMENTARY_INCOME_CANCELLED);
        cancellationBill.setDeptId(deptId);

        cancellationBill.setBilledBill(bill);

        billFacade.create(cancellationBill);

        bill.setCancelledBill(cancellationBill);
        billFacade.edit(bill);
        List<Payment> cancellationPayments = new ArrayList<>();
        for (Payment originalBp : billPayments) {
            Payment cancellationPayment = originalBp.createNewPaymentByCopyingAttributes();
            cancellationPayment.setPaidValue(0 - originalBp.getPaidValue());
            cancellationPayment.setCreatedAt(new Date());
            cancellationPayment.setCreater(sessionController.getLoggedUser());
            cancellationPayment.setReferancePayment(originalBp);
            cancellationPayment.setBill(cancellationBill);
            paymentFacade.create(cancellationPayment);
            cancellationPayments.add(cancellationPayment);
        }
        drawerController.updateDrawerForOuts(cancellationPayments);
        bill = cancellationBill;
        billPayments = cancellationPayments;
        printPreview = true;
    }

    public String navigateToViewExpenseBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill to Dsiplay");
            return "";
        }
        financialTransactionController.setCurrentBill(viewingBill);
        financialTransactionController.setCurrentBillPayments(viewingBillPayments);
        return "/cashier/expense_bill_print?faces-redirect=true";
    }

    public String navigateToManageExpenseBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill to Dsiplay");
            return "";
        }
        financialTransactionController.setCurrentBill(viewingBill);
        financialTransactionController.setCurrentBillPayments(viewingBillPayments);
        return "/cashier/expense_bill_reprint?faces-redirect=true";
    }

    public String navigateToCancelExpenseBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        billItems = billBean.fetchBillItems(bill);
        billFees = billBean.fetchBillFees(bill);
        billPayments = billBean.fetchBillPayments(bill);
        printPreview = false;
        return "/cashier/expense_bill_cancel?faces-redirect=true";
    }

    public String navigateToViewCancelExpenseBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill to Dsiplay");
            return "";
        }
        financialTransactionController.setCurrentBill(viewingBill);
        financialTransactionController.setCurrentBillPayments(viewingBillPayments);
        return "/cashier/expense_bill_cancellation_print?faces-redirect=true";
    }

    public String navigateToManageCancelExpenseBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill to Dsiplay");
            return "";
        }
        financialTransactionController.setCurrentBill(viewingBill);
        financialTransactionController.setCurrentBillPayments(viewingBillPayments);
        return "/cashier/expense_bill_cancelled_reprint?faces-redirect=true";
    }

    public void cancelExpenseBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected to Canel");
            return;
        }
        if (bill.getBillTypeAtomic() != BillTypeAtomic.OPERATIONAL_EXPENSES) {
            JsfUtil.addErrorMessage("Wrong Bill Type.");
            return;
        }
        if (billPayments == null) {
            JsfUtil.addErrorMessage("Payments Null.");
            return;
        }
        if (billPayments.isEmpty()) {
            JsfUtil.addErrorMessage("No Payments.");
            return;
        }
        bill = billFacade.findWithoutCache(bill.getId());
        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("Bill Is already cancelled.");
            return;
        }
        if (bill.isRefunded()) {
            JsfUtil.addErrorMessage("Bill Is refunded at least once.");
            return;
        }

        //TODO: Check Drawer Balance for each payment
        CancelledBill cancellationBill = new CancelledBill();
        cancellationBill.copy(bill);
        cancellationBill.copyValue(bill);
        cancellationBill.invertValueOfThisBill();

        cancellationBill.setCreatedAt(new Date());
        cancellationBill.setCreater(sessionController.getLoggedUser());
        cancellationBill.setComments(comment);
        cancellationBill.setBillType(BillType.OPERATIONAL_EXPENSES_CANCELLED);
        cancellationBill.setBillTypeAtomic(BillTypeAtomic.OPERATIONAL_EXPENSES_CANCELLED);

        getBill().setCancelled(true);
        getBill().setCancelledBill(cancellationBill);

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.OPERATIONAL_EXPENSES_CANCELLED);
        cancellationBill.setDeptId(deptId);

        cancellationBill.setBilledBill(bill);

        billFacade.create(cancellationBill);

        bill.setCancelledBill(cancellationBill);
        billFacade.edit(bill);
        List<Payment> cancellationPayments = new ArrayList<>();
        for (Payment originalBp : billPayments) {
            Payment cancellationPayment = originalBp.createNewPaymentByCopyingAttributes();
            cancellationPayment.setPaidValue(0 - originalBp.getPaidValue());
            cancellationPayment.setCreatedAt(new Date());
            cancellationPayment.setCreater(sessionController.getLoggedUser());
            cancellationPayment.setReferancePayment(originalBp);
            cancellationPayment.setBill(cancellationBill);
            paymentFacade.create(cancellationPayment);
            cancellationPayments.add(cancellationPayment);
        }
        drawerController.updateDrawerForIns(cancellationPayments);
        bill = cancellationBill;
        billPayments = cancellationPayments;
        printPreview = true;
    }

    public String navigateToAdminOpdBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill to Dsiplay");
            return "";
        }
        return "/opd/view/opd_bill_admin?faces-redirect=true";
    }

    public String navigateToAdminCcDepositBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill to Dsiplay");
            return "";
        }
        return "/opd/view/cc_deposit_bill_admin?faces-redirect=true";
    }

    public String navigateToAdminBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill to Dsiplay");
            return "";
        }
        return "/opd/view/bill_admin?faces-redirect=true";
    }

    public String navigateToBillListFromAdminBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill selected");
            return "";
        }
        return searchController.navigateToBillListFromBill(viewingBill);
    }

    public String navigateToViewOpdRefundBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill to Dsiplay");
            return "";
        }
        return "/opd/view/opd_refund_bill_admin?faces-redirect=true";
    }

    public String navigateToAdminOpdRefundBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill to Dsiplay");
            return "";
        }
        return "/opd/view/opd_refund_bill_admin?faces-redirect=true";
    }

    public String navigateToAdminOpdCancellationBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No Bill to Dsiplay");
            return "";
        }
        return "/opd/view/opd_cancellation_bill_admin?faces-redirect=true";
    }

    public String navigateToViewCancallationOpdBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        return "/opd/view/cancelled_opd_bill?faces-redirect=true";
    }

    public String navigateToReprintOpdProfessionalPaymentBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to Reprint");
            return "";
        }
        return "/opd/professional_payments/payment_bill_reprint?faces-redirect=true";
    }

    public String navigateToCancelOpdProfessionalPaymentBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        printPreview = false;
        return "/opd/professional_payments/payment_staff_bill_cancel?faces-redirect=true";
    }

    public String navigateToViewOpdProfessionalPaymentsDone() {
        return "/opd/professional_payments/opd_search_professional_payment_done?faces-redirect=true";
    }

    public String navigateToViewOpdProfessionalPaymentsDoneByUser() {
        searchController.getReportKeyWord().setWebUser(sessionController.getLoggedUser());
        searchController.createPaymentTableAll();
        return "/opd/professional_payments/opd_search_professional_payment_done?faces-redirect=true";
    }

    public String navigateToViewOpdProfessionalPaymentsDue() {
        return "/opd/professional_payments/opd_search_professional_payment_due?faces-redirect=true";
    }

    //    public String navigateToViewOpdPayProfessionalPayments() {
//        return "/opd/professional_payments/payment_staff_bill?faces-redirect=true";
//    }
    public String navigateToViewCancallationOpdbATCHBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        return "/opd/view/cancelled_opd_batch_bill?faces-redirect=true";
    }

    public String navigateToViewOpdBatchBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        return "/opd/view/opd_batch_bill?faces-redirect=true";
    }

    public String navigateToViewOpdCreditBatchBillSettle() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        cashRecieveBillController.setPrintPreview(true);
        cashRecieveBillController.setCurrent(getBill());
        return "/credit/credit_compnay_bill_opd?faces-redirect=true";
    }

    public String navigateToViewOpdProfessionalPaymentBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        return "/opd/view/opd_professional_payment?faces-redirect=true";
    }

    public String navigateToViewOpdProfessionalPaymentCancelledBill() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("Not Cancelled Yet");
            return "";
        }
        return "/opd/professional_payments/payment_staff_bill_cancel?faces-redirect=true";
    }

    public String navigateToManageOpdBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        if (configOptionController.getBooleanValueByKey("OPD Bill Cancelation is Limited to the Last 24 hours", OptionScope.APPLICATION, null, null, null)) {
            opdBillCancellationSameDay = chackRefundORCancelBill(bill);
        } else {
            opdBillCancellationSameDay = true;
        }
        if (configOptionController.getBooleanValueByKey("OPD Bill Refund Allowed to the Last 24 hours", OptionScope.APPLICATION, null, null, null)) {
            opdBillRefundAllowedSameDay = chackRefundORCancelBill(bill);
        } else {
            opdBillRefundAllowedSameDay = true;
        }
        paymentMethod = bill.getPaymentMethod();
        createBillItemsAndBillFees();
        billBean.checkBillItemFeesInitiated(bill);
        boolean flag = billController.checkBillValues(bill);
        bill.setTransError(flag);
        printPreview = false;
        return "/opd/bill_reprint?faces-redirect=true";
    }

    public String navigateToManageOpdPackageBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        if (configOptionController.getBooleanValueByKey("OPD Bill Cancelation is Limited to the Last 24 hours", OptionScope.APPLICATION, null, null, null)) {
            opdBillCancellationSameDay = chackRefundORCancelBill(bill);
        } else {
            opdBillCancellationSameDay = true;
        }

        if (configOptionController.getBooleanValueByKey("OPD Bill Refund Allowed to the Last 24 hours", OptionScope.APPLICATION, null, null, null)) {
            opdBillRefundAllowedSameDay = chackRefundORCancelBill(bill);
        } else {
            opdBillRefundAllowedSameDay = true;
        }
        paymentMethod = bill.getPaymentMethod();
        createBillItemsAndBillFees();
        billBean.checkBillItemFeesInitiated(bill);
        boolean flag = billController.checkBillValues(bill);
        bill.setTransError(flag);
        printPreview = false;
        return "/opd/package_bill_reprint?faces-redirect=true";
    }

    public String navigateToViewChannelingProfessionalPaymentBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        channelSearchController.setBill(bill);
        channelSearchController.setPrintPreview(true);
        return "/channel/channel_payment_bill_reprint?faces-redirect=true";
    }

    public String navigateToViewCashierShiftShortageBill(Bill bill) {
        loadBillDetails(bill);
        return "/cashier/shift_shortage_bill_reprint?faces-redirect=true";
    }
//    //to do
//    public String navigateToViewOpdProfessionalPaymentBill() {
//        if (bill == null) {
//            JsfUtil.addErrorMessage("No Bill Selected");
//            return "";
//        }
//        setBill(bill);
//        printPreview = true;
//        return "/payment_bill_reprint.xhtml?faces-redirect=true";
//    }

    public String navigateToDownloadBillsAndBillItems() {
        return "/analytics/download_bills?faces-redirect=true";
    }

    public String navigateToViewPayment() {
        if (payment == null) {
            JsfUtil.addErrorMessage("No Payment is Selected");
            return null;
        }
        return "/common/view_payment?faces-redirect=true";
    }

    public String navigateToEditPayment() {
        if (payment == null) {
            JsfUtil.addErrorMessage("No Payment is Selected");
            return null;
        }
        return "/admin/data/edit_payment?faces-redirect=true";
    }

    public String navigateToViewBillByAtomicBillTypeByBillId(Long BillId) {
        if (BillId == null) {
            JsfUtil.addErrorMessage("Bill ID is required");
            return null;
        }

        Bill foundBill = billFacade.find(BillId);
        if (foundBill == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }

        this.bill = foundBill;
        return navigateToViewBillByAtomicBillType();
    }

    public String navigateToViewBillByAtomicBillTypeByBillItemId(Long BillItemId) {
        if (BillItemId == null) {
            JsfUtil.addErrorMessage("Bill Item ID is required");
            return null;
        }

        BillItem foundBillItem = billItemFacede.find(BillItemId);
        if (foundBillItem == null) {
            JsfUtil.addErrorMessage("Bill Item not found");
            return null;
        }

        if (foundBillItem.getBill() == null) {
            JsfUtil.addErrorMessage("Associated Bill not found");
            return null;
        }

        this.bill = foundBillItem.getBill();
        return navigateToViewBillByAtomicBillType();
    }

    public String navigateToViewBillByAtomicBillType() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        if (bill.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No Bill type");
            return null;
        }
        BillTypeAtomic billTypeAtomic = bill.getBillTypeAtomic();
        loadBillDetails(bill);
        switch (billTypeAtomic) {
            case OPD_BILL_REFUND:
                return navigateToViewOpdRefundBill();
            case OPD_BILL_CANCELLATION:
                return navigateToManageOpdBill();
            case OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER:
                return navigateToManageOpdBill();
            case OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
                return navigateToManageOpdBill();
            case OPD_PROFESSIONAL_PAYMENT_BILL:
                return navigateToManageOpdBill();
            case OPD_BILL_WITH_PAYMENT:
                return navigateToViewOpdBill();
            case OPD_BATCH_BILL_WITH_PAYMENT:
                return navigateToViewOpdBatchBill();
            case OPD_CREDIT_COMPANY_PAYMENT_RECEIVED:
                return navigateToViewOpdCreditBatchBillSettle();
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES:
                return navigateToViewOpdProfessionalPaymentBill();

            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES_RETURN:
                return navigateToViewOpdProfessionalPaymentCancelledBill();
            case CHANNEL_BOOKING_WITH_PAYMENT:
                return "";
            case CC_BILL:
                return navigateToViewCcBill(bill);

            case CC_BILL_CANCELLATION:
                return navigateToViewCcBillCancellation(bill);

            case CC_BILL_REFUND:
                return navigateToViewCcBillRefund(bill);

            case CC_CREDIT_NOTE:
                return navigateToViewCcCreditNote(bill);

            case CC_DEBIT_NOTE:
                return navigateToViewCcDebitNote(bill);

            case CC_CREDIT_NOTE_CANCELLATION:
                return navigateToViewCcCreditNoteCancellation(bill);

            case CC_DEBIT_NOTE_CANCELLATION:
                return navigateToViewCcDebitNoteCancellation(bill);

            case CC_PAYMENT_CANCELLATION_BILL:
                return navigateToViewCcPaymentCancellationBill(bill);

            case CC_PAYMENT_MADE_BILL:
                return navigateToViewCcPaymentMadeBill(bill);

            case CC_PAYMENT_MADE_CANCELLATION_BILL:
                return navigateToViewCcPaymentMadeCancellationBill(bill);

            case CC_PAYMENT_RECEIVED_BILL:
                return navigateToViewCcPaymentReceivedBill(bill);

            case CHANNEL_REFUND:
                return "";

            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_SESSION:
                return navigateToViewChannelingProfessionalPaymentBill();

            case DIRECT_ISSUE_INWARD_MEDICINE:
                return navigateToViewPharmacyDirectIssueForInpatientBill();
            case ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN:
            case DIRECT_ISSUE_INWARD_MEDICINE_RETURN:
                return navigateToViewPharmacyDirectIssueReturnForInpatientBill();

            case DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION:
                return navigateToViewPharmacyDirectIssueCancellationForInpatientBill();

            case PHARMACY_RETAIL_SALE_PRE:
                return navigateToViewPharmacyPreBill();
            case PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER:
                pharmacySaleController.setPrintBill(bill);
                return pharmacySaleController.navigateToSaleBillForCashierPrint();
//                return navigateToViewPharmacySettledPreBill();
            case PHARMACY_RETAIL_SALE:
                pharmacyBillSearch.setBill(bill);
                return pharmacyBillSearch.navigatePharmacyReprintRetailBill();

            case PHARMACY_RETAIL_SALE_CANCELLED:
                pharmacyBillSearch.setBill(bill);
                return pharmacyBillSearch.navigateToViewPharmacyRetailCancellationBill();
            case PHARMACY_RETAIL_SALE_CANCELLED_PRE:
                pharmacyBillSearch.setBill(bill);
                return pharmacyBillSearch.navigateToViewPharmacyRetailCancellationPreBill();

            case CHANNEL_PAYMENT_FOR_BOOKING_BILL:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_FOR_AGENCIES:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_FOR_AGENCIES_RETURN:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_RETURN:

            case PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS:
            case PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER:
            case PHARMACY_RETAIL_SALE_REFUND:
            case PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY:
            case PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS:
            case PHARMACY_SALE_WITHOUT_STOCK:
            case PHARMACY_SALE_WITHOUT_STOCK_PRE:
            case PHARMACY_SALE_WITHOUT_STOCK_CANCELLED:
            case PHARMACY_SALE_WITHOUT_STOCK_REFUND:
            case PHARMACY_WHOLESALE:
            case PHARMACY_WHOLESALE_PRE:
            case PHARMACY_WHOLESALE_CANCELLED:
            case PHARMACY_WHOLESALE_REFUND:
            case PHARMACY_DIRECT_ISSUE:
                pharmacyBillSearch.setBill(bill);
                return pharmacyBillSearch.navigateToViewPharmacyBill();

            case PHARMACY_ORDER:
            case PHARMACY_ORDER_PRE:
            case PHARMACY_ORDER_CANCELLED:
            case PHARMACY_ORDER_APPROVAL:
            case PHARMACY_ORDER_APPROVAL_CANCELLED:
//            case PHARMACY_DIRECT_PURCHASE:
//            case PHARMACY_DIRECT_PURCHASE_CANCELLED:
//            case PHARMACY_DIRECT_PURCHASE_REFUND:
            case PHARMACY_GRN:
            case PHARMACY_GRN_PRE:
            case PHARMACY_GRN_WHOLESALE:
//            case PHARMACY_GRN_CANCELLED:
            case PHARMACY_GRN_REFUND:
//            case PHARMACY_GRN_RETURN:
            case PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL:
            case PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_CANCELLED:
            case PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_REFUND:
            case PHARMACY_WHOLESALE_GRN_BILL:
            case PHARMACY_WHOLESALE_GRN_BILL_CANCELLED:
            case PHARMACY_WHOLESALE_GRN_BILL_REFUND:
            case PHARMACY_GRN_PAYMENT:
            case PHARMACY_GRN_PAYMENT_CANCELLED:
            case PHARMACY_ADJUSTMENT:
            case PHARMACY_ADJUSTMENT_CANCELLED:
//            case PHARMACY_TRANSFER_REQUEST:
            case PHARMACY_TRANSFER_REQUEST_PRE:
            case PHARMACY_TRANSFER_REQUEST_CANCELLED:
//            case PHARMACY_ISSUE:
//            case PHARMACY_ISSUE_CANCELLED:

            case PHARMACY_DIRECT_ISSUE_CANCELLED:
            case PHARMACY_RECEIVE_PRE:
//            case PHARMACY_RECEIVE_CANCELLED:
            case MULTIPLE_PHARMACY_ORDER_CANCELLED_BILL:
            case PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION:
                pharmacyBillSearch.setBill(bill);
                return pharmacyBillSearch.navigateToViewPharmacyGrn();
            case SUPPLEMENTARY_INCOME:
                return navigateToViewIncomeBill();
            case OPERATIONAL_EXPENSES:
                return navigateToViewExpenseBill();
            case SUPPLEMENTARY_INCOME_CANCELLED:
                return navigateToViewCancelIncomeBill();
            case OPERATIONAL_EXPENSES_CANCELLED:
                return navigateToViewCancelExpenseBill();
            case PHARMACY_ISSUE_RETURN:
                return navigateToPharmacyIssueReturn();
            case PHARMACY_ISSUE:
            case PHARMACY_DISPOSAL_ISSUE:
                return navigateToPharmacyIssue();
            case PHARMACY_ISSUE_CANCELLED:
                return navigateToPharmacyIssueCancelled();

            case PHARMACY_RECEIVE:
            case PHARMACY_RECEIVE_CANCELLED:
                return navigateToPharmayReceive();

            case PHARMACY_DIRECT_PURCHASE:
                return navigateToDirectPurchaseBillView();
            case PHARMACY_DIRECT_PURCHASE_CANCELLED:
                return navigateToDirectPurchaseCancellationBillView();
            case PHARMACY_DIRECT_PURCHASE_REFUND:
                return navigateToDirectPurchaseReturnBillView();
            case PHARMACY_RETURN_WITHOUT_TREASING:
                return navigateToPharmacyReturnWithoutTreasingBillView();

            case PHARMACY_GRN_RETURN:
                return navigateToPharmacyGrnReturnBillView();
            case PHARMACY_GRN_CANCELLED:
                return navigateToPharmacyGrnCancellationBillView();
            case PHARMACY_TRANSFER_REQUEST:
                return navigateToPharmacyTransferRequestBillView();
            case PHARMACY_RETAIL_SALE_PRE_ADD_TO_STOCK:
                return navigateToPharmacyAddToStockBillPreview();
            case PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS_PREBILL:
                return navigateToPharmacyRetailSaleReturnBillPreview();
            case ISSUE_MEDICINE_ON_REQUEST_INWARD:
                return navigateToPharmacyBhtIssueBillPreview();
            case REQUEST_MEDICINE_INWARD:
                return navigateToPharmacyBhtRequestBillPreview();
            case ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION:
                return navigateToPharmacyBhtIssueCancellationBillView();

        }

        return "";
    }

    public String navigateToViewBillByAtomicBillTypeByDeptId(String deptId) {
        bill = null;

        if (deptId == null || deptId.isEmpty()) {
            JsfUtil.addErrorMessage("No Department is Selected");
            return null;
        }

        setBillByDeptId(deptId);
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        if (bill.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No Bill type");
            return null;
        }
        BillTypeAtomic billTypeAtomic = bill.getBillTypeAtomic();
        loadBillDetails(bill);
        switch (billTypeAtomic) {
            case OPD_BILL_REFUND:
                return navigateToViewOpdRefundBill();
            case OPD_BILL_CANCELLATION:
                return navigateToManageOpdBill();
            case OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER:
                return navigateToManageOpdBill();
            case OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
                return navigateToManageOpdBill();
            case OPD_PROFESSIONAL_PAYMENT_BILL:
                return navigateToManageOpdBill();
            case OPD_BILL_WITH_PAYMENT:
                return navigateToViewOpdBill();
            case OPD_BATCH_BILL_WITH_PAYMENT:
                return navigateToViewOpdBatchBill();
            case OPD_CREDIT_COMPANY_PAYMENT_RECEIVED:
                return navigateToViewOpdCreditBatchBillSettle();
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES:
                return navigateToViewOpdProfessionalPaymentBill();

            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES_RETURN:
                return navigateToViewOpdProfessionalPaymentCancelledBill();
            case CHANNEL_BOOKING_WITH_PAYMENT:
                return "";
            case CC_BILL:
                return navigateToViewCcBill(bill);

            case CC_BILL_CANCELLATION:
                return navigateToViewCcBillCancellation(bill);

            case CC_BILL_REFUND:
                return navigateToViewCcBillRefund(bill);

            case CC_CREDIT_NOTE:
                return navigateToViewCcCreditNote(bill);

            case CC_DEBIT_NOTE:
                return navigateToViewCcDebitNote(bill);

            case CC_CREDIT_NOTE_CANCELLATION:
                return navigateToViewCcCreditNoteCancellation(bill);

            case CC_DEBIT_NOTE_CANCELLATION:
                return navigateToViewCcDebitNoteCancellation(bill);

            case CC_PAYMENT_CANCELLATION_BILL:
                return navigateToViewCcPaymentCancellationBill(bill);

            case CC_PAYMENT_MADE_BILL:
                return navigateToViewCcPaymentMadeBill(bill);

            case CC_PAYMENT_MADE_CANCELLATION_BILL:
                return navigateToViewCcPaymentMadeCancellationBill(bill);

            case CC_PAYMENT_RECEIVED_BILL:
                return navigateToViewCcPaymentReceivedBill(bill);

            case CHANNEL_REFUND:
                return "";

            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_SESSION:
                return navigateToViewChannelingProfessionalPaymentBill();

            case DIRECT_ISSUE_INWARD_MEDICINE:
                return navigateToViewPharmacyDirectIssueForInpatientBill();
            case DIRECT_ISSUE_INWARD_MEDICINE_RETURN:
                return navigateToViewPharmacyDirectIssueReturnForInpatientBill();

            case DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION:
                return navigateToViewPharmacyDirectIssueCancellationForInpatientBill();
            case PHARMACY_RETAIL_SALE_PRE:
                return navigateToViewPharmacyPreBill();
            case PHARMACY_RETAIL_SALE:
                pharmacyBillSearch.setBill(bill);
                return pharmacyBillSearch.navigatePharmacyReprintRetailBill();

            case PHARMACY_RETAIL_SALE_CANCELLED:
                pharmacyBillSearch.setBill(bill);
                return pharmacyBillSearch.navigateToViewPharmacyRetailCancellationBill();
            case PHARMACY_RETAIL_SALE_CANCELLED_PRE:
                pharmacyBillSearch.setBill(bill);
                return pharmacyBillSearch.navigateToViewPharmacyRetailCancellationPreBill();
            case PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER:
                return navigateToViewPharmacySettledPreBill();

            case CHANNEL_PAYMENT_FOR_BOOKING_BILL:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_FOR_AGENCIES:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_FOR_AGENCIES_RETURN:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_RETURN:

            case PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS:
            case PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS_PREBILL:
            case PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER:
            case PHARMACY_RETAIL_SALE_REFUND:
            case PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY:
            case PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS:
            case PHARMACY_SALE_WITHOUT_STOCK:
            case PHARMACY_SALE_WITHOUT_STOCK_PRE:
            case PHARMACY_SALE_WITHOUT_STOCK_CANCELLED:
            case PHARMACY_SALE_WITHOUT_STOCK_REFUND:
            case PHARMACY_RETAIL_SALE_PRE_ADD_TO_STOCK:
            case PHARMACY_WHOLESALE:
            case PHARMACY_WHOLESALE_PRE:
            case PHARMACY_WHOLESALE_CANCELLED:
            case PHARMACY_WHOLESALE_REFUND:
            case PHARMACY_DIRECT_ISSUE:
                pharmacyBillSearch.setBill(bill);
                return pharmacyBillSearch.navigateToViewPharmacyBill();

            case PHARMACY_ORDER:
            case PHARMACY_ORDER_PRE:
            case PHARMACY_ORDER_CANCELLED:
            case PHARMACY_ORDER_APPROVAL:
            case PHARMACY_ORDER_APPROVAL_CANCELLED:
//            case PHARMACY_DIRECT_PURCHASE:
//            case PHARMACY_DIRECT_PURCHASE_CANCELLED:
//            case PHARMACY_DIRECT_PURCHASE_REFUND:
            case PHARMACY_GRN:
            case PHARMACY_GRN_PRE:
            case PHARMACY_GRN_WHOLESALE:
//            case PHARMACY_GRN_CANCELLED:
            case PHARMACY_GRN_REFUND:
//            case PHARMACY_GRN_RETURN:
            case PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL:
            case PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_CANCELLED:
            case PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_REFUND:
            case PHARMACY_WHOLESALE_GRN_BILL:
            case PHARMACY_WHOLESALE_GRN_BILL_CANCELLED:
            case PHARMACY_WHOLESALE_GRN_BILL_REFUND:
            case PHARMACY_GRN_PAYMENT:
            case PHARMACY_GRN_PAYMENT_CANCELLED:
            case PHARMACY_ADJUSTMENT:
            case PHARMACY_ADJUSTMENT_CANCELLED:
            case PHARMACY_TRANSFER_REQUEST:
            case PHARMACY_TRANSFER_REQUEST_PRE:
            case PHARMACY_TRANSFER_REQUEST_CANCELLED:
//            case PHARMACY_ISSUE:
//            case PHARMACY_ISSUE_CANCELLED:

            case PHARMACY_DIRECT_ISSUE_CANCELLED:
            case PHARMACY_RECEIVE_PRE:
//            case PHARMACY_RECEIVE_CANCELLED:
            case MULTIPLE_PHARMACY_ORDER_CANCELLED_BILL:
            case PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION:
                pharmacyBillSearch.setBill(bill);
                return pharmacyBillSearch.navigateToViewPharmacyGrn();
            case SUPPLEMENTARY_INCOME:
                return navigateToViewIncomeBill();
            case OPERATIONAL_EXPENSES:
                return navigateToViewExpenseBill();
            case SUPPLEMENTARY_INCOME_CANCELLED:
                return navigateToViewCancelIncomeBill();
            case OPERATIONAL_EXPENSES_CANCELLED:
                return navigateToViewCancelExpenseBill();
            case PHARMACY_ISSUE_RETURN:
                return navigateToPharmacyIssueReturn();
            case PHARMACY_ISSUE:
                return navigateToPharmacyIssue();
            case PHARMACY_ISSUE_CANCELLED:
                return navigateToPharmacyIssueCancelled();

            case PHARMACY_RECEIVE:
            case PHARMACY_RECEIVE_CANCELLED:
                return navigateToPharmayReceive();

            case PHARMACY_DIRECT_PURCHASE:
                return navigateToDirectPurchaseBillView();
            case PHARMACY_DIRECT_PURCHASE_CANCELLED:
                return navigateToDirectPurchaseCancellationBillView();
            case PHARMACY_DIRECT_PURCHASE_REFUND:
                return navigateToDirectPurchaseReturnBillView();

            case PHARMACY_DONATION_BILL:
                return navigateToDonationBillView();
            case PHARMACY_DONATION_BILL_CANCELLED:
                return navigateToDonationBillCancellationView();
            case PHARMACY_DONATION_BILL_REFUND:
                return navigateToDonationBillRefundView();

            case PHARMACY_RETURN_WITHOUT_TREASING:
                return navigateToPharmacyReturnWithoutTreasingBillView();

            case PHARMACY_GRN_RETURN:
                return navigateToPharmacyGrnReturnBillView();
            case PHARMACY_GRN_CANCELLED:
                return navigateToPharmacyGrnCancellationBillView();

        }

        return "";
    }

    public void setBillByDeptId(String deptId) {
        if (deptId == null || deptId.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please Enter Department Id");
            return;
        }
        String sql = "SELECT b FROM Bill b WHERE b.deptId = :deptId AND b.retired = false";
        Map<String, Object> params = new HashMap<>();
        params.put("deptId", deptId);

        bills = getBillFacade().findByJpql(sql, params);

        if (bills != null && !bills.isEmpty()) {
            this.bill = bills.get(0);
        } else {
            this.bill = null;
            JsfUtil.addErrorMessage("No bills found for the given Department ID");
        }
    }

    private void prepareToPharmacyCancellationBill() {
        Bill original = bill.getReferenceBill();
        if (original == null) {
            JsfUtil.addErrorMessage("Original bill not found for this cancellation / return.");
            return;
        }
        loadBillDetails(original);
        pharmacyBillSearch.setBill(original);
        loadBillDetails(bill);
        pharmacyBillSearch.getBill().setCancelledBill(bill);
        pharmacyBillSearch.setPrintPreview(true);

    }

    public String navigateToViewPharmacyDirectIssueCancellationForInpatientBill() {
        prepareToPharmacyCancellationBill();
        return "/inward/pharmacy_cancel_bill_retail_bht";
    }

    public String navigateToPharmacyIssueCancelled() {
        prepareToPharmacyCancellationBill();
        if (bill.getBillType() == BillType.PharmacyTransferIssue) {
            return "/pharmacy/pharmacy_cancel_transfer_issue";
        } else {
            return "/pharmacy/pharmacy_cancel_bill_unit_issue";
        }
    }

    public String navigateToViewPharmacyPreBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        loadBillDetails(bill);
        return "/pharmacy/view/pharmacy_pre_bill_view?faces-redirect=true";
    }

    @Deprecated // Use pharmacySaleController.navigateToSaleBillForCashierPrint();
    public String navigateToViewPharmacySettledPreBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        loadBillDetails(bill);
        pharmacyBillSearch.setBill(bill);
        return "/pharmacy/pharmacy_reprint_bill_sale_cashier?faces-redirect=true";
    }

    public String navigateToPharmacyGrnCancellationBillView() {
        prepareToPharmacyCancellationBill();
        return "/pharmacy/pharmacy_cancel_grn";
    }

    public String navigateToPharmacyBhtIssueCancellationBillView() {
        prepareToPharmacyCancellationBill();
        return "/inward/bht_bill_cancel";
    }

    public String navigateToPharmacyGrnReturnBillView() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        loadBillDetails(bill);
        boolean approvalOnly = configOptionApplicationController.getBooleanValueByKey("GRN Returns is only after Approval", true);
        if (approvalOnly) {
            grnReturnApprovalController.setGrnBill(bill);
            grnReturnApprovalController.prepareReturnRequest();
            return "/pharmacy/pharmacy_grn_return_request?faces-redirect=true";
        }
        grnReturnWithCostingController.resetValuesForReturn();
        boolean manageCosting = configOptionApplicationController.getBooleanValueByKey("Manage Costing", true);
        if (manageCosting) {
            grnReturnWithCostingController.setBill(bill);
            grnReturnWithCostingController.prepareReturnBill();
            grnReturnWithCostingController.setPrintPreview(false);
            return "/pharmacy/grn_return_with_costing?faces-redirect=true";
        } else {
            goodsReturnController.setReturnBill(bill);
            goodsReturnController.setPrintPreview(false);
            return "/pharmacy/pharmacy_return_good?faces-redirect=true";
        }
    }

    public String navigateToPharmacyIssue() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        loadBillDetails(bill);
        if (bill.getBillType() == BillType.PharmacyTransferIssue) {
            transferIssueController.setPrintPreview(true);
            transferIssueController.setIssuedBill(bill);
            return "/pharmacy/pharmacy_transfer_issue";
        } else {
            pharmacyIssueController.setBillPreview(true);
            pharmacyIssueController.setPrintBill(bill);
            return "/pharmacy/pharmacy_issue";
        }

    }

    public String navigateToDirectPurchaseBillView() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        BilledBill bb = (BilledBill) bill;
        viewingBill = billBean.fetchBill(bb.getId());
        loadBillDetails(bb);
        pharmacyPurchaseController.setPrintPreview(true);
        pharmacyPurchaseController.setBill(bb);
        return "/pharmacy/pharmacy_purchase";
    }

    public String navigateToDonationBillView() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        BilledBill bb = (BilledBill) bill;
        viewingBill = billBean.fetchBill(bb.getId());
        loadBillDetails(bb);
        pharmacyPurchaseController.setPrintPreview(true);
        pharmacyPurchaseController.setBill(bb);
        return "/pharmacy/pharmacy_donation_bill";
    }

    public String navigateToDonationBillCancellationView() {
        prepareToPharmacyCancellationBill();
        return "/pharmacy/pharmacy_donation_bill_cancelled";
    }

    public String navigateToDonationBillRefundView() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        viewingBill = billBean.fetchBill(bill.getId());
        loadBillDetails(bill);
        pharmacyPurchaseController.setPrintPreview(true);
        // For refund bills, we may need to access the original bill
        Bill originalBill = bill.getReferenceBill();
        if (originalBill instanceof BilledBill) {
            pharmacyPurchaseController.setBill((BilledBill) originalBill);
        } else {
            JsfUtil.addErrorMessage("Original donation bill not found for this refund");
            return null;
        }
        return "/pharmacy/pharmacy_donation_bill_refund";
    }

    public String navigateToPharmacyTransferRequestBillView() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        BilledBill bb = (BilledBill) bill;
        viewingBill = billBean.fetchBill(bb.getId());
        loadBillDetails(bb);
        transferRequestController.setPrintPreview(true);
        transferRequestController.setBill(bb);
        return "/pharmacy/pharmacy_transfer_request";
    }

    public String navigateToDirectPurchaseCancellationBillView() {
        prepareToPharmacyCancellationBill();
        return "/pharmacy/pharmacy_cancel_purchase?faces-redirect=true";
    }

    public String navigateToDirectPurchaseReturn() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        directPurchaseReturnController.resetValuesForReturn();
        loadBillDetails(bill);
        boolean manageCosting = configOptionApplicationController.getBooleanValueByKey("Manage Costing", true);
        if (manageCosting) {
            directPurchaseReturnController.setBill(bill);
            directPurchaseReturnController.prepareReturnBill();
            directPurchaseReturnController.setPrintPreview(false);
            return "/pharmacy/direct_purchase_return?faces-redirect=true";
        } else {
            purchaseReturnController.makeNull();
            purchaseReturnController.setBill(bill);
            purchaseReturnController.setPrintPreview(false);
            return "/pharmacy/pharmacy_return_purchase?faces-redirect=true";
        }
    }

    public String navigateToDirectPurchaseReturnBillView() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        loadBillDetails(bill);
        boolean manageCosting = configOptionApplicationController.getBooleanValueByKey("Manage Costing", true);
        if (manageCosting) {
            directPurchaseReturnController.setReturnBill(bill);
            directPurchaseReturnController.setPrintPreview(true);
            return "/pharmacy/direct_purchase_return?faces-redirect=true";
        } else {
            purchaseReturnController.setReturnBill(bill);
            purchaseReturnController.setPrintPreview(true);
            return "/pharmacy/pharmacy_return_purchase?faces-redirect=true";
        }
    }

    public String navigateToPharmacyReturnWithoutTreasingBillView() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        loadBillDetails(bill);
        pharmacyReturnwithouttresing.setBillPreview(true);
        pharmacyReturnwithouttresing.setPrintBill(bill);
        return "/pharmacy/pharmacy_return_withouttresing";
    }

    public String navigateToPharmacyAddToStockBillPreview() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        loadBillDetails(bill);
        searchController.setPrintPreview(true);
        searchController.setBill(bill);
        return "/pharmacy/pharmacy_search_pre_bill_not_paid";
    }

    public String navigateToPharmacyRetailSaleReturnBillPreview() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        loadBillDetails(bill);
        saleReturnController.setPrintPreview(true);
        saleReturnController.setReturnBill(bill);
        return "/pharmacy/pharmacy_bill_return_retail";
    }

    public String navigateToPharmacyBhtIssueBillPreview() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        loadBillDetails(bill);
        pharmacySaleBhtController.setBillPreview(true);
        pharmacySaleBhtController.setPrintBill(bill);
        pharmacySaleBhtController.setPatientEncounter(bill.getPatientEncounter());
        return "/ward/ward_pharmacy_bht_issue";
    }

    public String navigateToPharmacyBhtRequestBillPreview() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        loadBillDetails(bill);
        pharmacyRequestForBhtController.setBillPreview(true);
        pharmacyRequestForBhtController.setPrintBill(bill);
        pharmacyRequestForBhtController.setPatientEncounter(bill.getPatientEncounter());
        return "/ward/ward_pharmacy_bht_issue_request_bill";
    }

    public String navigateToViewPharmacyDirectIssueForInpatientBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        loadBillDetails(bill);
        pharmacySaleBhtController.setBillPreview(true);
        pharmacySaleBhtController.setPrintBill(bill);
        pharmacySaleBhtController.setPatientEncounter(bill.getPatientEncounter());
        return "/inward/pharmacy_bill_issue_bht";
    }

    public String navigateToViewPharmacyDirectIssueReturnForInpatientBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        loadBillDetails(bill.getReferenceBill());
        bhtIssueReturnController.setBill(bill.getReferenceBill());
        loadBillDetails(bill);
        bhtIssueReturnController.setReturnBill(bill);
        bhtIssueReturnController.setPrintPreview(true);
        return "/inward/pharmacy_bill_return_bht_issue";
    }

    public String navigateToPharmayReceive() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        loadBillDetails(bill);
        transferReceiveController.setPrintPreview(true);
        transferReceiveController.setReceivedBill(bill);
        return "/pharmacy/pharmacy_transfer_receive";
    }

    public String navigateToPharmacyIssueReturn() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        loadBillDetails(bill);
        issueReturnController.setPrintPreview(true);
        issueReturnController.setReturnBill(bill);
        return "/pharmacy/pharmacy_bill_return_issue";
    }

    public String navigateToAdminBillByAtomicBillType() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        if (bill.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No Bill type");
            return null;
        }
        BillTypeAtomic billTypeAtomic = bill.getBillTypeAtomic();
        loadBillDetails(bill);
        return navigateToAdminBill();
    }

    public String navigateToManageBillByAtomicBillType() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        if (bill.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No Bill type");
            return null;
        }
        BillTypeAtomic billTypeAtomic = bill.getBillTypeAtomic();
        loadBillDetails(bill);
        switch (billTypeAtomic) {
            case PACKAGE_OPD_BILL_WITH_PAYMENT:
                return navigateToManageOpdPackageBill();
            case PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT:
                return billPackageController.navigateToManageOpdPackageBatchBill(bill);

            case PHARMACY_RETAIL_SALE_CANCELLED:
                pharmacyBillSearch.setBill(bill);
                return pharmacyBillSearch.navigateToViewPharmacyGrn();
            case OPD_BILL_REFUND:
                return navigateToManageOpdBill();
            case OPD_BILL_CANCELLATION:
                return navigateToManageOpdBill();
            case OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER:
                return navigateToManageOpdBill();
            case OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
                return navigateToManageOpdBill();
            case OPD_PROFESSIONAL_PAYMENT_BILL:
                return navigateToManageOpdBill();
            case OPD_BILL_WITH_PAYMENT:
                return navigateToManageOpdBill();
            case OPD_BATCH_BILL_WITH_PAYMENT:
                return opdBillController.navigateToViewOpdBatchBill(bill);
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES:
                return navigateToViewOpdProfessionalPaymentBill();
            case CHANNEL_BOOKING_WITH_PAYMENT:
                return "";
            case CC_BILL:
                return navigateToViewCcBill(bill);

            case CC_BILL_CANCELLATION:
                return navigateToViewCcBillCancellation(bill);

            case CC_BILL_REFUND:
                return navigateToViewCcBillRefund(bill);

            case CC_CREDIT_NOTE:
                return navigateToViewCcCreditNote(bill);

            case CC_DEBIT_NOTE:
                return navigateToViewCcDebitNote(bill);

            case CC_CREDIT_NOTE_CANCELLATION:
                return navigateToViewCcCreditNoteCancellation(bill);

            case CC_DEBIT_NOTE_CANCELLATION:
                return navigateToViewCcDebitNoteCancellation(bill);

            case CC_PAYMENT_CANCELLATION_BILL:
                return navigateToViewCcPaymentCancellationBill(bill);

            case CC_PAYMENT_MADE_BILL:
                return navigateToViewCcPaymentMadeBill(bill);

            case CC_PAYMENT_MADE_CANCELLATION_BILL:
                return navigateToViewCcPaymentMadeCancellationBill(bill);

            case CC_PAYMENT_RECEIVED_BILL:
                return navigateToViewCcPaymentReceivedBill(bill);

            case CHANNEL_REFUND:
                return "";
            case CHANNEL_PAYMENT_FOR_BOOKING_BILL:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_FOR_AGENCIES:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_FOR_AGENCIES_RETURN:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_RETURN:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_SESSION:
                return navigateToViewChannelingProfessionalPaymentBill();
            case SUPPLEMENTARY_INCOME:
                return navigateToManageIncomeBill();
            case OPERATIONAL_EXPENSES:
                return navigateToManageExpenseBill();
            case SUPPLEMENTARY_INCOME_CANCELLED:
                return navigateToManageCancelIncomeBill();
            case OPERATIONAL_EXPENSES_CANCELLED:
                return navigateToManageCancelExpenseBill();
            case FUND_SHIFT_SHORTAGE_BILL:
                return navigateToViewCashierShiftShortageBill(bill);
            //                opdBillController.setBill(bill);
//                return opdBillController.navigateToViewPackageBatchBill();

        }

        return "";
    }

    public String navigateToViewCcBill(Bill bill) {
        loadBillDetails(bill); // Load the bill details
        return "/collecting_centre/view/cc_bill_view?faces-redirect=true";
    }

    public String navigateToViewCcBillCancellation(Bill bill) {
        loadBillDetails(bill);
        return "/collecting_centre/view/cc_bill_cancellation_view?faces-redirect=true";
    }

    public String navigateToViewCcBillRefund(Bill bill) {
        loadBillDetails(bill);
        return "/collecting_centre/view/cc_bill_refund_view?faces-redirect=true";
    }

    public String navigateToViewCcCreditNote(Bill bill) {
        loadBillDetails(bill);
        return "/collecting_centre/view/cc_credit_note_view?faces-redirect=true";
    }

    public String navigateToViewCcDebitNote(Bill bill) {
        loadBillDetails(bill);
        return "/collecting_centre/view/cc_debit_note_view?faces-redirect=true";
    }

    public String navigateToViewCcCreditNoteCancellation(Bill bill) {
        loadBillDetails(bill);
        return "/collecting_centre/view/cc_credit_note_cancellation_view?faces-redirect=true";
    }

    public String navigateToViewCcDebitNoteCancellation(Bill bill) {
        loadBillDetails(bill);
        return "/collecting_centre/view/cc_debit_note_cancellation_view?faces-redirect=true";
    }

    public String navigateToViewCcPaymentCancellationBill(Bill bill) {
        loadBillDetails(bill);
        return "/collecting_centre/view/cc_payment_cancellation_bill_view?faces-redirect=true";
    }

    public String navigateToViewCcPaymentMadeBill(Bill bill) {
        loadBillDetails(bill);
        return "/collecting_centre/view/cc_payment_made_bill_view?faces-redirect=true";
    }

    public String navigateToViewCcPaymentMadeCancellationBill(Bill bill) {
        loadBillDetails(bill);
        return "/collecting_centre/view/cc_payment_made_cancellation_bill_view?faces-redirect=true";
    }

    public String navigateToViewCcPaymentReceivedBill(Bill bill) {
        loadBillDetails(bill);
        return "/collecting_centre/view/cc_payment_received_bill_view?faces-redirect=true";
    }

    public String navigateViewOpdBillByBillTypeAtomic() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is Selected");
            return null;
        }
        viewingBill = bill;
        loadBillDetails(viewingBill);
        BillTypeAtomic billTypeAtomic = bill.getBillTypeAtomic();
        switch (billTypeAtomic) {
            case PHARMACY_RETAIL_SALE_CANCELLED:
                pharmacyBillSearch.setBill(bill);
                return pharmacyBillSearch.navigateToViewPharmacyGrn();
            case OPD_BILL_REFUND:
                return navigateToManageOpdBill();

            case OPD_BILL_CANCELLATION:
                return navigateToViewCancallationOpdBill();

            case OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER:
                return navigateToManageOpdBill();

            case OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
                return navigateToViewCancallationOpdbATCHBill();

            case OPD_PROFESSIONAL_PAYMENT_BILL:
                return navigateToManageOpdBill();

            case OPD_BILL_WITH_PAYMENT:
                return navigateToViewOpdBill();

            case OPD_BATCH_BILL_WITH_PAYMENT:
                return navigateToViewOpdBatchBill();
            case CC_BILL:

            case CHANNEL_BOOKING_WITH_PAYMENT:
                return "";

            case CHANNEL_REFUND:
                return "";
            case CHANNEL_PAYMENT_FOR_BOOKING_BILL:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_FOR_AGENCIES:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_FOR_AGENCIES_RETURN:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_RETURN:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_SESSION:
                return navigateToViewChannelingProfessionalPaymentBill();

            case OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER:
            case OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER:
            case OPD_BATCH_BILL_CANCELLATION:
            case OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER:
            case OPD_PROFESSIONAL_PAYMENT_BILL_RETURN:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES:
            case PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES_RETURN:
                return navigateToViewOpdProfessionalPaymentBill();

        }
        JsfUtil.addErrorMessage("No Handled Bill Type Atomic " + billTypeAtomic);
        return "";
    }

    public String navigateToViewOpdBillNewWindow() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        createBillItemsAndBillFees();
        billBean.checkBillItemFeesInitiated(bill);
        return "/opd/bill_view?faces-redirect=true";
    }

    public String navigateToManageCollectingCentreBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        paymentMethod = bill.getPaymentMethod();
        createBillItemsAndBillFees();
        billBean.checkBillItemFeesInitiated(bill);

        boolean flag = billController.checkBillValues(bill);
        bill.setTransError(flag);
        printPreview = false;
        collectingCentreBillController.getBills().clear();
        collectingCentreBillController.getBills().add(bill);
        return "/collecting_centre/bill_reprint?faces-redirect=true";
    }

    public String navigateToRefundOpdBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }

        if (configOptionApplicationController.getBooleanValueByKey("Set the Original Bill PaymentMethod to Refunded Bill")) {
            paymentMethod = getBill().getPaymentMethod();
        } else {
            paymentMethod = PaymentMethod.Cash;
        }
        paymentMethods = paymentService.fetchAvailablePaymentMethodsForRefundsAndCancellations(bill);
        createBillItemsAndBillFeesForOpdRefund();
        printPreview = false;
        return "/opd/bill_refund?faces-redirect=true";
    }

    public String navigateToRefundCollectingCentreBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to Refund");
            return "";
        }
        paymentMethod = bill.getPaymentMethod();
        createBillItemsAndBillFees();
        boolean flag = billController.checkBillValues(bill);
        bill.setTransError(flag);
        printPreview = false;
        return "/collecting_centre/bill_refund?faces-redirect=true";
    }

    public String navigateToCancelCollectingCentreBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        ccBillCancellingStarted.set(false);
        paymentMethod = bill.getPaymentMethod();
//        createBillItemsAndBillFees();
//        boolean flag = billController.checkBillValues(bill);
//        bill.setTransError(flag);
        printPreview = false;
        return "/collecting_centre/bill_cancel?faces-redirect=true";
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

    private void createBillItemsAndBillFeesForOpdRefund() {
        refundingBill = new RefundBill();
//        BeanUtils.copyProperties(refundingBill, bill);

        // Set unique properties for refundingBill
        refundingBill.setBillClassType(BillClassType.RefundBill);
        refundingBill.setBillType(BillType.OpdBill);
        refundingBill.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_REFUND);
        refundingBill.setBillDate(new Date());
        refundingBill.setBillTime(new Date());
        refundingBill.setCreatedAt(new Date());
        refundingBill.setCreater(sessionController.getLoggedUser());
        refundingBill.setDepartment(sessionController.getLoggedUser().getDepartment());
        refundingBill.setInstitution(sessionController.getLoggedUser().getInstitution());

        refundingBill.setPatient(bill.getPatient());
        refundingBill.setReferenceInstitution(bill.getReferenceInstitution());
        refundingBill.setReferredBy(bill.getReferredBy());
        refundingBill.setReferredByInstitution(bill.getReferredByInstitution());
        refundingBill.setBillClassType(BillClassType.RefundBill);
        refundingBill.setBillDate(new Date());
        refundingBill.setBillTime(new Date());
        refundingBill.setBilledBill(bill);
        refundingBill.setReferenceBill(bill);
        refundingBill.setForwardReferenceBill(bill);
        refundingBill.setId(null);
        refundingBill.setBillItems(new ArrayList<>());
        refundingBill.setBillFees(null);
        refundingBill.setBackwardReferenceBills(null);

        billItems = new ArrayList<>();
        refundingFees = new ArrayList<>();
        refundingItems = new ArrayList<>();

        List<BillItem> billedBillItems = billController.billItemsOfBill(bill);

        for (BillItem originalBillItem : billedBillItems) {
            BillItem newlyCreatedRefundingBillItem = new BillItem();
            newlyCreatedRefundingBillItem.copyWithoutFinancialData(originalBillItem);
            newlyCreatedRefundingBillItem.setBill(refundingBill);
            newlyCreatedRefundingBillItem.setReferanceBillItem(originalBillItem);
            newlyCreatedRefundingBillItem.setReferenceBill(bill);
            newlyCreatedRefundingBillItem.setId(null);

            List<BillFee> originalBillFeesOfBillItem = billController.billFeesOfBillItem(originalBillItem);
            for (BillFee originalBillFeeOfBillItem : originalBillFeesOfBillItem) {

                BillFee newlyCreatedRefundingBillFeeOfBillItem = new BillFee();
                newlyCreatedRefundingBillFeeOfBillItem.copyWithoutFinancialData(originalBillFeeOfBillItem);
                newlyCreatedRefundingBillFeeOfBillItem.setBill(refundingBill);
                newlyCreatedRefundingBillFeeOfBillItem.setBillItem(newlyCreatedRefundingBillItem);
                newlyCreatedRefundingBillFeeOfBillItem.setId(null);
                newlyCreatedRefundingBillFeeOfBillItem.setReferenceBillFee(originalBillFeeOfBillItem);
                newlyCreatedRefundingBillFeeOfBillItem.setReferenceBillItem(originalBillItem);
                newlyCreatedRefundingBillFeeOfBillItem.setFeeValue(0.0);
                newlyCreatedRefundingBillItem.getBillFees().add(newlyCreatedRefundingBillFeeOfBillItem);
//                refundingFees.add(newlyCreatedRefundingBillFeeOfBillItem);

            }
            refundingBill.getBillItems().add(newlyCreatedRefundingBillItem);
//            refundingItems.add(newlyCreatedRefundingBillItem);

        }

    }

    private void createBillItemsAndBillFees() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "SELECT b FROM BillItem b"
                + "  WHERE b.retired=false "
                + " and b.bill=:b";
        hm.put("b", getBillSearch());
        billItems = getBillItemFacede().findByJpql(sql, hm);

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

    private void loadBillItemsAndBillFees(Bill billToLoad) {
        if (billToLoad == null) {
            return;
        }
        if (billToLoad.getBillItems() == null || billToLoad.getBillItems().isEmpty()) {
            billToLoad.setBillItems(billBean.fillBillItems(billToLoad));
        }
        for (BillItem bi : billToLoad.getBillItems()) {
            if (bi.getBillFees() == null || bi.getBillFees().isEmpty()) {
                bi.setBillFees(billBean.fillBillItemFees(bi));
            }
        }
    }

    private void createBillItemsForRetire() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "SELECT b FROM BillItem b WHERE "
                + " b.bill=:b";
        hm.put("b", getBillSearch());
        billItems = getBillItemFacede().findByJpql(sql, hm);

    }

    public List<PharmaceuticalBillItem> getPharmacyBillItems() {
        List<PharmaceuticalBillItem> tmp = new ArrayList<>();
        if (getBill() != null) {
            String sql = "SELECT b FROM PharmaceuticalBillItem b WHERE b.billItem.retired=false and b.billItem.bill.id=" + getBill().getId();
            tmp = getPharmaceuticalBillItemFacade().findByJpql(sql);
        }

        return tmp;
    }

    public List<BillComponent> getBillComponents() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillComponent b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billComponents = getBillCommponentFacade().findByJpql(sql);
            if (billComponents == null) {
                billComponents = new ArrayList<>();
            }
        }
        return billComponents;
    }

    private List<BillFee> billFeesList;

    public List<BillFee> getBillFees() {
        if (getBill() != null) {
            if (billFees == null || billForRefund == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
                billFees = getBillFeeFacade().findByJpql(sql);
            }
        }

        if (billFees == null) {
            billFees = new ArrayList<>();
        }

        return billFees;
    }

    public List<BillFee> getBillFees2() {
        if (billFees == null) {
            if (getBill() != null) {
                Map m = new HashMap();
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill=:cb";
                m.put("cb", getBill());
                billFees = getBillFeeFacade().findByJpql(sql, m);
            }

            if (getBillSearch() != null) {
                Map m = new HashMap();
                String sql = "SELECT b FROM BillFee b WHERE b.bill=:cb";
                m.put("cb", getBill());
                billFees = getBillFeeFacade().findByJpql(sql, m);
            }

            if (billFees == null) {
                billFees = new ArrayList<>();
            }
        }

        return billFees;
    }

    public List<BillFee> getPayingBillFees() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billFees = getBillFeeFacade().findByJpql(sql);
            if (billFees == null) {
                billFees = new ArrayList<>();
            }

        }

        return billFees;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public void setBillComponents(List<BillComponent> billComponents) {
        this.billComponents = billComponents;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillItemFacade getBillItemFacede() {
        return billItemFacede;
    }

    public void setBillItemFacede(BillItemFacade billItemFacede) {
        this.billItemFacede = billItemFacede;
    }

    public BillComponentFacade getBillCommponentFacade() {
        return billCommponentFacade;
    }

    public void setBillCommponentFacade(BillComponentFacade billCommponentFacade) {
        this.billCommponentFacade = billCommponentFacade;
    }

    private void setRefundAttribute() {
        billForRefund.setBalance(getBill().getBalance());

        billForRefund.setBillDate(Calendar.getInstance().getTime());
        billForRefund.setBillTime(Calendar.getInstance().getTime());
        billForRefund.setCreater(getSessionController().getLoggedUser());
        billForRefund.setCreatedAt(Calendar.getInstance().getTime());

        billForRefund.setBillType(getBill().getBillType());
        billForRefund.setBilledBill(getBill());

        billForRefund.setCatId(getBill().getCatId());
        billForRefund.setCollectingCentre(getBill().getCollectingCentre());
        billForRefund.setCreditCardRefNo(getBill().getCreditCardRefNo());
        billForRefund.setCreditCompany(getBill().getCreditCompany());

        billForRefund.setDepartment(getBill().getDepartment());
        billForRefund.setDeptId(getBill().getDeptId());
        billForRefund.setDiscount(getBill().getDiscount());

        billForRefund.setDiscountPercent(getBill().getDiscountPercent());
        billForRefund.setFromDepartment(getBill().getFromDepartment());
        billForRefund.setFromInstitution(getBill().getFromInstitution());
        billForRefund.setFromStaff(getBill().getFromStaff());

        billForRefund.setInsId(getBill().getInsId());
        billForRefund.setInstitution(getBill().getInstitution());

        billForRefund.setPatient(getBill().getPatient());
        billForRefund.setPatientEncounter(getBill().getPatientEncounter());
        billForRefund.setPaymentScheme(getBill().getPaymentScheme());
        billForRefund.setPaymentMethod(getBill().getPaymentMethod());
        billForRefund.setPaymentSchemeInstitution(getBill().getPaymentSchemeInstitution());

        billForRefund.setReferredBy(getBill().getReferredBy());
        billForRefund.setReferringDepartment(getBill().getReferringDepartment());

        billForRefund.setStaff(getBill().getStaff());

        billForRefund.setToDepartment(getBill().getToDepartment());
        billForRefund.setToInstitution(getBill().getToInstitution());
        billForRefund.setToStaff(getBill().getToStaff());
        billForRefund.setTotal(calTot());
        //Need To Add Net Total Logic
        billForRefund.setNetTotal(billForRefund.getTotal());
    }

    public double calTot() {
        if (getBillFees() == null) {
            return 0.0;
        }
        double tot = 0.0;
        for (BillFee f : getBillFees()) {
            tot += f.getFeeValue();
        }
        getBillForRefund().setTotal(tot);
        return tot;
    }

    public RefundBill getBillForRefund() {

        if (billForRefund == null) {
            billForRefund = new RefundBill();
            setRefundAttribute();
        }

        return billForRefund;
    }

    public void setBillForRefund(RefundBill billForRefund) {
        this.billForRefund = billForRefund;
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

    public LazyDataModel<Bill> getLazyBills() {
        return lazyBills;
    }

    public void setLazyBills(LazyDataModel<Bill> lazyBills) {
        this.lazyBills = lazyBills;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public LazyDataModel<BillItem> getSearchBillItems() {
        return searchBillItems;
    }

    public void setSearchBillItems(LazyDataModel<BillItem> searchBillItems) {
        this.searchBillItems = searchBillItems;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
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

    @Deprecated
    public void setBillSearch(Bill bill) {
        recreateModel();
        this.bill = bill;
        paymentMethod = bill.getPaymentMethod();
        createBillItemsForRetire();
        createBillFees();
        createBillItemsAll();
        if (getBill().getBillType() == BillType.CollectingCentreBill) {
            createCollectingCenterfees(getBill());

        }
        if (getBill().getRefundedBill() != null) {
            bills = new ArrayList<>();
            String sql;
            Map m = new HashMap();
            sql = "Select b from Bill b where "
                    + " b.billedBill.id=:bid";
            m.put("bid", getBill().getId());
            bills = getBillFacade().findByJpql(sql, m);
            for (Bill b : bills) {
                createCollectingCenterfees(b);
            }
        }
    }

    @Deprecated
    public void createCollectingCenterfees(Bill b) {
        AgentHistory ah = new AgentHistory();
        if (b.getCancelledBill() != null) {
            b.getCancelledBill().setTransTotalCCFee(0.0);
            b.getCancelledBill().setTransTotalWithOutCCFee(0.0);
            for (BillItem bi : b.getCancelledBill().getBillItems()) {
                bi.setTransCCFee(0.0);
                bi.setTransWithOutCCFee(0.0);
                for (BillFee bf : createBillFees(bi)) {
                    if (bf.getFee().getFeeType() == FeeType.CollectingCentre) {
                        bi.setTransCCFee(bi.getTransCCFee() + bf.getFeeValue());
                    } else {
                        bi.setTransWithOutCCFee(bi.getTransWithOutCCFee() + bf.getFeeValue() + bf.getFeeVat());
//                        bi.setTransWithOutCCFee(bi.getTransWithOutCCFee() + bf.getFeeValue());  add vat to hos fee
                    }
                }
                b.getCancelledBill().setTransTotalCCFee(b.getCancelledBill().getTransTotalCCFee() + bi.getTransCCFee());
                b.getCancelledBill().setTransTotalWithOutCCFee(b.getCancelledBill().getTransTotalWithOutCCFee() + bi.getTransWithOutCCFee());
            }
            ah = fetchCCHistory(b.getCancelledBill());
            if (ah != null) {
                b.getCancelledBill().setTransCurrentCCBalance(ah.getBalanceBeforeTransaction() + ah.getTransactionValue());
            }

        } else if (b.getRefundedBill() != null) {
            b.getRefundedBill().setTransTotalCCFee(0.0);
            b.getRefundedBill().setTransTotalWithOutCCFee(0.0);
            for (BillItem bi : b.getRefundedBill().getBillItems()) {
                bi.setTransCCFee(0.0);
                bi.setTransWithOutCCFee(0.0);
                for (BillFee bf : createBillFees(bi)) {
                    if (bf.getFee().getFeeType() == FeeType.CollectingCentre) {
                        bi.setTransCCFee(bi.getTransCCFee() + bf.getFeeValue());
                    } else {
                        bi.setTransWithOutCCFee(bi.getTransWithOutCCFee() + bf.getFeeValue() + bf.getFeeVat());
//                        bi.setTransWithOutCCFee(bi.getTransWithOutCCFee() + bf.getFeeValue()); add vat for hos fee
                    }
                }
                b.getRefundedBill().setTransTotalCCFee(b.getRefundedBill().getTransTotalCCFee() + bi.getTransCCFee());
                b.getRefundedBill().setTransTotalWithOutCCFee(b.getRefundedBill().getTransTotalWithOutCCFee() + bi.getTransWithOutCCFee());
            }
            ah = fetchCCHistory(b.getRefundedBill());
            if (ah != null) {
                b.getRefundedBill().setTransCurrentCCBalance(ah.getBalanceBeforeTransaction() + ah.getTransactionValue());
            }
        } else {
            b.setTransTotalCCFee(0.0);
            b.setTransTotalWithOutCCFee(0.0);
            for (BillItem bi : b.getBillItems()) {
                bi.setTransCCFee(0.0);
                bi.setTransWithOutCCFee(0.0);
                for (BillFee bf : createBillFees(bi)) {
                    if (bf.getFee().getFeeType() == FeeType.CollectingCentre) {
                        bi.setTransCCFee(bi.getTransCCFee() + bf.getFeeValue());
                    } else {
                        bi.setTransWithOutCCFee(bi.getTransWithOutCCFee() + bf.getFeeValue() + bf.getFeeVat());
//                        bi.setTransWithOutCCFee(bi.getTransWithOutCCFee() + bf.getFeeValue());add vat for hos fee
                    }
                }
                b.setTransTotalCCFee(b.getTransTotalCCFee() + bi.getTransCCFee());
                b.setTransTotalWithOutCCFee(b.getTransTotalWithOutCCFee() + bi.getTransWithOutCCFee());
            }
            ah = fetchCCHistory(b);
            if (ah != null) {
                b.setTransCurrentCCBalance(ah.getBalanceBeforeTransaction() + ah.getTransactionValue());
            }
        }

    }

    public List<BillFee> createBillFees(BillItem bi) {
        List<BillFee> bfs = new ArrayList<>();
        String sql = "SELECT b FROM BillFee b WHERE b.billItem.id=" + bi.getId();
        bfs = getBillFeeFacade().findByJpql(sql);
        return bfs;
    }

    public List<Bill> fetchReferredBills(BillTypeAtomic billTypeAtomic, Bill rb) {
        Map m = new HashMap();
        String j;
        j = "select b "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.referenceBill=:rb "
                + " and b.billTypeAtomic=:bta";
        m.put("ret", false);
        m.put("rb", rb);
        m.put("bta", billTypeAtomic);
        return billFacade.findByJpql(j, m);
    }

    public Bill fetchReferredBill(BillTypeAtomic billTypeAtomic, Bill rb) {
        Map m = new HashMap();
        String j;
        j = "select b "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.referenceBill=:rb "
                + " and b.billTypeAtomic=:bta";
        m.put("ret", false);
        m.put("rb", rb);
        m.put("bta", billTypeAtomic);
        return billFacade.findFirstByJpql(j, m);
    }

    public AgentHistory fetchCCHistory(Bill b) {
        String sql;
        Map m = new HashMap();

        sql = " select ah from AgentHistory ah where ah.retired=false "
                + " and ah.bill.id=" + b.getId();
        AgentHistory ah = agentHistoryFacade.findFirstByJpql(sql);

        return ah;
    }

    public double getRefundTotal() {
        return refundTotal;
    }

    public void setRefundTotal(double refundTotal) {
        this.refundTotal = refundTotal;
    }

    public double getRefundDiscount() {
        return refundDiscount;
    }

    public void setRefundDiscount(double refundDiscount) {
        this.refundDiscount = refundDiscount;
    }

    public double getRefundMargin() {
        return refundMargin;
    }

    public void setRefundMargin(double refundMargin) {
        this.refundMargin = refundMargin;
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

    public List<BillFee> getBillFeesList() {
        return billFeesList;
    }

    public void setBillFeesList(List<BillFee> billFeesList) {
        this.billFeesList = billFeesList;
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

    public List<BillSummery> getBillSummeries() {
        return billSummeries;
    }

    public void setBillSummeries(List<BillSummery> billSummeries) {
        this.billSummeries = billSummeries;
    }

    public Institution getInstitution() {
        return institution;
    }

    public List<PatientInvestigation> fetchPatientInvestigationsAllowBypassSampleProcess(Bill batchBill) {
        if (batchBill == null) {
            return new ArrayList<>();
        }
        viewingPatientInvestigations = new ArrayList<>();
        String jpql = "SELECT pbi "
                + "FROM PatientInvestigation pbi "
                + "WHERE pbi.investigation.bypassSampleWorkflow = :bypass"
                + " and pbi.billItem.bill IN ("
                + " SELECT b FROM Bill b WHERE b.backwardReferenceBill = :bb"
                + ") "
                + "ORDER BY pbi.id";
        Map<String, Object> params = new HashMap<>();
        params.put("bb", batchBill);
        params.put("bypass", true);
        viewingPatientInvestigations = patientInvestigationFacade.findByJpql(jpql, params);
        return viewingPatientInvestigations;
    }

    public void calculateTotalForBillSummaries() {
        cashTotal = 0;
        slipTotal = 0;
        creditCardTotal = 0;
        creditTotal = 0;
        multiplePaymentsTotal = 0;
        patientDepositsTotal = 0;

        if (getBillSummeries() == null) {
            return;
        }
        if (getBillSummeries().isEmpty()) {
            return;
        }
        for (BillSummery bs : getBillSummeries()) {
            if (bs.getPaymentMethod() == PaymentMethod.Cash) {
                cashTotal += bs.getNetTotal();
            } else if (bs.getPaymentMethod() == PaymentMethod.Slip) {
                slipTotal += bs.getNetTotal();
            } else if (bs.getPaymentMethod() == PaymentMethod.Card) {
                creditCardTotal += bs.getNetTotal();
            } else if (bs.getPaymentMethod() == PaymentMethod.Credit) {
                creditTotal += bs.getNetTotal();
            } else if (bs.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
                multiplePaymentsTotal += bs.getNetTotal();
            } else if (bs.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                patientDepositsTotal += bs.getNetTotal();
            }
        }
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

    public String getEncryptedPatientReportId() {
        return encryptedPatientReportId;
    }

    public void setEncryptedPatientReportId(String encryptedPatientReportId) {
        this.encryptedPatientReportId = encryptedPatientReportId;
    }

    public String getEncryptedExpiary() {
        return encryptedExpiary;
    }

    public void setEncryptedExpiary(String encryptedExpiary) {
        this.encryptedExpiary = encryptedExpiary;
    }

    public SecurityController getSecurityController() {
        return securityController;
    }

    public BillSummery getBillSummery() {
        return billSummery;
    }

    public void setBillSummery(BillSummery billSummery) {
        this.billSummery = billSummery;
    }

    public BillLight getBillLight() {
        return billLight;
    }

    public void setBillLight(BillLight billLight) {
        bill = billFacade.find(billLight.getId());
        this.billLight = billLight;
    }

    private long billTypeSummaryId = 0; // Counter for BillTypeSummary IDs
    private long paymentSummaryId = 0; // Counter for PaymentSummary IDs

    public OverallSummary aggregateBillSummaries(List<BillSummery> billSummaries) {
        Map<String, BillTypeSummary> summaryMap = new HashMap<>();
        double billPaymentTotal = 0;
        for (BillSummery bs : billSummaries) {
            String billType = (bs.getBillType() != null) ? bs.getBillType().toString() : "UnknownBillType";
            String billClassType = (bs.getBillClassType() != null) ? bs.getBillClassType().toString() : "UnknownBillClassType";
            String key = billType + ":" + billClassType;

            BillTypeSummary billTypeSummary = summaryMap.get(key);
            if (billTypeSummary == null) {
                billTypeSummary = new BillTypeSummary(bs.getBillType(), bs.getBillClassType(), new ArrayList<>(), billPaymentTotal);
                summaryMap.put(key, billTypeSummary);
            }

            // Aggregate or update the payment summary for the billTypeSummary
            updatePaymentSummary(billTypeSummary, bs);
        }

        return new OverallSummary(new ArrayList<>(summaryMap.values()));
    }

    private void updatePaymentSummary(BillTypeSummary billTypeSummary, BillSummery bs) {
        boolean found = false;

        for (PaymentSummary ps : billTypeSummary.getPaymentSummaries()) {
            if (ps.getPaymentMethod() == paymentMethod.MultiplePaymentMethods && bs.getPaymentMethod() == paymentMethod.MultiplePaymentMethods) {
                return;
            }
            if (ps.getPaymentMethod().equals(bs.getPaymentMethod())) {
                // Aggregate existing payment summary
                ps.setTotal(ps.getTotal() + bs.getTotal());
                ps.setDiscount(ps.getDiscount() + bs.getDiscount());
                ps.setNetTotal(ps.getNetTotal() + bs.getNetTotal());
                ps.setTax(ps.getTax() + bs.getTax());
                ps.setCount(ps.getCount() + bs.getCount());
                break;
            }
        }
        if (!found) {

            // Create a new payment summary and add it
            double billPaymentTotal;
            PaymentSummary newPs = new PaymentSummary(bs.getPaymentMethod(), bs.getTotal(), bs.getDiscount(), bs.getNetTotal(), bs.getTax(), bs.getCount());
            if (newPs.getPaymentMethod() == paymentMethod.MultiplePaymentMethods) {
                return;
            }
            billTypeSummary.getPaymentSummaries().add(newPs);
            billPaymentTotal = bs.getNetTotal();
            double biltypeSum = billTypeSummary.getBillTypeTotal() + billPaymentTotal;
            billTypeSummary.setBillTypeTotal(biltypeSum);
        }
    }

    public OverallSummary getOverallSummary() {
        return overallSummary;
    }

    public void setOverallSummary(OverallSummary overallSummary) {
        this.overallSummary = overallSummary;
    }

    public List<BillFee> getRefundingFees() {
        return refundingFees;
    }

    public void setRefundingFees(List<BillFee> refundingFees) {
        this.refundingFees = refundingFees;
    }

    public Bill getRefundingBill() {
        return refundingBill;
    }

    public void setRefundingBill(Bill refundingBill) {
        this.refundingBill = refundingBill;
    }

    private boolean invertBillValuesAndCalculate(Bill b) {
        if (b == null) {
            return false;
        }
        if (b.getBillItems() == null) {
            return false;
        }
        if (b.getBillItems().isEmpty()) {
            return false;
        }
        double billTotal = 0.0;

        refundAmount = 0;
        refundDiscount = 0;
        refundTotal = 0;
        refundMargin = 0;
        refundVat = 0;
        refundVatPlusTotal = 0;

        for (BillItem bi : b.getBillItems()) {
            double billItemTotal = 0.0;
            for (BillFee bf : bi.getBillFees()) {
                if (bf.getFeeValue() != 0.0) {
                    double bfv = bf.getFeeValue();
                    bfv = Math.abs(bfv);
                    bf.setFeeValue(0 - bfv);
                    billItemTotal += bfv;
                }
            }
            bi.setNetValue(billItemTotal);
            bi.setGrossValue(billItemTotal);
            bi.setMarginValue(0);
            bi.setDiscount(0);
            bi.setVat(0);
            bi.setVatPlusNetValue(billItemTotal);
            refundTotal += bi.getGrossValue();
            refundAmount += bi.getNetValue();
            refundMargin += bi.getMarginValue();
            refundDiscount += bi.getDiscount();
            refundVat += bi.getVat();
            refundVatPlusTotal += bi.getVatPlusNetValue();
        }
        b.setTotal(0 - Math.abs(billTotal));
        b.setNetTotal(0 - Math.abs(billTotal));
        b.setTotal(0 - Math.abs(refundTotal));
        b.setDiscount(0 - Math.abs(refundDiscount));
        b.setNetTotal(0 - Math.abs(refundAmount));
        b.setVat(0 - Math.abs(refundVat));
        b.setVatPlusNetTotal(0 - Math.abs(refundVatPlusTotal));
        return true;

    }

    public Bill getCurrentRefundBill() {
        if (currentRefundBill == null && bill != null) {
            currentRefundBill = bill;
        }
        return currentRefundBill;
    }

    public void setCurrentRefundBill(Bill currentRefundBill) {
        this.currentRefundBill = currentRefundBill;
    }

    public double getCashTotal() {
        return cashTotal;
    }

    public void setCashTotal(double cashTotal) {
        this.cashTotal = cashTotal;
    }

    public double getSlipTotal() {
        return slipTotal;
    }

    public void setSlipTotal(double slipTotal) {
        this.slipTotal = slipTotal;
    }

    public double getCreditTotal() {
        return creditTotal;
    }

    public void setCreditTotal(double creditTotal) {
        this.creditTotal = creditTotal;
    }

    public double getCreditCardTotal() {
        return creditCardTotal;
    }

    public void setCreditCardTotal(double creditCardTotal) {
        this.creditCardTotal = creditCardTotal;
    }

    public double getMultiplePaymentsTotal() {
        return multiplePaymentsTotal;
    }

    public void setMultiplePaymentsTotal(double multiplePaymentsTotal) {
        this.multiplePaymentsTotal = multiplePaymentsTotal;
    }

    public double getPatientDepositsTotal() {
        return patientDepositsTotal;
    }

    public void setPatientDepositsTotal(double patientDepositsTotal) {
        this.patientDepositsTotal = patientDepositsTotal;
    }

    public boolean isOpdBillCancellationSameDay() {
        return opdBillCancellationSameDay;
    }

    public void setOpdBillCancellationSameDay(boolean opdBillCancellationSameDay) {
        this.opdBillCancellationSameDay = opdBillCancellationSameDay;
    }

    public boolean isOpdBillRefundAllowedSameDay() {
        return opdBillRefundAllowedSameDay;
    }

    public void setOpdBillRefundAllowedSameDay(boolean opdBillRefundAllowedSameDay) {
        this.opdBillRefundAllowedSameDay = opdBillRefundAllowedSameDay;
    }

    public boolean isCcBillCancellingStarted() {
        return ccBillCancellingStarted.get();
    }

    public void setCcBillCancellingStarted(boolean ccBillCancellingStarted) {
        this.ccBillCancellingStarted.set(ccBillCancellingStarted);
    }

    public Doctor getReferredBy() {
        return referredBy;
    }

    public void setReferredBy(Doctor referredBy) {
        this.referredBy = referredBy;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public Institution getCollectingCenter() {
        return collectingCenter;
    }

    public void setCollectingCenter(Institution collectingCenter) {
        this.collectingCenter = collectingCenter;
    }

    private void loadBillDetails(Bill bill) {
        viewingBill = billBean.fetchBill(bill.getId());
        viewingIndividualBillsOfBatchBill = billBean.fetchIndividualBillsOfBatchBill(bill);
        viewingRefundBills = billBean.fetchRefundBillsOfBilledBill(bill);
        viewingBillItems = billService.fetchBillItems(bill);
        viewingPharmaceuticalBillItems = billService.fetchPharmaceuticalBillItems(bill);
        viewingPatientInvestigations = billService.fetchPatientInvestigations(bill);
        viewingBillFees = billService.fetchBillFees(bill);
        viewingBillComponents = billBean.fetchBillComponents(bill);
        viewingBillPayments = billBean.fetchBillPayments(bill);
        viewingReferanceBills = billService.fetchAllReferanceBills(bill);
    }

    public List<PatientInvestigation> fetchPatientInvestigations(Bill batchBill) {
        return billService.fetchPatientInvestigationsOfBatchBill(batchBill);
    }

    public Bill getViewingBill() {
        return viewingBill;
    }

    public void setViewingBill(Bill viewingBill) {
        this.viewingBill = viewingBill;
    }

    public List<BillItem> getViewingBillItems() {
        return viewingBillItems;
    }

    public void setViewingBillItems(List<BillItem> viewingBillItems) {
        this.viewingBillItems = viewingBillItems;
    }

    public List<BillFee> getViewingBillFees() {
        return viewingBillFees;
    }

    public void setViewingBillFees(List<BillFee> viewingBillFees) {
        this.viewingBillFees = viewingBillFees;
    }

    public List<BillComponent> getViewingBillComponents() {
        return viewingBillComponents;
    }

    public void setViewingBillComponents(List<BillComponent> viewingBillComponents) {
        this.viewingBillComponents = viewingBillComponents;
    }

    public List<Payment> getViewingBillPayments() {
        return viewingBillPayments;
    }

    public void setViewingBillPayments(List<Payment> viewingBillPayments) {
        this.viewingBillPayments = viewingBillPayments;
    }

    public List<Bill> getViewingIndividualBillsOfBatchBill() {
        return viewingIndividualBillsOfBatchBill;
    }

    public void setViewingIndividualBillsOfBatchBill(List<Bill> viewingIndividualBillsOfBatchBill) {
        this.viewingIndividualBillsOfBatchBill = viewingIndividualBillsOfBatchBill;
    }

    public List<Bill> getViewingRefundBills() {
        return viewingRefundBills;
    }

    public void setViewingRefundBills(List<Bill> viewingRefundBills) {
        this.viewingRefundBills = viewingRefundBills;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public List<Payment> getBillPayments() {
        return billPayments;
    }

    public void setBillPayments(List<Payment> billPayments) {
        this.billPayments = billPayments;
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

    public class PaymentSummary {

        private long idCounter = 0;

        private long id; // Unique identifier
        private PaymentMethod paymentMethod;
        private Double total;
        private Double discount;
        private Double netTotal;
        private Double tax;
        private Long count;

        private Boolean canceld;
        private Boolean refund;

        // Constructors, getters, and setters
        public PaymentSummary(PaymentMethod paymentMethod, Double total, Double discount, Double netTotal, Double tax, Long count) {
            this.id = ++idCounter; // Increment and assign a unique ID
            this.paymentMethod = paymentMethod;
            this.total = total;
            this.discount = discount;
            this.netTotal = netTotal;
            this.tax = tax;
            this.count = count;
        }

        // Unique ID getter
        public long getId() {
            return id;
        }

        // Add methods to aggregate (sum up) totals, discounts, netTotals, and tax values from BillSummery instances
        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public Double getTotal() {
            return total;
        }

        public void setTotal(Double total) {
            this.total = total;
        }

        public Double getDiscount() {
            return discount;
        }

        public void setDiscount(Double discount) {
            this.discount = discount;
        }

        public Double getNetTotal() {
            return netTotal;
        }

        public void setNetTotal(Double netTotal) {
            this.netTotal = netTotal;
        }

        public Double getTax() {
            return tax;
        }

        public void setTax(Double tax) {
            this.tax = tax;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }

        public Boolean getCanceld() {
            return canceld;
        }

        public void setCanceld(Boolean canceld) {
            this.canceld = canceld;
        }

        public Boolean getRefund() {
            return refund;
        }

        public void setRefund(Boolean refund) {
            this.refund = refund;
        }

    }

    public class BillTypeSummary {

        private long idCounter = 0;

        private long id; // Unique identifier
        private BillType billType;
        private BillClassType billClassType;
        private List<PaymentSummary> paymentSummaries;
        private double billTypeTotal = 0;

        // Constructors, getters, and setters
        public BillTypeSummary(BillType billType, BillClassType billClassType, List<PaymentSummary> paymentSummaries, double billTypeTotal) {
            this.id = ++idCounter; // Increment and assign a unique ID
            this.billType = billType;
            this.billClassType = billClassType;
            this.paymentSummaries = paymentSummaries;
            this.billTypeTotal = billTypeTotal;
        }

        // Unique ID getter
        public long getId() {
            return id;
        }

        // Methods to add or update payment summaries based on new BillSummery instances
        public BillType getBillType() {
            return billType;
        }

        public void setBillType(BillType billType) {
            this.billType = billType;
        }

        public BillClassType getBillClassType() {
            return billClassType;
        }

        public void setBillClassType(BillClassType billClassType) {
            this.billClassType = billClassType;
        }

        public List<PaymentSummary> getPaymentSummaries() {
            return paymentSummaries;
        }

        public void setPaymentSummaries(List<PaymentSummary> paymentSummaries) {
            this.paymentSummaries = paymentSummaries;
        }

        public double getBillTypeTotal() {
            return billTypeTotal;
        }

        public void setBillTypeTotal(double billTypeTotal) {
            this.billTypeTotal = billTypeTotal;
        }

        public long getIdCounter() {
            return idCounter;
        }

        public void setIdCounter(long idCounter) {
            this.idCounter = idCounter;
        }

    }

    public class OverallSummary {

        private List<BillTypeSummary> billTypeSummaries;

        // Constructors, getters, and setters
        public OverallSummary(List<BillTypeSummary> billTypeSummaries) {
            this.billTypeSummaries = billTypeSummaries;
        }

        // Methods to add or update bill type summaries based on new BillSummery instances
        public List<BillTypeSummary> getBillTypeSummaries() {
            return billTypeSummaries;
        }

        public void setBillTypeSummaries(List<BillTypeSummary> billTypeSummaries) {
            this.billTypeSummaries = billTypeSummaries;
        }

    }

    public String navigateToDownloadBillsAndBillItems1() {
        return "/analytics/download_bills_and_items?faces-redirect=true";
    }

    public StreamedContent exportAsJson() {
        if (viewingBill == null) {
            JsfUtil.addErrorMessage("No bill is selected");
            return null;
        }

        String json = billService.convertBillToJson(viewingBill);
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        return DefaultStreamedContent.builder()
                .name("bill_" + viewingBill.getDeptId() + ".json")
                .contentType("application/json")
                .stream(() -> stream)
                .build();
    }

    public String findOriginalBillFromCancelledBill(Bill cancelBill) {
        Bill bill = null;
        String jpql = "SELECT b FROM Bill b "
                + " WHERE b.cancelledBill=:bi "
                + " and b.retired = false";
        Map params = new HashMap();
        params.put("bi", cancelBill);
        cancelBill = billFacade.findFirstByJpql(jpql, params);
        return cancelBill.getDeptId();

    }

}
