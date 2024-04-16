/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity;

import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PostLoad;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

/**
 *
 * @author buddhika
 */
@Entity
public class Patient implements Serializable {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    //Main Properties
    Long id;

    private Long patientPhoneNumber;
    @ManyToOne
    Person person;
    //personaI dentification Number
    Integer pinNo;
    //healthdentification Number
    Integer hinNo;
    //Created Properties
    @ManyToOne
    Institution createdInstitution;
    @ManyToOne
    WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
    @ManyToOne
    WebUser editer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date editedAt;
    //Retairing properties
    boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    String retireComments;

    @Transient
    String age;
    @Transient
    private String ageOnBilledDate;
    @Transient
    Long ageInDays;
    @Transient
    private Long ageInDaysOnBilledDate;
    @Transient
    private Date billedDate;
    @Transient
    private boolean editingMode;
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    @Basic(fetch = FetchType.LAZY)
    byte[] baImage;
    String fileName;
    String fileType;
    String code;
//    int code
    @Lob
    String comments;
    @Transient
    int ageMonths;
    @Transient
    int ageDays;
    @Transient
    int ageYears;
    @Transient
    private int ageMonthsOnBilledDate;
    @Transient
    private int ageDaysOnBilledDate;
    @Transient
    private int ageYearsonBilledDate;
    @Temporal(TemporalType.TIMESTAMP)
    Date fromDate;
    @Temporal(TemporalType.TIMESTAMP)
    Date toDate;
    @Size(max = 15)
    String phn;

    private Boolean hasAnAccount;
    private Double runningBalance;
    private Double creditLimit;

    private Long patientId;

    @Transient
    Bill bill;
    @Transient
    CommonFunctions commonFunctions;
            
    @Transient
    private String phoneNumberStringTransient;

    private Boolean cardIssues;

    @Temporal(javax.persistence.TemporalType.DATE)
    private Date cardIssuedDate;

    public Patient() {
        editingMode = true;
    }

    public void toggleEditMode() {
        editingMode = !editingMode;
    }

    public Institution getCreatedInstitution() {
        return createdInstitution;
    }

    public void setCreatedInstitution(Institution createdInstitution) {
        this.createdInstitution = createdInstitution;
    }

    public String getPhn() {

        return phn;
    }

