-- Schema changes for the profile API (GET/PUT /api/v1/user/profile).
--
-- Hibernate runs with ddl-auto: update, which ADDS columns but will never relax a
-- constraint that already exists. It therefore created keycloak_id, phone and
-- country by itself, but left password NOT NULL - and profiles provisioned from a
-- Keycloak login have no password, because Keycloak holds the credential. Every
-- first visit to /profile failed with a not-null violation until this ran.
--
-- Apply once per environment, against the user-service database:
--
--   docker exec postgres psql -U postgres -d user_db -f - < 2026-08-05-profile-columns.sql
--
-- Applied to production on 2026-08-05.

ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
