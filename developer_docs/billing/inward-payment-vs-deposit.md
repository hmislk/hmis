# Inward "Make a Payment" vs "Make a Deposit" — Developer Guide

## Overview

This document explains the `BillTypeAtomic` split between the inpatient **Make a Payment** feature
(`InwardPaymentController`) and the inpatient **Make a Deposit** feature (`InwardDepositController`),
why the split exists, and the config/privilege wiring both features depend on.

Deployment date for the atomic swap described below: **2026-08-10**.

---

## The Atomic Swap

Historically, the inpatient **Payments** feature (button labelled "Payments", now renamed to
"Make a Payment") was backed by `BillTypeAtomic.INWARD_DEPOSIT`. This was an internal naming bug —
the UI said "Payments" but every bill it created was persisted with `BILLTYPEATOMIC='INWARD_DEPOSIT'`
(`EnumType.STRING` on `Bill.billTypeAtomic`). There was no separate "Deposit" feature at the time, so
the mismatch had no functional consequence — it only mislabeled the data.

This plan (issue #22804 and its siblings) did two things:

1. Added a new `INWARD_PAYMENT` atomic family and repointed `InwardPaymentController` (the renamed
   "Make a Payment" feature) to it.
2. Added a genuinely new **Make a Deposit** feature (`InwardDepositController`), backed by the
   pre-existing `INWARD_DEPOSIT` family — which now, for the first time, actually means what its name says.

Both families live in `com.divudi.core.data.BillTypeAtomic` and share `BillType.InwardPaymentBill`:

```java
INWARD_DEPOSIT("Inward Deposit", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.CASH_IN, ...),
INWARD_DEPOSIT_CANCELLATION("Inward Deposit Cancellation", BillCategory.CANCELLATION, ...),
INWARD_DEPOSIT_REFUND("Inward Deposit Refund", BillCategory.REFUND, ...),
INWARD_DEPOSIT_REFUND_CANCELLATION("Inward Deposit Refund Cancellation", BillCategory.CANCELLATION, ...),
INWARD_PAYMENT("Inward Payment", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.CASH_IN, ...),
INWARD_PAYMENT_CANCELLATION("Inward Payment Cancellation", BillCategory.CANCELLATION, ...),
INWARD_PAYMENT_REFUND("Inward Payment Refund", BillCategory.REFUND, ...),
INWARD_PAYMENT_REFUND_CANCELLATION("Inward Payment Refund Cancellation", BillCategory.CANCELLATION, ...),
```

`INWARD_DEPOSIT`'s 4-member family already existed before this plan (it just had nothing genuine
pointing at it). `INWARD_PAYMENT`'s 4-member family was created fresh, mirroring it exactly.

`BillBeanController.updateInwardDipositList(...)` treats both atomics identically once a bill
reaches a finalized encounter: `balance`, `paidAmount`, and `settledAmountByPatient` are all
updated the same way for a Deposit bill as for a Payment bill — money paid via either path counts
as settled by the patient.

### Historical data note — not a bug

Any bill dated **before 2026-08-10** with `BILLTYPEATOMIC='INWARD_DEPOSIT'` under the
`InwardPaymentBill` bill type is a historical **Payment**, not a Deposit. This is an intentional,
accepted reclassification confirmed by the project owner during design — do not "fix" old rows to
`INWARD_PAYMENT`, and do not treat pre-2026-08-10 `INWARD_DEPOSIT` rows as genuine deposits in
reports or reconciliation. Any report that aggregates by this atomic across a date range spanning
the deployment date must account for the meaning change at the boundary.

---

## Bill Numbering Migration

Both Make a Payment and Make a Deposit moved from the legacy `institutionBillNumberGenerator`
(keyed by `BillType`, continuous/non-yearly numbering) to the same Yearly/atomic-keyed generator
family already used by OPD and Pharmacy — `departmentBillNumberGeneratorYearly` /
`institutionBillNumberGeneratorYearly` (keyed by `BillTypeAtomic`, resets per year).

