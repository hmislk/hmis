# Migrating Data from a Legacy EMR into HMIS — ETL Pattern

How to move a hospital's clinical history (master data, patients, visits,
diagnoses, findings, prescriptions) out of an old standalone EMR and into a live
HMIS instance. This distils the approach used for the first such migration
(a legacy MS-Access EMR, September 2026). The working code from that migration is
kept as an off-repo git bundle — ask the lead developer — and is the best starting
point for the next one.

> This is **not** about upgrading a stale HMIS database to `development` HEAD — for
> that see [Migrating a Stale Hospital DB](migrating-a-stale-hospital-to-development.md).

---

## 1. Ground rules

- **The ETL is a one-shot tool, not product code.** Keep it under `tmp/` on a local
  branch and **never push it to `hmislk/hmis`** (public repo). ETL workspaces
  accumulate things that must not be published: backup-archive passwords, patient
  rows in orphan/backup TSVs, per-hospital counts in reconciliation reports, and a
  multi-hundred-MB ledger database. When done, archive it with
  `git bundle create <outside-repo-path>/<name>.bundle <branch> ^<merge-base>`,
  verify the bundle by fetching it into a scratch repo, then delete the branch.
- **No credentials in the workspace.** Passwords come from environment variables;
  docs point at the external credentials file, never inline values.
- **Do a spike first.** Before designing loaders, answer questions about the source
  with small read-only scripts: which tables are live vs historical (the first
  migration found two date-split "eras" of visit tables, not active/archive),
  which rows are empty, primary-key stability, unit/code coverage, how drug names
  map to the existing HMIS catalogue, patient data quality, staff mapping. Write
  the answers and decisions in a `FINDINGS.md`.
- **Checkpoint before every full run.** Run each loader with `--dry-run`, then a
  real run with `--limit 5`, inspect the created rows in the HMIS UI/DB, *then*
  launch the full run.

## 2. Architecture

```
legacy DB reader ─► Loader (match → create) ─► HMIS REST API   (preferred)
                          │                 └► guarded direct SQL (only where no API)
                          ▼
                    SQLite ledger  (source_table, source_pk, target_type) → target_id
```

| Module | Responsibility |
|---|---|
| source reader | Read the legacy DB (e.g. `access-parser` for `.mdb`), skipping soft-deleted rows |
| `hmis_api` | GET/POST with the `Finance` API-key header, parses the `{status,data}` envelope and `already_exists`/409, dry-run, retry |
| `hmis_sql` | Direct-SQL fallback, heavily guarded (see §4) |
| `ledger` | SQLite map from source row → HMIS id; the single source of truth for "already migrated?" and for FK resolution |
| `loaders/base` | The shared flow; each loader implements only `match()` and `create()` |
| `runner` | CLI: `python -m etl.runner <loader> [--dry-run] [--limit N]`, writes a markdown report per run |

### The loader flow (match-first, idempotent)

For every source row:

1. **Ledger hit** → `skipped_already` (near-zero cost, so re-runs are always safe).
2. **`match()`** against existing HMIS data (exact name / natural key) → record in
   ledger as `api_matched` / `sql_matched`. Many master-data rows (drugs,
   investigations) already exist in HMIS — never create a duplicate of them.
3. Otherwise **`create()`** → record the new id in the ledger.
4. Any exception → `failed` with the source PK and error in the run report; the run
   continues.

Rules that came out of the first migration:

- **Resolve legacy IDs through the ledger only.** Stamp created rows with a
  human-readable marker (e.g. `code = "EMA3:<table>:<pk>"`) for support
  traceability, but *matched* rows keep their original HMIS codes and carry no
  marker — so a marker scan misses them. FK resolution (old PatientID → new
  `Patient.id`) must go through the ledger.
- **Include the source table name in every key.** Two tables can share PK values.
- **Dedupe the source.** Even "unique" legacy PKs had a duplicate row.
- **Load in dependency order:** units/lookups/diagnoses/findings/investigations →
  drug catalogue → patients → encounters → encounter children (diagnoses,
  findings, prescriptions).

## 3. REST-API gotchas

- **Search pagination can create duplicates.** Most `GET` search endpoints default
  to a page size of 20. If an exact-name match sits beyond the first page, `match()`
  misses it and `create()` makes a duplicate. Page through results or use an
  exact-name query. The ledger only protects rows the ETL itself migrated.
