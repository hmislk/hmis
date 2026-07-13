# Cross-Entity ID Boundary Bug — Shift Queries

## Background

Before the sequence pre-allocation change, all entities shared a single `AUTO` sequence table, so IDs were globally monotonic. Queries like `payment.id > shiftStartBill.id` were valid for temporal ordering because both IDs came from the same sequence.

After switching to `GenerationType.IDENTITY` (per-table `AUTO_INCREMENT`), **each table has its own independent ID series**. Comparing a `payment.id` against a `bill.id` is now meaningless — they count independently from different starting points.

## How the Bug Manifests

A payment created *after* a bill can receive an ID that is numerically *lower* than that bill's ID, because payment IDs come from the `payment` table's counter and bill IDs from the `bill` table's counter. The queries that used `payment.id > shiftStartBill.id` silently exclude those payments from shift totals, handover pages, and cashbook entries.

**Confirmed on coop DB (2026-04-16, user Dilka):**

| Payment ID | Bill ID | Created At | Shift Start Bill ID |
|---|---|---|---|
| 13860728 | 13860808 | 14:08:21 | 13860802 |
| 13860758 | 13860817 | 14:09:54 | 13860802 |
| 13860766 | 13860834 | 14:22:42 | 13860802 |
| 13860767 | 13860839 | 14:27:28 | 13860802 |

Payment IDs are lower than the shift start bill ID (13860802) even though they were collected after the shift started. However, their **own bill IDs** are all higher than 13860802.

## The Fix Pattern

**Wrong (cross-entity):**
```java
// p is Payment, :sid is a Bill ID — different tables, different counters
"AND p.id > :sid"
m.put("sid", startBill.getId());
```

**Correct option A — use the payment's own bill ID (simplest, same-table Bill-to-Bill):**
```java
// JOIN p.bill b is already in the query — b.id is same table as startBill.id
"AND b.id > :sid"
m.put("sid", startBill.getId());
```

**Correct option B — use timestamp:**
```java
"AND b.createdAt >= :startTime"
m.put("startTime", startBill.getCreatedAt());
```

Option A is preferred for queries that already `JOIN p.bill b`, as it avoids any clock-skew concern and keeps the comparison within the `bill` table's ID series.

---

## Issues

### Fixed

| Issue | PR | Methods Fixed |
|---|---|---|
| [#19965](https://github.com/hmislk/hmis/issues/19965) | [#19966](https://github.com/hmislk/hmis/pull/19966) | `fetchPaymentsFromShiftStartToEndByDateAndDepartment(Bill,Bill)`, `fetchShiftFloatsFromShiftStartToEnd(Bill,Bill,WebUser)`, `fetchBankPayments(Bill,Bill,WebUser)` |
| [#19969](https://github.com/hmislk/hmis/issues/19969) | [#19973](https://github.com/hmislk/hmis/pull/19973) | `fillPaymentsFromShiftStartToNow()`, `fillPaymentsFromShiftStartToNow(Bill,WebUser)`, `fillPaymentsFromShiftStartToEnd(Bill,Bill,WebUser)` |
| [#19970](https://github.com/hmislk/hmis/issues/19970) | [#19974](https://github.com/hmislk/hmis/pull/19974) | `fillPaymentsFromShiftStartToNowNotYetStartedToEntereToCashbook()`, `fillPaymentsFromShiftStartToNowNotYetStartedToEntereToCashbookFilteredByDateAndDepartment()`, `generatePaymentsFromShiftStartToEndToEnterToCashbookFilteredByDateAndDepartment(Bill,Bill)` |
| [#19971](https://github.com/hmislk/hmis/issues/19971) | [#19975](https://github.com/hmislk/hmis/pull/19975) | `fillBillsFromShiftStartToNow()` |
| [#19972](https://github.com/hmislk/hmis/issues/19972) | [#19976](https://github.com/hmislk/hmis/pull/19976) | `rejectToReceiveHandoverBill()`, `recallMyHandoverBill()`, `settleHandoverStartBill()` |

Notes:
- PR #19966 used Option B (timestamp). These can be revised to Option A if preferred.
- PR #19973 used Option A (`b.id`) for shift summary display — adds `JOIN p.bill b` where missing.
- PR #19974 used Option A (`b.id`) for cashbook entry — all three already had `JOIN p.bill b`.
- PR #19975 used Option B (timestamp) for `fillBillsFromShiftStartToNow()` — Bill-to-Bill, robustness fix.
- PR #19976 used Option A (`b.id`) for float marking/reset — all three already had `JOIN p.bill b`.

---

## Summary Table

| Issue | Area | Severity | Status |
|---|---|---|---|
| [#19965](https://github.com/hmislk/hmis/issues/19965) | Handover totals | Critical | Fixed ([PR #19966](https://github.com/hmislk/hmis/pull/19966)) |
| [#19969](https://github.com/hmislk/hmis/issues/19969) | Shift summary display | Critical | Fixed ([PR #19973](https://github.com/hmislk/hmis/pull/19973)) |
| [#19970](https://github.com/hmislk/hmis/issues/19970) | Cashbook entry | Critical | Fixed ([PR #19974](https://github.com/hmislk/hmis/pull/19974)) |
| [#19971](https://github.com/hmislk/hmis/issues/19971) | Shift end bills list | Low | Fixed ([PR #19975](https://github.com/hmislk/hmis/pull/19975)) |
| [#19972](https://github.com/hmislk/hmis/issues/19972) | Float marking/reset | High | Fixed ([PR #19976](https://github.com/hmislk/hmis/pull/19976)) |
