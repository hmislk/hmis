# Theatre Stay Billing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make theatre stays actually appear on the patient's bill by creating a concurrent, closable `TheatreRoom` record — alongside the still-active ward `PatientRoom` — so Room Stay History, the Interim Bill's Room Details tab, the Summary panel, and the Charges panel all pick it up automatically.

**Architecture:** `TheatreRoom extends PatientRoom` (single-table inheritance, same pattern as the existing `GuardianRoom`). `PatientTransferController.acceptInTheatre()` creates it (backdated to the transfer request's `initiatedAt`); `PatientTransferController.returnToWard()` discharges it. No other billing code changes — every billing surface already queries `PatientRoom` with no subclass filter.

**Tech Stack:** Java EE / Jakarta EE (JSF + PrimeFaces, JPA/EclipseLink), Maven, MySQL, Payara. This codebase has no JUnit coverage for `bean.inward`/`entity.inward` classes (22 test files total, none touch this package) — this project's established verification loop is compile → build → local Payara deploy → Playwright + DB verification (see `developer_docs/testing/playwright-e2e-workflow.md` and the project's `dev-issue` skill), not unit-test TDD. Each task below is sized so `mvn clean package -DskipTests` compiles cleanly after it; full functional verification happens once in Task 6, after all wiring is in place, because theatre-visit creation and discharge are two halves of one lifecycle that can't be meaningfully exercised in isolation against a real deployment.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-07-18-theatre-stay-billing-design.md` — follow it exactly; do not add scope beyond it.
- **JPQL first, native SQL last** (CLAUDE.md) — not applicable here (no new queries needed; existing queries already have no subclass filter).
- **Never modify existing constructors** (CLAUDE.md) — `TheatreRoom` has no custom constructor; `PatientTransferRequest`'s existing fields/methods are untouched, only a new field + getter/setter are added.
- Billing window: `TheatreRoom.admittedAt = request.getInitiatedAt()`, `TheatreRoom.dischargedAt` set the same instant as `request.setReturnedToWardAt(...)` inside `returnToWard()`. Do not use `acceptedAt` for either boundary — confirmed by the user during design.
- `previousRoom` on the new `TheatreRoom` must be `null` (it is a concurrent addition, not a room-chain link) — do not pass the ward `PatientRoom` as `previousRoom`.
- No backfill for historical admissions — out of scope per spec.

---

### Task 1: `TheatreRoom` entity + `PatientTransferRequest.theatreRoom` field

**Files:**
- Create: `src/main/java/com/divudi/core/entity/inward/TheatreRoom.java`
- Modify: `src/main/java/com/divudi/core/entity/inward/PatientTransferRequest.java:33-44` (field block), `:107-129` (getter/setter block)

**Interfaces:**
- Produces: `TheatreRoom` (public no-arg-constructible entity class, subtype of `PatientRoom`); `PatientTransferRequest.getTheatreRoom(): PatientRoom` / `PatientTransferRequest.setTheatreRoom(PatientRoom): void`.

- [ ] **Step 1: Create the `TheatreRoom` entity**

Write `src/main/java/com/divudi/core/entity/inward/TheatreRoom.java`:

```java
package com.divudi.core.entity.inward;

import java.io.Serializable;
import javax.persistence.Entity;

/**
 *
 * @author buddhika
 */
@Entity
public class TheatreRoom extends PatientRoom implements Serializable {

}
```

This mirrors `src/main/java/com/divudi/core/entity/inward/GuardianRoom.java` exactly — no new table, no new columns; EclipseLink adds a `DTYPE` discriminator value on the existing `PATIENTROOM` table.

- [ ] **Step 2: Add the `theatreRoom` field to `PatientTransferRequest`**

In `src/main/java/com/divudi/core/entity/inward/PatientTransferRequest.java`, the field block currently reads (lines 33-44):

```java
    @ManyToOne
    private Admission admission;

    /**
     * null = admission handover; non-null = ward-to-ward transfer
     */
    @ManyToOne
    private PatientRoom fromPatientRoom;

    @ManyToOne
    private RoomFacilityCharge toRoomFacilityCharge;

    @Enumerated(EnumType.STRING)
    private TransferRequestStatus status;
```

