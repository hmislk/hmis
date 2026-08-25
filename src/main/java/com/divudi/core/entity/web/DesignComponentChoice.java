package com.divudi.core.entity.web;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * One selectable option belonging to a choice-based DesignComponent field
 * (SelectOneMenu, SelectOneRadio, SelectManyButton, etc.).
 * label is shown to the user; value is stored in CaptureComponent.shortTextValue
 * (single-select) or appended to CaptureComponent.longTextValue as CSV (multi-select).
 */
@Entity
public class DesignComponentChoice implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private DesignComponent designComponent;

    private String label;
    private String value;
    private Integer orderNo;
    private boolean retired;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DesignComponent getDesignComponent() {
        return designComponent;
    }

    public void setDesignComponent(DesignComponent designComponent) {
        this.designComponent = designComponent;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DesignComponentChoice)) {
            return false;
        }
        DesignComponentChoice other = (DesignComponentChoice) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return label;
    }
}
