package com.divudi.bean.inward;

import com.divudi.core.data.dto.PatientEncounterDto;
import com.divudi.core.data.dto.RoomChangeReportDto;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.PatientTransferRequest;
import com.divudi.core.facade.PatientTransferRequestFacade;
import java.io.Serializable;
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
public class RoomChangeReportController implements Serializable {

    public RoomChangeReportController() {
    }

    @EJB
    private PatientTransferRequestFacade patientTransferRequestFacade;

    private Institution institution;
    private Institution site;
    private Department department;

    private Department fromWard;
    private Department toWard;

    private Date fromDate;
    private Date toDate;

    private PatientEncounterDto selectedPatient;
    private PatientEncounterDto selectedBhtNo;

    private List<PatientTransferRequest> patientTransferRequests;

    private List<RoomChangeReportDto> roomChangeReportDtoList;

//    public void createRoomChangeReport() {
//        StringBuilder jpql = new StringBuilder();
//        jpql.append(" select r from PatientTransferRequest r ")
//                .append(" where r.retired = false ")
//                .append(" and r.createdAt between :fd and :td ");
//
//        Map<String, Object> params = new HashMap<>();
//        params.put("fd", fromDate);
//        params.put("td", toDate);
//
//        if (selectedPatient != null && selectedPatient.getPatientEncounter() != null) {
//            jpql.append(" and r.admission.patient = :pt ");
//            params.put("pt", selectedPatient.getPatientEncounter().getPatient());
//        }
//
//        if (selectedBhtNo != null && selectedBhtNo.getPatientEncounter() != null) {
//            jpql.append(" and r.admission = :pe ");
//            params.put("pe", selectedBhtNo.getPatientEncounter());
//        }
//
//        if (fromWard != null) {
//            jpql.append(" and r.fromPatientRoom.roomFacilityCharge.room.department = :fw ");
//            params.put("fw", fromWard);
//        }
//
//        if (toWard != null) {
//            jpql.append(" and r.toRoomFacilityCharge.room.department = :tw ");
//            params.put("tw", toWard);
//        }
//
//        patientTransferRequests = patientTransferRequestFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
//
//    }
    public void createRoomChangeReport() {
        StringBuilder jpql = new StringBuilder();
        jpql.append(" select new com.divudi.core.data.dto.RoomChangeReportDto( ")
                .append("     r.id, ")
                .append("     r.status, ")
                .append("     pat.phn, ")
                .append("     per.name, ")
                .append("     per.dob, ")
                .append("     per.sex, ")
                .append("     adm.bhtNo, ")
                .append("     adm.dateOfAdmission, ")
                .append("     fromInst.name, ")
                .append("     fromDept.name, ")
                .append("     rcPerson.name, ")
                .append("     fromRoomEntity.name, ")
                .append("     fromRfc.roomCategory.name, ")//13
                .append("     toInst.name, ")
                .append("     toDept.name, ")
                .append("     rcPerson.name, ")
                .append("     toRoomEntity.name, ")
                .append("     toRfc.roomCategory.name, ")
                .append("     r.acceptedAt, ")
                .append("     abPerson.name, ")
                .append("     r.notes ")
                .append(" ) ")
                .append(" from PatientTransferRequest r ")
                .append(" left join r.admission adm ")
                .append(" left join adm.patient pat ")
                .append(" left join pat.person per ")
                .append(" left join adm.referringConsultant rc ")
                .append(" left join rc.person rcPerson ")
                .append(" left join r.fromPatientRoom fromRoom ")
                .append(" left join fromRoom.roomFacilityCharge fromRfc ")
                .append(" left join fromRfc.room fromRoomEntity ")
                .append(" left join fromRfc.department fromDept ")
                .append(" left join fromDept.institution fromInst ")
                .append(" left join r.toRoomFacilityCharge toRfc ")
                .append(" left join toRfc.room toRoomEntity ")
                .append(" left join toRfc.department toDept ")
                .append(" left join toDept.institution toInst ")
                .append(" left join r.acceptedBy ab ")
                .append(" left join ab.webUserPerson abPerson ")
                .append(" where r.retired = false ")
                .append(" and r.createdAt between :fd and :td ");

        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);

        if (selectedPatient != null && selectedPatient.getPatientEncounter() != null) {
            jpql.append(" and pat = :pt ");
            params.put("pt", selectedPatient.getPatientEncounter().getPatient());
        }

        if (selectedBhtNo != null && selectedBhtNo.getPatientEncounter() != null) {
            jpql.append(" and adm = :pe ");
            params.put("pe", selectedBhtNo.getPatientEncounter());
        }

        if (fromWard != null) {
            jpql.append(" and fromDept = :fw ");
            params.put("fw", fromWard);
        }

        if (toWard != null) {
            jpql.append(" and toDept = :tw ");
            params.put("tw", toWard);
        }

        jpql.append(" order by r.createdAt desc ");

        roomChangeReportDtoList =(List<RoomChangeReportDto>) patientTransferRequestFacade
                .findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
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

    public Department getFromWard() {
        return fromWard;
    }

    public void setFromWard(Department fromWard) {
        this.fromWard = fromWard;
    }

    public Department getToWard() {
        return toWard;
    }

    public void setToWard(Department toWard) {
        this.toWard = toWard;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = com.divudi.core.util.CommonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = com.divudi.core.util.CommonFunctions.getStartOfMonth(new Date());
        }
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

    public PatientEncounterDto getSelectedBhtNo() {
        return selectedBhtNo;
    }

    public void setSelectedBhtNo(PatientEncounterDto selectedBhtNo) {
        this.selectedBhtNo = selectedBhtNo;
    }

    public List<PatientTransferRequest> getPatientTransferRequests() {
        return patientTransferRequests;
    }

    public void setPatientTransferRequests(List<PatientTransferRequest> patientTransferRequests) {
        this.patientTransferRequests = patientTransferRequests;
    }

    public PatientTransferRequestFacade getPatientTransferRequestFacade() {
        return patientTransferRequestFacade;
    }

    public List<RoomChangeReportDto> getRoomChangeReportDtoList() {
        return roomChangeReportDtoList;
    }

    public void setRoomChangeReportDtoList(List<RoomChangeReportDto> roomChangeReportDtoList) {
        this.roomChangeReportDtoList = roomChangeReportDtoList;
    }
}
