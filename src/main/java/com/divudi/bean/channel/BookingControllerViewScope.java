/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.cashTransaction.FinancialTransactionController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.ControllerWithMultiplePayments;
import com.divudi.bean.common.ControllerWithPatientViewScope;
import com.divudi.bean.common.DoctorSpecialityController;
import com.divudi.bean.common.ItemForItemController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SecurityController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.ViewScopeDataTransferController;

import com.divudi.data.ApplicationInstitution;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.MessageType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.PersonInstitutionType;
import com.divudi.data.channel.ChannelScheduleEvent;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.ServiceSessionBean;
import com.divudi.ejb.SmsManagerEjb;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Patient;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Person;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Sms;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.channel.ArrivalRecord;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.entity.membership.MembershipScheme;
import com.divudi.entity.membership.PaymentSchemeDiscount;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.FingerPrintRecordFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.SmsFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.bean.opd.OpdBillController;
import com.divudi.data.BillFinanceType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.OptionScope;
import static com.divudi.data.PaymentMethod.OnlineSettlement;
import com.divudi.data.dataStructure.ComponentDetail;
import com.divudi.ejb.StaffBean;
import com.divudi.entity.Doctor;
import com.divudi.entity.Fee;
import com.divudi.entity.Payment;
import com.divudi.entity.UserPreference;
import com.divudi.entity.channel.AgentReferenceBook;
import com.divudi.entity.channel.AppointmentActivity;
import com.divudi.entity.channel.SessionInstanceActivity;
import com.divudi.facade.AgentReferenceBookFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.SessionInstanceFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultScheduleModel;
import org.primefaces.model.ScheduleModel;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@ViewScoped
public class BookingControllerViewScope implements Serializable, ControllerWithPatientViewScope, ControllerWithMultiplePayments {

    /**
     * EJBs
     */
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private ServiceSessionFacade serviceSessionFacade;
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    ItemFeeFacade ItemFeeFacade;
    @EJB
    private ChannelBean channelBean;
    @EJB
    FingerPrintRecordFacade fpFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private ServiceSessionBean serviceSessionBean;
    @EJB
    AgentHistoryFacade agentHistoryFacade;
    @EJB
    ItemFeeFacade itemFeeFacade;
    @EJB
    SmsManagerEjb smsManager;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    SessionInstanceFacade sessionInstanceFacade;
    @EJB
    private SmsFacade smsFacade;
    @EJB
    StaffBean staffBean;
    @EJB
    AgentReferenceBookFacade agentReferenceBookFacade;
    /**
     * Controllers
     */
    @Inject
    ConfigOptionController configOptionController;
    @Inject
    PriceMatrixController priceMatrixController;
    @Inject
    private SessionController sessionController;
    @Inject
    private ChannelBillController channelCancelController;
    @Inject
    private ChannelReportController channelReportController;
    @Inject
    private ChannelSearchController channelSearchController;
    @Inject
    ServiceSessionLeaveController serviceSessionLeaveController;
    @Inject
    ChannelBillController channelBillController;
    @Inject
    DoctorSpecialityController doctorSpecialityController;
    @Inject
    ChannelStaffPaymentBillController channelStaffPaymentBillController;
    @Inject
    AgentReferenceBookController agentReferenceBookController;
    @Inject
    BillBeanController billBeanController;
    @Inject
    BillController billController;
    @Inject
    SessionInstanceController sessionInstanceController;
    @Inject
    ItemForItemController itemForItemController;
    @Inject
    AppointmentActivityController appointmentActivityController;
    @Inject
    SessionInstanceActivityController sessionInstanceActivityController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private PaymentSchemeController paymentSchemeController;
    @Inject
    FinancialTransactionController financialTransactionController;
    @Inject
    ViewScopeDataTransferController viewScopeDataTransferController;
    @Inject
    OpdBillController opdBillController;
    @Inject
    ChannelScheduleController channelScheduleController;
    @Inject
    private CommonController commonController;
    @Inject
    SecurityController securityController;
    /**
     * Properties
     */
    private Speciality speciality;
    private Staff staff;
    private Staff toStaff;
    private boolean foriegn;
    private PaymentScheme paymentScheme;
    private boolean settleSucessFully;
    private Bill printingBill;
    boolean activeCreditLimitPannel = false;
    private Date channelDay;

    private SessionInstance selectedSessionInstance;
    private List<ItemFee> selectedItemFees;
    private List<ItemFee> sessionFees;
    private List<ItemFee> addedItemFees;
    private BillSession selectedBillSession;
    private List<AppointmentActivity> selectedAppointmentActivities;

    private List<SessionInstance> sessionInstances;
    private List<SessionInstance> sessionInstancesFiltered;
    private List<SessionInstance> sortedSessionInstances;
    private List<SessionInstance> oldSessionInstancesFiltered;
    private String sessionInstanceFilter;
    private List<BillSession> billSessions;
    private List<Staff> consultants;
    private List<BillSession> getSelectedBillSession;
    private boolean printPreview;
    private boolean printPreviewForSettle;
    private boolean printPreviewForReprintingAsOriginal;
    private boolean printPreviewForReprintingAsDuplicate;
    private boolean printPreviewForOnlineBill;
    private boolean printPreviewC;

    private double absentCount;
    private int serealNo;
    private Date fromDate;
    private Date toDate;
    private Boolean needToFillBillSessions;
    private Boolean needToFillBillSessionDetails;
    private Boolean needToFillSessionInstances;
    private Boolean needToFillSessionInstanceDetails;

    private String comment;
    private String commentR;
    private Date sessionStartingDate;
    private String selectTextSpeciality = "";
    private String selectTextConsultant = "";
    private String selectTextSession = "";
    private ArrivalRecord arrivalRecord;
    private String errorText;
    private Integer selectedReserverdBookingNumber;
    private List<Integer> reservedBookingNumbers;
    private Patient patient;
    private PaymentMethod paymentMethod;
    private PaymentMethodData paymentMethodData;
    private AppointmentActivity appointmentActivity;
    private String quickSearchPhoneNumber;

    private ScheduleModel eventModel;
    boolean patientDetailsEditable;

    private Item itemToAddToBooking;
    private List<Item> itemsAvailableToAddToBooking;
    private List<Item> itemsAddedToBooking;
    private Institution institution;
    private String agentRefNo;
    private List<BillFee> listBillFees;

    private ChannelScheduleEvent event = new ChannelScheduleEvent();

    private Double feeTotalForSelectedBill;
    private Double feeDiscountForSelectedBill;
    private Double feeNetTotalForSelectedBill;
    private PaymentMethod cancelPaymentMethod;
    private PaymentMethod settlePaymentMethod;

    private PaymentMethod refundPaymentMethod;

    private Institution settleInstitution;
    private double refundableTotal = 0;
    private boolean disableRefund;
    private List<Patient> quickSearchPatientList;
    private double total;
    private List<Payment> payments;

    @Deprecated
    private ServiceSession selectedServiceSession;
    @Deprecated
    private BillSession managingBillSession;
    private String strTenderedValue;
    private double cashPaid;
    private double cashBalance = 0.0;
    private List<BillItem> additionalBillItems;

    private Institution referredByInstitution;
    private Doctor referredBy;
    private Institution collectingCentre;
    private double netPlusVat;
    private Institution creditCompany;
    private List<SessionInstance> sessionInstanceByDoctor;
    private Staff selectedConsultant;
    private SessionInstance selectedSessionInstanceForRechedule;
    private boolean reservedBooking;
    private BillItem selectedBillItem;
    private BillSession newBillSessionForSMS;

    private String encryptedBillSessionId;
    private String encryptedExpiary;

    public void sessionReschedule() {
        if (getSelectedSessionInstanceForRechedule() == null) {
            JsfUtil.addErrorMessage("Pleace Select Session For Rechedule");
            return;
        }

        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("Bill session is not valid !");
            return;
        }

        if (getSelectedSessionInstanceForRechedule().getMaxNo() != 0) {
            if (getSelectedSessionInstanceForRechedule().getBookedPatientCount() != null) {
                int maxNo = getSelectedSessionInstanceForRechedule().getMaxNo();
                long bookedPatientCount = getSelectedSessionInstanceForRechedule().getBookedPatientCount();
                if (maxNo <= bookedPatientCount) {
                    JsfUtil.addErrorMessage("Cannot reschedule the selected session: The session has reached its maximum booking capacity.");
                    return;

                }
            }
        }

        if (selectedBillSession.getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Cannot reschedule: This bill session has been cancelled.");
        }

        if (selectedBillSession.isRecheduledSession()) {
            JsfUtil.addErrorMessage("Cannot reschedule: This bill session has been Alrady Recheduled To Another Session !");
        }

        if (selectedBillSession.getReferenceBillSession() == null) {
            createBillSessionForReschedule(selectedBillSession, getSelectedSessionInstanceForRechedule());
            JsfUtil.addSuccessMessage("Reschedule Successfully");
            sendSmsOnChannelBookingReschedule();
        } else {
            JsfUtil.addErrorMessage("Cannot reschedule the selected session: This appointment has already been rescheduled.");
        }

    }

    public void sendSmsOnChannelBookingReschedule() {
        if (selectedBillSession == null) {
            return;
        }
        if (selectedBillSession.getBill() == null) {
            return;
        }
        if (selectedBillSession.getBill().getPatient().getPerson().getSmsNumber() == null) {
            return;
        }
        if (getSelectedSessionInstanceForRechedule() == null) {
            return;
        }
        Sms e = new Sms();
        e.setCreatedAt(new Date());
        e.setCreater(sessionController.getLoggedUser());
        e.setBill(selectedBillSession.getBill());
        e.setReceipientNumber(selectedBillSession.getBill().getPatient().getPerson().getSmsNumber());
        e.setSendingMessage(createChanelBookingRescheduleSms(selectedBillSession.getBill(), newBillSessionForSMS));
        e.setDepartment(getSessionController().getLoggedUser().getDepartment());
        e.setInstitution(getSessionController().getLoggedUser().getInstitution());
        e.setPending(false);
        e.setSmsType(MessageType.ChannelPatientReschedule);
        getSmsFacade().create(e);
        Boolean sent = smsManager.sendSms(e);

//        JsfUtil.addSuccessMessage("SMS Sent to all Patients.");
    }

    private String createChanelBookingRescheduleSms(Bill b, BillSession s) {
//        String template = sessionController.getDepartmentPreference().getSmsTemplateForChannelBooking();
        String template = configOptionController.getLongTextValueByKey("Template for SMS sent on Patient Feedback", OptionScope.APPLICATION, null, null, null);
        if (template == null || template.isEmpty()) {
            template = "Dear {patient_name}, Your appointment No. {serial_no} with {doctor} is rescheduled to appointment No. {new_serial_no} with {new_doctor} on {new_appointment_date} at {new_appointment_time} . {ins_name}";
        }
        return createSmsForChannelBookingReschedule(b, s, template);
    }

    private void createBillSessionForReschedule(BillSession bs, SessionInstance si) {
        BillSession newBillSession = new BillSession();
        if (bs == null) {
            return;
        }
        if (si == null) {
            return;
        }

        newBillSession.copy(bs);
        newBillSession.setBill(bs.getBill());
        newBillSession.setBillItem(bs.getBillItem());
        newBillSession.setReferenceBillSession(bs);
        newBillSession.setCreatedAt(new Date());
        newBillSession.setCreater(getSessionController().getLoggedUser());
        newBillSession.setSessionInstance(getSelectedSessionInstanceForRechedule());
        newBillSession.setSessionDate(getSelectedSessionInstanceForRechedule().getSessionDate());
        newBillSession.setSessionTime(getSelectedSessionInstanceForRechedule().getSessionTime());
        newBillSession.setStaff(getSelectedSessionInstanceForRechedule().getStaff());
        List<Integer> lastSessionReservedNumbers = CommonFunctions.convertStringToIntegerList(getSelectedSessionInstance().getOriginatingSession().getReserveNumbers());
        List<Integer> reservedNumbers = CommonFunctions.convertStringToIntegerList(getSelectedSessionInstanceForRechedule().getOriginatingSession().getReserveNumbers());

        Integer count = null;
        boolean reservedSession;

        if (reservedBooking) {
            if (lastSessionReservedNumbers.isEmpty()) {
                JsfUtil.addErrorMessage("No Reserved Numbers FInd !");
                return;
            }
            for (Integer rn : lastSessionReservedNumbers) {
                System.out.println("rn = " + rn);
                if (bs.getSerialNo() == rn) {
                    count = serviceSessionBean.getNextAvailableReservedNumber(getSelectedSessionInstanceForRechedule(), reservedNumbers, selectedReserverdBookingNumber);
                    if (count == null) {
                        count = serviceSessionBean.getNextNonReservedSerialNumber(getSelectedSessionInstanceForRechedule(), reservedNumbers);
                        JsfUtil.addErrorMessage("No reserved numbers available. Normal number is given");
                    }
                }
            }

        } else {
            count = serviceSessionBean.getNextNonReservedSerialNumber(getSelectedSessionInstanceForRechedule(), reservedNumbers);
        }
        if (count != null) {
            newBillSession.setSerialNo(count);
            System.out.println("count = " + count);
        } else {
            newBillSession.setSerialNo(1);
            System.out.println("count serial number= " + bs.getSerialNo());
        }
        getBillSessionFacade().create(newBillSession);
        bs.setRecheduledSession(true);
        bs.setReferenceBillSession(newBillSession);
        getBillSessionFacade().edit(bs);
        newBillSessionForSMS = newBillSession;
    }

    public void fillSessionInstanceByDoctor() {
        sessionInstanceByDoctor = new ArrayList<>();
        Date currentDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(Calendar.DAY_OF_MONTH, 2);
        Map<String, Object> m = new HashMap<>();
        selectedConsultant = selectedBillSession.getSessionInstance().getStaff();
        String jpql = "select i from SessionInstance i where i.retired = :ret and i.sessionDate >=:cd";

        if (selectedConsultant != null) {
            jpql += " and i.originatingSession.staff =:staff";
            m.put("staff", selectedConsultant);
        }
        m.put("ret", false);
        m.put("cd", currentDate);
        sessionInstanceByDoctor = sessionInstanceFacade.findByJpql(jpql.toString(), m, TemporalType.DATE);
        System.out.println("sessionInstanceByDoctor = " + sessionInstanceByDoctor.size());
    }

    public void removeAddedAditionalItems(Item item) {
        itemsAddedToBooking.remove(item);
        fillFees();
    }

    public void closePrinting() {
        printPreview = false;
        printPreviewForSettle = false;
        printPreviewForReprintingAsDuplicate = false;
        printPreviewForReprintingAsOriginal = false;
        printPreviewForOnlineBill = false;
    }

    public void toReprintAsOriginal() {
        markBillAsRePrinted();
        printPreview = true;
        printPreviewForSettle = false;
        printPreviewForReprintingAsDuplicate = false;
        printPreviewForReprintingAsOriginal = true;
    }

    public void toReprintAsDuplicate() {
        markBillAsRePrinted();
        printPreview = true;
        printPreviewForSettle = false;
        printPreviewForReprintingAsDuplicate = true;
        printPreviewForReprintingAsOriginal = false;
    }

    public void toPrintOnlineBill() {
        selectedBillSession.getBill().setPrinted(true);
        billSessionFacade.edit(selectedBillSession);
        printPreviewForOnlineBill = true;
        printPreview = false;
        printPreviewForSettle = false;
        printPreviewForReprintingAsDuplicate = false;
        printPreviewForReprintingAsOriginal = false;
    }

    public void markSettlingBillAsPrinted() {
        System.out.println("markSettlingBillAsPrinted");
        System.out.println("selectedBillSession.getBillItem().getBill() = " + selectedBillSession.getBillItem().getBill());
        if (selectedBillSession != null && selectedBillSession.getBillItem() != null && selectedBillSession.getBillItem().getBill() != null) {
            selectedBillSession.getBillItem().getBill().setPrinted(true);
            selectedBillSession.getBillItem().getBill().setPrintedAt(new Date());
            selectedBillSession.getBillItem().getBill().setPrintedUser(sessionController.getLoggedUser());
            billFacade.edit(selectedBillSession.getBillItem().getBill());
        } else {
            System.out.println("Can not mark as Printed = " + selectedBillSession.getBillItem().getBill());
        }
        if (selectedBillSession != null && selectedBillSession.getBillItem() != null && selectedBillSession.getBillItem().getBill() != null
                && selectedBillSession.getBillItem().getBill().getPaidBill() != null) {
            selectedBillSession.getBillItem().getBill().getPaidBill().setPrinted(true);
            selectedBillSession.getBillItem().getBill().getPaidBill().setPrintedAt(new Date());
            selectedBillSession.getBillItem().getBill().getPaidBill().setPrintedUser(sessionController.getLoggedUser());
            billFacade.edit(selectedBillSession.getBillItem().getBill().getPaidBill());
        } else {
            System.out.println("Can not mark Paid Bill as Printed = " + selectedBillSession.getBillItem().getBill().getPaidBill());
        }
    }

    public void markBillAsRePrinted() {
        if (selectedBillSession != null && selectedBillSession.getBillItem() != null && selectedBillSession.getBillItem().getBill() != null) {
            selectedBillSession.getBillItem().getBill().setDuplicatePrintedAt(new Date());
            selectedBillSession.getBillItem().getBill().setDuplicatedPrinted(true);
            selectedBillSession.getBillItem().getBill().setDuplicatePrintedUser(sessionController.getLoggedUser());
            billFacade.edit(selectedBillSession.getBillItem().getBill());
        } else {
            System.out.println("Can not mark as Printed = " + selectedBillSession.getBillItem().getBill());
        }
        if (selectedBillSession != null && selectedBillSession.getBillItem() != null && selectedBillSession.getBillItem().getBill() != null
                && selectedBillSession.getBillItem().getBill().getPaidBill() != null) {
            selectedBillSession.getBillItem().getBill().getPaidBill().setDuplicatedPrinted(true);
            selectedBillSession.getBillItem().getBill().getPaidBill().setDuplicatePrintedAt(new Date());
            selectedBillSession.getBillItem().getBill().getPaidBill().setDuplicatePrintedUser(sessionController.getLoggedUser());
            billFacade.edit(selectedBillSession.getBillItem().getBill().getPaidBill());
        } else {
            System.out.println("Can not mark Paid Bill as Printed = " + selectedBillSession.getBillItem().getBill().getPaidBill());
        }
    }

    public double calculatRemainForMultiplePaymentTotal() {

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();

            }
            return feeTotalForSelectedBill - multiplePaymentMethodTotalValue;
        }
        return feeTotalForSelectedBill;
    }

    public void recieveRemainAmountAutomatically() {
        double remainAmount = calculatRemainForMultiplePaymentTotal();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            int arrSize = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
            ComponentDetail pm = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);
            if (pm.getPaymentMethod() == PaymentMethod.Cash) {
                pm.getPaymentMethodData().getCash().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Card) {
                pm.getPaymentMethodData().getCreditCard().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Cheque) {
                pm.getPaymentMethodData().getCheque().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Slip) {
                pm.getPaymentMethodData().getSlip().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.ewallet) {
                pm.getPaymentMethodData().getEwallet().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                pm.getPaymentMethodData().getPatient_deposit().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Credit) {
                pm.getPaymentMethodData().getCredit().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Staff) {
                pm.getPaymentMethodData().getStaffCredit().setTotalValue(remainAmount);
            }

        }
    }

    public String navigateToManageSessionInstance(SessionInstance sessionInstance) {
        this.selectedSessionInstance = sessionInstance;

        // Setting the properties in the viewScopeDataTransferController
        viewScopeDataTransferController.setSelectedSessionInstance(sessionInstance);
        viewScopeDataTransferController.setSessionInstanceFilter(sessionInstanceFilter);
        viewScopeDataTransferController.setFromDate(fromDate);
        viewScopeDataTransferController.setToDate(toDate);
        viewScopeDataTransferController.setNeedToFillSessionInstances(false);
        viewScopeDataTransferController.setNeedToFillBillSessions(true);
        viewScopeDataTransferController.setNeedToFillSessionInstanceDetails(true);
        viewScopeDataTransferController.setNeedToFillBillSessionDetails(false);
        viewScopeDataTransferController.setNeedToFillMembershipDetails(false);
        viewScopeDataTransferController.setNeedToPrepareForNewBooking(false);

        return "/channel/session_instance?faces-redirect=true";
    }

    public String navigateToManageSessionInstanceFromProfPay(SessionInstance sessionInstance) {
        this.selectedSessionInstance = sessionInstance;

        // Setting the properties in the viewScopeDataTransferController
        viewScopeDataTransferController.setSelectedSessionInstance(sessionInstance);
        viewScopeDataTransferController.setSessionInstanceFilter(sessionInstanceFilter);
        viewScopeDataTransferController.setFromDate(fromDate);
        viewScopeDataTransferController.setToDate(toDate);
        viewScopeDataTransferController.setNeedToFillSessionInstances(false);
        viewScopeDataTransferController.setNeedToFillBillSessions(true);
        viewScopeDataTransferController.setNeedToFillSessionInstanceDetails(true);
        viewScopeDataTransferController.setNeedToFillBillSessionDetails(false);
        viewScopeDataTransferController.setNeedToFillMembershipDetails(false);
        viewScopeDataTransferController.setNeedToPrepareForNewBooking(false);
        channelStaffPaymentBillController.makenull();

        return "/channel/session_instance?faces-redirect=true";
    }

    public String navigateToManageSessionInstanceFromBillSession() {
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("No Bill Session");
            return null;
        }
        // Setting the properties in the viewScopeDataTransferController
        viewScopeDataTransferController.setSelectedSessionInstance(selectedBillSession.getSessionInstance());
        viewScopeDataTransferController.setSessionInstanceFilter(sessionInstanceFilter);
        viewScopeDataTransferController.setFromDate(fromDate);
        viewScopeDataTransferController.setToDate(toDate);
        viewScopeDataTransferController.setNeedToFillBillSessions(true);
        viewScopeDataTransferController.setNeedToFillBillSessionDetails(false);
        viewScopeDataTransferController.setNeedToFillSessionInstances(false);
        viewScopeDataTransferController.setNeedToFillSessionInstanceDetails(true);
        viewScopeDataTransferController.setNeedToFillMembershipDetails(false);
        viewScopeDataTransferController.setNeedToPrepareForNewBooking(false);
        return "/channel/session_instance?faces-redirect=true";
    }

    public void cancelSession() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("No Session Instance is Selected");
            return;
        }
        if (selectedSessionInstance.isStarted()) {
            JsfUtil.addErrorMessage("Session Already Started. Can not cancel.");
            return;
        }
        if (selectedSessionInstance.isCompleted()) {
            JsfUtil.addErrorMessage("Session Already Completed. Can not cancel.");
            return;
        }
        selectedSessionInstance.setCancelled(true);
        selectedSessionInstance.setCancelledAt(new Date());
        selectedSessionInstance.setCancelledBy(sessionController.getLoggedUser());
        sessionInstanceFacade.edit(selectedSessionInstance);
        sendSmsChannelSessionCancelNotification();
        JsfUtil.addErrorMessage("Cancelled");

    }

    public void sendSmsChannelSessionCancelNotification() {

        if (selectedSessionInstance == null) {
            return;
        }

        for (BillSession bs : billSessions) {
            if (bs.getBill() == null) {
                continue;
            }
            if (bs.getBill().getPatient().getPerson().getSmsNumber() == null) {
                continue;
            }
            Sms e = new Sms();
            e.setCreatedAt(new Date());
            e.setCreater(sessionController.getLoggedUser());
            e.setBill(bs.getBill());
            e.setReceipientNumber(bs.getBill().getPatient().getPerson().getSmsNumber());
            e.setSendingMessage(createChanellSessionCancellationBookingSms(bs.getBill()));
            e.setDepartment(getSessionController().getLoggedUser().getDepartment());
            e.setInstitution(getSessionController().getLoggedUser().getInstitution());
            e.setPending(false);
            e.setSmsType(MessageType.ChannelCancellation);
            getSmsFacade().create(e);

        }
        JsfUtil.addSuccessMessage("SMS Sent to all Patients.");
    }

    public void reopenSession() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("No Session Instance is Selected");
            return;
        }
        if (!selectedSessionInstance.isCompleted()) {
            JsfUtil.addErrorMessage("Session Not Completed. Can not Reopen.");
            return;
        }
        selectedSessionInstance.setCompleted(false);
        sessionInstanceFacade.editAndCommit(selectedSessionInstance);
        JsfUtil.addErrorMessage("Reopened");
    }

    public void completeSession() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("No Session Instance is Selected");
            return;
        }
        if (selectedSessionInstance.isStarted()) {
            JsfUtil.addErrorMessage("Session not yet Started. Can not complete.");
            return;
        }
        if (selectedSessionInstance.isCompleted()) {
            JsfUtil.addErrorMessage("Session already Completed. Can not complete again.");
            return;
        }
        selectedSessionInstance.setCompleted(true);
        selectedSessionInstance.setCompletedAt(new Date());
        selectedSessionInstance.setCompletedBy(sessionController.getLoggedUser());
        sessionInstanceFacade.editAndCommit(selectedSessionInstance);
        JsfUtil.addErrorMessage("Reopened");
    }

    public void saveSessionInstanceDetails() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("No Session Instance is Selected");
            return;
        }
        if (selectedSessionInstance.getEndingTime().equals(selectedSessionInstance.getStartingTime()) || selectedSessionInstance.getEndingTime().before(selectedSessionInstance.getStartingTime())) {
            JsfUtil.addErrorMessage("Starting Time and Endtime are the same or Endtime is before Starting Time");
            return;
        }
        selectedSessionInstance.setEditedAt(new Date());
        selectedSessionInstance.setEditer(sessionController.getLoggedUser());
        sessionInstanceFacade.edit(selectedSessionInstance);

        JsfUtil.addSuccessMessage("Updated");
    }

    public String navigateToEditSessionInstance() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("No Session Instance is selected");
            return null;
        }
        if (selectedSessionInstance.getOriginatingSession() == null) {
            JsfUtil.addErrorMessage("No Originating Session is available");
            return null;
        }
        if (selectedSessionInstance.getOriginatingSession().getStaff() == null) {
            JsfUtil.addErrorMessage("No Staff linked to the Originating Session");
            return null;
        }
        if (selectedSessionInstance.getOriginatingSession().getStaff().getSpeciality() == null) {
            JsfUtil.addErrorMessage("Staff member has no Speciality defined");
            return null;
        }
        channelScheduleController.setSpeciality(selectedSessionInstance.getStaff().getSpeciality());
        channelScheduleController.getSpecialityStaff();
        channelScheduleController.setCurrentStaff(selectedSessionInstance.getStaff());
        channelScheduleController.getItems();
        channelScheduleController.setCurrent(selectedSessionInstance.getOriginatingSession());
        channelScheduleController.fillSessionInstance();
        channelScheduleController.setCurrentSessionInstance(selectedSessionInstance);
