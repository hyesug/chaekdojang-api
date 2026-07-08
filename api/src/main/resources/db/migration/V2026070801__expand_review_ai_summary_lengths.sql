ALTER TABLE review_ai_summaries
    ALTER COLUMN one_line_review TYPE VARCHAR(180),
    ALTER COLUMN recommended_for TYPE VARCHAR(200),
    ALTER COLUMN impressive_point TYPE VARCHAR(300);