Add a new field directly after `fromPatientRoom`:

```java
    @ManyToOne
    private Admission admission;

    /**
     * null = admission handover; non-null = ward-to-ward transfer
     */
    @ManyToOne
    private PatientRoom fromPatientRoom;

    /**
     * Set only on accepted SEND_TO_THEATRE requests — the concurrent
     * TheatreRoom billing record created by acceptInTheatre(), closed by
     * returnToWard(). Null on all other transfer types.
     */
    @ManyToOne
    private PatientRoom theatreRoom;

    @ManyToOne
    private RoomFacilityCharge toRoomFacilityCharge;

    @Enumerated(EnumType.STRING)
    private TransferRequestStatus status;
```

- [ ] **Step 3: Add the getter/setter**

The existing getter/setter block for `fromPatientRoom` (lines 115-121) reads:

```java
    public PatientRoom getFromPatientRoom() {
        return fromPatientRoom;
    }

    public void setFromPatientRoom(PatientRoom fromPatientRoom) {
        this.fromPatientRoom = fromPatientRoom;
    }
```

Add directly after it:

```java
    public PatientRoom getFromPatientRoom() {
        return fromPatientRoom;
    }

    public void setFromPatientRoom(PatientRoom fromPatientRoom) {
        this.fromPatientRoom = fromPatientRoom;
    }

    public PatientRoom getTheatreRoom() {
        return theatreRoom;
    }

    public void setTheatreRoom(PatientRoom theatreRoom) {
        this.theatreRoom = theatreRoom;
    }
```

- [ ] **Step 4: Compile**

Run: `mvn clean compile -DskipTests -q`
Expected: `BUILD SUCCESS`, no errors referencing `TheatreRoom` or `PatientTransferRequest`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/core/entity/inward/TheatreRoom.java src/main/java/com/divudi/core/entity/inward/PatientTransferRequest.java
git commit -m "feat(inward): add TheatreRoom entity and PatientTransferRequest.theatreRoom link

Part of #22213 — foundation for billing theatre stays as a concurrent
PatientRoom-family record, mirroring the existing GuardianRoom pattern."
```

---

### Task 2: Create the `TheatreRoom` when theatre accepts the patient

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/PatientTransferController.java:545-563` (`acceptInTheatre`), imports at top of file

**Interfaces:**
- Consumes: `TheatreRoom` (Task 1, no-arg constructor), `PatientTransferRequest.setTheatreRoom(PatientRoom)` (Task 1), `InwardBeanController.savePatientRoom(PatientRoom, PatientRoom, RoomFacilityCharge, PatientEncounter, Date, WebUser): PatientRoom` (existing, `src/main/java/com/divudi/bean/inward/InwardBeanController.java:2270`).
- Produces: `acceptInTheatre()` now also persists a `TheatreRoom` row and links it on the request — later tasks (Task 3) read it back via `persisted.getTheatreRoom()`.

- [ ] **Step 1: Add the import**

In `src/main/java/com/divudi/bean/inward/PatientTransferController.java`, the import block currently includes (line 12):

```java
import com.divudi.core.entity.inward.PatientRoom;
```

Add directly after it:

```java
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.entity.inward.TheatreRoom;
```

- [ ] **Step 2: Wire creation into `acceptInTheatre()`**

The current method (lines 545-563) reads:

```java
    public void acceptInTheatre(PatientTransferRequest req) {
        if (req == null || req.getId() == null) {
            return;
        }
        PatientTransferRequest persisted = patientTransferRequestFacade.find(req.getId());
        if (persisted == null || persisted.getStatus() != TransferRequestStatus.PENDING) {
            JsfUtil.addErrorMessage("This request is no longer pending.");
            loadPendingForTheatre();
            return;
        }
        persisted.setStatus(TransferRequestStatus.ACCEPTED);
        persisted.setAcceptedAt(new Date());
        persisted.setAcceptedBy(sessionController.getLoggedUser());
        persisted.setTheatreOccupancyStatus(TheatreOccupancyStatus.RECEIVED_IN_THEATRE);
        patientTransferRequestFacade.edit(persisted);
        loadPendingForTheatre();
        loadInTheatreRequests();
        JsfUtil.addSuccessMessage("Patient accepted in theatre.");
    }
```

