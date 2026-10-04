# Package Admission Menu Relocation + Change Package Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Relocate the existing "Package Admission" menu item to the Admissions submenu, and add a new, audit-logged "Change Package" action reachable from `admission_profile.xhtml` for changing the `InpatientPackage` on an existing admission.

**Architecture:** Two independent, small JSF/Java changes on top of an existing, already-working package-admission entry point (`package_admit.xhtml` / `InpatientPackageAdmissionController` / `PatientEncounter.inpatientPackage`, all untouched). The new "Change Package" screen follows the exact `RoomChangeController` / Room Change pattern already established in this codebase: a dedicated `@SessionScoped` controller with a `current` (`Admission`) field populated via `f:setPropertyActionListener` from `admission_profile.xhtml`, a `change()` method that saves the encounter and logs an audit event via `AuditService.logEncounterAudit(...)`, and a dedicated page.

**Tech Stack:** Java EE (JSF/PrimeFaces, CDI `@SessionScoped` beans, `@EJB` facades/services), JUnit 5.

**Spec:** `developer_docs/specs/2026-09-29-package-admission-menu-and-change-screen-design.md`

## Global Constraints

- Branch from `origin/development`, target `development` in the PR (CLAUDE.md).
- JSF-only file changes (`.xhtml`) do not require a Maven build/compile step; Java changes do.
- Audit logging must never block the user action: `AuditService.logEncounterAudit(...)` already wraps its body in try/catch internally — callers do not need their own try/catch around it.
- No new billing logic: `InpatientPackageApplicationBean` is not touched or called by the new controller. Changing the package only updates `PatientEncounter.inpatientPackage` and writes an audit event — deliberately, per the approved spec.
- No new persisted entity fields — `PatientEncounter.inpatientPackage` already exists, so no `generate-ddl` run is needed for this plan.
- Never navigate to a page by typing its URL during manual/Playwright verification — reach every page through the menus (CLAUDE.md).
- Privilege constants follow the existing `Privileges.java` enum-with-description convention and are grouped into the same category switch block as the other `"Inward"` privileges.

---

## File Structure

| File | Responsibility |
|---|---|
| `src/main/java/com/divudi/core/data/Privileges.java` | Modify: add `InwardPackageChange` and `InpatientDashboardPanelPackage` privilege constants |
| `src/main/java/com/divudi/bean/inward/AdmissionController.java` | Modify: add `navigateToPackageChange()` |
| `src/main/java/com/divudi/bean/inward/PackageChangeController.java` | Create: new `@SessionScoped` controller — holds `current`/`newPackage`, `change()`, `packageStateMap(...)` |
| `src/test/java/com/divudi/bean/inward/PackageChangeControllerTest.java` | Create: unit test for `packageStateMap(...)` |
| `src/main/webapp/inward/admission_profile.xhtml` | Modify: add new "Package" panel |
| `src/main/webapp/inward/inward_package_change.xhtml` | Create: new page for changing the package |
| `src/main/webapp/resources/ezcomp/menu.xhtml` | Modify: move "Package Admission" menu item from Appointment submenu to Admissions submenu |

---

### Task 1: Add new privileges

**Files:**
- Modify: `src/main/java/com/divudi/core/data/Privileges.java:171` (enum constant, Inward section)
- Modify: `src/main/java/com/divudi/core/data/Privileges.java:196` (enum constant, Inpatient Dashboard Panels section)
- Modify: `src/main/java/com/divudi/core/data/Privileges.java:1684` (category switch, Inward group)
- Modify: `src/main/java/com/divudi/core/data/Privileges.java:1708` (category switch, Inward group)

**Interfaces:**
- Produces: two new `Privileges` enum values, `InwardPackageChange` and `InpatientDashboardPanelPackage`, usable in XHTML as `webUserController.hasPrivilege('InwardPackageChange')` / `webUserController.hasPrivilege('InpatientDashboardPanelPackage')` (Tasks 3–5 consume these string names).

- [ ] **Step 1: Add the `InwardPackageChange` enum constant**

In `src/main/java/com/divudi/core/data/Privileges.java`, find this line (171):
```java
    InwardPackageAdmission("Inward Package Admission"),
```
Add immediately after it:
```java
    InwardPackageAdmission("Inward Package Admission"),
    InwardPackageChange("Inward Package Change"),
```

- [ ] **Step 2: Add the `InpatientDashboardPanelPackage` enum constant**

