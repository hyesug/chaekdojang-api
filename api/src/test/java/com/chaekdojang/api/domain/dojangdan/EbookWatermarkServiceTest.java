package com.chaekdojang.api.domain.dojangdan;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class EbookWatermarkServiceTest {

    private final EbookWatermarkService service = new EbookWatermarkService();

    @Test
    void 암호화_사전이_있는_PDF도_워터마크_사본을_만든다() throws Exception {
        byte[] source;
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            StandardProtectionPolicy policy = new StandardProtectionPolicy(
                    "", "", new AccessPermission());
            policy.setEncryptionKeyLength(128);
            document.protect(policy);
            document.save(out);
            source = out.toByteArray();
        }

        byte[] result = service.watermark(source, 1L, 2L, "독자");

        try (PDDocument document = Loader.loadPDF(result)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            assertThat(document.isEncrypted()).isFalse();
        }
    }
}
