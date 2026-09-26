# Room Charge Start Time: Accept Time vs. Assignment Time

## Problem

Room-charge billing (room, linen, MO, administration, medical care, nursing, maintenance,
and timed-item charges) is calculated from `PatientRoom.admittedAt` for the whole duration
of a room stay (`BhtSummeryController.getCharge()` → `InwardBeanController.calCount()`).

For institutions that admit and assign a room in a single simultaneous step (coop), this is
correct: the room is occupied the instant it's assigned, so "assignment time" and "occupancy
start time" are the same instant.

For institutions that require a ward-staff **Accept** step before a patient is considered
physically received into a room (Ruhunu — local staging only, inward not yet live in
production there), this is wrong: `admittedAt` is stamped at admission-save time
(`AdmissionController.saveSelected()` / `saveConvertSelected()`, using
`Admission.getDateOfAdmission()`), *before* the room is actually accepted. The subsequent
Accept action (`PatientTransferController.acceptTransfer()`) already exists and already
records `PatientTransferRequest.acceptedAt`/`acceptedBy`, but today it only flips
`Admission.roomAdmitted = true` — it never corrects `PatientRoom.admittedAt`. As a result,
room charges accrue from the moment the room was picked on the admission form, not from the
moment a nurse actually accepted the patient into the room.

## Root cause detail

- `AdmissionController.saveSelected()` (line ~3168) unconditionally creates the `PatientRoom`
  row via `InwardBeanController.savePatientRoom(..., admitted=true)`, regardless of the
  `"Patient admission and room assignment are simultaneous processes."` ConfigOption. The
  config only gates whether an *additional* `PatientTransferRequest` handover row
  (`status=PENDING`, `fromPatientRoom=null`) is also created for a nurse to formally accept.
  `saveConvertSelected()` (OPD→BHT conversion, line ~3343) has the identical pattern.
- `PatientTransferController.acceptTransfer()` (line ~351), in the admission-handover branch
  (`fromPatientRoom == null`, lines ~378-406), finds the room already exists
  (`alreadyInTargetRoom == true`) and short-circuits: it sets `roomAdmitted=true` and stamps
  `PatientTransferRequest.acceptedAt`/`acceptedBy`, but never touches
  `PatientRoom.admittedAt`.
- Ward-to-ward room *changes* are unaffected — that branch of `acceptTransfer()` already
  creates the new `PatientRoom` with `admittedAt` set from the real accept time
  (`effectiveAt = persisted.getAcceptedAt() != null ? persisted.getAcceptedAt() : new Date()`).
- Coop never creates a `PatientTransferRequest` at all (the config is true there), so none of
  this code executes for coop today.

## Design

No schema change. No new fields on `PatientRoom`. Every room-charge calculation method, every
UI display (room duration breakdown, Gantt bars, admission/room-change pages, room detail
reports), and every reporting JPQL keeps reading `PatientRoom.admittedAt` exactly as it does
today — none of those call sites change.

**Single change point:** `PatientTransferController.acceptTransfer()`, admission-handover
branch (`fromPatientRoom == null`). When `alreadyInTargetRoom == true` (the normal case,
since the room already exists by the time Accept is clicked), additionally:

```
currentPatientRoom.setAdmittedAt(effectiveAt);
currentPatientRoom.setAddmittedBy(sessionController.getLoggedUser());
patientRoomFacade.edit(currentPatientRoom);
```

using the same `effectiveAt` (`PatientTransferRequest.acceptedAt` if already set, else `new
Date()`) already computed earlier in the method. No change is needed to the
`alreadyInTargetRoom == false` branch (roomless-admission case) — it already calls
`savePatientRoom()` with `admittedAt = effectiveAt`.

**Historical assignment time** (when the room was originally picked/requested, as distinct
from when it was accepted) remains available via `PatientTransferRequest.initiatedAt` —
that field is untouched by this change and continues to exist only for institutions using
the non-simultaneous flow.

**Coop impact: none, by construction.** This code path only executes when a
`PatientTransferRequest` exists for the admission, which only happens when
`"Patient admission and room assignment are simultaneous processes."` is false — a config
coop does not set. The branch is unreachable for coop, not merely inert.

## Consequences / caveats

- **Interim bill during the pending-accept window**: between admission-save and the actual
  Accept click, `admittedAt` still holds the (soon-to-be-corrected) admission-time value, so
  an interim bill viewed in that gap shows inflated room duration. It self-corrects the
  moment Accept is clicked. The final bill is generated after acceptance in the normal
  workflow, so it reflects the corrected value. Interim bills are provisional by nature, so
  this was judged acceptable.
- **Everything downstream of `admittedAt`** (occupancy Gantt bars, room duration breakdown,
  overlap-conflict validation, census/occupancy reports, room-income reports) will now
  reflect the corrected accept-time value *after* acceptance, for institutions using this
  flow — this is intentional and desired, not a side effect to guard against, since
  `admittedAt` becomes a more accurate "actually received into this room" timestamp for
  those institutions. Before acceptance, these continue to show the not-yet-corrected value,
  consistent with the interim-bill caveat above.
- **Occupancy overlap validation** (`BhtSummeryController.getOverlappingRooms()` /
  `isOverlapConflict()`) reads `admittedAt` too. Retroactively moving a room's `admittedAt`
  forward at accept time can only shrink an apparent overlap window, never create a new one,
  so no regression expected there — worth confirming with a live test involving an
  in-flight second transfer request during the pending-accept window.

## Out of scope

- `PatientTransferController.acceptInTheatre()` has the same class of bug — it stamps
  `TheatreRoom.admittedAt` from `PatientTransferRequest.initiatedAt`, not the real
  theatre-accept time. This is a ward-room-charge fix; theatre occupancy/billing timing is a
  separate concern to be raised as its own issue if it needs the same treatment.
- The deprecated `RoomChangeController.admitRoom()` / `admit_room.xhtml` manual-assign path
  (used only for the rare roomless-admission / OPD-conversion edge case) is unaffected by
  this change — it doesn't go through `acceptTransfer()` at all, and continues to behave as
  it does today.

## Affected files

- `src/main/java/com/divudi/bean/inward/PatientTransferController.java` — `acceptTransfer()`,
  admission-handover branch (~line 378-406).

## Testing plan

1. **Coop regression check**: admit a patient in coop (simultaneous=true), confirm no
   `PatientTransferRequest` is created and `PatientRoom.admittedAt` is unchanged from current
   behavior (admission time).
2. **Ruhunu new-admission flow**: admit a patient with `"...simultaneous processes."=false`,
   confirm a `PatientTransferRequest` (PENDING, `fromPatientRoom=null`) is created, confirm
   `PatientRoom.admittedAt` still holds admission time until Accept.
3. Click **Accept Patients** for that admission; confirm `PatientRoom.admittedAt` is updated
   to the accept time (not the original admission time), and that the room-charge calculation
   (interim bill) reflects the shorter, correct duration from that point forward.
4. Confirm `PatientTransferRequest.initiatedAt` still shows the original assignment time,
   unaffected.
5. Ward-to-ward room change: confirm unchanged behavior (already correct today).
