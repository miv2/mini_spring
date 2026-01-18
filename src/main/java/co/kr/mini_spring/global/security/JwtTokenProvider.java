package co.kr.mini_spring.global.security;

import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.member.domain.MemberRole;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * JWT 토큰 생성/검증 컴포넌트
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /**
     * JwtAuthenticationEntryPoint에서 토큰 검증 실패 사유를 구분하기 위해 사용합니다.
     */
    public static final String JWT_ERROR_CODE_ATTRIBUTE = "jwt.error.code";

    private final Key secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        // secret을 그대로 byte로 사용하거나 Base64 인코딩 문자열인 경우 디코딩 처리
        Key key;
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            key = Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            // Base64가 아니라면 바로 bytes 사용
            key = Keys.hmacShaKeyFor(secret.getBytes());
        }
        this.secretKey = key;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * 액세스 토큰 생성
     */
    public TokenWithExpiry generateAccessToken(String email, MemberRole role) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessTokenExpiration);

        String token = Jwts.builder()
                .setSubject(email)
                .claim("role", role.name())
                .claim("type", "access")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        return new TokenWithExpiry(token, LocalDateTime.ofInstant(expiry, ZoneId.systemDefault()));
    }

    /**
     * 리프레시 토큰 생성
     */
    public TokenWithExpiry generateRefreshToken(String email) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(refreshTokenExpiration);

        String token = Jwts.builder()
                .setSubject(email)
                .claim("type", "refresh")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        return new TokenWithExpiry(token, LocalDateTime.ofInstant(expiry, ZoneId.systemDefault()));
    }

    /**
     * 토큰 검증 (서명/만료)
     */
    public boolean validateToken(String token) {
        return validateTokenWithResult(token).valid;
    }

    /**
     * 토큰 검증 결과를 상세 코드로 반환합니다.
     * - 필터/엔트리포인트에서 "만료/형식 오류/서명 오류" 등을 구분해 응답할 때 사용합니다.
     */
    public JwtValidationResult validateTokenWithResult(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return JwtValidationResult.valid();
        } catch (ExpiredJwtException e) {
            log.debug("JWT 만료: {}", e.getMessage());
            return JwtValidationResult.invalid(ResponseCode.EXPIRED_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.debug("JWT 미지원: {}", e.getMessage());
            return JwtValidationResult.invalid(ResponseCode.UNSUPPORTED_TOKEN);
        } catch (MalformedJwtException | SecurityException | IllegalArgumentException e) {
            log.debug("JWT 형식/서명 오류: {}", e.getMessage());
            return JwtValidationResult.invalid(ResponseCode.INVALID_TOKEN);
        } catch (Exception e) {
            log.debug("JWT 검증 실패: {}", e.getMessage());
            return JwtValidationResult.invalid(ResponseCode.INVALID_TOKEN);
        }
    }

    /**
     * 토큰에서 이메일(subject) 추출
     */
    public String getEmail(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build()
                .parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    /**
     * JWT 검증 결과 DTO.
     */
    @Getter
    @RequiredArgsConstructor
    public static class JwtValidationResult {
        private final boolean valid;
        private final ResponseCode errorCode;

        public static JwtValidationResult valid() {
            return new JwtValidationResult(true, null);
        }

        public static JwtValidationResult invalid(ResponseCode errorCode) {
            return new JwtValidationResult(false, errorCode);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class TokenWithExpiry {
        private final String token;
        private final LocalDateTime expiresAt;
    }
}
