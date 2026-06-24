package com.chaekdojang.api.domain.readinggroup;

import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "reading_group_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReadingGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private ReadingGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReadingGroupMemberRole role = ReadingGroupMemberRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReadingGroupMemberStatus status = ReadingGroupMemberStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static ReadingGroupMember owner(ReadingGroup group, User user) {
        ReadingGroupMember member = new ReadingGroupMember();
        member.group = group;
        member.user = user;
        member.role = ReadingGroupMemberRole.OWNER;
        member.status = ReadingGroupMemberStatus.APPROVED;
        return member;
    }

    public static ReadingGroupMember join(ReadingGroup group, User user, ReadingGroupMemberStatus status) {
        ReadingGroupMember member = new ReadingGroupMember();
        member.group = group;
        member.user = user;
        member.role = ReadingGroupMemberRole.MEMBER;
        member.status = status;
        return member;
    }

    public void approve() {
        this.status = ReadingGroupMemberStatus.APPROVED;
    }

    public boolean canManage() {
        return status == ReadingGroupMemberStatus.APPROVED
                && (role == ReadingGroupMemberRole.OWNER || role == ReadingGroupMemberRole.MANAGER);
    }
}
