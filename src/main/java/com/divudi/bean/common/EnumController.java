/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.core.data.*;
import com.divudi.core.data.ScheduledProcess;
import com.divudi.core.data.ScheduledFrequency;
import com.divudi.core.data.analytics.ReportTemplateColumn;
import com.divudi.core.data.analytics.ReportTemplateFilter;
import com.divudi.core.data.hr.*;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.data.inward.AdmissionTypeEnum;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.data.inward.PatientEncounterComponentType;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import com.divudi.core.data.lab.Priority;
import com.divudi.core.data.lab.SearchDateType;
import com.divudi.core.data.lab.TestHistoryType;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.Person;
import com.divudi.service.BillService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@ApplicationScoped
public class EnumController implements Serializable {

    @EJB
    BillService billService;

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    private PaymentScheme paymentScheme;
    private List<Class<? extends Enum<?>>> enumList;
    List<PaymentMethod> paymentMethodsForOpdBilling;
    private List<PaymentMethod> paymentMethodsForGrn;
    private List<PaymentMethod> paymentMethodsForDirectPurchase;
    List<PaymentMethod> paymentMethodsForChanneling;
    List<PaymentMethod> paymentMethodsForChannelSettling;
    List<PaymentMethod> paymentMethodsForPharmacyBilling;
    private List<PaymentMethod> paymentMethodsForMultiplePaymentMethod;
    private List<PaymentMethod> paymentMethodsForPatientDepositRefund;
    private List<PaymentMethod> paymentMethodsForPatientDepositCancel;
    private List<PaymentMethod> paymentMethodsForStaffCreditSettle;
    private List<PaymentMethod> paymentMethodsForPatientDeposit;
    private List<PaymentMethod> paymentMethodsForOpdBillCanceling;
    SessionNumberType[] sessionNumberTypes;
    private List<PatientInvestigationStatus> patientInvestigationStatuses;
    private List<PaymentMethod> paymentTypeOfPaymentMethods;

    private List<PatientInvestigationStatus> availableStatusforCancel;

    private List<BillTypeAtomic> allUtilizedBillTypeAtomics;
    private List<BillTypeAtomic> allUtilizedBillTypeAtomicsForPharmacy;

    @PostConstruct
    public void init() {
        enumList = new ArrayList<>();
        enumList.add(PaymentMethod.class);
        enumList.add(PaperType.class);
        enumList.add(ItemType.class);
        enumList.add(DiscountType.class);
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public List<PaymentMethod> getPaymentMethodsForOpdBilling() {
        if (paymentMethodsForOpdBilling == null) {
            fillPaymentMethodsForOpdBilling();
        }
        return paymentMethodsForOpdBilling;
    }

    public List<PaymentMethod> getPaymentMethodsForPackageBilling() {
        if (paymentMethodsForOpdBilling == null) {
            fillPaymentMethodsForPackageBilling();
        }
        return paymentMethodsForOpdBilling;
    }

    public List<PaymentMethod> getPaymentMethodsForPackageBillingWIthoutMultiple() {
        if (paymentMethodsForOpdBilling == null) {
            fillPaymentMethodsForPackageBilling();
        }
        try {
            paymentMethodsForOpdBilling.remove(PaymentMethod.MultiplePaymentMethods);
        } catch (Exception e) {

        }
        return paymentMethodsForOpdBilling;
    }

    public void resetPaymentMethods() {
        paymentMethodsForOpdBilling = null;
        paymentMethodsForChanneling = null;
    }

    public void fillPaymentMethodsForPatientDeposit() {
        paymentMethodsForPatientDeposit = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            boolean include = configOptionApplicationController.getBooleanValueByKey(pm.getLabel() + " is available for Patient Deposit", true);
            if (include) {
                paymentMethodsForPatientDeposit.add(pm);
            }
        }
    }

