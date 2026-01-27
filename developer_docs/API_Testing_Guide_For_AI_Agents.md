# API Testing Guide for AI Agents

## Overview

This guide provides comprehensive testing workflows for AI agents to verify payment generation and balance updates in the HMIS system. It includes complete Python code examples that can be directly used or adapted for automated testing.

## Table of Contents

1. [Testing Workflow Overview](#testing-workflow-overview)
2. [Setup and Configuration](#setup-and-configuration)
3. [Complete Python Test Class](#complete-python-test-class)
4. [Bill Type Atomic Test Scenarios](#bill-type-atomic-test-scenarios)
5. [Payment Method Test Matrix](#payment-method-test-matrix)
6. [Balance Verification Workflows](#balance-verification-workflows)
7. [Advanced Testing Scenarios](#advanced-testing-scenarios)
8. [Troubleshooting](#troubleshooting)

## Testing Workflow Overview

### General Test Flow

1. **Pre-test**: Record initial balances (optional, for differential verification)
2. **Execute**: Create bill with payments using HMIS UI or API
3. **Verify**: Retrieve bill details and balance histories via API
4. **Assert**: Validate all balance updates match expected calculations

### APIs Used

- **Bill Details API** (`/api/costing_data`): Retrieve bill information with payments
- **Balance History APIs** (`/api/balance_history`): Verify balance changes
  - `/drawer_entries` - Drawer balance updates
  - `/patient_deposits` - Patient deposit changes
  - `/agent_histories` - Agent/collecting centre balances
  - `/staff_welfare_histories` - Staff welfare balance changes

## Setup and Configuration

### Requirements

```bash
pip install requests
```

### Environment Configuration

```python
# config.py
class TestConfig:
    BASE_URL = "http://localhost:8080/hmis"
    API_KEY = "your-api-key-here"
    HEADERS = {"Finance": API_KEY}

    # For production testing
    # BASE_URL = "https://your-production-server.com/hmis"
```

## Complete Python Test Class

```python
import requests
from datetime import datetime
from typing import Dict, List, Optional

class HMISPaymentVerifier:
    """Comprehensive payment and balance verification for HMIS bills"""

    def __init__(self, base_url: str, api_key: str):
        self.base_url = base_url.rstrip('/')
        self.headers = {"Finance": api_key}

    def get_bill_details(self, bill_number: str) -> Dict:
        """Get complete bill details including payments"""
        response = requests.get(
            f"{self.base_url}/api/costing_data/bill",
            params={"number": bill_number},
            headers=self.headers
        )
        response.raise_for_status()
        data = response.json()
        if data["status"] != "success" or not data["data"]:
            raise ValueError(f"Bill not found: {bill_number}")
        return data["data"][0]

    def get_bill_by_id(self, bill_id: int) -> Dict:
        """Get bill details by ID"""
        response = requests.get(
            f"{self.base_url}/api/costing_data/by_bill_id/{bill_id}",
            headers=self.headers
        )
        response.raise_for_status()
        data = response.json()
        if data["status"] != "success":
            raise ValueError(f"Bill not found: {bill_id}")
        return data["data"]

    def get_drawer_entries(self, bill_id: int = None, payment_method: str = None,
                          from_date: str = None, to_date: str = None, limit: int = 100) -> List[Dict]:
        """Get drawer entries with optional filters"""
        params = {"limit": limit}
        if bill_id:
            params["billId"] = bill_id
        if payment_method:
            params["paymentMethod"] = payment_method
        if from_date:
            params["fromDate"] = from_date
        if to_date:
            params["toDate"] = to_date

        response = requests.get(
            f"{self.base_url}/api/balance_history/drawer_entries",
            params=params,
            headers=self.headers
        )
        response.raise_for_status()
        data = response.json()
        return data["data"] if data["status"] == "success" else []

    def get_patient_deposit_history(self, bill_id: int = None, patient_id: int = None,
                                   from_date: str = None, to_date: str = None, limit: int = 100) -> List[Dict]:
        """Get patient deposit history with optional filters"""
        params = {"limit": limit}
        if bill_id:
            params["billId"] = bill_id
        if patient_id:
            params["patientId"] = patient_id
        if from_date:
            params["fromDate"] = from_date
        if to_date:
            params["toDate"] = to_date

        response = requests.get(
            f"{self.base_url}/api/balance_history/patient_deposits",
            params=params,
            headers=self.headers
        )
        response.raise_for_status()
        data = response.json()
        return data["data"] if data["status"] == "success" else []

    def get_agent_history(self, bill_id: int = None, agency_id: int = None,
                         from_date: str = None, to_date: str = None, limit: int = 100) -> List[Dict]:
        """Get agent/collecting centre history with optional filters"""
        params = {"limit": limit}
        if bill_id:
            params["billId"] = bill_id
        if agency_id:
            params["agencyId"] = agency_id
        if from_date:
            params["fromDate"] = from_date
        if to_date:
            params["toDate"] = to_date

        response = requests.get(
            f"{self.base_url}/api/balance_history/agent_histories",
            params=params,
            headers=self.headers
        )
        response.raise_for_status()
        data = response.json()
        return data["data"] if data["status"] == "success" else []

    def get_staff_welfare_history(self, bill_id: int = None, staff_id: int = None,
                                  from_date: str = None, to_date: str = None, limit: int = 100) -> List[Dict]:
        """Get staff welfare history with optional filters"""
        params = {"limit": limit}
        if bill_id:
            params["billId"] = bill_id
        if staff_id:
            params["staffId"] = staff_id
        if from_date:
            params["fromDate"] = from_date
        if to_date:
            params["toDate"] = to_date

        response = requests.get(
            f"{self.base_url}/api/balance_history/staff_welfare_histories",
            params=params,
            headers=self.headers
        )
        response.raise_for_status()
        data = response.json()
        return data["data"] if data["status"] == "success" else []

    def verify_cash_sale(self, bill_number: str) -> Dict:
        """Verify cash sale bill and drawer balance updates"""
        print(f"\n=== Verifying Cash Sale: {bill_number} ===")

        # Get bill details
        bill = self.get_bill_details(bill_number)
        print(f"Bill ID: {bill['id']}")
        print(f"Bill Type Atomic: {bill['billTypeAtomic']}")
        print(f"Net Total: {bill['netTotal']}")

        # Verify payments exist
        assert "payments" in bill, "❌ Payments array missing in bill response"
        assert len(bill["payments"]) > 0, "❌ No payments found"
        print(f"✓ Found {len(bill['payments'])} payment(s)")

        # Verify payment total matches bill total
        payment_total = sum(p["paidValue"] for p in bill["payments"])
        assert abs(payment_total - bill["netTotal"]) < 0.01, \
            f"❌ Payment total ({payment_total}) != Bill total ({bill['netTotal']})"
        print(f"✓ Payment total matches bill total: {payment_total}")

        # Get drawer entries for this bill
        drawer_entries = self.get_drawer_entries(bill_id=bill["id"])
        print(f"✓ Found {len(drawer_entries)} drawer entry(ies)")

        # Verify each payment has corresponding drawer entry
        for payment in bill["payments"]:
            matching_entry = next(
                (e for e in drawer_entries
                 if e["paymentMethod"] == payment["paymentMethod"]
                 and abs(e["transactionValue"] - payment["paidValue"]) < 0.01),
                None
            )

            assert matching_entry is not None, \
                f"❌ No drawer entry for payment {payment['paymentMethod']} ({payment['paidValue']})"

            # Verify balance arithmetic
            expected_after = matching_entry["beforeBalance"] + matching_entry["transactionValue"]
            assert abs(matching_entry["afterBalance"] - expected_after) < 0.01, \
                f"❌ Drawer balance arithmetic incorrect for {payment['paymentMethod']}"

            print(f"✓ {payment['paymentMethod']}: {payment['paidValue']} - Drawer updated correctly")
            print(f"  Before: {matching_entry['beforeBalance']}, After: {matching_entry['afterBalance']}")

        return {
            "status": "PASS",
            "bill_number": bill_number,
            "bill_id": bill["id"],
            "total": bill["netTotal"],
            "payments": len(bill["payments"]),
            "drawer_entries_verified": len(drawer_entries)
        }

    def verify_patient_deposit_usage(self, bill_number: str) -> Dict:
        """Verify bill paid with patient deposit and balance deduction"""
        print(f"\n=== Verifying Patient Deposit Usage: {bill_number} ===")

        # Get bill details
        bill = self.get_bill_details(bill_number)
        print(f"Bill ID: {bill['id']}")
        print(f"Net Total: {bill['netTotal']}")

        # Find patient deposit payment
        pd_payments = [p for p in bill["payments"] if p["paymentMethod"] == "PatientDeposit"]
        assert len(pd_payments) > 0, "❌ No patient deposit payment found"
        print(f"✓ Found {len(pd_payments)} patient deposit payment(s)")

        # Get patient deposit history
        pd_history = self.get_patient_deposit_history(bill_id=bill["id"])
        assert len(pd_history) > 0, "❌ No patient deposit history found"
        print(f"✓ Found {len(pd_history)} patient deposit history entry(ies)")

        # Verify balance deduction for each patient deposit payment
        for pd_payment in pd_payments:
            matching_history = next(
                (h for h in pd_history
                 if abs(h["transactionValue"] - pd_payment["paidValue"]) < 0.01),
                None
            )

            assert matching_history is not None, \
                f"❌ No history entry for patient deposit payment {pd_payment['paidValue']}"

            # Verify balance decreased (deposit was deducted)
            expected_after = matching_history["balanceBeforeTransaction"] - matching_history["transactionValue"]
            assert abs(matching_history["balanceAfterTransaction"] - expected_after) < 0.01, \
                "❌ Patient deposit balance deduction incorrect"

            print(f"✓ Patient Deposit: {pd_payment['paidValue']} deducted correctly")
            print(f"  Before: {matching_history['balanceBeforeTransaction']}, "
                  f"After: {matching_history['balanceAfterTransaction']}")

        return {
            "status": "PASS",
            "bill_number": bill_number,
            "patient_deposit_used": sum(p["paidValue"] for p in pd_payments),
            "history_entries_verified": len(pd_history)
        }

    def verify_multiple_payment_methods(self, bill_number: str) -> Dict:
        """Verify bill with multiple payment methods"""
        print(f"\n=== Verifying Multiple Payment Methods: {bill_number} ===")

        # Get bill details
        bill = self.get_bill_details(bill_number)
        print(f"Bill ID: {bill['id']}")
        print(f"Net Total: {bill['netTotal']}")

        # Verify multiple payments
        assert len(bill["payments"]) > 1, \
            f"❌ Expected multiple payments, found {len(bill['payments'])}"
        print(f"✓ Found {len(bill['payments'])} payment methods:")

        for payment in bill["payments"]:
            print(f"  - {payment['paymentMethod']}: {payment['paidValue']}")

        # Verify payment total
        payment_total = sum(p["paidValue"] for p in bill["payments"])
        assert abs(payment_total - bill["netTotal"]) < 0.01, \
            f"❌ Payment total ({payment_total}) != Bill total ({bill['netTotal']})"

        # Get drawer entries
        drawer_entries = self.get_drawer_entries(bill_id=bill["id"])
        print(f"✓ Found {len(drawer_entries)} drawer entry(ies)")

        # Verify each payment has drawer entry (except PatientDeposit)
        for payment in bill["payments"]:
            if payment["paymentMethod"] == "PatientDeposit":
                # Verify patient deposit history instead
                pd_history = self.get_patient_deposit_history(bill_id=bill["id"])
                assert len(pd_history) > 0, "❌ No patient deposit history"
                print(f"✓ {payment['paymentMethod']} patient deposit history verified")
            else:
                matching_entry = next(
                    (e for e in drawer_entries
                     if e["paymentMethod"] == payment["paymentMethod"]
                     and abs(e["transactionValue"] - payment["paidValue"]) < 0.01),
                    None
                )
                assert matching_entry is not None, \
                    f"❌ No drawer entry for {payment['paymentMethod']}"
                print(f"✓ {payment['paymentMethod']} drawer entry verified")

        return {
            "status": "PASS",
            "bill_number": bill_number,
            "payment_methods": [p["paymentMethod"] for p in bill["payments"]],
            "total_payments": len(bill["payments"])
        }

    def verify_bill_by_type_atomic(self, bill_number: str, expected_type_atomic: str) -> Dict:
        """Verify bill has expected type atomic and all balances updated"""
        print(f"\n=== Verifying Bill Type Atomic: {bill_number} ===")

        # Get bill details
        bill = self.get_bill_details(bill_number)

        # Verify bill type atomic
        assert bill["billTypeAtomic"] == expected_type_atomic, \
            f"❌ Expected {expected_type_atomic}, got {bill['billTypeAtomic']}"
        print(f"✓ Bill Type Atomic: {bill['billTypeAtomic']}")

        # Verify all payments
        print(f"Verifying {len(bill['payments'])} payment(s)...")
        drawer_entries = self.get_drawer_entries(bill_id=bill["id"])

        for payment in bill["payments"]:
            # Check drawer entry or patient deposit history
            if payment["paymentMethod"] == "PatientDeposit":
                pd_history = self.get_patient_deposit_history(bill_id=bill["id"])
                assert len(pd_history) > 0, "❌ No patient deposit history"
                print(f"✓ {payment['paymentMethod']}: Patient deposit history verified")
            else:
                drawer_entry = next(
                    (e for e in drawer_entries if e["paymentMethod"] == payment["paymentMethod"]),
                    None
                )
                assert drawer_entry is not None, f"❌ No drawer entry for {payment['paymentMethod']}"
                print(f"✓ {payment['paymentMethod']}: Drawer entry verified")

        return {
            "status": "PASS",
            "bill_type_atomic": bill["billTypeAtomic"],
            "all_balances_verified": True
        }

    def verify_agent_payment(self, bill_number: str) -> Dict:
        """Verify agent/collecting centre payment processing"""
        print(f"\n=== Verifying Agent Payment: {bill_number} ===")

        # Get bill details
        bill = self.get_bill_details(bill_number)
        print(f"Bill ID: {bill['id']}")

        # Find agent payments
        agent_payments = [p for p in bill["payments"] if p["paymentMethod"] == "Agent"]
        if not agent_payments:
            return {"status": "SKIP", "message": "No agent payments in this bill"}

        print(f"✓ Found {len(agent_payments)} agent payment(s)")

        # Get agent history
        agent_history = self.get_agent_history(bill_id=bill["id"])
        assert len(agent_history) > 0, "❌ No agent history found"
        print(f"✓ Found {len(agent_history)} agent history entry(ies)")

        # Verify each agent payment
        for agent_payment in agent_payments:
            matching_history = next(
                (h for h in agent_history
                 if abs(h["transactionValue"] - agent_payment["paidValue"]) < 0.01),
                None
            )

            assert matching_history is not None, \
                f"❌ No history entry for agent payment {agent_payment['paidValue']}"

            # Verify balance arithmetic
            expected_after = matching_history["balanceBeforeTransaction"] + matching_history["transactionValue"]
            assert abs(matching_history["balanceAfterTransaction"] - expected_after) < 0.01, \
                "❌ Agent balance arithmetic incorrect"

            print(f"✓ Agent Payment: {agent_payment['paidValue']} processed correctly")
            print(f"  Total Before: {matching_history['balanceBeforeTransaction']}, "
                  f"After: {matching_history['balanceAfterTransaction']}")
            print(f"  Agent Balance: {matching_history['agentBalanceBefore']} -> {matching_history['agentBalanceAfter']}")
            print(f"  Company Balance: {matching_history['companyBalanceBefore']} -> {matching_history['companyBalanceAfter']}")

        return {
            "status": "PASS",
            "bill_number": bill_number,
            "agent_payments_verified": len(agent_payments)
        }


# Usage examples
if __name__ == "__main__":
    # Initialize verifier
    verifier = HMISPaymentVerifier(
        base_url="http://localhost:8080/hmis",
        api_key="YOUR_API_KEY_HERE"
    )

    # Test scenarios
    try:
        # Test 1: Simple cash sale
        result1 = verifier.verify_cash_sale("PHARM/2025/0001")
        print(f"\n✅ Cash Sale Test: {result1['status']}")

        # Test 2: Patient deposit usage
        result2 = verifier.verify_patient_deposit_usage("OPD/2025/0123")
        print(f"\n✅ Patient Deposit Test: {result2['status']}")

        # Test 3: Multiple payment methods
        result3 = verifier.verify_multiple_payment_methods("PHARM/2025/0045")
        print(f"\n✅ Multiple Payments Test: {result3['status']}")

        # Test 4: Bill type atomic verification
        result4 = verifier.verify_bill_by_type_atomic(
            "PHARM/2025/0001",
            "PHARMACY_RETAIL_SALE_WITH_PAYMENT"
        )
        print(f"\n✅ Type Atomic Test: {result4['status']}")

        # Test 5: Agent payment
        result5 = verifier.verify_agent_payment("CHANNEL/2025/0089")
        print(f"\n✅ Agent Payment Test: {result5['status']}")

    except AssertionError as e:
        print(f"\n❌ Test Failed: {e}")
    except Exception as e:
        print(f"\n❌ Error: {e}")
```

## Bill Type Atomic Test Scenarios

### PHARMACY_RETAIL_SALE_WITH_PAYMENT
```python
def test_pharmacy_retail_sale():
    verifier = HMISPaymentVerifier(BASE_URL, API_KEY)

    # Verify pharmacy retail sale
    result = verifier.verify_bill_by_type_atomic(
        bill_number="PHARM/2025/0001",
        expected_type_atomic="PHARMACY_RETAIL_SALE_WITH_PAYMENT"
    )

    # Expected balance changes:
    # - Drawer balance increases by payment amount
    # - Stock reduces for items sold
    assert result["status"] == "PASS"
```

### OPD_BATCH_BILL_WITH_PAYMENT
```python
def test_opd_batch_bill():
    verifier = HMISPaymentVerifier(BASE_URL, API_KEY)

    result = verifier.verify_bill_by_type_atomic(
        bill_number="OPD/2025/0123",
        expected_type_atomic="OPD_BATCH_BILL_WITH_PAYMENT"
    )

    # Expected balance changes:
    # - Drawer balance increases
    # - May include multiple service charges
    assert result["status"] == "PASS"
```

### INWARD_FINAL_BILL
```python
def test_inward_final_bill():
    verifier = HMISPaymentVerifier(BASE_URL, API_KEY)

    result = verifier.verify_bill_by_type_atomic(
        bill_number="INP/2025/0056",
        expected_type_atomic="INWARD_FINAL_BILL"
    )

    # Expected balance changes:
    # - Drawer balance increases
    # - May use patient deposit
    # - Comprehensive service charges
    assert result["status"] == "PASS"
```

### CHANNEL_BOOKING_WITH_PAYMENT_ONLINE_SETTLED
```python
def test_channel_booking():
    verifier = HMISPaymentVerifier(BASE_URL, API_KEY)

    result = verifier.verify_bill_by_type_atomic(
        bill_number="CHANNEL/2025/0089",
        expected_type_atomic="CHANNEL_BOOKING_WITH_PAYMENT_ONLINE_SETTLED"
    )

    # Expected balance changes:
    # - Agent/collecting centre balance increases
    # - Company commission calculated
    # - Doctor fee allocated
    assert result["status"] == "PASS"
```

## Payment Method Test Matrix

### Cash → Drawer Balance
```python
# Test cash payment updates drawer
bill = verifier.get_bill_details("PHARM/2025/0001")
drawer_entries = verifier.get_drawer_entries(bill_id=bill["id"], payment_method="Cash")

assert len(drawer_entries) > 0
assert drawer_entries[0]["paymentMethod"] == "Cash"
assert drawer_entries[0]["afterBalance"] > drawer_entries[0]["beforeBalance"]
```

### Card → Drawer Balance
```python
# Test card payment updates drawer
bill = verifier.get_bill_details("PHARM/2025/0002")
drawer_entries = verifier.get_drawer_entries(bill_id=bill["id"], payment_method="Card")

assert len(drawer_entries) > 0
assert drawer_entries[0]["paymentMethod"] == "Card"
# Verify bank details populated
card_payment = [p for p in bill["payments"] if p["paymentMethod"] == "Card"][0]
assert card_payment["bankId"] is not None
assert card_payment["bankName"] is not None
```

### PatientDeposit → Patient Deposit Balance
```python
# Test patient deposit deduction
bill = verifier.get_bill_details("OPD/2025/0123")
pd_history = verifier.get_patient_deposit_history(bill_id=bill["id"])

assert len(pd_history) > 0
# Balance should decrease
assert pd_history[0]["balanceAfterTransaction"] < pd_history[0]["balanceBeforeTransaction"]
```

### Credit → Credit Company Balance
```python
# Test credit company payment
# NOTE: No history tracking available
bill = verifier.get_bill_details("OPD/2025/0145")
credit_payments = [p for p in bill["payments"] if p["paymentMethod"] == "Credit"]

assert len(credit_payments) > 0
assert credit_payments[0]["creditCompanyId"] is not None
assert credit_payments[0]["creditCompanyName"] is not None
# Manual verification required for balance update
```

### Staff_Welfare → Staff Welfare Balance
```python
# Test staff welfare payment
bill = verifier.get_bill_details("PHARM/2025/0034")
sw_history = verifier.get_staff_welfare_history(bill_id=bill["id"])

assert len(sw_history) > 0
assert sw_history[0]["paymentMethod"] == "Staff_Welfare"
```

### Agent → Agent/Collecting Centre Balance
```python
# Test agent payment
bill = verifier.get_bill_details("CHANNEL/2025/0089")
agent_history = verifier.get_agent_history(bill_id=bill["id"])

assert len(agent_history) > 0
# Verify commission split
assert agent_history[0]["agentBalanceAfter"] > agent_history[0]["agentBalanceBefore"]
```

## Balance Verification Workflows

### Workflow 1: Simple Cash Sale Verification
```python
def workflow_simple_cash_sale(bill_number: str):
    """Complete workflow for verifying a simple cash sale"""

    verifier = HMISPaymentVerifier(BASE_URL, API_KEY)

    # Step 1: Get bill with payments
    bill = verifier.get_bill_details(bill_number)
    print(f"Bill ID: {bill['id']}, Total: {bill['netTotal']}")

    # Step 2: Verify payments array
    assert len(bill['payments']) > 0
    total_paid = sum(p['paidValue'] for p in bill['payments'])
    assert abs(total_paid - bill['netTotal']) < 0.01

    # Step 3: Get drawer entries
    drawer_entries = verifier.get_drawer_entries(bill_id=bill['id'])

    # Step 4: Verify each payment has drawer entry
    for payment in bill['payments']:
        entry = next((e for e in drawer_entries
                     if e['paymentMethod'] == payment['paymentMethod']), None)
        assert entry is not None
        assert abs(entry['transactionValue'] - payment['paidValue']) < 0.01
        assert abs(entry['afterBalance'] - (entry['beforeBalance'] + entry['transactionValue'])) < 0.01

    print("✅ All verifications passed")
```

### Workflow 2: Patient Deposit Verification
```python
def workflow_patient_deposit(bill_number: str):
    """Complete workflow for verifying patient deposit usage"""

    verifier = HMISPaymentVerifier(BASE_URL, API_KEY)

    # Step 1: Get bill with patient deposit payment
    bill = verifier.get_bill_details(bill_number)

    # Step 2: Find patient deposit payment
    pd_payment = next((p for p in bill['payments']
                      if p['paymentMethod'] == 'PatientDeposit'), None)
    assert pd_payment is not None

    # Step 3: Get patient deposit history
    pd_history = verifier.get_patient_deposit_history(bill_id=bill['id'])
    assert len(pd_history) > 0

    # Step 4: Verify balance deduction
    history = pd_history[0]
    assert abs(history['transactionValue'] - pd_payment['paidValue']) < 0.01
    expected_after = history['balanceBeforeTransaction'] - pd_payment['paidValue']
    assert abs(history['balanceAfterTransaction'] - expected_after) < 0.01

    print("✅ Patient deposit verified")
```

### Workflow 3: Multiple Payment Methods
```python
def workflow_multiple_payments(bill_number: str):
    """Complete workflow for bills with multiple payment methods"""

    verifier = HMISPaymentVerifier(BASE_URL, API_KEY)

    # Step 1: Get bill
    bill = verifier.get_bill_details(bill_number)
    assert len(bill['payments']) > 1

    # Step 2: Verify payment total
    total_paid = sum(p['paidValue'] for p in bill['payments'])
    assert abs(total_paid - bill['netTotal']) < 0.01

    # Step 3: Get all relevant histories
    drawer_entries = verifier.get_drawer_entries(bill_id=bill['id'])
    pd_history = verifier.get_patient_deposit_history(bill_id=bill['id'])

    # Step 4: Verify each payment
    for payment in bill['payments']:
        if payment['paymentMethod'] == 'PatientDeposit':
            # Verify in patient deposit history
            entry = next((h for h in pd_history
                         if abs(h['transactionValue'] - payment['paidValue']) < 0.01), None)
        else:
            # Verify in drawer entries
            entry = next((e for e in drawer_entries
                         if e['paymentMethod'] == payment['paymentMethod']), None)

        assert entry is not None, f"No history for {payment['paymentMethod']}"

    print("✅ Multiple payments verified")
```

## Advanced Testing Scenarios

### Testing Refunds
```python
def test_refund_verification(original_bill_number: str, refund_bill_number: str):
    """Verify refund processing"""

    verifier = HMISPaymentVerifier(BASE_URL, API_KEY)

    # Get original bill
    original = verifier.get_bill_details(original_bill_number)

    # Get refund bill
    refund = verifier.get_bill_details(refund_bill_number)

    # Verify refund references original
    assert refund['refundedBillId'] == original['id']

    # Get drawer entries for refund
    refund_entries = verifier.get_drawer_entries(bill_id=refund['id'])

    # Verify negative transaction values (money going out)
    for entry in refund_entries:
        assert entry['transactionValue'] < 0, "Refund should have negative transaction"
        assert entry['afterBalance'] < entry['beforeBalance'], "Balance should decrease"

    print("✅ Refund verified")
```

### Testing Cancelled Bills
```python
def test_cancelled_bill(bill_number: str):
    """Verify cancelled bill has no balance changes"""

    verifier = HMISPaymentVerifier(BASE_URL, API_KEY)

    bill = verifier.get_bill_details(bill_number)
    assert bill['retired'] == True or bill.get('cancelled') == True

    # Cancelled bills should not have active drawer entries
    # (or they should have been reversed)
    drawer_entries = verifier.get_drawer_entries(bill_id=bill['id'])

    if len(drawer_entries) > 0:
        # Check for reversal entries
        print(f"Warning: Cancelled bill has {len(drawer_entries)} drawer entries")
        print("Verify these are reversal entries if applicable")

    print("✅ Cancelled bill verified")
```

### Batch Testing Multiple Bills
```python
def batch_test_bills(bill_numbers: List[str]):
    """Test multiple bills in batch"""

    verifier = HMISPaymentVerifier(BASE_URL, API_KEY)
    results = []

    for bill_number in bill_numbers:
        try:
            result = verifier.verify_cash_sale(bill_number)
            results.append({"bill": bill_number, "status": "PASS", "details": result})
        except AssertionError as e:
            results.append({"bill": bill_number, "status": "FAIL", "error": str(e)})
        except Exception as e:
            results.append({"bill": bill_number, "status": "ERROR", "error": str(e)})

    # Summary
    passed = sum(1 for r in results if r['status'] == 'PASS')
    failed = sum(1 for r in results if r['status'] in ['FAIL', 'ERROR'])

    print(f"\n=== Batch Test Summary ===")
    print(f"Total: {len(results)}")
    print(f"Passed: {passed}")
    print(f"Failed: {failed}")

    return results
```

## Troubleshooting

### Common Issues

#### 1. API Key Not Valid
```python
# Error: {"status": "error", "code": 401, "message": "Not a valid key"}

# Solution: Check API key validity
def check_api_key():
    response = requests.get(
        f"{BASE_URL}/api/costing_data/last_bill",
        headers={"Finance": API_KEY}
    )
    if response.status_code == 401:
        print("❌ API Key is invalid or expired")
    else:
        print("✅ API Key is valid")
```

#### 2. Bill Not Found
```python
# Error: Bill not found: PHARM/2025/9999

# Solution: Verify bill number format
def find_bill_by_partial_number(partial: str):
    # Use ID-based search or verify bill number format
    # Bill numbers are case-sensitive and format-specific
    pass
```

#### 3. No Payments in Bill
```python
# Expected payments but found none

# Check bill type - some bills don't have payments
def check_bill_type(bill_number: str):
    bill = verifier.get_bill_details(bill_number)
    if len(bill.get('payments', [])) == 0:
        print(f"Bill type: {bill['billTypeAtomic']}")
        print("This bill type may not require payments (e.g., transfers, issues)")
```

#### 4. Balance Arithmetic Mismatch
```python
# Drawer entry balance calculation doesn't match

# Verify floating point comparison
def safe_compare(a, b, epsilon=0.01):
    return abs(a - b) < epsilon

# Check for multiple concurrent transactions
def check_concurrent_transactions(drawer_id: int, timestamp: str):
    # Query drawer entries around the same timestamp
    # to identify concurrent balance updates
    pass
```

### Debug Helper Functions

```python
def debug_bill_payments(bill_number: str):
    """Print detailed payment information for debugging"""

    verifier = HMISPaymentVerifier(BASE_URL, API_KEY)
    bill = verifier.get_bill_details(bill_number)

    print(f"\n=== Bill Debug Info: {bill_number} ===")
    print(f"Bill ID: {bill['id']}")
    print(f"Bill Type Atomic: {bill['billTypeAtomic']}")
    print(f"Net Total: {bill['netTotal']}")
    print(f"Payment Method (legacy): {bill.get('paymentMethod')}")

    print(f"\nPayments ({len(bill.get('payments', []))}):")
    for i, payment in enumerate(bill.get('payments', []), 1):
        print(f"  {i}. {payment['paymentMethod']}: {payment['paidValue']}")
        if payment.get('bankName'):
            print(f"     Bank: {payment['bankName']}")
        if payment.get('creditCompanyName'):
            print(f"     Credit Company: {payment['creditCompanyName']}")
        if payment.get('toStaffName'):
            print(f"     Staff: {payment['toStaffName']}")

    print(f"\nDrawer Entries:")
    drawer_entries = verifier.get_drawer_entries(bill_id=bill['id'])
    for entry in drawer_entries:
        print(f"  {entry['paymentMethod']}: {entry['transactionValue']}")
        print(f"    Before: {entry['beforeBalance']}, After: {entry['afterBalance']}")

def compare_expected_vs_actual(bill_number: str, expected_total: float):
    """Compare expected payment total vs actual"""

    verifier = HMISPaymentVerifier(BASE_URL, API_KEY)
    bill = verifier.get_bill_details(bill_number)

    actual_total = sum(p['paidValue'] for p in bill.get('payments', []))

    print(f"\n=== Payment Comparison ===")
    print(f"Expected Total: {expected_total}")
    print(f"Actual Total: {actual_total}")
    print(f"Difference: {abs(expected_total - actual_total)}")

    if abs(expected_total - actual_total) < 0.01:
        print("✅ Match")
    else:
        print("❌ Mismatch")
```

## Notes

- Always use absolute value comparison for floating-point numbers (`abs(a - b) < 0.01`)
- Bill numbers are case-sensitive and must match exactly
- Some bill types don't have payments (transfers, issues)
- Patient deposit transactions have negative `transactionValue` when deducted
- Drawer entries can have negative values for refunds/corrections
- API responses are limited to 100 records by default (use `limit` parameter)
- Date filters use format: `yyyy-MM-dd HH:mm:ss`
- All APIs require the `Finance` header with a valid API key

## Summary

This guide provides complete testing workflows for verifying payment processing in HMIS. The Python code examples are production-ready and can be directly integrated into automated test suites or used for manual verification.

For questions or issues, refer to:
- [API_COSTING_DATA.md](API_COSTING_DATA.md) - Bill API documentation
- [API_BALANCE_HISTORY.md](API_BALANCE_HISTORY.md) - Balance history API documentation
