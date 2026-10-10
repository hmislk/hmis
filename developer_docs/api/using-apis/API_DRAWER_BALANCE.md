# Drawer Balance API

## Overview

This API reads and resets a cashier's drawer balance per payment method, without going through the
UI's one-user-one-method-at-a-time "Adjust Drawer Balance" flow. It exists for QA/E2E test setup
(issue #24433), so automated tests can put a drawer into a known state before exercising a scenario.

- **Endpoints**:
  - `GET /api/drawer/{webUserId}`
  - `POST /api/drawer/adjust`
- **Auth Header**: `Finance: <API_KEY>`
- **Purpose**: Read or reset a cashier's drawer balance for QA/E2E test setup.

> **Audit trail**: `POST /api/drawer/adjust` goes through the exact same `DrawerEntry`/`Bill` audit
> trail as the UI's **Adjust Drawer Balance -> Admin** flow (`DrawerAdjustmentController` +
> `DrawerService.applyDrawerAdjustment`). Every non-zero adjustment creates a `DrawerAdjustment` bill
> (suffix `DRADJ`) and a `DrawerEntry`, and shows up in **My Drawer History** for the target user.

> **Privilege required**: `POST /api/drawer/adjust` requires the API key's resolved user to hold the
> `DrawerAdjustmentDirect` privilege (directly or via their role) — the same privilege the UI's direct
> admin-adjustment flow requires. `GET /api/drawer/{webUserId}` is read-only and has no privilege
> check beyond the `Finance` key, matching the `Balance History` API precedent.

> **Resetting "all payment methods"**: when `paymentMethod` is omitted/null on the POST body, every
> one of the 18 supported payment methods is reset to `targetBalance`. Only methods whose balance
> actually changes get a `Bill`/`DrawerEntry` written — methods already at the target value are
> skipped for the write, but still reported in the response with `"changed": false`, so idempotent
> re-runs do not flood the drawer history with zero-value entries.

### Supported Payment Methods

`OnCall`, `Cash`, `Card`, `MultiplePaymentMethods`, `Staff`, `Credit`, `Staff_Welfare`, `Voucher`,
`IOU`, `Agent`, `Cheque`, `Slip`, `ewallet`, `PatientDeposit`, `PatientPoints`, `OnlineSettlement`,
`None`, `YouOweMe`.

`OnlineBookingAgent` is **not** supported — it is not handled anywhere in `Drawer`/`DrawerService`
and is rejected with `400` if requested.

## GET /api/drawer/{webUserId}

Reads a user's current drawer balance, either for every supported payment method or for a single
one.

- **Endpoint**: `GET /api/drawer/{webUserId}`
- **Authentication**: `Finance: <API_KEY>` header. No additional privilege check.

### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `webUserId` | Long | Yes | The `WebUser` id whose drawer to read. |

### Query Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `paymentMethod` | String | No | One of the supported payment method names (case-sensitive). If omitted, returns balances for every supported payment method. |

If the user has never had a drawer adjustment (no `Drawer` row exists yet), every payment method is
reported as `0.0` rather than a `404` — "never adjusted" is operationally "balance is zero", which
keeps QA scripts simple (no special-casing a brand-new test user).

### Example Request (single payment method)

```bash
curl -s -H "Finance: $FINANCE_KEY" \
  "$BASE_URL/api/drawer/12345?paymentMethod=Cash"
```

### Response

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "webUserId": 12345,
    "paymentMethod": "Cash",
    "balance": 2500.0
  }
}
```

### Example Request (all payment methods)

```bash
curl -s -H "Finance: $FINANCE_KEY" \
  "$BASE_URL/api/drawer/12345"
```

### Response

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "webUserId": 12345,
    "balances": {
      "OnCall": 0.0,
      "Cash": 2500.0,
      "Card": 0.0,
      "MultiplePaymentMethods": 0.0,
      "Staff": 0.0,
      "Credit": 0.0,
      "Staff_Welfare": 0.0,
      "Voucher": 0.0,
      "IOU": 0.0,
      "Agent": 0.0,
      "Cheque": 0.0,
      "Slip": 0.0,
      "ewallet": 0.0,
      "PatientDeposit": 0.0,
      "PatientPoints": 0.0,
      "OnlineSettlement": 0.0,
      "None": 0.0,
      "YouOweMe": 0.0
    }
  }
}
```

### Error Responses

| Code | Condition |
|---|---|
| 400 | Invalid or unsupported `paymentMethod` |
| 401 | Missing/invalid `Finance` API key |
| 404 | `webUserId` does not match any `WebUser` |
| 500 | Unexpected server error |

## POST /api/drawer/adjust

