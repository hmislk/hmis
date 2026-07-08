package com.divudi.bean.inward;

import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.DepartmentDto;
import com.divudi.core.data.dto.HospitalCensusSummaryDto;
import com.divudi.core.data.dto.HospitalCensusDetailDto;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.facade.RoomFacilityChargeFacade;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HIGH-PERFORMANCE Hospital Census Report Controller.
 *
 * OPTIMIZATION STRATEGY: ====================== BEFORE : 8 queries × N
 * departments = O(N) roundtrips. AFTER : 3 fixed queries regardless of
 * department count = O(1) roundtrips.
 *
 * 1. SINGLE GROUP-BY query for all bed counts (replaces N × countTotalBeds). 2.
 * SINGLE GROUP-BY aggregation for all PatientRoom metrics (replaces N × 5 count
 * queries + N × 2 entity-fetch calls). 3. SINGLE DTO-projection query for
 * detail rows (replaces full entity hydration + lazy-chain navigation in
 * buildWardDetail). 4. Department joins use the numeric PK (d.id), never the
 * name VARCHAR.
 *
 * @author Senior Software Engineer - Performance Team
 */
@Named
@SessionScoped
public class InwardManagementReportController implements Serializable {

    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private RoomFacilityChargeFacade roomFacilityChargeFacade;

    private Institution institution;
    private Institution site;
    private Department department;
    private Date fromDate;
    private Date toDate;
    private RoomFacilityCharge ward;
    private String reportType;

    private List<HospitalCensusSummaryDto> hospitalCensusSummaryDtos;
    private List<HospitalCensusDetailDto> hospitalCensusDetailDtos;
    private List<DepartmentDto> departmentList;

    public InwardManagementReportController() {
    }

    public void createHospitalCensusReport() {

        hospitalCensusSummaryDtos = new ArrayList<>();
        hospitalCensusDetailDtos = new ArrayList<>();

        departmentList = fetchInwardDepartments();
        if (departmentList == null || departmentList.isEmpty()) {
            return;
        }

        // Collect department ids
        List<Long> deptIds = collectDeptIds(departmentList);

        // Query 1 : Bed counts
        Map<Long, Long> bedCountByDept = fetchBedCountsByDept(deptIds);

        // Query 2 : Census metrics
        Map<Long, long[]> metricsByDept = fetchPatientRoomMetrics(deptIds);

        // Build Summary DTOs
        for (DepartmentDto dept : departmentList) {

            long deptId = dept.getId();

            long totalBeds = bedCountByDept.getOrDefault(deptId, 0L);
            long[] m = metricsByDept.getOrDefault(deptId, new long[9]);

            long currentPresent = m[0];
            long previousDaysTotal = m[1];
            long newAdmissions = m[2];
            long transferIn = m[3];
            long transferOut = m[4];
            long normalDischarges = m[5];
            long lama = m[6];
            long deaths = m[7];
            long totalFinalDischarges = m[8];

            long others = Math.max(0, totalFinalDischarges - normalDischarges - lama - deaths);

            HospitalCensusSummaryDto summary = new HospitalCensusSummaryDto();

            summary.setWard(dept.getName());
            summary.setTotalBeds(totalBeds);
            summary.setOpenBeds(totalBeds - currentPresent);
            summary.setPreviousDaysTotal(previousDaysTotal);
            summary.setNewAdmissions(newAdmissions);
            summary.setTransferIn(transferIn);
            summary.setTransferOut(transferOut);
            summary.setMarkedForDischarge(0);
            summary.setNormalDischarges(normalDischarges);
            summary.setLama(lama);
            summary.setDeaths(deaths);
            summary.setOthers(others);
            summary.setTotalPresent(currentPresent);

            if (totalBeds > 0) {
                summary.setBedOccupancyRate((currentPresent * 100.0) / totalBeds);
            } else {
                summary.setBedOccupancyRate(0.0);
            }

            hospitalCensusSummaryDtos.add(summary);
        }

        // Load details ONLY for Detail report
        if (!"Summary".equalsIgnoreCase(reportType)) {
            hospitalCensusDetailDtos = fetchDetailDtos(deptIds);
        }
    }

