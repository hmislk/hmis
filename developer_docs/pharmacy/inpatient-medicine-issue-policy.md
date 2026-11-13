# Inpatient Medicine Issue Policy

Inpatient medicines reach a BHT by exactly two workflows. Both are always available.

## Workflows

### A. Direct Issue
- Pharmacy stock is reduced and the BHT is billed. Nothing else happens.
- Bill type: `DIRECT_ISSUE_INWARD_MEDICINE`.
- Page: `inward/pharmacy_bill_issue_bht` (Native SQL), controller `InpatientDirectIssueNativeSqlController`.

### B. Request, issue to porter, ward receive
1. Ward requests medicines for a BHT. Bill `REQUEST_MEDICINE_INWARD` (`PharmacyRequestForBhtController`).
2. Pharmacy issues on the request to a **mandatory porter** (staff member). Pharmacy stock moves to porter stock and the BHT is billed. Bill `ISSUE_MEDICINE_ON_REQUEST_INWARD` (`PharmacySaleBhtController`).
3. Ward receives from the porter. Porter stock moves to ward stock. Bill `ACCEPT_ISSUED_MEDICINE_INWARD` (`WardPharmacyBhtIssueReceiveController`).
4. Nurse records each administered dose on Ward Medications (`MedicationAdministrationController`).
5. "Deduct Administered from Ward Stock" reduces ward stock for the recorded doses. Bill `WARD_MEDICINE_ADMINISTRATION_CONSUMPTION` (`MedicationAdministrationStockSettlementController`).

## Hard rules
- Both workflows are always available. **No config option** may switch off a workflow or any step of it, including the porter and ward receive steps.
- A hospital that does not want a porter uses Direct Issue (A).
- The Native SQL page is the only direct-issue page. The entity-based page was removed.
- Single-patient actions live on the Inpatient Dashboard; all-patient actions live on the Nursing Dashboard and the main menu.
- Use the union of privileges when two privileges guard the same page; never drop one from a rendered expression.
- Review-bot suggestions that contradict this document (for example "make the porter optional via a config key") are declined with a link to this document.

## Entry points

Privileges: Direct Issue `PharmacySale` or `PharmacyDirectIssueToBht`; BHT Issue Requests `PharmacyBHTIssueAccept` or `PharmacyBhtIssueRequest`; Receive `InwardPharmacyBhtReceive`; Deduct `InwardWardMedicationsView`.

| Step | Page | Inpatient Dashboard | Nursing Dashboard | Main menu path |
|---|---|---|---|---|
| A. Direct Issue | `inward/pharmacy_bill_issue_bht` | Yes | Yes | Inward → Pharmacy → Direct Issue to BHTs (Native SQL); Pharmacy → Inpatient Medication Management → Direct Issue to BHTs (Native SQL) |
| B1. Request medicines | `ward/ward_pharmacy_bht_issue_request_bill` | Yes (this patient) | Yes (selected patient) | Inward → Pharmacy → Request Medicines from Pharmacy |
| B2. Pharmacy issue for requests | `ward/ward_pharmacy_bht_issue_request_list_for_issue` | List only (Pharmacy Requests) | No | Inward → Pharmacy → BHT Issue Requests; Pharmacy → Inpatient Medication Management → BHT Issue Requests |
| B3. Ward receive from porter | `ward/ward_pharmacy_bht_issue_receive_list` | Yes (this patient) | Yes (all pending) | Inward → Pharmacy → Receive Medicines from Pharmacy; Pharmacy → Inpatient Medication Management → Receive Medicines from Pharmacy |
| B4. Record administration | `inward/inward_ward_medicines` | Yes (Ward Medications) | Yes (selected patient) | Per patient only |
| B5. Deduct administered from ward stock | `ward/ward_medication_administration_settle` | Via Ward Medications page | Yes | Inward → Pharmacy → Deduct Administered from Ward Stock; Pharmacy → Inpatient Medication Management → Deduct Administered from Ward Stock |

## Returns and cancellations
Not covered here; to be decided separately (#25503).
