/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.inward;

import com.divudi.core.data.inward.AdmissionTypeEnum;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
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

/**
 * Stores the last-used admission (BHT) number for a given admission type and
 * optionally institution. Mirrors the BillNumber pattern used for bill number
 * generation, ensuring race-condition-free sequential numbering.
 *
 * @author Dr. M H B Ariyaratne
 */
@Entity
public class AdmissionNumber implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long lastAdmissionNumber;
    @ManyToOne
    private AdmissionType admissionType;
    @Enumerated(EnumType.STRING)
    private AdmissionTypeEnum admissionTypeEnum;
    @ManyToOne
    private Institution institution;
    //Retiring properties
    boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    private String retireComments;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLastAdmissionNumber() {
        return lastAdmissionNumber;
    }

    public void setLastAdmissionNumber(Long lastAdmissionNumber) {
        this.lastAdmissionNumber = lastAdmissionNumber;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public AdmissionTypeEnum getAdmissionTypeEnum() {
        return admissionTypeEnum;
    }

    public void setAdmissionTypeEnum(AdmissionTypeEnum admissionTypeEnum) {
        this.admissionTypeEnum = admissionTypeEnum;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
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

    public String getRetireComments() {
        return retireComments;
    }

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
        if (!(object instanceof AdmissionNumber)) {
            return false;
        }
        AdmissionNumber other = (AdmissionNumber) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.inward.AdmissionNumber[ id=" + id + " ]";
    }
}