Replace it with:

```java
    public void acceptInTheatre(PatientTransferRequest req) {
        if (req == null || req.getId() == null) {
            return;
        }
        PatientTransferRequest persisted = patientTransferRequestFacade.find(req.getId());
        if (persisted == null || persisted.getStatus() != TransferRequestStatus.PENDING) {
            JsfUtil.addErrorMessage("This request is no longer pending.");
            loadPendingForTheatre();
            return;
        }
        persisted.setStatus(TransferRequestStatus.ACCEPTED);
        persisted.setAcceptedAt(new Date());
        persisted.setAcceptedBy(sessionController.getLoggedUser());
        persisted.setTheatreOccupancyStatus(TheatreOccupancyStatus.RECEIVED_IN_THEATRE);

        TheatreRoom theatreRoom = new TheatreRoom();
        theatreRoom = (TheatreRoom) inwardBean.savePatientRoom(
                theatreRoom,
                null,
                persisted.getToRoomFacilityCharge(),
                persisted.getAdmission(),
                persisted.getInitiatedAt(),
                sessionController.getLoggedUser());
        persisted.setTheatreRoom(theatreRoom);

        patientTransferRequestFacade.edit(persisted);
        loadPendingForTheatre();
        loadInTheatreRequests();
        JsfUtil.addSuccessMessage("Patient accepted in theatre.");
    }
```

Note: `previousRoom` is passed as `null` (not `persisted.getFromPatientRoom()`) — the ward room is not being replaced, so this must not be chained via `previousRoom`/`nextRoom`.

- [ ] **Step 3: Compile**

Run: `mvn clean compile -DskipTests -q`
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/PatientTransferController.java
git commit -m "feat(inward): create TheatreRoom billing record on theatre acceptance

Part of #22213 — acceptInTheatre() now opens a concurrent TheatreRoom
record backdated to the transfer request's initiatedAt, so the
theatre's RoomFacilityCharge rate starts accruing alongside the still-
active ward room charge."
```

---

### Task 3: Discharge the `TheatreRoom` when the patient returns to ward

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/PatientTransferController.java:609-651` (`returnToWard`)

**Interfaces:**
- Consumes: `PatientTransferRequest.getTheatreRoom(): PatientRoom` (Task 1), `PatientRoom.setDischarged(boolean)` / `setDischargedAt(Date)` / `setDischargedBy(WebUser)` / `isDischarged(): boolean` (existing, `PatientRoom.java`).
- Produces: a fully closed `TheatreRoom` row with `discharged=true` — the billing window is now bounded, matching what `getCharge()` in `BhtSummeryController` needs to stop accruing.

- [ ] **Step 1: Wire discharge into `returnToWard()`**

The current method (lines 609-651) reads:

