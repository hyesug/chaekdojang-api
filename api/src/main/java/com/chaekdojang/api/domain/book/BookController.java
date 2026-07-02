package com.chaekdojang.api.domain.book;

import com.chaekdojang.api.domain.book.dto.BookResponse;
import com.chaekdojang.api.domain.book.dto.BookReactionReportResponse;
import com.chaekdojang.api.domain.book.dto.PublicBookDetailResponse;
import com.chaekdojang.api.domain.review.ReviewService;
import com.chaekdojang.api.domain.review.dto.ReviewResponse;
import com.chaekdojang.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "도서", description = "도서 검색 (카카오 + Google Books)")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final ReviewService reviewService;

    @Operation(summary = "도서 검색", description = "카카오 책 API와 Google Books API를 통합 검색합니다. 인증 불필요.")
    @GetMapping("/search")
    public ApiResponse<List<BookResponse>> search(
            @Parameter(description = "검색 키워드 (예: 채식주의자)", required = true)
            @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "저자명")
            @RequestParam(required = false) String author,
            @Parameter(description = "출판사")
            @RequestParam(required = false) String publisher) {
        return ApiResponse.ok(bookService.search(q, author, publisher));
    }

    @Operation(summary = "카테고리별 도서 조회", description = "특정 카테고리에 속한 도서 목록을 반환합니다. 인증 불필요.")
    @GetMapping("/category")
    public ApiResponse<List<BookResponse>> getByCategory(
            @Parameter(description = "카테고리명 (예: 소설, 자기계발)", required = true)
            @RequestParam String name) {
        return ApiResponse.ok(bookService.findByCategory(name));
    }

    @Operation(summary = "SEO 공개 책 상세", description = "slug 또는 book id로 로그인 없이 접근 가능한 공개 책 상세 정보를 반환합니다.")
    @GetMapping("/public/{slug}")
    public ApiResponse<PublicBookDetailResponse> getPublicBook(@PathVariable String slug) {
        return ApiResponse.ok(bookService.findPublicBySlug(slug));
    }

    @Operation(summary = "SEO sitemap 책 목록", description = "sitemap 생성에 사용할 공개 책 목록을 반환합니다.")
    @GetMapping("/public")
    public ApiResponse<List<BookResponse>> getPublicBooksForSitemap() {
        return ApiResponse.ok(bookService.findPublicBooksForSitemap());
    }

    @Operation(summary = "도서 단건 조회", description = "도서 ID로 특정 책 정보를 반환합니다. 인증 불필요.")
    @GetMapping("/{id:\\d+}")
    public ApiResponse<BookResponse> getOne(@PathVariable Long id) {
        return ApiResponse.ok(bookService.findById(id));
    }

    @Operation(summary = "책 반응 리포트", description = "책 1권의 공개 독후감과 AI 독서카드를 집계한 공개 리포트를 반환합니다. 인증 불필요.")
    @GetMapping("/{id:\\d+}/reaction-report")
    public ApiResponse<BookReactionReportResponse> getReactionReport(@PathVariable Long id) {
        return ApiResponse.ok(bookService.getReactionReport(id));
    }

    @Operation(summary = "책별 독후감 목록", description = "특정 책에 대해 작성된 독후감 목록을 반환합니다. 인증 불필요.")
    @GetMapping("/{id:\\d+}/reviews")
    public ApiResponse<List<ReviewResponse>> getReviewsByBook(
            @Parameter(description = "책 ID", required = true)
            @PathVariable Long id,
            @RequestParam(defaultValue = "recent") String sort) {
        return ApiResponse.ok(reviewService.getByBook(id, sort));
    }

    @Operation(summary = "작품별 독후감 목록", description = "제목과 저자가 같은 작품의 여러 판본 독후감을 함께 반환합니다. 인증 불필요.")
    @GetMapping("/work/reviews")
    public ApiResponse<List<ReviewResponse>> getReviewsByWork(
            @Parameter(description = "작품 제목", required = true)
            @RequestParam String title,
            @Parameter(description = "저자", required = true)
            @RequestParam String author) {
        return ApiResponse.ok(reviewService.getByBookWork(title, author));
    }
}