Find this line (196):
```java
    InpatientDashboardPanelRoomManagement("Inpatient Dashboard - Room Management Panel"),
```
Add immediately after it:
```java
    InpatientDashboardPanelRoomManagement("Inpatient Dashboard - Room Management Panel"),
    InpatientDashboardPanelPackage("Inpatient Dashboard - Package Panel"),
```

- [ ] **Step 3: Add both constants to the `"Inward"` category switch block**

Find this line (1684, inside a long fall-through `switch`/`case` block):
```java
            case InwardPackageAdmission:
```
Add immediately after it:
```java
            case InwardPackageAdmission:
            case InwardPackageChange:
```

Find this line (now shifted by +2, originally 1708):
```java
            case InpatientDashboardPanelRoomManagement:
```
Add immediately after it:
```java
            case InpatientDashboardPanelRoomManagement:
            case InpatientDashboardPanelPackage:
```

- [ ] **Step 4: Compile to verify**

Run: `mvn -q compile -DskipTests`
Expected: `BUILD SUCCESS`, no errors about duplicate or malformed enum constants.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/core/data/Privileges.java
git commit -m "feat(inward): add privileges for package change action and dashboard panel"
```

---

### Task 2: Add navigation method to `AdmissionController`

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/AdmissionController.java:875-880` (insert after `navigateToRoomChange()`)

**Interfaces:**
- Produces: `AdmissionController.navigateToPackageChange()` returning `String`, consumed by the new button in Task 4 (`action="#{admissionController.navigateToPackageChange()}"`).

- [ ] **Step 1: Add the method**

Find this existing method in `src/main/java/com/divudi/bean/inward/AdmissionController.java`:
```java
    public String navigateToRoomChange() {
//        roomChangeController.recreate();
        roomChangeController.createPatientRoom();
        roomChangeController.setInstitution(sessionController.getInstitution());
        return "/inward/inward_room_change?faces-redirect=true";
    }
```
Add immediately after it:
```java

    public String navigateToPackageChange() {
        return "/inward/inward_package_change?faces-redirect=true";
    }
```

No `packageChangeController` injection is needed in `AdmissionController` — the button in Task 4 hands off `admissionController.current` to `packageChangeController.current` directly via `f:setPropertyActionListener` in the XHTML, so this method only needs to return the redirect string, exactly like `navigateToRoomChange()`'s sibling methods that do no prep (e.g. `navigateToRoomOccupancy()`).

- [ ] **Step 2: Compile to verify**

Run: `mvn -q compile -DskipTests`
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/AdmissionController.java
git commit -m "feat(inward): add navigation to the new change-package page"
```

---

### Task 3: Create `PackageChangeController` + unit test

**Files:**
- Create: `src/main/java/com/divudi/bean/inward/PackageChangeController.java`
- Test: `src/test/java/com/divudi/bean/inward/PackageChangeControllerTest.java`

**Interfaces:**
- Consumes: `com.divudi.core.entity.inward.Admission` (has `getInpatientPackage()`/`setInpatientPackage(InpatientPackage)` inherited from `PatientEncounter`), `com.divudi.core.entity.inward.InpatientPackage` (`getName()`, `getAdmissionType()`, `getRoomCategory()`, `getTotalPrice()` returning `Double`), `com.divudi.core.facade.AdmissionFacade` (`.edit(Admission)`), `com.divudi.service.AuditService.logEncounterAudit(PatientEncounter pe, String eventTrigger, Object before, Object after, WebUser user)` (5-arg overload — defaults `entityType` to `"PatientEncounter"` and `objectId` to `pe.getId()`), `com.divudi.core.util.JsfUtil.addSuccessMessage(String)` / `addErrorMessage(String)`.
- Produces: `PackageChangeController.current` (`Admission`, get/set — Task 4/5 bind to this), `PackageChangeController.newPackage` (`InpatientPackage`, get/set — Task 5's autocomplete binds to this), `PackageChangeController.change()` (`void`, Task 5's Save button calls this), `PackageChangeController.removePackage()` (`void`, Task 5's Remove button calls this), `PackageChangeController.packageStateMap(InpatientPackage)` returning `Map<String, Object>` (public instance method, no dependency on injected fields — this is what the unit test below exercises directly).

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/divudi/bean/inward/PackageChangeControllerTest.java`:
```java
package com.divudi.bean.inward;

import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.InpatientPackage;
import com.divudi.core.entity.inward.RoomCategory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PackageChangeControllerTest {

    private final PackageChangeController controller = new PackageChangeController();

    @Test
    void mapsAllFieldsOfAPopulatedPackage() {
        AdmissionType admissionType = new AdmissionType();
        admissionType.setName("Surgical");
        RoomCategory roomCategory = new RoomCategory();
        roomCategory.setName("Normal Ward");

        InpatientPackage pkg = new InpatientPackage();
        pkg.setName("Normal Ward Maternity Package");
        pkg.setAdmissionType(admissionType);
        pkg.setRoomCategory(roomCategory);
        pkg.setTotalPrice(50000.0);

        Map<String, Object> state = controller.packageStateMap(pkg);

        assertEquals("Normal Ward Maternity Package", state.get("package"));
        assertEquals("Surgical", state.get("admissionType"));
        assertEquals("Normal Ward", state.get("roomCategory"));
        assertEquals(50000.0, state.get("totalPrice"));
    }

    @Test
    void mapsNullPackageToAllNullValues() {
        Map<String, Object> state = controller.packageStateMap(null);

        assertNull(state.get("package"));
        assertNull(state.get("admissionType"));
        assertNull(state.get("roomCategory"));
        assertNull(state.get("totalPrice"));
    }

    @Test
    void toleratesPackageWithNullAdmissionTypeAndRoomCategory() {
        InpatientPackage pkg = new InpatientPackage();
        pkg.setName("Unassigned Package");
        pkg.setTotalPrice(1000.0);

        Map<String, Object> state = controller.packageStateMap(pkg);

        assertEquals("Unassigned Package", state.get("package"));
        assertNull(state.get("admissionType"));
        assertNull(state.get("roomCategory"));
        assertEquals(1000.0, state.get("totalPrice"));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q test -Dtest=PackageChangeControllerTest`
