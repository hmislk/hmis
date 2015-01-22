/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.data.hr.EmployeeLeavingStatus;
import com.divudi.data.hr.EmployeeRecruitStatus;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
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
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author safrin
 */
@Entity
@XmlRootElement
public class StaffEmployment implements Serializable {

    @OneToMany(mappedBy = "staffEmployment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StaffBasics> staffBasics= new ArrayList<>();
    @OneToMany(mappedBy = "staffEmployment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StaffEmployeeStatus> staffEmployeeStatuss = new ArrayList<>();
    @OneToMany(mappedBy = "staffEmployment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StaffWorkingDepartment> staffWorkingDepartments = new ArrayList<>();
    @OneToMany(mappedBy = "staffEmployment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StaffStaffCategory> staffStaffCategorys = new ArrayList<>();
    @OneToMany(mappedBy = "staffEmployment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StaffGrade> staffGrades = new ArrayList<>();
    @OneToMany(mappedBy = "staffEmployment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StaffDesignation> staffDesignations = new ArrayList<>();
    @OneToOne
    private Staff staff;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    /////////////////////
    @Enumerated(EnumType.STRING)
    EmployeeLeavingStatus leavingStatus;
    @Lob
    String leavingComments;
    @Enumerated(EnumType.STRING)
    EmployeeRecruitStatus recruitStatus;
    @Lob
    String recruitComments;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date toDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date fromDate;
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

    public EmployeeLeavingStatus getLeavingStatus() {
        return leavingStatus;
    }

    public void setLeavingStatus(EmployeeLeavingStatus leavingStatus) {
        this.leavingStatus = leavingStatus;
    }

    public String getLeavingComments() {
        return leavingComments;
    }

    public void setLeavingComments(String leavingComments) {
        this.leavingComments = leavingComments;
    }

    public EmployeeRecruitStatus getRecruitStatus() {
        return recruitStatus;
    }

    public void setRecruitStatus(EmployeeRecruitStatus recruitStatus) {
        this.recruitStatus = recruitStatus;
    }

    public String getRecruitComments() {
        return recruitComments;
    }

    public void setRecruitComments(String recruitComments) {
        this.recruitComments = recruitComments;
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
        
        if (!(object instanceof StaffEmployment)) {
            return false;
        }
        StaffEmployment other = (StaffEmployment) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.hr.StaffEmployment[ id=" + id + " ]";
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
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

    @XmlTransient
    public List<StaffEmployeeStatus> getStaffEmployeeStatuss() {
        return staffEmployeeStatuss;
    }

    public void setStaffEmployeeStatuss(List<StaffEmployeeStatus> staffEmployeeStatuss) {
        this.staffEmployeeStatuss = staffEmployeeStatuss;
    }

    @XmlTransient
    public List<StaffWorkingDepartment> getStaffWorkingDepartments() {
        return staffWorkingDepartments;
    }

    public void setStaffWorkingDepartments(List<StaffWorkingDepartment> staffWorkingDepartments) {
        this.staffWorkingDepartments = staffWorkingDepartments;
    }

    @XmlTransient
    public List<StaffStaffCategory> getStaffStaffCategorys() {
        return staffStaffCategorys;
    }

    public void setStaffStaffCategorys(List<StaffStaffCategory> staffStaffCategorys) {
        this.staffStaffCategorys = staffStaffCategorys;
    }

    @XmlTransient
    public List<StaffGrade> getStaffGrades() {
        return staffGrades;
    }

    public void setStaffGrades(List<StaffGrade> staffGrades) {
        this.staffGrades = staffGrades;
    }

    @XmlTransient
    public List<StaffDesignation> getStaffDesignations() {
        return staffDesignations;
    }

    public void setStaffDesignations(List<StaffDesignation> staffDesignations) {
        this.staffDesignations = staffDesignations;
    }

    @XmlTransient
    public List<StaffBasics> getStaffBasics() {
        return staffBasics;
    }

    public void setStaffBasics(List<StaffBasics> staffBasics) {
        this.staffBasics = staffBasics;
    }


    
}
