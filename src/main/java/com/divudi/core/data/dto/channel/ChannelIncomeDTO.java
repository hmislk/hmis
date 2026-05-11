package com.divudi.core.data.dto.channel;

import java.util.Date;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Title;

public class ChannelIncomeDTO {

        private long bsId;
        private long billId;
        private String billDeptId;
        private BillTypeAtomic billTypeAtomic;
        private BillType billType;
        private Date appoinmentDate;
        private Date billedDate;
        private String billedBy;
        private String patientName;
        private Title patientTitle;
        private String patientPhone;

        private PaymentMethod paymentMethod;
        private String creditCompanyName;
        private String paymentReference;
        private String toInstitution;
        private String toDepartment;
        private String institution;
        private String department;

        private double doctorFee;
        private double hosFee;
        private double grossTotal;
        private double paymentFee;
        private double discount;

        private String remark;

        private boolean cancelled;
        private String cancelledBillDeptId;

        private boolean refunded;
        private String refundBillDeptId;



        public ChannelIncomeDTO( ) {
            doctorFee = 0.0;
            hosFee = 0.0;
            paymentFee = 0.0;
            grossTotal = 0.0;
            discount = 0.0;
        }

         // constructor for channel agent bookings
        public ChannelIncomeDTO(Long billId, Date createdAt, String billDeptId, BillTypeAtomic billTypeAtomic, String patientName, Title patientTitle, String cashierName, boolean canceled,
                        boolean refunded, Double hospitalFee, Double staffFee, Double grossTotal, Double netTotal, Double discount, String paymentReference, String creditCompany, String institution, String department, String toInstitution, String toDepartment) {
            this.billId = billId;
            this.billedDate = createdAt;
            this.billDeptId = billDeptId;
            this.billTypeAtomic = billTypeAtomic;
            this.patientName = patientName;
            this.patientTitle = patientTitle;
            this.billedBy = cashierName;
            this.cancelled = canceled;
            this.refunded = refunded;
            this.hosFee = hospitalFee;
            this.doctorFee = staffFee;
            this.grossTotal = grossTotal;
            this.paymentFee = netTotal;
            this.discount = discount;
            this.paymentReference = paymentReference;
            this.creditCompanyName = creditCompany;
            this.toInstitution = toInstitution;
            this.toDepartment = toDepartment;
            this.institution = institution;
            this.department = department;
        }

        public BillTypeAtomic getBillTypeAtomic() {
            return billTypeAtomic;
        }

        public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
            this.billTypeAtomic = billTypeAtomic;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        public boolean isRefunded() {
            return refunded;
        }

        public void setRefunded(boolean refunded) {
            this.refunded = refunded;
        }

        public String getBilledBy() {
            return billedBy;
        }

        public void setBilledBy(String billedBy) {
            this.billedBy = billedBy;
        }

        public Date getAppoinmentDate() {
            return appoinmentDate;
        }

        public void setAppoinmentDate(Date appoinmentDate) {
            this.appoinmentDate = appoinmentDate;
        }

        public String getPatientName() {
            return patientName;
        }

        public void setPatientName(String patientName) {
            this.patientName = patientName;
        }

        public String getPatientNameWithTitle() {
            String name = "";
            if (patientTitle != null) {
                name = patientTitle.getLabel() + " ";
            }
            name += patientName;
            return name;
        }

        public String getPatientPhone() {
            return patientPhone;
        }

        public void setPatientPhone(String patientPhone) {
            this.patientPhone = patientPhone;
        }

        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public double getDoctorFee() {
            return doctorFee;
        }

        public void setDoctorFee(double doctorFee) {
            this.doctorFee = doctorFee;
        }

        public double getHosFee() {
            return hosFee;
        }

        public void setHosFee(double hosFee) {
            this.hosFee = hosFee;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public long getBsId() {
            return bsId;
        }

        public void setBsId(long bsId) {
            this.bsId = bsId;
        }

        public long getBillId() {
            return billId;
        }

        public void setBillId(long billId) {
            this.billId = billId;
        }

        public Date getBilledDate() {
            return billedDate;
        }

        public void setBilledDate(Date billedDate) {
            this.billedDate = billedDate;
        }

        public String getBillDeptId() {
            return billDeptId;
        }

        public void setBillDeptId(String billDeptId) {
            this.billDeptId = billDeptId;
        }

        public BillType getBillType() {
            return billType;
        }

        public void setBillType(BillType billType) {
            this.billType = billType;
        }

        public String getCancelledBillDeptId() {
            return cancelledBillDeptId;
        }

        public void setCancelledBillDeptId(String cancelledBillDeptId) {
            this.cancelledBillDeptId = cancelledBillDeptId;
        }

        public String getRefundBillDeptId() {
            return refundBillDeptId;
        }

        public void setRefundBillDeptId(String refundBillDeptId) {
            this.refundBillDeptId = refundBillDeptId;
        }

        public String getPaymentReference() {
            return paymentReference;
        }

        public void setPaymentReference(String paymentReference) {
            this.paymentReference = paymentReference;
        }

        public String getCreditCompanyName() {
            return creditCompanyName;
        }

        public void setCreditCompanyName(String companyName) {
            this.creditCompanyName = companyName;
        }

        public double getPaymentFee() {
            return paymentFee;
        }

        public void setPaymentFee(double fee) {
            this.paymentFee = fee;
        }

        public String getToInstitution() {
            return toInstitution;
        }

        public void setToInstitution(String toInstitution) {
            this.toInstitution = toInstitution;
        }

        public String getToDepartment() {
            return toDepartment;
        }

        public void setToDepartment(String toDepartment) {
            this.toDepartment = toDepartment;
        }

        public String getInstitution() {
            return institution;
        }

        public void setInstitution(String institution) {
            this.institution = institution;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

        public double getGrossTotal() {
            return grossTotal;
        }

        public void setGrossTotal(double grossTotal) {
            this.grossTotal = grossTotal;
        }

        public String getBookedFrom() {
            String bf = "";
            if (institution != null && !institution.trim().isEmpty()) {
                bf = institution + " - ";
            }
            if (department != null) {
                bf += department;
            }
            return bf;
        }

        public String getChannelAt() {
            String ca = "";
            if (toInstitution != null && !toInstitution.trim().isEmpty()) {
                ca = toInstitution + " - ";
            }
            if (toDepartment != null) {
                ca += toDepartment;
            }
            return ca;
        }
    }
