-- 운세 결과 칸마다 받는 피드백(👍/👎 + 선택 한 줄).
-- 생년월일·이름은 저장하지 않는다 — 칸 이름과 그 칸에 보였던 문장(이름을 지운 것)만 남겨
-- "어느 해석 문장이 자주 틀리는가"를 규칙 단위로 본다.
CREATE TABLE fortune_feedback (
    id          BIGSERIAL PRIMARY KEY,
    mode        VARCHAR(8)   NOT NULL,
    section     VARCHAR(80)  NOT NULL,
    verdict     VARCHAR(8)   NOT NULL,
    snippet     VARCHAR(600),
    comment     VARCHAR(300),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fortune_feedback_section ON fortune_feedback (section, verdict);
CREATE INDEX idx_fortune_feedback_created ON fortune_feedback (created_at);
