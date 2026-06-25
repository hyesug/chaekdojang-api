package com.chaekdojang.api.domain.notification;

public enum NotificationType {
    LIKE,    // 좋아요
    COMMENT, // 댓글
    FOLLOW,  // 팔로우
    SAME_BOOK_REVIEW, // 내가 읽은 책의 새 독후감
    GROUP_JOIN_REQUEST, // 내가 만든 모임 가입 신청
    GROUP_JOINED, // 내가 만든 모임 바로 가입
    GROUP_JOIN_APPROVED // 내 독서모임 가입 승인
}
