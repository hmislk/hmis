# Multiple Payment Methods: credit and non-credit never combine

**Business rule (lead developer, 2026-11-09):** a bill paid with **Multiple Payment Methods** may only combine **non-credit** methods. Credit methods are never one of the parts.

| Allowed inside Multiple Payment Methods | Never inside Multiple Payment Methods |
|---|---|
| Cash, Card, Cheque, Slip, e-Wallet, Patient Deposit, Patient Points, Online Settlement, Agent, IOU, Voucher, **Staff Welfare** | **Staff Credit** (`PaymentMethod.Staff`), **Credit** (`PaymentMethod.Credit`) |

The split follows `PaymentMethod.getPaymentType()`: `PaymentType.CREDIT` methods are excluded, `PaymentType.NON_CREDIT` methods are allowed. Staff Welfare is non-credit, so it is allowed; Staff Credit is credit, so it is not.

## For developers
- Keep the existing validation in `PaymentService` ("Credit payments are not allowed inside Multiple Payment Methods."), which rejects any `CREDIT`-type component. Do not add a Staff Credit or Credit case to a multi-payment loop, and do not relax that validation for one module.
- A bill that is partly on credit is not a multi-payment bill. Settle it fully on the credit method, or fully with non-credit methods.
- A request to combine them ("allow Staff in multi-payment") is a policy change for the lead developer, not a bug fix.

Background: #25350 (#25339) proposed Staff Credit inside Multiple Payment Methods for package bills; the lead developer declined it. The part of that PR that shipped is the Staff Credit reversal on cancellation in `PaymentService`.
