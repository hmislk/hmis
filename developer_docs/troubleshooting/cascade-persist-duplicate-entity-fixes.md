# Investigating and Fixing Cascade PERSIST Errors and Duplicate Entity Creation

## Overview

This guide helps AI agents diagnose and fix common issues related to:
- Cascade PERSIST errors in JPA/EclipseLink
- Duplicate bill creation
- Duplicate bill item creation
- Duplicate pharmaceutical bill item creation
- Duplicate stock history records

## Understanding the Problem

### What is a Cascade PERSIST Error?

When you see an error like:
```
During synchronization a new object was found through a relationship
that was not marked cascade PERSIST: PharmaceuticalBillItem[id=null]
```

**This means**: An entity is trying to reference another entity that:
1. Has not been persisted to the database yet (id=null)
2. The relationship does NOT have cascade PERSIST configured
3. JPA cannot automatically persist the referenced entity

### Common Scenarios

1. **Entity passed between EJB methods**: Entity is persisted in one method, then passed to another method that creates new entities referencing it - but the original entity hasn't been flushed/committed yet.

2. **Detached entity collections**: Using a controller's list instead of the managed entity's collection causes entities to be detached from persistence context.

3. **Missing bidirectional relationship setup**: One side of the relationship is set, but the other side isn't, causing cascade to fail.

## Diagnostic Steps

### Step 1: Identify the Error Location

Look at the stack trace to find:
1. **Which entity is causing the error** (e.g., PharmaceuticalBillItem[id=null])
2. **Which method is trying to persist it** (e.g., PharmacyBean.addToStockHistory:1238)
3. **Which entity is trying to reference it** (e.g., StockHistory)

Example:
```
WARNING: StandardWrapperValve[Faces Servlet]: Servlet.service() for servlet Faces Servlet threw exception
javax.persistence.EntityNotFoundException: During synchronization a new object was found through a relationship
that was not marked cascade PERSIST: PharmaceuticalBillItem[id=null].
    at com.divudi.ejb.PharmacyBean.addToStockHistory(PharmacyBean.java:1238)
```

**Analysis**: StockHistory is trying to reference PharmaceuticalBillItem, but the PharmaceuticalBillItem has id=null.

### Step 2: Check the Entity Relationships

Examine the entity that's causing the error:

```java
// Example: StockHistory.java
@OneToOne(fetch = FetchType.LAZY)
private PharmaceuticalBillItem pbItem;  // NO CASCADE - This is the problem!
```

**Look for**:
- Is there a `cascade = CascadeType.ALL` or `cascade = CascadeType.PERSIST`?
- If NO cascade is configured, the referenced entity MUST be persisted before this entity can reference it

### Step 3: Trace the Entity Lifecycle

Follow the entity through the code:

1. **Where is it created?** (new PharmaceuticalBillItem())
2. **Where is it first persisted?** (facade.create(), facade.createAndFlush())
3. **Is it reloaded after persistence?** (reload to get the assigned ID)
4. **Where is it passed to another method?** (pharmacyBean.deductFromStock())
5. **Does that method create entities referencing it?** (StockHistory referencing PharmaceuticalBillItem)

### Step 4: Check Transaction Boundaries

Identify EJB transaction boundaries:

```java
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public void addToStockHistory(PharmaceuticalBillItem phItem, Stock stock, Department d) {
    // This runs in a NEW transaction
    // If phItem is not committed in the previous transaction, it will have id=null here
}
```

**Problem**: If the previous transaction hasn't committed, the entity won't have an ID yet.

## Solution Patterns

### Pattern 1: Use createAndFlush() Instead of create()

**Problem**: `create()` doesn't immediately flush to database.

**Solution**:
```java
// BEFORE
billFacade.create(issuedBill);

// AFTER
billFacade.createAndFlush(issuedBill);  // Forces immediate flush, assigns ID
```

### Pattern 2: Use editAndCommit() Instead of edit()

**Problem**: `edit()` doesn't commit the transaction immediately.

**Solution**:
```java
// BEFORE
billFacade.edit(issuedBill);

// AFTER
billFacade.editAndCommit(issuedBill);  // Commits transaction, ensures all entities get IDs
```

### Pattern 3: Reload Entity After Commit

