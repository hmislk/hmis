/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.inward.BhtEditController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.ejb.BillNumberGenerator;

import com.divudi.entity.Appointment;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.facade.AppointmentFacade;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Informatics)
 */
@Named
@SessionScoped
public class AppointmentController implements Serializable {
    
    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    BhtEditController bhtEditController;
    
    @Inject
    CommonController commonController;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @Inject
    private BillBeanController billBean;
    CommonFunctions commonFunctions;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private AppointmentFacade appointmentFacade;
    
    private boolean printPreview;
    
    private Patient newPatient;
    private Patient searchedPatient;
    //private String creditCardRefNo;
    //  private String chequeRefNo;
    private String patientTabId = "tabNewPt";
    private String ageText = "";
    private Bill currentBill;
    private Appointment currentAppointment;
    private YearMonthDay yearMonthDay;
    private PaymentMethodData paymentMethodData;
    
    public Title[] getTitle() {
        return Title.values();
    }
    
    public Sex[] getSex() {
        return Sex.values();
    }

//    public List<Bill> completeOpdCreditBill(String qry) {
//        List<Bill> a = null;
//        String sql;
//        HashMap hash = new HashMap();
//        if (qry != null) {
//            sql = "select c from BilledBill c where c.paidAmount is null and c.billType= :btp and c.paymentMethod= :pm and c.cancelledBill is null and c.refundedBill is null and c.retired=false and (c.insId) like '%" + qry.toUpperCase() + "%' order by c.creditCompany.name";
//            hash.put("btp", BillType.OpdBill);
//            hash.put("pm", PaymentMethod.Credit);
//            a = getFacade().findByJpql(sql, hash, TemporalType.TIME);
//        }
//        if (a == null) {
//            a = new ArrayList<Bill>();
//        }
//        return a;
//    }
    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }
    
    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }
    
    private Patient savePatient(Patient p) {

        if (p == null) {
            return null;
        }
        if (p.getPerson() == null) {
            return null;
        }
        
        if (p.getPerson().getId() == null) {
            p.getPerson().setCreater(sessionController.getLoggedUser());
            p.getPerson().setCreatedAt(new Date());
            personFacade.create(p.getPerson());
        } else {
            personFacade.edit(p.getPerson());
        }
        
        if (p.getId() == null) {
            p.setCreater(sessionController.getLoggedUser());
            p.setCreatedAt(new Date());
            patientFacade.create(p);
        } else {
            patientFacade.edit(p);
        }
        
        return p;
    }
    
    private void saveAppointment(Patient p) {
        getCurrentAppointment().setCreatedAt(new Date());
        getCurrentAppointment().setCreater(getSessionController().getLoggedUser());
        getCurrentAppointment().setPatient(p);
        getCurrentAppointment().setBill(getCurrentBill());
        getAppointmentFacade().create(getCurrentAppointment());
        //      currentAppointment=null;
    }
    
    public void settleBill() {
        Date startTime = new Date();
        Date fromDate = new Date();
        Date toDate = new Date();
        if (errorCheck()) {
            return;
        }
        
        Patient p = savePatient(getSearchedPatient());
        
        saveBill(p);
        saveAppointment(p);
        //  getBillBean().saveBillItems(b, getLstBillEntries(), getSessionController().getLoggedUser());
        // getBillBean().calculateBillItems(b, getLstBillEntries());
        //     getBills().add(b);

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;
        
        
    }
    
    public void dateChangeListen() {
        getNewPatient().getPerson().setDob(getCommonFunctions().guessDob(yearMonthDay));
        
    }
    
    private void saveBill(Patient p) {

        //getCurrentBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(),getCurrentAppointment(), BillNumberSuffix.INWSERBillNumberSuffix.INWSER);
//        getCurrentBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.Appointment));
        //  getCurrentBill().setBillType(BillType.OpdBill);
        getCurrentBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrentBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        
        getCurrentBill().setPatient(p);
        // getCurrentBill().setAppointment(getCurrentAppointment());
        //     getCurrentBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        //    getCurrentBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getBillBean().setPaymentMethodData(getCurrentBill(), getCurrentBill().getPaymentMethod(), getPaymentMethodData());
        
        getCurrentBill().setBillDate(new Date());
        getCurrentBill().setBillTime(new Date());
        //   getCurrentBill().setPatient(tmpPatient);
//        temp.setPatientEncounter(patientEncounter);
        //   temp.setPaymentScheme(getPaymentScheme());

        getCurrentBill().setCreatedAt(new Date());
        getCurrentBill().setCreater(sessionController.getLoggedUser());
        getFacade().create(getCurrentBill());
        //return getCurrentBill();

    }
    
    private boolean checkPatientAgeSex() {
        
//        if (getPatientTabId().toString().equals("tabNewPt")) {
            
            if (getNewPatient().getPerson().getName() == null || getNewPatient().getPerson().getName().trim().equals("") || getNewPatient().getPerson().getSex() == null || getAgeText() == null) {
                JsfUtil.addErrorMessage("Can not bill without Patient Name, Age or Sex.");
                return true;
            }
            
            if (!com.divudi.java.CommonFunctions.checkAgeSex(getNewPatient().getPerson().getDob(), getNewPatient().getPerson().getSex(), getNewPatient().getPerson().getTitle())) {
                JsfUtil.addErrorMessage("Check Title,Age,Sex");
                return true;
            }
            
            if (getNewPatient().getPerson().getPhone().length() < 1) {
                JsfUtil.addErrorMessage("Phone Number is Required it should be fill");
                return true;
            }
            
        //}
        
        return false;
        
    }
    
    @Inject
    private PaymentSchemeController paymentSchemeController;
    
    private boolean errorCheck() {

//        if (checkPatientAgeSex()) {
//            return true;
//        }
//
//        if (getPatientTabId().toString().equals("tabSearchPt")) {
//            if (getSearchedPatient() == null) {
//                JsfUtil.addErrorMessage("Plese Select Patient");
//            }
//        }
        //if (getPatientTabId().toString().equals("tabNewPt")) {
        if (getSearchedPatient() == null) {
            JsfUtil.addErrorMessage("No patient Selected");
            return false;
        }
        
        if (getSearchedPatient().getPerson() == null) {
            JsfUtil.addErrorMessage("No patient Selected");
            return false;
        }
        
        if (getSearchedPatient().getPerson().getName() == null || getSearchedPatient().getPerson().getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Can not bill without Patient Name");
            return true;
        }

        //}
        if (getCurrentBill().getPaymentMethod() == null) {
            return true;
        }
        
        if (getPaymentSchemeController().checkPaymentMethodError(getCurrentBill().getPaymentMethod(), paymentMethodData)) {
            return true;
        }
//       
        return false;
    }
    
    public String prepareNewBill() {
      
        currentBill = null;
        
        setPrintPreview(true);
        printPreview = false;
        return "";
    }
    
    public void onTabChange(TabChangeEvent event) {
        setPatientTabId(event.getTab().getId());
        
    }
    
    public BillFacade getEjbFacade() {
        return billFacade;
    }
    
    public void setEjbFacade(BillFacade ejbFacade) {
        this.billFacade = ejbFacade;
    }
    
    public SessionController getSessionController() {
        return sessionController;
    }
    
    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }
    
    public AppointmentController() {
    }
    
    private BillFacade getFacade() {
        return billFacade;
    }
    
    public boolean isPrintPreview() {
        return printPreview;
    }
    
    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }
    
    public BillFacade getBillFacade() {
        return billFacade;
    }
    
    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }
    
    public String getPatientTabId() {
        return patientTabId;
    }
    
    public void setPatientTabId(String patientTabId) {
        this.patientTabId = patientTabId;
    }
    
    public Patient getNewPatient() {
        if (newPatient == null) {
            newPatient = new Patient();
            Person p = new Person();
            
            newPatient.setPerson(p);
        }
        return newPatient;
    }
    
    public void setNewPatient(Patient newPatient) {
        this.newPatient = newPatient;
    }
    
    public Patient getSearchedPatient() {
        return searchedPatient;
    }
    
    public void setSearchedPatient(Patient searchedPatient) {
        this.searchedPatient = searchedPatient;
    }
    
    public String getAgeText() {
        ageText = getNewPatient().getAge();
        if (ageText.startsWith("0 days")) {
            return "";
        }
        return ageText;
    }
    
    public void setAgeText(String ageText) {
        this.ageText = ageText;
        getNewPatient().getPerson().setDob(getCommonFunctions().guessDob(ageText));
    }
    
    public BillBeanController getBillBean() {
        return billBean;
    }
    
    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
        
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
    
    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }
    
    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
        
    }
    
    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }
    
    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
    }
    
    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }
    
    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }
    
    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }
    
    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }
    
    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }
    
    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
        
    }
    
    public Bill getCurrentBill() {
        if (currentBill == null) {
            currentBill = new BilledBill();
            currentBill.setBillType(BillType.InwardAppointmentBill);
        }
        return currentBill;
    }
    
    public void setCurrentBill(Bill currentBill) {
        this.currentBill = currentBill;
    }
    
    public Appointment getCurrentAppointment() {
        if (currentAppointment == null) {
            currentAppointment = new Appointment();
        }
        return currentAppointment;
    }
    
    public void setCurrentAppointment(Appointment currentAppointment) {
        this.currentAppointment = currentAppointment;
    }
    
    public AppointmentFacade getAppointmentFacade() {
        return appointmentFacade;
    }
    
    public void setAppointmentFacade(AppointmentFacade appointmentFacade) {
        this.appointmentFacade = appointmentFacade;
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
    
    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }
    
    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }
    
    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }
    
    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }
    
    public void prepereForInwardAppointPatient() {
        printPreview = false;
        searchedPatient = null;
        currentBill = null;
        currentAppointment = null;
        getCurrentBill();
        getCurrentAppointment();
    }
    
    public String navigateToInwardAppointmentFromMenu() {
        prepereForInwardAppointPatient();
        setSearchedPatient(getNewPatient());
        getCurrentBill().setPatient(getSearchedPatient());
        getCurrentAppointment().setPatient(getSearchedPatient());
        return "/inward/inward_appointment";
    }

    /**
     *
     */
    @FacesConverter(forClass = Appointment.class)
    public static class AppointmentControllerConverter implements Converter {
        
        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AppointmentController controller = (AppointmentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "appointmentController");
            return controller.getFacade().find(getKey(value));
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
            if (object instanceof Appointment) {
                Appointment o = (Appointment) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Appointment.class.getName());
            }
        }
    }
    
    public CommonController getCommonController() {
        return commonController;
    }
    
}
