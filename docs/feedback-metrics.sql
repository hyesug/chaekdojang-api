-- 도장 코멘트 지표 확인용 쿼리
-- FEEDBACK_DAILY_LIMIT_MEMBER=0이면 사용 제한은 꺼져 있고, 성공 이벤트만 사용량 후보로 기록됩니다.

-- 도장 코멘트 요청/성공/실패 수
SELECT
    event_type,
    COUNT(*) AS event_count
FROM metric_events
WHERE event_type IN ('feedback_requested', 'feedback_succeeded', 'feedback_failed', 'revision_saved')
GROUP BY event_type
ORDER BY event_type;

-- 수정 후 저장률 = revision_saved / feedback_succeeded
SELECT
    COUNT(*) FILTER (WHERE event_type = 'feedback_succeeded') AS feedback_succeeded,
    COUNT(*) FILTER (WHERE event_type = 'revision_saved') AS revision_saved,
    ROUND(
        COUNT(*) FILTER (WHERE event_type = 'revision_saved')::numeric
        / NULLIF(COUNT(*) FILTER (WHERE event_type = 'feedback_succeeded'), 0),
        4
    ) AS revision_save_rate
FROM metric_events
WHERE event_type IN ('feedback_succeeded', 'revision_saved');

-- 회원별 오늘 성공 횟수(KST 기준). 나중에 일일 제한을 켤 때 점검용.
SELECT
    user_id,
    COUNT(*) AS today_feedback_succeeded
FROM metric_events
WHERE event_type = 'feedback_succeeded'
  AND created_at >= (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')::date
GROUP BY user_id
ORDER BY today_feedback_succeeded DESC;
