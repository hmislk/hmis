package com.divudi.bean.common;

import com.divudi.core.data.VisitHistoryType;
import com.divudi.core.data.dto.VisitHistoryRowDTO;
import com.divudi.core.entity.Patient;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.VisitHistoryService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Staff Visit History report (issue #25846): all bills and admissions of one
 * patient, newest first, with a visit type filter. Reached from the Reports
 * page (Statistics Reports), the patient dashboard and the inpatient dashboard.
 * Session scoped like the other navigation-driven controllers, because the
 * dashboards set the patient and then navigate here.
 */
@Named
@SessionScoped
public class VisitHistoryController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String PRIVILEGE = "ReportsVisitHistory";
    private static final String REPORT_PAGE = "/reports/stastistic_reports/visit_history?faces-redirect=true";

    @EJB
    private VisitHistoryService visitHistoryService;
    @Inject
    private WebUserController webUserController;

    private Patient patient;
    private VisitHistoryType visitType;
    private Date fromDate;
    private Date toDate;
    private List<VisitHistoryRowDTO> rows;
    private boolean truncated;

    /**
     * From the Reports page: start with an empty form.
     */
    public String navigateToVisitHistoryReport() {
        if (!hasAccess()) {
            return "";
        }
        clear();
        return REPORT_PAGE;
    }

    /**
     * From the patient dashboard or the inpatient dashboard: open the report
     * for that patient and run it.
     */
    public String navigateToVisitHistoryForPatient(Patient p) {
        if (!hasAccess()) {
            return "";
        }
        if (p == null || p.getId() == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        clear();
        patient = p;
        process();
        return REPORT_PAGE;
    }

    public void process() {
        rows = new ArrayList<>();
        truncated = false;
        if (!hasAccess()) {
            return;
        }
        if (patient == null || patient.getId() == null) {
            JsfUtil.addErrorMessage("Select a patient");
            return;
        }
        Date from = null;
        Date to = null;
        if (fromDate != null || toDate != null) {
            from = fromDate != null ? fromDate : new Date(0L);
            to = toDate != null ? endOfDayIfMidnight(toDate) : new Date();
            if (from.after(to)) {
                JsfUtil.addErrorMessage("From date is after To date");
                return;
            }
        }
        // Count truncation on the unfiltered list so a type filter cannot hide it.
        List<VisitHistoryRowDTO> all = visitHistoryService.findVisitHistory(patient.getId(), from, to, null, false);
        truncated = visitHistoryService.mayBeTruncated(all);
        for (VisitHistoryRowDTO r : all) {
            if (visitType == null || r.getVisitType() == visitType) {
                rows.add(r);
            }
        }
        if (truncated) {
            JsfUtil.addErrorMessage("This patient has a very large number of records. Older rows may be missing; narrow the date range.");
        }
    }

    public void clear() {
        patient = null;
        visitType = null;
        fromDate = null;
        toDate = null;
        rows = new ArrayList<>();
        truncated = false;
    }

    private boolean hasAccess() {
        if (webUserController == null || !webUserController.hasPrivilege(PRIVILEGE)) {
            JsfUtil.addErrorMessage("You do not have the privilege to view the Visit History");
            return false;
        }
        return true;
    }

    /**
     * A date picked without a time means the whole day.
     */
    private Date endOfDayIfMidnight(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        if (c.get(Calendar.HOUR_OF_DAY) == 0 && c.get(Calendar.MINUTE) == 0 && c.get(Calendar.SECOND) == 0) {
            c.set(Calendar.HOUR_OF_DAY, 23);
            c.set(Calendar.MINUTE, 59);
            c.set(Calendar.SECOND, 59);
            c.set(Calendar.MILLISECOND, 999);
        }
        return c.getTime();
    }

    public VisitHistoryType[] getVisitTypes() {
        return VisitHistoryType.values();
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public VisitHistoryType getVisitType() {
        return visitType;
    }

    public void setVisitType(VisitHistoryType visitType) {
        this.visitType = visitType;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<VisitHistoryRowDTO> getRows() {
        if (rows == null) {
            rows = new ArrayList<>();
        }
        return rows;
    }

    public boolean isTruncated() {
        return truncated;
    }
}
