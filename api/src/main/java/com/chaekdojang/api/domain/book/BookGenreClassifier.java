package com.chaekdojang.api.domain.book;

import java.util.Locale;

public final class BookGenreClassifier {
    private BookGenreClassifier() {
    }

    public static String resolve(Book book) {
        if (book == null) return null;
        return resolve(book.getCategory(), book.getSource(), book.getTitle(), book.getAuthor(), book.getDescription());
    }

    public static String resolve(
            String category, BookSource source, String title, String description) {
        return resolve(category, source, title, null, description);
    }

    public static String resolve(
            String category, BookSource source, String title, String author, String description) {
        if (source != null && source.isWebNovel()) return "소설";

        String explicit = normalizeExplicitCategory(category);
        String text = ((title == null ? "" : title) + " "
                + (description == null ? "" : description)).toLowerCase(Locale.ROOT);

        if (containsAny(text, "인문철학", "인문 철학", "철학 에세이", "인문 교양서")) return "인문";
        if (containsAtLeast(text, 2, "철학", "철학자", "철인", "스토아", "사상", "이성", "윤리",
                "성찰", "인간 본성", "삶과 죽음")) return "인문";
        if (containsAny(text, "장편소설", "단편소설", "중편소설", "소설집", "청춘 소설", "로맨스 소설",
                "미스터리 소설", "추리 소설", "sf 소설", "화제의 소설", "이 소설", "소설 《", "소설 『",
                "fiction", " novel")) return "소설";
        if (containsAtLeast(text, 3, "주인공", "그는", "그녀는", "소년", "소녀", "그러던 중", "사건",
                "인물", "여정", "거짓말", "두려움", "이야기가 시작")) return "소설";
        if (source != BookSource.KAKAO && explicit != null) return explicit;

        if (containsAny(text, "중등 참고서", "고등 참고서", "중학 참고서", "고교 참고서",
                "중학교", "고등학교", "수능", "내신 대비", "수능특강", "수능완성")) return "중/고등참고서";
        if (containsAny(text, "초등 참고서", "초등 문제집", "초등학교", "초등 학습")) return "초등참고서";
        if (containsAny(text, "취업", "공무원 시험", "자격증", "수험서", "기출문제", "적성검사",
                "면접 대비", "토익 수험", "test preparation")) return "취업/수험서";
        if (containsAny(text, "유아", "0~7세", "누리과정", "보드북", "아기 그림책", "preschool")) return "유아(0~7세)";
        if (containsAny(text, "어린이", "아동", "초등학생", "동화", "그림책", "juvenile")) return "어린이(초등)";
        if (containsAny(text, "청소년", "10대", "십 대", "young adult")) return "청소년";
        if (containsAny(text, "만화", "코믹", "그래픽노블", "graphic novel", "comics", "manga")) return "만화";
        if (containsAny(text, "외국어", "영어 학습", "영어회화", "일본어", "중국어", "한국어 학습",
                "어학", "외국어 공부", "foreign language")) return "외국어";
        if (containsAny(text, "프로그래밍", "소프트웨어", "인공지능", "데이터 분석", "컴퓨터", "코딩",
                "개발자", "coding", "computers")) return "컴퓨터/IT";
        if (containsAny(text, "공학", "기계공학", "전자공학", "전기공학", "토목", "건축공학",
                "engineering", "technology")) return "기술/공학";
        if (containsAny(text, "물리학", "생물학", "뇌과학", "천문학", "자연과학", "과학자", "유전자",
                "진화론", "우주 과학", "수학", "science")) return "과학";
        if (containsAny(text, "경제경영", "경제·경영", "경제/경영", "경제학", "경영학", "재테크", "주식 투자",
                "금융", "마케팅", "business", "economics")) return "경제/경영";
        if (containsAny(text, "자기계발", "습관의 힘", "성공법", "시간 관리", "리더십", "일하는 법",
                "말하기", "self-help")) return "자기계발";
        if (containsAny(text, "사회학", "정치학", "불평등", "민주주의", "자본주의", "페미니즘",
                "젠더", "사회 문제", "정치", "politics", "social science", "sociology")) return "정치/사회";
        if (containsAny(text, "세계사", "한국사", "근현대사", "역사서", "역사 이야기", "문화사",
                "history", "civilization")) return "역사/문화";
        if (containsAny(text, "철학", "철학자", "윤리학", "존재론", "심리학", "심리 상담", "트라우마",
                "인문학", "인문 교양", "고전 읽기", "humanities", "philosophy", "psychology")) return "인문";
        if (containsAny(text, "육아", "부모", "자녀교육", "임신", "출산", "가정생활", "parenting",
                "family & relationships")) return "가정/육아";
        if (containsAny(text, "요리", "레시피", "베이킹", "쿠킹", "cookbook", "cooking")) return "요리";
        if (containsAny(text, "건강", "의학", "질병", "영양학", "다이어트", "치료", "health", "medical")) return "건강";
        if (containsAny(text, "취미", "실용", "스포츠", "운동법", "반려동물", "원예", "공예", "바둑",
                "games", "sports", "hobbies", "crafts")) return "취미/실용/스포츠";
        if (containsAny(text, "종교", "기독교", "천주교", "불교", "성경", "신앙", "religion")) return "종교";
        if (containsAny(text, "미술", "음악", "예술", "디자인", "사진", "영화", "대중문화", "art",
                "music", "performing arts", "photography")) return "예술/대중문화";
        if (containsAny(text, "여행기", "여행 에세이", "여행 안내", "관광", "travel")) return "여행";
        if (containsAny(text, "잡지", "매거진", "정기간행물", "magazine", "periodical")) return "잡지";
        if (containsAny(text, "시집", "시선집", "시인", "시 작품", "에세이", "산문집", "수필",
                "poetry", "essay")) return "시/에세이";
        if (containsAny(text, "장편소설", "단편소설", "중편소설", "소설집", "청춘 소설", "로맨스",
                "미스터리", "스릴러", "sf 소설", "소설", "문학 작품", "문학전집", "fiction", " novel")) return "소설";
        if (containsAny(text, "인문", "교양서")) return "인문";
        return source == BookSource.KAKAO ? null : explicit;
    }

