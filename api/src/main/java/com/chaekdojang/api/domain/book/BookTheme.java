package com.chaekdojang.api.domain.book;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 책의 주제 — 운세 리포트가 "그 사람에게 지금 필요한 것"을 말하는 말과 같은 갈래다.
 * 책 소개글을 읽고 이 가운데 1~3개를 붙인다(BookThemeService). 운세 쪽은 사람에게 필요한 주제를
 * 고르고(public/unse/src/report.js bookNeeds) 이 코드로 추천 API 를 부른다.
 *
 * seeds — 그 주제의 책이 적을 때 후보를 모으는 검색어(제목 검색이라 짧은 낱말). 태그는 소개글로 다시 붙인다.
 */
public enum BookTheme {
    DECISION("결단·정리", "결정하고 끊고 정리하는 법, 단순하게 사는 법", List.of("결정", "정리", "단순하게")),
    REST("쉼·회복", "번아웃, 휴식, 지친 몸과 마음의 회복", List.of("번아웃", "휴식", "쉼")),
    RELATION("관계·대화", "말하기, 대화, 갈등을 푸는 법, 사람 사이", List.of("대화", "말 그릇", "관계")),
    MONEY("돈 관리", "재테크, 저축, 투자, 돈을 대하는 태도", List.of("재테크", "돈", "부자")),
    LEADERSHIP("리더십·책임", "사람을 이끌고 책임을 지는 법, 조직", List.of("리더", "리더십", "팀장")),
    CHALLENGE("시작·도전", "새로 시작하고 실행하는 용기, 창업", List.of("도전", "시작", "실행")),
    EXPRESSION("표현·창작", "글쓰기, 말하기, 창작, 나를 드러내는 법", List.of("글쓰기", "창작", "표현")),
    STUDY("공부·성장", "배우는 법, 공부법, 꾸준한 성장", List.of("공부", "배움", "독학")),
    INDEPENDENCE("나답게 살기", "자기다움, 독립, 남의 기대에서 벗어나기", List.of("나답게", "자존감", "혼자")),
    LOVE("연애·결혼", "사랑, 연애, 부부 관계", List.of("사랑", "연애", "결혼")),
    PARENTING("육아·가족", "아이를 키우는 법, 부모, 가족 관계", List.of("육아", "부모", "아이")),
    HEALTH("몸·건강", "몸 관리, 운동, 식습관, 잠", List.of("건강", "운동", "수면")),
    MIND("마음·감정", "불안, 걱정, 감정 다루기, 마음 챙김", List.of("불안", "감정", "마음")),
    CAREER("일·커리어", "일하는 법, 직업, 커리어 방향", List.of("커리어", "일하는", "직업")),
    HABIT("습관·집중", "좋은 습관, 집중, 미루지 않는 법", List.of("습관", "집중", "미루기")),
    WISDOM("삶의 지혜", "인생, 철학, 삶의 방향을 생각하게 하는 책", List.of("인생", "철학", "삶")),
    COMFORT("위로·공감", "지친 마음을 다독이는 에세이, 위로", List.of("위로", "괜찮아", "다정"));

    private final String label;
    private final String hint;
    private final List<String> seeds;

    BookTheme(String label, String hint, List<String> seeds) {
        this.label = label;
        this.hint = hint;
        this.seeds = seeds;
    }

    public String label() { return label; }
    public String hint() { return hint; }
    public List<String> seeds() { return seeds; }

    public static Optional<BookTheme> fromCode(String code) {
        if (code == null) return Optional.empty();
        String c = code.trim().toUpperCase();
        return Arrays.stream(values()).filter(t -> t.name().equals(c)).findFirst();
    }

    /** 추천·태그 대상이 아닌 종류 — 학습서·수험서·잡지·유아·어린이·외국어·한국소개도서 */
    public static final List<String> EXCLUDED_CATEGORIES = List.of(
            "중/고등참고서", "초등참고서", "취업/수험서", "잡지", "유아(0~7세)", "어린이(초등)", "외국어", "한국소개도서");
}
