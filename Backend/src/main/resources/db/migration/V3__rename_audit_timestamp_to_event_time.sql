-- ============================================================================
-- Migration: rename audit_log.TIMESTAMP to audit_log.EVENT_TIME
--
-- WHY
--   The audit column was called TIMESTAMP. That's both a reserved word and a
--   type in MySQL, so you couldn't create or write the column without
--   back-quoting it everywhere, and on MySQL the audit insert failed flat out.
--   Renaming it to EVENT_TIME (not reserved) makes the same DDL and DML work on
--   SQLite, MySQL and PostgreSQL with no quoting.
--
-- WHAT THIS DOES
--   Renames the one column audit_log.TIMESTAMP -> audit_log.EVENT_TIME. Stored
--   values stay as-is (still ISO-8601 text).
--
-- Run this ONCE against any DB whose audit_log table predates this change. Fresh
-- databases get EVENT_TIME straight from schema.sql / schema-mysql.sql. The new
-- MySQL DDL also moves audit_log.ID and period_lock.ID to BIGINT AUTO_INCREMENT;
-- a MySQL DB built from the old SQLite-flavoured DDL never managed to create
-- those tables at all, so there's nothing to convert there.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- MySQL (Docker / production)
-- ---------------------------------------------------------------------------
ALTER TABLE audit_log CHANGE TIMESTAMP EVENT_TIME VARCHAR(30) NOT NULL;

-- ---------------------------------------------------------------------------
-- PostgreSQL
-- ---------------------------------------------------------------------------
-- ALTER TABLE audit_log RENAME COLUMN TIMESTAMP TO EVENT_TIME;

-- ---------------------------------------------------------------------------
-- SQLite (default local profile) — needs SQLite >= 3.25
--   schema.sql uses CREATE TABLE IF NOT EXISTS, so an existing FA.db hangs on to
--   its old TIMESTAMP column. Run this once so audit inserts (which now write
--   EVENT_TIME) work.
-- ---------------------------------------------------------------------------
-- ALTER TABLE audit_log RENAME COLUMN "TIMESTAMP" TO EVENT_TIME;
