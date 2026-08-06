package com.chaekdojang.api.domain.review.reflection.dto;

import com.chaekdojang.api.domain.review.reflection.ChangeComparisonResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeComparisonUpdateRequest(
        @NotBlank @Size(max = 800) String previousFocus,
        @NotBlank @Size(max = 800) String currentFocus,
        @NotBlank @Size(max = 800) String sharedThought,
        @NotBlank @Size(max = 800) String changedPerspective,
        @NotBlank @Size(max = 800) String newElement,
        @NotBlank @Size(max = 600) String reflectionQuestion
) {
    public ChangeComparisonResult toResult() {
        return new ChangeComparisonResult(
                previousFocus.trim(), currentFocus.trim(), sharedThought.trim(),
                changedPerspective.trim(), newElement.trim(), reflectionQuestion.trim(), 0, 0);
    }
}
