# Wiki Docs Site Navigation — Master Plan & Progress

**Issue:** [#24864](https://github.com/hmislk/hmis/issues/24864) · **Branch:** `24864-wiki-site-nav-from-article-index` · **Site:** https://hmislk.github.io/hmis/

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
- [ ] 8. Commit, push, open PR to `development` (`Closes #24864`)
- [ ] 9. After merge: run the workflow (`workflow_dispatch`) and check the live site
- [ ] 10. Wiki content follow-up: use the job-summary report to add orphan pages to `Wiki-Article-Index.md`, fix broken index links, and link the Article Index / Video Library from `Home.md`

## Verification result (step 7, 2026-10-28)

Local build in `tmp/wikisite` against the current wiki:

- 1,196 pages built; 1,195 in the menu (only `_Footer.md` left out, by design). Old menu: 67.
- 0 broken links in `Wiki-Article-Index.md` (after mapping `Home` → `index.md`).
- 858 pages are not in the article index; they appear under More Articles (step 10 shrinks this).
- `mkdocs build` warnings: 189, the same as the old config. All of them come from links inside page content, none from the nav.
- Browser check: the tabs, all 9 index groups, Finance → Cashier Operations → pages, and More Articles groups all render.

## Known follow-ups (out of scope)

- Pages inside wiki sub-folders (`Pharmacy/`, `OPD/`, `Lab/` …) link to other pages with root-relative names, so the converter's relative links break inside those pages. Fix: make `convert-wiki-for-mkdocs.py` resolve link targets by page name and write paths relative to the linking file.
- The site logo and favicon (`images/hmis-logo.png`, `images/favicon.png`) return 404. They are not in the wiki. Add them to the wiki `images/` folder or to `mkdocs-extra/images/`.
- Wiki pages with a list right after a text line, with no blank line in between (e.g. `User-Manual.md`), render as one paragraph in MkDocs. GitHub renders them as lists. The converter could insert the blank line.
- Redirect stubs ("This page has moved…") show up under More Articles. Delete them from the wiki, or have the generator skip them.
- Duplicate page names exist in two folders (e.g. `Cost-of-Goods-Sold-Report`). The generator uses the root-level copy, then the first found.

## Log

- 2026-10-28 — Investigated, filed #24864, created branch and this plan.
- 2026-10-28 — Wrote `tools/generate-wiki-nav.py`; removed static nav; updated workflow and `.gitignore`; verified locally (see above).

## Next step

Step 8: commit, push, open the PR.
