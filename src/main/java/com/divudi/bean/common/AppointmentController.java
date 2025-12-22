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
import static com.divudi.core.data.PaymentMethod.Slip;
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
import com.divudi.core.entity.PatientDeposit;
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
import com.divudi.service.PatientDepositService;
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
public class AppointmentController implements Serializable, ControllerWithPatient, ControllerWithMultiplePayments {

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

        getCurrentBill().setDeptId(appointmentNo);
        billFacade.edit(currentBill);
    }

    private void saveReservation(Patient p, Appointment a) {
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

        if (getPatient().getId() != null && getPatient().isBlacklisted() && configOptionApplicationController.getBooleanValueByKey("Enable blacklist patient management for inward from the system", false)) {
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

        if (errorCheck()) {
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
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCash().getComment());
                        break;
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
                    case Slip:
                        p.setPaidValue(cd.getPaymentMethodData().getSlip().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
                        p.setComments(cd.getPaymentMethodData().getSlip().getComment());
                        p.setReferenceNo(cd.getPaymentMethodData().getSlip().getReferenceNo());
                        p.setPaymentDate(cd.getPaymentMethodData().getSlip().getDate());
                        p.setChequeDate(cd.getPaymentMethodData().getSlip().getDate());
                        break;
                    case OnlineSettlement:
                        p.setBank(cd.getPaymentMethodData().getOnlineSettlement().getInstitution());
                        p.setPaidValue(cd.getPaymentMethodData().getOnlineSettlement().getTotalValue());
                        p.setPaymentDate(cd.getPaymentMethodData().getOnlineSettlement().getDate());
                        p.setReferenceNo(cd.getPaymentMethodData().getOnlineSettlement().getReferenceNo());
                        p.setComments(cd.getPaymentMethodData().getOnlineSettlement().getComment());
                        break;
                    case Staff_Welfare:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffWelfare().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffWelfare().getToStaff() != null) {
                            p.setToStaff(cd.getPaymentMethodData().getStaffWelfare().getToStaff());
                            staffBean.updateStaffWelfare(cd.getPaymentMethodData().getStaffWelfare().getToStaff(), cd.getPaymentMethodData().getStaffWelfare().getTotalValue());
                            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
                        }
                        break;
                    case ewallet:
                        p.setPolicyNo(cd.getPaymentMethodData().getEwallet().getReferralNo());
                        p.setReferenceNo(cd.getPaymentMethodData().getEwallet().getReferenceNo());
                        p.setCreditCompany(cd.getPaymentMethodData().getEwallet().getInstitution());
                        p.setPaidValue(cd.getPaymentMethodData().getEwallet().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getEwallet().getComment());
                        break;
                    case PatientDeposit:
                        double paidValue = cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                        PatientDeposit currentDeposit = cd.getPaymentMethodData().getPatient_deposit().getPatientDepost();
                        p.setPaidValue(paidValue);
                        patientDepositService.updateBalance(p, currentDeposit);
                        JsfUtil.addSuccessMessage("Patient Deposit Balance Updated");
                        break;

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
                case Cash:
                    p.setPaidValue(bill.getTotal());
                    p.setComments("");
                    break;
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
                case ewallet:
                    p.setBank(paymentMethodData.getEwallet().getInstitution());
                    p.setPolicyNo(paymentMethodData.getEwallet().getReferralNo());
                    p.setReferenceNo(paymentMethodData.getEwallet().getReferenceNo());
                    p.setCreditCompany(paymentMethodData.getEwallet().getInstitution());
                    p.setPaidValue(paymentMethodData.getEwallet().getTotalValue());
                    p.setComments(paymentMethodData.getEwallet().getComment());
                    break;
                case PatientDeposit:
                    double paidValue = paymentMethodData.getPatient_deposit().getTotalValue();
                    PatientDeposit currentDeposit = paymentMethodData.getPatient_deposit().getPatientDepost();
                    p.setPaidValue(paidValue);
                    patientDepositService.updateBalance(p, currentDeposit);
                    break;
                case OnlineSettlement:
                    p.setBank(paymentMethodData.getOnlineSettlement().getInstitution());
                    p.setPaidValue(paymentMethodData.getOnlineSettlement().getTotalValue());
                    p.setPaymentDate(paymentMethodData.getOnlineSettlement().getDate());
                    p.setReferenceNo(paymentMethodData.getOnlineSettlement().getReferenceNo());
                    p.setComments(paymentMethodData.getOnlineSettlement().getComment());
                    break;

            }

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
        String cancelNo = numberGenerator.inwardAppointmentNumberGeneratorYearly(sessionController.getInstitution(), AppointmentType.IP_APPOINTMENT_CANCELATION);
        newCancelBill.setDeptId(cancelNo);
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
        createCancelPayments(currentBill, cancelBill);

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

    @Inject
    BillSearch billSearch;

    private void createCancelPayments(Bill originalBill, Bill cancelBill) {

        List<Payment> originalPayments = billSearch.fetchBillPayments(originalBill);

        for (Payment p : originalPayments) {
            Payment newlyCancelPayment = p.clonePaymentForNewBill();

            newlyCancelPayment.invertValues();
            newlyCancelPayment.setReferancePayment(p);
            newlyCancelPayment.setBill(cancelBill);
            newlyCancelPayment.setInstitution(getSessionController().getInstitution());
            newlyCancelPayment.setDepartment(getSessionController().getDepartment());
            newlyCancelPayment.setCreatedAt(new Date());
            newlyCancelPayment.setCreater(getSessionController().getLoggedUser());
            paymentFacade.create(newlyCancelPayment);

            if (newlyCancelPayment.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                PatientDeposit pd = patientDepositService.getDepositOfThePatient(originalBill.getPatient(), originalBill.getDepartment());
                patientDepositService.updateBalance(newlyCancelPayment, pd);
            }
            if (newlyCancelPayment.getPaymentMethod() == PaymentMethod.Staff_Welfare) {
                if (p.getToStaff() != null) {
                    staffBean.updateStaffWelfare(newlyCancelPayment.getToStaff(), newlyCancelPayment.getPaidValue());
                    JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
                }
            }
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

        if (checkErrorsInPaymentMethod(getCurrentBill().getPaymentMethod(), paymentMethodData)) {
            return true;
        }

        return false;
    }

    @EJB
    PatientDepositService patientDepositService;

    public void updatePaymentData() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No current bill available");
            return;
        }

        PaymentMethod pm = currentBill.getPaymentMethod();

        if (pm != null) {
            if (pm == PaymentMethod.PatientDeposit) {

                if (patient == null) {
                    JsfUtil.addErrorMessage("No Patient is selected. Can't proceed with Patient Deposits");
                    return;
                }
                if (patient.getId() == null) {
                    JsfUtil.addErrorMessage("No Patient is selected. Can't proceed with Patient Deposits");
                    return;
                } else {
                    patient = patientFacade.find(patient.getId());

                    if (patient == null) {
                        JsfUtil.addErrorMessage("Patient not found in system");
                        return;
                    }

                    if (paymentMethodData == null) {
                        paymentMethodData = new PaymentMethodData();
                    }

                    paymentMethodData.getPatient_deposit().setPatient(patient);
                    if (!patient.getHasAnAccount()) {
                        JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                        return;
                    }
                    PatientDeposit pd = patientDepositService.getDepositOfThePatient(patient, sessionController.getDepartment());
                    paymentMethodData.getPatient_deposit().setPatientDepost(pd);
                }
            }
        }
    }

    private boolean checkErrorsInPaymentMethod(PaymentMethod method, PaymentMethodData methodData) {
        if (method == null) {
            JsfUtil.addErrorMessage("Please Select a Payment Method");
            return true;
        }
        double amountToCheck = 0.0;

        if (method == PaymentMethod.Cash) {
            amountToCheck = getCurrentBill().getTotal();
            if (amountToCheck <= 0.0) {
                JsfUtil.addErrorMessage("Please enter a payment amount");
                return true;
            }
        } else {
            if (methodData == null) {
                JsfUtil.addErrorMessage("Error in Payment Data.");
                return true;
            }

            switch (method) {
                case Card:
                    if (methodData.getCreditCard() == null) {
                        JsfUtil.addErrorMessage("Error in card payment Details");
                        return true;
                    } else {
                        if (methodData.getCreditCard().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the card payment amount");
                            return true;
                        }
                        if (methodData.getCreditCard().getInstitution() == null || methodData.getCreditCard().getNo() == null) {
                            JsfUtil.addErrorMessage("Please Fill Credit Card Number and Bank.");
                            return true;
                        }
                        amountToCheck = methodData.getCreditCard().getTotalValue();
                    }
                    break;

                case Cheque:
                    if (methodData.getCheque() == null) {
                        JsfUtil.addErrorMessage("Error in Cheque payment Details");
                        return true;
                    } else {
                        if (methodData.getCheque().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the cheque payment amount");
                            return true;
                        }
                        if (methodData.getCheque().getInstitution() == null || methodData.getCheque().getNo() == null || methodData.getCheque().getDate() == null) {
                            JsfUtil.addErrorMessage("Please select Cheque Number, Bank and Cheque Date.");
                            return true;
                        }
                        amountToCheck = methodData.getCheque().getTotalValue();
                    }
                    break;

                case Slip:
                    if (methodData.getSlip() == null) {
                        JsfUtil.addErrorMessage("Error in Slip payment Details");
                        return true;
                    } else {
                        if (methodData.getSlip().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the slip payment details");
                            return true;
                        }
                        if (methodData.getSlip().getInstitution() == null || methodData.getSlip().getDate() == null) {
                            JsfUtil.addErrorMessage("Please Fill Bank and Slip Date.");
                            return true;
                        }
                        amountToCheck = methodData.getSlip().getTotalValue();
                    }
                    break;

                case ewallet:
                    if (methodData.getEwallet() == null) {
                        JsfUtil.addErrorMessage("Error in eWallet payment Details");
                        return true;
                    } else {
                        if (methodData.getEwallet().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the eWallet payment Amount");
                            return true;
                        }
                        if (methodData.getEwallet().getInstitution() == null || ((methodData.getEwallet().getReferenceNo() == null || methodData.getEwallet().getReferenceNo().trim().isEmpty()) && (methodData.getEwallet().getNo() == null || methodData.getEwallet().getNo().trim().isEmpty()))) {
                            JsfUtil.addErrorMessage("Please Fill eWallet Reference Number and Bank.");
                            return true;
                        }
                        amountToCheck = methodData.getEwallet().getTotalValue();
                    }
                    break;

                case PatientDeposit:
                    if (methodData.getPatient_deposit() == null) {
                        JsfUtil.addErrorMessage("Error in Patient Deposit Details");
                        return true;
                    } else {
                        double creditLimitAbsolute = Math.abs(patient.getCreditLimit());
                        PatientDeposit pd = methodData.getPatient_deposit().getPatientDepost();

                        double availableForPurchase = pd.getBalance() + creditLimitAbsolute;
                        double payhingThisTimeValue;

                        if (methodData.getPatient_deposit().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the Patient Deposit payment Amount");
                            return true;
                        } else {
                            payhingThisTimeValue = methodData.getPatient_deposit().getTotalValue();
                            if (payhingThisTimeValue > availableForPurchase) {
                                JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                                return true;
                            }
                            amountToCheck = methodData.getPatient_deposit().getTotalValue();
                        }
                    }
                    break;

                case OnlineSettlement:
                    if (methodData.getOnlineSettlement() == null) {
                        JsfUtil.addErrorMessage("Error in Online Settlement Details");
                        return true;
                    } else {
                        if (methodData.getOnlineSettlement().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the Online Settlement payment Amount");
                            return true;
                        }
                        if (methodData.getOnlineSettlement().getInstitution() == null || (methodData.getOnlineSettlement().getReferenceNo() == null || methodData.getOnlineSettlement().getReferenceNo().trim().isEmpty()) && methodData.getOnlineSettlement().getDate() == null) {
                            JsfUtil.addErrorMessage("Please Fill Online Settlement Reference Number, Date and Bank.");
                            return true;
                        }
                        amountToCheck = methodData.getOnlineSettlement().getTotalValue();
                    }
                    break;

                case MultiplePaymentMethods:
                    if (methodData.getPaymentMethodMultiple() == null || methodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null || methodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
                        JsfUtil.addErrorMessage("Please configure payment amounts");
                        return true;
                    }

                    List<ComponentDetail> paymentDetailsList = methodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails();

                    if (paymentDetailsList.isEmpty()) {
                        JsfUtil.addErrorMessage("Please select the first payment method.");
                        return true;
                    }

                    if (paymentDetailsList.size() == 1) {
                        JsfUtil.addErrorMessage("You can't use only one payment method.");
                        return true;
                    }

                    // Calculate total from all components
                    double multipleTotal = 0.0;
                    int componentCount = 0;
                    for (ComponentDetail cd : paymentDetailsList) {
                        componentCount++;
                        if (cd != null && cd.getPaymentMethodData() != null) {
                            // Check based on payment method type in the component
                            Double checkAmount = 0.0;
                            switch (cd.getPaymentMethod()) {
                                case Cash:
                                    if (cd.getPaymentMethodData().getCash() == null) {
                                        JsfUtil.addErrorMessage("Please enter the amount to be paid from Cash.");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getCash().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter a cash amount");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;

                                case Card:
                                    if (cd.getPaymentMethodData().getCreditCard() == null) {
                                        JsfUtil.addErrorMessage("Error in card payment Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getCash().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Card.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getCreditCard().getTotalValue() <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter card payment amount");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getCreditCard().getInstitution() == null || cd.getPaymentMethodData().getCreditCard().getNo() == null) {
                                            JsfUtil.addErrorMessage("Please Fill Credit Card Number and Bank.");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;

                                case Cheque:
                                    if (cd.getPaymentMethodData().getCheque() == null) {
                                        JsfUtil.addErrorMessage("Error in Cheque payment Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getCheque().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Cheque.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getCheque().getInstitution() == null || cd.getPaymentMethodData().getCheque().getNo() == null || cd.getPaymentMethodData().getCheque().getDate() == null) {
                                            JsfUtil.addErrorMessage("Please select Cheque Number, Bank and Cheque Date.");
                                            return true;
                                        }

                                        multipleTotal += checkAmount;
                                    }
                                    break;

                                case Slip:
                                    if (cd.getPaymentMethodData().getSlip() == null) {
                                        JsfUtil.addErrorMessage("Error in Slip payment Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getSlip().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Back Slip.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getSlip().getInstitution() == null || cd.getPaymentMethodData().getSlip().getDate() == null) {
                                            JsfUtil.addErrorMessage("Please Fill Bank and Slip Date.");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;
                                case OnlineSettlement:
                                    if (cd.getPaymentMethodData().getOnlineSettlement() == null) {
                                        JsfUtil.addErrorMessage("Error in Online Settlement Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getOnlineSettlement().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Online Settlement.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getOnlineSettlement().getInstitution() == null || (cd.getPaymentMethodData().getOnlineSettlement().getReferenceNo() == null || cd.getPaymentMethodData().getOnlineSettlement().getReferenceNo().trim().isEmpty()) && cd.getPaymentMethodData().getOnlineSettlement().getDate() == null) {
                                            JsfUtil.addErrorMessage("Please Fill Online Settlement Reference Number, Date and Bank.");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;
                                case Staff_Welfare:
                                    if (cd.getPaymentMethodData().getStaffWelfare() == null) {
                                        JsfUtil.addErrorMessage("Error in Staff Welfare Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getStaffWelfare().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Staff Welfare.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getStaffWelfare().getToStaff() == null) {
                                            JsfUtil.addErrorMessage("Please Fill Welfare Staff Name");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;

                                case ewallet:
                                    if (cd.getPaymentMethodData().getEwallet() == null) {
                                        JsfUtil.addErrorMessage("Error in eWallet payment Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getEwallet().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from eWallet.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getEwallet().getInstitution() == null || ((cd.getPaymentMethodData().getEwallet().getReferenceNo() == null || cd.getPaymentMethodData().getEwallet().getReferenceNo().trim().isEmpty()) && (cd.getPaymentMethodData().getEwallet().getNo() == null || cd.getPaymentMethodData().getEwallet().getNo().trim().isEmpty()))) {
                                            JsfUtil.addErrorMessage("Please Fill eWallet Reference Number and Bank.");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;
                                case PatientDeposit:
                                    if (cd.getPaymentMethodData().getPatient_deposit() == null) {
                                        JsfUtil.addErrorMessage("Error in Patient Deposit Details");
                                        return true;
                                    } else {
                                        double currentPatientCreditLimitAbsolute = Math.abs(patient.getCreditLimit());
                                        PatientDeposit currentPatientDeposit = cd.getPaymentMethodData().getPatient_deposit().getPatientDepost();

                                        double maximumAmount = currentPatientDeposit.getBalance() + currentPatientCreditLimitAbsolute;
                                        double payhingThisTimeAmount = cd.getPaymentMethodData().getPatient_deposit().getTotalValue();

                                        if (payhingThisTimeAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Patient Deposit.");
                                            return true;
                                        }
                                        if (payhingThisTimeAmount > maximumAmount) {
                                            JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                                            return true;
                                        }
                                        multipleTotal += payhingThisTimeAmount;
                                    }
                                    break;
                                default:
                                    System.out.println("[DEBUG] Processing default/unknown payment method: " + method);
                            }
                        }
                    }

                    if (multipleTotal <= 0.0) {
                        JsfUtil.addErrorMessage("Please enter valid payment amounts");
                        return true;
                    }
                    amountToCheck = multipleTotal;
                    break;
                default:
                    System.out.println("[DEBUG] Processing default/unknown payment method: " + method);
            }
        }

        getCurrentBill().setTotal(amountToCheck);
        getCurrentBill().setNetTotal(amountToCheck);

        return false;
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
        paymentMethodData = null;
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
    }

    @Override
    public double calculatRemainForMultiplePaymentTotal() {
        double multiplePaymentMethodTotalValue = 0.0;
        if (currentBill.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {

            if (paymentMethodData != null && paymentMethodData.getPaymentMethodMultiple() != null && paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() != null) {
                for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    if (cd == null || cd.getPaymentMethodData() == null) {
                        continue;
                    }
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffWelfare().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getOnlineSettlement().getTotalValue();
                }
            }
            getCurrentBill().setTotal(multiplePaymentMethodTotalValue);
            getCurrentBill().setNetTotal(multiplePaymentMethodTotalValue);

        }
        return multiplePaymentMethodTotalValue;
    }

    @Override
    public void recieveRemainAmountAutomatically() {

        calculatRemainForMultiplePaymentTotal();
        if (getCurrentBill().getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {

            if (paymentMethodData == null
                    || paymentMethodData.getPaymentMethodMultiple() == null
                    || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null
                    || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
                return;
            }

            int arrSize = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
            ComponentDetail pm = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);

            switch (pm.getPaymentMethod()) {
                case Cash:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getCash().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCash().setTotalValue(0.0);
                    }
                    break;
                case Card:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getCreditCard().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCreditCard().setTotalValue(0.0);
                    }
                    break;
                case Cheque:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getCheque().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCheque().setTotalValue(0.0);
                    }
                    break;
                case Slip:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getSlip().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getSlip().setTotalValue(0.0);
                    }
                    break;
                case ewallet:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getEwallet().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getEwallet().setTotalValue(0.0);
                    }
                    break;
                case PatientDeposit:
                    Patient p = patientFacade.find(patient.getId());

                    if (p == null) {
                        break;
                    } else {
                        pm.getPaymentMethodData().getPatient_deposit().setPatient(p);
                        PatientDeposit pd = patientDepositService.getDepositOfThePatient(p, sessionController.getDepartment());

                        if (pd != null) {
                            pm.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                            pm.getPaymentMethodData().getPatient_deposit().setTotalValue(0.0);
                        }
                    }
                    break;
                case Credit:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getCredit().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCredit().setTotalValue(0.0);
                    }
                    break;
                case Staff:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getStaffCredit().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getStaffCredit().setTotalValue(0.0);
                    }
                    break;
                case Staff_Welfare:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getStaffWelfare().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getStaffWelfare().setTotalValue(0.0);
                    }
                    break;
                case OnlineSettlement:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getOnlineSettlement().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getOnlineSettlement().setTotalValue(0.0);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected value: " + pm.getPaymentMethod());
            }
        }
        listnerForPaymentMethodChange();
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