```java
    public void returnToWard(PatientTransferRequest theatreReq) {
        if (theatreReq == null || theatreReq.getId() == null) {
            return;
        }
        PatientTransferRequest persisted = patientTransferRequestFacade.find(theatreReq.getId());
        if (persisted == null) {
            return;
        }
        TheatreOccupancyStatus currentStatus = persisted.getTheatreOccupancyStatus();
        if (currentStatus != TheatreOccupancyStatus.PROCEDURE_COMPLETED
                && currentStatus != TheatreOccupancyStatus.IN_RECOVERY) {
            JsfUtil.addErrorMessage("Patient must have completed the procedure before returning to ward.");
            return;
        }
        if (persisted.getFromPatientRoom() == null || persisted.getFromPatientRoom().getRoomFacilityCharge() == null) {
            JsfUtil.addErrorMessage("Cannot determine the ward room to return patient to.");
            return;
        }
        PatientTransferRequest returnReq = new PatientTransferRequest();
        returnReq.setAdmission(persisted.getAdmission());
        returnReq.setFromPatientRoom(persisted.getFromPatientRoom());
        returnReq.setToRoomFacilityCharge(persisted.getFromPatientRoom().getRoomFacilityCharge());
        returnReq.setTheatreTransferType(TheatreTransferType.RETURN_TO_WARD);
        returnReq.setSurgeryBill(persisted.getSurgeryBill());
        returnReq.setStatus(TransferRequestStatus.PENDING);
        returnReq.setNotes(notes);
        returnReq.setInitiatedAt(new Date());
        returnReq.setInitiatedBy(sessionController.getLoggedUser());
        returnReq.setCreatedAt(new Date());
        returnReq.setCreater(sessionController.getLoggedUser());
        patientTransferRequestFacade.create(returnReq);

        persisted.setTheatreOccupancyStatus(TheatreOccupancyStatus.RETURNED_TO_WARD);
        if (persisted.getReturnedToWardAt() == null) {
            persisted.setReturnedToWardAt(new Date());
        }
        patientTransferRequestFacade.edit(persisted);

        notes = null;
        loadPendingForTheatre();
        loadInTheatreRequests();
        JsfUtil.addSuccessMessage("Patient return to ward initiated.");
    }
```

Replace the tail (from `persisted.setTheatreOccupancyStatus(...)` onward) so the full method reads:

```java
    public void returnToWard(PatientTransferRequest theatreReq) {
        if (theatreReq == null || theatreReq.getId() == null) {
            return;
        }
        PatientTransferRequest persisted = patientTransferRequestFacade.find(theatreReq.getId());
        if (persisted == null) {
            return;
        }
        TheatreOccupancyStatus currentStatus = persisted.getTheatreOccupancyStatus();
        if (currentStatus != TheatreOccupancyStatus.PROCEDURE_COMPLETED
                && currentStatus != TheatreOccupancyStatus.IN_RECOVERY) {
            JsfUtil.addErrorMessage("Patient must have completed the procedure before returning to ward.");
            return;
        }
        if (persisted.getFromPatientRoom() == null || persisted.getFromPatientRoom().getRoomFacilityCharge() == null) {
            JsfUtil.addErrorMessage("Cannot determine the ward room to return patient to.");
            return;
        }
        PatientTransferRequest returnReq = new PatientTransferRequest();
        returnReq.setAdmission(persisted.getAdmission());
        returnReq.setFromPatientRoom(persisted.getFromPatientRoom());
        returnReq.setToRoomFacilityCharge(persisted.getFromPatientRoom().getRoomFacilityCharge());
        returnReq.setTheatreTransferType(TheatreTransferType.RETURN_TO_WARD);
        returnReq.setSurgeryBill(persisted.getSurgeryBill());
        returnReq.setStatus(TransferRequestStatus.PENDING);
        returnReq.setNotes(notes);
        returnReq.setInitiatedAt(new Date());
        returnReq.setInitiatedBy(sessionController.getLoggedUser());
        returnReq.setCreatedAt(new Date());
        returnReq.setCreater(sessionController.getLoggedUser());
        patientTransferRequestFacade.create(returnReq);

        Date returnedAt = new Date();
        persisted.setTheatreOccupancyStatus(TheatreOccupancyStatus.RETURNED_TO_WARD);
        if (persisted.getReturnedToWardAt() == null) {
            persisted.setReturnedToWardAt(returnedAt);
        }
        if (persisted.getTheatreRoom() != null && !persisted.getTheatreRoom().isDischarged()) {
            PatientRoom theatreRoom = persisted.getTheatreRoom();
            theatreRoom.setDischarged(true);
            theatreRoom.setDischargedAt(returnedAt);
            theatreRoom.setDischargedBy(sessionController.getLoggedUser());
            patientRoomFacade.edit(theatreRoom);
        }
        patientTransferRequestFacade.edit(persisted);

        notes = null;
        loadPendingForTheatre();
        loadInTheatreRequests();
        JsfUtil.addSuccessMessage("Patient return to ward initiated.");
    }
```

