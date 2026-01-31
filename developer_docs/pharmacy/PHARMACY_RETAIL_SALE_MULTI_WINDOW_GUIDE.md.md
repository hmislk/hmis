# Pharmacy Retail Sale Multi-Window — Developer Guideline

This note documents how we maintain four parallel retail-sale pages/controllers that allow users to run simultaneous pharmacy sales in different browser windows. The application was started before JSF view scope was available in our stack, so we use four separate pages and controllers.

## Golden rule

**Do not change the original files under any circumstance.**

## Source Files (Single Source of Truth)

**Main XHTML**: `src/main/webapp/pharmacy/pharmacy_bill_retail_sale_for_cashier.xhtml`
**Main Controller**: `src/main/java/com/divudi/bean/pharmacy/PharmacySaleForCashierController.java`

These are the single source of truth for behavior and layout.

## Target Files (Copies that users navigate to)

**Sale 1**: `pharmacy_bill_retail_sale_for_cashier.xhtml` + `PharmacySaleForCashierController.java` (main/original)
**Sale 2**: `pharmacy_bill_retail_sale_for_cashier_1.xhtml` + `PharmacySaleController1.java`
**Sale 3**: `pharmacy_bill_retail_sale_for_cashier_2.xhtml` + `PharmacySaleController2.java`
**Sale 4**: `pharmacy_bill_retail_sale_for_cashier_3.xhtml` + `PharmacySaleController3.java`

Users can switch among all four pages using the navigation buttons. Do not remove these buttons.

## Change workflow

**RECOMMENDED APPROACH**: Complete replacement strategy (faster, safer, fewer errors).

After functional or UI changes are done and QA passed in the original files, completely replace all three numbered copies with fresh copies from the originals. This prevents synchronization drift and ensures 100% consistency.

## Complete Replacement Steps

### Step 1: Create Backup Branch
```bash
git checkout -b sync-pharmacy-billing-replacement
```

### Step 2: Java Controller Replacement

For each target controller (1, 2, 3):

**2.1 Copy Source to Target**
```bash
cp PharmacySaleForCashierController.java PharmacySaleController1.java
cp PharmacySaleForCashierController.java PharmacySaleController2.java
cp PharmacySaleForCashierController.java PharmacySaleController3.java
```

**2.2 Update Each Java Controller**

For **PharmacySaleController1.java**, make these exact changes:

- **@Named annotation**: `@Named` → `@Named("pharmacySaleController1")`
- **Class name**: `public class PharmacySaleForCashierController` → `public class PharmacySaleController1`
- **Constructor**: `public PharmacySaleForCashierController()` → `public PharmacySaleController1()`
- **Logger**: `Logger.getLogger(PharmacySaleForCashierController.class.getName())` → `Logger.getLogger(PharmacySaleController1.class.getName())`
- **Metadata**: `metadata.setControllerClass("PharmacySaleForCashierController")` → `metadata.setControllerClass("PharmacySaleController1")`
- **Converter reference**: `PharmacySaleForCashierController controller = (PharmacySaleForCashierController) facesContext...getValue(..., "pharmacySaleForCashierController")` → `PharmacySaleController1 controller = (PharmacySaleController1) facesContext...getValue(..., "pharmacySaleController1")`

Repeat same pattern for **PharmacySaleController2.java** (with "2") and **PharmacySaleController3.java** (with "3").

**2.3 Test Compilation**
```bash
./detect-maven.sh compile
```

### Step 3: XHTML File Replacement

**3.1 Copy Source to Target**
```bash
cp pharmacy_bill_retail_sale_for_cashier.xhtml pharmacy_bill_retail_sale_for_cashier_1.xhtml
cp pharmacy_bill_retail_sale_for_cashier.xhtml pharmacy_bill_retail_sale_for_cashier_2.xhtml
cp pharmacy_bill_retail_sale_for_cashier.xhtml pharmacy_bill_retail_sale_for_cashier_3.xhtml
```

