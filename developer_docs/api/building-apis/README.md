# Building HMIS APIs — Index

For agents **implementing** a new REST endpoint (or extending an existing one) in
`com.divudi.ws.*`. If you only need to know how to *call* an existing endpoint, use
[`../using-apis/`](../using-apis/) instead — nothing here documents request/response payloads
for a specific business module.

| File | Use for |
|---|---|
| [rest-api-development-guide.md](rest-api-development-guide.md) | The mandatory pattern for any new HMIS REST API: file layout, `Finance` auth, response envelope, the 4 required registration points (`ApplicationConfig`, `CapabilityStatementResource`, `AnthropicApiService` module + tool), JPQL patterns, post-implementation test checklist |
| [sap-integration-guide.md](sap-integration-guide.md) | Architecture of the SAP S/4HANA integration (outbound billing, inbound payment webhook, inventory sync) — read before touching `com.divudi.service.sap.*` or `com.divudi.ws.sap.*` |
| [sap-integration-testing.md](sap-integration-testing.md) | Testing the SAP integration against the SAP API Business Hub sandbox and a local HMIS instance |

For the step-by-step checklist form of `rest-api-development-guide.md`, use the `api-development`
Claude Code skill (`.claude/skills/api-development/SKILL.md`) — it's the same content trimmed to
a build-order checklist.

New per-module endpoint documentation you write belongs in
[`../using-apis/API_<MODULE>.md`](../using-apis/), not here — this folder is for guides on
*how to build*, not references for *what a specific module does*.
