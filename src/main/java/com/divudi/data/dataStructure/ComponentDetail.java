/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.dataStructure;

import com.divudi.bean.opd.OpdBillController;
import com.divudi.data.PaymentMethod;
import com.divudi.entity.Institution;
import com.divudi.entity.Patient;
import com.divudi.entity.Staff;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;

/**
 *
 * @author safrin
 */
public class ComponentDetail {

    @Inject
    OpdBillController opdBillController;
    private String no;
    private String comment;
    private Institution institution;
    private Date date;
    private Patient patient;
    private double totalValue;
    private PaymentMethod paymentMethod;
    private List<ComponentDetail> multiplePaymentMethodComponentDetails;
    private PaymentMethodData paymentMethodData;
    private int creditDuration;
    private Staff toStaff;

    public List<ComponentDetail> getMultiplePaymentMethodComponentDetails() {
        if (multiplePaymentMethodComponentDetails == null) {
            multiplePaymentMethodComponentDetails = new ArrayList<>();
        }
        if (multiplePaymentMethodComponentDetails.isEmpty()) {
            multiplePaymentMethodComponentDetails.add(new ComponentDetail());
        }
        return multiplePaymentMethodComponentDetails;
    }
    
    public void addAnotherPaymentDetail(){
        ComponentDetail cd = new ComponentDetail();
        getMultiplePaymentMethodComponentDetails().add(cd);
    }
    
    public void removePaymentDetail(ComponentDetail cd){
        getMultiplePaymentMethodComponentDetails().remove(cd);
    }

    public void setMultiplePaymentMethod(List<ComponentDetail> multiplePaymentMethod) {
        this.multiplePaymentMethodComponentDetails = multiplePaymentMethod;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public double getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(double totalValue) {
        this.totalValue = totalValue;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentMethodData getPaymentMethodData() {
        if(paymentMethodData==null){
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public int getCreditDuration() {
        return creditDuration;
    }

    public void setCreditDuration(int creditDuration) {
        this.creditDuration = creditDuration;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }
    
    
    

}
