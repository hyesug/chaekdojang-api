package com.chaekdojang.api.domain.officialprofile;

import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "official_profile_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "user_id"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class OfficialProfileMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private OfficialProfile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfficialProfileMemberRole role = OfficialProfileMemberRole.OWNER;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static OfficialProfileMember owner(OfficialProfile profile, User user) {
        OfficialProfileMember member = new OfficialProfileMember();
        member.profile = profile;
        member.user = user;
        member.role = OfficialProfileMemberRole.OWNER;
        return member;
    }
}
