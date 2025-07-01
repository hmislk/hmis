package com.divudi.core.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import com.divudi.core.data.HistoricalRecordType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Size;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Buddhika & OpenAI ChatGPT
 */
@Entity
public class HistoricalRecord implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * @deprecated use {@link #historicalRecordType} instead.
     */
    @Deprecated
    @Size(max = 255)
    private String variableName;

    @Enumerated(EnumType.STRING)
    private HistoricalRecordType historicalRecordType;

    @NotNull
    private Double recordValue;

    @ManyToOne
    private Institution institution;

    @ManyToOne
    private Institution site;

    @ManyToOne
    private Department department;

    @ManyToOne
    private WebUser webUser;

    @ManyToOne
    private Institution collectionCentre;

    @Temporal(TemporalType.DATE)
    private Date recordDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date recordDateTime;

    private Boolean retired = false;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @ManyToOne
    private WebUser createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date retiredAt;

    @ManyToOne
    private WebUser retiredBy;

    
    
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
        if (!(object instanceof HistoricalRecord)) {
            return false;
        }
        HistoricalRecord other = (HistoricalRecord) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.HistoricalRecord[ id=" + id + " ]";
    }

    /**
     * @deprecated use {@link #historicalRecordType}
     */
    @Deprecated
    public String getVariableName() {
        return variableName;
    }

    /**
     * @deprecated use {@link #historicalRecordType}
     */
    @Deprecated
    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public HistoricalRecordType getHistoricalRecordType() {
        return historicalRecordType;
    }

    public void setHistoricalRecordType(HistoricalRecordType historicalRecordType) {
        this.historicalRecordType = historicalRecordType;
    }

    public Double getRecordValue() {
        return recordValue;
    }

    public void setRecordValue(Double recordValue) {
        this.recordValue = recordValue;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public Institution getCollectionCentre() {
        return collectionCentre;
    }

    public void setCollectionCentre(Institution collectionCentre) {
        this.collectionCentre = collectionCentre;
    }

    public Date getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(Date recordDate) {
        this.recordDate = recordDate;
    }

    public Boolean getRetired() {
        return retired;
    }

    public void setRetired(Boolean retired) {
        this.retired = retired;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public WebUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(WebUser createdBy) {
        this.createdBy = createdBy;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public WebUser getRetiredBy() {
        return retiredBy;
    }

    public void setRetiredBy(WebUser retiredBy) {
        this.retiredBy = retiredBy;
    }

    public Date getRecordDateTime() {
        return recordDateTime;
    }

    public void setRecordDateTime(Date recordDateTime) {
        this.recordDateTime = recordDateTime;
    }

}
