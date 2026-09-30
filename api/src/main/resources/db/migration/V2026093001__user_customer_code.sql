ALTER TABLE users ADD COLUMN IF NOT EXISTS customer_code VARCHAR(32);

CREATE SEQUENCE IF NOT EXISTS user_customer_code_seq;

WITH numbered_users AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, id) AS sequence_number
    FROM users
    WHERE customer_code IS NULL
)
UPDATE users u
SET customer_code = 'CD-' || LPAD(numbered_users.sequence_number::TEXT, 6, '0')
FROM numbered_users
WHERE u.id = numbered_users.id;

SELECT setval(
    'user_customer_code_seq',
    GREATEST(COALESCE((SELECT COUNT(*) FROM users), 0) + 1, 1),
    false
);

CREATE OR REPLACE FUNCTION assign_user_customer_code()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.customer_code IS NULL THEN
        NEW.customer_code := 'CD-' || LPAD(nextval('user_customer_code_seq')::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_assign_user_customer_code ON users;
CREATE TRIGGER trg_assign_user_customer_code
    BEFORE INSERT ON users
    FOR EACH ROW EXECUTE FUNCTION assign_user_customer_code();

ALTER TABLE users ALTER COLUMN customer_code SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_customer_code UNIQUE (customer_code);
