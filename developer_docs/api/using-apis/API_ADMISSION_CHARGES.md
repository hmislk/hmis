# Admission Charges API

Configuration for **automatic admission charges** — routine charges billed on every matching
admission by `AdmissionChargeApplicationBean` at the moment an admission is saved, using the same
`InwardServiceBillService` pipeline the *Add Services / Investigations to BHT* screen uses. Nothing
here bills a patient directly; it only configures which items are billed for which admissions.

- Base path: `/api/admission-charges`
- Auth: `Finance: <api-key>` header on every request
- Entity: `AdmissionChargeItem`

## Response envelope

```json
{ "status": "success", "code": 200, "data": { } }
{ "status": "error",   "code": 400, "message": "..." }
```

---

## The two-dimension resolution rule

Each configured item is resolved independently, in two steps, when an admission is saved:

1. **Admission type** (outer filter) — among the rows configured for an item, the rows whose
   `admissionTypeId` matches the admission's admission type are considered first. If none match,
   the rows with `admissionTypeId = null` ("any admission type") are considered instead.
2. **Payment method** (inner filter) — within whichever set step 1 produced, the row whose
   `paymentMethod` matches the admission's payment method (`Cash` or `Credit`) is picked. If none
   matches, the row with `paymentMethod = null` ("both") is picked instead.

A `null` in either column is a real configuration, not "unset" — it means "applies to all" for
that dimension.

### The configuration trap

Step 1 is a **filter**, not a preference. As soon as *any* row exists for an item with a specific
`admissionTypeId`, the entire `admissionTypeId = null` row set for that item is ignored for
admissions of that type — it does not fall back to it.

Concretely: configuring `(Admission Charge, admissionType=Day Case, paymentMethod=Cash)` and
forgetting the Day Case + Credit row leaves a **Credit Day Case admission with no admission charge
at all**, even if a `(Admission Charge, admissionType=null, paymentMethod=null)` row exists — that
null-type row is never reached once a Day Case–specific row exists for this item.

The rule of thumb: the moment you add one admission-type-specific row for an item, you are
committing to configure **every** payment method you care about for that admission type
separately, because the general fallback is gone for that type.

### The double-charge trap

`AdmissionType.admissionFee` is an existing, separate, older mechanism. When an admission is
billed, `InwardBhtChargeAggregationService.fetchAdmissionFeeCharges()` already adds that fee to the
bill under `InwardChargeType.AdmissionFee` — as a bill total component, **without** persisting a
`BillItem`.

If an admission type has a non-zero `admissionFee` **and** an `AdmissionChargeItem` is also
configured for an item that resolves to that same admission (an admission-type-specific row for
that type, or a null-type row with no more specific override), the admission is charged **twice**
for what is effectively the same thing: once via `admissionFee`, once via the `AdmissionChargeItem`
BillItem. Check the admission type's `admissionFee` before configuring a general admission charge
against it.

---

## Cash/Credit-only restriction

`paymentMethod` accepts exactly `Cash`, `Credit`, or is omitted/`null` (meaning "both"). No other
`PaymentMethod` value is valid here — an admission's payment method is only ever Cash or Credit
(`EnumController.getPaymentMethodForAdmission()`). Sending anything else, including a real
`PaymentMethod` enum constant such as `Card` or `Staff`, is rejected with `400`.

## Uniqueness constraint

At most one **live** (non-retired) row may exist per `(item, admissionType, paymentMethod)`
triple. Creating or updating a row into a triple that already has a live row is rejected with
`400`; retire the existing row first, or update it instead.

## Item requirements

The `item` an `AdmissionChargeItem` points at must, at the time the row is created or updated:

