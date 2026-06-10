-- ============================================================================
-- Migration: widen voucher id columns to VARCHAR(12)
--
-- WHY
--   Voucher ids used to be 'JV' + 4-digit year + 4-digit sequence = 10 chars in
--   J_ID CHAR(10). The 4-digit sequence ran out past 9,999: voucher 10,000 in a
--   year made an 11-char id and broke the fixed-width primary key. The sequence
--   is now 6 digits ('JV' + year + 6-digit sequence, e.g. JV2026000001 = 12
--   chars), good for up to 999,999 vouchers a year.
--
-- WHAT THIS DOES
--   Widens J_ID and everything that points at it (REVERSED_BY, REVERSES,
--   JournalDetail.J_ID) from CHAR(10) to VARCHAR(12).
--
-- EXISTING VOUCHER NUMBERS STAY PUT
--   We don't rewrite any existing id. Old 10-char ids stay as-is; new ones are
--   12 chars. They can't collide since the lengths differ, and the
--   JournalSequence counter only goes up, so no sequence number gets reused. No
--   backfill needed.
--
-- Run this ONCE against any DB created before the 6-digit change.
-- Fresh databases get the right widths straight from schema.sql.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- PostgreSQL (Docker / production)
-- ---------------------------------------------------------------------------
ALTER TABLE JournalDetail ALTER COLUMN J_ID        TYPE VARCHAR(12);
ALTER TABLE JournalMaster ALTER COLUMN J_ID        TYPE VARCHAR(12);
ALTER TABLE JournalMaster ALTER COLUMN REVERSED_BY TYPE VARCHAR(12);
ALTER TABLE JournalMaster ALTER COLUMN REVERSES    TYPE VARCHAR(12);

-- ---------------------------------------------------------------------------
-- SQLite (default local profile)
--   SQLite uses TEXT affinity and ignores CHAR(n) length, so old data and longer
--   ids are already stored without truncation. No DDL change needed; the 6-digit
--   format just works. schema.sql says VARCHAR(12) only for documentation and to
--   match PostgreSQL. Nothing to do for SQLite databases.
-- ---------------------------------------------------------------------------