**This changes the Payments bill number FORMAT** — from continuous non-yearly numbering to
year-resetting numbering. Verified live on first deployment:

- The first **Payment** bill taken after deployment got INSID `//26/000001` — a fresh sequence
  start under the new `INWARD_PAYMENT` atomic key.
- A **Deposit** bill taken in the same session got INSID `//26/000059` — continuing whatever count
  was already running under the pre-existing `INWARD_DEPOSIT` atomic key (e.g. from the Appointment
  Deposit conversion feature, which also uses `INWARD_DEPOSIT`).

**This format change requires client communication before deploying to a production environment.**
Any client relying on continuous non-yearly Payment bill numbers (e.g. for reconciliation against
printed receipts, or external references keyed to the old numbering) needs to be told the sequence
resets going forward and restarts at `000001` for the year of deployment.

---

## Required Post-Deployment Config

Two new `ConfigOption` rows (`SCOPE=APPLICATION`, `VALUETYPE=LONG_TEXT`) must be set via the
Application Options admin page (or CSV import) after deploying to any environment:

| Config Option Key | Purpose | Recommended Value |
|---|---|---|
| `Bill Number Suffix for INWARD_PAYMENT` | Suffix appended to Make a Payment bill numbers | `WA` (matches historical `INWPAY` suffix `WA`) |
| `Bill Number Suffix for INWARD_DEPOSIT` | Suffix appended to Make a Deposit bill numbers | `WD` (distinct from Payment — confirm with team before hardcoding in production) |

If left unset, bills still generate correctly — the suffix segment is simply blank in the bill
number string. This is not a functional blocker, but an operational gap: unset suffixes make it
harder to visually distinguish Payment/Deposit bill numbers from other inward numbering.

---

## Privilege: `InwardMakeDepositAccess` — Two Registration Points

Make a Deposit is gated by `Privileges.InwardMakeDepositAccess`. Adding the enum constant to
`Privileges.java` is **necessary but not sufficient** to make the privilege assignable to a role.

A new privilege in this codebase must be registered in **two** places:

1. `src/main/java/com/divudi/core/data/Privileges.java` — the enum constant itself.
2. `src/main/java/com/divudi/bean/common/UserPrivilageController.java` — the `inwardBillingNode`
   tree that drives the admin "assign privileges to role" UI page. Without this second
   registration, no admin can grant the privilege to any role, no matter how the enum is used
   elsewhere in code.

```java
// UserPrivilageController.java — inwardBillingNode tree
new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardMakeDepositAccess, "Make Deposit Access"), inwardBillingNode);
```

This was found as a real gap during implementation (via code trace) and reproduced live: the
"Make a Deposit" button was invisible even to a user with otherwise-full Payments access until
this second registration point was added **and** a role/user was granted the privilege through
the admin UI.

**General lesson**: for any future new privilege in this codebase, adding the `Privileges.java`
constant alone is not enough — always check whether the feature's privilege also needs adding to
the relevant tree builder method in `UserPrivilageController.java`, or it will be permanently
ungrantable through the UI.

---

## Latent Crash Bug Fixed as a Side Effect

`PatientDepositService.java` has two `updateBalance` overloads (`updateBalance(Payment, PatientDeposit)`
and `updateBalance(Bill, PatientDeposit)`), each `switch`ing on `BillTypeAtomic` with
`default: throw new AssertionError()`.

Because this plan repointed Payments bills from `INWARD_DEPOSIT` to `INWARD_PAYMENT`, any inward
payment settled via `PaymentMethod.PatientDeposit` would have hit the `default` branch and crashed
at runtime — invisible to the compiler, since `switch` on an enum doesn't require exhaustiveness
here. This was caught during implementation via a careful codebase sweep and fixed **in the
`updateBalance(Payment, PatientDeposit)` overload only** by adding `INWARD_PAYMENT` /
`INWARD_PAYMENT_CANCELLATION` as fall-through cases alongside the retained `INWARD_DEPOSIT` /
`INWARD_DEPOSIT_CANCELLATION` cases:

