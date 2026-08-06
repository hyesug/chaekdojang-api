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
@Table(name = "reading_group_questions")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReadingGroupQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_book_id", nullable = false)
    private ReadingGroupBook groupBook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(nullable = false)
    private boolean aiSuggested;

    @Column(nullable = false)
    private boolean published;

    @Column(length = 100)
    private String model;

    @Column(length = 60)
    private String promptVersion;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static ReadingGroupQuestion manual(ReadingGroupBook groupBook, User author, String question) {
        return create(groupBook, author, question, false, true, null, null);
    }

    public static ReadingGroupQuestion aiDraft(
            ReadingGroupBook groupBook, User author, String question, String model, String promptVersion) {
        return create(groupBook, author, question, true, false, model, promptVersion);
    }

    private static ReadingGroupQuestion create(
            ReadingGroupBook groupBook, User author, String question, boolean aiSuggested,
            boolean published, String model, String promptVersion) {
        ReadingGroupQuestion value = new ReadingGroupQuestion();
        value.groupBook = groupBook;
        value.author = author;
        value.question = question;
        value.aiSuggested = aiSuggested;
        value.published = published;
        value.model = model;
        value.promptVersion = promptVersion;
        return value;
    }

    public void edit(String question) {
        this.question = question;
    }

    public void publish() {
        this.published = true;
    }
}
