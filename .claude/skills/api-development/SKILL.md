---
name: api-development
description: >
  REST API development guide for the HMIS project. Use when creating or extending any REST API
  endpoint — covers file structure, auth pattern, response format, ApplicationConfig registration,
  CapabilityStatementResource update, AnthropicApiService integration (system prompt module +
  tool handler), and the full post-implementation checklist.
user-invocable: true
---

# HMIS REST API Development Guide

## When This Applies

Any time a new `@Path` class is created under `com.divudi.ws.*`, or an existing API gets new endpoints.

---

## Mandatory Checklist — Every New API

### 1. File placement
- REST class: `src/main/java/com/divudi/ws/<module>/<Name>Api.java`
- `@Path("<name>")`, `@RequestScoped`
- No `@Stateless` on the REST class itself

### 2. Auth — always `Finance` header (unless module uses a different scheme)
```java
String key = requestContext.getHeader("Finance");
if (validateApiKey(key) == null) return errorResponse("Not a valid key", 401);
```
Copy `validateApiKey`, `errorResponse`, `successResponse` verbatim from `DepartmentApi.java` (lines 437–488). Do not reinvent.

### 3. Response envelope — always this shape
```json
{"status":"success","code":200,"data":{...}}
{"status":"error","code":400,"message":"..."}
```
For POST duplicate-detection: `{"status":"already_exists","id":...,"name":...}` (no wrapping envelope).

### 4. Gson
```java
private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
```

### 5. Register in ApplicationConfig
`src/main/java/com/divudi/ws/common/ApplicationConfig.java` — add one line to `addRestResourceClasses()`:
```java
resources.add(com.divudi.ws.<module>.<Name>Api.class);
```

### 6. Document in CapabilityStatementResource
`src/main/java/com/divudi/ws/common/CapabilityStatementResource.java` — add a `resource(...)` entry to `buildResources()`.

### 7. Expose to AI Chat (AnthropicApiService) — TWO places
Both are required every time:

**a) `buildSystemPrompt` — add an `appendModule(...)` block** so the system prompt tells Claude what the endpoint does:
```java
appendModule(sb, "My Module", "/my_module",
    "Description of what this module manages.",
    null,  // or githubUrl(branch, "developer_docs/API_MY_MODULE.md")
    new String[][]{
        {"GET",    "/my_module",      "List entries. Supports query, page, size"},
        {"POST",   "/my_module",      "Create a new entry. Body: {name, code}"},
        {"PUT",    "/my_module/{id}", "Update an entry by ID"},
        {"DELETE", "/my_module/{id}", "Soft-delete an entry by ID"}
    });
```

**b) `buildToolsArray` + `executeToolCall` — add a tool** so Claude can actually call the API:
- Add a `JsonObject myModuleTool` in `buildToolsArray()` and include it in the returned array.
- Add a `case "my_tool_name":` in `executeToolCall(...)` that calls a private `callMyModuleApi(...)` method.
- The `callMyModuleApi` method uses `hmisBaseUrl` + `hmisApiKey` (both available as params to `executeToolCall`).
- `AiChatController.sendMessage(...)` already passes `hmisApiBaseUrl` and `userHmisApiKey` through to `executeToolCall` — use them.

See `ClinicalMetadataApi` + `manage_clinical_metadata` tool in `AnthropicApiService` as a reference implementation.

### 8. Write a developer_docs API file (optional but recommended)
`developer_docs/API_<MODULE>.md` — list all endpoints, params, example request/response.
Reference it in the `appendModule(...)` call via `githubUrl(branch, "developer_docs/API_<MODULE>.md")`.

---

## JPQL Patterns for Simple CRUD APIs

### List (with optional text search + limit)
```java
Map<String, Object> params = new HashMap<>();
params.put("t", symanticType);  // if filtering by type
String jpql;
if (query != null && !query.isEmpty()) {
    jpql = "select c from MyEntity c where c.retired=false and c.symanticType=:t"
         + " and upper(c.name) like :n order by c.name";
    params.put("n", "%" + query.trim().toUpperCase() + "%");
} else {
    jpql = "select c from MyEntity c where c.retired=false and c.symanticType=:t order by c.name";
}
List<MyEntity> items = facade.findByJpql(jpql, params, size);  // 3-arg: (jpql, map, maxRecords)
```

### Duplicate check before POST
```java
MyEntity existing = facade.findFirstByJpql(
    "select c from MyEntity c where c.retired=false and upper(c.name)=:n",
    Map.of("n", name.toUpperCase()));
if (existing != null) {
    // return already_exists response
}
```

### Soft delete
```java
entity.setRetired(true);
entity.setRetiredAt(new Date());
facade.edit(entity);
```

---

## No-service-layer Rule

For thin CRUD over a single entity (like ClinicalMetadataApi), **no `@Stateless` service EJB is needed** — inject facades directly into the REST class. Only add a service layer when there is real business logic, multiple entity coordination, or the method is reused elsewhere.

---

## Testing After Implementation

```bash
# List
curl -s -H "Finance: <key>" "http://localhost:9090/rh/api/<path>?type=X"

# Create
curl -s -H "Finance: <key>" -H "Content-Type: application/json" \
  -X POST "http://localhost:9090/rh/api/<path>?type=X" \
  -d '{"name":"Test"}' | python -m json.tool

# Duplicate check (POST same name again — must return already_exists)
# same command as above

# Update
curl -s -H "Finance: <key>" -H "Content-Type: application/json" \
  -X PUT "http://localhost:9090/rh/api/<path>/<id>" \
  -d '{"name":"Updated"}' | python -m json.tool

# Delete
curl -s -H "Finance: <key>" -X DELETE "http://localhost:9090/rh/api/<path>/<id>" | python -m json.tool

# Capabilities (confirm new entry appears)
curl -s http://localhost:9090/rh/api/capabilities | python -m json.tool
```

For complete reference: [developer_docs/api/rest-api-development-guide.md](../../developer_docs/api/rest-api-development-guide.md)
