package com.divudi.bean.common;

// Modified by Dr M H B Ariyaratne with assistance from ChatGPT from OpenAI
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.divudi.bean.clinical.PatientEncounterController;
import com.divudi.bean.clinical.PhotoCamBean;
import com.divudi.bean.clinical.PracticeBookingController;
import com.divudi.bean.collectingCentre.CollectingCentreBillController;
import com.divudi.bean.inward.AdmissionController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.bean.opd.OpdBillController;
import com.divudi.bean.pharmacy.PharmacySaleController;
import com.divudi.bean.web.CaptureComponentController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.HistoryType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.clinical.ClinicalFindingValueType;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.data.inward.PatientEncounterType;
import com.divudi.ejb.BillNumberGenerator;

import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Family;
import com.divudi.entity.FamilyMember;
import com.divudi.entity.Institution;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Person;
import com.divudi.entity.Relation;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.clinical.ClinicalFindingValue;
import com.divudi.entity.inward.Admission;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientSample;
import com.divudi.entity.membership.MembershipScheme;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.FamilyFacade;
import com.divudi.facade.FamilyMemberFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.Department;
import com.divudi.java.CommonFunctions;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.context.PrimeRequestContext;
import org.primefaces.event.CaptureEvent;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class PatientController implements Serializable, ControllerWithPatient {

    /**
     *
     * EJBs
     *
     *
     */
    @EJB
    private PatientFacade ejbFacade;
    @EJB
    FamilyFacade familyFacade;
    @EJB
    FamilyMemberFacade familyMemberFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    BillNumberGenerator billNumberBean;

    CommonFunctions commonFunctions;
    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    private WebUserFacade webUserFacade;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    /**
     *
     * Controllers
     *
     *
     */
    @Inject
    SessionController sessionController;
    @Inject
    PracticeBookingController practiceBookingController;
    @Inject
    PatientEncounterController patientEncounterController;
    @Inject
    private CommonController commonController;
    @Inject
    private SecurityController securityController;
    @Inject
    ApplicationController applicationController;
    @Inject
    BillController billController;
    @Inject
    PharmacySaleController pharmacySaleController;
    @Inject
    CaptureComponentController captureComponentController;
    @Inject
    OpdBillController opdBillController;
    @Inject
    BillPackageController billPackageController;
    @Inject
    OpdPreBillController opdPreBillController;
    @Inject
    AdmissionController admissionController;
    @Inject
    AppointmentController appointmentController;
    @Inject
    private PaymentSchemeController paymentSchemeController;
    @Inject
    BillBeanController billBeanController;
    @Inject
    BillPackageMedicalController billPackageMedicalController;
    @Inject
    CollectingCentreBillController collectingCentreBillController;
    @Inject
    PatientController patientController;

    /**
     *
     * Class Variables
     *
     *
     */
    Date fromDate;
    Date toDate;
    private static final long serialVersionUID = 1L;
    private Patient current;
    private Person currentPerson;
    Long patientId;
    private FamilyMember familyMember;
    private List<FamilyMember> familyMembers;
    Family currentFamily;
    private List<Family> families;
    FamilyMember currentFamilyMember;
    Patient addingPatientToFamily;
    FamilyMember removingFamilyMember;
    Relation currentRelation;
    private String password;
    private UploadedFile uploadedFile;

    private List<Patient> items = null;
    private List<Patient> selectedItems = null;

    private MembershipScheme membershipScheme;

    private Date dob;
    private String membershipTypeListner = "1";

    boolean patientDetailsEditable;

    StreamedContent barcode;
    ReportKeyWord reportKeyWord;

    private String searchText;

    private String searchName;
    private String searchPhone;
    private String searchNic;
    private String searchPhn;
    private String searchPatientCode;
    private String searchPatientId;
    private String searchBillId;
    private String searchSampleId;
    private String searchPatientPhoneNumber;

    private List<Patient> searchedPatients;
    private List<Person> searchedPersons;

    private Integer ageYearComponant;
    private Integer ageMonthComponant;
    private Integer ageDateComponant;
    private String quickSearchPhoneNumber;

    Bill bill;
    private BillItem billItem;
    private List<BillItem> billItems;
    private PaymentMethodData paymentMethodData;

    private boolean printPreview = false;

    private List<PatientInvestigation> patientInvestigations;
    private List<Patient> quickSearchPatientList;

    private Institution institution;
    private Department department;

    /**
     *
     *
     *
     *
     */
    //Damthi's contents
    /**
     *
     *
     *
     *
     */
    /**
     *
     *
     *
     *
     */
    //Pavan's contents
    private List<Patient> allPatientList;
    private List<Person> allPersonList;
    private Map<String, Patient> PatientMap;

    /**
     *
     *
     *
     *
     */
    private List<Bill> patientsPastChannelBookings;

    public String navigateToPatientPastChannelBiiking() {
        return "/channel/patients_pastbookings_channel?faces-redirect=true";
    }

    public void fillPatientsPastChannelbookings() {
        if (current == null) {
            return;
        }

        List<BillType> billTypes = new ArrayList<>();
        billTypes.add(BillType.ChannelCash);
        billTypes.add(BillType.ChannelOnCall);
        billTypes.add(BillType.ChannelPaid);
        Map m = new HashMap<>();
        String jpql = "Select b from Bill b where b.retired=:ret and b.billType in :btas and b.patient=:pt and b.createdAt between :fd and :td ";
        m.put("ret", false);
        m.put("btas", billTypes);
        m.put("pt", current);
        m.put("fd", fromDate);
        m.put("td", toDate);
        patientsPastChannelBookings = billFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
    }

    public Map<String, Patient> CreatePatientMap(List<Patient> patients) {

        Map<String, Patient> patientMap = new HashMap<>();
        for (Patient patient : patients) {
            if (patient != null && patient.getPerson().getId() != null) {
                String patientPerson = String.valueOf(patient.getPerson().getId());
                patientMap.put(patientPerson, patient);
            }
        }
        return patientMap;
    }

    public void validateMobile(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        String mobileRegex = sessionController.getApplicationPreference().getMobileRegex();
        if (mobileRegex != null && !mobileRegex.isEmpty()) {
            if (!value.toString().matches(mobileRegex)) {
                throw new ValidatorException(new FacesMessage("Please enter a valid number"));
            }
        }
    }

    @Deprecated
    public void convertOldPersonPhoneToPatientPhoneLongOld() {

        String j = "select p "
                + " from Patient p "
                + " where p.retired=:ret "
                + " and p.patientPhoneNumber is null "
                + " and (p.person.phone is not null or p.person.mobile is not null) "
                + " order by p.id desc";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        allPatientList = getFacade().findByJpql(j, m, 1000);

        String s = "select p "
                + " from Person p "
                + " where p.retired=:ret "
                + " order by p.id";
        Map<String, Object> h = new HashMap<>();
        h.put("ret", false);
        allPersonList = getPersonFacade().findByJpql(s, h);

        Map<String, Patient> patientMap = CreatePatientMap(allPatientList);
        if (patientMap.isEmpty() || allPersonList.isEmpty()) {
        }

        for (Person person : allPersonList) {
            if (person != null && person.getId() != null) {
                String personId = String.valueOf(person.getId());
                Patient patient = patientMap.get(personId);
                if (patient != null && person.getPhone() != null && !person.getPhone().isEmpty()) {
                    Long personPhone = CommonFunctions.removeSpecialCharsInPhonenumber(person.getPhone());
                    patient.setPatientPhoneNumber(personPhone);
                    getFacade().edit(patient);
                }

            }
        }
    }

    public void convertOldPersonPhoneToPatientPhoneLong() {

        String j = "select p "
                + " from Patient p "
                + " where p.retired=:ret "
                + " and p.patientPhoneNumber is null "
                + " and (p.person.phone is not null or p.person.mobile is not null) "
                + " order by p.id desc";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        allPatientList = getFacade().findByJpql(j, m, 100000);

        for (Patient pt : allPatientList) {
            if (pt.getPerson() == null) {
                continue;
            }
            if (pt.getPerson().getPhone() != null) {
                Long personPhone = CommonFunctions.removeSpecialCharsInPhonenumber(pt.getPerson().getPhone());
                pt.setPatientPhoneNumber(personPhone);
                getFacade().edit(pt);
            }
            if (pt.getPerson().getMobile() != null) {
                Long personPhone = CommonFunctions.removeSpecialCharsInPhonenumber(pt.getPerson().getMobile());
                pt.setPatientMobileNumber(personPhone);
                getFacade().edit(pt);
            }

        }
    }

    public String navigateToconvertOldPersonPhoneToPatientPhoneLong() {

        return "/dataAdmin/downloads";
    }

    public void generateNewPhnAndAssignToCurrentPatient() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return;
        }
        current.setPhn(applicationController.createNewPersonalHealthNumber(sessionController.getInstitution()));
    }

    public void downloadAllPatients() {
        List<Patient> downloadingPatients;
        String j = "select p "
                + " from Patient p "
                + " where p.retired=:ret "
                + " and p.createdAt between :fd and :td "
                + " order by p.id";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        downloadingPatients = getFacade().findByJpql(j, m, 10000);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Patients");
        Row header = sheet.createRow(0);

        String[] columns = {"ID", "Name", "Phone", "Mobile", "NIC", "DOB", "Address", "Email"};

        for (int i = 0; i < columns.length; i++) {
            Cell headerCell = header.createCell(i);
            headerCell.setCellValue(columns[i]);
        }

        int rowNum = 1;
        for (Patient p : downloadingPatients) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(p.getId());
            row.createCell(1).setCellValue(p.getPerson().getName());
            row.createCell(2).setCellValue(p.getPerson().getPhone());
            row.createCell(3).setCellValue(p.getPerson().getMobile());
            row.createCell(4).setCellValue(p.getPerson().getNic());
            row.createCell(5).setCellValue(p.getPerson().getAddress());
            row.createCell(6).setCellValue(p.getPerson().getEmail());
        }

        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance()
                .getExternalContext().getResponse();
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=Patients.xlsx");

        try ( ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FacesContext.getCurrentInstance().responseComplete();
    }

    public void uploadPhoto(FileUploadEvent event) {
        if (getCurrent() == null || getCurrent().getId() == null) {
            JsfUtil.addErrorMessage("Select Patient");
            return;
        }
        byte[] fileBytes;
        try {
            uploadedFile = event.getFile();
            fileBytes = uploadedFile.getContent();
            getCurrent().setBaImage(fileBytes);

            // Extracting the file extension and setting the file name
            String fileName = uploadedFile.getFileName();
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
            getCurrent().setFileName("patient_image_" + getCurrent().getId() + "." + extension);

            getCurrent().setFileType(event.getFile().getContentType());
            save(current);
        } catch (Exception ex) {
            Logger.getLogger(PhotoCamBean.class.getName()).log(Level.SEVERE, null, ex);
            JsfUtil.addErrorMessage("Error");
            return;
        }
    }

    public void oncapturePhoto(CaptureEvent captureEvent) {
        if (getCurrent() == null) {
            JsfUtil.addErrorMessage("Select Encounter");
            return;
        }
        if (getCurrent().getId() == null) {
            save(current);
        }
        getCurrent().setBaImage(captureEvent.getData());
        getCurrent().setFileName("patient_image_" + getCurrent().getId() + ".png");
        getCurrent().setFileType("image/png");
    }

    public String navigateToCapturePatientPhoto() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient");
            return "";
        }
        return "/emr/patient_photo_capture?faces-redirect=true;";
    }

    public StreamedContent getImage() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
            // So, we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.
            return DefaultStreamedContent.builder().build();
        } else if (getCurrent() == null) {
            return DefaultStreamedContent.builder().build();
        } else {
            String imageType = getCurrent().getFileType();
            if (imageType == null || imageType.trim().equals("")) {
                imageType = "image/png";
            }
            return DefaultStreamedContent.builder()
                    .contentType(imageType)
                    .stream(() -> new ByteArrayInputStream(getCurrent().getBaImage()))
                    .build();
        }
    }

    public void downloadPatientsPhoneNumbers() {
        Set<String> uniqueContactNumbers = new HashSet<>();

        String mobileQuery = "select p.person.mobile from Patient p where p.retired=:ret"
                + " and p.createdAt between :fd and :td "
                + " order by p.person.mobile";
        String phoneQuery = "select p.person.phone from Patient p where p.retired=:ret "
                + " and p.createdAt between :fd and :td "
                + "order by p.person.phone";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);

        List<String> mobileNumbers = getFacade().findString(mobileQuery, m);
        List<String> phoneNumbers = getFacade().findString(phoneQuery, m);

        uniqueContactNumbers.addAll(mobileNumbers);
        uniqueContactNumbers.addAll(phoneNumbers);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Patients");
        Row header = sheet.createRow(0);

        String[] columns = {"No", "Phone"};

        for (int i = 0; i < columns.length; i++) {
            Cell headerCell = header.createCell(i);
            headerCell.setCellValue(columns[i]);
        }

        int rowNum = 1;
        for (String p : uniqueContactNumbers) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rowNum + 1);
            row.createCell(1).setCellValue(p);
        }

        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance()
                .getExternalContext().getResponse();
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=PatientPhoneNumbers.xlsx");

        try ( ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FacesContext.getCurrentInstance().responseComplete();
    }

    private CellStyle dateCellStyle(Workbook workbook) {
        CreationHelper createHelper = workbook.getCreationHelper();
        CellStyle dateCellStyle = workbook.createCellStyle();
        dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy"));
        return dateCellStyle;
    }

    public void calculateAgeComponantsFromDob(Patient p) {
        if (p == null || p.getPerson() == null || p.getPerson().getDob() == null) {
            return;
        }

        Date dob = p.getPerson().getDob();
        Calendar today = Calendar.getInstance();
        Calendar birthdate = Calendar.getInstance();
        birthdate.setTime(dob);

        int years = today.get(Calendar.YEAR) - birthdate.get(Calendar.YEAR);
        int months = today.get(Calendar.MONTH) - birthdate.get(Calendar.MONTH);
        int days = today.get(Calendar.DATE) - birthdate.get(Calendar.DATE);

        if (months < 0 || (months == 0 && days < 0)) {
            years--;
            months += 12;
            if (days < 0) {
                months--;
                days += birthdate.getActualMaximum(Calendar.DATE);
            }
        }

        ageYearComponant = years;
        ageMonthComponant = months;
        ageDateComponant = days;
    }

    public void calculateDobFromAgeComponants(Patient p) {
        if (p == null) {
            return;
        }
        if (p.getPerson() == null) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        Integer currentYear = calendar.get(Calendar.YEAR);
        Integer currentMonth = calendar.get(Calendar.MONTH);
        Integer currentDate = calendar.get(Calendar.DATE);

        if (ageYearComponant == null) {
            ageYearComponant = 0;
        }
        if (ageMonthComponant == null) {
            ageMonthComponant = 0;
        }
        if (ageDateComponant == null) {
            ageDateComponant = 0;
        }

        Integer birthYear = currentYear - ageYearComponant;
        Integer birthMonth = currentMonth - ageMonthComponant;
        Integer birthDate = currentDate - ageDateComponant;

        // If the birth date is in the future, subtract a year from the birth year
        if (birthMonth > 0 || (birthMonth == 0 && birthDate > 0)) {
            birthYear--;
        }

        calendar.set(Calendar.YEAR, birthYear);
        calendar.set(Calendar.MONTH, birthMonth);
        calendar.set(Calendar.DATE, birthDate);

        Date calculatetDob = calendar.getTime();
        p.getPerson().setDob(calculatetDob);
    }

    public String navigateToNewOpdVisitFromSearch() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing");
            return "";
        }
        PatientEncounter opdVisit;
        opdVisit = new PatientEncounter();
        opdVisit.setCreatedAt(Calendar.getInstance().getTime());
        opdVisit.setCreater(getSessionController().getLoggedUser());
        opdVisit.setPatient(current);
        opdVisit.setInstitution(sessionController.getInstitution());
        opdVisit.setDepartment(sessionController.getDepartment());
        opdVisit.setPatientEncounterType(PatientEncounterType.OpdVisit);
        getPatientEncounterController().setCurrent(opdVisit);
        getPatientEncounterController().setStartedEncounter(opdVisit);
        getPatientEncounterController().fillCurrentPatientLists(current);
        getPatientEncounterController().fillCurrentEncounterLists(opdVisit);
        getPatientEncounterController().generateDocumentsFromDocumentTemplates(opdVisit);
        getPatientEncounterController().saveSelected();
        return "/emr/opd_visit?faces-redirect=true";
    }

    public String navigateToNewDataEntryForm() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing");
            return "";
        }
        captureComponentController.setDataEntryForms(captureComponentController.listDataEntryForms());
        PatientEncounter opdVisit;
        opdVisit = new PatientEncounter();
        opdVisit.setCreatedAt(Calendar.getInstance().getTime());
        opdVisit.setCreater(getSessionController().getLoggedUser());
        opdVisit.setPatient(current);
        opdVisit.setInstitution(sessionController.getInstitution());
        opdVisit.setDepartment(sessionController.getDepartment());
        opdVisit.setPatientEncounterType(PatientEncounterType.OpdVisit);
        getPatientEncounterController().setCurrent(opdVisit);
        getPatientEncounterController().setStartedEncounter(opdVisit);
        getPatientEncounterController().fillCurrentPatientLists(current);
        getPatientEncounterController().fillCurrentEncounterLists(opdVisit);
        getPatientEncounterController().generateDocumentsFromDocumentTemplates(opdVisit);
        getPatientEncounterController().saveSelected();
        return "/emr/select_data_entry_form?faces-redirect=true;";
    }

    public void generateNewPhn() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient");
            return;
        }
        if (sessionController.getLoggedUser() == null) {
            return;
        }
        if (sessionController.getLoggedUser().getInstitution() == null) {
            return;
        }
        Institution ins = sessionController.getLoggedUser().getInstitution();
        current.setPhn(applicationController.createNewPersonalHealthNumber(ins));
        current.setCreatedInstitution(ins);
    }

    public String toOpdBilling() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        billController.prepareNewBill();
        billController.setPatientSearchTab(1);
        billController.setSearchedPatient(current);
        return billController.toOpdBilling();
    }

    public String toPharmacyBilling() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        pharmacySaleController.prepareForNewPharmacyRetailBill();
        pharmacySaleController.setPatient(current);
        pharmacySaleController.setPatientSearchTab(1);
        return pharmacySaleController.toPharmacyRetailSale();
    }

    public String toEmrPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        patientController.setCurrent(current);
        patientEncounterController.setPatient(current);
        patientEncounterController.fillCurrentPatientLists(current);
        patientEncounterController.fillPatientInvestigations(current);
        return "/emr/patient_profile?faces-redirect=true;";
    }

