/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.CashBookEntryController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.AppointmentStatus;
import com.divudi.core.data.AppointmentType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.core.data.dto.ReservationDTO;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.core.entity.Appointment;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Person;
import com.divudi.core.facade.AppointmentFacade;
import com.divudi.core.facade.BillComponentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.inward.Reservation;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.ReservationFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.ejb.NumberGenerator;
import com.divudi.service.StaffService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Informatics)
 */
@Named
@SessionScoped
public class AppointmentController implements Serializable, ControllerWithPatient {

    private static final long serialVersionUID = 1L;

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private ReservationFacade reservationFacade;
    
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
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    NumberGenerator numberGenerator;
    @EJB
    StaffService staffBean;
    @EJB
    PaymentFacade paymentFacade;
    
    @Inject
    CashBookEntryController cashBookEntryController;
    @Inject
    private BillBeanController billBean;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    SessionController sessionController;
    @Inject
    private PaymentSchemeController paymentSchemeController;
    
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
    private Reservation reservation;
    private PaymentMethod paymentMethod;
    private boolean patientDetailsEditable;
    private Patient patient;
    
    private Date fromDate;
    private Date toDate;
    private String appointmentNo;
    private String patientName;
    private AppointmentStatus appointmentstatus;
    private List<ReservationDTO> reservationDTOs;

    public void makeNull(){
        fromDate = null;
        toDate = null;
        appointmentNo = null;
        patientName = null;
        appointmentstatus = null;
        reservationDTOs = new ArrayList<>();
    }

    public String navigateToSearchAppointmentsListFromMenu(){
        makeNull();
        appointmentstatus = AppointmentStatus.PENDING;
        return "/inward/view_appointment?faces-redirect=true";
    }
    
    public void searchAppointments(){
        reservationDTOs = new ArrayList<>();
        
        String jpql = "SELECT new com.divudi.core.data.dto.ReservationDTO("
                + "  "
                + " ) "
                + " FROM Reservation res "
                + " WHERE res.Room = :room "
                + " AND res.reservedFrom BETWEEN :today AND :endDate "
                + " ORDER BY res.reservedFrom";

        Double lookupDays = configOptionApplicationController.getDoubleValueByKey("Inward - Appoiment Lookup Duration (Days)", 30.0);

        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.add(Calendar.DAY_OF_YEAR, lookupDays.intValue());
        Date endDate = cal.getTime();
        
        HashMap hm = new HashMap();
        hm.put("room", r);
        hm.put("today", today);
        hm.put("endDate", endDate);
        
        List<Reservation> reservations = reservationFacade.findByJpql(jpql,hm);
        
        return reservations;
    }
    
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
        getCurrentAppointment().setAppointmentType(AppointmentType.IP_APPOINTMENT);
        getCurrentAppointment().setStatus(AppointmentStatus.PENDING);
        String appointmentNo = numberGenerator.inwardAppointmentNumberGeneratorYearly( sessionController.getInstitution(), AppointmentType.IP_APPOINTMENT);
        getCurrentAppointment().setAppointmentNumber(appointmentNo);
        
