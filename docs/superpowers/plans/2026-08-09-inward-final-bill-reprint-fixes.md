# Inward Final Bill Reprint Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix two bugs on the Inward Final Bill reprint page: the "With Professional Fee" checkbox desyncing from the rendered total, and the Custom Bills tab printing all three formats + a full preference checklist unconditionally.

**Architecture:** Two independent fixes on the same XHTML page (`inward_reprint_bill_final.xhtml`) plus its two backing beans (`InwardSearch`, `BhtSummeryController`). No new pages, no schema changes — only new `CONFIGOPTION` rows created lazily on first save, same mechanism already used by the existing `custom2Show*`/`custom4Show*` preferences.

**Tech Stack:** JSF/Facelets (XHTML), PrimeFaces, CDI managed beans (Java EE), `ConfigOptionController` for department-scoped config persistence.

## Global Constraints

- Never modify existing constructors — not applicable here (no constructor changes).
- JPQL first, native SQL last — not applicable here (no queries added).
- Restore local JNDI in `persistence.xml` after every push (already project convention, unrelated to this change but keep in mind before any `git push`).
- Follow JSF conventions: use `h:panelGroup` (never `ui:fragment`) for conditional rendering; `layout="block"` renders as `<div>`, plain `h:panelGroup` renders as `<span>`.
- Prefer `h:selectBooleanCheckbox` over `p:selectBooleanCheckbox` for configuration-dialog checkboxes (documented reliability reason in `developer_docs/configuration/printer-configuration-system.md`) — the two NEW checkboxes added in Task 3 follow this; the existing checkboxes touched in Task 1 are print-preview toggles, not config-dialog toggles, so they stay `p:selectBooleanCheckbox`.
- Reference spec: `docs/superpowers/specs/2026-08-09-inward-final-bill-reprint-fixes-design.md`.

---

### Task 1: Fix the "With Professional Fee" checkbox/total desync

**Files:**
- Modify: `src/main/webapp/inward/inward_reprint_bill_final.xhtml:339,370,406,436,522,557` (remove `onchange` from 6 checkboxes)
- Modify: `src/main/java/com/divudi/bean/inward/InwardSearch.java:467-474` (delete dead `showProfessionalFee()` method)

**Interfaces:**
- Consumes: nothing new — uses the existing `inwardSearch.withProfessionalFee` boolean property (getter/setter already present at `InwardSearch.java:2951,2954`) and the existing `<p:ajax process="@this" update="...">` already on each checkbox.
- Produces: nothing new for later tasks (Task 2/3 are independent of this).

- [ ] **Step 1: Remove `onchange` from the Final Bill tab, Original panel checkbox**

In `src/main/webapp/inward/inward_reprint_bill_final.xhtml`, find:

```xml
                                                            <p:selectBooleanCheckbox
                                                                id="box"
                                                                rendered="#{configOptionApplicationController.getBooleanValueByKey('Inward Final Bill - Disable With Professional Fee in Final Bill',true)}"
                                                                value="#{inwardSearch.withProfessionalFee}"
                                                                itemLabel="With Professional Fee"
                                                                onchange="#{inwardSearch.showProfessionalFee()}">
                                                                <p:ajax process="@this" update="finalOriginalBillPriview"/>
                                                            </p:selectBooleanCheckbox>
```

Replace with:

```xml
                                                            <p:selectBooleanCheckbox
                                                                id="box"
                                                                rendered="#{configOptionApplicationController.getBooleanValueByKey('Inward Final Bill - Disable With Professional Fee in Final Bill',true)}"
                                                                value="#{inwardSearch.withProfessionalFee}"
                                                                itemLabel="With Professional Fee">
                                                                <p:ajax process="@this" update="finalOriginalBillPriview"/>
                                                            </p:selectBooleanCheckbox>
```

- [ ] **Step 2: Remove `onchange` from the Final Bill tab, Duplicate panel checkbox**

Find:

```xml
                                                            <p:selectBooleanCheckbox
                                                                value="#{inwardSearch.withProfessionalFee}"
                                                                itemLabel="With Professional Fee"
                                                                rendered="#{configOptionApplicationController.getBooleanValueByKey('Inward Final Bill - Disable With Professional Fee in Final Bill',true)}"
                                                                onchange="#{inwardSearch.showProfessionalFee()}">
                                                                <p:ajax process="@this" update="finalDuplicateBillPriview"/>
                                                            </p:selectBooleanCheckbox>
```

Replace with:

```xml
                                                            <p:selectBooleanCheckbox
                                                                value="#{inwardSearch.withProfessionalFee}"
                                                                itemLabel="With Professional Fee"
                                                                rendered="#{configOptionApplicationController.getBooleanValueByKey('Inward Final Bill - Disable With Professional Fee in Final Bill',true)}">
                                                                <p:ajax process="@this" update="finalDuplicateBillPriview"/>
                                                            </p:selectBooleanCheckbox>
```

- [ ] **Step 3: Remove `onchange` from the Hospital Bill tab, Original panel checkbox**

Find:

