package com.divudi.core.entity.cashTransaction;

import com.divudi.core.entity.RetirableEntity;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
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
public class CashBookEntry implements Serializable, RetirableEntity  {

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

    private double entryValue;

    private double fromInstitutionBalanceBefore;
    private double fromDepartmentBalanceBefore;
    private double fromSiteBalanceBefore;

    private double toInstitutionBalanceBefore;
    private double toDepartmentBalanceBefore;
    private double toSiteBalanceBefore;

    private double fromInstitutionBalanceAfter;
    private double fromDepartmentBalanceAfter;
    private double fromSiteBalanceAfter;

    private double toInstitutionBalanceAfter;
    private double toDepartmentBalanceAfter;
    private double toSiteBalanceAfter;

    @ManyToOne
    private WebUser webUser;

    @ManyToOne
    private Institution institution;
    @ManyToOne
    private Institution site;
    @ManyToOne
    private Department department;

    @ManyToOne
    private Institution fromInstitution;
    @ManyToOne
    private Institution fromSite;
    @ManyToOne
    private Department fromDepartment;

    @ManyToOne
    private Institution toInstitution;
    @ManyToOne
    private Institution toSite;
    @ManyToOne
    private Department toDepartment;

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

    private double fromDepartmentCashBalanceBefore;
    private double fromDepartmentCardBalanceBefore;
    private double fromDepartmentAgentBalanceBefore;
    private double fromDepartmentChequeBalanceBefore;
    private double fromDepartmentCreditBalanceBefore;
    private double fromDepartmentIouBalanceBefore;
    private double fromDepartmentMultiplePaymentMethodsBalanceBefore;
    private double fromDepartmentOnCallBalanceBefore;
    private double fromDepartmentOnlineSettlementBalanceBefore;
    private double fromDepartmentPatientDepositBalanceBefore;
    private double fromDepartmentPatientPointsBalanceBefore;
    private double fromDepartmentSlipBalanceBefore;
    private double fromDepartmentStaffBalanceBefore;
    private double fromDepartmentStaffWelfareBalanceBefore;
    private double fromDepartmentVoucherBalanceBefore;
    private double fromDepartmentEwalletBalanceBefore;

    private double toDepartmentCashBalanceBefore;
    private double toDepartmentCardBalanceBefore;
    private double toDepartmentAgentBalanceBefore;
    private double toDepartmentChequeBalanceBefore;
    private double toDepartmentCreditBalanceBefore;
    private double toDepartmentIouBalanceBefore;
    private double toDepartmentMultiplePaymentMethodsBalanceBefore;
    private double toDepartmentOnCallBalanceBefore;
    private double toDepartmentOnlineSettlementBalanceBefore;
    private double toDepartmentPatientDepositBalanceBefore;
    private double toDepartmentPatientPointsBalanceBefore;
    private double toDepartmentSlipBalanceBefore;
    private double toDepartmentStaffBalanceBefore;
    private double toDepartmentStaffWelfareBalanceBefore;
    private double toDepartmentVoucherBalanceBefore;
    private double toDepartmentEwalletBalanceBefore;

    private double fromDepartmentCashBalanceAfter;
    private double fromDepartmentCardBalanceAfter;
    private double fromDepartmentAgentBalanceAfter;
    private double fromDepartmentChequeBalanceAfter;
    private double fromDepartmentCreditBalanceAfter;
    private double fromDepartmentIouBalanceAfter;
    private double fromDepartmentMultiplePaymentMethodsBalanceAfter;
    private double fromDepartmentOnCallBalanceAfter;
    private double fromDepartmentOnlineSettlementBalanceAfter;
    private double fromDepartmentPatientDepositBalanceAfter;
    private double fromDepartmentPatientPointsBalanceAfter;
    private double fromDepartmentSlipBalanceAfter;
    private double fromDepartmentStaffBalanceAfter;
    private double fromDepartmentStaffWelfareBalanceAfter;
    private double fromDepartmentVoucherBalanceAfter;
    private double fromDepartmentEwalletBalanceAfter;

    private double toDepartmentCashBalanceAfter;
    private double toDepartmentCardBalanceAfter;
    private double toDepartmentAgentBalanceAfter;
    private double toDepartmentChequeBalanceAfter;
    private double toDepartmentCreditBalanceAfter;
    private double toDepartmentIouBalanceAfter;
    private double toDepartmentMultiplePaymentMethodsBalanceAfter;
    private double toDepartmentOnCallBalanceAfter;
    private double toDepartmentOnlineSettlementBalanceAfter;
    private double toDepartmentPatientDepositBalanceAfter;
    private double toDepartmentPatientPointsBalanceAfter;
    private double toDepartmentSlipBalanceAfter;
    private double toDepartmentStaffBalanceAfter;
    private double toDepartmentStaffWelfareBalanceAfter;
    private double toDepartmentVoucherBalanceAfter;
    private double toDepartmentEwalletBalanceAfter;

    private double fromInstitutionCashBalanceBefore;
    private double fromInstitutionCardBalanceBefore;
    private double fromInstitutionAgentBalanceBefore;
    private double fromInstitutionChequeBalanceBefore;
    private double fromInstitutionCreditBalanceBefore;
    private double fromInstitutionIouBalanceBefore;
    private double fromInstitutionMultiplePaymentMethodsBalanceBefore;
    private double fromInstitutionOnCallBalanceBefore;
    private double fromInstitutionOnlineSettlementBalanceBefore;
    private double fromInstitutionPatientDepositBalanceBefore;
    private double fromInstitutionPatientPointsBalanceBefore;
    private double fromInstitutionSlipBalanceBefore;
    private double fromInstitutionStaffBalanceBefore;
    private double fromInstitutionStaffWelfareBalanceBefore;
    private double fromInstitutionVoucherBalanceBefore;
    private double fromInstitutionEwalletBalanceBefore;

    private double toInstitutionCashBalanceBefore;
    private double toInstitutionCardBalanceBefore;
    private double toInstitutionAgentBalanceBefore;
    private double toInstitutionChequeBalanceBefore;
    private double toInstitutionCreditBalanceBefore;
    private double toInstitutionIouBalanceBefore;
    private double toInstitutionMultiplePaymentMethodsBalanceBefore;
    private double toInstitutionOnCallBalanceBefore;
    private double toInstitutionOnlineSettlementBalanceBefore;
    private double toInstitutionPatientDepositBalanceBefore;
    private double toInstitutionPatientPointsBalanceBefore;
    private double toInstitutionSlipBalanceBefore;
    private double toInstitutionStaffBalanceBefore;
    private double toInstitutionStaffWelfareBalanceBefore;
    private double toInstitutionVoucherBalanceBefore;
    private double toInstitutionEwalletBalanceBefore;

    private double fromInstitutionCashBalanceAfter;
    private double fromInstitutionCardBalanceAfter;
    private double fromInstitutionAgentBalanceAfter;
    private double fromInstitutionChequeBalanceAfter;
    private double fromInstitutionCreditBalanceAfter;
    private double fromInstitutionIouBalanceAfter;
    private double fromInstitutionMultiplePaymentMethodsBalanceAfter;
    private double fromInstitutionOnCallBalanceAfter;
    private double fromInstitutionOnlineSettlementBalanceAfter;
    private double fromInstitutionPatientDepositBalanceAfter;
    private double fromInstitutionPatientPointsBalanceAfter;
    private double fromInstitutionSlipBalanceAfter;
    private double fromInstitutionStaffBalanceAfter;
    private double fromInstitutionStaffWelfareBalanceAfter;
    private double fromInstitutionVoucherBalanceAfter;
    private double fromInstitutionEwalletBalanceAfter;

    private double toInstitutionCashBalanceAfter;
    private double toInstitutionCardBalanceAfter;
    private double toInstitutionAgentBalanceAfter;
    private double toInstitutionChequeBalanceAfter;
    private double toInstitutionCreditBalanceAfter;
    private double toInstitutionIouBalanceAfter;
    private double toInstitutionMultiplePaymentMethodsBalanceAfter;
    private double toInstitutionOnCallBalanceAfter;
    private double toInstitutionOnlineSettlementBalanceAfter;
    private double toInstitutionPatientDepositBalanceAfter;
    private double toInstitutionPatientPointsBalanceAfter;
    private double toInstitutionSlipBalanceAfter;
    private double toInstitutionStaffBalanceAfter;
    private double toInstitutionStaffWelfareBalanceAfter;
    private double toInstitutionVoucherBalanceAfter;
    private double toInstitutionEwalletBalanceAfter;

    private double fromSiteCashBalanceBefore;
    private double fromSiteCardBalanceBefore;
    private double fromSiteAgentBalanceBefore;
    private double fromSiteChequeBalanceBefore;
    private double fromSiteCreditBalanceBefore;
    private double fromSiteIouBalanceBefore;
    private double fromSiteMultiplePaymentMethodsBalanceBefore;
    private double fromSiteOnCallBalanceBefore;
    private double fromSiteOnlineSettlementBalanceBefore;
    private double fromSitePatientDepositBalanceBefore;
    private double fromSitePatientPointsBalanceBefore;
    private double fromSiteSlipBalanceBefore;
    private double fromSiteStaffBalanceBefore;
    private double fromSiteStaffWelfareBalanceBefore;
    private double fromSiteVoucherBalanceBefore;
    private double fromSiteEwalletBalanceBefore;

    private double toSiteCashBalanceBefore;
    private double toSiteCardBalanceBefore;
    private double toSiteAgentBalanceBefore;
    private double toSiteChequeBalanceBefore;
    private double toSiteCreditBalanceBefore;
    private double toSiteIouBalanceBefore;
    private double toSiteMultiplePaymentMethodsBalanceBefore;
    private double toSiteOnCallBalanceBefore;
    private double toSiteOnlineSettlementBalanceBefore;
    private double toSitePatientDepositBalanceBefore;
    private double toSitePatientPointsBalanceBefore;
    private double toSiteSlipBalanceBefore;
    private double toSiteStaffBalanceBefore;
    private double toSiteStaffWelfareBalanceBefore;
    private double toSiteVoucherBalanceBefore;
    private double toSiteEwalletBalanceBefore;

