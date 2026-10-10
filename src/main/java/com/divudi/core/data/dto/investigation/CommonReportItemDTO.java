package com.divudi.core.data.dto.investigation;

/**
 * One row of a report format's common template ({@code CommonReportItem}) — the
 * patient-details block, signature block and footer that print on every report of
 * that format.
 *
 * <p>Sibling of {@link InvestigationItemDTO}. It carries the same geometry
 * ({@code riTop}/{@code riLeft}/{@code riWidth}/{@code riHeight}/{@code riFontSize})
 * but drops the analyzer-only fields (machine, test, resultCode, absolute
 * low/high values) which are meaningless on a common template, and adds the
 * fields the common-template screen exposes and the per-investigation one does
 * not: {@code reportItemType}, {@code cssVerticalAlign}, {@code cssFontFamily},
 * {@code cssFontWeight}, {@code htPix} and {@code wtPix}.</p>
 *
 * <p><b>Geometry is reported as rendered, not as stored.</b> {@code riWidth},
 * {@code riHeight} and {@code riFontSize} come from the entity getters, which
 * substitute 30, 2 and 12 respectively when the stored value is 0 — the same
 * numbers the printed report uses. See issue #23528.</p>
 */
public class CommonReportItemDTO {

    private Long id;
    private Long categoryId;
    private String categoryName;
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
    private String message;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
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
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
