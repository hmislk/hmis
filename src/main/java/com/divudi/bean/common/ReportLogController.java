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

    public void fillReportLogs() {
        String jpql = "select rl from ReportLog rl " +
                "where rl.createdAt between :fd and :td order by rl.startTime desc";
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
}
