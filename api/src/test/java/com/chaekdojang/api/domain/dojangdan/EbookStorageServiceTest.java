package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.upload.StorageProperties;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EbookStorageServiceTest {

    private static final byte[] PDF = "%PDF-test".getBytes(StandardCharsets.UTF_8);

    @Test
    void 전용_버킷이_없으면_전자책을_저장하지_않는다() {
        EbookStorageService service = new EbookStorageService(s3Properties("chaekdojang-public", ""));

        assertThatThrownBy(() -> service.putOriginal(PDF, "pdf"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EBOOK_STORAGE_NOT_CONFIGURED);
    }

    @Test
    void 전용_버킷이_없으면_전자책을_읽지_않는다() {
        EbookStorageService service = new EbookStorageService(s3Properties("chaekdojang-public", ""));

        assertThatThrownBy(() -> service.get("originals/any.pdf"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EBOOK_STORAGE_NOT_CONFIGURED);
    }

    @Test
    void 이미지_버킷을_전자책_버킷으로_지정하면_거부한다() {
        // 공개 읽기가 열린 버킷에 전자책을 올리면 워터마크 없는 원본이 그대로 새어나간다.
        EbookStorageService service =
                new EbookStorageService(s3Properties("chaekdojang-public", "chaekdojang-public"));

        assertThatThrownBy(() -> service.putOriginal(PDF, "pdf"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EBOOK_STORAGE_NOT_CONFIGURED);
    }

    @Test
    void 로컬_저장소는_비공개_폴더에_저장하고_다시_읽는다(@TempDir Path tempDir) {
        StorageProperties properties = new StorageProperties();
        properties.setType("local");
        properties.getLocal().setEbookDir(tempDir.toString());
        EbookStorageService service = new EbookStorageService(properties);

        String key = service.putOriginal(PDF, "pdf");

        assertThat(key).startsWith("originals/");
        assertThat(service.get(key)).isEqualTo(PDF);
        assertThat(tempDir.resolve(key)).exists();
    }

    private StorageProperties s3Properties(String imageBucket, String ebookBucket) {
        StorageProperties properties = new StorageProperties();
        properties.setType("s3");
        properties.getS3().setBucket(imageBucket);
        properties.getS3().setEbookBucket(ebookBucket);
        return properties;
    }
}
