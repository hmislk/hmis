/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.lab;

import com.divudi.data.CssFontStyle;
import com.divudi.data.CssOverflow;
import com.divudi.data.CssPosition;
import com.divudi.data.CssTextAlign;
import com.divudi.data.CssTextDecoration;
import com.divudi.data.CssVerticalAlign;
import com.divudi.data.InvestigationItemType;
import com.divudi.data.InvestigationItemValueType;
import com.divudi.data.ReportItemType;
import com.divudi.data.lab.DataEntryMethod;
import com.divudi.entity.Category;
import com.divudi.entity.Item;
import com.divudi.entity.WebUser;
import com.divudi.java.CommonFunctions;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import javax.persistence.Inheritance;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author Buddhika
 */
@Entity
@Inheritance
public class ReportItem implements Serializable {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    Long id;
    //Main Properties
    String name;
    String code;
    @Lob
    String description;
    int orderNo;
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
    @JsonIgnore
    String retireComments;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnore
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
    CssTextDecoration cssTextDecoration;
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
    @JsonIgnore
    Category category;
    int pageNo;
    @ManyToOne
    @JsonIgnore
    private Item referringItem;
    @ManyToOne
    @JsonIgnore
    private Category referringCategory;

    double riTop;
    double riHeight;
    double riLeft;
    double riWidth;
    double riFontSize;

    double htPix;
    double wtPix;

    @Lob
    String htmltext;

    @OneToOne
    @JsonIgnore
    private ReportItem testHeader;
    @OneToOne
    @JsonIgnore
    private ReportItem valueHeader;
    @OneToOne
    @JsonIgnore
    private ReportItem unitHeader;
    @OneToOne
    @JsonIgnore
    private ReportItem referenceHeader;
    @OneToOne
    @JsonIgnore
    private ReportItem testLabel;
    @ManyToOne
    @JsonIgnore
    private ReportItem valueValue;
    @ManyToOne
    @JsonIgnore
    private ReportItem flagValue;
    @OneToOne
    @JsonIgnore
    private ReportItem unitLabel;
    @OneToOne
    @JsonIgnore
    private ReportItem referenceLabel;
    @ManyToOne
    @JsonIgnore
    private ReportItem commentLabel;
    @ManyToOne
    @JsonIgnore
    private InvestigationComponent investigationComponent;
    @Enumerated(EnumType.STRING)
    private DataEntryMethod dataEntryMethod;

    private boolean automated;
    @ManyToOne
    private Machine machine;
    @ManyToOne
    private Item test;
    @ManyToOne
    private Sample sample;
    @ManyToOne
    private Item sampleComponent;
    @ManyToOne
    private InvestigationTube tube;
    private String resultCode;

    private boolean canNotApproveIfValueIsEmpty;
    private boolean canNotApproveIfValueIsBelowAbsoluteLowValue;
    private boolean canNotApproveIfValueIsAboveAbsoluteHighValue;

    private String emptyValueWarning;
    private String belowAbsoluteWarning;
    private String aboveAbsoluteWarning;

    private double absoluteLowValue;
    private double absoluteHighValue;

    public CssTextDecoration getCssTextDecoration() {
        return cssTextDecoration;
    }

    public void setCssTextDecoration(CssTextDecoration cssTextDecoration) {
        this.cssTextDecoration = cssTextDecoration;
    }

    public ReportItem getUnitLabel() {
        return unitLabel;
    }

    public void setUnitLabel(ReportItem unitLabel) {
        this.unitLabel = unitLabel;
    }

    public ReportItem getReferenceLabel() {
        return referenceLabel;
    }

    public void setReferenceLabel(ReportItem referenceLabel) {
        this.referenceLabel = referenceLabel;
    }

    public ReportItem getCommentLabel() {
        return commentLabel;
    }

    /**
     *
     *
     *
     * InvestigationItem testHeader = new InvestigationItem(); InvestigationItem
     * valueHeader = new InvestigationItem(); InvestigationItem unitHeader = new
     * InvestigationItem(); InvestigationItem referenceHeader = new
     * InvestigationItem(); InvestigationItem testLabel = new
     * InvestigationItem(); InvestigationItem valueValue = new
     * InvestigationItem(); InvestigationItem unitValue = new
     * InvestigationItem(); InvestigationItem referenceHeader = new
     * InvestigationItem(); InvestigationItem testComments = new
     * InvestigationItem();
     *
     *
     *
     *
     *
     */
    public void setCommentLabel(ReportItem commentLabel) {
        this.commentLabel = commentLabel;
    }

