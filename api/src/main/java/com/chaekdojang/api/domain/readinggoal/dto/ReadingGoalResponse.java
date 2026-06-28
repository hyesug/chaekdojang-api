package com.chaekdojang.api.domain.readinggoal.dto;

import com.chaekdojang.api.domain.readinggoal.ReadingGoal;

public record ReadingGoalResponse(
        int year,
        Integer targetCount,
        long finishedCount,
        long remainingCount,
        int progressPercent,
        boolean publicVisible
) {
    public static ReadingGoalResponse empty(int year, long finishedCount) {
        return new ReadingGoalResponse(year, null, finishedCount, 0, 0, true);
    }

    public static ReadingGoalResponse of(ReadingGoal goal, long finishedCount) {
        long remaining = Math.max(goal.getTargetCount() - finishedCount, 0);
        int progress = goal.getTargetCount() <= 0
                ? 0
                : (int) Math.min(100, Math.round((finishedCount * 100.0) / goal.getTargetCount()));
        return new ReadingGoalResponse(
                goal.getYear(),
                goal.getTargetCount(),
                finishedCount,
                remaining,
                progress,
                goal.isPublicVisible()
        );
    }
}
