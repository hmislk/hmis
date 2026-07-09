package com.divudi.bean.inward;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.util.CommonFunctions;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Thisara Samuditha | github - thisarasamuditha | thellamburavithanagethisarasam@gmail.com
 */
@Named(value = "outsideChargeReportController")
@SessionScoped
public class OutsideChargeReportController implements Serializable {

    private Date fromDate;
    private Date toDate;
    private Date dischargeFromDate;
    private Date dischargeToDate;
    private Date invoiceApprovedFromDate;
    private Date invoiceApprovedToDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private Doctor referringDoctor;
    private Institution creditCompany;
    private List<AdmissionType> admissionType;

    public OutsideChargeReportController() {
    }
    
    public void processOutsidePaymentReport(){
        
        
        
        
        
        
        
        
    }
    
    // getters and setters 

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
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getDischargeFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return dischargeFromDate;
    }

    public void setDischargeFromDate(Date dischargeFromDate) {
        this.dischargeFromDate = dischargeFromDate;
    }

    public Date getDischargeToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return dischargeToDate;
    }

    public void setDischargeToDate(Date dischargeToDate) {
        this.dischargeToDate = dischargeToDate;
    }

    public Date getInvoiceApprovedFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return invoiceApprovedFromDate;
    }

    public void setInvoiceApprovedFromDate(Date InvoiceApprovedFromDate) {
        this.invoiceApprovedFromDate = InvoiceApprovedFromDate;
    }

    public Date getInvoiceApprovedToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return invoiceApprovedToDate;
    }

    public void setInvoiceApprovedToDate(Date InvoiceApprovedToDate) {
        this.invoiceApprovedToDate = InvoiceApprovedToDate;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Doctor getReferringDoctor() {
        return referringDoctor;
    }

    public void setReferringDoctor(Doctor referringDoctor) {
        this.referringDoctor = referringDoctor;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public List<AdmissionType> getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(List<AdmissionType> admissionType) {
        this.admissionType = admissionType;
    }

}