Expected: compilation failure — `PackageChangeController` does not exist yet.

- [ ] **Step 3: Create the controller**

Create `src/main/java/com/divudi/bean/inward/PackageChangeController.java`:
```java
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.InpatientPackage;
import com.divudi.core.facade.AdmissionFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.AuditService;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Changes the {@link InpatientPackage} recorded on an existing admission and
 * logs an {@code AuditEvent} for the change. Deliberately does not touch
 * billing (no {@code InpatientPackageApplicationBean} calls) — see
 * developer_docs/specs/2026-09-29-package-admission-menu-and-change-screen-design.md.
 */
@Named
@SessionScoped
public class PackageChangeController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;
    @EJB
    private AuditService auditService;
    @EJB
    private AdmissionFacade ejbFacade;

    private Admission current;
    private InpatientPackage newPackage;

    public Map<String, Object> packageStateMap(InpatientPackage p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("package", p != null ? p.getName() : null);
        m.put("admissionType", p != null && p.getAdmissionType() != null ? p.getAdmissionType().getName() : null);
        m.put("roomCategory", p != null && p.getRoomCategory() != null ? p.getRoomCategory().getName() : null);
        m.put("totalPrice", p != null ? p.getTotalPrice() : null);
        return m;
    }

    public void change() {
        if (current == null) {
            JsfUtil.addErrorMessage("No admission selected.");
            return;
        }
        Map<String, Object> beforeState = packageStateMap(current.getInpatientPackage());
        current.setInpatientPackage(newPackage);
        ejbFacade.edit(current);
        Map<String, Object> afterState = packageStateMap(current.getInpatientPackage());
        auditService.logEncounterAudit(current, "Package Changed", beforeState, afterState,
                sessionController.getLoggedUser());
        JsfUtil.addSuccessMessage("Package updated successfully");
        newPackage = null;
    }

    public void removePackage() {
        newPackage = null;
        change();
    }

    public Admission getCurrent() {
        return current;
    }

    public void setCurrent(Admission current) {
        this.current = current;
    }

    public InpatientPackage getNewPackage() {
        return newPackage;
    }

    public void setNewPackage(InpatientPackage newPackage) {
        this.newPackage = newPackage;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -q test -Dtest=PackageChangeControllerTest`
Expected: `Tests run: 3, Failures: 0, Errors: 0`.

- [ ] **Step 5: Compile the full project to verify no other breakage**