    private double fromSiteCashBalanceAfter;
    private double fromSiteCardBalanceAfter;
    private double fromSiteAgentBalanceAfter;
    private double fromSiteChequeBalanceAfter;
    private double fromSiteCreditBalanceAfter;
    private double fromSiteIouBalanceAfter;
    private double fromSiteMultiplePaymentMethodsBalanceAfter;
    private double fromSiteOnCallBalanceAfter;
    private double fromSiteOnlineSettlementBalanceAfter;
    private double fromSitePatientDepositBalanceAfter;
    private double fromSitePatientPointsBalanceAfter;
    private double fromSiteSlipBalanceAfter;
    private double fromSiteStaffBalanceAfter;
    private double fromSiteStaffWelfareBalanceAfter;
    private double fromSiteVoucherBalanceAfter;
    private double fromSiteEwalletBalanceAfter;

    private double toSiteCashBalanceAfter;
    private double toSiteCardBalanceAfter;
    private double toSiteAgentBalanceAfter;
    private double toSiteChequeBalanceAfter;
    private double toSiteCreditBalanceAfter;
    private double toSiteIouBalanceAfter;
    private double toSiteMultiplePaymentMethodsBalanceAfter;
    private double toSiteOnCallBalanceAfter;
    private double toSiteOnlineSettlementBalanceAfter;
    private double toSitePatientDepositBalanceAfter;
    private double toSitePatientPointsBalanceAfter;
    private double toSiteSlipBalanceAfter;
    private double toSiteStaffBalanceAfter;
    private double toSiteStaffWelfareBalanceAfter;
    private double toSiteVoucherBalanceAfter;
    private double toSiteEwalletBalanceAfter;

    private double cashValue;
    private double cardValue;
    private double agentValue;
    private double chequeValue;
    private double creditValue;
    private double iouValue;
    private double multiplePaymentMethodsValue;
    private double onCallValue;
    private double onlineSettlementValue;
    private double patientDepositValue;
    private double patientPointsValue;
    private double slipValue;
    private double staffValue;
    private double staffWelfareValue;
    private double voucherValue;
    private double ewalletValue;

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
        return "com.divudi.core.entity.cashTransaction.CashBookEntry[ id=" + id + " ]";
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

    public double getEntryValue() {
        return entryValue;
    }

