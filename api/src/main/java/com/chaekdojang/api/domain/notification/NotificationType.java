package com.chaekdojang.api.domain.notification;

public enum NotificationType {
    LIKE,    // 좋아요
    COMMENT, // 댓글
    FOLLOW,  // 팔로우
    SAME_BOOK_REVIEW, // 내가 읽은 책의 새 독후감
    GROUP_JOIN_REQUEST, // 내가 만든 모임 가입 신청
    GROUP_JOINED, // 내가 만든 모임 바로 가입
    GROUP_JOIN_APPROVED, // 내 독서모임 가입 승인
    REVIEW_CONTINUED, // 내 독후감을 읽고 새 독후감 작성
    CAMPAIGN_SELECTED, // 서평단 선정
    CAMPAIGN_REJECTED, // 서평단 미선정
    CAMPAIGN_INVITED, // 관심 등록한 출판사·작가의 새 서평단 우선 초대
    CONTEST_AWARDED, // 공모전 수상
    CONTEST_NOT_AWARDED // 공모전 미수상
}
