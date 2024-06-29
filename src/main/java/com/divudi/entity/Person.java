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
import com.divudi.entity.membership.MembershipScheme;
import java.io.Serializable;
import java.util.Date;
import javax.annotation.PostConstruct;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Entity
public class Person implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    static final long serialVersionUID = 1L;

    String name;
    String description;
    String nic;
    String address;
    String fax;
    String email;
    String website;
    String mobile;
    String phone;
    @Column(name = "TNAME")
    String fullName;
    @Column(name = "SNAME")
    String nameWithInitials;

    @OneToOne(mappedBy = "webUserPerson", cascade = CascadeType.ALL)
    private WebUser webUser;

    @Transient
    boolean ageCalculated = false;

    String initials;
    String surName;
    String lastName;
    String zoneCode;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date dob;

    //Created Properties
    @ManyToOne
    WebUser creater;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
    @ManyToOne
    WebUser editer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date editedAt;

    //    Retairing properties
    boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    String retireComments;
    @ManyToOne

    Area area;
    @ManyToOne

    Institution institution;
    @ManyToOne

    Department department;
    @Enumerated(EnumType.STRING)
    Title title;
    @Enumerated(EnumType.STRING)
    Sex sex;
    @Transient
    String nameWithTitle;
    boolean foreigner = false;

    @ManyToOne
    Item civilStatus;
    @ManyToOne
    Item race;
    @ManyToOne
    Item bloodGroup;
    @ManyToOne
    Item occupation;
    @ManyToOne
    private Item religion;

    @ManyToOne
    @Deprecated
    private MembershipScheme membershipScheme;

    @Transient
    int ageMonthsComponent;
    @Transient
    int ageDaysComponent;
    @Transient
    int ageYearsComponent;
    @Transient
    String ageAsString;
    @Transient
    long ageInDays;
    @Transient
    int serealNumber;
    @Transient
    private String smsNumber;

//    @Inject
//    SessionController SessionController;
    @PostConstruct
    public void init() {
        calAgeFromDob();
    }

    public Item getCivilStatus() {
        return civilStatus;
    }

    public void setCivilStatus(Item civilStatus) {
        this.civilStatus = civilStatus;
    }

    public Item getRace() {
        return race;
    }

    public void setRace(Item race) {
        this.race = race;
    }

    public Item getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(Item bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public Item getOccupation() {
        return occupation;
    }

    public void setOccupation(Item occupation) {
        this.occupation = occupation;
    }

    public boolean isForeigner() {
        return foreigner;
    }

    public void setForeigner(boolean foreigner) {
        this.foreigner = foreigner;
    }

    public void calAgeFromDob() {
        ageAsString = "";
        ageInDays = 0l;
        if (getDob() == null) {
            return;
        }

        LocalDate ldDob = new LocalDate(getDob());
        LocalDate currentDate = LocalDate.now();

        Period period = new Period(ldDob, currentDate, PeriodType.yearMonthDay());

        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();

        if (years > 12) {
            ageAsString = years + " years";
        } else if (years > 0) {
            ageAsString = years + " years and " + months + " months";
        } else {
            ageAsString = months + " months and " + days + " days";
        }

        period = new Period(ldDob, currentDate, PeriodType.days());
        ageInDays = (long) period.getDays();
        ageDaysComponent = days;
        ageMonthsComponent = months;
        ageYearsComponent = years;
    }

    public void calDobFromAge() {
        LocalDate currentDate = new LocalDate();
        LocalDate ldDob = currentDate.minusYears(ageYearsComponent).minusMonths(ageMonthsComponent).minusDays(ageDaysComponent);
        this.dob = ldDob.toDate();
    }

    public String getAgeAsString() {
        calAgeFromDob();
        if (ageAsString == null || ageAsString.trim().equals("")) {
            ageAsString = "Not Recorded";
        }
        return ageAsString;
    }

    public Long getAgeInDays() {
        return ageInDays;
    }

    public int getAgeMonthsComponent() {
        calAgeFromDob();
        return ageMonthsComponent;
    }

    public int getAgeDaysComponent() {
        calAgeFromDob();
        return ageDaysComponent;
    }

    public int getAgeYearsComponent() {
        calAgeFromDob();
        return ageYearsComponent;
    }

    public void setAgeMonthsComponent(int ageMonthsComponent) {
        this.ageMonthsComponent = ageMonthsComponent;
        calDobFromAge();
    }

    public void setAgeDaysComponent(int ageDaysComponent) {
        this.ageDaysComponent = ageDaysComponent;
        calDobFromAge();
    }

    public void setAgeYearsComponent(int ageYearsComponent) {
        this.ageYearsComponent = ageYearsComponent;
        calDobFromAge();
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
        this.name = name;
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
        if (title == null) {
            return;
        }
        switch (this.title) {
            case Dr:
            case Master:
            case Mr:
            case Prof:
                this.sex = Sex.Male;
                break;
            case DrMiss:
            case DrMrs:
            case DrMs:
            case Miss:
            case Mrs:
            case Ms:
            case ProfMrs:
                this.sex = Sex.Female;
                break;
            default:
        }

    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
        calAgeFromDob();
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
        if (webUser == null) {
            webUser = new WebUser();
            webUser.setWebUserPerson(this);
        }
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public Item getReligion() {
        return religion;
    }

    public void setReligion(Item religion) {
        this.religion = religion;
    }

    public String getSmsNumber() {
        if (StringUtils.isNotBlank(mobile)) {
            return mobile;
        } else if (StringUtils.isNotBlank(phone)) {
            return phone;
        } else {
            return "";
        }
    }

    public void setSmsNumber(String smsNumber) {
        this.smsNumber = smsNumber;
    }

}
