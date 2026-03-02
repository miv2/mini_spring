package co.kr.mini_spring.chat.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomAccessCacheService {

    private static final String PREFIX = "chat:ws:access:";
    private static final String VALUE = "1";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${chat.websocket.access-cache-ttl-seconds:600}")
    private long accessCacheTtlSeconds;

    public boolean isAuthorized(Long roomId, Long userId) {
        try {
            Boolean exists = stringRedisTemplate.hasKey(key(roomId, userId));
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("[채팅 권한 캐시 조회 실패] roomId={}, userId={}, reason={}", roomId, userId, e.getMessage());
            return false;
        }
    }

    public void grant(Long roomId, Long userId) {
        try {
            stringRedisTemplate.opsForValue().set(
                    key(roomId, userId),
                    VALUE,
                    Duration.ofSeconds(accessCacheTtlSeconds)
            );
        } catch (Exception e) {
            log.warn("[채팅 권한 캐시 저장 실패] roomId={}, userId={}, reason={}", roomId, userId, e.getMessage());
        }
    }

    public void revoke(Long roomId, Long userId) {
        try {
            stringRedisTemplate.delete(key(roomId, userId));
        } catch (Exception e) {
            log.warn("[채팅 권한 캐시 삭제 실패] roomId={}, userId={}, reason={}", roomId, userId, e.getMessage());
        }
    }

    public void grantAfterCommit(Long roomId, Long userId) {
        afterCommit(() -> grant(roomId, userId));
    }

    public void revokeAfterCommit(Long roomId, Long userId) {
        afterCommit(() -> revoke(roomId, userId));
    }

    private void afterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
            return;
        }
        runnable.run();
    }

    private String key(Long roomId, Long userId) {
        return PREFIX + roomId + ":" + userId;
    }
}
