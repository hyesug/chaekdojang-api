package com.chaekdojang.api.domain.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final StorageProperties storageProperties;

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    public UploadResult uploadProfileImage(MultipartFile file) throws IOException {
        validateImage(file.getContentType(), file.getSize());

        String ext = getExtension(file.getOriginalFilename(), file.getContentType());
        String key = buildProfileImageKey(ext);

        if (storageProperties.isS3()) {
            putS3Object(key, file);
            return new UploadResult(key, buildS3PublicUrl(key));
        }

        Path uploadPath = Paths.get(storageProperties.getLocal().getUploadDir()).toAbsolutePath();
        Files.createDirectories(uploadPath);
        String filename = key.substring(key.lastIndexOf('/') + 1);
        file.transferTo(uploadPath.resolve(filename).toFile());

        String publicPath = trimTrailingSlash(storageProperties.getLocal().getPublicPath());
        return new UploadResult(key, trimTrailingSlash(backendUrl) + publicPath + "/" + filename);
    }

    public PresignedUploadResult createProfileImagePresignedUrl(String fileName, String contentType, long size) {
        if (!storageProperties.isS3()) {
            throw new IllegalArgumentException("S3 storage is not enabled.");
        }

        validateImage(contentType, size);
        String key = buildProfileImageKey(getExtension(fileName, contentType));

        StorageProperties.S3 s3 = storageProperties.getS3();
        Region region = Region.of(s3.getRegion());
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(requireBucket())
                .key(key)
                .contentType(contentType)
                .build();

        try (S3Presigner presigner = S3Presigner.builder().region(region).build()) {
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(s3.getPresignedUrlExpirationMinutes()))
                    .putObjectRequest(putObjectRequest)
                    .build();

            return new PresignedUploadResult(
                    key,
                    presigner.presignPutObject(presignRequest).url().toString(),
                    buildS3PublicUrl(key),
                    s3.getPresignedUrlExpirationMinutes()
            );
        }
    }

    private void putS3Object(String key, MultipartFile file) throws IOException {
        StorageProperties.S3 s3 = storageProperties.getS3();
        try (S3Client s3Client = S3Client.builder().region(Region.of(s3.getRegion())).build()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(requireBucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        }
    }

    private void validateImage(String contentType, long size) {
        if (size <= 0) {
            throw new IllegalArgumentException("File is empty.");
        }
        if (size > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size must be 5MB or less.");
        }
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPG, PNG, WEBP, and GIF images are allowed.");
        }
    }

    private String buildProfileImageKey(String ext) {
        String prefix = trimSlashes(storageProperties.getS3().getProfileImagePrefix());
        if (!storageProperties.isS3()) {
            prefix = "";
        }
        String filename = UUID.randomUUID() + "." + ext;
        return prefix.isBlank() ? filename : prefix + "/" + filename;
    }

    private String getExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            if (Set.of("jpg", "jpeg", "png", "webp", "gif").contains(ext)) {
                return ext.equals("jpeg") ? "jpg" : ext;
            }
        }
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "jpg";
        };
    }

    private String buildS3PublicUrl(String key) {
        StorageProperties.S3 s3 = storageProperties.getS3();
        if (s3.getPublicBaseUrl() != null && !s3.getPublicBaseUrl().isBlank()) {
            return trimTrailingSlash(s3.getPublicBaseUrl()) + "/" + key;
        }
        return "https://" + requireBucket() + ".s3." + s3.getRegion() + ".amazonaws.com/" + key;
    }

    private String requireBucket() {
        String bucket = storageProperties.getS3().getBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 upload bucket is required.");
        }
        return bucket;
    }

    private String trimTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String trimSlashes(String value) {
        if (value == null) return "";
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    public record UploadResult(String key, String url) {
    }

    public record PresignedUploadResult(String key, String uploadUrl, String publicUrl, long expiresInMinutes) {
    }
}