//        channelScheduleController.fillFees();
        return channelScheduleController.navigateToChannelScheduleManagement();
    }

    public String navigateToEditOriginatingSession() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("No Session Instance is selected");
            return null;
        }
        if (selectedSessionInstance.getOriginatingSession() == null) {
            JsfUtil.addErrorMessage("No Originating Session is available");
            return null;
        }
        if (selectedSessionInstance.getOriginatingSession().getStaff() == null) {
            JsfUtil.addErrorMessage("No Staff linked to the Originating Session");
            return null;
        }
        if (selectedSessionInstance.getOriginatingSession().getStaff().getSpeciality() == null) {
            JsfUtil.addErrorMessage("Staff member has no Speciality defined");
            return null;
        }
        channelScheduleController.setSpeciality(selectedSessionInstance.getStaff().getSpeciality());
        channelScheduleController.getSpecialityStaff();
        channelScheduleController.setCurrentStaff(selectedSessionInstance.getStaff());
        channelScheduleController.getItems();
        channelScheduleController.fillSessionInstance();
        channelScheduleController.setCurrent(selectedSessionInstance.getOriginatingSession());
        channelScheduleController.fillFees();
        return channelScheduleController.navigateToChannelSchedule();
    }

    public void addSingleDateToToDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getFromDate());
        cal.add(Calendar.DATE, 1);
        toDate = cal.getTime();
        listAllSesionInstances();
        filterSessionInstances();
    }

    public void addToDayToToDate() {
        toDate = new Date();
        listAllSesionInstances();
        filterSessionInstances();
    }

    public void addTwoDays() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getFromDate());
        cal.add(Calendar.DATE, 2);
        toDate = cal.getTime();
        listAllSesionInstances();
        filterSessionInstances();
    }

    public void addSevenDays() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getFromDate());
        cal.add(Calendar.DATE, 7);
        toDate = cal.getTime();
        listAllSesionInstances();
        filterSessionInstances();
    }

    public void addMonth() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getFromDate());
        cal.add(Calendar.MONTH, 1);
        toDate = cal.getTime();
        listAllSesionInstances();
        filterSessionInstances();
    }

    public void filterSessionInstances() {
        if (sessionInstanceFilter == null || sessionInstanceFilter.trim().isEmpty()) {
            if (sessionInstances != null) {
                sessionInstancesFiltered = new ArrayList<>(sessionInstances);
            } else {
                sessionInstancesFiltered = new ArrayList<>();
                return;
            }
            return;
        }

        sessionInstancesFiltered = new ArrayList<>();
        String[] filterKeywords = sessionInstanceFilter.trim().toLowerCase().split("\\s+");

        for (SessionInstance si : sessionInstances) {
            String match1 = (si.getOriginatingSession() != null && si.getOriginatingSession().getName() != null)
                    ? si.getOriginatingSession().getName().toLowerCase() : "";
            String match2 = (si.getOriginatingSession() != null && si.getOriginatingSession().getStaff() != null
                    && si.getOriginatingSession().getStaff().getPerson() != null
                    && si.getOriginatingSession().getStaff().getPerson().getName() != null)
                    ? si.getOriginatingSession().getStaff().getPerson().getName().toLowerCase() : "";
            String match3 = (si.getOriginatingSession() != null && si.getOriginatingSession().getStaff() != null
                    && si.getOriginatingSession().getStaff().getSpeciality() != null
                    && si.getOriginatingSession().getStaff().getSpeciality().getName() != null)
                    ? si.getOriginatingSession().getStaff().getSpeciality().getName().toLowerCase() : "";

            boolean matchesAll = true;
            for (String keyword : filterKeywords) {
                if (!(match1.contains(keyword) || match2.contains(keyword) || match3.contains(keyword))) {
                    matchesAll = false;
                    break;
                }
            }

            if (matchesAll) {
                sessionInstancesFiltered.add(si);
            }
        }
        if (!sessionInstancesFiltered.isEmpty()) {
            selectedSessionInstance = selectedSessionInstance = sessionInstancesFiltered.get(0);
            sessionInstanceSelected();
        }
    }

    public void handleDropAndNavigate() {
        // Retrieve the session ID from the request parameter
        String sessionId = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("sessionId");

        // Find the session by ID and navigate
        SessionInstance selectedSession = sessionInstancesFiltered.stream()
                .filter(s -> s.getId().equals(sessionId))
                .findFirst()
                .orElse(null);

        if (selectedSession != null) {
            this.selectedSessionInstance = selectedSession;
            navigateToAddBooking();
        }
    }

    public void sessionInstanceSelected() {
        fillSessionInstanceDetails();
        calculateSelectedBillSessionTotal();
    }

    public void fillSessionInstanceDetails() {
        if (selectedSessionInstance == null) {
            return;
        }
        if (selectedSessionInstance.getOriginatingSession() == null) {
            return;
        }
        fillItemAvailableToAdd();
        fillFees();
        fillReservedNumbers();
        printPreview = false;
        paymentMethod = sessionController.getDepartmentPreference().getChannellingPaymentMethod();
    }

    public void fillReservedNumbers() {
        reservedBookingNumbers = CommonFunctions.convertStringToIntegerList(getSelectedSessionInstance().getOriginatingSession().getReserveNumbers());
    }

    public boolean chackNull(String template) {
        boolean chack;
        chack = template.isEmpty();
        return chack;
    }

    public String fillDataForChannelingBillHeader(String template, Bill bill) {
        String output;

        output = template
                .replace("{from_department}", bill.getDepartment().getName())
                .replace("{from_department_address}", bill.getDepartment().getAddress())
                .replace("{telephone1}", bill.getDepartment().getTelephone1())
                .replace("{telephone2}", bill.getDepartment().getTelephone2())
                .replace("{email}", bill.getDepartment().getEmail())
                .replace("{fax}", bill.getDepartment().getFax());
        return output;
    }

    public void markSessionInstanceAsStarted() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("No session selected");
            return;
        }
        selectedSessionInstance.setStarted(true);
        selectedSessionInstance.setStartedAt(new Date());
        selectedSessionInstance.setStartedBy(sessionController.getLoggedUser());
        sessionInstanceController.save(selectedSessionInstance);
        JsfUtil.addSuccessMessage("Session Started");
        if (sessionController.getDepartmentPreference().isSendSmsOnChannelDoctorArrival()) {
            sendSmsOnChannelDoctorArrival();
        }
        for (BillSession bs : billSessions) {
            if (!bs.isCompleted()) {
                if (configOptionApplicationController.getBooleanValueByKey("Sent Channelling Status Update Notification SMS on Channel Session Start", true)) {
                    sendChannellingStatusUpdateNotificationSms(bs);
                    System.out.println("bs = " + bs);
                }
                bs.setNextInLine(true);
                billSessionFacade.edit(bs);
                selectedSessionInstance.setNextInLineBillSession(bs);
                sessionInstanceFacade.edit(selectedSessionInstance);
                return;
            }
        }
    }

    public void reopenSessionInstance() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("No session selected");
            return;
        }
        selectedSessionInstance.setCompleted(false);
        sessionInstanceController.save(selectedSessionInstance);
        JsfUtil.addSuccessMessage("Session Re-Started");
        for (BillSession bs : billSessions) {
            if (!bs.isCompleted()) {
                bs.setNextInLine(true);
                billSessionFacade.edit(bs);
                selectedSessionInstance.setNextInLineBillSession(bs);
                sessionInstanceFacade.edit(selectedSessionInstance);
                return;
            }
        }
    }

    public void sendChannellingStatusUpdateNotificationSms(BillSession methodBillSession) {
        if (methodBillSession == null) {
            JsfUtil.addErrorMessage("Nothing to send");
            return;
        }
        if (methodBillSession.getSessionInstance() == null) {
            JsfUtil.addErrorMessage("No Session");
            return;
        }
        if (methodBillSession.getSessionInstance().getOriginatingSession() == null) {
            JsfUtil.addErrorMessage("No Originating Session");
            return;
        }
        if (methodBillSession.getBill() == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }
        if (methodBillSession.getBill().getPatient() == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }

        if (!methodBillSession.getBill().getPatient().getPerson().getSmsNumber().trim().equals("")) {
            Sms e = new Sms();
            e.setCreatedAt(new Date());
            e.setCreater(sessionController.getLoggedUser());
            e.setBill(methodBillSession.getBill());
            e.setCreatedAt(new Date());
            e.setSmsType(MessageType.ChannelStatusUpdate);
            e.setCreater(sessionController.getLoggedUser());
            e.setReceipientNumber(methodBillSession.getBill().getPatient().getPerson().getSmsNumber());
            e.setSendingMessage(smsBody(methodBillSession));
            e.setDepartment(getSessionController().getLoggedUser().getDepartment());
            e.setInstitution(getSessionController().getLoggedUser().getInstitution());
            e.setPending(false);
            getSmsFacade().create(e);

            Boolean sent = smsManager.sendSms(e);

        }

        JsfUtil.addSuccessMessage("SMS Sent");
    }

    public String smsBody(BillSession r) {
        String securityKey = sessionController.getApplicationPreference().getEncrptionKey();
        if (securityKey == null || securityKey.trim().equals("")) {
            sessionController.getApplicationPreference().setEncrptionKey(securityController.generateRandomKey(10));
            sessionController.savePreferences(sessionController.getApplicationPreference());
        }
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 2);
        String temId = securityController.encryptAlphanumeric(r.getId().toString(), securityKey);
        String url = commonController.getBaseUrl() + "faces/requests/cbss.xhtml?id=" + temId;
        String b = "Your session of "
                + r.getSessionInstance().getOriginatingSession().getName()
                + " Started. "
                + url;
        return b;
    }

    public void reloadSessionInstance() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("No session selected");
            return;
        }
        fillBillSessions();
        fillSessionActivities();
    }

    public void markSessionInstanceAsCompleted() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("No session selected");
            return;
        }
        selectedSessionInstance.setCompleted(true);
        selectedSessionInstance.setCompletedAt(new Date());
        selectedSessionInstance.setCompletedBy(sessionController.getLoggedUser());
        sessionInstanceController.save(selectedSessionInstance);
        JsfUtil.addSuccessMessage("Session Completed");
        if (sessionController.getDepartmentPreference().isSendSmsOnChannelBookingNoShow()) {
            sendSmsOnChannelMissingChannelBookings();
        }
    }

    public String navigateToAddBooking() {
        if (staff == null) {
            JsfUtil.addErrorMessage("Please select a Docter");
            return "";
        }
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("Please select a Session Instance");
            return "";
        }
        if (selectedSessionInstance.getOriginatingSession() == null) {
            JsfUtil.addErrorMessage("Please select a Session");
            return "";
        }

        fillItemAvailableToAdd();
        fillFees();
        printPreview = false;
        patient = new Patient();
        if (speciality == null) {
            speciality = staff.getSpeciality();
        }
        paymentMethod = sessionController.getDepartmentPreference().getChannellingPaymentMethod();
        return "/channel/add_booking?faces-redirect=true";
    }

    public void fillFees() {
        selectedItemFees = new ArrayList<>();
        sessionFees = new ArrayList<>();
        addedItemFees = new ArrayList<>();
        if (selectedSessionInstance == null || selectedSessionInstance.getOriginatingSession() == null) {
            return;
        }
        String sql;
        Map<String, Object> m = new HashMap<>();
        sql = "Select f from ItemFee f where f.retired=false and f.serviceSession=:ses order by f.id";
        m.put("ses", selectedSessionInstance.getOriginatingSession());
        sessionFees = itemFeeFacade.findByJpql(sql, m);

        List<Item> itemsAddedToBooking = getItemsAddedToBooking();
        if (itemsAddedToBooking != null && !itemsAddedToBooking.isEmpty()) {
            m = new HashMap<>();
            sql = "Select f from ItemFee f where f.retired=false and f.item in :items order by f.id";
            m.put("items", itemsAddedToBooking);
            addedItemFees = itemFeeFacade.findByJpql(sql, m);
        }

        if (sessionFees != null) {
            selectedItemFees.addAll(sessionFees);
        }
        if (addedItemFees != null) {
            selectedItemFees.addAll(addedItemFees);
        }

        feeTotalForSelectedBill = 0.0;
        for (ItemFee tbf : selectedItemFees) {
            if (foriegn) {
                feeTotalForSelectedBill += tbf.getFfee();
            } else {
                feeTotalForSelectedBill += tbf.getFee();
            }
        }
    }

    @PostConstruct
    public void init() {
        fromDate = new Date();
        toDate = new Date();

        Date tmpfromDate = viewScopeDataTransferController.getFromDate();
        Date tmptoDate = viewScopeDataTransferController.getToDate();
        if (tmpfromDate != null) {
            fromDate = tmpfromDate;
        }
        if (tmptoDate != null) {
            toDate = tmptoDate;
        }
        sessionInstanceFilter = viewScopeDataTransferController.getSessionInstanceFilter();

        needToFillSessionInstances = viewScopeDataTransferController.getNeedToFillSessionInstances();
        if (needToFillSessionInstances == null || needToFillSessionInstances != false) {
            listAllSesionInstances();
        }

        selectedSessionInstance = viewScopeDataTransferController.getSelectedSessionInstance();
        needToFillSessionInstanceDetails = viewScopeDataTransferController.getNeedToFillSessionInstanceDetails();
        if (needToFillSessionInstanceDetails == null || needToFillSessionInstanceDetails != false) {
            fillSessionInstanceDetails();
        }

        needToFillBillSessions = viewScopeDataTransferController.getNeedToFillBillSessions();
        if (needToFillBillSessions != null && needToFillBillSessions) {
            fillBillSessions();
        }
        selectedBillSession = viewScopeDataTransferController.getSelectedBillSession();
        needToFillBillSessionDetails = viewScopeDataTransferController.getNeedToFillBillSessionDetails();

        if (Boolean.TRUE.equals(needToFillBillSessionDetails) && selectedBillSession != null) {
            fillBillSessionDetails();
            fillFees();
        }

        if (viewScopeDataTransferController.getNeedToPrepareForNewBooking() != null && viewScopeDataTransferController.getNeedToPrepareForNewBooking()) {
            prepareForNewChannellingBill();
        }

        if (viewScopeDataTransferController.getNeedToFillMembershipDetails() != null && viewScopeDataTransferController.getNeedToFillMembershipDetails()) {
            patient = viewScopeDataTransferController.getPatient();
            paymentScheme = viewScopeDataTransferController.getPaymentScheme();
        }

    }

    public String navigateToChannelBookingFromMenuByDate() {
        Boolean opdBillingAfterShiftStart = sessionController.getApplicationPreference().isOpdBillingAftershiftStart();

        viewScopeDataTransferController.setFromDate(fromDate);
        viewScopeDataTransferController.setToDate(toDate);

        viewScopeDataTransferController.setNeedToFillBillSessions(false);
        viewScopeDataTransferController.setNeedToFillBillSessionDetails(false);
        viewScopeDataTransferController.setNeedToFillSessionInstances(true);
        viewScopeDataTransferController.setNeedToFillSessionInstanceDetails(true);
        viewScopeDataTransferController.setNeedToFillMembershipDetails(false);
        viewScopeDataTransferController.setNeedToPrepareForNewBooking(true);

        if (opdBillingAfterShiftStart) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                fromDate = new Date();
                toDate = new Date();
                listAllSesionInstances();
                prepareForNewChannellingBill();
                return "/channel/channel_booking_by_date?faces-redirect=true";
            } else {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        } else {
            fromDate = new Date();
            toDate = new Date();
            listAllSesionInstances();
            prepareForNewChannellingBill();
            return "/channel/channel_booking_by_date?faces-redirect=true";
        }

    }

    public String navigateToChannelBookingFromMembershipByDate(Patient pt, PaymentScheme ps) {

        if (pt == null) {
            JsfUtil.addErrorMessage("No Patient Selected");
            return "";
        }
        if (ps == null) {
            JsfUtil.addErrorMessage("No Membership");
            return "";
        }

        Boolean opdBillingAfterShiftStart = sessionController.getApplicationPreference().isOpdBillingAftershiftStart();

        viewScopeDataTransferController.setFromDate(fromDate);
        viewScopeDataTransferController.setToDate(toDate);
        viewScopeDataTransferController.setPatient(pt);
        viewScopeDataTransferController.setPaymentScheme(ps);

        viewScopeDataTransferController.setNeedToFillBillSessions(false);
        viewScopeDataTransferController.setNeedToFillBillSessionDetails(false);
        viewScopeDataTransferController.setNeedToFillSessionInstances(true);
        viewScopeDataTransferController.setNeedToFillSessionInstanceDetails(true);
        viewScopeDataTransferController.setNeedToFillMembershipDetails(true);
        viewScopeDataTransferController.setNeedToPrepareForNewBooking(true);

        if (opdBillingAfterShiftStart) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                return "/channel/channel_booking_by_date?faces-redirect=true";
            } else {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        } else {
            return "/channel/channel_booking_by_date?faces-redirect=true";
        }

    }

    public UserPreference getDepartmentPreference() {
        return sessionController.getDepartmentPreference();
    }

    public String navigateToChannelBookingByDate() {
        fillBillSessions();
        prepareForNewChannellingBill();
        return null;
    }

    public String navigateToChannelQueueFromMenu() {
        sessionInstances = channelBean.listTodaysSesionInstances();
        return "/channel/channel_queue?faces-redirect=true";
    }

    public String navigateToChannelDisplayFromMenu() {
        sessionInstances = channelBean.listTodaysSessionInstances(true, false, false);
        return "/channel/channel_display?faces-redirect=true";
    }

    public String navigateToChannelQueueFromConsultantRoom() {
        sessionInstances = channelBean.listTodaysSesionInstances();
        return "/channel/channel_queue?faces-redirect=true";
    }

    public void listAllSesionInstances() {
        sessionInstances = channelBean.listSessionInstances(fromDate, toDate, null, null, null);
        filterSessionInstances();
    }

    public void listOngoingSesionInstances() {
        sessionInstances = channelBean.listSessionInstances(fromDate, toDate, true, null, null);
        filterSessionInstances();
    }

    public void listCompletedSesionInstances() {
        sessionInstances = channelBean.listSessionInstances(fromDate, toDate, null, true, null);
        filterSessionInstances();
    }

    public void listPendingSesionInstances() {
        sessionInstances = channelBean.listSessionInstances(fromDate, toDate, null, null, true);
        filterSessionInstances();
    }

    public void listCancelledSesionInstances() {
        sessionInstances = channelBean.listSessionInstances(fromDate, toDate, null, null, null, true);
        filterSessionInstances();
    }

    public void prepareForNewChannellingBill() {
        selectedBillSession = null;
        getSelectedBillSession();
        printPreview = false;
    }

    public String navigateToViewSessionData() {
        return "/channel/session_data?faces-redirect=true";
    }

    public String navigateToConsultantRoom() {
        return "/channel/consultant_room?faces-redirect=true";
    }

    public void loadSessionInstance() {
        sessionInstances = channelBean.listTodaysSessionInstances(true, false, false);
    }

    public String navigateToManageBooking(BillSession bs) {
        selectedBillSession = bs;
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("Please select a Patient");
            return "";
        }

        fillSessionInstanceByDoctor();
        // Setting the properties in the viewScopeDataTransferController
        viewScopeDataTransferController.setSelectedBillSession(selectedBillSession);
        viewScopeDataTransferController.setSelectedSessionInstance(selectedSessionInstance);
        viewScopeDataTransferController.setSessionInstanceFilter(sessionInstanceFilter);
        viewScopeDataTransferController.setFromDate(fromDate);
        viewScopeDataTransferController.setToDate(toDate);

        viewScopeDataTransferController.setNeedToFillBillSessions(true);
        viewScopeDataTransferController.setNeedToFillBillSessionDetails(false);
        viewScopeDataTransferController.setNeedToFillSessionInstances(false);
        viewScopeDataTransferController.setNeedToFillSessionInstanceDetails(true);
        viewScopeDataTransferController.setNeedToFillMembershipDetails(false);
        viewScopeDataTransferController.setNeedToPrepareForNewBooking(false);
        printPreviewC = false;

        return "/channel/manage_booking_by_date?faces-redirect=true";
    }

    public String navigateToOpdBilling(BillSession bs) {
        selectedBillSession = bs;
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("Please select a Patient");
            return "";
        }
        // Setting the properties in the viewScopeDataTransferController
        viewScopeDataTransferController.setSelectedBillSession(selectedBillSession);
        viewScopeDataTransferController.setNeedToCreateOpdBillForChannellingBillSession(true);
        viewScopeDataTransferController.setNeedToFillBillSessions(false);
        viewScopeDataTransferController.setNeedToFillBillSessionDetails(false);
        viewScopeDataTransferController.setNeedToFillMembershipDetails(false);
        viewScopeDataTransferController.setNeedToPrepareForNewBooking(false);

        return opdBillController.navigateToNewOpdBillFromChannelling();
    }

    public void fillBillSessionDetails() {
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("Selected Bill Session is Null");
            return;
        }
        if (selectedBillSession.getBill() == null) {
            JsfUtil.addErrorMessage("Selected Bill Session is Null");
            return;
        }
        if (selectedBillSession.getBill().getBillItems() == null) {
            selectedBillSession.getBill().setBillItems(billController.billItemsOfBill(selectedBillSession.getBill()));
        }
        if (selectedBillSession.getBill().getBillItems() == null) {
            JsfUtil.addErrorMessage("Bill Items Null");
            return;
        }
        if (selectedBillSession.getBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Items");
            return;
        }

        if (selectedBillSession.getBill().getBillFees() == null) {
            selectedBillSession.getBill().setBillFees(billController.billFeesOfBill(selectedBillSession.getBill()));
        }

        if (selectedBillSession.getBill().getBillFees() == null) {
            JsfUtil.addErrorMessage("Bill Fees Null");
            return;
        }

        if (selectedBillSession.getBill().getBillFees().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Fees");
            return;
        }
        setPrintPreview(false);
    }

    public String navigateBackToBookingsFromSessionInstance() {
        viewScopeDataTransferController.setSelectedSessionInstance(selectedSessionInstance);
        viewScopeDataTransferController.setSelectedBillSession(selectedBillSession);
        viewScopeDataTransferController.setSessionInstanceFilter(sessionInstanceFilter);
        viewScopeDataTransferController.setFromDate(fromDate);
        viewScopeDataTransferController.setToDate(toDate);

        viewScopeDataTransferController.setNeedToFillBillSessions(false);
        viewScopeDataTransferController.setNeedToFillBillSessionDetails(false);
        viewScopeDataTransferController.setNeedToFillSessionInstances(true);
        viewScopeDataTransferController.setNeedToFillSessionInstanceDetails(true);
        viewScopeDataTransferController.setNeedToFillMembershipDetails(false);
        viewScopeDataTransferController.setNeedToPrepareForNewBooking(true);

        return "/channel/channel_booking_by_date?faces-redirect=true";
    }

    public String navigateBackToBookingsFromProfPay() {
        viewScopeDataTransferController.setSelectedSessionInstance(selectedSessionInstance);
        viewScopeDataTransferController.setSelectedBillSession(selectedBillSession);
        viewScopeDataTransferController.setSessionInstanceFilter(sessionInstanceFilter);
        viewScopeDataTransferController.setFromDate(fromDate);
        viewScopeDataTransferController.setToDate(toDate);
        channelStaffPaymentBillController.makenull();

        viewScopeDataTransferController.setNeedToFillBillSessions(false);
        viewScopeDataTransferController.setNeedToFillBillSessionDetails(false);
        viewScopeDataTransferController.setNeedToFillSessionInstances(true);
        viewScopeDataTransferController.setNeedToFillSessionInstanceDetails(true);
        viewScopeDataTransferController.setNeedToFillMembershipDetails(false);
        viewScopeDataTransferController.setNeedToPrepareForNewBooking(true);

        return "/channel/channel_booking_by_date?faces-redirect=true";
    }

    public String navigateBackToBookingsFromBillSession() {
        viewScopeDataTransferController.setSelectedBillSession(selectedBillSession);
        viewScopeDataTransferController.setSelectedBillSession(selectedBillSession);
        viewScopeDataTransferController.setSelectedSessionInstance(selectedSessionInstance);
        viewScopeDataTransferController.setSessionInstanceFilter(sessionInstanceFilter);
        viewScopeDataTransferController.setFromDate(fromDate);
        viewScopeDataTransferController.setToDate(toDate);

        viewScopeDataTransferController.setNeedToFillBillSessions(false);
        viewScopeDataTransferController.setNeedToFillBillSessionDetails(false);
        viewScopeDataTransferController.setNeedToFillSessionInstances(true);
        viewScopeDataTransferController.setNeedToFillSessionInstanceDetails(true);
        viewScopeDataTransferController.setNeedToFillMembershipDetails(false);
        viewScopeDataTransferController.setNeedToPrepareForNewBooking(true);

        return "/channel/channel_booking_by_date?faces-redirect=true";
    }

    public String navigateBackToBookingsLoagingBillSessions() {
        fillBillSessions();
        return "/channel/channel_booking?faces-redirect=true";
    }

    public String navigateToManageSessionQueueAtConsultantRoom() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("Not Selected");
            return null;
        }
        fillBillSessions();
        fillSessionActivities();
        return "/channel/channel_queue_session?faces-redirect=true";
    }

    public String navigateBackToManageSessionQueueAtConsultantRoom() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("Not Selected");
            return null;
        }
        return "/channel/channel_queue_session?faces-redirect=true";
    }

    public String navigateToDisplatSessionQueueAtConsultantRoom() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("Not Selected");
            return null;
        }
        fillBillSessions();
        return "/channel/channel_queue_display?faces-redirect=true";
    }

    public String navigateToNurseView() {
        if (preSet()) {
            getChannelReportController().fillNurseView();
            return "/channel/channel_views/channel_nurse_view?faces-redirect=true";
        } else {
            return "";
        }
    }

    public String navigateToDoctorView() {
        if (preSet()) {
            getChannelReportController().fillDoctorView();
            return "/channel/channel_views/channel_doctor_view?faces-redirect=true";
        } else {
            return "";
        }
    }

    public String navigateToSessionView() {
        if (preSet()) {
            getChannelReportController().fillNurseView();
            return "/channel/channel_session_view?faces-redirect=true";
        } else {
            return "";
        }
    }

    public String navigateToPhoneView() {
        if (preSet()) {
            getChannelReportController().fillNurseView();
            return "/channel/channel_phone_view?faces-redirect=true";
        } else {
            return "";
        }
    }

    public String navigateToUserView() {
        if (preSet()) {
            getChannelReportController().fillDoctorView();
            return "/channel/channel_views/channel_user_view?faces-redirect=true";
        } else {
            return "";
        }
    }

    public String navigateToAllDoctorView() {
        return "/channel/channel_views/channel_patient_view_today?faces-redirect=true";
    }

    public String navigateToAllPatientView() {
        return "/channel/channel_user_view?faces-redirect=true";
    }

    public void channelBookingCancel() {
        if (selectedBillSession.getBill().getBillType() == BillType.ChannelAgent) {
            cancelAgentPaidBill();
            return;
        }
        if (selectedBillSession.getBill().getBillType().getParent() == BillType.ChannelCashFlow && selectedBillSession.getBill().getBillType() != BillType.ChannelAgent) {
            cancelCashFlowBill();
            return;
        }
        if ((selectedBillSession.getBill().getBillType() == BillType.ChannelOnCall || selectedBillSession.getBill().getBillType() == BillType.ChannelStaff) && selectedBillSession.getBill().getPaidBill() == null) {
            cancelBookingBill();
            return;
        }
        if (selectedBillSession.getBill().getBillType().getParent() == BillType.ChannelCreditFlow && selectedBillSession.getBill().getBillType() != BillType.ChannelAgent) {
            if (selectedBillSession.getBill().getPaidAmount() == 0) {
                JsfUtil.addErrorMessage("Can't Cancel. No Payments");
            } else {
                cancelCreditPaidBill();
                return;
            }
        }
    }

    public void cancelAgentPaidBill() {
        if (getBillSession() == null) {
            JsfUtil.addErrorMessage("No BillSession");
            return;
        }

        if (getBillSession().getBill() == null) {
            JsfUtil.addErrorMessage("No Bill To Cancel");
            return;
        }

        if (getBillSession().getPaidBillSession() == null) {
            if (getBillSession().getBillItem().getBill().getBalance() == 0.0) {
                getBillSession().setPaidBillSession(getBillSession());
            } else {
                JsfUtil.addErrorMessage("No Paid. Can not cancel.");
                return;
            }
        }

        if (getBillSession().getPaidBillSession().getBill() == null) {
            JsfUtil.addErrorMessage("No Paid Paid Bill Session");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            return;
        }

        if (getCancelPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Payment Method");
            return;
        }
        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }

        //cancel(getBillSession().getPaidBillSession().getBill(), getBillSession().getPaidBillSession().getBillItem(), getBillSession().getPaidBillSession());
        cancel(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession());
        sendSmsOnChannelCancellationBookings();
        cancelPaymentMethod = null;
        comment = null;
        printPreviewC = true;
    }

    public void cancelCashFlowBill() {
        if (errorCheckCancelling()) {
            return;
        }

        cancel(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession());
        sendSmsOnChannelCancellationBookings();
        comment = null;
        printPreviewC = true;
    }

    public void cancelBookingBill() {
        if (errorCheckCancelling()) {
            return;
        }

        CancelledBill cb = createCancelBill1(getBillSession().getBill());
        BillItem cItem = cancelBillItems(getBillSession().getBillItem(), cb);
        BillSession cbs = cancelBillSession(getBillSession(), cb, cItem);
        getBillSession().getBill().setCancelled(true);
        getBillSession().getBill().setCancelledBill(cb);
        getBillFacade().edit(getBillSession().getBill());
        getBillSession().setReferenceBillSession(cbs);
        billSessionFacade.edit(selectedBillSession);
        sendSmsOnChannelCancellationBookings();
        comment = null;
        printPreviewC = true;
    }

    private BillItem cancelBillItems(BillItem bi, CancelledBill can) {

        BillItem b = new BillItem();
        b.setBill(can);
        b.copy(bi);
        b.invertValue(bi);
        b.setCreatedAt(new Date());
        b.setCreater(getSessionController().getLoggedUser());

        getBillItemFacade().create(b);
        String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
        List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
        cancelBillFee(can, b, tmp);

        return b;
    }

    private BillSession cancelBillSession(BillSession selectedBillSession, CancelledBill can, BillItem canBillItem) {
        BillSession bs = new BillSession();
        bs.copy(selectedBillSession);
        bs.setBill(can);
        bs.setBillItem(canBillItem);
        bs.setCreatedAt(new Date());
        bs.setCreater(getSessionController().getLoggedUser());
        getBillSessionFacade().create(bs);

        can.setSingleBillSession(bs);
        getBillFacade().edit(can);

        return bs;
    }

    private boolean errorCheckCancelling() {
        if (getBillSession() == null) {
            return true;
        }

        if (getBillSession().getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            return true;
        }

        if (getBillSession().getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded");
            return true;
        }

        if (checkPaid()) {
            JsfUtil.addErrorMessage("Doctor Payment has paid");
            return true;
        }
        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }
        return false;
    }

    private boolean checkPaid() {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.bill.id=" + getBillSession().getBill().getId();
        List<BillFee> tempFe = getBillFeeFacade().findByJpql(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    public void cancelCreditPaidBill() {
        if (getBillSession() == null) {
            JsfUtil.addErrorMessage("No BillSession");
            return;
        }
        if (getBillSession().getBill() == null) {
            JsfUtil.addErrorMessage("No Bill To Cancel");
            return;
        }
        if (getBillSession().getPaidBillSession() == null) {
            JsfUtil.addErrorMessage("No Paid Paid Bill Session");
            return;
        }
        if (getBillSession().getPaidBillSession().getBill() == null) {
            JsfUtil.addErrorMessage("No Paid Paid Bill Session");
            return;
        }
        if (getBillSession().getPaidBillSession().getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded");
            return;
        }
        if (getBillSession().getPaidBillSession().getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            return;
        }
        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }
        cancel1(getBillSession().getPaidBillSession().getBill(), getBillSession().getPaidBillSession().getBillItem(), getBillSession().getPaidBillSession());
        cancel1(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession());
        sendSmsOnChannelCancellationBookings();
        comment = null;
        printPreviewC = true;
    }

    public void fetchRecentChannelBooks(Institution ins) {
        String sql;
        HashMap m = new HashMap();

        sql = "select a from AgentReferenceBook a "
                + " where a.retired=false "
                + " and a.institution=:ins"
                + " and a.deactivate=false "
                + " order by a.id desc ";

        m.put("ins", ins);

        List<AgentReferenceBook> agentReferenceBooks = agentReferenceBookFacade.findByJpql(sql, m, 5);
        if (agentReferenceBooks.size() > 0) {
            ins.setAgentReferenceBooks(agentReferenceBooks);
        }
    }

    private boolean errorCheckAgentValidate() {
        if (getSelectedSessionInstance() == null) {
            errorText = "Please Select Specility and Doctor.";
            JsfUtil.addErrorMessage("Please Select Specility and Doctor.");
            return true;
        }

        if (getSelectedSessionInstance().isDeactivated()) {
            errorText = "******** Doctor Leave day Can't Channel ********";
            JsfUtil.addErrorMessage("Doctor Leave day Can't Channel.");
            return true;
        }

        if (getSelectedSessionInstance().getOriginatingSession() == null) {
            errorText = "Please Select Session.";
            JsfUtil.addErrorMessage("Please Select Session");
            return true;
        }

        if (paymentMethod == PaymentMethod.Agent) {
            if (institution == null) {
                errorText = "Please select Agency";
                JsfUtil.addErrorMessage("Please select Agency");
                return true;
            }

            //To Do
//            if (institution.getBallance() - amount < 0 - institution.getAllowedCredit()) {
//                errorText = "Agency Balance is Not Enough";
//                JsfUtil.addErrorMessage("Agency Balance is Not Enough");
//                return true;
//            }
        }

        ////System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        return false;
    }

    public void validateAgentBalance() {
        fetchRecentChannelBooks(institution);
        activeCreditLimitPannel = false;

        if (errorCheckAgentValidate()) {
            activeCreditLimitPannel = true;
            return;
        }

        if (isForiegn()) {
            if (getSelectedSessionInstance().getOriginatingSession().getTotalFfee() > (institution.getBallance() + institution.getAllowedCredit())) {
                JsfUtil.addErrorMessage("Please Increase Credit Limit or Balance");
                activeCreditLimitPannel = true;
                return;
            }
        }

        if (!isForiegn()) {
            if (getSelectedSessionInstance().getOriginatingSession().getTotalFee() > (institution.getBallance() + institution.getAllowedCredit())) {
                JsfUtil.addErrorMessage("Please Increase Credit Limit or Balance");
                activeCreditLimitPannel = true;
                return;
            }
        }
        setAgentRefNo("");
    }

    public void cancel(Bill bill, BillItem billItem, BillSession selectedBillSession) {
        if (errorCheckCancelling()) {
            return;
        }

        if ((bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) && bill.getPaidBill() == null) {
            bill.setPaidBill(bill);
            billFacade.edit(bill);
        } else if (bill.getPaidBill() == null) {
            bill.setPaidBill(bill);
        }

        //dr. buddhika said
        if (bill.getPaidBill() == null) {
            return;
        }

        if (bill.getPaidBill().equals(bill)) {
            CancelledBill cb = createCancelCashBill(bill);
            BillItem cItem = cancelBillItems(billItem, cb);
            BillSession cbs = cancelBillSession(selectedBillSession, cb, cItem);
            bill.setCancelled(true);
            bill.setCancelledBill(cb);
            getBillFacade().edit(bill);

            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                if (cancelPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, cItem, cbs, cbs.getBillItem().getAgentRefNo());
                }
            }

            //Update BillSession        
            selectedBillSession.setReferenceBillSession(cbs);
            billSessionFacade.edit(selectedBillSession);

        } else {
            CancelledBill cb = createCancelBill(bill);
            BillItem cItem = cancelBillItems(billItem, cb);
            BillSession cbs = cancelBillSession(selectedBillSession, cb, cItem);
            bill.setCancelled(true);
            bill.setCancelledBill(cb);
            getBillFacade().edit(bill);
            selectedBillSession.setReferenceBillSession(cbs);
            billSessionFacade.edit(selectedBillSession);

            CancelledBill cpb = createCancelBill(bill.getPaidBill());
            BillItem cpItem = cancelBillItems(bill.getPaidBill().getSingleBillItem(), cb);
            BillSession cpbs = cancelBillSession(selectedBillSession.getPaidBillSession(), cpb, cpItem);
            bill.getPaidBill().setCancelled(true);
            bill.getPaidBill().setCancelledBill(cpb);
            getBillFacade().edit(bill.getPaidBill());
            selectedBillSession.getPaidBillSession().setReferenceBillSession(cpbs);
            billSessionFacade.edit(selectedBillSession.getPaidBillSession());
            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                if (cancelPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, cItem, cbs, cbs.getBillItem().getAgentRefNo());
                }
            }

        }

        JsfUtil.addSuccessMessage("Cancelled");

    }

    private CancelledBill createCancelBill(Bill bill) {
        CancelledBill cb = new CancelledBill();

        cb.copy(bill);
        cb.invertValue(bill);
        cb.setBilledBill(bill);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

//        cb.setInsId(billNumberBean.institutionChannelBillNumberGenerator(sessionController.getInstitution(), cb));
        String insId = generateBillNumberInsId(cb);

        if (insId.equals("")) {
            return null;
        }
        cb.setInsId(insId);

        String deptId = generateBillNumberDeptId(cb);

        if (deptId.equals("")) {
            return null;
        }
        cb.setDeptId(deptId);
        cb.setBillTypeAtomic(BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT_FOR_CREDIT_SETTLED_BOOKINGS);
        getBillFacade().create(cb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            cb.setPaymentMethod(cancelPaymentMethod);
//            if (cancelPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, selectedBillSession.getBillItem(), selectedBillSession, selectedBillSession.getBill().getReferralNumber());
//            }
        } else {
            cb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(cb);
        return cb;
    }

    private CancelledBill createCancelCashBill(Bill bill) {
        CancelledBill cb = new CancelledBill();

        cb.copy(bill);
        cb.invertValue(bill);
        cb.setBilledBill(bill);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

//        cb.setInsId(billNumberBean.institutionChannelBillNumberGenerator(sessionController.getInstitution(), cb));
        String insId = generateBillNumberInsId(cb);

        if (insId.equals("")) {
            return null;
        }
        cb.setInsId(insId);

        String deptId = generateBillNumberDeptId(cb);

        if (deptId.equals("")) {
            return null;
        }
        cb.setDeptId(deptId);
        cb.setBillTypeAtomic(BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT);
        getBillFacade().create(cb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            cb.setPaymentMethod(cancelPaymentMethod);
//            if (cancelPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, selectedBillSession.getBillItem(), selectedBillSession, selectedBillSession.getBill().getReferralNumber());
//            }
        } else {
            cb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(cb);
        return cb;
    }

    public void cancel1(Bill bill, BillItem billItem, BillSession selectedBillSession) {
        if (errorCheckCancelling()) {
            return;
        }

        if ((bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) && bill.getPaidBill() == null) {
            bill.setPaidBill(bill);
            billFacade.edit(bill);
        } else if (bill.getPaidBill() == null) {
            bill.setPaidBill(bill);
        }

        //dr. buddhika said
        if (bill.getPaidBill() == null) {
            return;
        }

        if (bill.getPaidBill().equals(bill)) {
            CancelledBill cb = createCancelCashBill1(bill);
            BillItem cItem = cancelBillItems(billItem, cb);
            BillSession cbs = cancelBillSession(selectedBillSession, cb, cItem);
            bill.setCancelled(true);
            bill.setCancelledBill(cb);
            getBillFacade().edit(bill);

            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                if (cancelPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, cItem, cbs, cbs.getBillItem().getAgentRefNo());
                }
            }

            //Update BillSession        
            selectedBillSession.setReferenceBillSession(cbs);
            billSessionFacade.edit(selectedBillSession);

        } else {
            CancelledBill cb = createCancelBill1(bill);
            BillItem cItem = cancelBillItems(billItem, cb);
            BillSession cbs = cancelBillSession(selectedBillSession, cb, cItem);
            bill.setCancelled(true);
            bill.setCancelledBill(cb);
            getBillFacade().edit(bill);
            selectedBillSession.setReferenceBillSession(cbs);
            billSessionFacade.edit(selectedBillSession);

            CancelledBill cpb = createCancelBill(bill.getPaidBill());
            BillItem cpItem = cancelBillItems(bill.getPaidBill().getSingleBillItem(), cb);
            BillSession cpbs = cancelBillSession(selectedBillSession.getPaidBillSession(), cpb, cpItem);
            bill.getPaidBill().setCancelled(true);
            bill.getPaidBill().setCancelledBill(cpb);
            getBillFacade().edit(bill.getPaidBill());
            selectedBillSession.getPaidBillSession().setReferenceBillSession(cpbs);
            billSessionFacade.edit(selectedBillSession.getPaidBillSession());
            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                if (cancelPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, cItem, cbs, cbs.getBillItem().getAgentRefNo());
                }
            }

        }

        JsfUtil.addSuccessMessage("Cancelled");

    }

    private CancelledBill createCancelBill1(Bill bill) {
        CancelledBill cb = new CancelledBill();

        cb.copy(bill);
        cb.invertValue(bill);
        cb.setBilledBill(bill);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

//        cb.setInsId(billNumberBean.institutionChannelBillNumberGenerator(sessionController.getInstitution(), cb));
        String insId = generateBillNumberInsId(cb);

        if (insId.equals("")) {
            return null;
        }
        cb.setInsId(insId);

        String deptId = generateBillNumberDeptId(cb);

        if (deptId.equals("")) {
            return null;
        }
        cb.setDeptId(deptId);
        getBillFacade().create(cb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            cb.setPaymentMethod(cancelPaymentMethod);
//            if (cancelPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, selectedBillSession.getBillItem(), selectedBillSession, selectedBillSession.getBill().getReferralNumber());
//            }
        } else {
            cb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(cb);
        return cb;
    }

    private CancelledBill createCancelCashBill1(Bill bill) {
        CancelledBill cb = new CancelledBill();

        cb.copy(bill);
        cb.invertValue(bill);
        cb.setBilledBill(bill);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

//        cb.setInsId(billNumberBean.institutionChannelBillNumberGenerator(sessionController.getInstitution(), cb));
        String insId = generateBillNumberInsId(cb);

        if (insId.equals("")) {
            return null;
        }
        cb.setInsId(insId);

        String deptId = generateBillNumberDeptId(cb);

        if (deptId.equals("")) {
            return null;
        }
        cb.setDeptId(deptId);
        getBillFacade().create(cb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            cb.setPaymentMethod(cancelPaymentMethod);
//            if (cancelPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, selectedBillSession.getBillItem(), selectedBillSession, selectedBillSession.getBill().getReferralNumber());
//            }
        } else {
            cb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(cb);
        return cb;
    }

    private void cancelBillFee(Bill can, BillItem bt, List<BillFee> tmp) {
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.copy(nB);
            bf.invertValue(nB);
            bf.setBill(can);
            bf.setBillItem(bt);

            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());

            getBillFeeFacade().create(bf);
        }
    }

    public List<BillSession> getGetSelectedBillSession() {
        return getSelectedBillSession;
    }

    public void setGetSelectedBillSession(List<BillSession> getSelectedBillSession) {
        this.getSelectedBillSession = getSelectedBillSession;
    }

    public void addItemToBooking() {
        if (itemToAddToBooking == null) {
            JsfUtil.addErrorMessage("Item to add to booking");
            return;
        }
        List<Item> items = getItemsAddedToBooking();
        for (Item item : items) {
            if (item.equals(itemToAddToBooking)) {
                JsfUtil.addErrorMessage("Item is Already Added");
                return;
            }
        }
        getItemsAddedToBooking().add(itemToAddToBooking);
        itemToAddToBooking = null;
        fillFees();
    }

    public void removeAddedAditionalItem() {
        if (selectedBillItem == null) {
            JsfUtil.addErrorMessage("Pleace Select A Bill Item !");
        }
        List<ItemFee> itemFeesofTheRemovingItem = billBeanController.fillFees(selectedBillItem.getItem());
        List<BillFee> billFeesToRemove = selectedBillItem.getBillFees();
        if (billFeesToRemove == null || billFeesToRemove.isEmpty()) {
            billFeesToRemove = billBeanController.fillBillItemFees(selectedBillItem);
        }
        if (billFeesToRemove != null) {
            selectedBillSession.getBillItem().getBill().getBillFees().removeAll(billFeesToRemove);
        }
        selectedBillSession.getBillItem().getBill().getBillItems().remove(selectedBillItem);
        calculateBillTotalsFromBillFees(selectedBillSession.getBillItem().getBill());
    }

    public void addItemToBookingAtSettling() {
        if (itemToAddToBooking == null) {
            JsfUtil.addErrorMessage("Item to add to booking");
            return;
        }
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("No Bill Session");
            return;
        }
        if (selectedBillSession.getBillItem() == null) {
            JsfUtil.addErrorMessage("No Bill Item");
            return;
        }
        if (selectedBillSession.getBillItem().getBill() == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }
        if (selectedBillSession.getBillItem().getBill().getBillItems() == null) {
            JsfUtil.addErrorMessage("No Bill Items");
            return;
        }
        BillItem newBillItem = new BillItem();
        newBillItem.setBill(selectedBillSession.getBillItem().getBill());
        newBillItem.setBillSession(selectedBillSession);
        newBillItem.setCreatedAt(new Date());
        newBillItem.setCreater(sessionController.getLoggedUser());
        newBillItem.setInwardChargeType(itemToAddToBooking.getInwardChargeType());
        newBillItem.setItem(itemToAddToBooking);
        newBillItem.setItemId(itemToAddToBooking.getId() + "");

        if (newBillItem.getQty() == null || newBillItem.getQty() == 0.0) {
            newBillItem.setQty(1.0);
        }
        if (foriegn) {
            newBillItem.setGrossValue(itemToAddToBooking.getTotalForForeigner() * newBillItem.getQty());
        } else {
            newBillItem.setGrossValue(itemToAddToBooking.getTotal() * newBillItem.getQty());
        }

        newBillItem.setDiscountRate(0);
        newBillItem.setDiscount(0);

        newBillItem.setNetRate(0);
        newBillItem.setNetValue(newBillItem.getGrossValue() - newBillItem.getDiscount());

        if (newBillItem.getId() == null) {
            billItemFacade.create(newBillItem);
        } else {
            billItemFacade.edit(newBillItem);
        }

        List<ItemFee> itemFeesofTheAddingItem = billBeanController.fillFees(itemToAddToBooking);
        List<BillFee> billFeesToAdd = null;
        if (itemFeesofTheAddingItem != null) {
            billFeesToAdd = billBeanController.createNewBillFeesAndReturnThem(newBillItem, itemFeesofTheAddingItem, sessionController.getLoggedUser(), null, null, foriegn);
        }

        selectedBillSession.getBillItem().getBill().getBillItems().add(newBillItem);
        if (billFeesToAdd != null) {
            selectedBillSession.getBillItem().getBill().getBillFees().addAll(billFeesToAdd);
            newBillItem.setBillFees(billFeesToAdd);
        }

        calculateBillTotalsFromBillFees(selectedBillSession.getBillItem().getBill());

        itemToAddToBooking = null;
