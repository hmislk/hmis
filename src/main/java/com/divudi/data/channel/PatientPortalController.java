/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.divudi.data.channel;

import com.divudi.bean.common.DoctorController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.PersonInstitutionType;
import com.divudi.entity.Bill;
import com.divudi.entity.Doctor;
import com.divudi.entity.Payment;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.StaffFacade;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 *
 * @author acer
 */
@Named(value = "patientPortalController")
@ApplicationScoped
public class PatientPortalController {

    private String PatientphoneNumber;
    private boolean bookDoctor;
    private Doctor selectedDoctor;
    private List<Bill> patientBills;
    private List<Payment> PatientPayments;
    private List<Payment> channelPayments;
    private List<ServiceSession> docotrSessions;
    private List<Doctor> doctors;
    private Speciality selectedSpeciality;

    @EJB
    StaffFacade staffFacade;
    @EJB
    PaymentFacade paymentFacade;

    @Inject
    private SessionController sessionController;
    @Inject
    DoctorController doctorController;

    public void docotrBooking() {
        doctors=null;
        bookDoctor = true;
        doctors = doctorController.listDoctors(selectedSpeciality);
    }
    
    public String getPatientphoneNumber() {
        return PatientphoneNumber;
    }

    public void setPatientphoneNumber(String PatientphoneNumber) {
        this.PatientphoneNumber = PatientphoneNumber;
    }

    public boolean isBookDoctor() {
        return bookDoctor;
    }

    public void setBookDoctor(boolean bookDoctor) {
        this.bookDoctor = bookDoctor;
    }

    public List<Bill> getPatientBills() {
        return patientBills;
    }

    public void setPatientBills(List<Bill> patientBills) {
        this.patientBills = patientBills;
    }

    public List<Payment> getPatientPayments() {
        return PatientPayments;
    }

    public void setPatientPayments(List<Payment> PatientPayments) {
        this.PatientPayments = PatientPayments;
    }

    public List<Payment> getChannelPayments() {
        return channelPayments;
    }

    public void setChannelPayments(List<Payment> channelPayments) {
        this.channelPayments = channelPayments;
    }

    public List<ServiceSession> getDocotrSessions() {
        return docotrSessions;
    }

    public void setDocotrSessions(List<ServiceSession> channels) {
        this.docotrSessions = channels;
    }

    public Doctor getSelectedDoctor() {
        return selectedDoctor;
    }

    public void setSelectedDoctor(Doctor selectedDoctor) {
        this.selectedDoctor = selectedDoctor;
    }

    public List<Doctor> getDoctors() {
        return doctors;
    }

    public void setDoctors(List<Doctor> doctors) {
        this.doctors = doctors;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Speciality getSelectedSpeciality() {
        return selectedSpeciality;
    }

    public void setSelectedSpeciality(Speciality selectedSpeciality) {
        this.selectedSpeciality = selectedSpeciality;
    }

}