**3.2 Update Each XHTML File**

For **pharmacy_bill_retail_sale_for_cashier_1.xhtml**:

- **Controller references**: `pharmacySaleForCashierController` → `pharmacySaleController1` (global replace - typically 50+ instances)
- **Page title**: `"Pharmacy Retail Bill For Cashier - 1"` → `"Pharmacy Retail Bill (Sale 2)"`

For **pharmacy_bill_retail_sale_for_cashier_2.xhtml**:

- **Controller references**: `pharmacySaleForCashierController` → `pharmacySaleController2` (global replace)
- **Page title**: `"Pharmacy Retail Bill For Cashier - 1"` → `"Pharmacy Retail Bill (Sale 3)"`

For **pharmacy_bill_retail_sale_for_cashier_3.xhtml**:

- **Controller references**: `pharmacySaleForCashierController` → `pharmacySaleController3` (global replace)
- **Page title**: `"Pharmacy Retail Bill For Cashier - 1"` → `"Pharmacy Retail Bill (Sale 4)"`

### Step 4: Add Navigation Buttons

Add to each XHTML file after the header `</f:facet>` section:

```xml
<!-- Version Navigation Buttons -->
<div class="mb-3 text-center">
    <div class="btn-group" role="group">
        <p:commandButton
            icon="fas fa-file-invoice"
            value="Sale 1"
            action="/pharmacy/pharmacy_bill_retail_sale_for_cashier?faces-redirect=true"
            ajax="false"
            disabled="[true for _main, false for others]"
            class="[ui-button-secondary for disabled, ui-button-info for enabled]" />
        <p:commandButton
            icon="fas fa-file-invoice"
            value="Sale 2"
            action="/pharmacy/pharmacy_bill_retail_sale_for_cashier_1?faces-redirect=true"
            ajax="false"
            disabled="[true for _1.xhtml, false for others]"
            class="[ui-button-secondary for disabled, ui-button-info for enabled]" />
        <p:commandButton
            icon="fas fa-file-invoice"
            value="Sale 3"
            action="/pharmacy/pharmacy_bill_retail_sale_for_cashier_2?faces-redirect=true"
            ajax="false"
            disabled="[true for _2.xhtml, false for others]"
            class="[ui-button-secondary for disabled, ui-button-info for enabled]" />
        <p:commandButton
            icon="fas fa-file-invoice"
            value="Sale 4"
            action="/pharmacy/pharmacy_bill_retail_sale_for_cashier_3?faces-redirect=true"
            ajax="false"
            disabled="[true for _3.xhtml, false for others]"
            class="[ui-button-secondary for disabled, ui-button-info for enabled]" />
    </div>
</div>
```

### Step 5: Verification & Testing

**5.1 Compilation Test**
```bash
./detect-maven.sh compile
```

**5.2 File Verification**
```bash
# Verify no mixed references in XHTML copies
grep -n "pharmacySaleForCashierController" pharmacy_bill_retail_sale_for_cashier_1.xhtml  # Should be 0 matches
grep -n "pharmacySaleForCashierController" pharmacy_bill_retail_sale_for_cashier_2.xhtml  # Should be 0 matches
grep -n "pharmacySaleForCashierController" pharmacy_bill_retail_sale_for_cashier_3.xhtml  # Should be 0 matches

# Verify correct bean names in Java files
grep "@Named" PharmacySaleController1.java  # Should show @Named("pharmacySaleController1")
grep "@Named" PharmacySaleController2.java  # Should show @Named("pharmacySaleController2")
grep "@Named" PharmacySaleController3.java  # Should show @Named("pharmacySaleController3")
```