**Problem**: Entity might not have IDs for nested children even after commit.

**Solution**:
```java
saveBill();
saveBillItems();

// Reload the bill to ensure all PharmaceuticalBillItems have been persisted with IDs
issuedBill = billService.reloadBill(issuedBill.getId());

updateStocks();  // Now this can safely reference PharmaceuticalBillItems
```

### Pattern 4: Use Managed Collections Instead of Detached Lists

**Problem**: Using controller's list instead of entity's managed collection.

**Solution**:
```java
// BEFORE - Using detached controller list
for (BillItem i : getBillItems()) {  // Controller's list - detached!
    i.setBill(issuedBill);
    billItemFacade.create(i);
}

// AFTER - Using managed entity collection
issuedBill.getBillItems().clear();  // Clear managed collection
for (BillItem i : getBillItems()) {
    i.setBill(issuedBill);
    issuedBill.getBillItems().add(i);  // Add to managed collection
}
billFacade.editAndCommit(issuedBill);  // Cascade handles the rest
```

### Pattern 5: Ensure Bidirectional Relationships

**Problem**: Only one side of the relationship is set.

**Solution**:
```java
// BEFORE
i.setBill(issuedBill);

// AFTER - Set BOTH sides
i.setBill(issuedBill);
if (i.getPharmaceuticalBillItem() != null) {
    i.getPharmaceuticalBillItem().setBillItem(i);  // Set reverse relationship
}
```

## Complete Fix Example: Transfer Issue Controller

This is a real-world example from TransferIssueDirectController.java that fixed a cascade PERSIST error:

### Problem
```
javax.persistence.EntityNotFoundException: During synchronization a new object was found through a relationship
that was not marked cascade PERSIST: PharmaceuticalBillItem[id=null].
    at com.divudi.ejb.PharmacyBean.addToStockHistory(PharmacyBean.java:1238)
```

### Root Cause Analysis

1. `settleDirectIssue()` calls `saveBill()`, then `saveBillItems()`, then `updateStocks()`
2. `updateStocks()` calls `pharmacyBean.deductFromStock()`
3. `deductFromStock()` calls `addToStockHistory(pbi, stock, d)` in a NEW transaction
4. `addToStockHistory()` creates StockHistory and sets `sh.setPbItem(phItem)`
5. StockHistory has NO CASCADE on pbItem relationship
6. PharmaceuticalBillItem wasn't fully committed with ID before being passed to addToStockHistory()

### Solution Implementation

#### Step 1: Change create() to createAndFlush() in saveBill()

```java
// File: TransferIssueDirectController.java
// Lines: 254-308

private void saveBill() {
    if (issuedBill == null) {
        issuedBill = new Bill();
        issuedBill.setBillType(BillType.PharmacyTransferIssue);
    }

    if (issuedBill.getId() == null) {
        issuedBill.setDepartment(sessionController.getDepartment());
        issuedBill.setInstitution(sessionController.getInstitution());
        issuedBill.setToInstitution(issuedBill.getToDepartment().getInstitution());
        issuedBill.setCreater(getSessionController().getLoggedUser());
        issuedBill.setCreatedAt(Calendar.getInstance().getTime());
        getBillFacade().createAndFlush(issuedBill);  // CHANGED: Forces immediate flush
    }
}
```

#### Step 2: Use managed collection and editAndCommit() in saveBillItems()

```java
// File: TransferIssueDirectController.java
// Lines: 310-331

private void saveBillItems() {
    // CHANGED: Use managed collection instead of detached controller list
    issuedBill.getBillItems().clear();

    for (BillItem i : getBillItems()) {
        i.getPharmaceuticalBillItem().setQty(0 - Math.abs(i.getPharmaceuticalBillItem().getQty()));

        // ADDED: Ensure bidirectional relationship is properly set
        if (i.getPharmaceuticalBillItem() != null) {
            i.getPharmaceuticalBillItem().setBillItem(i);
        }

        i.setBill(issuedBill);
        if (i.getId() == null) {
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
        }
        issuedBill.getBillItems().add(i);  // Add to managed collection
    }
    // CHANGED: Use editAndCommit to flush changes to database
    billFacade.editAndCommit(issuedBill);
}
```

