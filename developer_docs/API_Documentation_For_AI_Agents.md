# Pharmacy Stock Management API Documentation for AI Agents

## Overview

This document provides comprehensive guidance for AI agents to interact with the Pharmacy Stock Management API system. The API consists of three main components:

1. **Search/Lookup APIs** - Find stocks, departments, and items using human-readable criteria
2. **Adjustment APIs** - Modify stock quantities, retail rates, and expiry dates
3. **Batch Creation APIs** - Create new AMPs (items) and ItemBatches with Stock entries

## Base Configuration

- **Base URL**: `http://localhost:8080` (adjust for your environment)
- **API Base Paths**:
  - `/api/pharmacy_adjustments` (Stock adjustments)
  - `/api/pharmacy_batches` (Batch creation and AMP management)
- **Authentication**: API Key via `Finance` header
- **Content Type**: `application/json`
- **Date Format**: `yyyy-MM-dd` for input, `yyyy-MM-dd HH:mm:ss` for responses

## Authentication

All API calls require a valid API key in the request header:

```bash
-H "Finance: YOUR_API_KEY"
```

## Search/Lookup APIs

### 1. Department Search

**Purpose**: Find departments by name to get department IDs

**Endpoint**: `GET /api/pharmacy_adjustments/search/departments`

**Parameters**:
- `query` (required): Department name search term
- `limit` (optional): Result limit (default: 20, max: 50)

**Example Request**:
```bash
GET /api/pharmacy_adjustments/search/departments?query=Pharmacy&limit=10
```

**Example Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "Main Pharmacy",
      "code": "PHARM01"
    },
    {
      "id": 2,
      "name": "Emergency Pharmacy",
      "code": "PHARM02"
    }
  ]
}
```

### 2. Stock Search

**Purpose**: Find stocks using comprehensive criteria

**Endpoint**: `GET /api/pharmacy_adjustments/search/stocks`

**Required Parameters**:
- `query` (required): Item name, code, or barcode
- `department` (required): Department name (exact match)

**Optional Filters**:
- `minQuantity` / `maxQuantity`: Quantity range filters
- `minRetailRate` / `maxRetailRate`: Retail rate range filters
- `minCostRate` / `maxCostRate`: Cost rate range filters
- `minPurchaseRate` / `maxPurchaseRate`: Purchase rate range filters
- `expiryAfter` / `expiryBefore`: Expiry date filters (yyyy-MM-dd)
- `batchNo`: Batch number filter
- `includeZeroStock`: Include zero quantity stocks (default: false)
- `limit`: Result limit (default: 30, max: 100)

**Example Request**:
```bash
GET /api/pharmacy_adjustments/search/stocks?query=Paracetamol&department=Main%20Pharmacy&minQuantity=1&expiryAfter=2025-01-01
```

**Example Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 123,
      "stockId": 456,
      "itemBatchId": 789,
      "itemName": "Paracetamol 500mg",
      "code": "PAR001",
      "batchNo": "B001234",
      "retailRate": 25.00,
      "stockQty": 150.0,
      "dateOfExpire": "2025-12-31 00:00:00",
      "purchaseRate": 20.00,
      "wholesaleRate": 22.50,
      "costRate": 21.00,
      "allowFractions": false
    }
  ]
}
```

### 3. Item Search

**Purpose**: Find items by name, code, or barcode

**Endpoint**: `GET /api/pharmacy_adjustments/search/items`

**Parameters**:
- `query` (required): Item name, code, or barcode
- `limit` (optional): Result limit (default: 30, max: 50)

**Example Request**:
```bash
GET /api/pharmacy_adjustments/search/items?query=Para&limit=20
```

