package com.chaekdojang.api.domain.readinggoal;

import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "reading_goals", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "goal_year"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReadingGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "goal_year", nullable = false)
    private int year;

    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @Column(nullable = false)
    private boolean publicVisible = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static ReadingGoal create(User user, int year, int targetCount, boolean publicVisible) {
        ReadingGoal goal = new ReadingGoal();
        goal.user = user;
        goal.year = year;
        goal.targetCount = targetCount;
        goal.publicVisible = publicVisible;
        return goal;
    }

    public void update(int targetCount, boolean publicVisible) {
        this.targetCount = targetCount;
        this.publicVisible = publicVisible;
    }
}
