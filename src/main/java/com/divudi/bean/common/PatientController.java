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
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.facade.BillFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
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
import org.primefaces.context.RequestContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class PatientController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private PatientFacade ejbFacade;
    @Inject
    SessionController sessionController;
    @Inject
    PracticeBookingController practiceBookingController;

    private Patient current;
    private Person familyMember;
    private List<Person> familyMembers;
    ;
    private List<Patient> items = null;

    @EJB
    private PersonFacade personFacade;
    private Date dob;
    private String membershipTypeListner = "1";

    @Inject
    PatientEncounterController PatientEncounterController;
    @Inject
    CommonController commonController;

    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    CommonFunctions commonFunctions;

    StreamedContent barcode;
    ReportKeyWord reportKeyWord;

    @EJB
    BillFacade billFacade;

    public void patientSelected() {
        getPatientEncounterController().fillCurrentPatientLists(current);
    }

    public void createPatientBarcode() {
        File barcodeFile = new File("ptbarcode");
        if (current != null && current.getCode() != null && !current.getCode().trim().equals("")) {
            try {
                BarcodeImageHandler.saveJPEG(BarcodeFactory.createCode128(getCurrent().getCode()), barcodeFile);
                barcode = new DefaultStreamedContent(new FileInputStream(barcodeFile), "image/jpeg");

            } catch (Exception ex) {
                //   //System.out.println("ex = " + ex.getMessage());
            }
        } else {
            //   //System.out.println("else = ");
            try {
                Barcode bc = BarcodeFactory.createCode128A("0000");
                bc.setBarHeight(5);
                bc.setBarWidth(3);
                bc.setDrawingText(true);
                BarcodeImageHandler.saveJPEG(bc, barcodeFile);
                //   //System.out.println("12");
                barcode = new DefaultStreamedContent(new FileInputStream(barcodeFile), "image/jpeg");
            } catch (Exception ex) {
                //   //System.out.println("ex = " + ex.getMessage());
            }
        }
    }

    public void createFamilymembers(ActionEvent event) {

        RequestContext context = RequestContext.getCurrentInstance();
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
        System.out.println("familyMembers.size() = " + familyMembers.size());

        familyMember = null;

        context.addCallbackParam("loggedIn", loggedIn);
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
        ////System.out.println("p is " + p);
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            return new DefaultStreamedContent();
        } else if (p == null) {
            return new DefaultStreamedContent();
        } else {
            if (p.getId() != null && p.getBaImage() != null) {
                ////System.out.println("giving image");
                return new DefaultStreamedContent(new ByteArrayInputStream(p.getBaImage()), p.getFileType(), p.getFileName());
            } else {
                return new DefaultStreamedContent();
            }
        }

    }

    public StreamedContent getPhotoByByte(byte[] p) {
        ////System.out.println("p is " + p);
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            return new DefaultStreamedContent();
        } else if (p == null) {
            return new DefaultStreamedContent();
        } else {
            //   //System.out.println("giving image");
            return new DefaultStreamedContent(new ByteArrayInputStream(p), "image/png", "photo.");
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
            ////System.out.println(sql);
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
            System.err.println("*** Additonal ***");
            sql += " and p.code is not null ";
        }

        sql += " order by p.person.name";
        hm.put("q", "%" + query.toUpperCase() + "%");
        patientList = getFacade().findBySQL(sql, hm, 20);
        System.out.println("getReportKeyWord().isAdditionalDetails() = " + getReportKeyWord().isAdditionalDetails());
        System.out.println("query = " + query);
        System.out.println("sql = " + sql);
        System.err.println("patientList.size() = " + patientList.size());
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
        if (errorCheck()) {
            return;
        }
        if (getCurrent().getPerson().getMembershipScheme() != null) {
            if (checkCodeNull()) {
                System.err.println("****return");
                return;
            }
        }
