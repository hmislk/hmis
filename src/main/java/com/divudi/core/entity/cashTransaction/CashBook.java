package com.divudi.core.entity.cashTransaction;

import com.divudi.core.entity.RetirableEntity;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author Lawan Chaamindu
 */
@Entity
public class CashBook implements Serializable, RetirableEntity  {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String Name;
    @ManyToOne
    private Institution institution;
    @ManyToOne
    private Institution site;
    @ManyToOne
    private Department department;

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

        // Individual attributes for each PaymentMethod
    private Double onCallBalance = 0.0;
    private Double cashBalance = 0.0;
    private Double cardBalance = 0.0;
    private Double multiplePaymentMethodsBalance = 0.0;
    private Double staffBalance = 0.0;
    private Double creditBalance = 0.0;
    private Double staffWelfareBalance = 0.0;
    private Double voucherBalance = 0.0;
    private Double iouBalance = 0.0;
    private Double agentBalance = 0.0;
    private Double chequeBalance = 0.0;
    private Double slipBalance = 0.0;
    private Double ewalletBalance = 0.0;
    private Double patientDepositBalance = 0.0;
    private Double patientPointsBalance = 0.0;
    private Double onlineSettlementBalance = 0.0;
    private Double noneBalance = 0.0;
    private Double youOweMeBalance = 0.0;

    //Editer Properties
    @ManyToOne
    private WebUser editer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date editedAt;

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
        if (!(object instanceof CashBook)) {
            return false;
        }
        CashBook other = (CashBook) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.cashTransaction.CashBook[ id=" + id + " ]";
    }

    public String getName() {
        return Name;
    }

    public void setName(String Name) {
        this.Name = Name;
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

    public Double getOnCallBalance() {
        return onCallBalance;
    }

    public void setOnCallBalance(Double onCallBalance) {
        this.onCallBalance = onCallBalance;
    }

    public Double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(Double cashBalance) {
        this.cashBalance = cashBalance;
    }

    public Double getCardBalance() {
        return cardBalance;
    }

    public void setCardBalance(Double cardBalance) {
        this.cardBalance = cardBalance;
    }

    public Double getMultiplePaymentMethodsBalance() {
        return multiplePaymentMethodsBalance;
    }

    public void setMultiplePaymentMethodsBalance(Double multiplePaymentMethodsBalance) {
        this.multiplePaymentMethodsBalance = multiplePaymentMethodsBalance;
    }

    public Double getStaffBalance() {
        return staffBalance;
    }

    public void setStaffBalance(Double staffBalance) {
        this.staffBalance = staffBalance;
    }

    public Double getCreditBalance() {
        return creditBalance;
    }

    public void setCreditBalance(Double creditBalance) {
        this.creditBalance = creditBalance;
    }

    public Double getStaffWelfareBalance() {
        return staffWelfareBalance;
    }

    public void setStaffWelfareBalance(Double staffWelfareBalance) {
        this.staffWelfareBalance = staffWelfareBalance;
    }

    public Double getVoucherBalance() {
        return voucherBalance;
    }

    public void setVoucherBalance(Double voucherBalance) {
        this.voucherBalance = voucherBalance;
    }

    public Double getIouBalance() {
        return iouBalance;
    }

    public void setIouBalance(Double iouBalance) {
        this.iouBalance = iouBalance;
    }

    public Double getAgentBalance() {
        return agentBalance;
    }

    public void setAgentBalance(Double agentBalance) {
        this.agentBalance = agentBalance;
    }

    public Double getChequeBalance() {
        return chequeBalance;
    }

    public void setChequeBalance(Double chequeBalance) {
        this.chequeBalance = chequeBalance;
    }

    public Double getSlipBalance() {
        return slipBalance;
    }

    public void setSlipBalance(Double slipBalance) {
        this.slipBalance = slipBalance;
    }

    public Double getEwalletBalance() {
        return ewalletBalance;
    }

    public void setEwalletBalance(Double ewalletBalance) {
        this.ewalletBalance = ewalletBalance;
    }

    public Double getPatientDepositBalance() {
        return patientDepositBalance;
    }

    public void setPatientDepositBalance(Double patientDepositBalance) {
        this.patientDepositBalance = patientDepositBalance;
    }

    public Double getPatientPointsBalance() {
        return patientPointsBalance;
    }

    public void setPatientPointsBalance(Double patientPointsBalance) {
        this.patientPointsBalance = patientPointsBalance;
    }

    public Double getOnlineSettlementBalance() {
        return onlineSettlementBalance;
    }

    public void setOnlineSettlementBalance(Double onlineSettlementBalance) {
        this.onlineSettlementBalance = onlineSettlementBalance;
    }

    public Double getNoneBalance() {
        return noneBalance;
    }

    public void setNoneBalance(Double noneBalance) {
        this.noneBalance = noneBalance;
    }

    public Double getYouOweMeBalance() {
        return youOweMeBalance;
    }

    public void setYouOweMeBalance(Double youOweMeBalance) {
        this.youOweMeBalance = youOweMeBalance;
    }

}
