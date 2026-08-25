# Pharmacy Retail Sale Multi-Window — Developer Guideline

This note documents how we maintain four parallel retail-sale pages/controllers that allow users to run simultaneous pharmacy sales in different browser windows. The application was started before JSF view scope was available in our stack, so we use four separate pages and controllers.

## Golden rule

**Do not change the original files under any circumstance.**

## Page families covered by this guide

Three independent families use the numbered-copy pattern. Each has its own base page and
base controller; **never mix names between families.**

| Family | Base page | Base controller | Numbered controllers |
|---|---|---|---|
| Sale for Cashier | `pharmacy_bill_retail_sale_for_cashier.xhtml` | `PharmacySaleForCashierController` | `PharmacySaleForCashierController1/2/3` |
| Retail Sale (legacy, entity-based) | `pharmacy_bill_retail_sale.xhtml` | `PharmacySaleController` | `PharmacySaleController1/2/3` |
| Retail Sale (native SQL) | `pharmacy_bill_retail_sale_native.xhtml` | `RetailSaleNativeSqlController` | `RetailSaleNativeSqlController1/2/3` |

> Earlier revisions of this guide named `PharmacySaleController1/2/3` as the Sale for
> Cashier copies. That was wrong — those belong to the legacy Retail Sale family. The
> Sale for Cashier copies are `PharmacySaleForCashierController1/2/3`.

The examples below use the **Sale for Cashier** family. Substitute the base names from the
table above when working on another family.

## Source Files (Single Source of Truth)

**Main XHTML**: `src/main/webapp/pharmacy/pharmacy_bill_retail_sale_for_cashier.xhtml`
**Main Controller**: `src/main/java/com/divudi/bean/pharmacy/PharmacySaleForCashierController.java`

These are the single source of truth for behavior and layout.

## Target Files (Copies that users navigate to)

**Sale 1**: `pharmacy_bill_retail_sale_for_cashier.xhtml` + `PharmacySaleForCashierController.java` (main/original)
**Sale 2**: `pharmacy_bill_retail_sale_for_cashier_1.xhtml` + `PharmacySaleForCashierController1.java`
**Sale 3**: `pharmacy_bill_retail_sale_for_cashier_2.xhtml` + `PharmacySaleForCashierController2.java`
**Sale 4**: `pharmacy_bill_retail_sale_for_cashier_3.xhtml` + `PharmacySaleForCashierController3.java`

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
cp PharmacySaleForCashierController.java PharmacySaleForCashierController1.java
cp PharmacySaleForCashierController.java PharmacySaleForCashierController2.java
cp PharmacySaleForCashierController.java PharmacySaleForCashierController3.java
```

**2.2 Update Each Java Controller**

For **PharmacySaleForCashierController1.java**, make these exact changes:

- **@Named annotation**: `@Named` → `@Named("pharmacySaleForCashierController1")`
- **Class name**: `public class PharmacySaleForCashierController` → `public class PharmacySaleForCashierController1`
- **Constructor**: `public PharmacySaleForCashierController()` → `public PharmacySaleForCashierController1()`
- **Logger**: `Logger.getLogger(PharmacySaleForCashierController.class.getName())` → `Logger.getLogger(PharmacySaleForCashierController1.class.getName())`
- **Metadata**: `metadata.setControllerClass("PharmacySaleForCashierController")` → `metadata.setControllerClass("PharmacySaleForCashierController1")`
- **Converter reference**: `PharmacySaleForCashierController controller = (PharmacySaleForCashierController) facesContext...getValue(..., "pharmacySaleForCashierController")` → `PharmacySaleForCashierController1 controller = (PharmacySaleForCashierController1) facesContext...getValue(..., "pharmacySaleForCashierController1")`
- **Navigation return strings**: any `return "/pharmacy/<base page>?faces-redirect=true"` must point at that copy's own page (`..._1`, `..._2`, `..._3`).

Repeat same pattern for **PharmacySaleForCashierController2.java** (with "2") and **PharmacySaleForCashierController3.java** (with "3").

The Java side is safe to do with the same guarded replace described in Step 3.2 — the
class name is subject to exactly the same corruption hazard as the bean name.

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

**3.2 Update Each XHTML File — use the GUARDED replace**

> ### 🚨 Never use an unanchored global replace
>
> The source page already contains **numbered** navigation references (`...Controller1`,
> `...Controller2`, `...Controller3`) pointing at the other three windows. A plain global
> replace of the bare base name rewrites those too:
>
> ```
> pharmacySaleForCashierController   → pharmacySaleForCashierController1    intended
> pharmacySaleForCashierController1  → pharmacySaleForCashierController11   CORRUPTION
> pharmacySaleForCashierController2  → pharmacySaleForCashierController12   CORRUPTION
> pharmacySaleForCashierController3  → pharmacySaleForCashierController13   CORRUPTION
> ```
>
> The result is references to CDI beans that have no Java definition. EL resolution fails
> and the user gets an error page. This is exactly what caused
> [#15845](https://github.com/hmislk/hmis/issues/15845) — nine phantom bean references
> across the three numbered cashier pages, produced by following the old version of this
> very step.

**Rule: rewrite the already-numbered references FIRST, then the bare base name, anchored on word boundaries.**

For copy `N` (repeat with `N` = 1, 2, 3):

```bash
TARGET=src/main/webapp/pharmacy/pharmacy_bill_retail_sale_for_cashier_N.xhtml

# 1. Protect the existing numbered nav refs behind a placeholder
sed -i -E 's/\bpharmacySaleForCashierController([123])\b/@@KEEP\1@@/g' "$TARGET"

# 2. Rewrite the bare base name, word-boundary anchored
sed -i -E 's/\bpharmacySaleForCashierController\b/pharmacySaleForCashierControllerN/g' "$TARGET"

