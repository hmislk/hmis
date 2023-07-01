package com.divudi.entity.web;

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
 * @author Dr M H B Ariyaratne buddhika.ari@gmail.com
 */
@Entity
public class TemplateComponent implements Serializable {

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
    private TemplateComponent parent;
    
    
    
    

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
        if (!(object instanceof TemplateComponent)) {
            return false;
        }
        TemplateComponent other = (TemplateComponent) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.bean.web.TemplateComponent[ id=" + id + " ]";
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

    public TemplateComponent getParent() {
        return parent;
    }

    public void setParent(TemplateComponent parent) {
        this.parent = parent;
    }
    
}
