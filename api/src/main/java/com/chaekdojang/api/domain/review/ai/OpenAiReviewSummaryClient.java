package com.chaekdojang.api.domain.review.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiReviewSummaryClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ReviewAiSummaryProperties properties;

    public AiSummaryResult summarize(String content) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured.");
        }

        JsonNode response = webClient.post()
                .uri(properties.getApiUrl())
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .bodyValue(requestBody(content))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(properties.getTimeout());

        String text = extractOutputText(response);
        try {
            AiSummaryResult result = normalize(objectMapper.readValue(text, AiSummaryResult.class));
            validate(result);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("AI summary JSON parsing failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> requestBody(String content) {
        return Map.of(
                "model", properties.getModel(),
                "input", List.of(
                        Map.of(
                                "role", "system",
                                "content", """
                                        당신은 책도장 독후감 요약카드를 만드는 편집자입니다.
                                        반드시 JSON만 반환하세요. 사용자의 독후감 본문에 없는 사실을 만들지 마세요.
                                        recommendedFor는 반드시 마지막 단어가 "사람"이어야 합니다.
                                        예: 익숙한 일상의 의미를 다시 보고 싶은 사람
                                        """
                        ),
                        Map.of(
                                "role", "user",
                                "content", "독후감 본문:\n" + content
                        )
                ),
                "text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", "review_ai_summary",
                                "strict", true,
                                "schema", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "properties", Map.of(
                                                "oneLineReview", Map.of("type", "string", "maxLength", 60),
                                                "emotionKeywords", Map.of(
                                                        "type", "array",
                                                        "minItems", 3,
                                                        "maxItems", 5,
                                                        "items", Map.of("type", "string", "maxLength", 50)
                                                ),
                                                "recommendedFor", Map.of("type", "string", "maxLength", 120),
                                                "impressivePoint", Map.of("type", "string", "maxLength", 100)
                                        ),
                                        "required", List.of(
                                                "oneLineReview",
                                                "emotionKeywords",
                                                "recommendedFor",
                                                "impressivePoint"
                                        )
                                )
                        )
                ),
                "max_output_tokens", 500
        );
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("OpenAI response is empty.");
        }
        JsonNode output = response.path("output");
        if (output.isArray()) {
            for (JsonNode outputItem : output) {
                JsonNode content = outputItem.path("content");
                if (!content.isArray()) continue;
                for (JsonNode contentItem : content) {
                    JsonNode text = contentItem.path("text");
                    if (text.isTextual() && !text.asText().isBlank()) {
                        return text.asText();
                    }
                }
            }
        }
        JsonNode outputText = response.path("output_text");
        if (outputText.isTextual() && !outputText.asText().isBlank()) {
            return outputText.asText();
        }
        throw new IllegalStateException("OpenAI response did not contain output text.");
    }

    private AiSummaryResult normalize(AiSummaryResult result) {
        return new AiSummaryResult(
                trim(result.oneLineReview()),
                result.emotionKeywords().stream()
                        .map(this::trim)
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .limit(5)
                        .toList(),
                normalizeRecommendedFor(result.recommendedFor()),
                trim(result.impressivePoint())
        );
    }

    private String normalizeRecommendedFor(String value) {
        String normalized = trim(value);
        if (normalized.isBlank() || normalized.endsWith("사람")) {
            return normalized;
        }
        return normalized + " 사람";
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void validate(AiSummaryResult result) {
        if (result.oneLineReview() == null || result.oneLineReview().isBlank()
                || result.oneLineReview().length() > 60) {
            throw new IllegalStateException("oneLineReview must be 1-60 characters.");
        }
        if (result.emotionKeywords().size() < 3 || result.emotionKeywords().size() > 5) {
            throw new IllegalStateException("emotionKeywords must contain 3-5 items.");
        }
        if (result.recommendedFor() == null || result.recommendedFor().isBlank()
                || result.recommendedFor().length() > 120
                || !result.recommendedFor().endsWith("사람")) {
            throw new IllegalStateException("recommendedFor must be 1-120 characters and end with '사람'.");
        }
        if (result.impressivePoint() == null || result.impressivePoint().isBlank()
                || result.impressivePoint().length() > 100) {
            throw new IllegalStateException("impressivePoint must be 1-100 characters.");
        }
    }
}
