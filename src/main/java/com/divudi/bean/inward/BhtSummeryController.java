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
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.common.WebUserController;
import com.divudi.bean.membership.MembershipSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.ChargeItemTotal;
import com.divudi.data.dataStructure.DepartmentBillItems;
import com.divudi.data.dataStructure.InwardBillItem;
import com.divudi.data.inward.AdmissionTypeEnum;
import com.divudi.data.inward.InwardChargeType;
import static com.divudi.data.inward.InwardChargeType.AdministrationCharge;
import static com.divudi.data.inward.InwardChargeType.AdmissionFee;
import static com.divudi.data.inward.InwardChargeType.DoctorAndNurses;
import static com.divudi.data.inward.InwardChargeType.LinenCharges;
import static com.divudi.data.inward.InwardChargeType.MOCharges;
import static com.divudi.data.inward.InwardChargeType.MaintainCharges;
import static com.divudi.data.inward.InwardChargeType.MedicalCareICU;
import static com.divudi.data.inward.InwardChargeType.Medicine;
import static com.divudi.data.inward.InwardChargeType.NursingCharges;
import static com.divudi.data.inward.InwardChargeType.ProfessionalCharge;
import static com.divudi.data.inward.InwardChargeType.RoomCharges;
import com.divudi.ejb.BillNumberGenerator;

import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Item;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.PatientItem;
import com.divudi.entity.PreBill;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.RefundBill;
import com.divudi.entity.inward.Admission;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.entity.inward.GuardianRoom;
import com.divudi.entity.inward.PatientRoom;
import com.divudi.entity.inward.TimedItem;
import com.divudi.entity.inward.TimedItemFee;
import com.divudi.entity.membership.InwardMemberShipDiscount;
import com.divudi.entity.membership.MembershipScheme;
import com.divudi.facade.AdmissionTypeFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientItemFacade;
import com.divudi.facade.PatientRoomFacade;
import com.divudi.facade.ServiceFacade;
import com.divudi.facade.TimedItemFeeFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;

