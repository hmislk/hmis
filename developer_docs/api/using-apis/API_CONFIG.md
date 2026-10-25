# System Configuration API

Base path: `/api/config`
Authentication: `Config` header (**not** `Finance` — this is a separate key)

## Authentication

**Every** endpoint in this API — reads and writes alike — requires an API key
whose **key type is `Config`**, sent in the `Config` header. The key must also
be non-retired and not past its expiry date. Any other key type (`Finance`,
`Token`, `FHIR`, …) is rejected with HTTP 401 `Not authorized`, even when it is
otherwise valid and even if it is sent in the `Config` header.

Create a Config key from *Menu → Manage My API Keys* by choosing **Config** as the key
type.

> Before issue #24198 the legacy `setBoolean`/`setLongText`/`setInteger`
> endpoints accepted any valid key type (e.g. a Finance key), while the read
> endpoints required a Config key — so a caller could change a value it could
> not read back. All endpoints now enforce the same Config-type rule. Callers
> that were sending a Finance key to `setBoolean`/`setLongText`/`setInteger`
> must switch to a Config key.

Content types vary by endpoint:
- The read/list/update endpoints (`GET /api/config`, `GET /api/config/{key}`, `PUT /api/config/{key}`) accept/return **`application/json`**.
- The legacy `setBoolean`/`setLongText`/`setInteger` endpoints take the value in the path and return **`text/plain`**.
- `setShortText`/`setDouble` take a JSON body `{"value":"..."}` and return **`text/plain`**.

Used to read and set application configuration options at runtime without redeployment.

## Endpoints

### GET `/api/config?scope={tag}` — List config options by scope tag

```bash
GET /api/config?scope=inward
Header: Config: YOUR_CONFIG_API_KEY
```

`scope` is matched as a **case-insensitive substring** of the option key, so
`scope=inward` returns every application-scoped option whose key contains
"inward". Omit `scope` to return all application-scoped options. Returns a JSON
array of `{key, type, scope, value}` (sensitive values are masked).

---

### GET `/api/config/{key}` — Read a single config option

```bash
GET /api/config/Enable%20Collecting%20Payments%20on%20Add%20Services%20%26%20Investigations%20on%20Inward
Header: Config: YOUR_CONFIG_API_KEY
```

Returns `{key, type, scope, value}` for the exact key, or HTTP 404 if not found.

---

### PUT `/api/config/{key}` — Update a config option value

```bash
PUT /api/config/Enable%20Collecting%20Payments%20on%20Add%20Services%20%26%20Investigations%20on%20Inward
Header: Config: YOUR_CONFIG_API_KEY
Content-Type: application/json

{"value":"true"}
```

Updates the option's value and immediately reloads the `@ApplicationScoped`
config cache (`loadApplicationOptions()`) so the change takes effect without a
restart. The option must already exist (this endpoint does not create new keys;
returns 404 otherwise). The change is recorded in the server log as a
`CONFIG_UPDATED` audit entry (user, key, old value, new value, timestamp).
`value` may be a JSON string (`"true"`) or a raw JSON scalar (`true`, `5`);
it is persisted as a string. Returns the updated `{key, type, scope, value}`.

---

### POST `/api/config/setBoolean/{key}/{value}` — Set a boolean config value

```bash
POST /api/config/setBoolean/Pharmacy%20Show%20Expiry%20Warning/true
Header: Config: YOUR_CONFIG_API_KEY
```

`{value}` must be `true` or `false`.

---

### POST `/api/config/setLongText/{key}/{value}` — Set a text config value

```bash
POST /api/config/setLongText/AI%20Chat%20-%20Claude%20Model/claude-sonnet-4-6
Header: Config: YOUR_CONFIG_API_KEY
```

---

### POST `/api/config/setInteger/{key}/{value}` — Set an integer config value

```bash
POST /api/config/setInteger/Pharmacy%20Low%20Stock%20Threshold/10
Header: Config: YOUR_CONFIG_API_KEY
```

---

### POST `/api/config/setShortText/{key}` — Create or update a short-text config value

```bash
POST /api/config/setShortText/Some%20Registration%20Number
Header: Config: YOUR_CONFIG_API_KEY
Content-Type: application/json

{"value":"PHSRC/ MC/357"}
```

Body-based so the value may contain spaces or slashes.

---

### POST `/api/config/setDouble/{key}` — Create or update a double config value

```bash
POST /api/config/setDouble/Some%20Multiplier
Header: Config: YOUR_CONFIG_API_KEY
Content-Type: application/json

{"value":"1.08"}
```

---

### GET `/api/config/search?keyword={keyword}` — Search config options by keyword

```bash
GET /api/config/search?keyword=expiry
Header: Config: YOUR_CONFIG_API_KEY
```

Returns a JSON array of `{key, type, value}` (sensitive values masked).

---

### GET `/api/config/inward-charge-types` — List inward charge types with their configurable label/order/group

```bash
GET /api/config/inward-charge-types
Header: Config: YOUR_CONFIG_API_KEY
```

---

## Notes

- The `{key}` is the config option name as stored in the database (URL-encode spaces as `%20`)
- To discover valid config keys, query the database: `SELECT key_name FROM config_option_application`
- The legacy `setBoolean`/`setLongText`/`setInteger` endpoints return HTTP 200 plain text on success; the JSON read/update endpoints return a JSON body. All return 401 on an invalid/expired/retired key, or a key whose type is not `Config`.
- **Authentication header is `Config`, not `Finance`** — the Config API key is separate, and every endpoint (read and write) requires the key type to be `Config`
