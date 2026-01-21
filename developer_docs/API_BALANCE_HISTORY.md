# Balance History API Documentation

## Overview
The Balance History API provides endpoints to retrieve historical balance change records for various payment and deposit systems in HMIS. This enables verification of payment processing and balance updates across drawer entries, patient deposits, and agent/collecting centre transactions.

## Authentication
All endpoints require API Key-based authentication using the `Finance` header.

**Header:**
```
Finance: <your-api-key>
```

## Base URL
```
/api/balance_history
```

## Use Cases
- **Payment Verification**: Confirm that bill payments correctly updated drawer balances
- **Deposit Tracking**: Monitor patient deposit usage and refunds
- **Agent/CC Reconciliation**: Track agent and collecting centre balance changes
- **Audit Trail**: Review historical balance changes for specific bills or time periods
- **Automated Testing**: Verify payment processing logic in integration tests

## Endpoints

### 1. Get Drawer Entries
Retrieves drawer entry records showing before/after balances for drawer transactions.

**Endpoint:** `GET /api/balance_history/drawer_entries`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| billId | Long | No | Filter by bill ID |
| fromDate | String | No | Start date (format: yyyy-MM-dd HH:mm:ss) |
| toDate | String | No | End date (format: yyyy-MM-dd HH:mm:ss) |
| paymentMethod | String | No | Filter by payment method (e.g., "Cash", "Card") |
| limit | Integer | No | Max records to return (default: 100) |

**Authentication:** Required

**Example Request:**
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/balance_history/drawer_entries?billId=5661712&limit=10' \
  -H 'Finance: your-api-key-here'
```

**Response:**
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 123456,
      "drawerId": 45,
      "drawerName": "Main Cashier Drawer",
      "paymentMethod": "Cash",
      "beforeBalance": 10000.00,
      "afterBalance": 11500.00,
      "transactionValue": 1500.00,
      "beforeInHandValue": 5000.00,
      "afterInHandValue": 6500.00,
      "billId": 5661712,
      "billNumber": "PHARM/2025/001",
      "paymentId": 789012,
      "createdAt": "2025-10-10 19:08:35",
      "createrName": "John Doe"
    }
  ]
}
```

### 2. Get Patient Deposit Histories
Retrieves patient deposit transaction history showing deposit additions and deductions.

**Endpoint:** `GET /api/balance_history/patient_deposits`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| billId | Long | No | Filter by bill ID |
| patientId | Long | No | Filter by patient ID |
| fromDate | String | No | Start date (format: yyyy-MM-dd HH:mm:ss) |
| toDate | String | No | End date (format: yyyy-MM-dd HH:mm:ss) |
| limit | Integer | No | Max records to return (default: 100) |

**Authentication:** Required

**Example Request:**
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/balance_history/patient_deposits?patientId=12345&limit=20' \
  -H 'Finance: your-api-key-here'
```

**Response:**
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 234567,
      "patientDepositId": 56789,
      "patientId": 12345,
      "patientName": "Jane Smith",
      "patientPhn": "PHN123456",
      "billId": 5661712,
      "billNumber": "OPD/2025/0123",
      "balanceBeforeTransaction": 5000.00,
      "balanceAfterTransaction": 3500.00,
      "transactionValue": 1500.00,
      "historyType": "Bill",
      "departmentId": 485,
      "departmentName": "OPD",
      "institutionId": 2,
      "institutionName": "Main Hospital",
      "createdAt": "2025-10-10 19:08:35",
      "cancelled": false,
      "refunded": false,
      "createrName": "John Doe"
    }
  ]
}
```

### 3. Get Agent Histories
Retrieves agent/collecting centre transaction history.

**Endpoint:** `GET /api/balance_history/agent_histories`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| billId | Long | No | Filter by bill ID |
| agencyId | Long | No | Filter by agency/collecting centre ID |
| fromDate | String | No | Start date (format: yyyy-MM-dd HH:mm:ss) |
| toDate | String | No | End date (format: yyyy-MM-dd HH:mm:ss) |
| limit | Integer | No | Max records to return (default: 100) |

**Authentication:** Required

**Example Request:**
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/balance_history/agent_histories?agencyId=789&limit=15' \
  -H 'Finance: your-api-key-here'
```

**Response:**
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 345678,
      "agencyId": 789,
      "agencyName": "City Collecting Centre",
      "billId": 5661712,
      "billNumber": "CHANNEL/2025/045",
      "balanceBeforeTransaction": 20000.00,
      "balanceAfterTransaction": 21500.00,
      "transactionValue": 1500.00,
      "agentBalanceBefore": 15000.00,
      "agentBalanceAfter": 16500.00,
      "companyBalanceBefore": 5000.00,
      "companyBalanceAfter": 5000.00,
      "historyType": "AgentReferenceBooking",
      "createdAt": "2025-10-10 19:08:35"
    }
  ]
}
```

