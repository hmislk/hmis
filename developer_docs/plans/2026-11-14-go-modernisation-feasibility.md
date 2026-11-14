# Go and modern stack: high-level feasibility study

Status: OPTIONS ONLY. No decision taken; the recommendation in section 6 is advice, not a decision. Written 2026-11-14 to keep the options for future planning.

## 1. Current system (measured 2026-11-14)

| Item | Size |
|---|---|
| Java | about 2,350 files, about 1.18M lines |
| XHTML pages (JSF/PrimeFaces) | about 4,060 files, over 960K lines |
| JPA entities | about 330 |
| EJB facades | about 290 |
| JSF controllers/beans | about 580 |
| REST classes | about 78 |
| Stack | Java 11, Payara 5, EclipseLink, MySQL, PrimeFaces, JNDI datasources |

About 2M lines built over many years. The value is mostly the encoded business rules (billing, pharmacy stock, inward charging, finance and cash book), many hospital-specific config keys, and production quirks (intentional column typos, PRE/settled bill pairing). Those rules are documented mainly in the code and the data.

## 2. Possible target architecture

```
                    ┌──────────────────────────────┐
  Browser ─────────►│  Web UI (SPA or HTMX)        │
  Mobile / kiosks   │  React/Next.js or Templ+HTMX │
                    └──────────────┬───────────────┘
                                   │ HTTPS (REST/JSON + OpenAPI, SSE/WebSocket)
                    ┌──────────────▼───────────────┐
                    │ API gateway / BFF            │  auth (OIDC), rate limit, tenant routing
                    └──────────────┬───────────────┘
        ┌──────────┬───────────────┼──────────────┬───────────┐
        ▼          ▼               ▼              ▼           ▼
   Billing &   Pharmacy &      Inward /        Lab / LIMS   Reporting
   Payments    Inventory       Clinical        (ASTM/HL7)   (read side)
   (Go)        (Go)            (Go)            (Go)         (Go + SQL)
        └──────────┴───────┬───────┴──────────────┴───────────┘
                           ▼
     PostgreSQL or MySQL  ·  Redis  ·  NATS/Kafka (events)  ·  S3 (files)
     OpenTelemetry · Prometheus/Grafana · Docker · Kubernetes or Nomad
```

Components:

- **Backend:** Go as a modular monolith first (one repo, one package per domain: billing, pharmacy, inward, lab, finance). Split into services later, only where needed.
- **Data access:** `sqlc` (type-safe SQL), `pgx` (or the MySQL driver), `goose` or `atlas` for migrations. Go has no JPA/EclipseLink equivalent, so the query layer is written fresh.
- **API:** OpenAPI-first with generated clients. A FHIR/HL7 gateway as its own component.
- **Frontend:** A new SPA, or server-rendered Go templates with HTMX. HTMX is much cheaper for a form- and table-heavy HIS.
- **Platform:** Containers (for the current per-hospital VM estate, plain Docker with systemd is the realistic first step before Kubernetes or Nomad); CI/CD on GitHub Actions; OpenTelemetry tracing; multi-tenant by schema or database instead of JNDI-per-hospital.
- **Events:** Outbox plus NATS or Kafka for stock movements, accounting export, notifications and audit.

## 3. Potential gains

- **Operations:** Small single binary, about one second start, low memory. More hospitals per VM, no Payara domain restarts.
- **Reliability:** No EclipseLink L2 cache, no leaked JTA transactions, no cache-key deadlocks. Transactions are explicit and short.
- **Performance:** Reports as plain SQL instead of entity graphs.
- **Hiring and onboarding:** Go is smaller to learn than Java EE and JSF.
- **Deployment cost:** Smaller VMs, simpler CI.

## 4. Risks and costs

- **Rewrite scale:** At about 10-20K lines per developer-year including tests (a rough industry rule of thumb, not a measured team rate), 2M lines is dozens of person-years. Go would need perhaps 40-60% of the lines, but 4,000 pages must also be rebuilt.
- **Hidden rules:** A rewrite re-introduces bugs already fixed. In billing and stock these are expensive.
- **Data migration:** Several live hospitals hold years of data. The schema should stay compatible, with the new code reading and writing the existing DB.
- **Parity freeze:** Two codebases for a long time. Every feature and hotfix would be built twice.
- **No ORM:** Cache outages go away, but dirty-tracking is lost, so bugs like #25200 (duplicate rows from a wrong save call) move into hand-written SQL.
- **Team capacity and funding:** Both are limited.

## 5. Options

| Option | Effort | Risk | Verdict |
|---|---|---|---|
| **A. Big-bang rewrite in Go** | Very high, 3+ years | Very high | Not recommended |
| **B. Strangler pattern** | Incremental, 6-18 months per slice | Low to medium | Best fit if the goal is Go |
| **C. Modernise in place** (Java 17/21, Payara 6 or Quarkus, Jakarta EE, REST + new SPA) | Medium | Medium | Cheaper if the aim is "newer", not "Go" |
| **D. Greenfield Go product** for new modules or markets, HMIS kept as is | Medium | Low | Good for new product lines |

### Strangler path (option B)

1. **Keep the existing schema as the shared contract.** Add a gateway in front so some routes go to Go and the rest to Payara.
2. **Start with low-coupling, high-pain slices.** Heavy reports (read-only, easy to compare against old output), then REST/FHIR and lab-analyzer integrations, then the AI chat service.
3. **Move writes later.** Billing, pharmacy stock and inward charging come last. Run old and new side by side, or shadow-write with a diff, before cutover. Shadow writes must use an isolated database, not the shared production database, and must suppress external effects such as payments and notifications.
4. **Build the new UI slice by slice** behind the same login, and retire JSF pages as routes move.
5. **Exit criterion:** Payara serves nothing, then retire it.

## 6. Recommendation (at time of writing)

- A full move to Go is technically feasible but not economically at the current capacity. The cost is the business-rule surface, not the language.
- If the goals are operability and speed, start with **C**.
- Run **B** in parallel for a few new read-only and integration services in Go. This tests the stack at low risk and builds skill.
- Use **D** for any genuinely new product line.
- Before committing, define the target: lower cost, faster performance, easier hiring, or fewer outages. The answer decides between B, C and D.

## 7. Possible pilot slices (not started)

- A read-only report as a Go service (for example the Referring Doctor Wise Sale report), with parity tests against the Java output.
- A lab-analyzer / HL7 gateway.

Either pilot would need a rough estimate and a parity-test plan before it starts.
