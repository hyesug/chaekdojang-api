package com.chaekdojang.api.domain.readinggroup;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.notification.NotificationService;
import com.chaekdojang.api.domain.notification.NotificationType;
import com.chaekdojang.api.domain.readinggroup.dto.*;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReadingGroupService {

    private final ReadingGroupRepository groupRepository;
    private final ReadingGroupMemberRepository memberRepository;
    private final ReadingGroupBookRepository groupBookRepository;
    private final ReadingGroupReviewRepository groupReviewRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;

    public List<ReadingGroupResponse> getPublicGroups() {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        Map<Long, ReadingGroup> groups = new LinkedHashMap<>();
        if (userId != null) {
            memberRepository.findAllByUserIdAndStatusInOrderByUpdatedAtDesc(
                            userId,
                            List.of(ReadingGroupMemberStatus.APPROVED, ReadingGroupMemberStatus.PENDING))
                    .forEach(member -> groups.put(member.getGroup().getId(), member.getGroup()));
            groupRepository.findAllByOwnerIdOrderByCreatedAtDesc(userId)
                    .forEach(group -> groups.putIfAbsent(group.getId(), group));
        }
        groupRepository.findAllByOrderByCreatedAtDesc()
                .forEach(group -> groups.putIfAbsent(group.getId(), group));
        return groups.values().stream()
                .map(group -> toResponse(group, userId))
                .toList();
    }

    public ReadingGroupResponse getGroup(String slug) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        ReadingGroup group = findBySlug(slug);
        return toResponse(group, userId);
    }

    @Transactional
    public ReadingGroupResponse create(ReadingGroupCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        ReadingGroupVisibility visibility = request.visibility() == null ? ReadingGroupVisibility.PUBLIC : request.visibility();
        ReadingGroupJoinPolicy joinPolicy = visibility == ReadingGroupVisibility.PRIVATE
                ? ReadingGroupJoinPolicy.APPROVAL
                : request.joinPolicy() == null ? ReadingGroupJoinPolicy.OPEN : request.joinPolicy();
        ReadingGroup group = groupRepository.save(ReadingGroup.builder()
                .owner(owner)
                .name(trim(request.name()))
                .slug(createUniqueSlug(request.name()))
                .description(blankToNull(request.description()))
                .imageUrl(blankToNull(request.imageUrl()))
                .visibility(visibility)
                .joinPolicy(joinPolicy)
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
        if (!group.isJoinEnabled()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        ReadingGroupMemberStatus status = group.getVisibility() == ReadingGroupVisibility.PRIVATE
                ? ReadingGroupMemberStatus.PENDING
                : group.getJoinPolicy() == ReadingGroupJoinPolicy.OPEN
                ? ReadingGroupMemberStatus.APPROVED
                : ReadingGroupMemberStatus.PENDING;
        ReadingGroupMember existingMember = memberRepository.findByGroupIdAndUserId(group.getId(), userId).orElse(null);
        if (existingMember != null) {
            if (existingMember.getStatus() == ReadingGroupMemberStatus.APPROVED
                    || existingMember.getStatus() == ReadingGroupMemberStatus.PENDING) {
                return toResponse(group, userId);
            }
            if (existingMember.getStatus() == ReadingGroupMemberStatus.REJECTED) {
                existingMember.requestAgain(status);
                notifyOwnerAboutJoin(group, user, status);
                return toResponse(group, userId);
            }
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (isOwner(group, userId)) {
            memberRepository.save(ReadingGroupMember.owner(group, user));
            return toResponse(group, userId);
        }
        memberRepository.save(ReadingGroupMember.join(group, user, status));
        notifyOwnerAboutJoin(group, user, status);
        return toResponse(group, userId);
    }

    private void notifyOwnerAboutJoin(ReadingGroup group, User user, ReadingGroupMemberStatus status) {
        NotificationType type = status == ReadingGroupMemberStatus.APPROVED
                ? NotificationType.GROUP_JOINED
                : NotificationType.GROUP_JOIN_REQUEST;
        try {
            notificationService.send(group.getOwner(), user, type, group.getId(), group.getSlug());
        } catch (RuntimeException e) {
            log.warn("독서모임 가입 알림 생성 실패: groupId={}, userId={}, type={}", group.getId(), user.getId(), type, e);
        }
    }

    @Transactional
    public ReadingGroupResponse leave(String slug) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findBySlug(slug);
        ReadingGroupMember member = memberRepository.findByGroupIdAndUserId(group.getId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (member.getRole() == ReadingGroupMemberRole.OWNER) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        memberRepository.delete(member);
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
    public ReadingGroupMemberResponse blockMember(String slug, Long memberId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findBySlug(slug);
        assertManager(group, userId);
        ReadingGroupMember member = findMemberInGroup(group, memberId);
        if (member.getRole() == ReadingGroupMemberRole.OWNER) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        member.block();
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

    @Transactional
    public void detachReview(String slug, Long groupBookId, Long reviewId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findBySlug(slug);
        assertApprovedMember(group, userId);
        ReadingGroupBook groupBook = groupBookRepository.findByIdAndGroupId(groupBookId, group.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        ReadingGroupReview groupReview = groupReviewRepository.findByGroupBookIdAndReviewId(groupBook.getId(), reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (!groupReview.getReview().isAuthor(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        groupReviewRepository.delete(groupReview);
    }

    public List<ReadingGroupReviewResponse> getGroupBookReviews(String slug, Long groupBookId) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        ReadingGroup group = findBySlug(slug);
        assertContentReadable(group, userId);
        ReadingGroupBook groupBook = groupBookRepository.findByIdAndGroupId(groupBookId, group.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return groupReviewRepository.findAllByGroupBookIdOrderByCreatedAtDesc(groupBook.getId())
                .stream()
                .map(ReadingGroupReviewResponse::from)
                .toList();
    }

    public List<ReadingGroupMyReviewResponse> getMyGroupBookReviews(String slug, Long groupBookId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findBySlug(slug);
        assertApprovedMember(group, userId);
        ReadingGroupBook groupBook = groupBookRepository.findByIdAndGroupId(groupBookId, group.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return reviewRepository.findAllByAuthorIdAndBookIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(
                        userId,
                        groupBook.getBook().getId())
                .stream()
                .map(review -> ReadingGroupMyReviewResponse.of(
                        review,
                        groupReviewRepository.existsByGroupBookIdAndReviewId(groupBook.getId(), review.getId())))
                .toList();
    }

    private ReadingGroupResponse toResponse(ReadingGroup group, Long userId) {
        ReadingGroupMember currentMember = userId == null
                ? null
                : memberRepository.findByGroupIdAndUserId(group.getId(), userId).orElse(null);
        boolean owner = isOwner(group, userId);
        boolean member = owner || (currentMember != null && currentMember.getStatus() == ReadingGroupMemberStatus.APPROVED);
        boolean manager = owner || (currentMember != null && currentMember.canManage());
        boolean contentReadable = canReadContent(group, userId);
        long memberCount = memberRepository.countByGroupIdAndStatus(group.getId(), ReadingGroupMemberStatus.APPROVED);
        if (!memberRepository.existsByGroupIdAndUserIdAndStatus(group.getId(), group.getOwner().getId(), ReadingGroupMemberStatus.APPROVED)) {
            memberCount++;
        }
        List<ReadingGroupBookResponse> books = contentReadable
                ? groupBookRepository.findAllByGroupIdOrderByCreatedAtDesc(group.getId())
                        .stream()
                        .map(groupBook -> ReadingGroupBookResponse.of(groupBook, groupReviewRepository.countByGroupBookId(groupBook.getId())))
                        .toList()
                : List.of();
        return ReadingGroupResponse.of(
                group,
                memberCount,
                member,
                manager,
                owner ? ReadingGroupMemberStatus.APPROVED : currentMember != null ? currentMember.getStatus() : null,
                books,
                contentReadable
        );
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

    private void assertContentReadable(ReadingGroup group, Long userId) {
        if (canReadContent(group, userId)) return;
        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    private boolean canReadContent(ReadingGroup group, Long userId) {
        return group.getVisibility() == ReadingGroupVisibility.PUBLIC
                || isOwner(group, userId)
                || isApprovedMember(group.getId(), userId)
                || SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN");
    }

    private void assertManager(ReadingGroup group, Long userId) {
        if (isOwner(group, userId) || isManager(group.getId(), userId) || SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN")) return;
        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    private void assertApprovedMember(ReadingGroup group, Long userId) {
        if (isOwner(group, userId) || isApprovedMember(group.getId(), userId)) return;
        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    private boolean isOwner(ReadingGroup group, Long userId) {
        return userId != null && group.getOwner().getId().equals(userId);
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