```xml
                                                            <p:selectBooleanCheckbox
                                                                value="#{inwardSearch.withProfessionalFee}"
                                                                itemLabel="With Professional Fee"
                                                                rendered="#{configOptionApplicationController.getBooleanValueByKey('Inward Final Bill - Disable With Professional Fee in Final Bill',true)}"
                                                                onchange="#{inwardSearch.showProfessionalFee()}">
                                                                <p:ajax process="@this" update="originalHospitalBillPriview"/>
                                                            </p:selectBooleanCheckbox>
```

Replace with:

```xml
                                                            <p:selectBooleanCheckbox
                                                                value="#{inwardSearch.withProfessionalFee}"
                                                                itemLabel="With Professional Fee"
                                                                rendered="#{configOptionApplicationController.getBooleanValueByKey('Inward Final Bill - Disable With Professional Fee in Final Bill',true)}">
                                                                <p:ajax process="@this" update="originalHospitalBillPriview"/>
                                                            </p:selectBooleanCheckbox>
```

- [ ] **Step 4: Remove `onchange` from the Hospital Bill tab, Duplicate panel checkbox**

Find:

```xml
                                                            <p:selectBooleanCheckbox
                                                                value="#{inwardSearch.withProfessionalFee}"
                                                                rendered="#{configOptionApplicationController.getBooleanValueByKey('Inward Final Bill - Disable With Professional Fee in Final Bill',true)}"
                                                                itemLabel="With Professional Fee"
                                                                onchange="#{inwardSearch.showProfessionalFee()}">
                                                                <p:ajax process="@this" update="duplicateHospitalBillPriview"/>
                                                            </p:selectBooleanCheckbox>
```

Replace with:

```xml
                                                            <p:selectBooleanCheckbox
                                                                value="#{inwardSearch.withProfessionalFee}"
                                                                rendered="#{configOptionApplicationController.getBooleanValueByKey('Inward Final Bill - Disable With Professional Fee in Final Bill',true)}"
                                                                itemLabel="With Professional Fee">
                                                                <p:ajax process="@this" update="duplicateHospitalBillPriview"/>
                                                            </p:selectBooleanCheckbox>
```

- [ ] **Step 5: Remove `onchange` from the Final Bill Summary tab, Original panel checkbox**

Find:

```xml
                                                            <p:selectBooleanCheckbox
                                                                value="#{inwardSearch.withProfessionalFee}"
                                                                itemLabel="With Professional Fee"
                                                                rendered="#{!configOptionApplicationController.getBooleanValueByKey('Inward Final Bill - Disable With Professional Fee in Final Bill',false)}"
                                                                onchange="#{inwardSearch.showProfessionalFee()}">
                                                                <p:ajax process="@this" update="OriginalFinalBillSummaryPriview"/>
                                                            </p:selectBooleanCheckbox>
```

Replace with:

```xml
                                                            <p:selectBooleanCheckbox
                                                                value="#{inwardSearch.withProfessionalFee}"
                                                                itemLabel="With Professional Fee"
                                                                rendered="#{!configOptionApplicationController.getBooleanValueByKey('Inward Final Bill - Disable With Professional Fee in Final Bill',false)}">
                                                                <p:ajax process="@this" update="OriginalFinalBillSummaryPriview"/>
                                                            </p:selectBooleanCheckbox>
```

Note: the `!...,false)}` (negated, default-false) condition on this one checkbox is pre-existing and out of scope — only the `onchange` is being removed.

- [ ] **Step 6: Remove `onchange` from the Final Bill Summary tab, Duplicate panel checkbox**

Find:

```xml
                                                            <p:selectBooleanCheckbox
                                                                value="#{inwardSearch.withProfessionalFee}"
                                                                itemLabel="With Professional Fee"
                                                                rendered="#{configOptionApplicationController.getBooleanValueByKey('Inward Final Bill - Disable With Professional Fee in Final Bill',true)}"
                                                                onchange="#{inwardSearch.showProfessionalFee()}">
                                                                <p:ajax process="@this" update="duplicateFinalBillSummaryPriview"/>
                                                            </p:selectBooleanCheckbox>
```

Replace with:

```xml
                                                            <p:selectBooleanCheckbox
                                                                value="#{inwardSearch.withProfessionalFee}"
                                                                itemLabel="With Professional Fee"
                                                                rendered="#{configOptionApplicationController.getBooleanValueByKey('Inward Final Bill - Disable With Professional Fee in Final Bill',true)}">
                                                                <p:ajax process="@this" update="duplicateFinalBillSummaryPriview"/>
                                                            </p:selectBooleanCheckbox>
```

- [ ] **Step 7: Verify no other `onchange="#{inwardSearch.showProfessionalFee()}"` remain**

Run:
```bash
grep -n "showProfessionalFee" src/main/webapp/inward/inward_reprint_bill_final.xhtml
```
Expected: no output (all 6 removed).

- [ ] **Step 8: Delete the dead `showProfessionalFee()` method**

In `src/main/java/com/divudi/bean/inward/InwardSearch.java`, find:

