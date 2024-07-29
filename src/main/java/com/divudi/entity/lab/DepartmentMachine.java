package com.divudi.entity.lab;

import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.WebUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Entity
public class DepartmentMachine implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Department department;
    private Machine machine;
    private String name;
    private String code;
    private String serialNumber;
    @Lob
    private String description;

    //Created Properties
    @ManyToOne
    @JsonIgnore
    private WebUser creater;
    @JsonIgnore
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Retairing properties
    @JsonIgnore
    private boolean retired;
    @JsonIgnore
    @ManyToOne
    private WebUser retirer;
    @JsonIgnore
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;
    private Boolean active;
    @ManyToOne
    private Institution supplier;
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date manufactureDate;
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date installationDate;
    private String status;
    private String location;
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date calibrationDate;
    private String maintenanceSchedule;
    private String networkSettings;

    public DepartmentMachine() {
    }

    public DepartmentMachine(Department department, Machine machine) {
        this.department = department;
        this.machine = machine;
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
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DepartmentMachine)) {
            return false;
        }
        DepartmentMachine other = (DepartmentMachine) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.lab.DepartmentMachine[ id=" + id + " ]";
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
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
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Institution getSupplier() {
        return supplier;
    }

    public void setSupplier(Institution supplier) {
        this.supplier = supplier;
    }

    public Date getManufactureDate() {
        return manufactureDate;
    }

    public void setManufactureDate(Date manufactureDate) {
        this.manufactureDate = manufactureDate;
    }

    public Date getInstallationDate() {
        return installationDate;
    }

    public void setInstallationDate(Date installationDate) {
        this.installationDate = installationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getCalibrationDate() {
        return calibrationDate;
    }

    public void setCalibrationDate(Date calibrationDate) {
        this.calibrationDate = calibrationDate;
    }

    public String getMaintenanceSchedule() {
        return maintenanceSchedule;
    }

    public void setMaintenanceSchedule(String maintenanceSchedule) {
        this.maintenanceSchedule = maintenanceSchedule;
    }

    public String getNetworkSettings() {
        return networkSettings;
    }

    public void setNetworkSettings(String networkSettings) {
        this.networkSettings = networkSettings;
    }

}
