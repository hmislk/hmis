package com.divudi.core.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Entity
public class ItemMapping implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Item item;

    @ManyToOne
    private Institution institution;

    @ManyToOne
    private Department department;

    @ManyToOne
    private WebUser creater;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    private boolean retired;

    @ManyToOne
    private WebUser retirer;

    @Temporal(TemporalType.TIMESTAMP)
    private Date retiredAt;

    /**
     * Distinguishes a mapping created specifically for the outside-charge
     * item filter (Add Outside Charges, Mode B / issue #23250) from any other
     * general-purpose use of this table (OPD/Optician/Collecting Centre item
     * catalogs, etc.). Existing generic institution-scoped queries filter
     * this out so an outside-charge-only mapping does not leak into
     * unrelated features that also key off {@link #institution}.
     */
    private boolean outsideChargeMapping;

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
        if (!(object instanceof ItemMapping)) {
            return false;
        }
        ItemMapping other = (ItemMapping) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.ItemMapping[ id=" + id + " ]";
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
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

    public boolean isOutsideChargeMapping() {
        return outsideChargeMapping;
    }

    public void setOutsideChargeMapping(boolean outsideChargeMapping) {
        this.outsideChargeMapping = outsideChargeMapping;
    }

}
