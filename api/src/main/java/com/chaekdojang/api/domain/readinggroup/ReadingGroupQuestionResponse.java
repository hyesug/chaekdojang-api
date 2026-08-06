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
@Table(name = "reading_group_question_responses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"question_id", "user_id"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReadingGroupQuestionResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private ReadingGroupQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 2000)
    private String content;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static ReadingGroupQuestionResponse create(
            ReadingGroupQuestion question, User user, String content) {
        ReadingGroupQuestionResponse value = new ReadingGroupQuestionResponse();
        value.question = question;
        value.user = user;
        value.content = content;
        return value;
    }

    public void edit(String content) {
        this.content = content;
    }
}
