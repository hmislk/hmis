/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.data.PaymentMethod;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.inward.AdmissionType;

/**
 *
 * @author safrin
 */
public class SearchKeyword {

    private String billNo;
    private String refBillNo;
    private String patientName;
    String requestNo;
    private String patientPhone;
    private String total;
    private String netTotal;
    private String itemName;
    private String speciality;
    private String staffName;
    private String personName;
    private String fromInstitution;
    private String fromDepartment;
    private String toInstitution;
    private String toDepartment;
    private String creator;
    private String bank;
    private String number;
    private String department;
    private String code;
    private String category;
    private String institution;
    private String bhtNo="";
    private String paymentScheme;
    private String paymentmethod;
    private String insId;
    private String deptId;
    String serviceName;
    PatientEncounter patientEncounter;
    PaymentMethod paymentMethod;
    AdmissionType admissionType;
    Institution ins;
    Department frmDepartment;
    Department tooDepartment;
    

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    

    public boolean checkKeyword() {
        if (billNo != null && !billNo.trim().equals("")) {
            return true;
        }

        if (patientName != null && !patientName.trim().equals("")) {
            return true;
        }

        if (patientPhone != null && !patientPhone.trim().equals("")) {
            return true;
        }

        if (total != null && !total.trim().equals("")) {
            return true;
        }

        if (netTotal != null && !netTotal.trim().equals("")) {
            return true;
        }
        return false;

    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }
    
    

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(String netTotal) {
        this.netTotal = netTotal;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getSpeciality() {
        return speciality;
    }

    public void setSpeciality(String speciality) {
        this.speciality = speciality;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(String fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getRefBillNo() {
        return refBillNo;
    }

    public void setRefBillNo(String refBillNo) {
        this.refBillNo = refBillNo;
    }

    public String getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(String toInstitution) {
        this.toInstitution = toInstitution;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public String getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(String toDepartment) {
        this.toDepartment = toDepartment;
    }

    public String getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(String fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public String getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(String paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public String getPaymentmethod() {
        return paymentmethod;
    }

    public void setPaymentmethod(String paymentmethod) {
        this.paymentmethod = paymentmethod;
    }

    public String getInsId() {
        return insId;
    }

    public void setInsId(String insId) {
        this.insId = insId;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public Institution getIns() {
        return ins;
    }

    public void setIns(Institution ins) {
        this.ins = ins;
    }

    public Department getFrmDepartment() {
        return frmDepartment;
    }

    public void setFrmDepartment(Department frmDepartment) {
        this.frmDepartment = frmDepartment;
    }

    public Department getTooDepartment() {
        return tooDepartment;
    }

    public void setTooDepartment(Department tooDepartment) {
        this.tooDepartment = tooDepartment;
    }
    
    
    
    
}