Run: `mvn -q compile -DskipTests`
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/PackageChangeController.java src/test/java/com/divudi/bean/inward/PackageChangeControllerTest.java
git commit -m "feat(inward): add PackageChangeController with audit-logged package change"
```

---

### Task 4: Add "Package" panel to `admission_profile.xhtml`

**Files:**
- Modify: `src/main/webapp/inward/admission_profile.xhtml:327` (insert new panel immediately after the closing `</p:panel>` of the existing "Admission" panel, before the "Billing" panel starts at line 329)

**Interfaces:**
- Consumes: `admissionController.current` (`Admission`, already in scope on this page), `admissionController.current.inpatientPackage` (`InpatientPackage`, via `PatientEncounter.getInpatientPackage()`), `admissionController.navigateToPackageChange()` (from Task 2), `packageChangeController.current` (from Task 3), privileges `InpatientDashboardPanelPackage` / `InwardPackageChange` (from Task 1).

This is a JSF-only change — no compilation needed (CLAUDE.md).

- [ ] **Step 1: Insert the new panel**

Find this exact block in `src/main/webapp/inward/admission_profile.xhtml` (the closing of the "Admission" panel, lines 326-329):
```xml
                            </div>
                        </p:panel>

                        <p:panel header="Billing" class="m-1" rendered="#{admissionController.current.parentEncounter == null and webUserController.hasPrivilege('InpatientDashboardPanelBilling')}">
```
Replace it with:
```xml
                            </div>
                        </p:panel>

                        <p:panel header="Package" class="m-1" rendered="#{webUserController.hasPrivilege('InpatientDashboardPanelPackage')}">
                            <div class="row g-2">
                                <div class="col-12">
                                    <p:outputLabel value="Current Package: " class="fw-bold"/>
                                    <h:outputText value="#{admissionController.current.inpatientPackage.name}" rendered="#{admissionController.current.inpatientPackage ne null}"/>
                                    <h:outputText value="No Package" rendered="#{admissionController.current.inpatientPackage eq null}"/>
                                </div>
                                <div class="col-12">
                                    <p:commandButton
                                        id="btnChangePackage"
                                        rendered="#{webUserController.hasPrivilege('InwardPackageChange')}"
                                        disabled="#{admissionController.current.discharged eq true}"
                                        value="Change Package"
                                        ajax="false"
                                        action="#{admissionController.navigateToPackageChange()}"
                                        icon="fa fa-box-archive"
                                        class="w-100">
                                        <f:setPropertyActionListener
                                            value="#{admissionController.current}"
                                            target="#{packageChangeController.current}" />
                                    </p:commandButton>
                                </div>
                            </div>
                        </p:panel>

                        <p:panel header="Billing" class="m-1" rendered="#{admissionController.current.parentEncounter == null and webUserController.hasPrivilege('InpatientDashboardPanelBilling')}">
