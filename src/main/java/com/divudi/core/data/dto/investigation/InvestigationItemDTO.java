package com.divudi.core.data.dto.investigation;

public class InvestigationItemDTO {

    private Long id;
    private String name;
    private String code;
    private String description;
    private int orderNo;
    private String ixItemType;
    private String ixItemValueType;
    private boolean automated;
    private String resultCode;
    private String formatPrefix;
    private String formatSuffix;
    private String htmltext;
    private boolean canNotApproveIfValueIsEmpty;
    private double absoluteLowValue;
    private double absoluteHighValue;
    private double riTop;
    private double riLeft;
    private double riWidth;
    private double riHeight;
    private double riFontSize;
    private String cssTextAlign;
    private String cssFontStyle;
    private String message;

    public InvestigationItemDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getOrderNo() { return orderNo; }
    public void setOrderNo(int orderNo) { this.orderNo = orderNo; }
    public String getIxItemType() { return ixItemType; }
    public void setIxItemType(String ixItemType) { this.ixItemType = ixItemType; }
    public String getIxItemValueType() { return ixItemValueType; }
    public void setIxItemValueType(String ixItemValueType) { this.ixItemValueType = ixItemValueType; }
    public boolean isAutomated() { return automated; }
    public void setAutomated(boolean automated) { this.automated = automated; }
    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getFormatPrefix() { return formatPrefix; }
    public void setFormatPrefix(String formatPrefix) { this.formatPrefix = formatPrefix; }
    public String getFormatSuffix() { return formatSuffix; }
    public void setFormatSuffix(String formatSuffix) { this.formatSuffix = formatSuffix; }
    public String getHtmltext() { return htmltext; }
    public void setHtmltext(String htmltext) { this.htmltext = htmltext; }
    public boolean isCanNotApproveIfValueIsEmpty() { return canNotApproveIfValueIsEmpty; }
    public void setCanNotApproveIfValueIsEmpty(boolean canNotApproveIfValueIsEmpty) { this.canNotApproveIfValueIsEmpty = canNotApproveIfValueIsEmpty; }
    public double getAbsoluteLowValue() { return absoluteLowValue; }
    public void setAbsoluteLowValue(double absoluteLowValue) { this.absoluteLowValue = absoluteLowValue; }
    public double getAbsoluteHighValue() { return absoluteHighValue; }
    public void setAbsoluteHighValue(double absoluteHighValue) { this.absoluteHighValue = absoluteHighValue; }
    public double getRiTop() { return riTop; }
    public void setRiTop(double riTop) { this.riTop = riTop; }
    public double getRiLeft() { return riLeft; }
    public void setRiLeft(double riLeft) { this.riLeft = riLeft; }
    public double getRiWidth() { return riWidth; }
    public void setRiWidth(double riWidth) { this.riWidth = riWidth; }
    public double getRiHeight() { return riHeight; }
    public void setRiHeight(double riHeight) { this.riHeight = riHeight; }
    public double getRiFontSize() { return riFontSize; }
    public void setRiFontSize(double riFontSize) { this.riFontSize = riFontSize; }
    public String getCssTextAlign() { return cssTextAlign; }
    public void setCssTextAlign(String cssTextAlign) { this.cssTextAlign = cssTextAlign; }
    public String getCssFontStyle() { return cssFontStyle; }
    public void setCssFontStyle(String cssFontStyle) { this.cssFontStyle = cssFontStyle; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
