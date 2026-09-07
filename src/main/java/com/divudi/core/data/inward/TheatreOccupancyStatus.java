package com.divudi.core.data.inward;

public enum TheatreOccupancyStatus {
    SCHEDULED,
    SENT_TO_THEATRE,
    RECEIVED_IN_THEATRE,
    IN_THEATRE,
    PROCEDURE_COMPLETED,
    IN_RECOVERY,
    RETURNED_TO_WARD,
    CANCELLED;

    // Sequential position used by the visual stepper (1=earliest active step)
    public int getStepNumber() {
        switch (this) {
            case SCHEDULED: return 1;
            case SENT_TO_THEATRE: return 2;
            case RECEIVED_IN_THEATRE: return 3;
            case IN_THEATRE: return 4;
            case PROCEDURE_COMPLETED: return 5;
            case IN_RECOVERY: return 6;
            case RETURNED_TO_WARD: return 7;
            default: return 0;
        }
    }

    // Single source of truth for the display label - previously duplicated
    // (and drifted, e.g. SENT_TO_THEATRE) between SurgeryCostReportController
    // and TheatreRoomCard.
    public String getLabel() {
        switch (this) {
            case SCHEDULED: return "Scheduled";
            case SENT_TO_THEATRE: return "Sent to Theatre";
            case RECEIVED_IN_THEATRE: return "Received in Theatre";
            case IN_THEATRE: return "In Theatre";
            case PROCEDURE_COMPLETED: return "Procedure Completed";
            case IN_RECOVERY: return "In Recovery";
            case RETURNED_TO_WARD: return "Returned to Ward";
            case CANCELLED: return "Cancelled";
            default: return name();
        }
    }
}
