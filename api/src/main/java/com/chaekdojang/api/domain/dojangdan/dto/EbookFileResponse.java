package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.dojangdan.CampaignEbookFile;

import java.time.LocalDateTime;

public record EbookFileResponse(
        Long id,
        String originalFilename,
        long byteSize,
        Integer pageCount,
        LocalDateTime uploadedAt
) {
    public static EbookFileResponse from(CampaignEbookFile file) {
        return new EbookFileResponse(
                file.getId(),
                file.getOriginalFilename(),
                file.getByteSize(),
                file.getPageCount(),
                file.getUploadedAt()
        );
    }
}