        getAppointmentFacade().create(getCurrentAppointment());
    }

    private void saveReservation(Patient p, Appointment a) {
//        if (p == null) {
//            JsfUtil.addErrorMessage("No patient Selected");
//            return;
//        }
//        if (a == null) {
//            JsfUtil.addErrorMessage("No Appointment Selected");
//            return;
//        }
//        if (reservation.getRoom() == null) {
//            JsfUtil.addErrorMessage("No Room Selected");
//            return;
//        }
//        if (reservation.getReservedFrom() == null) {
//            JsfUtil.addErrorMessage("No Reserved From Date Selected");
//            return;
//        }
//
//        if (reservation.getReservedTo() == null) {
//            JsfUtil.addErrorMessage("No Reserved To Date Selected");
//            return;
//        }

        reservation.setAppointment(a);
        reservation.setPatient(p);
        reservation.setCreatedAt(new Date());
        reservation.setCreater(sessionController.getLoggedUser());

        if (reservation.getId() == null) {
            getReservationFacade().create(reservation);
        } else {
            getReservationFacade().edit(reservation);
        }

    }

    public void settleBill() {
        
        Date startTime = new Date();
        Date fromDate = new Date();
        Date toDate = new Date();
        
        if (errorCheck()) {
            return;
        }
        
        if (getPatient() != null && getPatient().getId() != null && getPatient().isBlacklisted() && configOptionApplicationController.getBooleanValueByKey("Enable blacklist patient management for inward from the system", false)) {
            JsfUtil.addErrorMessage("This patient is blacklisted from the system.");
            return;
        }
        
        if(reservation == null || reservation.getRoom() == null){
            JsfUtil.addErrorMessage("Please select a patient room for the appoiment.");
            return;
        }
        
        if(reservation.getReservedFrom() == null){
            JsfUtil.addErrorMessage("Please select a Reservation date for the appoiment.");
            return;
        }
        
        if(!reservation.getReservedFrom().after(new Date())){
            JsfUtil.addErrorMessage("Please select a valid Reservation from date and time without now.");
            return;
        }
        
        if(reservation.getReservedTo() != null && (!reservation.getReservedTo().after(new Date()) || !reservation.getReservedTo().after(reservation.getReservedFrom()))){
            JsfUtil.addErrorMessage("Please select a valid Reservation todate.");
            return;
        }

        Patient p = savePatient(getPatient());

        saveBill(p);
        saveAppointment(p);
        saveReservation(p, currentAppointment);
        createPayment(getCurrentBill(), getCurrentBill().getPaymentMethod());

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }
    
    public List<Payment> createPayment(Bill bill, PaymentMethod pm) {
        List<Payment> ps = new ArrayList<>();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment p = new Payment();
                p.setBill(bill);
                p.setInstitution(getSessionController().getInstitution());
                p.setDepartment(getSessionController().getDepartment());
                p.setCreatedAt(new Date());
                p.setCreater(getSessionController().getLoggedUser());
                p.setPaymentMethod(cd.getPaymentMethod());

                switch (cd.getPaymentMethod()) {
                    case Card:
                        p.setBank(cd.getPaymentMethodData().getCreditCard().getInstitution());
                        p.setCreditCardRefNo(cd.getPaymentMethodData().getCreditCard().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCreditCard().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCreditCard().getComment());
                        break;
                    case Cheque:
                        p.setBank(cd.getPaymentMethodData().getCheque().getInstitution());
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCheque().getComment());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCash().getComment());
                        break;
                    case ewallet:
                        p.setPolicyNo(cd.getPaymentMethodData().getEwallet().getReferralNo());
                        p.setReferenceNo(cd.getPaymentMethodData().getEwallet().getReferenceNo());
                        p.setCreditCompany(cd.getPaymentMethodData().getEwallet().getInstitution());
                        p.setPaidValue(cd.getPaymentMethodData().getEwallet().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getEwallet().getComment());
                        break;
                    case Agent:
