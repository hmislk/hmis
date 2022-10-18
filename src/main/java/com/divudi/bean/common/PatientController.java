package com.divudi.bean.common;

import com.divudi.bean.clinical.PatientEncounterController;
import com.divudi.bean.clinical.PracticeBookingController;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.Family;
import com.divudi.entity.FamilyMember;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.entity.Relation;
import com.divudi.entity.WebUser;
import com.divudi.entity.membership.MembershipScheme;
import com.divudi.facade.BillFacade;
import com.divudi.facade.FamilyFacade;
import com.divudi.facade.FamilyMemberFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.facade.util.JsfUtil;
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
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;
import org.primefaces.context.PrimeRequestContext;
//import org.primefaces.context.RequestContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class PatientController implements Serializable {

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
    @EJB
    CommonFunctions commonFunctions;
    @EJB
    BillFacade billFacade;
    @EJB
    private WebUserFacade webUserFacade;
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
    PatientEncounterController PatientEncounterController;
    @Inject
    private CommonController commonController;
    @Inject
    private SecurityController securityController;
    /**
     *
     * Class Variables
     *
     *
     */
    private static final long serialVersionUID = 1L;
    private Patient current;
    private Person familyMember;
    private List<Person> familyMembers;
    Family currentFamily;
    private List<Family> families;
    FamilyMember currentFamilyMember;
    Patient addingPatientToFamily;
    FamilyMember removingFamilyMember;
    Relation currentRelation;
    private String password;

    private List<Patient> items = null;
    private List<Patient> selectedItems = null;

    private MembershipScheme membershipScheme;

    private Date dob;
    private String membershipTypeListner = "1";

    StreamedContent barcode;
    ReportKeyWord reportKeyWord;

    private String searchText;

    public String toChangeMembershipOfSelectedPersons() {
        items = new ArrayList<>();
        return "/membership/change_membership";
    }

    public void listAllPatients() {
        String j = "select p from Patient p where p.retired=false order by p.person.name";
        items = getFacade().findBySQL(j);
    }

    public void listAllMembers() {
        String j = "select p from Patient p where p.retired=false and p.person.membershipScheme is not null order by p.person.name";
        items = getFacade().findBySQL(j);
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

    public String toAddAFamily() {
        currentFamily = new Family();
        return "/membership/add_family";
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
        List<Family> fs = getFamilyFacade().findBySQL(j, m);
        if (fs == null) {
            JsfUtil.addErrorMessage("No matches");
            return "";
        } else if (fs.size() == 1) {
            currentFamily = fs.get(0);
            searchText = "";
            return "/membership/add_family";
        } else {
            families = fs;
            searchText = "";
            return "/membership/search_family";
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

    public String saveAndClearForNewFamily() {
        saveFamily();
        currentFamily = new Family();
        return toFamily();
    }

    public String toAddNewFamily() {
        currentFamily = new Family();
        return toFamily();
    }

    public String toFamily() {
        return "/membership/add_family";
    }

    public String toNewPatient() {
        prepareAdd();
        return "/membership/patient";
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

    public void patientSelected() {
        getPatientEncounterController().fillCurrentPatientLists(current);
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

        if (familyMember.getFullName() == null || familyMember.getFullName().equals("")) {
            loggedIn = false;
            UtilityController.addErrorMessage("Please enter full name");
            return;

        }
        if (familyMember.getSex() == null) {
            loggedIn = false;
            UtilityController.addErrorMessage("Please enter gender");
            return;

        }
        if (familyMember.getNic() == null || familyMember.getNic().equals("")) {
            loggedIn = false;
            UtilityController.addErrorMessage("Please enter NIC no");
            return;
        }
        if (familyMember.getDob() == null) {
            loggedIn = false;
            UtilityController.addErrorMessage("Please enter Date Of Birth");
            return;
        }
        familyMember.setSerealNumber(familyMembers.size());
        familyMembers.add(familyMember);
        loggedIn = true;

        familyMember = null;

//        context.addCallbackParam("loggedIn", loggedIn);
        PrimeRequestContext.getCurrentInstance().getCallbackParams().put("loggedIn", loggedIn);
    }

    public void removeFamilyMember(Person p) {

        familyMembers.remove(p.getSerealNumber());
        int i = 0;
        for (Person familyMember1 : familyMembers) {
            familyMember1.setSerealNumber(i);
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

    private YearMonthDay yearMonthDay;

    public YearMonthDay getYearMonthDay() {
        if (yearMonthDay == null) {
            yearMonthDay = new YearMonthDay();

        }
        return yearMonthDay;
    }

    public void setYearMonthDay(YearMonthDay yearMonthDay) {
        this.yearMonthDay = yearMonthDay;
    }

    public void dateChangeListen() {
        getCurrent().getPerson().setDob(getCommonFunctions().guessDob(yearMonthDay));
    }

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

    public StreamedContent getPhotoByByte(byte[] p) {
        //////System.out.println("p is " + p);
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            return new DefaultStreamedContent();
        } else if (p == null) {
            return new DefaultStreamedContent();
        } else {
            InputStream targetStream = new ByteArrayInputStream(p);
            StreamedContent str = DefaultStreamedContent.builder().contentType("image/png").name("photo.png").stream(() -> targetStream).build();
            return  str;
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

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfull");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
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
                    + " and upper(p.person.name) like :q "
                    + " or upper(p.code) like :q "
                    + " or upper(p.person.nic) like :q"
                    + " or upper(p.person.mobile) like :q "
                    + "  order by p.person.name";
            hm.put("q", "%" + query.toUpperCase() + "%");
            //////System.out.println(sql);
            suggestions = getFacade().findBySQL(sql, hm, 20);
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
                + " and ( upper(p.person.name) like  :q "
                + " or upper(p.code) like :q "
                + " or upper(p.person.nic) like :q "
                + " or upper(p.person.mobile) like :q "
                + " or upper(p.person.phone) like :q "
                + " or upper(p.person.address) like :q "
                + " or upper(p.phn) like :q) ";

        if (getReportKeyWord().isAdditionalDetails()) {
            sql += " and p.code is not null ";
        }

        sql += " order by p.person.name";
        hm.put("q", "%" + query.toUpperCase() + "%");
        patientList = getFacade().findBySQL(sql, hm, 20);
        commonController.printReportDetails(null, null, startTime, "Autocomplet Patient Search");
        return patientList;
    }

    public void saveAndUpdateQueue() {
        saveSelected();
        getPracticeBookingController().listBillSessions();
    }

    public String getCountPatientCode() {

        String sql;

        sql = "select count(p) FROM Patient p where p.code is not null";

        long lng = getEjbFacade().countBySql(sql);
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
        return "/membership/add_family";
    }

    public void saveSelected(Patient p) {
        if (errorCheck(current)) {
            return;
        }
        if (p.getPerson().getMembershipScheme() != null) {
            if (checkCodeNull(p)) {
                return;
            }
        }
//        if (p == null) {
//            UtilityController.addErrorMessage("No Current. Error. NOT SAVED");
//            return;
//        }
//        if (p.getPerson() == null) {
//            UtilityController.addErrorMessage("No Person. Not Saved");
//            return;
//        }
//        if (p.getPerson().getName().trim().equals("")) {
//            UtilityController.addErrorMessage("Please enter a name");
//            return;
//        }
//        if (p.getPhn().equals("")) {
//            UtilityController.addErrorMessage("Please Enter PHN number");
//            return;
//        }

        if (p.getPerson().getId() == null) {
            p.getPerson().setCreatedAt(Calendar.getInstance().getTime());
            p.getPerson().setCreater(getSessionController().getLoggedUser());
            getPersonFacade().create(p.getPerson());
        } else {
//            p.getPerson().setEditedAt(Calendar.getInstance().getTime());
//            p.getPerson().setEditer(getSessionController().getLoggedUser());
            getPersonFacade().edit(p.getPerson());
        }
        if (p.getId() == null) {
            if (p.getPerson().getMembershipScheme() == null) {
//                p.setCode(null);
//                return;
            } else {
                if (p.getPerson().getMembershipScheme().getCode() == null || p.getPerson().getMembershipScheme().getCode().equals("")) {
//                    p.setCode(null);
                } else {
                    p.setCode(getCountPatientCode(p.getPerson().getMembershipScheme().getCode()));
                }
            }
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved as a new patient successfully.");
        } else {
            if (p.getPerson().getMembershipScheme() != null) {
                if (checkCodeNull(p)) {
                    return;
                }
            }
//            p.setEditedAt(Calendar.getInstance().getTime());
//            p.setEditer(getSessionController().getLoggedUser());
            getFacade().edit(p);
            UtilityController.addSuccessMessage("Updated the patient details successfully.");
        }

        if (password != null) {
            p.getPerson().getWebUser().setWebUserPassword(securityController.hash(password));

            password = null;
        }

        getPersonFacade().edit(p.getPerson());
        getWebUserFacade().edit(p.getPerson().getWebUser());
        getPersonFacade().flush();
        getFacade().flush();
    }

    public void saveSelectedPatient() {
        if (getCurrent().getPerson().getId() == null) {
            getCurrent().getPerson().setCreatedAt(Calendar.getInstance().getTime());
            getCurrent().getPerson().setCreater(getSessionController().getLoggedUser());
            getPersonFacade().create(getCurrent().getPerson());
        } else {
//            getCurrent().getPerson().setEditedAt(Calendar.getInstance().getTime());
//            getCurrent().getPerson().setEditer(getSessionController().getLoggedUser());
            getPersonFacade().edit(getCurrent().getPerson());
        }
        if (getCurrent().getId() == null) {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved as a new patient successfully.");
        } else {
//            getCurrent().setEditedAt(Calendar.getInstance().getTime());
//            getCurrent().setEditer(getSessionController().getLoggedUser());
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Updated the patient details successfully.");
        }
        getPersonFacade().flush();
        getFacade().flush();
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
            patientList = getFacade().findBySQL(sql, m, getReportKeyWord().getNumOfRows());
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
                    Bill b = getBillFacade().findFirstBySQL(sql, m);
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
            patientList = getFacade().findBySQL(sql, getReportKeyWord().getNumOfRows());
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

    public Patient getCurrent() {
        if (current == null) {
            Person p = new Person();
            current = new Patient();
//            current.setCode(getCountPatientCode());
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
        items = getFacade().findBySQL(sql);
    }

    public List<Patient> getItemsByDob() {
        String sql;
        Map m = new HashMap();
        m.put("dob", dob);
        sql = "select p from Patient p where p.retired = false and p.person.dob=:dob order by p.person.name";
        return getFacade().findBySQL(sql, m);
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
                + " and upper(p.code) like :q "
                + " order by p.code desc ";
        m.put("q", "%" + s.toUpperCase() + "%");

        Patient p = getEjbFacade().findFirstBySQL(sql, m);
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
            UtilityController.addErrorMessage("No Current. Error. NOT SAVED");
            return true;
        }
        if (p.getPerson() == null) {
            UtilityController.addErrorMessage("No Person. Not Saved");
            return true;
        }
        if (p.getPerson().getName().trim().equals("")) {
            UtilityController.addErrorMessage("Please Enter a Name");
            return true;
        }
        if (p.getPerson().getSex() == null) {
            UtilityController.addErrorMessage("Please Select Sex");
            return true;
        }
        if (p.getPerson().getDob() == null) {
            UtilityController.addErrorMessage("Please Pic a Birth Day");
            return true;
        }
        if (p.getPerson().getAddress() == null || p.getPerson().getAddress().equals("")) {
            UtilityController.addErrorMessage("Please Enter a Address");
            return true;
        }
        if (sessionController.getApplicationPreference().isNeedAreaForPatientRegistration()) {
            if (p.getPerson().getArea() == null) {
                UtilityController.addErrorMessage("Please Enter a Area");
                return true;
            }
        }
        if (sessionController.getApplicationPreference().isNeedPhoneNumberForPatientRegistration()) {
            if (p.getPerson().getPhone() == null || p.getPerson().getPhone().equals("")) {
                UtilityController.addErrorMessage("Please Enter a Phone Number");
                return true;
            }
        }
        if (sessionController.getApplicationPreference().isNeedNicForPatientRegistration()) {
            if (p.getPerson().getNic() == null || p.getPerson().getNic().equals("")) {
                UtilityController.addErrorMessage("Please Enter a Nic No");
                return true;
            }
        }
//        if (getCurrent().getPhn().equals("")) {
//            UtilityController.addErrorMessage("Please Enter PHN number");
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
                        + " and upper(p.code)=:q "
                        + " order by p.code desc ";
                m.put("q", pt.getCode().toUpperCase());
                m.put("p", pt);

                p = getEjbFacade().findFirstBySQL(sql, m);
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

    public Person getFamilyMember() {
        if (familyMember == null) {
            familyMember = new Person();
        }
        return familyMember;
    }

    public void setFamilyMember(Person familyMember) {
        this.familyMember = familyMember;
    }

    public List<Person> getFamilyMembers() {
        if (familyMembers == null) {
            familyMembers = new ArrayList<>();
        }
        return familyMembers;
    }

    public void setFamilyMembers(List<Person> familyMembers) {
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
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PatientController.class.getName());
            }
        }
    }

    public PracticeBookingController getPracticeBookingController() {
        return practiceBookingController;
    }

    public void setPracticeBookingController(PracticeBookingController practiceBookingController) {
        this.practiceBookingController = practiceBookingController;
    }

    public PatientEncounterController getPatientEncounterController() {
        return PatientEncounterController;
    }

    public void setPatientEncounterController(PatientEncounterController PatientEncounterController) {
        this.PatientEncounterController = PatientEncounterController;
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

    public void setCurrentRelation(Relation currentRelation) {
        this.currentRelation = currentRelation;
    }

    @FacesConverter("patientConverter")
    public static class PatientConverter implements Converter {

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
                //////System.out.println(key);
                //////System.out.println(value);
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
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PatientController.class.getName());
            }
        }
    }

}
