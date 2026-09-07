package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.dojangdan.dto.ReaderTrackRecordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 독자별 서평단 완주 이력 계산 (명세 §3).
 * 별도 집계 테이블 없이 신청 레코드의 단계별 시각에서 그때그때 계산한다.
 * 독자 수가 많지 않은 초기 단계에서는 이 방식이 배치보다 단순하고 항상 최신이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReaderTrackRecordService {

    private final ReviewCampaignApplicationRepository applicationRepository;

    public ReaderTrackRecordResponse forUser(Long userId) {
        return calculate(applicationRepository.findByUserIdOrderByAppliedAtDesc(userId));
    }

    /**
     * 신청자 목록 화면에서 N+1 조회를 피하려고 여러 사용자 이력을 한 번에 계산한다.
     */
    public Map<Long, ReaderTrackRecordResponse> forUsers(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        Map<Long, List<ReviewCampaignApplication>> byUser = applicationRepository.findByUserIdIn(userIds)
                .stream()
                .collect(Collectors.groupingBy(application -> application.getUser().getId()));
        return userIds.stream()
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        userId -> calculate(byUser.getOrDefault(userId, List.of()))
                ));
    }

    private ReaderTrackRecordResponse calculate(List<ReviewCampaignApplication> applications) {
        if (applications.isEmpty()) {
            return ReaderTrackRecordResponse.empty();
        }

        // 선정된 적이 있는지는 상태가 아니라 selectedAt 유무로 본다.
        // 제출·중도포기로 상태가 바뀐 뒤에도 "선정된 이력"은 남아야 하기 때문이다.
        List<ReviewCampaignApplication> selected = applications.stream()
                .filter(application -> application.getSelectedAt() != null)
                .toList();
        List<ReviewCampaignApplication> submitted = selected.stream()
                .filter(application -> application.getSubmittedAt() != null)
                .toList();

        Integer completionRate = selected.isEmpty()
                ? null
                : (int) Math.round(submitted.size() * 100.0 / selected.size());

        Integer averageReviewLength = averageReviewLength(submitted);
        Double averageDaysToSubmit = averageDaysToSubmit(submitted);
        Integer onTimeRate = onTimeRate(submitted);

        return new ReaderTrackRecordResponse(
                applications.size(),
                selected.size(),
                submitted.size(),
                completionRate,
                averageReviewLength,
                averageDaysToSubmit,
                onTimeRate
        );
    }

    private Integer averageReviewLength(List<ReviewCampaignApplication> submitted) {
        List<Integer> lengths = submitted.stream()
                .map(ReviewCampaignApplication::getReview)
                .filter(review -> review != null && review.getDeletedAt() == null)
                .map(review -> review.getContent().length())
                .toList();
        if (lengths.isEmpty()) return null;
        return (int) Math.round(lengths.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private Double averageDaysToSubmit(List<ReviewCampaignApplication> submitted) {
        if (submitted.isEmpty()) return null;
        double averageHours = submitted.stream()
                .mapToLong(application -> Duration.between(
                        application.getSelectedAt(), application.getSubmittedAt()).toHours())
                .average()
                .orElse(0);
        return Math.round(averageHours / 24.0 * 10) / 10.0;
    }

    private Integer onTimeRate(List<ReviewCampaignApplication> submitted) {
        if (submitted.isEmpty()) return null;
        long onTime = submitted.stream()
                .filter(application -> !application.getSubmittedAt()
                        .isAfter(application.getCampaign().getReviewDueAt()))
                .count();
        return (int) Math.round(onTime * 100.0 / submitted.size());
    }
}
