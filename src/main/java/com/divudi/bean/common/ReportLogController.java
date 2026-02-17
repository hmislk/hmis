package com.divudi.bean.common;

import com.divudi.core.entity.report.ReportLog;
import com.divudi.core.facade.ReportLogFacade;
import com.divudi.core.util.CommonFunctions;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Named
@SessionScoped
public class ReportLogController implements Serializable {
    private static final long serialVersionUID = 1L;

    @EJB
    private ReportLogFacade reportLogFacade;

    @Inject
    SessionController sessionController;

    private Date fromDate;
    private Date toDate;
    private List<ReportLog> items;
    
    // Analytics data
    private List<Object[]> mostUsedReports;
    private List<Object[]> heaviestReports;
    private List<Object[]> failedReports;
    private List<Object[]> usageSummary;

    public void fillReportLogs() {
        String jpql = "select rl from ReportLog rl " +
                "where rl.startTime between :fd and :td order by rl.startTime desc";
        Map<String, Object> hm = new HashMap<>();
        hm.put("fd", getFromDate());
        hm.put("td", getToDate());
        items = reportLogFacade.findByJpql(jpql, hm, TemporalType.TIMESTAMP);
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<ReportLog> getItems() {
        return items;
    }

    public void setItems(List<ReportLog> items) {
        this.items = items;
    }
    
    /**
     * Get most frequently used reports with execution counts
     */
    public void fillMostUsedReports() {
        String jpql = "SELECT rl.reportName, COUNT(rl), AVG(rl.executionTimeInMillis), MAX(rl.startTime) " +
                     "FROM ReportLog rl " +
                     "WHERE rl.startTime BETWEEN :fd AND :td " +
                     "GROUP BY rl.reportName " +
                     "ORDER BY COUNT(rl) DESC";
        
        Map<String, Object> hm = new HashMap<>();
        hm.put("fd", getFromDate());
        hm.put("td", getToDate());
        mostUsedReports = reportLogFacade.findObjectArrayByJpql(jpql, hm, TemporalType.TIMESTAMP);
    }

    /**
     * Get failed reports with execution counts
     */
    public void fillFailedReports() {
        String jpql = "SELECT rl.reportName, " +
                "COUNT(rl), " +
                "SUM(CASE WHEN rl.executionTimeInMillis IS NULL THEN 1 ELSE 0 END), " +
                "MAX(rl.startTime) " +
                "FROM ReportLog rl " +
                "WHERE rl.startTime BETWEEN :fd AND :td " +
                "GROUP BY rl.reportName " +
                "HAVING SUM(CASE WHEN rl.executionTimeInMillis IS NULL THEN 1 ELSE 0 END) >= 1 " +
                "ORDER BY COUNT(rl) DESC";

        Map<String, Object> hm = new HashMap<>();
        hm.put("fd", getFromDate());
        hm.put("td", getToDate());
        failedReports = reportLogFacade.findObjectArrayByJpql(jpql, hm, TemporalType.TIMESTAMP);
    }
    
    /**
     * Get heaviest reports by average execution time
     */
    public void fillHeaviestReports() {
        String jpql = "SELECT rl.reportName, AVG(rl.executionTimeInMillis), COUNT(rl), MAX(rl.executionTimeInMillis) " +
                     "FROM ReportLog rl " +
                     "WHERE rl.startTime BETWEEN :fd AND :td " +
                     "GROUP BY rl.reportName " +
                     "HAVING COUNT(rl) > 0 " +
                     "ORDER BY AVG(rl.executionTimeInMillis) DESC";
        
        Map<String, Object> hm = new HashMap<>();
        hm.put("fd", getFromDate());
        hm.put("td", getToDate());
        heaviestReports = reportLogFacade.findObjectArrayByJpql(jpql, hm, TemporalType.TIMESTAMP);
    }
    
    /**
     * Get overall usage summary statistics
     */
    public void fillUsageSummary() {
        String jpql = "SELECT " +
                     "COUNT(rl) as totalExecutions, " +
                     "COUNT(DISTINCT rl.reportName) as uniqueReports, " +
                     "AVG(rl.executionTimeInMillis) as avgExecutionTime, " +
                     "SUM(rl.executionTimeInMillis) as totalExecutionTime, " +
                     "MAX(rl.executionTimeInMillis) as maxExecutionTime, " +
                     "MIN(rl.executionTimeInMillis) as minExecutionTime " +
                     "FROM ReportLog rl " +
                     "WHERE rl.startTime BETWEEN :fd AND :td";
        
        Map<String, Object> hm = new HashMap<>();
        hm.put("fd", getFromDate());
        hm.put("td", getToDate());
        usageSummary = reportLogFacade.findObjectArrayByJpql(jpql, hm, TemporalType.TIMESTAMP);
    }
    
    /**
     * Fill all analytics data at once
     */
    public void fillAllAnalytics() {
        fillMostUsedReports();
        fillHeaviestReports();
        fillUsageSummary();
        fillFailedReports();
    }

    // Getters and setters for analytics data
    public List<Object[]> getMostUsedReports() {
        if (mostUsedReports == null) {
            mostUsedReports = new ArrayList<>();
        }
        return mostUsedReports;
    }

    public void setMostUsedReports(List<Object[]> mostUsedReports) {
        this.mostUsedReports = mostUsedReports;
    }

    public List<Object[]> getFailedReports() {
        return failedReports;
    }

    public void setFailedReports(List<Object[]> failedReports) {
        this.failedReports = failedReports;
    }

    public List<Object[]> getHeaviestReports() {
        if (heaviestReports == null) {
            heaviestReports = new ArrayList<>();
        }
        return heaviestReports;
    }

    public void setHeaviestReports(List<Object[]> heaviestReports) {
        this.heaviestReports = heaviestReports;
    }

    public List<Object[]> getUsageSummary() {
        if (usageSummary == null) {
            usageSummary = new ArrayList<>();
        }
        return usageSummary;
    }

    public void setUsageSummary(List<Object[]> usageSummary) {
        this.usageSummary = usageSummary;
    }
}
