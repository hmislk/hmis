package com.divudi.core.entity.clinical;

import com.divudi.core.data.clinical.CoveringLetterRecordType;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;

/**
 * A saved covering letter / invoice listing several admissions or OPD credit
 * bills, with a running reference number.
 */
@Entity
public class CoveringLetter implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String referenceNo;
    private Long serial;
    private Integer letterYear;

    @ManyToOne
    private Institution issuingInstitution;
    @ManyToOne
    private Institution creditCompany;
    @ManyToOne
    private DocumentTemplate documentTemplate;
    @Enumerated(EnumType.STRING)
    private CoveringLetterRecordType recordType;
    @Lob
    private String generatedHtml;
    private double total;
    private int rowCount;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date fromDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date toDate;

    @OneToMany(mappedBy = "coveringLetter", cascade = CascadeType.ALL)
    private List<CoveringLetterItem> items;

    //Created Properties
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Retiring properties
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;

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
        if (!(object instanceof CoveringLetter)) {
            return false;
        }
        CoveringLetter other = (CoveringLetter) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.clinical.CoveringLetter[ id=" + id + " ]";
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public Long getSerial() {
        return serial;
    }

    public void setSerial(Long serial) {
        this.serial = serial;
    }

    public Integer getLetterYear() {
        return letterYear;
    }

    public void setLetterYear(Integer letterYear) {
        this.letterYear = letterYear;
    }

    public Institution getIssuingInstitution() {
        return issuingInstitution;
    }

    public void setIssuingInstitution(Institution issuingInstitution) {
        this.issuingInstitution = issuingInstitution;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public DocumentTemplate getDocumentTemplate() {
        return documentTemplate;
    }

    public void setDocumentTemplate(DocumentTemplate documentTemplate) {
        this.documentTemplate = documentTemplate;
    }

    public CoveringLetterRecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(CoveringLetterRecordType recordType) {
        this.recordType = recordType;
    }

    public String getGeneratedHtml() {
        return generatedHtml;
    }

    public void setGeneratedHtml(String generatedHtml) {
        this.generatedHtml = generatedHtml;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<CoveringLetterItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void setItems(List<CoveringLetterItem> items) {
        this.items = items;
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

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

}
