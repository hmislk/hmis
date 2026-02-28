package com.divudi.bean.common;

import com.divudi.core.data.reports.IReportType;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.report.ReportLog;
import com.divudi.service.ReportLogAsyncService;

import javax.ejb.EJB;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO : Move to a more appropriate package (util, common, etc.)
@Stateless
public class ReportTimerController implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(ReportTimerController.class.getName());

    @EJB
    private ReportLogAsyncService reportLogAsyncService;

    public void trackReportExecution(Runnable reportGenerationLogic, IReportType reportType, WebUser loggedUser) {
        trackReportExecution(reportGenerationLogic, reportType, reportType.getReportName(), loggedUser);
    }

    public void trackReportExecution(Runnable reportGenerationLogic, IReportType reportType, String reportName, WebUser loggedUser) {
        final Date startTime = new Date();

        final ReportLog reportLog = new ReportLog(reportType, reportName, loggedUser, startTime, null);

        ReportLog savedLog = null;

        try {
            Future<ReportLog> futureLog = reportLogAsyncService.logReport(reportLog);
            savedLog = futureLog.get();

            reportGenerationLogic.run();
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Skipping report generation due to invalid date range or missing date values", e);
        } catch (EJBTransactionRolledbackException e) {
            Throwable rootCause = e.getCause();
            if (rootCause instanceof IllegalArgumentException) {
                LOGGER.log(Level.WARNING, "Skipping report generation due to invalid date range or missing date values", rootCause);
            } else {
                LOGGER.log(Level.SEVERE, "Error occurred while generating the report", e);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while generating the report", e);
        }

        final Date endTime = new Date();

        if (savedLog != null) {
            savedLog.setEndTime(endTime);
            reportLogAsyncService.logReport(savedLog);
        }
    }
}