**Example Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "Paracetamol 500mg",
      "code": "PAR001",
      "barcode": "1234567890123",
      "genericName": "Paracetamol"
    }
  ]
}
```

## Adjustment APIs

### 1. Stock Quantity Adjustment

**Purpose**: Change stock quantities

**Endpoint**: `POST /api/pharmacy_adjustments/stock_quantity`

**Request Body**:
```json
{
  "stockId": 456,
  "newQuantity": 150.0,
  "comment": "Physical count adjustment",
  "departmentId": 1
}
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "billId": 54321,
    "billNumber": "ADJ/2025/000123",
    "stockId": 456,
    "stockType": "QUANTITY",
    "beforeValue": 100.0,
    "afterValue": 150.0,
    "comment": "Physical count adjustment",
    "adjustmentDate": "2025-01-03 14:30:00"
  }
}
```

### 2. Retail Rate Adjustment

**Purpose**: Change item retail rates

**Endpoint**: `POST /api/pharmacy_adjustments/retail_rate`

**Request Body**:
```json
{
  "stockId": 456,
  "newRetailRate": 30.00,
  "comment": "Market price adjustment",
  "departmentId": 1
}
```

### 3. Expiry Date Adjustment

**Purpose**: Change batch expiry dates

**Endpoint**: `POST /api/pharmacy_adjustments/expiry_date`

**Request Body**:
```json
{
  "stockId": 456,
  "newExpiryDate": "2026-12-31",
  "comment": "Corrected supplier information",
  "departmentId": 1
}
```

### 4. Purchase Rate Adjustment

**Purpose**: Change item purchase rates (cost price)

**Endpoint**: `POST /api/pharmacy_adjustments/purchase_rate`

**Request Body**:
```json
{
  "stockId": 456,
  "newPurchaseRate": 85.00,
  "comment": "Supplier price update",
  "departmentId": 1
}
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "billId": 54321,
    "billNumber": "ADJ/2025/000123",
    "stockId": 456,
    "stockType": "PURCHASE_RATE",
    "beforeValue": 80.00,
    "afterValue": 85.00,
    "comment": "Supplier price update",
    "adjustmentDate": "2025-01-03 14:30:00"
  }
}
```

**Validation Rules**:
- `stockId`: Required, must be a valid stock ID
- `newPurchaseRate`: Required, cannot be negative
- `comment`: Required, cannot be empty
- `departmentId`: Required, must be a valid department ID

## Batch Creation APIs

### 1. AMP Search or Create

**Purpose**: Search for AMP (Actual Medicinal Product) by name, create if not found

**Endpoint**: `POST /api/pharmacy_batches/amp/search_or_create`

**Request Body**:
```json
{
  "name": "Paracetamol Tablet 500mg",
  "genericName": "Paracetamol",
  "categoryId": null,
  "dosageFormId": null
}
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 1234,
    "name": "Paracetamol Tablet 500mg",
    "code": "paracetamol_tablet_500mg",
    "genericName": "Paracetamol",
    "categoryName": "Pain Relief",
    "created": true
  }
}
```

**Key Features**:
- Auto-generates code from name (removes spaces, converts to lowercase with underscores)
- Uses any available VMP if genericName not specified
- Returns `created: true` if newly created, `false` if found existing

### 2. Batch Creation with Stock

**Purpose**: Create new ItemBatch and corresponding Stock entry for a department

**Endpoint**: `POST /api/pharmacy_batches/create`

**Request Body**:
```json
{
  "itemId": 1234,
  "batchNo": "BATCH001",
  "expiryDate": "2025-12-31 00:00:00",
  "retailRate": 100.0,
  "purchaseRate": 85.0,
  "costRate": null,
  "wholesaleRate": 90.0,
  "departmentId": 456,
  "comment": "Initial stock creation"
}
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "batchId": 5678,
    "stockId": 9012,
    "batchNo": "BATCH001",
    "item": {
      "id": 1234,
      "name": "Paracetamol Tablet 500mg",
      "code": "paracetamol_tablet_500mg",
      "genericName": "Paracetamol",
      "categoryName": "Pain Relief",
      "created": false
    },
    "departmentName": "Main Pharmacy",
    "retailRate": 100.0,
    "purchaseRate": 85.0,
    "costRate": 85.0,
    "expiryDate": "2025-12-31 00:00:00",
    "message": "Created new batch"
  }
}
```

**Rate Calculation Defaults**:
- **Purchase Rate**: If null, defaults to 85% of retail rate
- **Cost Rate**: If null, defaults to purchase rate
- **Wholesale Rate**: Optional, no default

**Batch Number Generation**:
- If `batchNo` not provided, auto-generates using "B" + timestamp

**Duplicate Handling**:
- If ItemBatch exists (same Item + BatchNo + ExpiryDate), uses existing batch
- Always creates new Stock entry for the specified department
- Message indicates whether batch was "Created new batch" or "Used existing batch"

### 3. AMP Search Only

**Purpose**: Search existing AMPs by name without creating new ones

**Endpoint**: `GET /api/pharmacy_batches/amp/search`

**Parameters**:
- `name` (required): AMP name search term
- `limit` (optional): Result limit (default: 30, max: 50)

**Example Request**:
```bash
GET /api/pharmacy_batches/amp/search?name=Paracetamol&limit=10
```

**Example Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 1234,
      "name": "Paracetamol Tablet 500mg",
      "code": "paracetamol_tablet_500mg",
      "genericName": "Paracetamol",
      "categoryName": "Pain Relief",
      "created": false
    },
    {
      "id": 1235,
      "name": "Paracetamol Syrup 120mg/5ml",
      "code": "paracetamol_syrup_120mg_5ml",
      "genericName": "Paracetamol",
      "categoryName": "Pain Relief",
      "created": false
    }
  ]
}
```

