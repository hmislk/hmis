package com.divudi.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import java.util.UUID;
import javax.persistence.Column;

/**
 *
 * @author Senula Nanayakkara
 */
@Entity
public class AuditEvent implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Date eventDataTime;
    private Date eventEndTime;
    private Long webUserId;
    @Column(columnDefinition = "CHAR(36)")
    private String uuid; // Changed from UUID to String

    @Lob
    private String url;
    private String eventTrigger;
    private Long institutionId;
    private Long departmentId;

    private String beforeEdit;
    private String afterEdit;
    private Long eventDuration;
    private String eventStatus;
    private String ipAddress;

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
        if (!(object instanceof AuditEvent)) {
            return false;
        }
        AuditEvent other = (AuditEvent) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.AuditEvent[ id=" + id + " ]";
    }

    public Date getEventDataTime() {
        return eventDataTime;
    }

    public void setEventDataTime(Date eventDataTime) {
        this.eventDataTime = eventDataTime;
    }

    public Long getWebUserId() {
        return webUserId;
    }

    public void setWebUserId(Long webUserId) {
        this.webUserId = webUserId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEventTrigger() {
        return eventTrigger;
    }

    public void setEventTrigger(String eventTrigger) {
        this.eventTrigger = eventTrigger;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getBeforeEdit() {
        return beforeEdit;
    }

    public void setBeforeEdit(String beforeEdit) {
        this.beforeEdit = beforeEdit;
    }

    public String getAfterEdit() {
        return afterEdit;
    }

    public void setAfterEdit(String afterEdit) {
        this.afterEdit = afterEdit;
    }

    public Long getEventDuration() {
        return eventDuration;
    }

    public void setEventDuration(Long eventDuration) {
        this.eventDuration = eventDuration;
    }

    public String getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(String eventStatus) {
        this.eventStatus = eventStatus;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

  

    public Date getEventEndTime() {
        return eventEndTime;
    }

    public void setEventEndTime(Date eventEndTime) {
        this.eventEndTime = eventEndTime;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

}
