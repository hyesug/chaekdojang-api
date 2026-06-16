# Flyway migration workspace

This directory is reserved for Flyway migrations.

Do not create a real `V1__init_schema.sql` by guessing from JPA entities.
The first migration must be based on the current production schema exported with:

```bash
pg_dump --schema-only --no-owner --no-privileges "$DATABASE_URL" > V1__init_schema.sql
```

Rules:

- Do not write `DROP`, `TRUNCATE`, or data-deleting `DELETE` statements.
- Review the dump before committing it.
- Keep `spring.flyway.enabled=false` until the initial schema file is reviewed and local verification passes.
- Keep production `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`.

