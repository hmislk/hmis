# Pharmacy Token Workflow

How a pharmacy queue token travels from issue to completion: which page is backed by which bean,
how a token gets linked to its bill, and the traps that broke this flow before #24225.

User-facing walkthrough video: https://youtu.be/-HS4gyV2kcM

## Menu path

**Menu → Pharmacy → Token Management** (privilege `PharmacyTokenManagement`). This opens
`token/index.xhtml`, a template with a left button panel that every token page uses:

| Button | Page | Bean method |
|---|---|---|
| New Pharmacy Token | `token/pharmacy_token.xhtml` → `token/pharmacy_token_print.xhtml` | `tokenController.navigateToCreateNewPharmacyToken()` / `settlePharmacyToken()` |
| Manage Pharmacy Tokens | `token/pharmacy_tokens.xhtml` | `navigateToManagePharmacyTokens()` (today's, not completed) |
| Called Pharmacy Tokens | `token/pharmacy_tokens_called.xhtml`, the full-screen **token board** for the waiting-area TV | `navigateToManagePharmacyTokensCalled()` |
| Called Pharmacy Tokens (CounterWise) | `token/pharmacy_tokens_called_counter_wise.xhtml` | `navigateToManagePharmacyTokensCalledByCounter()` |
| Completed Pharmacy Tokens | `token/pharmacy_tokens_completed.xhtml` | `navigateToManagePharmacyTokensCompleted()` |

Tokens are **per department, per day**. Every list filters on `t.department = session department`
and `t.tokenDate = today`, and the token number restarts daily
(`billNumberGenerator.generateDailyTokenNumber`).

## Lifecycle

```
Issue (PHARMACY_TOKEN, Pending)
  ├─ Retail Sale ───────► pharmacy_bill_retail_sale.xhtml  (pay at the counter)
  │                        settle → token.bill = pre-bill, pre-bill.referenceBill = sale bill → "PAYMENT DONE"
  └─ Sale for Cashier ──► pharmacy_bill_retail_sale_for_cashier.xhtml
                           settle → token.bill = pre-bill (PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER) → "AWAITING PAYMENT"
                           Pay at Cashier → pharmacy_bill_pre_settle.xhtml → referenceBill set → "PAYMENT DONE"
Call Token   → called = true          → shows on the token board
Complete     → completed = true       → leaves Manage list, listed under Completed
```

The **Bill** column on Manage Tokens is derived entirely from `token.bill`:

- `bill.referenceBill != null` → *PAYMENT DONE*
- otherwise not cancelled → *AWAITING PAYMENT*
- otherwise → *ALL ITEMS GET ADDED TO STOCK*

The sale buttons show while `token.bill` is null **or cancelled**, so a token whose bill was cancelled
can be billed again.

## Page ↔ bean map (the trap)

| Page | Backing bean | Token entry point |
|---|---|---|
| `pharmacy/pharmacy_bill_retail_sale.xhtml` | `PharmacySaleController` | `TokenController.navigateToNewPharmacyRetailSale()` |
| `pharmacy/pharmacy_bill_retail_sale_for_cashier.xhtml` | **`PharmacySaleForCashierController`** | `TokenController.navigateToNewPharmacyBillForCashier()` |
| `pharmacy/pharmacy_bill_pre_settle.xhtml` | `PharmacyPreSettleController` | `TokenController.navigateToSettlePharmacyPreBill()` |

`PharmacySaleController.navigateToPharmacyBillForCashier()` is `@Deprecated`. It redirects to the
Sale-for-Cashier page, but that page reads `pharmacySaleForCashierController`, so anything set on
`pharmacySaleController` is invisible there. Before #24225, the token button did exactly that, and the
page opened empty.

Two rules when handing state to a sale page:

1. **Set the state on the bean that backs the target page.** Grep the XHTML for its `#{...Controller.`
   references before choosing.
2. **Navigate first, then set the state.** These `navigateTo…()` methods call `resetAll()` →
   `clearBill()`, which nulls `patient`, `token` and `currentToken`. Setting the patient or token
   before calling them throws it away.

## Linking the token to the bill

`linkIssuedPharmacyTokenToPreBill()` sets `token.bill = preBill` for a `PHARMACY_TOKEN`:

- In `PharmacySaleForCashierController`, it runs in both settle paths (`settlePreBillAndNavigateToPrint`,
  `settlePreBill`) **before** the `Enable token system in sale for cashier` block.
- In `PharmacySaleController`, it runs at the end of `settleBillWithPay()`, before `resetAll()`.

The order matters when that config is on. The block looks up `findPharmacyTokens(preBill)` (a
`PHARMACY_TOKEN` whose `bill` is this pre-bill):

- **found** → `markToken()`: the issued token is marked *Called*.
- **not found** → it creates a *second* token of type `PHARMACY_TOKEN_SALE_FOR_CASHIER`. That is
  meant for sales that never had a token, and it duplicated the issued token before the link was
  added.

## Config keys

All are resolved per department first (`"<Dept> - <key>"`); see
[Institution-specific behavior](../configuration/institution-specific-behavior.md). After changing a
value with SQL, redeploy, because the config is cached at startup.

| Key | Effect on this flow |
|---|---|
| `Enable token system in sale for cashier` | On: settling a Sale for Cashier for an issued token marks it *Called*. Sales without a token get their own `PHARMACY_TOKEN_SALE_FOR_CASHIER` token. |
| `Enable bill edit in sale for cashier token management system` | Shows **Edit Bill** on Manage Tokens and on Search Sale for Cashier Bills. Off by default; see below. |
| `Print Total Amount On Pharmacy Token` | Adds the bill total to the sale-bill token slips (`resources/pharmacy/saleBill*Token*.xhtml`), not to the queue token printed at issue. |

## Known gaps

- **Edit Bill** (#24226): `TokenController.navigateToSaleForCashier()` cancels the pre-bill (stock
  returned) before the user does anything, and loads its items into the deprecated
  `PharmacySaleController`, so the user lands on a blank page with nothing to edit. It is hidden
  unless the config above is on. A proper edit needs a design decision; cancellation stays whole-bill.
- **Counters**: the Counter dropdowns list `sessionController.loggableSubDepartments`. With no counter
  sub-departments configured they offer only *Any*/*All*.
- The Manage Tokens page nests an `<h:form>` inside `token/index.xhtml`'s form. Browsers drop the inner
  form tag and the buttons still work, but don't add more nested forms.

## Testing notes

- Leave test tokens in place (CLAUDE.md). Because lists are per department per day, a clean list for a
  demo or screenshot is easiest in another pharmacy department that hasn't issued a token today.
- Icon-only row buttons lose their `title` after the first hover; locate them by icon class or id. See
  [Playwright E2E Workflow §137](../testing/playwright-e2e-workflow.md).
- Receipt pages call `window.print()`. In a headless *recording*, stub it (see the demo-video skill).
