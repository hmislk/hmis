/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.common.WebUserController;
import com.divudi.bean.membership.MembershipSchemeController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.ChargeItemTotal;
import com.divudi.core.data.dataStructure.DepartmentBillItems;
import com.divudi.core.data.dataStructure.InwardBillItem;
import com.divudi.core.data.inward.AdmissionTypeEnum;
import com.divudi.core.data.inward.InwardChargeType;
import static com.divudi.core.data.inward.InwardChargeType.RoomCharges;
import com.divudi.ejb.BillNumberGenerator;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PatientItem;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.GuardianRoom;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.entity.inward.TimedItem;
import com.divudi.core.entity.inward.TimedItemFee;
import com.divudi.core.entity.membership.InwardMemberShipDiscount;
import com.divudi.core.facade.AdmissionTypeFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientItemFacade;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.facade.ServiceFacade;
import com.divudi.core.facade.TimedItemFeeFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.EncounterCreditCompany;
import com.divudi.core.entity.Staff;
import com.divudi.core.facade.EncounterCreditCompanyFacade;
import com.divudi.core.util.CommonFunctions;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.RowEditEvent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class BhtSummeryController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private ServiceFacade serviceFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PatientItemFacade patientItemFacade;
    @EJB
    private TimedItemFeeFacade timedItemFeeFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    AdmissionTypeFacade admissionTypeFacade;
    @EJB
    EncounterCreditCompanyFacade encounterCreditCompanyFacade;
    ////////////////////////////

    @EJB
    private BillNumberGenerator billNumberBean;
    @Inject
    WebUserController webUserController;
    @Inject
    PriceMatrixController priceMatrixController;
    //////////////////////////
    @Inject
    private SessionController sessionController;
    @Inject
    private InwardTimedItemController inwardTimedItemController;
    @Inject
    private DischargeController dischargeController;
    @Inject
    BillController billController;
    @Inject
    RoomChangeController roomChangeController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    AdmissionController admissionController;
    ////////////////////////
    private List<DepartmentBillItems> departmentBillItems;
    private List<BillFee> profesionallFee;
    private List<BillFee> doctorAndNurseFee;
    List<BillItem> pharmacyItems;
    private List<Bill> paymentBill;
    private List<Bill> pharmacyIssues;
    List<Bill> storeIssues;
    private List<Bill> surgeryBills;
    private Bill surgeryBill;
    List<PatientItem> patientItems;
    private List<ChargeItemTotal> chargeItemTotals;
    List<PatientRoom> patientRooms;
    //////////////////////////
    private double grantTotal = 0.0;
    private double discount;
    private double due;
    private double paid;
    private double paidByPatient;
    private double paidByCompany;
    private PatientItem tmpPI;
    private PatientEncounter patientEncounter;
    private Bill current;
    private Bill originalBill;
    private Bill tempBill;
    private Date currentTime;
    private Date toTime;
    Date fromDate;
    Date toDate;
    private Date date;
    private boolean printPreview;
    @Inject
    private InwardMemberShipDiscount inwardMemberShipDiscount;
    @Inject
    BillBhtController billBhtController;
    private Item item;
    boolean changed = false;
    boolean showOrginalBill = false;
    private String duration;
    private boolean patientEncounterHasProvisionalBill = false;
    private List<PatientEncounter> childPatientEncouters;

    public String navigateToIntrimBillEstimate() {
        createTablesWithEstimatedProfessionalFees();
        return "/inward/inward_bill_intrim_estimate?faces-redirect=true";
    }

    public String navigateToInpatientProfile() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No Admission Selected");
            return "";
        }
        fillSurgeryBills();
        return "/inward/admission_profile.xhtml?faces-redirect=true";
    }

    private void fillSurgeryBills() {
        surgeryBills = billController.fillPatientSurgeryBills(patientEncounter);
        if (surgeryBills == null || surgeryBills.isEmpty()) {
            surgeryBill = null;
        } else {
            surgeryBill = surgeryBills.get(0);
        }
    }

    public String navigateToAddServiceFromSurgeriesFromAdmissionProfile() {
        if (surgeryBills == null) {
            JsfUtil.addErrorMessage("No Surgeries added yet");
            return null;
        }
        if (surgeryBills.isEmpty()) {
            JsfUtil.addErrorMessage("No Surgeries added yet");
            return null;
        }
        if (surgeryBill == null) {
            surgeryBill = surgeryBills.get(0);
        }
        billBhtController.resetBillData();
        billBhtController.setBills(surgeryBills);
        billBhtController.setBatchBill(surgeryBill);
        return "/theater/inward_bill_surgery_service";
    }

    public List<PatientRoom> getPatientRooms() {
        if (patientRooms == null) {
            patientRooms = createPatientRooms();
        }
        return patientRooms;
    }

    public void setPatientRooms(List<PatientRoom> patientRooms) {
        this.patientRooms = patientRooms;
    }

    public List<BillFee> getDoctorAndNurseFee() {
        if (doctorAndNurseFee == null) {
            List<PatientEncounter> cpts = getInwardBean().fetchChildPatientEncounter(getPatientEncounter());
            doctorAndNurseFee = getInwardBean().createDoctorAndNurseFee(getPatientEncounter(), cpts);
        }
        return doctorAndNurseFee;
    }

    public void setDoctorAndNurseFee(List<BillFee> doctorAndNurseFee) {
        this.doctorAndNurseFee = doctorAndNurseFee;
    }

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public void changeDiscountListener(ChargeItemTotal cit) {
        double discountPercent = (cit.getDiscount() * 100) / cit.getTotal();
        double disValue = 0;
        switch (cit.getInwardChargeType()) {
            case MaintainCharges:
                break;
            case MOCharges:
                break;
            case NursingCharges:
                break;
            case RoomCharges:
                break;
            case MedicalCareICU:
                break;
            case AdministrationCharge:
                break;
            case LinenCharges:
                break;
            case Medicine:
                disValue = updateIssueBillFees(cit.getInwardChargeType(), discountPercent, BillType.PharmacyBhtPre);
                break;
            case GeneralIssuing:
                disValue = updateIssueBillFees(cit.getInwardChargeType(), discountPercent, BillType.StoreBhtPre);
                break;
            default:
                disValue = discountSet(cit, discountPercent);
        }

        cit.setDiscount(disValue);
//        cit.setAdjustedTotal(cit.getTotal());

        updateTotal();
    }

    public void changeDiscountListenerPatientRoomRoomCharge(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double disCountPercent = 0;

        //Room Charge
        if (pR.getDiscountRoomCharge() != 0 && pR.getCalculatedRoomCharge() != 0) {
            disCountPercent = (pR.getDiscountRoomCharge() * 100) / pR.getCalculatedRoomCharge();
            updatePatientRoomCharge(pR, disCountPercent);
        }

        updateRoomChargeTypeTotal();

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
    }

    public void changeDiscountListenerPatientRoomMaintain(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double disCountPercent = 0;

        //Maintain
        if (pR.getDiscountMaintainCharge() != 0 && pR.getCalculatedMaintainCharge() != 0) {
            disCountPercent = (pR.getDiscountMaintainCharge() * 100) / pR.getCalculatedMaintainCharge();
            updatePatientMaintainCharge(pR, disCountPercent);
        }

        updateRoomChargeTypeTotal();

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
    }

    public void changeDiscountListenerPatientRoomLinen(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double disCountPercent = 0;

        //Linen
        if (pR.getDiscountLinenCharge() != 0 && pR.getCalculatedLinenCharge() != 0) {
            disCountPercent = (pR.getDiscountLinenCharge() * 100) / pR.getCalculatedLinenCharge();
            updatePatientLinenCharge(pR, disCountPercent);
        }

        updateRoomChargeTypeTotal();

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
    }

    public void changeDiscountListenerPatientRoomMedicalCare(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double disCountPercent = 0;

        //Medical Care Charge
        if (pR.getDiscountMedicalCareCharge() != 0 && pR.getCalculatedMedicalCareCharge() != 0) {
            disCountPercent = (pR.getDiscountMedicalCareCharge() * 100) / pR.getCalculatedMedicalCareCharge();
            updatePatientMedicalCareIcuCharge(pR, disCountPercent);
        }

        updateRoomChargeTypeTotal();

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
    }

    public void changeDiscountListenerPatientRoomAdministration(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double disCountPercent = 0;
        //Administration Charge
        if (pR.getDiscountAdministrationCharge() != 0 && pR.getCalculatedAdministrationCharge() != 0) {
            disCountPercent = (pR.getDiscountAdministrationCharge() * 100) / pR.getCalculatedAdministrationCharge();
            updatePatientAdministrationCharge(pR, disCountPercent);
        }

        updateRoomChargeTypeTotal();

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
    }

    public void changeDiscountListenerPatientRoomNursing(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double disCountPercent = 0;

        //Nursing
        if (pR.getDiscountNursingCharge() != 0 && pR.getCalculatedNursingCharge() != 0) {
            disCountPercent = (pR.getDiscountNursingCharge() * 100) / pR.getCalculatedNursingCharge();
            updatePatientNursingCharge(pR, disCountPercent);
        }

        updateRoomChargeTypeTotal();

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
    }

    public void changeDiscountListenerPatientRoomMo(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double disCountPercent = 0;

        //MO
        if (pR.getDiscountMoCharge() != 0 && pR.getCalculatedMoCharge() != 0) {
            disCountPercent = (pR.getDiscountMoCharge() * 100) / pR.getCalculatedMoCharge();
            updatePatientMoCharge(pR, disCountPercent);
        }

        updateRoomChargeTypeTotal();

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
    }

    private void updateRoomChargeTypeTotal() {
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {
                case AdministrationCharge:
                    chargeItemTotal.setDiscount(getInwardBean().calPatientRoomAdminChargeDiscount(getPatientEncounter()));
                    //    chargeItemTotal.setAdjustedTotal(chargeItemTotal.getTotal());
                    break;
                case MedicalCareICU:
                    chargeItemTotal.setDiscount(getInwardBean().calPatientRoomMadicalCareChargeDiscount(getPatientEncounter()));
                    //chargeItemTotal.setAdjustedTotal(chargeItemTotal.getTotal());
                    break;
                case LinenCharges:
                    chargeItemTotal.setDiscount(getInwardBean().calPatientRoomLinenChargeDiscount(getPatientEncounter()));
                    ///   chargeItemTotal.setAdjustedTotal(chargeItemTotal.getTotal());
                    break;
                case RoomCharges:
                    chargeItemTotal.setDiscount(getInwardBean().calPatientRoomChargeDiscount(getPatientEncounter()));
                    //   chargeItemTotal.setAdjustedTotal(chargeItemTotal.getTotal());
                    break;
                case MOCharges:
                    chargeItemTotal.setDiscount(getInwardBean().calPatientMoChargeDiscount(getPatientEncounter()));
                    // chargeItemTotal.setAdjustedTotal(chargeItemTotal.getTotal());
                    break;
                case MaintainCharges:
                    chargeItemTotal.setDiscount(getInwardBean().calPatientMaintananceChargeDiscount(getPatientEncounter()));
                    ///   chargeItemTotal.setAdjustedTotal(chargeItemTotal.getTotal());
                    break;
                case NursingCharges:
                    chargeItemTotal.setDiscount(getInwardBean().calPatientNursingChargeDiscount(getPatientEncounter()));
                    //  chargeItemTotal.setAdjustedTotal(chargeItemTotal.getTotal());
                    break;
            }

        }

        updateTotal();
    }

    public void changeAdjustedProValue(BillFee billFee) {
        getBillFeeFacade().edit(billFee);
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {
                case ProfessionalCharge:
                    chargeItemTotal.setAdjustedTotal(getInwardBean().getProfessionalCharge(getPatientEncounter(), childPatientEncouters));
                    break;
            }
        }

        updateTotal();
    }

    public void changeAdjustedValueRoomCharge(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double value = 0;
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {
                case RoomCharges:
                    value = getInwardBean().calPatientRoomChargeAdjusted(getPatientEncounter());
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    chargeItemTotal.setAdjustedTotal(value);
                    break;
            }

        }

        updateTotal();
    }

    public void changeAdjustedValueLinen(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double value = 0;
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {

                case LinenCharges:
                    value = getInwardBean().calPatientRoomLinenChargeAdjusted(getPatientEncounter());
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    chargeItemTotal.setAdjustedTotal(value);
                    break;

            }

        }

        updateTotal();
    }

    public void changeAdjustedValueMedicalCare(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double value = 0;
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {

                case MedicalCareICU:
                    value = getInwardBean().calPatientRoomMadicalCareAdjusted(getPatientEncounter());
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    chargeItemTotal.setAdjustedTotal(value);
                    break;

            }

        }

        updateTotal();
    }

    public void changeAdjustedValueAdministration(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double value = 0;
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {

                case AdministrationCharge:
                    value = getInwardBean().calPatientRoomAdminAdjusted(getPatientEncounter());
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    chargeItemTotal.setAdjustedTotal(value);
                    break;

            }

        }

        updateTotal();
    }

    public void changeAdjustedValueNursing(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double value = 0;
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {

                case NursingCharges:
                    value = getInwardBean().calPatientNursingChargeAdjusted(getPatientEncounter());
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    chargeItemTotal.setAdjustedTotal(value);
                    break;

            }

        }

        updateTotal();
    }

    public void changeAdjustedValueMo(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double value = 0;
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {

                case MOCharges:
                    value = getInwardBean().calPatientMoChargeAdjusted(getPatientEncounter());
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    chargeItemTotal.setAdjustedTotal(value);
                    break;
            }

        }

        updateTotal();
    }

    public void changeAdjustedValueMaintain(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double value = 0;
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {
                case MaintainCharges:
                    value = getInwardBean().calPatientMaintananceChargeAdjusted(getPatientEncounter());
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    chargeItemTotal.setAdjustedTotal(value);
                    break;
            }

        }

        updateTotal();
    }

    public void calculateDiscount() {

        for (ChargeItemTotal cit : chargeItemTotals) {
            double discountValue = 0;
            switch (cit.getInwardChargeType()) {
                case MaintainCharges:
                    discountValue = updatePatientMaintainCharge(cit.getInwardChargeType());
                    break;
                case MOCharges:
                    discountValue = updatePatientMoCharge(cit.getInwardChargeType());
                    break;
                case NursingCharges:
                    discountValue = updatePatientNursingCharge(cit.getInwardChargeType());
                    break;
                case RoomCharges:
                    discountValue = updatePatientRoomCharge(cit.getInwardChargeType());
                    break;
                case MedicalCareICU:
                    discountValue = updatePatientMedicalCareIcuCharge(cit.getInwardChargeType());
                    break;
                case AdministrationCharge:
                    discountValue = updatePatientAdministrationCharge(cit.getInwardChargeType());
                    break;
                case LinenCharges:
                    discountValue = updatePatientLinenCharge(cit.getInwardChargeType());
                    break;
                case Medicine:
                    discountValue = updateIssueBillFees(cit.getInwardChargeType(), BillType.PharmacyBhtPre);
                    break;
                case GeneralIssuing:
                    discountValue = updateIssueBillFees(cit.getInwardChargeType(), BillType.StoreBhtPre);
                    break;
                default:
                    discountValue = discountSet(cit);
            }

            cit.setDiscount(discountValue);
            cit.setAdjustedTotal(cit.getTotal());

        }

    }

    public double discountSet(ChargeItemTotal cit, double discountPercent) {
        if (discountPercent == 0 || cit.getTotal() == 0
                || cit.getInwardChargeType() == InwardChargeType.ProfessionalCharge
                || cit.getInwardChargeType() == InwardChargeType.DoctorAndNurses) {

            cit.setDiscount(0);
            cit.setAdjustedTotal(cit.getTotal());
            return 0;
        }

        double dis = 0;

        dis += updateServiceBillFees(cit.getInwardChargeType(), discountPercent);
        dis += updatePatientItems(cit.getInwardChargeType(), discountPercent);

        //Unknown Total Discount
        //   dis += (getValueForDiscount(cit) * discountPercent) / 100;
        return dis;
    }

    @Inject
    MembershipSchemeController membershipSchemeController;

    public double discountSet(ChargeItemTotal cit) {
//        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatientEncounter().getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());
        PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(), null, getPatientEncounter().getCreditCompany(), cit.getInwardChargeType(), getPatientEncounter().getAdmissionType());
        if (pm == null || pm.getDiscountPercent() == 0 || cit.getTotal() == 0
                || cit.getInwardChargeType() == InwardChargeType.ProfessionalCharge
                || cit.getInwardChargeType() == InwardChargeType.DoctorAndNurses) {

            updateServiceBillFeesWithOutMatrix(cit.getInwardChargeType());
            updatePatientItemsWithOutMatrix(cit.getInwardChargeType());
            cit.setDiscount(0);
            cit.setAdjustedTotal(cit.getTotal());
            return 0;
        }

        double dis = 0;

        dis += updateServiceBillFees(cit.getInwardChargeType(), pm.getDiscountPercent());
        dis += updatePatientItems(cit.getInwardChargeType(), pm.getDiscountPercent());

        //Unknown Total Discount
        //  dis += (getValueForDiscount(cit) * pm.getDiscountPercent()) / 100;
        return dis;
    }

    private double getValueForDiscount(ChargeItemTotal chargeItemTotal) {
        double total = chargeItemTotal.getTotal();

        double serviceValue = getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
        double patientItemTotal = getInwardBean().calTimedPatientItemByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
        double outSide = getInwardBean().calOutSideBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter());

        double value = total - (serviceValue + patientItemTotal + outSide);

        return value;
    }

    private double updateServiceBillFees(InwardChargeType inwardChargeType, double discountPercent) {
        double disTot = 0;
        List<BillFee> list = getInwardBean().getServiceBillFeesByInwardChargeType(inwardChargeType, getPatientEncounter());

        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (BillFee bf : list) {
            double value = bf.getFeeGrossValue() + bf.getFeeMargin();
            double dis = (value * discountPercent) / 100;
            disTot += dis;
            bf.setFeeDiscount(dis);
            bf.setFeeValue(value - dis);
            getBillFeeFacade().edit(bf);
        }

        List<BillItem> listBillItems = getInwardBean().getServiceBillItemByInwardChargeType(inwardChargeType, getPatientEncounter());

        for (BillItem b : listBillItems) {
            getBillBean().updateBillItemByBillFee(b);
        }

        return disTot;
    }

    private void updateServiceBillFeesWithOutMatrix(InwardChargeType inwardChargeType) {
        List<BillFee> list = getInwardBean().getServiceBillFeesByInwardChargeType(inwardChargeType, getPatientEncounter());

        for (BillFee bf : list) {
            double value = bf.getFeeGrossValue() + bf.getFeeMargin();
            bf.setFeeDiscount(0.0);
            bf.setFeeValue(value);
            getBillFeeFacade().edit(bf);
        }

        List<BillItem> listBillItems = getInwardBean().getServiceBillItemByInwardChargeType(inwardChargeType, getPatientEncounter());

        for (BillItem b : listBillItems) {
            getBillBean().updateBillItemByBillFee(b);
        }

    }

    private double updateIssueBillFees(InwardChargeType inwardChargeType, double discountPercent, BillType billType) {
        List<BillItem> listBillItems = getInwardBean().getIssueBillItemByInwardChargeType(getPatientEncounter(), billType);

        double disTot = 0;
        if (listBillItems == null || listBillItems.isEmpty()) {
            return disTot;
        }

        for (BillItem bf : listBillItems) {
            double value = bf.getGrossValue() + bf.getMarginValue();
            double dis = (value * discountPercent) / 100;
            disTot += dis;
            bf.setDiscount(dis);
            bf.setNetValue(value - dis);
            getBillItemFacade().edit(bf);
        }

        disTot += calDiscountServicePatientItems(inwardChargeType, discountPercent);

        return disTot;
    }

    private double updateIssueBillFees(InwardChargeType inwardChargeType, BillType billType) {
        List<BillItem> listBillItems = getInwardBean().getIssueBillItemByInwardChargeType(getPatientEncounter(), billType);

        double disTot = 0;
        if (listBillItems == null || listBillItems.isEmpty()) {
            return disTot;
        }

        PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                null,
                getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType());

        if (pm == null) {
            for (BillItem bf : listBillItems) {
                double value = bf.getGrossValue() + bf.getMarginValue();
                bf.setDiscount(0.0);
                bf.setNetValue(value);
                getBillItemFacade().edit(bf);
                updateServiceBillFeesWithOutMatrix(inwardChargeType);
                updatePatientItemsWithOutMatrix(inwardChargeType);
            }
            return 0;
        }

        for (BillItem bf : listBillItems) {
            double value = bf.getGrossValue() + bf.getMarginValue();
            double dis = (value * pm.getDiscountPercent()) / 100;
//            disTot += dis;
            bf.setDiscount(dis);
            bf.setNetValue(value - dis);
            getBillItemFacade().edit(bf);
        }

        disTot = getInwardBean().calIssueBillItemDiscountByInwardChargeType(getPatientEncounter(), billType);

        disTot += calDiscountServicePatientItems(inwardChargeType);

        List<Bill> bills = getInwardBean().fetchIssueBills(getPatientEncounter(), billType);

        for (Bill b : bills) {
            Double[] dbl = inwardBean.fetchDiscountAndNetTotalByBillItem(b);
            b.setDiscount(dbl[0]);
            b.setNetTotal(dbl[1]);
            billFacade.edit(b);
        }

        return disTot;
    }

    private double updatePatientItems(InwardChargeType inwardChargeType, double discountPercent) {
        List<PatientItem> list = getInwardBean().fetchTimedPatientItemByInwardChargeType(inwardChargeType, getPatientEncounter());
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientItem bf : list) {
            double value = bf.getServiceValue();
            double dis = (value * discountPercent) / 100;
            disTot += dis;
            bf.setDiscount(dis);
            getPatientItemFacade().edit(bf);
        }

        return disTot;
    }

    private void updatePatientItemsWithOutMatrix(InwardChargeType inwardChargeType) {
        List<PatientItem> list = getInwardBean().fetchTimedPatientItemByInwardChargeType(inwardChargeType, getPatientEncounter());

        for (PatientItem bf : list) {
            double value = bf.getServiceValue();
            bf.setDiscount(0.0);
            getPatientItemFacade().edit(bf);
        }

    }

    private double updatePatientRoomCharge(InwardChargeType inwardChargeType) {
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(
                    getPatientEncounter().getPaymentMethod(), null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());

            if (pm != null) {
                disTot += updatePatientRoomCharge(bf, pm.getDiscountPercent());
            } else {
                bf.setDiscountRoomCharge(0.0);
                getPatientRoomFacade().edit(bf);
            }
        }

        disTot += calDiscountServicePatientItems(inwardChargeType);

        return disTot;
    }

    private double updatePatientRoomCharge(PatientRoom patientRoom, double discountPercent) {

        double value = patientRoom.getCalculatedRoomCharge();
        double dis = (value * discountPercent) / 100;

        patientRoom.setDiscountRoomCharge(dis);
        //   patientRoom.setAdjustedRoomCharge(value);
        getPatientRoomFacade().edit(patientRoom);

        return dis;
    }

    private double updatePatientMaintainCharge(InwardChargeType inwardChargeType) {
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        PriceMatrix pm;
        for (PatientRoom bf : list) {
            pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());

            if (pm != null) {
                disTot += updatePatientMaintainCharge(bf, pm.getDiscountPercent());
            } else {
                bf.setDiscountMaintainCharge(0.0);
                getPatientRoomFacade().edit(bf);
            }
        }

        disTot += calDiscountServicePatientItems(inwardChargeType);

        return disTot;
    }

    private double calDiscountServicePatientItems(InwardChargeType inwardChargeType) {
        PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType());
        double disTot = 0;
        if (pm != null) {
            disTot += updateServiceBillFees(inwardChargeType, pm.getDiscountPercent());
            disTot += updatePatientItems(inwardChargeType, pm.getDiscountPercent());
        } else {
            updateServiceBillFeesWithOutMatrix(inwardChargeType);
            updatePatientItemsWithOutMatrix(inwardChargeType);
        }

        return disTot;
    }

    private double calDiscountServicePatientItems(InwardChargeType inwardChargeType, double discount) {
        double disTot = 0;

        disTot += updateServiceBillFees(inwardChargeType, discount);
        disTot += updatePatientItems(inwardChargeType, discount);

        return disTot;
    }

    private double updatePatientMaintainCharge(PatientRoom patientRoom, double discountPercent) {

        double value = patientRoom.getCalculatedMaintainCharge();
        double dis = (value * discountPercent) / 100;
        patientRoom.setDiscountMaintainCharge(dis);
        //   patientRoom.setAdjustedMaintainCharge(value);
        getPatientRoomFacade().edit(patientRoom);

        return dis;
    }

    private double updatePatientMoCharge(InwardChargeType inwardChargeType) {
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());

            if (pm != null) {
                disTot += updatePatientMoCharge(bf, pm.getDiscountPercent());
            } else {
                bf.setDiscountMoCharge(0.0);
                getPatientRoomFacade().edit(bf);
            }
        }

        disTot += calDiscountServicePatientItems(inwardChargeType);

        return disTot;
    }

    private double updatePatientMoCharge(PatientRoom patientRoom, double discountPercent) {

        double value = patientRoom.getCalculatedMoCharge();
        double dis = (value * discountPercent) / 100;
        patientRoom.setDiscountMoCharge(dis);
        // patientRoom.setAdjustedMoCharge(value);
        getPatientRoomFacade().edit(patientRoom);

        return dis;
    }

    private double updatePatientMedicalCareIcuCharge(InwardChargeType inwardChargeType) {
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());

            if (pm != null) {
                disTot += updatePatientMedicalCareIcuCharge(bf, pm.getDiscountPercent());
            } else {
                bf.setDiscountMedicalCareCharge(0.0);
                getPatientRoomFacade().edit(bf);
            }
        }

        disTot += calDiscountServicePatientItems(inwardChargeType);

        return disTot;
    }

    private double updatePatientMedicalCareIcuCharge(PatientRoom patientRoom, double discountPercent) {

        double value = patientRoom.getCalculatedMedicalCareCharge();
        double dis = (value * discountPercent) / 100;
        patientRoom.setDiscountMedicalCareCharge(dis);
        //  patientRoom.setAjdustedMedicalCareCharge(value);
        getPatientRoomFacade().edit(patientRoom);

        return dis;
    }

    private double updatePatientAdministrationCharge(InwardChargeType inwardChargeType) {
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());
            if (pm != null) {
                disTot += updatePatientAdministrationCharge(bf, pm.getDiscountPercent());
            } else {
                bf.setDiscountAdministrationCharge(0.0);
                getPatientRoomFacade().edit(bf);
            }
        }

        disTot += calDiscountServicePatientItems(inwardChargeType);

        return disTot;
    }

    private double updatePatientAdministrationCharge(PatientRoom patientRoom, Double discountPercent) {

        double value = patientRoom.getCalculatedAdministrationCharge();

        double dis = (value * discountPercent) / 100;
        patientRoom.setDiscountAdministrationCharge(dis);
        //  patientRoom.setAjdustedAdministrationCharge(value);
        getPatientRoomFacade().edit(patientRoom);

        return dis;
    }

    private double updatePatientLinenCharge(InwardChargeType inwardChargeType) {
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());
            if (pm != null) {
                disTot += updatePatientLinenCharge(bf, pm.getDiscountPercent());
            } else {
                bf.setDiscountLinenCharge(0.0);
                getPatientRoomFacade().edit(bf);
            }
        }

        disTot += calDiscountServicePatientItems(inwardChargeType);
        return disTot;

    }

    private double updatePatientLinenCharge(PatientRoom patientRoom, double discountPercent) {

        double value = patientRoom.getCalculatedLinenCharge();
        double dis = (value * discountPercent) / 100;
        patientRoom.setDiscountLinenCharge(dis);
        //    patientRoom.setAjdustedLinenCharge(value);
        getPatientRoomFacade().edit(patientRoom);
        return dis;

    }

    private double updatePatientNursingCharge(InwardChargeType inwardChargeType) {
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());

            if (pm != null) {
                disTot += updatePatientNursingCharge(bf, pm.getDiscountPercent());
            } else {
                bf.setDiscountNursingCharge(0.0);
                getPatientRoomFacade().edit(bf);
            }
        }

        disTot += calDiscountServicePatientItems(inwardChargeType);

        return disTot;
    }

    private double updatePatientNursingCharge(PatientRoom patientRoom, double discountPercent) {

        double value = patientRoom.getCalculatedNursingCharge();
        double dis = (value * discountPercent) / 100;
        patientRoom.setDiscountNursingCharge(dis);
        //   patientRoom.setAjdustedNursingCharge(value);
        getPatientRoomFacade().edit(patientRoom);

        return dis;
    }

    public void updatePatientItem(PatientItem patientItem) {
        getInwardTimedItemController().finalizeService(patientItem);
        createPatientItems();
        createChargeItemTotals();

    }

    public void updatePatientRoom(PatientRoom patientRoom) {
        if (patientRoom == null) {
            return;
        }

        List<PatientRoom> rooms = patientRooms;
        if (rooms != null) {
            if (rooms.size() > 1) {
                int currentRoomIndex = rooms.indexOf(patientRoom);
                if (currentRoomIndex > 0) {
                    PatientRoom previousRoom = rooms.get(currentRoomIndex - 1);

                    if (patientRoom.getAdmittedAt() != null && previousRoom.getDischargedAt() != null) {
                        if (patientRoom.getAdmittedAt().before(previousRoom.getDischargedAt())) {
                            JsfUtil.addErrorMessage("Admitted time must be after the discharge time of the previous room.");
                            return;
                        }
                    }
                }
            }
        }

        // If validation passes, save the room (edit or create)
        if (patientRoom.getId() != null) {
            getPatientRoomFacade().edit(patientRoom);
        } else {
            getPatientRoomFacade().create(patientRoom);
        }

        // Refresh the tables or any other necessary actions after saving
        createTables();
    }

    public void updatePrintingPatientRoom(PatientRoom patientRoom) {
        if (patientRoom.getId() != null) {
            getPatientRoomFacade().edit(patientRoom);
        } else {
            getPatientRoomFacade().create(patientRoom);
        }

        patientRoom.setCurrentRoomCharge(patientRoom.getRoomFacilityCharge().getRoomCharge());

        calCulateRoomCharge(patientRoom);

        updatePaitentRoomAdjustedTotal();
    }

    private void updatePaitentRoomAdjustedTotal() {
        for (ChargeItemTotal cit : chargeItemTotals) {
            if (cit.getInwardChargeType() == InwardChargeType.RoomCharges) {
                double dbl = 0;
                for (PatientRoom pr : cit.getPatientRooms()) {
                    if (pr.getReferencePatientRoom() != null) {
                        dbl += pr.getReferencePatientRoom().getCalculatedRoomCharge();
                    }
                }
                cit.setAdjustedTotal(dbl);
            }
        }
    }

    private void calCulateRoomCharge(PatientRoom p) {
        double charge;
        //    System.err.println("1 " + p.getRoomFacilityCharge());
        //   System.err.println("2 " + p.getCurrentRoomCharge());
        if (p.getRoomFacilityCharge() == null || p.getCurrentRoomCharge() == 0) {
            return;
        }

        TimedItemFee timedFee = p.getRoomFacilityCharge().getTimedItemFee();
        double roomCharge = p.getCurrentRoomCharge();

        charge = roomCharge * getInwardBean().calCount(timedFee, p.getAdmittedAt(), p.getDischargedAt());

        p.setCalculatedRoomCharge(charge);
    }

    private boolean checkDischargeTime() {
        if (getPatientEncounter() == null) {
            return true;
        }

        if (getPatientEncounter().getDateOfAdmission() == null) {
            return true;
        }

        if (date == null) {
            return true;
        }

        if (getPatientEncounter().getDateOfAdmission().after(date)) {
            JsfUtil.addErrorMessage("Check Discharge Time should be after Admitted Time");
            return true;
        }

        if (checkRoomIsDischarged()) {
            JsfUtil.addErrorMessage("Please Discharged From Room");
            return true;
        }

        if (getInwardBean().checkRoomDischarge(date, getPatientEncounter())) {
            JsfUtil.addErrorMessage("Check Discharge Time should be after Room Discharge Time");
            return true;
        }

        return false;

    }

    public void checkDate() {
        if (checkDischargeTime()) {
            return;
        }

        makeNull();
        createTables();
    }

    private List<BillItem> billItems;

    @Inject
    BillBeanController billBean;

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    private void updatePaymentBillList() {
        for (Bill bill : getPaymentBill()) {
            //getBillBean().updateInwardDipositList(getCurrent().getPatientEncounter(), b);
            if (getCurrent().getPatientEncounter() != null && bill != null) {
                if (getCurrent().getPatientEncounter().isPaymentFinalized() && getCurrent().getPatientEncounter().getFinalBill() != null) {
                    bill.setForwardReferenceBill(getCurrent().getPatientEncounter().getFinalBill());
                    getBillFacade().edit(bill);
                    if (!getCurrent().getPatientEncounter().getFinalBill().getBackwardReferenceBills().contains(bill)) {
                        getCurrent().getPatientEncounter().getFinalBill().getBackwardReferenceBills().add(bill);
                    }
                    getBillFacade().edit(patientEncounter.getFinalBill());
                }
            }
        }
    }

    public void settleOriginalBill() {
        if (errorCheck()) {
            return;
        }

        saveOriginalBill();
        saveOriginalBillItem();

        JsfUtil.addSuccessMessage("Original Bill Saved");

    }

    public void createTempBill() {
        tempBill = null;
        updateTotal();
        saveTempBill();
        saveTempBillItem();
    }

    public void saveProvisionalBill() {
        if (errorCheck()) {
            return;
        }

        originalBill.setDiscount(discount);
        originalBill.setNetTotal(originalBill.getGrantTotal() - discount);
        getBillFacade().edit(originalBill);

        saveBill();
        getCurrent().setBillTypeAtomic(BillTypeAtomic.INWARD_PROVISIONAL_BILL);
        getCurrent().setBillType(BillType.InwardProvisionalBill);
        getCurrent().setBackwardReferenceBill(originalBill);
        getBillFacade().edit(getCurrent());
        saveBillItem();
        JsfUtil.addSuccessMessage("Provisional Bill Saved");
        showOrginalBill = false;
        printPreview = true;
        originalBill = null;
    }

    public void settle() {
        if (errorCheck()) {
            return;
        }

        originalBill.setDiscount(discount);
        originalBill.setNetTotal(originalBill.getGrantTotal() - discount);
        getBillFacade().edit(originalBill);

        saveBill();
        saveBillItem();

        if (getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
            getInwardBean().updateCreditDetail(getPatientEncounter(), getCurrent().getNetTotal());
            createCreditBillForCreditCompany(getPatientEncounter(), getCurrent().getNetTotal());
        }

        getPatientEncounter().setFinalBill(getCurrent());
        getPatientEncounter().setGrantTotal(getCurrent().getGrantTotal());
        getPatientEncounter().setDiscount(getCurrent().getDiscount());
        getPatientEncounter().setNetTotal(getCurrent().getNetTotal());
        getPatientEncounter().setPaymentFinalized(true);
        getPatientEncounterFacade().edit(getPatientEncounter());
        getCurrent().setReferenceBill(originalBill);
        getBillFacade().edit(getCurrent());

        updatePaymentBillList();
        JsfUtil.addSuccessMessage("Bill Saved");

        showOrginalBill = false;
        printPreview = true;
        originalBill = null;
    }

    public String settleProvisionalBill(Bill b) {

        if (b == null) {
            JsfUtil.addErrorMessage("Error : Bill Not Found!");
            return "";
        }

        setPatientEncounter(b.getPatientEncounter());

        originalBill = b.getBackwardReferenceBill();
        originalBill.setDiscount(b.getDiscount());
        originalBill.setNetTotal(originalBill.getGrantTotal() - b.getDiscount());
        getBillFacade().edit(originalBill);

//        current = new BilledBill();
//        getCurrent().copy(b);
//        getCurrent().copyValue(b);
//        getCurrent().setBillTypeAtomic(BillTypeAtomic.INWARD_FINAL_BILL);
//        getCurrent().setBillType(BillType.InwardFinalBill);
//        getBillFacade().create(getCurrent());
//        
//        for (BillItem bi : b.getBillItems()) {
//            BillItem bin = new BillItem();
//            bin.copy(bi);
//            bin.setBill(getCurrent());
//            getBillItemFacade().create(bin);
//            if (!bi.getProFees().isEmpty() || bi.getProFees() != null) {
//                for (BillFee bf : bi.getProFees()) {
//                    BillFee nbf = new BillFee();
//                    nbf.copy(bf);
//                    nbf.setBill(getCurrent());
//                    nbf.setBillItem(bin);
//                    billFeeFacade.create(nbf);
//                }
//            }
//            if(bi.getBillFees() != null || !bi.getBillFees().isEmpty()){
//                for(BillFee bf : bi.getBillFees()){
//                    BillFee nbf = new BillFee();
//                    nbf.copy(bf);
//                    nbf.setBill(getCurrent());
//                    nbf.setBillItem(bin);
//                    billFeeFacade.create(nbf);
//                }
//            }
//        }
        setCurrent(b);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.INWARD_FINAL_BILL);
        getCurrent().setBillType(BillType.InwardFinalBill);

        getBillFacade().edit(current);

        if (getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
            getInwardBean().updateCreditDetail(getPatientEncounter(), getCurrent().getNetTotal());
            createCreditBillForCreditCompany(getPatientEncounter(), getCurrent().getNetTotal());
        }

        getPatientEncounter().setFinalBill(getCurrent());
        getPatientEncounter().setGrantTotal(getCurrent().getGrantTotal());
        getPatientEncounter().setDiscount(getCurrent().getDiscount());
        getPatientEncounter().setNetTotal(getCurrent().getNetTotal());
        getPatientEncounter().setPaymentFinalized(true);
        getPatientEncounterFacade().edit(getPatientEncounter());
        getCurrent().setReferenceBill(originalBill);
        getBillFacade().edit(getCurrent());

        updatePaymentBillList();
        JsfUtil.addSuccessMessage("Bill Saved");

        showOrginalBill = false;
        printPreview = true;
        originalBill = null;

        return "inward_bill_final?faces-redirect=true";
    }

    public void createCreditBillForCreditCompany(PatientEncounter patientEncounter, Double netTotal) {
        updateTotal();
        Double due = netTotal - (paidByCompany + paidByPatient);
        List<EncounterCreditCompany> encounterCreditCompanys = fillCreditCompaniesByPatient(patientEncounter);
        for (EncounterCreditCompany ecc : encounterCreditCompanys) {
            if (due > 0) {
                if (due > ecc.getCreditLimit()) {
                    saveCreditBillForCreditCompany(patientEncounter, ecc, ecc.getCreditLimit());
                    due = due - ecc.getCreditLimit();
                } else {
                    saveCreditBillForCreditCompany(patientEncounter, ecc, due);
                    due = due - due;
                }
            }
        }
        JsfUtil.addSuccessMessage("Credit Bill Successfully Created.");
    }

    public List<EncounterCreditCompany> fillCreditCompaniesByPatient(PatientEncounter patientEncounter) {
        List<EncounterCreditCompany> encounterCreditCompanys = new ArrayList<>();
        String sql = "select ecc from EncounterCreditCompany ecc"
                + "  where ecc.retired=false "
                + " and ecc.patientEncounter=:pEnc ";
        HashMap hm = new HashMap();
        hm.put("pEnc", patientEncounter);
        encounterCreditCompanys = encounterCreditCompanyFacade.findByJpql(sql, hm);
        return encounterCreditCompanys;
    }

    public void saveCreditBillForCreditCompany(PatientEncounter pe, EncounterCreditCompany ecc, Double value) {
        saveCCBill(pe, ecc, value);
    }

    private boolean checkRoomIsDischarged() {
        for (PatientRoom patientRoom : patientRooms) {
            if (getPatientEncounter().getCurrentPatientRoom().getId() != patientRoom.getId()
                    && patientRoom.getDischargedAt() == null) {
                return true;
            }
        }

        return false;
    }

    private boolean checkPatientItems() {
        List<PatientItem> lst = createPatientItems();

        for (PatientItem pi : lst) {
            if (pi != null && pi.getToTime() == null) {
                return true;
            }
        }

        return false;
    }

    public void dischargeCancel() {

        if (getPatientEncounter().isDischarged() == false) {
            JsfUtil.addErrorMessage("There is no discharge to cancel");
            return;
        }

        if (getPatientEncounter().getCurrentPatientRoom() != null) {
            if (getPatientEncounter().getCurrentPatientRoom().getDischargedAt() == getPatientEncounter().getDateOfDischarge()) {
                getPatientEncounter().getCurrentPatientRoom().setDischargedAt(null);
                getPatientRoomFacade().edit(getPatientEncounter().getCurrentPatientRoom());
            }
        }

        patientEncounter.setDischarged(false);
        patientEncounter.setDateOfDischarge(null);
        getPatientEncounterFacade().edit(patientEncounter);
        JsfUtil.addSuccessMessage("Discharge Cancelled Successfully");

    }

    public void addVat() {
        if (getPatientEncounter() == null) {
            return;
        }
        Double rc = 0.0;
        List<ChargeItemTotal> cts = getChargeItemTotals();
        for (ChargeItemTotal ci : cts) {
            if (ci.getInwardChargeType() == RoomCharges) {
                rc = ci.getNetTotal();
            }
        }

        String j = "select i from Item i where i.inwardChargeType=:ict and i.retired=false order by i.id desc";
        Map m = new HashMap();
        m.put("ict", InwardChargeType.VAT);
        Item i = getItemFacade().findFirstByJpql(j, m);

        if (i == null) {
            JsfUtil.addErrorMessage("No VAT service");
            return;
        } else {

        }

    }

    public void discharge() {
        if (getPatientEncounter() == null) {
            return;
        }

        if (getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage("Patient Already Discharged");
            return;
        }

        if (date == null) {
            JsfUtil.addErrorMessage("Please Enter the Date");
            return;
        }

        if (checkDischargeTime()) {
            return;
        }

        if (checkPatientItems()) {
            JsfUtil.addErrorMessage("Please Finalize Patient Timed Service");
            return;
        }

        getPatientEncounter().setDateOfDischarge(date);
        getDischargeController().setCurrent((Admission) getPatientEncounter());
        getDischargeController().discharge();

        if (getPatientEncounter().isDischarged()) {
            if (getPatientEncounter().getAdmissionType().isRoomChargesAllowed() && getPatientEncounter().getDateOfDischarge() != null) {
                getPatientEncounter().getCurrentPatientRoom().setDischargedAt(getPatientEncounter().getDateOfDischarge());
                roomChangeController.discharge(getPatientEncounter().getCurrentPatientRoom());
            }
        }

        if (getPatientEncounter().getCurrentPatientRoom() != null && getPatientEncounter().getCurrentPatientRoom().getDischargedAt() == null) {
            if (getPatientEncounter().getAdmissionType().isRoomChargesAllowed() || getPatientEncounter().getDateOfDischarge() != null) {
                getPatientEncounter().getCurrentPatientRoom().setDischargedAt(getPatientEncounter().getDateOfDischarge());
                getPatientRoomFacade().edit(getPatientEncounter().getCurrentPatientRoom());
            }
        }

        JsfUtil.addSuccessMessage("Patient  Discharged");

        setPatientEncounter(getPatientEncounter());
        createTables();
    }

    private boolean errorCheck() {
        if (getPatientEncounter() == null) {
            return true;
        }

        if (getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Payment is Finalized U need to cancel Previuios Final Bill of This Bht");
            return true;
        }

        if (checkCatTotal()) {
            return true;
        }

        return false;

    }

    private boolean checkCatTotal() {
        double tot = 0.0;
        double tot2 = 0.0;
        for (ChargeItemTotal cit : getChargeItemTotals()) {
            tot += cit.getTotal();
            tot2 += cit.getAdjustedTotal();
        }

        //   System.err.println("Total " + tot);
        //    System.err.println("Total 2 " + tot2);
        double different = Math.abs((tot - tot2));

        if (different > 0.1) {
            if (!getWebUserController().hasPrivilege("InwardSettleFinalBillUnrestricted")) {
                JsfUtil.addErrorMessage("Please Adjust category amount correctly");
                return true;
            }
        }
        return false;
    }

    @Inject
    IntrimPrintController intrimPrintController;

    public IntrimPrintController getIntrimPrintController() {
        return intrimPrintController;
    }

    public void setIntrimPrintController(IntrimPrintController intrimPrintController) {
        this.intrimPrintController = intrimPrintController;
    }

    public String toPrintItrim() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;
        //  makeNull();
        getIntrimPrintController().makeNull();
        if (getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Please Select Patient Encounter");
            return "";
        }

        createTables();

        getIntrimPrintController().getCurrentBill().setPatientEncounter(getPatientEncounter());
        getIntrimPrintController().getCurrentBill().setTotal(grantTotal);
        getIntrimPrintController().getCurrentBill().setPaidAmount(paid);
        getIntrimPrintController().getCurrentBill().setAdjustedTotal(grantTotal);

        for (ChargeItemTotal cit : chargeItemTotals) {
            BillItem billItem = new BillItem();
            billItem.setInwardChargeType(cit.getInwardChargeType());
            billItem.setBill(getIntrimPrintController().getCurrentBill());
            billItem.setGrossValue(cit.getTotal());
            billItem.setAdjustedValue(cit.getTotal());
            billItem.setReferanceBillItem(getBillBean().fetchBillItem(patientEncounter, BillType.InwardIntrimBill, cit.getInwardChargeType()));
            getIntrimPrintController().getCurrentBill().getBillItems().add(billItem);
        }

        return "inward_bill_intrim_print";
    }

    public boolean checkBill() {
        if (configOptionApplicationController.getBooleanValueByKey("Need to check inward bills before discharge")) {
            return false;
        }

        if (getInwardBean().checkByBillFee(getPatientEncounter(), new BilledBill(), BillType.InwardBill)) {
            JsfUtil.addErrorMessage("Some Inward Service Bills Are Not Checked ");
            return true;
        }

//        if (getInwardBean().checkByBillFee(getPatientEncounter(), new RefundBill(), BillType.InwardBill)) {
//            JsfUtil.addErrorMessage("Some Inward Service Bills Are Not Checked ");
//            return true;
//        }
        if (getInwardBean().checkByBillFee(getPatientEncounter(), new BilledBill(), BillType.InwardProfessional)) {
            JsfUtil.addErrorMessage("Some Inward Pro Bills Are Not Checked ");
            return true;
        }

//        if (getInwardBean().checkByBillFee(getPatientEncounter(), new RefundBill(), BillType.InwardProfessional)) {
//            JsfUtil.addErrorMessage("Some Inward Pro Bills Are Not Checked ");
//            return true;
//        }
        if (getInwardBean().checkByBillItem(getPatientEncounter(), new PreBill(), BillType.PharmacyBhtPre)) {
            JsfUtil.addErrorMessage("Some Pharmacy Issue Bills Are Not Checked 1 ");
            return true;
        }

        if (getInwardBean().checkByBillItem(getPatientEncounter(), new RefundBill(), BillType.PharmacyBhtPre)) {
            JsfUtil.addErrorMessage("Some Pharmacy Issue Bills Are Not Checked 2 ");
            return true;
        }

        if (getInwardBean().checkByBillItem(getPatientEncounter(), new PreBill(), BillType.StoreBhtPre)) {
            JsfUtil.addErrorMessage("Some Store Issue Bills Are Not Checked 1");
            return true;
        }

        if (getInwardBean().checkByBillItem(getPatientEncounter(), new RefundBill(), BillType.StoreBhtPre)) {
            JsfUtil.addErrorMessage("Some Store Issue Bills Are Not Checked 2");
            return true;
        }

        if (getInwardBean().checkByBillItem(getPatientEncounter(), new BilledBill(), BillType.InwardOutSideBill)) {
            JsfUtil.addErrorMessage("Some Inward Out Side Bills Are Not Checked ");
            return true;
        }

//        if (getInwardBean().checkByBillItem(getPatientEncounter(), new RefundBill(), BillType.InwardOutSideBill)) {
//            JsfUtil.addErrorMessage("Some Inward Out Side Bills Are Not Checked ");
//            return true;
//        }
        if (getInwardBean().checkByBillItem(getPatientEncounter(), new BilledBill(), BillType.InwardPaymentBill)) {
            JsfUtil.addErrorMessage("Some Inward Payment Bills Are Not Checked ");
            return true;
        }

        if (getInwardBean().checkByBillItem(getPatientEncounter(), new RefundBill(), BillType.InwardPaymentBill)) {
            JsfUtil.addErrorMessage("Some Inward Payment Bills Are Not Checked ");
            return true;
        }

        return false;
    }

    public String toSettle() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (getPatientEncounter() == null) {
            return "";
        }

        if (!getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage(" Please Discharge This Patient ");
            return "";
        }

        if (getPatientEncounter().getAdmissionType() == null) {
            return "";
        }

        if (getPatientEncounter().getAdmissionType().getAdmissionTypeEnum() == AdmissionTypeEnum.Admission && !getWebUserController().hasPrivilege("InwardBillSettleWithoutCheck")) {
            if (checkBill()) {
                return "";
            }
        }
        if (getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
            if (getPatientEncounter().getCreditCompany() == null) {
                JsfUtil.addErrorMessage("Payment method is Credit So Please Select Credit Company");
            }
        }
        childPatientEncouters = getInwardBean().fetchChildPatientEncounter(patientEncounter);
        createTables();
        calculateDiscount();
        createPatientRooms();
        updateTotal();
        settleOriginalBill();

        return "inward_bill_final?faces-redirect=true;";

    }

    private void saveBill() {

        getCurrent().setGrantTotal(grantTotal);
        getCurrent().setTotal(grantTotal);
        getCurrent().setDiscount(discount);
        getCurrent().setNetTotal(grantTotal - discount);
        getCurrent().setPaidAmount(paid);
        getCurrent().setClaimableTotal(adjustedTotal);
        getCurrent().setSettledAmountBySponsor(paidByCompany);
        getCurrent().setSettledAmountByPatient(paidByPatient);
        getCurrent().setPaymentMethod(getPatientEncounter().getPaymentMethod());
        getCurrent().setCreditCompany(getPatientEncounter().getCreditCompany());
        getCurrent().setInstitution(getSessionController().getInstitution());
        getCurrent().setBillTypeAtomic(BillTypeAtomic.INWARD_FINAL_BILL);
        getCurrent().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.InwardFinalBill, BillClassType.BilledBill, BillNumberSuffix.INWFINAL));
        getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.InwardFinalBill, BillClassType.BilledBill, BillNumberSuffix.INWFINAL));

        getCurrent().setBillType(BillType.InwardFinalBill);

        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());
        getCurrent().setPatientEncounter(patientEncounter);
        getCurrent().setPatient(patientEncounter.getPatient());
