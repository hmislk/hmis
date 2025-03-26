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

    public ReportLog(IReportType reportType, WebUser generatedBy, Date startTime, Date endTime) {
        this.generatedBy = generatedBy;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reportType = reportType.getReportType();
        this.reportName = reportType.getValue();
        this.executionTimeInMillis = endTime.getTime() - startTime.getTime();
    }

    public ReportLog() {
    }

    public Long getId() {
        return id;
    }

    public String getReportType() {
        return reportType;
    }

    public String getReportName() {
        return reportName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public WebUser getGeneratedBy() {
        return generatedBy;
    }

    public Long getExecutionTimeInMillis() {
        return executionTimeInMillis;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
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