## Batch Creation Workflow Examples

### Example 1: Complete New Item and Batch Creation

**Scenario**: Create "Amoxicillin 250mg Capsule" with initial batch

```bash
# Step 1: Create AMP (will auto-create if not found)
curl -X POST "http://localhost:8080/api/pharmacy_batches/amp/search_or_create" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "name": "Amoxicillin 250mg Capsule",
    "genericName": "Amoxicillin"
  }'

# Response: { "id": 2001, "created": true, ... }

# Step 2: Create batch using the returned AMP ID
curl -X POST "http://localhost:8080/api/pharmacy_batches/create" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "itemId": 2001,
    "batchNo": "AMX2025001",
    "expiryDate": "2026-06-30 00:00:00",
    "retailRate": 50.0,
    "departmentId": 1,
    "comment": "New medication stock creation"
  }'

# Auto-calculated: purchaseRate = 42.5 (85% of 50.0), costRate = 42.5
```

### Example 2: Batch Creation with Rate Defaults

**Scenario**: Minimal input, rely on defaults

```bash
# Minimum required fields
curl -X POST "http://localhost:8080/api/pharmacy_batches/create" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "itemId": 1234,
    "expiryDate": "2025-12-31 00:00:00",
    "retailRate": 100.0,
    "departmentId": 456
  }'

# System will:
# - Auto-generate batchNo (e.g., "B1704723456789")
# - Calculate purchaseRate = 85.0 (85% of 100.0)
# - Set costRate = 85.0 (equals purchaseRate)
# - Create Stock with 0 quantity
```

### Example 3: Duplicate Batch Handling

**Scenario**: Creating batch that already exists

```bash
# Create initial batch
curl -X POST "http://localhost:8080/api/pharmacy_batches/create" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "itemId": 1234,
    "batchNo": "EXISTING001",
    "expiryDate": "2025-12-31 00:00:00",
    "retailRate": 100.0,
    "departmentId": 1
  }'

# Later, create same batch for different department
curl -X POST "http://localhost:8080/api/pharmacy_batches/create" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "itemId": 1234,
    "batchNo": "EXISTING001",
    "expiryDate": "2025-12-31 00:00:00",
    "retailRate": 100.0,
    "departmentId": 2
  }'

# Response: message: "Used existing batch"
# Creates new Stock entry for department 2
```

### Example 4: AI Agent Batch Creation Pattern