//        getCurrent().setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(patientEncounter.getPatient(), getSessionController().getApplicationPreference().isMembershipExpires()));
        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        } else {
            getBillFacade().edit(getCurrent());
        }
    }

    private void saveTempBill() {

        getTempBill().setGrantTotal(grantTotal);
        getTempBill().setTotal(grantTotal);
        getTempBill().setDiscount(discount);
        getTempBill().setNetTotal(grantTotal - discount);
        getTempBill().setPaidAmount(paid);
        getTempBill().setClaimableTotal(adjustedTotal);
        getTempBill().setSettledAmountBySponsor(paidByCompany);
        getTempBill().setSettledAmountByPatient(paidByPatient);
        getTempBill().setPaymentMethod(getPatientEncounter().getPaymentMethod());
        getTempBill().setCreditCompany(getPatientEncounter().getCreditCompany());
        getTempBill().setInstitution(getSessionController().getInstitution());
        getTempBill().setDeptId("Temp/Final Bill");
        getTempBill().setInsId("Temp/Final Bill");

        getTempBill().setBillDate(new Date());
        getTempBill().setBillTime(new Date());
        getTempBill().setPatientEncounter(patientEncounter);
        getTempBill().setPatient(patientEncounter.getPatient());
//        getCurrent().setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(patientEncounter.getPatient(), getSessionController().getApplicationPreference().isMembershipExpires()));
        getTempBill().setCreatedAt(new Date());
        getTempBill().setCreater(getSessionController().getLoggedUser());

    }

    private void saveOriginalBill() {

        getOriginalBill().setGrantTotal(grantTotal);
        getOriginalBill().setTotal(grantTotal);
        getOriginalBill().setDiscount(discount);
        getOriginalBill().setNetTotal(grantTotal - discount);
        getOriginalBill().setPaidAmount(paid);
        getOriginalBill().setClaimableTotal(adjustedTotal);
        getOriginalBill().setSettledAmountBySponsor(paidByCompany);
        getOriginalBill().setSettledAmountByPatient(paidByPatient);
        getOriginalBill().setPaymentMethod(getPatientEncounter().getPaymentMethod());
        getOriginalBill().setCreditCompany(getPatientEncounter().getCreditCompany());
        getOriginalBill().setInstitution(getSessionController().getInstitution());
        getOriginalBill().setBillTypeAtomic(BillTypeAtomic.INWARD_ORIGINAL_FINAL_BILL);
        getOriginalBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.InwardOriginalFinalBill, BillClassType.BilledBill, BillNumberSuffix.INWFINALORG));
        getOriginalBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.InwardOriginalFinalBill, BillClassType.BilledBill, BillNumberSuffix.INWFINALORG));

        getOriginalBill().setBillType(BillType.InwardOriginalFinalBill);

        getOriginalBill().setBillDate(new Date());
        getOriginalBill().setBillTime(new Date());
        getOriginalBill().setPatientEncounter(patientEncounter);
        getOriginalBill().setPatient(patientEncounter.getPatient());
