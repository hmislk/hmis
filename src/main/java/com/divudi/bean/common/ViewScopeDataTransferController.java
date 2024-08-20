/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.entity.BillSession;
import com.divudi.entity.Patient;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.channel.SessionInstance;
import java.io.Serializable;
import java.util.Date;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
/**
 *
 * @author divudi_lk
 */
@Named
@SessionScoped
public class ViewScopeDataTransferController  implements Serializable {
    
    private String previousPage;
    private int manageFeeIndex=-1;
    
    private Date fromDate;
    private Date toDate;
    private Boolean needToFillBillSessions;
    private Boolean needToFillBillSessionDetails;
    private Boolean needToFillSessionInstances;
    private Boolean needToFillSessionInstanceDetails;
    private Boolean needToCreateOpdBillForChannellingBillSession;
    private Boolean needToFillMembershipDetails;
    private Boolean needToPrepareForNewBooking;
    
    
    private String sessionInstanceFilter;
    private BillSession selectedBillSession;
    private SessionInstance selectedSessionInstance;
    private Patient patient;
    private PaymentScheme paymentScheme;
       
        

    /**
     * Creates a new instance of ViewController
     */
    public ViewScopeDataTransferController() {
    }

    public String getPreviousPage() {
        return previousPage;
    }

    
    
    public void setPreviousPage(String previousPage) {
        this.previousPage = previousPage;
    }
    
    public void makeAdminFeesAsPreviousPage(){
        previousPage = "/admin_fees?faces-redirect=true";
    }
    
    public void makeInvestigationAsPreviousPage(){
        previousPage = "/lab/investigation?faces-redirect=true";
    }
    
    public void makeBulkFeesAsPreviousPage(){
        previousPage = "/dataAdmin/manage_item_fees_bulk?faces-redirect=true";
    }
    
    public String backToPreviousPage(){
        if(previousPage==null||previousPage.trim().equals("")){
            previousPage = "/index";
        }
        return previousPage;
    }

    public int getManageFeeIndex() {
        return manageFeeIndex;
    }

    public void setManageFeeIndex(int manageFeeIndex) {
        this.manageFeeIndex = manageFeeIndex;
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

    public Boolean getNeedToFillBillSessions() {
        return needToFillBillSessions;
    }

    public void setNeedToFillBillSessions(Boolean needToFillBillSessions) {
        this.needToFillBillSessions = needToFillBillSessions;
    }

    public Boolean getNeedToFillBillSessionDetails() {
        return needToFillBillSessionDetails;
    }

    public void setNeedToFillBillSessionDetails(Boolean needToFillBillSessionDetails) {
        this.needToFillBillSessionDetails = needToFillBillSessionDetails;
    }

    public String getSessionInstanceFilter() {
        return sessionInstanceFilter;
    }

    public void setSessionInstanceFilter(String sessionInstanceFilter) {
        this.sessionInstanceFilter = sessionInstanceFilter;
    }

    public BillSession getSelectedBillSession() {
        return selectedBillSession;
    }

    public void setSelectedBillSession(BillSession selectedBillSession) {
        this.selectedBillSession = selectedBillSession;
    }

    public SessionInstance getSelectedSessionInstance() {
        return selectedSessionInstance;
    }

    public void setSelectedSessionInstance(SessionInstance selectedSessionInstance) {
        this.selectedSessionInstance = selectedSessionInstance;
    }

    public Boolean getNeedToCreateOpdBillForChannellingBillSession() {
        return needToCreateOpdBillForChannellingBillSession;
    }

    public void setNeedToCreateOpdBillForChannellingBillSession(Boolean needToCreateOpdBillForChannellingBillSession) {
        this.needToCreateOpdBillForChannellingBillSession = needToCreateOpdBillForChannellingBillSession;
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

    public Boolean getNeedToFillMembershipDetails() {
        return needToFillMembershipDetails;
    }

    public void setNeedToFillMembershipDetails(Boolean needToFillMembershipDetails) {
        this.needToFillMembershipDetails = needToFillMembershipDetails;
    }

    public Boolean getNeedToPrepareForNewBooking() {
        return needToPrepareForNewBooking;
    }

    public void setNeedToPrepareForNewBooking(Boolean needToPrepareForNewBooking) {
        this.needToPrepareForNewBooking = needToPrepareForNewBooking;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }
    
    
    
    
}