#### Step 3: Reload bill after commit in settleDirectIssue()

```java
// File: TransferIssueDirectController.java
// Lines: 226-232

public void settleDirectIssue() {
    if (errorCheck()) {
        return;
    }

    saveBill();
    saveBillItems();

    // ADDED: Reload the bill to ensure all PharmaceuticalBillItems have been persisted with IDs
    issuedBill = billService.reloadBill(issuedBill.getId());

    updateStocks();

    // ... rest of method
}
```

### Why This Works

1. **createAndFlush()**: Ensures Bill gets an ID immediately
2. **Managed collection**: Cascade properly manages BillItem and PharmaceuticalBillItem persistence
3. **editAndCommit()**: Forces transaction commit, assigning IDs to all entities
4. **Reload**: Refreshes entity from database, ensuring all nested children have IDs
5. **updateStocks() can now safely pass PharmaceuticalBillItems** to pharmacyBean methods that create StockHistory records

## Investigating Duplicate Bill/BillItem Creation

### Symptoms

1. Same bill appears twice in reports
2. Same bill item appears multiple times
3. "Duplicate entries until next run" in reports

### Diagnostic Steps

#### 1. Check for Multiple create() Calls

Search for duplicate creation logic:

```bash
# Find all create calls for Bill
grep -n "billFacade.create" ControllerName.java

# Find all createAndFlush calls
grep -n "createAndFlush" ControllerName.java
```

**Look for**:
- Multiple create() calls in the same method
- Create() called in loops without proper checks
- Create() called in both controller and EJB methods

#### 2. Check for Missing ID Checks

```java
// BAD - Always creates new bill
billFacade.create(bill);

// GOOD - Only creates if new
if (bill.getId() == null) {
    billFacade.create(bill);
} else {
    billFacade.edit(bill);
}
```

#### 3. Check Transaction Boundaries

```java
// BAD - Creates new bill in REQUIRES_NEW transaction every time
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public void saveBill(Bill bill) {
    billFacade.create(bill);  // Always creates!
}

// GOOD - Checks for existing bill first
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public void saveBill(Bill bill) {
    if (bill.getId() == null) {
        billFacade.create(bill);
    } else {
        billFacade.edit(bill);
    }
}
```

#### 4. Check for Duplicate Collection Additions

```java
// BAD - Doesn't clear existing items
for (BillItem item : newItems) {
    bill.getBillItems().add(item);  // Might add duplicates!
}

// GOOD - Clears before adding
bill.getBillItems().clear();
for (BillItem item : newItems) {
    bill.getBillItems().add(item);
}
```

### Common Fixes for Duplicates

#### Fix 1: Add Proper ID Checks

```java
public void saveBill() {
    if (currentBill.getId() == null) {
        billFacade.create(currentBill);
    } else {
        billFacade.edit(currentBill);
    }
}
```

#### Fix 2: Clear Collections Before Repopulating

```java
public void saveBillItems() {
    currentBill.getBillItems().clear();  // Prevent duplicates

    for (BillItem item : getNewItems()) {
        currentBill.getBillItems().add(item);
    }

    billFacade.edit(currentBill);
}
```

#### Fix 3: Use orphanRemoval for Child Entities

```java
// In Bill.java
@OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
private List<BillItem> billItems;
```

**This ensures**: When you clear() the collection, orphaned BillItems are automatically deleted.

#### Fix 4: Avoid Multiple Saves in Same Method

```java
// BAD - Saves multiple times
public void processBill() {
    billFacade.create(bill);
    // ... some logic
    billFacade.edit(bill);  // Might create duplicate if first save failed partially
}

// GOOD - Single save at end
public void processBill() {
    // ... all logic
    if (bill.getId() == null) {
        billFacade.create(bill);
    } else {
        billFacade.edit(bill);
    }
}
```

## Special Case: Stock History Duplicates

### Expected vs Unexpected Duplicates

#### Expected: Transfer Transactions Create TWO Records

```java
// This is CORRECT behavior for transfers
// One OUT record from source department
StockHistory outRecord = new StockHistory();
outRecord.setQty(-10);  // Deduction
outRecord.setDepartment(sourceDepartment);

// One IN record for destination department
StockHistory inRecord = new StockHistory();
inRecord.setQty(+10);  // Addition
inRecord.setDepartment(destinationDepartment);
```

