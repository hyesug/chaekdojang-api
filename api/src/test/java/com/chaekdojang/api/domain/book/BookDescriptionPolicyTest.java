package com.chaekdojang.api.domain.book;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookDescriptionPolicyTest {

    @Test
    void trimsAbruptExternalExcerptAtLastCompleteSentence() {
        String description = "푸시킨, 고골 등과 함께 러시아 문학의 황금시대라 불리는 19세기 단편문학을 주도한 체호프의 단편선이 민음사에서 출간되었다. "
                + "세계문학전집 70번으로 발간된 체호프의 작품집에는 국내에 처음으로 소개되는 단편소설 아홉 편-「공포」, 「베짱이」, 「드라마」, 「베로치카」, 「미녀」, 「거울」, 「내기」, 「티푸스」, 「주교」-과 체호프 식 소설 구조의 전형을 보여 주는 작품 「관리의 죽음」이 수록되었다. "
                + "이 작품들은 모두 다양한 인물 군상을 통해 사소";

        assertThat(BookDescriptionPolicy.looksAbruptlyTruncated(description)).isTrue();
        assertThat(BookDescriptionPolicy.displaySynopsis(description, false))
                .isEqualTo("푸시킨, 고골 등과 함께 러시아 문학의 황금시대라 불리는 19세기 단편문학을 주도한 체호프의 단편선이 민음사에서 출간되었다. "
                        + "세계문학전집 70번으로 발간된 체호프의 작품집에는 국내에 처음으로 소개되는 단편소설 아홉 편-「공포」, 「베짱이」, 「드라마」, 「베로치카」, 「미녀」, 「거울」, 「내기」, 「티푸스」, 「주교」-과 체호프 식 소설 구조의 전형을 보여 주는 작품 「관리의 죽음」이 수록되었다.");
    }

    @Test
    void prefersCompleteDescriptionOverLongerAbruptExcerpt() {
        String complete = "작품에 대한 완결된 책 소개입니다.";
        String abrupt = "중간에서 끝난 외부 소개 ".repeat(30);

        assertThat(BookDescriptionPolicy.qualityScore(complete))
                .isGreaterThan(BookDescriptionPolicy.qualityScore(abrupt));
    }

    @Test
    void doesNotRewriteWebNovelOfficialDescription() {
        String description = "문장부호 없이 끝나는 웹소설 공식 작품 소개 ".repeat(20).trim();

        assertThat(BookDescriptionPolicy.displaySynopsis(description, true)).isEqualTo(description);
    }
}