//        fillFees();
    }

    public boolean errorCheckForSerial() {
        if (selectedBillSession == null || billSessions == null) {
            // Handle the case when selectedBillSession or billSessions is null
            return false;
        }

        boolean alreadyExists = false;
        for (BillSession bs : billSessions) {
            if (bs == null) {
                // Skip null elements in billSessions
                continue;
            }

            if (selectedBillSession.equals(bs)) {
                // Skip comparison with selectedBillSession itself
                continue;
            }

            if (bs.getSerialNo() == selectedBillSession.getSerialNo()) {
                alreadyExists = true;
                break; // No need to continue checking if alreadyExists is true
            }

            for (BillItem bi : bs.getBill().getBillItems()) {
                if (bi != null && bi.getBillSession() != null && serealNo == bi.getBillSession().getSerialNo()) {
                    alreadyExists = true;
                    JsfUtil.addErrorMessage("This Number Is Already Exist");
                    break; // No need to continue checking if alreadyExists is true
                }
            }
        }

        return alreadyExists;
    }

    public String startNewChannelBookingForSelectingSpeciality() {
        resetToStartFromSelectingSpeciality();
        printPreview = false;
        return navigateBackToBookingsFromSessionInstance();
    }

    public String startNewChannelBookingFormSelectingConsultant() {
        resetToStartFromSelectingConsultant();
        generateSessions();
        printPreview = false;
        return navigateBackToBookingsFromSessionInstance();
    }

    public String startNewChannelBookingForSelectingSession() {
        resetToStartFromSameSessionInstance();
        fillBillSessions();
        printPreview = false;
        return navigateBackToBookingsFromSessionInstance();
    }

    public String startNewChannelBookingForSameSession() {
        resetToStartFromSameSessionInstance();
        printPreview = false;
        return navigateToAddBooking();
    }

    public String navigateToManageBookingForSameSession() {
        printPreview = false;
        if (printingBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        selectedBillSession = printingBill.getSingleBillSession();
        return navigateToManageBooking(selectedBillSession);
    }

    public boolean billSessionErrorPresent() {
        boolean flag = false;
        if (billSessions != null && getSelectedBillSession() != null) {
            for (BillSession bs : billSessions) {
                if (!bs.equals(selectedBillSession)) {
                    List<BillItem> billItems = bs.getBill().getBillItems();
                    if (billItems != null) {
                        for (BillItem bi : billItems) {
                            BillSession selectedBillSession = bi.getBillSession();
                            if (selectedBillSession != null && serealNo == selectedBillSession.getSerialNo()) {
                                JsfUtil.addErrorMessage("This Number Is Already Exist");
                                flag = true;
                                break; // No need to continue checking if flag is already true
                            }
                        }
                    }
                }
            }
        }
        return flag;
    }

    public double getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(double absentCount) {
        this.absentCount = absentCount;
    }

//    public void errorCheckChannelNumber() {
//
//        for (BillSession bs : billSessions) {
//            //System.out.println("billSessions" + bs.getName());
//            for (BillItem bi : getSelectedBillSession().getBill().getBillItems()) {
//                //System.out.println("billitem" + bi.getId());
//                if (bs.getSerialNo() == bi.getBillSession().getSerialNo()) {
//                    JsfUtil.addErrorMessage("Number you entered already exist");
//                    setSelectedBillSession(bs);
//
//                }
//
//            }
//        }
//
//    }
    public void updatePatient() {
        getPersonFacade().edit(getSelectedBillSession().getBill().getPatient().getPerson());
        JsfUtil.addSuccessMessage("Patient Updated");
    }

    public void updateSelectedBillSessionPatient() {
        if (selectedBillSession == null) {
            return;
        }
        if (selectedBillSession.getId() == null) {
            billSessionFacade.create(selectedBillSession);
        } else {
            billSessionFacade.edit(selectedBillSession);
        }
    }

    public boolean patientErrorPresent(Patient p) {
        if (p == null) {
            JsfUtil.addErrorMessage("No Current. Error. NOT SAVED");
            return true;
        }
        if (p.getPerson() == null) {
            p = patientFacade.find(p.getId());
        }
        if (p.getPerson() == null) {
            JsfUtil.addErrorMessage("No Person. Not Saved");
            return true;
        }
        if (p.getPerson().getName() == null) {
            JsfUtil.addErrorMessage("Please enter a name");
            return true;
        }

        if (p.getPerson().getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a name");
            return true;
        }

        return false;
    }

    private boolean paymentMethodErrorPresent() {
        if (paymentMethod == null) {
            return true;
        }

        if (!(paymentMethod == PaymentMethod.Cash || paymentMethod == PaymentMethod.Card || paymentMethod == PaymentMethod.MultiplePaymentMethods)) {
            if (selectedSessionInstance.getOriginatingSession().isPaidAppointmentsOnly()) {
                JsfUtil.addErrorMessage("This session is only available for paid appointments.");
                return true;
            }
        }

        if (paymentMethod == PaymentMethod.Agent) {
            if (institution == null) {
                return true;
            }
        }

        if (paymentMethod == PaymentMethod.Staff) {

            if (toStaff == null) {
                return true;
            }
            setNetPlusVat(feeTotalForSelectedBill);
            if (toStaff.getCreditLimitQualified() >= toStaff.getCurrentCreditValue() + netPlusVat) {
                staffBean.updateStaffCredit(toStaff, netPlusVat);
            } else {
                JsfUtil.addErrorMessage("Staff credit limit exceeded. The current credit value cannot exceed the credit limit qualified by the staff member.");
                return true;
            }

        }

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            if (getPaymentMethodData() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (paymentSchemeController.checkPaymentMethodError(cd.getPaymentMethod(), cd.getPaymentMethodData())) {
                    return true;
                }
                if (cd.getPaymentMethod().equals(PaymentMethod.Staff)) {
                    if (cd.getPaymentMethodData().getStaffCredit().getTotalValue() == 0.0 || cd.getPaymentMethodData().getStaffCredit().getToStaff() == null) {
                        JsfUtil.addErrorMessage("Please fill the Paying Amount and Staff Name");
                        return true;
                    }
                    if (cd.getPaymentMethodData().getStaffCredit().getToStaff().getCurrentCreditValue() + cd.getPaymentMethodData().getStaffCredit().getTotalValue() > cd.getPaymentMethodData().getStaffCredit().getToStaff().getCreditLimitQualified()) {
                        JsfUtil.addErrorMessage("No enough Credit.");
                        return true;
                    }
                }
                //TODO - filter only relavant value
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();
            }
            double differenceOfBillTotalAndPaymentValue = feeTotalForSelectedBill - multiplePaymentMethodTotalValue;
            differenceOfBillTotalAndPaymentValue = Math.abs(differenceOfBillTotalAndPaymentValue);
            if (differenceOfBillTotalAndPaymentValue > 1.0) {
                JsfUtil.addErrorMessage("Mismatch in differences of multiple payment method total and bill total");
                return true;
            }
            if (cashPaid == 0.0) {
                setCashPaid(multiplePaymentMethodTotalValue);
            }

        }

        if (configOptionApplicationController.getBooleanValueByKey("Channel Credit Booking Settle Requires Additional Information")) {
            if (paymentMethod == PaymentMethod.Card) {
                if (paymentMethodData.getCreditCard().getInstitution() == null) {
                    return true;
                }
                if (paymentMethodData.getCreditCard().getNo() == null) {
                    return true;
                }
            }
            if (paymentMethod == PaymentMethod.Cheque) {
                if (paymentMethodData.getCheque().getNo() == null) {
                    return true;
                }
                if (paymentMethodData.getCheque().getInstitution() == null) {
                    return true;
                }
                if (paymentMethodData.getCheque().getDate() == null) {
                    return true;
                }
            }
            if (paymentMethod == PaymentMethod.Slip) {
                if (paymentMethodData.getSlip().getInstitution() == null) {
                    return true;
                }
                if (paymentMethodData.getSlip().getDate() == null) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addNormalChannelBooking() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("Please select a Session");
            return;
        }
        addChannelBooking(false);
        fillBillSessions();
    }

    public void addReservedChannelBooking() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("Please select a Session Instance");
            return;
        }
        boolean reservedBooking = true;
        addChannelBooking(reservedBooking);
        fillBillSessions();
    }

    public void addChannelBooking(boolean reservedBooking) {
        errorText = "";
        if (patient == null) {
            JsfUtil.addErrorMessage("Please select a patient");
            return;
        }
        if (patientErrorPresent(patient)) {
            JsfUtil.addErrorMessage("Please Enter Patient Details.");
            settleSucessFully = false;
            return;
        }
        if (paymentMethodErrorPresent()) {
            JsfUtil.addErrorMessage("Please Enter Payment Details");
            settleSucessFully = false;
            return;
        }
        if (configOptionApplicationController.getBooleanValueByKey("Channelling Patients Cannot Be Added After the Channel Has Been Completed")) {
            if (selectedSessionInstance.isCompleted()) {
                JsfUtil.addErrorMessage("This Session Has Been Completed");
                settleSucessFully = false;
                return;
            }
        }
        if (configOptionApplicationController.getBooleanValueByKey("Channel Items Sessions Need to Have a Referance Doctor")) {
            if (!(itemsAddedToBooking == null || itemsAddedToBooking.isEmpty())) {
                if (referredBy == null) {
                    JsfUtil.addErrorMessage("Referring Doctor is required");
                    settleSucessFully = false;
                    return;
                }
            }
        }

        if (selectedSessionInstance.getMaxNo() != 0) {
            if (selectedSessionInstance.getBookedPatientCount() != null) {
                int maxNo = selectedSessionInstance.getMaxNo();
                long bookedPatientCount = selectedSessionInstance.getBookedPatientCount();
                long totalPatientCount;
                System.out.println("selectedSessionInstance.getCancelPatientCount() = " + selectedSessionInstance.getCancelPatientCount());
                if (selectedSessionInstance.getCancelPatientCount() != null) {
                    long canceledPatientCount = selectedSessionInstance.getCancelPatientCount();
                    totalPatientCount = bookedPatientCount - canceledPatientCount;
                    System.out.println("totalPatientCount = " + totalPatientCount);
                    System.out.println("bookedPatientCount = " + bookedPatientCount);
                    System.out.println("canceledPatientCount = " + canceledPatientCount);
                } else {
                    totalPatientCount = bookedPatientCount;
                }
                if (maxNo <= totalPatientCount) {
                    JsfUtil.addErrorMessage("Error: The maximum number of bookings (" + maxNo + ") has been Reached.");
                    return;

                }
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Allow bill settlement without patient area")) {
            if (patient.getPerson().getArea() == null || patient.getPerson().getArea().getName().isEmpty()) {
                JsfUtil.addErrorMessage("Pleace Select Patient Area");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Allow Tenderd amount for channel booking")) {
            if (paymentMethod == PaymentMethod.Cash) {
                if (strTenderedValue == "" || strTenderedValue.isEmpty()) {
                    JsfUtil.addErrorMessage("Please Enter Tenderd Amount");
                    return;
                }
            }
        }

        if (selectedSessionInstance.isCancelled()) {
            JsfUtil.addErrorMessage("Cannot add patient to a canceled session. Please select an active session.");
            return;
        }

        saveSelected(patient);
        printingBill = saveBilledBill(reservedBooking);
        if (printingBill.getBillTypeAtomic().getBillFinanceType() == BillFinanceType.CASH_IN) {
            createPayment(printingBill, paymentMethod);
        }
        sendSmsAfterBooking();
        settleSucessFully = true;
        printPreview = true;
        JsfUtil.addSuccessMessage("Channel Booking Added.");
    }

    public BillSession addChannelBookingForOnlinePayment() {
        errorText = "";
        if (billSessionErrorPresent()) {
            JsfUtil.addErrorMessage("Session Selection Error. Please Retry From Beginning");
            settleSucessFully = false;
            return null;
        }
        if (patientErrorPresent(patient)) {
            JsfUtil.addErrorMessage("Please Enter Patient Details.");
            settleSucessFully = false;
            return null;
        }
        if (paymentMethodErrorPresent()) {
            JsfUtil.addErrorMessage("Please Enter Payment Details");
            settleSucessFully = false;
            return null;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Channelling Patients Cannot Be Added After the Channel Has Been Completed")) {
            if (selectedSessionInstance.isCompleted()) {
                JsfUtil.addErrorMessage("This Session Has Been Completed");
                settleSucessFully = false;
                return null;
            }
        }
        saveSelected(patient);
        return saveBilledBillForPatientPortal();
    }

    public void sendSmsAfterBooking() {
        Sms e = new Sms();
        e.setCreatedAt(new Date());
        e.setCreater(sessionController.getLoggedUser());
        e.setBill(printingBill);
        e.setCreatedAt(new Date());
        e.setCreater(sessionController.getLoggedUser());
        e.setReceipientNumber(printingBill.getPatient().getPerson().getPhone());
        e.setSendingMessage(createChanellBookingSms(printingBill));
        e.setDepartment(getSessionController().getLoggedUser().getDepartment());
        e.setInstitution(getSessionController().getLoggedUser().getInstitution());
        e.setPending(false);
        e.setSmsType(MessageType.ChannelBooking);
        getSmsFacade().create(e);
        Boolean sent = smsManager.sendSms(e);

        if (sent) {
            JsfUtil.addSuccessMessage("SMS Sent");
        } else {
            JsfUtil.addSuccessMessage("SMS Failed");
        }

    }

    public void sendSmsOnChannelDoctorArrival() {
        if (billSessions == null || billSessions.isEmpty()) {
            return;
        }
        for (BillSession bs : billSessions) {
            if (bs.getBill() == null) {
                continue;
            }
            if (bs.getBill().getPatient().getPerson().getSmsNumber() == null) {
                continue;
            }
            Sms e = new Sms();
            e.setCreatedAt(new Date());
            e.setCreater(sessionController.getLoggedUser());
            e.setBill(bs.getBill());
            e.setReceipientNumber(bs.getBill().getPatient().getPerson().getSmsNumber());
            e.setSendingMessage(createChanellBookingDoctorArrivalSms(bs.getBill()));
            e.setDepartment(getSessionController().getLoggedUser().getDepartment());
            e.setInstitution(getSessionController().getLoggedUser().getInstitution());
            e.setPending(false);
            e.setSmsType(MessageType.ChannelDoctorArrival);
            getSmsFacade().create(e);
            Boolean sent = smsManager.sendSms(e);

        }
        JsfUtil.addSuccessMessage("SMS Sent to all Patients.");
    }

    public void sendSmsOnChannelMissingChannelBookings() {
        if (billSessions == null || billSessions.isEmpty()) {
            return;
        }
        for (BillSession bs : billSessions) {
            if (bs.getBill() == null) {
                continue;
            }
            if (bs.getBill().getPatient().getPerson().getSmsNumber() == null) {
                continue;
            }
            if (bs.isCompleted()) {
                continue;
            }
            Sms e = new Sms();
            e.setCreatedAt(new Date());
            e.setCreater(sessionController.getLoggedUser());
            e.setBill(bs.getBill());
            e.setReceipientNumber(bs.getBill().getPatient().getPerson().getSmsNumber());
            e.setSendingMessage(createChanellBookingNoShowSms(bs.getBill()));
            e.setDepartment(getSessionController().getLoggedUser().getDepartment());
            e.setInstitution(getSessionController().getLoggedUser().getInstitution());
            e.setPending(false);
            e.setSmsType(MessageType.ChannelNoShow);
            getSmsFacade().create(e);
            Boolean sent = smsManager.sendSms(e);

        }
        JsfUtil.addSuccessMessage("SMS Sent to all No Show Patients.");
    }

    public void sendSmsOnChannelCancellationBookings() {
        if (billSessions == null || billSessions.isEmpty()) {
            return;
        }
        BillSession bs = getBillSession();
        if (bs.getBill() == null) {
            return;
        }
        if (bs.getBill().getPatient().getPerson().getSmsNumber() == null) {
            return;
        }
        if (bs.isCompleted()) {
            return;
        }
        Sms e = new Sms();
        e.setCreatedAt(new Date());
        e.setCreater(sessionController.getLoggedUser());
        e.setBill(bs.getBill());
        e.setReceipientNumber(bs.getBill().getPatient().getPerson().getSmsNumber());
        e.setSendingMessage(createChanellCancellationBookingSms(bs.getBill()));
        e.setDepartment(getSessionController().getLoggedUser().getDepartment());
        e.setInstitution(getSessionController().getLoggedUser().getInstitution());
        e.setPending(false);
        e.setSmsType(MessageType.ChannelBookingCancellation);
        getSmsFacade().create(e);
        Boolean sent = smsManager.sendSms(e);

        JsfUtil.addSuccessMessage("SMS Sent Patient.");
    }

    private String createChanellBookingDoctorArrivalSms(Bill b) {
        return createSmsForChannelBooking(b, sessionController.getDepartmentPreference().getSmsTemplateForChannelDoctorArrival());
    }

    private String createChanellBookingNoShowSms(Bill b) {
        return createSmsForChannelBooking(b, sessionController.getDepartmentPreference().getSmsTemplateForChannelBookingNoShow());
    }

    private String createChanellBookingSms(Bill b) {
//        String template = sessionController.getDepartmentPreference().getSmsTemplateForChannelBooking();
        String template = configOptionController.getLongTextValueByKey("Template for SMS sent on Channel Booking", OptionScope.APPLICATION, null, null, null);
        if (template == null || template.isEmpty()) {
            template = "Dear {patient_name}, Your appointment with {doctor} is confirmed for {appointment_time} on {appointment_date}. Your serial No. is {serial_no}. Please arrive 10 minutes early. Thank you.";
        return createSmsForChannelBooking(b, template);
    }
    
    private String createChanellCancellationBookingSms(Bill b) {
//        String template = sessionController.getDepartmentPreference().getSmsTemplateForChannelBooking();
        String template = configOptionController.getLongTextValueByKey("Template for SMS sent on Channel Booking Cancellation", OptionScope.APPLICATION, null, null, null);
        if (template == null || template.isEmpty()) {
            template = "Dear {patient_name}, Your appointment No. {serial_no} with {doctor} on {appointment_date} at {appointment_time} is cancelled. {ins_name}";
        }
        return createSmsForChannelBooking(b, template);
    }
    
    private String createChanellCancellationBookingSms(Bill b) {
//        String template = sessionController.getDepartmentPreference().getSmsTemplateForChannelBooking();
        String template = configOptionController.getLongTextValueByKey("Template for SMS sent on Channel Booking Cancellation", OptionScope.APPLICATION, null, null, null);
        if (template == null || template.isEmpty()) {
            template = "Dear {patient_name}, Your appointment No. {serial_no} with {doctor} on {appointment_date} at {appointment_time} is cancelled. {ins_name}";
        }
        return createSmsForChannelBooking(b, template);
    }

    private String createChanellCancellationBookingSms(Bill b) {
//        String template = sessionController.getDepartmentPreference().getSmsTemplateForChannelBooking();
        String template = configOptionController.getLongTextValueByKey("Template for SMS sent on Channel Booking Cancellation", OptionScope.APPLICATION, null, null, null);
        if (template == null || template.isEmpty()) {
            template = "Dear {patient_name}, Your appointment No. {serial_no} with {doctor} on {appointment_date} at {appointment_time} is cancelled. {ins_name}";
        }
        return createSmsForChannelBooking(b, template);
    }

    private String createChanellSessionCancellationBookingSms(Bill b) {
//        String template = sessionController.getDepartmentPreference().getSmsTemplateForChannelBooking();
        String smsTemplateForchannelSessionCancellation = configOptionController.getLongTextValueByKey("Template for SMS Sent on Channel Booking Session Cancellation", OptionScope.APPLICATION, null, null, null);
        if (smsTemplateForchannelSessionCancellation == null || smsTemplateForchannelSessionCancellation.isEmpty()) {
            smsTemplateForchannelSessionCancellation = "Dear {patient_name}, We are sorry! {doctor}'s consultation session on {appointment_date} at {appointment_time} is cancelled. {ins_name}";
        }
        return createSmsForChannelBooking(b, smsTemplateForchannelSessionCancellation);
    }

    private String createChanellCancellationBookingSms(Bill b) {
//        String template = sessionController.getDepartmentPreference().getSmsTemplateForChannelBooking();
        String template = configOptionController.getLongTextValueByKey("Template for SMS sent on Channel Booking Cancellation", OptionScope.APPLICATION, null, null, null);
        if (template == null || template.isEmpty()) {
            template = "Dear {patient_name}, Your appointment No. {serial_no} with {doctor} on {appointment_date} at {appointment_time} is cancelled. {ins_name}";
        }
        return createSmsForChannelBooking(b, template);
    }
    
    private String createChanellCancellationBookingSms(Bill b) {
//        String template = sessionController.getDepartmentPreference().getSmsTemplateForChannelBooking();
        String template = configOptionController.getLongTextValueByKey("Template for SMS sent on Channel Booking Cancellation", OptionScope.APPLICATION, null, null, null);
        if (template == null || template.isEmpty()) {
            template = "Dear {patient_name}, Your appointment No. {serial_no} with {doctor} on {appointment_date} at {appointment_time} is cancelled. {ins_name}";
        }
        return createSmsForChannelBooking(b, template);
    }
    
    private String createChanellSessionCancellationBookingSms(Bill b) {
//        String template = sessionController.getDepartmentPreference().getSmsTemplateForChannelBooking();
        String smsTemplateForchannelSessionCancellation = configOptionController.getLongTextValueByKey("Template for SMS Sent on Channel Booking Session Cancellation", OptionScope.APPLICATION, null, null, null);
        if (smsTemplateForchannelSessionCancellation == null || smsTemplateForchannelSessionCancellation.isEmpty()) {
            smsTemplateForchannelSessionCancellation = "Dear {patient_name}, We are sorry! {doctor}'s consultation session on {appointment_date} at {appointment_time} is cancelled. {ins_name}";
        }
        return createSmsForChannelBooking(b, smsTemplateForchannelSessionCancellation);
    }

    private String createChanellSessionCancellationBookingSms(Bill b) {
//        String template = sessionController.getDepartmentPreference().getSmsTemplateForChannelBooking();
        String smsTemplateForchannelSessionCancellation = configOptionController.getLongTextValueByKey("Template for SMS Sent on Channel Booking Session Cancellation", OptionScope.APPLICATION, null, null, null);
        if (smsTemplateForchannelSessionCancellation == null || smsTemplateForchannelSessionCancellation.isEmpty()) {
            smsTemplateForchannelSessionCancellation = "Dear {patient_name}, We are sorry! {doctor}'s consultation session on {appointment_date} at {appointment_time} is cancelled. {ins_name}";
        }
        return createSmsForChannelBooking(b, smsTemplateForchannelSessionCancellation);
    }

    public String createSmsForChannelBooking(Bill b, String template) {
        if (b == null) {
            return "";
        }
        if (b.getSingleBillSession() == null) {
            return "";
        }
        if (b.getSingleBillSession().getSessionInstance() == null) {
            return "";
        }
        if (b.getSingleBillSession().getSessionInstance().getOriginatingSession() == null) {
            return "";
        }
        SessionInstance si = b.getSingleBillSession().getSessionInstance();
        BillSession bs = b.getSingleBillSession();
        ServiceSession ss = b.getSingleBillSession().getSessionInstance().getOriginatingSession();
        String s;

        String sessionTime = CommonController.getDateFormat(si.getStartingTime(), sessionController.getApplicationPreference().getShortTimeFormat());
        String sessionDate = CommonController.getDateFormat(si.getSessionDate(), sessionController.getApplicationPreference().getLongDateFormat());
        String doc = bs.getStaff().getPerson().getNameWithTitle();
        String patientName = b.getPatient().getPerson().getNameWithTitle();
        int no = b.getSingleBillSession().getSerialNo();

        String insName = sessionController.getLoggedUser().getInstitution().getName();

        s = template.replace("{patient_name}", patientName)
                .replace("{doctor}", doc)
                .replace("{appointment_time}", sessionTime)
                .replace("{appointment_date}", sessionDate)
                .replace("{serial_no}", String.valueOf(no))
                .replace("{doc}", doc)
                .replace("{time}", sessionTime)
                .replace("{date}", sessionDate)
                .replace("{ins_name}", insName)
                .replace("{No}", String.valueOf(no));

        return s;
    }

    public String createSmsForChannelBookingReschedule(Bill b, BillSession b1, String template) {
        if (b == null) {
            return "";
        }
        if (b.getSingleBillSession() == null) {
            return "";
        }
        if (b.getSingleBillSession().getSessionInstance() == null) {
            return "";
        }
        if (b.getSingleBillSession().getSessionInstance().getOriginatingSession() == null) {
            return "";
        }
        SessionInstance si = b.getSingleBillSession().getSessionInstance();
        BillSession bs = b.getSingleBillSession();
        ServiceSession ss = b.getSingleBillSession().getSessionInstance().getOriginatingSession();
        String s;

        String sessionTime = CommonController.getDateFormat(si.getStartingTime(), sessionController.getApplicationPreference().getShortTimeFormat());
        String sessionDate = CommonController.getDateFormat(si.getSessionDate(), sessionController.getApplicationPreference().getLongDateFormat());
        String doc = bs.getStaff().getPerson().getNameWithTitle();
        String patientName = b.getPatient().getPerson().getNameWithTitle();
        int no = b.getSingleBillSession().getSerialNo();
        String insName = sessionController.getLoggedUser().getInstitution().getName();

        String newSessionTime = CommonController.getDateFormat(b1.getSessionTime(), sessionController.getApplicationPreference().getShortTimeFormat());
        String newSessionDate = CommonController.getDateFormat(b1.getSessionDate(), sessionController.getApplicationPreference().getLongDateFormat());
        String newDoc = bs.getStaff().getPerson().getNameWithTitle();
        int newNo = b1.getSerialNo();

        s = template.replace("{patient_name}", patientName)
                .replace("{doctor}", doc)
                .replace("{appointment_time}", sessionTime)
                .replace("{appointment_date}", sessionDate)
                .replace("{serial_no}", String.valueOf(no))
                .replace("{doc}", doc)
                .replace("{time}", sessionTime)
                .replace("{date}", sessionDate)
                .replace("{No}", String.valueOf(no))
                .replace("{new_serial_no}", String.valueOf(newNo))
                .replace("{new_doctor}", newDoc)
                .replace("{new_appointment_date}", newSessionDate)
                .replace("{new_appointment_time}", newSessionTime)
                .replace("{ins_name}", insName);

        return s;
    }

    public void updateSerial() {
        if (errorCheckForSerial()) {
            return;
        }
        if (billSessionErrorPresent()) {
            return;
        }

        BillSession selectedBillSession = getSelectedBillSession();
        if (selectedBillSession == null) {
            // Handle case when selectedBillSession is null
            return;
        }

//        for (BillItem bi : selectedBillSession.getBill().getBillItems()) {
//            BillSession tmpSession = bi.getBillSession();
//            if (tmpSession != null) {
//                tmpSession.setSerialNo(serealNo);
//                getBillItemFacade().edit(bi);
//            }
//        }
        getBillSessionFacade().edit(selectedBillSession);
        JsfUtil.addSuccessMessage("Serial Updated");
    }

    public void resetToStartFromSelectingSpeciality() {
        speciality = null;
        staff = null;
        sessionInstances = null;
        billSessions = null;
        sessionStartingDate = null;
        itemsAvailableToAddToBooking = new ArrayList<>();
        itemToAddToBooking = null;
        itemsAddedToBooking = null;
        patient = new Patient();
    }

    public void resetToStartFromSelectingConsultant() {
        staff = null;
        sessionInstances = null;
        billSessions = null;
        sessionStartingDate = null;
        itemToAddToBooking = null;
        itemsAddedToBooking = null;
        itemsAvailableToAddToBooking = new ArrayList<>();
        patient = new Patient();
    }

    public void resetToStartFromSelectingSessionInstance() {
        sessionInstances = null;
        billSessions = null;
        sessionStartingDate = null;
        itemToAddToBooking = null;
        itemsAddedToBooking = null;
        patient = new Patient();
    }

    public void resetToStartFromSameSessionInstance() {
        patient = new Patient();
        itemToAddToBooking = null;
        itemsAddedToBooking = null;
    }

    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (getSpeciality() != null) {
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
            } else {
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            }
            ////System.out.println(sql);
            suggestions = getStaffFacade().findByJpql(sql);
        }
        return suggestions;
    }

    public void fillConsultants() {
        String sql;
        Map m = new HashMap();
        m.put("sp", getSpeciality());
        if (getSpeciality() != null) {
            if (getSessionController().getInstitutionPreference().isShowOnlyMarkedDoctors()) {

                sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                        + " and pi.type=:typ "
                        + " and pi.institution=:ins "
                        + " and pi.staff.speciality=:sp "
                        + " order by pi.staff.person.name ";

                m.put("ins", getSessionController().getInstitution());
                m.put("typ", PersonInstitutionType.Channelling);

            } else {
                sql = "select p from Staff p where p.retired=false and p.speciality=:sp order by p.person.name";
            }

            consultants = getStaffFacade().findByJpql(sql, m);
        } else {
            sql = "select p from Staff p where p.retired=false order by p.person.name";
            consultants = getStaffFacade().findByJpql(sql);
        }
//        //System.out.println("consultants = " + consultants);
        setStaff(null);
    }

    public List<Staff> getSelectedConsultants() {
        String sql;
        Map m = new HashMap();
        if (getSpeciality() == null) {
            sql = " select s "
                    + " from Staff s "
                    + " where s.retired=:ret "
                    + " and s.speciality in :sps "
                    + " order by s.person.name ";
            m.put("ret", false);
            m.put("sps", doctorSpecialityController.getSelectedItems());
            consultants = getStaffFacade().findByJpql(sql, m);
        } else {
            sql = "select s "
                    + " from Staff s "
                    + " where s.retired=:ret "
                    + " and s.speciality=:sp "
                    + "order by s.person.name";
            m.put("ret", false);
            m.put("sp", getSpeciality());
            consultants = getStaffFacade().findByJpql(sql, m);
        }
        if (consultants == null) {
            consultants = new ArrayList<>();
        }
        return consultants;
    }

    public List<Staff> getConsultants() {
        if (consultants == null) {
            consultants = new ArrayList<>();
        }
        return consultants;
    }

    public void setConsultants(List<Staff> consultants) {
        this.consultants = consultants;
    }

    /**
     * Creates a new instance of BookingController
     */
    public BookingControllerViewScope() {
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

//    public void setSpeciality(Speciality speciality) {
//        this.speciality = speciality;
//        fillConsultants();
//        setStaff(null);
//    }
    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

//    public void setStaff(Staff staff) {
////        System.err.println("CLIKED");
//        this.staff = staff;
//        //generateSessions();
//        setSelectedServiceSession(null);
//        serviceSessionLeaveController.setSelectedServiceSession(null);
//        serviceSessionLeaveController.setCurrentStaff(staff);
//    }
    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = new Date();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    private Double[] fetchFee(Item item, FeeType feeType) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select sum(f.fee),sum(f.ffee) "
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses "
                + " and f.feeType=:ftp";
        m.put("ses", item);
        m.put("ftp", feeType);
        Object[] obj = getItemFeeFacade().findAggregateModified(jpql, m, TemporalType.TIMESTAMP);

        if (obj == null) {
            Double[] dbl = new Double[2];
            dbl[0] = 0.0;
            dbl[1] = 0.0;
            return dbl;

        }

        Double[] dbl = Arrays.copyOf(obj, obj.length, Double[].class);
//        System.err.println("Fetch Fee Values " + dbl);
        return dbl;
    }

    private double fetchLocalFee(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        jpql = "Select sum(f.fee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            return 0;
        }

        return obj;
    }

    private double fetchForiegnFee(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        jpql = "Select sum(f.ffee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            return 0;
        }

        return obj;
    }

    private List<ItemFee> fetchFee(Item item) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select f "
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";
        m.put("ses", item);
        List<ItemFee> list = getItemFeeFacade().findByJpql(jpql, m, TemporalType.TIMESTAMP);
