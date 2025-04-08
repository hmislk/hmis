package com.divudi.core.entity.process;

import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author ASUS
 */
@Entity
public class ProcessStepInstance implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private ProcessInstance processInstance; // Link to the specific process instance

    @ManyToOne
    @JoinColumn(nullable = false)
    private ProcessStepDefinition processStepDefinition; // The step definition this instance is based on

    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser creator; // The user who initiated this step

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt; // The timestamp when this step was initiated

    @Column(nullable = false)
    private boolean completed = false; // Flag to indicate if the step is completed

    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser completedBy; // User who completed this step

    @Temporal(TemporalType.TIMESTAMP)
    private Date completedAt; // When the step was completed

    @Column(nullable = false)
    private boolean retired = false; // Flag to indicate if the step instance is retired

    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser retirer; // User who retired this step instance

    @Temporal(TemporalType.TIMESTAMP)
    private Date retiredAt; // When the step was retired

    @ManyToOne(fetch = FetchType.LAZY)
    private ProcessStepInstance precedingStepInstance; // Link to the preceding step instance

    @ManyToOne(fetch = FetchType.LAZY)
    private ProcessStepInstance nextStepInstance; // Link to the subsequent step instance

    @ManyToOne
    private ProcessStepActionDefinition processStepActionDefinition;



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
        if (!(object instanceof ProcessStepInstance)) {
            return false;
        }
        ProcessStepInstance other = (ProcessStepInstance) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.process.ProcessStepInstance[ id=" + id + " ]";
    }

    public ProcessInstance getProcessInstance() {
        return processInstance;
    }

    public void setProcessInstance(ProcessInstance processInstance) {
        this.processInstance = processInstance;
    }

    public ProcessStepDefinition getProcessStepDefinition() {
        return processStepDefinition;
    }

    public void setProcessStepDefinition(ProcessStepDefinition processStepDefinition) {
        this.processStepDefinition = processStepDefinition;
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

    public ProcessStepInstance getPrecedingStepInstance() {
        return precedingStepInstance;
    }

    public void setPrecedingStepInstance(ProcessStepInstance precedingStepInstance) {
        this.precedingStepInstance = precedingStepInstance;
    }

    public ProcessStepInstance getNextStepInstance() {
        return nextStepInstance;
    }

    public void setNextStepInstance(ProcessStepInstance nextStepInstance) {
        this.nextStepInstance = nextStepInstance;
    }

    public ProcessStepActionDefinition getProcessStepActionDefinition() {
        return processStepActionDefinition;
    }

    public void setProcessStepActionDefinition(ProcessStepActionDefinition processStepActionDefinition) {
        this.processStepActionDefinition = processStepActionDefinition;
    }

    public String getStatus() {
        if(status==null||status.trim().equals("")){
            status="Pending";
        }
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }



}