//                        TODO:Add Details
                        break;
                    case Credit:
                        p.setPolicyNo(cd.getPaymentMethodData().getCredit().getReferralNo());
                        p.setReferenceNo(cd.getPaymentMethodData().getCredit().getReferenceNo());
                        p.setCreditCompany(cd.getPaymentMethodData().getCredit().getInstitution());
                        p.setPaidValue(cd.getPaymentMethodData().getCredit().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCredit().getComment());
                        break;
                    case PatientDeposit:
                        if (getPatient().getRunningBalance() != null) {
                            getPatient().setRunningBalance(getPatient().getRunningBalance() - cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                        } else {
                            getPatient().setRunningBalance(0.0 - cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                        }
                        getPatientFacade().edit(getPatient());
                        p.setPaidValue(cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                        break;
                    case Slip:
                        p.setPaidValue(cd.getPaymentMethodData().getSlip().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
                        p.setComments(cd.getPaymentMethodData().getSlip().getComment());
                        p.setReferenceNo(cd.getPaymentMethodData().getSlip().getReferenceNo());
                        p.setPaymentDate(cd.getPaymentMethodData().getSlip().getDate());
                        p.setChequeDate(cd.getPaymentMethodData().getSlip().getDate());

                        break;
                    case OnCall:
                    case OnlineSettlement:
                    case Staff:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffCredit().getToStaff() != null) {
                            staffBean.updateStaffCredit(cd.getPaymentMethodData().getStaffCredit().getToStaff(), cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                            JsfUtil.addSuccessMessage("Staff Credit Updated");
                        }
                        break;
                    case Staff_Welfare:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffWelfare().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffWelfare().getToStaff() != null) {
                            staffBean.updateStaffWelfare(cd.getPaymentMethodData().getStaffWelfare().getToStaff(), cd.getPaymentMethodData().getStaffWelfare().getTotalValue());
                            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
                        }
                        break;
                    case MultiplePaymentMethods:
                }

                paymentFacade.create(p);
                cashBookEntryController.writeCashBookEntryAtPaymentCreation(p);
                ps.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(bill);
            p.setInstitution(getSessionController().getInstitution());
            p.setDepartment(getSessionController().getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            p.setPaymentMethod(pm);

            switch (pm) {
                case Card:
                    p.setBank(paymentMethodData.getCreditCard().getInstitution());
                    p.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
                    p.setPaidValue(paymentMethodData.getCreditCard().getTotalValue());
                    p.setComments(paymentMethodData.getCreditCard().getComment());
                    break;
                case Cheque:
                    p.setBank(paymentMethodData.getCheque().getInstitution());
                    p.setChequeDate(paymentMethodData.getCheque().getDate());
                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
                    p.setPaidValue(paymentMethodData.getCheque().getTotalValue());
                    p.setComments(paymentMethodData.getCheque().getComment());
                    break;
                case Cash:
                    p.setPaidValue(bill.getTotal());
                    p.setComments("");
                    break;
                case ewallet:
                    p.setBank(paymentMethodData.getEwallet().getInstitution());
                    p.setPolicyNo(paymentMethodData.getEwallet().getReferralNo());
                    p.setReferenceNo(paymentMethodData.getEwallet().getReferenceNo());
                    p.setCreditCompany(paymentMethodData.getEwallet().getInstitution());
                    p.setPaidValue(paymentMethodData.getEwallet().getTotalValue());
                    p.setComments(paymentMethodData.getEwallet().getComment());
                    break;

                case Agent:
                    break;
                case Credit:
                    p.setPolicyNo(paymentMethodData.getCredit().getReferralNo());
                    p.setComments(paymentMethodData.getCredit().getComment());
                    p.setReferenceNo(paymentMethodData.getCredit().getReferenceNo());
                    p.setCreditCompany(paymentMethodData.getCredit().getInstitution());
                    break;
                case PatientDeposit:
                    break;
                case Slip:
                    p.setBank(paymentMethodData.getSlip().getInstitution());
                    p.setPaidValue(paymentMethodData.getSlip().getTotalValue());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
                    p.setPaymentDate(paymentMethodData.getSlip().getDate());
                    p.setChequeDate(paymentMethodData.getSlip().getDate());
                    p.setComments(paymentMethodData.getSlip().getComment());
                    p.setReferenceNo(paymentMethodData.getSlip().getReferenceNo());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
                    break;
                case OnCall:
                case OnlineSettlement:
                case Staff:
                case YouOweMe:
                case MultiplePaymentMethods:
            }

            p.setPaidValue(p.getBill().getNetTotal());
            paymentFacade.create(p);
            cashBookEntryController.writeCashBookEntryAtPaymentCreation(p);
            ps.add(p);
        }
        return ps;
    }

    public void dateChangeListen() {
        getNewPatient().getPerson().setDob(CommonFunctions.guessDob(yearMonthDay));

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

        //getBillBean().setPaymentMethodData(getCurrentBill(), getCurrentBill().getPaymentMethod(), getPaymentMethodData());

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
        if (getNewPatient().getPerson().getName() == null || getNewPatient().getPerson().getName().trim().isEmpty() || getNewPatient().getPerson().getSex() == null || getAgeText() == null) {
            JsfUtil.addErrorMessage("Can not bill without Patient Name, Age or Sex.");
            return true;
        }

        if (!Person.checkAgeSex(getNewPatient().getPerson().getDob(), getNewPatient().getPerson().getSex(), getNewPatient().getPerson().getTitle())) {
            JsfUtil.addErrorMessage("Check Title,Age,Sex");
            return true;
        }

        if (getNewPatient().getPerson().getPhone().isEmpty()) {
            JsfUtil.addErrorMessage("Phone Number is Required it should be fill");
            return true;
        }

        //}
        return false;

    }

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
        if (getPatient() == null) {
            JsfUtil.addErrorMessage("No patient Selected");
            return true;
        }

        if(getPatient().getPerson().getMobile() == null && getPatient().getPerson().getPhone() == null){
            JsfUtil.addErrorMessage("Please provide a patient phone number.");
            return true;
        }

        if (getPatient().getPerson() == null) {
            JsfUtil.addErrorMessage("No patient Selected");
            return false;
        }

        if (getPatient().getPerson().getName() == null || getPatient().getPerson().getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Can not bill without Patient Name");
            return true;
        }

        //}
        if (getCurrentBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please Select a Payment Method");
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrentBill().getPaymentMethod(), paymentMethodData)) {
            return true;
        }
