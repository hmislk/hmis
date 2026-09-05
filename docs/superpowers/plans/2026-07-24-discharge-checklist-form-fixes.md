# Discharge Checklist Dynamic-Form Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix two dynamic-form defects in the inward admission-forms feature (radio button column count hardcoded to 2; edit-from-list not defaulting to read-only view) so a 22-row Yes/No/N/A discharge checklist can later be created via the `/api/forms` REST API and render/behave correctly.

**Architecture:** Both fixes are localized to `InwardFormController.java` (a `@SessionScoped` JSF managed bean) and its single view, `admission_forms.xhtml`. No new entities, no new REST endpoints, no DB schema changes. Fix 1 adds one pure static helper method (unit-testable in isolation, no EJB dependencies) plus one instance method that reads live choice count; Fix 2 is a one-line change to an existing instance method.

**Tech Stack:** Java 11, JSF 2.x / PrimeFaces, JUnit 5, Maven.

## Global Constraints

- Base the branch on `origin/development`, never `master` (per project rules).
- JPQL only for any DB queries — not applicable here (no new queries added), but `getChoicesFor()` (already JPQL-based) must not be changed.
- Do not modify existing constructors — not applicable, no constructors touched.
- Commit messages should include `Closes #<issue-number>` once the GitHub issue is filed (Task 0).

---

### Task 0: File the GitHub issue

**Files:** none (GitHub only)

- [ ] **Step 1: Create the issue**

Run (replace `<repo>` is already `hmislk/hmis`):

```bash
gh issue create --repo hmislk/hmis \
  --title "Dynamic forms: radio column count hardcoded to 2; edit-from-list opens in edit mode instead of read-only" \
  --body "$(cat <<'EOF'
## Problem

Two defects found while preparing a 22-row Yes/No/N/A discharge checklist as a dynamic form (`DesignComponent`/`CaptureComponent`, `/api/forms` REST API):

### 1. Hardcoded radio column count

`src/main/webapp/inward/admission_forms.xhtml` hardcodes:
```xml
<p:selectOneRadio value="#{cc.shortTextValue}" layout="grid" columns="2"
                  rendered="#{cc.componentPresentationType eq 'SelectOneRadio'}">
```
`columns="2"` is fixed regardless of how many choices the field actually has. A 3-choice field (e.g. Yes/No/N/A) wraps awkwardly — 2 options on one row, the 3rd dangling onto a second row — instead of sitting inline on one row.

**Fix:** derive the column count from the field's live choice count, clamped to a sane range.

### 2. Edit-from-list doesn't default to read-only

`InwardFormController.editForm(PatientFormEntry entry)` never touches the `viewMode` field, so opening an already-filled form from the entries list inherits whatever `viewMode` was last left at in the session (e.g. `false`, left over from a prior `startNewForm()`/`cancelForm()` call), rather than deterministically opening read-only. Filled-in forms should always open in read-only View Mode when opened from the list; the existing "Switch to Edit Mode" button should remain the only way to make them editable.

**Fix:** set `viewMode = true` at the start of `editForm()`.

## Also noted (not fixed here, future enhancement)

The dynamic form system (`DesignComponent`/`CaptureComponent`, C3 hybrid layout — see `developer_docs/forms/custom-layout-guide.md`) has no first-class "matrix/table checklist" component: there's no way to render a single shared header row (e.g. "YES / NO / N/A") spanning many fields' worth of radio/checkbox inputs — each field is a fully independent row. Not a blocker for Yes/No/N/A-style checklists since each radio row already labels its own options, but worth tracking as a future form-designer/API enhancement if more complex matrix-style clinical forms are needed.
EOF
)"
```

Expected: command prints the created issue URL, e.g. `https://github.com/hmislk/hmis/issues/22346`.

- [ ] **Step 2: Record the issue number**

Note the issue number from the URL — it's referenced in every commit message below as `#<issue-number>`.

