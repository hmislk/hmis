# BHT Pharmacy Issue Request — Professional POS/FiveFive/A4 Print Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the inpatient pharmacy "BHT Issue Request" document professional POS/FiveFive/A4 print formats plus a print-config gear button, via one new native-SQL-backed receipt composite and one new config-button composite, used from all 3 places it currently appears.

**Architecture:** A new DTO pair (`BhtIssueRequestPrintDto` + item DTO) is populated by a new `@Stateless` native-SQL service (two LEFT-JOIN queries: header + items). Two new composite components — `phprint:bht_issue_request_receipt` (one file, 3 internal format blocks) and `phprint:bht_issue_request_print_config_button` (self-contained gear button + dialog) — replace the old entity-based composite on all 3 consuming pages. Each of the 3 controllers gets a lazily-populated, per-bill-id-cached DTO getter (chosen over hooking a single "load" method, because none of the 3 controllers has one single safe hook point — see Task 6/7/8 notes).

**Tech Stack:** Java EE (JSF/PrimeFaces, Facelets composites), EclipseLink JPA + native SQL (`EntityManager`), MySQL, JUnit 5.

## Global Constraints

- JPQL first, native SQL last — **exception granted here**: this feature deliberately follows the project's documented native-SQL print-page pattern (`developer_docs/billing/native-sql-print-page-guide.md`) for read-only print DTOs, which is an established, sanctioned use of native SQL for this exact scenario.
- Use `findLongByJpql` for JPQL `COUNT` — not applicable, no JPQL in this feature.
- Never modify existing constructors — not applicable, no entity constructors touched.
- Never rename composite components without checking all usages — the old composite `pharmacy_bht_issue_request_receipt.xhtml` is being retired; Task 9 greps for and confirms all usages are migrated first.
- Primitive `boolean` fields use `isX()` getters, not `getX()` — applies to `Bill.isCompleted()`/`isCancelled()` (already correct in the codebase) and to the new DTO's `boolean completed`/`cancelled` fields (must generate `isCompleted()`/`isCancelled()`).
- Persistence unit name for native queries: `"hmisPU"` (confirmed from `PurchaseOrderNativeSqlService`).
- After every `git push`, restore local JNDI in `persistence.xml` — not triggered until the final commit/push step, and only if the user asks to push.

---

## File Structure

**New files:**
- `src/main/java/com/divudi/core/data/dto/pharmacy/BhtIssueRequestPrintDto.java` — header-level print DTO.
- `src/main/java/com/divudi/core/data/dto/pharmacy/BhtIssueRequestItemPrintDto.java` — line-item print DTO.
- `src/main/java/com/divudi/service/pharmacy/BhtIssueRequestNativeSqlService.java` — `@Stateless` native-SQL loader.
- `src/main/webapp/resources/pharmacy/print/bht_issue_request_receipt.xhtml` — the single receipt composite (3 internal format blocks).
- `src/main/webapp/resources/pharmacy/print/bht_issue_request_print_config_button.xhtml` — the config gear-button+dialog composite.
- `src/test/java/com/divudi/core/data/dto/pharmacy/BhtIssueRequestPrintDtoTest.java`
- `src/test/java/com/divudi/service/pharmacy/BhtIssueRequestNativeSqlServiceTest.java`

**Modified files:**
- `src/main/java/com/divudi/bean/pharmacy/PharmacyConfigController.java` — add 3 boolean fields + load lines + save method + getters/setters.
- `src/main/java/com/divudi/bean/pharmacy/PharmacyBillSearch.java` — inject service, add cached DTO getter.
- `src/main/java/com/divudi/bean/pharmacy/PharmacyRequestForBhtController.java` — inject service, add cached DTO getter.
- `src/main/java/com/divudi/bean/pharmacy/PharmacySaleBhtController.java` — inject service, add cached DTO getter.
- `src/main/webapp/ward/ward_pharmacy_reprint_bht_issue_request.xhtml` — swap composite, add gear button.
- `src/main/webapp/ward/ward_pharmacy_bht_issue_request_bill.xhtml` — swap composite, add gear button.
- `src/main/webapp/ward/ward_pharmacy_bht_issue.xhtml` — swap composite (no gear button).

**Deleted files:**
- `src/main/webapp/resources/pharmacy/pharmacy_bht_issue_request_receipt.xhtml` — old entity-based composite (Task 9, after all 3 call sites are migrated).

---

### Task 1: Print DTOs

**Files:**
- Create: `src/main/java/com/divudi/core/data/dto/pharmacy/BhtIssueRequestItemPrintDto.java`
- Create: `src/main/java/com/divudi/core/data/dto/pharmacy/BhtIssueRequestPrintDto.java`
- Test: `src/test/java/com/divudi/core/data/dto/pharmacy/BhtIssueRequestPrintDtoTest.java`

