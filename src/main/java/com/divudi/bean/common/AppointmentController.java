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
import com.divudi.bean.inward.AdmissionController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.AppointmentStatus;
import com.divudi.core.data.AppointmentType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
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
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.PatientRoom;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class AppointmentController implements Serializable, ControllerWithPatient {

    private static final long serialVersionUID = 1L;

    // <editor-fold defaultstate="collapsed" desc="EJBs">
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
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
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
    @Inject
    AdmissionController admissionController;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Vaiables">
    private boolean printPreview;

    private Patient newPatient;
    private Patient searchedPatient;
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

    private Date reservedFromDate;
    private Date reservedToDate;
    private RoomFacilityCharge reservedRoom;
    private String comment;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Navigations">
    public String navigateToSearchAppointmentsListFromMenu() {
        makeNull();
        appointmentstatus = AppointmentStatus.PENDING;
        return "/inward/view_appointment?faces-redirect=true";
    }

    public String navigateToAppointmentAdminFromMenu() {
        makeNull();
        return "/inward/appointment_admit?faces-redirect=true";
    }

    public String navigateToSearchAppointmentsListFromViewAppointments() {
        searchAppointments();
        return "/inward/view_appointment?faces-redirect=true";
    }

    public String navigatePatientAdmit() {
        if (reservation == null) {
            JsfUtil.addErrorMessage("No Reservation Found");
            return "";
        }

        if (reservation == null || reservation.getAppointment() == null || reservation.getAppointment().getBill() == null) {
            JsfUtil.addErrorMessage("No Reservation Found");
            return "";
        }

        Admission ad = new Admission();
        if (ad.getDateOfAdmission() == null) {
            ad.setDateOfAdmission(CommonFunctions.getCurrentDateTime());
        }

        admissionController.setCurrent(ad);
        admissionController.setPrintPreview(false);
        admissionController.setAdmittingProcessStarted(false);
        admissionController.setPatientRoom(new PatientRoom());
        admissionController.setAppointmentBill(reservation.getAppointment().getBill());
        admissionController.setPatientAllergies(null);
        admissionController.setCurrentReservation(reservation);
        admissionController.setBhtText("");
        admissionController.listnerForAppoimentSelect(reservation.getAppointment().getBill());

        return "/inward/inward_admission?faces-redirect=true";
    }

    public String navigatePatientAdmit(Long reservationId) {

        if (reservationId == null) {
            JsfUtil.addErrorMessage("Error in Reservation");
            return "";
        }

        reservation = reservationFacade.find(reservationId);

        return navigatePatientAdmit();
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Functions">
    public AppointmentController() {
    }

    public void makeNull() {
        fromDate = null;
        toDate = null;
        appointmentNo = null;
        patientName = null;
        reservation = null;
        appointmentstatus = null;
        reservationDTOs = new ArrayList<>();
        comment = null;
    }

    public void updateChangesReservation() {
        if (reservation == null) {
            JsfUtil.addErrorMessage("No Reservation Found");
            return;
        }

        if (currentAppointment == null) {
            JsfUtil.addErrorMessage("No Appointment Found");
            return;
        }

        if (reservedRoom == null) {
            JsfUtil.addErrorMessage("No Reserved Room Found");
            return;
        }

        if (reservedFromDate.before(new Date())) {
            JsfUtil.addErrorMessage("Reserved From Date not Valid");
            return;
        }

        if (reservedToDate.before(reservedFromDate)) {
            JsfUtil.addErrorMessage("Reserved To Date not Valid");
            return;
        }

        if (currentAppointment.getAppointmentDate() == null) {
            JsfUtil.addErrorMessage("Appointment Date is Missing.");
            return;
        }

        if (currentBill.getReferredBy() == null) {
            JsfUtil.addErrorMessage("Referring Doctor is Missing.");
            return;
        }

        Reservation res = checkRoomAvailability();

        if (res != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm a");
            String fDate = sdf.format(res.getReservedFrom());
            String tDate = sdf.format(res.getReservedTo());
            JsfUtil.addErrorMessage("This room is already booked from " + fDate + " to " + tDate + ".");
            return;
        }

        savePatient(patient);
        updateBill();
        updateAppointment();
        updateReservation();
        JsfUtil.addSuccessMessage("Successfully Updated.");
    }

    public void updateBill() {
        if (currentBill.getId() == null) {
            billFacade.create(currentBill);
        } else {
            billFacade.edit(currentBill);
        }
    }

    private void updateAppointment() {
        if (currentAppointment.getId() == null) {
            appointmentFacade.create(currentAppointment);
        } else {
            appointmentFacade.edit(currentAppointment);
        }
    }

    private void updateReservation() {
        reservation.setReservedFrom(reservedFromDate);
        reservation.setReservedTo(reservedToDate);
        reservation.setRoom(reservedRoom);

        if (reservation.getId() == null) {
            reservationFacade.create(reservation);
        } else {
            reservationFacade.edit(reservation);
        }
    }

    public String navigateToManageAppointment(Long reservationId) {
        if (reservationId == null) {
            JsfUtil.addErrorMessage("Error in Reservation");
            return "";
        }

        reservation = reservationFacade.find(reservationId);

        if (reservation == null) {
            JsfUtil.addErrorMessage("No Reservation Found");
            return "";
        }

        reservedRoom = reservation.getRoom();
        reservedFromDate = reservation.getReservedFrom();
        reservedToDate = reservation.getReservedTo();

        currentAppointment = appointmentFacade.find(reservation.getAppointment().getId());

        if (currentAppointment == null) {
            JsfUtil.addErrorMessage("No Appointment Found");
            return "";
        }

        patient = patientFacade.find(currentAppointment.getPatient().getId());
        currentBill = billFacade.find(currentAppointment.getBill().getId());
        comment = null;

        return "/inward/inward_appointment_edit?faces-redirect=true";
    }

    public void searchAppointments() {
        reservationDTOs = new ArrayList<>();

        HashMap params = new HashMap();

        String jpql = "SELECT new com.divudi.core.data.dto.ReservationDTO( "
                + " COALESCE(res.id, 0),"
                + " res.reservedFrom, "
                + " res.reservedTo, "
                + " COALESCE(res.appointment.appointmentNumber, ''),"
                + " res.createdAt, "
                + " COALESCE(res.room.name, ''), "
                + " res.patient.person.title, "
                + " COALESCE(res.patient.person.name, ''), "
                + " res.patient.person.dob, "
                + " COALESCE(res.patient.person.sex, ''), "
                + " COALESCE(res.patient.person.mobile, ''), "
                + " res.appointment.bill.referredBy.person.title, "
                + " COALESCE(res.appointment.bill.referredBy.person.name, ''), "
                + " res.appointment.status "
                + " ) "
                + " FROM Reservation res "
                + " WHERE res.retired =:ret"
                + " AND res.reservedFrom BETWEEN :today AND :endDate ";

        if (appointmentstatus != null) {
            jpql += " AND res.appointment.status = :status ";
            params.put("status", appointmentstatus);
        }

        if (appointmentNo != null) {
            jpql += " AND res.appointment.appointmentNumber like :aptNumber ";
            params.put("aptNumber", "%" + appointmentNo.trim() + "%");
        }

        if (patientName != null) {
            jpql += " AND res.patient.person.name LIKE :patientName ";
            params.put("patientName", "%" + getPatientName().trim() + "%");
        }

        jpql += " ORDER BY res.createdAt";

        params.put("ret", false);
        params.put("today", fromDate);
        params.put("endDate", toDate);

        reservationDTOs = reservationFacade.findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);

    }

    public List<Reservation> searcheservations(String quary) {

        HashMap params = new HashMap();

        String jpql = "SELECT res "
                + " FROM Reservation res "
                + " WHERE res.retired =:ret"
                + " AND res.appointment.status = :status "
                + " AND "
                + " (res.appointment.appointmentNumber like :number "
                + " or res.patient.person.name like :name )"
                + " and res.appointment.appointmentType =:type";

        jpql += " ORDER BY res.createdAt";

        params.put("ret", false);
        params.put("number", "%" + quary + "%");
        params.put("name", "%" + quary + "%");
        params.put("type", AppointmentType.IP_APPOINTMENT);
        params.put("status", AppointmentStatus.PENDING);

        return reservationFacade.findByJpql(jpql, params);
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
        getCurrentAppointment().setAppointmentType(AppointmentType.IP_APPOINTMENT);
        getCurrentAppointment().setStatus(AppointmentStatus.PENDING);
        String appointmentNo = numberGenerator.inwardAppointmentNumberGeneratorYearly(sessionController.getInstitution(), AppointmentType.IP_APPOINTMENT);
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
        reservation.setRoom(reservedRoom);
        reservation.setReservedFrom(reservedFromDate);
        reservation.setReservedTo(reservedToDate);

        if (reservation.getId() == null) {
            getReservationFacade().create(reservation);
        } else {
            getReservationFacade().edit(reservation);
        }

    }

    public void settleBill() {

        if (errorCheck()) {
            return;
        }

        if (getPatient() != null && getPatient().getId() != null && getPatient().isBlacklisted() && configOptionApplicationController.getBooleanValueByKey("Enable blacklist patient management for inward from the system", false)) {
            JsfUtil.addErrorMessage("This patient is blacklisted from the system.");
            return;
        }

        if (reservation == null) {
            JsfUtil.addErrorMessage("Please select a patient room for the appoiment.");
            return;
        }

        if (getReservedRoom() == null) {
            JsfUtil.addErrorMessage("Please select a patient room for the appoiment.");
            return;
        }

        if (getReservedFromDate() == null) {
            JsfUtil.addErrorMessage("Please select a Reservation date for the appoiment.");
            return;
        }

        if (!getReservedFromDate().after(new Date())) {
            JsfUtil.addErrorMessage("Please select a valid Reservation from date and time without now.");
            return;
        }

        if (getReservedToDate() != null && (!getReservedToDate().after(new Date()) || !getReservedToDate().after(getReservedFromDate()))) {
            JsfUtil.addErrorMessage("Please select a valid Reservation todate.");
            return;
        }

        if (currentAppointment.getAppointmentDate() == null) {
            JsfUtil.addErrorMessage("Appointment Date is Missing.");
            return;
        }

        if (currentBill.getReferredBy() == null) {
            JsfUtil.addErrorMessage("Referring Doctor is Missing.");
            return;
        }

        Reservation res = checkRoomAvailability();

        if (res != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm a");
            String fDate = sdf.format(res.getReservedFrom());
            String tDate = sdf.format(res.getReservedTo());
            JsfUtil.addErrorMessage("This room is already booked from " + fDate + " to " + tDate + ".");
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

    private List<Payment> createPayment(Bill bill, PaymentMethod pm) {
        List<Payment> ps = new ArrayList<>();
        if (pm == PaymentMethod.MultiplePaymentMethods) {
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

        getCurrentBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrentBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getCurrentBill().setBillTypeAtomic(BillTypeAtomic.INWARD_APPOINTMENT_BILL);
        getCurrentBill().setBillType(BillType.InwardAppointmentBill);
        getCurrentBill().setPatient(p);

        getCurrentBill().setBillDate(new Date());
        getCurrentBill().setBillTime(new Date());

        getCurrentBill().setCreatedAt(new Date());
        getCurrentBill().setCreatedAt(new Date());
        getCurrentBill().setCreater(sessionController.getLoggedUser());
        getFacade().create(getCurrentBill());

    }

    private Bill saveCacelBill(Bill originalBill) {
        Bill newCancelBill = new Bill();

        newCancelBill.copy(originalBill);
        newCancelBill.copyValue(originalBill);
        newCancelBill.invertValueOfThisBill();

        newCancelBill.setDepartment(getSessionController().getLoggedUser().getDepartment());
        newCancelBill.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        newCancelBill.setBillTypeAtomic(BillTypeAtomic.INWARD_APPOINTMENT_CANCEL_BILL);
        newCancelBill.setBillType(BillType.InwardAppointmentBill);
        newCancelBill.setReferenceBill(originalBill);
        newCancelBill.setBillDate(new Date());
        newCancelBill.setBillTime(new Date());
        newCancelBill.setComments(comment);

        newCancelBill.setCreatedAt(new Date());
        newCancelBill.setCreater(sessionController.getLoggedUser());

        if (newCancelBill.getId() == null) {
            getFacade().create(newCancelBill);
        } else {
            getFacade().edit(newCancelBill);
        }

        //Update Original Bill
        originalBill.setCancelled(true);
        originalBill.setCancelledBill(newCancelBill);

        if (originalBill.getId() == null) {
            getFacade().create(originalBill);
        } else {
            getFacade().edit(originalBill);
        }

        createPayment(newCancelBill, newCancelBill.getPaymentMethod());

        return newCancelBill;
    }

    public void cancelAppointment() {
        if (comment == null || comment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("No Comment Selected.");
            return;
        }

        if (currentBill == null) {
            JsfUtil.addErrorMessage("Bill not Found.");
            return;
        }

        if (currentAppointment == null) {
            JsfUtil.addErrorMessage("Appointment is not Found.");
            return;
        }

        Bill cancelBill = saveCacelBill(currentBill);

        cancelAppointment(cancelBill, currentAppointment, comment);

        JsfUtil.addSuccessMessage("Appointment Canceled");
    }

    private void cancelAppointment(Bill CancelBill, Appointment apt, String reason) {
        apt.setAppointmentCancel(true);
        apt.setAppointmentCancelAt(new Date());
        apt.setAppointmentCancelReason(reason);
        apt.setAppointmentCancelBy(sessionController.getLoggedUser());
        apt.setStatus(AppointmentStatus.CANCEL);
        apt.setAppointmentCancelBill(CancelBill);
        if (apt.getId() == null) {
            appointmentFacade.create(apt);
        } else {
            appointmentFacade.edit(apt);
        }
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

        if (getPatient().getPerson().getMobile() == null && getPatient().getPerson().getPhone() == null) {
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

        if (!isPaymentAmountValid()) {
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrentBill().getPaymentMethod(), paymentMethodData)) {
            return true;
        }

        return false;
    }

    private boolean isPaymentAmountValid() {
        PaymentMethod pm = getCurrentBill().getPaymentMethod();

        if (pm == null) {
            return false;
        }

        double amountToCheck = 0.0;

        switch (pm) {
            case Cash:
                // For cash payment, check if bill total is greater than 0
                amountToCheck = getCurrentBill().getTotal();
                if (amountToCheck <= 0.0) {
                    JsfUtil.addErrorMessage("Please enter a payment amount");
                    return false;
                }
                break;

            case Card:
                // For card payment, check paymentMethodData
                if (paymentMethodData == null || paymentMethodData.getCreditCard() == null
                        || paymentMethodData.getCreditCard().getTotalValue() <= 0.0) {
                    JsfUtil.addErrorMessage("Please enter card payment details");
                    return false;
                }
                amountToCheck = paymentMethodData.getCreditCard().getTotalValue();
                break;

            case Cheque:
                // For cheque payment
                if (paymentMethodData == null || paymentMethodData.getCheque() == null
                        || paymentMethodData.getCheque().getTotalValue() <= 0.0) {
                    JsfUtil.addErrorMessage("Please enter cheque payment details");
                    return false;
                }
                amountToCheck = paymentMethodData.getCheque().getTotalValue();
                break;

            case Slip:
                // For slip payment
                if (paymentMethodData == null || paymentMethodData.getSlip() == null
                        || paymentMethodData.getSlip().getTotalValue() <= 0.0) {
                    JsfUtil.addErrorMessage("Please enter slip payment details");
                    return false;
                }
                amountToCheck = paymentMethodData.getSlip().getTotalValue();
                break;

            case MultiplePaymentMethods:
                // For multiple payment methods - check if it's properly configured
                if (paymentMethodData == null || paymentMethodData.getPaymentMethodMultiple() == null
                        || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null
                        || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
                    JsfUtil.addErrorMessage("Please configure payment amounts");
                    return false;
                }

                // Calculate total from all components
                double multipleTotal = 0.0;
                for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    if (cd != null && cd.getPaymentMethodData() != null) {
                        // Check based on payment method type in the component
                        switch (cd.getPaymentMethod()) {
                            case Cash:
                                if (cd.getPaymentMethodData().getCash() != null) {
                                    multipleTotal += cd.getPaymentMethodData().getCash().getTotalValue();
                                }
                                break;
                            case Card:
                                if (cd.getPaymentMethodData().getCreditCard() != null) {
                                    multipleTotal += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                                }
                                break;
                            case Cheque:
                                if (cd.getPaymentMethodData().getCheque() != null) {
                                    multipleTotal += cd.getPaymentMethodData().getCheque().getTotalValue();
                                }
                                break;
                            case Slip:
                                if (cd.getPaymentMethodData().getSlip() != null) {
                                    multipleTotal += cd.getPaymentMethodData().getSlip().getTotalValue();
                                }
                                break;
                        }
                    }
                }

                if (multipleTotal <= 0.0) {
                    JsfUtil.addErrorMessage("Please enter valid payment amounts");
                    return false;
                }
                amountToCheck = multipleTotal;
                break;

            // other payment methods as needed
            case ewallet:
                if (paymentMethodData == null || paymentMethodData.getEwallet() == null
                        || paymentMethodData.getEwallet().getTotalValue() <= 0.0) {
                    JsfUtil.addErrorMessage("Please enter eWallet payment details");
                    return false;
                }
                amountToCheck = paymentMethodData.getEwallet().getTotalValue();
                break;

            case Credit:
                if (paymentMethodData == null || paymentMethodData.getCredit() == null
                        || paymentMethodData.getCredit().getTotalValue() <= 0.0) {
                    JsfUtil.addErrorMessage("Please enter credit payment details");
                    return false;
                }
                amountToCheck = paymentMethodData.getCredit().getTotalValue();
                break;

            default:
                // For other payment methods, just check that bill total is valid
                amountToCheck = getCurrentBill().getTotal();
                if (amountToCheck <= 0.0) {
                    JsfUtil.addErrorMessage("Please enter a payment amount");
                    return false;
                }
        }

        // Update bill total with the validated amount
        //  ensures bill.total matches the payment amount
        if (getCurrentBill().getTotal() != amountToCheck) {
            getCurrentBill().setTotal(amountToCheck);
            getCurrentBill().setNetTotal(amountToCheck);
        }

        return true;
    }

    public List<Reservation> checkAppointmentsForRoom(RoomFacilityCharge r) {
        String jpql = "SELECT res FROM Reservation res "
                + "WHERE res.room = :room "
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

        List<Reservation> reservations = reservationFacade.findByJpql(jpql, hm);

        return reservations;
    }

    public Reservation checkRoomAvailability() {
        if (reservedRoom == null || reservedFromDate == null || reservedToDate == null) {
            JsfUtil.addErrorMessage("Reservation, room, and dates must not be null");
        }

        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT r FROM Reservation r "
                + "WHERE r.room = :room "
                + "AND r.appointment.status =:status "; // Optional: exclude cancelled reservations

        if (reservation.getId() != null) {
            jpql += " AND r.id !=:id ";
            parameters.put("id", reservation.getId());
        }

        jpql += " AND ( "
                + "   (r.reservedFrom < :reservedTo AND r.reservedTo > :reservedFrom) "
                + "   OR r.reservedFrom BETWEEN :reservedFrom AND :reservedTo "
                + "   OR r.reservedTo BETWEEN :reservedFrom AND :reservedTo "
                + ") "
                + "ORDER BY r.reservedFrom";

        parameters.put("room", reservedRoom);
        parameters.put("status", AppointmentStatus.PENDING);
        parameters.put("reservedFrom", reservedFromDate);
        parameters.put("reservedTo", reservedToDate);

        Reservation r = reservationFacade.findFirstByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        return r;
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

    public void prepereForInwardAppointPatient() {
        printPreview = false;
        searchedPatient = null;
        patient = null;
        currentBill = null;
        currentAppointment = null;
        reservation = null;
        reservedFromDate = null;
        reservedToDate = null;
        reservedRoom = null;
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
        return "/inward/inward_appointment?faces-redirect=true";
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getter & Setters">
    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
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

    public ReservationFacade getReservationFacade() {
        return reservationFacade;
    }

    public void setReservationFacade(ReservationFacade reservationFacade) {
        this.reservationFacade = reservationFacade;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            Double lookupDays = configOptionApplicationController.getDoubleValueByKey("Inward - Appoiment Lookup Duration (Days)", 30.0);

            Date today = getFromDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(today);
            cal.add(Calendar.DAY_OF_YEAR, lookupDays.intValue());
            toDate = cal.getTime();
        }

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
        if (reservationDTOs == null) {
            reservationDTOs = new ArrayList<>();
        }
        return reservationDTOs;
    }

    public void setReservationDTOs(List<ReservationDTO> reservationDTOs) {
        this.reservationDTOs = reservationDTOs;
    }

    public Date getReservedFromDate() {
        return reservedFromDate;
    }

    public void setReservedFromDate(Date reservedFromDate) {
        this.reservedFromDate = reservedFromDate;
    }

    public Date getReservedToDate() {
        return reservedToDate;
    }

    public void setReservedToDate(Date reservedToDate) {
        this.reservedToDate = reservedToDate;
    }

    public RoomFacilityCharge getReservedRoom() {
        return reservedRoom;
    }

    public void setReservedRoom(RoomFacilityCharge reservedRoom) {
        this.reservedRoom = reservedRoom;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
    // </editor-fold>

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

    /**
     * Converter for Reservation entity
     */
    @FacesConverter(forClass = Reservation.class)
    public static class ReservationControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty() || value.equals("null")) {
                return null;
            }
            AppointmentController controller = (AppointmentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "appointmentController");
            Long key = getKey(value);
            if (key == null) {
                return null;
            }
            return controller.getReservationFacade().find(key);
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
            if (object instanceof Reservation) {
                Reservation o = (Reservation) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Reservation.class.getName());
            }
        }
    }
}
