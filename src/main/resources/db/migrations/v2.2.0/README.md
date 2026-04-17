# Migration v2.2.0 — Switch to AUTO_INCREMENT (IDENTITY strategy)

## Summary
Replaces the shared `SEQUENCE` table generator with MySQL's native `AUTO_INCREMENT` per table.

## Why
All entity inserts shared one `SEQ_GEN` row. Under concurrent load, multiple threads competing for the same row lock caused `MySQL Error 1205: Lock wait timeout exceeded`, resulting in a production incident at COOP on 2026-04-11 where a pharmacy stock receive transaction silently rolled back.

## Requirements
- **Payara must be STOPPED** before running
- Full database backup required
- Deploy new app build (with `GenerationType.IDENTITY`) immediately after

## Backward Compatibility
MySQL sets `AUTO_INCREMENT = MAX(ID) + 1` for each table automatically — no existing IDs change, no data loss.

## Hotfix Application (COOP production)
Run `migration.sql` directly against the production database via SSH tunnel after taking a backup.
