package com.divudi.entity.cashTransaction;

import com.divudi.data.PaymentMethod;
import com.divudi.entity.Bill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Payment;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author Buddhika
 */
@Entity
public class CashBookEntry implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;
    @ManyToOne
    private Bill bill;
    @ManyToOne
    private Payment payment;
    @ManyToOne
    private CashBook cashBook;
    @Enumerated
    private PaymentMethod paymentMethod;
    private Double entryValue;
    private Double institutionBalance;
    private Double departmentBalance;
    private Double siteBalance;

    @ManyToOne
    private Institution institution;
    @ManyToOne
    private Institution site;
    @ManyToOne
    private Department department;
    @ManyToOne
    private WebUser webUser;
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date cashbookDate;

    //Created Properties
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;

    //Retairing properties 
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;

    //Editer Properties
    @ManyToOne
    private WebUser editer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date editedAt;

    // Balances before the entry
    private Double onCallBalanceBefore;
    private Double cashBalanceBefore;
    private Double cardBalanceBefore;
    private Double multiplePaymentMethodsBalanceBefore;
    private Double staffBalanceBefore;
    private Double creditBalanceBefore;
    private Double staffWelfareBalanceBefore;
    private Double voucherBalanceBefore;
    private Double iouBalanceBefore;
    private Double agentBalanceBefore;
    private Double chequeBalanceBefore;
    private Double slipBalanceBefore;
    private Double ewalletBalanceBefore;
    private Double patientDepositBalanceBefore;
    private Double patientPointsBalanceBefore;
    private Double onlineSettlementBalanceBefore;
    private Double noneBalanceBefore;
    private Double youOweMeBalanceBefore;