```

- [ ] **Step 2: Commit**

```bash
git add src/main/webapp/inward/admission_profile.xhtml
git commit -m "feat(inward): add Package panel with Change Package button to admission dashboard"
```

---

### Task 5: Create `inward_package_change.xhtml`

**Files:**
- Create: `src/main/webapp/inward/inward_package_change.xhtml`

**Interfaces:**
- Consumes: `packageChangeController.current` / `.newPackage` / `.change()` / `.removePackage()` (Task 3), `inpatientPackageAdmissionController.completeInpatientPackage(String query)` (existing method in `InpatientPackageAdmissionController`, reused unchanged — returns `List<InpatientPackage>`), `admissionController.navigateToAdmissionProfilePage` (existing navigation method, same one `inward_room_change.xhtml`'s "Inpatient Dashboard" button uses), the `in:bhtDetail` composite component (`src/main/webapp/resources/inward/bhtDetail.xhtml`, attribute `admission`), privilege `InwardPackageChange` (Task 1).

This is a JSF-only change — no compilation needed (CLAUDE.md).

- [ ] **Step 1: Create the page**

Create `src/main/webapp/inward/inward_package_change.xhtml`:
```xml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE composition PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<ui:composition xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
                template="/resources/template/template.xhtml"
                xmlns:h="http://xmlns.jcp.org/jsf/html"
                xmlns:f="http://xmlns.jcp.org/jsf/core"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:p="http://primefaces.org/ui"
                xmlns:in="http://xmlns.jcp.org/jsf/composite/inward"
                xmlns:na="http://xmlns.jcp.org/jsf/composite/template">

    <ui:define name="content">
        <h:form id="frmPackageChange">
            <h:panelGroup rendered="#{not webUserController.hasPrivilege('InwardPackageChange')}">
                <na:not_authorize />
            </h:panelGroup>

            <h:panelGroup rendered="#{webUserController.hasPrivilege('InwardPackageChange')}" layout="block">
                <p:panel class="my-3" styleClass="shadow-sm">
                    <f:facet name="header">
                        <div class="d-flex justify-content-between align-items-center">
                            <div class="d-flex align-items-center">
                                <i class="fa-solid fa-box-archive fa-lg text-primary me-3"></i>
                                <h4 class="mb-0 text-primary">Change Package</h4>
                            </div>
                            <p:commandButton
                                ajax="false"
                                icon="fa-solid fa-address-card"
                                class="ui-button-secondary"
                                value="Inpatient Dashboard"
                                action="#{admissionController.navigateToAdmissionProfilePage}">
                                <f:setPropertyActionListener
                                    value="#{packageChangeController.current}"
                                    target="#{admissionController.current}" />
                            </p:commandButton>
                        </div>
                    </f:facet>

                    <in:bhtDetail admission="#{packageChangeController.current}"/>

                    <p:panel class="mt-3">
                        <f:facet name="header">
                            <p:outputLabel value="Package"/>
                        </f:facet>

                        <div class="row g-2 align-items-center">
                            <div class="col-md-3">
                                <p:outputLabel value="Current Package"/>
                            </div>
                            <div class="col-md-9">
                                <h:outputText value="#{packageChangeController.current.inpatientPackage.name}" rendered="#{packageChangeController.current.inpatientPackage ne null}"/>
                                <h:outputText value="No Package" rendered="#{packageChangeController.current.inpatientPackage eq null}"/>
                            </div>

                            <div class="col-md-3">
                                <p:outputLabel value="Select New Package"/>
                            </div>
                            <div class="col-md-6">
                                <p:autoComplete
                                    value="#{packageChangeController.newPackage}"
                                    forceSelection="true"
                                    id="acNewPackage"
                                    completeMethod="#{inpatientPackageAdmissionController.completeInpatientPackage}"
                                    var="pkg"
                                    itemLabel="#{pkg.name}"
                                    itemValue="#{pkg}"
                                    maxResults="15"
                                    class="w-100"
                                    placeholder="Search by package name"
                                    inputStyleClass="form-control">
                                    <p:column headerText="Package Name" style="padding: 6px;">#{pkg.name}</p:column>
                                    <p:column headerText="Admission Type" style="padding: 6px;">#{pkg.admissionType.name}</p:column>
                                    <p:column headerText="Total Price" style="padding: 6px;">#{pkg.totalPrice}</p:column>
                                </p:autoComplete>
                            </div>
                            <div class="col-md-3">
                                <p:commandButton
                                    id="btnSavePackageChange"
                                    value="Change"
                                    icon="fa fa-check"
                                    class="ui-button-success w-100"
                                    action="#{packageChangeController.change()}"
                                    ajax="false" />
                            </div>

                            <div class="col-md-3 offset-md-3">
                                <p:commandButton
                                    id="btnRemovePackage"
                                    value="Remove Package"
                                    icon="fa fa-times"
                                    class="ui-button-danger w-100"
                                    action="#{packageChangeController.removePackage()}"
                                    ajax="false" />
                            </div>
                        </div>
                    </p:panel>
                </p:panel>
            </h:panelGroup>
        </h:form>
    </ui:define>
</ui:composition>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/webapp/inward/inward_package_change.xhtml
git commit -m "feat(inward): add Change Package page"
```

---

### Task 6: Relocate the "Package Admission" menu item

**Files:**
- Modify: `src/main/webapp/resources/ezcomp/menu.xhtml:453-461` (Admissions submenu — add item)
- Modify: `src/main/webapp/resources/ezcomp/menu.xhtml:478-484` (Appointment submenu — remove item)

**Interfaces:**
- No new interfaces — reuses the existing `inpatientPackageAdmissionController.navigateToPackageAdmitFromMenu()` action and `InwardPackageAdmission` privilege unchanged.

This is a JSF-only change — no compilation needed (CLAUDE.md).

- [ ] **Step 1: Add the menu item to the Admissions submenu**

Find this exact block (the last item in the Admissions submenu, lines 453-461):
```xml
                        <p:menuitem
                            ajax="false"
                            action="#{admissionPatientChangeController.navigateToChangeAdmissionPatient()}"
                            value="Change Patient for Admission"
                            icon="fa fa-exchange-alt"
                            rendered="#{webUserController.hasPrivilege('InwardAdmissionsEditAdmission')}" >
                        </p:menuitem>

                    </p:submenu>
