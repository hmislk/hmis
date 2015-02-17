/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.data.hr.PaysheetComponentType;
import com.divudi.entity.Institution;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author safrin
 */
@Entity
@XmlRootElement
public class PaysheetComponent implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    @Enumerated(EnumType.STRING)
    protected PaysheetComponentType componentType;
    @OneToOne
    private Staff staff;
    private double componentValue;
    private boolean includedForEpf;
    private boolean includedForEtf;
    private boolean includedForPayTax;
    private boolean includedForOt;
    private boolean includedForPh;
    private boolean includedForNoPay;
    boolean includeForAllowance;
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
    ////////////////
    @ManyToOne
    private Institution sendingInstitution;
    int orderNo;
    @Transient
    double transValue;

    public double getTransValue() {
        return transValue;
    }

    public void setTransValue(double transValue) {
        this.transValue = transValue;
    }
    
    

    public boolean isIncludeForAllowance() {
        return includeForAllowance;
    }

    public void setIncludeForAllowance(boolean includeForAllowance) {
        this.includeForAllowance = includeForAllowance;
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
        
        if (!(object instanceof PaysheetComponent)) {
            return false;
        }
        PaysheetComponent other = (PaysheetComponent) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.hr.PaysheetComponent[ id=" + id + " ]";
    }

    public PaysheetComponentType getComponentType() {
        return componentType;
    }

    public void setComponentType(PaysheetComponentType componentType) {
        this.componentType = componentType;
    }

    public double getComponentValue() {
        return componentValue;
    }

    public void setComponentValue(double componentValue) {
        this.componentValue = componentValue;
    }

    public boolean isIncludedForEpf() {
        return includedForEpf;
    }

    public void setIncludedForEpf(boolean includedForEpf) {
        this.includedForEpf = includedForEpf;
    }

    public boolean isIncludedForEtf() {
        return includedForEtf;
    }

    public void setIncludedForEtf(boolean includedForEtf) {
        this.includedForEtf = includedForEtf;
    }

    public boolean isIncludedForPayTax() {
        return includedForPayTax;
    }

    public void setIncludedForPayTax(boolean includedForPayTax) {
        this.includedForPayTax = includedForPayTax;
    }

    public boolean isIncludedForOt() {
        return includedForOt;
    }

    public void setIncludedForOt(boolean includedForOt) {
        this.includedForOt = includedForOt;
    }

    public boolean isIncludedForPh() {
        return includedForPh;
    }

    public void setIncludedForPh(boolean includedForPh) {
        this.includedForPh = includedForPh;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Institution getSendingInstitution() {
        return sendingInstitution;
    }

    public void setSendingInstitution(Institution sendingInstitution) {
        this.sendingInstitution = sendingInstitution;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public boolean isIncludedForNoPay() {
        return includedForNoPay;
    }

    public void setIncludedForNoPay(boolean includedForNoPay) {
        this.includedForNoPay = includedForNoPay;
    }
}