```java
    public boolean showProfessionalFee() {
        if (withProfessionalFee == true) {
            withProfessionalFee = false;
        } else {
            withProfessionalFee = true;
        }
        return withProfessionalFee;
    }

```

Delete it entirely (including the trailing blank line shown above, so the method above and `public void edit()` below end up separated by exactly one blank line as before).

- [ ] **Step 9: Confirm no remaining references and compile**

Run:
```bash
grep -rn "showProfessionalFee()" src/main/java/ src/main/webapp/
```
Expected: no output.

Run:
```bash
mvn compile -q
```
Expected: exits with no error output (compiles cleanly).

- [ ] **Step 10: Rebuild, redeploy, and verify in Playwright**

```bash
mvn clean package -DskipTests -q
/home/carecode/payara/glassfish/bin/asadmin --port 9048 redeploy --name rh /home/carecode/development/rh/target/rh-3.0.0.war
```

In Playwright: log in, select the department, navigate to the same BHT's Final Bill reprint page used in the original demo (Inward → search final bill → View Bill). On the **Final Bill** tab:
- Both Original and Duplicate "With Professional Fee" checkboxes load **checked**.
- Both totals show the amount **including** the professional charge (matches the demo data: 4,750.00 = 500 admission + 1,250 room + 3,000 professional).
- Uncheck Original's checkbox → its own total updates to exclude the professional fee (1,750.00) while Duplicate stays checked at 4,750.00.
- Repeat the checked-on-load + matching-total check on the **Hospital Bill** and **Final Bill Summary** tabs.

- [ ] **Step 11: Commit**

```bash
git add src/main/webapp/inward/inward_reprint_bill_final.xhtml src/main/java/com/divudi/bean/inward/InwardSearch.java
git commit -m "fix(inward): stop professional-fee checkbox from desyncing final bill totals

onchange=\"#{inwardSearch.showProfessionalFee()}\" on p:selectBooleanCheckbox
is a plain HTML pass-through attribute, so JSF evaluated it at render time
(once per each of the 6 checkbox occurrences on the page) instead of on an
actual browser change event. Each render silently flipped the shared
withProfessionalFee field as a side effect before that panel's own bill
preview re-read the same field, producing an inverted/stale total vs. the
checkbox's own visible checked state.

Removed the onchange (the existing p:ajax already commits the checkbox's
value through the normal JSF lifecycle) and deleted the now-dead toggle
method."
```

---

### Task 2: Add custom-bill-format visibility config to `BhtSummeryController`

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/BhtSummeryController.java:221-234` (new fields)
- Modify: `src/main/java/com/divudi/bean/inward/BhtSummeryController.java:475-504` (new load/save helpers, wired into existing methods)
- Modify: `src/main/java/com/divudi/bean/inward/BhtSummeryController.java:584-623` (new getters/setters)

**Interfaces:**
- Consumes: existing `configOptionController` field (already injected at `BhtSummeryController.java:165`, type `com.divudi.bean.common.ConfigOptionController`), existing `loadCustom2Config()` / `saveCustom2Config()` methods (already wired to the Custom Bills Settings dialog's `p:ajax event="open"` and "Apply & Close" button).
- Produces (for Task 3's XHTML to bind against):
  - `boolean isShowCustomBill2Format()` / `void setShowCustomBill2Format(boolean)`
  - `boolean isShowCustomBill3Format()` / `void setShowCustomBill3Format(boolean)`
  - `boolean isShowCustomBill4Format()` / `void setShowCustomBill4Format(boolean)`
  - Config keys read/written: `"Inward Final Bill - Show Custom Bill 2 Format"` (default `true`), `"Inward Final Bill - Show Custom Bill 3 Format"` (default `false`), `"Inward Final Bill - Show Custom Bill 4 Format"` (default `false`).

- [ ] **Step 1: Add the 3 new fields**

In `src/main/java/com/divudi/bean/inward/BhtSummeryController.java`, find:

```java
    // Custom4 (Custom Bills tab - letterhead) print-format settings
    private boolean custom4ShowAddress;
    private boolean custom4ShowNic;
    private boolean custom4ShowPhone;
    private boolean custom4ShowGuardian;
    private boolean custom4ShowCorporateSponsor;
    @Inject
    private InwardMemberShipDiscount inwardMemberShipDiscount;
```

Replace with:

```java
    // Custom4 (Custom Bills tab - letterhead) print-format settings
    private boolean custom4ShowAddress;
    private boolean custom4ShowNic;
    private boolean custom4ShowPhone;
    private boolean custom4ShowGuardian;
    private boolean custom4ShowCorporateSponsor;
    // Custom Bills tab - which custom format(s) are shown to end users.
    // An admin flips these via the Settings dialog; end users never pick a
    // format at print time (department-wide choice, not a per-print option).
    private boolean showCustomBill2Format;
    private boolean showCustomBill3Format;
    private boolean showCustomBill4Format;
    @Inject
    private InwardMemberShipDiscount inwardMemberShipDiscount;
