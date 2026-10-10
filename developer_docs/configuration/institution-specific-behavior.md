# Institution-Specific Behavior: Config, Not Hardcoded Names

## The anti-pattern

Do not write conditions like:

```xhtml
rendered="#{sessionController.userPreference.applicationInstitution eq 'Ruhuna'}"
```

```xhtml
rendered="#{sessionController.userPreference.applicationInstitution eq 'Cooperative'}"
```

anywhere in the codebase — XHTML `rendered`/`disabled` attributes, Java `if`
branches, JPQL filters, anything. This includes the negated form (`ne 'X'`)
used as a stand-in for "every other hospital."

**As of 2026-08-31 this already exists 135 times across 35 files** (`Ruhuna` 75,
`Cooperative` 54, `Arogya` 6), overwhelmingly in the Inward final-bill print
templates (`inward_bill_final.xhtml`, `finalBillCustom*.xhtml`, etc.). That is
existing debt, not a model to copy. See the "Existing debt" section below for
how to handle it if you're touching that code anyway.

## Why this is highly discouraged

1. **It doesn't scale.** Every time a *different* hospital wants "the Ruhuna
   behavior," or a *new* customer wants a variant of it, someone has to add
   another literal-name branch and redeploy. The config-driven equivalent is an
   admin ticking a checkbox — no code change, no deploy, no developer involved.
2. **It fails silently on rename.** If `Institution.name` is ever edited in the
   DB (a real hospital renaming, a typo fix, a staging-data reset), every branch
   keyed on the old string quietly goes dead — with no compile error, no runtime
   warning, just a feature that stops working for reasons nobody can see from
   the code.
3. **It encodes customer identity into shared source.** The project already
   treats institution/patient identity as something to keep out of
   publicly-visible content (see the "No PII in public GitHub content" policy).
   A customer's name baked into an `eq` check throughout an open history is the
   same category of problem, just self-inflicted instead of accidental.
4. **It conflates "which customer" with "which behavior."** The actual thing
   being toggled (e.g., "show the day-case Green Sheet print with professional
   fees combined") is a real, nameable feature flag. Keying it on a customer's
   identity instead means the branch has to be re-read and re-understood every
   time — "wait, why does `eq 'Ruhuna'` control whether professional fees show
   separately?" — instead of the config key just saying what it does.
5. **It's untestable by name.** Verifying "does this work correctly for
   Hospital X" requires knowing and reproducing that hospital's exact literal
   institution string, rather than flipping a documented, discoverable config
   flag in a test environment.

## The correct pattern

Use `ConfigOption`, resolved per-department first, then per-institution, then
globally — the mechanism already implemented in `ConfigOptionController` /
`ConfigOptionApplicationController` and used correctly almost everywhere else in
the app. See [Application Options](application-options.md) and
[Configuration Options Guide](configuration-options-guide.md).

```xhtml
rendered="#{configOptionController.getBooleanValueByKeyReadOnly('Show Day-Case Green Sheet With Professional Fees', false)}"
```

The admin at each hospital turns their own behavior on or off. The default
(`false`/whatever matches current behavior) means every hospital that never
touches the setting sees no change — which is exactly the bar every hospital-
specific request in this project needs to clear. See, for example, the
"Final Bill Group" per-`InwardChargeType` design in
[`docs/superpowers/specs/2026-08-31-inward-final-bill-charge-type-grouping-design.md`](../../docs/superpowers/specs/2026-08-31-inward-final-bill-charge-type-grouping-design.md),
which deliberately chose a config key over an institution-name check for this
exact reason, even though an existing nearby pattern (`eq 'Ruhuna'`) would have
been the path of least resistance.

## Existing debt

The 135 existing occurrences are not all being fixed as a side effect of this
doc. If you are already editing one of these branches for an unrelated bug fix
or feature, replace *that* branch with a named boolean `ConfigOption` describing
the actual behavior (not a project-wide sweep in the same PR). A dedicated,
deliberate cleanup of the rest belongs in its own tracked GitHub issue — file
one if you want to start that effort, but don't let it block unrelated work.

## Origin note

The earliest instance found in the Inward final-bill flow (`eq 'Ruhuna'` gating
the day-case Green Sheet print) traces to commits by Lawan Samarasekara,
February 2025 — noted here only as a historical anchor for "why this guidance
exists now," not as blame. The pattern was a reasonable shortcut with one or two
hospitals live; it stopped scaling once a third and fourth customer showed up
wanting their own variants.
