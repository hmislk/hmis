# Two-factor login: server key, restore and clone safety

Developer and administrator note for the authenticator-app and SMS two-factor login (issues #25755-#25762).

## Server key (required before anyone sets up an authenticator app)

Authenticator secrets are stored encrypted (AES-256-GCM; stored value starts with `gcm2:`, and the user id is authenticated data so a value cannot be moved to another user). The key is **not** in the database and **not** in the source. Give the application server a random key of at least 32 characters (at least 16 different characters), then restart it. Generate it on the server:

```bash
openssl rand -base64 48
```

Preferred ways, in the order the server reads them:
1. **Key file** (recommended): write the key as the first line of a file readable only by the server user (`chmod 600`), outside the application and outside the repository, and point to it with the environment variable `HMIS_TWOFACTOR_SECRET_KEY_FILE` (or the JVM property `hmis.twofactor.secretkeyfile`).
2. Environment variable `HMIS_TWOFACTOR_SECRET_KEY` for the server process.
3. JVM property `hmis.twofactor.secretkey`: last resort. A `-D` option is visible in the process list and is stored in the domain configuration file, so avoid it on a shared host.

Rules:
- Never write the value in the repository, an issue, a PR or a chat. Keep a copy in the organisation's credentials store. **If the key is lost or changed, every stored secret becomes unreadable** and each linked user needs an admin Reset and a new set-up.
- A weaker value (shorter, or few different characters) is logged and treated as no key.
- Each server has its own key. A database copied to another server (a clone, a restore) cannot be read there; see the clone step below.
- At start-up, secrets saved by the older scheme (and `gcm1` values) are re-encrypted as `gcm2`. Once a key is set, an older-scheme value that start-up did not re-encrypt is treated as unreadable (message, then admin Reset).

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

## Deploying and rolling back
1. Run migration `v2.23.0` **before** deploying the new build.
2. Deploy the new build with the master switch **off**, set the key, restart, check the start-up log line about re-encrypted secrets, then switch on.
3. **Rollback:** the previous build cannot read `gcm` secrets. First switch **Enable authenticator app two-factor login** off in the admin UI (Administration → Manage Institutions → Application Options), then deploy the old build. Do not set the key on an older build, only on the final one. Users linked before the rollback need an admin Reset when the final build returns.
