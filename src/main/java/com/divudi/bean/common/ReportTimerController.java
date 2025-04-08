package com.divudi.bean.common;

import com.divudi.core.data.reports.IReportType;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.report.ReportLog;
import com.divudi.core.facade.ReportLogFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO : Move to a more appropriate package (util, common, etc.)
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

        final ReportLog reportLog = new ReportLog(reportType, loggedUser, startTime, endTime);

        save(reportLog);
    }

    public void save(ReportLog reportLog) {
        if (reportLog == null) {
            return;
        }

        if (reportLog.getId() == null) {
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
