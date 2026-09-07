package com.divudi.core.data.dto;

import com.divudi.core.data.Sex;
import com.divudi.core.data.inward.TransferRequestStatus;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Date;

public class RoomChangeReportDto implements Serializable {

    private Long transferRequestId;
    private TransferRequestStatus status;

    private String patientPhn;
    private String patientName;
    private Date patientDob;
    private String patientSex;
    private String bhtNo;
    private Date dateOfAdmission;

    private String fromHospital;
    private String fromDepartment;
    private String fromPrimaryConsultant;
    private String fromWardRoomName;
    private String fromBedType;

    private String toHospital;
    private String toDepartment;
    private String toPrimaryConsultant;
    private String toWardRoomName;
    private String toBedType;

    private Date acceptedAt;
    private String acceptedByName;
    private String notes;

    public RoomChangeReportDto(Long transferRequestId,
            TransferRequestStatus status,
            String patientPhn,
            String patientName,
            Date patientDob,
            String patientSex,
            String bhtNo,
            Date dateOfAdmission,
            String fromHospital,
            String fromDepartment,
            String fromPrimaryConsultant,
            String fromWardRoomName,
            String fromBedType,
            String toHospital,
            String toDepartment,
            String toPrimaryConsultant,
            String toWardRoomName,
            String toBedType,
            Date acceptedAt,
            String acceptedByName,
            String notes) {
        this.transferRequestId = transferRequestId;
        this.status = status;
        this.patientPhn = patientPhn;
        this.patientName = patientName;
        this.patientDob = patientDob;
        this.patientSex = patientSex;
        this.bhtNo = bhtNo;
        this.dateOfAdmission = dateOfAdmission;
        this.fromHospital = fromHospital;
        this.fromDepartment = fromDepartment;
        this.fromPrimaryConsultant = fromPrimaryConsultant;
        this.fromWardRoomName = fromWardRoomName;
        this.fromBedType = fromBedType;
        this.toHospital = toHospital;
        this.toDepartment = toDepartment;
        this.toPrimaryConsultant = toPrimaryConsultant;
        this.toWardRoomName = toWardRoomName;
        this.toBedType = toBedType;
        this.acceptedAt = acceptedAt;
        this.acceptedByName = acceptedByName;
        this.notes = notes;
    }


    public RoomChangeReportDto(Long transferRequestId,
            TransferRequestStatus status,
            String patientPhn,
            String patientName,
            Date patientDob,
            Sex patientSex,
            String bhtNo,
            Date dateOfAdmission,
            String fromHospital,
            String fromDepartment,
            String fromPrimaryConsultant,
            String fromWardRoomName,
            String fromBedType,
            String toHospital,
            String toDepartment,
            String toPrimaryConsultant,
            String toWardRoomName,
            String toBedType,
            Date acceptedAt,
            String acceptedByName,
            String notes) {
        this(transferRequestId, status, patientPhn, patientName, patientDob,
                patientSex != null ? patientSex.name() : null,
                bhtNo, dateOfAdmission,
                fromHospital, fromDepartment, fromPrimaryConsultant, fromWardRoomName, fromBedType,
                toHospital, toDepartment, toPrimaryConsultant, toWardRoomName, toBedType,
                acceptedAt, acceptedByName, notes);
    }


    public Integer getPatientAge() {
        if (patientDob == null) {
            return null;
        }
        LocalDate dob = patientDob.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
        return Period.between(dob, LocalDate.now()).getYears();
    }

    public Long getTransferRequestId() {
        return transferRequestId;
    }

    public TransferRequestStatus getStatus() {
        return status;
    }

    public String getPatientPhn() {
        return patientPhn;
    }

    public String getPatientName() {
        return patientName;
    }

    public Date getPatientDob() {
        return patientDob;
    }

    public String getPatientSex() {
        return patientSex;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public Date getDateOfAdmission() {
        return dateOfAdmission;
    }

    public String getFromHospital() {
        return fromHospital;
    }

    public String getFromDepartment() {
        return fromDepartment;
    }

    public String getFromPrimaryConsultant() {
        return fromPrimaryConsultant;
    }

    public String getFromWardRoomName() {
        return fromWardRoomName;
    }

    public String getFromBedType() {
        return fromBedType;
    }

    public String getToHospital() {
        return toHospital;
    }

    public String getToDepartment() {
        return toDepartment;
    }

    public String getToPrimaryConsultant() {
        return toPrimaryConsultant;
    }

    public String getToWardRoomName() {
        return toWardRoomName;
    }

    public String getToBedType() {
        return toBedType;
    }

    public Date getAcceptedAt() {
        return acceptedAt;
    }

    public String getAcceptedByName() {
        return acceptedByName;
    }

    public String getNotes() {
        return notes;
    }
}
