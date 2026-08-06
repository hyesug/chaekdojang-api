package com.chaekdojang.api.domain.readinggroup;

import com.chaekdojang.api.domain.readinggroup.dto.*;
import com.chaekdojang.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class ReadingGroupController {

    private final ReadingGroupService readingGroupService;
    private final ReadingGroupQuestionService readingGroupQuestionService;

    @GetMapping
    public ApiResponse<List<ReadingGroupResponse>> getPublicGroups() {
        return ApiResponse.ok(readingGroupService.getPublicGroups());
    }

    @PostMapping
    public ApiResponse<ReadingGroupResponse> create(@RequestBody @Valid ReadingGroupCreateRequest request) {
        return ApiResponse.ok(readingGroupService.create(request));
    }

    @GetMapping("/{slug}")
    public ApiResponse<ReadingGroupResponse> getGroup(@PathVariable String slug) {
        return ApiResponse.ok(readingGroupService.getGroup(slug));
    }

    @PostMapping("/{slug}/join")
    public ApiResponse<ReadingGroupResponse> join(@PathVariable String slug) {
        return ApiResponse.ok(readingGroupService.join(slug));
    }

    @PostMapping("/{slug}/leave")
    public ApiResponse<ReadingGroupResponse> leave(@PathVariable String slug) {
        return ApiResponse.ok(readingGroupService.leave(slug));
    }

    @GetMapping("/{slug}/members")
    public ApiResponse<List<ReadingGroupMemberResponse>> getMembers(
            @PathVariable String slug,
            @RequestParam(required = false) ReadingGroupMemberStatus status) {
        if (status == ReadingGroupMemberStatus.PENDING) {
            return ApiResponse.ok(readingGroupService.getPendingMembers(slug));
        }
        return ApiResponse.ok(readingGroupService.getMembers(slug));
    }

    @PostMapping("/{slug}/members/{memberId}/approve")
    public ApiResponse<ReadingGroupMemberResponse> approveMember(
            @PathVariable String slug,
            @PathVariable Long memberId) {
        return ApiResponse.ok(readingGroupService.approveMember(slug, memberId));
    }

    @PostMapping("/{slug}/members/{memberId}/reject")
    public ApiResponse<ReadingGroupMemberResponse> rejectMember(
            @PathVariable String slug,
            @PathVariable Long memberId) {
        return ApiResponse.ok(readingGroupService.rejectMember(slug, memberId));
    }

    @PostMapping("/{slug}/members/{memberId}/block")
    public ApiResponse<ReadingGroupMemberResponse> blockMember(
            @PathVariable String slug,
            @PathVariable Long memberId) {
        return ApiResponse.ok(readingGroupService.blockMember(slug, memberId));
    }

    @PostMapping("/{slug}/books")
    public ApiResponse<ReadingGroupResponse> addBook(
            @PathVariable String slug,
            @RequestBody @Valid ReadingGroupBookAddRequest request) {
        return ApiResponse.ok(readingGroupService.addBook(slug, request));
    }

    @PatchMapping("/{slug}/notice")
    public ApiResponse<ReadingGroupResponse> updateNotice(
            @PathVariable String slug,
            @RequestBody @Valid ReadingGroupNoticeUpdateRequest request) {
        return ApiResponse.ok(readingGroupService.updateNotice(slug, request));
    }

    @PatchMapping("/{slug}/books/{groupBookId}/progress")
    public ApiResponse<ReadingGroupResponse> updateBookProgress(
            @PathVariable String slug,
            @PathVariable Long groupBookId,
            @RequestBody @Valid ReadingGroupBookProgressUpdateRequest request) {
        return ApiResponse.ok(readingGroupService.updateBookProgress(slug, groupBookId, request));
    }

    @GetMapping("/{slug}/books/{groupBookId}/reviews")
    public ApiResponse<List<ReadingGroupReviewResponse>> getGroupBookReviews(
            @PathVariable String slug,
            @PathVariable Long groupBookId) {
        return ApiResponse.ok(readingGroupService.getGroupBookReviews(slug, groupBookId));
    }

    @GetMapping("/{slug}/books/{groupBookId}/result")
    public ApiResponse<ReadingGroupBookResultResponse> getGroupBookResult(
            @PathVariable String slug,
            @PathVariable Long groupBookId) {
        return ApiResponse.ok(readingGroupService.getGroupBookResult(slug, groupBookId));
    }

    @GetMapping("/{slug}/books/{groupBookId}/my-reviews")
    public ApiResponse<List<ReadingGroupMyReviewResponse>> getMyGroupBookReviews(
            @PathVariable String slug,
            @PathVariable Long groupBookId) {
        return ApiResponse.ok(readingGroupService.getMyGroupBookReviews(slug, groupBookId));
    }

    @PostMapping("/{slug}/books/{groupBookId}/reviews")
    public ApiResponse<ReadingGroupReviewResponse> attachReview(
            @PathVariable String slug,
            @PathVariable Long groupBookId,
            @RequestBody @Valid ReadingGroupReviewAttachRequest request) {
        return ApiResponse.ok(readingGroupService.attachReview(slug, groupBookId, request));
    }

    @DeleteMapping("/{slug}/books/{groupBookId}/reviews/{reviewId}")
    public ApiResponse<Void> detachReview(
            @PathVariable String slug,
            @PathVariable Long groupBookId,
            @PathVariable Long reviewId) {
        readingGroupService.detachReview(slug, groupBookId, reviewId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{slug}/books/{groupBookId}/questions")
    public ApiResponse<ReadingGroupQuestionListResponse> getQuestions(
            @PathVariable String slug,
            @PathVariable Long groupBookId) {
        return ApiResponse.ok(readingGroupQuestionService.getQuestions(slug, groupBookId));
    }

    @PostMapping("/{slug}/books/{groupBookId}/questions")
    public ApiResponse<ReadingGroupQuestionItemResponse> createQuestion(
            @PathVariable String slug,
            @PathVariable Long groupBookId,
            @RequestBody @Valid ReadingGroupQuestionCreateRequest request) {
        return ApiResponse.ok(readingGroupQuestionService.createQuestion(slug, groupBookId, request));
    }

    @PostMapping("/{slug}/books/{groupBookId}/questions/ai-draft")
    public ApiResponse<ReadingGroupQuestionItemResponse> generateQuestionDraft(
            @PathVariable String slug,
            @PathVariable Long groupBookId) {
        return ApiResponse.ok(readingGroupQuestionService.generateAiDraft(slug, groupBookId));
    }

    @PatchMapping("/{slug}/books/{groupBookId}/questions/{questionId}")
    public ApiResponse<ReadingGroupQuestionItemResponse> updateQuestion(
            @PathVariable String slug,
            @PathVariable Long groupBookId,
            @PathVariable Long questionId,
            @RequestBody @Valid ReadingGroupQuestionUpdateRequest request) {
        return ApiResponse.ok(readingGroupQuestionService.updateQuestion(
                slug, groupBookId, questionId, request));
    }

    @PostMapping("/{slug}/books/{groupBookId}/questions/{questionId}/publish")
    public ApiResponse<ReadingGroupQuestionItemResponse> publishQuestion(
            @PathVariable String slug,
            @PathVariable Long groupBookId,
            @PathVariable Long questionId) {
        return ApiResponse.ok(readingGroupQuestionService.publishQuestion(slug, groupBookId, questionId));
    }

    @DeleteMapping("/{slug}/books/{groupBookId}/questions/{questionId}")
    public ApiResponse<Void> deleteQuestion(
            @PathVariable String slug,
            @PathVariable Long groupBookId,
            @PathVariable Long questionId) {
        readingGroupQuestionService.deleteQuestion(slug, groupBookId, questionId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{slug}/books/{groupBookId}/questions/{questionId}/response")
    public ApiResponse<ReadingGroupQuestionAnswerResponse> saveQuestionAnswer(
            @PathVariable String slug,
            @PathVariable Long groupBookId,
            @PathVariable Long questionId,
            @RequestBody @Valid ReadingGroupQuestionAnswerRequest request) {
        return ApiResponse.ok(readingGroupQuestionService.saveAnswer(
                slug, groupBookId, questionId, request));
    }

    @DeleteMapping("/{slug}/books/{groupBookId}/questions/{questionId}/response")
    public ApiResponse<Void> deleteQuestionAnswer(
            @PathVariable String slug,
            @PathVariable Long groupBookId,
            @PathVariable Long questionId) {
        readingGroupQuestionService.deleteAnswer(slug, groupBookId, questionId);
        return ApiResponse.ok(null);
    }
}