```

- [ ] **Step 2: Add load/persist helpers and wire them into the existing load/save methods**

Find:

```java
    // <editor-fold defaultstate="collapsed" desc="Custom Bills tab - Custom2 print format">
    public void loadCustom2Config() {
        custom2ShowAddress = configOptionController.getBooleanValueByKey("Inward Final Bill Custom2 - Show Patient Address", false);
        custom2ShowNic = configOptionController.getBooleanValueByKey("Inward Final Bill Custom2 - Show Patient NIC", false);
        custom2ShowPhone = configOptionController.getBooleanValueByKey("Inward Final Bill Custom2 - Show Patient Phone", false);
        custom2ShowGuardian = configOptionController.getBooleanValueByKey("Inward Final Bill Custom2 - Show Guardian", true);
        custom2ShowCorporateSponsor = configOptionController.getBooleanValueByKey("Inward Final Bill Custom2 - Show Corporate Sponsor", true);
        // Custom Bills tab shares one settings dialog across Custom2 and Custom4 (letterhead)
        loadCustom4Config();
    }

    public void saveCustom2Config() {
        if (!webUserController.hasPrivilege("ChangeReceiptPrintingPaperTypes")) {
            JsfUtil.addErrorMessage("You do not have privilege to change Custom Bills configuration");
            return;
        }
        try {
            configOptionController.setBooleanValueByKey("Inward Final Bill Custom2 - Show Patient Address", custom2ShowAddress);
            configOptionController.setBooleanValueByKey("Inward Final Bill Custom2 - Show Patient NIC", custom2ShowNic);
            configOptionController.setBooleanValueByKey("Inward Final Bill Custom2 - Show Patient Phone", custom2ShowPhone);
            configOptionController.setBooleanValueByKey("Inward Final Bill Custom2 - Show Guardian", custom2ShowGuardian);
            configOptionController.setBooleanValueByKey("Inward Final Bill Custom2 - Show Corporate Sponsor", custom2ShowCorporateSponsor);
            // Custom Bills tab shares one settings dialog across Custom2 and Custom4 (letterhead)
            persistCustom4Config();
            JsfUtil.addSuccessMessage("Custom Bills configuration saved successfully");
            loadCustom2Config();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Custom Bills configuration: " + e.getMessage());
        }
    }
```

Replace with:

```java
    // <editor-fold defaultstate="collapsed" desc="Custom Bills tab - Custom2 print format">
    private void loadCustomBillFormatVisibility() {
        showCustomBill2Format = configOptionController.getBooleanValueByKey("Inward Final Bill - Show Custom Bill 2 Format", true);
        showCustomBill3Format = configOptionController.getBooleanValueByKey("Inward Final Bill - Show Custom Bill 3 Format", false);
        showCustomBill4Format = configOptionController.getBooleanValueByKey("Inward Final Bill - Show Custom Bill 4 Format", false);
    }

    private void persistCustomBillFormatVisibility() {
        configOptionController.setBooleanValueByKey("Inward Final Bill - Show Custom Bill 2 Format", showCustomBill2Format);
        configOptionController.setBooleanValueByKey("Inward Final Bill - Show Custom Bill 3 Format", showCustomBill3Format);
        configOptionController.setBooleanValueByKey("Inward Final Bill - Show Custom Bill 4 Format", showCustomBill4Format);
    }

    public void loadCustom2Config() {
        loadCustomBillFormatVisibility();
        custom2ShowAddress = configOptionController.getBooleanValueByKey("Inward Final Bill Custom2 - Show Patient Address", false);
        custom2ShowNic = configOptionController.getBooleanValueByKey("Inward Final Bill Custom2 - Show Patient NIC", false);
        custom2ShowPhone = configOptionController.getBooleanValueByKey("Inward Final Bill Custom2 - Show Patient Phone", false);
        custom2ShowGuardian = configOptionController.getBooleanValueByKey("Inward Final Bill Custom2 - Show Guardian", true);
        custom2ShowCorporateSponsor = configOptionController.getBooleanValueByKey("Inward Final Bill Custom2 - Show Corporate Sponsor", true);
        // Custom Bills tab shares one settings dialog across Custom2 and Custom4 (letterhead)
        loadCustom4Config();
    }

    public void saveCustom2Config() {
        if (!webUserController.hasPrivilege("ChangeReceiptPrintingPaperTypes")) {
            JsfUtil.addErrorMessage("You do not have privilege to change Custom Bills configuration");
            return;
        }
        try {
            persistCustomBillFormatVisibility();
            configOptionController.setBooleanValueByKey("Inward Final Bill Custom2 - Show Patient Address", custom2ShowAddress);
            configOptionController.setBooleanValueByKey("Inward Final Bill Custom2 - Show Patient NIC", custom2ShowNic);
            configOptionController.setBooleanValueByKey("Inward Final Bill Custom2 - Show Patient Phone", custom2ShowPhone);
            configOptionController.setBooleanValueByKey("Inward Final Bill Custom2 - Show Guardian", custom2ShowGuardian);
            configOptionController.setBooleanValueByKey("Inward Final Bill Custom2 - Show Corporate Sponsor", custom2ShowCorporateSponsor);
            // Custom Bills tab shares one settings dialog across Custom2 and Custom4 (letterhead)
            persistCustom4Config();
            JsfUtil.addSuccessMessage("Custom Bills configuration saved successfully");
            loadCustom2Config();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Custom Bills configuration: " + e.getMessage());
        }
    }
