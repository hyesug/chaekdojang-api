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

    @PostMapping("/{slug}/books")
    public ApiResponse<ReadingGroupResponse> addBook(
            @PathVariable String slug,
            @RequestBody @Valid ReadingGroupBookAddRequest request) {
        return ApiResponse.ok(readingGroupService.addBook(slug, request));
    }

    @GetMapping("/{slug}/books/{groupBookId}/reviews")
    public ApiResponse<List<ReadingGroupReviewResponse>> getGroupBookReviews(
            @PathVariable String slug,
            @PathVariable Long groupBookId) {
        return ApiResponse.ok(readingGroupService.getGroupBookReviews(slug, groupBookId));
    }

    @PostMapping("/{slug}/books/{groupBookId}/reviews")
    public ApiResponse<ReadingGroupReviewResponse> attachReview(
            @PathVariable String slug,
            @PathVariable Long groupBookId,
            @RequestBody @Valid ReadingGroupReviewAttachRequest request) {
        return ApiResponse.ok(readingGroupService.attachReview(slug, groupBookId, request));
    }
}