    public void setEntryValue(double entryValue) {
        this.entryValue = entryValue;
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

    public double getFromInstitutionBalanceBefore() {
        return fromInstitutionBalanceBefore;
    }

    public void setFromInstitutionBalanceBefore(double fromInstitutionBalanceBefore) {
        this.fromInstitutionBalanceBefore = fromInstitutionBalanceBefore;
    }

    public double getFromDepartmentBalanceBefore() {
        return fromDepartmentBalanceBefore;
    }

    public void setFromDepartmentBalanceBefore(double fromDepartmentBalanceBefore) {
        this.fromDepartmentBalanceBefore = fromDepartmentBalanceBefore;
    }

    public double getFromSiteBalanceBefore() {
        return fromSiteBalanceBefore;
    }

    public void setFromSiteBalanceBefore(double fromSiteBalanceBefore) {
        this.fromSiteBalanceBefore = fromSiteBalanceBefore;
    }

    public double getToInstitutionBalanceBefore() {
        return toInstitutionBalanceBefore;
    }

    public void setToInstitutionBalanceBefore(double toInstitutionBalanceBefore) {
        this.toInstitutionBalanceBefore = toInstitutionBalanceBefore;
    }

    public double getToDepartmentBalanceBefore() {
        return toDepartmentBalanceBefore;
    }

    public void setToDepartmentBalanceBefore(double toDepartmentBalanceBefore) {
        this.toDepartmentBalanceBefore = toDepartmentBalanceBefore;
    }

    public double getToSiteBalanceBefore() {
        return toSiteBalanceBefore;
    }

    public void setToSiteBalanceBefore(double toSiteBalanceBefore) {
        this.toSiteBalanceBefore = toSiteBalanceBefore;
    }

    public double getFromInstitutionBalanceAfter() {
        return fromInstitutionBalanceAfter;
    }

    public void setFromInstitutionBalanceAfter(double fromInstitutionBalanceAfter) {
        this.fromInstitutionBalanceAfter = fromInstitutionBalanceAfter;
    }

    public double getFromDepartmentBalanceAfter() {
        return fromDepartmentBalanceAfter;
    }

    public void setFromDepartmentBalanceAfter(double fromDepartmentBalanceAfter) {
        this.fromDepartmentBalanceAfter = fromDepartmentBalanceAfter;
    }

    public double getFromSiteBalanceAfter() {
        return fromSiteBalanceAfter;
    }

    public void setFromSiteBalanceAfter(double fromSiteBalanceAfter) {
        this.fromSiteBalanceAfter = fromSiteBalanceAfter;
    }

    public double getToInstitutionBalanceAfter() {
        return toInstitutionBalanceAfter;
    }

    public void setToInstitutionBalanceAfter(double toInstitutionBalanceAfter) {
        this.toInstitutionBalanceAfter = toInstitutionBalanceAfter;
    }

    public double getToDepartmentBalanceAfter() {
        return toDepartmentBalanceAfter;
    }

    public void setToDepartmentBalanceAfter(double toDepartmentBalanceAfter) {
        this.toDepartmentBalanceAfter = toDepartmentBalanceAfter;
    }

    public double getToSiteBalanceAfter() {
        return toSiteBalanceAfter;
    }

    public void setToSiteBalanceAfter(double toSiteBalanceAfter) {
        this.toSiteBalanceAfter = toSiteBalanceAfter;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getFromSite() {
        return fromSite;
    }

    public void setFromSite(Institution fromSite) {
        this.fromSite = fromSite;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Institution getToSite() {
        return toSite;
    }

    public void setToSite(Institution toSite) {
        this.toSite = toSite;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public double getFromDepartmentCashBalanceBefore() {
        return fromDepartmentCashBalanceBefore;
    }

    public void setFromDepartmentCashBalanceBefore(double fromDepartmentCashBalanceBefore) {
        this.fromDepartmentCashBalanceBefore = fromDepartmentCashBalanceBefore;
    }

    public double getFromDepartmentCardBalanceBefore() {
        return fromDepartmentCardBalanceBefore;
    }

    public void setFromDepartmentCardBalanceBefore(double fromDepartmentCardBalanceBefore) {
        this.fromDepartmentCardBalanceBefore = fromDepartmentCardBalanceBefore;
    }

    public double getFromDepartmentAgentBalanceBefore() {
        return fromDepartmentAgentBalanceBefore;
    }

    public void setFromDepartmentAgentBalanceBefore(double fromDepartmentAgentBalanceBefore) {
        this.fromDepartmentAgentBalanceBefore = fromDepartmentAgentBalanceBefore;
    }

    public double getFromDepartmentChequeBalanceBefore() {
        return fromDepartmentChequeBalanceBefore;
    }

    public void setFromDepartmentChequeBalanceBefore(double fromDepartmentChequeBalanceBefore) {
        this.fromDepartmentChequeBalanceBefore = fromDepartmentChequeBalanceBefore;
    }

    public double getFromDepartmentCreditBalanceBefore() {
        return fromDepartmentCreditBalanceBefore;
    }

    public void setFromDepartmentCreditBalanceBefore(double fromDepartmentCreditBalanceBefore) {
        this.fromDepartmentCreditBalanceBefore = fromDepartmentCreditBalanceBefore;
    }

    public double getFromDepartmentIouBalanceBefore() {
        return fromDepartmentIouBalanceBefore;
    }

    public void setFromDepartmentIouBalanceBefore(double fromDepartmentIouBalanceBefore) {
        this.fromDepartmentIouBalanceBefore = fromDepartmentIouBalanceBefore;
    }

    public double getFromDepartmentMultiplePaymentMethodsBalanceBefore() {
        return fromDepartmentMultiplePaymentMethodsBalanceBefore;
    }

    public void setFromDepartmentMultiplePaymentMethodsBalanceBefore(double fromDepartmentMultiplePaymentMethodsBalanceBefore) {
        this.fromDepartmentMultiplePaymentMethodsBalanceBefore = fromDepartmentMultiplePaymentMethodsBalanceBefore;
    }

    public double getFromDepartmentOnCallBalanceBefore() {
        return fromDepartmentOnCallBalanceBefore;
    }

    public void setFromDepartmentOnCallBalanceBefore(double fromDepartmentOnCallBalanceBefore) {
        this.fromDepartmentOnCallBalanceBefore = fromDepartmentOnCallBalanceBefore;
    }

    public double getFromDepartmentOnlineSettlementBalanceBefore() {
        return fromDepartmentOnlineSettlementBalanceBefore;
    }

    public void setFromDepartmentOnlineSettlementBalanceBefore(double fromDepartmentOnlineSettlementBalanceBefore) {
        this.fromDepartmentOnlineSettlementBalanceBefore = fromDepartmentOnlineSettlementBalanceBefore;
    }

    public double getFromDepartmentPatientDepositBalanceBefore() {
        return fromDepartmentPatientDepositBalanceBefore;
    }

    public void setFromDepartmentPatientDepositBalanceBefore(double fromDepartmentPatientDepositBalanceBefore) {
        this.fromDepartmentPatientDepositBalanceBefore = fromDepartmentPatientDepositBalanceBefore;
    }

    public double getFromDepartmentPatientPointsBalanceBefore() {
        return fromDepartmentPatientPointsBalanceBefore;
    }

    public void setFromDepartmentPatientPointsBalanceBefore(double fromDepartmentPatientPointsBalanceBefore) {
        this.fromDepartmentPatientPointsBalanceBefore = fromDepartmentPatientPointsBalanceBefore;
    }

    public double getFromDepartmentSlipBalanceBefore() {
        return fromDepartmentSlipBalanceBefore;
    }

    public void setFromDepartmentSlipBalanceBefore(double fromDepartmentSlipBalanceBefore) {
        this.fromDepartmentSlipBalanceBefore = fromDepartmentSlipBalanceBefore;
    }

    public double getFromDepartmentStaffBalanceBefore() {
        return fromDepartmentStaffBalanceBefore;
    }

    public void setFromDepartmentStaffBalanceBefore(double fromDepartmentStaffBalanceBefore) {
        this.fromDepartmentStaffBalanceBefore = fromDepartmentStaffBalanceBefore;
    }

    public double getFromDepartmentStaffWelfareBalanceBefore() {
        return fromDepartmentStaffWelfareBalanceBefore;
    }

    public void setFromDepartmentStaffWelfareBalanceBefore(double fromDepartmentStaffWelfareBalanceBefore) {
        this.fromDepartmentStaffWelfareBalanceBefore = fromDepartmentStaffWelfareBalanceBefore;
    }

    public double getFromDepartmentVoucherBalanceBefore() {
        return fromDepartmentVoucherBalanceBefore;
    }

    public void setFromDepartmentVoucherBalanceBefore(double fromDepartmentVoucherBalanceBefore) {
        this.fromDepartmentVoucherBalanceBefore = fromDepartmentVoucherBalanceBefore;
    }

    public double getFromDepartmentEwalletBalanceBefore() {
        return fromDepartmentEwalletBalanceBefore;
    }

    public void setFromDepartmentEwalletBalanceBefore(double fromDepartmentEwalletBalanceBefore) {
        this.fromDepartmentEwalletBalanceBefore = fromDepartmentEwalletBalanceBefore;
    }

    public double getToDepartmentCashBalanceBefore() {
        return toDepartmentCashBalanceBefore;
    }

    public void setToDepartmentCashBalanceBefore(double toDepartmentCashBalanceBefore) {
        this.toDepartmentCashBalanceBefore = toDepartmentCashBalanceBefore;
    }

    public double getToDepartmentCardBalanceBefore() {
        return toDepartmentCardBalanceBefore;
    }

    public void setToDepartmentCardBalanceBefore(double toDepartmentCardBalanceBefore) {
        this.toDepartmentCardBalanceBefore = toDepartmentCardBalanceBefore;
    }

    public double getToDepartmentAgentBalanceBefore() {
        return toDepartmentAgentBalanceBefore;
    }

    public void setToDepartmentAgentBalanceBefore(double toDepartmentAgentBalanceBefore) {
        this.toDepartmentAgentBalanceBefore = toDepartmentAgentBalanceBefore;
    }

    public double getToDepartmentChequeBalanceBefore() {
        return toDepartmentChequeBalanceBefore;
    }

    public void setToDepartmentChequeBalanceBefore(double toDepartmentChequeBalanceBefore) {
        this.toDepartmentChequeBalanceBefore = toDepartmentChequeBalanceBefore;
    }

    public double getToDepartmentCreditBalanceBefore() {
        return toDepartmentCreditBalanceBefore;
    }

    public void setToDepartmentCreditBalanceBefore(double toDepartmentCreditBalanceBefore) {
        this.toDepartmentCreditBalanceBefore = toDepartmentCreditBalanceBefore;
    }

    public double getToDepartmentIouBalanceBefore() {
        return toDepartmentIouBalanceBefore;
    }

    public void setToDepartmentIouBalanceBefore(double toDepartmentIouBalanceBefore) {
        this.toDepartmentIouBalanceBefore = toDepartmentIouBalanceBefore;
    }

    public double getToDepartmentMultiplePaymentMethodsBalanceBefore() {
        return toDepartmentMultiplePaymentMethodsBalanceBefore;
    }

    public void setToDepartmentMultiplePaymentMethodsBalanceBefore(double toDepartmentMultiplePaymentMethodsBalanceBefore) {
        this.toDepartmentMultiplePaymentMethodsBalanceBefore = toDepartmentMultiplePaymentMethodsBalanceBefore;
    }

    public double getToDepartmentOnCallBalanceBefore() {
        return toDepartmentOnCallBalanceBefore;
    }

    public void setToDepartmentOnCallBalanceBefore(double toDepartmentOnCallBalanceBefore) {
        this.toDepartmentOnCallBalanceBefore = toDepartmentOnCallBalanceBefore;
    }

    public double getToDepartmentOnlineSettlementBalanceBefore() {
        return toDepartmentOnlineSettlementBalanceBefore;
    }

    public void setToDepartmentOnlineSettlementBalanceBefore(double toDepartmentOnlineSettlementBalanceBefore) {
        this.toDepartmentOnlineSettlementBalanceBefore = toDepartmentOnlineSettlementBalanceBefore;
    }

    public double getToDepartmentPatientDepositBalanceBefore() {
        return toDepartmentPatientDepositBalanceBefore;
    }

    public void setToDepartmentPatientDepositBalanceBefore(double toDepartmentPatientDepositBalanceBefore) {
        this.toDepartmentPatientDepositBalanceBefore = toDepartmentPatientDepositBalanceBefore;
    }

    public double getToDepartmentPatientPointsBalanceBefore() {
        return toDepartmentPatientPointsBalanceBefore;
    }

    public void setToDepartmentPatientPointsBalanceBefore(double toDepartmentPatientPointsBalanceBefore) {
        this.toDepartmentPatientPointsBalanceBefore = toDepartmentPatientPointsBalanceBefore;
    }

    public double getToDepartmentSlipBalanceBefore() {
        return toDepartmentSlipBalanceBefore;
    }

    public void setToDepartmentSlipBalanceBefore(double toDepartmentSlipBalanceBefore) {
        this.toDepartmentSlipBalanceBefore = toDepartmentSlipBalanceBefore;
    }

    public double getToDepartmentStaffBalanceBefore() {
        return toDepartmentStaffBalanceBefore;
    }

    public void setToDepartmentStaffBalanceBefore(double toDepartmentStaffBalanceBefore) {
        this.toDepartmentStaffBalanceBefore = toDepartmentStaffBalanceBefore;
    }

    public double getToDepartmentStaffWelfareBalanceBefore() {
        return toDepartmentStaffWelfareBalanceBefore;
    }

    public void setToDepartmentStaffWelfareBalanceBefore(double toDepartmentStaffWelfareBalanceBefore) {
        this.toDepartmentStaffWelfareBalanceBefore = toDepartmentStaffWelfareBalanceBefore;
    }

    public double getToDepartmentVoucherBalanceBefore() {
        return toDepartmentVoucherBalanceBefore;
    }

    public void setToDepartmentVoucherBalanceBefore(double toDepartmentVoucherBalanceBefore) {
        this.toDepartmentVoucherBalanceBefore = toDepartmentVoucherBalanceBefore;
    }

    public double getToDepartmentEwalletBalanceBefore() {
        return toDepartmentEwalletBalanceBefore;
    }

    public void setToDepartmentEwalletBalanceBefore(double toDepartmentEwalletBalanceBefore) {
        this.toDepartmentEwalletBalanceBefore = toDepartmentEwalletBalanceBefore;
    }

    public double getFromDepartmentCashBalanceAfter() {
        return fromDepartmentCashBalanceAfter;
    }

    public void setFromDepartmentCashBalanceAfter(double fromDepartmentCashBalanceAfter) {
        this.fromDepartmentCashBalanceAfter = fromDepartmentCashBalanceAfter;
    }

    public double getFromDepartmentCardBalanceAfter() {
        return fromDepartmentCardBalanceAfter;
    }

    public void setFromDepartmentCardBalanceAfter(double fromDepartmentCardBalanceAfter) {
        this.fromDepartmentCardBalanceAfter = fromDepartmentCardBalanceAfter;
    }

    public double getFromDepartmentAgentBalanceAfter() {
        return fromDepartmentAgentBalanceAfter;
    }

    public void setFromDepartmentAgentBalanceAfter(double fromDepartmentAgentBalanceAfter) {
        this.fromDepartmentAgentBalanceAfter = fromDepartmentAgentBalanceAfter;
    }

    public double getFromDepartmentChequeBalanceAfter() {
        return fromDepartmentChequeBalanceAfter;
    }

    public void setFromDepartmentChequeBalanceAfter(double fromDepartmentChequeBalanceAfter) {
        this.fromDepartmentChequeBalanceAfter = fromDepartmentChequeBalanceAfter;
    }

    public double getFromDepartmentCreditBalanceAfter() {
        return fromDepartmentCreditBalanceAfter;
    }

    public void setFromDepartmentCreditBalanceAfter(double fromDepartmentCreditBalanceAfter) {
        this.fromDepartmentCreditBalanceAfter = fromDepartmentCreditBalanceAfter;
    }

    public double getFromDepartmentIouBalanceAfter() {
        return fromDepartmentIouBalanceAfter;
    }

    public void setFromDepartmentIouBalanceAfter(double fromDepartmentIouBalanceAfter) {
        this.fromDepartmentIouBalanceAfter = fromDepartmentIouBalanceAfter;
    }

    public double getFromDepartmentMultiplePaymentMethodsBalanceAfter() {
        return fromDepartmentMultiplePaymentMethodsBalanceAfter;
    }

    public void setFromDepartmentMultiplePaymentMethodsBalanceAfter(double fromDepartmentMultiplePaymentMethodsBalanceAfter) {
        this.fromDepartmentMultiplePaymentMethodsBalanceAfter = fromDepartmentMultiplePaymentMethodsBalanceAfter;
    }

    public double getFromDepartmentOnCallBalanceAfter() {
        return fromDepartmentOnCallBalanceAfter;
    }

    public void setFromDepartmentOnCallBalanceAfter(double fromDepartmentOnCallBalanceAfter) {
        this.fromDepartmentOnCallBalanceAfter = fromDepartmentOnCallBalanceAfter;
    }

    public double getFromDepartmentOnlineSettlementBalanceAfter() {
        return fromDepartmentOnlineSettlementBalanceAfter;
    }

    public void setFromDepartmentOnlineSettlementBalanceAfter(double fromDepartmentOnlineSettlementBalanceAfter) {
        this.fromDepartmentOnlineSettlementBalanceAfter = fromDepartmentOnlineSettlementBalanceAfter;
    }

    public double getFromDepartmentPatientDepositBalanceAfter() {
        return fromDepartmentPatientDepositBalanceAfter;
    }

    public void setFromDepartmentPatientDepositBalanceAfter(double fromDepartmentPatientDepositBalanceAfter) {
        this.fromDepartmentPatientDepositBalanceAfter = fromDepartmentPatientDepositBalanceAfter;
    }

    public double getFromDepartmentPatientPointsBalanceAfter() {
        return fromDepartmentPatientPointsBalanceAfter;
    }

    public void setFromDepartmentPatientPointsBalanceAfter(double fromDepartmentPatientPointsBalanceAfter) {
        this.fromDepartmentPatientPointsBalanceAfter = fromDepartmentPatientPointsBalanceAfter;
    }

    public double getFromDepartmentSlipBalanceAfter() {
        return fromDepartmentSlipBalanceAfter;
    }

    public void setFromDepartmentSlipBalanceAfter(double fromDepartmentSlipBalanceAfter) {
        this.fromDepartmentSlipBalanceAfter = fromDepartmentSlipBalanceAfter;
    }

    public double getFromDepartmentStaffBalanceAfter() {
        return fromDepartmentStaffBalanceAfter;
    }

    public void setFromDepartmentStaffBalanceAfter(double fromDepartmentStaffBalanceAfter) {
        this.fromDepartmentStaffBalanceAfter = fromDepartmentStaffBalanceAfter;
    }

    public double getFromDepartmentStaffWelfareBalanceAfter() {
        return fromDepartmentStaffWelfareBalanceAfter;
    }

    public void setFromDepartmentStaffWelfareBalanceAfter(double fromDepartmentStaffWelfareBalanceAfter) {
        this.fromDepartmentStaffWelfareBalanceAfter = fromDepartmentStaffWelfareBalanceAfter;
    }

    public double getFromDepartmentVoucherBalanceAfter() {
        return fromDepartmentVoucherBalanceAfter;
    }

    public void setFromDepartmentVoucherBalanceAfter(double fromDepartmentVoucherBalanceAfter) {
        this.fromDepartmentVoucherBalanceAfter = fromDepartmentVoucherBalanceAfter;
    }

    public double getFromDepartmentEwalletBalanceAfter() {
        return fromDepartmentEwalletBalanceAfter;
    }

    public void setFromDepartmentEwalletBalanceAfter(double fromDepartmentEwalletBalanceAfter) {
        this.fromDepartmentEwalletBalanceAfter = fromDepartmentEwalletBalanceAfter;
    }

    public double getToDepartmentCashBalanceAfter() {
        return toDepartmentCashBalanceAfter;
    }

    public void setToDepartmentCashBalanceAfter(double toDepartmentCashBalanceAfter) {
        this.toDepartmentCashBalanceAfter = toDepartmentCashBalanceAfter;
    }

    public double getToDepartmentCardBalanceAfter() {
        return toDepartmentCardBalanceAfter;
    }

    public void setToDepartmentCardBalanceAfter(double toDepartmentCardBalanceAfter) {
        this.toDepartmentCardBalanceAfter = toDepartmentCardBalanceAfter;
    }

    public double getToDepartmentAgentBalanceAfter() {
        return toDepartmentAgentBalanceAfter;
    }

    public void setToDepartmentAgentBalanceAfter(double toDepartmentAgentBalanceAfter) {
        this.toDepartmentAgentBalanceAfter = toDepartmentAgentBalanceAfter;
    }

    public double getToDepartmentChequeBalanceAfter() {
        return toDepartmentChequeBalanceAfter;
    }

    public void setToDepartmentChequeBalanceAfter(double toDepartmentChequeBalanceAfter) {
        this.toDepartmentChequeBalanceAfter = toDepartmentChequeBalanceAfter;
    }

    public double getToDepartmentCreditBalanceAfter() {
        return toDepartmentCreditBalanceAfter;
    }

    public void setToDepartmentCreditBalanceAfter(double toDepartmentCreditBalanceAfter) {
        this.toDepartmentCreditBalanceAfter = toDepartmentCreditBalanceAfter;
    }

    public double getToDepartmentIouBalanceAfter() {
        return toDepartmentIouBalanceAfter;
    }

    public void setToDepartmentIouBalanceAfter(double toDepartmentIouBalanceAfter) {
        this.toDepartmentIouBalanceAfter = toDepartmentIouBalanceAfter;
    }

    public double getToDepartmentMultiplePaymentMethodsBalanceAfter() {
        return toDepartmentMultiplePaymentMethodsBalanceAfter;
    }

    public void setToDepartmentMultiplePaymentMethodsBalanceAfter(double toDepartmentMultiplePaymentMethodsBalanceAfter) {
        this.toDepartmentMultiplePaymentMethodsBalanceAfter = toDepartmentMultiplePaymentMethodsBalanceAfter;
    }

    public double getToDepartmentOnCallBalanceAfter() {
        return toDepartmentOnCallBalanceAfter;
    }

    public void setToDepartmentOnCallBalanceAfter(double toDepartmentOnCallBalanceAfter) {
        this.toDepartmentOnCallBalanceAfter = toDepartmentOnCallBalanceAfter;
    }

    public double getToDepartmentOnlineSettlementBalanceAfter() {
        return toDepartmentOnlineSettlementBalanceAfter;
    }

    public void setToDepartmentOnlineSettlementBalanceAfter(double toDepartmentOnlineSettlementBalanceAfter) {
        this.toDepartmentOnlineSettlementBalanceAfter = toDepartmentOnlineSettlementBalanceAfter;
    }

    public double getToDepartmentPatientDepositBalanceAfter() {
        return toDepartmentPatientDepositBalanceAfter;
    }

    public void setToDepartmentPatientDepositBalanceAfter(double toDepartmentPatientDepositBalanceAfter) {
        this.toDepartmentPatientDepositBalanceAfter = toDepartmentPatientDepositBalanceAfter;
    }

    public double getToDepartmentPatientPointsBalanceAfter() {
        return toDepartmentPatientPointsBalanceAfter;
    }

    public void setToDepartmentPatientPointsBalanceAfter(double toDepartmentPatientPointsBalanceAfter) {
        this.toDepartmentPatientPointsBalanceAfter = toDepartmentPatientPointsBalanceAfter;
    }

    public double getToDepartmentSlipBalanceAfter() {
        return toDepartmentSlipBalanceAfter;
    }

    public void setToDepartmentSlipBalanceAfter(double toDepartmentSlipBalanceAfter) {
        this.toDepartmentSlipBalanceAfter = toDepartmentSlipBalanceAfter;
    }

    public double getToDepartmentStaffBalanceAfter() {
        return toDepartmentStaffBalanceAfter;
    }

    public void setToDepartmentStaffBalanceAfter(double toDepartmentStaffBalanceAfter) {
        this.toDepartmentStaffBalanceAfter = toDepartmentStaffBalanceAfter;
    }

    public double getToDepartmentStaffWelfareBalanceAfter() {
        return toDepartmentStaffWelfareBalanceAfter;
    }

    public void setToDepartmentStaffWelfareBalanceAfter(double toDepartmentStaffWelfareBalanceAfter) {
        this.toDepartmentStaffWelfareBalanceAfter = toDepartmentStaffWelfareBalanceAfter;
    }

    public double getToDepartmentVoucherBalanceAfter() {
        return toDepartmentVoucherBalanceAfter;
    }

    public void setToDepartmentVoucherBalanceAfter(double toDepartmentVoucherBalanceAfter) {
        this.toDepartmentVoucherBalanceAfter = toDepartmentVoucherBalanceAfter;
    }

    public double getToDepartmentEwalletBalanceAfter() {
        return toDepartmentEwalletBalanceAfter;
    }

    public void setToDepartmentEwalletBalanceAfter(double toDepartmentEwalletBalanceAfter) {
        this.toDepartmentEwalletBalanceAfter = toDepartmentEwalletBalanceAfter;
    }

    public double getFromInstitutionCashBalanceBefore() {
        return fromInstitutionCashBalanceBefore;
    }

    public void setFromInstitutionCashBalanceBefore(double fromInstitutionCashBalanceBefore) {
        this.fromInstitutionCashBalanceBefore = fromInstitutionCashBalanceBefore;
    }

    public double getFromInstitutionCardBalanceBefore() {
        return fromInstitutionCardBalanceBefore;
    }

    public void setFromInstitutionCardBalanceBefore(double fromInstitutionCardBalanceBefore) {
        this.fromInstitutionCardBalanceBefore = fromInstitutionCardBalanceBefore;
    }

    public double getFromInstitutionAgentBalanceBefore() {
        return fromInstitutionAgentBalanceBefore;
    }

    public void setFromInstitutionAgentBalanceBefore(double fromInstitutionAgentBalanceBefore) {
        this.fromInstitutionAgentBalanceBefore = fromInstitutionAgentBalanceBefore;
    }

    public double getFromInstitutionChequeBalanceBefore() {
        return fromInstitutionChequeBalanceBefore;
    }

    public void setFromInstitutionChequeBalanceBefore(double fromInstitutionChequeBalanceBefore) {
        this.fromInstitutionChequeBalanceBefore = fromInstitutionChequeBalanceBefore;
    }

    public double getFromInstitutionCreditBalanceBefore() {
        return fromInstitutionCreditBalanceBefore;
    }

    public void setFromInstitutionCreditBalanceBefore(double fromInstitutionCreditBalanceBefore) {
        this.fromInstitutionCreditBalanceBefore = fromInstitutionCreditBalanceBefore;
    }

    public double getFromInstitutionIouBalanceBefore() {
        return fromInstitutionIouBalanceBefore;
    }

    public void setFromInstitutionIouBalanceBefore(double fromInstitutionIouBalanceBefore) {
        this.fromInstitutionIouBalanceBefore = fromInstitutionIouBalanceBefore;
    }

    public double getFromInstitutionMultiplePaymentMethodsBalanceBefore() {
        return fromInstitutionMultiplePaymentMethodsBalanceBefore;
    }

    public void setFromInstitutionMultiplePaymentMethodsBalanceBefore(double fromInstitutionMultiplePaymentMethodsBalanceBefore) {
        this.fromInstitutionMultiplePaymentMethodsBalanceBefore = fromInstitutionMultiplePaymentMethodsBalanceBefore;
    }

    public double getFromInstitutionOnCallBalanceBefore() {
        return fromInstitutionOnCallBalanceBefore;
    }

    public void setFromInstitutionOnCallBalanceBefore(double fromInstitutionOnCallBalanceBefore) {
        this.fromInstitutionOnCallBalanceBefore = fromInstitutionOnCallBalanceBefore;
    }

    public double getFromInstitutionOnlineSettlementBalanceBefore() {
        return fromInstitutionOnlineSettlementBalanceBefore;
    }

    public void setFromInstitutionOnlineSettlementBalanceBefore(double fromInstitutionOnlineSettlementBalanceBefore) {
        this.fromInstitutionOnlineSettlementBalanceBefore = fromInstitutionOnlineSettlementBalanceBefore;
    }

    public double getFromInstitutionPatientDepositBalanceBefore() {
        return fromInstitutionPatientDepositBalanceBefore;
    }

    public void setFromInstitutionPatientDepositBalanceBefore(double fromInstitutionPatientDepositBalanceBefore) {
        this.fromInstitutionPatientDepositBalanceBefore = fromInstitutionPatientDepositBalanceBefore;
    }

    public double getFromInstitutionPatientPointsBalanceBefore() {
        return fromInstitutionPatientPointsBalanceBefore;
    }

    public void setFromInstitutionPatientPointsBalanceBefore(double fromInstitutionPatientPointsBalanceBefore) {
        this.fromInstitutionPatientPointsBalanceBefore = fromInstitutionPatientPointsBalanceBefore;
    }

    public double getFromInstitutionSlipBalanceBefore() {
        return fromInstitutionSlipBalanceBefore;
    }

    public void setFromInstitutionSlipBalanceBefore(double fromInstitutionSlipBalanceBefore) {
        this.fromInstitutionSlipBalanceBefore = fromInstitutionSlipBalanceBefore;
    }

    public double getFromInstitutionStaffBalanceBefore() {
        return fromInstitutionStaffBalanceBefore;
    }

    public void setFromInstitutionStaffBalanceBefore(double fromInstitutionStaffBalanceBefore) {
        this.fromInstitutionStaffBalanceBefore = fromInstitutionStaffBalanceBefore;
    }

    public double getFromInstitutionStaffWelfareBalanceBefore() {
        return fromInstitutionStaffWelfareBalanceBefore;
    }

    public void setFromInstitutionStaffWelfareBalanceBefore(double fromInstitutionStaffWelfareBalanceBefore) {
        this.fromInstitutionStaffWelfareBalanceBefore = fromInstitutionStaffWelfareBalanceBefore;
    }

    public double getFromInstitutionVoucherBalanceBefore() {
        return fromInstitutionVoucherBalanceBefore;
    }

    public void setFromInstitutionVoucherBalanceBefore(double fromInstitutionVoucherBalanceBefore) {
        this.fromInstitutionVoucherBalanceBefore = fromInstitutionVoucherBalanceBefore;
    }

    public double getFromInstitutionEwalletBalanceBefore() {
        return fromInstitutionEwalletBalanceBefore;
    }

    public void setFromInstitutionEwalletBalanceBefore(double fromInstitutionEwalletBalanceBefore) {
        this.fromInstitutionEwalletBalanceBefore = fromInstitutionEwalletBalanceBefore;
    }

    public double getToInstitutionCashBalanceBefore() {
        return toInstitutionCashBalanceBefore;
    }

    public void setToInstitutionCashBalanceBefore(double toInstitutionCashBalanceBefore) {
        this.toInstitutionCashBalanceBefore = toInstitutionCashBalanceBefore;
    }

    public double getToInstitutionCardBalanceBefore() {
        return toInstitutionCardBalanceBefore;
    }

    public void setToInstitutionCardBalanceBefore(double toInstitutionCardBalanceBefore) {
        this.toInstitutionCardBalanceBefore = toInstitutionCardBalanceBefore;
    }

    public double getToInstitutionAgentBalanceBefore() {
        return toInstitutionAgentBalanceBefore;
    }

    public void setToInstitutionAgentBalanceBefore(double toInstitutionAgentBalanceBefore) {
        this.toInstitutionAgentBalanceBefore = toInstitutionAgentBalanceBefore;
    }

    public double getToInstitutionChequeBalanceBefore() {
        return toInstitutionChequeBalanceBefore;
    }

    public void setToInstitutionChequeBalanceBefore(double toInstitutionChequeBalanceBefore) {
        this.toInstitutionChequeBalanceBefore = toInstitutionChequeBalanceBefore;
    }

    public double getToInstitutionCreditBalanceBefore() {
        return toInstitutionCreditBalanceBefore;
    }

    public void setToInstitutionCreditBalanceBefore(double toInstitutionCreditBalanceBefore) {
        this.toInstitutionCreditBalanceBefore = toInstitutionCreditBalanceBefore;
    }

    public double getToInstitutionIouBalanceBefore() {
        return toInstitutionIouBalanceBefore;
    }

    public void setToInstitutionIouBalanceBefore(double toInstitutionIouBalanceBefore) {
        this.toInstitutionIouBalanceBefore = toInstitutionIouBalanceBefore;
    }

    public double getToInstitutionMultiplePaymentMethodsBalanceBefore() {
        return toInstitutionMultiplePaymentMethodsBalanceBefore;
    }

    public void setToInstitutionMultiplePaymentMethodsBalanceBefore(double toInstitutionMultiplePaymentMethodsBalanceBefore) {
        this.toInstitutionMultiplePaymentMethodsBalanceBefore = toInstitutionMultiplePaymentMethodsBalanceBefore;
    }

    public double getToInstitutionOnCallBalanceBefore() {
        return toInstitutionOnCallBalanceBefore;
    }

    public void setToInstitutionOnCallBalanceBefore(double toInstitutionOnCallBalanceBefore) {
        this.toInstitutionOnCallBalanceBefore = toInstitutionOnCallBalanceBefore;
    }

    public double getToInstitutionOnlineSettlementBalanceBefore() {
        return toInstitutionOnlineSettlementBalanceBefore;
    }

    public void setToInstitutionOnlineSettlementBalanceBefore(double toInstitutionOnlineSettlementBalanceBefore) {
        this.toInstitutionOnlineSettlementBalanceBefore = toInstitutionOnlineSettlementBalanceBefore;
    }

    public double getToInstitutionPatientDepositBalanceBefore() {
        return toInstitutionPatientDepositBalanceBefore;
    }

    public void setToInstitutionPatientDepositBalanceBefore(double toInstitutionPatientDepositBalanceBefore) {
        this.toInstitutionPatientDepositBalanceBefore = toInstitutionPatientDepositBalanceBefore;
    }

    public double getToInstitutionPatientPointsBalanceBefore() {
        return toInstitutionPatientPointsBalanceBefore;
    }

    public void setToInstitutionPatientPointsBalanceBefore(double toInstitutionPatientPointsBalanceBefore) {
        this.toInstitutionPatientPointsBalanceBefore = toInstitutionPatientPointsBalanceBefore;
    }

    public double getToInstitutionSlipBalanceBefore() {
        return toInstitutionSlipBalanceBefore;
    }

    public void setToInstitutionSlipBalanceBefore(double toInstitutionSlipBalanceBefore) {
        this.toInstitutionSlipBalanceBefore = toInstitutionSlipBalanceBefore;
    }

    public double getToInstitutionStaffBalanceBefore() {
        return toInstitutionStaffBalanceBefore;
    }

    public void setToInstitutionStaffBalanceBefore(double toInstitutionStaffBalanceBefore) {
        this.toInstitutionStaffBalanceBefore = toInstitutionStaffBalanceBefore;
    }

    public double getToInstitutionStaffWelfareBalanceBefore() {
        return toInstitutionStaffWelfareBalanceBefore;
    }

    public void setToInstitutionStaffWelfareBalanceBefore(double toInstitutionStaffWelfareBalanceBefore) {
        this.toInstitutionStaffWelfareBalanceBefore = toInstitutionStaffWelfareBalanceBefore;
    }

    public double getToInstitutionVoucherBalanceBefore() {
        return toInstitutionVoucherBalanceBefore;
    }

    public void setToInstitutionVoucherBalanceBefore(double toInstitutionVoucherBalanceBefore) {
        this.toInstitutionVoucherBalanceBefore = toInstitutionVoucherBalanceBefore;
    }

    public double getToInstitutionEwalletBalanceBefore() {
        return toInstitutionEwalletBalanceBefore;
    }

    public void setToInstitutionEwalletBalanceBefore(double toInstitutionEwalletBalanceBefore) {
        this.toInstitutionEwalletBalanceBefore = toInstitutionEwalletBalanceBefore;
    }

    public double getFromInstitutionCashBalanceAfter() {
        return fromInstitutionCashBalanceAfter;
    }

    public void setFromInstitutionCashBalanceAfter(double fromInstitutionCashBalanceAfter) {
        this.fromInstitutionCashBalanceAfter = fromInstitutionCashBalanceAfter;
    }

    public double getFromInstitutionCardBalanceAfter() {
        return fromInstitutionCardBalanceAfter;
    }

    public void setFromInstitutionCardBalanceAfter(double fromInstitutionCardBalanceAfter) {
        this.fromInstitutionCardBalanceAfter = fromInstitutionCardBalanceAfter;
    }

    public double getFromInstitutionAgentBalanceAfter() {
        return fromInstitutionAgentBalanceAfter;
    }

    public void setFromInstitutionAgentBalanceAfter(double fromInstitutionAgentBalanceAfter) {
        this.fromInstitutionAgentBalanceAfter = fromInstitutionAgentBalanceAfter;
    }

    public double getFromInstitutionChequeBalanceAfter() {
        return fromInstitutionChequeBalanceAfter;
    }

    public void setFromInstitutionChequeBalanceAfter(double fromInstitutionChequeBalanceAfter) {
        this.fromInstitutionChequeBalanceAfter = fromInstitutionChequeBalanceAfter;
    }

    public double getFromInstitutionCreditBalanceAfter() {
        return fromInstitutionCreditBalanceAfter;
    }

    public void setFromInstitutionCreditBalanceAfter(double fromInstitutionCreditBalanceAfter) {
        this.fromInstitutionCreditBalanceAfter = fromInstitutionCreditBalanceAfter;
    }

    public double getFromInstitutionIouBalanceAfter() {
        return fromInstitutionIouBalanceAfter;
    }

    public void setFromInstitutionIouBalanceAfter(double fromInstitutionIouBalanceAfter) {
        this.fromInstitutionIouBalanceAfter = fromInstitutionIouBalanceAfter;
    }

    public double getFromInstitutionMultiplePaymentMethodsBalanceAfter() {
        return fromInstitutionMultiplePaymentMethodsBalanceAfter;
    }

    public void setFromInstitutionMultiplePaymentMethodsBalanceAfter(double fromInstitutionMultiplePaymentMethodsBalanceAfter) {
        this.fromInstitutionMultiplePaymentMethodsBalanceAfter = fromInstitutionMultiplePaymentMethodsBalanceAfter;
    }

    public double getFromInstitutionOnCallBalanceAfter() {
        return fromInstitutionOnCallBalanceAfter;
    }

    public void setFromInstitutionOnCallBalanceAfter(double fromInstitutionOnCallBalanceAfter) {
        this.fromInstitutionOnCallBalanceAfter = fromInstitutionOnCallBalanceAfter;
    }

    public double getFromInstitutionOnlineSettlementBalanceAfter() {
        return fromInstitutionOnlineSettlementBalanceAfter;
    }

    public void setFromInstitutionOnlineSettlementBalanceAfter(double fromInstitutionOnlineSettlementBalanceAfter) {
        this.fromInstitutionOnlineSettlementBalanceAfter = fromInstitutionOnlineSettlementBalanceAfter;
    }

    public double getFromInstitutionPatientDepositBalanceAfter() {
        return fromInstitutionPatientDepositBalanceAfter;
    }

    public void setFromInstitutionPatientDepositBalanceAfter(double fromInstitutionPatientDepositBalanceAfter) {
        this.fromInstitutionPatientDepositBalanceAfter = fromInstitutionPatientDepositBalanceAfter;
    }

    public double getFromInstitutionPatientPointsBalanceAfter() {
        return fromInstitutionPatientPointsBalanceAfter;
    }

    public void setFromInstitutionPatientPointsBalanceAfter(double fromInstitutionPatientPointsBalanceAfter) {
        this.fromInstitutionPatientPointsBalanceAfter = fromInstitutionPatientPointsBalanceAfter;
    }

    public double getFromInstitutionSlipBalanceAfter() {
        return fromInstitutionSlipBalanceAfter;
    }

    public void setFromInstitutionSlipBalanceAfter(double fromInstitutionSlipBalanceAfter) {
        this.fromInstitutionSlipBalanceAfter = fromInstitutionSlipBalanceAfter;
    }

    public double getFromInstitutionStaffBalanceAfter() {
        return fromInstitutionStaffBalanceAfter;
    }

    public void setFromInstitutionStaffBalanceAfter(double fromInstitutionStaffBalanceAfter) {
        this.fromInstitutionStaffBalanceAfter = fromInstitutionStaffBalanceAfter;
    }

    public double getFromInstitutionStaffWelfareBalanceAfter() {
        return fromInstitutionStaffWelfareBalanceAfter;
    }

    public void setFromInstitutionStaffWelfareBalanceAfter(double fromInstitutionStaffWelfareBalanceAfter) {
        this.fromInstitutionStaffWelfareBalanceAfter = fromInstitutionStaffWelfareBalanceAfter;
    }

    public double getFromInstitutionVoucherBalanceAfter() {
        return fromInstitutionVoucherBalanceAfter;
    }

    public void setFromInstitutionVoucherBalanceAfter(double fromInstitutionVoucherBalanceAfter) {
        this.fromInstitutionVoucherBalanceAfter = fromInstitutionVoucherBalanceAfter;
    }

    public double getFromInstitutionEwalletBalanceAfter() {
        return fromInstitutionEwalletBalanceAfter;
    }

    public void setFromInstitutionEwalletBalanceAfter(double fromInstitutionEwalletBalanceAfter) {
        this.fromInstitutionEwalletBalanceAfter = fromInstitutionEwalletBalanceAfter;
    }

    public double getToInstitutionCashBalanceAfter() {
        return toInstitutionCashBalanceAfter;
    }

    public void setToInstitutionCashBalanceAfter(double toInstitutionCashBalanceAfter) {
        this.toInstitutionCashBalanceAfter = toInstitutionCashBalanceAfter;
    }

    public double getToInstitutionCardBalanceAfter() {
        return toInstitutionCardBalanceAfter;
    }

    public void setToInstitutionCardBalanceAfter(double toInstitutionCardBalanceAfter) {
        this.toInstitutionCardBalanceAfter = toInstitutionCardBalanceAfter;
    }

    public double getToInstitutionAgentBalanceAfter() {
        return toInstitutionAgentBalanceAfter;
    }

    public void setToInstitutionAgentBalanceAfter(double toInstitutionAgentBalanceAfter) {
        this.toInstitutionAgentBalanceAfter = toInstitutionAgentBalanceAfter;
    }

    public double getToInstitutionChequeBalanceAfter() {
        return toInstitutionChequeBalanceAfter;
    }

    public void setToInstitutionChequeBalanceAfter(double toInstitutionChequeBalanceAfter) {
        this.toInstitutionChequeBalanceAfter = toInstitutionChequeBalanceAfter;
    }

    public double getToInstitutionCreditBalanceAfter() {
        return toInstitutionCreditBalanceAfter;
    }

    public void setToInstitutionCreditBalanceAfter(double toInstitutionCreditBalanceAfter) {
        this.toInstitutionCreditBalanceAfter = toInstitutionCreditBalanceAfter;
    }

    public double getToInstitutionIouBalanceAfter() {
        return toInstitutionIouBalanceAfter;
    }

    public void setToInstitutionIouBalanceAfter(double toInstitutionIouBalanceAfter) {
        this.toInstitutionIouBalanceAfter = toInstitutionIouBalanceAfter;
    }

    public double getToInstitutionMultiplePaymentMethodsBalanceAfter() {
        return toInstitutionMultiplePaymentMethodsBalanceAfter;
    }

    public void setToInstitutionMultiplePaymentMethodsBalanceAfter(double toInstitutionMultiplePaymentMethodsBalanceAfter) {
        this.toInstitutionMultiplePaymentMethodsBalanceAfter = toInstitutionMultiplePaymentMethodsBalanceAfter;
    }

    public double getToInstitutionOnCallBalanceAfter() {
        return toInstitutionOnCallBalanceAfter;
    }

    public void setToInstitutionOnCallBalanceAfter(double toInstitutionOnCallBalanceAfter) {
        this.toInstitutionOnCallBalanceAfter = toInstitutionOnCallBalanceAfter;
    }

    public double getToInstitutionOnlineSettlementBalanceAfter() {
        return toInstitutionOnlineSettlementBalanceAfter;
    }

    public void setToInstitutionOnlineSettlementBalanceAfter(double toInstitutionOnlineSettlementBalanceAfter) {
        this.toInstitutionOnlineSettlementBalanceAfter = toInstitutionOnlineSettlementBalanceAfter;
    }

    public double getToInstitutionPatientDepositBalanceAfter() {
        return toInstitutionPatientDepositBalanceAfter;
    }

    public void setToInstitutionPatientDepositBalanceAfter(double toInstitutionPatientDepositBalanceAfter) {
        this.toInstitutionPatientDepositBalanceAfter = toInstitutionPatientDepositBalanceAfter;
    }

    public double getToInstitutionPatientPointsBalanceAfter() {
        return toInstitutionPatientPointsBalanceAfter;
    }

    public void setToInstitutionPatientPointsBalanceAfter(double toInstitutionPatientPointsBalanceAfter) {
        this.toInstitutionPatientPointsBalanceAfter = toInstitutionPatientPointsBalanceAfter;
    }

    public double getToInstitutionSlipBalanceAfter() {
        return toInstitutionSlipBalanceAfter;
    }

    public void setToInstitutionSlipBalanceAfter(double toInstitutionSlipBalanceAfter) {
        this.toInstitutionSlipBalanceAfter = toInstitutionSlipBalanceAfter;
    }

    public double getToInstitutionStaffBalanceAfter() {
        return toInstitutionStaffBalanceAfter;
    }

    public void setToInstitutionStaffBalanceAfter(double toInstitutionStaffBalanceAfter) {
        this.toInstitutionStaffBalanceAfter = toInstitutionStaffBalanceAfter;
    }

    public double getToInstitutionStaffWelfareBalanceAfter() {
        return toInstitutionStaffWelfareBalanceAfter;
    }

    public void setToInstitutionStaffWelfareBalanceAfter(double toInstitutionStaffWelfareBalanceAfter) {
        this.toInstitutionStaffWelfareBalanceAfter = toInstitutionStaffWelfareBalanceAfter;
    }

    public double getToInstitutionVoucherBalanceAfter() {
        return toInstitutionVoucherBalanceAfter;
    }

    public void setToInstitutionVoucherBalanceAfter(double toInstitutionVoucherBalanceAfter) {
        this.toInstitutionVoucherBalanceAfter = toInstitutionVoucherBalanceAfter;
    }

    public double getToInstitutionEwalletBalanceAfter() {
        return toInstitutionEwalletBalanceAfter;
    }

    public void setToInstitutionEwalletBalanceAfter(double toInstitutionEwalletBalanceAfter) {
        this.toInstitutionEwalletBalanceAfter = toInstitutionEwalletBalanceAfter;
    }

    public double getFromSiteCashBalanceBefore() {
        return fromSiteCashBalanceBefore;
    }

    public void setFromSiteCashBalanceBefore(double fromSiteCashBalanceBefore) {
        this.fromSiteCashBalanceBefore = fromSiteCashBalanceBefore;
    }

    public double getFromSiteCardBalanceBefore() {
        return fromSiteCardBalanceBefore;
    }

    public void setFromSiteCardBalanceBefore(double fromSiteCardBalanceBefore) {
        this.fromSiteCardBalanceBefore = fromSiteCardBalanceBefore;
    }

    public double getFromSiteAgentBalanceBefore() {
        return fromSiteAgentBalanceBefore;
    }

    public void setFromSiteAgentBalanceBefore(double fromSiteAgentBalanceBefore) {
        this.fromSiteAgentBalanceBefore = fromSiteAgentBalanceBefore;
    }

    public double getFromSiteChequeBalanceBefore() {
        return fromSiteChequeBalanceBefore;
    }

    public void setFromSiteChequeBalanceBefore(double fromSiteChequeBalanceBefore) {
        this.fromSiteChequeBalanceBefore = fromSiteChequeBalanceBefore;
    }

    public double getFromSiteCreditBalanceBefore() {
        return fromSiteCreditBalanceBefore;
    }

    public void setFromSiteCreditBalanceBefore(double fromSiteCreditBalanceBefore) {
        this.fromSiteCreditBalanceBefore = fromSiteCreditBalanceBefore;
    }

    public double getFromSiteIouBalanceBefore() {
        return fromSiteIouBalanceBefore;
    }

    public void setFromSiteIouBalanceBefore(double fromSiteIouBalanceBefore) {
        this.fromSiteIouBalanceBefore = fromSiteIouBalanceBefore;
    }

    public double getFromSiteMultiplePaymentMethodsBalanceBefore() {
        return fromSiteMultiplePaymentMethodsBalanceBefore;
    }

    public void setFromSiteMultiplePaymentMethodsBalanceBefore(double fromSiteMultiplePaymentMethodsBalanceBefore) {
        this.fromSiteMultiplePaymentMethodsBalanceBefore = fromSiteMultiplePaymentMethodsBalanceBefore;
    }

    public double getFromSiteOnCallBalanceBefore() {
        return fromSiteOnCallBalanceBefore;
    }

    public void setFromSiteOnCallBalanceBefore(double fromSiteOnCallBalanceBefore) {
        this.fromSiteOnCallBalanceBefore = fromSiteOnCallBalanceBefore;
    }

    public double getFromSiteOnlineSettlementBalanceBefore() {
        return fromSiteOnlineSettlementBalanceBefore;
    }

    public void setFromSiteOnlineSettlementBalanceBefore(double fromSiteOnlineSettlementBalanceBefore) {
        this.fromSiteOnlineSettlementBalanceBefore = fromSiteOnlineSettlementBalanceBefore;
    }

    public double getFromSitePatientDepositBalanceBefore() {
        return fromSitePatientDepositBalanceBefore;
    }

    public void setFromSitePatientDepositBalanceBefore(double fromSitePatientDepositBalanceBefore) {
        this.fromSitePatientDepositBalanceBefore = fromSitePatientDepositBalanceBefore;
    }

    public double getFromSitePatientPointsBalanceBefore() {
        return fromSitePatientPointsBalanceBefore;
    }

    public void setFromSitePatientPointsBalanceBefore(double fromSitePatientPointsBalanceBefore) {
        this.fromSitePatientPointsBalanceBefore = fromSitePatientPointsBalanceBefore;
    }

    public double getFromSiteSlipBalanceBefore() {
        return fromSiteSlipBalanceBefore;
    }

    public void setFromSiteSlipBalanceBefore(double fromSiteSlipBalanceBefore) {
        this.fromSiteSlipBalanceBefore = fromSiteSlipBalanceBefore;
    }

    public double getFromSiteStaffBalanceBefore() {
        return fromSiteStaffBalanceBefore;
    }

    public void setFromSiteStaffBalanceBefore(double fromSiteStaffBalanceBefore) {
        this.fromSiteStaffBalanceBefore = fromSiteStaffBalanceBefore;
    }

    public double getFromSiteStaffWelfareBalanceBefore() {
        return fromSiteStaffWelfareBalanceBefore;
    }

    public void setFromSiteStaffWelfareBalanceBefore(double fromSiteStaffWelfareBalanceBefore) {
        this.fromSiteStaffWelfareBalanceBefore = fromSiteStaffWelfareBalanceBefore;
    }

    public double getFromSiteVoucherBalanceBefore() {
        return fromSiteVoucherBalanceBefore;
    }

    public void setFromSiteVoucherBalanceBefore(double fromSiteVoucherBalanceBefore) {
        this.fromSiteVoucherBalanceBefore = fromSiteVoucherBalanceBefore;
    }

    public double getFromSiteEwalletBalanceBefore() {
        return fromSiteEwalletBalanceBefore;
    }

    public void setFromSiteEwalletBalanceBefore(double fromSiteEwalletBalanceBefore) {
        this.fromSiteEwalletBalanceBefore = fromSiteEwalletBalanceBefore;
    }

    public double getToSiteCashBalanceBefore() {
        return toSiteCashBalanceBefore;
    }

    public void setToSiteCashBalanceBefore(double toSiteCashBalanceBefore) {
        this.toSiteCashBalanceBefore = toSiteCashBalanceBefore;
    }

    public double getToSiteCardBalanceBefore() {
        return toSiteCardBalanceBefore;
    }

    public void setToSiteCardBalanceBefore(double toSiteCardBalanceBefore) {
        this.toSiteCardBalanceBefore = toSiteCardBalanceBefore;
    }

    public double getToSiteAgentBalanceBefore() {
        return toSiteAgentBalanceBefore;
    }

    public void setToSiteAgentBalanceBefore(double toSiteAgentBalanceBefore) {
        this.toSiteAgentBalanceBefore = toSiteAgentBalanceBefore;
    }

    public double getToSiteChequeBalanceBefore() {
        return toSiteChequeBalanceBefore;
    }

    public void setToSiteChequeBalanceBefore(double toSiteChequeBalanceBefore) {
        this.toSiteChequeBalanceBefore = toSiteChequeBalanceBefore;
    }

    public double getToSiteCreditBalanceBefore() {
        return toSiteCreditBalanceBefore;
    }

    public void setToSiteCreditBalanceBefore(double toSiteCreditBalanceBefore) {
        this.toSiteCreditBalanceBefore = toSiteCreditBalanceBefore;
    }

    public double getToSiteIouBalanceBefore() {
        return toSiteIouBalanceBefore;
    }

    public void setToSiteIouBalanceBefore(double toSiteIouBalanceBefore) {
        this.toSiteIouBalanceBefore = toSiteIouBalanceBefore;
    }

    public double getToSiteMultiplePaymentMethodsBalanceBefore() {
        return toSiteMultiplePaymentMethodsBalanceBefore;
    }

    public void setToSiteMultiplePaymentMethodsBalanceBefore(double toSiteMultiplePaymentMethodsBalanceBefore) {
        this.toSiteMultiplePaymentMethodsBalanceBefore = toSiteMultiplePaymentMethodsBalanceBefore;
    }

    public double getToSiteOnCallBalanceBefore() {
        return toSiteOnCallBalanceBefore;
    }

    public void setToSiteOnCallBalanceBefore(double toSiteOnCallBalanceBefore) {
        this.toSiteOnCallBalanceBefore = toSiteOnCallBalanceBefore;
    }

    public double getToSiteOnlineSettlementBalanceBefore() {
        return toSiteOnlineSettlementBalanceBefore;
    }

    public void setToSiteOnlineSettlementBalanceBefore(double toSiteOnlineSettlementBalanceBefore) {
        this.toSiteOnlineSettlementBalanceBefore = toSiteOnlineSettlementBalanceBefore;
    }

    public double getToSitePatientDepositBalanceBefore() {
        return toSitePatientDepositBalanceBefore;
    }

    public void setToSitePatientDepositBalanceBefore(double toSitePatientDepositBalanceBefore) {
        this.toSitePatientDepositBalanceBefore = toSitePatientDepositBalanceBefore;
    }

    public double getToSitePatientPointsBalanceBefore() {
        return toSitePatientPointsBalanceBefore;
    }

    public void setToSitePatientPointsBalanceBefore(double toSitePatientPointsBalanceBefore) {
        this.toSitePatientPointsBalanceBefore = toSitePatientPointsBalanceBefore;
    }

    public double getToSiteSlipBalanceBefore() {
        return toSiteSlipBalanceBefore;
    }

    public void setToSiteSlipBalanceBefore(double toSiteSlipBalanceBefore) {
        this.toSiteSlipBalanceBefore = toSiteSlipBalanceBefore;
    }

    public double getToSiteStaffBalanceBefore() {
        return toSiteStaffBalanceBefore;
    }

    public void setToSiteStaffBalanceBefore(double toSiteStaffBalanceBefore) {
        this.toSiteStaffBalanceBefore = toSiteStaffBalanceBefore;
    }

    public double getToSiteStaffWelfareBalanceBefore() {
        return toSiteStaffWelfareBalanceBefore;
    }

    public void setToSiteStaffWelfareBalanceBefore(double toSiteStaffWelfareBalanceBefore) {
        this.toSiteStaffWelfareBalanceBefore = toSiteStaffWelfareBalanceBefore;
    }

    public double getToSiteVoucherBalanceBefore() {
        return toSiteVoucherBalanceBefore;
    }

    public void setToSiteVoucherBalanceBefore(double toSiteVoucherBalanceBefore) {
        this.toSiteVoucherBalanceBefore = toSiteVoucherBalanceBefore;
    }

    public double getToSiteEwalletBalanceBefore() {
        return toSiteEwalletBalanceBefore;
    }

    public void setToSiteEwalletBalanceBefore(double toSiteEwalletBalanceBefore) {
        this.toSiteEwalletBalanceBefore = toSiteEwalletBalanceBefore;
    }

    public double getFromSiteCashBalanceAfter() {
        return fromSiteCashBalanceAfter;
    }

    public void setFromSiteCashBalanceAfter(double fromSiteCashBalanceAfter) {
        this.fromSiteCashBalanceAfter = fromSiteCashBalanceAfter;
    }

    public double getFromSiteCardBalanceAfter() {
        return fromSiteCardBalanceAfter;
    }

    public void setFromSiteCardBalanceAfter(double fromSiteCardBalanceAfter) {
        this.fromSiteCardBalanceAfter = fromSiteCardBalanceAfter;
    }

    public double getFromSiteAgentBalanceAfter() {
        return fromSiteAgentBalanceAfter;
    }

    public void setFromSiteAgentBalanceAfter(double fromSiteAgentBalanceAfter) {
        this.fromSiteAgentBalanceAfter = fromSiteAgentBalanceAfter;
    }

    public double getFromSiteChequeBalanceAfter() {
        return fromSiteChequeBalanceAfter;
    }

    public void setFromSiteChequeBalanceAfter(double fromSiteChequeBalanceAfter) {
        this.fromSiteChequeBalanceAfter = fromSiteChequeBalanceAfter;
    }

    public double getFromSiteCreditBalanceAfter() {
        return fromSiteCreditBalanceAfter;
    }

    public void setFromSiteCreditBalanceAfter(double fromSiteCreditBalanceAfter) {
        this.fromSiteCreditBalanceAfter = fromSiteCreditBalanceAfter;
    }

    public double getFromSiteIouBalanceAfter() {
        return fromSiteIouBalanceAfter;
    }

    public void setFromSiteIouBalanceAfter(double fromSiteIouBalanceAfter) {
        this.fromSiteIouBalanceAfter = fromSiteIouBalanceAfter;
    }

    public double getFromSiteMultiplePaymentMethodsBalanceAfter() {
        return fromSiteMultiplePaymentMethodsBalanceAfter;
    }

    public void setFromSiteMultiplePaymentMethodsBalanceAfter(double fromSiteMultiplePaymentMethodsBalanceAfter) {
        this.fromSiteMultiplePaymentMethodsBalanceAfter = fromSiteMultiplePaymentMethodsBalanceAfter;
    }

    public double getFromSiteOnCallBalanceAfter() {
        return fromSiteOnCallBalanceAfter;
    }

    public void setFromSiteOnCallBalanceAfter(double fromSiteOnCallBalanceAfter) {
        this.fromSiteOnCallBalanceAfter = fromSiteOnCallBalanceAfter;
    }

    public double getFromSiteOnlineSettlementBalanceAfter() {
        return fromSiteOnlineSettlementBalanceAfter;
    }

    public void setFromSiteOnlineSettlementBalanceAfter(double fromSiteOnlineSettlementBalanceAfter) {
        this.fromSiteOnlineSettlementBalanceAfter = fromSiteOnlineSettlementBalanceAfter;
    }

    public double getFromSitePatientDepositBalanceAfter() {
        return fromSitePatientDepositBalanceAfter;
    }

    public void setFromSitePatientDepositBalanceAfter(double fromSitePatientDepositBalanceAfter) {
        this.fromSitePatientDepositBalanceAfter = fromSitePatientDepositBalanceAfter;
    }

    public double getFromSitePatientPointsBalanceAfter() {
        return fromSitePatientPointsBalanceAfter;
    }

    public void setFromSitePatientPointsBalanceAfter(double fromSitePatientPointsBalanceAfter) {
        this.fromSitePatientPointsBalanceAfter = fromSitePatientPointsBalanceAfter;
    }

    public double getFromSiteSlipBalanceAfter() {
        return fromSiteSlipBalanceAfter;
    }

    public void setFromSiteSlipBalanceAfter(double fromSiteSlipBalanceAfter) {
        this.fromSiteSlipBalanceAfter = fromSiteSlipBalanceAfter;
    }

    public double getFromSiteStaffBalanceAfter() {
        return fromSiteStaffBalanceAfter;
    }

    public void setFromSiteStaffBalanceAfter(double fromSiteStaffBalanceAfter) {
        this.fromSiteStaffBalanceAfter = fromSiteStaffBalanceAfter;
    }

    public double getFromSiteStaffWelfareBalanceAfter() {
        return fromSiteStaffWelfareBalanceAfter;
    }

    public void setFromSiteStaffWelfareBalanceAfter(double fromSiteStaffWelfareBalanceAfter) {
        this.fromSiteStaffWelfareBalanceAfter = fromSiteStaffWelfareBalanceAfter;
    }

    public double getFromSiteVoucherBalanceAfter() {
        return fromSiteVoucherBalanceAfter;
    }

    public void setFromSiteVoucherBalanceAfter(double fromSiteVoucherBalanceAfter) {
        this.fromSiteVoucherBalanceAfter = fromSiteVoucherBalanceAfter;
    }

    public double getFromSiteEwalletBalanceAfter() {
        return fromSiteEwalletBalanceAfter;
    }

    public void setFromSiteEwalletBalanceAfter(double fromSiteEwalletBalanceAfter) {
        this.fromSiteEwalletBalanceAfter = fromSiteEwalletBalanceAfter;
    }

    public double getToSiteCashBalanceAfter() {
        return toSiteCashBalanceAfter;
    }

    public void setToSiteCashBalanceAfter(double toSiteCashBalanceAfter) {
        this.toSiteCashBalanceAfter = toSiteCashBalanceAfter;
    }

    public double getToSiteCardBalanceAfter() {
        return toSiteCardBalanceAfter;
    }

    public void setToSiteCardBalanceAfter(double toSiteCardBalanceAfter) {
        this.toSiteCardBalanceAfter = toSiteCardBalanceAfter;
    }

    public double getToSiteAgentBalanceAfter() {
        return toSiteAgentBalanceAfter;
    }

    public void setToSiteAgentBalanceAfter(double toSiteAgentBalanceAfter) {
        this.toSiteAgentBalanceAfter = toSiteAgentBalanceAfter;
    }

    public double getToSiteChequeBalanceAfter() {
        return toSiteChequeBalanceAfter;
    }

    public void setToSiteChequeBalanceAfter(double toSiteChequeBalanceAfter) {
        this.toSiteChequeBalanceAfter = toSiteChequeBalanceAfter;
    }

    public double getToSiteCreditBalanceAfter() {
        return toSiteCreditBalanceAfter;
    }

    public void setToSiteCreditBalanceAfter(double toSiteCreditBalanceAfter) {
        this.toSiteCreditBalanceAfter = toSiteCreditBalanceAfter;
    }

    public double getToSiteIouBalanceAfter() {
        return toSiteIouBalanceAfter;
    }

    public void setToSiteIouBalanceAfter(double toSiteIouBalanceAfter) {
        this.toSiteIouBalanceAfter = toSiteIouBalanceAfter;
    }

    public double getToSiteMultiplePaymentMethodsBalanceAfter() {
        return toSiteMultiplePaymentMethodsBalanceAfter;
    }

    public void setToSiteMultiplePaymentMethodsBalanceAfter(double toSiteMultiplePaymentMethodsBalanceAfter) {
        this.toSiteMultiplePaymentMethodsBalanceAfter = toSiteMultiplePaymentMethodsBalanceAfter;
    }

    public double getToSiteOnCallBalanceAfter() {
        return toSiteOnCallBalanceAfter;
    }

    public void setToSiteOnCallBalanceAfter(double toSiteOnCallBalanceAfter) {
        this.toSiteOnCallBalanceAfter = toSiteOnCallBalanceAfter;
    }

    public double getToSiteOnlineSettlementBalanceAfter() {
        return toSiteOnlineSettlementBalanceAfter;
    }

    public void setToSiteOnlineSettlementBalanceAfter(double toSiteOnlineSettlementBalanceAfter) {
        this.toSiteOnlineSettlementBalanceAfter = toSiteOnlineSettlementBalanceAfter;
    }

    public double getToSitePatientDepositBalanceAfter() {
        return toSitePatientDepositBalanceAfter;
    }

    public void setToSitePatientDepositBalanceAfter(double toSitePatientDepositBalanceAfter) {
        this.toSitePatientDepositBalanceAfter = toSitePatientDepositBalanceAfter;
    }

    public double getToSitePatientPointsBalanceAfter() {
        return toSitePatientPointsBalanceAfter;
    }

    public void setToSitePatientPointsBalanceAfter(double toSitePatientPointsBalanceAfter) {
        this.toSitePatientPointsBalanceAfter = toSitePatientPointsBalanceAfter;
    }

    public double getToSiteSlipBalanceAfter() {
        return toSiteSlipBalanceAfter;
    }

    public void setToSiteSlipBalanceAfter(double toSiteSlipBalanceAfter) {
        this.toSiteSlipBalanceAfter = toSiteSlipBalanceAfter;
    }

    public double getToSiteStaffBalanceAfter() {
        return toSiteStaffBalanceAfter;
    }

    public void setToSiteStaffBalanceAfter(double toSiteStaffBalanceAfter) {
        this.toSiteStaffBalanceAfter = toSiteStaffBalanceAfter;
    }

    public double getToSiteStaffWelfareBalanceAfter() {
        return toSiteStaffWelfareBalanceAfter;
    }

    public void setToSiteStaffWelfareBalanceAfter(double toSiteStaffWelfareBalanceAfter) {
        this.toSiteStaffWelfareBalanceAfter = toSiteStaffWelfareBalanceAfter;
    }

    public double getToSiteVoucherBalanceAfter() {
        return toSiteVoucherBalanceAfter;
    }

    public void setToSiteVoucherBalanceAfter(double toSiteVoucherBalanceAfter) {
        this.toSiteVoucherBalanceAfter = toSiteVoucherBalanceAfter;
    }

    public double getToSiteEwalletBalanceAfter() {
        return toSiteEwalletBalanceAfter;
    }

    public void setToSiteEwalletBalanceAfter(double toSiteEwalletBalanceAfter) {
        this.toSiteEwalletBalanceAfter = toSiteEwalletBalanceAfter;
    }

    public double getCashValue() {
        return cashValue;
    }

    public void setCashValue(double cashValue) {
        this.cashValue = cashValue;
    }

    public double getCardValue() {
        return cardValue;
    }

    public void setCardValue(double cardValue) {
        this.cardValue = cardValue;
    }

    public double getAgentValue() {
        return agentValue;
    }

    public void setAgentValue(double agentValue) {
        this.agentValue = agentValue;
    }

    public double getChequeValue() {
        return chequeValue;
    }

    public void setChequeValue(double chequeValue) {
        this.chequeValue = chequeValue;
    }

    public double getCreditValue() {
        return creditValue;
    }

    public void setCreditValue(double creditValue) {
        this.creditValue = creditValue;
    }

    public double getIouValue() {
        return iouValue;
    }

    public void setIouValue(double iouValue) {
        this.iouValue = iouValue;
    }

    public double getMultiplePaymentMethodsValue() {
        return multiplePaymentMethodsValue;
    }

    public void setMultiplePaymentMethodsValue(double multiplePaymentMethodsValue) {
        this.multiplePaymentMethodsValue = multiplePaymentMethodsValue;
    }

    public double getOnCallValue() {
        return onCallValue;
    }

    public void setOnCallValue(double onCallValue) {
        this.onCallValue = onCallValue;
    }

    public double getOnlineSettlementValue() {
        return onlineSettlementValue;
    }

    public void setOnlineSettlementValue(double onlineSettlementValue) {
        this.onlineSettlementValue = onlineSettlementValue;
    }

    public double getPatientDepositValue() {
        return patientDepositValue;
    }

    public void setPatientDepositValue(double patientDepositValue) {
        this.patientDepositValue = patientDepositValue;
    }

    public double getPatientPointsValue() {
        return patientPointsValue;
    }

    public void setPatientPointsValue(double patientPointsValue) {
        this.patientPointsValue = patientPointsValue;
    }

    public double getSlipValue() {
        return slipValue;
    }

    public void setSlipValue(double slipValue) {
        this.slipValue = slipValue;
    }

    public double getStaffValue() {
        return staffValue;
    }

    public void setStaffValue(double staffValue) {
        this.staffValue = staffValue;
    }

    public double getStaffWelfareValue() {
        return staffWelfareValue;
    }

    public void setStaffWelfareValue(double staffWelfareValue) {
        this.staffWelfareValue = staffWelfareValue;
    }

    public double getVoucherValue() {
        return voucherValue;
    }

    public void setVoucherValue(double voucherValue) {
        this.voucherValue = voucherValue;
    }

    public double getEwalletValue() {
        return ewalletValue;
    }

    public void setEwalletValue(double ewalletValue) {
        this.ewalletValue = ewalletValue;
    }

}
