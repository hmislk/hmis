package com.divudi.data;

import com.divudi.entity.ReportTemplate;
import com.divudi.entity.channel.SessionInstance;
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
    private Double totalIn;
    private Double totalOut;
    private Long countIn;
    private Long countOut;
    private Long count;
    private String name;
    private String description;
    private SessionInstance sessionInstance;
    private Long long1;
    private Long long2;
    private Long long3;
    private Long long4;
    private Long long5;
    private Long long6;
    private Long long7;
    private Long long8;
    private Long long9;
    private Long long10;
    
    
    
    
    
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

    public Double getTotalIn() {
        return totalIn;
    }

    public void setTotalIn(Double totalIn) {
        this.totalIn = totalIn;
    }

    public Double getTotalOut() {
        return totalOut;
    }

    public void setTotalOut(Double totalOut) {
        this.totalOut = totalOut;
    }

    public Long getCountIn() {
        return countIn;
    }

    public void setCountIn(Long countIn) {
        this.countIn = countIn;
    }

    public Long getCountOut() {
        return countOut;
    }

    public void setCountOut(Long countOut) {
        this.countOut = countOut;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
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

    public SessionInstance getSessionInstance() {
        return sessionInstance;
    }

    public void setSessionInstance(SessionInstance sessionInstance) {
        this.sessionInstance = sessionInstance;
    }

    public Long getLong1() {
        return long1;
    }

    public void setLong1(Long long1) {
        this.long1 = long1;
    }

    public Long getLong2() {
        return long2;
    }

    public void setLong2(Long long2) {
        this.long2 = long2;
    }

    public Long getLong3() {
        return long3;
    }

    public void setLong3(Long long3) {
        this.long3 = long3;
    }

    public Long getLong4() {
        return long4;
    }

    public void setLong4(Long long4) {
        this.long4 = long4;
    }

    public Long getLong5() {
        return long5;
    }

    public void setLong5(Long long5) {
        this.long5 = long5;
    }

    public Long getLong6() {
        return long6;
    }

    public void setLong6(Long long6) {
        this.long6 = long6;
    }

    public Long getLong7() {
        return long7;
    }

    public void setLong7(Long long7) {
        this.long7 = long7;
    }

    public Long getLong8() {
        return long8;
    }

    public void setLong8(Long long8) {
        this.long8 = long8;
    }

    public Long getLong9() {
        return long9;
    }

    public void setLong9(Long long9) {
        this.long9 = long9;
    }

    public Long getLong10() {
        return long10;
    }

    public void setLong10(Long long10) {
        this.long10 = long10;
    }

   
    
    
    
    
}
