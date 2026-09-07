package com.chaekdojang.api.domain.dojangdan.dto;

/**
 * 독자별 서평단 이력. 명세 §3 완주율 지표.
 * 신청/선정/제출 시각에서 계산하므로 별도 집계 테이블 없이 산출한다.
 */
public record ReaderTrackRecordResponse(
        long appliedCount,          // 신청 횟수
        long selectedCount,         // 선정 횟수
        long submittedCount,        // 독후감 제출 횟수
        Integer completionRate,     // 완주율(%) = 제출 / 선정. 선정 이력이 없으면 null
        Integer averageReviewLength,// 평균 독후감 글자수
        Double averageDaysToSubmit, // 평균 제출 소요일 (선정 → 제출)
        Integer onTimeRate          // 마감 준수율(%). 제출 이력이 없으면 null
) {
    public static ReaderTrackRecordResponse empty() {
        return new ReaderTrackRecordResponse(0, 0, 0, null, null, null, null);
    }
}
