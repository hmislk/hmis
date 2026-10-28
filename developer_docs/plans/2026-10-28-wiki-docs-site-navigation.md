# Wiki Docs Site — Master Plan & Progress

**Phase 1 (menu):** issue [#24864](https://github.com/hmislk/hmis/issues/24864), PR [#24865](https://github.com/hmislk/hmis/pull/24865), merged
**Phase 2 (content rendering):** issue [#24866](https://github.com/hmislk/hmis/issues/24866), PR [#24867](https://github.com/hmislk/hmis/pull/24867), merged
**Phase 3 (User Guide structure + landing page):** issue [#24888](https://github.com/hmislk/hmis/issues/24888), branch `24888-wiki-site-user-guide-structure`
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
- [x] 18. Build locally with the new index + generator; check the menu, landing page and report; get the developer's review (preview built; review pending)
- [ ] 19. Open the PR (generator + mkdocs.yml); after merge push the new index to the wiki (the push triggers the publish workflow)
- [ ] 20. Check the live site
- [ ] 21. Fix the 40 links that point to pages missing from the wiki (content)

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
- 2026-10-28 — Phase 2: filed #24866. Converter now resolves links by page name and fixes lists; Material icon logo; stubs left out of the menu. Verified locally.

## Next step

Step 18: developer reviews the local preview (`tmp/wikisite/site`, served with `python3 -m http.server 8765`) and the new `../hmis.wiki/Wiki-Article-Index.md`. Then step 19: open the PR, and after it merges push the wiki index (`git -C ../hmis.wiki commit -am ... && git push`).
