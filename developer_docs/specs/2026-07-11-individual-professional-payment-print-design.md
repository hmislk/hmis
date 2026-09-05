# Individual Per-Patient/BHT Professional Payment Print — Design

- **Date**: 2026-07-11
- **Issue**: [hmislk/hmis#17489](https://github.com/hmislk/hmis/issues/17489)
- **Branch**: `17489-batch-professional-fee-vouchers`
- **Status**: Approved by user (sections reviewed 2026-07-11)

## Problem

When professional (doctor) payments are settled, a single payment voucher is
printed covering all payments in the batch. For surgery lists and busy OPD
sessions this forces staff to take vouchers one payment at a time to get
per-patient documents. We need a second print option that prints **one
voucher per OPD patient / BHT**, separated by page breaks, in one click.

## Scope

New **"Print Individual"** button on all 4 pages where professional payments
are settled:

| Page | Controller |
|---|---|
| `opd/professional_payments/payment_staff_bill.xhtml` | `StaffPaymentBillController` |
| `inward/inward_bill_professional_payment.xhtml` | `InwardStaffPaymentBillController` |
| `inward/inward_bill_surgery_payment.xhtml` | `InwardSurgeryPaymentBillController` |
| `channel/channel_payment_staff_bill.xhtml` | `ChannelStaffPaymentBillController` |

Out of scope (deliberately, easy follow-up): reprint/cancel pages
(`payment_bill_reprint`, `inward_reprint_staff_payment`,
`channel_payment_bill_reprint`, payment-done search pages).

## Key data facts (from investigation)

- Settling creates **one payment `Bill`**; each paid `BillFee` becomes a
  `BillItem` on it with `referanceBillItem`/`referenceBill` pointing to the
  original patient bill.
- Patient resolution: `billItem.referanceBillItem.bill.patientEncounter`
  (BHT) if present, else `billItem.referanceBillItem.bill.patient` (OPD).
- No data-model change needed; grouping is derivable from the persisted
  payment bill.
- Payment-bill money values are stored **negative**; existing vouchers
  display `-bill.total`, `-bill.tax`, `-bill.netTotal`.

## Decisions (user-confirmed)

1. **Pages**: all 4 settle pages.
2. **Content**: full mini-voucher per patient/BHT (header, patient details,
   fee lines, patient subtotal). WHT + grand totals appear **once**, on a
   final summary page (WHT is computed on the whole payment, not per
   patient).
3. **Paper formats**: follow each page's existing paper-format config keys
   (OPD: A4 / 5x5 / POS; inward + surgery: A4 / 5x5; channel: A4).
4. **Signature footer** (Cashier / Payment Received / Professional): on
   **every** patient page (pages may be filed separately per patient/BHT
   file) and on the summary page.
5. **Grouping key**: by patient (OPD) / by BHT (inward). A patient with two
   source bills in the same payment gets one page with both bills' lines.
6. **Approach**: shared grouping helper + reusable composites (Approach 1
   below).

## Approach

Chosen: **shared grouping helper in `ProfessionalPaymentService` + one new
composite per paper format**, wired into all 4 pages.

Rejected alternatives:
- *Duplicated per-page composites*: EL cannot group cleanly; ~10–12
  near-duplicate XHTML files.
- *Server-generated PDF*: perfect page control but new infrastructure;
  every existing voucher uses `p:printer`.

## Design

### Java

**New class** `com.divudi.core.data.ProfessionalPaymentVoucherGroup`:
- Fields: `Patient patient`, `PatientEncounter patientEncounter` (null for
  pure OPD), `List<BillItem> billItems`, `double subtotal`.
- Helpers: `getDisplayName()`; `getDisplayIdentifier()` → BHT number when
  encounter present, otherwise MRN; miscellaneous group returns
  "Miscellaneous".

**New method** in `com.divudi.service.ProfessionalPaymentService`:

```java
List<ProfessionalPaymentVoucherGroup> groupPaymentBillItemsByPatientOrBht(Bill paymentBill)
```

- JPQL fetch of the payment bill's non-retired `BillItem`s (JPQL-first rule).
- Per item, resolve source bill via `referanceBillItem.bill`, falling back to
  `referenceBill` (absorbs channel-path differences — verify the channel
  item path during implementation).
- Group by `patientEncounter` id (BHT) when present, else `patient` id, else
  a single "Miscellaneous" group (misc staff fees with no patient must not
  disappear from the printout).
- Subtotal per group = sum of item `netValue` within the one payment bill
  (single bill type — sign-convention safe); displayed negated (`-value`)
  like the existing vouchers.
- Preserve insertion order of items.

**Controllers** (all 4): add a lazy getter `getIndividualVoucherGroups()`
delegating to the service with `current`; cache in a field; clear the cache
on settle / new payment / makeNull. **Additions only** — no existing
constructor or method signatures change.

### UI

**Three new composites** in `src/main/webapp/resources/bill/`:
- `staff_payment_individual_a4.xhtml`
- `staff_payment_individual_five_five.xhtml`
- `staff_payment_individual_pos.xhtml`

Interface: `controller`, `bill` (same convention as existing voucher
composites). Implementation per composite:

1. `ui:repeat` over `controller.individualVoucherGroups`; each block renders
   the existing voucher layout — institution/department header, "PAYMENT
   VOUCHER" title, bill ID / date / time / payment method, doctor +
   speciality — then a highlighted patient/BHT line (name + BHT no or MRN),
   the group's fee lines (source bill no, service, fee type, value), the
   patient subtotal, and the signature footer. Block wrapper:
   `h:panelGroup layout="block"` with `page-break-after: always`
   (project rule: no `ui:fragment`).
2. Final summary page: Gross Total / Withholding Tax / Net Total
   (`-bill.total`, `-bill.tax`, `-bill.netTotal`) + signature footer; no
   trailing page break.

**Page wiring** (each of the 4 pages):
- New button `id="btnPrintIndividual"`, value "Print Individual",
  `icon="fas fa-print"`, `ajax="false"`, containing
  `<p:printer target="gpIndividualPreview"/>` — placed beside the existing
  print button(s).
- New `h:panelGroup id="gpIndividualPreview"` in the print-preview panel
  containing the composites, conditionally rendered by that page's existing
  paper-format config keys. Preview stacks below existing previews (same
  pattern the OPD page already uses for Detailed/Summarised/Summary);
  `p:printer` prints only the targeted panel.

Existing buttons and composites are untouched — purely additive.

## Edge cases

- **Single-patient payment**: one voucher page + summary page; no
  special-casing.
- **Miscellaneous fees** (no patient/BHT): one "Miscellaneous" block; lines
  still counted in grand totals.
- **Null `current` / empty groups**: getter returns empty list; preview
  renders nothing; no NPEs.
- **Reprint of old payments**: out of scope, but grouping reads only
  persisted data, so extension later is trivial.

## Testing plan

Local Payara build + deploy, then Playwright E2E (login → department →
page):

1. **OPD**: settle a payment covering fees of 2 different patients → preview
   shows 2 patient blocks + summary; correct lines, subtotals, signature
   footer; `page-break-after` present between blocks; grand totals match
   Print Detailed.
2. **Inward + surgery**: same with 2 BHTs; BHT number shown as identifier.
3. **Channel**: settle a channel professional payment; verify grouping works
   with the channel item path.
4. **DB cross-check**: per-group subtotals sum to payment bill `netTotal`
   (+ WHT).
5. **Regression glance**: existing print buttons render unchanged.
