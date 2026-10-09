# PVC Card Print Configuration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the patient page's "Print Card" button into independent Print Front / Print Back actions, and make card size, field positions/visibility, barcode settings, and background images configurable per deployment via an admin page, with zero code changes required for future tuning.

**Architecture:** Two new plain-Java value classes (`PvcCardSlot`, `PvcCardLayout`) hold the per-side layout as JSON, parsed/serialized with the project's existing `org.json` dependency and falling back to built-in defaults on any missing/invalid data. A new `@ViewScoped` CDI bean (`PvcCardLayoutController`) loads/saves that JSON via the existing `ConfigOptionApplicationController` (two LONG_TEXT keys) and resolves front/back background images via the existing `Upload` entity (two new `UploadType` values), reusing the codebase's established blob-or-URL convention. A single reusable composite component renders a card side from a `PvcCardLayout` plus the real patient/session data, so the print panel on `opd/patient.xhtml` and the live preview on the new admin page can never drift apart.

**Tech Stack:** Java EE 8 (`javax.*`), JSF 2.x + PrimeFaces 14.0.6, EclipseLink JPA, `org.json` for JSON, JUnit 5 for unit tests, Maven build, Payara app server.

**Spec:** `developer_docs/specs/2026-10-04-pvc-card-print-config-design.md`

> **Post-implementation note (2026-10-05):** the composite's background-streaming attribute described below as `backgroundStream` (a `StreamedContent` bound to `cc.attrs`) was replaced with a `side` (`String`) attribute, because PrimeFaces's secondary image-resource request cannot reliably re-resolve `cc.attrs.*` or reactivate a `@ViewScoped` bean's context. The image is now streamed by a new `@RequestScoped` `PvcCardBackgroundViewController`, selecting front/back via an `f:param name="side"` baked into the resource URL at render time (mirroring the existing `UploadViewController` pattern). Code blocks below still show `backgroundStream` as originally planned — treat `side` as the actual, as-built attribute.

## Global Constraints

- Namespace is `javax.faces`/`javax.persistence` (pre-Jakarta) throughout — do not use `jakarta.*` imports.
- PrimeFaces version is 14.0.6 (`pom.xml`) — `<p:barcode>`/`<p:fileUpload>`/`<p:graphicImage>` attributes must match this version's API.
- `ConfigOptionApplicationController` config is **app-wide**, not department-scoped — per explicit user instruction, do not use `ConfigOptionController` (department-scoped) for this feature.
- No new JPA entity and no schema migration — reuse the existing `Upload` entity (`core/entity/Upload.java`) plus two new `UploadType` enum constants, and the existing `ConfigOption` table via `ConfigOptionApplicationController`'s LONG_TEXT accessors.
- JSON (de)serialization uses `org.json.JSONObject` (already a `pom.xml` dependency, already used elsewhere e.g. `com.divudi.ejb.SmsManagerEjb`) — do not add Jackson/Gson for this feature.
- The barcode always encodes the patient's PHN (`patientController.current.phn`) — barcode *content* is never configurable, only type/width/height/position.
- Layout JSON stores position/size/visibility/style only, never patient/institution data — actual values are bound live via EL (`patientController.current...`, `sessionController.institution.name`, `sessionController.department.name`).
- Playwright verification must navigate only by clicking through menus, never by typing an inner page URL (project convention) — see `developer_docs/testing/playwright-e2e-workflow.md`.

## Review Focus

- **Fresh deployment, no ConfigOption rows yet exist for either layout key** — expected: card renders with the built-in default layout, not blank/broken. Covered by Task 2's `fromJson("")`/`fromJson(null)` tests and Task 7's Playwright check against a database with no `PVC Card Front/Back Layout` rows.
- **Layout JSON present but malformed (hand-edited bad JSON, or a future schema change)** — expected: falls back to defaults rather than throwing and breaking the whole patient page. Covered by Task 2's malformed-JSON test.
- **No `Upload` row exists yet for a side's background** (first deployment, nobody has uploaded anything) — expected: card renders with no background image, text/barcode still positioned correctly, no NPE. Covered by Task 3's background-resolution tests and Task 7's Playwright check.
- **Switching a background from an uploaded file to an external URL (or back)** — expected: the old value is cleared so the card doesn't keep rendering stale/duplicate content. Covered by Task 3's upload/URL save tests.
- **A visible slot whose underlying patient field is null** (e.g. `address` turned on for a patient with no address on file) — expected: that slot renders blank, not an error. Covered by Task 7's Playwright check using a patient known to have a null address field.

---

## Task 1: `PvcCardSlot` data class

**Files:**
- Create: `src/main/java/com/divudi/core/data/PvcCardSlot.java`
- Test: `src/test/java/com/divudi/core/data/PvcCardSlotTest.java`

**Interfaces:**
- Produces: `PvcCardSlot` — a mutable POJO with `visible` (boolean), `leftMm`/`topMm`/`fontSizePt`/`widthMm`/`heightMm` (double), `fontColor`/`type` (String); a no-arg constructor (for JSON round-tripping and JSF EL mutation), a convenience constructor `PvcCardSlot(boolean visible, double leftMm, double topMm, double fontSizePt, String fontColor)`, a static factory `PvcCardSlot.barcodeSlot(boolean visible, double leftMm, double topMm, double widthMm, double heightMm, String type)`, a static `PvcCardSlot.fromJson(JSONObject json)`, and an instance `toJson()` returning `org.json.JSONObject`. Used by Task 2's `PvcCardLayout`.

- [ ] **Step 1: Write the failing test**

```java
package com.divudi.core.data;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvcCardSlotTest {

    @Test
    void roundTripsThroughJson() {
        PvcCardSlot slot = new PvcCardSlot(true, 5.5, 20.25, 9.0, "#112233");

        JSONObject json = slot.toJson();
        PvcCardSlot restored = PvcCardSlot.fromJson(json);

        assertTrue(restored.isVisible());
        assertEquals(5.5, restored.getLeftMm());
        assertEquals(20.25, restored.getTopMm());
        assertEquals(9.0, restored.getFontSizePt());
        assertEquals("#112233", restored.getFontColor());
    }

    @Test
    void barcodeSlotCarriesWidthHeightAndType() {
        PvcCardSlot slot = PvcCardSlot.barcodeSlot(true, 5, 48, 40, 10, "code128");

        JSONObject json = slot.toJson();
        PvcCardSlot restored = PvcCardSlot.fromJson(json);

        assertEquals(40.0, restored.getWidthMm());
        assertEquals(10.0, restored.getHeightMm());
        assertEquals("code128", restored.getType());
    }

    @Test
    void fromJsonFillsMissingFieldsWithDefaults() {
        PvcCardSlot restored = PvcCardSlot.fromJson(new JSONObject());

        assertFalse(restored.isVisible());
        assertEquals(0.0, restored.getLeftMm());
        assertEquals(8.0, restored.getFontSizePt());
        assertEquals("#000000", restored.getFontColor());
        assertEquals("code128", restored.getType());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=com.divudi.core.data.PvcCardSlotTest test`
