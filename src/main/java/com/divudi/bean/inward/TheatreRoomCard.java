package com.divudi.bean.inward;

import com.divudi.core.data.inward.TheatreOccupancyStatus;
import com.divudi.core.entity.inward.PatientTransferRequest;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TheatreRoomCard {

    private RoomFacilityCharge theatreRoom;
    private PatientTransferRequest currentRequest;

    public TheatreRoomCard(RoomFacilityCharge theatreRoom, PatientTransferRequest currentRequest) {
        this.theatreRoom = theatreRoom;
        this.currentRequest = currentRequest;
    }

    public boolean isOccupied() {
        return currentRequest != null;
    }

    public long getMinutesInCurrentStatus() {
        if (currentRequest == null) {
            return 0;
        }
        Date since = null;
        TheatreOccupancyStatus status = currentRequest.getTheatreOccupancyStatus();
        if (status == null) {
            since = currentRequest.getAcceptedAt();
        } else {
            switch (status) {
                case IN_THEATRE:
                    since = currentRequest.getProcedureStartAt();
                    break;
                case PROCEDURE_COMPLETED:
                    since = currentRequest.getProcedureEndAt();
                    break;
                case IN_RECOVERY:
                    since = currentRequest.getInRecoveryAt();
                    break;
                default:
                    since = currentRequest.getAcceptedAt();
            }
        }
        if (since == null) {
            since = currentRequest.getAcceptedAt();
        }
        if (since == null) {
            return 0;
        }
        long diffMs = new Date().getTime() - since.getTime();
        return TimeUnit.MILLISECONDS.toMinutes(diffMs);
    }

    public String getStatusLabel() {
        if (currentRequest == null) {
            return "Available";
        }
        TheatreOccupancyStatus status = currentRequest.getTheatreOccupancyStatus();
        if (status == null) {
            return "In Transit";
        }
        switch (status) {
            case SENT_TO_THEATRE: return "Sent — Awaiting Acceptance";
            case RECEIVED_IN_THEATRE: return "Received in Theatre";
            case IN_THEATRE: return "In Theatre";
            case PROCEDURE_COMPLETED: return "Procedure Completed";
            case IN_RECOVERY: return "In Recovery";
            case RETURNED_TO_WARD: return "Returned to Ward";
            default: return status.name();
        }
    }

    public String getCardStyleClass() {
        if (currentRequest == null) {
            return "border-success";
        }
        TheatreOccupancyStatus status = currentRequest.getTheatreOccupancyStatus();
        if (status == null) {
            return "border-secondary";
        }
        switch (status) {
            case SENT_TO_THEATRE: return "border-warning";
            case RECEIVED_IN_THEATRE: return "border-info";
            case IN_THEATRE: return "border-danger";
            case PROCEDURE_COMPLETED: return "border-primary";
            case IN_RECOVERY: return "border-warning";
            default: return "border-secondary";
        }
    }

    public String getStatusBadgeClass() {
        if (currentRequest == null) {
            return "bg-success";
        }
        TheatreOccupancyStatus status = currentRequest.getTheatreOccupancyStatus();
        if (status == null) {
            return "bg-secondary";
        }
        switch (status) {
            case SENT_TO_THEATRE: return "bg-warning text-dark";
            case RECEIVED_IN_THEATRE: return "bg-info text-dark";
            case IN_THEATRE: return "bg-danger";
            case PROCEDURE_COMPLETED: return "bg-primary";
            case IN_RECOVERY: return "bg-warning text-dark";
            default: return "bg-secondary";
        }
    }

    public RoomFacilityCharge getTheatreRoom() {
        return theatreRoom;
    }

    public PatientTransferRequest getCurrentRequest() {
        return currentRequest;
    }
}