//        getCurrent().setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(patientEncounter.getPatient(), getSessionController().getApplicationPreference().isMembershipExpires()));
        getOriginalBill().setCreatedAt(new Date());
        getOriginalBill().setCreater(getSessionController().getLoggedUser());

        if (getOriginalBill().getId() == null) {
            getBillFacade().create(getOriginalBill());
        } else {
            getBillFacade().edit(getOriginalBill());
        }
    }

    private void saveCCBill(PatientEncounter pe, EncounterCreditCompany ecc, Double value) {

        Bill creditCompanyBill = new BilledBill();

        creditCompanyBill.setGrantTotal(value);
        creditCompanyBill.setTotal(value);
        creditCompanyBill.setNetTotal(value);
        creditCompanyBill.setInstitution(getSessionController().getInstitution());
        creditCompanyBill.setCreditCompany(ecc.getInstitution());
        creditCompanyBill.setPaymentMethod(PaymentMethod.Credit);

        creditCompanyBill.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.InwardFinalBillCCPayment, BillClassType.BilledBill, BillNumberSuffix.INWFINALCCPAY));
        creditCompanyBill.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.InwardFinalBillCCPayment, BillClassType.BilledBill, BillNumberSuffix.INWFINALCCPAY));

        creditCompanyBill.setBillType(BillType.InwardFinalBillCCPayment);
        creditCompanyBill.setBillTypeAtomic(BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);

        creditCompanyBill.setBillDate(new Date());
        creditCompanyBill.setBillTime(new Date());
        creditCompanyBill.setPatientEncounter(patientEncounter);
        creditCompanyBill.setPatient(patientEncounter.getPatient());
