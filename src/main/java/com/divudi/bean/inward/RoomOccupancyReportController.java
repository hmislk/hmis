package com.divudi.bean.inward;

import com.divudi.core.data.dto.PatientEncounterDto;
import com.divudi.core.data.dto.RoomOccupancyReportDto;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.PatientRoomFacade;
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
import java.util.stream.Collectors;

@Named
@SessionScoped
public class RoomOccupancyReportController implements Serializable {

    @EJB
    private PatientRoomFacade patientRoomFacade;

    private Date fromDate;
    private Date toDate;

    private PatientEncounterDto selectedPatient;
    private Department ward;
    private RoomFacilityCharge roomFacilityCharge;
    private AdmissionType admissionType;
    private RoomCategory roomCategory;
    private Institution institution;
    private Institution site;

    private String reportType = "Detail"; // "Detail", "Summary By Room Category", "Summary By Admission Type"
    private String summaryType = "Category"; // Internal toggle for which summary to render

    private List<RoomOccupancyReportDto> detailDtos;
    private List<RoomOccupancyReportDto> summaryDtos;

    public RoomOccupancyReportController() {
    }

    public void processReport() {
        if ("Detail".equals(reportType)) {
            processDetailReport();
        } else {
            processSummaryReport();
        }
    }

    private void processDetailReport() {
        detailDtos = new ArrayList<>();
        summaryDtos = new ArrayList<>();

        StringBuilder jpql = new StringBuilder();
        jpql.append(" select new com.divudi.core.data.dto.RoomOccupancyReportDto( ")
            .append("     pat.phn, ")
            .append("     per.name, ")
            .append("     adm.dateOfAdmission, ")
            .append("     adm.dateOfDischarge, ")
            .append("     rc.name, ")
            .append("     at.name, ")
            .append("     dept.name, ")
            .append("     r.name, ")
            .append("     pr.admittedAt, ")
            .append("     pr.dischargedAt ")
            .append(" ) ")
            .append(" from PatientRoom pr ")
            .append(" left join pr.patientEncounter adm ")
            .append(" left join adm.patient pat ")
            .append(" left join pat.person per ")
            .append(" left join pr.roomFacilityCharge rfc ")
            .append(" left join rfc.room r ")
            .append(" left join rfc.roomCategory rc ")
            .append(" left join rfc.department dept ")
            .append(" left join dept.institution inst ")
            .append(" left join adm.admissionType at ")
            .append(" where pr.retired = false ")
            .append(" and pr.dischargedAt = true ");

        Map<String, Object> params = new HashMap<>();

        // Date filter on dischargedAt (if discharged in range) or if still admitted (admittedAt before range end)
        jpql.append(" and (pr.dischargedAt between :fd and :td or (pr.dischargedAt is null and pr.admittedAt <= :td)) ");
        params.put("fd", fromDate);
        params.put("td", toDate);

        if (selectedPatient != null && selectedPatient.getPatientEncounter() != null) {
            jpql.append(" and pat = :pt ");
            params.put("pt", selectedPatient.getPatientEncounter().getPatient());
        }

        if (ward != null) {
            jpql.append(" and dept = :wd ");
            params.put("wd", ward);
        }

        if (roomFacilityCharge != null) {
            jpql.append(" and rfc = :rfc ");
            params.put("rfc", roomFacilityCharge);
        }

        if (admissionType != null) {
            jpql.append(" and at = :adt ");
            params.put("adt", admissionType);
        }

        if (roomCategory != null) {
            jpql.append(" and rc = :rc ");
            params.put("rc", roomCategory);
        }

        if (site != null) {
            jpql.append(" and dept.institution = :st ");
            params.put("st", site);
        } else if (institution != null) {
            jpql.append(" and dept.institution = :ins ");
            params.put("ins", institution);
        }

        jpql.append(" order by pr.admittedAt desc ");
        detailDtos = (List<RoomOccupancyReportDto>) patientRoomFacade
                .findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        
        System.out.println("This is msg " + detailDtos.size());

        
    }

    private void processSummaryReport() {
        processDetailReport(); // Fetch all raw data first
        
        summaryDtos = new ArrayList<>();
        
        if (detailDtos == null || detailDtos.isEmpty()) {
            return;
        }

        if ("Summary By Room Category".equals(reportType)) {
            summaryType = "Category";
            Map<String, Double> map = detailDtos.stream()
                .filter(dto -> dto.getRoomCategory() != null)
                .collect(Collectors.groupingBy(RoomOccupancyReportDto::getRoomCategory, Collectors.summingDouble(RoomOccupancyReportDto::getOccupancyDays)));
            
            for (Map.Entry<String, Double> entry : map.entrySet()) {
                RoomOccupancyReportDto dto = new RoomOccupancyReportDto();
                dto.setGroupName(entry.getKey());
                dto.setTotalOccupancyDays(entry.getValue());
                summaryDtos.add(dto);
            }
        } else if ("Summary By Admission Type".equals(reportType)) {
            summaryType = "AdmissionType";
            Map<String, Double> map = detailDtos.stream()
                .filter(dto -> dto.getAdmissionType() != null)
                .collect(Collectors.groupingBy(RoomOccupancyReportDto::getAdmissionType, Collectors.summingDouble(RoomOccupancyReportDto::getOccupancyDays)));
            
            for (Map.Entry<String, Double> entry : map.entrySet()) {
                RoomOccupancyReportDto dto = new RoomOccupancyReportDto();
                dto.setGroupName(entry.getKey());
                dto.setTotalOccupancyDays(entry.getValue());
                summaryDtos.add(dto);
            }
        }
        
        // Sort
        summaryDtos.sort((d1, d2) -> d1.getGroupName().compareToIgnoreCase(d2.getGroupName()));
    }
    
    // Getters and Setters...

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

    public PatientEncounterDto getSelectedPatient() {
        return selectedPatient;
    }

    public void setSelectedPatient(PatientEncounterDto selectedPatient) {
        this.selectedPatient = selectedPatient;
    }

    public Department getWard() {
        return ward;
    }

    public void setWard(Department ward) {
        this.ward = ward;
    }

    public RoomFacilityCharge getRoomFacilityCharge() {
        return roomFacilityCharge;
    }

    public void setRoomFacilityCharge(RoomFacilityCharge roomFacilityCharge) {
        this.roomFacilityCharge = roomFacilityCharge;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public RoomCategory getRoomCategory() {
        return roomCategory;
    }

    public void setRoomCategory(RoomCategory roomCategory) {
        this.roomCategory = roomCategory;
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

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getSummaryType() {
        return summaryType;
    }

    public void setSummaryType(String summaryType) {
        this.summaryType = summaryType;
    }

    public List<RoomOccupancyReportDto> getDetailDtos() {
        return detailDtos;
    }

    public void setDetailDtos(List<RoomOccupancyReportDto> detailDtos) {
        this.detailDtos = detailDtos;
    }

    public List<RoomOccupancyReportDto> getSummaryDtos() {
        return summaryDtos;
    }

    public void setSummaryDtos(List<RoomOccupancyReportDto> summaryDtos) {
        this.summaryDtos = summaryDtos;
    }
}
