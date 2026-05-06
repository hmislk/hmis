# Inward (Inpatient) Module — Developer Reference

This document covers the pages, navigation paths, controllers, and workflow for the Inward (IP) module. It is intended as a quick reference so that developers do not need to rediscover file locations and flow for each issue.

---

## Module Overview

The Inward module manages the full lifecycle of an inpatient admission:

```
Admission → Room Assignment → (Services / Pharmacy / Lab / Timed Services / Outside Charges / Professional Fees)
         → Room Change (if needed) → Clinical Discharge (under development) → Room Discharge → Final Bill → Payment
```

Key concept: **BHT (Bed Head Ticket)** — the unique identifier for one admission episode. One BHT = one admission = one eventual final bill.

---

## Menu Structure

The top-level menu entry is **Inpatient** (privilege: `Inward`).

### Admissions (privilege: `InwardAdmissions`)

| Menu Item | Page | Controller | Notes |
|---|---|---|---|
| Patient Admit | `/inward/inward_admission.xhtml` | `AdmissionController.navigateToAdmitFromMenu()` | Main admission form |
| Patient Lookup & Registration | via controller | `AdmissionController` | Search/register before admitting |
| Edit Admission Details | `/inward/inward_edit_bht.xhtml` | `BhtEditController.prepereForNew()` | Privilege: `InwardAdmissionsEditAdmission` |
| Change Patient for Admission | via controller | `AdmissionPatientChangeController` | Privilege: `InwardAdmissionsEditAdmission` |

### Appointment (privilege: `InwardAppointmentMenu`)

| Menu Item | Page | Controller | Notes |
|---|---|---|---|
| Add Appointment | `/inward/inward_appointment.xhtml` | `appointmentController` | Privilege: `AddInwardAppointment` |
| Appointment Admission | `/inward/appointment_admit.xhtml` | `appointmentController` | Admit from existing appointment; Privilege: `InwardAppointmentAdmission` |
| Manage Appointment | `/inward/inward_appointment_edit.xhtml` | `appointmentController` | |
| Room Reservations | `/inward/inward_reservations_schedule_calendar.xhtml` | `InwardReservationController` | Privilege: `InwardRoomRoomOccupency` |

### Room (privilege: `InwardRoom`)

| Menu Item | Page | Controller | Notes |
|---|---|---|---|
| Admit Room | `/inward/admit_room.xhtml` | `AdmissionController` | Assigns room to admitted patient; shown when admission and room assignment are separate processes (config key: `Patient admission and room assignment are simultaneous processes.`) |
| Room Occupancy | `/inward/inward_room_occupancy.xhtml` | `AdmissionController.navigateToRoomOccupancy()` | Privilege: `InwardRoomRoomOccupency` |
| Room Vacancy | `/inward/inward_room_vacant.xhtml` | `AdmissionController.navigateToRoomVacancy()` | |
| Room Change | `/inward/inward_room_change.xhtml` | `RoomChangeController.recreate()` | Privilege: `InwardRoomRoomChange`; closes old room record, opens new with timestamps |
| Guardian Room Change | `/inward/inward_room_change_guardian.xhtml` | `RoomChangeController.recreate()` | Privilege: `InwardRoomGurdianRoomChange` |

### Services & Items (privilege: `InwardServicesAndItems`)

| Menu Item | Page | Controller | Notes |
|---|---|---|---|
| Add Services & Investigations | `/inward/inward_bill_service.xhtml` | `InwardServiceController` | Privilege: `InwardServicesAndItemsAddServices` |
| Add Services & Investigations with Payments | `/inward/inward_service_bill_ac.xhtml` | `opdBillController` | Only shown when config `Enable Collecting Payments on Add Services & Investigations on Inward` = true |
| Add Outside Charges | `/inward/inward_bill_outside_charge.xhtml` | `InwardAdditionalChargeController.navigateToAddOutsideChargeFromMenu()` | Charges from external providers; Privilege: `InwardServicesAndItemsAddOutSideCharges` |
| Add Professional Fee | `/inward/inward_bill_professional.xhtml` | `InwardProfessionalBillController.navigateToAddProfessionalFeesFromMenu()` | Doctor fees; Privilege: `InwardServicesAndItemsAddProfessionalFee` |
| Add Estimated Professional Fee | `/inward/inward_bill_professional_estimate.xhtml` | `InwardProfessionalBillController.navigateToAddEstimatedProfessionalFeeFromMenu()` | |
| Add Timed Services | `/inward/inward_timed_service_consume.xhtml` | `InwardTimedItemController.navigateToAddInwardTimedServicesFromMenu` | Ongoing charges (room rent, oxygen, etc.); Privilege: `InwardServicesAndItemsAddTimedServices` |