Expected: FAIL (compilation error — `PvcCardSlot` does not exist yet)

- [ ] **Step 3: Write minimal implementation**

```java
package com.divudi.core.data;

import org.json.JSONObject;

import java.io.Serializable;

public class PvcCardSlot implements Serializable {

    private boolean visible;
    private double leftMm;
    private double topMm;
    private double fontSizePt = 8.0;
    private String fontColor = "#000000";
    private double widthMm;
    private double heightMm;
    private String type = "code128";

    public PvcCardSlot() {
    }

    public PvcCardSlot(boolean visible, double leftMm, double topMm, double fontSizePt, String fontColor) {
        this.visible = visible;
        this.leftMm = leftMm;
        this.topMm = topMm;
        this.fontSizePt = fontSizePt;
        this.fontColor = fontColor;
    }

    public static PvcCardSlot barcodeSlot(boolean visible, double leftMm, double topMm, double widthMm, double heightMm, String type) {
        PvcCardSlot slot = new PvcCardSlot();
        slot.visible = visible;
        slot.leftMm = leftMm;
        slot.topMm = topMm;
        slot.widthMm = widthMm;
        slot.heightMm = heightMm;
        slot.type = type;
        return slot;
    }

    public static PvcCardSlot fromJson(JSONObject json) {
        PvcCardSlot slot = new PvcCardSlot();
        slot.visible = json.optBoolean("visible", false);
        slot.leftMm = json.optDouble("leftMm", 0.0);
        slot.topMm = json.optDouble("topMm", 0.0);
        slot.fontSizePt = json.optDouble("fontSizePt", 8.0);
        slot.fontColor = json.optString("fontColor", "#000000");
        slot.widthMm = json.optDouble("widthMm", 0.0);
        slot.heightMm = json.optDouble("heightMm", 0.0);
        slot.type = json.optString("type", "code128");
        return slot;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("visible", visible);
        json.put("leftMm", leftMm);
        json.put("topMm", topMm);
        json.put("fontSizePt", fontSizePt);
        json.put("fontColor", fontColor);
        json.put("widthMm", widthMm);
        json.put("heightMm", heightMm);
        json.put("type", type);
        return json;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public double getLeftMm() {
        return leftMm;
    }

    public void setLeftMm(double leftMm) {
        this.leftMm = leftMm;
    }

    public double getTopMm() {
        return topMm;
    }

    public void setTopMm(double topMm) {
        this.topMm = topMm;
    }

    public double getFontSizePt() {
        return fontSizePt;
    }

    public void setFontSizePt(double fontSizePt) {
        this.fontSizePt = fontSizePt;
    }

    public String getFontColor() {
        return fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }

    public double getWidthMm() {
        return widthMm;
    }

    public void setWidthMm(double widthMm) {
        this.widthMm = widthMm;
    }

    public double getHeightMm() {
        return heightMm;
    }

    public void setHeightMm(double heightMm) {
        this.heightMm = heightMm;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=com.divudi.core.data.PvcCardSlotTest test`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/core/data/PvcCardSlot.java src/test/java/com/divudi/core/data/PvcCardSlotTest.java
git commit -m "feat(pvc-card): add PvcCardSlot layout value object

