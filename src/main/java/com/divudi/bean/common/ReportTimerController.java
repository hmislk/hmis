package com.divudi.bean.common;

import com.divudi.core.data.reports.IReportType;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.report.ReportLog;
import com.divudi.service.ReportLogAsyncService;

import javax.ejb.EJB;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import com.divudi.core.util.JsfUtil;
import javax.faces.context.FacesContext;
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

    // No transaction: entities a report only reads must not be saved. Inside one, those it
    // touches (e.g. auto-created finance details) are saved at commit (#24735).
    // Explicit service writes still commit in their own transaction.
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void trackReportExecution(Runnable reportGenerationLogic, IReportType reportType, WebUser loggedUser) {
        trackReportExecution(reportGenerationLogic, reportType, reportType.getReportName(), loggedUser);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void trackReportExecution(Runnable reportGenerationLogic, IReportType reportType, String reportName, WebUser loggedUser) {
        final Date startTime = new Date();

        final ReportLog reportLog = new ReportLog(reportType, reportName, loggedUser, startTime, null);

        ReportLog savedLog = null;

        // Auditing is a side concern: if the log row cannot be written, log it and still
        // run the report. Before, a failed write skipped the report and left it empty (#23525).
        try {
            Future<ReportLog> futureLog = reportLogAsyncService.logReport(reportLog);
            savedLog = futureLog.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Report log could not be written; running the report anyway", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Report log could not be written; running the report anyway", e);
        }

        try {
            reportGenerationLogic.run();
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Skipping report generation due to invalid date range or missing date values", e);
        } catch (EJBTransactionRolledbackException e) {
            IllegalArgumentException foundCause = null;
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (cause instanceof IllegalArgumentException) {
                    foundCause = (IllegalArgumentException) cause;
                    break;
                }
            }
            if (foundCause != null) {
                LOGGER.log(Level.WARNING, "Skipping report generation due to invalid date range or missing date values", foundCause);
            } else {
                LOGGER.log(Level.SEVERE, "Error occurred while generating the report", e);
                showReportFailedMessage();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while generating the report", e);
            showReportFailedMessage();
        }

        final Date endTime = new Date();

        if (savedLog != null) {
            savedLog.setEndTime(endTime);
            reportLogAsyncService.logReport(savedLog);
        }
    }

    /**
     * Tells the user the report did not run, so an empty result is not read as
     * "no transactions" (#23525). Skipped outside a JSF request (e.g. REST callers).
     */
    private void showReportFailedMessage() {
        if (FacesContext.getCurrentInstance() == null) {
            return;
        }
        JsfUtil.addErrorMessage("The report could not be generated. Please try again, or contact the system administrator if it keeps happening.");
    }
}
