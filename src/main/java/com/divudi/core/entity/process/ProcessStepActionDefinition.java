package com.divudi.core.entity.process;

import com.divudi.core.data.process.ProcessStepActionType;
import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entity class for process step action definitions. Dr M H B Ariyaratne
 */
@Entity
public class ProcessStepActionDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @JoinColumn(nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private ProcessStepDefinition processStepDefinition;

    @JoinColumn(nullable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    private ProcessStepDefinition directedProcessStepDefinition;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProcessStepActionType actionType;

    private Double sequenceOrder;

    @Column(nullable = false)
    private boolean allowsMultipleActions = false; // Default to false to enforce single action unless explicitly allowed

    @ManyToOne(fetch = FetchType.LAZY)
    private ProcessStepActionDefinition parent;

    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser creator;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(nullable = false)
    private boolean retired = false;

    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser retirer;

    @Temporal(TemporalType.TIMESTAMP)
    private Date retiredAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProcessStepDefinition getProcessStepDefinition() {
        return processStepDefinition;
    }

    public void setProcessStepDefinition(ProcessStepDefinition processStepDefinition) {
        this.processStepDefinition = processStepDefinition;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Double getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(Double sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public ProcessStepActionType getActionType() {
        return actionType;
    }

    public void setActionType(ProcessStepActionType actionType) {
        this.actionType = actionType;
    }

    public ProcessStepActionDefinition getParent() {
        return parent;
    }

    public void setParent(ProcessStepActionDefinition parent) {
        this.parent = parent;
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



    // hashCode, equals, and toString methods
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProcessStepActionDefinition)) {
            return false;
        }
        ProcessStepActionDefinition other = (ProcessStepActionDefinition) object;
        if ((this.id == null && other.id != null)
                || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.workflow.ProcessStepActionDefinition[ id=" + id + " ]";
    }

    public boolean isAllowsMultipleActions() {
        return allowsMultipleActions;
    }

    public void setAllowsMultipleActions(boolean allowsMultipleActions) {
        this.allowsMultipleActions = allowsMultipleActions;
    }

    public ProcessStepDefinition getDirectedProcessStepDefinition() {
        return directedProcessStepDefinition;
    }

    public void setDirectedProcessStepDefinition(ProcessStepDefinition directedProcessStepDefinition) {
        this.directedProcessStepDefinition = directedProcessStepDefinition;
    }
}
