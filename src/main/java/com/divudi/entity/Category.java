/*
 * Author
 * Dr. M H B Ariyaratne, MO(Health Information), email : buddhika.ari@gmail.com
 */
package com.divudi.entity;

import com.divudi.data.SymanticHyrachi;
import com.divudi.java.CommonFunctions;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author IT B
 *
 */
@Entity
@Inheritance
public class Category implements Serializable {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    Long id;
    //Main Properties
    String name;
    String description;
    int orderNo;
    //Created Properties
    @ManyToOne
    @JsonIgnore
    WebUser creater;
    @JsonIgnore
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
    //Retairing properties
    @JsonIgnore
    private boolean retired;
    @ManyToOne
    @JsonIgnore
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @JsonIgnore
    Date retiredAt;
    @JsonIgnore
    String retireComments;
    @JsonIgnore
    Double dblValue;
    @JsonIgnore
    Long longValue;
    @JsonIgnore
    @ManyToOne
    Category parentCategory;
    @JsonIgnore
    Double saleMargin = 0.0;
    @JsonIgnore
    Double wholeSaleMargin = 0.0;
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @JsonIgnore
    List<Item> items;
    String code;
    @OneToMany(mappedBy = "parentCategory", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    @JsonIgnore
    List<Category> childCategories;
    @Enumerated
    SymanticHyrachi symanticType;
    @Transient
    @JsonIgnore
    private String entityClass;
    @JsonIgnore
    boolean filled;
    private double profitMargin;

    @ManyToOne
    private Institution institution;

    // @ManyToOne
    //   private Department department;
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

    @XmlTransient
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

    @XmlTransient
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

}
