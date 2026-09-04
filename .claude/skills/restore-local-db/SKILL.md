---
name: restore-local-db
description: >
  Refresh a LOCAL MySQL database from a PRODUCTION dump, safely. Use when asked to
  "restore <db> locally from prod", "get a fresh copy of coop/ruhunu for testing",
  "load this dump into my local database", or "back up prod and restore it locally".
  Two entry points: (1) name one or more databases and the skill dumps them from the
  Azure VM, downloads, and restores; (2) point at an existing dump file and the skill
  only restores. Works for ANY database, not just coop/ruhunu. Hard guardrails prevent
  ever writing to the Azure production databases: it refuses to run while any SSH
  tunnel is open and asserts the MySQL target is the local instance before every
  destructive step.
allowed-tools: Bash
user-invocable: true
---

# Restore a Local Database from a Production Dump

Bring a **local** MySQL database up to date with a **production** copy, without
any risk of touching the live Azure production databases.

The destructive step is `DROP DATABASE`. Every guardrail exists to guarantee
that when it runs, it runs against **local** MySQL and nothing else.

## Inputs

Parse the user's request into:

| Input | Meaning | Default |
|---|---|---|
| `<db>` (one or more) | Database name(s) to restore locally, e.g. `coop`, `ruhunu`, `mp` | required |
| `--dump <path>` | An already-downloaded dump file (`.sql` or `.sql.gz`). When given, **skip acquisition** — go straight to Phase 0. One `--dump` per `<db>`. | none |
| `--skip-pre-backup` | Skip the local rollback dump in Phase 2. Announce this loudly. | off (backup is taken) |
| `--bill-threshold N` | Sanity-gate row limit | `100` |
| `--bill-days D` | Sanity-gate look-back window in days | `5` |

