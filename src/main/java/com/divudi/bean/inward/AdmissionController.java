    /*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Appointment;
import com.divudi.entity.Bill;
import com.divudi.entity.Institution;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Person;
import com.divudi.entity.inward.Admission;
import com.divudi.entity.inward.PatientRoom;
import com.divudi.facade.AdmissionFacade;
import com.divudi.facade.AppointmentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PatientRoomFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.RoomFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class AdmissionController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;

    @Inject
    InwardStaffPaymentBillController inwardStaffPaymentBillController;
    ////////////
    @EJB
    private AdmissionFacade ejbFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    private RoomFacade roomFacade;
    ////////////////////////////
    @EJB
    private CommonFunctions commonFunctions;
    ///////////////////////
    List<Admission> selectedItems;
    private Admission current;
    private Admission parentAdmission;
    private PatientRoom patientRoom;
    private List<Admission> items = null;
    private List<Patient> patientList;
    private boolean printPreview;
    ///////////////////////////
    String selectText = "";
    private String ageText = "";
    private String bhtText = "";
    private String patientTabId = "tabNewPt";
    private Patient newPatient;
    private YearMonthDay yearMonthDay;
    private Bill appointmentBill;
    private PaymentMethodData paymentMethodData;
    @EJB
    PatientEncounterFacade patientEncounterFacade;

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public void resetCreditDetail(PatientEncounter patientEncounter) {
        if (patientEncounter == null) {
            UtilityController.addErrorMessage("Nuull");
            return;
        }

        patientEncounter.setCreditCompany(null);
        patientEncounter.setCreditLimit(0);
        patientEncounter.setCreditPaidAmount(0);
        patientEncounter.setCreditUsedAmount(0);
        getPatientEncounterFacade().edit(patientEncounter);
    }

    public void dateChangeListen() {
        getNewPatient().getPerson().setDob(getCommonFunctions().guessDob(yearMonthDay));

    }

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
                    + " and (upper(c.bhtNo) like :q"
                    + " or upper(c.patient.person.name) like :q "
                    + " or upper(c.creditCompany.name) like :q ) "
                    + " order by c.creditCompany.name";

            hash.put("pm", PaymentMethod.Credit);
            hash.put("val", 0.1);
            hash.put("q", "%" + qry.toUpperCase() + "%");
            a = getFacade().findBySQL(sql, hash, 20);
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
        List<Admission> lst = getFacade().findBySQL(sql, hash);

        return lst;
    }

    public List<Admission> completePatientPaymentDue(String qry) {
        String sql = "Select b.patientEncounter From "
                + " BilledBill b where"
                + " b.retired=false "
                + " and b.cancelled=false "
                + " and b.billType=:btp "
                + " and (abs(b.netTotal)-abs(b.paidAmount)) > :val "
                + " and (upper(b.patientEncounter.bhtNo) like :q or"
                + " upper(b.patientEncounter.patient.person.name) like :q ) "
                + " order by b.patientEncounter.bhtNo";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardFinalBill);
        hm.put("val", 0.1);
        hm.put("q", "%" + qry.toUpperCase() + "%");

        List<Admission> b = getEjbFacade().findBySQL(sql, hm, 20);

        if (b == null) {
            return new ArrayList<>();
        }

        return b;

    }

    public List<Admission> completePatientPaymentMax(String qry) {
        String sql = "Select b.patientEncounter From "
                + " BilledBill b where"
                + " b.retired=false "
                + " and b.cancelled=false "
                + " and b.billType=:btp"
                + " and (abs(b.paidAmount)- abs(b.netTotal)) > :val "
                + " and (upper(b.patientEncounter.bhtNo) like :q or"
                + " upper(b.patientEncounter.patient.person.name) like :q ) "
                + " order by b.patientEncounter.bhtNo";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardFinalBill);
        hm.put("val", 0.1);
        hm.put("q", "%" + qry.toUpperCase() + "%");

        List<Admission> b = getEjbFacade().findBySQL(sql, hm, 20);

        if (b == null) {
            return new ArrayList<>();
        }

        return b;

    }

    public List<Admission> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from Admission c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void onTabChange(TabChangeEvent event) {
        setPatientTabId(event.getTab().getId());

    }

    public List<Admission> completePatient(String query) {
        List<Admission> suggestions;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Admission c"
                + " where c.retired=false "
                + " and c.discharged=false "
                + " and (upper(c.bhtNo) like :q or"
                + " upper(c.patient.person.name) like :q ) "
                + " order by c.bhtNo";
        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getFacade().findBySQL(sql, hm, 20);

        return suggestions;
    }

    public List<Admission> completePatientAll(String query) {
        List<Admission> suggestions;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Admission c"
                + " where (upper(c.bhtNo) like :q or"
                + " upper(c.patient.person.name) like :q ) "
                + " order by c.bhtNo";
        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getFacade().findBySQL(sql, hm, 20);

        return suggestions;
    }

    public List<Admission> completePatientFinaled(String query) {
        List<Admission> suggestions;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Admission c"
                + " where (upper(c.bhtNo) like :q or"
                + " upper(c.patient.person.name) like :q ) "
                + " and c.paymentFinalized=true"
                + " order by c.bhtNo";
        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getFacade().findBySQL(sql, hm, 20);

        return suggestions;
    }

    public List<Admission> completePatientCredit(String query) {
        List<Admission> suggestions;
        String sql;
        HashMap hm = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Admission c where c.retired=false and c.paymentMethod=:pm  and (upper(c.bhtNo) like '%" + query.toUpperCase() + "%' or upper(c.patient.person.name) like '%" + query.toUpperCase() + "%') order by c.bhtNo";
            hm.put("pm", PaymentMethod.Credit);
            //System.out.println(sql);
            suggestions = getFacade().findBySQL(sql, hm, TemporalType.TIME, 20);
        }
        return suggestions;
    }

    public List<Admission> completePatientDishcargedNotFinalized(String query) {
        List<Admission> suggestions;
        String sql;
        HashMap h = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Admission c where c.retired=false and "
                    + " ( c.paymentFinalized is null or c.paymentFinalized=false )"
                    + " and ( (upper(c.bhtNo) like :q )or (upper(c.patient.person.name)"
                    + " like :q) ) order by c.bhtNo";
            //System.out.println(sql);
            //      h.put("btp", BillType.InwardPaymentBill);
            h.put("q", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findBySQL(sql, h, 20);
        }
        return suggestions;
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
                    + " and (upper(c.bhtNo) like :q "
                    + " or upper(c.patient.person.name) like :q)"
                    + "  order by c.bhtNo";
            //System.out.println(sql);
            //      h.put("btp", BillType.InwardPaymentBill);
            h.put("q", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findBySQL(sql, h, 20);
        }
        return suggestions;
    }

    public List<Admission> completeDishcahrgedPatient(String query) {
        List<Admission> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Admission c where c.retired=false and c.discharged=true and (upper(c.bhtNo) like '%" + query.toUpperCase() + "%' or upper(c.patient.person.name) like '%" + query.toUpperCase() + "%') order by c.bhtNo";
            //System.out.println(sql);
            suggestions = getFacade().findBySQL(sql, 20);
        }
        return suggestions;
    }

    public void prepareAdd() {
        current = new Admission();
    }

    List<Admission> admissionsWithErrors;

    public List<Admission> getAdmissionsWithErrors() {
        String sql;
        sql = "select p from Admission p where p.retired=false "
                + "and (p.patient is null or p.bhtNo is null)";
        admissionsWithErrors = getFacade().findBySQL(sql, 20);
        if (admissionsWithErrors == null) {
            admissionsWithErrors = new ArrayList<>();
        }
        return admissionsWithErrors;
    }

    public void setAdmissionsWithErrors(List<Admission> admissionsWithErrors) {
        this.admissionsWithErrors = admissionsWithErrors;
    }

    public List<Admission> completeBht(String query) {
        List<Admission> suggestions;
        String sql;
        if (query == null || query.trim().equals("")) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from Admission p where p.retired=false and upper(p.bhtNo) like '%" + query.toUpperCase() + "%'";
            //System.out.println(sql);
            suggestions = getFacade().findBySQL(sql, 20);
        }
        if (suggestions == null) {
            suggestions = new ArrayList<>();
        }
        return suggestions;
    }

    public void delete() {

        if (getCurrent() != null) {
            getCurrent().setRetired(true);
            getCurrent().setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getCurrent().setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        makeNull();
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

    public void makeNull() {
        current = null;
        patientRoom = null;
        items = null;
        bhtText = "";
        ageText = null;
        patientList = null;
        patientTabId = "tabNewPt";
        selectText = "";
        selectedItems = null;
        newPatient = null;
        yearMonthDay = null;
        printPreview = false;
        bhtNumberCalculation();
    }

    public void discharge() {
        if (getCurrent().getId() == null || getCurrent().getId() == 0) {
            UtilityController.addSuccessMessage("No Patient Data Found");
        } else {
            getCurrent().setDischarged(Boolean.TRUE);
            getCurrent().setDateOfDischarge(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getEjbFacade().edit(current);
        }

    }

    private void savePatient() {
        Person person = getNewPatient().getPerson();
        getNewPatient().setPerson(null);

        if (person != null) {
            person.setCreatedAt(Calendar.getInstance().getTime());
            person.setCreater(getSessionController().getLoggedUser());

            if (person.getId() == null) {
                getPersonFacade().create(person);
            } else {
                getPersonFacade().edit(person);
            }

        }

        getNewPatient().setCreatedAt(Calendar.getInstance().getTime());
        getNewPatient().setCreater(getSessionController().getLoggedUser());

        if (getNewPatient().getId() == null) {
            getPatientFacade().create(getNewPatient());
        } else {
            getPatientFacade().edit(getNewPatient());
        }

        getNewPatient().setPerson(person);
        getPatientFacade().edit(getNewPatient());
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

    private boolean errorCheck() {

        if (getCurrent().getAdmissionType() == null) {
            UtilityController.addErrorMessage("Please select Admission Type");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            UtilityController.addErrorMessage("Select Paymentmethod");
            return true;
        }

        if (getCurrent().getPaymentMethod() == PaymentMethod.Credit) {
            if (getCurrent().getCreditCompany() == null) {
                UtilityController.addErrorMessage("Select Credit Company");
                return true;
            }

        }

        if (getPatientRoom().getRoomFacilityCharge() == null) {
            UtilityController.addErrorMessage("Select Room ");
            return true;
        }

        if (sessionController.getInstitutionPreference().isInwardMoChargeCalculateInitialTime()) {
            if (getPatientRoom().getRoomFacilityCharge().getTimedItemFee().getDurationDaysForMoCharge() == 0.0) {
                JsfUtil.addErrorMessage("Plase Add Duration Days For Mo Charge");
                return true;
            }
            if (getPatientRoom().getRoomFacilityCharge().getMoChargeForAfterDuration()==null) {
                JsfUtil.addErrorMessage("Plase Add Charge for After Duration Days");
                return true;
            }
            if (getPatientRoom().getRoomFacilityCharge().getMoChargeForAfterDuration().equals("") || getPatientRoom().getRoomFacilityCharge().getMoChargeForAfterDuration().equals(0.0)) {
                JsfUtil.addErrorMessage("Plase Add Charge for After Duration Days");
                return true;
            }
        }

        if (getCurrent().getAdmissionType().isRoomChargesAllowed()) {
            if (getInwardBean().isRoomFilled(getPatientRoom().getRoomFacilityCharge().getRoom())) {
                UtilityController.addErrorMessage("Select Empty Room");
                return true;
            }
        }

        if (getCurrent().getReferringDoctor() == null) {
            UtilityController.addErrorMessage("Please Select Referring Doctor");
            return true;
        }

//        if (inwardStaffPaymentBillController.referringDoctorSpeciality == null) {
//            UtilityController.addErrorMessage("Please Select Referring Doctor Speciality");
//            return true;
//        }
        if (getPatientTabId().toString().equals("tabNewPt")) {
            if ("".equals(getAgeText())) {
                UtilityController.addErrorMessage("Patient Age Should be Typed");
                return true;
            }
            if (getNewPatient().getPerson().getName() == null || getNewPatient().getPerson().getName().trim().equals("") || getNewPatient().getPerson().getSex() == null || getAgeText() == null) {
                UtilityController.addErrorMessage("Can not admit without Patient Name, Age or Sex.");
                return true;
            }
        }

        if (getPatientTabId().toString().trim().equals("tabSearchPt")) {
            if (getCurrent().getPatient() == null) {
                UtilityController.addErrorMessage("Select Patient");
                return true;
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
        Appointment apt = getAppointmentFacade().findFirstBySQL(sql, hm);
        apt.setPatientEncounter(getCurrent());
        getAppointmentFacade().edit(apt);

    }

    private void updateAppointmentBill() {
        getAppointmentBill().setRefunded(true);
        getBillFacade().edit(getAppointmentBill());

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
        System.out.println("current.getBhtNo() = " + current.getBhtNo());
        System.out.println("current.getCurrentPatientRoom() = " + patientRoom.getRoomFacilityCharge());
        System.out.println("current.getAdmissionType() = " + current.getAdmissionType());
        if (current.getBhtNo() == null || current.getBhtNo().isEmpty()) {
            UtilityController.addErrorMessage("BHT NO");
            return;
        }
        if (patientRoom.getRoomFacilityCharge() == null) {
            UtilityController.addErrorMessage("Room...");
            return;
        }
        if (current.getAdmissionType() == null) {
            UtilityController.addErrorMessage("Admission Type.");
            return;
        }
        addPatient();
        addGuardian();
        addPatientRoom();
        System.out.println("BHT No = " + current.getBhtNo());
        getFacade().edit(current);
        current = new Admission();
        patientRoom = new PatientRoom();
    }

    public void saveSelected() {

        if (errorCheck()) {
            return;
        }

        if (getPatientTabId().equals("tabNewPt")) {
            savePatient();
            getCurrent().setPatient(getNewPatient());
        }

        saveGuardian();
        bhtText = getInwardBean().getBhtText(getCurrent().getAdmissionType());
        getCurrent().setBhtNo(getBhtText());

        //  getCurrent().setBhtNo(bhtText);
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            //      getCurrent().setDateOfAdmission(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getFacade().create(getCurrent());
            UtilityController.addSuccessMessage("Patient Admitted Succesfully");
        }

        PatientRoom currentPatientRoom = getInwardBean().savePatientRoom(getPatientRoom(), null, getPatientRoom().getRoomFacilityCharge(), getCurrent(), getCurrent().getDateOfAdmission(), getSessionController().getLoggedUser());
        getCurrent().setCurrentPatientRoom(currentPatientRoom);
        getFacade().edit(getCurrent());

        double appointmentFee = 0;
        if (getAppointmentBill() != null) {
            appointmentFee = getAppointmentBill().getTotal();
            updateAppointment();
            updateAppointmentBill();
        }

        if (appointmentFee != 0) {
            System.err.println("Appoint ");
            getInwardPaymentController().getCurrent().setPaymentMethod(getCurrent().getPaymentMethod());
            getInwardPaymentController().getCurrent().setPatientEncounter(current);
            getInwardPaymentController().getCurrent().setTotal(appointmentFee);
            getInwardPaymentController().pay();
            getInwardPaymentController().makeNull();
        }

        printPreview = true;

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
            UtilityController.addErrorMessage("Please select the mother encounter");
            return;
        }
        if (parentAdmission.getParentEncounter() != null) {
            UtilityController.addErrorMessage("This mother encounter already has another Mother ENcounter, which is NOT possible. Select some other encounter");
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
        if (items == null) {
            String temSql;
            temSql = "SELECT i FROM Admission i where i.retired=false and i.discharged=false order by i.bhtNo";
            items = getFacade().findBySQL(temSql);
            if (items == null) {
                items = new ArrayList<>();
            }
        }

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
            patientList = getPatientFacade().findBySQL(temSql);
        }
        return patientList;
    }

    public void setPatientList(List<Patient> patientList) {
        this.patientList = patientList;
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
        ageText = getNewPatient().getAge();
        return ageText;
    }

    public void setAgeText(String ageText) {
        this.ageText = ageText;
        getNewPatient().getPerson().setDob(getCommonFunctions().guessDob(ageText));
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public PatientRoomFacade getPatientRoomFacade() {
        return patientRoomFacade;
    }

    public void setPatientRoomFacade(PatientRoomFacade patientRoomFacade) {
        this.patientRoomFacade = patientRoomFacade;
    }

    public void bhtNumberCalculation() {
        if (getCurrent() == null || getCurrent().getAdmissionType() == null) {
//            UtilityController.addErrorMessage("Please Set Admission Type DayCase/Admission For this this Admission ");
            return;
        }

        if (getCurrent().getAdmissionType().getAdmissionTypeEnum() == null) {
            UtilityController.addErrorMessage("Please Set Admission Type DayCase/Admission For this this Admission ");
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

    public Patient getNewPatient() {
        if (newPatient == null) {
            Person p = new Person();
            newPatient = new Patient();
            newPatient.setPerson(p);
        }
        return newPatient;
    }

    public void setNewPatient(Patient newPatient) {
        this.newPatient = newPatient;
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

    /**
     *
     */
    @FacesConverter("admis")
    public static class AdmissionConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AdmissionController controller = (AdmissionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "admissionController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
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

    @FacesConverter(forClass = Admission.class)
    public static class AdmissionControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AdmissionController controller = (AdmissionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "admissionController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
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
