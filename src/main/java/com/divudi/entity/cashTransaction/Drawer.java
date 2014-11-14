/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.cashTransaction;

import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;

/**
 *
 * @author safrin
 */
@Entity
public class Drawer implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
//    @OneToMany(mappedBy = "drawer")
//    private List<WebUser> webUsers = new ArrayList<>();
    String name;
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
    private double runningBallance;
    private double creditCardBallance;
    private double slipBallance;
    private double chequeBallance;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Drawer)) {
            return false;
        }
        Drawer other = (Drawer) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.Drawer[ id=" + id + " ]";
    }

//    public List<WebUser> getWebUsers() {
//        return webUsers;
//    }
//
//    public void setWebUsers(List<WebUser> webUsers) {
//        this.webUsers = webUsers;
//    }

    public double getRunningBallance() {
        return runningBallance;
    }

    public void setRunningBallance(double runningBallance) {
        this.runningBallance = runningBallance;
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

    public double getCreditCardBallance() {
        return creditCardBallance;
    }

    public void setCreditCardBallance(double creditCardBallance) {
        this.creditCardBallance = creditCardBallance;
    }

    public double getSlipBallance() {
        return slipBallance;
    }

    public void setSlipBallance(double slipBallance) {
        this.slipBallance = slipBallance;
    }

    public double getChequeBallance() {
        return chequeBallance;
    }

    public void setChequeBallance(double chequeBallance) {
        this.chequeBallance = chequeBallance;
    }

}
