package com.divudi.data;

import com.divudi.entity.ReportTemplate;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author buddhika
 */
public class ReportTemplateRowBundle {

    private ReportTemplate reportTemplate;
    private List<ReportTemplateRow> reportTemplateRows;
    private Double total;

    public ReportTemplate getReportTemplate() {
        return reportTemplate;
    }

    public void setReportTemplate(ReportTemplate reportTemplate) {
        this.reportTemplate = reportTemplate;
    }

    public List<ReportTemplateRow> getReportTemplateRows() {
        if(reportTemplateRows==null){
            reportTemplateRows = new ArrayList<>();
        }
        return reportTemplateRows;
    }

    public void setReportTemplateRows(List<ReportTemplateRow> reportTemplateRows) {
        this.reportTemplateRows = reportTemplateRows;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

   
    
    
    
    
}