### Billing (privilege: `InwardBilling`)

| Menu Item | Page | Controller | Notes |
|---|---|---|---|
| Interim Bill | `/inward/inward_bill_intrim.xhtml` | `BhtSummeryController` | Running total of all charges; Privilege: `InwardBillingInterimBill` |
| Interim Bill — Estimated Professional Fees | `/inward/inward_bill_intrim_estimate.xhtml` | `BhtSummeryController` | |
| Interim Bill Search | `/inward/inward_search_intrim.xhtml` | `searchController.makeNull()` | Privilege: `InwardBillingInterimBillSearch` |

### Search (privilege: `InwardSearch`)

| Menu Item | Page |
|---|---|
| Admissions | `/inward/inpatient_search.xhtml` |
| Service Bill | `/inward/inward_search_service.xhtml` |
| Professional Bill | `/inward/inward_search_professional.xhtml` |
| Estimated Professional Bill | `/inward/inward_search_professional_estimate.xhtml` |
| Provisional Bill Search | `/inward/inward_search_provisional.xhtml` |
| Final Bill Search | `/inward/inward_search_final.xhtml` |
| Final Bill Search by Discharge Date | `/inward/inward_search_final_check.xhtml` |

### Pharmacy (privilege: `InwardPharmacyMenu`)

| Menu Item | Page | Notes |
|---|---|---|
| Request Medicines from Pharmacy | `/ward/ward_pharmacy_bht_issue_request_bill.xhtml` | Nurse-initiated request; Privilege: `InwardPharmacyIssueRequest` |
| Direct Issue to BHTs | `/inward/pharmacy_bill_issue_bht.xhtml` | Pharmacist issues directly without request |
| Direct Issue to Theatre Cases | `/theater/inward_bill_surgery_issue.xhtml` | For theatre/OT pharmacy issues |
| BHT Issue Requests | `/ward/ward_pharmacy_bht_issue_request_list_for_issue.xhtml` | Pharmacist view to action pending requests |
| View Pharmacy Requests | `/ward/ward_pharmacy_bht_issue_request_bill_search.xhtml` | Search/view requests; Privilege: `InwardPharmacyIssueRequestSearch` |
| Search Inpatient Direct Issues by Bill | `/inward/pharmacy_search_sale_bill_bht.xhtml` | |
| Search Inpatient Direct Issues by Item | `/inward/pharmacy_search_sale_bill_item_bht.xhtml` | |
| Search Inpatient Direct Issue Returns by Bill/Item | `/inward/pharmacy_search_return_bill_bht.xhtml` | |
| Investigation Trace | `/inward/investigation_search_for_reporting_bht.xhtml` | |
| Inpatient Analytics | `/inward/inward_reports.xhtml` | |

---

## Discharge Flow

Clinical discharge is **under development**. Current implemented flow:

1. **Room Discharge** — `/inward/inward_discharge.xhtml` — `DischargeController`
2. **Final Bill Creation** — `/inward/inward_bill_final.xhtml` — `BhtSummeryController` / `InwardBeanController`
3. **Final Bill Breakdown** — `/inward/inward_bill_final_break_down.xhtml`
4. **Finalized Interim Bill** — `/inward/inward_bill_intrim_finalized.xhtml`
5. **Bill Payment** — `/inward/inward_bill_payment.xhtml` — `InwardPaymentController`
6. **Professional Payment** — `/inward/inward_bill_professional_payment.xhtml`
7. **Staff Payment** — `/inward/inward_bill_staff_payment.xhtml` — `InwardStaffPaymentBillController`
8. **Surgery Payment** — `/inward/inward_bill_surgery_payment.xhtml` — `InwardSurgeryPaymentBillController`

### Cancel / Refund Pages

| Page | Purpose |
|---|---|
| `/inward/inward_cancel_bill_final.xhtml` | Cancel final bill |
| `/inward/inward_cancel_bill_payment.xhtml` | Cancel payment bill |
| `/inward/inward_cancel_bill_professional.xhtml` | Cancel professional bill |
| `/inward/inward_cancel_bill_provisional.xhtml` | Cancel provisional bill |
| `/inward/inward_cancel_bill_refund.xhtml` | Cancel refund bill |
| `/inward/inward_cancel_bill_service.xhtml` | Cancel service bill |
| `/inward/inward_bill_refund.xhtml` | Inward refund |
| `/inward/inward_bill_service_refund.xhtml` | Service bill refund (`InwardServiceController`) |
| `/inward/bht_bill_cancel.xhtml` | BHT bill cancel |
| `/inward/inward_deposit_cancel_bill_payment.xhtml` | Cancel deposit payment bill |

