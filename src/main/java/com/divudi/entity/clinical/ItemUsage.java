/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.clinical;

import com.divudi.data.Sex;
import com.divudi.data.clinical.ItemUsageType;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.WebUser;
import com.divudi.entity.pharmacy.MeasurementUnit;
import java.io.Serializable;
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
@Deprecated
public class ItemUsage implements Serializable {

    @ManyToOne
    private ItemUsage parent;

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

    @ManyToOne
    Patient patient;
    @ManyToOne
    PatientEncounter patientEncounter;

    @Enumerated(EnumType.STRING)
    private ItemUsageType type;

    @ManyToOne
    private Item item;
    @ManyToOne
    private Category category;
    private Double dblValue1;
    Integer intValue1;
    @ManyToOne
    private MeasurementUnit measurementUnit1;
    private Double dblValue2;
    Integer intValue2;
    @ManyToOne
    private MeasurementUnit measurementUnit2;
    @Enumerated(EnumType.STRING)
    private Sex sex;
    private Long ageInMonthsFrom;
    private Long ageInMonthsTo;
    private int orderNo;

    @OneToMany(mappedBy = "parent")
    private List<ItemUsage> children;

    //Created Properties
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Retairing properties
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;
    //Editer Properties
    @ManyToOne
    private WebUser editer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date editedAt;

    public Long getId() {
        return id;
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

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Integer getIntValue1() {
        return intValue1;
    }

    public void setIntValue1(Integer intValue1) {
        this.intValue1 = intValue1;
    }

    public Integer getIntValue2() {
        return intValue2;
    }

    public void setIntValue2(Integer intValue2) {
        this.intValue2 = intValue2;
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
        if (!(object instanceof ItemUsage)) {
            return false;
        }
        ItemUsage other = (ItemUsage) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.clinical.FavouriteItem[ id=" + id + " ]";
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

    public ItemUsageType getType() {
        return type;
    }

    public void setType(ItemUsageType type) {
        this.type = type;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Double getDblValue1() {
        return dblValue1;
    }

    public void setDblValue1(Double dblValue1) {
        this.dblValue1 = dblValue1;
    }

    public MeasurementUnit getMeasurementUnit1() {
        return measurementUnit1;
    }

    public void setMeasurementUnit1(MeasurementUnit measurementUnit1) {
        this.measurementUnit1 = measurementUnit1;
    }

    public Double getDblValue2() {
        return dblValue2;
    }

    public void setDblValue2(Double dblValue2) {
        this.dblValue2 = dblValue2;
    }

    public MeasurementUnit getMeasurementUnit2() {
        return measurementUnit2;
    }

    public void setMeasurementUnit2(MeasurementUnit measurementUnit2) {
        this.measurementUnit2 = measurementUnit2;
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

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public List<ItemUsage> getChildren() {
        return children;
    }

    public void setChildren(List<ItemUsage> children) {
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

    public WebUser getEditer() {
        return editer;
    }

    public void setEditer(WebUser editer) {
        this.editer = editer;
    }

    public Date getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Date editedAt) {
        this.editedAt = editedAt;
    }

    public ItemUsage getParent() {
        return parent;
    }

    public void setParent(ItemUsage parent) {
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

}
