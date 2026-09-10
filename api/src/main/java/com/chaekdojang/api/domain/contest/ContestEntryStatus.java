package com.chaekdojang.api.domain.contest;

public enum ContestEntryStatus {
    SUBMITTED,   // 응모 완료
    WITHDRAWN,   // 응모 취소
    AWARDED,     // 수상
    NOT_AWARDED  // 미수상 (발표 시점에 확정)
}
