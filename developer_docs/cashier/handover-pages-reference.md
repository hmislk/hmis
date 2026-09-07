# Handover Module - Page Reference

> Complete reference of all XHTML pages, print templates, and composite components
> related to the Cash Handover workflow.

**Primary Controller:** `FinancialTransactionController` (`@SessionScoped`)
**Path:** `com.divudi.bean.cashTransaction.FinancialTransactionController`

**Supporting Controllers:**
- `CashBookEntryController` - writes cashbook entries during handover acceptance
- `PaymentHandoverItemController` - manages `PaymentHandoverItem` entity persistence

---

## 1. Handover Creation Flow

### 1.1 Shift End (Pre-Handover)

| Page | File | Purpose |
|------|------|---------|
| Shift End for Handover | `cashier/shift_end_for_handover.xhtml` | Shift closure and preparation page. Shows shift start details, collected payment summary, denomination entry for cash, and overall shift financial breakdown by payment method. |

**Navigation:** `navigateToCreateShiftEndSummaryBillForHandover()`

### 1.2 Handover Start (Current Shift)

| Page | File | Purpose |
|------|------|---------|
| Handover Start | `cashier/handover_start.xhtml` | Initial handover creation page with shift details, payment method breakdown, denomination entry, and list of payments to hand over. Entry point for the handover creation workflow. |
| Handover Start All | `cashier/handover_start_all.xhtml` | Master handover creation dashboard for the current shift. Shows all payment methods collected, handover values, denomination entries, and a large datatable with all shift payment rows organized by date/department/user. |
| Handover Start Select | `cashier/handover_start_select.xhtml` | Per-payment-method detail selection page during creation. Allows selecting individual payments within a payment method, denomination inputs for cash, and individual payment listings for card/cheque/slip/eWallet. |
| Bill Type Details | `cashier/handover_start_all_bill_type_details.xhtml` | Detailed view of shift collections broken down by bill type. Shows institution/site/department/user context and payment method details with sorting/filtering. Allows navigation to individual bills. |

**Navigation:**
- `navigateToHandoverCreateBill()` → `handover_start.xhtml`
- `navigateToHandoverCreateBillForCurrentShift()` → `handover_start_all.xhtml`
- `navigateToSelectPaymentsForHandoverCreate(bundle, paymentMethod)` → `handover_start_select.xhtml`
- `navigateBackToPaymentHandoverCreate()` → `handover_start_all.xhtml`
- `navigateToViewDetailsOfSelectedBundleDuringHandover()` → `handover_start_all_bill_type_details.xhtml`

### 1.3 Handover Start (For Period)

| Page | File | Purpose |
|------|------|---------|
| Handover Start for Period | `cashier/handover_start_for_period.xhtml` | Period-based handover creation with from/to date filters. Shows shift details, payment summary table, denomination entries, and datatable of period collections by department with individual payment method selection. |

**Navigation:** `navigateToHandoverCreateBillForSelectedPeriod()`

### 1.4 Print Pages After Handover Creation

| Page | File | Purpose |
|------|------|---------|
| **Creation Print - Summary** | `cashier/handover_creation_print_summary.xhtml` | Summary print of the newly created handover bill. Uses the `five_five_paper_with_headings_for_handover` composite. Shows payment method totals, handover value, and key identifiers. |
| **Creation Print - Details** | `cashier/handover_creation_print_details.xhtml` | Detailed print of the newly created handover bill. Uses the `five_five_paper_with_headings_for_handover_detail` composite. Shows individual payment line items, denominations, and full breakdown. |

**Navigation:**
- `settleHandoverStartBill()` → `handover_creation_print_summary.xhtml` (auto-navigated after creation)
- `navigateToHandoverCreationPrintSummary()` → `handover_creation_print_summary.xhtml`
- `navigateToHandoverCreationPrintDetails()` → `handover_creation_print_details.xhtml`

---

## 2. Handover Listing / Queue Pages

