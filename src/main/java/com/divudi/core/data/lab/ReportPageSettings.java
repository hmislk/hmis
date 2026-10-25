package com.divudi.core.data.lab;

import com.divudi.core.entity.lab.ReportItem;
import com.google.gson.Gson;
import java.io.Serializable;

/**
 * Per-page settings of a multi-page lab report template. Stored as JSON in the
 * html text of an inert "@page-settings" template item, one per page, so no
 * schema change is needed. A page without a settings item uses the defaults,
 * which equal today's single-page behaviour.
 */
public class ReportPageSettings implements Serializable {

    public static final String SETTINGS_ITEM_NAME = "@page-settings";
    public static final String DEFAULT_PAGE_NUMBER_TEXT = "Page {page} of {pages}";
    public static final String BACKGROUND_OWN = "own";
    public static final String BACKGROUND_PAGE1 = "page1";
    public static final String BACKGROUND_NONE = "none";
    public static final String CONFIG_KEY_ENABLED = "Lab Report - Multi-page templates enabled";

    private static final Gson GSON = new Gson();

    private boolean commonReport = true;
    private boolean pageNumber = true;
    private String pageNumberText = DEFAULT_PAGE_NUMBER_TEXT;
    private String background = BACKGROUND_OWN;

    public static ReportPageSettings fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ReportPageSettings();
        }
        try {
            ReportPageSettings s = GSON.fromJson(json, ReportPageSettings.class);
            return s == null ? new ReportPageSettings() : s;
        } catch (RuntimeException e) {
            return new ReportPageSettings();
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Page of an item: 0 (unset) and 1 are page 1.
     */
    public static int pageOf(ReportItem item) {
        if (item == null || item.getPageNo() < 1) {
            return 1;
        }
        return item.getPageNo();
    }

    public static boolean isSettingsItem(ReportItem item) {
        return item != null && SETTINGS_ITEM_NAME.equals(item.getName());
    }

    public String formatPageNumber(int page, int pages) {
        String text = (pageNumberText == null || pageNumberText.trim().isEmpty())
                ? DEFAULT_PAGE_NUMBER_TEXT : pageNumberText;
        return text.replace("{page}", String.valueOf(page)).replace("{pages}", String.valueOf(pages));
    }

    public boolean isCommonReport() {
        return commonReport;
    }

    public void setCommonReport(boolean commonReport) {
        this.commonReport = commonReport;
    }

    public boolean isPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(boolean pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getPageNumberText() {
        return pageNumberText;
    }

    public void setPageNumberText(String pageNumberText) {
        this.pageNumberText = pageNumberText;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }
}
