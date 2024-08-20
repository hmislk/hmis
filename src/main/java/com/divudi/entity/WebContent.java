/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.entity;

import com.divudi.data.WebContentType;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

/**
 *
 * @author Buddhika
 */
@Entity
public class WebContent implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private boolean retired;
    @Lob
    private String sinhala;
    @Lob
    private String tamil;
    @Lob
    private String english;
    private double orderNo;
    @ManyToOne
    private WebLanguage webLanguage;
    private WebContentType type;
    private String shortContext;
    @Lob
    private String longContext;
    @ManyToOne
    private WebContent parent;

    public String getIdStr() {
        if (id == null) {
            return "";
        }
        return id.toString();

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
        if (!(object instanceof WebContent)) {
            return false;
        }
        WebContent other = (WebContent) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.WebContent[ id=" + id + " ]";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSinhala() {
        return sinhala;
    }

    public void setSinhala(String sinhala) {
        this.sinhala = sinhala;
    }

    public String getTamil() {
        return tamil;
    }

    public void setTamil(String tamil) {
        this.tamil = tamil;
    }

    public String getEnglish() {
        return english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public double getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(double orderNo) {
        this.orderNo = orderNo;
    }

    public WebLanguage getWebLanguage() {
        return webLanguage;
    }

    public void setWebLanguage(WebLanguage webLanguage) {
        this.webLanguage = webLanguage;
    }

    public WebContentType getType() {
        if (type == null) {
            type = WebContentType.ShortText;
        }
        return type;
    }

    public void setType(WebContentType type) {
        this.type = type;
    }

    public String getShortContext() {
        return shortContext;
    }

    public void setShortContext(String shortContext) {
        this.shortContext = shortContext;
    }

    public WebContent getParent() {
        return parent;
    }

    public void setParent(WebContent parent) {
        this.parent = parent;
    }

    public String getLongContext() {
        return longContext;
    }

    public void setLongContext(String longContext) {
        this.longContext = longContext;
    }

}
