# HMIS API Documentation

Split into two audiences so an agent only loads the docs relevant to its task:

- **[`using-apis/`](using-apis/)** — reference for **calling** an existing HMIS REST endpoint
  (the in-app AI Chat feature, an external integration, or a coding agent that needs to know an
  endpoint's request/response shape before writing a caller). One file per API module: base
  path, auth header, parameters, example request/response, error codes. No implementation detail.

- **[`building-apis/`](building-apis/)** — guide for **implementing** a new REST endpoint or
  extending an existing one: file layout, auth pattern, response envelope, the mandatory
  registration checklist (`ApplicationConfig`, `CapabilityStatementResource`,
  `AnthropicApiService`), JPQL patterns. No per-module endpoint reference.

When you finish building a new API, its consumer-facing reference doc goes in `using-apis/`,
named `API_<MODULE>.md` — see the last step of `building-apis/rest-api-development-guide.md`.
