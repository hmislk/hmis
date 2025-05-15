/*
 * Author
 * Dr. M H B Ariyaratne, MO(Health Information), email : buddhika.ari@gmail.com
 */
package com.divudi.core.entity;


import com.divudi.core.data.CategoryType;
import com.divudi.core.data.SymanticHyrachi;
import com.divudi.core.entity.hr.Designation;
import com.divudi.core.entity.hr.Grade;
import com.divudi.core.entity.hr.StaffCategory;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.Room;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.entity.inward.TimedItemCategory;
import com.divudi.core.entity.lab.*;
import com.divudi.core.entity.membership.MembershipScheme;
import com.divudi.core.entity.pharmacy.*;
import com.divudi.core.util.CommonFunctions;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * @author IT B
 */
@Entity
@Inheritance
public class Category implements Serializable {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    //Main Properties
    String name;
    String description;
    int orderNo;
    @Enumerated(EnumType.STRING)
    private CategoryType categoryType;
    //Created Properties
    @ManyToOne
    WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
    //Retairing properties

    private boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    String retireComments;

    Double dblValue;
    Long longValue;

    @ManyToOne
    Category parentCategory;
    Double saleMargin = 0.0;
    Double wholeSaleMargin = 0.0;
    private Double pointesForThousand;
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    List<Item> items;
    String code;
    @OneToMany(mappedBy = "parentCategory", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    List<Category> childCategories;
    @Enumerated
    SymanticHyrachi symanticType;

    @Transient
    private String entityClass;

    boolean filled;
    private double profitMargin;
    @ManyToOne
    private PaymentScheme paymentScheme;
    @ManyToOne
    private Institution institution;

    public boolean isFilled() {
        return filled;
    }

    public String getIdStr() {
        if (this.id == null) {
            return null;
        }
        return id + "";
    }

    public void setFilled(boolean filled) {
        this.filled = filled;
    }

    public SymanticHyrachi getSymanticType() {
        return symanticType;
    }

    public void setSymanticType(SymanticHyrachi symanticType) {
        this.symanticType = symanticType;
    }

    public List<Category> getChildCategories() {
        return childCategories;
    }

    public void setChildCategories(List<Category> childCategories) {
        this.childCategories = childCategories;
    }

    public String getCode() {
        if (code == null || code.trim().equals("")) {
            code = CommonFunctions.nameToCode(name);
        }
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Category() {
    }

    public Double getSaleMargin() {
        return saleMargin;
    }

    public void setSaleMargin(Double saleMargin) {
        this.saleMargin = saleMargin;
    }

    public Double getWholeSaleMargin() {
        return wholeSaleMargin;
    }

    public void setWholeSaleMargin(Double wholeSaleMargin) {
        this.wholeSaleMargin = wholeSaleMargin;
    }

    public Double getDblValue() {
        return dblValue;
    }

    public void setDblValue(Double dblValue) {
        this.dblValue = dblValue;
    }

    public Long getLongValue() {
        return longValue;
    }

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }

    public Category getParentCategory() {
        return parentCategory;
    }

    public void setParentCategory(Category parentCategory) {
        this.parentCategory = parentCategory;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public WebUser getCreater() {
        return creater;
    }

    public void setCreater(WebUser creater) {
        this.creater = creater;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public WebUser getRetirer() {
        return retirer;
    }

    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

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
        if (!(object instanceof Category)) {
            return false;
        }
        Category other = (Category) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public String getEntityClass() {
        entityClass = this.getClass().toString();
        return entityClass;
    }

    public void setEntityClass(String entityClass) {
        this.entityClass = entityClass;
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

    public double getProfitMargin() {
        return profitMargin;
    }

    public void setProfitMargin(double profitMargin) {
        this.profitMargin = profitMargin;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public Double getPointesForThousand() {
        return pointesForThousand;
    }

    public void setPointesForThousand(Double pointesForThousand) {
        this.pointesForThousand = pointesForThousand;
    }


    public CategoryType getCategoryType() {
        return this.categoryType;
    }

    public void setCategoryType(CategoryType categoryType) {
        this.categoryType = categoryType;
    }

    @PrePersist
    @PreUpdate
    public void updateCategoryType() {
        if (this.categoryType == null) {
            if (this instanceof ServiceCategory) {
                this.categoryType = CategoryType.SERVICE_CATEGORY;
            } else if (this instanceof ServiceSubCategory) {
                this.categoryType = CategoryType.SERVICE_SUB_CATEGORY;
            } else if (this instanceof InvestigationCategory) {
                this.categoryType = CategoryType.INVESTIGATION_CATEGORY;
            } else if (this instanceof InvestigationTube) {
                this.categoryType = CategoryType.INVESTIGATION_TUBE;
            } else if (this instanceof RouteOfAdministration) {
                this.categoryType = CategoryType.ROUTE_OF_ADMINISTRATION;
            } else if (this instanceof Make) {
                this.categoryType = CategoryType.MAKE;
            } else if (this instanceof DosageForm) {
                this.categoryType = CategoryType.DOSAGE_FORM;
            } else if (this instanceof PharmaceuticalCategory) {
                this.categoryType = CategoryType.PHARMACEUTICAL_CATEGORY;
            } else if (this instanceof PharmaceuticalItemCategory) {
                this.categoryType = CategoryType.PHARMACEUTICAL_ITEM_CATEGORY;
            } else if (this instanceof PharmaceuticalItemType) {
                this.categoryType = CategoryType.PHARMACEUTICAL_ITEM_TYPE;
            } else if (this instanceof StoreItemCategory) {
                this.categoryType = CategoryType.STORE_ITEM_CATEGORY;
            } else if (this instanceof MeasurementUnit) {
                this.categoryType = CategoryType.MEASUREMENT_UNIT;
            } else if (this instanceof FrequencyUnit) {
                this.categoryType = CategoryType.FREQUENCY_UNIT;
            } else if (this instanceof AdjustmentCategory) {
                this.categoryType = CategoryType.ADJUSTMENT_CATEGORY;
            } else if (this instanceof DiscardCategory) {
                this.categoryType = CategoryType.DISCARD_CATEGORY;
            } else if (this instanceof Designation) {
                this.categoryType = CategoryType.DESIGNATION;
            } else if (this instanceof Grade) {
                this.categoryType = CategoryType.GRADE;
            } else if (this instanceof StaffCategory) {
                this.categoryType = CategoryType.STAFF_CATEGORY;
            } else if (this instanceof AdmissionType) {
                this.categoryType = CategoryType.ADMISSION_TYPE;
            } else if (this instanceof RoomCategory) {
                this.categoryType = CategoryType.ROOM_CATEGORY;
            } else if (this instanceof Room) {
                this.categoryType = CategoryType.ROOM;
            } else if (this instanceof TimedItemCategory) {
                this.categoryType = CategoryType.TIMED_ITEM_CATEGORY;
            } else if (this instanceof ReportFormat) {
                this.categoryType = CategoryType.REPORT_FORMAT;
            } else if (this instanceof WorksheetFormat) {
                this.categoryType = CategoryType.WORKSHEET_FORMAT;
            } else if (this instanceof Sample) {
                this.categoryType = CategoryType.SAMPLE;
            } else if (this instanceof InvestigationItemValueCategory) {
                this.categoryType = CategoryType.INVESTIGATION_ITEM_VALUE_CATEGORY;
            } else if (this instanceof Machine) {
                this.categoryType = CategoryType.MACHINE;
            } else if (this instanceof MembershipScheme) {
                this.categoryType = CategoryType.MEMBERSHIP_SCHEME;
            } else if (this instanceof FormFormat) {
                this.categoryType = CategoryType.FORM_FORMAT;
            } else if (this instanceof InventoryCategory) {
                this.categoryType = CategoryType.INVENTORY_CATEGORY;
            } else if (this instanceof MetadataCategory) {
                this.categoryType = CategoryType.METADATA_CATEGORY;
            } else if (this instanceof MetadataSuperCategory) {
                this.categoryType = CategoryType.METADATA_SUPER_CATEGORY;
            } else if (this instanceof Nationality) {
                this.categoryType = CategoryType.NATIONALITY;
            } else if (this instanceof Religion) {
                this.categoryType = CategoryType.RELIGION;
            } else if (this instanceof Relation) {
                this.categoryType = CategoryType.RELATION;
            } else if (this instanceof Vocabulary) {
                this.categoryType = CategoryType.VOCABULARY;
            } else if (this instanceof Speciality) {
                this.categoryType = CategoryType.SPECIALITY;
            } else if (this instanceof SessionNumberGenerator) {
                this.categoryType = CategoryType.SESSION_NUMBER_GENERATOR;
            }
        }
    }

}
