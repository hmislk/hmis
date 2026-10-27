package com.divudi.bean.inward;

import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.PatientRoom;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * PatientTransferController.stampAcceptedRoomTiming() (Issue: room-charge accept-time fix).
 *
 * <p>Room-charge billing reads PatientRoom.admittedAt/addmittedBy. This helper is what
 * acceptTransfer() calls to correct those fields to the real accept time when a nurse
 * accepts a patient into a room that was already created at admission-save time.
 */
class PatientTransferControllerAcceptedRoomTimingTest {

    @Test
    void overwritesAdmittedAtWithTheAcceptedTime() {
        PatientRoom room = new PatientRoom();
        Date originalAdmissionTime = new Date(1_000_000_000_000L);
        room.setAdmittedAt(originalAdmissionTime);

        Date acceptedAt = new Date(1_000_000_600_000L);
        PatientTransferController.stampAcceptedRoomTiming(room, acceptedAt, new WebUser());

        assertEquals(acceptedAt, room.getAdmittedAt());
        assertNotEquals(originalAdmissionTime, room.getAdmittedAt());
    }

    @Test
    void setsAddmittedByToTheAcceptingUser() {
        PatientRoom room = new PatientRoom();
        WebUser acceptingNurse = new WebUser();

        PatientTransferController.stampAcceptedRoomTiming(room, new Date(), acceptingNurse);

        assertSame(acceptingNurse, room.getAddmittedBy());
    }
}