//        System.err.println("Fetch Fess " + list.size());
        return list;
    }

    public void calculateFee(List<ServiceSession> lstSs, PaymentMethod paymentMethod) {
        for (ServiceSession ss : lstSs) {
            Double[] dbl = fetchFee(ss, FeeType.OwnInstitution);
            ss.setHospitalFee(dbl[0]);
            ss.setHospitalFfee(dbl[1]);
            dbl = fetchFee(ss, FeeType.Staff);
            ss.setProfessionalFee(dbl[0]);
            ss.setProfessionalFfee(dbl[1]);
//            System.err.println("1111");
            dbl = fetchFee(ss, FeeType.Tax);
//            System.err.println("2222");
            ss.setTaxFee(dbl[0]);
            ss.setTaxFfee(dbl[1]);
            ss.setTotalFee(fetchLocalFee(ss, paymentMethod));
            ss.setTotalFfee(fetchForiegnFee(ss, paymentMethod));
            ss.setItemFees(fetchFee(ss));
        }
    }

    @Deprecated
    public void calculateFeeBooking(List<ServiceSession> lstSs, PaymentMethod paymentMethod) {
        for (ServiceSession ss : lstSs) {
            Double[] dbl = fetchFee(ss.getOriginatingSession(), FeeType.OwnInstitution);
            ss.setHospitalFee(dbl[0]);
            ss.setHospitalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setHospitalFee(dbl[0]);
            ss.getOriginatingSession().setHospitalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Staff);
            ss.setProfessionalFee(dbl[0]);
            ss.setProfessionalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setProfessionalFee(dbl[0]);
            ss.getOriginatingSession().setProfessionalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Tax);
            ss.setTaxFee(dbl[0]);
            ss.setTaxFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setTaxFee(dbl[0]);
            ss.getOriginatingSession().setTaxFfee(dbl[1]);
            //For Settle bill
            ss.setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
            ss.setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));
            ss.setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
            ss.getOriginatingSession().setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
            ss.getOriginatingSession().setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));
            ss.getOriginatingSession().setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
        }
    }

    public void calculateFeeForSessionInstances(List<SessionInstance> lstSs, PaymentMethod paymentMethod) {
        for (SessionInstance ss : lstSs) {
            Double[] dbl = fetchFee(ss.getOriginatingSession(), FeeType.OwnInstitution);
            ss.setHospitalFee(dbl[0]);
            ss.setHospitalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setHospitalFee(dbl[0]);
            ss.getOriginatingSession().setHospitalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Staff);
            ss.setProfessionalFee(dbl[0]);
            ss.setProfessionalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setProfessionalFee(dbl[0]);
            ss.getOriginatingSession().setProfessionalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Tax);
            ss.setTaxFee(dbl[0]);
            ss.setTaxFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setTaxFee(dbl[0]);
            ss.getOriginatingSession().setTaxFfee(dbl[1]);
            //For Settle bill
            ss.setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
            ss.setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));
            ss.setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
            ss.getOriginatingSession().setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
            ss.getOriginatingSession().setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));
            ss.getOriginatingSession().setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
        }
    }

    public void generateSessions() {
        sessionInstances = new ArrayList<>();
        String jpql;
        Map params = new HashMap();
        params.put("staff", getStaff());
        params
                .put("class", ServiceSession.class
                );
        if (staff != null) {
            jpql = "Select s From ServiceSession s "
                    + " where s.retired=false "
                    + " and s.staff=:staff "
                    + " and s.originatingSession is null"
                    + " and type(s)=:class ";
            boolean listChannelSessionsForLoggedDepartmentOnly = configOptionApplicationController.getBooleanValueByKey("List Channel Sessions For Logged Department Only", false);
            boolean listChannelSessionsForLoggedInstitutionOnly = configOptionApplicationController.getBooleanValueByKey("List Channel Sessions For Logged Institution Only", false);
            if (listChannelSessionsForLoggedDepartmentOnly) {
                jpql += " and s.department=:dept ";
                params.put("dept", sessionController.getDepartment());
            }
            if (listChannelSessionsForLoggedInstitutionOnly) {
                jpql += " and s.institution=:ins ";
                params.put("ins", sessionController.getInstitution());
            }
            jpql += " order by s.sessionWeekday,s.startingTime ";
            List<ServiceSession> selectedDoctorsServiceSessions = getServiceSessionFacade().findByJpql(jpql, params);
            calculateFee(selectedDoctorsServiceSessions, channelBillController.getPaymentMethod());
            try {
                sessionInstances = getChannelBean().generateSesionInstancesFromServiceSessions(selectedDoctorsServiceSessions, sessionStartingDate);
            } catch (Exception e) {
            }
            generateSessionEvents(sessionInstances);
        } else {
            sessionInstances = new ArrayList<>();
        }

    }

    public void generateSessionEvents(List<SessionInstance> sss) {
        eventModel = new DefaultScheduleModel();
        for (SessionInstance s : sss) {
            ChannelScheduleEvent e = new ChannelScheduleEvent();
            e.setSessionInstance(s);
            e.setTitle(s.getName());
            e.setStartDate(CommonFunctions.convertDateToLocalDateTime(s.getTransStartTime()));
            e.setEndDate(CommonFunctions.convertDateToLocalDateTime(s.getTransEndTime()));
            eventModel.addEvent(e);
        }
    }

    public void onEventSelect(SelectEvent selectEvent) {
        event = (ChannelScheduleEvent) selectEvent.getObject();
        selectedServiceSession = event.getServiceSession();
        fillBillSessions();
    }

    public void generateSessionsFutureBooking(SelectEvent event) {
        fromDate = null;
        fromDate = ((Date) event.getObject());
        sessionInstances = new ArrayList<>();
        Map m = new HashMap();

        Date currenDate = new Date();
        if (getFromDate().before(currenDate)) {
            JsfUtil.addErrorMessage("Please Select Future Date");
            return;
        }

        String sql = "";

        if (staff != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(getFromDate());
            int wd = c.get(Calendar.DAY_OF_WEEK);

            sql = "Select s From ServiceSession s "
                    + " where s.retired=false "
                    + " and s.staff=:staff "
                    + " and s.sessionWeekday=:wd ";

            m.put("staff", getStaff());
            m.put("wd", wd);
            List<ServiceSession> tmp = getServiceSessionFacade().findByJpql(sql, m);
            calculateFee(tmp, channelBillController.getPaymentMethod());//check work future bokking
            sessionInstances = getChannelBean().generateSesionInstancesFromServiceSessions(tmp, fromDate);
        }

        billSessions = new ArrayList<>();
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public List<SessionInstance> getServiceSessions() {
        return sessionInstances;
    }

    public void setServiceSessions(List<SessionInstance> serviceSessions) {
        this.sessionInstances = serviceSessions;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public List<BillSession> getBillSessions() {
        if (billSessions == null) {
            billSessions = new ArrayList<>();
        }
        return billSessions;
    }

//    public void fillBillSessions(SelectEvent event) {
//        selectedBillSession = null;
//        selectedServiceSession = ((ServiceSession) event.getObject());
//
//        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
//        List<BillType> bts = Arrays.asList(billTypes);
//
//        String sql = "Select bs From BillSession bs "
//                + " where bs.retired=false"
//                + " and bs.serviceSession=:ss "
//                + " and bs.bill.billType in :bt"
//                + " and type(bs.bill)=:class "
//                + " and bs.sessionDate= :ssDate "
//                + " order by bs.serialNo ";
//        HashMap hh = new HashMap();
//        hh.put("bt", bts);
//        hh.put("class", BilledBill.class);
//        hh.put("ssDate", getSelectedServiceSession().getSessionAt());
//        hh.put("ss", getSelectedServiceSession());
//        billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);
//        System.out.println("hh = " + hh);
//        System.out.println("getSelectedServiceSession().isTransLeave() = " + getSelectedServiceSession().isTransLeave());
//        if (getSelectedServiceSession().isTransLeave()) {
//            billSessions=null;
//        }
//        System.out.println("billSessions" + billSessions);
//
//    }
    public void findArrivals() {

        String sql = "Select bs From ArrivalRecord bs "
                + " where bs.retired=false"
                + " and bs.serviceSession=:ss "
                + " and bs.sessionDate= :ssDate ";
        HashMap hh = new HashMap();
        hh.put("ssDate", getSelectedServiceSession().getSessionDate());
        hh.put("ss", getSelectedServiceSession());
        arrivalRecord = (ArrivalRecord) fpFacade.findFirstByJpql(sql, hh);
    }

    public void markAsArrived() {
        if (selectedSessionInstance == null) {
            return;
        }
        if (selectedSessionInstance.getSessionDate() == null) {
            return;
        }
        if (arrivalRecord == null) {
            arrivalRecord = new ArrivalRecord();
            arrivalRecord.setSessionDate(selectedSessionInstance.getSessionDate());
            arrivalRecord.setSessionInstance(selectedSessionInstance);
            arrivalRecord.setCreatedAt(new Date());
            arrivalRecord.setCreater(sessionController.getLoggedUser());
            fpFacade.create(arrivalRecord);
        }
        selectedSessionInstance.setArrived(true);
        selectedSessionInstance.setArrivalRecord(arrivalRecord);
        sessionInstanceFacade.edit(selectedSessionInstance);
        arrivalRecord.setRecordTimeStamp(new Date());
        arrivalRecord.setApproved(false);
        fpFacade.edit(arrivalRecord);
        sendSmsOnChannelDoctorArrival();
    }

    public void markAsNotArrived() {
        if (selectedSessionInstance == null) {
            return;
        }
        if (selectedSessionInstance.getSessionDate() == null) {
            return;
        }
        if (arrivalRecord == null) {
            arrivalRecord = new ArrivalRecord();
            arrivalRecord.setSessionDate(selectedSessionInstance.getSessionDate());
            arrivalRecord.setSessionInstance(selectedSessionInstance);
            arrivalRecord.setCreatedAt(new Date());
            arrivalRecord.setCreater(sessionController.getLoggedUser());
            fpFacade.create(arrivalRecord);
        }
        selectedSessionInstance.setArrived(false);
        selectedSessionInstance.setArrivalRecord(arrivalRecord);
        sessionInstanceFacade.edit(selectedSessionInstance);
        arrivalRecord.setRecordTimeStamp(new Date());
        arrivalRecord.setApproved(false);
        fpFacade.edit(arrivalRecord);
        sendSmsOnChannelDoctorArrival();
    }

    public void markAsLeft() {
        if (selectedServiceSession == null) {
            return;
        }
        if (selectedServiceSession.getSessionDate() == null) {
            return;
        }
        if (arrivalRecord == null) {
            arrivalRecord = new ArrivalRecord();
            arrivalRecord.setSessionDate(selectedServiceSession.getSessionDate());
            arrivalRecord.setServiceSession(selectedServiceSession);
            arrivalRecord.setCreatedAt(new Date());
            arrivalRecord.setCreater(sessionController.getLoggedUser());
            fpFacade.create(arrivalRecord);
        }

        arrivalRecord.setApproved(true);
        arrivalRecord.setApprovedAt(new Date());
        arrivalRecord.setApprover(sessionController.getLoggedUser());
        fpFacade.edit(arrivalRecord);
    }

    public void markToCancel() {
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return;
        }
        if (Boolean.TRUE.equals(selectedBillSession.getMarkedToRefund())) {
            JsfUtil.addErrorMessage("Cannot cancel a session marked for refund.");
            return;
        }
        selectedBillSession.setMarkedToCancel(true);
        selectedBillSession.setMarkedToCancelAt(new Date());
        selectedBillSession.setMarkedToCancelBy(sessionController.getLoggedUser());
        billSessionFacade.edit(selectedBillSession);
        JsfUtil.addErrorMessage("Marked to Cancelled");
    }

    public void markToCancelReversed() {
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return;
        }
        if (Boolean.TRUE.equals(selectedBillSession.getMarkedToCancel())) {
            JsfUtil.addErrorMessage("Not a session marked as cancelled. Can not reverse.");
            return;
        }

        selectedBillSession.setMarkedToCancel(false);
        selectedBillSession.setMarkedToCancelAt(new Date());
        selectedBillSession.setMarkedToCancelBy(sessionController.getLoggedUser());
        billSessionFacade.edit(selectedBillSession);
        JsfUtil.addErrorMessage("Marked to Cancelle Reversed");
    }

    public void markToRefund() {
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("Nothing to refund");
            return;
        }
        if (Boolean.TRUE.equals(selectedBillSession.getMarkedToCancel())) {
            JsfUtil.addErrorMessage("Cannot cancel a session marked for refund.");
            return;
        }

        selectedBillSession.setMarkedToRefund(true);
        selectedBillSession.setMarkedToRefundAt(new Date());
        selectedBillSession.setMarkedToRefundBy(sessionController.getLoggedUser());
        billSessionFacade.edit(selectedBillSession);
        JsfUtil.addErrorMessage("Marked to Refund");
    }

    public void markToRefundReversed() {
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return;
        }
        if (Boolean.TRUE.equals(selectedBillSession.getMarkedToRefund())) {
            JsfUtil.addErrorMessage("Is not a session marked as to refund. Can not reverse.");
            return;
        }
        selectedBillSession.setMarkedToRefund(false);
        selectedBillSession.setMarkedToRefundAt(new Date());
        selectedBillSession.setMarkedToRefundBy(sessionController.getLoggedUser());
        billSessionFacade.edit(selectedBillSession);
        JsfUtil.addErrorMessage("Marked to Refund was reversed.");
    }

    public void markCurrentCompleteAndCallNext() {
        BillSession lastCompletedSession = null;
        BillSession currentSession = null;
        BillSession nextSession = null;

        // Iterate through the billSessions list
        for (int i = 0; i < getValidBillSessions().size(); i++) {
            BillSession bs = getValidBillSessions().get(i);
            if (Boolean.TRUE.equals(bs.getCurrentlyConsulted())) {
                // Mark the currently consulted session as completed
                bs.setCompleted(true);
                bs.setCurrentlyConsulted(false);
                lastCompletedSession = bs; // This becomes the last completed session
                billSessionFacade.edit(bs);

                // Check for the next session in the list
                if (i + 1 < getValidBillSessions().size()) {
                    nextSession = getValidBillSessions().get(i + 1);
                    nextSession.setCurrentlyConsulted(true);
                    nextSession.setNextInLine(false);
                    currentSession = nextSession; // This is now the currently consulting session
                    billSessionFacade.edit(nextSession);
                }
                break;
            }
        }

        // Set the next in line session if there is one
        if (nextSession != null && getValidBillSessions().size() > getValidBillSessions().indexOf(nextSession) + 1) {
            BillSession newNextInLine = getValidBillSessions().get(getValidBillSessions().indexOf(nextSession) + 1);
            newNextInLine.setNextInLine(true);
            billSessionFacade.edit(newNextInLine); // Update the next in line session
            selectedSessionInstance.setNextInLineBillSession(newNextInLine);
        } else {
            selectedSessionInstance.setNextInLineBillSession(null);
        }

        // Update the SessionInstance with the new lastCompleted, currentlyConsulting, and nextInLine sessions
        selectedSessionInstance.setLastCompletedBillSession(lastCompletedSession);
        selectedSessionInstance.setCurrentlyConsultingBillSession(currentSession);
        // Note: The next in line has already been set above if available

        // Persist changes to the SessionInstance
        sessionInstanceFacade.edit(selectedSessionInstance);
    }

    public List<BillSession> getValidBillSessions() {
        List<BillSession> validBillSessions = new ArrayList<>();
        if (billSessions == null) {
            return null;
        }
        if (billSessions.isEmpty()) {
            return validBillSessions;
        }
        for (BillSession tbs : billSessions) {
            if (tbs.getPaidBillSession() == null) {
                continue;
            }
            if (tbs.isRetired()) {
                continue;
            }
            if (tbs.getBill().isCancelled()) {
                continue;
            }
            validBillSessions.add(tbs);
        }
        return validBillSessions;
    }

    public void startFirstSession() {
        BillSession lastCompletedSession = null;
        BillSession currentSession = null;
        BillSession nextSession = null;
        boolean currentFound = false;

        if (getValidBillSessions() == null) {
            return;
        }
        if (getValidBillSessions().isEmpty()) {
            return;
        }

        currentSession = getValidBillSessions().get(0);
        currentSession.setCurrentlyConsulted(true);
        currentSession.setNextInLine(false);
        billSessionFacade.edit(currentSession);

        if (getValidBillSessions().size() > 1) {
            nextSession = getValidBillSessions().get(1);
            nextSession.setCurrentlyConsulted(false);
            nextSession.setNextInLine(true);
            billSessionFacade.edit(nextSession);
            selectedSessionInstance.setNextInLineBillSession(nextSession);
        }

        selectedSessionInstance.setLastCompletedBillSession(null);
        selectedSessionInstance.setCurrentlyConsultingBillSession(currentSession);
        sessionInstanceFacade.edit(selectedSessionInstance);

    }

    public void reverseCurrentCompleteAndCallPrevious() {
        BillSession currentSession = null;
        BillSession previousSession = null;

        // Find the index of the currently consulting session
        int currentIndex = -1;
        for (int i = 0; i < getValidBillSessions().size(); i++) {
            if (Boolean.TRUE.equals(getValidBillSessions().get(i).getCurrentlyConsulted())) {
                currentIndex = i;
                break;
            }
        }

        // If a currently consulting session is found
        if (currentIndex != -1) {
            // Reverse the completion of the current session
            currentSession = getValidBillSessions().get(currentIndex);
            currentSession.setCompleted(false);
            billSessionFacade.edit(currentSession);

            // There is a session before the current one
            if (currentIndex - 1 >= 0) {
                previousSession = getValidBillSessions().get(currentIndex - 1);

                // Reverse the last completed session to the previous session
                previousSession.setCurrentlyConsulted(true);
                if (!previousSession.isCompleted()) {
                    selectedSessionInstance.setLastCompletedBillSession(null);
                } else {
                    // If the previous session was completed, update the lastCompletedBillSession accordingly
                    selectedSessionInstance.setLastCompletedBillSession(previousSession);
                }
                billSessionFacade.edit(previousSession);

                // Update currently consulting session to previous session
                selectedSessionInstance.setCurrentlyConsultingBillSession(previousSession);

                // Set the nextInLineBillSession to the currentSession
                currentSession.setNextInLine(true);
                billSessionFacade.edit(currentSession);
                selectedSessionInstance.setNextInLineBillSession(currentSession);
            } else {
                // If there's no previous session, handle accordingly, perhaps clearing the currentlyConsulted status
                selectedSessionInstance.setCurrentlyConsultingBillSession(null);
                selectedSessionInstance.setNextInLineBillSession(null);
            }
        }

        // If a previous session is found and set as the current session
        if (previousSession != null) {
            // Update the currentlyConsulted status for the session that was just reversed
            currentSession.setCurrentlyConsulted(false);
            billSessionFacade.edit(currentSession);
        }

        // Update changes to SessionInstance
        sessionInstanceFacade.edit(selectedSessionInstance);
    }

    public void markSelectedAsCurrentSession() {
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("Select");
            return;
        }
        BillSession currentSession = selectedBillSession;
        billSessionFacade.edit(currentSession);

        selectedSessionInstance.setCurrentlyConsultingBillSession(currentSession);
        if (selectedSessionInstance.getNextInLineBillSession() != null && selectedSessionInstance.getNextInLineBillSession().equals(currentSession)) {
            selectedSessionInstance.setNextInLineBillSession(null);
        }
        if (selectedSessionInstance.getLastCompletedBillSession() != null && selectedSessionInstance.getLastCompletedBillSession().equals(currentSession)) {
            selectedSessionInstance.setLastCompletedBillSession(null);
        }
        sessionInstanceFacade.edit(selectedSessionInstance);
    }

    public void markSelectedAsLastCompleted() {
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("Select a session to mark as last completed.");
            return;
        }
        BillSession lastCompletedSession = selectedBillSession;
        billSessionFacade.edit(lastCompletedSession);

        selectedSessionInstance.setLastCompletedBillSession(lastCompletedSession);
        if (selectedSessionInstance.getCurrentlyConsultingBillSession() != null && selectedSessionInstance.getCurrentlyConsultingBillSession().equals(lastCompletedSession)) {
            selectedSessionInstance.setCurrentlyConsultingBillSession(null);
        }
        if (selectedSessionInstance.getNextInLineBillSession() != null && selectedSessionInstance.getNextInLineBillSession().equals(lastCompletedSession)) {
            selectedSessionInstance.setNextInLineBillSession(null);
        }
        sessionInstanceFacade.edit(selectedSessionInstance);
    }

    public void markSelectedAsNextInLine() {
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("Select a session to mark as next in line.");
            return;
        }
        BillSession nextInLineSession = selectedBillSession;
        billSessionFacade.edit(nextInLineSession);

        selectedSessionInstance.setNextInLineBillSession(nextInLineSession);
        if (selectedSessionInstance.getCurrentlyConsultingBillSession() != null && selectedSessionInstance.getCurrentlyConsultingBillSession().equals(nextInLineSession)) {
            selectedSessionInstance.setCurrentlyConsultingBillSession(null);
        }
        if (selectedSessionInstance.getLastCompletedBillSession() != null && selectedSessionInstance.getLastCompletedBillSession().equals(nextInLineSession)) {
            selectedSessionInstance.setLastCompletedBillSession(null);
        }
        sessionInstanceFacade.edit(selectedSessionInstance);
    }

    public void resetAndSetSelectedAsCurrentlyConsulted() {
        if (selectedBillSession == null) {
            // Handle the case where no session is selected
            return;
        }

        BillSession nextInLineSession = null;
        BillSession lastCompletedSession = null;

        // Reset the status for all sessions and identify nextInLine and lastCompleted sessions
        for (int i = 0; i < billSessions.size(); i++) {
            BillSession bs = billSessions.get(i);
            bs.setNextInLine(false);
            bs.setCurrentlyConsulted(false);

            // Do not reset the completed status; determine nextInLine and lastCompleted sessions instead
            // If selectedBillSession is found, look for the next session that is not completed to be the nextInLine
            if (selectedBillSession.equals(bs) && i + 1 < billSessions.size()) {
                for (int j = i + 1; j < billSessions.size(); j++) {
                    if (!billSessions.get(j).isCompleted()) {
                        nextInLineSession = billSessions.get(j);
                        break;
                    }
                }
            }
            // Find the last completed session before the selectedBillSession
            if (selectedBillSession.equals(bs) && i - 1 >= 0) {
                for (int k = i - 1; k >= 0; k--) {
                    if (Boolean.TRUE.equals(billSessions.get(k).isCompleted())) {
                        lastCompletedSession = billSessions.get(k);
                        break;
                    }
                }
            }

            billSessionFacade.edit(bs);
        }

        // Set the selectedBillSession as currentlyConsulted
        selectedBillSession.setCurrentlyConsulted(true);
        billSessionFacade.edit(selectedBillSession);

        // Update the SessionInstance with nextInLine and lastCompleted sessions
        selectedSessionInstance.setCurrentlyConsultingBillSession(selectedBillSession);
        selectedSessionInstance.setLastCompletedBillSession(lastCompletedSession);
        selectedSessionInstance.setNextInLineBillSession(nextInLineSession);

        // If there's a nextInLine session, mark it accordingly
        if (nextInLineSession != null) {
            nextInLineSession.setNextInLine(true);
            billSessionFacade.edit(nextInLineSession);
        }
        // Similarly, update lastCompletedSession if necessary
        if (lastCompletedSession != null) {
            lastCompletedSession.setCompleted(true); // Ensure it's marked as completed
            billSessionFacade.edit(lastCompletedSession);
        }

        sessionInstanceFacade.edit(selectedSessionInstance);
    }

    public void fillSessionActivities() {
        if (selectedSessionInstance == null) {
            return;
        } else {
            if (selectedSessionInstance.getOriginatingSession().getActivities() == null || selectedSessionInstance.getOriginatingSession().getActivities().trim().equals("")) {
                return;
            }
            selectedAppointmentActivities = appointmentActivityController.findActivitiesByCodesOrNames(selectedSessionInstance.getOriginatingSession().getActivities());
        }
    }

    public boolean getActivityStatus(AppointmentActivity activity, BillSession selectedBillSession) {
        if (activity == null) {
            JsfUtil.addErrorMessage("No Activity Selected");
            return false;
        }
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("No Session Selected");
            return false;
        }
        SessionInstanceActivity sia = sessionInstanceActivityController.findSessionInstanceActivity(selectedBillSession.getSessionInstance(), activity, selectedBillSession);
        if (sia == null) {
            return false;
        }

        if (sia.getActivityCompleted() == null) {
            return false;
        }
        return sia.getActivityCompleted();
    }

    public void markActivity(AppointmentActivity activity, BillSession selectedBillSession) {
        if (activity == null) {
            JsfUtil.addErrorMessage("No Activity Selected");
            return;
        }
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("No Session Selected");
            return;
        }
        SessionInstanceActivity sia = sessionInstanceActivityController.findSessionInstanceActivity(selectedBillSession.getSessionInstance(), activity, selectedBillSession);
        if (sia == null) {
            sia = new SessionInstanceActivity();
            sia.setBillSession(selectedBillSession);
            sia.setSessionInstance(selectedBillSession.getSessionInstance());
            sia.setAppointmentActivity(activity);
        }
        sia.setActivityCompleted(true);
        sessionInstanceActivityController.save(sia);
        JsfUtil.addSuccessMessage("Marked");
    }

    public void unmarkActivity(AppointmentActivity activity, BillSession session) {
        if (activity == null) {
            JsfUtil.addErrorMessage("No Activity Selected");
            return;
        }
        if (session == null) {
            JsfUtil.addErrorMessage("No Session Selected");
            return;
        }
        SessionInstanceActivity sia = sessionInstanceActivityController.findSessionInstanceActivity(session.getSessionInstance(), activity, session);
        if (sia == null) {
            sia = new SessionInstanceActivity();
            sia.setBillSession(selectedBillSession);
            sia.setSessionInstance(selectedBillSession.getSessionInstance());
            sia.setAppointmentActivity(activity);
        }
        sia.setActivityCompleted(false);
        sessionInstanceActivityController.save(sia);
        JsfUtil.addSuccessMessage("Un Marked");
    }

    public String navigateToListSessionInstanceActivities(AppointmentActivity activity, SessionInstance instance) {
        if (activity == null) {
            JsfUtil.addErrorMessage("No Activity Selected");
            return null;
        }
        if (instance == null) {
            JsfUtil.addErrorMessage("No Instance is Selected");
            return null;
        }
        appointmentActivity = activity;
        return "/channel/channel_session_activities?faces-redirect=true";
    }

    public void fillBillSessions() {
        selectedBillSession = null;
        BillType[] billTypes = {
            BillType.ChannelAgent,
            BillType.ChannelCash,
            BillType.ChannelOnCall,
            BillType.ChannelStaff,
            BillType.ChannelCredit
        };
        List<BillType> bts = Arrays.asList(billTypes);
        String sql = "Select bs "
                + " From BillSession bs "
                + " where bs.retired=false"
                + " and bs.bill.billType in :bts"
                + " and type(bs.bill)=:class "
                + " and bs.sessionInstance=:ss "
                + " order by bs.serialNo ";
        HashMap<String, Object> hh = new HashMap<>();
        hh.put("bts", bts);
        hh.put("class", BilledBill.class);
        hh.put("ss", getSelectedSessionInstance());
        billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);

        // Initialize counts
        long bookedPatientCount = 0;
        long paidPatientCount = 0;
        long completedPatientCount = 0;
        long cancelPatientCount = 0;
        long refundedPatientCount = 0;
        long onCallPatientCount = 0;

        if (billSessions == null) {
            selectedSessionInstance.setBookedPatientCount(0l);
            selectedSessionInstance.setPaidPatientCount(0l);
            selectedSessionInstance.setCompletedPatientCount(0l);
            selectedSessionInstance.setRemainingPatientCount(0l);
            sessionInstanceController.save(selectedSessionInstance);
            return;
        }

        // Loop through billSessions to calculate counts
        for (BillSession bs : billSessions) {
            if (bs != null) {
                bookedPatientCount++; // Always increment if bs is not null

                // Additional check for completion status
                try {
                    if (bs.isCompleted()) {
                        completedPatientCount++;
                    }
                } catch (NullPointerException npe) {
                    // Log or handle the fact that there was an NPE checking completion status

                }

                // Additional check for paid status
                try {
                    if (bs.getPaidBillSession() != null) {
                        paidPatientCount++;
                    }
                } catch (NullPointerException npe) {
                    // Log or handle the fact that there was an NPE checking paid status

                }
                // Additional check for cancel status
                try {
                    if (bs.getBill().isCancelled()) {
                        cancelPatientCount++;
                    }
                } catch (NullPointerException npe) {
                    // Log or handle the fact that there was an NPE checking paid status

                }

                // Additional check for refund status
                try {
                    if (bs.getBill().isRefunded()) {
                        refundedPatientCount++;
                    }
                } catch (NullPointerException npe) {
                    // Log or handle the fact that there was an NPE checking paid status

                }

                // Additional check for Oncall status
                try {
                    if (bs.getPaidBillSession() == null && !bs.getBill().isCancelled()) {
                        onCallPatientCount++;
                    }
                } catch (NullPointerException npe) {
                    // Log or handle the fact that there was an NPE checking paid status

                }
            }
        }

        // Set calculated counts to selectedSessionInstance
        selectedSessionInstance.setBookedPatientCount(bookedPatientCount);
        selectedSessionInstance.setPaidPatientCount(paidPatientCount);
        selectedSessionInstance.setCompletedPatientCount(completedPatientCount);
        selectedSessionInstance.setCancelPatientCount(cancelPatientCount);
        selectedSessionInstance.setRefundedPatientCount(refundedPatientCount);
        selectedSessionInstance.setOnCallPatientCount(onCallPatientCount);

        // Assuming remainingPatientCount is calculated as booked - completed
        selectedSessionInstance.setRemainingPatientCount(bookedPatientCount - completedPatientCount);
        sessionInstanceController.save(selectedSessionInstance);
    }

    private boolean errorCheckForAddingNewBooking() {
        if (getSelectedSessionInstance() == null) {
            errorText = "Please Select Specility and Doctor.";
            JsfUtil.addErrorMessage("Please Select Specility and Doctor.");
            return true;
        }

        if (getSelectedSessionInstance().isDeactivated()) {
            errorText = "******** Doctor Leave day Can't Channel ********";
            JsfUtil.addErrorMessage("Doctor Leave day Can't Channel.");
            return true;
        }

        if (getSelectedSessionInstance().getOriginatingSession() == null) {
            errorText = "Please Select Session.";
            JsfUtil.addErrorMessage("Please Select Session");
            return true;
        }

        if (getPatient().getPerson().getName() == null || getPatient().getPerson().getName().trim().equals("")) {
            errorText = "Can not bill without Patient.";
            JsfUtil.addErrorMessage("Can't Settle Without Patient.");
            return true;
        }
        if ((getPatient().getPerson().getPhone() == null || getPatient().getPerson().getPhone().trim().equals("")) && !getSessionController().getInstitutionPreference().isChannelSettleWithoutPatientPhoneNumber()) {
            errorText = "Can not bill without Patient Contact Number.";
            JsfUtil.addErrorMessage("Can't Settle Without Patient Contact Number.");
            return true;
        }

        if (paymentMethod == null) {
            errorText = "Please select Paymentmethod";
            JsfUtil.addErrorMessage("Please select Paymentmethod");
            return true;
        }

        if (paymentMethod == PaymentMethod.Agent) {
            if (institution == null) {
                errorText = "Please select Agency";
                JsfUtil.addErrorMessage("Please select Agency");
                return true;
            }

            if (institution.getBallance() - getSelectedSessionInstance().getOriginatingSession().getTotal() < 0 - institution.getAllowedCredit()) {
                errorText = "Agency Balance is Not Enough";
                JsfUtil.addErrorMessage("Agency Balance is Not Enough");
                return true;
            }
            if (agentReferenceBookController.checkAgentReferenceNumber(getAgentRefNo()) && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                errorText = "Invaild Reference Number.";
                JsfUtil.addErrorMessage("Invaild Reference Number.");
                return true;
            }
            if (agentReferenceBookController.checkAgentReferenceNumberAlredyExsist(getAgentRefNo(), institution, BillType.ChannelAgent, PaymentMethod.Agent) && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                errorText = "This Reference Number( " + getAgentRefNo() + " ) is alredy Given.";
                JsfUtil.addErrorMessage("This Reference Number is alredy Given.");
                setAgentRefNo("");
                return true;
            }
            if (agentReferenceBookController.checkAgentReferenceNumber(institution, getAgentRefNo()) && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                errorText = "This Reference Number is Blocked Or This channel Book is Not Issued.";
                JsfUtil.addErrorMessage("This Reference Number is Blocked Or This channel Book is Not Issued.");
                return true;
            }
        }
        if (paymentMethod == PaymentMethod.Staff) {
            if (toStaff == null) {
                errorText = "Please Select Staff.";
                JsfUtil.addErrorMessage("Please Select Staff.");
                return true;
            }
        }
        ////System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        if (institution != null) {
            if (getAgentRefNo().trim().isEmpty() && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                errorText = "Please Enter Agent Ref No";
                JsfUtil.addErrorMessage("Please Enter Agent Ref No.");
                return true;
            }
        }
        ////System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        if (getSelectedSessionInstance().getOriginatingSession().getMaxNo() != 0.0 && getSelectedSessionInstance().getTransDisplayCountWithoutCancelRefund() >= getSelectedSessionInstance().getOriginatingSession().getMaxNo()) {
            errorText = "No Space to Book.";
            JsfUtil.addErrorMessage("No Space to Book");
            return true;
        }

        ////System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        return false;
    }

    public void fillAbsentBillSessions(SelectEvent event) {
        selectedBillSession = null;
        selectedServiceSession = ((ServiceSession) event.getObject());

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff, BillType.ChannelCredit};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false"
                + " and bs.serviceSession=:ss "
                + " and bs.bill.billType in :bt "
                + " and bs.absent=true "
                + " and type(bs.bill)=:class "
                + " and bs.sessionDate= :ssDate "
                + " order by bs.serialNo ";
        HashMap hh = new HashMap();
        hh.put("bt", bts);
        hh
                .put("class", BilledBill.class
                );
        hh.put("ssDate", getSelectedServiceSession().getSessionAt());
        hh.put("ss", getSelectedServiceSession());
        billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);
        //absentCount=billSessions.size();

    }

    public String paySelectedDoctor() {
        if (getSpeciality() == null) {
            JsfUtil.addErrorMessage("Please Select Specility And Staff");
            return "";
        }
        if (getStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Staff");
            return "";
        }
        channelStaffPaymentBillController.setSpeciality(getSpeciality());
        channelStaffPaymentBillController.setCurrentStaff(getStaff());
        channelStaffPaymentBillController.setConsiderDate(true);
        channelStaffPaymentBillController.calculateDueFees();

        return "/channel/channel_payment_staff_bill?faces-redirect=true";

    }

    public String paySelectedSession() {
        if (getSelectedSessionInstance() == null) {
            JsfUtil.addErrorMessage("Please Select Session Instance");
            return "";
        }
        if (selectedSessionInstance.getOriginatingSession() == null) {
            JsfUtil.addErrorMessage("Please Select Session");
            return "";
        }
        if (selectedSessionInstance.getOriginatingSession().getStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Staff");
            return "";
        }
        if (selectedSessionInstance.getOriginatingSession().getStaff().getSpeciality() == null) {
            JsfUtil.addErrorMessage("Please Select Speciality");
            return "";
        }
        channelStaffPaymentBillController.setSessionInstance(selectedSessionInstance);
        channelStaffPaymentBillController.calculateSessionDueFees();
        return "/channel/channel_payment_session?faces-redirect=true";

    }

    public String paySelectedSessionByDates() {
        if (getSelectedSessionInstance() == null) {
            JsfUtil.addErrorMessage("Please Select Session Instance");
            return "";
        }
        if (selectedSessionInstance.getOriginatingSession() == null) {
            JsfUtil.addErrorMessage("Please Select Session");
            return "";
        }
        if (selectedSessionInstance.getOriginatingSession().getStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Staff");
            return "";
        }
        if (selectedSessionInstance.getOriginatingSession().getStaff().getSpeciality() == null) {
            JsfUtil.addErrorMessage("Please Select Speciality");
            return "";
        }
        channelStaffPaymentBillController.setSessionInstance(selectedSessionInstance);
        channelStaffPaymentBillController.calculateSessionDueFees();
        return "/channel/channel_payment_session_by_dates?faces-redirect=true";

    }

    private Bill saveBilledBill(boolean forReservedNumbers) {
        Bill savingBill = createBill();
        BillItem savingBillItem = createSessionItem(savingBill);

        PriceMatrix priceMatrix;
        List<BillItem> additionalBillItems = new ArrayList<>();
        if (!getItemsAddedToBooking().isEmpty()) {
            for (Item ai : itemsAddedToBooking) {
                BillItem aBillItem = createAdditionalItem(savingBill, ai);
                additionalBillItems.add(aBillItem);
            }
        }
        BillSession savingBillSession;

        savingBillSession = createBillSession(savingBill, savingBillItem, forReservedNumbers);

        List<BillFee> savingBillFees = new ArrayList<>();

        priceMatrix = priceMatrixController.fetchChannellingMemberShipDiscount(paymentMethod, paymentScheme, selectedSessionInstance.getOriginatingSession().getCategory());
        System.out.println("priceMatrix = " + priceMatrix);

        List<BillFee> savingBillFeesFromSession = createBillFeeForSessions(savingBill, savingBillItem, false, priceMatrix);

        List<BillFee> savingBillFeesFromAdditionalItems = new ArrayList<>();
        if (!additionalBillItems.isEmpty()) {
            for (BillItem abi : additionalBillItems) {
                List<BillFee> blf = createBillFeeForSessions(savingBill, abi, true, priceMatrix);
                for (BillFee bf : blf) {
                    savingBillFeesFromAdditionalItems.add(bf);
                }
            }
        }

        if (savingBillFeesFromSession != null) {
            savingBillFees.addAll(savingBillFeesFromSession);
        }
        if (!savingBillFeesFromAdditionalItems.isEmpty()) {
            savingBillFees.addAll(savingBillFeesFromAdditionalItems);
        }

        List<BillItem> savingBillItems = new ArrayList<>();
        savingBillItems.add(savingBillItem);
        getBillItemFacade().edit(savingBillItem);
        savingBillItem.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBillItem));
        savingBillItem.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBillItem));
        savingBillItem.setBillSession(savingBillSession);
        getBillSessionFacade().edit(savingBillSession);
        savingBill.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBill));
        savingBill.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBill));
        savingBill.setSingleBillItem(savingBillItem);
        savingBill.setSingleBillSession(savingBillSession);
        savingBill.setBillItems(savingBillItems);
        savingBill.setBillFees(savingBillFees);
        savingBill.setCashPaid(cashPaid);
        savingBill.setCashBalance(cashBalance);
        if (savingBill.getBillType() == BillType.ChannelAgent) {
            updateBallance(savingBill.getCreditCompany(), 0 - savingBill.getNetTotal(), HistoryType.ChannelBooking, savingBill, savingBillItem, savingBillSession, savingBillItem.getAgentRefNo());
            savingBill.setBalance(0.0);
            savingBillSession.setPaidBillSession(savingBillSession);
        } else if (savingBill.getBillType() == BillType.ChannelCash) {
            savingBill.setBalance(0.0);
            savingBillSession.setPaidBillSession(savingBillSession);
        } else if (savingBill.getBillType() == BillType.ChannelOnCall) {
            savingBill.setBalance(savingBill.getNetTotal());
        } else if (savingBill.getBillType() == BillType.ChannelStaff) {
            savingBill.setBalance(0.0);
            savingBillSession.setPaidBillSession(savingBillSession);
        }

        savingBill.setSingleBillItem(savingBillItem);
        savingBill.setSingleBillSession(savingBillSession);
        if (referredBy != null) {
            savingBill.setReferredBy(referredBy);
        }
        if (referredByInstitution != null) {
            savingBill.setReferenceInstitution(referredByInstitution);
        }
        if (collectingCentre != null) {
            savingBill.setCollectingCentre(collectingCentre);
        }

        calculateBillTotalsFromBillFees(savingBill, savingBillFees);

        getBillFacade().edit(savingBill);
        getBillSessionFacade().edit(savingBillSession);
        return savingBill;
    }

    private BillSession saveBilledBillForPatientPortal() {
        paymentMethod = PaymentMethod.OnlineSettlement;
        paymentScheme = null;
        Bill savingBill = new BilledBill();
        savingBill.setStaff(getSelectedSessionInstance().getOriginatingSession().getStaff());
        savingBill.setToStaff(toStaff);
        savingBill.setAppointmentAt(getSelectedSessionInstance().getSessionDate());
        savingBill.setTotal(getSelectedSessionInstance().getOriginatingSession().getTotal());
        savingBill.setNetTotal(getSelectedSessionInstance().getOriginatingSession().getTotal());
        savingBill.setPaymentMethod(paymentMethod);
        savingBill.setPatient(getPatient());
        savingBill.setToDepartment(getSelectedSessionInstance().getDepartment());
        savingBill.setToInstitution(getSelectedSessionInstance().getInstitution());
        savingBill.setDepartment(selectedSessionInstance.getDepartment());
        savingBill.setInstitution(selectedSessionInstance.getInstitution());
        savingBill.setBillType(BillType.ChannelOnCall);
        savingBill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT);
        savingBill.setRetired(true);
        String deptId = generateBillNumberDeptIdForOnlinePayment(savingBill);

        if (deptId.equals("")) {
            return null;
        }
        savingBill.setDeptId(deptId);
        savingBill.setInsId(deptId);
        savingBill.setPaidAmount(0.0);
        savingBill.setBalance(getSelectedSessionInstance().getOriginatingSession().getTotal());
        savingBill.setBillDate(new Date());
        savingBill.setBillTime(new Date());
        savingBill.setCreatedAt(new Date());
        getBillFacade().create(savingBill);

        BillItem savingBillItem = new BillItem();
        savingBillItem.setAdjustedValue(0.0);
        savingBillItem.setAgentRefNo(agentRefNo);
        savingBillItem.setBill(savingBill);
        savingBillItem.setBillTime(new Date());
        savingBillItem.setCreatedAt(new Date());
        savingBillItem.setGrossValue(getSelectedSessionInstance().getOriginatingSession().getTotal());
        savingBillItem.setItem(getSelectedSessionInstance().getOriginatingSession());
        savingBillItem.setNetRate(getSelectedSessionInstance().getOriginatingSession().getTotal());
        savingBillItem.setNetValue(getSelectedSessionInstance().getOriginatingSession().getTotal());
        savingBillItem.setQty(1.0);
        savingBillItem.setRate(getSelectedSessionInstance().getOriginatingSession().getTotal());
        savingBillItem.setSessionDate(getSelectedSessionInstance().getSessionAt());
        billItemFacade.create(savingBillItem);

        BillItem additionalBillItem = createAdditionalItem(savingBill, itemToAddToBooking);

        if (itemToAddToBooking != null) {
            BillItem bi = new BillItem();
            bi.setAdjustedValue(0.0);
            bi.setAgentRefNo(agentRefNo);
            bi.setBill(savingBill);
            bi.setBillTime(new Date());
            bi.setCreatedAt(new Date());
            bi.setGrossValue(itemToAddToBooking.getDblValue());
            bi.setItem(itemToAddToBooking);
            bi.setNetRate(itemToAddToBooking.getDblValue());
            bi.setNetValue(itemToAddToBooking.getDblValue());
            bi.setQty(1.0);
            bi.setRate(itemToAddToBooking.getDblValue());
            bi.setSessionDate(getSelectedSessionInstance().getSessionAt());
            billItemFacade.create(bi);
        }

        BillSession savingBillSession;
        savingBillSession = createBillSession(savingBill, savingBillItem, false);

        BillSession bs = new BillSession();
        bs.setAbsent(false);
        bs.setBill(savingBill);
        bs.setBillItem(savingBillItem);
        bs.setCreatedAt(new Date());
        bs.setDepartment(getSelectedSessionInstance().getOriginatingSession().getDepartment());
        bs.setInstitution(getSelectedSessionInstance().getOriginatingSession().getInstitution());
        bs.setItem(getSelectedSessionInstance().getOriginatingSession());
        bs.setSessionInstance(getSelectedSessionInstance());
        bs.setSessionDate(getSelectedSessionInstance().getSessionDate());
        bs.setSessionTime(getSelectedSessionInstance().getSessionTime());
        bs.setStaff(getSelectedSessionInstance().getStaff());

        List<Integer> reservedNumbers = CommonFunctions.convertStringToIntegerList(getSelectedSessionInstance().getOriginatingSession().getReserveNumbers());
        Integer count;

        count = serviceSessionBean.getNextNonReservedSerialNumber(getSelectedSessionInstance(), reservedNumbers);

        if (count != null) {
            bs.setSerialNo(count);
        } else {
            bs.setSerialNo(1);
        }

        getBillSessionFacade().create(bs);

        List<BillFee> savingBillFees = new ArrayList<>();

        List<BillFee> savingBillFeesFromSession = createBillFeeForSessionsForPatientPortal(savingBill, savingBillItem, false);
        List<BillFee> savingBillFeesFromAdditionalItem = createBillFeeForSessionsForPatientPortal(savingBill, additionalBillItem, true);

        if (savingBillFeesFromSession != null) {
            savingBillFees.addAll(savingBillFeesFromSession);
        }
        if (savingBillFeesFromAdditionalItem != null) {
            savingBillFees.addAll(savingBillFeesFromAdditionalItem);
        }

        List<BillItem> savingBillItems = new ArrayList<>();
        savingBillItems.add(savingBillItem);
        getBillItemFacade().edit(savingBillItem);
        savingBillItem.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBillItem));
        savingBillItem.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBillItem));
        savingBillItem.setBillSession(savingBillSession);
        getBillSessionFacade().edit(savingBillSession);
        savingBill.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBill));
        savingBill.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBill));
        savingBill.setSingleBillItem(savingBillItem);
        savingBill.setSingleBillSession(savingBillSession);
        savingBill.setBillItems(savingBillItems);
        savingBill.setBillFees(savingBillFees);
        savingBill.setBalance(savingBill.getNetTotal());
        savingBill.setSingleBillItem(savingBillItem);
        savingBill.setSingleBillSession(savingBillSession);

        calculateBillTotalsFromBillFees(savingBill, savingBillFees);

        getBillFacade().edit(savingBill);
        getBillSessionFacade().edit(savingBillSession);
        return savingBillSession;
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
                        break;
                    case Cheque:
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        break;
                    case ewallet:

                    case Agent:
                    case Credit:
                    case PatientDeposit:
                    case Slip:
                        p.setPaidValue(cd.getPaymentMethodData().getSlip().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
                    case OnCall:
                    case OnlineSettlement:
                    case Staff:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffCredit().getToStaff() != null) {
                            staffBean.updateStaffCredit(cd.getPaymentMethodData().getStaffCredit().getToStaff(), cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
                        }
                    case YouOweMe:
                    case MultiplePaymentMethods:
                }

                paymentFacade.create(p);
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
                    break;
                case Cheque:
                    p.setChequeDate(paymentMethodData.getCheque().getDate());
                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
                    break;
                case Cash:
                    break;
                case ewallet:
                    break;
                case Agent:
                    p.setInstitution(institution);
                case Credit:
                    break;
                case PatientDeposit:
                    break;
                case Slip:
                    p.setBank(paymentMethodData.getSlip().getInstitution());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
                case OnCall:
                    break;
                case OnlineSettlement:
                    break;
                case Staff:
                    break;
                case YouOweMe:
                    break;
                case MultiplePaymentMethods:
                    break;
            }

            p.setPaidValue(p.getBill().getNetTotal());
            paymentFacade.create(p);

            ps.add(p);
        }
        return ps;
    }

    public void updateBallance(Institution ins, double transactionValue, HistoryType historyType, Bill bill, BillItem billItem, BillSession selectedBillSession, String refNo) {
        AgentHistory agentHistory = new AgentHistory();
        agentHistory.setCreatedAt(new Date());
        agentHistory.setCreater(getSessionController().getLoggedUser());
        agentHistory.setBill(bill);
        agentHistory.setBillItem(billItem);
        agentHistory.setBillSession(selectedBillSession);
        agentHistory.setBeforeBallance(ins.getBallance());
        agentHistory.setTransactionValue(transactionValue);
        agentHistory.setReferenceNumber(refNo);
        agentHistory.setHistoryType(historyType);
        agentHistoryFacade.create(agentHistory);

        ins.setBallance(ins.getBallance() + transactionValue);
        getInstitutionFacade().edit(ins);

    }

    public void createBillfees(SelectEvent event) {
        BillSession bs = ((BillSession) event.getObject());
        String sql;
        HashMap hm = new HashMap();
        sql = "Select bf From BillFee bf where bf.retired=false"
                + " and bf.billItem=:bt ";
        hm.put("bt", bs.getBillItem());

        listBillFees = billFeeFacade.findByJpql(sql, hm);
        selectedBillSession = bs;

        for (BillFee bf : selectedBillSession.getBill().getBillFees()) {
            if (bf.getFee().getFeeType() == FeeType.Staff && getSessionController().getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna) {
                bf.setTmpChangedValue(bf.getFeeValue());
            }
        }
    }

    public List<ItemFee> findServiceSessionFees(ServiceSession ss) {
        String sql;
        Map m = new HashMap();
        sql = "Select f from ItemFee f "
                + " where f.retired=false "
                + " and (f.serviceSession=:ses "
                + " or f.item=:ses )"
                + " order by f.id";
        m.put("ses", ss);
        List<ItemFee> tfs = itemFeeFacade.findByJpql(sql, m);
        return tfs;
    }

    public List<ItemFee> findItemFees(Item i) {
        String sql;
        Map m = new HashMap();
        sql = "Select f from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:i "
                + " order by f.id";
        m.put("i", i);
        return itemFeeFacade.findByJpql(sql, m);
    }

    private List<BillFee> createBillFeeForSessions(Bill bill, BillItem billItem, boolean thisIsAnAdditionalFee, PriceMatrix priceMatrix) {
        List<BillFee> billFeeList = new ArrayList<>();
        double tmpTotal = 0;
        double tmpDiscount = 0;
        double tmpGrossTotal = 0.0;

        List<ItemFee> sessionsFees = null;

        if (thisIsAnAdditionalFee) {
            sessionsFees = findItemFees(billItem.getItem());
        } else {
            if (billItem.getItem() instanceof ServiceSession) {
                sessionsFees = findServiceSessionFees((ServiceSession) billItem.getItem());
            }
        }

        if (sessionsFees == null) {
            return billFeeList;
        }
        for (ItemFee f : sessionsFees) {
            if (paymentMethod != PaymentMethod.Agent) {
                if (f.getFeeType() == FeeType.OtherInstitution) {
                    continue;
                }
            }
            if (paymentMethod != PaymentMethod.OnCall) {
                if (f.getFeeType() == FeeType.OwnInstitution && f.getName().equalsIgnoreCase("On-Call Fee")) {
                    continue;
                }
            }
            BillFee bf = new BillFee();
            bf.setBill(bill);
            bf.setBillItem(billItem);
            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());
            if (f.getFeeType() == FeeType.OwnInstitution) {
                bf.setInstitution(f.getInstitution());
                bf.setDepartment(f.getDepartment());
            } else if (f.getFeeType() == FeeType.OtherInstitution) {
                bf.setInstitution(institution);
            } else if (f.getFeeType() == FeeType.Staff) {
                bf.setSpeciality(f.getSpeciality());
                bf.setStaff(f.getStaff());
            }

            bf.setFee(f);
            bf.setFeeAt(new Date());
            bf.setOrderNo(0);
            bf.setPatient(bill.getPatient());

            if (bf.getPatienEncounter() != null) {
                bf.setPatienEncounter(bill.getPatientEncounter());
            }

            bf.setPatient(bill.getPatient());

            if (f.getFeeType() == FeeType.Staff) {
                bf.setStaff(f.getStaff());
            }

            if (f.getFeeType() == FeeType.OwnInstitution) {
                bf.setInstitution(sessionController.getInstitution());
            }

            double d = 0;
            if (foriegn) {
                bf.setFeeValue(f.getFfee());
                bf.setFeeGrossValue(f.getFfee());
            } else {
                bf.setFeeValue(f.getFee());
                bf.setFeeGrossValue(f.getFee());
            }

//            priceMatrix = priceMatrixController.fetchChannellingMemberShipDiscount(paymentMethod, paymentScheme, selectedSessionInstance.getOriginatingSession().getCategory());
            if (priceMatrix != null) {
                if (f.getFeeType() == FeeType.OwnInstitution) {
                    d = bf.getFeeValue() * (priceMatrix.getDiscountPercent() / 100);
                    bf.setFeeDiscount(d);
                } else if (f.getFeeType() == FeeType.Staff) {
                    bf.setFeeDiscount(0.0);
                } else {
                    bf.setFeeDiscount(0.0);
                }

                bf.setFeeGrossValue(bf.getFeeGrossValue());
                bf.setFeeValue(bf.getFeeGrossValue() - bf.getFeeDiscount());
                tmpDiscount += d;
            }

            tmpGrossTotal += bf.getFeeGrossValue();
            tmpTotal += bf.getFeeValue();
            billFeeFacade.create(bf);
            billFeeList.add(bf);
        }

        billItem.setDiscount(tmpDiscount);
        billItem.setNetValue(tmpTotal);
        getBillItemFacade().edit(billItem);

        return billFeeList;

    }

    private List<BillFee> createBillFeeForSessionsForPatientPortal(Bill bill, BillItem billItem, boolean thisIsAnAdditionalFee) {
        List<BillFee> billFeeList = new ArrayList<>();
        double tmpTotal = 0;
        double tmpDiscount = 0;
        double tmpGrossTotal = 0.0;
        List<ItemFee> sessionsFees = null;
        if (thisIsAnAdditionalFee) {
            sessionsFees = findItemFees(billItem.getItem());
        } else {
            if (billItem.getItem() instanceof ServiceSession) {
                sessionsFees = findServiceSessionFees((ServiceSession) billItem.getItem());
            }
        }
        if (sessionsFees == null) {
            return billFeeList;
        }
        for (ItemFee f : sessionsFees) {
            BillFee bf = new BillFee();
            bf.setBill(bill);
            bf.setBillItem(billItem);
            bf.setCreatedAt(new Date());
            if (f.getFeeType() == FeeType.OwnInstitution) {
                bf.setInstitution(f.getInstitution());
                bf.setDepartment(f.getDepartment());
            } else if (f.getFeeType() == FeeType.OtherInstitution) {
                bf.setInstitution(f.getInstitution());
            } else if (f.getFeeType() == FeeType.Staff) {
                bf.setSpeciality(f.getSpeciality());
                bf.setStaff(f.getStaff());
            }

            bf.setFee(f);
            bf.setFeeAt(new Date());
            bf.setFeeDiscount(0.0);
            bf.setOrderNo(0);
            bf.setPatient(bill.getPatient());

            bf.setPatient(bill.getPatient());

            if (f.getFeeType() == FeeType.Staff) {
                bf.setStaff(f.getStaff());
            }

            PaymentSchemeDiscount paymentSchemeDiscount = priceMatrixController.fetchPaymentSchemeDiscount(paymentScheme, paymentMethod);

            double d = 0;
            if (foriegn) {
                bf.setFeeValue(f.getFfee());
                bf.setFeeGrossValue(f.getFfee());
            } else {
                bf.setFeeValue(f.getFee());
                bf.setFeeGrossValue(f.getFee());
            }

            if (f.getFeeType() == FeeType.OwnInstitution && paymentSchemeDiscount != null) {
                d = bf.getFeeValue() * (paymentSchemeDiscount.getDiscountPercent() / 100);
                bf.setFeeDiscount(d);
                bf.setFeeGrossValue(bf.getFeeGrossValue());
                bf.setFeeValue(bf.getFeeGrossValue() - bf.getFeeDiscount());
                tmpDiscount += d;
            } else if (bill.getPatient().getPerson().getMembershipScheme() != null && f.getFeeType() == FeeType.OwnInstitution) {
//                MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(bill.getPatient());

                MembershipScheme membershipScheme = bill.getPatient().getPerson().getMembershipScheme();

                PriceMatrix priceMatrix = priceMatrixController.getChannellingDisCount(paymentMethod, membershipScheme, f.getDepartment());
//                priceMatrix.getDiscountPercent();
//                //System.out.println("priceMatrix.getDiscountPercent() = " + priceMatrix.getDiscountPercent());

                if (priceMatrix != null) {

                    d = bf.getFeeValue() * (priceMatrix.getDiscountPercent() / 100);
                    bf.setFeeDiscount(d);
                    bf.setFeeGrossValue(bf.getFeeGrossValue());
                    bf.setFeeValue(bf.getFeeGrossValue() - bf.getFeeDiscount());
                    tmpDiscount += d;
                }
            }

            tmpGrossTotal += bf.getFeeGrossValue();
            tmpTotal += bf.getFeeValue();
            billFeeFacade.create(bf);
            billFeeList.add(bf);
        }

        billItem.setDiscount(tmpDiscount);
        billItem.setNetValue(tmpTotal);
        getBillItemFacade().edit(billItem);

        return billFeeList;

    }

    private void calculateBillTotalsFromBillFees(Bill billToCaclculate, List<BillFee> billfeesAvailable) {
        System.out.println("calculateBillTotalsFromBillFees");
        System.out.println("billToCaclculate = " + billToCaclculate);
        System.out.println("billfeesAvailable = " + billfeesAvailable);
        double calculatingGrossBillTotal = 0.0;
        double calculatingNetBillTotal = 0.0;

        for (BillFee iteratingBillFee : billfeesAvailable) {
            System.out.println("iteratingBillFee = " + iteratingBillFee);
            if (iteratingBillFee.getFee() == null) {
                continue;
            }

            calculatingGrossBillTotal += iteratingBillFee.getFeeGrossValue();
            System.out.println("calculatingGrossBillTotal = " + calculatingGrossBillTotal);
            calculatingNetBillTotal += iteratingBillFee.getFeeValue();
            System.out.println("calculatingNetBillTotal = " + calculatingNetBillTotal);

        }
        billToCaclculate.setDiscount(calculatingGrossBillTotal - calculatingNetBillTotal);
        billToCaclculate.setNetTotal(calculatingNetBillTotal);
        billToCaclculate.setTotal(calculatingGrossBillTotal);
        getBillFacade().edit(billToCaclculate);
    }

    private void calculateBillTotalsFromBillFees(Bill billToCaclculate) {
        double calculatingGrossBillTotal = 0.0;
        double calculatingNetBillTotal = 0.0;
        List<BillFee> billfeesAvailable = billToCaclculate.getBillFees();
        if (billfeesAvailable == null || billfeesAvailable.isEmpty()) {
            billfeesAvailable = billBeanController.getBillFee(billToCaclculate);
        }
        if (billfeesAvailable == null || billfeesAvailable.isEmpty()) {
            billToCaclculate.setDiscount(0.0);
            billToCaclculate.setNetTotal(0.0);
            billToCaclculate.setTotal(0.0);
            getBillFacade().edit(billToCaclculate);
            return;
        }

        for (BillFee iteratingBillFee : billfeesAvailable) {
            if (iteratingBillFee.getFee() == null) {
                continue;
            }
            calculatingGrossBillTotal += iteratingBillFee.getFeeGrossValue();
            calculatingNetBillTotal += iteratingBillFee.getFeeValue();
        }
        billToCaclculate.setDiscount(calculatingGrossBillTotal - calculatingNetBillTotal);
        billToCaclculate.setNetTotal(calculatingNetBillTotal);
        billToCaclculate.setTotal(calculatingGrossBillTotal);
        getBillFacade().edit(billToCaclculate);
    }

    public void prepareBillSessionForReportLink() {
        selectedBillSession = null;
        if (encryptedBillSessionId == null) {
            return;
        }

        String decodedIdStr;
        try {
            decodedIdStr = URLDecoder.decode(encryptedBillSessionId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Handle the exception, possibly with logging
            return;
        }
        String securityKey = sessionController.getApplicationPreference().getEncrptionKey();
        if (securityKey == null || securityKey.trim().equals("")) {
            sessionController.getApplicationPreference().setEncrptionKey(securityController.generateRandomKey(10));
            sessionController.savePreferences(sessionController.getApplicationPreference());
        }

        String idStr = securityController.decryptAlphanumeric(encryptedBillSessionId, securityKey);

        if (idStr == null || idStr.trim().isEmpty()) {
            // Handle the situation where decryption returns null or an empty string
            return;
        }

        Long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            // Handle the exception, possibly with logging
            return;
        }

        selectedBillSession = billSessionFacade.find(id);

    }

    private Bill createBill() {
        Bill bill = new BilledBill();
        bill.setStaff(getSelectedSessionInstance().getOriginatingSession().getStaff());
        bill.setToStaff(toStaff);
        bill.setAppointmentAt(getSelectedSessionInstance().getSessionDate());
        bill.setTotal(getSelectedSessionInstance().getOriginatingSession().getTotal());
        bill.setNetTotal(getSelectedSessionInstance().getOriginatingSession().getTotal());
        bill.setPaymentMethod(paymentMethod);
        bill.setPatient(getPatient());
        switch (paymentMethod) {
            case OnCall:
                bill.setBillType(BillType.ChannelOnCall);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITHOUT_PAYMENT);
                break;

            case Cash:
                bill.setBillType(BillType.ChannelCash);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
                break;

            case Card:
                bill.setBillType(BillType.ChannelCash);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
                break;

            case Cheque:
                bill.setBillType(BillType.ChannelCash);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
                break;

            case Slip:
                bill.setBillType(BillType.ChannelCash);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
                break;
            case Agent:
                bill.setBillType(BillType.ChannelAgent);
                bill.setCreditCompany(institution);
                bill.setAgentRefNo(agentRefNo);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
                break;
            case Staff:
                bill.setBillType(BillType.ChannelStaff);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITHOUT_PAYMENT);
                break;
            case Credit:
                bill.setBillType(BillType.ChannelCredit);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITHOUT_PAYMENT);
                break;
            case OnlineSettlement:
                bill.setBillType(BillType.ChannelCash);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT_ONLINE);
                break;

            case MultiplePaymentMethods:
                bill.setBillType(BillType.ChannelCash);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
                break;
        }
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
        String deptId = generateBillNumberDeptId(bill);

        if (deptId.equals("")) {
            return null;
        }
        bill.setDeptId(deptId);
        bill.setInsId(deptId);

        if (bill.getBillType().getParent() == BillType.ChannelCashFlow) {
//            bill.setBookingId(billNumberBean.bookingIdGenerator(sessionController.getInstitution(), new BilledBill()));
            bill.setPaidAmount(getSelectedSessionInstance().getOriginatingSession().getTotal());
            bill.setPaidAt(new Date());
        }

        bill.setBillDate(new Date());
        bill.setBillTime(new Date());
        bill.setCreatedAt(new Date());
        bill.setCreater(getSessionController().getLoggedUser());
        bill.setDepartment(getSessionController().getDepartment());
        bill.setInstitution(sessionController.getInstitution());

        bill.setToDepartment(getSelectedSessionInstance().getDepartment());
        bill.setToInstitution(getSelectedSessionInstance().getInstitution());

        getBillFacade().create(bill);

        if (bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) {
            bill.setPaidBill(bill);
            getBillFacade().edit(bill);
        }
        return bill;
    }

    private String generateBillNumberDeptId(Bill bill) {
        String suffix = getSessionController().getDepartment().getDepartmentCode();
        BillClassType billClassType = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff, BillType.ChannelCredit};
        List<BillType> bts = Arrays.asList(billTypes);
        BillType billType = null;
        String deptId = null;
        if (bill instanceof BilledBill) {

            billClassType = BillClassType.BilledBill;
            if (bill.getBillType() == BillType.ChannelOnCall || bill.getBillType() == BillType.ChannelStaff) {
                billType = bill.getBillType();
                if (billType == BillType.ChannelOnCall) {
                    suffix += "BKONCALL";
                } else {
                    suffix += "BKSTAFF";
                }
                deptId = billNumberBean.departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), billType, billClassType, suffix);
            } else {
                suffix += "CHANN";
                deptId = billNumberBean.departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            suffix += "CHANNCAN";
            billClassType = BillClassType.CancelledBill;
            deptId = billNumberBean.departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            suffix += "CHANNREF";
            billClassType = BillClassType.RefundBill;
            deptId = billNumberBean.departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), bts, billClassType, suffix);
        }

        return deptId;
    }

    private String generateBillNumberDeptIdForOnlinePayment(Bill bill) {
        String suffix = bill.getDepartment().getDepartmentCode();
        BillClassType billClassType = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff, BillType.ChannelCredit};
        List<BillType> bts = Arrays.asList(billTypes);
        BillType billType = null;
        String deptId = null;
        if (bill instanceof BilledBill) {

            billClassType = BillClassType.BilledBill;
            if (bill.getBillType() == BillType.ChannelOnCall || bill.getBillType() == BillType.ChannelStaff) {
                billType = bill.getBillType();
                if (billType == BillType.ChannelOnCall) {
                    suffix += "BKONCALL";
                } else {
                    suffix += "BKSTAFF";
                }
                deptId = billNumberBean.departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), billType, billClassType, suffix);
            } else {
                suffix += "CHANN";
                deptId = billNumberBean.departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            suffix += "CHANNCAN";
            billClassType = BillClassType.CancelledBill;
            deptId = billNumberBean.departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            suffix += "CHANNREF";
            billClassType = BillClassType.RefundBill;
            deptId = billNumberBean.departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), bts, billClassType, suffix);
        }

        return deptId;
    }

    private BillItem createSessionItem(Bill bill) {
        BillItem bi = new BillItem();
        bi.setAdjustedValue(0.0);
        bi.setAgentRefNo(agentRefNo);
        bi.setBill(bill);
        bi.setBillTime(new Date());
        bi.setCreatedAt(new Date());
        bi.setCreater(getSessionController().getLoggedUser());
        bi.setGrossValue(getSelectedSessionInstance().getOriginatingSession().getTotal());
        bi.setItem(getSelectedSessionInstance().getOriginatingSession());
//        bi.setItem(getSelectedSessionInstance().getOriginatingSession());
        bi.setNetRate(getSelectedSessionInstance().getOriginatingSession().getTotal());
        bi.setNetValue(getSelectedSessionInstance().getOriginatingSession().getTotal());
        bi.setQty(1.0);
        bi.setRate(getSelectedSessionInstance().getOriginatingSession().getTotal());
        bi.setSessionDate(getSelectedSessionInstance().getSessionAt());

        billItemFacade.create(bi);
        return bi;
    }

    private BillItem createAdditionalItem(Bill bill, Item i) {
        BillItem bi = new BillItem();
        bi.setAdjustedValue(0.0);
        bi.setAgentRefNo(agentRefNo);
        bi.setBill(bill);
        bi.setBillTime(new Date());
        bi.setCreatedAt(new Date());
        bi.setCreater(getSessionController().getLoggedUser());
        if (i != null) {
            bi.setGrossValue(i.getDblValue());
            bi.setItem(i);
            bi.setNetRate(i.getDblValue());
            bi.setNetValue(i.getDblValue());
            bi.setQty(1.0);
            bi.setRate(i.getDblValue());
        }
        bi.setSessionDate(getSelectedSessionInstance().getSessionAt());
        billItemFacade.create(bi);
        return bi;
    }

    private BillSession createBillSession(Bill bill, BillItem billItem, boolean forReservedNumbers) {
        BillSession bs = new BillSession();
        bs.setAbsent(false);
        bs.setBill(bill);
        bs.setBillItem(billItem);
        bs.setCreatedAt(new Date());
        bs.setCreater(getSessionController().getLoggedUser());
        bs.setDepartment(getSelectedSessionInstance().getOriginatingSession().getDepartment());
        bs.setInstitution(getSelectedSessionInstance().getOriginatingSession().getInstitution());
        bs.setItem(getSelectedSessionInstance().getOriginatingSession());
//        bs.setPresent(true);
//        bs.setPresent(true);
//        bs.setItem(getSelectedSessionInstance().getOriginatingSession());

        bs.setServiceSession(getSelectedSessionInstance().getOriginatingSession());
        bs.setSessionInstance(getSelectedSessionInstance());
//        bs.setServiceSession(getSelectedSessionInstance().getOriginatingSession());
        bs.setSessionDate(getSelectedSessionInstance().getSessionDate());
        bs.setSessionTime(getSelectedSessionInstance().getSessionTime());
        bs.setStaff(getSelectedSessionInstance().getStaff());

        List<Integer> reservedNumbers = CommonFunctions.convertStringToIntegerList(getSelectedSessionInstance().getOriginatingSession().getReserveNumbers());
        Integer count;

        if (forReservedNumbers) {
            // Pass the selectedReservedBookingNumber to the service method
            count = serviceSessionBean.getNextAvailableReservedNumber(getSelectedSessionInstance(), reservedNumbers, selectedReserverdBookingNumber);
            if (count == null) {
                count = serviceSessionBean.getNextNonReservedSerialNumber(getSelectedSessionInstance(), reservedNumbers);
                JsfUtil.addErrorMessage("No reserved numbers available. Normal number is given");
            }
        } else {
            count = serviceSessionBean.getNextNonReservedSerialNumber(getSelectedSessionInstance(), reservedNumbers);
        }

        if (count != null) {
            bs.setSerialNo(count);
        } else {
            bs.setSerialNo(1);
        }

        getBillSessionFacade().create(bs);

        return bs;
    }

    private String generateBillNumberInsId(Bill bill) {
        String suffix = getSessionController().getInstitution().getInstitutionCode();
        BillClassType billClassType = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff, BillType.ChannelCredit};
        List<BillType> bts = Arrays.asList(billTypes);
        BillType billType = null;
        String insId = null;
        if (bill instanceof BilledBill) {

            billClassType = BillClassType.BilledBill;
            if (bill.getBillType() == BillType.ChannelOnCall || bill.getBillType() == BillType.ChannelStaff) {
                billType = bill.getBillType();
                if (billType == BillType.ChannelOnCall) {
                    suffix += "BKONCALL";
                } else {
                    suffix += "BKSTAFF";
                }
                insId = billNumberBean.institutionBillNumberGenerator(sessionController.getInstitution(), billType, billClassType, suffix);
            } else {
                suffix += "CHANN";
                insId = billNumberBean.institutionBillNumberGenerator(sessionController.getInstitution(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            suffix += "CHANNCAN";
            billClassType = BillClassType.CancelledBill;
            insId = billNumberBean.institutionBillNumberGenerator(sessionController.getInstitution(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            suffix += "CHANNREF";
            billClassType = BillClassType.RefundBill;
            insId = billNumberBean.institutionBillNumberGenerator(sessionController.getInstitution(), bts, billClassType, suffix);
        }

        return insId;
    }

    public void onEditItem(RowEditEvent event) {
        ServiceSession tmp = (ServiceSession) event.getObject();
        ServiceSession ss = getServiceSessionFacade().find(tmp.getId());
        if (ss.getMaxNo() != tmp.getMaxNo()) {
            tmp.setEditedAt(new Date());
            tmp.setEditer(getSessionController().getLoggedUser());
        }
        getServiceSessionFacade().edit(tmp);
    }

    public void listnerStaffListForRowSelect() {
        getSelectedConsultants();
        setStaff(null);
        sessionInstances = new ArrayList<>();
        selectedBillSession = null;
    }

    public void listnerStaffRowSelect() {
        getSelectedConsultants();
        setSelectedServiceSession(null);
        serviceSessionLeaveController.setSelectedServiceSession(null);
        serviceSessionLeaveController.setCurrentStaff(staff);
    }

    public void listnerSessionRowSelect() {
        if (sessionInstances == null) {
            selectedServiceSession = null;
            return;
        }
        for (SessionInstance ss : sessionInstances) {
            if (ss.getSessionText().toLowerCase().contains(selectTextSession.toLowerCase())) {
                selectedSessionInstance = ss;
            }
        }
    }

    public void listnerStaffListForSpecilitySelectedText() {
        if (doctorSpecialityController.getSelectedItems().size() > 0) {
            setSpeciality(doctorSpecialityController.getSelectedItems().get(0));
            listnerStaffListForRowSelect();
        }
    }

    public void setBillSessions(List<BillSession> billSessions) {
        this.billSessions = billSessions;
    }

    @Deprecated
    public ServiceSession getSelectedServiceSession() {
        return selectedServiceSession;
    }

    @Deprecated
    public void setSelectedServiceSession(ServiceSession selectedServiceSession) {
        this.selectedServiceSession = selectedServiceSession;

    }

    public void makeBillSessionNull() {
        billSessions = null;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
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

    public void createNewChannelBooking() {
        patient = new Patient();
        selectedBillSession = new BillSession();
        Bill b = new BilledBill();
        b.setPatient(patient);
        selectedBillSession.setBill(b);
    }

    public BillSession getSelectedBillSession() {
        if (selectedBillSession == null) {
            createNewChannelBooking();
        }
        return selectedBillSession;
    }

    public void setSelectedBillSession(BillSession selectedBillSession) {
        this.selectedBillSession = selectedBillSession;
        if (selectedBillSession != null) {
            Bill bill = selectedBillSession.getBill();
            if (bill != null) {
                if (bill.getBillItems() != null) {
                    bill.getBillItems().size();
                }
                if (bill.getBillFees() != null) {
                    bill.getBillFees().size();
                }
            }
        }
        getChannelCancelController().resetVariablesFromBooking();
        getChannelCancelController().setBillSession(selectedBillSession);
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
    public Patient getPatient() {
        if (patient == null) {
            patient = new Patient();
            Person p = new Person();
            patientDetailsEditable = true;
            patient.setPerson(p);
        }
        return patient;
    }

    @Override
    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public ChannelBillController getChannelCancelController() {
        return channelCancelController;
    }

    public void setChannelCancelController(ChannelBillController channelCancelController) {
        this.channelCancelController = channelCancelController;
    }

    public ChannelReportController getChannelReportController() {
        return channelReportController;
    }

    public void setChannelReportController(ChannelReportController channelReportController) {
        this.channelReportController = channelReportController;
    }

    public Boolean preSet() {
        if (getSelectedSessionInstance() == null) {
            JsfUtil.addErrorMessage("Please select Service Session");
            return false;
        }
        getChannelReportController().setSessionInstance(selectedSessionInstance);
        return true;
    }

    public ChannelSearchController getChannelSearchController() {
        return channelSearchController;
    }

    public void setChannelSearchController(ChannelSearchController channelSearchController) {
        this.channelSearchController = channelSearchController;
    }

    public ChannelBean getChannelBean() {
        return channelBean;
    }

    public void setChannelBean(ChannelBean channelBean) {
        this.channelBean = channelBean;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return ItemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade ItemFeeFacade) {
        this.ItemFeeFacade = ItemFeeFacade;
    }

    public Date getChannelDay() {
        return channelDay;
    }

    public void setChannelDay(Date channelDay) {
        this.channelDay = channelDay;
    }

    public int getSerealNo() {
        return serealNo;
    }

    public void setSerealNo(int serealNo) {
        this.serealNo = serealNo;
    }

    public ScheduleModel getEventModel() {
        if (eventModel == null) {
            eventModel = new DefaultScheduleModel();
        }
        return eventModel;
    }

    public void setEventModel(ScheduleModel eventModel) {
        this.eventModel = eventModel;
    }

    public ChannelScheduleEvent getEvent() {
        if (event == null) {
            event = new ChannelScheduleEvent();
        }
        return event;
    }

    public void setEvent(ChannelScheduleEvent event) {
        this.event = event;
    }

    public Date getSessionStartingDate() {
        if (sessionStartingDate == null) {
            sessionStartingDate = new Date();
        }
        return sessionStartingDate;
    }

    public void setSessionStartingDate(Date sessionStartingDate) {
        this.sessionStartingDate = sessionStartingDate;
    }

    public String getSelectTextSpeciality() {
        return selectTextSpeciality;
    }

    public void setSelectTextSpeciality(String selectTextSpeciality) {
        this.selectTextSpeciality = selectTextSpeciality;
    }

    public String getSelectTextConsultant() {
        return selectTextConsultant;
    }

    public void setSelectTextConsultant(String selectTextConsultant) {
        this.selectTextConsultant = selectTextConsultant;
    }

    public String getSelectTextSession() {
        return selectTextSession;
    }

    public void setSelectTextSession(String selectTextSession) {
        this.selectTextSession = selectTextSession;
    }

    public ArrivalRecord getArrivalRecord() {
        return arrivalRecord;
    }

    public void setArrivalRecord(ArrivalRecord arrivalRecord) {
        this.arrivalRecord = arrivalRecord;
    }

    public FingerPrintRecordFacade getFpFacade() {
        return fpFacade;
    }

    public ServiceSessionLeaveController getServiceSessionLeaveController() {
        return serviceSessionLeaveController;
    }

    public ChannelBillController getChannelBillController() {
        return channelBillController;
    }

    public DoctorSpecialityController getDoctorSpecialityController() {
        return doctorSpecialityController;
    }

    public ChannelStaffPaymentBillController getChannelStaffPaymentBillController() {
        return channelStaffPaymentBillController;
    }

    @Deprecated
    public BillSession getManagingBillSession() {
        return managingBillSession;
    }

    @Deprecated
    public void setManagingBillSession(BillSession managingBillSession) {
        if (managingBillSession != null) {
            Bill bill = managingBillSession.getBill();
            if (bill != null) {
                if (bill.getBillItems() != null) {
                    bill.getBillItems().size();
                }
                if (bill.getBillFees() != null) {
                    bill.getBillFees().size();
                }
            }
        }
        this.managingBillSession = managingBillSession;
    }

    public SessionInstance getSelectedSessionInstance() {
        return selectedSessionInstance;
    }

    public void setSelectedSessionInstance(SessionInstance selectedSessionInstance) {
        this.selectedSessionInstance = selectedSessionInstance;
    }

    public List<SessionInstance> getSessionInstances() {
        return sessionInstances;
    }

    public void setSessionInstances(List<SessionInstance> sessionInstances) {
        this.sessionInstances = sessionInstances;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public String getAgentRefNo() {
        return agentRefNo;
    }

    public void setAgentRefNo(String agentRefNo) {
        this.agentRefNo = agentRefNo;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public boolean isForiegn() {
        return foriegn;
    }

    public void setForiegn(boolean foriegn) {
        this.foriegn = foriegn;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public List<BillFee> getListBillFees() {
        return listBillFees;
    }

    public void setListBillFees(List<BillFee> listBillFees) {
        this.listBillFees = listBillFees;
    }

    public BillSession getBillSession() {
        return selectedBillSession;
    }

    public void setBillSession(BillSession selectedBillSession) {
        this.selectedBillSession = selectedBillSession;
    }

    public boolean isSettleSucessFully() {
        return settleSucessFully;
    }

    public void setSettleSucessFully(boolean settleSucessFully) {
        this.settleSucessFully = settleSucessFully;
    }

    public Bill getPrintingBill() {
        return printingBill;
    }

    public void setPrintingBill(Bill printingBill) {
        this.printingBill = printingBill;
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

    public void settleCreditWithCash() {
        settlePaymentMethod = PaymentMethod.Cash;
        settleCredit();
    }

    public void settleCreditWithCard() {
        settlePaymentMethod = PaymentMethod.Card;
        settleCredit();
    }

    public void settleCreditWithOnlinePayment() {
        settlePaymentMethod = PaymentMethod.OnlineSettlement;
        settleCredit();
    }

    public void settleCreditWithCredit() {
        settlePaymentMethod = PaymentMethod.Credit;
        settleCredit();
    }

    private boolean errorCheckForSettle() {

        if (settlePaymentMethod == null) {
            JsfUtil.addErrorMessage("Settle Payment Method for Settling");
            return true;
        }

        if (settlePaymentMethod == paymentMethod.OnCall) {
            JsfUtil.addErrorMessage("Settlement using 'On Call' is not allowed. Please select a different payment method.");
            return true;
        }

        if (getBillSession().getBill().getPaymentMethod() == PaymentMethod.Credit) {
            if (getBillSession().getBill().getFromInstitution() != null
                    && getBillSession().getBill().getFromInstitution().getBallance()
                    - getBillSession().getBill().getTotal() < -getBillSession().getBill().getFromInstitution().getAllowedCredit()) {
                JsfUtil.addErrorMessage("Agency Balance is Not Enough");
                return true;
            }
        }

        if (settlePaymentMethod == PaymentMethod.Agent && settleInstitution == null) {
            JsfUtil.addErrorMessage("Please select Agency");
            return true;
        }

        return false;
    }

    public void settleCredit() {
        if (errorCheckForSettle()) {
            return;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Channel Credit Booking Settle Requires Additional Information")) {

            if (settlePaymentMethod == PaymentMethod.Card) {
                if (paymentMethodData.getCreditCard().getInstitution() == null) {
                    JsfUtil.addErrorMessage("Please Enter Bank Details");
                    return;
                }
                if (paymentMethodData.getCreditCard().getNo() == null || paymentMethodData.getCreditCard().getNo().isEmpty()) {
                    JsfUtil.addErrorMessage("Please Enter Reference No.");
                    return;
                }
            }
            if (settlePaymentMethod == PaymentMethod.Credit) {
                if (toStaff == null && creditCompany == null) {
                    JsfUtil.addErrorMessage("Please Select the Staff or Credit Company");
                    return;
                }
            }
            if (errorChecksettle()) {
                return;
            }

        }

        Bill b = savePaidBill();
        BillItem bi = savePaidBillItem(b);
        savePaidBillFee(b, bi);
        BillSession bs = savePaidBillSession(b, bi);

        getBillSession().setPaidBillSession(bs);
        getBillSessionFacade().edit(bs);
        getBillSessionFacade().edit(getBillSession());

        getBillSession().getBill().setPaidAmount(b.getPaidAmount());
        getBillSession().getBill().setBalance(0.0);
        getBillSession().getBill().setPaidBill(b);
        getBillFacade().edit(getBillSession().getBill());

        b.setSingleBillItem(bi);
        b.setSingleBillSession(bs);
        getBillFacade().editAndCommit(b);

        createPayment(b, settlePaymentMethod);

        if (toStaff != null && settlePaymentMethod == PaymentMethod.Staff_Welfare) {
            staffBean.updateStaffWelfare(toStaff, getBillSession().getBill().getTotal());
            JsfUtil.addSuccessMessage("User Credit Updated");
        } else if (toStaff != null && settlePaymentMethod == PaymentMethod.Staff) {
            staffBean.updateStaffCredit(toStaff, getBillSession().getBill().getTotal());
            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
        }

        if (settlePaymentMethod == PaymentMethod.PatientDeposit) {
            if (getBillSession().getBill().getPatient().getRunningBalance() != null) {
                getBillSession().getBill().getPatient().setRunningBalance(getBillSession().getBill().getPatient().getRunningBalance() - getBillSession().getBill().getTotal());
            } else {
                getBillSession().getBill().getPatient().setRunningBalance(0.0 - getBillSession().getBill().getTotal());
            }
            getPatientFacade().edit(getBillSession().getBill().getPatient());
        }
        markSettlingBillAsPrinted();
        printPreview = true;
        printPreviewForSettle = true;
        printPreviewForReprintingAsDuplicate = false;
        printPreviewForReprintingAsOriginal = false;
        creditCompany = null;
        toStaff = null;
        JsfUtil.addSuccessMessage("On Call Channel Booking Settled");
    }

    private BillSession savePaidBillSession(Bill bill, BillItem billItem) {
        BillSession bs = new BillSession();
        bs.copy(getBillSession());
        bs.setBill(bill);
        bs.setBillItem(billItem);
        bs.setCreatedAt(new Date());
        bs.setCreater(getSessionController().getLoggedUser());

        getBillSessionFacade().create(bs);
        return bs;

    }

    private Bill savePaidBill() {
        Bill temp = new BilledBill();
        temp.copy(getBillSession().getBill());
        temp.copyValue(getBillSession().getBill());
        temp.setPaidAmount(getBillSession().getBill().getNetTotal());
        temp.setBalance(0.0);
        temp.setPaymentMethod(settlePaymentMethod);
        temp.setReferenceBill(getBillSession().getBill());
        temp.setBillType(BillType.ChannelPaid);
        temp.setBillTypeAtomic(BillTypeAtomic.CHANNEL_PAYMENT_FOR_BOOKING_BILL);
        String deptId = generateBillNumberDeptId(temp);
        temp.setInsId(deptId);
        temp.setDeptId(deptId);
        temp.setBookingId(deptId);
        temp.setDepartment(getSessionController().getDepartment());
        temp.setInstitution(getSessionController().getInstitution());
        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setCreatedAt(new Date());
        temp.setCreater(getSessionController().getLoggedUser());

        getBillFacade().create(temp);

        return temp;
    }

    private BillItem savePaidBillItem(Bill b) {
        BillItem bi = new BillItem();
        bi.copy(selectedBillSession.getBillItem());
        bi.setCreatedAt(new Date());
        bi.setCreater(getSessionController().getLoggedUser());
        bi.setBill(b);
        getBillItemFacade().create(bi);

        return bi;
    }

    private void savePaidBillFee(Bill b, BillItem bi) {

        for (BillFee f : selectedBillSession.getBill().getBillFees()) {

            BillFee bf = new BillFee();
            bf.copy(f);
            bf.setCreatedAt(Calendar.getInstance().getTime());
            bf.setCreater(getSessionController().getLoggedUser());
            bf.setBill(b);
            bf.setBillItem(bi);
            getBillFeeFacade().create(bf);
        }
    }

    private boolean errorChecksettle() {
        if (getPaymentSchemeController().checkPaymentMethodError(settlePaymentMethod, getPaymentMethodData())) {
            return true;
        }

        if (settlePaymentMethod == PaymentMethod.PatientDeposit) {
            if (!getBillSession().getBill().getPatient().getHasAnAccount()) {
                JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                return true;
            }
            double creditLimitAbsolute = Math.abs(getBillSession().getBill().getPatient().getCreditLimit());
            double runningBalance;
            if (getBillSession().getBill().getPatient().getRunningBalance() != null) {
                runningBalance = getBillSession().getBill().getPatient().getRunningBalance();
            } else {
                runningBalance = 0.0;
            }
            double availableForPurchase = runningBalance + creditLimitAbsolute;

            if (getBillSession().getBill().getTotal() > availableForPurchase) {
                JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                return true;
            }

        }

        if (settlePaymentMethod == PaymentMethod.Credit) {
            if (creditCompany == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare or credit company.");
                return true;
            }
        }

        if (settlePaymentMethod == PaymentMethod.Staff) {
            if (toStaff == null) {
                JsfUtil.addErrorMessage("Please select Staff Member.");
                return true;
            }
            if (toStaff.getCurrentCreditValue() + getBillSession().getBill().getTotal() > toStaff.getCreditLimitQualified()) {
                JsfUtil.addErrorMessage("No enough Credit.");
                return true;
            }
        }

        if (settlePaymentMethod == PaymentMethod.Staff_Welfare) {
            if (toStaff == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare.");
                return true;
            }
            if (Math.abs(toStaff.getAnnualWelfareUtilized()) + getBillSession().getBill().getTotal() > toStaff.getAnnualWelfareQualified()) {
                JsfUtil.addErrorMessage("No enough credit.");
                return true;
            }

        }

        if (settlePaymentMethod == PaymentMethod.MultiplePaymentMethods) {
            if (getPaymentMethodData() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (paymentSchemeController.checkPaymentMethodError(cd.getPaymentMethod(), cd.getPaymentMethodData())) {
                    return true;
                }
                if (cd.getPaymentMethod().equals(PaymentMethod.Staff)) {
                    if (cd.getPaymentMethodData().getStaffCredit().getTotalValue() == 0.0 || cd.getPaymentMethodData().getStaffCredit().getToStaff() == null) {
                        JsfUtil.addErrorMessage("Please fill the Paying Amount and Staff Name");
                        return true;
                    }
                    if (cd.getPaymentMethodData().getStaffCredit().getToStaff().getCurrentCreditValue() + cd.getPaymentMethodData().getStaffCredit().getTotalValue() > cd.getPaymentMethodData().getStaffCredit().getToStaff().getCreditLimitQualified()) {
                        JsfUtil.addErrorMessage("No enough Credit.");
                        return true;
                    }
                }
                //TODO - filter only relavant value
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();
            }
            System.out.println("multiplePaymentMethodTotalValue = " + multiplePaymentMethodTotalValue);
            double differenceOfBillTotalAndPaymentValue = getSelectedBillSession().getBillItem().getBill().getNetTotal() - multiplePaymentMethodTotalValue;
            differenceOfBillTotalAndPaymentValue = Math.abs(differenceOfBillTotalAndPaymentValue);
            System.out.println("differenceOfBillTotalAndPaymentValue = " + differenceOfBillTotalAndPaymentValue);
            if (differenceOfBillTotalAndPaymentValue > 1.0) {
                JsfUtil.addErrorMessage("Mismatch in differences of multiple payment method total and bill total");
                return true;
            }
            if (cashPaid == 0.0) {
                setCashPaid(multiplePaymentMethodTotalValue);
            }
        }
        return false;
    }

    public void channelBookingRefund() {
        if (refundableTotal > 0.0) {
            if (selectedBillSession.getBillItem().getBill().getPaymentMethod() == PaymentMethod.Agent) {
                refundAgentBill();
                return;
            }
            if (selectedBillSession.getBill().getBillType().getParent() == BillType.ChannelCashFlow && selectedBillSession.getBillItem().getBill().getPaymentMethod() != PaymentMethod.Agent) {
                refundCashFlowBill();
                return;
            }
            if (selectedBillSession.getBill().getBillType().getParent() == BillType.ChannelCreditFlow) {
                if (selectedBillSession.getBill().getPaidAmount() == 0) {
                    JsfUtil.addErrorMessage("Can't Refund. No Payments");
                } else {
                    refundCreditPaidBill();
                }
            }
            setPrintPreview(false);
        } else {
            JsfUtil.addErrorMessage("Nothing to Refund");
        }
    }

    public void refundCashFlowBill() {
        if (getBillSession() == null) {
            return;
        }

        if (getBillSession().getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            return;
        }

        if (getBillSession().getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded");
            return;
        }
        if (getCommentR() == null || getCommentR().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }

        refund(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession().getBill().getBillFees(), getBillSession());
        commentR = null;
    }

    public void refundAgentBill() {
        if (getBillSession() == null) {
            return;
        }

        if (getBillSession().getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            return;
        }

        if (getBillSession().getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded");
            return;
        }
        if (refundPaymentMethod == null) {
            JsfUtil.addErrorMessage("Select Refund Payment Method");
            return;
        }
        if (getCommentR() == null || getCommentR().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }

        refund(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession().getBill().getBillFees(), getBillSession());

        refundPaymentMethod = null;
        commentR = null;
    }

    public void refundCreditPaidBill() {
        if (getBillSession() == null) {
            return;
        }

        if (getBillSession().getPaidBillSession() == null) {
            JsfUtil.addErrorMessage("No Paid Bill Session");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded");
            return;
        }
        if (getCommentR() == null || getCommentR().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }

        if (getBillSession().getBill().getBillFees() != null) {

            for (BillFee bf : getBillSession().getBill().getBillFees()) {

                if (bf.getTmpChangedValue() == null) {
                    continue;
                }

                switch (bf.getFee().getFeeType()) {
                    case OtherInstitution:
                        if (bf.getFeeValue() < bf.getTmpChangedValue()) {
                            JsfUtil.addErrorMessage("Enter Lesser Amount for Agency Fee");
                            return;
                        }
                        break;
                    case Staff:
                        if (bf.getFeeValue() < bf.getTmpChangedValue()) {
                            JsfUtil.addErrorMessage("Enter Lesser Amount for Doctor Fee");
                            return;
                        }
                        break;

                    case Service:
                        if (bf.getFeeValue() < bf.getTmpChangedValue()) {
                            JsfUtil.addErrorMessage("Enter Lesser Amount for Scan Fee");
                            return;
                        }
                        break;

                    case OwnInstitution:
                        if (bf.getFeeValue() < bf.getTmpChangedValue()) {
                            JsfUtil.addErrorMessage("Enter Lesser Amount for Hospital Fee");
                            return;
                        }
                        break;

                    default:
                        JsfUtil.addErrorMessage("Enter Refund Amount");
                        break;

                }
            }

        }

        refund1(getBillSession().getPaidBillSession().getBill(), getBillSession().getPaidBillSession().getBillItem(), getBillSession().getBill().getBillFees(), getBillSession().getPaidBillSession());
        refund1(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession().getBill().getBillFees(), getBillSession());
        commentR = null;
//        bookingController.fillBillSessions();
    }

    public void refund(Bill bill, BillItem billItem, List<BillFee> billFees, BillSession billSession) {
        calRefundTotal();

        if ((bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) && bill.getPaidBill() == null) {
            bill.setPaidBill(bill);
            billFacade.edit(bill);
        }

        if (bill.getPaidBill() == null) {
            return;
        }

        if (bill.getPaidBill().equals(bill)) {
            RefundBill rb = (RefundBill) createCashRefundBill(bill);
            BillItem rBilItm = refundBillItems(billItem, rb);
            createReturnBillFee(billFees, rb, rBilItm);
            BillSession rSession = refundBillSession(billSession, rb, rBilItm);

            billSession.setReferenceBillSession(rSession);
            billSessionFacade.edit(billSession);

            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                rb.setPaymentMethod(refundPaymentMethod);
                if (refundPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, rBilItm, rSession, rSession.getBillItem().getAgentRefNo());
                }
            }
            rb.setPaymentMethod(refundPaymentMethod);
            bill.setRefunded(true);
            bill.setRefundedBill(rb);
            getBillFacade().edit(bill);

        } else {
            RefundBill rb = (RefundBill) createRefundBill(bill);
            BillItem rBilItm = refundBillItems(billItem, rb);
            createReturnBillFee(billFees, rb, rBilItm);
            BillSession rSession = refundBillSession(billSession, rb, rBilItm);

            billSession.setReferenceBillSession(rSession);
            billSessionFacade.edit(billSession);

            bill.setRefunded(true);
            bill.setRefundedBill(rb);
            getBillFacade().edit(bill);

            RefundBill rpb = (RefundBill) createRefundBill(bill.getPaidBill());
            BillItem rpBilItm = refundBillItems(bill.getSingleBillItem(), rb);
            BillSession rpSession = refundBillSession(billSession.getPaidBillSession(), rpb, rpBilItm);

            billSession.getPaidBillSession().setReferenceBillSession(rpSession);
            billSessionFacade.edit(billSession.getPaidBillSession());

            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                rb.setPaymentMethod(refundPaymentMethod);
                if (refundPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, rBilItm, rSession, rSession.getBillItem().getAgentRefNo());
                }
            }

            bill.getPaidBill().setRefunded(true);
            bill.getPaidBill().setRefundedBill(rpb);
            getBillFacade().edit(bill.getPaidBill());
            JsfUtil.addSuccessMessage("Successfully Refunded");
        }

    }

    private BillSession refundBillSession(BillSession billSession, Bill bill, BillItem billItem) {
        BillSession bs = new BillSession();
        bs.copy(billSession);
        bs.setBill(bill);
        bs.setBillItem(billItem);
        bs.setCreatedAt(new Date());
        bs.setCreater(getSessionController().getLoggedUser());
        getBillSessionFacade().create(bs);

        bill.setSingleBillSession(bs);
        getBillFacade().edit(bill);

        return bs;
    }

    public BillItem refundBillItems(BillItem bi, RefundBill rb) {
        BillItem rbi = new BillItem();
        rbi.copy(bi);
        rbi.resetValue();
        rbi.setBill(rb);
        rbi.setCreatedAt(Calendar.getInstance().getTime());
        rbi.setCreater(getSessionController().getLoggedUser());
        rbi.setItem(bi.getItem());
        rbi.setQty(0 - 1.0);
        rbi.setGrossValue(0 - getRefundableTotal());
        rbi.setNetValue(0 - getRefundableTotal());
        rbi.setReferanceBillItem(bi);
        getBillItemFacade().create(rbi);

        bi.setRefunded(Boolean.TRUE);
        getBillItemFacade().edit(bi);

        return rbi;

    }

    private void createReturnBillFee(List<BillFee> billFees, Bill b, BillItem bt) {
        double hf = 0.0;
        double sf = 0.0;
        for (BillFee bf : billFees) {
            if (bf.getTmpChangedValue() != null && bf.getTmpChangedValue() != 0) {
                BillFee newBf = new BillFee();
                newBf.copy(bf);
                newBf.setFeeGrossValue(0 - bf.getTmpChangedValue());
                newBf.setFeeValue(0 - bf.getTmpChangedValue());
                newBf.setBill(b);
                newBf.setBillItem(bt);
                newBf.setCreatedAt(new Date());
                newBf.setCreater(sessionController.getLoggedUser());
                billFeeFacade.create(newBf);

                if (bf.getFee().getFeeType() == FeeType.Staff) {
                    bt.setStaffFee(0 - bf.getTmpChangedValue());
                    sf += bt.getStaffFee();
                }

                if (bf.getFee().getFeeType() == FeeType.OwnInstitution) {
                    bt.setHospitalFee(0 - bf.getTmpChangedValue());
                    hf += bt.getHospitalFee();
                }

            }
        }
        b.setHospitalFee(hf);
        b.setStaffFee(sf);
        billFacade.edit(b);

        billItemFacade.edit(bt);
    }

    public void calRefundTotal() {
        refundableTotal = 0;
        for (BillFee bf : selectedBillSession.getBill().getBillFees()) {
            if (bf.getTmpChangedValue() != null) {
                refundableTotal += bf.getTmpChangedValue();
            }
        }
    }

    public void checkRefundTotal() {
        refundableTotal = 0;
        for (BillFee bf : selectedBillSession.getBill().getBillFees()) {
            if (bf.getTmpChangedValue() != null) {
                if (bf.getTmpChangedValue() > bf.getFeeValue()) {
                    bf.setTmpChangedValue(bf.getFeeValue());
                }
            }
        }

        calRefundTotal();
    }

    public void refund1(Bill bill, BillItem billItem, List<BillFee> billFees, BillSession billSession) {
        System.out.println("refund1 = 1");
        calRefundTotal();

        if ((bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) && bill.getPaidBill() == null) {
            bill.setPaidBill(bill);
            billFacade.edit(bill);
        }

        if (bill.getPaidBill() == null) {
            return;
        }

        if (bill.getPaidBill().equals(bill)) {
            RefundBill rb = (RefundBill) createCashRefundBill1(bill);
            BillItem rBilItm = refundBillItems(billItem, rb);
            createReturnBillFee(billFees, rb, rBilItm);
            BillSession rSession = refundBillSession(billSession, rb, rBilItm);

            billSession.setReferenceBillSession(rSession);
            billSessionFacade.edit(billSession);

            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                if (refundPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, rBilItm, rSession, rSession.getBillItem().getAgentRefNo());
                }
            }
            rb.setPaymentMethod(refundPaymentMethod);
            bill.setRefunded(true);
            bill.setRefundedBill(rb);
            getBillFacade().edit(bill);

        } else {
            System.out.println("bill.getPaidBill().equals(bill)");
            RefundBill rb = (RefundBill) createRefundBill1(bill);
            BillItem rBilItm = refundBillItems(billItem, rb);
            createReturnBillFee(billFees, rb, rBilItm);
            BillSession rSession = refundBillSession(billSession, rb, rBilItm);

            billSession.setReferenceBillSession(rSession);
            billSessionFacade.edit(billSession);

            bill.setRefunded(true);
            bill.setRefundedBill(rb);
            getBillFacade().edit(bill);

            RefundBill rpb = (RefundBill) createRefundBill(bill.getPaidBill());
            BillItem rpBilItm = refundBillItems(bill.getSingleBillItem(), rb);
            BillSession rpSession = refundBillSession(billSession.getPaidBillSession(), rpb, rpBilItm);

            billSession.getPaidBillSession().setReferenceBillSession(rpSession);
            billSessionFacade.edit(billSession.getPaidBillSession());

            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                rb.setPaymentMethod(refundPaymentMethod);
                if (refundPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, rBilItm, rSession, rSession.getBillItem().getAgentRefNo());
                }
            }

            bill.getPaidBill().setRefunded(true);
            bill.getPaidBill().setRefundedBill(rpb);
            getBillFacade().edit(bill.getPaidBill());
            JsfUtil.addSuccessMessage("Successfully Refunded");
        }

    }

    private Bill createRefundBill(Bill bill) {
        RefundBill rb = new RefundBill();
        rb.copy(bill);
        rb.setBilledBill(bill);
        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setBilledBill(bill);
        rb.setComments(comment);
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setDepartment(getSessionController().getDepartment());
        rb.setInstitution(getSessionController().getInstitution());

        rb.setNetTotal(0 - getRefundableTotal());
        rb.setTotal(0 - getRefundableTotal());
        rb.setPaidAmount(0 - getRefundableTotal());

        String insId = generateBillNumberInsId(rb);

        if (insId.equals("")) {
            return null;
        }
        rb.setInsId(insId);

        String deptId = generateBillNumberDeptId(rb);

        if (deptId.equals("")) {
            return null;
        }
        rb.setDeptId(deptId);
        rb.setBillTypeAtomic(BillTypeAtomic.CHANNEL_REFUND_WITH_PAYMENT_FOR_CREDIT_SETTLED_BOOKINGS);

        getBillFacade().create(rb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            rb.setPaymentMethod(refundPaymentMethod);
//            if (refundPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, billSession.getBillItem(), billSession, billSession.getBill().getReferralNumber());
//            }
        } else {
            rb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(rb);

//Need To Update Agent BAllance
        return rb;

    }

    private Bill createCashRefundBill(Bill bill) {
        RefundBill rb = new RefundBill();
        rb.copy(bill);
        rb.setBilledBill(bill);
        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setBilledBill(bill);
        rb.setComments(comment);
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setDepartment(getSessionController().getDepartment());
        rb.setInstitution(getSessionController().getInstitution());

        rb.setNetTotal(0 - getRefundableTotal());
        rb.setTotal(0 - getRefundableTotal());
        rb.setPaidAmount(0 - getRefundableTotal());

        String insId = generateBillNumberInsId(rb);

        if (insId.equals("")) {
            return null;
        }
        rb.setInsId(insId);

        String deptId = generateBillNumberDeptId(rb);

        if (deptId.equals("")) {
            return null;
        }
        rb.setDeptId(deptId);
        rb.setBillTypeAtomic(BillTypeAtomic.CHANNEL_REFUND_WITH_PAYMENT);

        getBillFacade().create(rb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            rb.setPaymentMethod(refundPaymentMethod);
//            if (refundPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, billSession.getBillItem(), billSession, billSession.getBill().getReferralNumber());
//            }
        } else {
            rb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(rb);

//Need To Update Agent BAllance
        return rb;

    }

    private Bill createRefundBill1(Bill bill) {
        RefundBill rb = new RefundBill();
        rb.copy(bill);
        rb.setBilledBill(bill);
        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setBilledBill(bill);
        rb.setComments(comment);
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setDepartment(getSessionController().getDepartment());
        rb.setInstitution(getSessionController().getInstitution());

        rb.setNetTotal(0 - getRefundableTotal());
        rb.setTotal(0 - getRefundableTotal());
        rb.setPaidAmount(0 - getRefundableTotal());

        String insId = generateBillNumberInsId(rb);

        if (insId.equals("")) {
            return null;
        }
        rb.setInsId(insId);

        String deptId = generateBillNumberDeptId(rb);

        if (deptId.equals("")) {
            return null;
        }
        rb.setDeptId(deptId);

        getBillFacade().create(rb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            rb.setPaymentMethod(refundPaymentMethod);
//            if (refundPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, billSession.getBillItem(), billSession, billSession.getBill().getReferralNumber());
//            }
        } else {
            rb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(rb);

//Need To Update Agent BAllance
        return rb;

    }

    private Bill createCashRefundBill1(Bill bill) {
        RefundBill rb = new RefundBill();
        rb.copy(bill);
        rb.setBilledBill(bill);
        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setBilledBill(bill);
        rb.setComments(comment);
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setDepartment(getSessionController().getDepartment());
        rb.setInstitution(getSessionController().getInstitution());

        rb.setNetTotal(0 - getRefundableTotal());
        rb.setTotal(0 - getRefundableTotal());
        rb.setPaidAmount(0 - getRefundableTotal());

        String insId = generateBillNumberInsId(rb);

        if (insId.equals("")) {
            return null;
        }
        rb.setInsId(insId);

        String deptId = generateBillNumberDeptId(rb);

        if (deptId.equals("")) {
            return null;
        }
        rb.setDeptId(deptId);

        getBillFacade().create(rb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            rb.setPaymentMethod(refundPaymentMethod);
//            if (refundPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, billSession.getBillItem(), billSession, billSession.getBill().getReferralNumber());
//            }
        } else {
            rb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(rb);

//Need To Update Agent BAllance
        return rb;

    }

    public String changePatient() {
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("No Session Selected");
            return null;
        }
        if (patient == null) {
            JsfUtil.addErrorMessage("No Session Selected");
            return null;
        }
        if (patient.getPerson().getName().isEmpty()) {
            JsfUtil.addErrorMessage("Please Enter a Name");
            return null;
        }

        selectedBillSession.getBill().setPatient(patient);
        billFacade.edit(selectedBillSession.getBill());
        JsfUtil.addSuccessMessage("Patient Changed");
        return navigateToManageBooking(selectedBillSession);
    }

    public void calculateSelectedBillSessionTotal() {
        PaymentSchemeDiscount paymentSchemeDiscount = priceMatrixController.fetchChannellingMemberShipDiscount(paymentMethod, paymentScheme, getSelectedSessionInstance().getOriginatingSession().getCategory());
        feeTotalForSelectedBill = 0.0;
        feeDiscountForSelectedBill = 0.0;
        feeNetTotalForSelectedBill = 0.0;
        if (paymentSchemeDiscount != null) {
            for (ItemFee itmf : getSelectedItemFees()) {
                System.out.println("itmf = " + itmf);
                if (foriegn) {
                    feeTotalForSelectedBill += itmf.getFfee();
                    if (itmf.isDiscountAllowed()) {
                        feeDiscountForSelectedBill += itmf.getFfee() * (paymentSchemeDiscount.getDiscountPercent() / 100);
                    }
                } else {
                    feeTotalForSelectedBill += itmf.getFee();
                    if (itmf.isDiscountAllowed()) {
                        feeDiscountForSelectedBill += itmf.getFee() * (paymentSchemeDiscount.getDiscountPercent() / 100);
                    }
                }
            }
        } else {
            for (ItemFee itmf : getSelectedItemFees()) {
                System.out.println("itmf = " + itmf);
                if (foriegn) {
                    feeTotalForSelectedBill += itmf.getFfee();
                } else {
                    feeTotalForSelectedBill += itmf.getFee();
                }
            }

        }
        feeNetTotalForSelectedBill = feeTotalForSelectedBill - feeDiscountForSelectedBill;
    }

    public SmsFacade getSmsFacade() {
        return smsFacade;
    }

    public void setSmsFacade(SmsFacade smsFacade) {
        this.smsFacade = smsFacade;
    }

    public List<ItemFee> getSelectedItemFees() {
        return selectedItemFees;
    }

    public List<ItemFee> getSelectedItemFeesWithouZeros() {
        if (selectedItemFees == null) {
            return null;
        }
        List<ItemFee> feesWithoutZeros = new ArrayList<>();
        if (selectedItemFees.isEmpty()) {
            return feesWithoutZeros;
        }
        for (ItemFee bf : selectedItemFees) {
            if (bf.getFee() > 0) {
                feesWithoutZeros.add(bf);
            }
        }
        return feesWithoutZeros;
    }

    public List<ItemFee> getFilteredSelectedItemFees() {
        if (selectedItemFees == null) {

            return null;
        }
        return selectedItemFees.stream()
                .filter(i -> (foriegn ? i.getFfee() != 0 : i.getFee() != 0))
                .collect(Collectors.toList());
    }

    public void setSelectedItemFees(List<ItemFee> selectedItemFees) {
        this.selectedItemFees = selectedItemFees;
    }

    public Item getItemToAddToBooking() {
        return itemToAddToBooking;
    }

    public void setItemToAddToBooking(Item itemToAddToBooking) {
        this.itemToAddToBooking = itemToAddToBooking;
    }

    public List<Item> getItemsAvailableToAddToBooking() {
        return itemsAvailableToAddToBooking;
    }

    public void setItemsAvailableToAddToBooking(List<Item> itemsAvailableToAddToBooking) {
        this.itemsAvailableToAddToBooking = itemsAvailableToAddToBooking;
    }

    private void fillItemAvailableToAdd() {
        itemsAvailableToAddToBooking = itemForItemController.getItemsForParentItem(selectedSessionInstance.getOriginatingSession());
    }

    public List<ItemFee> getSessionFees() {
        return sessionFees;
    }

    public void setSessionFees(List<ItemFee> sessionFees) {
        this.sessionFees = sessionFees;
    }

    public List<ItemFee> getAddedItemFees() {
        return addedItemFees;
    }

    public void setAddedItemFees(List<ItemFee> addedItemFees) {
        this.addedItemFees = addedItemFees;
    }

    public Double getFeeTotalForSelectedBill() {
        return feeTotalForSelectedBill;
    }

    public void setFeeTotalForSelectedBill(Double feeTotalForSelectedBill) {
        this.feeTotalForSelectedBill = feeTotalForSelectedBill;
    }

    public List<AppointmentActivity> getSelectedAppointmentActivities() {
        return selectedAppointmentActivities;
    }

    public void setSelectedAppointmentActivities(List<AppointmentActivity> selectedAppointmentActivities) {
        this.selectedAppointmentActivities = selectedAppointmentActivities;
    }

    public AppointmentActivity getAppointmentActivity() {
        return appointmentActivity;
    }

    public void setAppointmentActivity(AppointmentActivity appointmentActivity) {
        this.appointmentActivity = appointmentActivity;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = new Date();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<SessionInstance> getSessionInstancesFiltered() {
        return sessionInstancesFiltered;
    }

    public List<SessionInstance> getSortedSessionInstances() {

        if (oldSessionInstancesFiltered == null) {
            oldSessionInstancesFiltered = sessionInstancesFiltered;
        }

        if (sortedSessionInstances == null) {
            if (sessionInstancesFiltered != null) {
                sessionInstances = channelBean.listSessionInstances(fromDate, toDate, null, null, null);
                System.out.println("sortedSessionInstances == null");
                filterSessionInstances();
                sortSessions();
            }
        }

        if (oldSessionInstancesFiltered != sessionInstancesFiltered) {
            if (sessionInstancesFiltered != null) {
                sessionInstances = channelBean.listSessionInstances(fromDate, toDate, null, null, null);
                System.out.println("sortedSessionInstances == null");
                filterSessionInstances();
                sortSessions();
            }
            oldSessionInstancesFiltered = sortedSessionInstances;
        }

        return sortedSessionInstances;
    }

    private void sortSessions() {
        sortedSessionInstances = new ArrayList<>(sessionInstancesFiltered);
        Collections.sort(sortedSessionInstances, new Comparator<SessionInstance>() {
            @Override
            public int compare(SessionInstance s1, SessionInstance s2) {
                if (s1.isStarted() && !s2.isStarted() && !s1.isCompleted()) {
                    return -1;
                } else if (!s1.isStarted() && s2.isStarted() && !s2.isCompleted()) {
                    return 1;
                } else if (s1.isCompleted() && !s2.isCompleted()) {
                    return 1;
                } else if (!s1.isCompleted() && s2.isCompleted()) {
                    return -1;
                }
                return 0;
            }
        });
    }

    public void setSessionInstancesFiltered(List<SessionInstance> sessionInstancesFiltered) {
        this.sessionInstancesFiltered = sessionInstancesFiltered;
    }

    public String getSessionInstanceFilter() {
        return sessionInstanceFilter;
    }

    public void setSessionInstanceFilter(String sessionInstanceFilter) {
        this.sessionInstanceFilter = sessionInstanceFilter;
    }

    public Boolean getNeedToFillBillSessions() {
        return needToFillBillSessions;
    }

    public void setNeedToFillBillSessions(Boolean needToFillBillSessions) {
        this.needToFillBillSessions = needToFillBillSessions;
    }

    public PaymentMethod getCancelPaymentMethod() {
        return cancelPaymentMethod;
    }

    public void setCancelPaymentMethod(PaymentMethod cancelPaymentMethod) {
        this.cancelPaymentMethod = cancelPaymentMethod;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getNeedToFillBillSessionDetails() {
        return needToFillBillSessionDetails;
    }

    public void setNeedToFillBillSessionDetails(Boolean needToFillBillSessionDetails) {
        this.needToFillBillSessionDetails = needToFillBillSessionDetails;
    }

    public PaymentMethod getSettlePaymentMethod() {
        return settlePaymentMethod;
    }

    public void setSettlePaymentMethod(PaymentMethod settlePaymentMethod) {
        this.settlePaymentMethod = settlePaymentMethod;
    }

    public Institution getSettleInstitution() {
        return settleInstitution;
    }

    public void setSettleInstitution(Institution settleInstitution) {
        this.settleInstitution = settleInstitution;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public double getRefundableTotal() {
        return refundableTotal;
    }

    public void setRefundableTotal(double refundableTotal) {
        this.refundableTotal = refundableTotal;
    }

    public PaymentMethod getRefundPaymentMethod() {
        return refundPaymentMethod;
    }

    public void setRefundPaymentMethod(PaymentMethod refundPaymentMethod) {
        this.refundPaymentMethod = refundPaymentMethod;
    }

    public String getCommentR() {
        return commentR;
    }

    public void setCommentR(String commentR) {
        this.commentR = commentR;
    }

    public boolean isDisableRefund() {
        if (selectedBillSession.getBill().getBillType().getParent() == BillType.ChannelCreditFlow) {
            if (selectedBillSession.getBill().getPaidAmount() == 0) {
                disableRefund = true;
            } else {
                disableRefund = false;
            }
        }
        return disableRefund;
    }

    public void setDisableRefund(boolean disableRefund) {
        this.disableRefund = disableRefund;
    }

    @Override
    public void selectQuickOneFromQuickSearchPatient() {
        setPatient(patient);
        setPatientDetailsEditable(false);
        quickSearchPatientList = null;
    }

    @Override
    public void saveSelected(Patient p) {
        if (patient == null) {
            return;
        }
        if (patient.getPerson() == null) {
            return;
        }
        if (patient.getPerson().getId() == null) {
            patient.getPerson().setCreatedAt(new Date());
            patient.getPerson().setCreater(sessionController.getLoggedUser());
            personFacade.create(patient.getPerson());
        } else {
            personFacade.edit(patient.getPerson());
        }
        if (patient.getId() == null) {
            patient.setCreatedAt(new Date());
            patient.setCreatedInstitution(sessionController.getInstitution());
            patient.setCreater(sessionController.getLoggedUser());
            patientFacade.create(patient);
        } else {
            patientFacade.edit(patient);
        }
    }

    @Override
    public void saveSelectedPatient() {
        saveSelected(patient);
    }

    @Override
    public String getQuickSearchPhoneNumber() {
        return quickSearchPhoneNumber;
    }

    @Override
    public void setQuickSearchPhoneNumber(String quickSearchPhoneNumber) {
        this.quickSearchPhoneNumber = quickSearchPhoneNumber;
    }

    @Override
    public void quickSearchPatientLongPhoneNumber() {
        Patient patientSearched = null;
        String j;
        Map m = new HashMap();
        j = "select p from Patient p where p.retired=false and (p.patientPhoneNumber=:pp or p.patientMobileNumber=:pp)";
        Long searchedPhoneNumber = CommonFunctions.removeSpecialCharsInPhonenumber(quickSearchPhoneNumber);
        m.put("pp", searchedPhoneNumber);
        quickSearchPatientList = patientFacade.findByJpql(j, m);
        if (quickSearchPatientList == null) {
            JsfUtil.addErrorMessage("No Patient found !");
            setPatientDetailsEditable(true);
            setPatient(null);
            getPatient().setPhoneNumberStringTransient(quickSearchPhoneNumber);
            getPatient().setMobileNumberStringTransient(quickSearchPhoneNumber);
            setPatientDetailsEditable(true);
            return;
        } else if (quickSearchPatientList.isEmpty()) {
            JsfUtil.addErrorMessage("No Patient found !");
            setPatient(null);
            getPatient().setPhoneNumberStringTransient(quickSearchPhoneNumber);
            getPatient().setMobileNumberStringTransient(quickSearchPhoneNumber);
            setPatientDetailsEditable(true);
            return;
        } else if (quickSearchPatientList.size() == 1) {
            patientSearched = quickSearchPatientList.get(0);
            setPatient(patientSearched);
            setPatientDetailsEditable(false);
            quickSearchPatientList = null;
        } else {
            setPatient(null);
            patientSearched = null;
            setPatientDetailsEditable(false);
            JsfUtil.addErrorMessage("Pleace Select Patient");
        }
    }

    @Override
    public void quickSearchNewPatient() {
        quickSearchPatientList = null;
        setPatient(new Patient());
        setPatientDetailsEditable(true);
        if (quickSearchPhoneNumber != null) {
            getPatient().setPhoneNumberStringTransient(quickSearchPhoneNumber);
            getPatient().setMobileNumberStringTransient(quickSearchPhoneNumber);
        }
    }

    @Override
    public List<Patient> getQuickSearchPatientList() {
        return quickSearchPatientList;
    }

    @Override
    public void setQuickSearchPatientList(List<Patient> quickSearchPatientList) {
        this.quickSearchPatientList = quickSearchPatientList;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getStrTenderedValue() {
        return strTenderedValue;
    }

    public List<Item> getItemsAddedToBooking() {
        if (itemsAddedToBooking == null) {
            itemsAddedToBooking = new ArrayList<>();
        }
        return itemsAddedToBooking;
    }

    public void setItemsAddedToBooking(List<Item> itemsAddedToBooking) {
        this.itemsAddedToBooking = itemsAddedToBooking;
    }

    public void setStrTenderedValue(String strTenderedValue) {

        this.strTenderedValue = strTenderedValue;
        try {
            cashPaid = Double.parseDouble(strTenderedValue);
        } catch (NumberFormatException e) {
        }
    }

    public double getCashPaid() {
        return cashPaid;
    }

    public void setCashPaid(double cashPaid) {
        this.cashPaid = cashPaid;
    }

    public double getCashBalance() {
        if (feeTotalForSelectedBill != null) {
            cashBalance = feeNetTotalForSelectedBill - cashPaid;
        }
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public List<BillItem> getAdditionalBillItems() {
        return additionalBillItems;
    }

    public void setAdditionalBillItems(List<BillItem> additionalBillItems) {
        this.additionalBillItems = additionalBillItems;
    }

    public Institution getReferredByInstitution() {
        return referredByInstitution;
    }

    public void setReferredByInstitution(Institution referredByInstitution) {
        this.referredByInstitution = referredByInstitution;
    }

    public Doctor getReferredBy() {
        return referredBy;
    }

    public void setReferredBy(Doctor referredBy) {
        this.referredBy = referredBy;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public boolean isActiveCreditLimitPannel() {
        return activeCreditLimitPannel;
    }

    public void setActiveCreditLimitPannel(boolean activeCreditLimitPannel) {
        this.activeCreditLimitPannel = activeCreditLimitPannel;
    }

    public Integer getSelectedReserverdBookingNumber() {
        return selectedReserverdBookingNumber;
    }

    public void setSelectedReserverdBookingNumber(Integer selectedReserverdBookingNumber) {
        this.selectedReserverdBookingNumber = selectedReserverdBookingNumber;
    }

    public List<Integer> getReservedBookingNumbers() {
        return reservedBookingNumbers;
    }

    public void setReservedBookingNumbers(List<Integer> reservedBookingNumbers) {
        this.reservedBookingNumbers = reservedBookingNumbers;
    }

    public Boolean getNeedToFillSessionInstances() {
        return needToFillSessionInstances;
    }

    public void setNeedToFillSessionInstances(Boolean needToFillSessionInstances) {
        this.needToFillSessionInstances = needToFillSessionInstances;
    }

    public Boolean getNeedToFillSessionInstanceDetails() {
        return needToFillSessionInstanceDetails;
    }

    public void setNeedToFillSessionInstanceDetails(Boolean needToFillSessionInstanceDetails) {
        this.needToFillSessionInstanceDetails = needToFillSessionInstanceDetails;
    }

    public double getNetPlusVat() {
        return netPlusVat;
    }

    public void setNetPlusVat(double netPlusVat) {
        this.netPlusVat = netPlusVat;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public boolean isPrintPreviewForSettle() {
        return printPreviewForSettle;
    }

    public void setPrintPreviewForSettle(boolean printPreviewForSettle) {
        this.printPreviewForSettle = printPreviewForSettle;
    }

    public boolean isPrintPreviewForReprintingAsOriginal() {
        return printPreviewForReprintingAsOriginal;
    }

    public void setPrintPreviewForReprintingAsOriginal(boolean printPreviewForReprintingAsOriginal) {
        this.printPreviewForReprintingAsOriginal = printPreviewForReprintingAsOriginal;
    }

    public boolean isPrintPreviewForReprintingAsDuplicate() {
        return printPreviewForReprintingAsDuplicate;
    }

    public void setPrintPreviewForReprintingAsDuplicate(boolean printPreviewForReprintingAsDuplicate) {
        this.printPreviewForReprintingAsDuplicate = printPreviewForReprintingAsDuplicate;
    }

    public List<SessionInstance> getSessionInstanceByDoctor() {
        return sessionInstanceByDoctor;
    }

    public void setSessionInstanceByDoctor(List<SessionInstance> sessionInstanceByDoctor) {
        this.sessionInstanceByDoctor = sessionInstanceByDoctor;
    }

    public Staff getSelectedConsultant() {
        return selectedConsultant;
    }

    public void setSelectedConsultant(Staff selectedConsultant) {
        this.selectedConsultant = selectedConsultant;
    }

    public SessionInstance getSelectedSessionInstanceForRechedule() {
        return selectedSessionInstanceForRechedule;
    }

    public void setSelectedSessionInstanceForRechedule(SessionInstance selectedSessionInstanceForRechedule) {
        this.selectedSessionInstanceForRechedule = selectedSessionInstanceForRechedule;
    }

    public Double getFeeDiscountForSelectedBill() {
        return feeDiscountForSelectedBill;
    }

    public void setFeeDiscountForSelectedBill(Double feeDiscountForSelectedBill) {
        this.feeDiscountForSelectedBill = feeDiscountForSelectedBill;
    }

    public Double getFeeNetTotalForSelectedBill() {
        return feeNetTotalForSelectedBill;
    }

    public void setFeeNetTotalForSelectedBill(Double feeNetTotalForSelectedBill) {
        this.feeNetTotalForSelectedBill = feeNetTotalForSelectedBill;
    }

    public BillItem getSelectedBillItem() {
        return selectedBillItem;
    }

    public void setSelectedBillItem(BillItem selectedBillItem) {
        this.selectedBillItem = selectedBillItem;
    }

    public boolean isPrintPreviewForOnlineBill() {
        return printPreviewForOnlineBill;
    }

    public void setPrintPreviewForOnlineBill(boolean printPreviewForOnlineBill) {
        this.printPreviewForOnlineBill = printPreviewForOnlineBill;
    }
      
    public boolean isPrintPreviewC() {
        return printPreviewC;
    }

    public void setPrintPreviewC(boolean printPreviewC) {
        this.printPreviewC = printPreviewC;
    }

    public String getEncryptedBillSessionId() {
        return encryptedBillSessionId;
    }

    public void setEncryptedBillSessionId(String encryptedBillSessionId) {
        this.encryptedBillSessionId = encryptedBillSessionId;
    }

    public String getEncryptedExpiary() {
        return encryptedExpiary;
    }

    public void setEncryptedExpiary(String encryptedExpiary) {
        this.encryptedExpiary = encryptedExpiary;
    }

}