    private static String normalizeExplicitCategory(String category) {
        if (category == null || category.isBlank()) return null;
        String value = category.trim();
        String lower = value.toLowerCase(Locale.ROOT);

        if (containsAny(lower, "중/고등참고서", "중고등참고서", "중등참고서", "고등참고서")) return "중/고등참고서";
        if (containsAny(lower, "초등참고서")) return "초등참고서";
        if (containsAny(lower, "취업/수험서", "취업·수험서", "study aids", "test preparation")) return "취업/수험서";
        if (containsAny(lower, "유아(0~7세)", "유아", "preschool")) return "유아(0~7세)";
        if (containsAny(lower, "어린이(초등)", "juvenile")) return "어린이(초등)";
        if (containsAny(lower, "청소년", "young adult")) return "청소년";
        if (containsAny(lower, "만화", "comics", "graphic novels", "manga")) return "만화";
        if (containsAny(lower, "외국어", "foreign language", "language study")) return "외국어";
        if (containsAny(lower, "컴퓨터/it", "컴퓨터·it", "computers", "computer")) return "컴퓨터/IT";
        if (containsAny(lower, "기술/공학", "기술·공학", "technology", "engineering")) return "기술/공학";
        if (containsAny(lower, "과학", "science", "mathematics")) return "과학";
        if (containsAny(lower, "경제/경영", "경제·경영", "business", "economics", "경제", "경영")) return "경제/경영";
        if (containsAny(lower, "자기계발", "self-help")) return "자기계발";
        if (containsAny(lower, "정치/사회", "사회·정치", "politics", "social science", "sociology", "사회", "정치")) return "정치/사회";
        if (containsAny(lower, "역사/문화", "history", "역사", "문화")) return "역사/문화";
        if (containsAny(lower, "인문", "philosophy", "psychology", "철학", "심리")) return "인문";
        if (containsAny(lower, "가정/육아", "가정·육아", "parenting", "family & relationships", "육아")) return "가정/육아";
        if (containsAny(lower, "요리", "cooking")) return "요리";
        if (containsAny(lower, "건강", "health", "medical")) return "건강";
        if (containsAny(lower, "취미/실용/스포츠", "취미·실용·스포츠", "sports", "hobbies", "crafts")) return "취미/실용/스포츠";
        if (containsAny(lower, "종교", "religion")) return "종교";
        if (containsAny(lower, "예술/대중문화", "예술·대중문화", "arts", "art", "music", "photography")) return "예술/대중문화";
        if (containsAny(lower, "여행", "travel")) return "여행";
        if (containsAny(lower, "잡지", "periodical", "magazine")) return "잡지";
        if (containsAny(lower, "시/에세이", "시·에세이", "poetry", "essay", "에세이", "수필", "시집")) return "시/에세이";
        if (!lower.contains("non-fiction") && containsAny(lower, "fiction", "novel", "소설", "웹소설")) return "소설";

        return null;
    }

    private static boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) return true;
        }
        return false;
    }

    private static boolean containsAtLeast(String value, int minimum, String... keywords) {
        int matches = 0;
        for (String keyword : keywords) {
            if (value.contains(keyword) && ++matches >= minimum) return true;
        }
        return false;
    }
}
