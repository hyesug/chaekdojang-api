package com.chaekdojang.api.domain.readinggroup;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.readinggroup.dto.*;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadingGroupService {

    private final ReadingGroupRepository groupRepository;
    private final ReadingGroupMemberRepository memberRepository;
    private final ReadingGroupBookRepository groupBookRepository;
    private final ReadingGroupReviewRepository groupReviewRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;

    public List<ReadingGroupResponse> getPublicGroups() {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        return groupRepository.findAllByVisibilityOrderByCreatedAtDesc(ReadingGroupVisibility.PUBLIC)
                .stream()
                .map(group -> toResponse(group, userId))
                .toList();
    }

    public ReadingGroupResponse getGroup(String slug) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        ReadingGroup group = findBySlug(slug);
        assertReadable(group, userId);
        return toResponse(group, userId);
    }

    @Transactional
    public ReadingGroupResponse create(ReadingGroupCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        ReadingGroup group = groupRepository.save(ReadingGroup.builder()
                .owner(owner)
                .name(trim(request.name()))
                .slug(createUniqueSlug(request.name()))
                .description(blankToNull(request.description()))
                .imageUrl(blankToNull(request.imageUrl()))
                .visibility(request.visibility() == null ? ReadingGroupVisibility.PUBLIC : request.visibility())
                .joinPolicy(request.joinPolicy() == null ? ReadingGroupJoinPolicy.OPEN : request.joinPolicy())
                .build());
        memberRepository.save(ReadingGroupMember.owner(group, owner));
        return toResponse(group, userId);
    }

    @Transactional
    public ReadingGroupResponse join(String slug) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        ReadingGroup group = findBySlug(slug);
        memberRepository.findByGroupIdAndUserId(group.getId(), userId).ifPresent(member -> {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        });
        ReadingGroupMemberStatus status = group.getJoinPolicy() == ReadingGroupJoinPolicy.OPEN
                ? ReadingGroupMemberStatus.APPROVED
                : ReadingGroupMemberStatus.PENDING;
        memberRepository.save(ReadingGroupMember.join(group, user, status));
        return toResponse(group, userId);
    }

    public List<ReadingGroupMemberResponse> getMembers(String slug) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findBySlug(slug);
        assertManager(group, userId);
        return memberRepository.findAllByGroupIdOrderByCreatedAtAsc(group.getId())
                .stream()
                .map(ReadingGroupMemberResponse::from)
                .toList();
    }

    public List<ReadingGroupMemberResponse> getPendingMembers(String slug) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findBySlug(slug);
        assertManager(group, userId);
        return memberRepository.findAllByGroupIdAndStatusOrderByCreatedAtAsc(group.getId(), ReadingGroupMemberStatus.PENDING)
                .stream()
                .map(ReadingGroupMemberResponse::from)
                .toList();
    }

    @Transactional
    public ReadingGroupMemberResponse approveMember(String slug, Long memberId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findBySlug(slug);
        assertManager(group, userId);
        ReadingGroupMember member = findMemberInGroup(group, memberId);
        member.approve();
        return ReadingGroupMemberResponse.from(member);
    }

    @Transactional
    public ReadingGroupMemberResponse rejectMember(String slug, Long memberId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findBySlug(slug);
        assertManager(group, userId);
        ReadingGroupMember member = findMemberInGroup(group, memberId);
        if (member.getRole() == ReadingGroupMemberRole.OWNER) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        member.reject();
        return ReadingGroupMemberResponse.from(member);
    }

    @Transactional
    public ReadingGroupResponse addBook(String slug, ReadingGroupBookAddRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findBySlug(slug);
        assertManager(group, userId);
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
        if (!groupBookRepository.existsByGroupIdAndBookId(group.getId(), book.getId())) {
            groupBookRepository.save(ReadingGroupBook.of(group, book, blankToNull(request.note())));
        }
        return toResponse(group, userId);
    }

    @Transactional
    public ReadingGroupReviewResponse attachReview(String slug, Long groupBookId, ReadingGroupReviewAttachRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findBySlug(slug);
        assertApprovedMember(group, userId);
        ReadingGroupBook groupBook = groupBookRepository.findByIdAndGroupId(groupBookId, group.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Review review = reviewRepository.findByIdAndDeletedAtIsNullAndHiddenFalse(request.reviewId())
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.isAuthor(userId) || review.getBook() == null || !review.getBook().getId().equals(groupBook.getBook().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (groupReviewRepository.existsByGroupBookIdAndReviewId(groupBookId, review.getId())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return ReadingGroupReviewResponse.from(groupReviewRepository.save(ReadingGroupReview.of(group, groupBook, review)));
    }

    public List<ReadingGroupReviewResponse> getGroupBookReviews(String slug, Long groupBookId) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        ReadingGroup group = findBySlug(slug);
        assertReadable(group, userId);
        ReadingGroupBook groupBook = groupBookRepository.findByIdAndGroupId(groupBookId, group.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return groupReviewRepository.findAllByGroupBookIdOrderByCreatedAtDesc(groupBook.getId())
                .stream()
                .map(ReadingGroupReviewResponse::from)
                .toList();
    }

    private ReadingGroupResponse toResponse(ReadingGroup group, Long userId) {
        boolean member = isApprovedMember(group.getId(), userId);
        boolean manager = isManager(group.getId(), userId);
        List<ReadingGroupBookResponse> books = groupBookRepository.findAllByGroupIdOrderByCreatedAtDesc(group.getId())
                .stream()
                .map(groupBook -> ReadingGroupBookResponse.of(groupBook, groupReviewRepository.countByGroupBookId(groupBook.getId())))
                .toList();
        return ReadingGroupResponse.of(group, member, manager, books);
    }

    private ReadingGroup findBySlug(String slug) {
        return groupRepository.findBySlug(slug)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    private ReadingGroupMember findMemberInGroup(ReadingGroup group, Long memberId) {
        ReadingGroupMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (!member.getGroup().getId().equals(group.getId())) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        return member;
    }

    private void assertReadable(ReadingGroup group, Long userId) {
        if (group.getVisibility() == ReadingGroupVisibility.PUBLIC) return;
        if (isApprovedMember(group.getId(), userId)) return;
        if (SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN")) return;
        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    private void assertManager(ReadingGroup group, Long userId) {
        if (isManager(group.getId(), userId) || SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN")) return;
        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    private void assertApprovedMember(ReadingGroup group, Long userId) {
        if (isApprovedMember(group.getId(), userId)) return;
        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    private boolean isApprovedMember(Long groupId, Long userId) {
        return userId != null && memberRepository.existsByGroupIdAndUserIdAndStatus(groupId, userId, ReadingGroupMemberStatus.APPROVED);
    }

    private boolean isManager(Long groupId, Long userId) {
        return userId != null && memberRepository.findByGroupIdAndUserId(groupId, userId)
                .map(ReadingGroupMember::canManage)
                .orElse(false);
    }

    private String createUniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9가-힣]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isBlank()) base = "group";
        if (base.length() > 80) base = base.substring(0, 80).replaceAll("-+$", "");
        String slug = base;
        int suffix = 2;
        while (groupRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
