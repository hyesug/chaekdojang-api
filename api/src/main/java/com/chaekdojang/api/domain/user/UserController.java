package com.chaekdojang.api.domain.user;

import com.chaekdojang.api.domain.review.ReviewService;
import com.chaekdojang.api.domain.review.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import com.chaekdojang.api.domain.user.dto.*;
import com.chaekdojang.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "유저 프로필", description = "내 프로필 조회·수정, 다른 사람 프로필 조회, 특정 유저 독후감 목록")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ReviewService reviewService;

    @Operation(summary = "내 프로필 조회", description = "로그인한 사용자의 프로필을 반환합니다. JWT 필요.")
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile() {
        return ApiResponse.ok(userService.getMyProfile());
    }

    @Operation(summary = "내 프로필 수정", description = "닉네임, 소개, 프로필 이미지를 수정합니다. JWT 필요.")
    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateMyProfile(@RequestBody @Valid UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateMyProfile(request));
    }

    @Operation(summary = "회원 탈퇴", description = "계정을 soft delete 처리합니다. JWT 필요.")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe() {
        userService.deleteMe();
    }

    @Operation(summary = "독자 추천", description = "같은 책을 읽은 독자를 추천합니다. JWT 필요.")
    @GetMapping("/me/recommendations")
    public ApiResponse<List<UserRecommendationResponse>> getRecommendations() {
        return ApiResponse.ok(userService.getRecommendations());
    }

    @Operation(summary = "독서 통계", description = "월별 독서량 및 선호 장르를 반환합니다. JWT 필요.")
    @GetMapping("/me/stats")
    public ApiResponse<ReadingStatsResponse> getReadingStats() {
        return ApiResponse.ok(userService.getReadingStats());
    }

    @Operation(summary = "내 독후감 목록 (페이징+검색)", description = "내가 쓴 독후감을 페이지 단위로 반환합니다. q로 책 제목·내용 검색. JWT 필요.")
    @GetMapping("/me/reviews")
    public ApiResponse<Page<ReviewResponse>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q) {
        return ApiResponse.ok(reviewService.getMyReviews(page, size, q));
    }

    @Operation(summary = "인생책 설정", description = "나의 인생책을 설정합니다. bookId가 null이면 삭제. JWT 필요.")
    @PatchMapping("/me/life-book")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setLifeBook(@RequestBody SetLifeBookRequest request) {
        userService.setLifeBook(request.bookId());
    }

    @Operation(summary = "다른 유저 프로필 조회", description = "userId로 다른 사용자의 프로필을 조회합니다. 인증 불필요.")
    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        return ApiResponse.ok(userService.getUserProfile(userId));
    }

    @Operation(summary = "닉네임으로 프로필 조회", description = "공유 URL용으로 닉네임에 해당하는 사용자 프로필을 조회합니다. 인증 불필요.")
    @GetMapping("/nickname/{nickname}")
    public ApiResponse<UserProfileResponse> getUserProfileByNickname(@PathVariable String nickname) {
        return ApiResponse.ok(userService.getUserProfileByNickname(nickname));
    }

    @Operation(summary = "특정 유저 독후감 목록", description = "userId에 해당하는 사용자의 독후감 목록을 반환합니다. 인증 불필요.")
    @GetMapping("/{userId}/reviews")
    public ApiResponse<List<ReviewResponse>> getUserReviews(@PathVariable Long userId) {
        return ApiResponse.ok(reviewService.getByUser(userId));
    }

    @Operation(summary = "사용자 검색", description = "닉네임으로 사용자를 검색합니다. 인증 불필요.")
    @GetMapping("/search")
    public ApiResponse<List<UserSummary>> searchUsers(@RequestParam String q) {
        return ApiResponse.ok(userService.searchUsers(q));
    }
}
