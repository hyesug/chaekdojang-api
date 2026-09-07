package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.dojangdan.CampaignEbookFile;
import com.chaekdojang.api.domain.dojangdan.EbookAccessGrant;

import java.time.LocalDateTime;

public record MyEbookAccessResponse(
        Long grantId,
        LocalDateTime grantedAt,
        LocalDateTime expiresAt,
        boolean readable,
        boolean expired,
        boolean revoked,
        int openCount,
        LocalDateTime firstOpenedAt,
        String originalFilename,
        Integer pageCount,
        Long byteSize
) {
    public static MyEbookAccessResponse of(EbookAccessGrant grant, CampaignEbookFile file,
                                           LocalDateTime now) {
        return new MyEbookAccessResponse(
                grant.getId(),
                grant.getGrantedAt(),
                grant.getExpiresAt(),
                grant.isReadable(now) && file != null,
                grant.isExpired(now),
                grant.isRevoked(),
                grant.getOpenCount(),
                grant.getFirstOpenedAt(),
                file == null ? null : file.getOriginalFilename(),
                file == null ? null : file.getPageCount(),
                file == null ? null : file.getByteSize()
        );
    }
}
