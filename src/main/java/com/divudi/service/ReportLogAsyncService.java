package com.divudi.service;

import com.divudi.core.entity.report.ReportLog;
import com.divudi.core.facade.ReportLogFacade;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class ReportLogAsyncService {
    private static final Logger LOGGER = Logger.getLogger(ReportLogAsyncService.class.getName());

    @EJB
    private ReportLogFacade reportLogFacade;

    @Asynchronous
    public void logReport(ReportLog reportLog) {
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