import java.io.Serializable;
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

    private CommonFunctions commonFunctions;
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
    ////////////////////////////

    @EJB
    private BillNumberGenerator billNumberBean;
    @Inject
    WebUserController webUserController;
    @Inject
    BhtEditController bhtEditController;
    @Inject
    PriceMatrixController priceMatrixController;
    @Inject
    CommonController commonController;
    @Inject
    InpatientClinicalDataController inpatientClinicalDataController;
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
    private PatientItem tmpPI;
    private PatientEncounter patientEncounter;
    private Bill current;
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
            surgeryBill=surgeryBills.get(0);
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
            doctorAndNurseFee = getInwardBean().createDoctorAndNurseFee(getPatientEncounter());
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

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter());
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

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter());
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

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter());
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

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter());
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

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter());
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

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter());
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

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter());
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
                    chargeItemTotal.setAdjustedTotal(getInwardBean().getProfessionalCharge(getPatientEncounter()));
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
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
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
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
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
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
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
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
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
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
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
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
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
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
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
        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatientEncounter().getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());
        PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(), membershipScheme, getPatientEncounter().getCreditCompany(), cit.getInwardChargeType(), getPatientEncounter().getAdmissionType());
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

        double serviceValue = getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
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

        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatientEncounter().getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());

        PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                membershipScheme,
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
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter());
        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatientEncounter().getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(
                    getPatientEncounter().getPaymentMethod(), membershipScheme, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());

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
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter());
        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatientEncounter().getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        PriceMatrix pm;
        for (PatientRoom bf : list) {
            pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    membershipScheme, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());

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
        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatientEncounter().getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());
        PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                membershipScheme, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType());
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
        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatientEncounter().getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter());
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    membershipScheme, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());

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
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter());
        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatientEncounter().getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    membershipScheme, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());

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
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter());
        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatientEncounter().getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    membershipScheme, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());
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
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter());
        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatientEncounter().getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    membershipScheme, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());
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
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter());
        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatientEncounter().getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    membershipScheme, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());

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
        //   ////System.out.println("patientRoom = " + patientRoom);
        if (patientRoom == null) {
            return;
        }
        if (patientRoom.getId() != null) {
            getPatientRoomFacade().edit(patientRoom);
        } else {
            getPatientRoomFacade().create(patientRoom);
        }

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
        for (Bill b : getPaymentBill()) {
            getBillBean().updateInwardDipositList(getCurrent().getPatientEncounter(), b);
        }

    }

    public void settle() {
        if (errorCheck()) {
            return;
        }

        saveBill();
        saveBillItem();

        if (getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
            getInwardBean().updateCreditDetail(getPatientEncounter(), getCurrent().getNetTotal());
        }

        getPatientEncounter().setFinalBill(getCurrent());
        getPatientEncounter().setGrantTotal(getCurrent().getGrantTotal());
        getPatientEncounter().setDiscount(getCurrent().getDiscount());
        getPatientEncounter().setNetTotal(getCurrent().getNetTotal());
        getPatientEncounter().setPaymentFinalized(true);
        getPatientEncounterFacade().edit(getPatientEncounter());
        getBillFacade().edit(getCurrent());

        updatePaymentBillList();
        JsfUtil.addSuccessMessage("Bill Saved");

        printPreview = true;
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
        
        if(getPatientEncounter().isDischarged() == false){
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
        
        if (date == null){
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
            getPatientEncounter().getCurrentPatientRoom().setDischargedAt(getPatientEncounter().getDateOfDischarge());
            roomChangeController.discharge(getPatientEncounter().getCurrentPatientRoom());
        }

        if (getPatientEncounter().getCurrentPatientRoom() != null && getPatientEncounter().getCurrentPatientRoom().getDischargedAt() == null) {
            getPatientEncounter().getCurrentPatientRoom().setDischargedAt(getPatientEncounter().getDateOfDischarge());
            getPatientRoomFacade().edit(getPatientEncounter().getCurrentPatientRoom());
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
            JsfUtil.addErrorMessage("Please Adjust category amount correctly");
            return true;
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

        commonController.printReportDetails(fromDate, toDate, startTime, "(Billing/Interim Bill/Inprint(/faces/inward/inward_bill_intrim.xhtml)");

        return "inward_bill_intrim_print";
    }

    public boolean checkBill() {
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

        createTables();
        calculateDiscount();
        createPatientRooms();
        updateTotal();

        commonController.printReportDetails(fromDate, toDate, startTime, "(Billing/Interim Bill/Tosettle(/faces/inward/inward_bill_intrim.xhtml)");

        return "inward_bill_final";

    }

    private void saveBill() {

        getCurrent().setGrantTotal(grantTotal);
        getCurrent().setTotal(grantTotal);
        getCurrent().setDiscount(discount);
        getCurrent().setNetTotal(grantTotal - discount);
        getCurrent().setPaidAmount(paid);
        getCurrent().setClaimableTotal(adjustedTotal);
        getCurrent().setInstitution(getSessionController().getInstitution());

        getCurrent().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.InwardFinalBill, BillClassType.BilledBill, BillNumberSuffix.INWFINAL));
        getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.InwardFinalBill, BillClassType.BilledBill, BillNumberSuffix.INWFINAL));

        getCurrent().setBillType(BillType.InwardFinalBill);
        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());
        getCurrent().setPatientEncounter(patientEncounter);
        getCurrent().setPatient(patientEncounter.getPatient());
        getCurrent().setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(patientEncounter.getPatient(), getSessionController().getApplicationPreference().isMembershipExpires()));
        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        } else {
            getBillFacade().edit(getCurrent());
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
                temProfFee += cit.getTotal();
            } else {
                temHosFee += cit.getTotal();
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

    private void updateProBillFee(BillItem bItem) {
        for (BillFee bf : getProfesionallFee()) {
            bf.setReferenceBillItem(bItem);
            getBillFeeFacade().edit(bf);

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

    public void createTables() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;
        makeNull();

        if (patientEncounter == null) {
            return;
        }

        createPatientRooms();
        createPatientItems();
        pharmacyIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.PharmacyBhtPre);
        storeIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.StoreBhtPre);
        departmentBillItems = getInwardBean().createDepartmentBillItems(patientEncounter, null);
        additionalChargeBill = getInwardBean().fetchOutSideBill(getPatientEncounter());
        getInwardBean().setProfesionallFeeAdjusted(getPatientEncounter());
        profesionallFee = getInwardBean().createProfesionallFee(getPatientEncounter());
        doctorAndNurseFee = getInwardBean().createDoctorAndNurseFee(getPatientEncounter());
        paymentBill = getInwardBean().fetchPaymentBill(getPatientEncounter());

        updateRoomChargeList();
        createChargeItemTotals();

        updateTotal();
        JsfUtil.addSuccessMessage("Recalculated Successfully");

        if (patientEncounter != null && patientEncounter.getDateOfDischarge() != null) {
            date = patientEncounter.getDateOfDischarge();
        } else {
            date = null;
        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Billing/Interim Bill(/faces/inward/inward_bill_intrim.xhtml)");
    }

    public void createTablesWithEstimatedProfessionalFees() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        makeNull();

        if (patientEncounter == null) {
            return;
        }

        createPatientRooms();
        createPatientItems();
        pharmacyIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.PharmacyBhtPre);
        storeIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.StoreBhtPre);
        departmentBillItems = getInwardBean().createDepartmentBillItems(patientEncounter, null);
        additionalChargeBill = getInwardBean().fetchOutSideBill(getPatientEncounter());
        getInwardBean().setProfesionallFeeAdjusted(getPatientEncounter());
        profesionallFee = getInwardBean().createProfesionallFeeEstimated(getPatientEncounter());
        doctorAndNurseFee = getInwardBean().createDoctorAndNurseFee(getPatientEncounter());
        paymentBill = getInwardBean().fetchPaymentBill(getPatientEncounter());

        updateRoomChargeList();
        createChargeItemTotals();

        updateTotal();

        if (patientEncounter != null && patientEncounter.getDateOfDischarge() != null) {
            date = patientEncounter.getDateOfDischarge();
        } else {
            date = null;
        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Billing/Estimated Professional Fees/Refresh(/faces/inward/inward_bill_intrim.xhtml)");

    }

    private List<PatientItem> createPatientItems() {
        patientItems = getInwardBean().fetchPatientItem(getPatientEncounter());

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

    public String navigateToIntrimBill() {
        patientEncounter = null;
        makeNull();
        return "/inward/inward_bill_intrim";
    }

    public String navigateToIntrimBillFromPatientProfile() {
        createTables();
        return "/inward/inward_bill_intrim";
    }

    public String toIntrimBillclear() {
        patientEncounter = null;
        makeNull();
        return "/inward/inward_bill_intrim";
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

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter());

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

        if (!sessionController.getLoggedPreference().isInwardMoChargeCalculateInitialTime()) {
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
            ////System.out.println("value * getInwardBean().calCountWithoutOverShoot(timedFee, patientRoom.getAdmittedAt(), dischargeAt) = " + value * getInwardBean().calCountWithoutOverShoot(timedFee, patientRoom.getAdmittedAt(), dischargeAt));
            return value * getInwardBean().calCountWithoutOverShoot(timedFee, patientRoom.getAdmittedAt(), dischargeAt);
        } else {
            ////System.out.println("value * getInwardBean().calCount(timedFee, patientRoom.getAdmittedAt(), dischargeAt) = " + value * getInwardBean().calCount(timedFee, patientRoom.getAdmittedAt(), dischargeAt));
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
            profesionallFee = getInwardBean().createProfesionallFee(getPatientEncounter());
        }
        return profesionallFee;
    }

    public void setProfesionallFee(List<BillFee> profesionallFee) {
        this.profesionallFee = profesionallFee;
    }

    public List<Bill> getPaymentBill() {
        if (paymentBill == null) {
            paymentBill = getInwardBean().fetchPaymentBill(getPatientEncounter());
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
            double adj = getInwardBean().caltValueFromAdditionalCharge(cit.getInwardChargeType(), getPatientEncounter());
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

        paid = getInwardBean().getPaidValue(getPatientEncounter());

        due = (grantTotal - discount) - paid;

        if (getPatientEncounter().getCreditLimit() != 0) {
            due -= getPatientEncounter().getCreditLimit();
        }

        changed = false;

    }

    public void changeIsMade() {
        changed = true;
    }

    public List<ChargeItemTotal> getChargeItemTotals() {
        if (chargeItemTotals == null) {
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
                        i.setTotal(getPatientEncounter().getAdmissionType().getAdmissionFee());
                    }
                    break;
                case RoomCharges:
                    i.setTotal(getInwardBean().getRoomCharge(getPatientEncounter()));
                    break;
                case MOCharges:
                    i.setTotal(getInwardBean().getMoCharge(getPatientEncounter()));
                    break;
                case NursingCharges:
                    i.setTotal(getInwardBean().getNursingCharge(getPatientEncounter()));
                    break;
                case MaintainCharges:
                    i.setTotal(getInwardBean().getMaintainCharge(getPatientEncounter()));
                    break;
                case MedicalCareICU:
                    i.setTotal(getInwardBean().getMedicalCareIcuCharge(getPatientEncounter()));
                    break;
                case AdministrationCharge:
                    i.setTotal(getInwardBean().getAdminCharge(getPatientEncounter()));
                    break;
                case LinenCharges:
                    i.setTotal(getInwardBean().getLinenCharge(getPatientEncounter()));
                    break;
                case Medicine:
                    i.setTotal(getInwardBean().calCostOfIssue(getPatientEncounter(), BillType.PharmacyBhtPre));
                    break;
                case GeneralIssuing:
                    i.setTotal(getInwardBean().calCostOfIssue(getPatientEncounter(), BillType.StoreBhtPre));
                    break;
                case ProfessionalCharge:
                    i.setTotal(getInwardBean().calculateProfessionalCharges(getPatientEncounter()));
                    break;
                case DoctorAndNurses:
                    i.setTotal(getInwardBean().calculateDoctorAndNurseCharges(getPatientEncounter()));
                    break;
            }
        }
    }

    private void setServiceTotCategoryWise() {
        for (ChargeItemTotal ch : chargeItemTotals) {
            ch.setTotal(ch.getTotal() + getInwardBean().calServiceBillItemsTotalByInwardChargeType(ch.getInwardChargeType(), getPatientEncounter()));
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

    private void setTimedServiceTotCategoryWise() {

        for (ChargeItemTotal ch : chargeItemTotals) {
            ch.setTotal(ch.getTotal() + getInwardBean().getTimedItemFeeTotalByInwardChargeType(ch.getInwardChargeType(), getPatientEncounter()));
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
            additionalChargeBill = getInwardBean().fetchOutSideBill(getPatientEncounter());
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
            departmentBillItems = getInwardBean().createDepartmentBillItems(patientEncounter, null);
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

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
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

}
