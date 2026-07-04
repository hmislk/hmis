# Cross-Department PO Receiving (Config-Gated)

## Problem

Purchase Orders (POs) are created in one department and, on every current
institution, must be received (GRN created) from that same department. The
receive-list query filters strictly by the user's currently active
department.

RMH Hambantota needs a different workflow: POs are created in the Pharmacy
department but finalized/approved and received (GRN created) in the Store
department. Today, `pharmacy_purchase_order_list_for_recieve.xhtml` never
shows Pharmacy-created POs to a user working in the Store department, so RMH
cannot complete their receiving workflow.

This must be opt-in per hospital, defaulting to today's behavior (department
must match), so no other institution's workflow changes.

## Current Behavior (traced in code)

- **PO creation** — `PurchaseOrderRequestController.java:806-808` sets both
  `bill.department` and `bill.fromDepartment` to the creating user's current
  department at creation time.
- **GRN receive-list query** — `SearchController.createPoTable(List, List)`
  and `SearchController.createPoTablePharmacyDto()` both filter with
  `AND b.department = :dept`, where `:dept` is
  `sessionController.getDepartment()` — the department the logged-in user is
  currently working in. This is the hard restriction causing the issue.
- **GRN creation** — `GrnController.createGrn()` sets the new GRN bill's
  department from the *current session context*, not copied from the PO. So
  a GRN created while working in Store already lands stock in Store
  correctly today — there is no business-logic blocker here, only the
  listing query needs to change.
- **Pages affected by the two query methods above**:
  - `pharmacy_purchase_order_list_for_recieve.xhtml` (main page named in the
    request)
  - `pharmacy_purchase_order_list_for_recieve_with_approval.xhtml`
  - `pharmacy_wholesale/pharmacy_purchase_order_list_for_recieve.xhtml`
  - `pharmacy_purchase_order_list_for_recieve_dto.xhtml` (DTO variant)
- **Out of scope**: `SearchController.createPoTableStore(...)`, which backs
  Store's own separate PO/GRN flow (external distributor purchases into
  Store) — unrelated to this request.

## Design

Add one boolean config option:

```
Pharmacy - Allow Cross-Department PO Receiving   (default: false)
```

Read via the existing generic pattern:
`configOptionApplicationController.getBooleanValueByKey(key, false)`.

In `SearchController.createPoTable(List<BillTypeAtomic>, List<BillTypeAtomic>)`
and `SearchController.createPoTablePharmacyDto()`, change the JPQL builder so
the `AND b.department = :dept` clause (and the `dept` parameter binding) is
only appended when the config is `false`. The
`AND b.referenceBill.institution = :ins` clause remains unconditional in
both cases — this is what keeps the relaxation scoped to *within one
institution* (one hospital's deployment/database) and never leaks POs across
institutions.

No new entity, table, or admin UI is needed — this reuses the existing
generic config-option mechanism. No changes to GRN creation, since it
already assigns the GRN to the user's current department regardless of the
PO's department.

### Access control

The existing `GoodsRecipt` privilege check on the page
(`webUserController.hasPrivilege('GoodsRecipt')`) remains the only gate. A
user still needs that privilege while working in whatever department they're
currently in to see or receive any PO — cross-department or not. No new
privilege is introduced.

## Alternatives Considered

**Explicit department-pair mapping** (e.g. a configurable "Pharmacy → Store"
pairing via a new entity/admin UI) was considered but rejected: it's more
precise but adds real complexity (new entity, migration, admin screen) for a
need that today is a single hospital, single department pair. The simple
institution-wide toggle can be revisited if a second hospital needs a
narrower, asymmetric rule.

## Testing Plan

- Config `false` (default): behavior unchanged on all four affected pages —
  POs from other departments remain excluded.
- Config `true`: a PO created in Pharmacy department appears on the
  receive-list page while the logged-in user's active department is Store
  (and vice versa), and a GRN can be created against it, landing stock in
  the currently active department.
- Cross-institution isolation still holds regardless of the config value —
  a PO belonging to a different institution never appears.
