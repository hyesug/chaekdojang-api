ALTER TABLE public.contests
    ADD CONSTRAINT contests_status_check
    CHECK (status IN ('DRAFT', 'OPEN', 'CLOSED', 'ANNOUNCED'));

ALTER TABLE public.contests
    ADD CONSTRAINT contests_entry_type_check
    CHECK (entry_type IN ('REVIEW', 'TEXT'));

ALTER TABLE public.contest_entries
    ADD CONSTRAINT contest_entries_status_check
    CHECK (status IN ('SUBMITTED', 'WITHDRAWN', 'AWARDED', 'NOT_AWARDED'));
