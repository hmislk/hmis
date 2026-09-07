package com.divudi.service.investigation;

import com.divudi.core.data.CssFontStyle;
import com.divudi.core.data.CssTextAlign;
import com.divudi.core.data.CssVerticalAlign;
import com.divudi.core.data.InvestigationItemType;
import com.divudi.core.data.InvestigationItemValueType;
import com.divudi.core.data.ReportItemType;
import com.divudi.core.data.dto.investigation.CommonReportItemDTO;
import com.divudi.core.data.dto.investigation.CommonReportItemUpdateDTO;
import com.divudi.core.data.dto.investigation.ReportFormatDTO;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.CommonReportItem;
import com.divudi.core.entity.lab.ReportFormat;
import com.divudi.core.facade.CommonReportItemFacade;
import com.divudi.core.facade.ReportFormatFacade;
import com.divudi.core.util.CommonFunctions;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read/write access to a lab report format's <b>common template</b> — the
 * {@code CommonReportItem} rows (patient-details block, signature block, footer)
 * that print on every report produced in that format.
 *
 * <p>Sibling of {@link InvestigationFormatApiService}, which covers the
 * per-investigation {@code InvestigationItem} rows and cannot reach these: common
 * template rows are keyed on the report-format {@code Category} and carry no
 * {@code item}, so the investigation-scoped queries never match them (#23529).</p>
 *
 * <p>Everything here is scoped to the {@code ReportFormat} Category subclass, which
 * is exactly what the common-template screen
 * (<code>admin/lims/report_template.xhtml</code>) offers. {@code CommonReportItem}
 * rows also back the HR/clinical form templates under arbitrary categories
 * ({@code FormFormatController}, {@code StaffController}); those are deliberately
 * out of reach of this lab-facing API.</p>
 */
@Stateless
public class ReportFormatApiService implements Serializable {

    @EJB
    private ReportFormatFacade reportFormatFacade;
    @EJB
    private CommonReportItemFacade commonReportItemFacade;

    // =========================================================================
    // Report formats
    // =========================================================================

    /**
     * Lists the non-retired report formats, each with the number of common-template
     * rows it owns, in the same order the common-template screen shows them.
     */
    public List<ReportFormatDTO> listFormats() {
        String j = "select f from ReportFormat f where f.retired=false order by f.name";
        List<ReportFormat> rows = reportFormatFacade.findByJpql(j);
        Map<Long, Integer> counts = countItemsByFormat();
        List<ReportFormatDTO> out = new ArrayList<>();
        for (ReportFormat row : rows) {
            Integer count = counts.get(row.getId());
            out.add(toFormatDTO(row, count == null ? 0 : count, null));
        }
        return out;
    }

    // =========================================================================
    // Common template items
    // =========================================================================

    public List<CommonReportItemDTO> listItems(Long categoryId) throws Exception {
        loadFormat(categoryId);
        Map<String, Object> m = new HashMap<>();
        m.put("catId", categoryId);
        String j = "select i from CommonReportItem i where i.retired=false "
                + "and i.category.id=:catId order by i.name";
        List<CommonReportItem> rows = commonReportItemFacade.findByJpql(j, m);
        List<CommonReportItemDTO> out = new ArrayList<>();
        for (CommonReportItem row : rows) {
            out.add(toItemDTO(row, null));
        }
        return out;
    }

    public CommonReportItemDTO getItem(Long categoryId, Long itemId) throws Exception {
        return toItemDTO(loadItem(itemId, categoryId), "Item found successfully");
    }

    public CommonReportItemDTO createItem(Long categoryId, CommonReportItemUpdateDTO req, WebUser user) throws Exception {
        if (req == null || req.getName() == null || req.getName().trim().isEmpty()) {
            throw new Exception("Item name is required");
        }
        ReportFormat format = loadFormat(categoryId);
        CommonReportItem item = new CommonReportItem();
        item.setCategory(format);
        item.setName(req.getName().trim());
        item.setCode(req.getCode() != null && !req.getCode().trim().isEmpty()
                ? req.getCode().trim() : CommonFunctions.nameToCode(req.getName()));
        applyFields(item, req);
        item.setCreater(user);
        item.setCreatedAt(new Date());
        item.setRetired(false);
        commonReportItemFacade.createAndFlush(item);
        return toItemDTO(item, "Item created successfully");
    }

    public CommonReportItemDTO updateItem(Long categoryId, Long itemId, CommonReportItemUpdateDTO req, WebUser user) throws Exception {
        if (req == null || !req.isValid()) {
            throw new Exception("Valid update request is required");
        }
        CommonReportItem item = loadItem(itemId, categoryId);
        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            item.setName(req.getName().trim());
        }
        if (req.getCode() != null) {
            item.setCode(req.getCode().trim());
        }
        applyFields(item, req);
        commonReportItemFacade.edit(item);
        return toItemDTO(item, "Item updated successfully");
    }

    public CommonReportItemDTO deleteItem(Long categoryId, Long itemId, WebUser user) throws Exception {
        CommonReportItem item = loadItem(itemId, categoryId);
        item.setRetired(true);
        item.setRetirer(user);
        item.setRetiredAt(new Date());
        commonReportItemFacade.edit(item);
        return toItemDTO(item, "Item retired successfully");
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Applies every field the request actually carries. A null field is left
     * untouched so a PUT can nudge a single coordinate; for the enum-backed fields
     * an empty string clears the value.
     */
    private void applyFields(CommonReportItem item, CommonReportItemUpdateDTO req) throws Exception {
        if (req.getDescription() != null) item.setDescription(req.getDescription());
        if (req.getOrderNo() != null) item.setOrderNo(req.getOrderNo());
        if (req.getPageNo() != null) item.setPageNo(req.getPageNo());
        if (req.getHtmltext() != null) item.setHtmltext(req.getHtmltext());
        if (req.getFormatPrefix() != null) item.setFormatPrefix(req.getFormatPrefix());
        if (req.getFormatSuffix() != null) item.setFormatSuffix(req.getFormatSuffix());
        if (req.getRiTop() != null) item.setRiTop(req.getRiTop());
        if (req.getRiLeft() != null) item.setRiLeft(req.getRiLeft());
        if (req.getRiWidth() != null) item.setRiWidth(req.getRiWidth());
        if (req.getRiHeight() != null) item.setRiHeight(req.getRiHeight());
        if (req.getRiFontSize() != null) item.setRiFontSize(req.getRiFontSize());
        if (req.getHtPix() != null) item.setHtPix(req.getHtPix());
        if (req.getWtPix() != null) item.setWtPix(req.getWtPix());
        if (req.getCssFontFamily() != null) item.setCssFontFamily(req.getCssFontFamily());
        if (req.getCssFontWeight() != null) item.setCssFontWeight(req.getCssFontWeight());
        if (req.getReportItemType() != null) {
            item.setReportItemType(parseEnum(ReportItemType.class, req.getReportItemType(), "reportItemType"));
        }
        if (req.getIxItemType() != null) {
            item.setIxItemType(parseEnum(InvestigationItemType.class, req.getIxItemType(), "ixItemType"));
        }
        if (req.getIxItemValueType() != null) {
            item.setIxItemValueType(parseEnum(InvestigationItemValueType.class, req.getIxItemValueType(), "ixItemValueType"));
        }
        if (req.getCssTextAlign() != null) {
            item.setCssTextAlign(parseEnum(CssTextAlign.class, req.getCssTextAlign(), "cssTextAlign"));
        }
        if (req.getCssVerticalAlign() != null) {
            item.setCssVerticalAlign(parseEnum(CssVerticalAlign.class, req.getCssVerticalAlign(), "cssVerticalAlign"));
        }
        if (req.getCssFontStyle() != null) {
            item.setCssFontStyle(parseEnum(CssFontStyle.class, req.getCssFontStyle(), "cssFontStyle"));
        }
    }

    /** Blank clears the value; anything else must name a constant of the enum. */
    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String raw, String field) throws Exception {
        if (raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new Exception("Invalid " + field + ": " + raw
                    + ". Valid values: " + Arrays.toString(enumClass.getEnumConstants()));
        }
    }

    private ReportFormat loadFormat(Long id) throws Exception {
        ReportFormat format = id == null ? null : reportFormatFacade.find(id);
        if (format == null || format.isRetired()) {
            throw new Exception("Report format not found with ID: " + id);
        }
        return format;
    }

    private CommonReportItem loadItem(Long itemId, Long categoryId) throws Exception {
        loadFormat(categoryId);
        CommonReportItem item = itemId == null ? null : commonReportItemFacade.find(itemId);
        if (item == null || item.isRetired()) {
            throw new Exception("Common report item not found with ID: " + itemId);
        }
        if (item.getCategory() == null || !item.getCategory().getId().equals(categoryId)) {
            throw new Exception("Item " + itemId + " does not belong to report format " + categoryId);
        }
        return item;
    }

    /** Row counts for every format in one query, so listing formats is not N+1. */
    private Map<Long, Integer> countItemsByFormat() {
        String j = "select i.category.id, count(i) from CommonReportItem i "
                + "where i.retired=false and i.category is not null group by i.category.id";
        List<Object[]> rows = commonReportItemFacade.findObjectsArrayByJpql(j, new HashMap<>(), TemporalType.TIMESTAMP);
        Map<Long, Integer> counts = new HashMap<>();
        if (rows != null) {
            for (Object[] row : rows) {
                counts.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
            }
        }
        return counts;
    }

    private ReportFormatDTO toFormatDTO(ReportFormat format, Integer itemCount, String msg) {
        ReportFormatDTO dto = new ReportFormatDTO();
        dto.setId(format.getId());
        dto.setName(format.getName());
        dto.setCode(format.getCode());
        dto.setDescription(format.getDescription());
        dto.setOrderNo(format.getOrderNo());
        if (format.getParentCategory() != null) {
            dto.setParentCategoryId(format.getParentCategory().getId());
            dto.setParentCategoryName(format.getParentCategory().getName());
        }
        dto.setItemCount(itemCount);
        dto.setMessage(msg);
        return dto;
    }

    /**
     * Reports the row as rendered, not as stored. Several {@code ReportItem} getters
     * substitute a default when the stored value is empty — {@code riWidth} 30,
     * {@code riHeight} 2, {@code riFontSize} 12 (#23528), {@code ixItemType} Label,
     * {@code cssTextAlign} Left, {@code cssFontStyle} Normal — and this reports what
     * they return, i.e. what the printed report uses. Matches
     * {@code InvestigationFormatApiService.toItemDTO}.
     */
    private CommonReportItemDTO toItemDTO(CommonReportItem item, String msg) {
        CommonReportItemDTO dto = new CommonReportItemDTO();
        dto.setId(item.getId());
        if (item.getCategory() != null) {
            dto.setCategoryId(item.getCategory().getId());
            dto.setCategoryName(item.getCategory().getName());
        }
        dto.setName(item.getName());
        dto.setCode(item.getCode());
        dto.setDescription(item.getDescription());
        dto.setOrderNo(item.getOrderNo());
        dto.setPageNo(item.getPageNo());
        dto.setReportItemType(item.getReportItemType() != null ? item.getReportItemType().name() : null);
        dto.setIxItemType(item.getIxItemType() != null ? item.getIxItemType().name() : null);
        dto.setIxItemValueType(item.getIxItemValueType() != null ? item.getIxItemValueType().name() : null);
        dto.setHtmltext(item.getHtmltext());
        dto.setFormatPrefix(item.getFormatPrefix());
        dto.setFormatSuffix(item.getFormatSuffix());
        dto.setRiTop(item.getRiTop());
        dto.setRiLeft(item.getRiLeft());
        dto.setRiWidth(item.getRiWidth());
        dto.setRiHeight(item.getRiHeight());
        dto.setRiFontSize(item.getRiFontSize());
        dto.setHtPix(item.getHtPix());
        dto.setWtPix(item.getWtPix());
        dto.setCssTextAlign(item.getCssTextAlign() != null ? item.getCssTextAlign().name() : null);
        dto.setCssVerticalAlign(item.getCssVerticalAlign() != null ? item.getCssVerticalAlign().name() : null);
        dto.setCssFontStyle(item.getCssFontStyle() != null ? item.getCssFontStyle().name() : null);
        dto.setCssFontFamily(item.getCssFontFamily());
        dto.setCssFontWeight(item.getCssFontWeight());
        dto.setMessage(msg);
        return dto;
    }
}
