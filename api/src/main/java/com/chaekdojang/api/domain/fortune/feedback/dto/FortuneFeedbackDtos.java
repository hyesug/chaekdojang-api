package com.chaekdojang.api.domain.fortune.feedback.dto;

import com.chaekdojang.api.domain.fortune.feedback.FortuneFeedback;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class FortuneFeedbackDtos {

    private FortuneFeedbackDtos() {}

    public record CreateRequest(
            @NotBlank @Size(max = 8) String mode,
            @NotBlank @Size(max = 80) String section,
            @NotBlank @Size(max = 8) String verdict,
            @Size(max = 2000) String snippet,
            @Size(max = 1000) String comment
    ) {}

    public record SectionCount(String section, long up, long down) {}

    public record Item(Long id, String mode, String section, String verdict, String snippet, String comment, LocalDateTime createdAt) {
        public static Item from(FortuneFeedback f) {
            return new Item(f.getId(), f.getMode(), f.getSection(), f.getVerdict(), f.getSnippet(), f.getComment(), f.getCreatedAt());
        }
    }

    public record Summary(List<SectionCount> sections, List<Item> recent) {}
}
