package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.dojangdan.ConsentDisplayNameType;

public record ConsentUpdateRequest(
        boolean consentPromotional,
        boolean consentExcerpt,
        ConsentDisplayNameType displayNameType
) {
}
