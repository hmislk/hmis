package com.divudi.core.data.dto;

import com.divudi.core.data.Title;
import java.io.Serializable;
import java.util.Date;

/**
 * One row of the pending inward appointments table on the Admit page (#25707).
 * Carries ids and plain values only, so the list is built without loading
 * entities per row.
 */
public class PendingAppointmentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long appointmentId;
    private Long billId;
    private String appointmentNumber;
    private Date appointmentDate;
    private Date appointmentTimeFrom;
    private Title patientTitle;
    private String patientName;
    private String patientPhone;
    private String procedureName;
    private double billTotal;
    /** Set after the main query, from the reservation (room appointments only). */
    private String roomName;

    public PendingAppointmentDTO() {
    }

    public PendingAppointmentDTO(Long appointmentId, Long billId, String appointmentNumber,
            Date appointmentDate, Date appointmentTimeFrom, Title patientTitle, String patientName,
            String patientPhone, String procedureName, double billTotal) {
        this.appointmentId = appointmentId;
        this.billId = billId;
        this.appointmentNumber = appointmentNumber;
        this.appointmentDate = appointmentDate;
        this.appointmentTimeFrom = appointmentTimeFrom;
        this.patientTitle = patientTitle;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.procedureName = procedureName;
        this.billTotal = billTotal;
    }

    public boolean isRoomAppointment() {
        return roomName != null && !roomName.trim().isEmpty();
    }

    public String getTypeLabel() {
        return isRoomAppointment() ? "Room" : "Procedure";
    }

    public boolean isPaid() {
        return billTotal > 0;
    }

    public String getPaidLabel() {
        return isPaid() ? "Paid" : "Not Paid";
    }

    public String getPatientNameWithTitle() {
        String t = patientTitle != null ? patientTitle.getLabel() : null;
        String n = patientName != null ? patientName : "";
        return t != null && !t.isEmpty() ? t + " " + n : n;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public Long getBillId() {
        return billId;
    }

    public String getAppointmentNumber() {
        return appointmentNumber;
    }

    public Date getAppointmentDate() {
        return appointmentDate;
    }

    public Date getAppointmentTimeFrom() {
        return appointmentTimeFrom;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public double getBillTotal() {
        return billTotal;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
}
