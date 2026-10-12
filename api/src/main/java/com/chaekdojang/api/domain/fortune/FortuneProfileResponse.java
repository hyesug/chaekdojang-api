package com.chaekdojang.api.domain.fortune;

public record FortuneProfileResponse(
        Long id,
        boolean self,
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
        return new FortuneProfileResponse(p.getId(), p.isSelf(), p.getName(), p.getGender(), p.getBirthYear(),
                p.getBirthMonth(), p.getBirthDay(), p.getBirthHour(), p.getBirthMinute(), p.getBirthPlace(),
                p.getHomePlace(), p.isDst());
    }
}