```

- [ ] **Step 3: Add getters/setters**

Find:

```java
    public boolean isCustom2ShowCorporateSponsor() {
        return custom2ShowCorporateSponsor;
    }

    public void setCustom2ShowCorporateSponsor(boolean custom2ShowCorporateSponsor) {
        this.custom2ShowCorporateSponsor = custom2ShowCorporateSponsor;
    }
    // </editor-fold>
```

Replace with:

```java
    public boolean isCustom2ShowCorporateSponsor() {
        return custom2ShowCorporateSponsor;
    }

    public void setCustom2ShowCorporateSponsor(boolean custom2ShowCorporateSponsor) {
        this.custom2ShowCorporateSponsor = custom2ShowCorporateSponsor;
    }

    public boolean isShowCustomBill2Format() {
        return showCustomBill2Format;
    }

    public void setShowCustomBill2Format(boolean showCustomBill2Format) {
        this.showCustomBill2Format = showCustomBill2Format;
    }

    public boolean isShowCustomBill3Format() {
        return showCustomBill3Format;
    }

    public void setShowCustomBill3Format(boolean showCustomBill3Format) {
        this.showCustomBill3Format = showCustomBill3Format;
    }

    public boolean isShowCustomBill4Format() {
        return showCustomBill4Format;
    }

    public void setShowCustomBill4Format(boolean showCustomBill4Format) {
        this.showCustomBill4Format = showCustomBill4Format;
    }
    // </editor-fold>
```

- [ ] **Step 4: Compile**

```bash
mvn compile -q
```
Expected: exits with no error output.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/BhtSummeryController.java
git commit -m "feat(inward): add custom-bill-format visibility config to BhtSummeryController

Three new department-scoped config keys (Inward Final Bill - Show Custom
Bill 2/3/4 Format) let an admin choose which of the three Custom Bills
formats a department actually prints, instead of all three always
rendering. Wired into the existing loadCustom2Config()/saveCustom2Config()
so the one Settings dialog on inward_reprint_bill_final.xhtml continues to
load/save everything in one round-trip.

No XHTML wiring yet - that is the next commit."
```

---

### Task 3: Gate Custom Bills tab formats + add format toggles to Settings dialog

**Files:**
- Modify: `src/main/webapp/inward/inward_reprint_bill_final.xhtml:608-839` (Custom Bills tab)

**Interfaces:**
- Consumes: `bhtSummeryController.isShowCustomBill2Format()/isShowCustomBill3Format()/isShowCustomBill4Format()` and their setters (from Task 2), plus the existing `configOptionController` EL usage pattern already on the page (used the same way as `configOptionApplicationController` elsewhere on this page, just a different injected controller name in EL — `configOptionController` is already used by other pages per `developer_docs/configuration/printer-configuration-system.md` and is available as a managed bean, no new injection needed since this is pure XHTML/EL).
- Produces: nothing further downstream.

- [ ] **Step 1: Wrap the "Custom Bill" (Custom2) row in a rendered `h:panelGroup`**

Find (opening boundary):

```xml
                                    <div class="row">
                                        <div class="col-lg-6">
                                            <p:panel>
                                                <f:facet name="header">
                                                    <div class="d-flex justify-content-between">
                                                        <div><h:outputLabel value="Original" class="mt-2"/></div>
                                                        <div>
                                                            <p:commandButton
                                                                icon="fa fa-print"
                                                                value="Custom Bill"
                                                                class="ui-button-info"
                                                                ajax="false">
                                                                <p:printer target="originalCustom2BillPriview" ></p:printer>
```

Replace with:

```xml
                                    <h:panelGroup layout="block" rendered="#{bhtSummeryController.showCustomBill2Format}">
                                    <div class="row">
                                        <div class="col-lg-6">
                                            <p:panel>
                                                <f:facet name="header">
                                                    <div class="d-flex justify-content-between">
                                                        <div><h:outputLabel value="Original" class="mt-2"/></div>
                                                        <div>
                                                            <p:commandButton
                                                                icon="fa fa-print"
                                                                value="Custom Bill"
                                                                class="ui-button-info"
                                                                ajax="false">
                                                                <p:printer target="originalCustom2BillPriview" ></p:printer>
```

Find (closing boundary):

```xml
                                                <h:panelGroup id="duplicateCustom2BillPriview">
                                                    <bi:finalBillCustom2 bill="#{inwardSearch.showOrginalBill ? inwardSearch.bill.referenceBill : inwardSearch.bill}" duplicate="true"/>
                                                </h:panelGroup>

                                            </p:panel>
                                        </div>
                                    </div>
```

Replace with:

