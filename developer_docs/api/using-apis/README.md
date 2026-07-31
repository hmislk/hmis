# Using HMIS APIs — Index

For agents **calling** existing HMIS REST endpoints (the in-app AI Chat, external integrations,
or a coding agent that needs to know what an endpoint returns before writing a caller for it).
Each file below is a self-contained endpoint reference: base path, auth header, request/response
shapes, error codes. Load only the file(s) for the module you need — don't read this whole
directory at once.

If you are instead **building a new** REST endpoint, go to
[`../building-apis/`](../building-apis/) — none of these files cover implementation.

All standard endpoints use the `Finance` header unless noted otherwise below.

| File | Module | Auth |
|---|---|---|
| [API_ADMISSION_NUMBERS.md](API_ADMISSION_NUMBERS.md) | View/reset BHT/OPD admission-number sequence counters | `Finance` |
| [API_BALANCE_HISTORY.md](API_BALANCE_HISTORY.md) | Drawer/patient-deposit/agent balance change history | `Finance` |
| [API_BILL_DATA_CORRECTION.md](API_BILL_DATA_CORRECTION.md) | Correct saved bill finance-detail fields (UPDATE only) | `Finance` |
| [API_BILL_NUMBER_SOLUTION.md](API_BILL_NUMBER_SOLUTION.md) | Working with bill numbers containing `/` in URL paths | `Finance` |
| [API_CHANNEL_BOOKING.md](API_CHANNEL_BOOKING.md) | Channel/booking appointments | `Token` |
| [API_CLINICAL_FAVOURITE_MEDICINES.md](API_CLINICAL_FAVOURITE_MEDICINES.md) | Prescription templates by patient age/sex/setting | `Finance` |
| [API_CONFIG.md](API_CONFIG.md) | Read/update `ConfigOption` application settings | `Config` |
| [API_CONSULTANT_MANAGEMENT.md](API_CONSULTANT_MANAGEMENT.md) | Create/update consultant (doctor) records | `Token` |
| [API_COSTING_DATA.md](API_COSTING_DATA.md) | Retrieve bill details with items + finance details | `Finance` |
| [API_F15_REPORT.md](API_F15_REPORT.md) | Pharmacy daily stock balance (F15) report | `Finance` |
| [API_FHIR.md](API_FHIR.md) | FHIR Patient resource endpoints | `FHIR` |
| [API_FINANCE_LEGACY.md](API_FINANCE_LEGACY.md) | Original `/api/finance` bill query endpoints | `Finance` |
| [API_INSTITUTION_DEPARTMENT_MANAGEMENT.md](API_INSTITUTION_DEPARTMENT_MANAGEMENT.md) | Institution, department, and site CRUD | `Finance` |
| [API_INWARD.md](API_INWARD.md) | Inpatient admission data and payment processing | `Finance` |
| [API_INWARD_ROOM.md](API_INWARD_ROOM.md) | Room categories, rooms, room facility charges | `Finance` |
| [API_LIMS.md](API_LIMS.md) | Lab middleware, analyzer, and sample management | Custom (URL/JSON/Basic) |
| [API_LOGIN_HISTORY.md](API_LOGIN_HISTORY.md) | User login history records | `Finance` |
| [API_MEMBERSHIP.md](API_MEMBERSHIP.md) | Membership scheme registration and payment | none (public) |
| [API_PHARMACEUTICAL_MANAGEMENT.md](API_PHARMACEUTICAL_MANAGEMENT.md) | VTM/ATM/VMP/AMP/VMPP/AMPP item master CRUD + backfill | `Finance` |
| [API_PHARMACY_STOCK_ADJUSTMENTS.md](API_PHARMACY_STOCK_ADJUSTMENTS.md) | Search stocks, adjust qty/rates/expiry, create batches | `Finance` |
| [API_QUICKBOOKS.md](API_QUICKBOOKS.md) | Read-only export of financial data for QuickBooks | `Finance` |
| [API_SERVICE_MANAGEMENT.md](API_SERVICE_MANAGEMENT.md) | `/api/services` service master data | `Finance` |
| [API_SITES.md](API_SITES.md) | Sites (collection points, satellite clinics) | `Finance` |
| [API_STOCK_HISTORY.md](API_STOCK_HISTORY.md) | Transaction-level stock movement records | `Finance` |
| [API_TESTING_WORKFLOWS.md](API_TESTING_WORKFLOWS.md) | Verifying payments/balances after a bill is created | `Finance` |
| [API_USER_MANAGEMENT.md](API_USER_MANAGEMENT.md) | `/api/users` user account management | `Finance` |
| [ITEM_REQUEST_API.md](ITEM_REQUEST_API.md) | External item/service requests against an inpatient BHT | `Finance` |
| [pharmacy-discount-api.md](pharmacy-discount-api.md) | Per-category pharmacy discount percentages by payment scheme | `Finance` |