    /**
     * Fetches all active inward departments filtered by the user-selected
     * institution and/or department. Returns lightweight DTOs only.
     */
    private List<DepartmentDto> fetchInwardDepartments() {
        StringBuilder jpql = new StringBuilder()
                .append(" select new com.divudi.core.data.dto.DepartmentDto(d.id, d.name) ")
                .append(" from Department d ")
                .append(" where d.retired = false ")
                .append(" and d.departmentType IN :dt ");

        Map<String, Object> params = new HashMap<>();
        params.put("dt", Arrays.asList(DepartmentType.Inward));

        if (institution != null) {
            jpql.append(" and d.institution = :ins ");
            params.put("ins", institution);
        }
        if (department != null) {
            jpql.append(" and d = :dept ");
            params.put("dept", department);
        }
        jpql.append(" order by d.name ");

        return (List<DepartmentDto>) departmentFacade.findDTOsByJpql(jpql.toString(), params);
    }

    /**
     * ONE query: COUNT of active beds grouped by department id. Replaces N ×
     * countTotalBeds().
     *
     * @return map of departmentId → bed count
     */
    private Map<Long, Long> fetchBedCountsByDept(List<Long> deptIds) {
        if (deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String jpql = "select r.department.id, count(r) "
                + "from RoomFacilityCharge r "
                + "where r.retired = false "
                + "and r.department.id in :deptIds "
                + "group by r.department.id";

        Map<String, Object> params = new HashMap<>();
        params.put("deptIds", deptIds);

        List<?> rows = roomFacilityChargeFacade.findDTOsByJpql(jpql, params);
        Map<Long, Long> result = new HashMap<>();
        for (Object row : rows) {
            Object[] cols = (Object[]) row;
            result.put(((Number) cols[0]).longValue(), ((Number) cols[1]).longValue());
        }
        return result;
    }

    /**
     * ONE query: all PatientRoom census metrics grouped by department id.
     *
     * Returns a map of departmentId → long[9] where: [0] currentPresent
     * (admitted as-of toDate) [1] previousDaysTotal (admitted as-of fromDate)
     * [2] newAdmissions (first admission, within [fromDate, toDate]) [3]
     * transferIn (room transfer in, within period) [4] transferOut (room
     * transfer out, within period) [5] normalDischarges (final discharge; dc IS
     * NULL or name contains routine/normal) [6] lama (final discharge; dc name
     * contains lama/against) [7] deaths (final discharge; dc name contains
     * death/dead/died) [8] totalFinalDischarges (all final discharges; used to
     * compute 'others' in Java)
     *
     * Design notes: - Explicit LEFT JOINs on patientEncounter and
     * dischargeCondition are required. Implicit path navigation
     * (pr.patientEncounter.dischargeCondition) creates inner joins, making "dc
     * IS NULL" always false and skewing normal-discharge counts. -
     * EclipseLink's JPQL parser does not support NOT LIKE inside CASE WHEN
     * conditional expressions. The 'others' category is therefore computed in
     * Java as: others = totalFinalDischarges - normalDischarges - lama -
     * deaths.
     */
    private Map<Long, long[]> fetchPatientRoomMetrics(List<Long> deptIds) {

        if (deptIds == null || deptIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String jpql
                = "select d.id, "
                // 0 Current Present
                + "(select count(pr1) "
                + " from PatientRoom pr1 "
                + " where pr1.retired=false "
                + " and pr1.roomFacilityCharge.department.id=d.id "
                + " and pr1.admittedAt<=:td "
                + " and (pr1.dischargedAt is null or pr1.dischargedAt>:td)), "
                // 1 Previous Day Total
                + "(select count(pr2) "
                + " from PatientRoom pr2 "
                + " where pr2.retired=false "
                + " and pr2.roomFacilityCharge.department.id=d.id "
                + " and pr2.admittedAt<=:fd "
                + " and (pr2.dischargedAt is null or pr2.dischargedAt>:fd)), "
                // 2 New Admission
                + "(select count(pr3) "
                + " from PatientRoom pr3 "
                + " where pr3.retired=false "
                + " and pr3.roomFacilityCharge.department.id=d.id "
                + " and pr3.previousRoom is null "
                + " and pr3.admittedAt>=:fd "
                + " and pr3.admittedAt<=:td), "
                // 3 Transfer In
                + "(select count(pr4) "
                + " from PatientRoom pr4 "
                + " where pr4.retired=false "
                + " and pr4.roomFacilityCharge.department.id=d.id "
                + " and pr4.previousRoom is not null "
                + " and pr4.admittedAt>=:fd "
                + " and pr4.admittedAt<=:td), "
                // 4 Transfer Out
                + "(select count(pr5) "
                + " from PatientRoom pr5 "
                + " where pr5.retired=false "
                + " and pr5.roomFacilityCharge.department.id=d.id "
                + " and pr5.nextRoom is not null "
                + " and pr5.dischargedAt>=:fd "
                + " and pr5.dischargedAt<=:td), "
                // 5 Normal Discharge
                + "(select count(pr6) "
                + " from PatientRoom pr6 "
                + " left join pr6.patientEncounter pe6 "
                + " left join pe6.dischargeCondition dc6 "
                + " where pr6.retired=false "
                + " and pr6.roomFacilityCharge.department.id=d.id "
                + " and pr6.nextRoom is null "
                + " and pr6.dischargedAt>=:fd "
                + " and pr6.dischargedAt<=:td "
                + " and (dc6 is null "
                + "      or lower(dc6.name) like '%routine%' "
                + "      or lower(dc6.name) like '%normal%')), "
                // 6 LAMA
                + "(select count(pr7) "
                + " from PatientRoom pr7 "
                + " left join pr7.patientEncounter pe7 "
                + " left join pe7.dischargeCondition dc7 "
                + " where pr7.retired=false "
                + " and pr7.roomFacilityCharge.department.id=d.id "
                + " and pr7.nextRoom is null "
                + " and pr7.dischargedAt>=:fd "
                + " and pr7.dischargedAt<=:td "
                + " and dc7 is not null "
                + " and (lower(dc7.name) like '%lama%' "
                + "      or lower(dc7.name) like '%against%')), "
                // 7 Death
                + "(select count(pr8) "
                + " from PatientRoom pr8 "
                + " left join pr8.patientEncounter pe8 "
                + " left join pe8.dischargeCondition dc8 "
                + " where pr8.retired=false "
                + " and pr8.roomFacilityCharge.department.id=d.id "
                + " and pr8.nextRoom is null "
                + " and pr8.dischargedAt>=:fd "
                + " and pr8.dischargedAt<=:td "
                + " and dc8 is not null "
                + " and (lower(dc8.name) like '%death%' "
                + "      or lower(dc8.name) like '%dead%' "
                + "      or lower(dc8.name) like '%died%')), "
                // 8 Total Final Discharge
                + "(select count(pr9) "
                + " from PatientRoom pr9 "
                + " where pr9.retired=false "
                + " and pr9.roomFacilityCharge.department.id=d.id "
                + " and pr9.nextRoom is null "
                + " and pr9.dischargedAt>=:fd "
                + " and pr9.dischargedAt<=:td) "
                + "from Department d "
                + "where d.id in :deptIds";

        Map<String, Object> params = new HashMap<>();
        params.put("deptIds", deptIds);
        params.put("fd", fromDate);
        params.put("td", toDate);

        List<Object[]> rows = (List<Object[]>) departmentFacade.findDTOsByJpql(jpql, params);

        Map<Long, long[]> result = new HashMap<>();

        for (Object[] row : rows) {

            Long deptId = ((Number) row[0]).longValue();

            long[] metrics = new long[9];

            for (int i = 0; i < 9; i++) {
                metrics[i] = row[i + 1] == null ? 0L : ((Number) row[i + 1]).longValue();
            }

            result.put(deptId, metrics);
        }

        return result;
    }

    /**
     * ONE query: DTO-projection for the detail grid — no entity hydration.
     *
     * Fetches only the 9 scalar columns needed by HospitalCensusDetailDto. Age
     * is computed inside the DTO constructor from dob to keep JPQL clean.
     *
     * Replaces N × (fetchCurrentPatients entity load + buildWardDetail loop +
     * lazy PatientEncounter / Patient / Person navigation).
     */
    @SuppressWarnings("unchecked")
    private List<HospitalCensusDetailDto> fetchDetailDtos(List<Long> deptIds) {
        if (deptIds.isEmpty()) {
            return Collections.emptyList();
        }
        String jpql
                = "select new com.divudi.core.data.dto.HospitalCensusDetailDto("
                + "  d.name, " // area
                + "  pat.phn, " // mrn
                + "  per.name, " // personName
                + "  per.dob, " // dob  (age computed in ctor)
                + "  per.sex, " // sex  (enum)
                + "  pe.dateOfAdmission, " // doa
                + "  rfc.name, " // bedNo
                + "  pe.discharged, " // discharged flag
                + "  doc_per.name" // consultant name (nullable)
                + ") "
                + "from PatientRoom pr "
                + "join pr.roomFacilityCharge rfc "
                + "join rfc.department d "
                + "join pr.patientEncounter pe "
                + "join pe.patient pat "
                + "join pat.person per "
                + "left join pe.referringDoctor doc "
                + "left join doc.person doc_per "
                + "where pr.retired = false "
                + "and d.id in :deptIds "
                + "and pr.admittedAt <= :td "
                + "and (pr.dischargedAt is null or pr.dischargedAt > :td) "
                + "order by d.name, pat.phn";

        Map<String, Object> params = new HashMap<>();
        params.put("deptIds", deptIds);
        params.put("td", toDate);

        return (List<HospitalCensusDetailDto>) departmentFacade.findDTOsByJpql(jpql, params);
    }

    // =========================================================================
    // Private utilities
    // =========================================================================
    private boolean isSummaryOnly() {
        return "Summary".equals(reportType);
    }

    private static List<Long> collectDeptIds(List<DepartmentDto> departments) {
        List<Long> ids = new ArrayList<>(departments.size());
        for (DepartmentDto d : departments) {
            ids.add(d.getId());
        }
        return ids;
    }

    // =========================================================================
    // Getters & Setters
    // =========================================================================
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

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = com.divudi.core.util.CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = com.divudi.core.util.CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public RoomFacilityCharge getWard() {
        return ward;
    }

    public void setWard(RoomFacilityCharge ward) {
        this.ward = ward;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public List<DepartmentDto> getDepartmentList() {
        return departmentList;
    }

    public void setDepartmentList(List<DepartmentDto> departmentList) {
        this.departmentList = departmentList;
    }

    public List<HospitalCensusSummaryDto> getHospitalCensusSummaryDtos() {
        return hospitalCensusSummaryDtos;
    }

    public void setHospitalCensusSummaryDtos(List<HospitalCensusSummaryDto> hospitalCensusSummaryDtos) {
        this.hospitalCensusSummaryDtos = hospitalCensusSummaryDtos;
    }

    public List<HospitalCensusDetailDto> getHospitalCensusDetailDtos() {
        return hospitalCensusDetailDtos;
    }

    public void setHospitalCensusDetailDtos(List<HospitalCensusDetailDto> hospitalCensusDetailDtos) {
        this.hospitalCensusDetailDtos = hospitalCensusDetailDtos;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }
}
