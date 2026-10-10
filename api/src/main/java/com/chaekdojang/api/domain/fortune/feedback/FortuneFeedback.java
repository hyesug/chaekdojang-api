package com.chaekdojang.api.domain.fortune.feedback;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

/**
 * 운세 결과 칸마다 받은 피드백. 생년월일·이름은 담지 않는다.
 */
@Entity
@Table(name = "fortune_feedback")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class FortuneFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** solo | pair */
    @Column(nullable = false, length = 8)
    private String mode;

    /** 칸 이름(예: 핵심 요약, 일과 돈) */
    @Column(nullable = false, length = 80)
    private String section;

    /** up | down */
    @Column(nullable = false, length = 8)
    private String verdict;

    /** 그 칸에 보였던 문장(이름은 웹에서 지운 뒤 보낸다) */
    @Column(length = 600)
    private String snippet;

    /** 선택 한 줄 — 연락처·출생일처럼 보이는 것은 지운 뒤 저장한다 */
    @Column(length = 300)
    private String comment;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public FortuneFeedback(String mode, String section, String verdict, String snippet, String comment) {
        this.mode = mode;
        this.section = section;
        this.verdict = verdict;
        this.snippet = snippet;
        this.comment = comment;
    }
}
