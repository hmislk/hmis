# AF15 Daily Stock Values Report API

## Overview

The AF15 Report API exposes the same pharmacy daily stock balance report that appears at:
`/pharmacy/reports/summary_reports/daily_stock_values_report_optimized.xhtml`

Both the UI and the API call the **same underlying service methods** (`PharmacyService` + `StockHistoryFacade`),
so any bug fix to those services automatically applies to both.

AI agents can use this API to:
1. Generate the full F15 report for any date and department
2. Detect balance discrepancies using the built-in `balanceCheck` field
3. Drill into specific bills to find the root cause
4. Propose corrections and apply them after human approval

---

## Authentication

> **AI Agent Instructions:** At the start of every session, before making any API call,
> ask the user for the following two values:
> 1. **Base URL** — the root URL of the HMIS instance, e.g. `https://hospital.example.com/rh`
> 2. **Finance API key** — the value to pass in the `Finance` HTTP header
>
> These values differ per deployment and must **never** be hardcoded.

All endpoints require the `Finance` header:
```
Finance: <ask the user for their Finance API key>
```

Base URL: `<ask the user for their HMIS base URL>`/api

---

## Endpoint 1: Generate F15 Report

```
GET /api/pharmacy_f15_report?date=yyyy-MM-dd&departmentId=<long>
```

### Parameters

| Parameter    | Required | Description                          | Example        |
|-------------|----------|--------------------------------------|----------------|
| date        | Yes      | Report date                          | 2026-02-18     |
| departmentId| Yes      | Department ID (from institution table)| 485           |

### Example Request

```bash
# Set BASE_URL and FINANCE_KEY from user input before running
curl -s "$BASE_URL/api/pharmacy_f15_report?date=2026-02-18&departmentId=485" \
  -H "Finance: $FINANCE_KEY"
```

### Response Structure

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "date": "2026-02-18",
    "departmentId": 485,
    "departmentName": "Main Pharmacy",

    "openingStock": {
      "retailRate": 44160559.31,
      "costRate": 37077769.56
    },

    "sales": {
      "rows": [
        {
          "type": "DIRECT_ISSUE_INWARD_MEDICINE - BHT",
          "grossTotal": 181887.78,
          "discount": 0.0,
          "serviceCharge": 55745.48,
          "netTotal": 237633.26,
          "stockValueAtCostRate": -146391.42,
          "stockValueAtRetailRate": -181887.78
        },
        "... more rows ..."
      ],
      "totals": {
        "type": null,
        "grossTotal": 756601.82,
        "discount": 2352.89,
        "serviceCharge": 57130.17,
        "netTotal": 811379.09,
        "stockValueAtCostRate": -676260.87,
        "stockValueAtRetailRate": -800657.98
      }
    },

    "purchases": {
      "rows": [ "... PHARMACY_GRN, PHARMACY_DIRECT_PURCHASE ..." ],
      "totals": {
        "stockValueAtCostRate": 2061552.32,
        "stockValueAtRetailRate": 2487780.04,
        "..."
      }
    },

    "transfers": {
      "rows": [ "... PHARMACY_ISSUE, PHARMACY_RECEIVE, PHARMACY_DIRECT_ISSUE ..." ],
      "totals": {
        "stockValueAtCostRate": -822709.11,
        "stockValueAtRetailRate": -971720.90,
        "..."
      }
    },

    "adjustments": {
      "rows": [ "... PHARMACY_PURCHASE_RATE_ADJUSTMENT, PHARMACY_RETAIL_RATE_ADJUSTMENT ..." ],
      "totals": {
        "stockValueAtCostRate": 0.0,
        "stockValueAtRetailRate": -62422.21,
        "..."
      }
    },

    "closingStock": {
      "retailRate": 44813825.93,
      "costRate": 37599181.64
    },

    "balanceCheck": {
      "expectedClosingAtRetailRate": 44813538.26,
      "actualClosingAtRetailRate": 44813825.93,
      "discrepancyAtRetailRate": 287.67,
      "balanced": false,
      "toleranceUsed": 1.0,
      "formula": "expectedClosing = opening(44160559.31) + sales(-800657.98) + purchases(2487780.04) + transfers(-971720.90) + adjustments(-62422.21) = 44813538.26 | actual=44813825.93 | discrepancy=287.67"
    }
  }
}
```

### Sign Convention for stockValueAtRetailRate

| Transaction Type     | Sign     | Meaning                          |
|---------------------|----------|----------------------------------|
| Sales               | Negative | Stock flowing OUT of pharmacy    |
| Sale Returns        | Positive | Stock flowing BACK IN            |
| Purchases (GRN)     | Positive | Stock flowing INTO pharmacy      |
| Purchase Returns    | Negative | Stock flowing OUT to supplier    |
| PHARMACY_ISSUE      | Negative | Stock issued OUT to ward         |
| PHARMACY_RECEIVE    | Positive | Stock received FROM another dept |
| PHARMACY_DIRECT_ISSUE| Negative| Direct issue OUT                 |
| Rate Adjustments    | Zero     | No stock quantity change         |
| Stock Adjustments   | Signed   | Depends on direction             |

### Balance Formula

```
expectedClosing = opening
                + sales.totals.stockValueAtRetailRate        (negative)
                + purchases.totals.stockValueAtRetailRate    (positive)
                + transfers.totals.stockValueAtRetailRate    (net signed)
                + adjustments.totals.stockValueAtRetailRate  (net signed)

