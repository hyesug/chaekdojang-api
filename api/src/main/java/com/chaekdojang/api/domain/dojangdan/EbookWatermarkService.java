package com.chaekdojang.api.domain.dojangdan;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 전자책 워터마크.
 *
 * 완벽한 DRM은 불가능하다. 목표는 차단이 아니라 억제와 추적이다.
 * 그래서 정교한 보호 대신 "누구에게 언제 발급된 파일인지"만 확실히 새긴다.
 *
 * 표준 14 폰트는 한글을 그릴 수 없어서 폰트 파일을 함께 배포해야 하는데,
 * 추적에 필요한 것은 이름이 아니라 식별자이므로 ASCII 식별자로 새긴다.
 */
@Slf4j
@Service
public class EbookWatermarkService {

    private static final DateTimeFormatter ISSUED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] watermark(byte[] source, Long campaignId, Long userId, String nickname) {
        String footer = "chaekdojang.com  |  campaign #%d  |  reader #%d  |  issued %s%s".formatted(
                campaignId,
                userId,
                LocalDateTime.now().format(ISSUED_AT),
                asciiSuffix(nickname));
        String diagonal = "CHAEKDOJANG  READER #%d".formatted(userId);

        try (PDDocument document = Loader.loadPDF(source);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 비밀번호 없이 열리는 PDF도 배포 도구에 따라 암호화 사전만 남아 있을 수 있다.
            // 원본 보안 설정을 그대로 저장하려 하면 PDFBox가 예외를 내므로,
            // 선정자용 사본에서는 제거한 뒤 책도장 워터마크를 적용한다.
            document.setAllSecurityToBeRemoved(true);

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PDExtendedGraphicsState faint = new PDExtendedGraphicsState();
            faint.setNonStrokingAlphaConstant(0.12f);

            PDExtendedGraphicsState solid = new PDExtendedGraphicsState();
            solid.setNonStrokingAlphaConstant(0.55f);

            for (PDPage page : document.getPages()) {
                PDRectangle box = page.getMediaBox();
                try (PDPageContentStream content = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    // 대각선 워터마크
                    content.setGraphicsStateParameters(faint);
                    content.beginText();
                    content.setFont(font, 28);
                    content.setTextMatrix(Matrix.getRotateInstance(
                            Math.toRadians(45), box.getLowerLeftX() + 60, box.getLowerLeftY() + 120));
                    content.showText(diagonal);
                    content.endText();

                    // 하단 식별 문구
                    content.setGraphicsStateParameters(solid);
                    content.beginText();
                    content.setFont(font, 7);
                    content.newLineAtOffset(box.getLowerLeftX() + 24, box.getLowerLeftY() + 16);
                    content.showText(footer);
                    content.endText();
                }
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("전자책 워터마크를 만들지 못했습니다.", e);
        }
    }

    public Integer readPageCount(byte[] source) {
        try (PDDocument document = Loader.loadPDF(source)) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            log.warn("PDF 페이지 수를 읽지 못했습니다: {}", e.getMessage());
            return null;
        }
    }

    /** 표준 14 폰트로 그릴 수 있는 문자만 남긴다. 한글 닉네임은 통째로 빠진다. */
    private String asciiSuffix(String nickname) {
        if (nickname == null) return "";
        String ascii = nickname.replaceAll("[^\\x20-\\x7E]", "").trim();
        return ascii.isBlank() ? "" : "  |  " + ascii;
    }
}