Note: `returnedAt` is computed once and reused for both `persisted.setReturnedToWardAt(...)` and `theatreRoom.setDischargedAt(...)` so the two timestamps stay identical, per the spec's confirmed billing window.

- [ ] **Step 2: Compile**

Run: `mvn clean compile -DskipTests -q`
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/PatientTransferController.java
git commit -m "feat(inward): discharge TheatreRoom when patient returns to ward

Part of #22213 — returnToWard() now closes the TheatreRoom billing
record at the same instant returnedToWardAt is stamped, so the
theatre charge window matches the Patient Room & Theatre Timeline
exactly."
```

---

### Task 4: Add a "Theatre" badge in Room Stay History and the Interim Bill

**Files:**
- Modify: `src/main/webapp/inward/inward_patient_room_details.xhtml:115-137` (Room column)
- Modify: `src/main/webapp/inward/inward_bill_intrim.xhtml:458-471` (Room column)

**Interfaces:**
- Consumes: `PatientRoom.getPatientRoomClass(): String` (existing, returns `this.getClass().toString()`), already used by the existing `GuardianRoom` badge check.

- [ ] **Step 1: Add the badge in `inward_patient_room_details.xhtml`**

The current "Room" column (lines 115-137) reads:

```xml
                                        <p:column headerText="Room" style="min-width:220px;">
                                            <div class="d-flex align-items-center gap-2 flex-wrap">
                                                <p:autoComplete
                                                    forceSelection="true"
                                                    readonly="#{bhtSummeryController.patientEncounter.discharged}"
                                                    value="#{rm.roomFacilityCharge}"
                                                    completeMethod="#{roomFacilityChargeController.completeRoomChargeAll}"
                                                    var="rf"
                                                    itemLabel="#{rf.name}"
                                                    itemValue="#{rf}"
                                                    style="width:160px;"
                                                    minQueryLength="1"/>
                                                <h:panelGroup rendered="#{rm.patientRoomClass eq 'class com.divudi.core.entity.inward.GuardianRoom'}">
                                                    <p:badge value="Guardian" severity="success"/>
                                                </h:panelGroup>
                                                <h:panelGroup rendered="#{rm.discharged}">
                                                    <p:badge value="Left" severity="info"/>
                                                </h:panelGroup>
                                                <h:panelGroup rendered="#{not rm.discharged}">
                                                    <p:badge value="Active" severity="warning"/>
                                                </h:panelGroup>
                                            </div>
                                        </p:column>
```

Add a second badge check directly after the `GuardianRoom` one:

```xml
                                        <p:column headerText="Room" style="min-width:220px;">
                                            <div class="d-flex align-items-center gap-2 flex-wrap">
                                                <p:autoComplete
                                                    forceSelection="true"
                                                    readonly="#{bhtSummeryController.patientEncounter.discharged}"
                                                    value="#{rm.roomFacilityCharge}"
                                                    completeMethod="#{roomFacilityChargeController.completeRoomChargeAll}"
                                                    var="rf"
                                                    itemLabel="#{rf.name}"
                                                    itemValue="#{rf}"
                                                    style="width:160px;"
                                                    minQueryLength="1"/>
                                                <h:panelGroup rendered="#{rm.patientRoomClass eq 'class com.divudi.core.entity.inward.GuardianRoom'}">
                                                    <p:badge value="Guardian" severity="success"/>
                                                </h:panelGroup>
                                                <h:panelGroup rendered="#{rm.patientRoomClass eq 'class com.divudi.core.entity.inward.TheatreRoom'}">
                                                    <p:badge value="Theatre" severity="danger"/>
                                                </h:panelGroup>
                                                <h:panelGroup rendered="#{rm.discharged}">
                                                    <p:badge value="Left" severity="info"/>
                                                </h:panelGroup>
                                                <h:panelGroup rendered="#{not rm.discharged}">
                                                    <p:badge value="Active" severity="warning"/>
                                                </h:panelGroup>
                                            </div>
                                        </p:column>
