package com.divudi.entity.report;

import com.divudi.data.reports.IReportType;
import com.divudi.entity.WebUser;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
public class ReportLog implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    static final long serialVersionUID = 1L;

    @Column(nullable = false)
    private String reportType;

    @Column(nullable = false)
    private String reportName;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GENERATEDBY")
    private WebUser generatedBy;

    @Column(nullable = true)
    private Long executionTimeInMillis;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }

    public ReportLog(String reportType, String reportName, Date createdAt, WebUser generatedBy,
                     Long executionTimeInMillis, Date startTime, Date endTime) {
        this.reportType = reportType;
        this.reportName = reportName;
        this.createdAt = createdAt;
        this.generatedBy = generatedBy;
        this.executionTimeInMillis = executionTimeInMillis;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public ReportLog() {
    }

    public Long getId() {
        return id;
    }

    public ReportLog setId(Long id) {
        this.id = id;

        return this;
    }

    public String getReportType() {
        return reportType;
    }

    public ReportLog setReportType(IReportType report) {
        this.reportType = report.getReportType();

        return this;
    }

    public String getReportName() {
        return reportName;
    }

    public ReportLog setReportName(IReportType report) {
        this.reportName = report.getValue();

        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public ReportLog setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;

        return this;
    }

    public WebUser getGeneratedBy() {
        return generatedBy;
    }

    public ReportLog setGeneratedBy(WebUser generatedBy) {
        this.generatedBy = generatedBy;

        return this;
    }

    public Long getExecutionTimeInMillis() {
        return executionTimeInMillis;
    }

    public ReportLog setExecutionTimeInMillis(Long executionTimeInMillis) {
        this.executionTimeInMillis = executionTimeInMillis;

        return this;
    }

    public Date getStartTime() {
        return startTime;
    }

    public ReportLog setStartTime(Date startTime) {
        this.startTime = startTime;

        if (endTime != null && startTime != null) {
            executionTimeInMillis = endTime.getTime() - startTime.getTime();
        }

        return this;
    }

    public Date getEndTime() {
        return endTime;
    }

    public ReportLog setEndTime(Date endTime) {
        this.endTime = endTime;

        if (endTime != null && startTime != null) {
            executionTimeInMillis = endTime.getTime() - startTime.getTime();
        }

        return this;
    }

    public ReportLog setReport(IReportType reportType) {
        this.reportType = reportType.getReportType();
        this.reportName = reportType.getValue();

        return this;
    }

    public IReportType getReport() {
        if (reportType == null || reportName == null) {
            return null;
        }

        try {
            Class<?> clazz = Class.forName("com.divudi.data.reports." + reportType);

            if (clazz.isEnum() && IReportType.class.isAssignableFrom(clazz)) {
                for (Object constant : clazz.getEnumConstants()) {
                    IReportType report = (IReportType) constant;
                    if (report.getValue().equalsIgnoreCase(reportName)) {
                        return report;
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }
}
