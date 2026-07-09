package com.chaekdojang.api.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "독후감을 찾을 수 없습니다."),
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST, "자기 자신을 팔로우할 수 없습니다."),
    FOLLOW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 팔로우한 사용자입니다."),
    FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "팔로우 관계가 존재하지 않습니다."),
    LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 좋아요한 독후감입니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요 기록이 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    LIBRARY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 서재에 있는 책입니다."),
    LIBRARY_NOT_FOUND(HttpStatus.NOT_FOUND, "서재 항목을 찾을 수 없습니다."),
    BOOKMARK_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 저장한 독후감입니다."),
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "저장 기록이 없습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    TRANSLATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "번역에 실패했습니다."),
    FEEDBACK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "도장 코멘트를 만들지 못했습니다. 잠시 후 다시 시도해주세요."),
    FEEDBACK_DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "도장 코멘트 기능을 잠시 이용할 수 없습니다."),
    FEEDBACK_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "오늘 도장 코멘트를 모두 사용했습니다."),
    SUBSCRIPTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 구독 중입니다."),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "구독 정보를 찾을 수 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.");

    private final HttpStatus status;
    private final String message;
}
