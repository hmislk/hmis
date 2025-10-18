# Allow Fractions in Pharmacy Quantities

Owner: Platform Engineering
Created: 2025-08-24

Goal

- Introduce a configuration at Item level to control whether fractional quantities are allowed for AMPs/AMPPs and enforce it consistently across pharmacy transactions via UI validation and server-side conversion.

Scope

- Entities: `Item` (parent of `Amp`/`Ampp`)
- DTO: `StockDTO` used in autocomplete/search flows
- UI Pages: AMP/AMPP admin, Issues (Disposals/Disbursements), Direct Purchase, GRN (receive/costing/approval), Purchase Return, GRN Return

Out-of-Scope (phase 1)

- Retail sales flows that bind quantity to Integer (`intQty`) controllers. These remain integer-only in this phase. A phase-2 task enables fractions in retail sale if needed.

Design

- Item-level flag `allowFractions` (default `false`). When `false`, quantity inputs must accept integers only. When `true`, decimals are allowed.
- UI applies both client-side filter (`p:keyFilter`) and server-side conversion (`f:convertNumber integerOnly=...`).
- For DTO-based UIs (autocomplete of stocks), surface the flag through `StockDTO` to avoid entity joins in the view.

Changes Implemented

- Entity
  - Added `boolean allowFractions = false` with getter/setter to `src/main/java/com/divudi/core/entity/Item.java`.

- DTO
  - Extended `src/main/java/com/divudi/core/data/dto/StockDTO.java`:
    - Field `Boolean allowFractions` with accessor `isAllowFractions()`.
    - Overloaded constructors to include `allowFractions` without breaking existing calls.

- JPQL providers updated to populate DTO flag
  - `src/main/java/com/divudi/service/pharmacy/StockSearchService.java`
  - `src/main/java/com/divudi/bean/pharmacy/PharmacyFastRetailSaleController.java` (loadAvailableStockDtos)
  - `src/main/java/com/divudi/bean/pharmacy/PharmacyAdjustmentController.java`
  - `src/main/java/com/divudi/bean/pharmacy/StockController.java` (autocomplete variants)

- Admin UI
  - AMP: `src/main/webapp/pharmacy/admin/amp.xhtml` — added `Allow Fractions` toggle.
  - AMPP: `src/main/webapp/pharmacy/admin/ampp.xhtml` — added `Allow Fractions` toggle.

- Transaction UIs (key filter + convert number)
  - Disposal/Issue: `src/main/webapp/pharmacy/pharmacy_issue.xhtml` — uses `stockDto.allowFractions`.
  - Direct Purchase: `src/main/webapp/pharmacy/direct_purchase.xhtml` — uses `currentBillItem.item.allowFractions` for `txtQty`, `txtFreeQty`.
  - GRN Receive variants (row var `bi`):
    - `src/main/webapp/pharmacy/pharmacy_grn.xhtml` — added id `ordQty`, filters for `ordQty` and `freeQty`.
    - `src/main/webapp/pharmacy/pharmacy_grn_with_approval.xhtml` — filters for `ordQty`, `freeQty`.
    - `src/main/webapp/pharmacy/pharmacy_grn_approval_finalized.xhtml` — filters for `ordQty`, `freeQty`.
    - `src/main/webapp/pharmacy/pharmacy_grn_with_save_approve.xhtml` — added id `ordQty`, filters for `ordQty`, `freeQty`.
  - Returns:
    - Direct Purchase Return Form: `src/main/webapp/pharmacy/pharmacy_direct_purchase_return_form.xhtml` — filters for `txtReturningTotalQty`, `txtReturningQty`, `txtReturningFreeQty`.
    - GRN Return Form: `src/main/webapp/pharmacy/pharmacy_grn_return_form.xhtml` — filters for `txtReturningTotalQty`, `txtReturningQty`, `txtReturningFreeQty`.
    - GRN Return With Costing: `src/main/webapp/pharmacy/grn_return_with_costing.xhtml` — filters for `txtReturningTotalQty`, `txtReturningQty`, `txtReturningFreeQty`.
    - Direct Purchase Return: `src/main/webapp/pharmacy/direct_purchase_return.xhtml` — filters for `txtReturningTotalQty`, `txtReturningQty`, `txtReturningFreeQty`.
  - Store Disbursement (for completeness of disbursement pattern):
    - `src/main/webapp/store/store_transfer_issue.xhtml` — filters for both edit and inline qty inputs.

Validation Notes

- For pages using `StockDTO`, flag is available with no backend controller change due to DTO constructor overloads.
- For entity-bound pages, EL reads `item.allowFractions` directly.
- PrimeFaces `p:keyFilter` masks:
  - `'int'` — integers only (no negatives).
  - `'num'` — numbers with optional decimal point; conversion still standardizes server-side.

Known Gaps / Phase 2

- Retail Sale Pages (e.g., `pharmacy_fast_retail_sale*.xhtml`, `pharmacy_bill_retail_sale_for_cashier*.xhtml`) bind quantity to `intQty` in controllers. To enable fractions when flag is true:
  - Change controller quantity from `Integer intQty` to `Double qty` (or add a parallel field),
  - Update calculations and validators accordingly,
  - Apply the same keyFilter/convertNumber patterns using `stockDto.allowFractions`.

Progress Tracker

