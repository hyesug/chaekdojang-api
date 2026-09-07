package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.dojangdan.ConsentDisplayNameType;
import jakarta.validation.constraints.Size;

public record CampaignApplyRequest(
        @Size(max = 2000) String message,

        /** (필수) 서평단 참여 및 독후감 작성 약관 동의 */
        boolean agreeTerms,

        /** (선택) 출판사·작가의 홍보 활용 동의 */
        boolean consentPromotional,

        /** (선택) 일부 발췌·편집 허용 */
        boolean consentExcerpt,

        /** 표기 방식. 비어 있으면 닉네임 표기로 본다. */
        ConsentDisplayNameType displayNameType,

        /** (선택, 기본 켜짐) 선정되지 않아도 이 출판사·작가의 다음 책 소식 받기 */
        Boolean followIntent
) {
    public boolean wantsFollowIntent() {
        return followIntent == null || followIntent;
    }
}
