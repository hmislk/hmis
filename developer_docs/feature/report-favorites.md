# Report Favorites — Implementation Guide

## Overview

**Report Favorites** lets a logged-in user star any report button on a report/analytics index page (Asset Reports, Pharmacy Analytics, OPD Analytics, etc.) so it also appears on a pinned **⭐ Favorites** tab at the top of that page. It is self-service — no admin assignment step — and was first implemented on the main Reports page (`reports/index.xhtml`, PR #22728, spacing fix #22737/issue #22736).

This guide is for two situations:

1. **Adding a new report button to a page that already has Favorites** — you must wire the new button into the Favorites tab too, or it will simply be impossible to favorite.
2. **Adding Favorites to a report/analytics index page that doesn't have it yet** (tracked under the "Report Favorites rollout" master issue).

Read the whole guide before touching either — the two checklists at the bottom assume you understand the architecture and the two gotchas first.

---

## Architecture

| Piece | What it is |
|---|---|
| `UserFavoriteReport` (`core/entity`) | Soft-delete entity: `id`, `webUser`, `reportKey`, `reportLabel`, `category`, `orderNumber`, `retired`. Table: `USERFAVORITEREPORT`. |
| `UserFavoriteReportFacade` | Plain JPA facade for the entity above. |
| `UserFavoriteReportController` (`@SessionScoped`) | `isFavorite(reportKey)`, `toggleFavorite(reportKey, label, category)`, `getFavorites()`. Caches the logged-in user's non-retired favorites for the session; reloads after every toggle. |
| `resources/ezcomp/reports/favoriteStar.xhtml` (`<fav:favoriteStar>`) | The star toggle button composite. Takes `reportKey`, `label`, `category`, optional `visible` (default `true`). |

---

## 🚨 Gotcha 1 — `reportKey` is a single, flat, app-wide namespace

`UserFavoriteReportController` is one `@SessionScoped` bean shared across the **entire application**, not one instance per page. `isFavorite(reportKey)` only ever checks the reportKey string — it has no idea which page called it. This means:

- **Every `reportKey` in the whole app must be globally unique.** Two different reports on two different pages must never share a key — if they did, favoriting one would incorrectly show the other as favorited too (and clicking it from the Favorites tab would run the wrong navigation action).
- The original Reports page (`reports/index.xhtml`) uses short camelCase keys with no prefix (`assetRegister`, `profitMatrixReport`, …). **Leave those as-is** — do not rename existing keys, since they're persisted per-user in the `USERFAVORITEREPORT` table and a rename silently orphans existing users' favorites.
- **Every other page being wired in under the Favorites rollout must prefix its keys** with a short page code, to make collisions structurally impossible without having to manually grep the whole codebase every time:

| Page | Prefix |
|---|---|
| Pharmacy Analytics / Theatre Analytics (same page) | `pharmacyAnalytics_` |
| OPD Analytics | `opdAnalytics_` |
| Lab Analytics | `labAnalytics_` |
| Membership Analytics | `membershipAnalytics_` |
| Inpatient Analytics | `inpatientAnalytics_` |
| Store Analytics | `storeAnalytics_` |
| Channel Analytics | `channelAnalytics_` |
| HR Analytics | `hrAnalytics_` |
| EMR Analytics | `emrAnalytics_` |
| Reports section's own "Analytics" page (`/analytics/index.xhtml`, distinct from the Reports page) | `reportsAnalytics_` |

Example: the "Daily Return" button on Pharmacy Analytics would use `reportKey="pharmacyAnalytics_dailyReturn"`, not `reportKey="dailyReturn"`.

---

## 🚨 Gotcha 2 — gate the whole row, never just the button (the spacing bug)

The Favorites tab is built by **duplicating** every report button from its home category tab into the Favorites tab, gated with `rendered="#{userFavoriteReportController.isFavorite('key')}"` so only starred reports actually show.

**This `rendered` condition must be on the row's outer wrapper, not just the button/star inside it.** Plain HTML `<div>` does not support JSF's `rendered` attribute at all (JSF silently ignores it), so an early version of this feature put `rendered` only on the inner `<p:commandButton>` and the star's `visible` — leaving an empty-but-still-present `<div>` in the DOM for every non-favorited report. The parent list uses CSS Grid (`d-grid gap-2`), which adds spacing **between every direct child, including empty ones** — with only 2 of ~213 reports favorited, roughly 211 invisible rows still each contributed to the grid gap, producing one large blank space between the two visible favorites. See issue #22736 / PR #22737 for the full writeup and before/after screenshots.

**Correct pattern** — use `<h:panelGroup layout="block">` (a real JSF component) as the row wrapper, with `rendered` on it:

```xml
<!-- Home category tab: unconditional, no rendered/visible needed -->
<div class="d-flex align-items-center gap-1 w-100">
    <p:commandButton styleClass="w-100" ajax="false" value="1. Asset Register"
                      action="#{reportController.navigateToAssetRegister()}" />
    <fav:favoriteStar reportKey="assetRegister" label="1. Asset Register" category="Asset Reports" />
</div>

<!-- Favorites tab: rendered on the WRAPPER, not the button/star -->
<h:panelGroup layout="block" styleClass="d-flex align-items-center gap-1 w-100"
              rendered="#{userFavoriteReportController.isFavorite('assetRegister')}">
    <p:commandButton styleClass="w-100" ajax="false" value="1. Asset Register"
                      action="#{reportController.navigateToAssetRegister()}" />
    <fav:favoriteStar reportKey="assetRegister" label="1. Asset Register" category="Asset Reports" />
</h:panelGroup>
```

**If the home-tab button is privilege-gated**, AND the same privilege check into the Favorites-tab wrapper's `rendered`, so a favorited report a user no longer has access to disappears from the Favorites tab too:

```xml
rendered="#{webUserController.hasPrivilege('ViewFundTransferReports') and userFavoriteReportController.isFavorite('fundTransferReport')}"
```

---

## 🚨 Gotcha 3 — the "no favorites yet" empty-state message reads the *global* favorites list

`reports/index.xhtml`'s Favorites tab shows an empty-state message with:

```xml
rendered="#{empty userFavoriteReportController.favorites}"
```

`getFavorites()` returns the user's favorites **across the whole app**, not just this page. Once a second page adopts Favorites, a user whose only favorite is on that *other* page will hit this on the Reports page: `favorites` is non-empty globally, so the empty-state message won't render — but none of this page's own duplicated rows match either, so the tab renders as a blank, unexplained space instead of a helpful message.

**Every new page must use a page-scoped empty check, not the raw `empty ... favorites` expression.** Add a category-scoped helper to `UserFavoriteReportController` (add once, share across all pages) instead of copy-pasting the broken pattern:

```java
public boolean hasAnyFavoriteInCategories(List<String> categories) {
    for (UserFavoriteReport f : getCachedFavorites()) {
        if (categories.contains(f.getCategory())) {
            return true;
        }
    }
    return false;
}
```

Each page's own controller then exposes a fixed `List<String>` of its own category names (the same strings passed as `category="..."` to `<fav:favoriteStar>` on that page), and the empty-state check becomes:

```xml
rendered="#{not userFavoriteReportController.hasAnyFavoriteInCategories(pharmacyController.analyticsFavoriteCategories)}"
```

This is a small, one-time shared change — add it as part of whichever sub-issue is implemented first under the rollout, then every subsequent page just reuses `hasAnyFavoriteInCategories(...)`.

---

## Checklist: adding a NEW report button to a page that already has Favorites

- [ ] Pick a `reportKey` — reuse the page's existing prefix (see the table above), never reuse an existing key
- [ ] `grep -rn 'reportKey="yourNewKey"' src/main/webapp` first to confirm it's not already used anywhere in the app
- [ ] Add `<fav:favoriteStar reportKey="..." label="..." category="..." />` beside the button in its home tab
- [ ] Duplicate the whole row into the page's Favorites tab, wrapped in `<h:panelGroup layout="block" ... rendered="#{userFavoriteReportController.isFavorite('yourNewKey')}">` — **never a raw `<div>`** (Gotcha 2)
- [ ] If the button is privilege-gated, AND that same privilege check into the Favorites-tab wrapper's `rendered` too
- [ ] Never rename or reuse an existing `reportKey` once it has shipped — the underlying report's label/action can change freely, but the key is the stable identity users' starred rows are keyed against

## Checklist: adding Favorites to a report/analytics page for the first time

- [ ] Add a pinned **⭐ Favorites** tab/panel as the first tab on the page
- [ ] Use the page-scoped empty-state check (Gotcha 3), not `#{empty userFavoriteReportController.favorites}`
- [ ] Pick this page's `reportKey` prefix (see the table above) and use it for every button on the page
- [ ] Duplicate every existing report button into the Favorites tab per the pattern in Gotcha 2
- [ ] Verify with Playwright: star 2–3 reports across different categories/tabs on the page, confirm the Favorites tab shows them stacked tightly (no large blank gap), confirm clicking a favorited report navigates correctly, confirm unfavoriting and the empty-state message both work
- [ ] Verify against the DB: `USERFAVORITEREPORT` rows are created on favorite and soft-deleted (`RETIRED=1`) on unfavorite
- [ ] Update the user-facing wiki page ([Report Favorites](https://github.com/hmislk/hmis/wiki/Report-Favorites)) to mention the new page

---

## Reference implementation

- `reports/index.xhtml` — original feature (PR #22728), spacing fix (issue #22736 / PR #22737)
- Wiki: [Report Favorites](https://github.com/hmislk/hmis/wiki/Report-Favorites)
