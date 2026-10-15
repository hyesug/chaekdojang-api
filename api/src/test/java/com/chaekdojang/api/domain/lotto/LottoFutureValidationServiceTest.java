package com.chaekdojang.api.domain.lotto;

import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LottoFutureValidationServiceTest {
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final LottoFuturePredictionGenerator generator = mock(LottoFuturePredictionGenerator.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final LottoFutureValidationService service =
            new LottoFutureValidationService(jdbcTemplate, new ObjectMapper(), userRepository, generator);

    private static Map<String, List<Integer>> eightTickets() {
        Map<String, List<Integer>> tickets = new HashMap<>();
        int offset = 0;
        for (String model : LottoFutureValidationPolicy.MODELS) {
            int start = 1 + offset++ * 5;
            tickets.put(model, List.of(start, start + 1, start + 2, start + 3, start + 4, start + 5));
        }
        return tickets;
    }

    private List<Object[]> predictionInserts() {
        return mockingDetails(jdbcTemplate).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("update"))
                .map(Invocation::getRawArguments)
                .filter(arguments -> ((String) arguments[0]).contains("INSERT INTO lotto_future_predictions"))
                .map(arguments -> (Object[]) arguments[1])
                .toList();
    }

    @Test
    void newRoundStoresAllEightTicketsWithOneRevisionAndOneGeneratedAt() {
        when(generator.generate(anyInt(), any(), any(), anyList()))
                .thenReturn(new LottoFuturePredictionGenerator.Generated(eightTickets(), "v3-votes", "abc1234", "unse-c991927"));
        when(jdbcTemplate.queryForObject(contains("SELECT id FROM lotto_future_prediction_rounds"), eq(Long.class), anyInt())).thenReturn(7L);

        // 응답 조회 단계는 DB 없이 NOT_FOUND 로 끝난다. 저장 호출만 검증한다.
        assertThatThrownBy(service::generateNext).isInstanceOf(CustomException.class);

        List<Object[]> inserts = predictionInserts();
        assertThat(inserts).extracting(arguments -> arguments[1]).containsExactly("A", "D", "E", "F", "H", "S1", "S2", "S3");
        assertThat(inserts).allSatisfy(arguments -> {
            assertThat(arguments[0]).isEqualTo(7L);
            assertThat(arguments[2]).isEqualTo(1);
            assertThat(arguments[4]).isEqualTo("v3-votes");
            assertThat(arguments[5]).isEqualTo("abc1234");
            assertThat(arguments[6]).isEqualTo("unse-c991927");
            assertThat(arguments[7]).isEqualTo(inserts.getFirst()[7]);
            assertThat(arguments[8]).isEqualTo("자동 생성");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void strategyRecalculationReturnsOnlyS1ToS3WithHitsAndStoresNothing() throws Exception {
        User admin = mock(User.class);
        when(admin.isAdmin()).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        ResultSet row = mock(ResultSet.class);
        when(row.getLong(1)).thenReturn(5L);
        when(row.getInt(2)).thenReturn(1245);
        when(row.getObject(3, LocalDate.class)).thenReturn(LocalDate.of(2026, 10, 10));
        when(row.getObject(4, LocalTime.class)).thenReturn(LocalTime.of(20, 35));
        when(row.getString(9)).thenReturn("[4,5,25,28,32,42]");
        when(jdbcTemplate.query(contains("WHERE draw_round = ?"), any(RowMapper.class), eq(1245)))
                .thenAnswer(invocation -> List.of(((RowMapper<Object>) invocation.getArgument(1)).mapRow(row, 0)));
        Map<String, List<Integer>> tickets = eightTickets();
        tickets.put("S1", List.of(4, 5, 6, 7, 8, 9));
        when(generator.generate(eq(1245), eq(LocalDate.of(2026, 10, 10)), eq(LocalTime.of(20, 35)), anyList()))
                .thenReturn(new LottoFuturePredictionGenerator.Generated(tickets, "v4-votes", "abc1234", "unse-c991927"));

        LottoFutureValidationService.RecalculationResponse response = service.recalculateStrategies(1L, 1245);

        assertThat(response.modelVersion()).isEqualTo("v4-votes");
        assertThat(response.engineVersion()).isEqualTo("unse-c991927");
        assertThat(response.predictions()).extracting(LottoFutureValidationService.RecalculatedPrediction::model).containsExactly("S1", "S2", "S3");
        assertThat(response.predictions().getFirst().hitNumbers()).containsExactly(4, 5);
        assertThat(response.predictions().getFirst().hitCount()).isEqualTo(2);
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void onlyPredictionsGeneratedBeforeTheDrawAreOfficial() {
        LocalDate date = LocalDate.of(2026, 10, 17);
        LocalTime time = LocalTime.of(20, 35);
        assertThat(LottoFutureValidationPolicy.isPreDraw(LocalDateTime.of(2026, 10, 12, 9, 0), date, time)).isTrue();
        assertThat(LottoFutureValidationPolicy.isPreDraw(LocalDateTime.of(2026, 10, 17, 20, 35), date, time)).isFalse();
        assertThat(LottoFutureValidationPolicy.isPreDraw(LocalDateTime.of(2026, 10, 18, 9, 0), date, time)).isFalse();
    }

    private List<Object[]> leaderboardInserts() {
        return mockingDetails(jdbcTemplate).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("update"))
                .map(Invocation::getRawArguments)
                .filter(arguments -> ((String) arguments[0]).contains("INSERT INTO lotto_future_leaderboard_entries"))
                .map(arguments -> (Object[]) arguments[1])
                .toList();
    }

    @Test
    void newRoundAlsoStoresLeaderboardWithTopFiveAndBaselineCutoff() {
        when(generator.generate(anyInt(), any(), any(), anyList()))
                .thenReturn(new LottoFuturePredictionGenerator.Generated(eightTickets(), "v4-votes", "abc1234", "unse-c991927"));
        when(jdbcTemplate.queryForObject(contains("SELECT id FROM lotto_future_prediction_rounds"), eq(Long.class), anyInt())).thenReturn(7L);
        when(jdbcTemplate.queryForObject(contains("FROM lotto_future_leaderboard_entries"), eq(Integer.class), anyLong())).thenReturn(1);
        when(generator.generateLeaderboard(anyInt(), any(), any(), anyList(), anyList())).thenAnswer(invocation -> new LottoFuturePredictionGenerator.Leaderboard(
                invocation.getArgument(0), "leaderboard-v1", "unse-c991927+unse-latest", List.of(
                new LottoFuturePredictionGenerator.LeaderboardEntry("SYS-L-yukim", List.of(8, 9, 18, 19, 20, 31), 1, 0.83, 1144, 0.836, 0.894, 1),
                new LottoFuturePredictionGenerator.LeaderboardEntry("N4", List.of(1, 2, 3, 4, 5, 6), null, null, 0, null, null, null))));

        assertThatThrownBy(service::generateNext).isInstanceOf(CustomException.class);

        assertThat(predictionInserts()).hasSize(8);
        List<Object[]> inserts = leaderboardInserts();
        assertThat(inserts).extracting(arguments -> arguments[2]).containsExactly("SYS-L-yukim", "N4");
        assertThat(inserts.getFirst()[9]).isEqualTo(1);
        assertThat(inserts.get(1)[9]).isNull();
        assertThat(inserts).allSatisfy(arguments -> assertThat(arguments[11]).isEqualTo("unse-c991927+unse-latest"));
        // 순위표 성적은 기준선(1245) 이후 회차만 웹으로 넘긴다
        verify(jdbcTemplate).query(contains("JOIN lotto_future_prediction_rounds"), any(RowMapper.class),
                eq(LottoFutureValidationService.LEADERBOARD_BASELINE_ROUND), anyInt());
    }

    @Test
    void leaderboardFailureDoesNotBlockTheEightModels() {
        when(generator.generate(anyInt(), any(), any(), anyList()))
                .thenReturn(new LottoFuturePredictionGenerator.Generated(eightTickets(), "v4-votes", "abc1234", "unse-c991927"));
        when(jdbcTemplate.queryForObject(contains("SELECT id FROM lotto_future_prediction_rounds"), eq(Long.class), anyInt())).thenReturn(7L);
        when(generator.generateLeaderboard(anyInt(), any(), any(), anyList(), anyList())).thenThrow(new IllegalStateException("web down"));

        assertThatThrownBy(service::generateNext).isInstanceOf(CustomException.class);

        assertThat(predictionInserts()).hasSize(8);
        assertThat(leaderboardInserts()).isEmpty();
    }

    @Test
    void missingEngineVersionFailsBeforeAnyPredictionIsStored() {
        when(generator.generate(anyInt(), any(), any(), anyList()))
                .thenReturn(new LottoFuturePredictionGenerator.Generated(eightTickets(), "v4-votes", "abc1234", null));
        when(jdbcTemplate.queryForObject(contains("SELECT id FROM lotto_future_prediction_rounds"), eq(Long.class), anyInt())).thenReturn(7L);

        assertThatThrownBy(service::generateNext).isInstanceOf(IllegalStateException.class);
        assertThat(predictionInserts()).isEmpty();
    }

    @Test
    void missingStrategyTicketFailsBeforeAnyPredictionIsStored() {
        Map<String, List<Integer>> tickets = eightTickets();
        tickets.remove("S3");
        when(generator.generate(anyInt(), any(), any(), anyList()))
                .thenReturn(new LottoFuturePredictionGenerator.Generated(tickets, "v3-votes", "abc1234", "unse-c991927"));
        when(jdbcTemplate.queryForObject(contains("SELECT id FROM lotto_future_prediction_rounds"), eq(Long.class), anyInt())).thenReturn(7L);

        assertThatThrownBy(service::generateNext).isInstanceOf(IllegalArgumentException.class);
        assertThat(predictionInserts()).isEmpty();
    }
}