| Page | File | Purpose |
|------|------|---------|
| My Handovers (From Me) | `cashier/handover_bills_from_me.xhtml` | Lists handovers initiated by the current user. Shows shift numbers, dates, handover details, with from/to date filters. Allows recall/cancel actions. |
| Handovers For Me to Receive | `cashier/handover_bills_for_me_to_receive.xhtml` | Pending handover acceptance queue. Shows bills awaiting acceptance with username, staff name, created date, value, and accept/reject action buttons. |

**Navigation:**
- `navigateToMyHandovers()` → `handover_bills_from_me.xhtml`
- `navigateToReceiveHandoverBillsForMe()` → `handover_bills_for_me_to_receive.xhtml`

---

## 3. Handover Accept Flow

### 3.1 Accept Pages

| Page | File | Purpose |
|------|------|---------|
| Handover Accept | `cashier/handover_accept.xhtml` | Main handover acceptance page. Shows shift details, collected payment table, accept/reject buttons with confirmation dialogs, and summary of handover breakdown by payment method. |
| Handover Accept Select | `cashier/handover_accept_select.xhtml` | Per-payment-method selection interface during acceptance. Shows denomination table for cash or generic payment rows for other methods with bank/cheque/slip/eWallet specific columns. |
| Handover Accept View | `cashier/handover_accept_view.xhtml` | Handover acceptance confirmation and processing page. Shows shift details, payment summary table, accept/reject buttons (privilege-gated), and collection details. |
| Accept Row Detail | `cashier/handover_accept_row_detail.xhtml` | Detail view of an individual handover row. Shows float transactions or normal payment rows with payment method-specific columns (Bank/Cheque No/Ref No) and summary totals. |

**Navigation:**
- `navigateToReceiveNewHandoverBill()` → `handover_accept.xhtml`
- `navigateToSelectPaymentsForHandoverAccept(bundle, paymentMethod)` → `handover_accept_select.xhtml`
- `navigateBackToPaymentHandoverAccept()` → `handover_accept.xhtml`
- `navigateToViewIndividualShiftForHandover(bundle)` → `handover_accept_row_detail.xhtml`

### 3.2 Print Pages After Handover Accept

| Page | File | Purpose |
|------|------|---------|
| **Accept Bill Print** | `cashier/handover_accept_bill_print.xhtml` | Print preview of the accepted handover bill. Uses the `five_five_paper_with_headings_for_handover_accept` composite. |
| Receive Bill Print | `cashier/handover_receive_bill_print.xhtml` | Fund transfer receipt showing from-staff and payment method breakdown. |

**Navigation:**
- `acceptHandoverBillAndWriteToCashbook()` → triggers accept print
- `navigateToHandoverAcceptBillReprintFromReport()` → `handover_accept_bill_print.xhtml`

---

## 4. Handover View / Reprint Flow

| Page | File | Purpose |
|------|------|---------|
| Handover View | `cashier/handover_view.xhtml` | Main handover review page with print navigation buttons. Uses the `five_five_paper_with_headings_for_handover_reprint` composite for displaying handover details. |
| **View Print - Summary** | `cashier/handover_view_print_summary.xhtml` | Printable summary of a completed handover. Uses reprint composite. Navigation buttons for back/view details. |
| **View Print - Details** | `cashier/handover_view_print_details.xhtml` | Printable detail view of a completed handover. Uses detail print composite. |
| Handover Reprint | `cashier/handover_reprint.xhtml` | Handover bill preview for reprinting. Uses the `five_five_paper_with_headings_for_handover` composite. |

**Navigation:**
- `navigateToViewHandoverBill()` / `navigateToHandoverView()` → `handover_view.xhtml`
- `navigateToHandoverViewPrintSummary()` → `handover_view_print_summary.xhtml`
- `navigateToHandoverViewPrintDetails()` → `handover_view_print_details.xhtml`
- `navigateToHandoverReprint()` → `handover_reprint.xhtml`

---

## 5. Proof Missing / Settlement Pages

