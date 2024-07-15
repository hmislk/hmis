package com.divudi.data;

import com.divudi.entity.ReportTemplate;
import java.util.List;

/**
 *
 * @author buddhika
 */
public class ReportTemplateRowBundle {

    private ReportTemplate reportTemplate;
    private List<ReportTemplateRowBundle> reportTemplateRowBundles;

    public ReportTemplate getReportTemplate() {
        return reportTemplate;
    }

    public void setReportTemplate(ReportTemplate reportTemplate) {
        this.reportTemplate = reportTemplate;
    }

    public List<ReportTemplateRowBundle> getReportTemplateRowBundles() {
        return reportTemplateRowBundles;
    }

    public void setReportTemplateRowBundles(List<ReportTemplateRowBundle> reportTemplateRowBundles) {
        this.reportTemplateRowBundles = reportTemplateRowBundles;
    }
    
    
    
    
}
