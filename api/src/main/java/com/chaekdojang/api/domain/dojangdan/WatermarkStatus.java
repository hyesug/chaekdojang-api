package com.chaekdojang.api.domain.dojangdan;

public enum WatermarkStatus {
    PENDING, // 아직 만들지 않음
    READY,   // 워터마크 파일 준비됨
    FAILED   // 생성 실패 (원본으로 대체 제공하지 않고 재시도한다)
}