    public void setPhn(String phn) {

        this.phn = phn;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @PostLoad
    @Deprecated
    private void onLoad() {
        calAgeFromDob();
    }

    @Deprecated
    public void calAgeFromDob() {
        age = "";
        ageInDays = 0l;
        ageMonths = 0;
        ageDays = 0;
        ageYears = 0;
        if (person == null) {
            age = "No Person";
            return;
        }
        if (person.getDob() == null) {
            age = "Date of birth NOT recorded.";
            return;
        }

        LocalDate dob = new LocalDate(person.getDob());
        LocalDate date = new LocalDate(new Date());

        Period period = new Period(dob, date, PeriodType.yearMonthDay());
        ageYears = period.getYears();
        ageMonths = period.getMonths();
        ageDays = period.getDays();
        if (ageYears > 12) {
            age = period.getYears() + " years.";
        } else if (ageYears > 0) {
            if (period.getMonths() > 0) {
                age = period.getYears() + " years and " + period.getMonths() + " months.";
            } else {
                age = period.getYears() + " years.";
            }
        } else {
            if (period.getDays() > 0) {
                age = period.getMonths() + " months and " + period.getDays() + " days.";
            } else {
                age = period.getMonths() + " months.";
            }
        }
        period = new Period(dob, date, PeriodType.days());
        ageInDays = (long) period.getDays();
    }

    @Deprecated
    public void calAgeFromDob(Date billedDate) {
        this.billedDate = billedDate;
        ageOnBilledDate = "";
        ageInDaysOnBilledDate = 0l;
        ageMonthsOnBilledDate = 0;
        ageDaysOnBilledDate = 0;
        ageYearsonBilledDate = 0;
        if (person == null) {
            ageOnBilledDate = "No Person";
            return;
        }
        if (person.getDob() == null) {
            ageOnBilledDate = "Date of birth NOT recorded.";
            return;
        }

        LocalDate dob = new LocalDate(person.getDob());
        LocalDate date = new LocalDate(billedDate);

        Period period = new Period(dob, date, PeriodType.yearMonthDay());
        ageYearsonBilledDate = period.getYears();
        ageMonthsOnBilledDate = period.getMonths();
        ageDaysOnBilledDate = period.getDays();
        if (ageYearsonBilledDate > 12) {
            ageOnBilledDate = period.getYears() + " years.";
        } else if (ageYearsonBilledDate > 0) {
            if (period.getMonths() > 0) {
                ageOnBilledDate = period.getYears() + " years and " + period.getMonths() + " months.";
            } else {
                ageOnBilledDate = period.getYears() + " years.";
            }
        } else {
            if (period.getDays() > 0) {
                ageOnBilledDate = period.getMonths() + " months and " + period.getDays() + " days.";
            } else {
                ageOnBilledDate = period.getMonths() + " months.";
            }
        }
        period = new Period(dob, date, PeriodType.days());
        ageInDaysOnBilledDate = (long) period.getDays();
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    @Deprecated
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

    public String getIdStr() {
        String formatted = String.format("%07d", id);
        return formatted;
    }

    @Deprecated
    public int getAgeDays() {
        calAgeFromDob();
        return ageDays;
    }

    @Deprecated
    public int getAgeYears() {
        calAgeFromDob();
        return ageYears;
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

        if (!(object instanceof Patient)) {
            return false;
        }
        Patient other = (Patient) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.Patient[ id=" + id + " ]";
    }

    public Person getPerson() {
        if (person == null) {
            person = new Person();
        }
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Integer getPinNo() {
        return pinNo;
    }

    public void setPinNo(Integer pinNo) {
        this.pinNo = pinNo;
    }

    public Integer getHinNo() {
        return hinNo;
    }

    public void setHinNo(Integer hinNo) {
        this.hinNo = hinNo;
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

    public byte[] getBaImage() {
        return baImage;
    }

    public void setBaImage(byte[] baImage) {
        this.baImage = baImage;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public String getAgeOnBilledDate() {
        calAgeFromDob(billedDate);
        return ageOnBilledDate;
    }

    public String getAgeOnBilledDate(Date billedDate) {
        calAgeFromDob(billedDate);
        return ageOnBilledDate;
    }

    public String ageOnBilledDate(Date billedDate) {
        calAgeFromDob(billedDate);
        return ageOnBilledDate;
    }

    public Long getAgeInDaysOnBilledDate() {
        return ageInDaysOnBilledDate;
    }

    public void setAgeInDaysOnBilledDate(Long ageInDaysOnBilledDate) {
        this.ageInDaysOnBilledDate = ageInDaysOnBilledDate;
    }

    public int getAgeMonthsOnBilledDate() {
        return ageMonthsOnBilledDate;
    }

    public void setAgeMonthsOnBilledDate(int ageMonthsOnBilledDate) {
        this.ageMonthsOnBilledDate = ageMonthsOnBilledDate;
    }

    public int getAgeDaysOnBilledDate() {
        return ageDaysOnBilledDate;
    }

    public void setAgeDaysOnBilledDate(int ageDaysOnBilledDate) {
        this.ageDaysOnBilledDate = ageDaysOnBilledDate;
    }

    public int getAgeYearsonBilledDate() {
        return ageYearsonBilledDate;
    }

    public void setAgeYearsonBilledDate(int ageYearsonBilledDate) {
        this.ageYearsonBilledDate = ageYearsonBilledDate;
    }

    public Date getBilledDate() {
        return billedDate;
    }

    public void setBilledDate(Date billedDate) {
        this.billedDate = billedDate;
    }

    public Boolean getCardIssues() {
        return cardIssues;
    }

    public void setCardIssues(Boolean cardIssues) {
        this.cardIssues = cardIssues;
    }

    public Date getCardIssuedDate() {
        return cardIssuedDate;
    }

    public void setCardIssuedDate(Date cardIssuedDate) {
        this.cardIssuedDate = cardIssuedDate;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Boolean getHasAnAccount() {
        return hasAnAccount;
    }

    public void setHasAnAccount(Boolean hasAnAccount) {
        this.hasAnAccount = hasAnAccount;
    }

    public Double getRunningBalance() {
        return runningBalance;
    }

    public void setRunningBalance(Double runningBalance) {
        this.runningBalance = runningBalance;
    }

    public Double getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(Double creditLimit) {
        this.creditLimit = creditLimit;
    }

    public boolean isEditingMode() {
        return editingMode;
    }

    public void setEditingMode(boolean editingMode) {
        this.editingMode = editingMode;
    }

    public String getPhoneNumberStringTransient() {
        if (this.getPerson() == null) {
            return null;
        }
        phoneNumberStringTransient = this.getPerson().getPhone();
        return phoneNumberStringTransient;
    }

    public void setPhoneNumberStringTransient(String phoneNumberStringTransient) {
        try {
            if (this.getPerson() == null) {
                return;
            }
            this.getPerson().setMobile(phoneNumberStringTransient);
            this.patientPhoneNumber = commonFunctions.removeSpecialCharsInPhonenumber(phoneNumberStringTransient);  
            this.phoneNumberStringTransient = phoneNumberStringTransient;
        } catch (Exception e) {
        }
    }

    public Long getPatientPhoneNumber() {
        return patientPhoneNumber;
    }

    public void setPatientPhoneNumber(Long patientPhoneNumber) {
        this.patientPhoneNumber = patientPhoneNumber;
    }

}
