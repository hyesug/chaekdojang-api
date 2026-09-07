package com.chaekdojang.api.domain.dojangdan.dto;

import java.util.List;

/**
 * 출판사에게 보여줄 관심 독자 집계.
 * 개별 독자를 식별할 수 있는 정보(이메일·연락처·닉네임 목록)는 담지 않는다.
 */
public record ProfileAudienceResponse(
        Long profileId,
        String profileName,
        long interestedReaderCount,
        long campaignExperiencedCount, // 이 중 서평단에 선정된 적 있는 독자 수
        List<CategoryCount> topCategories
) {
    public record CategoryCount(String category, long count) {
    }
}