//    public String toEmrPatientProfile() {
//        if (current == null) {
//            JsfUtil.addErrorMessage("No patient selected");
//            return "";
//        }
//        
//        
//        patientController.setCurrent(current);
//        patientEncounterController.setPatient(current);
//        patientEncounterController.fillCurrentPatientLists(current);
//        patientEncounterController.fillPatientInvestigations(current);
//        return "/emr/patient_profile?faces-redirect=true;";
//    }
    public String navigateToOpdBilling() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        opdBillController.setPatient(current);
        patientEncounterController.setPatient(current);
        patientEncounterController.fillCurrentPatientLists(current);
        patientEncounterController.fillPatientInvestigations(current);
        return "/opd/opd_bill?faces-redirect=true;";
    }

    public String navigateToPharamecyBilling() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        pharmacySaleController.setPatient(current);
        patientEncounterController.setPatient(current);
        patientEncounterController.fillCurrentPatientLists(current);
        patientEncounterController.fillPatientInvestigations(current);
        return "/pharmacy/pharmacy_bill_retail_sale?faces-redirect=true;";
    }

    public String navigateToOpdPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        return "/opd/patient?faces-redirect=true";
    }

    public String navigateToAdmitFromPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        admissionController.prepereToAdmitNewPatient();
        admissionController.getCurrent().setPatient(current);
        return "/inward/inward_admission?faces-redirect=true;";
    }

    public String navigatePatientAdmit() {
        Admission ad = new Admission();
        if (ad.getDateOfAdmission() == null) {
            ad.setDateOfAdmission(commonController.getCurrentDateTime());
        }
        admissionController.setCurrent(ad);
        admissionController.setPrintPreview(false);
        return "/inward/inward_admission?faces-redirect=true;";

    }

    public String navigateToInwardAppointmentFromPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        appointmentController.prepereForInwardAppointPatient();
        appointmentController.setSearchedPatient(getCurrent());
        appointmentController.getCurrentAppointment().setPatient(getCurrent());
        appointmentController.getCurrentBill().setPatient(getCurrent());
        return "/inward/inward_appointment?faces-redirect=true;";
    }

    public String navigateToMedicalPakageBillingFromPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        billPackageMedicalController.clearBillValues();
        billPackageMedicalController.setPatient(getCurrent());
