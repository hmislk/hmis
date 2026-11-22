# Patient Complaints Register — Business Rules

**Applies to:** complaints from patients, on site or online, kept in one register (issue #25886).

## Where to find it

- Staff, OPD menu: **Patient Complaints** (the register), **Record Patient Complaint** (on-site entry), **Complaint Categories** (admin list).
- Patient, client portal: **My Complaints** on the portal home page.

## Two ways in, one register

- Staff type an on-site complaint on **Record Patient Complaint**. Pick the registered patient, or, for someone who is not registered, type the name and phone.
- A patient logged in to the client portal sends a complaint from **My Complaints**. It is always filed under the patient of the portal login.
- Both land in the same register with a source (On site or Online).
- A repeat of the same text, category and patient within 10 minutes is treated as a double click: the first complaint is kept and no second one is made.

## Status flow

| From | Can move to | Needs |
|---|---|---|
| New | In Progress | nothing |
| In Progress | Closed | a response to the patient |
| Closed | In Progress (reopen) | a note saying why |

- No other jumps (for example New straight to Closed).
- Closing records who closed it and when. Reopening clears the closed date; the earlier response stays on the complaint for staff but the patient does not see it until it is Closed again.
- Assigning does not change the status. A closed complaint cannot be assigned; reopen it first.

## Per institution

The register is per institution. Staff see and open only the complaints of the institution they are logged in to; a complaint of another institution shows "Complaint not found." Complaints sent from the portal are filed under the default institution. A staff login with no institution cannot record complaints.

## Assignee, category, response

- Each complaint has one assignee (a user) and one category. The category list is kept under **Complaint Categories** (needs the Admin privilege); removing a category keeps it on old complaints.
- The response is free text. It can be saved before closing without changing the status. On a Closed complaint it cannot be changed; reopen the complaint first.
- Only a user who holds `PatientComplaintHandle` can be assigned a complaint; the assignee box lists only such users.

## Log

Every change (received, assigned, response saved, status changed) is logged with the user and the time. A complaint sent from the portal is logged against the patient, not a user. The log is for staff only.

## Who can do what

| Privilege | Allows |
|---|---|
| `PatientComplaintHandle` | open the register and complaint page, assign, save response, change status, close, reopen |
| `PatientComplaintCreate` | record an on-site complaint |
| `Admin` | maintain the category list |

Grant them on the User Privileges admin page (OPD, Patient Complaints). A user with only Create can enter complaints but cannot open the register.

## What the portal patient sees

Only their own complaints: category, text, dates, status, and the response once Closed. Never staff names, the assignee, notes or the log. Every portal read is filtered on the server by the patient of the portal login; ids sent from the page are only ever checked against that patient.

## Not included

No email or SMS alerts.

## Database

New tables `PATIENTCOMPLAINT` and `PATIENTCOMPLAINTLOG`. Production has no DDL generation: run migration `v2.30.0` by hand before deploying a build that contains the entities. The category list reuses the shared `CATEGORY` table and needs no change.
