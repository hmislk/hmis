/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity;

import com.divudi.core.data.DepartmentType;
import com.divudi.core.util.CommonFunctions;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 * @author buddhika
 */
@Entity
@Inheritance
public class Department implements Serializable {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    //Main Properties
    Long id;
    String departmentCode;
    String name;
    @Lob
    String description;
    String code;
    String printingName;
    String address;
    String telephone1;
    String telephone2;
    String fax;
    String email;

    @ManyToOne
    Institution institution;

    @ManyToOne
    private Institution site;

    @ManyToOne
    Department superDepartment;
    DepartmentType departmentType;
    @ManyToOne

    Department sampleDepartment;
    @ManyToOne

    Department labDepartment;

    @ManyToOne

    Institution sampleInstitution;
    @ManyToOne

    Institution labInstitution;
//     double maxDiscount;

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
    private Boolean active;

    double margin;
    double pharmacyMarginFromPurchaseRate;

    public double getPharmacyMarginFromPurchaseRate() {
        return pharmacyMarginFromPurchaseRate;
    }

    public void setPharmacyMarginFromPurchaseRate(double pharmacyMarginFromPurchaseRate) {
        this.pharmacyMarginFromPurchaseRate = pharmacyMarginFromPurchaseRate;
    }

    public double getMargin() {
        return margin;
    }

    public void setMargin(double margin) {
        this.margin = margin;
    }

    public Institution getSampleInstitution() {
        return sampleInstitution;
    }

    public void setSampleInstitution(Institution sampleInstitution) {
        this.sampleInstitution = sampleInstitution;
    }

    public Institution getLabInstitution() {
        return labInstitution;
    }

    public void setLabInstitution(Institution labInstitution) {
        this.labInstitution = labInstitution;
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

        if (!(object instanceof Department)) {
            return false;
        }
        Department other = (Department) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getSuperDepartment() {
        return superDepartment;
    }

    public void setSuperDepartment(Department superDepartment) {
        this.superDepartment = superDepartment;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        if (code == null || code.trim().equals("")) {
            if (departmentCode != null && !departmentCode.trim().equals("")) {
                code = departmentCode;
            } else {
                code = CommonFunctions.nameToCode(name);
            }
        }
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTname() {
        return code;
    }

    public void setTname(String tName) {
        this.code = tName;
    }

    public DepartmentType getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(DepartmentType departmentType) {
        this.departmentType = departmentType;
    }

    public Department getSampleDepartment() {
        return sampleDepartment;
    }

    public void setSampleDepartment(Department sampleDepartment) {
        this.sampleDepartment = sampleDepartment;
    }

    public Department getLabDepartment() {
        return labDepartment;
    }

    public void setLabDepartment(Department labDepartment) {
        this.labDepartment = labDepartment;
    }

    public String getDepartmentCode() {
        if (departmentCode == null || departmentCode.trim().isEmpty()) {
            if (code != null && !code.trim().isEmpty()) {
                departmentCode = code;
            } else if (name != null && name.trim().length() >= 2) {
                departmentCode = name.trim().substring(0, 2).toUpperCase(); // Extract the first two letters and make them uppercase
            }
        }
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    //    public double getMaxDiscount() {
//        return maxDiscount;
//    }
//
//    public void setMaxDiscount(double maxDiscount) {
//        this.maxDiscount = maxDiscount;
//    }
    public String getPrintingName() {
        return printingName;
    }

    public void setPrintingName(String printingName) {
        this.printingName = printingName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTelephone1() {
        return telephone1;
    }

    public void setTelephone1(String telephone1) {
        this.telephone1 = telephone1;
    }

    public String getTelephone2() {
        return telephone2;
    }

    public void setTelephone2(String telephone2) {
        this.telephone2 = telephone2;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public String trim() {
        if (name != null) {
            return name.trim();
        }
        return null;
    }
}
