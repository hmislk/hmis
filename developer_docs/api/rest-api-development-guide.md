# HMIS REST API Development Guide

## Overview

This guide documents the mandatory steps and patterns for adding any new REST API to HMIS. It was established during the Clinical Metadata API implementation (hmislk/hmis#19791) and consolidates patterns from all prior API work.

---

## File Structure

| Role | Location | Pattern |
|---|---|---|
| REST endpoint class | `com.divudi.ws.<module>/<Name>Api.java` | `@Path`, `@RequestScoped` |
| Business service (only if needed) | `com.divudi.service.<module>/<Name>ApiService.java` | `@Stateless` EJB |
| DTOs (only if needed) | `com.divudi.core.data.dto.<module>/` | JPQL constructor query targets |

> **No-service-layer rule**: For thin CRUD over a single entity, inject facades directly into the REST class. Only add a `@Stateless` service EJB when there is real business logic or multi-entity coordination.

---

## Auth Pattern

All standard APIs use the `Finance` header. Copy these methods verbatim from `DepartmentApi.java`:

```java
private WebUser validateApiKey(String key) {
    if (key == null || key.trim().isEmpty()) return null;
    ApiKey apiKey = apiKeyController.findApiKey(key);
    if (apiKey == null) return null;
    WebUser user = apiKey.getWebUser();
    if (user == null || user.isRetired() || !user.isActivated()) return null;
    if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) return null;
    return user;
}
```

Different header schemes by module:
| Header | Used by |
|---|---|
| `Finance` | All standard APIs (pharmacy, institution, clinical, users, finance…) |
| `Token` | Channel/Booking, Consultant Management |
| `FHIR` | FHIR Patient (`/fhir/Patient`) |
| `Config` | Config Management (`/config`) |
| Custom | LIMS (URL-embedded / JSON body / HTTP Basic) |

---

## Response Envelope

**Always** use this shape — copy `errorResponse` and `successResponse` from `DepartmentApi.java`:

```json
{"status":"success","code":200,"data":{...}}
{"status":"error","code":400,"message":"..."}
```

For POST duplicate detection (not wrapped in the success envelope):
```json
{"status":"already_exists","id":42,"name":"Fever","type":"symptom"}
```

---

## Registering the API — 4 Required Files

Every new API must touch **all four** of these files:

### 1. `ApplicationConfig.java`
`src/main/java/com/divudi/ws/common/ApplicationConfig.java`

Add one line to `addRestResourceClasses()`:
```java
resources.add(com.divudi.ws.<module>.<Name>Api.class);
```

### 2. `CapabilityStatementResource.java`
`src/main/java/com/divudi/ws/common/CapabilityStatementResource.java`

Add a `resource(...)` entry to `buildResources()`:
```java
.add(resource("My Module", "/api/my_module",
    "Description of what this module does.",
    "API Key",
    "GET", "POST", "PUT", "DELETE"))
```

### 3. `AnthropicApiService.java` — `buildSystemPrompt` (module listing)
`src/main/java/com/divudi/service/AnthropicApiService.java`

Add an `appendModule(...)` block so the AI chat system prompt knows what the endpoint does:
```java
appendModule(sb, "My Module", "/my_module",
    "What this module manages.",
    null,  // or githubUrl(branch, "developer_docs/API_MY_MODULE.md")
    new String[][]{
        {"GET",    "/my_module",      "List entries"},
        {"POST",   "/my_module",      "Create entry. Returns already_exists if duplicate."},
        {"PUT",    "/my_module/{id}", "Update entry by ID"},
        {"DELETE", "/my_module/{id}", "Soft-delete entry by ID"}
    });
```

### 4. `AnthropicApiService.java` — `buildToolsArray` + `executeToolCall` (tool)
So the AI can **actually call** the API (not just know it exists), add:

**In `buildToolsArray()`:**
```java
JsonObject myTool = Json.createObjectBuilder()
    .add("name", "manage_my_module")
    .add("description", "Create, list, update, or delete ...")
    .add("input_schema", Json.createObjectBuilder()
        .add("type", "object")
        .add("properties", Json.createObjectBuilder()
            .add("method", Json.createObjectBuilder()
                .add("type", "string")
                .add("enum", Json.createArrayBuilder().add("GET").add("POST").add("PUT").add("DELETE")))
            // ... other inputs
        )
        .add("required", Json.createArrayBuilder().add("method")))
    .build();
// add to the returned Json.createArrayBuilder()
```

**In `executeToolCall(...)`:**
```java
case "manage_my_module": {
    // extract inputs from toolInput
    return callMyModuleApi(..., hmisBaseUrl, hmisApiKey);
}
```

**New private method:**
```java
private String callMyModuleApi(..., String hmisBaseUrl, String hmisApiKey) {
    // build URL, make HttpClient call with Finance header, return response body
}
```

The `hmisBaseUrl` and `hmisApiKey` are already threaded through `sendMessage` → `executeToolCall`. Use them directly.

**Also update `buildSystemPrompt` tools description** — the count in `"You have N tools"` and the tool listing.

---

## JPQL Patterns

### List with optional search + limit
```java
Map<String, Object> params = new HashMap<>();
params.put("t", symanticType);
String jpql = query != null && !query.isEmpty()
    ? "select c from MyEntity c where c.retired=false and c.symanticType=:t and upper(c.name) like :n order by c.name"
    : "select c from MyEntity c where c.retired=false and c.symanticType=:t order by c.name";
if (query != null && !query.isEmpty()) params.put("n", "%" + query.toUpperCase() + "%");
List<MyEntity> items = facade.findByJpql(jpql, params, size);  // 3-arg variant
```

### Duplicate check before POST
```java
MyEntity existing = facade.findFirstByJpql(
    "select c from MyEntity c where c.retired=false and upper(c.name)=:n",
    Map.of("n", name.toUpperCase()));
```

### Soft delete
```java
entity.setRetired(true);
entity.setRetiredAt(new Date());
facade.edit(entity);
```

---

## Reference Implementations

| API | Class | Notes |
|---|---|---|
| Clinical Metadata | `ClinicalMetadataApi.java` | No service layer, inline maps, two entity types, `manage_clinical_metadata` tool |
| Department | `DepartmentApi.java` | Service layer, DTOs, full CRUD |
| Pharmaceutical Items | `PharmaceuticalItemApi.java` | Complex hierarchy, service layer |
| Favourite Medicines | `FavouriteMedicineApi.java` | Service layer, clinical data |

---

## Post-Implementation Testing Checklist

```bash
BASE="http://localhost:9090/rh/api/<path>"
KEY="<finance-api-key>"

# 1. GET list
curl -s -H "Finance: $KEY" "$BASE?type=X" | python -m json.tool

# 2. POST create
curl -s -H "Finance: $KEY" -H "Content-Type: application/json" \
  -X POST "$BASE?type=X" -d '{"name":"Test Entry"}' | python -m json.tool

# 3. POST same name → must return already_exists with id
# (repeat same command)

# 4. GET with search
curl -s -H "Finance: $KEY" "$BASE?type=X&query=test" | python -m json.tool

# 5. PUT update
curl -s -H "Finance: $KEY" -H "Content-Type: application/json" \
  -X PUT "$BASE/<id>" -d '{"name":"Updated Name"}' | python -m json.tool

# 6. DELETE
curl -s -H "Finance: $KEY" -X DELETE "$BASE/<id>" | python -m json.tool

# 7. Capabilities endpoint shows new entry
curl -s http://localhost:9090/rh/api/capabilities | python -m json.tool

# 8. AI Chat can list and create via manage_* tool
# Open /ai_chat.xhtml and prompt: "List all procedures in the clinical metadata API"
```
