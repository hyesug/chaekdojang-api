package com.chaekdojang.api.domain.lotto;

import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LottoFutureValidationService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    // 순위표 기준선(웹 analysis/fortune-lotto/output/adaptive-leaderboard/baseline.json)의 마지막 회차. 그 뒤 저장분만 성적으로 넘긴다.
    static final int LEADERBOARD_BASELINE_ROUND = 1245;
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
        LottoFutureValidationPolicy.DrawSchedule schedule = LottoFutureValidationPolicy.drawSchedule(date);
        if (date.equals(now.toLocalDate()) && !now.toLocalTime().isBefore(schedule.drawTime())) {
            date = date.plusWeeks(1);
            schedule = LottoFutureValidationPolicy.drawSchedule(date);
        }
        return generate(LottoFutureValidationPolicy.roundOf(date), date, schedule.drawTime(),
                schedule.timeSource(), schedule.timeSourceDetail(), "자동 생성");
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
        tryLeaderboard(existing.id, round, existing.drawDate, drawTime, reason.trim());
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
        tryLeaderboard(existing.id, round, existing.drawDate, existing.drawTime, reason.trim());
        return response(roundRow(round));
    }

    // 추첨 전 회차의 순위표를 (다시) 만든다. 실패하면 오류를 그대로 돌려준다.
    @Transactional
    public RoundResponse regenerateLeaderboard(Long userId, int round, String reason) {
        assertAdmin(userId);
        RoundRow existing = roundRow(round);
        if (reason == null || reason.isBlank() || !ZonedDateTime.of(existing.drawDate, existing.drawTime, KST).isAfter(ZonedDateTime.now(KST))) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        storeLeaderboard(existing.id, round, existing.drawDate, existing.drawTime, reason.trim());
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

    // 득표 전략을 현재 엔진으로 다시 계산해 참고값으로만 돌려준다. 저장하지 않으며 정식 성적에 넣지 않는다.
    @Transactional(readOnly = true)
    public RecalculationResponse recalculateStrategies(Long userId, int round) {
        assertAdmin(userId);
        RoundRow existing = roundRow(round);
        if (existing.actualNumbers == null) throw new CustomException(ErrorCode.INVALID_REQUEST);
        List<LottoFuturePredictionGenerator.History> pastHistory = history().stream().filter(row -> row.round() < round).toList();
        LottoFuturePredictionGenerator.Generated generated = generator.generate(round, existing.drawDate, existing.drawTime, pastHistory);
        List<Integer> actual = numbers(existing.actualNumbers);
        List<RecalculatedPrediction> predictions = LottoFutureValidationPolicy.STRATEGY_MODELS.stream().map(model -> {
            List<Integer> ticket = generated.predictions().get(model);
            LottoFutureValidationPolicy.requireTicket(ticket);
            List<Integer> hits = ticket.stream().filter(actual::contains).toList();
            return new RecalculatedPrediction(model, ticket, hits.size(), hits);
        }).toList();
        return new RecalculationResponse(round, generated.modelVersion(), generated.sourceCommit(), generated.engineVersion(), predictions);
    }

    private RoundResponse generate(int round, LocalDate drawDate, LocalTime drawTime, String timeSource, String detail, String reason) {
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(?)", (rs, rowNum) -> 0, (long) round);
        List<Long> ids = jdbcTemplate.query("SELECT id FROM lotto_future_prediction_rounds WHERE draw_round = ?", (rs, rowNum) -> rs.getLong(1), round);
        if (!ids.isEmpty()) {
            RoundRow existing = roundRow(round);
            if ("official_schedule_override".equals(timeSource) && "official_default".equals(existing.timeSource)
                    && ZonedDateTime.of(existing.drawDate, existing.drawTime, KST).isAfter(ZonedDateTime.now(KST))) {
                jdbcTemplate.update("UPDATE lotto_future_prediction_rounds SET draw_time = ?, time_source = ?, time_source_detail = ? WHERE id = ?",
                        drawTime, timeSource, detail, existing.id);
                jdbcTemplate.update("UPDATE lotto_future_predictions SET active = false WHERE prediction_round_id = ? AND active", existing.id);
                LottoFuturePredictionGenerator.Generated generated = generator.generate(round, existing.drawDate, drawTime, history());
                insertPredictions(existing.id, generated, detail, nextRevision(existing.id));
                tryLeaderboard(existing.id, round, existing.drawDate, drawTime, detail);
            }
            return response(roundRow(round));
        }
        LottoFuturePredictionGenerator.Generated generated = generator.generate(round, drawDate, drawTime, history());
        LocalDateTime now = LocalDateTime.now(KST);
        jdbcTemplate.update("""
                INSERT INTO lotto_future_prediction_rounds
                (draw_round, draw_date, draw_time, timezone, time_source, time_source_detail, generated_at)
                VALUES (?, ?, ?, 'Asia/Seoul', ?, ?, ?)
                """, round, drawDate, drawTime, timeSource, detail, now);
        long roundId = jdbcTemplate.queryForObject("SELECT id FROM lotto_future_prediction_rounds WHERE draw_round = ?", Long.class, round);
        insertPredictions(roundId, generated, reason, 1);
        tryLeaderboard(roundId, round, drawDate, drawTime, reason);
        return response(roundRow(round));
    }

    private void insertPredictions(long roundId, LottoFuturePredictionGenerator.Generated generated, String reason, int revision) {
        Map<String, List<Integer>> predictions = generated.predictions();
        LottoFutureValidationPolicy.MODELS.forEach(model -> LottoFutureValidationPolicy.requireTicket(predictions.get(model)));
        if (generated.engineVersion() == null || generated.engineVersion().isBlank()) {
            throw new IllegalStateException("로또 미래검증 계산 결과에 엔진 버전이 없습니다.");
        }
        LocalDateTime generatedAt = LocalDateTime.now(KST);
        for (String model : LottoFutureValidationPolicy.MODELS) {
            List<Integer> ticket = predictions.get(model);
            jdbcTemplate.update("""
                    INSERT INTO lotto_future_predictions
                    (prediction_round_id, model, revision, numbers, model_version, source_commit, engine_version, generated_at, reason, active)
                    VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, true)
                    """, roundId, model, revision, json(ticket), generated.modelVersion(), generated.sourceCommit(),
                    generated.engineVersion(), generatedAt, reason);
        }
    }

    // 순위표 실패가 기존 8개 모델 저장을 막지 않게 한다(관리자 화면에서 다시 생성할 수 있다).
    private void tryLeaderboard(long roundId, int round, LocalDate drawDate, LocalTime drawTime, String reason) {
        try {
            storeLeaderboard(roundId, round, drawDate, drawTime, reason);
        } catch (RuntimeException exception) {
            log.warn("로또 순위표를 만들지 못했습니다. round={}", round, exception);
        }
    }

    private void storeLeaderboard(long roundId, int round, LocalDate drawDate, LocalTime drawTime, String reason) {
        if (round <= LEADERBOARD_BASELINE_ROUND) throw new CustomException(ErrorCode.INVALID_REQUEST);
        LottoFuturePredictionGenerator.Leaderboard board = generator.generateLeaderboard(round, drawDate, drawTime, history(), leaderboardResults(round));
        if (board == null || board.entries() == null || board.entries().isEmpty() || board.round() != round
                || board.engineVersion() == null || board.engineVersion().isBlank()) {
            throw new IllegalStateException("로또 순위표 계산 결과가 올바르지 않습니다.");
        }
        board.entries().forEach(entry -> LottoFutureValidationPolicy.requireTicket(entry.numbers()));
        if (board.entries().stream().filter(entry -> entry.top5Rank() != null).count() > 5) {
            throw new IllegalStateException("로또 순위표 상위 규칙이 5개를 넘습니다.");
        }
        Integer revision = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(revision), 0) + 1 FROM lotto_future_leaderboard_entries WHERE prediction_round_id = ?", Integer.class, roundId);
        jdbcTemplate.update("UPDATE lotto_future_leaderboard_entries SET active = false WHERE prediction_round_id = ? AND active", roundId);
        LocalDateTime generatedAt = LocalDateTime.now(KST);
        for (LottoFuturePredictionGenerator.LeaderboardEntry entry : board.entries()) {
            jdbcTemplate.update("""
                    INSERT INTO lotto_future_leaderboard_entries
                    (prediction_round_id, revision, rule_id, numbers, rank, score, record_rounds, record_mean, recent_mean, top5_rank,
                     model_version, engine_version, generated_at, reason, active, detail)
                    VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true, ?)
                    """, roundId, revision == null ? 1 : revision, entry.ruleId(), json(entry.numbers()), entry.rank(), entry.score(),
                    entry.recordRounds(), entry.recordMean(), entry.recentMean(), entry.top5Rank(), board.modelVersion(),
                    board.engineVersion(), generatedAt, reason, detail(entry.detail()));
        }
    }

    // 기준선 이후·대상 이전 회차 중 당첨이 확정되고 추첨 전에 저장된 순위표 번호만 성적으로 넘긴다.
    private List<LottoFuturePredictionGenerator.LeaderboardResult> leaderboardResults(int round) {
        record Row(int round, String actual, String ruleId, String numbers) {}
        List<Row> rows = jdbcTemplate.query("""
                SELECT r.draw_round, r.actual_numbers::text, e.rule_id, e.numbers::text
                FROM lotto_future_leaderboard_entries e
                JOIN lotto_future_prediction_rounds r ON r.id = e.prediction_round_id
                WHERE e.active AND r.actual_numbers IS NOT NULL AND r.draw_round > ? AND r.draw_round < ?
                  AND e.generated_at < r.draw_date + r.draw_time
                ORDER BY r.draw_round, e.rule_id
                """, (rs, rowNum) -> new Row(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                LEADERBOARD_BASELINE_ROUND, round);
        Map<Integer, LottoFuturePredictionGenerator.LeaderboardResult> results = new TreeMap<>();
        for (Row row : rows) {
            results.computeIfAbsent(row.round(), key -> new LottoFuturePredictionGenerator.LeaderboardResult(key, numbers(row.actual()), new LinkedHashMap<>()))
                    .tickets().put(row.ruleId(), numbers(row.numbers()));
        }
        return List.copyOf(results.values());
    }

    private static String detail(String value) {
        return value == null || value.isBlank() ? null : value.length() > 2000 ? value.substring(0, 2000) : value;
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
                SELECT model, revision, numbers::text, model_version, source_commit, generated_at, reason, active, engine_version
                FROM lotto_future_predictions WHERE prediction_round_id = ? ORDER BY model, revision DESC
                """, (rs, rowNum) -> {
            List<Integer> ticket = numbers(rs.getString(3));
            List<Integer> actual = round.actualNumbers == null ? null : numbers(round.actualNumbers);
            List<Integer> hits = actual == null ? List.of() : ticket.stream().filter(actual::contains).toList();
            LocalDateTime generatedAt = rs.getObject(6, LocalDateTime.class);
            return new PredictionResponse(rs.getString(1), rs.getInt(2), ticket, rs.getString(4), rs.getString(5),
                    generatedAt, rs.getString(7), rs.getBoolean(8), hits.size(), hits,
                    LottoFutureValidationPolicy.isPreDraw(generatedAt, round.drawDate, round.drawTime), rs.getString(9));
        }, round.id);
        List<Integer> actualNumbers = round.actualNumbers == null ? null : numbers(round.actualNumbers);
        List<LeaderboardEntryResponse> leaderboard = jdbcTemplate.query("""
                SELECT rule_id, numbers::text, rank, score, record_rounds, record_mean, recent_mean, top5_rank,
                       model_version, engine_version, generated_at, revision, detail
                FROM lotto_future_leaderboard_entries WHERE prediction_round_id = ? AND active
                ORDER BY rank NULLS LAST, rule_id
                """, (rs, rowNum) -> {
            List<Integer> ticket = numbers(rs.getString(2));
            List<Integer> hits = actualNumbers == null ? List.of() : ticket.stream().filter(actualNumbers::contains).toList();
            LocalDateTime generatedAt = rs.getObject(11, LocalDateTime.class);
            return new LeaderboardEntryResponse(rs.getString(1), ticket, rs.getObject(3, Integer.class), rs.getObject(4, Double.class),
                    rs.getInt(5), rs.getObject(6, Double.class), rs.getObject(7, Double.class), rs.getObject(8, Integer.class),
                    rs.getString(9), rs.getString(10), generatedAt, rs.getInt(12), hits.size(), hits,
                    LottoFutureValidationPolicy.isPreDraw(generatedAt, round.drawDate, round.drawTime), rs.getString(13));
        }, round.id);
        return new RoundResponse(round.round, round.drawDate, round.drawTime, round.timezone, round.timeSource,
                round.timeSourceDetail, round.generatedAt, actualNumbers, round.resultConfirmedAt, predictions, leaderboard);
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
                                     int hitCount, List<Integer> hitNumbers, boolean official, String engineVersion) {}
    public record RecalculatedPrediction(String model, List<Integer> numbers, int hitCount, List<Integer> hitNumbers) {}
    public record RecalculationResponse(int round, String modelVersion, String sourceCommit, String engineVersion,
                                        List<RecalculatedPrediction> predictions) {}
    public record RoundResponse(int round, LocalDate drawDate, LocalTime drawTime, String timezone, String timeSource,
                                String timeSourceDetail, LocalDateTime generatedAt, List<Integer> actualNumbers,
                                LocalDateTime resultConfirmedAt, List<PredictionResponse> predictions,
                                List<LeaderboardEntryResponse> leaderboard) {}
    public record LeaderboardEntryResponse(String ruleId, List<Integer> numbers, Integer rank, Double score, int recordRounds,
                                           Double recordMean, Double recentMean, Integer top5Rank, String modelVersion,
                                           String engineVersion, LocalDateTime generatedAt, int revision, int hitCount,
                                           List<Integer> hitNumbers, boolean official, String detail) {}
}
