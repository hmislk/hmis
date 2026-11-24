package com.divudi.bean.inward;

import com.divudi.core.data.dto.CancelledAdmissionDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.AdmissionCancellationReason;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.AdmissionFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.TemporalType;

/**
 * Cancelled Admissions report (#25745): one row per cancelled admission in a
 * cancelled-date range, with who decided the admission and the cancellation and
 * why. Admissions cancelled before these facts were recorded show blanks.
 */
@Named
@SessionScoped
public class CancelledAdmissionReportController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(CancelledAdmissionReportController.class.getName());

    /** The retire comment the Cancel Admission action writes. */
    static final String CANCEL_MARK = "BHT Cancel";

    @EJB
    private AdmissionFacade admissionFacade;

    private Date fromDate = startOfCurrentMonth();
    private Date toDate = new Date();
    private AdmissionType admissionType;
    private AdmissionCancellationReason reason;
    private Institution institution;
    private Institution site;
    private Department department;

    private List<CancelledAdmissionDTO> rows;

    public void fill() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Select the From and To dates.");
            return;
        }
        Map<String, Object> params = new HashMap<>();
        String jpql = buildJpql(params, fromDate, toDate, admissionType, reason, institution, site, department);
        List<CancelledAdmissionDTO> list;
        try {
            @SuppressWarnings("unchecked")
            List<CancelledAdmissionDTO> found = (List<CancelledAdmissionDTO>) admissionFacade.findLightsByJpqlStrict(
                    jpql, params, TemporalType.TIMESTAMP, Integer.MAX_VALUE);
            list = found;
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Cancelled admissions report query failed", e);
            rows = null;
            JsfUtil.addErrorMessage("The report could not be loaded. Try again or contact the administrator.");
            return;
        }
        rows = list == null ? new ArrayList<>() : list;
    }

    /**
     * Builds the report query and fills {@code params}. Static and free of
     * container state so the filters can be tested alone.
     */
    static String buildJpql(Map<String, Object> params, Date from, Date to, AdmissionType type,
            AdmissionCancellationReason reason, Institution institution, Institution site, Department department) {
        StringBuilder jpql = new StringBuilder(
                "select new com.divudi.core.data.dto.CancelledAdmissionDTO("
                + "ad.id,"
                + "ad.retiredAt,"
                + "ad.bhtNo,"
                + "pp.name,"
                + "p.phn,"
                + "admt.name,"
                + "ad.dateOfAdmission,"
                + "rcp.name,"
                + "cr.name,"
                + "ad.comments,"
                + "cdbp.name,"
                + "coalesce(rup.name, ru.name)"
                + ") from Admission ad"
                + " left join ad.patient p"
                + " left join p.person pp"
                + " left join ad.admissionType admt"
                + " left join ad.referringConsultant rc"
                + " left join rc.person rcp"
                + " left join ad.cancellationReason cr"
                + " left join ad.cancellationDecidedBy cdb"
                + " left join cdb.person cdbp"
                + " left join ad.retirer ru"
                + " left join ru.webUserPerson rup"
                + " where ad.retired = true"
                + " and ad.retireComments = :cancelMark"
                + " and ad.retiredAt between :fromDate and :toDate");
        params.put("cancelMark", CANCEL_MARK);
        params.put("fromDate", from);
        params.put("toDate", to);
        if (type != null) {
            jpql.append(" and ad.admissionType = :admType");
            params.put("admType", type);
        }
        if (reason != null) {
            jpql.append(" and ad.cancellationReason = :reason");
            params.put("reason", reason);
        }
        if (institution != null) {
            jpql.append(" and ad.institution = :ins");
            params.put("ins", institution);
        }
        if (site != null) {
            jpql.append(" and ad.department.site = :site");
            params.put("site", site);
        }
        if (department != null) {
            jpql.append(" and ad.department = :dept");
            params.put("dept", department);
        }
        jpql.append(" order by ad.retiredAt desc");
        return jpql.toString();
    }

    public void clearDepartment() {
        department = null;
    }

    public void makeNull() {
        fromDate = startOfCurrentMonth();
        toDate = new Date();
        admissionType = null;
        reason = null;
        institution = null;
        site = null;
        department = null;
        rows = null;
    }

    private static Date startOfCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
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

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public AdmissionCancellationReason getReason() {
        return reason;
    }

    public void setReason(AdmissionCancellationReason reason) {
        this.reason = reason;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<CancelledAdmissionDTO> getRows() {
        return rows;
    }
}