```python
class PharmacyBatchAI:
    def create_new_item_with_batch(self, item_name, expiry_date, retail_rate,
                                   department_query="Pharmacy", **options):
        """Complete workflow: Create AMP and batch in one operation"""

        # Step 1: Create or find AMP
        amp_result = self.search_or_create_amp({
            "name": item_name,
            "genericName": options.get("generic_name")
        })

        if not amp_result["success"]:
            return {"error": f"Failed to create AMP: {amp_result['error']}"}

        amp = amp_result["data"]
        print(f"✅ AMP {'created' if amp['created'] else 'found'}: {amp['name']}")

        # Step 2: Find department
        dept_result = self.search_departments(department_query)
        if not dept_result["success"] or not dept_result["data"]:
            return {"error": "Department not found"}

        department = dept_result["data"][0]

        # Step 3: Create batch with defaults
        batch_data = {
            "itemId": amp["id"],
            "expiryDate": expiry_date,
            "retailRate": retail_rate,
            "departmentId": department["id"],
            "comment": options.get("comment", f"AI batch creation - {item_name}")
        }

        # Add optional fields
        if options.get("batch_no"):
            batch_data["batchNo"] = options["batch_no"]
        if options.get("purchase_rate"):
            batch_data["purchaseRate"] = options["purchase_rate"]
        if options.get("cost_rate"):
            batch_data["costRate"] = options["cost_rate"]
        if options.get("wholesale_rate"):
            batch_data["wholesaleRate"] = options["wholesale_rate"]

        batch_result = self.create_batch(batch_data)

        if batch_result["success"]:
            batch = batch_result["data"]
            return {
                "success": True,
                "amp": {
                    "id": amp["id"],
                    "name": amp["name"],
                    "code": amp["code"],
                    "created": amp["created"]
                },
                "batch": {
                    "id": batch["batchId"],
                    "number": batch["batchNo"],
                    "expiry": batch["expiryDate"],
                    "rates": {
                        "retail": batch["retailRate"],
                        "purchase": batch["purchaseRate"],
                        "cost": batch["costRate"]
                    }
                },
                "stock": {
                    "id": batch["stockId"],
                    "department": batch["departmentName"],
                    "quantity": 0.0
                },
                "message": batch["message"]
            }
        else:
            return batch_result

    def bulk_create_batches(self, items_list):
        """Create multiple batches efficiently"""
        results = []

        for item_spec in items_list:
            print(f"\n🔄 Processing: {item_spec['name']}")

            result = self.create_new_item_with_batch(
                item_name=item_spec["name"],
                expiry_date=item_spec["expiry_date"],
                retail_rate=item_spec["retail_rate"],
                **item_spec.get("options", {})
            )

            results.append({
                "item": item_spec["name"],
                "result": result
            })

            if result.get("success"):
                print(f"  ✅ Success: Batch {result['batch']['number']}")
            else:
                print(f"  ❌ Failed: {result.get('error')}")

        return results

# Example usage
if __name__ == "__main__":
    ai = PharmacyBatchAI("http://localhost:8080", "YOUR_API_KEY")

    # Single item creation
    result = ai.create_new_item_with_batch(
        item_name="Metformin 500mg Tablet",
        expiry_date="2026-03-31 00:00:00",
        retail_rate=25.0,
        generic_name="Metformin",
        batch_no="MET2025001",
        comment="Initial diabetes medication stock"
    )

    # Bulk creation
    bulk_items = [
        {
            "name": "Aspirin 300mg Tablet",
            "expiry_date": "2026-01-31 00:00:00",
            "retail_rate": 15.0,
            "options": {"batch_no": "ASP2025001"}
        },
        {
            "name": "Ibuprofen 400mg Tablet",
            "expiry_date": "2026-02-28 00:00:00",
            "retail_rate": 20.0,
            "options": {"generic_name": "Ibuprofen"}
        }
    ]

    bulk_results = ai.bulk_create_batches(bulk_items)
```

## Complete Workflow for AI Agents

### Standard Process: Search → Verify → Adjust

#### Step 1: Find Department
```bash
GET /api/pharmacy_adjustments/search/departments?query=Pharmacy
```
**Extract**: `departmentId` and `departmentName` from response

#### Step 2: Search for Stock
```bash
GET /api/pharmacy_adjustments/search/stocks?query=ITEM_NAME&department=DEPARTMENT_NAME&minQuantity=1
```
**Extract**: `stockId`, `currentQuantity`, `currentRate`, `expiryDate` from response

#### Step 3: Handle Multiple Matches (AI Logic)
- **Single Match**: Proceed with adjustment
- **Multiple Matches**: Present options to user for selection
- **No Matches**: Retry with broader search terms

#### Step 4: Make Adjustment
```bash
POST /api/pharmacy_adjustments/stock_quantity
# or retail_rate or expiry_date
```
**Extract**: `billId`, `beforeValue`, `afterValue` for confirmation

## AI Agent Implementation Guidelines

### 1. Error Handling

**Common Error Responses**:
```json
{
  "status": "error",
  "code": 400,
  "message": "Query parameter is required"
}
```