//
        return false;
    }

    public List<Reservation> checkAppointmentsForRoom(RoomFacilityCharge r) {
        String jpql = "SELECT res FROM Reservation res "
                + "WHERE res.Room = :room "
                + "AND res.reservedFrom BETWEEN :today AND :endDate "
                + "ORDER BY res.reservedFrom";

        Double lookupDays = configOptionApplicationController.getDoubleValueByKey("Inward - Appoiment Lookup Duration (Days)", 30.0);

        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.add(Calendar.DAY_OF_YEAR, lookupDays.intValue());
        Date endDate = cal.getTime();
        
        HashMap hm = new HashMap();
        hm.put("room", r);
        hm.put("today", today);
        hm.put("endDate", endDate);
        
        List<Reservation> reservations = reservationFacade.findByJpql(jpql,hm);
        
        return reservations;
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
        getNewPatient().getPerson().setDob(CommonFunctions.guessDob(ageText));
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

    public Reservation getReservation() {
        if (reservation == null) {
            reservation = new Reservation();
        }
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public void prepereForInwardAppointPatient() {
        printPreview = false;
        searchedPatient = null;
        patient = null;
        currentBill = null;
        currentAppointment = null;
        reservation = null;
        getCurrentBill();
        getCurrentAppointment();
        getReservation();
    }

    public String navigateToInwardAppointmentFromMenu() {
        prepereForInwardAppointPatient();
        setSearchedPatient(getPatient());
        setPatient(getNewPatient());
        getCurrentBill().setPatient(getPatient());
        getCurrentAppointment().setPatient(getPatient());
        return "/inward/inward_appointment?faces-redirect=true;";
    }

    public ReservationFacade getReservationFacade() {
        return reservationFacade;
    }

    public void setReservationFacade(ReservationFacade reservationFacade) {
        this.reservationFacade = reservationFacade;
    }
    
    @Override
    public Patient getPatient() {
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
    }
    
    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
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

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getAppointmentNo() {
        return appointmentNo;
    }

    public void setAppointmentNo(String appointmentNo) {
        this.appointmentNo = appointmentNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public AppointmentStatus getAppointmentstatus() {
        return appointmentstatus;
    }

    public void setAppointmentstatus(AppointmentStatus appointmentstatus) {
        this.appointmentstatus = appointmentstatus;
    }

    public List<ReservationDTO> getReservationDTOs() {
        if(reservationDTOs == null){
            reservationDTOs = new ArrayList<>();
        }
        return reservationDTOs;
    }

    public void setReservationDTOs(List<ReservationDTO> reservationDTOs) {
        this.reservationDTOs = reservationDTOs;
    }

    /**
     *
     */
    @FacesConverter(forClass = Appointment.class)
    public static class AppointmentControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
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

}
