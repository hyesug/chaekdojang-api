package com.chaekdojang.api.security;

import com.chaekdojang.api.domain.feedback.dto.FeedbackConfigResponse;
import com.chaekdojang.api.domain.review.ai.dto.ReviewAiSummaryResponse;
import com.chaekdojang.api.domain.user.dto.PublicUserProfileResponse;
import com.chaekdojang.api.domain.user.dto.UserRecommendationResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PublicResponseExposureTest {

    @Test
    void publicDtosDoNotExposePromptsScoresAdminOrOperationalFields() {
        assertThat(fields(FeedbackConfigResponse.class))
                .doesNotContain("prompt", "systemPrompt", "apiKey");
        assertThat(fields(UserRecommendationResponse.class))
                .doesNotContain("score", "recommendationScore");
        assertThat(fields(PublicUserProfileResponse.class))
                .doesNotContain("role", "onboardingCompleted", "preferredGenres", "email");
        assertThat(fields(ReviewAiSummaryResponse.class))
                .doesNotContain("retryCount", "errorMessage", "summarySource", "jobId", "prompt");
    }

    private String[] fields(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
                .map(RecordComponent::getName)
                .toArray(String[]::new);
    }
}
