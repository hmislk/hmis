# Theatre Stay Billing — Design Spec

**Issue**: [hmislk/hmis#22213](https://github.com/hmislk/hmis/issues/22213)
**Date**: 2026-07-18
**Status**: Approved

## Problem

When an inpatient is sent to theatre (`PatientTransferRequest` with `theatreTransferType = SEND_TO_THEATRE`), the theatre stay is never billed. The theatre's `RoomFacilityCharge` has a real, configured rate (room/MO/nursing/linen/admin/medical-care components), but that rate is never applied to the patient's bill. Room Stay History, the Interim Bill's Room Details tab, the Summary panel, and the Charges panel all silently omit the theatre visit — only the Patient Room & Theatre Timeline widget is aware it happened.

**Confirmed business rule**: during a theatre stay, both the ward room charge AND the theatre charge apply concurrently. The hospital does not reallocate the ward room for another patient during surgery, and the patient's relatives/guardians may continue occupying it. So the fix must add the theatre charge on top of the still-active room charge for the overlap period — not replace it.

**Exception (out of scope)**: some hospitals manually discharge the patient from the ward room and re-admit once theatre is finished. That workflow already bills correctly today via normal Room Change / discharge-readmit `PatientRoom` mechanics and needs no change.

## Root cause

- `PatientRoom` (`src/main/java/com/divudi/core/entity/inward/PatientRoom.java`) carries a full set of charge fields (`current*Charge`, `discount*Charge`, `adjusted*Charge`, etc.) and is the sole source for every billing surface, via `BhtSummeryController.getPatientRooms()`/`createPatientRooms()` and `InwardBeanController.fetchPatientRoomAll()`/`getPatientRoomChargeSumsBulk()`.
- `PatientTransferRequest` (`src/main/java/com/divudi/core/entity/inward/PatientTransferRequest.java`), the entity used for theatre visits, has no charge fields at all — only a pointer (`toRoomFacilityCharge`) to the rate.
- `PatientTransferController.acceptInTheatre()` deliberately never creates a `PatientRoom` row (per issue #22211 investigation) — the ward bed stays reserved during surgery, which is correct, but leaves the theatre's rate structurally unreachable by any billing code path.

## Existing precedent

`GuardianRoom extends PatientRoom` (single-table inheritance, no new table/columns, just a `DTYPE` discriminator) already lets two concurrent billable "room" records exist for one admission — the patient's ward room and a guardian's accompanying bed. Every billing surface queries `PatientRoom` generically with **no subclass filter**, so a `GuardianRoom` row is automatically included in Room Stay History, the Interim Bill, and the bulk-sum totals. The XHTML already special-cases it with a "Guardian" badge (`inward_patient_room_details.xhtml` line ~127: `rm.patientRoomClass eq 'class com.divudi.core.entity.inward.GuardianRoom'`).

A reusable creation helper already exists: `InwardBeanController.savePatientRoom(PatientRoom, previousRoom, RoomFacilityCharge, PatientEncounter, admittedAt, WebUser)`, used today by the ward-to-ward Room Change flow.

## Approaches considered

1. **New `TheatreRoom extends PatientRoom` subclass, created/closed by the theatre workflow controller (chosen).** Reuses the `GuardianRoom` pattern exactly. Zero changes needed to Room Stay History, Interim Bill Room Details, Summary, or Charges — they inherit the new row for free. Smallest, lowest-risk surface area.
2. **Reuse plain `PatientRoom` directly, no subclass.** Same lifecycle, but staff can't visually distinguish a theatre charge from a real room change in Room Stay History, and there's no `instanceof` hook for theatre-specific UI/behavior.
3. **Add charge fields to `PatientTransferRequest` itself, merge into every billing surface.** Avoids a second concurrent room record, but requires duplicating all charge-calculation logic (discount %, price matrix, timed items) currently centralized around `PatientRoom`, and touching every billing surface individually to union two different entity types. Much larger, riskier surface area; diverges from the pattern the codebase already uses for this exact kind of problem.

**Chosen: Approach 1.**

## Design

### New entity

`src/main/java/com/divudi/core/entity/inward/TheatreRoom.java`:
```java
@Entity
public class TheatreRoom extends PatientRoom implements Serializable { }
```

### `PatientTransferRequest` — new field

Add `theatreRoom` (`@ManyToOne PatientRoom theatreRoom`) to `PatientTransferRequest`, mirroring the existing `fromPatientRoom` field. Links a `SEND_TO_THEATRE` request to the `TheatreRoom` row it created, so `returnToWard()` can close the right row without a lookup query.

### Creation — billing window start

**Trigger**: `PatientTransferController.acceptInTheatre(PatientTransferRequest req)` — only once theatre formally accepts the patient (status → `ACCEPTED`, `theatreOccupancyStatus` → `RECEIVED_IN_THEATRE`). A request cancelled before acceptance never creates a billable row.

**Backdated window start**: the created row's `admittedAt = req.getInitiatedAt()`, not the acceptance instant — per the confirmed requirement that billing should match the Timeline widget exactly (which already uses `initiatedAt` as its bar start).

Implementation, appended inside the existing `acceptInTheatre()` method after the current status/occupancy updates:
```java
TheatreRoom theatreRoom = new TheatreRoom();
theatreRoom = (TheatreRoom) inwardBean.savePatientRoom(
        theatreRoom,
        /* previousRoom */ null,
        persisted.getToRoomFacilityCharge(),
        persisted.getAdmission(),
        persisted.getInitiatedAt(),
        sessionController.getLoggedUser());
persisted.setTheatreRoom(theatreRoom);
```
`previousRoom = null` — this is a concurrent addition, not a chain link. The ward `PatientRoom` is untouched and remains `admission.currentPatientRoom` throughout.

### Closing — billing window end

**Trigger**: `PatientTransferController.returnToWard(PatientTransferRequest theatreReq)` — the same instant `returnedToWardAt` is stamped on the request (OT-side initiation of the return, not the ward's later confirmation — matches the chosen billing-window answer and the existing Timeline bar end).

```java
PatientRoom theatreRoom = persisted.getTheatreRoom();
if (theatreRoom != null && !theatreRoom.isDischarged()) {
    theatreRoom.setDischarged(true);
    theatreRoom.setDischargedAt(new Date());
    theatreRoom.setDischargedBy(sessionController.getLoggedUser());
    patientRoomFacade.edit(theatreRoom);
}
```

### Why no other billing code changes

- `InwardBeanController.fetchPatientRoomAll()`: `SELECT pr FROM PatientRoom pr WHERE pr.retired=false AND pr.patientEncounter IN :pe` — no `DTYPE` filter, picks up `TheatreRoom` rows automatically. Drives Room Stay History and the Interim Bill Room Details tab.
- `InwardBeanController.getPatientRoomChargeSumsBulk()`: same — `FROM PatientRoom p WHERE ...`, no subclass filter. Drives the "Room Charges" line in the Charges panel and the Summary totals.
- `BhtSummeryController.setPatientRoomData()`/`calculateRoomCharge()`/`getCharge()`: iterate `patientRooms` generically; confirmed `getCharge()` falls back to `new Date()` when `dischargedAt == null`, so an in-progress (not yet returned) theatre visit already shows a live, growing charge on the interim bill — same behavior as an active ward stay. No change needed.
- The `GuardianRoom` charge-field exclusion (`if (!(p instanceof GuardianRoom)) { ...nursing/MO/admin/medicalCare... }` in `setPatientRoomData()`) does **not** apply to `TheatreRoom` — a theatre visit should charge all configured components (room, MO, nursing, etc.), unlike a guardian bed.

### UI changes

- `inward_patient_room_details.xhtml` (~line 127): add a "Theatre" badge alongside the existing "Guardian" badge check, using `rm.patientRoomClass eq 'class com.divudi.core.entity.inward.TheatreRoom'`.
- `inward_bill_intrim.xhtml` Fees and Details → Room Details tab: same badge treatment for consistency.

### Edge cases

- **Cancelled/rejected `SEND_TO_THEATRE` requests**: no `TheatreRoom` ever created (creation only happens in `acceptInTheatre()`), so nothing is billed for a visit that never happened.
- **Multiple theatre visits per admission**: each `acceptInTheatre()`/`returnToWard()` pair creates and closes its own `TheatreRoom` row via the per-request `theatreRoom` FK — no shared state, no special-casing needed.
- **`BhtSummeryController.updatePatientRoom()` sequential-order guard**: rejects a manually-edited row's `admittedAt` only if the previous list-ordered row's `dischargedAt` is non-null. Since the ward room stays open (`dischargedAt == null`) throughout the theatre visit, this guard does not false-positive. Verify with a regression test.
- **Manual edit of a `TheatreRoom` row**: Room Stay History's existing action buttons (Save Changes, Update Charges from Room Rates, Discharge from Room, Remove Room) remain available, matching current `GuardianRoom` behavior — no special lockdown.
- **Historical/already-completed admissions**: out of scope. This fix is forward-only; no automatic backfill for encounters that already went through theatre before this ships, since retroactively adjusting historical bills is a financial decision outside this issue.

## Testing plan

1. **DB-level reproduction**: replay the original repro (admit → send to theatre → accept in theatre → return to ward) against local test data; verify a `TheatreRoom` row appears in `PATIENTROOM` carrying the theatre's `RoomFacilityCharge` rate.
2. **UI/Playwright**: Room Stay History shows two rows (ward + "Theatre" badge); Interim Bill Room Details tab shows both; Summary/Charges totals include both amounts.
3. **Regression**: existing ward-only admissions (no theatre visit) and existing guardian-room admissions still compute unchanged totals.

## Files touched

- `src/main/java/com/divudi/core/entity/inward/TheatreRoom.java` (new)
- `src/main/java/com/divudi/core/entity/inward/PatientTransferRequest.java` (new `theatreRoom` field + getter/setter)
- `src/main/java/com/divudi/bean/inward/PatientTransferController.java` (`acceptInTheatre()`, `returnToWard()`)
- `src/main/webapp/inward/inward_patient_room_details.xhtml` (badge)
- `src/main/webapp/inward/inward_bill_intrim.xhtml` (badge)
- DDL regeneration required (new entity, new FK column) — run the `generate-ddl` skill per dev-issue step 5a.
