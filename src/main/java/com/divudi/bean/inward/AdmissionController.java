/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.AppointmentController;
import com.divudi.bean.common.ClinicalFindingValueController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ControllerWithPatient;
import com.divudi.bean.common.SessionController;

import com.divudi.core.data.ApplicationInstitution;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.data.inward.AdmissionTypeEnum;

import com.divudi.core.entity.Appointment;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.EncounterCreditCompany;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.facade.AdmissionFacade;
import com.divudi.core.facade.AppointmentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.EncounterCreditCompanyFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.RoomFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.pharmacy.PharmacyRequestForBhtController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.clinical.ClinicalFindingValueType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.clinical.ClinicalFindingValue;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.Reservation;
import com.divudi.core.facade.ClinicalFindingValueFacade;
import com.divudi.core.util.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class AdmissionController implements Serializable, ControllerWithPatient {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    RoomOccupancyController roomOccupancyController;
    @Inject
    private InwardStaffPaymentBillController inwardStaffPaymentBillController;
    @Inject
    RoomChangeController roomChangeController;
    @Inject
    InpatientClinicalDataController inpatientClinicalDataController;
    @Inject
    PharmacyRequestForBhtController pharmacyRequestForBhtController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    ////////////
    @EJB
    private AdmissionFacade ejbFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    private RoomFacade roomFacade;
    @EJB
    private EncounterCreditCompanyFacade encounterCreditCompanyFacade;
    @EJB
    ClinicalFindingValueFacade clinicalFindingValueFacade;

    @Inject
    BhtEditController bhtEditController;
    @Inject
    BhtSummeryController bhtSummeryController;
    @Inject
    ClinicalFindingValueController clinicalFindingValueController;
    @Inject
    AppointmentController appointmentController;

    ////////////////////////////
    ///////////////////////
    List<Admission> selectedItems;
    private Admission current;
    private Admission currentNonBht;
    private List<EncounterCreditCompany> encounterCreditCompanies;
    private EncounterCreditCompany encounterCreditCompany;
    private Admission parentAdmission;
    private PatientRoom patientRoom;
    private List<Admission> items = null;
    private List<Patient> patientList;
    private boolean printPreview;
    private List<Admission> currentAdmissions;
    ///////////////////////////
    String selectText = "";
    private String ageText = "";
    private String bhtText = "";
    private String patientTabId = "tabNewPt";
    private int patientSearchTab;
    private Patient patient;
    private YearMonthDay yearMonthDay;
    private Bill appointmentBill;
    private PaymentMethodData paymentMethodData;
    private Date fromDate;
    private Date toDate;
    private String patientNameForSearch;
    private String patientIdentityNumberForSearch;
    private String patientPhoneNumberForSearch;
    private String patientCodeForSearch;
    private String patientNumberForSearch;
    private String bhtNumberForSearch;
    private Doctor referringDoctorForSearch;
    private Institution institutionForSearch;
    private AdmissionStatus admissionStatusForSearch;
    private AdmissionType admissionTypeForSearch;
    private Admission perantAddmission;
    private boolean patientDetailsEditable;
    private List<ClinicalFindingValue> patientAllergies;
    private ClinicalFindingValue currentPatientAllergy;
    private Institution lastCreditCompany;
    private Department loggedDepartment;
    private Institution site;

    private PaymentMethod paymentMethod;
    private boolean admittingProcessStarted;
    private Reservation latestfoundReservation;

    public void addPatientAllergy() {
        if (currentPatientAllergy == null) {
            return;
        }
        patientAllergies.add(currentPatientAllergy);
        currentPatientAllergy = null;
    }

    public void removePatientAllergy(ClinicalFindingValue pa) {
        if (currentPatientAllergy == null) {
            return;
        }
        pa.setRetired(true);
        clinicalFindingValueFacade.edit(pa);
        patientAllergies.remove(pa);
    }

    public void savePatientAllergies() {
        if (patientAllergies == null) {
            return;
        }
        for (ClinicalFindingValue al : patientAllergies) {
            if (al.getPatient() == null) {
                al.setPatient(getCurrent().getPatient());
            }
            if (al.getId() == null) {
                clinicalFindingValueFacade.create(al);
            } else {
                clinicalFindingValueFacade.edit(al);
            }
        }
    }

    public void fillCurrentPatientAllergies(Patient pt) {
        if (pt == null) {
            return;
        }
        patientAllergies = new ArrayList<>();
        Map params = new HashMap<>();
        String s = "SELECT c FROM ClinicalFindingValue c WHERE c.retired = false AND c.patient = :pt";
        params.put("pt", pt);
        patientAllergies = clinicalFindingValueFacade.findByJpql(s, params);
    }

    public void copyPatientAddressToGurdian() {
        current.getGuardian().setAddress(current.getPatient().getPerson().getAddress());
    }

    public void copyPatientPhoneNumberToGurdian() {
        current.getGuardian().setMobile(current.getPatient().getPerson().getMobile());
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public void resetCreditDetail(PatientEncounter patientEncounter) {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("Nuull");
            return;
        }

        patientEncounter.setCreditCompany(null);
        patientEncounter.setCreditLimit(0);
        patientEncounter.setCreditPaidAmount(0);
        patientEncounter.setCreditUsedAmount(0);
        getPatientEncounterFacade().edit(patientEncounter);
    }

    public String navigateToInpatientClinicalData() {
        inpatientClinicalDataController.setCurrent(current);
        return inpatientClinicalDataController.navigateToEncounterClinicalData();
    }

    public void addCreditCompnay() {
        if (encounterCreditCompany.getCreditLimit() <= 0) {
            JsfUtil.addErrorMessage("Credit limit must be greater than zero");
            return;
        }
        // need to add encounterCreditCompany to list
        if (encounterCreditCompany.getInstitution() != null) {
            encounterCreditCompany.setPatientEncounter(current);
            encounterCreditCompanies.add(encounterCreditCompany);
            current.setCreditLimit(current.getCreditLimit() + encounterCreditCompany.getCreditLimit());
            encounterCreditCompany = new EncounterCreditCompany();
        }

    }

    public void removeCreditCompany(EncounterCreditCompany encounterCreditCompany) {
        if (encounterCreditCompany != null) {
            encounterCreditCompanies.remove(encounterCreditCompany);
            encounterCreditCompany = new EncounterCreditCompany();
        }
    }

    public String navigateToInpatientDrugChart() {
        inpatientClinicalDataController.setCurrent(current);
        return inpatientClinicalDataController.navigateToDrugChart();
    }

    public String navigateToInpatientInvestigations() {
        inpatientClinicalDataController.setCurrent(current);
        return inpatientClinicalDataController.navigateToInvestigations();
    }

    public String navigateToInpatientImages() {
        inpatientClinicalDataController.setCurrent(current);
        return inpatientClinicalDataController.navigateToImages();
    }

    public String navigateToInpatientDiagnosisCard() {
        inpatientClinicalDataController.setCurrent(current);
        return inpatientClinicalDataController.navigateToEncounterClinicalData();
    }

    public void dateChangeListen() {
        getPatient().getPerson().setDob(CommonFunctions.guessDob(yearMonthDay));

    }
    
    public boolean onRoomFacilityChargeSelected(){
        List<Reservation> foundReservations = appointmentController.checkAppointmentsForRoom(patientRoom.getRoomFacilityCharge());
        System.out.println("foundReservations = " + foundReservations.size());
        if(foundReservations == null || foundReservations.isEmpty()){
            latestfoundReservation = new Reservation();
            return false;
        }else{
            latestfoundReservation = foundReservations.get(0);
            String warnMessage = "You have an upcoming reservation for this room from " + CommonFunctions.getDateFormat(latestfoundReservation.getReservedFrom(), sessionController.getApplicationPreference().getLongDateFormat()) + " to " + CommonFunctions.getDateFormat(latestfoundReservation.getReservedTo(), sessionController.getApplicationPreference().getLongDateFormat());
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", warnMessage));
            return true;
        }
    }

    public void admissionPaymentMethodChange() {
        if (current.getPaymentMethod() == PaymentMethod.Credit) {
            isPatientHaveALastUsedCreditCompany(current.getPatient());
        }
    }

    /**
     * Checks if the given patient has a previous non-retired admission with a credit payment method and sets the last used credit company.
     *
     * @param p the patient to check for previous credit admissions
     * @return true if a previous credit admission exists and the last credit company is set; false otherwise
     */
    public boolean isPatientHaveALastUsedCreditCompany(Patient p) {
        if (p == null) {
            return false;
        }

        Admission a = null;
        lastCreditCompany = null;
        String sql;
        HashMap hash = new HashMap();
        sql = "select c from Admission c "
                + " where c.patient=:pt "
                + " and c.paymentMethod= :pm"
                + " and c.retired=false "
                + " order by c.id desc";

        hash.put("pm", PaymentMethod.Credit);
        hash.put("pt", p);
        a = getFacade().findFirstByJpql(sql, hash);
        if (a == null) {
            return false;
        } else {
            lastCreditCompany = a.getCreditCompany();
            return true;
        }
    }

    /**
     * Retrieves the most recent credit-based admission for the current patient and populates the current encounter's credit companies list with associated institutions and credit limits.
     *
     * If the current patient has a previous admission with credit payment, copies each linked, non-retired credit company to the current encounter and accumulates their credit limits.
     */
    public void findLastUsedCreditCompanies() {
        if(configOptionApplicationController.getBooleanValueByKey("Inward Admission - Find And Fill Last Used Credit Companies of a Patient",false)) {
            if (current.getPatient() == null) {
                return;
            }

            Admission a = null;
            String sql;
            HashMap hash = new HashMap();
            sql = "select c from Admission c "
                    + " where c.patient=:pt "
                    + " and c.paymentMethod= :pm"
                    + " and c.retired=false "
                    + " order by c.id desc";

            hash.put("pm", PaymentMethod.Credit);
            hash.put("pt", current.getPatient());
            a = getFacade().findFirstByJpql(sql, hash);

            if (a == null) {
                return;
            } else {
                List<EncounterCreditCompany> encounterCreditCompanys = new ArrayList<>();
                String jpql = "select ecc from EncounterCreditCompany ecc"
                        + "  where ecc.retired=false "
                        + " and ecc.patientEncounter=:pEnc ";
                HashMap hm = new HashMap();
                hm.put("pEnc", a);
                encounterCreditCompanys = encounterCreditCompanyFacade.findByJpql(jpql, hm);

                encounterCreditCompanies = new ArrayList<>();

                for (EncounterCreditCompany ecc : encounterCreditCompanys) {
                    encounterCreditCompany = new EncounterCreditCompany();
                    encounterCreditCompany.setPatientEncounter(current);
                    encounterCreditCompany.setInstitution(ecc.getInstitution());
                    encounterCreditCompany.setCreditLimit(ecc.getCreditLimit());
                    encounterCreditCompany.setPolicyNo(ecc.getPolicyNo());
                    current.setCreditLimit(current.getCreditLimit() + encounterCreditCompany.getCreditLimit());
                    encounterCreditCompanies.add(encounterCreditCompany);
                    encounterCreditCompany = new EncounterCreditCompany();
                }
            }
        }

    }

    /**
     * Returns a list of discharged credit admissions with outstanding credit balances matching the given query.
     *
     * The query matches against BHT number, patient name, or credit company name. Only admissions with a positive credit balance, payment method set to credit, and not retired are included.
     *
     * @param qry the search string to match against BHT number, patient name, or credit company name
     * @return a list of matching admissions with outstanding credit balances
     */
    public List<Admission> completeBhtCredit(String qry) {
        List<Admission> a = null;
        String sql;
        HashMap hash = new HashMap();
        if (qry != null) {
            sql = "select c from Admission c "
                    + " where abs(c.creditUsedAmount)-abs(c.creditPaidAmount) >:val  "
                    + " and c.paymentMethod= :pm "
                    + " and c.discharged=true "
                    + " and c.retired=false "
                    + " and ((c.bhtNo) like :q"
                    + " or (c.patient.person.name) like :q "
                    + " or (c.creditCompany.name) like :q ) "
                    + " order by c.creditCompany.name";

            hash.put("pm", PaymentMethod.Credit);
            hash.put("val", 0.1);
            hash.put("q", "%" + qry.toUpperCase() + "%");
            a = getFacade().findByJpql(sql, hash, 20);
        }
        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }

    public List<Admission> getCreditBillsBht(Institution institution) {
        String sql;
        HashMap hash = new HashMap();

        sql = "select c from PatientEncounter c "
                + " where c.retired=false "
                + " and abs(c.creditUsedAmount)-abs(c.creditPaidAmount) >:val "
                + " and c.discharged=true "
                + " and c.paymentMethod=:pm  "
                + " and c.creditCompany=:ins ";

        hash.put("pm", PaymentMethod.Credit);
        hash.put("val", 0.1);
        hash.put("ins", institution);
        //     hash.put("pm", PaymentMethod.Credit);
        List<Admission> lst = getFacade().findByJpql(sql, hash);

        return lst;
    }

    public List<Bill> getCreditPaymentBillsBht(Institution institution) {
        String sql;
        HashMap hash = new HashMap();

        sql = "select b from Bill b "
                + " where b.retired=false "
                + " and b.creditCompany=:ins"
                + " and b.billTypeAtomic=:bta ";

        hash.put("ins", institution);
        hash.put("bta", BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
        //     hash.put("pm", PaymentMethod.Credit);
        List<Bill> lst = getBillFacade().findByJpql(sql, hash);
        System.out.println("lst = " + lst);
        return lst;
    }

//    public List<Admission> completePatientPaymentDue(String qry) {
//        String sql = "Select b.patientEncounter From "
//                + " BilledBill b where"
//                + " b.retired=false "
//                + " and b.cancelled=false "
//                + " and b.billType=:btp "
//                + " and (abs(b.netTotal)-abs(b.paidAmount)) > :val "
//                + " and ((b.patientEncounter.bhtNo) like :q or"
//                + " (b.patientEncounter.patient.person.name) like :q ) "
//                + " order by b.patientEncounter.bhtNo";
//        HashMap hm = new HashMap();
//        hm.put("btp", BillType.InwardFinalBill);
//        hm.put("val", 0.1);
//        hm.put("q", "%" + qry.toUpperCase() + "%");
//
//        List<Admission> b = getEjbFacade().findByJpql(sql, hm, 20);
//
//        if (b == null) {
//            return new ArrayList<>();
//        }
//
//        return b;
//
//    }
//    public List<Admission> completePatientPaymentMax(String qry) {
//        String sql = "Select b.patientEncounter From "
//                + " BilledBill b where"
//                + " b.retired=false "
//                + " and b.cancelled=false "
//                + " and b.billType=:btp"
//                + " and (abs(b.paidAmount)- abs(b.netTotal)) > :val "
//                + " and ((b.patientEncounter.bhtNo) like :q or"
//                + " (b.patientEncounter.patient.person.name) like :q ) "
//                + " order by b.patientEncounter.bhtNo";
//        HashMap hm = new HashMap();
//        hm.put("btp", BillType.InwardFinalBill);
//        hm.put("val", 0.1);
//        hm.put("q", "%" + qry.toUpperCase() + "%");
//
//        List<Admission> b = getEjbFacade().findByJpql(sql, hm, 20);
//
//        if (b == null) {
//            return new ArrayList<>();
//        }
//
//        return b;
//
//    }
    public List<Admission> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from Admission c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void onTabChange(TabChangeEvent event) {
        setPatientTabId(event.getTab().getId());

    }

    public String navigateToEditAdmission() {
        if (current == null) {
            current = new Admission();
        }
        bhtEditController.setCurrent(current);
        bhtEditController.getCurrent().getPatient().setEditingMode(true);
        return bhtEditController.navigateToEditAdmissionDetails();
    }

    public String navigateToRoomOccupancy() {
        roomOccupancyController.setPatientRooms(null);
        return "/inward/inward_room_occupancy?faces-redirect=true";
    }

    public String navigateToRoomVacancy() {
        roomOccupancyController.setRoomFacilityCharges(null);
        return "/inward/inward_room_vacant?faces-redirect=true";
    }

    public String navigateToRoomChange() {
//        roomChangeController.recreate();
        roomChangeController.createPatientRoom();
        return "/inward/inward_room_change?faces-redirect=true";
    }

    public String navigateToGuardianRoomChange() {
//         roomChangeController.recreate();
        roomChangeController.createGuardianRoom();
        return "/inward/inward_room_change_guardian?faces-redirect=true";
    }

    public String navigateToAddBabyAdmission() {
        parentAdmission = current;
        Admission ad = new Admission();
        if (ad.getDateOfAdmission() == null) {
            ad.setDateOfAdmission(CommonFunctions.getCurrentDateTime());
        }
        setCurrent(ad);
        current.setParentEncounter(parentAdmission);
        setPrintPreview(false);
        return "/inward/inward_admission_child?faces-redirect=true";
    }

//    // Services & Items Submenu Methods
//    public String navigateToAddServices() {
//        return "/inward/inward_bill_service?faces-redirect=true";
//    }
//    public String navigateToAddOutsideCharge() {
//        return "/inward/inward_bill_outside_charge?faces-redirect=true";
//    }
//    public String navigateToAddProfessionalFee() {
//        return "/inward/inward_bill_professional?faces-redirect=true";
//    }
    public String navigateToAddEstimatedProfessionalFee() {
        return "/inward/inward_bill_professional_estimate?faces-redirect=true";
    }

    public String navigateToPharmacyBhtRequest() {
        pharmacyRequestForBhtController.resetAll();
        pharmacyRequestForBhtController.setPatientEncounter(current);
        return "/ward/ward_pharmacy_bht_issue_request_bill?faces-redirect=true";
    }

    public String navigateToPharmacyBhtRequestFromMenu() {
        pharmacyRequestForBhtController.resetAll();
        return "/ward/ward_pharmacy_bht_issue_request_bill?faces-redirect=true";
    }

    public String navigateToSearchInwardBills() {
        return "/ward/ward_pharmacy_bht_issue_request_bill_search?faces-redirect=true";
    }

    public String navigateToSearchAdmissions() {
        bhtSummeryController.setPatientEncounterHasProvisionalBill(false);
        return "/inward/inpatient_search?faces-redirect=true";
    }

    public List<Admission> completePatient(String query) {
        List<Admission> suggestions;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Admission c "
                + " where c.retired=false "
                + " and c.discharged=false "
                + " and ((c.bhtNo) like :q "
                + " or (c.patient.person.name) like :q "
                + " or (c.patient.code) like :q "
                + " or (c.patient.phn) like :q) "
                + " order by c.bhtNo ";

        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getFacade().findByJpql(sql, hm, 20);

        return suggestions;
    }

    public void searchAdmissions() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select date");
            return;
        }

        String j;
        HashMap m = new HashMap();
        j = "select c from Admission c "
                + " where c.retired=:ret ";
        m.put("ret", false);
        j += " and c.dateOfAdmission between :fd and :td ";
        m.put("fd", fromDate);
        m.put("td", toDate);

        if (patientNameForSearch != null && !patientNameForSearch.trim().equals("")) {
            j += " and c.patient.person.name like :name ";
            m.put("name", "%" + patientNameForSearch + "%");
        }
        if (bhtNumberForSearch != null && !bhtNumberForSearch.trim().equals("")) {
            j += "  and c.bhtNo like :bht ";
            m.put("bht", "%" + bhtNumberForSearch + "%");
        }

        if (patientNumberForSearch != null && !patientNumberForSearch.trim().equals("")) {
            j += " and (c.patient.code =:phn or c.patient.phn =:phn)";
            m.put("phn", patientNumberForSearch);
        }

        if (patientPhoneNumberForSearch != null && !patientPhoneNumberForSearch.trim().equals("")) {
            j += " and (c.patient.person.phone =:phone or c.patient.person.mobile =:phone)";
            m.put("phone", patientPhoneNumberForSearch);
        }

        if (patientIdentityNumberForSearch != null && !patientIdentityNumberForSearch.trim().equals("")) {
            j += " and c.patient.person.nic =:nic";
            m.put("nic", patientIdentityNumberForSearch);
        }

        if (admissionStatusForSearch != null) {
            if (null != admissionStatusForSearch) {
                switch (admissionStatusForSearch) {
                    case ADMITTED_BUT_NOT_DISCHARGED:
                        j += "  and c.discharged=:dis ";
                        m.put("dis", false);
                        break;
                    case DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED:
                        j += "  and c.discharged=:dis and c.paymentFinalized=:bf ";
                        m.put("dis", true);
                        m.put("bf", false);
                        break;
                    case DISCHARGED_AND_FINAL_BILL_COMPLETED:
                        j += "  and c.discharged=:dis and c.paymentFinalized=:bf ";
                        m.put("dis", true);
                        m.put("bf", true);
                        break;
                    case ANY_STATUS:
                        break;
                    default:
                        break;
                }
            }
        }
        if (institutionForSearch != null) {
            j += "  and c.institution=:ins ";
            m.put("ins", institutionForSearch);
        }

        if (loggedDepartment != null) {
            j += "  and c.department=:dept ";
            m.put("dept", loggedDepartment);
        }

        if (parentAdmission != null) {
            j += "  and c.parentEncounter=:pent ";
            m.put("pent", parentAdmission);
        }

        if (admissionTypeForSearch != null) {
            j += "  and c.admissionType=:at ";
            m.put("at", admissionTypeForSearch);
        }

        items = getFacade().findByJpql(j, m, TemporalType.TIMESTAMP);
    }

    public void searchAdmissionsWithoutRoom() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select date");
            return;
        }

        String j;
        HashMap m = new HashMap();
        j = "select c from Admission c "
                + " where c.retired=:ret "
                + " AND c.currentPatientRoom IS NULL ";
        m.put("ret", false);
        j += " and c.dateOfAdmission between :fd and :td ";
        m.put("fd", fromDate);
        m.put("td", toDate);

        if (patientNameForSearch != null && !patientNameForSearch.trim().equals("")) {
            j += " and c.patient.person.name like :name ";
            m.put("name", "%" + patientNameForSearch + "%");
        }
        if (bhtNumberForSearch != null && !bhtNumberForSearch.trim().equals("")) {
            j += "  and c.bhtNo like :bht ";
            m.put("bht", "%" + bhtNumberForSearch + "%");
        }

        if (patientNumberForSearch != null && !patientNumberForSearch.trim().equals("")) {
            j += " and (c.patient.code =:phn or c.patient.phn =:phn)";
            m.put("phn", patientNumberForSearch);
        }

        if (patientPhoneNumberForSearch != null && !patientPhoneNumberForSearch.trim().equals("")) {
            j += " and (c.patient.person.phone =:phone or c.patient.person.mobile =:phone)";
            m.put("phone", patientPhoneNumberForSearch);
        }

        if (patientIdentityNumberForSearch != null && !patientIdentityNumberForSearch.trim().equals("")) {
            j += " and c.patient.person.nic =:nic";
            m.put("nic", patientIdentityNumberForSearch);
        }

        if (admissionStatusForSearch != null) {
            if (null != admissionStatusForSearch) {
                switch (admissionStatusForSearch) {
                    case ADMITTED_BUT_NOT_DISCHARGED:
                        j += "  and c.discharged=:dis ";
                        m.put("dis", false);
                        break;
                    case DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED:
                        j += "  and c.discharged=:dis and c.paymentFinalized=:bf ";
                        m.put("dis", true);
                        m.put("bf", false);
                        break;
                    case DISCHARGED_AND_FINAL_BILL_COMPLETED:
                        j += "  and c.discharged=:dis and c.paymentFinalized=:bf ";
                        m.put("dis", true);
                        m.put("bf", true);
                        break;
                    case ANY_STATUS:
                        break;
                    default:
                        break;
                }
            }
        }
        if (institutionForSearch != null) {
            j += "  and c.institution=:ins ";
            m.put("ins", institutionForSearch);
        }

        if (loggedDepartment != null) {
            j += "  and c.department=:dept ";
            m.put("dept", loggedDepartment);
        }

        if (parentAdmission != null) {
            j += "  and c.parentEncounter=:pent ";
            m.put("pent", parentAdmission);
        }

        items = getFacade().findByJpql(j, m, TemporalType.TIMESTAMP);
    }

    public String navigateToAdmissionProfilePage() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return "";
        }
        if (current.getPatient() == null) {
            JsfUtil.addErrorMessage("No Patient. Program Error");
            return "";
        }
        current.getPatient().setEditingMode(false);
        bhtSummeryController.setPatientEncounter(current);
        bhtSummeryController.setPatientEncounterHasProvisionalBill(isAddmissionHaveProvisionalBill((Admission) current));
        return bhtSummeryController.navigateToInpatientProfile();
    }

    public List<Admission> completeAdmission(String query) {
        List<Admission> suggestions;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Admission c "
                + " where c.retired=false "
                + " and c.discharged=false "
                + " and ((c.bhtNo) like :q "
                + " or (c.patient.person.name) like :q "
                + " or (c.patient.code) like :q) "
                + " order by c.bhtNo ";
        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getFacade().findByJpql(sql, hm, 20);

        return suggestions;
    }

    public List<Admission> findAdmissions(Staff admittingOfficer, Date fromDate, Date toDate) {
        List<Admission> admissions;
        String jpql;
        HashMap params = new HashMap();
        jpql = "select c from Admission c "
                + " where c.retired=:ret "
                + " and c.opdDoctor=:admittingOfficer "
                + " and c.dateOfAdmission between :fromDate and :toDate "
                + " order by c.bhtNo ";
        params.put("admittingOfficer", admittingOfficer);
        params.put("ret", false);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        admissions = getFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return admissions;
    }

    public List<Admission> completePatientAll(String query) {
        List<Admission> suggestions;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Admission c"
                + " where ((c.bhtNo) like :q or"
                + " (c.patient.person.name) like :q ) "
                + " order by c.bhtNo";
        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getFacade().findByJpql(sql, hm, 20);

        return suggestions;
    }

    public List<Admission> completePatientFinaled(String query) {
        List<Admission> suggestions;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Admission c"
                + " where ((c.bhtNo) like :q or"
                + " (c.patient.person.name) like :q ) "
                + " and c.paymentFinalized=true"
                + " order by c.bhtNo";
        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getFacade().findByJpql(sql, hm, 20);

        return suggestions;
    }

