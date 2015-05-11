/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import com.divudi.data.InstitutionType;
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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author buddhika
 */
@Entity
@XmlRootElement
public class Institution implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY)
    Institution institution;
    @ManyToOne(cascade = CascadeType.ALL)
    private Person contactPerson;

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    //Main Properties   
    Long id;
    String institutionCode;
    String name;
    String address;
    String fax;
    String email;
    String phone;
    String mobile;
    String web;
    String chequePrintingName;

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
    double ballance;
    double allowedCredit;
    @Transient
    String transAddress1;
    @Transient
    String transAddress2;
    @Transient
    String transAddress3;
    @Transient
    String transAddress4;
    
    @OneToMany(mappedBy = "institution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Institution> branch = new ArrayList<>();
    @Lob
    String descreption;

    public Institution() {
        split();
    }   
    

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
        if(address == null){
            return;
        }
        
        String arr[] = address.split(",");
        //System.out.println(arr);
        if(arr==null){
            return;
        }
       try{
            transAddress1=arr[0];
            transAddress2=arr[1];
            transAddress3=arr[2];
            transAddress4=arr[3];
       }catch(Exception e){
           //System.out.println(e.getMessage());
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

    public String getChequePrintingName() {
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

}
