package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.upload.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 서평단 전자책 저장소.
 *
 * 전자책은 공개 경로(/uploads/**)에 절대 두지 않는다.
 * 로컬이든 S3든 파일을 여는 유일한 통로는 권한을 확인하는 서버 엔드포인트다.
 */
@Service
@RequiredArgsConstructor
public class EbookStorageService {

    private final StorageProperties storageProperties;

    public String putOriginal(byte[] content, String extension) {
        return put("originals/" + java.util.UUID.randomUUID() + "." + extension, content);
    }

    public String putWatermarked(Long grantId, byte[] content) {
        return put("watermarked/grant-" + grantId + ".pdf", content);
    }

    public byte[] get(String storageKey) {
        if (storageProperties.isS3()) {
            StorageProperties.S3 s3 = storageProperties.getS3();
            try (S3Client client = S3Client.builder().region(Region.of(s3.getRegion())).build()) {
                ResponseBytes<GetObjectResponse> object = client.getObjectAsBytes(
                        GetObjectRequest.builder()
                                .bucket(requireBucket())
                                .key(s3Key(storageKey))
                                .build());
                return object.asByteArray();
            }
        }

        try {
            return Files.readAllBytes(localPath(storageKey));
        } catch (IOException e) {
            throw new UncheckedIOException("전자책 파일을 읽지 못했습니다: " + storageKey, e);
        }
    }

    private String put(String storageKey, byte[] content) {
        if (storageProperties.isS3()) {
            StorageProperties.S3 s3 = storageProperties.getS3();
            try (S3Client client = S3Client.builder().region(Region.of(s3.getRegion())).build()) {
                client.putObject(
                        PutObjectRequest.builder()
                                .bucket(requireBucket())
                                .key(s3Key(storageKey))
                                .contentType("application/pdf")
                                .contentLength((long) content.length)
                                .build(),
                        RequestBody.fromBytes(content));
            }
            return storageKey;
        }

        try {
            Path path = localPath(storageKey);
            Files.createDirectories(path.getParent());
            Files.write(path, content);
            return storageKey;
        } catch (IOException e) {
            throw new UncheckedIOException("전자책 파일을 저장하지 못했습니다: " + storageKey, e);
        }
    }

    private Path localPath(String storageKey) {
        Path root = Paths.get(storageProperties.getLocal().getEbookDir()).toAbsolutePath().normalize();
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("잘못된 전자책 경로입니다.");
        }
        return resolved;
    }

    private String s3Key(String storageKey) {
        String prefix = storageProperties.getS3().getEbookPrefix();
        if (prefix == null || prefix.isBlank()) return storageKey;
        return prefix.replaceAll("^/+", "").replaceAll("/+$", "") + "/" + storageKey;
    }

    private String requireBucket() {
        String bucket = storageProperties.getS3().getBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 upload bucket is required.");
        }
        return bucket;
    }
}