//    public List<Admission> completePatientCredit(String query) {
//        List<Admission> suggestions;
//        String sql;
//        HashMap hm = new HashMap();
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//            sql = "select c from Admission c where c.retired=false and c.paymentMethod=:pm  and ((c.bhtNo) like '%" + query.toUpperCase() + "%' or (c.patient.person.name) like '%" + query.toUpperCase() + "%') order by c.bhtNo";
//            hm.put("pm", PaymentMethod.Credit);
//            ////// // System.out.println(sql);
//            suggestions = getFacade().findByJpql(sql, hm, TemporalType.TIME, 20);
//        }
//        return suggestions;
//    }
    public List<Admission> completePatientDishcargedNotFinalized(String query) {
        List<Admission> suggestions;
        String sql;
        HashMap h = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Admission c where c.retired=false and "
                    + " ( c.paymentFinalized is null or c.paymentFinalized=false )"
                    + " and ( ((c.bhtNo) like :q )or ((c.patient.person.name)"
                    + " like :q) ) order by c.bhtNo";
            ////// // System.out.println(sql);
            //      h.put("btp", BillType.InwardPaymentBill);
            h.put("q", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findByJpql(sql, h, 20);
        }
        return suggestions;
    }

    public String navigateToListCurrentInpatients() {
        listCurrentInpatients();
        return "";

    }

    /**
     * Checks if the given admission has any non-retired, non-canceled provisional bills.
     *
     * @param ad the admission to check for provisional bills
     * @return true if at least one provisional bill exists for the admission; false otherwise
     */
    public boolean isAddmissionHaveProvisionalBill(Admission ad) {
        List<Bill> ads = new ArrayList<>();
        String sql;
        HashMap h = new HashMap();
        sql = "select b from Bill b where b.retired=false "
                + " and b.billTypeAtomic=:bt "
                + " and b.cancelled=false"
                + " and b.patientEncounter=:pe";
        h.put("bt", BillTypeAtomic.INWARD_PROVISIONAL_BILL);
        h.put("pe", ad);
        ads = getBillFacade().findByJpql(sql, h);

        System.out.println("ads.size() = " + ads.size());

        if (ads.size() > 0 || !ads.isEmpty()) {
            return true;
        } else {
            return false;
        }

    }

    public void clearSearchValues() {
        institutionForSearch = null;
        patientNameForSearch = null;
        patientIdentityNumberForSearch = null;
        patientPhoneNumberForSearch = null;
        patientCodeForSearch = null;
        patientNumberForSearch = null;
        bhtNumberForSearch = null;
        referringDoctorForSearch = null;
        institutionForSearch = null;
        admissionStatusForSearch = null;
        admissionTypeForSearch = null;
        parentAdmission = null;
    }

    public String navigateToListAdmissions() {
        institutionForSearch = sessionController.getLoggedUser().getInstitution();
        if (configOptionApplicationController.getBooleanValueByKey("Restirct Inward Admission Search to Logged Department of the User")) {
            loggedDepartment = sessionController.getLoggedUser().getDepartment();
        }
        clearSearchValues();
        return "/inward/inpatient_search?faces-redirect=true";
    }

    public String navigateToListChildAdmissions() {
        perantAddmission = current;
        searchAdmissions();
        return "/inward/inpatient_search?faces-redirect=true";
    }

    public void listCurrentInpatients() {
        String sql;
        HashMap h = new HashMap();
        sql = "select c "
                + " from Admission c "
                + " where c.retired=false "
                + " and "
                + " (c.paymentFinalized is null or c.paymentFinalized=false)"
                + " order by c.bhtNo";
        currentAdmissions = getFacade().findByJpql(sql, h, 20);
    }

    public List<Admission> completePatientPaymentFinalized(String query) {
        List<Admission> suggestions;
        String sql;
        HashMap h = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Admission c "
                    + " where c.retired=false "
                    + " and c.paymentFinalized=true "
                    + " and ((c.bhtNo) like :q "
                    + " or (c.patient.person.name) like :q)"
                    + "  order by c.bhtNo";
            ////// // System.out.println(sql);
            //      h.put("btp", BillType.InwardPaymentBill);
            h.put("q", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findByJpql(sql, h, 20);
        }
        return suggestions;
    }

