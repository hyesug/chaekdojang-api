-- 운세 프로필을 사용자당 여러 개 저장한다 (본인 하나 + 가족·지인).
-- 지금까지 저장된 행은 전부 그 사람의 '내 프로필'이다.

-- user_id 의 UNIQUE 제약을 뗀다. 이 표의 UNIQUE 는 그것 하나뿐이고, 자동으로 붙은 이름을
-- 짐작하지 않도록 이름을 찾아서 지운다.
DO $$
DECLARE c text;
BEGIN
    FOR c IN SELECT conname FROM pg_constraint
             WHERE conrelid = 'fortune_profiles'::regclass AND contype = 'u'
    LOOP
        EXECUTE 'ALTER TABLE fortune_profiles DROP CONSTRAINT ' || quote_ident(c);
    END LOOP;
END $$;

ALTER TABLE fortune_profiles ADD COLUMN is_self BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE fortune_profiles SET is_self = TRUE;
CREATE INDEX idx_fortune_profiles_user ON fortune_profiles (user_id);
-- 본인 프로필은 사용자당 하나
CREATE UNIQUE INDEX uq_fortune_profiles_self ON fortune_profiles (user_id) WHERE is_self;