```xml
                                                <h:panelGroup id="duplicateCustom2BillPriview">
                                                    <bi:finalBillCustom2 bill="#{inwardSearch.showOrginalBill ? inwardSearch.bill.referenceBill : inwardSearch.bill}" duplicate="true"/>
                                                </h:panelGroup>

                                            </p:panel>
                                        </div>
                                    </div>
                                    </h:panelGroup>
```

- [ ] **Step 2: Wrap the "Custom Bill 2" (Custom3) row in a rendered `h:panelGroup`**

Find (opening boundary):

```xml
                                    <div class="row mt-3">
                                        <div class="col-lg-6">
                                            <p:panel>
                                                <f:facet name="header">
                                                    <div class="d-flex justify-content-between">
                                                        <div><h:outputLabel value="Original" class="mt-2"/></div>
                                                        <div>
                                                            <p:commandButton
                                                                icon="fa fa-print"
                                                                value="Custom Bill 2"
                                                                class="ui-button-info"
                                                                ajax="false">
                                                                <p:printer target="originalCustom3BillPriview" ></p:printer>
```

Replace with:

```xml
                                    <h:panelGroup layout="block" rendered="#{bhtSummeryController.showCustomBill3Format}">
                                    <div class="row mt-3">
                                        <div class="col-lg-6">
                                            <p:panel>
                                                <f:facet name="header">
                                                    <div class="d-flex justify-content-between">
                                                        <div><h:outputLabel value="Original" class="mt-2"/></div>
                                                        <div>
                                                            <p:commandButton
                                                                icon="fa fa-print"
                                                                value="Custom Bill 2"
                                                                class="ui-button-info"
                                                                ajax="false">
                                                                <p:printer target="originalCustom3BillPriview" ></p:printer>
```

Find (closing boundary):

```xml
                                                <h:panelGroup id="duplicateCustom3BillPriview">
                                                    <bi:finalBillCustom3 bill="#{inwardSearch.showOrginalBill ? inwardSearch.bill.referenceBill : inwardSearch.bill}" duplicate="true"/>
                                                </h:panelGroup>

                                            </p:panel>
                                        </div>
                                    </div>
```

Replace with:

```xml
                                                <h:panelGroup id="duplicateCustom3BillPriview">
                                                    <bi:finalBillCustom3 bill="#{inwardSearch.showOrginalBill ? inwardSearch.bill.referenceBill : inwardSearch.bill}" duplicate="true"/>
                                                </h:panelGroup>

                                            </p:panel>
                                        </div>
                                    </div>
                                    </h:panelGroup>
```

- [ ] **Step 3: Wrap the "Custom Bill 3 (Letterhead)" (Custom4) row in a rendered `h:panelGroup`**

Find (opening boundary):

```xml
                                    <div class="row mt-3">
                                        <div class="col-lg-6">
                                            <p:panel>
                                                <f:facet name="header">
                                                    <div class="d-flex justify-content-between">
                                                        <div><h:outputLabel value="Original" class="mt-2"/></div>
                                                        <div>
                                                            <p:commandButton
                                                                icon="fa fa-print"
                                                                value="Custom Bill 3"
                                                                class="ui-button-info"
                                                                type="button"
                                                                onclick="printCustom4Bill('#{p:resolveFirstComponentWithId('originalCustom4BillPriview',view).clientId}', '#{resource['js:paged.polyfill.js']}'); return false;" />
```

Replace with:

```xml
                                    <h:panelGroup layout="block" rendered="#{bhtSummeryController.showCustomBill4Format}">
                                    <div class="row mt-3">
                                        <div class="col-lg-6">
                                            <p:panel>
                                                <f:facet name="header">
                                                    <div class="d-flex justify-content-between">
                                                        <div><h:outputLabel value="Original" class="mt-2"/></div>
                                                        <div>
                                                            <p:commandButton
                                                                icon="fa fa-print"
                                                                value="Custom Bill 3"
                                                                class="ui-button-info"
                                                                type="button"
                                                                onclick="printCustom4Bill('#{p:resolveFirstComponentWithId('originalCustom4BillPriview',view).clientId}', '#{resource['js:paged.polyfill.js']}'); return false;" />
```

Find (closing boundary):

```xml
                                                <h:panelGroup id="duplicateCustom4BillPriview">
                                                    <bi:finalBillCustom4 bill="#{inwardSearch.showOrginalBill ? inwardSearch.bill.referenceBill : inwardSearch.bill}" duplicate="true" showProfessional="#{inwardSearch.withProfessionalFee}"/>
                                                </h:panelGroup>

                                            </p:panel>
                                        </div>
                                    </div>
```

Replace with:

```xml
                                                <h:panelGroup id="duplicateCustom4BillPriview">
                                                    <bi:finalBillCustom4 bill="#{inwardSearch.showOrginalBill ? inwardSearch.bill.referenceBill : inwardSearch.bill}" duplicate="true" showProfessional="#{inwardSearch.withProfessionalFee}"/>
                                                </h:panelGroup>

                                            </p:panel>
                                        </div>
                                    </div>
                                    </h:panelGroup>
```

