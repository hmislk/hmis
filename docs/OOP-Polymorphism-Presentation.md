# **Polymorphism in OOP: Theory & Real-World Examples from HMIS**

## **Presentation for CareCode Software Engineering Interns**
*Date: December 1, 2025*

---

## **📚 Table of Contents**

1. [What is Polymorphism?](#1-what-is-polymorphism)
2. [Types of Polymorphism](#2-types-of-polymorphism)
3. [Good Examples from HMIS Codebase](#3-good-examples-from-hmis-codebase)
4. [Bad Examples & Anti-Patterns](#4-bad-examples--anti-patterns)
5. [Comparison Summary](#5-comparison-summary)
6. [Key Lessons & Best Practices](#6-key-lessons--best-practices)
7. [Real-World Impact in HMIS](#7-real-world-impact-in-hmis)
8. [Practice Exercise](#8-practice-exercise)
9. [Additional Resources](#9-additional-resources)
10. [Key Takeaways](#10-key-takeaways)

---

## **1️⃣ What is Polymorphism?**

### **Definition**
> **Polymorphism** = "Many Forms" (Greek: poly = many, morph = forms)

**In OOP:** The ability of objects to take on many forms or behave differently based on their specific type.

### **Core Concept**
- Same interface, different implementations
- Write code that works with a general type (interface/abstract class)
- Actual behavior determined by the specific object type at runtime

### **Why Does It Matter?**
- **Extensibility**: Add new types without changing existing code
- **Maintainability**: One change fixes behavior everywhere
- **Testability**: Test each implementation independently
- **Code Reduction**: Eliminate duplicate logic

---

## **2️⃣ Types of Polymorphism**

### **A. Compile-Time Polymorphism (Static)**

**Also known as:** Method Overloading

**Characteristics:**
- Resolved at compile time
- Same method name, different parameters
- Faster execution (resolved during compilation)

**Example:**
```java
public class Calculator {
    // Same method name, different parameters
    public int add(int a, int b) {
        return a + b;
    }

    public double add(double a, double b) {
        return a + b;
    }

    public int add(int a, int b, int c) {
        return a + b + c;
    }
}

// Usage:
Calculator calc = new Calculator();
calc.add(5, 10);           // Calls int version
calc.add(5.5, 10.3);       // Calls double version
calc.add(5, 10, 15);       // Calls three-parameter version
```

---

### **B. Runtime Polymorphism (Dynamic)**

**Also known as:** Method Overriding

**Characteristics:**
- Resolved at runtime
- Parent class reference, child class object
- Slower execution (JVM determines at runtime)
- More flexible and powerful

**Example:**
```java
// Parent class
public class Animal {
    public void makeSound() {
        System.out.println("Some generic sound");
    }
}

// Child classes override the method
public class Dog extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Woof! Woof!");
    }
}

public class Cat extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Meow!");
    }
}

// Runtime polymorphism in action:
Animal myPet;              // Parent class reference

myPet = new Dog();         // Child class object
myPet.makeSound();         // Output: "Woof! Woof!"

myPet = new Cat();         // Different child class object
myPet.makeSound();         // Output: "Meow!"
```

**Key Point:** The JVM determines which `makeSound()` to call at **runtime** based on the actual object type, not the reference type.

---

## **3️⃣ ✅ Good Examples from HMIS Codebase**

### **Example 1: Bill Inheritance Hierarchy**

**Files:**
- `src/main/java/com/divudi/core/entity/Bill.java:51`
- `src/main/java/com/divudi/core/entity/BilledBill.java:15-24`
- `src/main/java/com/divudi/core/entity/PreBill.java:12-19`
- `src/main/java/com/divudi/core/entity/RefundBill.java`
- `src/main/java/com/divudi/core/entity/CancelledBill.java`

**Code:**
```java
// Base Class - Bill.java:51
public class Bill implements Serializable, RetirableEntity {
    @Enumerated(EnumType.STRING)
    protected BillClassType billClassType;
    protected double qty;
    // ... other common fields
}

// Concrete Implementation 1
@Entity
public class BilledBill extends Bill implements Serializable {
    public BilledBill() {
        super();
        billClassType = BillClassType.BilledBill;
        qty = 1;  // Positive quantity
    }
}

// Concrete Implementation 2
@Entity
public class CancelledBill extends Bill implements Serializable {
    public CancelledBill() {
        super();
        billClassType = BillClassType.CancelledBill;
        qty = -1;  // Negative quantity for cancellation
    }
}

// Usage - Polymorphic behavior
public void processBills(List<Bill> bills) {
    for (Bill bill : bills) {  // Works with any Bill subtype
        double qty = bill.getQty();
        // BilledBill returns 1, CancelledBill returns -1
        // Behavior changes based on actual object type!
    }
}
```

**✅ Why This is Good:**
- Multiple bill types share common behavior (inherited from `Bill`)
- Each subclass specializes behavior (different qty values)
- Can process all bills uniformly via `Bill` reference
- Easy to add new bill types (just extend `Bill`)
- Follows Open-Closed Principle

---

### **Example 2: Interface-Based Design (Controller Pattern)**

**File:** `src/main/java/com/divudi/bean/common/ControllerWithPatient.java:6-23`

**Code:**
```java
// Interface defining contract
public interface ControllerWithPatient {
    Patient getPatient();
    void setPatient(Patient patient);
    boolean isPatientDetailsEditable();
    void setPatientDetailsEditable(boolean patientDetailsEditable);
    void toggalePatientEditable();
    void setPaymentMethod(PaymentMethod paymentMethod);
    PaymentMethod getPaymentMethod();
    void listnerForPaymentMethodChange();
}

// Multiple unrelated controllers implement the same interface
@Named
@SessionScoped
public class OpdBillController implements ControllerWithPatient {
    private Patient patient;

    @Override
    public Patient getPatient() { return patient; }

    @Override
    public void setPatient(Patient patient) { this.patient = patient; }

    // ... implement other methods
}

@Named
@SessionScoped
public class PharmacySaleController implements ControllerWithPatient {
    private Patient patient;

    @Override
    public Patient getPatient() { return patient; }

    // ... implement other methods differently
}

// Polymorphic usage
public void updatePatientAcrossControllers(
        List<ControllerWithPatient> controllers, Patient newPatient) {
    for (ControllerWithPatient controller : controllers) {
        controller.setPatient(newPatient);  // Works with any controller!
    }
}
```

**✅ Why This is Good:**
- Defines clear contract for patient-handling controllers
- Multiple unrelated classes can implement same interface
- Code can work with any controller that handles patients
- Supports composition over inheritance
- Flexible and extensible

---

### **Example 3: Strategy Pattern (Validators)**

**Files:**
- `src/main/java/com/divudi/core/validator/NonNegativeNumberValidator.java:20-50`
- `src/main/java/com/divudi/core/validator/PositiveNumberValidator.java:20-50`

**Code:**
```java
// Common interface (from javax.faces.validator)
public interface Validator {
    void validate(FacesContext context, UIComponent component, Object value)
        throws ValidatorException;
}

// Strategy 1: Non-negative validation
@FacesValidator("nonNegativeNumberValidator")
public class NonNegativeNumberValidator implements Validator {
    @Override
    public void validate(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {
        if (value == null) return;

        BigDecimal numberValue = convertToDecimal(value);

        if (numberValue.compareTo(BigDecimal.ZERO) < 0) {
            FacesMessage msg = new FacesMessage("Invalid Quantity",
                "Quantity cannot be negative");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }
}

// Strategy 2: Positive (strictly greater than zero)
@FacesValidator("positiveNumberValidator")
public class PositiveNumberValidator implements Validator {
    @Override
    public void validate(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {
        if (value == null) return;

        BigDecimal numberValue = convertToDecimal(value);

        if (numberValue.compareTo(BigDecimal.ZERO) <= 0) {  // Note: <= instead of <
            FacesMessage msg = new FacesMessage("Invalid Quantity",
                "Quantity must be positive");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }
}

// Framework uses polymorphism - doesn't care which validator
<h:inputText value="#{bean.quantity}"
             validator="nonNegativeNumberValidator" />

<h:inputText value="#{bean.price}"
             validator="positiveNumberValidator" />
```

**✅ Why This is Good:**
- Encapsulates different validation rules in separate classes
- Each validator has single responsibility
- Easy to add new validation rules (just implement `Validator`)
- Can be used interchangeably wherever `Validator` is expected
- Perfect example of Strategy Pattern

---

### **Example 4: Template Method Pattern (Abstract Facade)**

**File:** `src/main/java/com/divudi/core/facade/AbstractFacade.java:34-342`

**Code:**
```java
// Abstract base class with template methods
public abstract class AbstractFacade<T> {
    private Class<T> entityClass;

    // Template Method Pattern: subclasses must provide EntityManager
    protected abstract EntityManager getEntityManager();

    // Common CRUD operations using the abstract method
    public void create(T entity) {
        getEntityManager().persist(entity);  // Uses subclass's EntityManager
    }

    public void edit(T entity) {
        getEntityManager().merge(entity);
    }

    public T find(Object id) {
        if (id == null) return null;
        try {
            return getEntityManager().find(entityClass, id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public List<T> findAll() {
        javax.persistence.criteria.CriteriaBuilder cb =
            getEntityManager().getCriteriaBuilder();
        javax.persistence.criteria.CriteriaQuery<T> cq =
            cb.createQuery(entityClass);
        return getEntityManager().createQuery(cq).getResultList();
    }

    // ... 20+ more common database operations
}

// Concrete facades just provide EntityManager
@Stateless
public class BillFacade extends AbstractFacade<Bill> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;  // That's it! Gets all CRUD operations for free
    }
}

@Stateless
public class PatientFacade extends AbstractFacade<Patient> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    // Can add Patient-specific methods here
    public List<Patient> findByName(String name) {
        // Custom query for patients only
    }
}
```

**✅ Why This is Good:**
- Eliminates massive code duplication (20+ methods in every facade)
- Type-safe generic implementation
- Template Method Pattern: base class defines algorithm, subclasses provide details
- All facades get consistent behavior automatically
- Easy to add new entity facades

**📊 Impact:** Without this polymorphism, every facade would duplicate ~300 lines of CRUD code!

---

## **4️⃣ ❌ Bad Examples & Anti-Patterns**

Now let's look at real code that **works** but could be **significantly improved** with polymorphism.

---

### **Anti-Pattern 1: instanceof Chains (CRITICAL)**

**File:** `src/main/java/com/divudi/ws/channel/ChannelApi.java:3554-3627`

**Also found in:**
- `src/main/java/com/divudi/bean/clinic/ClinicController.java:6243-6270`
- `src/main/java/com/divudi/bean/channel/PastBookingController.java:2040-2144`
- `src/main/java/com/divudi/bean/channel/ChannelReportTemplateController.java:4951-4970`
- 6+ more files

**❌ Current Code:**
```java
private String generateBillNumberInsId(Bill bill, ServiceSession ss) {
    String suffix = ss.getInstitution().getCode();
    BillClassType billClassType = null;
    BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash,
                           BillType.ChannelOnCall, BillType.ChannelStaff};
    List<BillType> bts = Arrays.asList(billTypes);
    BillType billType = null;
    String insId = null;

    // Anti-pattern: Type checking instead of polymorphism
    if (bill instanceof BilledBill) {
        billClassType = BillClassType.BilledBill;
        if (bill.getBillType() == BillType.ChannelOnCall ||
            bill.getBillType() == BillType.ChannelStaff) {
            billType = bill.getBillType();
            if (billType == BillType.ChannelOnCall) {
                suffix += "COS";
            } else {
                suffix += "CS";
            }
            insId = getBillNumberBean().institutionBillNumberGenerator(
                ss.getInstitution(), billType, billClassType, suffix);
        } else {
            suffix += "C";
            insId = getBillNumberBean().institutionBillNumberGenerator(
                ss.getInstitution(), bts, billClassType, suffix);
        }
    }

    if (bill instanceof CancelledBill) {
        suffix += "CC";
        billClassType = BillClassType.CancelledBill;
        insId = getBillNumberBean().institutionBillNumberGenerator(
            ss.getInstitution(), bts, billClassType, suffix);
    }

    if (bill instanceof RefundBill) {
        suffix += "CF";
        billClassType = BillClassType.RefundBill;
        insId = getBillNumberBean().institutionBillNumberGenerator(
            ss.getInstitution(), bts, billClassType, suffix);
    }

    return insId;
}
```

**🚨 Problems:**
1. Multiple `instanceof` checks violate OOP principles
2. Same pattern duplicated in `generateBillNumberDeptId()` method
3. This pattern appears in **10+ controller classes**
4. Adding new bill type requires modifying multiple files
5. Violates Open-Closed Principle (not open for extension)
6. Type casting after instanceof is error-prone

---

**✅ Better Solution Using Polymorphism:**

```java
// Add abstract methods to Bill base class
public abstract class Bill implements Serializable, RetirableEntity {
    // Let each subclass define its own suffix logic
    public abstract String getNumberSuffix();

    public abstract BillClassType getBillClassType();

    // Common fields...
}

// BilledBill knows how to generate its own suffix
public class BilledBill extends Bill {
    @Override
    public String getNumberSuffix() {
        if (getBillType() == BillType.ChannelOnCall) {
            return "COS";
        } else if (getBillType() == BillType.ChannelStaff) {
            return "CS";
        }
        return "C";
    }

    @Override
    public BillClassType getBillClassType() {
        return BillClassType.BilledBill;
    }
}

// CancelledBill knows its own suffix
public class CancelledBill extends Bill {
    @Override
    public String getNumberSuffix() {
        return "CC";
    }

    @Override
    public BillClassType getBillClassType() {
        return BillClassType.CancelledBill;
    }
}

// RefundBill knows its own suffix
public class RefundBill extends Bill {
    @Override
    public String getNumberSuffix() {
        return "CF";
    }

    @Override
    public BillClassType getBillClassType() {
        return BillClassType.RefundBill;
    }
}

// Refactored method - NO instanceof checks!
private String generateBillNumberInsId(Bill bill, ServiceSession ss) {
    String suffix = ss.getInstitution().getCode() + bill.getNumberSuffix();

    BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash,
                           BillType.ChannelOnCall, BillType.ChannelStaff};

    return getBillNumberBean().institutionBillNumberGenerator(
        ss.getInstitution(),
        Arrays.asList(billTypes),
        bill.getBillClassType(),  // Polymorphic call
        suffix);
}
```

**✅ Benefits:**
- One method instead of 10+ duplicated versions
- No `instanceof` checks
- Each bill type encapsulates its own behavior
- Easy to add new bill types (just extend Bill and implement methods)
- Follows Single Responsibility Principle
- Reduces code from ~70 lines to ~10 lines per usage

**📊 Impact:** This refactoring would eliminate ~600 lines of duplicated code across the codebase!

---

### **Anti-Pattern 2: Repeated instanceof for Item Types**

**Files:**
- `src/main/java/com/divudi/service/pharmacy/PharmacyCostingService.java:101-116, 200-215, 549-561`
- `src/main/java/com/divudi/ejb/PrescriptionToItemService.java:88-96, 160-166`
- `src/main/java/com/divudi/ejb/PharmacyService.java:115-121, 156-166`
- 5+ more files

**❌ Current Code:**
```java
// This pattern repeats in 8+ different files!
Amp amp = null;
Vmp vmp = null;
if (item instanceof Ampp) {
    amp = ((Ampp) item).getAmp();
} else if (item instanceof Amp) {
    amp = (Amp) item;
} else if (item instanceof Vmp) {
    vmp = (Vmp) item;
}

// Another variant from PharmacyCostingService.java:101-116
BigDecimal unitsPerPack;
if (item instanceof Ampp) {
    double dblVal = item.getDblValue();
    unitsPerPack = dblVal > 0.0 ? BigDecimal.valueOf(dblVal) : BigDecimal.ONE;
    qtyInUnits = qty.multiply(unitsPerPack);
    freeQtyInUnits = freeQty.multiply(unitsPerPack);
    totalQtyInUnits = totalQty.multiply(unitsPerPack);
    prPerUnit = lineGrossRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP)
        .doubleValue();
    rrPerUnit = retailRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP)
        .doubleValue();
} else {
    unitsPerPack = BigDecimal.ONE;
    qtyInUnits = qty;
    freeQtyInUnits = freeQty;
    totalQtyInUnits = totalQty;
    prPerUnit = lineGrossRate.doubleValue();
    rrPerUnit = retailRate.doubleValue();
}
```

**🚨 Problems:**
1. Exact same pattern duplicated 8+ times
2. Every new method handling items needs this check
3. If logic changes (e.g., handle Vmpp), must update all locations
4. Violates DRY (Don't Repeat Yourself)

**Item Hierarchy:**
```
Item (abstract)
├── Amp (Actual Medicinal Product)
├── Vmp (Virtual Medicinal Product)
├── Ampp (Amp with packaging) ← has packaging multiplier
├── Vmpp (Vmp with packaging)
├── Atm (Actual Therapeutic Moiety)
└── Vtm (Virtual Therapeutic Moiety)
```

---

**✅ Better Solution Using Polymorphism:**

```java
// Add interface to Item hierarchy
public interface PharmaceuticalItem {
    Amp getAsAmp();
    Vmp getAsVmp();
    BigDecimal getUnitsPerPack();

    default BigDecimal convertQuantityToUnits(BigDecimal quantity) {
        return quantity.multiply(getUnitsPerPack());
    }
}

// Ampp implementation
public class Ampp extends Item implements PharmaceuticalItem {
    private Amp amp;

    @Override
    public Amp getAsAmp() {
        return amp;
    }

    @Override
    public Vmp getAsVmp() {
        return amp != null ? amp.getVmp() : null;
    }

    @Override
    public BigDecimal getUnitsPerPack() {
        double dblVal = getDblValue();
        return dblVal > 0.0 ? BigDecimal.valueOf(dblVal) : BigDecimal.ONE;
    }
}

// Amp implementation
public class Amp extends Item implements PharmaceuticalItem {
    private Vmp vmp;

    @Override
    public Amp getAsAmp() {
        return this;
    }

    @Override
    public Vmp getAsVmp() {
        return vmp;
    }

    @Override
    public BigDecimal getUnitsPerPack() {
        return BigDecimal.ONE;  // No packaging multiplier
    }
}

// Vmp implementation
public class Vmp extends Item implements PharmaceuticalItem {
    @Override
    public Amp getAsAmp() {
        return null;  // VMP doesn't have AMP
    }

    @Override
    public Vmp getAsVmp() {
        return this;
    }

    @Override
    public BigDecimal getUnitsPerPack() {
        return BigDecimal.ONE;
    }
}

// Refactored service method - NO instanceof!
public void recalculateFinancialsBeforeAddingBillItem(
        BillItemFinanceDetails billItemFinanceDetails) {
    Item item = billItemFinanceDetails.getBillItem().getItem();

    // Use polymorphism - works for all item types!
    BigDecimal unitsPerPack = (item instanceof PharmaceuticalItem)
        ? ((PharmaceuticalItem) item).getUnitsPerPack()
        : BigDecimal.ONE;

    BigDecimal qtyInUnits = qty.multiply(unitsPerPack);
    BigDecimal freeQtyInUnits = freeQty.multiply(unitsPerPack);
    BigDecimal totalQtyInUnits = totalQty.multiply(unitsPerPack);

    double prPerUnit = lineGrossRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP)
        .doubleValue();
    double rrPerUnit = retailRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP)
        .doubleValue();

    // ... rest of method
}
```

**✅ Benefits:**
- Each item type knows its own conversion logic
- No repeated instanceof chains
- One place to change conversion logic per item type
- Type-safe polymorphic calls

---

### **Anti-Pattern 3: Massive Code Duplication (300+ Lines!)**

**File:** `src/main/java/com/divudi/service/pharmacy/PharmacyCostingService.java`

**Four Nearly Identical Methods:**
1. `calculateBillTotalsFromItemsForPurchases` (Line 621, ~100 lines)
2. `calculateBillTotalsFromItemsForGrnReturns` (Line 826, ~100 lines)
3. `calculateBillTotalsFromItemsForTransferOuts` (Line 1031, ~100 lines)
4. `calculateBillTotalsFromItemsForDisposalIssue` (Line 1237, ~100 lines)

**❌ Current Code Structure (simplified):**
```java
// Method 1: For Purchases
public void calculateBillTotalsFromItemsForPurchases(Bill bill,
        List<BillItem> billItems) {
    // Common initialization (duplicated 4 times)
    BigDecimal billDiscount = BigDecimal.valueOf(bill.getDiscount());
    BigDecimal billExpense = BigDecimal.valueOf(bill.getExpensesTotalConsideredForCosting());
    BigDecimal billTax = BigDecimal.valueOf(bill.getTax());
    BigDecimal billCost = billDiscount.subtract(billExpense.add(billTax));

    BigDecimal totalLineDiscounts = BigDecimal.ZERO;
    BigDecimal totalLineExpenses = BigDecimal.ZERO;
    BigDecimal totalLineCosts = BigDecimal.ZERO;
    // ... 12 more similar variables

    // Loop through items (duplicated 4 times with minor variations)
    for (BillItem bi : billItems) {
        PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();

        // Purchase-specific calculations
        // ~50 lines of logic
    }

    // Final bill updates (duplicated 4 times)
    bill.setTotal(...);
    bill.setTax(...);
    bill.setDiscount(...);
    // ... more setters
}

// Methods 2, 3 & 4: SAME PATTERN REPEATED THREE MORE TIMES!
```

**🚨 Problems:**
1. **~300 lines of duplicated code** (4 methods × ~75 lines each)
2. Bug fix requires changes in 4 places
3. Optimization must be done 4 times
4. Hard to maintain consistency
5. Violates DRY principle severely
6. Only difference is ~10 lines in the middle of each method

---

**✅ Better Solution Using Strategy Pattern:**

```java
// Strategy interface for different bill calculation types
public interface BillCalculationStrategy {
    void initializeBill(Bill bill, PharmacyCostingService service);

    void processBillItem(BillItem bi,
                        PharmaceuticalBillItem pbi,
                        BillItemFinanceDetails f,
                        BillCalculationContext context);

    void finalizeBill(Bill bill, BillCalculationContext context);
}

// Context holds calculation state
public class BillCalculationContext {
    public BigDecimal totalLineDiscounts = BigDecimal.ZERO;
    public BigDecimal totalLineExpenses = BigDecimal.ZERO;
    public BigDecimal totalLineCosts = BigDecimal.ZERO;
    // ... all accumulator variables
}

// Strategy 1: Purchase Bills
public class PurchaseBillCalculator implements BillCalculationStrategy {
    @Override
    public void initializeBill(Bill bill, PharmacyCostingService service) {
        // Purchase-specific initialization
    }

    @Override
    public void processBillItem(BillItem bi, PharmaceuticalBillItem pbi,
                               BillItemFinanceDetails f,
                               BillCalculationContext context) {
        // Purchase-specific item processing (~10 unique lines)
        BigDecimal itemCost = f.getPurchaseRate().multiply(f.getQty());
        context.totalLineCosts = context.totalLineCosts.add(itemCost);
        // ... purchase calculations
    }

    @Override
    public void finalizeBill(Bill bill, BillCalculationContext context) {
        // Purchase-specific finalization
    }
}

// Strategy 2: GRN Return Bills
public class GrnReturnBillCalculator implements BillCalculationStrategy {
    @Override
    public void processBillItem(BillItem bi, PharmaceuticalBillItem pbi,
                               BillItemFinanceDetails f,
                               BillCalculationContext context) {
        // GRN return calculations (different from purchase)
        BigDecimal returnCost = f.getPurchaseRate().multiply(f.getQty()).negate();
        context.totalLineCosts = context.totalLineCosts.add(returnCost);
        // ... return calculations
    }
    // ... other methods
}

// Strategies 3 & 4: Similar implementations for TransferOut and Disposal

// Single generic method replaces all four!
public void calculateBillTotals(Bill bill,
                               List<BillItem> billItems,
                               BillCalculationStrategy strategy) {
    // Common initialization (written once)
    BillCalculationContext context = new BillCalculationContext();
    strategy.initializeBill(bill, this);

    // Common loop (written once)
    for (BillItem bi : billItems) {
        PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();

        // Strategy determines specific behavior
        strategy.processBillItem(bi, pbi, f, context);
    }

    // Common finalization (written once)
    strategy.finalizeBill(bill, context);

    // Common bill updates (written once)
    bill.setTotal(context.totalLineCosts.doubleValue());
    bill.setTax(context.totalTaxLines.doubleValue());
    bill.setDiscount(context.totalLineDiscounts.doubleValue());
    // ... more setters
}

// Usage:
calculateBillTotals(bill, items, new PurchaseBillCalculator());
calculateBillTotals(bill, items, new GrnReturnBillCalculator());
calculateBillTotals(bill, items, new TransferOutBillCalculator());
calculateBillTotals(bill, items, new DisposalBillCalculator());
```

**✅ Benefits:**
- Reduces ~300 lines to ~100 lines (~70% reduction)
- Bug fixes in one place affect all bill types
- Each strategy focuses on its unique ~10 lines
- Easy to add new bill calculation types
- Follows Strategy Pattern (GoF design pattern)
- Testable: test each strategy independently

**📊 Impact:** This single refactoring would:
- **Eliminate 200+ lines of duplicate code**
- **Improve maintainability** by centralizing common logic
- **Make testing easier** (test strategies independently)
- **Reduce bugs** (one fix updates all calculations)

---

### **Anti-Pattern 4: Giant Switch Statement**

**File:** `src/main/java/com/divudi/service/PaymentService.java:205-300+`

**❌ Current Code:**
```java
public void populatePaymentDetails(Payment payment,
                                  PaymentMethod paymentMethod,
                                  PaymentMethodData paymentMethodData) {
    // Giant switch statement with 10+ cases
    switch (paymentMethod) {
        case Card:
            if (paymentMethodData.getCreditCard() != null) {
                payment.setBank(paymentMethodData.getCreditCard().getInstitution());
                payment.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
                payment.setPaidValue(paymentMethodData.getCreditCard().getTotalValue());
                payment.setComments(paymentMethodData.getCreditCard().getComment());
            }
            break;

        case Cheque:
            if (paymentMethodData.getCheque() != null) {
                payment.setBank(paymentMethodData.getCheque().getInstitution());
                payment.setChequeDate(paymentMethodData.getCheque().getDate());
                payment.setChequeRefNo(paymentMethodData.getCheque().getNo());
                payment.setPaidValue(paymentMethodData.getCheque().getTotalValue());
                payment.setComments(paymentMethodData.getCheque().getComment());
            }
            break;

        case Cash:
            if (paymentMethodData.getCash() != null) {
                payment.setPaidValue(paymentMethodData.getCash().getTotalValue());
            }
            break;

        case Credit:
        case ewallet:
        case PatientDeposit:
        case Slip:
        case OnCall:
        case Staff:
        case Staff_Welfare:
        case OnlineSettlement:
            // ... similar code for each case
            break;
    }
}
```

**🚨 Problems:**
1. Massive 100+ line method that's hard to read
2. All payment method logic in one place
3. Adding new payment method requires modifying this giant method
4. Each case has similar structure (code duplication)
5. Hard to unit test individual payment methods
6. Violates Single Responsibility Principle

---

**✅ Better Solution Using Strategy Pattern:**

```java
// Strategy interface
public interface PaymentMethodHandler {
    void populatePayment(Payment payment, PaymentMethodData data);
}

// Strategy for Credit Card
public class CreditCardHandler implements PaymentMethodHandler {
    @Override
    public void populatePayment(Payment payment, PaymentMethodData data) {
        if (data.getCreditCard() != null) {
            CreditCardPayment card = data.getCreditCard();
            payment.setBank(card.getInstitution());
            payment.setCreditCardRefNo(card.getNo());
            payment.setPaidValue(card.getTotalValue());
            payment.setComments(card.getComment());
        }
    }
}

// Strategy for Cheque
public class ChequeHandler implements PaymentMethodHandler {
    @Override
    public void populatePayment(Payment payment, PaymentMethodData data) {
        if (data.getCheque() != null) {
            ChequePayment cheque = data.getCheque();
            payment.setBank(cheque.getInstitution());
            payment.setChequeDate(cheque.getDate());
            payment.setChequeRefNo(cheque.getNo());
            payment.setPaidValue(cheque.getTotalValue());
            payment.setComments(cheque.getComment());
        }
    }
}

// Strategy for Cash
public class CashHandler implements PaymentMethodHandler {
    @Override
    public void populatePayment(Payment payment, PaymentMethodData data) {
        if (data.getCash() != null) {
            payment.setPaidValue(data.getCash().getTotalValue());
        }
    }
}

// ... similar handlers for other payment methods

// In PaymentService - register handlers once
private Map<PaymentMethod, PaymentMethodHandler> handlers = new HashMap<>();

@PostConstruct
public void initializeHandlers() {
    handlers.put(PaymentMethod.Card, new CreditCardHandler());
    handlers.put(PaymentMethod.Cheque, new ChequeHandler());
    handlers.put(PaymentMethod.Cash, new CashHandler());
    handlers.put(PaymentMethod.Credit, new CreditHandler());
    handlers.put(PaymentMethod.ewallet, new EwalletHandler());
    handlers.put(PaymentMethod.PatientDeposit, new PatientDepositHandler());
    // ... register other handlers
}

// Refactored method - NO switch statement!
public void populatePaymentDetails(Payment payment,
                                  PaymentMethod paymentMethod,
                                  PaymentMethodData data) {
    PaymentMethodHandler handler = handlers.get(paymentMethod);

    if (handler != null) {
        handler.populatePayment(payment, data);
    } else {
        // Handle unknown payment method
        throw new IllegalArgumentException("Unknown payment method: " + paymentMethod);
    }
}
```

**✅ Benefits:**
- Each payment method in its own class (Single Responsibility)
- Easy to add new payment methods (just create new handler and register)
- Easy to unit test (test each handler independently)
- No giant switch statement
- Follows Strategy Pattern and Open-Closed Principle
- Can use dependency injection for handlers

**Testing Example:**
```java
@Test
public void testCreditCardHandler() {
    PaymentMethodHandler handler = new CreditCardHandler();
    Payment payment = new Payment();
    PaymentMethodData data = new PaymentMethodData();

    CreditCardPayment card = new CreditCardPayment();
    card.setNo("1234-5678");
    card.setTotalValue(100.0);
    data.setCreditCard(card);

    handler.populatePayment(payment, data);

    assertEquals("1234-5678", payment.getCreditCardRefNo());
    assertEquals(100.0, payment.getPaidValue());
}
```

---

## **5️⃣ Comparison Summary**

### **Anti-Pattern Impact Analysis**

| Anti-Pattern | Current Code | With Polymorphism | LOC Saved | Maintenance Impact |
|--------------|--------------|-------------------|-----------|-------------------|
| Bill instanceof chains | ~70 lines × 10 files = 700 LOC | ~10 lines × 10 files = 100 LOC | **600 lines** | Bug fixes: 10 places → 1 place |
| Item instanceof chains | ~15 lines × 8 files = 120 LOC | ~5 lines × 8 files = 40 LOC | **80 lines** | Logic changes: 8 places → 1 place |
| Duplicate calculations | ~300 lines (4 methods) | ~100 lines (1 method + 4 strategies) | **200 lines** | Fixes: 4 places → 1 place |
| Payment switch | ~100 lines (1 giant method) | ~50 lines (handlers + dispatch) | **50 lines** | New payment: modify → extend |
| **TOTAL** | **~1220 lines** | **~290 lines** | **~930 lines (76% reduction)** | **Dramatically improved** |

---

## **6️⃣ Key Lessons & Best Practices**

### **When to Use Polymorphism**

✅ **USE polymorphism when:**
- Multiple types share common behavior with specific variations
- You find yourself checking types with `instanceof` or `switch`
- Code is duplicated across similar classes
- You need to add new types frequently
- Different implementations of the same operation exist

❌ **DON'T force polymorphism when:**
- You only have one implementation (YAGNI - You Aren't Gonna Need It)
- The operation truly is a simple conditional (e.g., if/else for true/false)
- The types are unrelated and don't share common behavior
- Over-engineering simple code

---

### **Best Practices**

1. **Favor Composition Over Inheritance**
   - Use interfaces for behavior contracts
   - Inheritance is "is-a", interfaces are "can-do"

2. **Follow SOLID Principles**
   - **S**ingle Responsibility: One class, one reason to change
   - **O**pen-Closed: Open for extension, closed for modification
   - **L**iskov Substitution: Subclasses should be substitutable for parents
   - **I**nterface Segregation: Many specific interfaces > one general interface
   - **D**ependency Inversion: Depend on abstractions, not concretions

3. **Avoid Type Checking**
   - `instanceof` chains suggest missing polymorphism
   - Large `switch` statements on type suggest Strategy Pattern

4. **Use Design Patterns**
   - **Strategy**: Different algorithms for same operation
   - **Template Method**: Common algorithm, specific steps
   - **Factory**: Create objects polymorphically
   - **State**: Behavior changes based on state

5. **Think "Tell, Don't Ask"**
   - ❌ Ask object its type, then do something
   - ✅ Tell object to do something (it knows its type)

---

### **Code Review Checklist**

When reviewing code, look for:
- [ ] Multiple `instanceof` checks
- [ ] Large `switch` statements on types
- [ ] Duplicated code with minor variations
- [ ] Methods that do similar things with different names
- [ ] Type casting after type checking
- [ ] Comments like "TODO: refactor" or "duplicated from..."

---

## **7️⃣ Real-World Impact in HMIS**

### **Current State**
- ~1,220 lines of anti-pattern code
- Bug fixes require changes in 10+ places
- New bill/payment types require modifying multiple files
- Testing is difficult (can't test types independently)

### **With Polymorphism Refactoring**
- ~290 lines of clean code (**76% reduction**)
- Bug fixes in one place automatically apply everywhere
- New types added by extending classes (no modification)
- Easy testing (each implementation tested independently)

### **Business Value**
- **Faster development**: Add new features without fear of breaking existing code
- **Fewer bugs**: One fix applies everywhere
- **Easier onboarding**: New developers understand single-responsibility classes
- **Better scalability**: System grows by extension, not modification

---

## **8️⃣ Practice Exercise**

### **Challenge: Refactor This Code**

```java
public class ReportGenerator {
    public void generateReport(String reportType, List<Bill> bills) {
        if (reportType.equals("DAILY_SALES")) {
            // 50 lines of daily sales logic
            System.out.println("Generating Daily Sales Report");
            double total = 0;
            for (Bill bill : bills) {
                if (bill.getBillType() == BillType.Sale) {
                    total += bill.getTotal();
                }
            }
            System.out.println("Total Sales: " + total);
        } else if (reportType.equals("MONTHLY_SUMMARY")) {
            // 50 lines of monthly summary logic
            System.out.println("Generating Monthly Summary Report");
            Map<String, Double> summary = new HashMap<>();
            for (Bill bill : bills) {
                String month = getMonth(bill.getBillDate());
                summary.put(month, summary.getOrDefault(month, 0.0) + bill.getTotal());
            }
            System.out.println("Summary: " + summary);
        } else if (reportType.equals("PATIENT_STATEMENT")) {
            // 50 lines of patient statement logic
            System.out.println("Generating Patient Statement");
            // ... more code
        }
        // ... 5 more report types
    }
}
```

**Questions:**
1. What anti-pattern do you see?
2. How would you refactor using polymorphism?
3. What design pattern would you use?

**Hint:** Think Strategy Pattern!

---

## **9️⃣ Additional Resources**

### **Web Resources**
- [Polymorphism in Java - GeeksforGeeks](https://www.geeksforgeeks.org/java/polymorphism-in-java/)
- [Compile-time vs Runtime Polymorphism - GeeksforGeeks](https://www.geeksforgeeks.org/java/difference-between-compile-time-and-run-time-polymorphism-in-java/)
- [Polymorphism Examples - Great Learning](https://www.mygreatlearning.com/blog/polymorphism-in-java/)
- [Types of Polymorphism - Software Testing Help](https://www.softwaretestinghelp.com/polymorphism-in-java/)
- [Runtime Polymorphism - javatpoint](https://www.javatpoint.com/runtime-polymorphism-in-java)
- [OOP Polymorphism Guide 2025 - WikiTechy](https://www.wikitechy.com/polymorphism-in-oops-guide-2025/)

### **Recommended Books**
- *Head First Design Patterns* by Freeman & Robson
- *Effective Java* by Joshua Bloch
- *Clean Code* by Robert C. Martin
- *Refactoring* by Martin Fowler

### **HMIS Codebase Files to Study**

**Good Examples:**
- `src/main/java/com/divudi/core/facade/AbstractFacade.java:34-342` - Template Method Pattern
- `src/main/java/com/divudi/bean/common/ControllerWithPatient.java:6-23` - Interface-based design
- `src/main/java/com/divudi/core/validator/NonNegativeNumberValidator.java:20-50` - Strategy Pattern
- `src/main/java/com/divudi/core/entity/Bill.java:51` - Inheritance hierarchy

**Anti-Patterns (Learning Opportunities):**
- `src/main/java/com/divudi/ws/channel/ChannelApi.java:3554-3627` - instanceof chains
- `src/main/java/com/divudi/service/pharmacy/PharmacyCostingService.java:621-1300` - code duplication
- `src/main/java/com/divudi/service/PaymentService.java:205-300` - switch statement
- `src/main/java/com/divudi/ejb/PrescriptionToItemService.java:88-96` - item instanceof

---

## **🎯 Key Takeaways**

1. **Polymorphism = "Many Forms"**
   - Same interface, different implementations
   - Determined at compile-time (overloading) or runtime (overriding)

2. **Benefits**
   - Code reuse and reduction
   - Easier maintenance and testing
   - Flexible and extensible systems
   - Follows SOLID principles

3. **Watch Out For**
   - `instanceof` chains → use polymorphism
   - Large `switch` statements → use Strategy Pattern
   - Duplicate code → extract common behavior

4. **Real Impact in HMIS**
   - Can reduce ~1,220 lines to ~290 lines (76% savings)
   - Improves maintainability dramatically
   - Makes adding features easier and safer

5. **Think "Tell, Don't Ask"**
   - Let objects handle their own behavior
   - Don't check type and then act
   - Trust the polymorphism!

---

## **Thank You!**

**Remember:** Good code is not about being clever, it's about being **clear, maintainable, and extensible**.

Polymorphism is one of the most powerful tools to achieve this!

---

## **❓ Questions & Discussion**

**Questions to Consider:**
1. Can you think of other examples in your daily work where polymorphism would help?
2. What challenges might you face when refactoring existing code to use polymorphism?
3. How would you convince a team to refactor anti-patterns?
4. When is it okay to NOT use polymorphism?

---

*Presentation created: December 1, 2025*
*Based on real code analysis of HMIS codebase*
