# Retail Sale Multi-Window — Developer Guideline

This note documents how we maintain four parallel retail-sale pages/controllers that allow users to run simultaneous pharmacy sales in different browser windows. The application was started before JSF view scope was available in our stack, so we use four separate pages and controllers.

## Golden rule

Do not change the original files under any circumstance.

`src/main/webapp/pharmacy/pharmacy_bill_retail_sale.xhtml`
`src/main/java/com/divudi/bean/pharmacy/PharmacySaleController.java`

These are the single source of truth for behaviour and layout.

## Copies that users navigate to

Sale 2
`/pharmacy/pharmacy_bill_retail_sale_1.xhtml` + `PharmacySaleController1.java`

Sale 3
`/pharmacy/pharmacy_bill_retail_sale_2.xhtml` + `PharmacySaleController2.java`

Sale 4
`/pharmacy/pharmacy_bill_retail_sale_3.xhtml` + `PharmacySaleController3.java`

Users can switch among all four pages using the existing navigation buttons. Do not remove these buttons.

## Change workflow

This change is done iteratively once functional or UI change are done and qa passed only in the original files. Then there is a need to replicate the same change into each of the three numbered copies. The copies must not diverge from the original except for bean/page identifiers and navigation targets.

## Replication steps

1. Apply the same edits from the original `.xhtml` to each numbered `.xhtml`. Only permitted differences are the page title and navigation button targets pointing to the correct “Sale N” pages.

2. Mirror controller edits from the original controller into each numbered controller with consistent renames:
   Class `PharmacySaleController` → `PharmacySaleController1|2|3`.
   `@Named("pharmacySaleController")` → `"pharmacySaleController1|2|3"`.

3. In each `.xhtml`, update EL references to the correct bean name:
   `#{pharmacySaleController}` → `#{pharmacySaleController1|2|3}`.

4. Verify navigation button actions and links on every page:
   Sale 1: `/pharmacy/pharmacy_bill_retail_sale.xhtml`
   Sale 2: `/pharmacy/pharmacy_bill_retail_sale_1.xhtml`
   Sale 3: `/pharmacy/pharmacy_bill_retail_sale_2.xhtml`
   Sale 4: `/pharmacy/pharmacy_bill_retail_sale_3.xhtml`

5. Build and test all four pages for the same scenario. Confirm that quantities, payment flows (including multiple methods), printing, and returns behave identically and that data does not cross between controllers.

## What not to do

Do not special-case Sale 1. Do not hide navigation buttons. Do not introduce new bean names or create a fifth copy. Do not change anything in the original files.

## Pre-commit checklist

Original files untouched.
Three `.xhtml` copies updated and aligned.
Three controllers updated and aligned.
EL bean names correct per page.
Navigation among all four pages works.
Smoke tests (add item, discount, settle, print, return) pass on all four pages.

## Automation tips for AI agents

When using Claude Code or similar AI tools to perform this synchronization:

### Recommended Workflow: Delete, Copy, and Update

This approach is simpler and avoids many synchronization errors:

1. **Step 1: Delete all existing copies**
   - Delete `pharmacy_bill_retail_sale_1.xhtml`
   - Delete `pharmacy_bill_retail_sale_2.xhtml`
   - Delete `pharmacy_bill_retail_sale_3.xhtml`
   - Delete `PharmacySaleController1.java`
   - Delete `PharmacySaleController2.java`
   - Delete `PharmacySaleController3.java`

2. **Step 2: Create fresh copies from originals**
   - Copy `pharmacy_bill_retail_sale.xhtml` → `pharmacy_bill_retail_sale_1.xhtml`
   - Copy `pharmacy_bill_retail_sale.xhtml` → `pharmacy_bill_retail_sale_2.xhtml`
   - Copy `pharmacy_bill_retail_sale.xhtml` → `pharmacy_bill_retail_sale_3.xhtml`
   - Copy `PharmacySaleController.java` → `PharmacySaleController1.java`
   - Copy `PharmacySaleController.java` → `PharmacySaleController2.java`
   - Copy `PharmacySaleController.java` → `PharmacySaleController3.java`

3. **Step 3: Update specific locations in each copy**

   For each numbered copy, update these exact locations (use find-and-replace):

   **XHTML Files (_1.xhtml, _2.xhtml, _3.xhtml):**
   - Page title: "Sale 1" → "Sale 2/3/4"
   - Bean name: `#{pharmacySaleController` → `#{pharmacySaleController1/2/3` (GLOBAL replace)
   - Navigation actions: `pharmacy_bill_retail_sale?` → `pharmacy_bill_retail_sale_1/2/3?` (for "New Bill" button)
   - Disabled button: Update which "Sale N" button is disabled to match page number

   **Java Files (Controller1/2/3.java):**
   - Class name: `class PharmacySaleController` → `class PharmacySaleController1/2/3`
   - Constructor: `public PharmacySaleController()` → `public PharmacySaleController1/2/3()`
   - @Named annotation: `@Named` → `@Named("pharmacySaleController1/2/3")` (or add if just `@Named`)
   - Navigation returns: `return "pharmacy_bill_retail_sale"` → `return "pharmacy_bill_retail_sale_1/2/3"` (GLOBAL replace)
   - Converter bean lookup: `"pharmacySaleController"` in ELResolver → `"pharmacySaleController1/2/3"`

### Why This Approach Is Better

- **Prevents drift**: Copies are always in sync with the original
- **Fewer errors**: No partial updates or missed locations
- **Easier to verify**: Only need to check the specific update points
- **Token efficient**: Simple copy operation + targeted find-replace
- **No mixed references**: Fresh copy eliminates risk of stale references

### Verification Checklist

After completing the copy and update process, verify:

1. **Java files compile**: Run Maven compile to catch constructor/class name mismatches
2. **Bean names correct**: Grep for `@Named` in each Java file to verify `"pharmacySaleController1/2/3"`
3. **No mixed references**: Grep for `pharmacySaleController\.` (base controller without number) in XHTML copies - should find ZERO matches
4. **Navigation buttons**: Check that the correct "Sale N" button is disabled on each page
5. **Page titles**: Verify each page shows "Sale 2", "Sale 3", "Sale 4" respectively

### Common Compilation Errors After Sync

If compilation fails, check these common issues:

1. **"invalid method declaration; return type required"** → Constructor name doesn't match class name (line ~130)
2. **"cannot find symbol: class PharmacySaleController"** → Class name not updated in some location
3. **Navigation issues at runtime** → Check return statements still have base page name instead of numbered versions

This delete-copy-update workflow typically prevents 90% of synchronization errors and uses 30-40% fewer tokens than comparison-based methods.

