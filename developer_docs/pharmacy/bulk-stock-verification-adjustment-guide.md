# Bulk Pharmacy Stock Verification via API

## When to use this

A client periodically does a full physical stock count of a pharmacy department and hands over a spreadsheet of item / batch / physical quantity. This spreadsheet needs to be reconciled against system stock at scale — hundreds to thousands of rows — which is impractical to do one-by-one through the UI. This guide documents the process, using the pharmacy adjustment and batch-creation REST APIs, that has now been run successfully for two departments across two client systems.

This is not a one-off script. Expect to repeat this exercise for other departments/clients. Re-read this guide each time rather than reconstructing the approach from memory — the failure modes below were each found the hard way.

## Prerequisites

- A `Finance`-header API key for the target environment (see `developer_docs/api/building-apis/rest-api-development-guide.md` for the auth pattern).
- The exact department name/ID, confirmed via `GET /api/pharmacy_adjustments/search/departments?query=...` — do not assume the name in the spreadsheet matches the system exactly (whitespace, "Pharmacy" suffixes, etc. can differ).
- The physical count spreadsheet, with at minimum: item name, batch number and/or expiry date, current system quantity (as of export), physical count.

## Relevant endpoints

| Endpoint | Purpose |
|---|---|
| `GET /pharmacy_adjustments/search/departments` | Resolve department name → ID |
| `GET /pharmacy_adjustments/search/stocks` | Resolve item+expiry → `stockId`, returns live `stockQty`, `dateOfExpire`, rates |
| `POST /pharmacy_adjustments/stock_quantity` | Set a stock record's quantity (the adjustment itself) |
| `GET /pharmacy_batches/amp/search` | Fuzzy/exact search for an item's AMP (product) record |
| `POST /pharmacy_batches/amp/search_or_create` | Find or create an AMP by name |
| `POST /pharmacy_batches/create` | Create a new `ItemBatch` + `Stock` (always starts at qty 0) for a genuinely new batch |

## The core rule: always resolve by (item, expiry), never by item or batch-number text alone

The first time this exercise was run, matching was done by (item name, batch-number text). This was wrong: a single item can have **multiple separate stock records sharing the same batch-number text** (e.g. two different GRN lines that happened to get the same batch string), and the script picked one of them and set it to the full physical count — leaving the other, untouched record's stale quantity still in the system. This required two separate follow-up correction passes after the fact to find and fix.

The fix: use **expiry date**, not batch-number text, as the join key. The search API returns `dateOfExpire` per stock record and supports exact-date filtering independent of `batchNo`. Batch-number text in a spreadsheet is frequently unreliable — it can be transcribed wrong, or just not match the system's stored string — while expiry date is far more likely to be entered/read correctly and is what the search API actually indexes cleanly.

Even with expiry as the key, **more than one live stock record can still share the same (item, expiry)** (again, separate GRN lines). When this happens:

1. If the spreadsheet has exactly as many rows for that (item, expiry) as there are live records, match each spreadsheet row to the live record whose **current quantity is closest to the spreadsheet's recorded "before" quantity** — this tolerates a small amount of stock movement between the count date and when the adjustment is actually run.
2. If that match is not unique (a true tie — same expiry *and* same live quantity across candidates), **do not guess**. Log it for manual review. Guessing was the original mistake; don't repeat it even at small scale.
3. If the spreadsheet has *more* rows for that (item, expiry) than there are live records, that's a genuine shortfall — also don't guess, flag for manual review.
4. If the spreadsheet has *fewer* rows than live records exist, that's fine — just don't touch the untouched extra live records.

## A blank/missing system quantity does not always mean "new"

Spreadsheet exports can be inconsistent about which zero-stock batches they include. Do not treat "spreadsheet shows no current quantity for this item" as proof the item has no live record at all — confirm via a live search first.

Separately, the spreadsheet's **expiry column itself can contain transcription errors** distinct from the batch-number issue above — e.g. the year or day is wrong. Before concluding a row is genuinely new stock (no existing record at that expiry), try a fallback: search the item's other live batches and see if one has a **quantity matching the spreadsheet's recorded "before" value exactly**. An exact quantity match at a *different* expiry than stated is strong evidence the expiry was mistyped, not that the batch is new. In one run, over two-thirds of the rows initially flagged as "genuinely new" turned out to be this — only creating a batch for the small remainder that had no quantity match anywhere.

## Creating genuinely new batches/items

For rows that really are new (no live record, no quantity match elsewhere):

