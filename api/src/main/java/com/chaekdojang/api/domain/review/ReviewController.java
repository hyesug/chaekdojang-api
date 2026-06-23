package com.chaekdojang.api.domain.review;

import com.chaekdojang.api.domain.review.dto.ReviewCreateRequest;
import com.chaekdojang.api.domain.review.dto.ReviewResponse;
import com.chaekdojang.api.domain.review.dto.ReviewUpdateRequest;
import com.chaekdojang.api.domain.review.dto.ReviewVisibilityRequest;
import com.chaekdojang.api.domain.user.UserService;
import com.chaekdojang.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "독후감", description = "독후감 작성·조회·수정·삭제")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    @Operation(summary = "독후감 작성", description = "로그인한 사용자가 새 독후감을 작성합니다. JWT 필요.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> create(@RequestBody @Valid ReviewCreateRequest request) {
        return ApiResponse.ok(reviewService.create(request));
    }

    @Operation(summary = "독후감 피드 조회",
            description = "모든 사용자의 독후감을 최신순으로 페이지 단위 반환. page(기본 0), size(기본 10) 파라미터 지원.")
    @GetMapping
    public ApiResponse<Page<ReviewResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "recent") String sort) {
        return ApiResponse.ok(reviewService.getAll(page, size, sort));
    }

    @Operation(summary = "독후감 단건 조회", description = "독후감 ID로 특정 독후감을 조회합니다. 인증 불필요.")
    @GetMapping("/{id}")
    public ApiResponse<ReviewResponse> getOne(@PathVariable Long id) {
        return ApiResponse.ok(reviewService.getOne(id));
    }

    @Operation(summary = "독후감 조회수 기록", description = "공개 독후감 상세 진입을 조회수로 기록합니다. 인증 불필요.")
    @PostMapping("/{id}/view")
    public ApiResponse<Void> recordView(@PathVariable Long id) {
        reviewService.recordView(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "독후감 수정", description = "본인이 작성한 독후감의 내용을 수정합니다. JWT 필요.")
    @PutMapping("/{id}")
    public ApiResponse<ReviewResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid ReviewUpdateRequest request) {
        return ApiResponse.ok(reviewService.update(id, request));
    }

    @PatchMapping("/{id}/hidden")
    public ApiResponse<ReviewResponse> updateVisibility(
            @PathVariable Long id,
            @RequestBody @Valid ReviewVisibilityRequest request) {
        return ApiResponse.ok(reviewService.updateVisibility(id, request));
    }

    @Operation(summary = "독후감 삭제", description = "본인이 작성한 독후감을 삭제합니다. JWT 필요.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        reviewService.delete(id);
    }

    @Operation(summary = "팔로잉 피드", description = "내가 팔로우한 사람들의 독후감을 최신순으로 반환합니다. JWT 필요.")
    @GetMapping("/feed")
    public ApiResponse<List<ReviewResponse>> getFeed() {
        return ApiResponse.ok(reviewService.getFeed());
    }

    @Operation(summary = "취향 피드", description = "취향이 비슷한 추천 유저들의 독후감을 최신순으로 반환합니다. JWT 필요.")
    @GetMapping("/feed/taste")
    public ApiResponse<List<ReviewResponse>> getTasteFeed() {
        List<Long> recommendedIds = userService.getRecommendations().stream()
                .map(r -> r.id())
                .toList();
        return ApiResponse.ok(reviewService.getByTaste(recommendedIds));
    }
}