```java
// PatientDepositService.updateBalance(Payment, PatientDeposit)
case INWARD_PAYMENT:
case INWARD_DEPOSIT:
    ...
    break;
case INWARD_PAYMENT_CANCELLATION:
case INWARD_DEPOSIT_CANCELLATION:
    ...
    break;
```

### Known remaining gap (pre-existing, out of scope)

Neither `updateBalance` overload has a case for `INWARD_PAYMENT_REFUND` / `INWARD_PAYMENT_REFUND_CANCELLATION`,
nor their `INWARD_DEPOSIT_REFUND` / `INWARD_DEPOSIT_REFUND_CANCELLATION` twins. This gap predates
this plan (the refund atomics were never handled here, even back when `INWARD_DEPOSIT` alone backed
Payments). Refunding an inward payment or deposit that was originally paid via `PaymentMethod.PatientDeposit`
will still hit `default: throw new AssertionError()` today. Recommended as a follow-up issue —
explicitly out of scope for this plan.

Additionally, the fix above only touched `updateBalance(Payment, PatientDeposit)`. The sibling
`updateBalance(Bill, PatientDeposit)` overload was never updated and still has no `INWARD_PAYMENT`
/ `INWARD_DEPOSIT` (or cancellation/refund) cases at all — any code path that reaches it with an
inward atomic will still hit `default: throw new AssertionError()`. Same class of pre-existing,
deferred gap as the refund cases above; also recommended as a follow-up rather than something this
plan resolves.

---

## Deposit Cancellation/Refund Not Implemented

The `INWARD_DEPOSIT` atomic family includes `INWARD_DEPOSIT_CANCELLATION`, `INWARD_DEPOSIT_REFUND`,
and `INWARD_DEPOSIT_REFUND_CANCELLATION` — all pre-existing (unlike `INWARD_PAYMENT`'s family, which
was created fresh with all 4 members in this plan) and structurally ready for use. However, **no
UI or controller code exists yet** to cancel or refund a Deposit bill. This is explicitly out of
scope for this plan and is planned as a future issue.

---

## Deployment Checklist

- [ ] Set the two config options: `Bill Number Suffix for INWARD_PAYMENT`, `Bill Number Suffix for INWARD_DEPOSIT` (Application Options admin page or CSV import).
- [ ] Register `InwardMakeDepositAccess` on whichever roles should have Make a Deposit access, via the admin UI's user-privilege tree — department-scoped to each inward billing department.
- [ ] Communicate the Payments bill-number format change (continuous → yearly-resetting) to affected clients **before** deploying to their environment.

---

## Related Files

| File | Role |
|---|---|
| `src/main/java/com/divudi/core/data/BillTypeAtomic.java` | `INWARD_PAYMENT` / `INWARD_DEPOSIT` atomic family definitions |
| `src/main/java/com/divudi/bean/inward/InwardPaymentController.java` | Make a Payment controller (repointed to `INWARD_PAYMENT`) |
| `src/main/java/com/divudi/bean/inward/InwardDepositController.java` | Make a Deposit controller (new, backed by `INWARD_DEPOSIT`) |
| `src/main/java/com/divudi/core/data/Privileges.java` | `InwardMakeDepositAccess` privilege constant |
| `src/main/java/com/divudi/bean/common/UserPrivilageController.java` | `inwardBillingNode` — admin privilege-assignment tree (2nd registration point) |
| `src/main/java/com/divudi/service/PatientDepositService.java` | `updateBalance` — PatientDeposit settlement switch, fixed for `INWARD_PAYMENT` |
| `src/main/java/com/divudi/ejb/BillNumberGenerator.java` | Yearly/atomic-keyed bill number generator overloads |

---

## Related Issues

- **#22804** — Inward Make a Deposit (this plan's umbrella issue) — renamed Payments to Make a Payment, added the sibling Make a Deposit feature, split the atomic families, migrated both to yearly bill numbering.
