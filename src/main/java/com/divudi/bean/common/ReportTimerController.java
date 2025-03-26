package com.divudi.bean.common;

import com.divudi.data.reports.IReportType;
import com.divudi.entity.WebUser;
import com.divudi.entity.report.ReportLog;
import com.divudi.facade.ReportLogFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class ReportTimerController implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(ReportTimerController.class.getName());

    @EJB
    private ReportLogFacade reportLogFacade;

    public void trackReportExecution(Runnable reportGenerationLogic, IReportType reportType, WebUser loggedUser) {
        final Date startTime = new Date();

        try {
            reportGenerationLogic.run();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while generating the report", e);
        }

        final Date endTime = new Date();

        final long durationInMilliseconds = endTime.getTime() - startTime.getTime();

        ReportLog reportLog = new ReportLog()
                .setReport(reportType)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setExecutionTimeInMillis(durationInMilliseconds);

        save(reportLog, loggedUser);
    }

    public void save(ReportLog reportLog, WebUser loggedUser) {
        if (reportLog == null) {
            return;
        }

        if (reportLog.getId() == null) {
            reportLog.setGeneratedBy(loggedUser);

            try {
                getFacade().create(reportLog);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error occurred while saving the report log", e);
            }
        } else {
            getFacade().edit(reportLog);
        }
    }

    public ReportLogFacade getFacade() {
        return reportLogFacade;
    }
}
