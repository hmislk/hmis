# Two-factor login: server key, restore and clone safety

Developer and administrator note for the authenticator-app and SMS two-factor login (issues #25755-#25762).

## Server key (required before anyone sets up an authenticator app)

Authenticator secrets are stored encrypted (AES-256-GCM, stored value starts with `gcm1:`). The key is **not** in the database and **not** in the source. Set one of these on the application server, to a random string of at least 32 characters, and restart the application server:

- JVM system property `hmis.twofactor.secretkey` (for example `asadmin create-jvm-options "-Dhmis.twofactor.secretkey=<value>"`), or
- environment variable `HMIS_TWOFACTOR_SECRET_KEY` for the server process.

Rules:
- Generate the value on the server (for example `openssl rand -base64 48`); never write it in the repository, an issue, a PR or a chat.
- Keep a copy in the organisation's credentials store. **If the key is lost or changed, every stored secret becomes unreadable** and each linked user needs an admin Reset and a new set-up.
- Each server has its own key. A database copied to another server (a clone, a restore) cannot be read there; see the next section.
- At start-up, secrets saved by the older scheme are re-encrypted under the key automatically.

### What happens without the key (or with a different key)
- With the master switch on and no key: a user for whom a code is needed and cannot be checked is refused at login with "Two-factor login is not set up on this server. Ask the administrator." This is never counted as a failed code and never locks the account. Nobody is let in without the second step.
- Admin Reset, Unlock and the master switch keep working for anyone who can sign in (an admin who is not linked, or who is allowed SMS codes).
- Recovery: set the key and restart; or switch **Enable authenticator app two-factor login** off from Administration → Manage Institutions → Application Options; or run the clone step below on the database.

## Set-up period (24 hours)

A user who must set up (Required ticked, or "all users" on) has 24 hours from the moment an admin ticked Required, reset the authenticator, or a 2FA switch was turned on. After that the user sees a message and an admin must Reset (Manage User shows the end of the period and a "Reopen set-up" button). A user with no valid mobile number on the staff record cannot be set to Required, and the "all users" switch skips such a user and lists them when saved. Set-up always starts with an SMS code to the staff-record mobile.

## Clone safety: one SQL step after restoring or copying a database

After restoring a production dump anywhere that is not that production server (local, QA, a copy), run this once, then **restart the application** (raw SQL on `CONFIGOPTION` is not seen by the cached options until restart):

```sql
UPDATE CONFIGOPTION SET OPTIONVALUE = 'false'
 WHERE OPTIONKEY IN ('Enable authenticator app two-factor login',
                     'Require authenticator app two-factor login for all users',
                     'Allow SMS code as an alternative to the authenticator app at login');
UPDATE WEBUSER SET TWOFACTORENROLLED = 0, TWOFACTORSECRET = NULL, TWOFACTORENROLLEDAT = NULL,
       TWOFACTORLASTUSEDSTEP = NULL, TWOFACTORFAILEDATTEMPTS = 0, TWOFACTORLOCKED = 0,
       TWOFACTORLOCKEDAT = NULL, TWOFACTORMOBILE = NULL, TWOFACTORMOBILEVERIFIEDAT = NULL,
       TWOFACTORSETUPWINDOWSTARTEDAT = NULL;
DELETE FROM OTPCODE;
```

Table names are upper case on Linux (`lower_case_table_names=0`); on a case-folded server use the lower-case names. The step keeps each user's Required and Allow-SMS flags. It removes secrets and verified mobile numbers, which are useless (and personal data) on a copy.

## Migration

`v2.23.0` creates the `OTPCODE` table when it is missing and adds `WEBUSER.TWOFACTORSETUPWINDOWSTARTEDAT`. Run it before deploying a build that contains this change.

## Other rules in this change
- Lab analysers, middleware and the channelling API log in with a name and password only. They are refused for a user who is linked, flagged Required or allowed SMS codes; the "all users" switch does not apply to them. Use a dedicated, never-flagged account for each.
- The testing grant (`grantAllPrivilegesToAllUsersForTesting`) never works while the master switch is on.
- A login that has passed the password step and waits for a code is dropped after 10 minutes.
