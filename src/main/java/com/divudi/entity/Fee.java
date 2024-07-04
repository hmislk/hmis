
/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity;


import com.divudi.data.FeeType;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import javax.persistence.Transient;

/**
 *
 * @author buddhika
 */
@Entity
@Inheritance
public class Fee implements Serializable {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    //Main Properties
    Long id;
    String name;
    @Lob
    String description;
    String code;
    double fee = 0.0;
    double ffee = 0.0;
    private double ccFee = 0.0;
    @JsonIgnore
    @ManyToOne
    Item item; // FBC, ESR, UFR
    @ManyToOne
    private Institution forInstitution;
    @ManyToOne
    private Category forCategory;
    @ManyToOne
    Institution institution;
    @ManyToOne
    Department department;
    @ManyToOne
    Speciality speciality;
    @ManyToOne
    Staff staff;
    @ManyToOne
    @JsonIgnore
    ServiceSession serviceSession;
    private boolean booleanValue;
    //Created Properties
    @ManyToOne
    @JsonIgnore
    WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @JsonIgnore
    Date createdAt;
    //Created Properties
    @ManyToOne
    @JsonIgnore
    WebUser editer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @JsonIgnore
    Date editedAt;
    //Retairing properties
    @JsonIgnore
    boolean retired;
    @ManyToOne
    @JsonIgnore
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @JsonIgnore
    Date retiredAt;
    @JsonIgnore
    String retireComments;
    @Enumerated(EnumType.STRING)
    FeeType feeType;
    @ManyToOne
    @JsonIgnore
    Item packege;  //Ceylinco, LEC ,
    @ManyToOne
    @JsonIgnore
    Department fromDepartment;
    @ManyToOne
    @JsonIgnore
    Department toDepartment;
    @JsonIgnore
    @ManyToOne
    Institution fromInstitution;
    @JsonIgnore
    @ManyToOne
    Institution toInstitution;
    @JsonIgnore
    @ManyToOne
    Staff fromStaff;
    @ManyToOne
    @JsonIgnore
    Staff toStaff;
    @ManyToOne
    @JsonIgnore
    Speciality fromSpeciality;
    @ManyToOne
    @JsonIgnore
    Speciality toSpaciality;
    private boolean discountAllowed;

    
    
    
    public Fee() {
    }

    public double getFfee() {
        if (ffee == 0.0) {
            ffee = fee;
        }
        return ffee;
    }

    public void setFfee(double ffee) {
        this.ffee = ffee;
    }

    public ServiceSession getServiceSession() {
        return serviceSession;
    }

    public void setServiceSession(ServiceSession serviceSession) {
        this.serviceSession = serviceSession;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public void setFeeType(FeeType feeType) {
        this.feeType = feeType;
    }

    public Item getPackege() {
        return packege;
    }

    public void setPackege(Item packege) {
        this.packege = packege;
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

        if (!(object instanceof Fee)) {
            return false;
        }
        Fee other = (Fee) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.Fee[ id=" + id + " ]";
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
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

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Staff getFromStaff() {
        return fromStaff;
    }

    public void setFromStaff(Staff fromStaff) {
        this.fromStaff = fromStaff;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public Speciality getFromSpeciality() {
        return fromSpeciality;
    }

    public void setFromSpeciality(Speciality fromSpeciality) {
        this.fromSpeciality = fromSpeciality;
    }

    public Speciality getToSpaciality() {
        return toSpaciality;
    }

    public void setToSpaciality(Speciality toSpaciality) {
        this.toSpaciality = toSpaciality;
    }
    @Transient
    double hospitalFee;
    @Transient
    double professionalFee;
    @Transient
    double hospitalFfee;
    @Transient
    double professionalFfee;

    public double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public double getProfessionalFee() {
        return professionalFee;
    }

    public void setProfessionalFee(double professionalFee) {
        this.professionalFee = professionalFee;
    }

    public double getHospitalFfee() {
        return hospitalFfee;
    }

    public void setHospitalFfee(double hospitalFfee) {
        this.hospitalFfee = hospitalFfee;
    }

    public double getProfessionalFfee() {
        return professionalFfee;
    }

    public void setProfessionalFfee(double professionalFfee) {
        this.professionalFfee = professionalFfee;
    }

    public boolean isBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public boolean isDiscountAllowed() {
        return discountAllowed;
    }

    public void setDiscountAllowed(boolean discountAllowed) {
        this.discountAllowed = discountAllowed;
    }

    public double getCcFee() {
        return ccFee;
    }

    public void setCcFee(double ccFee) {
        this.ccFee = ccFee;
    }

    public Institution getForInstitution() {
        return forInstitution;
    }

    public void setForInstitution(Institution forInstitution) {
        this.forInstitution = forInstitution;
    }

    public Category getForCategory() {
        return forCategory;
    }

    public void setForCategory(Category forCategory) {
        this.forCategory = forCategory;
    }


}
