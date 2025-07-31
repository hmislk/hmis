/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity;

import com.divudi.core.data.CollectingCentrePaymentMethod;
import com.divudi.core.data.IdentifiableWithNameOrCode;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.entity.channel.AgentReferenceBook;
import com.divudi.core.util.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author buddhika
 */
@Entity
public class Institution implements Serializable, IdentifiableWithNameOrCode {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    String institutionCode;
    @ManyToOne(fetch = FetchType.LAZY)
    Institution institution;
    @ManyToOne(cascade = CascadeType.ALL)
    private Person contactPerson;

    String name;
    private String code;
    String address;
    String fax;
    String email;
    private String ownerEmail;
    String phone;
    String mobile;
    String web;
    String chequePrintingName;
    private String qbSupplierName;
    private String contactPersonName;
    private String paymentCompanyName;
    private String bankName;
    private String branchName;
    private String legalCompany;
    private String ownerName;

    @ManyToOne
    private Institution labInstitution;
    @ManyToOne
    private Department labDepartment;
    @ManyToOne
    private Category feeListType;

    @Lob
    String labBillHeading;
    @Lob
    String pharmacyBillHeading;
    @Lob
    String radiologyBillHeading;
    @Lob
    String opdBillHeading;
    @Lob
    String cashierBillHeading;
    @Enumerated(EnumType.STRING)
    InstitutionType institutionType;
    @Enumerated(EnumType.STRING)
    private CollectingCentrePaymentMethod CollectingCentrePaymentMethod;
    @ManyToOne
    private Route route;
    //Created Properties
    @ManyToOne

    WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)

    Date createdAt;
    //Retairing properties

    boolean retired;
    @ManyToOne

    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)

    Date retiredAt;
    String retireComments;
    double labBillDiscount;
    double opdBillDiscount;
    double inwardDiscount;
    double pharmacyDiscount;
    
    /**
     * Spelling mistake. Should be balance. This field is kept for
     * backward compatibility with existing MySQL databases. Use carefully.
     * Planned for correction in a future major version.
     */
    double ballance;

    private double companyBalance;
    private double agentBalance;

    double allowedCredit;
    private double allowedCreditLimit;
    double maxCreditLimit;
    double standardCreditLimit;
    private double percentage;
    @Transient
    String transAddress1;
    @Transient
    String transAddress2;
    @Transient
    String transAddress3;
    @Transient
    String transAddress4;
    @Transient
    private String transAddress5;
    @Transient
    private String transAddress6;
    @Transient
    private String transAddress7;
    @Transient
    List<AgentReferenceBook> agentReferenceBooks;
    String pointOfIssueNo;

    @OneToMany(mappedBy = "institution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)

    List<Institution> branch = new ArrayList<>();
    @Lob
    String descreption;
    String accountNo;

    @ManyToOne
    Institution bankBranch;

    @Deprecated // Use Application Configuration Options
    String emailSendingUsername;
    @Deprecated // Use Application Configuration Options
    String emailSendingPassword;

    @Deprecated // Use Application Configuration Options
    private String smsSendingUsername;
    @Deprecated // Use Application Configuration Options
    private String smsSendingPassword;
    @Deprecated // Use Application Configuration Options
    private String smsSendingAlias;

    //Inactive Status
    private boolean inactive;
    @ManyToOne
    private Institution parentInstitution;

    public String getEmailSendingUsername() {
        return emailSendingUsername;
    }

    public void setEmailSendingUsername(String emailSendingUsername) {
        this.emailSendingUsername = emailSendingUsername;
    }

    public String getEmailSendingPassword() {
        return emailSendingPassword;
    }

    public void setEmailSendingPassword(String emailSendingPassword) {
        this.emailSendingPassword = emailSendingPassword;
    }

    public String getPointOfIssueNo() {
        return pointOfIssueNo;
    }

    public void setPointOfIssueNo(String pointOfIssueNo) {
        this.pointOfIssueNo = pointOfIssueNo;
    }

//    public Institution() {
//        split();
//    }
    public String getDescreption() {
        return descreption;
    }

    public void setDescreption(String descreption) {
        this.descreption = descreption;
    }

    public double getLabBillDiscount() {
        return labBillDiscount;
    }

    public void setLabBillDiscount(double labBillDiscount) {
        this.labBillDiscount = labBillDiscount;
    }

    public double getOpdBillDiscount() {
        return opdBillDiscount;
    }

    public void setOpdBillDiscount(double opdBillDiscount) {
        this.opdBillDiscount = opdBillDiscount;
    }

    public double getInwardDiscount() {
        return inwardDiscount;
    }

    public void setInwardDiscount(double inwardDiscount) {
        this.inwardDiscount = inwardDiscount;
    }

    public String getTransAddress1() {
        if (transAddress1 == null) {
            split();
        }
        return transAddress1;
    }

    public void setTransAddress1(String transAddress1) {
        this.transAddress1 = transAddress1;
    }

    public String getTransAddress2() {
        return transAddress2;
    }

    public void setTransAddress2(String transAddress2) {
        this.transAddress2 = transAddress2;
    }

    public String getTransAddress3() {
        return transAddress3;
    }

    public void setTransAddress3(String transAddress3) {
        this.transAddress3 = transAddress3;
    }

    public String getTransAddress4() {
        return transAddress4;
    }

    public void setTransAddress4(String transAddress4) {
        this.transAddress4 = transAddress4;
    }

    public void split() {
        if (address == null) {
            return;
        }

        String arr[] = address.split(",");
        ////// // System.out.println(arr);
        if (arr == null) {
            return;
        }
        try {
            transAddress1 = arr[0];
            transAddress2 = arr[1];
            transAddress3 = arr[2];
            transAddress4 = arr[3];
            transAddress5 = arr[4];
            transAddress6 = arr[5];
            transAddress7 = arr[6];
        } catch (Exception e) {
            ////// // System.out.println(e.getMessage());
        }

    }

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

        if (!(object instanceof Institution)) {
            return false;
        }
        Institution other = (Institution) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "lk.gov.health.entity.Institution[ id=" + id + " ]";
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

    public String getLabBillHeading() {
        return labBillHeading;
    }

    public void setLabBillHeading(String labBillHeading) {
        this.labBillHeading = labBillHeading;
    }

    public String getPharmacyBillHeading() {
        return pharmacyBillHeading;
    }

    public void setPharmacyBillHeading(String pharmacyBillHeading) {
        this.pharmacyBillHeading = pharmacyBillHeading;
    }

    public String getRadiologyBillHeading() {
        return radiologyBillHeading;
    }

    public void setRadiologyBillHeading(String radiologyBillHeading) {
        this.radiologyBillHeading = radiologyBillHeading;
    }

    public String getOpdBillHeading() {
        return opdBillHeading;
    }

    public void setOpdBillHeading(String opdBillHeading) {
        this.opdBillHeading = opdBillHeading;
    }

    public String getCashierBillHeading() {
        return cashierBillHeading;
    }

    public void setCashierBillHeading(String cashierBillHeading) {
        this.cashierBillHeading = cashierBillHeading;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getWeb() {
        return web;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public InstitutionType getInstitutionType() {
        return institutionType;
    }

    public void setInstitutionType(InstitutionType institutionType) {
        this.institutionType = institutionType;
    }

    public String getInstitutionCode() {
        if (institutionCode == null || institutionCode.trim().isEmpty()) {
            if (code != null && !code.trim().isEmpty()) {
                institutionCode = code;
            } else if (name != null && name.trim().length() >= 2) {
                institutionCode = name.trim().substring(0, 2).toUpperCase(); // Extract the first two letters and make them uppercase
            }
        }
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }

    public double getBallance() {
        return ballance;
    }

    public void setBallance(double ballance) {
        this.ballance = ballance;
    }

    public double getAllowedCredit() {
        return allowedCredit;
    }

    public void setAllowedCredit(double allowedCredit) {
        this.allowedCredit = allowedCredit;
    }

    public double getMaxCreditLimit() {
        return maxCreditLimit;
    }

    public void setMaxCreditLimit(double maxCreditLimit) {
        this.maxCreditLimit = maxCreditLimit;
    }

    public String getChequePrintingName() {
        if (chequePrintingName == null || chequePrintingName.trim().equals("")) {
            chequePrintingName = name;
        }
        return chequePrintingName;
    }

    public void setChequePrintingName(String chequePrintingName) {
        this.chequePrintingName = chequePrintingName;
    }

    @XmlTransient
    public List<Institution> getBranch() {
        return branch;
    }

    public void setBranch(List<Institution> branch) {
        this.branch = branch;
    }

    public Institution getInstitution() {

        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public double getPharmacyDiscount() {
        return pharmacyDiscount;
    }

    public void setPharmacyDiscount(double pharmacyDiscount) {
        this.pharmacyDiscount = pharmacyDiscount;
    }

    public Person getContactPerson() {
        if (contactPerson == null) {
            contactPerson = new Person();
        }
        return contactPerson;
    }

    public void setContactPerson(Person contactPerson) {
        this.contactPerson = contactPerson;
    }

    public double getStandardCreditLimit() {
        return standardCreditLimit;
    }

    public void setStandardCreditLimit(double standardCreditLimit) {
        this.standardCreditLimit = standardCreditLimit;
    }

    public List<AgentReferenceBook> getAgentReferenceBooks() {
        if (agentReferenceBooks == null) {
            agentReferenceBooks = new ArrayList<>();
        }
        return agentReferenceBooks;
    }

    public void setAgentReferenceBooks(List<AgentReferenceBook> agentReferenceBooks) {
        this.agentReferenceBooks = agentReferenceBooks;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getQbSupplierName() {
        return qbSupplierName;
    }

    public void setQbSupplierName(String qbSupplierName) {
        this.qbSupplierName = qbSupplierName;
    }

    public String getContactPersonName() {
        return contactPersonName;
    }

    public void setContactPersonName(String contactPersonName) {
        this.contactPersonName = contactPersonName;
    }

    public String getPaymentCompanyName() {
        return paymentCompanyName;
    }

    public void setPaymentCompanyName(String paymentCompanyName) {
        this.paymentCompanyName = paymentCompanyName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getLegalCompany() {
        return legalCompany;
    }

    public void setLegalCompany(String legalCompany) {
        this.legalCompany = legalCompany;
    }

    public Institution getBankBranch() {
        return bankBranch;
    }

    public void setBankBranch(Institution bankBranch) {
        this.bankBranch = bankBranch;
    }

    public boolean isInactive() {
        return inactive;
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }

    public String getTransAddress5() {
        return transAddress5;
    }

    public void setTransAddress5(String transAddress5) {
        this.transAddress5 = transAddress5;
    }

    public String getTransAddress6() {
        return transAddress6;
    }

    public void setTransAddress6(String transAddress6) {
        this.transAddress6 = transAddress6;
    }

    public String getTransAddress7() {
        return transAddress7;
    }

    public void setTransAddress7(String transAddress7) {
        this.transAddress7 = transAddress7;
    }

    public String getSmsSendingUsername() {
        return smsSendingUsername;
    }

    public void setSmsSendingUsername(String smsSendingUsername) {
        this.smsSendingUsername = smsSendingUsername;
    }

    public String getSmsSendingPassword() {
        return smsSendingPassword;
    }

    public void setSmsSendingPassword(String smsSendingPassword) {
        this.smsSendingPassword = smsSendingPassword;
    }

    public String getSmsSendingAlias() {
        return smsSendingAlias;
    }

    public void setSmsSendingAlias(String smsSendingAlias) {
        this.smsSendingAlias = smsSendingAlias;
    }

    @Override
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        if (code == null || code.trim().equals("")) {
            if (institutionCode != null && !institutionCode.trim().equals("")) {
                code = institutionCode;
            } else {
                code = CommonFunctions.nameToCode(name);
            }
        }
        this.code = code;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public CollectingCentrePaymentMethod getCollectingCentrePaymentMethod() {
        return CollectingCentrePaymentMethod;
    }

    public void setCollectingCentrePaymentMethod(CollectingCentrePaymentMethod CollectingCentrePaymentMethod) {
        this.CollectingCentrePaymentMethod = CollectingCentrePaymentMethod;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public double getAllowedCreditLimit() {
        return allowedCreditLimit;
    }

    public void setAllowedCreditLimit(double allowedCreditLimit) {
        this.allowedCreditLimit = allowedCreditLimit;
    }

    public Institution getParentInstitution() {
        return parentInstitution;
    }

    public void setParentInstitution(Institution parentInstitution) {
        this.parentInstitution = parentInstitution;
    }

    public Institution getLabInstitution() {
        return labInstitution;
    }

    public void setLabInstitution(Institution labInstitution) {
        this.labInstitution = labInstitution;
    }

    public Department getLabDepartment() {
        return labDepartment;
    }

    public void setLabDepartment(Department labDepartment) {
        this.labDepartment = labDepartment;
    }

    public Category getFeeListType() {
        return feeListType;
    }

    public void setFeeListType(Category feeListType) {
        this.feeListType = feeListType;
    }

    public double getCompanyBalance() {
        return companyBalance;
    }

    public void setCompanyBalance(double companyBalance) {
        this.companyBalance = companyBalance;
    }

    public double getAgentBalance() {
        return agentBalance;
    }

    public void setAgentBalance(double agentBalance) {
        this.agentBalance = agentBalance;
    }

}
