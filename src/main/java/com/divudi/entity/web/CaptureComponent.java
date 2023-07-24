/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.entity.web;

import com.divudi.data.web.ComponentDataType;
import com.divudi.data.web.ComponentPresentationType;
import com.divudi.data.web.TemplateComponentType;
import com.divudi.entity.Item;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author Chamuditha Siritunga
 */
@Entity
public class CaptureComponent implements Serializable {

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
    private CaptureComponent parent;

    @Enumerated(EnumType.STRING)
    private ComponentPresentationType componentPresentationType;

    @Enumerated(EnumType.STRING)
    private ComponentDataType componentDataType;

    @ManyToOne
    private DesignComponent designComponent;

    private Double doubleValue;
    private Long longValue;
    private Integer intValue;
    private String shortTextValue;
    @Lob
    private String longTextValue;
    private Integer ratingIntValue;
    private Byte[] byteArrayValue;
    private Boolean booleanValue;

    @ManyToOne
    private Item itemValue;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date dateValue;

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
        if (!(object instanceof CaptureComponent)) {
            return false;
        }
        CaptureComponent other = (CaptureComponent) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.bean.web.CaptureComponent[ id=" + id + " ]";
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

    public CaptureComponent getParent() {
        return parent;
    }

    public void setParent(CaptureComponent parent) {
        this.parent = parent;
    }

    public DesignComponent getDesignComponent() {
        return designComponent;
    }

    public void setDesignComponent(DesignComponent designComponent) {
        this.designComponent = designComponent;
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

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public Long getLongValue() {
        return longValue;
    }

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }

    public Integer getIntValue() {
        return intValue;
    }

    public void setIntValue(Integer intValue) {
        this.intValue = intValue;
    }

    public String getShortTextValue() {
        return shortTextValue;
    }

    public void setShortTextValue(String shortTextValue) {
        this.shortTextValue = shortTextValue;
    }

    public String getLongTextValue() {
        return longTextValue;
    }

    public void setLongTextValue(String longTextValue) {
        this.longTextValue = longTextValue;
    }

    public Integer getRatingIntValue() {
        return ratingIntValue;
    }

    public void setRatingIntValue(Integer ratingIntValue) {
        this.ratingIntValue = ratingIntValue;
    }

    public Byte[] getByteArrayValue() {
        return byteArrayValue;
    }

    public void setByteArrayValue(Byte[] byteArrayValue) {
        this.byteArrayValue = byteArrayValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public Date getDateValue() {
        return dateValue;
    }

    public void setDateValue(Date dateValue) {
        this.dateValue = dateValue;
    }

    public Item getItemValue() {
        return itemValue;
    }

    public void setItemValue(Item itemValue) {
        this.itemValue = itemValue;
    }

    
    
}