---

## Deposits

Patients pay deposits during or after admission. Deposits are deducted from the final bill; remaining balance goes to insurance company or is settled by cash.

| Page | Purpose |
|---|---|
| `/inward/bht_deposit_of_admitted_patient.xhtml` | Deposits for currently admitted patients |
| `/inward/bht_deposit_of_not_discharged_patient.xhtml` | Deposits — not yet discharged |
| `/inward/bht_deposit_of_not_discharged_patient_by_bht.xhtml` | By BHT number |
| `/inward/bht_deposit_by_created_date_all.xhtml` | All deposits by creation date |
| `/inward/bht_deposit_by_created_date_discharged.xhtml` | Discharged patients, by creation date |
| `/inward/bht_deposit_by_discharge_date.xhtml` | By discharge date |
| `/inward/bht_deposit_by_discharge_date_and_created_date.xhtml` | By both dates |

---

## Reprint Pages

| Page | Purpose |
|---|---|
| `/inward/inward_reprint_bill.xhtml` | General bill reprint |
| `/inward/inward_reprint_bill_final.xhtml` | Final bill reprint |
| `/inward/inward_reprint_bill_intrim.xhtml` | Interim bill reprint |
| `/inward/inward_reprint_bill_payment.xhtml` | Payment bill reprint |
| `/inward/inward_reprint_bill_professional.xhtml` | Professional bill reprint |
| `/inward/inward_reprint_bill_refund.xhtml` | Refund bill reprint |
| `/inward/inward_reprint_bill_service.xhtml` | Service bill reprint |
| `/inward/inward_reprint_staff_payment.xhtml` | Staff payment reprint |
| `/inward/pharmacy_reprint_bill_sale_bht.xhtml` | Pharmacy issue bill reprint |
| `/inward/pharmacy_reprint_bill_return_bht.xhtml` | Pharmacy return bill reprint |

---

## Credit / Insurance Management

| Page | Purpose |
|---|---|
| `/credit/credit_compnay_bill_inward.xhtml` | Credit company bill — by BHT |
| `/credit/credit_compnay_bill_inward_all.xhtml` | Credit company bill — by company |
| `/credit/credit_compnay_bill_payment_inward.xhtml` | Credit company payment |
| `/credit/inward_due_search.xhtml` | Inward due search |
| `/credit/inward_due_age.xhtml` | Inward due ageing |
| `/credit/inward_bht_credit_payment_report.xhtml` | BHT credit payment report |
| `/credit/inpatient_credit_company_bill_cancel.xhtml` | Cancel credit bill |
| `/credit/inpatient_credit_company_bill_reprint.xhtml` | Reprint credit bill |

---

## Theatre / Surgery

| Page | Purpose | Controller |
|---|---|---|
| `/theater/inward_bill_surgery.xhtml` | Surgery billing — **legacy, replaced by `surgery_workbench.xhtml`** | `SurgeryBillController` |
| `/theater/surgery_workbench.xhtml` | Surgery workbench (current page) | `SurgeryBillController` |
| `/theater/surgery_professional_fees.xhtml` | Add surgery professional fees (bill-per-session) | `InwardProfessionalBillController.navigateToSurgeryProfessionalFees(Bill)` |
| `/theater/surgery_professional_fees_list.xhtml` | List of professional fee bills for a surgery | `InwardProfessionalBillController.navigateToSurgeryProfessionalFeesList(Bill)` |
| `/theater/surgery_professional_fees_cancel.xhtml` | Cancel a professional fee bill (with reason + print) | `InwardProfessionalBillController.navigateToSurgeryProfessionalFeeCancel(Bill)` |
| `/theater/inward_bill_surgery_issue.xhtml` | Pharmacy direct issue to theatre | |
| `/theater/inward_timed_service_consume_surgery.xhtml` | Timed services for surgery/theatre | `InwardTimedItemController` |
| `/inward/inward_surgery_type.xhtml` | Admin: surgery types setup | `SurgeryTypeController` |

---

## Admission Profile (Central Hub)

`/inward/admission_profile.xhtml` — `BhtSummeryController.navigateToInpatientProfile()`

This is the central dashboard for a single BHT. After searching for or selecting a patient, many workflows navigate here first, then branch to specific actions.

---

## Baby Admission

`/inward/inward_admission_child.xhtml` — `AdmissionController.navigateToAddBabyAdmission()`

