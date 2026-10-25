# Non-Cash Settlement Approval Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every Settle Non-Cash submission a pending request that a second user must approve before the payments settle; block the requester's shift end and handover create while it is pending; debit the requester's drawer on approval.

**Architecture:** `PaymentSettlementService` (a `@Stateless` EJB) gains request / approve / reject / withdraw methods and reuses the existing `Request` entity (new `RequestType`) and `DrawerService.applyDrawerAdjustment`. Pure validation and aggregation logic lives in a new `NonCashSettlementRules` class so it can be unit-tested without a container. `FinancialTransactionController` asks the service for a pending request before shift end and handover create. A new view-scoped `NonCashSettlementApprovalController` + page lets approvers act.

**Tech Stack:** Java 11, Java EE 8 (EJB, JPA/EclipseLink, JSF 2.3), PrimeFaces, JUnit 5, Maven.

**Spec:** `developer_docs/plans/2026-10-16-non-cash-settlement-approval-design.md` (issue #24541)

## Global Constraints

- Branch `24541-non-cash-settlement-approval`. Commit messages end with `Closes #24541` only on the final commit; others use `Refs #24541`. Every commit ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Never stage** `src/main/resources/META-INF/persistence.xml` or `.claude/settings.json` (local-only changes). Stage files by explicit path.
- No new tables or columns. No config key (approval is mandatory).
- Cash book is never touched by any code in this plan.
- JPQL only. Count queries use `findLongByJpql`, never `findDoubleByJpql`.
- Never modify or remove existing constructors.
- Privilege-gated buttons use `disabled`, not `rendered=false`, when the viewer can see the page but may not act (tooltip says why).
- XHTML: no `h:panelGrid` for layout, no raw `h1`–`h6`; filters in a left sidebar (see `ui-guidelines` skill). AJAX `update` targets must exist (see `jsf-ajax` skill).
- New request numbers: `"NCSET-" + request.getId()` (same pattern as `DrawerAdjustmentController`, which uses `"DRADJ-" + id`).
- Build (no tests): `$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-11.0.23.9-hotspot"; & "D:\Program Files\NetBeans-18\netbeans\java\maven\bin\mvn.cmd" -q compile -Dmaven.test.skip=true`
- Single test: `& "D:\Program Files\NetBeans-18\netbeans\java\maven\bin\mvn.cmd" -q test -Dtest=NonCashSettlementRulesTest -Dsurefire.failIfNoSpecifiedTests=false` (with the same `JAVA_HOME`).

## Review Focus

1. **Approver is the requester** → approval refused with a clear message (test in Task 1: `approvalBlocker_sameUser`).
2. **A payment changed after the request** (bill cancelled, payment retired, holder changed, already settled) → whole approval refused, nothing partially applied (tests in Task 1: `paymentBlocker_*`).
3. **Double-click / two approvers** on the same request → second call gets "no longer pending" (service status re-check in Task 3 Step 3; verified in Playwright).
4. **Cancelling a legacy (pre-approval) settlement** → no drawer credit (test in Task 1: `reversesDrawerOnCancel_legacyBill`).
5. **Mixed-method settlement** (card + cheque in one request) → each method debited separately by its own sum (test in Task 1: `totalsByMethod_mixed`).

---

### Task 1: `NonCashSettlementRules` — pure rules + unit tests

**Files:**
- Create: `src/main/java/com/divudi/service/NonCashSettlementRules.java`
- Test: `src/test/java/com/divudi/service/NonCashSettlementRulesTest.java`

**Interfaces:**
- Produces:
  - `public static Map<PaymentMethod, Double> totalsByMethod(List<Payment> payments)` — EnumMap, skips null payments / null methods
  - `public static String approvalBlocker(WebUser requester, WebUser approver)` — `null` if allowed, else message
  - `public static String paymentApprovalBlocker(Payment p, Bill settlementBill, WebUser requester)` — `null` if OK, else message naming the payment id
  - `public static boolean reversesDrawerOnCancel(Bill settlementBill)` — true iff `settlementBill.getApproveAt() != null`

- [ ] **Step 1: Write the failing test**

```java
package com.divudi.service;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NonCashSettlementRulesTest {

    private static WebUser user(long id) {
        WebUser u = new WebUser();
        u.setId(id);
        return u;
    }

    private static Bill bill(long id) {
        Bill b = new Bill();
        b.setId(id);
        return b;
    }

    private static Payment payment(long id, PaymentMethod m, double value, Bill settlementBill, WebUser holder) {
        Payment p = new Payment();
        p.setId(id);
        p.setPaymentMethod(m);
        p.setPaidValue(value);
        p.setPaymentSettlementBill(settlementBill);
        p.setCurrentHolder(holder);
        return p;
    }

    @Test
    public void totalsByMethod_mixed() {
        Bill b = bill(1);
        WebUser h = user(10);
        Map<PaymentMethod, Double> t = NonCashSettlementRules.totalsByMethod(Arrays.asList(
                payment(1, PaymentMethod.Card, 100.0, b, h),
                payment(2, PaymentMethod.Card, 50.5, b, h),
                payment(3, PaymentMethod.Cheque, 200.0, b, h),
                null));
        assertEquals(2, t.size());
        assertEquals(150.5, t.get(PaymentMethod.Card), 0.0001);
        assertEquals(200.0, t.get(PaymentMethod.Cheque), 0.0001);
    }

    @Test
    public void approvalBlocker_sameUser() {
        assertNotNull(NonCashSettlementRules.approvalBlocker(user(5), user(5)));
    }

    @Test
    public void approvalBlocker_differentUser() {
        assertNull(NonCashSettlementRules.approvalBlocker(user(5), user(6)));
    }

    @Test
    public void approvalBlocker_nulls() {
        assertNotNull(NonCashSettlementRules.approvalBlocker(null, user(6)));
        assertNotNull(NonCashSettlementRules.approvalBlocker(user(5), null));
    }

    @Test
    public void paymentBlocker_ok() {
        Bill b = bill(1);
        WebUser h = user(10);
        assertNull(NonCashSettlementRules.paymentApprovalBlocker(payment(7, PaymentMethod.Card, 1, b, h), b, h));
    }

    @Test
    public void paymentBlocker_otherBill() {
        WebUser h = user(10);
        assertNotNull(NonCashSettlementRules.paymentApprovalBlocker(payment(7, PaymentMethod.Card, 1, bill(2), h), bill(1), h));
    }

    @Test
    public void paymentBlocker_holderChanged() {
        Bill b = bill(1);
        assertNotNull(NonCashSettlementRules.paymentApprovalBlocker(payment(7, PaymentMethod.Card, 1, b, user(11)), b, user(10)));
    }

    @Test
    public void paymentBlocker_alreadySettled() {
        Bill b = bill(1);
        WebUser h = user(10);
        Payment p = payment(7, PaymentMethod.Card, 1, b, h);
        p.setPaymentSettled(true);
        assertNotNull(NonCashSettlementRules.paymentApprovalBlocker(p, b, h));
    }

    @Test
    public void paymentBlocker_cancelledOrRetired() {
        Bill b = bill(1);
        WebUser h = user(10);
        Payment c = payment(7, PaymentMethod.Card, 1, b, h);
        c.setCancelled(true);
        assertNotNull(NonCashSettlementRules.paymentApprovalBlocker(c, b, h));
        Payment r = payment(8, PaymentMethod.Card, 1, b, h);
        r.setRetired(true);
        assertNotNull(NonCashSettlementRules.paymentApprovalBlocker(r, b, h));
    }

    @Test
    public void reversesDrawerOnCancel_legacyBill() {
        assertFalse(NonCashSettlementRules.reversesDrawerOnCancel(bill(1)));
    }

    @Test
    public void reversesDrawerOnCancel_approvedBill() {
        Bill b = bill(1);
        b.setApproveAt(new Date());
        assertTrue(NonCashSettlementRules.reversesDrawerOnCancel(b));
    }
}
```

Before running: confirm the setters used exist (`Payment.setId`, `setPaidValue(double)`, `setCancelled`, `setRetired`, `setPaymentSettled`, `WebUser.setId`, `Bill.setId`, `Bill.setApproveAt`). If a setter is named differently, adapt the test to the real name — do not add setters to entities.

- [ ] **Step 2: Run test to verify it fails**

Run the single-test command from Global Constraints. Expected: compilation failure, `cannot find symbol: class NonCashSettlementRules`.

- [ ] **Step 3: Write minimal implementation**

```java
package com.divudi.service;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Container-free rules for the Non-Cash Settlement approval workflow (#24541).
 * Kept separate from PaymentSettlementService so they can be unit tested.
 */
public final class NonCashSettlementRules {

    private NonCashSettlementRules() {
    }

    public static Map<PaymentMethod, Double> totalsByMethod(List<Payment> payments) {
        Map<PaymentMethod, Double> totals = new EnumMap<>(PaymentMethod.class);
        if (payments == null) {
            return totals;
        }
        for (Payment p : payments) {
            if (p == null || p.getPaymentMethod() == null) {
                continue;
            }
            totals.merge(p.getPaymentMethod(), p.getPaidValue(), Double::sum);
        }
        return totals;
    }

    public static String approvalBlocker(WebUser requester, WebUser approver) {
        if (requester == null) {
            return "The settlement request has no requester.";
        }
        if (approver == null) {
            return "Approver is required.";
        }
        if (requester.equals(approver)) {
            return "You cannot approve your own settlement request. Another authorised user must approve it.";
        }
        return null;
    }

    public static String paymentApprovalBlocker(Payment p, Bill settlementBill, WebUser requester) {
        if (p == null) {
            return "A payment in this request no longer exists.";
        }
        if (p.isCancelled() || p.isRetired()) {
            return "Payment " + p.getId() + " has been cancelled or removed since the request was made.";
        }
        if (p.isPaymentSettled()) {
            return "Payment " + p.getId() + " is already settled.";
        }
        if (p.getPaymentSettlementBill() == null || !p.getPaymentSettlementBill().equals(settlementBill)) {
            return "Payment " + p.getId() + " is no longer part of this settlement request.";
        }
        if (p.getCurrentHolder() == null || !p.getCurrentHolder().equals(requester)) {
            return "Payment " + p.getId() + " is no longer held by the requester.";
        }
        return null;
    }

    public static boolean reversesDrawerOnCancel(Bill settlementBill) {
        return settlementBill != null && settlementBill.getApproveAt() != null;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run the single-test command. Expected: `Tests run: 11, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/service/NonCashSettlementRules.java src/test/java/com/divudi/service/NonCashSettlementRulesTest.java
git commit -m "feat(cashier): non-cash settlement approval rules

Refs #24541

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Request type and approver privilege

**Files:**
- Modify: `src/main/java/com/divudi/core/data/RequestType.java` (add enum constant after `DRAWER_ADJUSTMENT`)
- Modify: `src/main/java/com/divudi/core/data/Privileges.java` (~line 237 and the category switch ~line 1673)
- Modify: `src/main/java/com/divudi/bean/common/UserPrivilageController.java` (~line 795)
- Modify: `src/main/java/com/divudi/bean/common/EnumController.java` (~line 647, the list that includes `RequestType.DRAWER_ADJUSTMENT`, so the type shows in request search filters)

**Interfaces:**
- Produces: `RequestType.NON_CASH_SETTLEMENT_APPROVAL`, `Privileges.ApproveNonCashSettlement`

- [ ] **Step 1: Add the request type** — in `RequestType`, directly after `DRAWER_ADJUSTMENT(...)`:

```java
    NON_CASH_SETTLEMENT_APPROVAL("Non-Cash Settlement Approval", RequestCategory.APROVEL, "NC-SET-APV"),
```

- [ ] **Step 2: Add the privilege** — in `Privileges`, directly after `SettleNonCashPayments("Settle Non-Cash Payments"),`:

```java
    ApproveNonCashSettlement("Approve Non-Cash Settlement"),
```

In the category switch, add `case ApproveNonCashSettlement:` to the group that returns `"Finance"` (next to `case SettleNonCashPayments:`).

- [ ] **Step 3: Register in the privilege tree** — in `UserPrivilageController`, directly after the `SettleNonCashPayments` node:

```java
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ApproveNonCashSettlement, "Approve Non-Cash Settlement"), cashTransactionNode);
```

- [ ] **Step 4: Add to EnumController list** — next to `rt.add(RequestType.DRAWER_ADJUSTMENT);`:

```java
        rt.add(RequestType.NON_CASH_SETTLEMENT_APPROVAL);
```

- [ ] **Step 5: Compile** — run the build command. Expected: BUILD SUCCESS. Also `git grep -n "switch (.*RequestType\|case DRAWER_ADJUSTMENT" -- src/main/java` and confirm no exhaustive switch now needs the new constant (if one throws on unknown types, add a case mirroring `DRAWER_ADJUSTMENT`).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/divudi/core/data/RequestType.java src/main/java/com/divudi/core/data/Privileges.java src/main/java/com/divudi/bean/common/UserPrivilageController.java src/main/java/com/divudi/bean/common/EnumController.java
git commit -m "feat(cashier): request type and privilege for non-cash settlement approval

Refs #24541

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: `PaymentSettlementService` — request / approve / reject / withdraw / cancel

**Files:**
- Modify: `src/main/java/com/divudi/service/PaymentSettlementService.java`

**Interfaces:**
- Consumes: `NonCashSettlementRules.*` (Task 1); `RequestType.NON_CASH_SETTLEMENT_APPROVAL` (Task 2); `RequestService.save(Request, WebUser)`; `RequestFacade.find(Long)`, `RequestFacade.findByJpql`, `RequestFacade.findFirstByJpql`, `RequestFacade.findLongByJpql`; `DrawerService.getUsersDrawer(WebUser)`, `DrawerService.applyDrawerAdjustment(Drawer, PaymentMethod, double delta, Bill, WebUser actor)`.
- Produces (used by Tasks 4–6):
  - `public Bill requestSettlement(List<Payment> payments, String referenceNumber, String comments, WebUser actor)` — replaces `settlePayments`
  - `public Request findPendingRequestOf(WebUser requester)` — first PENDING request or `null`
  - `public List<Request> findPendingRequestsOf(WebUser requester)`
  - `public long countPendingRequests()`
  - `public List<Request> findRequests(RequestStatus status, Department department, Date fromDate, Date toDate)` — any arg may be null
  - `public List<Payment> findPaymentsOfSettlementBill(Bill settlementBill)`
  - `public void approveSettlement(Long requestId, WebUser approver)`
  - `public void rejectSettlement(Long requestId, WebUser actor, String reason)`
  - `public void withdrawSettlement(Long requestId, WebUser actor)`
  - `cancelSettlement(Bill, String, WebUser)` — signature unchanged
  - All throw `IllegalArgumentException` / `IllegalStateException` with user-readable messages.

- [ ] **Step 1: Inject dependencies and update the class Javadoc**

Add imports (`Request`, `RequestStatus`, `RequestType`, `RequestFacade`, `Drawer`, `Date` already imported) and fields:

```java
    @EJB
    private RequestFacade requestFacade;
    @EJB
    private RequestService requestService;
    @EJB
    private DrawerService drawerService;

    public static final String REQUEST_NO_PREFIX = "NCSET-";
```

Replace the class Javadoc paragraph "Settlement does NOT write to the Cash Book..." with:

```java
 * Settlement is a two-step maker-checker workflow (#24541): the holder submits a
 * request (payments linked to a pending PAYMENT_SETTLEMENT_BILL, nothing moves),
 * and a different user with ApproveNonCashSettlement approves it, which settles the
 * payments and debits the holder's drawer. Cash Book is never touched here.
 * See developer_docs/plans/2026-10-16-non-cash-settlement-approval-design.md.
```

- [ ] **Step 2: Lock pending payments out of the eligible lists**

In both `findPendingSettlementDepartments` and `findPendingSettlementPayments`, add after `" AND p.paymentSettled = false"`:

```java
                + " AND p.paymentSettlementBill IS NULL"
                + " AND p.handingOverStarted = false"
```

- [ ] **Step 3: Replace `settlePayments` with `requestSettlement`**

Rename the method to `requestSettlement` (same parameters). Update its Javadoc to "Creates a pending PAYMENT_SETTLEMENT_BILL and a NON_CASH_SETTLEMENT_APPROVAL request. Payments are linked to the bill but not settled; drawer and cash book untouched." Inside the validation loop, after the `isPaymentSettled()` check, add:

```java
            if (p.getPaymentSettlementBill() != null) {
                throw new IllegalStateException(
                        "Payment " + p.getId() + " is already in a settlement request awaiting approval");
            }
            if (p.isHandingOverStarted()) {
                throw new IllegalStateException(
                        "Payment " + p.getId() + " is in a handover that has not been accepted yet — cannot settle");
            }
```

Replace the final loop (which set `paymentSettled`/`currentHolder`) and the `return` with:

```java
        for (Payment p : authoritativePayments) {
            p.setPaymentSettlementBill(settlementBill);
            paymentFacade.edit(p);
        }

        Request request = new Request();
        request.setRequestType(RequestType.NON_CASH_SETTLEMENT_APPROVAL);
        request.setStatus(RequestStatus.PENDING);
        request.setBill(settlementBill);
        request.setRequester(actor);
        request.setRequestAt(new Date());
        request.setRequestReason(comments);
        request.setReferenceNumber(referenceNumber);
        request.setTargetWebUser(actor);
        request.setInstitution(settlementInstitution);
        request.setDepartment(settlementDepartment);
        requestService.save(request, actor);
        request.setRequestNo(REQUEST_NO_PREFIX + request.getId());
        requestService.save(request, actor);

        return settlementBill;
```

- [ ] **Step 4: Add the query methods**

```java
    public Request findPendingRequestOf(WebUser requester) {
        if (requester == null) {
            return null;
        }
        String jpql = "SELECT q FROM Request q"
                + " WHERE q.retired = false"
                + " AND q.requestType = :type"
                + " AND q.status = :status"
                + " AND q.requester = :requester"
                + " ORDER BY q.id";
        Map<String, Object> params = new HashMap<>();
        params.put("type", RequestType.NON_CASH_SETTLEMENT_APPROVAL);
        params.put("status", RequestStatus.PENDING);
        params.put("requester", requester);
        return requestFacade.findFirstByJpql(jpql, params);
    }

    public List<Request> findPendingRequestsOf(WebUser requester) {
        if (requester == null) {
            return new ArrayList<>();
        }
        String jpql = "SELECT q FROM Request q"
                + " WHERE q.retired = false"
                + " AND q.requestType = :type"
                + " AND q.status = :status"
                + " AND q.requester = :requester"
                + " ORDER BY q.id";
        Map<String, Object> params = new HashMap<>();
        params.put("type", RequestType.NON_CASH_SETTLEMENT_APPROVAL);
        params.put("status", RequestStatus.PENDING);
        params.put("requester", requester);
        return requestFacade.findByJpql(jpql, params);
    }

    public long countPendingRequests() {
        String jpql = "SELECT COUNT(q) FROM Request q"
                + " WHERE q.retired = false"
                + " AND q.requestType = :type"
                + " AND q.status = :status";
        Map<String, Object> params = new HashMap<>();
        params.put("type", RequestType.NON_CASH_SETTLEMENT_APPROVAL);
        params.put("status", RequestStatus.PENDING);
        return requestFacade.findLongByJpql(jpql, params);
    }

    public List<Request> findRequests(RequestStatus status, Department department, Date fromDate, Date toDate) {
        StringBuilder jpql = new StringBuilder("SELECT q FROM Request q"
                + " WHERE q.retired = false"
                + " AND q.requestType = :type");
        Map<String, Object> params = new HashMap<>();
        params.put("type", RequestType.NON_CASH_SETTLEMENT_APPROVAL);
        if (status != null) {
            jpql.append(" AND q.status = :status");
            params.put("status", status);
        }
        if (department != null) {
            jpql.append(" AND q.department = :dep");
            params.put("dep", department);
        }
        if (fromDate != null) {
            jpql.append(" AND q.requestAt >= :fd");
            params.put("fd", fromDate);
        }
        if (toDate != null) {
            jpql.append(" AND q.requestAt <= :td");
            params.put("td", toDate);
        }
        jpql.append(" ORDER BY q.id DESC");
        return requestFacade.findByJpql(jpql.toString(), params, javax.persistence.TemporalType.TIMESTAMP);
    }

    public List<Payment> findPaymentsOfSettlementBill(Bill settlementBill) {
        if (settlementBill == null) {
            return new ArrayList<>();
        }
        String jpql = "SELECT p FROM Payment p WHERE p.paymentSettlementBill = :b ORDER BY p.paymentMethod, p.createdAt";
        Map<String, Object> params = new HashMap<>();
        params.put("b", settlementBill);
        return paymentFacade.findByJpql(jpql, params);
    }
```

Check `RequestFacade`/`AbstractFacade` has `findByJpql(String, Map, TemporalType)`; if the overload is named differently, use the existing one that accepts a `TemporalType`.

- [ ] **Step 5: Add a private loader shared by approve/reject/withdraw**

```java
    private Request loadPendingRequest(Long requestId) {
        if (requestId == null) {
            throw new IllegalArgumentException("Request is required");
        }
        Request request = requestFacade.find(requestId);
        if (request == null || request.isRetired()
                || request.getRequestType() != RequestType.NON_CASH_SETTLEMENT_APPROVAL) {
            throw new IllegalStateException("Settlement request not found");
        }
        if (request.getStatus() != RequestStatus.PENDING && request.getStatus() != RequestStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Request " + request.getRequestNo() + " is no longer pending");
        }
        Bill bill = request.getBill();
        if (bill == null || bill.isRetired() || bill.isCancelled() || bill.getApproveAt() != null) {
            throw new IllegalStateException("Request " + request.getRequestNo() + " has no pending settlement bill");
        }
        return request;
    }
```

- [ ] **Step 6: Add `approveSettlement`**

```java
    public void approveSettlement(Long requestId, WebUser approver) {
        Request request = loadPendingRequest(requestId);
        WebUser requester = request.getRequester();
        String blocker = NonCashSettlementRules.approvalBlocker(requester, approver);
        if (blocker != null) {
            throw new IllegalStateException(blocker);
        }
        Bill bill = request.getBill();
        List<Payment> payments = findPaymentsOfSettlementBill(bill);
        if (payments.isEmpty()) {
            throw new IllegalStateException("Request " + request.getRequestNo() + " has no payments");
        }
        for (Payment p : payments) {
            String paymentBlocker = NonCashSettlementRules.paymentApprovalBlocker(p, bill, requester);
            if (paymentBlocker != null) {
                throw new IllegalStateException(paymentBlocker);
            }
        }
        Drawer drawer = drawerService.getUsersDrawer(requester);
        if (drawer == null) {
            throw new IllegalStateException("Drawer not found for " + requester.getName());
        }

        for (Payment p : payments) {
            p.setPaymentSettled(true);
            p.setCurrentHolder(null);
            paymentFacade.edit(p);
        }
        for (Map.Entry<PaymentMethod, Double> e : NonCashSettlementRules.totalsByMethod(payments).entrySet()) {
            drawerService.applyDrawerAdjustment(drawer, e.getKey(), -e.getValue(), bill, approver);
        }

        Date now = new Date();
        bill.setApproveUser(approver);
        bill.setApproveAt(now);
        billFacade.edit(bill);

        request.setApproved(true);
        request.setApprovedBy(approver);
        request.setApprovedAt(now);
        request.setStatus(RequestStatus.APPROVED);
        requestService.save(request, approver);
    }
```

- [ ] **Step 7: Add `rejectSettlement` and `withdrawSettlement`**

```java
    public void rejectSettlement(Long requestId, WebUser actor, String reason) {
        if (actor == null) {
            throw new IllegalArgumentException("Actor is required");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A reason is required to reject a settlement request");
        }
        Request request = loadPendingRequest(requestId);
        releasePayments(request, actor, reason.trim());
        Date now = new Date();
        request.setRejected(true);
        request.setRejectedBy(actor);
        request.setRejectedAt(now);
        request.setRejectionReason(reason.trim());
        request.setStatus(RequestStatus.REJECTED);
        requestService.save(request, actor);
    }

    public void withdrawSettlement(Long requestId, WebUser actor) {
        if (actor == null) {
            throw new IllegalArgumentException("Actor is required");
        }
        Request request = loadPendingRequest(requestId);
        if (!actor.equals(request.getRequester())) {
            throw new IllegalStateException("Only the user who made the request can withdraw it");
        }
        releasePayments(request, actor, "Withdrawn by requester");
        Date now = new Date();
        request.setCancelled(true);
        request.setCancelledBy(actor);
        request.setCancelledAt(now);
        request.setCancellationReason("Withdrawn by requester");
        request.setStatus(RequestStatus.CANCELLED);
        requestService.save(request, actor);
    }

    private void releasePayments(Request request, WebUser actor, String reason) {
        Bill bill = request.getBill();
        for (Payment p : findPaymentsOfSettlementBill(bill)) {
            if (p.isPaymentSettled()) {
                continue;
            }
            p.setPaymentSettlementBill(null);
            paymentFacade.edit(p);
        }
        bill.setRetired(true);
        bill.setRetirer(actor);
        bill.setRetiredAt(new Date());
        bill.setRetireComments(reason);
        billFacade.edit(bill);
    }
```

- [ ] **Step 8: Update `cancelSettlement` for pending and drawer reversal**

At the top of `cancelSettlement`, after the existing `isCancelled()` check, add:

```java
        if (settlementBill.isRetired()) {
            throw new IllegalStateException("Settlement bill was rejected or withdrawn — nothing to cancel");
        }
        Request pending = findRequestOfBill(settlementBill);
        if (pending != null && (pending.getStatus() == RequestStatus.PENDING
                || pending.getStatus() == RequestStatus.UNDER_REVIEW)) {
            throw new IllegalStateException("Settlement is still awaiting approval — reject or withdraw it instead");
        }
        boolean reverseDrawer = NonCashSettlementRules.reversesDrawerOnCancel(settlementBill);
```

After `billFacade.create(cancellationBill);` add:

```java
        if (reverseDrawer) {
            Drawer drawer = drawerService.getUsersDrawer(originalHolder);
            if (drawer == null) {
                throw new IllegalStateException("Drawer not found for " + originalHolder.getName());
            }
            for (Map.Entry<PaymentMethod, Double> e : NonCashSettlementRules.totalsByMethod(settledPayments).entrySet()) {
                drawerService.applyDrawerAdjustment(drawer, e.getKey(), e.getValue(), cancellationBill, actor);
            }
        }
```

Add the helper:

```java
    private Request findRequestOfBill(Bill bill) {
        String jpql = "SELECT q FROM Request q WHERE q.retired = false AND q.bill = :b AND q.requestType = :type ORDER BY q.id DESC";
        Map<String, Object> params = new HashMap<>();
        params.put("b", bill);
        params.put("type", RequestType.NON_CASH_SETTLEMENT_APPROVAL);
        return requestFacade.findFirstByJpql(jpql, params);
    }
```

Update the `cancelSettlement` Javadoc line "Cash book is NOT touched." to "Cash book is NOT touched. The holder's drawer is credited back only for settlements approved under #24541 (approveAt set); legacy settlements never debited it."

- [ ] **Step 9: Compile** — build command. Expected: BUILD SUCCESS, except `PaymentSettlementController` still calls `settlePayments` → fix in Task 5. To keep each commit compiling, make the one-line rename in `PaymentSettlementController.settleSelected()` now: `paymentSettlementService.settlePayments(` → `paymentSettlementService.requestSettlement(`.

- [ ] **Step 10: Run Task 1 tests again** (single-test command). Expected: PASS.

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/divudi/service/PaymentSettlementService.java src/main/java/com/divudi/bean/cashTransaction/PaymentSettlementController.java
git commit -m "feat(cashier): non-cash settlement goes through approval and debits drawer

Refs #24541

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Shift-end and handover gates

**Files:**
- Modify: `src/main/java/com/divudi/bean/cashTransaction/FinancialTransactionController.java`

**Interfaces:**
- Consumes: `PaymentSettlementService.findPendingRequestOf(WebUser)` (Task 3)
- Produces: private `boolean blockedByPendingNonCashSettlement(String actionText)`

- [ ] **Step 1: Inject the service and add the helper**

Add near the other `@EJB` fields:

```java
    @EJB
    private PaymentSettlementService paymentSettlementService;
```

(import `com.divudi.service.PaymentSettlementService` and `com.divudi.core.entity.Request` if not already imported.)

Add the helper near `isHandoverRequiredButNotComplete()`:

```java
    /**
     * A user with a non-cash settlement awaiting approval may not end their shift or
     * create a handover (#24541). Not bypassable by any configuration option.
     */
    private boolean blockedByPendingNonCashSettlement(String actionText) {
        Request pending = paymentSettlementService.findPendingRequestOf(sessionController.getLoggedUser());
        if (pending == null) {
            return false;
        }
        JsfUtil.addErrorMessage("You have a non-cash settlement awaiting approval (request "
                + pending.getRequestNo() + "). It must be approved or rejected before you can "
                + actionText + ".");
        return true;
    }
```

- [ ] **Step 2: Authoritative checks in the three persisting methods**

- `settleShiftEnd()`: immediately after the `currentBill.getReferenceBill() == null` check, add
  ```java
        if (blockedByPendingNonCashSettlement("end your shift")) {
            return "";
        }
  ```
- `settleShiftEndFundBill()`: same block, same position (after its `getReferenceBill() == null` check).
- `settleHandoverStartBill()`: immediately after the `user.equals(sessionController.getLoggedUser())` check (inside the `try`), add
  ```java
        if (blockedByPendingNonCashSettlement("create a handover")) {
            return null;
        }
  ```

- [ ] **Step 3: Early checks in navigation methods** — as the first statement of each:

  - `navigateToCreateShiftEndSummaryBill()`, `navigateToCreateShiftEndSummaryBillForHandover()`, `navigateToCreateShiftEndSummaryBillByBills()`:
    ```java
        if (blockedByPendingNonCashSettlement("end your shift")) {
            return null;
        }
    ```
  - `navigateToHandoverCreateBill()`, `navigateToHandoverCreateBillForCurrentShift()`, `navigateToHandoverCreateBillForSelectedPeriod()`, `navigateToHandoverCreateBillForSelectedShift(Bill startBill)`:
    ```java
        if (blockedByPendingNonCashSettlement("create a handover")) {
            return null;
        }
    ```

Returning `null` keeps the user on the calling page (the cashier menu buttons are `ajax="false"`, so the page re-renders with the message).

- [ ] **Step 4: Compile** — build command. Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/bean/cashTransaction/FinancialTransactionController.java
git commit -m "feat(cashier): block shift end and handover while a non-cash settlement awaits approval

Refs #24541

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Settle Non-Cash page — submit for approval, my pending requests, withdraw

**Files:**
- Modify: `src/main/java/com/divudi/bean/cashTransaction/PaymentSettlementController.java`
- Modify: `src/main/webapp/cashier/settle_non_cash.xhtml`

**Interfaces:**
- Consumes: `requestSettlement`, `findPendingRequestsOf`, `withdrawSettlement`, `findPaymentsOfSettlementBill` (Task 3)
- Produces: controller properties `myPendingRequests` (`List<Request>`), `lastRequest` (`Request`), action `withdraw(Request)`

- [ ] **Step 1: Controller**

Add fields `private List<Request> myPendingRequests = new ArrayList<>();` and `private Request lastRequest;` with getters.

At the end of `loadPending()` (both branches — set to empty list in the no-privilege branch), add:

```java
        myPendingRequests = paymentSettlementService.findPendingRequestsOf(sessionController.getLoggedUser());
```

In `settleSelected()`: after the `requestSettlement(...)` call, set

```java
            lastRequest = paymentSettlementService.findPendingRequestsOf(sessionController.getLoggedUser())
                    .stream()
                    .filter(r -> r.getBill() != null && r.getBill().equals(lastSettlementBill))
                    .findFirst().orElse(null);
            myPendingRequests = paymentSettlementService.findPendingRequestsOf(sessionController.getLoggedUser());
```

and replace the success message with:

```java
            JsfUtil.addSuccessMessage("Settlement request " + (lastRequest == null ? "" : lastRequest.getRequestNo() + " ")
                    + "submitted for approval (" + snapshot.size() + " payment(s), "
                    + selectedDepartment.getName() + "). You cannot end your shift or create a handover until it is approved or rejected.");
```

Update the comment block in `settleSelected()` that says the service "sets paymentSettled=true and currentHolder=null" to say the service now only links the payment to the pending bill.

Add:

```java
    public void withdraw(Request request) {
        if (request == null) {
            return;
        }
        try {
            paymentSettlementService.withdrawSettlement(request.getId(), sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Request " + request.getRequestNo() + " withdrawn. The payments are back in your pending list.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        loadPending();
    }
```

Update the class Javadoc: "Backing bean for the Settle Non-Cash page (#17964). Submissions go for approval (#24541)."

- [ ] **Step 2: Page**

In `settle_non_cash.xhtml`:
1. Change the submit button `value` to `"Submit for Approval"` (keep its action `settleSelected()`); change any confirm text to "Submit these payments for settlement approval?".
2. Below the header panel's `<p:messages id="msgs"/>`, add a "My pending settlement requests" block (rendered when `not empty paymentSettlementController.myPendingRequests`): a `p:dataTable` with columns Request No (`requestNo`), Bill No (`bill.deptId`), Department (`department.name`), Requested At (`requestAt`, `dd MMM yyyy HH:mm`), Total (`bill.netTotal`, `#,##0.00`, right-aligned), and an action column with a `p:commandButton` value "Withdraw", `icon="fa fa-undo"`, `styleClass="ui-button-warning"`, `action="#{paymentSettlementController.withdraw(r)}"`, `update="settleNonCashForm"`, `onclick="if(!confirm('Withdraw this settlement request?')) return false;"`. Precede the table with a short warning line: "While a request is pending you cannot end your shift or create a handover."
3. In the print-preview region: show the request number (`paymentSettlementController.lastRequest.requestNo`) next to the bill number, show a prominent "PENDING APPROVAL" label, and replace the "Verified By" signature caption with "Approved By".

- [ ] **Step 3: Compile** — build command. Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/divudi/bean/cashTransaction/PaymentSettlementController.java src/main/webapp/cashier/settle_non_cash.xhtml
git commit -m "feat(cashier): settle non-cash page submits for approval and lists pending requests

Refs #24541

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Approvals page, controller and menu entry

**Files:**
- Create: `src/main/java/com/divudi/bean/cashTransaction/NonCashSettlementApprovalController.java`
- Create: `src/main/webapp/cashier/non_cash_settlement_approvals.xhtml`
- Modify: `src/main/webapp/cashier/index.xhtml` (after the "Settle Non-Cash Payments" button, ~line 153)

**Interfaces:**
- Consumes: `findRequests`, `countPendingRequests`, `findPaymentsOfSettlementBill`, `approveSettlement`, `rejectSettlement` (Task 3); `Privileges.ApproveNonCashSettlement` (Task 2)
- Produces: `nonCashSettlementApprovalController` bean with `navigateToApprovals()`, `search()`, `select(Request)`, `approve()`, `reject()`, `canAct(Request)`, `getPendingCount()`

- [ ] **Step 1: Controller**

```java
package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.Privileges;
import com.divudi.core.data.RequestStatus;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Request;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.PaymentSettlementService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Approve / reject pending Non-Cash Settlement requests (#24541).
 */
@Named
@ViewScoped
public class NonCashSettlementApprovalController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String PRIVILEGE = Privileges.ApproveNonCashSettlement.name();

    @EJB
    private PaymentSettlementService paymentSettlementService;
    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;

    private RequestStatus status = RequestStatus.PENDING;
    private Department department;
    private Date fromDate;
    private Date toDate;
    private List<Request> requests = new ArrayList<>();
    private Request selected;
    private List<Payment> selectedPayments = new ArrayList<>();
    private String rejectReason;
    private boolean acting;

    public String navigateToApprovals() {
        return "/cashier/non_cash_settlement_approvals?faces-redirect=true";
    }

    /** Called by f:viewAction on every entry. */
    public void init() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth(new Date());
        }
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        search();
    }

    public void search() {
        requests = paymentSettlementService.findRequests(status, department, fromDate, toDate);
        selected = null;
        selectedPayments = new ArrayList<>();
        rejectReason = null;
    }

    public void select(Request r) {
        selected = r;
        selectedPayments = r == null ? new ArrayList<>()
                : paymentSettlementService.findPaymentsOfSettlementBill(r.getBill());
        rejectReason = null;
    }

    public boolean canAct(Request r) {
        return r != null
                && (r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.UNDER_REVIEW)
                && webUserController.hasPrivilege(PRIVILEGE)
                && !sessionController.getLoggedUser().equals(r.getRequester());
    }

    public String actionBlockedReason(Request r) {
        if (r == null) {
            return "";
        }
        if (!webUserController.hasPrivilege(PRIVILEGE)) {
            return "You need the Approve Non-Cash Settlement privilege.";
        }
        if (sessionController.getLoggedUser().equals(r.getRequester())) {
            return "You cannot approve or reject your own request.";
        }
        if (r.getStatus() != RequestStatus.PENDING && r.getStatus() != RequestStatus.UNDER_REVIEW) {
            return "This request is no longer pending.";
        }
        return "";
    }

    public void approve() {
        if (acting || selected == null) {
            return;
        }
        acting = true;
        try {
            paymentSettlementService.approveSettlement(selected.getId(), sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Request " + selected.getRequestNo() + " approved. The payments are settled.");
            search();
        } catch (IllegalArgumentException | IllegalStateException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } finally {
            acting = false;
        }
    }

    public void reject() {
        if (acting || selected == null) {
            return;
        }
        acting = true;
        try {
            paymentSettlementService.rejectSettlement(selected.getId(), sessionController.getLoggedUser(), rejectReason);
            JsfUtil.addSuccessMessage("Request " + selected.getRequestNo() + " rejected. The payments are back with the requester.");
            search();
        } catch (IllegalArgumentException | IllegalStateException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } finally {
            acting = false;
        }
    }

    public long getPendingCount() {
        return paymentSettlementService.countPendingRequests();
    }

    public double getSelectedTotal() {
        double t = 0.0;
        for (Payment p : selectedPayments) {
            t += p.getPaidValue();
        }
        return t;
    }

    public RequestStatus[] getStatuses() {
        return RequestStatus.values();
    }

    // getters and setters for status, department, fromDate, toDate, requests (getter only),
    // selected (getter only), selectedPayments (getter only), rejectReason
}
```

Write out every getter/setter named in the last comment (no Lombok in this project). Verify `CommonFunctions.getStartOfMonth(Date)` and `getEndOfDay(Date)` exist (`git grep -n "public static Date getStartOfMonth\|public static Date getEndOfDay" -- src/main/java`); if not, use the equivalents that do exist.

- [ ] **Step 2: Page** — `src/main/webapp/cashier/non_cash_settlement_approvals.xhtml`, composed into `/cashier/index.xhtml` the same way as `settle_non_cash.xhtml` (`ui:composition template="/cashier/index.xhtml"`, `ui:define name="subcontent"`, `f:metadata` with `f:viewAction action="#{nonCashSettlementApprovalController.init()}"`). Layout (follow `ui-guidelines`):
  - Wrap in `h:form id="ncsApprovalForm"`; `p:messages id="msgs"` at top.
  - Bootstrap row: left `col-md-3` sidebar `p:panel header="Filters"` with Status `p:selectOneMenu` (items from `getStatuses()`, `itemLabel="#{s.label}"`, plus an "All" no-selection item mapping to `null`), Department (`p:autoComplete` using `departmentController.completeDept` — check the exact completer name used by other cashier pages), From/To `p:datePicker` (`pattern="dd MMM yyyy HH:mm"`, `showTime="true"`), and a "Search" `p:commandButton` (`action="#{nonCashSettlementApprovalController.search()}"`, `update="ncsApprovalForm"`).
  - Right `col-md-9`: `p:panel header="Non-Cash Settlement Approvals"` containing a `p:dataTable value="#{nonCashSettlementApprovalController.requests}" var="r"` with columns Request No, Bill No (`r.bill.deptId`), Department, Requested By (`r.requester.webUserPerson.name`), Requested At, Reference (`r.referenceNumber`), Total (`r.bill.netTotal`), Status (`r.status.label`), Approved/Rejected By (`r.approvedBy.webUserPerson.name` or `r.rejectedBy.webUserPerson.name`), and a "View" button (`action="#{nonCashSettlementApprovalController.select(r)}"`, `update="ncsApprovalForm"`).
  - Below the table, rendered when `selected != null`: a details `p:panel` with the payments table (Method `p.paymentMethod.label`, Bill `p.bill.deptId`, Reference `p.referenceNo`, Bank `p.bank.name`, Amount `p.paidValue`) and footer total `selectedTotal`; a `p:inputTextarea` for `rejectReason` (placeholder "Reason (required to reject)"); buttons **Approve** (`ui-button-success`, `icon="fa fa-check"`, `action="#{nonCashSettlementApprovalController.approve()}"`, `disabled="#{not nonCashSettlementApprovalController.canAct(nonCashSettlementApprovalController.selected)}"`, `title="#{nonCashSettlementApprovalController.actionBlockedReason(nonCashSettlementApprovalController.selected)}"`, confirm "Approve this settlement? The payments will be settled and removed from the requester's drawer.") and **Reject** (`ui-button-danger`, same `disabled`/`title`, confirm "Reject this settlement request?"). Both `update="ncsApprovalForm"`.
  - Confirm `webUserPerson` is the correct path from `WebUser` to the person name (`git grep -n "getWebUserPerson" -- src/main/java/com/divudi/core/entity/WebUser.java`); use the real path.

- [ ] **Step 3: Menu entry** — in `cashier/index.xhtml`, directly after the "Settle Non-Cash Payments" button:

```xml
                                    <p:commandButton
                                        class="my-1 w-100 ui-button-success"
                                        value="Non-Cash Settlement Approvals (#{nonCashSettlementApprovalController.pendingCount})"
                                        icon="pi pi-verified"
                                        ajax="false"
                                        action="#{nonCashSettlementApprovalController.navigateToApprovals()}"
                                        rendered="#{webUserController.hasPrivilege('ApproveNonCashSettlement')}">
                                    </p:commandButton>
```

(`rendered` is correct here: the whole menu entry is privilege-scoped like its neighbour; the `disabled` rule applies to action buttons on a page the user can already see.)

- [ ] **Step 4: Compile** — build command. Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/bean/cashTransaction/NonCashSettlementApprovalController.java src/main/webapp/cashier/non_cash_settlement_approvals.xhtml src/main/webapp/cashier/index.xhtml
git commit -m "feat(cashier): non-cash settlement approvals page

Refs #24541

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Developer documentation

**Files:**
- Modify: `developer_docs/cashier-drawer-cashbook-master-design.md` (§4.2, §4.4, §4.7, §4.8)
- Add (already written, untracked): `developer_docs/plans/2026-10-16-non-cash-settlement-approval-design.md`, this plan file

(The wiki pages are updated in dev-issue step 10 together with screenshots, not in this task.)

- [ ] **Step 1: Master design doc edits**
  - §4.2: after the sentence ending "…no handover query change needed).", add: "Since #24541 this is a two-step workflow: submitting creates a *pending* settlement bill and a `NON_CASH_SETTLEMENT_APPROVAL` request; the three field updates happen only when a different user with `ApproveNonCashSettlement` approves. While pending, the requester cannot end their shift or create a handover. On approval the holder's drawer is debited per payment method. See `developer_docs/plans/2026-10-16-non-cash-settlement-approval-design.md`."
  - §4.4: append "Cancelling an approved (#24541) settlement also credits the drawer back; legacy settlements (no `approveAt`) do not. A pending request is rejected or withdrawn, not cancelled."
  - §4.7: replace the bullet "Approval workflow for non-cash settlement (deferred to a later issue if needed)" with "Approval workflow — implemented in #24541 (reuses `Request`)".
  - §4.5 table text "`BillFinanceType.NO_FINANCE_TRANSACTIONS` because the settlement does NOT move drawer or cash book balances" → "…does NOT move cash book balances (the drawer is debited on approval via a drawer entry, #24541)".

- [ ] **Step 2: Commit**

```bash
git add developer_docs/cashier-drawer-cashbook-master-design.md developer_docs/plans/2026-10-16-non-cash-settlement-approval-design.md developer_docs/plans/2026-10-16-non-cash-settlement-approval-plan.md
git commit -m "docs(cashier): non-cash settlement approval design, plan and master-design update

Refs #24541

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## After the tasks (dev-issue steps, done by the orchestrator)

1. Full build + redeploy to local Payara (dev-issue step 6).
2. Playwright through the menus (spec §9 scenarios 1–8) with DB checks on `payment`, `request`, `bill`, `drawer`, `drawerentry` (dev-issue step 7).
3. Wiki: `Non-Cash-Settlement.md`, `Finance-Starting-and-Ending-a-Shift.md` with screenshots (dev-issue step 10).
4. Push, PR to `development`, review loop.
