package com.chaekdojang.api.domain.lotto;

import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LottoFutureValidationService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final List<String> MODELS = List.of("A", "D", "E", "F", "H");
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final LottoFuturePredictionGenerator generator;

    @Transactional(readOnly = true)
    public List<RoundResponse> list(Long userId) {
        assertAdmin(userId);
        List<RoundRow> rounds = jdbcTemplate.query("""
                SELECT id, draw_round, draw_date, draw_time, timezone, time_source, time_source_detail,
                       generated_at, actual_numbers::text, result_confirmed_at
                FROM lotto_future_prediction_rounds ORDER BY draw_round DESC
                """, (rs, rowNum) -> new RoundRow(rs.getLong(1), rs.getInt(2), rs.getObject(3, LocalDate.class),
                rs.getObject(4, LocalTime.class), rs.getString(5), rs.getString(6), rs.getString(7),
                rs.getObject(8, LocalDateTime.class), rs.getString(9), rs.getObject(10, LocalDateTime.class)));
        return rounds.stream().map(this::response).toList();
    }

    @Transactional
    public RoundResponse generateNext() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        LocalDate date = LottoFutureValidationPolicy.nextDrawDate(now.toLocalDate());
        if (date.equals(now.toLocalDate()) && !now.toLocalTime().isBefore(LottoFutureValidationPolicy.DEFAULT_DRAW_TIME)) {
            date = date.plusWeeks(1);
        }
        return generate(LottoFutureValidationPolicy.roundOf(date), date, LottoFutureValidationPolicy.DEFAULT_DRAW_TIME,
                "official_default", "기본 추첨 시각 20:35 KST", "자동 생성");
    }

    @Transactional
    public RoundResponse generateNext(Long userId) {
        assertAdmin(userId);
        return generateNext();
    }

    @Transactional
    public RoundResponse reviseTime(Long userId, int round, LocalTime drawTime, String reason) {
        assertAdmin(userId);
        RoundRow existing = roundRow(round);
        if (drawTime == null || reason == null || reason.isBlank()) throw new CustomException(ErrorCode.INVALID_REQUEST);
        if (!ZonedDateTime.of(existing.drawDate, existing.drawTime, KST).isAfter(ZonedDateTime.now(KST))) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (existing.drawTime.equals(drawTime)) return response(existing);
        jdbcTemplate.update("UPDATE lotto_future_prediction_rounds SET draw_time = ?, time_source = ?, time_source_detail = ? WHERE id = ?",
                drawTime, "official_schedule_override", reason.trim(), existing.id);
        jdbcTemplate.update("UPDATE lotto_future_predictions SET active = false WHERE prediction_round_id = ? AND active", existing.id);
        LottoFuturePredictionGenerator.Generated generated = generator.generate(round, existing.drawDate, drawTime, history());
        insertPredictions(existing.id, generated, reason.trim(), nextRevision(existing.id));
        return response(roundRow(round));
    }

    @Transactional
    public RoundResponse reviseModel(Long userId, int round, String reason) {
        assertAdmin(userId);
        RoundRow existing = roundRow(round);
        if (reason == null || reason.isBlank() || !ZonedDateTime.of(existing.drawDate, existing.drawTime, KST).isAfter(ZonedDateTime.now(KST))) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        LottoFuturePredictionGenerator.Generated generated = generator.generate(round, existing.drawDate, existing.drawTime, history());
        jdbcTemplate.update("UPDATE lotto_future_predictions SET active = false WHERE prediction_round_id = ? AND active", existing.id);
        insertPredictions(existing.id, generated, reason.trim(), nextRevision(existing.id));
        return response(roundRow(round));
    }

    @Transactional
    public RoundResponse confirmResult(Long userId, int round, List<Integer> numbers) {
        assertAdmin(userId);
        LottoFutureValidationPolicy.requireTicket(numbers);
        RoundRow existing = roundRow(round);
        if (ZonedDateTime.of(existing.drawDate, existing.drawTime, KST).isAfter(ZonedDateTime.now(KST))) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        jdbcTemplate.update("UPDATE lotto_future_prediction_rounds SET actual_numbers = ?::jsonb, result_confirmed_at = ? WHERE id = ?",
                json(numbers), LocalDateTime.now(KST), existing.id);
        return response(roundRow(round));
    }

    private RoundResponse generate(int round, LocalDate drawDate, LocalTime drawTime, String timeSource, String detail, String reason) {
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(?)", (rs, rowNum) -> 0, (long) round);
        List<Long> ids = jdbcTemplate.query("SELECT id FROM lotto_future_prediction_rounds WHERE draw_round = ?", (rs, rowNum) -> rs.getLong(1), round);
        if (!ids.isEmpty()) return response(roundRow(round));
        LottoFuturePredictionGenerator.Generated generated = generator.generate(round, drawDate, drawTime, history());
        LocalDateTime now = LocalDateTime.now(KST);
        jdbcTemplate.update("""
                INSERT INTO lotto_future_prediction_rounds
                (draw_round, draw_date, draw_time, timezone, time_source, time_source_detail, generated_at)
                VALUES (?, ?, ?, 'Asia/Seoul', ?, ?, ?)
                """, round, drawDate, drawTime, timeSource, detail, now);
        long roundId = jdbcTemplate.queryForObject("SELECT id FROM lotto_future_prediction_rounds WHERE draw_round = ?", Long.class, round);
        insertPredictions(roundId, generated, reason, 1);
        return response(roundRow(round));
    }

    private void insertPredictions(long roundId, LottoFuturePredictionGenerator.Generated generated, String reason, int revision) {
        Map<String, List<Integer>> predictions = generated.predictions();
        for (String model : MODELS) {
            List<Integer> ticket = predictions.get(model);
            LottoFutureValidationPolicy.requireTicket(ticket);
            jdbcTemplate.update("""
                    INSERT INTO lotto_future_predictions
                    (prediction_round_id, model, revision, numbers, model_version, source_commit, generated_at, reason, active)
                    VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, true)
                    """, roundId, model, revision, json(ticket), generated.modelVersion(), generated.sourceCommit(),
                    LocalDateTime.now(KST), reason);
        }
    }

    private int nextRevision(long roundId) {
        Integer value = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(revision), 0) + 1 FROM lotto_future_predictions WHERE prediction_round_id = ?", Integer.class, roundId);
        return value == null ? 1 : value;
    }

    private List<LottoFuturePredictionGenerator.History> history() {
        return jdbcTemplate.query("""
                SELECT draw_round, draw_date, draw_time, actual_numbers::text
                FROM lotto_future_prediction_rounds
                WHERE actual_numbers IS NOT NULL ORDER BY draw_round
                """, (rs, rowNum) -> new LottoFuturePredictionGenerator.History(rs.getInt(1),
                rs.getObject(2, LocalDate.class), rs.getObject(3, LocalTime.class), numbers(rs.getString(4))));
    }

    private RoundResponse response(RoundRow round) {
        List<PredictionResponse> predictions = jdbcTemplate.query("""
                SELECT model, revision, numbers::text, model_version, source_commit, generated_at, reason, active
                FROM lotto_future_predictions WHERE prediction_round_id = ? ORDER BY model, revision DESC
                """, (rs, rowNum) -> {
            List<Integer> ticket = numbers(rs.getString(3));
            List<Integer> actual = round.actualNumbers == null ? null : numbers(round.actualNumbers);
            List<Integer> hits = actual == null ? List.of() : ticket.stream().filter(actual::contains).toList();
            return new PredictionResponse(rs.getString(1), rs.getInt(2), ticket, rs.getString(4), rs.getString(5),
                    rs.getObject(6, LocalDateTime.class), rs.getString(7), rs.getBoolean(8), hits.size(), hits);
        }, round.id);
        return new RoundResponse(round.round, round.drawDate, round.drawTime, round.timezone, round.timeSource,
                round.timeSourceDetail, round.generatedAt, round.actualNumbers == null ? null : numbers(round.actualNumbers),
                round.resultConfirmedAt, predictions);
    }

    private RoundRow roundRow(int round) {
        List<RoundRow> values = jdbcTemplate.query("""
                SELECT id, draw_round, draw_date, draw_time, timezone, time_source, time_source_detail,
                       generated_at, actual_numbers::text, result_confirmed_at
                FROM lotto_future_prediction_rounds WHERE draw_round = ?
                """, (rs, rowNum) -> new RoundRow(rs.getLong(1), rs.getInt(2), rs.getObject(3, LocalDate.class),
                rs.getObject(4, LocalTime.class), rs.getString(5), rs.getString(6), rs.getString(7),
                rs.getObject(8, LocalDateTime.class), rs.getString(9), rs.getObject(10, LocalDateTime.class)), round);
        if (values.isEmpty()) throw new CustomException(ErrorCode.NOT_FOUND);
        return values.getFirst();
    }

    private void assertAdmin(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!user.isAdmin()) throw new CustomException(ErrorCode.FORBIDDEN);
    }

    private List<Integer> numbers(String source) {
        try { return objectMapper.readValue(source, new TypeReference<>() {}); }
        catch (Exception exception) { throw new IllegalStateException("로또 번호 저장값을 읽지 못했습니다.", exception); }
    }
    private String json(List<Integer> values) {
        try { return objectMapper.writeValueAsString(values); }
        catch (Exception exception) { throw new IllegalStateException("로또 번호 저장값을 만들지 못했습니다.", exception); }
    }

    private record RoundRow(long id, int round, LocalDate drawDate, LocalTime drawTime, String timezone,
                            String timeSource, String timeSourceDetail, LocalDateTime generatedAt,
                            String actualNumbers, LocalDateTime resultConfirmedAt) {}
    public record PredictionResponse(String model, int revision, List<Integer> numbers, String modelVersion,
                                     String sourceCommit, LocalDateTime generatedAt, String reason, boolean active,
                                     int hitCount, List<Integer> hitNumbers) {}
    public record RoundResponse(int round, LocalDate drawDate, LocalTime drawTime, String timezone, String timeSource,
                                String timeSourceDetail, LocalDateTime generatedAt, List<Integer> actualNumbers,
                                LocalDateTime resultConfirmedAt, List<PredictionResponse> predictions) {}
}
