package com.chaekdojang.api.domain.officialprofile;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "official_profiles")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class OfficialProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfficialProfileType type;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String officialUrl;

    @Column(length = 500)
    private String instagramUrl;

    @Column(length = 500)
    private String brunchUrl;

    @Column(length = 500)
    private String tumblbugUrl;

    @Column(length = 255)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfficialProfileStatus status = OfficialProfileStatus.DRAFT;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private boolean featured = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private OfficialProfile(OfficialProfileType type, String displayName, String slug, String bio,
                            String imageUrl, String officialUrl, String instagramUrl, String brunchUrl,
                            String tumblbugUrl, String contactEmail) {
        this.type = type;
        this.displayName = displayName;
        this.slug = slug;
        this.bio = bio;
        this.imageUrl = imageUrl;
        this.officialUrl = officialUrl;
        this.instagramUrl = instagramUrl;
        this.brunchUrl = brunchUrl;
        this.tumblbugUrl = tumblbugUrl;
        this.contactEmail = contactEmail;
        this.status = OfficialProfileStatus.ACTIVE;
    }

    public void update(String displayName, String bio, String imageUrl, String officialUrl,
                       String instagramUrl, String brunchUrl, String tumblbugUrl, String contactEmail,
                       OfficialProfileStatus status, boolean verified, boolean featured) {
        this.displayName = displayName;
        this.bio = bio;
        this.imageUrl = imageUrl;
        this.officialUrl = officialUrl;
        this.instagramUrl = instagramUrl;
        this.brunchUrl = brunchUrl;
        this.tumblbugUrl = tumblbugUrl;
        this.contactEmail = contactEmail;
        this.status = status;
        this.verified = verified;
        this.featured = featured;
    }
}