```
Replace it with:
```xml
                        <p:menuitem
                            ajax="false"
                            action="#{admissionPatientChangeController.navigateToChangeAdmissionPatient()}"
                            value="Change Patient for Admission"
                            icon="fa fa-exchange-alt"
                            rendered="#{webUserController.hasPrivilege('InwardAdmissionsEditAdmission')}" >
                        </p:menuitem>

                        <p:menuitem
                            ajax="false"
                            value="Package Admission"
                            action="#{inpatientPackageAdmissionController.navigateToPackageAdmitFromMenu()}"
                            icon="fa fa-box-archive"
                            rendered="#{webUserController.hasPrivilege('InwardPackageAdmission')}">
                        </p:menuitem>

                    </p:submenu>
```

- [ ] **Step 2: Remove the menu item from the Appointment submenu**

Find this exact block (lines 478-484, now that Step 1 has been applied the surrounding lines are unchanged since it's a different submenu):
```xml
                        <p:menuitem
                            ajax="false"
                            value="Package Admission"
                            action="#{inpatientPackageAdmissionController.navigateToPackageAdmitFromMenu()}"
                            icon="fa fa-box-archive"
                            rendered="#{webUserController.hasPrivilege('InwardPackageAdmission')}">
                        </p:menuitem>
                        <p:menuitem
                            ajax="false"
                            value="Manage Appointment"
```
Replace it with:
```xml
                        <p:menuitem
                            ajax="false"
                            value="Manage Appointment"
```
(This deletes the "Package Admission" `p:menuitem` block that follows "Appointment admission", leaving "Manage Appointment" as the next item — i.e. delete lines 478-484 entirely, keeping everything else in the Appointment submenu unchanged.)

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/resources/ezcomp/menu.xhtml
git commit -m "feat(inward): move Package Admission menu item to Admissions submenu"
```

---

### Task 7: Build, redeploy, and end-to-end verification

**Files:** none (verification only)

- [ ] **Step 1: Full build**

Run: `mvn clean package -DskipTests`
Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Local redeploy**

Follow `playwright-e2e` skill §0a (rebuild/redeploy local code changes) to redeploy the built WAR to local Payara.

- [ ] **Step 3: Menu relocation check (Playwright, never by URL)**

Log in, select a department, navigate **Inpatient → Admissions** — confirm "Package Admission" now appears there. Navigate **Inpatient → Appointment** — confirm it no longer appears there. Click "Package Admission" from Admissions and confirm it still reaches `package_admit.xhtml` and the existing select-patient-and-package flow still works unchanged.

- [ ] **Step 4: Change Package screen — swap an existing package**

Find (via local DB, read-only) an existing admitted encounter that already has an `inpatientPackage` set. Navigate to its `admission_profile.xhtml` through the menus, confirm the new "Package" panel shows the current package name, click "Change Package", confirm the new page shows the correct BHT/patient context via `in:bhtDetail`, pick a different package in the autocomplete, click "Change", confirm a success message and that the panel (after returning to the dashboard) shows the new package.

- [ ] **Step 5: Change Package screen — remove a package**

From the same or another admission with a package set, open Change Package again and click "Remove Package". Confirm the panel now shows "No Package".

- [ ] **Step 6: Change Package screen — add a package to an admission that had none**

Find an admission with no `inpatientPackage`. Confirm its Package panel shows "No Package", open Change Package, pick a package, click "Change", confirm the panel now shows that package.

- [ ] **Step 7: Verify audit events in the local DB**

```sql
SELECT id, eventTrigger, patientEncounterId, beforeJson, afterJson, eventDataTime
FROM AUDITEVENT
WHERE eventTrigger = 'Package Changed'
ORDER BY id DESC
LIMIT 5;
```
Confirm one row per change made in Steps 4-6, with `beforeJson`/`afterJson` reflecting the actual before/after package names, and `patientEncounterId` matching the test admission's id.

- [ ] **Step 8: Confirm no new billing rows were created**

For the encounter used in Step 4 (swap), confirm no new `BILLITEM` rows were created purely by the package change (query `BILLITEM` for that encounter's bills, before/after the change, row count unchanged). This confirms the "record-only, no billing engine calls" boundary from the spec held.

- [ ] **Step 9: Discharged-admission guard check**

Open `admission_profile.xhtml` for a discharged admission and confirm the "Change Package" button is disabled.

- [ ] **Step 10: Final full test suite run**

Run: `mvn test -Dtest=PackageChangeControllerTest,InpatientPackagePricingTest`
Expected: all tests pass (confirms this plan's new test and the pre-existing, unrelated package-pricing tests both still pass).