Closes #24297"
```

---

## Task 2: `PvcCardLayout` data class with defaults and JSON fallback

**Files:**
- Create: `src/main/java/com/divudi/core/data/PvcCardLayout.java`
- Test: `src/test/java/com/divudi/core/data/PvcCardLayoutTest.java`

**Interfaces:**
- Consumes: `PvcCardSlot` (Task 1) — `new PvcCardSlot(boolean, double, double, double, String)`, `PvcCardSlot.barcodeSlot(...)`, `PvcCardSlot.fromJson(JSONObject)`, `slot.toJson()`.
- Produces: `PvcCardLayout` — mutable POJO with `widthMm`/`heightMm`/`marginMm` (double, getters/setters) and `slots` (`Map<String, PvcCardSlot>`, getter/setter), slot-name constants `SLOT_NAME="name"`, `SLOT_DOB="dob"`, `SLOT_PHONE="phone"`, `SLOT_GENDER="gender"`, `SLOT_ADDRESS="address"`, `SLOT_INSTITUTION_NAME="institutionName"`, `SLOT_DEPARTMENT_NAME="departmentName"`, `SLOT_BARCODE="barcode"`; static `PvcCardLayout.defaultLayout()`; static `PvcCardLayout.fromJson(String json)` (never throws — falls back to `defaultLayout()` on null/blank/malformed input, and fills any missing slot with its default); instance `toJson()` returning a JSON `String`. Used by Task 3 (`PvcCardLayoutController`) and Task 4 (print-panel composite).

> **Post-implementation addition (2026-10-05):** a ninth slot, `SLOT_PHN="phn"`, was added after this plan was originally written, following this exact same mechanism, to show the readable PHN (plain text) that the pre-existing inline card had and the barcode-only card had dropped. Not reflected in the task write-up below.

- [ ] **Step 1: Write the failing test**

```java
package com.divudi.core.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvcCardLayoutTest {

    @Test
    void defaultLayoutHasSensibleCardSizeAndAllSlots() {
        PvcCardLayout layout = PvcCardLayout.defaultLayout();

        assertEquals(85.6, layout.getWidthMm());
        assertEquals(54.0, layout.getHeightMm());
        assertTrue(layout.getSlots().containsKey(PvcCardLayout.SLOT_NAME));
        assertTrue(layout.getSlots().containsKey(PvcCardLayout.SLOT_BARCODE));
        assertTrue(layout.getSlots().get(PvcCardLayout.SLOT_BARCODE).isVisible());
        assertFalse(layout.getSlots().get(PvcCardLayout.SLOT_ADDRESS).isVisible());
    }

    @Test
    void fromJsonNullOrBlankFallsBackToDefault() {
        PvcCardLayout fromNull = PvcCardLayout.fromJson(null);
        PvcCardLayout fromBlank = PvcCardLayout.fromJson("   ");

        assertEquals(85.6, fromNull.getWidthMm());
        assertEquals(85.6, fromBlank.getWidthMm());
    }

    @Test
    void fromJsonMalformedFallsBackToDefaultInsteadOfThrowing() {
        PvcCardLayout layout = PvcCardLayout.fromJson("{not valid json");

        assertEquals(85.6, layout.getWidthMm());
        assertTrue(layout.getSlots().containsKey(PvcCardLayout.SLOT_NAME));
    }

    @Test
    void fromJsonRoundTripsCustomValues() {
        PvcCardLayout original = PvcCardLayout.defaultLayout();
        original.setWidthMm(90.0);
        original.getSlots().get(PvcCardLayout.SLOT_NAME).setLeftMm(12.5);
        original.getSlots().get(PvcCardLayout.SLOT_ADDRESS).setVisible(true);

        PvcCardLayout restored = PvcCardLayout.fromJson(original.toJson());

        assertEquals(90.0, restored.getWidthMm());
        assertEquals(12.5, restored.getSlots().get(PvcCardLayout.SLOT_NAME).getLeftMm());
        assertTrue(restored.getSlots().get(PvcCardLayout.SLOT_ADDRESS).isVisible());
    }

    @Test
    void fromJsonFillsAnyMissingSlotWithDefault() {
        PvcCardLayout layout = PvcCardLayout.fromJson("{\"widthMm\":85.6,\"heightMm\":54.0,\"marginMm\":2.0,\"slots\":{}}");

        assertTrue(layout.getSlots().containsKey(PvcCardLayout.SLOT_BARCODE));
        assertTrue(layout.getSlots().get(PvcCardLayout.SLOT_BARCODE).isVisible());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=com.divudi.core.data.PvcCardLayoutTest test`
Expected: FAIL (compilation error — `PvcCardLayout` does not exist yet)

- [ ] **Step 3: Write minimal implementation**

```java
package com.divudi.core.data;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class PvcCardLayout implements Serializable {

    public static final String SLOT_INSTITUTION_NAME = "institutionName";
    public static final String SLOT_DEPARTMENT_NAME = "departmentName";
    public static final String SLOT_NAME = "name";
    public static final String SLOT_DOB = "dob";
    public static final String SLOT_PHONE = "phone";
    public static final String SLOT_GENDER = "gender";
    public static final String SLOT_ADDRESS = "address";
    public static final String SLOT_BARCODE = "barcode";

    private double widthMm;
    private double heightMm;
    private double marginMm;
    private Map<String, PvcCardSlot> slots = new LinkedHashMap<>();

    public static PvcCardLayout defaultLayout() {
        PvcCardLayout layout = new PvcCardLayout();
        layout.widthMm = 85.6;
        layout.heightMm = 54.0;
        layout.marginMm = 2.0;
        layout.slots.put(SLOT_INSTITUTION_NAME, new PvcCardSlot(true, 5, 4, 11, "#000000"));
        layout.slots.put(SLOT_DEPARTMENT_NAME, new PvcCardSlot(false, 5, 10, 9, "#000000"));
        layout.slots.put(SLOT_NAME, new PvcCardSlot(true, 5, 20, 9, "#000000"));
        layout.slots.put(SLOT_DOB, new PvcCardSlot(true, 5, 26, 8, "#000000"));
        layout.slots.put(SLOT_PHONE, new PvcCardSlot(true, 5, 32, 8, "#000000"));
        layout.slots.put(SLOT_GENDER, new PvcCardSlot(true, 5, 38, 8, "#000000"));
        layout.slots.put(SLOT_ADDRESS, new PvcCardSlot(false, 5, 44, 7, "#000000"));
        layout.slots.put(SLOT_BARCODE, PvcCardSlot.barcodeSlot(true, 5, 48, 40, 10, "code128"));
        return layout;
    }

    public static PvcCardLayout fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return defaultLayout();
        }
        try {
            JSONObject root = new JSONObject(json);
            PvcCardLayout defaults = defaultLayout();
            PvcCardLayout layout = new PvcCardLayout();
            layout.widthMm = root.optDouble("widthMm", defaults.widthMm);
            layout.heightMm = root.optDouble("heightMm", defaults.heightMm);
            layout.marginMm = root.optDouble("marginMm", defaults.marginMm);
            JSONObject slotsJson = root.optJSONObject("slots");
            for (Map.Entry<String, PvcCardSlot> entry : defaults.slots.entrySet()) {
                JSONObject slotJson = slotsJson == null ? null : slotsJson.optJSONObject(entry.getKey());
                layout.slots.put(entry.getKey(), slotJson == null ? entry.getValue() : PvcCardSlot.fromJson(slotJson));
            }
            return layout;
        } catch (Exception e) {
            return defaultLayout();
        }
    }

    public String toJson() {
        JSONObject root = new JSONObject();
        root.put("widthMm", widthMm);
        root.put("heightMm", heightMm);
        root.put("marginMm", marginMm);
        JSONObject slotsJson = new JSONObject();
        for (Map.Entry<String, PvcCardSlot> entry : slots.entrySet()) {
            slotsJson.put(entry.getKey(), entry.getValue().toJson());
        }
        root.put("slots", slotsJson);
        return root.toString();
    }

    public double getWidthMm() {
        return widthMm;
    }

    public void setWidthMm(double widthMm) {
        this.widthMm = widthMm;
    }

    public double getHeightMm() {
        return heightMm;
    }

    public void setHeightMm(double heightMm) {
        this.heightMm = heightMm;
    }

    public double getMarginMm() {
        return marginMm;
    }

    public void setMarginMm(double marginMm) {
        this.marginMm = marginMm;
    }

    public Map<String, PvcCardSlot> getSlots() {
        return slots;
    }

    public void setSlots(Map<String, PvcCardSlot> slots) {
        this.slots = slots;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=com.divudi.core.data.PvcCardLayoutTest test`
Expected: PASS (5 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/core/data/PvcCardLayout.java src/test/java/com/divudi/core/data/PvcCardLayoutTest.java
git commit -m "feat(pvc-card): add PvcCardLayout with defaults and JSON fallback

Closes #24297"
```

---

## Task 3: `UploadType` additions + `PvcCardLayoutController` bean

**Files:**
- Modify: `src/main/java/com/divudi/core/data/UploadType.java`
- Create: `src/main/java/com/divudi/bean/common/PvcCardLayoutController.java`

**Interfaces:**
- Consumes: `PvcCardLayout.fromJson(String)` / `layout.toJson()` (Task 2); `ConfigOptionApplicationController.getLongTextValueByKeyReadOnly(String key, String defaultValue)` and `.setLongTextValueByKey(String key, String value)` (existing, `src/main/java/com/divudi/bean/common/ConfigOptionApplicationController.java:1443,1539`); `UploadFacade.create(Upload)`, `.edit(Upload)`, `.findFirstByJpql(String, Map)` (existing, `AbstractFacade`); `SessionController.getLoggedUser()` (existing, `SessionController.java:2434`).
- Produces: `PvcCardLayoutController` (`@Named @ViewScoped`) with `getFront()`/`getBack()` returning the mutable `PvcCardLayout` (two-way bindable from JSF for both rendering and admin editing), `saveFrontLayout()`/`saveBackLayout()`, `getFrontBackgroundExternalUrl()`/`getBackBackgroundExternalUrl()` (String, empty if none), `getFrontBackgroundStream()`/`getBackBackgroundStream()` (`org.primefaces.model.StreamedContent`), `getFrontUrlInput()`/`setFrontUrlInput(String)` and the `back` equivalents (bound to an admin text field), `saveFrontBackgroundUrl()`/`saveBackBackgroundUrl()`, `uploadFrontBackground(FileUploadEvent)`/`uploadBackBackground(FileUploadEvent)`. Used by Task 4 (composite component) and Task 6 (admin page).

This task has no JUnit test: its logic is thin CDI/EJB/JPA glue around already-tested `PvcCardLayout`/`PvcCardSlot` (Task 1/2) and the existing `ConfigOptionApplicationController`/`UploadFacade`, none of which this codebase's test suite mocks (no existing test instantiates a bean with `@EJB`/`@Inject` fields directly — see e.g. `BhtSummeryControllerDoctorChargeSummaryTest`, which only tests a controller's pure-logic methods). Correctness of this task is verified by a successful Maven compile (Step 2) and by Task 7's Playwright run, which exercises every method on this bean through the real running application.

- [ ] **Step 1: Add the two new `UploadType` constants**

Edit `src/main/java/com/divudi/core/data/UploadType.java`, adding two constants to the existing enum (full current contents for context — only the two new lines are additions):

```java
public enum UploadType {

    User_Signature("User Signature"),
    Lab_Report("Out Source Report"),
    Web_Image("Web Image"),
    Report_background_image("Report Background Image"),
    Background_Image("Background Image"),
    Diagnosis_Card_Template("Diagnosis Card Template"),
    Inward_Consent_Form("Consent Form"),
    Inward_Insurance_Document("Insurance Document"),
    Inward_Referral_Letter("Referral Letter"),
    Inward_GOP("GOP"),
    Inward_Other("Other Document"),
    PVC_Card_Front_Background("PVC Card Front Background"),
    PVC_Card_Back_Background("PVC Card Back Background");

    private final String label;

    UploadType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
```

- [ ] **Step 2: Write `PvcCardLayoutController`**

Create `src/main/java/com/divudi/bean/common/PvcCardLayoutController.java`:

```java
package com.divudi.bean.common;

import com.divudi.core.data.PvcCardLayout;
import com.divudi.core.data.PvcCardSlot;
import com.divudi.core.data.UploadType;
import com.divudi.core.entity.Upload;
import com.divudi.core.facade.UploadFacade;
import com.divudi.core.util.JsfUtil;
import org.apache.commons.io.IOUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Named
@ViewScoped
public class PvcCardLayoutController implements Serializable {

    public static final String KEY_FRONT_LAYOUT = "PVC Card Front Layout";
    public static final String KEY_BACK_LAYOUT = "PVC Card Back Layout";

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private SessionController sessionController;
    @EJB
    private UploadFacade uploadFacade;

    private PvcCardLayout front;
    private PvcCardLayout back;
    private String frontUrlInput;
    private String backUrlInput;

    @PostConstruct
    public void init() {
        front = PvcCardLayout.fromJson(configOptionApplicationController.getLongTextValueByKeyReadOnly(KEY_FRONT_LAYOUT, ""));
        back = PvcCardLayout.fromJson(configOptionApplicationController.getLongTextValueByKeyReadOnly(KEY_BACK_LAYOUT, ""));
        frontUrlInput = backgroundUrl(UploadType.PVC_Card_Front_Background);
        backUrlInput = backgroundUrl(UploadType.PVC_Card_Back_Background);
    }

    public PvcCardLayout getFront() {
        return front;
    }

    public PvcCardLayout getBack() {
        return back;
    }

    public void saveFrontLayout() {
        if (!isValid(front)) {
            return;
        }
        configOptionApplicationController.setLongTextValueByKey(KEY_FRONT_LAYOUT, front.toJson());
        JsfUtil.addSuccessMessage("Front layout saved");
    }

    public void saveBackLayout() {
        if (!isValid(back)) {
            return;
        }
        configOptionApplicationController.setLongTextValueByKey(KEY_BACK_LAYOUT, back.toJson());
        JsfUtil.addSuccessMessage("Back layout saved");
    }

    private boolean isValid(PvcCardLayout layout) {
        if (layout.getWidthMm() <= 0 || layout.getHeightMm() <= 0) {
            JsfUtil.addErrorMessage("Card width and height must be greater than zero");
            return false;
        }
        if (layout.getMarginMm() < 0) {
            JsfUtil.addErrorMessage("Margin cannot be negative");
            return false;
        }
        for (Map.Entry<String, PvcCardSlot> entry : layout.getSlots().entrySet()) {
            PvcCardSlot slot = entry.getValue();
            if (!slot.isVisible()) {
                continue;
            }
            if (slot.getFontSizePt() <= 0) {
                JsfUtil.addErrorMessage("Font size for '" + entry.getKey() + "' must be greater than zero");
                return false;
            }
            if (slot.getLeftMm() < 0 || slot.getLeftMm() > layout.getWidthMm()
                    || slot.getTopMm() < 0 || slot.getTopMm() > layout.getHeightMm()) {
                JsfUtil.addErrorMessage("Position for '" + entry.getKey() + "' is outside the card bounds");
                return false;
            }
        }
        return true;
    }

    public String getFrontBackgroundExternalUrl() {
        return backgroundUrl(UploadType.PVC_Card_Front_Background);
    }

    public String getBackBackgroundExternalUrl() {
        return backgroundUrl(UploadType.PVC_Card_Back_Background);
    }

    public StreamedContent getFrontBackgroundStream() {
        return backgroundStream(UploadType.PVC_Card_Front_Background);
    }

    public StreamedContent getBackBackgroundStream() {
        return backgroundStream(UploadType.PVC_Card_Back_Background);
    }

    public String getFrontUrlInput() {
        return frontUrlInput;
    }

    public void setFrontUrlInput(String frontUrlInput) {
        this.frontUrlInput = frontUrlInput;
    }

    public String getBackUrlInput() {
        return backUrlInput;
    }

    public void setBackUrlInput(String backUrlInput) {
        this.backUrlInput = backUrlInput;
    }

    public void saveFrontBackgroundUrl() {
        saveBackgroundUrl(UploadType.PVC_Card_Front_Background, frontUrlInput);
    }

    public void saveBackBackgroundUrl() {
        saveBackgroundUrl(UploadType.PVC_Card_Back_Background, backUrlInput);
    }

    public void uploadFrontBackground(FileUploadEvent event) {
        saveBackgroundUpload(UploadType.PVC_Card_Front_Background, event);
    }

    public void uploadBackBackground(FileUploadEvent event) {
        saveBackgroundUpload(UploadType.PVC_Card_Back_Background, event);
    }

    private String backgroundUrl(UploadType type) {
        Upload upload = findUploadByType(type);
        if (upload == null || upload.getFileUrl() == null) {
            return "";
        }
        return upload.getFileUrl();
    }

    private StreamedContent backgroundStream(UploadType type) {
        Upload upload = findUploadByType(type);
        if (upload == null || upload.getBaImage() == null) {
            return new DefaultStreamedContent();
        }
        byte[] bytes = upload.getBaImage();
        String contentType = upload.getFileType() != null ? upload.getFileType() : "image/png";
        return DefaultStreamedContent.builder()
                .contentType(contentType)
                .stream(() -> new ByteArrayInputStream(bytes))
                .build();
    }

    private void saveBackgroundUpload(UploadType type, FileUploadEvent event) {
        try {
            Upload upload = existingOrNewUpload(type);
            byte[] bytes = IOUtils.toByteArray(event.getFile().getInputStream());
            upload.setBaImage(bytes);
            upload.setFileName(event.getFile().getFileName());
            upload.setFileType(event.getFile().getContentType());
            upload.setFileUrl(null);
            persist(upload);
            JsfUtil.addSuccessMessage("Background image uploaded");
        } catch (IOException ex) {
            JsfUtil.addErrorMessage("Upload failed");
        }
    }

    private void saveBackgroundUrl(UploadType type, String url) {
        Upload upload = existingOrNewUpload(type);
        upload.setFileUrl(url);
        upload.setBaImage(null);
        persist(upload);
        JsfUtil.addSuccessMessage("Background URL saved");
    }

    private Upload existingOrNewUpload(UploadType type) {
        Upload upload = findUploadByType(type);
        if (upload == null) {
            upload = new Upload();
            upload.setUploadType(type);
            upload.setCreater(sessionController.getLoggedUser());
            upload.setCreatedAt(new Date());
        }
        return upload;
    }

    private void persist(Upload upload) {
        if (upload.getId() == null) {
            uploadFacade.create(upload);
        } else {
            uploadFacade.edit(upload);
        }
    }

    private Upload findUploadByType(UploadType type) {
        String jpql = "select u from Upload u where u.retired=:ret and u.uploadType=:ut order by u.id desc";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("ut", type);
        return uploadFacade.findFirstByJpql(jpql, m);
    }
}
```

- [ ] **Step 3: Compile**

Run: `mvn -q compile`
Expected: BUILD SUCCESS, no compilation errors

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/divudi/core/data/UploadType.java src/main/java/com/divudi/bean/common/PvcCardLayoutController.java
git commit -m "feat(pvc-card): add PvcCardLayoutController for config-driven card layout

Closes #24297"
```

---

## Task 4: Shared print-panel composite component

**Files:**
- Create: `src/main/webapp/resources/ezcomp/pvc_card_panel.xhtml`

**Interfaces:**
- Consumes: `PvcCardLayout` (Task 2) via `cc.attrs.layout` (its `widthMm`/`heightMm` and `slots` map, keyed by `PvcCardLayout.SLOT_*` string constants: `"institutionName"`, `"departmentName"`, `"name"`, `"dob"`, `"phone"`, `"gender"`, `"address"`, `"barcode"`); `cc.attrs.backgroundExternalUrl` / `cc.attrs.backgroundStream` (Strings/StreamedContent from `PvcCardLayoutController`, Task 3); existing `patientController.current.*` and `sessionController.institution.name` / `sessionController.department.name` EL paths (same as today's inline card at `opd/patient.xhtml:331-353`).
- Produces: composite tag `<pvc:pvc_card_panel layout="#{...}" backgroundExternalUrl="#{...}" backgroundStream="#{...}"/>`, namespace `xmlns:pvc="http://xmlns.jcp.org/jsf/composite/ezcomp"` (mirrors the existing `resources/ezcomp/common_report.xhtml` convention). The composite does **not** declare its own id for the printable root — composite components are JSF `NamingContainer`s, so an id set inside one does not resolve the way `<p:printer target="...">`'s simple relative id search does today against the existing plain `h:panelGroup id="groupPatientCard"` (`opd/patient.xhtml:328`). Callers (Task 5, Task 6) wrap each usage in their own plain `h:panelGroup` with the real target/update id, exactly mirroring the current working pattern.

No automated test for this task — it is pure XHTML composition with no Java logic, consistent with the project's "XHTML-only changes need no compile or test" convention. It is exercised end-to-end by Task 7's Playwright run. Visual/positional correctness against a physical printout is confirmed manually by the user post-deployment (per spec's Testing section), not automatable here.

- [ ] **Step 1: Create the composite component**

```xml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:cc="http://xmlns.jcp.org/jsf/composite"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:p="http://primefaces.org/ui">

    <cc:interface>
        <cc:attribute name="layout" type="com.divudi.core.data.PvcCardLayout" required="true"/>
        <cc:attribute name="backgroundExternalUrl" type="java.lang.String"/>
        <cc:attribute name="backgroundStream" type="org.primefaces.model.StreamedContent"/>
    </cc:interface>

    <cc:implementation>
        <h:panelGroup layout="block"
                      style="position:relative; overflow:hidden; width:#{cc.attrs.layout.widthMm}mm; height:#{cc.attrs.layout.heightMm}mm;">

            <h:panelGroup layout="block" rendered="#{not empty cc.attrs.backgroundExternalUrl}"
                          style="position:absolute; top:0; left:0; width:100%; height:100%;">
                <p:graphicImage value="#{cc.attrs.backgroundExternalUrl}" cache="false"
                                 style="width:100%; height:100%; object-fit:cover;"/>
            </h:panelGroup>
            <h:panelGroup layout="block" rendered="#{empty cc.attrs.backgroundExternalUrl}"
                          style="position:absolute; top:0; left:0; width:100%; height:100%;">
                <p:graphicImage value="#{cc.attrs.backgroundStream}" cache="false"
                                 style="width:100%; height:100%; object-fit:cover;"/>
            </h:panelGroup>

            <h:panelGroup layout="block" rendered="#{cc.attrs.layout.slots['institutionName'].visible}"
                          style="position:absolute; left:#{cc.attrs.layout.slots['institutionName'].leftMm}mm; top:#{cc.attrs.layout.slots['institutionName'].topMm}mm; font-size:#{cc.attrs.layout.slots['institutionName'].fontSizePt}pt; color:#{cc.attrs.layout.slots['institutionName'].fontColor};">
                <h:outputText value="#{sessionController.institution.name}"/>
            </h:panelGroup>

            <h:panelGroup layout="block" rendered="#{cc.attrs.layout.slots['departmentName'].visible}"
                          style="position:absolute; left:#{cc.attrs.layout.slots['departmentName'].leftMm}mm; top:#{cc.attrs.layout.slots['departmentName'].topMm}mm; font-size:#{cc.attrs.layout.slots['departmentName'].fontSizePt}pt; color:#{cc.attrs.layout.slots['departmentName'].fontColor};">
                <h:outputText value="#{sessionController.department.name}"/>
            </h:panelGroup>

            <h:panelGroup layout="block" rendered="#{cc.attrs.layout.slots['name'].visible}"
                          style="position:absolute; left:#{cc.attrs.layout.slots['name'].leftMm}mm; top:#{cc.attrs.layout.slots['name'].topMm}mm; font-size:#{cc.attrs.layout.slots['name'].fontSizePt}pt; color:#{cc.attrs.layout.slots['name'].fontColor};">
                <h:outputText value="#{patientController.current.person.nameWithTitle}"/>
            </h:panelGroup>

            <h:panelGroup layout="block" rendered="#{cc.attrs.layout.slots['dob'].visible}"
                          style="position:absolute; left:#{cc.attrs.layout.slots['dob'].leftMm}mm; top:#{cc.attrs.layout.slots['dob'].topMm}mm; font-size:#{cc.attrs.layout.slots['dob'].fontSizePt}pt; color:#{cc.attrs.layout.slots['dob'].fontColor};">
                <h:outputText value="#{patientController.current.person.dob}">
                    <f:convertDateTime pattern="dd/MM/yyyy"/>
                </h:outputText>
            </h:panelGroup>

            <h:panelGroup layout="block" rendered="#{cc.attrs.layout.slots['phone'].visible}"
                          style="position:absolute; left:#{cc.attrs.layout.slots['phone'].leftMm}mm; top:#{cc.attrs.layout.slots['phone'].topMm}mm; font-size:#{cc.attrs.layout.slots['phone'].fontSizePt}pt; color:#{cc.attrs.layout.slots['phone'].fontColor};">
                <h:outputText value="#{patientController.current.person.phone}"/>
            </h:panelGroup>

            <h:panelGroup layout="block" rendered="#{cc.attrs.layout.slots['gender'].visible}"
                          style="position:absolute; left:#{cc.attrs.layout.slots['gender'].leftMm}mm; top:#{cc.attrs.layout.slots['gender'].topMm}mm; font-size:#{cc.attrs.layout.slots['gender'].fontSizePt}pt; color:#{cc.attrs.layout.slots['gender'].fontColor};">
                <h:outputText value="#{patientController.current.person.sex}"/>
            </h:panelGroup>

            <h:panelGroup layout="block" rendered="#{cc.attrs.layout.slots['address'].visible}"
                          style="position:absolute; left:#{cc.attrs.layout.slots['address'].leftMm}mm; top:#{cc.attrs.layout.slots['address'].topMm}mm; font-size:#{cc.attrs.layout.slots['address'].fontSizePt}pt; color:#{cc.attrs.layout.slots['address'].fontColor};">
                <h:outputText value="#{patientController.current.person.address}"/>
            </h:panelGroup>

            <h:panelGroup layout="block" rendered="#{cc.attrs.layout.slots['barcode'].visible}"
                          style="position:absolute; left:#{cc.attrs.layout.slots['barcode'].leftMm}mm; top:#{cc.attrs.layout.slots['barcode'].topMm}mm;">
                <p:barcode value="#{patientController.current.phn}"
                           type="#{cc.attrs.layout.slots['barcode'].type}"
                           format="svg" hrp="none" cache="false"
                           width="#{cc.attrs.layout.slots['barcode'].widthMm * 3.78}"
                           height="#{cc.attrs.layout.slots['barcode'].heightMm * 3.78}"/>
            </h:panelGroup>

        </h:panelGroup>
    </cc:implementation>
</html>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/webapp/resources/ezcomp/pvc_card_panel.xhtml
git commit -m "feat(pvc-card): add shared front/back print-panel composite component

Closes #24297"
```

---

## Task 5: Wire Print Front / Print Back into `opd/patient.xhtml`

**Files:**
- Modify: `src/main/webapp/opd/patient.xhtml:13` (remove the Google Font stylesheet include, no longer needed), `:29-35` (replace the single Print Card button), `:327-356` (replace the inline card markup)

**Interfaces:**
- Consumes: `<pvc:pvc_card_panel>` (Task 4), `pvcCardLayoutController.front` / `.back` / `.frontBackgroundExternalUrl` / `.backBackgroundExternalUrl` / `.frontBackgroundStream` / `.backBackgroundStream` (Task 3). Each composite usage is wrapped in its own plain `h:panelGroup` carrying the real `id` that `<p:printer target="...">` targets — same structure as the `id="groupPatientCard"` panelGroup being replaced, so the existing simple relative-id resolution keeps working.

No automated test for this task (XHTML-only change) — verified by Task 7's Playwright run.

- [ ] **Step 1: Add the composite namespace and remove the now-unused font stylesheet**

In `src/main/webapp/opd/patient.xhtml`, change the `<html>` tag (currently lines 3-8) to add the `pvc` namespace, and remove line 13's `<h:outputStylesheet>` (the `Libre Barcode 128 Text` font was only used by the old inline card's hardcoded font-family):

```xml
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:p="http://primefaces.org/ui"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
      xmlns:na="http://xmlns.jcp.org/jsf/composite/template"
      xmlns:pvc="http://xmlns.jcp.org/jsf/composite/ezcomp">

    <h:head>
        <title>Patient Profile</title>
    </h:head>
```

- [ ] **Step 2: Replace the "Print Card" button with "Print Front" / "Print Back"**

Replace lines 29-35 (the single `p:commandButton` with `<p:printer target="groupPatientCard"/>`) with:

```xml
<p:commandButton
    icon="pi pi-print"
    value="Print Front"
    styleClass="ui-button-info"
    title="Print front of patient card" >
    <p:printer target="pvcCardFront"/>
</p:commandButton>
<p:commandButton
    icon="pi pi-print"
    value="Print Back"
    styleClass="ui-button-info"
    title="Print back of patient card" >
    <p:printer target="pvcCardBack"/>
</p:commandButton>
```

- [ ] **Step 3: Replace the inline card markup with the two composite panels**

Replace lines 327-356 (the `<div style="transform: scale(2.0)...">...</div>` block containing the hardcoded card) with:

```xml
<div style="display:none;">
    <h:panelGroup id="pvcCardFront" layout="block">
        <pvc:pvc_card_panel layout="#{pvcCardLayoutController.front}"
                             backgroundExternalUrl="#{pvcCardLayoutController.frontBackgroundExternalUrl}"
                             backgroundStream="#{pvcCardLayoutController.frontBackgroundStream}"/>
    </h:panelGroup>
    <h:panelGroup id="pvcCardBack" layout="block">
        <pvc:pvc_card_panel layout="#{pvcCardLayoutController.back}"
                             backgroundExternalUrl="#{pvcCardLayoutController.backBackgroundExternalUrl}"
                             backgroundStream="#{pvcCardLayoutController.backBackgroundStream}"/>
    </h:panelGroup>
</div>
```

- [ ] **Step 4: Commit**

```bash
git add src/main/webapp/opd/patient.xhtml
git commit -m "feat(pvc-card): split Print Card into Print Front/Back using configurable layout

Closes #24297"
```

---

## Task 6: Admin "Card Layout" page with live preview

**Files:**
- Create: `src/main/webapp/admin/institutions/pvc_card_layout.xhtml`
- Modify: `src/main/webapp/admin/institutions/admin_institutions_index.xhtml` (add a navigation button, mirroring the existing "Application Options" button at lines 33-35)

**Interfaces:**
- Consumes: `PvcCardLayoutController` (Task 3) — `getFront()`/`getBack()` (two-way bound to form inputs), `saveFrontLayout()`/`saveBackLayout()`, `getFrontUrlInput()`/`setFrontUrlInput`/`getBackUrlInput()`/`setBackUrlInput`, `saveFrontBackgroundUrl()`/`saveBackBackgroundUrl()`, `uploadFrontBackground(FileUploadEvent)`/`uploadBackBackground(FileUploadEvent)`, `getFrontBackgroundExternalUrl()`/`getBackBackgroundExternalUrl()`/`getFrontBackgroundStream()`/`getBackBackgroundStream()`; `<pvc:pvc_card_panel>` (Task 4) for the live preview, bound directly to `pvcCardLayoutController.front`/`.back` so edits are visible before saving.

No automated test for this task (XHTML-only, admin UI) — verified by Task 7's Playwright run, including the save-and-reload persistence check from the spec's Testing section.

- [ ] **Step 1: Create the admin page**

```xml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:p="http://primefaces.org/ui"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
      xmlns:pvc="http://xmlns.jcp.org/jsf/composite/ezcomp">

    <h:head>
        <title>PVC Card Layout</title>
    </h:head>

    <h:body>
        <ui:composition template="/resources/template/template.xhtml">
            <ui:define name="content">
                <h:form id="pvcCardLayoutForm">
                    <p:growl id="messages" showDetail="true"/>
                    <p:panel header="PVC Card Layout">
                        <p:tabView>

                            <p:tab title="Front">
                                <p:panelGrid columns="2" styleClass="w-100">
                                    <h:panelGroup layout="block">
                                        <h:outputText value="Card width (mm)"/>
                                        <p:inputNumber value="#{pvcCardLayoutController.front.widthMm}" minValue="1"/>
                                        <h:outputText value="Card height (mm)"/>
                                        <p:inputNumber value="#{pvcCardLayoutController.front.heightMm}" minValue="1"/>
                                        <h:outputText value="Margin (mm)"/>
                                        <p:inputNumber value="#{pvcCardLayoutController.front.marginMm}" minValue="0"/>

                                        <p:dataTable value="#{pvcCardLayoutController.front.slots.entrySet().toArray()}" var="entry">
                                            <p:column headerText="Field">
                                                <h:outputText value="#{entry.key}"/>
                                            </p:column>
                                            <p:column headerText="Visible">
                                                <p:selectBooleanCheckbox value="#{entry.value.visible}">
                                                    <p:ajax update="frontPreview"/>
                                                </p:selectBooleanCheckbox>
                                            </p:column>
                                            <p:column headerText="X (mm)">
                                                <p:inputNumber value="#{entry.value.leftMm}">
                                                    <p:ajax update="frontPreview"/>
                                                </p:inputNumber>
                                            </p:column>
                                            <p:column headerText="Y (mm)">
                                                <p:inputNumber value="#{entry.value.topMm}">
                                                    <p:ajax update="frontPreview"/>
                                                </p:inputNumber>
                                            </p:column>
                                            <p:column headerText="Font size (pt)">
                                                <p:inputNumber value="#{entry.value.fontSizePt}">
                                                    <p:ajax update="frontPreview"/>
                                                </p:inputNumber>
                                            </p:column>
                                            <p:column headerText="Font color">
                                                <p:colorPicker value="#{entry.value.fontColor}">
                                                    <p:ajax update="frontPreview"/>
                                                </p:colorPicker>
                                            </p:column>
                                            <p:column headerText="Barcode type/size">
                                                <p:inputText value="#{entry.value.type}" style="width:6rem;"
                                                             rendered="#{entry.key eq 'barcode'}">
                                                    <p:ajax update="frontPreview"/>
                                                </p:inputText>
                                                <p:inputNumber value="#{entry.value.widthMm}" style="width:5rem;"
                                                               rendered="#{entry.key eq 'barcode'}">
                                                    <p:ajax update="frontPreview"/>
                                                </p:inputNumber>
                                                <p:inputNumber value="#{entry.value.heightMm}" style="width:5rem;"
                                                               rendered="#{entry.key eq 'barcode'}">
                                                    <p:ajax update="frontPreview"/>
                                                </p:inputNumber>
                                            </p:column>
                                        </p:dataTable>

                                        <p:fileUpload listener="#{pvcCardLayoutController.uploadFrontBackground}"
                                                      mode="advanced" auto="true" update="frontPreview messages"
                                                      label="Upload front background" allowTypes="/(\.|\/)(gif|jpe?g|png)$/"/>
                                        <h:outputText value="or external URL:"/>
                                        <p:inputText value="#{pvcCardLayoutController.frontUrlInput}" style="width:20rem;"/>
                                        <p:commandButton value="Save Background URL" action="#{pvcCardLayoutController.saveFrontBackgroundUrl}"
                                                         update="frontPreview messages"/>

                                        <p:commandButton value="Save Front Layout" action="#{pvcCardLayoutController.saveFrontLayout}"
                                                          update="messages" styleClass="ui-button-success"/>
                                    </h:panelGroup>

                                    <h:panelGroup id="frontPreview" layout="block">
                                        <pvc:pvc_card_panel layout="#{pvcCardLayoutController.front}"
                                                             backgroundExternalUrl="#{pvcCardLayoutController.frontBackgroundExternalUrl}"
                                                             backgroundStream="#{pvcCardLayoutController.frontBackgroundStream}"/>
                                    </h:panelGroup>
                                </p:panelGrid>
                            </p:tab>

                            <p:tab title="Back">
                                <p:panelGrid columns="2" styleClass="w-100">
                                    <h:panelGroup layout="block">
                                        <h:outputText value="Card width (mm)"/>
                                        <p:inputNumber value="#{pvcCardLayoutController.back.widthMm}" minValue="1"/>
                                        <h:outputText value="Card height (mm)"/>
                                        <p:inputNumber value="#{pvcCardLayoutController.back.heightMm}" minValue="1"/>
                                        <h:outputText value="Margin (mm)"/>
                                        <p:inputNumber value="#{pvcCardLayoutController.back.marginMm}" minValue="0"/>

                                        <p:dataTable value="#{pvcCardLayoutController.back.slots.entrySet().toArray()}" var="entry">
                                            <p:column headerText="Field">
                                                <h:outputText value="#{entry.key}"/>
                                            </p:column>
                                            <p:column headerText="Visible">
                                                <p:selectBooleanCheckbox value="#{entry.value.visible}">
                                                    <p:ajax update="backPreview"/>
                                                </p:selectBooleanCheckbox>
                                            </p:column>
                                            <p:column headerText="X (mm)">
                                                <p:inputNumber value="#{entry.value.leftMm}">
                                                    <p:ajax update="backPreview"/>
                                                </p:inputNumber>
                                            </p:column>
                                            <p:column headerText="Y (mm)">
                                                <p:inputNumber value="#{entry.value.topMm}">
                                                    <p:ajax update="backPreview"/>
                                                </p:inputNumber>
                                            </p:column>
                                            <p:column headerText="Font size (pt)">
                                                <p:inputNumber value="#{entry.value.fontSizePt}">
                                                    <p:ajax update="backPreview"/>
                                                </p:inputNumber>
                                            </p:column>
                                            <p:column headerText="Font color">
                                                <p:colorPicker value="#{entry.value.fontColor}">
                                                    <p:ajax update="backPreview"/>
                                                </p:colorPicker>
                                            </p:column>
                                            <p:column headerText="Barcode type/size">
                                                <p:inputText value="#{entry.value.type}" style="width:6rem;"
                                                             rendered="#{entry.key eq 'barcode'}">
                                                    <p:ajax update="backPreview"/>
                                                </p:inputText>
                                                <p:inputNumber value="#{entry.value.widthMm}" style="width:5rem;"
                                                               rendered="#{entry.key eq 'barcode'}">
                                                    <p:ajax update="backPreview"/>
                                                </p:inputNumber>
                                                <p:inputNumber value="#{entry.value.heightMm}" style="width:5rem;"
                                                               rendered="#{entry.key eq 'barcode'}">
                                                    <p:ajax update="backPreview"/>
                                                </p:inputNumber>
                                            </p:column>
                                        </p:dataTable>

                                        <p:fileUpload listener="#{pvcCardLayoutController.uploadBackBackground}"
                                                      mode="advanced" auto="true" update="backPreview messages"
                                                      label="Upload back background" allowTypes="/(\.|\/)(gif|jpe?g|png)$/"/>
                                        <h:outputText value="or external URL:"/>
                                        <p:inputText value="#{pvcCardLayoutController.backUrlInput}" style="width:20rem;"/>
                                        <p:commandButton value="Save Background URL" action="#{pvcCardLayoutController.saveBackBackgroundUrl}"
                                                         update="backPreview messages"/>

                                        <p:commandButton value="Save Back Layout" action="#{pvcCardLayoutController.saveBackLayout}"
                                                          update="messages" styleClass="ui-button-success"/>
                                    </h:panelGroup>

                                    <h:panelGroup id="backPreview" layout="block">
                                        <pvc:pvc_card_panel layout="#{pvcCardLayoutController.back}"
                                                             backgroundExternalUrl="#{pvcCardLayoutController.backBackgroundExternalUrl}"
                                                             backgroundStream="#{pvcCardLayoutController.backBackgroundStream}"/>
                                    </h:panelGroup>
                                </p:panelGrid>
                            </p:tab>

                        </p:tabView>
                    </p:panel>
                </h:form>
            </ui:define>
        </ui:composition>
    </h:body>
</html>
```

- [ ] **Step 2: Add a menu entry on the institutions admin index**

In `src/main/webapp/admin/institutions/admin_institutions_index.xhtml`, add a new button next to the existing "Application Options" button (around line 35):

```xml
<!-- PVC Card Layout Button -->
<p:commandButton styleClass="linkButton ui-button-info" ajax="false" value="PVC Card Layout" icon="pi pi-id-card"
                 action="/admin/institutions/pvc_card_layout?faces-redirect=true"/>
```

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/admin/institutions/pvc_card_layout.xhtml src/main/webapp/admin/institutions/admin_institutions_index.xhtml
git commit -m "feat(pvc-card): add admin Card Layout page with live preview

Closes #24297"
```

---

## Task 7: Build, deploy, and Playwright-verify end to end

**Files:** none (verification only)

**Interfaces:** none — this task exercises Tasks 1-6 through the real running application.

- [ ] **Step 1: Run the full unit test suite**

Run: `mvn -q test`
Expected: BUILD SUCCESS, all tests pass including `PvcCardSlotTest` and `PvcCardLayoutTest`

- [ ] **Step 2: Build and locally redeploy**

Use the project's standard local build/redeploy flow (Maven package + Payara `asadmin` redeploy to `domain1`) per `developer_docs/testing/playwright-e2e-workflow.md` and the project's `playwright-e2e` skill. Confirm no deployment errors in `/home/buddhika/payara/glassfish/domains/domain1/logs/server.log`.

- [ ] **Step 3: Playwright — verify patient page print buttons (fresh deployment, no config yet)**

Using the Playwright MCP server: open the app root (never navigate by typing an inner URL), log in, select a department, navigate via menus to a patient's profile page. Verify:
- Two buttons are present: "Print Front" and "Print Back" (no single "Print Card" button remains).
- The hidden front/back panels render with the default layout (institution name, patient name, DOB, phone, gender visible; address not visible — per `PvcCardLayout.defaultLayout()`), with no background image (since no `Upload` row exists yet) and no console errors.
- This confirms the Review Focus items "fresh deployment, no ConfigOption rows yet" and "no Upload row yet for a side's background" render correctly rather than blank/broken.

- [ ] **Step 4: Playwright — verify a null optional field renders blank, not an error**

Find or use a test patient whose `address` is empty/null. Temporarily enable the `address` slot's visibility via the admin page (next step covers navigating there) and confirm the front panel still renders without a JS/console error, with the address text simply empty. Leave this test patient and any config changes in place afterward — do not clean up test data (per project convention), and list in your final report what was created/changed.

- [ ] **Step 5: Playwright — verify the admin Card Layout page and persistence**

Navigate via menus (Admin → Institutions → "PVC Card Layout" button added in Task 6) to the new admin page. Verify:
- Front/Back tabs show the current layout values.
- Changing a field's X/Y position or visibility checkbox updates the live preview panel via AJAX without a full page reload.
- Clicking "Save Front Layout" shows a success growl message.
- Reloading the patient page (via menu navigation) shows the patient card reflecting the saved change — confirming the `ConfigOptionApplicationController` round-trip persisted correctly.
- Uploading a background image (any small PNG/JPG) shows it applied in the preview; entering an external URL and saving switches the preview to that URL instead (confirming the blob-vs-URL mutual-exclusion behavior from `PvcCardLayoutController`).

- [ ] **Step 6: Report results**

Summarize in chat: which patient/department was used for testing, what config rows/Upload rows were created (list them, per the "leave test data in place" convention), and confirm all six Review Focus items were observed behaving correctly. Do not commit anything in this task (verification only).
