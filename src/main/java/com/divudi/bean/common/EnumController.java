/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.data.ApplicationInstitution;
import com.divudi.data.BillClassType;
import com.divudi.data.BillItemStatus;
import com.divudi.data.BillType;
import com.divudi.data.CalculationType;
import com.divudi.data.CreditDuration;
import com.divudi.data.CssVerticalAlign;
import com.divudi.data.Dashboard;
import com.divudi.data.DepartmentListMethod;
import com.divudi.data.DepartmentType;
import com.divudi.data.DiscountType;
import com.divudi.data.FeeType;
import com.divudi.data.InvestigationItemType;
import com.divudi.data.InvestigationItemValueType;
import com.divudi.data.ItemListingStrategy;
import com.divudi.data.ItemType;
import com.divudi.data.LoginPage;
import com.divudi.data.PaperType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.ReportItemType;
import com.divudi.data.SessionNumberType;
import com.divudi.data.Sex;
import com.divudi.data.MessageType;
import com.divudi.data.PaymentContext;
import com.divudi.data.RestAuthenticationType;
import com.divudi.data.SymanticType;
import com.divudi.data.Title;
import com.divudi.data.hr.DayType;
import com.divudi.data.hr.LeaveType;
import com.divudi.data.hr.PaysheetComponentType;
import com.divudi.data.hr.Times;
import com.divudi.data.inward.AdmissionStatus;
import com.divudi.data.inward.AdmissionTypeEnum;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.data.inward.PatientEncounterComponentType;
import com.divudi.data.lab.Priority;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Person;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class EnumController implements Serializable {

    private PaymentScheme paymentScheme;
    private List<Class<? extends Enum<?>>> enumList;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    List<PaymentMethod> paymentMethodsForOpdBilling;
    List<PaymentMethod> paymentMethodsForChanneling;
    List<PaymentMethod> paymentMethodsForPharmacyBilling;
    SessionNumberType[] sessionNumberTypes;

    @PostConstruct
    public void init() {
        enumList = new ArrayList<>();
        enumList.add(PaymentMethod.class);
        enumList.add(PaperType.class);
        enumList.add(ItemType.class);
        enumList.add(DiscountType.class);
    }

    public List<PaymentMethod> getPaymentMethodsForOpdBilling() {
        if (paymentMethodsForOpdBilling == null) {
            fillPaymentMethodsForOpdBilling();
        }
        return paymentMethodsForOpdBilling;
    }

    public void resetPaymentMethods() {
        paymentMethodsForOpdBilling = null;
        paymentMethodsForChanneling = null;
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
    
     public List<PaymentMethod> getPaymentMethodsForChanneling() {
        if (paymentMethodsForChanneling == null) {
            fillPaymentMethodsForChanneling();
        }
        return paymentMethodsForChanneling;
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
    
    public List<PaymentMethod> getPaymentMethodsForPharmacyBilling() {
        if (paymentMethodsForPharmacyBilling == null) {
            fillPaymentMethodsForPharmacyBilling();
        }
        return paymentMethodsForPharmacyBilling;
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

    public Priority[] getPriorities() {
        return Priority.values();
    }

    public Dashboard[] getDashboardTypes() {
        return Dashboard.values();
    }

    public SessionNumberType[] getSessionNumberTypes() {
        sessionNumberTypes = SessionNumberType.values();
        return sessionNumberTypes;
    }

    public List<LoginPage> getLoginPages() {
        return Arrays.asList(LoginPage.values());
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

    public Sex[] getSex() {
        return Sex.values();
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
        InwardChargeType[] b = {
            InwardChargeType.AdmissionFee,
            InwardChargeType.Medicine,
            InwardChargeType.BloodTransfusioncharges,
            InwardChargeType.Immunization,
            InwardChargeType.Equipment,
            InwardChargeType.MealCharges,
            InwardChargeType.OperationTheatreCharges,
            InwardChargeType.OperationTheatreNursingCharges,
            InwardChargeType.OperationTheatreMachineryCharges,
            InwardChargeType.LarbourRoomCharges,
            InwardChargeType.ETUCharges,
            InwardChargeType.TreatmentCharges,
            InwardChargeType.IntensiveCareManagement,
            InwardChargeType.AmbulanceCharges,
            InwardChargeType.HomeVisiting,
            InwardChargeType.GeneralIssuing,
            InwardChargeType.WardProcedures,
            InwardChargeType.ReimbursementCharges,
            InwardChargeType.DressingCharges,
            InwardChargeType.OxygenCharges,
            InwardChargeType.physiotherapy,
            InwardChargeType.Laboratory,
            InwardChargeType.X_Ray,
            InwardChargeType.CT,
            InwardChargeType.Scanning,
            InwardChargeType.ECG_EEG,
            InwardChargeType.MedicalServices,
            InwardChargeType.AdministrationCharge,
            InwardChargeType.LinenCharges,
            InwardChargeType.MaintainCharges,
            InwardChargeType.MedicalCareICU,
            InwardChargeType.MOCharges,
            InwardChargeType.NursingCharges,
            InwardChargeType.RoomCharges,
            InwardChargeType.CardiacMonitoring,
            InwardChargeType.Nebulisation,
            InwardChargeType.Echo,
            InwardChargeType.SyringePump,
            InwardChargeType.TheaterConsumbale,
            InwardChargeType.ExerciseECG,
            InwardChargeType.TheaterConsumbale,
            InwardChargeType.VAT,
            InwardChargeType.EyeLence,
            InwardChargeType.AccessoryCharges,
            InwardChargeType.HospitalSupportService,
            InwardChargeType.ExtraMedicine,
            InwardChargeType.DialysisTreatment,
            InwardChargeType.OtherCharges,
            InwardChargeType.Eye,
            InwardChargeType.Dental};

        return b;
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
            PaymentMethod.PatientDeposit,
            PaymentMethod.MultiplePaymentMethods};

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
            PaymentMethod.PatientDeposit};
        return p;
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
            PaymentMethod.MultiplePaymentMethods};
        return p;
    }
    
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
        PaymentMethod[] p = {PaymentMethod.Cash, PaymentMethod.Credit};

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

    public PaymentMethod[] getAllPaymentMethods() {
        return PaymentMethod.values();
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

}
