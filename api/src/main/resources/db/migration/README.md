# Flyway migration workspace

`V1__init_schema.sql` was exported from the current production PostgreSQL 17
schema with `pg_dump --schema-only --no-owner --no-privileges`.

Production was already running before Flyway was introduced, so production
must use `baseline-on-migrate` for the first Flyway run. New empty databases
can run `V1__init_schema.sql` normally.

Rules:

- Do not write `DROP`, `TRUNCATE`, or data-deleting `DELETE` statements.
- Add future schema changes as `V2__...`, `V3__...`, and so on.
- Keep production `SPRING_FLYWAY_ENABLED=true`.
- Keep production `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` for the initial rollout.
- Keep production `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`.

