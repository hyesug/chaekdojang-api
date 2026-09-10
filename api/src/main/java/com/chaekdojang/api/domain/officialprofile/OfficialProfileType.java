package com.chaekdojang.api.domain.officialprofile;

public enum OfficialProfileType {
    AUTHOR, PUBLISHER, BOOKSTORE, LIBRARY,
    /** 책도장 운영진이 직접 주최할 때 쓰는 프로필. 사용자가 신청할 수 없고 관리자가 직접 만든다. */
    PLATFORM
}
