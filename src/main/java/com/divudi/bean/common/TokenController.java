/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.divudi.bean.common;

import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Patient;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author acer
 */
@Named(value = "tokenController")
@SessionScoped
public class TokenController implements Serializable {

    /**
     * Creates a new instance of TokenController
     */
    // <editor-fold defaultstate="collapsed" desc="Class variables">
    private String tokenId;
    private String tokenNumber;
    private Date issuedAt;
    private boolean isCalled;
    private Date calledAt;
    private boolean isInProgress;
    private Date startedAt;
    private boolean isCompleted;
    private Date completedAt;
    private Patient patient;
    private Department serviceDepartment;
    private Department fromDepartment;
    private Department toDepartment;
    private Institution serviceInstitution;
    private Institution fromInstitution;
    private Institution toInstitution;
    private Department serviceCounter;
    private WebUser createdBy;
    private Date CreatedAt;
    private boolean retired;
    private WebUser retiredBy;
    private Staff staff;
    private Staff fromStaff;
    private Staff toStaff;
// </editor-fold> 

    public TokenController() {
        
    }

    public void createNewToken(){
        
    }
    
    public void listTokens(){
        
    }
    
    public void callToken(){
    
    }
    
    public void startTokenService(){
    
    }
    
    public void completeTokenService(){
    
    }
    
    public void reverseCallToken(){
    
    }
    
    public void recallToken(){
    
    }
    
    public void restartTokenService(){
    
    }
    
    public void reverseCompleteTokenService(){
    
    }
    
    public void fetchNextToken(){
    
    }
    
    public void fetchPreviousToken(){
    
    }
    
    public void searchToken(){
    
    }

    
    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(String tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
    }

    public boolean isIsCalled() {
        return isCalled;
    }

    public void setIsCalled(boolean isCalled) {
        this.isCalled = isCalled;
    }

    public Date getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(Date calledAt) {
        this.calledAt = calledAt;
    }

    public boolean isIsInProgress() {
        return isInProgress;
    }

    public void setIsInProgress(boolean isInProgress) {
        this.isInProgress = isInProgress;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public boolean isIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Department getServiceDepartment() {
        return serviceDepartment;
    }

    public void setServiceDepartment(Department serviceDepartment) {
        this.serviceDepartment = serviceDepartment;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public Institution getServiceInstitution() {
        return serviceInstitution;
    }

    public void setServiceInstitution(Institution serviceInstitution) {
        this.serviceInstitution = serviceInstitution;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Department getServiceCounter() {
        return serviceCounter;
    }

    public void setServiceCounter(Department serviceCounter) {
        this.serviceCounter = serviceCounter;
    }

    public WebUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(WebUser createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return CreatedAt;
    }

    public void setCreatedAt(Date CreatedAt) {
        this.CreatedAt = CreatedAt;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public WebUser getRetiredBy() {
        return retiredBy;
    }

    public void setRetiredBy(WebUser retiredBy) {
        this.retiredBy = retiredBy;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Staff getFromStaff() {
        return fromStaff;
    }

    public void setFromStaff(Staff fromStaff) {
        this.fromStaff = fromStaff;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }
    // </editor-fold> 
}