    public void fillPaymentMethodsForOpdBilling() {
        paymentMethodsForOpdBilling = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            boolean include = configOptionApplicationController.getBooleanValueByKey(pm.getLabel() + " is available for OPD Billing", true);
            if (include) {
                paymentMethodsForOpdBilling.add(pm);
            }
        }
    }

    public void fillPaymentMethodsForGrn() {
        paymentMethodsForGrn = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            boolean include;
            if (pm == PaymentMethod.Cash || pm == PaymentMethod.Credit) {
                include = configOptionApplicationController.getBooleanValueByKey(pm.getLabel() + " is available for GRN", true);
            } else {
                include = configOptionApplicationController.getBooleanValueByKey(pm.getLabel() + " is available for GRN", false);
            }
            if (include) {
                paymentMethodsForGrn.add(pm);
            }
        }
    }

    public void fillPaymentMethodsForDirectPurchase() {
        paymentMethodsForDirectPurchase = new ArrayList<>();
        // Include other methods based on configuration (default false)
        for (PaymentMethod pm : PaymentMethod.values()) {
            boolean include;
            if (pm == PaymentMethod.Cash || pm == PaymentMethod.Credit) {
                include = configOptionApplicationController.getBooleanValueByKey(pm.getLabel() + " is available for Direct Purchase", true);
            } else {
                include = configOptionApplicationController.getBooleanValueByKey(pm.getLabel() + " is available for Direct Purchase", false);
            }

            if (include) {
                paymentMethodsForDirectPurchase.add(pm);
            }
        }
    }

    public void fillPaymentMethodsForPackageBilling() {
        paymentMethodsForOpdBilling = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            boolean include = configOptionApplicationController.getBooleanValueByKey(pm.getLabel() + " is available for Package Billing", true);
            if (include) {
                paymentMethodsForOpdBilling.add(pm);
            }
        }
    }

    public List<PaymentMethod> getPaymentMethodsForChanneling() {
        if (paymentMethodsForChanneling == null) {
            fillPaymentMethodsForChanneling();
        }
        return paymentMethodsForChanneling;
    }

    public List<InvestigationReportType> getInvestigationReportTypes() {
        return Arrays.asList(InvestigationReportType.values());
    }

    public List<PaymentMethod> getPaymentMethodsForChannelSettling() {
        if (paymentMethodsForChannelSettling == null) {
            fillPaymentMethodsForChannelSettling();
        }
        return paymentMethodsForChannelSettling;
    }

    public void fillPaymentMethodsForChanneling() {
        paymentMethodsForChanneling = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            boolean include = configOptionApplicationController.getBooleanValueByKey(pm.getLabel() + " is available for Channeling", true);
            if (include) {
                paymentMethodsForChanneling.add(pm);
            }
        }
    }

    public void fillPaymentMethodsForChannelSettling() {
        paymentMethodsForChannelSettling = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            if (!pm.equals(PaymentMethod.OnCall)) {
                boolean include = configOptionApplicationController.getBooleanValueByKey(pm.getLabel() + " is available for Channeling", true);
                if (include) {
                    paymentMethodsForChannelSettling.add(pm);
                }
            }
        }
    }

    public List<PaymentMethod> getPaymentMethodsForPharmacyBilling() {
        if (paymentMethodsForPharmacyBilling == null) {
            fillPaymentMethodsForPharmacyBilling();
        }
        return paymentMethodsForPharmacyBilling;
    }

    public List<PaymentMethod> getPaymentMethodsForPharmacyBillCancellations() {
        List<PaymentMethod> paymentMethodsForPharmacyBillCancellations = new ArrayList<>();
        if (paymentMethodsForPharmacyBilling == null) {
            fillPaymentMethodsForPharmacyBilling();
        }
        if (paymentMethodsForPharmacyBilling == null) {
            paymentMethodsForPharmacyBilling = new ArrayList<>();
        }
        if (paymentMethodsForPharmacyBilling.isEmpty()) {
            paymentMethodsForPharmacyBilling.add(PaymentMethod.Cash);
            paymentMethodsForPharmacyBilling.add(PaymentMethod.Card);
        }
        paymentMethodsForPharmacyBillCancellations.addAll(paymentMethodsForPharmacyBilling);
        try {
            paymentMethodsForPharmacyBillCancellations.remove(PaymentMethod.MultiplePaymentMethods);
        } catch (Exception e) {
            System.err.println("e = " + e);
        }
        return paymentMethodsForPharmacyBillCancellations;
    }

    public void fillPaymentMethodsForPharmacyBilling() {
        paymentMethodsForPharmacyBilling = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            boolean include = configOptionApplicationController.getBooleanValueByKey(pm.getLabel() + " is available for Pharmacy Billing", true);
            if (include) {
                paymentMethodsForPharmacyBilling.add(pm);
            }
        }
    }

    public List<PaymentMethod> getPaymentTypeOfPaymentMethods(PaymentType paymentType) {
        paymentTypeOfPaymentMethods = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.asList()) {
            if (pm.getPaymentType() == paymentType) {
                paymentTypeOfPaymentMethods.add(pm);
            }
        }
        return paymentTypeOfPaymentMethods;
    }

    public void setPaymentTypeOfPaymentMethods(List<PaymentMethod> paymentTypeOfPaymentMethods) {
        this.paymentTypeOfPaymentMethods = paymentTypeOfPaymentMethods;
    }

    public List<SearchDateType> getSearchDateTypes() {
        return Arrays.asList(SearchDateType.values());
    }

    public List<String> getEnumValues(String enumClassName) {
        try {
            Class<?> enumClass = Class.forName(enumClassName);
            if (enumClass.isEnum()) {
                Object[] enumConstants = enumClass.getEnumConstants();
                return Arrays.stream(enumConstants)
                        .map(e -> ((Enum<?>) e).name())
                        .collect(Collectors.toList());
            }
        } catch (ClassNotFoundException e) {
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }

    public <E extends Enum<E>> E getEnumValue(Class<E> enumType, String enumName) {
        for (E enumConstant : enumType.getEnumConstants()) {
            if (enumConstant.name().equals(enumName)) {
                return enumConstant;
            }
        }
        return null; // Return null if no match is found
    }

    public List<Priority> getPriorities() {
        return Arrays.asList(Priority.values());
    }

    public List<CategoryType> getCategoryTypes() {
        return Arrays.asList(CategoryType.values());
    }

    public List<HistoryType> getHistoryTypes() {
        return Arrays.asList(HistoryType.values());
    }

    public List<ScheduledProcess> getScheduledProcesses() {
        return Arrays.asList(ScheduledProcess.values());
    }

    public List<EmployeeStatus> getEmploymentStatuses() {
        return Arrays.asList(EmployeeStatus.values());
    }

    public List<ScheduledFrequency> getScheduledFrequencies() {
        return Arrays.asList(ScheduledFrequency.values());
    }

    public Dashboard[] getDashboardTypes() {
        return Dashboard.values();
    }

    public SessionNumberType[] getSessionNumberTypes() {
        sessionNumberTypes = SessionNumberType.values();
        return sessionNumberTypes;
    }

    public List<PatientInvestigationStatus> getPatientInvestigationStatuses() {
        patientInvestigationStatuses = Arrays.asList(PatientInvestigationStatus.values());
        return patientInvestigationStatuses;
    }

    public List<LoginPage> getLoginPages() {
        return Arrays.asList(LoginPage.values());
    }

    public List<ReportViewType> getReportViewTypes() {
        return Arrays.asList(ReportViewType.values());
    }

    public List<ReportViewType> getPharmacyIncomeReportViewTypes() {
        return Arrays.asList(
                ReportViewType.BY_BILL,
                ReportViewType.BY_BILL_TYPE,
                ReportViewType.BY_DISCOUNT_TYPE_AND_ADMISSION_TYPE,
                ReportViewType.BY_BILL_TYPE_AND_DISCOUNT_TYPE_AND_ADMISSION_TYPE
        //                ReportViewType.BY_ITEM
        );
    }

    public List<ReportViewType> getPharmacyProcurementByBillItemViewTypes() {
        return Arrays.asList(
                ReportViewType.BY_BILL,
                ReportViewType.BY_BILL_ITEM
        );
    }

    public List<ReportViewType> getPharmacyIncomeCostReportViewTypes() {
        return Arrays.asList(
                ReportViewType.BY_BILL,
                ReportViewType.BY_BILL_ITEM,
                ReportViewType.BY_BILL_TYPE
        );
    }

    public ItemListingStrategy[] getItemListingStrategys() {
        return ItemListingStrategy.values();
    }

    public SymanticType[] getSymanticTypes() {
        return SymanticType.values();
    }

    public ItemListingStrategy[] getOpdItemListingStrategys() {
        ItemListingStrategy[] sts
                = {ItemListingStrategy.ALL_ITEMS,
                    ItemListingStrategy.SITE_FEE_ITEMS,
                    ItemListingStrategy.ITEMS_OF_LOGGED_DEPARTMENT,
                    ItemListingStrategy.ITEMS_OF_LOGGED_INSTITUTION,
                    ItemListingStrategy.ITEMS_MAPPED_TO_LOGGED_DEPARTMENT,
                    ItemListingStrategy.ITEMS_MAPPED_TO_LOGGED_INSTITUTION};
        return sts;
    }

    public ItemListingStrategy[] getCcItemListingStrategys() {
        ItemListingStrategy[] sts
                = {ItemListingStrategy.ALL_ITEMS,
                    ItemListingStrategy.ITEMS_OF_SELECTED_DEPARTMENT,
                    ItemListingStrategy.ITEMS_OF_SELECTED_INSTITUTIONS,
                    ItemListingStrategy.ITEMS_MAPPED_TO_SELECTED_DEPARTMENT,
                    ItemListingStrategy.ITEMS_MAPPED_TO_SELECTED_INSTITUTION,
                    ItemListingStrategy.ITEMS_OF_LOGGED_DEPARTMENT,
                    ItemListingStrategy.ITEMS_OF_LOGGED_INSTITUTION,
                    ItemListingStrategy.ITEMS_MAPPED_TO_LOGGED_DEPARTMENT,
                    ItemListingStrategy.ITEMS_MAPPED_TO_LOGGED_INSTITUTION};
        return sts;
    }

    public ItemListingStrategy[] getInwardItemListingStrategys() {
        ItemListingStrategy[] sts
                = {ItemListingStrategy.ALL_ITEMS,
                    ItemListingStrategy.ITEMS_OF_LOGGED_DEPARTMENT,
                    ItemListingStrategy.ITEMS_OF_LOGGED_INSTITUTION,
                    ItemListingStrategy.ITEMS_MAPPED_TO_LOGGED_DEPARTMENT,
                    ItemListingStrategy.ITEMS_MAPPED_TO_LOGGED_INSTITUTION};
        return sts;
    }

    public RestAuthenticationType[] getRestAuthenticationTypes() {
        return RestAuthenticationType.values();
    }

    public BillItemStatus[] getBillItemStatuses() {
        return BillItemStatus.values();
    }

    public AdmissionStatus[] getAdmissionStatuses() {
        return AdmissionStatus.values();
    }

    public CssVerticalAlign[] getCssVerticalAlign() {
        return CssVerticalAlign.values();
    }

    public DepartmentListMethod[] getDepartmentListMethods() {
        return DepartmentListMethod.values();
    }

    public DepartmentType[] getDepartmentType() {
        return DepartmentType.values();
    }

    public ApplicationInstitution[] getApplicationInstitutions() {
        return ApplicationInstitution.values();
    }

    public PaperType[] getPaperTypes() {
        return PaperType.values();
    }

    public ReportItemType[] getReportItemTypes() {
        Person p;
        return ReportItemType.values();
    }

    public LeaveType[] getLeaveType() {
        LeaveType[] ltp = {LeaveType.Annual,
            LeaveType.AnnualHalf,
            LeaveType.Casual,
            LeaveType.CasualHalf,
            LeaveType.DutyLeave,
            LeaveType.DutyLeaveHalf,
            LeaveType.Lieu,
            LeaveType.LieuHalf,
            LeaveType.Maternity1st,
            LeaveType.Maternity2nd,
            LeaveType.Medical,
            LeaveType.No_Pay,
            LeaveType.No_Pay_Half};
        return ltp;
    }

    public Times[] getTimeses() {
        return new Times[]{Times.inTime, Times.outTime};
    }

    public void setSessionNumberTypes(SessionNumberType[] sessionNumberTypes) {
        this.sessionNumberTypes = sessionNumberTypes;
    }

    public FeeType[] getFeeTypes() {
        return FeeType.values();
    }

    public DayType[] getDayTypes() {
        return DayType.values();
    }

    public DayType[] getDayTypeForShift() {
        DayType[] dayTypes = {DayType.Normal, DayType.DayOff, DayType.SleepingDay, DayType.Poya};

        return dayTypes;
    }

    public DayType[] getDayTypesForPh() {
        DayType[] dayTypes = {DayType.MurchantileHoliday, DayType.Poya, DayType.PublicHoliday};

        return dayTypes;
    }

    public InvestigationItemType[] getInvestigationItemTypes() {
        return InvestigationItemType.values();
    }

    public InvestigationItemValueType[] getInvestigationItemValueTypes() {
        return InvestigationItemValueType.values();
    }

    public PaysheetComponentType[] getAddingComponentTypes() {
        return PaysheetComponentType.addition.children();

    }

    public BillType[] getBillTypes() {
        return BillType.values();
    }

    public BillTypeAtomic[] getBillTypesAtomic() {
        return BillTypeAtomic.values();
    }

    public List<BillTypeAtomic> getBillTypesAtomicForLabTests() {
        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.CC_BILL);
        btas.add(BillTypeAtomic.CC_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.CC_BILL_REFUND);
        btas.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        btas.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.OPD_BILL_REFUND);
        btas.add(BillTypeAtomic.INWARD_SERVICE_BILL);
        btas.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);
        return btas;
    }

    public List<BillTypeAtomic> getBillTypesAtomic(String query) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(bt -> bt.getLabel().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<String> completeBillTypeAtomics(String query) {
        return Arrays.stream(BillTypeAtomic.values())
                .map(Enum::toString) // Using toString() to get the string representation of the enum
                .filter(name -> name.toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<String> completeReportTemplateColumns(String query) {
        return Arrays.stream(ReportTemplateColumn.values())
                .map(ReportTemplateColumn::getLabel) // Using getLabel() to get the user-friendly string
                .filter(label -> label.toLowerCase().contains(query.toLowerCase())) // Filtering based on query
                .collect(Collectors.toList()); // Collecting results into a List
    }

    public List<String> completeReportTemplateFilters(String query) {
        return Arrays.stream(ReportTemplateFilter.values())
                .map(ReportTemplateFilter::getLabel) // Using getLabel() to get the user-friendly string
                .filter(label -> label.toLowerCase().contains(query.toLowerCase())) // Filtering based on query
                .collect(Collectors.toList()); // Collecting results into a List
    }

    public StaffWelfarePeriod[] getStaffWelfarePeriods() {
        return StaffWelfarePeriod.values();
    }

    public BillClassType[] getBillClassTypes() {
        return BillClassType.values();
    }

    public CalculationType[] getCalculationTypes() {
        return CalculationType.values();
    }

    public PaysheetComponentType[] getDiductionComponentTypes() {
        return PaysheetComponentType.subtraction.children();

    }

    public PaysheetComponentType[] getPaysheetComponentTypes() {
        List<PaysheetComponentType> list = new ArrayList<>();

        for (PaysheetComponentType pct : PaysheetComponentType.addition.children()) {
            list.add(pct);
        }

        for (PaysheetComponentType pct : PaysheetComponentType.subtraction.children()) {
            list.add(pct);
        }

        return list.toArray(new PaysheetComponentType[list.size()]);
    }

    public List<PaysheetComponentType> getPaysheetComponentTypesUserDefinded() {
        return PaysheetComponentType.addition.getUserDefinedComponents();
    }

    public List<PaysheetComponentType> getPaysheetComponentTypesSystemDefinded() {
        return PaysheetComponentType.addition.getSystemDefinedComponents();
    }

    public Title[] getTitle() {
        return Title.values();
    }

    public Title[] getTitleDoctor() {
        Title[] tem = {
            Title.Dr,
            Title.DrMrs,
            Title.DrMs,
            Title.DrMiss,
            Title.Prof,
            Title.ProfMrs,
            Title.Mr,
            Title.Ms,
            Title.Miss,
            Title.Mrs,
            Title.Other,};
        return tem;
    }

    public Sex[] getGender() {
        Sex[] sexes = {Sex.Male, Sex.Female};
        return sexes;
    }

    public PaymentMethod[] getPaymentMethodForAdmission() {
        PaymentMethod[] tmp = {PaymentMethod.Credit, PaymentMethod.Cash};
        return tmp;
    }

    public InwardChargeType[] getInwardChargeTypes() {
        return InwardChargeType.values();
    }

    public InwardChargeType[] getInwardChargeTypesForSetting() {
        return InwardChargeType.values();
    }

    public PatientEncounterComponentType[] getPatientEncounterComponentTypes() {
        return PatientEncounterComponentType.values();
    }

    public BillType[] getCashFlowBillTypes() {
        BillType[] b = {
            BillType.OpdBill,
            BillType.PaymentBill,
            BillType.PettyCash,
            BillType.CashRecieveBill,
            BillType.AgentPaymentReceiveBill,
            BillType.InwardPaymentBill,
            BillType.PharmacySale,
            BillType.ChannelCash,
            BillType.ChannelPaid,
            BillType.GrnPaymentPre,
            BillType.CollectingCentrePaymentReceiveBill,//            BillType.PharmacyPurchaseBill,
        //            BillType.GrnPayment,
        };

        return b;
    }

    public BillType[] getCashFlowBillTypesCashier() {
        BillType[] b = {
            BillType.OpdBill,
            BillType.PaymentBill,
            BillType.PettyCash,
            BillType.CashRecieveBill,
            BillType.AgentPaymentReceiveBill,
            BillType.InwardPaymentBill,
            BillType.PharmacySale,
            BillType.GrnPaymentPre,
            BillType.CollectingCentrePaymentReceiveBill,};

        return b;
    }

    public BillType[] getCashFlowBillTypesChannel() {
        BillType[] b = {
            BillType.ChannelCash,
            BillType.ChannelPaid,
            BillType.ChannelProPayment,
            BillType.ChannelIncomeBill,
            BillType.ChannelExpenesBill,};

        return b;
    }

    public BillType[] getStoreBillTypes() {

        BillType[] b = {
            BillType.StoreGrnBill,
            BillType.StoreGrnReturn,
            BillType.StoreOrder,
            BillType.StoreOrderApprove,
            BillType.StorePre,
            BillType.StorePurchase,
            BillType.StoreSale,
            BillType.StoreAdjustment,
            BillType.StorePurchaseReturn,
            BillType.StoreTransferRequest,
            BillType.StoreTransferIssue,};

        return b;
    }

    public BillType[] getPharmacyBillTypes() {
        BillType[] b = {
            BillType.PharmacyGrnBill,
            BillType.PharmacyGrnReturn,
            BillType.PharmacyReturnWithoutTraising,
            BillType.PharmacyOrder,
            BillType.PharmacyOrderApprove,
            BillType.PharmacyPre,
            BillType.PharmacyPurchaseBill,
            BillType.PharmacySale,
            BillType.PharmacyAdjustment,
            BillType.PurchaseReturn,
            BillType.GrnPaymentPre,
            BillType.PharmacyTransferRequest,
            BillType.PharmacyTransferIssue,
            BillType.PharmacyWholeSale,
            BillType.PharmacyIssue,
            BillType.PharmacyTransferReceive};

        return b;
    }

    public BillType[] getPharmacyBillTypes2() {
        BillType[] b = {
            BillType.PharmacySale,
            BillType.PharmacyAdjustment,
            BillType.PharmacyTransferIssue,
            BillType.PharmacyIssue,
            BillType.PharmacyBhtPre};

        return b;
    }

    public BillType[] getPharmacyBillTypesForMovementReports() {
        BillType[] b = {
            BillType.PharmacySale,
            BillType.PharmacyAdjustment,
            BillType.PharmacyTransferIssue,
            BillType.PharmacyIssue,
            BillType.PharmacyBhtPre};
        return b;
    }

    public BillType[] getPharmacyBillTypes3() {
        BillType[] b = {
            BillType.PharmacyPre,
            BillType.PharmacyWholesalePre,
            BillType.PharmacyAdjustment,
            BillType.PharmacyTransferIssue,
            BillType.PharmacyIssue,
            BillType.PharmacyBhtPre};

        return b;
    }

    public BillType[] getPharmacyBillTypes4() {
        BillType[] b = {
            BillType.PharmacyPre,
            BillType.PharmacyWholesalePre,
            BillType.PharmacyAdjustment,
            BillType.PharmacyTransferIssue,
            BillType.PharmacyIssue,
            BillType.PharmacyBhtPre,
            BillType.PharmacyDisposalIssue};

        return b;
    }

    public BillType[] getPharmacySaleBillTypes() {
        BillType[] bt = {
            BillType.PharmacySale,
            BillType.PharmacyWholeSale,};
        return bt;
    }

    public PaymentMethod[] getPaymentMethods() {
        PaymentMethod[] p = {
            PaymentMethod.Cash,
            PaymentMethod.Card,
            PaymentMethod.Cheque,
            PaymentMethod.Slip,
            PaymentMethod.Credit,
            PaymentMethod.ewallet,
            PaymentMethod.Staff_Welfare,
            PaymentMethod.PatientDeposit,
            PaymentMethod.MultiplePaymentMethods};

        return p;
    }

    public PaymentMethod[] getPaymentMethodsForIncome() {
        PaymentMethod[] p = {
            PaymentMethod.Cash,
            PaymentMethod.Card,
            PaymentMethod.Cheque,
            PaymentMethod.Slip};

        return p;
    }

    public PaymentMethod[] getPaymentMethodsExceptMultiple() {
        PaymentMethod[] p = {
            PaymentMethod.Cash,
            PaymentMethod.Card,
            PaymentMethod.Cheque,
            PaymentMethod.Slip,
            PaymentMethod.Credit,
            PaymentMethod.ewallet,
            PaymentMethod.PatientDeposit};
        return p;
    }

    public InwardChargeType getInaChargeType(String name) {
        for (InwardChargeType chargeType : InwardChargeType.values()) {
            if (chargeType.getLabel().equalsIgnoreCase(name)) {
                return chargeType;
            }
        }
        return null; // or throw an exception if an unknown name is not acceptable
    }

    public PaymentMethod[] getPaymentMethodsNonCreditExceptMultiple() {
        PaymentMethod[] p = {
            PaymentMethod.Cash,
            PaymentMethod.Card,
            PaymentMethod.Cheque,
            PaymentMethod.Slip,
            PaymentMethod.ewallet,
            PaymentMethod.Staff,
            PaymentMethod.PatientDeposit};
        return p;
    }

    public List<PaymentMethod> getPaymentMethodsNonCreditExceptMultiple(List<PaymentMethod> pm) {
        List<PaymentMethod> paymentMethod = new ArrayList<>();
        if (pm == null) {
            paymentMethod.add(PaymentMethod.Cash);
            paymentMethod.add(PaymentMethod.Card);
            paymentMethod.add(PaymentMethod.Cheque);
            paymentMethod.add(PaymentMethod.Slip);
            paymentMethod.add(PaymentMethod.ewallet);
            paymentMethod.add(PaymentMethod.Staff);
            paymentMethod.add(PaymentMethod.PatientDeposit);
        } else {
            paymentMethod.addAll(pm);
            paymentMethod.remove(PaymentMethod.MultiplePaymentMethods);
        }
        return paymentMethod;
    }

    public PaymentMethod[] getPaymentMethodsforAgencyManagement() {
        PaymentMethod[] pm = {
            PaymentMethod.Card,
            PaymentMethod.Cash,
            PaymentMethod.Cheque,
            PaymentMethod.Slip
        };

        return pm;
    }

    public PaymentMethod[] getCollectingCentrePaymentMethods() {
        PaymentMethod[] p = {
            PaymentMethod.Agent,};
        return p;
    }

    public PaymentMethod[] getPaymentMethodsWithoutCredit() {
        PaymentMethod[] p = {PaymentMethod.Cash,
            PaymentMethod.Card,
            PaymentMethod.Cheque,
            PaymentMethod.Slip,
            PaymentMethod.ewallet};
        return p;
    }

    public PaymentMethod[] getPaymentMethodsForSupplierPayments() {
        PaymentMethod[] p = {PaymentMethod.Cash,
            PaymentMethod.Card,
            PaymentMethod.Cheque,
            PaymentMethod.Slip,
            PaymentMethod.IOU};
        return p;
    }

    public PaymentMethod[] getPaymentMethodsForIwardDeposit() {
        PaymentMethod[] p = {PaymentMethod.Cash,
            PaymentMethod.Card,
            PaymentMethod.Cheque,
            PaymentMethod.Slip,
            PaymentMethod.ewallet,
            PaymentMethod.PatientDeposit,
            PaymentMethod.OnlineSettlement};
        return p;
    }

    public PaymentMethod[] getPaymentMethodsForIou() {
        PaymentMethod[] p = {PaymentMethod.Cash,
            PaymentMethod.Cheque,
            PaymentMethod.Slip};
        return p;
    }

    public PaymentMethod[] getPaymentMethodsForIouSettle() {
        PaymentMethod[] p = {PaymentMethod.Cash,
            PaymentMethod.Cheque,
            PaymentMethod.IOU,
            PaymentMethod.Voucher,
            PaymentMethod.Slip};
        return p;
    }

    @Deprecated // Use getPaymentMethodsForPharmacyBilling
    public PaymentMethod[] PaymentMethodsForPharmacyRetailSale() {
        PaymentMethod[] p = {
            PaymentMethod.Cash,
            PaymentMethod.Card,
            PaymentMethod.Credit,
            PaymentMethod.Cheque,
            PaymentMethod.Slip,
            PaymentMethod.MultiplePaymentMethods};
        return p;
    }

    public PaymentMethod[] getPaymentMethodsForPo() {
        PaymentMethod[] p = {PaymentMethod.Cash, PaymentMethod.Credit, PaymentMethod.Cheque, PaymentMethod.Slip};
        return p;
    }

    public List<PaymentMethod> getPaymentMethodsForPurchases() {
        return PaymentMethod.getMethodsByContext(PaymentContext.PURCHASES);
    }

    public CreditDuration[] getCreditDuration() {
        CreditDuration[] c = {CreditDuration.D30, CreditDuration.D60, CreditDuration.D90};

        return c;
    }

    public PaymentMethod[] getPaymentMethodsForChannel() {
        PaymentMethod[] p = {PaymentMethod.OnCall, PaymentMethod.Cash, PaymentMethod.Agent, PaymentMethod.Staff, PaymentMethod.Card, PaymentMethod.Cheque, PaymentMethod.Slip};
        return p;
    }

    public PaymentMethod[] getPaymentMethodsForMakingProfessionalPayments() {
        PaymentMethod[] p = {PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip};
        return p;
    }

    public PaymentMethod[] getPaymentMethodsForChannelSettle() {
        PaymentMethod[] p = {PaymentMethod.Cash, PaymentMethod.Card};
        return p;
    }

    public PaymentMethod[] getPaymentMethodsForChannelAgentSettle() {
        PaymentMethod[] p = {PaymentMethod.Cash, PaymentMethod.Agent};
        return p;
    }

    public List<ItemBarcodeGenerationStrategy> getItemBarcodeGenerationStrategies() {
        return Arrays.asList(ItemBarcodeGenerationStrategy.values());
    }

    public PaymentMethod[] getAllPaymentMethods() {
        return PaymentMethod.values();
    }

    public List<BankAccountType> getBankAccountTypes() {
        return BankAccountType.getAllValues();
    }

    public List<PaymentMethod> getActivePaymentMethods() {
        return PaymentMethod.getActivePaymentMethods();
    }

    public BillType[] getChannelType() {
        BillType[] bt = {BillType.Channel, BillType.XrayScan};
        return bt;
    }

//    public boolean checkPaymentScheme(PaymentScheme scheme, String paymentMathod) {
//        if (scheme != null && scheme.getPaymentMethod() != null) {
//            //System.err.println("Payment Scheme : " + scheme.getPaymentMethod());
//            //System.err.println("Payment Method : " + PaymentMethod.valueOf(paymentMathod));
//            if (scheme.getPaymentMethod().equals(PaymentMethod.valueOf(paymentMathod))) {
//                //System.err.println("Returning True");
//                return true;
//            } else {
//                return false;
//            }
//        }
//
//        return false;
//
//    }
//    public boolean checkPaymentScheme(String paymentMathod) {
//        if (getPaymentScheme() != null && getPaymentScheme().getPaymentMethod() != null) {
//            //System.err.println("Payment Scheme : " +getPaymentScheme().getPaymentMethod());
//            //System.err.println("Payment Method : " + PaymentMethod.valueOf(paymentMathod));
//            if (getPaymentScheme().getPaymentMethod().equals(PaymentMethod.valueOf(paymentMathod))) {
//                //System.err.println("Returning True");
//                return true;
//            } else {
//                return false;
//            }
//        }
//
//        return false;
//
//    }
    public boolean checkPaymentMethod(PaymentMethod paymentMethod, String paymentMathodStr) {
        if (paymentMethod != null) {
            //System.err.println("Payment method : " + paymentMethod);
            //System.err.println("Payment Method String : " + PaymentMethod.valueOf(paymentMathodStr));
            if (paymentMethod.equals(PaymentMethod.valueOf(paymentMathodStr))) {
                //System.err.println("Returning True");
                return true;
            } else {
                return false;
            }
        }

        return false;

    }

    public AdmissionTypeEnum[] getAdmissionTypeEnum() {
        return AdmissionTypeEnum.values();
    }

    public MessageType[] getSmsType() {
        return MessageType.values();
    }

    /**
     * Creates a new instance of EnumController
     */
    public EnumController() {
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public List<PaymentMethod> getPaymentMethodsForPatientDeposit() {
        paymentMethodsForPatientDeposit = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            boolean include = configOptionApplicationController.getBooleanValueByKey(pm.getLabel() + " is available for Patient Deposit", true);
            if (include) {
                paymentMethodsForPatientDeposit.add(pm);
            }
        }
        return paymentMethodsForPatientDeposit;
    }

    public void setPaymentMethodsForPatientDeposit(List<PaymentMethod> paymentMethodsForPatientDeposit) {
        this.paymentMethodsForPatientDeposit = paymentMethodsForPatientDeposit;
    }

    public List<PaymentMethod> getPaymentMethodsForStaffCreditSettle() {
        paymentMethodsForStaffCreditSettle = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            boolean include = configOptionApplicationController.getBooleanValueByKey(pm.getLabel() + " is available for Staff Credit Settle", true);
            if (include) {
                paymentMethodsForStaffCreditSettle.add(pm);
            }
        }
        return paymentMethodsForStaffCreditSettle;
    }

    public void setPaymentMethodsForStaffCreditSettle(List<PaymentMethod> paymentMethodsForStaffCreditSettle) {
        this.paymentMethodsForStaffCreditSettle = paymentMethodsForStaffCreditSettle;
    }

    public List<PaymentMethod> getPaymentMethodsForPatientDepositRefund() {
        paymentMethodsForPatientDepositRefund = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            boolean include = configOptionApplicationController.getBooleanValueByKey(pm.getLabel() + " is available for Patient Deposit Return", true);
            if (include) {
                paymentMethodsForPatientDepositRefund.add(pm);
            }
        }
        return paymentMethodsForPatientDepositRefund;
    }

    public void setPaymentMethodsForPatientDepositRefund(List<PaymentMethod> paymentMethodsForPatientDepositRefund) {
        this.paymentMethodsForPatientDepositRefund = paymentMethodsForPatientDepositRefund;
    }

    public List<PaymentMethod> getPaymentMethodsForOpdBillCanceling() {
        paymentMethodsForOpdBillCanceling = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            boolean include = configOptionApplicationController.getBooleanValueByKey(pm.getLabel() + " is available for OPD Bill Cancel", true);
            if (include) {
                if (pm != PaymentMethod.MultiplePaymentMethods) {
                    paymentMethodsForOpdBillCanceling.add(pm);
                }
            }
        }
        return paymentMethodsForOpdBillCanceling;
    }

    public void setPaymentMethodsForOpdBillCanceling(List<PaymentMethod> paymentMethodsForOpdBillCanceling) {
        this.paymentMethodsForOpdBillCanceling = paymentMethodsForOpdBillCanceling;
    }

    public List<PaymentMethod> getPaymentMethodsForPatientDepositCancel() {
        paymentMethodsForPatientDepositCancel = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            boolean include = configOptionApplicationController.getBooleanValueByKey(pm.getLabel() + " is available for Patient Deposit Cancel", true);
            if (include) {
                paymentMethodsForPatientDepositCancel.add(pm);
            }
        }
        return paymentMethodsForPatientDepositCancel;
    }

    public void setPaymentMethodsForPatientDepositCancel(List<PaymentMethod> paymentMethodsForPatientDepositCancel) {
        this.paymentMethodsForPatientDepositCancel = paymentMethodsForPatientDepositCancel;
    }

    public List<PaymentMethod> getPaymentMethodsForMultiplePaymentMethod() {
        List<PaymentMethod> p = new ArrayList<>();
        p.add(PaymentMethod.Cash);
        p.add(PaymentMethod.Card);
        p.add(PaymentMethod.Cheque);
        p.add(PaymentMethod.Slip);
        p.add(PaymentMethod.OnlineSettlement);
        p.add(PaymentMethod.Staff_Welfare);
        p.add(PaymentMethod.PatientDeposit);
        paymentMethodsForMultiplePaymentMethod = p;
        return paymentMethodsForMultiplePaymentMethod;
    }

    public void setPaymentMethodsForMultiplePaymentMethod(List<PaymentMethod> paymentMethodsForMultiplePaymentMethod) {
        this.paymentMethodsForMultiplePaymentMethod = paymentMethodsForMultiplePaymentMethod;
    }

    public List<PatientInvestigationStatus> getAvailableStatusforCancel() {
        availableStatusforCancel = new ArrayList<>();
        availableStatusforCancel.add(PatientInvestigationStatus.ORDERED);
        availableStatusforCancel.add(PatientInvestigationStatus.SAMPLE_GENERATED);
        availableStatusforCancel.add(PatientInvestigationStatus.SAMPLE_COLLECTED);
        availableStatusforCancel.add(PatientInvestigationStatus.SAMPLE_SENT);
        availableStatusforCancel.add(PatientInvestigationStatus.SAMPLE_REJECTED);
        return availableStatusforCancel;
    }

    public void setAvailableStatusforCancel(List<PatientInvestigationStatus> availableStatusforCancel) {
        this.availableStatusforCancel = availableStatusforCancel;
    }

    public List<PaymentMethod> getPaymentMethodsForGrn() {
        if (paymentMethodsForGrn == null) {
            fillPaymentMethodsForGrn();
        }
        return paymentMethodsForGrn;
    }

    public InvestigationItemType getInvestigationItemType(String name) {
        for (InvestigationItemType type : InvestigationItemType.values()) {
            if (type.toString().equalsIgnoreCase(name.trim())) {
                return type;
            }
        }
        return null;
    }

    public InvestigationItemValueType getInvestigationItemValueType(String name) {
        for (InvestigationItemValueType type : InvestigationItemValueType.values()) {
            if (type.toString().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

    public CssVerticalAlign getCssVerticalAlign(String name) {
        for (CssVerticalAlign verAlign : CssVerticalAlign.values()) {
            if (verAlign.toString().equalsIgnoreCase(name)) {
                return verAlign;
            }
        }
        return null;
    }

    public CssTextDecoration getCssTextDecoration(String name) {
        for (CssTextDecoration txDeco : CssTextDecoration.values()) {
            if (txDeco.toString().equalsIgnoreCase(name)) {
                return txDeco;
            }
        }
        return null;
    }

    public CssFontStyle getCssFontStyle(String name) {
        for (CssFontStyle fontstyle : CssFontStyle.values()) {
            if (fontstyle.toString().equalsIgnoreCase(name)) {
                return fontstyle;
            }
        }
        return null;
    }

    public CssTextAlign getCssTextAlign(String name) {
        for (CssTextAlign txetAlign : CssTextAlign.values()) {
            if (txetAlign.toString().equalsIgnoreCase(name)) {
                return txetAlign;
            }
        }
        return null;
    }

    public synchronized List<BillTypeAtomic> getAllUtilizedBillTypeAtomics() {
        if (allUtilizedBillTypeAtomics == null) {
            try {
                allUtilizedBillTypeAtomics = billService.fetchAllUtilizedBillTypeAtomics();
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.SEVERE, "Error fetching BillTypeAtomics", e);
                allUtilizedBillTypeAtomics = new ArrayList<>();
            }
        }
        return allUtilizedBillTypeAtomics;
    }

    public void setAllUtilizedBillTypeAtomics(List<BillTypeAtomic> allUtilizedBillTypeAtomics) {
        this.allUtilizedBillTypeAtomics = allUtilizedBillTypeAtomics;
    }

    public synchronized List<BillTypeAtomic> getAllUtilizedBillTypeAtomicsForPharmacy() {
        if (allUtilizedBillTypeAtomicsForPharmacy == null) {
            allUtilizedBillTypeAtomicsForPharmacy = filterBillTypeAtomics(getAllUtilizedBillTypeAtomics(), ServiceType.PHARMACY);
        }
        return allUtilizedBillTypeAtomicsForPharmacy;
    }

    public List<BillTypeAtomic> filterBillTypeAtomics(List<BillTypeAtomic> btas, ServiceType serviceType) {
        List<BillTypeAtomic> filteredList = new ArrayList<>();

        if (btas == null || serviceType == null) {
            return filteredList;
        }

        for (BillTypeAtomic bta : btas) {
            if (bta != null && serviceType.equals(bta.getServiceType())) {
                filteredList.add(bta);
            }
        }

        return filteredList;
    }

    public void setAllUtilizedBillTypeAtomicsForPharmacy(List<BillTypeAtomic> allUtilizedBillTypeAtomicsForPharmacy) {
        this.allUtilizedBillTypeAtomicsForPharmacy = allUtilizedBillTypeAtomicsForPharmacy;
    }

    public TestHistoryType[] getLabTestHistoryList() {
        return TestHistoryType.values();
    }

    public TestHistoryType getLabTestHistory(String name) {
        for (TestHistoryType type : TestHistoryType.values()) {
            if (type.toString().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

    public List<PaymentMethod> getPaymentMethodsForDirectPurchase() {
        return paymentMethodsForDirectPurchase;
    }
    
    

}
