package com.divudi.bean.clinical;

import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.clinical.ClinicalFindingValueType;
import com.divudi.core.data.dto.EmrDiagnosisCountDTO;
import com.divudi.core.data.dto.EmrDiagnosisVisitDTO;
import com.divudi.core.data.inward.PatientEncounterType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * EMR report of OPD visits (encounter type OpdVisit) grouped by the diagnosis
 * recorded on the visit (ClinicalFindingValue type VisitDiagnosis), with the
 * presenting complaints of each visit as a secondary view. Only visits where a
 * diagnosis was recorded are covered.
 */
@Named
@SessionScoped
public class EmrDiagnosisReportController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @Inject
    private WebUserController webUserController;

    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Department department;
    private Doctor doctor;

    private List<EmrDiagnosisCountDTO> diagnosisCounts;
    private long totalVisits;
    private long totalDiagnosisRecords;
    private EmrDiagnosisCountDTO selectedDiagnosis;
    private List<EmrDiagnosisVisitDTO> drillDownVisits;
    private List<EmrDiagnosisVisitDTO> complaintRows;

    public String navigateToEmrDiagnosisReport() {
        clearResults();
        return "/emr/reports/visits_by_diagnosis?faces-redirect=true";
    }

    public void process() {
        clearResults();
        if (!webUserController.hasPrivilege("EmrVisitsByDiagnosisReport")) {
            JsfUtil.addErrorMessage("You do not have the privilege to view this report");
            return;
        }
        if (getFromDate() == null || getToDate() == null) {
            JsfUtil.addErrorMessage("Select the From and To dates");
            return;
        }
        Map<String, Object> params = new HashMap<>();
        String jpql = "select new com.divudi.core.data.dto.EmrDiagnosisCountDTO("
                + " v.itemValue.id, v.itemValue.name, count(distinct e.id)) "
                + fromWhere(params, null)
                + " group by v.itemValue.id, v.itemValue.name "
                + " order by count(distinct e.id) desc, v.itemValue.name";
        diagnosisCounts = (List<EmrDiagnosisCountDTO>) patientEncounterFacade
                .findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (diagnosisCounts == null) {
            diagnosisCounts = new ArrayList<>();
        }
        Map<String, Object> countParams = new HashMap<>();
        String countJpql = "select count(distinct e.id) " + fromWhere(countParams, null);
        totalVisits = patientEncounterFacade.findLongByJpql(countJpql, countParams, TemporalType.TIMESTAMP);
        totalDiagnosisRecords = 0;
        for (EmrDiagnosisCountDTO d : diagnosisCounts) {
            totalDiagnosisRecords += d.getVisitCount() == null ? 0 : d.getVisitCount();
        }
        complaintRows = fetchVisitRows(null);
    }

    public void showVisitsForDiagnosis(EmrDiagnosisCountDTO row) {
        selectedDiagnosis = row;
        drillDownVisits = row == null ? null : fetchVisitRows(row.getDiagnosisId());
    }

    private List<EmrDiagnosisVisitDTO> fetchVisitRows(Long diagnosisId) {
        Map<String, Object> params = new HashMap<>();
        String jpql = "select new com.divudi.core.data.dto.EmrDiagnosisVisitDTO("
                + " e.id, coalesce(e.encounterDate, e.createdAt), e.patient.person.name, e.patient.phn, "
                + " d.name, coalesce(odp.name, crp.name), v.itemValue.name, e.comments) "
                + fromWhere(params, diagnosisId)
                + " order by coalesce(e.encounterDate, e.createdAt) desc, e.id desc";
        List<EmrDiagnosisVisitDTO> rows = (List<EmrDiagnosisVisitDTO>) patientEncounterFacade
                .findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        return rows == null ? new ArrayList<>() : rows;
    }

    /**
     * Shared FROM / WHERE part: OPD visits in the date range that have at least
     * one diagnosis recorded. The select list decides which aliases are used.
     */
    private String fromWhere(Map<String, Object> params, Long diagnosisId) {
        StringBuilder sb = new StringBuilder();
        sb.append(" from ClinicalFindingValue v join v.encounter e ")
                .append(" left join e.department d ")
                .append(" left join e.opdDoctor od left join od.person odp ")
                .append(" left join e.creater cr left join cr.webUserPerson crp left join cr.staff crs ")
                .append(" where v.retired=false and e.retired=false ")
                .append(" and v.clinicalFindingValueType=:vt ")
                .append(" and v.itemValue is not null ")
                .append(" and e.patientEncounterType=:et ")
                .append(" and coalesce(e.encounterDate, e.createdAt) between :fd and :td ");
        params.put("vt", ClinicalFindingValueType.VisitDiagnosis);
        params.put("et", PatientEncounterType.OpdVisit);
        params.put("fd", getFromDate());
        params.put("td", getToDate());
        if (institution != null) {
            sb.append(" and e.institution.id=:insId ");
            params.put("insId", institution.getId());
        }
        if (department != null) {
            sb.append(" and d.id=:deptId ");
            params.put("deptId", department.getId());
        }
        if (doctor != null) {
            sb.append(" and (od.id=:docId or crs.id=:docId) ");
            params.put("docId", doctor.getId());
        }
        if (diagnosisId != null) {
            sb.append(" and v.itemValue.id=:dxId ");
            params.put("dxId", diagnosisId);
        }
        return sb.toString();
    }

    private void clearResults() {
        diagnosisCounts = null;
        totalVisits = 0;
        totalDiagnosisRecords = 0;
        selectedDiagnosis = null;
        drillDownVisits = null;
        complaintRows = null;
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

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public List<EmrDiagnosisCountDTO> getDiagnosisCounts() {
        return diagnosisCounts;
    }

    public long getTotalVisits() {
        return totalVisits;
    }

    public long getTotalDiagnosisRecords() {
        return totalDiagnosisRecords;
    }

    public EmrDiagnosisCountDTO getSelectedDiagnosis() {
        return selectedDiagnosis;
    }

    public List<EmrDiagnosisVisitDTO> getDrillDownVisits() {
        return drillDownVisits;
    }

    public List<EmrDiagnosisVisitDTO> getComplaintRows() {
        return complaintRows;
    }
}
