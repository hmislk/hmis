/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.clinical;

import com.divudi.bean.common.BillController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.CommonFunctionsController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.bean.pharmacy.PharmacySaleController;
import com.divudi.data.BillType;
import com.divudi.data.SymanticType;
import com.divudi.data.clinical.ClinicalFindingValueType;
import com.divudi.data.clinical.ItemUsageType;
import com.divudi.entity.Bill;
import com.divudi.entity.Department;
import com.divudi.entity.Doctor;
import com.divudi.entity.Institution;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Person;
import com.divudi.entity.clinical.ClinicalFindingItem;
import com.divudi.entity.clinical.ClinicalFindingValue;
import com.divudi.entity.clinical.ItemUsage;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.facade.BillFacade;
import com.divudi.facade.ClinicalFindingItemFacade;
import com.divudi.facade.ClinicalFindingValueFacade;
import com.divudi.facade.ItemUsageFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class PatientEncounterController implements Serializable {

    /**
     * EJBs
     */
    @EJB
    private PatientEncounterFacade ejbFacade;
    @EJB
    ClinicalFindingItemFacade clinicalFindingItemFacade;
    @EJB
    ClinicalFindingValueFacade clinicalFindingValueFacade;
    @EJB
    PersonFacade personFacade;
    @EJB
    PatientFacade patientFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    PatientInvestigationFacade piFacade;
    @EJB
    ItemUsageFacade itemUsageFacade;
    /**
     * Controllers
     */
    @Inject
    private CommonFunctionsController commonFunctions;
    @Inject
    SessionController sessionController;
    @Inject
    PharmacySaleController pharmacySaleController;
    @Inject
    BillController billController;
    @Inject
    CommonController commonController;

    /**
     * Properties
     */
    List<String> completeStrings = null;
    private static final long serialVersionUID = 1L;
    //
    private List<PatientEncounter> selectedItems;
    private PatientEncounter current;
    private List<PatientEncounter> items = null;
    List<PatientEncounter> encounters;

    private Patient patient;

    private ClinicalFindingValue patientAllergy;
    private ClinicalFindingValue patientMedicine;
    private ClinicalFindingValue patientImage;
    private ClinicalFindingValue patientDiagnosis;
    private ClinicalFindingValue patientDiagnosticImage;
    private ClinicalFindingValue removingClinicalFindingValue;

    private List<ClinicalFindingValue> patientAllergies;
    private List<ClinicalFindingValue> patientMedicines;
    private List<ClinicalFindingValue> patientImages;
    private List<ClinicalFindingValue> patientDiagnoses;
    private List<ClinicalFindingValue> patientDiagnosticImages;

    private List<ItemUsage> currentEncounterMedicines;
    private List<ItemUsage> currentEncounterDiagnosis;
    List<Bill> opdBills;
    private List<Bill> pharmacyBills;
    List<Bill> channelBills;
    List<PatientInvestigation> investigations;
    String selectText = "";

    ClinicalFindingItem diagnosis;
    String diagnosisComments;
    Investigation investigation;

    ClinicalFindingValue removingCfv;

    PatientEncounter encounterToDisplay;
    PatientEncounter startedEncounter;

    Date fromDate;
    Date toDate;
    Institution institution;
    Department department;
    Doctor doctor;

    public void listInstitutionEncounters() {
        System.out.println("listInstitutionEncounters = " );
        String jpql = "select e "
                + " from PatientEncounter e "
                + " where e.retired=:ret "
                + " and e.institution=:ins "
                + " and e.encounterDate between :fd and :td "
                + "order by e.id";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("ins", sessionController.getInstitution());
        m.put("fd", fromDate);
        m.put("td", toDate);
        System.out.println("m = " + m);
        System.out.println("jpql = " + jpql);
        items = getFacade().findByJpql(jpql, m);
        System.out.println("items = " + items.size());
    }

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

        //   ////// // System.out.println("intHx = " + intHx);
        //   ////// // System.out.println("intEx = " + intEx);
        //   ////// // System.out.println("intIx = " + intIx);
        //   ////// // System.out.println("intRx = " + intRx);
        //   ////// // System.out.println("intMx = " + intMx);
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
        //   ////// // System.out.println("complete item");
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
        //   ////// // System.out.println("complete hx");
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

        //   ////// // System.out.println("complete ex");
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
        //   ////// // System.out.println("complete Ix");
        if (qry == null) {
            qry = "";
        }
        String sql;
        sql = "select c.name from Investigation c where c.retired=false and "
                + "upper(c.name) like :q "
                + "order by c.name";
        Map tmpMap = new HashMap();
        tmpMap.put("q", qry.toUpperCase() + "%");
        completeStrings = getFacade().findString(sql, tmpMap, TemporalType.TIMESTAMP);
    }

    public void completeRx(String qry) {
        //   ////// // System.out.println("complete rx");
        //   ////// // System.out.println("qry = " + qry);
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
        //////// // System.out.println(sql);
        Map tmpMap = new HashMap();
        tmpMap.put("amp", Amp.class);
        tmpMap.put("vmp", Vmp.class);
        tmpMap.put("vtm", Vmp.class);
        tmpMap.put("ce", ClinicalFindingItem.class);
        tmpMap.put("st", SymanticType.Pharmacologic_Substance);
        tmpMap.put("q", qry.toUpperCase() + "%");
        completeStrings = getFacade().findString(sql, tmpMap);
    }
    
    public String navigateToListInstitutionEncounters() {
        items=null;
        return "/emr/reports/visits";
    }

    public String listAllEncounters() {
        Date startTime = new Date();

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
        ////// // System.out.println("1. m = " + m);
        ////// // System.out.println("2. sql = " + jpql);
        items = getFacade().findBySQL(jpql, m, TemporalType.TIMESTAMP);
        ////// // System.out.println("3. items = " + items);

        commonController.printReportDetails(fromDate, toDate, startTime, "EHR/Reports/All visits/(/faces/clinical/clinical_reports_all_opd_visits.xhtml)");
        return "/clinical/clinical_reports_all_opd_visits?faces-redirect=true";
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
        //   ////// // System.out.println("m = " + m);
        //   ////// // System.out.println("sql = " + jpql);
        items = getFacade().findByJpql(jpql, m);

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

    public List<PatientEncounter> getEncounters() {
        return encounters;
    }

    public List<PatientEncounter> fillCurrentPatientEncounters(PatientEncounter pe) {
        Map m = new HashMap();
        m.put("p", pe.getPatient());
        m.put("pe", pe);
        String sql;
        sql = "Select e from PatientEncounter e where e.patient=:p and e!=:pe order by e.id desc";
        return getFacade().findByJpql(sql, m);
    }

    public List<ClinicalFindingValue> fillPatientAllergies(Patient patient) {
        List<ClinicalFindingValueType> clinicalFindingValueTypes = new ArrayList<>();
        clinicalFindingValueTypes.add(ClinicalFindingValueType.PatientAllergy);
        return fillCurrentPatientClinicalFindingValues(patient, clinicalFindingValueTypes);
    }

    public List<ClinicalFindingValue> fillPatientDiagnoses(Patient patient) {
        List<ClinicalFindingValueType> clinicalFindingValueTypes = new ArrayList<>();
        clinicalFindingValueTypes.add(ClinicalFindingValueType.PatientDiagnosis);
        return fillCurrentPatientClinicalFindingValues(patient, clinicalFindingValueTypes);
    }

    public List<ClinicalFindingValue> fillPatientMedicines(Patient patient) {
        List<ClinicalFindingValueType> clinicalFindingValueTypes = new ArrayList<>();
        clinicalFindingValueTypes.add(ClinicalFindingValueType.PatientMedicine);
        return fillCurrentPatientClinicalFindingValues(patient, clinicalFindingValueTypes);
    }

    public List<ClinicalFindingValue> fillPatientImages(Patient patient) {
        List<ClinicalFindingValueType> clinicalFindingValueTypes = new ArrayList<>();
        clinicalFindingValueTypes.add(ClinicalFindingValueType.PatientImage);
        return fillCurrentPatientClinicalFindingValues(patient, clinicalFindingValueTypes);
    }

    public List<ClinicalFindingValue> fillPatientDiadnosticImages(Patient patient) {
        List<ClinicalFindingValueType> clinicalFindingValueTypes = new ArrayList<>();
        clinicalFindingValueTypes.add(ClinicalFindingValueType.PatientDiagnosticImage);
        return fillCurrentPatientClinicalFindingValues(patient, clinicalFindingValueTypes);
    }

    public List<ClinicalFindingValue> fillCurrentPatientClinicalFindingValues(Patient patient, List<ClinicalFindingValueType> clinicalFindingValueTypes) {
        Map m = new HashMap();
        m.put("p", patient);
        m.put("ts", clinicalFindingValueTypes);
        m.put("ret", false);
        String sql;
        ClinicalFindingValue e = new ClinicalFindingValue();
        e.getPatient();
        sql = "Select e "
                + " from ClinicalFindingValue e "
                + " where e.patient=:p "
                + " and e.retired=:ret "
                + " and e.clinicalFindingValueType in :ts "
                + " order by e.orderNo";
        return clinicalFindingValueFacade.findByJpql(sql, m);
    }

    public void fillCurrentPatientLists(Patient patient) {
        encounters = fillPatientEncounters(patient);

        opdBills = fillPatientOpdBills(patient);
        pharmacyBills = fillPatientPharmacyBills(patient);
        channelBills = fillPatientChannelBills(patient);

        investigations = fillPatientInvestigations(patient);

        patientAllergies = fillPatientAllergies(patient);
        patientDiagnoses = fillPatientDiagnoses(patient);
        patientImages = fillPatientImages(patient);
        patientDiagnosticImages = fillPatientDiadnosticImages(patient);
        patientMedicines = fillPatientMedicines(patient);

//        currentEncounterMedicines = fillCurrentEncounterMedicines();
//        currentEncounterDiagnosis = fillCurrentEncounterDiagnosis();
    }

    public void removePatientAllergy() {
        if (getRemovingClinicalFindingValue() == null) {
            JsfUtil.addErrorMessage("Select Allergy");
            return;
        }
        getRemovingClinicalFindingValue().setRetired(true);
        getPatientAllergies().remove(getRemovingClinicalFindingValue());
        setRemovingClinicalFindingValue(null);
    }

    public void removePatientMedicine() {
        if (getRemovingClinicalFindingValue() == null) {
            JsfUtil.addErrorMessage("Select Allergy");
            return;
        }
        getRemovingClinicalFindingValue().setRetired(true);
        getPatientMedicines().remove(getRemovingClinicalFindingValue());
        setRemovingClinicalFindingValue(null);
    }

    public void removePatientDiagnosis() {
        if (getRemovingClinicalFindingValue() == null) {
            JsfUtil.addErrorMessage("Select Allergy");
            return;
        }
        getRemovingClinicalFindingValue().setRetired(true);
        getPatientDiagnoses().remove(getRemovingClinicalFindingValue());
        setRemovingClinicalFindingValue(null);
    }

    public void addPatientAllergy() {
        if (getPatientAllergy().getItemValue() == null) {
            JsfUtil.addErrorMessage("Select Allergy");
            return;
        }
        getPatientAllergy().setPatient(patient);
        getPatientAllergy().setClinicalFindingValueType(ClinicalFindingValueType.PatientAllergy);
        clinicalFindingValueFacade.create(getPatientAllergy());
        getPatientAllergies().add(getPatientAllergy());
        setPatientAllergy(null);
        JsfUtil.addSuccessMessage("Added");
    }

    public void addPatientDiagnosis() {
        if (getPatientDiagnosis().getItemValue() == null) {
            JsfUtil.addErrorMessage("Select Allergy");
            return;
        }
        getPatientDiagnosis().setPatient(patient);
        getPatientDiagnosis().setClinicalFindingValueType(ClinicalFindingValueType.PatientAllergy);
        clinicalFindingValueFacade.create(getPatientDiagnosis());
        getPatientDiagnoses().add(getPatientDiagnosis());
        setPatientDiagnosis(null);
        JsfUtil.addSuccessMessage("Added");
    }

    public void addPatientMedicine() {
        if (getPatientMedicine().getItemValue() == null) {
            JsfUtil.addErrorMessage("Select Allergy");
            return;
        }
        getPatientMedicine().setPatient(patient);
        getPatientMedicine().setClinicalFindingValueType(ClinicalFindingValueType.PatientAllergy);
        clinicalFindingValueFacade.create(getPatientMedicine());
        getPatientMedicines().add(getPatientMedicine());
        setPatientMedicine(null);
        JsfUtil.addSuccessMessage("Added");
    }

    public List<ItemUsage> fillCurrentEncounterMedicines() {
        Map m = new HashMap();
        m.put("pe", getCurrent());
        m.put("t", ItemUsageType.EncounterItems);
        String sql;
        sql = "Select e "
                + " from ItemUsage e "
                + " where e.patientEncounter=:pe "
                + " and e.type=:t "
                + " order by e.id desc";
        return itemUsageFacade.findByJpql(sql, m);
    }

    public List<ItemUsage> fillCurrentEncounterDiagnosis() {
        Map m = new HashMap();
        m.put("pe", getCurrent());
        m.put("t", ItemUsageType.EncounterDiagnosis);
        String sql;
        sql = "Select e "
                + " from ItemUsage e "
                + " where e.patientEncounter=:pe "
                + " and e.type=:t "
                + " order by e.id desc";
        return itemUsageFacade.findByJpql(sql, m);
    }

    public List<Bill> fillPatientBills(Patient patient, List<BillType> bts, Integer count) {
        Map m = new HashMap();
        m.put("p", patient);
        m.put("bts", bts);
        m.put("ret", false);
        String sql;
        Bill b = new Bill();
        b.getBillType();
        sql = "Select e "
                + " from Bill e "
                + " where e.patient=:p "
                + " and e.billType in :bts"
                + " and e.retired=:ret "
                + " order by e.id desc";
        if (count != null) {
            return getBillFacade().findBySQL(sql, m, count);
        } else {
            return getBillFacade().findByJpql(sql, m);
        }
    }

    public List<Bill> fillPatientPharmacyBills(Patient patient) {
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.PharmacySale);
        return fillPatientBills(patient, bts, 10);
    }

    public List<Bill> fillPatientOpdBills(Patient patient) {
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.OpdBill);
        return fillPatientBills(patient, bts, 10);
    }

    public List<Bill> fillPatientChannellingBills(Patient patient) {
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.Channel);
        return fillPatientBills(patient, bts, 10);
    }

    public List<Bill> fillPatientChannelBills(Patient patient) {
        Map m = new HashMap();
        m.put("p", patient);
        BillType[] bts = {BillType.ChannelCash, BillType.ChannelAgent, BillType.ChannelStaff, BillType.ChannelOnCall};
        List<BillType> billTypes = Arrays.asList(bts);
        m.put("bts", billTypes);
        String sql;
        sql = "Select b from Bill b where b.patient=:p and b.billType in :bts order by b.id desc";
        return getBillFacade().findByJpql(sql, m);
    }

    public List<PatientInvestigation> fillPatientInvestigations(Patient patient) {
        Map m = new HashMap();
        m.put("p", patient);
        m.put("ret", false);
        String sql;
        sql = "Select e "
                + " from PatientInvestigation e "
                + " where e.patient=:p "
                + " and e.retired=:ret "
                + "order by e.id desc";
        return getPiFacade().findByJpql(sql, m);
    }

    public List<PatientEncounter> fillPatientEncounters(Patient patient, Integer count) {
        Map m = new HashMap();
        m.put("p", patient);
        String sql;
        sql = "Select e from PatientEncounter e where e.patient=:p order by e.id desc";
        if (count != null) {
            return getFacade().findBySQL(sql, m, count);
        } else {
            return getFacade().findByJpql(sql, m);
        }
    }

    public List<PatientEncounter> fillPatientEncounters(Patient patient) {
        return fillPatientEncounters(patient, null);
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
//        selectedItems = getFacade().findByJpql("select c from PatientEncounter c where c.retired=false and i.institutionType = com.divudi.data.PatientEncounterType.Agency and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
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
        current.setEncounterDate(new Date());
        current.setDepartment(sessionController.getDepartment());
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {

            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        UtilityController.addSuccessMessage("Saved");
    }

    public void saveEncounter(PatientEncounter pe) {
        if (pe.getId() != null && pe.getId() > 0) {
            getFacade().edit(pe);
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            getFacade().create(pe);
        }
    }

    public void updateComments() {
        //   ////// // System.out.println("updating comments");
        //   ////// // System.out.println("current.getComments() = " + current.getComments());
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
        }
    }

    public void updatePerson() {
        //   ////// // System.out.println("updating person");
        if (current == null) {
            //   ////// // System.out.println("current = " + current);
            return;
        }
        if (current.getPatient() == null) {
            //   ////// // System.out.println("current.getPatient()  = " + current.getPatient());
            return;
        }
        if (current.getPatient().getPerson() == null) {
            //   ////// // System.out.println("current.getPatient().getPerson() = " + current.getPatient().getPerson());
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
        if (current == null) {
            current = new PatientEncounter();
            Patient pt = new Patient();
            Person p = new Person();
            pt.setPerson(p);
            current.setPatient(pt);
        }
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
            current.setRetiredAt(new Date());
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

    public List<Bill> getOpdBills() {
        return opdBills;
    }

    public List<PatientInvestigation> getInvestigations() {
        return investigations;
    }

    public List<String> getCompleteStrings() {
        return completeStrings;
    }

    public void setCompleteStrings(List<String> completeStrings) {
        this.completeStrings = completeStrings;
    }

    public List<Bill> getChannelBills() {
        return channelBills;
    }

    public void setChannelBills(List<Bill> channelBills) {
        this.channelBills = channelBills;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public List<PatientEncounter> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectedItems(List<PatientEncounter> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public List<Bill> getPharmacyBills() {
        return pharmacyBills;
    }

    public void setPharmacyBills(List<Bill> pharmacyBills) {
        this.pharmacyBills = pharmacyBills;
    }

    public List<ItemUsage> getCurrentEncounterMedicines() {
        return currentEncounterMedicines;
    }

    public void setCurrentEncounterMedicines(List<ItemUsage> currentEncounterMedicines) {
        this.currentEncounterMedicines = currentEncounterMedicines;
    }

    public List<ItemUsage> getCurrentEncounterDiagnosis() {
        return currentEncounterDiagnosis;
    }

    public void setCurrentEncounterDiagnosis(List<ItemUsage> currentEncounterDiagnosis) {
        this.currentEncounterDiagnosis = currentEncounterDiagnosis;
    }

    public List<ClinicalFindingValue> getPatientAllergies() {
        return patientAllergies;
    }

    public void setPatientAllergies(List<ClinicalFindingValue> patientAllergies) {
        this.patientAllergies = patientAllergies;
    }

    public List<ClinicalFindingValue> getPatientMedicines() {
        return patientMedicines;
    }

    public void setPatientMedicines(List<ClinicalFindingValue> patientMedicines) {
        this.patientMedicines = patientMedicines;
    }

    public List<ClinicalFindingValue> getPatientImages() {
        return patientImages;
    }

    public void setPatientImages(List<ClinicalFindingValue> patientImages) {
        this.patientImages = patientImages;
    }

    public List<ClinicalFindingValue> getPatientDiagnoses() {
        return patientDiagnoses;
    }

    public void setPatientDiagnoses(List<ClinicalFindingValue> patientDiagnoses) {
        this.patientDiagnoses = patientDiagnoses;
    }

    public List<ClinicalFindingValue> getPatientDiagnosticImages() {
        return patientDiagnosticImages;
    }

    public void setPatientDiagnosticImages(List<ClinicalFindingValue> patientDiagnosticImages) {
        this.patientDiagnosticImages = patientDiagnosticImages;
    }

    public ClinicalFindingValue getPatientAllergy() {
        if (patientAllergy == null) {
            patientAllergy = new ClinicalFindingValue();
        }
        return patientAllergy;
    }

    public void setPatientAllergy(ClinicalFindingValue patientAllergy) {
        this.patientAllergy = patientAllergy;
    }

    public ClinicalFindingValue getPatientMedicine() {
        if (patientMedicine == null) {
            patientMedicine = new ClinicalFindingValue();
        }
        return patientMedicine;
    }

    public void setPatientMedicine(ClinicalFindingValue patientMedicine) {
        this.patientMedicine = patientMedicine;
    }

    public ClinicalFindingValue getPatientImage() {
        if (patientImage == null) {
            patientImage = new ClinicalFindingValue();
        }
        return patientImage;
    }

    public void setPatientImage(ClinicalFindingValue patientImage) {
        this.patientImage = patientImage;
    }

    public ClinicalFindingValue getPatientDiagnosis() {
        if (patientDiagnosis == null) {
            patientDiagnosis = new ClinicalFindingValue();
        }
        return patientDiagnosis;
    }

    public void setPatientDiagnosis(ClinicalFindingValue patientDiagnosis) {
        this.patientDiagnosis = patientDiagnosis;
    }

    public ClinicalFindingValue getPatientDiagnosticImage() {
        if (patientDiagnosticImage == null) {
            patientDiagnosticImage = new ClinicalFindingValue();
        }
        return patientDiagnosticImage;
    }

    public void setPatientDiagnosticImage(ClinicalFindingValue patientDiagnosticImage) {
        this.patientDiagnosticImage = patientDiagnosticImage;
    }

    public ClinicalFindingValue getRemovingClinicalFindingValue() {
        return removingClinicalFindingValue;
    }

    public void setRemovingClinicalFindingValue(ClinicalFindingValue removingClinicalFindingValue) {
        this.removingClinicalFindingValue = removingClinicalFindingValue;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

}

enum ClinicalField {

    History,
    Examination,
    Investigations,
    Treatments,
    Management,
}
