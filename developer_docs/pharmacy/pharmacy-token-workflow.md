# Pharmacy Token Workflow

Menu → Pharmacy → Token Management (`PharmacyTokenManagement`). All token lists are per department, per day. User video: https://youtu.be/-HS4gyV2kcM

## Flow

Issue (`PHARMACY_TOKEN`) → Manage Pharmacy Tokens → **Retail Sale** (pay at counter) or **Sale for Cashier** → **Pay at Cashier** → Call Token (shown on Called Pharmacy Tokens board) → Complete Service.

Manage Tokens' Bill column comes from `token.bill`: `referenceBill` set → PAYMENT DONE; else not cancelled → AWAITING PAYMENT. Sale buttons show while `token.bill` is null or cancelled.

## Page ↔ bean

| Page | Bean | Token entry |
|---|---|---|
| `pharmacy/pharmacy_bill_retail_sale.xhtml` | `PharmacySaleController` | `tokenController.navigateToNewPharmacyRetailSale()` |
| `pharmacy/pharmacy_bill_retail_sale_for_cashier.xhtml` | `PharmacySaleForCashierController` | `tokenController.navigateToNewPharmacyBillForCashier()` |
| `pharmacy/pharmacy_bill_pre_settle.xhtml` | `PharmacyPreSettleController` | `tokenController.navigateToSettlePharmacyPreBill()` |

## Rules

- Set state on the bean that backs the target page (grep the XHTML), not the deprecated `PharmacySaleController` methods.
- Call the bean's `navigateTo…()` first, then set patient/token: navigation runs `resetAll()`, which clears them.
- Link `token.bill = preBill` (`linkIssuedPharmacyTokenToPreBill()`) before the `Enable token system in sale for cashier` block; otherwise that block creates a duplicate `PHARMACY_TOKEN_SALE_FOR_CASHIER` token.

## Config

- `Enable token system in sale for cashier`: on → Sale for Cashier settle marks the issued token Called.
- `Enable bill edit in sale for cashier token management system`: shows Edit Bill (off; Edit Bill is broken, #24226).