//        appointmentController.prepereForInwardAppointPatient();
//        appointmentController.setSearchedPatient(getCurrent());
//        appointmentController.getCurrentAppointment().setPatient(getCurrent());
//        appointmentController.getCurrentBill().setPatient(getCurrent());
        return "/opd_bill_package_medical?faces-redirect=true;";
    }

    public String navigateToBillingForCashierFromPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        opdPreBillController.prepareNewBill();
        opdPreBillController.setPatient(getCurrent());
        return "/opd/opd_pre_bill?faces-redirect=true;";
    }

    public String navigateToBillingForCashierFromFamilyMembership() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        if (current.getPerson() == null) {
            JsfUtil.addErrorMessage("No person");
            return "";
        }
        if (current.getPerson().getMembershipScheme() == null) {
            JsfUtil.addErrorMessage("No Membership");
            return "";
        }
        if (current.getPerson().getMembershipScheme().getPaymentScheme() == null) {
            JsfUtil.addErrorMessage("No Discount Scheme");
            return "";
        }
        opdPreBillController.prepareNewBill();
        opdPreBillController.setPatient(getCurrent());
        opdPreBillController.setPaymentScheme(current.getPerson().getMembershipScheme().getPaymentScheme());
        return "/opd/opd_pre_bill?faces-redirect=true;";
    }

    public String navigateToReceiveDepositsFromPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        paymentMethodData = new PaymentMethodData();
        bill = new Bill();
        billItem = new BillItem();
        billItems = new ArrayList<>();
        printPreview = false;
        return "/payments/patient/receive?faces-redirect=true;";
    }

    public String navigateToCollectingCenterBillingFromPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }

        collectingCentreBillController.prepareNewBill();
        collectingCentreBillController.setPatient(getCurrent());
        return "/collecting_centre/bill?faces-redirect=true;";
    }

    public String navigateToOpdPatientEdit() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        return "/opd/patient_edit?faces-redirect=true;";
    }

    public String navigateToOpdPatientEditFromId() {
//        if (patientId == null) {
//            JsfUtil.addErrorMessage("No patient selected");
//            return "";
//        }
//        current = getFacade().find(patientId);
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }

        return "/opd/patient?faces-redirect=true;";
    }

    public String navigateToOpdBillFromOpdPatient() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        return opdBillController.navigateToNewOpdBill(current);
    }

    public String navigateToOpdBillFromFamilyMembership() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        if (current.getPerson() == null) {
            JsfUtil.addErrorMessage("No person");
            return "";
        }
        if (current.getPerson().getMembershipScheme() == null) {
            JsfUtil.addErrorMessage("No Membership");
            return "";
        }
        if (current.getPerson().getMembershipScheme().getPaymentScheme() == null) {
            JsfUtil.addErrorMessage("No Discount Scheme");
            return "";
        }

        return opdBillController.navigateToNewOpdBillWithPaymentScheme(current, current.getPerson().getMembershipScheme().getPaymentScheme());
    }

    public String navigateToOpdPackageBillFromOpdPatient() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        return billPackageController.navigateToNewOpdPackageBill(current);
    }

    public String navigateToOpdBillForCashier() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        return "/opd/opd_bill?faces-redirect=true;";
    }

    public String navigateToConvertOldPatientPhoneNumbers() {
        return "/dataAdmin/phone_number_converter";
    }

    public String navigateToSearchPatients() {
        setSearchedPatients(null);
        return "/opd/patient_search?faces-redirect=true;";
    }

    public String navigateToPatientAcceptPayment() {
        setSearchedPatients(null);
        return "/opd/patient_accept_payment?faces-redirect=true;";
    }

    public String navigateToPatientRefundPayment() {
        setSearchedPatients(null);
        return "/opd/patient_refund_payment?faces-redirect=true;";
    }

    public void createPatientInvestigationsTableAllByLoggedInstitution() {

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  "
                + " and b.institution =:ins ";

        Map temMap = new HashMap();
        temMap.put("ins", getSessionController().getInstitution());
        sql += " order by pi.approveAt desc  ";

        //System.err.println("Sql " + sql);
//        patientInvestigations = getPatientInvestigationFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        // patientInvestigations=
    }

    public void settlePatientDepositReceive() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;
        settleBill(BillType.PatientPaymentReceiveBill, HistoryType.PatientDeposit, BillNumberSuffix.PD, current);
        printPreview = true;
    }

    private boolean errorCheck() {
        if (paymentSchemeController.checkPaymentMethodError(getBill().getPaymentMethod(), paymentMethodData)) {
            return true;
        }
        return false;
    }

    public void settleBill(BillType billType, HistoryType historyType, BillNumberSuffix billNumberSuffix, Patient patient) {
        if (getBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a Payment Method");
            return;
        }
        if (paymentSchemeController.checkPaymentMethodError(getBill().getPaymentMethod(), paymentMethodData)) {
            JsfUtil.addErrorMessage("Please enter all relavent Payment Method Details");
            return;
        }

        saveBill(billType, billNumberSuffix, patient);
        billBeanController.setPaymentMethodData(getBill(), getBill().getPaymentMethod(), getPaymentMethodData());
        addToBill();
        saveBillItem();
        billFacade.edit(getBill());
        //TODO: Add Patient Balance History
        if (patient.getRunningBalance() == null) {
            patient.setRunningBalance(getBill().getNetTotal());
        } else {
            patient.setRunningBalance(patient.getRunningBalance() + getBill().getNetTotal());
        }
        getFacade().edit(patient);

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public void addToBill() {
        getBillItem().setNetValue(getBill().getNetTotal());
        getBillItem().setGrossValue(getBill().getNetTotal());
        getBillItem().setBillSession(null);
        getBillItem().setDiscount(0.0);
        getBillItem().setItem(null);
        getBillItem().setQty(1.0);
        getBillItem().setRate(getBill().getNetTotal());
        getBillItems().add(getBillItem());
    }

    private void saveBillItem() {
        for (BillItem tmp : getBillItems()) {
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setBill(getBill());
            tmp.setNetValue(tmp.getNetValue());
            billItemFacade.create(tmp);
        }
    }

    private void saveBill(BillType billType, BillNumberSuffix billNumberSuffix, Patient patient) {
        getBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), billType, BillClassType.BilledBill, billNumberSuffix));
        getBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(sessionController.getDepartment(), billType, BillClassType.BilledBill, billNumberSuffix));
        getBill().setBillType(billType);

        getBill().setPatient(patient);

        getBill().setCreatedAt(new Date());
        getBill().setCreater(sessionController.getLoggedUser());
        getBill().setBillDate(new Date());
        getBill().setBillTime(new Date());

        getBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getBill().setInstitution(getSessionController().getLoggedUser().getInstitution());

        getBill().setCreatedAt(new Date());
        getBill().setCreater(getSessionController().getLoggedUser());

        getBill().setGrantTotal(getBill().getNetTotal());
        getBill().setTotal(getBill().getNetTotal());
        getBill().setDiscount(0.0);
        getBill().setDiscountPercent(0);

        if (getBill().getId() == null) {
            billFacade.create(getBill());
        } else {
            billFacade.edit(getBill());
        }
    }

    public String toChannelling() {
        return "";
    }

    public String toQueue() {
        return "";
    }

    public String toAdmit() {
        return "";
    }

    public String toRecords() {
        return "";
    }

    public String toSearchPatient() {
        return "/emr/patient_search?faces-redirect=true;";
    }

    public void generateNewCode() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient");
            return;
        }
        current.setCode(getCountPatientCode());
    }

    public String toChangeMembershipOfSelectedPersons() {
        items = new ArrayList<>();
        return "/membership/change_membership?faces-redirect=true;";
    }

    public String toAddToQueueFromSearchPatients() {
        if (current == null) {
            JsfUtil.addErrorMessage("No Patient Selected");
            return "";
        }
        patientSelected();
        return "/emr/patient_add_to_queue?faces-redirect=true;";
    }

    public void patientSelected() {
        getPatientEncounterController().fillCurrentPatientLists(current);
    }

    public String searchPatient() {
        if (searchBillId != null && !searchBillId.trim().equals("")) {
            searchByBill();
        } else if (searchSampleId != null && !searchSampleId.trim().equals("")) {
            searchBySample();
        } else if (searchPatientId != null && !searchPatientId.trim().equals("")) {
            searchByPatientId();
        } else {
            searchPatientByDetails();
        }
        if (searchedPatients == null) {
            JsfUtil.addErrorMessage("No Matches. Please use different criteria.");
            return "";
        }
        clearSearchDetails();
        return "";
    }

    public String searchPatientForOpd() {

        boolean noSearchCriteriaWasFound = true;

        if (searchPhn != null && !searchPhn.trim().equals("")) {
            noSearchCriteriaWasFound = false;
        }

        if (searchPhone != null && !searchPhone.trim().equals("")) {
            noSearchCriteriaWasFound = false;
        }

        if (searchNic != null && !searchNic.trim().equals("")) {
            noSearchCriteriaWasFound = false;
        }

        if (searchPatientCode != null && !searchPatientCode.trim().equals("")) {
            noSearchCriteriaWasFound = false;
        }

        if (searchName != null && !searchName.trim().equals("")) {
            noSearchCriteriaWasFound = false;
        }

        if (searchBillId != null && !searchBillId.trim().equals("")) {
            noSearchCriteriaWasFound = false;
        }

        if (searchPhone != null && !searchPhone.trim().equals("")) {
            noSearchCriteriaWasFound = false;
        }

        if (searchPatientId != null && !searchPatientId.trim().equals("")) {
            noSearchCriteriaWasFound = false;
        }

        if (searchSampleId != null && !searchSampleId.trim().equals("")) {
            noSearchCriteriaWasFound = false;
        }

        if (noSearchCriteriaWasFound) {
            JsfUtil.addErrorMessage("No Search Criteria Found !");
            return "";
        }

        if (searchBillId != null && !searchBillId.trim().equals("")) {
            searchByBill();
        } else if (searchSampleId != null && !searchSampleId.trim().equals("")) {
            searchBySample();
        } else if (searchPatientId != null && !searchPatientId.trim().equals("")) {
            searchByPatientId();
        } else if (searchPhone == null && searchNic == null && searchName != null && !searchName.trim().equals("")) {
            searchPatientByName();
        } else if (searchPhone == null && searchName == null && searchNic != null && !searchNic.trim().equals("")) {
            searchPatientByNic();
        } else if (searchPhone == null && searchName == null && searchNic != null && searchNic != null && !searchPatientPhoneNumber.trim().equals("")) {
            searchByPatientPhoneNumber();
        } else {
            searchPatientByDetails();
        }
        if (searchedPatients == null || searchedPatients.isEmpty()) {
            JsfUtil.addErrorMessage("No Matches. Please use different criteria");
            return navigateToAddNewPatientForOpd(getSearchName(), getSearchNic(), getSearchPhone());

        } else if (searchedPatients.size() == 1) {
            setCurrent(searchedPatients.get(0));
            return navigateToOpdPatientProfile();
        }
        clearSearchDetails();
        return "";
    }

    public String searchPatientForOpdFromPatientPhoneLong() {
        //to do by Damith

        return "";
    }

    public void clearSearchDetails() {
        searchName = null;
        searchPhone = null;
        searchPhn = null;
        searchNic = null;
        searchPatientCode = null;
        searchPatientId = null;
        searchBillId = null;
        searchSampleId = null;
        searchPatientPhoneNumber = null;
    }

    public void searchByBill() {
        String j;
        j = "select b.patient from Bill b where b.retired=false ";
        Map m = new HashMap();
        Long temId;
//        if(false){
//            Bill temP = new Bill();
//            temP.getPerson().getName();
//            temP.setRetired(true);
//            temP.getIdStr();
//            temP.getInsId();
//        }
        if (StringUtils.isNumeric(searchBillId)) {
            try {
                temId = Long.parseLong(searchBillId);
                j += " and b.id=:id ";
                m.put("id", temId);
            } catch (NumberFormatException e) {
                temId = 0l;
                j += " and b.id=:id ";
                m.put("id", temId);
            }
        } else {
            j += " and b.insId=:insid ";
            m.put("insid", searchBillId);
            temId = 0l;
        }
        j += " order by b.patient.person.name";
        searchedPatients = getFacade().findByJpql(j, m);
    }

    public void searchBySample() {
        String j;
        j = "select ps.patientInvestigation.billItem.bill.patient from PatientSample ps where ps.patientInvestigation.retired=false ";
        Map m = new HashMap();
        Long temId;
        if (false) {
            PatientSample ps = new PatientSample();
            ps.getId();
            ps.getIdStr();
            ps.getPatientInvestigation().getBillItem().getBill().getPatient();
        }
        if (StringUtils.isNumeric(searchBillId)) {
            try {
                temId = Long.parseLong(searchSampleId);
                j += " and ps.id=:id ";
                m.put("id", temId);
            } catch (Exception e) {
                temId = 0l;
                j += " and ps.id=:id ";
                m.put("id", temId);
                searchedPatients = new ArrayList<>();
            }
        }
        j += " order by ps.patientInvestigation.billItem.bill.patient.person.name";
        searchedPatients = getFacade().findByJpql(j, m);
    }

    public void searchPatientByDetails() {
        boolean atLeastOneCriteriaIsGiven = false;
        String j;
        Map m = new HashMap();
        if (false) {
            Patient temP = new Patient();
            temP.getPerson().getName();
            temP.setRetired(true);
        }

        j = "select p "
                + " from Patient p "
                + " where p.retired=false ";

        if (searchName != null && !searchName.trim().equals("")) {
            j += " and (p.person.name) like :name ";
            m.put("name", "%" + searchName.toLowerCase() + "%");
            atLeastOneCriteriaIsGiven = true;
        }

        if (searchPatientCode != null && !searchPatientCode.trim().equals("")) {
            j += " and (p.code) like :name ";
            m.put("name", "%" + searchPatientCode.toLowerCase() + "%");
            atLeastOneCriteriaIsGiven = true;
        }

        if (searchPhone != null && !searchPhone.trim().equals("")) {
            j += " and (p.person.phone =:phone or p.person.mobile =:phone)";
            m.put("phone", searchPhone);
            atLeastOneCriteriaIsGiven = true;
        }

        if (searchNic != null && !searchNic.trim().equals("")) {
            j += " and p.person.nic =:nic";
            m.put("nic", searchNic);
            atLeastOneCriteriaIsGiven = true;
        }

        if (searchPhn != null && !searchPhn.trim().equals("")) {
            j += " and p.phn =:phn";
            m.put("phn", searchPhn);
            atLeastOneCriteriaIsGiven = true;
        }

        j += " order by p.person.name";

        if (!atLeastOneCriteriaIsGiven) {
            JsfUtil.addErrorMessage("At least one search criteria should be given");
            return;
        }
        searchedPatients = getFacade().findByJpql(j, m);

    }

    public void searchPatientByName() {
        boolean atLeastOneCriteriaIsGiven = false;
        String j;
        Map m = new HashMap();
        if (false) {
            Patient temP = new Patient();
            temP.getPerson().getName();
            temP.setRetired(true);
        }

        j = "select p "
                + " from Patient p "
                + " where p.retired=false ";

        if (searchName != null && !searchName.trim().equals("")) {
            j += " and (p.person.name) like :name ";
            m.put("name", "%" + searchName.toLowerCase() + "%");
            atLeastOneCriteriaIsGiven = true;
        }
        j += " order by p.person.name";

        if (!atLeastOneCriteriaIsGiven) {
            JsfUtil.addErrorMessage("At least one search criteria should be given");
            return;
        }
        searchedPatients = getFacade().findByJpql(j, m);

    }

    public void searchPatientByNic() {
        boolean atLeastOneCriteriaIsGiven = false;
        String j;
        Map m = new HashMap();
        if (false) {
            Patient temP = new Patient();
            temP.getPerson().getName();
            temP.setRetired(true);
        }

        j = "select p "
                + " from Patient p "
                + " where p.retired=false ";

        if (searchNic != null && !searchNic.trim().equals("")) {
            j += " and p.person.nic =:nic";
            m.put("nic", searchNic);
            atLeastOneCriteriaIsGiven = true;
        }
        j += " order by p.person.name";

        if (!atLeastOneCriteriaIsGiven) {
            JsfUtil.addErrorMessage("At least one search criteria should be given");
            return;
        }
        searchedPatients = getFacade().findByJpql(j, m);

    }

    public void searchPatientByPhone() {
        boolean atLeastOneCriteriaIsGiven = false;
        String j;
        Map m = new HashMap();
        if (false) {
            Patient temP = new Patient();
            temP.getPerson().getName();
            temP.setRetired(true);
        }

        j = "select p "
                + " from Patient p "
                + " where p.retired=false ";

        if (searchPhone != null && !searchPhone.trim().equals("")) {
            j += " and (p.person.phone =:phone or p.person.mobile =:phone)";
            m.put("phone", searchPhone);
            atLeastOneCriteriaIsGiven = true;
        }

        j += " order by p.person.name";

        if (!atLeastOneCriteriaIsGiven) {
            JsfUtil.addErrorMessage("At least one search criteria should be given");
            return;
        }
        searchedPatients = getFacade().findByJpql(j, m);

    }

    public void searchByPatientId() {
        String j;
        Map m = new HashMap();
        j = "select p from Patient p where p.retired=false and p.id=:id";
        Long ptId = 0l;
        try {
            ptId = Long.parseLong(searchPatientId);
        } catch (Exception e) {
            m.put("id", ptId);
            searchedPatients = getFacade().findByJpql(j, m);

        }
    }

    public void searchByPatientPhoneNumber() {
        Long patientPhoneNumber = CommonFunctions.removeSpecialCharsInPhonenumber(searchPatientPhoneNumber);
        if (patientPhoneNumber == null) {
            searchedPatients = new ArrayList<>();
            return;
        }
        String j;
        Map m = new HashMap();
        j = "select p from Patient p where p.retired=false and (p.patientPhoneNumber=:pp or p.patientMobileNumber=:pp)";
        m.put("pp", patientPhoneNumber);
        searchedPatients = getFacade().findByJpql(j, m);
    }

    public void quickSearchPatientLongPhoneNumber(ControllerWithPatient controller) {
        Patient patientSearched = null;
        String j;
        Map m = new HashMap();
        j = "select p from Patient p where p.retired=false and (p.patientPhoneNumber=:pp or p.patientMobileNumber=:pp)";
        Long searchedPhoneNumber = CommonFunctions.removeSpecialCharsInPhonenumber(quickSearchPhoneNumber);
        m.put("pp", searchedPhoneNumber);
        quickSearchPatientList = getFacade().findByJpql(j, m);
        if (quickSearchPatientList == null) {
            JsfUtil.addErrorMessage("No Patient found !");
            controller.setPatient(null);
            controller.getPatient().setPhoneNumberStringTransient(quickSearchPhoneNumber);
            controller.getPatient().setMobileNumberStringTransient(quickSearchPhoneNumber);
            controller.setPatientDetailsEditable(true);
            return;
        } else if (quickSearchPatientList.isEmpty()) {
            JsfUtil.addErrorMessage("No Patient found !");
            controller.setPatient(null);
            controller.getPatient().setPhoneNumberStringTransient(quickSearchPhoneNumber);
            controller.getPatient().setMobileNumberStringTransient(quickSearchPhoneNumber);
            controller.setPatientDetailsEditable(true);
            return;
        } else if (quickSearchPatientList.size() == 1) {
            patientSearched = quickSearchPatientList.get(0);
            controller.setPatient(patientSearched);
            controller.setPatientDetailsEditable(false);
            quickSearchPatientList = null;
        } else {
            controller.setPatient(null);
            patientSearched = null;
            controller.setPatientDetailsEditable(false);
            JsfUtil.addErrorMessage("Pleace Select Patient");
        }
    }

    public void quickSearchNewPatient(ControllerWithPatient controller) {
        quickSearchPatientList = null;
        controller.setPatient(new Patient());
        controller.setPatientDetailsEditable(true);
        if (quickSearchPhoneNumber != null) {
            controller.getPatient().setPhoneNumberStringTransient(quickSearchPhoneNumber);
            controller.getPatient().setMobileNumberStringTransient(quickSearchPhoneNumber);
        }
    }

    public void selectQuickOneFromQuickSearchPatient(ControllerWithPatient controller) {
        if (controller == null) {
            JsfUtil.addErrorMessage("Programming Error. Controller is null.");
            return;
        }
        controller.setPatient(current);
        admissionController.fillCurrentPatientAllergies(current);
        controller.setPatientDetailsEditable(false);
        quickSearchPatientList = null;
    }

    public void selectQuickSearchOneFromQuickSearchPatient(ControllerWithPatient controller, Patient pt) {
        if (controller == null) {
            JsfUtil.addErrorMessage("Programming Error. Controller is null.");
            return;
        }
        if (pt == null) {
            JsfUtil.addErrorMessage("Programming Error. Controller is null.");
            return;
        }
        current = pt;
        controller.setPatient(current);
        admissionController.fillCurrentPatientAllergies(current);
        controller.setPatientDetailsEditable(false);
        quickSearchPatientList = null;
    }

    public void listAllPatients() {
        String j = "select p from Patient p where p.retired=false order by p.person.name";
        items = getFacade().findByJpql(j);
    }

    public void listAllMembers() {
        String j = "select p from Patient p where p.retired=false and p.person.membershipScheme is not null order by p.person.name";
        items = getFacade().findByJpql(j);
    }

    public void changeMembershipOfSelectedPersons() {
        for (Patient p : getSelectedItems()) {
            if (p.getPerson() != null) {
                p.getPerson().setMembershipScheme(membershipScheme);
//                p.getPerson().setEditedAt(new Date());
//                p.getPerson().setEditer(sessionController.getLoggedUser());
                getFacade().edit(p);
                getPersonFacade().edit(p.getPerson());
            }
        }
        JsfUtil.addSuccessMessage("Membership Updated");
    }

    @Deprecated
    public String toAddAFamily() {
        currentFamily = new Family();
        return "/membership/add_family";
    }

    public String navigateToAddNewFamilyMembership() {
        currentFamily = new Family();
        return "/membership/family_membership_new?faces-redirect=true";
    }

    public String navigateToAddNewIndividualMembership() {
        currentFamily = new Family();
        if (institution == null) {
            institution = sessionController.getInstitution();
        }
        if (department == null) {
            department = sessionController.getDepartment();
        }
        currentFamily.setCreatedInstitution(institution);
        currentFamily.setCreatedDepartment(department);
        currentFamily.setMembershipScheme(membershipScheme);
        return "/membership/individual_membership_new?faces-redirect=true";
    }

    public String navigateToManageFamilyMembership() {
        return "/membership/family_membership_manage?faces-redirect=true";
    }

    public String navigateToManageIndividualMembership() {
        return "/membership/patient?faces-redirect=true";
    }

    public String navigateToSearchFamilyMembership() {
        currentFamily = null;
        return "/membership/family_membership_search?faces-redirect=true";
    }

    public String navigateToSearchIndividualMembership() {
        currentFamily = null;
        currentFamilyMember = null;
        return "/membership/individual_membership_search?faces-redirect=true";
    }

    public String searchFamily() {
        families = null;
        String j = "Select f from Family f where f.retired=false and f.phoneNo = :pn or f.membershipCardNo = :mcn";
        Map m = new HashMap();
        Long mcn;
        try {
            mcn = Long.parseLong(searchText);
        } catch (Exception e) {
            mcn = 0L;
        }
        m.put("pn", searchText);
        m.put("mcn", mcn);
        List<Family> fs = getFamilyFacade().findByJpql(j, m);
        if (fs == null) {
            JsfUtil.addErrorMessage("No matches");
            return "";
        } else if (fs.size() == 1) {
            currentFamily = fs.get(0);
            searchText = "";
            return navigateToManageFamilyMembership();
        } else {
            families = fs;
            searchText = "";
            return "/membership/search_family?faces-redirect=true;";
        }
    }

    public String searchFamilyMember() {
        familyMembers = null;
        String j = "Select fm "
                + " from FamilyMember fm"
                + " where fm.retired=false "
                + " and fm.family.retired=false "
                + " and (fm.family.phoneNo=:pn or fm.family.membershipCardNo=:mcn or fm.patient.person.mobile=:mobile or fm.patient.person.phone=:phone) ";
        Map m = new HashMap();
        Long mcn;
        try {
            mcn = Long.parseLong(searchText);
        } catch (Exception e) {
            mcn = 0L;
        }
        m.put("pn", searchText);
        m.put("mcn", mcn);
        m.put("mobile", searchText);
        m.put("phone", searchText);

        List<FamilyMember> fs = familyMemberFacade.findByJpql(j, m);
        if (fs == null) {
            JsfUtil.addErrorMessage("No matches");
            return "";
        } else if (fs.size() == 1) {
            currentFamilyMember = fs.get(0);
            current = currentFamilyMember.getPatient();
            searchText = "";
            return navigateToManageIndividualMembership();
        } else {
            familyMembers = fs;
            familyMember = null;
            current = null;
            searchText = "";
            return "";
        }
    }

    public void saveFamily() {
        if (currentFamily == null) {
            JsfUtil.addErrorMessage("No Family Selected to Save or Update");
            return;
        }
        if (currentFamily.getId() == null) {
            currentFamily.setCreatedAt(new Date());
            currentFamily.setCreater(getSessionController().getLoggedUser());
            getFamilyFacade().create(currentFamily);
            JsfUtil.addSuccessMessage("Family Added");
        } else {
            currentFamily.setEditedAt(new Date());
            currentFamily.setEditer(getSessionController().getLoggedUser());
            getFamilyFacade().edit(currentFamily);
            JsfUtil.addSuccessMessage("Family Updated");
        }

    }

    public String saveFamilyAndNavigateToManageFamily() {
        if (currentFamily == null) {
            JsfUtil.addErrorMessage("No Family Selected to Save or Update");
            return "";
        }
        if (currentFamily.getId() == null) {
            currentFamily.setCreatedAt(new Date());
            currentFamily.setCreater(getSessionController().getLoggedUser());
            getFamilyFacade().create(currentFamily);
            JsfUtil.addSuccessMessage("Family Added");
        } else {
            currentFamily.setEditedAt(new Date());
            currentFamily.setEditer(getSessionController().getLoggedUser());
            getFamilyFacade().edit(currentFamily);
            JsfUtil.addSuccessMessage("Family Updated");
        }
        return navigateToManageFamilyMembership();
    }

    public String saveIndividualMembershipAndNavigateToManageIndividual() {
        if (currentFamily == null) {
            JsfUtil.addErrorMessage("No Membership is Selected to Save or Update");
            return "";
        }
        if (current == null) {
            JsfUtil.addErrorMessage("No Patient to Save or Update");
            return "";
        }
        if (current.getPerson().getName() == null || current.getPerson().getName().isEmpty()) {
            JsfUtil.addErrorMessage("No Patient to Save or Update");
            return "";
        }
        // Check if both phone and mobile are either null or empty
        if ((current.getPerson().getPhone() == null || current.getPerson().getPhone().isEmpty())
                && (current.getPerson().getMobile() == null || current.getPerson().getMobile().isEmpty())) {
            JsfUtil.addErrorMessage("No Phone Number");
            return "";
        }
        // Prefer non-empty phone number, else take non-empty mobile number
        if (current.getPerson().getPhone() != null && !current.getPerson().getPhone().isEmpty()) {
            currentFamily.setPhoneNo(current.getPerson().getPhone());
        } else if (current.getPerson().getMobile() != null && !current.getPerson().getMobile().isEmpty()) {
            currentFamily.setPhoneNo(current.getPerson().getMobile());
        }
        saveIndividualMembership();
        JsfUtil.addSuccessMessage("Individual Membership Added to Family");
        return navigateToManageIndividualMembership();
    }

    public void saveIndividualMembership() {
        if (currentFamily == null) {
            JsfUtil.addErrorMessage("No Membership is Selected to Save or Update");
            return;
        }
        if (current == null) {
            JsfUtil.addErrorMessage("No Patient to Save or Update");
            return;
        }
        if (current.getPerson().getName() == null || current.getPerson().getName().isEmpty()) {
            JsfUtil.addErrorMessage("No Patient to Save or Update");
            return;
        }

        if (currentFamily.getId() == null) {
            currentFamily.setCreatedAt(new Date());
            currentFamily.setCreater(getSessionController().getLoggedUser());
            getFamilyFacade().create(currentFamily);
            JsfUtil.addSuccessMessage("Family Added");
        } else {
            currentFamily.setEditedAt(new Date());
            currentFamily.setEditer(getSessionController().getLoggedUser());
            getFamilyFacade().edit(currentFamily);
            JsfUtil.addSuccessMessage("Family Updated");
        }
        current.getPerson().setMembershipScheme(currentFamily.getMembershipScheme());
        save(current);
        FamilyMember tfm = new FamilyMember();
        tfm.setPatient(current);
        tfm.setFamily(currentFamily);
        tfm.setCreatedAt(new Date());
        tfm.setCreater(sessionController.getLoggedUser());
        tfm.setRelationToChh(currentRelation);
        getFamilyMemberFacade().create(tfm);
        currentFamily.getFamilyMembers().add(tfm);
        saveFamily();
        department = currentFamily.getCreatedDepartment();
        institution = currentFamily.getCreatedInstitution();
        membershipScheme = currentFamily.getMembershipScheme();
    }

    public String saveAndClearForNewFamilyMembership() {
        saveFamily();
        currentFamily = new Family();
        return toFamily();
    }

    public String saveAndClearForNewFamily() {
        saveFamily();
        currentFamily = new Family();
        return navigateToAddNewFamilyMembership();
    }

    public String saveAndClearForNewIndividual() {
        if (currentFamily == null) {
            JsfUtil.addErrorMessage("No Membership is Selected to Save or Update");
            return "";
        }
        if (current == null) {
            JsfUtil.addErrorMessage("No Patient to Save or Update");
            return "";
        }
        if (current.getPerson().getName() == null || current.getPerson().getName().isEmpty()) {
            JsfUtil.addErrorMessage("No Patient to Save or Update");
            return "";
        }
        saveIndividualMembership();
        currentFamily = new Family();
        current = new Patient();
        return navigateToAddNewIndividualMembership();
    }

    public String toAddNewFamily() {
        currentFamily = new Family();
        return toFamily();
    }

    public String toFamily() {
        return "/membership/add_family?faces-redirect=true;";
    }

    public String toNewPatient() {
        prepareAdd();
        return "/membership/patient?faces-redirect=true;";
    }

    public void addNewMemberToFamily() {
        saveFamily();
        if (currentFamily == null) {
            JsfUtil.addErrorMessage("No Family Selected.");
            return;
        }
        if (current == null) {
            JsfUtil.addErrorMessage("No Member is selected to add to family.");
            return;
        }
        if (current.getPerson().getMembershipScheme() == null) {
            current.getPerson().setMembershipScheme(currentFamily.getMembershipScheme());
            getPersonFacade().edit(current.getPerson());
        }
        save(current);
        FamilyMember tfm = new FamilyMember();
        tfm.setPatient(current);
        tfm.setFamily(currentFamily);
        tfm.setCreatedAt(new Date());
        tfm.setCreater(sessionController.getLoggedUser());
        tfm.setRelationToChh(currentRelation);
        getFamilyMemberFacade().create(tfm);
        currentFamily.getFamilyMembers().add(tfm);
        saveFamily();
        JsfUtil.addSuccessMessage("Family Member Added to Family");
        current = null;
        currentRelation = null;
    }

    public void removeFamilyMember() {
        if (currentFamily == null) {
            JsfUtil.addErrorMessage("No Family Selected.");
            return;
        }
        if (removingFamilyMember == null) {
            JsfUtil.addErrorMessage("No Member is selected to remove.");
            return;
        }
        try {
            currentFamily.getFamilyMembers().remove(removingFamilyMember);
            getFamilyMemberFacade().remove(removingFamilyMember);
            JsfUtil.addSuccessMessage("Removed");
        } catch (Error e) {
            JsfUtil.addErrorMessage("Error in removing. " + e.getMessage());
        }
    }

    public void removeFamily() {
        if (currentFamily == null) {
            JsfUtil.addErrorMessage("No user");
            return;
        }
        if (currentFamily.getId() == null) {
            JsfUtil.addErrorMessage("User Not yet Added to system to remove");
        } else {
            currentFamily.setRetired(true);
            currentFamily.setRetiredAt(new Date());
            currentFamily.setRetirer(getSessionController().getLoggedUser());
            JsfUtil.addSuccessMessage("Family Removed. But the family members remain in the system.");
        }

    }

    public void removeFamilyAndMembers() {
        if (currentFamily == null) {
            JsfUtil.addErrorMessage("No user");
            return;
        }
        if (currentFamily.getId() == null) {
            JsfUtil.addErrorMessage("User Not yet Added to system to remove");
        } else {
            for (FamilyMember fm : currentFamily.getFamilyMembers()) {
                Patient pt = fm.getPatient();
                pt.setRetired(true);
                pt.setRetiredAt(new Date());
                pt.setRetirer(getSessionController().getLoggedUser());
                getFacade().edit(pt);

                Person p = pt.getPerson();
                p.setRetired(true);
                p.setRetirer(getSessionController().getLoggedUser());
                p.setRetiredAt(new Date());
                getPersonFacade().edit(p);

                WebUser u = p.getWebUser();
                if (u != null) {
                    u.setActivated(false);
                    u.setRetired(true);
                    u.setRetiredAt(new Date());
                    u.setRetirer(getSessionController().getLoggedUser());
                }

            }
            currentFamily.setRetired(true);
            currentFamily.setRetiredAt(new Date());
            currentFamily.setRetirer(getSessionController().getLoggedUser());
            JsfUtil.addSuccessMessage("Family Members and all user details removed.");
        }

    }

    public String toPatientFromSearchPatients() {
        if (current == null) {
            JsfUtil.addErrorMessage("No Patient Selected");
            return "";
        }
        patientSelected();
        return "/emr/patient_basic_info?faces-redirect=true;";
    }

    public String toPatientFromSearchPatientsProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No Patient Selected");
            return "";
        }
        patientSelected();
        return "/emr/patient_profile?faces-redirect=true;";
    }

    public void createPatientBarcode() {
        File barcodeFile = new File("ptbarcode");
        if (current != null && current.getCode() != null && !current.getCode().trim().equals("")) {
            try {
                BarcodeImageHandler.saveJPEG(BarcodeFactory.createCode128(getCurrent().getCode()), barcodeFile);
                InputStream targetStream = new FileInputStream(barcodeFile);
                StreamedContent str = DefaultStreamedContent.builder().contentType("image/jpeg").name(barcodeFile.getName()).stream(() -> targetStream).build();
                barcode = str;
//                return str;

            } catch (Exception ex) {
                //   ////System.out.println("ex = " + ex.getMessage());
            }
        } else {
            //   ////System.out.println("else = ");
            try {
                Barcode bc = BarcodeFactory.createCode128A("0000");
                bc.setBarHeight(5);
                bc.setBarWidth(3);
                bc.setDrawingText(true);
                BarcodeImageHandler.saveJPEG(bc, barcodeFile);
                //   ////System.out.println("12");
                InputStream targetStream = new FileInputStream(barcodeFile);
                StreamedContent str = DefaultStreamedContent.builder().contentType("image/jpeg").name(barcodeFile.getName()).stream(() -> targetStream).build();
                barcode = str;
            } catch (Exception ex) {
                //   ////System.out.println("ex = " + ex.getMessage());
            }
        }
    }

    public void createFamilymembers(ActionEvent event) {
        FacesMessage message = null;
        boolean loggedIn;
        if (familyMember == null) {
            loggedIn = false;
            JsfUtil.addErrorMessage("Please enter full name");
            return;

        }
        if (familyMember.getPatient() == null) {
            loggedIn = false;
            JsfUtil.addErrorMessage("Please enter full name");
            return;

        }
        if (familyMember.getPatient().getPerson().getName() == null || familyMember.getPatient().getPerson().getName().equals("")) {
            loggedIn = false;
            JsfUtil.addErrorMessage("Please enter full name");
            return;

        }
        if (familyMember.getPatient().getPerson().getSex() == null) {
            loggedIn = false;
            JsfUtil.addErrorMessage("Please enter gender");
            return;

        }
       
        if (familyMember.getPatient().getPerson().getDob() == null) {
            loggedIn = false;
            JsfUtil.addErrorMessage("Please enter Date Of Birth");
            return;
        }
        familyMembers.add(familyMember);
        loggedIn = true;
        familyMember = null;
        PrimeRequestContext.getCurrentInstance().getCallbackParams().put("loggedIn", loggedIn);
    }

    public void removeFamilyMember(Person p) {

        familyMembers.remove(p.getSerealNumber());
        int i = 0;
        for (FamilyMember familyMember1 : familyMembers) {
            familyMember1.getPatient().getPerson().setSerealNumber(i);
            i++;
        }
    }

    public void listnerFamilyMember() {
        familyMember = null;

    }

    public void listnerMembershipType() {
        membershipTypeListner = null;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    @Deprecated
    private YearMonthDay yearMonthDay;

    @Deprecated
    public YearMonthDay getYearMonthDay() {
        if (yearMonthDay == null) {
            yearMonthDay = new YearMonthDay();
        }
        return yearMonthDay;
    }

    @Deprecated
    public void setYearMonthDay(YearMonthDay yearMonthDay) {
        this.yearMonthDay = yearMonthDay;
    }

    @Deprecated
    public void dateChangeListen() {
        getCurrent().getPerson().setDob(getCommonFunctions().guessDob(yearMonthDay));
    }

    @Deprecated
    public void dobChangeListen() {
        yearMonthDay = getCommonFunctions().guessAge(getCurrent().getPerson().getDob());
    }

    public StreamedContent getPhoto(Patient p) {
        //////System.out.println("p is " + p);
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            return new DefaultStreamedContent();
        } else if (p == null) {
            return new DefaultStreamedContent();
        } else {
            if (p.getId() != null && p.getBaImage() != null) {
                //////System.out.println("giving image");
                InputStream targetStream = new ByteArrayInputStream(p.getBaImage());
                StreamedContent str = DefaultStreamedContent.builder().contentType(p.getFileType()).name(p.getFileName()).stream(() -> targetStream).build();
                return str;
//                return new DefaultStreamedContent(new ByteArrayInputStream(p.getBaImage()), p.getFileType(), p.getFileName());
            } else {
                return new DefaultStreamedContent();
            }
        }

    }

    public Title[] getTitles() {
        return Title.values();
    }

    public Sex[] getSexs() {
        return Sex.values();
    }

    public void prepareAddReg() {
        prepareAdd();
        current.setCode(null);
    }

    public void prepareAdd() {
        current = null;
        yearMonthDay = null;
        //familyMember=null;
        familyMembers = new ArrayList<>();
        reportKeyWord = new ReportKeyWord();
        getCurrent();

        getYearMonthDay();
    }

    public String toAddNewPatient() {
        current = null;
        yearMonthDay = null;
        getCurrent();
        getYearMonthDay();
        return "/emr/patient?faces-redirect=true;";
    }

    public String navigateToAddNewPatientForOpd() {
        current = null;
        getCurrent();
        return "/opd/patient_edit?faces-redirect=true;";
    }

    public String navigateToAddNewPatientForOpd(String name, String nic, String phone) {
        current = null;
        getCurrent();
        getCurrent().getPerson().setName(name);
        getCurrent().getPerson().setNic(nic);
        getCurrent().getPerson().setPhone(phone);
        getCurrent().getPerson().setMobile(phone);
        return "/opd/patient_edit?faces-redirect=true;";
    }

    public String navigateToAddNewPatientForOpd(String phone) {
        current = null;
        getCurrent();
        getCurrent().getPerson().setPhone(phone);
        getCurrent().getPerson().setMobile(phone);
        return "/opd/patient_edit?faces-redirect=true;";
    }

    public String navigateToEmrEditPatient() {
        getCurrent();
        return "/emr/patient?faces-redirect=true;";
    }

    public String toViewPatient() {
        current = null;
        return "/emr/patient_profile?faces-redirect=true;";
    }

    public String savePatientAndThenNavigateToPatientProfile() {
        saveSelectedPatient();
        return toViewPatient();
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfull");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private void recreateModel() {
        items = null;
    }

    public void createRandomPatient(String ptName) {
        Person person = new Person();
        Patient pt = new Patient();
        person.setName(ptName);
        pt.setPerson(person);
        getPersonFacade().create(person);
        getFacade().create(pt);
    }

    public List<Patient> completePatient(String query) {
        List<Patient> suggestions;
        String sql;
        HashMap hm = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from Patient p where p.retired=false "
                    + " and (p.person.name) like :q "
                    + " or (p.code) like :q "
                    + " or (p.person.nic) like :q"
                    + " or (p.person.mobile) like :q "
                    + "  order by p.person.name";
            hm.put("q", "%" + query.toUpperCase() + "%");
            //////System.out.println(sql);
            suggestions = getFacade().findByJpql(sql, hm, 20);
        }
        return suggestions;
    }

    List<Patient> patientList;

    public List<Patient> completePatientByNameOrCode(String query) {
        if (query == null) {
            return null;
        }
        Date startTime = new Date();
        String sql;
        HashMap hm = new HashMap();
        sql = "select p from Patient p where p.retired=false "
                + " and ((p.person.name) like  :q "
                + " or (p.code) like :q "
                + " or (p.person.nic) like :q "
                + " or (p.person.mobile) like :q "
                + " or (p.person.phone) like :q "
                + " or (p.phn) like :q) ";
        sql += " order by p.person.name";
        hm.put("q", "%" + query.toUpperCase() + "%");
        patientList = getFacade().findByJpql(sql, hm, 20);

        return patientList;
    }

    public void saveAndUpdateQueue() {
        saveSelected();
        getPracticeBookingController().listBillSessions();
    }

    public String getCountPatientCode() {

        String sql;

        sql = "select count(p) FROM Patient p where p.code is not null";

        long lng = getEjbFacade().countByJpql(sql);
        lng++;
        String str = "";
        str += lng;
        return str;
    }

    public void saveSelected() {
        saveSelected(current);
    }

    public String saveSelectedAndToFamily() {
        saveSelected(current);
        return "/membership/add_family?faces-redirect=true;";
    }

    public String saveAndNavigateToOpdPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        saveSelected(current);
        return "/opd/patient";
    }

    public void saveSelected(Patient p) {
        if (p == null) {
            JsfUtil.addErrorMessage("No Current. Error. NOT SAVED");
            return;
        }
        if (p.getPerson() == null) {
            JsfUtil.addErrorMessage("No Person. Not Saved");
            return;
        }
        if (p.getPerson().getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a name");
            return;
        }
        if (p.getPerson().getId() == null) {
            p.getPerson().setCreatedAt(Calendar.getInstance().getTime());
            p.getPerson().setCreater(getSessionController().getLoggedUser());
            getPersonFacade().create(p.getPerson());
        } else {
            getPersonFacade().edit(p.getPerson());
        }
        if (p.getId() == null) {
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            p.setCreatedInstitution(getSessionController().getInstitution());
            getFacade().create(p);
            JsfUtil.addSuccessMessage("Patient Saved Successfully");
        } else {
            getFacade().edit(p);
            JsfUtil.addSuccessMessage("Patient Saved Successfully");
        }
    }

    public String searchByPatientPhoneNumberForPatientLookup() {
        Long patientPhoneNumber = CommonFunctions.removeSpecialCharsInPhonenumber(searchPatientPhoneNumber);
        if (patientPhoneNumber == null) {
            searchedPatients = new ArrayList<>();
            return "No Search Number Given";
        }
        String j;
        Map m = new HashMap();
        j = "select p from Patient p where p.retired=false and (p.patientPhoneNumber=:pp or p.patientMobileNumber=:pp ) ";
        m.put("pp", patientPhoneNumber);
        searchedPatients = getFacade().findByJpql(j, m);
        if (searchedPatients == null || searchedPatients.isEmpty()) {
            JsfUtil.addErrorMessage("No Matches. Please use different criteria");
            return navigateToAddNewPatientForOpd(searchPatientPhoneNumber);
        } else if (searchedPatients.size() == 1) {
            setCurrent(searchedPatients.get(0));
            return navigateToOpdPatientProfile();
        }
        clearSearchDetails();
        return "";

    }

    public void save(Patient p) {
        if (p == null) {
            JsfUtil.addErrorMessage("No Current. Error. NOT SAVED");
            return;
        }

        if (p.getPerson() == null) {
            JsfUtil.addErrorMessage("No Person. Not Saved");
            return;
        }

        if (p.getPerson().getName() == null) {
            JsfUtil.addErrorMessage("Please enter a name");
            return;
        }

        if (p.getPerson().getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a name");
            return;
        }

        if (p.getPerson().getId() == null) {
            p.getPerson().setCreatedAt(Calendar.getInstance().getTime());
            p.getPerson().setCreater(getSessionController().getLoggedUser());
            getPersonFacade().create(p.getPerson());
        } else {
            getPersonFacade().edit(p.getPerson());
        }

        if (p.getId() == null) {
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            p.setCreatedInstitution(getSessionController().getInstitution());
            getFacade().create(p);
            JsfUtil.addSuccessMessage("Saved Successfully");
        } else {
            getFacade().edit(p);
        }
        if (p.getPhn() == null || p.getPhn().trim().equals("")) {
            p.setPhn(applicationController.createNewPersonalHealthNumber(getSessionController().getInstitution()));
            getEjbFacade().edit(p);
        }
        p.setEditingMode(false);
    }

    public String saveAndNavigateToProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No Current. Error. NOT SAVED");
            return "";
        }
        if (current.getPerson() == null) {
            JsfUtil.addErrorMessage("No Person. Not Saved");
            return "";
        }
        if (current.getPerson().getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a name");
            return "";
        }
        if (current.getPerson().getId() == null) {
            current.getPerson().setCreatedAt(Calendar.getInstance().getTime());
            current.getPerson().setCreater(getSessionController().getLoggedUser());
            getPersonFacade().create(current.getPerson());
        } else {
            getPersonFacade().edit(current.getPerson());
        }
        if (current.getId() == null) {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            current.setCreatedInstitution(getSessionController().getInstitution());
            getFacade().create(current);
        } else {
            getFacade().edit(current);
        }

        if (current.getCreatedAt() == null) {
            current.setCreatedAt(new Date());
            getFacade().edit(current);
        }

        if (current.getPerson().getCreatedAt() == null) {
            current.getPerson().setCreatedAt(new Date());
            getPersonFacade().edit(current.getPerson());
        }

        return toEmrPatientProfile();
    }

    public void saveSelectedPatient() {
        if (getCurrent().getPerson().getId() == null) {
            getCurrent().getPerson().setCreatedAt(Calendar.getInstance().getTime());
            getCurrent().getPerson().setCreater(getSessionController().getLoggedUser());
            getPersonFacade().create(getCurrent().getPerson());
        } else {
            getPersonFacade().edit(getCurrent().getPerson());
        }
        if (getCurrent().getId() == null) {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved as a new patient successfully.");
        } else {
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated the patient details successfully.");
        }
        if (getCurrent().getPhn() == null || getCurrent().getPhn().trim().equals("")) {
            getCurrent().setPhn(applicationController.createNewPersonalHealthNumber(getSessionController().getInstitution()));
            getEjbFacade().edit(getCurrent());
        }
//        getPersonFacade().flush();
//        getFacade().flush();
    }

    public void createPatientList() {
        String sql;
        Map m = new HashMap();
        sql = " select p from Patient p ";

        if (getReportKeyWord().isAdditionalDetails()) {
            sql += " where ( p.code is not null "
                    + " or p.code=:code ) ";
            if (getReportKeyWord().getMembershipScheme() != null) {
                sql += " and p.person.membershipScheme=:mem ";
                m.put("mem", getReportKeyWord().getMembershipScheme());
            }
            if (getReportKeyWord().getString().equals("0")) {
            }
            if (getReportKeyWord().getString().equals("1")) {
                sql += " and p.retired=false ";
            }
            if (getReportKeyWord().getString().equals("2")) {
                sql += " and p.retired=true ";
            }
            m.put("code", "");
            sql += " order by p.code ";
            patientList = getFacade().findByJpql(sql, m, getReportKeyWord().getNumOfRows());
            for (Patient p : patientList) {
                if (p.getCreatedAt() != null) {
                    m = new HashMap();
                    sql = "select b from Bill b where b.retired=false "
                            + " and b.billDate=:d "
                            + " and b.patient.id=:p "
                            + " and b.paymentMethod=:pm ";
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(p.getCreatedAt());
                    cal.set(Calendar.HOUR, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    m.put("pm", PaymentMethod.OnlineSettlement);
                    m.put("d", cal.getTime());
                    m.put("p", p.getId());
                    Bill b = getBillFacade().findFirstByJpql(sql, m);
                    if (b != null) {
                        p.setBill(b);
                    }
                }
            }
        } else {
            if (getReportKeyWord().getString().equals("0")) {
            }
            if (getReportKeyWord().getString().equals("1")) {
                sql += " where p.retired=false ";
            }
            if (getReportKeyWord().getString().equals("2")) {
                sql += " where p.retired=true ";
            }
            sql += " order by p.createdAt desc ";
            patientList = getFacade().findByJpql(sql, getReportKeyWord().getNumOfRows());
        }

    }

    public void activePatient(Patient p) {
//        p.setEditedAt(new Date());
//        p.setEditer(getSessionController().getLoggedUser());
        p.setRetired(false);
        p.setRetireComments("Re-Activated");
        getFacade().edit(p);

//        p.getPerson().setEditedAt(new Date());
//        p.getPerson().setEditer(getSessionController().getLoggedUser());
        p.getPerson().setRetired(false);
        p.getPerson().setRetireComments("Re-Activated");
        getPersonFacade().edit(p.getPerson());
        createPatientList();
        JsfUtil.addSuccessMessage("Re-Activated");
    }

    public void deActivePatient(Patient p) {
//        p.setEditedAt(new Date());
//        p.setEditer(getSessionController().getLoggedUser());
        p.setRetired(true);
        p.setRetireComments("De-Activated");
        getFacade().edit(p);

//        p.getPerson().setEditedAt(new Date());
//        p.getPerson().setEditer(getSessionController().getLoggedUser());
        p.getPerson().setRetired(true);
        p.getPerson().setRetireComments("De-Activated");
        getPersonFacade().edit(p.getPerson());
        createPatientList();
        JsfUtil.addSuccessMessage("De-Activated");
    }

    public PatientFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(PatientFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PatientController() {
    }

    @Deprecated
    public Patient findPatientByPatientId(Long pid) {
        String j = "select p "
                + " from Patient p "
                + " where p.patientId=:pid";
        Map m = new HashMap();
        m.put("pid", pid);
        return getFacade().findFirstByJpql(j, m);
    }

    public Patient findPatientById(Long pid) {
        String j = "select p "
                + " from Patient p "
                + " where p.id=:pid";
        Map m = new HashMap();
        m.put("pid", pid);
        return getFacade().findFirstByJpql(j, m);
    }

    public Patient findPatientById(String pidString) {
        Long pid;
        try {
            pid = Long.parseLong(pidString);
        } catch (NumberFormatException e) {
            return null;
        }
        return findPatientById(pid);
    }

    public Patient getCurrent() {
        if (current == null) {
            Person p = new Person();
            current = new Patient();
            current.setPerson(p);
        }
        return current;
    }

    public void setCurrent(Patient current) {
        this.current = current;
        getYearMonthDay();
        if (current == null) {
            yearMonthDay.setDay("0");
            yearMonthDay.setMonth("0");
            yearMonthDay.setYear("0");
        } else {
            yearMonthDay.setDay(current.getAgeDays() + "");
            yearMonthDay.setMonth(current.getAgeMonths() + "");
            yearMonthDay.setYear(current.getAgeYears() + "");
        }
        getPatientEncounterController().fillCurrentPatientLists(current);
    }

    private PatientFacade getFacade() {
        return ejbFacade;
    }

    public List<Patient> getItems() {
        return items;
    }

    public void fillAllPatients() {
        String sql;
        sql = "select p from Patient p where p.retired = false order by p.person.name";
        items = getFacade().findByJpql(sql);
    }

    public List<Patient> fillAllPatientstoList() {
        fillAllPatients();
        return items;
    }

    public List<Patient> getItemsByDob() {
        String sql;
        Map m = new HashMap();
        m.put("dob", dob);
        sql = "select p from Patient p where p.retired = false and p.person.dob=:dob order by p.person.name";
        return getFacade().findByJpql(sql, m);
    }

    public void membershipChangeListner() {
        if (getCurrent().getPerson().getMembershipScheme() == null) {
            getCurrent().setCode(null);
            return;
        }
        if (getCurrent().getPerson().getMembershipScheme().getCode() == null
                || getCurrent().getPerson().getMembershipScheme().getCode().equals("")) {
            getCurrent().setCode(null);
            JsfUtil.addErrorMessage("Please Select Membership Scheme Code Correctly");
            return;
        }
        if (getCurrent().getId() == null) {
            getCurrent().setCode(getCountPatientCode(getCurrent().getPerson().getMembershipScheme().getCode()));
        } else {
            Patient p = getEjbFacade().find(getCurrent().getId());
            getCurrent().setCode(p.getCode());
        }
    }

    public String getCountPatientCode(String s) {

        String sql;
        Map m = new HashMap();
        sql = "select p FROM Patient p "
                + " where p.code is not null"
                + " and p.retired=false "
                + " and (p.code) like :q "
                + " order by p.code desc ";
        m.put("q", "%" + s.toUpperCase() + "%");

        Patient p = getEjbFacade().findFirstByJpql(sql, m);
        DecimalFormat df = new DecimalFormat("000000");
        String st = "";
        if (p != null) {
            String str = p.getCode();
//        //System.out.println("str.substring(0,1) = " + str.substring(0, 1));
//        //System.out.println("str.substring(0,2) = " + str.substring(0, 2));
//        //System.out.println("str.substring(2) = " + str.substring(2));
//        //System.out.println("str.substring(3) = " + str.substring(3));
//        //System.out.println("str.substring(3,7) = " + str.substring(3, 7));
            long l = Long.parseLong(str.substring(2));
            l++;
            st += s;
            st += df.format(l);
            return st;
        } else {
            st += s;
            st += df.format(1l);
            return st;
        }

    }

    private boolean errorCheck(Patient p) {
        if (p == null) {
            JsfUtil.addErrorMessage("No Current. Error. NOT SAVED");
            return true;
        }
        if (p.getPerson() == null) {
            JsfUtil.addErrorMessage("No Person. Not Saved");
            return true;
        }
        if (p.getPerson().getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please Enter a Name");
            return true;
        }
        if (p.getPerson().getSex() == null) {
            JsfUtil.addErrorMessage("Please Select Sex");
            return true;
        }
        if (p.getPerson().getDob() == null) {
            JsfUtil.addErrorMessage("Please Pic a Birth Day");
            return true;
        }
        if (p.getPerson().getAddress() == null || p.getPerson().getAddress().equals("")) {
            JsfUtil.addErrorMessage("Please Enter a Address");
            return true;
        }
        if (sessionController.getApplicationPreference().isNeedAreaForPatientRegistration()) {
            if (p.getPerson().getArea() == null) {
                JsfUtil.addErrorMessage("Please Enter a Area");
                return true;
            }
        }
        if (sessionController.getApplicationPreference().isNeedPhoneNumberForPatientRegistration()) {
            if (p.getPerson().getPhone() == null || p.getPerson().getPhone().equals("")) {
                JsfUtil.addErrorMessage("Please Enter a Phone Number");
                return true;
            }
        }
        if (sessionController.getApplicationPreference().isNeedNicForPatientRegistration()) {
            if (p.getPerson().getNic() == null || p.getPerson().getNic().equals("")) {
                JsfUtil.addErrorMessage("Please Enter a Nic No");
                return true;
            }
        }
//        if (getCurrent().getPhn().equals("")) {
//            JsfUtil.addErrorMessage("Please Enter PHN number");
//            return;
//        }
        return false;
    }

    private boolean checkCodeNull(Patient pt) {
        Patient p = null;
        if (pt.getId() != null) {
            p = getEjbFacade().find(pt.getId());
        }
        if (p != null) {
            if (pt.getCode() == null || pt.getCode().equals("")) {
                JsfUtil.addErrorMessage("Please Enter a Code");
                return true;
            } else {
                String sql;
                Map m = new HashMap();
                sql = "select p FROM Patient p "
                        + " where p.code is not null"
                        + " and p.retired=false "
                        + " and p!=:p "
                        + " and (p.code)=:q "
                        + " order by p.code desc ";
                m.put("q", pt.getCode().toUpperCase());
                m.put("p", pt);

                p = getEjbFacade().findFirstByJpql(sql, m);
                if (p != null) {
                    JsfUtil.addErrorMessage("Code Already Exsist.Please Try - " + getCountPatientCode(pt.getPerson().getMembershipScheme().getCode()));
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }

    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void searchPatients() {
        int criteriaCount = countNonEmptyCriteria();

        if (criteriaCount == 1) {
            if (searchName != null && !searchName.isEmpty()) {
                searchByName(searchName);
            } else if (searchPhone != null && !searchPhone.isEmpty()) {
                searchByPhone(searchPhone);
            } else if (searchNic != null && !searchNic.isEmpty()) {
                searchByNic(searchNic);
            } else if (searchPhn != null && !searchPhn.isEmpty()) {
                searchByPhn(searchPhn);
            } else if (searchPatientCode != null && !searchPatientCode.isEmpty()) {
                searchByPatientCode(searchPatientCode);
            } else if (searchPatientId != null && !searchPatientId.isEmpty()) {
                searchByPatientId(searchPatientId);
            } else if (searchBillId != null && !searchBillId.isEmpty()) {
                searchByBillId(searchBillId);
            } else if (searchSampleId != null && !searchSampleId.isEmpty()) {
                searchBySampleId(searchSampleId);
            }
        } else if (criteriaCount > 1) {
            searchByMultipleCriteria();
        }
    }

    public void searchPersons() {
        int criteriaCount = countNonEmptyCriteria();

        if (criteriaCount == 1) {
            if (searchName != null && !searchName.isEmpty()) {
                searchByName(searchName);
            } else if (searchPhone != null && !searchPhone.isEmpty()) {
                searchByPhone(searchPhone);
            } else if (searchNic != null && !searchNic.isEmpty()) {
                searchByNic(searchNic);
            } else if (searchPhn != null && !searchPhn.isEmpty()) {
                searchByPhn(searchPhn);
            } else if (searchPatientCode != null && !searchPatientCode.isEmpty()) {
                searchByPatientCode(searchPatientCode);
            } else if (searchPatientId != null && !searchPatientId.isEmpty()) {
                searchByPatientId(searchPatientId);
            } else if (searchBillId != null && !searchBillId.isEmpty()) {
                searchByBillId(searchBillId);
            } else if (searchSampleId != null && !searchSampleId.isEmpty()) {
                searchBySampleId(searchSampleId);
            }
        } else if (criteriaCount > 1) {
            searchByMultipleCriteria();
        }
    }

    private int countNonEmptyCriteria() {
        int count = 0;
        if (searchName != null && !searchName.isEmpty()) {
            count++;
        }
        if (searchPhone != null && !searchPhone.isEmpty()) {
            count++;
        }
        if (searchNic != null && !searchNic.isEmpty()) {
            count++;
        }
        if (searchPhn != null && !searchPhn.isEmpty()) {
            count++;
        }
        if (searchPatientCode != null && !searchPatientCode.isEmpty()) {
            count++;
        }
        if (searchPatientId != null && !searchPatientId.isEmpty()) {
            count++;
        }
        if (searchBillId != null && !searchBillId.isEmpty()) {
            count++;
        }
        if (searchSampleId != null && !searchSampleId.isEmpty()) {
            count++;
        }
        return count;
    }

    // Placeholder for the common search method
    private void searchByMultipleCriteria() {
        // Logic for searching by multiple criteria
    }

    // Placeholder methods for individual search criteria
    private void searchByName(String name) {
        String j1 = "Select p "
                + " from Person p"
                + " where p.retired=:ret"
                + " and p.name like :name";
        Map m1 = new HashMap();
        m1.put("ret", false);
        m1.put("name", "%" + name + "%");
        searchedPersons = personFacade.findLongList(j1, m1);
    }

    public void searchByName1() {
        Long start = new Date().getTime();
        String j1 = "Select p.id "
                + " from Person p"
                + " where p.retired=:ret"
                + " and p.name like :name";
        Map m1 = new HashMap();
        m1.put("ret", false);
        m1.put("name", "%" + searchName + "%");
        List<Long> pids = personFacade.findLongList(j1, m1);

        String j2 = "Select pt "
                + " from Patient pt "
                + " where pt.retired=:ret "
                + " and pt.person.id in :ids";
        Map m2 = new HashMap();
        m2.put("ret", false);
        m2.put("ids", pids);
        searchedPatients = getFacade().findByJpql(j2, m2);
        Long end = new Date().getTime();
        Long duration = end - start;
    }

    public void searchByName2() {
        Long start = new Date().getTime();
        String j1 = "Select p "
                + " from Person p"
                + " where p.retired=:ret"
                + " and p.name like :name";
        Map m1 = new HashMap();
        m1.put("ret", false);
        m1.put("name", "%" + searchName + "%");
        List<Person> pids = personFacade.findByJpql(j1, m1);

        String j2 = "Select pt "
                + " from Patient pt "
                + " where pt.retired=:ret "
                + " and pt.person in :ps";
        Map m2 = new HashMap();
        m2.put("ret", false);
        m2.put("ps", pids);
        searchedPatients = getFacade().findByJpql(j2, m2);
        Long end = new Date().getTime();
        Long duration = end - start;
    }

    public void searchByName3() {
        Long start = new Date().getTime();
        String j1 = "Select p "
                + " from Patient p"
                + " where p.retired=:ret"
                + " and p.person.name like :name";
        Map m1 = new HashMap();
        m1.put("ret", false);
        m1.put("name", "%" + searchName + "%");
        searchedPatients = getFacade().findByJpql(j1, m1);
        Long end = new Date().getTime();
        Long duration = end - start;
    }

    public void searchPersons1() {
        Long start = new Date().getTime();
        String j1 = "Select p "
                + " from Person p"
                + " where p.retired=:ret"
                + " and p.name like :name";
        Map m1 = new HashMap();
        m1.put("ret", false);
        m1.put("name", "%" + searchName + "%");
        List<Person> pids = personFacade.findByJpql(j1, m1);
        Long end = new Date().getTime();
        Long duration = end - start;
    }

    public void searchByNameOptimized() {
        Long start = new Date().getTime();

        // JPQL query with an explicit join
        String jpql = "SELECT pt FROM Patient pt JOIN pt.person p "
                + "WHERE pt.retired = :ret "
                + "AND p.name LIKE :name";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ret", false);
        parameters.put("name", "%" + searchName + "%");

        // Execute the query and measure duration
        searchedPatients = getFacade().findByJpql(jpql, parameters);

        Long end = new Date().getTime();
        Long duration = end - start;
    }

    private void searchByPhone(String phone) {
        String j1 = "Select p "
                + " from Person p"
                + " where p.retired=:ret"
                + " and (p.phone=:phone or p.mobile=:phone) ";
        Map m1 = new HashMap();
        m1.put("ret", false);
        m1.put("phone", phone);
        searchedPersons = personFacade.findLongList(j1, m1);
    }

    private void searchByNic(String nic) {
        String j1 = "Select p "
                + " from Person p"
                + " where p.retired=:ret"
                + " and p.nic=:nic ";
        Map m1 = new HashMap();
        m1.put("ret", false);
        m1.put("nic", nic);
        searchedPersons = personFacade.findLongList(j1, m1);
    }

    private void searchByPhn(String phn) {
        String j1 = "Select p.person "
                + " from Patient p"
                + " where p.retired=:ret"
                + " and p.phn=:phn ";
        Map m1 = new HashMap();
        m1.put("ret", false);
        m1.put("phn", phn);
        searchedPersons = personFacade.findLongList(j1, m1);
    }

    private void searchByPatientCode(String patientCode) {
        // Logic to search by patient code
    }

    private void searchByPatientId(String patientId) {
        // Logic to search by patient ID
    }

    private void searchByBillId(String billId) {
        // Logic to search by bill ID
    }

    private void searchBySampleId(String sampleId) {
        // Logic to search by sample ID
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public StreamedContent getBarcode() {
        return barcode;
    }

    public void setBarcode(StreamedContent barcode) {
        this.barcode = barcode;
    }

    public String getMembershipTypeListner() {
        return membershipTypeListner;
    }

    public void setMembershipTypeListner(String membershipTypeListner) {
        this.membershipTypeListner = membershipTypeListner;
    }

    public FamilyMember getFamilyMember() {
        if (familyMember == null) {
            familyMember = new FamilyMember();
            Patient pt = new Patient();
            familyMember.setPatient(pt);
        }
        return familyMember;
    }

    public void setFamilyMember(FamilyMember familyMember) {
        this.familyMember = familyMember;
    }

    public List<FamilyMember> getFamilyMembers() {
        if (familyMembers == null) {
            familyMembers = new ArrayList<>();
        }
        return familyMembers;
    }

    public void setFamilyMembers(List<FamilyMember> familyMembers) {
        this.familyMembers = familyMembers;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public List<Patient> getPatientList() {
        return patientList;
    }

    public void setPatientList(List<Patient> patientList) {
        this.patientList = patientList;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public List<Family> getFamilies() {
        return families;
    }

    public void setFamilies(List<Family> families) {
        this.families = families;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public List<Patient> getSelectedItems() {
        if (selectedItems == null) {
            selectedItems = new ArrayList<>();
        }
        return selectedItems;
    }

    public void setSelectedItems(List<Patient> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public MembershipScheme getMembershipScheme() {
        return membershipScheme;
    }

    public void setMembershipScheme(MembershipScheme membershipScheme) {
        this.membershipScheme = membershipScheme;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public SecurityController getSecurityController() {
        return securityController;
    }

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public String getSearchName() {
        return searchName;
    }

    public void setSearchName(String searchName) {
        this.searchName = searchName;
    }

    public String getSearchPhone() {
        return searchPhone;
    }

    public void setSearchPhone(String searchPhone) {
        this.searchPhone = searchPhone;
    }

    public String getSearchNic() {
        return searchNic;
    }

    public void setSearchNic(String searchNic) {
        this.searchNic = searchNic;
    }

    public String getSearchPhn() {
        return searchPhn;
    }

    public void setSearchPhn(String searchPhn) {
        this.searchPhn = searchPhn;
    }

    public String getSearchPatientCode() {
        return searchPatientCode;
    }

    public void setSearchPatientCode(String searchPatientCode) {
        this.searchPatientCode = searchPatientCode;
    }

    public String getSearchPatientId() {
        return searchPatientId;
    }

    public void setSearchPatientId(String searchPatientId) {
        this.searchPatientId = searchPatientId;
    }

    public String getSearchBillId() {
        return searchBillId;
    }

    public void setSearchBillId(String searchBillId) {
        this.searchBillId = searchBillId;
    }

    public String getSearchSampleId() {
        return searchSampleId;
    }

    public void setSearchSampleId(String searchSampleId) {
        this.searchSampleId = searchSampleId;
    }

    public List<Patient> getSearchedPatients() {
        return searchedPatients;
    }

    public void setSearchedPatients(List<Patient> searchedPatients) {
        this.searchedPatients = searchedPatients;
    }

    public Integer getAgeYearComponant() {
        return ageYearComponant;
    }

    public void setAgeYearComponant(Integer ageYearComponant) {
        this.ageYearComponant = ageYearComponant;
    }

    public Integer getAgeMonthComponant() {
        return ageMonthComponant;
    }

    public void setAgeMonthComponant(Integer ageMonthComponant) {
        this.ageMonthComponant = ageMonthComponant;
    }

    public Integer getAgeDateComponant() {
        return ageDateComponant;
    }

    public void setAgeDateComponant(Integer ageDateComponant) {
        this.ageDateComponant = ageDateComponant;
    }

    public PracticeBookingController getPracticeBookingController() {
        return practiceBookingController;
    }

    public void setPracticeBookingController(PracticeBookingController practiceBookingController) {
        this.practiceBookingController = practiceBookingController;
    }

    public PatientEncounterController getPatientEncounterController() {
        return patientEncounterController;
    }

    public void setPatientEncounterController(PatientEncounterController PatientEncounterController) {
        this.patientEncounterController = PatientEncounterController;
    }

    public FamilyFacade getFamilyFacade() {
        return familyFacade;
    }

    public FamilyMemberFacade getFamilyMemberFacade() {
        return familyMemberFacade;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Family getCurrentFamily() {
        return currentFamily;
    }

    public void setCurrentFamily(Family currentFamily) {
        this.currentFamily = currentFamily;
    }

    public FamilyMember getCurrentFamilyMember() {
        return currentFamilyMember;
    }

    public void setCurrentFamilyMember(FamilyMember currentFamilyMember) {
        this.currentFamilyMember = currentFamilyMember;
    }

    public Patient getAddingPatientToFamily() {
        return addingPatientToFamily;
    }

    public void setAddingPatientToFamily(Patient addingPatientToFamily) {
        this.addingPatientToFamily = addingPatientToFamily;
    }

    public FamilyMember getRemovingFamilyMember() {
        return removingFamilyMember;
    }

    public void setRemovingFamilyMember(FamilyMember removingFamilyMember) {
        this.removingFamilyMember = removingFamilyMember;
    }

    public Relation getCurrentRelation() {
        return currentRelation;
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

    public void setCurrentRelation(Relation currentRelation) {
        this.currentRelation = currentRelation;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public PaymentMethodData getPaymentMethodData() {
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public List<BillItem> getBillItems() {
        if (billItems == null) {
            billItems = new ArrayList<>();
        }
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public List<Person> getSearchedPersons() {
        return searchedPersons;
    }

    public void setSearchedPersons(List<Person> searchedPersons) {
        this.searchedPersons = searchedPersons;
    }

    public Person getCurrentPerson() {
        return currentPerson;
    }

    public void setCurrentPerson(Person currentPerson) {
        this.currentPerson = currentPerson;
        if (currentPerson != null) {
            current = findPatientOfAPerson(currentPerson);
        }
    }

    public Patient findPatientOfAPerson(Person p) {
        String jpql;
        Map m = new HashMap();
        jpql = "select p "
                + " from Patient p "
                + " where p.person=:person "
                + " order by p.id";
        m.put("person", p);
        return getFacade().findFirstByJpql(jpql, m);
    }

    public List<Patient> getAllPatientList() {
        return allPatientList;
    }

    public void setAllPatientList(List<Patient> allPatientList) {
        this.allPatientList = allPatientList;
    }

    public List<Person> getAllPersonList() {
        return allPersonList;
    }

    public void setAllPersonList(List<Person> allPersonList) {
        this.allPersonList = allPersonList;
    }

    public Map<String, Patient> getPatientMap() {
        return PatientMap;
    }

    public void setPatientMap(Map<String, Patient> PatientMap) {
        this.PatientMap = PatientMap;
    }

    public String getSearchPatientPhoneNumber() {
        return searchPatientPhoneNumber;
    }

    public void setSearchPatientPhoneNumber(String searchPatientPhoneNumber) {
        this.searchPatientPhoneNumber = searchPatientPhoneNumber;
    }

    public String getQuickSearchPhoneNumber() {
        return quickSearchPhoneNumber;
    }

    public void setQuickSearchPhoneNumber(String quickSearchPhoneNumber) {
        this.quickSearchPhoneNumber = quickSearchPhoneNumber;
    }

    public List<Patient> getQuickSearchPatientList() {
        return quickSearchPatientList;
    }

    public void setQuickSearchPatientList(List<Patient> quickSearchPatientList) {
        this.quickSearchPatientList = quickSearchPatientList;
    }

    public List<Bill> getPatientsPastChannelBookings() {
        return patientsPastChannelBookings;
    }

    public void setPatientsPastChannelBookings(List<Bill> patientsPastChannelBookings) {
        this.patientsPastChannelBookings = patientsPastChannelBookings;
    }

    @Override
    public Patient getPatient() {
        if (current == null) {
            current = new Patient();
        }
        return current;
    }

    @Override
    public void setPatient(Patient patient) {
        this.current = patient;
    }

    @Override
    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
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

    /**
     *
     * Set all Patients to null
     *
     */
    /**
     *
     */
    /**
     *
     * Delete the current Patient
     *
     */
    /**
     *
     */
    @FacesConverter(forClass = Patient.class)
    public static class PatientControllerConverter implements Converter {

        /**
         *
         * @param facesContext
         * @param component
         * @param value
         * @return
         */
        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PatientController controller = (PatientController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "patientController");
            //////System.out.println("value at converter getAsObject is " + value);
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            //////System.out.println(value);
            if (value == null || value.equals("null") || value.trim().equals("")) {
                key = 0l;
            } else {
                key = Long.valueOf(value);
            }
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        /**
         *
         * @param facesContext
         * @param component
         * @param object
         * @return
         */
        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Patient) {
                Patient o = (Patient) object;
                return getStringKey(o.getId());
            } else {
                String error = "object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PatientController.class.getName();
                return null;
            }
        }
    }

}
