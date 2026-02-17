package com.divudi.core.entity.web;

import com.divudi.core.entity.process.ProcessDefinition;
import com.divudi.core.entity.process.ProcessStepActionDefinition;
import com.divudi.core.entity.process.ProcessStepDefinition;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 *
 * @author Dr M H Buddhika Ariyaratne
 */
@Entity
public class DesignComponentAssignment implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    private DesignComponent designComponent;
    private String assignedEntityType;
    @ManyToOne
    private ProcessDefinition processDefinition;
    @ManyToOne
    private ProcessStepDefinition processStepDefinition;
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
        if (!(object instanceof DesignComponentAssignment)) {
            return false;
        }
        DesignComponentAssignment other = (DesignComponentAssignment) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.web.DesignComponentAssignment[ id=" + id + " ]";
    }

    public ProcessDefinition getProcessDefinition() {
        return processDefinition;
    }

    public void setProcessDefinition(ProcessDefinition processDefinition) {
        this.processDefinition = processDefinition;
    }

    public ProcessStepDefinition getProcessStepDefinition() {
        return processStepDefinition;
    }

    public void setProcessStepDefinition(ProcessStepDefinition processStepDefinition) {
        this.processStepDefinition = processStepDefinition;
    }

    public ProcessStepActionDefinition getProcessStepActionDefinition() {
        return processStepActionDefinition;
    }

    public void setProcessStepActionDefinition(ProcessStepActionDefinition processStepActionDefinition) {
        this.processStepActionDefinition = processStepActionDefinition;
    }

    public DesignComponent getDesignComponent() {
        return designComponent;
    }

    public void setDesignComponent(DesignComponent designComponent) {
        this.designComponent = designComponent;
    }

    public String getAssignedEntityType() {
        return assignedEntityType;
    }

    public void setAssignedEntityType(String assignedEntityType) {
        this.assignedEntityType = assignedEntityType;
    }

}
