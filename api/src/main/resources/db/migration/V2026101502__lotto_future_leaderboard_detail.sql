-- 순위표 규칙의 그 회차 설명(조합한 체계·고른 식 등). 기존 행은 기록 이전 값으로 NULL 그대로 둔다.
ALTER TABLE lotto_future_leaderboard_entries ADD COLUMN detail VARCHAR(2000);
