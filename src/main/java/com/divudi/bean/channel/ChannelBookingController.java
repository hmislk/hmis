package com.divudi.bean.channel;

import com.divudi.bean.common.ControllerWithPatient;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.entity.Bill;
import com.divudi.entity.BillSession;
import com.divudi.entity.Institution;
import com.divudi.entity.Patient;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Person;
import com.divudi.entity.ServiceSessionInstance;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.List;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@SessionScoped
public class ChannelBookingController implements Serializable, ControllerWithPatient  {

    private Speciality speciality;
    private Staff staff;
    private List<Staff> consultants;
    private List<ServiceSessionInstance> serviceSessionInstances;
    private ServiceSessionInstance selectedServiceSessionInstance;

    private List<BillSession> billSessions;
    private BillSession selectedBillSession;
    private PaymentMethod paymentMethod;
    private PaymentScheme paymentScheme;
    private PaymentMethodData paymentMethodData;
    private boolean foriegn = false;
    private Institution institution;
    private int patientSearchTab;
    private Patient patient;
    private Bill printingBill;
    private boolean settleSucessFully = false;
    private String agentRefNo;
    private Staff toStaff;
    private String errorText;
    private boolean patientDetailsEditable;

    public ChannelBookingController() {
    }


    public String navigateToChannelBookingFromMenu() {
        prepareForNewChannellingBill();
        return "/channel/channel_booking?faces-redirect=true";
    }
    
   

    public void prepareForNewChannellingBill() {

    }

    public void listnerStaffListForRowSelectNew() {

    }

    public void listnerServiceSessionListForRowSelectNew() {

    }

    public void listnerBillSessionListForRowSelectNew() {

    }

    public void createBillfees(SelectEvent event) {

    }

    public void changeListener() {

    }
    

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public List<Staff> getConsultants() {
        return consultants;
    }

    public void setConsultants(List<Staff> consultants) {
        this.consultants = consultants;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public int getPatientSearchTab() {
        return patientSearchTab;
    }

    public void setPatientSearchTab(int patientSearchTab) {
        this.patientSearchTab = patientSearchTab;
    }

    public Bill getPrintingBill() {
        return printingBill;
    }

    public void setPrintingBill(Bill printingBill) {
        this.printingBill = printingBill;
    }

    public boolean isSettleSucessFully() {
        return settleSucessFully;
    }

    public void setSettleSucessFully(boolean settleSucessFully) {
        this.settleSucessFully = settleSucessFully;
    }

    public List<ServiceSessionInstance> getServiceSessionInstances() {
        return serviceSessionInstances;
    }

    public void setServiceSessionInstances(List<ServiceSessionInstance> serviceSessionInstances) {
        this.serviceSessionInstances = serviceSessionInstances;
    }

    public ServiceSessionInstance getSelectedServiceSessionInstance() {
        return selectedServiceSessionInstance;
    }

    public void setSelectedServiceSessionInstance(ServiceSessionInstance selectedServiceSessionInstance) {
        this.selectedServiceSessionInstance = selectedServiceSessionInstance;
    }

    public List<BillSession> getBillSessions() {
        return billSessions;
    }

    public void setBillSessions(List<BillSession> billSessions) {
        this.billSessions = billSessions;
    }

    public BillSession getSelectedBillSession() {
        return selectedBillSession;
    }

    public void setSelectedBillSession(BillSession selectedBillSession) {
        this.selectedBillSession = selectedBillSession;
    }

    public boolean isForiegn() {
        return foriegn;
    }

    public void setForiegn(boolean foriegn) {
        this.foriegn = foriegn;
    }

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

    public String getAgentRefNo() {
        return agentRefNo;
    }

    public void setAgentRefNo(String agentRefNo) {
        this.agentRefNo = agentRefNo;
    }

    public PaymentMethodData getPaymentMethodData() {
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
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

    
    
    
}
