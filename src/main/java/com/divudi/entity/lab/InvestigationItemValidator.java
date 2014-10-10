/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.lab;

import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;

/**
 *
 * @author pasan
 */
@Entity
public class InvestigationItemValidator implements Serializable {
    @OneToMany(mappedBy = "investigationItemValidator")
    private List<InvestigationValidateComponent> investigationValueComponents;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    //Created Properties
    @ManyToOne
    WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
    //Edited Properties
    @ManyToOne
    private WebUser editor;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date editedAt;
    
    private String item;
    private String investigationItem;
    private String maxValue;
    private String minValue;
    
    @ManyToMany
    private List<InvestigationValidateComponent> investigationValidateComponents;
    
    
    

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

    public WebUser getEditor() {
        return editor;
    }

    public void setEditor(WebUser editor) {
        this.editor = editor;
    }

    public Date getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Date editedAt) {
        this.editedAt = editedAt;
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
        if (!(object instanceof InvestigationItemValidator)) {
            return false;
        }
        InvestigationItemValidator other = (InvestigationItemValidator) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.lab.InvestigationItemValidator[ id=" + id + " ]";
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getInvestigationItem() {
        return investigationItem;
    }

    public void setInvestigationItem(String investigationItem) {
        this.investigationItem = investigationItem;
    }

    public List<InvestigationValidateComponent> getInvestigationValidateComponents() {
        return investigationValidateComponents;
    }

    public void setInvestigationValidateComponents(List<InvestigationValidateComponent> investigationValidateComponents) {
        this.investigationValidateComponents = investigationValidateComponents;
    }

    public List<InvestigationValidateComponent> getInvestigationValueComponents() {
        return investigationValueComponents;
    }

    public void setInvestigationValueComponents(List<InvestigationValidateComponent> investigationValueComponents) {
        this.investigationValueComponents = investigationValueComponents;
    }

    public String getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(String maxValue) {
        this.maxValue = maxValue;
    }

    public String getMinValue() {
        return minValue;
    }

    public void setMinValue(String minValue) {
        this.minValue = minValue;
    }

    

    
    
}
