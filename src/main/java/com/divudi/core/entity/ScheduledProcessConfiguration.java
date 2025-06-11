package com.divudi.core.entity;

import com.divudi.core.data.ScheduledFrequency;
import com.divudi.core.data.ScheduledProcess;
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
import javax.persistence.TemporalType;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.WebUser;

/**
 * Entity representing a configuration for a scheduled process.
 */
@Entity
public class ScheduledProcessConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ScheduledProcess scheduledProcess;

    @Enumerated(EnumType.STRING)
    private ScheduledFrequency scheduledFrequency;

    @ManyToOne
    private Institution institution;
    
    @ManyToOne
    private Institution site;

    @ManyToOne
    private Department department;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @ManyToOne
    private WebUser createdBy;

    private boolean retired;

    @ManyToOne
    private WebUser retiredBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date retiredAt;

    private String retireComments;

    @Temporal(TemporalType.TIMESTAMP)
    private Date nextSupposedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastSupposedAt;

    private Boolean lastProcessCompleted;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastRunStarted;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastRunEnded;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ScheduledProcess getScheduledProcess() {
        return scheduledProcess;
    }

    public void setScheduledProcess(ScheduledProcess scheduledProcess) {
        this.scheduledProcess = scheduledProcess;
    }

    public ScheduledFrequency getScheduledFrequency() {
        return scheduledFrequency;
    }

    public void setScheduledFrequency(ScheduledFrequency scheduledFrequency) {
        this.scheduledFrequency = scheduledFrequency;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public WebUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(WebUser createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public WebUser getRetiredBy() {
        return retiredBy;
    }

    public void setRetiredBy(WebUser retiredBy) {
        this.retiredBy = retiredBy;
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

    public Date getNextSupposedAt() {
        return nextSupposedAt;
    }

    public void setNextSupposedAt(Date nextSupposedAt) {
        this.nextSupposedAt = nextSupposedAt;
    }

    public Date getLastSupposedAt() {
        return lastSupposedAt;
    }

    public void setLastSupposedAt(Date lastSupposedAt) {
        this.lastSupposedAt = lastSupposedAt;
    }

    public Boolean getLastProcessCompleted() {
        return lastProcessCompleted;
    }

    public void setLastProcessCompleted(Boolean lastProcessCompleted) {
        this.lastProcessCompleted = lastProcessCompleted;
    }

    public Date getLastRunStarted() {
        return lastRunStarted;
    }

    public void setLastRunStarted(Date lastRunStarted) {
        this.lastRunStarted = lastRunStarted;
    }

    public Date getLastRunEnded() {
        return lastRunEnded;
    }

    public void setLastRunEnded(Date lastRunEnded) {
        this.lastRunEnded = lastRunEnded;
    }
    
    
    

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ScheduledProcessConfiguration)) {
            return false;
        }
        ScheduledProcessConfiguration other = (ScheduledProcessConfiguration) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.ScheduledProcessConfiguration[ id=" + id + " ]";
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }
}
