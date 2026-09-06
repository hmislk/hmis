# Client Portal Registration — Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the shared, testable foundation (data model, pure decision logic, config/OTP plumbing) that all four client-portal registration channels (staff-assisted, self-service phone-OTP, self-service email-OTP, kiosk) will build on in separate follow-up plans.

**Architecture:** One new JPA entity (`ClientAccount`, 1:1 with `Person`) with its own facade, plus three small pure-logic utility classes extracted so they're unit-testable without a running container: a phone/email match classifier (0/1/many existing patients), a kiosk IP-allowlist checker, and an OTP code generator. A new `MessageType` enum value and config keys keep this feature's OTP traffic distinct from the existing channel-booking-payment OTP flow it's modeled on.

**Tech Stack:** Java EE 8 (JPA/EclipseLink, CDI/EJB), JUnit 5 (no Mockito in this project — pure-logic classes only are unit tested; DB/JSF-dependent code is verified later via Playwright per the project's `playwright-e2e` skill, not in this plan).

## Global Constraints

- JPQL only for all queries — no native SQL (`nativeScalarQuery`/`executeNativeSql` are last resorts only; not needed here).
- Never modify existing constructors on `WebUser`, `Sms`, `Patient`, etc. — this plan only *adds* a new entity and new standalone utility classes; it does not touch existing entity constructors.
- Soft-delete convention: follow the existing `retired`/`retirer`/`retiredAt`/`retireComments` pattern (seen on `Sms`) instead of inventing a separate "enabled" flag.
- This is a foundation-only plan — **no XHTML pages, no controllers, no login wiring** in this plan. Those are built in follow-up, channel-specific plans that depend on this one.
- After this plan's entity is merged, the `generate-ddl` skill must be run (Task 8) since it adds a new table.

---

### Task 1: `ClientAccountCreationChannel` enum

**Files:**
- Create: `src/main/java/com/divudi/core/data/ClientAccountCreationChannel.java`
- Test: `src/test/java/com/divudi/core/data/ClientAccountCreationChannelTest.java`

**Interfaces:**
- Produces: enum `ClientAccountCreationChannel` with constants `SELF_PHONE`, `SELF_EMAIL`, `STAFF_ASSISTED`, `KIOSK` — consumed by `ClientAccount.createdVia` (Task 2) and by every channel-specific controller in later plans.

- [ ] **Step 1: Write the failing test**

```java
package com.divudi.core.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClientAccountCreationChannelTest {

    @Test
    public void testAllFourChannelsExist() {
        ClientAccountCreationChannel[] values = ClientAccountCreationChannel.values();
        assertEquals(4, values.length);
        assertNotNull(ClientAccountCreationChannel.valueOf("SELF_PHONE"));
        assertNotNull(ClientAccountCreationChannel.valueOf("SELF_EMAIL"));
        assertNotNull(ClientAccountCreationChannel.valueOf("STAFF_ASSISTED"));
        assertNotNull(ClientAccountCreationChannel.valueOf("KIOSK"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ClientAccountCreationChannelTest test`
Expected: FAIL — compilation error, `ClientAccountCreationChannel` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.divudi.core.data;

public enum ClientAccountCreationChannel {
    SELF_PHONE,
    SELF_EMAIL,
    STAFF_ASSISTED,
    KIOSK
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ClientAccountCreationChannelTest test`
Expected: PASS — 1 test run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/core/data/ClientAccountCreationChannel.java src/test/java/com/divudi/core/data/ClientAccountCreationChannelTest.java
git commit -m "feat(client-portal): add ClientAccountCreationChannel enum"
```

---

### Task 2: `MessageType.ClientPortalRegistrationOTP` enum value

**Files:**
- Modify: `src/main/java/com/divudi/core/data/MessageType.java`
- Test: `src/test/java/com/divudi/core/data/MessageTypeClientPortalTest.java`

**Interfaces:**
- Produces: `MessageType.ClientPortalRegistrationOTP` — consumed by the OTP-sending code in the phone/email self-service and kiosk channel plans (kept distinct from the existing `MessageType.PatientPortalOTP` used by channel-booking payments, per the design spec's reuse-but-don't-collide note).

- [ ] **Step 1: Write the failing test**

```java
package com.divudi.core.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MessageTypeClientPortalTest {

    @Test
    public void testClientPortalRegistrationOtpValueExists() {
        assertNotNull(MessageType.valueOf("ClientPortalRegistrationOTP"));
    }

    @Test
    public void testDistinctFromExistingPatientPortalOtp() {
        assertNotEquals(MessageType.PatientPortalOTP, MessageType.ClientPortalRegistrationOTP);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=MessageTypeClientPortalTest test`
Expected: FAIL — compilation error, no constant `ClientPortalRegistrationOTP` in `MessageType`.

- [ ] **Step 3: Write minimal implementation**

Open `src/main/java/com/divudi/core/data/MessageType.java`. Find the existing enum body (verified at lines 11-44):

```java
public enum MessageType {
    LabReport, OpdBillSettle, Marketing, BillCancellationInformationMail, BillReturnInfromationMail,
    ChannelBooking, ChannelDoctorArrival, ChannelCancellation, ChannelCompletion, ChannelNoShow,
    ChannelDoctorPayment, ChannelCustom, ChannelPatientFeedback, ChannelPatientReschedule, ChannelReminder,
    ChannelBookingCancellation, ChannelTimeDateChange, ChannelDoctorReminder, ChannelStatusUpdate,
    DoctorPayment, BulkPatientSms, BulkNumberSms, ConfirmationEmail, CustomSMS,
    @Deprecated OTP,
    PatientPortalOTP, PatientPortal_Link, InpatientDocumentUpload, InpatientFilledForm, InpatientClinicalDocument
}
```

Add the new constant at the end, before the closing brace:

```java
public enum MessageType {
    LabReport, OpdBillSettle, Marketing, BillCancellationInformationMail, BillReturnInfromationMail,
    ChannelBooking, ChannelDoctorArrival, ChannelCancellation, ChannelCompletion, ChannelNoShow,
    ChannelDoctorPayment, ChannelCustom, ChannelPatientFeedback, ChannelPatientReschedule, ChannelReminder,
    ChannelBookingCancellation, ChannelTimeDateChange, ChannelDoctorReminder, ChannelStatusUpdate,
    DoctorPayment, BulkPatientSms, BulkNumberSms, ConfirmationEmail, CustomSMS,
    @Deprecated OTP,
    PatientPortalOTP, PatientPortal_Link, InpatientDocumentUpload, InpatientFilledForm, InpatientClinicalDocument,
    ClientPortalRegistrationOTP
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=MessageTypeClientPortalTest test`
Expected: PASS — 2 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/core/data/MessageType.java src/test/java/com/divudi/core/data/MessageTypeClientPortalTest.java
git commit -m "feat(client-portal): add ClientPortalRegistrationOTP message type"
```

---

### Task 3: `ClientAccount` entity

**Files:**
- Create: `src/main/java/com/divudi/core/entity/ClientAccount.java`
- Test: `src/test/java/com/divudi/core/entity/ClientAccountTest.java`

**Interfaces:**
- Consumes: `com.divudi.core.entity.Person`, `com.divudi.core.entity.WebUser`, `com.divudi.core.data.ClientAccountCreationChannel` (Task 1).
- Produces: `ClientAccount` with fields `id`, `person` (`Person`), `passwordHash` (`String`), `verifiedPhone` (`String`), `verifiedEmail` (`String`), `phoneVerified`/`emailVerified` (`boolean`), `createdVia` (`ClientAccountCreationChannel`), `createdByWebUser` (`WebUser`, nullable), `createdAt` (`Date`), `retired`/`retirer`/`retiredAt`/`retireComments` — consumed by `ClientAccountFacade` (Task 4) and every channel plan.

- [ ] **Step 1: Write the failing test**

```java
package com.divudi.core.entity;

import com.divudi.core.data.ClientAccountCreationChannel;
import java.util.Date;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClientAccountTest {

    @Test
    public void testGettersAndSetters() {
        ClientAccount account = new ClientAccount();
        Person person = new Person();
        person.setName("Test Client");
        Date now = new Date();

        account.setPerson(person);
        account.setPasswordHash("hashed-value");
        account.setVerifiedPhone("0771234567");
        account.setVerifiedEmail("client@example.com");
        account.setPhoneVerified(true);
        account.setEmailVerified(false);
        account.setCreatedVia(ClientAccountCreationChannel.SELF_PHONE);
        account.setCreatedAt(now);
        account.setRetired(false);

        assertEquals(person, account.getPerson());
        assertEquals("hashed-value", account.getPasswordHash());
        assertEquals("0771234567", account.getVerifiedPhone());
        assertEquals("client@example.com", account.getVerifiedEmail());
        assertTrue(account.isPhoneVerified());
        assertFalse(account.isEmailVerified());
        assertEquals(ClientAccountCreationChannel.SELF_PHONE, account.getCreatedVia());
        assertEquals(now, account.getCreatedAt());
        assertFalse(account.isRetired());
    }

    @Test
    public void testDefaultRetiredIsFalse() {
        ClientAccount account = new ClientAccount();
        assertFalse(account.isRetired());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ClientAccountTest test`
Expected: FAIL — compilation error, `ClientAccount` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.divudi.core.entity;

import com.divudi.core.data.ClientAccountCreationChannel;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class ClientAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne (not @OneToOne): person_id must stay non-unique at the DB level so
    // a retired account and a newly created active account can coexist for the same
    // Person. ClientAccountFacade.findByPerson only looks up an existing account —
    // it does not by itself enforce "one active account per person" under
    // concurrent registrations. That requires locking the Person row (or an
    // equivalent conflict-handling strategy) for the duration of the
    // check-then-create, see the design spec's concurrency note.
    @ManyToOne
    private Person person;

    private String passwordHash;

    private String verifiedPhone;
    private String verifiedEmail;

    private boolean phoneVerified;
    private boolean emailVerified;

    @Enumerated(EnumType.STRING)
    private ClientAccountCreationChannel createdVia;

    @ManyToOne
    private WebUser createdByWebUser;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    private boolean retired;

    @ManyToOne
    private WebUser retirer;

    @Temporal(TemporalType.TIMESTAMP)
    private Date retiredAt;

    private String retireComments;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getVerifiedPhone() {
        return verifiedPhone;
    }

    public void setVerifiedPhone(String verifiedPhone) {
        this.verifiedPhone = verifiedPhone;
    }

    public String getVerifiedEmail() {
        return verifiedEmail;
    }

    public void setVerifiedEmail(String verifiedEmail) {
        this.verifiedEmail = verifiedEmail;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public void setPhoneVerified(boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public ClientAccountCreationChannel getCreatedVia() {
        return createdVia;
    }

    public void setCreatedVia(ClientAccountCreationChannel createdVia) {
        this.createdVia = createdVia;
    }

    public WebUser getCreatedByWebUser() {
        return createdByWebUser;
    }

    public void setCreatedByWebUser(WebUser createdByWebUser) {
        this.createdByWebUser = createdByWebUser;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public WebUser getRetirer() {
        return retirer;
    }

    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ClientAccountTest test`
Expected: PASS — 2 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/core/entity/ClientAccount.java src/test/java/com/divudi/core/entity/ClientAccountTest.java
git commit -m "feat(client-portal): add ClientAccount entity"
```

---

### Task 4: `ClientAccountFacade` with `findByPerson`

**Files:**
- Create: `src/main/java/com/divudi/core/facade/ClientAccountFacade.java`

**Interfaces:**
- Consumes: `ClientAccount` (Task 3), `AbstractFacade<T>` (`src/main/java/com/divudi/core/facade/AbstractFacade.java`, constructor `AbstractFacade(Class<T> entityClass)`, method `findByJpql(String jpql, Map<String, Object> parameters)`).
- Produces: `ClientAccountFacade.findByPerson(Long personId)` returning `ClientAccount` or `null` — consumed by every channel plan's duplicate-account check (design spec §3: "duplicate-account detection happens after a specific person is selected").

This task has no dedicated unit test: `AbstractFacade` requires a live `EntityManager`/persistence context, which this project's test suite does not stand up (no Mockito, no embedded container — confirmed by inspecting `pom.xml` and existing tests, which only unit-test pure logic). Facade correctness is verified via Playwright/DB checks once a channel plan wires this into a real registration flow, matching how the rest of this codebase verifies facade-level code. This mirrors the exact shape of `src/main/java/com/divudi/core/facade/SmsFacade.java`.

- [ ] **Step 1: Write the facade**

```java
package com.divudi.core.facade;

import com.divudi.core.entity.ClientAccount;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class ClientAccountFacade extends AbstractFacade<ClientAccount> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ClientAccountFacade() {
        super(ClientAccount.class);
    }

    public ClientAccount findByPerson(Long personId) {
        String jpql = "select c from ClientAccount c where c.retired=false and c.person.id=:personId";
        Map<String, Object> params = new HashMap<>();
        params.put("personId", personId);
        List<ClientAccount> results = findByJpql(jpql, params);
        if (results == null || results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }
}
```

- [ ] **Step 2: Compile to verify no errors**

Run: `mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/divudi/core/facade/ClientAccountFacade.java
git commit -m "feat(client-portal): add ClientAccountFacade with findByPerson lookup"
```

---

### Task 5: `ClientPortalMatcher` — pure 0/1/many match classifier

**Files:**
- Create: `src/main/java/com/divudi/core/util/ClientPortalMatcher.java`
- Test: `src/test/java/com/divudi/core/util/ClientPortalMatcherTest.java`

**Interfaces:**
- Consumes: `com.divudi.core.entity.Patient`.
- Produces: `enum ClientPortalMatcher.MatchResult { NO_MATCH, SINGLE_MATCH, MULTIPLE_MATCH }` and `static ClientPortalMatcher.MatchResult classify(List<Patient> matches)` — this is the extracted, unit-testable version of the 0/1/many decision embedded today in `PatientPortalController.findPatients()` (`src/main/java/com/divudi/bean/channel/PatientPortalController.java:543-576`). Every self-service/kiosk channel plan calls this after querying `Patient` by phone/email instead of re-implementing the branching.

- [ ] **Step 1: Write the failing test**

```java
package com.divudi.core.util;

import com.divudi.core.entity.Patient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClientPortalMatcherTest {

    @Test
    public void testNullListIsNoMatch() {
        assertEquals(ClientPortalMatcher.MatchResult.NO_MATCH, ClientPortalMatcher.classify(null));
    }

    @Test
    public void testEmptyListIsNoMatch() {
        List<Patient> matches = new ArrayList<>();
        assertEquals(ClientPortalMatcher.MatchResult.NO_MATCH, ClientPortalMatcher.classify(matches));
    }

    @Test
    public void testSingleEntryIsSingleMatch() {
        List<Patient> matches = Arrays.asList(new Patient());
        assertEquals(ClientPortalMatcher.MatchResult.SINGLE_MATCH, ClientPortalMatcher.classify(matches));
    }

    @Test
    public void testMultipleEntriesIsMultipleMatch() {
        List<Patient> matches = Arrays.asList(new Patient(), new Patient(), new Patient());
        assertEquals(ClientPortalMatcher.MatchResult.MULTIPLE_MATCH, ClientPortalMatcher.classify(matches));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ClientPortalMatcherTest test`
Expected: FAIL — compilation error, `ClientPortalMatcher` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.divudi.core.util;

import com.divudi.core.entity.Patient;
import java.util.List;

public class ClientPortalMatcher {

    public enum MatchResult {
        NO_MATCH,
        SINGLE_MATCH,
        MULTIPLE_MATCH
    }

    private ClientPortalMatcher() {
    }

    public static MatchResult classify(List<Patient> matches) {
        if (matches == null || matches.isEmpty()) {
            return MatchResult.NO_MATCH;
        }
        if (matches.size() == 1) {
            return MatchResult.SINGLE_MATCH;
        }
        return MatchResult.MULTIPLE_MATCH;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ClientPortalMatcherTest test`
Expected: PASS — 4 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/core/util/ClientPortalMatcher.java src/test/java/com/divudi/core/util/ClientPortalMatcherTest.java
git commit -m "feat(client-portal): extract pure 0/1/many patient-match classifier"
```

---

### Task 6: `ClientPortalIpAllowlist` — pure kiosk IP-allowlist checker

**Files:**
- Create: `src/main/java/com/divudi/core/util/ClientPortalIpAllowlist.java`
- Test: `src/test/java/com/divudi/core/util/ClientPortalIpAllowlistTest.java`

**Interfaces:**
- Produces: `static boolean ClientPortalIpAllowlist.isAllowed(String requestIp, String allowedIpsCsv)` — mirrors the exact semantics of `WebUser.isIpAllowed(String requestIp)` (`src/main/java/com/divudi/core/entity/WebUser.java:420-433`) but operates on a config-supplied CSV string instead of a per-`WebUser` field, since no account exists yet when a client is standing at the kiosk (design spec §5). Consumed by the kiosk channel plan, reading the CSV from `ConfigOptionApplicationController.getLongTextValueByKey("Client Portal - Kiosk Allowed IPs")`.

- [ ] **Step 1: Write the failing test**

```java
package com.divudi.core.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClientPortalIpAllowlistTest {

    @Test
    public void testExactMatchIsAllowed() {
        assertTrue(ClientPortalIpAllowlist.isAllowed("192.168.1.10", "192.168.1.10"));
    }

    @Test
    public void testMatchAmongMultipleCsvEntries() {
        assertTrue(ClientPortalIpAllowlist.isAllowed("192.168.1.11", "192.168.1.10, 192.168.1.11, 192.168.1.12"));
    }

    @Test
    public void testWhitespaceAroundCsvEntriesIsTrimmed() {
        assertTrue(ClientPortalIpAllowlist.isAllowed("10.0.0.5", "  10.0.0.5  ,10.0.0.6"));
    }

    @Test
    public void testNoMatchIsRejected() {
        assertFalse(ClientPortalIpAllowlist.isAllowed("10.0.0.99", "10.0.0.5,10.0.0.6"));
    }

    @Test
    public void testNullRequestIpIsRejected() {
        assertFalse(ClientPortalIpAllowlist.isAllowed(null, "10.0.0.5"));
    }

    @Test
    public void testBlankRequestIpIsRejected() {
        assertFalse(ClientPortalIpAllowlist.isAllowed("", "10.0.0.5"));
        assertFalse(ClientPortalIpAllowlist.isAllowed("   ", "10.0.0.5"));
    }

    @Test
    public void testBlankRequestIpDoesNotMatchLeadingEmptyCsvEntry() {
        assertFalse(ClientPortalIpAllowlist.isAllowed("", ",10.0.0.5"));
    }

    @Test
    public void testNullOrEmptyAllowlistIsRejected() {
        assertFalse(ClientPortalIpAllowlist.isAllowed("10.0.0.5", null));
        assertFalse(ClientPortalIpAllowlist.isAllowed("10.0.0.5", ""));
        assertFalse(ClientPortalIpAllowlist.isAllowed("10.0.0.5", "   "));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ClientPortalIpAllowlistTest test`
Expected: FAIL — compilation error, `ClientPortalIpAllowlist` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.divudi.core.util;

public class ClientPortalIpAllowlist {

    private ClientPortalIpAllowlist() {
    }

    public static boolean isAllowed(String requestIp, String allowedIpsCsv) {
        if (requestIp == null || requestIp.trim().isEmpty()
                || allowedIpsCsv == null || allowedIpsCsv.trim().isEmpty()) {
            return false;
        }
        for (String allowed : allowedIpsCsv.split(",")) {
            if (allowed.trim().equalsIgnoreCase(requestIp.trim())) {
                return true;
            }
        }
        return false;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ClientPortalIpAllowlistTest test`
Expected: PASS — 8 tests run, 0 failures.

> **Amended after PR review:** the original version only rejected a `null`
> `requestIp`. A blank (`""`) `requestIp` combined with a malformed allowlist
> containing a leading/stray comma (e.g. `",10.0.0.5"`, whose split retains a
> leading empty entry) would have matched. Both the implementation and this
> test file were updated to reject blank `requestIp` too (PR
> [#22358](https://github.com/hmislk/hmis/pull/22358) review comment).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/core/util/ClientPortalIpAllowlist.java src/test/java/com/divudi/core/util/ClientPortalIpAllowlistTest.java
git commit -m "feat(client-portal): add pure kiosk IP-allowlist checker"
```

---

### Task 7: `ClientPortalOtpGenerator` — pure OTP code generator

**Files:**
- Create: `src/main/java/com/divudi/core/util/ClientPortalOtpGenerator.java`
- Test: `src/test/java/com/divudi/core/util/ClientPortalOtpGeneratorTest.java`

**Interfaces:**
- Produces: `static String ClientPortalOtpGenerator.generate(int length)` — a configurable-length, all-numeric OTP generator, extracted from the pattern in `PatientPortalController.otpCodeConverter()` (`src/main/java/com/divudi/bean/channel/PatientPortalController.java:399-410`) so it is unit-testable and reusable across the phone-OTP, email-OTP, and kiosk channel plans without duplicating the loop. Callers read the length from `ConfigOptionApplicationController.getLongValueByKey("Client Portal - OTP Length", 6L)` and pass it in — this class does not read config itself, keeping it a pure function.

- [ ] **Step 1: Write the failing test**

```java
package com.divudi.core.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClientPortalOtpGeneratorTest {

    @Test
    public void testGeneratesRequestedLength() {
        String otp = ClientPortalOtpGenerator.generate(6);
        assertEquals(6, otp.length());
    }

    @Test
    public void testGeneratesOnlyDigits() {
        String otp = ClientPortalOtpGenerator.generate(8);
        assertTrue(otp.matches("[0-9]+"));
    }

    @Test
    public void testDifferentLengthProducesDifferentSize() {
        String otp = ClientPortalOtpGenerator.generate(4);
        assertEquals(4, otp.length());
    }

    @Test
    public void testZeroLengthThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ClientPortalOtpGenerator.generate(0));
    }

    @Test
    public void testNegativeLengthThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ClientPortalOtpGenerator.generate(-1));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ClientPortalOtpGeneratorTest test`
Expected: FAIL — compilation error, `ClientPortalOtpGenerator` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.divudi.core.util;

import java.security.SecureRandom;

public class ClientPortalOtpGenerator {

    private static final String DIGITS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private ClientPortalOtpGenerator() {
    }

    public static String generate(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("OTP length must be positive");
        }
        StringBuilder otpBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(DIGITS.length());
            otpBuilder.append(DIGITS.charAt(index));
        }
        return otpBuilder.toString();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ClientPortalOtpGeneratorTest test`
Expected: PASS — 5 tests run, 0 failures.

> **Amended after PR review:** the original version silently returned an
> empty string for `length <= 0`, meaning a misconfigured OTP length could
> produce a "secret" with no actual entropy. `generate` now throws
> `IllegalArgumentException` for non-positive lengths; the zero-length test
> was changed from asserting an empty string to asserting the exception, and
> a negative-length test was added (PR
> [#22358](https://github.com/hmislk/hmis/pull/22358) review comments).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/core/util/ClientPortalOtpGenerator.java src/test/java/com/divudi/core/util/ClientPortalOtpGeneratorTest.java
git commit -m "feat(client-portal): add pure OTP code generator"
```

---

### Task 8: Regenerate DDL for the new `ClientAccount` table

> **DEFERRED — not part of this Foundation PR.** Per an explicit decision
> made before execution (see PR [#22358](https://github.com/hmislk/hmis/pull/22358)),
> this task is deliberately **not** run as part of this plan. `generate-ddl`
> is a heavyweight, machine-specific procedure (full local build + Payara
> redeploy + a push to the separate `hmis.wiki` repo) that shouldn't run
> once per plan — it will be run once, manually (not via an implementer
> subagent), after **all** client-portal follow-up plans (staff-assisted
> registration, self-service phone/email OTP, kiosk registration, and the
> `Institution.defaultInstitution` field for the landing page) have added
> their schema changes too, so the full schema diff is captured in a single
> pass instead of one redeploy cycle per plan. The steps below describe how
> that eventual, single DDL-regeneration pass will work — they are not
> something the Foundation PR itself completes.

**Files:**
- Modify: `tmp/createDDL.jdbc` (or wherever the `generate-ddl` skill writes output)
- Modify: `Database-Schema-DDL-Generation-Guide` wiki page (handled by the skill)

**Interfaces:**
- Consumes: the `ClientAccount` entity from Task 3 (new table).
- Produces: updated DDL artifacts so other developers and fresh installs pick up the new `CLIENTACCOUNT` table without hand-writing a migration.

- [ ] **Step 1: Run the `generate-ddl` skill**

Invoke the `generate-ddl` skill now that `ClientAccount` (Task 3) exists. Follow its instructions exactly — it handles regenerating `tmp/createDDL.jdbc` and the wiki page.

- [ ] **Step 2: Confirm the new table appears in the generated DDL**

Run: `grep -i "CLIENTACCOUNT" tmp/createDDL.jdbc`
Expected: at least one `CREATE TABLE CLIENTACCOUNT` (or similarly-cased) statement present.

- [ ] **Step 3: Commit**

```bash
git add tmp/createDDL.jdbc
git commit -m "docs(client-portal): regenerate DDL for ClientAccount table"
```

---

## Self-Review Notes

- **Spec coverage**: This plan covers only the "shared foundation" slice of the design spec (§1 data model's `ClientAccount` entity, and the reusable pieces §2-§5 lean on: match classification, IP allowlist, OTP generation, distinct `MessageType`). It deliberately does **not** cover any of the four registration channels, password/login, or the landing page — those are separate follow-up plans per the phased approach agreed with the user.
- **Placeholder scan**: no TBD/TODO; every step has complete, runnable code.
- **Type consistency**: `ClientAccount.createdVia` is `ClientAccountCreationChannel` (Task 1) throughout; `ClientAccountFacade.findByPerson` returns `ClientAccount` (nullable) consistently; `ClientPortalMatcher.classify` takes `List<Patient>` and returns the nested `MatchResult` enum consistently across its test and implementation.
- **Not yet covered by this plan** (left to channel-specific follow-up plans, to be written after this one lands): staff-assisted admin screen + new privilege, self-service phone/email OTP controllers + XHTML, kiosk controller + XHTML + new-patient creation reuse (`PatientController.saveSelected`), email-OTP body builder + `EmailManagerEjb` wiring, password hashing/verification wiring (reusing `SecurityController.hashAndCheck`/`matchPassword` — no new hashing code needed), password reset flow, `Institution.defaultInstitution` field + landing page.
