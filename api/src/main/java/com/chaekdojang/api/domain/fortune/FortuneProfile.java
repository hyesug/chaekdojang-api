package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

/**
 * 운세 화면에서 저장한 본인 출생 정보. 사용자당 하나.
 * 개인정보라 soft delete 하지 않고, 지우라고 하면 행을 바로 지운다.
 */
@Entity
@Table(name = "fortune_profiles")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class FortuneProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false, length = 10)
    private String gender;

    @Column(name = "birth_year", nullable = false)
    private int birthYear;

    @Column(name = "birth_month", nullable = false)
    private int birthMonth;

    @Column(name = "birth_day", nullable = false)
    private int birthDay;

    @Column(name = "birth_hour")
    private Integer birthHour;

    @Column(name = "birth_minute")
    private Integer birthMinute;

    @Column(name = "birth_place", nullable = false, length = 40)
    private String birthPlace;

    @Column(name = "home_place", nullable = false, length = 40)
    private String homePlace;

    @Column(nullable = false)
    private boolean dst;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static FortuneProfile create(User user) {
        FortuneProfile profile = new FortuneProfile();
        profile.user = user;
        return profile;
    }

    public void update(String name, String gender, int birthYear, int birthMonth, int birthDay,
                       Integer birthHour, Integer birthMinute, String birthPlace, String homePlace, boolean dst) {
        this.name = name;
        this.gender = gender;
        this.birthYear = birthYear;
        this.birthMonth = birthMonth;
        this.birthDay = birthDay;
        this.birthHour = birthHour;
        this.birthMinute = birthHour == null ? null : birthMinute;
        this.birthPlace = birthPlace;
        this.homePlace = homePlace;
        this.dst = dst;
    }
}