discrepancy = actualClosing - expectedClosing
```

Verified on production data (2026-02-18, dept 485):
- Expected: 44,813,538.26
- Actual:   44,813,825.93
- **Real discrepancy: +287.67**

---

## Endpoint 2: List Bills by Type (Drill-Down)

When a discrepancy is found, use this to list all bills of a specific type for investigation.

```
GET /api/costing_data/bills_by_type
```

### Parameters

| Parameter      | Required | Description                          | Example                      |
|---------------|----------|--------------------------------------|------------------------------|
| fromDate      | Yes      | Start datetime                       | 2026-02-18 00:00:00          |
| toDate        | Yes      | End datetime                         | 2026-02-18 23:59:59          |
| departmentId  | Yes      | Department ID                        | 485                          |
| billTypeAtomic| Yes      | Bill type enum name                  | PHARMACY_RETAIL_SALE         |
| limit         | No       | Max results (default 200, max 1000)  | 50                           |

### Example Request

```bash
# Set BASE_URL and FINANCE_KEY from user input before running
curl -s "$BASE_URL/api/costing_data/bills_by_type\
?fromDate=2026-02-18%2000:00:00\
&toDate=2026-02-18%2023:59:59\
&departmentId=485\
&billTypeAtomic=PHARMACY_GRN" \
  -H "Finance: $FINANCE_KEY"
```

### Response

```json
{
  "status": "success",
  "code": 200,
  "count": 27,
  "departmentId": 485,
  "billTypeAtomic": "PHARMACY_GRN",
  "fromDate": "2026-02-18 00:00:00",
  "toDate": "2026-02-18 23:59:59",
  "data": [
    {
      "billId": 1234567,
      "billNumber": "MP/GRN/26/000089",
      "billType": "PharmacyBill",
      "billTypeAtomic": "PHARMACY_GRN",
      "billTime": "2026-02-18 09:23:41",
      "netTotal": -76120.50,
      "grossTotal": 0.0,
      "retired": false,
      "completed": true,
      "stockValueAtRetailRate": 91800.00,
      "stockValueAtCostRate": 76120.50,
      "stockValueAtPurchaseRate": 76120.50
    }
  ]
}
```

---

## Endpoint 3: Get Full Bill Details (Existing)

Use to inspect specific bill items and pharmaceutical bill item data.

```
GET /api/costing_data/by_bill_id/{billId}
```

Response includes:
- `billItems[]` with `billItemFinanceDetails` (costRate, retailSaleRate, valueAtRetailRate, etc.)
- `billItems[].pharmaceuticalBillItem` (qty, retailRate, costRate, retailValue, costValue)

---

## AI Agent Investigation Workflow

### Step 1: Generate F15 Report

```python
import requests

# ── Session Setup ──────────────────────────────────────────────────────────
# At the very start of every session, ask the user for these two values.
# They differ per deployment and must never be hardcoded.
BASE_URL = input("Enter the HMIS base URL (e.g. https://hospital.example.com/rh): ").strip().rstrip("/")
API_KEY  = input("Enter your Finance API key: ").strip()
HEADERS  = {"Finance": API_KEY}
# ──────────────────────────────────────────────────────────────────────────

def get_f15_report(date_str, department_id):
    r = requests.get(
        f"{BASE_URL}/api/pharmacy_f15_report",
        params={"date": date_str, "departmentId": department_id},
        headers=HEADERS
    )
    return r.json()["data"]

report = get_f15_report("2026-02-18", 485)
check = report["balanceCheck"]

