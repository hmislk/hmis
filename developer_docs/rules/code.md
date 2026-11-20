# Code rules (detail)

Moved from the root CLAUDE.md; each rule there links here. The rules are unchanged.

- Line endings: they differ by machine (many files are CRLF on Windows). Edit with the Edit tool; if you script a replacement, match the file's line endings and check the diff for whole-file changes.
- COUNT queries: use `findLongByJpql`, never `findDoubleByJpql` (it silently returns 0.0).
- Date-time ranges: `findByJpql(jpql, params)` binds every `Date` as `TemporalType.DATE`, so an end-of-day `toDate` becomes 00:00 and today's rows vanish (#25526). Pass `TemporalType.TIMESTAMP`. If running the query fails, the `TemporalType` overload throws, where the 2-arg one catches it and returns an empty list; when you switch an existing caller, check what its callers do with an exception (#25778).
- create() vs edit(): `edit()` merges a copy and leaves the object without an id, so every later save of it, or of anything pointing to it, inserts another row (#25200: one cancel wrote 7 bills).
- Id lists: EclipseLink serialises the entities for `b in :bills`, so the query silently matches nothing for a root entity. Use `b.id in :billIds`.
- Business errors from a `@Stateless` service: a plain `IllegalStateException`/`IllegalArgumentException` reaches the JSF controller or REST class as `EJBException`, so its `catch` never fires and the user sees no message. Throw an `@ApplicationException(rollback = true)` exception.