### 4. Get Staff Welfare Histories
Retrieves staff welfare transaction history (uses DrawerEntry filtered by Staff_Welfare payment method).

**Endpoint:** `GET /api/balance_history/staff_welfare_histories`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| billId | Long | No | Filter by bill ID |
| staffId | Long | No | Filter by staff ID |
| fromDate | String | No | Start date (format: yyyy-MM-dd HH:mm:ss) |
| toDate | String | No | End date (format: yyyy-MM-dd HH:mm:ss) |
| limit | Integer | No | Max records to return (default: 100) |

**Authentication:** Required

**Example Request:**
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/balance_history/staff_welfare_histories?staffId=456&limit=10' \
  -H 'Finance: your-api-key-here'
```

**Response:**
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 456789,
      "drawerId": 45,
      "drawerName": "Main Cashier Drawer",
      "paymentMethod": "Staff_Welfare",
      "beforeBalance": 50000.00,
      "afterBalance": 48500.00,
      "transactionValue": -1500.00,
      "beforeInHandValue": 25000.00,
      "afterInHandValue": 23500.00,
      "billId": 5661712,
      "billNumber": "PHARM/2025/002",
      "paymentId": 890123,
      "createdAt": "2025-10-10 19:08:35",
      "createrName": "John Doe"
    }
  ]
}
```

## Response Object Structure

### DrawerEntryDTO
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Unique drawer entry identifier |
| drawerId | Long | Drawer ID |
| drawerName | String | Drawer name |
| paymentMethod | String | Payment method |
| beforeBalance | Double | Balance before transaction |
| afterBalance | Double | Balance after transaction |
| transactionValue | Double | Transaction amount (positive = addition, negative = deduction) |
| beforeInHandValue | Double | In-hand value before transaction |
| afterInHandValue | Double | In-hand value after transaction |
| billId | Long | Associated bill ID |
| billNumber | String | Bill number (deptId) |
| paymentId | Long | Associated payment ID |
| createdAt | DateTime | Creation timestamp |
| createrName | String | Creator name |

### PatientDepositHistoryDto
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Unique history entry identifier |
| patientDepositId | Long | Patient deposit ID |
| patientId | Long | Patient ID |
| patientName | String | Patient name |
| patientPhn | String | Patient PHN |
| billId | Long | Associated bill ID |
| billNumber | String | Bill number (deptId) |
| balanceBeforeTransaction | Double | Balance before transaction |
| balanceAfterTransaction | Double | Balance after transaction |
| transactionValue | Double | Transaction amount |
| historyType | String | Type of history entry |
| departmentId | Long | Department ID |
| departmentName | String | Department name |
| institutionId | Long | Institution ID |
| institutionName | String | Institution name |
| createdAt | DateTime | Creation timestamp |
| cancelled | Boolean | Bill cancelled status |
| refunded | Boolean | Bill refunded status |
| createrName | String | Creator name |

### AgentHistoryDTO
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Unique history entry identifier |
| agencyId | Long | Agency/collecting centre ID |
| agencyName | String | Agency name |
| billId | Long | Associated bill ID |
| billNumber | String | Bill number (deptId) |
| balanceBeforeTransaction | Double | Total balance before transaction |
| balanceAfterTransaction | Double | Total balance after transaction |
| transactionValue | Double | Transaction amount |
| agentBalanceBefore | Double | Agent balance before |
| agentBalanceAfter | Double | Agent balance after |
| companyBalanceBefore | Double | Company balance before |
| companyBalanceAfter | Double | Company balance after |
| historyType | String | Type of history entry |
| createdAt | DateTime | Creation timestamp |

## Query Parameter Guide

### Date Filtering
When using `fromDate` and `toDate` parameters, use the format: `yyyy-MM-dd HH:mm:ss`

**Examples:**
```
fromDate=2025-01-01 00:00:00
toDate=2025-12-31 23:59:59
```

**URL Encoding:**
When using dates in URLs, spaces should be encoded as `%20`:
```
fromDate=2025-01-01%2000:00:00
```

### Limit Parameter
The `limit` parameter controls the maximum number of records returned. Default is 100 if not specified.

**Examples:**
```
limit=10   # Return max 10 records
limit=100  # Return max 100 records (default)
limit=500  # Return max 500 records
```

### Combining Filters
You can combine multiple query parameters to narrow down results:

