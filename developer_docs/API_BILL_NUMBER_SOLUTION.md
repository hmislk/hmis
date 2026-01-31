# Solution for Bill Numbers with Special Characters

## Problem
Bill numbers containing forward slashes (e.g., `MP/OP/25/000074`) cannot be used in URL path parameters because the slashes are interpreted as path separators.

Example of **FAILING** URL:
```
❌ http://localhost:8080/hmis/api/costing_data/by_bill_number/MP/OP/25/000074
```

## Solution: Use Query Parameter Endpoint

### New Recommended Endpoint
```
✅ GET /api/costing_data/bill?number={bill_number}
```

### Why This Works
- Query parameters are automatically URL-decoded by the web server
- Special characters like `/`, `-`, `_`, etc. are handled correctly
- No manual URL encoding needed
- Cleaner API design for values with special characters

## Usage Examples

### Example 1: Bill Number with Slashes
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/costing_data/bill?number=MP/OP/25/000074' \
  -H 'Finance: your-api-key-here'
```

### Example 2: Simple Bill Number
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/costing_data/bill?number=PHARM-2025-001' \
  -H 'Finance: your-api-key-here'
```

### Example 3: Using Postman
1. Method: GET
2. URL: `http://localhost:8080/hmis/api/costing_data/bill`
3. Params Tab:
   - Key: `number`
   - Value: `MP/OP/25/000074`
4. Headers Tab:
   - Key: `Finance`
   - Value: `your-api-key-here`

### Example 4: Using JavaScript/Fetch
```javascript
const billNumber = 'MP/OP/25/000074';
const apiKey = 'your-api-key-here';

fetch(`http://localhost:8080/hmis/api/costing_data/bill?number=${encodeURIComponent(billNumber)}`, {
  method: 'GET',
  headers: {
    'Finance': apiKey
  }
})
.then(response => response.json())
.then(data => console.log(data));
```

### Example 5: Using Python
```python
import requests

bill_number = 'MP/OP/25/000074'
api_key = 'your-api-key-here'

response = requests.get(
    'http://localhost:8080/hmis/api/costing_data/bill',
    params={'number': bill_number},  # Automatically URL-encoded
    headers={'Finance': api_key}
)

data = response.json()
print(data)
```

## Comparison: Old vs New Endpoint

| Feature | Old Endpoint (Path Param) | New Endpoint (Query Param) |
|---------|---------------------------|----------------------------|
| URL | `/by_bill_number/{number}` | `/bill?number={number}` |
| Works with `/` | ❌ No | ✅ Yes |
| Works with `-` | ✅ Yes | ✅ Yes |
| Works with `_` | ✅ Yes | ✅ Yes |
| Works with spaces | ❌ No | ✅ Yes (auto-decoded) |
| Requires URL encoding | ⚠️ Sometimes | ❌ No (automatic) |
| Status | Deprecated | ✅ Recommended |

## Alternative Solution (Not Recommended)

If you must use the old path parameter endpoint, you would need to URL-encode the bill number:

```bash
# Manual URL encoding (complex and error-prone)
# MP/OP/25/000074 becomes MP%2FOP%2F25%2F000074

curl -X GET \
  'http://localhost:8080/hmis/api/costing_data/by_bill_number/MP%2FOP%2F25%2F000074' \
  -H 'Finance: your-api-key-here'
```

**Why not recommended:**
- ❌ Requires manual URL encoding
- ❌ More error-prone
- ❌ Less readable URLs
- ❌ May still have edge cases with certain characters

## Implementation Details

The new endpoint was added to `CostingData.java`:

```java
@GET
@Path("/bill")
@Produces(MediaType.APPLICATION_JSON)
public String getBillByNumberQuery(@QueryParam("number") String billNumber) {
    // Implementation handles bill numbers with any special characters
}
```

## Response Format

Both endpoints return the same response format:

```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 5661712,
      "dtype": "BilledBill",
      "createdAt": "2025-10-10 19:08:35",
      "billType": "PharmacyTransferIssue",
      "netTotal": 124.8,
      "billFinanceDetails": { ... },
      "billItems": [ ... ]
    }
  ]
}
```

## Summary

✅ **Use:** `GET /api/costing_data/bill?number={bill_number}` (Query Parameter)

❌ **Avoid:** `GET /api/costing_data/by_bill_number/{bill_number}` (Path Parameter)

The query parameter approach is:
- More flexible
- Easier to use
- Handles all special characters
- Industry best practice for searchable values
