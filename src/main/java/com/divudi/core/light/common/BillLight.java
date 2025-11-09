package com.divudi.core.light.common;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Title;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PatientEncounter;
import java.math.BigDecimal;
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
    private String patientAge;
    private String patientPhone;
    private Double grossValue;
    private Double discount;
    private Double netValue;
    private Long patientId;
    private String canterName;
    private String referringDoctorName;
    private BillTypeAtomic billTypeAtomic;
    private Long count;
    private Department toDpartment;
    private Double ccTotal;
    private Double hospitalTotal;
    private String referenceNumber;
    private Double total;
    private Double netTotal;
    private Double margin;
    private Double serviceCharge;
    private BigDecimal totalCostValue;
    private BigDecimal totalPurchaseValue;
    private BigDecimal totalRetailSaleValue;
    private PaymentMethod paymentMethod;
    private PatientEncounter patientEncounter;
    private String billItemNames;

    public BillLight() {
    }

    public BillLight(BillTypeAtomic billTypeAtomic, Long count) {
        this.billTypeAtomic = billTypeAtomic;
        this.count = count;
    }

    public BillLight(Department toDpartment, Long count) {
        this.toDpartment = toDpartment;
        this.count = count;
    }

    public BillLight(Department toDpartment, BillTypeAtomic billTypeAtomic, Long count) {
        this.billTypeAtomic = billTypeAtomic;
        this.toDpartment = toDpartment;
        this.count = count;
    }

    public BillLight(Long id, String billNo, Date billDate, Long patientId) {
        this.id = id;
        this.billNo = billNo;
        this.billDate = billDate;
        this.patientId = patientId;
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

    public BillLight(Long id, String billNo, String referenceNumber, Date billDate, String patientName, Double ccTotal, Double hospitalTotal) {
        this.id = id;
        this.billNo = billNo;
        this.referenceNumber = referenceNumber;
        this.billDate = billDate;
        this.patientName = patientName;
        this.ccTotal = ccTotal;
        this.hospitalTotal = hospitalTotal;
    }

    //Use 9B Report
    public BillLight(Long id, BillTypeAtomic billTypeAtomic, Double netValue) {
        this.id = id;
        this.billTypeAtomic = billTypeAtomic;
        this.netValue = netValue;
    }

    // Constructor for Pharmacy Daily Stock Value Report
    public BillLight(Long id, BillTypeAtomic billTypeAtomic, Double total, Double netTotal,
                     Double discount, Double margin, Double serviceCharge,
                     BigDecimal totalCostValue, BigDecimal totalPurchaseValue, BigDecimal totalRetailSaleValue,
                     PaymentMethod paymentMethod, PatientEncounter patientEncounter) {
        this.id = id;
        this.billTypeAtomic = billTypeAtomic;
        this.total = total;
        this.netTotal = netTotal;
        this.discount = discount;
        this.margin = margin;
        this.serviceCharge = serviceCharge;
        this.totalCostValue = totalCostValue;
        this.totalPurchaseValue = totalPurchaseValue;
        this.totalRetailSaleValue = totalRetailSaleValue;
        this.paymentMethod = paymentMethod;
        this.patientEncounter = patientEncounter;
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

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
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

    public Double getCcTotal() {
        return ccTotal;
    }

    public void setCcTotal(Double ccTotal) {
        this.ccTotal = ccTotal;
    }

    public Double getHospitalTotal() {
        return hospitalTotal;
    }

    public void setHospitalTotal(Double hospitalTotal) {
        this.hospitalTotal = hospitalTotal;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Double getMargin() {
        return margin;
    }

    public void setMargin(Double margin) {
        this.margin = margin;
    }

    public Double getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(Double serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public BigDecimal getTotalCostValue() {
        return totalCostValue;
    }

    public void setTotalCostValue(BigDecimal totalCostValue) {
        this.totalCostValue = totalCostValue;
    }

    public BigDecimal getTotalPurchaseValue() {
        return totalPurchaseValue;
    }

    public void setTotalPurchaseValue(BigDecimal totalPurchaseValue) {
        this.totalPurchaseValue = totalPurchaseValue;
    }

    public BigDecimal getTotalRetailSaleValue() {
        return totalRetailSaleValue;
    }

    public void setTotalRetailSaleValue(BigDecimal totalRetailSaleValue) {
        this.totalRetailSaleValue = totalRetailSaleValue;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public String getBillItemNames() {
        return billItemNames;
    }

    public void setBillItemNames(String billItemNames) {
        this.billItemNames = billItemNames;
    }

    public String getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(String patientAge) {
        this.patientAge = patientAge;
    }

}
