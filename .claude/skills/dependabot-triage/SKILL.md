---
name: dependabot-triage
description: >
  Use when Dependabot (or Renovate/any bot) has open dependency-bump PRs on
  hmislk/hmis and the developer wants them checked and merged or closed, e.g.
  "handle the dependabot PRs", "merge or close the pending dependency
  updates", "/dependabot-triage". Verifies each bump against our JDK 11 /
  Payara 5 / javax stack beyond CI compile (bytecode level, javax vs jakarta,
  sibling-artifact versions, pom pins, release-note breaking changes, a
  combined build and a local runtime smoke test), then merges the safe ones,
  closes the rest with a reason, and adds a dependabot.yml ignore rule so a
  rejected bump does not come back.
---

# Dependabot Triage

CI `validate-compilation` only proves the code **compiles**. Every past
Dependabot breakage compiled fine and failed at **runtime** on Payara:
`UnsupportedClassVersionError` (Java 17 jars), `jakarta.*` vs `javax.*`,
`NoSuchMethodError`/`NoClassDefFoundError` (siblings on different versions,
a pom pin holding a transitive back), or a quiet feature loss from an API
change. So a green PR is a candidate, never a verdict.

## Our stack (what every bump must fit)

| Fact | Consequence |
|---|---|
| Production: **JDK 11**, **Payara 5** (Java EE 8, `javax.*`, JSF 2.3) | Class files must be major **≤ 55**; no `jakarta.*` packages; JSF libs must be the JSF 2.3 line (OmniFaces 3.x, PrimeFaces non-`jakarta` classifier) |
| `pom.xml` has no `dependencyManagement`; versions are explicit | Sibling artifacts are NOT aligned automatically |
| `maven-enforcer-plugin` `enforceBytecodeVersion maxJdkVersion=11` | `mvn package` catches Java 12+ jars (incl. transitive) — but only if you build |
| `provided` scope (EclipseLink, `javaee-web-api`) | Payara's copy runs, not ours; a bump changes compile only |
| Prod builds on `ubuntu-latest` (Maven 3.9.x); self-hosted runners use older Maven (local `/usr/bin/mvn` 3.8.7, NetBeans-bundled Maven on qa3 / Ruhunu-local) | Build-plugin bumps must work on **Maven 3.8.x** — check the plugin pom `<prerequisites>` |
| `.github/dependabot.yml` already ignores all semver-major bumps and known-bad families | Read its comments first — they record earlier rejections |

### Known sibling sets (must move together)

`${jackson.version}` (all Jackson, and stays < 2.20) · `poi` + `poi-ooxml` (+ `commons-io` pin) · `hapi-fhir-base` + `hapi-fhir-structures-r5` · `junit-jupiter-api` + `junit-jupiter-engine` · `log4j-api` + any log4j artifact · `commons-text` needs the `commons-lang3` it was built against · `itextpdf` 5.x vs `com.itextpdf:io`/`html2pdf` (different product lines, independent).
If a bump touches one member, the PR must bump the others in the same PR (push a commit to the Dependabot branch) or be closed.

## Step 1 — Collect

```bash
gh pr list --repo hmislk/hmis --author "app/dependabot" --state open \
  --json number,title,headRefName,baseRefName,statusCheckRollup
gh pr diff <N> --repo hmislk/hmis      # confirm it only touches the expected pom line(s)
```

A PR whose base is not `development` is closed (deploy branches get bumps only via `development`).

## Step 2 — Classify each PR

For each: groupId:artifactId, from → to, update type (patch / minor / major), scope (runtime lib / `provided` / test / build plugin), and **where the code uses it**:

```bash
grep -rhoE "import <package>[^;]+" src/main/java | sort | uniq -c
grep -rn "<library-or-tag-prefix>" src/main/webapp src/main/webapp/WEB-INF/*.xml
```

