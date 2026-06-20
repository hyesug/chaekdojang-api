package com.chaekdojang.api.domain.upload.dto;

import com.chaekdojang.api.domain.upload.UploadService;

public record PresignedUploadResponse(
        String key,
        String uploadUrl,
        String publicUrl,
        long expiresInMinutes
) {
    public static PresignedUploadResponse from(UploadService.PresignedUploadResult result) {
        return new PresignedUploadResponse(
                result.key(),
                result.uploadUrl(),
                result.publicUrl(),
                result.expiresInMinutes()
        );
    }
}
