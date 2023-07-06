package com.divudi.entity.web;

import com.divudi.data.web.ComponentDataType;
import com.divudi.data.web.ComponentPresentationType;
import com.divudi.data.web.TemplateComponentType;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

/**
 *
 * @author Senula Nanayakkara
 */
@Entity
public class DesignComponent implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    private String name;
    @Lob
    private String description;
    @Enumerated(EnumType.STRING)
    private TemplateComponentType type;
    @ManyToOne
    private DesignComponent parent;
    
    @ManyToOne
    private DesignComponent dataEntryForm;
    
    @Enumerated(EnumType.STRING)
    private ComponentPresentationType componentPresentationType;
    
    @Enumerated(EnumType.STRING)
    private ComponentDataType componentDataType;
    

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
        if (!(object instanceof DesignComponent)) {
            return false;
        }
        DesignComponent other = (DesignComponent) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.bean.web.DesignComponent[ id=" + id + " ]";
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

    public TemplateComponentType getType() {
        return type;
    }

    public void setType(TemplateComponentType type) {
        this.type = type;
    }

    public DesignComponent getParent() {
        return parent;
    }

    public void setParent(DesignComponent parent) {
        this.parent = parent;
    }

    public ComponentPresentationType getComponentPresentationType() {
        return componentPresentationType;
    }

    public void setComponentPresentationType(ComponentPresentationType componentPresentationType) {
        this.componentPresentationType = componentPresentationType;
    }

    public ComponentDataType getComponentDataType() {
        return componentDataType;
    }

    public void setComponentDataType(ComponentDataType componentDataType) {
        this.componentDataType = componentDataType;
    }

    public DesignComponent getDataEntryForm() {
        return dataEntryForm;
    }

    public void setDataEntryForm(DesignComponent dataEntryForm) {
        this.dataEntryForm = dataEntryForm;
    }
    
}