- Data model flag on `Item`: Done
- StockDTO field + constructors: Done
- JPQL providers returning flag: Done
- AMP/AMPP admin toggles: Done
- Issue/Disposal page filters: Done
- Direct Purchase page filters: Done
- GRN (all listed variants) filters: Done
- Returns (DP/GRN) filters: Done
- Store disbursement filters: Done
- Retail Sale fraction support: Pending (phase 2)

Operational Notes

- Default for existing items is `false` (integers only) to preserve current behavior.
- No schema changes requiring migration are introduced beyond a new boolean column with default `false`.
- Testing: Manual UI verification is sufficient for JSF-only pages. For Java changes, run `./detect-maven.sh test` when requested.

Changed XHTML Files (for QA)

- `src/main/webapp/pharmacy/admin/amp.xhtml`
- `src/main/webapp/pharmacy/admin/ampp.xhtml`
- `src/main/webapp/pharmacy/pharmacy_issue.xhtml`
- `src/main/webapp/pharmacy/direct_purchase.xhtml`
- `src/main/webapp/pharmacy/pharmacy_grn.xhtml`
- `src/main/webapp/pharmacy/pharmacy_grn_with_approval.xhtml`
- `src/main/webapp/pharmacy/pharmacy_grn_approval_finalized.xhtml`
- `src/main/webapp/pharmacy/pharmacy_grn_with_save_approve.xhtml`
- `src/main/webapp/pharmacy/pharmacy_direct_purchase_return_form.xhtml`
- `src/main/webapp/pharmacy/pharmacy_grn_return_form.xhtml`
- `src/main/webapp/pharmacy/grn_return_with_costing.xhtml`
- `src/main/webapp/pharmacy/direct_purchase_return.xhtml`
- `src/main/webapp/store/store_transfer_issue.xhtml`

QA Verification Checklist

Pre‑setup

- Prepare two test items with stock in a test department:
  - Item A: AMP/AMPP where `Allow Fractions = Yes`.
  - Item B: AMP/AMPP where `Allow Fractions = No`.
- If needed, use AMP/AMPP admin to toggle and save; refresh the transaction page after saving.

What to check on every page below

- When the selected item is Item A (allow fractions):
  - Typing/pasting decimals in quantity inputs is allowed (keyFilter mask = `num`).
  - Values like `1.5` or `2.25` are accepted after blur and used in calculations.
- When the selected item is Item B (disallow fractions):
  - Typing/pasting decimals is blocked in the field (keyFilter mask = `int`).
  - If pasted via context menu, server‑side converter rejects non‑integer input on blur; value should not be accepted.

Pages and steps

1) AMP admin — `pharmacy/admin/amp.xhtml`
- Find an AMP and verify the new `Allow Fractions` toggle is present.
- Toggle it, Save, reselect the AMP to confirm persistence.

2) AMPP admin — `pharmacy/admin/ampp.xhtml`
- Find an AMPP and verify `Allow Fractions` toggle is present.
- Toggle it, Save, reselect to confirm persistence.

3) Disposal/Issue — `pharmacy/pharmacy_issue.xhtml`
- In the item autocomplete, select a stock for Item A; in `Quantity` (`txtQty`), confirm decimals allowed; integers still work.
- Select a stock for Item B; confirm only integers allowed; decimals are blocked or rejected on blur.

4) Direct Purchase — `pharmacy/direct_purchase.xhtml`
- Select Item A under “Add New Item”; in `Quantity` (`txtQty`) and `Free Qty` (`txtFreeQty`), confirm decimals allowed.
- Repeat with Item B; confirm only integers allowed.

5) GRN Receive — `pharmacy/pharmacy_grn.xhtml`
- Ensure the bill contains Item A; in the table, for `Receiving Qty` (`ordQty`) and `Recieved Free Qty` (`freeQty`), confirm decimals allowed.
- For Item B rows, confirm only integers accepted.

6) GRN Receive (with approval variants)
- `pharmacy/pharmacy_grn_with_save_approve.xhtml`, `pharmacy/pharmacy_grn_with_approval.xhtml`, `pharmacy/pharmacy_grn_approval_finalized.xhtml`:
  - For Item A rows, decimals allowed; for Item B rows, only integers.

7) Direct Purchase Return — `pharmacy/pharmacy_direct_purchase_return_form.xhtml`
- For Item A, verify decimals accepted for `Returning Total Qty`, `Returning Qty`, `Returning Free Qty`.
- For Item B, verify only integers accepted in the same fields.

8) GRN Return — `pharmacy/pharmacy_grn_return_form.xhtml` and `pharmacy/grn_return_with_costing.xhtml`
- For Item A, verify decimals in `Returning Total Qty`, `Returning Qty`, `Returning Free Qty`.
- For Item B, verify integer‑only behavior.

9) Direct Purchase Return (simple) — `pharmacy/direct_purchase_return.xhtml`
- For Item A and B, verify the same decimal vs. integer behavior on `Returning Total Qty`, `Returning Qty`, `Returning Free Qty`.

10) Store Disbursement — `store/store_transfer_issue.xhtml`
- In the editable qty fields for a row with Item A, decimals accepted; for Item B, integers only.

Notes

- If switching the `Allow Fractions` state during testing, reselect the item on the transaction page to ensure updated behavior.
- Retail sale pages using `intQty` remain integer‑only in this phase by design.