- exist and not be retired
- have a `department` (bills are grouped on it)
- have an `institution` (fee resolution reads it)
- have an `inwardChargeType` (it drives the interim/final bill's charge-type breakdown)
- have at least one **live** `ItemFee` — an item with no fee produces a `BillItem` whose charge
  cancels and refunds as zero, which is worse than not charging it at all

All of these are enforced here so a row this API accepts is a row `AdmissionChargeApplicationBean`
can actually bill — the checks mirror `AdmissionChargeApplicationBean.buildEntry` exactly.

---

## Search

```
GET /api/admission-charges/search
```

| Param | Notes |
|---|---|
| `itemId` | Item id |
| `admissionTypeId` | `AdmissionType` id |
| `paymentMethod` | `Cash` or `Credit` — filters to that exact value (a row does not match this filter just because it "would resolve to" that method) |
| `includeRetired` | `true` to include retired rows. Default `false` |
| `limit` | Page size, 1–100. Default 30 |
| `offset` | Rows to skip. Default 0 |

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "items": [
      {
        "id": 918273,
        "itemId": 44821,
        "itemName": "Admission Charge",
        "itemDepartmentId": 486,
        "itemDepartmentName": "General Ward",
        "inwardChargeType": "OtherCharges",
        "admissionTypeId": 71,
        "admissionTypeName": "Day Case",
        "paymentMethod": "Cash",
        "price": 500.0,
        "qty": 1.0,
        "orderNo": 0,
        "retired": false
      }
    ],
    "total": 4,
    "limit": 30,
    "offset": 0
  }
}
```

`total` is the number of rows matching the filters, ignoring `limit`/`offset`.

## Get one

```
GET /api/admission-charges/{id}
GET /api/admission-charges/{id}?includeRetired=true
```

Returns a single row in the same shape as a search result item. A retired row is `404` unless
`includeRetired=true`.

## Create

```
POST /api/admission-charges
```

```json
{
  "itemId": 44821,
  "admissionTypeId": 71,
  "paymentMethod": "Cash",
  "price": 500.0,
  "qty": 1.0,
  "orderNo": 0
}
```

`itemId` and `price` are required (`price` must be `>= 0`). `admissionTypeId` omitted/`null` means
"any admission type". `paymentMethod` omitted/`null` means "both Cash and Credit". `qty` defaults
to `1.0`; `orderNo` defaults to `0`.

Rejected with `400` when: the item does not exist, the item fails any of the requirements above,
`paymentMethod` is anything other than `Cash`/`Credit`/absent, `price` is negative, or a live row
already exists for the resulting `(item, admissionType, paymentMethod)` triple.

## Update

```
PUT /api/admission-charges/{id}
```

```json
{
  "price": 550.0
}
```

All fields optional; only fields present are applied. Because `admissionTypeId` and
`paymentMethod` are two of the three columns in the uniqueness key, and `null` is itself a
meaningful value for both, a plain JSON body cannot tell "field omitted" apart from "field sent as
null" — so clearing either one back to `null` is done explicitly:

```json
{ "clearAdmissionType": true }
{ "clearPaymentMethod": true }
```

`admissionTypeId`/`paymentMethod`, when present, take priority over the corresponding `clear*`
flag. Every update re-checks the uniqueness constraint against the row's resulting identity
(excluding itself), so an update that would create a duplicate is rejected with `400` the same way
a create is.

## Retire / Restore

```
DELETE /api/admission-charges/{id}?retireComments=reason   soft-retire
PATCH  /api/admission-charges/{id}/restore                  un-retire
```

`DELETE` never removes a row — it sets `retired = true`. `restore` is rejected with `400` if
another live row now occupies the same `(item, admissionType, paymentMethod)` triple (e.g. a
replacement row was configured while this one was retired) — resolve that conflict first.

---

## Status codes

| Code | When |
|---|---|
| 400 | Validation failure — missing/invalid item, bad `paymentMethod`, negative `price`, duplicate triple, malformed JSON or query param |
| 401 | Missing, unknown or expired `Finance` key |
| 404 | Row does not exist, or is retired and `includeRetired` was not set |
| 409 | Restoring something that is not retired |

---

## Related

- `AdmissionChargeApplicationBean` — runtime resolution and billing (`resolveChargesForEncounter`,
  `resolveForItem`)
- `InwardBhtChargeAggregationService.fetchAdmissionFeeCharges()` — the separate `admissionFee`
  mechanism described in the double-charge trap above
- [API_TIMED_ITEMS.md](API_TIMED_ITEMS.md) — a similarly-shaped configuration API for timed
  (duration-based) inward services