```bash
# Get drawer entries for specific bill and payment method
/api/balance_history/drawer_entries?billId=5661712&paymentMethod=Cash

# Get patient deposits for date range and specific patient
/api/balance_history/patient_deposits?patientId=12345&fromDate=2025-01-01%2000:00:00&toDate=2025-01-31%2023:59:59

# Get agent histories with limit
/api/balance_history/agent_histories?agencyId=789&limit=50
```

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
  "message": "Invalid fromDate format. Expected: yyyy-MM-dd HH:mm:ss"
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

## Common Use Cases

### 1. Verify Payment Processing for a Bill
Get all drawer entries for a specific bill to verify payment processing:

```bash
curl -X GET \
  'http://localhost:8080/hmis/api/balance_history/drawer_entries?billId=5661712' \
  -H 'Finance: your-api-key-here'
```

**Verification:**
- Check that `transactionValue` matches the payment amount
- Verify `afterBalance = beforeBalance + transactionValue`
- Confirm `paymentMethod` matches expected payment type

### 2. Track Patient Deposit Usage
Get patient deposit history for a specific patient:

```bash
curl -X GET \
  'http://localhost:8080/hmis/api/balance_history/patient_deposits?patientId=12345' \
  -H 'Finance: your-api-key-here'
```

**Analysis:**
- Monitor deposit additions (positive `transactionValue`)
- Track deposit deductions for bills (negative `transactionValue`)
- Verify running balance calculations

### 3. Audit Drawer Transactions by Payment Method
Get all cash transactions in a date range:

```bash
curl -X GET \
  'http://localhost:8080/hmis/api/balance_history/drawer_entries?paymentMethod=Cash&fromDate=2025-10-01%2000:00:00&toDate=2025-10-31%2023:59:59' \
  -H 'Finance: your-api-key-here'
```

### 4. Verify Agent Commission Processing
Get agent history for a specific bill:

```bash
curl -X GET \
  'http://localhost:8080/hmis/api/balance_history/agent_histories?billId=5661712' \
  -H 'Finance: your-api-key-here'
```

**Verification:**
- Check agent and company balance splits
- Verify commission calculations
- Confirm total balance changes

## Known Limitations

### Credit Company History
**Limitation:** No dedicated history table exists for credit company balance changes.
- Current balance tracked in `Institution.allowedCredit`
- Cannot retrieve historical balance changes via API
- **Workaround:** Query bills with `paymentMethod = "Credit"` from Bill API and manually calculate balances

**Example:**
```bash
# This endpoint does NOT exist (no credit company history tracking)
# /api/balance_history/credit_company_histories  # NOT AVAILABLE
```

**Future Enhancement:** Consider creating a `CreditCompanyHistory` entity similar to `AgentHistory` and `PatientDepositHistory`.

### Staff Welfare History
**Implementation:** Uses `DrawerEntry` filtered by `PaymentMethod.Staff_Welfare`
- No dedicated `StaffWelfareHistory` table
- Only transaction-level changes available
- No running balance snapshots specific to staff welfare

**Sufficient For:** Payment verification testing and audit trails

## Integration Testing Example

### Python Test for Cash Payment Verification
```python
import requests

# Configuration
BASE_URL = "http://localhost:8080/hmis/api"
API_KEY = "your-api-key-here"
HEADERS = {"Finance": API_KEY}

def verify_cash_payment(bill_id, expected_amount):
    """Verify cash payment was processed correctly"""

    # Get drawer entries for the bill
    response = requests.get(
        f"{BASE_URL}/balance_history/drawer_entries",
        params={"billId": bill_id, "paymentMethod": "Cash"},
        headers=HEADERS
    )

    data = response.json()

    if data["status"] != "success":
        return False, f"API Error: {data.get('message')}"

    entries = data["data"]

    if len(entries) == 0:
        return False, "No drawer entries found"

    entry = entries[0]

    # Verify transaction value matches expected amount
    if abs(entry["transactionValue"] - expected_amount) > 0.01:
        return False, f"Transaction value mismatch: {entry['transactionValue']} != {expected_amount}"

    # Verify balance arithmetic
    expected_after = entry["beforeBalance"] + entry["transactionValue"]
    if abs(entry["afterBalance"] - expected_after) > 0.01:
        return False, f"Balance arithmetic error: {entry['afterBalance']} != {expected_after}"

    return True, "Payment verified successfully"

# Usage
success, message = verify_cash_payment(bill_id=5661712, expected_amount=1500.00)
print(f"Verification: {message}")
```

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
- History records are filtered by `retired = false` by default
- Results are ordered by ID descending (newest first)
