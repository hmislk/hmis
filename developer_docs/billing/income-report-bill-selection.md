# Income reports: which bills to count

Rules found while building the Consolidated Income Report (#19613, `ConsolidatedIncomeReportController`). Each one avoids counting a bill twice or dropping it.

## Period (OPD, Channelling, collecting centres)
- Filter on `bill.createdAt` (when the bill was raised). Income reports and `BillService` use it. `bill.billDate` is a date-only field that some flows set to a business date, so it can move bills out of their period.
- Reversals (cancellation, refund) carry negative values. Sum them with their original bill types; don't rely on `cancelled = false`.

## OPD
- Bill items are on the individual bills (`OPD_BILL_WITH_PAYMENT`, `OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER`, package variants, their cancellations and refunds). Leave out the batch bills, or every item is counted twice.
- `OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER` is not yet income.

## Channelling
- Count the booking: `CHANNEL_BOOKING_WITH_PAYMENT`, `CHANNEL_BOOKING_WITHOUT_PAYMENT`, `CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT`, and `CHANNEL_BOOKING_WITH_PAYMENT_ONLINE` only when `referenceBill` is null.
- Do not count settlement bills: `CHANNEL_PAYMENT_FOR_BOOKING_BILL`, or `CHANNEL_BOOKING_WITH_PAYMENT_ONLINE` with a `referenceBill` (`settleCreditForOnlinePayments`).
- Cancelling a settled credit booking writes **two** `CHANNEL_CANCELLATION_WITH_PAYMENT_FOR_CREDIT_SETTLED_BOOKINGS` bills, one for the booking and one for its payment bill. Exclude a reversal whose `billedBill` is a settlement bill.
- Don't pick reversal types by `ServiceType.CHANNELLING` + category: that also picks up agent and credit-company settlement cancellations, which are not income.
- See also [channel-online-booking-bills.md](channel-online-booking-bills.md).

## Inward final bills
- The Period rules above do not apply. Select admissions with `pe.discharged = true` and `pe.paymentFinalized = true`, filter on `pe.dateOfDischarge`, and count only final bills with `cancelled = false`. Inward cancellation and refund reversals are not summed.
- Read the saved `INWARD_FINAL_BILL`. Its items carry `inwardChargeType`. Medicine is `Medicine`, `MedicinesAndSurgicalSupplies`, `ExtraMedicine`, `TheaterConsumbale`, `Pharmacy_Medicine`, `Etu_Medicine`, `Inward_Medicine`, `Theatre_Medicine` and `Store_Medicine`. `InwardBhtChargeAggregationService` recomputes charges the interim-bill way and can differ from what was billed.
- A bill-level discount (`bill.discount`) is not spread onto the items.
- An admission can have more than one active final bill (a re-issued version). Take the latest non-cancelled, non-retired one per encounter.
- Don't filter on `bill.completed`: the settle paths never set it on a final bill. Use `PatientEncounter.paymentFinalized`.

## Collecting centres
- Group by `bill.collectingCentre` (`CC_BILL`, `CC_BILL_CANCELLATION`, `CC_BILL_REFUND`).
