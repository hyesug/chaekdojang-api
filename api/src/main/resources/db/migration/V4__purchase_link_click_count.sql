ALTER TABLE public.purchase_links
    ADD COLUMN IF NOT EXISTS click_count bigint NOT NULL DEFAULT 0;
