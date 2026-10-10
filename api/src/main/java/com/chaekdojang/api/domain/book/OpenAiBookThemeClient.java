package com.chaekdojang.api.domain.book;

import com.chaekdojang.api.domain.review.ai.ReviewAiSummaryProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 책 여러 권의 소개글을 한 번에 보내 주제 태그를 받는다.
 * 키·주소는 독후감 AI 요약(ai-summary)과 같은 OpenAI 연결을 쓰고, 모델은 app.book-theme.model.
 * 주제는 BookTheme 코드 안에서만 고르게 스키마로 묶고, 받은 뒤에도 목록 밖 코드는 버린다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiBookThemeClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ReviewAiSummaryProperties openAi;
    private final BookThemeProperties properties;

    /** 넣을 책 한 권 — 소개글은 앞부분만 */
    public record Input(long id, String title, String category, String description) {}

    public boolean isConfigured() {
        return openAi.getApiKey() != null && !openAi.getApiKey().isBlank();
    }

    /** @return 책 id → 주제 코드(0~3개) */
    public Map<Long, List<String>> tag(List<Input> books) {
        if (!isConfigured()) throw new IllegalStateException("OPENAI_API_KEY is not configured.");
        long started = System.currentTimeMillis();
        JsonNode response = webClient.post()
                .uri(openAi.getApiUrl())
                .header("Authorization", "Bearer " + openAi.getApiKey())
                .header("Content-Type", "application/json")
                .bodyValue(requestBody(books))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(properties.getTimeout());
        JsonNode usage = response == null ? null : response.path("usage");
        log.info("book-theme tag: model={} books={} inputTokens={} outputTokens={} latencyMs={}",
                properties.getModel(), books.size(),
                usage == null ? -1 : usage.path("input_tokens").asInt(-1),
                usage == null ? -1 : usage.path("output_tokens").asInt(-1),
                System.currentTimeMillis() - started);
        return parse(extractOutputText(response));
    }

    Map<Long, List<String>> parse(String json) {
        try {
            JsonNode items = objectMapper.readTree(json).path("items");
            Map<Long, List<String>> out = new LinkedHashMap<>();
            for (JsonNode item : items) {
                Set<String> codes = new LinkedHashSet<>();
                for (JsonNode t : item.path("themes")) {
                    BookTheme.fromCode(t.asText()).ifPresent(theme -> codes.add(theme.name()));
                }
                out.put(item.path("id").asLong(), new ArrayList<>(codes).subList(0, Math.min(3, codes.size())));
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("book theme JSON parsing failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> requestBody(List<Input> books) {
        String themes = Arrays.stream(BookTheme.values())
                .map(t -> "- " + t.name() + ": " + t.label() + " (" + t.hint() + ")")
                .collect(Collectors.joining("\n"));
        String list = books.stream()
                .map(b -> "[" + b.id() + "] " + b.title() + " / " + (b.category() == null ? "" : b.category())
                        + "\n" + b.description())
                .collect(Collectors.joining("\n\n"));
        List<String> codes = Arrays.stream(BookTheme.values()).map(Enum::name).toList();
        return Map.of(
                "model", properties.getModel(),
                "input", List.of(
                        Map.of("role", "system", "content", """
                                책 소개글을 읽고, 그 책이 독자에게 도움이 되는 주제를 아래 목록에서 1~3개 고르세요.
                                소개글에 근거가 없으면 고르지 말고 빈 배열을 두세요. 반드시 JSON만 반환하세요.
                                주제 목록:
                                """ + themes),
                        Map.of("role", "user", "content", list)
                ),
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "book_themes",
                        "strict", true,
                        "schema", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of("items", Map.of(
                                        "type", "array",
                                        "items", Map.of(
                                                "type", "object",
                                                "additionalProperties", false,
                                                "properties", Map.of(
                                                        "id", Map.of("type", "integer"),
                                                        "themes", Map.of("type", "array", "maxItems", 3,
                                                                "items", Map.of("type", "string", "enum", codes))),
                                                "required", List.of("id", "themes")))),
                                "required", List.of("items")))),
                // 책 한 권에 코드 세 개 안팎 — 권당 40토큰이면 넉넉하다
                "max_output_tokens", 200 + books.size() * 40
        );
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) throw new IllegalStateException("OpenAI response is empty.");
        for (JsonNode outputItem : response.path("output")) {
            for (JsonNode contentItem : outputItem.path("content")) {
                JsonNode text = contentItem.path("text");
                if (text.isTextual() && !text.asText().isBlank()) return text.asText();
            }
        }
        JsonNode outputText = response.path("output_text");
        if (outputText.isTextual() && !outputText.asText().isBlank()) return outputText.asText();
        throw new IllegalStateException("OpenAI response did not contain output text.");
    }
}
