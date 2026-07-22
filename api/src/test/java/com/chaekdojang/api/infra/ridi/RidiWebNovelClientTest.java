package com.chaekdojang.api.infra.ridi;

import com.chaekdojang.api.domain.book.BookSource;
import com.chaekdojang.api.domain.book.dto.WebNovelSearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RidiWebNovelClientTest {

    private final RidiWebNovelClient client = new RidiWebNovelClient(new ObjectMapper());

    @Test
    void parsesOfficialPartialTitleSearchResult() {
        String html = """
                <script id="__NEXT_DATA__" type="application/json">
                {
                  "props": {
                    "pageProps": {
                      "gridData": {
                        "riGrid": {
                          "grid": {
                            "cells": [{
                              "cell__SearchBookListWithTab": {
                                "books": [{
                                  "book": {
                                    "authors": [{"name": "연산호", "role": "AUTHOR"}],
                                    "introduction": {"description": "은행에서 시작되는 현대 판타지"},
                                    "series": {
                                      "id": "6188000001",
                                      "title": "은행원도 용꿈을 꾸나요",
                                      "thumbnail": {
                                        "large": "https://img.ridicdn.net/cover/6188000157/large#1"
                                      }
                                    }
                                  }
                                }]
                              }
                            }]
                          }
                        }
                      }
                    }
                  }
                }
                </script>
                """;

        List<WebNovelSearchResult> results = client.parseSearchResults("용꿈", html);

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.title()).isEqualTo("은행원도 용꿈을 꾸나요");
            assertThat(result.author()).isEqualTo("연산호");
            assertThat(result.platform()).isEqualTo(BookSource.RIDI);
            assertThat(result.externalId()).isEqualTo("6188000001");
            assertThat(result.sourceUrl()).isEqualTo("https://ridibooks.com/books/6188000001");
            assertThat(result.thumbnail())
                    .isEqualTo("https://img.ridicdn.net/cover/6188000157/large");
        });
    }
}
