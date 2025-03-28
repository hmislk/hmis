package com.divudi.core.entity.process;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Entity
public class ProcessInstance implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String status;

    @ManyToOne
    private ProcessDefinition processDefinition;

    @ManyToOne
    private Institution institution;

    @ManyToOne
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser creator;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(nullable = false)
    private boolean completed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser completedBy;

    @Column(nullable = false)
    private boolean cancelled = false;

    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser cancelledBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date cancelledAt;

    @Column(nullable = false)
    private boolean rejected = false;

    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser rejectedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date rejectedAt;

    @Column(nullable = false)
    private boolean paused = false;

    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser pausedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date pausedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date completedAt;

    @Column(nullable = false)
    private boolean retired = false;

    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser retirer;

    @Temporal(TemporalType.TIMESTAMP)
    private Date retiredAt;

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
        if (!(object instanceof ProcessInstance)) {
            return false;
        }
        ProcessInstance other = (ProcessInstance) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.process.ProcessInstance[ id=" + id + " ]";
    }

    public ProcessDefinition getProcessDefinition() {
        return processDefinition;
    }

    public void setProcessDefinition(ProcessDefinition processDefinition) {
        this.processDefinition = processDefinition;
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

    public WebUser getCreator() {
        return creator;
    }

    public void setCreator(WebUser creator) {
        this.creator = creator;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public WebUser getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(WebUser completedBy) {
        this.completedBy = completedBy;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
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

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public WebUser getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(WebUser cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }

    public WebUser getRejectedBy() {
        return rejectedBy;
    }

    public void setRejectedBy(WebUser rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public Date getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Date rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public WebUser getPausedBy() {
        return pausedBy;
    }

    public void setPausedBy(WebUser pausedBy) {
        this.pausedBy = pausedBy;
    }

    public Date getPausedAt() {
        return pausedAt;
    }

    public void setPausedAt(Date pausedAt) {
        this.pausedAt = pausedAt;
    }

    public String getStatus() {
        if (status == null || status.trim().equals("")) {
            status = "Ongoing";
        }
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
