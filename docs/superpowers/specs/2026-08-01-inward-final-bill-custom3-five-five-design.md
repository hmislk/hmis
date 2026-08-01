# Inward Final Bill — "Custom Bill 2" (5x5 Impact-Printer Format)

**Date:** 2026-08-01
**Branch:** TBD (to be created from `origin/development` at implementation time)
**Related issue:** none provided — reference sample: `tmp/COOP/OPD CARD.pdf`

## Problem

The **Inward Final Bill** screen (`src/main/webapp/inward/inward_reprint_bill_final.xhtml`)
has a "Custom Bills" tab that currently offers exactly one custom print format: a
3-page A4 document (Final Bill → Professional Bill → Final Bill Summary), backed by
the composite `resources/inward/bill/finalBillCustom2.xhtml` and
`BhtSummeryController.getCustom2CategoryTotals(...)`.

One customer (COOP) wants a second custom format available in the same tab: a
single-page, 5-inch-wide bill sized for an impact/dot-matrix printer, replicating a
specific sample bill (`tmp/COOP/OPD CARD.pdf`) produced by their previous system
(Sierra Technology Holdings). It consolidates admission charges into one slip instead
of three A4 pages.

This format is intended for "OPD Card" admissions specifically, but per the user
it should simply be available as a second option in the Custom Bills tab —
staff will pick the appropriate print for the admission type, no admission-type
gating is required.

## Sample being replicated

```
OPD CARD
BILL NO      : DAC/OPC/6374
BILL DATE    : 2026-07-31 : 21:50
PATIENT NAME : MR.S VIJE KUMAR
OPD CARD     : OC43888
------------------------------------------------------------------------
PARTICULAR                                                       AMOUNT
------------------------------------------------------------------------
ADMISSION FEES                                                  1000.00
ROOM CHARGES                                                     1250.00
PHARMACY DRUGS                                                   2129.00
EXTRA MEALS                                                        510.00
WARD PROCEDURE & CONSUMABLE                                      1961.00
E T U CHARGES                                                     250.00
DR.JAYAMINI HORADUGODA (Oncologiss)                             26000.00
------------------------------------------------------------------------
TOTAL AMOUNT                                                   33,100.00
DISCOUNTS                                                        1,200.00
DEPOSIT                                                        10,000.00
NET AMOUNT (LKR)                                               21,900.00

Pay Type      Amount Rs.
Cash          21900.00

BILL PREPARED BY : NUWAN
```

(Footer branding "Software By Sierra Technology Holdings" is **not** replicated —
replaced with "Powered by CareCode", matching every other 5x5 composite in this
codebase.)

## Field mapping

| PDF field | Source | Notes |
|---|---|---|
| Title | Static "FINAL BILL" or similar | Sample says "OPD CARD"; naming confirmed with user as internal label, see Open Questions |
| BILL NO | `bill.deptId` | |
| BILL DATE | `bill.createdAt` | `dd/MM/yyyy hh:mm a` |
| PATIENT NAME | `bill.patientEncounter.patient.person.nameWithTitle` | |
| OPD CARD | `bill.patientEncounter.bhtNo` | Confirmed by user: "opd card is BHT number" |
| Particulars (grouped) | New `BhtSummeryController.getCustom3CategoryTotals(bill)` | Twin of `getCustom2CategoryTotals`, keyed off `BillItem.inwardChargeType`, **but does not exclude `InwardChargeType.ProfessionalCharge`** — doctor/consultant fee prints as its own grouped line, since this is a single-page format with no separate Professional Bill page |
| TOTAL AMOUNT | `bill.total` | |
| DISCOUNTS | `bill.discount` | |
| DEPOSIT | Sum of `bill.backwardReferenceBills[].netTotal` | Prior payments/receipts recorded against this admission — same source Custom2's receipts table already reads. Confirmed by user: "deposits are equal to payments" |
| NET AMOUNT | `bill.netTotal` | |
| Pay Type / Amount | Single row: `bill.paymentMethod` + `bill.paidAmount` | The payment collected at this final settlement (distinct from the Deposit sum above, which covers prior payments) |
| BILL PREPARED BY | `bill.creater.name` | |
| Footer | Static "Powered by CareCode" | Matches other 5x5 composites |

## Layout

Single page, 5-inch wide (12.7cm), reusing the impact/dot-matrix CSS pattern already
established in `resources/ezcomp/prints/five_five_inward_outside.xhtml`:

- Plain sans-serif font, solid black text, no background fills or borders beyond
  simple horizontal rules
- `width: 12.7cm` with matching `@media print` rule and `page-break-after: always`
- Department header block (name / address / phone) at top, consistent with
  existing 5x5 composites — sample's original header ("OPD CARD" branding block)
  is COOP-specific and not replicated; standard department header used instead

## Placement

A second panel pair in the existing **"Custom Bills"** tab of
`inward_reprint_bill_final.xhtml`, alongside the current Original/Duplicate
`Custom Bill` panels (`finalBillCustom2.xhtml`). New pair of panels labelled
**"Custom Bill 2"**, wired the same way (`p:printer` targeting a new
`h:panelGroup` id, Original + Duplicate side by side).

New composite: `src/main/webapp/resources/inward/bill/finalBillCustom3.xhtml`
(internal name `Custom3`, independent of the existing `Custom2` code/config so
nothing about the current format is touched or renumbered).

## Configuration

No new config toggles. Fixed layout matching the sample exactly. (Custom2's
show/hide-address-style toggles are not needed for this narrow, minimal slip;
can be added later if requested — YAGNI for now.)

## Data/query changes

- New method `BhtSummeryController.getCustom3CategoryTotals(Bill bill)` —
  copy of `getCustom2CategoryTotals` without the `ProfessionalCharge` exclusion.
  No JPQL changes; reuses `bill.getBillItems()` already loaded on the bean.
- No entity, DTO, or facade changes required. All fields above already exist on
  `Bill` / `BillItem` / `PatientEncounter` / `Patient`.

## Out of scope

- Any change to the existing Custom2 format, its composites, or its settings dialog.
- Admission-type gating (format is available for any admission; staff choose the
  right print manually, per user instruction).
- New configurable show/hide toggles for the new format.
- The OPD module's `PaperType`-driven print pipeline (`opd_bill_pre_settle.xhtml`,
  `posOpdBill.xhtml`, etc.) — explicitly not touched; this is purely an
  Inward/BHT final-bill feature, despite referencing "OPD Card" terminology.

## Open questions for implementation

- Exact on-page title text for the new format (sample says "OPD CARD"; needs a
  decision — could be literal "OPD CARD", "FINAL BILL", or something else the
  user prefers once they see a rendered preview).
- Confirm department header content (name/address/phone) is acceptable, since the
  sample's original header/branding block is not being replicated.