- **FHIR endpoints need `Content-Type: application/fhir+json`** — plain
  `application/json` is rejected with 415.
- **Retry transient network errors patiently.** Unreliable links drop connections
  mid-run. Use a long backoff for transport errors (seconds up to ~5 minutes, ~20
  minutes total) so a blip pauses the run instead of failing thousands of rows, and
  a short separate retry for HTTP 5xx.
- **Known API gaps when this was written** — check their current state before
  relying on the SQL fallback:
  - #23463 MeasurementUnit create returns 501
  - #23464 Category / dose-form create returns 501
  - #23465 no ingest endpoints for PatientEncounter, ClinicalFindingValue, Prescription
  - #23466 `POST /api/investigations` returns 200 with no `id` (fall back to `match()` after create)
  - #23496 `POST /api/fhir/Patient` does not persist custom `identifier` entries (use the ledger, not the identifier, for idempotency)
  - #23494 (no duplicate-by-name check on pharmaceutical item create) and #23495
    (FHIR Patient ignored `active:false`) were fixed after the first migration.

## 4. Direct-SQL fallback — only where no API exists

Policy: **if a create API is missing or a stub, write direct SQL and file a GitHub
issue requesting the API**, so a later migration can switch to it.

Guardrails, all mandatory:

- Connect **only** through an operator-opened SSH tunnel to `127.0.0.1:<port>`; the
  ETL never opens the tunnel itself.
- `verify_target()`: refuse to write unless `SELECT DATABASE()` equals the expected
  schema.
- Every write in an explicit transaction; whitelist table/column identifiers with a
  regex; parameterise all values.
- Read `information_schema.columns` to build inserts that match the live schema;
  set `DTYPE` correctly for single-table-inheritance entities (e.g. a
  MeasurementUnit is a `category` row).
- Tunnels drop (every 15–30 min on a poor link). Open the tunnel with
  `ServerAliveInterval=20 ServerAliveCountMax=10`, wrap it in an auto-reconnect loop,
  and have the SQL client retry connecting and reconnect after a lost connection.

## 5. Running large loads in parallel

Hundreds of thousands of rows at ~2–3 rows/s need parallelism. The pattern that
worked (8 workers):

- The supervisor copies the main ledger to one private SQLite file per worker
  (shared SQLite serialises writes; MySQL connections must not be shared across
  processes).
- Worker `ix` owns the source rows where `i % N == ix` and builds its **own** DB
  connection.
- After all workers exit, the supervisor merges worker ledgers back into the main
  ledger (`INSERT OR IGNORE`) and prints the main count.
- The run is resumable: re-run until the ledger count equals the source total.
  Already-done rows are skipped cheaply.

### Incidents to design against

1. **Double launch.** A second supervisor was started while the first was still
   recovering from a tunnel drop and inserted thousands of duplicate encounters the
   ledger didn't know about. Before relaunching, **confirm via the process list**
   that no instance is running, not just that a command reported failure.
2. **Killed mid-run.** A multi-hour run launched as a background task of an agent
   harness was killed along with its whole process tree. A few rows were inserted
   but never recorded in the ledger (the kill landed between the INSERT and the
   ledger write). Run long jobs in a detached shell/scheduled task, not as an agent
   background task, and split runs into chunks that fit comfortably in the host's
   lifetime.

**Reconcile after every run:** compare the live row count for the target table
against the ledger count plus any pre-existing rows. For a mismatch, compute the
exact orphan ID set (live IDs minus ledger ids), exclude rows that existed before the
migration, spot-check that the orphans really are duplicates, confirm nothing else
references them by FK, **back them up**, then delete — and record it in an incident
note. If a legacy source row can't be matched uniquely to an orphan, delete the
orphan and let an idempotent re-run insert it again, rather than guessing.

## 6. Checklist

- [ ] Spike done, `FINDINGS.md` written, decisions agreed with the developer
- [ ] API gaps identified; issues filed; SQL fallback guarded
- [ ] Every loader: unit tests, `--dry-run`, `--limit 5` real run, inspected
- [ ] Full runs launched one at a time; process list checked before any relaunch
- [ ] Reconciliation report per loader (live vs ledger vs source)
- [ ] Legacy-deleted/inactive status carried over correctly (verify, don't assume)
- [ ] Workspace archived as a git bundle outside the repo; branch deleted; nothing pushed