---

### Task 1: Add a testable radio-column clamp helper

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/InwardFormController.java`
- Test: `src/test/java/com/divudi/bean/inward/InwardFormControllerTest.java` (new)

**Interfaces:**
- Produces: `static int InwardFormController.clampRadioColumns(int choiceCount)` — pure function, no side effects, no EJB dependency. Returns `choiceCount` clamped to `[1, 4]`.
- Produces: `public int InwardFormController.getRadioColumns(CaptureComponent cc)` — instance method used from the xhtml; returns `clampRadioColumns(getChoicesFor(cc).size())`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/divudi/bean/inward/InwardFormControllerTest.java`:

```java
package com.divudi.bean.inward;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InwardFormControllerTest {

    @Test
    void clampRadioColumns_returnsChoiceCount_whenWithinRange() {
        assertEquals(3, InwardFormController.clampRadioColumns(3));
    }

    @Test
    void clampRadioColumns_returnsOne_whenChoiceCountIsZero() {
        assertEquals(1, InwardFormController.clampRadioColumns(0));
    }

    @Test
    void clampRadioColumns_returnsOne_whenChoiceCountIsNegative() {
        assertEquals(1, InwardFormController.clampRadioColumns(-5));
    }

    @Test
    void clampRadioColumns_capsAtFour_whenChoiceCountIsLarge() {
        assertEquals(4, InwardFormController.clampRadioColumns(9));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `"D:\Program Files\NetBeans-18\netbeans\java\maven\bin\mvn.cmd" -q test -Dtest=InwardFormControllerTest`

Expected: FAIL — compile error, `clampRadioColumns` is not defined on `InwardFormController`.

- [ ] **Step 3: Add the pure helper and the instance method**

In `src/main/java/com/divudi/bean/inward/InwardFormController.java`, add both methods near `getChoicesFor` (after line 490, right before `navigateBackToAdmissionProfile`):

```java
    /**
     * Column count for a SelectOneRadio field's PrimeFaces grid layout,
     * derived from how many choices the field actually has (clamped so a
     * 2-choice field doesn't get 1 column and a 10-choice field doesn't
     * get 10 columns).
     */
    static int clampRadioColumns(int choiceCount) {
        return Math.max(1, Math.min(4, choiceCount));
    }

    public int getRadioColumns(CaptureComponent cc) {
        return clampRadioColumns(getChoicesFor(cc).size());
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `"D:\Program Files\NetBeans-18\netbeans\java\maven\bin\mvn.cmd" -q test -Dtest=InwardFormControllerTest`

Expected: PASS — 4 tests, 0 failures.

- [ ] **Step 5: Wire the xhtml to use the new method**

In `src/main/webapp/inward/admission_forms.xhtml`, replace the line (around line 167):

```xml
                        <p:selectOneRadio value="#{cc.shortTextValue}" layout="grid" columns="2"
                                          rendered="#{cc.componentPresentationType eq 'SelectOneRadio'}">
```

with:

```xml
                        <p:selectOneRadio value="#{cc.shortTextValue}" layout="grid"
                                          columns="#{inwardFormController.getRadioColumns(cc)}"
                                          rendered="#{cc.componentPresentationType eq 'SelectOneRadio'}">
```

- [ ] **Step 6: Compile the full project to confirm no regressions**

Run: `"D:\Program Files\NetBeans-18\netbeans\java\maven\bin\mvn.cmd" -q compile`

Expected: `BUILD SUCCESS`, no errors.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/InwardFormController.java src/test/java/com/divudi/bean/inward/InwardFormControllerTest.java src/main/webapp/inward/admission_forms.xhtml
git commit -m "$(cat <<'EOF'
Derive SelectOneRadio column count from field choice count

3-option fields (e.g. Yes/No/N/A checklists) previously wrapped
awkwardly under the hardcoded columns="2". clampRadioColumns() keeps
the layout sane for any choice count between 1 and 4.

Closes #<issue-number>
EOF
)"
```

(Replace `<issue-number>` with the number from Task 0.)

---

### Task 2: Default to read-only when opening a filled form from the list

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/InwardFormController.java:200-216` (`editForm` method)

