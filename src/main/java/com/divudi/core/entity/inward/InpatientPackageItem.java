package com.divudi.core.entity.inward;

import com.divudi.core.data.inward.InpatientPackageComponentType;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.RetirableEntity;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class InpatientPackageItem implements Serializable, RetirableEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private InpatientPackage inpatientPackage;

    @Enumerated(EnumType.STRING)
    private InpatientPackageComponentType componentType;

    @ManyToOne
    private Item item;

    @ManyToOne
    private Speciality speciality;

    private String roleLabel;

    private Double qty = 1.0;

    private Double fixedPrice = 0.0;

    @ManyToOne
    private WebUser creater;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InpatientPackage getInpatientPackage() {
        return inpatientPackage;
    }

    public void setInpatientPackage(InpatientPackage inpatientPackage) {
        this.inpatientPackage = inpatientPackage;
    }

    public InpatientPackageComponentType getComponentType() {
        return componentType;
    }

    public void setComponentType(InpatientPackageComponentType componentType) {
        this.componentType = componentType;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public String getRoleLabel() {
        return roleLabel;
    }

    public void setRoleLabel(String roleLabel) {
        this.roleLabel = roleLabel;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getFixedPrice() {
        return fixedPrice;
    }

    public void setFixedPrice(Double fixedPrice) {
        this.fixedPrice = fixedPrice;
    }

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

    @Override
    public boolean isRetired() {
        return retired;
    }

    @Override
    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    @Override
    public WebUser getRetirer() {
        return retirer;
    }

    @Override
    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    @Override
    public Date getRetiredAt() {
        return retiredAt;
    }

    @Override
    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    @Override
    public String getRetireComments() {
        return retireComments;
    }

    @Override
    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof InpatientPackageItem)) {
            return false;
        }
        InpatientPackageItem other = (InpatientPackageItem) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.inward.InpatientPackageItem[ id=" + id + " ]";
    }
}