Unused at runtime (no imports, not in xml) → low risk, but note it; it may be a removable dependency (raise an issue, don't remove in the bump PR).

## Step 3 — Hard rejects (close without further testing)

- **Major** version bump (should already be ignored; if it arrived, it's a security update — evaluate as a deliberate upgrade issue, not a merge).
- Any class in the new jar (or new transitive) with major **> 55**:
  ```bash
  curl -s -o x.jar https://repo1.maven.org/maven2/<g/as/path>/<a>/<v>/<a>-<v>.jar
  c=$(unzip -Z1 x.jar | grep '\.class$' | grep -vE 'module-info|META-INF/versions' | head -1)
  unzip -p x.jar "$c" | head -c 8 | od -An -tu1 | awk '{print $8}'   # 52=Java8, 55=Java11, 61=Java17
  unzip -l x.jar | grep -c 'jakarta/'                                  # must be 0
  ```
- The library moved to `jakarta.*` / Jakarta EE 9+ (release notes say "Jakarta", "Faces 4", "Servlet 5+").
- A known sibling set would be split (see above) and fixing it widens the change beyond a patch.
- The new version is built against a newer version of something `pom.xml` pins directly (check `mvn dependency:tree -Dverbose` for `omitted for conflict with` on the bumped library's dependencies).

## Step 4 — Read what changed

Read every release between the two versions, not just the last:

```bash
gh api "repos/<owner>/<repo>/releases?per_page=40" --jq '.[] | .tag_name+"\n"+.body'
gh api "repos/<owner>/<repo>/compare/<from-tag>...<to-tag>" --jq '.commits[].commit.message | split("\n")[0]'
```

Look for: removed / renamed / deprecated API, changed defaults, "requires Java/Maven X", Jakarta, behaviour fixes in features we use (map each to the Step 2 usage). A change in a feature we use means that feature goes on the smoke-test list.

## Step 5 — Combined build (all candidates together, plus a baseline)

Never use a git worktree (CLAUDE.md). Export snapshots instead:

```bash
D=tmp/dependabot/$(date +%F); rm -rf $D; mkdir -p $D/base $D/combined
git fetch origin development
git archive origin/development | tar -x -C $D/base
git archive origin/development | tar -x -C $D/combined
# apply each surviving PR's pom change to $D/combined/pom.xml (sed on the exact old version line), then:
diff $D/base/pom.xml $D/combined/pom.xml              # must equal the union of the PR diffs
cp src/main/resources/META-INF/persistence.xml $D/combined/src/main/resources/META-INF/   # local JNDI, for deploy
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 PATH=$JAVA_HOME/bin:$PATH
(cd $D/base     && mvn -B -ntp dependency:tree -DoutputFile=../tree-base.txt     && mvn -B -ntp clean package -DskipTests > ../build-base.log 2>&1)
(cd $D/combined && mvn -B -ntp dependency:tree -DoutputFile=../tree-combined.txt && mvn -B -ntp clean package -DskipTests > ../build-combined.log 2>&1)
```

Run it in the background (several minutes each). Then check:

1. Both `BUILD SUCCESS`; the enforcer ran (`enforce-banned-dependencies` in the log) and passed.
2. `diff tree-base.txt tree-combined.txt` — only the intended artifacts (and their own transitives) changed; nothing else was pulled up or down.
3. `diff <(unzip -Z1 base/target/*.war | grep WEB-INF/lib | sort) <(unzip -Z1 combined/target/*.war | grep WEB-INF/lib | sort)` — only the intended jars changed.
4. **Build-plugin bumps** (`maven-compiler-plugin`, `maven-war-plugin`, enforcer, …): compare the output, not just success. Same class count, same class-file major (55), same debug attributes (stack traces in production need line numbers):
   ```bash
   for t in base combined; do find $D/$t/target/classes -name '*.class' | wc -l; done
   C=com/divudi/bean/common/SessionController.class
   for t in base combined; do javap -v $D/$t/target/classes/$C | grep -E "major version|LineNumberTable|LocalVariableTable|SourceFile" | sort | uniq -c; done
   ```
   Plus WAR layout: same `WEB-INF/` top-level entries and same `WEB-INF/print-agent` resources.

Any failure → find which bump caused it (rebuild with that one reverted) and send that PR to Step 7 (close); keep testing the rest.

## Step 6 — Runtime smoke test on local Payara

Skip only when every surviving bump is build-plugin/test-scope/`provided` **and** Step 5.4 showed identical output.

```bash
/home/buddhika/payara/bin/asadmin redeploy --name rh-3.0.0 --contextroot /rh $D/combined/target/rh-3.0.0.war
grep -nE "UnsupportedClassVersion|NoSuchMethod|NoClassDefFound|ClassNotFound|LinkageError|AbstractMethodError|IncompatibleClassChange|jakarta\." \
  /home/buddhika/payara/glassfish/domains/domain1/logs/server.log | tail
```

Known noise, present on `development` too (compare with lines logged before your deploy, not with zero): `Unsupported class file major version 65 … META-INF/versions/21/com/fasterxml/jackson/…` (Payara's scanner reading Jackson's multi-release classes; harmless) and HAPI FHIR `INFO` lines logged as `SEVERE` because they go to stderr. Only new error types count.

Then with the `playwright-e2e` skill: open the app root, log in, select a department (click through menus, never type inner URLs), and exercise **each bumped library's feature** plus whatever Step 4 flagged:

| Library | Exercise |
|---|---|
| OmniFaces (`o:socket` in `ezcomp/menu.xhtml`, every page) | Server log shows `Using OmniFaces version <new>`; no JS console error; on reload a websocket to `/rh/omnifaces.push/notifications` opens and stays open (`browser_run_code_unsafe`: `page.on('websocket', …)` then `page.reload()`) |
| PrimeFaces | Open a page with a dataTable + dialog + autocomplete (e.g. OPD billing) |
| itextpdf (5.x, `com.itextpdf.text`) | Pharmacy → Pharmacy Analytics → Summary Reports → Pharmacy Income Report → set From to a date with sales (query `BILL` for one) → Process → PDF. `pdfinfo` must show `Producer: iText® <new version>` and ≥1 page. **An empty date range downloads a 0-byte file by design** — that is not a failure, pick dates with data |
| html2pdf / `com.itextpdf:io` | Any bill/report PDF that goes through `PdfController` |
| POI / commons-io | Export a report to Excel |
| Jackson / Gson / JSON | Call a REST endpoint (`api-usage` skill) and check the response body |
| HAPI FHIR | Hit a FHIR endpoint |
| zxing / barcode4j | Print preview a bill or sample label showing a barcode/QR |
| tomcat-catalina, log4j, slf4j, commons-* | Startup log clean is enough unless Step 2 found a direct use |

Re-grep `server.log` after the clicks. Record the menu paths used.

After the run, the local app is on the combined build. If any PR is closed, rebuild/redeploy from `development` when the merges land.

## Step 7 — Act

**Merge** (each PR that passed everything), one at a time, oldest first:

```bash
gh pr merge <N> --repo hmislk/hmis --merge
```

After each merge GitHub reports the others as `mergeable: UNKNOWN` for a while — poll (`gh pr view <N> --json mergeable,headRefOid`) until `MERGEABLE`, then merge with `--match-head-commit <sha>`. If the head changed (Dependabot rebased it), re-check that `gh pr diff` is still exactly the tested line before merging. If one shows a conflict, comment `@dependabot rebase` and wait for green CI. Merges to `development` deploy nowhere by themselves; deploy branches pick them up through the normal QA flow.

**Close** (each PR that failed), with a comment stating the concrete reason and the evidence (class-file version, the missing method, the release-note line), then stop it coming back:

```bash
gh pr close <N> --repo hmislk/hmis --comment "Closing: <reason + evidence>. Dependabot is told to skip this in #<PR>."
```

Add an `ignore` entry to `.github/dependabot.yml` with a one-line comment saying **why** (match the existing style: version range for a Java-17 cutover, `update-types` for an API-breaking line). Do this on a branch from `origin/development` with an issue (`Closes #N`), one PR for all new ignore rules of the run. Only close without an ignore rule when the failure is a temporary sibling mismatch that the next grouped bump will fix — say so in the comment.

**Needs a deliberate upgrade** (the version is worth having but needs code changes): close as above and open an issue describing the port (no hospital data).

## Step 8 — Report

A table in chat: PR · artifact from→to · scope · used where · checks run · verdict (merged / closed + reason). Plus: new ignore rules (PR link), issues opened, anything noticed but not acted on (e.g. an unused dependency). Save the evidence (logs, tree diffs) under `tmp/dependabot/<date>/`; it is not committed.

## Rules

- 🚨 Never merge on green CI alone; Steps 3–5 always run, Step 6 whenever a runtime jar changed.
- 🚨 Never merge a PR whose head changed after it was tested (`gh pr view <N> --json headRefOid` before merging).
- 🚨 Never bump a member of a sibling set alone.
- Never edit `pom.xml` on `development` directly to "fix" a Dependabot PR; push the fix to the Dependabot branch or close the PR.
- No worktrees; snapshots via `git archive` under `tmp/`.
- Closing comments follow the [Public Content Policy](../../../developer_docs/git/github-public-content-policy.md).
