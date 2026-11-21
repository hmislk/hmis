# Visit History: Business Rule

Issue #25846 (part of #25857).

## Rules
1. There is no per-patient sequential visit number. Do not add one.
2. Document numbers stay as they are: OPD bill number, inpatient BHT number, lab = its bill number.
3. A patient's visits are shown as a **Visit History**, computed at query time by patient id. Never store it. Merge and demerge are then correct with no extra work.
4. Scope is every encounter that has a bill or an admission: OPD, inpatient, lab, channel, emergency, pharmacy retail, and others.
5. Cancellations and returns/refunds are separate rows, marked as such. Never hide or net them out.
6. Each row has: date, visit type label, reference number, department/doctor, status, link to the existing bill/admission preview.
7. The visit type label is derived from the bill type or encounter type. It is the "prefix by visit type". The mapping lives in one place in code.
8. The list can be filtered by visit type.
9. Patient portal: a patient sees only rows for their own patient id. Internal bill types the patient should not see are excluded (list to be agreed).
10. Staff report: needs its own privilege; staff select a patient and see all rows.
11. The report is added to both the category tab and the Favorites tab of its index page.
12. Links go to the existing preview pages (see bill-preview-navigation-guide.md). Do not build new previews.

## For developers
- JPQL constructor query into a DTO; do not modify existing constructors (DTO Implementation Guidelines).
- Filter by patient id with id lists, not entity lists. Date filters use `TemporalType.TIMESTAMP`.
- Check an index on the patient column of the bill table.
- Related: cancellation-vs-return-policy.md (cancel and return are different bill kinds and both shown), multiple-payment-methods-policy.md.
- Related wiki: Patient-Merge-and-Demerge.