//        getCurrent().setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(patientEncounter.getPatient(), getSessionController().getApplicationPreference().isMembershipExpires()));
        creditCompanyBill.setCreatedAt(new Date());
        creditCompanyBill.setCreater(getSessionController().getLoggedUser());
        creditCompanyBill.setReferenceBill(getCurrent());

        if (creditCompanyBill.getId() == null) {
            getBillFacade().create(creditCompanyBill);
        } else {
            getBillFacade().edit(creditCompanyBill);
        }
    }

//    public void edit
    // private void saveAdmissionBillFee
    private void saveBillItem() {
        double temProfFee = 0;
        double temHosFee = 0.0;
        for (ChargeItemTotal cit : chargeItemTotals) {
            BillItem temBi = new BillItem();
            temBi.setBill(getCurrent());
            temBi.setInwardChargeType(cit.getInwardChargeType());
            temBi.setGrossValue(cit.getTotal());
            temBi.setDiscount(cit.getDiscount());
            temBi.setNetValue(cit.getNetTotal());
            temBi.setAdjustedValue(cit.getAdjustedTotal());
            temBi.setCreatedAt(new Date());
            temBi.setCreater(getSessionController().getLoggedUser());

            if (temBi.getId() == null) {
                getBillItemFacade().create(temBi);
            } else {
                getBillItemFacade().edit(temBi);
            }

            if (cit.getInwardChargeType() == InwardChargeType.ProfessionalCharge) {
                updateProBillFee(temBi);
                temProfFee += cit.getAdjustedTotal();
            } else {
                if (configOptionApplicationController.getBooleanValueByKey("Create Professional Bill Fees For Assistant Chargers", false)) {
                    if (cit.getInwardChargeType() == InwardChargeType.DoctorAndNurses) {
                        updateProBillFeeForDocAndNeurses(temBi);;
                    }
                }
                temHosFee += cit.getAdjustedTotal();
            }

            if (cit.getInwardChargeType() == InwardChargeType.RoomCharges) {
                saveRoomBillFee(getPatientRooms(), temBi);
            }

            getBillItemFacade().edit(temBi);

            getCurrent().getBillItems().add(temBi);
        }

        getCurrent().setProfessionalFee(temProfFee);
        getCurrent().setHospitalFee(temHosFee);

        getBillFacade().edit(getCurrent());
    }

    private void saveTempBillItem() {
        double temProfFee = 0;
        double temHosFee = 0.0;
        for (ChargeItemTotal cit : chargeItemTotals) {
            BillItem temBi = new BillItem();
            temBi.setBill(getTempBill());
            temBi.setInwardChargeType(cit.getInwardChargeType());
            temBi.setGrossValue(cit.getTotal());
            temBi.setDiscount(cit.getDiscount());
            temBi.setNetValue(cit.getNetTotal());
            temBi.setAdjustedValue(cit.getAdjustedTotal());
            temBi.setCreatedAt(new Date());
            temBi.setCreater(getSessionController().getLoggedUser());

            if (cit.getInwardChargeType() == InwardChargeType.ProfessionalCharge) {
                updateProTempBillFee(temBi);
                temProfFee += cit.getAdjustedTotal();
            } else {
                if (configOptionApplicationController.getBooleanValueByKey("Create Professional Bill Fees For Assistant Chargers", false)) {
                    if (cit.getInwardChargeType() == InwardChargeType.DoctorAndNurses) {
                        updateProTempBillFeeForDocAndNeurses(temBi);;
                    }
                }
                temHosFee += cit.getAdjustedTotal();
            }

            if (cit.getInwardChargeType() == InwardChargeType.RoomCharges) {
                saveTempRoomBillFee(getPatientRooms(), temBi);
            }

            getTempBill().getBillItems().add(temBi);
        }

        getTempBill().setProfessionalFee(temProfFee);
        getTempBill().setHospitalFee(temHosFee);
    }

    private void saveOriginalBillItem() {
        double temProfFee = 0;
        double temHosFee = 0.0;
        for (ChargeItemTotal cit : chargeItemTotals) {
            BillItem temBi = new BillItem();
            temBi.setBill(getOriginalBill());
            temBi.setInwardChargeType(cit.getInwardChargeType());
            temBi.setGrossValue(cit.getTotal());
            temBi.setDiscount(cit.getDiscount());
            temBi.setNetValue(cit.getNetTotal());
            temBi.setAdjustedValue(cit.getAdjustedTotal());
            temBi.setCreatedAt(new Date());
            temBi.setCreater(getSessionController().getLoggedUser());

            if (temBi.getId() == null) {
                getBillItemFacade().create(temBi);
            } else {
                getBillItemFacade().edit(temBi);
            }

            if (cit.getInwardChargeType() == InwardChargeType.ProfessionalCharge) {
                updateProBillFee(temBi);
                temProfFee += cit.getAdjustedTotal();
            } else {
                temHosFee += cit.getAdjustedTotal();
            }

            if (cit.getInwardChargeType() == InwardChargeType.RoomCharges) {
                saveRoomBillFee(getPatientRooms(), temBi);
            }

            getBillItemFacade().edit(temBi);

            getOriginalBill().getBillItems().add(temBi);
        }

        getOriginalBill().setProfessionalFee(temProfFee);
        getOriginalBill().setHospitalFee(temHosFee);

        getBillFacade().edit(getOriginalBill());
    }

    private void updateProBillFee(BillItem bItem) {
        for (BillFee bf : getProfesionallFee()) {
            bf.setReferenceBillItem(bItem);
            getBillFeeFacade().edit(bf);

            bItem.getProFees().add(bf);

        }

    }

    private void updateProTempBillFee(BillItem bItem) {
        for (BillFee bf : getProfesionallFee()) {
            bf.setReferenceBillItem(bItem);

            bItem.getProFees().add(bf);

        }

    }

    private void updateProBillFeeForDocAndNeurses(BillItem bItem) {
        for (BillFee bf : getDoctorAndNurseFee()) {
            bf.setReferenceBillItem(bItem);
            getBillFeeFacade().edit(bf);

            bItem.getProFees().add(bf);

        }

    }

    private void updateProTempBillFeeForDocAndNeurses(BillItem bItem) {
        for (BillFee bf : getDoctorAndNurseFee()) {
            bf.setReferenceBillItem(bItem);

            bItem.getProFees().add(bf);

        }

    }

    private void saveRefencePatientRoom(PatientRoom pr) {
        if (pr.getId() == null) {
            getPatientRoomFacade().create(pr);
        } else {
            getPatientRoomFacade().edit(pr);
        }
    }

    private void saveRoomBillFee(List<PatientRoom> patientRooms, BillItem bItem) {
        List<BillFee> list = new ArrayList<>();
        for (PatientRoom pt : patientRooms) {
            BillFee tmp = new BillFee();
            tmp.setBill(bItem.getBill());
            tmp.setBillItem(bItem);

            saveRefencePatientRoom(pt);

            tmp.setReferencePatientRoom(pt);

            if (tmp.getId() == null) {
                getBillFeeFacade().create(tmp);
            } else {
                getBillFeeFacade().edit(tmp);
            }

            list.add(tmp);

        }

        bItem.setBillFees(list);

    }

    private void saveTempRoomBillFee(List<PatientRoom> patientRooms, BillItem bItem) {
        List<BillFee> list = new ArrayList<>();
        for (PatientRoom pt : patientRooms) {
            BillFee tmp = new BillFee();
            tmp.setBill(bItem.getBill());
            tmp.setBillItem(bItem);

            tmp.setReferencePatientRoom(pt);
            list.add(tmp);

        }

        bItem.setBillFees(list);

    }

    public void createIntrimBillTable() {
        if (configOptionApplicationController.getBooleanValueByKey("Restrict Access to Intrim Bill if Provisional Bill is Created")) {
            if (patientEncounter != null
                    && admissionController.isAddmissionHaveProvisionalBill((Admission) patientEncounter)) {
                JsfUtil.addErrorMessage("There is a Provisional Bill For This Admission");
                clear();            // resets patientEncounter and cached data safely
                return;
            }
        }
        childPatientEncouters = null;
        createTables();
    }

    public void createTables() {
        makeNull();

        if (patientEncounter == null) {
            return;
        }

        if (childPatientEncouters == null || childPatientEncouters.isEmpty()) {
            childPatientEncouters = getInwardBean().fetchChildPatientEncounter(getPatientEncounter());
        }

        createPatientRooms();
        createPatientItems();
        pharmacyIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.PharmacyBhtPre, childPatientEncouters);
        storeIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.StoreBhtPre, childPatientEncouters);
        departmentBillItems = getInwardBean().createDepartmentBillItems(patientEncounter, null, childPatientEncouters);
        additionalChargeBill = getInwardBean().fetchOutSideBill(getPatientEncounter(), childPatientEncouters);
        getInwardBean().setProfesionallFeeAdjusted(getPatientEncounter(), childPatientEncouters);
        profesionallFee = getInwardBean().createProfesionallFee(getPatientEncounter(), childPatientEncouters);
        doctorAndNurseFee = getInwardBean().createDoctorAndNurseFee(getPatientEncounter(), childPatientEncouters);
        paymentBill = getInwardBean().fetchPaymentBill(getPatientEncounter(), childPatientEncouters);

        updateRoomChargeList();
        createChargeItemTotals();

        updateTotal();
        JsfUtil.addSuccessMessage("Recalculated Successfully");

        if (patientEncounter != null && patientEncounter.getDateOfDischarge() != null) {
            date = patientEncounter.getDateOfDischarge();
        } else {
            date = null;
        }

    }

    public void createTablesWithEstimatedProfessionalFees() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        makeNull();

        if (patientEncounter == null) {
            return;
        }

        if (childPatientEncouters == null || childPatientEncouters.isEmpty()) {
            childPatientEncouters = getInwardBean().fetchChildPatientEncounter(getPatientEncounter());
        }

        createPatientRooms();
        createPatientItems();
        pharmacyIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.PharmacyBhtPre, childPatientEncouters);
        storeIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.StoreBhtPre, childPatientEncouters);
        departmentBillItems = getInwardBean().createDepartmentBillItems(patientEncounter, null, childPatientEncouters);
        additionalChargeBill = getInwardBean().fetchOutSideBill(getPatientEncounter(), childPatientEncouters);
        getInwardBean().setProfesionallFeeAdjusted(getPatientEncounter(), childPatientEncouters);
        profesionallFee = getInwardBean().createProfesionallFeeEstimated(getPatientEncounter());
        doctorAndNurseFee = getInwardBean().createDoctorAndNurseFee(getPatientEncounter(), childPatientEncouters);
        paymentBill = getInwardBean().fetchPaymentBill(getPatientEncounter(), childPatientEncouters);

        updateRoomChargeList();
        createChargeItemTotals();

        updateTotal();

        if (patientEncounter != null && patientEncounter.getDateOfDischarge() != null) {
            date = patientEncounter.getDateOfDischarge();
        } else {
            date = null;
        }

    }

    private List<PatientItem> createPatientItems() {
        patientItems = getInwardBean().fetchPatientItem(getPatientEncounter(), childPatientEncouters);

        if (patientItems == null) {
            patientItems = new ArrayList<>();
        }

        for (PatientItem pi : patientItems) {
            TimedItemFee timedItemFee = getInwardBean().getTimedItemFee((TimedItem) pi.getItem());
            double count = getInwardBean().calCount(timedItemFee, pi.getFromTime(), pi.getToTime());
            pi.setServiceValue(count * timedItemFee.getFee());
        }

        return patientItems;
    }

    public List<PatientItem> getPatientItems() {
        if (patientItems == null) {
            patientItems = createPatientItems();
        }

        return patientItems;
    }

    public void finalizeService(PatientItem patientItem) {
        if (patientItem.getToTime() != null) {
            if (patientItem.getToTime().before(patientItem.getFromTime())) {
                JsfUtil.addErrorMessage("Service Not Finalize check Service Start Time & End Time");
                return;
            }
        }

        if (patientItem.getToTime() == null) {
            patientItem.setToTime(Calendar.getInstance().getTime());
        }

        TimedItemFee timedItemFee = getInwardBean().getTimedItemFee((TimedItem) patientItem.getItem());
        double count = getInwardBean().calCount(timedItemFee, patientItem.getFromTime(), patientItem.getToTime());
        patientItem.setServiceValue(count * timedItemFee.getFee());

        getPatientItemFacade().edit(patientItem);

        createPatientItems();

    }

    public void makeNull() {
        changed = false;
        chargeItemTotals = null;
        grantTotal = 0.0;
        discount = 0.0;
        due = 0.0;
        paid = 0.0;
        profesionallFee = null;
        doctorAndNurseFee = null;
        patientItems = null;
        paymentBill = null;
        departmentBillItems = null;
        printPreview = false;
        current = null;
        tmpPI = null;
        currentTime = null;
        toTime = null;
        patientRooms = null;
    }

    public void clear() {
        patientEncounter = null;
        makeNull();
    }

    public List<BillItem> getSummaryOfDoctorChargers(List<BillItem> bi, PatientEncounter pe) {
        List<BillItem> newBillItems = new ArrayList<>();
        Map<Staff, BillFee> staffFeeMap = new HashMap<>();
        double totalFee = 0.0;

        for (BillItem i : bi) {
            if ((i.getInwardChargeType() == InwardChargeType.ProfessionalCharge
                    || i.getInwardChargeType() == InwardChargeType.DoctorAndNurses)
                    && i.getAdjustedValue() != 0) {
//                System.out.println("i = " + i);
//                System.out.println("i.getInwardChargeType() = " + i.getInwardChargeType());

                if (i.getProFees() == null) {
                    List<BillFee> docAndNurseFee = new ArrayList<>();
                    for (BillFee bf : getInwardBean().createDoctorAndNurseFee(pe, childPatientEncouters)) {
                        bf.setFeeAdjusted(bf.getFeeValue());
                        docAndNurseFee.add(bf);
                        i.setProFees(docAndNurseFee);
                    }
                }

                for (BillFee bf : i.getProFees()) {
                    Staff staff = bf.getStaff();
//                    System.out.println("staff = " + staff.getPerson().getNameWithTitle());
//                    System.out.println("bf.fee = " + bf.getFeeAdjusted());
//                    System.out.println("bf.feeV = " + bf.getFeeValue());

                    if (staffFeeMap.containsKey(staff)) {
                        if (bf.getFeeAdjusted() > 0) {
                            staffFeeMap.get(staff).setFeeAdjusted(staffFeeMap.get(staff).getFeeAdjusted() + bf.getFeeAdjusted());
                        } else {
                            staffFeeMap.get(staff).setFeeAdjusted(staffFeeMap.get(staff).getFeeAdjusted() + bf.getFeeValue());
                        }
                    } else {
                        BillFee newBillFee = new BillFee();
                        newBillFee.setStaff(staff);
                        if (bf.getFeeAdjusted() > 0) {
                            newBillFee.setFeeAdjusted(bf.getFeeAdjusted());
                        } else {
                            newBillFee.setFeeAdjusted(bf.getFeeValue());
                        }

                        staffFeeMap.put(staff, newBillFee);
                    }
                }

                totalFee += i.getAdjustedValue();
            }
        }

        List<BillFee> proFees = new ArrayList<>(staffFeeMap.values());

        if (!proFees.isEmpty()) {
            BillItem newBillItem = new BillItem();
            newBillItem.setInwardChargeType(InwardChargeType.ProfessionalCharge);
            newBillItem.setProFees(proFees);
            newBillItem.setAdjustedValue(totalFee);
            newBillItems.add(newBillItem);
        }

        return newBillItems;
    }

    public String navigateToIntrimBill() {
        patientEncounter = null;
        makeNull();
        return "/inward/inward_bill_intrim?faces-redirect=true";
    }

    public String navigateToIntrimBillFromPatientProfile() {
        childPatientEncouters = null;
        createTables();
        return "/inward/inward_bill_intrim?faces-redirect=true";
    }

    public String toIntrimBillclear() {
        patientEncounter = null;
        makeNull();
        return "/inward/inward_bill_intrim?faces-redirect=true";
    }

    public void updateAdmissionFee(AdmissionType at) {
        getAdmissionTypeFacade().edit(at);
        createTables();
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
//        makeNull();
        this.patientEncounter = patientEncounter;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    private List<PatientRoom> createPatientRooms() {

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);

        setPatientRoomData();
        // totalLinen = getInwardBean().calTotalLinen(tmp);

        return patientRooms;
    }

    private void setPatientRoomData() {

        for (PatientRoom p : patientRooms) {
            if (p.getAdmittedAt() == null) {
                p.setAdmittedAt(new Date());
            }
            calculateRoomCharge(p);
            calculateMaintananceCharge(p);
            calculateLinenCharge(p);
            if (!(p instanceof GuardianRoom)) {
                calculateNursingCharge(p);
                calculateMoCharge(p);
                calculateAdministrationCharge(p);
                calculateMedicalCareCharge(p);
            }

            p.setAdjustedMaintainCharge(p.getCalculatedMaintainCharge());
            p.setAdjustedMoCharge(p.getCalculatedMoCharge());
            p.setAdjustedRoomCharge(p.getCalculatedRoomCharge());

            p.setAjdustedAdministrationCharge(p.getCalculatedAdministrationCharge());
            p.setAjdustedLinenCharge(p.getCalculatedLinenCharge());
            p.setAjdustedMedicalCareCharge(p.getCalculatedMedicalCareCharge());
            p.setAjdustedNursingCharge(p.getCalculatedNursingCharge());

            getPatientRoomFacade().edit(p);

        }
    }

    private void calculateLinenCharge(PatientRoom p) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentLinenCharge() == 0.0) {
            p.setCalculatedLinenCharge(0);
            return;
        }

        double linen = p.getCurrentLinenCharge();
        Date dischargedAt = p.getDischargedAt();
        ////System.out.println("dischargedAt = " + dischargedAt);
        ////System.out.println("linen = " + linen);
        if (dischargedAt == null) {
            dischargedAt = new Date();
        }

        double extra = p.getAddedLinenCharge();
        ////System.out.println("extra = " + extra);
        if (CommonFunctions.checkToDateAreInSameDay(p.getAdmittedAt(), dischargedAt)) {
            if (p.getAdmittedAt().equals(dischargedAt)) {
                p.setCalculatedLinenCharge(0 + extra);
                ////System.out.println("1.1 p.getCalculatedLinenCharge() = " + p.getCalculatedLinenCharge());
            } else {
                p.setCalculatedLinenCharge(linen + extra);
                ////System.out.println("1.2 p.getCalculatedLinenCharge() = " + p.getCalculatedLinenCharge());
            }
        } else {
            p.setCalculatedLinenCharge((linen * CommonFunctions.getDayCount(p.getAdmittedAt(), dischargedAt)) + extra);
            ////System.out.println("2 p.getCalculatedLinenCharge() = " + p.getCalculatedLinenCharge());
        }
    }

    private void calculateMoCharge(PatientRoom p) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentMoCharge() == 0.0) {
            p.setCalculatedMoCharge(0);
            return;
        }

        if (!sessionController.getApplicationPreference().isInwardMoChargeCalculateInitialTime()) {
            double mo = p.getCurrentMoCharge();
            double calculated = getCharge(p, mo) + p.getAddedMoCharge();
            p.setCalculatedMoCharge(calculated);
        } else {
            Date dischargedAt = p.getDischargedAt();
            long dCount = CommonFunctions.getDayCount(p.getAdmittedAt(), dischargedAt);

            if (dCount <= p.getRoomFacilityCharge().getTimedItemFee().getDurationDaysForMoCharge()) {
                double calculated = p.getCurrentMoCharge() + p.getAddedMoCharge();
                p.setCalculatedMoCharge(calculated);
            } else {
                long extra = dCount - p.getRoomFacilityCharge().getTimedItemFee().getDurationDaysForMoCharge();
                double calculated = (p.getCurrentMoChargeForAfterDuration() * extra) + p.getCurrentMoCharge();
                p.setCalculatedMoCharge(calculated);
            }
        }
    }

    private void calculateAdministrationCharge(PatientRoom p) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentAdministrationCharge() == 0.0) {
            p.setCalculatedAdministrationCharge(0);
            return;
        }

        double adm = p.getCurrentAdministrationCharge();
        double calculated = getCharge(p, adm) + p.getAddedAdministrationCharge();
        p.setCalculatedAdministrationCharge(calculated);
    }

    private void calculateMedicalCareCharge(PatientRoom p) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentMedicalCareCharge() == 0.0) {
            p.setCalculatedMedicalCareCharge(0);
            return;
        }

        double med = p.getCurrentMedicalCareCharge();
        double calculated = getCharge(p, med) + p.getAddedMedicalCareCharge();
        p.setCalculatedMedicalCareCharge(calculated);
    }

    private void calculateNursingCharge(PatientRoom p) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentNursingCharge() == 0) {
            p.setCalculatedNursingCharge(0);
            return;
        }

        double nursing = p.getCurrentNursingCharge();
        double calculated = getCharge(p, nursing) + p.getAddedNursingCharge();

        p.setCalculatedNursingCharge(calculated);
    }

    private void calculateRoomCharge(PatientRoom p) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentRoomCharge() == 0) {
            p.setCalculatedRoomCharge(0);
            return;
        }

        double roomCharge = p.getCurrentRoomCharge();
        ////System.out.println("roomCharge = " + roomCharge);
        double calculated = getCharge(p, roomCharge) + p.getAddedRoomCharge();
        ////System.out.println("calculated = " + calculated);

        p.setCalculatedRoomCharge(calculated);
    }

    private double getCharge(PatientRoom patientRoom, double value) {
        ////System.out.println("value = " + value);
        ////System.out.println("patientRoom = " + patientRoom);
        TimedItemFee timedFee = patientRoom.getRoomFacilityCharge().getTimedItemFee();
        ////System.out.println("timedFee = " + timedFee);
        Date dischargeAt = patientRoom.getDischargedAt();
        ////System.out.println("dischargeAt = " + dischargeAt);

        if (dischargeAt == null) {
            dischargeAt = new Date();
        }

        if (getPatientEncounter().getCurrentPatientRoom() == null) {
            return 0;
        }

        if (getPatientEncounter().getCurrentPatientRoom().equals(patientRoom)) {
            if (patientRoom.isDischarged()) {
                //System.out.println("value * getInwardBean().calCount(timedFee, patientRoom.getAdmittedAt(), dischargeAt); = " + value * getInwardBean().calCount(timedFee, patientRoom.getAdmittedAt(), dischargeAt));
                return value * getInwardBean().calCount(timedFee, patientRoom.getAdmittedAt(), dischargeAt);
            } else {
                // System.out.println("value * getInwardBean().calCountWithoutOverShoot(timedFee, patientRoom.getAdmittedAt(), dischargeAt) = " + value * getInwardBean().calCountWithoutOverShoot(timedFee, patientRoom.getAdmittedAt(), dischargeAt));
                return value * getInwardBean().calCountWithoutOverShoot(timedFee, patientRoom.getAdmittedAt(), dischargeAt);
            }

        } else {
            //System.out.println("value * getInwardBean().calCount(timedFee, patientRoom.getAdmittedAt(), dischargeAt) = " + value * getInwardBean().calCount(timedFee, patientRoom.getAdmittedAt(), dischargeAt));
            return value * getInwardBean().calCount(timedFee, patientRoom.getAdmittedAt(), dischargeAt);
        }

    }

    private void calculateMaintananceCharge(PatientRoom p) {
        if (p.getRoomFacilityCharge() == null || p.getCurrentMaintananceCharge() == 0) {
            p.setCalculatedMaintainCharge(0);
            return;
        }
        double maintanance = p.getCurrentMaintananceCharge();
        double calculated = getCharge(p, maintanance) + p.getAddedMaintainCharge();

        p.setCalculatedMaintainCharge(calculated);
    }

    public PatientRoomFacade getPatientRoomFacade() {
        return patientRoomFacade;
    }

    public void setPatientRoomFacade(PatientRoomFacade patientRoomFacade) {
        this.patientRoomFacade = patientRoomFacade;
    }

    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private ItemFacade itemFacade;

    public ServiceFacade getServiceFacade() {
        return serviceFacade;
    }

    public void setServiceFacade(ServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    public List<BillFee> getProfesionallFee() {
        if (profesionallFee == null) {
            profesionallFee = getInwardBean().createProfesionallFee(getPatientEncounter(), childPatientEncouters);
        }
        return profesionallFee;
    }

    public void setProfesionallFee(List<BillFee> profesionallFee) {
        this.profesionallFee = profesionallFee;
    }

    public List<Bill> getPaymentBill() {
        if (paymentBill == null) {
            paymentBill = getInwardBean().fetchPaymentBill(getPatientEncounter(), childPatientEncouters);
        }
        return paymentBill;
    }

    public void setPaymentBill(List<Bill> paymentBill) {
        this.paymentBill = paymentBill;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public double getPaid() {
        return paid;
    }

    public void setPaid(double paid) {
        this.paid = paid;
    }

    public void calFinalValue() {
        grantTotal = 0;
        discount = 0;
        adjustedTotal = 0;
        for (ChargeItemTotal c : getChargeItemTotals()) {
            grantTotal += c.getTotal();
            discount += c.getDiscount();
            adjustedTotal += c.getAdjustedTotal();
        }
    }

    double adjustedTotal = 0;

    public double getGrantTotal() {
        return grantTotal;
    }

    public void setGrantTotal(double grantTotal) {
        this.grantTotal = grantTotal;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getDue() {
        return due;
    }

    public void setDue(double due) {
        this.due = due;
    }

    public Date getCurrentTime() {
        currentTime = Calendar.getInstance().getTime();

        return currentTime;
    }

    public void setCurrentTime(Date currentTime) {
        this.currentTime = currentTime;
    }

    public PatientItemFacade getPatientItemFacade() {
        return patientItemFacade;
    }

    public void setPatientItemFacade(PatientItemFacade patientItemFacade) {
        this.patientItemFacade = patientItemFacade;
    }

    public TimedItemFeeFacade getTimedItemFeeFacade() {
        return timedItemFeeFacade;
    }

    public void setTimedItemFeeFacade(TimedItemFeeFacade timedItemFeeFacade) {
        this.timedItemFeeFacade = timedItemFeeFacade;
    }

    private void createChargeItemTotals() {
        chargeItemTotals = new ArrayList<>();

        for (InwardChargeType i : InwardChargeType.values()) {
            ChargeItemTotal cit = new ChargeItemTotal();
            cit.setInwardChargeType(i);

            chargeItemTotals.add(cit);
        }

        if (getPatientEncounter() != null) {
            setKnownChargeTot();
            setServiceTotCategoryWise();
            setTimedServiceTotCategoryWise();
            setChargeValueFromAdditional();

        }

        setNetAdjustValue();

    }

    private void setNetAdjustValue() {
        for (ChargeItemTotal cit : chargeItemTotals) {
            cit.setAdjustedTotal(cit.getTotal());
        }
    }

    private void setChargeValueFromAdditional() {
        for (ChargeItemTotal cit : chargeItemTotals) {
            double adj = getInwardBean().caltValueFromAdditionalCharge(cit.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
            double tot = cit.getTotal();

            cit.setTotal(tot + adj);
        }
    }

    private void updateRoomChargeList() {

        for (PatientRoom rcd : patientRooms) {
            getPatientRoomFacade().edit(rcd);
        }

    }

    @Inject
    private InwardBeanController inwardBean;

    public void updateTotal() {
        calFinalValue();

        if (configOptionApplicationController.getBooleanValueByKey("Allow Final Bill Total Without Restrictions & Price Difference")) {
            grantTotal = adjustedTotal;
        }

        paid = getInwardBean().getPaidValue(getPatientEncounter());
        paidByPatient = getInwardBean().getPaidByPatientValue(getPatientEncounter());
        paidByCompany = getInwardBean().getPaidByCompanyValue(getPatientEncounter());

        due = (grantTotal - discount) - paid;

//        if (getPatientEncounter().getCreditLimit() != 0) {
//            due -= getPatientEncounter().getCreditLimit();
//        }
        changed = false;

    }

    public void changeIsMade() {
        changed = true;
    }

    public void listnerDiscontAmmountChanged() {
        due = (grantTotal - discount) - paid;
    }

    public boolean isShowOrginalBill() {
        return showOrginalBill;
    }

    public void setShowOrginalBill(boolean showOrginalBill) {
        this.showOrginalBill = showOrginalBill;
    }

    public List<ChargeItemTotal> getChargeItemTotals() {
        if (chargeItemTotals == null) {
            if (childPatientEncouters == null || childPatientEncouters.isEmpty()) {
                childPatientEncouters = getInwardBean().fetchChildPatientEncounter(getPatientEncounter());
            }
            createChargeItemTotals();
        }
        return chargeItemTotals;
    }

    public void onEdit(RowEditEvent event) {
    }

    private List<Bill> additionalChargeBill;

    private void setKnownChargeTot() {

        for (ChargeItemTotal i : chargeItemTotals) {
            switch (i.getInwardChargeType()) {
                case AdmissionFee:
                    if (getPatientEncounter().getAdmissionType() != null) {
                        i.setTotal(getInwardBean().getAdmissionCharge(getPatientEncounter(), childPatientEncouters));
                    }
                    break;
                case RoomCharges:
                    i.setTotal(getInwardBean().getRoomCharge(getPatientEncounter(), childPatientEncouters));
                    break;
                case MOCharges:
                    i.setTotal(getInwardBean().getMoCharge(getPatientEncounter(), childPatientEncouters));
                    break;
                case NursingCharges:
                    i.setTotal(getInwardBean().getNursingCharge(getPatientEncounter(), childPatientEncouters));
                    break;
                case MaintainCharges:
                    i.setTotal(getInwardBean().getMaintainCharge(getPatientEncounter(), childPatientEncouters));
                    break;
                case MedicalCareICU:
                    i.setTotal(getInwardBean().getMedicalCareIcuCharge(getPatientEncounter(), childPatientEncouters));
                    break;
                case AdministrationCharge:
                    i.setTotal(getInwardBean().getAdminCharge(getPatientEncounter(), childPatientEncouters));
                    break;
                case LinenCharges:
                    i.setTotal(getInwardBean().getLinenCharge(getPatientEncounter(), childPatientEncouters));
                    break;
                case Medicine:
                    List<BillTypeAtomic> btas = new ArrayList<>();
                    btas.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
                    btas.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED);
                    btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE);
                    btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN);
                    btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION);
                    btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD);
                    btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN);
                    btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION);
                    i.setTotal(getInwardBean().calCostOfIssueByBill(getPatientEncounter(), btas, childPatientEncouters));
                    break;
                case GeneralIssuing:
                    i.setTotal(getInwardBean().calCostOfIssue(getPatientEncounter(), BillType.StoreBhtPre, childPatientEncouters));
                    break;
                case ProfessionalCharge:
                    i.setTotal(getInwardBean().calculateProfessionalCharges(getPatientEncounter(), childPatientEncouters));
                    break;
                case DoctorAndNurses:
                    i.setTotal(getInwardBean().calculateDoctorAndNurseCharges(getPatientEncounter(), childPatientEncouters));
                    break;
            }
        }
    }

    private void setServiceTotCategoryWise() {
        for (ChargeItemTotal ch : chargeItemTotals) {
            ch.setTotal(ch.getTotal() + getInwardBean().calServiceBillItemsTotalByInwardChargeType(ch.getInwardChargeType(), getPatientEncounter(), childPatientEncouters));
        }
    }

    public List<InwardBillItem> getInwardBillItemByType() {
        List<InwardBillItem> inwardBillItems = new ArrayList<>();
        for (InwardChargeType i : InwardChargeType.values()) {
            InwardBillItem tmp = new InwardBillItem();
            tmp.setInwardChargeType(i);
            tmp.setBillItems(getInwardBean().getService(i, getPatientEncounter()));
            inwardBillItems.add(tmp);
        }

        return inwardBillItems;

    }

    public void calculateDuration() {
        if (patientEncounter.getDateOfAdmission() != null && patientEncounter.getDateOfDischarge() != null) {
            // Convert java.util.Date to LocalDateTime
            LocalDateTime admissionDateTime = patientEncounter.getDateOfAdmission().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            LocalDateTime dischargeDateTime = patientEncounter.getDateOfDischarge().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // Calculate duration between admission and discharge
            Duration durationObj = Duration.between(admissionDateTime, dischargeDateTime);

            long days = durationObj.toDays();
            long hours = durationObj.toHours() % 24;
            long minutes = durationObj.toMinutes() % 60;
            long seconds = durationObj.getSeconds() % 60;

            // Format the result as a string
            this.duration = String.format("%d days, %d hours, %d minutes, %d seconds", days, hours, minutes, seconds);
        }
    }

    private void setTimedServiceTotCategoryWise() {

        for (ChargeItemTotal ch : chargeItemTotals) {
            ch.setTotal(ch.getTotal() + getInwardBean().getTimedItemFeeTotalByInwardChargeType(ch.getInwardChargeType(), getPatientEncounter(), childPatientEncouters));
        }

    }

    public void setChargeItemTotals(List<ChargeItemTotal> chargeItemTotals) {
        this.chargeItemTotals = chargeItemTotals;
    }

    public Bill getCurrent() {
        if (current == null) {
            current = new BilledBill();
        }
        return current;
    }

    public void setCurrent(Bill current) {
        this.current = current;
    }

    public Bill getOriginalBill() {
        if (originalBill == null) {
            originalBill = new BilledBill();
        }
        return originalBill;
    }

    public void setOriginalBill(Bill originalBill) {
        this.originalBill = originalBill;
    }

    public Bill getTempBill() {
        if (tempBill == null) {
            tempBill = new BilledBill();
        }
        return tempBill;
    }

    public void setTempBill(Bill tempBill) {
        this.tempBill = tempBill;
    }

    public void prepareNewBill() {
        patientEncounter = null;
        makeNull();

    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public DischargeController getDischargeController() {
        return dischargeController;
    }

    public void setDischargeController(DischargeController dischargeController) {
        this.dischargeController = dischargeController;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public InwardTimedItemController getInwardTimedItemController() {
        return inwardTimedItemController;
    }

    public void setInwardTimedItemController(InwardTimedItemController inwardTimedItemController) {
        this.inwardTimedItemController = inwardTimedItemController;
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public List<Bill> getAdditionalChargeBill() {
        if (additionalChargeBill == null) {
            additionalChargeBill = getInwardBean().fetchOutSideBill(getPatientEncounter(), childPatientEncouters);
        }
        return additionalChargeBill;
    }

    public void setAdditionalChargeBill(List<Bill> additionalChargeBill) {
        this.additionalChargeBill = additionalChargeBill;
    }

    public PatientItem getTmpPI() {
        return tmpPI;
    }

    public void setTmpPI(PatientItem tmpPI) {
        this.tmpPI = tmpPI;
    }

    public Date getToTime() {
        return toTime;
    }

    public void setToTime(Date toTime) {
        this.toTime = toTime;
    }

    public List<DepartmentBillItems> getDepartmentBillItems() {
        if (departmentBillItems == null) {
            departmentBillItems = getInwardBean().createDepartmentBillItems(patientEncounter, null, childPatientEncouters);
        }
        return departmentBillItems;
    }

    public void setDepartmentBillItems(List<DepartmentBillItems> departmentBillItems) {
        this.departmentBillItems = departmentBillItems;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public InwardMemberShipDiscount getInwardMemberShipDiscount() {
        return inwardMemberShipDiscount;
    }

    public void setInwardMemberShipDiscount(InwardMemberShipDiscount inwardMemberShipDiscount) {
        this.inwardMemberShipDiscount = inwardMemberShipDiscount;
    }

    public List<Bill> getPharmacyIssues() {
        return pharmacyIssues;
    }

    public void setPharmacyIssues(List<Bill> pharmacyIssues) {
        this.pharmacyIssues = pharmacyIssues;
    }

    public List<Bill> getStoreIssues() {
        return storeIssues;
    }

    public void setStoreIssues(List<Bill> storeIssues) {
        this.storeIssues = storeIssues;
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public double getAdjustedTotal() {
        return adjustedTotal;
    }

    public void setAdjustedTotal(double adjustedTotal) {
        this.adjustedTotal = adjustedTotal;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        billItems = getInwardBean().createBillItems(item, getPatientEncounter());
        this.item = item;
    }

    public List<BillItem> getPharmacyItems() {
        return pharmacyItems;
    }

    public void setPharmacyItems(List<BillItem> pharmacyItems) {
        this.pharmacyItems = pharmacyItems;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public AdmissionTypeFacade getAdmissionTypeFacade() {
        return admissionTypeFacade;
    }

    public void setAdmissionTypeFacade(AdmissionTypeFacade admissionTypeFacade) {
        this.admissionTypeFacade = admissionTypeFacade;
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    public List<Bill> getSurgeryBills() {
        return surgeryBills;
    }

    public void setSurgeryBills(List<Bill> surgeryBills) {
        this.surgeryBills = surgeryBills;
    }

    public Bill getSurgeryBill() {
        return surgeryBill;
    }

    public void setSurgeryBill(Bill surgeryBill) {
        this.surgeryBill = surgeryBill;
    }

    public double getPaidByPatient() {
        return paidByPatient;
    }

    public void setPaidByPatient(double paidByPatient) {
        this.paidByPatient = paidByPatient;
    }

    public double getPaidByCompany() {
        return paidByCompany;
    }

    public void setPaidByCompany(double paidByCompany) {
        this.paidByCompany = paidByCompany;
    }

    public String getDuration() {
        calculateDuration();
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public boolean isPatientEncounterHasProvisionalBill() {
        return patientEncounterHasProvisionalBill;
    }

    public void setPatientEncounterHasProvisionalBill(boolean patientEncounterHasProvisionalBill) {
        this.patientEncounterHasProvisionalBill = patientEncounterHasProvisionalBill;
    }

    public List<PatientEncounter> getChildPatientEncouters() {
        return childPatientEncouters;
    }

    public void setChildPatientEncouters(List<PatientEncounter> childPatientEncouters) {
        this.childPatientEncouters = childPatientEncouters;
    }

}
