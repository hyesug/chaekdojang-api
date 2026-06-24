package com.chaekdojang.api.domain.officialprofile;

import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "official_profile_applications")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class OfficialProfileApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfficialProfileType type;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 500)
    private String officialUrl;

    @Column(nullable = false, length = 255)
    private String contactEmail;

    @Column(length = 500)
    private String proofUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfficialProfileApplicationStatus status = OfficialProfileApplicationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private OfficialProfile profile;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private OfficialProfileApplication(User applicant, OfficialProfileType type, String displayName,
                                       String bio, String officialUrl, String contactEmail, String proofUrl) {
        this.applicant = applicant;
        this.type = type;
        this.displayName = displayName;
        this.bio = bio;
        this.officialUrl = officialUrl;
        this.contactEmail = contactEmail;
        this.proofUrl = proofUrl;
    }

    public void approve(OfficialProfile profile, String reviewNote) {
        this.status = OfficialProfileApplicationStatus.APPROVED;
        this.profile = profile;
        this.reviewNote = reviewNote;
    }

    public void reject(String reviewNote) {
        this.status = OfficialProfileApplicationStatus.REJECTED;
        this.reviewNote = reviewNote;
    }
}
