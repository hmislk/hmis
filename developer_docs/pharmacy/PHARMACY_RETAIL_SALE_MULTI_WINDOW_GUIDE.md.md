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

1. **Use a specialized agent**: Launch a `general-purpose` agent with explicit instructions to:
   - Read the original files first to understand current state
   - Copy entire file contents to each numbered copy
   - Apply systematic find-and-replace for bean names
   - Update page-specific elements (titles, navigation actions)

2. **Provide complete context in a single prompt** rather than iterative instructions:
   - List all 8 files (2 originals + 6 copies) with their exact paths
   - Specify exact bean name mappings (e.g., `pharmacySaleController` → `pharmacySaleController1`)
   - Include page title mappings (e.g., "Sale 1" → "Sale 2")
   - List action URL mappings (e.g., `pharmacy_bill_retail_sale` → `pharmacy_bill_retail_sale_1`)

3. **Token optimization**:
   - DO NOT read large files line-by-line for comparison
   - DO NOT use Grep for multiple patterns sequentially
   - Instead: Read original once, then perform complete file replacement with systematic substitutions
   - For Java files: Read first 150 lines to verify package/imports/class declaration, then perform full replacement

4. **Verification strategy**:
   - After updates, spot-check 2-3 EL expressions per file using Grep
   - Verify class names and @Named annotations in Java files using Grep with pattern matching
   - Check navigation button disabled states match the page number

5. **Example agent prompt structure**:
   ```
   Task: Synchronize 3 copies from 2 originals

   Originals (DO NOT MODIFY):
   - [full path] uses #{beanName}
   - [full path] @Named("beanName")

   Copies to UPDATE (list all 6):
   - [full path] → use #{beanName1}, title "Sale 2", action "page_1"
   - [full path] → @Named("beanName1")
   (repeat for all copies)

   Instructions:
   1. Read original files
   2. For each copy: full content replacement with substitutions
   3. Verify with spot checks
   ```

This approach typically uses 30-40% fewer tokens than iterative comparison methods.