# 3. Restore the protected refs unchanged
sed -i -E 's/@@KEEP([123])@@/pharmacySaleForCashierController\1/g' "$TARGET"

# 4. Point the page's own navigation/config targets at this copy
sed -i -E 's/\bpharmacy_bill_retail_sale_for_cashier\b/pharmacy_bill_retail_sale_for_cashier_N/g' "$TARGET"
```

Step 4's `\b` anchor matters as much as step 2's: `_` is a word character, so
`\bpharmacy_bill_retail_sale_for_cashier\b` cannot match inside an
already-suffixed `..._for_cashier_1`.

Then set the page title per copy:

- `pharmacy_bill_retail_sale_for_cashier_1.xhtml` → `"Pharmacy Retail Bill (Sale 2)"`
- `pharmacy_bill_retail_sale_for_cashier_2.xhtml` → `"Pharmacy Retail Bill (Sale 3)"`
- `pharmacy_bill_retail_sale_for_cashier_3.xhtml` → `"Pharmacy Retail Bill (Sale 4)"`

**3.3 Mandatory phantom-bean verification**

Run this after every numbered-copy operation, before compiling and before pushing. It
asserts that every bean the pages reference actually has a Java definition:

```bash
BEAN=pharmacySaleForCashierController          # bare base bean name for the family
CLASS=PharmacySaleForCashierController         # base class name
GLOB='src/main/webapp/pharmacy/pharmacy_bill_retail_sale_for_cashier*.xhtml'

for b in $(grep -ohE "\b${BEAN}[0-9]*\b" $GLOB | sort -u); do
  n="${b#$BEAN}"
  grep -rqE "class ${CLASS}${n}\b|\"${b}\"" src/main/java/ || echo "PHANTOM BEAN: $b"
done
```

**Zero output = safe.** Any output means the #15845 defect has been reintroduced — fix it
before compiling. Also confirm no placeholder leaked through:

```bash
grep -rn '@@KEEP' src/main/webapp/pharmacy/ src/main/java/com/divudi/bean/pharmacy/   # must be empty
```

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

Re-run the phantom-bean check from Step 3.3 — that is the authoritative test.

> A bare `grep -n "pharmacySaleForCashierController" ..._1.xhtml # should be 0 matches`
> is **not** a valid check: each copy legitimately references the other three windows'
> beans, so matches are expected. It was that assumption that hid #15845.

```bash
# Verify correct bean names in Java files
grep "@Named" PharmacySaleForCashierController1.java  # @Named("pharmacySaleForCashierController1")
grep "@Named" PharmacySaleForCashierController2.java  # @Named("pharmacySaleForCashierController2")
grep "@Named" PharmacySaleForCashierController3.java  # @Named("pharmacySaleForCashierController3")
```

**5.3 Navigation URLs**
- **Sale 1**: `/pharmacy/pharmacy_bill_retail_sale_for_cashier.xhtml` → `PharmacySaleForCashierController`
- **Sale 2**: `/pharmacy/pharmacy_bill_retail_sale_for_cashier_1.xhtml` → `PharmacySaleForCashierController1`
- **Sale 3**: `/pharmacy/pharmacy_bill_retail_sale_for_cashier_2.xhtml` → `PharmacySaleForCashierController2`
- **Sale 4**: `/pharmacy/pharmacy_bill_retail_sale_for_cashier_3.xhtml` → `PharmacySaleForCashierController3`

**5.4 Window-switch buttons must not destroy a parked cart**

Switching windows is how a cashier parks one customer's bill and serves the next. The
button's `actionListener` must therefore **not** call `resetAll()` on the target window —
it should only clear the settle-in-progress latch. See
`RetailSaleNativeSqlController.switchToThisSaleWindow()` and
`PharmacySaleController.pharmacyRetailSale()`.

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
- [ ] **Guarded replace used** (numbered refs rewritten first — Step 3.2)
- [ ] **Phantom-bean check passes with zero output** (Step 3.3) — non-negotiable
- [ ] **No `@@KEEP` placeholders left** anywhere
- [ ] **All controllers compile** successfully (`./detect-maven.sh compile`)
- [ ] **Bean names verified** (`@Named("pharmacySaleForCashierController1/2/3")`)
- [ ] **Page titles correct** ("Sale 2", "Sale 3", "Sale 4")
- [ ] **Navigation buttons work** (can switch between all versions)
- [ ] **Session isolation works** (bills stay separate in different versions)
- [ ] **Critical features tested** (add item, calculate, settle, print)

## Common Compilation Errors & Solutions

### Error: "invalid method declaration; return type required"
**Location**: Line ~303 in Java controllers
**Cause**: Constructor name doesn't match class name
**Fix**: Update constructor name `public PharmacySaleForCashierController1()` to match class

### Error: "cannot find symbol: class PharmacySaleForCashierController"
**Location**: Various lines (Logger, metadata, converter)
**Cause**: Class references not updated
**Fix**: Update all locations listed in Step 2.2 above

### Error: Error page when clicking "Sale 2" / "Sale 3" / "Sale 4" (issue #15845)
**Location**: XHTML navigation buttons
**Cause**: Unanchored global replace produced double-numbered bean names
(`...Controller11`, `...Controller12`, `...Controller13`) that have no Java definition
**Fix**: Run the Step 3.3 phantom-bean check and repoint each reported reference at the
real bean. Then redo the copy with the guarded procedure in Step 3.2.

### Error: Page won't load / Bean not found
**Location**: XHTML → Java binding
**Cause**: `@Named` annotation incorrect or missing
**Fix**: Verify exact bean name format: `@Named("pharmacySaleForCashierController1")`

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

