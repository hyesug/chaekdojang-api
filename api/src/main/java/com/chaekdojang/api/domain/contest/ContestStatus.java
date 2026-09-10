package com.chaekdojang.api.domain.contest;

public enum ContestStatus {
    DRAFT,     // 작성 중 (독자에게 안 보임)
    OPEN,      // 응모 접수 중
    CLOSED,    // 접수 마감, 심사 중
    ANNOUNCED  // 수상 발표 완료
}