**Result**: Stock ledger will show both records - this is correct accounting!

#### Unexpected: Same Transaction Creates Multiple OUT or IN Records

**Symptom**: Stock ledger shows:
- Same bill number with same quantity deducted twice
- Same item added to stock twice with same reference

**Diagnosis**:
1. Check if `updateStocks()` or equivalent is called multiple times
2. Check if stock update logic is in a loop without proper guards
3. Check for duplicate EJB method calls

## Quick Reference Checklist

When fixing cascade PERSIST or duplicate creation issues:

- [ ] Identified which entity has id=null
- [ ] Checked if relationship has cascade configured
- [ ] Changed create() to createAndFlush() if needed
- [ ] Changed edit() to editAndCommit() if needed
- [ ] Added reload after commit if entity is passed to other methods
- [ ] Using managed collections instead of detached lists
- [ ] Set both sides of bidirectional relationships
- [ ] Added proper id==null checks before create()
- [ ] Cleared collections before repopulating
- [ ] Verified orphanRemoval is configured if needed
- [ ] Checked for multiple create() calls
- [ ] Verified transaction boundaries are correct
- [ ] Confirmed expected duplicates (like transfer IN/OUT) vs unexpected duplicates

## Key Entity Relationships Reference

### Bill → BillItem → PharmaceuticalBillItem Cascade Chain

```java
// Bill.java - Line 102-104
@OneToMany(mappedBy = "bill", cascade = CascadeType.ALL,
           fetch = FetchType.LAZY, orphanRemoval = true)
private List<BillItem> billItems;

// BillItem.java - Line 55-56
@OneToOne(mappedBy = "billItem", fetch = FetchType.EAGER,
          cascade = CascadeType.ALL)
private PharmaceuticalBillItem pharmaceuticalBillItem;

// BillItem.java - Lines 475-481 (Auto-creates if null)
public PharmaceuticalBillItem getPharmaceuticalBillItem() {
    if (pharmaceuticalBillItem == null) {
        pharmaceuticalBillItem = new PharmaceuticalBillItem();
    }
    return pharmaceuticalBillItem;
}
```

**Important**: Cascade flows Bill → BillItem → PharmaceuticalBillItem. BUT:

```java
// StockHistory.java - Line 64-65
@OneToOne(fetch = FetchType.LAZY)
private PharmaceuticalBillItem pbItem;  // NO CASCADE!
```

**This means**: PharmaceuticalBillItem MUST be persisted with an ID before StockHistory can reference it.

## Debugging Commands

### Find all create() calls in a controller
```bash
grep -n "Facade.create(" src/main/java/com/divudi/bean/pharmacy/ControllerName.java
```

### Find all edit() calls
```bash
grep -n "Facade.edit(" src/main/java/com/divudi/bean/pharmacy/ControllerName.java
```

### Find transaction boundaries
```bash
grep -n "@TransactionAttribute" src/main/java/com/divudi/ejb/BeanName.java
```

### Check cascade configuration in entity
```bash
grep -A 2 "@OneToMany\|@OneToOne\|@ManyToOne" src/main/java/com/divudi/core/entity/EntityName.java
```

## Additional Resources

- JPA Cascade Types: https://docs.oracle.com/javaee/7/api/javax/persistence/CascadeType.html
- EclipseLink Flush Modes: https://wiki.eclipse.org/EclipseLink/UserGuide/JPA/Basic_JPA_Development/Persisting/Flush
- Transaction Attributes: https://docs.oracle.com/javaee/7/api/javax/ejb/TransactionAttributeType.html

## Summary

**Key Principles**:
1. Entities must have IDs before being referenced by entities without cascade
2. Use managed collections for cascade to work properly
3. Set both sides of bidirectional relationships
4. Reload entities after commit when passing to other EJB methods
5. Clear collections before repopulating to avoid duplicates
6. Always check id==null before create()
7. Use createAndFlush() and editAndCommit() when immediate persistence is needed
8. Understand transaction boundaries - REQUIRES_NEW creates new context

**When in doubt**: Reload the entity after commit, before passing it to another method.
