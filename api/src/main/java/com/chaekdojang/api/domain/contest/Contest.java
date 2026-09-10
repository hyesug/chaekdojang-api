package com.chaekdojang.api.domain.contest;

import com.chaekdojang.api.domain.officialprofile.OfficialProfile;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

/**
 * 공모전 1건.
 * 주최자는 항상 공식 프로필이다. 책도장이 직접 여는 공모전도 PLATFORM 타입 공식 프로필로 주최한다.
 * 주최 주체를 한 가지로 통일해두면 권한 확인·목록 조회를 한 경로로 처리할 수 있다.
 */
@Entity
@Table(name = "contests")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Contest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_profile_id", nullable = false)
    private OfficialProfile host;

    @Column(nullable = false, length = 200)
    private String title;

    /** 공모 요강 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 시상 내역 안내 */
    @Column(columnDefinition = "TEXT")
    private String prizeDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContestEntryType entryType = ContestEntryType.REVIEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContestStatus status = ContestStatus.DRAFT;

    @Column(nullable = false)
    private LocalDateTime submitStartAt;

    @Column(nullable = false)
    private LocalDateTime submitEndAt;

    /** 수상 발표 예정 시각 */
    @Column(nullable = false)
    private LocalDateTime announceAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Contest(OfficialProfile host, String title, String description, String prizeDescription,
                    ContestEntryType entryType, LocalDateTime submitStartAt, LocalDateTime submitEndAt,
                    LocalDateTime announceAt) {
        this.host = host;
        this.title = title;
        this.description = description;
        this.prizeDescription = prizeDescription;
        this.entryType = entryType == null ? ContestEntryType.REVIEW : entryType;
        this.submitStartAt = submitStartAt;
        this.submitEndAt = submitEndAt;
        this.announceAt = announceAt;
        this.status = ContestStatus.DRAFT;
    }

    public void update(String title, String description, String prizeDescription,
                       ContestEntryType entryType, LocalDateTime submitStartAt,
                       LocalDateTime submitEndAt, LocalDateTime announceAt) {
        this.title = title;
        this.description = description;
        this.prizeDescription = prizeDescription;
        if (entryType != null) {
            this.entryType = entryType;
        }
        this.submitStartAt = submitStartAt;
        this.submitEndAt = submitEndAt;
        this.announceAt = announceAt;
    }

    public void changeStatus(ContestStatus status) {
        this.status = status;
    }

    /** 책도장이 직접 주최하는 공모전인지 */
    public boolean isPlatformHosted() {
        return host.getType() == OfficialProfileType.PLATFORM;
    }

    /** 독자에게 목록·상세를 공개할 수 있는 상태인지 */
    public boolean isPublic() {
        return status != ContestStatus.DRAFT;
    }

    /** 지금 응모를 받을 수 있는지 */
    public boolean isAcceptingEntries(LocalDateTime now) {
        return status == ContestStatus.OPEN
                && !now.isBefore(submitStartAt)
                && !now.isAfter(submitEndAt);
    }
}