**Error Codes**:
- `400`: Bad request (invalid parameters, missing fields)
- `401`: Unauthorized (invalid API key)
- `404`: Not found (no results, invalid stock/department)
- `500`: Server error (database issues, system errors)

**AI Error Recovery Strategy**:
1. Retry with different search terms if no results
2. Validate API key if getting 401 errors
3. Check parameter formats for 400 errors
4. Implement exponential backoff for 500 errors

### 2. Search Strategy

**Fuzzy Search Approach**:
1. Start with exact item name
2. Try partial matches (first few characters)
3. Try code-based search if available
4. Use broader department search if specific fails

**Example Search Evolution**:
```bash
# Try 1: Exact name
GET /search/stocks?query=Zaart 50mg tablet&department=Main Pharmacy

# Try 2: Partial name
GET /search/stocks?query=Zaart&department=Main Pharmacy

# Try 3: Different department
GET /search/stocks?query=Zaart&department=Pharmacy
```

### 3. Multiple Match Handling

**When Multiple Stocks Found**:
```python
def present_stock_options_to_user(stocks):
    """Present stock options when multiple matches found"""
    print(f"Found {len(stocks)} matching stocks. Please select one:")
    print("-" * 80)

    for i, stock in enumerate(stocks, 1):
        expiry_status = get_expiry_status(stock['dateOfExpire'])
        print(f"{i}. {stock['itemName']}")
        print(f"   Batch: {stock['batchNo']} | Qty: {stock['stockQty']}")
        print(f"   Expiry: {stock['dateOfExpire'][:10]} {expiry_status}")
        print(f"   Rates: Retail={stock['retailRate']}, Cost={stock['costRate']}")
        print(f"   Stock ID: {stock['stockId']}")
        print()

    while True:
        try:
            choice = int(input(f"Select option (1-{len(stocks)}): ")) - 1
            if 0 <= choice < len(stocks):
                return stocks[choice]
            else:
                print("Invalid selection. Please try again.")
        except ValueError:
            print("Please enter a valid number.")

def get_expiry_status(expiry_date_str):
    """Get expiry status indicator"""
    from datetime import datetime, timedelta

    expiry_date = datetime.strptime(expiry_date_str[:10], '%Y-%m-%d')
    now = datetime.now()

    if expiry_date < now:
        return "⚠️ EXPIRED"
    elif expiry_date < now + timedelta(days=90):
        return "⏰ EXPIRING SOON"
    else:
        return "✅ VALID"
```

### 4. Flexible Validation Logic

**Simplified Validation (No Current Stock Required)**:
```python
def validate_stock_for_adjustment(stock_data, expected_criteria):
    """Validate stock matches expected criteria (flexible matching)"""
    checks = {
        'name_match': True,      # Always true - user confirmed selection
        'expiry_match': True,    # Validate only if specified
        'department_match': True # Ensured by search department filter
    }

    # Optional expiry validation only if specified
    if expected_criteria.get('expiry_date'):
        stock_expiry = stock_data['dateOfExpire'][:10]
        checks['expiry_match'] = stock_expiry == expected_criteria['expiry_date']

    # Optional batch validation only if specified
    if expected_criteria.get('batch_number'):
        checks['batch_match'] = expected_criteria['batch_number'] in stock_data['batchNo']

    return all(checks.values()), checks
```

### 4. Response Processing

**Key Data Extraction**:
```python
# From stock search response
stock_id = response['data'][0]['stockId']
item_batch_id = response['data'][0]['itemBatchId']
current_quantity = response['data'][0]['stockQty']
current_rate = response['data'][0]['retailRate']
expiry_date = response['data'][0]['dateOfExpire'][:10]

# From department search response
department_id = response['data'][0]['id']
department_name = response['data'][0]['name']

# From adjustment response
bill_id = response['data']['billId']
bill_number = response['data']['billNumber']
before_value = response['data']['beforeValue']
after_value = response['data']['afterValue']
```

## Practical Examples

### Example 1: Quantity Adjustment (No Current Stock Verification)

**Scenario**: Adjust "Zaart 50mg tablet" to 50 units

