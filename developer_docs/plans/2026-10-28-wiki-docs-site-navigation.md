# Wiki Docs Site — Master Plan & Progress

**Phase 1 (menu):** issue [#24864](https://github.com/hmislk/hmis/issues/24864), PR [#24865](https://github.com/hmislk/hmis/pull/24865), merged
**Phase 2 (content rendering):** issue [#24866](https://github.com/hmislk/hmis/issues/24866), PR [#24867](https://github.com/hmislk/hmis/pull/24867), merged
**Phase 3 (User Guide structure + landing page):** issue [#24888](https://github.com/hmislk/hmis/issues/24888), PR [#24889](https://github.com/hmislk/hmis/pull/24889), merged
**Phase 4 (user manual first + wiki clean-up):** issue [#24891](https://github.com/hmislk/hmis/issues/24891), branch `24891-docs-user-manual-first`
**Site:**  https://hmislk.github.io/hmis/

> **New session? Start here.** Read "Progress" and resume at the first unchecked item. Update the checkbox, the log, and "Next step" before you stop.

## Problem

`.github/workflows/publish-wiki-docs.yml` builds the GitHub Wiki into a MkDocs Material site on every wiki edit (`gollum`), on manual dispatch, and nightly. The menu was a hand-written `nav:` in `mkdocs.yml`: 67 of ~1,140 wiki pages. Whole modules (Finance, Video Library, Article Index) were missing and some entries pointed at the wrong page, so readers concluded the system had only a few modules.

## Decision

The menu is generated from the wiki's own **`Wiki-Article-Index.md`** on every build (agreed with the developer on 2026-10-28). Wiki editors control the menu by editing that page.

## Design

| Piece | Behaviour |
|---|---|
| `tools/generate-wiki-nav.py <docs-dir> <base-config> <out-config>` | Runs after `convert-wiki-for-mkdocs.py`. Parses `Wiki-Article-Index.md` from the converted docs and writes `mkdocs.generated.yml` (`INHERIT: mkdocs.yml` + `nav:`). |
| Index parsing | `##` = section, `###` = sub-section, every Markdown link in a list item = page. Targets resolve by page name, case-insensitively, anywhere in the wiki (the GitHub Wiki namespace is flat; files may sit in sub-folders). |
| Tabs | Home · User Manual (Overview, Article Index, then every index `##` group except Developer Documentation) · Videos · Knowledge Base · Developer Guide (index group "Developer Documentation" + developer pages) · More Articles · About |
| More Articles | Every page not reached above, grouped by sub-folder or filename prefix (groups with ≥ 5 pages; the rest under "Other"), sorted alphabetically. |
| Report | Written to `$GITHUB_STEP_SUMMARY` (stdout locally): index links whose page does not exist, and pages not listed in the index. |
| `mkdocs.yml` | Static `nav:` removed. A plain `mkdocs build` still works (MkDocs then auto-generates its own nav). |
| Workflow | Adds the generate step; builds with `mkdocs build -f mkdocs.generated.yml`. |

## Progress

- [x] 1. Investigate the pipeline and measure coverage (67 / ~1,140 pages in the menu; 231 pages unreachable from Home)
- [x] 2. File issue #24864
- [x] 3. Create branch from `origin/development`; write this plan
- [x] 4. Write `tools/generate-wiki-nav.py`
- [x] 5. Remove static `nav:` from `mkdocs.yml`; add `mkdocs.generated.yml` to `.gitignore`
- [x] 6. Update `publish-wiki-docs.yml` (generate step + `-f mkdocs.generated.yml`)
- [x] 7. Verify locally: convert the wiki in `tmp/`, generate the nav, `mkdocs build`, check every index section and its pages appear, and every wiki page is in the nav
- [x] 8. Commit, push, open PR to `development` (`Closes #24864`): [#24865](https://github.com/hmislk/hmis/pull/24865)
- [x] 9. After merge: run the workflow (`workflow_dispatch`) and check the live site (2026-10-28: new menu live)
- [x] 11. Phase 2 (#24866): fix broken links between pages, run-on lists, 2-space nested lists, missing logo/favicon; keep redirect stubs out of the menu
- [x] 12a. Open the phase 2 PR (#24867)
- [x] 12b. After merge, run the workflow and check the live site (2026-10-28: links, lists and logo live)
- [~] 10. Wiki content follow-up: absorbed into phase 3 (steps 13–21), which rewrites the whole article index
- [x] 13. Phase 3: compare competitor docs (Odoo, DHIS2, OpenMRS); agree the design with the developer (2026-10-28)
- [x] 14. Generator: read the 3-level index format (`#` tab, `##` module + description + icon, `###` sub-section); fall back to the old format
- [x] 15. Generator: write the landing page (`docs/index.md`) with audience cards and module cards; enable `pymdownx.emoji` in `mkdocs.yml`
- [x] 16. Classify every wiki page: tab / module / sub-section. Working table: `tmp/wiki-page-classification.tsv`
- [x] 17. Write the new `Wiki-Article-Index.md` from the table (local wiki checkout, not pushed yet)
- [x] 18. Build locally with the new index + generator; check the menu, landing page and report; get the developer's review
- [x] 19. Open the PR (generator + mkdocs.yml); after merge push the new index to the wiki (the push triggers the publish workflow) — PR #24889
- [x] 20. Check the live site (8 tabs, 6 + 13 landing cards, `/Pharmacy/` shows the Pharmacy Module page, 1,200 pages in the sitemap)
- [x] 21. Fix the links that point to pages missing from the wiki (content): done in phase 4, step 23
- [x] 22. Phase 4 (#24891): User Guide becomes the first tab (Get Started folded in as its first module, "Getting Started"); the home page shows module cards first and the other tabs as a short "Other resources" list (generator change)
- [x] 23. Wiki links: every link to a missing page repointed or the page written (`Inward-Report-Timed-Service`, `Document-Templates` — the app's "View Template Examples" button opens it); missing images removed; sub-folder pages' root-relative images resolved by the converter
- [x] 24. Duplicates merged into the kept page, then deleted: the 8 same-title pairs and the 3 `Pharmacy/` copies (unique sections such as troubleshooting and privileges moved over)
- [x] 25. Unnecessary pages deleted after repointing their links: 31 "page has moved" stubs, 2 test pages, 16 empty screen-reference stubs, empty developer stubs, the Institutions / Major Stock Adjustment hubs. Their old site URLs redirect to the replacement page (`mkdocs-redirects`, `redirect_maps` in `mkdocs.yml`); the empty screen stubs redirect to their module page
- [x] 26. Skeleton pages written from the code: Suppliers (absorbs Manufacturers, Importers, Item Suppliers), Wholesale Transactions, Referral Reports, Revenue Reports, Lab Report Templates, Lab Setup Tools, Notification System, EHR Settings, Change Log
- [x] 27. Outdated content fixed: privilege names that no longer exist (transfer privileges → `PharmacyDisbursement*`, room details, role example), Department Preferences screen option list, composite component names in the disposal-return developer page
- [x] 28. Verify locally: 0 broken index links, 0 broken page links (4,920 checked), 0 `mkdocs build` warnings (was 51), More Articles holds only `Pharmacy/Readme`
- [x] 29. Merge the phase 4 PR (generator + converter); push the wiki commit (deletions included); check the live site
- [ ] 30. Later, module by module: review page content against the current screens (accuracy, screenshots). The checks so far were automatic (links, privilege names, screen files) plus the pages touched above

## Phase 3 design (agreed 2026-10-28)

Decision: the hand-curated `Wiki-Article-Index.md` is the **only** source of the menu and landing page. No repo-side mapping file and no automatic classification by filename prefix (the developer ruled both out: "not needed if 1 is done perfectly"). Every user-facing page must therefore be placed in the index.

**Index format**

| In `Wiki-Article-Index.md` | On the site |
|---|---|
| First `#` heading | Page title (ignored by the generator) |
| Later `#` headings: Get Started, User Guide, Administration, Knowledge Base, Developer Guide | Top tabs |
| Italic line right under a `#` or `##` heading | Description on its landing-page card |
| `<!-- icon: material/pill -->` under a `##` heading (invisible on GitHub) | Card icon |
| `##` | Module (sidebar section + landing card) |
| `###` Overview / Daily Tasks / Configuration / Reports | Sub-sections, in this order |
| `- [Title](Page)` | Page |

Home, Videos, More Articles and About stay generated as in phase 1. An index without later `#` headings (the old format) still works: its `##` groups all go under User Guide.

**User Guide modules:** Patient Front Office · Inpatient Care · Pharmacy · Laboratory (LIMS) · Clinical & EHR · Finance & Cashier · Stores & Assets · HR & Payroll · Support Services. Module-specific configuration stays in the module's Configuration sub-section. The Administration tab holds system-wide setup only: users and privileges, institutions and departments, staff and practitioners, items and fees, application settings, audit.

**Landing page:** generated `docs/index.md`, replacing the copy of the wiki `Home.md` (which stays wiki-friendly). It has an intro, audience cards (one per tab, Material grid cards) and module cards (one per User Guide module, linking to the module's first page).

### Phase 3 refinements made while sorting the pages (2026-10-28)

- **Patient Front Office split** into Patients & Registration, OPD (Outpatients), Channelling & Appointments and Membership. **Theatre & Surgery** is its own module, not part of Inpatient Care. One module would have held ~100 pages.
- **Large modules use topic sub-sections** in place of a single "Daily Tasks" list, e.g. Pharmacy: Sales & Returns · Ward & Inpatient Issue · Purchasing & Receiving · Supplier Payments · Stock Transfers · Stock Control & Adjustments · Disposal. The order is always Overview → topics → Configuration → Reports.
- **Administration, Get Started, Knowledge Base and Developer Guide** have modules but no sub-sections.
- **New tab: සිංහල (Sinhala)** for the ~70 Sinhala pages.
- **New Developer Guide section: Screen Reference** for the `pharmacy_*` stubs that only name a source `.xhtml`.
- **Menu labels** come from the page's `#` title, then the old index label, then the page name. Typos are fixed in labels only (Intrim → Interim…); page names are untouched. Where two pages share a label, the one created first is marked "(older page)".
- **Converter fix:** `README.md` → `Readme.md`. MkDocs built `Pharmacy/README.md` as the `/Pharmacy/` index, which hid `Pharmacy.md`.

**Working files (untracked, `tmp/`):**
- `tmp/wiki-pages-raw.tsv`: page, title, first line, size.
- `tmp/classify_wiki_pages.py`: rules + `OVERRIDES` + `LABELS`.
- `tmp/wiki-page-classification.tsv`: the reviewed placement.
- `tmp/build_wiki_index.py`: writes `Wiki-Article-Index.md`; holds tab/module descriptions and icons, sub-section order, and the first page of each module.

**Re-sorting later:** edit `Wiki-Article-Index.md` in the wiki directly. The scripts were for the one-off rewrite.

**Phase 3 verification (local build, 2026-10-28):**
- 1,168 of 1,200 pages are in the menu; the rest are redirect stubs and `_Footer`.
- 0 broken index links; More Articles holds 4 pages.
- `mkdocs build` passes with 51 warnings, all from links inside page content.
- Landing page: 6 audience cards and 13 module cards.

**Wiki clean-up list (content):**
- Merge or delete duplicate pages: the `Pharmacy/` copies of `Cost-of-Goods-Sold-Report`, `Disposal-Issue-Search` and `Pharmacy-Requested-and-Issued-Drug-Overview` (a flat wiki namespace can't link both), plus the 8 "(older page)" pairs.
- Fill in or remove ~55 near-empty pages (under 200 characters).

## Verification result (step 7, 2026-10-28)

Local build in `tmp/wikisite` against the current wiki:

- 1,196 pages built; 1,195 in the menu (only `_Footer.md` left out, by design). Old menu: 67.
- 0 broken links in `Wiki-Article-Index.md` (after mapping `Home` → `index.md`).
- 858 pages are not in the article index; they appear under More Articles (step 10 shrinks this).
- `mkdocs build` warnings: 189, the same as the old config. All of them come from links inside page content, none from the nav.
- Browser check: the tabs, all 9 index groups, Finance → Cashier Operations → pages, and More Articles groups all render.

## Phase 2 verification (2026-10-28)

Local build against the current wiki, compared with the phase 1 converter:

- Links in page content that resolve: 4,098 of 4,138 (was 1,289 of 4,126). The 40 still broken point to pages or images that are not in the wiki. They belong to step 10 (e.g. `Pharmacy-Batch-Management`, `Privileges-and-Access-Control`, `Scope/*`, LIMS `images/*.png`).
- `mkdocs build` warnings: 52 (was 189).
- Lists render as lists (e.g. User Manual); 2-space nested lists render nested.
- The logo shows a Material icon; the favicon is the theme default; no console errors.
- 28 redirect stubs are left out of More Articles. They still build, so old links keep working.
- The link check script is `tmp/linkcheck.py <site-dir> [top-N]`. It is untracked, and easy to recreate: walk the `<article>` hrefs and check that each target exists.

## Known follow-ups (out of scope)

- Duplicate page names exist in two folders (e.g. `Cost-of-Goods-Sold-Report`). The generator uses the root-level copy, then the first found.

## Log

- 2026-10-28 — Investigated, filed #24864, created branch and this plan.
- 2026-10-28 — Wrote `tools/generate-wiki-nav.py`; removed static nav; updated workflow and `.gitignore`; verified locally (see above).
- 2026-10-28 — Opened PR #24865; merged; ran the workflow; new menu live.
- 2026-10-28 — Phase 3: generator reads 3-level index + writes landing page; all 1,162 pages classified; new index written to the local wiki checkout (not pushed); verified locally.
- 2026-10-28 — Phase 2 merged and live.
- 2026-10-28 — Phase 3: competitor review; design agreed; filed #24888; branch created.
- 2026-10-28 — Phase 3: PR #24889 merged; new index pushed to the wiki; publish workflow ran (gollum + manual); live site checked.
- 2026-10-28 — Phase 4: filed #24891; generator + converter changes; wiki clean-up committed locally in `../hmis.wiki` (not pushed); verified locally (steps 22–28).
- 2026-10-28 — Phase 2: filed #24866. Converter now resolves links by page name and fixes lists; Material icon logo; stubs left out of the menu. Verified locally.
- 2026-10-29 — Phase 4: PR #24897 merged (with redirects for removed pages); wiki commit pushed; gollum publish run succeeded; live site checked (User Guide first tab, module cards then "Other resources", old URLs redirect, new pages live).
- 2026-10-29 — Step 30 batch 1 (wiki pushed, live): menu-bar icon guide in How-to-Navigate; Pharmacy Navigation Guide rebuilt from the menu; Application Options / Preferences-tab paths normalised; guessed privileges, procurement and Store admin paths fixed; stale `configuration_keys` / `privileges` lists removed (redirected). CLAUDE.md rule for wiki menu paths.
- 2026-10-29 — Step 30 batch 2 (wiki pushed): invented or outdated menu paths replaced with real menu, page and tab steps on 129 pages (Store, Inpatient, Administration, Channelling, Theatre, Pharmacy, LIMS, Finance); LIMS sample receiving, metadata, antibiotics, investigation items, reference ranges and value flags rewritten from the screens; agency pages rewritten; Courier icon added to How-to-Navigate. Defect found: the **Antibiotics** button on Manage Investigations points to a missing page (`/lab/antibiotic`).

## Next step

Step 30: review content accuracy module by module against the current screens (paths, button labels, privileges, screenshots), starting with the most-used modules. Batches 1 and 2 (privileges, config keys, screen files, menu paths and button labels across the wiki) are done. Next: the button labels `tmp/check_clicks.py` still flags (mostly Finance-*, Channelling and Patient pages), then module-by-module screenshots, starting with Pharmacy and Inpatient. Rebuild locally with `tools/build-docs-site-locally.sh` after each batch and push the wiki.

Local build: `tools/build-docs-site-locally.sh` (needs `../hmis.wiki`; set `MKDOCS` to pick the mkdocs binary). Ad-hoc working scripts (untracked, `tmp/`): `wiki_audit.py` (broken links, near-empty pages, duplicates), `fix_wiki_links.py`, `delete_wiki_pages.py` (deletes pages and repoints links; lists in `wiki-deleted-pages-batch*.txt`).
