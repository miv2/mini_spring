package co.kr.mini_spring.auth.service;

import co.kr.mini_spring.auth.dto.request.LoginRequest;
import co.kr.mini_spring.auth.dto.response.TokenResponse;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.member.domain.repository.MemberRepository;
import co.kr.mini_spring.auth.token.domain.RefreshToken;
import co.kr.mini_spring.auth.token.repository.RefreshTokenRepository;
import co.kr.mini_spring.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));

        if (member.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
            throw new BusinessException(ResponseCode.INVALID_PASSWORD);
        }

        if (member.getStatus() != co.kr.mini_spring.member.domain.MemberStatus.ACTIVE) {
            throw new BusinessException(ResponseCode.UNAUTHENTICATED);
        }

        JwtTokenProvider.TokenWithExpiry accessToken =
                jwtTokenProvider.generateAccessToken(member.getEmail(), member.getRole());
        JwtTokenProvider.TokenWithExpiry refreshToken =
                jwtTokenProvider.generateRefreshToken(member.getEmail());

        refreshTokenRepository.findByMemberId(member.getId())
                .ifPresentOrElse(
                        existing -> existing.updateToken(refreshToken.getToken(), refreshToken.getExpiresAt()),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .token(refreshToken.getToken())
                                        .memberId(member.getId())
                                        .expiresAt(refreshToken.getExpiresAt())
                                        .revoked(false)
                                        .build()
                        )
                );

        log.info("[Login] 로그인 성공 memberId={}, email={}", member.getId(), member.getEmail());

        return TokenResponse.builder()
                .accessToken(accessToken.getToken())
                .refreshToken(refreshToken.getToken())
                .accessTokenExpiresAt(accessToken.getExpiresAt())
                .refreshTokenExpiresAt(refreshToken.getExpiresAt())
                .build();
    }

    @Transactional
    public TokenResponse refreshToken(String refreshTokenValue) {
        JwtTokenProvider.JwtValidationResult validation = jwtTokenProvider.validateTokenWithResult(refreshTokenValue);
        if (!validation.isValid()) {
            throw new BusinessException(validation.getErrorCode());
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ResponseCode.REFRESH_TOKEN_NOT_FOUND));

        if (refreshToken.isExpired() || refreshToken.isRevoked()) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException(ResponseCode.REFRESH_TOKEN_EXPIRED);
        }

        Member member = memberRepository.findById(refreshToken.getMemberId())
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));

        JwtTokenProvider.TokenWithExpiry newAccessToken =
                jwtTokenProvider.generateAccessToken(member.getEmail(), member.getRole());
        JwtTokenProvider.TokenWithExpiry newRefreshToken =
                jwtTokenProvider.generateRefreshToken(member.getEmail());

        refreshToken.updateToken(newRefreshToken.getToken(), newRefreshToken.getExpiresAt());

        log.info("[TokenRefresh] 토큰 갱신 성공 memberId={}, email={}", member.getId(), member.getEmail());

        return TokenResponse.builder()
                .accessToken(newAccessToken.getToken())
                .refreshToken(newRefreshToken.getToken())
                .accessTokenExpiresAt(newAccessToken.getExpiresAt())
                .refreshTokenExpiresAt(newRefreshToken.getExpiresAt())
                .build();
    }

    /**
     * 로그아웃
     * - email로 회원을 찾아 RefreshToken을 삭제
     */
    @Transactional
    public void logout(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));

        refreshTokenRepository.findByMemberId(member.getId())
                .ifPresent(refreshToken -> {
                    refreshToken.revoke();
                    refreshTokenRepository.save(refreshToken);
                });

        log.info("[Logout] 로그아웃 성공 memberId={}", member.getId());
    }
}