```bash
# Step 1: Find department
curl -X GET "http://localhost:8080/api/pharmacy_adjustments/search/departments?query=Pharmacy" \
  -H "Finance: YOUR_API_KEY"

# Step 2: Search stock (no current quantity filter needed)
curl -X GET "http://localhost:8080/api/pharmacy_adjustments/search/stocks?query=Zaart%2050mg&department=Main%20Pharmacy&minQuantity=0.1" \
  -H "Finance: YOUR_API_KEY"

# Response might contain multiple matches:
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "stockId": 456,
      "itemName": "Zaart 50mg tablet",
      "batchNo": "B2025001",
      "stockQty": 30.0,
      "dateOfExpire": "2026-01-30 00:00:00",
      "retailRate": 15.50
    },
    {
      "stockId": 457,
      "itemName": "Zaart 50mg tablet",
      "batchNo": "B2025002",
      "stockQty": 25.0,
      "dateOfExpire": "2026-03-15 00:00:00",
      "retailRate": 15.50
    }
  ]
}

# Step 3: AI presents options to user
# User selects option 1 (stockId: 456)

# Step 4: Make adjustment with selected stock
curl -X POST "http://localhost:8080/api/pharmacy_adjustments/stock_quantity" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "stockId": 456,
    "newQuantity": 50.0,
    "comment": "Physical count adjustment - user selected batch B2025001",
    "departmentId": 1
  }'
```

### Example 2: Rate Adjustment

**Scenario**: Change Paracetamol retail rate from 25.00 to 28.00

```bash
# Step 1 & 2: Find department and stock (same as above)

# Step 3: Adjust retail rate
curl -X POST "http://localhost:8080/api/pharmacy_adjustments/retail_rate" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "stockId": 456,
    "newRetailRate": 28.00,
    "comment": "Price update - market adjustment",
    "departmentId": 1
  }'
```

### Example 3: Expiry Date Correction

**Scenario**: Correct expiry date from 2025-06-15 to 2025-08-15

```bash
# Step 1 & 2: Find department and stock (same as above)

# Step 3: Adjust expiry date
curl -X POST "http://localhost:8080/api/pharmacy_adjustments/expiry_date" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "stockId": 456,
    "newExpiryDate": "2025-08-15",
    "comment": "Corrected supplier expiry information",
    "departmentId": 1
  }'
```

## AI Agent Implementation Template

