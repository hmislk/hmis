package com.divudi.core.entity;

import com.divudi.core.data.AppointmentScheduleDateOverrideType;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

@Entity
public class AppointmentScheduleDateOverride implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private AppointmentScheduleTemplate template;

    @Temporal(javax.persistence.TemporalType.DATE)
    private Date overrideDate;

    @Enumerated(EnumType.STRING)
    private AppointmentScheduleDateOverrideType overrideType;

    private Integer overriddenMaxCount;

    @Temporal(javax.persistence.TemporalType.TIME)
    private Date overriddenStartTime;

    @Temporal(javax.persistence.TemporalType.TIME)
    private Date overriddenEndTime;

    private String reason;

    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;

    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppointmentScheduleTemplate getTemplate() {
        return template;
    }

    public void setTemplate(AppointmentScheduleTemplate template) {
        this.template = template;
    }

    public Date getOverrideDate() {
        return overrideDate;
    }

    public void setOverrideDate(Date overrideDate) {
        this.overrideDate = overrideDate;
    }

    public AppointmentScheduleDateOverrideType getOverrideType() {
        return overrideType;
    }

    public void setOverrideType(AppointmentScheduleDateOverrideType overrideType) {
        this.overrideType = overrideType;
    }

    public Integer getOverriddenMaxCount() {
        return overriddenMaxCount;
    }

    public void setOverriddenMaxCount(Integer overriddenMaxCount) {
        this.overriddenMaxCount = overriddenMaxCount;
    }

    public Date getOverriddenStartTime() {
        return overriddenStartTime;
    }

    public void setOverriddenStartTime(Date overriddenStartTime) {
        this.overriddenStartTime = overriddenStartTime;
    }

    public Date getOverriddenEndTime() {
        return overriddenEndTime;
    }

    public void setOverriddenEndTime(Date overriddenEndTime) {
        this.overriddenEndTime = overriddenEndTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AppointmentScheduleDateOverride)) {
            return false;
        }
        AppointmentScheduleDateOverride other = (AppointmentScheduleDateOverride) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "AppointmentScheduleDateOverride[ id=" + id + " ]";
    }
}
