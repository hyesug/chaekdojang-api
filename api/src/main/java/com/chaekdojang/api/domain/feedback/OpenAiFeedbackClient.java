package com.chaekdojang.api.domain.feedback;

import com.chaekdojang.api.domain.feedback.dto.FeedbackResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiFeedbackClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final FeedbackProperties properties;

    public FeedbackResult createFeedback(String content) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured.");
        }
        if (properties.getModel() == null || properties.getModel().isBlank()) {
            throw new IllegalStateException("OPENAI_REVIEW_SUMMARY_MODEL or LLM_MODEL is not configured.");
        }

        RuntimeException lastError = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return parseFeedback(callOpenAi(content));
            } catch (RuntimeException e) {
                lastError = e;
            }
        }
        throw lastError != null ? lastError : new IllegalStateException("Feedback generation failed.");
    }

    private JsonNode callOpenAi(String content) {
        return webClient.post()
                .uri(properties.getApiUrl())
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .bodyValue(requestBody(content))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(properties.getTimeout());
    }

    private Map<String, Object> requestBody(String content) {
        return Map.of(
                "model", properties.getModel(),
                "input", List.of(
                        Map.of(
                                "role", "system",
                                "content", FeedbackPrompt.SYSTEM_PROMPT
                        ),
                        Map.of(
                                "role", "user",
                                "content", "<독후감>\n" + content + "\n</독후감>"
                        )
                ),
                "text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", "review_feedback",
                                "strict", true,
                                "schema", feedbackSchema()
                        )
                ),
                "max_output_tokens", properties.getMaxTokens()
        );
    }

    private Map<String, Object> feedbackSchema() {
        Map<String, Object> nullableString = Map.of("type", List.of("string", "null"));
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "not_review", Map.of("type", "boolean"),
                        "message", nullableString,
                        "core_theme", nullableString,
                        "strengths", Map.of(
                                "type", "array",
                                "minItems", 0,
                                "maxItems", 2,
                                "items", Map.of("type", "string")
                        ),
                        "improvements", Map.of(
                                "type", "array",
                                "minItems", 0,
                                "maxItems", 2,
                                "items", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "properties", Map.of(
                                                "point", Map.of("type", "string"),
                                                "before", Map.of("type", "string"),
                                                "direction", Map.of("type", "string"),
                                                "after", Map.of("type", "string"),
                                                "reason", Map.of("type", "string")
                                        ),
                                        "required", List.of("point", "before", "direction", "after", "reason")
                                )
                        ),
                        "sentence_examples", Map.of(
                                "type", "array",
                                "minItems", 0,
                                "maxItems", 2,
                                "items", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "properties", Map.of(
                                                "before", Map.of("type", "string"),
                                                "after", Map.of("type", "string")
                                        ),
                                        "required", List.of("before", "after")
                                )
                        ),
                        "title_suggestions", Map.of(
                                "type", "array",
                                "minItems", 0,
                                "maxItems", 3,
                                "items", Map.of("type", "string")
                        ),
                        "deep_question", nullableString
                ),
                "required", List.of(
                        "not_review",
                        "message",
                        "core_theme",
                        "strengths",
                        "improvements",
                        "sentence_examples",
                        "title_suggestions",
                        "deep_question"
                )
        );
    }

    private FeedbackResult parseFeedback(JsonNode response) {
        String text = extractText(response);
        try {
            FeedbackResult result = objectMapper.readValue(text, FeedbackResult.class);
            validate(result);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Feedback JSON parsing failed: " + e.getMessage(), e);
        }
    }

    private String extractText(JsonNode response) {
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

    private void validate(FeedbackResult result) {
        if (result == null) {
            throw new IllegalStateException("Feedback result is empty.");
        }
        if (result.notReview()) {
            if (isBlank(result.message())) {
                throw new IllegalStateException("not_review feedback must include message.");
            }
            return;
        }
        if (isBlank(result.coreTheme())
                || result.strengths() == null || result.strengths().size() < 2
                || result.improvements() == null || result.improvements().size() < 2
                || result.sentenceExamples() == null || result.sentenceExamples().isEmpty()
                || result.titleSuggestions() == null || result.titleSuggestions().size() < 2
                || isBlank(result.deepQuestion())) {
            throw new IllegalStateException("Feedback result does not match required schema.");
        }
        result.improvements().forEach(improvement -> {
            if (isBlank(improvement.point())
                    || isBlank(improvement.before())
                    || isBlank(improvement.direction())
                    || isBlank(improvement.after())
                    || isBlank(improvement.reason())) {
                throw new IllegalStateException("Improvement must include point, before, direction, after, and reason.");
            }
            if (normalizeForComparison(improvement.before()).equals(normalizeForComparison(improvement.after()))) {
                throw new IllegalStateException("Improvement before and after must be different.");
            }
        });
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeForComparison(String value) {
        return value == null
                ? ""
                : value.replaceAll("[\\s\\p{Punct}·…]+", "").trim();
    }
}
