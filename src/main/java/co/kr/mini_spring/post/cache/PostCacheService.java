package co.kr.mini_spring.post.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostCacheService {
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${cache.post.list-ttl-seconds:60}")
    private long listTtlSeconds;

    public Optional<Object> get(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    public void putList(String key, Object value) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(listTtlSeconds));
    }

    public void evict(String key) {
        redisTemplate.delete(key);
    }

    public void evictLists() {
        redisTemplate.delete(redisTemplate.keys("posts:list:*"));
    }
}
