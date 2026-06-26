package com.chaekdojang.api.domain.user;

import com.chaekdojang.api.domain.book.Book;
import jakarta.persistence.*;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이메일이 없는 OAuth 제공자(카카오 비동의 등)도 가입 가능하도록 nullable
    @Column(unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column
    private String profileImage;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(nullable = false, length = 10)
    private String country;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private boolean onboardingCompleted = false;

    @Column(columnDefinition = "TEXT")
    private String preferredGenres;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private UserRole role = UserRole.USER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "life_book_id")
    private Book lifeBook;

    @Column
    private Integer readingGoalYear;

    @Column
    private Integer readingGoalCount;

    public static User create(String email, String nickname, String profileImage) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.profileImage = profileImage;
        user.language = "ko";
        user.country = "KR";
        user.role = UserRole.USER;
        return user;
    }

    public void updateLifeBook(Book book) {
        this.lifeBook = book;
    }

    public void updateReadingGoal(Integer year, Integer targetCount) {
        this.readingGoalYear = year;
        this.readingGoalCount = targetCount;
    }

    public void updateProfile(String nickname, String bio, String profileImage) {
        if (nickname != null) this.nickname = nickname;
        if (bio != null) this.bio = bio;
        if (profileImage != null) this.profileImage = profileImage.isBlank() ? null : profileImage;
    }

    public void completeOnboarding(String preferredGenres) {
        this.preferredGenres = preferredGenres;
        this.onboardingCompleted = true;
    }

    public void anonymizeForDeletion(String deletedNickname) {
        this.email = null;
        this.nickname = deletedNickname;
        this.profileImage = null;
        this.bio = null;
        this.lifeBook = null;
        this.preferredGenres = null;
        this.readingGoalYear = null;
        this.readingGoalCount = null;
        this.onboardingCompleted = false;
        this.role = UserRole.USER;
        this.deletedAt = LocalDateTime.now();
    }

    public void reactivate() {
        this.deletedAt = null;
    }

    public void promoteToAdmin() { this.role = UserRole.ADMIN; }
    public void demoteToUser() { this.role = UserRole.USER; }
    public void setSuperAdmin() { this.role = UserRole.SUPER_ADMIN; }
    public boolean isAdmin() { return this.role == UserRole.ADMIN || this.role == UserRole.SUPER_ADMIN; }
    public boolean isSuperAdmin() { return this.role == UserRole.SUPER_ADMIN; }
}