```

- [ ] **Step 2: Add the badge in `inward_bill_intrim.xhtml`**

The current "Room" column (lines 458-471) reads:

```xml
                                            <p:column headerText="Room" styleClass="text-start">
                                                <div class="d-flex align-items-center gap-2">
                                                    <h:outputLabel value="#{rm.roomFacilityCharge.name}" style="display: inline;"/>
                                                    <h:panelGroup rendered="#{rm.discharged}">
                                                        <p:badge value="Left" severity="info"/>
                                                    </h:panelGroup>
                                                    <h:panelGroup rendered="#{not rm.discharged}">
                                                        <p:badge value="Active" severity="warning"/>
                                                    </h:panelGroup>
                                                    <h:panelGroup rendered="#{rm.patientRoomClass eq 'class com.divudi.core.entity.inward.GuardianRoom'}">
                                                        <p:badge value="Guardian" severity="success"/>
                                                    </h:panelGroup>
                                                </div>
                                            </p:column>
```

Add the same "Theatre" badge check directly after the `GuardianRoom` one:

```xml
                                            <p:column headerText="Room" styleClass="text-start">
                                                <div class="d-flex align-items-center gap-2">
                                                    <h:outputLabel value="#{rm.roomFacilityCharge.name}" style="display: inline;"/>
                                                    <h:panelGroup rendered="#{rm.discharged}">
                                                        <p:badge value="Left" severity="info"/>
                                                    </h:panelGroup>
                                                    <h:panelGroup rendered="#{not rm.discharged}">
                                                        <p:badge value="Active" severity="warning"/>
                                                    </h:panelGroup>
                                                    <h:panelGroup rendered="#{rm.patientRoomClass eq 'class com.divudi.core.entity.inward.GuardianRoom'}">
                                                        <p:badge value="Guardian" severity="success"/>
                                                    </h:panelGroup>
                                                    <h:panelGroup rendered="#{rm.patientRoomClass eq 'class com.divudi.core.entity.inward.TheatreRoom'}">
                                                        <p:badge value="Theatre" severity="danger"/>
                                                    </h:panelGroup>
                                                </div>
                                            </p:column>
```

- [ ] **Step 3: Commit**

XHTML-only change — per CLAUDE.md, JSF-only changes don't require compilation, but Task 6's full build will still compile the whole webapp, so any typo surfaces there.

```bash
git add src/main/webapp/inward/inward_patient_room_details.xhtml src/main/webapp/inward/inward_bill_intrim.xhtml
git commit -m "feat(inward): badge TheatreRoom rows in Room Stay History and Interim Bill