print(f"Department: {report['departmentName']}")
print(f"Date: {report['date']}")
print(f"Opening (Retail): {report['openingStock']['retailRate']:,.2f}")
print(f"Closing (Retail): {report['closingStock']['retailRate']:,.2f}")
print(f"Discrepancy: {check['discrepancyAtRetailRate']:,.2f}")
print(f"Balanced: {check['balanced']}")
```

### Step 2: Identify Suspect Bundle

```python
if not check["balanced"]:
    discrepancy = check["discrepancyAtRetailRate"]
    print(f"\nDISCREPANCY DETECTED: {discrepancy:,.2f}")

    # Show all bundle totals
    for bundle_name in ["sales", "purchases", "transfers", "adjustments"]:
        bundle = report[bundle_name]
        total_retail = bundle["totals"]["stockValueAtRetailRate"]
        count = len(bundle["rows"])
        print(f"  {bundle_name}: {total_retail:,.2f} ({count} row types)")
        for row in bundle["rows"]:
            print(f"    {row['type']}: retail={row['stockValueAtRetailRate']:,.2f}")
```

### Step 3: Drill Into Bills

```python
def get_bills_by_type(from_date, to_date, department_id, bill_type_atomic, limit=200):
    r = requests.get(
        f"{BASE_URL}/api/costing_data/bills_by_type",
        params={
            "fromDate": from_date,
            "toDate": to_date,
            "departmentId": department_id,
            "billTypeAtomic": bill_type_atomic,
            "limit": limit
        },
        headers=HEADERS
    )
    return r.json()

# Example: investigate GRN bills
grn_bills = get_bills_by_type(
    "2026-02-18 00:00:00", "2026-02-18 23:59:59",
    485, "PHARMACY_GRN"
)
print(f"\nGRN bills found: {grn_bills['count']}")
for b in grn_bills["data"]:
    print(f"  Bill {b['billId']} ({b['billNumber']}): "
          f"stockRetail={b.get('stockValueAtRetailRate', 'N/A'):,.2f}")
```

### Step 4: Inspect Individual Bill

```python
def get_bill_detail(bill_id):
    r = requests.get(
        f"{BASE_URL}/api/costing_data/by_bill_id/{bill_id}",
        headers=HEADERS
    )
    return r.json()["data"]

bill = get_bill_detail(1234567)
print(f"\nBill {bill['id']} - {bill['deptId']}")
for item in bill.get("billItems", []):
    bfd = item.get("billItemFinanceDetails", {})
    pbi = item.get("pharmaceuticalBillItem", {})
    print(f"  Item {item['id']}:")
    print(f"    qty={item['qty']}, rate={item['rate']}")
    print(f"    BillItemFinanceDtl: valueAtRetail={bfd.get('valueAtRetailRate')}, "
          f"costRate={bfd.get('costRate')}")
    if pbi:
        print(f"    PharmaceuticalBillItem: qty={pbi.get('qty')}, "
              f"retailRate={pbi.get('retailRate')}, retailValue={pbi.get('retailValue')}")
```

### Step 5: Cross-Check for Inconsistency

```python
def check_bill_item_consistency(bill):
    """Find bill items where BillItemFinanceDetails doesn't match PharmaceuticalBillItem."""
    issues = []
    for item in bill.get("billItems", []):
        bfd = item.get("billItemFinanceDetails")
        pbi = item.get("pharmaceuticalBillItem")
        if not bfd or not pbi:
            continue
        bfd_retail = bfd.get("valueAtRetailRate") or 0
        pbi_retail = pbi.get("retailValue") or 0
        diff = abs(bfd_retail - pbi_retail)
        if diff > 0.01:
            issues.append({
                "billItemId": item["id"],
                "bfdValueAtRetail": bfd_retail,
                "pbiRetailValue": pbi_retail,
                "difference": pbi_retail - bfd_retail
            })
    return issues
```

### Step 6: Report to Developer and Get Approval

```python
# Present findings as text to developer
def generate_discrepancy_report(date_str, dept_id):
    report = get_f15_report(date_str, dept_id)
    check = report["balanceCheck"]

    lines = [
        f"=== F15 Discrepancy Report ===",
        f"Date: {date_str}",
        f"Department: {report['departmentName']} (ID: {dept_id})",
        f"",
        f"Opening Stock (Retail): {report['openingStock']['retailRate']:,.2f}",
        f"Closing Stock (Retail): {report['closingStock']['retailRate']:,.2f}",
        f"Expected Closing:       {check['expectedClosingAtRetailRate']:,.2f}",
        f"DISCREPANCY:            {check['discrepancyAtRetailRate']:+,.2f}",
        f"",
        f"Transaction Breakdown:",
    ]
    for bundle in ["sales", "purchases", "transfers", "adjustments"]:
        total = report[bundle]["totals"]["stockValueAtRetailRate"]
        lines.append(f"  {bundle.capitalize()}: {total:+,.2f}")
        for row in report[bundle]["rows"]:
            lines.append(f"    {row['type']}: {row['stockValueAtRetailRate']:+,.2f}")

    print("\n".join(lines))
    # Present to developer for review → await approval → apply corrections
