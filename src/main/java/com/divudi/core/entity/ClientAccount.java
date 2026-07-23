package com.divudi.core.entity;

import com.divudi.core.data.ClientAccountCreationChannel;
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
import javax.persistence.TemporalType;

@Entity
public class ClientAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Person person;

    private String passwordHash;

    private String verifiedPhone;
    private String verifiedEmail;

    private boolean phoneVerified;
    private boolean emailVerified;

    @Enumerated(EnumType.STRING)
    private ClientAccountCreationChannel createdVia;

    @ManyToOne
    private WebUser createdByWebUser;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    private boolean retired;

    @ManyToOne
    private WebUser retirer;

    @Temporal(TemporalType.TIMESTAMP)
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
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ClientAccount)) {
            return false;
        }
        ClientAccount other = (ClientAccount) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.ClientAccount[ id=" + id + " ]";
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getVerifiedPhone() {
        return verifiedPhone;
    }

    public void setVerifiedPhone(String verifiedPhone) {
        this.verifiedPhone = verifiedPhone;
    }

    public String getVerifiedEmail() {
        return verifiedEmail;
    }

    public void setVerifiedEmail(String verifiedEmail) {
        this.verifiedEmail = verifiedEmail;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public void setPhoneVerified(boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public ClientAccountCreationChannel getCreatedVia() {
        return createdVia;
    }

    public void setCreatedVia(ClientAccountCreationChannel createdVia) {
        this.createdVia = createdVia;
    }

    public WebUser getCreatedByWebUser() {
        return createdByWebUser;
    }

    public void setCreatedByWebUser(WebUser createdByWebUser) {
        this.createdByWebUser = createdByWebUser;
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
