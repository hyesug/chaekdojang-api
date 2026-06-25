package com.chaekdojang.api.domain.admin.dto;

import com.chaekdojang.api.domain.readinggroup.ReadingGroup;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupBook;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupJoinPolicy;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupMember;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupMemberRole;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupMemberStatus;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupReview;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupVisibility;
import com.chaekdojang.api.domain.review.Review;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AdminReadingGroupDetailResponse(
        Long id,
        String name,
        String slug,
        String description,
        ReadingGroupVisibility visibility,
        ReadingGroupJoinPolicy joinPolicy,
        boolean joinEnabled,
        Long ownerId,
        String ownerNickname,
        long memberCount,
        long pendingCount,
        long bookCount,
        long reviewCount,
        LocalDateTime createdAt,
        List<MemberItem> members,
        List<BookItem> books,
        List<ReviewItem> reviews
) {
    public static AdminReadingGroupDetailResponse of(
            ReadingGroup group,
            List<ReadingGroupMember> members,
            List<ReadingGroupBook> books,
            List<ReadingGroupReview> reviews,
            Map<Long, Long> reviewCountByGroupBookId
    ) {
        return new AdminReadingGroupDetailResponse(
                group.getId(),
                group.getName(),
                group.getSlug(),
                group.getDescription(),
                group.getVisibility(),
                group.getJoinPolicy(),
                group.isJoinEnabled(),
                group.getOwner().getId(),
                group.getOwner().getNickname(),
                members.stream().filter(member -> member.getStatus() == ReadingGroupMemberStatus.APPROVED).count(),
                members.stream().filter(member -> member.getStatus() == ReadingGroupMemberStatus.PENDING).count(),
                books.size(),
                reviews.size(),
                group.getCreatedAt(),
                members.stream().map(MemberItem::from).toList(),
                books.stream().map(book -> BookItem.of(book, reviewCountByGroupBookId.getOrDefault(book.getId(), 0L))).toList(),
                reviews.stream().map(ReviewItem::from).toList()
        );
    }

    public record MemberItem(
            Long id,
            Long userId,
            String nickname,
            String profileImage,
            ReadingGroupMemberRole role,
            ReadingGroupMemberStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        private static MemberItem from(ReadingGroupMember member) {
            return new MemberItem(
                    member.getId(),
                    member.getUser().getId(),
                    member.getUser().getNickname(),
                    member.getUser().getProfileImage(),
                    member.getRole(),
                    member.getStatus(),
                    member.getCreatedAt(),
                    member.getUpdatedAt()
            );
        }
    }

    public record BookItem(
            Long id,
            Long bookId,
            String title,
            String author,
            String publisher,
            String thumbnail,
            String note,
            long reviewCount,
            LocalDateTime createdAt
    ) {
        private static BookItem of(ReadingGroupBook groupBook, long reviewCount) {
            return new BookItem(
                    groupBook.getId(),
                    groupBook.getBook().getId(),
                    groupBook.getBook().getTitle(),
                    groupBook.getBook().getAuthor(),
                    groupBook.getBook().getPublisher(),
                    groupBook.getBook().getThumbnail(),
                    groupBook.getNote(),
                    reviewCount,
                    groupBook.getCreatedAt()
            );
        }
    }

    public record ReviewItem(
            Long id,
            Long groupBookId,
            Long reviewId,
            Long authorId,
            String authorNickname,
            Long bookId,
            String bookTitle,
            int rating,
            boolean hidden,
            String content,
            LocalDateTime createdAt
    ) {
        private static ReviewItem from(ReadingGroupReview groupReview) {
            Review review = groupReview.getReview();
            return new ReviewItem(
                    groupReview.getId(),
                    groupReview.getGroupBook().getId(),
                    review.getId(),
                    review.getAuthor().getId(),
                    review.getAuthor().getNickname(),
                    review.getBook() == null ? null : review.getBook().getId(),
                    review.getBook() == null ? null : review.getBook().getTitle(),
                    review.getRating(),
                    review.isHidden(),
                    review.getContent(),
                    groupReview.getCreatedAt()
            );
        }
    }
}
