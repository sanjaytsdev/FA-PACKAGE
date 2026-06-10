-- ============================================================================
-- Migration: switch fixed-width CHAR codes/descriptions to VARCHAR
--
-- WHY
--   Code and description columns were CHAR(n). CHAR right-pads values with
--   spaces to the full width. On engines that hand that padding back (PostgreSQL,
--   H2), an account read as 'Cash' + 46 spaces no longer equals 'Cash', so
--   callers and tests had to .trim() everything and lookups/equality by code
--   broke. VARCHAR stores exactly what you wrote, so nothing comes back padded.
--
-- WHAT THIS DOES
--   Widens the free-text and id columns to VARCHAR and trims any padding already
--   sitting in existing rows:
--       FAGroup.A_CODE, FAGroup.A_DESC
--       FASubGroup.S_CODE, FASubGroup.S_DESC, FASubGroup.A_CODE, FASubGroup.S_TYPE
--       JournalMaster.J_NARR
--       JournalDetail.J_CODE
--   The enum/flag columns (A_TYPE, S_FLAG, S_DRCR, J_DRCR, J_DOC) always fill
--   their width, so they never pad and stay CHAR.
--
-- Run this ONCE against any DB created before this change.
-- Fresh databases get the right types straight from schema.sql.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- MySQL (Docker / production)
--   Retyping columns that are part of a foreign key means turning off the FK
--   check while the ALTERs run. MySQL already drops trailing spaces from CHAR on
--   read, but moving to VARCHAR and trimming makes the stored bytes match what
--   you get back.
-- ---------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE FAGroup      MODIFY A_CODE VARCHAR(2);
ALTER TABLE FAGroup      MODIFY A_DESC VARCHAR(50);
ALTER TABLE FASubGroup   MODIFY S_CODE VARCHAR(5);
ALTER TABLE FASubGroup   MODIFY S_DESC VARCHAR(50);
ALTER TABLE FASubGroup   MODIFY A_CODE VARCHAR(2);
ALTER TABLE FASubGroup   MODIFY S_TYPE VARCHAR(2);
ALTER TABLE JournalMaster MODIFY J_NARR VARCHAR(100);
ALTER TABLE JournalDetail MODIFY J_CODE VARCHAR(5);

UPDATE FAGroup      SET A_CODE = TRIM(TRAILING ' ' FROM A_CODE), A_DESC = TRIM(TRAILING ' ' FROM A_DESC);
UPDATE FASubGroup   SET S_CODE = TRIM(TRAILING ' ' FROM S_CODE), S_DESC = TRIM(TRAILING ' ' FROM S_DESC),
                        A_CODE = TRIM(TRAILING ' ' FROM A_CODE), S_TYPE = TRIM(TRAILING ' ' FROM S_TYPE);
UPDATE JournalMaster SET J_NARR = TRIM(TRAILING ' ' FROM J_NARR) WHERE J_NARR IS NOT NULL;
UPDATE JournalDetail SET J_CODE = TRIM(TRAILING ' ' FROM J_CODE);

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- PostgreSQL
--   Casting char(n) -> varchar(n) drops trailing blanks on its own, and we cast
--   both sides of each foreign key so the constraints stay valid (char(2) and
--   varchar(2) compare as text). USING rtrim(...) spells out the removal of any
--   padding that's already there.
-- ---------------------------------------------------------------------------
-- ALTER TABLE FAGroup      ALTER COLUMN A_CODE TYPE VARCHAR(2)  USING rtrim(A_CODE);
-- ALTER TABLE FAGroup      ALTER COLUMN A_DESC TYPE VARCHAR(50) USING rtrim(A_DESC);
-- ALTER TABLE FASubGroup   ALTER COLUMN S_CODE TYPE VARCHAR(5)  USING rtrim(S_CODE);
-- ALTER TABLE FASubGroup   ALTER COLUMN S_DESC TYPE VARCHAR(50) USING rtrim(S_DESC);
-- ALTER TABLE FASubGroup   ALTER COLUMN A_CODE TYPE VARCHAR(2)  USING rtrim(A_CODE);
-- ALTER TABLE FASubGroup   ALTER COLUMN S_TYPE TYPE VARCHAR(2)  USING rtrim(S_TYPE);
-- ALTER TABLE JournalMaster ALTER COLUMN J_NARR TYPE VARCHAR(100) USING rtrim(J_NARR);
-- ALTER TABLE JournalDetail ALTER COLUMN J_CODE TYPE VARCHAR(5)  USING rtrim(J_CODE);

-- ---------------------------------------------------------------------------
-- SQLite (default local profile)
--   SQLite uses TEXT affinity and never pads CHAR(n), so old data has no trailing
--   spaces and no DDL change is needed. The VARCHAR declarations in schema.sql
--   are just for documentation and to match the other engines.
-- ---------------------------------------------------------------------------
