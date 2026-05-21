package com.divudi.core.data.dto.investigation;

public class InvestigationItemCreateDTO {

    private String name;
    private String code;
    private String description;
    private Integer orderNo;
    private String ixItemType;
    private String ixItemValueType;
    private Boolean automated;
    private String resultCode;
    private String formatPrefix;
    private String formatSuffix;
    private String htmltext;
    private Boolean canNotApproveIfValueIsEmpty;
    private Double absoluteLowValue;
    private Double absoluteHighValue;
    private Double riTop;
    private Double riLeft;
    private Double riWidth;
    private Double riHeight;
    private Double riFontSize;
    private String cssTextAlign;
    private String cssFontStyle;

    public boolean isValid() {
        return name != null && !name.trim().isEmpty() && ixItemType != null && !ixItemType.trim().isEmpty();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getOrderNo() { return orderNo; }
    public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
    public String getIxItemType() { return ixItemType; }
    public void setIxItemType(String ixItemType) { this.ixItemType = ixItemType; }
    public String getIxItemValueType() { return ixItemValueType; }
    public void setIxItemValueType(String ixItemValueType) { this.ixItemValueType = ixItemValueType; }
    public Boolean getAutomated() { return automated; }
    public void setAutomated(Boolean automated) { this.automated = automated; }
    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getFormatPrefix() { return formatPrefix; }
    public void setFormatPrefix(String formatPrefix) { this.formatPrefix = formatPrefix; }
    public String getFormatSuffix() { return formatSuffix; }
    public void setFormatSuffix(String formatSuffix) { this.formatSuffix = formatSuffix; }
    public String getHtmltext() { return htmltext; }
    public void setHtmltext(String htmltext) { this.htmltext = htmltext; }
    public Boolean getCanNotApproveIfValueIsEmpty() { return canNotApproveIfValueIsEmpty; }
    public void setCanNotApproveIfValueIsEmpty(Boolean canNotApproveIfValueIsEmpty) { this.canNotApproveIfValueIsEmpty = canNotApproveIfValueIsEmpty; }
    public Double getAbsoluteLowValue() { return absoluteLowValue; }
    public void setAbsoluteLowValue(Double absoluteLowValue) { this.absoluteLowValue = absoluteLowValue; }
    public Double getAbsoluteHighValue() { return absoluteHighValue; }
    public void setAbsoluteHighValue(Double absoluteHighValue) { this.absoluteHighValue = absoluteHighValue; }
    public Double getRiTop() { return riTop; }
    public void setRiTop(Double riTop) { this.riTop = riTop; }
    public Double getRiLeft() { return riLeft; }
    public void setRiLeft(Double riLeft) { this.riLeft = riLeft; }
    public Double getRiWidth() { return riWidth; }
    public void setRiWidth(Double riWidth) { this.riWidth = riWidth; }
    public Double getRiHeight() { return riHeight; }
    public void setRiHeight(Double riHeight) { this.riHeight = riHeight; }
    public Double getRiFontSize() { return riFontSize; }
    public void setRiFontSize(Double riFontSize) { this.riFontSize = riFontSize; }
    public String getCssTextAlign() { return cssTextAlign; }
    public void setCssTextAlign(String cssTextAlign) { this.cssTextAlign = cssTextAlign; }
    public String getCssFontStyle() { return cssFontStyle; }
    public void setCssFontStyle(String cssFontStyle) { this.cssFontStyle = cssFontStyle; }
}
