package com.divudi.core.light.common;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Department;
import com.divudi.core.data.PaymentMethod;
import java.util.Date;
import java.util.Objects;

/**
 *
 * @author Senula Nanayakkara
 */
public class BillLight {

    private Long id;
    private String billNo;
    private Date billDate;
    private Date billTime;
    private String institutionName;
    private String departmentName;
    private String userName;
    private String patientName;
    private String patientPhone;
    private Double grossValue;
    private Double discount;
    private Double netValue;
    private Long patientId;
    private String referenceBillNo;
    private String invoiceNo;
    private Date invoiceDate;
    private PaymentMethod paymentMethod;
    private Double saleValue;
    private String canterName;
    private String referringDoctorName;
    private BillTypeAtomic billTypeAtomic;
    private Long count;
    private Department toDpartment;

    public BillLight() {
    }
    
    public BillLight(BillTypeAtomic  billTypeAtomic, Long count) {
        this.billTypeAtomic = billTypeAtomic;
        this.count = count;
    }
    
    public BillLight(Department  toDpartment, Long count) {
        this.toDpartment = toDpartment;
        this.count = count;
    }
    
    public BillLight(Department  toDpartment, BillTypeAtomic  billTypeAtomic, Long count) {
        this.billTypeAtomic = billTypeAtomic;
        this.toDpartment = toDpartment;
        this.count = count;
    }

    public BillLight(Long id, String billNo, Date billDate, Date billTime, String patientName, Double netValue) {
        this.id = id;
        this.billNo = billNo;
        this.billDate = billDate;
        this.billTime = billTime;
        this.patientName = patientName;
        this.netValue = netValue;
    }

    public BillLight(Long id, String billNo, Date billDate, String CenterName, String institutionName, String departmentName, String userName, String patientName, String patientPhone, Double grossValue, Double discount, Double netValue, Long patientId) {
        this.id = id;
        this.billNo = billNo;
        this.billDate = billDate;
        this.canterName = CenterName;
        this.institutionName = institutionName;
        this.departmentName = departmentName;
        this.userName = userName;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.grossValue = grossValue;
        this.discount = discount;
        this.netValue = netValue;
        this.patientId = patientId;
    }

    public BillLight(Long id, String billNo, Date billDate, String institutionName, String departmentName, String userName, String patientName, String patientPhone, Double grossValue, Double discount, Double netValue) {
        this.id = id;
        this.billNo = billNo;
        this.billDate = billDate;
        this.institutionName = institutionName;
        this.departmentName = departmentName;
        this.userName = userName;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.grossValue = grossValue;
        this.discount = discount;
        this.netValue = netValue;
    }

    public BillLight(Long id, String billNo, Date billDate, String institutionName, String departmentName, String userName, String patientName, String patientPhone, Double grossValue, Double discount, Double netValue, Long patientId) {
        this.id = id;
        this.billNo = billNo;
        this.billDate = billDate;
        this.institutionName = institutionName;
        this.departmentName = departmentName;
        this.userName = userName;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.grossValue = grossValue;
        this.discount = discount;
        this.netValue = netValue;
        this.patientId = patientId;
    }

    public BillLight(Long id, String billNo, Date billDate,
            String institutionName, String departmentName, String userName,
            String patientName, String patientPhone, Double grossValue,
            Double discount, Double netValue, Long patientId,
            String referenceBillNo, String invoiceNo, Date invoiceDate,
            PaymentMethod paymentMethod, Double saleValue) {
        this.id = id;
        this.billNo = billNo;
        this.billDate = billDate;
        this.institutionName = institutionName;
        this.departmentName = departmentName;
        this.userName = userName;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.grossValue = grossValue;
        this.discount = discount;
        this.netValue = netValue;
        this.patientId = patientId;
        this.referenceBillNo = referenceBillNo;
        this.invoiceNo = invoiceNo;
        this.invoiceDate = invoiceDate;
        this.paymentMethod = paymentMethod;
        this.saleValue = saleValue;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Date getBillDate() {
        return billDate;
    }

    public void setBillDate(Date billDate) {
        this.billDate = billDate;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public Double getGrossValue() {
        return grossValue;
    }

    public void setGrossValue(Double grossValue) {
        this.grossValue = grossValue;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getNetValue() {
        return netValue;
    }

    public void setNetValue(Double netValue) {
        this.netValue = netValue;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getReferenceBillNo() {
        return referenceBillNo;
    }

    public void setReferenceBillNo(String referenceBillNo) {
        this.referenceBillNo = referenceBillNo;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public Date getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(Date invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getSaleValue() {
        return saleValue;
    }

    public void setSaleValue(Double saleValue) {
        this.saleValue = saleValue;
    }

    public String getReferringDoctorName() {
        return referringDoctorName;
    }

    public void setReferringDoctorName(String referringDoctorName) {
        this.referringDoctorName = referringDoctorName;
    }

    public Date getBillTime() {
        return billTime;
    }

    public void setBillTime(Date billTime) {
        this.billTime = billTime;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BillLight other = (BillLight) obj;
        return Objects.equals(this.id, other.id);
    }

    public String getCanterName() {
        return canterName;
    }

    public void setCanterName(String canterName) {
        this.canterName = canterName;
    }

    public BillTypeAtomic  getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic  billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Department getToDpartment() {
        return toDpartment;
    }

    public void setTodDpartment(Department toDpartment) {
        this.toDpartment = toDpartment;
    }

}