```python
import requests
import json
from datetime import datetime, timedelta

class PharmacyAdjustmentAI:
    def __init__(self, base_url, api_key):
        self.base_url = base_url.rstrip('/')
        self.headers = {
            "Finance": api_key,
            "Content-Type": "application/json"
        }

    def search_departments(self, query):
        """Search for departments"""
        url = f"{self.base_url}/api/pharmacy_adjustments/search/departments"
        params = {"query": query, "limit": 10}

        response = requests.get(url, headers=self.headers, params=params)
        return self._process_response(response)

    def search_stocks(self, item_query, department_name, **filters):
        """Search for stocks with optional filters"""
        url = f"{self.base_url}/api/pharmacy_adjustments/search/stocks"
        params = {
            "query": item_query,
            "department": department_name,
            "minQuantity": 0.1  # Exclude zero stock by default
        }
        params.update(filters)

        response = requests.get(url, headers=self.headers, params=params)
        return self._process_response(response)

    def adjust_stock_quantity(self, stock_id, department_id, new_quantity, comment):
        """Adjust stock quantity"""
        url = f"{self.base_url}/api/pharmacy_adjustments/stock_quantity"
        payload = {
            "stockId": stock_id,
            "newQuantity": new_quantity,
            "comment": comment,
            "departmentId": department_id
        }

        response = requests.post(url, headers=self.headers, json=payload)
        return self._process_response(response)

    def adjust_retail_rate(self, stock_id, department_id, new_rate, comment):
        """Adjust retail rate"""
        url = f"{self.base_url}/api/pharmacy_adjustments/retail_rate"
        payload = {
            "stockId": stock_id,
            "newRetailRate": new_rate,
            "comment": comment,
            "departmentId": department_id
        }

        response = requests.post(url, headers=self.headers, json=payload)
        return self._process_response(response)

    def adjust_expiry_date(self, stock_id, department_id, new_expiry, comment):
        """Adjust expiry date"""
        url = f"{self.base_url}/api/pharmacy_adjustments/expiry_date"
        payload = {
            "stockId": stock_id,
            "newExpiryDate": new_expiry,
            "comment": comment,
            "departmentId": department_id
        }

        response = requests.post(url, headers=self.headers, json=payload)
        return self._process_response(response)

    def adjust_purchase_rate(self, stock_id, department_id, new_rate, comment):
        """Adjust purchase rate"""
        url = f"{self.base_url}/api/pharmacy_adjustments/purchase_rate"
        payload = {
            "stockId": stock_id,
            "newPurchaseRate": new_rate,
            "comment": comment,
            "departmentId": department_id
        }

        response = requests.post(url, headers=self.headers, json=payload)
        return self._process_response(response)

    def _process_response(self, response):
        """Process API response"""
        try:
            data = response.json()
            if response.status_code == 200 and data.get("status") == "success":
                return {"success": True, "data": data["data"]}
            else:
                return {
                    "success": False,
                    "error": data.get("message", "Unknown error"),
                    "code": response.status_code
                }
        except json.JSONDecodeError:
            return {
                "success": False,
                "error": f"Invalid JSON response: {response.text}",
                "code": response.status_code
            }

    def execute_stock_adjustment(self, item_name, new_quantity, **criteria):
        """Complete workflow: search -> select -> adjust (with user interaction)"""

        # Step 1: Find department
        dept_result = self.search_departments(criteria.get('department_query', 'Pharmacy'))
        if not dept_result["success"] or not dept_result["data"]:
            return {"error": "Department not found"}

        department = dept_result["data"][0]

        # Step 2: Search stock (no current quantity verification)
        search_filters = {'minQuantity': 0.1}  # Only exclude zero stock

        # Add optional filters if specified
        if criteria.get('expiry_date'):
            search_filters['expiryAfter'] = criteria['expiry_date']
            search_filters['expiryBefore'] = criteria['expiry_date']

        if criteria.get('batch_number'):
            search_filters['batchNo'] = criteria['batch_number']

        stock_result = self.search_stocks(item_name, department["name"], **search_filters)
        if not stock_result["success"] or not stock_result["data"]:
            return {"error": "No stocks found"}

        # Step 3: Handle multiple matches
        if len(stock_result["data"]) == 1:
            # Single match - proceed directly
            selected_stock = stock_result["data"][0]
            print(f"✅ Found single match: {selected_stock['itemName']}")
            print(f"   Batch: {selected_stock['batchNo']} | Current Qty: {selected_stock['stockQty']}")
        else:
            # Multiple matches - ask user to select
            print(f"\n🔍 Found {len(stock_result['data'])} stocks matching '{item_name}':")
            selected_stock = self._present_stock_options(stock_result["data"])
            if not selected_stock:
                return {"error": "No stock selected"}

        # Step 4: Confirm adjustment with user
        print(f"\n📝 Adjustment Summary:")
        print(f"   Item: {selected_stock['itemName']}")
        print(f"   Batch: {selected_stock['batchNo']}")
        print(f"   Current Quantity: {selected_stock['stockQty']}")
        print(f"   New Quantity: {new_quantity}")
        print(f"   Expiry: {selected_stock['dateOfExpire'][:10]}")

        if criteria.get('auto_confirm', False):
            confirm = True
        else:
            confirm = input("\nProceed with adjustment? (y/N): ").lower().startswith('y')

        if not confirm:
            return {"cancelled": "User cancelled adjustment"}

        # Step 5: Make adjustment
        comment = criteria.get('comment', f'AI adjustment - {datetime.now().strftime("%Y-%m-%d %H:%M")}')

        adjust_result = self.adjust_stock_quantity(
            selected_stock["stockId"],
            department["id"],
            new_quantity,
            comment
        )

        if adjust_result["success"]:
            return {
                "success": True,
                "stock_info": {
                    "name": selected_stock["itemName"],
                    "batch": selected_stock["batchNo"],
                    "before": selected_stock["stockQty"],
                    "after": new_quantity,
                    "expiry": selected_stock["dateOfExpire"][:10]
                },
                "adjustment": adjust_result["data"]
            }
        else:
            return adjust_result

    def _present_stock_options(self, stocks):
        """Present stock options to user and get selection"""
        print("-" * 80)

        for i, stock in enumerate(stocks, 1):
            expiry_status = self._get_expiry_status(stock['dateOfExpire'])
            print(f"{i}. {stock['itemName']}")
            print(f"   Batch: {stock['batchNo']} | Qty: {stock['stockQty']}")
            print(f"   Expiry: {stock['dateOfExpire'][:10]} {expiry_status}")
            print(f"   Rates: Retail=${stock['retailRate']}, Cost=${stock.get('costRate', 'N/A')}")
            print(f"   Stock ID: {stock['stockId']}")
            print()

        while True:
            try:
                choice = input(f"Select option (1-{len(stocks)}, or 0 to cancel): ")
                if choice == '0':
                    return None

                choice_idx = int(choice) - 1
                if 0 <= choice_idx < len(stocks):
                    return stocks[choice_idx]
                else:
                    print("Invalid selection. Please try again.")
            except ValueError:
                print("Please enter a valid number.")

    def _get_expiry_status(self, expiry_date_str):
        """Get expiry status with emoji indicators"""
        from datetime import datetime, timedelta

        expiry_date = datetime.strptime(expiry_date_str[:10], '%Y-%m-%d')
        now = datetime.now()

        if expiry_date < now:
            return "⚠️ EXPIRED"
        elif expiry_date < now + timedelta(days=90):
            return "⏰ EXPIRING SOON"
        else:
            return "✅ VALID"

    def _matches_criteria(self, stock, criteria):
        """Check if stock matches expected criteria (simplified - no current qty requirement)"""
        # Optional expiry matching
        expected_expiry = criteria.get('expiry_date')
        if expected_expiry:
            stock_expiry = stock["dateOfExpire"][:10]
            if stock_expiry != expected_expiry:
                return False

        # Optional batch matching
        expected_batch = criteria.get('batch_number')
        if expected_batch:
            if expected_batch not in stock["batchNo"]:
                return False

        return True

# Example Usage:
if __name__ == "__main__":
    ai = PharmacyAdjustmentAI("http://localhost:8080", "YOUR_API_KEY")

    # Example 1: Simple adjustment (no current stock verification)
    result = ai.execute_stock_adjustment(
        item_name="Zaart 50mg tablet",
        new_quantity=50.0,
        comment="Physical count adjustment"
    )

    # Example 2: With specific expiry date filter
    result = ai.execute_stock_adjustment(
        item_name="Paracetamol 500mg",
        new_quantity=100.0,
        expiry_date="2026-01-30",  # Filter by expiry
        comment="Adjustment with expiry filter"
    )

    # Example 3: Auto-confirm mode (no user interaction)
    result = ai.execute_stock_adjustment(
        item_name="Aspirin 300mg",
        new_quantity=75.0,
        auto_confirm=True,  # Skip user confirmation
        comment="Automated bulk adjustment"
    )

    if result.get("success"):
        print(f"✅ Adjustment successful!")
        print(f"Item: {result['stock_info']['name']}")
        print(f"Batch: {result['stock_info']['batch']}")
        print(f"Quantity: {result['stock_info']['before']} → {result['stock_info']['after']}")
        print(f"Bill Number: {result['adjustment']['billNumber']}")
    elif result.get("cancelled"):
        print(f"🚫 Adjustment cancelled by user")
    else:
        print(f"❌ Adjustment failed: {result.get('error')}")
```

## Security and Best Practices

### 1. API Key Management
- Store API key securely (environment variables, secure config)
- Validate API key before making requests
- Handle authentication failures gracefully

### 2. Rate Limiting
- Implement delays between requests if needed
- Handle rate limit responses (429 status codes)
- Use connection pooling for efficiency

### 3. Data Validation
- Always validate search results before making adjustments
- Confirm quantities, rates, and dates match expectations
- Implement rollback logic if adjustments fail

### 4. Logging and Audit
- Log all API calls with timestamps
- Record adjustment reasons and sources
- Maintain audit trail for compliance

### 5. Error Recovery
- Implement retry logic for transient failures
- Use circuit breaker pattern for sustained failures
- Provide clear error messages for debugging

This documentation provides everything an AI agent needs to successfully interact with the Pharmacy Stock Management APIs. The combination of search and adjustment capabilities allows for fully automated pharmacy stock management while maintaining proper audit trails and validation.