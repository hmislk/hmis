---
name: performance-optimization
description: >
  Performance optimization patterns for the HMIS project. Use when optimizing slow
  queries, improving autocomplete performance, tuning database queries, reducing page
  load times, optimizing report generation, or investigating performance issues.
user-invocable: true
---

# Performance Optimization Guide

## Core Principles

1. **Use DTOs throughout** - Avoid converting to entities until necessary
2. **Cache autocomplete results** - Preserve full DTO data for converters
3. **Skip unnecessary operations** - Use configuration and conditional logic
4. **Defer expensive operations** - Move to background or later in workflow

## Autocomplete Optimization (85-90% improvement)

Key pattern from Pharmacy Retail Sale (639ms -> 50ms):

1. **Enhance DTO** with fields needed for lightweight entity creation
2. **Cache autocomplete results** in controller for converter reuse
3. **Skip discount calculations** when no discount scheme active
4. **Defer item instructions** loading to later in workflow

## Query Optimization

- Use **DTO constructor queries** instead of entity queries for display
- Always include `retired = false` in WHERE clauses
- Use **indexed columns** in WHERE and ORDER BY
- Add **pagination** to limit result sets
- Use **JPQL aggregation** (SUM, COUNT, GROUP BY) instead of Java loops

## MySQL Performance

- Enable slow query log for queries > 1 second
- Use `EXPLAIN` to analyze query plans
- Add indexes for frequently queried columns
- Consider covering indexes for common queries

## Report Optimization

- Use DTO queries for report data
- Pre-aggregate data in queries instead of Java
- Implement lazy loading for large datasets
- Cache report results when data doesn't change frequently

### 🚨 Found a slow report? File an issue

Whenever a report is found slow (during testing, an investigation or a user
complaint), file a GitHub issue for it then, without waiting to be asked. A
report that times out makes users think its data is missing. The issue asks for:

- a fast DTO-based report as the default button, and
- the existing report kept beside it as "(Legacy)" so the two can be
  cross-checked (as Stock Ledger does: "12. Stock Ledger" and "12. Stock Leger
  (Legacy)").

Record the menu path, the filters used and how long it took. No hospital data
in the issue. One issue per report; search for an existing one first.

For detailed guides, read:
- [Autocomplete Optimization](../../developer_docs/performance/autocomplete-optimization-guide.md)
- [MySQL Performance](../../developer_docs/database/mysql-performance-configuration.md)
- [Performance Plan](../../developer_docs/performance/PERFORMANCE_IMPROVEMENT_PLAN.md)
