package com.chaekdojang.api.domain.upload;

import com.chaekdojang.api.domain.upload.dto.PresignedUploadRequest;
import com.chaekdojang.api.domain.upload.dto.PresignedUploadResponse;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Tag(name = "Upload", description = "File upload")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;
    private final UserRepository userRepository;

    @Operation(summary = "Upload profile image", description = "Uploads a profile image, saves it to my profile, and returns its public URL. JWT required.")
    @PostMapping("/profile-image")
    @Transactional
    public ApiResponse<Map<String, String>> uploadProfileImage(
            @RequestParam("file") MultipartFile file) throws IOException {
        UploadService.UploadResult result = uploadService.uploadProfileImage(file);
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.updateProfile(null, null, result.url());
        return ApiResponse.ok(Map.of("url", result.url(), "key", result.key()));
    }

    @Operation(summary = "Create S3 presigned upload URL", description = "Creates a direct S3 upload URL for a profile image. JWT required.")
    @PostMapping("/profile-image/presigned-url")
    public ApiResponse<PresignedUploadResponse> createProfileImagePresignedUrl(
            @Valid @RequestBody PresignedUploadRequest request) {
        return ApiResponse.ok(PresignedUploadResponse.from(
                uploadService.createProfileImagePresignedUrl(request.fileName(), request.contentType(), request.size())
        ));
    }
}
