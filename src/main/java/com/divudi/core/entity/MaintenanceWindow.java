package com.divudi.core.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * A scheduled maintenance period announced to users through a banner (#25838).
 * The banner shows from {@code showFromAt} until {@code endAt}. Empty
 * institutions and departments mean the window affects everyone.
 */
@Entity
public class MaintenanceWindow implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String title;
    @Lob
    private String message;

    @Temporal(TemporalType.TIMESTAMP)
    private Date startAt;
    @Temporal(TemporalType.TIMESTAMP)
    private Date endAt;
    @Temporal(TemporalType.TIMESTAMP)
    private Date showFromAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "maintenancewindow_institution")
    private List<Institution> institutions = new ArrayList<>();
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "maintenancewindow_department")
    private List<Department> departments = new ArrayList<>();

    @ManyToOne
    private WebUser createdBy;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    /** Where the window came from, for example "admin page" or "api". */
    private String source;
    /** Optional reference, for example a PR number or a note. */
    private String externalReference;

    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(TemporalType.TIMESTAMP)
    private Date retiredAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getStartAt() {
        return startAt;
    }

    public void setStartAt(Date startAt) {
        this.startAt = startAt;
    }

    public Date getEndAt() {
        return endAt;
    }

    public void setEndAt(Date endAt) {
        this.endAt = endAt;
    }

    public Date getShowFromAt() {
        return showFromAt;
    }

    public void setShowFromAt(Date showFromAt) {
        this.showFromAt = showFromAt;
    }

    public List<Institution> getInstitutions() {
        if (institutions == null) {
            institutions = new ArrayList<>();
        }
        return institutions;
    }

    public void setInstitutions(List<Institution> institutions) {
        this.institutions = institutions;
    }

    public List<Department> getDepartments() {
        if (departments == null) {
            departments = new ArrayList<>();
        }
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public WebUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(WebUser createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
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

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MaintenanceWindow)) {
            return false;
        }
        MaintenanceWindow other = (MaintenanceWindow) object;
        if (this.id == null || other.id == null) {
            return this == other;
        }
        return this.id.equals(other.id);
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.MaintenanceWindow[ id=" + id + " ]";
    }
}