```

### Step 7: Apply Corrections (After Approval)

For stock-level corrections (quantity/rate), use the existing adjustment APIs:

```bash
# Set BASE_URL, FINANCE_KEY, and DEPT_ID from user input before running

# Adjust stock quantity
curl -s -X POST "$BASE_URL/api/pharmacy_adjustments/stock_quantity" \
  -H "Finance: $FINANCE_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "stockId": <id>,
    "newQuantity": <corrected_qty>,
    "comment": "F15 discrepancy correction - approved by <name> on <date>",
    "departmentId": <departmentId>
  }'

# Adjust purchase rate
curl -s -X POST "$BASE_URL/api/pharmacy_adjustments/purchase_rate" \
  -H "Finance: $FINANCE_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "stockId": <id>,
    "newPurchaseRate": <correct_rate>,
    "comment": "F15 discrepancy correction - approved by <name> on <date>",
    "departmentId": <departmentId>
  }'
```

For bill-level data corrections (wrong `BillItemFinanceDetails` values), these require
developer action — a future correction API will be added once specific root causes are identified.

---

## Valid billTypeAtomic Values

### Sales
```
PHARMACY_RETAIL_SALE, PHARMACY_RETAIL_SALE_CANCELLED, PHARMACY_RETAIL_SALE_REFUND,
PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS, PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER,
PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY, PHARMACY_WHOLESALE, PHARMACY_WHOLESALE_CANCELLED,
PHARMACY_WHOLESALE_PRE, PHARMACY_WHOLESALE_REFUND, DIRECT_ISSUE_INWARD_MEDICINE,
DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION, DIRECT_ISSUE_INWARD_MEDICINE_RETURN,
DIRECT_ISSUE_THEATRE_MEDICINE, DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION,
DIRECT_ISSUE_THEATRE_MEDICINE_RETURN, ISSUE_MEDICINE_ON_REQUEST_INWARD,
ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION, ISSUE_MEDICINE_ON_REQUEST_THEATRE,
ISSUE_MEDICINE_ON_REQUEST_THEATRE_CANCELLATION, ACCEPT_RETURN_MEDICINE_INWARD,
ACCEPT_RETURN_MEDICINE_THEATRE
```

### Purchases
```
PHARMACY_DIRECT_PURCHASE, PHARMACY_DIRECT_PURCHASE_REFUND, PHARMACY_DIRECT_PURCHASE_CANCELLED,
PHARMACY_GRN, PHARMACY_RETURN_WITHOUT_TREASING, PHARMACY_GRN_RETURN, PHARMACY_GRN_CANCELLED
```

### Transfers
```
PHARMACY_ISSUE, PHARMACY_RECEIVE, PHARMACY_DIRECT_ISSUE, PHARMACY_DIRECT_ISSUE_CANCELLED,
PHARMACY_DISPOSAL_ISSUE, PHARMACY_DISPOSAL_ISSUE_CANCELLED, PHARMACY_DISPOSAL_ISSUE_RETURN,
PHARMACY_ISSUE_CANCELLED, PHARMACY_ISSUE_RETURN, PHARMACY_RECEIVE_CANCELLED
```

### Adjustments
```
PHARMACY_PURCHASE_RATE_ADJUSTMENT, PHARMACY_RETAIL_RATE_ADJUSTMENT,
PHARMACY_COST_RATE_ADJUSTMENT, PHARMACY_WHOLESALE_RATE_ADJUSTMENT,
PHARMACY_STOCK_ADJUSTMENT, PHARMACY_ADJUSTMENT, PHARMACY_ADJUSTMENT_CANCELLED
```

---

## Known Production Discrepancy (2026-02-18, Main Pharmacy)

As of the initial implementation, a discrepancy of **+287.67** was detected:

```
Opening:      44,160,559.31
+ Sales:        -800,657.98
+ Purchases:  +2,487,780.04
+ Transfers:    -971,720.90
+ Adjustments:   -62,422.21
─────────────────────────────
Expected:     44,813,538.26
Actual:       44,813,825.93
Discrepancy:       +287.67  ← needs investigation
```

Next step: Run the agent workflow above to identify which specific bills/items
contribute to this 287.67 difference.
