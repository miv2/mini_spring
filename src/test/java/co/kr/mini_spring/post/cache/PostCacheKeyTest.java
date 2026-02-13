package co.kr.mini_spring.post.cache;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostCacheKeyTest {
    @Test
    void listKey_isStableAndOrderInsensitiveForHashtags() {
        String key1 = PostCacheKey.list(0, 10, "latest", "hello", List.of("java", "spring"), 3L);
        String key2 = PostCacheKey.list(0, 10, "latest", "hello", List.of("spring", "java"), 3L);
        assertThat(key1).isEqualTo(key2);
    }
}