A separate flow for baby admissions. Birth time capture is a known pending improvement (Issue #19297).

---

## Price Adjustments

Accessible from Admin → Manage Inpatient Services:

| Page | Purpose |
|---|---|
| `/inward/inward_price_adjustment_service.xhtml` | Price adjustment for services |
| `/inward/inward_price_adjustment_investigation.xhtml` | Price adjustment for investigations |
| `/inward/inward_price_adjustment_pharmacy.xhtml` | Price adjustment for pharmacy |
| `/inward/inward_price_adjustment_store.xhtml` | Price adjustment for store items |

---

## Administration / Metadata Configuration

Accessed via **Admin → Manage Inpatient Services** → `/inward/inward_administration.xhtml` (Privilege: `InwardAdministration`)

| Configuration Area | Page | Controller |
|---|---|---|
| Admission Types | `/inward/inward_admission_type.xhtml` | `AdmissionTypeController` |
| Admission Items and Fees | `/inward/inward_admission_items_and_fees.xhtml` | `AdmissionTypeController.navigateToManageAdmissionItemsAndFees()` |
| Surgery Types | `/inward/inward_surgery_type.xhtml` | `SurgeryTypeController` |
| Room Categories (Facility Categories) | `/inward/inward_room_category.xhtml` | `RoomCategoryController` |
| Rooms | `/inward/inward_room.xhtml` | `RoomController` |
| Room Fees | `/inward/inward_room_facility.xhtml` | `RoomFacilityChargeController` |
| Time-based Service Categories | `/inward/inward_timed_item_category.xhtml` | `TimedItemCategoryController` |
| Time-based Services | `/inward/inward_timed_item.xhtml` | `TimedItemController` |
| Time-based Service Charges | `/inward/inward_timed_item_fee.xhtml` | `TimedItemFeeController` |
| Surgeries (Clinical Procedures) | `/clinical/clinical_procedures.xhtml` | |
| Errors in Admissions | `/inward/inward_admission_edit.xhtml` | |

---

## Key Controllers — Quick Reference

| Controller | Package | Responsibility |
|---|---|---|
| `AdmissionController` | `com.divudi.bean.inward` | Admission, patient search, navigation hub |
| `BhtEditController` | `com.divudi.bean.inward` | Edit BHT/admission details |
| `BhtSummeryController` | `com.divudi.bean.inward` | Interim bill, admission profile, final bill |
| `BhtSummeryFinalizedController` | `com.divudi.bean.inward` | Finalized interim bill |
| `BillBhtController` | `com.divudi.bean.inward` | BHT bill operations |
| `DischargeController` | `com.divudi.bean.inward` | Room discharge |
| `InwardBeanController` | `com.divudi.bean.inward` | Core inward business logic, final bill |
| `InwardPaymentController` | `com.divudi.bean.inward` | Inward payments and deposits |
| `InwardServiceController` | `com.divudi.bean.inward` | Add services & investigations to BHT |
| `InwardAdditionalChargeController` | `com.divudi.bean.inward` | Outside charges |
| `InwardProfessionalBillController` | `com.divudi.bean.inward` | Professional (doctor) fees |
| `InwardTimedItemController` | `com.divudi.bean.inward` | Timed services (room, oxygen, etc.) |
| `InwardRefundController` | `com.divudi.bean.inward` | Refunds |
| `InwardStaffPaymentBillController` | `com.divudi.bean.inward` | Staff payment bills |
| `InwardSurgeryPaymentBillController` | `com.divudi.bean.inward` | Surgery payment bills |
| `RoomChangeController` | `com.divudi.bean.inward` | Room change with timestamp management |
| `RoomController` | `com.divudi.bean.inward` | Room master data |
| `RoomCategoryController` | `com.divudi.bean.inward` | Room category master data |
| `RoomOccupancyController` | `com.divudi.bean.inward` | Room occupancy tracking |
| `SurgeryBillController` | `com.divudi.bean.inward` | Surgery billing |
| `SurgeryTypeController` | `com.divudi.bean.inward` | Surgery type master data |
| `TheatreServiceController` | `com.divudi.bean.inward` | Theatre service management |
| `NursingWorkBenchController` | `com.divudi.bean.inward` | Nursing workbench (under development) |
| `InwardReportController` | `com.divudi.bean.inward` | Inward reports |
| `InwardReportControllerBht` | `com.divudi.bean.inward` | BHT-level reports |
| `AdmissionTypeController` | `com.divudi.bean.inward` | Admission type master data |
| `InwardPriceAdjustmntController` | `com.divudi.bean.inward` | Price adjustments (note: typo in class name is intentional — DB compatibility) |
| `PatientSampleController` | `com.divudi.bean.lab` | Lab sample collection |
| `SampleController` | `com.divudi.bean.lab` | Lab sample management, receive/reject |

---

## Pharmacy Workflows in Inward

Two parallel workflows exist:

### 1. Request-Based (Nurse → Pharmacy)
- Nurse creates request: `/ward/ward_pharmacy_bht_issue_request_bill.xhtml`
- Pharmacist views and actions requests: `/ward/ward_pharmacy_bht_issue_request_list_for_issue.xhtml`
- Pharmacist issues against request: `/ward/ward_pharmacy_bht_issue.xhtml`

### 2. Direct Issue (Pharmacy → BHT)
- Pharmacist issues directly: `/inward/pharmacy_bill_issue_bht.xhtml`
- For theatre cases: `/theater/inward_bill_surgery_issue.xhtml`

### Return Workflows
- Nurse-initiated return request → pharmacist accepts (partially implemented — Issue #19312 related)
- Direct return by pharmacist: `/inward/pharmacy_bill_return_bht_issue.xhtml`

---

## Reports

| Page | Purpose |
|---|---|
| `/inward/inward_reports.xhtml` | Inpatient analytics hub |
| `/inward/report_admissions.xhtml` | Admissions report |
| `/inward/admission_book.xhtml` | Admission book |
| `/inward/admission_book_all.xhtml` | All admissions book |
| `/inward/discharge_book.xhtml` | Discharge book |
| `/inward/inward_report_bht_income_by_caregories.xhtml` | BHT income by category |
| `/inward/inward_report_professional_payment.xhtml` | Professional payment report |
| `/inward/inward_report_professional_payment_due.xhtml` | Professional payment due |
| `/inward/report_doctor_payment.xhtml` | Doctor payment report |
| `/inward/report_doctor_payment_summery.xhtml` | Doctor payment summary |
| `/inward/inward_report_room.xhtml` | Room income report |
| `/inward/inward_report_timed_service.xhtml` | Timed service report | `InwardTimedItemController` | Filters: date range, timed item, **institution, site, department** (fixed #19715) |
| `/inward/report_income_room.xhtml` | Room income |
| `/inward/inward_report_discount.xhtml` | Discount report |
| `/inward/pharmacy_report_bht_issue_by_item.xhtml` | Pharmacy BHT issue by item |
| `/inward/report_bht_issue_by_bill.xhtml` | BHT issue by bill |
| `/inward/reports/inpatient_lab_investigation_item_list.xhtml` | Lab investigation item list |
| `/inward/reports/inpatient_pharmacy_item_list.xhtml` | Pharmacy item list |
| `/inward/reports/inpatient_service_item_list_dto.xhtml` | Service item list (DTO) |

---

## Known Open Issues (RH Issue Tracker)

See parent issue [#19290](https://github.com/hmislk/hmis/issues/19290) for the full list of open inward issues reported by Ruhunu Hospital.

Key areas with multiple open issues:
- **Discharge validation** — #19307, #19308 (pending transactions / pending payments)
- **Room overlap validation** — #19295, #19300
- **Nursing workbench** — #19301
- **Clinical discharge** — not yet implemented
- **Baby admission** — #19297 (birth time capture)
- **Surgery management on BHT** — #19317 (duplicate surgery, cannot remove)
- **Pharmacy privileges** — #19309 (medicine amount privilege), #19312 (qty limits)
- **Lab privileges** — #19319 (sample receive/reject not enforced)

## Timed Service Report — Filter Pattern

**Controller**: `InwardTimedItemController` (`@SessionScoped`)

The timed service report (`inward_report_timed_service.xhtml`) supports the following filters, all optional:

| Filter | Field | JPQL path |
|---|---|---|
| Date range | `frmDate` / `toDate` | `i.patientEncounter.dateOfDischarge between :fd and :td` |
| Timed item | `current.item` | `i.item = :item` |
| Institution | `institution` (`Institution`) | `i.patientEncounter.institution = :ins` |
| Site | `site` (`Institution`) | `i.patientEncounter.department.site = :site` |
| Department | `department` (`Department`) | `i.patientEncounter.department = :dept` |

**Department dropdown** uses 4 rendered variants (same pattern as `AdmissionReportController`) — the list of selectable departments is narrowed based on which of institution/site is currently selected. Selecting institution or site triggers a `p:ajax` call to `clearDepartment()` so a stale department from the previous selection is not silently applied.

**Site** is stored as `Institution` (not a separate entity). Sites are loaded via `institutionController.sites`.
