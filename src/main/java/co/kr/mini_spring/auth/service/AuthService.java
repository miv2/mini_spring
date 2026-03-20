package co.kr.mini_spring.auth.service;

import co.kr.mini_spring.auth.dto.response.TokenResponse;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import co.kr.mini_spring.auth.token.domain.RefreshToken;
import co.kr.mini_spring.auth.token.repository.RefreshTokenRepository;
import co.kr.mini_spring.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 인증 관련 서비스 (OAuth2 전용)
 * - 토큰 갱신 (Refresh Token)
 * - 로그아웃 및 블랙리스트 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SocialMemberRepository socialMemberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String BLACKLIST_PREFIX = "bl:access:";

    /**
     * Refresh Token을 사용하여 새 Access/Refresh Token 발급
     */
    @Transactional
    public TokenResponse refreshToken(String refreshTokenValue) {
        JwtTokenProvider.JwtValidationResult validation = jwtTokenProvider
                .validateTokenWithResult(refreshTokenValue);
        if (!validation.isValid()) {
            throw new BusinessException(validation.getErrorCode());
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ResponseCode.REFRESH_TOKEN_NOT_FOUND));

        if (refreshToken.isExpired() || refreshToken.isRevoked()) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException(ResponseCode.REFRESH_TOKEN_EXPIRED);
        }

        SocialMember member = socialMemberRepository.findById(refreshToken.getAuthorId())
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));

        JwtTokenProvider.TokenWithExpiry newAccessToken = jwtTokenProvider
                .generateAccessToken(member.getEmail(), member.getRole());
        JwtTokenProvider.TokenWithExpiry newRefreshToken = jwtTokenProvider
                .generateRefreshToken(member.getEmail());

        refreshToken.updateToken(newRefreshToken.getToken(), newRefreshToken.getExpiresAt());
        refreshTokenRepository.save(refreshToken);

        log.info("[TokenRefresh] 토큰 갱신 성공 memberId={}, email={}", member.getId(), member.getEmail());

        return TokenResponse.builder()
                .accessToken(newAccessToken.getToken())
                .refreshToken(newRefreshToken.getToken())
                .accessTokenExpiresAt(newAccessToken.getExpiresAt())
                .refreshTokenExpiresAt(newRefreshToken.getExpiresAt())
                .build();
    }

    /**
     * 로그아웃 - RefreshToken 폐기 및 AccessToken 블랙리스트 등록
     */
    @Transactional
    public void logout(String email, String accessToken) {
        SocialMember member = socialMemberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));

        // 1. Refresh Token 폐기
        refreshTokenRepository.findByAuthorId(member.getId())
                .ifPresent(refreshToken -> {
                    refreshToken.revoke();
                    refreshTokenRepository.save(refreshToken);
                });

        // 2. Access Token 블랙리스트 등록
        if (accessToken != null && !accessToken.isBlank()) {
            blacklistAccessToken(accessToken);
        }

        log.info("[Logout] 로그아웃 성공 memberId={}", member.getId());
    }

    /**
     * Access Token을 블랙리스트에 등록하여 남은 수명 동안 재사용 방지
     */
    private void blacklistAccessToken(String token) {
        try {
            LocalDateTime expiresAt = jwtTokenProvider.getExpiresAt(token);
            long ttlSeconds = Duration.between(LocalDateTime.now(), expiresAt).getSeconds();

            if (ttlSeconds > 0) {
                stringRedisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + token,
                        "1",
                        Duration.ofSeconds(ttlSeconds));
                log.debug("[Blacklist] 토큰 등록 완료. 남은 시간: {}초", ttlSeconds);
            }
        } catch (Exception e) {
            log.warn("[Blacklist] 토큰 등록 실패: {}", e.getMessage());
        }
    }
}
