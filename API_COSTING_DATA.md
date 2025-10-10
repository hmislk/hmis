# Costing Data API Documentation

## Overview
The Costing Data API provides endpoints to retrieve bill details from the HMIS system with comprehensive financial information including bill items, finance details, and pharmaceutical item details.

## Authentication
All endpoints require API Key-based authentication using the `Finance` header.

**Header:**
```
Finance: <your-api-key>
```

## Base URL
```
/api/costing_data
```

## Endpoints

### 1. Get Last Bill
Retrieves the most recent bill in the system.

**Endpoint:** `GET /api/costing_data/last_bill`

**Authentication:** Required

**Response:**
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 5661712,
    "dtype": "BilledBill",
    "createdAt": "2025-10-10 19:08:35",
    "fromDepartmentId": 485,
    "toDepartmentId": 90094,
    "departmentId": 485,
    "fromInstitutionId": 2,
    "toInstitutionId": 2,
    "institutionId": 2,
    "billTypeAtomic": "PHARMACY_ISSUE",
    "billType": "PharmacyTransferIssue",
    "discount": 0,
    "tax": 0,
    "expenseTotal": 0,
    "netTotal": 124.8,
    "total": 124.8,
    "billFinanceDetailsId": 5661713,
    "billFinanceDetails": {
      "id": 5661713,
      "netTotal": 124.8,
      "grossTotal": 124.8,
      "totalCostValue": 130,
      "totalPurchaseValue": 107.499,
      "totalRetailSaleValue": 124.8
    },
    "billItems": [
      {
        "id": 5661714,
        "createdAt": "2025-10-10 19:08:35",
        "billId": 5661712,
        "itemId": 117323,
        "billItemFinanceDetailsId": 5661715,
        "qty": 10,
        "rate": 12.48,
        "netRate": 12.48,
        "grossValue": 124.8,
        "netValue": 124.8,
        "retired": false,
        "billItemFinanceDetails": {
          "id": 5661715,
          "createdAt": "2025-10-10 19:08:25",
          "quantity": 10,
          "quantityByUnits": -10,
          "lineNetRate": 12.48,
          "grossRate": 12.48,
          "lineGrossRate": 12.48,
          "costRate": null,
          "purchaseRate": null,
          "retailSaleRate": 12.48,
          "lineCostRate": 13,
          "billCostRate": null,
          "totalCostRate": 13,
          "lineGrossTotal": 124.8,
          "grossTotal": 124.8,
          "lineCost": -130,
          "billCost": null,
          "totalCost": 130,
          "valueAtCostRate": -130,
          "valueAtPurchaseRate": -107.499,
          "valueAtRetailRate": -124.8
        },
        "pharmaceuticalBillItem": {
          "createdAt": null,
          "createrId": null,
          "id": 5661716,
          "billItemId": 5661714,
          "qty": -10,
          "freeQty": 0,
          "retailRate": 12.48,
          "purchaseRate": 10.7499,
          "costRate": 0,
          "purchaseValue": -107.499,
          "retailValue": -124.8,
          "costValue": 0
        }
      }
    ]
  }
}
```

### 2. Get Bill by Bill Number (Recommended)
Retrieves all bills matching the specified bill number (deptId) using query parameter.

**Endpoint:** `GET /api/costing_data/bill?number={bill_number}`

**Query Parameters:**
- `number` (string, required): The bill number (deptId) to search for

**Authentication:** Required

**Why Use This Endpoint:**
- ✅ Works with bill numbers containing special characters like `/`, `-`, etc.
- ✅ No URL encoding required
- ✅ Handles bill numbers like `MP/OP/25/000074` without issues

**Example Requests:**
```
GET /api/costing_data/bill?number=MP/OP/25/000074
GET /api/costing_data/bill?number=PHARM-2025-001
```

**Response:**
Returns an array of bills matching the bill number with the same structure as the Get Last Bill endpoint.

```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 5661712,
      "dtype": "BilledBill",
      ...
    }
  ]
}
```

### 2b. Get Bill by Bill Number (Alternative - Path Parameter)
⚠️ **DEPRECATED**: Use the query parameter endpoint above for better compatibility.

Retrieves all bills matching the specified bill number (deptId) using path parameter.

**Endpoint:** `GET /api/costing_data/by_bill_number/{bill_number}`

**Path Parameters:**
- `bill_number` (string, required): The bill number (deptId) to search for

**Authentication:** Required

**⚠️ Limitations:**
- Does NOT work with bill numbers containing forward slashes (`/`)
- Requires URL encoding for special characters
- Example: `MP/OP/25/000074` will fail

**Example Request (Only for simple bill numbers):**
```
GET /api/costing_data/by_bill_number/PHARM-2025-001
```

**Response:**
Returns an array of bills matching the bill number with the same structure as the Get Last Bill endpoint.

```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 5661712,
      "dtype": "BilledBill",
      ...
    }
  ]
}
```

### 3. Get Bill by ID
Retrieves a specific bill by its unique ID.

**Endpoint:** `GET /api/costing_data/by_bill_id/{bill_id}`

**Path Parameters:**
- `bill_id` (number, required): The unique ID of the bill

**Authentication:** Required

**Example Request:**
```
GET /api/costing_data/by_bill_id/5661712
```

**Response:**
Returns a single bill object with the same structure as the Get Last Bill endpoint.

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 5661712,
    "dtype": "BilledBill",
    ...
  }
}
```

## Response Object Structure

