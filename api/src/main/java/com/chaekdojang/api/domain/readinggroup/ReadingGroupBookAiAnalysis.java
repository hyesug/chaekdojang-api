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
@Table(name = "reading_group_book_ai_analyses")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReadingGroupBookAiAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_book_id", nullable = false, unique = true)
    private ReadingGroupBook groupBook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String analysisJson;

    @Column(nullable = false, length = 64)
    private String sourceHash;

    @Column(nullable = false)
    private int reviewCount;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false, length = 60)
    private String promptVersion;

    @Column(nullable = false)
    private long inputTokens;

    @Column(nullable = false)
    private long outputTokens;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static ReadingGroupBookAiAnalysis create(
            ReadingGroupBook groupBook, User generatedBy, String analysisJson,
            String sourceHash, int reviewCount, String model, String promptVersion,
            long inputTokens, long outputTokens) {
        ReadingGroupBookAiAnalysis value = new ReadingGroupBookAiAnalysis();
        value.groupBook = groupBook;
        value.replace(generatedBy, analysisJson, sourceHash, reviewCount, model, promptVersion,
                inputTokens, outputTokens);
        return value;
    }

    public void replace(
            User generatedBy, String analysisJson, String sourceHash, int reviewCount,
            String model, String promptVersion, long inputTokens, long outputTokens) {
        this.generatedBy = generatedBy;
        this.analysisJson = analysisJson;
        this.sourceHash = sourceHash;
        this.reviewCount = reviewCount;
        this.model = model;
        this.promptVersion = promptVersion;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
    }
}
