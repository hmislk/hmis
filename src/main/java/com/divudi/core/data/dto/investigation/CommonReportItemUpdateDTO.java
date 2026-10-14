package com.divudi.core.data.dto.investigation;

/**
 * Request body for creating or updating one common-template row
 * ({@code CommonReportItem}) of a lab report format.
 *
 * <p>Mirrors {@link InvestigationItemUpdateDTO} for the geometry fields, minus the
 * analyzer-only ones (machineId, testId, resultCode, absolute low/high values) which
 * a common template never uses, plus the fields the common-template screen
 * (<code>admin/lims/report_template.xhtml</code>) exposes: {@code reportItemType},
 * {@code cssVerticalAlign}, {@code cssFontFamily}, {@code cssFontWeight},
 * {@code htPix} and {@code wtPix}.</p>
 *
 * <p>Every field is optional and a {@code null} field is left untouched, so a PUT can
 * carry just the one value being nudged. For the enum-backed fields
 * ({@code reportItemType}, {@code ixItemType}, {@code ixItemValueType},
 * {@code cssTextAlign}, {@code cssVerticalAlign}, {@code cssFontStyle}) an empty
 * string clears the value instead of setting it.</p>
 */
public class CommonReportItemUpdateDTO {

    private String name;
    private String code;
    private String description;
    private Integer orderNo;
    private Integer pageNo;
    private String reportItemType;
    private String ixItemType;
    private String ixItemValueType;
    private String htmltext;
    private String formatPrefix;
    private String formatSuffix;
    private Double riTop;
    private Double riLeft;
    private Double riWidth;
    private Double riHeight;
    private Double riFontSize;
    private Double htPix;
    private Double wtPix;
    private String cssTextAlign;
    private String cssVerticalAlign;
    private String cssFontStyle;
    private String cssFontFamily;
    private String cssFontWeight;

    /**
     * True when the body carries at least one field to apply. Guards against a PUT
     * with an empty or unparseable body silently reporting success.
     */
    public boolean isValid() {
        return name != null || code != null || description != null || orderNo != null
                || pageNo != null || reportItemType != null || ixItemType != null
                || ixItemValueType != null || htmltext != null || formatPrefix != null
                || formatSuffix != null || riTop != null || riLeft != null || riWidth != null
                || riHeight != null || riFontSize != null || htPix != null || wtPix != null
                || cssTextAlign != null || cssVerticalAlign != null || cssFontStyle != null
                || cssFontFamily != null || cssFontWeight != null;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getOrderNo() { return orderNo; }
    public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
    public Integer getPageNo() { return pageNo; }
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
    public String getReportItemType() { return reportItemType; }
    public void setReportItemType(String reportItemType) { this.reportItemType = reportItemType; }
    public String getIxItemType() { return ixItemType; }
    public void setIxItemType(String ixItemType) { this.ixItemType = ixItemType; }
    public String getIxItemValueType() { return ixItemValueType; }
    public void setIxItemValueType(String ixItemValueType) { this.ixItemValueType = ixItemValueType; }
    public String getHtmltext() { return htmltext; }
    public void setHtmltext(String htmltext) { this.htmltext = htmltext; }
    public String getFormatPrefix() { return formatPrefix; }
    public void setFormatPrefix(String formatPrefix) { this.formatPrefix = formatPrefix; }
    public String getFormatSuffix() { return formatSuffix; }
    public void setFormatSuffix(String formatSuffix) { this.formatSuffix = formatSuffix; }
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
    public Double getHtPix() { return htPix; }
    public void setHtPix(Double htPix) { this.htPix = htPix; }
    public Double getWtPix() { return wtPix; }
    public void setWtPix(Double wtPix) { this.wtPix = wtPix; }
    public String getCssTextAlign() { return cssTextAlign; }
    public void setCssTextAlign(String cssTextAlign) { this.cssTextAlign = cssTextAlign; }
    public String getCssVerticalAlign() { return cssVerticalAlign; }
    public void setCssVerticalAlign(String cssVerticalAlign) { this.cssVerticalAlign = cssVerticalAlign; }
    public String getCssFontStyle() { return cssFontStyle; }
    public void setCssFontStyle(String cssFontStyle) { this.cssFontStyle = cssFontStyle; }
    public String getCssFontFamily() { return cssFontFamily; }
    public void setCssFontFamily(String cssFontFamily) { this.cssFontFamily = cssFontFamily; }
    public String getCssFontWeight() { return cssFontWeight; }
    public void setCssFontWeight(String cssFontWeight) { this.cssFontWeight = cssFontWeight; }
}
