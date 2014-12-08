/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.lab;

import com.divudi.data.CssFontStyle;
import com.divudi.data.CssOverflow;
import com.divudi.data.CssPosition;
import com.divudi.data.CssTextAlign;
import com.divudi.data.CssVerticalAlign;
import com.divudi.data.InvestigationItemType;
import com.divudi.data.InvestigationItemValueType;
import com.divudi.data.ReportItemType;
import com.divudi.entity.Category;
import com.divudi.entity.Item;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author Buddhika
 */
@Entity
public class ReportItem implements Serializable {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    //Main Properties
    String name;
    String tName;
    String sName;
    String description;
    int orderNo;
    //Created Properties
    @ManyToOne
    WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
    //Retairing properties
    boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    String retireComments;
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    Item item;
    @Enumerated(EnumType.STRING)
    InvestigationItemType ixItemType;
    @Enumerated(EnumType.STRING)
    InvestigationItemValueType ixItemValueType;
    //CSS Property
    @Enumerated(EnumType.STRING)
    CssPosition cssPosition;
    @Enumerated(EnumType.STRING) // I have not earlier defined it as an Enum for Persistance. // Sorry. If u run this, wi
    CssOverflow cssOverflow;
    @Enumerated(EnumType.STRING)
    CssFontStyle cssFontStyle;
    @Enumerated(EnumType.STRING)
    CssVerticalAlign cssVerticalAlign;
    @Enumerated(EnumType.STRING)
    CssTextAlign cssTextAlign;
    String cssLeft;
    String cssTop;
    String cssWidth;
    String cssHeight;
    String cssZorder;
    String cssClip;
    String cssFontFamily;
    String cssFontVariant;
    String cssFontWeight;
    String cssFontSize;
    String cssLineHeight;
    String cssBackColor;
    String cssColor;
    String cssBorderRadius;
    String cssMargin;
    String cssPadding;
    String cssBorder;
    String formatPrefix;
    String formatSuffix;
    String formatString;
    @Transient
    String cssStyle;
    @Enumerated(EnumType.STRING)
    ReportItemType reportItemType;
    @ManyToOne
    Category category;
    int pageNo;
    @ManyToOne
    private Item referringItem;
    @ManyToOne
    private Category referringCategory;

    @Transient
    double transCssTop;

    public double getTransCssTop() {
        try{
            transCssTop= Double.parseDouble(cssTop);
        }catch(Exception e){
            transCssTop = 0;
        }
        return transCssTop;
    }

    public void setTransCssTop(double transCssTop) {
        this.transCssTop = transCssTop;
    }
    
    
    
    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public ReportItemType getReportItemType() {
        return reportItemType;
    }