### Bill Details (BillDetailsDTO)
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Unique bill identifier |
| dtype | String | Bill discriminator type (e.g., "BilledBill") |
| createdAt | DateTime | Bill creation timestamp |
| fromDepartmentId | Long | Source department ID |
| toDepartmentId | Long | Destination department ID |
| departmentId | Long | Department ID |
| fromInstitutionId | Long | Source institution ID |
| toInstitutionId | Long | Destination institution ID |
| institutionId | Long | Institution ID |
| billTypeAtomic | String | Atomic bill type |
| billType | String | Bill type classification |
| discount | Double | Discount amount |
| tax | Double | Tax amount |
| expenseTotal | Double | Total expenses |
| netTotal | Double | Net total amount |
| total | Double | Total amount |
| billFinanceDetailsId | Long | Reference to bill finance details |
| billFinanceDetails | Object | Bill finance details object |
| billItems | Array | Array of bill items |

### Bill Finance Details (BillFinanceDetailsDTO)
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Unique identifier |
| netTotal | Double | Net total |
| grossTotal | Double | Gross total |
| totalCostValue | Double | Total cost value |
| totalPurchaseValue | Double | Total purchase value |
| totalRetailSaleValue | Double | Total retail sale value |

### Bill Item Details (BillItemDetailsDTO)
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Unique bill item identifier |
| createdAt | DateTime | Creation timestamp |
| billId | Long | Reference to parent bill |
| itemId | Long | Reference to item |
| billItemFinanceDetailsId | Long | Reference to finance details |
| qty | Double | Quantity |
| rate | Double | Rate |
| netRate | Double | Net rate |
| grossValue | Double | Gross value |
| netValue | Double | Net value |
| retired | Boolean | Retirement status |
| billItemFinanceDetails | Object | Bill item finance details |
| pharmaceuticalBillItem | Object | Pharmaceutical item details |

### Bill Item Finance Details (BillItemFinanceDetailsDTO)
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Unique identifier |
| createdAt | DateTime | Creation timestamp |
| quantity | Double | Quantity |
| quantityByUnits | Double | Quantity by units |
| lineNetRate | Double | Line net rate |
| grossRate | Double | Gross rate |
| lineGrossRate | Double | Line gross rate |
| costRate | Double | Cost rate |
| purchaseRate | Double | Purchase rate |
| retailSaleRate | Double | Retail sale rate |
| lineCostRate | Double | Line cost rate |
| billCostRate | Double | Bill cost rate |
| totalCostRate | Double | Total cost rate |
| lineGrossTotal | Double | Line gross total |
| grossTotal | Double | Gross total |
| lineCost | Double | Line cost |
| billCost | Double | Bill cost |
| totalCost | Double | Total cost |
| valueAtCostRate | Double | Value at cost rate |
| valueAtPurchaseRate | Double | Value at purchase rate |
| valueAtRetailRate | Double | Value at retail rate |

### Pharmaceutical Bill Item (PharmaceuticalBillItemDTO)
| Field | Type | Description |
|-------|------|-------------|
| createdAt | DateTime | Creation timestamp |
| createrId | Long | Creator ID |
| id | Long | Unique identifier |
| billItemId | Long | Reference to bill item |
| qty | Double | Quantity |
| freeQty | Double | Free quantity |
| retailRate | Double | Retail rate |
| purchaseRate | Double | Purchase rate |
| costRate | Double | Cost rate |
| purchaseValue | Double | Purchase value |
| retailValue | Double | Retail value |
| costValue | Double | Cost value |

## Error Responses

### 401 Unauthorized
```json
{
  "status": "error",
  "code": 401,
  "message": "Not a valid key"
}
```

### 400 Bad Request
```json
{
  "status": "error",
  "code": 400,
  "message": "Bill number is required"
}
```

### 404 Not Found
```json
{
  "status": "error",
  "code": 404,
  "message": "No bills found with bill number: PHARM-2025-001"
}
```

### 500 Internal Server Error
```json
{
  "status": "error",
  "code": 500,
  "message": "An error occurred: [error details]"
}
```

## Example Usage

### Using cURL

#### Get Last Bill
```bash
curl -X GET \
  http://localhost:8080/hmis/api/costing_data/last_bill \
  -H 'Finance: your-api-key-here'
```

#### Get Bill by Number (Recommended - Query Parameter)
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/costing_data/bill?number=MP/OP/25/000074' \
  -H 'Finance: your-api-key-here'
```

#### Get Bill by Number (Alternative - Simple bill numbers only)
```bash
curl -X GET \
  http://localhost:8080/hmis/api/costing_data/by_bill_number/PHARM-2025-001 \
  -H 'Finance: your-api-key-here'
```

#### Get Bill by ID
```bash
curl -X GET \
  http://localhost:8080/hmis/api/costing_data/by_bill_id/5661712 \
  -H 'Finance: your-api-key-here'
```

### Using Postman

1. Create a new GET request
2. Enter the endpoint URL
3. Go to Headers tab
4. Add header: `Finance` with value: `your-api-key-here`
5. Click Send

## API Key Management

To generate or manage API keys:
1. Log in to the HMIS system
2. Navigate to User Settings
3. Go to API Keys section
4. Create a new API key
5. Copy the generated key (it will only be shown once)
6. Use the key in the `Finance` header for all API requests

## Notes

- All datetime fields are in format: `yyyy-MM-dd HH:mm:ss`
- All monetary values are in Double format
- Null values are returned as `null` in JSON
- The API uses JAX-RS (REST) with JSON responses
- Response character encoding is UTF-8
