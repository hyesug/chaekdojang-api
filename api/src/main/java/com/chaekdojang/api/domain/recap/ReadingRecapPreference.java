package com.chaekdojang.api.domain.recap;

import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "reading_recap_preferences")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReadingRecapPreference {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
    @Column(nullable = false) private boolean enabled = true;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ReadingRecapFrequency frequency;
    @Column(nullable = false) private boolean includeFollowing;
    @Column(nullable = false) private boolean includeMemories;
    @Column private LocalDateTime lastDeliveredAt;
    @CreationTimestamp @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(nullable = false) private LocalDateTime updatedAt;

    public static ReadingRecapPreference create(
            User user, ReadingRecapFrequency frequency, boolean includeFollowing, boolean includeMemories) {
        ReadingRecapPreference value = new ReadingRecapPreference();
        value.user = user;
        value.update(frequency, includeFollowing, includeMemories);
        return value;
    }

    public void update(ReadingRecapFrequency frequency, boolean includeFollowing, boolean includeMemories) {
        this.enabled = true;
        this.frequency = frequency;
        this.includeFollowing = includeFollowing;
        this.includeMemories = includeMemories;
    }

    public void disable() { this.enabled = false; }
    public void markDelivered() { this.lastDeliveredAt = LocalDateTime.now(); }
}
