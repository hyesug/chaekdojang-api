package com.chaekdojang.api.infra.wikidata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WikidataBookCategoryClientTest {

    @Test
    void matchesAuthorDespiteExpandedForeignName() {
        assertThat(WikidataBookCategoryClient.authorMatches(
                "존 윌리엄스", "존 에드워드 윌리엄스의 소설")).isTrue();
        assertThat(WikidataBookCategoryClient.authorMatches(
                "헨리 데이비드 소로", "헨리 데이비드 소로의 대표적 수필집")).isTrue();
        assertThat(WikidataBookCategoryClient.authorMatches(
                "다른 작가", "존 에드워드 윌리엄스의 소설")).isFalse();
    }

    @Test
    void mapsAuthorityDescriptionsToStandardCategories() {
        assertThat(WikidataBookCategoryClient.categoryFromDescription(
                "존 에드워드 윌리엄스의 소설")).isEqualTo("소설");
        assertThat(WikidataBookCategoryClient.categoryFromDescription(
                "헨리 데이비드 소로의 대표적 수필집")).isEqualTo("시/에세이");
        assertThat(WikidataBookCategoryClient.categoryFromDescription(
                "미국의 록 밴드")).isNull();
    }
}
