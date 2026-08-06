package com.chaekdojang.api.domain.book;

import java.util.Locale;

public final class BookGenreClassifier {
    private BookGenreClassifier() {
    }

    public static String resolve(Book book) {
        if (book == null) return "기타";
        return resolve(book.getCategory(), book.getSource(), book.getTitle(), book.getDescription());
    }

    public static String resolve(
            String category, BookSource source, String title, String description) {
        if (source != null && source.isWebNovel()) return "웹소설";
        String explicit = normalizeExplicitCategory(category);
        if (explicit != null) return explicit;

        String text = ((title == null ? "" : title) + " "
                + (description == null ? "" : description)).toLowerCase(Locale.ROOT);

        if (containsAny(text, "장편소설", "단편소설", "중편소설", "소설집", "청춘 소설",
                "로맨스", "미스터리", "스릴러", "sf 소설", "fiction", " novel")) return "소설";
        if (containsAny(text, "시집", "시선집", "시인", "시 작품", "poetry")) return "시";
        if (containsAny(text, "에세이", "산문집", "수필", "essay")) return "에세이";
        if (containsAny(text, "세계사", "한국사", "근현대사", "역사서", "역사 이야기", "history")) return "역사";
        if (containsAny(text, "철학", "철학자", "윤리학", "존재론", "philosophy")) return "철학";
        if (containsAny(text, "경제경영", "경제·경영", "경제학", "경영학", "재테크", "주식 투자",
                "금융", "마케팅", "business", "economics")) return "경제·경영";
        if (containsAny(text, "자기계발", "습관의 힘", "성공법", "시간 관리", "리더십", "일하는 법",
                "말하기", "self-help")) return "자기계발";
        if (containsAny(text, "심리학", "심리 상담", "마음 치유", "트라우마", "psychology")) return "심리";
        if (containsAny(text, "물리학", "생물학", "뇌과학", "천문학", "자연과학", "과학자", "유전자",
                "진화론", "우주 과학", "science")) return "과학";
        if (containsAny(text, "사회학", "정치학", "불평등", "민주주의", "자본주의", "페미니즘",
                "젠더", "국가는 왜", "사회 문제", "politics")) return "사회·정치";
        if (containsAny(text, "인문학", "인문 교양", "고전 읽기", "문명", "문화사", "humanities")) return "인문";
        if (containsAny(text, "미술", "음악", "예술", "디자인", "사진")) return "예술";
        if (containsAny(text, "여행기", "여행 에세이", "여행 안내", "travel")) return "여행";
        if (containsAny(text, "건강", "의학", "요리", "레시피", "운동법", "health")) return "건강·생활";
        if (containsAny(text, "프로그래밍", "소프트웨어", "인공지능", "데이터 분석", "컴퓨터", "coding")) return "IT";
        if (containsAny(text, "어린이", "아동", "그림책", "동화", "children")) return "어린이";
        if (containsAny(text, "만화", "코믹", "그래픽노블", "comics")) return "만화";
        if (containsAny(text, "소설", "문학 작품", "문학전집")) return "소설";
        if (containsAny(text, "인문", "교양서", "고전")) return "인문";
        return "기타";
    }

    private static String normalizeExplicitCategory(String category) {
        if (category == null || category.isBlank()) return null;
        String value = category.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (!lower.contains("non-fiction")
                && containsAny(lower, "fiction", "novel", "소설")) return "소설";
        if (containsAny(lower, "poetry", "시집")) return "시";
        if (containsAny(lower, "essay", "에세이", "수필")) return "에세이";
        if (containsAny(lower, "history", "역사")) return "역사";
        if (containsAny(lower, "philosophy", "철학")) return "철학";
        if (containsAny(lower, "psychology", "심리")) return "심리";
        if (containsAny(lower, "business", "economics", "경제", "경영")) return "경제·경영";
        if (containsAny(lower, "self-help", "자기계발")) return "자기계발";
        if (containsAny(lower, "science", "과학")) return "과학";
        if (containsAny(lower, "politics", "sociology", "사회", "정치")) return "사회·정치";
        return value.length() <= 100 ? value : value.substring(0, 100);
    }

    private static boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) return true;
        }
        return false;
    }
}
