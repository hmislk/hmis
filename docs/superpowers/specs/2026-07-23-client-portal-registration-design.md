# Client Portal Registration — Design

**Issue**: [hmislk/hmis#398](https://github.com/hmislk/hmis/issues/398)
**Date**: 2026-07-23

## Problem

Issue #398 ("Web Interface for Clients") is an old, vague issue (body:
"The index page should display details of the institution for clients.")
that was triaged into the backlog with no concrete design. This spec
refocuses it: how does a client (patient) actually get a portal account?

Scope is deliberately narrow — **registration and a minimal landing page
only**. What the account can *do* once logged in (view bills, appointments,
lab reports, etc.) is explicitly out of scope, left for future issues, so
the account isn't designed around features that don't exist yet.

## Existing mechanisms this design reuses

- **OTP + multi-match selection already ships**, just scoped to channel
  booking payments: `PatientPortalController`
  (`src/main/java/com/divudi/bean/channel/PatientPortalController.java`,
  `@ViewScoped`). It generates a numeric OTP, sends it via the `Sms` entity
  (`core/entity/Sms.java`) tagged `MessageType.PatientPortalOTP`
  (`core/data/MessageType.java`), verifies by comparing against the last
  `Sms` row for that number/type, and — critically — its `findPatients()`
  already implements "0 matches / 1 match / >1 matches → let the user pick"
  against `Patient.patientPhoneNumber`. This is the exact family/shared-phone
  behavior this feature needs, just for a different purpose (payment, not
  account creation) — extend/refactor rather than reinvent.
- **SMS sending**: `com.divudi.ejb.eSmsManager` / `SmsManagerEjb`.
- **Email sending**: `com.divudi.ejb.EmailManagerEjb` /
  `bean/common/EmailController.java` — exists for email delivery in general,
  but has **no OTP capability today**; that's new work.
- **IP-restricted login already exists for staff**: `WebUser` has
  `restrictLoginByIp` (boolean) + `allowedIpAddresses` (CSV) +
  `isIpAllowed(String requestIp)`, enforced in `SessionController`. The
  kiosk registration channel follows the same shape, but since no account
  exists yet at registration time, the allowlist must live in
  `ConfigOptionApplicationController` (a config key), not on a per-user
  field.
- **No existing patient-portal account entity**. `WebUser` is staff-shaped
  (`Staff`, `department`, `site`, `institution`, `WebUserRole` privileges) —
  deliberately **not** reused for clients, to avoid ever mixing patient
  logins with the staff privilege system.
- **No "CLIENT"/"PATIENT" role or privilege exists** in the `WebUserRole` /
  `WebUserPrivilege` system, confirming client auth is a fully separate path.

## Design

### 1. Data model

New entity `ClientAccount`, `@OneToOne` to `Person` (not to `Patient`
directly — a `Person` may correspond to multiple `Patient` records across
visits/departments, but exactly one portal identity).

| Field | Notes |
|---|---|
| `person` | `@OneToOne`, the portal identity |
| `passwordHash` | set at registration (all channels) |
| `verifiedPhone` | the phone number OTP-verified at registration/most recent update |
| `verifiedEmail` | the email OTP-verified at registration/most recent update |
| `phoneVerified` / `emailVerified` | booleans |
| `createdVia` | enum: `SELF_PHONE`, `SELF_EMAIL`, `STAFF_ASSISTED`, `KIOSK` |
| `createdByWebUser` | nullable `WebUser`, set only for `STAFF_ASSISTED` |
| `retired` | boolean, soft-delete flag (standard codebase convention, matching `Sms.java`) — `false` means active |
| `retirer` | nullable `WebUser`, the staff member who retired the account |
| `retiredAt` | nullable `Date`, when the account was retired |
| `retireComments` | nullable `String`, reason for retiring/disabling the account |

One `ClientAccount` per `Person`, enforced at creation time (see §3 —
uniqueness is per-person, **not** per-phone/email, because phone numbers are
routinely shared across a household).

New field on `Institution`: `defaultInstitution` (boolean) — used by the
landing page (§6) to pick which institution's details to show when a
deployment has more than one `Institution` row. Falls back to the
lowest-ID institution if none is flagged.

### 2. Four registration channels

1. **Staff-assisted**: An existing staff `WebUser` opens a new admin screen,
   looks up the patient, and creates the `ClientAccount` directly on the
   patient's behalf. No OTP — identity is verified by the staff member
   in person/by phone. Gated by a new privilege (e.g. "Create Client Portal
   Account").
2. **Self-service phone-OTP**: Client enters a phone number. It **must
   already match** an existing patient's phone (`Patient.patientPhoneNumber`
   / `Person.mobile`) — this channel is for existing patients getting portal
   access, not for creating new patient records remotely. OTP sent via SMS
   (extending the `PatientPortalController` mechanism with a new
   `MessageType`, e.g. `ClientPortalRegistrationOTP`, so it doesn't collide
   with the existing payment-OTP timeout/config). No match at all → rejected,
   with a message pointing to the kiosk or staff-assisted channel.
