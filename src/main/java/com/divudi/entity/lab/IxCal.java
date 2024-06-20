package com.divudi.entity.lab;

import com.divudi.data.CalculationType;
import com.divudi.entity.WebUser;
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
 * @author Buddhika
 */
@Entity
public class IxCal implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    @ManyToOne
    private InvestigationItem calIxItem;
    @Enumerated(EnumType.STRING)
    private CalculationType calculationType;
    private Double constantValue;
    private Double maleConstantValue;
    private Double femaleConstantValue;
    @Lob
    private String javascript;
    @ManyToOne
    private InvestigationItem valIxItem;
    private Integer orderNo;
    private Boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;

    public Boolean isRetired() {
        return retired;
    }

    public void setRetired(Boolean retired) {
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

    public Double getMaleConstantValue() {
        return maleConstantValue;
    }

    public void setMaleConstantValue(Double maleConstantValue) {
        this.maleConstantValue = maleConstantValue;
    }

    public Double getFemaleConstantValue() {
        return femaleConstantValue;
    }

    public void setFemaleConstantValue(Double femaleConstantValue) {
        this.femaleConstantValue = femaleConstantValue;
    }

    public InvestigationItem getValIxItem() {
        return valIxItem;
    }

    public void setValIxItem(InvestigationItem valIxItem) {
        this.valIxItem = valIxItem;
    }

    public Double getConstantValue() {
        return constantValue;
    }

    public void setConstantValue(Double constantValue) {
        this.constantValue = constantValue;
    }

    public CalculationType getCalculationType() {
        return calculationType;
    }

    public void setCalculationType(CalculationType calculationType) {
        this.calculationType = calculationType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InvestigationItem getCalIxItem() {
        return calIxItem;
    }

    public void setCalIxItem(InvestigationItem calIxItem) {
        this.calIxItem = calIxItem;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof IxCal)) {
            return false;
        }
        IxCal other = (IxCal) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.lab.IxCal[ id=" + id + " ]";
    }

    public String getJavascript() {
        return javascript;
    }

    public void setJavascript(String javascript) {
        this.javascript = javascript;
    }
}
