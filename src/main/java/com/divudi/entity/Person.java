/*
 * Author : Dr. M H B Ariyaratne
 *
 * Acting Consultant (Health Informatics), Department of Health Services, Southern Province
 * (94) 71 5812399
 * Email : buddhika.ari@gmail.com
 */
package com.divudi.entity;

import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.entity.clinical.ClinicalFindingValue;
import com.divudi.entity.membership.MembershipScheme;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Entity
@XmlRootElement
public class Person implements Serializable {

    @OneToOne(mappedBy = "webUserPerson",cascade = CascadeType.ALL) @JsonIgnore
    private WebUser webUser;

    @OneToMany(mappedBy = "person",fetch = FetchType.LAZY)
    private List<ClinicalFindingValue> clinicalFindingValues;

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String name;
    String description;
    String nic;
    String address;
    String fax;
    String email;
    String website;
    String mobile;
    @Column(name = "TNAME")
    String fullName;
    @Column(name = "SNAME")
    String nameWithInitials;
    String phone;
    String initials;
    String surName;
    String lastName;
    String zoneCode;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date dob;

    //Created Properties
    @ManyToOne @JsonIgnore
    WebUser creater; @JsonIgnore
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
//    @ManyToOne
//    WebUser editer;
//    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
//    Date editedAt;
    //Retairing properties
     @JsonIgnore boolean retired;
    @ManyToOne  @JsonIgnore
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)  @JsonIgnore
    Date retiredAt; @JsonIgnore
    String retireComments;
    @ManyToOne   @JsonIgnore
    Area area;
    @ManyToOne  @JsonIgnore
    Institution institution;
    @ManyToOne @JsonIgnore
    Department department;
    @Enumerated(EnumType.STRING)
    Title title;
    @Enumerated(EnumType.STRING)
    Sex sex;
    @Transient
    String nameWithTitle;
    boolean foreigner = false;

    @ManyToOne @JsonIgnore
    private MembershipScheme membershipScheme;

    @Transient
    int ageMonths;
    @Transient
    int ageDays;
    @Transient
    int ageYears;
    @Transient
    String age;
    @Transient
    long ageInDays;
    @Transient
    int serealNumber;
    
    
    
    
    public boolean isForeigner() {
        return foreigner;
    }

    public void setForeigner(boolean foreigner) {
        this.foreigner = foreigner;
    }

    public void calAgeFromDob() {
        age = "";
        ageInDays = 0l;
        ageMonths = 0;
        ageDays = 0;
        ageYears = 0;
        if (getDob() == null) {
            return;
        }

        LocalDate dob = new LocalDate(getDob());
        LocalDate date = new LocalDate(new Date());

        Period period = new Period(dob, date, PeriodType.yearMonthDay());
        ageYears = period.getYears();
        ageMonths = period.getMonths();
        ageDays = period.getDays();
        if (ageYears > 12) {
            age = period.getYears() + " years.";
        } else if (ageYears > 0) {
            age = period.getYears() + " years and " + period.getMonths() + " months.";
        } else {
            age = period.getMonths() + " months and " + period.getDays() + " days.";
        }
        period = new Period(dob, date, PeriodType.days());
        ageInDays = (long) period.getDays();
    }

    public String getAge() {
        calAgeFromDob();
        return age;
    }

    public Long getAgeInDays() {
        calAgeFromDob();
        return ageInDays;
    }

    public int getAgeMonths() {
        calAgeFromDob();
        return ageMonths;
    }

    public int getAgeDays() {
        calAgeFromDob();
        return ageDays;
    }

    public int getAgeYears() {
        calAgeFromDob();
        return ageYears;
    }

    public String getNameWithTitle() {
        String temT;
        Title t = getTitle();
        if (t != null) {
            temT = t.getLabel();
        } else {
            temT = "";
        }
        nameWithTitle = temT + " " + getName();
        return nameWithTitle;
    }

    public void setNameWithTitle(String nameWithTitle) {
        this.nameWithTitle = nameWithTitle.toUpperCase();
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

//    public WebUser getEditer() {
//        return editer;
//    }
//
//    public void setEditer(WebUser editer) {
//        this.editer = editer;
//    }
//
//    public Date getEditedAt() {
//        return editedAt;
//    }
//
//    public void setEditedAt(Date editedAt) {
//        this.editedAt = editedAt;
//    }

    public WebUser getCreater() {
        return creater;
    }

    public void setCreater(WebUser creater) {
        this.creater = creater;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.toUpperCase();
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

    public String getSurName() {
        return surName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }

    public String getZoneCode() {
        return zoneCode;
    }

    public void setZoneCode(String zoneCode) {
        this.zoneCode = zoneCode;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public WebUser getRetirer() {
        return retirer;
    }

    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Person)) {
            return false;
        }
        Person other = (Person) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address.toUpperCase();
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

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
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

    public Title getTitle() {
        return title;
    }

    public void setTitle(Title title) {
        this.title = title;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getNameWithInitials() {
        return nameWithInitials;
    }

    public void setNameWithInitials(String nameWithInitials) {
        this.nameWithInitials = nameWithInitials;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @XmlTransient
    public List<ClinicalFindingValue> getClinicalFindingValues() {
        return clinicalFindingValues;
    }

    public void setClinicalFindingValues(List<ClinicalFindingValue> clinicalFindingValues) {
        this.clinicalFindingValues = clinicalFindingValues;
    }

    public MembershipScheme getMembershipScheme() {
        return membershipScheme;
    }

    public void setMembershipScheme(MembershipScheme membershipScheme) {
        this.membershipScheme = membershipScheme;
    }

    public int getSerealNumber() {
        return serealNumber;
    }

    public void setSerealNumber(int serealNumber) {
        this.serealNumber = serealNumber;
    }

    public WebUser getWebUser() {
        if(webUser==null){
            webUser = new WebUser();
            webUser.setWebUserPerson(this);
        }
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }
}
