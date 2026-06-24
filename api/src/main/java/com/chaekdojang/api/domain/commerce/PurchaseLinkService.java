package com.chaekdojang.api.domain.commerce;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.commerce.dto.PurchaseLinkAddRequest;
import com.chaekdojang.api.domain.commerce.dto.PurchaseLinkResponse;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseLinkService {

    private final PurchaseLinkRepository purchaseLinkRepository;
    private final BookRepository bookRepository;

    public List<PurchaseLinkResponse> getLinks(Long bookId) {
        List<PurchaseLink> saved = purchaseLinkRepository.findAllByBookId(bookId);
        if (!saved.isEmpty()) {
            return saved.stream().map(PurchaseLinkResponse::from).toList();
        }
        // DB에 저장된 링크 없으면 책 제목 기반 검색 URL을 fallback으로 반환
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
        String encoded = URLEncoder.encode(book.getTitle(), StandardCharsets.UTF_8);
        return List.of(
                new PurchaseLinkResponse(null, PurchaseProvider.COUPANG,
                        "https://www.coupang.com/np/search?q=" + encoded),
                new PurchaseLinkResponse(null, PurchaseProvider.KYOBO,
                        "https://search.kyobobook.co.kr/search?keyword=" + encoded)
        );
    }

    @Transactional
    public PurchaseLinkResponse addLink(PurchaseLinkAddRequest request) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
        PurchaseLink link = PurchaseLink.builder()
                .book(book)
                .provider(request.provider())
                .url(request.url())
                .build();
        return PurchaseLinkResponse.from(purchaseLinkRepository.save(link));
    }

    @Transactional
    public void recordClick(Long linkId) {
        PurchaseLink link = purchaseLinkRepository.findById(linkId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        link.increaseClickCount();
    }
}