**Interfaces:**
- Produces: `BhtIssueRequestPrintDto` with fields `fromDepartmentPrintingName`, `fromDepartmentName`, `fromDepartmentAddress`, `fromDepartmentTelephone1`, `fromDepartmentTelephone2`, `fromDepartmentFax`, `toDepartmentName`, `requestNo`, `createdAt` (`java.util.Date`), `bhtNo`, `patientName`, `patientPhn`, `roomName`, `requestedByName`, `systemUserName`, `comments` (all `String`), `completed`, `cancelled` (`boolean`), `items` (`List<BhtIssueRequestItemPrintDto>`). Produces: `BhtIssueRequestItemPrintDto` with fields `itemName` (`String`), `qty` (`double`), `directions` (`String`).

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/divudi/core/data/dto/pharmacy/BhtIssueRequestPrintDtoTest.java`:

```java
package com.divudi.core.data.dto.pharmacy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BhtIssueRequestPrintDtoTest {

    @Test
    void items_defaultsToEmptyMutableList() {
        BhtIssueRequestPrintDto dto = new BhtIssueRequestPrintDto();
        assertNotNull(dto.getItems());
        assertTrue(dto.getItems().isEmpty());
        dto.getItems().add(new BhtIssueRequestItemPrintDto());
        assertEquals(1, dto.getItems().size());
    }

    @Test
    void stringFields_defaultToEmptyStringNotNull() {
        BhtIssueRequestPrintDto dto = new BhtIssueRequestPrintDto();
        assertEquals("", dto.getFromDepartmentPrintingName());
        assertEquals("", dto.getFromDepartmentName());
        assertEquals("", dto.getPatientName());
        assertEquals("", dto.getRequestedByName());
        assertEquals("", dto.getComments());
    }

    @Test
    void booleanFields_defaultToFalse() {
        BhtIssueRequestPrintDto dto = new BhtIssueRequestPrintDto();
        assertFalse(dto.isCompleted());
        assertFalse(dto.isCancelled());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./detect-maven.sh test -Dtest=BhtIssueRequestPrintDtoTest`
Expected: FAIL — compilation error, `BhtIssueRequestPrintDto`/`BhtIssueRequestItemPrintDto` do not exist.

- [ ] **Step 3: Write the DTO classes**

Create `src/main/java/com/divudi/core/data/dto/pharmacy/BhtIssueRequestItemPrintDto.java`:

```java
package com.divudi.core.data.dto.pharmacy;

import java.io.Serializable;

public class BhtIssueRequestItemPrintDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String itemName = "";
    private double qty;
    private String directions = "";

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public String getDirections() {
        return directions;
    }

    public void setDirections(String directions) {
        this.directions = directions;
    }
}
```

Create `src/main/java/com/divudi/core/data/dto/pharmacy/BhtIssueRequestPrintDto.java`:

```java
package com.divudi.core.data.dto.pharmacy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BhtIssueRequestPrintDto implements Serializable {

    private static final long serialVersionUID = 1L;

    // ---- from department ----
    private String fromDepartmentPrintingName = "";
    private String fromDepartmentName = "";
    private String fromDepartmentAddress = "";
    private String fromDepartmentTelephone1 = "";
    private String fromDepartmentTelephone2 = "";
    private String fromDepartmentFax = "";

    // ---- to department ----
    private String toDepartmentName = "";

    // ---- bill header ----
    private String requestNo = "";
    private Date createdAt;
    private String comments = "";
    private boolean completed;
    private boolean cancelled;

    // ---- patient / encounter ----
    private String bhtNo = "";
    private String patientName = "";
    private String patientPhn = "";
    private String roomName = "";

    // ---- requester ----
    private String requestedByName = "";
    private String systemUserName = "";

    private List<BhtIssueRequestItemPrintDto> items = new ArrayList<>();

    public String getFromDepartmentPrintingName() {
        return fromDepartmentPrintingName;
    }

    public void setFromDepartmentPrintingName(String fromDepartmentPrintingName) {
        this.fromDepartmentPrintingName = fromDepartmentPrintingName;
    }

    public String getFromDepartmentName() {
        return fromDepartmentName;
    }

    public void setFromDepartmentName(String fromDepartmentName) {
        this.fromDepartmentName = fromDepartmentName;
    }

    public String getFromDepartmentAddress() {
        return fromDepartmentAddress;
    }

    public void setFromDepartmentAddress(String fromDepartmentAddress) {
        this.fromDepartmentAddress = fromDepartmentAddress;
    }

    public String getFromDepartmentTelephone1() {
        return fromDepartmentTelephone1;
    }

    public void setFromDepartmentTelephone1(String fromDepartmentTelephone1) {
        this.fromDepartmentTelephone1 = fromDepartmentTelephone1;
    }

    public String getFromDepartmentTelephone2() {
        return fromDepartmentTelephone2;
    }

    public void setFromDepartmentTelephone2(String fromDepartmentTelephone2) {
        this.fromDepartmentTelephone2 = fromDepartmentTelephone2;
    }

    public String getFromDepartmentFax() {
        return fromDepartmentFax;
    }

    public void setFromDepartmentFax(String fromDepartmentFax) {
        this.fromDepartmentFax = fromDepartmentFax;
    }

    public String getToDepartmentName() {
        return toDepartmentName;
    }

    public void setToDepartmentName(String toDepartmentName) {
        this.toDepartmentName = toDepartmentName;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhn() {
        return patientPhn;
    }

    public void setPatientPhn(String patientPhn) {
        this.patientPhn = patientPhn;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRequestedByName() {
        return requestedByName;
    }

    public void setRequestedByName(String requestedByName) {
        this.requestedByName = requestedByName;
    }

    public String getSystemUserName() {
        return systemUserName;
    }

    public void setSystemUserName(String systemUserName) {
        this.systemUserName = systemUserName;
    }

    public List<BhtIssueRequestItemPrintDto> getItems() {
        return items;
    }

    public void setItems(List<BhtIssueRequestItemPrintDto> items) {
        this.items = items;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./detect-maven.sh test -Dtest=BhtIssueRequestPrintDtoTest`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/core/data/dto/pharmacy/BhtIssueRequestItemPrintDto.java src/main/java/com/divudi/core/data/dto/pharmacy/BhtIssueRequestPrintDto.java src/test/java/com/divudi/core/data/dto/pharmacy/BhtIssueRequestPrintDtoTest.java
git commit -m "feat(pharmacy): add BHT issue request print DTOs"
```

---

### Task 2: Native SQL service

**Files:**
- Create: `src/main/java/com/divudi/service/pharmacy/BhtIssueRequestNativeSqlService.java`
- Test: `src/test/java/com/divudi/service/pharmacy/BhtIssueRequestNativeSqlServiceTest.java`

**Interfaces:**
- Consumes: `BhtIssueRequestPrintDto`, `BhtIssueRequestItemPrintDto` (Task 1).
- Produces: `BhtIssueRequestNativeSqlService.loadPrintDtoByBillId(long billId)` returns `BhtIssueRequestPrintDto` or `null`. Package-private helpers `str(Object)`, `toBool(Object)`, `toDate(Object)`, `titleLabel(String)` for later tasks/tests to call directly (test lives in the same package).

Verified DB columns (queried directly against the local `coop` schema): `bill(ID, FROMDEPARTMENT_ID, TODEPARTMENT_ID, DEPTID, CREATEDAT, PATIENTENCOUNTER_ID, CREATER_ID, COMMENTS, COMPLETED, CANCELLED, RETIRED)`, `department(ID, PRINTINGNAME, NAME, ADDRESS, TELEPHONE1, TELEPHONE2, FAX)`, `patientencounter(ID, BHTNO, PATIENT_ID, CURRENTPATIENTROOM_ID)`, `patient(ID, PERSON_ID, PHN)`, `patientroom(ID, ROOMFACILITYCHARGE_ID)`, `roomfacilitycharge(ID, NAME)`, `webuser(ID, WEBUSERPERSON_ID, NAME)`, `person(ID, NAME, TITLE)` (note: `person` has **no** `NAMEWITHTITLE` column — it's a `@Transient` Java property on `Person` built from `TITLE` + `NAME`; the service must replicate `Person.getNameWithTitle()`'s exact concatenation: `title.getLabel() + " " + name`, where `Title.getLabel()` already includes a trailing space/period, e.g. `"Mr. "` — do **not** trim the result, to match existing entity behavior exactly, including its harmless double-space when a title is present), `billitem(ID, BILL_ID, ITEM_ID, QTY, DESCREPTION, RETIRED)`, `item(ID, NAME)`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/divudi/service/pharmacy/BhtIssueRequestNativeSqlServiceTest.java`:

```java
package com.divudi.service.pharmacy;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class BhtIssueRequestNativeSqlServiceTest {

    private final BhtIssueRequestNativeSqlService service = new BhtIssueRequestNativeSqlService();

    @Test
    void str_returnsEmptyStringForNull() {
        assertEquals("", service.str(null));
    }

    @Test
    void str_trimsValue() {
        assertEquals("Ward 3", service.str("  Ward 3  "));
    }

    @Test
    void toBool_handlesBooleanNumberAndNull() {
        assertFalse(service.toBool(null));
        assertTrue(service.toBool(Boolean.TRUE));
        assertFalse(service.toBool(Boolean.FALSE));
        assertTrue(service.toBool(1));
        assertFalse(service.toBool(0));
    }

    @Test
    void toDate_convertsTimestamp() {
        long millis = System.currentTimeMillis();
        Date result = service.toDate(new Timestamp(millis));
        assertEquals(millis, result.getTime());
    }

    @Test
    void toDate_returnsNullForNull() {
        assertNull(service.toDate(null));
    }

    @Test
    void titleLabel_returnsEmptyForNullOrBlank() {
        assertEquals("", service.titleLabel(null));
        assertEquals("", service.titleLabel("   "));
    }

    @Test
    void titleLabel_returnsEmptyForUnknownTitle() {
        assertEquals("", service.titleLabel("NotARealTitle"));
    }

    @Test
    void titleLabel_returnsLabelForKnownTitle() {
        assertEquals("Mr. ", service.titleLabel("Mr"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./detect-maven.sh test -Dtest=BhtIssueRequestNativeSqlServiceTest`
Expected: FAIL — compilation error, `BhtIssueRequestNativeSqlService` does not exist.

- [ ] **Step 3: Write the service**

Create `src/main/java/com/divudi/service/pharmacy/BhtIssueRequestNativeSqlService.java`:

```java
package com.divudi.service.pharmacy;

import com.divudi.core.data.Title;
import com.divudi.core.data.dto.pharmacy.BhtIssueRequestItemPrintDto;
import com.divudi.core.data.dto.pharmacy.BhtIssueRequestPrintDto;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class BhtIssueRequestNativeSqlService {

    private static final Logger LOGGER = Logger.getLogger(BhtIssueRequestNativeSqlService.class.getName());

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    public BhtIssueRequestPrintDto loadPrintDtoByBillId(long billId) {
        try {
            String sql = "SELECT "
                    + "fd.PRINTINGNAME, fd.NAME, fd.ADDRESS, fd.TELEPHONE1, fd.TELEPHONE2, fd.FAX, "
                    + "td.NAME, "
                    + "b.DEPTID, b.CREATEDAT, "
                    + "pe.BHTNO, pp.NAME, pp.TITLE, pt.PHN, rfc.NAME, "
                    + "rp.NAME, rp.TITLE, wu.NAME, "
                    + "b.COMMENTS, b.COMPLETED, b.CANCELLED "
                    + "FROM bill b "
                    + "LEFT JOIN department fd ON fd.ID = b.FROMDEPARTMENT_ID "
                    + "LEFT JOIN department td ON td.ID = b.TODEPARTMENT_ID "
                    + "LEFT JOIN patientencounter pe ON pe.ID = b.PATIENTENCOUNTER_ID "
                    + "LEFT JOIN patient pt ON pt.ID = pe.PATIENT_ID "
                    + "LEFT JOIN person pp ON pp.ID = pt.PERSON_ID "
                    + "LEFT JOIN patientroom pr ON pr.ID = pe.CURRENTPATIENTROOM_ID "
                    + "LEFT JOIN roomfacilitycharge rfc ON rfc.ID = pr.ROOMFACILITYCHARGE_ID "
                    + "LEFT JOIN webuser wu ON wu.ID = b.CREATER_ID "
                    + "LEFT JOIN person rp ON rp.ID = wu.WEBUSERPERSON_ID "
                    + "WHERE b.ID = ?1 AND b.RETIRED = 0";

            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(sql).setParameter(1, billId).getResultList();
            if (rows.isEmpty()) {
                return null;
            }
            Object[] r = rows.get(0);
            int col = 0;

            BhtIssueRequestPrintDto dto = new BhtIssueRequestPrintDto();
            dto.setFromDepartmentPrintingName(str(r[col++]));
            dto.setFromDepartmentName(str(r[col++]));
            dto.setFromDepartmentAddress(str(r[col++]));
            dto.setFromDepartmentTelephone1(str(r[col++]));
            dto.setFromDepartmentTelephone2(str(r[col++]));
            dto.setFromDepartmentFax(str(r[col++]));
            dto.setToDepartmentName(str(r[col++]));
            dto.setRequestNo(str(r[col++]));
            dto.setCreatedAt(toDate(r[col++]));
            dto.setBhtNo(str(r[col++]));

            String patientPlainName = str(r[col++]);
            String patientTitle = str(r[col++]);
            dto.setPatientName(titleLabel(patientTitle) + " " + patientPlainName);

            dto.setPatientPhn(str(r[col++]));
            dto.setRoomName(str(r[col++]));

            String requesterPlainName = str(r[col++]);
            String requesterTitle = str(r[col++]);
            dto.setRequestedByName(titleLabel(requesterTitle) + " " + requesterPlainName);

            dto.setSystemUserName(str(r[col++]));
            dto.setComments(str(r[col++]));
            dto.setCompleted(toBool(r[col++]));
            dto.setCancelled(toBool(r[col++]));

            dto.setItems(loadItems(billId));
            return dto;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load BHT issue request print DTO for bill " + billId, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<BhtIssueRequestItemPrintDto> loadItems(long billId) {
        List<BhtIssueRequestItemPrintDto> items = new ArrayList<>();
        String sql = "SELECT i.NAME, bi.QTY, bi.DESCREPTION "
                + "FROM billitem bi "
                + "LEFT JOIN item i ON i.ID = bi.ITEM_ID "
                + "WHERE bi.BILL_ID = ?1 AND bi.RETIRED = 0 "
                + "ORDER BY bi.ID";
        List<Object[]> rows = em.createNativeQuery(sql).setParameter(1, billId).getResultList();
        for (Object[] r : rows) {
            int col = 0;
            BhtIssueRequestItemPrintDto item = new BhtIssueRequestItemPrintDto();
            item.setItemName(str(r[col++]));
            item.setQty(r[col] != null ? ((Number) r[col]).doubleValue() : 0.0);
            col++;
            item.setDirections(str(r[col++]));
            items.add(item);
        }
        return items;
    }

    String str(Object o) {
        return o != null ? o.toString().trim() : "";
    }

    boolean toBool(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue() != 0;
        }
        return false;
    }

    Date toDate(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Timestamp) {
            return new Date(((Timestamp) o).getTime());
        }
        if (o instanceof Date) {
            return (Date) o;
        }
        return null;
    }

    String titleLabel(String titleName) {
        if (titleName == null || titleName.trim().isEmpty()) {
            return "";
        }
        try {
            return Title.valueOf(titleName.trim()).getLabel();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./detect-maven.sh test -Dtest=BhtIssueRequestNativeSqlServiceTest`
Expected: PASS (8 tests). The query methods (`loadPrintDtoByBillId`, `loadItems`) are not unit-tested here (no container-managed `EntityManager` available outside Payara) — they are verified against the real local database in Task 10's manual E2E pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/service/pharmacy/BhtIssueRequestNativeSqlService.java src/test/java/com/divudi/service/pharmacy/BhtIssueRequestNativeSqlServiceTest.java
git commit -m "feat(pharmacy): add native-SQL loader for BHT issue request print DTO"
```

---

### Task 3: Receipt composite (POS/FiveFive/A4)

**Files:**
- Create: `src/main/webapp/resources/pharmacy/print/bht_issue_request_receipt.xhtml`

**Interfaces:**
- Consumes: `BhtIssueRequestPrintDto` (Task 1), config keys `Pharmacy BHT Issue Request Receipt is A4` / `...is FiveFive` / `...is POS` (application-wide, read directly via `configOptionApplicationController.getBooleanValueByKey`), and the pre-existing keys `Pharmacy BHT Issue Request Receipt CSS` / `...Header` / `...Footer`.
- Produces: composite tag `phprint:bht_issue_request_receipt`, attributes `dto` (`BhtIssueRequestPrintDto`, required), `duplicate` (optional boolean), `comment` (optional String, overrides `dto.comments` when non-blank).

This is a JSF-only file (no Java compile step); per project convention it's verified visually in Task 10, not via a Maven test run. No test/run/commit micro-steps for this task — it is one file, reviewed as a whole and committed with Task 4 (the companion composite) since neither is independently wireable into a page without the other's sibling controller changes (Tasks 6–8).

- [ ] **Step 1: Create the composite**

Create `src/main/webapp/resources/pharmacy/print/bht_issue_request_receipt.xhtml`:

```xml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:cc="http://xmlns.jcp.org/jsf/composite"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets">

    <!-- INTERFACE -->
    <cc:interface>
        <cc:attribute name="dto" type="com.divudi.core.data.dto.pharmacy.BhtIssueRequestPrintDto" required="true"/>
        <cc:attribute name="duplicate"/>
        <cc:attribute name="comment"/>
    </cc:interface>

    <!-- IMPLEMENTATION -->
    <cc:implementation>

        <style>
            .biq-status-pending { color: #b8860b; font-weight: bold; }
            .biq-status-completed { color: #1a7a1a; font-weight: bold; }
            .biq-status-cancelled { color: #c0392b; font-weight: bold; }
            .biq-watermark { color: #c0392b; font-weight: bold; }
            .biq-signature-row { display: flex; justify-content: space-between; margin-top: 2em; }
            .biq-signature-line { border-top: 1px solid #000; width: 45%; text-align: center; padding-top: 4px; font-size: 0.85em; }

            .biq-a4 { width: 100%; font-family: Arial, sans-serif; font-size: 13px; color: #000; padding: 1.5em; }
            .biq-a4 table { width: 100%; border-collapse: collapse; }
            .biq-a4 .biq-items th, .biq-a4 .biq-items td { border: 1px solid #999; padding: 4px 6px; }

            .biq-five-five { width: 100%; font-family: Arial, sans-serif; font-size: 11px; color: #000; padding: 0.5em; }
            .biq-five-five table { width: 100%; border-collapse: collapse; }
            .biq-five-five .biq-items th, .biq-five-five .biq-items td { border-bottom: 1px dashed #999; padding: 2px 4px; }

            .biq-pos { width: 100%; font-family: 'Courier New', monospace; font-size: 10px; color: #000; padding: 0.25em; }
            .biq-pos table { width: 100%; }
            .biq-pos .biq-items td { padding: 1px 2px; }

            @media print {
                .biq-a4, .biq-five-five, .biq-pos { -webkit-print-color-adjust: exact; }
            }
        </style>

        <h:outputText escape="false"
                      value="#{configOptionApplicationController.getLongTextValueByKey('Pharmacy BHT Issue Request Receipt CSS')}"/>

        <!-- ==================== A4 ==================== -->
        <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy BHT Issue Request Receipt is A4', false)}">
            <div class="biq-a4">
                <h:outputText escape="false"
                              value="#{configOptionApplicationController.getLongTextValueByKey('Pharmacy BHT Issue Request Receipt Header')}"/>

                <div style="text-align:center;">
                    <strong><h:outputText value="#{cc.attrs.dto.fromDepartmentPrintingName}"/></strong><br/>
                    <h:panelGroup rendered="#{cc.attrs.dto.fromDepartmentAddress ne ''}">
                        <h:outputText value="#{cc.attrs.dto.fromDepartmentAddress}"/><br/>
                    </h:panelGroup>
                    <h:panelGroup rendered="#{cc.attrs.dto.fromDepartmentTelephone1 ne ''}">
                        <h:outputText value="#{cc.attrs.dto.fromDepartmentTelephone1}"/>
                    </h:panelGroup>
                    <h:panelGroup rendered="#{cc.attrs.dto.fromDepartmentTelephone2 ne ''}">
                        <h:outputText value=" / #{cc.attrs.dto.fromDepartmentTelephone2}"/>
                    </h:panelGroup>
                    <br/>
                    <h:panelGroup rendered="#{cc.attrs.dto.fromDepartmentFax ne ''}">
                        <h:outputText value="Fax: #{cc.attrs.dto.fromDepartmentFax}"/>
                    </h:panelGroup>
                </div>

                <h2 style="text-align:center;">
                    BHT Pharmacy Issue Request
                    <h:panelGroup rendered="#{cc.attrs.duplicate eq true}">
                        <span class="biq-watermark"> **DUPLICATE**</span>
                    </h:panelGroup>
                </h2>
                <div style="text-align:center;">
                    <h:panelGroup rendered="#{cc.attrs.dto.cancelled eq true}">
                        <span class="biq-status-cancelled">CANCELLED</span>
                    </h:panelGroup>
                    <h:panelGroup rendered="#{cc.attrs.dto.cancelled eq false and cc.attrs.dto.completed eq true}">
                        <span class="biq-status-completed">COMPLETED</span>
                    </h:panelGroup>
                    <h:panelGroup rendered="#{cc.attrs.dto.cancelled eq false and cc.attrs.dto.completed eq false}">
                        <span class="biq-status-pending">PENDING</span>
                    </h:panelGroup>
                </div>
                <hr/>

                <table>
                    <tr>
                        <td>Date</td><td>:</td>
                        <td>
                            <h:outputLabel value="#{cc.attrs.dto.createdAt}">
                                <f:convertDateTime pattern="#{sessionController.applicationPreference.longDateFormat}"/>
                            </h:outputLabel>
                            <h:outputLabel value="#{cc.attrs.dto.createdAt}">
                                <f:convertDateTime timeZone="Asia/Colombo" pattern="#{sessionController.applicationPreference.shortTimeFormat}"/>
                            </h:outputLabel>
                        </td>
                    </tr>
                    <tr><td>Request No</td><td>:</td><td><h:outputText value="#{cc.attrs.dto.requestNo}"/></td></tr>
                    <tr><td>BHT No</td><td>:</td><td><h:outputText value="#{cc.attrs.dto.bhtNo}"/></td></tr>
                    <tr><td>Patient Name</td><td>:</td><td><h:outputText value="#{cc.attrs.dto.patientName}"/></td></tr>
                    <h:panelGroup rendered="#{cc.attrs.dto.patientPhn ne ''}">
                        <tr><td>PHN</td><td>:</td><td><h:outputText value="#{cc.attrs.dto.patientPhn}"/></td></tr>
                    </h:panelGroup>
                    <h:panelGroup rendered="#{cc.attrs.dto.roomName ne ''}">
                        <tr><td>Ward/Room</td><td>:</td><td><h:outputText value="#{cc.attrs.dto.roomName}"/></td></tr>
                    </h:panelGroup>
                    <tr><td>Requesting Dept</td><td>:</td><td><h:outputText value="#{cc.attrs.dto.fromDepartmentName}"/></td></tr>
                    <tr><td>Pharmacy Dept</td><td>:</td><td><h:outputText value="#{cc.attrs.dto.toDepartmentName}"/></td></tr>
                    <tr><td>Requested By</td><td>:</td><td><h:outputText value="#{cc.attrs.dto.requestedByName}"/></td></tr>
                    <h:panelGroup rendered="#{(cc.attrs.comment ne null and cc.attrs.comment ne '') or cc.attrs.dto.comments ne ''}">
                        <tr>
                            <td>Comments</td><td>:</td>
                            <td><h:outputText value="#{(cc.attrs.comment ne null and cc.attrs.comment ne '') ? cc.attrs.comment : cc.attrs.dto.comments}"/></td>
                        </tr>
                    </h:panelGroup>
                </table>
                <hr/>

                <table class="biq-items">
                    <thead>
                        <tr><th>#</th><th>Item</th><th>Quantity</th><th>Directions</th></tr>
                    </thead>
                    <tbody>
                        <ui:repeat value="#{cc.attrs.dto.items}" var="bii" varStatus="st">
                            <tr>
                                <td><h:outputText value="#{st.index + 1}"/></td>
                                <td><h:outputText value="#{bii.itemName}"/></td>
                                <td><h:outputText value="#{bii.qty}"><f:convertNumber pattern="#,##0"/></h:outputText></td>
                                <td><h:outputText value="#{bii.directions}"/></td>
                            </tr>
                        </ui:repeat>
                    </tbody>
                </table>
                <hr/>
                <p>Number of Items: <h:outputText value="#{cc.attrs.dto.items.size()}"><f:convertNumber pattern="#,##0"/></h:outputText></p>

                <div class="biq-signature-row">
                    <div class="biq-signature-line">Requested By</div>
                    <div class="biq-signature-line">Issued By</div>
                </div>

                <p>System User: <h:outputText value="#{cc.attrs.dto.systemUserName}"/></p>

                <h:outputText escape="false"
                              value="#{configOptionApplicationController.getLongTextValueByKey('Pharmacy BHT Issue Request Receipt Footer')}"/>
            </div>
        </h:panelGroup>

        <!-- ==================== FiveFive ==================== -->
        <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy BHT Issue Request Receipt is FiveFive', true)}">
            <div class="biq-five-five">
                <h:outputText escape="false"
                              value="#{configOptionApplicationController.getLongTextValueByKey('Pharmacy BHT Issue Request Receipt Header')}"/>

                <div style="text-align:center;">
                    <strong><h:outputText value="#{cc.attrs.dto.fromDepartmentPrintingName}"/></strong>
                </div>

                <div style="text-align:center;">
                    <strong>BHT Pharmacy Issue Request</strong>
                    <h:panelGroup rendered="#{cc.attrs.duplicate eq true}">
                        <span class="biq-watermark"> DUPLICATE</span>
                    </h:panelGroup>
                    <br/>
                    <h:panelGroup rendered="#{cc.attrs.dto.cancelled eq true}">
                        <span class="biq-status-cancelled">CANCELLED</span>
                    </h:panelGroup>
                    <h:panelGroup rendered="#{cc.attrs.dto.cancelled eq false and cc.attrs.dto.completed eq true}">
                        <span class="biq-status-completed">COMPLETED</span>
                    </h:panelGroup>
                    <h:panelGroup rendered="#{cc.attrs.dto.cancelled eq false and cc.attrs.dto.completed eq false}">
                        <span class="biq-status-pending">PENDING</span>
                    </h:panelGroup>
                </div>
                <hr/>

                <table>
                    <tr><td>Date</td><td>:</td><td>
                        <h:outputLabel value="#{cc.attrs.dto.createdAt}">
                            <f:convertDateTime pattern="#{sessionController.applicationPreference.longDateFormat}"/>
                        </h:outputLabel>
                    </td></tr>
                    <tr><td>Req No</td><td>:</td><td><h:outputText value="#{cc.attrs.dto.requestNo}"/></td></tr>
                    <tr><td>BHT No</td><td>:</td><td><h:outputText value="#{cc.attrs.dto.bhtNo}"/></td></tr>
                    <tr><td>Patient</td><td>:</td><td><h:outputText value="#{cc.attrs.dto.patientName}"/></td></tr>
                    <h:panelGroup rendered="#{cc.attrs.dto.roomName ne ''}">
                        <tr><td>Room</td><td>:</td><td><h:outputText value="#{cc.attrs.dto.roomName}"/></td></tr>
                    </h:panelGroup>
                    <tr><td>To</td><td>:</td><td><h:outputText value="#{cc.attrs.dto.toDepartmentName}"/></td></tr>
                    <tr><td>By</td><td>:</td><td><h:outputText value="#{cc.attrs.dto.requestedByName}"/></td></tr>
                </table>
                <hr/>

                <table class="biq-items">
                    <thead><tr><th>Item</th><th>Qty</th><th>Directions</th></tr></thead>
                    <tbody>
                        <ui:repeat value="#{cc.attrs.dto.items}" var="bii">
                            <tr>
                                <td><h:outputText value="#{bii.itemName}"/></td>
                                <td><h:outputText value="#{bii.qty}"><f:convertNumber pattern="#,##0"/></h:outputText></td>
                                <td><h:outputText value="#{bii.directions}"/></td>
                            </tr>
                        </ui:repeat>
                    </tbody>
                </table>
                <hr/>

                <div class="biq-signature-row">
                    <div class="biq-signature-line">Req By</div>
                    <div class="biq-signature-line">Issued By</div>
                </div>

                <h:outputText escape="false"
                              value="#{configOptionApplicationController.getLongTextValueByKey('Pharmacy BHT Issue Request Receipt Footer')}"/>
            </div>
        </h:panelGroup>

        <!-- ==================== POS ==================== -->
        <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy BHT Issue Request Receipt is POS', false)}">
            <div class="biq-pos">
                <div style="text-align:center;">
                    <strong><h:outputText value="#{cc.attrs.dto.fromDepartmentPrintingName}"/></strong><br/>
                    BHT Issue Request
                    <h:panelGroup rendered="#{cc.attrs.duplicate eq true}">
                        <span class="biq-watermark"> (DUP)</span>
                    </h:panelGroup>
                    <br/>
                    <h:panelGroup rendered="#{cc.attrs.dto.cancelled eq true}">
                        <span class="biq-status-cancelled">CANCELLED</span>
                    </h:panelGroup>
                    <h:panelGroup rendered="#{cc.attrs.dto.cancelled eq false and cc.attrs.dto.completed eq true}">
                        <span class="biq-status-completed">COMPLETED</span>
                    </h:panelGroup>
                    <h:panelGroup rendered="#{cc.attrs.dto.cancelled eq false and cc.attrs.dto.completed eq false}">
                        <span class="biq-status-pending">PENDING</span>
                    </h:panelGroup>
                </div>
                <hr/>
                <div>
                    BHT: <h:outputText value="#{cc.attrs.dto.bhtNo}"/><br/>
                    Patient: <h:outputText value="#{cc.attrs.dto.patientName}"/><br/>
                    Req No: <h:outputText value="#{cc.attrs.dto.requestNo}"/><br/>
                    <h:outputLabel value="#{cc.attrs.dto.createdAt}">
                        <f:convertDateTime pattern="#{sessionController.applicationPreference.longDateFormat}"/>
                    </h:outputLabel>
                </div>
                <hr/>
                <table class="biq-items">
                    <ui:repeat value="#{cc.attrs.dto.items}" var="bii">
                        <tr>
                            <td colspan="2"><h:outputText value="#{bii.itemName}"/></td>
                        </tr>
                        <tr>
                            <td>&#160;&#160;Qty: <h:outputText value="#{bii.qty}"><f:convertNumber pattern="#,##0"/></h:outputText></td>
                            <td><h:outputText value="#{bii.directions}"/></td>
                        </tr>
                    </ui:repeat>
                </table>
                <hr/>
                <div>Req By: <h:outputText value="#{cc.attrs.dto.requestedByName}"/></div>

                <div class="biq-signature-row">
                    <div class="biq-signature-line">Req</div>
                    <div class="biq-signature-line">Issued</div>
                </div>
            </div>
        </h:panelGroup>

    </cc:implementation>
</html>
```

- [ ] **Step 2: Manual check (no automated test for JSF-only files)**

Confirm the file parses as well-formed XML: `python -c "import xml.etree.ElementTree as ET; ET.parse('src/main/webapp/resources/pharmacy/print/bht_issue_request_receipt.xhtml')"` — expect no exception. (Full visual/functional verification happens in Task 10 once the composite is wired into a real page.)

---

### Task 4: Print-config gear button composite

**Files:**
- Create: `src/main/webapp/resources/pharmacy/print/bht_issue_request_print_config_button.xhtml`

**Interfaces:**
- Consumes: `pharmacyConfigController.loadCurrentConfig()` (existing shared method, extended in Task 5), `pharmacyConfigController.bhtIssueRequestReceiptA4/FiveFive/Pos` (new bean properties from Task 5), `pharmacyConfigController.saveBhtIssueRequestReceiptConfig()` (new method from Task 5), privilege `ChangeReceiptPrintingPaperTypes` (pre-existing, used unchanged).
- Produces: composite tag `phprint:bht_issue_request_print_config_button`, no required attributes.

- [ ] **Step 1: Create the composite**

Create `src/main/webapp/resources/pharmacy/print/bht_issue_request_print_config_button.xhtml`:

```xml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:cc="http://xmlns.jcp.org/jsf/composite"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:p="http://primefaces.org/ui">

    <!-- INTERFACE -->
    <cc:interface>
    </cc:interface>

    <!-- IMPLEMENTATION -->
    <cc:implementation>

        <p:commandButton
            id="btnBhtIssueRequestPrintConfig"
            rendered="#{webUserController.hasPrivilege('ChangeReceiptPrintingPaperTypes')}"
            icon="fas fa-cog"
            styleClass="ui-button-secondary ui-button-outlined"
            type="button"
            title="Print Format Settings"
            onclick="PF('bhtIssueRequestPrintConfigDialog').show();"/>

        <p:dialog id="bhtIssueRequestPrintConfigDialog"
                  header="BHT Issue Request - Print Format Settings"
                  widgetVar="bhtIssueRequestPrintConfigDialog"
                  modal="true" width="500" resizable="false" closeOnEscape="true">
            <p:ajax event="open" listener="#{pharmacyConfigController.loadCurrentConfig}" update="bhtIssueRequestPrintConfigForm"/>
            <h:form id="bhtIssueRequestPrintConfigForm">
                <div class="mb-3">
                    <h:selectBooleanCheckbox id="cbBiqA4" value="#{pharmacyConfigController.bhtIssueRequestReceiptA4}"/>
                    <h:outputLabel for="cbBiqA4" value="A4 Paper" class="ms-2"/>
                </div>
                <div class="mb-3">
                    <h:selectBooleanCheckbox id="cbBiqFiveFive" value="#{pharmacyConfigController.bhtIssueRequestReceiptFiveFive}"/>
                    <h:outputLabel for="cbBiqFiveFive" value="5 x 5 Paper" class="ms-2"/>
                </div>
                <div class="mb-3">
                    <h:selectBooleanCheckbox id="cbBiqPos" value="#{pharmacyConfigController.bhtIssueRequestReceiptPos}"/>
                    <h:outputLabel for="cbBiqPos" value="POS Paper" class="ms-2"/>
                </div>
                <p:messages showDetail="true" closable="true"/>
                <div class="d-flex gap-2">
                    <p:commandButton value="Apply &amp; Close" icon="fas fa-save"
                                     styleClass="ui-button-success" ajax="false"
                                     action="#{pharmacyConfigController.saveBhtIssueRequestReceiptConfig}"/>
                    <p:commandButton value="Cancel" icon="fas fa-times" styleClass="ui-button-secondary"
                                     onclick="PF('bhtIssueRequestPrintConfigDialog').hide(); return false;" type="button"/>
                </div>
            </h:form>
        </p:dialog>

    </cc:implementation>
</html>
```

- [ ] **Step 2: Manual check + commit both composites**

Confirm well-formed XML the same way as Task 3, then commit both new composite files together:

```bash
git add src/main/webapp/resources/pharmacy/print/bht_issue_request_receipt.xhtml src/main/webapp/resources/pharmacy/print/bht_issue_request_print_config_button.xhtml
git commit -m "feat(pharmacy): add BHT issue request receipt and print-config composites"
```

---

### Task 5: PharmacyConfigController — new format keys

**Files:**
- Modify: `src/main/java/com/divudi/bean/pharmacy/PharmacyConfigController.java`

**Interfaces:**
- Produces: `pharmacyConfigController.bhtIssueRequestReceiptA4/FiveFive/Pos` (boolean bean properties, get/set), `pharmacyConfigController.saveBhtIssueRequestReceiptConfig()`. Reuses existing `loadCurrentConfig()` (extended) and existing `configOptionApplicationController`.
- Config keys (application-wide, `getBooleanValueByKey`/`setBooleanValueByKey`): `"Pharmacy BHT Issue Request Receipt is A4"` (default `false`), `"Pharmacy BHT Issue Request Receipt is FiveFive"` (default `true`), `"Pharmacy BHT Issue Request Receipt is POS"` (default `false`).

- [ ] **Step 1: Add the 3 fields**

In `src/main/java/com/divudi/bean/pharmacy/PharmacyConfigController.java`, find the existing block:

```java
    // Inward Direct Issue Bill — Service Charge/Discount print formats (Issue #22035)
    private boolean inwardDirectIssueBillA4;
    private boolean inwardDirectIssueBillFiveFive;
    private boolean inwardDirectIssueBillPos;
```

Add immediately after it:

```java

    // BHT Pharmacy Issue Request Receipt print formats
    private boolean bhtIssueRequestReceiptA4;
    private boolean bhtIssueRequestReceiptFiveFive;
    private boolean bhtIssueRequestReceiptPos;
```

- [ ] **Step 2: Add the load lines**

Find the existing block inside `loadCurrentConfig()`:

```java
        // Inward Direct Issue Bill print formats (Issue #22035) — application-wide
        inwardDirectIssueBillA4 = configOptionApplicationController.getBooleanValueByKey("Pharmacy Inward Direct Issue Bill is A4", false);
        inwardDirectIssueBillFiveFive = configOptionApplicationController.getBooleanValueByKey("Pharmacy Inward Direct Issue Bill is FiveFive", true);
        inwardDirectIssueBillPos = configOptionApplicationController.getBooleanValueByKey("Pharmacy Inward Direct Issue Bill is POS", false);
```

Add immediately after it:

```java

        // BHT Pharmacy Issue Request Receipt print formats — application-wide
        bhtIssueRequestReceiptA4 = configOptionApplicationController.getBooleanValueByKey("Pharmacy BHT Issue Request Receipt is A4", false);
        bhtIssueRequestReceiptFiveFive = configOptionApplicationController.getBooleanValueByKey("Pharmacy BHT Issue Request Receipt is FiveFive", true);
        bhtIssueRequestReceiptPos = configOptionApplicationController.getBooleanValueByKey("Pharmacy BHT Issue Request Receipt is POS", false);
```

- [ ] **Step 3: Add the save method**

Find the existing method:

```java
    /**
     * Save Inward Direct Issue Bill print format configuration changes specifically
     */
    public void saveInwardDirectIssueBillPaperConfig() {
        try {
            configOptionApplicationController.setBooleanValueByKey("Pharmacy Inward Direct Issue Bill is A4", inwardDirectIssueBillA4);
            configOptionApplicationController.setBooleanValueByKey("Pharmacy Inward Direct Issue Bill is FiveFive", inwardDirectIssueBillFiveFive);
            configOptionApplicationController.setBooleanValueByKey("Pharmacy Inward Direct Issue Bill is POS", inwardDirectIssueBillPos);
            JsfUtil.addSuccessMessage("Inward Direct Issue Bill print format settings saved");
            loadCurrentConfig();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving print format settings: " + e.getMessage());
        }
    }
```

Add immediately after it:

```java

    /**
     * Save BHT Pharmacy Issue Request Receipt print format configuration changes
     */
    public void saveBhtIssueRequestReceiptConfig() {
        try {
            configOptionApplicationController.setBooleanValueByKey("Pharmacy BHT Issue Request Receipt is A4", bhtIssueRequestReceiptA4);
            configOptionApplicationController.setBooleanValueByKey("Pharmacy BHT Issue Request Receipt is FiveFive", bhtIssueRequestReceiptFiveFive);
            configOptionApplicationController.setBooleanValueByKey("Pharmacy BHT Issue Request Receipt is POS", bhtIssueRequestReceiptPos);
            JsfUtil.addSuccessMessage("BHT Issue Request Receipt print format settings saved");
            loadCurrentConfig();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving print format settings: " + e.getMessage());
        }
    }
```

- [ ] **Step 4: Add getters/setters**

Find the existing tail block:

```java
    // Inward Direct Issue Bill Getters and Setters
    public boolean isInwardDirectIssueBillA4() {
        return inwardDirectIssueBillA4;
    }

    public void setInwardDirectIssueBillA4(boolean inwardDirectIssueBillA4) {
        this.inwardDirectIssueBillA4 = inwardDirectIssueBillA4;
    }

    public boolean isInwardDirectIssueBillFiveFive() {
        return inwardDirectIssueBillFiveFive;
    }

    public void setInwardDirectIssueBillFiveFive(boolean inwardDirectIssueBillFiveFive) {
        this.inwardDirectIssueBillFiveFive = inwardDirectIssueBillFiveFive;
    }

    public boolean isInwardDirectIssueBillPos() {
        return inwardDirectIssueBillPos;
    }

    public void setInwardDirectIssueBillPos(boolean inwardDirectIssueBillPos) {
        this.inwardDirectIssueBillPos = inwardDirectIssueBillPos;
    }

}
```

Replace with (adds the new getters/setters before the closing brace):

```java
    // Inward Direct Issue Bill Getters and Setters
    public boolean isInwardDirectIssueBillA4() {
        return inwardDirectIssueBillA4;
    }

    public void setInwardDirectIssueBillA4(boolean inwardDirectIssueBillA4) {
        this.inwardDirectIssueBillA4 = inwardDirectIssueBillA4;
    }

    public boolean isInwardDirectIssueBillFiveFive() {
        return inwardDirectIssueBillFiveFive;
    }

    public void setInwardDirectIssueBillFiveFive(boolean inwardDirectIssueBillFiveFive) {
        this.inwardDirectIssueBillFiveFive = inwardDirectIssueBillFiveFive;
    }

    public boolean isInwardDirectIssueBillPos() {
        return inwardDirectIssueBillPos;
    }

    public void setInwardDirectIssueBillPos(boolean inwardDirectIssueBillPos) {
        this.inwardDirectIssueBillPos = inwardDirectIssueBillPos;
    }

    // BHT Pharmacy Issue Request Receipt Getters and Setters
    public boolean isBhtIssueRequestReceiptA4() {
        return bhtIssueRequestReceiptA4;
    }

    public void setBhtIssueRequestReceiptA4(boolean bhtIssueRequestReceiptA4) {
        this.bhtIssueRequestReceiptA4 = bhtIssueRequestReceiptA4;
    }

    public boolean isBhtIssueRequestReceiptFiveFive() {
        return bhtIssueRequestReceiptFiveFive;
    }

    public void setBhtIssueRequestReceiptFiveFive(boolean bhtIssueRequestReceiptFiveFive) {
        this.bhtIssueRequestReceiptFiveFive = bhtIssueRequestReceiptFiveFive;
    }

    public boolean isBhtIssueRequestReceiptPos() {
        return bhtIssueRequestReceiptPos;
    }

    public void setBhtIssueRequestReceiptPos(boolean bhtIssueRequestReceiptPos) {
        this.bhtIssueRequestReceiptPos = bhtIssueRequestReceiptPos;
    }

}
```

- [ ] **Step 5: Compile check**

Run: `./detect-maven.sh compile`
Expected: BUILD SUCCESS, no compilation errors in `PharmacyConfigController.java`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/divudi/bean/pharmacy/PharmacyConfigController.java
git commit -m "feat(pharmacy): add BHT issue request receipt print-format config keys"
```

---

### Task 6: Wire the reprint page (`PharmacyBillSearch`)

**Files:**
- Modify: `src/main/java/com/divudi/bean/pharmacy/PharmacyBillSearch.java`
- Modify: `src/main/webapp/ward/ward_pharmacy_reprint_bht_issue_request.xhtml`

**Interfaces:**
- Consumes: `BhtIssueRequestNativeSqlService.loadPrintDtoByBillId(long)` (Task 2), composites from Tasks 3–4.
- Produces: `pharmacyBillSearch.getBhtIssueRequestPrintDto()` returning `BhtIssueRequestPrintDto` or `null`.

**Why a lazy cached getter, not a hook into a single "load" method**: `PharmacyBillSearch.bill` is set from 4 different list pages via `f:setPropertyActionListener` with a plain navigation string (no bean method call in between), and `setBill()` itself is a general-purpose setter used by many unrelated bill types across this large controller — populating the DTO unconditionally inside it would run an extra native query on every unrelated `pharmacyBillSearch.bill` assignment app-wide. A getter that loads once per distinct bill id, cached until the id changes, avoids that blast radius entirely.

- [ ] **Step 1: Add the injected service + cached getter**

In `src/main/java/com/divudi/bean/pharmacy/PharmacyBillSearch.java`, find:

```java
    public Bill getBill() {
        //recreateModel();
        return bill;
    }

    public void setBill(Bill bb) {
        recreateModel();
        if (bb == null) {
            bb = this.bill;
        }
        this.bill = bb;
//        if (bb.getPaymentMethod() != null) {
//            paymentMethod = bb.getPaymentMethod();
//        }

    }
```

Add immediately after it:

```java

    @EJB
    private com.divudi.service.pharmacy.BhtIssueRequestNativeSqlService bhtIssueRequestNativeSqlService;

    private com.divudi.core.data.dto.pharmacy.BhtIssueRequestPrintDto bhtIssueRequestPrintDto;
    private Long bhtIssueRequestPrintDtoBillId;

    public com.divudi.core.data.dto.pharmacy.BhtIssueRequestPrintDto getBhtIssueRequestPrintDto() {
        if (bill == null || bill.getId() == null) {
            return null;
        }
        if (bhtIssueRequestPrintDto == null || !bill.getId().equals(bhtIssueRequestPrintDtoBillId)) {
            bhtIssueRequestPrintDto = bhtIssueRequestNativeSqlService.loadPrintDtoByBillId(bill.getId());
            bhtIssueRequestPrintDtoBillId = bill.getId();
            if (bhtIssueRequestPrintDto == null) {
                JsfUtil.addErrorMessage("BHT Issue Request not found");
            }
        }
        return bhtIssueRequestPrintDto;
    }
```

(`@EJB` and `JsfUtil` are already imported in this file — confirmed at lines 84 and 58 respectively — no new imports needed since fully-qualified names are used inline for the new DTO/service types.)

- [ ] **Step 2: Update the reprint page**

In `src/main/webapp/ward/ward_pharmacy_reprint_bht_issue_request.xhtml`, add the new namespace to the root `<ui:composition>` tag. Find:

```xml
<ui:composition xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
                template="/resources/template/template.xhtml"
                xmlns:h="http://xmlns.jcp.org/jsf/html"
                xmlns:p="http://primefaces.org/ui"
                xmlns:f="http://xmlns.jcp.org/jsf/core"
                xmlns:ph="http://xmlns.jcp.org/jsf/composite/pharmacy"
                xmlns:phe="http://xmlns.jcp.org/jsf/composite/pharmacy/inward">
```

Replace with:

```xml
<ui:composition xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
                template="/resources/template/template.xhtml"
                xmlns:h="http://xmlns.jcp.org/jsf/html"
                xmlns:p="http://primefaces.org/ui"
                xmlns:f="http://xmlns.jcp.org/jsf/core"
                xmlns:ph="http://xmlns.jcp.org/jsf/composite/pharmacy"
                xmlns:phe="http://xmlns.jcp.org/jsf/composite/pharmacy/inward"
                xmlns:phprint="http://xmlns.jcp.org/jsf/composite/pharmacy/print">
```

Then find the Reprint button + preview block:

```xml
                            <p:commandButton
                                id="btnReprint"
                                title="Reprint this request"
                                styleClass="ui-button-success"
                                style="min-width:120px"
                                icon="fas fa-print"
                                value="Reprint"
                                ajax="false" >
                                <p:printer target="gpBillPreview" ></p:printer>
                            </p:commandButton>
                        </div>
```

Replace with (adds the gear button next to Reprint):

```xml
                            <p:commandButton
                                id="btnReprint"
                                title="Reprint this request"
                                styleClass="ui-button-success"
                                style="min-width:120px"
                                icon="fas fa-print"
                                value="Reprint"
                                ajax="false" >
                                <p:printer target="gpBillPreview" ></p:printer>
                            </p:commandButton>

                            <phprint:bht_issue_request_print_config_button/>
                        </div>
```

Then find the preview composite:

```xml
                                <h:panelGroup id="gpBillPreview">
                                    <ph:pharmacy_bht_issue_request_receipt
                                        bill="#{pharmacyBillSearch.bill}"
                                        duplicate="true" />
                                </h:panelGroup>
```

Replace with:

```xml
                                <h:panelGroup id="gpBillPreview">
                                    <phprint:bht_issue_request_receipt
                                        dto="#{pharmacyBillSearch.bhtIssueRequestPrintDto}"
                                        duplicate="true" />
                                </h:panelGroup>
```

- [ ] **Step 3: Compile check**

Run: `./detect-maven.sh compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/divudi/bean/pharmacy/PharmacyBillSearch.java src/main/webapp/ward/ward_pharmacy_reprint_bht_issue_request.xhtml
git commit -m "feat(pharmacy): wire professional print into BHT issue request reprint page"
```

---

### Task 7: Wire the create/settle page (`PharmacyRequestForBhtController`)

**Files:**
- Modify: `src/main/java/com/divudi/bean/pharmacy/PharmacyRequestForBhtController.java`
- Modify: `src/main/webapp/ward/ward_pharmacy_bht_issue_request_bill.xhtml`

**Interfaces:**
- Consumes: `BhtIssueRequestNativeSqlService.loadPrintDtoByBillId(long)` (Task 2), composites from Tasks 3–4.
- Produces: `pharmacyRequestForBhtController.getBhtIssueRequestPrintDto()`.

**Why the same lazy-cached pattern here too**: `setPrintBill(...)` is called from at least 5 different settle paths in this controller (confirmed at lines 1057, 1130, 1196, 1227, 1265) — a single shared getter keyed off `printBill`'s current id is simpler and safer than hooking every call site.

- [ ] **Step 1: Add the injected service + cached getter**

In `src/main/java/com/divudi/bean/pharmacy/PharmacyRequestForBhtController.java`, find:

```java
    public Bill getPrintBill() {
        return printBill;
    }

    public void setPrintBill(Bill printBill) {
        this.printBill = printBill;
    }
```

Add immediately after it:

```java

    @EJB
    private com.divudi.service.pharmacy.BhtIssueRequestNativeSqlService bhtIssueRequestNativeSqlService;

    private com.divudi.core.data.dto.pharmacy.BhtIssueRequestPrintDto bhtIssueRequestPrintDto;
    private Long bhtIssueRequestPrintDtoBillId;

    public com.divudi.core.data.dto.pharmacy.BhtIssueRequestPrintDto getBhtIssueRequestPrintDto() {
        if (printBill == null || printBill.getId() == null) {
            return null;
        }
        if (bhtIssueRequestPrintDto == null || !printBill.getId().equals(bhtIssueRequestPrintDtoBillId)) {
            bhtIssueRequestPrintDto = bhtIssueRequestNativeSqlService.loadPrintDtoByBillId(printBill.getId());
            bhtIssueRequestPrintDtoBillId = printBill.getId();
            if (bhtIssueRequestPrintDto == null) {
                JsfUtil.addErrorMessage("BHT Issue Request not found");
            }
        }
        return bhtIssueRequestPrintDto;
    }
```

(`@EJB` and `JsfUtil` already imported — lines 63 and 15 respectively.)

- [ ] **Step 2: Update the create/settle page**

In `src/main/webapp/ward/ward_pharmacy_bht_issue_request_bill.xhtml`, find the root namespace declaration:

```xml
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:p="http://primefaces.org/ui"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:phi="http://xmlns.jcp.org/jsf/composite/pharmacy"
      xmlns:ph="http://xmlns.jcp.org/jsf/composite/pharmacy"
      xmlns:bill="http://xmlns.jcp.org/jsf/composite/inward">
```

Replace with:

```xml
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:p="http://primefaces.org/ui"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:phi="http://xmlns.jcp.org/jsf/composite/pharmacy"
      xmlns:ph="http://xmlns.jcp.org/jsf/composite/pharmacy"
      xmlns:bill="http://xmlns.jcp.org/jsf/composite/inward"
      xmlns:phprint="http://xmlns.jcp.org/jsf/composite/pharmacy/print">
```

Then find the Print button:

```xml
                                    <p:commandButton
                                        id="btnPrint"
                                        value="Print"
                                        ajax="false"
                                        title="Print this bill"
                                        styleClass="ui-button-info"
                                        icon="fa fa-print">
                                        <p:printer target="gpBillPreview" ></p:printer>
                                    </p:commandButton>

                                    <p:commandButton
                                        id="btnNewMedicineRequest"
```

Replace with (adds the gear button between Print and New Medicine Request):

```xml
                                    <p:commandButton
                                        id="btnPrint"
                                        value="Print"
                                        ajax="false"
                                        title="Print this bill"
                                        styleClass="ui-button-info"
                                        icon="fa fa-print">
                                        <p:printer target="gpBillPreview" ></p:printer>
                                    </p:commandButton>

                                    <phprint:bht_issue_request_print_config_button/>

                                    <p:commandButton
                                        id="btnNewMedicineRequest"
```

Then find the preview composite:

```xml
                        <h:panelGroup id="gpBillPreview">
                            <ph:pharmacy_bht_issue_request_receipt 
                                bill="#{pharmacyRequestForBhtController.printBill}"
                                comment="#{pharmacyRequestForBhtController.comment}" />
                        </h:panelGroup>
```

Replace with:

```xml
                        <h:panelGroup id="gpBillPreview">
                            <phprint:bht_issue_request_receipt
                                dto="#{pharmacyRequestForBhtController.bhtIssueRequestPrintDto}"
                                comment="#{pharmacyRequestForBhtController.comment}" />
                        </h:panelGroup>
```

- [ ] **Step 3: Compile check**

Run: `./detect-maven.sh compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/divudi/bean/pharmacy/PharmacyRequestForBhtController.java src/main/webapp/ward/ward_pharmacy_bht_issue_request_bill.xhtml
git commit -m "feat(pharmacy): wire professional print into BHT issue request create page"
```

---

### Task 8: Wire the pharmacist's issue page (`PharmacySaleBhtController`)

**Files:**
- Modify: `src/main/java/com/divudi/bean/pharmacy/PharmacySaleBhtController.java`
- Modify: `src/main/webapp/ward/ward_pharmacy_bht_issue.xhtml`

**Interfaces:**
- Consumes: `BhtIssueRequestNativeSqlService.loadPrintDtoByBillId(long)` (Task 2), composite from Task 3 (no config-button composite here — this page has no print action, it's a read-only reference dialog).
- Produces: `pharmacySaleBhtController.getBhtIssueRequestPrintDto()`.

- [ ] **Step 1: Add the injected service + cached getter**

In `src/main/java/com/divudi/bean/pharmacy/PharmacySaleBhtController.java`, find:

```java
/////////////////////////
    Item selectedAlternative;
    private PreBill preBill;
    Bill printBill;
    Bill bill;
    private Bill bhtRequestBill;
    BillItem billItem;
```

Replace with (adds the injected service + cached fields right after `bhtRequestBill`):

```java
/////////////////////////
    Item selectedAlternative;
    private PreBill preBill;
    Bill printBill;
    Bill bill;
    private Bill bhtRequestBill;

    @EJB
    private com.divudi.service.pharmacy.BhtIssueRequestNativeSqlService bhtIssueRequestNativeSqlService;

    private com.divudi.core.data.dto.pharmacy.BhtIssueRequestPrintDto bhtIssueRequestPrintDto;
    private Long bhtIssueRequestPrintDtoBillId;

    public com.divudi.core.data.dto.pharmacy.BhtIssueRequestPrintDto getBhtIssueRequestPrintDto() {
        if (bhtRequestBill == null || bhtRequestBill.getId() == null) {
            return null;
        }
        if (bhtIssueRequestPrintDto == null || !bhtRequestBill.getId().equals(bhtIssueRequestPrintDtoBillId)) {
            bhtIssueRequestPrintDto = bhtIssueRequestNativeSqlService.loadPrintDtoByBillId(bhtRequestBill.getId());
            bhtIssueRequestPrintDtoBillId = bhtRequestBill.getId();
            if (bhtIssueRequestPrintDto == null) {
                JsfUtil.addErrorMessage("BHT Issue Request not found");
            }
        }
        return bhtIssueRequestPrintDto;
    }

    BillItem billItem;
```

(`@EJB` and `JsfUtil` already imported — lines 84 and 17 respectively.)

- [ ] **Step 2: Update the issue page**

In `src/main/webapp/ward/ward_pharmacy_bht_issue.xhtml`, find the root namespace declaration:

```xml
<ui:composition xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
                template="/resources/template/template.xhtml"
                xmlns:h="http://xmlns.jcp.org/jsf/html"
                xmlns:p="http://primefaces.org/ui"
                xmlns:f="http://xmlns.jcp.org/jsf/core"
                xmlns:phe="http://xmlns.jcp.org/jsf/composite/pharmacy/inward"
                xmlns:ph="http://xmlns.jcp.org/jsf/composite/pharmacy"
                xmlns:phi="http://xmlns.jcp.org/jsf/composite/pharmacy">
```

Replace with:

```xml
<ui:composition xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
                template="/resources/template/template.xhtml"
                xmlns:h="http://xmlns.jcp.org/jsf/html"
                xmlns:p="http://primefaces.org/ui"
                xmlns:f="http://xmlns.jcp.org/jsf/core"
                xmlns:phe="http://xmlns.jcp.org/jsf/composite/pharmacy/inward"
                xmlns:ph="http://xmlns.jcp.org/jsf/composite/pharmacy"
                xmlns:phi="http://xmlns.jcp.org/jsf/composite/pharmacy"
                xmlns:phprint="http://xmlns.jcp.org/jsf/composite/pharmacy/print">
```

Then find the "Original Request" dialog:

```xml
            <p:dialog
                id="viewRequestDlg"
                widgetVar="viewRequestDialog"
                header="Original Request"
                modal="true"
                appendTo="@(body)"
                width="60%"
                fitViewport="true"
                position="top"
                rendered="#{pharmacySaleBhtController.bhtRequestBill ne null}">
                <ph:pharmacy_bht_issue_request_receipt
                    bill="#{pharmacySaleBhtController.bhtRequestBill}" />
            </p:dialog>
```

Replace with:

```xml
            <p:dialog
                id="viewRequestDlg"
                widgetVar="viewRequestDialog"
                header="Original Request"
                modal="true"
                appendTo="@(body)"
                width="60%"
                fitViewport="true"
                position="top"
                rendered="#{pharmacySaleBhtController.bhtRequestBill ne null}">
                <phprint:bht_issue_request_receipt
                    dto="#{pharmacySaleBhtController.bhtIssueRequestPrintDto}" />
            </p:dialog>
```

- [ ] **Step 3: Compile check**

Run: `./detect-maven.sh compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/divudi/bean/pharmacy/PharmacySaleBhtController.java src/main/webapp/ward/ward_pharmacy_bht_issue.xhtml
git commit -m "feat(pharmacy): wire professional print into BHT issue original-request view"
```

---

### Task 9: Delete the old entity-based composite

**Files:**
- Delete: `src/main/webapp/resources/pharmacy/pharmacy_bht_issue_request_receipt.xhtml`

**Interfaces:**
- Consumes: none (this task only removes dead code once Tasks 6–8 have replaced every usage).

- [ ] **Step 1: Verify no remaining usages**

Run: `grep -rn "pharmacy_bht_issue_request_receipt" src/main/webapp`
Expected: no output (all 3 previous usages were replaced in Tasks 6, 7, 8).

- [ ] **Step 2: Delete the file**

```bash
git rm src/main/webapp/resources/pharmacy/pharmacy_bht_issue_request_receipt.xhtml
```

- [ ] **Step 3: Commit**

```bash
git commit -m "chore(pharmacy): remove superseded BHT issue request receipt composite"
```

---

### Task 10: Build, redeploy, and end-to-end verification

**Files:** none (build/deploy/verification only).

- [ ] **Step 1: Full build**

Run: `./detect-maven.sh clean package -DskipTests`
Expected: BUILD SUCCESS, produces the WAR.

- [ ] **Step 2: Redeploy locally**

Use the `/run` skill (or the project's standard local Payara redeploy flow) to deploy the freshly built WAR to the local Payara domain.

- [ ] **Step 3: Playwright E2E pass**

Use the `/playwright-e2e` skill against the local deployment to walk through the spec's testing checklist:

1. Create a BHT pharmacy issue request, settle it, verify the print preview renders for each of A4/FiveFive/POS in turn (toggle via the new gear dialog on the create page), including the status badge and signature lines.
2. Open the reprint page for the same request and verify the DUPLICATE watermark appears and all 3 formats render consistently with the settle-page preview.
3. As a pharmacist, open the issue page for this request and confirm the "Original Request" dialog still displays correctly (no gear button, no watermark).
4. Cancel a request and reprint; confirm the status badge shows `CANCELLED`.
5. Mark a request as completed (existing Mark as Complete flow) and reprint; confirm the status badge shows `COMPLETED`.
6. Confirm the gear dialog's format toggles persist across a page reload.

Fix any visual/layout issues found directly in the composite CSS (Task 3's file) — this is expected, since print-CSS is inherently easier to tune against real rendered output than to get exactly right up front.

- [ ] **Step 4: Final verification commit (if CSS was adjusted)**

If Step 3 required CSS fixes:

```bash
git add src/main/webapp/resources/pharmacy/print/bht_issue_request_receipt.xhtml
git commit -m "fix(pharmacy): tune BHT issue request print CSS after E2E verification"
```

---

## Self-Review

**Spec coverage:**
- DTO pair — Task 1. ✅
- Native-SQL service — Task 2. ✅
- Single receipt composite, 3 internal format blocks — Task 3. ✅
- Config gear-button composite — Task 4. ✅
- `PharmacyConfigController` 3 new keys — Task 5. ✅
- All 3 call sites wired (reprint, create, pharmacist view) — Tasks 6, 7, 8. ✅
- Old composite deleted — Task 9. ✅
- Status badge, DUPLICATE watermark, signature lines — Task 3. ✅
- Manual E2E testing per spec's 6-point checklist — Task 10. ✅

**Placeholder scan:** No TBD/TODO markers; every step has complete code copied from verified real file contents or newly written in full.

**Type consistency:** `BhtIssueRequestPrintDto`/`BhtIssueRequestItemPrintDto` field names and types are identical across Tasks 1, 2, 3, 6, 7, 8. `getBhtIssueRequestPrintDto()` signature and null-handling are identical across the 3 controllers (Tasks 6, 7, 8), differing only in which bill field they key off (`bill`, `printBill`, `bhtRequestBill`). Config property names (`bhtIssueRequestReceiptA4/FiveFive/Pos`) match between Task 5 (Java) and Task 4 (EL in the composite).