// Balances after the entry
    private Double onCallBalanceAfter;
    private Double cashBalanceAfter;
    private Double cardBalanceAfter;
    private Double multiplePaymentMethodsBalanceAfter;
    private Double staffBalanceAfter;
    private Double creditBalanceAfter;
    private Double staffWelfareBalanceAfter;
    private Double voucherBalanceAfter;
    private Double iouBalanceAfter;
    private Double agentBalanceAfter;
    private Double chequeBalanceAfter;
    private Double slipBalanceAfter;
    private Double ewalletBalanceAfter;
    private Double patientDepositBalanceAfter;
    private Double patientPointsBalanceAfter;
    private Double onlineSettlementBalanceAfter;
    private Double noneBalanceAfter;
    private Double youOweMeBalanceAfter;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CashBookEntry)) {
            return false;
        }
        CashBookEntry other = (CashBookEntry) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.cashTransaction.CashBookEntry[ id=" + id + " ]";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public CashBook getCashBook() {
        return cashBook;
    }

    public void setCashBook(CashBook cashBook) {
        this.cashBook = cashBook;
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

    public WebUser getCreater() {
        return creater;
    }

    public void setCreater(WebUser creater) {
        this.creater = creater;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public WebUser getRetirer() {
        return retirer;
    }

    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

    public WebUser getEditer() {
        return editer;
    }

    public void setEditer(WebUser editer) {
        this.editer = editer;
    }

    public Date getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Date editedAt) {
        this.editedAt = editedAt;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getEntryValue() {
        return entryValue;
    }

    public void setEntryValue(Double entryValue) {
        this.entryValue = entryValue;
    }

    public Double getInstitutionBalance() {
        return institutionBalance;
    }

    public void setInstitutionBalance(Double institutionBalance) {
        this.institutionBalance = institutionBalance;
    }

    public Double getDepartmentBalance() {
        return departmentBalance;
    }

    public void setDepartmentBalance(Double departmentBalance) {
        this.departmentBalance = departmentBalance;
    }

    public Double getSiteBalance() {
        return siteBalance;
    }

    public void setSiteBalance(Double siteBalance) {
        this.siteBalance = siteBalance;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public Date getCashbookDate() {
        return cashbookDate;
    }

    public void setCashbookDate(Date cashbookDate) {
        this.cashbookDate = cashbookDate;
    }

    public Double getOnCallBalanceBefore() {
        return onCallBalanceBefore;
    }

    public void setOnCallBalanceBefore(Double onCallBalanceBefore) {
        this.onCallBalanceBefore = onCallBalanceBefore;
    }

    public Double getCashBalanceBefore() {
        return cashBalanceBefore;
    }

    public void setCashBalanceBefore(Double cashBalanceBefore) {
        this.cashBalanceBefore = cashBalanceBefore;
    }

    public Double getCardBalanceBefore() {
        return cardBalanceBefore;
    }

    public void setCardBalanceBefore(Double cardBalanceBefore) {
        this.cardBalanceBefore = cardBalanceBefore;
    }

    public Double getMultiplePaymentMethodsBalanceBefore() {
        return multiplePaymentMethodsBalanceBefore;
    }

    public void setMultiplePaymentMethodsBalanceBefore(Double multiplePaymentMethodsBalanceBefore) {
        this.multiplePaymentMethodsBalanceBefore = multiplePaymentMethodsBalanceBefore;
    }

    public Double getStaffBalanceBefore() {
        return staffBalanceBefore;
    }

    public void setStaffBalanceBefore(Double staffBalanceBefore) {
        this.staffBalanceBefore = staffBalanceBefore;
    }

    public Double getCreditBalanceBefore() {
        return creditBalanceBefore;
    }

    public void setCreditBalanceBefore(Double creditBalanceBefore) {
        this.creditBalanceBefore = creditBalanceBefore;
    }

    public Double getStaffWelfareBalanceBefore() {
        return staffWelfareBalanceBefore;
    }

    public void setStaffWelfareBalanceBefore(Double staffWelfareBalanceBefore) {
        this.staffWelfareBalanceBefore = staffWelfareBalanceBefore;
    }

    public Double getVoucherBalanceBefore() {
        return voucherBalanceBefore;
    }

    public void setVoucherBalanceBefore(Double voucherBalanceBefore) {
        this.voucherBalanceBefore = voucherBalanceBefore;
    }

    public Double getIouBalanceBefore() {
        return iouBalanceBefore;
    }

    public void setIouBalanceBefore(Double iouBalanceBefore) {
        this.iouBalanceBefore = iouBalanceBefore;
    }

    public Double getAgentBalanceBefore() {
        return agentBalanceBefore;
    }

    public void setAgentBalanceBefore(Double agentBalanceBefore) {
        this.agentBalanceBefore = agentBalanceBefore;
    }

    public Double getChequeBalanceBefore() {
        return chequeBalanceBefore;
    }

    public void setChequeBalanceBefore(Double chequeBalanceBefore) {
        this.chequeBalanceBefore = chequeBalanceBefore;
    }

    public Double getSlipBalanceBefore() {
        return slipBalanceBefore;
    }

    public void setSlipBalanceBefore(Double slipBalanceBefore) {
        this.slipBalanceBefore = slipBalanceBefore;
    }

    public Double getEwalletBalanceBefore() {
        return ewalletBalanceBefore;
    }

    public void setEwalletBalanceBefore(Double ewalletBalanceBefore) {
        this.ewalletBalanceBefore = ewalletBalanceBefore;
    }

    public Double getPatientDepositBalanceBefore() {
        return patientDepositBalanceBefore;
    }

    public void setPatientDepositBalanceBefore(Double patientDepositBalanceBefore) {
        this.patientDepositBalanceBefore = patientDepositBalanceBefore;
    }

    public Double getPatientPointsBalanceBefore() {
        return patientPointsBalanceBefore;
    }

    public void setPatientPointsBalanceBefore(Double patientPointsBalanceBefore) {
        this.patientPointsBalanceBefore = patientPointsBalanceBefore;
    }

    public Double getOnlineSettlementBalanceBefore() {
        return onlineSettlementBalanceBefore;
    }

    public void setOnlineSettlementBalanceBefore(Double onlineSettlementBalanceBefore) {
        this.onlineSettlementBalanceBefore = onlineSettlementBalanceBefore;
    }

    public Double getNoneBalanceBefore() {
        return noneBalanceBefore;
    }

    public void setNoneBalanceBefore(Double noneBalanceBefore) {
        this.noneBalanceBefore = noneBalanceBefore;
    }

    public Double getYouOweMeBalanceBefore() {
        return youOweMeBalanceBefore;
    }

    public void setYouOweMeBalanceBefore(Double youOweMeBalanceBefore) {
        this.youOweMeBalanceBefore = youOweMeBalanceBefore;
    }

    public Double getOnCallBalanceAfter() {
        return onCallBalanceAfter;
    }

    public void setOnCallBalanceAfter(Double onCallBalanceAfter) {
        this.onCallBalanceAfter = onCallBalanceAfter;
    }

    public Double getCashBalanceAfter() {
        return cashBalanceAfter;
    }

    public void setCashBalanceAfter(Double cashBalanceAfter) {
        this.cashBalanceAfter = cashBalanceAfter;
    }

    public Double getCardBalanceAfter() {
        return cardBalanceAfter;
    }

    public void setCardBalanceAfter(Double cardBalanceAfter) {
        this.cardBalanceAfter = cardBalanceAfter;
    }

    public Double getMultiplePaymentMethodsBalanceAfter() {
        return multiplePaymentMethodsBalanceAfter;
    }

    public void setMultiplePaymentMethodsBalanceAfter(Double multiplePaymentMethodsBalanceAfter) {
        this.multiplePaymentMethodsBalanceAfter = multiplePaymentMethodsBalanceAfter;
    }

    public Double getStaffBalanceAfter() {
        return staffBalanceAfter;
    }

    public void setStaffBalanceAfter(Double staffBalanceAfter) {
        this.staffBalanceAfter = staffBalanceAfter;
    }

    public Double getCreditBalanceAfter() {
        return creditBalanceAfter;
    }

    public void setCreditBalanceAfter(Double creditBalanceAfter) {
        this.creditBalanceAfter = creditBalanceAfter;
    }

    public Double getStaffWelfareBalanceAfter() {
        return staffWelfareBalanceAfter;
    }

    public void setStaffWelfareBalanceAfter(Double staffWelfareBalanceAfter) {
        this.staffWelfareBalanceAfter = staffWelfareBalanceAfter;
    }

    public Double getVoucherBalanceAfter() {
        return voucherBalanceAfter;
    }

    public void setVoucherBalanceAfter(Double voucherBalanceAfter) {
        this.voucherBalanceAfter = voucherBalanceAfter;
    }

    public Double getIouBalanceAfter() {
        return iouBalanceAfter;
    }

    public void setIouBalanceAfter(Double iouBalanceAfter) {
        this.iouBalanceAfter = iouBalanceAfter;
    }

    public Double getAgentBalanceAfter() {
        return agentBalanceAfter;
    }

    public void setAgentBalanceAfter(Double agentBalanceAfter) {
        this.agentBalanceAfter = agentBalanceAfter;
    }

    public Double getChequeBalanceAfter() {
        return chequeBalanceAfter;
    }

    public void setChequeBalanceAfter(Double chequeBalanceAfter) {
        this.chequeBalanceAfter = chequeBalanceAfter;
    }

    public Double getSlipBalanceAfter() {
        return slipBalanceAfter;
    }

    public void setSlipBalanceAfter(Double slipBalanceAfter) {
        this.slipBalanceAfter = slipBalanceAfter;
    }

    public Double getEwalletBalanceAfter() {
        return ewalletBalanceAfter;
    }

    public void setEwalletBalanceAfter(Double ewalletBalanceAfter) {
        this.ewalletBalanceAfter = ewalletBalanceAfter;
    }

    public Double getPatientDepositBalanceAfter() {
        return patientDepositBalanceAfter;
    }

    public void setPatientDepositBalanceAfter(Double patientDepositBalanceAfter) {
        this.patientDepositBalanceAfter = patientDepositBalanceAfter;
    }

    public Double getPatientPointsBalanceAfter() {
        return patientPointsBalanceAfter;
    }

    public void setPatientPointsBalanceAfter(Double patientPointsBalanceAfter) {
        this.patientPointsBalanceAfter = patientPointsBalanceAfter;
    }

    public Double getOnlineSettlementBalanceAfter() {
        return onlineSettlementBalanceAfter;
    }

    public void setOnlineSettlementBalanceAfter(Double onlineSettlementBalanceAfter) {
        this.onlineSettlementBalanceAfter = onlineSettlementBalanceAfter;
    }

    public Double getNoneBalanceAfter() {
        return noneBalanceAfter;
    }

    public void setNoneBalanceAfter(Double noneBalanceAfter) {
        this.noneBalanceAfter = noneBalanceAfter;
    }

    public Double getYouOweMeBalanceAfter() {
        return youOweMeBalanceAfter;
    }

    public void setYouOweMeBalanceAfter(Double youOweMeBalanceAfter) {
        this.youOweMeBalanceAfter = youOweMeBalanceAfter;
    }

    
    
}