Note: `p:resolveFirstComponentWithId('originalCustom4BillPriview',view)` in the print `onclick` still resolves correctly once the panel is wrapped — `h:panelGroup rendered="false"` removes the component from the tree entirely when off, but when `showCustomBill4Format` is `true` (which it must be for the user to be looking at/clicking this button) the component is present and resolves normally.

- [ ] **Step 4: Add the 3 format-visibility checkboxes to the Settings dialog**

Find:

```xml
                                        <h:form id="custom2ConfigForm" rendered="#{webUserController.hasPrivilege('ChangeReceiptPrintingPaperTypes')}">
                                            <div class="card-body">
                                                <div class="mb-3">
                                                    <h:selectBooleanCheckbox id="custom2ShowAddress" value="#{bhtSummeryController.custom2ShowAddress}" />
                                                    <h:outputLabel for="custom2ShowAddress" value="Show Patient Address" class="ms-2" />
                                                </div>
```

Replace with:

```xml
                                        <h:form id="custom2ConfigForm" rendered="#{webUserController.hasPrivilege('ChangeReceiptPrintingPaperTypes')}">
                                            <div class="card-body">
                                                <h6>Which formats print</h6>
                                                <div class="mb-3">
                                                    <h:selectBooleanCheckbox id="showCustomBill2Format" value="#{bhtSummeryController.showCustomBill2Format}" />
                                                    <h:outputLabel for="showCustomBill2Format" value="Show &quot;Custom Bill&quot; format" class="ms-2" />
                                                </div>
                                                <div class="mb-3">
                                                    <h:selectBooleanCheckbox id="showCustomBill3Format" value="#{bhtSummeryController.showCustomBill3Format}" />
                                                    <h:outputLabel for="showCustomBill3Format" value="Show &quot;Custom Bill 2&quot; format" class="ms-2" />
                                                </div>
                                                <div class="mb-3">
                                                    <h:selectBooleanCheckbox id="showCustomBill4Format" value="#{bhtSummeryController.showCustomBill4Format}" />
                                                    <h:outputLabel for="showCustomBill4Format" value="Show &quot;Custom Bill 3 (Letterhead)&quot; format" class="ms-2" />
                                                </div>
                                                <hr/>
                                                <h:panelGroup rendered="#{bhtSummeryController.showCustomBill2Format}">
                                                <div class="mb-3">
                                                    <h:selectBooleanCheckbox id="custom2ShowAddress" value="#{bhtSummeryController.custom2ShowAddress}" />
                                                    <h:outputLabel for="custom2ShowAddress" value="Show Patient Address" class="ms-2" />
                                                </div>
```

- [ ] **Step 5: Close the Custom2 preferences `h:panelGroup` and gate the Custom4 ("Custom Bill 3 (Letterhead)") preferences block**

Find:

```xml
                                                <div class="mb-3">
                                                    <h:selectBooleanCheckbox id="custom2ShowCorporateSponsor" value="#{bhtSummeryController.custom2ShowCorporateSponsor}" />
                                                    <h:outputLabel for="custom2ShowCorporateSponsor" value="Show Corporate Sponsor" class="ms-2" />
                                                </div>

                                                <hr/>
                                                <h6>Custom Bill 3 (Letterhead) Settings</h6>
                                                <div class="mb-3">
                                                    <h:selectBooleanCheckbox id="custom4ShowAddress" value="#{bhtSummeryController.custom4ShowAddress}" />
                                                    <h:outputLabel for="custom4ShowAddress" value="Show Patient Address" class="ms-2" />
                                                </div>
```

Replace with:

```xml
                                                <div class="mb-3">
                                                    <h:selectBooleanCheckbox id="custom2ShowCorporateSponsor" value="#{bhtSummeryController.custom2ShowCorporateSponsor}" />
                                                    <h:outputLabel for="custom2ShowCorporateSponsor" value="Show Corporate Sponsor" class="ms-2" />
                                                </div>
                                                </h:panelGroup>

                                                <h:panelGroup rendered="#{bhtSummeryController.showCustomBill4Format}">
                                                <hr/>
                                                <h6>Custom Bill 3 (Letterhead) Settings</h6>
                                                <div class="mb-3">
                                                    <h:selectBooleanCheckbox id="custom4ShowAddress" value="#{bhtSummeryController.custom4ShowAddress}" />
                                                    <h:outputLabel for="custom4ShowAddress" value="Show Patient Address" class="ms-2" />
                                                </div>
```

- [ ] **Step 6: Close the Custom4 preferences `h:panelGroup`**

Find:

```xml
                                                <div class="mb-3">
                                                    <h:selectBooleanCheckbox id="custom4ShowCorporateSponsor" value="#{bhtSummeryController.custom4ShowCorporateSponsor}" />
                                                    <h:outputLabel for="custom4ShowCorporateSponsor" value="Show Corporate Sponsor" class="ms-2" />
                                                </div>

                                                <p:messages id="custom2ConfigMessages" showDetail="true" closable="true" />
```

Replace with:

```xml
                                                <div class="mb-3">
                                                    <h:selectBooleanCheckbox id="custom4ShowCorporateSponsor" value="#{bhtSummeryController.custom4ShowCorporateSponsor}" />
                                                    <h:outputLabel for="custom4ShowCorporateSponsor" value="Show Corporate Sponsor" class="ms-2" />
                                                </div>
                                                </h:panelGroup>

                                                <p:messages id="custom2ConfigMessages" showDetail="true" closable="true" />
```

- [ ] **Step 7: Sanity-check the file is well-formed XML**

Run:
```bash
xmllint --noout src/main/webapp/inward/inward_reprint_bill_final.xhtml
```
Expected: no output (no unclosed-tag errors). If `xmllint` is not installed, instead visually recount: every `<h:panelGroup ...>` opened in Steps 1-6 must have exactly one matching `</h:panelGroup>` — 6 new panelGroups added total (3 format rows + 2 preference blocks... wait, that's 5: 3 row wraps from Steps 1-3, plus the Custom2-preferences wrap opened in Step 4/closed in Step 5, plus the Custom4-preferences wrap opened in Step 5/closed in Step 6 — 5 new open/close pairs in total).

- [ ] **Step 8: Rebuild, redeploy, and verify in Playwright**

```bash
mvn clean package -DskipTests -q
/home/carecode/payara/glassfish/bin/asadmin --port 9048 redeploy --name rh /home/carecode/development/rh/target/rh-3.0.0.war
```

In Playwright, same BHT as before, Custom Bills tab:
- Only the "Custom Bill" (Custom2) format row renders by default (Custom Bill 2 / Custom Bill 3 (Letterhead) rows are gone).
- Click Settings → dialog shows 3 new checkboxes at top ("Show \"Custom Bill\" format" checked, the other two unchecked) followed by only the "Custom Bill" preferences (5 toggles) — no "Custom Bill 3 (Letterhead) Settings" section.
- Check "Show \"Custom Bill 3 (Letterhead)\" format" → Apply & Close (full postback) → both the "Custom Bill" row and the "Custom Bill 3 (Letterhead)" row now render; re-opening Settings now also shows the Custom Bill 3 (Letterhead) preference toggles.
- Uncheck "Show \"Custom Bill\" format" → Apply & Close → only "Custom Bill 3 (Letterhead)" row renders.

- [ ] **Step 9: Commit**

```bash
git add src/main/webapp/inward/inward_reprint_bill_final.xhtml
git commit -m "fix(inward): gate Custom Bills tab formats behind per-format config

Matches the documented pattern in
developer_docs/configuration/printer-configuration-system.md (config-gated
rendered, not a runtime dropdown - that pattern is explicitly deprecated).

All three custom bill formats (Custom Bill / Custom Bill 2 / Custom Bill 3
Letterhead) previously rendered unconditionally, stacked, regardless of
which one a department actually uses. Each format's row is now wrapped in
rendered=\"#{bhtSummeryController.showCustomBillNFormat}\", defaulting to
only \"Custom Bill\" (Custom2) on so existing departments keep seeing what
they see today. The Settings dialog gained 3 toggles to pick which
format(s) are active, and its per-format preference sections (address/
NIC/phone/guardian/sponsor) now only show for the currently active
format(s) instead of always showing both Custom2's and Custom4's full
10-checkbox set."
```

---

### Task 4: Combined end-to-end verification

**Files:** none (verification only).

**Interfaces:**
- Consumes: the fully assembled page from Tasks 1-3.
- Produces: nothing — this is the final sign-off before considering the fixes done.

- [ ] **Step 1: Full rebuild and redeploy**

```bash
mvn clean package -DskipTests -q
/home/carecode/payara/glassfish/bin/asadmin --port 9048 redeploy --name rh /home/carecode/development/rh/target/rh-3.0.0.war
```

Expected: deploy succeeds with no errors; check `tail -100 /home/carecode/payara/glassfish/domains/rh/logs/server.log | grep -iE "error|exception|severe"` comes back empty.

- [ ] **Step 2: Walk through the exact scenario from the original live demo**

In Playwright: log in, select department, open the same BHT's Final Bill reprint page (`inward_reprint_bill_final.xhtml`) used throughout this work.

- Final Bill tab: Original and Duplicate both load checked, both totals include the professional fee, toggling either independently updates only that panel.
- Custom Bills tab: only "Custom Bill" format shows by default; Settings dialog is uncluttered (3 format toggles + 5 preference toggles for the one active format).
- Hospital Bill and Final Bill Summary tabs: professional-fee checkboxes behave the same as Final Bill tab (checked on load, matching totals).
- Credit Company Letter and Professional Bill tabs (untouched by this work): still render as before — quick regression check that nothing else on the page broke.

- [ ] **Step 3: Confirm final git state**

```bash
git status
git log --oneline -5
```

Expected: working tree clean except the routine unstaged `persistence.xml` local-JNDI diff (per `CLAUDE.md` § persistence.xml lifecycle — restore local JNDI after every push, leave unstaged); the last 3 commits are the Task 1/2/3 commits from this plan.

No further commit needed for this task — it is verification-only.
