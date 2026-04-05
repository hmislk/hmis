# System Configuration API

Base path: `/api/config`
Authentication: `Config` header (**not** `Finance` — this is a separate key)
Content-Type: `text/plain`

Used to set application configuration options at runtime without redeployment.

## Endpoints

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
