package com.chaekdojang.api.domain.readinggroup;

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
@Table(name = "reading_groups")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReadingGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReadingGroupVisibility visibility = ReadingGroupVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReadingGroupJoinPolicy joinPolicy = ReadingGroupJoinPolicy.OPEN;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private ReadingGroup(User owner, String name, String slug, String description, String imageUrl,
                         ReadingGroupVisibility visibility, ReadingGroupJoinPolicy joinPolicy) {
        this.owner = owner;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.imageUrl = imageUrl;
        this.visibility = visibility == null ? ReadingGroupVisibility.PUBLIC : visibility;
        this.joinPolicy = joinPolicy == null ? ReadingGroupJoinPolicy.OPEN : joinPolicy;
    }

    public void update(String name, String description, String imageUrl,
                       ReadingGroupVisibility visibility, ReadingGroupJoinPolicy joinPolicy) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.visibility = visibility;
        this.joinPolicy = joinPolicy;
    }
}