Part of #22213 — visually distinguishes a theatre charge row from a
real room change/admission, same pattern as the existing Guardian badge."
```

---

### Task 5: Regenerate DDL

**Files:**
- Regenerated by the `generate-ddl` skill (do not hand-edit): `tmp/createDDL.jdbc`, the `Database-Schema-DDL-Generation-Guide` wiki page.

**Interfaces:**
- Consumes: the new `TheatreRoom` entity and `PatientTransferRequest.theatreRoom` field from Task 1 (new entity class → new `DTYPE` value; new `@ManyToOne` field → new FK column on `PATIENTTRANSFERREQUEST`).

- [ ] **Step 1: Run the DDL generator**

Invoke the project's `generate-ddl` skill (per `CLAUDE.md` § "When Working on Database" and the `dev-issue` skill's step 5a — this entity/field addition is exactly the trigger condition for that step).

- [ ] **Step 2: Review the diff**

Confirm the generated DDL includes a new nullable FK column for `theatreRoom` on `PATIENTTRANSFERREQUEST` (referencing `PATIENTROOM`), and nothing else changed unexpectedly (no `DTYPE` column changes needed — `TheatreRoom` reuses `PatientRoom`'s existing single-table-inheritance discriminator).

- [ ] **Step 3: Commit**

```bash
git add tmp/createDDL.jdbc
git commit -m "chore(db): regenerate DDL for TheatreRoom entity (#22213)"
```

(If the skill also updates the wiki page, that's committed/pushed separately from `../hmis.wiki` per its own instructions — not part of this repo's commit.)

---

### Task 6: Build, deploy, and verify end-to-end

**Files:** none (verification only).

**Interfaces:** none — this task exercises everything built in Tasks 1-5 together.

- [ ] **Step 1: Full build**

Run: `mvn clean package -DskipTests`
Expected: `BUILD SUCCESS`, `target/rh-3.0.0.war` produced.

- [ ] **Step 2: Local redeploy**

Per this machine's deploy command (see the `local-carecode-dev-credentials` memory):
```bash
/home/carecode/payara/bin/asadmin --port 9048 undeploy rh-3.0.0 2>/dev/null; \
/home/carecode/payara/bin/asadmin --port 9048 deploy --force=true target/rh-3.0.0.war
```
Expected: deploy succeeds; check `/home/carecode/payara/glassfish/domains/rh/logs/server.log` for no `TheatreRoom`/EclipseLink mapping errors on startup.

- [ ] **Step 3: Playwright — reproduce the original bug scenario end-to-end**

Using the `playwright-e2e` skill against `http://localhost:9080/rh/`: log in, select department, admit a test patient to a ward room, send them to theatre, accept in theatre, mark procedure completed, return to ward. Confirm:
- Room Stay History now shows **two rows** — the ward room (no badge) and the theatre visit (**"Theatre"** badge) — with the theatre row's charge columns populated from its `RoomFacilityCharge` rate, not zero.
- Interim Bill → Fees and Details → Room Details tab shows both rows with the same badge.
- Summary panel's Gross Total / Total Charges now includes both amounts.
- Charges panel's "Room Charges" row sums both amounts.

Per the [No PII in public GitHub content](../../../CLAUDE.md) rule reinforced during this issue's filing: do not use real patient/institution names in any screenshot or note taken during this verification; use test/synthetic data only, and if screenshots are published later, genericize any identifying labels first.

- [ ] **Step 4: DB verification**

Query the local test database (credentials per the project's local MySQL credentials reference) to confirm a new `PATIENTROOM` row exists with `DTYPE = 'TheatreRoom'`, `ADMITTEDAT` equal to the transfer request's `INITIATEDAT`, and (after returning to ward) `DISCHARGEDAT` populated and equal to the request's `RETURNEDTOWARDAT`.

- [ ] **Step 5: Regression check**

Repeat the same admission flow *without* a theatre visit (ward room only) and confirm Room Stay History, Summary, and Charges are unchanged from current behavior — no phantom theatre row, no altered totals.

- [ ] **Step 6: Restore local persistence.xml**

Per CLAUDE.md's persistence.xml lifecycle rule — after the eventual `git push` for this branch (not part of this plan; happens in the `dev-issue` workflow's later steps), restore `persistence.xml` to local JNDI values (`jdbc/coop` / `jdbc/ruhunuAudit`) and leave that change unstaged.

---

## Self-Review Notes

- **Spec coverage**: every section of the design spec maps to a task — entity/field (Task 1), creation trigger (Task 2), closing trigger (Task 3), UI badges (Task 4), DDL (Task 5), verification plan (Task 6, mirrors the spec's "Testing plan" section exactly).
- **No placeholders**: every step shows complete, exact code — no "add appropriate handling" language.
- **Type/name consistency checked**: `TheatreRoom` (Task 1) is the exact type used in `PatientTransferController` (Tasks 2-3) and the exact string literal (`class com.divudi.core.entity.inward.TheatreRoom`) used in both XHTML files (Task 4). `PatientTransferRequest.getTheatreRoom()`/`setTheatreRoom()` (Task 1) match the calls made in Tasks 2 and 3 exactly.
- **Out of scope, confirmed with user**: historical backfill, changing the Gantt timeline's own data source (it already works, untouched), and the manual discharge-and-readmit workaround workflow (already bills correctly today).
