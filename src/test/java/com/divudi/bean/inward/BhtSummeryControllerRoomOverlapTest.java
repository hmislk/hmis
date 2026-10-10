package com.divudi.bean.inward;

import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.inward.GuardianRoom;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Room-stay overlap detection on the Patient Room Details screen (Issue #23641).
 *
 * <p>Reproduces the Coop case that prompted the issue: a patient moved from one
 * room to another with no gap in their own stays, while a different patient was
 * still recorded in the first room's bed. The warning was correct but described
 * only the room, so it read as "Room 90 overlaps with Room 90".
 */
class BhtSummeryControllerRoomOverlapTest {

    /** Mirrors the default shortDateTimeFormat preference. */
    private static final String PATTERN = "dd MMM yyyy HH:mm";

    private static Date at(String yyyyMMddHHmm) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(yyyyMMddHHmm);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static RoomFacilityCharge bed(long id, String name) {
        RoomFacilityCharge bed = new RoomFacilityCharge();
        bed.setId(id);
        bed.setName(name);
        return bed;
    }

    private static PatientEncounter encounter(long id, String bhtNo, String patientName) {
        PatientEncounter pe = new PatientEncounter();
        pe.setId(id);
        pe.setBhtNo(bhtNo);
        Person person = new Person();
        person.setName(patientName);
        Patient patient = new Patient();
        patient.setPerson(person);
        pe.setPatient(patient);
        return pe;
    }

    private static PatientRoom stay(long id, PatientEncounter pe, RoomFacilityCharge bed, String from, String to) {
        PatientRoom pr = new PatientRoom();
        pr.setId(id);
        pr.setPatientEncounter(pe);
        pr.setRoomFacilityCharge(bed);
        pr.setAdmittedAt(at(from));
        if (to != null) {
            pr.setDischargedAt(at(to));
        }
        return pr;
    }

    // ---------------------------------------------------------------- retired

    @Test
    void retiredCandidateIsNeverAConflict() {
        PatientEncounter pe = encounter(1L, "BHT/1", "A Patient");
        RoomFacilityCharge room90 = bed(90L, "Room 90 wm");

        PatientRoom live = stay(200L, pe, room90, "2026-09-08 15:17", "2026-09-08 19:19");
        // A withdrawn correction that squarely overlaps the live row.
        PatientRoom retired = stay(201L, pe, room90, "2026-09-08 15:01", "2026-09-08 19:16");
        retired.setRetired(true);

        assertFalse(BhtSummeryController.isOverlapConflict(live, retired),
                "a retired stay was withdrawn and must never raise an overlap");
    }

    @Test
    void retiredSubjectNeverHasAConflict() {
        PatientEncounter pe = encounter(1L, "BHT/1", "A Patient");
        RoomFacilityCharge room90 = bed(90L, "Room 90 wm");

        PatientRoom retired = stay(201L, pe, room90, "2026-09-08 15:01", "2026-09-08 19:16");
        retired.setRetired(true);
        PatientRoom live = stay(200L, pe, room90, "2026-09-08 15:17", "2026-09-08 19:19");

        assertFalse(BhtSummeryController.isOverlapConflict(retired, live),
                "a retired stay cannot be in conflict with anything either");
    }

    // ------------------------------------------------------- non-overlapping

    @Test
    void backToBackStaysForTheSamePatientDoNotConflict() {
        PatientEncounter pe = encounter(1L, "BHT/57914", "A Patient");

        PatientRoom first = stay(200L, pe, bed(90L, "Room 90 wm"), "2026-09-08 15:17", "2026-09-08 19:19");
        PatientRoom second = stay(202L, pe, bed(511L, "Room 511 wm"), "2026-09-08 19:20", null);

        assertFalse(BhtSummeryController.isOverlapConflict(first, second));
        assertFalse(BhtSummeryController.isOverlapConflict(second, first));
    }

    @Test
    void guardianRoomIsExcluded() {
        PatientEncounter pe = encounter(1L, "BHT/1", "A Patient");
        RoomFacilityCharge room90 = bed(90L, "Room 90 wm");

        PatientRoom ward = stay(200L, pe, room90, "2026-09-08 15:17", "2026-09-08 19:19");
        GuardianRoom guardian = new GuardianRoom();
        guardian.setId(203L);
        guardian.setPatientEncounter(pe);
        guardian.setRoomFacilityCharge(room90);
        guardian.setAdmittedAt(at("2026-09-08 15:17"));

        assertFalse(BhtSummeryController.isOverlapConflict(ward, guardian));
        assertFalse(BhtSummeryController.isOverlapConflict(guardian, ward));
    }

    @Test
    void differentPatientsInDifferentBedsDoNotConflict() {
        PatientRoom mine = stay(200L, encounter(1L, "BHT/57914", "A Patient"),
                bed(90L, "Room 90 wm"), "2026-09-08 15:17", "2026-09-08 19:19");
        PatientRoom theirs = stay(300L, encounter(2L, "BHT/57843", "B Patient"),
                bed(511L, "Room 511 wm"), "2026-09-05 00:48", null);

        assertFalse(BhtSummeryController.isOverlapConflict(mine, theirs));
    }

    // ------------------------------------------------------ cross-patient bed

    @Test
    void anotherPatientStillInTheSameBedIsAConflict() {
        PatientRoom mine = stay(200L, encounter(1L, "BHT/57914", "A Patient"),
                bed(90L, "Room 90 wm"), "2026-09-08 15:17", "2026-09-08 19:19");
        PatientRoom theirs = stay(300L, encounter(2L, "BHT/57843", "B Patient"),
                bed(90L, "Room 90 wm"), "2026-09-05 00:48", null);

        assertTrue(BhtSummeryController.isOverlapConflict(mine, theirs));
    }

    @Test
    void crossPatientConflictNamesTheOtherBhtPatientAndWindow() {
        PatientRoom mine = stay(200L, encounter(1L, "BHT/57914", "A Patient"),
                bed(90L, "Room 90 wm"), "2026-09-08 15:17", "2026-09-08 19:19");
        PatientRoom theirs = stay(300L, encounter(2L, "BHT/57843", "B Patient"),
                bed(90L, "Room 90 wm"), "2026-09-05 00:48", null);

        String description = BhtSummeryController.describeOverlaps(mine, Collections.singletonList(theirs), PATTERN);

        assertTrue(description.contains("Room 90 wm"), description);
        assertTrue(description.contains("BHT/57843"), description);
        assertTrue(description.contains("B Patient"), description);
        assertTrue(description.contains("05 Sep 2026 00:48"), description);
        assertTrue(description.contains("still in room"), description);
    }

    @Test
    void crossPatientConflictShowsTheOtherStaysDischargeTimeWhenItHasOne() {
        PatientRoom mine = stay(200L, encounter(1L, "BHT/57914", "A Patient"),
                bed(90L, "Room 90 wm"), "2026-09-08 15:17", "2026-09-08 19:19");
        PatientRoom theirs = stay(300L, encounter(2L, "BHT/57843", "B Patient"),
                bed(90L, "Room 90 wm"), "2026-09-05 00:48", "2026-09-09 11:11");

        String description = BhtSummeryController.describeOverlaps(mine, Collections.singletonList(theirs), PATTERN);

        assertTrue(description.contains("09 Sep 2026 11:11"), description);
        assertFalse(description.contains("still in room"), description);
    }

    /**
     * The description sits next to the row's own Admitted At / Discharged At
     * pickers, which render with the deployment's shortDateTimeFormat. Rendering
     * the conflict in a different format would have staff comparing "19:19" with
     * "07:19 PM" on the one screen meant to resolve the conflict.
     */
    @Test
    void stayWindowUsesTheSuppliedDateTimePattern() {
        PatientRoom mine = stay(200L, encounter(1L, "BHT/57914", "A Patient"),
                bed(90L, "Room 90 wm"), "2026-09-08 15:17", "2026-09-08 19:19");
        PatientRoom theirs = stay(300L, encounter(2L, "BHT/57843", "B Patient"),
                bed(90L, "Room 90 wm"), "2026-09-05 00:48", "2026-09-09 23:11");

        String description = BhtSummeryController.describeOverlaps(
                mine, Collections.singletonList(theirs), "dd/MM/yyyy hh:mm a");

        assertTrue(description.contains("05/09/2026 12:48 AM"), description);
        assertTrue(description.contains("09/09/2026 11:11 PM"), description);
    }

    /** A blank or malformed preference must not blank the row or throw. */
    @Test
    void stayWindowFallsBackWhenThePatternIsUnusable() {
        PatientRoom mine = stay(200L, encounter(1L, "BHT/57914", "A Patient"),
                bed(90L, "Room 90 wm"), "2026-09-08 15:17", "2026-09-08 19:19");
        PatientRoom theirs = stay(300L, encounter(2L, "BHT/57843", "B Patient"),
                bed(90L, "Room 90 wm"), "2026-09-05 00:48", null);

        for (String pattern : new String[]{null, "   ", "dd 'unterminated"}) {
            String description = BhtSummeryController.describeOverlaps(
                    mine, Collections.singletonList(theirs), pattern);
            assertTrue(description.contains("05 Sep 2026 00:48"),
                    "pattern " + pattern + " -> " + description);
        }
    }

    @Test
    void crossPatientConflictFallsBackWhenTheOtherEncounterHasNoBht() {
        PatientEncounter noBht = encounter(2L, null, null);
        PatientRoom mine = stay(200L, encounter(1L, "BHT/57914", "A Patient"),
                bed(90L, "Room 90 wm"), "2026-09-08 15:17", "2026-09-08 19:19");
        PatientRoom theirs = stay(300L, noBht, bed(90L, "Room 90 wm"), "2026-09-05 00:48", null);

        String description = BhtSummeryController.describeOverlaps(mine, Collections.singletonList(theirs), PATTERN);

        assertTrue(description.contains("another patient"), description);
    }

    // ---------------------------------------------------- same-patient wording

    @Test
    void sameEncounterConflictKeepsTheShortWording() {
        PatientEncounter pe = encounter(1L, "BHT/57914", "A Patient");
        PatientRoom mine = stay(200L, pe, bed(90L, "Room 90 wm"), "2026-09-08 15:17", "2026-09-08 19:19");
        PatientRoom otherRoomSamePatient = stay(202L, pe, bed(511L, "Room 511 wm"), "2026-09-08 18:00", null);

        String description = BhtSummeryController.describeOverlaps(mine, Collections.singletonList(otherRoomSamePatient), PATTERN);

        assertEquals("Overlaps with Room 511 wm (Active)", description);
    }

    @Test
    void sameEncounterConflictThatHasLeftIsMarkedLeft() {
        PatientEncounter pe = encounter(1L, "BHT/57914", "A Patient");
        PatientRoom mine = stay(200L, pe, bed(90L, "Room 90 wm"), "2026-09-08 15:17", "2026-09-08 19:19");
        PatientRoom otherRoomSamePatient = stay(202L, pe, bed(511L, "Room 511 wm"), "2026-09-08 18:00", "2026-09-08 18:30");

        String description = BhtSummeryController.describeOverlaps(mine, Collections.singletonList(otherRoomSamePatient), PATTERN);

        assertEquals("Overlaps with Room 511 wm (Left)", description);
    }

    @Test
    void multipleConflictsAreAllListed() {
        PatientEncounter pe = encounter(1L, "BHT/57914", "A Patient");
        PatientRoom mine = stay(200L, pe, bed(90L, "Room 90 wm"), "2026-09-08 15:17", "2026-09-08 19:19");
        PatientRoom sameEncounter = stay(202L, pe, bed(511L, "Room 511 wm"), "2026-09-08 18:00", null);
        PatientRoom otherPatient = stay(300L, encounter(2L, "BHT/57843", "B Patient"),
                bed(90L, "Room 90 wm"), "2026-09-05 00:48", null);

        List<PatientRoom> conflicts = Arrays.asList(sameEncounter, otherPatient);
        String description = BhtSummeryController.describeOverlaps(mine, conflicts, PATTERN);

        assertTrue(description.contains("Room 511 wm (Active)"), description);
        assertTrue(description.contains("BHT/57843"), description);
        assertTrue(description.contains(";"), "multiple conflicts should be separated: " + description);
    }

    @Test
    void noConflictsProducesNoDescription() {
        PatientRoom mine = stay(200L, encounter(1L, "BHT/57914", "A Patient"),
                bed(90L, "Room 90 wm"), "2026-09-08 15:17", "2026-09-08 19:19");

        assertEquals("", BhtSummeryController.describeOverlaps(mine, Collections.<PatientRoom>emptyList(), PATTERN));
        assertEquals("", BhtSummeryController.describeOverlaps(mine, null, PATTERN));
    }
}
