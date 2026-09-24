package com.chaekdojang.api.domain.fortune;

public record FortuneProfileResponse(
        String name,
        String gender,
        int year,
        int month,
        int day,
        Integer hour,
        Integer minute,
        String birthPlace,
        String homePlace,
        boolean dst
) {
    static FortuneProfileResponse from(FortuneProfile p) {
        return new FortuneProfileResponse(p.getName(), p.getGender(), p.getBirthYear(), p.getBirthMonth(),
                p.getBirthDay(), p.getBirthHour(), p.getBirthMinute(), p.getBirthPlace(), p.getHomePlace(), p.isDst());
    }
}
