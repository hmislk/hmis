# Object-Oriented Programming: Abstraction

## Table of Contents
1. [Introduction to Abstraction](#introduction-to-abstraction)
2. [Why Abstraction Matters](#why-abstraction-matters)
3. [Types of Abstraction in Java](#types-of-abstraction-in-java)
4. [Abstraction in HMIS Codebase](#abstraction-in-hmis-codebase)
5. [Areas for Improvement](#areas-for-improvement)
6. [Anti-Patterns to Avoid](#anti-patterns-to-avoid)
7. [Best Practices](#best-practices)
8. [Exercises](#exercises)

---

## Introduction to Abstraction

### What is Abstraction?

**Abstraction** is one of the four fundamental pillars of Object-Oriented Programming (along with Encapsulation, Inheritance, and Polymorphism).

> **Abstraction** is the process of hiding implementation details and showing only essential features to the user.

Think of a car: When you drive, you use the steering wheel, pedals, and gear shift. You don't need to know how the engine combustion works, how the transmission system operates, or how the electronic systems communicate. The car provides an **abstraction** - a simple interface to a complex system.

### Key Concepts

1. **Hiding Complexity**: Show only what's necessary, hide the internal workings
2. **Focus on "What" not "How"**: Define what an object does, not how it does it
3. **Contract-based Design**: Establish agreements between different parts of code
4. **Flexibility**: Change implementation without affecting code that uses it

---

## Why Abstraction Matters

### Benefits of Abstraction

#### 1. **Reduced Complexity**
```java
// Without abstraction - Complex and confusing
public void processPatientBill() {
    // 50 lines of database queries
    // 30 lines of validation logic
    // 40 lines of calculation
    // 20 lines of notification logic
}

// With abstraction - Clean and understandable
public void processPatientBill() {
    Patient patient = patientRepository.findById(id);
    validatePatient(patient);
    Bill bill = billService.calculateBill(patient);
    notificationService.sendBillNotification(patient, bill);
}
```

#### 2. **Maintainability**
- Changes to implementation don't affect code using the abstraction
- Easier to locate and fix bugs
- Reduces ripple effects of changes

#### 3. **Testability**
- Mock abstract dependencies easily
- Test components in isolation
- Verify contracts without implementation details

#### 4. **Reusability**
- Abstract code can be reused across different contexts
- Reduces code duplication
- Promotes DRY (Don't Repeat Yourself) principle

#### 5. **Team Collaboration**
- Clear contracts between components
- Different team members can work on different implementations
- Reduces merge conflicts and integration issues

---

## Types of Abstraction in Java

### 1. Abstract Classes

An **abstract class** is a class that cannot be instantiated and may contain both abstract methods (without implementation) and concrete methods (with implementation).

```java
public abstract class Animal {
    // Abstract method (no implementation)
    public abstract void makeSound();

    // Concrete method (with implementation)
    public void sleep() {
        System.out.println("Zzz...");
    }

    // Instance variables
    protected String name;

    // Constructor
    public Animal(String name) {
        this.name = name;
    }
}

public class Dog extends Animal {
    public Dog(String name) {
        super(name);
    }

    @Override
    public void makeSound() {
        System.out.println("Woof! Woof!");
    }
}
```

**When to use Abstract Classes:**
- When you want to share code among several closely related classes
- When classes that extend it have many common methods or fields
- When you need non-static or non-final fields
- When you have a "is-a" relationship

### 2. Interfaces

An **interface** is a completely abstract contract that defines what a class must do, but not how it does it.

```java
public interface Drawable {
    void draw();
    void resize(int width, int height);
}

public interface Colorable {
    void setColor(String color);
    String getColor();
}

// A class can implement multiple interfaces
public class Circle implements Drawable, Colorable {
    private int radius;
    private String color;

    @Override
    public void draw() {
        System.out.println("Drawing circle with radius " + radius);
    }

    @Override
    public void resize(int width, int height) {
        radius = Math.min(width, height) / 2;
    }

    @Override
    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String getColor() {
        return color;
    }
}
```

**When to use Interfaces:**
- When you expect unrelated classes to implement your interface
- When you want to specify the behavior of a particular data type
- When you want to take advantage of multiple inheritance
- When you have a "can-do" relationship

### 3. Abstraction through Encapsulation

Encapsulation is a form of abstraction where you hide data and provide controlled access through methods.

```java
public class BankAccount {
    // Private fields - hidden from outside
    private double balance;
    private String accountNumber;

    // Public methods - controlled access
    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }

    public boolean withdraw(double amount) {
        if (amount > 0 && amount <= balance) {
            balance -= amount;
            return true;
        }
        return false;
    }

    public double getBalance() {
        return balance;
    }

    // The complexity is hidden
    private void logTransaction(String type, double amount) {
        // Complex logging logic here
    }
}
```

---

## Abstraction in HMIS Codebase

Let's examine real examples of abstraction from our HMIS project.

### Example 1: AbstractFacade - Generic Data Access Layer

**Location**: `src/main/java/com/divudi/core/facade/AbstractFacade.java`

The `AbstractFacade` class is an excellent example of abstraction in our codebase:

```java
public abstract class AbstractFacade<T> {
    private Class<T> entityClass;

    // Abstract method - subclasses must implement
    protected abstract EntityManager getEntityManager();

    // Concrete methods - shared by all subclasses
    public void create(T entity) {
        getEntityManager().persist(entity);
    }

    public void edit(T entity) {
        getEntityManager().merge(entity);
    }

    public void remove(T entity) {
        getEntityManager().remove(getEntityManager().merge(entity));
    }

    public T find(Object id) {
        if (id == null) {
            return null;
        }
        return getEntityManager().find(entityClass, id);
    }

    public List<T> findAll() {
        CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
        return getEntityManager().createQuery(cq).getResultList();
    }

    public List<T> findByJpql(String jpql, Map<String, Object> parameters) {
        TypedQuery<T> qry = getEntityManager().createQuery(jpql, entityClass);
        // ... parameter binding logic
        return qry.getResultList();
    }
}
```

**Why this is good abstraction:**

1. ✅ **Hides Complexity**: All facade classes don't need to reimplement CRUD operations
2. ✅ **Generic Design**: Works with any entity type through Java generics
3. ✅ **Template Method Pattern**: `getEntityManager()` is abstract, implementation varies per subclass
4. ✅ **Code Reuse**: Hundreds of lines of query logic available to all facades
5. ✅ **Consistent API**: All data access follows the same pattern

**Usage Example:**

```java
@Stateless
public class PatientFacade extends AbstractFacade<Patient> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PatientFacade() {
        super(Patient.class);
    }

    // Can now use all methods from AbstractFacade:
    // create(), edit(), remove(), find(), findByJpql(), etc.
}
```

### Example 2: Interface-Based Abstraction

**Location**: `src/main/java/com/divudi/core/entity/RetirableEntity.java`

```java
public interface RetirableEntity {
    void setRetired(boolean retired);
    boolean isRetired();

    void setRetiredAt(Date retiredAt);
    Date getRetiredAt();

    void setRetirer(WebUser retirer);
    WebUser getRetirer();

    void setRetireComments(String comments);
    String getRetireComments();
}
```

**Why this is good abstraction:**

1. ✅ **Contract Definition**: Any entity implementing this can be "retired"
2. ✅ **Polymorphism**: Code can work with `RetirableEntity` without knowing specific type
3. ✅ **Flexibility**: Different entities can implement retirement differently
4. ✅ **Type Safety**: Compiler ensures all methods are implemented

**Usage Example:**

```java
// Works with any RetirableEntity
public void retireEntity(RetirableEntity entity, WebUser user, String reason) {
    entity.setRetired(true);
    entity.setRetiredAt(new Date());
    entity.setRetirer(user);
    entity.setRetireComments(reason);
}

// Can be called with different entity types
retireEntity(patient, currentUser, "Duplicate record");
retireEntity(bill, currentUser, "Cancelled by patient");
retireEntity(item, currentUser, "Obsolete");
```

### Example 3: Controller Interface Abstraction

**Location**: `src/main/java/com/divudi/bean/common/ControllerWithPatient.java`

```java
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
```

**Why this is good abstraction:**

1. ✅ **Behavioral Contract**: Defines what controllers with patients must do
2. ✅ **Consistency**: All patient-related controllers follow same pattern
3. ✅ **Testability**: Easy to mock for testing
4. ✅ **Documentation**: Interface serves as documentation of capabilities

### Example 4: Service Layer Abstraction

**Location**: `src/main/java/com/divudi/ejb/PharmacyService.java`

```java
@Stateless
public class PharmacyService {
    @EJB
    private ClinicalFindingValueFacade clinicalFindingValueFacade;

    @EJB
    BillService billService;

    // High-level abstraction
    public List<ClinicalFindingValue> getAllergyListForPatient(Patient patient) {
        if (patient == null) {
            return new ArrayList<>();
        }

        String jpql = "Select clinicalValue from ClinicalFindingValue clinicalValue "
                + " where clinicalValue.patient = :patient "
                + " and clinicalValue.clinicalFindingValueType = :type "
                + " and clinicalValue.retired = :retireStatus";

        Map<String, Object> params = new HashMap<>();
        params.put("patient", patient);
        params.put("type", ClinicalFindingValueType.PatientAllergy);
        params.put("retireStatus", false);

        return clinicalFindingValueFacade.findByJpql(jpql, params);
    }

    public boolean isAllergyForPatient(Patient patient, BillItem billItem,
                                       List<ClinicalFindingValue> allergyList) {
        return !getAllergyMessageForPatient(patient, billItem, allergyList).isEmpty();
    }
}
```

**Why this is good abstraction:**

1. ✅ **Business Logic Encapsulation**: Hides complex allergy checking logic
2. ✅ **Single Responsibility**: Service focuses only on pharmacy operations
3. ✅ **Dependency Injection**: Uses @EJB to abstract dependency creation
4. ✅ **Reusability**: Methods can be called from any controller

---

## Areas for Improvement

While our codebase has good abstraction in many places, there are opportunities for improvement:

### 1. Inconsistent Service Layer Usage

**Current State:**
Some controllers have business logic directly embedded:

```java
// ❌ Business logic in controller
public class SomeController {
    public void processBill() {
        // 100 lines of business logic here
        // Database queries mixed with UI logic
        // Calculations mixed with validation
        // No abstraction layer
    }
}
```

**Improvement:**
Extract business logic to service layer:

```java
// ✅ Business logic in service
@Stateless
public class BillingService {
    public Bill processBill(Patient patient, List<BillItem> items) {
        // Business logic here
        return bill;
    }
}

public class BillController {
    @EJB
    private BillingService billingService;

    public void processBill() {
        Bill bill = billingService.processBill(patient, selectedItems);
        displayBill(bill);
    }
}
```

### 2. Magic Numbers and Hardcoded Values

**Current State:**
```java
// ❌ Magic numbers
if (bill.getBillType().equals("OPD")) {
    discount = total * 0.15;
}

// ❌ Hardcoded strings
String query = "SELECT b FROM Bill b WHERE b.cancelled = false";
```

**Improvement:**
```java
// ✅ Constants and enums
public enum BillType {
    OPD(0.15),
    IPD(0.10),
    PHARMACY(0.05);

    private final double discountRate;

    BillType(double discountRate) {
        this.discountRate = discountRate;
    }

    public double getDiscountRate() {
        return discountRate;
    }
}

if (bill.getBillType() == BillType.OPD) {
    discount = total * BillType.OPD.getDiscountRate();
}
```

### 3. God Classes

**Current State:**
```java
// ❌ One class doing everything
public class PharmacyController {
    public void searchItems() { }
    public void addToCart() { }
    public void calculateTotal() { }
    public void processPayment() { }
    public void printReceipt() { }
    public void updateInventory() { }
    public void sendNotification() { }
    public void generateReport() { }
    // ... 50 more methods
}
```

**Improvement:**
```java
// ✅ Separated concerns
public class PharmacySearchController {
    public void searchItems() { }
}

public class PharmacyCartService {
    public void addToCart() { }
    public void calculateTotal() { }
}

public class PharmacyPaymentService {
    public void processPayment() { }
}

public class PharmacyReportService {
    public void generateReport() { }
}
```

### 4. Missing Interface Segregation

**Current State:**
```java
// ❌ Fat interface - implementing classes may not need all methods
public interface MedicalRecord {
    void addDiagnosis();
    void addPrescription();
    void scheduleSurgery();
    void orderLabTest();
    void bookBed();
    void dischargePatient();
}

// Classes forced to implement methods they don't need
public class OutpatientRecord implements MedicalRecord {
    public void scheduleSurgery() {
        throw new UnsupportedOperationException();
    }

    public void bookBed() {
        throw new UnsupportedOperationException();
    }
}
```

**Improvement:**
```java
// ✅ Interface segregation - small, focused interfaces
public interface DiagnosisCapable {
    void addDiagnosis();
}

public interface PrescriptionCapable {
    void addPrescription();
}

public interface InpatientCapable {
    void bookBed();
    void dischargePatient();
}

// Implement only what you need
public class OutpatientRecord implements DiagnosisCapable, PrescriptionCapable {
    public void addDiagnosis() { }
    public void addPrescription() { }
}

public class InpatientRecord implements DiagnosisCapable, PrescriptionCapable, InpatientCapable {
    public void addDiagnosis() { }
    public void addPrescription() { }
    public void bookBed() { }
    public void dischargePatient() { }
}
```

### 5. Lack of Strategy Pattern for Algorithms

**Current State:**
```java
// ❌ Conditional logic for different calculation strategies
public double calculateDiscount(Bill bill) {
    if (bill.getPaymentScheme().equals("CASH")) {
        return bill.getTotal() * 0.05;
    } else if (bill.getPaymentScheme().equals("INSURANCE")) {
        return bill.getTotal() * 0.20;
    } else if (bill.getPaymentScheme().equals("CREDIT")) {
        return bill.getTotal() * 0.10;
    }
    return 0;
}
```

**Improvement:**
```java
// ✅ Strategy pattern
public interface DiscountStrategy {
    double calculateDiscount(double total);
}

public class CashDiscountStrategy implements DiscountStrategy {
    public double calculateDiscount(double total) {
        return total * 0.05;
    }
}

public class InsuranceDiscountStrategy implements DiscountStrategy {
    public double calculateDiscount(double total) {
        return total * 0.20;
    }
}

public class DiscountCalculator {
    private Map<String, DiscountStrategy> strategies = new HashMap<>();

    public DiscountCalculator() {
        strategies.put("CASH", new CashDiscountStrategy());
        strategies.put("INSURANCE", new InsuranceDiscountStrategy());
        strategies.put("CREDIT", new CreditDiscountStrategy());
    }

    public double calculateDiscount(Bill bill) {
        DiscountStrategy strategy = strategies.get(bill.getPaymentScheme());
        return strategy != null ? strategy.calculateDiscount(bill.getTotal()) : 0;
    }
}
```

---

## Anti-Patterns to Avoid

### 1. Leaky Abstraction

**Problem**: Implementation details leak through the abstraction

```java
// ❌ Leaky abstraction - exposes database implementation
public interface UserRepository {
    EntityManager getEntityManager(); // Internal implementation exposed!
    TypedQuery<User> createQuery(String jpql); // JPA-specific!
}

// ✅ Proper abstraction - hides implementation
public interface UserRepository {
    User findById(Long id);
    List<User> findAll();
    void save(User user);
    void delete(User user);
}
```

**Why it's bad:**
- Defeats the purpose of abstraction
- Makes it hard to change implementation
- Couples client code to implementation details

### 2. Abstraction for Its Own Sake

**Problem**: Over-abstracting simple things

```java
// ❌ Unnecessary abstraction
public interface StringGetter {
    String getString();
}

public interface StringSetter {
    void setString(String s);
}

public interface StringHolder extends StringGetter, StringSetter {
}

// ✅ Simple and direct
public class Person {
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

**Why it's bad:**
- Makes code harder to understand
- Adds unnecessary complexity
- Increases maintenance burden

**Rule of Thumb**: Only abstract when you have:
- Multiple implementations
- Changing requirements
- Need for testability
- Complex logic to hide

### 3. Abstract Everything Syndrome

**Problem**: Making everything abstract or interface-based

```java
// ❌ Pointless abstraction
public interface MathOperations {
    int add(int a, int b);
    int subtract(int a, int b);
}

public class BasicMath implements MathOperations {
    public int add(int a, int b) {
        return a + b;
    }

    public int subtract(int a, int b) {
        return a - b;
    }
}

// ✅ Direct implementation
public class MathUtils {
    public static int add(int a, int b) {
        return a + b;
    }

    public static int subtract(int a, int b) {
        return a - b;
    }
}
```

**Why it's bad:**
- No foreseeable need for multiple implementations
- Adds layers without benefit
- Wastes development time

### 4. God Interface

**Problem**: Interface with too many unrelated methods

```java
// ❌ God interface
public interface Hospital {
    // Patient management
    void admitPatient();
    void dischargePatient();

    // Billing
    void generateBill();
    void processPayment();

    // Inventory
    void orderSupplies();
    void checkStock();

    // HR
    void hireStaff();
    void processPayroll();

    // ... 50 more methods
}

// ✅ Segregated interfaces
public interface PatientManagement {
    void admitPatient();
    void dischargePatient();
}

public interface BillingManagement {
    void generateBill();
    void processPayment();
}

public interface InventoryManagement {
    void orderSupplies();
    void checkStock();
}
```

**Why it's bad:**
- Violates Interface Segregation Principle
- Forces implementations to provide methods they don't need
- Hard to maintain and understand

### 5. Anemic Domain Model

**Problem**: Objects with no behavior, only getters/setters

```java
// ❌ Anemic model
public class Bill {
    private double total;
    private double discount;
    private double netTotal;

    // Only getters and setters, no behavior
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    // ...
}

// Business logic in service
public class BillService {
    public void applyDiscount(Bill bill, double discountPercent) {
        double discount = bill.getTotal() * discountPercent;
        bill.setDiscount(discount);
        bill.setNetTotal(bill.getTotal() - discount);
    }
}

// ✅ Rich domain model
public class Bill {
    private double total;
    private double discount;

    public void applyDiscount(double discountPercent) {
        if (discountPercent < 0 || discountPercent > 1) {
            throw new IllegalArgumentException("Invalid discount");
        }
        this.discount = total * discountPercent;
    }

    public double getNetTotal() {
        return total - discount;
    }
}
```

**Why it's bad:**
- Violates encapsulation
- Business logic scattered across services
- Objects don't protect their own invariants

### 6. Wrong Abstraction Level

**Problem**: Mixing high-level and low-level abstractions

```java
// ❌ Mixed abstraction levels
public void processPatientAdmission() {
    Patient patient = new Patient();
    patient.setName(name);
    patient.setAge(age);

    // High-level
    validatePatient(patient);

    // Suddenly drops to low-level
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    tx.begin();
    em.persist(patient);
    tx.commit();
    em.close();

    // Back to high-level
    sendAdmissionNotification(patient);
}

// ✅ Consistent abstraction level
public void processPatientAdmission() {
    Patient patient = createPatient();
    validatePatient(patient);
    savePatient(patient); // Hides persistence details
    sendAdmissionNotification(patient);
}
```

**Why it's bad:**
- Hard to understand and maintain
- Mixes concerns
- Violates Single Responsibility Principle

---

## Best Practices

### 1. Follow SOLID Principles

- **S**ingle Responsibility Principle: One class, one reason to change
- **O**pen/Closed Principle: Open for extension, closed for modification
- **L**iskov Substitution Principle: Subtypes must be substitutable for base types
- **I**nterface Segregation Principle: Many specific interfaces over one general interface
- **D**ependency Inversion Principle: Depend on abstractions, not concretions

### 2. Design by Contract

```java
/**
 * Calculates patient's bill total
 *
 * @param billItems List of bill items (must not be null or empty)
 * @return Total amount (always >= 0)
 * @throws IllegalArgumentException if billItems is null or empty
 */
public double calculateTotal(List<BillItem> billItems) {
    // Precondition
    if (billItems == null || billItems.isEmpty()) {
        throw new IllegalArgumentException("Bill items cannot be null or empty");
    }

    // Business logic
    double total = billItems.stream()
        .mapToDouble(BillItem::getAmount)
        .sum();

    // Postcondition
    assert total >= 0 : "Total must be non-negative";

    return total;
}
```

### 3. Favor Composition Over Inheritance

```java
// ❌ Deep inheritance
public class Animal { }
public class Mammal extends Animal { }
public class Dog extends Mammal { }
public class Labrador extends Dog { }

// ✅ Composition
public class Dog {
    private Breed breed;
    private Diet diet;
    private Behavior behavior;
}
```

### 4. Use Dependency Injection

```java
// ❌ Hard-coded dependencies
public class BillController {
    private BillService billService = new BillService(); // Hard to test!
}

// ✅ Dependency injection
public class BillController {
    @EJB
    private BillService billService; // Easy to mock and test
}
```

### 5. Keep Abstractions Stable

- Don't change interface contracts frequently
- Use versioning if breaking changes are needed
- Deprecate before removing
- Provide migration paths

---

## Exercises

### Exercise 1: Identify Abstraction Type

For each scenario, identify whether you should use an **abstract class**, **interface**, or **both**:

1. You want to define a shape that has area and perimeter calculations, and all shapes should have a color field.
2. Multiple unrelated classes need to implement a `save()` method.
3. You want to provide a base implementation for database operations but allow customization.
4. You want to ensure certain classes can be serialized to JSON.

### Exercise 2: Refactor God Class

Refactor this God class into proper abstractions:

```java
public class PharmacyManager {
    public void searchMedicine() { }
    public void addToCart() { }
    public void removeFromCart() { }
    public void calculateTotal() { }
    public void applyDiscount() { }
    public void processPayment() { }
    public void updateInventory() { }
    public void checkStock() { }
    public void orderSupplies() { }
    public void generateInvoice() { }
    public void sendNotification() { }
    public void printReceipt() { }
}
```

### Exercise 3: Design an Abstraction

Design an abstraction for a payment processing system that supports:
- Cash payments
- Credit card payments
- Insurance claims
- Mobile wallet payments

Your design should:
- Be extensible for new payment types
- Hide implementation details
- Provide a consistent interface

### Exercise 4: Spot the Anti-Pattern

Identify the anti-pattern in this code and fix it:

```java
public interface DataAccess {
    Connection getConnection();
    PreparedStatement prepareStatement(String sql);
    ResultSet executeQuery(PreparedStatement stmt);
    void closeConnection();
}
```

### Exercise 5: Real HMIS Improvement

Look at your recent code in the HMIS project. Identify:
1. One place where abstraction would improve the code
2. One example of good abstraction
3. One potential anti-pattern

---

## Summary

### Key Takeaways

1. **Abstraction hides complexity** and shows only essential features
2. **Use abstract classes** when sharing code among related classes
3. **Use interfaces** for defining contracts and enabling multiple inheritance
4. **Our AbstractFacade** is a great example of abstraction done right
5. **Avoid over-abstraction** - keep it simple when appropriate
6. **Watch for anti-patterns** like leaky abstractions and god interfaces
7. **Follow SOLID principles** for maintainable abstractions

### Remember

> "Premature optimization is the root of all evil, but premature abstraction is the path to confusion."

Abstract when you need to, not when you might need to.

### Questions for Reflection

1. How can abstraction improve the maintainability of our HMIS system?
2. What's the cost of abstraction, and when is it worth paying?
3. How do we balance between "too abstract" and "not abstract enough"?
4. What role does abstraction play in team collaboration?

---

## Further Reading

- **Design Patterns: Elements of Reusable Object-Oriented Software** (Gang of Four)
- **Clean Architecture** by Robert C. Martin
- **Effective Java** by Joshua Bloch
- **Head First Design Patterns** by Freeman & Freeman

---

**Document Version**: 1.0
**Last Updated**: December 2025
**Target Audience**: New software engineering trainees at CareCode
**Prerequisites**: Basic Java knowledge, familiarity with OOP concepts
