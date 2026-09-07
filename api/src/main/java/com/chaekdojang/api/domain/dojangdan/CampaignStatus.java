package com.chaekdojang.api.domain.dojangdan;

public enum CampaignStatus {
    DRAFT,      // 작성 중 (독자에게 안 보임)
    RECRUITING, // 모집 중
    CLOSED,     // 모집 마감, 선정 전
    SELECTED,   // 선정 완료, 독후감 작성 기간
    COMPLETED   // 캠페인 종료
}