Resets a user's drawer balance for one payment method, or for all supported payment methods, to a
target value. Internally converts the target into a delta (`targetBalance - currentInHandValue`)
and applies it via `DrawerService.applyDrawerAdjustment` — the same method the UI's direct admin
adjustment flow uses.

- **Endpoint**: `POST /api/drawer/adjust`
- **Authentication**: `Finance: <API_KEY>` header, resolved user must hold the
  `DrawerAdjustmentDirect` privilege (directly, or via their role).

### Request Body

| Field | Type | Required | Description |
|---|---|---|---|
| `webUserId` | Long | Yes | The target drawer's owner (`WebUser` id). |
| `paymentMethod` | String | No | One supported payment method name. Omit/null to reset every supported payment method. |
| `targetBalance` | Double | No (defaults to `0.0`) | The balance each selected payment method should end up at. |
| `comment` | String | No | Stored on the adjustment bill's `comments`, tagged with `[<PaymentMethod>]`. Defaults to `"QA/E2E drawer balance reset via API"` when blank. |

The adjustment bill(s) are created with the **target user's** department/institution (there is no
admin session in an API call), numbered with suffix `DRADJ` like the UI flow. The API key's resolved
user — not the target user — is recorded as the creator of each bill and `DrawerEntry` (issue
#24433's "record the API key's user as the creator").

One `Bill` is created per payment method that actually changes (mirrors the UI, where each
adjustment is a single-payment-method transaction) — a multi-method reset is just several of those
back-to-back.

### Example Request (single payment method)

```bash
curl -s -X POST "$BASE_URL/api/drawer/adjust" \
  -H "Finance: $FINANCE_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "webUserId": 12345,
    "paymentMethod": "Cash",
    "targetBalance": 0.0,
    "comment": "Reset before E2E test run #24433"
  }'
```

### Response

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "webUserId": 12345,
    "adjustedBy": "admin_user",
    "adjustments": [
      {
        "paymentMethod": "Cash",
        "before": 2500.0,
        "after": 0.0,
        "changed": true
      }
    ]
  }
}
```

### Example Request (reset all payment methods to zero)

```bash
curl -s -X POST "$BASE_URL/api/drawer/adjust" \
  -H "Finance: $FINANCE_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "webUserId": 12345,
    "targetBalance": 0.0
  }'
```

### Response

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "webUserId": 12345,
    "adjustedBy": "admin_user",
    "adjustments": [
      {"paymentMethod": "OnCall", "before": 0.0, "after": 0.0, "changed": false},
      {"paymentMethod": "Cash", "before": 2500.0, "after": 0.0, "changed": true},
      {"paymentMethod": "Card", "before": 0.0, "after": 0.0, "changed": false},
      {"paymentMethod": "MultiplePaymentMethods", "before": 0.0, "after": 0.0, "changed": false},
      {"paymentMethod": "Staff", "before": 0.0, "after": 0.0, "changed": false},
      {"paymentMethod": "Credit", "before": 0.0, "after": 0.0, "changed": false},
      {"paymentMethod": "Staff_Welfare", "before": 0.0, "after": 0.0, "changed": false},
      {"paymentMethod": "Voucher", "before": 0.0, "after": 0.0, "changed": false},
      {"paymentMethod": "IOU", "before": 0.0, "after": 0.0, "changed": false},
      {"paymentMethod": "Agent", "before": 0.0, "after": 0.0, "changed": false},
      {"paymentMethod": "Cheque", "before": 0.0, "after": 0.0, "changed": false},
      {"paymentMethod": "Slip", "before": 0.0, "after": 0.0, "changed": false},
      {"paymentMethod": "ewallet", "before": 0.0, "after": 0.0, "changed": false},
      {"paymentMethod": "PatientDeposit", "before": 300.0, "after": 0.0, "changed": true},
      {"paymentMethod": "PatientPoints", "before": 0.0, "after": 0.0, "changed": false},
      {"paymentMethod": "OnlineSettlement", "before": 0.0, "after": 0.0, "changed": false},
      {"paymentMethod": "None", "before": 0.0, "after": 0.0, "changed": false},
      {"paymentMethod": "YouOweMe", "before": 0.0, "after": 0.0, "changed": false}
    ]
  }
}
```

Only `Cash` and `PatientDeposit` actually changed here, so only those two got a `Bill`/`DrawerEntry`
written; the rest are reported for completeness with `"changed": false`.

### Error Responses

| Code | Condition |
|---|---|
| 400 | Missing `webUserId`, invalid/unsupported `paymentMethod` |
| 401 | Missing/invalid `Finance` API key |
| 403 | Resolved API user lacks the `DrawerAdjustmentDirect` privilege |
| 404 | `webUserId` does not match any `WebUser` |
| 500 | Unexpected server error |
