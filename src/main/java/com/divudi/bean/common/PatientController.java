package com.divudi.bean.common;

// Modified by Dr M H B Ariyaratne with assistance from ChatGPT from OpenAI

import com.divudi.bean.optician.OpticianRepairBillController;
import com.divudi.bean.optician.OpticianSaleController;
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
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.HistoryType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.core.data.hr.ReportKeyWord;
import com.divudi.core.data.inward.PatientEncounterType;
import com.divudi.ejb.BillNumberGenerator;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Family;
import com.divudi.core.entity.FamilyMember;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.Relation;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.lab.PatientSample;
import com.divudi.core.entity.membership.MembershipScheme;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.FamilyFacade;
import com.divudi.core.facade.FamilyMemberFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.SpecificPatientStatus;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.MembershipService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    PatientFacade ejbFacade;
    @EJB
    FamilyFacade familyFacade;
    @EJB
    FamilyMemberFacade familyMemberFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    private BillNumberGenerator billNumberGenerator;

    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    private WebUserFacade webUserFacade;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    MembershipService membershipService;
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
    OpticianSaleController opticianSaleController;
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
    OpticianRepairBillController opticianRepairBillController;
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
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    PatientDepositController patientDepositController;
    @Inject
    WebUserController webUserController;

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
    private Family currentFamily;
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
    private String searchMembershipCardNo;
    private String searchPatientAddress;

    private String searchName;
    private String searchPhone;
    private String searchNic;
    private String searchPhn;
    private String searchMrn;
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
    Bill cancelBill;

    private BillItem billItem;
    private List<BillItem> billItems;
    private PaymentMethodData paymentMethodData;

    private boolean printPreview = false;

    private List<PatientInvestigation> patientInvestigations;
    private List<Patient> quickSearchPatientList;

    private Institution institution;
    private Department department;

    private boolean reGenerateePhn;
    private PaymentMethod paymentMethod;
    private String blacklistComment;

    public String getBlacklistComment() {
        return blacklistComment;
    }

    public void setBlacklistComment(String blacklistComment) {
        this.blacklistComment = blacklistComment;
    }

    /**
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
    
    public List<SpecificPatientStatus> getAllPatientSpecificLabels(){
        return Arrays.asList(SpecificPatientStatus.values());
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

        try (ServletOutputStream outputStream = response.getOutputStream()) {
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
        return "/emr/patient_photo_capture?faces-redirect=true";
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

        try (ServletOutputStream outputStream = response.getOutputStream()) {
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
        return "/emr/select_data_entry_form?faces-redirect=true";
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
        return "/emr/patient_profile?faces-redirect=true";
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
//        return "/emr/patient_profile?faces-redirect=true";
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
        return "/opd/opd_bill?faces-redirect=true";
    }

    public String navigateToPharamecyBilling() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        pharmacySaleController.setPatient(current);
        pharmacySaleController.setBillSettlingStarted(false);
        patientEncounterController.setPatient(current);
        patientEncounterController.fillCurrentPatientLists(current);
        patientEncounterController.fillPatientInvestigations(current);
        return "/pharmacy/pharmacy_bill_retail_sale?faces-redirect=true";
    }

    public String navigateToOpticianBilling() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        opticianSaleController.setPatient(current);
        patientEncounterController.setPatient(current);
        patientEncounterController.fillCurrentPatientLists(current);
        patientEncounterController.fillPatientInvestigations(current);
        return "/optician/sale?faces-redirect=true";
    }

    public String navigateToOpdPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        return "/opd/patient?faces-redirect=true";
    }

    public String navigateToOpticianProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        return "/optician/patient?faces-redirect=true";
    }

    public String navigateToAdmitFromPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        admissionController.prepereToAdmitNewPatient();
        admissionController.getCurrent().setPatient(current);
        return "/inward/inward_admission?faces-redirect=true";
    }

    public String navigateToAddToQueueFromPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        admissionController.prepereToAdmitNewPatient();
        admissionController.getCurrent().setPatient(current);
        return "/inward/inward_admission?faces-redirect=true";
    }

    public String navigatePatientAdmit() {
        Admission ad = new Admission();
        if (ad.getDateOfAdmission() == null) {
            ad.setDateOfAdmission(CommonFunctions.getCurrentDateTime());
        }
        admissionController.setCurrent(ad);
        admissionController.setPrintPreview(false);
        admissionController.setAdmittingProcessStarted(false);
        admissionController.setPatientRoom(new PatientRoom());
        quickSearchPhoneNumber = null;
        admissionController.setPatientAllergies(null);
        return "/inward/inward_admission?faces-redirect=true";

    }

    public String navigateToConvertNonBhtToBht(Admission nonBhtAd) {
        Admission ad = new Admission();
        if (ad.getDateOfAdmission() == null) {
            ad.setDateOfAdmission(CommonFunctions.getCurrentDateTime());
        }
        ad.setPatient(nonBhtAd.getPatient());
        admissionController.setCurrentNonBht(nonBhtAd);
        admissionController.setCurrent(ad);
        admissionController.setPrintPreview(false);
        return "/inward/convert_inward_admission?faces-redirect=true";

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
        return "/inward/inward_appointment?faces-redirect=true";
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
        return "/opd_bill_package_medical?faces-redirect=true";
    }

    public String navigateToBillingForCashierFromPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        opdPreBillController.prepareNewBill();
        opdPreBillController.setPatient(getCurrent());
        return "/opd/opd_pre_bill?faces-redirect=true";
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
        return "/opd/opd_pre_bill?faces-redirect=true";
    }

    public String navigateToReceiveDepositsFromPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }

        if (current.getHasAnAccount() == null) {
            JsfUtil.addErrorMessage("Patient has No Account");
            return "";
        }
        if (!current.getHasAnAccount()) {
            JsfUtil.addErrorMessage("Patient has No Account");
            return "";
        }

        paymentMethodData = new PaymentMethodData();
        bill = new Bill();
        billItem = new BillItem();
        billItems = new ArrayList<>();
        printPreview = false;
        return "/payments/patient/receive?faces-redirect=true";
    }

    public void clearDataForPatientDeposite() {
        current = null;
        paymentMethodData = new PaymentMethodData();
        bill = new Bill();
        billItem = new BillItem();
        billItems = new ArrayList<>();
        printPreview = false;
    }

    public void preparePatientDepositCancel() {
        cancelBill = new CancelledBill();
        current = getBill().getPatient();
    }

    public void clearDataForPatientRefund() {
        current = null;
        paymentMethodData = new PaymentMethodData();
        bill = new Bill();
        billItem = new BillItem();
        billItems = new ArrayList<>();
        printPreview = false;
    }

    public String navigateToCollectingCenterBillingFromPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }

        collectingCentreBillController.prepareNewBill();
        collectingCentreBillController.setPatient(getCurrent());
        collectingCentreBillController.setCcBillSettlingStarted(false);
        return "/collecting_centre/bill?faces-redirect=true";
    }

    public String navigateToOpdPatientEdit() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        reGenerateePhn = webUserController.hasPrivilege("EditData");

        return "/opd/patient_edit?faces-redirect=true";
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

        return "/opd/patient?faces-redirect=true";
    }

    public String navigateToOpdBillFromOpdPatient() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        return opdBillController.navigateToNewOpdBill(current);
    }

    public String navigateToSaleFromOpticianRepair() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        return opticianRepairBillController.navigateToNewOpdBill(current);
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
        return "/opd/opd_bill?faces-redirect=true";
    }

    public String navigateToConvertOldPatientPhoneNumbers() {
        return "/dataAdmin/phone_number_converter";
    }

    public String navigateToSearchPatients() {
        setSearchedPatients(null);
        return "/opd/patient_search?faces-redirect=true";
    }

    public String navigateToPatientAcceptPayment() {
        setSearchedPatients(null);
        return "/opd/patient_accept_payment?faces-redirect=true";
    }

    public String navigateToPatientRefundPayment() {
        setSearchedPatients(null);
        return "/opd/patient_refund_payment?faces-redirect=true";
    }

    public void createPatientInvestigationsTableAllByLoggedInstitution() {

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  "
                + " and b.institution =:ins ";

        Map temMap = new HashMap();
        temMap.put("ins", getSessionController().getInstitution());
        sql += " order by pi.approveAt desc  ";

//        patientInvestigations = getPatientInvestigationFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        // patientInvestigations=
    }

    public void setBillNetTotal() {
        if (bill.getPaymentMethod() == PaymentMethod.Card) {
            bill.setNetTotal(getPaymentMethodData().getCreditCard().getTotalValue());
        } else if (bill.getPaymentMethod() == PaymentMethod.Cheque) {
            bill.setNetTotal(getPaymentMethodData().getCheque().getTotalValue());
        } else if (bill.getPaymentMethod() == PaymentMethod.ewallet) {
            bill.setNetTotal(getPaymentMethodData().getEwallet().getTotalValue());
        } else if (bill.getPaymentMethod() == PaymentMethod.Slip) {
            bill.setNetTotal(getPaymentMethodData().getSlip().getTotalValue());
        } else if (bill.getPaymentMethod() == PaymentMethod.Credit) {
            bill.setNetTotal(getPaymentMethodData().getCredit().getTotalValue());
        }
    }

    public void listnerForPaymentMethodChange() {
        if (patientDepositController.getCurrent() != null) {
            if (bill.getPaymentMethod() == PaymentMethod.Card) {
                getPaymentMethodData().getCreditCard().setTotalValue(patientDepositController.getCurrent().getBalance());
            } else if (bill.getPaymentMethod() == PaymentMethod.Cheque) {
                getPaymentMethodData().getCheque().setTotalValue(patientDepositController.getCurrent().getBalance());
            } else if (bill.getPaymentMethod() == PaymentMethod.ewallet) {
                getPaymentMethodData().getEwallet().setTotalValue(patientDepositController.getCurrent().getBalance());
            } else if (bill.getPaymentMethod() == PaymentMethod.Slip) {
                getPaymentMethodData().getSlip().setTotalValue(patientDepositController.getCurrent().getBalance());
            } else if (bill.getPaymentMethod() == PaymentMethod.Credit) {
                getPaymentMethodData().getCredit().setTotalValue(patientDepositController.getCurrent().getBalance());
            } else {
                bill.setNetTotal(patientDepositController.getCurrent().getBalance());
            }
        }
    }

    public boolean validatePaymentMethodData() {
        boolean error = false;

        if (bill.getPaymentMethod() == PaymentMethod.Card) {
            if (getPaymentMethodData().getCreditCard().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Patient Deposit - CreditCard Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please Enter a Credit Card Comment..");
                error = true;
            }
            if (getPaymentMethodData().getCreditCard().getTotalValue() <= 0) {
                JsfUtil.addErrorMessage("Entered Value is Wrong..");
                error = true;
            }
        } else if (bill.getPaymentMethod() == PaymentMethod.Cheque) {
            if (getPaymentMethodData().getCheque().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Patient Deposit - Cheque Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please Enter a Cheque Comment..");
                error = true;
            }
            if (getPaymentMethodData().getCheque().getTotalValue() <= 0) {
                JsfUtil.addErrorMessage("Entered Value is Wrong..");
                error = true;
            }
        } else if (bill.getPaymentMethod() == PaymentMethod.ewallet) {
            if (getPaymentMethodData().getEwallet().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Patient Deposit - E-Wallet Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please Enter a E-Wallet Comment..");
                error = true;
            }
            if (getPaymentMethodData().getEwallet().getTotalValue() <= 0) {
                JsfUtil.addErrorMessage("Entered Value is Wrong..");
                error = true;
            }
        } else if (bill.getPaymentMethod() == PaymentMethod.Slip) {
            if (getPaymentMethodData().getSlip().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Patient Deposit - Slip Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please Enter a Slip Comment..");
                error = true;
            }
            if (getPaymentMethodData().getSlip().getTotalValue() <= 0) {
                JsfUtil.addErrorMessage("Entered Value is Wrong..");
                error = true;
            }
        } else if (bill.getPaymentMethod() == PaymentMethod.Credit) {
            if (getPaymentMethodData().getCredit().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Patient Deposit - Credit Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please Enter a Credit Comment..");
                error = true;
            }
            if (getPaymentMethodData().getCredit().getTotalValue() <= 0) {
                JsfUtil.addErrorMessage("Entered Value is Wrong..");
                error = true;
            }
        } else if (bill.getPaymentMethod() == PaymentMethod.Cash) {
            if (getPaymentMethodData().getCash().getTotalValue() <= 0) {
                JsfUtil.addErrorMessage("Please Enter Correct Value");
                error = true;
            }
        }
        return error;
    }

    public void settlePatientDepositReceive() {
        if (getBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a Payment Method");
            return;
        }
//        if (!current.getHasAnAccount()) {
//            JsfUtil.addErrorMessage("Please Create Patient Account");
//            return;
//        }
        if (paymentSchemeController.checkPaymentMethodError(getBill().getPaymentMethod(), paymentMethodData)) {
            JsfUtil.addErrorMessage("Please enter all relavent Payment Method Details");
            return;
        }

        settleBill(BillType.PatientPaymentReceiveBill, HistoryType.PatientDeposit, BillNumberSuffix.PD, current);

        printPreview = true;
    }

    public int settlePatientDepositReceiveNew() {
        if (getBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a Payment Method");
            return 1;
        }
//        if (!current.getHasAnAccount()) {
//            JsfUtil.addErrorMessage("Please Create Patient Account");
//            return;
//        }
        if (paymentSchemeController.checkPaymentMethodError(getBill().getPaymentMethod(), paymentMethodData)) {
            JsfUtil.addErrorMessage("Please enter all relavent Payment Method Details");
            return 2;
        }

        settleBill(BillType.PatientPaymentReceiveBill, HistoryType.PatientDeposit, BillNumberSuffix.PD, current);

        printPreview = true;
        return 0;
    }

    public int settlePatientDepositReceiveCancelNew() {
        if (getCancelBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a Payment Method");
            return 1;
        }
//        if (!current.getHasAnAccount()) {
//            JsfUtil.addErrorMessage("Please Create Patient Account");
//            return;
//        }
        if (paymentSchemeController.checkPaymentMethodError(getCancelBill().getPaymentMethod(), paymentMethodData)) {
            JsfUtil.addErrorMessage("Please enter all relavent Payment Method Details");
            return 2;
        }

        PaymentMethod tempPm = getCancelBill().getPaymentMethod();
        String tempComment = getCancelBill().getComments();

        cancelBill.copy(getBill());
        getCancelBill().setPaymentMethod(tempPm);
        getCancelBill().setComments(tempComment);
        getBill().setCancelled(true);
        getBill().setCancelledBill(getCancelBill());
        getCancelBill().setReferenceBill(cancelBill);
        getCancelBill().setNetTotal(0 - getBill().getNetTotal());

        settleCancelBill(BillType.PatientPaymentCanceldBill, HistoryType.PatientDeposit, BillNumberSuffix.PDC, current);

        billFacade.edit(getCancelBill());
        billFacade.edit(getBill());

        printPreview = true;
        return 0;
    }

    private boolean errorCheck() {
        if (paymentSchemeController.checkPaymentMethodError(getBill().getPaymentMethod(), paymentMethodData)) {
            return true;
        }
        return false;
    }

    public void settleBill(BillType billType, HistoryType historyType, BillNumberSuffix billNumberSuffix, Patient patient) {

        saveBill(billType, billNumberSuffix, patient);
        billBeanController.setPaymentMethodData(getBill(), getBill().getPaymentMethod(), getPaymentMethodData());
        addToBill();
        saveBillItem();
        getBill().setBillTypeAtomic(BillTypeAtomic.PATIENT_DEPOSIT);
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

    public void settleCancelBill(BillType billType, HistoryType historyType, BillNumberSuffix billNumberSuffix, Patient patient) {

        saveCancelBill(billType, billNumberSuffix, patient);
        billBeanController.setPaymentMethodData(getBill(), getCancelBill().getPaymentMethod(), getPaymentMethodData());
        addToCancelBill();
        saveBillItem();
        getCancelBill().setBillTypeAtomic(BillTypeAtomic.PATIENT_DEPOSIT_CANCELLED);
        billFacade.edit(getCancelBill());
        //TODO: Add Patient Balance History
        if (patient.getRunningBalance() == null) {
            patient.setRunningBalance(getCancelBill().getNetTotal());
        } else {
            patient.setRunningBalance(patient.getRunningBalance() + getCancelBill().getNetTotal());
        }
        getFacade().edit(patient);

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public String navigateToPatientDepositRefund() {
        createNewPatientDepositRefund();
        return "/payments/patient/send?faces-redirect=true";
    }

    public String navigateToPatientDepositRefundFromOPDBill(Patient patient) {
        current = patient;
        bill = new Bill();
        paymentMethodData = null;
        printPreview = false;
        return "/payments/patient/send?faces-redirect=true";
    }

    public void makeNull() {
        current = null;
        paymentMethodData = null;
        printPreview = false;
    }

    public void createNewPatientDepositRefund() {
        makeNull();
        bill = new Bill();
    }

    public void settlePatientDepositReturn() {
        if (getPatient().getId() == null) {
            JsfUtil.addErrorMessage("Please Create Patient Account");
            return;
        }
        if (getBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a Payment Method");
            return;
        }

//        if (!getPatient().getHasAnAccount() || getPatient().getHasAnAccount() == null) {
//            JsfUtil.addErrorMessage("Patient has No Account");
//            return;
//        }
        if (getBill().getNetTotal() <= 0.0) {
            JsfUtil.addErrorMessage("The Refunded Value is Missing");
            return;
        }

        if (getPatient().getRunningBalance() < getBill().getNetTotal()) {
            JsfUtil.addErrorMessage("The Refunded Value is more than the Current Deposit Value of the Patient");
            return;
        }

        if (getBill().getComments().trim().equalsIgnoreCase("")) {
            JsfUtil.addErrorMessage("Please Add Comment");
            return;
        }

        if (paymentSchemeController.checkPaymentMethodError(getBill().getPaymentMethod(), paymentMethodData)) {
            JsfUtil.addErrorMessage("Please enter all relavent Payment Method Details");
            return;
        }
        settleReturnBill(BillType.PatientPaymentRefundBill, HistoryType.PatientDepositReturn, BillNumberSuffix.PDR, current, BillTypeAtomic.PATIENT_DEPOSIT_REFUND, BillClassType.RefundBill);
        printPreview = true;
    }

    public int settlePatientDepositReturnNew() {
        if (getPatient().getId() == null) {
            JsfUtil.addErrorMessage("Please Create Patient Account");
            return 1;
        }
        if (getBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a Payment Method");
            return 2;
        }

//        if (!getPatient().getHasAnAccount() || getPatient().getHasAnAccount() == null) {
//            JsfUtil.addErrorMessage("Patient has No Account");
//            return;
//        }
        if (getBill().getNetTotal() <= 0.0) {
            JsfUtil.addErrorMessage("The Refunded Value is Missing");
            return 3;
        }

//        if (getPatient().getRunningBalance() < getBill().getNetTotal()) {
//            JsfUtil.addErrorMessage("The Refunded Value is more than the Current Deposit Value of the Patient");
//            System.out.println("error 4 = ");
//            return 4;
//        }
        if (getBill().getPaymentMethod() == PaymentMethod.Cash) {
            if (getBill().getComments().trim().equalsIgnoreCase("")) {
                JsfUtil.addErrorMessage("Please Add Comment");
                return 5;
            }
        }

        if (paymentSchemeController.checkPaymentMethodError(getBill().getPaymentMethod(), paymentMethodData)) {
            JsfUtil.addErrorMessage("Please enter all relavent Payment Method Details");
            return 6;
        }
        settleReturnBill(BillType.PatientPaymentRefundBill, HistoryType.PatientDepositReturn, BillNumberSuffix.PDR, current, BillTypeAtomic.PATIENT_DEPOSIT_REFUND, BillClassType.RefundBill);
        printPreview = true;
        return 0;
    }

    public void settleReturnBill(BillType billType, HistoryType historyType, BillNumberSuffix billNumberSuffix, Patient patient, BillTypeAtomic billTypeAtomic, BillClassType billClassType) {
        saveBill(billType, billNumberSuffix, patient, billTypeAtomic, billClassType);
        billBeanController.setPaymentMethodData(getBill(), getBill().getPaymentMethod(), getPaymentMethodData());
        addToBill();
        saveBillItem();
        billFacade.edit(getBill());
        //TODO: Add Patient Balance History
        PatientDeposit corremtPatientDeposit = patientDepositController.checkDepositOfThePatient(patient, sessionController.getDepartment());
        patient.setRunningBalance(Math.abs(corremtPatientDeposit.getBalance()) - Math.abs(getBill().getNetTotal()));
        getFacade().edit(patient);

        JsfUtil.addSuccessMessage("Bill Saved");
    }

    private void saveBill(BillType billType, BillNumberSuffix billNumberSuffix, Patient patient, BillTypeAtomic billTypeAtomic, BillClassType billClassType) {
        getBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), billType, BillClassType.BilledBill, billNumberSuffix));
//        getBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(sessionController.getDepartment(), billType, BillClassType.BilledBill, billNumberSuffix));
        getBill().setDeptId(getBillNumberGenerator().departmentBillNumberGenerator(sessionController.getDepartment(), sessionController.getDepartment(), billType, BillClassType.BilledBill));
        getBill().setBillType(billType);
        getBill().setBillClassType(billClassType);
        getBill().setPatient(patient);

        getBill().setCreatedAt(new Date());
        getBill().setCreater(sessionController.getLoggedUser());
        getBill().setBillDate(new Date());
        getBill().setBillTime(new Date());

        getBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getBill().setInstitution(getSessionController().getLoggedUser().getInstitution());

        getBill().setCreatedAt(new Date());
        getBill().setCreater(getSessionController().getLoggedUser());
        Double refundingValue = Math.abs(getBill().getNetTotal());
        getBill().setGrantTotal(-refundingValue);
        getBill().setTotal(-refundingValue);
        getBill().setNetTotal(-refundingValue);
        getBill().setDiscount(0.0);
        getBill().setDiscountPercent(0);
        getBill().setBillTypeAtomic(billTypeAtomic);

        if (getBill().getId() == null) {
            billFacade.create(getBill());
        } else {
            billFacade.edit(getBill());
        }
    }

    public void pasteCurrentPatientRunningBalance() {
        if (current != null) {
            getBill().setNetTotal(current.getRunningBalance());
        } else {
            JsfUtil.addErrorMessage("Please Select the Patient");
        }

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

    public void addToCancelBill() {
        getBillItem().setNetValue(0 - getBill().getNetTotal());
        getBillItem().setGrossValue(0 - getBill().getNetTotal());
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
//        getBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(sessionController.getDepartment(), billType, BillClassType.BilledBill, billNumberSuffix));
        getBill().setDeptId(getBillNumberGenerator().departmentBillNumberGenerator(sessionController.getDepartment(), sessionController.getDepartment(), billType, BillClassType.BilledBill));
        getBill().setBillType(billType);
        getBill().setBillClassType(BillClassType.BilledBill);
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

    private void saveCancelBill(BillType billType, BillNumberSuffix billNumberSuffix, Patient patient) {
        getCancelBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), billType, BillClassType.CancelledBill, billNumberSuffix));
//        getCancelBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(sessionController.getDepartment(), billType, BillClassType.CancelledBill, billNumberSuffix));
        getCancelBill().setDeptId(getBillNumberGenerator().departmentBillNumberGenerator(sessionController.getDepartment(), sessionController.getDepartment(), billType, BillClassType.CancelledBill));
        getCancelBill().setBillType(billType);

        getCancelBill().setPatient(patient);

        getCancelBill().setCreatedAt(new Date());
        getCancelBill().setCreater(sessionController.getLoggedUser());
        getCancelBill().setBillDate(new Date());
        getCancelBill().setBillTime(new Date());

        getCancelBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCancelBill().setInstitution(getSessionController().getLoggedUser().getInstitution());

        getCancelBill().setCreatedAt(new Date());
        getCancelBill().setCreater(getSessionController().getLoggedUser());

        getCancelBill().setGrantTotal(0 - getBill().getNetTotal());
        getCancelBill().setTotal(0 - getBill().getNetTotal());
        getCancelBill().setDiscount(0.0);
        getCancelBill().setDiscountPercent(0);

        if (getCancelBill().getId() == null) {
            billFacade.create(getCancelBill());
        } else {
            billFacade.edit(getCancelBill());
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
        return "/emr/patient_search?faces-redirect=true";
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
        return "/membership/change_membership?faces-redirect=true";
    }

    public String toAddToQueueFromSearchPatients() {
        if (current == null) {
            JsfUtil.addErrorMessage("No Patient Selected");
            return "";
        }
        patientSelected();
        return "/emr/patient_add_to_queue?faces-redirect=true";
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

    public Boolean searchPatientCommon() {
        boolean noSearchCriteriaWasFound = true;

        if (searchPhn != null && !searchPhn.trim().equals("")) {
            noSearchCriteriaWasFound = false;
        }

        if (searchMrn != null && !searchMrn.trim().equals("")) {
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
            return false;
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

        return true;
    }

    public String searchPatientForOpd() {
        boolean noError = searchPatientCommon();
        if (!noError) {
            return "";
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

    public String searchPatientForPatientDepost() {
        boolean noError = searchPatientCommon();
        if (!noError) {
            return "";
        }
        if (searchedPatients == null || searchedPatients.isEmpty()) {
            JsfUtil.addErrorMessage("No Matches. Please use different criteria");
            return "";
        } else if (searchedPatients.size() == 1) {
            setCurrent(searchedPatients.get(0));
        }
        clearSearchDetails();
        return "";
    }

    public String searchPatientForOptician() {
        boolean noError = searchPatientCommon();
        if (!noError) {
            return "";
        }
        if (searchedPatients == null || searchedPatients.isEmpty()) {
            JsfUtil.addErrorMessage("No Matches. Please use different criteria");
            return navigateToAddNewPatientForOptician(getSearchName(), getSearchNic(), getSearchPhone());
        } else if (searchedPatients.size() == 1) {
            setCurrent(searchedPatients.get(0));
            return navigateToOpticianProfile();
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
        searchMrn = null;
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

        if (searchMrn != null && !searchMrn.trim().equals("")) {
            j += " and p.phn =:mrn";
            m.put("mrn", searchMrn);
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
        boolean checkOnlyNumeric = CommonFunctions.checkOnlyNumeric(quickSearchPhoneNumber);
        Patient patientSearched = null;
        boolean usePHN = false;
        String j;
        Map m = new HashMap();
        
        admissionController.setPatientAllergies(null);

        if (checkOnlyNumeric) {
            j = "select p from Patient p where p.retired=false and (p.patientPhoneNumber=:pp or p.patientMobileNumber=:pp)";
            Long searchedPhoneNumber = CommonFunctions.removeSpecialCharsInPhonenumber(quickSearchPhoneNumber);
            m.put("pp", searchedPhoneNumber);
            quickSearchPatientList = getFacade().findByJpql(j, m);
        } else {
            if (!quickSearchPhoneNumber.trim().isEmpty()) {
                quickSearchPatientList = findPatientUsingPhnNumber(quickSearchPhoneNumber);
                usePHN = true;
            }

        }
//        controller.setPaymentMethod(null);
        if (quickSearchPatientList == null) {

            controller.setPatient(null);
            if (!usePHN) {
                controller.getPatient().setPhoneNumberStringTransient(quickSearchPhoneNumber);
                controller.getPatient().setMobileNumberStringTransient(quickSearchPhoneNumber);
            }
            controller.setPatientDetailsEditable(true);
            JsfUtil.addErrorMessage("No Patient found !");
            return;
        } else if (quickSearchPatientList.isEmpty()) {

            controller.setPatient(null);
            if (!usePHN) {
                controller.getPatient().setPhoneNumberStringTransient(quickSearchPhoneNumber);
                controller.getPatient().setMobileNumberStringTransient(quickSearchPhoneNumber);
            }
            controller.setPatientDetailsEditable(true);
            JsfUtil.addErrorMessage("No Patient found !");
            return;
        } else if (quickSearchPatientList.size() == 1) {
            patientSearched = quickSearchPatientList.get(0);
            controller.setPatient(patientSearched);
            controller.setPatientDetailsEditable(false);
//            controller.setPaymentMethod(null);
            System.out.println("line 2071"+patientSearched);
            admissionController.fillCurrentPatientAllergies(patientSearched);//TODO
            System.out.println("line 2071"+current);
            boolean automaticallySetPatientDeposit = configOptionApplicationController.getBooleanValueByKey("Automatically set the PatientDeposit payment Method if a Deposit is Available", false);
            System.out.println("One patient found - controller.getPatient().getHasAnAccount() = " + controller.getPatient().getHasAnAccount());
            if (controller.getPatient().getHasAnAccount() != null) {
                if (controller.getPatient().getHasAnAccount() && automaticallySetPatientDeposit) {
                    controller.setPatient(controller.getPatient());
                    controller.setPaymentMethod(PaymentMethod.PatientDeposit);
                }
            }
            controller.listnerForPaymentMethodChange();
            quickSearchPatientList = null;
        } else {
            controller.setPatient(null);
            patientSearched = null;
            this.current = null;
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
        controller.setPatientDetailsEditable(false);
//        controller.setPaymentMethod(null);

        admissionController.fillCurrentPatientAllergies(current); //TODO

        boolean automaticallySetPatientDeposit = configOptionApplicationController.getBooleanValueByKey("Automatically set the PatientDeposit payment Method if a Deposit is Available", false);
        System.out.println("Select Patient - controller.getPatient().getHasAnAccount() = " + controller.getPatient().getHasAnAccount());
        if (controller.getPatient().getHasAnAccount() != null) {
            if (controller.getPatient().getHasAnAccount() && automaticallySetPatientDeposit) {
                controller.setPatient(controller.getPatient());
                controller.setPaymentMethod(PaymentMethod.PatientDeposit);
                controller.listnerForPaymentMethodChange();
            }
        }
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

    //    @Deprecated
//    public String toAddAFamily() {
//        currentFamily = new Family();
//        return "/membership/add_family";
//    }
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
        if (currentFamily == null) {
            JsfUtil.addErrorMessage("No Family is Selected");
            return null;
        }
        familyMembers = membershipService.fetchFamilyMembers(currentFamily);
        return "/membership/family_membership_manage?faces-redirect=true";
    }

    public String navigateToManageFamily(Family family) {
        if (family == null) {
            JsfUtil.addErrorMessage("No Family is Selected");
            return null;
        }
        currentFamily = family;
        familyMembers = membershipService.fetchFamilyMembers(currentFamily);
        return "/membership/family_membership_manage?faces-redirect=true";
    }

    public String deleteFamilyMembership() {
        if (currentFamily == null) {
            JsfUtil.addErrorMessage("No Family is Selected");
            return null;
        }
        membershipService.deleteFamily(currentFamily, sessionController.getLoggedUser());
        families = null;
        JsfUtil.addSuccessMessage("Family Deleted.");
        return navigateToSearchFamilyMembership();
    }

    public String deleteFamilyMembershipAndMembers() {
        if (currentFamily == null) {
            JsfUtil.addErrorMessage("No Family is Selected");
            return null;
        }
        membershipService.deleteFamilyAndMembers(currentFamily, sessionController.getLoggedUser());
        families = null;
        JsfUtil.addSuccessMessage("Family and Members Deleted.");
        return navigateToSearchFamilyMembership();
    }

    public String navigateToManageIndividualMembership() {
        if (current == null) {
            JsfUtil.addErrorMessage("No Patient is Selected");
            return null;
        }
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
        String jpql = "Select f "
                + "from Family f "
                + "where f.retired=false "
                + "and (f.phoneNo = :pn or f.membershipCardNo = :mcn) ";
        Map params = new HashMap();
        Long mcn;
        try {
            mcn = Long.parseLong(searchMembershipCardNo);
        } catch (Exception e) {
            mcn = 0L;
        }
        params.put("pn", searchMembershipCardNo);
        params.put("mcn", mcn);
        List<Family> fs = getFamilyFacade().findByJpql(jpql, params);
        if (fs == null || fs.isEmpty()) {
            JsfUtil.addErrorMessage("No matching families found");
            return "";
        } else if (fs.size() == 1) {
            currentFamily = fs.get(0);
            searchMembershipCardNo = "";
            return navigateToManageFamilyMembership();
        } else {
            families = fs;
            searchMembershipCardNo = "";
            return null;
        }
    }

    public String searchFamilyByChhName() {
        if (searchText == null) {
            JsfUtil.addErrorMessage("No Search Text");
            return null;
        }
        if (searchText.trim().isEmpty()) {
            JsfUtil.addErrorMessage("No Search Text");
            return null;
        }
        if (searchText.trim().length() < 4) {
            JsfUtil.addErrorMessage("Enter At Least 4 charactors");
            return null;
        }
        families = null;
        String jpql = "Select f "
                + " from Family f "
                + " where f.retired=false "
                + " and f.chiefHouseHolder.person.name like :pn "
                + " order by f.chiefHouseHolder.person.name";
        Map params = new HashMap();
        params.put("pn", "%" + searchText + "%");
        List<Family> fs = getFamilyFacade().findByJpql(jpql, params);
        if (fs == null || fs.isEmpty()) {
            JsfUtil.addErrorMessage("No matching families found");
            return "";
        } else if (fs.size() == 1) {
            currentFamily = fs.get(0);
            searchText = "";
            return navigateToManageFamilyMembership();
        } else {
            families = fs;
            searchText = "";
            return null;
        }
    }

    public String searchFamilyByNIC() {
        if (searchNic == null) {
            JsfUtil.addErrorMessage("No NIC provided");
            return null;
        }
        if (searchNic.trim().isEmpty()) {
            JsfUtil.addErrorMessage("No NIC provided");
            return null;
        }

        families = null;
        String jpql = "Select f "
                + " from Family f "
                + " where f.retired=false "
                + " and f.chiefHouseHolder.person.nic like :nic "
                + " order by f.chiefHouseHolder.person.name";
        Map params = new HashMap();
        params.put("nic", "%" + searchNic.trim().toUpperCase() + "%");

        List<Family> fs = getFamilyFacade().findByJpql(jpql, params);
        if (fs == null || fs.isEmpty()) {
            JsfUtil.addErrorMessage("No matching families found");
            return "";
        } else if (fs.size() == 1) {
            currentFamily = fs.get(0);
            searchNic = "";
            return navigateToManageFamilyMembership();
        } else {
            families = fs;
            searchNic = "";
            return null;
        }
    }

    public String searchFamilyByAddress() {
        if (searchPatientAddress == null) {
            JsfUtil.addErrorMessage("No Search Text");
            return null;
        }
        if (searchPatientAddress.trim().isEmpty()) {
            JsfUtil.addErrorMessage("No Search Text");
            return null;
        }
        if (searchPatientAddress.trim().length() < 4) {
            JsfUtil.addErrorMessage("Enter at least 4 characters");
            return null;
        }
        families = null;
        String jpql = "Select f "
                + " from Family f "
                + " where f.retired=false "
                + " and f.chiefHouseHolder.person.address like :address "
                + " order by f.chiefHouseHolder.person.name";
        Map params = new HashMap();
        params.put("address", "%" + searchPatientAddress + "%");
        List<Family> fs = getFamilyFacade().findByJpql(jpql, params);
        if (fs == null || fs.isEmpty()) {
            JsfUtil.addErrorMessage("No matching families found");
            return "";
        } else if (fs.size() == 1) {
            currentFamily = fs.get(0);
            searchPatientAddress = "";
            return navigateToManageFamilyMembership();
        } else {
            families = fs;
            searchPatientAddress = "";
            return null;
        }
    }


    public Family fetchFamilyFromMembershipNumber(String paramMembershipNumber, MembershipScheme paramMembershipScheme, String phoneNumber) {
        if (paramMembershipNumber == null) {
            return null;
        }
        String membershipNumberWithDigitsOnly = CommonFunctions.getDigitsOnlyByRemovingWhitespacesAndNonDigitCharacters(paramMembershipNumber);
        Long membershipNumberLong = CommonFunctions.convertStringToLongOrNull(membershipNumberWithDigitsOnly);
        return fetchFamilyFromMembershipNumber(membershipNumberLong, paramMembershipScheme, phoneNumber);
    }

    public Family fetchFamilyFromMembershipNumber(Long membershipNumber, MembershipScheme paramMembershipScheme, String phoneNumber) {
        if (membershipNumber == null) {
            return null;
        }
        String jpql = "Select f from Family f where f.retired=false and f.membershipCardNo = :mcn";
        Map<String, Object> params = new HashMap<>();
        params.put("mcn", membershipNumber);
        Family fm = getFamilyFacade().findFirstByJpql(jpql, params);
        if (fm == null) {
            fm = new Family();
            fm.setMembershipCardNo(membershipNumber);
            fm.setMembershipScheme(paramMembershipScheme);
            fm.setCreatedAt(new Date());
            fm.setCreatedDepartment(sessionController.getDepartment());
            fm.setCreatedInstitution(sessionController.getInstitution());
            fm.setCreater(sessionController.getLoggedUser());
            fm.setPhoneNo(phoneNumber);
            familyFacade.create(fm);
        } else {
            fm.setMembershipScheme(paramMembershipScheme);
            fm.setPhoneNo(phoneNumber);
            familyFacade.edit(fm);
        }
        return fm;
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
        if (currentFamily.getPhoneNo() == null) {
            currentFamily.setPhoneNo(current.getPerson().getPhone());
        }
        currentFamily.setChiefHouseHolder(current);
        getFamilyFacade().edit(currentFamily);
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
        return navigateToAddNewFamilyMembership();
    }

    //    public String saveAndClearForNewFamily() {
//        saveFamily();
//        currentFamily = new Family();
//        return navigateToAddNewFamilyMembership();
//    }
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

    //    public String toAddNewFamily() {
//        currentFamily = new Family();
//        return toFamily();
//    }
//    public String toFamily() {
//        return "/membership/add_family?faces-redirect=true";
//    }
    public String toNewPatient() {
        prepareAdd();
        return "/membership/patient?faces-redirect=true";
    }

    public void clearPatientToAddNewMemberToFamily() {
        current = new Patient();
    }

    public void addNewMemberToFamily() {
        if (currentFamily == null) {
            JsfUtil.addErrorMessage("No Family Selected.");
            return;
        }
        if (current == null) {
            JsfUtil.addErrorMessage("No Member is selected to add to family.");
            return;
        }
        if (current.getPerson() == null) {
            JsfUtil.addErrorMessage("No Member is selected to add to family.");
            return;
        }
        if (current.getPerson().getName() == null || current.getPerson().getName().trim().isEmpty()) {
            JsfUtil.addErrorMessage("No Name for the Member to add to family.");
            return;
        }
        if (currentFamily.getMembershipScheme() == null) {
            JsfUtil.addErrorMessage("No Membership Scheme for the family.");
            return;
        }
        if (currentRelation == null) {
            JsfUtil.addErrorMessage("No relationship.");
            return;
        }
        current.getPerson().setMembershipScheme(currentFamily.getMembershipScheme());
        save(current);
        saveFamily();
        FamilyMember tfm = new FamilyMember();
        tfm.setPatient(current);
        tfm.setFamily(currentFamily);
        tfm.setCreatedAt(new Date());
        tfm.setCreater(sessionController.getLoggedUser());
        tfm.setRelationToChh(currentRelation);
        getFamilyMemberFacade().create(tfm);
        currentFamily.getFamilyMembers().add(tfm);
        if (currentFamily.getChiefHouseHolder() == null) {
            currentFamily.setChiefHouseHolder(current);
        }
        if (currentFamily.getPhoneNo() == null || currentFamily.getPhoneNo().trim().equals("")) {
            currentFamily.setPhoneNo(current.getPerson().getPhone());
        }
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
        return "/emr/patient_basic_info?faces-redirect=true";
    }

    public String toPatientFromSearchPatientsProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("No Patient Selected");
            return "";
        }
        patientSelected();
        return "/emr/patient_profile?faces-redirect=true";
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
            }
        } else {
            try {
                Barcode bc = BarcodeFactory.createCode128A("0000");
                bc.setBarHeight(5);
                bc.setBarWidth(3);
                bc.setDrawingText(true);
                BarcodeImageHandler.saveJPEG(bc, barcodeFile);
                InputStream targetStream = new FileInputStream(barcodeFile);
                StreamedContent str = DefaultStreamedContent.builder().contentType("image/jpeg").name(barcodeFile.getName()).stream(() -> targetStream).build();
                barcode = str;
            } catch (Exception ex) {
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
        getCurrent().getPerson().setDob(CommonFunctions.guessDob(yearMonthDay));
    }

    @Deprecated
    public void dobChangeListen() {
        yearMonthDay = CommonFunctions.guessAge(getCurrent().getPerson().getDob());
    }

    public StreamedContent getPhoto(Patient p) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            return new DefaultStreamedContent();
        } else if (p == null) {
            return new DefaultStreamedContent();
        } else {
            if (p.getId() != null && p.getBaImage() != null) {
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
        return "/emr/patient?faces-redirect=true";
    }

    public String navigateToAddNewPatientForOpd() {
        current = null;
        getCurrent();

        reGenerateePhn = true;
        return "/opd/patient_edit?faces-redirect=true";
    }

    public String navigateToAddNewPatientForOptician() {
        current = null;
        getCurrent();
        return "/optician/patient_edit?faces-redirect=true";
    }

    public String navigateToAddNewPatientForOpd(String name, String nic, String phone) {
        current = null;
        getCurrent();
        getCurrent().getPerson().setName(name);
        getCurrent().getPerson().setNic(nic);
        getCurrent().getPerson().setPhone(phone);
        getCurrent().getPerson().setMobile(phone);
        return "/opd/patient_edit?faces-redirect=true";
    }

    public String navigateToAddNewPatientForOptician(String name, String nic, String phone) {
        current = null;
        getCurrent();
        getCurrent().getPerson().setName(name);
        getCurrent().getPerson().setNic(nic);
        getCurrent().getPerson().setPhone(phone);
        getCurrent().getPerson().setMobile(phone);
        return "/optician/patient_edit?faces-redirect=true";
    }

    public String navigateToAddNewPatientForOpd(String phone) {
        current = null;
        getCurrent();
        getCurrent().getPerson().setPhone(phone);
        getCurrent().getPerson().setMobile(phone);
        return "/opd/patient_edit?faces-redirect=true";
    }

    public String navigateToEmrEditPatient() {
        getCurrent();
        return "/emr/patient?faces-redirect=true";
    }

    public String toViewPatient() {
        current = null;
        return "/emr/patient_profile?faces-redirect=true";
    }

    public String savePatientAndThenNavigateToPatientProfile() {
        saveSelectedPatient();
        return toViewPatient();
    }
    
    public void toggleBlacklistPatient( boolean blacklist){
        Patient patient = this.current;
        if(patient == null || patient.getId() == null){
            return;   
        }

        if(blacklist && !patient.isBlacklisted()){
            if(blacklistComment == null || blacklistComment.isEmpty()){
                JsfUtil.addErrorMessage("Please provide a reason for blacklisting. ");
                return;
            }
            Patient newb = getFacade().find(patient.getId());
            newb.setBlacklisted(true);
            newb.setBlacklistedAt(new Date());
            getFacade().edit(newb);
            newb.setBlacklistedBy(sessionController.getLoggedUser());
            newb.setReasonForBlacklist(newb.getReasonForBlacklist() != null ? newb.getReasonForBlacklist() + " / " + blacklistComment  : blacklistComment);
//            getFacade().edit(patient);

            getFacade().editAndCommit(newb);
            this.current = getFacade().findWithoutCache(newb.getId());
            blacklistComment = null;
            JsfUtil.addSuccessMessage("Patient is blacklisted.");
            
        }else if(!blacklist && patient.isBlacklisted()){
            if(blacklistComment == null || blacklistComment.isEmpty()){
                JsfUtil.addErrorMessage("Please provide a reason for revert blacklisting. ");
                return;
            }

            Patient newb = getFacade().find(patient.getId());
            newb.setBlacklisted(false);
            getFacade().edit(newb);
            newb.setReasonForBlacklist(patient.getReasonForBlacklist() +" at " 
                    + newb.getBlacklistedAt() + " by " 
                    + newb.getBlacklistedBy() 
                    + " / revert by " + sessionController.getWebUser() 
                    + " at "+new Date() + " revert comment - " + blacklistComment);
            
            newb.setBlacklistedAt(null);
            newb.setBlacklistedBy(null);

            getFacade().editAndCommit(newb);

            blacklistComment = null;
            JsfUtil.addSuccessMessage("Patient blacklist is reverted.");
        }
    }

    public String deletePatient() {
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
        return navigateToSearchPatients();
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

    /**
     * Applies patient name capitalization based on configuration settings
     *
     * @param patient Patient whose name should be capitalized
     */
    public void applyPatientNameCapitalization(Patient patient) {
        if (patient == null || patient.getPerson() == null) {
            return;
        }

        boolean capitalizeAll = configOptionApplicationController.getBooleanValueByKey("Capitalize Entire Patient Name", false);
        boolean capitalizeEach = configOptionApplicationController.getBooleanValueByKey("Capitalize Each Word in Patient Name", false);
        String personName = patient.getPerson().getName();

        if (personName != null) {
            if (capitalizeAll) {
                personName = personName.toUpperCase();
            } else if (capitalizeEach) {
                personName = CommonFunctions.capitalizeFirstLetter(personName);
            }
            patient.getPerson().setName(personName);
        }
    }

    public boolean saveSelected() {
        return saveSelected(current);
    }

    public String saveSelectedAndToFamily() {
        saveSelected(current);
        boolean savedSuccessfully = saveSelected(current);
        if (!savedSuccessfully) {
            return null;
        }
        return "/membership/add_family?faces-redirect=true";
    }

    public String saveAndNavigateToOpdPatientProfile() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        boolean savedSuccessfully = saveSelected(current);
        if (!savedSuccessfully) {
            return null;
        }
        return "/opd/patient?faces-redirect=true";
    }

    public boolean saveSelected(Patient p) {
        if (p == null) {
            JsfUtil.addErrorMessage("No Current. Error. NOT SAVED");
            return false;
        }
        if (p.getPerson() == null) {
            JsfUtil.addErrorMessage("No Person. Not Saved");
            return false;
        }
        if (p.getPerson().getName() == null) {
            JsfUtil.addErrorMessage("Please enter a name");
            return false;
        }
        if (p.getPerson().getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a name");
            return false;
        }
        if (p.getHasAnAccount() == null) {
            p.setHasAnAccount(false);
        }
        if (p.getHasAnAccount() && p.getCreditLimit() == null) {
            p.setCreditLimit(0.0);
        }
        if (configOptionApplicationController.getBooleanValueByKey("Patients Title is Mandatory", false)) {
            if (p.getPerson().getTitle() == null) {
                JsfUtil.addErrorMessage("Please select title");
                return false;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Patients Gender is Mandatory", false)) {
            if (p.getPerson().getSex() == null) {
                JsfUtil.addErrorMessage("Please select gender");
                return false;
            }
        }
        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Age to Save Patient", false)) {
            if (p.getPerson().getDob() == null) {
                JsfUtil.addErrorMessage("Please select patient date of birth");
                return false;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Patients Area is Mandatory", false)) {
            if (p.getPerson().getArea() == null) {
                JsfUtil.addErrorMessage("Please select area of the patient");
                return false;
            }
        }

        //applyPatientNameCapitalization(p);
//        if (p.getPerson().getId() == null) {
//            p.getPerson().setCreatedAt(Calendar.getInstance().getTime());
//            p.getPerson().setCreater(getSessionController().getLoggedUser());
//            getPersonFacade().create(p.getPerson());
//        } else {
//            getPersonFacade().edit(p.getPerson());
//        }
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
        return true;
    }

    public List<Patient> findPatientUsingPhnNumber(String phn) {
        Map m = new HashMap();
        String j = "select p from Patient p where p.retired=false and p.phn=:phn ";
        m.put("phn", phn.trim());
        List<Patient> searchedPatients = getFacade().findByJpql(j, m);
        //System.out.println("searched Patients From PHN = " + searchedPatients.size());
        return searchedPatients;
    }

    public String searchByPatientPhoneNumberForPatientLookup() {
        boolean checkOnlyNumeric = CommonFunctions.checkOnlyNumeric(searchPatientPhoneNumber);
        boolean usePHN = false;
        if (checkOnlyNumeric) {
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
            //System.out.println("searched Patients From Phone Number = " + searchedPatients.size());
            usePHN = false;
        } else {
            searchedPatients = findPatientUsingPhnNumber(searchPatientPhoneNumber);
            usePHN = true;
        }

        //System.out.println("Use PHN = " + usePHN);
        if (searchedPatients == null || searchedPatients.isEmpty()) {
            if (usePHN) {
                //System.out.println("Use PHN");
                JsfUtil.addErrorMessage("No Matches. Please use Correct PHN");
                return "";
            } else {
                //System.out.println("No Use PHN");
                JsfUtil.addErrorMessage("No Matches. Please use different criteria");
                return navigateToAddNewPatientForOpd(searchPatientPhoneNumber);
            }
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

        //applyPatientNameCapitalization(p);

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
        //applyPatientNameCapitalization(getCurrent());

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
            } else if (searchMrn != null && !searchMrn.isEmpty()) {
                searchByMrn(searchMrn);
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
            } else if (searchMrn != null && !searchMrn.isEmpty()) {
                searchByMrn(searchMrn);
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
        if (searchMrn != null && !searchMrn.isEmpty()) {
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

    private void searchByMrn(String mrn) {
        String j1 = "Select p.person "
                + " from Patient p"
                + " where p.retired=:ret"
                + " and p.code=:mrn ";
        Map m1 = new HashMap();
        m1.put("ret", false);
        m1.put("mrn", mrn);
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

    @Deprecated // Use membershipService.fetchFamilyMembers
    public List<FamilyMember> fetchFamilyMembers(Family family) {
        return membershipService.fetchFamilyMembers(currentFamily);
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
        if (billItem == null) {
            billItem = new BillItem();
        }
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

    public String getSearchMrn() {
        return searchMrn;
    }

    public void setSearchMrn(String searchMrn) {
        this.searchMrn = searchMrn;
    }

    public Bill getCancelBill() {
        return cancelBill;
    }

    public void setCancelBill(Bill cancelBill) {
        this.cancelBill = cancelBill;
    }

    public BillNumberGenerator getBillNumberGenerator() {
        return billNumberGenerator;
    }

    public void setBillNumberGenerator(BillNumberGenerator billNumberGenerator) {
        this.billNumberGenerator = billNumberGenerator;

    }

    public boolean isReGenerateePhn() {
        return reGenerateePhn;
    }

    public void setReGenerateePhn(boolean reGenerateePhn) {
        this.reGenerateePhn = reGenerateePhn;
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    @Override
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getSearchMembershipCardNo() {
        return searchMembershipCardNo;
    }

    public void setSearchMembershipCardNo(String searchMembershipCardNo) {
        this.searchMembershipCardNo = searchMembershipCardNo;
    }

    public String getSearchPatientAddress() {
        return searchPatientAddress;
    }

    public void setSearchPatientAddress(String searchPatientAddress) {
        this.searchPatientAddress = searchPatientAddress;
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
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
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