3. **Self-service email-OTP**: Same shape as phone, but by email — requires
   new email-OTP sending (via `EmailManagerEjb`).
4. **Kiosk**: IP-restricted (config-driven allowlist, see §5). Still
   requires OTP to whatever phone/email is entered, but — unlike channels
   2/3 — that contact does **not** need to already exist in the system. If
   it matches existing patients, show the multi-match list (§3) to pick
   from; if it matches nothing, the kiosk flow may create a **brand-new**
   `Patient` + `Person` + `ClientAccount` from scratch. This is the one
   channel that can onboard someone with no prior hospital record at all.

### 3. Family/shared-contact disambiguation & duplicate detection

Reuses the `findPatients()` pattern from `PatientPortalController`: after
OTP success, query all `Patient` rows by that phone/email.

- **Zero matches** → per-channel rule above (reject for 2/3, allow
  new-patient creation for kiosk).
- **One match** → proceed directly to account creation for that person.
- **Multiple matches** (e.g. shared household/chief-householder phone) →
  show the list of matched patients/persons; the client picks who they are.

**Duplicate-account detection happens after a specific person is selected**,
never on the phone/email itself (since one phone can legitimately map to
several people, each of whom may or may not already have an account): if the
selected `Person` already has a `ClientAccount`, warn and block creating a
second one — offer login or password-reset instead.

**Concurrency and uniqueness note**: `ClientAccount.person` is intentionally
NOT enforced unique at the database level, because a DB unique constraint
would conflict with the soft-delete/retire-then-recreate pattern — a person
could legitimately end up with one retired and one active `ClientAccount`
over time. At DDL-generation time (a separately-deferred step), the
generated `PERSON_ID` column on the `CLIENTACCOUNT` table is expected to come
out as non-unique — that is correct, not a bug.

Running the "does this person already have a non-retired account" check
(`ClientAccountFacade.findByPerson`) inside the same transaction as the
create is **not by itself** sufficient to prevent two concurrent registration
attempts for the same person from both succeeding: under normal database
isolation levels, two concurrent transactions can each independently observe
"no active account" and then each insert one, since neither transaction's
uncommitted insert is visible to the other's read. Preventing this requires
an actual serialization point, not just transaction scoping. Whichever
follow-up plan implements account creation must take one of these two
concrete approaches: (a) lock the `Person` row for the duration of the
check-then-create (e.g. JPA `LockModeType.PESSIMISTIC_WRITE` /
`SELECT ... FOR UPDATE` on `Person` before calling `findByPerson`, so a
second concurrent transaction blocks until the first commits or rolls back),
or (b) add a partial/conditional uniqueness mechanism scoped to
non-retired rows only (MySQL has no native partial unique index, so this
would need a generated/computed column or an application-level distributed
lock keyed on the person id) with explicit conflict handling on insert
failure. Option (a) is the simpler default recommendation for the first
channel plan that implements creation.

### 4. Login & credentials

A password is set at the end of every registration channel (immediately, by
the client after OTP verification, or immediately by staff for the
assisted path). Subsequent logins are phone-or-email + password, same shape
as staff logins today. OTP is not used for routine login — only at
registration and, later, for password reset (re-verify the phone/email on
file for that `ClientAccount`).

### 5. Kiosk IP restriction

A new config key in `ConfigOptionApplicationController` (e.g. "Client
Portal - Kiosk Allowed IPs", comma-separated), checked the same way
`WebUser.isIpAllowed()` checks its per-user allowlist today. Config-level
rather than per-account because no `ClientAccount` exists yet when someone
is standing at the kiosk trying to register.

### 6. Minimal landing page

Post-login landing page shows: the (default) institution's name, address,
and contact details (using the new `Institution.defaultInstitution` flag),
a personalized welcome using the client's name, and clearly-labeled
"coming soon" placeholders for future features (bills, appointments,
reports) so the account isn't a dead end while those remain separate,
future issues.

## Explicitly out of scope

- Any post-login feature beyond the landing page (bills, appointments, lab
  reports, messaging, etc.) — future issues.
- Cross-institution accounts — one registration is sufficient per HMIS
  deployment/instance; each institution's app instance has its own
  independent client accounts, matching how `WebUser` already works today.
- Any change to the existing `WebUser` / `WebUserRole` / privilege system —
  client accounts are a fully separate authentication path by design.
