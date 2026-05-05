---
name: database-guide
description: >
  MySQL database development guide for the HMIS project. Use when working with database
  queries, SQL debugging, database schema investigation, MySQL performance tuning,
  credential management, or database connection troubleshooting.
user-invocable: true
---

# MySQL Developer Guide

## Security Policy

- MySQL credentials MUST NEVER be committed to git
- Credentials stored in separate folder outside the project directory
- Windows: `C:\Credentials\credentials.txt`
- Linux/Mac: `~/.config/hmis/credentials.txt`

## Common Database Operations

### Structure Investigation
```sql
SHOW TABLES LIKE '%bill%';
DESCRIBE bill;
SHOW CREATE TABLE bill;
SELECT COUNT(*) FROM bill WHERE retired = 0;
```

### JPQL Query Patterns
```java
// Standard entity query
String jpql = "SELECT b FROM Bill b WHERE b.retired = false AND b.department = :dept";

// DTO constructor query (use findLightsByJpql)
String jpql = "SELECT new com.divudi.core.data.dto.MyDTO(b.id, b.name) FROM Bill b WHERE ...";
List<MyDTO> results = (List<MyDTO>) facade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

// Aggregate query
String jpql = "SELECT SUM(b.netTotal) FROM Bill b WHERE ...";
```

### Performance Tips
- Always include `b.retired = false` in queries
- Use indexed columns in WHERE clauses
- Use DTOs instead of full entities for display
- Limit result sets with pagination

## Local Development Databases

Each developer machine may have different Payara JDBC pool names and JNDI names. Common local databases:

- **`rh`** — primary dev database (JNDI typically `jdbc/rhDS`)
- **`ruhunu`** — useful as a testing environment with richer data coverage

When a restored database has **lowercase table names**, it must be converted to uppercase before the app can use it. Generate and run the rename script:
```sql
SELECT CONCAT('RENAME TABLE `', table_name, '` TO `temp_', table_name, '`; ',
              'RENAME TABLE `temp_', table_name, '` TO `', UPPER(table_name), '`;')
FROM information_schema.tables
WHERE table_schema = '<db_name>' AND table_name != UPPER(table_name);
```
Then pipe the output into mysql for that database. See `src/main/webapp/resources/sql/UpperCase.sql` for reference (note: that file targets a specific schema — always generate fresh from the actual database).

## Persistence Configuration

- **Development**: Use hardcoded JNDI (machine-specific — check your local Payara setup)
- **Before push**: Must use environment variables (`${JDBC_DATASOURCE}`)

For complete reference, read [developer_docs/database/mysql-developer-guide.md](../../../developer_docs/database/mysql-developer-guide.md).
