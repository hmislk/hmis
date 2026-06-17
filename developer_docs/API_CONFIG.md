# System Configuration API

Base path: `/api/config`
Authentication: `Config` header (**not** `Finance` — this is a separate key)
Content-Type: `text/plain`

Used to set application configuration options at runtime without redeployment.

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

## Notes

- The `{key}` is the config option name as stored in the database (URL-encode spaces as `%20`)
- To discover valid config keys, query the database: `SELECT key_name FROM config_option_application`
- Returns HTTP 200 plain text `"Success"` on success, 401 on invalid key
- **Authentication header is `Config`, not `Finance`** — the Config API key is separate