    public void setReportItemType(ReportItemType reportItemType) {
        this.reportItemType = reportItemType;
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

        if (!(object instanceof ReportItem)) {
            return false;
        }
        ReportItem other = (ReportItem) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.ReportItem[ id=" + id + " ]";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
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

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public InvestigationItemType getIxItemType() {
        return ixItemType;
    }

    public void setIxItemType(InvestigationItemType ixItemType) {
        this.ixItemType = ixItemType;
    }

    public InvestigationItemValueType getIxItemValueType() {
        return ixItemValueType;
    }

    public void setIxItemValueType(InvestigationItemValueType ixItemValueType) {
        this.ixItemValueType = ixItemValueType;
    }

    public CssPosition getCssPosition() {
        return cssPosition;
    }

    public void setCssPosition(CssPosition cssPosition) {
        this.cssPosition = cssPosition;
    }

    public CssOverflow getCssOverflow() {
        return cssOverflow;
    }

    public void setCssOverflow(CssOverflow cssOverflow) {
        this.cssOverflow = cssOverflow;
    }

    public String getCssLeft() {
        return cssLeft;
    }

    public void setCssLeft(String cssLeft) {
        this.cssLeft = cssLeft;
    }

    public String getCssTop() {
        if (cssTop == null || cssTop.equals("")) {
            cssTop = "10%";
        }
        return cssTop;
    }

    public void setCssTop(String cssTop) {
        this.cssTop = cssTop;
    }

    public String getCssWidth() {
        return cssWidth;
    }

    public void setCssWidth(String cssWidth) {
        this.cssWidth = cssWidth;
    }

    public String getCssHeight() {
        return cssHeight;
    }

    public void setCssHeight(String cssHeight) {
        this.cssHeight = cssHeight;
    }

    public String getCssZorder() {
        return cssZorder;
    }

    public void setCssZorder(String cssZorder) {
        this.cssZorder = cssZorder;
    }

    public String getCssClip() {
        return cssClip;
    }

    public void setCssClip(String cssClip) {
        this.cssClip = cssClip;
    }

    public String getCssFontFamily() {
        return cssFontFamily;
    }

    public void setCssFontFamily(String cssFontFamily) {
        this.cssFontFamily = cssFontFamily;
    }

    public CssFontStyle getCssFontStyle() {
        return cssFontStyle;
    }

    public void setCssFontStyle(CssFontStyle cssFontStyle) {
        this.cssFontStyle = cssFontStyle;
    }

    public String getCssFontVariant() {
        return cssFontVariant;
    }

    public void setCssFontVariant(String cssFontVariant) {
        this.cssFontVariant = cssFontVariant;
    }

    public String getCssFontWeight() {
        return cssFontWeight;
    }

    public void setCssFontWeight(String cssFontWeight) {
        this.cssFontWeight = cssFontWeight;
    }

    public String getCssFontSize() {
        return cssFontSize;
    }

    public void setCssFontSize(String cssFontSize) {
        this.cssFontSize = cssFontSize;
    }

    public String getCssLineHeight() {
        return cssLineHeight;
    }

    public void setCssLineHeight(String cssLineHeight) {
        this.cssLineHeight = cssLineHeight;
    }

    public String getCssBackColor() {
        return cssBackColor;
    }

    public void setCssBackColor(String cssBackColor) {
        this.cssBackColor = cssBackColor;
    }

    public String getCssColor() {
        return cssColor;
    }
    
    public String getInnerCss() {
        return cssColor + "; " + cssWidth + ";" ;
    }

    public void setCssColor(String cssColor) {
        this.cssColor = cssColor;
    }

    public String getCssBorderRadius() {
        return cssBorderRadius;
    }

    public void setCssBorderRadius(String cssBorderRadius) {
        this.cssBorderRadius = cssBorderRadius;
    }

    public String getCssMargin() {
        return cssMargin;
    }

    public void setCssMargin(String cssMargin) {
        this.cssMargin = cssMargin;
    }

    public String getCssPadding() {
        return cssPadding;
    }

    public void setCssPadding(String cssPadding) {
        this.cssPadding = cssPadding;
    }

    public String getCssBorder() {
        return cssBorder;
    }

    public void setCssBorder(String cssBorder) {
        this.cssBorder = cssBorder;
    }

    public CssVerticalAlign getCssVerticalAlign() {
        return cssVerticalAlign;
    }

    public void setCssVerticalAlign(CssVerticalAlign cssVerticalAlign) {
        this.cssVerticalAlign = cssVerticalAlign;
    }

    public CssTextAlign getCssTextAlign() {
        return cssTextAlign;
    }

    public void setCssTextAlign(CssTextAlign cssTextAlign) {
        this.cssTextAlign = cssTextAlign;
    }

    public String getFormatPrefix() {
        return formatPrefix;
    }

    public void setFormatPrefix(String formatPrefix) {
        this.formatPrefix = formatPrefix;
    }

    public String getFormatSuffix() {
        return formatSuffix;
    }

    public void setFormatSuffix(String formatSuffix) {
        this.formatSuffix = formatSuffix;
    }

    public String getFormatString() {
        return formatString;
    }

    public void setFormatString(String formatString) {
        this.formatString = formatString;
    }

    public String gettName() {
        return tName;
    }

    public void settName(String tName) {
        this.tName = tName;
    }

    public String getsName() {
        return sName;
    }

    public void setsName(String sName) {
        this.sName = sName;
    }

    public String getCssStyle() {
        cssStyle = "top:" + cssTop + "%; left:" + cssLeft
                + "%; width:" + cssWidth + "; height:"
                + cssHeight + "; background-color:" + cssColor + ";"
                + "font-style:" + cssFontStyle + ";font-size:"
                + cssFontSize + "%;line-height:" + cssLineHeight
                + "%;margin:" + cssMargin + "%;padding:" + cssPadding + "%;"
                + "border:" + cssBorder + "%;background-color:" + cssBackColor + ";"
                + "position:" + cssPosition + "; vertical-align: " + cssVerticalAlign + ";"
                + "text-align: " + cssTextAlign + ";z-index: " + cssZorder + ";"
                + "clip:" + cssClip + ";font-family: " + cssFontFamily + ";font-variant:" + cssFontVariant + ";"
                + "font-weight: " + cssFontWeight + ":border-radius: " + cssBorderRadius + ";";

        //TODO (Later) to add cssHeight, font styles, etc, Now 12
        return cssStyle;
    }

    public String getInnerCssStyle() {
        cssStyle = "width:" + cssWidth + "!important; height:"
                + cssHeight + "; background-color:" + cssColor + ";"
                + "font-style:" + cssFontStyle + ";font-size:"
                + cssFontSize + "%;line-height:" + cssLineHeight
                + "%; vertical-align: " + cssVerticalAlign + ";"
                + "text-align: " + cssTextAlign + ";z-index: " + cssZorder + ";"
                + "clip:" + cssClip + ";font-family: " + cssFontFamily + ";font-variant:" + cssFontVariant + ";"
                + "font-weight: " + cssFontWeight + ";";

        //TODO (Later) to add cssHeight, font styles, etc, Now 12
        return cssStyle;
    }

    
    public void setCssStyle(String cssStyle) {
        this.cssStyle = cssStyle;
    }

    public Item getReferringItem() {
        return referringItem;
    }

    public void setReferringItem(Item referringItem) {
        this.referringItem = referringItem;
    }

    public Category getReferringCategory() {
        return referringCategory;
    }

    public void setReferringCategory(Category referringCategory) {
        this.referringCategory = referringCategory;
    }
}
