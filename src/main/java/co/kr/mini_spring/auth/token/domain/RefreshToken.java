package co.kr.mini_spring.auth.token.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "refresh_token")
public class RefreshToken {

    @Id
    private Long authorId; // 사용자 ID를 Key로 사용 (사용자당 하나의 Refresh Token 유지)

    @Indexed
    private String token;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private boolean revoked = false;

    @TimeToLive
    private Long expiration; // Redis TTL (초 단위)

    @Builder
    public RefreshToken(String token, Long authorId, LocalDateTime expiresAt, boolean revoked) {
        this.token = token;
        this.authorId = authorId;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.createdAt = LocalDateTime.now();
        this.expiration = calculateExpirationSeconds(expiresAt);
    }

    private Long calculateExpirationSeconds(LocalDateTime expiresAt) {
        return Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void updateToken(String token, LocalDateTime expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.revoked = false;
        this.expiration = calculateExpirationSeconds(expiresAt);
    }

    public void revoke() {
        this.revoked = true;
        this.expiration = 1L; // 즉시 만료 처리하거나 짧은 시간 부여
    }
}