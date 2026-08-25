# Client Portal Registration — Handover (2026-07-24)

**Master issue**: [hmislk/hmis#22359](https://github.com/hmislk/hmis/issues/22359)
**Design spec**: [`docs/superpowers/specs/2026-07-23-client-portal-registration-design.md`](../specs/2026-07-23-client-portal-registration-design.md)
**Wiki**: [Client Portal Registration](https://github.com/hmislk/hmis/wiki/Client-Portal-Registration)

## Status at handover time

| Phase | Issue | Status |
|---|---|---|
| Foundation (entity, facade, enums, pure utilities) | [#398](https://github.com/hmislk/hmis/issues/398) | ✅ Merged — PR [#22358](https://github.com/hmislk/hmis/pull/22358) |
| Self-service phone-OTP registration | [#22367](https://github.com/hmislk/hmis/issues/22367) | ✅ Merged — PR [#22372](https://github.com/hmislk/hmis/pull/22372) |
| Staff-assisted registration | [#22363](https://github.com/hmislk/hmis/issues/22363) | ⬜ Not started — **recommended next** |
| Self-service email-OTP registration | [#22368](https://github.com/hmislk/hmis/issues/22368) | ⬜ Not started (parallel-safe alternative) |
| Kiosk registration | [#22369](https://github.com/hmislk/hmis/issues/22369) | ⬜ Not started (parallel-safe alternative) |
| Login and password reset | [#22370](https://github.com/hmislk/hmis/issues/22370) | ⬜ Not started — needs ≥1 channel done (phone-OTP now satisfies this) |
| Landing page, `Institution.defaultInstitution`, deferred DDL regen | [#22371](https://github.com/hmislk/hmis/issues/22371) | ⬜ Not started — deliberately last (owns the one-time DDL regen) |

Per the master issue's own sequencing note: #22363, #22368, and #22369 are
all unblocked now and can be done in any order or in parallel — none
depend on each other, only on the Foundation PR (already merged). This
handover recommends **#22363 (staff-assisted)** next, but the completing
session should feel free to pick a different one if there's a reason to
(e.g. a more urgent stakeholder need for kiosk or email).

## Reusable pieces from the phone-OTP implementation

Don't re-derive these — read the code directly before starting:

- **`ClientAccount`** (`src/main/java/com/divudi/core/entity/ClientAccount.java`) — `@ManyToOne` to `Person` (intentionally not `@OneToOne`; see the class comment on why).
- **`ClientAccountFacade.createIfNoActiveAccount(personId, newAccount)`** (`src/main/java/com/divudi/core/facade/ClientAccountFacade.java`) — locks the `Person` row (`PESSIMISTIC_WRITE`) for the check-then-create. **Every channel that creates an account must call this**, not `create()` directly, or the concurrency guarantee from the design spec's §3 is silently lost.
- **`ClientPortalMatcher.classify(List<Patient>)`** (`core/util`) — pure 0/1/many match classifier. Reuse directly for email-OTP and kiosk matching.
- **`ClientPortalOtpGenerator.generate(int length)`** (`core/util`) — pure OTP generator. Reuse for email-OTP and kiosk OTP too.
- **`ClientPortalIpAllowlist.isAllowed(requestIp, allowedIpsCsv)`** (`core/util`) — already built in the Foundation PR for the kiosk channel's IP restriction; not yet consumed by any controller.
- **`ClientPortalPhoneRegistrationController`** (`src/main/java/com/divudi/bean/clientportal/ClientPortalPhoneRegistrationController.java`) — the OTP-send/verify/match/register controller for phone. Use as the structural template for the email-OTP and kiosk controllers (same package `com.divudi.bean.clientportal`); the staff-assisted controller will look different (no OTP step, but still ends with the same `createIfNoActiveAccount` call and privilege-gating instead).
- **`client_portal/register_phone.xhtml`** — standalone public page (own `<h:head>`/`<h:body>`, no internal template), matching the pre-existing `patient_portal/portal_login.xhtml` CSS/structural pattern. Use as the template for new `client_portal/*.xhtml` pages. Staff-assisted registration will instead be an **internal** admin screen using the normal app template — don't copy this standalone pattern for it.
- **`MessageType.ClientPortalRegistrationOTP`** — reuse for email-OTP and kiosk OTP too (or add a new distinct `MessageType` if there's a reason email/kiosk OTP traffic needs to be told apart from phone OTP — not required by the design spec, judgment call for whoever builds it).

## Known gotchas hit while building phone-OTP (avoid re-discovering these)

1. **`p:commandButton update=` inside a `p:dataTable` row can't resolve sibling ids outside the table** — `p:dataTable` is a JSF `NamingContainer`; a relative `update="someId"` from inside a row throws a server-side `ComponentNotFoundException` and leaves the button silently stuck disabled client-side with **no browser console error**. Fix: absolute ids (`update=":form:someId"`) or `update="@form"`. Already documented in `developer_docs/testing/playwright-e2e-workflow.md` §42 — read it before building the kiosk/multi-match-reusing pages.
2. **Local dev has no SMS gateway configured** — `SmsManagerEjb.sendSms()` always returns `false` locally. Don't gate the flow on that return value the way a naive first draft would; `sendOtp()` should still advance to the verify step regardless of delivery status (see the comment in `ClientPortalPhoneRegistrationController.sendOtp()` for the exact reasoning — it mirrors `PatientPortalController`'s existing behavior).
3. **`CLIENTACCOUNT` table doesn't exist in the shared/CI schema** — DDL regeneration is deliberately deferred to #22371 (one shared `generate-ddl` pass after all 6 channels land). For local Playwright/DB testing, hand-create the table locally only (not committed) — see the phone-OTP PR discussion for the exact `CREATE TABLE` statement used.
4. **`persistence_for_local_testing.xml`'s JNDI names are wrong for this project** (they're actually for a sibling `ruhunu` project). For local `rh` testing the correct local JNDI names are `jdbc/coop` (main) and `jdbc/rhAuditDS` (audit) — confirmed against the actual running Payara pools, not the reference file. The `start-issue` skill currently reads the wrong file for this; don't trust it blindly for this project.

## Review outcome to be aware of

CodeRabbit flagged OTP hardening (attempt-limiting, cooldowns, atomic
consumption) on PR #22372. Two cheap fixes were applied (OTP length floor
raised to 4 digits; expiry checked against persisted `Sms.createdAt`
instead of view-scoped state). The larger hardening (rate-limiting,
cooldowns, atomic one-time consumption) was deliberately deferred — it
needs new persisted state on `Sms` (a schema change) and is a gap shared
identically by the pre-existing `PatientPortalController`, not something
introduced by this PR. **No GitHub issue has been filed for this yet** —
if the next session has time/mandate, consider filing one, but it's not a
blocker for any of the remaining channel issues.

## Handover prompt (paste into a new Claude Code session)

```
Continue the Client Portal Registration feature (master issue #22359 in hmislk/hmis).

Read docs/superpowers/handover/2026-07-24-client-portal-registration-handover.md
first for full context: what's done, what's reusable, and gotchas already hit.

Then take issue #22363 (staff-assisted registration) through its full lifecycle
end-to-end using the dev-issue skill: investigate, discuss the approach with me,
implement, rebuild + local redeploy, verify with Playwright + DB, iterate until
passing, commit/push, open a PR targeting development, and loop on review
comments until mergeable. Do not merge — that's my call.

If you think a different unblocked issue (#22368 email-OTP or #22369 kiosk)
makes more sense to do first, tell me why before starting either one.

Once the PR is mergeable:
1. Update the Client Portal Registration wiki page
   (../hmis.wiki/Client-Portal-Registration.md) with this channel's status,
   clean screenshots (no real patient data), relevant config keys, and any
   new troubleshooting notes — following the same structure already used
   for the phone-OTP section.
2. Write a new handover doc at
   docs/superpowers/handover/<today's-date>-client-portal-registration-handover.md,
   following the same structure as this one, covering whichever child issue
   should reasonably come next (check the master issue's sequencing note
   and update it if anything about the remaining order has changed).
```

## Notes for whoever picks this up

- Follow this repo's `CLAUDE.md` rules throughout (branch from
  `origin/development`, restore local JNDI after every push, no native
  SQL unless justified, discuss uncertainties rather than guessing).
- The `start-issue` and `dev-issue` skills cover the full workflow —
  use them rather than improvising the branch/PR/review mechanics.
- If patient data shows up in any screenshot destined for the wiki or a
  GitHub issue/PR comment, discard or replace it before publishing — this
  came up during the phone-OTP work (multi-match picker and the
  registration-complete screen both show real patient names from the
  local dev DB).
