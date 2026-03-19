package co.kr.mini_spring.post.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HashtagNormalizerTest {

    @Test
    void normalize는_해시태그를_정규화한다() {
        assertThat(HashtagNormalizer.normalize(" #Java ")).isEqualTo("java");
        assertThat(HashtagNormalizer.normalize("스프링부트2026")).isEqualTo("스프링부트2026");
    }

    @Test
    void normalize는_자모만_있으면_빈문자열을_반환한다() {
        assertThat(HashtagNormalizer.normalize("ㅋㅋ")).isEmpty();
        assertThat(HashtagNormalizer.normalize("ㅎㅎ")).isEmpty();
    }

    @Test
    void normalizeAll은_중복과_무효값을_제거한다() {
        assertThat(HashtagNormalizer.normalizeAll(List.of(" Java ", "#java", "ㅋㅋ", "spring")))
                .containsExactlyInAnyOrder("java", "spring");
    }
}