| Page | File | Purpose |
|------|------|---------|
| Settle Proof Missing | `cashier/settle_handover_proof_missing.xhtml` | Form to settle handover bills with missing proof. Shows bill number, total value, cashier, with input fields for settlement amount and resolution notes. Privilege-gated. |
| Settle Proof Missing Print | `cashier/settle_handover_proof_missing_print.xhtml` | Print receipt for proof missing settlement. Shows settlement bill details, proof missing bill reference, and settlement amount. |

**Navigation:**
- `navigateToSettleHandoverProofMissingBill()` → `settle_handover_proof_missing.xhtml`
- `settleHandoverProofMissingBill()` → `settle_handover_proof_missing_print.xhtml`

---

## 6. Reports

| Page | File | Purpose |
|------|------|---------|
| Handover Status Report | `reports/cashier_reports/handover_status_report.xhtml` | Handover tracking and status report. Supports from/to date filters, initiator/acceptor autocomplete search, and datatable of handover statuses. |

**Navigation:** `navigateToHandoverStatusReport()`

---

## 7. Print Composite Components

These are reusable `<cc:composite>` templates used across multiple handover pages. All accept a `bundle` attribute of type `ReportTemplateRowBundle`.

| Composite | File | Used By |
|-----------|------|---------|
| Handover Summary Print | `resources/ezcomp/prints/five_five_paper_with_headings_for_handover.xhtml` | `handover_creation_print_summary.xhtml`, `handover_reprint.xhtml` |
| Handover Detail Print | `resources/ezcomp/prints/five_five_paper_with_headings_for_handover_detail.xhtml` | `handover_creation_print_details.xhtml`, `handover_view_print_details.xhtml` |
| Handover Accept Print | `resources/ezcomp/prints/five_five_paper_with_headings_for_handover_accept.xhtml` | `handover_accept_bill_print.xhtml` |
| Handover Reprint | `resources/ezcomp/prints/five_five_paper_with_headings_for_handover_reprint.xhtml` | `handover_view.xhtml`, `handover_view_print_summary.xhtml` |

---

## 8. Complete Workflow Summary

### Handover Creation Workflow

```
shift_end_for_handover  →  handover_start  →  handover_start_all
                                                    ├── handover_start_select (per payment method)
                                                    ├── handover_start_all_bill_type_details (drill-down)
                                                    └── [Settle] → handover_creation_print_summary
                                                                        └── handover_creation_print_details
```

### Handover Accept Workflow

```
handover_bills_for_me_to_receive  →  handover_accept
                                         ├── handover_accept_select (per payment method)
                                         ├── handover_accept_row_detail (drill-down)
                                         ├── handover_accept_view (confirmation)
                                         └── [Accept] → handover_accept_bill_print
```

### View / Reprint Workflow

```
handover_bills_from_me  →  handover_view
                               ├── handover_view_print_summary
                               ├── handover_view_print_details
                               └── handover_reprint
```

### Proof Missing Settlement

```
settle_handover_proof_missing  →  [Settle]  →  settle_handover_proof_missing_print
```

---

## 9. Page Count Summary

| Category | Count | Pages |
|----------|-------|-------|
| Creation flow | 5 | shift_end_for_handover, handover_start, handover_start_all, handover_start_select, handover_start_for_period |
| Creation drill-down | 1 | handover_start_all_bill_type_details |
| Creation prints | 2 | handover_creation_print_summary, handover_creation_print_details |
| Listing/queue | 2 | handover_bills_from_me, handover_bills_for_me_to_receive |
| Accept flow | 4 | handover_accept, handover_accept_select, handover_accept_view, handover_accept_row_detail |
| Accept prints | 2 | handover_accept_bill_print, handover_receive_bill_print |
| View/reprint | 4 | handover_view, handover_view_print_summary, handover_view_print_details, handover_reprint |
| Proof missing | 2 | settle_handover_proof_missing, settle_handover_proof_missing_print |
| Reports | 1 | handover_status_report |
| Print composites | 4 | five_five_paper_with_headings_for_handover, _detail, _accept, _reprint |
| **Total pages** | **23** | |
| **Total composites** | **4** | |
