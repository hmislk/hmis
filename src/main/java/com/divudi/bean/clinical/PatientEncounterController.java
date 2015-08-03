/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.clinical;

import com.divudi.bean.common.BillController;
import com.divudi.bean.common.CommonFunctionsController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.bean.pharmacy.PharmacySaleController;
import com.divudi.data.BillType;
import com.divudi.data.SymanticType;
import com.divudi.entity.Bill;
import com.divudi.entity.Department;
import com.divudi.entity.Doctor;
import com.divudi.entity.Institution;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.clinical.ClinicalFindingItem;
import com.divudi.entity.clinical.ClinicalFindingValue;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.facade.BillFacade;
import com.divudi.facade.ClinicalFindingItemFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class PatientEncounterController implements Serializable {

    @Inject
    private CommonFunctionsController commonFunctions;
    List<String> completeStrings = null;
    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private PatientEncounterFacade ejbFacade;
    @EJB
    ClinicalFindingItemFacade clinicalFindingItemFacade;
    @EJB
    PersonFacade personFacade;
    @EJB
    PatientFacade patientFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    PatientInvestigationFacade piFacade;
    //
    List<PatientEncounter> selectedItems;
    private PatientEncounter current;
    private List<PatientEncounter> items = null;
    List<PatientEncounter> currentPatientEncounters;
    List<Bill> currentBills;
    List<PatientInvestigation> currentPatientInvestigations;
    String selectText = "";

    ClinicalFindingItem diagnosis;
    String diagnosisComments;
    Investigation investigation;

    ClinicalFindingValue removingCfv;
    @Inject
    PharmacySaleController pharmacySaleController;
    @Inject
    BillController billController;

    PatientEncounter encounterToDisplay;
    PatientEncounter startedEncounter;

    Date fromDate;
    Date toDate;
    Institution institution;
    Department department;
    Doctor doctor;

    public List<String> completeClinicalComments(String qry) {
        if (current == null || current.getComments() == null) {
            completeRx(qry);
            return completeStrings;
        }
        String c = current.getComments();
        int intHx = c.lastIndexOf(getSessionController().getUserPreference().getAbbreviationForHistory());
        int intEx = c.lastIndexOf(getSessionController().getUserPreference().getAbbreviationForExamination());
        int intIx = c.lastIndexOf(getSessionController().getUserPreference().getAbbreviationForInvestigations());
        int intRx = c.lastIndexOf(getSessionController().getUserPreference().getAbbreviationForTreatments());
        int intMx = c.lastIndexOf(getSessionController().getUserPreference().getAbbreviationForManagement());

        //   //System.out.println("intHx = " + intHx);
        //   //System.out.println("intEx = " + intEx);
        //   //System.out.println("intIx = " + intIx);
        //   //System.out.println("intRx = " + intRx);
        //   //System.out.println("intMx = " + intMx);
        ClinicalField lastField = ClinicalField.History;
        int lastValue = intHx;

        if (intEx > lastValue) {
            lastField = ClinicalField.Examination;
            lastValue = intEx;
        }

        if (intIx > lastValue) {
            lastField = ClinicalField.Investigations;
            lastValue = intIx;
        }

        if (intRx > lastValue) {
            lastField = ClinicalField.Treatments;
            lastValue = intRx;
        }

        if (intMx > lastValue) {
            lastField = ClinicalField.Management;
            lastValue = intMx;
        }

        switch (lastField) {
            case History:
                completeHx(qry);
                break;
            case Examination:
                completeEx(qry);
                break;
            case Investigations:
                 completeIx(qry);
                break;
            case Treatments:
                 completeRx(qry);
                break;
            default:
                completeStrings = completeItem(qry);
        }

        return completeStrings;
    }

    public List<String> completeItem(String qry) {
        //   //System.out.println("complete item");
        if (qry == null) {
            qry = "";
        }
        String sql;
        sql = "select c.name from Item c where c.retired=false and "
                + "upper(c.name) like :q "
                + "order by c.name";
        Map tmpMap = new HashMap();
        tmpMap.put("q", qry.toUpperCase() + "%");
        return getFacade().findString(sql, tmpMap);
    }

    public void completeHx(String qry) {
        //   //System.out.println("complete hx");
        if (qry == null) {
            qry = "";
        }
        String sql;
        sql = "select c.name from Item c where c.retired=false and "
                + "type(c)= :cls and "
                + "c.symanticType=:st and "
                + "upper(c.name) like :q "
                + "order by c.name";
        Map tmpMap = new HashMap();
        tmpMap.put("cls", ClinicalFindingItem.class);
        tmpMap.put("st", SymanticType.Symptom);
        tmpMap.put("q", qry.toUpperCase() + "%");
        completeStrings = getFacade().findString(sql, tmpMap, TemporalType.TIMESTAMP);
    }

    public void completeEx(String qry) {

        //   //System.out.println("complete ex");
        if (qry == null) {
            qry = "";
        }
        String sql;
        sql = "select c.name from Item c where c.retired=false and "
                + "type(c)= :cls and "
                + "c.symanticType=:st and "
                + "upper(c.name) like :q "
                + "order by c.name";
        Map tmpMap = new HashMap();
        tmpMap.put("cls", ClinicalFindingItem.class);
        tmpMap.put("st", SymanticType.Sign);
        tmpMap.put("q", qry.toUpperCase() + "%");
        completeStrings = getFacade().findString(sql, tmpMap, TemporalType.TIMESTAMP);
    }

    public void completeIx(String qry) {
        //   //System.out.println("complete Ix");
        if (qry == null) {
            qry = "";
        }
        String sql;
        sql = "select c.name from Investigation c where c.retired=false and "
                + "upper(c.name) like :q "
                + "order by c.name";
        Map tmpMap = new HashMap();
        tmpMap.put("q", qry.toUpperCase() + "%");
        completeStrings =  getFacade().findString(sql, tmpMap, TemporalType.TIMESTAMP);
    }

    public void completeRx(String qry) {
        //   //System.out.println("complete rx");
        //   //System.out.println("qry = " + qry);
        if (qry == null) {
            qry = "";
        }
        String sql;
        sql = "select c.name from Item c where c.retired=false and "
                + "(type(c)= :amp or type(c)= :vmp or "
                + "type(c)= :vtm or "
                + "(type(c)= :ce and c.symanticType=:st)) "
                + "and upper(c.name) like :q "
                + "order by c.name";
        ////System.out.println(sql);
        Map tmpMap = new HashMap();
        tmpMap.put("amp", Amp.class);
        tmpMap.put("vmp", Vmp.class);
        tmpMap.put("vtm", Vmp.class);
        tmpMap.put("ce", ClinicalFindingItem.class);
        tmpMap.put("st", SymanticType.Pharmacologic_Substance);
        tmpMap.put("q", qry.toUpperCase() + "%");
        completeStrings = getFacade().findString(sql, tmpMap);
    }

    public void listAllEncounters() {
        String jpql;
        Map m = new HashMap();
        jpql = "select pe from PatientEncounter pe where pe.retired=false and pe.createdAt between :fd and :td ";
        m.put("fd", fromDate);
        m.put("td", toDate);
        if (institution != null) {
            jpql = jpql + " and pe.department.institution=:ins ";
            m.put("ins", institution);

        } else if (department != null) {
            jpql = jpql + " and pe.department=:dep ";
            m.put("dep", department);
        }
        if (doctor != null) {
            jpql = jpql + " and pe.opdDoctor=:doc ";
            m.put("doc", doctor);
        }
        //System.out.println("1. m = " + m);
        //System.out.println("2. sql = " + jpql);
        items = getFacade().findBySQL(jpql, m, TemporalType.TIMESTAMP);
        //System.out.println("3. items = " + items);
    }

    public void listPeriodEncounters() {
        String jpql;
        Map m = new HashMap();
        jpql = "select pe from PatientEncounter pe where pe.retired=false and pe.createdAt between :fd and :td ";
        m.put("fd", fromDate);
        m.put("td", toDate);
        if (institution != null) {
            jpql = jpql + " and pe.department.institution=:ins ";
            m.put("ins", institution);

        } else if (department != null) {
            jpql = jpql + " and pe.department=:dep ";
            m.put("dep", department);
        }
        if (doctor != null) {
            jpql = jpql + " and pe.opdDoctor=:doc ";
            m.put("doc", doctor);
        }
        //   //System.out.println("m = " + m);
        //   //System.out.println("sql = " + jpql);
        items = getFacade().findBySQL(jpql, m);

    }

    public void addDx() {
        if (diagnosis == null) {
            UtilityController.addErrorMessage("Please select a diagnosis");
            return;
        }
        if (current == null) {
            UtilityController.addErrorMessage("Please select a visit");
            return;
        }
        ClinicalFindingValue dx = new ClinicalFindingValue();
        dx.setItemValue(diagnosis);
        dx.setClinicalFindingItem(diagnosis);
        dx.setEncounter(current);
        dx.setPerson(current.getPatient().getPerson());
        dx.setStringValue(diagnosis.getName());
        dx.setLobValue(diagnosisComments);
        current.getClinicalFindingValues().add(dx);
        getFacade().edit(current);
        diagnosis = new ClinicalFindingItem();
        diagnosisComments = "";
        UtilityController.addSuccessMessage("Diagnosis added");
        setCurrent(getFacade().find(current.getId()));
    }

    public List<PatientEncounter> getCurrentPatientEncounters() {
        return currentPatientEncounters;
    }

    public void fillCurrentPatientEncounters() {
        Map m = new HashMap();
        m.put("p", current.getPatient());
        m.put("pe", current);
        String sql;
        sql = "Select e from PatientEncounter e where e.patient=:p and e!=:pe order by e.id desc";
        currentPatientEncounters = getFacade().findBySQL(sql, m);

        fillPatientBills(current.getPatient());
        fillPatientInvestigations(current.getPatient());
    }

    public void fillCurrentPatientLists(Patient patient) {
        fillPatientEncounters(patient);
        fillPatientBills(patient);
        fillPatientInvestigations(patient);
    }

    public void fillPatientBills(Patient patient) {
        Map m = new HashMap();
        m.put("p", patient);
        m.put("bt1", BillType.OpdBill);
        m.put("bt2", BillType.PharmacySale);
        String sql;
        Bill b = new Bill();
        b.getBillType();
        sql = "Select e from Bill e where e.patient=:p and (e.billType=:bt1 or e.billType=:bt2 ) order by e.id desc";
        //System.out.println("sql = " + sql);
        //System.out.println("m = " + m);
        currentBills = getBillFacade().findBySQL(sql, m);
    }

    public void fillPatientInvestigations(Patient patient) {
        Map m = new HashMap();
        m.put("p", patient);
        String sql;
        sql = "Select e from PatientInvestigation e where e.patient=:p order by e.id desc";
        //System.out.println("sql = " + sql);
        //System.out.println("m = " + m);
        currentPatientInvestigations = getPiFacade().findBySQL(sql, m);
    }

    public void fillPatientEncounters(Patient patient) {
        //   //System.out.println("fill current patient encounters");
        Map m = new HashMap();
        m.put("p", patient);
        String sql;
        sql = "Select e from PatientEncounter e where e.patient=:p order by e.id desc";
        currentPatientEncounters = getFacade().findBySQL(sql, m);
    }

    public void removeCfv() {
        if (current == null) {
            UtilityController.addErrorMessage("No Patient Encounter");
            return;
        }
        if (removingCfv == null) {
            UtilityController.addErrorMessage("No Finding selected to remove");
            return;
        }
        current.getClinicalFindingValues().remove(removingCfv);
        saveSelected();
        UtilityController.addSuccessMessage("Removed");
    }

    public ClinicalFindingItem getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(ClinicalFindingItem diagnosis) {
        this.diagnosis = diagnosis;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        this.investigation = investigation;
    }

//    public List<PatientEncounter> getSelectedItems() {
//        selectedItems = getFacade().findBySQL("select c from PatientEncounter c where c.retired=false and i.institutionType = com.divudi.data.PatientEncounterType.Agency and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
//        return selectedItems;
//    }
    public void prepareAdd() {
        setCurrent(new PatientEncounter());
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        current.setDateTime(new Date());
        current.setDepartment(sessionController.getDepartment());
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {

            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        UtilityController.addSuccessMessage("Saved");
    }

    public void updateComments() {
        //   //System.out.println("updating comments");
        //   //System.out.println("current.getComments() = " + current.getComments());
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
        }
    }

    public void updatePerson() {
        //   //System.out.println("updating person");
        if (current == null) {
            //   //System.out.println("current = " + current);
            return;
        }
        if (current.getPatient() == null) {
            //   //System.out.println("current.getPatient()  = " + current.getPatient());
            return;
        }
        if (current.getPatient().getPerson() == null) {
            //   //System.out.println("current.getPatient().getPerson() = " + current.getPatient().getPerson());
            return;
        }
        getPersonFacade().edit(current.getPatient().getPerson());
        getPatientFacade().edit(current.getPatient());
    }

    public String issueItems() {
        if (current == null) {
            return "";
        }
        getPharmacySaleController().setSearchedPatient(current.getPatient());
        getPharmacySaleController().setPatientSearchTab(1);
        getPharmacySaleController().setOpdEncounterComments(current.getComments());
        getPharmacySaleController().setFromOpdEncounter(true);
        getPharmacySaleController().setPatientTabId("tabSearchPt");
//        getPharmacySaleController().getBill().setPatientEncounter(current);
//        getPharmacySaleController().getBill().setPatient(current.getPatient());
        return "/clinical/clinical_pharmacy_sale";
    }

    public String issueServices() {
        if (current == null) {
            return "";
        }
        getBillController().prepareNewBill();
        getBillController().setSearchedPatient(current.getPatient());
        getBillController().setFromOpdEncounter(true);
        getBillController().setOpdEncounterComments(current.getComments());
        getBillController().setPatientSearchTab(1);
        getBillController().setPatientTabId("tabSearchPt");
        getBillController().setReferredBy(doctor);
        //        getPharmacySaleController().getBill().setPatientEncounter(current);
        //        getPharmacySaleController().getBill().setPatient(current.getPatient());
        return "/opd_bill";
    }

    public PatientEncounter getEncounterToDisplay() {
        return encounterToDisplay;
    }

    public void setEncounterToDisplay(PatientEncounter encounterToDisplay) {
        this.encounterToDisplay = encounterToDisplay;
    }

    public PatientEncounter getStartedEncounter() {
        return startedEncounter;
    }

    public void setStartedEncounter(PatientEncounter startedEncounter) {
        this.startedEncounter = startedEncounter;
    }

    public String prepareToDisplayPastVisit() {
        if (current == null) {
            JsfUtil.addErrorMessage("No visit");
            return "";
        }
        if (encounterToDisplay == null) {
            JsfUtil.addErrorMessage("Select Visit");
            return "";
        }
        setCurrent(encounterToDisplay);
        return "";
    }

    public void backToStartingEncounter() {
        if (startedEncounter == null) {
            JsfUtil.addErrorMessage("No visit");
            return;
        }
        setCurrent(startedEncounter);
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public PatientEncounterFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(PatientEncounterFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public PatientEncounterController() {
    }

    public PatientEncounter getCurrent() {
        return current;
    }

    public void setCurrent(PatientEncounter current) {
        if (this.current == current) {
            return;
        }
        this.current = current;
        if (this != null) {
            fillCurrentPatientLists(current.getPatient());
        }
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private PatientEncounterFacade getFacade() {
        return ejbFacade;
    }

    public List<PatientEncounter> getItems() {
        return items;
    }

    public String getDiagnosisComments() {
        return diagnosisComments;
    }

    public void setDiagnosisComments(String diagnosisComments) {
        this.diagnosisComments = diagnosisComments;
    }

    public ClinicalFindingValue getRemovingCfv() {
        return removingCfv;
    }

    public void setRemovingCfv(ClinicalFindingValue removingCfv) {
        this.removingCfv = removingCfv;
    }

    public PharmacySaleController getPharmacySaleController() {
        return pharmacySaleController;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = new Date();
            fromDate = getCommonFunctions().getStartOfDay(fromDate);
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = new Date();
            toDate = getCommonFunctions().getEndOfDay(toDate);
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public ClinicalFindingItemFacade getClinicalFindingItemFacade() {
        return clinicalFindingItemFacade;
    }

    public BillController getBillController() {
        return billController;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public CommonFunctionsController getCommonFunctions() {
        return commonFunctions;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public PatientInvestigationFacade getPiFacade() {
        return piFacade;
    }

    public List<Bill> getCurrentBills() {
        return currentBills;
    }

    public List<PatientInvestigation> getCurrentPatientInvestigations() {
        return currentPatientInvestigations;
    }

    public List<String> getCompleteStrings() {
        return completeStrings;
    }

    public void setCompleteStrings(List<String> completeStrings) {
        this.completeStrings = completeStrings;
    }

}

enum ClinicalField {

    History,
    Examination,
    Investigations,
    Treatments,
    Management,
}
