package com.chaekdojang.api.infra.ridi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RidiBookMetadataClientTest {

    private final RidiBookMetadataClient client = new RidiBookMetadataClient(new ObjectMapper());

    @Test
    void parsesAuthorFromBookJsonLd() {
        String html = """
                <script type="application/ld+json">
                  {
                    "@context": "http://schema.org",
                    "@type": "Book",
                    "name": "웨딩케이크 살인사건",
                    "author": { "@type": "Person", "name": "조앤 플루크" }
                  }
                </script>
                """;

        assertThat(client.parseAuthor(html)).isEqualTo("조앤 플루크");
    }

    @Test
    void joinsMultipleAuthorsWithoutDuplicates() {
        String html = """
                <script type="application/ld+json">
                  {
                    "@type": "Book",
                    "author": [
                      { "@type": "Person", "name": "김수지" },
                      { "@type": "Person", "name": "P" },
                      { "@type": "Person", "name": "김수지" }
                    ]
                  }
                </script>
                """;

        assertThat(client.parseAuthor(html)).isEqualTo("김수지, P");
    }
}