    public String getHtmltext() {
        if (htmltext == null) {
            htmltext = "";
        }
        return htmltext;
    }

    public void setHtmltext(String htmltext) {
        this.htmltext = htmltext;
    }

    public double getHtPix() {
        return htPix;
    }

    public void setHtPix(double htPix) {
        this.htPix = htPix;
    }

    public double getWtPix() {
        return wtPix;
    }

    public void setWtPix(double wtPix) {
        this.wtPix = wtPix;
    }

    public String removeLastPercentage(String inputString) {
        if (inputString == null) {
            return null;
        }
        if (inputString.contains("%")) {
            int len = inputString.length();
            return inputString.substring(0, len - 1);
        }
        return inputString;
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
        if (ixItemType == null) {
            ixItemType = InvestigationItemType.Label;
        }
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
        if (cssFontStyle == null) {
            cssFontStyle = CssFontStyle.Normal;
        }
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
        if (cssTextAlign == null) {
            cssTextAlign = CssTextAlign.Left;
        }
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

    public String getCode() {
        if (code == null || code.trim().equals("")) {
            code = CommonFunctions.nameToCode(name);
        }
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

//    public String getCssStyle() {
//        cssStyle = "top:" + getRiTop() + "%; left:" + getRiLeft()
//                + "%; width:" + getRiWidth() + "; height:"
//                + getRiHeight() + "; background-color:" + cssColor + ";"
//                + "font-style:" + cssFontStyle + ";font-size:"
//                + cssFontSize + "%;line-height:" + cssLineHeight
//                + "%;margin:" + cssMargin + "%;padding:" + cssPadding + "%;"
//                + "border:" + cssBorder + "%;background-color:" + cssBackColor + ";"
//                + "position:" + cssPosition + "; vertical-align: " + cssVerticalAlign + ";"
//                + "text-align: " + cssTextAlign + ";z-index: " + cssZorder + ";"
//                + "clip:" + cssClip + ";font-family: " + cssFontFamily + ";font-variant:" + cssFontVariant + ";"
//                + "font-weight: " + cssFontWeight + ":border-radius: " + cssBorderRadius + ";";
//
//        //TODO (Later) to add cssHeight, font styles, etc, Now 12
//        return cssStyle;
//    }
    public String getCssStyle() {
        cssStyle = "";
        if (getRiTop() != 0) {
            cssStyle += "top:" + getRiTop() + "%;";
        }
        if (getRiLeft() != 0) {
            cssStyle += "left:" + getRiLeft() + "%;";
        }
        if (getRiWidth() != 0) {
            cssStyle += "width:" + getRiWidth() + "%;";
        }
        if (getRiHeight() != 0) {
            cssStyle += "height:" + getRiHeight() + "%;";
        }
        if (getRiFontSize() != 0) {
            cssStyle += "font-size:" + getRiFontSize() + "pt;";
        }

        switch (getCssTextAlign()) {
            case Center:
                cssStyle += "text-align:center;";
                break;
            case Justify:
                cssStyle += "text-align:justify;";
                break;
            case Left:
                cssStyle += "text-align:left;";
                break;
            case Right:
                cssStyle += "text-align:right;";
                break;
            default:
        }

        switch (getCssFontStyle()) {
            case Normal:
                cssStyle += "font-style:normal;";
                break;
            case Italic:
                cssStyle += "font-style:italic;";
                break;
            case Oblique:
                cssStyle += "font-style:oblique;";
                break;
            default:
        }

        if (cssFontFamily != null && !cssFontFamily.equals("")) {
            cssStyle += "font-family:" + cssFontFamily + "; ";
        }

        if (cssFontWeight != null && !cssFontWeight.equals("")) {
            cssStyle += "font-weight:" + getCssFontWeight() + "; ";
        }

        cssStyle += "position:static; ";
        if (cssVerticalAlign != null) {
            cssStyle += "vertical-align: " + cssVerticalAlign + "; ";
        }

//        cssStyle = "top:" + getRiTop() + "%; left:" + getRiLeft()
//                + "%; width:" + getRiWidth() + "; height:"
//                + getRiHeight() + "; background-color:" + cssColor + ";"
//                + "font-style:" + cssFontStyle + ";font-size:"
//                + cssFontSize + "%;line-height:" + cssLineHeight
//                + "%;margin:" + cssMargin + "%;padding:" + cssPadding + "%;"
//                + "border:" + cssBorder + "%;background-color:" + cssBackColor + ";"
//                + "position:" + cssPosition + "; vertical-align: " + cssVerticalAlign + ";"
//                + "text-align: " + cssTextAlign + ";z-index: " + cssZorder + ";"
//                + "clip:" + cssClip + ";font-family: " + cssFontFamily + ";font-variant:" + cssFontVariant + ";"
//                + "font-weight: " + cssFontWeight + ":border-radius: " + cssBorderRadius + ";";
        //TODO (Later) to add cssHeight, font styles, etc, Now 12
//        //// // System.out.println("cssStyle = " + cssStyle);
        return cssStyle;
    }

    @JsonIgnore
    public String getInnerCssStyle() {
        cssStyle = "";
        if (getRiFontSize() != 0) {
            cssStyle += "font-size:" + getRiFontSize() + "pt;";
        }

        switch (getCssTextAlign()) {
            case Center:
                cssStyle += "text-align:center;";
                break;
            case Justify:
                cssStyle += "text-align:justify;";
                break;
            case Left:
                cssStyle += "text-align:left;";
                break;
            case Right:
                cssStyle += "text-align:right;";
                break;
            default:
        }

        switch (getCssFontStyle()) {
            case Normal:
                cssStyle += "font-style:normal;";
                break;
            case Italic:
                cssStyle += "font-style:italic;";
                break;
            case Oblique:
                cssStyle += "font-style:oblique;";
                break;
            default:
        }

        if (cssFontFamily != null && !cssFontFamily.equals("")) {
            cssStyle += "font-family:" + getCssFontFamily() + "; ";
        }

        if (cssFontWeight != null && !cssFontWeight.equals("")) {
            cssStyle += "font-weight:" + getCssFontWeight() + "; ";
        }

        cssStyle += "min-width:100%;max-width:100%;width:100%;min-height:100%;max-height:100%;height:100%;padding:0px;margin:0px;border:0px;";

        return cssStyle;
    }

    @JsonIgnore
    public String getOuterCssStyle() {
        cssStyle = "";
        if (getRiTop() != 0) {
            cssStyle += "top:" + getRiTop() + "%;";
        }
        if (getRiLeft() != 0) {
            cssStyle += "left:" + getRiLeft() + "%;";
        }
        if (getRiWidth() != 0) {
            cssStyle += "width:" + getRiWidth() + "%;";
        }
        if (getRiHeight() != 0) {
            cssStyle += "height:" + getRiHeight() + "%;";
        }
        if (getRiFontSize() != 0) {
            cssStyle += "font-size:" + getRiFontSize() + "pt;";
        }

        switch (getCssTextAlign()) {
            case Center:
                cssStyle += "text-align:center;";
                break;
            case Justify:
                cssStyle += "text-align:justify;";
                break;
            case Left:
                cssStyle += "text-align:left;";
                break;
            case Right:
                cssStyle += "text-align:right;";
                break;
            default:
        }

        switch (getCssFontStyle()) {
            case Normal:
                cssStyle += "font-style:normal;";
                break;
            case Italic:
                cssStyle += "font-style:italic;";
                break;
            case Oblique:
                cssStyle += "font-style:oblique;";
                break;
            default:
        }

        if (cssFontFamily != null && !cssFontFamily.equals("")) {
            cssStyle += "font-family:" + getCssFontFamily() + "; ";
        }

        if (cssFontWeight != null && !cssFontWeight.equals("")) {
            cssStyle += "font-weight:" + getCssFontWeight() + "; ";
        }

        if (cssVerticalAlign != null) {
            cssStyle += "vertical-align: " + cssVerticalAlign + "; ";
        }

        cssStyle += " position:absolute; overflow: hidden!important; ";

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

    public double getRiTop() {
        return riTop;
    }

    public void setRiTop(double riTop) {
        this.riTop = riTop;
    }

    public double getRiHeight() {
        if (riHeight == 0) {
            riHeight = 2;
        }
        return riHeight;
    }

    public void setRiHeight(double riHeight) {
        this.riHeight = riHeight;
    }

    public double getRiLeft() {
        return riLeft;
    }

    public void setRiLeft(double riLeft) {
        this.riLeft = riLeft;
    }

    public double getRiWidth() {
        if (riWidth == 0) {
            riWidth = 30;
        }
        return riWidth;
    }

    public void setRiWidth(double riWidth) {
        this.riWidth = riWidth;
    }

    public double getRiFontSize() {
        if (riFontSize == 0) {
            riFontSize = 12;
        }
        return riFontSize;
    }

    public void setRiFontSize(double riFontSize) {
        this.riFontSize = riFontSize;
    }

    public ReportItem getTestHeader() {
        return testHeader;
    }

    public void setTestHeader(ReportItem testHeader) {
        this.testHeader = testHeader;
    }

    public ReportItem getUnitHeader() {
        return unitHeader;
    }

    public void setUnitHeader(ReportItem unitHeader) {
        this.unitHeader = unitHeader;
    }

    public ReportItem getValueHeader() {
        return valueHeader;
    }

    public void setValueHeader(ReportItem valueHeader) {
        this.valueHeader = valueHeader;
    }

    public ReportItem getReferenceHeader() {
        return referenceHeader;
    }

    public void setReferenceHeader(ReportItem referenceHeader) {
        this.referenceHeader = referenceHeader;
    }

    public ReportItem getTestLabel() {
        return testLabel;
    }

    public void setTestLabel(ReportItem testLabel) {
        this.testLabel = testLabel;
    }

    public ReportItem getValueValue() {
        return valueValue;
    }

    public void setValueValue(ReportItem valueValue) {
        this.valueValue = valueValue;
    }

    public static void copyReportItem(ReportItem fromRi, ReportItem toRi) {
        toRi.name = fromRi.name;
        toRi.code = fromRi.code;
        toRi.htmltext = fromRi.htmltext;
        toRi.description = fromRi.description;
        toRi.orderNo = fromRi.orderNo;
        toRi.creater = fromRi.creater;
        toRi.createdAt = fromRi.createdAt;
        toRi.retired = fromRi.retired;
        toRi.retirer = fromRi.retirer;
        toRi.retiredAt = fromRi.retiredAt;
        toRi.retireComments = fromRi.retireComments;
        toRi.item = fromRi.item;
        toRi.ixItemType = fromRi.ixItemType;
        toRi.ixItemValueType = fromRi.ixItemValueType;
        toRi.cssPosition = fromRi.cssPosition;
        toRi.cssOverflow = fromRi.cssOverflow;
        toRi.cssFontStyle = fromRi.cssFontStyle;
        toRi.cssVerticalAlign = fromRi.cssVerticalAlign;
        toRi.cssTextAlign = fromRi.cssTextAlign;
        toRi.cssLeft = fromRi.cssLeft;
        toRi.cssTop = fromRi.cssTop;
        toRi.cssWidth = fromRi.cssWidth;
        toRi.cssHeight = fromRi.cssHeight;
        toRi.cssZorder = fromRi.cssZorder;
        toRi.cssClip = fromRi.cssClip;
        toRi.cssFontFamily = fromRi.cssFontFamily;
        toRi.cssFontVariant = fromRi.cssFontVariant;
        toRi.cssFontWeight = fromRi.cssFontWeight;
        toRi.cssFontSize = fromRi.cssFontSize;
        toRi.cssLineHeight = fromRi.cssLineHeight;
        toRi.cssBackColor = fromRi.cssBackColor;
        toRi.cssColor = fromRi.cssColor;
        toRi.cssBorderRadius = fromRi.cssBorderRadius;
        toRi.cssMargin = fromRi.cssMargin;
        toRi.cssPadding = fromRi.cssPadding;
        toRi.cssBorder = fromRi.cssBorder;
        toRi.formatPrefix = fromRi.formatPrefix;
        toRi.formatSuffix = fromRi.formatSuffix;
        toRi.formatString = fromRi.formatString;
        toRi.reportItemType = fromRi.reportItemType;
        toRi.category = fromRi.category;
        toRi.pageNo = fromRi.pageNo;
        toRi.referringItem = fromRi.referringItem;
        toRi.referringCategory = fromRi.referringCategory;
        toRi.riTop = fromRi.riTop;
        toRi.riHeight = fromRi.riHeight;
        toRi.riLeft = fromRi.riLeft;
        toRi.riWidth = fromRi.riWidth;
        toRi.riFontSize = fromRi.riFontSize;
        toRi.htPix = fromRi.htPix;
        toRi.wtPix = fromRi.wtPix;
        toRi.htmltext = fromRi.htmltext;
        toRi.testHeader = fromRi.testHeader;
        toRi.unitHeader = fromRi.unitHeader;
        toRi.valueHeader = fromRi.valueHeader;
        toRi.referenceHeader = fromRi.referenceHeader;
        toRi.testLabel = fromRi.testLabel;
        toRi.valueValue = fromRi.valueValue;

    }

    public InvestigationComponent getInvestigationComponent() {
        return investigationComponent;
    }

    public void setInvestigationComponent(InvestigationComponent investigationComponent) {
        this.investigationComponent = investigationComponent;
    }

    public DataEntryMethod getDataEntryMethod() {
        return dataEntryMethod;
    }

    public void setDataEntryMethod(DataEntryMethod dataEntryMethod) {
        this.dataEntryMethod = dataEntryMethod;
    }

    public ReportItem getFlagValue() {
        return flagValue;
    }

    public void setFlagValue(ReportItem flagValue) {
        this.flagValue = flagValue;
    }

    public boolean isAutomated() {
        return automated;
    }

    public void setAutomated(boolean automated) {
        this.automated = automated;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public Item getTest() {
        return test;
    }

    public void setTest(Item test) {
        this.test = test;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public Item getSampleComponent() {
        return sampleComponent;
    }

    public void setSampleComponent(Item sampleComponent) {
        this.sampleComponent = sampleComponent;
    }

    public InvestigationTube getTube() {
        return tube;
    }

    public void setTube(InvestigationTube tube) {
        this.tube = tube;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public boolean isCanNotApproveIfValueIsEmpty() {
        return canNotApproveIfValueIsEmpty;
    }

    public void setCanNotApproveIfValueIsEmpty(boolean canNotApproveIfValueIsEmpty) {
        this.canNotApproveIfValueIsEmpty = canNotApproveIfValueIsEmpty;
    }

    public boolean isCanNotApproveIfValueIsBelowAbsoluteLowValue() {
        return canNotApproveIfValueIsBelowAbsoluteLowValue;
    }

    public void setCanNotApproveIfValueIsBelowAbsoluteLowValue(boolean canNotApproveIfValueIsBelowAbsoluteLowValue) {
        this.canNotApproveIfValueIsBelowAbsoluteLowValue = canNotApproveIfValueIsBelowAbsoluteLowValue;
    }

    public boolean isCanNotApproveIfValueIsAboveAbsoluteHighValue() {
        return canNotApproveIfValueIsAboveAbsoluteHighValue;
    }

    public void setCanNotApproveIfValueIsAboveAbsoluteHighValue(boolean canNotApproveIfValueIsAboveAbsoluteHighValue) {
        this.canNotApproveIfValueIsAboveAbsoluteHighValue = canNotApproveIfValueIsAboveAbsoluteHighValue;
    }

    public double getAbsoluteLowValue() {
        return absoluteLowValue;
    }

    public void setAbsoluteLowValue(double absoluteLowValue) {
        this.absoluteLowValue = absoluteLowValue;
    }

    public double getAbsoluteHighValue() {
        return absoluteHighValue;
    }

    public void setAbsoluteHighValue(double absoluteHighValue) {
        this.absoluteHighValue = absoluteHighValue;
    }

    public String getEmptyValueWarning() {
        return emptyValueWarning;
    }

    public void setEmptyValueWarning(String emptyValueWarning) {
        this.emptyValueWarning = emptyValueWarning;
    }

    public String getBelowAbsoluteWarning() {
        return belowAbsoluteWarning;
    }

    public void setBelowAbsoluteWarning(String belowAbsoluteWarning) {
        this.belowAbsoluteWarning = belowAbsoluteWarning;
    }

    public String getAboveAbsoluteWarning() {
        return aboveAbsoluteWarning;
    }

    public void setAboveAbsoluteWarning(String aboveAbsoluteWarning) {
        this.aboveAbsoluteWarning = aboveAbsoluteWarning;
    }

}
