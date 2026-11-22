package com.divudi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.divudi.core.data.PatientComplaintStatus;
import com.divudi.core.data.dto.PatientComplaintPortalRowDto;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.Test;

class PatientComplaintServiceTest {

    private static final PatientComplaintStatus NEW = PatientComplaintStatus.NEW;
    private static final PatientComplaintStatus IN_PROGRESS = PatientComplaintStatus.IN_PROGRESS;
    private static final PatientComplaintStatus CLOSED = PatientComplaintStatus.CLOSED;

    @Test
    void allowedTransitionsAreNewToInProgressToClosedAndReopen() {
        assertTrue(NEW.canMoveTo(IN_PROGRESS));
        assertTrue(IN_PROGRESS.canMoveTo(CLOSED));
        assertTrue(CLOSED.canMoveTo(IN_PROGRESS));
        assertFalse(NEW.canMoveTo(CLOSED));
        assertFalse(NEW.canMoveTo(NEW));
        assertFalse(IN_PROGRESS.canMoveTo(NEW));
        assertFalse(CLOSED.canMoveTo(NEW));
        assertFalse(CLOSED.canMoveTo(CLOSED));
        assertFalse(NEW.canMoveTo(null));
    }

    @Test
    void startingWorkNeedsNoResponse() {
        PatientComplaintService.checkStatusChange(NEW, IN_PROGRESS, null, null);
    }

    @Test
    void closingNeedsAResponse() {
        assertThrows(PatientComplaintException.class,
                () -> PatientComplaintService.checkStatusChange(IN_PROGRESS, CLOSED, null, null));
        assertThrows(PatientComplaintException.class,
                () -> PatientComplaintService.checkStatusChange(IN_PROGRESS, CLOSED, "   ", null));
        PatientComplaintService.checkStatusChange(IN_PROGRESS, CLOSED, "We have fixed it.", null);
    }

    @Test
    void reopeningNeedsANote() {
        assertThrows(PatientComplaintException.class,
                () -> PatientComplaintService.checkStatusChange(CLOSED, IN_PROGRESS, "x", null));
        assertThrows(PatientComplaintException.class,
                () -> PatientComplaintService.checkStatusChange(CLOSED, IN_PROGRESS, "x", " "));
        PatientComplaintService.checkStatusChange(CLOSED, IN_PROGRESS, null, "Patient is not satisfied");
    }

    @Test
    void skippingAStepIsRefused() {
        assertThrows(PatientComplaintException.class,
                () -> PatientComplaintService.checkStatusChange(NEW, CLOSED, "done", "n"));
        assertThrows(PatientComplaintException.class,
                () -> PatientComplaintService.checkStatusChange(IN_PROGRESS, NEW, "done", "n"));
        assertThrows(PatientComplaintException.class,
                () -> PatientComplaintService.checkStatusChange(null, IN_PROGRESS, null, null));
        assertThrows(PatientComplaintException.class,
                () -> PatientComplaintService.checkStatusChange(NEW, null, null, null));
    }

    @Test
    void complaintIsOwnedOnlyByAPatientOfTheSession() {
        assertTrue(PatientComplaintService.isOwnedByAny(5L, Arrays.asList(4L, 5L)));
        assertFalse(PatientComplaintService.isOwnedByAny(6L, Arrays.asList(4L, 5L)));
        assertFalse(PatientComplaintService.isOwnedByAny(null, Arrays.asList(4L, 5L)));
        assertFalse(PatientComplaintService.isOwnedByAny(5L, Collections.<Long>emptyList()));
        assertFalse(PatientComplaintService.isOwnedByAny(5L, null));
    }

    @Test
    void portalRowShowsResponseOnlyWhenClosed() {
        Date d = new Date();
        PatientComplaintPortalRowDto open = new PatientComplaintPortalRowDto(1L, "Cat", "text", IN_PROGRESS, d, d, "old answer");
        assertNull(open.getResponse());
        assertNull(open.getClosedAt());
        PatientComplaintPortalRowDto closed = new PatientComplaintPortalRowDto(1L, "Cat", "text", CLOSED, d, d, "the answer");
        assertEquals("the answer", closed.getResponse());
        assertEquals(d, closed.getClosedAt());
    }

    @Test
    void cleanTrimsAndTurnsBlankIntoNull() {
        assertNull(PatientComplaintService.clean("  "));
        assertNull(PatientComplaintService.clean(null));
        assertEquals("a b", PatientComplaintService.clean("  a b "));
    }

    @Test
    void staffReachOnlyComplaintsOfTheirInstitution() {
        assertTrue(PatientComplaintService.isSameInstitution(3L, 3L));
        assertFalse(PatientComplaintService.isSameInstitution(3L, 4L));
        assertFalse(PatientComplaintService.isSameInstitution(null, 3L));
        assertFalse(PatientComplaintService.isSameInstitution(3L, null));
        assertFalse(PatientComplaintService.isSameInstitution(null, null));
        PatientComplaintService.checkInstitution(3L, 3L);
        PatientComplaintException e = assertThrows(PatientComplaintException.class,
                () -> PatientComplaintService.checkInstitution(3L, 4L));
        assertEquals("Complaint not found.", e.getMessage());
    }

    @Test
    void responseCannotBeChangedOnAClosedComplaint() {
        PatientComplaintService.checkResponseEditable(NEW);
        PatientComplaintService.checkResponseEditable(IN_PROGRESS);
        PatientComplaintException e = assertThrows(PatientComplaintException.class,
                () -> PatientComplaintService.checkResponseEditable(CLOSED));
        assertEquals("Reopen the complaint to change the response.", e.getMessage());
    }
}
