/*
 * Author : Dr. M H B Ariyaratne
 *
 * Acting Consultant (Health Informatics), Department of Health Services, Southern Province
 * (94) 71 5812399
 * Email : buddhika.ari@gmail.com
 */
package com.divudi.entity;

import com.divudi.data.LoginPage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.xml.bind.annotation.XmlRootElement;
import org.checkerframework.common.value.qual.EnumVal;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Entity
@XmlRootElement
public class WebUser implements Serializable {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
//    @ManyToOne(fetch = FetchType.LAZY)
//    Drawer drawer;
//
//    public Drawer getDrawer() {
//        return drawer;
//    }
//
//    public void setDrawer(Drawer drawer) {
//        this.drawer = drawer;
//    }
    @ManyToOne
    WebTheme userWebTheme;
    String webUserPassword;

    @OneToOne
    Person webUserPerson;
    //Main Properties
    @Column(unique = true, nullable = false)
    String name;
    String description;
    //Created Properties
    @ManyToOne
    @JsonIgnore
    WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @JsonIgnore
    Date createdAt;
    //Retairing properties
    @JsonIgnore
    boolean retired;
    @ManyToOne
    @JsonIgnore
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @JsonIgnore
    Date retiredAt;
    String retireComments;
    //Activation properties
    @JsonIgnore
    boolean activated;
    @JsonIgnore
    @ManyToOne
    WebUser activator;
    @JsonIgnore
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date activatedAt;
    String activateComments;
    @ManyToOne
    WebUserRole role;
    String primeTheme;
    String defLocale;
    String email;
    String telNo;
    @ManyToOne
    @JsonIgnore
    Institution institution;
    @ManyToOne
    @JsonIgnore
    Department department;
    @ManyToOne
    @JsonIgnore
    Staff staff;

    String code;
    @Enumerated(EnumType.STRING)
    private LoginPage loginPage;

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public WebUser() {
    }

    public Institution getInstitution() {
        //////// // System.out.println("Getting Institution");
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelNo() {
        return telNo;
    }

    public void setTelNo(String telNo) {
        this.telNo = telNo;
    }

    public String getDefLocale() {
        return defLocale;
    }

    public void setDefLocale(String defLocale) {
        this.defLocale = defLocale;
    }

    public String getPrimeTheme() {
        List<String> validThemes = Arrays.asList(
                "material-light-outlined",
                "material-light-filled",
                "material-dark-outlined",
                "material-dark-filled",
                "bootstrap-light-outlined",
                "bootstrap-light-filled",
                "bootstrap-dark-outlined",
                "bootstrap-dark-filled",
                "primeone-light-outlined",
                "primeone-light-filled",
                "primeone-dim-outlined",
                "primeone-dim-filled",
                "primeone-dark-outlined",
                "primeone-dark-filled",
                "saga",
                "vela",
                "arya"
        );

        
        if (validThemes.contains(primeTheme)) {
            return primeTheme;
        } else {
            return "material-light-outlined"; // Default theme
        }
    }

    public void setPrimeTheme(String primeTheme) {
        this.primeTheme = primeTheme;
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

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWebUserPassword() {
        return webUserPassword;
    }

    public void setWebUserPassword(String webUserPassword) {
        this.webUserPassword = webUserPassword;
    }

    public Person getWebUserPerson() {
        return webUserPerson;
    }

    public void setWebUserPerson(Person webUserPerson) {
        this.webUserPerson = webUserPerson;
    }

    public WebTheme getUserWebTheme() {
        return userWebTheme;
    }

    public void setUserWebTheme(WebTheme userWebTheme) {
        this.userWebTheme = userWebTheme;
    }

    public String getActivateComments() {
        return activateComments;
    }

    public void setActivateComments(String activateComments) {
        this.activateComments = activateComments;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public Date getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(Date activatedAt) {
        this.activatedAt = activatedAt;
    }

    public WebUser getActivator() {
        return activator;
    }

    public void setActivator(WebUser activator) {
        this.activator = activator;
    }

    public WebUserRole getRole() {
        return role;
    }

    public void setRole(WebUserRole role) {
        this.role = role;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof WebUser)) {
            return false;
        }
        WebUser other = (WebUser) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LoginPage getLoginPage() {
        return loginPage;
    }

    public void setLoginPage(LoginPage loginPage) {
        this.loginPage = loginPage;
    }

  

}
