package com.chaekdojang.api.domain.readinggoal;

import com.chaekdojang.api.domain.readinggoal.dto.ReadingGoalResponse;
import com.chaekdojang.api.domain.readinggoal.dto.ReadingGoalUpdateRequest;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadingGoalService {

    private final ReadingGoalRepository readingGoalRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ReadingGoalResponse getMyGoal() {
        Long userId = SecurityUtils.getCurrentUserId();
        return getGoal(userId, currentYear(), true);
    }

    public ReadingGoalResponse getPublicGoal(Long userId) {
        return getGoal(userId, currentYear(), false);
    }

    @Transactional
    public ReadingGoalResponse updateMyGoal(ReadingGoalUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        int year = currentYear();
        boolean publicVisible = request.publicVisible() == null || request.publicVisible();

        ReadingGoal goal = readingGoalRepository.findByUserIdAndYear(userId, year)
                .orElseGet(() -> ReadingGoal.create(user, year, request.targetCount(), publicVisible));
        goal.update(request.targetCount(), publicVisible);
        ReadingGoal saved = readingGoalRepository.save(goal);
        return ReadingGoalResponse.of(saved, countFinishedInYear(userId, year));
    }

    private ReadingGoalResponse getGoal(Long userId, int year, boolean includePrivate) {
        long finishedCount = countFinishedInYear(userId, year);
        return readingGoalRepository.findByUserIdAndYear(userId, year)
                .filter(goal -> includePrivate || goal.isPublicVisible())
                .map(goal -> ReadingGoalResponse.of(goal, finishedCount))
                .orElseGet(() -> ReadingGoalResponse.empty(year, finishedCount));
    }

    private long countFinishedInYear(Long userId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = start.plusYears(1);
        Object result = entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM libraries
                        WHERE user_id = :userId
                          AND status = 'FINISHED'
                          AND completed_at >= :startDate
                          AND completed_at < :endDate
                        """)
                .setParameter("userId", userId)
                .setParameter("startDate", start)
                .setParameter("endDate", end)
                .getSingleResult();
        return ((Number) result).longValue();
    }

    private int currentYear() {
        return LocalDate.now().getYear();
    }
}