1. Check for an existing AMP (product) by **exact name** first via `amp/search`. If found, reuse its id — do not create a duplicate product.
2. If no exact match, but a **fuzzy/similar name** is found, do not auto-create — flag for manual review. System item names very often carry a dosage-form suffix (e.g. "Tablet", "Capsules", "Tab") that a hand-written count sheet omits ("Brufen 400mg Tab" in the sheet vs. "Brufen 400mg Tablet" in the system). Verify by generic name and strength before treating it as the same product, then reuse the existing AMP id and just add a new batch — don't create a second AMP for the same drug.
3. Only call `amp/search_or_create` (or create a new AMP) when there is truly no name match at all.
4. `POST /pharmacy_batches/create` always creates the `Stock` row at quantity 0 — follow it with a normal `stock_quantity` adjustment to set the counted amount.
5. This endpoint is **not idempotent** — calling it twice creates two separate stock records. Any retry/resume logic must record "batch created" as its own durable checkpoint step *before* attempting the quantity adjustment, so a resume never re-creates it.

### Known API response bug (fixed)

`/pharmacy_batches/create` and `/pharmacy_batches/amp/search_or_create` used to omit `stockId`/`id` from their JSON response when the field was still null at serialization time (the JPA id hadn't been flushed yet), even though the underlying row was created correctly. This was fixed in `PharmacyBatchApiService` (flush before reading the generated id; `serializeNulls()` scoped to the batch-create response) — see GitHub issue #21814 / PR #21816. If working against an environment that predates that fix, treat a response with a missing id as "creation likely succeeded, re-resolve the id via a follow-up search by exact name/expiry" rather than an outright failure.

## Data-quality gaps in the source spreadsheet

Real count sheets are rarely complete. Agree these conventions with the requester up front, apply them consistently, and always flag the affected rows in the final report so they can be corrected at the source:

- **Missing expiry date**: use an agreed placeholder date (a clearly-artificial one, e.g. the end of the current calendar year) rather than guessing a real date. Flag it.
- **Missing or zero purchase price**: estimate as an agreed percentage of the retail/sale price (agree the percentage each time — it will not always be the same number). Do not silently rely on the system's own default markup unless that's explicitly what was agreed. Flag it.
- **Expiry given without a day component** (e.g. "YY/MM" only): agree whether to use the first or last day of the month. Last day is usually the safer default since batch numbering conventions in this system are generally end-of-month.

## Process pattern

- **Checkpoint every row's outcome to an append-only log** (one JSON line per completed step) as you go, not just at the end. This makes the run resumable after any interruption — on restart, skip any row already recorded as done, and re-fetch state for genuinely in-flight ones. For multi-step rows (create batch, *then* adjust quantity), checkpoint each step independently so a resume never repeats a non-idempotent step.
- **Smoke-test on a small, deliberately-curated set of rows first** — include at least one normal single match, one duplicate-expiry group, one genuinely-new-batch case, and one known edge case (e.g. missing expiry) — before running the full set. This is what catches response-shape bugs and matching-logic errors cheaply.
- **Rate-limit writes** and expect individual calls to occasionally be slow — a request that appears to time out client-side does not necessarily mean it failed server-side; verify via a follow-up read before assuming an error, rather than blindly retrying (a retry against an already-applied write is usually harmless for quantity-set operations, but always confirm rather than assume).
- If a specific write endpoint is found to hang unreliably against a given environment, and a local instance can be pointed at the same database, routing just that endpoint's calls through the local instance is a reasonable workaround while the root cause is investigated separately — it does not require redoing the rest of the exercise differently.
- **Reconcile at the end**: sum every row's target quantity from the spreadsheet and compare against the sum of system quantities actually applied. These must match exactly (zero tolerance) before considering the exercise done. Any row that couldn't be auto-resolved must be accounted for explicitly, not silently dropped from the total.

## Reporting to the client

The client does not need or want any of the above process detail. Produce two deliverables:

1. **Executive summary** (short, plain business language): purpose, headline counts (items reviewed / corrected / already-correct / new batches added), net value impact, the verification statement (system stock now equals the physical count, item by item), and a short list of anything flagged for the client to confirm (placeholder expiries, estimated prices).
2. **Detailed item-by-item report** (spreadsheet): a full comparison table (item, expiry, physical count, system quantity after, match yes/no) proving the reconciliation, plus a breakdown of corrections vs. new batches vs. new products, and the flagged-items list repeated for easy reference.

No API names, endpoint paths, scripts, or bug numbers belong in either document.
