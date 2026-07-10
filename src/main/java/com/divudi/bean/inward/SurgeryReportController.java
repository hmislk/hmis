package com.divudi.bean.inward;

import com.divudi.core.data.BillType;
import com.divudi.core.data.dto.SurgeryReportDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

@Named
@SessionScoped
public class SurgeryReportController implements Serializable {

    public SurgeryReportController() {
    }

    @EJB
    BillFacade billFacade;

    private Institution institution;
    private Institution site;
    private Department department;

    private Date fromDate;
    private Date toDate;

    private Item procedure;
    private RoomFacilityCharge operationTheatreRoom;
    private List<Bill> billList;

    public void processSurgeryStatusReport() {
        reportList = new ArrayList<>();
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both From and To dates.");
            return;
        }
        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("From Date cannot be after To Date.");
            return;
        }

        StringBuilder jpql = new StringBuilder();
        jpql.append(" select new com.divudi.core.data.dto.SurgeryReportDTO( ")
                .append("   b.id, pat.phn, pp.name, pe.dateOfAdmission, i.name, ")
                .append("   d.name, rfc.name, stp.name, rcp.name, pe.id ")
                .append(" ) ")
                .append(" from Bill b ")
                .append(" join b.procedure p ")
                .append(" join p.item i ")
                .append(" join b.patientEncounter pe ")
                .append(" join pe.patient pat ")
                .append(" join pat.person pp ")
                .append(" left join pe.currentPatientRoom cpr ")
                .append(" left join cpr.roomFacilityCharge rfc ")
                .append(" left join b.staff st ")
                .append(" left join st.person stp ")
                .append(" left join pe.referringConsultant rc ")
                .append(" left join rc.person rcp ")
                .append(" join b.department d ")
                .append(" where b.retired = false ")
                .append(" and b.cancelled = false ")
                .append(" and b.billType = :bt ")
                .append(" and b.createdAt between :fd and :td ");

        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.SurgeryBill);
        params.put("fd", com.divudi.core.util.CommonFunctions.getStartOfDay(fromDate));
        params.put("td", com.divudi.core.util.CommonFunctions.getEndOfDay(toDate));

        if (institution != null) {
            jpql.append(" and b.institution = :inst ");
            params.put("inst", institution);
        }
        if (department != null) {
            jpql.append(" and d = :dept ");
            params.put("dept", department);
        }
        if (site != null) {
            jpql.append(" and d.site = :site ");
            params.put("site", site);
        }
        if (procedure != null) {
            jpql.append(" and i = :proc ");
            params.put("proc", procedure);
        }
        if (operationTheatreRoom != null) {
            jpql.append(" and cpr.roomFacilityCharge = :otRoom ");
            params.put("otRoom", operationTheatreRoom);
        }

        jpql.append(" order by i.name ");

        reportList =(List<SurgeryReportDTO>) billFacade.findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP, 1000);

        attachOtStatuses(reportList);
    }

    private void attachOtStatuses(List<SurgeryReportDTO> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<Long> encounterIds = new ArrayList<>();
        for (SurgeryReportDTO r : rows) {
            encounterIds.add(r.getPatientEncounterId());
        }

        String jpql = " select str.admission.id, str.theatreOccupancyStatus "
                + " from PatientTransferRequest str "
                + " where str.retired = false "
                + " and str.admission.id in :ids "
                + " order by str.createdAt ASC";
        Map<String, Object> p = new HashMap<>();
        p.put("ids", encounterIds);

        List<Object[]> statusRows = billFacade.findAggregates(jpql, p, TemporalType.TIMESTAMP);

        Map<Long, String> statusByEncounter = new HashMap<>();
        for (Object[] row : statusRows) {
            Long encounterId = (Long) row[0];
            Object statusObj = row[1];
            String status = statusObj != null ? statusObj.toString() : "";
            statusByEncounter.put(encounterId, status);
        }

        for (SurgeryReportDTO r : rows) {
            r.setOtStatus(statusByEncounter.get(r.getPatientEncounterId()));
        }
    }

    private List<SurgeryReportDTO> reportList;

    public List<SurgeryReportDTO> getReportList() {
        return reportList;
    }

    public void setReportList(List<SurgeryReportDTO> reportList) {
        this.reportList = reportList;
    }

    public List<Bill> getBillList() {
        return billList;
    }

    public void setBillList(List<Bill> billList) {
        this.billList = billList;
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

    public com.divudi.core.entity.Item getProcedure() {
        return procedure;
    }

    public void setProcedure(com.divudi.core.entity.Item procedure) {
        this.procedure = procedure;
    }

    public RoomFacilityCharge getOperationTheatreRoom() {
        return operationTheatreRoom;
    }

    public void setOperationTheatreRoom(RoomFacilityCharge operationTheatreRoom) {
        this.operationTheatreRoom = operationTheatreRoom;
    }

}
