# Cache Warmup API

Warms the EclipseLink shared (L2) entity cache after a deployment or restart, so the first pharmacy bills opened afterwards do not load each `Stock`, `ItemBatch` and `Item` one by one.

Authentication: `Finance` header with a valid API key (same as the other APIs).

## GET /api/warmup/stocks

Loads every non-retired `Stock` with `stock > 0`, together with its `ItemBatch` and `Item`, in one JPQL query with `JOIN FETCH`. The entities are not returned.

```
curl -s -H "Finance: <key>" "http://localhost:9090/rh/api/warmup/stocks"
```

```json
{"status":"success","code":200,"data":{"stocksLoaded":1234,"timeTakenMs":4200}}
```

An invalid or missing key returns `{"status":"error","code":401,"message":"Not a valid key"}`.

A post-deploy script can call this once the application is up. The first call on a cold cache is the slow one; later calls are much faster because the entities are already cached.