If the user pastes a dump path in prose ("restore local coop from
`<BACKUP_DIR>\coop_x.sql.gz`"), treat it as `--dump`.

## Credentials file

All passwords are read at runtime from the machine's credentials file — never
hardcode them in this skill. Location is platform-specific (per the
`database-guide` skill):

- Windows: `C:\Credentials\Credentials.txt`
- Linux / macOS: `~/.config/hmis/credentials.txt`

Resolve it once at the start: use the Windows path if it exists, else the
`$HOME/.config/hmis/credentials.txt` path, else **stop and ask the user** where
their credentials file is. Referenced below as `<CREDS>`.

Downloaded dumps and rollback backups go under a backup directory —
`C:\Backups` on the Windows dev laptop (`C:\Backups\pre-restore` for rollback
copies). Adjust for the platform if the skill ever runs elsewhere. Referenced
below as `<BACKUP_DIR>`. Temp files (`.err` logs etc.) go in the session
scratchpad directory, referenced as `<SCRATCH>`.

Section names and the exact set of DB servers in that file change over time —
read the file and match on the section headings and the "Databases :" lines,
don't assume a fixed layout.

## Path selection

```
user named DB(s) only            -> Acquisition sub-procedure, then Phase 0..3 per DB
user gave --dump <path>          -> Phase 0..3 per DB (no acquisition)
```

**Phase 0 runs in BOTH paths.** A provided dump does not exempt the run from the
no-SSH-tunnel assertion — the `DROP DATABASE` is just as destructive either way.

## Keep the machine awake for the whole run

A large import takes many minutes and **the machine going to sleep mid-import
kills it**, leaving the target DB half-populated. Before starting, disable sleep:

```bash
powercfg //change standby-timeout-ac 0
powercfg //change standby-timeout-dc 0
powercfg //change hibernate-timeout-ac 0
powercfg //change hibernate-timeout-dc 0
```

If the laptop lid may be closed, also set the lid action to "do nothing"
(`powercfg //setacvalueindex SCHEME_CURRENT SUB_BUTTONS LIDACTION 0` +
`//setdcvalueindex ... 0` + `//setactive SCHEME_CURRENT`). Restore the previous
power scheme (`powercfg //setactive SCHEME_BALANCED`, or re-apply the saved
timeouts) in Phase 3 once every DB is done.

Run the import itself as a **detached process** (a hidden `powershell.exe
Start-Process`, or `nohup` under a persistent shell) that survives the
controlling session being interrupted, and have it write progress to a status
file you poll.

---

## Acquisition sub-procedure (only when no `--dump` given)

Runs **before** Phase 0. It opens an SSH tunnel to Azure, dumps on the VM,
downloads the compressed file, then **kills the tunnel**. The restore never
coexists with a tunnel.

### A1 — Resolve each DB to its Azure DB server

Read `<CREDS>`. In the
`NEW - MySQL Flexible Servers` section it lists, per server:

```
--- db3 --- 10.30.2.6 ---
Databases : coop, rhdrawer (+ audits)
Username  : hmis_admin
Password  : <complex>
```

and the SSH host aliases in the `NEW - SSH access` section
(`new-shared1..4`, each forwarding local ports `4305=db1 4306=db2 4307=db3
4308=db4`). Any `new-shared*` alias forwards all four DB ports, so you can
tunnel through a single host for several DBs.

Build, for each `<db>`:
- `DB_PRIVATE_IP` (e.g. `10.30.2.6`)
- `DB_LOCAL_PORT` (`4305`..`4308` matching db1..db4)
- `DB_ADMIN_PASSWORD` (the complex Azure password for that server)
- an SSH host alias to tunnel through (any `new-shared*`; prefer the one whose
  "Clients" list names this DB)

If a DB is not listed in the credentials file, **stop and ask the user** for the
DB server's private IP, the matching local port, and which SSH alias to use.

### A2 — Open ONE tunnel

```bash
/c/Windows/System32/OpenSSH/ssh.exe -F ~/.ssh/config -N -f <ssh-alias>
```

Verify the needed port(s) answer:

```bash
mysql -h 127.0.0.1 -P <DB_LOCAL_PORT> -u hmis_admin -p'<DB_ADMIN_PASSWORD>' -e "SELECT 1;"
```

Only one `new-shared*` tunnel at a time — the 4305-4308 ports are identical
across every alias.

### A3 — Dump on the VM, gzip, download

Do the dump **on the VM** against the DB's private IP. Dumping through the
tunnel from the laptop is far slower — an 8 GB DB did not finish in 2 minutes
that way, versus ~90 seconds on the VM.

Write a `--defaults-extra-file` cnf locally, `scp` it to `/var/tmp` on the VM,
`chmod 600`, then run mysqldump on the VM. **Quote the password in the cnf**
(`password="..."`) — the Azure admin passwords contain `#`, `?`, `=`, `+`, and
an unquoted value silently truncates at the first special char and fails with
`Access denied`. Then:

```bash
# on the VM, detached:
nohup sh -c 'nice -n 10 mysqldump --defaults-extra-file=/var/tmp/<db>.cnf \
  --single-transaction --quick --routines --triggers --events \
  --set-gtid-purged=OFF --no-tablespaces <db> 2>/var/tmp/<db>.err \
  | gzip > /var/tmp/<db>_prod_<ts>.sql.gz; echo $? > /var/tmp/<db>.done' >/dev/null 2>&1 &
```

Poll `/var/tmp/<db>.done`. When it reads `0` and `<db>.err` is empty:
`gzip -t` the file on the VM, check its tail decompresses to
`-- Dump completed on ...`, then `scp` it to
`<BACKUP_DIR>\<db>_prod_<ts>.sql.gz`. Confirm the downloaded byte size matches
the VM's exactly.

### A4 — Tear the tunnel down

```bash
taskkill //F //IM ssh.exe
```

Then clean up the VM: `rm -f /var/tmp/<db>.cnf /var/tmp/<db>_prod_<ts>.sql.gz
/var/tmp/<db>.err /var/tmp/<db>.done`. Delete the local cnf too — it holds the
Azure admin password.

The downloaded `<BACKUP_DIR>\<db>_prod_<ts>.sql.gz` is now the `--dump` for that DB.

---

## Phase 0 — Safety preconditions (ABORT on any failure)

Run this immediately before touching local MySQL, in **every** path.

### 0.1 — No SSH tunnels

```bash
taskkill //F //IM ssh.exe 2>&1 || true          # ssh-agent service is fine, leave it
# assert zero ssh.exe (excluding the ssh-agent Windows service):
tasklist | grep -i "ssh.exe" | grep -v "ssh-agent"          # must produce NOTHING
# assert no forwarded DB / Payara-admin ports listening (the ~/.ssh/config
# new-shared* hosts forward 4305-4308 for the four DBs plus a 3x848 admin port):
netstat -ano | grep LISTENING | grep -E ":(430[5-9]|3[0-9]848)\b"   # must produce NOTHING
```

If any `ssh.exe` remains or any forwarded port is listening -> **ABORT**, tell
the user to close their tunnels first. (On the acquisition path this check runs
*after* A4 has already killed the tunnel this skill opened; a surviving tunnel
here means one the user opened separately.)

### 0.2 — MySQL target is the LOCAL instance

Get the local simple password from `<CREDS>`
(`LOCAL DEVELOPMENT - MySQL` section). Then:

```bash
mysql --host=127.0.0.1 --port=3306 --protocol=TCP -uroot -p<simple> -N \
  -e "SELECT CONCAT(@@hostname,'|',@@version,'|',@@port,'|',@@datadir);"
```

Assert ALL of:
- `@@version` does **NOT** contain `azure` (Azure servers report `8.0.46-azure`)
- `@@port` = `3306`
- `@@hostname` is this machine (not a `db*`/Azure host)
- `@@datadir` is a local Windows path

Any mismatch -> **ABORT**.

### 0.3 — Command self-scan

Before running any `DROP` / import / `mysqldump` command, grep the command
string for forbidden tokens and abort on a match:

```
10.30.    20.24.    40.83.    57.158.    carecode.org
```

plus any Azure admin-password fragment you loaded in A1. Every restore command
must use `--host=127.0.0.1 --port=3306 --protocol=TCP` with the **simple local**
password only. A complex password anywhere in a restore command = wrong target.

---

## Phase 1 — Sanity gate (per DB)

Confirm the local DB is stale test data, not something real:

```bash
mysql --host=127.0.0.1 --port=3306 --protocol=TCP -uroot -p<simple> <db> -N \
  -e "SELECT COUNT(*) FROM bill WHERE createdAt >= NOW() - INTERVAL <D> DAY;"
```

- result `< <N>` -> **PASS**, safe to drop & restore.
- result `>= <N>` -> **STOP this DB**. Do not drop. Report the count and the
  latest `MAX(createdAt)` and ask the user to investigate.
- no `bill` table -> fall back to `COUNT(*)` of the largest table and require
  explicit user confirmation before proceeding.

Also print, for the report: `SELECT COUNT(*) FROM bill;` and
`SELECT MAX(createdAt) FROM bill;` (the "before" numbers).

---

## Phase 2 — Restore (per DB, only if Phase 1 passed)

### 2.1 — Validate the dump file

```bash
gzip -t <dump>                                        # integrity (skip for plain .sql)
gzip -dc <dump> | head -40 | grep -E "^-- Host:|Database:"       # identity
gzip -dc <dump> | grep -c -E "^(CREATE DATABASE|USE )" || true   # MUST print 0
gzip -dc <dump> | grep -c "^CREATE TABLE "                       # expected table count
gzip -dc <dump> | tail -3                                        # MUST end "-- Dump completed on ..."
```

- `-- Host:` / `Database:` should name the expected prod server and DB.
- The `CREATE DATABASE` / `USE` count must be `0` — if not, **ABORT**: those
  statements override the target DB and defeat the `mysql <db>` argument safety.
- Record the `CREATE TABLE` count — Phase 2.5 compares the restored DB's table
  count against it to detect a truncated import.
- If the dump does **not** end with `-- Dump completed on ...`, it was itself
  truncated (interrupted `mysqldump`, sleep, disk full) — **ABORT**, re-acquire.
- `grep -c` on a `gzip -dc` pipe may report a broken pipe / non-zero exit
  because `head`/`-m` closed the reader early; that is harmless, judge on the
  printed count.

### 2.2 — Pre-restore rollback backup (unless `--skip-pre-backup`)

```bash
mkdir -p <BACKUP_DIR>/pre-restore
mysqldump --host=127.0.0.1 --port=3306 --protocol=TCP -uroot -p<simple> \
  --single-transaction --quick --routines --triggers --events --no-tablespaces \
  <db> 2><BACKUP_DIR>/pre-restore/<db>_local_before_<ts>.err \
  | gzip > <BACKUP_DIR>/pre-restore/<db>_local_before_<ts>.sql.gz
gzip -t <BACKUP_DIR>/pre-restore/<db>_local_before_<ts>.sql.gz    # verify
```

Large DBs take minutes — run it in the background and wait. Keep this file until
the user confirms the restored DB works in the app.

If `--skip-pre-backup`: print a clear warning that there is **no rollback copy**
and continue.

### 2.3 — Drop & recreate (LOCAL)

Re-run Phase 0.1 + 0.2 + 0.3 one more time, then:

```bash
mysql --host=127.0.0.1 --port=3306 --protocol=TCP -uroot -p<simple> \
  -e "DROP DATABASE IF EXISTS <db>; CREATE DATABASE <db> CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
```

If `CREATE DATABASE` hangs on "Waiting for schema metadata lock", a stale local
Payara connection pool is holding the old schema. Options:

- easiest: **stop local Payara before Phase 2.3** (`asadmin stop-domain`) so
  nothing holds the schema, restart it in Phase 3.
- or wait — the lock clears on its own once the import starts inserting.

Do **not** issue a second `CREATE DATABASE` while the first is waiting — it
queues behind the same metadata lock and looks stuck. Do not kill the import to
"fix" a stalled `CREATE`.

### 2.4 — Import

```bash
gzip -dc <dump> | mysql --host=127.0.0.1 --port=3306 --protocol=TCP \
  -uroot -p<simple> <db> 2><SCRATCH>/<db>_restore.err
```

Run this **detached** (see "Keep the machine awake") for anything over ~1 GB
uncompressed — an interrupted import leaves the DB half-loaded. `mysql` reports
only the first error; check the `.err` file (the "Using a password" line is a
warning, not an error).

**If the import is interrupted** (sleep, killed shell, power loss): the target
DB is now partial. Recovery is simply to **redo Phase 2.3 + 2.4** — `DROP` the
half-loaded DB and re-import from the same dump. The dump is a full snapshot, so
replaying it from an empty DB is always correct; there is no "resume". The
Phase 2.2 rollback copy and the prod dump are both untouched by a failed import.

### 2.5 — Verify

```bash
mysql --host=127.0.0.1 --port=3306 --protocol=TCP -uroot -p<simple> <db> -e "
  SELECT COUNT(*) AS total_bills FROM bill;
  SELECT MAX(createdAt) AS latest_bill FROM bill;
  SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema='<db>';"
```

- `table_count` must equal the `CREATE TABLE` count recorded in Phase 2.1. If it
  is short, the import was truncated — redo Phase 2.3 + 2.4.
- `total_bills` should be production-scale and `latest_bill` recent (close to
  when the dump was taken). A tiny count means the `bill` table did not finish.
- The import `.err` file must contain nothing but the password warning.

### 2.6 — Table-name case

```bash
mysql --host=127.0.0.1 --port=3306 --protocol=TCP -uroot -p<simple> \
  -e "SHOW VARIABLES LIKE 'lower_case_table_names';"
```

- value `1` (typical on the Windows dev laptop) -> names are already
  case-folded, **nothing to do**.
- value `0` **and** the dump's `CREATE TABLE` names are lowercase **and** the app
  expects uppercase -> generate and run the rename script:

```sql
SELECT CONCAT('RENAME TABLE `', table_name, '` TO `tmp_', table_name, '`; ',
              'RENAME TABLE `tmp_', table_name, '` TO `', UPPER(table_name), '`;')
FROM information_schema.tables
WHERE table_schema='<db>' AND table_name <> UPPER(table_name);
```

Pipe its output back into `mysql <db>`.

---

## Phase 3 — Report

One block per DB:

```
<db>
  restored from : <dump path>   (source: <-- Host line> / <Database line>)
  bills before  : <n>   (latest <date>)
  bills after   : <n>   (latest <date>)
  tables        : <n>
  rollback copy : <BACKUP_DIR>\pre-restore\<db>_local_before_<ts>.sql.gz   [or: SKIPPED]
```

Then:
- Restore the previous power scheme / sleep timeouts disabled in "Keep the
  machine awake".
- Restart local Payara (`asadmin restart-domain`) so its connection pool
  reconnects to the fresh schema.
- Tell the user to confirm the app works against the restored DB **before**
  deleting the Phase 2.2 rollback copies.

---

## Guardrail summary (why each exists)

| Guardrail | Prevents |
|---|---|
| Kill + assert no `ssh.exe`, no tunnel ports (Phase 0.1, every path) | A restore command resolving through a live tunnel to an Azure DB |
| `@@version` not `*-azure`, `@@port` 3306, local `@@datadir` (0.2) | Being connected to a production server at all |
| Command self-scan for Azure IPs / complex passwords (0.3) | A copy-paste of a prod host/credential into a destructive command |
| Acquisition tunnel killed **before** Phase 0 (A4) | The restore ever running while a tunnel is open |
| Sanity gate on recent local bill count (Phase 1) | Dropping a local DB that actually holds real/unsaved work |
| Dump must have no `CREATE DATABASE` / `USE` (2.1) | The dump redirecting the write to a different DB than the `mysql <db>` arg |
| Pre-restore rollback dump (2.2) | An unrecoverable mistake |