**Interfaces:**
- Consumes: existing `private boolean viewMode` field, existing `isViewMode()`/`setViewMode()` accessors (already present at lines 551-557).
- Produces: no new public interface — behavior change only.

- [ ] **Step 1: Make the change**

In `src/main/java/com/divudi/bean/inward/InwardFormController.java`, in `editForm(PatientFormEntry entry)` (currently lines 200-216), add `viewMode = true;` right after the null check:

```java
    public void editForm(PatientFormEntry entry) {
        if (entry == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        viewMode = true;
        currentEntry = entry;
        selectedTemplate = entry.getDesignComponent();
        String jpql = "select cc "
                + " from CaptureComponent cc "
                + " where cc.patientFormEntry=:pfe "
                + " and cc.retired=:ret "
                + " order by cc.designComponent.orderNo";
        Map<String, Object> m = new HashMap<>();
        m.put("pfe", entry);
        m.put("ret", false);
        currentCaptureComponents = captureComponentFacade.findByJpql(jpql, m);
    }
```

- [ ] **Step 2: Compile to confirm no regressions**

Run: `"D:\Program Files\NetBeans-18\netbeans\java\maven\bin\mvn.cmd" -q compile`

Expected: `BUILD SUCCESS`, no errors.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/InwardFormController.java
git commit -m "$(cat <<'EOF'
Default to read-only view when opening a filled form from the list

editForm() previously left viewMode untouched, so a filled-in form
opened from the entries list could inherit an editable state from
earlier session actions. Filled forms now always open read-only;
"Switch to Edit Mode" remains the explicit way to edit.

Closes #<issue-number>
EOF
)"
```

(Same issue number as Task 1 — one issue covers both fixes.)

---

### Task 3: Push and open the PR

**Files:** none (git/GitHub only)

**Interfaces:**
- Consumes: the two commits from Task 1 and Task 2, already on branch `22345-discharge-checklist-sample-form`.

- [ ] **Step 1: Push the branch**

```bash
git push -u origin 22345-discharge-checklist-sample-form
```

Expected: push succeeds, prints the new remote branch ref.

- [ ] **Step 2: Open the PR targeting development**

```bash
gh pr create --repo hmislk/hmis --base development \
  --title "Fix dynamic-form radio column count and edit-from-list read-only default" \
  --body "$(cat <<'EOF'
## Summary
- SelectOneRadio fields now size their PrimeFaces grid columns to the field's actual choice count (clamped 1-4) instead of a hardcoded 2, so 3-option fields (e.g. Yes/No/N/A) render inline on one row.
- Opening an already-filled form from the inward forms list now always defaults to read-only View Mode; "Switch to Edit Mode" remains the explicit way to edit.

Prepares the dynamic form system for a 22-row Yes/No/N/A discharge checklist to be created via the `/api/forms` REST API.

Closes #<issue-number>

## Test plan
- [x] `InwardFormControllerTest` (4 new unit tests for `clampRadioColumns`) passes
- [x] `mvn compile` succeeds
- [ ] Manual verification on rh staging after merge: a 3-choice radio field renders inline; opening a filled form from the list opens read-only
EOF
)"
```

Expected: command prints the created PR URL.

---

## After This Plan

Once the PR is merged and rh staging (`<rh staging URL — see C:\Credentials\>`) is redeployed, proceed with Phase 2 from the design spec (`docs/superpowers/specs/2026-07-24-discharge-checklist-form-design.md`): create the discharge checklist form template, fields, and choices via the `/api/forms` REST API using the `Finance` header, verify it in the browser, and write the wiki page. This is data/content work, not code, and does not need its own TDD-style plan.