//    public List<Admission> completeDishcahrgedPatient(String query) {
//        List<Admission> suggestions;
//        String sql;
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//            sql = "select c from Admission c where c.retired=false and c.discharged=true and ((c.bhtNo) like '%" + query.toUpperCase() + "%' or (c.patient.person.name) like '%" + query.toUpperCase() + "%') order by c.bhtNo";
//            ////// // System.out.println(sql);
//            suggestions = getFacade().findByJpql(sql, 20);
//        }
//        return suggestions;
//    }
    public void prepareAdd() {
        current = new Admission();
    }

    List<Admission> admissionsWithErrors;

//    public List<Admission> getAdmissionsWithErrors() {
//        String sql;
//        sql = "select p from Admission p where c.patient.retired=false "
//                + "and (c.patient.patient is null or c.patient.bhtNo is null)";
//        admissionsWithErrors = getFacade().findByJpql(sql, 20);
//        if (admissionsWithErrors == null) {
//            admissionsWithErrors = new ArrayList<>();
//        }
//        return admissionsWithErrors;
//    }
    public void setAdmissionsWithErrors(List<Admission> admissionsWithErrors) {
        this.admissionsWithErrors = admissionsWithErrors;
    }

    public List<Admission> completeBht(String query) {
        List<Admission> suggestions;
        String sql;
        if (query == null || query.trim().equals("")) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from Admission p where c.patient.retired=false and (c.patient.bhtNo) like '%" + query.toUpperCase() + "%'";
            ////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql, 20);
        }
        if (suggestions == null) {
            suggestions = new ArrayList<>();
        }
        return suggestions;
    }

    public List<PatientEncounter> completePatientEncounter(String query) {
        List<PatientEncounter> suggestions;
        String sql;
        if (query == null || query.trim().equals("")) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from PatientEncounter p where c.patient.retired=false and (c.patient.bhtNo) like '%" + query.toUpperCase() + "%'";
            ////// // System.out.println(sql);
            suggestions = patientEncounterFacade.findByJpql(sql, 20);
        }
        if (suggestions == null) {
            suggestions = new ArrayList<>();
        }
        return suggestions;
    }

    public void delete() {

        if (getCurrent() != null) {
            getCurrent().setRetired(true);
            getCurrent().setRetiredAt(new Date());
            getCurrent().setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        prepereToAdmitNewPatient();
//        getItems();
        current = null;
        getCurrent();
    }

    public void setSelectedItems(List<Admission> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    public String navigateToAdmitFromMenu() {
        prepereToAdmitNewPatient();
        getCurrent().setPatient(getPatient());
        return "/inward/inward_admission";
    }

    public void prepereToAdmitNewPatient() {
        current = null;
        patientRoom = null;
        items = null;
        bhtText = "";
        ageText = null;
        patientList = null;
        patientTabId = "tabNewPt";
        patientSearchTab = 0;
        selectText = "";
        selectedItems = null;
        patient = null;
        yearMonthDay = null;
        printPreview = false;
        bhtNumberCalculation();
    }

    public String toAdmitAMember() {
        patientRoom = null;
        items = null;
        bhtText = "";
        ageText = null;
        patientList = null;
        patientTabId = "tabSearchPt";
        patientSearchTab = 1;
        selectText = "";
        selectedItems = null;
        patient = null;
        yearMonthDay = null;
        printPreview = false;
        bhtNumberCalculation();
        return "/inward/inward_admission";
    }

    public void discharge() {
        if (getCurrent().getId() == null || getCurrent().getId() == 0) {
            JsfUtil.addSuccessMessage("No Patient Data Found");
        } else {
            getCurrent().setDischarged(Boolean.TRUE);
            getCurrent().setDateOfDischarge(new Date());
            getEjbFacade().edit(current);
        }

    }

    private void savePatient() {
        String tc = sessionController.getApplicationPreference().getChangeTextCasesPatientName();
        String updatedPersonName = CommonFunctions.changeTextCases(getPatient().getPerson().getName(), tc);
        String updatedAddress = CommonFunctions.changeTextCases(getPatient().getPerson().getAddress(), tc);
        if (updatedPersonName != null) {
            getPatient().getPerson().setName(updatedPersonName);
        }
        if (updatedAddress != null) {
            getPatient().getPerson().setAddress(updatedAddress);
        }
        Person person = getPatient().getPerson();
        getPatient().setPerson(null);

        if (person != null) {
            person.setCreatedAt(Calendar.getInstance().getTime());
            person.setCreater(getSessionController().getLoggedUser());

            if (person.getId() == null) {
                getPersonFacade().create(person);
            } else {
                getPersonFacade().edit(person);
            }

        }

        getPatient().setCreatedAt(Calendar.getInstance().getTime());
        getPatient().setCreater(getSessionController().getLoggedUser());

        if (getPatient().getId() == null) {
            getPatientFacade().create(getPatient());
        } else {
            getPatientFacade().edit(getPatient());
        }

        getPatient().setPerson(person);
        getPatientFacade().edit(getPatient());
    }

    private void saveGuardian() {
        Person temG = getCurrent().getGuardian();
        temG.setCreatedAt(Calendar.getInstance().getTime());
        temG.setCreater(getSessionController().getLoggedUser());

        if (temG.getId() == null || temG.getId() == 0) {
            getPersonFacade().create(temG);
        } else {
            getPersonFacade().edit(temG);
        }

    }

    /**
     * Validates the current admission for required fields and configuration-based constraints.
     *
     * Checks admission type, payment method, admission date, credit company details, room assignment, referring consultant, patient, and guardian information as per application settings. Returns {@code true} if any validation fails and an error message is added; otherwise, returns {@code false}.
     *
     * @return {@code true} if validation errors are found; {@code false} otherwise
     */
    private boolean errorCheck() {
        if (getCurrent().getAdmissionType() == null) {
            JsfUtil.addErrorMessage("Please select Admission Type");
            return true;
        }
        if (getCurrent().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Paymentmethod");
            return true;
        }

        if (getCurrent().getDateOfAdmission() == null) {
            JsfUtil.addErrorMessage("Pleace Select Admitted Time");
            return true;
        }

        if (getCurrent().getPaymentMethod() == PaymentMethod.Credit) {
            if(!getCurrent().isClaimable()){
                JsfUtil.addErrorMessage("Please mark as Claimable");
                return true;
            }
            
            if (encounterCreditCompany.getInstitution() != null) {
                getCurrent().setCreditCompany(encounterCreditCompany.getInstitution());
                getCurrent().setCreditLimit(encounterCreditCompany.getCreditLimit());
                getCurrent().setPolicyNo(encounterCreditCompany.getPolicyNo());
                getCurrent().setReferanceNo(encounterCreditCompany.getReferanceNo());
                //TO DO - Add credit limit, etc
            }

            if (!getEncounterCreditCompanies().isEmpty()) {
                EncounterCreditCompany tec = getEncounterCreditCompanies().get(0);
                getCurrent().setCreditCompany(tec.getInstitution());
                getCurrent().setCreditLimit(tec.getCreditLimit());
                getCurrent().setPolicyNo(tec.getPolicyNo());
                getCurrent().setReferanceNo(tec.getReferanceNo());

                for (EncounterCreditCompany ecc : getEncounterCreditCompanies()) {
                    if (configOptionApplicationController.getBooleanValueByKey("Inward Patient Admit - Credit Companies Require Reference Number", false)) {
                        if (ecc.getReferanceNo() == null || ecc.getReferanceNo().isEmpty()) {
                            JsfUtil.addErrorMessage("Please Add the Reference Number for " + ecc.getInstitution().getName() + " Company");
                            return true;
                        }
                    }
                }

                //TO Do - add other fields
            }

            if (getCurrent().getCreditCompany() == null) {
                JsfUtil.addErrorMessage("Select Credit Company");
                return true;
            }
        }

        if (getCurrent().getAdmissionType().isRoomChargesAllowed()) {
            if (getPatientRoom().getRoomFacilityCharge() == null) {
                JsfUtil.addErrorMessage("Select Room ");
                return true;
            }
        }
        if (sessionController.getApplicationPreference().isInwardMoChargeCalculateInitialTime()) {
            if (getPatientRoom().getRoomFacilityCharge().getTimedItemFee().getDurationDaysForMoCharge() == 0.0) {
                JsfUtil.addErrorMessage("Plase Add Duration Days For Mo Charge");
                return true;
            }
            if (getPatientRoom().getRoomFacilityCharge().getMoChargeForAfterDuration() == null) {
                JsfUtil.addErrorMessage("Plase Add Charge for After Duration Days");
                return true;
            }
            if (getPatientRoom().getRoomFacilityCharge().getMoChargeForAfterDuration().equals("") || getPatientRoom().getRoomFacilityCharge().getMoChargeForAfterDuration().equals(0.0)) {
                JsfUtil.addErrorMessage("Plase Add Charge for After Duration Days");
                return true;
            }
        }

        if (getCurrent().getAdmissionType().isRoomChargesAllowed()) {
            if (getPatientRoom() != null) {
                if (getInwardBean().isRoomFilled(getPatientRoom().getRoomFacilityCharge().getRoom())) {
                    JsfUtil.addErrorMessage("Select Empty Room");
                    return true;
                }
            } else {
                JsfUtil.addErrorMessage("Room is Empty");
                return true;
            }
        }

        if (getCurrent().getReferringConsultant() == null) {
            JsfUtil.addErrorMessage("Please Select Referring Doctor");
            return true;
        }

        if (getCurrent().getPatient() == null) {
            JsfUtil.addErrorMessage("Select Patient");
            return true;
        }
        
        if (configOptionApplicationController.getBooleanValueByKey("Patient Details Required in Patient Admission",false)) {
            if (configOptionApplicationController.getBooleanValueByKey("Patient Title is Required in Patient Admission", false)) {
                if (getCurrent().getPatient().getPerson().getTitle()== null) {
                    JsfUtil.addErrorMessage("Patient Title is Required");
                    return true;
                }
            }
            if (configOptionApplicationController.getBooleanValueByKey("Patient Gender is Required in Patient Admission", false)) {
                if (getCurrent().getPatient().getPerson().getSex()== null) {
                    JsfUtil.addErrorMessage("Patient Gender is Required");
                    return true;
                }
            }
            if (configOptionApplicationController.getBooleanValueByKey("Patient Age is Required in Patient Admission", false)) {
                if (getCurrent().getPatient().getPerson().getDob()== null) {
                    JsfUtil.addErrorMessage("Patient Age is Required");
                    return true;
                }
            }
        }

        if (getCurrent().getAdmissionType().getAdmissionTypeEnum().equals(AdmissionTypeEnum.DayCase) && sessionController.getApplicationPreference().getApplicationInstitution().equals(ApplicationInstitution.Cooperative)) {
            if (getCurrent().getComments() == null || getCurrent().getComments().isEmpty()) {
                JsfUtil.addErrorMessage("Please Add Reference No");
                return true;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Guardian Details Required in Patient Admission")) {
            if (getCurrent().getGuardian() == null) {
                JsfUtil.addErrorMessage("Guardian is Required");
                return true;
            }
            if (configOptionApplicationController.getBooleanValueByKey("Guardian Title is Required in Patient Admission", true)) {
                if (getCurrent().getGuardian().getTitle() == null) {
                    JsfUtil.addErrorMessage("Guardian Title is Required");
                    return true;
                }
            }
            if (configOptionApplicationController.getBooleanValueByKey("Guardian Name is Required in Patient Admission", true)) {
                if (getCurrent().getGuardian().getName() == null || getCurrent().getGuardian().getName().isEmpty()) {
                    JsfUtil.addErrorMessage("Guardian Name is Required");
                    return true;
                }
            }
            if (configOptionApplicationController.getBooleanValueByKey("Guardian NIC is Required in Patient Admission", true)) {
                if (getCurrent().getGuardian().getNic() == null || getCurrent().getGuardian().getNic().isEmpty()) {
                    JsfUtil.addErrorMessage("Guardian NIC is Required");
                    return true;
                }
            }
            if (configOptionApplicationController.getBooleanValueByKey("Guardian Address is Required in Patient Admission", true)) {
                if (getCurrent().getGuardian().getAddress() == null || getCurrent().getGuardian().getAddress().isEmpty()) {
                    JsfUtil.addErrorMessage("Guardian Address is Required");
                    return true;
                }
            }
            if (configOptionApplicationController.getBooleanValueByKey("Guardian Mobile Number is Required in Patient Admission", true)) {
                if (getCurrent().getGuardian().getMobile() == null || getCurrent().getGuardian().getMobile().isEmpty()) {
                    JsfUtil.addErrorMessage("Guardian Mobile Number is Required");
                    return true;
                }
            }
            if (configOptionApplicationController.getBooleanValueByKey("Guardian Home Phone Number is Required in Patient Admission", true)) {
                if (getCurrent().getGuardian().getPhone() == null || getCurrent().getGuardian().getPhone().isEmpty()) {
                    JsfUtil.addErrorMessage("Guardian Home Phone Number is Required");
                    return true;
                }
            }
            if (configOptionApplicationController.getBooleanValueByKey("Guardian Relationship is Required in Patient Admission", true)) {
                if (getCurrent().getGuardianRelationshipToPatient() == null) {
                    JsfUtil.addErrorMessage("Guardian Relationship is Required");
                    return true;
                }
            }
        }

        return false;
    }

    @Inject
    private InwardPaymentController inwardPaymentController;
    @EJB
    private AppointmentFacade appointmentFacade;
    @EJB
    private BillFacade billFacade;

    private void updateAppointment() {
        String sql = "Select s from Appointment s"
                + " where s.retired=false "
                + " and s.bill=:b ";
        HashMap hm = new HashMap();
        hm.put("b", getAppointmentBill());
        Appointment apt = getAppointmentFacade().findFirstByJpql(sql, hm);
        apt.setPatientEncounter(getCurrent());
        getAppointmentFacade().edit(apt);

    }

    private void updateAppointmentBill() {
        getAppointmentBill().setRefunded(true);
        getBillFacade().edit(getAppointmentBill());

    }
    
    public void listnerForAppoimentSelect(Bill ap){
        System.out.println("ap = " + ap);
        if(ap == null){
            return;
        }
        setPatient(ap.getPatient());
    }

    @Inject
    private InwardBeanController inwardBean;

    public void addPatientRoom() {
        PatientRoom currentPatientRoom = getInwardBean().savePatientRoom(getPatientRoom(), null, getPatientRoom().getRoomFacilityCharge(), getCurrent(), getCurrent().getDateOfAdmission(), getSessionController().getLoggedUser());
        getCurrent().setCurrentPatientRoom(currentPatientRoom);
        getFacade().edit(getCurrent());
        JsfUtil.addSuccessMessage("Patient room added");
    }

    public void updateSelected() {
        if (current == null) {
            JsfUtil.addErrorMessage("No Admission");
            return;
        }
        getFacade().edit(current);
        JsfUtil.addSuccessMessage("Updated");
    }

    public void addPatient() {
        if (current == null) {
            JsfUtil.addErrorMessage("No Admission");
            return;
        }
        Person p = new Person();
        getPersonFacade().create(p);
        Patient pt = new Patient();
        patientDetailsEditable = true;
        pt.setPerson(p);
        getPatientFacade().create(pt);
        getCurrent().setPatient(pt);
        getFacade().edit(current);
        JsfUtil.addSuccessMessage("Patient Added. Go to Edit BHT and edit. ALso add a patient room");

    }

    public void addGuardian() {
        if (current == null) {
            JsfUtil.addErrorMessage("No Admission");
            return;
        }
        Person p = new Person();
        getPersonFacade().create(p);
        getCurrent().setGuardian(p);
        JsfUtil.addSuccessMessage("Patient Added. Go to Edit BHT and edit. ALso add a patient room");

    }

    public void updateBHTNo() {
        if (current.getBhtNo() == null || current.getBhtNo().isEmpty()) {
            JsfUtil.addErrorMessage("BHT NO");
            return;
        }
        if (patientRoom.getRoomFacilityCharge() == null) {
            JsfUtil.addErrorMessage("Room...");
            return;
        }
        if (current.getAdmissionType() == null) {
            JsfUtil.addErrorMessage("Admission Type.");
            return;
        }
        addPatient();
        addGuardian();
        addPatientRoom();
        getFacade().edit(current);
        current = new Admission();
        patientRoom = new PatientRoom();
    }

    public void saveSelected() {
        if (admittingProcessStarted) {
            JsfUtil.addErrorMessage("Admittin process already started.");
            return;
        }
        admittingProcessStarted = true;

        if (errorCheck()) {
            admittingProcessStarted = false;
            return;
        }
        savePatient();
        savePatientAllergies();
        saveGuardian();
        boolean bhtCanBeEdited = configOptionApplicationController.getBooleanValueByKey("BHT Number can be edited at the time of admission");
        if (bhtText == null || bhtText.trim().equals("")) {
            bhtText = getInwardBean().getBhtText(getCurrent().getAdmissionType());
        } else {
            if (!bhtCanBeEdited) {
                bhtText = getInwardBean().getBhtText(getCurrent().getAdmissionType());
            }
        }
//        bhtText = getInwardBean().getBhtText(getCurrent().getAdmissionType());
        getCurrent().setBhtNo(getBhtText());

        //  getCurrent().setBhtNo(bhtText);
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getCurrent().setInstitution(sessionController.getInstitution());
            getCurrent().setDepartment(sessionController.getDepartment());
            getFacade().create(getCurrent());
            JsfUtil.addSuccessMessage("Patient Admitted Succesfully");
        }

        if (getCurrent().getAdmissionType().isRoomChargesAllowed() || getPatientRoom().getRoomFacilityCharge() != null) {
            PatientRoom currentPatientRoom = getInwardBean().savePatientRoom(getPatientRoom(), null, getPatientRoom().getRoomFacilityCharge(), getCurrent(), getCurrent().getDateOfAdmission(), getSessionController().getLoggedUser());
            getCurrent().setCurrentPatientRoom(currentPatientRoom);
        }

        getFacade().edit(getCurrent());

        double appointmentFee = 0;
        if (getAppointmentBill() != null) {
            appointmentFee = getAppointmentBill().getTotal();
            updateAppointment();
            updateAppointmentBill();
        }

        if (appointmentFee != 0) {
            getInwardPaymentController().getCurrent().setPaymentMethod(getCurrent().getPaymentMethod());
            getInwardPaymentController().getCurrent().setPatientEncounter(current);
            getInwardPaymentController().getCurrent().setTotal(appointmentFee);
            getInwardPaymentController().pay();
            getInwardPaymentController().makeNull();
        }

        saveEncounterCreditCompanies(current);

        // Save EncounterCreditCompanies
        // Need to create EncounterCredit
        admittingProcessStarted = false;
        printPreview = true;
    }

    public void saveConvertSelected() {
        if (errorCheck()) {
            return;
        }
        savePatient();
        savePatientAllergies();
        saveGuardian();
        boolean bhtCanBeEdited = configOptionApplicationController.getBooleanValueByKey("BHT Number can be edited at the time of admission");
        if (bhtText == null || bhtText.trim().equals("")) {
            bhtText = getInwardBean().getBhtText(getCurrent().getAdmissionType());
        } else {
            if (!bhtCanBeEdited) {
                bhtText = getInwardBean().getBhtText(getCurrent().getAdmissionType());
            }
        }
//        bhtText = getInwardBean().getBhtText(getCurrent().getAdmissionType());
        getCurrent().setBhtNo(getBhtText());

        //  getCurrent().setBhtNo(bhtText);
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getCurrent().setInstitution(sessionController.getInstitution());
            getCurrent().setDepartment(sessionController.getDepartment());
            getFacade().create(getCurrent());
            JsfUtil.addSuccessMessage("Patient Admitted Succesfully");
        }

        if (getCurrent().getAdmissionType().isRoomChargesAllowed() || getPatientRoom().getRoomFacilityCharge() != null) {
            PatientRoom currentPatientRoom = getInwardBean().savePatientRoom(getPatientRoom(), null, getPatientRoom().getRoomFacilityCharge(), getCurrent(), getCurrent().getDateOfAdmission(), getSessionController().getLoggedUser());
            getCurrent().setCurrentPatientRoom(currentPatientRoom);
        }

        getFacade().edit(getCurrent());

        double appointmentFee = 0;
        if (getAppointmentBill() != null) {
            appointmentFee = getAppointmentBill().getTotal();
            updateAppointment();
            updateAppointmentBill();
        }

        if (appointmentFee != 0) {
            getInwardPaymentController().getCurrent().setPaymentMethod(getCurrent().getPaymentMethod());
            getInwardPaymentController().getCurrent().setPatientEncounter(current);
            getInwardPaymentController().getCurrent().setTotal(appointmentFee);
            getInwardPaymentController().pay();
            getInwardPaymentController().makeNull();
        }

        saveEncounterCreditCompanies(current);

        getCurrentNonBht().setParentEncounter(current);
        getCurrentNonBht().setDischarged(true);
        getCurrentNonBht().setDateOfDischarge(new Date());
        getCurrentNonBht().setConvertedToAnotherEncounter(true);
        getFacade().edit(currentNonBht);
        currentNonBht = null;

        // Save EncounterCreditCompanies
        // Need to create EncounterCredit
        printPreview = true;
    }

    public void saveEncounterCreditCompanies(PatientEncounter current) {
        if (!encounterCreditCompanies.isEmpty() && current != null) {
            for (EncounterCreditCompany ecc : encounterCreditCompanies) {
                ecc.setPatientEncounter(current);
                ecc.setCreatedAt(new Date());
                ecc.setCreater(sessionController.getLoggedUser());
                if (ecc.getInstitution() != null) {
                    getEncounterCreditCompanyFacade().create(ecc);
                } else {
                    getEncounterCreditCompanyFacade().edit(ecc);
                }
            }
        }
        encounterCreditCompanies = new ArrayList<>();
        encounterCreditCompany = new EncounterCreditCompany();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public AdmissionFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(AdmissionFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public AdmissionController() {
    }

    public Admission getCurrent() {
        if (current == null) {
            current = new Admission();
            current.setDateOfAdmission(new Date());
            EncounterCreditCompany encounterCreditCompany = new EncounterCreditCompany();
            encounterCreditCompany.setPatientEncounter(current);
        }
        return current;
    }

    public Admission getParentAdmission() {
        return parentAdmission;
    }

    public void setParentAdmission(Admission parentAdmission) {
        this.parentAdmission = parentAdmission;
    }

    public void createChildAdmission() {
        if (parentAdmission == null) {
            JsfUtil.addErrorMessage("Please select the mother encounter");
            return;
        }
        if (parentAdmission.getParentEncounter() != null) {
            JsfUtil.addErrorMessage("This mother encounter already has another Mother ENcounter, which is NOT possible. Select some other encounter");
            return;
        }

        current = null;
        getCurrent();

        current.setAdmissionType(parentAdmission.getAdmissionType());

    }

    public void setCurrent(Admission current) {
        this.current = current;
    }

    private AdmissionFacade getFacade() {
        return ejbFacade;
    }

    public List<Admission> getItems() {
//        if (items == null) {
//            String temSql;
//            temSql = "SELECT i FROM Admission i where i.retired=false and i.discharged=false order by i.bhtNo";
//            items = getFacade().findByJpql(temSql);
//            if (items == null) {
//                items = new ArrayList<>();
//            }
//        }

        return items;
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

    public List<Patient> getPatientList() {
        if (patientList == null) {
            String temSql;
            temSql = "SELECT i FROM Patient i where i.retired=false ";
            patientList = getPatientFacade().findByJpql(temSql);
        }
        return patientList;
    }

    public void setPatientList(List<Patient> patientList) {
        this.patientList = patientList;
    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
    }

    public PatientRoom getPatientRoom() {
        if (patientRoom == null) {
            patientRoom = new PatientRoom();
        }
        return patientRoom;
    }

    public void setPatientRoom(PatientRoom patientRoom) {
        this.patientRoom = patientRoom;
    }

    public String getAgeText() {
        ageText = getPatient().getAge();
        return ageText;
    }

    public void setAgeText(String ageText) {
        this.ageText = ageText;
        getPatient().getPerson().setDob(CommonFunctions.guessDob(ageText));
    }

    public PatientRoomFacade getPatientRoomFacade() {
        return patientRoomFacade;
    }

    public void setPatientRoomFacade(PatientRoomFacade patientRoomFacade) {
        this.patientRoomFacade = patientRoomFacade;
    }

    public void bhtNumberCalculation() {
        if (getCurrent() == null || getCurrent().getAdmissionType() == null) {
//            JsfUtil.addErrorMessage("Please Set Admission Type DayCase/Admission For this this Admission ");
            return;
        }

        if (getCurrent().getAdmissionType().getAdmissionTypeEnum() == null) {
            JsfUtil.addErrorMessage("Please Set Admission Type DayCase/Admission For this this Admission ");
            return;
        }

        bhtText = getInwardBean().getBhtText(getCurrent().getAdmissionType());

        getPatientRoom().setRoomFacilityCharge(getCurrent().getAdmissionType().getRoomFacilityCharge());
    }

    public String getBhtText() {
        return bhtText;
    }

    public void setBhtText(String bhtText) {
        this.bhtText = bhtText;
    }

    public RoomFacade getRoomFacade() {
        return roomFacade;
    }

    public void setRoomFacade(RoomFacade roomFacade) {
        this.roomFacade = roomFacade;
    }

    public String getPatientTabId() {
        return patientTabId;
    }

    public void setPatientTabId(String patientTabId) {
        this.patientTabId = patientTabId;
    }

    @Override
    public Patient getPatient() {
        if (current != null) {
            patient = getCurrent().getPatient();
        }
        if (patient == null) {
            Person p = new Person();
            patient = new Patient();
            patientDetailsEditable = true;
            patient.setPerson(p);
        }
        return patient;
    }

    @Override
    public void setPatient(Patient patient) {
        this.patient = patient;
        if (current != null) {
            current.setPatient(patient);
            patientAllergies = clinicalFindingValueController.findClinicalFindingValues(patient, ClinicalFindingValueType.PatientAllergy);
        }
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

    public Bill getAppointmentBill() {
        return appointmentBill;
    }

    public void setAppointmentBill(Bill appointmentBill) {
        this.appointmentBill = appointmentBill;
    }

    public InwardPaymentController getInwardPaymentController() {
        return inwardPaymentController;
    }

    public void setInwardPaymentController(InwardPaymentController inwardPaymentController) {
        this.inwardPaymentController = inwardPaymentController;
    }

    public AppointmentFacade getAppointmentFacade() {
        return appointmentFacade;
    }

    public void setAppointmentFacade(AppointmentFacade appointmentFacade) {
        this.appointmentFacade = appointmentFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
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

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public int getPatientSearchTab() {
        return patientSearchTab;
    }

    public void setPatientSearchTab(int patientSearchTab) {
        this.patientSearchTab = patientSearchTab;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getPatientNameForSearch() {
        return patientNameForSearch;
    }

    public void setPatientNameForSearch(String patientNameForSearch) {
        this.patientNameForSearch = patientNameForSearch;
    }

    public String getBhtNumberForSearch() {
        return bhtNumberForSearch;
    }

    public void setBhtNumberForSearch(String bhtNumberForSearch) {
        this.bhtNumberForSearch = bhtNumberForSearch;
    }

    public Doctor getReferringDoctorForSearch() {
        return referringDoctorForSearch;
    }

    public void setReferringDoctorForSearch(Doctor referringDoctorForSearch) {
        this.referringDoctorForSearch = referringDoctorForSearch;
    }

    public InwardStaffPaymentBillController getInwardStaffPaymentBillController() {
        return inwardStaffPaymentBillController;
    }

    public void setInwardStaffPaymentBillController(InwardStaffPaymentBillController inwardStaffPaymentBillController) {
        this.inwardStaffPaymentBillController = inwardStaffPaymentBillController;
    }

    public List<Admission> getCurrentAdmissions() {
        return currentAdmissions;
    }

    public void setCurrentAdmissions(List<Admission> currentAdmissions) {
        this.currentAdmissions = currentAdmissions;
    }

    public String getPatientIdentityNumberForSearch() {
        return patientIdentityNumberForSearch;
    }

    public void setPatientIdentityNumberForSearch(String patientIdentityNumberForSearch) {
        this.patientIdentityNumberForSearch = patientIdentityNumberForSearch;
    }

    public String getPatientPhoneNumberForSearch() {
        return patientPhoneNumberForSearch;
    }

    public void setPatientPhoneNumberForSearch(String patientPhoneNumberForSearch) {
        this.patientPhoneNumberForSearch = patientPhoneNumberForSearch;
    }

    public Institution getInstitutionForSearch() {
        return institutionForSearch;
    }

    public void setInstitutionForSearch(Institution institutionForSearch) {
        this.institutionForSearch = institutionForSearch;
    }

    public AdmissionStatus getAdmissionStatusForSearch() {
        return admissionStatusForSearch;
    }

    public void setAdmissionStatusForSearch(AdmissionStatus admissionStatusForSearch) {
        this.admissionStatusForSearch = admissionStatusForSearch;
    }

    public String getPatientCodeForSearch() {
        return patientCodeForSearch;
    }

    public void setPatientCodeForSearch(String patientCodeForSearch) {
        this.patientCodeForSearch = patientCodeForSearch;
    }

    public String getPatientNumberForSearch() {
        return patientNumberForSearch;
    }

    public void setPatientNumberForSearch(String patientNumberForSearch) {
        this.patientNumberForSearch = patientNumberForSearch;
    }

    @Override
    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    public List<EncounterCreditCompany> getEncounterCreditCompanies() {
        if (encounterCreditCompanies == null) {
            encounterCreditCompanies = new ArrayList<>();
        }
        return encounterCreditCompanies;
    }

    public void setEncounterCreditCompanies(List<EncounterCreditCompany> encounterCreditCompanies) {
        this.encounterCreditCompanies = encounterCreditCompanies;
    }

    public EncounterCreditCompany getEncounterCreditCompany() {
        if (encounterCreditCompany == null) {
            encounterCreditCompany = new EncounterCreditCompany();
            encounterCreditCompany.setPatientEncounter(current);
        }
        return encounterCreditCompany;
    }

    public void setEncounterCreditCompany(EncounterCreditCompany encounterCreditCompany) {
        this.encounterCreditCompany = encounterCreditCompany;
    }

    public EncounterCreditCompanyFacade getEncounterCreditCompanyFacade() {
        return encounterCreditCompanyFacade;
    }

    public void setEncounterCreditCompanyFacade(EncounterCreditCompanyFacade encounterCreditCompanyFacade) {
        this.encounterCreditCompanyFacade = encounterCreditCompanyFacade;
    }

    public ClinicalFindingValue getCurrentPatientAllergy() {
        if (currentPatientAllergy == null) {
            currentPatientAllergy = new ClinicalFindingValue();
            currentPatientAllergy.setClinicalFindingValueType(ClinicalFindingValueType.PatientAllergy);
            currentPatientAllergy.setPatient(getPatient());
        }
        return currentPatientAllergy;
    }

    public void setCurrentPatientAllergy(ClinicalFindingValue currentPatientAllergy) {
        this.currentPatientAllergy = currentPatientAllergy;
    }

    public List<ClinicalFindingValue> getPatientAllergies() {
        if (patientAllergies == null) {
            patientAllergies = new ArrayList<>();
        }
        return patientAllergies;
    }

    public void setPatientAllergies(List<ClinicalFindingValue> patientAllergies) {
        this.patientAllergies = patientAllergies;
    }

    public Institution getLastCreditCompany() {
        return lastCreditCompany;
    }

    public void setLastCreditCompany(Institution lastCreditCompany) {
        this.lastCreditCompany = lastCreditCompany;
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    @Override
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    @Override
    public void listnerForPaymentMethodChange() {
        // ToDo: Add Logic
    }

    public Department getLoggedDepartment() {
        return loggedDepartment;
    }

    public void setLoggedDepartment(Department loggedDepartment) {
        this.loggedDepartment = loggedDepartment;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Admission getPerantAddmission() {
        return perantAddmission;
    }

    public void setPerantAddmission(Admission perantAddmission) {
        this.perantAddmission = perantAddmission;
    }

    public Admission getCurrentNonBht() {
        return currentNonBht;
    }

    public void setCurrentNonBht(Admission currentNonBht) {
        this.currentNonBht = currentNonBht;
    }

    public boolean isAdmittingProcessStarted() {
        return admittingProcessStarted;
    }

    public void setAdmittingProcessStarted(boolean admittingProcessStarted) {
        this.admittingProcessStarted = admittingProcessStarted;
    }

    public AdmissionType getAdmissionTypeForSearch() {
        return admissionTypeForSearch;
    }

    public void setAdmissionTypeForSearch(AdmissionType admissionTypeForSearch) {
        this.admissionTypeForSearch = admissionTypeForSearch;
    }

    public Reservation getLatestfoundReservation() {
        return latestfoundReservation;
    }

    public void setLatestfoundReservation(Reservation latestfoundReservation) {
        this.latestfoundReservation = latestfoundReservation;
    }

    /**
     *
     */
    @FacesConverter(forClass = Admission.class)
    public static class AdmissionControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AdmissionController controller = (AdmissionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "admissionController");
            if (controller == null) {
                return null;
            }
            Long l = getKey(value);
            if (l == null) {
                return null;
            }
            return controller.getEjbFacade().find(l);
        }

        java.lang.Long getKey(String value) {
            if (value == null) {
                return null;
            }
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            if (value == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Admission) {
                Admission o = (Admission) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + AdmissionController.class.getName());
            }
        }
    }
}