//        if (getCurrent() == null) {
//            UtilityController.addErrorMessage("No Current. Error. NOT SAVED");
//            return;
//        }
//        if (getCurrent().getPerson() == null) {
//            UtilityController.addErrorMessage("No Person. Not Saved");
//            return;
//        }
//        if (getCurrent().getPerson().getName().trim().equals("")) {
//            UtilityController.addErrorMessage("Please enter a name");
//            return;
//        }
//        if (getCurrent().getPhn().equals("")) {
//            UtilityController.addErrorMessage("Please Enter PHN number");
//            return;
//        }
        if (getCurrent().getPerson().getId() == null) {
            getCurrent().getPerson().setCreatedAt(Calendar.getInstance().getTime());
            getCurrent().getPerson().setCreater(getSessionController().getLoggedUser());
            getPersonFacade().create(getCurrent().getPerson());
            System.out.println("1.getCurrent().getPerson().getTitle() = " + getCurrent().getPerson().getTitle());
            System.out.println("getCurrent().getPerson().getNameWithTitle() = " + getCurrent().getPerson().getNameWithTitle());
        } else {
            getCurrent().getPerson().setEditedAt(Calendar.getInstance().getTime());
            getCurrent().getPerson().setEditer(getSessionController().getLoggedUser());
            getPersonFacade().edit(getCurrent().getPerson());
            System.out.println("2.getCurrent().getPerson().getTitle() = " + getCurrent().getPerson().getTitle());
            System.out.println("getCurrent().getPerson().getNameWithTitle() = " + getCurrent().getPerson().getNameWithTitle());
        }
        if (getCurrent().getId() == null) {
            System.out.println("********getCurrent().getCode() = " + getCurrent().getCode());
            if (getCurrent().getPerson().getMembershipScheme() == null) {
//                getCurrent().setCode(null);
//                return;
            } else {
                if (getCurrent().getPerson().getMembershipScheme().getCode() == null || getCurrent().getPerson().getMembershipScheme().getCode().equals("")) {
//                    getCurrent().setCode(null);
                } else {
                    getCurrent().setCode(getCountPatientCode(getCurrent().getPerson().getMembershipScheme().getCode()));
                }
            }
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            System.out.println("3.getCurrent().getPerson().getTitle() = " + getCurrent().getPerson().getTitle());
            System.out.println("getCurrent().getPerson().getNameWithTitle() = " + getCurrent().getPerson().getNameWithTitle());
            UtilityController.addSuccessMessage("Saved as a new patient successfully.");
        } else {
            if (getCurrent().getPerson().getMembershipScheme() != null) {
                if (checkCodeNull()) {
                    return;
                }
            }
            getCurrent().setEditedAt(Calendar.getInstance().getTime());
            getCurrent().setEditer(getSessionController().getLoggedUser());
            getFacade().edit(getCurrent());
            System.out.println("4.getCurrent().getPerson().getTitle() = " + getCurrent().getPerson().getTitle());
            System.out.println("getCurrent().getPerson().getNameWithTitle() = " + getCurrent().getPerson().getNameWithTitle());
            UtilityController.addSuccessMessage("Updated the patient details successfully.");
        }
        getPersonFacade().flush();
        getFacade().flush();
        System.out.println("5.getCurrent().getPerson().getTitle() = " + getCurrent().getPerson().getTitle());
        System.out.println("getCurrent().getPerson().getNameWithTitle() = " + getCurrent().getPerson().getNameWithTitle());
    }
    
    public void saveSelectedPatient() {
        if (getCurrent().getPerson().getId() == null) {
            getCurrent().getPerson().setCreatedAt(Calendar.getInstance().getTime());
            getCurrent().getPerson().setCreater(getSessionController().getLoggedUser());
            getPersonFacade().create(getCurrent().getPerson());
        } else {
            getCurrent().getPerson().setEditedAt(Calendar.getInstance().getTime());
            getCurrent().getPerson().setEditer(getSessionController().getLoggedUser());
            getPersonFacade().edit(getCurrent().getPerson());
        }
        if (getCurrent().getId() == null) {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved as a new patient successfully.");
        } else {
            getCurrent().setEditedAt(Calendar.getInstance().getTime());
            getCurrent().setEditer(getSessionController().getLoggedUser());
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
            if (getReportKeyWord().getMembershipScheme()!=null) {
                sql+=" and p.person.membershipScheme=:mem ";
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
                    System.out.println("p.getCreatedAt() = " + p.getCreatedAt());
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
                    System.out.println("m = " + m);
                    Bill b = getBillFacade().findFirstBySQL(sql, m);
                    System.out.println("b = " + b);
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

        System.err.println("patientList.size() = " + patientList.size());
    }

    public void activePatient(Patient p) {
        p.setEditedAt(new Date());
        p.setEditer(getSessionController().getLoggedUser());
        p.setRetired(false);
        p.setRetireComments("Re-Activated");
        getFacade().edit(p);

        p.getPerson().setEditedAt(new Date());
        p.getPerson().setEditer(getSessionController().getLoggedUser());
        p.getPerson().setRetired(false);
        p.getPerson().setRetireComments("Re-Activated");
        getPersonFacade().edit(p.getPerson());
        createPatientList();
        JsfUtil.addSuccessMessage("Re-Activated");
    }

    public void deActivePatient(Patient p) {
        p.setEditedAt(new Date());
        p.setEditer(getSessionController().getLoggedUser());
        p.setRetired(true);
        p.setRetireComments("De-Activated");
        getFacade().edit(p);

        p.getPerson().setEditedAt(new Date());
        p.getPerson().setEditer(getSessionController().getLoggedUser());
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
        if (items == null || items.isEmpty()) {
            fillAllPatients();
        }
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
        System.out.println("getReportKeyWord().getMembershipScheme().getName() = " + getCurrent().getPerson().getMembershipScheme().getName());
        System.out.println("getReportKeyWord().getMembershipScheme().getCode() = " + getCurrent().getPerson().getMembershipScheme().getCode());
        if (getCurrent().getPerson().getMembershipScheme().getCode() == null
                || getCurrent().getPerson().getMembershipScheme().getCode().equals("")) {
            getCurrent().setCode(null);
            JsfUtil.addErrorMessage("Please Select Membership Scheme Code Correctly");
            return;
        }
        if (getCurrent().getId() == null) {
            getCurrent().setCode(getCountPatientCode(getCurrent().getPerson().getMembershipScheme().getCode()));
            System.out.println("********getCurrent().getCode() = " + getCurrent().getCode());
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
            System.out.println("p.getCode() = " + p.getCode());
            String str = p.getCode();
//        System.out.println("str.substring(0,1) = " + str.substring(0, 1));
//        System.out.println("str.substring(0,2) = " + str.substring(0, 2));
//        System.out.println("str.substring(2) = " + str.substring(2));
//        System.out.println("str.substring(3) = " + str.substring(3));
//        System.out.println("str.substring(3,7) = " + str.substring(3, 7));
            long l = Long.parseLong(str.substring(2));
            System.out.println("l = " + l);
            l++;
            System.out.println("l = " + l);
            st += s;
            st += df.format(l);
            System.out.println("s = " + st);
            return st;
        } else {
            System.err.println("P Null");
            st += s;
            st += df.format(1l);
            System.out.println("s = " + st);
            return st;
        }

    }

    private boolean errorCheck() {
        if (getCurrent() == null) {
            UtilityController.addErrorMessage("No Current. Error. NOT SAVED");
            return true;
        }
        if (getCurrent().getPerson() == null) {
            UtilityController.addErrorMessage("No Person. Not Saved");
            return true;
        }
        if (getCurrent().getPerson().getName().trim().equals("")) {
            UtilityController.addErrorMessage("Please Enter a Name");
            return true;
        }
        if (getCurrent().getPerson().getSex() == null) {
            UtilityController.addErrorMessage("Please Select Sex");
            return true;
        }
        if (getCurrent().getPerson().getDob() == null) {
            UtilityController.addErrorMessage("Please Pic a Birth Day");
            return true;
        }
        if (getCurrent().getPerson().getAddress() == null || getCurrent().getPerson().getAddress().equals("")) {
            UtilityController.addErrorMessage("Please Enter a Address");
            return true;
        }
        if (getCurrent().getPerson().getArea() == null) {
            UtilityController.addErrorMessage("Please Enter a Area");
            return true;
        }
        if (getCurrent().getPerson().getPhone() == null || getCurrent().getPerson().getPhone().equals("")) {
            UtilityController.addErrorMessage("Please Enter a Phone Number");
            return true;
        }
        if (getCurrent().getPerson().getNic() == null || getCurrent().getPerson().getNic().equals("")) {
            UtilityController.addErrorMessage("Please Enter a Nic No");
            return true;
        }
//        if (getCurrent().getPhn().equals("")) {
//            UtilityController.addErrorMessage("Please Enter PHN number");
//            return;
//        }
        return false;
    }

    private boolean checkCodeNull() {
        Patient p = null;
        if (getCurrent().getId() != null) {
            System.out.println("getCurrent().getId() = " + getCurrent().getId());
            p = getEjbFacade().find(getCurrent().getId());
        }
        if (p != null) {
            System.out.println("******p.getCode() = " + p.getCode());
            if (getCurrent().getCode() == null || getCurrent().getCode().equals("")) {
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
                m.put("q", getCurrent().getCode().toUpperCase());
                m.put("p", getCurrent());

                p = getEjbFacade().findFirstBySQL(sql, m);
                if (p != null) {
                    JsfUtil.addErrorMessage("Code Already Exsist.Please Try - " + getCountPatientCode(getCurrent().getPerson().getMembershipScheme().getCode()));
                    System.out.println("p.getCode() = " + p.getCode());
                    System.out.println("p.getPerson().getName() = " + p.getPerson().getName());
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
            ////System.out.println("value at converter getAsObject is " + value);
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            ////System.out.println(value);
            if (value == null || value.equals("null") || value.trim().equals("")) {
                key = 0l;
            } else {
                key = Long.valueOf(value);
                System.out.println(key);
                System.out.println(value);
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
            ////System.out.println("value at converter getAsObject is " + value);
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            ////System.out.println(value);
            if (value == null || value.equals("null") || value.trim().equals("")) {
                key = 0l;
            } else {
                key = Long.valueOf(value);
                ////System.out.println(key);
                ////System.out.println(value);
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
