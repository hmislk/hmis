/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.clinical;

import com.divudi.data.Sex;
import com.divudi.data.clinical.FavouriteType;
import com.divudi.data.clinical.PrescriptionTemplateType;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.WebUser;
import com.divudi.entity.pharmacy.MeasurementUnit;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;

/**
 *
 * @author buddhika
 */
@Entity
public class PrescriptionTemplate implements Serializable {

    @ManyToOne
    private PrescriptionTemplate parent;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private Institution forInstitution;
    @ManyToOne
    private Department forDepartment;
    @ManyToOne
    private WebUser forWebUser;
    @ManyToOne
    private Item forItem;
    
    private Double templateFrom;
    private Double templateTo;

    
    private Double fromDays;
    private Double toDays;
    private Double fromKg;
    private Double toKg;

    @ManyToOne
    Patient patient;

    @Enumerated(EnumType.STRING)
    private PrescriptionTemplateType type;

    @Enumerated
    private FavouriteType favouriteType;
    
    @ManyToOne
    private Item item;
    List<PrescriptionTemplate> items;
    @ManyToOne
    private Category category;
    private Double dose;
    @ManyToOne
    private MeasurementUnit doseUnit;
    private Double duration;
    @ManyToOne
    private MeasurementUnit frequencyUnit;
    @ManyToOne
    private MeasurementUnit durationUnit;
    private Double issue;
    @ManyToOne
    private MeasurementUnit issueUnit;
    
    
    
    
    @Enumerated(EnumType.STRING)
    private Sex sex;
    private Long ageInMonthsFrom;
    private Long ageInMonthsTo;
    private double orderNo;

    @OneToMany(mappedBy = "parent")
    private List<PrescriptionTemplate> children;

    //Created Properties
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Retairing properties
    private boolean retired;

    public Long getId() {
        return id;
    }

    // Getter for issue field
    public Double getIssue() {
        return issue;
    }

    // Setter for issue field
    public void setIssue(Double issue) {
        this.issue = issue;
    }
    
    // Getter for issueUnit field
    public MeasurementUnit getIssueUnit() {
        return issueUnit;
    }

    // Setter for issueUnit field
    public void setIssueUnit(MeasurementUnit issueUnit) {
        this.issueUnit = issueUnit;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
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
        if (!(object instanceof PrescriptionTemplate)) {
            return false;
        }
        PrescriptionTemplate other = (PrescriptionTemplate) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.clinical.PrescriptionTemplate[ id=" + id + " ]";
    }

    
    
    public Institution getForInstitution() {
        return forInstitution;
    }

    public void setForInstitution(Institution forInstitution) {
        this.forInstitution = forInstitution;
    }

    public Department getForDepartment() {
        return forDepartment;
    }

    public void setForDepartment(Department forDepartment) {
        this.forDepartment = forDepartment;
    }

    public WebUser getForWebUser() {
        return forWebUser;
    }

    public void setForWebUser(WebUser forWebUser) {
        this.forWebUser = forWebUser;
    }
    
    public List<PrescriptionTemplate> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void setItems(List<PrescriptionTemplate> items) {
        this.items = items;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Double getDose() {
        return dose;
    }

    public void setDose(Double dose) {
        this.dose = dose;
    }

    public MeasurementUnit getDoseUnit() {
        return doseUnit;
    }

    public void setDoseUnit(MeasurementUnit doseUnit) {
        this.doseUnit = doseUnit;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public MeasurementUnit getDurationUnit() {
        return durationUnit;
    }

    public void setDurationUnit(MeasurementUnit durationUnit) {
        this.durationUnit = durationUnit;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public Long getAgeInMonthsFrom() {
        return ageInMonthsFrom;
    }

    public void setAgeInMonthsFrom(Long ageInMonthsFrom) {
        this.ageInMonthsFrom = ageInMonthsFrom;
    }

    public Long getAgeInMonthsTo() {
        return ageInMonthsTo;
    }

    public void setAgeInMonthsTo(Long ageInMonthsTo) {
        this.ageInMonthsTo = ageInMonthsTo;
    }

    public Double getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Double orderNo) {
        this.orderNo = orderNo;
    }

    public List<PrescriptionTemplate> getChildren() {
        return children;
    }

    public void setChildren(List<PrescriptionTemplate> children) {
        this.children = children;
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

    public PrescriptionTemplate getParent() {
        return parent;
    }

    public void setParent(PrescriptionTemplate parent) {
        this.parent = parent;
    }

    public Item getForItem() {
        return forItem;
    }

    public void setForItem(Item forItem) {
        this.forItem = forItem;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public PrescriptionTemplateType getType() {
        return type;
    }

    public void setType(PrescriptionTemplateType type) {
        this.type = type;
    }

    public MeasurementUnit getFrequencyUnit() {
        return frequencyUnit;
    }

    public void setFrequencyUnit(MeasurementUnit frequencyUnit) {
        this.frequencyUnit = frequencyUnit;
    }

    public Double getTemplateFrom() {
        return templateFrom;
    }

    public void setTemplateFrom(Double templateFrom) {
        this.templateFrom = templateFrom;
    }

    public Double getTemplateTo() {
        return templateTo;
    }

    public void setTemplateTo(Double templateTo) {
        this.templateTo = templateTo;
    }

    public Double getFromDays() {
        return fromDays;
    }

    public void setFromDays(Double fromDays) {
        this.fromDays = fromDays;
    }

    public Double getToDays() {
        return toDays;
    }

    public void setToDays(Double toDays) {
        this.toDays = toDays;
    }

    public Double getFromKg() {
        return fromKg;
    }

    public void setFromKg(Double fromKg) {
        this.fromKg = fromKg;
    }

    public Double getToKg() {
        return toKg;
    }

    public void setToKg(Double toKg) {
        this.toKg = toKg;
    }

    public FavouriteType getFavouriteType() {
        return favouriteType;
    }

    public void setFavouriteType(FavouriteType favouriteType) {
        this.favouriteType = favouriteType;
    }
    
    

}