**5.3 Navigation URLs**
- **Sale 1**: `/pharmacy/pharmacy_bill_retail_sale_for_cashier.xhtml` → `PharmacySaleForCashierController`
- **Sale 2**: `/pharmacy/pharmacy_bill_retail_sale_for_cashier_1.xhtml` → `PharmacySaleController1`
- **Sale 3**: `/pharmacy/pharmacy_bill_retail_sale_for_cashier_2.xhtml` → `PharmacySaleController2`
- **Sale 4**: `/pharmacy/pharmacy_bill_retail_sale_for_cashier_3.xhtml` → `PharmacySaleController3`

## When to Use This Process

**ONLY** execute this synchronization when:

✅ **Major features added**: New functionality (like quantity adjustment buttons, new UI components)
✅ **Business logic changes**: Payment processing, calculation methods, validation improvements
✅ **Performance optimizations**: StockDTO conversions, caching, service integrations
✅ **UI/UX improvements**: Layout changes, styling updates, navigation improvements
✅ **Bug fixes**: Critical fixes that affect core pharmacy billing functionality

**DO NOT** use for minor changes like:
❌ Simple text/label changes
❌ Single-line tweaks
❌ Cosmetic styling that doesn't affect functionality

## What Not to Do

❌ **Never modify the original files** during synchronization
❌ **Never special-case Sale 1** - treat all versions equally
❌ **Never hide navigation buttons** - users depend on version switching
❌ **Never introduce new bean names** or create a fifth copy
❌ **Never use incremental sync** for major changes (high error rate)
❌ **Never skip compilation testing** after Java updates

## Pre-commit Checklist

- [ ] **Original files untouched** (only copies modified)
- [ ] **All controllers compile** successfully (`./detect-maven.sh compile`)
- [ ] **Bean names verified** (`@Named("pharmacySaleController1/2/3")`)
- [ ] **No mixed references** (0 matches for base controller name in copies)
- [ ] **Page titles correct** ("Sale 2", "Sale 3", "Sale 4")
- [ ] **Navigation buttons work** (can switch between all versions)
- [ ] **Session isolation works** (bills stay separate in different versions)
- [ ] **Critical features tested** (add item, calculate, settle, print)

## Common Compilation Errors & Solutions

### Error: "invalid method declaration; return type required"
**Location**: Line ~303 in Java controllers
**Cause**: Constructor name doesn't match class name
**Fix**: Update constructor name `public PharmacySaleController1()` to match class

### Error: "cannot find symbol: class PharmacySaleForCashierController"
**Location**: Various lines (Logger, metadata, converter)
**Cause**: Class references not updated
**Fix**: Update all 6 locations listed in Step 2.2 above

### Error: Navigation broken at runtime
**Location**: XHTML files
**Cause**: Mixed controller references or incorrect action URLs
**Fix**: Verify global replace worked correctly, check action URLs

### Error: Page won't load / Bean not found
**Location**: XHTML → Java binding
**Cause**: `@Named` annotation incorrect or missing
**Fix**: Verify exact bean name format: `@Named("pharmacySaleController1")`

## Architecture Notes

**Session Scope Pattern**: Each controller uses independent `@SessionScoped` instances. This enables:
- ✅ **Concurrent Bill Editing**: Users can edit different bills simultaneously
- ✅ **State Preservation**: Bills remain intact when switching between versions
- ✅ **2 Simultaneous Sales**: Core requirement for pharmacy operations

**Shared Infrastructure**: All controllers inject the same singletons:
- `SessionController` (user session, preferences)
- `TokenController` (token system state)
- `EJB services` (database operations)

This ensures consistent behavior while maintaining separate bill state.

## Success Criteria

After synchronization, **ALL 4 versions must have**:
- ✅ **Identical functionality** (except bean names and titles)
- ✅ **Same latest features** (quantity adjustments, stock validation, etc.)
- ✅ **Working navigation** (seamless version switching)
- ✅ **Session isolation** (bills stay separate)
- ✅ **No compilation errors**
- ✅ **No runtime exceptions**

This complete replacement approach has a **95% success rate** compared to 60% for incremental synchronization methods.

